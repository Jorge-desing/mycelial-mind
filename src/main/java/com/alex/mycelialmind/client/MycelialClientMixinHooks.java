package com.alex.mycelialmind.client;

import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialBossMob;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.util.MycelialClientMixinPolicy;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.ModDimensions;
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
        if (viewer == null || entity == null || entity.level() != viewer.level() || !isSynapticVisionTarget(entity)) {
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

    public static int resolveEntityTeamColor(@Nullable Entity entity, int originalColor) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer viewer = mc != null ? mc.player : null;
        if (viewer == null || entity == null || entity.level() != viewer.level()) {
            return originalColor;
        }
        boolean hasExplicitTeam = entity.getTeam() != null;
        boolean viewerHasSynapticVision = viewer.hasEffect(ModEffects.SYNAPTIC_VISION);
        boolean isDisguisedMimic = entity instanceof SynapticMimicEntity mimic && !mimic.isUnmasked();
        MycelialClientMixinPolicy.OutlineCategory category = classifyOutlineCategory(entity);
        return MycelialClientMixinPolicy.resolveSynapticOutlineColor(
                originalColor,
                hasExplicitTeam,
                viewerHasSynapticVision,
                isDisguisedMimic,
                category);
    }

    public static boolean shouldEntityShake(@Nullable LivingEntity entity, boolean originalShaking) {
        if (originalShaking || entity == null) {
            return originalShaking;
        }
        boolean hasSporeRot = entity.hasEffect(ModEffects.SPORE_ROT);
        boolean isHostileNonBossMycelial = entity instanceof MycelialMob && !(entity instanceof MycelialBossMob);
        return MycelialClientMixinPolicy.shouldEntityShake(
                false,
                hasSporeRot,
                isHostileNonBossMycelial,
                ClientHiveCache.getSuppressionSecondsRemaining());
    }

    public static boolean shouldSuppressVanillaMusic(@Nullable Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) {
            return false;
        }
        boolean bossNearby = ClientSoundscapeDirector.isBossNearby();
        boolean fungalTideOutsidePylon = ClientHiveCache.getFungalTideSecondsRemaining() > 0
                && !ClientHiveCache.isInProtectedZone();
        boolean inDeepMycelium = mc.level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);
        boolean homeostasisElevated = ClientHiveCache.getHomeostasisStage()
                != DimensionalHomeostasisEngine.HomeostasisStage.STABLE;
        return MycelialClientMixinPolicy.shouldSuppressVanillaMusic(
                bossNearby,
                fungalTideOutsidePylon,
                inDeepMycelium,
                homeostasisElevated);
    }

    public static boolean hasHungerOrSporeRotFoodPenalty(@Nullable Player player, boolean vanillaHasHunger) {
        if (vanillaHasHunger || player == null) {
            return vanillaHasHunger;
        }
        return MycelialClientMixinPolicy.shouldShowSporeRotFoodSprites(
                false,
                player.hasEffect(ModEffects.SPORE_ROT),
                ClientHiveCache.isInProtectedZone());
    }

    private static boolean isSynapticVisionTarget(Entity entity) {
        return entity instanceof MycelialMob
                || entity instanceof ColonySymbiontEntity
                || entity instanceof PurifierDroneEntity
                || entity instanceof MycelialChimeraEntity;
    }

    private static MycelialClientMixinPolicy.OutlineCategory classifyOutlineCategory(Entity entity) {
        if (entity instanceof ColonySymbiontEntity
                || entity instanceof PurifierDroneEntity
                || entity instanceof MycelialChimeraEntity) {
            return MycelialClientMixinPolicy.OutlineCategory.ALLY;
        }
        if (entity instanceof MycelialBossMob) {
            return MycelialClientMixinPolicy.OutlineCategory.BOSS;
        }
        if (entity instanceof MycelialMob mob) {
            EvolutionRank rank = mob.getEvolutionRank();
            if (rank == EvolutionRank.ELITE) {
                return MycelialClientMixinPolicy.OutlineCategory.ELITE;
            }
            if (rank == EvolutionRank.MUTATED) {
                return MycelialClientMixinPolicy.OutlineCategory.MUTATED;
            }
            return MycelialClientMixinPolicy.OutlineCategory.BASE;
        }
        return MycelialClientMixinPolicy.OutlineCategory.NONE;
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
