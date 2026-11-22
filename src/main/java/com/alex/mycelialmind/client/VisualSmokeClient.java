package com.alex.mycelialmind.client;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Automated in-game visual smoke harness active only when -Dmycelialmind.visualSmoke=true.
 */
public final class VisualSmokeClient {
    private static boolean worldRequested = false;
    private static boolean sceneBuilt = false;
    private static boolean blocksShot = false;
    private static boolean mobsShot = false;
    private static boolean bossesShot = false;
    private static int showcaseY = 0;
    private static int inWorldTicks = 0;

    private VisualSmokeClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
            worldRequested = true;
            MycelialMind.LOGGER.info("VisualSmoke: creating flat Creative world from {}", mc.screen.getClass().getSimpleName());
            LevelSettings settings = new LevelSettings(
                    "visual_smoke",
                    GameType.CREATIVE,
                    false,
                    Difficulty.NORMAL,
                    true,
                    new GameRules(),
                    WorldDataConfiguration.DEFAULT
            );
            mc.createWorldOpenFlows().createFreshLevel(
                    "visual_smoke_" + System.currentTimeMillis(),
                    settings,
                    new WorldOptions(481516L, true, false),
                    reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen
            );
            return;
        }

        if (mc.level == null || mc.player == null) {
            return;
        }

        inWorldTicks++;
        mc.options.pauseOnLostFocus = false;
        if (Boolean.getBoolean("mycelialmind.animationVisualSmoke")) {
            runAnimationScene(mc);
            return;
        }
        if (!sceneBuilt && inWorldTicks >= 15) {
            sceneBuilt = true;
            if (mc.screen != null) {
                mc.setScreen(null);
            }
            IntegratedServer server = mc.getSingleplayerServer();
            if (server != null) {
                var playerId = mc.player.getUUID();
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYMBIOTIC_BLADE.get()));
                mc.player.setYRot(0.0F);
                mc.player.setXRot(23.0F);
                mc.player.yRotO = 0.0F;
                mc.player.xRotO = 23.0F;
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    level.setDayTime(6000);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
                    var player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                        player.teleportTo(0.5, y + 2.0, 0.5);
                        if (!Boolean.getBoolean("mycelialmind.particleVisualSmoke")) {
                            populateShowcase(level, new BlockPos(0, y, 0));
                        }
                    }
                });
            }
        }

        if (Boolean.getBoolean("mycelialmind.particleVisualSmoke") && sceneBuilt) {
            mc.options.hideGui = true;
            if (inWorldTicks == 38) {
                double x = mc.player.getX();
                double y = mc.player.getY() + 0.8;
                double z = mc.player.getZ() + 3.0;
                IntegratedServer particleServer = mc.getSingleplayerServer();
                if (particleServer != null) {
                    particleServer.execute(() -> {
                        ServerLevel particleLevel = particleServer.overworld();
                        particleLevel.sendParticles(com.alex.mycelialmind.registry.ModParticles.RECOVERY.get(),
                                x - 1.2, y, z, 0, 0.65, 1.8, 0, 1);
                        particleLevel.sendParticles(com.alex.mycelialmind.registry.ModParticles.PURGE.get(),
                                x, y, z, 0, 0.65, 1.8, 0, 1);
                        particleLevel.sendParticles(com.alex.mycelialmind.registry.ModParticles.SERUM_MAGIC.get(),
                                x + 1.2, y, z, 1, 0, 0, 0, 0);
                    });
                }
            }
            if (inWorldTicks >= 40 && inWorldTicks <= 60) {
                Screenshot.grab(mc.gameDirectory, "particle_smoke_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle smoke: {}", msg.getString()));
            }
            if (inWorldTicks == 75) {
                double x = mc.player.getX();
                double y = mc.player.getY() + 0.8;
                double z = mc.player.getZ() + 3.0;
                IntegratedServer particleServer = mc.getSingleplayerServer();
                if (particleServer != null) {
                    particleServer.execute(() -> {
                        ServerLevel level = particleServer.overworld();
                        level.sendParticles(com.alex.mycelialmind.registry.ModParticles.SCAN.get(), x - 1.5, y, z, 1, 0, 0, 0, 0);
                        level.sendParticles(com.alex.mycelialmind.registry.ModParticles.ENGRAM.get(), x, y, z, 1, 0, 0, 0, 0);
                        level.sendParticles(com.alex.mycelialmind.registry.ModParticles.IMPACT.get(), x + 1.5, y, z, 0, 0, 0.2, -1, 1);
                    });
                }
            }
            if (inWorldTicks >= 77 && inWorldTicks <= 95) {
                Screenshot.grab(mc.gameDirectory, "particle_smoke_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle smoke: {}", msg.getString()));
            }
            if (inWorldTicks == 105) {
                IntegratedServer particleServer = mc.getSingleplayerServer();
                double x = mc.player.getX();
                double y = mc.player.getY() - 1.0;
                double z = mc.player.getZ() + 3.0;
                if (particleServer != null) {
                    particleServer.execute(() -> {
                        ServerLevel level = particleServer.overworld();
                        var effects = java.util.List.of(
                                com.alex.mycelialmind.registry.ModEffects.MYCELIAL_INFECTION,
                                com.alex.mycelialmind.registry.ModEffects.SPORE_ROT,
                                com.alex.mycelialmind.registry.ModEffects.SYNAPTIC_INSIGHT,
                                com.alex.mycelialmind.registry.ModEffects.SYNAPTIC_VISION);
                        for (int i = 0; i < effects.size(); i++) {
                            var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
                            if (pig != null) {
                                pig.setNoAi(true);
                                pig.moveTo(x + (i - 1.5) * 1.4, y, z, 0, 0);
                                pig.addEffect(new MobEffectInstance(effects.get(i), 200));
                                level.addFreshEntity(pig);
                            }
                        }
                    });
                }
            }
            if (inWorldTicks == 155 || inWorldTicks == 175) {
                Screenshot.grab(mc.gameDirectory, "particle_status_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle status: {}", msg.getString()));
            }
            if (inWorldTicks == 190) {
                IntegratedServer particleServer = mc.getSingleplayerServer();
                if (particleServer != null) {
                    particleServer.execute(() -> {
                        ServerLevel level = particleServer.overworld();
                        for (var pig : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class,
                                new net.minecraft.world.phys.AABB(-10, -70, -10, 10, -40, 10))) {
                            for (var effect : java.util.List.of(
                                    com.alex.mycelialmind.registry.ModEffects.MYCELIAL_INFECTION,
                                    com.alex.mycelialmind.registry.ModEffects.SPORE_ROT,
                                    com.alex.mycelialmind.registry.ModEffects.SYNAPTIC_INSIGHT,
                                    com.alex.mycelialmind.registry.ModEffects.SYNAPTIC_VISION)) {
                                if (pig.hasEffect(effect)) {
                                    pig.removeEffect(effect);
                                    pig.addEffect(new MobEffectInstance(effect, 200, 0, false, false, true));
                                }
                            }
                        }
                    });
                }
            }
            if (inWorldTicks == 250) {
                Screenshot.grab(mc.gameDirectory, "particle_status_hidden.png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Hidden particle status: {}", msg.getString()));
            }
            if (inWorldTicks == 265) {
                IntegratedServer particleServer = mc.getSingleplayerServer();
                var playerId = mc.player.getUUID();
                if (particleServer != null) {
                    particleServer.execute(() -> {
                        var player = particleServer.getPlayerList().getPlayer(playerId);
                        if (player != null) {
                            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPORE_ANTIDOTE.get()));
                            ModItems.SPORE_ANTIDOTE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                            com.alex.mycelialmind.item.BioScannerItem.activateThermalPulse(particleServer.overworld(), player);
                        }
                    });
                }
            }
            if (inWorldTicks == 270 || inWorldTicks == 278) {
                Screenshot.grab(mc.gameDirectory, "particle_action_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle action: {}", msg.getString()));
            }
            if (inWorldTicks == 300) {
                IntegratedServer particleServer = mc.getSingleplayerServer();
                var playerId = mc.player.getUUID();
                if (particleServer != null) {
                    particleServer.execute(() -> {
                        ServerLevel level = particleServer.overworld();
                        var player = particleServer.getPlayerList().getPlayer(playerId);
                        var pigs = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class,
                                new net.minecraft.world.phys.AABB(-10, -70, -10, 10, -40, 10));
                        if (player != null && pigs.size() >= 2) {
                            player.removeAllEffects();
                            pigs.sort(java.util.Comparator.comparingDouble(net.minecraft.world.entity.Entity::getX));
                            pigs.get(0).addEffect(new MobEffectInstance(com.alex.mycelialmind.registry.ModEffects.MYCELIAL_INFECTION, 200));
                            pigs.get(1).addEffect(new MobEffectInstance(com.alex.mycelialmind.registry.ModEffects.MYCELIAL_INFECTION, 200));
                            var basic = new ItemStack(ModItems.SPORE_ANTIDOTE.get());
                            var concentrated = new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get());
                            ModItems.SPORE_ANTIDOTE.get().interactLivingEntity(basic, player, pigs.get(0), InteractionHand.MAIN_HAND);
                            ModItems.CONCENTRATED_ANTIDOTE.get().interactLivingEntity(concentrated, player, pigs.get(1), InteractionHand.MAIN_HAND);
                        }
                    });
                }
            }
            if (inWorldTicks >= 302 && inWorldTicks <= 320) {
                Screenshot.grab(mc.gameDirectory, "particle_cure_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle cure: {}", msg.getString()));
            }
            if (inWorldTicks >= 335 && inWorldTicks < 435 && (inWorldTicks - 335) % 20 == 0) {
                int index = (inWorldTicks - 335) / 20;
                IntegratedServer particleServer = mc.getSingleplayerServer();
                var playerId = mc.player.getUUID();
                if (particleServer != null) {
                    particleServer.execute(() -> {
                        var player = particleServer.getPlayerList().getPlayer(playerId);
                        if (player != null) {
                            var serum = new ItemStack(ModItems.COUNTER_SERUM.get());
                            serum.set(com.alex.mycelialmind.registry.ModDataComponents.TRAIT_ID.get(),
                                    com.alex.mycelialmind.hive.AdaptationTrait.values()[index].id());
                            player.setItemInHand(InteractionHand.MAIN_HAND, serum);
                            ModItems.COUNTER_SERUM.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                        }
                    });
                }
            }
            if (inWorldTicks >= 340 && inWorldTicks < 440 && (inWorldTicks - 340) % 20 == 0) {
                Screenshot.grab(mc.gameDirectory, "particle_serum_" + ((inWorldTicks - 340) / 20) + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle serum: {}", msg.getString()));
            }
            if (inWorldTicks == 445) {
                IntegratedServer particleServer = mc.getSingleplayerServer();
                var playerId = mc.player.getUUID();
                if (particleServer != null) particleServer.execute(() -> {
                    var player = particleServer.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VOLATILE_ENGRAM.get()));
                        ModItems.VOLATILE_ENGRAM.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                    }
                });
            }
            if (inWorldTicks == 450) {
                Screenshot.grab(mc.gameDirectory, "particle_engram_action.png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle engram: {}", msg.getString()));
            }
            if (inWorldTicks == 470) {
                IntegratedServer particleServer = mc.getSingleplayerServer();
                var playerId = mc.player.getUUID();
                if (particleServer != null) particleServer.execute(() -> {
                    ServerLevel level = particleServer.overworld();
                    var player = particleServer.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        var target = com.alex.mycelialmind.registry.ModEntities.CHITIN_BRUTE.get().create(level);
                        if (target != null) {
                            target.setNoAi(true);
                            target.moveTo(player.getX(), player.getY() - 1.0, player.getZ() + 5.0, 180, 0);
                            level.addFreshEntity(target);
                            var projectile = new com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile(level, player);
                            projectile.setPos(player.getX(), player.getEyeY(), player.getZ() + 1.0);
                            projectile.setDeltaMovement(0, -0.03, 0.65);
                            level.addFreshEntity(projectile);
                        }
                    }
                });
            }
            if (inWorldTicks >= 472 && inWorldTicks <= 490) {
                Screenshot.grab(mc.gameDirectory, "particle_projectile_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle projectile: {}", msg.getString()));
            }
            if (inWorldTicks == 510 || inWorldTicks == 550) {
                boolean turretTest = inWorldTicks == 550;
                IntegratedServer particleServer = mc.getSingleplayerServer();
                var playerId = mc.player.getUUID();
                if (particleServer != null) particleServer.execute(() -> {
                    ServerLevel level = particleServer.overworld();
                    var player = particleServer.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        player.removeAllEffects();
                        for (var entity : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12))) entity.discard();
                        BlockPos target = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ() + 5);
                        level.setBlockAndUpdate(target, turretTest ? ModBlocks.PURIFICATION_TURRET.get().defaultBlockState()
                                : ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState());
                        if (turretTest && level.getBlockEntity(target) instanceof com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity turret) {
                            turret.applyCorrosiveJam(200);
                        }
                        var projectile = new com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile(level, player);
                        projectile.setPos(player.getX(), target.getY() + 0.5, player.getZ() + 1);
                        projectile.setNoGravity(true);
                        projectile.setDeltaMovement(0, 0, 0.65);
                        level.addFreshEntity(projectile);
                    }
                });
            }
            if (inWorldTicks == 520 || inWorldTicks == 560) {
                Screenshot.grab(mc.gameDirectory, "particle_block_impact_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle block impact: {}", msg.getString()));
            }
            if (inWorldTicks == 565) {
                IntegratedServer particleServer = mc.getSingleplayerServer();
                var playerId = mc.player.getUUID();
                if (particleServer != null) particleServer.execute(() -> {
                    var player = particleServer.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ() + 5);
                        var blockEntity = particleServer.overworld().getBlockEntity(pos);
                        boolean cleared = blockEntity instanceof com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity turret && !turret.isJammed();
                        MycelialMind.LOGGER.info("Particle smoke turret cleared: {}", cleared);
                    }
                });
            }
            if (inWorldTicks == 580) {
                mc.level.addParticle(com.alex.mycelialmind.registry.ModParticles.SERUM_MAGIC.get(),
                        mc.player.getX(), mc.player.getEyeY(), mc.player.getZ() + 2.0, 0, 0, 0);
            }
            if (inWorldTicks >= 582 && inWorldTicks <= 594) {
                mc.player.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() + (inWorldTicks < 588 ? 0.08 : -0.08));
                Screenshot.grab(mc.gameDirectory, "particle_camera_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle camera: {}", msg.getString()));
            }
            if (inWorldTicks >= 605 && inWorldTicks <= 632) {
                mc.player.setDeltaMovement(0, 0, 0);
                mc.player.setPos(0.5, -58.0, 0.5);
            }
            if (inWorldTicks == 605) {
                IntegratedServer particleServer = mc.getSingleplayerServer();
                if (particleServer != null) particleServer.execute(() ->
                        particleServer.overworld().removeBlock(new BlockPos(0, -57, 5), false));
            }
            if (inWorldTicks == 610) {
                mc.level.addParticle(com.alex.mycelialmind.registry.ModParticles.SCAN.get(),
                        mc.player.getX(), mc.player.getEyeY(), mc.player.getZ() + 3.0, 0, 0, 0);
            }
            if (inWorldTicks >= 612 && inWorldTicks <= 630) {
                Screenshot.grab(mc.gameDirectory, "particle_scan_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle scan: {}", msg.getString()));
            }
            if (inWorldTicks == 636) {
                double px = mc.player.getX();
                double py = mc.player.getY();
                double pz = mc.player.getZ();
                IntegratedServer particleServer = mc.getSingleplayerServer();
                if (particleServer != null) {
                    particleServer.execute(() -> {
                        ServerLevel level = particleServer.overworld();
                        level.setDayTime(18000L);
                        int bx = net.minecraft.util.Mth.floor(px);
                        int by = net.minecraft.util.Mth.floor(py);
                        int bz = net.minecraft.util.Mth.floor(pz) + 7;
                        for (int x = -5; x <= 5; x++) {
                            for (int y = -1; y <= 4; y++) {
                                level.setBlockAndUpdate(new BlockPos(bx + x, by + y, bz),
                                        net.minecraft.world.level.block.Blocks.BLACK_CONCRETE.defaultBlockState());
                            }
                        }
                    });
                }
            }
            if (inWorldTicks == 638) {
                mc.options.fov().set(45);
                mc.player.setYRot(0.0F);
                mc.player.setXRot(0.0F);
                mc.player.yRotO = 0.0F;
                mc.player.xRotO = 0.0F;
            }
            if (inWorldTicks == 640) {
                int small = 0;
                int large = 0;
                var player = mc.player;
                for (var holder : com.alex.mycelialmind.registry.ModParticles.all()) {
                    var type = holder.get();
                    if (isGalleryEmitter(type)) {
                        continue;
                    }
                    double gx;
                    double gy;
                    double gz;
                    if (isGalleryLarge(type)) {
                        gx = player.getX() - 2.6 + large * 1.3;
                        gy = player.getEyeY() + 0.2;
                        gz = player.getZ() + 5.6;
                        large++;
                    } else {
                        int col = small % 5;
                        int row = small / 5;
                        gx = player.getX() - 1.10 + col * 0.55;
                        gy = player.getEyeY() + 0.85 - row * 0.55;
                        gz = player.getZ() + 2.6;
                        small++;
                    }
                    if (type instanceof net.minecraft.core.particles.SimpleParticleType simple) {
                        mc.level.addParticle(simple, gx, gy, gz, 0, 0, 0);
                    } else {
                        mc.level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(
                                com.alex.mycelialmind.registry.ModParticles.HIVE_DUST.get(), 0xFFFFFFFF),
                                gx, gy, gz, 0, 0, 0);
                    }
                }
                MycelialMind.LOGGER.info("Particle gallery: {} small + {} large types", small, large);
            }
            if (inWorldTicks == 644 || inWorldTicks == 652) {
                Screenshot.grab(mc.gameDirectory, "particle_gallery_" + inWorldTicks + ".png", mc.getMainRenderTarget(), msg ->
                        MycelialMind.LOGGER.info("Particle gallery: {}", msg.getString()));
            }
            if (inWorldTicks >= 700) mc.stop();
            return;
        }

        if (!blocksShot && inWorldTicks >= 45) {
            blocksShot = true;
            Screenshot.grab(mc.gameDirectory, "visual_smoke.png", mc.getMainRenderTarget(), msg ->
                    MycelialMind.LOGGER.info("VisualSmoke screenshot: {}", msg.getString()));
        }

        if (blocksShot && !mobsShot) {
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ENZYME_LANCE.get()));
            mc.player.setYRot(180.0F);
            mc.player.setXRot(6.0F);
            mc.player.yRotO = 180.0F;
            mc.player.xRotO = 6.0F;
            mc.options.fov().set(70);
        }

        if (!mobsShot && inWorldTicks >= 65) {
            mobsShot = true;
            Screenshot.grab(mc.gameDirectory, "visual_smoke_mobs.png", mc.getMainRenderTarget(), msg ->
                    MycelialMind.LOGGER.info("VisualSmoke mobs screenshot: {}", msg.getString()));
        }

        if (mobsShot && inWorldTicks == 66) {
            IntegratedServer server = mc.getSingleplayerServer();
            var playerId = mc.player.getUUID();
            if (server != null) {
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    for (var entity : level.getAllEntities()) {
                        if (entity instanceof Mob mob
                                && mob.getTags().contains("mycelial_showcase")
                                && !mob.getTags().contains("mycelial_showcase_row_2")) {
                            mob.discard();
                        }
                    }
                    var player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        player.teleportTo(0.5D, showcaseY + 2.4D, -8.5D);
                    }
                });
            }
        }

        if (mobsShot && inWorldTicks >= 68) {
            mc.player.setYRot(180.0F);
            mc.player.setXRot(0.0F);
            mc.player.yRotO = 180.0F;
            mc.player.xRotO = 0.0F;
        }

        if (mobsShot && !bossesShot && inWorldTicks >= 76) {
            bossesShot = true;
            Screenshot.grab(mc.gameDirectory, "visual_smoke_bosses.png", mc.getMainRenderTarget(), msg ->
                    MycelialMind.LOGGER.info("VisualSmoke bosses screenshot: {}", msg.getString()));
        }

        if (bossesShot && inWorldTicks >= 82) {
            mc.stop();
        }
    }

    private static boolean isGalleryLarge(net.minecraft.core.particles.ParticleType<?> type) {
        return type == com.alex.mycelialmind.registry.ModParticles.SPORE_CLOUD.get()
                || type == com.alex.mycelialmind.registry.ModParticles.HIVE_PULSE.get()
                || type == com.alex.mycelialmind.registry.ModParticles.PSYCHIC_WAVE.get()
                || type == com.alex.mycelialmind.registry.ModParticles.THERMAL_RETICLE.get()
                || type == com.alex.mycelialmind.registry.ModParticles.PURIFIER_CROSS.get();
    }

    private static boolean isGalleryEmitter(net.minecraft.core.particles.ParticleType<?> type) {
        return type == com.alex.mycelialmind.registry.ModParticles.RECOVERY.get()
                || type == com.alex.mycelialmind.registry.ModParticles.PURGE.get()
                || type == com.alex.mycelialmind.registry.ModParticles.SCAN.get()
                || type == com.alex.mycelialmind.registry.ModParticles.SERUM_MELEE.get()
                || type == com.alex.mycelialmind.registry.ModParticles.SERUM_PROJECTILE.get()
                || type == com.alex.mycelialmind.registry.ModParticles.SERUM_FIRE.get()
                || type == com.alex.mycelialmind.registry.ModParticles.SERUM_EXPLOSION.get()
                || type == com.alex.mycelialmind.registry.ModParticles.SERUM_MAGIC.get()
                || type == com.alex.mycelialmind.registry.ModParticles.IMPACT.get();
    }

    private static void runAnimationScene(Minecraft mc) {        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return;
        boolean movingScene = "burrower_stalker".equals(System.getProperty("mycelialmind.animationPair"));
        int lastSample = movingScene ? 130 : 90;
        mc.options.fov().set(40);
        mc.options.hideGui = true;
        if (!sceneBuilt && inWorldTicks >= 15) {
            sceneBuilt = true;
            if (mc.screen != null) mc.setScreen(null);
            var playerId = mc.player.getUUID();
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                level.setDayTime(6000);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null) return;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(0.5, y + 1.0, 0.5);
                EntityType<?> firstType = switch (System.getProperty("mycelialmind.animationPair", "brute_warden")) {
                    case "stalker_warden" -> ModEntities.MYCELIAL_STALKER.get();
                    case "architect_warden" -> ModEntities.SPORE_ARCHITECT.get();
                    case "burrower_stalker" -> ModEntities.BURROWER.get();
                    case "spitter_warden" -> ModEntities.ACID_SPITTER.get();
                    case "pyro_warden" -> ModEntities.PYRO_SHROOM.get();
                    case "carrier_warden" -> ModEntities.GOLIATH_CARRIER.get();
                    case "weaver_warden" -> ModEntities.SYNAPSE_WEAVER.get();
                    case "mimic_warden" -> ModEntities.SYNAPTIC_MIMIC.get();
                    case "chimera_warden" -> ModEntities.MYCELIAL_CHIMERA.get();
                    case "behemoth_warden" -> ModEntities.SPORE_BEHEMOTH.get();
                    default -> ModEntities.CHITIN_BRUTE.get();
                };
                String rankSetting = System.getProperty("mycelialmind.animationRank", "base").toLowerCase(java.util.Locale.ROOT);
                EvolutionRank forcedRank = rankSetting.equals("elite") ? EvolutionRank.ELITE
                        : rankSetting.equals("mutated") ? EvolutionRank.MUTATED : EvolutionRank.BASE;
                EntityType<?>[] types = {
                        firstType,
                        movingScene ? ModEntities.MYCELIAL_STALKER.get() : ModEntities.COLONY_WARDEN.get()
                };
                for (int i = 0; i < types.length; i++) {
                    if (types[i].create(level) instanceof Mob mob) {
                        mob.setInvulnerable(true);
                        mob.addTag("mycelial_animation_smoke");
                        if (forcedRank != EvolutionRank.BASE) {
                            if (mob instanceof MycelialMob mycelialMob) {
                                mycelialMob.promoteRank(forcedRank);
                            } else if (mob instanceof MycelialChimeraEntity chimera) {
                                chimera.promoteRank(forcedRank);
                            }
                        }
                        double x = i == 0 ? -1.1 : 2.1;
                        mob.moveTo(x, y, movingScene ? -9.0 : -6.0, 0.0F, 0.0F);
                        mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
                        level.addFreshEntity(mob);
                        Mob target = EntityType.IRON_GOLEM.create(level);
                        if (target != null) {
                            target.setNoAi(true);
                            target.setInvulnerable(true);
                            target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12000, 0, false, false));
                            target.addTag("mycelial_animation_target_" + i);
                            target.moveTo(x, y, -4.3, 180.0F, 0.0F);
                            level.addFreshEntity(target);
                            mob.addTag("mycelial_animation_unit_" + i);
                        }
                    }
                }
            });
        }
        mc.player.setYRot(180.0F);
        mc.player.setXRot(0.0F);
        mc.player.yRotO = 180.0F;
        mc.player.xRotO = 0.0F;
        if (inWorldTicks >= 55 && inWorldTicks <= lastSample) {
            for (var entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof Mob mob) || !(mc.getEntityRenderDispatcher().getRenderer(mob) instanceof MobRenderer<?, ?> renderer)) continue;
                if (movingScene && mob.getType() != ModEntities.BURROWER.get() && mob.getType() != ModEntities.MYCELIAL_STALKER.get()) continue;
                if (!movingScene && mob.getType() != ModEntities.SPORE_ARCHITECT.get() && mob.getType() != ModEntities.COLONY_WARDEN.get()) continue;
                try {
                    var model = renderer.getModel();
                    if (movingScene) {
                        String fieldName = mob.getType() == ModEntities.BURROWER.get() ? "drillHead" : "rShin";
                        var partField = model.getClass().getDeclaredField(fieldName);
                        partField.setAccessible(true);
                        ModelPart part = (ModelPart) partField.get(model);
                        float thighAngle = Float.NaN;
                        if (mob.getType() == ModEntities.MYCELIAL_STALKER.get()) {
                            var thighField = model.getClass().getDeclaredField("rightLeg");
                            thighField.setAccessible(true);
                            thighAngle = ((ModelPart) thighField.get(model)).xRot;
                        }
                        MycelialMind.LOGGER.info("AnimationVisualSmoke motion: {} tick={} z={} moving={} angle={} thigh={} swinging={}",
                                mob.getType(), inWorldTicks, mob.getZ(), mob.walkAnimation.speed(),
                                mob.getType() == ModEntities.BURROWER.get() ? part.zRot : part.xRot, thighAngle, mob.swinging);
                        continue;
                    }
                    var rightField = model.getClass().getDeclaredField("armR");
                    var leftField = model.getClass().getDeclaredField("armL");
                    rightField.setAccessible(true);
                    leftField.setAccessible(true);
                    ModelPart right = (ModelPart) rightField.get(model);
                    ModelPart left = (ModelPart) leftField.get(model);
                    MycelialMind.LOGGER.info("AnimationVisualSmoke pose: {} tick={} aggressive={} rightX={} leftX={} leftY={}",
                            mob.getType(), inWorldTicks, mob.isAggressive(), right.xRot, left.xRot, left.yRot);
                } catch (ReflectiveOperationException ex) {
                    MycelialMind.LOGGER.warn("AnimationVisualSmoke: unable to inspect arm pose", ex);
                }
            }
        }
        if (inWorldTicks == 48 || (inWorldTicks >= 55 && inWorldTicks <= lastSample)) {
            int actionTick = inWorldTicks;
            server.execute(() -> {
                var entities = server.overworld().getAllEntities();
                for (var entity : entities) {
                    if (entity instanceof Mob mob && mob.getTags().contains("mycelial_animation_smoke")) {
                        if (actionTick == 48) {
                            if (movingScene) mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
                            int index = mob.getTags().contains("mycelial_animation_unit_0") ? 0 : 1;
                            for (var candidate : server.overworld().getAllEntities()) {
                                if (candidate instanceof LivingEntity target
                                        && target.getTags().contains("mycelial_animation_target_" + index)) {
                                    mob.setTarget(target);
                                    break;
                                }
                            }
                        } else {
                            if (movingScene && actionTick == 100) {
                                mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
                                mob.getNavigation().stop();
                            }
                            MycelialMind.LOGGER.info("AnimationVisualSmoke: {} target={} aggressive={} swinging={} tick={}",
                                    mob.getType(), mob.getTarget() != null, mob.isAggressive(), mob.swinging, actionTick);
                        }
                    }
                }
            });
        }
        if (inWorldTicks >= 43 && inWorldTicks <= lastSample) {
            String filename = String.format(java.util.Locale.ROOT, "animation_smoke_%03d.png", inWorldTicks);
            Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(), msg -> {});
        }
        if (inWorldTicks >= lastSample + 5) mc.stop();
    }

    private static void populateShowcase(ServerLevel level, BlockPos origin) {
        Block[] blocks = {
                ModBlocks.CREEPING_MYCELIUM.get(),
                ModBlocks.SPORE_VENT.get(),
                ModBlocks.MYCELIAL_NODE.get(),
                ModBlocks.PURIFICATION_PYLON.get(),
                ModBlocks.REINFORCED_BIO_BARRIER.get(),
                ModBlocks.HIVE_ALTAR.get(),
                ModBlocks.INCUBATOR_NODE.get(),
                ModBlocks.SENTINEL_NODE.get(),
                ModBlocks.PURIFICATION_TURRET.get(),
                ModBlocks.SYNAPTIC_RESEARCH_BENCH.get(),
                ModBlocks.CHITIN_PILLAR.get(),
                ModBlocks.BIO_CHITIN_LATTICE.get(),
                ModBlocks.MATRIARCH_ALTAR.get(),
                ModBlocks.MYCELIAL_RIFT.get(),
                ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()
        };
        for (int i = 0; i < blocks.length; i++) {
            // Compact 2-row showcase (7 front + 8 back) close to camera so all 15 blocks fill the frame
            boolean frontRow = i < 7;
            int x = frontRow ? (i - 3) : ((i - 7) - 4);
            int z = frontRow ? 5 : 7;
            BlockPos pos = origin.offset(x, 0, z);
            level.setBlockAndUpdate(pos, blocks[i].defaultBlockState());
        }

        showcaseY = origin.getY();
        EntityType<?>[][] rows = {
                {
                        ModEntities.SPORELING.get(),
                        ModEntities.LEECH_PARASITE.get(),
                        ModEntities.PYRO_SHROOM.get(),
                        ModEntities.BURROWER.get(),
                        ModEntities.PURIFIER_DRONE.get(),
                        ModEntities.ACID_SPITTER.get(),
                        ModEntities.SYNAPTIC_MIMIC.get()
                },
                {
                        ModEntities.CHITIN_BRUTE.get(),
                        ModEntities.MYCELIAL_STALKER.get(),
                        ModEntities.SYNAPSE_WEAVER.get(),
                        ModEntities.SPORE_ARCHITECT.get(),
                        ModEntities.MYCELIAL_CHIMERA.get()
                },
                {
                        ModEntities.GOLIATH_CARRIER.get(),
                        ModEntities.COLONY_WARDEN.get(),
                        ModEntities.SPORE_BEHEMOTH.get(),
                        ModEntities.HIVE_CORE.get(),
                        ModEntities.APEX_MATRIARCH.get()
                }
        };
        double[] rowZ = {-7.0D, -11.5D, -18.5D};
        double[] rowSpacing = {1.4D, 2.2D, 4.2D};
        for (int row = 0; row < rows.length; row++) {
            EntityType<?>[] rowMobs = rows[row];
            for (int col = 0; col < rowMobs.length; col++) {
                var entity = rowMobs[col].create(level);
                if (entity instanceof Mob mob) {
                    mob.setNoAi(true);
                    mob.setInvulnerable(true);
                    mob.addTag("mycelial_showcase");
                    mob.addTag("mycelial_showcase_row_" + row);
                    double x = origin.getX() + 0.5D + (col - (rowMobs.length - 1) * 0.5D) * rowSpacing[row];
                    double z = origin.getZ() + rowZ[row];
                    mob.moveTo(x, origin.getY(), z, row == 2 ? 22.0F : 0.0F, 0.0F);
                    if (row == 2) {
                        mob.setYBodyRot(22.0F);
                        mob.setYHeadRot(22.0F);
                    }
                    level.addFreshEntity(mob);
                }
            }
        }
    }
}
