package com.alex.mycelialmind;

import com.alex.mycelialmind.mixin.EntityMixin;
import com.alex.mycelialmind.mixin.FoodDataMixin;
import com.alex.mycelialmind.mixin.LivingEntityAccessor;
import com.alex.mycelialmind.mixin.LivingEntityMixin;
import com.alex.mycelialmind.mixin.MobAccessor;
import com.alex.mycelialmind.mixin.MonsterMixin;
import com.alex.mycelialmind.mixin.client.SoundEngineAccessor;
import com.alex.mycelialmind.util.MixinRuntimeProbe;
import com.alex.mycelialmind.util.MycelialMixinHooks;
import com.alex.mycelialmind.util.MycelialMixinPolicy;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinInfrastructureTest {

    @Test
    void mixinConfigJsonDeclaresValidJava21Infrastructure() throws Exception {
        String raw = Files.readString(Path.of("src/main/resources/mycelialmind.mixins.json"));
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();

        assertTrue(root.get("required").getAsBoolean(), "mixins.json must set required=true");
        assertEquals("0.8", root.get("minVersion").getAsString());
        assertEquals("com.alex.mycelialmind.mixin", root.get("package").getAsString());
        assertEquals("JAVA_21", root.get("compatibilityLevel").getAsString());

        JsonArray mixins = root.getAsJsonArray("mixins");
        Set<String> declared = new HashSet<>();
        mixins.forEach(elem -> declared.add(elem.getAsString()));
        assertEquals(Set.of(
                "LivingEntityMixin",
                "EntityMixin",
                "MonsterMixin",
                "FoodDataMixin",
                "LivingEntityAccessor",
                "MobAccessor"), declared);

        JsonArray clientMixins = root.getAsJsonArray("client");
        Set<String> declaredClient = new HashSet<>();
        clientMixins.forEach(elem -> declaredClient.add(elem.getAsString()));
        assertEquals(Set.of("client.SoundEngineAccessor"), declaredClient);

        assertEquals(1, root.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        assertTrue(root.getAsJsonObject("overwrites").get("requireAnnotations").getAsBoolean());
    }

    @Test
    void modsTomlEnablesMycelialMixinConfig() throws Exception {
        String toml = Files.readString(Path.of("src/main/templates/META-INF/neoforge.mods.toml"));
        assertTrue(toml.contains("\n[[mixins]]") || toml.startsWith("[[mixins]]"),
                "neoforge.mods.toml must uncomment [[mixins]]");
        assertTrue(toml.contains("config=\"${mod_id}.mixins.json\""),
                "neoforge.mods.toml must declare mycelialmind.mixins.json");
    }

    @Test
    void mixinsAndAccessorsCarryExpectedAnnotationsAndHelpersStayOutsideMixinPackage() throws Exception {
        for (Class<?> helper : new Class<?>[]{MixinRuntimeProbe.class, MycelialMixinPolicy.class, MycelialMixinHooks.class}) {
            assertFalse(helper.getPackageName().startsWith("com.alex.mycelialmind.mixin"),
                    helper.getSimpleName() + " must live outside com.alex.mycelialmind.mixin.*");
        }

        Map<String, Set<String>> livingMethods = inspectMethodAnnotations(LivingEntityMixin.class);
        assertTrue(livingMethods.getOrDefault("mycelialmind$adjustVisibilityPercent", Set.of())
                .contains("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;"));
        assertTrue(livingMethods.getOrDefault("mycelialmind$softenMycelialFallDamage", Set.of())
                .contains("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;"));
        assertTrue(livingMethods.getOrDefault("mycelialmind$applyChitinFreezeImmunity", Set.of())
                .contains("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;"));
        assertTrue(livingMethods.getOrDefault("mycelialmind$applyChitinAirConservation", Set.of())
                .contains("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;"));

        Map<String, Set<String>> entityMethods = inspectMethodAnnotations(EntityMixin.class);
        assertTrue(entityMethods.getOrDefault("mycelialmind$applyMycelialTraction", Set.of())
                .contains("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;"));

        Map<String, Set<String>> monsterMethods = inspectMethodAnnotations(MonsterMixin.class);
        assertTrue(monsterMethods.getOrDefault("mycelialmind$filterNaturalMycelialSpawn", Set.of())
                .contains("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;"));
        assertTrue(monsterMethods.getOrDefault("mycelialmind$filterAnyLightMycelialSpawn", Set.of())
                .contains("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;"));

        Map<String, Set<String>> foodMethods = inspectMethodAnnotations(FoodDataMixin.class);
        assertTrue(foodMethods.getOrDefault("mycelialmind$scaleSporeRotNaturalHeal", Set.of())
                .contains("Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;"));

        Map<String, Set<String>> livingAccMethods = inspectMethodAnnotations(LivingEntityAccessor.class);
        assertTrue(livingAccMethods.getOrDefault("mycelialmind$getLastHurtByMobTimestamp", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Accessor;"));
        assertTrue(livingAccMethods.getOrDefault("mycelialmind$getLastHurtMobTimestamp", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Accessor;"));
        assertTrue(livingAccMethods.getOrDefault("mycelialmind$invokeGetBlockSpeedFactor", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Invoker;"));
        assertTrue(livingAccMethods.getOrDefault("mycelialmind$invokeDecreaseAirSupply", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Invoker;"));

        Map<String, Set<String>> mobAccMethods = inspectMethodAnnotations(MobAccessor.class);
        assertTrue(mobAccMethods.getOrDefault("mycelialmind$getGoalSelector", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Accessor;"));
        assertTrue(mobAccMethods.getOrDefault("mycelialmind$getTargetSelector", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Accessor;"));
        assertTrue(mobAccMethods.getOrDefault("mycelialmind$getSensing", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Accessor;"));
        assertTrue(mobAccMethods.getOrDefault("mycelialmind$invokeResetAmbientSoundTime", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Invoker;"));

        Map<String, Set<String>> soundAccMethods = inspectMethodAnnotations(SoundEngineAccessor.class);
        assertTrue(soundAccMethods.getOrDefault("mycelialmind$isLoaded", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Accessor;"));
        assertTrue(soundAccMethods.getOrDefault("mycelialmind$getInstanceToChannel", Set.of())
                .contains("Lorg/spongepowered/asm/mixin/gen/Accessor;"));
    }

    @Test
    void parchmentMappingsAreConfiguredForMinecraft1211() throws Exception {
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("gradle.properties"))) {
            props.load(reader);
        }
        assertEquals("1.21.1", props.getProperty("parchment_minecraft_version"));
        assertEquals("2024.11.17", props.getProperty("parchment_mappings_version"));

        String buildGradle = Files.readString(Path.of("build.gradle"));
        assertTrue(buildGradle.contains("parchment {"), "build.gradle must configure neoForge.parchment");
        assertTrue(buildGradle.contains("project.parchment_mappings_version"));
        assertTrue(buildGradle.contains("project.parchment_minecraft_version"));
    }

    @Test
    void pheromoneCamouflageRespectsCapAndAntiCheeseLockouts() {
        double stealth = MycelialMixinPolicy.computePheromoneVisibility(
                1.0D, true, 4, 4, false, false, false, false);
        assertEquals(0.56D, stealth, 1e-6);
        assertTrue(stealth >= MycelialMixinPolicy.MIN_VISIBILITY_FLOOR);

        // Sprinting, burning, infection/rot, active combat, or boss/non-mycelial observers disable camouflage.
        assertEquals(1.0D, MycelialMixinPolicy.computePheromoneVisibility(1.0D, true, 4, 4, true, false, false, false), 1e-6);
        assertEquals(1.0D, MycelialMixinPolicy.computePheromoneVisibility(1.0D, true, 4, 4, false, true, false, false), 1e-6);
        assertEquals(1.0D, MycelialMixinPolicy.computePheromoneVisibility(1.0D, true, 4, 4, false, false, true, false), 1e-6);
        assertEquals(1.0D, MycelialMixinPolicy.computePheromoneVisibility(1.0D, true, 4, 4, false, false, false, true), 1e-6);
        assertEquals(1.0D, MycelialMixinPolicy.computePheromoneVisibility(1.0D, false, 4, 4, false, false, false, false), 1e-6);
    }

    @Test
    void combatLockoutWindowExpiresAfterHundredTicks() {
        assertTrue(MycelialMixinPolicy.isRecentlyInCombat(150, 120, 0));
        assertTrue(MycelialMixinPolicy.isRecentlyInCombat(150, 0, 80));
        assertFalse(MycelialMixinPolicy.isRecentlyInCombat(250, 120, 80));
    }

    @Test
    void mycelialTractionIsBoundedAndIgnoredInAirOrLiquid() {
        assertEquals(1.08F, MycelialMixinPolicy.computeMycelialSpeedFactor(1.0F, true, true, false, false, false), 1e-5);
        assertEquals(1.10F, MycelialMixinPolicy.computeMycelialSpeedFactor(1.0F, true, false, true, false, false), 1e-5);
        assertEquals(1.06F, MycelialMixinPolicy.computeMycelialSpeedFactor(1.0F, true, false, false, true, false), 1e-5);
        // Anti-cheese: airborne, liquid, or already >= cap returns unchanged speed factor.
        assertEquals(1.0F, MycelialMixinPolicy.computeMycelialSpeedFactor(1.0F, true, true, true, true, true), 1e-5);
        assertEquals(1.0F, MycelialMixinPolicy.computeMycelialSpeedFactor(1.0F, false, true, true, true, false), 1e-5);
        assertEquals(1.25F, MycelialMixinPolicy.computeMycelialSpeedFactor(1.25F, true, false, true, false, false), 1e-5);
    }

    @Test
    void mycelialFallCushionReducesModerateFallsWithoutPreventingHighFallDamage() {
        // Moderate fall (8 blocks, 5 damage) -> -30% bare (=3), -45% with Chitin boots (=2).
        assertEquals(3, MycelialMixinPolicy.computeMycelialFallDamage(5, 8.0F, true, false));
        assertEquals(2, MycelialMixinPolicy.computeMycelialFallDamage(5, 8.0F, true, true));

        // High fall anti-MLG (>= 14 blocks): retains at least 50% damage and never becomes 0.
        int highFallWithBoots = MycelialMixinPolicy.computeMycelialFallDamage(17, 20.0F, true, true);
        assertEquals(9, highFallWithBoots);
        assertEquals(1, MycelialMixinPolicy.computeMycelialFallDamage(1, 16.0F, true, true));
        assertEquals(10, MycelialMixinPolicy.computeMycelialFallDamage(10, 15.0F, false, true));
    }

    @Test
    void naturalSpawnFilterRespectsVanillaMobsAndNonNaturalSpawnsInsidePylons() {
        assertFalse(MycelialMixinPolicy.allowMycelialSpawn(true, true, true, true));
        assertTrue(MycelialMixinPolicy.allowMycelialSpawn(true, false, true, true)); // vanilla mob farms untouched
        assertTrue(MycelialMixinPolicy.allowMycelialSpawn(true, true, false, true)); // spawner/spawn egg untouched
        assertTrue(MycelialMixinPolicy.allowMycelialSpawn(true, true, true, false)); // outside pylon untouched
        assertFalse(MycelialMixinPolicy.allowMycelialSpawn(false, true, true, false)); // respects vanilla denial
    }

    @Test
    void sporeRotFoodRegenPenaltyHalvesHealOutsidePylonOnly() {
        assertEquals(0.5F, MycelialMixinPolicy.computeNaturalFoodHeal(1.0F, true, false), 1e-6F);
        assertEquals(1.0F, MycelialMixinPolicy.computeNaturalFoodHeal(1.0F, true, true), 1e-6F);
        assertEquals(1.0F, MycelialMixinPolicy.computeNaturalFoodHeal(1.0F, false, false), 1e-6F);
        assertEquals(0.0F, MycelialMixinPolicy.computeNaturalFoodHeal(0.0F, true, false), 1e-6F);
    }

    @Test
    void evolvedChitinGrantsFreezeImmunityUnlessSporeRotted() {
        assertFalse(MycelialMixinPolicy.computeCanFreeze(true, 2, false));
        assertTrue(MycelialMixinPolicy.computeCanFreeze(true, 1, false));
        assertTrue(MycelialMixinPolicy.computeCanFreeze(true, 4, true)); // SPORE_ROT breaks immunity
        assertFalse(MycelialMixinPolicy.computeCanFreeze(false, 0, true));
    }

    @Test
    void chitinHelmetConservesAirSupplyWithinBoundsUnlessSporeRotted() {
        // MUTATED saves below 0.25, ELITE saves below 0.40, never saves if roll >= chance or SPORE_ROT active.
        assertEquals(100, MycelialMixinPolicy.computeConservedAirSupply(100, 99, 1, false, 0.20D));
        assertEquals(99, MycelialMixinPolicy.computeConservedAirSupply(100, 99, 1, false, 0.26D));
        assertEquals(100, MycelialMixinPolicy.computeConservedAirSupply(100, 99, 2, false, 0.35D));
        assertEquals(99, MycelialMixinPolicy.computeConservedAirSupply(100, 99, 2, false, 0.45D));
        assertEquals(99, MycelialMixinPolicy.computeConservedAirSupply(100, 99, 2, true, 0.05D));
        assertEquals(0, MycelialMixinPolicy.computeConservedAirSupply(0, 0, 2, false, 0.05D));
    }

    private static Map<String, Set<String>> inspectMethodAnnotations(Class<?> clazz) throws Exception {
        Map<String, Set<String>> methodAnnotations = new HashMap<>();
        try (InputStream stream = clazz.getResourceAsStream(clazz.getSimpleName() + ".class")) {
            assertNotNull(stream, "Compiled class must exist on test classpath: " + clazz.getName());
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    Set<String> annotations = methodAnnotations.computeIfAbsent(name, ignored -> new HashSet<>());
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            annotations.add(desc);
                            return null;
                        }
                    };
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return methodAnnotations;
    }
}
