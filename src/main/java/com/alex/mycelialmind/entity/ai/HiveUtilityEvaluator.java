package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.compat.sbl.MycelialSblBridge;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.StimulusType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Continuous utility scorer [0.0, 1.0] combining mob health, Hive Mind alert level,
 * EvolutionRank, AdaptationTraits, and nearest Node/Pylon blackboard memories.
 */
public final class HiveUtilityEvaluator {
    private HiveUtilityEvaluator() {
    }

    public static float responseSigmoid(float x, float k, float midpoint) {
        float clamped = Mth.clamp(x, 0.0F, 1.0F);
        return (float) (1.0D / (1.0D + Math.exp(-k * (clamped - midpoint))));
    }

    public static float responseExponential(float x, float exponent) {
        float clamped = Mth.clamp(x, 0.0F, 1.0F);
        return (float) Math.pow(clamped, Math.max(0.1F, exponent));
    }

    public static float inverseLerp01(float value, float zeroAt, float oneAt) {
        if (Math.abs(oneAt - zeroAt) < 1.0E-5F) {
            return 0.0F;
        }
        return Mth.clamp((value - zeroAt) / (oneAt - zeroAt), 0.0F, 1.0F);
    }

    /**
     * Dave Mark's GDC 2015 compensation factor: prevents multi-factor products from
     * collapsing toward 0 while preserving an exact 0.0F as a hard veto.
     */
    public static float compensate(float rawProduct, int factorCount) {
        if (rawProduct <= 0.0F) {
            return 0.0F;
        }
        if (factorCount <= 1) {
            return Mth.clamp(rawProduct, 0.0F, 1.0F);
        }
        float modFactor = 1.0F - (1.0F / factorCount);
        float makeup = (1.0F - rawProduct) * modFactor;
        return Mth.clamp(rawProduct + makeup * rawProduct, 0.0F, 1.0F);
    }

    /**
     * Adds hysteresis inertia only when rawScore > 0 so vetoed actions are never revived.
     */
    public static float applyHysteresis(float rawScore, boolean isCurrentAction, float bonus) {
        if (rawScore <= 0.0F || !isCurrentAction) {
            return Mth.clamp(rawScore, 0.0F, 1.0F);
        }
        return Mth.clamp(rawScore + Math.max(0.0F, bonus), 0.0F, 1.0F);
    }

