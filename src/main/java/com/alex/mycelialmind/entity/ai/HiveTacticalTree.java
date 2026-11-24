package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.ai.HiveAiBlackboard.TacticalIntent;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBehaviorTree;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBtDynamicGuardSelector;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBtLeafTask;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBtStatus;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveDoctrine;
import net.minecraft.util.Mth;

/**
 * Clean-room reactive {@link HiveBehaviorTree} ("BT outside, utility inside") using
 * {@link HiveBtDynamicGuardSelector} for top-level preemption and Dave Mark's compensated Utility AI
 * enriched with Hive Doctrine, spatial trauma, and mob episodic memory.
 */
public final class HiveTacticalTree {
    private static final float HYSTERESIS_BONUS = 0.08F;

    private HiveTacticalTree() {
    }

    public static HiveBehaviorTree<HiveAiBlackboard> create(HiveAiBlackboard blackboard) {
        HiveBtDynamicGuardSelector<HiveAiBlackboard> root = new HiveBtDynamicGuardSelector<>();

        DesyncLeaf desync = new DesyncLeaf();
        desync.setGuard(bb -> bb.desynced);

        RegroupLeaf regroup = new RegroupLeaf();
        regroup.setGuard(bb -> {
            if (bb.frenzyActive) {
                return false;
            }
            float threshold = bb.activeIntent == TacticalIntent.REGROUP ? 0.45F : 0.65F;
            return scoreRegroup(bb) >= threshold;
        });

        UtilityCombatLeaf combat = new UtilityCombatLeaf();
        combat.setGuard(bb -> bb.hasTarget);

        PatrolLeaf patrol = new PatrolLeaf();
        patrol.setGuard(bb -> !bb.hasTarget && bb.hasSharedTarget);

        IdleLeaf idle = new IdleLeaf();

        root.addChild(desync);
        root.addChild(regroup);
        root.addChild(combat);
        root.addChild(patrol);
        root.addChild(idle);

        return new HiveBehaviorTree<>(root, blackboard);
    }

    public static float scoreRegroup(HiveAiBlackboard bb) {
        float score = 0.0F;
        if (bb.health01 < 0.45F && bb.hasNearestNode) {
            score += (0.45F - bb.health01) * 1.6F + 0.25F;
        }
        if (bb.nearPylon && !bb.hasTarget) {
            score += 0.70F;
        }
        if (bb.canRallyOrder && bb.hasNearestNode) {
            score += 0.35F;
        }
        if (bb.nearbyAllies == 0 && bb.health01 < 0.60F) {
            score += 0.20F;
        }
        score += 0.15F * bb.influenceThreat01 + 0.10F * bb.influenceThreat01 * (1.0F - bb.influenceControl01);
        if (bb.doctrine == HiveDoctrine.RECOVERY) {
            score += 0.18F;
        }
        score += 0.12F * Mth.clamp(bb.influenceTrauma01, 0.0F, 1.0F);
        if (bb.hasPylonTrauma && bb.nearPylon) {
            score += 0.10F;
        }
        if (bb.fungalTideActive) {
            score -= 0.08F;
        }
        return Mth.clamp(score, 0.0F, 1.0F);
    }

    public static float scoreCharge(HiveAiBlackboard bb) {
        if (!bb.hasTarget || bb.gameTime < bb.chargeCooldownUntil) {
            return 0.0F;
        }
        float healthFactor = HiveUtilityEvaluator.responseSigmoid(bb.health01, 6.0F, 0.35F);
        float distFactor = HiveUtilityEvaluator.inverseLerp01(bb.distToTarget, 24.0F, 4.0F);
        float pressureFactor = Mth.clamp(0.45F + 0.35F * bb.hiveAlert01 + 0.20F * bb.influencePheromone01, 0.0F, 1.0F);
        float raw = HiveUtilityEvaluator.compensate(healthFactor * distFactor * pressureFactor, 3);
        if (bb.frenzyActive) {
            raw = Mth.clamp(raw + 0.35F, 0.0F, 1.0F);
        }
        if (bb.doctrine == HiveDoctrine.SIEGE_ASSAULT) {
            raw = Mth.clamp(raw + 0.10F, 0.0F, 1.0F);
        }
        if (bb.hasTargetGrudge) {
            raw = Mth.clamp(raw + 0.08F, 0.0F, 1.0F);
        }
        if (bb.evolutionRank == EvolutionRank.ELITE) {
            raw = Mth.clamp(raw + 0.06F, 0.0F, 1.0F);
        }
        return HiveUtilityEvaluator.applyHysteresis(raw,
                bb.activeIntent == TacticalIntent.CHARGE || bb.activeIntent == TacticalIntent.FRENZY_CHARGE,
                HYSTERESIS_BONUS);
    }

