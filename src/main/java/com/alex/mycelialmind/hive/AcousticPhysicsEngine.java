package com.alex.mycelialmind.hive;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure acoustic physics, OpenAL EFX preset, raycast occlusion, and swarm anti-cacophony engine.
 * Zero Minecraft client or LWJGL imports so all formulas remain deterministic and JUnit-testable.
 */
public final class AcousticPhysicsEngine {

    public enum MaterialAbsorption {
        AIR(0.0F),
        FOLIAGE_GLASS_WATER(0.10F),
        MYCELIUM_SOIL_MOSS(0.24F),
        CHITIN_WOOD_BONE(0.36F),
        STONE_DEEPSLATE_METAL(0.55F),
        DENSE_BARRIER_OBSIDIAN(0.85F);

        private final float absorptionPerBlock;

        MaterialAbsorption(float absorptionPerBlock) {
            this.absorptionPerBlock = absorptionPerBlock;
        }

        public float absorptionPerBlock() {
            return absorptionPerBlock;
        }
    }

    public enum AcousticEnvironment {
        OPEN_AIR(0.25F, 0.72F, 0.05F, 0.04F, 0.45F, 0.60F, 0.92F, 0.0F, 0.4F),
        FOREST_CANOPY(0.72F, 0.62F, 0.14F, 0.16F, 0.65F, 0.78F, 0.80F, 0.22F, 0.9F),
        CAVERN_SINKHOLE(1.65F, 0.58F, 0.28F, 0.38F, 0.85F, 0.86F, 0.74F, 0.42F, 1.2F),
        SANCTUM_VAULT(2.35F, 0.68F, 0.34F, 0.50F, 0.92F, 0.92F, 0.78F, 0.50F, 1.5F),
        DEEP_MYCELIUM_ABYSS(3.15F, 0.46F, 0.38F, 0.62F, 1.00F, 0.96F, 0.64F, 0.60F, 2.4F);

        private final float decayTime;
        private final float decayHfRatio;
        private final float reflectionsGain;
        private final float lateReverbGain;
        private final float density;
        private final float diffusion;
        private final float gainHf;
        private final float wetSendGain;
        private final float airAbsorptionFactor;

        AcousticEnvironment(
                float decayTime,
                float decayHfRatio,
                float reflectionsGain,
                float lateReverbGain,
                float density,
                float diffusion,
                float gainHf,
                float wetSendGain,
                float airAbsorptionFactor
        ) {
            this.decayTime = decayTime;
            this.decayHfRatio = decayHfRatio;
            this.reflectionsGain = reflectionsGain;
            this.lateReverbGain = lateReverbGain;
            this.density = density;
            this.diffusion = diffusion;
            this.gainHf = gainHf;
            this.wetSendGain = wetSendGain;
            this.airAbsorptionFactor = airAbsorptionFactor;
        }

        public float decayTime() {
            return decayTime;
        }

        public float decayHfRatio() {
            return decayHfRatio;
        }

        public float reflectionsGain() {
            return reflectionsGain;
        }

        public float lateReverbGain() {
            return lateReverbGain;
        }

        public float density() {
            return density;
        }

        public float diffusion() {
            return diffusion;
        }

        public float gainHf() {
            return gainHf;
        }

        public float wetSendGain() {
            return wetSendGain;
        }

        public float airAbsorptionFactor() {
            return airAbsorptionFactor;
        }
    }

    public enum VoicePriority {
        CRITICAL,
        HIGH,
        NORMAL,
        LOW
    }

    private AcousticPhysicsEngine() {
    }

    /**
     * Converts cumulative raycast material absorption along a listener-to-source path into a [0, 1] occlusion factor.
     */
    public static float computeOcclusionFactor(float cumulativeAbsorption) {
        if (!Float.isFinite(cumulativeAbsorption) || cumulativeAbsorption <= 0.0F) {
            return 0.0F;
        }
        double factor = 1.0D - Math.exp(-cumulativeAbsorption * 1.15D);
        return (float) Math.max(0.0D, Math.min(1.0D, factor));
    }

    /**
     * Broadband OpenAL AL_LOWPASS_GAIN direct path attenuation [0.38, 1.0].
     */
    public static float computeDirectLowPassGain(float occlusionFactor) {
        float occ = clamp01(occlusionFactor);
        return 1.0F - (0.62F * occ);
    }

    /**
     * High-frequency OpenAL AL_LOWPASS_GAINHF direct path rolloff [0.06, 1.0].
     */
    public static float computeDirectLowPassGainHf(float occlusionFactor) {
        float occ = clamp01(occlusionFactor);
        return (float) Math.max(0.06D, Math.min(1.0D, Math.pow(0.06D, occ)));
    }

    /**
     * Auxiliary reverb send HF damping when a sound originates behind occluding walls [0.18, 1.0].
     */
    public static float computeSendLowPassGainHf(float occlusionFactor) {
        float occ = clamp01(occlusionFactor);
        return (float) Math.max(0.18D, Math.min(1.0D, Math.pow(0.18D, occ * 0.85D)));
    }

