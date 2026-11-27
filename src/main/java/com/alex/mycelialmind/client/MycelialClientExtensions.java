package com.alex.mycelialmind.client;

import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.Supplier;

/**
 * Registers NeoForge client extensions for bio-effect status framing and ergonomic tool arm poses.
 */
public final class MycelialClientExtensions {
    private MycelialClientExtensions() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(accentEffectExtension(0xFFD95BFF), ModEffects.MYCELIAL_INFECTION.get());
        event.registerMobEffect(accentEffectExtension(0xFF7BE048), ModEffects.SPORE_ROT.get());
        event.registerMobEffect(accentEffectExtension(0xFF4CD6E8), ModEffects.SYNAPTIC_VISION.get());

        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return entityLiving.isShiftKeyDown()
                        ? HumanoidModel.ArmPose.CROSSBOW_HOLD
                        : HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
        }, ModItems.NEUTRALIZING_SPORE_LAUNCHER.get());

        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                if (entityLiving.isUsingItem() && entityLiving.getUsedItemHand() == hand) {
                    return HumanoidModel.ArmPose.SPYGLASS;
                }
                return entityLiving.isShiftKeyDown()
                        ? HumanoidModel.ArmPose.SPYGLASS
                        : HumanoidModel.ArmPose.ITEM;
            }
        }, ModItems.BIO_SCANNER.get());

        event.registerBlock(organicBlockExtension(ModParticles.SPORE_MOTE::get, 10),
                ModBlocks.CREEPING_MYCELIUM.get(),
                ModBlocks.SPORE_VENT.get(),
                ModBlocks.SPORE_BULB.get(),
                ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get(),
                ModBlocks.MYCELIAL_TENDRILS.get());

        event.registerBlock(organicBlockExtension(ModParticles.SYNAPSE_SPARK::get, 12),
                ModBlocks.MYCELIAL_NODE.get(),
                ModBlocks.INCUBATOR_NODE.get(),
                ModBlocks.SENTINEL_NODE.get(),
                ModBlocks.SYMBIOTIC_NODE.get(),
                ModBlocks.HIVE_ALTAR.get(),
                ModBlocks.MATRIARCH_ALTAR.get(),
                ModBlocks.SOVEREIGN_NEXUS.get());

        event.registerBlock(organicBlockExtension(ModParticles.PURGE::get, 10),
                ModBlocks.PURIFICATION_PYLON.get(),
                ModBlocks.PURIFICATION_TURRET.get(),
                ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());

        event.registerBlock(organicBlockExtension(ModParticles.HISTAMINE_PULSE::get, 12),
                ModBlocks.ANTIBODY_CYST.get());
    }

    private static IClientBlockExtensions organicBlockExtension(
            Supplier<? extends ParticleOptions> particleSupplier,
            int destroyBurstCount
    ) {
        return new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
                if (target instanceof BlockHitResult blockHit) {
                    double scale = MycelialClientConfig.particleDensityScale();
                    int count = scale >= 1.15D ? 2 : 1;
                    BlockPos pos = blockHit.getBlockPos();
                    Direction face = blockHit.getDirection();
                    var random = level.getRandom();
                    ParticleOptions particle = particleSupplier.get();
                    for (int i = 0; i < count; i++) {
                        double ox = 0.5D + face.getStepX() * 0.55D + (random.nextDouble() - 0.5D) * 0.45D;
                        double oy = 0.5D + face.getStepY() * 0.55D + (random.nextDouble() - 0.5D) * 0.45D;
                        double oz = 0.5D + face.getStepZ() * 0.55D + (random.nextDouble() - 0.5D) * 0.45D;
                        level.addParticle(particle,
                                pos.getX() + ox, pos.getY() + oy, pos.getZ() + oz,
                                (random.nextDouble() - 0.5D) * 0.03D,
                                0.02D + random.nextDouble() * 0.03D,
                                (random.nextDouble() - 0.5D) * 0.03D);
                    }
                }
                return false;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                double scale = MycelialClientConfig.particleDensityScale();
                int count = Math.max(2, (int) Math.round(destroyBurstCount * scale));
                var random = level.getRandom();
                ParticleOptions particle = particleSupplier.get();
                for (int i = 0; i < count; i++) {
                    double px = pos.getX() + 0.15D + random.nextDouble() * 0.70D;
                    double py = pos.getY() + 0.15D + random.nextDouble() * 0.70D;
                    double pz = pos.getZ() + 0.15D + random.nextDouble() * 0.70D;
                    level.addParticle(particle, px, py, pz,
                            (random.nextDouble() - 0.5D) * 0.07D,
                            0.03D + random.nextDouble() * 0.06D,
                            (random.nextDouble() - 0.5D) * 0.07D);
                }
                return false;
            }
        };
    }

    private static IClientMobEffectExtensions accentEffectExtension(int argbColor) {
        return new IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(
                    MobEffectInstance instance,
                    EffectRenderingInventoryScreen<?> screen,
                    GuiGraphics guiGraphics,
                    int x,
                    int y,
                    int blitOffset
            ) {
                guiGraphics.fill(x + 1, y + 4, x + 3, y + 20, argbColor);
                return false;
            }

            @Override
            public boolean renderGuiIcon(
                    MobEffectInstance instance,
                    Gui gui,
                    GuiGraphics guiGraphics,
                    int x,
                    int y,
                    float z,
                    float alpha
            ) {
                int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
                int color = (a << 24) | (argbColor & 0x00FFFFFF);
                guiGraphics.fill(x + 1, y + 2, x + 3, y + 22, color);
                return false;
            }
        };
    }
}
