package com.alex.mycelialmind;

import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.AcousticEnvironment;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.MaterialAbsorption;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.SwarmVoiceBudget;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.VoicePriority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JUnit 5 verification for OpenAL EFX acoustic physics, material raycast occlusion curves,
 * environment classification, swarm anti-cacophony voice budget, and client audio v4 config defaults.
 */
public class AcousticPhysicsEngineTest {

    @Test
    void materialAbsorptionOrderingAndOcclusionCurveAreMonotonicAndBounded() {
        assertEquals(0.0F, MaterialAbsorption.AIR.absorptionPerBlock(), 1e-6F);
        assertTrue(MaterialAbsorption.FOLIAGE_GLASS_WATER.absorptionPerBlock()
                < MaterialAbsorption.MYCELIUM_SOIL_MOSS.absorptionPerBlock());
        assertTrue(MaterialAbsorption.MYCELIUM_SOIL_MOSS.absorptionPerBlock()
                < MaterialAbsorption.CHITIN_WOOD_BONE.absorptionPerBlock());
        assertTrue(MaterialAbsorption.CHITIN_WOOD_BONE.absorptionPerBlock()
                < MaterialAbsorption.STONE_DEEPSLATE_METAL.absorptionPerBlock());
        assertTrue(MaterialAbsorption.STONE_DEEPSLATE_METAL.absorptionPerBlock()
                < MaterialAbsorption.DENSE_BARRIER_OBSIDIAN.absorptionPerBlock());

        assertEquals(0.0F, AcousticPhysicsEngine.computeOcclusionFactor(0.0F), 1e-6F);
        assertEquals(0.0F, AcousticPhysicsEngine.computeOcclusionFactor(-1.5F), 1e-6F);
        assertEquals(0.0F, AcousticPhysicsEngine.computeOcclusionFactor(Float.NaN), 1e-6F);

        float occLeaves = AcousticPhysicsEngine.computeOcclusionFactor(MaterialAbsorption.FOLIAGE_GLASS_WATER.absorptionPerBlock());
        float occStone1 = AcousticPhysicsEngine.computeOcclusionFactor(MaterialAbsorption.STONE_DEEPSLATE_METAL.absorptionPerBlock());
        float occStone2 = AcousticPhysicsEngine.computeOcclusionFactor(MaterialAbsorption.STONE_DEEPSLATE_METAL.absorptionPerBlock() * 2.0F);
        float occDense = AcousticPhysicsEngine.computeOcclusionFactor(MaterialAbsorption.DENSE_BARRIER_OBSIDIAN.absorptionPerBlock() * 3.0F);

        assertTrue(occLeaves > 0.05F && occLeaves < occStone1);
        assertTrue(occStone1 < occStone2 && occStone2 < occDense && occDense <= 1.0F);

        // Direct low-pass broadband & HF gains decrease smoothly as occlusion rises
        assertEquals(1.0F, AcousticPhysicsEngine.computeDirectLowPassGain(0.0F), 1e-5F);
        assertEquals(1.0F, AcousticPhysicsEngine.computeDirectLowPassGainHf(0.0F), 1e-5F);
        assertTrue(AcousticPhysicsEngine.computeDirectLowPassGain(occStone1)
                > AcousticPhysicsEngine.computeDirectLowPassGain(occDense));
        assertTrue(AcousticPhysicsEngine.computeDirectLowPassGainHf(occStone1)
                > AcousticPhysicsEngine.computeDirectLowPassGainHf(occDense));
        assertTrue(AcousticPhysicsEngine.computeDirectLowPassGainHf(1.0F) >= 0.06F);
        assertTrue(AcousticPhysicsEngine.computeSendLowPassGainHf(1.0F) >= 0.18F);
    }

