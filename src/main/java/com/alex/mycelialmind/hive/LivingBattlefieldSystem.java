package com.alex.mycelialmind.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Coordinates v1.7.0 Living Battlefield AI mechanics:
 * tactical anti-cheese terraforming, arrow barricades, lethal trauma zone avoidance
 * with Sapper priority, and battlefield carrion recycling rules.
 */
public final class LivingBattlefieldSystem {
    public static final float TRAUMA_AVOID_THRESHOLD = 0.55F;
    public static final int CARRION_TTL_TICKS = 240; // 12 seconds
    public static final float CARRION_HEAL_AMOUNT = 6.0F;
    public static final int CARRION_XP_REWARD = 20;
    public static final int TURRET_CORROSIVE_JAM_TICKS = 120; // 6 seconds
    public static final int DRONE_PARASITIC_JAM_TICKS = 80; // 4 seconds

    private LivingBattlefieldSystem() {
    }

    public static boolean isSapperUnit(
            boolean isBurrower,
            boolean isColonyWarden,
            EvolutionRank rank,
            boolean hasBlastShielded
    ) {
        return isBurrower || isColonyWarden || rank == EvolutionRank.ELITE || hasBlastShielded;
    }

    public static boolean shouldAvoidTraumaZone(float cellTrauma, boolean isSapper) {
        return !isSapper && cellTrauma >= TRAUMA_AVOID_THRESHOLD;
    }

    public static Vec3 computeTraumaDetourVector(Vec3 toTarget, Vec3 traumaEscapeVec, float cellTrauma) {
        if (cellTrauma < TRAUMA_AVOID_THRESHOLD) {
            return Vec3.ZERO;
        }
        Vec3 flatToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
        Vec3 tangent = flatToTarget.lengthSqr() > 1.0E-5D
                ? new Vec3(-flatToTarget.z, 0.0D, flatToTarget.x).normalize()
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 flatEscape = new Vec3(traumaEscapeVec.x, 0.0D, traumaEscapeVec.z);
        Vec3 escapeNorm = flatEscape.lengthSqr() > 1.0E-5D ? flatEscape.normalize() : Vec3.ZERO;
        return tangent.scale(0.65D)
                .add(escapeNorm.scale(0.75D))
                .scale(2.5D * cellTrauma);
    }

    public static List<BlockPos> computeAntiPillarStepPositions(BlockPos mobPos, BlockPos targetPos) {
        if (targetPos.getY() - mobPos.getY() < 2) {
            return List.of();
        }
        int dx = Integer.compare(targetPos.getX(), mobPos.getX());
        int dz = Integer.compare(targetPos.getZ(), mobPos.getZ());
        if (dx == 0 && dz == 0) {
            dx = 1;
        }
        return List.of(mobPos.offset(dx, 0, dz), mobPos.offset(dx * 2, 1, dz * 2));
    }

    public static List<BlockPos> computeBarricadePositions(BlockPos mobPos, Vec3 attackerPos) {
        double mobCenterX = mobPos.getX() + 0.5D;
        double mobCenterZ = mobPos.getZ() + 0.5D;
        int dx = Double.compare(attackerPos.x, mobCenterX);
        int dz = Double.compare(attackerPos.z, mobCenterZ);
        if (dx == 0 && dz == 0) {
            dx = 1;
        }
        BlockPos front = mobPos.offset(dx, 0, dz);
        return List.of(front, front.above());
    }

    public static boolean shouldDropCarrion(boolean isBoss, boolean isFireDamage, boolean inPylonZone) {
        return !isBoss && !isFireDamage && !inPylonZone;
    }

    /**
     * Acid rain response for allied Chimeras:
     * Fire Resistant + Magic Carapace symbiotically heal (+0.8F),
     * either defensive gene confers immunity (0.0F), unadapted chimeras suffer mild corrosion (-0.75F).
     */
    public static float computeChimeraAcidRainDelta(boolean hasFireResistant, boolean hasMagicCarapace) {
        if (hasFireResistant && hasMagicCarapace) {
            return 0.8F;
        }
        if (hasFireResistant || hasMagicCarapace) {
            return 0.0F;
        }
        return -0.75F;
    }
}
