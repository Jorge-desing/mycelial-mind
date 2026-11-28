package com.alex.mycelialmind.client;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.util.MycelialClientMixinPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Client-side runtime glue invoked by Mixins under {@code com.alex.mycelialmind.mixin.client.*}.
 * Must remain outside the Mixin package so SpongePowered Mixin does not forbid direct classloading.
 */
public final class MycelialClientMixinHooks {
    private MycelialClientMixinHooks() {
    }

    public static boolean shouldEntityAppearGlowing(Minecraft minecraft, @Nullable Entity entity, boolean original) {
        if (original) {
            return true;
        }
        LocalPlayer viewer = minecraft.player;
        if (viewer == null || entity == null || !(entity instanceof MycelialMob) || entity.level() != viewer.level()) {
            return false;
        }
        boolean hasSynapticVision = viewer.hasEffect(ModEffects.SYNAPTIC_VISION);
        if (!hasSynapticVision) {
            return false;
        }
        boolean isDisguisedMimic = entity instanceof SynapticMimicEntity mimic && !mimic.isUnmasked();
        double distanceSq = viewer.distanceToSqr(entity);
        return MycelialClientMixinPolicy.shouldSynapticVisionGlow(
                false,
                true,
                true,
                isDisguisedMimic,
                distanceSq);
    }

    public static boolean hasNightVisionOrSynapticVision(LocalPlayer player, boolean vanillaHasNightVision) {
        boolean hasSynapticVision = player != null && player.hasEffect(ModEffects.SYNAPTIC_VISION);
        return MycelialClientMixinPolicy.shouldEnableSynapticNightVision(vanillaHasNightVision, hasSynapticVision);
    }

    public static float resolveNightVisionScale(@Nullable LivingEntity entity, float partialTicks, float vanillaScale) {
        if (entity == null) {
            return vanillaScale;
        }
        MobEffectInstance synapticEffect = entity.getEffect(ModEffects.SYNAPTIC_VISION);
        if (synapticEffect == null) {
            return vanillaScale;
        }
        int duration = synapticEffect.isInfiniteDuration() ? -1 : synapticEffect.getDuration();
        return MycelialClientMixinPolicy.computeSynapticNightVisionScale(vanillaScale, true, duration, partialTicks);
    }

    public static Gui.HeartType resolveHeartType(@Nullable Player player, Gui.HeartType original) {
        if (player == null || original != Gui.HeartType.NORMAL) {
            return original;
        }
        MycelialClientMixinPolicy.DiegeticHeartStyle style = MycelialClientMixinPolicy.resolveDiegeticHeartStyle(
                true,
                player.hasEffect(ModEffects.SPORE_ROT),
                player.hasEffect(ModEffects.MYCELIAL_INFECTION));
        return switch (style) {
            case SPORE_ROT -> Gui.HeartType.WITHERED;
            case MYCELIAL_INFECTION -> Gui.HeartType.POISIONED;
            case VANILLA -> original;
        };
    }
}
