package com.alex.mycelialmind.world;

import java.util.Set;

/**
 * Pure dimensional homeostasis rules for deep_mycelium:
 * chunk stress escalation/decay, foreign-block & natural-ceiling classification,
 * enzymatic acid-rain mitigation via Chitin Armor, and spore-fog visibility curves.
 */
public final class DimensionalHomeostasisEngine {
    public enum HomeostasisStage {
        STABLE(0, "STABLE", 0xFF72EFDD),
        IRRITATED(1, "IRRITATED", 0xFFFFB703),
        IMMUNE_RESPONSE(2, "IMMUNE RESPONSE", 0xFFFF3366);

        private static final HomeostasisStage[] BY_ID = values();

        private final int id;
        private final String displayLabel;
        private final int argbColor;

        HomeostasisStage(int id, String displayLabel, int argbColor) {
            this.id = id;
            this.displayLabel = displayLabel;
            this.argbColor = argbColor;
        }

        public int id() {
            return id;
        }

        public String displayLabel() {
            return displayLabel;
        }

        public int argbColor() {
            return argbColor;
        }

        public static HomeostasisStage byId(int id) {
            return (id >= 0 && id < BY_ID.length) ? BY_ID[id] : STABLE;
        }
    }

    public record FogRange(float nearPlane, float farPlane) {
    }

    public static final double STRESS_MIN = 0.0;
    public static final double STRESS_MAX = 100.0;
    public static final double IRRITATED_THRESHOLD = 40.0;
    public static final double IMMUNE_THRESHOLD = 70.0;
    public static final double CYTOKINE_STORM_THRESHOLD = 85.0;
    public static final double INFLAMMATORY_DIFFUSION_CAP_WITHOUT_FOREIGN = 45.0;
    public static final int MAX_SENSITIZATION_LEVEL = 3;
    public static final int LEUKOCYTE_PATROL_COOLDOWN_TICKS = 1200; // 60 seconds

    public static final int PYLON_EXCESS_THRESHOLD = 1;
    public static final int TURRET_EXCESS_THRESHOLD = 2;
    public static final int SMOTHER_FACE_THRESHOLD = 4;

    public static final double PYLON_FIRST_PLACE_STRESS = 12.0;
    public static final double PYLON_EXCESS_PLACE_STRESS = 30.0;
    public static final double TURRET_BASE_PLACE_STRESS = 8.0;
    public static final double TURRET_EXCESS_PLACE_STRESS = 18.0;
    public static final double FOREIGN_BLOCK_PLACE_STRESS = 2.5;

    public static final double SYMBIOTIC_PLACE_CALM = 8.0;
    public static final double SUPPRESSOR_SEDATION = 45.0;
    public static final double TRUCE_PHEROMONE_SEDATION = 28.0;
    public static final int SUPPRESSOR_SENSITIZATION_REDUCTION = 2;
    public static final int TRUCE_PHEROMONE_SENSITIZATION_REDUCTION = 1;
    public static final int SYMBIOTIC_SENSITIZATION_REDUCTION = 1;

    public static final double PASSIVE_DECAY_PER_CYCLE = 1.6;
    public static final double SYMBIOTIC_BONUS_DECAY = 2.4;

    public static final int THERMAL_PULSE_DURATION_TICKS = 300; // 15 seconds
    public static final int THERMAL_PULSE_COOLDOWN_TICKS = 440; // 22 seconds

    private static final Set<String> FOREIGN_MYCELIAL_PATHS = Set.of(
            "purification_pylon",
            "purification_turret",
            "synaptic_research_bench",
            "reinforced_bio_barrier"
    );

    private static final Set<String> NATURAL_CEILING_MYCELIAL_PATHS = Set.of(
            "mycelial_stone",
            "creeping_mycelium",
            "mycelial_arch_brick",
            "antibody_cyst",
            "chitin_pillar",
            "luminescent_amber_vein",
            "mycelial_node",
            "incubator_node",
            "sentinel_node",
            "spore_vent"
    );

    private DimensionalHomeostasisEngine() {
    }

    public static double clampStress(double stress) {
        if (!Double.isFinite(stress)) {
            return STRESS_MIN;
        }
        return Math.max(STRESS_MIN, Math.min(STRESS_MAX, stress));
    }

    public static int clampSensitization(int level) {
        return Math.max(0, Math.min(MAX_SENSITIZATION_LEVEL, level));
    }

    public static boolean isCytokineStorm(double stress) {
        return clampStress(stress) >= CYTOKINE_STORM_THRESHOLD;
    }

    public static double computeSensitizedStressDelta(double rawDelta, int sensitizationLevel) {
        if (!Double.isFinite(rawDelta) || rawDelta == 0.0) {
            return 0.0;
        }
        int sens = clampSensitization(sensitizationLevel);
        if (rawDelta > 0.0) {
            return rawDelta * (1.0 + 0.15 * sens);
        }
        return rawDelta * (1.0 - 0.12 * sens);
    }

    public static double computeNativeTissueTraumaStress(
            String namespace,
            String path,
            boolean nearSymbioticNode
    ) {
        if (nearSymbioticNode || !"mycelialmind".equals(namespace) || path == null) {
            return 0.0;
        }
        return switch (path) {
            case "mycelial_node", "incubator_node", "sentinel_node" -> 4.0;
            case "luminescent_amber_vein", "spore_vent" -> 2.8;
            case "chitin_pillar", "mycelial_arch_brick", "creeping_mycelium" -> 1.8;
            case "mycelial_stone", "bioluminescent_mycelial_cap", "spore_bulb", "mycelial_tendrils" -> 1.2;
            default -> 0.0;
        };
    }

