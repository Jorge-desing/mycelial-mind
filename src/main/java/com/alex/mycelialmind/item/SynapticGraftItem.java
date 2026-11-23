package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.IncubatorNodeBlock;
import com.alex.mycelialmind.block.MycelialNodeBlock;
import com.alex.mycelialmind.block.SentinelNodeBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Reprograms an enemy Synaptic Node (Relay, Incubator, or Sentinel) into an allied
 * Symbiotic Node, subject to the per-dimension active Symbiotic Node cap.
 */
public class SynapticGraftItem extends Item {
    public SynapticGraftItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = serverLevel.getBlockState(pos);
        Block block = state.getBlock();
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
        PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
        int cap = MycelialConfig.MAX_SYMBIOTIC_NODES.get();
        if (tracker.countSymbioticNodes() >= cap) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.symbiotic_cap_reached", cap)
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer
                && !com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector.beforeNodeGrafted(
                        serverPlayer, serverLevel, pos)) {
            return InteractionResult.FAIL;
        }

        if (serverLevel.getBlockEntity(pos) instanceof SynapticNodeBlockEntity prevBe) {
            prevBe.purgeVolatileBuffer(serverLevel, pos, true);
        }
        serverLevel.setBlockAndUpdate(pos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
        tracker.addNode(pos, SynapticNodeType.SYMBIOTIC);
        if (serverLevel.getBlockEntity(pos) instanceof SynapticNodeBlockEntity be) {
            be.setSymbioticStability(100);
        }

        serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                22, 0.45, 0.45, 0.45, 0.04);
        serverLevel.playSound(null, pos, ModSounds.SYNAPTIC_GRAFT_SPLICE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        player.displayClientMessage(
                Component.translatable("message.mycelialmind.node_grafted")
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        if (!player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.mycelialmind.synaptic_graft").withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