    @Test
    void environmentClassificationAndAirAbsorptionRespectBiomeAndGeometry() {
        assertEquals(AcousticEnvironment.OPEN_AIR,
                AcousticPhysicsEngine.classifyEnvironment(false, false, false, true, 0.0F, 14.0F));
        assertEquals(AcousticEnvironment.FOREST_CANOPY,
                AcousticPhysicsEngine.classifyEnvironment(false, false, false, true, 0.35F, 8.0F));
        assertEquals(AcousticEnvironment.CAVERN_SINKHOLE,
                AcousticPhysicsEngine.classifyEnvironment(false, true, false, true, 0.20F, 10.0F));
        assertEquals(AcousticEnvironment.CAVERN_SINKHOLE,
                AcousticPhysicsEngine.classifyEnvironment(false, false, false, false, 0.65F, 6.0F));
        assertEquals(AcousticEnvironment.SANCTUM_VAULT,
                AcousticPhysicsEngine.classifyEnvironment(false, false, false, false, 0.70F, 11.5F));
        assertEquals(AcousticEnvironment.SANCTUM_VAULT,
                AcousticPhysicsEngine.classifyEnvironment(true, false, true, false, 0.50F, 8.0F));
        assertEquals(AcousticEnvironment.DEEP_MYCELIUM_ABYSS,
                AcousticPhysicsEngine.classifyEnvironment(true, false, false, false, 0.20F, 12.0F));

        assertTrue(AcousticPhysicsEngine.computeAirAbsorptionFactor(AcousticEnvironment.OPEN_AIR, false)
                < AcousticPhysicsEngine.computeAirAbsorptionFactor(AcousticEnvironment.DEEP_MYCELIUM_ABYSS, false));
        assertEquals(6.0F, AcousticPhysicsEngine.computeAirAbsorptionFactor(AcousticEnvironment.OPEN_AIR, true), 1e-5F);
    }

    @Test
    void efxPolicyAppliesToModAlwaysAndExtendsToVanillaInsideMycelialZonesOnly() {
        assertFalse(AcousticPhysicsEngine.shouldApplyEfx("mycelialmind", true, true, false, true));
        assertTrue(AcousticPhysicsEngine.shouldApplyEfx("mycelialmind", false, false, true, false));

        // Vanilla sounds receive EFX only when enabled AND inside Deep Mycelium or near a Sinkhole
        assertFalse(AcousticPhysicsEngine.shouldApplyEfx("minecraft", false, false, true, true));
        assertTrue(AcousticPhysicsEngine.shouldApplyEfx("minecraft", true, false, true, true));
        assertTrue(AcousticPhysicsEngine.shouldApplyEfx("minecraft", false, true, true, true));
        assertFalse(AcousticPhysicsEngine.shouldApplyEfx("minecraft", true, true, true, false));

        // Third-party namespaces are left untouched
        assertFalse(AcousticPhysicsEngine.shouldApplyEfx("create", true, true, true, true));
    }

