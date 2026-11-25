package com.alex.mycelialmind;

import com.alex.mycelialmind.hive.HyphalPhysics;
import com.alex.mycelialmind.hive.HyphalVerletRope;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure headless JUnit 5 tests for Hyphal entity drag, mechanical wave decay,
 * fluid turgor calculation, and Verlet filament rope physics.
 */
class HyphalPhysicsTest {
    @Test
    void entityDragSlowsNonMycelialBoostsMycelialAndMitigatesBootsOrSneak() {
        assertEquals(1.10F,
                HyphalPhysics.computeEntityDragFactor(3, 1.0F, true, false, false), 1.0E-4F);
        assertEquals(1.0F,
                HyphalPhysics.computeEntityDragFactor(1, 1.0F, false, false, false), 1.0E-4F);

        float denseBare = HyphalPhysics.computeEntityDragFactor(3, 0.80F, false, false, false);
        float denseBoots = HyphalPhysics.computeEntityDragFactor(3, 0.80F, false, true, false);
        float denseSneak = HyphalPhysics.computeEntityDragFactor(3, 0.80F, false, false, true);

        assertTrue(denseBare < 0.75F, "Dense turgid vein should significantly slow unarmored non-mycelial entities");
        assertTrue(denseBoots > denseBare && denseBoots < 1.0F, "Chitin boots should mitigate 75% of drag");
        assertEquals(denseBoots, denseSneak, 1.0E-4F);
    }

    @Test
    void mechanicalWaveDecaysMonotonicallyAndDetachesFragileDryVeins() {
        float amp = 1.0F;
        for (int hop = 0; hop < 8; hop++) {
            float next = HyphalPhysics.nextWaveAmplitude(amp);
            assertTrue(next < amp || next == 0.0F);
            amp = next;
        }
        float low = HyphalPhysics.nextWaveAmplitude(0.09F);
        assertEquals(0.0F, low, 1.0E-5F, "Amplitudes below WAVE_MIN_AMPLITUDE should terminate to 0");

        assertTrue(HyphalPhysics.shouldDetachFromWave(0.80F, 0, 0.20F));
        assertFalse(HyphalPhysics.shouldDetachFromWave(0.80F, 1, 0.20F));
        assertFalse(HyphalPhysics.shouldDetachFromWave(0.80F, 0, 0.80F));
        assertFalse(HyphalPhysics.shouldDetachFromWave(0.40F, 0, 0.10F));
    }

    @Test
    void fluidTurgorRespondsToNutrientsMoistureHeatAndPressurizesThreshold() {
        float dryRoot = HyphalPhysics.computeTurgor(15, false, false);
        float wetRoot = HyphalPhysics.computeTurgor(15, true, false);
        float hotRoot = HyphalPhysics.computeTurgor(15, false, true);
        float starvedHot = HyphalPhysics.computeTurgor(0, false, true);

        assertEquals(0.70F, dryRoot, 1.0E-4F);
        assertEquals(0.95F, wetRoot, 1.0E-4F);
        assertEquals(0.35F, hotRoot, 1.0E-4F);
        assertEquals(0.0F, starvedHot, 1.0E-4F);

        assertTrue(HyphalPhysics.isPressurizedForSpray(dryRoot));
        assertTrue(HyphalPhysics.isPressurizedForSpray(wetRoot));
        assertFalse(HyphalPhysics.isPressurizedForSpray(hotRoot));
    }

    @Test
    void verletRopeSagsUnderGravityPinsEndsDeflectsAndSnapsUnderExcessTension() {
        Vec3 anchorA = new Vec3(0.5D, 10.5D, 0.5D);
        Vec3 anchorB = new Vec3(4.5D, 10.5D, 0.5D);
        int n = HyphalVerletRope.DEFAULT_NODE_COUNT;
        Vec3[] pos = HyphalVerletRope.initLine(anchorA, anchorB, n);
        Vec3[] prev = pos.clone();
        double restLength = anchorA.distanceTo(anchorB) * 1.05D;
        double segLen = restLength / (double) (n - 1);

        for (int step = 0; step < 15; step++) {
            HyphalVerletRope.stepVerlet(
                    pos, prev, anchorA, anchorB, segLen,
                    HyphalVerletRope.DEFAULT_GRAVITY,
                    HyphalVerletRope.DEFAULT_DAMPING,
                    Vec3.ZERO,
                    HyphalVerletRope.DEFAULT_CONSTRAINT_ITERATIONS);
        }

        assertEquals(anchorA, pos[0]);
        assertEquals(anchorB, pos[n - 1]);
        assertTrue(pos[n / 2].y < anchorA.y - 0.10D, "Midpoint should sag below horizontal anchor line");

        Vec3 beforeHit = pos[n / 2];
        HyphalVerletRope.applyEntityImpulse(pos, prev, beforeHit, new Vec3(0.0D, 0.0D, 0.5D), 1.5D);
        assertTrue(pos[n / 2].z > beforeHit.z, "Entity velocity impulse should deflect nearby filament nodes");

        // Overstretch beyond max tension ratio -> snap threshold reached
        Vec3 pulledB = new Vec3(12.5D, 10.5D, 0.5D);
        HyphalVerletRope.stepVerlet(
                pos, prev, anchorA, pulledB, segLen,
                HyphalVerletRope.DEFAULT_GRAVITY,
                HyphalVerletRope.DEFAULT_DAMPING,
                Vec3.ZERO,
                1);
        double tension = HyphalVerletRope.computeTensionRatio(pos, restLength);
        assertTrue(HyphalVerletRope.shouldSnap(tension, HyphalVerletRope.DEFAULT_MAX_TENSION_RATIO),
                "Overstretched rope should exceed tension snap threshold");
    }
}
