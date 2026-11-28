package com.alex.mycelialmind.util;

import com.alex.mycelialmind.entity.ApexMatriarchEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SporeBehemothEntity;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.mixin.LivingEntityAccessor;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Runtime glue invoked by Mycelial Mind Mixins, keeping Mixin targets concise and clean.
 */
public final class MycelialMixinHooks {
    private MycelialMixinHooks() {
    }

    public static double adjustVisibilityPercent(LivingEntity self, @Nullable Entity lookingEntity, double original) {
        MixinRuntimeProbe.markLivingEntityHookInvoked();
        boolean observerIsNonBossMycelial = lookingEntity instanceof MycelialMob
                && !(lookingEntity instanceof HiveCoreBossEntity)
                && !(lookingEntity instanceof ApexMatriarchEntity)
                && !(lookingEntity instanceof SporeBehemothEntity);
        if (!observerIsNonBossMycelial) {
            return original;
        }

        int chitinPieces = 0;
        int elitePieces = 0;
        for (ItemStack armor : self.getArmorSlots()) {
            if (!armor.isEmpty() && armor.getItem() instanceof ChitinArmorItem) {
                chitinPieces++;
                if (ChitinArmorItem.getArmorRank(armor) == EvolutionRank.ELITE) {
                    elitePieces++;
                }
            }
        }
        if (chitinPieces <= 0) {
            return original;
        }

        LivingEntityAccessor accessor = (LivingEntityAccessor) self;
        boolean recentlyInCombat = MycelialMixinPolicy.isRecentlyInCombat(
                self.tickCount,
                accessor.mycelialmind$getLastHurtMobTimestamp(),
                accessor.mycelialmind$getLastHurtByMobTimestamp());
        boolean infectedOrRotted = self.hasEffect(ModEffects.MYCELIAL_INFECTION)
                || self.hasEffect(ModEffects.SPORE_ROT);

        return MycelialMixinPolicy.computePheromoneVisibility(
                original,
                true,
                chitinPieces,
                elitePieces,
                self.isSprinting(),
                self.isOnFire(),
                infectedOrRotted,
                recentlyInCombat);
    }

    public static float adjustBlockSpeedFactor(Entity self, float original) {
        Level level = self.level();
        if (level == null) {
            return original;
        }
        BlockState state = resolveSurfaceState(self, level);
        boolean onMycelialSurface = isMycelialTractionSurface(state);
        boolean isMycelialMob = self instanceof MycelialMob;
        boolean hasFullChitinSet = self instanceof LivingEntity living && ChitinArmorItem.hasFullSet(living);
        boolean hasEvolvedBoots = self instanceof LivingEntity living && hasEvolvedChitinBoots(living);
        boolean inLiquidOrAirborne = !self.onGround()
                || self.isInWaterOrBubble()
                || self.isInLava()
                || (self instanceof LivingEntity living && living.isFallFlying());

        return MycelialMixinPolicy.computeMycelialSpeedFactor(
                original,
                onMycelialSurface,
                isMycelialMob,
                hasFullChitinSet,
                hasEvolvedBoots,
                inLiquidOrAirborne);
    }

    public static int adjustFallDamage(LivingEntity self, int originalDamage, float fallDistance) {
        Level level = self.level();
        if (level == null || originalDamage <= 0) {
            return originalDamage;
        }
        BlockState state = resolveSurfaceState(self, level);
        boolean onCushionedMycelialBlock = isFallCushionSurface(state);
        if (!onCushionedMycelialBlock) {
            return originalDamage;
        }
        ItemStack feet = self.getItemBySlot(EquipmentSlot.FEET);
        boolean wearingChitinBoots = !feet.isEmpty() && feet.getItem() instanceof ChitinArmorItem;
        return MycelialMixinPolicy.computeMycelialFallDamage(
                originalDamage,
                fallDistance,
                true,
                wearingChitinBoots);
    }

    private static BlockState resolveSurfaceState(Entity self, Level level) {
        BlockPos pos = self.getBlockPosBelowThatAffectsMyMovement();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            state = level.getBlockState(self.blockPosition().below());
        }
        return state;
    }

    private static boolean isMycelialTractionSurface(BlockState state) {
        return state.is(ModBlocks.CREEPING_MYCELIUM.get())
                || state.is(ModBlocks.MYCELIAL_STONE.get())
                || state.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get());
    }

    private static boolean isFallCushionSurface(BlockState state) {
        return state.is(ModBlocks.CREEPING_MYCELIUM.get())
                || state.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                || state.is(ModBlocks.SPORE_BULB.get());
    }

    private static boolean hasEvolvedChitinBoots(LivingEntity living) {
        ItemStack feet = living.getItemBySlot(EquipmentSlot.FEET);
        if (feet.isEmpty() || !(feet.getItem() instanceof ChitinArmorItem)) {
            return false;
        }
        EvolutionRank rank = ChitinArmorItem.getArmorRank(feet);
        return rank == EvolutionRank.MUTATED || rank == EvolutionRank.ELITE;
    }
}
