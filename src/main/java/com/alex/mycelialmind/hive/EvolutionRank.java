package com.alex.mycelialmind.hive;

/**
 * Evolution tiers for MycelialMob units, scaling physical size, combat stats,
 * emissive tint, and XP/loot rewards.
 */
public enum EvolutionRank {
    BASE(0, 1.0D, 1.0D, 1.0F, 1.0D, 1.0F, 1.0F),
    MUTATED(1, 1.25D, 1.15D, 1.12F, 1.25D, 0.89F, 1.10F),
    ELITE(2, 1.60D, 1.35D, 1.25F, 1.75D, 0.78F, 1.22F);

    private final int id;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final float scale;
    private final double xpMultiplier;
    private final float voicePitchMultiplier;
    private final float soundVolumeMultiplier;

    EvolutionRank(int id, double healthMultiplier, double damageMultiplier, float scale,
                  double xpMultiplier, float voicePitchMultiplier, float soundVolumeMultiplier) {
        this.id = id;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.scale = scale;
        this.xpMultiplier = xpMultiplier;
        this.voicePitchMultiplier = voicePitchMultiplier;
        this.soundVolumeMultiplier = soundVolumeMultiplier;
    }

    public int id() {
        return id;
    }

    public double healthMultiplier() {
        return healthMultiplier;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public float scale() {
        return scale;
    }

    public double xpMultiplier() {
        return xpMultiplier;
    }

    public float voicePitchMultiplier() {
        return voicePitchMultiplier;
    }

    public float soundVolumeMultiplier() {
        return soundVolumeMultiplier;
    }

    public static float computeTacticalVoicePitchMultiplier(
            EvolutionRank rank,
            HiveStrain strain,
            boolean frenzy,
            boolean desynced,
            SynapticCommandSystem.CommandRole role,
            boolean occluded
    ) {
        float pitch = rank == null ? 1.0F : rank.voicePitchMultiplier();
        if (strain == HiveStrain.AMBER) {
            pitch *= 1.06F;
        } else if (strain == HiveStrain.CRIMSON) {
            pitch *= 0.93F;
        }
        if (frenzy && !desynced) {
            pitch *= 1.10F;
        }
        if (role == SynapticCommandSystem.CommandRole.LEADER && !desynced) {
            pitch *= 0.94F;
        }
        if (desynced) {
            pitch *= 0.86F;
        }
        if (occluded) {
            pitch *= 0.92F;
        }
        return Math.max(0.45F, Math.min(1.85F, pitch));
    }

    public static float computeTacticalSoundVolumeMultiplier(
            EvolutionRank rank,
            boolean frenzy,
            SynapticCommandSystem.CommandRole role,
            boolean occluded
    ) {
        float vol = rank == null ? 1.0F : rank.soundVolumeMultiplier();
        if (role == SynapticCommandSystem.CommandRole.LEADER) {
            vol *= 1.12F;
        }
        if (frenzy) {
            vol *= 1.08F;
        }
        if (occluded) {
            vol *= 0.58F;
        }
        return Math.max(0.20F, Math.min(1.80F, vol));
    }

    public static int computeThreatMilestone(double threatPercent) {
        if (!Double.isFinite(threatPercent) || threatPercent < 25.0D) {
            return 0;
        }
        if (threatPercent >= 100.0D) {
            return 100;
        }
        if (threatPercent >= 75.0D) {
            return 75;
        }
        if (threatPercent >= 50.0D) {
            return 50;
        }
        return 25;
    }

    public static boolean shouldTriggerImmuneSurgeAlert(
            boolean inDeepMycelium,
            int previousStageOrdinal,
            int currentStageOrdinal,
            int immuneResponseOrdinal
    ) {
        return inDeepMycelium
                && currentStageOrdinal == immuneResponseOrdinal
                && previousStageOrdinal >= 0
                && previousStageOrdinal < immuneResponseOrdinal;
    }

    public static float computeAcidRainSizzleVolume(
            double homeostasisStress,
            boolean hasOverheadShelter,
            double ambientVolume
    ) {
        if (hasOverheadShelter || !Double.isFinite(ambientVolume) || ambientVolume <= 0.01D) {
            return 0.0F;
        }
        double safeStress = Double.isFinite(homeostasisStress) ? homeostasisStress : 0.0D;
        double clampedAmbient = Math.min(1.0D, Math.max(0.0D, ambientVolume));
        double stressNorm = Math.min(1.0D, Math.max(0.0D, safeStress / 100.0D));
        return (float) (clampedAmbient * (0.24D + 0.26D * stressNorm));
    }

    public static float computePylonHumPitch(int pylonTier) {
        if (pylonTier >= 3) {
            return 1.24F;
        }
        if (pylonTier == 2) {
            return 1.12F;
        }
        return 1.00F;
    }

    public static double computePylonHumRadius(int pylonTier) {
        if (pylonTier >= 3) {
            return 24.0D;
        }
        if (pylonTier == 2) {
            return 19.0D;
        }
        return 15.0D;
    }

    public static EvolutionRank fromId(int id) {
        for (EvolutionRank rank : values()) {
            if (rank.id == id) {
                return rank;
            }
        }
        return BASE;
    }
}
