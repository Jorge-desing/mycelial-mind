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
        FOREST_CANOPY(0.72F, 0.62F, 0.14F, 0.16F, 0.65F, 0.78F, 0.80F, 0.18F, 0.9F),
        CAVERN_SINKHOLE(1.65F, 0.58F, 0.28F, 0.38F, 0.85F, 0.86F, 0.74F, 0.36F, 1.2F),
        SANCTUM_VAULT(2.35F, 0.68F, 0.34F, 0.50F, 0.92F, 0.92F, 0.78F, 0.46F, 1.5F),
        DEEP_MYCELIUM_ABYSS(3.15F, 0.46F, 0.38F, 0.62F, 1.00F, 0.96F, 0.64F, 0.56F, 2.4F);

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

    public enum SubmersionState {
        NONE,
        SPORE_FOG,
        WATER
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
     * Combines wall raycast occlusion with listener medium submersion (dense spore fog or water)
     * via multiplicative transmission loss so NONE &lt; SPORE_FOG &lt; WATER remains strictly ordered.
     */
    public static float computeEffectiveOcclusion(float wallOcclusion, SubmersionState submersion) {
        float occ = clamp01(wallOcclusion);
        SubmersionState safe = submersion != null ? submersion : SubmersionState.NONE;
        float mediumFloor = switch (safe) {
            case NONE -> 0.0F;
            case SPORE_FOG -> 0.28F;
            case WATER -> 0.74F;
        };
        return clamp01(1.0F - (1.0F - occ) * (1.0F - mediumFloor));
    }

    /**
     * Exponentially smooths occlusion transitions over live tracked OpenAL sources to prevent LPF zipper noise.
     */
    public static float smoothOcclusion(float currentOcclusion, float targetOcclusion, float alpha) {
        float current = clamp01(currentOcclusion);
        float target = clamp01(targetOcclusion);
        float step = clamp01(alpha);
        return clamp01(current + (target - current) * step);
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
     * Layered sidechain ducking multiplier stacking alert ducking with active boss combat suppression.
     */
    public static float computeLayerDuckMultiplier(
            boolean bossActive,
            float alertDuckMultiplier,
            boolean dynamicDuckingEnabled
    ) {
        if (!dynamicDuckingEnabled) {
            return 1.0F;
        }
        float safeAlert = Float.isFinite(alertDuckMultiplier) ? Math.max(0.15F, Math.min(1.0F, alertDuckMultiplier)) : 1.0F;
        float bossScale = bossActive ? 0.52F : 1.0F;
        return Math.max(0.15F, Math.min(1.0F, safeAlert * bossScale));
    }

    /**
     * Computes the adaptive low-intensity Hive tension exploration bed volume (fades out during boss encounters).
     */
    public static float computeAdaptiveExplorationBedVolume(
            double threatLevel,
            boolean bossActive,
            float ambientScale,
            float layerDuck,
            boolean enabled
    ) {
        if (!enabled || bossActive || !Double.isFinite(threatLevel) || threatLevel < 12.0D
                || !Float.isFinite(ambientScale) || ambientScale <= 0.01F) {
            return 0.0F;
        }
        double norm = Math.max(0.0D, Math.min(1.0D, (threatLevel - 12.0D) / 58.0D));
        float safeAmb = clamp01(ambientScale);
        float safeDuck = Float.isFinite(layerDuck) ? Math.max(0.15F, Math.min(1.0F, layerDuck)) : 1.0F;
        return (float) ((0.10D + 0.16D * norm) * safeAmb * safeDuck);
    }

    /**
     * Computes the rhythmic swarm skirmish combat bed volume (superseded during boss encounters).
     */
    public static float computeSwarmCombatBedVolume(
            int nearbyHostileCount,
            double threatLevel,
            boolean bossActive,
            float ambientScale,
            float layerDuck,
            boolean enabled
    ) {
        if (!enabled || bossActive || nearbyHostileCount < 2
                || !Float.isFinite(ambientScale) || ambientScale <= 0.01F) {
            return 0.0F;
        }
        double countNorm = Math.max(0.0D, Math.min(1.0D, (nearbyHostileCount - 1) / 7.0D));
        double threatNorm = Double.isFinite(threatLevel) ? Math.max(0.0D, Math.min(1.0D, threatLevel / 100.0D)) : 0.0D;
        float safeAmb = clamp01(ambientScale);
        float safeDuck = Float.isFinite(layerDuck) ? Math.max(0.15F, Math.min(1.0F, layerDuck)) : 1.0F;
        return (float) ((0.12D + 0.14D * countNorm + 0.06D * threatNorm) * safeAmb * safeDuck);
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
                || soundPath.startsWith("hive_tension_bed_")
                || soundPath.startsWith("hive_swarm_combat_")
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
                || "sporeling_chitter".equals(soundPath)
                || "cavern_spore_drip".equals(soundPath)
                || "subterranean_hyphal_creak".equals(soundPath)
                || "deep_mycelium_whisper".equals(soundPath)
                || "sanctum_harmonic_chime".equals(soundPath)) {
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

    /**
     * Normalized pitch/gain multipliers and per-hit HF timbre damping applied when a world sound begins playing.
     */
    public record HumanizedPlayback(float pitchMultiplier, float gainMultiplier, float timbreDamping) {
        public static final HumanizedPlayback IDENTITY = new HumanizedPlayback(1.0F, 1.0F, 0.0F);

        public HumanizedPlayback(float pitchMultiplier, float gainMultiplier) {
            this(pitchMultiplier, gainMultiplier, 0.0F);
        }

        public boolean isNonUnity() {
            return Math.abs(pitchMultiplier - 1.0F) > 1e-4F
                    || Math.abs(gainMultiplier - 1.0F) > 1e-4F
                    || Math.abs(timbreDamping) > 1e-4F;
        }
    }

    /**
     * Stateful deterministic anti-repeat and micro-prosody humanizer for 3D world SFX.
     * Enforces distinct pitch & HF color offsets across rapid consecutive hits of the same sound ID while
     * keeping UI cues, seamless loops, and musical/copper-amethyst tones tightly controlled.
     */
    public static final class PlaybackHumanizer {
        private static final long RECENCY_WINDOW_MS = 3200L;
        private static final float ORGANIC_PITCH_SPAN = 0.055F;
        private static final float ORGANIC_GAIN_SPAN = 0.075F;
        private static final float ORGANIC_TIMBRE_SPAN = 0.180F;
        private static final float TECH_PITCH_SPAN = 0.015F;
        private static final float TECH_GAIN_SPAN = 0.030F;
        private static final float TECH_TIMBRE_SPAN = 0.060F;

        private final long baseSeed;
        private final Map<String, RecentPlay> recentBySound = new HashMap<>();
        private long callSeq = 0L;
        private long humanizedCount = 0L;

        public PlaybackHumanizer() {
            this(0x4D5943454C49414CL);
        }

        public PlaybackHumanizer(long baseSeed) {
            this.baseSeed = baseSeed;
        }

        public synchronized HumanizedPlayback nextForSound(
                String soundPath,
                boolean isLoop,
                boolean isRelative,
                long nowMs
        ) {
            if (isLoop || isRelative || soundPath == null || soundPath.isEmpty() || soundPath.startsWith("ui_")) {
                return HumanizedPlayback.IDENTITY;
            }
            callSeq++;
            boolean cleanTech = isCleanTechOrHarmonic(soundPath);
            float pitchSpan = cleanTech ? TECH_PITCH_SPAN : ORGANIC_PITCH_SPAN;
            float gainSpan = cleanTech ? TECH_GAIN_SPAN : ORGANIC_GAIN_SPAN;
            float timbreSpan = cleanTech ? TECH_TIMBRE_SPAN : ORGANIC_TIMBRE_SPAN;
            float minDelta = cleanTech ? 0.008F : 0.025F;
            float minTimbreDelta = cleanTech ? 0.020F : 0.055F;

            long h1 = splitMix64(baseSeed ^ mixString(soundPath) ^ (callSeq * 0x9E3779B97F4A7C15L));
            long h2 = splitMix64(h1 ^ 0xBF58476D1CE4E5B9L);
            long h3 = splitMix64(h2 ^ 0x94D049BB133111EBL);
            float uPitch = (((h1 >>> 40) & 0xFFFFFFL) / (float) 0xFFFFFFL) * 2.0F - 1.0F;
            float uGain = (((h2 >>> 40) & 0xFFFFFFL) / (float) 0xFFFFFFL) * 2.0F - 1.0F;
            float uTimbre = (((h3 >>> 40) & 0xFFFFFFL) / (float) 0xFFFFFFL);

            float pitchOffset = uPitch * pitchSpan;
            float gainOffset = uGain * gainSpan;
            float timbreDamping = uTimbre * timbreSpan;

            RecentPlay prev = recentBySound.get(soundPath);
            if (prev != null && (nowMs - prev.timestampMs) <= RECENCY_WINDOW_MS) {
                float candPitch = 1.0F + pitchOffset;
                if (Math.abs(candPitch - prev.lastPitch) < minDelta) {
                    float prevOffset = prev.lastPitch - 1.0F;
                    float forcedDir = (prevOffset >= 0.0F) ? -1.0F : 1.0F;
                    float mag = Math.max(minDelta * 1.15F, Math.min(pitchSpan, Math.abs(pitchOffset) + minDelta * 0.65F));
                    pitchOffset = forcedDir * mag;
                }
                if (Math.abs(timbreDamping - prev.lastTimbre) < minTimbreDelta) {
                    timbreDamping = (prev.lastTimbre >= timbreSpan * 0.5F)
                            ? Math.max(0.0F, prev.lastTimbre - minTimbreDelta * 1.25F)
                            : Math.min(timbreSpan, prev.lastTimbre + minTimbreDelta * 1.25F);
                }
            }

            float finalPitch = Math.max(0.90F, Math.min(1.10F, 1.0F + pitchOffset));
            float finalGain = Math.max(0.88F, Math.min(1.06F, 1.0F + gainOffset));
            float finalTimbre = Math.max(0.0F, Math.min(timbreSpan, timbreDamping));

            if (recentBySound.size() > 128) {
                recentBySound.entrySet().removeIf(e -> (nowMs - e.getValue().timestampMs) > RECENCY_WINDOW_MS * 2L);
            }
            recentBySound.put(soundPath, new RecentPlay(finalPitch, finalTimbre, nowMs));

            HumanizedPlayback result = new HumanizedPlayback(finalPitch, finalGain, finalTimbre);
            if (result.isNonUnity()) {
                humanizedCount++;
            }
            return result;
        }

        public synchronized void reset() {
            recentBySound.clear();
            callSeq = 0L;
            humanizedCount = 0L;
        }

        public synchronized long getHumanizedCount() {
            return humanizedCount;
        }

        private static boolean isCleanTechOrHarmonic(String path) {
            return path.startsWith("purifier_")
                    || path.startsWith("purification_")
                    || path.startsWith("bio_scanner_")
                    || path.startsWith("bio_extractor_")
                    || path.startsWith("synaptic_bench_")
                    || path.startsWith("command_staff_")
                    || path.startsWith("colony_dialect_")
                    || path.startsWith("station_");
        }

        private static long mixString(String s) {
            long h = 0x100000001B3L;
            for (int i = 0; i < s.length(); i++) {
                h ^= s.charAt(i);
                h *= 0x100000001B3L;
            }
            return h;
        }

        private static long splitMix64(long z) {
            z += 0x9E3779B97F4A7C15L;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        private record RecentPlay(float lastPitch, float lastTimbre, long timestampMs) {
        }
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
