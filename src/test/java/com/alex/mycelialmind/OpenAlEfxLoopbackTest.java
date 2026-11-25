package com.alex.mycelialmind;

import com.alex.mycelialmind.client.audio.OpenAlEfxBridge;
import com.alex.mycelialmind.client.audio.OpenAlEfxBridge.SourceAcousticSnapshot;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.AcousticEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.openal.SOFTLoopback;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless OpenAL Soft ({@code ALC_SOFT_loopback}) integration test that drives {@link OpenAlEfxBridge}
 * directly against a real native OpenAL EFX pipeline and verifies acoustic responses via FFT.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class OpenAlEfxLoopbackTest {

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = 2;
    private static final int FFT_SIZE = 4096;

    private long device;
    private long context;

    private record SpectralMeasurement(double lowPower, double highPower) {
    }

    private record TailMeasurement(double earlyRms, double lateRms, double earlyFftPower) {
    }

    @BeforeEach
    void setUpLoopback() {
        device = SOFTLoopback.alcLoopbackOpenDeviceSOFT((ByteBuffer) null);
        assertNotEquals(0L, device, "OpenAL Soft loopback device should open");

        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        assertTrue(alcCaps.ALC_SOFT_loopback, "ALC_SOFT_loopback extension must be supported");
        assertTrue(ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX"), "ALC_EXT_EFX extension must be supported");
        assertTrue(
                SOFTLoopback.alcIsRenderFormatSupportedSOFT(
                        device,
                        SAMPLE_RATE,
                        SOFTLoopback.ALC_STEREO_SOFT,
                        SOFTLoopback.ALC_FLOAT_SOFT
                ),
                "Stereo Float32 44100 Hz loopback render format must be supported"
        );

        IntBuffer attrs = BufferUtils.createIntBuffer(9);
        attrs.put(SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT).put(SOFTLoopback.ALC_STEREO_SOFT);
        attrs.put(SOFTLoopback.ALC_FORMAT_TYPE_SOFT).put(SOFTLoopback.ALC_FLOAT_SOFT);
        attrs.put(ALC10.ALC_FREQUENCY).put(SAMPLE_RATE);
        attrs.put(EXTEfx.ALC_MAX_AUXILIARY_SENDS).put(2);
        attrs.put(0);
        attrs.flip();

        context = ALC10.alcCreateContext(device, attrs);
        assertNotEquals(0L, context, "OpenAL Soft loopback context should be created");
        assertTrue(ALC10.alcMakeContextCurrent(context), "Loopback context must become current");

        ALCapabilities alCaps = AL.createCapabilities(alcCaps);
        assertTrue(alCaps.OpenAL11, "OpenAL 1.1 capabilities must be initialized");

        AL10.alListener3f(AL10.AL_POSITION, 0.0F, 0.0F, 0.0F);
        AL10.alListener3f(AL10.AL_VELOCITY, 0.0F, 0.0F, 0.0F);
        FloatBuffer orientation = BufferUtils.createFloatBuffer(6);
        orientation.put(new float[]{0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.0F}).flip();
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
        AL10.alListenerf(AL10.AL_GAIN, 1.0F);
        AL10.alGetError();

        OpenAlEfxBridge.markNeedsReinit();
    }

    @AfterEach
    void tearDownLoopback() {
        try {
            OpenAlEfxBridge.shutdownOnAudioThread();
        } finally {
            ALC10.alcMakeContextCurrent(0L);
            if (context != 0L) {
                ALC10.alcDestroyContext(context);
                context = 0L;
            }
            if (device != 0L) {
                ALC10.alcCloseDevice(device);
                device = 0L;
            }
        }
    }

    @Test
    void occlusionFactor08AttenuatesHighFrequenciesAbove2kHz() {
        short[] multiTonePcm = synthesizeMultiTonePcm(
                SAMPLE_RATE / 2,
                new double[]{300.0D, 600.0D, 3500.0D, 5500.0D, 8000.0D},
                0.18D
        );
        int bufferId = createMono16Buffer(multiTonePcm);
        try {
            SpectralMeasurement unoccluded = measurePlaybackSpectrum(
                    bufferId,
                    new SourceAcousticSnapshot(AcousticEnvironment.OPEN_AIR, 0.0F, false, false, 0L)
            );
            SpectralMeasurement occluded = measurePlaybackSpectrum(
                    bufferId,
                    new SourceAcousticSnapshot(AcousticEnvironment.OPEN_AIR, 0.8F, false, false, 0L)
            );

            assertTrue(unoccluded.lowPower() > 1e-4D, "Unoccluded low-frequency power must be present");
            assertTrue(unoccluded.highPower() > 1e-4D, "Unoccluded >2 kHz power must be present");
            assertTrue(occluded.lowPower() > 1e-6D, "Occluded low-frequency power must remain measurable");

            double hfDropDb = 10.0D * Math.log10(unoccluded.highPower() / Math.max(1e-18D, occluded.highPower()));
            double unoccludedRatio = unoccluded.highPower() / unoccluded.lowPower();
            double occludedRatio = occluded.highPower() / occluded.lowPower();
            double tiltDropDb = 10.0D * Math.log10(unoccludedRatio / Math.max(1e-18D, occludedRatio));

            assertTrue(
                    hfDropDb >= 10.0D,
                    () -> String.format("Expected >2 kHz drop >= 10 dB at occlusion 0.8, got %.2f dB", hfDropDb)
            );
            assertTrue(
                    tiltDropDb >= 6.0D,
                    () -> String.format("Expected HF/LF spectral tilt drop >= 6 dB (low-pass selectivity), got %.2f dB", tiltDropDb)
            );
        } finally {
            AL10.alDeleteBuffers(bufferId);
        }
    }

    @Test
    void deepMyceliumAbyssProducesDecayingReverbTailAfterSourceCutoff() {
        short[] burstPcm = synthesizeMultiTonePcm(
                SAMPLE_RATE / 4,
                new double[]{440.0D, 1200.0D, 2600.0D},
                0.25D
        );
        int bufferId = createMono16Buffer(burstPcm);
        try {
            // Run OPEN_AIR dry baseline first so the shared EFX aux reverb slots start cold.
            TailMeasurement dry = measurePostCutoffTail(bufferId, AcousticEnvironment.OPEN_AIR, false);
            TailMeasurement abyss = measurePostCutoffTail(bufferId, AcousticEnvironment.DEEP_MYCELIUM_ABYSS, true);

            assertTrue(
                    dry.earlyRms() < 1e-4D,
                    () -> String.format("Dry OPEN_AIR post-cutoff tail should be silent, got RMS=%.6f", dry.earlyRms())
            );
            assertTrue(
                    dry.earlyFftPower() < 1e-7D,
                    () -> String.format("Dry OPEN_AIR post-cutoff FFT power should be ~0, got %.9f", dry.earlyFftPower())
            );

            assertTrue(
                    abyss.earlyRms() > 5e-3D,
                    () -> String.format("Expected audible DEEP_MYCELIUM_ABYSS reverb tail RMS > 5e-3, got %.6f", abyss.earlyRms())
            );
            assertTrue(
                    abyss.earlyRms() > dry.earlyRms() * 50.0D,
                    () -> String.format("Reverb tail RMS (%.6f) should vastly exceed dry floor (%.6f)", abyss.earlyRms(), dry.earlyRms())
            );
            assertTrue(
                    abyss.earlyFftPower() > 1e-5D,
                    () -> String.format("Expected measurable FFT energy in reverb tail, got %.8f", abyss.earlyFftPower())
            );
            assertTrue(
                    abyss.lateRms() > 1e-3D && abyss.lateRms() < abyss.earlyRms(),
                    () -> String.format("Expected decaying reverb tail (early=%.6f, late=%.6f)", abyss.earlyRms(), abyss.lateRms())
            );
        } finally {
            AL10.alDeleteBuffers(bufferId);
        }
    }

    @Test
    void combinedAbyssReverbAndOcclusionApplySimultaneouslyWithoutAlErrors() {
        short[] signalPcm = synthesizeMultiTonePcm(
                SAMPLE_RATE / 2,
                new double[]{350.0D, 700.0D, 3500.0D, 6500.0D},
                0.20D
        );
        int bufferId = createMono16Buffer(signalPcm);
        try {
            SpectralMeasurement clearAbyss = measurePlaybackSpectrum(
                    bufferId,
                    new SourceAcousticSnapshot(AcousticEnvironment.DEEP_MYCELIUM_ABYSS, 0.0F, false, true, 0L)
            );
            SpectralMeasurement occludedAbyss = measurePlaybackSpectrum(
                    bufferId,
                    new SourceAcousticSnapshot(AcousticEnvironment.DEEP_MYCELIUM_ABYSS, 0.8F, false, true, 0L)
            );

            double hfDropDb = 10.0D * Math.log10(clearAbyss.highPower() / Math.max(1e-18D, occludedAbyss.highPower()));
            assertTrue(
                    hfDropDb >= 8.0D,
                    () -> String.format("Expected >2 kHz attenuation >= 8 dB with occlusion + abyss reverb, got %.2f dB", hfDropDb)
            );

            int sourceId = createUnitSource(bufferId);
            try {
                assertTrue(
                        OpenAlEfxBridge.applyToSource(
                                sourceId,
                                new SourceAcousticSnapshot(AcousticEnvironment.DEEP_MYCELIUM_ABYSS, 0.8F, false, true, 0L)
                        ),
                        "OpenAlEfxBridge.applyToSource should accept combined reverb + occlusion"
                );
                assertEquals(AL10.AL_NO_ERROR, AL10.alGetError(), "Combined EFX application must leave AL_NO_ERROR");

                AL10.alSourcePlay(sourceId);
                renderStereoFrames(6615);
                AL10.alSourceStop(sourceId);
                assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE));

                renderStereoFrames(1323);
                float[] tail = renderStereoFrames(16384);
                double tailRms = rms(tail);
                assertTrue(
                        tailRms > 2e-3D,
                        () -> String.format("Expected surviving reverb tail RMS > 2e-3 when occluded, got %.6f", tailRms)
                );
                assertEquals(AL10.AL_NO_ERROR, AL10.alGetError(), "Loopback rendering must leave AL_NO_ERROR");
            } finally {
                AL10.alSourceStop(sourceId);
                AL10.alDeleteSources(sourceId);
            }
        } finally {
            AL10.alDeleteBuffers(bufferId);
        }
    }

    @Test
    void dynamicOcclusionTransitionOnActiveSourceUpdatesSpectrumLive() {
        short[] pcm = synthesizeMultiTonePcm(
                SAMPLE_RATE * 2,
                new double[]{220.0D, 440.0D, 3500.0D, 6200.0D},
                0.22D
        );
        int bufferId = createMono16Buffer(pcm);
        int sourceId = createUnitSource(bufferId);
        try {
            assertTrue(
                    OpenAlEfxBridge.applyToSource(
                            sourceId,
                            new SourceAcousticSnapshot(
                                    AcousticEnvironment.OPEN_AIR,
                                    0.0F,
                                    AcousticPhysicsEngine.SubmersionState.NONE,
                                    false,
                                    0L
                            )
                    ),
                    "Initial applyToSource should succeed"
            );
            AL10.alSourcePlay(sourceId);
            renderStereoFrames(2048);

            float[] clearRendered = renderStereoFrames(FFT_SIZE);
            double[] clearSpec = computeStereoPowerSpectrum(clearRendered, 0, FFT_SIZE);
            double clearHighPower = bandPower(clearSpec, 2000.0D, 16000.0D);
            assertEquals(AL10.AL_PLAYING, AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE));

            long updatesBefore = OpenAlEfxBridge.getDynamicUpdatesCount();
            assertTrue(
                    OpenAlEfxBridge.updateActiveSource(
                            sourceId,
                            new SourceAcousticSnapshot(
                                    AcousticEnvironment.OPEN_AIR,
                                    0.85F,
                                    AcousticPhysicsEngine.SubmersionState.NONE,
                                    false,
                                    1L
                            )
                    ),
                    "Live updateActiveSource must succeed on actively playing source"
            );
            assertTrue(OpenAlEfxBridge.getDynamicUpdatesCount() > updatesBefore);
            assertEquals(AL10.AL_NO_ERROR, AL10.alGetError(), "Live EFX update must leave AL_NO_ERROR");

            renderStereoFrames(2048);
            float[] occRendered = renderStereoFrames(FFT_SIZE);
            double[] occSpec = computeStereoPowerSpectrum(occRendered, 0, FFT_SIZE);
            double occHighPower = bandPower(occSpec, 2000.0D, 16000.0D);

            assertEquals(AL10.AL_PLAYING, AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE));
            assertTrue(clearHighPower > 1e-6D, "Clear spectrum must preserve >2 kHz energy");
            double dropDb = 10.0D * Math.log10(clearHighPower / Math.max(occHighPower, 1e-12D));
            assertTrue(dropDb >= 8.0D, "Live transition to 0.85 occlusion must attenuate >2 kHz by >= 8 dB, got " + dropDb);
        } finally {
            AL10.alSourceStop(sourceId);
            AL10.alDeleteSources(sourceId);
            AL10.alDeleteBuffers(bufferId);
        }
    }

    @Test
    void waterAndSporeFogSubmersionApplyProgressiveLowPassDamping() {
        short[] pcm = synthesizeMultiTonePcm(
                SAMPLE_RATE,
                new double[]{220.0D, 440.0D, 3200.0D, 6500.0D},
                0.22D
        );
        int bufferId = createMono16Buffer(pcm);
        try {
            SpectralMeasurement clear = measurePlaybackSpectrum(
                    bufferId,
                    new SourceAcousticSnapshot(AcousticEnvironment.OPEN_AIR, 0.0F, AcousticPhysicsEngine.SubmersionState.NONE, false, 0L)
            );
            SpectralMeasurement sporeFog = measurePlaybackSpectrum(
                    bufferId,
                    new SourceAcousticSnapshot(AcousticEnvironment.OPEN_AIR, 0.0F, AcousticPhysicsEngine.SubmersionState.SPORE_FOG, false, 0L)
            );
            SpectralMeasurement water = measurePlaybackSpectrum(
                    bufferId,
                    new SourceAcousticSnapshot(AcousticEnvironment.OPEN_AIR, 0.0F, AcousticPhysicsEngine.SubmersionState.WATER, false, 0L)
            );

            assertTrue(clear.highPower() > sporeFog.highPower() * 2.0D,
                    "SPORE_FOG must noticeably damp >2 kHz energy vs clear air");
            assertTrue(sporeFog.highPower() > water.highPower() * 2.5D,
                    "WATER must heavily damp >2 kHz energy vs SPORE_FOG");

            double clearRatio = clear.highPower() / Math.max(clear.lowPower(), 1e-12D);
            double fogRatio = sporeFog.highPower() / Math.max(sporeFog.lowPower(), 1e-12D);
            double waterRatio = water.highPower() / Math.max(water.lowPower(), 1e-12D);
            assertTrue(clearRatio > fogRatio && fogRatio > waterRatio,
                    "Expected strictly progressive HF/LF tilt reduction NONE > SPORE_FOG > WATER");
        } finally {
            AL10.alDeleteBuffers(bufferId);
        }
    }

    @Test
    void reverbTailDecayOrdersAcrossEnvironmentsFromCanopyToAbyss() {
        short[] impulseBurst = synthesizeMultiTonePcm(
                8820,
                new double[]{280.0D, 640.0D, 1320.0D},
                0.28D
        );
        int bufferId = createMono16Buffer(impulseBurst);
        try {
            TailMeasurement canopy = measurePostCutoffTail(bufferId, AcousticEnvironment.FOREST_CANOPY, true);
            renderStereoFrames(8192);
            TailMeasurement cavern = measurePostCutoffTail(bufferId, AcousticEnvironment.CAVERN_SINKHOLE, true);
            renderStereoFrames(8192);
            TailMeasurement abyss = measurePostCutoffTail(bufferId, AcousticEnvironment.DEEP_MYCELIUM_ABYSS, true);

            assertTrue(canopy.earlyRms() > 1e-4D,
                    "FOREST_CANOPY must produce audible early reverb tail, got " + canopy.earlyRms());
            assertTrue(canopy.earlyRms() < cavern.earlyRms() && cavern.earlyRms() < abyss.earlyRms(),
                    "Expected early reverb RMS ordering CANOPY < CAVERN < ABYSS, got canopy="
                            + canopy.earlyRms() + " cavern=" + cavern.earlyRms() + " abyss=" + abyss.earlyRms());
            assertTrue(canopy.earlyFftPower() < cavern.earlyFftPower() && cavern.earlyFftPower() < abyss.earlyFftPower(),
                    "Expected early FFT power ordering CANOPY < CAVERN < ABYSS");
            assertTrue(canopy.lateRms() < abyss.lateRms() && cavern.lateRms() < abyss.lateRms(),
                    "Expected late tail energy to sustain longer in DEEP_MYCELIUM_ABYSS, got canopy="
                            + canopy.lateRms() + " cavern=" + cavern.lateRms() + " abyss=" + abyss.lateRms());
        } finally {
            AL10.alDeleteBuffers(bufferId);
        }
    }

    private SpectralMeasurement measurePlaybackSpectrum(int bufferId, SourceAcousticSnapshot snapshot) {
        int sourceId = createUnitSource(bufferId);
        try {
            assertTrue(OpenAlEfxBridge.applyToSource(sourceId, snapshot), "applyToSource should succeed");
            assertTrue(OpenAlEfxBridge.isEfxSupported(), "EFX must be active on loopback device");
            assertEquals(4, OpenAlEfxBridge.getActiveSlotCount(), "Expected 4 preallocated EAXReverb slots");
            assertEquals(AL10.AL_NO_ERROR, AL10.alGetError(), "applyToSource must produce AL_NO_ERROR");

            AL10.alSourcePlay(sourceId);
            renderStereoFrames(2048); // Skip attack / filter settling frames
            float[] rendered = renderStereoFrames(FFT_SIZE);

            double[] spectrum = computeStereoPowerSpectrum(rendered, 0, FFT_SIZE);
            double lowPower = bandPower(spectrum, 150.0D, 1000.0D);
            double highPower = bandPower(spectrum, 2000.0D, 16000.0D);
            return new SpectralMeasurement(lowPower, highPower);
        } finally {
            AL10.alSourceStop(sourceId);
            AL10.alDeleteSources(sourceId);
        }
    }

    private TailMeasurement measurePostCutoffTail(int bufferId, AcousticEnvironment env, boolean applyReverb) {
        int sourceId = createUnitSource(bufferId);
        try {
            assertTrue(
                    OpenAlEfxBridge.applyToSource(
                            sourceId,
                            new SourceAcousticSnapshot(env, 0.0F, false, applyReverb, 0L)
                    ),
                    "applyToSource should succeed for " + env
            );
            assertEquals(AL10.AL_NO_ERROR, AL10.alGetError(), "applyToSource must leave AL_NO_ERROR");

            AL10.alSourcePlay(sourceId);
            renderStereoFrames(6615); // 150 ms playback to excite reverb feedback tank

            AL10.alSourceStop(sourceId);
            assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE));

            renderStereoFrames(1323); // 30 ms settling window after source stop
            float[] earlyTail = renderStereoFrames(16384); // ~371 ms early reverb tail
            float[] lateTail = renderStereoFrames(16384);  // ~371 ms late reverb tail

            double[] earlySpec = computeStereoPowerSpectrum(earlyTail, 0, FFT_SIZE);
            double earlyFftPower = bandPower(earlySpec, 100.0D, 12000.0D);
            return new TailMeasurement(rms(earlyTail), rms(lateTail), earlyFftPower);
        } finally {
            AL10.alSourceStop(sourceId);
            AL10.alDeleteSources(sourceId);
        }
    }

    private int createMono16Buffer(short[] pcm) {
        int bufferId = AL10.alGenBuffers();
        ShortBuffer data = BufferUtils.createShortBuffer(pcm.length);
        data.put(pcm).flip();
        AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, data, SAMPLE_RATE);
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError(), "Buffer upload must succeed");
        return bufferId;
    }

    private int createUnitSource(int bufferId) {
        int sourceId = AL10.alGenSources();
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
        AL10.alSource3f(sourceId, AL10.AL_POSITION, 0.0F, 0.0F, -1.0F);
        AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, 1.0F);
        AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, 0.0F);
        AL10.alSourcef(sourceId, AL10.AL_GAIN, 1.0F);
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError(), "Source creation must succeed");
        return sourceId;
    }

    private float[] renderStereoFrames(int frames) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(frames * CHANNELS);
        SOFTLoopback.alcRenderSamplesSOFT(device, buffer, frames);
        float[] samples = new float[frames * CHANNELS];
        buffer.get(samples);
        return samples;
    }

    private static short[] synthesizeMultiTonePcm(int sampleCount, double[] frequenciesHz, double amplitudePerTone) {
        short[] pcm = new short[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double t = i / (double) SAMPLE_RATE;
            double sample = 0.0D;
            for (double freq : frequenciesHz) {
                sample += amplitudePerTone * Math.sin(2.0D * Math.PI * freq * t);
            }
            double clamped = Math.max(-0.98D, Math.min(0.98D, sample));
            pcm[i] = (short) Math.round(clamped * 32767.0D);
        }
        return pcm;
    }

    private static double[] computeStereoPowerSpectrum(float[] interleavedStereo, int offsetFrames, int fftSize) {
        double[] leftRe = new double[fftSize];
        double[] leftIm = new double[fftSize];
        double[] rightRe = new double[fftSize];
        double[] rightIm = new double[fftSize];

        for (int n = 0; n < fftSize; n++) {
            double hann = 0.5D * (1.0D - Math.cos((2.0D * Math.PI * n) / (fftSize - 1)));
            int base = (offsetFrames + n) * CHANNELS;
            leftRe[n] = interleavedStereo[base] * hann;
            rightRe[n] = interleavedStereo[base + 1] * hann;
        }

        fftRadix2(leftRe, leftIm);
        fftRadix2(rightRe, rightIm);

        int half = fftSize / 2;
        double norm = (double) fftSize * fftSize;
        double[] power = new double[half + 1];
        for (int k = 0; k <= half; k++) {
            double leftMag2 = leftRe[k] * leftRe[k] + leftIm[k] * leftIm[k];
            double rightMag2 = rightRe[k] * rightRe[k] + rightIm[k] * rightIm[k];
            power[k] = 0.5D * (leftMag2 + rightMag2) / norm;
        }
        return power;
    }

    private static void fftRadix2(double[] re, double[] im) {
        int n = re.length;
        if (Integer.bitCount(n) != 1) {
            throw new IllegalArgumentException("FFT length must be a power of 2, got " + n);
        }

        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            while ((j & bit) != 0) {
                j ^= bit;
                bit >>= 1;
            }
            j ^= bit;
            if (i < j) {
                double tmpRe = re[i];
                re[i] = re[j];
                re[j] = tmpRe;
                double tmpIm = im[i];
                im[i] = im[j];
                im[j] = tmpIm;
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2.0D * Math.PI / len;
            double wLenRe = Math.cos(angle);
            double wLenIm = Math.sin(angle);
            int halfLen = len >> 1;

            for (int i = 0; i < n; i += len) {
                double wRe = 1.0D;
                double wIm = 0.0D;
                for (int j = 0; j < halfLen; j++) {
                    int even = i + j;
                    int odd = even + halfLen;
                    double oddRe = re[odd] * wRe - im[odd] * wIm;
                    double oddIm = re[odd] * wIm + im[odd] * wRe;

                    re[odd] = re[even] - oddRe;
                    im[odd] = im[even] - oddIm;
                    re[even] += oddRe;
                    im[even] += oddIm;

                    double nextWRe = wRe * wLenRe - wIm * wLenIm;
                    wIm = wRe * wLenIm + wIm * wLenRe;
                    wRe = nextWRe;
                }
            }
        }
    }

    private static double bandPower(double[] powerSpectrum, double minHz, double maxHz) {
        int fftSize = (powerSpectrum.length - 1) * 2;
        double sum = 0.0D;
        for (int k = 0; k < powerSpectrum.length; k++) {
            double freq = (k * (double) SAMPLE_RATE) / fftSize;
            if (freq >= minHz && freq <= maxHz) {
                sum += powerSpectrum[k];
            }
        }
        return sum;
    }

    private static double rms(float[] samples) {
        if (samples.length == 0) {
            return 0.0D;
        }
        double sumSq = 0.0D;
        for (float s : samples) {
            sumSq += (double) s * s;
        }
        return Math.sqrt(sumSq / samples.length);
    }
}
