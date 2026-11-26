package com.alex.mycelialmind.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Custom dimension visual effects for {@code mycelialmind:deep_mycelium}, providing a dark
 * bioluminescent indigo-violet subterranean fog baseline and gentle cavern lightmap tinting
 * without conflicting with Iris shaderpacks or dynamic Homeostasis fog overrides.
 */
public final class DeepMyceliumSpecialEffects extends DimensionSpecialEffects {
    private static final double BASE_FOG_RED = 0.09D;
    private static final double BASE_FOG_GREEN = 0.06D;
    private static final double BASE_FOG_BLUE = 0.14D;

    public DeepMyceliumSpecialEffects() {
        super(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, false, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float brightness) {
        return computeFogColor(biomeFogColor, brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    @Override
    public void adjustLightmapColors(
            ClientLevel level,
            float partialTicks,
            float skyDarken,
            float blockLightRedFlicker,
            float skyLightModifier,
            int pixelX,
            int pixelY,
            Vector3f colors
    ) {
        float blockBrightness = clamp01(pixelX / 15.0F);
        applyBioluminescentLightmapTint(colors, blockBrightness);
    }

    public static Vec3 computeFogColor(Vec3 biomeFogColor, float brightness) {
        float clamped = clamp01(brightness);
        double scale = 0.78D + 0.22D * clamped;
        double r = (biomeFogColor.x * 0.82D + BASE_FOG_RED * 0.18D) * scale;
        double g = (biomeFogColor.y * 0.82D + BASE_FOG_GREEN * 0.18D) * scale;
        double b = (biomeFogColor.z * 0.82D + BASE_FOG_BLUE * 0.18D) * scale;
        return new Vec3(clamp01((float) r), clamp01((float) g), clamp01((float) b));
    }

    public static void applyBioluminescentLightmapTint(Vector3f colors, float blockBrightness) {
        float darkness = 1.0F - clamp01(blockBrightness);
        float boost = darkness * darkness;
        colors.set(
                clamp01(colors.x + 0.015F * boost),
                clamp01(colors.y + 0.025F * boost),
                clamp01(colors.z + 0.045F * boost)
        );
    }

    private static float clamp01(float value) {
        return Math.clamp(value, 0.0F, 1.0F);
    }
}
