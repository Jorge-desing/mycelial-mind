package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SporeBehemothEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.network.DiplomacySyncPayload;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.network.SymbiosisSyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.ModDimensions;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Triggers escalating Mycelial raid waves when Hive threat crosses 25%, 50%, 75%, and 100%
 * while respecting Purification Pylon safe zones and synchronizing Hive state to clients.
 */
public final class HiveWaveDirector {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int[] MILESTONES = {25, 50, 75, 100};

    private HiveWaveDirector() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && serverPlayer.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
                ModDimensions.syncCrossDimensionProgression(serverLevel.getServer().overworld(), serverLevel);
            }
            SymbiosisAttributeSync.sync(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, HiveSyncPayload.forPlayer(serverLevel, serverPlayer));
            SymbiosisSyncPayload.syncToPlayer(serverPlayer);
            DiplomacySyncPayload.syncToPlayer(serverPlayer);
            syncPlayerDiscovery(serverLevel, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SymbiosisAttributeSync.sync(serverPlayer);
            SymbiosisSyncPayload.syncToPlayer(serverPlayer);
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                syncPlayerDiscovery(serverLevel, serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && serverPlayer.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
                ModDimensions.syncCrossDimensionProgression(serverLevel.getServer().overworld(), serverLevel);
            }
            PacketDistributor.sendToPlayer(serverPlayer, HiveSyncPayload.forPlayer(serverLevel, serverPlayer));
            SymbiosisSyncPayload.syncToPlayer(serverPlayer);
            DiplomacySyncPayload.syncToPlayer(serverPlayer);
            syncPlayerDiscovery(serverLevel, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof MycelialMob mob)
                || !usesGlobalHiveAdaptations(mob)) {
            return;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        hive.recomputeActiveTraits(serverLevel.getGameTime());
        if (!mob.hasCurrentGlobalHiveState(serverLevel, hive)) {
            mob.applyGlobalHiveState(serverLevel, hive);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        long gameTime = serverLevel.getGameTime();
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        hive.recomputeActiveTraits(gameTime);
        if (hive.needsMobTraitSynchronization()) {
            for (var entity : serverLevel.getAllEntities()) {
                if (entity instanceof MycelialMob mob && usesGlobalHiveAdaptations(mob)
                        && !mob.hasCurrentGlobalHiveState(serverLevel, hive)) {
                    mob.applyGlobalHiveState(serverLevel, hive);
                }
            }
            hive.markMobTraitsSynchronized();
        }

        if (gameTime % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        HiveBlackboard.stepDirectorAndInfluence(serverLevel);
        HiveBlackboard.updateStrategicState(serverLevel, hive);
        handleFungalTide(serverLevel, hive, gameTime);
        if (gameTime % 600 == 0) {
            trySpontaneousSchism(serverLevel, hive);
        }
        for (ServerPlayer player : serverLevel.players()) {
            PacketDistributor.sendToPlayer(player, HiveSyncPayload.forPlayer(serverLevel, player));
            SymbiosisSyncPayload.syncToPlayer(player);
            syncPlayerDiscovery(serverLevel, player);
        }

        if (!MycelialConfig.WAVES_ENABLED.get()) {
            return;
        }

        int nextMilestone = computePendingMilestone(hive);
        if (nextMilestone <= 0) {
            return;
        }

        long cooldown = MycelialConfig.WAVE_COOLDOWN_TICKS.get();
        if (hive.getLastWaveGameTime() > 0 && (gameTime - hive.getLastWaveGameTime()) < cooldown) {
            return;
        }

        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.isSpectator() && !player.isCreative()) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        candidates.sort((a, b) -> Integer.compare(
                b.getData(ModAttachments.SYMBIOSIS).getEquippedCount(),
                a.getData(ModAttachments.SYMBIOSIS).getEquippedCount()
        ));

        PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
        boolean overworld = serverLevel.dimension().equals(net.minecraft.world.level.Level.OVERWORLD);
        boolean tideActive = hive.isFungalTideActive(gameTime);
        boolean establishedRoutes = hive.hasDenseHyphalRoutes();
        if (overworld && establishedRoutes && !tideActive) {
            List<ServerPlayer> inNetwork = new ArrayList<>();
            for (ServerPlayer candidate : candidates) {
                double density = hive.getHyphalDensityAt(candidate.blockPosition());
                BlockPos nearestNode = tracker.findNearestEnemyNode(candidate.blockPosition());
                boolean nearNode = nearestNode != null && nearestNode.distSqr(candidate.blockPosition()) <= 48.0D * 48.0D;
                if (!HyphalAdaptiveArchitectureEngine.shouldDeferOverworldRaidOutsideNetwork(
                        density, nearNode, false, true)) {
                    inNetwork.add(candidate);
                }
            }
            if (inNetwork.isEmpty()) {
                if (gameTime % 600L == 0L) {
                    for (ServerPlayer sp : candidates) {
                        sp.displayClientMessage(Component.translatable("message.mycelialmind.hyphal_detour_evaded")
                                .withStyle(ChatFormatting.GREEN), true);
                    }
                }
                return;
            }
            candidates = inNetwork;
        }

        boolean spawnedAny = false;
        for (ServerPlayer target : candidates) {
            if (spawnWaveAround(serverLevel, tracker, target, nextMilestone)) {
                spawnedAny = true;
                target.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.wave_triggered", nextMilestone)
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }
        }

        if (spawnedAny) {
            hive.setLastWaveMilestone(nextMilestone, gameTime);
            HiveBlackboard.recordCombatStress(serverLevel, 0.30F);
        }
    }

    private static void syncPlayerDiscovery(ServerLevel serverLevel, ServerPlayer player) {
        PlayerDiscoveryData discovery = player.getData(ModAttachments.DISCOVERY);
        discovery.setBastionScore(PlayerDiscoveryData.computeBastionScoreForPlayer(serverLevel, player));
        DiscoverySyncPayload.syncToPlayer(player);
    }

    private static boolean usesGlobalHiveAdaptations(MycelialMob mob) {
        return !mob.isBossMob();
    }

    private static void trySpontaneousSchism(ServerLevel level, HiveMindSavedData hive) {
        if (hive.getThreatLevel() < 65.0 || hive.getActiveSynergies().size() < 2 || level.random.nextFloat() >= 0.12F) {
            return;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        for (ServerPlayer sp : level.players()) {
            BlockPos nodePos = tracker.findNearestEnemyNode(sp.blockPosition());
            if (nodePos != null && nodePos.distSqr(sp.blockPosition()) <= 48.0 * 48.0
                    && level.getBlockEntity(nodePos) instanceof SynapticNodeBlockEntity nodeBe) {
                HiveStrain strain = level.random.nextBoolean() ? HiveStrain.AMBER : HiveStrain.CRIMSON;
                nodeBe.induceSchism(strain, 500);
                sp.sendSystemMessage(Component.translatable("message.mycelialmind.spontaneous_schism",
                        MycelialText.strain(strain)).withStyle(ChatFormatting.GOLD));
                break;
            }
        }
    }

    public static float getDirectorPacingFactor(ServerLevel level) {
        float base = switch (HiveBlackboard.getPacingPhase(level)) {
            case BUILD_UP -> 1.0F;
            case PEAK -> 1.20F;
            case RELAXATION -> 0.70F;
        };
        if (HiveMindSavedData.get(level).isMatriarchRemissionActive(level.getGameTime())) {
            return base * 0.50F;
        }
        return base;
    }

    public static void handleFungalTide(ServerLevel level, HiveMindSavedData hive, long gameTime) {
        if (!MycelialConfig.FUNGAL_TIDE_ENABLED.get() || hive.isMatriarchRemissionActive(gameTime)) {
            return;
        }
        if (!hive.isFungalTideActive(gameTime)) {
            if (hive.getThreatLevel() >= 75.0) {
                long cooldown = MycelialConfig.FUNGAL_TIDE_COOLDOWN_TICKS.get();
                if (hive.getLastFungalTideGameTime() == 0 || (gameTime - hive.getLastFungalTideGameTime()) >= cooldown) {
                    hive.triggerFungalTide(gameTime, MycelialConfig.FUNGAL_TIDE_DURATION_TICKS.get());
                    for (ServerPlayer sp : level.players()) {
                        sp.sendSystemMessage(Component.translatable("message.mycelialmind.fungal_tide_started")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                        level.playSound(null, sp.blockPosition(), ModSounds.FUNGAL_TIDE_HORN.get(), SoundSource.HOSTILE, 0.75F, 0.85F);
                    }
                }
            }
            return;
        }

        if (hive.getThreatLevel() < 50.0) {
            hive.endFungalTide(gameTime);
            return;
        }

        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        for (ServerPlayer sp : level.players()) {
            if (sp.isSpectator() || tracker.isProtected(sp.blockPosition())) {
                continue;
            }
            level.sendParticles(ModParticles.TIDE_MOTE.get(), sp.getX(), sp.getY() + 1.5, sp.getZ(), 10, 4.5, 2.0, 4.5, 0.02);
            level.sendParticles(ModParticles.TIDE_MOTE.get(), sp.getX(), sp.getY() + 1.5, sp.getZ(), 10, 4.5, 2.0, 4.5, 0.02);

            if (!sp.isCreative() && level.getEntitiesOfClass(SporeBehemothEntity.class, new AABB(sp.blockPosition()).inflate(64.0D)).isEmpty()) {
                spawnFungalTideBehemoth(level, tracker, sp);
            }
        }
    }

    private static void spawnFungalTideBehemoth(ServerLevel level, PurificationZoneTracker tracker, ServerPlayer target) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 24.0 + random.nextDouble() * 12.0;
            int x = (int) Math.round(target.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(target.getZ() + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!tracker.isProtected(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                SporeBehemothEntity behemoth = ModEntities.SPORE_BEHEMOTH.get().create(level);
                if (behemoth != null) {
                    behemoth.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
                    behemoth.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                    behemoth.setTarget(target);
                    if (level.noCollision(behemoth) && level.addFreshEntity(behemoth)) {
                        return;
                    }
                }
            }
        }
    }

    public static int computePendingMilestone(HiveMindSavedData hive) {
        double threat = hive.getThreatLevel();
        int last = hive.getLastWaveMilestone();
        int targetMilestone = 0;
        for (int milestone : MILESTONES) {
            if (threat >= milestone && milestone > last) {
                targetMilestone = milestone;
            }
        }
        return targetMilestone;
    }

    private static boolean spawnWaveAround(
            ServerLevel level,
            PurificationZoneTracker tracker,
            ServerPlayer player,
            int milestone
    ) {
        RandomSource random = level.getRandom();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        HiveDoctrine doctrine = HiveBlackboard.getDoctrine(level);
        List<EntityType<? extends MycelialMob>> squad = HiveSquadComposer.composeWaveSquad(
                hive,
                doctrine,
                milestone,
                random,
                HiveBlackboard.getPacingPhase(level),
                tracker.countSymbioticNodes(),
                hive.isFungalTideActive(level.getGameTime())
        );
        int spawned = 0;

        // Prefer spawning from an active unprotected Incubator or Relay node near the player
        BlockPos anchorNode = tracker.findNearestNode(player.blockPosition(), com.alex.mycelialmind.world.SynapticNodeType.INCUBATOR);
        if (anchorNode == null || tracker.isProtected(anchorNode) || anchorNode.distSqr(player.blockPosition()) > 56.0 * 56.0) {
            anchorNode = tracker.findNearestNode(player.blockPosition(), com.alex.mycelialmind.world.SynapticNodeType.RELAY);
        }
        boolean useNodeAnchor = anchorNode != null
                && !tracker.isProtected(anchorNode)
                && anchorNode.distSqr(player.blockPosition()) <= 56.0 * 56.0;

        double promoChance = hive.getEffectivenessPressure()
                + HiveProgressionTier.fromThreat(hive.getThreatLevel()).wavePromotionBonus();
        for (EntityType<? extends MycelialMob> type : squad) {
            if (useNodeAnchor) {
                BlockPos spawnPos = anchorNode.offset(random.nextIntBetweenInclusive(-2, 2), 1, random.nextIntBetweenInclusive(-2, 2));
                if (!tracker.isProtected(spawnPos)) {
                    MycelialMob mob = type.create(level);
                    if (mob != null) {
                        mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
                        prepareWaveMob(mob, hive, random, promoChance);
                        mob.setTarget(player);
                        BlockPos floor = spawnPos.below();
                        if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                                && level.noCollision(mob)
                                && level.addFreshEntity(mob)) {
                            spawned++;
                            continue;
                        }
                    }
                }
            }

            for (int attempt = 0; attempt < 8; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double dist = 22.0 + random.nextDouble() * 12.0;
                int x = (int) Math.round(player.getX() + Math.cos(angle) * dist);
                int z = (int) Math.round(player.getZ() + Math.sin(angle) * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y, z);
                if (tracker.isProtected(pos) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                    continue;
                }
                MycelialMob mob = type.create(level);
                if (mob != null) {
                    mob.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                    prepareWaveMob(mob, hive, random, promoChance);
                    mob.setTarget(player);
                    if (level.noCollision(mob) && level.addFreshEntity(mob)) {
                        spawned++;
                        break;
                    }
                }
            }
        }
        return spawned > 0;
    }

    private static void prepareWaveMob(
            MycelialMob mob,
            HiveMindSavedData hive,
            RandomSource random,
            double promoChance
    ) {
        if (promoChance > 0.0 && random.nextDouble() < promoChance) {
            mob.promoteRank(promoChance >= 0.25 ? EvolutionRank.ELITE : EvolutionRank.MUTATED);
        }
        if (hive.hasHabitAdaptation(PlayerHabit.BASTION_BUILDER)
                && (mob instanceof com.alex.mycelialmind.entity.ChitinBruteEntity
                || mob instanceof com.alex.mycelialmind.entity.BurrowerEntity)) {
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 0, false, false));
        }
    }
}
