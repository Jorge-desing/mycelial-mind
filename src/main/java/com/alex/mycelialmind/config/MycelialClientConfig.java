package com.alex.mycelialmind.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side accessibility & UI layout preferences for Bio-Scanner HUD and spore fog.
 * Common-safe: no net.minecraft.client.* imports so pure unit tests & server code never load client classes.
 */
public final class MycelialClientConfig {
    public enum HudAnchor implements net.neoforged.neoforge.common.TranslatableEnum {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        @Override
        public net.minecraft.network.chat.Component getTranslatedName() {
            return net.minecraft.network.chat.Component.translatable(
                    "mycelialmind.configuration.hudAnchor." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ModConfigSpec.IntValue HUD_OFFSET_X;
    public static final ModConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ModConfigSpec.BooleanValue HUD_COMPACT_MODE;
    public static final ModConfigSpec.BooleanValue HUD_CONTEXTUAL_COMBAT;
    public static final ModConfigSpec.BooleanValue HUD_REQUIRE_HELD;
    public static final ModConfigSpec.BooleanValue SHOW_SYNAPTIC_VISION_BORDER;
    public static final ModConfigSpec.DoubleValue SPORE_FOG_DENSITY_SCALE;
    public static final ModConfigSpec.DoubleValue AMBIENT_SOUNDSCAPE_VOLUME;
    public static final ModConfigSpec.BooleanValue HIVE_ADAPTATION_AUDIO_ALERTS;
    public static final ModConfigSpec.BooleanValue CAVE_SOUND_OCCLUSION;
    public static final ModConfigSpec.BooleanValue DYNAMIC_AUDIO_DUCKING;
    public static final ModConfigSpec.BooleanValue BOSS_TENSION_BED_ENABLED;
    public static final ModConfigSpec.BooleanValue OPENAL_EFX_ACOUSTICS_ENABLED;
    public static final ModConfigSpec.BooleanValue OPENAL_EFX_VANILLA_IN_MYCELIAL_ZONES;
    public static final ModConfigSpec.BooleanValue SWARM_VOICE_LIMITING_ENABLED;
    public static final ModConfigSpec.BooleanValue SINKHOLE_UPDRAFT_BED_ENABLED;
    public static final ModConfigSpec.BooleanValue OPENAL_EFX_DYNAMIC_TRACKING_ENABLED;
    public static final ModConfigSpec.BooleanValue ADAPTIVE_AMBIENT_LAYERS_ENABLED;
    public static final ModConfigSpec.BooleanValue SPATIAL_MICRO_AMBIENCE_ENABLED;
    public static final ModConfigSpec.DoubleValue OPENAL_EFX_REVERB_SCALE;
    public static final ModConfigSpec.DoubleValue OPENAL_EFX_OCCLUSION_SCALE;
    public static final ModConfigSpec.DoubleValue PARTICLE_DENSITY_SCALE;
    public static final ModConfigSpec.BooleanValue ENABLE_ADDITIVE_PARTICLE_GLOW;
    public static final ModConfigSpec.DoubleValue SCREEN_SHAKE_SCALE;
    public static final ModConfigSpec.BooleanValue STATUS_VIGNETTE_ENABLED;
    public static final ModConfigSpec.BooleanValue UI_AUDIO_FEEDBACK_ENABLED;
    public static final ModConfigSpec.BooleanValue SMOOTH_HUD_BARS_ENABLED;
    public static final ModConfigSpec.BooleanValue JOURNEYMAP_OVERLAYS_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("hud");
        HUD_ANCHOR = builder
                .comment("Screen corner anchor for the Bio-Scanner HUD overlay.")
                .defineEnum("hudAnchor", HudAnchor.TOP_LEFT);

        HUD_OFFSET_X = builder
                .comment("Horizontal pixel offset from the selected HUD anchor.")
                .defineInRange("hudOffsetX", 0, -200, 200);

        HUD_OFFSET_Y = builder
                .comment("Vertical pixel offset from the selected HUD anchor.")
                .defineInRange("hudOffsetY", 0, -200, 200);

        HUD_COMPACT_MODE = builder
                .comment("Use a compact summary layout for the Bio-Scanner HUD overlay.")
                .define("hudCompactMode", false);

        HUD_CONTEXTUAL_COMBAT = builder
                .comment("Automatically switch the Bio-Scanner HUD to compact combat focus during active combat.")
                .define("hudContextualCombat", true);

        HUD_REQUIRE_HELD = builder
                .comment("If true, the HUD displays only while holding Bio-Scanner or Bio-Extractor; if false, carrying either in the inventory suffices.")
                .define("hudRequireHeld", true);

        SHOW_SYNAPTIC_VISION_BORDER = builder
                .comment("Show the pulsing cyan perimeter border while Synaptic Thermal Vision is active.")
                .define("showSynapticVisionBorder", true);
        builder.pop();

        builder.push("atmosphere");
        SPORE_FOG_DENSITY_SCALE = builder
                .comment("Accessibility scale for Deep Mycelium / Fungal Tide spore fog density (0.25 = gentler fog, 1.0 = full vanilla+ density).")
                .defineInRange("sporeFogDensityScale", 1.0, 0.25, 1.0);
        builder.pop();

        builder.push("audio");
        AMBIENT_SOUNDSCAPE_VOLUME = builder
                .comment("Volume scale for reactive positional soundscapes (Purification Pylon hum, Fungal Tide storm & Homeostasis heartbeat).")
                .defineInRange("ambientSoundscapeVolume", 1.0, 0.0, 1.0);

        HIVE_ADAPTATION_AUDIO_ALERTS = builder
                .comment("Play client audio alerts when the Hive Mind unlocks a new AdaptationTrait, HiveSynergy, or threat milestone.")
                .define("hiveAdaptationAudioAlerts", true);

        CAVE_SOUND_OCCLUSION = builder
                .comment("Attenuate volume and pitch when Mycelial organisms vocalize behind solid cave walls.")
                .define("caveSoundOcclusion", true);

        DYNAMIC_AUDIO_DUCKING = builder
                .comment("Automatically duck ambient soundscape loops during high-priority Hive alerts and boss encounters.")
                .define("dynamicAudioDucking", true);

        BOSS_TENSION_BED_ENABLED = builder
                .comment("Play a reactive sub-harmonic tension drone bed when fighting Mycelial bosses.")
                .define("bossTensionBedEnabled", true);

        OPENAL_EFX_ACOUSTICS_ENABLED = builder
                .comment("Enable native OpenAL EFX cavern/sanctum reverb, material raycast occlusion filtering, and distance air absorption (auto-yields if Sound Physics Remastered is installed).")
                .define("openAlEfxAcousticsEnabled", true);

        OPENAL_EFX_VANILLA_IN_MYCELIAL_ZONES = builder
                .comment("Extend OpenAL EFX reverb and occlusion to vanilla Minecraft sounds while inside Deep Mycelium or near a Mycelial Sinkhole.")
                .define("openAlEfxVanillaInMycelialZones", true);

        SWARM_VOICE_LIMITING_ENABLED = builder
                .comment("Limit redundant simultaneous swarm footsteps and chitters via a sliding-window voice budget while prioritizing bosses, alerts, and UI cues.")
                .define("swarmVoiceLimitingEnabled", true);

        SINKHOLE_UPDRAFT_BED_ENABLED = builder
                .comment("Play a reactive vertical spore-wind chimney loop bed near active Sinkhole updraft vents.")
                .define("sinkholeUpdraftBedEnabled", true);

        OPENAL_EFX_DYNAMIC_TRACKING_ENABLED = builder
                .comment("Continuously re-evaluate occlusion, underwater/spore-fog submersion, and room EFX reverb on active playing channels.")
                .define("openAlEfxDynamicTrackingEnabled", true);

        ADAPTIVE_AMBIENT_LAYERS_ENABLED = builder
                .comment("Play multi-layer adaptive tension and swarm skirmish loops alongside reactive sidechain ducking.")
                .define("adaptiveAmbientLayersEnabled", true);

        SPATIAL_MICRO_AMBIENCE_ENABLED = builder
                .comment("Emit periodic directional 3D cavern/sanctum micro-ambience (drips, hyphal creaks, whispers, chimes).")
                .define("spatialMicroAmbienceEnabled", true);

        OPENAL_EFX_REVERB_SCALE = builder
                .comment("Global scale factor for adaptive OpenAL EFX cavern/sanctum reverb intensity (0.0 = dry, 1.5 = deep cathedral).")
                .defineInRange("openAlEfxReverbScale", 1.0, 0.0, 1.5);

        OPENAL_EFX_OCCLUSION_SCALE = builder
                .comment("Global scale factor for wall/material low-pass sound occlusion muffling (0.0 = off, 1.5 = strong muffling).")
                .defineInRange("openAlEfxOcclusionScale", 1.0, 0.0, 1.5);
        builder.pop();

        builder.push("particles");
        PARTICLE_DENSITY_SCALE = builder
                .comment("Accessibility density scale for Mycelial Mind particles (0.25 = sparse, 1.5 = denser).")
                .defineInRange("particleDensityScale", 1.0, 0.25, 1.5);

        ENABLE_ADDITIVE_PARTICLE_GLOW = builder
                .comment("Render emissive particles with the additive glow pass; disable if a shader pack misbehaves.")
                .define("enableAdditiveParticleGlow", true);
        builder.pop();

        builder.push("juice");
        SCREEN_SHAKE_SCALE = builder
                .comment("Intensity scale for tactile camera micro-shake on boss impacts, relic discharges, and sudden threat spikes (0.0 = off, 1.0 = full).")
                .defineInRange("screenShakeScale", 0.65, 0.0, 1.0);

        STATUS_VIGNETTE_ENABLED = builder
                .comment("Render soft perimeter status vignettes during Spore Rot, Mycelial Infection, Suppressed state, or Overmind threat.")
                .define("statusVignetteEnabled", true);

        UI_AUDIO_FEEDBACK_ENABLED = builder
                .comment("Play crisp audio micro-cues when switching tabs or confirming actions in mod screens.")
                .define("uiAudioFeedbackEnabled", true);

        SMOOTH_HUD_BARS_ENABLED = builder
                .comment("Smooth Bio-Scanner HUD threat and homeostasis progress bars with subtle change pulses.")
                .define("smoothHudBarsEnabled", true);
        builder.pop();

        builder.push("integrations");
        JOURNEYMAP_OVERLAYS_ENABLED = builder
                .comment("Render Purification Pylon zones, Synaptic Nodes, and inflamed Deep Mycelium chunks on JourneyMap when installed.")
                .define("journeymapOverlaysEnabled", true);
        builder.pop();

        SPEC = builder.build();
    }

    private MycelialClientConfig() {
    }

    public static HudAnchor hudAnchor() {
        return SPEC.isLoaded() ? HUD_ANCHOR.get() : HUD_ANCHOR.getDefault();
    }

    public static int hudOffsetX() {
        return SPEC.isLoaded() ? HUD_OFFSET_X.get() : HUD_OFFSET_X.getDefault();
    }

    public static int hudOffsetY() {
        return SPEC.isLoaded() ? HUD_OFFSET_Y.get() : HUD_OFFSET_Y.getDefault();
    }

    public static boolean isHudCompact() {
        return SPEC.isLoaded() ? HUD_COMPACT_MODE.get() : HUD_COMPACT_MODE.getDefault();
    }

    public static boolean isHudContextualCombat() {
        return SPEC.isLoaded() ? HUD_CONTEXTUAL_COMBAT.get() : HUD_CONTEXTUAL_COMBAT.getDefault();
    }

    public static boolean shouldUseCombatCompactHud(
            boolean contextualCombatEnabled,
            boolean manualCompact,
            boolean inActiveCombat
    ) {
        return manualCompact || (contextualCombatEnabled && inActiveCombat);
    }

    public static boolean isHudRequireHeld() {
        return SPEC.isLoaded() ? HUD_REQUIRE_HELD.get() : HUD_REQUIRE_HELD.getDefault();
    }

    public static boolean showSynapticVisionBorder() {
        return SPEC.isLoaded() ? SHOW_SYNAPTIC_VISION_BORDER.get() : SHOW_SYNAPTIC_VISION_BORDER.getDefault();
    }

    public static double sporeFogDensityScale() {
        return SPEC.isLoaded() ? SPORE_FOG_DENSITY_SCALE.get() : SPORE_FOG_DENSITY_SCALE.getDefault();
    }

    public static double ambientSoundscapeVolume() {
        return SPEC.isLoaded() ? AMBIENT_SOUNDSCAPE_VOLUME.get() : AMBIENT_SOUNDSCAPE_VOLUME.getDefault();
    }

    public static boolean hiveAdaptationAudioAlerts() {
        return SPEC.isLoaded() ? HIVE_ADAPTATION_AUDIO_ALERTS.get() : HIVE_ADAPTATION_AUDIO_ALERTS.getDefault();
    }

    public static boolean caveSoundOcclusion() {
        return SPEC.isLoaded() ? CAVE_SOUND_OCCLUSION.get() : CAVE_SOUND_OCCLUSION.getDefault();
    }

    public static boolean dynamicAudioDucking() {
        return SPEC.isLoaded() ? DYNAMIC_AUDIO_DUCKING.get() : DYNAMIC_AUDIO_DUCKING.getDefault();
    }

    public static boolean bossTensionBedEnabled() {
        return SPEC.isLoaded() ? BOSS_TENSION_BED_ENABLED.get() : BOSS_TENSION_BED_ENABLED.getDefault();
    }

    public static boolean openAlEfxAcousticsEnabled() {
        return SPEC.isLoaded() ? OPENAL_EFX_ACOUSTICS_ENABLED.get() : OPENAL_EFX_ACOUSTICS_ENABLED.getDefault();
    }

    public static boolean openAlEfxVanillaInMycelialZones() {
        return SPEC.isLoaded() ? OPENAL_EFX_VANILLA_IN_MYCELIAL_ZONES.get() : OPENAL_EFX_VANILLA_IN_MYCELIAL_ZONES.getDefault();
    }

    public static boolean swarmVoiceLimitingEnabled() {
        return SPEC.isLoaded() ? SWARM_VOICE_LIMITING_ENABLED.get() : SWARM_VOICE_LIMITING_ENABLED.getDefault();
    }

    public static boolean sinkholeUpdraftBedEnabled() {
        return SPEC.isLoaded() ? SINKHOLE_UPDRAFT_BED_ENABLED.get() : SINKHOLE_UPDRAFT_BED_ENABLED.getDefault();
    }

    public static boolean openAlEfxDynamicTrackingEnabled() {
        return SPEC.isLoaded() ? OPENAL_EFX_DYNAMIC_TRACKING_ENABLED.get() : OPENAL_EFX_DYNAMIC_TRACKING_ENABLED.getDefault();
    }

    public static boolean adaptiveAmbientLayersEnabled() {
        return SPEC.isLoaded() ? ADAPTIVE_AMBIENT_LAYERS_ENABLED.get() : ADAPTIVE_AMBIENT_LAYERS_ENABLED.getDefault();
    }

    public static boolean spatialMicroAmbienceEnabled() {
        return SPEC.isLoaded() ? SPATIAL_MICRO_AMBIENCE_ENABLED.get() : SPATIAL_MICRO_AMBIENCE_ENABLED.getDefault();
    }

    public static double openAlEfxReverbScale() {
        return SPEC.isLoaded() ? OPENAL_EFX_REVERB_SCALE.get() : OPENAL_EFX_REVERB_SCALE.getDefault();
    }

    public static double openAlEfxOcclusionScale() {
        return SPEC.isLoaded() ? OPENAL_EFX_OCCLUSION_SCALE.get() : OPENAL_EFX_OCCLUSION_SCALE.getDefault();
    }

    public static double particleDensityScale() {
        return SPEC.isLoaded() ? PARTICLE_DENSITY_SCALE.get() : PARTICLE_DENSITY_SCALE.getDefault();
    }

    public static boolean enableAdditiveParticleGlow() {
        return SPEC.isLoaded() ? ENABLE_ADDITIVE_PARTICLE_GLOW.get() : ENABLE_ADDITIVE_PARTICLE_GLOW.getDefault();
    }

    public static double screenShakeScale() {
        return SPEC.isLoaded() ? SCREEN_SHAKE_SCALE.get() : SCREEN_SHAKE_SCALE.getDefault();
    }

    public static boolean statusVignetteEnabled() {
        return SPEC.isLoaded() ? STATUS_VIGNETTE_ENABLED.get() : STATUS_VIGNETTE_ENABLED.getDefault();
    }

    public static boolean uiAudioFeedbackEnabled() {
        return SPEC.isLoaded() ? UI_AUDIO_FEEDBACK_ENABLED.get() : UI_AUDIO_FEEDBACK_ENABLED.getDefault();
    }

    public static boolean smoothHudBarsEnabled() {
        return SPEC.isLoaded() ? SMOOTH_HUD_BARS_ENABLED.get() : SMOOTH_HUD_BARS_ENABLED.getDefault();
    }

    public static boolean journeyMapOverlaysEnabled() {
        return SPEC.isLoaded() ? JOURNEYMAP_OVERLAYS_ENABLED.get() : JOURNEYMAP_OVERLAYS_ENABLED.getDefault();
    }

    public static int[] computeHudPosition(
            HudAnchor anchor,
            int screenW,
            int screenH,
            int panelW,
            int panelH,
            int offsetX,
            int offsetY
    ) {
        final int margin = 10;
        HudAnchor safeAnchor = anchor != null ? anchor : HudAnchor.TOP_LEFT;
        int rawX = switch (safeAnchor) {
            case TOP_LEFT, BOTTOM_LEFT -> margin + offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - panelW - margin + offsetX;
        };
        int rawY = switch (safeAnchor) {
            case TOP_LEFT, TOP_RIGHT -> margin + offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - panelH - margin + offsetY;
        };
        int maxX = Math.max(2, screenW - panelW - 2);
        int maxY = Math.max(2, screenH - panelH - 2);
        int clampedX = Math.max(2, Math.min(maxX, rawX));
        int clampedY = Math.max(2, Math.min(maxY, rawY));
        return new int[]{clampedX, clampedY};
    }

    public static float computeScaledFogFarPlane(float baseFarPlane, float nearPlane, double densityScale) {
        double safeScale = Double.isFinite(densityScale) ? Math.max(0.25D, Math.min(1.0D, densityScale)) : 1.0D;
        if (safeScale >= 0.999D) {
            return baseFarPlane;
        }
        float mult = (float) (1.0D + (1.0D - safeScale) * 2.0D);
        return Math.max(nearPlane + 1.0F, baseFarPlane * mult);
    }
}