    @Test
    void voicePriorityAndSwarmAntiCacophonyBudgetDropSpamButPreserveCriticalCues() {
        assertEquals(VoicePriority.CRITICAL, AcousticPhysicsEngine.classifySoundPriority("ui_codex_tab", false, true));
        assertEquals(VoicePriority.CRITICAL, AcousticPhysicsEngine.classifySoundPriority("boss_stagger_break", false, false));
        assertEquals(VoicePriority.CRITICAL, AcousticPhysicsEngine.classifySoundPriority("sinkhole_updraft_loop", true, false));
        assertEquals(VoicePriority.LOW, AcousticPhysicsEngine.classifySoundPriority("mob_step_chitin", false, false));
        assertEquals(VoicePriority.LOW, AcousticPhysicsEngine.classifySoundPriority("sporeling_chitter", false, false));
        assertEquals(VoicePriority.NORMAL, AcousticPhysicsEngine.classifySoundPriority("chitin_brute_growl", false, false));
        assertEquals(VoicePriority.HIGH, AcousticPhysicsEngine.classifySoundPriority("brute_wall_stun", false, false));

        SwarmVoiceBudget budget = new SwarmVoiceBudget();
        long t0 = 10_000L;

        // First two nearby footsteps pass; third identical footstep inside 140 ms window is throttled
        assertTrue(budget.evaluate("mob_step_chitin", VoicePriority.LOW, 25.0D, t0));
        assertTrue(budget.evaluate("mob_step_chitin", VoicePriority.LOW, 25.0D, t0 + 20L));
        assertFalse(budget.evaluate("mob_step_chitin", VoicePriority.LOW, 25.0D, t0 + 40L));

        // Distant duplicate normal voice is capped even tighter (1 per 180 ms)
        assertTrue(budget.evaluate("stalker_hiss", VoicePriority.NORMAL, 500.0D, t0 + 50L));
        assertFalse(budget.evaluate("stalker_hiss", VoicePriority.NORMAL, 500.0D, t0 + 70L));

        // Critical boss / UI / alert cues are NEVER dropped even under heavy swarm saturation
        for (int i = 0; i < 12; i++) {
            assertTrue(budget.evaluate("boss_telegraph_lock", VoicePriority.CRITICAL, 64.0D, t0 + 80L + i));
        }

        // After the 350 ms window elapses, voices recover cleanly
        assertTrue(budget.evaluate("mob_step_chitin", VoicePriority.LOW, 25.0D, t0 + 600L));
        assertTrue(budget.getAcceptedCount() >= 15L);
        assertTrue(budget.getSuppressedCount() >= 2L);
    }

    @Test
    void sinkholeUpdraftLoopVolumeAndClientConfigDefaultsAreCalibrated() {
        float closeVol = AcousticPhysicsEngine.computeSinkholeUpdraftLoopVolume(4.0D, 18.0D, 1.0F, 1.0F, true);
        float midVol = AcousticPhysicsEngine.computeSinkholeUpdraftLoopVolume(100.0D, 18.0D, 1.0F, 1.0F, true);
        float duckedVol = AcousticPhysicsEngine.computeSinkholeUpdraftLoopVolume(4.0D, 18.0D, 1.0F, 0.45F, true);
        float farVol = AcousticPhysicsEngine.computeSinkholeUpdraftLoopVolume(400.0D, 18.0D, 1.0F, 1.0F, true);
        float disabledVol = AcousticPhysicsEngine.computeSinkholeUpdraftLoopVolume(4.0D, 18.0D, 1.0F, 1.0F, false);

        assertTrue(closeVol > midVol && midVol > 0.0F);
        assertTrue(duckedVol > 0.0F && duckedVol < closeVol);
        assertEquals(0.0F, farVol, 1e-6F);
        assertEquals(0.0F, disabledVol, 1e-6F);

        assertTrue(MycelialClientConfig.openAlEfxAcousticsEnabled());
        assertTrue(MycelialClientConfig.openAlEfxVanillaInMycelialZones());
        assertTrue(MycelialClientConfig.swarmVoiceLimitingEnabled());
        assertTrue(MycelialClientConfig.sinkholeUpdraftBedEnabled());
        assertTrue(MycelialClientConfig.openAlEfxDynamicTrackingEnabled());
        assertTrue(MycelialClientConfig.adaptiveAmbientLayersEnabled());
        assertTrue(MycelialClientConfig.spatialMicroAmbienceEnabled());
    }