    public static double computeInflammatoryNeighborDiffusion(
            double sourceStress,
            double neighborStress,
            boolean neighborHasForeignBlocks
    ) {
        double safeSource = clampStress(sourceStress);
        double safeNeighbor = clampStress(neighborStress);
        if (safeSource < IMMUNE_THRESHOLD) {
            return 0.0;
        }
        double rawTransfer = Math.max(0.0, (safeSource - 60.0) * 0.06);
        if (!neighborHasForeignBlocks) {
            double headroom = Math.max(0.0, INFLAMMATORY_DIFFUSION_CAP_WITHOUT_FOREIGN - safeNeighbor);
            return Math.min(rawTransfer, headroom);
        }
        return Math.min(rawTransfer, Math.max(0.0, STRESS_MAX - safeNeighbor));
    }

    public static HomeostasisStage stageForStress(double stress) {
        double clamped = clampStress(stress);
        if (clamped >= IMMUNE_THRESHOLD) {
            return HomeostasisStage.IMMUNE_RESPONSE;
        }
        if (clamped >= IRRITATED_THRESHOLD) {
            return HomeostasisStage.IRRITATED;
        }
        return HomeostasisStage.STABLE;
    }

    public static double computeExplosionStress(int affectedBlocks, float explosionRadius, double configScale) {
        int blocks = Math.max(0, affectedBlocks);
        double radius = Math.max(0.0, explosionRadius);
        double raw = Math.min(55.0, Math.max(15.0, 22.0 + blocks * 0.65 + radius * 2.0));
        return raw * Math.max(0.1, configScale);
    }

    public static double computePeriodicStressDelta(
            int activePylons,
            int nearbyTurrets,
            int nearbySymbioticNodes
    ) {
        return computePeriodicStressDelta(activePylons, activePylons > 0 ? 1 : 0, nearbyTurrets, nearbySymbioticNodes);
    }

    public static double computePeriodicStressDelta(
            int activePylons,
            int maxActivePylonTier,
            int nearbyTurrets,
            int nearbySymbioticNodes
    ) {
        int pylons = Math.max(0, activePylons);
        int tier = pylons > 0 ? Math.max(1, Math.min(3, maxActivePylonTier)) : 0;
        int turrets = Math.max(0, nearbyTurrets);
        int symbiotic = Math.max(0, nearbySymbioticNodes);
        double tierStabilization = tier >= 3 ? 1.8D : (tier == 2 ? 0.8D : 0.0D);

        if (pylons > PYLON_EXCESS_THRESHOLD || turrets > TURRET_EXCESS_THRESHOLD) {
            double aggravation = Math.max(0, pylons - PYLON_EXCESS_THRESHOLD) * 3.5
                    + Math.max(0, turrets - TURRET_EXCESS_THRESHOLD) * 1.5
                    - symbiotic * 1.5
                    - tierStabilization * 0.5;
            return Math.max(0.5, aggravation);
        }
        return -(PASSIVE_DECAY_PER_CYCLE + symbiotic * SYMBIOTIC_BONUS_DECAY + tierStabilization);
    }

    public static boolean isForeignBlockId(
            String namespace,
            String path,
            boolean isAir,
            boolean isFluid,
            boolean isBedrock
    ) {
        if (isAir || isFluid || isBedrock || namespace == null || path == null) {
            return false;
        }
        if ("mycelialmind".equals(namespace)) {
            return FOREIGN_MYCELIAL_PATHS.contains(path);
        }
        return true;
    }

    public static boolean isNaturalCavernCeilingId(String namespace, String path) {
        if (namespace == null || path == null) {
            return false;
        }
        if ("minecraft".equals(namespace) && "bedrock".equals(path)) {
            return true;
        }
        return "mycelialmind".equals(namespace) && NATURAL_CEILING_MYCELIAL_PATHS.contains(path);
    }

    public static float computeAcidRainDamage(float baseDamage, int chitinPieces, boolean fullChitinSet) {
        float safeBase = Math.max(0.25F, baseDamage);
        if (fullChitinSet || chitinPieces >= 4) {
            return Math.max(0.25F, safeBase * 0.25F);
        }
        int clamped = Math.max(0, Math.min(3, chitinPieces));
        return Math.max(0.5F, safeBase * (1.0F - 0.18F * clamped));
    }

    public static FogRange computeSporeFogRange(
            HomeostasisStage stage,
            double stress,
            boolean thermalVisionActive,
            boolean irisShadersActive
    ) {
        if (stage == null || stage == HomeostasisStage.STABLE) {
            return null;
        }
        if (thermalVisionActive) {
            return new FogRange(6.0F, irisShadersActive ? 38.0F : 32.0F);
        }
        if (stage == HomeostasisStage.IRRITATED) {
            float t = (float) Math.max(0.0, Math.min(1.0, (clampStress(stress) - IRRITATED_THRESHOLD) / 30.0));
            float near = irisShadersActive ? 5.0F : 3.5F;
            float far = (irisShadersActive ? 28.0F : 22.0F) - t * 6.0F;
            return new FogRange(near, far);
        }
        float t = (float) Math.max(0.0, Math.min(1.0, (clampStress(stress) - IMMUNE_THRESHOLD) / 30.0));
        float near = irisShadersActive ? 2.0F : 1.2F;
        float far = (irisShadersActive ? 10.5F : 7.5F) - t * 2.0F;
        return new FogRange(near, far);
    }
}
