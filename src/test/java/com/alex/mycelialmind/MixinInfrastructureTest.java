package com.alex.mycelialmind;

import com.alex.mycelialmind.mixin.LivingEntityMixin;
import com.alex.mycelialmind.util.MixinRuntimeProbe;
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
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

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
        assertTrue(mixins.size() >= 1);
        assertEquals("LivingEntityMixin", mixins.get(0).getAsString());

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
    void livingEntityMixinUsesMixinExtrasModifyReturnValue() throws Exception {
        assertFalse(MixinRuntimeProbe.class.getPackageName().startsWith("com.alex.mycelialmind.mixin"),
                "Runtime helper classes must live outside com.alex.mycelialmind.mixin.* to avoid Mixin IllegalClassLoadError");

        AtomicBoolean hasMixinAnnotation = new AtomicBoolean(false);
        AtomicBoolean hasModifyReturnValue = new AtomicBoolean(false);
        try (InputStream stream = LivingEntityMixin.class.getResourceAsStream("LivingEntityMixin.class")) {
            assertNotNull(stream, "Compiled LivingEntityMixin.class must exist on test classpath");
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) {
                        hasMixinAnnotation.set(true);
                    }
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if ("mycelialmind$probeVisibilityPercent".equals(name)) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                if ("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;".equals(desc)) {
                                    hasModifyReturnValue.set(true);
                                }
                                return null;
                            }
                        };
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }

        assertTrue(hasMixinAnnotation.get(), "LivingEntityMixin bytecode must carry @Mixin");
        assertTrue(hasModifyReturnValue.get(), "LivingEntityMixin hook must carry MixinExtras @ModifyReturnValue");
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
}
