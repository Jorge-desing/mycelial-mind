package com.alex.mycelialmind;

import com.alex.mycelialmind.compat.claims.ClaimProtectionCompat;
import com.alex.mycelialmind.compat.combat.ModdedDamageClassifier;
import com.alex.mycelialmind.hive.StimulusType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless unit and data-integrity tests for optional mod compatibility integrations.
 */
class ModCompatTest {
    private static final Path REPO_ROOT = resolveRepoRoot();
    private static final Path MAIN_DATA = REPO_ROOT.resolve("src/main/resources/data/mycelialmind");
    private static final Path REGISTRY_ROOT = REPO_ROOT.resolve("src/main/java/com/alex/mycelialmind/registry");
    private static final Pattern REGISTER_BLOCK_PATTERN = Pattern.compile("registerBlock\\(\"([a-z0-9_]+)\"");
    private static final Pattern REGISTER_ITEM_PATTERN = Pattern.compile("ITEMS\\.register\\(\"([a-z0-9_]+)\"");

    @Test
    void classifiesArsNouveauAndIronsSpellsDamageTypes() {
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("ars_nouveau:spell")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("ars_nouveau:wind_shear")));
        assertEquals(StimulusType.FIRE,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("ars_nouveau:flare")));

        assertEquals(StimulusType.FIRE,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("irons_spellbooks:fire_magic")));
        assertEquals(StimulusType.FIRE,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("irons_spellbooks:firebolt")));
        assertEquals(StimulusType.PROJECTILE,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("irons_spellbooks:icicle")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("irons_spellbooks:blood_magic")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("irons_spellbooks:eldritch_magic")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("irons_spellbooks:holy_magic")));
    }

    @Test
    void classifiesGenericModdedDamageByPrecedenceAndIgnoresVanilla() {
        assertEquals(StimulusType.EXPLOSION,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("other_magic:arcane_blast")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("other_magic:mana_surge")));

        assertNull(ModdedDamageClassifier.classifyModdedDamageType(null));
        assertNull(ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("minecraft:magic")));
        assertNull(ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("mycelialmind:acid")));
        assertNull(ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("other_mod:sword_slash")));
    }

    @Test
    void claimProtectionCompatFailsSafeWithoutServerOrLoadedMods() {
        assertFalse(ClaimProtectionCompat.isClaimProtected(null, null));
        assertFalse(ClaimProtectionCompat.isClaimProtected(null, BlockPos.ZERO));
    }

    @Test
    void allCompatRecipesHaveModLoadedConditionsAndValidModItemIds() throws IOException {
        Set<String> validModIds = new TreeSet<>();
        validModIds.addAll(extractMatches(REGISTRY_ROOT.resolve("ModBlocks.java"), REGISTER_BLOCK_PATTERN));
        validModIds.addAll(extractMatches(REGISTRY_ROOT.resolve("ModItems.java"), REGISTER_ITEM_PATTERN));

        Path compatRoot = MAIN_DATA.resolve("recipe/compat");
        assertTrue(Files.isDirectory(compatRoot), "Missing recipe/compat directory");

        List<Path> recipeFiles;
        try (Stream<Path> walk = Files.walk(compatRoot)) {
            recipeFiles = walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .toList();
        }
        assertTrue(recipeFiles.size() >= 11, "Expected >= 11 compat recipe JSON files, found " + recipeFiles.size());

        for (Path recipeFile : recipeFiles) {
            String expectedModId = compatRoot.relativize(recipeFile).getName(0).toString();
            String rawJson = Files.readString(recipeFile, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();

            assertTrue(obj.has("type"), "Missing recipe type in " + recipeFile);
            assertTrue(obj.get("type").getAsString().startsWith(expectedModId + ":"),
                    "Recipe type should match compat folder modid in " + recipeFile);

            JsonArray conditions = obj.getAsJsonArray("neoforge:conditions");
            assertNotNull(conditions, "Missing neoforge:conditions in " + recipeFile);
            assertFalse(conditions.isEmpty(), "Empty neoforge:conditions in " + recipeFile);
            JsonObject firstCond = conditions.get(0).getAsJsonObject();
            assertEquals("neoforge:mod_loaded", firstCond.get("type").getAsString());
            assertEquals(expectedModId, firstCond.get("modid").getAsString());

            Matcher modItemMatcher = Pattern.compile("\"mycelialmind:([a-z0-9_]+)\"").matcher(rawJson);
            while (modItemMatcher.find()) {
                String itemId = modItemMatcher.group(1);
                assertTrue(validModIds.contains(itemId),
                        "Compat recipe " + recipeFile + " references unknown mod item mycelialmind:" + itemId);
            }
        }
    }

    @Test
    void betterCombatWeaponAttributesParseCleanly() throws IOException {
        Path weaponAttrDir = MAIN_DATA.resolve("weapon_attributes");
        for (String weapon : List.of("enzyme_lance.json", "symbiotic_blade.json")) {
            Path file = weaponAttrDir.resolve(weapon);
            assertTrue(Files.isRegularFile(file), "Missing Better Combat attributes file: " + file);
            JsonObject obj = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            String parent = obj.get("parent").getAsString();
            assertTrue(parent.startsWith("bettercombat:"), "Expected bettercombat parent in " + file);
        }
    }

    private static Set<String> extractMatches(Path sourceFile, Pattern pattern) throws IOException {
        Matcher matcher = pattern.matcher(Files.readString(sourceFile, StandardCharsets.UTF_8));
        Set<String> matches = new TreeSet<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
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
}
