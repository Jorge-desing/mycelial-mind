package com.alex.mycelialmind.hive;

/**
 * Pure deterministic rules and tuning constants for Layer 1-4 ecosystem purposes:
 * Living Architecture blocks, Reclaimed Bloom sanctuary auras, diegetic surplus
 * item uses, and emergent environmental mob counterplay.
 */
public final class EcosystemPurposeEngine {
    public enum BloomAuraKind {
        SANCTUARY_REGEN,
        STRENGTH,
        SPEED,
        FIRE_RESISTANCE,
        RESISTANCE,
        INSIGHT
    }

    public static final int BLOOM_AURA_INTERVAL_TICKS = 80;
    public static final int BLOOM_AURA_RADIUS = 6;
    public static final int BLOOM_AURA_DURATION_TICKS = 120;
    public static final float BLOOM_FATIGUE_RELIEF = 8.0F;
    public static final float BLOOM_MORALE_BOOST = 5.0F;
    public static final double BLOOM_BASE_HOMEOSTASIS_CALM = 1.25D;

    public static final int AMBER_PYLON_BONUS_RADIUS = 4;
    public static final int BARRIER_PYLON_BONUS_RADIUS = 3;
    public static final int BARRIER_PYLON_REQUIRED_FACES = 2;

    public static final int DUPLICATE_ENGRAM_XP = 12;
    public static final int DUPLICATE_ENGRAM_INSIGHT_TICKS = 200;
    public static final double DUPLICATE_ENGRAM_THREAT_REDUCTION = 2.0D;

    public static final int NEURAL_ECHO_NEXUS_BIOMASS = 20;
    public static final int HIVE_HEART_NEXUS_BIOMASS = 35;
    public static final int HIVE_HEART_PULSE_RADIUS = 16;
    public static final int HIVE_HEART_CYST_PURGE_RADIUS = 12;
    public static final double HIVE_HEART_THREAT_CALM = 6.0D;
    public static final double HIVE_HEART_HOMEOSTASIS_CALM = 20.0D;

    public static final int OVERMIND_ESSENCE_SYMBIONT_XP = 25;
    public static final int OVERMIND_ESSENCE_INSIGHT_TICKS = 600;
    public static final double OVERMIND_ESSENCE_HOMEOSTASIS_CALM = 45.0D;
    public static final double OVERMIND_ESSENCE_THREAT_CALM = 8.0D;

    public static final int BRUTE_WALL_STUN_SLOW_TICKS = 60;
    public static final int BRUTE_WALL_STUN_WEAK_TICKS = 80;
    public static final float BRUTE_WALL_SELF_DAMAGE = 4.0F;
    public static final int PYRO_SHROOM_CAUTERIZE_RADIUS = 3;
    public static final double WARDEN_AURA_RADIUS = 7.0D;
    public static final int WARDEN_AURA_INTERVAL_TICKS = 40;
    public static final int WARDEN_AURA_DURATION_TICKS = 60;

    private EcosystemPurposeEngine() {
    }

    public static BloomAuraKind bloomAuraForPalette(int palette) {
        return switch (palette) {
            case 1 -> BloomAuraKind.STRENGTH;
            case 2 -> BloomAuraKind.SPEED;
            case 3 -> BloomAuraKind.FIRE_RESISTANCE;
            case 4 -> BloomAuraKind.RESISTANCE;
            case 5 -> BloomAuraKind.INSIGHT;
            default -> BloomAuraKind.SANCTUARY_REGEN;
        };
    }

    public static boolean isAntiAssimilationBlockPath(String namespace, String path) {
        return "mycelialmind".equals(namespace)
                && ("mycelial_arch_brick".equals(path) || "reinforced_bio_barrier".equals(path));
    }

    public static boolean isSeismicDamperBlockPath(String namespace, String path) {
        return "mycelialmind".equals(namespace) && "chitin_pillar".equals(path);
    }

    public static boolean isFertileSubstrateBlockPath(String namespace, String path) {
        return "mycelialmind".equals(namespace)
                && ("mycelial_stone".equals(path) || "luminescent_amber_vein".equals(path));
    }

    public static boolean isBruteStunBarrierPath(String namespace, String path) {
        return "mycelialmind".equals(namespace)
                && ("chitin_pillar".equals(path) || "reinforced_bio_barrier".equals(path));
    }

    public static boolean isPermeableEmbrasurePath(String namespace, String path) {
        return "mycelialmind".equals(namespace) && "bio_chitin_lattice".equals(path);
    }

    public static boolean isAmberConduitPath(String namespace, String path) {
        return "mycelialmind".equals(namespace) && "luminescent_amber_vein".equals(path);
    }

    public static int computePylonEffectiveRadius(
            int baseEffectiveRadius,
            boolean hasAdjacentAmberConduit,
            int adjacentBarrierCount
    ) {
        int bonus = (hasAdjacentAmberConduit ? AMBER_PYLON_BONUS_RADIUS : 0)
                + (adjacentBarrierCount >= BARRIER_PYLON_REQUIRED_FACES ? BARRIER_PYLON_BONUS_RADIUS : 0);
        return Math.max(1, baseEffectiveRadius + bonus);
    }

    public static int computeBenchEffectiveLabTier(int storedTier, boolean hasAdjacentAmberConduit) {
        int clamped = Math.max(1, Math.min(3, storedTier));
        return hasAdjacentAmberConduit ? Math.min(3, clamped + 1) : clamped;
    }

    public static boolean shouldProjectilePassThroughLattice(boolean isPurifyingProjectile, boolean ownedByHostileMob) {
        return isPurifyingProjectile || !ownedByHostileMob;
    }

    public static boolean shouldDefusePyroShroom(int fuseTicks, boolean wetOrWaterDamage, boolean neutralizingSporeHit) {
        return fuseTicks > 0 && (wetOrWaterDamage || neutralizingSporeHit);
    }

    public static int computeDowngradedInfectionAmplifier(int currentAmplifier) {
        return currentAmplifier > 0 ? currentAmplifier - 1 : -1;
    }

    public static boolean shouldProjectWardenAura(boolean alive, boolean desynced, boolean isAlliedEscort) {
        return alive && !desynced && !isAlliedEscort;
    }
}
