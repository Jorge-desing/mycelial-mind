package com.alex.mycelialmind.world;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.PurificationPylonBlock;
import com.alex.mycelialmind.block.PurificationTurretBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.LivingBattlefieldSystem;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Coordinates Dimensional Homeostasis inside deep_mycelium:
 * tracks foreign block placements/explosions, escalates per-chunk Immune Response,
 * secretes physical ceiling acid rain, grows antibody_cyst encapsulations, and handles sedation.
 */
public final class DimensionalHomeostasisDirector {
    private static final int CHUNK_STEP_INTERVAL_TICKS = 40; // 2 seconds
    private static final int ACID_RAIN_INTERVAL_TICKS = 30;  // 1.5 seconds
    private static final int HUD_SYNC_INTERVAL_TICKS = 20;   // 1 second
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final ResourceLocation IMMUNE_HOMEOSTASIS_ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "immune_homeostasis");

    private DimensionalHomeostasisDirector() {
    }

    public static double applyProgressionStressScaling(ServerLevel level, double delta) {
        if (delta == 0.0) {
            return 0.0;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        HiveProgressionTier tier = HiveProgressionTier.fromThreat(hive.getThreatLevel());
        boolean remission = hive.isMatriarchRemissionActive(level.getGameTime());
        if (delta > 0.0) {
            double cycleMult = com.alex.mycelialmind.hive.HiveCycleDirector.getActiveHomeostasisStressMultiplier(level);
            return delta * tier.deepMyceliumStressMultiplier() * (remission ? 0.50 : 1.0) * cycleMult;
        }
        return remission ? delta * 1.50 : delta;
    }

    public static boolean isHomeostasisDimension(Level level) {
        return level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);
    }

    public static boolean isForeignBlock(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        boolean isBedrock = state.is(Blocks.BEDROCK);
        return DimensionalHomeostasisEngine.isForeignBlockId(
                id.getNamespace(),
                id.getPath(),
                state.isAir(),
                !state.getFluidState().isEmpty() && state.liquid(),
                isBedrock
        );
    }

    public static boolean isNaturalCavernCeiling(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return DimensionalHomeostasisEngine.isNaturalCavernCeilingId(id.getNamespace(), id.getPath());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || !isHomeostasisDimension(serverLevel)
                || !MycelialConfig.HOMEOSTASIS_ENABLED.get()) {
            return;
        }
        handleForeignPlacement(serverLevel, event.getPos(), event.getPlacedBlock(), event.getEntity() instanceof Player);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || !isHomeostasisDimension(serverLevel)
                || !MycelialConfig.HOMEOSTASIS_ENABLED.get()) {
            return;
        }
        handleBlockBreak(serverLevel, event.getPos(), event.getState(), event.getPlayer());
    }

    public static void handleBlockBreak(ServerLevel serverLevel, BlockPos pos, BlockState state, @Nullable Player player) {
        if (!MycelialConfig.HOMEOSTASIS_ENABLED.get()) {
            return;
        }
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(serverLevel);
        data.removeForeignBlock(pos);
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        boolean nearSymbiotic = PurificationZoneTracker.get(serverLevel).hasNearbySymbioticNode(pos, 16);
        double trauma = DimensionalHomeostasisEngine.computeNativeTissueTraumaStress(
                id.getNamespace(), id.getPath(), nearSymbiotic
        );
        if (trauma > 0.0) {
            ChunkPos cp = new ChunkPos(pos);
            double scaled = applyProgressionStressScaling(serverLevel, trauma);
            double next = data.addSensitizedStress(cp, scaled, serverLevel.getGameTime());
            checkAndAnnounceTransition(serverLevel, cp, next);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !isHomeostasisDimension(serverLevel)
                || !MycelialConfig.HOMEOSTASIS_ENABLED.get()) {
            return;
        }
        BlockPos center = BlockPos.containing(event.getExplosion().center());
        recordExplosionImpact(serverLevel, center, event.getAffectedBlocks().size(), event.getExplosion().radius());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !isHomeostasisDimension(serverLevel)
                || !MycelialConfig.HOMEOSTASIS_ENABLED.get()) {
            return;
        }
        long gameTime = serverLevel.getGameTime();
        if (gameTime % DimensionalHomeostasisEngine.LEUKOCYTE_PATROL_COOLDOWN_TICKS == 0L) {
            DimensionalHomeostasisSavedData.get(serverLevel).pruneExpiredLeukocyteCooldowns(gameTime);
        }
        if (gameTime % CHUNK_STEP_INTERVAL_TICKS == 0) {
            stepActiveChunks(serverLevel);
        }
        if (gameTime % ACID_RAIN_INTERVAL_TICKS == 0) {
            runAcidRainTick(serverLevel);
        }
        if (gameTime % HUD_SYNC_INTERVAL_TICKS == 0) {
            DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(serverLevel);
            for (ServerPlayer player : serverLevel.players()) {
                ChunkPos cp = new ChunkPos(player.blockPosition());
                if (data.getEffectiveNeighborhoodStress(cp) > 0.0) {
                    PacketDistributor.sendToPlayer(player, HiveSyncPayload.forPlayer(serverLevel, player));
                }
            }
        }
    }

    public static void handleForeignPlacement(ServerLevel level, BlockPos pos, BlockState placedState, boolean placedByPlayer) {
        if (!MycelialConfig.HOMEOSTASIS_ENABLED.get()) {
            return;
        }
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        ChunkPos chunkPos = new ChunkPos(pos);
        long now = level.getGameTime();

        if (placedState.getBlock() instanceof SymbioticNodeBlock) {
            data.sedateArea(chunkPos, 0,
                    DimensionalHomeostasisEngine.SYMBIOTIC_PLACE_CALM,
                    DimensionalHomeostasisEngine.SYMBIOTIC_SENSITIZATION_REDUCTION,
                    now);
            return;
        }

        if (placedState.getBlock() instanceof PurificationPylonBlock) {
            reconcileForeignBlocksInChunk(level, data, chunkPos);
            data.recordForeignBlock(pos);
            int activePylons = tracker.countActivePylonsWithin(pos, MycelialConfig.PYLON_PURIFICATION_RADIUS.get());
            double delta = activePylons > DimensionalHomeostasisEngine.PYLON_EXCESS_THRESHOLD
                    ? DimensionalHomeostasisEngine.PYLON_EXCESS_PLACE_STRESS
                    : DimensionalHomeostasisEngine.PYLON_FIRST_PLACE_STRESS;
            double next = data.addSensitizedStress(chunkPos, applyProgressionStressScaling(level, delta), now);
            checkAndAnnounceTransition(level, chunkPos, next);
            return;
        }

        if (placedState.getBlock() instanceof PurificationTurretBlock) {
            reconcileForeignBlocksInChunk(level, data, chunkPos);
            data.recordForeignBlock(pos);
            int nearbyTurrets = countTurretsNear(level, data, pos, 24);
            double delta = nearbyTurrets > DimensionalHomeostasisEngine.TURRET_EXCESS_THRESHOLD
                    ? DimensionalHomeostasisEngine.TURRET_EXCESS_PLACE_STRESS
                    : DimensionalHomeostasisEngine.TURRET_BASE_PLACE_STRESS;
            double next = data.addSensitizedStress(chunkPos, applyProgressionStressScaling(level, delta), now);
            checkAndAnnounceTransition(level, chunkPos, next);
            return;
        }

        if (placedByPlayer && isForeignBlock(placedState)) {
            reconcileForeignBlocksInChunk(level, data, chunkPos);
            data.recordForeignBlock(pos);
            double next = data.addSensitizedStress(chunkPos,
                    applyProgressionStressScaling(level, DimensionalHomeostasisEngine.FOREIGN_BLOCK_PLACE_STRESS), now);
            checkAndAnnounceTransition(level, chunkPos, next);
        }
    }

    public static void recordExplosionImpact(ServerLevel level, BlockPos center, int affectedBlocks, float radius) {
        if (!MycelialConfig.HOMEOSTASIS_ENABLED.get()) {
            return;
        }
        double delta = DimensionalHomeostasisEngine.computeExplosionStress(
                affectedBlocks, radius, MycelialConfig.HOMEOSTASIS_EXPLOSION_STRESS_SCALE.get()
        );
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        ChunkPos epicentre = new ChunkPos(center);
        long now = level.getGameTime();

        double epicentreStress = data.addSensitizedStress(epicentre, delta, now);
        checkAndAnnounceTransition(level, epicentre, epicentreStress);

        if (affectedBlocks >= 12) {
            double spillover = delta * 0.4;
            int cx = epicentre.x;
            int cz = epicentre.z;
            for (ChunkPos neighbor : new ChunkPos[]{
                    new ChunkPos(cx + 1, cz),
                    new ChunkPos(cx - 1, cz),
                    new ChunkPos(cx, cz + 1),
                    new ChunkPos(cx, cz - 1)
            }) {
                double nStress = data.addSensitizedStress(neighbor, spillover, now);
                checkAndAnnounceTransition(level, neighbor, nStress);
            }
        }
    }

    public static void stepActiveChunks(ServerLevel level) {
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        Set<Long> activeKeys = new HashSet<>(data.getTrackedChunkKeys());
        for (ServerPlayer player : level.players()) {
            activeKeys.add(new ChunkPos(player.blockPosition()).toLong());
        }
        for (long key : activeKeys) {
            ChunkPos cp = new ChunkPos(key);
            if (!level.isLoaded(cp.getMiddleBlockPosition(64))) {
                continue;
            }
            stepChunkHomeostasis(level, cp);
        }
    }

    public static void stepChunkHomeostasis(ServerLevel level, ChunkPos chunkPos) {
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos anchor = chunkAnchorPos(level, data, tracker, chunkPos);

        int baseRadius = MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
        int activePylons = tracker.countActivePylonsWithin(anchor, baseRadius);
        int maxPylonTier = tracker.getMaxActivePylonTierWithin(anchor, baseRadius);
        if (activePylons == 0 && maxPylonTier > 0) {
            activePylons = 1;
        }
        int nearbyTurrets = countTurretsNear(level, data, anchor, 24);
        int nearbySymbiotic = countSymbioticNodesNear(tracker, anchor, 24);

        double delta = applyProgressionStressScaling(level,
                DimensionalHomeostasisEngine.computePeriodicStressDelta(
                        activePylons, maxPylonTier, nearbyTurrets, nearbySymbiotic));
        SovereignColonyData nearbyColony = SovereignColonySavedData.get(level).findNearestColony(anchor, null, 24.0D);
        if (nearbyColony != null && level.getBlockState(nearbyColony.getNexusPos()).is(ModBlocks.SOVEREIGN_NEXUS.get())) {
            delta -= 1.5D * nearbyColony.getTier();
        }
        double currentStress = data.getStress(chunkPos);
        if (currentStress <= 0.0 && delta <= 0.0) {
            if (data.getLastRecordedStage(chunkPos) != HomeostasisStage.STABLE) {
                checkAndAnnounceTransition(level, chunkPos, currentStress);
            }
            return;
        }

        double nextStress = data.addSensitizedStress(chunkPos, delta, level.getGameTime());
        HomeostasisStage stage = checkAndAnnounceTransition(level, chunkPos, nextStress);

        if (stage != HomeostasisStage.STABLE) {
            HiveBlackboard.getInfluenceMap(level).addInfluence(
                    anchor,
                    0.15F,
                    0.30F,
                    stage == HomeostasisStage.IMMUNE_RESPONSE ? 0.65F : 0.35F
            );
        }

        if (stage == HomeostasisStage.IMMUNE_RESPONSE) {
            HiveBlackboard.reportTarget(level, anchor);
            diffuseInflammationToNeighbors(level, data, chunkPos, nextStress);
            if (MycelialConfig.HOMEOSTASIS_ANTIBODY_ENCAPSULATION_ENABLED.get()) {
                tryEncapsulateChunk(level, chunkPos, 2, false);
            }
            if (DimensionalHomeostasisEngine.isCytokineStorm(nextStress)) {
                trySpawnLeukocytePatrol(level, data, chunkPos, anchor);
            }
        }
    }

    public static int diffuseInflammationToNeighbors(
            ServerLevel level,
            DimensionalHomeostasisSavedData data,
            ChunkPos sourceChunk,
            double sourceStress
    ) {
        if (sourceStress < DimensionalHomeostasisEngine.IMMUNE_THRESHOLD) {
            return 0;
        }
        int diffused = 0;
        long now = level.getGameTime();
        for (ChunkPos neighbor : new ChunkPos[]{
                new ChunkPos(sourceChunk.x + 1, sourceChunk.z),
                new ChunkPos(sourceChunk.x - 1, sourceChunk.z),
                new ChunkPos(sourceChunk.x, sourceChunk.z + 1),
                new ChunkPos(sourceChunk.x, sourceChunk.z - 1)
        }) {
            double neighborStress = data.getStress(neighbor);
            double transfer = DimensionalHomeostasisEngine.computeInflammatoryNeighborDiffusion(
                    sourceStress, neighborStress, hasLiveForeignBlocks(level, data, neighbor)
            );
            if (transfer > 0.0) {
                double nextNeighbor = data.addStress(neighbor, transfer, now);
                checkAndAnnounceTransition(level, neighbor, nextNeighbor);
                diffused++;
            }
        }
        return diffused;
    }

    public static int trySpawnLeukocytePatrol(
            ServerLevel level,
            DimensionalHomeostasisSavedData data,
            ChunkPos chunkPos,
            BlockPos anchor
    ) {
        long now = level.getGameTime();
        long lastWave = data.getLastLeukocyteWaveTick(chunkPos);
        if (lastWave > 0L && (now - lastWave) < DimensionalHomeostasisEngine.LEUKOCYTE_PATROL_COOLDOWN_TICKS) {
            return 0;
        }
        int nearbyMobs = level.getEntitiesOfClass(MycelialMob.class, new AABB(anchor).inflate(32.0D)).size();
        if (nearbyMobs >= 10) {
            return 0;
        }
        boolean hasTarget = !level.getEntitiesOfClass(ServerPlayer.class, new AABB(anchor).inflate(36.0D),
                p -> !p.isCreative() && !p.isSpectator()).isEmpty()
                || hasLiveForeignBlocks(level, data, chunkPos);
        if (!hasTarget) {
            return 0;
        }

        int count = nearbyMobs <= 4 ? 3 : 2;
        int spawned = 0;
        List<EntityType<? extends MycelialMob>> pool = List.of(
                ModEntities.SPORELING.get(),
                ModEntities.LEECH_PARASITE.get(),
                ModEntities.ACID_SPITTER.get(),
                ModEntities.BURROWER.get()
        );
        for (int i = 0; i < count; i++) {
            BlockPos candidate = findLeukocyteSpawnPos(level, anchor, i);
            if (candidate == null) {
                continue;
            }
            EntityType<? extends MycelialMob> type = pool.get((level.random.nextInt(pool.size()) + i) % pool.size());
            MycelialMob mob = type.create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(candidate), MobSpawnType.EVENT, null);
            mob.setAggressive(true);
            if (!level.getWorldBorder().isWithinBounds(mob.blockPosition())
                    || !level.noCollision(mob)
                    || !level.addFreshEntity(mob)) {
                continue;
            }
            level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                    candidate.getX() + 0.5, candidate.getY() + 0.8, candidate.getZ() + 0.5,
                    10, 0.35, 0.35, 0.35, 0.03);
            spawned++;
        }
        if (spawned > 0) {
            data.setLastLeukocyteWaveTick(chunkPos, now);
            level.playSound(null, anchor, ModSounds.IMMUNE_SURGE.get(), SoundSource.HOSTILE, 0.8F, 1.25F);
        }
        return spawned;
    }

    @Nullable
    private static BlockPos findLeukocyteSpawnPos(ServerLevel level, BlockPos anchor, int attemptSeed) {
        for (int tries = 0; tries < 6; tries++) {
            int dx = level.random.nextIntBetweenInclusive(-6, 6);
            int dz = level.random.nextIntBetweenInclusive(-6, 6);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                dx = (attemptSeed % 2 == 0 ? 3 : -3);
            }
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos pos = anchor.offset(dx, dy, dz);
                if (!level.isInWorldBounds(pos)
                        || !level.isInWorldBounds(pos.above())
                        || !level.getWorldBorder().isWithinBounds(pos)
                        || !level.getWorldBorder().isWithinBounds(pos.above())) {
                    continue;
                }
                if (level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.above()).isAir()
                        && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static HomeostasisStage checkAndAnnounceTransition(ServerLevel level, ChunkPos chunkPos, double stress) {
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        HomeostasisStage prev = data.getLastRecordedStage(chunkPos);
        HomeostasisStage next = DimensionalHomeostasisEngine.stageForStress(stress);
        if (prev == next) {
            return next;
        }
        data.setLastRecordedStage(chunkPos, next);
        BlockPos center = chunkPos.getMiddleBlockPosition(64);
        if (next == HomeostasisStage.IRRITATED && prev == HomeostasisStage.STABLE) {
            level.playSound(null, center, ModSounds.HOMEOSTASIS_HEARTBEAT.get(), SoundSource.AMBIENT, 1.0F, 0.85F);
            notifyChunkPlayers(level, chunkPos, "message.mycelialmind.homeostasis_irritated", ChatFormatting.GOLD);
        } else if (next == HomeostasisStage.IMMUNE_RESPONSE) {
            data.adjustSensitization(chunkPos, 1);
            level.playSound(null, center, ModSounds.IMMUNE_SURGE.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            notifyChunkPlayers(level, chunkPos, "message.mycelialmind.homeostasis_immune_response", ChatFormatting.RED);
        } else if (next == HomeostasisStage.STABLE) {
            notifyChunkPlayers(level, chunkPos, "message.mycelialmind.homeostasis_stabilized", ChatFormatting.GREEN);
        }
        return next;
    }

    private static void notifyChunkPlayers(ServerLevel level, ChunkPos chunkPos, String key, ChatFormatting color) {
        BlockPos center = chunkPos.getMiddleBlockPosition(64);
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - center.getX();
            double dz = player.getZ() - center.getZ();
            if ((dx * dx + dz * dz) <= 48.0D * 48.0D) {
                player.sendSystemMessage(Component.translatable(key).withStyle(color));
            }
        }
    }

    public static int tryEncapsulateChunk(ServerLevel level, ChunkPos chunkPos, int maxPlacements, boolean deterministic) {
        if (maxPlacements <= 0) {
            return 0;
        }
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        int cap = MycelialConfig.HOMEOSTASIS_MAX_CYSTS_PER_CHUNK.get();
        if (cap <= 0) {
            return 0;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();
        for (BlockPos pylonPos : tracker.getPylonsInChunk(chunkPos.x, chunkPos.z)) {
            if (level.isLoaded(pylonPos)
                    && level.getBlockState(pylonPos).is(ModBlocks.PURIFICATION_PYLON.get())
                    && !tracker.isPylonSmothered(pylonPos)) {
                targets.add(pylonPos);
            }
        }

        List<BlockPos> foreignSnapshot = data.getForeignBlocksSnapshot(chunkPos);
        List<BlockPos> turretTargets = new ArrayList<>();
        List<BlockPos> regularTargets = new ArrayList<>();
        for (BlockPos fPos : foreignSnapshot) {
            if (!level.isLoaded(fPos)) {
                continue;
            }
            BlockState st = level.getBlockState(fPos);
            if (!isForeignBlock(st)) {
                data.removeForeignBlock(fPos);
                continue;
            }
            if (st.is(ModBlocks.PURIFICATION_TURRET.get())) {
                turretTargets.add(fPos);
            } else if (!st.is(ModBlocks.PURIFICATION_PYLON.get())) {
                regularTargets.add(fPos);
            }
        }
        targets.addAll(turretTargets);
        targets.addAll(regularTargets);

        int placed = 0;
        for (BlockPos target : targets) {
            if (placed >= maxPlacements) {
                break;
            }
            if (isNearProtectedAnchor(level, tracker, target)) {
                continue;
            }
            BlockState targetState = level.getBlockState(target);
            if (targetState.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                    || targetState.is(ModBlocks.REINFORCED_BIO_BARRIER.get())) {
                continue;
            }

            List<Direction> dirs = new ArrayList<>(List.of(
                    Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN
            ));
            if (!deterministic) {
                Collections.shuffle(dirs, new java.util.Random(level.random.nextLong()));
            }
            for (Direction dir : dirs) {
                if (placed >= maxPlacements) {
                    break;
                }
                BlockPos wrapPos = target.relative(dir);
                ChunkPos destinationChunk = new ChunkPos(wrapPos);
                if (data.getActiveCystCount(destinationChunk) >= cap) {
                    continue;
                }
                if (!canPlaceAntibodyCyst(level, wrapPos)) {
                    continue;
                }
                if (!level.setBlock(wrapPos, ModBlocks.ANTIBODY_CYST.get().defaultBlockState(), 3)) {
                    continue;
                }
                level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                        wrapPos.getX() + 0.5, wrapPos.getY() + 0.5, wrapPos.getZ() + 0.5,
                        8, 0.25, 0.25, 0.25, 0.02);
                level.sendParticles(ModParticles.ACID_DROPLET.get(),
                        wrapPos.getX() + 0.5, wrapPos.getY() + 0.5, wrapPos.getZ() + 0.5,
                        6, 0.25, 0.25, 0.25, 0.02);
                level.playSound(null, wrapPos, ModSounds.ANTIBODY_CYST_ENCAPSULATE.get(), SoundSource.BLOCKS, 0.65F, 0.85F);
                placed++;

                if (level.getBlockEntity(target) instanceof PurificationTurretBlockEntity turret
                        && AntibodyCystBlock.countAdjacentCysts(level, target) >= 2) {
                    turret.applyCorrosiveJam(100);
                }
            }
        }
        return placed;
    }

    private static boolean canPlaceAntibodyCyst(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
            return false;
        }
        if (level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return false;
        }
        if (!existing.getFluidState().isEmpty()) {
            return false;
        }
        if (hasNearbyBlock(level, pos, ModBlocks.MYCELIAL_RIFT.get(), 2)) {
            return false;
        }
        List<Player> blockingPlayers = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(0.1D));
        return blockingPlayers.isEmpty();
    }

    private static boolean isNearProtectedAnchor(ServerLevel level, PurificationZoneTracker tracker, BlockPos pos) {
        if (tracker.hasNearbySymbioticNode(pos, 3)) {
            return true;
        }
        return hasNearbyBlock(level, pos, ModBlocks.MYCELIAL_RIFT.get(), 2);
    }

    private static boolean hasNearbyBlock(ServerLevel level, BlockPos center, net.minecraft.world.level.block.Block block, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.getBlockState(p).is(block)) {
                return true;
            }
        }
        return false;
    }

    public static void runAcidRainTick(ServerLevel level) {
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        Set<UUID> affectedEntities = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            ChunkPos cp = new ChunkPos(player.blockPosition());
            double stress = data.getEffectiveNeighborhoodStress(cp);
            if (DimensionalHomeostasisEngine.stageForStress(stress) != HomeostasisStage.IMMUNE_RESPONSE) {
                continue;
            }
            spawnAmbientCeilingAcidColumns(level, player.blockPosition());
            applyAcidRainPulseToEntity(level, player, stress);

            AABB area = player.getBoundingBox().inflate(24.0D);
            for (MycelialMob mob : level.getEntitiesOfClass(MycelialMob.class, area)) {
                if (mob.isAlive() && affectedEntities.add(mob.getUUID())
                        && isExposedToCeilingAcidRain(level, mob.blockPosition())) {
                    mob.heal(1.5F);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
                    level.sendParticles(ModParticles.SPORE_FALL.get(),
                            mob.getX(), mob.getY() + 1.0, mob.getZ(), 4, 0.3, 0.3, 0.3, 0.01);
                }
            }
            for (PurifierDroneEntity drone : level.getEntitiesOfClass(PurifierDroneEntity.class, area)) {
                if (drone.isAlive() && affectedEntities.add(drone.getUUID())
                        && isExposedToCeilingAcidRain(level, drone.blockPosition())) {
                    drone.hurt(level.damageSources().magic(), 1.0F);
                    drone.setJammedTicks(Math.max(drone.getJammedTicks(), 60));
                }
            }
            for (MycelialChimeraEntity chimera : level.getEntitiesOfClass(MycelialChimeraEntity.class, area)) {
                if (chimera.isAlive() && affectedEntities.add(chimera.getUUID())
                        && isExposedToCeilingAcidRain(level, chimera.blockPosition())) {
                    float delta = LivingBattlefieldSystem.computeChimeraAcidRainDelta(
                            chimera.hasGene(AdaptationTrait.FIRE_RESISTANT),
                            chimera.hasGene(AdaptationTrait.MAGIC_CARAPACE)
                    );
                    if (delta > 0.0F) {
                        chimera.heal(delta);
                        level.sendParticles(ModParticles.SPORE_FALL.get(),
                                chimera.getX(), chimera.getY() + 1.0, chimera.getZ(), 4, 0.3, 0.3, 0.3, 0.01);
                    } else if (delta < 0.0F) {
                        chimera.hurt(level.damageSources().magic(), -delta);
                        level.sendParticles(ModParticles.ACID_DROPLET.get(),
                                chimera.getX(), chimera.getY() + 1.0, chimera.getZ(), 4, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }
            for (ColonySymbiontEntity symbiont : level.getEntitiesOfClass(ColonySymbiontEntity.class, area)) {
                if (symbiont.isAlive() && affectedEntities.add(symbiont.getUUID())
                        && isExposedToCeilingAcidRain(level, symbiont.blockPosition())) {
                    SovereignColonyData symbiontColony = symbiont.getColonyData(level);
                    boolean protectedByColony = (symbiontColony != null
                            && symbiontColony.hasTrait(SovereignTrait.APEX_GUARDIAN_MATRIX))
                            || PurificationZoneTracker.get(level).hasNearbySymbioticNode(symbiont.blockPosition(), 16);
                    if (protectedByColony) {
                        symbiont.heal(1.0F);
                        level.sendParticles(ModParticles.SPORE_FALL.get(),
                                symbiont.getX(), symbiont.getY() + 0.8, symbiont.getZ(), 3, 0.25, 0.25, 0.25, 0.01);
                    } else {
                        symbiont.hurt(level.damageSources().magic(), 0.5F);
                        level.sendParticles(ModParticles.ACID_DROPLET.get(),
                                symbiont.getX(), symbiont.getY() + 0.8, symbiont.getZ(), 3, 0.25, 0.25, 0.25, 0.01);
                    }
                }
            }
        }

        for (long key : data.getTrackedChunkKeys()) {
            ChunkPos cp = new ChunkPos(key);
            if (data.getStage(cp) != HomeostasisStage.IMMUNE_RESPONSE) {
                continue;
            }
            for (BlockPos fPos : data.getForeignBlocksSnapshot(cp)) {
                if (level.isLoaded(fPos) && level.getBlockEntity(fPos) instanceof PurificationTurretBlockEntity) {
                    applyAcidRainCorrosionToTurret(level, fPos);
                }
            }
        }
    }

    public static boolean applyAcidRainCorrosionToTurret(ServerLevel level, BlockPos turretPos) {
        if (!(level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity turret)) {
            return false;
        }
        if (!isExposedToCeilingAcidRain(level, turretPos)) {
            return false;
        }
        turret.applyCorrosiveJam(80);
        level.sendParticles(ModParticles.ACID_DROPLET.get(),
                turretPos.getX() + 0.5, turretPos.getY() + 1.1, turretPos.getZ() + 0.5,
                5, 0.25, 0.15, 0.25, 0.02);
        return true;
    }

    public static boolean isExposedToCeilingAcidRain(ServerLevel level, BlockPos feetPos) {
        int maxScan = level.getMaxBuildHeight() - 1;
        for (int y = feetPos.getY() + 2; y <= maxScan; y++) {
            BlockPos scanPos = new BlockPos(feetPos.getX(), y, feetPos.getZ());
            BlockState state = level.getBlockState(scanPos);
            if (!state.getCollisionShape(level, scanPos).isEmpty()) {
                return isNaturalCavernCeiling(state);
            }
        }
        return true;
    }

    public static int findCeilingY(ServerLevel level, BlockPos feetPos) {
        int maxScan = level.getMaxBuildHeight() - 1;
        for (int y = feetPos.getY() + 2; y <= maxScan; y++) {
            BlockPos scanPos = new BlockPos(feetPos.getX(), y, feetPos.getZ());
            BlockState state = level.getBlockState(scanPos);
            if (!state.getCollisionShape(level, scanPos).isEmpty()) {
                return y - 1;
            }
        }
        return Math.min(level.getMaxBuildHeight() - 2, feetPos.getY() + 18);
    }

    private static void spawnAmbientCeilingAcidColumns(ServerLevel level, BlockPos center) {
        for (int i = 0; i < 6; i++) {
            int ox = level.random.nextIntBetweenInclusive(-8, 8);
            int oz = level.random.nextIntBetweenInclusive(-8, 8);
            BlockPos sample = center.offset(ox, 0, oz);
            int topY = findCeilingY(level, sample);
            int startY = Math.min(topY, sample.getY() + 10);
            for (int y = startY; y >= sample.getY(); y -= 3) {
                level.sendParticles(ModParticles.ACID_DROPLET.get(),
                        sample.getX() + 0.5, y + 0.5, sample.getZ() + 0.5,
                        1, 0.15, 0.2, 0.15, 0.01);
            }
        }
    }

    public static boolean applyAcidRainPulseToEntity(ServerLevel level, LivingEntity target, double stress) {
        BlockPos feetPos = target.blockPosition();
        if (!isExposedToCeilingAcidRain(level, feetPos)) {
            int shelterY = findCeilingY(level, feetPos) + 2;
            level.sendParticles(ModParticles.ACID_DROPLET.get(),
                    target.getX(), shelterY, target.getZ(),
                    4, 0.35, 0.05, 0.35, 0.02);
            return false;
        }

        int ceilingY = findCeilingY(level, feetPos);
        int startY = Math.min(ceilingY, feetPos.getY() + 10);
        for (int y = startY; y >= feetPos.getY() + 1; y -= 2) {
            level.sendParticles(ModParticles.ACID_DROPLET.get(),
                    target.getX(), y + 0.4, target.getZ(),
                    2, 0.2, 0.1, 0.2, 0.01);
            level.sendParticles(ModParticles.ACID_DROPLET.get(),
                    target.getX(), y + 0.2, target.getZ(),
                    1, 0.2, 0.1, 0.2, 0.01);
        }
        level.playSound(null, feetPos, ModSounds.ACID_RAIN_SIZZLE.get(), SoundSource.WEATHER, 0.45F, 1.55F);

        int chitinPieces = ChitinArmorItem.countEquippedPieces(target);
        boolean fullChitin = ChitinArmorItem.hasFullSet(target);
        float dmg = DimensionalHomeostasisEngine.computeAcidRainDamage(
                MycelialConfig.HOMEOSTASIS_ACID_RAIN_DAMAGE.get().floatValue(),
                chitinPieces,
                fullChitin
        );
        target.hurt(level.damageSources().magic(), dmg);

        if (!fullChitin) {
            damageOneNonChitinArmorPiece(level, target);
            int infectionTicks = ChitinArmorItem.reduceInfectionDuration(target, 80);
            target.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, infectionTicks, 0));
            target.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 60, 0));
        }
        return true;
    }

    private static void damageOneNonChitinArmorPiece(ServerLevel level, LivingEntity target) {
        List<EquipmentSlot> candidates = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = target.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem() && !(armor.getItem() instanceof ChitinArmorItem)) {
                candidates.add(slot);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        EquipmentSlot chosen = candidates.get(level.random.nextInt(candidates.size()));
        target.getItemBySlot(chosen).hurtAndBreak(1, target, chosen);
    }

    public static boolean sedateArea(
            ServerLevel level,
            BlockPos origin,
            int chunkRadius,
            double amount,
            @Nullable ServerPlayer actor
    ) {
        return sedateArea(level, origin, chunkRadius, amount, 0, actor);
    }

    public static boolean sedateArea(
            ServerLevel level,
            BlockPos origin,
            int chunkRadius,
            double amount,
            int sensitizationReduction,
            @Nullable ServerPlayer actor
    ) {
        if (!MycelialConfig.HOMEOSTASIS_ENABLED.get()) {
            return false;
        }
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        ChunkPos center = new ChunkPos(origin);
        boolean calmedFromImmune = data.sedateArea(
                center, chunkRadius, amount, sensitizationReduction, level.getGameTime()
        );
        checkAndAnnounceTransition(level, center, data.getStress(center));

        level.sendParticles(ModParticles.SPORE_FALL.get(),
                origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
                24, 2.5, 1.0, 2.5, 0.04);

        if (calmedFromImmune && actor != null) {
            grantHomeostasisAdvancement(actor);
            actor.sendSystemMessage(Component.translatable("message.mycelialmind.homeostasis_sedated",
                    String.format("%.0f", data.getStress(center))).withStyle(ChatFormatting.AQUA));
        }
        for (ServerPlayer sp : level.players()) {
            PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(level, sp));
        }
        return calmedFromImmune;
    }

    public static void grantHomeostasisAdvancement(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AdvancementHolder holder = serverLevel.getServer().getAdvancements().get(IMMUNE_HOMEOSTASIS_ADVANCEMENT);
        if (holder != null) {
            player.getAdvancements().award(holder, "overcome_immune_response");
        }
    }

    private static BlockPos chunkAnchorPos(
            ServerLevel level,
            DimensionalHomeostasisSavedData data,
            PurificationZoneTracker tracker,
            ChunkPos chunkPos
    ) {
        List<BlockPos> pylons = tracker.getPylonsInChunk(chunkPos.x, chunkPos.z);
        if (!pylons.isEmpty()) {
            return pylons.get(0);
        }
        List<BlockPos> foreign = data.getForeignBlocksSnapshot(chunkPos);
        for (BlockPos pos : foreign) {
            if (level.isLoaded(pos) && isForeignBlock(level.getBlockState(pos))) {
                return pos;
            }
            if (level.isLoaded(pos)) {
                data.removeForeignBlock(pos);
            }
        }
        for (ServerPlayer player : level.players()) {
            if (new ChunkPos(player.blockPosition()).equals(chunkPos)) {
                return player.blockPosition();
            }
        }
        return chunkPos.getMiddleBlockPosition(64);
    }

    private static boolean hasLiveForeignBlocks(
            ServerLevel level,
            DimensionalHomeostasisSavedData data,
            ChunkPos chunkPos
    ) {
        for (BlockPos pos : data.getForeignBlocksSnapshot(chunkPos)) {
            if (!level.isLoaded(pos)) {
                return true;
            }
            if (isForeignBlock(level.getBlockState(pos))) {
                return true;
            }
            data.removeForeignBlock(pos);
        }
        return false;
    }

    private static void reconcileForeignBlocksInChunk(
            ServerLevel level,
            DimensionalHomeostasisSavedData data,
            ChunkPos chunkPos
    ) {
        for (BlockPos pos : data.getForeignBlocksSnapshot(chunkPos)) {
            if (level.isLoaded(pos) && !isForeignBlock(level.getBlockState(pos))) {
                data.removeForeignBlock(pos);
            }
        }
    }

    private static int countTurretsNear(
            ServerLevel level,
            DimensionalHomeostasisSavedData data,
            BlockPos center,
            int radius
    ) {
        int chunkRad = (radius >> 4) + 1;
        ChunkPos originChunk = new ChunkPos(center);
        double maxSq = (double) radius * radius;
        int count = 0;
        for (int dx = -chunkRad; dx <= chunkRad; dx++) {
            for (int dz = -chunkRad; dz <= chunkRad; dz++) {
                ChunkPos cp = new ChunkPos(originChunk.x + dx, originChunk.z + dz);
                for (BlockPos pos : data.getForeignBlocksSnapshot(cp)) {
                    if (pos.distSqr(center) <= maxSq
                            && level.isLoaded(pos)
                            && level.getBlockState(pos).is(ModBlocks.PURIFICATION_TURRET.get())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int countSymbioticNodesNear(PurificationZoneTracker tracker, BlockPos center, int radius) {
        int count = 0;
        for (var entry : tracker.getNodesWithin(center, radius, 16)) {
            if (entry.getValue() == SynapticNodeType.SYMBIOTIC) {
                count++;
            }
        }
        return count;
    }
}
