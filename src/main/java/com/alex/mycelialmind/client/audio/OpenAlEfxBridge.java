package com.alex.mycelialmind.client.audio;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.AcousticEnvironment;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.SubmersionState;
import com.alex.mycelialmind.mixin.client.ChannelAccessor;
import com.mojang.blaze3d.audio.Channel;
import net.neoforged.fml.ModList;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Zero-Mixin, zero-AT native LWJGL OpenAL EFX acoustic bridge.
 * Initializes EAXReverb auxiliary slots and a quantized low-pass filter pool lazily on the
 * SoundEngine audio thread so OpenAL context ownership is always respected across reloads.
 */
public final class OpenAlEfxBridge {
    private static final int OCCLUSION_BUCKETS = 32;

    private static final AtomicBoolean REINIT_REQUESTED = new AtomicBoolean(true);
    private static final AtomicLong APPLIED_SOURCES = new AtomicLong(0L);
    private static final AtomicLong DYNAMIC_UPDATES = new AtomicLong(0L);
    private static final AtomicLong HUMANIZED_SOURCES = new AtomicLong(0L);
    private static final AtomicLong RESET_SOURCES = new AtomicLong(0L);
    private static final AtomicLong REVERB_GAIN_UPDATES = new AtomicLong(0L);

    private static volatile boolean externalModConflict = false;
    private static volatile boolean efxSupported = false;
    private static volatile boolean initialized = false;
    private static volatile String statusReason = "pending_first_source";

    private static long boundContext = 0L;
    private static Field channelSourceField;
    private static boolean reflectionResolved = false;

    private static final Map<AcousticEnvironment, Integer> AUX_SLOTS = new EnumMap<>(AcousticEnvironment.class);
    private static final Map<AcousticEnvironment, Integer> REVERB_EFFECTS = new EnumMap<>(AcousticEnvironment.class);
    private static final Map<AcousticEnvironment, Float> LAST_SLOT_GAINS = new EnumMap<>(AcousticEnvironment.class);
    private static final int[] DIRECT_LPF_POOL = new int[OCCLUSION_BUCKETS];
    private static final int[] SEND_LPF_POOL = new int[OCCLUSION_BUCKETS];

    private OpenAlEfxBridge() {
    }

    public record SourceAcousticSnapshot(
            AcousticEnvironment environment,
            float occlusionFactor,
            SubmersionState submersion,
            boolean applyReverb,
            long createdAtMs,
            float pitchMultiplier,
            float gainMultiplier,
            float timbreDamping,
            float distanceMeters,
            boolean bossCue
    ) {
        public SourceAcousticSnapshot(
                AcousticEnvironment environment,
                float occlusionFactor,
                SubmersionState submersion,
                boolean applyReverb,
                long createdAtMs,
                float pitchMultiplier,
                float gainMultiplier,
                float timbreDamping,
                float distanceMeters
        ) {
            this(environment, occlusionFactor, submersion, applyReverb, createdAtMs,
                    pitchMultiplier, gainMultiplier, timbreDamping, distanceMeters, false);
        }

        public SourceAcousticSnapshot(
                AcousticEnvironment environment,
                float occlusionFactor,
                SubmersionState submersion,
                boolean applyReverb,
                long createdAtMs,
                float pitchMultiplier,
                float gainMultiplier,
                float timbreDamping
        ) {
            this(environment, occlusionFactor, submersion, applyReverb, createdAtMs,
                    pitchMultiplier, gainMultiplier, timbreDamping, -1.0F, false);
        }

        public SourceAcousticSnapshot(
                AcousticEnvironment environment,
                float occlusionFactor,
                SubmersionState submersion,
                boolean applyReverb,
                long createdAtMs,
                float pitchMultiplier,
                float gainMultiplier
        ) {
            this(environment, occlusionFactor, submersion, applyReverb, createdAtMs,
                    pitchMultiplier, gainMultiplier, 0.0F, -1.0F, false);
        }

        public SourceAcousticSnapshot(
                AcousticEnvironment environment,
                float occlusionFactor,
                SubmersionState submersion,
                boolean applyReverb,
                long createdAtMs
        ) {
            this(environment, occlusionFactor, submersion, applyReverb, createdAtMs,
                    1.0F, 1.0F, 0.0F, -1.0F, false);
        }

        public SourceAcousticSnapshot(
                AcousticEnvironment environment,
                float occlusionFactor,
                boolean underwater,
                boolean applyReverb,
                long createdAtMs
        ) {
            this(
                    environment,
                    occlusionFactor,
                    underwater ? SubmersionState.WATER : SubmersionState.NONE,
                    applyReverb,
                    createdAtMs,
                    1.0F,
                    1.0F,
                    0.0F,
                    -1.0F,
                    false
            );
        }

        public boolean underwater() {
            return submersion == SubmersionState.WATER;
        }
    }

