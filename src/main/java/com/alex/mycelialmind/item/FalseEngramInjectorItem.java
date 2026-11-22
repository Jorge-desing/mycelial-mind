package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.IncubatorNodeBlock;
import com.alex.mycelialmind.block.MycelialNodeBlock;
import com.alex.mycelialmind.block.SentinelNodeBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Pavlovian disinformation injector: Shift+Right-click air to select a false StimulusType,
 * then Right-click an enemy Synaptic Node to induce 90s false over-specialization (+25% dmg from other types).
 */
public class FalseEngramInjectorItem extends Item {
    public FalseEngramInjectorItem(Properties properties) {
        super(properties);
    }

    public static StimulusType getSelectedStimulus(ItemStack stack) {
        String id = stack.get(ModDataComponents.TRAIT_ID.get());
        if (id != null) {
            for (StimulusType st : StimulusType.values()) {
                if (st.id().equals(id)) {
                    return st;
                }
            }
        }
        return StimulusType.FIRE;
    }

    public static void setSelectedStimulus(ItemStack stack, StimulusType type) {
        stack.set(ModDataComponents.TRAIT_ID.get(), type.id());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && !level.isClientSide()) {
            StimulusType current = getSelectedStimulus(stack);
            StimulusType[] vals = StimulusType.values();
            StimulusType next = vals[(current.ordinal() + 1) % vals.length];
            setSelectedStimulus(stack, next);
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.false_engram_selected", MycelialText.stimulus(next))
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        Block block = serverLevel.getBlockState(pos).getBlock();
        boolean isEnemyNode = (block instanceof MycelialNodeBlock && !(block instanceof SymbioticNodeBlock))
                || block instanceof IncubatorNodeBlock
                || block instanceof SentinelNodeBlock;
        if (!isEnemyNode) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        long now = serverLevel.getGameTime();
        if (!hive.canStartDeception(now)) {
            int rem = Math.max(hive.getDeceptionRemainingSeconds(now), hive.getDeceptionCooldownRemainingSeconds(now));
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.false_engram_cooldown", rem)
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResult.FAIL;
        }

        StimulusType selected = getSelectedStimulus(context.getItemInHand());
        hive.startDeception(selected, now);
        if (serverLevel.getBlockEntity(pos) instanceof SynapticNodeBlockEntity be) {
            be.purgeVolatileBuffer(serverLevel, pos, false);
        }

        for (ServerPlayer sp : serverLevel.players()) {
            PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(serverLevel, sp));
        }

        serverLevel.sendParticles(ModParticles.ENGRAM.get(), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                22, 0.45, 0.45, 0.45, 0.05);
        serverLevel.playSound(null, pos, ModSounds.VOLATILE_ENGRAM_PULSE.get(), SoundSource.PLAYERS, 1.0F, 1.15F);

        player.displayClientMessage(
                Component.translatable("message.mycelialmind.false_engram_injected", MycelialText.stimulus(selected))
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                true
        );
        if (!player.isCreative()) {
            EquipmentSlot slot = context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            context.getItemInHand().hurtAndBreak(1, player, slot);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.VOLATILE_ENGRAM.get()) || repair.is(ModItems.SYNAPTIC_CORE.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.mycelialmind.false_engram_injector",
                MycelialText.stimulus(getSelectedStimulus(stack))).withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
