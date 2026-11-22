package com.alex.mycelialmind;

import com.alex.mycelialmind.client.screen.MycelialCodexBookScreen;
import com.alex.mycelialmind.data.CodexBookData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodexBookScreenLogicTest {

    @Test
    void parseMarkdownComponentStripsMarkersAndAppliesStyles() {
        Component parsed = MycelialCodexBookScreen.parseMarkdownComponent(
                "Activa **Visión Sináptica** con `/mycelial status` — prueba • ok"
        );
        assertEquals("Activa Visión Sináptica con /mycelial status - prueba * ok", parsed.getString());
        boolean sawBold = parsed.getSiblings().stream().anyMatch(c -> c.getStyle().isBold());
        boolean sawCode = parsed.getSiblings().stream()
                .anyMatch(c -> c.getStyle().getColor() != null && c.getStyle().getColor().getValue() == 0x72EFDD);
        assertTrue(sawBold, "Expected bold sibling for **...**");
        assertTrue(sawCode, "Expected cyan code sibling for `...`");
    }

    @Test
    void parseMarkdownComponentHandlesEmptyNullAndUnclosedMarkers() {
        assertEquals("", MycelialCodexBookScreen.parseMarkdownComponent(null).getString());
        assertEquals("", MycelialCodexBookScreen.parseMarkdownComponent("").getString());
        assertEquals("Sin cerrar **texto y `codigo",
                MycelialCodexBookScreen.parseMarkdownComponent("Sin cerrar **texto y `codigo").getString());
    }

    @Test
    void parseAccentColorParsesHexAndFallsBackSafely() {
        assertEquals(0xFF9B4DFF, MycelialCodexBookScreen.parseAccentColor("#9B4DFF", 0xFF000000));
        assertEquals(0xFF06D6A0, MycelialCodexBookScreen.parseAccentColor("06D6A0", 0xFF000000));
        assertEquals(0xFF123456, MycelialCodexBookScreen.parseAccentColor("not-a-color", 0xFF123456));
        assertEquals(0xFF123456, MycelialCodexBookScreen.parseAccentColor(null, 0xFF123456));
    }

    @Test
    void computeHeartbeatIntensityIsBoundedAndScalesWithThreat() {
        float lowMin = MycelialCodexBookScreen.computeHeartbeatIntensity(0.0F, 0L);
        float highFloor = 1.0F;
        float lowPeak = 0.0F;
        for (long t = 0; t <= 4000; t += 100) {
            float vLow = MycelialCodexBookScreen.computeHeartbeatIntensity(0.0F, t);
            float vHigh = MycelialCodexBookScreen.computeHeartbeatIntensity(100.0F, t);
            assertTrue(vLow >= 0.0F && vLow <= 1.0F);
            assertTrue(vHigh >= 0.0F && vHigh <= 1.0F);
            lowPeak = Math.max(lowPeak, vLow);
            highFloor = Math.min(highFloor, vHigh);
        }
        assertTrue(lowPeak <= 0.36F);
        assertTrue(highFloor >= 0.14F);
        assertTrue(lowMin >= 0.0F);
    }

    @Test
    void atlasForTierAndPreviewCyclingCoverAllFourEvolutionStages() {
        assertEquals("mycelialmind:textures/gui/codex_book.png",
                MycelialCodexBookScreen.atlasForTier(HiveProgressionTier.DORMANT).toString());
        assertEquals("mycelialmind:textures/gui/codex_book_awakened.png",
                MycelialCodexBookScreen.atlasForTier(HiveProgressionTier.AWAKENED).toString());
        assertEquals("mycelialmind:textures/gui/codex_book_adaptive.png",
                MycelialCodexBookScreen.atlasForTier(HiveProgressionTier.ADAPTIVE).toString());
        assertEquals("mycelialmind:textures/gui/codex_book_overmind.png",
                MycelialCodexBookScreen.atlasForTier(HiveProgressionTier.OVERMIND).toString());
        assertEquals("mycelialmind:textures/gui/codex_book.png",
                MycelialCodexBookScreen.atlasForTier(null).toString());

        // Unlocked up to Act II (effectiveLevel=1), allowAllActs=false
        assertEquals(0, MycelialCodexBookScreen.cyclePreviewActTier(-1, 1, false));
        assertEquals(1, MycelialCodexBookScreen.cyclePreviewActTier(0, 1, false));
        assertEquals(-1, MycelialCodexBookScreen.cyclePreviewActTier(1, 1, false));

        // Spoilers/Creative mode allows cycling all 4 Acts (0..3) and returning to Auto (-1)
        int state = MycelialCodexBookScreen.cyclePreviewActTier(-1, 0, true);
        assertEquals(1, state);
        assertEquals(2, MycelialCodexBookScreen.cyclePreviewActTier(1, 0, true));
        assertEquals(3, MycelialCodexBookScreen.cyclePreviewActTier(2, 0, true));
        assertEquals(-1, MycelialCodexBookScreen.cyclePreviewActTier(3, 0, true));
    }

    @Test
    void codexBookDataFromJsonParsesDualRecipesEntityScaleAndCalloutAccent() {
        String json = """
                {
                  "book_id": "mycelialmind:mycelial_codex",
                  "title_key": "book.title",
                  "subtitle_key": "book.sub",
                  "tooltip_key": "book.tip",
                  "chapters": [
                    {
                      "id": "ch1",
                      "number": 1,
                      "title_key": "ch1.title",
                      "subtitle_key": "ch1.sub",
                      "icon_item": "mycelialmind:bio_scanner",
                      "unlock_gate": "always",
                      "entries": [
                        {
                          "id": "e1",
                          "order": 1,
                          "title_key": "e1.title",
                          "subtitle_key": "e1.sub",
                          "icon_item": "mycelialmind:mycelial_codex",
                          "unlock_gate": "act_2",
                          "pages": [
                            {
                              "page_number": 1,
                              "id": "p1",
                              "type": "recipe",
                              "title_key": "p1.title",
                              "text_key": "p1.text",
                              "is_spoiler": false,
                              "recipe_id": "mycelialmind:bio_scanner",
                              "recipe_id_2": "mycelialmind:mycelial_codex",
                              "title2_key": "p1.title2"
                            },
                            {
                              "page_number": 2,
                              "id": "p2",
                              "type": "entity",
                              "title_key": "p2.title",
                              "text_key": "p2.text",
                              "is_spoiler": true,
                              "entity_id": "mycelialmind:sporeling",
                              "entity_name_key": "entity.mycelialmind.sporeling",
                              "entity_scale": 1.15,
                              "accent_color": "#06D6A0"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
        CodexBookData data = CodexBookData.fromJson(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(1, data.chapters().size());
        CodexBookData.Entry entry = data.chapters().get(0).entries().get(0);
        assertEquals("act_2", entry.unlockGate());
        assertEquals(2, entry.pages().size());

        CodexBookData.Page p1 = entry.pages().get(0);
        assertFalse(p1.isSpoiler());
        assertEquals("mycelialmind:bio_scanner", p1.recipeId());
        assertEquals("mycelialmind:mycelial_codex", p1.recipeId2());
        assertEquals("p1.title2", p1.title2Key());

        CodexBookData.Page p2 = entry.pages().get(1);
        assertTrue(p2.isSpoiler());
        assertNotNull(p2.entityId());
        assertEquals("entity.mycelialmind.sporeling", p2.entityNameKey());
        assertEquals(1.15F, p2.entityScale(), 0.001F);
        assertEquals("#06D6A0", p2.accentColor());
    }
}
