package com.alex.mycelialmind.client;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModFeatures;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SurfaceHiveFeature;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.alex.mycelialmind.world.structure.MatriarchCitadelPiece;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Optional automated in-game visual smoke harness for Mycelial Mind structures,
 * active only when -Dmycelialmind.structureVisualSmoke=true.
 * Generates 4 structure showcases in 3 spatially isolated zones (>= 240 blocks apart)
 * and captures 5 deterministic screenshots (3 exterior + 2 interior) plus metadata JSON.
 */
public final class StructureVisualSmokeClient {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int WATCHDOG_MAX_TICKS = 1200;

    private record ShotSpec(
            String filename,
            double xOffset,
            double yOffset,
            double zOffset,
            float yaw,
            float pitch,
            int settleTicks,
            BlockPos anchorOffset,
            BlockPos horizonAnchorOffset
    ) {
    }

    private static final List<ShotSpec> SHOTS = List.of(
            new ShotSpec("structure_outbreak_and_hive.png", 0.0, 12.5, -13.0, 0.0F, 13.0F, 12,
                    new BlockPos(9, 0, 16), new BlockPos(0, -1, 100)),
            new ShotSpec("structure_hive_sanctum.png", 229.5, 16.5, -10.5, -45.0F, 16.0F, 16,
                    new BlockPos(251, 3, 11), new BlockPos(310, -1, 70)),
            new ShotSpec("structure_sanctum_interior.png", 251.5, 5.5, 17.5, 180.0F, 14.0F, 10,
                    new BlockPos(251, 3, 11), null),
            new ShotSpec("structure_matriarch_citadel.png", -224.0, 30.0, 64.0, 166.0F, 12.0F, 16,
                    new BlockPos(-240, 17, 0), new BlockPos(-260, -1, -50)),
            new ShotSpec("structure_citadel_interior.png", -239.5, 19.5, 16.0, 180.0F, 6.0F, 10,
                    new BlockPos(-240, 17, 0), null)
    );

    private static boolean worldRequested = false;
    private static boolean structuresBuilt = false;
    private static volatile boolean structuresReady = false;
    private static int inWorldTicks = 0;
    private static int baseSurfaceY = -60;

    private static int currentShotIndex = 0;
    private static boolean shotMoved = false;
    private static int shotWaitTicks = 0;
    private static boolean stopping = false;

    private static volatile boolean outbreakPlaced = false;
    private static volatile boolean hivePlaced = false;
    private static volatile boolean sanctumPlaced = false;
    private static volatile boolean citadelPlaced = false;
    private static volatile int frozenGuardians = 0;
    private static final List<String> capturedShots = new ArrayList<>();

