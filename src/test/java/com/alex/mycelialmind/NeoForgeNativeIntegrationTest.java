package com.alex.mycelialmind;

import com.alex.mycelialmind.client.DeepMyceliumSpecialEffects;
import com.alex.mycelialmind.client.MycelialItemDecorators;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.item.tooltip.MycelialTooltipData;
import com.alex.mycelialmind.registry.ModDataMaps;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless JUnit 5 coverage for NeoForge 21.1.267 native codecs, custom dimension visual
 * transforms, and diegetic item decorator helpers.
 */
class NeoForgeNativeIntegrationTest {
    @Test
    void customDataMapCodecsRoundTripAndRejectInvalidValues() {
        for (StimulusType stimulus : StimulusType.values()) {
            ModDataMaps.DamageStimulusData data = new ModDataMaps.DamageStimulusData(stimulus, 2.25F);
            JsonElement encoded = ModDataMaps.DamageStimulusData.CODEC.encodeStart(JsonOps.INSTANCE, data)
                    .getOrThrow();
            ModDataMaps.DamageStimulusData decoded = ModDataMaps.DamageStimulusData.CODEC
                    .parse(JsonOps.INSTANCE, encoded)
                    .getOrThrow();
            assertEquals(data, decoded);
        }

        JsonObject stimulusOnly = new JsonObject();
        stimulusOnly.addProperty("stimulus", "fire");
        ModDataMaps.DamageStimulusData defaulted = ModDataMaps.DamageStimulusData.CODEC
                .parse(JsonOps.INSTANCE, stimulusOnly)
                .getOrThrow();
        assertEquals(StimulusType.FIRE, defaulted.stimulus());
        assertEquals(1.0F, defaulted.weightMultiplier(), 1.0e-5F);

        JsonObject badStimulus = new JsonObject();
        badStimulus.addProperty("stimulus", "unknown_kind");
        assertTrue(ModDataMaps.DamageStimulusData.CODEC.parse(JsonOps.INSTANCE, badStimulus).error().isPresent());

        JsonObject badWeight = new JsonObject();
        badWeight.addProperty("stimulus", "magic");
        badWeight.addProperty("weight_multiplier", 25.0F);
        assertTrue(ModDataMaps.DamageStimulusData.CODEC.parse(JsonOps.INSTANCE, badWeight).error().isPresent());

        ModDataMaps.SymbioticNutrientData nutrient = new ModDataMaps.SymbioticNutrientData(18);
        JsonElement encodedNutrient = ModDataMaps.SymbioticNutrientData.CODEC.encodeStart(JsonOps.INSTANCE, nutrient)
                .getOrThrow();
        assertEquals(nutrient, ModDataMaps.SymbioticNutrientData.CODEC.parse(JsonOps.INSTANCE, encodedNutrient).getOrThrow());

        JsonObject badNutrient = new JsonObject();
        badNutrient.addProperty("stability_restore", 0);
        assertTrue(ModDataMaps.SymbioticNutrientData.CODEC.parse(JsonOps.INSTANCE, badNutrient).error().isPresent());
    }

    @Test
    void deepMyceliumSpecialEffectsFogAndLightmapStayBounded() {
        DeepMyceliumSpecialEffects effects = new DeepMyceliumSpecialEffects();
        assertTrue(effects.isFoggyAt(0, 0));
        assertEquals(DimensionSpecialEffects.SkyType.NONE, effects.skyType());

        Vec3 brightFog = DeepMyceliumSpecialEffects.computeFogColor(new Vec3(0.4D, 0.2D, 0.5D), 1.0F);
        Vec3 darkFog = DeepMyceliumSpecialEffects.computeFogColor(new Vec3(0.4D, 0.2D, 0.5D), 0.0F);
        assertTrue(brightFog.x > darkFog.x && brightFog.z > darkFog.z);
        assertTrue(brightFog.x >= 0.0D && brightFog.x <= 1.0D);
        assertTrue(brightFog.y >= 0.0D && brightFog.y <= 1.0D);
        assertTrue(brightFog.z >= 0.0D && brightFog.z <= 1.0D);

        Vector3f darkLight = new Vector3f(0.02F, 0.02F, 0.02F);
        DeepMyceliumSpecialEffects.applyBioluminescentLightmapTint(darkLight, 0.0F);
        assertTrue(darkLight.z > darkLight.x && darkLight.z > 0.05F);

        Vector3f maxLight = new Vector3f(0.99F, 0.99F, 0.99F);
        DeepMyceliumSpecialEffects.applyBioluminescentLightmapTint(maxLight, 1.0F);
        assertEquals(0.99F, maxLight.x, 1.0e-5F);
        assertEquals(0.99F, maxLight.z, 1.0e-5F);
    }

