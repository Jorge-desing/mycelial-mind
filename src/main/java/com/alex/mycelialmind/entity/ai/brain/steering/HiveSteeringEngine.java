package com.alex.mycelialmind.entity.ai.brain.steering;

import net.minecraft.world.phys.Vec3;

/**
 * Pure clean-room 2D (XZ-plane) steering engine replacing external steering libraries.
 * Supports Separation, Cohesion, Alignment, Seek, Flee, Arrive, Pursuit, Evade,
 * Pylon/hazard repulsion, and angular attack-slot ring positioning with zero neighbor-loop allocations.
 */
public final class HiveSteeringEngine {
    public static final double DEFAULT_MAX_SPEED = 2.2D;
    public static final double DEFAULT_MAX_ACCEL = 4.0D;
    public static final double DEFAULT_ARRIVAL_TOLERANCE = 0.6D;
    public static final double DEFAULT_DECELERATION_RADIUS = 3.5D;
    public static final double DEFAULT_TIME_TO_TARGET = 0.15D;
    public static final double DEFAULT_SEPARATION_DECAY = 8.0D;

    private HiveSteeringEngine() {
    }

    public static Vec3 computeSeparation(
            double ox, double oz,
            double[] nx, double[] nz, int count,
            double decayCoefficient, double maxAccel) {
        if (count <= 0 || nx == null || nz == null) {
            return Vec3.ZERO;
        }
        int limit = Math.min(count, Math.min(nx.length, nz.length));
        double ax = 0.0D;
        double az = 0.0D;
        double safeMax = Math.max(0.0D, maxAccel);
        for (int i = 0; i < limit; i++) {
            double dx = ox - nx[i];
            double dz = oz - nz[i];
            double distSq = dx * dx + dz * dz;
            if (distSq < 1.0E-8D) {
                continue;
            }
            double strength = Math.min(safeMax, decayCoefficient / distSq);
            double invDist = 1.0D / Math.sqrt(distSq);
            ax += dx * invDist * strength;
            az += dz * invDist * strength;
        }
        return clampHorizontal(ax, az, safeMax);
    }

    public static Vec3 computeCohesion(
            double ox, double oz,
            double[] nx, double[] nz, int count,
            double maxAccel) {
        if (count <= 0 || nx == null || nz == null) {
            return Vec3.ZERO;
        }
        int limit = Math.min(count, Math.min(nx.length, nz.length));
        if (limit <= 0) {
            return Vec3.ZERO;
        }
        double cx = 0.0D;
        double cz = 0.0D;
        for (int i = 0; i < limit; i++) {
            cx += nx[i];
            cz += nz[i];
        }
        cx /= limit;
        cz /= limit;
        return computeSeek(ox, oz, cx, cz, maxAccel);
    }

    public static Vec3 computeAlignment(
            double ovx, double ovz,
            double[] nvx, double[] nvz, int count,
            double maxAccel) {
        if (count <= 0 || nvx == null || nvz == null) {
            return Vec3.ZERO;
        }
        int limit = Math.min(count, Math.min(nvx.length, nvz.length));
        if (limit <= 0) {
            return Vec3.ZERO;
        }
        double avx = 0.0D;
        double avz = 0.0D;
        for (int i = 0; i < limit; i++) {
            avx += nvx[i];
            avz += nvz[i];
        }
        avx /= limit;
        avz /= limit;
        return clampHorizontal(avx - ovx, avz - ovz, Math.max(0.0D, maxAccel));
    }

    public static Vec3 computeSeek(double ox, double oz, double tx, double tz, double maxAccel) {
        double dx = tx - ox;
        double dz = tz - oz;
        double distSq = dx * dx + dz * dz;
        double safeMax = Math.max(0.0D, maxAccel);
        if (distSq < 1.0E-8D || safeMax <= 0.0D) {
            return Vec3.ZERO;
        }
        double inv = safeMax / Math.sqrt(distSq);
        return new Vec3(dx * inv, 0.0D, dz * inv);
    }

    public static Vec3 computeFlee(double ox, double oz, double threatX, double threatZ, double maxAccel) {
        return computeSeek(threatX, threatZ, ox, oz, maxAccel);
    }

    public static Vec3 computeArrive(
            double ox, double oz,
            double ovx, double ovz,
            double tx, double tz,
            double arrivalTolerance,
            double decelerationRadius,
            double maxSpeed,
            double maxAccel,
            double timeToTarget) {
        double dx = tx - ox;
        double dz = tz - oz;
        double dist = Math.hypot(dx, dz);
        if (dist <= Math.max(0.0D, arrivalTolerance) || dist < 1.0E-6D) {
            return Vec3.ZERO;
        }
        double safeDecel = Math.max(1.0E-3D, decelerationRadius);
        double targetSpeed = dist > safeDecel ? maxSpeed : maxSpeed * (dist / safeDecel);
        double desiredVx = (dx / dist) * targetSpeed;
        double desiredVz = (dz / dist) * targetSpeed;
        double safeTtt = Math.max(0.05D, timeToTarget);
        double ax = (desiredVx - ovx) / safeTtt;
        double az = (desiredVz - ovz) / safeTtt;
        return clampHorizontal(ax, az, Math.max(0.0D, maxAccel));
    }

