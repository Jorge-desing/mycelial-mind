package com.alex.mycelialmind;

import com.alex.mycelialmind.hive.PostMatriarchExpeditionRules;
import com.alex.mycelialmind.hive.StimulusType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostMatriarchExpeditionRulesTest {
    @Test
    void dominantStimulusSelectsStableFloraPalette() {
        assertEquals(PostMatriarchExpeditionRules.BASE_PALETTE, PostMatriarchExpeditionRules.paletteForStimuli(Map.of()));

        Map<StimulusType, Double> scores = new EnumMap<>(StimulusType.class);
        scores.put(StimulusType.PROJECTILE, 25.0);
        scores.put(StimulusType.FIRE, 40.0);
        assertEquals(PostMatriarchExpeditionRules.FIRE_PALETTE, PostMatriarchExpeditionRules.paletteForStimuli(scores));

        scores.put(StimulusType.MAGIC, 40.0);
        assertEquals(PostMatriarchExpeditionRules.FIRE_PALETTE, PostMatriarchExpeditionRules.paletteForStimuli(scores),
                "Tied scores keep the first StimulusType order for a repeatable color.");
    }

    @Test
    void routePulsesRequirePylonClearAreaAndCooldown() {
        assertFalse(PostMatriarchExpeditionRules.canStartNextRoutePulse(false, 0, 0, 0L, 0L));
        assertFalse(PostMatriarchExpeditionRules.canStartNextRoutePulse(true, 1, 0, 0L, 0L));
        assertTrue(PostMatriarchExpeditionRules.canStartNextRoutePulse(true, 0, 0, 0L, 0L));
        assertFalse(PostMatriarchExpeditionRules.canStartNextRoutePulse(
                true, 0, 1, 99L, 0L));
        assertTrue(PostMatriarchExpeditionRules.canStartNextRoutePulse(
                true, 0, 1, PostMatriarchExpeditionRules.ROUTE_PULSE_COOLDOWN_TICKS, 0L));
        assertFalse(PostMatriarchExpeditionRules.canStartNextRoutePulse(
                true, 0, PostMatriarchExpeditionRules.REQUIRED_ROUTE_PULSES, 1000L, 0L));
        assertFalse(PostMatriarchExpeditionRules.routeIsComplete(3, 1));
        assertTrue(PostMatriarchExpeditionRules.routeIsComplete(3, 0));
    }

    @Test
    void gardenCompletesAfterThreeLinkedFloraFoci() {
        assertFalse(PostMatriarchExpeditionRules.gardenIsComplete(2));
        assertTrue(PostMatriarchExpeditionRules.gardenIsComplete(3));
        assertTrue(PostMatriarchExpeditionRules.gardenIsComplete(4));
    }
}
