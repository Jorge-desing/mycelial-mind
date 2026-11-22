package com.alex.mycelialmind.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable data model for the standalone native Mycelial Codex loaded from
 * data/mycelialmind/codex/codex_book.json.
 */
public record CodexBookData(
        String bookId,
        String titleKey,
        String subtitleKey,
        String tooltipKey,
        List<Chapter> chapters
) {
    public record Chapter(
            String id,
            int number,
            String titleKey,
            String subtitleKey,
            String iconItem,
            String unlockGate,
            List<Entry> entries
    ) {
    }

    public record Entry(
            String id,
            int order,
            String titleKey,
            String subtitleKey,
            String iconItem,
            String unlockGate,
            List<Page> pages
    ) {
    }

    public record Page(
            int pageNumber,
            String id,
            String type,
            String titleKey,
            String textKey,
            boolean isSpoiler,
            @Nullable String itemId,
            @Nullable String recipeId,
            @Nullable String recipeId2,
            @Nullable String title2Key,
            @Nullable String entityId,
            @Nullable String entityNameKey,
            float entityScale,
            @Nullable String accentColor
    ) {
    }

    public static CodexBookData fromJson(JsonObject root) {
        String bookId = getStr(root, "book_id", "mycelialmind:mycelial_codex");
        String titleKey = getStr(root, "title_key", "book.mycelialmind.mycelial_codex.name");
        String subtitleKey = getStr(root, "subtitle_key", "book.mycelialmind.mycelial_codex.subtitle");
        String tooltipKey = getStr(root, "tooltip_key", "book.mycelialmind.mycelial_codex.tooltip");

        List<Chapter> chapters = new ArrayList<>();
        JsonArray chArr = root.getAsJsonArray("chapters");
        if (chArr != null) {
            for (JsonElement chEl : chArr) {
                if (!chEl.isJsonObject()) continue;
                JsonObject chObj = chEl.getAsJsonObject();
                List<Entry> entries = new ArrayList<>();
                JsonArray eArr = chObj.getAsJsonArray("entries");
                if (eArr != null) {
                    for (JsonElement eEl : eArr) {
                        if (!eEl.isJsonObject()) continue;
                        JsonObject eObj = eEl.getAsJsonObject();
                        List<Page> pages = new ArrayList<>();
                        JsonArray pArr = eObj.getAsJsonArray("pages");
                        if (pArr != null) {
                            for (JsonElement pEl : pArr) {
                                if (!pEl.isJsonObject()) continue;
                                JsonObject pObj = pEl.getAsJsonObject();
                                pages.add(new Page(
                                        getInt(pObj, "page_number", pages.size() + 1),
                                        getStr(pObj, "id", ""),
                                        getStr(pObj, "type", "text"),
                                        getStr(pObj, "title_key", ""),
                                        getStr(pObj, "text_key", ""),
                                        getBool(pObj, "is_spoiler", false),
                                        getOptStr(pObj, "item_id"),
                                        getOptStr(pObj, "recipe_id"),
                                        getOptStr(pObj, "recipe_id_2"),
                                        getOptStr(pObj, "title2_key"),
                                        getOptStr(pObj, "entity_id"),
                                        getOptStr(pObj, "entity_name_key"),
                                        getFloat(pObj, "entity_scale", 0.85F),
                                        getOptStr(pObj, "accent_color")
                                ));
                            }
                        }
                        entries.add(new Entry(
                                getStr(eObj, "id", ""),
                                getInt(eObj, "order", entries.size() + 1),
                                getStr(eObj, "title_key", ""),
                                getStr(eObj, "subtitle_key", ""),
                                getStr(eObj, "icon_item", "mycelialmind:bio_scanner"),
                                getStr(eObj, "unlock_gate", "always"),
                                List.copyOf(pages)
                        ));
                    }
                }
                chapters.add(new Chapter(
                        getStr(chObj, "id", ""),
                        getInt(chObj, "number", chapters.size() + 1),
                        getStr(chObj, "title_key", ""),
                        getStr(chObj, "subtitle_key", ""),
                        getStr(chObj, "icon_item", "mycelialmind:bio_scanner"),
                        getStr(chObj, "unlock_gate", "always"),
                        List.copyOf(entries)
                ));
            }
        }
        return new CodexBookData(bookId, titleKey, subtitleKey, tooltipKey, List.copyOf(chapters));
    }

    private static String getStr(JsonObject obj, String key, String def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : def;
    }

    @Nullable
    private static String getOptStr(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : def;
    }

    private static float getFloat(JsonObject obj, String key, float def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsFloat() : def;
    }

    private static boolean getBool(JsonObject obj, String key, boolean def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsBoolean() : def;
    }
}