    /**
     * OpenAL AL_AIR_ABSORPTION_FACTOR for distance-dependent high-frequency damping (0.0 - 10.0).
     */
    public static float computeAirAbsorptionFactor(AcousticEnvironment env, boolean underwater) {
        if (underwater) {
            return 6.0F;
        }
        AcousticEnvironment safeEnv = env != null ? env : AcousticEnvironment.OPEN_AIR;
        return safeEnv.airAbsorptionFactor();
    }

    /**
     * Classifies the listener's real-time acoustic environment from biome/dimension context and 6-ray enclosure telemetry.
     */
    public static AcousticEnvironment classifyEnvironment(
            boolean inDeepMycelium,
            boolean nearSinkhole,
            boolean inSanctumOrCitadel,
            boolean hasSkyAbove,
            float enclosureRatio,
            float averageWallDistance
    ) {
        float enc = clamp01(enclosureRatio);
        float wallDist = Float.isFinite(averageWallDistance) ? Math.max(0.0F, averageWallDistance) : 16.0F;

        if (inDeepMycelium) {
            return inSanctumOrCitadel ? AcousticEnvironment.SANCTUM_VAULT : AcousticEnvironment.DEEP_MYCELIUM_ABYSS;
        }
        if (inSanctumOrCitadel) {
            return AcousticEnvironment.SANCTUM_VAULT;
        }
        if (nearSinkhole) {
            return AcousticEnvironment.CAVERN_SINKHOLE;
        }
        if (!hasSkyAbove && enc >= 0.45F) {
            return wallDist >= 9.5F ? AcousticEnvironment.SANCTUM_VAULT : AcousticEnvironment.CAVERN_SINKHOLE;
        }
        if (enc >= 0.25F) {
            return AcousticEnvironment.FOREST_CANOPY;
        }
        return AcousticEnvironment.OPEN_AIR;
    }

    /**
     * Determines whether native OpenAL EFX processing should apply to a sound source.
     * Always applies to mycelialmind:* when enabled, and extends to minecraft:* inside Deep Mycelium or near a Sinkhole.
     */
    public static boolean shouldApplyEfx(
            String namespace,
            boolean inDeepMycelium,
            boolean nearSinkhole,
            boolean efxEnabled,
            boolean vanillaInMycelialZonesEnabled
    ) {
        if (!efxEnabled || namespace == null || namespace.isEmpty()) {
            return false;
        }
        if ("mycelialmind".equals(namespace)) {
            return true;
        }
        if ("minecraft".equals(namespace)) {
            return vanillaInMycelialZonesEnabled && (inDeepMycelium || nearSinkhole);
        }
        return false;
    }

    /**
     * Classifies a sound event path into an anti-cacophony voice priority tier.
     */
    public static VoicePriority classifySoundPriority(String soundPath, boolean isLoop, boolean isRelative) {
        if (isLoop || isRelative || soundPath == null || soundPath.isEmpty()) {
            return VoicePriority.CRITICAL;
        }
        if (soundPath.startsWith("ui_")
                || soundPath.startsWith("boss_")
                || soundPath.startsWith("apex_matriarch_")
                || soundPath.startsWith("hive_core_")
                || soundPath.startsWith("spore_behemoth_")
                || soundPath.startsWith("hive_trait_")
                || soundPath.startsWith("hive_synergy_")
                || soundPath.startsWith("hive_threat_")
                || soundPath.startsWith("fungal_tide_")
                || soundPath.startsWith("colony_raid_")
                || soundPath.startsWith("biome_")
                || soundPath.startsWith("sinkhole_updraft_")
                || soundPath.startsWith("hive_altar_")
                || soundPath.startsWith("matriarch_altar_")) {
            return VoicePriority.CRITICAL;
        }
        if (soundPath.startsWith("mob_step_")
                || soundPath.endsWith("_step")
                || "sporeling_chitter".equals(soundPath)) {
            return VoicePriority.LOW;
        }
        if (soundPath.endsWith("_ambient")
                || soundPath.endsWith("_chitter")
                || soundPath.endsWith("_hiss")
                || soundPath.endsWith("_gurgle")
                || soundPath.endsWith("_growl")
                || soundPath.endsWith("_bellow")
                || soundPath.endsWith("_screech")
                || soundPath.endsWith("_hum")
                || soundPath.endsWith("_hurt")
                || soundPath.endsWith("_death")) {
            return VoicePriority.NORMAL;
        }
        return VoicePriority.HIGH;
    }

