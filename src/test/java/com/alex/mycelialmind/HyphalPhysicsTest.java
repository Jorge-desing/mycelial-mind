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

    @Test
    void verletSingleEndpointPinHangsAndCollidesWithSolidTerrain() {
        Vec3 anchorA = new Vec3(0.5D, 11.0D, 0.5D);
        Vec3 anchorB = new Vec3(4.5D, 11.0D, 0.5D);
        int n = 9;
        Vec3[] pos = HyphalVerletRope.initLine(anchorA, anchorB, n);
        Vec3[] prev = pos.clone();
        double segLen = anchorA.distanceTo(anchorB) / (double) (n - 1);

        boolean touchedAny = false;
        for (int step = 0; step < 30; step++) {
            boolean hit = HyphalVerletRope.stepVerlet(
                    pos, prev, anchorA, anchorB,
                    true, false,
                    segLen,
                    HyphalVerletRope.DEFAULT_GRAVITY,
                    HyphalVerletRope.DEFAULT_DAMPING,
                    Vec3.ZERO,
                    HyphalVerletRope.DEFAULT_CONSTRAINT_ITERATIONS,
                    bp -> bp.getY() <= 9);
            touchedAny |= hit;
        }

        assertEquals(anchorA, pos[0], "Pinned root endpoint must stay fixed");
        assertTrue(pos[n - 1].y < anchorB.y - 0.5D, "Unpinned tip endpoint should hang under gravity");
        assertTrue(touchedAny, "Hanging rope should collide with solid floor at y <= 9");
        for (Vec3 node : pos) {
            assertTrue(node.y >= 10.0D, "Solid terrain collision must keep nodes above y=9 floor, got " + node.y);
        }
    }

    @Test
    void verletSplitAndExtendRespectNodeBoundsAndSplitContinuously() {
        Vec3[] shortRope = HyphalVerletRope.initLine(new Vec3(0, 10, 0), new Vec3(2, 10, 0), 3);
        assertNull(HyphalVerletRope.splitRope(shortRope, 1), "Strands with < 4 nodes must not split");

        Vec3[] longRope = HyphalVerletRope.initLine(new Vec3(0, 10, 0), new Vec3(7, 10, 0), 8);
        HyphalVerletRope.SplitResult split = HyphalVerletRope.splitRope(longRope, 3);
        assertNotNull(split);
        assertEquals(4, split.rootSegment().length);
        assertEquals(4, split.severedSegment().length);
        assertEquals(longRope[0], split.rootSegment()[0]);
        assertEquals(longRope[7], split.severedSegment()[3]);

        Vec3[] grown = HyphalVerletRope.extendRope(shortRope, new Vec3(3, 10, 0), HyphalVerletRope.MAX_NODE_COUNT);
        assertEquals(4, grown.length);
        assertEquals(new Vec3(3, 10, 0), grown[3]);
        assertEquals(3, HyphalVerletRope.findClosestNodeIndex(grown, new Vec3(3.1, 10, 0)));

        Vec3[] capped = HyphalVerletRope.initLine(new Vec3(0, 10, 0), new Vec3(15, 10, 0), HyphalVerletRope.MAX_NODE_COUNT);
        Vec3[] afterCap = HyphalVerletRope.extendRope(capped, new Vec3(16, 10, 0), HyphalVerletRope.MAX_NODE_COUNT);
        assertEquals(HyphalVerletRope.MAX_NODE_COUNT, afterCap.length, "extendRope must not exceed MAX_NODE_COUNT");
    }

    @Test
    void lengthScalingDarkensAndIncreasesOpacityFromShortToLongStrands() {
        HyphalVerletRope.StrandRgba tiny = HyphalVerletRope.computeColorAndAlphaForLength(2, 0.5D);
        HyphalVerletRope.StrandRgba medium = HyphalVerletRope.computeColorAndAlphaForLength(12, 6.0D);
        HyphalVerletRope.StrandRgba mature = HyphalVerletRope.computeColorAndAlphaForLength(24, 12.0D);

        assertTrue(tiny.a() <= 70, "Very short strands must be nearly transparent, got alpha=" + tiny.a());
        assertTrue(medium.a() > tiny.a() && medium.a() < mature.a(), "Opacity must increase monotonically with length");
        assertEquals(255, mature.a(), "Mature long strands (>=24 nodes / >=12m) must be fully opaque");

        int tinyLuma = tiny.r() + tiny.g() + tiny.b();
        int matureLuma = mature.r() + mature.g() + mature.b();
        assertTrue(matureLuma < tinyLuma / 2, "Long strands must be significantly darker than short strands");

        assertEquals(0, HyphalVerletRope.strengthForLength(3));
        assertEquals(1, HyphalVerletRope.strengthForLength(8));
        assertEquals(2, HyphalVerletRope.strengthForLength(15));
        assertEquals(3, HyphalVerletRope.strengthForLength(24));
    }

    @Test
    void verletRaycastHitsClosestSegmentAndMissesDiagonalEmptySpace() {
        Vec3[] diagonal = HyphalVerletRope.initLine(new Vec3(0.5D, 10.06D, 0.5D), new Vec3(4.5D, 10.06D, 4.5D), 9);

        // Aim directly down onto the midpoint (2.5, 10.06, 2.5)
        HyphalVerletRope.RaycastHit midHit = HyphalVerletRope.raycastClosestSegment(
                diagonal, new Vec3(2.5D, 11.8D, 2.5D), new Vec3(0.0D, -1.0D, 0.0D), 6.0D, 0.45D);
        assertNotNull(midHit, "Ray aimed directly at diagonal midpoint must hit");
        assertTrue(midHit.distance() < 0.05D, "Midpoint raycast distance should be near zero, got " + midHit.distance());
        assertEquals(4, midHit.closestNodeIndex());

        // Aim into the empty corner of the diagonal AABB (4.2, 10.06, 0.8) -> >2.3m away from strand
        HyphalVerletRope.RaycastHit missHit = HyphalVerletRope.raycastClosestSegment(
                diagonal, new Vec3(4.2D, 11.8D, 0.8D), new Vec3(0.0D, -1.0D, 0.0D), 6.0D, 0.45D);
        assertNull(missHit, "Ray aimed into empty space inside diagonal AABB must return null");
    }

    @Test
    void turgorModulatesEffectiveGravityAndConstraintStiffness() {
        double lowTurgorGrav = HyphalVerletRope.effectiveGravityForTurgor(0.0F);
        double highTurgorGrav = HyphalVerletRope.effectiveGravityForTurgor(1.0F);
        int lowTurgorIters = HyphalVerletRope.constraintIterationsForTurgor(0.0F);
        int highTurgorIters = HyphalVerletRope.constraintIterationsForTurgor(1.0F);

        assertTrue(highTurgorGrav < lowTurgorGrav, "High turgor should resist gravitational sag more than dry strand");
        assertTrue(highTurgorIters > lowTurgorIters, "High turgor should yield stiffer constraint iterations");
    }

    @Test
    void severRecoilImpartsOutwardVelocityAndWitherDesaturatesColor() {
        Vec3[] pos = HyphalVerletRope.initLine(new Vec3(0.5D, 10.0D, 0.5D), new Vec3(3.5D, 10.0D, 0.5D), 5);
        Vec3[] prev = pos.clone();
        Vec3 posBefore = pos[4];

        HyphalVerletRope.applySeverRecoil(pos, prev, true, new Vec3(4.0D, 10.0D, 0.5D), 0.16D);
        assertEquals(posBefore, pos[4], "Recoil impulse must not teleport pos directly");
        Vec3 impliedVelocity = pos[4].subtract(prev[4]);
        assertTrue(impliedVelocity.x < -0.05D, "Recoil at tip facing +X cutPoint should accelerate backward (-X)");
        assertTrue(impliedVelocity.y > 0.0D, "Recoil should add slight upward snapback velocity");

        HyphalVerletRope.StrandRgba healthy = HyphalVerletRope.computeColorAndAlphaForLength(24, 12.0D);
        HyphalVerletRope.StrandRgba withering = HyphalVerletRope.applyWitherTint(healthy, 0.70F);
        assertTrue(withering.a() < healthy.a(), "Withering must reduce opacity");
        int healthySpread = Math.abs(healthy.b() - healthy.g());
        int witherSpread = Math.abs(withering.b() - withering.g());
        assertTrue(witherSpread < healthySpread, "Withering must desaturate violet hue toward dry ash gray");
    }
}
