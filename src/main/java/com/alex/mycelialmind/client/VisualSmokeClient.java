package com.alex.mycelialmind.client;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
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
    private static boolean expeditionRequested;
    private static int expeditionTicks;
    private static java.util.UUID expeditionEscortId;
    private static boolean expeditionPulseSoundReceived;
    private static final java.util.concurrent.atomic.AtomicReference<Throwable> expeditionFailure = new java.util.concurrent.atomic.AtomicReference<>();

    private VisualSmokeClient() {
    }

    @SubscribeEvent
    public static void onSound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
        if (expeditionTicks < 150 || expeditionTicks > 200 || event.getSound() == null) return;
        var sound = event.getSound();
        if (!expeditionPulseSoundReceived && sound.getLocation().equals(
                com.alex.mycelialmind.registry.ModSounds.PURIFICATION_PYLON_PULSE.get().getLocation())) {
            expeditionPulseSoundReceived = true;
            MycelialMind.LOGGER.info("Expedition smoke: client received the defended-pulse sound");
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
            worldRequested = true;
            String reloadWorld = System.getProperty("mycelialmind.expeditionReloadWorld", "");
            if (!reloadWorld.isBlank()) {
                mc.createWorldOpenFlows().openWorld(reloadWorld, () -> {
                    throw new IllegalStateException("Could not open expedition smoke world: " + reloadWorld);
                });
                return;
            }
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
        if (!System.getProperty("mycelialmind.expeditionReloadWorld", "").isBlank()) {
            if (expeditionFailure.get() != null) throw new IllegalStateException("Expedition reload failed", expeditionFailure.get());
            if (Boolean.getBoolean("mycelialmind.expeditionReloadPulse")) {
                runExpeditionPulseReload(mc);
                return;
            }
            if (!expeditionRequested && mc.getSingleplayerServer() != null) {
                expeditionRequested = true;
                expeditionExecute(mc.getSingleplayerServer(), () -> {
                    ServerLevel level = mc.getSingleplayerServer().getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                    if (level == null) throw new IllegalStateException("Deep Mycelium missing on reload");
                    var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
                    if (!data.hasCompletedEnclave(new BlockPos(6, 96, 8))
                            || !data.hasCompletedEnclave(new BlockPos(6, 96, 7))
                            || !com.alex.mycelialmind.hive.HiveMindSavedData.get(level).isMatriarchDefeated()) {
                        throw new IllegalStateException("Saved expedition enclaves or Matriarch victory missing on reload");
                    }
                    MycelialMind.LOGGER.info("Expedition reload smoke: route, garden and Matriarch victory restored from disk");
                    BlockPos pending = new BlockPos(5, 96, 9);
                    if (!pending.equals(data.getActivePos()) || data.getLinkedFlora().size() != 2
                            || data.getActiveOutcome() != com.alex.mycelialmind.hive.PostMatriarchExpeditionData.Outcome.GARDEN) {
                        throw new IllegalStateException("Expedition reload smoke: two-focus garden was not restored");
                    }
                    for (BlockPos focus : data.getLinkedFlora()) {
                        if (!level.getBlockState(focus).is(ModBlocks.RECLAIMED_BLOOM.get())
                                || level.getBlockState(focus).getValue(ReclaimedBloomBlock.PALETTE) != data.getActivePalette()) {
                            throw new IllegalStateException("Expedition reload smoke: linked flower or palette was not restored");
                        }
                    }
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    if (player == null || player.serverLevel() != level) throw new IllegalStateException("Reloaded player not in Deep Mycelium");
                    BlockPos third = new BlockPos(3, 96, 3);
                    level.setBlockAndUpdate(third, ModBlocks.SPORE_BULB.get().defaultBlockState());
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
                    int enclaveCount = data.getEnclaves().size();
                    long scanTick = level.getGameTime();
                    ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, third));
                    var secondPlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "GardenProbe"));
                    secondPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
                    ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(secondPlayer, third));
                    if (level.getGameTime() != scanTick || data.getEnclaves().size() != enclaveCount + 1
                            || !level.getBlockState(third).is(ModBlocks.RECLAIMED_BLOOM.get())) {
                        throw new IllegalStateException("Expedition reload smoke: final garden focus did not resolve once");
                    }
                    MycelialMind.LOGGER.info("Expedition reload smoke: two scans in tick {} completed one garden", scanTick);
                    if (!data.hasCompletedEnclave(pending) || data.hasActiveExpedition()) {
                        throw new IllegalStateException("Expedition reload smoke: restored garden did not complete");
                    }
                    MycelialMind.LOGGER.info("Expedition reload smoke: two-focus garden restored and completed after restart");
                    if (com.alex.mycelialmind.config.MycelialConfig.SPEC.isLoaded()) {
                        com.alex.mycelialmind.config.MycelialConfig.HOMEOSTASIS_ENABLED.set(false);
                    }
                    BlockPos route = new BlockPos(5, 96, 8);
                    BlockPos pylon = new BlockPos(4, 96, 8);
                    level.setBlockAndUpdate(route, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
                    player.setShiftKeyDown(true);
                    ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, route));
                    player.setShiftKeyDown(false);
                    ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, pylon));
                    if (!data.isPulseInProgress() || data.getPulseAttackers().isEmpty()) {
                        throw new IllegalStateException("Expedition reload smoke: could not prepare active pulse for restart");
                    }
                    MycelialMind.LOGGER.info("Expedition reload smoke: prepared {} real attackers for pulse restart", data.getPulseAttackers().size());
                });
            }
            if (inWorldTicks >= 80) mc.stop();
            return;
        }
        if (Boolean.getBoolean("mycelialmind.expeditionVisualSmoke")) {
            runExpeditionScene(mc);
            return;
        }
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

    private static void runExpeditionPulseReload(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return;
        if (inWorldTicks >= 40 && !expeditionRequested) {
            expeditionRequested = true;
            expeditionExecute(server, () -> {
                ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
                if (!data.isPulseInProgress() || !new BlockPos(5, 96, 8).equals(data.getActivePos())
                        || data.getPulseAttackers().isEmpty()) {
                    throw new IllegalStateException("Expedition reload smoke: active pulse was not restored");
                }
                var attackers = data.getPulseAttackers();
                for (var id : attackers) {
                    var entity = level.getEntity(id);
                    if (!(entity instanceof MycelialMob)) {
                        throw new IllegalStateException("Expedition reload smoke: tracked attacker missing after restart: " + id);
                    }
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setNoGravity(true);
                player.teleportTo(5.5, 140, 8.5);
            });
        }
        if (inWorldTicks == 85) expeditionExecute(server, () -> {
            ServerLevel level = server.getPlayerList().getPlayers().getFirst().serverLevel();
            var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
            if (!data.isPulsePaused() || data.getPulsesCompleted() != 0 || data.getPulseAttackers().isEmpty()
                    || !data.getPulseAttackers().stream().allMatch(id ->
                            level.getEntity(id) instanceof MycelialMob mob && mob.isNoAi())) {
                throw new IllegalStateException("Expedition reload smoke: leaving did not pause the tracked attackers");
            }
            var player = server.getPlayerList().getPlayers().getFirst();
            player.teleportTo(5.5, 97, 8.5);
            player.setNoGravity(false);
        });
        if (inWorldTicks == 130) expeditionExecute(server, () -> {
            ServerLevel level = server.getPlayerList().getPlayers().getFirst().serverLevel();
            var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
            var attackers = data.getPulseAttackers();
            if (!data.isPulseInProgress() || data.isPulsePaused() || attackers.isEmpty()
                    || !attackers.stream().allMatch(id ->
                            level.getEntity(id) instanceof MycelialMob mob && !mob.isNoAi())) {
                throw new IllegalStateException("Expedition reload smoke: returning did not resume the tracked attackers");
            }
            for (var id : attackers) level.getEntity(id).kill();
            MycelialMind.LOGGER.info("Expedition reload smoke: paused, resumed and resolved {} real pulse attackers", attackers.size());
        });
        if (inWorldTicks == 175) expeditionExecute(server, () -> {
            ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
            var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
            if (data.isPulseInProgress() || data.getPulsesCompleted() != 1) {
                throw new IllegalStateException("Expedition reload smoke: restored pulse did not complete exactly once");
            }
            MycelialMind.LOGGER.info("Expedition reload smoke: restored pulse completed exactly once");
        });
        if (inWorldTicks >= 195) mc.stop();
    }

    private static void runExpeditionScene(Minecraft mc) {
        Throwable failure = expeditionFailure.get();
        if (failure != null) throw new IllegalStateException("Expedition smoke failed on server", failure);
        var server = mc.getSingleplayerServer();
        if (!expeditionRequested && inWorldTicks >= 15 && server != null) {
            expeditionRequested = true;
            var playerId = mc.player.getUUID();
            mc.options.particles().set(net.minecraft.client.ParticleStatus.MINIMAL);
            if (com.alex.mycelialmind.config.MycelialClientConfig.SPEC.isLoaded()) {
                com.alex.mycelialmind.config.MycelialClientConfig.PARTICLE_DENSITY_SCALE.set(0.25);
            }
            expeditionExecute(server, () -> {
                if (Boolean.getBoolean("mycelialmind.expeditionForceFailure")) {
                    throw new IllegalStateException("Forced expedition smoke failure");
                }
                if (server.getWorldData() instanceof net.minecraft.world.level.storage.PrimaryLevelData worldData) {
                    worldData.withConfirmedWarning(true);
                }
                ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                var player = server.getPlayerList().getPlayer(playerId);
                if (level == null || player == null) return;
                if (com.alex.mycelialmind.config.MycelialConfig.SPEC.isLoaded()) {
                    com.alex.mycelialmind.config.MycelialConfig.HOMEOSTASIS_ENABLED.set(false);
                }
                BlockPos base = new BlockPos(0, 96, 0);
                for (int x = -7; x <= 7; x++) for (int z = -2; z <= 11; z++) {
                    level.setBlockAndUpdate(base.offset(x, -1, z), ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
                    for (int y = 0; y <= 5; y++) level.setBlockAndUpdate(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                for (int i = 0; i < 6; i++) {
                    level.setBlockAndUpdate(base.offset(i - 3, 0, 5), ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState()
                            .setValue(ReclaimedBloomBlock.PALETTE, i));
                }
                BlockPos node = base.offset(0, 0, 9);
                level.setBlockAndUpdate(node, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
                var hive = com.alex.mycelialmind.hive.HiveMindSavedData.get(level);
                hive.recordMatriarchDefeat(level.getGameTime());
                var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
                var route = base.offset(-6, 0, 8);
                BlockPos testPylon = base.offset(-4, 0, 8);
                level.setBlockAndUpdate(testPylon, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
                if (!data.start(route, com.alex.mycelialmind.hive.PostMatriarchExpeditionData.Outcome.ROUTE, 1)
                        || !data.beginPulse(testPylon, java.util.List.of(java.util.UUID.randomUUID()), level.getGameTime())) {
                    throw new IllegalStateException("Expedition smoke could not prepare pylon interruption");
                }
                level.setBlockAndUpdate(testPylon, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                if (data.isPulseInProgress() || data.getPulsesCompleted() != 0 || !data.abandon(route)) {
                    throw new IllegalStateException("Expedition smoke: removing pylon did not interrupt the pulse");
                }
                MycelialMind.LOGGER.info("Expedition smoke: pylon removal interrupted the active pulse in deep_mycelium");
                level.setBlockAndUpdate(testPylon, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
                if (!data.start(route, com.alex.mycelialmind.hive.PostMatriarchExpeditionData.Outcome.ROUTE, 1)
                        || !data.beginPulse(testPylon, java.util.List.of(java.util.UUID.randomUUID()), level.getGameTime())) {
                    throw new IllegalStateException("Expedition smoke could not prepare smothered pylon");
                }
                for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    level.setBlockAndUpdate(testPylon.relative(direction), ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
                }
                if (!com.alex.mycelialmind.world.PurificationZoneTracker.get(level).isPylonSmothered(testPylon)
                        || data.isPulseInProgress() || data.getPulsesCompleted() != 0 || !data.abandon(route)) {
                    throw new IllegalStateException("Expedition smoke: cysts did not interrupt the pulse");
                }
                MycelialMind.LOGGER.info("Expedition smoke: four cysts interrupted the active pulse in deep_mycelium");
                for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    level.setBlockAndUpdate(testPylon.relative(direction), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(testPylon, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                data.start(route, com.alex.mycelialmind.hive.PostMatriarchExpeditionData.Outcome.ROUTE, 1);
                data.recordPulse(1); data.recordPulse(2); data.recordPulse(3); data.complete();
                data.start(node, com.alex.mycelialmind.hive.PostMatriarchExpeditionData.Outcome.GARDEN, 2);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(level, 0.5, 97.5, 0.5, 0.0F, 12.0F);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
                com.alex.mycelialmind.network.PostMatriarchExpeditionSyncPayload.syncToPlayer(player);
            });
        }
        if (!mc.level.dimension().equals(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL)) return;
        expeditionTicks++;
        if (expeditionTicks == 50) {
            Screenshot.grab(mc.gameDirectory, "expedition_garden_before.png", mc.getMainRenderTarget(), msg -> MycelialMind.LOGGER.info("Expedition smoke: {}", msg.getString()));
            var playerId = mc.player.getUUID();
            expeditionExecute(server, () -> {
                ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                var player = server.getPlayerList().getPlayer(playerId);
                BlockPos flower = new BlockPos(0, 96, 5);
                if (level != null && player != null) ModBlocks.RECLAIMED_BLOOM.get().harvest(level, flower, level.getBlockState(flower), player);
            });
        }
        if (expeditionTicks == 70) Screenshot.grab(mc.gameDirectory, "expedition_garden_after.png", mc.getMainRenderTarget(), msg -> MycelialMind.LOGGER.info("Expedition smoke: {}", msg.getString()));
        if (expeditionTicks == 72) {
            var inventory = mc.player.getInventory();
            inventory.setItem(0, new ItemStack(ModBlocks.RECLAIMED_BLOOM.get()));
            inventory.setItem(1, new ItemStack(ModItems.RECLAIMED_BLOOM_MELEE.get()));
            inventory.setItem(2, new ItemStack(ModItems.RECLAIMED_BLOOM_PROJECTILE.get()));
            inventory.setItem(3, new ItemStack(ModItems.RECLAIMED_BLOOM_FIRE.get()));
            inventory.setItem(4, new ItemStack(ModItems.RECLAIMED_BLOOM_EXPLOSION.get()));
            inventory.setItem(5, new ItemStack(ModItems.RECLAIMED_BLOOM_MAGIC.get()));
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }
        if (expeditionTicks == 78) Screenshot.grab(mc.gameDirectory, "expedition_bloom_inventory.png",
                mc.getMainRenderTarget(), msg -> MycelialMind.LOGGER.info("Expedition smoke: {}", msg.getString()));
        if (expeditionTicks == 80) mc.setScreen(new com.alex.mycelialmind.client.screen.BioScannerCodexScreen());
        if (expeditionTicks == 83 && mc.screen != null) {
            int left = (mc.screen.width - 312) / 2;
            int top = (mc.screen.height - 220) / 2;
            mc.screen.mouseClicked(left + 150, top + 30, 0);
        }
        if (expeditionTicks == 90) Screenshot.grab(mc.gameDirectory, "expedition_radar.png", mc.getMainRenderTarget(), msg -> MycelialMind.LOGGER.info("Expedition smoke: {}", msg.getString()));
        if (expeditionTicks == 95 && mc.screen != null) {
            int left = (mc.screen.width - 312) / 2;
            int top = (mc.screen.height - 220) / 2;
            mc.screen.mouseClicked(left + 110, top + 206, 0);
        }
        if (expeditionTicks == 105) Screenshot.grab(mc.gameDirectory, "expedition_directory.png", mc.getMainRenderTarget(), msg -> MycelialMind.LOGGER.info("Expedition smoke: {}", msg.getString()));
        if (expeditionTicks == 110 && mc.screen != null) {
            int left = (mc.screen.width - 312) / 2;
            int top = (mc.screen.height - 220) / 2;
            mc.screen.mouseClicked(left + 25, top + 66, 0);
            mc.screen.mouseClicked(left + 30, top + 179, 0);
            for (char c : "Portal".toCharArray()) mc.screen.charTyped(c, 0);
            mc.screen.mouseClicked(left + 250, top + 179, 0);
        }
        if (expeditionTicks == 120 && mc.screen != null) {
            int left = (mc.screen.width - 312) / 2;
            int top = (mc.screen.height - 220) / 2;
            mc.screen.mouseClicked(left + 190, top + 179, 0);
        }
        if (expeditionTicks == 130) Screenshot.grab(mc.gameDirectory, "expedition_named.png", mc.getMainRenderTarget(), msg -> MycelialMind.LOGGER.info("Expedition smoke: {}", msg.getString()));
        if (expeditionTicks == 135) mc.setScreen(null);
        if (expeditionTicks == 145) Screenshot.grab(mc.gameDirectory, "expedition_pinned_hud.png", mc.getMainRenderTarget(), msg -> MycelialMind.LOGGER.info("Expedition smoke: {}", msg.getString()));
        if (expeditionTicks == 150) {
            var playerId = mc.player.getUUID();
            expeditionExecute(server, () -> {
                ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                var player = server.getPlayerList().getPlayer(playerId);
                if (level == null || player == null) throw new IllegalStateException("Expedition escort smoke has no level or player");
                var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
                BlockPos node = new BlockPos(6, 96, 8);
                BlockPos pylon = new BlockPos(4, 96, 8);
                if (!data.abandon(data.getActivePos())) throw new IllegalStateException("Could not clear smoke garden");
                level.setBlockAndUpdate(node, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
                level.setBlockAndUpdate(pylon, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
                player.setGameMode(GameType.SURVIVAL);
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2000, 4, false, false));
                com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector.handleScannerUseOn(player, level, node, true);
                com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector.handleScannerUseOn(player, level, pylon, false);
                var attackers = data.getPulseAttackers();
                if (!data.isPulseInProgress() || attackers.isEmpty()
                        || !(level.getEntity(attackers.get(0)) instanceof MycelialMob mob)) {
                    throw new IllegalStateException("Scanner did not start a pulse with real attackers");
                }
                MycelialMind.LOGGER.info("Expedition smoke: scanner started a pulse with {} attackers in deep_mycelium", attackers.size());
                long scanTick = level.getGameTime();
                var secondPlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                        new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ExpeditionProbe"));
                secondPlayer.setPos(player.getX(), player.getY(), player.getZ());
                boolean handled = com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector
                        .handleScannerUseOn(secondPlayer, level, pylon, false);
                if (!handled || level.getGameTime() != scanTick || !data.isPulseInProgress()
                        || data.getPulsesCompleted() != 0 || !attackers.equals(data.getPulseAttackers())) {
                    throw new IllegalStateException("Second scanner changed the active pulse in the same server tick");
                }
                MycelialMind.LOGGER.info("Expedition smoke: two player scans in tick {} preserved one pulse with {} attackers",
                        scanTick, attackers.size());
                mob.setNoAi(true);
                expeditionEscortId = mob.getUUID();
                mob.setAlliedEscort(playerId, level.getGameTime() + 1200);
                for (int i = 1; i < attackers.size(); i++) {
                    var attacker = level.getEntity(attackers.get(i));
                    if (attacker != null) attacker.discard();
                }
            });
        }
        if (expeditionTicks == 200) {
            if (!expeditionPulseSoundReceived) {
                throw new IllegalStateException("Expedition smoke: client did not receive the defended-pulse sound");
            }
            expeditionExecute(server, () -> {
                ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
                if (data.getPulsesCompleted() != 1 || data.isPulseInProgress()
                        || !(level.getEntity(expeditionEscortId) instanceof MycelialMob mob) || !mob.isAlive()) {
                    throw new IllegalStateException("Expedition smoke: allied escort blocked the pulse or was removed");
                }
                MycelialMind.LOGGER.info("Expedition smoke: allied escort survived and the pulse completed in deep_mycelium");
                String keep = System.getProperty("mycelialmind.expeditionKeepSmokeEntities", "false");
                if (!keep.equals("true") && !keep.equals("mob")) {
                    mob.discard();
                }
                if (!keep.equals("false") && !keep.equals("true") && !keep.equals("blocks")) {
                    data.abandon(data.getActivePos());
                    level.setBlockAndUpdate(new BlockPos(4, 96, 8), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(6, 96, 8), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            });
        }
        if ((expeditionTicks == 320 || expeditionTicks == 470 || expeditionTicks == 620)
                && System.getProperty("mycelialmind.expeditionKeepSmokeEntities", "false").equals("false")) {
            int expected = expeditionTicks == 620 ? 2 : 1;
            boolean interrupt = expeditionTicks == 320;
            expeditionExecute(server, () -> {
                ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
                if (data.getPulsesCompleted() != expected || data.isPulseInProgress()) {
                    throw new IllegalStateException("Expedition smoke: previous route pulse did not finish");
                }
                ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, new BlockPos(4, 96, 8)));
                var attackers = data.getPulseAttackers();
                if (!data.isPulseInProgress() || attackers.isEmpty()) {
                    throw new IllegalStateException("Expedition smoke: next route pulse did not start");
                }
                if (interrupt) {
                    BlockPos pylon = new BlockPos(4, 96, 8);
                    level.setBlockAndUpdate(pylon, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    if (data.isPulseInProgress() || data.getPulsesCompleted() != 1 || !data.getPulseAttackers().isEmpty()) {
                        throw new IllegalStateException("Expedition smoke: interruption did not preserve the defended pulse");
                    }
                    level.setBlockAndUpdate(pylon, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
                    MycelialMind.LOGGER.info("Expedition smoke: rebuilt pylon preserved the first defended pulse");
                    return;
                }
                for (var id : attackers) {
                    var attacker = level.getEntity(id);
                    if (attacker == null) throw new IllegalStateException("Expedition smoke: attacker missing");
                    attacker.kill();
                }
            });
        }
        if (expeditionTicks == 670 && System.getProperty("mycelialmind.expeditionKeepSmokeEntities", "false").equals("false")) {
            expeditionExecute(server, () -> {
                    ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                    var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
                    BlockPos route = new BlockPos(6, 96, 8);
                    if (!data.hasCompletedEnclave(route) || !level.getBlockState(route).is(ModBlocks.MYCELIAL_STONE.get())) {
                        throw new IllegalStateException("Expedition smoke: three pulses did not recover the route");
                    }
                    MycelialMind.LOGGER.info("Expedition smoke: three scanner pulses recovered the route in deep_mycelium");
                    var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                    BlockPos routePylon = new BlockPos(4, 96, 8);
                    if (!expeditionRouteProtected(player, route)) throw new IllegalStateException("Recovered route should be protected");
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        level.setBlockAndUpdate(routePylon.relative(direction), ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
                    }
                    if (expeditionRouteProtected(player, route)) throw new IllegalStateException("Smothered route should be unprotected");
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        level.setBlockAndUpdate(routePylon.relative(direction), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                    if (!expeditionRouteProtected(player, route)) throw new IllegalStateException("Cleared route should be protected again");
                    MycelialMind.LOGGER.info("Expedition smoke: route marker followed pylon protection changes");
                    BlockPos gardenNode = new BlockPos(6, 96, 7);
                    level.setBlockAndUpdate(gardenNode, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
                    player.setShiftKeyDown(true);
                    ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, gardenNode));
                    ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, gardenNode));
                    player.setShiftKeyDown(false);
                    int palette = data.getActivePalette();
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYNAPTIC_GRAFT.get()));
                    var tracker = com.alex.mycelialmind.world.PurificationZoneTracker.get(level);
                    var temporaryNodes = new java.util.ArrayList<BlockPos>();
                    int cap = com.alex.mycelialmind.config.MycelialConfig.MAX_SYMBIOTIC_NODES.get();
                    while (tracker.countSymbioticNodes() < cap) {
                        BlockPos extra = new BlockPos(-6 + temporaryNodes.size(), 96, 1);
                        temporaryNodes.add(extra);
                        level.setBlockAndUpdate(extra, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
                    }
                    ModItems.SYNAPTIC_GRAFT.get().useOn(expeditionUseContext(player, gardenNode));
                    if (player.getMainHandItem().getCount() != 1 || !level.getBlockState(gardenNode).is(ModBlocks.MYCELIAL_NODE.get())) {
                        throw new IllegalStateException("Expedition smoke: node cap consumed the graft or converted the node");
                    }
                    for (BlockPos extra : temporaryNodes) {
                        level.setBlockAndUpdate(extra, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                    ModItems.SYNAPTIC_GRAFT.get().useOn(expeditionUseContext(player, gardenNode));
                    if (!player.getMainHandItem().isEmpty() || !level.getBlockState(gardenNode).is(ModBlocks.SYMBIOTIC_NODE.get())) {
                        throw new IllegalStateException("Expedition smoke: graft did not convert the garden node exactly once");
                    }
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
                    for (int i = 0; i < 3; i++) {
                        BlockPos focus = new BlockPos(3 + i, 96, 5);
                        level.setBlockAndUpdate(focus, ModBlocks.SPORE_BULB.get().defaultBlockState());
                        ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, focus));
                        if (!level.getBlockState(focus).is(ModBlocks.RECLAIMED_BLOOM.get())
                                || level.getBlockState(focus).getValue(ReclaimedBloomBlock.PALETTE) != palette) {
                            throw new IllegalStateException("Expedition smoke: garden focus did not retain the selected palette");
                        }
                        if (i == 1 && (!data.hasActiveExpedition() || data.getLinkedFlora().size() != 2)) {
                            throw new IllegalStateException("Expedition smoke: two foci must not complete the garden");
                        }
                    }
                    if (!data.hasCompletedEnclave(gardenNode) || data.hasActiveExpedition()) {
                        throw new IllegalStateException("Expedition smoke: garden did not complete after three foci");
                    }
                    MycelialMind.LOGGER.info("Expedition smoke: scanner and graft completed a three-focus garden in deep_mycelium; cap and two-focus checks passed");
            });
        }
        if (expeditionTicks == 690 && System.getProperty("mycelialmind.expeditionKeepSmokeEntities", "false").equals("false")) {
            var playerId = mc.player.getUUID();
            expeditionExecute(server, () -> {
                ServerLevel level = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
                var player = server.getPlayerList().getPlayer(playerId);
                var data = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
                BlockPos pylon = new BlockPos(0, 115, 0);
                BlockPos node = pylon.offset(2, 0, 0);
                for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) for (int y = -5; y <= 5; y++) {
                    level.setBlockAndUpdate(pylon.offset(x, y, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                }
                level.setBlockAndUpdate(node, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
                level.setBlockAndUpdate(pylon, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
                player.setShiftKeyDown(true);
                ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, node));
                player.setShiftKeyDown(false);
                ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, pylon));
                if (!data.hasActiveExpedition() || data.isPulseInProgress() || data.getPulsesCompleted() != 0
                        || !data.getPulseAttackers().isEmpty()) {
                    throw new IllegalStateException("Expedition smoke: obstructed pylon advanced or started a pulse");
                }
                MycelialMind.LOGGER.info("Expedition smoke: obstructed pylon did not start or award a pulse");
                data.abandon(node);
                BlockPos pending = new BlockPos(5, 96, 9);
                level.setBlockAndUpdate(pending, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
                player.setShiftKeyDown(true);
                ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, pending));
                ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, pending));
                player.setShiftKeyDown(false);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYNAPTIC_GRAFT.get()));
                var tracker = com.alex.mycelialmind.world.PurificationZoneTracker.get(level);
                var temporaryNodes = new java.util.ArrayList<BlockPos>();
                int cap = com.alex.mycelialmind.config.MycelialConfig.MAX_SYMBIOTIC_NODES.get();
                for (int i = tracker.countSymbioticNodes(); i < cap; i++) {
                    BlockPos extra = pylon.offset(i % 19 - 9, 3 + i / 19, -9);
                    level.setBlockAndUpdate(extra, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
                    tracker.addNode(extra, com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC);
                    temporaryNodes.add(extra);
                }
                try {
                    var rejected = ModItems.SYNAPTIC_GRAFT.get().useOn(expeditionUseContext(player, pending));
                    if (rejected != net.minecraft.world.InteractionResult.FAIL
                            || player.getMainHandItem().getCount() != 1
                            || !level.getBlockState(pending).is(ModBlocks.MYCELIAL_NODE.get())) {
                        throw new IllegalStateException("Expedition smoke: graft cap did not preserve the node and item");
                    }
                    MycelialMind.LOGGER.info("Expedition smoke: garden graft rejected at the {}-node cap without consuming the graft", cap);
                } finally {
                    for (BlockPos extra : temporaryNodes) level.removeBlock(extra, false);
                }
                ModItems.SYNAPTIC_GRAFT.get().useOn(expeditionUseContext(player, pending));
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
                for (int x = 1; x <= 2; x++) {
                    BlockPos focus = new BlockPos(x, 96, 3);
                    level.setBlockAndUpdate(focus, ModBlocks.SPORE_BULB.get().defaultBlockState());
                    ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, focus));
                }
                if (!pending.equals(data.getActivePos()) || data.getLinkedFlora().size() != 2) {
                    throw new IllegalStateException("Expedition smoke: failed to prepare two-focus garden for reload");
                }
                MycelialMind.LOGGER.info("Expedition smoke: prepared two-focus garden for restart");
                for (BlockPos candidate : BlockPos.betweenClosed(pending.offset(-24, -24, -24),
                        pending.offset(24, 24, 24))) {
                    if (candidate.distSqr(pending) > 24 * 24 || !level.isLoaded(candidate)) continue;
                    var state = level.getBlockState(candidate);
                    if (state.is(ModBlocks.SPORE_BULB.get()) || state.is(ModBlocks.MYCELIAL_TENDRILS.get())
                            || state.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())) {
                        level.removeBlock(candidate, false);
                    }
                }
                ModItems.BIO_SCANNER.get().useOn(expeditionUseContext(player, pending));
            });
        }
        int finishTick = System.getProperty("mycelialmind.expeditionKeepSmokeEntities", "false").equals("false") ? 710 : 215;
        if (expeditionTicks >= finishTick + Math.max(0, Integer.getInteger("mycelialmind.expeditionHoldTicks", 0))) mc.stop();
    }

    private static void expeditionExecute(IntegratedServer server, Runnable action) {
        server.execute(() -> {
            try {
                action.run();
            } catch (Throwable failure) {
                expeditionFailure.compareAndSet(null, failure);
                MycelialMind.LOGGER.error("Expedition smoke server assertion failed", failure);
            }
        });
    }

    private static net.minecraft.world.item.context.UseOnContext expeditionUseContext(
            net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        return new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                        net.minecraft.core.Direction.UP, pos, false));
    }

    private static boolean expeditionRouteProtected(net.minecraft.server.level.ServerPlayer player, BlockPos route) {
        return com.alex.mycelialmind.network.PostMatriarchExpeditionSyncPayload.forPlayer(player).enclaves().stream()
                .filter(marker -> marker.pos().equals(route)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Expedition smoke: route marker missing")).protectedZone();
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
                ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get(),
                ModBlocks.RECLAIMED_BLOOM.get()
        };
        for (int i = 0; i < blocks.length; i++) {
            // Compact 2-row showcase close to camera so the full block set fills the frame.
            boolean frontRow = i < 8;
            int x = (i % 8) - 3;
            int z = frontRow ? 5 : 7;
            BlockPos pos = origin.offset(x, 0, z);
            level.setBlockAndUpdate(pos, blocks[i].defaultBlockState());
        }
        for (int palette = 0; palette <= 5; palette++) {
            BlockPos bloomPos = origin.offset(palette - 2, 0, 3);
            level.setBlockAndUpdate(bloomPos, ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState()
                    .setValue(ReclaimedBloomBlock.PALETTE, palette));
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
