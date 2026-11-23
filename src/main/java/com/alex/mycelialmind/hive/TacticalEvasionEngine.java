package com.alex.mycelialmind.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Pure clean-room hysteresis band evaluator, imminent-hazard evade vector solver,
 * and adaptive AI tick interval calculator.
 */
public final class TacticalEvasionEngine {
    public enum KiteBand {
        NONE,
        STANDARD,
        EMERGENCY
    }

    public static final double EMERGENCY_ENTER_DIST_SQR = 16.0D;
    public static final double EMERGENCY_TRAUMA_ENTER_DIST_SQR = 20.25D;
    public static final double EMERGENCY_EXIT_DIST_SQR = 30.25D;
    public static final double STANDARD_ENTER_DIST_SQR = 25.0D;
    public static final double STANDARD_EXIT_DIST_SQR = 49.0D;
    public static final float STANDARD_ENTER_UTILITY = 0.60F;
    public static final float STANDARD_EXIT_UTILITY = 0.42F;

    private TacticalEvasionEngine() {
    }

    public static KiteBand evaluateKiteBand(
            double distSqr,
            KiteBand currentBand,
            float kiteUtility,
            boolean hasHazardTrauma
    ) {
        KiteBand band = currentBand != null ? currentBand : KiteBand.NONE;
        double emergencyEnterSqr = hasHazardTrauma ? EMERGENCY_TRAUMA_ENTER_DIST_SQR : EMERGENCY_ENTER_DIST_SQR;
        if (distSqr < emergencyEnterSqr || (band == KiteBand.EMERGENCY && distSqr <= EMERGENCY_EXIT_DIST_SQR)) {
            return KiteBand.EMERGENCY;
        }
        boolean enterStandard = distSqr < STANDARD_ENTER_DIST_SQR || kiteUtility >= STANDARD_ENTER_UTILITY;
        boolean stayStandard = (band == KiteBand.STANDARD || band == KiteBand.EMERGENCY)
                && (distSqr <= STANDARD_EXIT_DIST_SQR || kiteUtility >= STANDARD_EXIT_UTILITY);
        if (enterStandard || stayStandard) {
            return KiteBand.STANDARD;
        }
        return KiteBand.NONE;
    }

    public static double kiteSpeedMultiplier(KiteBand band) {
        if (band == null) {
            return 1.0D;
        }
        return switch (band) {
            case EMERGENCY -> 1.28D;
            case STANDARD -> 1.12D;
            case NONE -> 1.0D;
        };
    }

    public static Vec3 computeHazardEvadeVector(
            double mobX,
            double mobZ,
            double hazardX,
            double hazardZ,
            @Nullable BlockPos nearestNodePos
    ) {
        double dx = mobX - hazardX;
        double dz = mobZ - hazardZ;
        double lenSqr = dx * dx + dz * dz;
        Vec3 away = lenSqr > 1.0E-4D ? new Vec3(dx, 0.0D, dz).normalize() : new Vec3(1.0D, 0.0D, 0.0D);

        if (nearestNodePos != null) {
            Vec3 toNode = new Vec3(nearestNodePos.getX() + 0.5D - mobX, 0.0D, nearestNodePos.getZ() + 0.5D - mobZ);
            if (toNode.lengthSqr() > 1.0E-4D) {
                Vec3 nodeDir = toNode.normalize();
                if (away.dot(nodeDir) >= 0.0D) {
                    Vec3 blended = away.add(nodeDir.scale(0.35D));
                    if (blended.lengthSqr() > 1.0E-4D) {
                        return blended.normalize();
                    }
                }
            }
        }
        return away;
    }

    public static int computeAdaptiveAiInterval(boolean hasTarget, boolean nearPlayer, boolean inAlarmOrFrenzy) {
        if (hasTarget || inAlarmOrFrenzy) {
            return 4;
        }
        return nearPlayer ? 10 : 20;
    }
}