    /**
     * Signals that SoundEngine reloaded its OpenAL library and EFX handles must be rebuilt on next audio play.
     */
    public static void markNeedsReinit() {
        REINIT_REQUESTED.set(true);
    }

    /**
     * Applies precomputed environment reverb, direct occlusion low-pass filter, and air absorption
     * directly to an OpenAL channel on the SoundEngine executor thread.
     */
    public static synchronized boolean applyToChannel(Channel channel, SourceAcousticSnapshot snapshot) {
        if (channel == null || snapshot == null) {
            return false;
        }
        if (!ensureInitializedOnAudioThread()) {
            return false;
        }
        if (!resolveChannelReflection()) {
            statusReason = "channel_source_reflection_unavailable";
            return false;
        }
        int sourceId = extractSourceId(channel);
        return applyToSource(sourceId, snapshot);
    }

    /**
     * Checks on the active OpenAL audio thread whether a channel is currently bound to a playing AL source.
     */
    public static synchronized boolean isChannelPlaying(Channel channel) {
        if (channel == null || !initialized || !efxSupported) {
            return false;
        }
        long ctx = safeGetCurrentContext();
        if (ctx == 0L || ctx != boundContext || !resolveChannelReflection()) {
            return false;
        }
        int sourceId = extractSourceId(channel);
        if (sourceId <= 0 || !AL10.alIsSource(sourceId)) {
            return false;
        }
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    /**
     * Re-applies dynamic room EFX, smoothed occlusion, and fluid submersion to an already-playing OpenAL channel
     * on the audio thread without restarting playback.
     */
    public static synchronized boolean updateActiveChannel(Channel channel, SourceAcousticSnapshot snapshot) {
        if (channel == null || snapshot == null) {
            return false;
        }
        if (safeGetCurrentContext() == 0L) {
            return false;
        }
        if (!ensureInitializedOnAudioThread()) {
            return false;
        }
        if (!resolveChannelReflection()) {
            return false;
        }
        int sourceId = extractSourceId(channel);
        return updateActiveSource(sourceId, snapshot);
    }

    /**
     * Updates an already-playing OpenAL source handle live without restarting playback.
     */
    public static synchronized boolean updateActiveSource(int sourceId, SourceAcousticSnapshot snapshot) {
        if (sourceId <= 0 || snapshot == null || safeGetCurrentContext() == 0L) {
            return false;
        }
        if (!ensureInitializedOnAudioThread()) {
            return false;
        }
        if (!AL10.alIsSource(sourceId)) {
            return false;
        }
        if (AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            return false;
        }
        boolean applied = applyToSource(sourceId, snapshot, false);
        if (applied) {
            DYNAMIC_UPDATES.incrementAndGet();
        }
        return applied;
    }

    private static long safeGetCurrentContext() {
        try {
            return ALC10.alcGetCurrentContext();
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    /**
     * Clears any EFX direct filter, auxiliary reverb send, and air absorption attached to a pooled channel
     * so subsequent non-spatial / non-EFX sounds never inherit stale occlusion or cavern reverb.
     */
    public static synchronized boolean resetChannel(Channel channel) {
        if (channel == null || !initialized || !efxSupported) {
            return false;
        }
        long ctx = safeGetCurrentContext();
        if (ctx == 0L || ctx != boundContext || !resolveChannelReflection()) {
            return false;
        }
        int sourceId = extractSourceId(channel);
        return resetSource(sourceId);
    }

    /**
     * Clears any EFX direct filter, auxiliary reverb send, and air absorption on a raw OpenAL source handle.
     */
    public static synchronized boolean resetSource(int sourceId) {
        if (sourceId <= 0 || !initialized || !efxSupported) {
            return false;
        }
        long ctx = safeGetCurrentContext();
        if (ctx == 0L || ctx != boundContext || !AL10.alIsSource(sourceId)) {
            return false;
        }
        try {
            AL10.alGetError();
            AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
            AL11.alSource3i(sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER,
                    EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
            AL10.alSourcef(sourceId, EXTEfx.AL_AIR_ABSORPTION_FACTOR, 0.0F);
            int err = AL10.alGetError();
            if (err == AL10.AL_NO_ERROR) {
                RESET_SOURCES.incrementAndGet();
                return true;
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Applies precomputed environment reverb, direct occlusion low-pass filter, air absorption,
     * and initial micro-prosody pitch/gain directly to a raw OpenAL source handle on the audio thread.
     */
    public static synchronized boolean applyToSource(int sourceId, SourceAcousticSnapshot snapshot) {
        return applyToSource(sourceId, snapshot, true);
    }

    private static boolean applyToSource(
            int sourceId,
            SourceAcousticSnapshot snapshot,
            boolean applyInitialHumanization
    ) {
        if (sourceId <= 0 || snapshot == null) {
            return false;
        }
        if (!ensureInitializedOnAudioThread()) {
            return false;
        }
        if (!AL10.alIsSource(sourceId)) {
            return false;
        }
        try {
            // Drain any unrelated prior AL error so it does not taint this source's EFX status check.
            AL10.alGetError();
            float safeTimbre = Float.isFinite(snapshot.timbreDamping())
                    ? Math.max(0.0F, Math.min(0.24F, snapshot.timbreDamping()))
                    : 0.0F;
            boolean humanizedApplied = false;
            if (applyInitialHumanization) {
                float pMul = Float.isFinite(snapshot.pitchMultiplier())
                        ? Math.max(0.75F, Math.min(1.25F, snapshot.pitchMultiplier()))
                        : 1.0F;
                float gMul = Float.isFinite(snapshot.gainMultiplier())
                        ? Math.max(0.65F, Math.min(1.15F, snapshot.gainMultiplier()))
                        : 1.0F;
                humanizedApplied = safeTimbre > 1e-4F;
                if (Math.abs(pMul - 1.0F) > 1e-4F) {
                    float basePitch = AL10.alGetSourcef(sourceId, AL10.AL_PITCH);
                    float safeBasePitch = (Float.isFinite(basePitch) && basePitch > 0.05F) ? basePitch : 1.0F;
                    AL10.alSourcef(sourceId, AL10.AL_PITCH, Math.max(0.5F, Math.min(2.0F, safeBasePitch * pMul)));
                    humanizedApplied = true;
                }
                if (Math.abs(gMul - 1.0F) > 1e-4F) {
                    float baseGain = AL10.alGetSourcef(sourceId, AL10.AL_GAIN);
                    float safeBaseGain = (Float.isFinite(baseGain) && baseGain > 0.0F) ? baseGain : 1.0F;
                    AL10.alSourcef(sourceId, AL10.AL_GAIN, Math.max(0.02F, Math.min(1.5F, safeBaseGain * gMul)));
                    humanizedApplied = true;
                }
            }

            float baseOcc = AcousticPhysicsEngine.computeEffectiveOcclusion(
                    snapshot.occlusionFactor(), snapshot.submersion());
            float distAirDamp = AcousticPhysicsEngine.computeBossDistanceAirDamping(
                    snapshot.distanceMeters(), snapshot.environment(), snapshot.submersion(), snapshot.bossCue());
            float rawSendBias = AcousticPhysicsEngine.computeDistanceSendBias(snapshot.distanceMeters());
            float nearSendBias = snapshot.bossCue() ? Math.min(0.15F, rawSendBias) : rawSendBias;
            float effectiveDirectOcc = Math.min(1.0F, baseOcc + safeTimbre + distAirDamp);
            float effectiveSendOcc = Math.min(1.0F, baseOcc + nearSendBias);
            int directBucket = Math.min(OCCLUSION_BUCKETS - 1, Math.max(0, Math.round(effectiveDirectOcc * (OCCLUSION_BUCKETS - 1))));
            int sendBucket = Math.min(OCCLUSION_BUCKETS - 1, Math.max(0, Math.round(effectiveSendOcc * (OCCLUSION_BUCKETS - 1))));

            // 1) Direct path material occlusion, submersion & per-hit HF timbre low-pass filter
            int directFilter = (directBucket > 0) ? DIRECT_LPF_POOL[directBucket] : EXTEfx.AL_FILTER_NULL;
            AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, directFilter);

            // 2) Auxiliary EAXReverb send (skipped for relative/UI or OPEN_AIR)
            AcousticEnvironment env = snapshot.environment() != null ? snapshot.environment() : AcousticEnvironment.OPEN_AIR;
            Integer auxSlot = (snapshot.applyReverb() && env != AcousticEnvironment.OPEN_AIR)
                    ? AUX_SLOTS.get(env)
                    : null;
            if (auxSlot != null && auxSlot > 0) {
                int sendFilter = SEND_LPF_POOL[sendBucket];
                AL11.alSource3i(sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER, auxSlot, 0, sendFilter);
            } else {
                AL11.alSource3i(sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
            }

            // 3) Distance-based high-frequency air absorption
            float airAbs = AcousticPhysicsEngine.computeAirAbsorptionFactor(
                    env, snapshot.submersion() == SubmersionState.WATER);
            AL10.alSourcef(sourceId, EXTEfx.AL_AIR_ABSORPTION_FACTOR, airAbs);

            int err = AL10.alGetError();
            if (err == AL10.AL_NO_ERROR) {
                APPLIED_SOURCES.incrementAndGet();
                if (humanizedApplied) {
                    HUMANIZED_SOURCES.incrementAndGet();
                }
                return true;
            }
            return false;
        } catch (Throwable t) {
            statusReason = "runtime_al_error:" + t.getClass().getSimpleName();
            efxSupported = false;
            initialized = false;
            return false;
        }
    }

    /**
     * Dynamically adjusts an active auxiliary EAXReverb slot gain on the audio thread
     * according to real-time room enclosure geometry and client reverb scaling.
     */
    public static synchronized boolean updateEnvironmentReverbGain(AcousticEnvironment env, float slotGain) {
        if (env == null || env == AcousticEnvironment.OPEN_AIR || safeGetCurrentContext() == 0L) {
            return false;
        }
        if (!ensureInitializedOnAudioThread()) {
            return false;
        }
        float clamped = Float.isFinite(slotGain) ? Math.max(0.0F, Math.min(1.0F, slotGain)) : env.wetSendGain();
        Integer slot = AUX_SLOTS.get(env);
        if (slot == null || slot <= 0 || !EXTEfx.alIsAuxiliaryEffectSlot(slot)) {
            return false;
        }
        Float prev = LAST_SLOT_GAINS.get(env);
        if (prev != null && Math.abs(prev - clamped) < 0.015F) {
            return true;
        }
        try {
            AL10.alGetError();
            EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, clamped);
            if (AL10.alGetError() == AL10.AL_NO_ERROR) {
                LAST_SLOT_GAINS.put(env, clamped);
                REVERB_GAIN_UPDATES.incrementAndGet();
                return true;
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Releases allocated EFX effect slots and filters on the active audio context and resets bridge state.
     */
    public static synchronized void shutdownOnAudioThread() {
        long ctx = 0L;
        try {
            ctx = ALC10.alcGetCurrentContext();
        } catch (Throwable ignored) {
            // OpenAL context may already be closed
        }
        if (initialized && ctx != 0L && ctx == boundContext) {
            cleanupHandlesOnCurrentContext();
        } else {
            resetHandleMaps();
        }
        boundContext = 0L;
        initialized = false;
        efxSupported = false;
        REINIT_REQUESTED.set(true);
        statusReason = "shutdown";
    }

    private static boolean isExternalAudioModLoaded() {
        try {
            ModList modList = ModList.get();
            return modList != null && modList.isLoaded("sound_physics_remastered");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean ensureInitializedOnAudioThread() {
        if (isExternalAudioModLoaded()) {
            externalModConflict = true;
            efxSupported = false;
            initialized = false;
            statusReason = "yielded_to_sound_physics_remastered";
            return false;
        }
        externalModConflict = false;

        long ctx;
        try {
            ctx = ALC10.alcGetCurrentContext();
        } catch (Throwable t) {
            efxSupported = false;
            initialized = false;
            statusReason = "openal_unavailable";
            return false;
        }
        if (ctx == 0L) {
            statusReason = "no_active_al_context";
            return false;
        }

        boolean contextChanged = (ctx != boundContext);
        if (initialized && !contextChanged && !REINIT_REQUESTED.get()) {
            return efxSupported;
        }

        if (!contextChanged && initialized) {
            cleanupHandlesOnCurrentContext();
        } else {
            resetHandleMaps();
        }

        REINIT_REQUESTED.set(false);
        boundContext = ctx;

        try {
            long device = ALC10.alcGetContextsDevice(ctx);
            if (device == 0L || !ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
                efxSupported = false;
                initialized = true;
                statusReason = "alc_ext_efx_unsupported";
                return false;
            }

            // Clear any pre-existing AL error before allocation
            AL10.alGetError();

            // Allocate 4 EAXReverb auxiliary slots (FOREST_CANOPY, CAVERN_SINKHOLE, SANCTUM_VAULT, DEEP_MYCELIUM_ABYSS)
            for (AcousticEnvironment env : AcousticEnvironment.values()) {
                if (env == AcousticEnvironment.OPEN_AIR) {
                    continue;
                }
                int effect = EXTEfx.alGenEffects();
                if (effect <= 0) {
                    continue;
                }
                boolean configured = configureReverbEffect(effect, env);
                if (!configured) {
                    EXTEfx.alDeleteEffects(effect);
                    continue;
                }
                int slot = EXTEfx.alGenAuxiliaryEffectSlots();
                if (slot <= 0) {
                    EXTEfx.alDeleteEffects(effect);
                    continue;
                }
                EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, effect);
                EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, env.wetSendGain());
                EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);
                REVERB_EFFECTS.put(env, effect);
                AUX_SLOTS.put(env, slot);
                LAST_SLOT_GAINS.put(env, env.wetSendGain());
            }

            // Allocate quantized direct & reverb-send low-pass filters (16 buckets: 0.0 -> 1.0 occlusion)
            for (int i = 0; i < OCCLUSION_BUCKETS; i++) {
                float occ = i / (float) (OCCLUSION_BUCKETS - 1);
                int directFilter = EXTEfx.alGenFilters();
                if (directFilter > 0) {
                    EXTEfx.alFilteri(directFilter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                    EXTEfx.alFilterf(directFilter, EXTEfx.AL_LOWPASS_GAIN,
                            AcousticPhysicsEngine.computeDirectLowPassGain(occ));
                    EXTEfx.alFilterf(directFilter, EXTEfx.AL_LOWPASS_GAINHF,
                            AcousticPhysicsEngine.computeDirectLowPassGainHf(occ));
                    DIRECT_LPF_POOL[i] = directFilter;
                }
                int sendFilter = EXTEfx.alGenFilters();
                if (sendFilter > 0) {
                    EXTEfx.alFilteri(sendFilter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                    EXTEfx.alFilterf(sendFilter, EXTEfx.AL_LOWPASS_GAIN, 1.0F);
                    EXTEfx.alFilterf(sendFilter, EXTEfx.AL_LOWPASS_GAINHF,
                            AcousticPhysicsEngine.computeSendLowPassGainHf(occ));
                    SEND_LPF_POOL[i] = sendFilter;
                }
            }

            int err = AL10.alGetError();
            if (AUX_SLOTS.isEmpty()) {
                cleanupHandlesOnCurrentContext();
                efxSupported = false;
                initialized = true;
                statusReason = "efx_slot_allocation_failed_al_" + err;
                return false;
            }

            efxSupported = true;
            initialized = true;
            statusReason = "active_eaxreverb_slots_" + AUX_SLOTS.size();
            MycelialMind.LOGGER.info("OpenAL EFX acoustic engine initialized ({}, {} LPF buckets).",
                    statusReason, OCCLUSION_BUCKETS);
            return true;
        } catch (Throwable t) {
            resetHandleMaps();
            efxSupported = false;
            initialized = true;
            statusReason = "init_error:" + t.getClass().getSimpleName();
            MycelialMind.LOGGER.warn("OpenAL EFX acoustic engine disabled due to error: {}", t.toString());
            return false;
        }
    }

    private static boolean configureReverbEffect(int effect, AcousticEnvironment env) {
        AL10.alGetError();
        EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        if (AL10.alGetError() == AL10.AL_NO_ERROR) {
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DENSITY, env.density());
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DIFFUSION, env.diffusion());
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAIN, 0.36F);
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAINHF, env.gainHf());
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_TIME, env.decayTime());
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, env.decayHfRatio());
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, env.reflectionsGain());
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, env.lateReverbGain());
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, 0.992F);
            return AL10.alGetError() == AL10.AL_NO_ERROR;
        }
        // Fallback to standard OpenAL Reverb effect if EAXReverb is unsupported
        EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
        if (AL10.alGetError() != AL10.AL_NO_ERROR) {
            return false;
        }
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DENSITY, env.density());
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DIFFUSION, env.diffusion());
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_GAIN, 0.36F);
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_GAINHF, env.gainHf());
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DECAY_TIME, env.decayTime());
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DECAY_HFRATIO, env.decayHfRatio());
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, env.reflectionsGain());
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, env.lateReverbGain());
        return AL10.alGetError() == AL10.AL_NO_ERROR;
    }

    private static boolean resolveChannelReflection() {
        if (reflectionResolved) {
            return channelSourceField != null;
        }
        reflectionResolved = true;
        try {
            Field f = Channel.class.getDeclaredField("source");
            f.setAccessible(true);
            channelSourceField = f;
            return true;
        } catch (Throwable ignored) {
            for (Field f : Channel.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    try {
                        f.setAccessible(true);
                        channelSourceField = f;
                        return true;
                    } catch (Throwable ignoredInner) {
                        // continue searching
                    }
                }
            }
        }
        return false;
    }

    private static int extractSourceId(Channel channel) {
        if (channel instanceof ChannelAccessor accessor) {
            return accessor.mycelialmind$getSource();
        }
        if (!reflectionResolved) {
            resolveChannelReflection();
        }
        if (channelSourceField == null) {
            return 0;
        }
        try {
            return channelSourceField.getInt(channel);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static void cleanupHandlesOnCurrentContext() {
        try {
            for (int slot : AUX_SLOTS.values()) {
                if (slot > 0 && EXTEfx.alIsAuxiliaryEffectSlot(slot)) {
                    EXTEfx.alDeleteAuxiliaryEffectSlots(slot);
                }
            }
            for (int eff : REVERB_EFFECTS.values()) {
                if (eff > 0 && EXTEfx.alIsEffect(eff)) {
                    EXTEfx.alDeleteEffects(eff);
                }
            }
            for (int f : DIRECT_LPF_POOL) {
                if (f > 0 && EXTEfx.alIsFilter(f)) {
                    EXTEfx.alDeleteFilters(f);
                }
            }
            for (int f : SEND_LPF_POOL) {
                if (f > 0 && EXTEfx.alIsFilter(f)) {
                    EXTEfx.alDeleteFilters(f);
                }
            }
        } catch (Throwable ignored) {
            // Context may already have been destroyed during reload
        }
        resetHandleMaps();
    }

    private static void resetHandleMaps() {
        AUX_SLOTS.clear();
        REVERB_EFFECTS.clear();
        LAST_SLOT_GAINS.clear();
        for (int i = 0; i < OCCLUSION_BUCKETS; i++) {
            DIRECT_LPF_POOL[i] = 0;
            SEND_LPF_POOL[i] = 0;
        }
    }

    public static boolean isEfxSupported() {
        return efxSupported;
    }

    public static boolean isExternalModConflict() {
        return externalModConflict;
    }

    public static String getStatusReason() {
        return statusReason;
    }

    public static long getAppliedSourcesCount() {
        return APPLIED_SOURCES.get();
    }

    public static long getDynamicUpdatesCount() {
        return DYNAMIC_UPDATES.get();
    }

    public static long getHumanizedSourcesCount() {
        return HUMANIZED_SOURCES.get();
    }

    public static long getResetSourcesCount() {
        return RESET_SOURCES.get();
    }

    public static long getReverbGainUpdatesCount() {
        return REVERB_GAIN_UPDATES.get();
    }

    public static int getActiveSlotCount() {
        return AUX_SLOTS.size();
    }

    public static int getOcclusionBucketCount() {
        return OCCLUSION_BUCKETS;
    }
}
