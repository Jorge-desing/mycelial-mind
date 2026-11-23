package com.alex.mycelialmind.item;

import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyData.WorkZoneType;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.network.SovereignColonySyncPayload;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;
import java.util.Locale;

/**
 * Sovereign Command Staff: cycles tactical marking modes on Shift+Right-Click in air,
 * marks colony INPUT/OUTPUT containers & work zones on blocks, and cycles caste on Colony Symbionts.
 */
public class SovereignCommandStaffItem extends Item {
    public enum StaffMode {
        LINK_INPUT_CHEST("link_input_chest", ChatFormatting.GREEN),
        LINK_OUTPUT_CHEST("link_output_chest", ChatFormatting.AQUA),
        MARK_HARVEST_ZONE("mark_harvest_zone", ChatFormatting.YELLOW),
        MARK_MINE_ZONE("mark_mine_zone", ChatFormatting.GOLD),
        MARK_PATROL_ZONE("mark_patrol_zone", ChatFormatting.RED),
        MARK_BUILD_ZONE("mark_build_zone", ChatFormatting.LIGHT_PURPLE),
        RALLY_COLONY("rally_colony", ChatFormatting.WHITE);

        private final String id;
        private final ChatFormatting color;

        StaffMode(String id, ChatFormatting color) {
            this.id = id;
            this.color = color;
        }

        public String id() {
            return id;
        }

        public ChatFormatting color() {
            return color;
        }

        public String nameKey() {
            return "staff_mode.mycelialmind." + id;
        }

        public StaffMode next() {
            StaffMode[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }

        public static StaffMode byId(String raw) {
            if (raw != null && !raw.isBlank()) {
                String norm = raw.trim().toLowerCase(Locale.ROOT);
                for (StaffMode m : values()) {
                    if (m.id.equals(norm)) {
                        return m;
                    }
                }
            }
            return LINK_INPUT_CHEST;
        }
    }

    public SovereignCommandStaffItem(Properties properties) {
        super(properties);
    }

    public static StaffMode getMode(ItemStack stack) {
        return StaffMode.byId(stack.get(ModDataComponents.STAFF_MODE.get()));
    }