    public static float evaluateChargeUtility(MycelialMob mob, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return 0.0F;
        }
        float healthRatio = responseSigmoid(mob.getHealth() / Math.max(1.0F, mob.getMaxHealth()), 6.0F, 0.35F);
        float alert = MycelialSblBridge.getHiveAlertLevel(mob);
        float traitBonus = mob.hasTrait(AdaptationTrait.BLAST_SHIELDED) ? 0.20F : 0.0F;
        float rankBonus = mob.getEvolutionRank() == EvolutionRank.ELITE ? 0.15F
                : (mob.getEvolutionRank() == EvolutionRank.MUTATED ? 0.08F : 0.0F);
        var order = MycelialSblBridge.getHiveOrder(mob);
        float orderBonus = (order == com.alex.mycelialmind.hive.HiveOrder.PRESSURE_TARGET
                || order == com.alex.mycelialmind.hive.HiveOrder.FLANK_PERIMETER) ? 0.18F : 0.0F;
        float cohesionBonus = MycelialSblBridge.getNearbyAllyCount(mob) >= 2 ? 0.12F : 0.0F;
        float influenceBonus = 0.0F;
        if (mob.level() instanceof ServerLevel serverLevel) {
            var cell = HiveBlackboard.getInfluenceMap(serverLevel).sample(mob.blockPosition());
            influenceBonus = 0.10F * cell.pheromone() + 0.08F * cell.control();
        }
        float xpBonus = combatXpChargeBonus(MycelialSblBridge.getCombatXp(mob));
        return Mth.clamp(0.35F * healthRatio + 0.30F * alert + traitBonus + rankBonus + orderBonus + cohesionBonus + influenceBonus + xpBonus, 0.0F, 1.0F);
    }

    public static float combatXpChargeBonus(int combatXp) {
        return Math.min(0.10F, Math.max(0, combatXp) / 1000.0F);
    }

    public static float stimulusKiteOffset(@Nullable StimulusType lastStimulus) {
        if (lastStimulus == StimulusType.MELEE) {
            return 0.08F;
        }
        if (lastStimulus == StimulusType.PROJECTILE) {
            return -0.05F;
        }
        return 0.0F;
    }

    public static float stimulusRegroupBonus(@Nullable StimulusType lastStimulus, int combatXp) {
        float stimBonus = (lastStimulus == StimulusType.EXPLOSION || lastStimulus == StimulusType.FIRE) ? 0.08F : 0.0F;
        return stimBonus + Math.min(0.06F, Math.max(0, combatXp) / 1500.0F);
    }

    public static float evaluateKiteUtility(MycelialMob mob, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return 0.0F;
        }
        double distSq = mob.distanceToSqr(target);
        float proximity = distSq < 25.0D ? 1.0F : (distSq < 64.0D ? 0.45F : 0.0F);
        float lowHealth = responseExponential(1.0F - Mth.clamp(mob.getHealth() / Math.max(1.0F, mob.getMaxHealth()), 0.0F, 1.0F), 1.35F);
        float alert = MycelialSblBridge.getHiveAlertLevel(mob);
        float traitBonus = mob.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT) ? 0.15F : 0.0F;
        float influencePressure = 0.0F;
        float tideOffset = 0.0F;
        if (mob.level() instanceof ServerLevel serverLevel) {
            var cell = HiveBlackboard.getInfluenceMap(serverLevel).sample(mob.blockPosition());
            influencePressure = 0.14F * cell.threat() * (1.0F - 0.5F * Math.max(0.0F, cell.control()));
            if (HiveMindSavedData.get(serverLevel).isFungalTideActive(serverLevel.getGameTime())) {
                tideOffset = -0.06F;
            }
        }
        StimulusType lastStimulus = MycelialSblBridge.getLastAttackStimulus(mob);
        float stimulusOffset = stimulusKiteOffset(lastStimulus);
        return Mth.clamp(0.50F * proximity + 0.30F * lowHealth + 0.15F * alert + traitBonus + influencePressure + tideOffset + stimulusOffset, 0.0F, 1.0F);
    }

    public static float evaluateRegroupUtility(MycelialMob mob) {
        float healthRatio = Mth.clamp(mob.getHealth() / Math.max(1.0F, mob.getMaxHealth()), 0.0F, 1.0F);
        BlockPos node = MycelialSblBridge.getNearestNodePos(mob);
        BlockPos pylon = MycelialSblBridge.getNearestPylonPos(mob);

        float score = 0.0F;
        if (healthRatio < 0.45F && node != null && mob.blockPosition().distSqr(node) > 36.0D) {
            score += (0.45F - healthRatio) * 1.6F + 0.25F;
        }
        if (pylon != null && mob.blockPosition().distSqr(pylon) < 400.0D && mob.getTarget() == null) {
            score += 0.70F;
        }
        if (MycelialSblBridge.getHiveOrder(mob) == com.alex.mycelialmind.hive.HiveOrder.RALLY_NODE && node != null) {
            score += 0.35F;
        }
        if (MycelialSblBridge.getNearbyAllyCount(mob) == 0 && healthRatio < 0.60F) {
            score += 0.20F;
        }
        if (mob.level() instanceof ServerLevel serverLevel) {
            var cell = HiveBlackboard.getInfluenceMap(serverLevel).sample(mob.blockPosition());
            score += 0.15F * cell.threat() + 0.10F * cell.threat() * (1.0F - cell.control());
            if (HiveMindSavedData.get(serverLevel).isFungalTideActive(serverLevel.getGameTime())) {
                score -= 0.08F;
            }
        }
        StimulusType lastStimulus = MycelialSblBridge.getLastAttackStimulus(mob);
        score += stimulusRegroupBonus(lastStimulus, MycelialSblBridge.getCombatXp(mob));
        return Mth.clamp(score, 0.0F, 1.0F);
    }
}
