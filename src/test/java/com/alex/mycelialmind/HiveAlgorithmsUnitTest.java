package com.alex.mycelialmind;

import com.alex.mycelialmind.block.MyceliumSpreadPolicy;
import com.alex.mycelialmind.entity.ai.HiveAiBlackboard;
import com.alex.mycelialmind.entity.ai.HiveTacticalTree;
import com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveInfluenceMap;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.util.ParticleEmissionBudget;
import com.alex.mycelialmind.world.MycelialFastNoise;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HiveAlgorithmsUnitTest {
    @Test
    void particleEmissionBudgetsClampAndScaleMonotonically() {
        assertEquals(0, ParticleEmissionBudget.clamp(-8));
        assertEquals(0, ParticleEmissionBudget.clamp(0));
        assertEquals(12, ParticleEmissionBudget.clamp(12));
        assertEquals(24, ParticleEmissionBudget.clamp(99));
        assertEquals(12, ParticleEmissionBudget.scaled(12, 1.0));
        assertEquals(6, ParticleEmissionBudget.scaled(12, 0.5));
        assertEquals(18, ParticleEmissionBudget.scaled(12, 1.5));
        assertEquals(24, ParticleEmissionBudget.scaled(24, 10.0));
        assertEquals(12, ParticleEmissionBudget.scaled(12, Double.NaN));
        assertEquals(0, ParticleEmissionBudget.composition(-4, 0, 0));
        assertEquals(48, ParticleEmissionBudget.composition(24, 24, 24));
        assertEquals(30, ParticleEmissionBudget.composition(10, 20, 0));
    }

    @Test
    void myceliumSpreadChanceTriplesAttemptsForTheSameTickRolls() {
        int attemptsAtPreviousChance = 0;
        int attemptsAtNewDefault = 0;
        for (int i = 0; i < 10_000; i++) {
            double roll = i / 10_000.0;
            if (MyceliumSpreadPolicy.shouldAttempt(roll, 0.12)) {
                attemptsAtPreviousChance++;
            }
            if (MyceliumSpreadPolicy.shouldAttempt(roll, MyceliumSpreadPolicy.DEFAULT_CHANCE)) {
                attemptsAtNewDefault++;
            }
        }

        assertEquals(1_200, attemptsAtPreviousChance);
        assertEquals(3_600, attemptsAtNewDefault);
        assertEquals(3.0, (double) attemptsAtNewDefault / attemptsAtPreviousChance, 1.0E-9);
    }

    @Test
    void influenceMapPropagatesSamplesAndPrunesOnDecay() {
        HiveInfluenceMap map = new HiveInfluenceMap();
        BlockPos origin = new BlockPos(8, 64, 8);
        map.addInfluence(origin, 0.5F, 0.7F, 1.0F);

        var center = map.sample(origin);
        assertEquals(0.5F, center.control(), 0.01F);
        assertEquals(0.7F, center.threat(), 0.01F);
        assertEquals(1.0F, center.pheromone(), 0.01F);

        var east = map.sample(origin.offset(16, 0, 0));
        assertTrue(east.pheromone() > 0.3F);

        var grad = map.samplePheromoneGradient(origin.offset(-16, 0, 0));
        assertTrue(grad.x > 0.5D);

        for (int i = 0; i < 60; i++) {
            map.stepDecay(0.85F);
        }
        assertEquals(0, map.cellCount());
    }

    @Test
    void utilityResponseCurvesAreMonotonicAndBounded() {
        float low = HiveUtilityEvaluator.responseSigmoid(0.1F, 6.0F, 0.5F);
        float mid = HiveUtilityEvaluator.responseSigmoid(0.5F, 6.0F, 0.5F);
        float high = HiveUtilityEvaluator.responseSigmoid(0.9F, 6.0F, 0.5F);
        assertTrue(low < mid && mid < high);
        assertTrue(low < 0.2F && high > 0.8F);
        assertEquals(0.5F, mid, 0.01F);

        assertEquals(0.25F, HiveUtilityEvaluator.responseExponential(0.5F, 2.0F), 0.001F);
        assertEquals(0.0F, HiveUtilityEvaluator.responseExponential(-1.0F, 2.0F), 0.001F);
        assertEquals(1.0F, HiveUtilityEvaluator.responseExponential(2.0F, 2.0F), 0.001F);
    }

    @Test
    void fastNoiseLiteProducesDeterministicBoundedCellularSamples() {
        var s1 = MycelialFastNoise.domainWarpedCellular2D(14.5D, -9.25D, 98765L, 0.35D);
        var s2 = MycelialFastNoise.domainWarpedCellular2D(14.5D, -9.25D, 98765L, 0.35D);
        assertEquals(s1.f1(), s2.f1(), 1.0E-9D);
        assertEquals(s1.edge(), s2.edge(), 1.0E-9D);
        assertTrue(s1.f1() >= 0.0D && s1.f1() <= 1.0D);
        assertTrue(s1.edge() >= 0.0D && s1.edge() <= 1.0D);
    }

    @Test
    void wfcCavernLayoutGeneratesDeterministicValidTiles() {
        var g1 = com.alex.mycelialmind.world.WfcCavernLayout.generate(6, 6, 42L);
        var g2 = com.alex.mycelialmind.world.WfcCavernLayout.generate(6, 6, 42L);
        assertEquals(6, g1.length);
        assertEquals(6, g1[0].length);
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                assertNotNull(g1[y][x]);
                assertEquals(g1[y][x], g2[y][x]);
            }
        }
    }

    @Test
    void influenceMapErodesControlAndClampsThreatPressure() {
        HiveInfluenceMap map = new HiveInfluenceMap();
        BlockPos dronePos = new BlockPos(24, 64, 24);
        map.addInfluence(dronePos, 0.80F, 0.0F, 0.0F);
        for (int i = 0; i < 6; i++) {
            map.addInfluence(dronePos, -0.30F, 0.35F, 0.0F);
        }
        var cell = map.sample(dronePos);
        assertEquals(0.0F, cell.control(), 0.001F);
        assertTrue(cell.threat() > 0.8F && cell.threat() <= 1.0F);
    }

    @Test
    void compensatedUtilityVetoesZeroAndAppliesHysteresisOnlyToPositiveScores() {
        assertEquals(0.0F, HiveUtilityEvaluator.compensate(0.0F, 3), 0.0001F);
        float raw = 0.8F * 0.8F * 0.8F;
        float compensated = HiveUtilityEvaluator.compensate(raw, 3);
        assertTrue(compensated > raw && compensated <= 1.0F);

        assertEquals(0.0F, HiveUtilityEvaluator.applyHysteresis(0.0F, true, 0.08F), 0.0001F);
        assertEquals(0.58F, HiveUtilityEvaluator.applyHysteresis(0.50F, true, 0.08F), 0.0001F);
        assertEquals(0.50F, HiveUtilityEvaluator.applyHysteresis(0.50F, false, 0.08F), 0.0001F);
    }

    @Test
    void behaviorTreeReactivePreemptionResetsAndPreservesCooldowns() {
        HiveAiBlackboard bb = new HiveAiBlackboard();
        BehaviorTree<HiveAiBlackboard> tree = HiveTacticalTree.create(bb);

        bb.hasTarget = true;
        bb.distToTarget = 6.0F;
        bb.health01 = 0.90F;
        bb.hiveAlert01 = 0.80F;
        bb.gameTime = 100L;
        bb.chargeCooldownUntil = 500L;
        tree.step();

        assertTrue(bb.activeIntent == HiveAiBlackboard.TacticalIntent.FLANK
                || bb.activeIntent == HiveAiBlackboard.TacticalIntent.KITE);
        assertEquals(0.0F, HiveTacticalTree.scoreCharge(bb), 0.0001F);
        assertEquals(1, bb.runningTicks);

        // Losing target mid-action must preempt Combat branch and reset its running counter
        bb.hasTarget = false;
        bb.hasSharedTarget = true;
        tree.step();
        assertEquals(HiveAiBlackboard.TacticalIntent.PATROL, bb.activeIntent);
        assertTrue(bb.resetCount >= 1);
        assertEquals(500L, bb.chargeCooldownUntil);

        // Low health near a node must preempt Patrol into Regroup
        bb.health01 = 0.20F;
        bb.hasNearestNode = true;
        tree.step();
        assertEquals(HiveAiBlackboard.TacticalIntent.REGROUP, bb.activeIntent);
    }

    @Test
    void fastNoiseOrganicFlowIsDeterministicAndBounded() {
        var f1 = MycelialFastNoise.organicFlow2D(18.5D, -12.25D, 4242L, 3.5D);
        var f2 = MycelialFastNoise.organicFlow2D(18.5D, -12.25D, 4242L, 3.5D);
        assertEquals(f1.x, f2.x, 1.0E-9D);
        assertEquals(f1.z, f2.z, 1.0E-9D);
        assertTrue(Math.abs(f1.x) <= 1.0D && Math.abs(f1.z) <= 1.0D);

        double affinity = MycelialFastNoise.foragingAffinity(18.5D, -12.25D, 4242L);
        assertTrue(affinity >= 0.0D && affinity <= 1.0D);
    }

    @Test
    void tacticalTreeKiteScoringHysteresisAndFungalTideModulation() {
        HiveAiBlackboard bb = new HiveAiBlackboard();
        BehaviorTree<HiveAiBlackboard> tree = HiveTacticalTree.create(bb);

        bb.hasTarget = true;
        bb.isRangedOrKiter = true;
        bb.distToTarget = 3.0F;
        bb.health01 = 0.35F;
        bb.influenceThreat01 = 0.80F;
        bb.gameTime = 200L;
        tree.step();
        assertEquals(HiveAiBlackboard.TacticalIntent.KITE, bb.activeIntent);

        float kiteNormal = HiveTacticalTree.scoreKite(bb);
        float regroupNormal = HiveTacticalTree.scoreRegroup(bb);
        bb.fungalTideActive = true;
        float kiteTide = HiveTacticalTree.scoreKite(bb);
        float regroupTide = HiveTacticalTree.scoreRegroup(bb);
        assertTrue(kiteTide < kiteNormal);
        assertTrue(regroupTide < regroupNormal);

        // Cooldown must veto KITE and hysteresis must NOT revive it
        bb.kiteCooldownUntil = 500L;
        assertEquals(0.0F, HiveTacticalTree.scoreKite(bb), 0.0001F);

        // Hysteresis bonus lifts active CHARGE above equal raw score
        bb.activeIntent = HiveAiBlackboard.TacticalIntent.NONE;
        float chargeWithoutHysteresis = HiveTacticalTree.scoreCharge(bb);
        bb.activeIntent = HiveAiBlackboard.TacticalIntent.CHARGE;
        float chargeWithHysteresis = HiveTacticalTree.scoreCharge(bb);
        if (chargeWithoutHysteresis > 0.0F) {
            assertTrue(chargeWithHysteresis > chargeWithoutHysteresis);
        }
    }

    @Test
    void fastNoiseOrganicFlowVariesAcrossSpaceAndTime() {
        var f0 = MycelialFastNoise.organicFlow2D(0.0D, 0.0D, 777L, 0.0D);
        var fSpace = MycelialFastNoise.organicFlow2D(42.5D, -31.25D, 777L, 0.0D);
        var fTime = MycelialFastNoise.organicFlow2D(0.0D, 0.0D, 777L, 12.0D);
        assertTrue(Math.abs(f0.x - fSpace.x) > 1.0E-4D || Math.abs(f0.z - fSpace.z) > 1.0E-4D);
        assertTrue(Math.abs(f0.x - fTime.x) > 1.0E-4D || Math.abs(f0.z - fTime.z) > 1.0E-4D);
    }

    @Test
    void synapticCommandGeometryAndBallisticLeadAreBounded() {
        assertTrue(SynapticCommandSystem.leaderPriority(true, false, EvolutionRank.BASE, false)
                > SynapticCommandSystem.leaderPriority(false, true, EvolutionRank.ELITE, false));
        assertTrue(SynapticCommandSystem.leaderPriority(false, true, EvolutionRank.BASE, false)
                > SynapticCommandSystem.leaderPriority(false, false, EvolutionRank.ELITE, false));
        assertEquals(-1, SynapticCommandSystem.leaderPriority(false, false, EvolutionRank.ELITE, true));

        Vec3 shooter = new Vec3(0.0D, 64.0D, 0.0D);
        Vec3 target = new Vec3(10.0D, 64.0D, 0.0D);
        Vec3 vel = new Vec3(0.0D, 0.0D, 0.30D);
        Vec3 zeroLead = SynapticCommandSystem.computeBallisticLeadPos(shooter, target, vel, 1.35D, 0.0D);
        assertEquals(target.z, zeroLead.z, 1.0E-5D);

        Vec3 syncedLead = SynapticCommandSystem.computeBallisticLeadPos(
                new Vec3(0.0D, 64.0D, 0.0D),
                new Vec3(40.0D, 64.0D, 0.0D),
                vel,
                1.35D,
                SynapticCommandSystem.MAX_SYNCED_LEAD_SECONDS
        );
        // Capped at 0.75s * 20 ticks = 15 ticks -> 15 * 0.30 = 4.50
        assertEquals(4.50D, syncedLead.z, 0.05D);

        Vec3 phalanxPos = SynapticCommandSystem.computePhalanxGuardPos(new Vec3(0.0D, 64.0D, 0.0D), target, 3.0D);
        assertEquals(3.0D, phalanxPos.x, 0.05D);

        Vec3 lookEast = new Vec3(1.0D, 0.0D, 0.0D);
        assertTrue(SynapticCommandSystem.isOutsideTargetFov(target, lookEast, new Vec3(5.0D, 64.0D, 0.0D), 0.15D));
        assertFalse(SynapticCommandSystem.isOutsideTargetFov(target, lookEast, new Vec3(15.0D, 64.0D, 0.0D), 0.15D));
        assertTrue(SynapticCommandSystem.isPhalanxFrontHit(lookEast, new Vec3(15.0D, 64.0D, 0.0D), target));
        assertFalse(SynapticCommandSystem.isPhalanxFrontHit(lookEast, new Vec3(5.0D, 64.0D, 0.0D), target));

        Vec3 cutoff = SynapticCommandSystem.computeRetreatCutoffPos(
                target, new Vec3(0.25D, 0.0D, 0.0D), new Vec3(30.0D, 64.0D, 0.0D), 4.0D);
        assertTrue(cutoff.x > target.x && cutoff.x < 30.0D);
    }

    @Test
    void tacticalTreePrioritizesDesyncOverFrenzyAndRegroup() {
        HiveAiBlackboard bb = new HiveAiBlackboard();
        BehaviorTree<HiveAiBlackboard> tree = HiveTacticalTree.create(bb);
        bb.hasTarget = true;
        bb.distToTarget = 10.0F;
        bb.health01 = 0.25F;
        bb.hasNearestNode = true;
        bb.frenzyActive = true;
        bb.gameTime = 100L;
        tree.step();
        assertEquals(HiveAiBlackboard.TacticalIntent.FRENZY_CHARGE, bb.activeIntent);

        bb.desynced = true;
        bb.frenzyActive = false;
        tree.step();
        assertEquals(HiveAiBlackboard.TacticalIntent.DESYNC_SCATTER, bb.activeIntent);
    }

    @Test
    void hiveProgressionTierScalesMonotonicallyWithThreat() {
        var dormant = com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(0.0);
        var awakened = com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(25.0);
        var adaptive = com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(50.0);
        var overmind = com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(85.0);

        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, dormant);
        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED, awakened);
        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, adaptive);
        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND, overmind);

        assertTrue(awakened.healthMultiplier() > dormant.healthMultiplier());
        assertTrue(adaptive.healthMultiplier() > awakened.healthMultiplier());
        assertTrue(overmind.healthMultiplier() > adaptive.healthMultiplier());
        assertTrue(overmind.damageMultiplier() > adaptive.damageMultiplier());
        assertTrue(overmind.armorBonus() > adaptive.armorBonus());
        assertTrue(overmind.wavePromotionBonus() > adaptive.wavePromotionBonus());
    }

    @Test
    void structureProgressionPolicyScalesRaritySpacingAndLootAcrossTiers() {
        var dormant = com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT;
        var awakened = com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED;
        var adaptive = com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE;
        var overmind = com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND;

        // Lower 1-in-N denominator = higher spawn density as Hive awakens
        assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(dormant)
                > com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(awakened));
        assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(awakened)
                > com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(adaptive));
        assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(adaptive)
                > com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(overmind));

        assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveRarityChance(dormant)
                > com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveRarityChance(overmind));
        assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakMinSpacing(dormant)
                > com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakMinSpacing(overmind));
        assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveMinSpacing(dormant)
                > com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveMinSpacing(overmind));
        assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveNodeClearance(dormant)
                > com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveNodeClearance(overmind));

        assertEquals(com.alex.mycelialmind.world.SynapticNodeType.RELAY,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.pickOutbreakNodeType(dormant, 0.30F));
        assertEquals(com.alex.mycelialmind.world.SynapticNodeType.SENTINEL,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.pickOutbreakNodeType(overmind, 0.30F));

        assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.chestBonusCount(overmind)
                > com.alex.mycelialmind.world.structure.StructureProgressionPolicy.chestBonusCount(dormant));
        assertEquals(com.alex.mycelialmind.hive.EvolutionRank.BASE,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.citadelGuardianRank(dormant));
        assertEquals(com.alex.mycelialmind.hive.EvolutionRank.MUTATED,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.citadelGuardianRank(adaptive));
        assertEquals(com.alex.mycelialmind.hive.EvolutionRank.ELITE,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.citadelGuardianRank(overmind));
    }

    @Test
    void traumaInfluenceDecaysSlowerAndGuidesDetour() {
        HiveInfluenceMap map = new HiveInfluenceMap();
        BlockPos origin = new BlockPos(8, 64, 8);
        map.addInfluence(origin, 0.0F, 0.80F, 0.0F);
        map.addTrauma(origin, 0.80F);
        assertTrue(map.sample(origin).trauma() >= 0.79F);

        Vec3 escapeFromEast = map.sampleTraumaEscapeVector(origin.offset(16, 0, 0));
        assertTrue(escapeFromEast.x > 0.0D);

        assertTrue(com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldAvoidTraumaZone(0.70F, false));
        assertFalse(com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldAvoidTraumaZone(0.70F, true));
        Vec3 detour = com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeTraumaDetourVector(
                new Vec3(5.0D, 0.0D, 0.0D), escapeFromEast, 0.70F);
        assertTrue(detour.lengthSqr() > 0.1D);

        for (int i = 0; i < 6; i++) {
            map.stepDecay(0.85F);
        }
        var decayed = map.sample(origin);
        assertTrue(decayed.trauma() > decayed.threat());
    }

    @Test
    void livingBattlefieldAntiPillarAndCarrionRules() {
        var steps = com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeAntiPillarStepPositions(
                new BlockPos(0, 64, 0), new BlockPos(2, 67, 0));
        assertEquals(2, steps.size());
        assertEquals(64, steps.get(0).getY());
        assertEquals(65, steps.get(1).getY());

        assertTrue(com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeAntiPillarStepPositions(
                new BlockPos(0, 64, 0), new BlockPos(2, 65, 0)).isEmpty());

        assertTrue(com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldDropCarrion(false, false, false));
        assertFalse(com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldDropCarrion(true, false, false));
        assertFalse(com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldDropCarrion(false, true, false));
        assertFalse(com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldDropCarrion(false, false, true));
    }

    @Test
    void adaptiveMobLootScalesWithRankStrainTraitsAndLooting() {
        var baseDrops = new java.util.ArrayList<com.alex.mycelialmind.world.MycelialLootSystem.AdaptiveDropSpec>();
        var eliteDrops = new java.util.ArrayList<com.alex.mycelialmind.world.MycelialLootSystem.AdaptiveDropSpec>();
        var crimsonDrops = new java.util.ArrayList<com.alex.mycelialmind.world.MycelialLootSystem.AdaptiveDropSpec>();

        var baseRng = net.minecraft.util.RandomSource.create(42L);
        var eliteRng = net.minecraft.util.RandomSource.create(42L);
        var crimsonRng = net.minecraft.util.RandomSource.create(42L);

        for (int i = 0; i < 120; i++) {
            baseDrops.addAll(com.alex.mycelialmind.world.MycelialLootSystem.computeAdaptiveMobDropSpecs(
                    EvolutionRank.BASE,
                    com.alex.mycelialmind.hive.HiveStrain.ORTHODOX,
                    com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT,
                    java.util.Set.of(),
                    false,
                    0,
                    baseRng
            ));
            eliteDrops.addAll(com.alex.mycelialmind.world.MycelialLootSystem.computeAdaptiveMobDropSpecs(
                    EvolutionRank.ELITE,
                    com.alex.mycelialmind.hive.HiveStrain.AMBER,
                    com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND,
                    java.util.Set.of(com.alex.mycelialmind.hive.AdaptationTrait.FIRE_RESISTANT),
                    true,
                    3,
                    eliteRng
            ));
            crimsonDrops.addAll(com.alex.mycelialmind.world.MycelialLootSystem.computeAdaptiveMobDropSpecs(
                    EvolutionRank.MUTATED,
                    com.alex.mycelialmind.hive.HiveStrain.CRIMSON,
                    com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE,
                    java.util.Set.of(com.alex.mycelialmind.hive.AdaptationTrait.MELEE_THORNS),
                    false,
                    2,
                    crimsonRng
            ));
        }

        assertTrue(eliteDrops.size() > baseDrops.size());
        assertTrue(eliteDrops.stream().anyMatch(s ->
                "mycelialmind:genetic_sample".equals(s.itemId())
                        && s.sampleTrait() == com.alex.mycelialmind.hive.AdaptationTrait.FIRE_RESISTANT));
        assertTrue(eliteDrops.stream().anyMatch(s ->
                "mycelialmind:luminescent_amber_vein".equals(s.itemId())
                        || "mycelialmind:truce_pheromone".equals(s.itemId())));
        assertTrue(crimsonDrops.stream().anyMatch(s ->
                "mycelialmind:spore_antidote".equals(s.itemId())
                        || "mycelialmind:volatile_engram".equals(s.itemId())));
        assertTrue(eliteDrops.stream().anyMatch(s -> s.armorRank() == EvolutionRank.MUTATED));

        // Same seed produces identical deterministic result
        var det1 = com.alex.mycelialmind.world.MycelialLootSystem.computeAdaptiveMobDropSpecs(
                EvolutionRank.ELITE,
                com.alex.mycelialmind.hive.HiveStrain.AMBER,
                com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND,
                java.util.Set.of(com.alex.mycelialmind.hive.AdaptationTrait.FIRE_RESISTANT),
                true,
                3,
                net.minecraft.util.RandomSource.create(77L)
        );
        var det2 = com.alex.mycelialmind.world.MycelialLootSystem.computeAdaptiveMobDropSpecs(
                EvolutionRank.ELITE,
                com.alex.mycelialmind.hive.HiveStrain.AMBER,
                com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND,
                java.util.Set.of(com.alex.mycelialmind.hive.AdaptationTrait.FIRE_RESISTANT),
                true,
                3,
                net.minecraft.util.RandomSource.create(77L)
        );
        assertEquals(det1, det2);
    }

    @Test
    void dimensionalHomeostasisStressDecayAcidAndFogMath() {
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.STABLE,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.stageForStress(15.0));
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IRRITATED,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.stageForStress(40.0));
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.stageForStress(70.0));

        double expStress = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeExplosionStress(24, 4.0F, 1.0);
        assertTrue(expStress >= 35.0 && expStress <= 55.0);

        double excessPylonDelta = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(2, 0, 0);
        double calmDecay = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(1, 2, 0);
        double t2CalmDecay = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(1, 2, 2, 0);
        double t3CalmDecay = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(1, 3, 2, 0);
        double t3ExcessDelta = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(2, 3, 0, 0);
        double symbioticDecay = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(1, 2, 2);
        assertTrue(excessPylonDelta > 0.0);
        assertTrue(t3ExcessDelta > 0.0 && t3ExcessDelta < excessPylonDelta);
        assertTrue(calmDecay < 0.0);
        assertTrue(t3CalmDecay < t2CalmDecay && t2CalmDecay < calmDecay);
        assertTrue(symbioticDecay < calmDecay);

        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId(
                "mycelialmind", "symbiotic_node", false, false, false));
        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId(
                "mycelialmind", "mycelial_stone", false, false, false));
        assertTrue(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId(
                "mycelialmind", "purification_pylon", false, false, false));
        assertTrue(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId(
                "minecraft", "cobblestone", false, false, false));

        assertTrue(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isNaturalCavernCeilingId(
                "mycelialmind", "mycelial_stone"));
        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isNaturalCavernCeilingId(
                "mycelialmind", "reinforced_bio_barrier"));
        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isNaturalCavernCeilingId(
                "minecraft", "cobblestone"));

        float unarmoredAcid = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(2.0F, 0, false);
        float partialAcid = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(2.0F, 2, false);
        float fullChitinAcid = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(2.0F, 4, true);
        assertTrue(unarmoredAcid > partialAcid && partialAcid > fullChitinAcid);

        assertNull(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.STABLE, 10.0, false, false));
        var immuneFog = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE, 85.0, false, false);
        var thermalFog = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE, 85.0, true, false);
        assertNotNull(immuneFog);
        assertNotNull(thermalFog);
        assertTrue(immuneFog.farPlane() <= 8.0F);
        assertTrue(thermalFog.farPlane() >= 30.0F);
    }

    @Test
    void stimulusAndHabitThresholdsAlignWithDoctrineAndPacingEvaluation() {
        double n1 = com.alex.mycelialmind.config.MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.getDefault();
        double n2 = com.alex.mycelialmind.config.MycelialConfig.TIER2_ACTIVATION_THRESHOLD.getDefault();
        double habit = com.alex.mycelialmind.hive.HiveMindSavedData.HABIT_ACTIVATION_THRESHOLD;

        assertEquals(70.0, n1, 1.0E-6);
        assertEquals(200.0, n2, 1.0E-6);
        assertEquals(60.0, habit, 1.0E-6);
        assertEquals(0.35F, HiveUtilityEvaluator.inverseLerp01((float) n1, 0.0F, (float) n2), 1.0E-5F);
        assertEquals(1.0F, HiveUtilityEvaluator.inverseLerp01((float) n2, 0.0F, (float) n2), 1.0E-5F);

        var hive = new com.alex.mycelialmind.hive.HiveMindSavedData();
        assertEquals(com.alex.mycelialmind.hive.HiveDoctrine.DORMANT,
                com.alex.mycelialmind.hive.HiveDoctrine.evaluate(hive, 100L, false));

        hive.addThreatPenalty(20.0);
        assertEquals(com.alex.mycelialmind.hive.HiveDoctrine.FORAGING,
                com.alex.mycelialmind.hive.HiveDoctrine.evaluate(hive, 100L, false));
        assertEquals(com.alex.mycelialmind.hive.HiveDoctrine.RECOVERY,
                com.alex.mycelialmind.hive.HiveDoctrine.evaluate(hive, 100L, true));

        hive.addThreatPenalty(25.0);
        assertEquals(com.alex.mycelialmind.hive.HiveDoctrine.ADAPTIVE_HUNT,
                com.alex.mycelialmind.hive.HiveDoctrine.evaluate(hive, 100L, false));

        hive.triggerFungalTide(100L, 400);
        assertEquals(com.alex.mycelialmind.hive.HiveDoctrine.SIEGE_ASSAULT,
                com.alex.mycelialmind.hive.HiveDoctrine.evaluate(hive, 150L, false));

        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT,
                com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(24.9));
        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED,
                com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(25.0));
        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE,
                com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(50.0));
        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND,
                com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(75.0));
    }

    @Test
    void influenceMapBoundaryDiffusionTraumaEscapeAndClearWork() {
        HiveInfluenceMap map = new HiveInfluenceMap();
        BlockPos origin = new BlockPos(0, 64, 0);
        map.addInfluence(origin, 0.0F, 0.0F, 0.0F);
        assertEquals(1, map.cellCount());

        map.addTrauma(origin, 0.0F);
        map.addTrauma(origin, -0.5F);
        assertEquals(0.0F, map.sample(origin).trauma(), 1.0E-5F);

        // Symmetric trauma plateau returns fallback (0, 0, 1) when >= 0.55F, or ZERO when < 0.55F
        BlockPos plateau = new BlockPos(160, 64, 160);
        map.addTrauma(plateau, 0.80F);
        Vec3 plateauEscape = map.sampleTraumaEscapeVector(plateau);
        assertEquals(0.0D, plateauEscape.x, 1.0E-5D);
        assertEquals(1.0D, plateauEscape.z, 1.0E-5D);

        BlockPos lowTrauma = new BlockPos(480, 64, 480);
        map.addTrauma(lowTrauma, 0.40F);
        assertEquals(Vec3.ZERO, map.sampleTraumaEscapeVector(lowTrauma));

        // Pheromone + threat*0.75F gradient along Z and zero-gradient fallback
        HiveInfluenceMap gradMap = new HiveInfluenceMap();
        gradMap.addInfluence(new BlockPos(0, 64, 16), 0.0F, 0.8F, 0.2F);
        Vec3 zGrad = gradMap.samplePheromoneGradient(new BlockPos(0, 64, 0));
        assertTrue(zGrad.z > 0.5D);
        assertEquals(Vec3.ZERO, gradMap.samplePheromoneGradient(new BlockPos(640, 64, 640)));

        // stepDecay clamps factor < 0 to 0 while trauma + 0.04F decays until pruned
        gradMap.addTrauma(new BlockPos(320, 64, 320), 0.90F);
        gradMap.stepDecay(-1.0F);
        assertTrue(gradMap.sample(new BlockPos(320, 64, 320)).trauma() > 0.01F);
        gradMap.stepDecay(0.0F);
        gradMap.stepDecay(0.0F);
        assertEquals(0, gradMap.cellCount());

        // MAX_CELLS cap and clear()
        for (int i = 0; i < HiveInfluenceMap.MAX_CELLS + 10; i++) {
            map.addTrauma(new BlockPos(i * 16, 64, 1024), 0.5F);
        }
        assertEquals(HiveInfluenceMap.MAX_CELLS, map.cellCount());
        map.clear();
        assertEquals(0, map.cellCount());
    }

    @Test
    void utilityEvaluatorAndFastNoiseEdgeBranchesAreDeterministic() {
        assertEquals(0.0F, HiveUtilityEvaluator.compensate(0.0F, 3), 1.0E-5F);
        assertEquals(0.0F, HiveUtilityEvaluator.compensate(-0.5F, 3), 1.0E-5F);
        assertEquals(0.5F, HiveUtilityEvaluator.compensate(0.5F, 1), 1.0E-5F);
        assertEquals(1.0F, HiveUtilityEvaluator.compensate(1.5F, 1), 1.0E-5F);
        assertEquals(0.6875F, HiveUtilityEvaluator.compensate(0.5F, 4), 1.0E-4F);

        assertEquals(0.0F, HiveUtilityEvaluator.inverseLerp01(5.0F, 10.0F, 10.0F), 1.0E-5F);
        assertEquals(0.0F, HiveUtilityEvaluator.inverseLerp01(2.0F, 10.0F, 20.0F), 1.0E-5F);
        assertEquals(1.0F, HiveUtilityEvaluator.inverseLerp01(25.0F, 10.0F, 20.0F), 1.0E-5F);

        assertTrue(HiveUtilityEvaluator.responseExponential(0.25F, 0.01F) > 0.85F);
        assertEquals(0.0F, HiveUtilityEvaluator.applyHysteresis(0.0F, true, 0.4F), 1.0E-5F);
        assertEquals(0.4F, HiveUtilityEvaluator.applyHysteresis(0.4F, false, 0.3F), 1.0E-5F);
        assertEquals(0.4F, HiveUtilityEvaluator.applyHysteresis(0.4F, true, -0.3F), 1.0E-5F);
        assertEquals(1.0F, HiveUtilityEvaluator.applyHysteresis(0.9F, true, 0.5F), 1.0E-5F);

        var cBase = MycelialFastNoise.cellular2D(12.5D, -8.25D, 12345L);
        var cZeroWarp = MycelialFastNoise.domainWarpedCellular2D(12.5D, -8.25D, 12345L, 0.0D);
        var cWarped = MycelialFastNoise.domainWarpedCellular2D(12.5D, -8.25D, 12345L, 2.5D);
        var cOtherSeed = MycelialFastNoise.cellular2D(12.5D, -8.25D, 67890L);
        assertEquals(cBase, cZeroWarp);
        assertNotEquals(cBase, cWarped);
        assertNotEquals(cBase, cOtherSeed);

        double f1 = MycelialFastNoise.foragingAffinity(10.0D, 20.0D, 42L);
        double f2 = MycelialFastNoise.foragingAffinity(26.0D, 44.0D, 42L);
        assertTrue(f1 >= 0.0D && f1 <= 1.0D && f2 >= 0.0D && f2 <= 1.0D);
        assertNotEquals(f1, f2);
    }

    @Test
    void clientHudPositionAndSporeFogScalingRespectBounds() {
        var topLeft = com.alex.mycelialmind.config.MycelialClientConfig.computeHudPosition(
                com.alex.mycelialmind.config.MycelialClientConfig.HudAnchor.TOP_LEFT, 480, 270, 204, 100, 0, 0);
        assertArrayEquals(new int[]{10, 10}, topLeft);

        var topRight = com.alex.mycelialmind.config.MycelialClientConfig.computeHudPosition(
                com.alex.mycelialmind.config.MycelialClientConfig.HudAnchor.TOP_RIGHT, 480, 270, 204, 100, 0, 0);
        assertArrayEquals(new int[]{480 - 204 - 10, 10}, topRight);

        var bottomLeft = com.alex.mycelialmind.config.MycelialClientConfig.computeHudPosition(
                com.alex.mycelialmind.config.MycelialClientConfig.HudAnchor.BOTTOM_LEFT, 480, 270, 204, 100, -50, 50);
        assertArrayEquals(new int[]{2, 270 - 100 - 2}, bottomLeft);

        var bottomRight = com.alex.mycelialmind.config.MycelialClientConfig.computeHudPosition(
                com.alex.mycelialmind.config.MycelialClientConfig.HudAnchor.BOTTOM_RIGHT, 480, 270, 204, 100, 50, -20);
        assertArrayEquals(new int[]{480 - 204 - 2, 270 - 100 - 30}, bottomRight);

        assertEquals(72.0F, com.alex.mycelialmind.config.MycelialClientConfig.computeScaledFogFarPlane(72.0F, 8.0F, 1.0D), 1.0E-4F);
        assertEquals(180.0F, com.alex.mycelialmind.config.MycelialClientConfig.computeScaledFogFarPlane(72.0F, 8.0F, 0.25D), 1.0E-4F);
        assertEquals(180.0F, com.alex.mycelialmind.config.MycelialClientConfig.computeScaledFogFarPlane(72.0F, 8.0F, 0.05D), 1.0E-4F);
        assertEquals(9.0F, com.alex.mycelialmind.config.MycelialClientConfig.computeScaledFogFarPlane(2.0F, 8.0F, 0.5D), 1.0E-4F);
    }

    @Test
    void homeostasisLivingBattlefieldAndProgressionBoundariesHold() {
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.clampStress(Double.NaN));
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.clampStress(-5.0));
        assertEquals(100.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.clampStress(150.0));
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.STABLE,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.stageForStress(39.9));
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IRRITATED,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.stageForStress(40.0));
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IRRITATED,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.stageForStress(69.9));
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.stageForStress(70.0));
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.STABLE,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.byId(-1));
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.STABLE,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.byId(99));

        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId("minecraft", "stone", true, false, false));
        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId("minecraft", "water", false, true, false));
        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId("minecraft", "bedrock", false, false, true));
        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId(null, "stone", false, false, false));
        assertTrue(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isNaturalCavernCeilingId("minecraft", "bedrock"));
        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isNaturalCavernCeilingId("mycelialmind", null));

        assertEquals(0.25F, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(0.1F, 4, true), 1.0E-5F);
        assertEquals(1.0F, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(1.0F, 0, false), 1.0E-5F);
        assertNotNull(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IRRITATED, 55.0, false, true));
        assertNull(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(null, 50.0, false, false));

        assertTrue(com.alex.mycelialmind.hive.LivingBattlefieldSystem.isSapperUnit(true, false, EvolutionRank.BASE, false));
        assertTrue(com.alex.mycelialmind.hive.LivingBattlefieldSystem.isSapperUnit(false, false, EvolutionRank.ELITE, false));
        assertFalse(com.alex.mycelialmind.hive.LivingBattlefieldSystem.isSapperUnit(false, false, EvolutionRank.BASE, false));
        assertEquals(Vec3.ZERO, com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeTraumaDetourVector(
                new Vec3(1, 0, 0), Vec3.ZERO, 0.40F));
        assertTrue(com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeTraumaDetourVector(
                Vec3.ZERO, Vec3.ZERO, 0.80F).lengthSqr() > 0.1D);
        assertEquals(2, com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeBarricadePositions(
                new BlockPos(5, 64, 5), new Vec3(5.0, 64.0, 5.0)).size());
        assertFalse(com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldDropCarrion(false, true, false));

        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT,
                com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(Double.NaN));
        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT,
                com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(-5.0));
        assertEquals(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.resolveTier((net.minecraft.server.level.ServerLevel) null));
        assertEquals(com.alex.mycelialmind.world.SynapticNodeType.SENTINEL,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.pickOutbreakNodeType(
                        com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 0.01F));
        assertEquals(com.alex.mycelialmind.world.SynapticNodeType.INCUBATOR,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.pickOutbreakNodeType(
                        com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED, 0.25F));
        assertEquals(com.alex.mycelialmind.hive.EvolutionRank.BASE,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.citadelGuardianRank(
                        com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED));
    }

    @Test
    void redstoneTelemetryComparatorSignalsCoverAllBoundsAndModes() {
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.benchSignal(false, -1));
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.benchSignal(false, 0));
        assertEquals(3, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.benchSignal(false, 1));
        assertEquals(6, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.benchSignal(false, 2));
        assertEquals(9, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.benchSignal(false, 3));
        assertEquals(9, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.benchSignal(false, 5));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.benchSignal(true, 0));

        assertEquals(5, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.turretSignal(1, false));
        assertEquals(10, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.turretSignal(2, false));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.turretSignal(3, false));
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.turretSignal(3, true));
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.turretSignal(1, true));

        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.pylonSignal(false));
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.pylonSignal(true));
        assertEquals(5, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.pylonSignal(false, 1));
        assertEquals(10, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.pylonSignal(false, 2));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.pylonSignal(false, 3));
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.pylonSignal(true, 3));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.regenerativeFloraSignal(true));
        assertEquals(5, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.regenerativeFloraSignal(false));

        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.symbioticNodeSignal(-10));
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.symbioticNodeSignal(0));
        assertEquals(8, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.symbioticNodeSignal(50));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.symbioticNodeSignal(100));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.symbioticNodeSignal(140));

        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hostileNodeSignal(Double.NaN, -5));
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hostileNodeSignal(0.0D, 0));
        assertEquals(8, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hostileNodeSignal(50.0D, 0));
        assertEquals(9, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hostileNodeSignal(0.0D, 60));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hostileNodeSignal(150.0D, 20));
    }

    @Test
    void influenceMapDiffusionGradientsAndPruneBoundariesAreExact() {
        HiveInfluenceMap map = new HiveInfluenceMap();
        BlockPos origin = new BlockPos(0, 64, 0);
        map.addInfluence(origin, 0.4F, 0.2F, 0.8F);

        BlockPos[] cardinals = {
                origin.offset(16, 0, 0),
                origin.offset(-16, 0, 0),
                origin.offset(0, 0, 16),
                origin.offset(0, 0, -16)
        };
        for (BlockPos n : cardinals) {
            var c = map.sample(n);
            assertEquals(0.14F, c.control(), 1.0E-4F);
            assertEquals(0.07F, c.threat(), 1.0E-4F);
            assertEquals(0.28F, c.pheromone(), 1.0E-4F);
        }

        // Below 1e-4 diffused threshold must not create neighbor cells
        map.addInfluence(new BlockPos(1024, 64, 1024), 0.0002F, 0.0F, 0.0F);
        assertEquals(0.0F, map.sample(new BlockPos(1040, 64, 1024)).control(), 1.0E-6F);
        map.addInfluence(new BlockPos(1024, 64, 1024), 0.0F, 0.001F, 0.0F);
        assertTrue(map.sample(new BlockPos(1040, 64, 1024)).threat() > 0.0003F);

        // Trauma diffusion & escape gradient
        map.clear();
        BlockPos tCenter = new BlockPos(2048, 64, 0);
        map.addTrauma(tCenter, 0.0F);
        assertEquals(0, map.cellCount());
        map.addTrauma(tCenter, 0.8F);
        assertEquals(0.8F, map.sample(tCenter).trauma(), 1.0E-4F);
        assertEquals(0.20F, map.sample(tCenter.offset(16, 0, 0)).trauma(), 1.0E-4F);
        assertEquals(0.20F, map.sample(tCenter.offset(-16, 0, 0)).trauma(), 1.0E-4F);
        assertEquals(0.20F, map.sample(tCenter.offset(0, 0, 16)).trauma(), 1.0E-4F);
        assertEquals(0.20F, map.sample(tCenter.offset(0, 0, -16)).trauma(), 1.0E-4F);

        Vec3 escapeEast = map.sampleTraumaEscapeVector(tCenter.offset(16, 0, 0));
        assertEquals(1.0D, escapeEast.x, 1.0E-4D);
        assertEquals(0.0D, escapeEast.z, 1.0E-4D);

        Vec3 escapeSouth = map.sampleTraumaEscapeVector(tCenter.offset(0, 0, 16));
        assertEquals(0.0D, escapeSouth.x, 1.0E-4D);
        assertEquals(1.0D, escapeSouth.z, 1.0E-4D);

        map.clear();
        BlockPos sym = new BlockPos(3072, 64, 3072);
        map.addTrauma(sym, 0.54F);
        assertEquals(Vec3.ZERO, map.sampleTraumaEscapeVector(sym));
        map.addTrauma(sym, 0.02F);
        assertEquals(new Vec3(0.0D, 0.0D, 1.0D), map.sampleTraumaEscapeVector(sym));

        // Pheromone + threat composite gradient
        map.clear();
        BlockPos pCenter = new BlockPos(4096, 64, 0);
        map.addInfluence(pCenter.offset(32, 0, 0), 0.0F, 0.4F, 0.5F);
        map.addInfluence(pCenter.offset(0, 0, 32), 0.0F, 0.0F, 0.6F);
        Vec3 g = map.samplePheromoneGradient(pCenter.offset(16, 0, 0));
        assertTrue(g.x > 0.7D && g.z < 0.01D);
        Vec3 gz = map.samplePheromoneGradient(pCenter.offset(0, 0, 16));
        assertTrue(gz.z > 0.7D);

        // Trauma decays slower than control (factor+0.04 capped at 0.98)
        map.clear();
        map.addTrauma(pCenter, 0.5F);
        map.addInfluence(pCenter, 0.5F, 0.0F, 0.0F);
        map.stepDecay(0.5F);
        assertEquals(0.25F, map.sample(pCenter).control(), 1.0E-4F);
        assertEquals(0.27F, map.sample(pCenter).trauma(), 1.0E-4F);
    }

    @Test
    void homeostasisMathFogCurvesAndProgressionTierGettersAreExact() {
        assertEquals(36.5D,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeExplosionStress(10, 4.0F, 1.0D),
                1.0E-4D);
        assertEquals(55.0D,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeExplosionStress(100, 10.0F, 1.0D),
                1.0E-4D);
        assertEquals(2.2D,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeExplosionStress(0, 0.0F, 0.0D),
                1.0E-4D);

        assertEquals(-4.0D,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(1, 2, 1),
                1.0E-4D);
        assertEquals(8.5D,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(3, 4, 1),
                1.0E-4D);
        assertEquals(0.5D,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(2, 2, 10),
                1.0E-4D);

        assertEquals(4.0F,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(4.0F, 0, false),
                1.0E-4F);
        assertEquals(3.28F,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(4.0F, 1, false),
                1.0E-4F);
        assertEquals(2.56F,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(4.0F, 2, false),
                1.0E-4F);
        assertEquals(0.5F,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(0.5F, 3, false),
                1.0E-4F);
        assertEquals(1.0F,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAcidRainDamage(4.0F, 0, true),
                1.0E-4F);

        var irrNoIris = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IRRITATED, 55.0D, false, false);
        assertEquals(3.5F, irrNoIris.nearPlane(), 1.0E-4F);
        assertEquals(19.0F, irrNoIris.farPlane(), 1.0E-4F);
        var irrIris = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IRRITATED, 55.0D, false, true);
        assertEquals(5.0F, irrIris.nearPlane(), 1.0E-4F);
        assertEquals(25.0F, irrIris.farPlane(), 1.0E-4F);

        var immNoIris = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE, 85.0D, false, false);
        assertEquals(1.2F, immNoIris.nearPlane(), 1.0E-4F);
        assertEquals(6.5F, immNoIris.farPlane(), 1.0E-4F);
        var immIris = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE, 85.0D, false, true);
        assertEquals(2.0F, immIris.nearPlane(), 1.0E-4F);
        assertEquals(9.5F, immIris.farPlane(), 1.0E-4F);

        var thermalNoIris = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSporeFogRange(
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE, 85.0D, true, false);
        assertEquals(6.0F, thermalNoIris.nearPlane(), 1.0E-4F);
        assertEquals(32.0F, thermalNoIris.farPlane(), 1.0E-4F);

        assertEquals("IMMUNE RESPONSE",
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE.displayLabel());
        assertEquals(0xFFFF3366,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE.argbColor());
        assertEquals(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IRRITATED,
                com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.byId(1));

        for (var tier : com.alex.mycelialmind.hive.HiveProgressionTier.values()) {
            assertEquals(tier, com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(tier.minThreat()));
            assertFalse(tier.displayTag().isEmpty());
            assertTrue(tier.healthMultiplier() >= 1.0D && tier.damageMultiplier() >= 1.0D);
            assertTrue(tier.armorBonus() >= 0.0D && tier.speedMultiplier() >= 1.0D);
            assertTrue(tier.wavePromotionBonus() >= 0.0D);
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(tier) > 0);
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceOutbreakRarityChance(tier) > 0);
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.deepMyceliumOutbreakRarityChance(tier) > 0);
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakMinSpacing(tier) > 0);
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveRarityChance(tier) > 0);
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveMinSpacing(tier) > 0);
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveNodeClearance(tier) > 0);
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveMinSpawnDistance(tier) >= 0);
            assertEquals(tier.level(), com.alex.mycelialmind.world.structure.StructureProgressionPolicy.chestBonusCount(tier));
        }
        assertEquals(28, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT));
        assertEquals(192, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceOutbreakRarityChance(
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT));
        assertEquals(32, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.deepMyceliumOutbreakRarityChance(
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT));
        var tiers = com.alex.mycelialmind.hive.HiveProgressionTier.values();
        int[][] expectedOutbreakRarities = {
                {28, 192, 32},
                {20, 128, 24},
                {14, 88, 18},
                {10, 56, 12}
        };
        for (int i = 0; i < tiers.length; i++) {
            assertEquals(expectedOutbreakRarities[i][0],
                    com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(tiers[i]));
            assertEquals(expectedOutbreakRarities[i][1],
                    com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceOutbreakRarityChance(tiers[i]));
            assertEquals(expectedOutbreakRarities[i][2],
                    com.alex.mycelialmind.world.structure.StructureProgressionPolicy.deepMyceliumOutbreakRarityChance(tiers[i]));
        }
        assertEquals(220, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveRarityChance(
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT));
        assertEquals(96, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveMinSpawnDistance(
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT));
        assertEquals(28, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(null));
        assertEquals(192, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceOutbreakRarityChance(null));
        assertEquals(32, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.deepMyceliumOutbreakRarityChance(null));
        assertEquals(96, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveMinSpawnDistance(null));
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(tiers[i])
                    <= com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(tiers[i - 1]));
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceOutbreakRarityChance(tiers[i])
                    <= com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceOutbreakRarityChance(tiers[i - 1]));
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.deepMyceliumOutbreakRarityChance(tiers[i])
                    <= com.alex.mycelialmind.world.structure.StructureProgressionPolicy.deepMyceliumOutbreakRarityChance(tiers[i - 1]));
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveRarityChance(tiers[i])
                    <= com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveRarityChance(tiers[i - 1]));
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveMinSpawnDistance(tiers[i])
                    <= com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveMinSpawnDistance(tiers[i - 1]));
            assertTrue(com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceBreachChance(tiers[i])
                    >= com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceBreachChance(tiers[i - 1]));
        }
        assertEquals(0.35F, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceBreachChance(null), 1.0E-5F);
        assertTrue(com.alex.mycelialmind.world.PurificationZoneTracker.isWithinLinkRange(
                new BlockPos(0, 20, 0), new BlockPos(28, 20, 0), 28));
        assertFalse(com.alex.mycelialmind.world.PurificationZoneTracker.isWithinLinkRange(
                new BlockPos(0, 20, 0), new BlockPos(29, 20, 0), 28));
        assertTrue(com.alex.mycelialmind.world.PurificationZoneTracker.isWithinLinkRange(
                new BlockPos(0, 20, 0), new BlockPos(4, 68, 4), 28));
        assertFalse(com.alex.mycelialmind.world.PurificationZoneTracker.isWithinLinkRange(
                new BlockPos(0, 20, 0), new BlockPos(4, 104, 4), 28));
        assertFalse(com.alex.mycelialmind.world.PurificationZoneTracker.isWithinLinkRange(
                new BlockPos(0, 20, 0), new BlockPos(15, 68, 0), 28));
        assertEquals(com.alex.mycelialmind.world.SynapticNodeType.SENTINEL,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.pickOutbreakNodeType(
                        com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 0.049F));
        assertEquals(com.alex.mycelialmind.world.SynapticNodeType.INCUBATOR,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.pickOutbreakNodeType(
                        com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 0.05F));
        assertEquals(com.alex.mycelialmind.world.SynapticNodeType.RELAY,
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.pickOutbreakNodeType(
                        com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 0.20F));
    }

    @Test
    void livingBattlefieldDetourBarricadesAndFastNoiseDeterminism() {
        Vec3 detourZero = com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeTraumaDetourVector(
                new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D), 0.54F);
        assertEquals(Vec3.ZERO, detourZero);

        Vec3 detourBoundary = com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeTraumaDetourVector(
                new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D), 0.55F);
        assertEquals(0.0D, detourBoundary.x, 1.0E-4D);
        assertEquals(1.925D, detourBoundary.z, 1.0E-4D);

        assertTrue(com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeAntiPillarStepPositions(
                new BlockPos(0, 64, 0), new BlockPos(0, 65, 0)).isEmpty());
        var steps = com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeAntiPillarStepPositions(
                new BlockPos(0, 64, 0), new BlockPos(0, 66, 0));
        assertEquals(List.of(new BlockPos(1, 64, 0), new BlockPos(2, 65, 0)), steps);

        var bar = com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeBarricadePositions(
                new BlockPos(5, 64, 5), new Vec3(8.0D, 64.0D, 3.0D));
        assertEquals(List.of(new BlockPos(6, 64, 4), new BlockPos(6, 65, 4)), bar);

        Vec3 f0 = MycelialFastNoise.organicFlow2D(10.0D, 20.0D, 42L, 0.0D);
        Vec3 f1 = MycelialFastNoise.organicFlow2D(10.0D, 20.0D, 42L, 3.5D);
        Vec3 f2 = MycelialFastNoise.organicFlow2D(10.0D, 20.0D, 99L, 0.0D);
        assertTrue(Math.abs(f0.x) <= 1.0D && Math.abs(f0.z) <= 1.0D);
        assertEquals(0.0D, f0.y, 1.0E-6D);
        assertNotEquals(f0, f1);
        assertNotEquals(f0, f2);
    }

    @Test
    void homeostasisV2SensitizationTraumaDiffusionAndCystSignalAreExact() {
        assertEquals(0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.clampSensitization(-2));
        assertEquals(3, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.clampSensitization(9));
        assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isCytokineStorm(84.9));
        assertTrue(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isCytokineStorm(85.0));

        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSensitizedStressDelta(0.0, 2), 1.0E-6);
        assertEquals(14.5, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSensitizedStressDelta(10.0, 3), 1.0E-6);
        assertEquals(-6.4, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeSensitizedStressDelta(-10.0, 3), 1.0E-6);

        assertEquals(4.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                "mycelialmind", "mycelial_node", false), 1.0E-6);
        assertEquals(2.8, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                "mycelialmind", "luminescent_amber_vein", false), 1.0E-6);
        assertEquals(1.2, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                "mycelialmind", "mycelial_stone", false), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                "mycelialmind", "mycelial_node", true), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                "minecraft", "stone", false), 1.0E-6);

        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                69.9, 10.0, false), 1.0E-6);
        assertEquals(1.8, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                90.0, 10.0, false), 1.0E-6);
        assertEquals(0.5, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                90.0, 44.5, false), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                90.0, 45.0, false), 1.0E-6);
        assertEquals(1.8, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                90.0, 50.0, true), 1.0E-6);

        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.antibodyCystSignal(false, 95.0, 6));
        assertEquals(3, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.antibodyCystSignal(true, 0.0, 0));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.antibodyCystSignal(true, 100.0, 6));
    }

    @Test
    void progressionTierDimensionalSyncMultiplierAndPostMatriarchMathWork() {
        var dormant = com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT;
        var awakened = com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED;
        var adaptive = com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE;
        var overmind = com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND;

        assertEquals(0.85, dormant.deepMyceliumStressMultiplier(), 1.0E-6);
        assertEquals(1.00, awakened.deepMyceliumStressMultiplier(), 1.0E-6);
        assertEquals(1.15, adaptive.deepMyceliumStressMultiplier(), 1.0E-6);
        assertEquals(1.30, overmind.deepMyceliumStressMultiplier(), 1.0E-6);

        assertFalse(com.alex.mycelialmind.hive.HiveProgressionTier.isRiftResonantTier(null));
        assertFalse(com.alex.mycelialmind.hive.HiveProgressionTier.isRiftResonantTier(dormant));
        assertFalse(com.alex.mycelialmind.hive.HiveProgressionTier.isRiftResonantTier(awakened));
        assertTrue(com.alex.mycelialmind.hive.HiveProgressionTier.isRiftResonantTier(adaptive));
        assertTrue(com.alex.mycelialmind.hive.HiveProgressionTier.isRiftResonantTier(overmind));

        assertEquals(0.0, com.alex.mycelialmind.hive.HiveProgressionTier.clampThreat(Double.NaN), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HiveProgressionTier.clampThreat(-10.0), 1.0E-6);
        assertEquals(100.0, com.alex.mycelialmind.hive.HiveProgressionTier.clampThreat(150.0), 1.0E-6);

        assertEquals(51.0, com.alex.mycelialmind.hive.HiveProgressionTier.computeSyncedDeepThreat(60.0, 0.0), 1.0E-6);
        assertEquals(70.0, com.alex.mycelialmind.hive.HiveProgressionTier.computeSyncedDeepThreat(60.0, 70.0), 1.0E-6);
        assertEquals(52.0, com.alex.mycelialmind.hive.HiveProgressionTier.computePostMatriarchThreat(80.0), 1.0E-6);
    }

    @Test
    void chimeraAcidRainGeneMitigationRulesHold() {
        assertEquals(-0.75F, com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeChimeraAcidRainDelta(false, false), 1.0E-6F);
        assertEquals(0.0F, com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeChimeraAcidRainDelta(true, false), 1.0E-6F);
        assertEquals(0.0F, com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeChimeraAcidRainDelta(false, true), 1.0E-6F);
        assertEquals(0.8F, com.alex.mycelialmind.hive.LivingBattlefieldSystem.computeChimeraAcidRainDelta(true, true), 1.0E-6F);
    }

    @Test
    void homeostasisSensitizationDiffusionAndProgressionThresholdsHold() {
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.clampStress(Double.NaN), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.clampStress(-5.0), 1.0E-6);
        assertEquals(100.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.clampStress(140.0), 1.0E-6);

        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                70.0, 45.0, false), 1.0E-6);
        assertEquals(0.6, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                70.0, 20.0, false), 1.0E-6);
        assertEquals(2.4, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                100.0, 95.0, true), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                100.0, 100.0, true), 1.0E-6);

        assertEquals(2.8, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                "mycelialmind", "spore_vent", false), 1.0E-6);
        assertEquals(1.8, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                "mycelialmind", "creeping_mycelium", false), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                "mycelialmind", "creeping_mycelium", true), 1.0E-6);

        assertEquals(3, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.antibodyCystSignal(true, -10.0, -2));
        assertEquals(11, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.antibodyCystSignal(true, 60.0, 6));
        assertEquals(6, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.antibodyCystSignal(true, Double.NaN, 10));

        assertEquals(20.0F, com.alex.mycelialmind.config.MycelialClientConfig.computeScaledFogFarPlane(20.0F, 5.0F, Double.NaN), 1.0E-5F);
        assertEquals(20.0F, com.alex.mycelialmind.config.MycelialClientConfig.computeScaledFogFarPlane(20.0F, 5.0F, 1.0D), 1.0E-5F);
        assertEquals(50.0F, com.alex.mycelialmind.config.MycelialClientConfig.computeScaledFogFarPlane(20.0F, 5.0F, 0.25D), 1.0E-5F);
        assertEquals(11.0F, com.alex.mycelialmind.config.MycelialClientConfig.computeScaledFogFarPlane(2.0F, 10.0F, 0.50D), 1.0E-5F);
    }

    @Test
    void evolutionRankAudioDynamicsScaleMonotonically() {
        float basePitch = com.alex.mycelialmind.hive.EvolutionRank.BASE.voicePitchMultiplier();
        float mutPitch = com.alex.mycelialmind.hive.EvolutionRank.MUTATED.voicePitchMultiplier();
        float elitePitch = com.alex.mycelialmind.hive.EvolutionRank.ELITE.voicePitchMultiplier();
        assertEquals(1.0F, basePitch, 1.0E-6F);
        assertEquals(com.alex.mycelialmind.hive.EvolutionRank.BASE,
                com.alex.mycelialmind.hive.EvolutionRank.fromId(-1));
        assertTrue(elitePitch < mutPitch && mutPitch < basePitch);

        float baseVol = com.alex.mycelialmind.hive.EvolutionRank.BASE.soundVolumeMultiplier();
        float mutVol = com.alex.mycelialmind.hive.EvolutionRank.MUTATED.soundVolumeMultiplier();
        float eliteVol = com.alex.mycelialmind.hive.EvolutionRank.ELITE.soundVolumeMultiplier();
        assertEquals(1.0F, baseVol, 1.0E-6F);
        assertTrue(eliteVol > mutVol && mutVol > baseVol);

        float amberFrenzyPitch = com.alex.mycelialmind.hive.EvolutionRank.computeTacticalVoicePitchMultiplier(
                com.alex.mycelialmind.hive.EvolutionRank.BASE,
                com.alex.mycelialmind.hive.HiveStrain.AMBER,
                true, false,
                com.alex.mycelialmind.hive.SynapticCommandSystem.CommandRole.NONE,
                false
        );
        float crimsonLeaderOccludedPitch = com.alex.mycelialmind.hive.EvolutionRank.computeTacticalVoicePitchMultiplier(
                com.alex.mycelialmind.hive.EvolutionRank.BASE,
                com.alex.mycelialmind.hive.HiveStrain.CRIMSON,
                false, false,
                com.alex.mycelialmind.hive.SynapticCommandSystem.CommandRole.LEADER,
                true
        );
        assertTrue(amberFrenzyPitch > 1.10F && crimsonLeaderOccludedPitch < 0.85F);

        float leaderUnoccludedVol = com.alex.mycelialmind.hive.EvolutionRank.computeTacticalSoundVolumeMultiplier(
                com.alex.mycelialmind.hive.EvolutionRank.MUTATED,
                true,
                com.alex.mycelialmind.hive.SynapticCommandSystem.CommandRole.LEADER,
                false
        );
        float occludedVol = com.alex.mycelialmind.hive.EvolutionRank.computeTacticalSoundVolumeMultiplier(
                com.alex.mycelialmind.hive.EvolutionRank.MUTATED,
                false,
                com.alex.mycelialmind.hive.SynapticCommandSystem.CommandRole.NONE,
                true
        );
        assertTrue(leaderUnoccludedVol > mutVol && occludedVol < mutVol);
        assertEquals(0, com.alex.mycelialmind.hive.EvolutionRank.computeThreatMilestone(15.0));
        assertEquals(50, com.alex.mycelialmind.hive.EvolutionRank.computeThreatMilestone(64.0));
        assertEquals(100, com.alex.mycelialmind.hive.EvolutionRank.computeThreatMilestone(100.0));

        assertTrue(com.alex.mycelialmind.hive.EvolutionRank.shouldTriggerImmuneSurgeAlert(true, 1, 2, 2));
        assertFalse(com.alex.mycelialmind.hive.EvolutionRank.shouldTriggerImmuneSurgeAlert(false, 1, 2, 2));
        assertFalse(com.alex.mycelialmind.hive.EvolutionRank.shouldTriggerImmuneSurgeAlert(true, 2, 2, 2));
        assertEquals(0.0F, com.alex.mycelialmind.hive.EvolutionRank.computeAcidRainSizzleVolume(85.0, true, 1.0), 1.0E-5F);
        assertEquals(0.0F, com.alex.mycelialmind.hive.EvolutionRank.computeAcidRainSizzleVolume(85.0, false, 0.0), 1.0E-5F);
        float sizzleHigh = com.alex.mycelialmind.hive.EvolutionRank.computeAcidRainSizzleVolume(100.0, false, 1.0);
        float sizzleLow = com.alex.mycelialmind.hive.EvolutionRank.computeAcidRainSizzleVolume(25.0, false, 1.0);
        assertTrue(sizzleHigh > sizzleLow && sizzleHigh <= 0.5001F);

        assertEquals(1.00F, com.alex.mycelialmind.hive.EvolutionRank.computePylonHumPitch(1), 1.0E-5F);
        assertEquals(1.12F, com.alex.mycelialmind.hive.EvolutionRank.computePylonHumPitch(2), 1.0E-5F);
        assertEquals(1.24F, com.alex.mycelialmind.hive.EvolutionRank.computePylonHumPitch(3), 1.0E-5F);
        assertEquals(15.0D, com.alex.mycelialmind.hive.EvolutionRank.computePylonHumRadius(1), 1.0E-5D);
        assertEquals(19.0D, com.alex.mycelialmind.hive.EvolutionRank.computePylonHumRadius(2), 1.0E-5D);
        assertEquals(24.0D, com.alex.mycelialmind.hive.EvolutionRank.computePylonHumRadius(3), 1.0E-5D);
    }

    @Test
    void progressionGoalEngineScalesBastionAndBestiaryDeterministically() {
        var engineShort = com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.CRAFT_SCANNER;
        assertEquals("craft_scanner", engineShort.id());
        assertEquals(0, engineShort.stepIndex());
        assertEquals("codex.mycelialmind.goal.short.craft_scanner", engineShort.translationKey());
        assertEquals("maintain_dominion", com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.MAINTAIN_DOMINION.id());
        assertEquals(4, com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.MAINTAIN_DOMINION.stepIndex());
        assertEquals("codex.mycelialmind.goal.mid.maintain_dominion",
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.MAINTAIN_DOMINION.translationKey());

        assertEquals(17, com.alex.mycelialmind.hive.ProgressionGoalEngine.BESTIARY_SPECIES_IDS.size());
        assertEquals(0, com.alex.mycelialmind.hive.ProgressionGoalEngine.speciesIndex("sporeling"));
        assertEquals(16, com.alex.mycelialmind.hive.ProgressionGoalEngine.speciesIndex("mycelial_chimera"));
        assertEquals(-1, com.alex.mycelialmind.hive.ProgressionGoalEngine.speciesIndex(null));
        assertEquals(-1, com.alex.mycelialmind.hive.ProgressionGoalEngine.speciesIndex(""));
        assertEquals(-1, com.alex.mycelialmind.hive.ProgressionGoalEngine.speciesIndex("unknown_mob"));

        int m0 = 0;
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.CRAFT_SCANNER,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveShortTermGoal(m0, 0, 0));
        assertEquals(0, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeShortTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.CRAFT_SCANNER, m0, 0, 0));

        int m1 = 1 << com.alex.mycelialmind.hive.ProgressionGoalEngine.MILESTONE_BIO_SCANNER;
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.BUILD_RESEARCH_BENCH,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveShortTermGoal(m1, 0, 0));
        assertEquals(20, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeShortTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.BUILD_RESEARCH_BENCH, m1, 0, 0));

        int m2 = m1 | (1 << com.alex.mycelialmind.hive.ProgressionGoalEngine.MILESTONE_SYNAPTIC_BENCH);
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.EXTRACT_GENETIC_SAMPLE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveShortTermGoal(m2, 0, 0));
        assertEquals(40, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeShortTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.EXTRACT_GENETIC_SAMPLE, m2, 0, 0));
        // Discovering a trait directly advances past EXTRACT_GENETIC_SAMPLE even without extractor milestone
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.SYNTHESIZE_BIOTECH,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveShortTermGoal(m2, 1, 0));

        int m3 = m2 | (1 << com.alex.mycelialmind.hive.ProgressionGoalEngine.MILESTONE_BIO_EXTRACTOR);
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.SYNTHESIZE_BIOTECH,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveShortTermGoal(m3, 0, 0));
        assertEquals(60, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeShortTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.SYNTHESIZE_BIOTECH, m3, 0, 0));

        int m4 = m3 | (1 << com.alex.mycelialmind.hive.ProgressionGoalEngine.MILESTONE_SYNTHESIZED_BIOTECH);
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.UPGRADE_GEAR_OR_STATION,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveShortTermGoal(m4, 0, 0));
        assertEquals(80, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeShortTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.UPGRADE_GEAR_OR_STATION, m4, 0, 0));

        int m5 = m4 | (1 << com.alex.mycelialmind.hive.ProgressionGoalEngine.MILESTONE_CHITIN_EVOLVED);
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.MASTER_EXPERIMENTS,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveShortTermGoal(m5, 0, 0));
        assertEquals(80, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeShortTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.MASTER_EXPERIMENTS, m5, 0, 0));
        assertEquals(100, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeShortTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ShortTermGoal.MASTER_EXPERIMENTS, m5, 0x1F, 0xFF));
        assertEquals(0, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeShortTermProgressPercent(
                null, m5, 0x1F, 0xFF));

        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.ESTABLISH_PERIMETER,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveMidTermGoal(
                        com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 0, false, false));
        assertEquals(10, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.ESTABLISH_PERIMETER,
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 0, false, false));
        assertEquals(20, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.ESTABLISH_PERIMETER,
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 1, false, false));

        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.CONQUER_HIVE_SANCTUM,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveMidTermGoal(
                        com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 1, false, false));
        assertEquals(30, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.CONQUER_HIVE_SANCTUM,
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 1, false, false));
        assertEquals(45, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.CONQUER_HIVE_SANCTUM,
                com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED, 1, false, false));

        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.BREACH_DEEP_MYCELIUM,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveMidTermGoal(
                        com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, 2, false, false));
        assertEquals(55, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.BREACH_DEEP_MYCELIUM,
                com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, 2, false, false));
        assertEquals(65, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.BREACH_DEEP_MYCELIUM,
                com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, 2, true, false, false));
        assertEquals(75, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.BREACH_DEEP_MYCELIUM,
                com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, 2, true, false));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.BREACH_DEEP_MYCELIUM,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveMidTermGoal(
                        com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED, 1, true, false, false));
        assertEquals(50, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.CONQUER_HIVE_SANCTUM,
                com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED, 1, true, false, false));

        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.OVERCOME_TIDE_AND_MATRIARCH,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveMidTermGoal(
                        com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, 2, true, false));
        assertEquals(85, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.OVERCOME_TIDE_AND_MATRIARCH,
                com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND, 2, true, false));
        assertEquals(75, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.OVERCOME_TIDE_AND_MATRIARCH,
                com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND, 2, false, false));

        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.MAINTAIN_DOMINION,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveMidTermGoal(
                        com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND, 2, true, true));
        assertEquals(100, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MidTermGoal.MAINTAIN_DOMINION,
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 0, false, false));
        assertEquals(0, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeMidTermProgressPercent(
                null, com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, 0, false, false));

        assertEquals(0, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeBastionScore(0, 3, 0, 3, 0, 0, false));
        assertEquals(5, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeBastionScore(-2, 0, -1, 0, -5, 1, false));
        assertEquals(30, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeBastionScore(1, 2, 1, 1, 0, 1, false));
        assertEquals(65, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeBastionScore(2, 2, 1, 2, 1, 2, true));
        assertEquals(100, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeBastionScore(5, 9, 4, 9, 5, 3, true));

        assertEquals(0, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeLongTermMasteryPercent(-1, -1, -1, -10));
        assertEquals(100, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeLongTermMasteryPercent(99, 99, 99, 500));
        int midLong = com.alex.mycelialmind.hive.ProgressionGoalEngine.computeLongTermMasteryPercent(17, 0, 0, 0);
        assertEquals(35, midLong);

        // 4-Act progression policy & campaign completion tests
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.hasMilestone(
                1 << com.alex.mycelialmind.hive.ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED));
        assertFalse(com.alex.mycelialmind.hive.ProgressionGoalEngine.hasMilestone(0, -1));
        assertFalse(com.alex.mycelialmind.hive.ProgressionGoalEngine.hasMilestone(0xFFFF, 16));

        assertEquals(10, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeActCampaignCompletionPercent(
                null, false, false, false));
        assertEquals(25, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeActCampaignCompletionPercent(
                com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED, false, false, false));
        assertEquals(65, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeActCampaignCompletionPercent(
                com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, true, false, false));
        assertEquals(95, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeActCampaignCompletionPercent(
                com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND, true, true, false));
        assertEquals(100, com.alex.mycelialmind.hive.ProgressionGoalEngine.computeActCampaignCompletionPercent(
                com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, false, false, true));

        var act1 = com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT;
        var act2 = com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED;
        var act3 = com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE;
        var act4 = com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND;
        assertEquals(1, act1.actNumber());
        assertEquals(4, act4.actNumber());
        assertTrue(act2.meetsRequiredAct(null));
        assertTrue(act2.meetsRequiredAct(act2));
        assertFalse(act1.meetsRequiredAct(act2));
        assertEquals(act4, com.alex.mycelialmind.hive.HiveProgressionTier.resolveEffectiveTier(10.0, 99));
        assertEquals(act3, com.alex.mycelialmind.hive.HiveProgressionTier.resolveEffectiveTier(55.0, 0));
        assertEquals(act1, com.alex.mycelialmind.hive.HiveProgressionTier.resolveEffectiveTier(5.0, -5));

        assertTrue(act4.primordialSpawnChance(true, true, false) > act1.primordialSpawnChance(false, false, false));
        assertEquals(0.0075F, act1.primordialSpawnChance(false, false, true), 1.0E-5F);
        assertEquals(0.110F, act4.primordialSpawnChance(true, false, false), 1.0E-5F);
        assertEquals(0.75F, act1.primordialAllyHealAmount(false), 1.0E-5F);
        assertEquals(1.0F, act4.primordialAllyHealAmount(true), 1.0E-5F);
        assertTrue(act4.floraRegrowthMultiplier(true) > act4.floraRegrowthMultiplier(false));
        assertEquals(0.0F, act1.floraBonusYieldChance(), 1.0E-5F);
        assertEquals(0.35F, act4.floraBonusYieldChance(), 1.0E-5F);
        assertEquals(1.0F, act2.pylonTier3PurgeDamage(), 1.0E-5F);
        assertEquals(2.0F, act4.pylonTier3PurgeDamage(), 1.0E-5F);
        assertEquals(act2, com.alex.mycelialmind.hive.HiveProgressionTier.requiredActForSecret(0));
        assertEquals(act2, com.alex.mycelialmind.hive.HiveProgressionTier.requiredActForSecret(2));
        assertEquals(act3, com.alex.mycelialmind.hive.HiveProgressionTier.requiredActForSecret(3));
        assertEquals(act1, com.alex.mycelialmind.hive.HiveProgressionTier.requiredActForSecret(1));

        assertEquals(0.20F, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.sealedBioVaultChance(null), 1.0E-5F);
        assertEquals(0.20F, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.sealedBioVaultChance(act1), 1.0E-5F);
        assertEquals(0.35F, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.sealedBioVaultChance(act2), 1.0E-5F);
        assertEquals(0.45F, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.sealedBioVaultChance(act3), 1.0E-5F);
        assertEquals(0.55F, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.sealedBioVaultChance(act4), 1.0E-5F);
    }

    @Test
    void hyphalRouteDensityAccumulatesDecaysThresholdsAndVeinInterpolationWork() {
        double gainNormal = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(10.0, 4, false, false, false);
        double gainCrouch = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(10.0, 0, false, false, true);
        double gainUncappedRep = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(10.0, 10, false, false, false);
        double gainBeforeDamp = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(84.9, 0, false, false, false);
        double gainAtDamp = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(85.0, 0, false, false, false);
        double gainNearCap = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(99.0, 0, false, false, false);
        double gainNan = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(Double.NaN, -3, false, false, false);
        double protectedPenalty = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(50.0, 4, true, false, false);
        double symbioticPenalty = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeRouteDensityDelta(50.0, 4, false, true, false);
        assertEquals(5.0, gainNormal, 1.0E-6);
        assertEquals(1.0, gainCrouch, 1.0E-6);
        assertEquals(6.25, gainUncappedRep, 1.0E-6);
        assertEquals(2.5, gainBeforeDamp, 1.0E-6);
        assertEquals(1.25, gainAtDamp, 1.0E-6);
        assertEquals(1.0, gainNearCap, 1.0E-6);
        assertEquals(2.5, gainNan, 1.0E-6);
        assertEquals(-4.0, protectedPenalty, 1.0E-6);
        assertEquals(-4.0, symbioticPenalty, 1.0E-6);

        double recentDecay = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(50.0, 1199L, 2.0);
        double negativeTickDecay = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(50.0, -5L, 2.0);
        double boundaryDecay = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(50.0, 1200L, 2.0);
        double midDecay = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(50.0, 3600L, 2.0);
        double maxStaleDecay = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(150.0, 10000L, 2.0);
        assertEquals(49.0, recentDecay, 1.0E-6);
        assertEquals(49.0, negativeTickDecay, 1.0E-6);
        assertEquals(48.0, boundaryDecay, 1.0E-6);
        assertEquals(46.0, midDecay, 1.0E-6);
        assertEquals(94.0, maxStaleDecay, 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(-1.0, 2400L, 1.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(Double.NaN, 2400L, 1.0), 1.0E-6);
        assertEquals(20.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(20.0, 2400L, 0.0), 1.0E-6);
        assertEquals(20.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(20.0, 2400L, Double.NaN), 1.0E-6);
        assertEquals(0.5, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(1.0, 1200L, 0.5), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeDecayedRouteDensity(1.0, 1200L, 0.51), 1.0E-6);

        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.isOutsideHyphalNetwork(Double.NaN));
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.isOutsideHyphalNetwork(19.99));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.isOutsideHyphalNetwork(20.0));
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.shouldSproutRouteVein(35.0, false));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.shouldSproutRouteVein(34.99, false));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.shouldSproutRouteVein(Double.NaN, false));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.shouldSproutRouteVein(35.0, true));
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldTriggerTacticalAmbush(60.0, true, false, 1200L));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldTriggerTacticalAmbush(59.99, true, false, 1200L));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldTriggerTacticalAmbush(Double.NaN, true, false, 1200L));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldTriggerTacticalAmbush(60.0, false, false, 1200L));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldTriggerTacticalAmbush(60.0, true, true, 1200L));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldTriggerTacticalAmbush(60.0, true, false, 1199L));

        assertEquals(22.5, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeVeinSeverDensityDrop(true, false, 0), 1.0E-6);
        assertEquals(31.5, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeVeinSeverDensityDrop(true, true, 3), 1.0E-6);
        assertEquals(54.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeVeinSeverDensityDrop(true, true, 12), 1.0E-6);
        assertEquals(21.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeVeinSeverDensityDrop(false, true, 3), 1.0E-6);
        assertEquals(7.5, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeVeinSeverDensityDrop(false, false, 0), 1.0E-6);
        assertEquals(10.5, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeVeinSeverDensityDrop(false, false, 3), 1.0E-6);

        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(3, 64, 2);
        List<BlockPos> path = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .interpolateRouteVeinPositions(from, to, 16);
        assertEquals(6, path.size());
        assertEquals(from, path.get(0));
        assertEquals(to, path.get(path.size() - 1));
        for (int i = 1; i < path.size(); i++) {
            assertEquals(1, path.get(i).distManhattan(path.get(i - 1)));
        }
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .interpolateRouteVeinPositions(null, to, 8).isEmpty());
        assertEquals(List.of(from), com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .interpolateRouteVeinPositions(from, null, 8));
        assertEquals(17, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .interpolateRouteVeinPositions(from, new BlockPos(16, 64, 0), 20).size());
        assertEquals(List.of(from), com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .interpolateRouteVeinPositions(from, new BlockPos(16, 64, 1), 20));
        assertEquals(4, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .interpolateRouteVeinPositions(from, new BlockPos(0, 67, 0), 10).size());
        assertEquals(2, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .interpolateRouteVeinPositions(from, new BlockPos(3, 64, 0), 0).size());
    }

    @Test
    void architecturalAccentClassifiesProportionsLightStylesAndCompanionBlocks() {
        assertEquals("tower_spire",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.TOWER_SPIRE.id());
        assertEquals("hall_gallery",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.HALL_GALLERY.id());
        assertEquals("bunker_vault",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.BUNKER_VAULT.id());
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.TOWER_SPIRE,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.fromId("tower_spire"));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.BUNKER_VAULT,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.fromId("BUNKER_VAULT"));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.HALL_GALLERY,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.fromId("unknown"));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.HALL_GALLERY,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.fromId(null));

        assertEquals("shadow_crypt",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.SHADOW_CRYPT.id());
        assertEquals("balanced_glow",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.BALANCED_GLOW.id());
        assertEquals("radiant_canopy",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.RADIANT_CANOPY.id());

        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.TOWER_SPIRE,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyProportion(1.35));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.HALL_GALLERY,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyProportion(1.34));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.BUNKER_VAULT,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyProportion(0.75));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.HALL_GALLERY,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyProportion(0.76));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.HALL_GALLERY,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyProportion(Double.NaN));

        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.SHADOW_CRYPT,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyLightStyle(0.29));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.BALANCED_GLOW,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyLightStyle(0.30));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.RADIANT_CANOPY,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyLightStyle(0.68));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.BALANCED_GLOW,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyLightStyle(0.67));
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.BALANCED_GLOW,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.classifyLightStyle(Double.NaN));

        assertEquals("minecraft:polished_basalt",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId(null));
        assertEquals("minecraft:polished_basalt",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("   "));
        assertEquals("minecraft:dark_oak_log",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:dark_oak_planks"));
        assertEquals("minecraft:oak_log",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("oak_planks"));
        assertEquals("minecraft:_planks",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:_planks"));
        assertEquals("minecraft:crimson_stem",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:crimson_planks"));
        assertEquals("minecraft:warped_stem",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:warped_planks"));
        assertEquals("minecraft:chiseled_stone_bricks",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:stone_bricks"));
        assertEquals("minecraft:deepslate_tiles",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:deepslate_bricks"));
        assertEquals("minecraft:mossy_cobblestone",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:cobblestone"));
        assertEquals("minecraft:quartz_pillar",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:quartz_block"));
        assertEquals("minecraft:cut_copper",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("minecraft:copper_block"));
        assertEquals("custom:slate",
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId("custom:slate"));

        int[] towerDims = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeScaledDimensions(4.0, 2.0);
        int[] bunkerDims = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeScaledDimensions(10.0, 0.5);
        int[] nanDims = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeScaledDimensions(Double.NaN, Double.NaN);
        int[] maxDims = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeScaledDimensions(20.0, 3.0);
        assertArrayEquals(new int[]{2, 10}, towerDims);
        assertArrayEquals(new int[]{5, 6}, bunkerDims);
        assertArrayEquals(new int[]{2, 5}, nanDims);
        assertArrayEquals(new int[]{6, 16}, maxDims);

        assertEquals(12.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .updateRunningMetric(5.0, 12.0, 1, 2.0, 16.0), 1.0E-6);
        assertEquals(8.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .updateRunningMetric(5.0, 11.0, 2, 2.0, 16.0), 1.0E-6);
        assertEquals(5.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .updateRunningMetric(4.0, 8.0, 10, 2.0, 16.0), 1.0E-6);
        assertEquals(2.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .updateRunningMetric(Double.NaN, Double.NaN, 1, 16.0, 2.0), 1.0E-6);

        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot.EMPTY.hasCustomAccent());
        assertFalse(new com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot(
                "minecraft:oak_planks", "minecraft:oak_log", 4.0, 1.5, 0.2, 0).hasCustomAccent());
        assertFalse(new com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot(
                " ", "minecraft:oak_log", 4.0, 1.5, 0.2, 3).hasCustomAccent());
        assertFalse(new com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot(
                null, "minecraft:oak_log", 4.0, 1.5, 0.2, 3).hasCustomAccent());
        var validAccent = new com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot(
                "minecraft:oak_planks", "minecraft:oak_log", 4.0, 1.5, 0.2, 3);
        assertTrue(validAccent.hasCustomAccent());
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.TOWER_SPIRE,
                validAccent.proportion());
        assertEquals(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle.SHADOW_CRYPT,
                validAccent.lightStyle());
    }

    @Test
    void blindSpotSuitabilityGrowthDelaysPhotophobicStallAndBlueprintConnectivityWork() {
        assertEquals(0.92, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(80.0, 0, false, true, true, 16.0), 1.0E-6);
        assertEquals(0.22, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(25.0, 4, false, true, true, 16.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(24.99, 4, false, true, true, 16.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(80.0, 5, false, true, true, 16.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(80.0, 0, true, true, true, 16.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(80.0, 0, false, false, true, 16.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(80.0, 0, false, true, false, 16.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(80.0, 0, false, true, true, 15.99), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(Double.NaN, 0, false, true, true, 16.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(80.0, 0, false, true, true, Double.NaN), 1.0E-6);

        assertEquals(-1L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, false, 9));
        assertEquals(2700L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, false, 5));
        assertEquals(2700L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, false, 8));
        assertEquals(1800L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(null, false, 4));
        assertEquals(1500L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED, false, 0));
        assertEquals(1200L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, false, 0));
        assertEquals(900L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND, false, 0));
        assertEquals(450L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND, true, 0));
        assertEquals(675L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND, true, 5));
        assertEquals(900L, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeGrowthStepDelayTicks(com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, true, 0));

        assertEquals(1.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(80.0, 0, false, true, true, 16.0, true), 1.0E-6);
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(30.0, 4, false, true, true, 16.0, true)
                > com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .evaluateBlindSpotSuitability(30.0, 4, false, true, true, 16.0, false));

        BlockPos core = new BlockPos(10, 65, 10);
        var towerBp = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.generateBlindSpotBlueprint(
                core,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.TOWER_SPIRE,
                4.5, 1.9, 0.80);
        var wideTowerBp = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.generateBlindSpotBlueprint(
                core,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.TOWER_SPIRE,
                6.5, 1.35, 0.50);
        var hallBp = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.generateBlindSpotBlueprint(
                core,
                null,
                Double.NaN, Double.NaN, Double.NaN);
        var bunkerBp = com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.generateBlindSpotBlueprint(
                core,
                com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.BUNKER_VAULT,
                8.0, 0.6, 0.15);

        int maxTowerY = towerBp.stream().mapToInt(v -> v.pos().getY()).max().orElse(0);
        int maxBunkerY = bunkerBp.stream().mapToInt(v -> v.pos().getY()).max().orElse(0);
        assertEquals(65 + 7 + 1, maxTowerY);
        assertEquals(65 + 3 + 1, maxBunkerY);
        assertTrue(wideTowerBp.size() > towerBp.size());
        assertFalse(hallBp.isEmpty());

        long towerLumCount = towerBp.stream()
                .filter(v -> v.role() == com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.BlueprintVoxelRole.LUMINESCENT_VEIN)
                .count();
        long wideBalancedLumCount = wideTowerBp.stream()
                .filter(v -> v.role() == com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.BlueprintVoxelRole.LUMINESCENT_VEIN)
                .count();
        long bunkerLumCount = bunkerBp.stream()
                .filter(v -> v.role() == com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.BlueprintVoxelRole.LUMINESCENT_VEIN)
                .count();
        assertEquals(6L, towerLumCount);
        assertEquals(2L, wideBalancedLumCount);
        assertEquals(0L, bunkerLumCount);
        assertTrue(towerBp.stream().noneMatch(v -> v.pos().equals(core)));

        for (var bp : List.of(towerBp, wideTowerBp, hallBp, bunkerBp)) {
            java.util.Set<BlockPos> placedSet = new java.util.HashSet<>();
            for (var v : bp) {
                placedSet.add(v.pos());
            }
            for (var v : bp) {
                if (v.role() == com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.BlueprintVoxelRole.HANGING_TENDRIL) {
                    assertTrue(placedSet.contains(v.pos().above()));
                }
            }
            java.util.Set<BlockPos> visited = new java.util.HashSet<>();
            java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
            visited.add(core.below());
            queue.add(core.below());
            while (!queue.isEmpty()) {
                BlockPos cur = queue.poll();
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    BlockPos next = cur.relative(dir);
                    if (placedSet.contains(next) && visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            assertEquals(placedSet.size(), visited.size());
        }
    }

    @Test
    void overworldNetworkGatingAndCombinedLightingPoliciesWork() {
        assertEquals(0.08, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeOverworldActivityMultiplier(0.0, false, false), 1.0E-6);
        assertEquals(0.08, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeOverworldActivityMultiplier(19.99, false, false), 1.0E-6);
        assertEquals(0.08, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeOverworldActivityMultiplier(Double.NaN, false, false), 1.0E-6);
        assertEquals(0.45, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeOverworldActivityMultiplier(20.0, false, false), 1.0E-6);
        assertEquals(1.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeOverworldActivityMultiplier(35.0, false, false), 1.0E-6);
        assertEquals(1.35, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeOverworldActivityMultiplier(60.0, false, false), 1.0E-6);
        assertEquals(1.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeOverworldActivityMultiplier(0.0, true, false), 1.0E-6);
        assertEquals(1.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeOverworldActivityMultiplier(0.0, false, true), 1.0E-6);

        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldAllowOverworldNaturalSpawn(0.0, true, false, false, 0.99));
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldAllowOverworldNaturalSpawn(0.0, false, true, false, 0.99));
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldAllowOverworldNaturalSpawn(0.0, false, false, true, 0.99));
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldAllowOverworldNaturalSpawn(35.0, false, false, false, 0.99));
        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldAllowOverworldNaturalSpawn(0.0, false, false, false, 0.079));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldAllowOverworldNaturalSpawn(0.0, false, false, false, 0.08));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldAllowOverworldNaturalSpawn(0.0, false, false, false, -0.01));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldAllowOverworldNaturalSpawn(0.0, false, false, false, Double.NaN));

        assertTrue(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldDeferOverworldRaidOutsideNetwork(0.0, false, false, true));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldDeferOverworldRaidOutsideNetwork(20.0, false, false, true));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldDeferOverworldRaidOutsideNetwork(0.0, true, false, true));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldDeferOverworldRaidOutsideNetwork(0.0, false, true, true));
        assertFalse(com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .shouldDeferOverworldRaidOutsideNetwork(0.0, false, false, false));

        assertEquals(0.4, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeCombinedLightDensity(0, 0, 0.4), 1.0E-6);
        assertEquals(0.5, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeCombinedLightDensity(0, 0, Double.NaN), 1.0E-6);
        assertEquals(1.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeCombinedLightDensity(0, 0, 2.0), 1.0E-6);
        assertEquals(0.0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeCombinedLightDensity(0, 0, -1.0), 1.0E-6);
        assertEquals(0.79, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeCombinedLightDensity(5, 30, 0.4), 1.0E-6);
        assertEquals(0.07, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                .computeCombinedLightDensity(0, 20, 0.2), 1.0E-6);

        assertEquals(0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(Double.NaN));
        assertEquals(0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(0.0));
        assertEquals(0, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(49.99));
        assertEquals(1, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(50.0));
        assertEquals(1, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(64.99));
        assertEquals(2, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(65.0));
        assertEquals(2, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(84.99));
        assertEquals(3, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(85.0));
        assertEquals(3, com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.computeVeinStrength(100.0));
    }

    @Test
    void hyphalVeinRedstoneAndCodexGateUnlockRespectEffectiveActAndBounds() {
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(-2, false));
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(0, false));
        assertEquals(3, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(0, true));
        assertEquals(4, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(1, false));
        assertEquals(8, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(2, false));
        assertEquals(11, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(2, true));
        assertEquals(12, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(3, false));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(3, true));
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(9, true));

        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked(null, 1, false, false, false));
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("", 1, false, false, false));
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("act_4", 1, false, false, true));
        assertFalse(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("act_2", 1, false, false, false));
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("act_2", 2, false, false, false));
        assertFalse(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("act_3", 2, false, false, false));
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("act_3", 3, false, false, false));
        assertFalse(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("act_4", 3, false, false, false));
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("act_4", 4, false, false, false));
        assertFalse(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("deep_mycelium", 4, false, false, false));
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("deep_mycelium", 1, true, false, false));
        assertFalse(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("admin", 4, true, false, false));
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("admin", 1, false, true, false));
        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.isCodexGateUnlocked("custom", 1, false, false, false));
    }

    @Test
    void hyphalPhysicsSupercoverAndRootNutrientRulesHold() {
        var line = com.alex.mycelialmind.hive.HyphalPhysics.supercoverLine(
                new BlockPos(0, 64, 0), new BlockPos(3, 64, 2), 16);
        assertFalse(line.isEmpty());
        assertEquals(new BlockPos(0, 64, 0), line.get(0));
        assertEquals(new BlockPos(3, 64, 2), line.get(line.size() - 1));
        boolean sawDiagonal = false;
        for (int i = 1; i < line.size(); i++) {
            BlockPos prev = line.get(i - 1);
            BlockPos cur = line.get(i);
            int dx = Math.abs(cur.getX() - prev.getX());
            int dy = Math.abs(cur.getY() - prev.getY());
            int dz = Math.abs(cur.getZ() - prev.getZ());
            assertTrue(Math.max(dx, Math.max(dy, dz)) <= 1, "Supercover line must have no gaps");
            if (dx == 1 && dz == 1) {
                sawDiagonal = true;
            }
        }
        assertTrue(sawDiagonal, "Supercover line (0,0)->(3,2) should include diagonal steps");

        // Teleport distance (>16) should not interpolate
        var teleportLine = com.alex.mycelialmind.hive.HyphalPhysics.supercoverLine(
                new BlockPos(0, 64, 0), new BlockPos(25, 64, 0), 16);
        assertEquals(1, teleportLine.size());

        // Root threshold & nutrient attenuation
        assertFalse(com.alex.mycelialmind.hive.HyphalPhysics.isRootCandidate(4, 5));
        assertTrue(com.alex.mycelialmind.hive.HyphalPhysics.isRootCandidate(5, 5));
        assertEquals(15, com.alex.mycelialmind.hive.HyphalPhysics.nextNutrient(0, true));
        assertEquals(14, com.alex.mycelialmind.hive.HyphalPhysics.nextNutrient(15, false));
        assertEquals(0, com.alex.mycelialmind.hive.HyphalPhysics.nextNutrient(1, false));
        assertEquals(0, com.alex.mycelialmind.hive.HyphalPhysics.nextNutrient(0, false));

        // Traversal thickening & charge threshold
        assertEquals(0, com.alex.mycelialmind.hive.HyphalPhysics.strengthForTraversals(4));
        assertEquals(1, com.alex.mycelialmind.hive.HyphalPhysics.strengthForTraversals(5));
        assertEquals(2, com.alex.mycelialmind.hive.HyphalPhysics.strengthForTraversals(10));
        assertEquals(3, com.alex.mycelialmind.hive.HyphalPhysics.strengthForTraversals(20));
        assertFalse(com.alex.mycelialmind.hive.HyphalPhysics.chargedForNutrient(7));
        assertTrue(com.alex.mycelialmind.hive.HyphalPhysics.chargedForNutrient(8));

        // Diagonal & face masks
        assertEquals(com.alex.mycelialmind.hive.HyphalPhysics.DIAG_SE,
                com.alex.mycelialmind.hive.HyphalPhysics.diagonalBit(1, 1));
        assertEquals(com.alex.mycelialmind.hive.HyphalPhysics.DIAG_NW,
                com.alex.mycelialmind.hive.HyphalPhysics.oppositeDiagonalBit(1, 1));
        assertEquals(com.alex.mycelialmind.hive.HyphalPhysics.FACE_DOWN,
                com.alex.mycelialmind.hive.HyphalPhysics.effectiveFaces((byte) 0));
    }

    @Test
    void neoforgeTraceLootSpecsAndBossPartDamageMathHold() {
        var rng = net.minecraft.util.RandomSource.create(12345L);
        java.util.Set<String> undergroundItems = new java.util.HashSet<>();
        java.util.Set<String> deepItems = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            var u = com.alex.mycelialmind.world.MycelialLootSystem.rollTraceDropSpec("underground_traces", rng);
            assertNotNull(u);
            assertTrue(u.count() >= 1 && u.count() <= 4);
            undergroundItems.add(u.itemId());

            var d = com.alex.mycelialmind.world.MycelialLootSystem.rollTraceDropSpec("deep_traces", rng);
            assertNotNull(d);
            assertTrue(d.count() >= 1 && d.count() <= 4);
            deepItems.add(d.itemId());
        }
        assertEquals(4, undergroundItems.size());
        assertEquals(5, deepItems.size());
        assertNull(com.alex.mycelialmind.world.MycelialLootSystem.rollTraceDropSpec("unknown", rng));
        assertNull(com.alex.mycelialmind.world.MycelialLootSystem.rollTraceDropSpec("underground_traces", null));

        assertEquals(13.5F, SynapticCommandSystem.computeBossPartDamage(
                true, false, false, false, 10.0F), 1.0E-4F);
        assertEquals(7.5F, SynapticCommandSystem.computeBossPartDamage(
                false, true, false, false, 10.0F), 1.0E-4F);
        assertEquals(10.0F, SynapticCommandSystem.computeBossPartDamage(
                false, true, true, false, 10.0F), 1.0E-4F);
        assertEquals(10.0F, SynapticCommandSystem.computeBossPartDamage(
                false, true, false, true, 10.0F), 1.0E-4F);
        assertEquals(0.0F, SynapticCommandSystem.computeBossPartDamage(
                true, false, false, false, -5.0F), 1.0E-4F);
        assertEquals(0.0F, SynapticCommandSystem.computeBossPartDamage(
                true, false, false, false, Float.NaN), 1.0E-4F);
    }

    @Test
    void ongoingResearchObservationAndNextStepEvaluationHoldAllBranches() {
        // CreatureObservation & ExpeditionGoal id mapping
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.NONE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.byId(-99));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.CONFIRMED_PROJECTILE_ADAPTATION,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.byId(3));
        assertEquals("message.mycelialmind.scanner_obs.confirmed_projectile",
                com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.CONFIRMED_PROJECTILE_ADAPTATION.translationKey());
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.NONE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.byId(99));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.byId(1));
        assertEquals("use_to_advantage",
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE.key());
        assertEquals("codex.mycelialmind.research.goal.reclaim_cave",
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE.translationKey());

        // Observations across pre-combat, learning, confirmed, and other dominant stimulus
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.INSUFFICIENT_EVIDENCE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        false, false, false, 0.0D, null, 0.0D, 70.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.INSUFFICIENT_EVIDENCE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        false, false, false, 5.0D, com.alex.mycelialmind.hive.StimulusType.PROJECTILE, 9.9D, 70.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.LEARNING_PROJECTILE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        false, false, false, 10.0D, com.alex.mycelialmind.hive.StimulusType.PROJECTILE, 10.0D, 70.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.LEARNING_PROJECTILE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        false, true, false, 75.0D, com.alex.mycelialmind.hive.StimulusType.PROJECTILE, 75.0D, 70.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.LEARNING_PROJECTILE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        false, false, true, 12.0D, com.alex.mycelialmind.hive.StimulusType.MELEE, 25.0D, 70.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.OTHER_DOMINANT_RESPONSE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        false, false, false, 0.0D, com.alex.mycelialmind.hive.StimulusType.MELEE, 25.0D, 70.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.OTHER_DOMINANT_RESPONSE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        false, false, false, Double.NaN, com.alex.mycelialmind.hive.StimulusType.FIRE, 75.0D, Double.NaN));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.CONFIRMED_PROJECTILE_ADAPTATION,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        true, false, false, 70.0D, com.alex.mycelialmind.hive.StimulusType.PROJECTILE, 70.0D, 70.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.CreatureObservation.CONFIRMED_PROJECTILE_ADAPTATION,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveCreatureObservation(
                        false, true, true, 75.0D, com.alex.mycelialmind.hive.StimulusType.PROJECTILE, 75.0D, 70.0D));

        // Next steps: pre-adaptation and unpinned
        int identified = com.alex.mycelialmind.hive.ProgressionGoalEngine.RESEARCH_BIT_ADAPTATION_IDENTIFIED;
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.AWAITING_FIRST_ADAPTATION,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE,
                        0, true, true, true, true, true, true, true, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.CHOOSE_EXPEDITION_GOAL,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        null, identified, true, true, false, false, false, false, false, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.CHOOSE_EXPEDITION_GOAL,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.NONE,
                        identified, true, true, false, false, false, false, false, 20.0D));

        // USE_TO_ADVANTAGE branch
        var goalAdv = com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE;
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.MISSING_BIO_EXTRACTOR,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalAdv, identified, false, true, false, false, false, false, false, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.WEAKEN_AND_EXTRACT_SAMPLE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalAdv, identified, true, true, false, false, false, false, false, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_MISSING_BIOMASS,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalAdv, identified, true, true, true, false, true, true, true, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_MISSING_CATALYST,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalAdv, identified | com.alex.mycelialmind.hive.ProgressionGoalEngine.RESEARCH_BIT_SAMPLE_EXTRACTED,
                        true, true, false, true, false, false, true, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_MISSING_BENCH,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalAdv, identified, true, true, true, true, false, true, false, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_TO_SYNTHESIZE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalAdv, identified, true, true, true, true, false, true, true, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.COMPLETED_BIOTECH_SYNTHESIS,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalAdv, identified | com.alex.mycelialmind.hive.ProgressionGoalEngine.RESEARCH_BIT_BIOTECH_SYNTHESIZED,
                        false, false, false, false, false, false, false, 20.0D));

        // RECLAIM_CAVE branch
        var goalCave = com.alex.mycelialmind.hive.ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE;
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.MISSING_BIO_SCANNER,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalCave, identified, true, false, false, false, false, false, false, 20.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.THREAT_ALREADY_ZERO,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalCave, identified, true, true, false, false, false, false, false, 0.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.THREAT_ALREADY_ZERO,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalCave, identified, true, true, false, false, false, false, false, Double.NaN));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.LOCATE_NODE_AND_RECORD_THREAT,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalCave, identified, true, true, false, false, false, false, false, 15.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.DESTROY_LOCATED_NODE,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalCave, identified | com.alex.mycelialmind.hive.ProgressionGoalEngine.RESEARCH_BIT_NODE_THREAT_RECORDED,
                        true, true, false, false, false, false, false, 15.0D));
        assertEquals(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.COMPLETED_NODE_DESTRUCTION,
                com.alex.mycelialmind.hive.ProgressionGoalEngine.resolveResearchNextStep(
                        goalCave, identified | com.alex.mycelialmind.hive.ProgressionGoalEngine.RESEARCH_BIT_NODE_DESTROYED,
                        true, false, false, false, false, false, false, 0.0D));

        assertTrue(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_MISSING_BIOMASS.isWarning());
        assertFalse(com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.WEAKEN_AND_EXTRACT_SAMPLE.isWarning());
        assertEquals("sample_ready_missing_biomass",
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_MISSING_BIOMASS.id());
        assertEquals("codex.mycelialmind.research.step.sample_ready_missing_biomass",
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_MISSING_BIOMASS.translationKey());
        assertEquals("hud.mycelialmind.research.step.sample_ready_missing_biomass",
                com.alex.mycelialmind.hive.ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_MISSING_BIOMASS.hudTranslationKey());
    }

    @Test
    void synapticMimicArchitecturePersonaStanceMutagenAndSabotageRulesHold() {
        var unmasked = com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.evaluatePersona(
                true, false, true, true, true);
        assertEquals(com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.MimicPersona.UNMASKED_DUELIST, unmasked);
        assertFalse(unmasked.isCovert());

        var desynced = com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.evaluatePersona(
                false, true, true, false, false);
        assertEquals(com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.MimicPersona.UNMASKED_DUELIST, desynced);

        var saboteur = com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.evaluatePersona(
                false, false, true, true, false);
        assertEquals(com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.MimicPersona.NETWORK_SABOTEUR, saboteur);

        var spoof = com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.evaluatePersona(
                false, false, false, true, true);
        assertEquals(com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.MimicPersona.INFILTRATION_SPOOF, spoof);
        assertTrue(spoof.isCovert());

        var lure = com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.evaluatePersona(
                false, false, false, false, true);
        assertEquals(com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.MimicPersona.ECHO_LURE, lure);
        assertTrue(lure.isCovert());

        // Habit counter-stance selection when gear is neutral
        assertEquals(com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.SPORE_MARKSMAN,
                com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.evaluateCombatStance(
                        false, false, 0, null, java.util.Set.of(com.alex.mycelialmind.hive.PlayerHabit.HIGH_MOBILITY)));
        assertEquals(com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.AEGIS_MIRROR,
                com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.evaluateCombatStance(
                        false, false, 0, null, java.util.Set.of(com.alex.mycelialmind.hive.PlayerHabit.SHADOW_INFILTRATOR)));

        // Parasitized SymbioticMutagen mirror selection & clearing on Desync
        assertEquals(com.alex.mycelialmind.symbiosis.SymbioticMutagen.KINETIC_STRAFE,
                com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.selectParasitizedMutagen(
                        java.util.Set.of(
                                com.alex.mycelialmind.symbiosis.SymbioticMutagen.CHITIN_THORNS,
                                com.alex.mycelialmind.symbiosis.SymbioticMutagen.KINETIC_STRAFE),
                        com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.SPORE_MARKSMAN,
                        false));
        assertNull(com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.selectParasitizedMutagen(
                java.util.Set.of(com.alex.mycelialmind.symbiosis.SymbioticMutagen.CHITIN_THORNS),
                com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.BLADE_DANCER,
                true));

        // Deceptive false stimulus avoids repeating actual dominant
        assertNotEquals(com.alex.mycelialmind.hive.StimulusType.PROJECTILE,
                com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.selectDeceptiveFalseStimulus(
                        com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.SPORE_MARKSMAN,
                        com.alex.mycelialmind.hive.StimulusType.PROJECTILE));

        assertEquals(85, com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.computeSymbioticStabilityDrain(100,
                com.alex.mycelialmind.hive.EvolutionRank.BASE));
        assertEquals(75, com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.computeSymbioticStabilityDrain(100,
                com.alex.mycelialmind.hive.EvolutionRank.ELITE));
        assertEquals(1.25F, com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.computeUnmaskShockDamageMultiplier(true, true), 1.0E-4F);
        assertEquals("mycelialmind:false_engram_injector",
                com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.computeMimicCounterIntelDropId(
                        com.alex.mycelialmind.hive.EvolutionRank.ELITE, true, 0.10F));
    }

    @Test
    void bossCombatArchitectureSelectionCombosAndCarapaceMathWork() {
        var kit = List.of(
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_CHARGE,
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_SLAM,
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SPORE_RAIN,
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_MYCELIAL_SHIELD
        );

        // Adaptive utility selection: RANGED_KITER + PROJECTILE at 10m favors Seismic Charge gap-closer
        var pickedRanged = com.alex.mycelialmind.hive.BossCombatArchitectureEngine.selectNextAttack(
                kit,
                1,
                10.0D,
                java.util.Map.of(),
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.NONE,
                com.alex.mycelialmind.hive.StimulusType.PROJECTILE,
                java.util.Set.of(com.alex.mycelialmind.hive.PlayerHabit.HIGH_MOBILITY),
                false
        );
        assertEquals(com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_CHARGE, pickedRanged);

        // Anti-spam penalty shifts selection away from lastAttack when another eligible option exists
        var pickedAntiSpam = com.alex.mycelialmind.hive.BossCombatArchitectureEngine.selectNextAttack(
                kit,
                1,
                10.0D,
                java.util.Map.of(),
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_CHARGE,
                com.alex.mycelialmind.hive.StimulusType.FIRE,
                java.util.Set.of(),
                false
        );
        assertEquals(com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SPORE_RAIN, pickedAntiSpam);

        // Single-step combo chaining in advanced phases only
        assertEquals(com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.NONE,
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveComboFollowUp(
                        com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_CHARGE, 1, 5.0D, false));
        assertEquals(com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_SLAM,
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveComboFollowUp(
                        com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_CHARGE, 2, 5.0D, false));
        assertEquals(com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.NONE,
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveComboFollowUp(
                        com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_CHARGE, 2, 5.0D, true));
        assertEquals(com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.HIVE_CORE_SPORE_SHOCKWAVE,
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveComboFollowUp(
                        com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.HIVE_CORE_SYNAPTIC_SWEEP, 3, 6.0D, false));
        assertEquals(com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.MATRIARCH_SYNAPTIC_STORM,
                com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveComboFollowUp(
                        com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.MATRIARCH_ECHO_TELEPORT, 3, 6.0D, false));

        // Carapace intact vs broken damage multipliers
        assertEquals(7.5F, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.computeBossPartDamageWithCarapace(
                false, true, false, false, false, 10.0F), 1.0E-4F);
        assertEquals(10.0F, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.computeBossPartDamageWithCarapace(
                false, true, true, false, false, 10.0F), 1.0E-4F);
        assertEquals(13.5F, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.computeBossPartDamageWithCarapace(
                true, false, false, false, false, 10.0F), 1.0E-4F);
        assertEquals(15.5F, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.computeBossPartDamageWithCarapace(
                true, false, true, false, false, 10.0F), 1.0E-4F);

        // Phase thresholds
        assertEquals(1, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveBehemothPhase(0.55F));
        assertEquals(2, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveBehemothPhase(0.50F));
        assertEquals(2, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveHiveCorePhase(0.60F, false));
        assertEquals(3, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveHiveCorePhase(0.28F, true));
        assertEquals(3, com.alex.mycelialmind.hive.BossCombatArchitectureEngine.resolveMatriarchPhase(0.20F));
    }

    @Test
    void sovereignNexusRedstoneAndSblUtilityWeightsHold() {
        assertEquals(0, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.sovereignNexusSignal(0, 0, 4, 0));
        assertEquals(1, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.sovereignNexusSignal(0, 0, 4, 1));
        int t1 = com.alex.mycelialmind.util.MycelialRedstoneTelemetry.sovereignNexusSignal(1, 0, 4, 0);
        int t2 = com.alex.mycelialmind.util.MycelialRedstoneTelemetry.sovereignNexusSignal(2, 4, 8, 128);
        int t3 = com.alex.mycelialmind.util.MycelialRedstoneTelemetry.sovereignNexusSignal(3, 12, 12, 512);
        assertTrue(t1 >= 1 && t1 < t2 && t2 < t3 && t3 == 15);
        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.sovereignNexusSignal(9, 99, 12, 9999));

        assertEquals(15, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.regenerativeFloraSignal(true));
        assertEquals(5, com.alex.mycelialmind.util.MycelialRedstoneTelemetry.regenerativeFloraSignal(false));

        assertEquals(0.0F, HiveUtilityEvaluator.combatXpChargeBonus(-5), 1.0E-6F);
        assertEquals(0.05F, HiveUtilityEvaluator.combatXpChargeBonus(50), 1.0E-6F);
        assertEquals(0.10F, HiveUtilityEvaluator.combatXpChargeBonus(100), 1.0E-6F);
        assertEquals(0.10F, HiveUtilityEvaluator.combatXpChargeBonus(999), 1.0E-6F);

        assertEquals(0.08F, HiveUtilityEvaluator.stimulusKiteOffset(com.alex.mycelialmind.hive.StimulusType.MELEE), 1.0E-6F);
        assertEquals(-0.05F, HiveUtilityEvaluator.stimulusKiteOffset(com.alex.mycelialmind.hive.StimulusType.PROJECTILE), 1.0E-6F);
        assertEquals(0.0F, HiveUtilityEvaluator.stimulusKiteOffset(com.alex.mycelialmind.hive.StimulusType.MAGIC), 1.0E-6F);
        assertEquals(0.0F, HiveUtilityEvaluator.stimulusKiteOffset(null), 1.0E-6F);

        assertEquals(0.0F, HiveUtilityEvaluator.stimulusRegroupBonus(null, 0), 1.0E-6F);
        assertEquals(0.08F, HiveUtilityEvaluator.stimulusRegroupBonus(com.alex.mycelialmind.hive.StimulusType.FIRE, 0), 1.0E-6F);
        assertEquals(0.14F, HiveUtilityEvaluator.stimulusRegroupBonus(com.alex.mycelialmind.hive.StimulusType.EXPLOSION, 150), 1.0E-6F);
    }

    @Test
    void reactiveLoopFadingDuckingBossTensionAndNexusHumMathHold() {
        assertEquals(1.0F, EvolutionRank.computeAudioDuckingMultiplier(0, 40, 0.55F), 1.0E-5F);
        assertEquals(0.45F, EvolutionRank.computeAudioDuckingMultiplier(40, 40, 0.55F), 1.0E-5F);
        assertEquals(0.725F, EvolutionRank.computeAudioDuckingMultiplier(20, 40, 0.55F), 1.0E-5F);
        assertEquals(0.15F, EvolutionRank.computeAudioDuckingMultiplier(40, 40, 2.0F), 1.0E-5F);

        assertEquals(0.04F, EvolutionRank.computeSmoothFadeVolume(0.0F, 0.50F, 0.04F), 1.0E-5F);
        assertEquals(0.50F, EvolutionRank.computeSmoothFadeVolume(0.48F, 0.50F, 0.04F), 1.0E-5F);
        assertEquals(0.26F, EvolutionRank.computeSmoothFadeVolume(0.30F, 0.0F, 0.04F), 1.0E-5F);

        assertEquals(0.0F, EvolutionRank.computeBossTensionBedVolume(false, 0.5F, 1.0F, 1.0F), 1.0E-5F);
        float bossFullHp = EvolutionRank.computeBossTensionBedVolume(true, 1.0F, 1.0F, 1.0F);
        float bossLowHp = EvolutionRank.computeBossTensionBedVolume(true, 0.1F, 1.0F, 1.0F);
        float bossDucked = EvolutionRank.computeBossTensionBedVolume(true, 0.1F, 1.0F, 0.5F);
        assertTrue(bossLowHp > bossFullHp && bossFullHp > 0.25F);
        assertEquals(bossLowHp * 0.5F, bossDucked, 1.0E-4F);

        assertEquals(0.0F, EvolutionRank.computeSovereignNexusHumVolume(700.0D, 24.0D, 1.0F, 1.0F), 1.0E-5F);
        float nexusNear = EvolutionRank.computeSovereignNexusHumVolume(4.0D, 24.0D, 1.0F, 1.0F);
        float nexusFar = EvolutionRank.computeSovereignNexusHumVolume(400.0D, 24.0D, 1.0F, 1.0F);
        assertTrue(nexusNear > nexusFar && nexusFar > 0.05F);
    }
}