    public static void setMode(ItemStack stack, StaffMode mode) {
        stack.set(ModDataComponents.STAFF_MODE.get(), mode.id());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                StaffMode next = getMode(stack).next();
                setMode(stack, next);
                level.playSound(null, player.blockPosition(), ModSounds.COMMAND_STAFF_MARK.get(),
                        SoundSource.PLAYERS, 0.7F, 1.3F);
                player.displayClientMessage(Component.translatable("message.mycelialmind.staff_mode_changed",
                        Component.translatable(next.nameKey()).withStyle(next.color())), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && getMode(stack) == StaffMode.RALLY_COLONY) {
            SovereignColonyData colony = SovereignColonySavedData.get(serverLevel)
                    .findNearestColony(player.blockPosition(), player.getUUID(), 128.0D);
            if (colony == null) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.no_colony_nearby")
                        .withStyle(ChatFormatting.RED), true);
            } else if (serverLevel.getBlockEntity(colony.getNexusPos()) instanceof SovereignNexusBlockEntity nexus) {
                int recalled = nexus.recallAllWorkers(serverLevel);
                player.displayClientMessage(Component.translatable("message.mycelialmind.colony_recalled", recalled)
                        .withStyle(ChatFormatting.AQUA), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        if (serverLevel.getBlockEntity(clicked) instanceof SovereignNexusBlockEntity) {
            return InteractionResult.PASS;
        }

        SovereignColonyData colony = SovereignColonySavedData.get(serverLevel)
                .findNearestColony(clicked, player.getUUID(), 128.0D);
        if (colony == null) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.no_colony_nearby")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        StaffMode mode = getMode(context.getItemInHand());
        if (mode == StaffMode.RALLY_COLONY) {
            if (serverLevel.getBlockEntity(colony.getNexusPos()) instanceof SovereignNexusBlockEntity nexus) {
                int recalled = nexus.recallAllWorkers(serverLevel);
                player.displayClientMessage(Component.translatable("message.mycelialmind.colony_recalled", recalled)
                        .withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.CONSUME;
        }

        if (mode == StaffMode.LINK_INPUT_CHEST || mode == StaffMode.LINK_OUTPUT_CHEST) {
            if (serverLevel.getCapability(Capabilities.ItemHandler.BLOCK, clicked, context.getClickedFace()) == null) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.staff_requires_container")
                        .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.FAIL;
            }
            boolean added = mode == StaffMode.LINK_INPUT_CHEST
                    ? colony.toggleInputChest(clicked)
                    : colony.toggleOutputChest(clicked);
            SovereignColonySavedData.get(serverLevel).setDirty();
            sendFeedback(serverLevel, player, clicked, mode, added);
            if (player instanceof ServerPlayer sp) {
                SovereignColonySyncPayload.syncToPlayer(sp, colony.getNexusPos(), false);
            }
            return InteractionResult.CONSUME;
        }

        WorkZoneType zoneType = switch (mode) {
            case MARK_HARVEST_ZONE -> WorkZoneType.HARVEST;
            case MARK_MINE_ZONE -> WorkZoneType.MINE;
            case MARK_PATROL_ZONE -> WorkZoneType.PATROL;
            default -> WorkZoneType.BUILD;
        };
        BlockPos anchor = zoneType == WorkZoneType.HARVEST || zoneType == WorkZoneType.MINE
                ? clicked
                : clicked.above();
        boolean added = colony.toggleWorkZone(zoneType, anchor);
        SovereignColonySavedData.get(serverLevel).setDirty();
        sendFeedback(serverLevel, player, anchor, mode, added);
        if (player instanceof ServerPlayer sp) {
            SovereignColonySyncPayload.syncToPlayer(sp, colony.getNexusPos(), false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity interactionTarget,
            InteractionHand usedHand
    ) {
        if (!(interactionTarget instanceof ColonySymbiontEntity worker)) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            SymbiontCaste nextCaste = worker.getCaste().next();
            worker.setCaste(nextCaste);
            serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                    worker.getX(), worker.getY() + 0.9, worker.getZ(), 10, 0.3, 0.3, 0.3, 0.03);
            serverLevel.playSound(null, worker.blockPosition(), ModSounds.COMMAND_STAFF_MARK.get(),
                    SoundSource.PLAYERS, 0.85F, 1.15F);
            player.displayClientMessage(Component.translatable(
                    "message.mycelialmind.symbiont_caste_switched",
                    Component.translatable(nextCaste.nameKey()).withStyle(ChatFormatting.AQUA)
            ).withStyle(ChatFormatting.GREEN), true);
            if (player instanceof ServerPlayer sp && worker.getNexusPos() != null) {
                SovereignColonySyncPayload.syncToPlayer(sp, worker.getNexusPos(), false);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    private static void sendFeedback(ServerLevel level, Player player, BlockPos pos, StaffMode mode, boolean added) {
        level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.03);
        level.playSound(null, pos, ModSounds.COMMAND_STAFF_MARK.get(), SoundSource.PLAYERS, 0.85F, added ? 1.25F : 0.85F);
        player.displayClientMessage(Component.translatable(
                added ? "message.mycelialmind.staff_marked" : "message.mycelialmind.staff_unmarked",
                Component.translatable(mode.nameKey()).withStyle(mode.color()),
                pos.getX(), pos.getY(), pos.getZ()
        ).withStyle(added ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        StaffMode mode = getMode(stack);
        tooltip.add(Component.translatable("tooltip.mycelialmind.sovereign_command_staff",
                Component.translatable(mode.nameKey()).withStyle(mode.color())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mycelialmind.sovereign_command_staff.hint").withStyle(ChatFormatting.AQUA));
    }
}