    public static Vec3 computePursuit(
            double ox, double oz,
            double tx, double tz,
            double tvx, double tvz,
            double maxPredictionTicks,
            double maxAccel) {
        double dist = Math.hypot(tx - ox, tz - oz);
        double speed = Math.hypot(tvx, tvz);
        double prediction = speed > 1.0E-4D
                ? Math.min(Math.max(0.0D, maxPredictionTicks), dist / Math.max(0.2D, speed))
                : 0.0D;
        return computeSeek(ox, oz, tx + tvx * prediction, tz + tvz * prediction, maxAccel);
    }

    public static Vec3 computeEvade(
            double ox, double oz,
            double tx, double tz,
            double tvx, double tvz,
            double maxPredictionTicks,
            double maxAccel) {
        double dist = Math.hypot(tx - ox, tz - oz);
        double speed = Math.hypot(tvx, tvz);
        double prediction = speed > 1.0E-4D
                ? Math.min(Math.max(0.0D, maxPredictionTicks), dist / Math.max(0.2D, speed))
                : 0.0D;
        return computeFlee(ox, oz, tx + tvx * prediction, tz + tvz * prediction, maxAccel);
    }

    public static Vec3 computePylonRepulsion(
            double ox, double oz,
            double pylonX, double pylonZ,
            double avoidRadius,
            double strength) {
        double dx = ox - pylonX;
        double dz = oz - pylonZ;
        double distSq = dx * dx + dz * dz;
        double safeRadius = Math.max(0.5D, avoidRadius);
        if (distSq >= safeRadius * safeRadius || distSq < 1.0E-8D) {
            return Vec3.ZERO;
        }
        double dist = Math.sqrt(distSq);
        double ratio = 1.0D - (dist / safeRadius);
        double mag = Math.max(0.0D, strength) * ratio;
        return new Vec3((dx / dist) * mag, 0.0D, (dz / dist) * mag);
    }

    /**
     * Computes an angular attack-slot coordinate around a target to distribute melee attackers evenly
     * without clumping on the exact same voxel.
     */
    public static Vec3 computeAttackSlotPosition(
            double targetX, double targetY, double targetZ,
            int slotIndex, int totalSlots,
            double ringRadius, double baseAngleRad) {
        int safeTotal = Math.max(1, totalSlots);
        int normalizedSlot = Math.floorMod(slotIndex, safeTotal);
        double angle = baseAngleRad + (2.0D * Math.PI * normalizedSlot) / safeTotal;
        double safeRadius = Math.max(0.5D, ringRadius);
        return new Vec3(
                targetX + Math.cos(angle) * safeRadius,
                targetY,
                targetZ + Math.sin(angle) * safeRadius
        );
    }

    public static Vec3 computeBlendedSwarmSteering(
            boolean includeArrive,
            double ox, double oz,
            double ovx, double ovz,
            double tx, double tz,
            double[] nx, double[] nz,
            double[] nvx, double[] nvz,
            int neighborCount,
            boolean hasPylonRepulsion,
            double pylonX, double pylonZ,
            double maxMagnitude) {
        double sumX = 0.0D;
        double sumZ = 0.0D;

        if (includeArrive) {
            Vec3 arrive = computeArrive(
                    ox, oz, ovx, ovz, tx, tz,
                    DEFAULT_ARRIVAL_TOLERANCE,
                    DEFAULT_DECELERATION_RADIUS,
                    DEFAULT_MAX_SPEED,
                    DEFAULT_MAX_ACCEL,
                    DEFAULT_TIME_TO_TARGET);
            sumX += arrive.x;
            sumZ += arrive.z;
        }

        if (neighborCount > 0) {
            double sepWeight = includeArrive ? 1.35D : 1.50D;
            Vec3 sep = computeSeparation(ox, oz, nx, nz, neighborCount, DEFAULT_SEPARATION_DECAY, DEFAULT_MAX_ACCEL);
            Vec3 coh = computeCohesion(ox, oz, nx, nz, neighborCount, DEFAULT_MAX_ACCEL);
            Vec3 ali = computeAlignment(ovx, ovz, nvx, nvz, neighborCount, DEFAULT_MAX_ACCEL);
            sumX += sep.x * sepWeight + coh.x * 0.20D + ali.x * 0.20D;
            sumZ += sep.z * sepWeight + coh.z * 0.20D + ali.z * 0.20D;
        }

        if (hasPylonRepulsion) {
            Vec3 rep = computePylonRepulsion(ox, oz, pylonX, pylonZ, 7.0D, 0.85D);
            sumX += rep.x;
            sumZ += rep.z;
        }

        return clampHorizontal(sumX, sumZ, Math.max(0.0D, maxMagnitude));
    }

    public static Vec3 clampHorizontal(double x, double z, double maxMagnitude) {
        double lenSq = x * x + z * z;
        if (lenSq <= 1.0E-12D || maxMagnitude <= 0.0D) {
            return Vec3.ZERO;
        }
        double maxSq = maxMagnitude * maxMagnitude;
        if (lenSq > maxSq) {
            double scale = maxMagnitude / Math.sqrt(lenSq);
            return new Vec3(x * scale, 0.0D, z * scale);
        }
        return new Vec3(x, 0.0D, z);
    }
}
