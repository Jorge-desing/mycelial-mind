package com.alex.mycelialmind.data;

import com.alex.mycelialmind.MycelialMind;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads data/mycelialmind/codex/codex_book.json via NeoForge SimpleJsonResourceReloadListener
 * with classpath fallback for integrated/client-side GUI access.
 */
public final class CodexReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation BOOK_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "codex_book");

    @Nullable
    private static volatile CodexBookData cachedBook;

    public CodexReloadListener() {
        super(GSON, "codex");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CodexReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement el = objects.get(BOOK_ID);
        if (el != null && el.isJsonObject()) {
            cachedBook = CodexBookData.fromJson(el.getAsJsonObject());
        }
    }

    @Nullable
    public static CodexBookData getBook() {
        if (cachedBook == null) {
            cachedBook = loadFromClasspath();
        }
        return cachedBook;
    }

    @Nullable
    private static CodexBookData loadFromClasspath() {
        String path = "/data/" + MycelialMind.MODID + "/codex/codex_book.json";
        try (InputStream is = CodexReloadListener.class.getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonElement el = JsonParser.parseReader(reader);
                if (el != null && el.isJsonObject()) {
                    return CodexBookData.fromJson(el.getAsJsonObject());
                }
            }
        } catch (Exception ex) {
            MycelialMind.LOGGER.warn("Failed to load fallback codex_book.json from classpath", ex);
        }
        return null;
    }
}
