package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Bio-forged Chimera Ovum synthesized at the Synaptic Research Bench.
 * Must be incubated on an allied Symbiotic Node with >=35% stability,
 * hatching a Mycelial Chimera with 2 randomized genes (max 1 active per player).
 */
public class ChimeraOvumItem extends Item {
    public static final int STABILITY_COST = 35;

    public ChimeraOvumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        if (!(serverLevel.getBlockState(pos).getBlock() instanceof SymbioticNodeBlock)) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("message.mycelialmind.chimera_requires_symbiotic_node")
                                .withStyle(ChatFormatting.YELLOW),
                        true
                );
            }
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        if (MycelialChimeraEntity.countActiveChimerasForOwner(serverLevel, player.getUUID()) >= 1) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.chimera_limit_reached")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (!(serverLevel.getBlockEntity(pos) instanceof SynapticNodeBlockEntity be)
                || be.getSymbioticStability() < STABILITY_COST) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.chimera_low_stability", STABILITY_COST)
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResult.FAIL;
        }

        MycelialChimeraEntity chimera = ModEntities.MYCELIAL_CHIMERA.get().create(serverLevel);
        if (chimera == null) {
            return InteractionResult.FAIL;
        }

        chimera.moveTo(pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, player.getYRot(), 0.0F);
        chimera.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), MobSpawnType.BREEDING, null);
        chimera.setHomePos(pos);
        chimera.randomizeGenes(serverLevel.random);
        if (!serverLevel.noCollision(chimera) || !serverLevel.addFreshEntity(chimera)) {
            return InteractionResult.FAIL;
        }
        chimera.setOwnerUUID(player.getUUID());

        be.setSymbioticStability(be.getSymbioticStability() - STABILITY_COST);
        HiveMindSavedData.get(serverLevel).recordStimulus(StimulusType.MAGIC, 5.0, serverLevel.getGameTime());

        serverLevel.sendParticles(ModParticles.SPORE_MOTE.get(),
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                24, 0.5, 0.5, 0.5, 0.04);
        serverLevel.playSound(null, pos, ModSounds.CHIMERA_OVUM_HATCH.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        serverLevel.playSound(null, pos, ModSounds.CHIMERA_ROAR.get(), SoundSource.NEUTRAL, 0.85F, 1.15F);

        player.displayClientMessage(
                Component.translatable("message.mycelialmind.chimera_hatched",
                        MycelialText.trait(chimera.getChassisGene()),
                        MycelialText.trait(chimera.getMutationGene()))
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
        tooltipComponents.add(Component.translatable("tooltip.mycelialmind.chimera_ovum").withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