    private StructureVisualSmokeClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
            worldRequested = true;
            MycelialMind.LOGGER.info("StructureVisualSmoke: creating flat Creative world from {}", mc.screen.getClass().getSimpleName());
            LevelSettings settings = new LevelSettings(
                    "structure_visual_smoke",
                    GameType.CREATIVE,
                    false,
                    Difficulty.NORMAL,
                    true,
                    new GameRules(),
                    WorldDataConfiguration.DEFAULT
            );
            mc.createWorldOpenFlows().createFreshLevel(
                    "structure_visual_smoke_" + System.currentTimeMillis(),
                    settings,
                    new WorldOptions(481516L, false, false),
                    reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen
            );
            return;
        }

        if (stopping || mc.level == null || mc.player == null) {
            return;
        }

        inWorldTicks++;
        if (inWorldTicks > WATCHDOG_MAX_TICKS) {
            stopping = true;
            MycelialMind.LOGGER.error("StructureVisualSmoke WATCHDOG TIMEOUT at tick {} (shotIndex={}).", inWorldTicks, currentShotIndex);
            writeMetadata(mc, true);
            mc.stop();
            return;
        }

        IntegratedServer server = mc.getSingleplayerServer();
        UUID playerId = mc.player.getUUID();

        if (!structuresBuilt && inWorldTicks >= 5 && server != null) {
            structuresBuilt = true;
            mc.options.hideGui = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.cloudStatus().set(CloudStatus.OFF);
            mc.options.bobView().set(false);
            mc.options.renderDistance().set(12);
            mc.options.fov().set(70);
            server.execute(() -> {
                ServerLevel level = server.overworld();
                configureDeterministicWorld(level, server);
                discardFlatWorldSlimes(level, null);
                baseSurfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
                buildAllStructureShowcases(level, baseSurfaceY);
                ShotSpec first = SHOTS.get(0);
                var player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    positionSpectator(player, level, first);
                }
                structuresReady = true;
            });
            return;
        }

        if (!structuresReady) {
            return;
        }

        if (currentShotIndex >= SHOTS.size()) {
            shotWaitTicks++;
            if (shotWaitTicks >= 4) {
                stopping = true;
                writeMetadata(mc, false);
                MycelialMind.LOGGER.info("StructureVisualSmoke COMPLETED ({} structure screenshots captured).", capturedShots.size());
                mc.stop();
            }
            return;
        }

        ShotSpec spec = SHOTS.get(currentShotIndex);
        if (!shotMoved) {
            shotMoved = true;
            shotWaitTicks = 0;
            if (currentShotIndex > 0 && server != null) {
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    discardFlatWorldSlimes(level, new BlockPos((int) spec.xOffset(), baseSurfaceY, (int) spec.zOffset()));
                    var player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        positionSpectator(player, level, spec);
                    }
                });
            }
            return;
        }

        // Wait until the client player teleport, structure section, and far horizon flanks are compiled for this zone
        if (mc.player.distanceToSqr(spec.xOffset(), baseSurfaceY + spec.yOffset(), spec.zOffset()) > 4.0) {
            setClientCamera(mc, spec.yaw(), spec.pitch());
            return;
        }
        BlockPos clientAnchor = spec.anchorOffset().offset(0, baseSurfaceY, 0);
        if (mc.level.isEmptyBlock(clientAnchor) || !mc.levelRenderer.isSectionCompiled(clientAnchor)) {
            setClientCamera(mc, spec.yaw(), spec.pitch());
            return;
        }
        if (spec.horizonAnchorOffset() != null) {
            BlockPos horizonAnchor = spec.horizonAnchorOffset().offset(0, baseSurfaceY, 0);
            if (mc.level.isEmptyBlock(horizonAnchor)
                    || !mc.levelRenderer.isSectionCompiled(horizonAnchor)
                    || !areHorizonFlanksCompiled(mc, spec)) {
                setClientCamera(mc, spec.yaw(), spec.pitch());
                return;
            }
        }

        shotWaitTicks++;
        setClientCamera(mc, spec.yaw(), spec.pitch());
        if (mc.level != null && shotWaitTicks >= Math.max(1, spec.settleTicks() - 2)) {
            mc.particleEngine.setLevel(mc.level);
        }
        if (shotWaitTicks >= spec.settleTicks()) {
            setClientCamera(mc, spec.yaw(), spec.pitch());
            capturedShots.add(spec.filename());
            Screenshot.grab(mc.gameDirectory, spec.filename(), mc.getMainRenderTarget(), msg ->
                    MycelialMind.LOGGER.info("StructureVisualSmoke screenshot: {}", msg.getString()));
            currentShotIndex++;
            shotMoved = false;
            shotWaitTicks = 0;
        }
    }

    private static boolean areHorizonFlanksCompiled(Minecraft mc, ShotSpec spec) {
        if (mc.level == null) {
            return false;
        }
        double[] yawOffsets = {-36.0, 36.0};
        double[] distances = {128.0, 168.0};
        for (double yawOffsetDeg : yawOffsets) {
            double rad = Math.toRadians(spec.yaw() + yawOffsetDeg);
            for (double dist : distances) {
                int px = (int) Math.round(spec.xOffset() + (-Math.sin(rad)) * dist);
                int pz = (int) Math.round(spec.zOffset() + Math.cos(rad) * dist);
                BlockPos probe = new BlockPos(px, baseSurfaceY - 1, pz);
                if (!mc.level.hasChunk(probe.getX() >> 4, probe.getZ() >> 4)
                        || mc.level.isEmptyBlock(probe)
                        || !mc.levelRenderer.isSectionCompiled(probe)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void configureDeterministicWorld(ServerLevel level, IntegratedServer server) {
        GameRules rules = level.getGameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        level.setDayTime(6000);
        level.setWeatherParameters(100000, 0, false, false);
    }

    private static void discardFlatWorldSlimes(ServerLevel level, BlockPos nearCenter) {
        if (nearCenter == null) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof Mob mob) {
                    mob.discard();
                }
            }
            return;
        }
        AABB box = new AABB(nearCenter).inflate(96.0);
        for (Slime slime : level.getEntitiesOfClass(Slime.class, box)) {
            slime.discard();
        }
    }

    private static void positionSpectator(net.minecraft.server.level.ServerPlayer player, ServerLevel level, ShotSpec spec) {
        player.setGameMode(GameType.SPECTATOR);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.teleportTo(level, spec.xOffset(), baseSurfaceY + spec.yOffset(), spec.zOffset(), spec.yaw(), spec.pitch());
    }

    private static void setClientCamera(Minecraft mc, float yaw, float pitch) {
        if (mc.player != null) {
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
        }
    }

    private static void buildAllStructureShowcases(ServerLevel level, int y) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        // Zone A (x = 0, z = 16): MycelialOutbreakFeature + SurfaceHiveFeature side-by-side
        BlockPos outbreakPos = new BlockPos(-13, y, 16);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlock(outbreakPos.below().offset(dx, 0, dz), Blocks.DIRT.defaultBlockState(), 18);
            }
        }
        outbreakPlaced = ModFeatures.MYCELIAL_OUTBREAK.get().place(new FeaturePlaceContext<>(
                Optional.empty(),
                level,
                level.getChunkSource().getGenerator(),
                RandomSource.create(0x4D594301L),
                outbreakPos.below(),
                NoneFeatureConfiguration.INSTANCE
        ));
        // Clear outbreak tracker nodes so SurfaceHiveFeature's minNodeClearance never rejects the side-by-side hive
        for (Map.Entry<BlockPos, SynapticNodeType> entry : tracker.getNodesWithin(outbreakPos, 16, 32)) {
            tracker.removeNode(entry.getKey());
        }

        BlockPos hivePos = new BlockPos(9, y, 16);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.setBlock(hivePos.below().offset(dx, 0, dz), Blocks.DIRT.defaultBlockState(), 18);
            }
        }
        hivePlaced = SurfaceHiveFeature.generateHiveAt(level, hivePos, RandomSource.create(0x4D594302L));

        // Zone B (x = +240, z = 0): hive_sanctum Jigsaw atrium template (23x18x23)
        for (int cx = 13; cx <= 17; cx++) {
            for (int cz = -2; cz <= 3; cz++) {
                level.getChunk(cx, cz);
            }
        }
        ResourceLocation atriumId = ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sanctum/sanctum_atrium");
        level.getStructureManager().get(atriumId).ifPresent(template -> {
            StructurePlaceSettings placeSettings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setIgnoreEntities(true);
            BlockPos atriumOrigin = new BlockPos(240, y, 0);
            sanctumPlaced = template.placeInWorld(level, atriumOrigin, atriumOrigin, placeSettings, RandomSource.create(0x4D594303L), 2);
        });

        // Zone C (x = -240, z = 0): MatriarchCitadelPiece procedural 53x50x53 Imperial Fungal Citadel
        BlockPos citadelCenter = new BlockPos(-240, y + 13, 0);
        int citChunkX = citadelCenter.getX() >> 4;
        for (int cx = citChunkX - 3; cx <= citChunkX + 3; cx++) {
            for (int cz = -3; cz <= 5; cz++) {
                level.getChunk(cx, cz);
            }
        }
        MatriarchCitadelPiece citadel = new MatriarchCitadelPiece(citadelCenter);
        BoundingBox fullBox = new BoundingBox(
                citadelCenter.getX() - 26, citadelCenter.getY() - 20, citadelCenter.getZ() - 26,
                citadelCenter.getX() + 26, citadelCenter.getY() + 38, citadelCenter.getZ() + 26
        );
        citadel.postProcess(
                level,
                level.structureManager(),
                level.getChunkSource().getGenerator(),
                RandomSource.create(0x4D594304L),
                fullBox,
                new ChunkPos(citadelCenter),
                citadelCenter
        );
        citadelPlaced = level.getBlockState(citadelCenter.above(4)).is(ModBlocks.MATRIARCH_ALTAR.get());

        // Freeze spawned Citadel guardians in pose for deterministic framing
        AABB citadelArea = new AABB(citadelCenter).inflate(26.0, 26.0, 26.0);
        int frozen = 0;
        for (Mob guardian : level.getEntitiesOfClass(Mob.class, citadelArea)) {
            guardian.setNoAi(true);
            guardian.setInvulnerable(true);
            guardian.setDeltaMovement(Vec3.ZERO);
            guardian.setYRot(180.0F);
            guardian.setYHeadRot(180.0F);
            frozen++;
        }
        frozenGuardians = frozen;
    }

    private static void writeMetadata(Minecraft mc, boolean timedOut) {
        try {
            Path shotsDir = mc.gameDirectory.toPath().resolve("screenshots");
            Files.createDirectories(shotsDir);
            JsonObject meta = new JsonObject();
            meta.addProperty("outbreak_placed", outbreakPlaced);
            meta.addProperty("hive_placed", hivePlaced);
            meta.addProperty("sanctum_placed", sanctumPlaced);
            meta.addProperty("citadel_placed", citadelPlaced);
            meta.addProperty("frozen_guardians", frozenGuardians);
            JsonArray arr = new JsonArray();
            for (String s : capturedShots) {
                arr.add(s);
            }
            meta.add("shots_captured", arr);
            boolean allPlaced = outbreakPlaced && hivePlaced && sanctumPlaced && citadelPlaced;
            meta.addProperty("all_placed", allPlaced);
            meta.addProperty("timed_out", timedOut);
            Files.writeString(shotsDir.resolve("structure_smoke_meta.json"), GSON.toJson(meta), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            MycelialMind.LOGGER.warn("Failed to write structure_smoke_meta.json", ex);
        }
    }
}