    @Test
    void v5SubmersionSmoothingAdaptiveBedsAndLayeredDuckingAreMonotonic() {
        float clearOcc = AcousticPhysicsEngine.computeEffectiveOcclusion(0.0F, AcousticPhysicsEngine.SubmersionState.NONE);
        float fogOcc = AcousticPhysicsEngine.computeEffectiveOcclusion(0.0F, AcousticPhysicsEngine.SubmersionState.SPORE_FOG);
        float waterOcc = AcousticPhysicsEngine.computeEffectiveOcclusion(0.0F, AcousticPhysicsEngine.SubmersionState.WATER);
        assertTrue(clearOcc < fogOcc && fogOcc < waterOcc && waterOcc <= 1.0F);
        assertTrue(AcousticPhysicsEngine.computeEffectiveOcclusion(0.5F, AcousticPhysicsEngine.SubmersionState.SPORE_FOG)
                < AcousticPhysicsEngine.computeEffectiveOcclusion(0.5F, AcousticPhysicsEngine.SubmersionState.WATER));

        float smoothed = AcousticPhysicsEngine.smoothOcclusion(0.0F, 0.80F, 0.50F);
        assertEquals(0.40F, smoothed, 1e-5F);
        assertEquals(0.0F, AcousticPhysicsEngine.smoothOcclusion(Float.NaN, -1.0F, 0.5F), 1e-5F);

        float normalDuck = AcousticPhysicsEngine.computeLayerDuckMultiplier(false, 1.0F, true);
        float bossDuck = AcousticPhysicsEngine.computeLayerDuckMultiplier(true, 1.0F, true);
        float stackedDuck = AcousticPhysicsEngine.computeLayerDuckMultiplier(true, 0.5F, true);
        assertEquals(1.0F, normalDuck, 1e-5F);
        assertTrue(bossDuck < normalDuck && stackedDuck < bossDuck && stackedDuck >= 0.15F);
        assertEquals(1.0F, AcousticPhysicsEngine.computeLayerDuckMultiplier(true, 0.4F, false), 1e-5F);

        assertEquals(0.0F, AcousticPhysicsEngine.computeAdaptiveExplorationBedVolume(5.0D, false, 1.0F, 1.0F, true), 1e-6F);
        float expLow = AcousticPhysicsEngine.computeAdaptiveExplorationBedVolume(20.0D, false, 1.0F, 1.0F, true);
        float expHigh = AcousticPhysicsEngine.computeAdaptiveExplorationBedVolume(75.0D, false, 1.0F, 1.0F, true);
        assertTrue(expLow > 0.0F && expHigh > expLow);
        assertEquals(0.0F, AcousticPhysicsEngine.computeAdaptiveExplorationBedVolume(75.0D, true, 1.0F, 1.0F, true), 1e-6F);

        assertEquals(0.0F, AcousticPhysicsEngine.computeSwarmCombatBedVolume(1, 50.0D, false, 1.0F, 1.0F, true), 1e-6F);
        float swarmSmall = AcousticPhysicsEngine.computeSwarmCombatBedVolume(2, 30.0D, false, 1.0F, 1.0F, true);
        float swarmHeavy = AcousticPhysicsEngine.computeSwarmCombatBedVolume(7, 80.0D, false, 1.0F, 1.0F, true);
        assertTrue(swarmSmall > 0.0F && swarmHeavy > swarmSmall);
        assertEquals(0.0F, AcousticPhysicsEngine.computeSwarmCombatBedVolume(7, 80.0D, true, 1.0F, 1.0F, true), 1e-6F);

        assertEquals(VoicePriority.LOW, AcousticPhysicsEngine.classifySoundPriority("cavern_spore_drip", false, false));
        assertEquals(VoicePriority.LOW, AcousticPhysicsEngine.classifySoundPriority("sanctum_harmonic_chime", false, false));
        assertEquals(VoicePriority.CRITICAL, AcousticPhysicsEngine.classifySoundPriority("hive_tension_bed_low", true, false));
        assertEquals(VoicePriority.CRITICAL, AcousticPhysicsEngine.classifySoundPriority("hive_swarm_combat_bed", true, false));
    }

