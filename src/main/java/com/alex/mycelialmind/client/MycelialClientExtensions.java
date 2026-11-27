package com.alex.mycelialmind.client;

import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

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
