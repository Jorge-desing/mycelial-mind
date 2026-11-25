package com.alex.mycelialmind.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Pure, world-independent classic Verlet rope physics solver for unified 3D Hyphal Veins:
 * endpoint pinning (both, root-only, tip-only, or free-fall), solid-block terrain collision
 * for interior nodes, wind sway, entity impulse deflection, rope splitting/extension,
 * tension snap detection, and length-scaled RGBA intensity.
 */
public final class HyphalVerletRope {
    public static final int DEFAULT_NODE_COUNT = 9;
    public static final int MIN_NODE_COUNT = 2;
    public static final int MAX_NODE_COUNT = 32;
    public static final double DEFAULT_GRAVITY = 0.035D;
    public static final double DEFAULT_DAMPING = 0.94D;
    public static final int DEFAULT_CONSTRAINT_ITERATIONS = 5;
    public static final double DEFAULT_MAX_TENSION_RATIO = 1.65D;

    public record SplitResult(Vec3[] rootSegment, Vec3[] severedSegment) {
    }

    public record StrandRgba(int r, int g, int b, int a) {
        public float alphaNormalized() {
            return a / 255.0F;
        }
    }

    public record RaycastHit(int segmentIndex, int closestNodeIndex, Vec3 hitPoint, double distance) {
    }

    private HyphalVerletRope() {
    }

    public static Vec3[] initLine(Vec3 anchorA, Vec3 anchorB, int nodeCount) {
        int n = Math.max(MIN_NODE_COUNT, Math.min(MAX_NODE_COUNT, nodeCount));
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
        stepVerlet(pos, prevPos, anchorA, anchorB, true, true, restSegmentLen, gravity, damping,
                windAccel, constraintIterations, null);
    }

    /**
     * Advances a classic Verlet strand one tick with configurable endpoint pinning and
     * optional solid-block terrain collision so unpinned interior nodes drape over solid
     * floors/steps and sag freely across gaps.
     *
     * @return {@code true} if any node collided with a solid block during this step.
     */
    public static boolean stepVerlet(
            Vec3[] pos,
            Vec3[] prevPos,
            Vec3 anchorA,
            Vec3 anchorB,
            boolean pinStart,
            boolean pinEnd,
            double restSegmentLen,
            double gravity,
            double damping,
            @Nullable Vec3 windAccel,
            int constraintIterations,
            @Nullable Predicate<BlockPos> isSolidBlock
    ) {
        int n = pos.length;
        if (n < MIN_NODE_COUNT || prevPos.length != n) {
            return false;
        }
        double safeSeg = Math.max(0.05D, restSegmentLen);
        double safeDamping = Math.max(0.0D, Math.min(0.999D, damping));
        Vec3 safeWind = windAccel != null ? windAccel : Vec3.ZERO;

        for (int i = 0; i < n; i++) {
            boolean pinned = (i == 0 && pinStart) || (i == n - 1 && pinEnd);
            if (pinned) {
                Vec3 pin = (i == 0) ? anchorA : anchorB;
                pos[i] = pin;
                prevPos[i] = pin;
            } else {
                Vec3 cur = pos[i];
                Vec3 prev = prevPos[i];
                Vec3 vel = cur.subtract(prev).scale(safeDamping);
                prevPos[i] = cur;
                pos[i] = cur.add(vel).add(safeWind.x, safeWind.y - gravity, safeWind.z);
            }
        }

        boolean touchedSolid = false;
        int iterations = Math.max(1, constraintIterations);
        for (int iter = 0; iter < iterations; iter++) {
            if (pinStart) {
                pos[0] = anchorA;
            }
            if (pinEnd) {
                pos[n - 1] = anchorB;
            }
            for (int i = 0; i < n - 1; i++) {
                Vec3 a = pos[i];
                Vec3 b = pos[i + 1];
                Vec3 delta = b.subtract(a);
                double dist = delta.length();
                if (dist < 1.0E-6D) {
                    continue;
                }
                double diff = (dist - safeSeg) / dist;
                boolean pA = (i == 0 && pinStart);
                boolean pB = (i + 1 == n - 1 && pinEnd);
                if (pA && pB) {
                    continue;
                } else if (pA) {
                    pos[i + 1] = b.subtract(delta.scale(diff));
                } else if (pB) {
                    pos[i] = a.add(delta.scale(diff));
                } else {
                    Vec3 half = delta.scale(diff * 0.5D);
                    pos[i] = a.add(half);
                    pos[i + 1] = b.subtract(half);
                }
            }
            if (isSolidBlock != null) {
                for (int i = 0; i < n; i++) {
                    if ((i == 0 && pinStart) || (i == n - 1 && pinEnd)) {
                        continue;
                    }
                    Vec3 resolved = resolveSolidCollision(pos[i], isSolidBlock);
                    if (resolved != pos[i]) {
                        pos[i] = resolved;
                        touchedSolid = true;
                    }
                }
            }
        }
        if (pinStart) {
            pos[0] = anchorA;
        }
        if (pinEnd) {
            pos[n - 1] = anchorB;
        }
        return touchedSolid;
    }