    @Test
    void v6PlaybackHumanizerVariesConsecutiveHitsAndKeepsCleanTechAndLoopsBounded() {
        AcousticPhysicsEngine.PlaybackHumanizer humanizer = new AcousticPhysicsEngine.PlaybackHumanizer(1337L);

        assertEquals(
                AcousticPhysicsEngine.HumanizedPlayback.IDENTITY,
                humanizer.nextForSound("sporeling_chitter", true, false, 1000L)
        );
        assertEquals(
                AcousticPhysicsEngine.HumanizedPlayback.IDENTITY,
                humanizer.nextForSound("sporeling_chitter", false, true, 1010L)
        );
        assertEquals(
                AcousticPhysicsEngine.HumanizedPlayback.IDENTITY,
                humanizer.nextForSound("ui_codex_tab", false, false, 1020L)
        );
        assertEquals(0L, humanizer.getHumanizedCount());

        AcousticPhysicsEngine.HumanizedPlayback step1 = humanizer.nextForSound("mob_step_chitin", false, false, 2000L);
        AcousticPhysicsEngine.HumanizedPlayback step2 = humanizer.nextForSound("mob_step_chitin", false, false, 2180L);
        AcousticPhysicsEngine.HumanizedPlayback step3 = humanizer.nextForSound("mob_step_chitin", false, false, 2360L);
        assertTrue(Math.abs(step1.pitchMultiplier() - 1.0F) <= 0.06F);
        assertTrue(Math.abs(step2.pitchMultiplier() - step1.pitchMultiplier()) >= 0.025F,
                "Consecutive footsteps must step apart to prevent machine-gunning");
        assertTrue(Math.abs(step3.pitchMultiplier() - step2.pitchMultiplier()) >= 0.025F);
        assertTrue(step1.gainMultiplier() >= 0.88F && step1.gainMultiplier() <= 1.06F);

        for (int i = 0; i < 8; i++) {
            AcousticPhysicsEngine.HumanizedPlayback tech =
                    humanizer.nextForSound("purifier_drone_fire", false, false, 3000L + i * 120L);
            assertTrue(Math.abs(tech.pitchMultiplier() - 1.0F) <= 0.016F,
                    "Clean Purifier tech must preserve tighter pitch stability");
        }
        assertTrue(humanizer.getHumanizedCount() >= 10L);
        humanizer.reset();
        assertEquals(0L, humanizer.getHumanizedCount());
    }

    @Test
    void v7TimbreDampingVariesWithinBoundsAndMaintainsEnvironmentOrdering() {
        assertEquals(0.0F, AcousticPhysicsEngine.HumanizedPlayback.IDENTITY.timbreDamping(), 1e-6F);
        AcousticPhysicsEngine.PlaybackHumanizer humanizer = new AcousticPhysicsEngine.PlaybackHumanizer(202607L);

        assertEquals(0.0F, humanizer.nextForSound("mob_step_chitin", true, false, 100L).timbreDamping(), 1e-6F);
        assertEquals(0.0F, humanizer.nextForSound("mob_step_chitin", false, true, 110L).timbreDamping(), 1e-6F);
        assertEquals(0.0F, humanizer.nextForSound("ui_action_confirm", false, false, 120L).timbreDamping(), 1e-6F);

        AcousticPhysicsEngine.HumanizedPlayback h1 = humanizer.nextForSound("mob_step_chitin", false, false, 500L);
        AcousticPhysicsEngine.HumanizedPlayback h2 = humanizer.nextForSound("mob_step_chitin", false, false, 700L);
        assertTrue(h1.timbreDamping() >= 0.0F && h1.timbreDamping() <= 0.1801F);
        assertTrue(h2.timbreDamping() >= 0.0F && h2.timbreDamping() <= 0.1801F);
        assertTrue(Math.abs(h2.timbreDamping() - h1.timbreDamping()) >= 0.050F,
                "Consecutive organic hits must vary HF timbre damping");

        for (int i = 0; i < 6; i++) {
            float td = humanizer.nextForSound("purification_turret_fire", false, false, 1200L + i * 100L).timbreDamping();
            assertTrue(td >= 0.0F && td <= 0.0601F,
                    "Copper-amethyst Purifier tech must stay brighter with bounded timbreDamping <= 0.06");
        }

        AcousticEnvironment[] envs = AcousticEnvironment.values();
        for (int i = 1; i < envs.length; i++) {
            assertTrue(envs[i - 1].wetSendGain() < envs[i].wetSendGain());
            assertTrue(envs[i - 1].decayTime() < envs[i].decayTime());
        }
    }
}