    @Test
    void itemDecoratorHelpersMapAllEnumsAndClampThreatBars() {
        assertEquals(0, MycelialItemDecorators.colorForStimulus(null));
        assertEquals(0, MycelialItemDecorators.colorForTrait(null));
        assertEquals(0, MycelialItemDecorators.colorForMutagen(null));
        assertEquals(0, MycelialItemDecorators.colorForEngram(null));

        Set<Integer> stimulusColors = new HashSet<>();
        for (StimulusType stimulus : StimulusType.values()) {
            int color = MycelialItemDecorators.colorForStimulus(stimulus);
            assertTrue(color != 0);
            stimulusColors.add(color);
        }
        assertEquals(StimulusType.values().length, stimulusColors.size());

        for (AdaptationTrait trait : AdaptationTrait.values()) {
            assertEquals(MycelialItemDecorators.colorForStimulus(trait.trigger()),
                    MycelialItemDecorators.colorForTrait(trait));
        }
        for (SymbioticMutagen mutagen : SymbioticMutagen.values()) {
            assertEquals(MycelialItemDecorators.colorForTrait(mutagen.sourceTrait()),
                    MycelialItemDecorators.colorForMutagen(mutagen));
        }
        for (FieldEngramId engram : FieldEngramId.values()) {
            assertTrue(MycelialItemDecorators.colorForEngram(engram) != 0);
        }

        assertEquals(0, MycelialItemDecorators.threatBarWidth(-5.0D));
        assertEquals(13, MycelialItemDecorators.threatBarWidth(150.0D));
        assertTrue(MycelialItemDecorators.threatBarWidth(50.0D) >= 6 && MycelialItemDecorators.threatBarWidth(50.0D) <= 7);

        int lowColor = MycelialItemDecorators.threatBarColor(10.0D);
        int midColor = MycelialItemDecorators.threatBarColor(35.0D);
        int warnColor = MycelialItemDecorators.threatBarColor(60.0D);
        int critColor = MycelialItemDecorators.threatBarColor(90.0D);
        assertFalse(lowColor == midColor || midColor == warnColor || warnColor == critColor);
    }

    @Test
    void nativeAttributesTooltipDataAndMilestoneTriggerValidateSafely() {
        assertEquals(60.0D, DimensionalHomeostasisEngine.computeAttributeMitigatedValue(100.0D, 0.40D, 20.0D), 1.0e-5D);
        assertEquals(20.0D, DimensionalHomeostasisEngine.computeAttributeMitigatedValue(100.0D, 0.90D, 20.0D), 1.0e-5D);
        assertEquals(20.0D, DimensionalHomeostasisEngine.computeAttributeMitigatedValue(100.0D, 2.50D, 20.0D), 1.0e-5D);
        assertEquals(100.0D, DimensionalHomeostasisEngine.computeAttributeMitigatedValue(100.0D, -0.50D, 20.0D), 1.0e-5D);

        MycelialTooltipData.ThreatBarTooltip threatTooltip =
                new MycelialTooltipData.ThreatBarTooltip(125.0F, true, -2);
        assertEquals(100.0F, threatTooltip.clampedThreat(), 1.0e-5F);
        assertEquals(0, threatTooltip.clampedTraitCount());

        MycelialTooltipData.TraitBadgeTooltip badge = new MycelialTooltipData.TraitBadgeTooltip(
                MycelialTooltipData.colorForTrait(AdaptationTrait.FIRE_RESISTANT),
                Component.literal("Pyro Surge"),
                null
        );
        assertTrue(badge.accentRgb() != 0);
    }
}
