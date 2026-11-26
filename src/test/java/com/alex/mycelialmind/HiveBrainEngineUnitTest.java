package com.alex.mycelialmind;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.ai.HiveAiBlackboard;
import com.alex.mycelialmind.entity.ai.HiveTacticalTree;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AvoidEntity;
import com.alex.mycelialmind.entity.ai.brain.behaviour.FleeTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.StrafeTarget;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBehaviorTree;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBtDynamicGuardSelector;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBtLeafTask;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBtSelector;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBtSequence;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBtStatus;
import com.alex.mycelialmind.entity.ai.brain.core.AllApplicableBehaviours;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainBlackboard;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainKey;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainScheduler;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainSignal;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainSnapshot;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainTrace;
import com.alex.mycelialmind.entity.ai.brain.core.HiveResourceArbiter;
import com.alex.mycelialmind.entity.ai.brain.core.HiveResourceChannel;
import com.alex.mycelialmind.entity.ai.brain.core.UtilitySelectorBehaviour;
import com.alex.mycelialmind.entity.ai.brain.sensor.IncomingProjectilesSensor;
import com.alex.mycelialmind.entity.ai.brain.steering.HiveSteeringEngine;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveDoctrine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiveBrainEngineUnitTest {

    @Test
    void sensorSchedulingAlwaysRunsInitiallyAndRespectsScanRate() {
        assertTrue(HiveBrainScheduler.shouldRunSensor(0L, HiveBrainScheduler.INITIAL_TICK_SENTINEL));
        assertTrue(HiveBrainScheduler.shouldRunSensor(100L, HiveBrainScheduler.INITIAL_TICK_SENTINEL));

        long next = HiveBrainScheduler.computeNextTickTime(100L, 20);
        assertEquals(120L, next);
        assertFalse(HiveBrainScheduler.shouldRunSensor(100L, next));
        assertFalse(HiveBrainScheduler.shouldRunSensor(119L, next));
        assertTrue(HiveBrainScheduler.shouldRunSensor(120L, next));
        assertTrue(HiveBrainScheduler.shouldRunSensor(150L, next));

        // Clamps zero/negative scan rates to at least 1 tick and protects against Long.MAX_VALUE overflow
        assertEquals(11L, HiveBrainScheduler.computeNextTickTime(10L, 0));
        assertEquals(11L, HiveBrainScheduler.computeNextTickTime(10L, -5));
        assertEquals(Long.MAX_VALUE, HiveBrainScheduler.computeNextTickTime(Long.MAX_VALUE - 5L, 20));
    }

    @Test
    void cooldownAndTimeoutBoundsHandleNoTimeoutAndOverflow() {
        assertFalse(HiveBrainScheduler.isOnCooldown(0L, HiveBrainScheduler.INITIAL_TICK_SENTINEL));

        long cooldownEnd = HiveBrainScheduler.computeCooldownFinishedAt(50L, 40);
        assertEquals(90L, cooldownEnd);
        assertTrue(HiveBrainScheduler.isOnCooldown(50L, cooldownEnd));
        assertTrue(HiveBrainScheduler.isOnCooldown(89L, cooldownEnd));
        assertFalse(HiveBrainScheduler.isOnCooldown(90L, cooldownEnd));

        assertEquals(HiveBrainScheduler.INITIAL_TICK_SENTINEL,
                HiveBrainScheduler.computeCooldownFinishedAt(50L, 0));
        assertEquals(HiveBrainScheduler.INITIAL_TICK_SENTINEL,
                HiveBrainScheduler.computeCooldownFinishedAt(50L, -10));
        assertEquals(Long.MAX_VALUE,
                HiveBrainScheduler.computeCooldownFinishedAt(Long.MAX_VALUE - 2L, 10));

        // Timeout handling including Integer.MAX_VALUE (noTimeout) and zero runtime
        long noTimeoutEnd = HiveBrainScheduler.computeEndTimestamp(100L, Integer.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, noTimeoutEnd);
        assertFalse(HiveBrainScheduler.isTimedOut(Long.MAX_VALUE, noTimeoutEnd));

        long zeroEnd = HiveBrainScheduler.computeEndTimestamp(100L, 0);
        assertEquals(100L, zeroEnd);
        assertFalse(HiveBrainScheduler.isTimedOut(100L, zeroEnd));
        assertTrue(HiveBrainScheduler.isTimedOut(101L, zeroEnd));

        long clampedNegEnd = HiveBrainScheduler.computeEndTimestamp(100L, -5);
        assertEquals(100L, clampedNegEnd);
        assertEquals(Long.MAX_VALUE, HiveBrainScheduler.computeEndTimestamp(Long.MAX_VALUE - 3L, 10));
    }

    @Test
    void weightedSelectionAndSequentialProgressionAreDeterministic() {
        assertEquals(-1, HiveBrainScheduler.selectWeightedIndex(null, 0));
        assertEquals(-1, HiveBrainScheduler.selectWeightedIndex(new int[0], 0));
        assertEquals(-1, HiveBrainScheduler.selectWeightedIndex(new int[]{0, -3, 0}, 5));

        int[] weights = {1, 0, 3, -2, 2}; // total positive weight = 6 (indices 0:[0], 2:[1,2,3], 4:[4,5])
        assertEquals(0, HiveBrainScheduler.selectWeightedIndex(weights, 0));
        assertEquals(2, HiveBrainScheduler.selectWeightedIndex(weights, 1));
        assertEquals(2, HiveBrainScheduler.selectWeightedIndex(weights, 3));
        assertEquals(4, HiveBrainScheduler.selectWeightedIndex(weights, 4));
        assertEquals(4, HiveBrainScheduler.selectWeightedIndex(weights, 5));
        assertEquals(0, HiveBrainScheduler.selectWeightedIndex(weights, 6));
        assertTrue(HiveBrainScheduler.selectWeightedIndex(weights, Integer.MIN_VALUE) >= 0);

        assertEquals(-1, HiveBrainScheduler.nextSequentialIndex(0, 0));
        assertEquals(1, HiveBrainScheduler.nextSequentialIndex(0, 3));
        assertEquals(2, HiveBrainScheduler.nextSequentialIndex(1, 3));
        assertEquals(-1, HiveBrainScheduler.nextSequentialIndex(2, 3));
    }

    @Test
    void incomingProjectilesSensorHeadingHelperDistinguishesApproachingAndRecedingProjectiles() {
        Vec3 entityPos = new Vec3(6.0D, 64.0D, 0.0D);
        Vec3 projPos = new Vec3(0.0D, 64.0D, 0.0D);

        assertTrue(IncomingProjectilesSensor.isProjectileHeadingToward(
                projPos, new Vec3(1.5D, 0.0D, 0.0D), entityPos, IncomingProjectilesSensor.DEFAULT_MIN_HEADING_DOT));
        assertFalse(IncomingProjectilesSensor.isProjectileHeadingToward(
                projPos, new Vec3(0.0D, 0.0D, 1.5D), entityPos, IncomingProjectilesSensor.DEFAULT_MIN_HEADING_DOT));
        assertFalse(IncomingProjectilesSensor.isProjectileHeadingToward(
                projPos, new Vec3(-1.5D, 0.0D, 0.0D), entityPos, IncomingProjectilesSensor.DEFAULT_MIN_HEADING_DOT));
        // Stationary / embedded projectile below speed squared threshold
        assertFalse(IncomingProjectilesSensor.isProjectileHeadingToward(
                projPos, new Vec3(0.1D, 0.0D, 0.0D), entityPos, IncomingProjectilesSensor.DEFAULT_MIN_HEADING_DOT));
        // Coincident position while in flight
        assertTrue(IncomingProjectilesSensor.isProjectileHeadingToward(
                entityPos, new Vec3(0.8D, 0.0D, 0.0D), entityPos, IncomingProjectilesSensor.DEFAULT_MIN_HEADING_DOT));
        assertFalse(IncomingProjectilesSensor.isProjectileHeadingToward(null, Vec3.ZERO, entityPos, 0.35D));
    }

    @Test
    void reactToUnreachableWindowHelperChecksExactBoundary() {
        assertFalse(ReactToUnreachableTarget.hasExceededUnreachableWindow(100L, 100L, 40L));
        assertFalse(ReactToUnreachableTarget.hasExceededUnreachableWindow(139L, 100L, 40L));
        assertTrue(ReactToUnreachableTarget.hasExceededUnreachableWindow(140L, 100L, 40L));
        assertTrue(ReactToUnreachableTarget.hasExceededUnreachableWindow(200L, 100L, 40L));
        assertFalse(ReactToUnreachableTarget.hasExceededUnreachableWindow(100L, -1L, 40L));
        assertFalse(ReactToUnreachableTarget.hasExceededUnreachableWindow(100L, 50L, -1L));
    }

    @Test
    void newBehavioursSensorsAndMemoryIdExposeConfiguredPropertiesAndFluentChains() {
        IncomingProjectilesSensor<MycelialMob> sensor = new IncomingProjectilesSensor<MycelialMob>()
                .setRadius(18.5D);
        assertEquals(18.5D, sensor.getRadius(), 1.0E-4D);
        sensor.setRadius(0.1D);
        assertEquals(1.0D, sensor.getRadius(), 1.0E-4D);

        ReactToUnreachableTarget<MycelialMob> react = new ReactToUnreachableTarget<MycelialMob>()
                .afterTicks(25L)
                .clearAttackTarget(true);
        assertEquals(25L, react.getUnreachableThresholdTicks());
        assertTrue(react.isClearAttackTarget());

        StrafeTarget<MycelialMob> strafe = new StrafeTarget<MycelialMob>()
                .strafeDistance(14.0F)
                .speedMod(1.15F);
        assertEquals(14.0F, strafe.getStrafeDistance(), 1.0E-4F);
        assertEquals(1.15F, strafe.getSpeedMod(), 1.0E-4F);

        AvoidEntity<MycelialMob> avoid = new AvoidEntity<MycelialMob>()
                .noCloserThan(9.5F)
                .speedModifier(1.3F);
        assertEquals(9.5F, avoid.getNoCloserThan(), 1.0E-4F);
        assertEquals(1.3F, avoid.getSpeedModifier(), 1.0E-4F);

        FleeTarget<MycelialMob> flee = new FleeTarget<MycelialMob>()
                .fleeDistance(15.0F)
                .speedModifier(1.4F);
        assertEquals(15.0F, flee.getFleeDistance(), 1.0E-4F);
        assertEquals(1.4F, flee.getSpeedModifier(), 1.0E-4F);

        assertEquals(0.35D, IncomingProjectilesSensor.DEFAULT_MIN_HEADING_DOT, 1.0E-6D);
    }

    @Test
    void phasedSensorTicksDayWindowsAndUtilitySelectionWithHysteresisWorkDeterministically() {
        // Phased sensor offsets distribute entities across scanRate buckets without overflow
        assertEquals(101L, HiveBrainScheduler.computePhasedFirstTickTime(100L, 1, 7));
        assertEquals(101L, HiveBrainScheduler.computePhasedFirstTickTime(100L, 20, 0));
        assertEquals(106L, HiveBrainScheduler.computePhasedFirstTickTime(100L, 20, 5));
        assertEquals(118L, HiveBrainScheduler.computePhasedFirstTickTime(100L, 20, -3));
        assertEquals(Long.MAX_VALUE, HiveBrainScheduler.computePhasedFirstTickTime(Long.MAX_VALUE - 2L, 20, 5));

        // Minecraft 24000-tick day normalization and window matching (normal & overnight wrap)
        assertEquals(0, HiveBrainScheduler.normalizeDayTick(24000L));
        assertEquals(18000, HiveBrainScheduler.normalizeDayTick(-6000L));
        assertTrue(HiveBrainScheduler.isTickWithinWindow(6000, 1000, 12000));
        assertFalse(HiveBrainScheduler.isTickWithinWindow(13000, 1000, 12000));
        assertTrue(HiveBrainScheduler.isTickWithinWindow(23000, 22000, 2000));
        assertTrue(HiveBrainScheduler.isTickWithinWindow(500, 22000, 2000));
        assertFalse(HiveBrainScheduler.isTickWithinWindow(12000, 22000, 2000));

        // Utility selection with threshold, hysteresis bonus, NaN handling, and preemption
        assertEquals(-1, HiveBrainScheduler.selectHighestUtilityIndex(null, -1, 0.1F, 0.1F));
        assertEquals(-1, HiveBrainScheduler.selectHighestUtilityIndex(new float[]{0.05F, Float.NaN}, -1, 0.1F, 0.1F));

        float[] scores = {0.60F, 0.65F, Float.NaN};
        assertEquals(1, HiveBrainScheduler.selectHighestUtilityIndex(scores, -1, 0.1F, 0.10F));
        // Running index 0 gets +0.10 hysteresis (0.70 > 0.65), so it holds against 0.65
        assertEquals(0, HiveBrainScheduler.selectHighestUtilityIndex(scores, 0, 0.1F, 0.10F));
        // Once index 1 exceeds 0.70 (0.75 > 0.70), it preempts index 0
        scores[1] = 0.75F;
        assertEquals(1, HiveBrainScheduler.selectHighestUtilityIndex(scores, 0, 0.1F, 0.10F));
        // Running index below minThreshold gets NO hysteresis rescue
        scores[0] = 0.05F;
        scores[1] = 0.12F;
        assertEquals(1, HiveBrainScheduler.selectHighestUtilityIndex(scores, 0, 0.10F, 0.50F));
    }

    @Test
    void utilitySelectorAndReactiveAllApplicableExposeFluentConfiguration() {
        var b1 = new CustomBehaviour<MycelialMob>(mob -> {});
        var b2 = new CustomBehaviour<MycelialMob>(mob -> {});

        var selector = new UtilitySelectorBehaviour<MycelialMob>()
                .minScoreThreshold(0.05F)
                .hysteresisBonus(0.15F)
                .continuousPreemption(false)
                .add(b1, mob -> 0.5F)
                .add(b2, mob -> 0.8F);

        assertEquals(0.05F, selector.getMinScoreThreshold(), 1.0E-5F);
        assertEquals(0.15F, selector.getHysteresisBonus(), 1.0E-5F);
        assertFalse(selector.isContinuousPreemption());
        assertEquals(2, selector.getChildBehaviours().size());

        var allDefault = new AllApplicableBehaviours<MycelialMob>(b1, b2);
        assertFalse(allDefault.isReEvaluateStoppedChildren());
        allDefault.reEvaluateStoppedChildren(true);
        assertTrue(allDefault.isReEvaluateStoppedChildren());
    }

    @Test
    void nativeBehaviorTreeCompositesPreemptAndTrackLifecycleWithoutExternalDeps() {
        final class TestBb {
            boolean highPriority = false;
            int stepCount = 0;
            int endedCount = 0;
            int activeBranch = 0;
        }

        TestBb bb = new TestBb();
        var highLeaf = new HiveBtLeafTask<TestBb>() {
            @Override
            protected HiveBtStatus run(TestBb state) {
                state.activeBranch = 1;
                state.stepCount++;
                return HiveBtStatus.RUNNING;
            }

            @Override
            protected void end(TestBb state) {
                state.endedCount++;
            }
        }.setGuard(state -> state.highPriority);

        var lowLeaf = new HiveBtLeafTask<TestBb>() {
            @Override
            protected HiveBtStatus run(TestBb state) {
                state.activeBranch = 2;
                state.stepCount++;
                return HiveBtStatus.RUNNING;
            }

            @Override
            protected void end(TestBb state) {
                state.endedCount++;
            }
        };

        var dynamic = new HiveBtDynamicGuardSelector<TestBb>()
                .addChild(highLeaf)
                .addChild(lowLeaf);
        var tree = new HiveBehaviorTree<>(dynamic, bb);

        assertEquals(HiveBtStatus.RUNNING, tree.step());
        assertEquals(2, bb.activeBranch);
        assertEquals(0, bb.endedCount);

        bb.highPriority = true;
        assertEquals(HiveBtStatus.RUNNING, tree.step());
        assertEquals(1, bb.activeBranch);
        assertEquals(1, bb.endedCount);

        tree.reset();
        assertEquals(2, bb.endedCount);
        assertEquals(HiveBtStatus.FRESH, tree.getStatus());

        // Sequence & Selector check
        int[] seqOrder = {0};
        var seq = new HiveBtSequence<TestBb>()
                .addChild(new HiveBtLeafTask<>() {
                    @Override
                    protected HiveBtStatus run(TestBb state) {
                        seqOrder[0] += 1;
                        return HiveBtStatus.SUCCEEDED;
                    }
                })
                .addChild(new HiveBtLeafTask<>() {
                    @Override
                    protected HiveBtStatus run(TestBb state) {
                        seqOrder[0] += 10;
                        return HiveBtStatus.SUCCEEDED;
                    }
                });
        var selector = new HiveBtSelector<TestBb>()
                .addChild(new HiveBtLeafTask<TestBb>() {
                    @Override
                    protected HiveBtStatus run(TestBb state) {
                        return HiveBtStatus.FAILED;
                    }
                })
                .addChild(seq);
        assertEquals(HiveBtStatus.SUCCEEDED, new HiveBehaviorTree<>(selector, bb).step());
        assertEquals(11, seqOrder[0]);
    }

    @Test
    void nativeSteeringEngineProducesBoundedSeparationArrivePursuitRepulsionAndAttackSlots() {
        double[] nx = {2.0D};
        double[] nz = {0.0D};
        Vec3 sep = HiveSteeringEngine.computeSeparation(0.0D, 0.0D, nx, nz, 1, 8.0D, 4.0D);
        assertTrue(sep.x < 0.0D && Math.abs(sep.z) < 1.0E-6D);

        Vec3 coh = HiveSteeringEngine.computeCohesion(0.0D, 0.0D, nx, nz, 1, 2.0D);
        assertEquals(2.0D, coh.x, 1.0E-5D);

        Vec3 arriveFar = HiveSteeringEngine.computeArrive(
                0.0D, 0.0D, 0.0D, 0.0D, 10.0D, 0.0D, 0.6D, 3.5D, 2.2D, 4.0D, 0.15D);
        assertEquals(4.0D, arriveFar.x, 1.0E-5D);

        Vec3 arriveInside = HiveSteeringEngine.computeArrive(
                0.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.6D, 3.5D, 2.2D, 4.0D, 0.15D);
        assertEquals(0.0D, arriveInside.length(), 1.0E-6D);

        Vec3 pursuit = HiveSteeringEngine.computePursuit(0.0D, 0.0D, 5.0D, 0.0D, 0.0D, 1.0D, 2.0D, 3.0D);
        assertTrue(pursuit.x > 0.0D && pursuit.z > 0.0D);

        Vec3 evade = HiveSteeringEngine.computeEvade(0.0D, 0.0D, 5.0D, 0.0D, 0.0D, 1.0D, 2.0D, 3.0D);
        assertTrue(evade.x < 0.0D && evade.z < 0.0D);

        Vec3 pylonRepel = HiveSteeringEngine.computePylonRepulsion(3.0D, 0.0D, 0.0D, 0.0D, 6.0D, 1.0D);
        assertEquals(0.5D, pylonRepel.x, 1.0E-5D);

        Vec3 slot0 = HiveSteeringEngine.computeAttackSlotPosition(10.0D, 64.0D, 10.0D, 0, 4, 2.0D, 0.0D);
        Vec3 slot1 = HiveSteeringEngine.computeAttackSlotPosition(10.0D, 64.0D, 10.0D, 1, 4, 2.0D, 0.0D);
        assertEquals(12.0D, slot0.x, 1.0E-5D);
        assertEquals(12.0D, slot1.z, 1.0E-5D);

        Vec3 blended = HiveSteeringEngine.computeBlendedSwarmSteering(
                true, 0.0D, 0.0D, 0.0D, 0.0D, 8.0D, 0.0D,
                nx, nz, new double[]{0.0D}, new double[]{0.0D}, 1,
                true, 0.0D, 2.0D, 2.2D);
        assertTrue(blended.length() <= 2.2001D);
    }

    @Test
    void lodTickingAndTacticalTreeDoctrineTraumaAndGrudgeSignalsWorkCleanly() {
        assertTrue(HiveBrainScheduler.shouldTickBrainWithLod(0L, 5, 13L, false, false));
        assertTrue(HiveBrainScheduler.shouldTickBrainWithLod(10L, 5, 13L, true, false));
        assertTrue(HiveBrainScheduler.shouldTickBrainWithLod(10L, 5, 13L, false, true));
        assertTrue(HiveBrainScheduler.shouldTickBrainWithLod(10L, 3, 13L, false, false)); // (13+3)%4==0
        assertFalse(HiveBrainScheduler.shouldTickBrainWithLod(10L, 2, 13L, false, false)); // (13+2)%4==1

        HiveAiBlackboard bb = new HiveAiBlackboard();
        bb.hasTarget = true;
        bb.distToTarget = 8.0F;
        bb.health01 = 0.80F;
        float baseCharge = HiveTacticalTree.scoreCharge(bb);

        bb.doctrine = HiveDoctrine.SIEGE_ASSAULT;
        bb.hasTargetGrudge = true;
        bb.evolutionRank = EvolutionRank.ELITE;
        assertTrue(HiveTacticalTree.scoreCharge(bb) > baseCharge);

        bb.hasNearestNode = true;
        bb.health01 = 0.35F;
        float baseRegroup = HiveTacticalTree.scoreRegroup(bb);
        bb.doctrine = HiveDoctrine.RECOVERY;
        bb.influenceTrauma01 = 0.80F;
        assertTrue(HiveTacticalTree.scoreRegroup(bb) > baseRegroup);
    }

    @Test
    void hiveBrainKeyBlackboardTtlCooldownsAndNbtRoundTripWorkWithoutRegistryBootstrap() {
        HiveBrainKey<String> ephemeralKey = HiveBrainKey.of("test_ephemeral_tag", String.class);
        HiveBrainKey<Integer> intKey = HiveBrainKey.persistentInt("test_persistent_kills");
        HiveBrainKey<String> strKey = HiveBrainKey.persistentString("test_persistent_note");
        HiveBrainKey<BlockPos> posKey = HiveBrainKey.persistentBlockPos("test_persistent_anchor");
        HiveBrainKey<UUID> uuidKey = HiveBrainKey.persistentUuid("test_persistent_target");

        HiveBrainBlackboard board = new HiveBrainBlackboard();
        board.set(ephemeralKey, "temp");
        board.set(intKey, 7);
        board.setExpiring(strKey, "alert", 20, 100L);
        BlockPos anchor = new BlockPos(12, 64, -8);
        UUID targetId = UUID.fromString("12345678-1234-5678-9abc-123456789abc");
        board.set(posKey, anchor);
        board.set(uuidKey, targetId);
        board.setCooldown("slam", 100L, 40);

        assertEquals("temp", board.get(ephemeralKey, 100L));
        assertEquals(7, board.get(intKey, 100L));
        assertEquals("alert", board.get(strKey, 119L));
        assertTrue(board.isOnCooldown("slam", 100L));
        assertEquals(40L, board.getRemainingCooldown("slam", 100L));
        assertEquals(1, board.getActiveCooldownCount(100L));

        CompoundTag saved = board.saveToNbt(new CompoundTag(), 110L);
        HiveBrainBlackboard reloaded = new HiveBrainBlackboard();
        reloaded.loadFromNbt(saved);

        assertNull(reloaded.get(ephemeralKey, 110L));
        assertEquals(7, reloaded.get(intKey, 110L));
        assertEquals("alert", reloaded.get(strKey, 119L));
        assertNull(reloaded.get(strKey, 120L));
        assertEquals(anchor, reloaded.get(posKey, 200L));
        assertEquals(targetId, reloaded.get(uuidKey, 200L));
        assertTrue(reloaded.isOnCooldown("slam", 110L));
        assertEquals(30L, reloaded.getRemainingCooldown("slam", 110L));
        assertFalse(reloaded.isOnCooldown("slam", 140L));

        assertEquals(0, reloaded.pruneExpired(200L)); // get/isOnCooldown already lazily pruned expired entries
        reloaded.setExpiring(ephemeralKey, "x", 5, 200L);
        reloaded.setCooldown("temp_cd", 200L, 5);
        assertEquals(2, reloaded.pruneExpired(205L));
        reloaded.remove(posKey);
        assertFalse(reloaded.has(posKey, 200L));
    }

    @Test
    void hiveBrainSignalFactoriesLodWakeWindowAndSnapshotFormatWorkDeterministically() {
        BlockPos origin = new BlockPos(5, 70, -3);
        HiveBrainSignal acoustic = HiveBrainSignal.acousticPulse(origin, null, Float.NaN, 150L);
        assertEquals(HiveBrainSignal.SignalType.ACOUSTIC_PULSE, acoustic.type());
        assertEquals(origin, acoustic.sourcePos());
        assertEquals(0.0F, acoustic.intensity());
        assertFalse(acoustic.urgent());
        assertEquals("acoustic", acoustic.tag());

        HiveBrainSignal desync = HiveBrainSignal.desyncTriggered(100, 200L);
        assertEquals(HiveBrainSignal.SignalType.DESYNC_TRIGGERED, desync.type());
        assertTrue(desync.urgent());
        assertEquals(100.0F, desync.intensity());

        long wakeUntil = HiveBrainScheduler.computeLodWakeUntil(100L, 40);
        assertEquals(140L, wakeUntil);
        assertEquals(HiveBrainScheduler.INITIAL_TICK_SENTINEL, HiveBrainScheduler.computeLodWakeUntil(100L, 0));
        assertEquals(Long.MAX_VALUE, HiveBrainScheduler.computeLodWakeUntil(Long.MAX_VALUE - 5L, 40));

        // entityId=2, gameTime=139 -> (139+2)%4=1 (normally throttled), but woken until 140
        assertTrue(HiveBrainScheduler.shouldTickBrainWithLod(10L, 2, 139L, wakeUntil, false, false));
        assertFalse(HiveBrainScheduler.shouldTickBrainWithLod(10L, 2, 141L, wakeUntil, false, false));

        HiveBrainSnapshot snapshot = new HiveBrainSnapshot(
                "", null, 12L, 50L, 6L, 3L, null, "", List.of("fight:Melee"), 2, 4, true);
        String formatted = snapshot.formatCompact();
        assertTrue(formatted.contains("act=none"));
        assertTrue(formatted.contains("prof=default"));
        assertTrue(formatted.contains("sig=3 (NONE)"));
        assertTrue(formatted.contains("cd=2"));
        assertTrue(formatted.contains("lodSkip=6"));
        assertTrue(formatted.contains("PAUSED"));
    }

    @Test
    void hiveResourceChannelArbiterPreemptsLowerPriorityAndReleasesCleanly() {
        HiveResourceArbiter arbiter = new HiveResourceArbiter();
        Object low = new Object();
        Object conflict = new Object();
        Object high = new Object();

        int moveLookMask = HiveResourceChannel.maskOf(HiveResourceChannel.MOVE, HiveResourceChannel.LOOK);
        assertEquals("MOVE|LOOK", HiveResourceChannel.formatMask(moveLookMask));

        assertTrue(arbiter.acquire(low, moveLookMask, 1, null));
        assertEquals(low, arbiter.getOwner(HiveResourceChannel.MOVE));
        assertEquals(low, arbiter.getOwner(HiveResourceChannel.LOOK));
        assertFalse(arbiter.canAcquire(conflict, HiveResourceChannel.MOVE.bit(), 1));
        assertFalse(arbiter.acquire(conflict, HiveResourceChannel.MOVE.bit(), 1, null));

        List<Object> displaced = new ArrayList<>();
        assertTrue(arbiter.acquire(high, HiveResourceChannel.MOVE.bit(), 5, displaced::add));
        assertEquals(1, displaced.size());
        assertEquals(low, displaced.get(0));
        assertEquals(high, arbiter.getOwner(HiveResourceChannel.MOVE));
        assertNull(arbiter.getOwner(HiveResourceChannel.LOOK));

        arbiter.release(high);
        assertEquals(0, arbiter.occupiedMask());
        assertEquals("NONE", arbiter.formatOccupied());
    }

    @Test
    void hiveBrainTraceRingBufferMaintainsChronologicalOrderAndCapacity() {
        HiveBrainTrace trace = new HiveBrainTrace(4);
        assertEquals(4, trace.capacity());
        assertEquals(0, trace.size());
        assertNull(trace.latest());

        for (int i = 0; i < 6; i++) {
            trace.record(100L + i, HiveBrainTrace.EventKind.SIGNAL, "ev_" + i);
        }

        assertEquals(4, trace.size());
        List<HiveBrainTrace.Entry> entries = trace.recentEntries();
        assertEquals(4, entries.size());
        assertEquals("ev_2", entries.get(0).detail());
        assertEquals("ev_3", entries.get(1).detail());
        assertEquals("ev_4", entries.get(2).detail());
        assertEquals("ev_5", entries.get(3).detail());
        assertEquals("ev_5", trace.latest().detail());
        assertTrue(trace.latest().format().contains("[t=105] SIGNAL: ev_5"));

        trace.clear();
        assertEquals(0, trace.size());
        assertNull(trace.latest());
    }

    @Test
    void hiveBrainSchedulerLodMultiplierSupportsFullCadenceAndDeepSleep() {
        // Multiplier 0 disables LOD throttling cleanly
        for (long t = 0L; t < 8L; t++) {
            assertTrue(HiveBrainScheduler.shouldTickBrainWithLod(
                    10L, 2, t, HiveBrainScheduler.INITIAL_TICK_SENTINEL, 0, false, false));
        }

        // Multiplier 1 = 1-in-4; Multiplier 2 = 1-in-8
        assertTrue(HiveBrainScheduler.shouldTickBrainWithLod(
                10L, 1, 11L, HiveBrainScheduler.INITIAL_TICK_SENTINEL, 1, false, false));
        assertFalse(HiveBrainScheduler.shouldTickBrainWithLod(
                10L, 1, 11L, HiveBrainScheduler.INITIAL_TICK_SENTINEL, 2, false, false));
        assertTrue(HiveBrainScheduler.shouldTickBrainWithLod(
                10L, 1, 15L, HiveBrainScheduler.INITIAL_TICK_SENTINEL, 2, false, false));
    }
}
