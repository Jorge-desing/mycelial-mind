package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Pure, deterministic policy coupling worldgen structure rarity, minimum spacing,
 * node role distribution, and bonus loot/guardian escalation to {@link HiveProgressionTier}.
 */
public final class StructureProgressionPolicy {
    private StructureProgressionPolicy() {
    }

    public static HiveProgressionTier resolveTier(WorldGenLevel level) {
        if (level != null && level.getLevel() instanceof ServerLevel serverLevel) {
            return resolveTier(serverLevel);
        }
        return HiveProgressionTier.DORMANT;
    }

    public static HiveProgressionTier resolveTier(ServerLevel serverLevel) {
        if (serverLevel == null) {
            return HiveProgressionTier.DORMANT;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        HiveProgressionTier current = HiveProgressionTier.fromThreat(hive.getThreatLevel());
        int highestIdx = Math.max(0, Math.min(HiveProgressionTier.values().length - 1, hive.getHighestTierReached()));
        return HiveProgressionTier.values()[Math.max(current.ordinal(), highestIdx)];
    }

    /**
     * 1-in-N chunk rarity for MycelialOutbreakFeature. Higher tiers spawn more densely.
     */
    public static int outbreakRarityChance(HiveProgressionTier tier) {
        return switch (tier) {
            case DORMANT -> 36;
            case AWAKENED -> 26;
            case ADAPTIVE -> 18;
            case OVERMIND -> 12;
        };
    }

    /**
     * Minimum block spacing between Mycelial Outbreak nodes.
     */
    public static int outbreakMinSpacing(HiveProgressionTier tier) {
        return switch (tier) {
            case DORMANT -> 72;
            case AWAKENED -> 60;
            case ADAPTIVE -> 52;
            case OVERMIND -> 44;
        };
    }

    /**
     * Picks the Outbreak's central SynapticNodeType based on progression tier and a [0,1) roll.
     */
    public static SynapticNodeType pickOutbreakNodeType(HiveProgressionTier tier, float roll) {
        float sentinelCutoff = switch (tier) {
            case DORMANT -> 0.05F;
            case AWAKENED -> 0.12F;
            case ADAPTIVE -> 0.22F;
            case OVERMIND -> 0.35F;
        };
        float incubatorCutoff = switch (tier) {
            case DORMANT -> 0.20F;
            case AWAKENED -> 0.35F;
            case ADAPTIVE -> 0.52F;
            case OVERMIND -> 0.70F;
        };
        if (roll < sentinelCutoff) {
            return SynapticNodeType.SENTINEL;
        }
        if (roll < incubatorCutoff) {
            return SynapticNodeType.INCUBATOR;
        }
        return SynapticNodeType.RELAY;
    }

    /**
     * 1-in-N chunk rarity for SurfaceHiveFeature.
     */
    public static int surfaceHiveRarityChance(HiveProgressionTier tier) {
        return switch (tier) {
            case DORMANT -> 240;
            case AWAKENED -> 160;
            case ADAPTIVE -> 112;
            case OVERMIND -> 80;
        };
    }

    /**
     * Minimum block spacing between Surface Hives.
     */
    public static int surfaceHiveMinSpacing(HiveProgressionTier tier) {
        return switch (tier) {
            case DORMANT -> 256;
            case AWAKENED -> 208;
            case ADAPTIVE -> 176;
            case OVERMIND -> 144;
        };
    }

    /**
     * Minimum clearance from any existing node before placing a Surface Hive.
     */
    public static int surfaceHiveNodeClearance(HiveProgressionTier tier) {
        return switch (tier) {
            case DORMANT -> 72;
            case AWAKENED -> 64;
            case ADAPTIVE -> 56;
            case OVERMIND -> 48;
        };
    }

    /**
     * Extra loot stack bonus granted in SurfaceHive / MatriarchCitadel chests at higher tiers.
     */
    public static int chestBonusCount(HiveProgressionTier tier) {
        return switch (tier) {
            case DORMANT -> 0;
            case AWAKENED -> 1;
            case ADAPTIVE -> 2;
            case OVERMIND -> 3;
        };
    }

    /**
     * Guardian evolution rank inside Matriarch Citadel based on dimension progression tier.
     */
    public static EvolutionRank citadelGuardianRank(HiveProgressionTier tier) {
        return switch (tier) {
            case DORMANT, AWAKENED -> EvolutionRank.BASE;
            case ADAPTIVE -> EvolutionRank.MUTATED;
            case OVERMIND -> EvolutionRank.ELITE;
        };
    }

    /**
     * Spawn probability of a subterranean Sealed Bio-Vault beneath an Outbreak cluster.
     */
    public static float sealedBioVaultChance(HiveProgressionTier tier) {
        if (tier == null) {
            return 0.20F;
        }
        return switch (tier) {
            case DORMANT -> 0.20F;
            case AWAKENED -> 0.35F;
            case ADAPTIVE -> 0.45F;
            case OVERMIND -> 0.55F;
        };
    }
}
