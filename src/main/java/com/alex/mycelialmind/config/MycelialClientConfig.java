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
    public static final ModConfigSpec.DoubleValue PARTICLE_DENSITY_SCALE;
    public static final ModConfigSpec.BooleanValue ENABLE_ADDITIVE_PARTICLE_GLOW;

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
        builder.pop();

        builder.push("particles");
        PARTICLE_DENSITY_SCALE = builder
                .comment("Accessibility density scale for Mycelial Mind particles (0.25 = sparse, 1.5 = denser).")
                .defineInRange("particleDensityScale", 1.0, 0.25, 1.5);

        ENABLE_ADDITIVE_PARTICLE_GLOW = builder
                .comment("Render emissive particles with the additive glow pass; disable if a shader pack misbehaves.")
                .define("enableAdditiveParticleGlow", true);
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

    public static double particleDensityScale() {
        return SPEC.isLoaded() ? PARTICLE_DENSITY_SCALE.get() : PARTICLE_DENSITY_SCALE.getDefault();
    }

    public static boolean enableAdditiveParticleGlow() {
        return SPEC.isLoaded() ? ENABLE_ADDITIVE_PARTICLE_GLOW.get() : ENABLE_ADDITIVE_PARTICLE_GLOW.getDefault();
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
