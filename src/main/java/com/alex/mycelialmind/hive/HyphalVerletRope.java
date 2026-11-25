package com.alex.mycelialmind.hive;

import net.minecraft.world.phys.Vec3;

/**
 * Pure, world-independent Verlet rope physics solver for 3D hanging/bridging
 * Hyphal Filaments: gravity sag, wind sway, entity collision impulse, distance
 * constraints, and tension snap thresholding.
 */
public final class HyphalVerletRope {
    public static final int DEFAULT_NODE_COUNT = 9;
    public static final double DEFAULT_GRAVITY = 0.035D;
    public static final double DEFAULT_DAMPING = 0.94D;
    public static final int DEFAULT_CONSTRAINT_ITERATIONS = 5;
    public static final double DEFAULT_MAX_TENSION_RATIO = 1.65D;

    private HyphalVerletRope() {
    }

    public static Vec3[] initLine(Vec3 anchorA, Vec3 anchorB, int nodeCount) {
        int n = Math.max(2, nodeCount);
        Vec3[] nodes = new Vec3[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            nodes[i] = anchorA.lerp(anchorB, t);
        }
        return nodes;
    }

    public static void stepVerlet(
            Vec3[] pos,
            Vec3[] prevPos,
            Vec3 anchorA,
            Vec3 anchorB,
            double restSegmentLen,
            double gravity,
            double damping,
            Vec3 windAccel,
            int constraintIterations
    ) {
        int n = pos.length;
        if (n < 2 || prevPos.length != n) {
            return;
        }
        double safeSeg = Math.max(0.05D, restSegmentLen);
        double safeDamping = Math.max(0.0D, Math.min(0.999D, damping));
        Vec3 safeWind = windAccel != null ? windAccel : Vec3.ZERO;

        // Integrate interior nodes
        for (int i = 1; i < n - 1; i++) {
            Vec3 cur = pos[i];
            Vec3 prev = prevPos[i];
            Vec3 vel = cur.subtract(prev).scale(safeDamping);
            prevPos[i] = cur;
            pos[i] = cur.add(vel).add(safeWind.x, safeWind.y - gravity, safeWind.z);
        }
        pos[0] = anchorA;
        prevPos[0] = anchorA;
        pos[n - 1] = anchorB;
        prevPos[n - 1] = anchorB;

        int iterations = Math.max(1, constraintIterations);
        for (int iter = 0; iter < iterations; iter++) {
            pos[0] = anchorA;
            pos[n - 1] = anchorB;
            for (int i = 0; i < n - 1; i++) {
                Vec3 a = pos[i];
                Vec3 b = pos[i + 1];
                Vec3 delta = b.subtract(a);
                double dist = delta.length();
                if (dist < 1.0E-6D) {
                    continue;
                }
                double diff = (dist - safeSeg) / dist;
                boolean pinA = (i == 0);
                boolean pinB = (i + 1 == n - 1);
                if (pinA && pinB) {
                    continue;
                } else if (pinA) {
                    pos[i + 1] = b.subtract(delta.scale(diff));
                } else if (pinB) {
                    pos[i] = a.add(delta.scale(diff));
                } else {
                    Vec3 half = delta.scale(diff * 0.5D);
                    pos[i] = a.add(half);
                    pos[i + 1] = b.subtract(half);
                }
            }
        }
        pos[0] = anchorA;
        pos[n - 1] = anchorB;
    }

    public static void applyEntityImpulse(
            Vec3[] pos,
            Vec3[] prevPos,
            Vec3 entityPos,
            Vec3 entityVel,
            double radius
    ) {
        int n = pos.length;
        if (n < 3 || prevPos.length != n || radius <= 0.0D) {
            return;
        }
        double radiusSq = radius * radius;
        Vec3 safeVel = entityVel != null ? entityVel : Vec3.ZERO;
        for (int i = 1; i < n - 1; i++) {
            Vec3 node = pos[i];
            double dSq = node.distanceToSqr(entityPos);
            if (dSq <= radiusSq) {
                double falloff = 1.0D - Math.sqrt(dSq) / radius;
                Vec3 push = safeVel.scale(0.35D * falloff);
                if (push.lengthSqr() > 0.04D) {
                    push = push.normalize().scale(0.20D);
                }
                pos[i] = node.add(push);
            }
        }
    }

    public static double computeTotalLength(Vec3[] pos) {
        if (pos == null || pos.length < 2) {
            return 0.0D;
        }
        double total = 0.0D;
        for (int i = 0; i < pos.length - 1; i++) {
            total += pos[i].distanceTo(pos[i + 1]);
        }
        return total;
    }

    public static double computeTensionRatio(Vec3[] pos, double totalRestLength) {
        return computeTotalLength(pos) / Math.max(0.01D, totalRestLength);
    }

    public static boolean shouldSnap(double tensionRatio, double maxTensionRatio) {
        return Double.isFinite(tensionRatio) && tensionRatio > maxTensionRatio;
    }
}