    public static float scoreKite(HiveAiBlackboard bb) {
        if (!bb.hasTarget || bb.gameTime < bb.kiteCooldownUntil) {
            return 0.0F;
        }
        float proximityFactor = HiveUtilityEvaluator.inverseLerp01(bb.distToTarget, 12.0F, 2.0F);
        if (proximityFactor <= 0.0F) {
            return 0.0F;
        }
        float cautionFactor = Mth.clamp(
                (bb.isRangedOrKiter ? 0.55F : 0.25F)
                        + 0.30F * (1.0F - bb.health01)
                        + 0.15F * bb.influenceThreat01,
                0.0F,
                1.0F
        );
        float raw = HiveUtilityEvaluator.compensate(proximityFactor * cautionFactor, 2);
        if (bb.hasGrenadeTrauma || bb.influenceTrauma01 >= 0.55F) {
            raw = Mth.clamp(raw + 0.12F, 0.0F, 1.0F);
        }
        if (bb.fungalTideActive) {
            raw = Math.max(0.0F, raw - 0.06F);
        }
        return HiveUtilityEvaluator.applyHysteresis(raw, bb.activeIntent == TacticalIntent.KITE, HYSTERESIS_BONUS);
    }

    public static float scoreFlank(HiveAiBlackboard bb) {
        if (!bb.hasTarget) {
            return 0.0F;
        }
        float midRange = 1.0F - Math.abs(HiveUtilityEvaluator.inverseLerp01(bb.distToTarget, 2.0F, 14.0F) - 0.5F) * 1.4F;
        float rangeFactor = Mth.clamp(midRange, 0.15F, 1.0F);
        float swarmFactor = Mth.clamp(0.45F + 0.15F * Math.min(3, bb.nearbyAllies) + 0.15F * bb.influenceControl01, 0.0F, 1.0F);
        float raw = HiveUtilityEvaluator.compensate(rangeFactor * swarmFactor, 2);
        if (bb.doctrine == HiveDoctrine.ADAPTIVE_HUNT) {
            raw = Mth.clamp(raw + 0.10F, 0.0F, 1.0F);
        }
        if (bb.hasGrenadeTrauma) {
            raw = Mth.clamp(raw + 0.08F, 0.0F, 1.0F);
        }
        return HiveUtilityEvaluator.applyHysteresis(raw, bb.activeIntent == TacticalIntent.FLANK, HYSTERESIS_BONUS);
    }

    private static final class RegroupLeaf extends HiveBtLeafTask<HiveAiBlackboard> {
        @Override
        protected HiveBtStatus run(HiveAiBlackboard bb) {
            bb.activeIntent = TacticalIntent.REGROUP;
            bb.runningTicks++;
            return HiveBtStatus.RUNNING;
        }

        @Override
        protected void end(HiveAiBlackboard bb) {
            if (bb != null) {
                bb.runningTicks = 0;
                bb.resetCount++;
            }
        }
    }

    private static final class DesyncLeaf extends HiveBtLeafTask<HiveAiBlackboard> {
        @Override
        protected HiveBtStatus run(HiveAiBlackboard bb) {
            bb.activeIntent = TacticalIntent.DESYNC_SCATTER;
            bb.runningTicks++;
            return HiveBtStatus.RUNNING;
        }

        @Override
        protected void end(HiveAiBlackboard bb) {
            if (bb != null) {
                bb.runningTicks = 0;
                bb.resetCount++;
            }
        }
    }

    private static final class UtilityCombatLeaf extends HiveBtLeafTask<HiveAiBlackboard> {
        @Override
        protected HiveBtStatus run(HiveAiBlackboard bb) {
            float charge = scoreCharge(bb);
            float kite = scoreKite(bb);
            float flank = scoreFlank(bb);

            if (bb.frenzyActive && charge > 0.0F) {
                bb.activeIntent = TacticalIntent.FRENZY_CHARGE;
            } else if (bb.phalanxActive && bb.distToTarget > 3.0F) {
                bb.activeIntent = TacticalIntent.PHALANX_GUARD;
            } else if (kite >= charge && kite >= flank && kite > 0.0F) {
                bb.activeIntent = TacticalIntent.KITE;
            } else if (charge >= flank && charge > 0.0F) {
                bb.activeIntent = TacticalIntent.CHARGE;
            } else if (flank > 0.0F) {
                bb.activeIntent = TacticalIntent.FLANK;
            } else {
                bb.activeIntent = TacticalIntent.NONE;
                return HiveBtStatus.FAILED;
            }
            bb.runningTicks++;
            return HiveBtStatus.RUNNING;
        }

        @Override
        protected void end(HiveAiBlackboard bb) {
            if (bb != null) {
                bb.runningTicks = 0;
                bb.resetCount++;
            }
        }
    }

    private static final class PatrolLeaf extends HiveBtLeafTask<HiveAiBlackboard> {
        @Override
        protected HiveBtStatus run(HiveAiBlackboard bb) {
            bb.activeIntent = TacticalIntent.PATROL;
            bb.runningTicks++;
            return HiveBtStatus.RUNNING;
        }

        @Override
        protected void end(HiveAiBlackboard bb) {
            if (bb != null) {
                bb.runningTicks = 0;
                bb.resetCount++;
            }
        }
    }

    private static final class IdleLeaf extends HiveBtLeafTask<HiveAiBlackboard> {
        @Override
        protected HiveBtStatus run(HiveAiBlackboard bb) {
            bb.activeIntent = TacticalIntent.NONE;
            return HiveBtStatus.SUCCEEDED;
        }
    }
}
