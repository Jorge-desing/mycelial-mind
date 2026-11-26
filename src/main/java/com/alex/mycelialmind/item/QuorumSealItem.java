package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.AlliedEscortSavedData;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.network.DiplomacySyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Relic of Chemical Diplomacy:
 * - Shift+Right-Click on a Symbiotic Node: Offer 4 Mycelial Biomass for +12 strain reputation.
 * - Right-Click when ALLIED (>= +60): Summon up to 2 temporary strain escorts (45s, 90s cooldown).
 */
public class QuorumSealItem extends Item {
    public static final int MAX_ALLIED_ESCORTS = 2;
    public static final int ESCORT_DURATION_TICKS = 900; // 45s
    public static final int ESCORT_COOLDOWN_TICKS = 1800; // 90s

    public QuorumSealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof SymbioticNodeBlock)) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            if (!consumeTribute(sp)) {
                sp.displayClientMessage(Component.translatable("message.mycelialmind.quorum_tribute_missing")
                        .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.FAIL;
            }
            PlayerDiplomacyData dip = sp.getData(ModAttachments.DIPLOMACY);
            HiveStrain strain = dip.getSelectedStrain();
            dip.adjustReputation(strain, 12);
            DiplomacySyncPayload.syncToPlayer(sp);
            sp.getCooldowns().addCooldown(this, 100);
            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.04);
            serverLevel.playSound(null, pos, ModSounds.QUORUM_SEAL_PACT.get(), SoundSource.PLAYERS, 1.0F, 1.15F);
            sp.displayClientMessage(Component.translatable("message.mycelialmind.quorum_tribute_accepted",
                    MycelialText.strain(strain), dip.getReputation(strain)).withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            PlayerDiplomacyData dip = sp.getData(ModAttachments.DIPLOMACY);
            HiveStrain strain = dip.getSelectedStrain();
            if (dip.getBetrayalCooldownTicks() > 0) {
                sp.displayClientMessage(Component.translatable("message.mycelialmind.diplomacy_betrayal_locked",
                        (dip.getBetrayalCooldownTicks() + 19) / 20).withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (dip.getStanding(strain) != PlayerDiplomacyData.Standing.ALLIED) {
                sp.displayClientMessage(Component.translatable("message.mycelialmind.quorum_requires_alliance",
                        MycelialText.strain(strain), PlayerDiplomacyData.ALLIED_THRESHOLD).withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(stack);
            }

            AlliedEscortSavedData escortData = AlliedEscortSavedData.get(serverLevel.getServer());
            long currentTick = serverLevel.getServer().overworld().getGameTime();
            for (ServerLevel candidateLevel : serverLevel.getServer().getAllLevels()) {
                for (var entity : candidateLevel.getAllEntities()) {
                    if (entity instanceof MycelialMob escort && escort.isAlive()
                            && escort.isAlliedEscortActive()
                            && sp.getUUID().equals(escort.getAlliedOwnerUuid())) {
                        escort.registerAlliedEscort();
                    }
                }
            }
            long activeEscorts = escortData.countActiveForOwner(sp.getUUID(), currentTick);
            if (activeEscorts >= MAX_ALLIED_ESCORTS) {
                sp.displayClientMessage(Component.translatable("message.mycelialmind.quorum_escort_cap",
                        MAX_ALLIED_ESCORTS).withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(stack);
            }

            int toSpawn = (int) Math.min(MAX_ALLIED_ESCORTS - activeEscorts, 2);
            long expiry = serverLevel.getGameTime() + ESCORT_DURATION_TICKS;
            int spawned = 0;
            for (int i = 0; i < toSpawn; i++) {
                MycelialMob escort = (i == 0)
                        ? ModEntities.MYCELIAL_STALKER.get().create(serverLevel)
                        : ModEntities.SPORELING.get().create(serverLevel);
                if (escort != null) {
                    double ox = (i == 0 ? -1.5 : 1.5);
                    escort.moveTo(sp.getX() + ox, sp.getY() + 0.2, sp.getZ() + 1.2, sp.getYRot(), 0.0F);
                    escort.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(sp.blockPosition()),
                            MobSpawnType.TRIGGERED, null);
                    escort.setHiveStrain(strain);
                    escort.setAlliedEscort(sp.getUUID(), expiry);
                    if (serverLevel.noCollision(escort) && serverLevel.addFreshEntity(escort)) {
                        escort.registerAlliedEscort();
                        spawned++;
                    }
                }
            }
            if (spawned == 0) {
                return InteractionResultHolder.fail(stack);
            }
            sp.getCooldowns().addCooldown(this, ESCORT_COOLDOWN_TICKS);
            serverLevel.playSound(null, sp.blockPosition(), ModSounds.QUORUM_SEAL_PACT.get(), SoundSource.PLAYERS, 0.95F, 1.0F);
            sp.displayClientMessage(Component.translatable("message.mycelialmind.quorum_escorts_summoned",
                    spawned, MycelialText.strain(strain)).withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static boolean consumeTribute(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int needed = 4;
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.MYCELIAL_BIOMASS.get())) {
                found += s.getCount();
            }
        }
        if (found < needed) {
            return false;
        }
        int remaining = needed;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.MYCELIAL_BIOMASS.get())) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.quorum_seal").withStyle(ChatFormatting.GREEN));
    }
}
