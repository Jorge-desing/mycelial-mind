package com.alex.mycelialmind;

import com.alex.mycelialmind.compat.claims.ClaimProtectionCompat;
import com.alex.mycelialmind.compat.combat.ModdedDamageClassifier;
import com.alex.mycelialmind.compat.curios.MycelialCuriosCompat;
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
import java.util.HashSet;
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
    private static final Set<String> RESTRICTED_BOSS_OUTPUTS = Set.of(
            "mycelialmind:hive_heart",
            "mycelialmind:overmind_essence",
            "mycelialmind:synaptic_core"
    );

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
    void classifiesWave2ModdedNamespacesAndEnergyKeywords() {
        for (String ns : List.of("botania", "malum", "occultism", "cataclysm", "twilightforest",
                "mahoutsukai", "eidolon", "forbidden_arcanus", "bloodmagic")) {
            assertEquals(StimulusType.MAGIC,
                    ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse(ns + ":ritual_damage")));
        }
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("mekanism:radiation")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("custom_mod:laser_beam")));
        assertEquals(StimulusType.FIRE,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("custom_mod:laser_burn")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("custom_mod:void_rift")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("custom_mod:sonic_boom")));
        assertEquals(StimulusType.MAGIC,
                ModdedDamageClassifier.classifyModdedDamageType(ResourceLocation.parse("custom_mod:lightning_strike")));
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
    void curiosCompatHelpersScalePassivesAndFailSafeWithoutPlayer() {
        assertFalse(MycelialCuriosCompat.hasEquippedScanner(null));
        assertFalse(MycelialCuriosCompat.hasEquippedOvermindCrown(null));
        assertFalse(MycelialCuriosCompat.hasEquippedQuorumSeal(null));

        assertEquals(100, MycelialCuriosCompat.applyOvermindCrownInfectionReduction(100, false));
        assertEquals(65, MycelialCuriosCompat.applyOvermindCrownInfectionReduction(100, true));
        assertEquals(20, MycelialCuriosCompat.applyOvermindCrownInfectionReduction(25, true));

        assertEquals(4.0F, MycelialCuriosCompat.applyOvermindCrownAcidRainMitigation(4.0F, false), 1e-4F);
        assertEquals(2.8F, MycelialCuriosCompat.applyOvermindCrownAcidRainMitigation(4.0F, true), 1e-4F);

        assertEquals(12, MycelialCuriosCompat.applyQuorumSealReputationBonus(12, false));
        assertEquals(14, MycelialCuriosCompat.applyQuorumSealReputationBonus(12, true));
        assertEquals(3, MycelialCuriosCompat.applyQuorumSealReputationBonus(2, true));
        assertEquals(-6, MycelialCuriosCompat.applyQuorumSealReputationBonus(-6, true));

        assertEquals(600, MycelialCuriosCompat.applyQuorumSealBetrayalCooldown(600, false));
        assertEquals(300, MycelialCuriosCompat.applyQuorumSealBetrayalCooldown(600, true));
    }

    @Test
    void curiosPlayerSlotsDeclareBeltHeadAndCharm() throws IOException {
        Path playerJson = MAIN_DATA.resolve("curios/entities/player.json");
        assertTrue(Files.isRegularFile(playerJson), "Missing curios/entities/player.json");
        JsonObject obj = JsonParser.parseString(Files.readString(playerJson, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> slots = new HashSet<>();
        for (JsonElement el : obj.getAsJsonArray("slots")) {
            slots.add(el.getAsString());
        }
        assertTrue(slots.containsAll(List.of("belt", "head", "charm")),
                "Expected belt, head, and charm Curios slots, found: " + slots);
    }

    @Test
    void allCompatRecipesHaveModLoadedConditionsValidModItemIdsAndRespectActGating() throws IOException {
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
        assertTrue(recipeFiles.size() >= 25, "Expected >= 25 compat recipe JSON files, found " + recipeFiles.size());

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
            for (String restricted : RESTRICTED_BOSS_OUTPUTS) {
                assertFalse(rawJson.contains("\"" + restricted + "\""),
                        "Compat recipe " + recipeFile + " must not reference restricted progression item " + restricted);
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
        return TestRepoPaths.resolveRepoRoot();
    }
}
