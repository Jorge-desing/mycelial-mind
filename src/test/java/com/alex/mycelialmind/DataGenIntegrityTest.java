package com.alex.mycelialmind;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless structural integrity checks guarding the boundary between hand-maintained/Python-studio
 * resources under {@code src/main/resources} and NeoForge DataGen outputs under {@code src/generated/resources}.
 */
class DataGenIntegrityTest {
    private static final Path MAIN_RESOURCES = resolveRepoRoot().resolve("src/main/resources");
    private static final Path GENERATED_RESOURCES = resolveRepoRoot().resolve("src/generated/resources");

    @Test
    void zeroFileOverlapsBetweenMainAndGeneratedResources() throws IOException {
        Set<String> mainFiles = new HashSet<>();
        collectRelativeFiles(MAIN_RESOURCES, "data", mainFiles);
        collectRelativeFiles(MAIN_RESOURCES, "assets", mainFiles);

        Set<String> generatedFiles = new HashSet<>();
        collectRelativeFiles(GENERATED_RESOURCES, "data", generatedFiles);
        collectRelativeFiles(GENERATED_RESOURCES, "assets", generatedFiles);

        Set<String> overlaps = new TreeSet<>(mainFiles);
        overlaps.retainAll(generatedFiles);
        assertTrue(overlaps.isEmpty(), "Duplicate resource paths across main and generated trees: " + overlaps);
    }

    @Test
    void allBlockstatesReferenceExistingBlockModels() throws IOException {
        Map<String, Path> blockstates = collectJsonByName("assets/mycelialmind/blockstates");
        assertEquals(29, blockstates.size(), "Expected 29 blockstate files across main + generated resources");

        Set<String> blockModels = collectJsonByName("assets/mycelialmind/models/block").keySet();
        assertEquals(43, blockModels.size(), "Expected 43 static block model files in models/block");

        for (Map.Entry<String, Path> entry : blockstates.entrySet()) {
            JsonElement parsed = parseJson(entry.getValue());
            List<String> referencedModels = new ArrayList<>();
            collectModelStrings(parsed, referencedModels);
            assertFalse(referencedModels.isEmpty(), "Blockstate has no model references: " + entry.getKey());
            for (String modelRef : referencedModels) {
                assertTrue(modelRef.startsWith("mycelialmind:block/"),
                        "Unexpected model namespace in blockstate " + entry.getKey() + ": " + modelRef);
                String modelId = modelRef.substring("mycelialmind:block/".length());
                assertTrue(blockModels.contains(modelId),
                        "Blockstate " + entry.getKey() + " references missing block model: " + modelRef);
            }
        }
    }

    @Test
    void allAdvancementsHaveValidParentsAndBilingualLangKeys() throws IOException {
        Map<String, Path> advancements = collectJsonByName("data/mycelialmind/advancement");
        assertEquals(31, advancements.size(), "Expected 31 progression advancements across main + generated resources");

        JsonObject enLang = parseJson(MAIN_RESOURCES.resolve("assets/mycelialmind/lang/en_us.json")).getAsJsonObject();
        JsonObject esLang = parseJson(MAIN_RESOURCES.resolve("assets/mycelialmind/lang/es_es.json")).getAsJsonObject();

        for (Map.Entry<String, Path> entry : advancements.entrySet()) {
            JsonObject adv = parseJson(entry.getValue()).getAsJsonObject();
            if (adv.has("parent")) {
                String parent = adv.get("parent").getAsString();
                assertTrue(parent.startsWith("mycelialmind:"),
                        "Advancement " + entry.getKey() + " has non-mod parent: " + parent);
                String parentId = parent.substring("mycelialmind:".length());
                assertTrue(advancements.containsKey(parentId),
                        "Advancement " + entry.getKey() + " references missing parent: " + parent);
            }
            JsonObject display = adv.getAsJsonObject("display");
            assertNotNull(display, "Missing display object on advancement " + entry.getKey());
            for (String field : List.of("title", "description")) {
                String key = display.getAsJsonObject(field).get("translate").getAsString();
                assertTrue(enLang.has(key), "Missing en_us translation key " + key + " for " + entry.getKey());
                assertTrue(esLang.has(key), "Missing es_es translation key " + key + " for " + entry.getKey());
            }
            JsonObject criteria = adv.getAsJsonObject("criteria");
            assertNotNull(criteria, "Missing criteria object on advancement " + entry.getKey());
            assertFalse(criteria.isEmpty(), "Empty criteria object on advancement " + entry.getKey());
        }
    }

    @Test
    void pythonStudioOwnedAssetsStayExclusiveToMainResources() throws IOException {
        Path mainModAssets = MAIN_RESOURCES.resolve("assets/mycelialmind");
        Path genModAssets = GENERATED_RESOURCES.resolve("assets/mycelialmind");

        assertTrue(Files.isRegularFile(mainModAssets.resolve("sounds.json")));
        assertTrue(Files.isRegularFile(mainModAssets.resolve("lang/en_us.json")));
        assertTrue(Files.isRegularFile(mainModAssets.resolve("lang/es_es.json")));
        assertFalse(Files.exists(genModAssets.resolve("sounds.json")), "sounds.json must stay owned by sound_studio");
        assertFalse(Files.exists(genModAssets.resolve("lang")), "lang must stay owned by Python studios");
        assertFalse(Files.exists(genModAssets.resolve("particles")), "particles must stay owned by particle_studio");

        long particleJsonCount;
        try (Stream<Path> stream = Files.list(mainModAssets.resolve("particles"))) {
            particleJsonCount = stream.filter(p -> p.getFileName().toString().endsWith(".json")).count();
        }
        assertTrue(particleJsonCount >= 34, "Expected >= 34 particle descriptors in main resources");

        assertTrue(Files.isRegularFile(GENERATED_RESOURCES.resolve("assets/minecraft/atlases/blocks.json")),
                "Expected generated blocks atlas under src/generated/resources");
        assertFalse(Files.exists(MAIN_RESOURCES.resolve("assets/minecraft/atlases/blocks.json")),
                "Duplicate static blocks.json atlas found under src/main/resources");
    }

    private static Path resolveRepoRoot() {
        Path cursor = Path.of("").toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isRegularFile(cursor.resolve("build.gradle"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate repository root containing build.gradle");
    }

    private static void collectRelativeFiles(Path root, String subDir, Set<String> out) throws IOException {
        Path start = root.resolve(subDir);
        if (!Files.isDirectory(start)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(start)) {
            walk.filter(Files::isRegularFile)
                    .forEach(p -> out.add(root.relativize(p).toString().replace('\\', '/')));
        }
    }

    private static Map<String, Path> collectJsonByName(String relDir) throws IOException {
        Map<String, Path> result = new HashMap<>();
        for (Path base : List.of(MAIN_RESOURCES, GENERATED_RESOURCES)) {
            Path dir = base.resolve(relDir);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".json"))
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            result.put(name.substring(0, name.length() - ".json".length()), p);
                        });
            }
        }
        return result;
    }

    private static JsonElement parseJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static void collectModelStrings(JsonElement element, List<String> out) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("model") && obj.get("model").isJsonPrimitive()) {
                out.add(obj.get("model").getAsString());
            }
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (!"model".equals(entry.getKey())) {
                    collectModelStrings(entry.getValue(), out);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectModelStrings(child, out);
            }
        }
    }
}