    /**
     * Computes the reactive volume for the Sinkhole updraft ambient bed loop with smooth distance falloff and ducking.
     */
    public static float computeSinkholeUpdraftLoopVolume(
            double distSq,
            double maxRadius,
            float ambientScale,
            float duckMultiplier,
            boolean bedEnabled
    ) {
        if (!bedEnabled || !Double.isFinite(distSq) || !Double.isFinite(maxRadius) || maxRadius <= 0.0D) {
            return 0.0F;
        }
        double maxSq = maxRadius * maxRadius;
        if (distSq >= maxSq || distSq < 0.0D) {
            return 0.0F;
        }
        double norm = Math.sqrt(distSq) / maxRadius;
        double falloff = (1.0D - norm) * (1.0D - norm);
        float safeAmb = Float.isFinite(ambientScale) ? Math.max(0.0F, Math.min(1.0F, ambientScale)) : 1.0F;
        float safeDuck = Float.isFinite(duckMultiplier) ? Math.max(0.15F, Math.min(1.0F, duckMultiplier)) : 1.0F;
        return (float) Math.max(0.0D, Math.min(0.45D, 0.36D * falloff * safeAmb * safeDuck));
    }

    /**
     * Sliding-window voice budget limiter preventing swarm audio saturation without dropping critical cues.
     */
    public static final class SwarmVoiceBudget {
        private static final long GLOBAL_WINDOW_MS = 350L;
        private static final int GLOBAL_NON_CRITICAL_CAP = 14;
        private static final int HIGH_CAP = 8;
        private static final int NORMAL_CAP = 6;
        private static final int LOW_CAP = 5;
        private static final long NORMAL_SAME_ID_WINDOW_MS = 180L;
        private static final long LOW_SAME_ID_WINDOW_MS = 140L;
        private static final double DISTANT_SQ = 18.0D * 18.0D;

        private final Deque<VoiceStamp> recentVoices = new ArrayDeque<>();
        private final Map<String, Deque<Long>> bySoundId = new HashMap<>();
        private long acceptedCount;
        private long suppressedCount;

        public synchronized boolean evaluate(String soundId, VoicePriority priority, double distSq, long nowMs) {
            if (priority == VoicePriority.CRITICAL || soundId == null) {
                acceptedCount++;
                return true;
            }
            prune(nowMs);

            int totalRecent = recentVoices.size();
            int samePriorityCount = 0;
            for (VoiceStamp stamp : recentVoices) {
                if (stamp.priority == priority) {
                    samePriorityCount++;
                }
            }

            boolean distant = Double.isFinite(distSq) && distSq > DISTANT_SQ;
            int priorityCap = switch (priority) {
                case HIGH -> distant ? 5 : HIGH_CAP;
                case NORMAL -> distant ? 3 : NORMAL_CAP;
                case LOW -> distant ? 2 : LOW_CAP;
                case CRITICAL -> Integer.MAX_VALUE;
            };
            int globalCap = (priority == VoicePriority.HIGH) ? GLOBAL_NON_CRITICAL_CAP + 4 : GLOBAL_NON_CRITICAL_CAP;

            if (totalRecent >= globalCap || samePriorityCount >= priorityCap) {
                suppressedCount++;
                return false;
            }

            long sameIdWindow = switch (priority) {
                case LOW -> LOW_SAME_ID_WINDOW_MS;
                case NORMAL -> NORMAL_SAME_ID_WINDOW_MS;
                default -> 90L;
            };
            int sameIdCap = (priority == VoicePriority.HIGH) ? 3 : (distant ? 1 : 2);
            Deque<Long> idQueue = bySoundId.computeIfAbsent(soundId, k -> new ArrayDeque<>());
            while (!idQueue.isEmpty() && (nowMs - idQueue.peekFirst()) > sameIdWindow) {
                idQueue.pollFirst();
            }
            if (idQueue.size() >= sameIdCap) {
                suppressedCount++;
                return false;
            }

            recentVoices.addLast(new VoiceStamp(priority, nowMs));
            idQueue.addLast(nowMs);
            acceptedCount++;
            return true;
        }

        private void prune(long nowMs) {
            while (!recentVoices.isEmpty() && (nowMs - recentVoices.peekFirst().timestampMs) > GLOBAL_WINDOW_MS) {
                recentVoices.pollFirst();
            }
            if (bySoundId.size() > 64) {
                bySoundId.entrySet().removeIf(entry -> {
                    Deque<Long> q = entry.getValue();
                    while (!q.isEmpty() && (nowMs - q.peekFirst()) > GLOBAL_WINDOW_MS) {
                        q.pollFirst();
                    }
                    return q.isEmpty();
                });
            }
        }

        public synchronized void reset() {
            recentVoices.clear();
            bySoundId.clear();
            acceptedCount = 0L;
            suppressedCount = 0L;
        }

        public synchronized long getAcceptedCount() {
            return acceptedCount;
        }

        public synchronized long getSuppressedCount() {
            return suppressedCount;
        }

        public synchronized int getActiveWindowCount(long nowMs) {
            prune(nowMs);
            return recentVoices.size();
        }

        private record VoiceStamp(VoicePriority priority, long timestampMs) {
        }
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
