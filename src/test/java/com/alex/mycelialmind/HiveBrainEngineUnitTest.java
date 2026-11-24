package com.alex.mycelialmind;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AvoidEntity;
import com.alex.mycelialmind.entity.ai.brain.behaviour.FleeTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.StrafeTarget;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainScheduler;
import com.alex.mycelialmind.entity.ai.brain.sensor.IncomingProjectilesSensor;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