    private static Vec3 resolveSolidCollision(Vec3 node, Predicate<BlockPos> isSolidBlock) {
        BlockPos bp = BlockPos.containing(node.x, node.y, node.z);
        if (!isSolidBlock.test(bp)) {
            return node;
        }
        double topY = bp.getY() + 1.05D;
        BlockPos above = bp.above();
        if (!isSolidBlock.test(above)) {
            return new Vec3(node.x, topY, node.z);
        }
        // Push horizontally out of solid wall toward closest open face
        double fracX = node.x - bp.getX();
        double fracZ = node.z - bp.getZ();
        double nx = node.x;
        double nz = node.z;
        if (Math.min(fracX, 1.0D - fracX) <= Math.min(fracZ, 1.0D - fracZ)) {
            nx = fracX < 0.5D ? bp.getX() - 0.06D : bp.getX() + 1.06D;
        } else {
            nz = fracZ < 0.5D ? bp.getZ() - 0.06D : bp.getZ() + 1.06D;
        }
        return new Vec3(nx, node.y, nz);
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

    public static int findClosestNodeIndex(Vec3[] pos, Vec3 target) {
        if (pos == null || pos.length == 0 || target == null) {
            return -1;
        }
        int best = 0;
        double bestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < pos.length; i++) {
            double dSq = pos[i].distanceToSqr(target);
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                best = i;
            }
        }
        return best;
    }

    /**
     * Splits a rope at {@code splitIndex} into a root-side segment and a severed segment,
     * each containing at least 2 nodes. Returns {@code null} if the rope is too short to split.
     */
    @Nullable
    public static SplitResult splitRope(Vec3[] pos, int splitIndex) {
        if (pos == null || pos.length < 4) {
            return null;
        }
        int clampedSplit = Math.max(1, Math.min(pos.length - 3, splitIndex));
        Vec3[] left = Arrays.copyOfRange(pos, 0, clampedSplit + 1);
        Vec3[] right = Arrays.copyOfRange(pos, clampedSplit + 1, pos.length);
        if (left.length < MIN_NODE_COUNT || right.length < MIN_NODE_COUNT) {
            return null;
        }
        return new SplitResult(left, right);
    }

    public static Vec3[] extendRope(Vec3[] pos, Vec3 newTip, int maxNodes) {
        int cap = Math.max(MIN_NODE_COUNT, Math.min(MAX_NODE_COUNT, maxNodes));
        if (pos == null || pos.length == 0) {
            return new Vec3[]{newTip, newTip};
        }
        if (pos.length >= cap) {
            Vec3[] copy = pos.clone();
            copy[copy.length - 1] = newTip;
            return copy;
        }
        Vec3[] extended = Arrays.copyOf(pos, pos.length + 1);
        extended[extended.length - 1] = newTip;
        return extended;
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

    /**
     * Computes length-scaled visual RGBA: short/young strands are nearly transparent and pale
     * (alpha ~0.22, 0xB8B0C2), progressively deepening to fully opaque obsidian-violet
     * (alpha 1.0, 0x240533) on long mature hyphae (>= 24 nodes / >= 12 blocks).
     */
    public static StrandRgba computeColorAndAlphaForLength(int nodeCount, double totalLength) {
        double safeLen = Double.isFinite(totalLength) ? Math.max(0.0D, totalLength) : 0.0D;
        float nodeFactor = Mth.clamp((nodeCount - 2) / 22.0F, 0.0F, 1.0F);
        float lenFactor = Mth.clamp((float) ((safeLen - 1.0D) / 11.0D), 0.0F, 1.0F);
        float t = Math.max(nodeFactor, lenFactor);

        int r = Math.round(Mth.lerp(t, 0xB8, 0x24));
        int g = Math.round(Mth.lerp(t, 0xB0, 0x05));
        int b = Math.round(Mth.lerp(t, 0xC2, 0x33));
        int a = Math.round(Mth.lerp(t, 0.22F, 1.0F) * 255.0F);
        return new StrandRgba(r, g, b, Math.max(40, Math.min(255, a)));
    }

    public static int strengthForLength(int nodeCount) {
        if (nodeCount <= 4) {
            return 0;
        }
        if (nodeCount <= 10) {
            return 1;
        }
        if (nodeCount <= 18) {
            return 2;
        }
        return 3;
    }

    /**
     * Turgidity modulates effective gravitational sag: turgid veins resist sag,
     * dry veins droop more noticeably.
     */
    public static double effectiveGravityForTurgor(float turgor) {
        float t = Float.isFinite(turgor) ? Mth.clamp(turgor, 0.0F, 1.0F) : 0.5F;
        return Mth.lerp(t, (float) (DEFAULT_GRAVITY * 1.35D), (float) (DEFAULT_GRAVITY * 0.72D));
    }

    /**
     * Turgidity modulates constraint solver stiffness (iterations per Verlet step).
     */
    public static int constraintIterationsForTurgor(float turgor) {
        float t = Float.isFinite(turgor) ? Mth.clamp(turgor, 0.0F, 1.0F) : 0.5F;
        return Math.round(Mth.lerp(t, 3.0F, 7.0F));
    }

    /**
     * Imparts an outward/upward recoil impulse to the 1-2 nodes adjacent to a severed end
     * by shifting {@code prevPos} so the next Verlet integration creates organic snapback.
     */
    public static void applySeverRecoil(
            Vec3[] pos,
            Vec3[] prevPos,
            boolean recoilAtEnd,
            Vec3 cutPoint,
            double impulseMagnitude
    ) {
        int n = pos.length;
        if (n < MIN_NODE_COUNT || prevPos.length != n || impulseMagnitude <= 0.0D) {
            return;
        }
        int tipIdx = recoilAtEnd ? n - 1 : 0;
        int innerIdx = recoilAtEnd ? Math.max(0, n - 2) : Math.min(n - 1, 1);
        int neighborRef = recoilAtEnd ? Math.max(0, n - 2) : Math.min(n - 1, 1);

        Vec3 dir = pos[tipIdx].subtract(cutPoint);
        if (dir.lengthSqr() < 1.0E-6D) {
            dir = pos[tipIdx].subtract(pos[neighborRef]);
        }
        if (dir.lengthSqr() < 1.0E-6D) {
            dir = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            dir = dir.normalize().add(0.0D, 0.15D, 0.0D).normalize();
        }
        Vec3 impulse = dir.scale(impulseMagnitude);
        // Move prevPos opposite to desired velocity (cur - prev = desired velocity)
        prevPos[tipIdx] = prevPos[tipIdx].subtract(impulse);
        if (innerIdx != tipIdx) {
            prevPos[innerIdx] = prevPos[innerIdx].subtract(impulse.scale(0.5D));
        }
    }

    /**
     * Desaturates and fades strand RGBA toward dry ash gray as {@code witherProgress} approaches 1.0.
     */
    public static StrandRgba applyWitherTint(StrandRgba base, float witherProgress) {
        float w = Float.isFinite(witherProgress) ? Mth.clamp(witherProgress, 0.0F, 0.90F) : 0.0F;
        if (w <= 1.0E-4F) {
            return base;
        }
        int r = Math.round(Mth.lerp(w * 0.85F, base.r(), 0x7A));
        int g = Math.round(Mth.lerp(w * 0.85F, base.g(), 0x78));
        int b = Math.round(Mth.lerp(w * 0.85F, base.b(), 0x76));
        int a = Math.max(24, Math.round(base.a() * (1.0F - w)));
        return new StrandRgba(r, g, b, a);
    }

    /**
     * Finds the closest segment in {@code pos} intersected within {@code maxPerpDistance}
     * by the bounded ray {@code rayOrigin + rayDir * s} ({@code s in [0, maxRayDistance]}).
     */
    @Nullable
    public static RaycastHit raycastClosestSegment(
            Vec3[] pos,
            Vec3 rayOrigin,
            Vec3 rayDir,
            double maxRayDistance,
            double maxPerpDistance
    ) {
        if (pos == null || pos.length < MIN_NODE_COUNT || rayOrigin == null || rayDir == null
                || maxRayDistance <= 0.0D || maxPerpDistance < 0.0D) {
            return null;
        }
        double dirLenSq = rayDir.lengthSqr();
        if (dirLenSq < 1.0E-8D) {
            return null;
        }
        Vec3 normDir = rayDir.scale(1.0D / Math.sqrt(dirLenSq));
        Vec3 rayEnd = rayOrigin.add(normDir.scale(maxRayDistance));

        RaycastHit bestHit = null;
        double bestDistSq = maxPerpDistance * maxPerpDistance;
        double bestRayParam = Double.MAX_VALUE;

        for (int i = 0; i < pos.length - 1; i++) {
            Vec3 p0 = pos[i];
            Vec3 p1 = pos[i + 1];
            double[] st = closestParamsBetweenSegments(rayOrigin, rayEnd, p0, p1);
            double s = st[0];
            double t = st[1];
            Vec3 onRay = rayOrigin.lerp(rayEnd, s);
            Vec3 onSeg = p0.lerp(p1, t);
            double dSq = onRay.distanceToSqr(onSeg);
            if (dSq <= bestDistSq + 1.0E-9D) {
                boolean strictlyCloser = dSq < bestDistSq - 1.0E-5D;
                boolean tieNearerCamera = Math.abs(dSq - bestDistSq) <= 1.0E-5D && s < bestRayParam;
                if (bestHit == null || strictlyCloser || tieNearerCamera) {
                    bestDistSq = dSq;
                    bestRayParam = s;
                    int closestNode = t < 0.5D ? i : i + 1;
                    bestHit = new RaycastHit(i, closestNode, onSeg, Math.sqrt(dSq));
                }
            }
        }
        return bestHit;
    }

    /**
     * Standard bounded segment-segment closest parameters (s, t in [0, 1]) for P1->Q1 and P2->Q2.
     */
    private static double[] closestParamsBetweenSegments(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
        Vec3 d1 = q1.subtract(p1);
        Vec3 d2 = q2.subtract(p2);
        Vec3 r = p1.subtract(p2);
        double a = d1.lengthSqr();
        double e = d2.lengthSqr();
        double f = d2.dot(r);

        double s;
        double t;
        final double eps = 1.0E-8D;
        if (a <= eps && e <= eps) {
            return new double[]{0.0D, 0.0D};
        }
        if (a <= eps) {
            s = 0.0D;
            t = Mth.clamp(f / e, 0.0D, 1.0D);
        } else {
            double c = d1.dot(r);
            if (e <= eps) {
                t = 0.0D;
                s = Mth.clamp(-c / a, 0.0D, 1.0D);
            } else {
                double b = d1.dot(d2);
                double denom = a * e - b * b;
                if (denom != 0.0D) {
                    s = Mth.clamp((b * f - c * e) / denom, 0.0D, 1.0D);
                } else {
                    s = 0.0D;
                }
                t = (b * s + f) / e;
                if (t < 0.0D) {
                    t = 0.0D;
                    s = Mth.clamp(-c / a, 0.0D, 1.0D);
                } else if (t > 1.0D) {
                    t = 1.0D;
                    s = Mth.clamp((b - c) / a, 0.0D, 1.0D);
                }
            }
        }
        return new double[]{s, t};
    }
}
