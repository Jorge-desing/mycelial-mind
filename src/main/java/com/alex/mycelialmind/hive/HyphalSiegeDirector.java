package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.block.CreepingMyceliumBlock;
import com.alex.mycelialmind.block.MycelialNodeBlock;
import com.alex.mycelialmind.entity.HyphalVeinEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.hive.HiveMindSavedData.BlindSpotOutbreakSite;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.BlueprintVoxel;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.BlueprintVoxelRole;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.alex.mycelialmind.world.structure.ArchitecturalStructureAdapter;
import com.alex.mycelialmind.world.structure.ArchitecturalStructureAdapter.ResolvedPalette;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime director uniting the three adaptive spatial-behavioral systems:
 * 1) Coarse chunk Hyphal Route Network density, floor vein sprouting, ambushes & tool severing,
 * 2) Player Architectural Accent sampling (block palette, room span, H/W ratio, lighting),
 * 3) Blind-Spot live block-by-block outbreak growth, photophobic stalling & vulnerable core loop.
 */
public final class HyphalSiegeDirector {
    private static final long ROUTE_SAMPLE_INTERVAL_TICKS = 40L;
    private static final long BLIND_SPOT_TICK_INTERVAL = 100L;
    private static final long BLIND_SPOT_NUCLEATION_INTERVAL = 200L;

    private static final Map<UUID, Long> LAST_ROUTE_SAMPLE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PLAYER_CHUNK = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> LAST_PLAYER_GROUND_BLOCK = new ConcurrentHashMap<>();

    private HyphalSiegeDirector() {
    }

    private static boolean hasForeignBlockEntity(LevelReader level, BlockPos pos) {
        return level.getBlockEntity(pos) != null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)
                || player.isSpectator()
                || player.isCreative()) {
            return;
        }
        long now = serverLevel.getGameTime();
        Long last = LAST_ROUTE_SAMPLE_TICK.get(player.getUUID());
        if (last != null && (now - last) < ROUTE_SAMPLE_INTERVAL_TICKS) {
            BlockPos groundPos = findPlayerSurfaceAnchor(serverLevel, player);
            if (groundPos != null && !Objects.equals(LAST_PLAYER_GROUND_BLOCK.get(player.getUUID()), groundPos)) {
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
                if (recordTrailMovement(serverLevel, hive, tracker, player, groundPos)) {
                    ChunkPos cp = new ChunkPos(groundPos);
                    if (!tracker.isProtected(groundPos)
                            && !hive.isMatriarchRemissionActive(now)
                            && (HyphalAdaptiveArchitectureEngine.shouldSproutRouteVein(hive.getHyphalDensity(cp), false)
                            || hive.hasRootCandidateInChunk(cp, HyphalPhysics.getRootTraversalThreshold()))) {
                        sproutRouteVeinsInChunk(serverLevel, hive, cp);
                    }
                }
            }
            return;
        }
        LAST_ROUTE_SAMPLE_TICK.put(player.getUUID(), now);
        recordPlayerRouteFootstep(serverLevel, player);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        long gameTime = serverLevel.getGameTime();
        if (gameTime % BLIND_SPOT_TICK_INTERVAL != 0L) {
            return;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        tickBlindSpotOutbreaks(serverLevel, hive);

        if (gameTime % BLIND_SPOT_NUCLEATION_INTERVAL == 0L && !hive.isMatriarchRemissionActive(gameTime)) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player.isSpectator() || player.isCreative()) {
                    continue;
                }
                double density = hive.getHyphalDensityAt(player.blockPosition());
                boolean nearStoredBase = false;
                if (hive.hasDenseHyphalRoutes()) {
                    for (BlockPos base : hive.getBaseAnchors()) {
                        if (base.distSqr(player.blockPosition()) <= 4096.0D) {
                            nearStoredBase = true;
                            break;
                        }
                    }
                }
                double hyphalThreshold = HiveCycleDirector.getActiveHyphalPressureMultiplier(serverLevel) > 1.05D ? 22.0 : 30.0;
                if (density >= hyphalThreshold || nearStoredBase) {
                    tryNucleateBlindSpotNear(serverLevel, hive, player.blockPosition());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof Player player)
                || player.isSpectator()) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState placed = event.getPlacedBlock();
        recordPlayerArchitecturePlacement(serverLevel, player, pos, placed);

        if (placed.getLightEmission() >= 8 || placed.is(ModBlocks.PURIFICATION_PYLON.get())) {
            checkActiveBlindSpotsForPhotophobia(serverLevel, pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);

        BlindSpotOutbreakSite site = hive.getBlindSpotSite(pos);
        if (site != null && !site.isCompleted()) {
            destroyBlindSpotVulnerableCore(serverLevel, hive, site, event.getPlayer());
            return;
        }

        if (state.is(ModBlocks.CREEPING_MYCELIUM.get())
                || state.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())) {
            Player player = event.getPlayer();
            ItemStack tool = player != null ? player.getMainHandItem() : ItemStack.EMPTY;
            if (isSeveringTool(tool) || hive.isTrackedHyphalVein(pos)) {
                trySeverHyphalVein(serverLevel, pos, player, tool);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack tool = event.getItemStack();
        if (!isRightClickSeveringTool(tool)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = serverLevel.getBlockState(pos);
        boolean hasNearbyStrand = !serverLevel.getEntitiesOfClass(
                HyphalVeinEntity.class, new AABB(pos).inflate(1.5D)).isEmpty();
        if (!hasNearbyStrand
                && !state.is(ModBlocks.CREEPING_MYCELIUM.get())
                && !state.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())) {
            return;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        if (!hasNearbyStrand && state.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get()) && !hive.isTrackedHyphalVein(pos)) {
            return;
        }
        if (trySeverHyphalVein(serverLevel, pos, player, tool) > 0) {
            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            event.setCancellationResult(InteractionResult.sidedSuccess(false));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = event.getExplosion().center();
        AABB box = new AABB(center, center).inflate(6.5D);
        for (HyphalVeinEntity strand : serverLevel.getEntitiesOfClass(HyphalVeinEntity.class, box)) {
            double dist = Math.sqrt(strand.getMidpointNode().distanceToSqr(center));
            if (dist <= 6.5D) {
                float amp = Mth.clamp(1.0F - (float) (dist / 7.0D), 0.15F, 1.0F);
                strand.injectWaveNear(center, amp);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            LAST_ROUTE_SAMPLE_TICK.clear();
            LAST_PLAYER_CHUNK.clear();
            LAST_PLAYER_GROUND_BLOCK.clear();
        }
    }

    public static double recordPlayerRouteFootstep(ServerLevel level, Player player) {
        BlockPos groundPos = findPlayerSurfaceAnchor(level, player);
        if (groundPos == null) {
            return 0.0;
        }
        ChunkPos chunkPos = new ChunkPos(groundPos);
        long chunkKey = chunkPos.toLong();
        Long prevChunk = LAST_PLAYER_CHUNK.put(player.getUUID(), chunkKey);
        boolean changedChunk = !Objects.equals(prevChunk, chunkKey);

        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        boolean inProtected = tracker.isProtected(groundPos);
        boolean nearSymbiotic = tracker.hasNearbySymbioticNode(groundPos, 16);
        boolean crouching = player.isCrouching() || player.isShiftKeyDown();

        HiveMindSavedData hive = HiveMindSavedData.get(level);
        boolean inRemission = hive.isMatriarchRemissionActive(level.getGameTime());
        if (inRemission && (level.getGameTime() / ROUTE_SAMPLE_INTERVAL_TICKS) % 2L != 0L) {
            return hive.getHyphalDensity(chunkPos);
        }

        recordTrailMovement(level, hive, tracker, player, groundPos);
        double density = hive.recordRouteStep(
                chunkPos, groundPos, changedChunk, inProtected, nearSymbiotic, crouching, level.getGameTime(), false);

        HiveBlackboard.getInfluenceMap(level).addInfluence(groundPos, 0.08F, 0.0F, 0.12F);

        if (HyphalAdaptiveArchitectureEngine.shouldSproutRouteVein(density, inProtected)
                || (!inProtected && !nearSymbiotic && !inRemission
                && hive.hasRootCandidateInChunk(chunkPos, HyphalPhysics.getRootTraversalThreshold()))) {
            sproutRouteVeinsInChunk(level, hive, chunkPos);
        }

        boolean nearVein = isNearHyphalVein(level, hive, player.blockPosition());
        if (nearVein && !inProtected && !inRemission && hive.canTriggerRouteAmbush(chunkPos, level.getGameTime())) {
            spawnRouteTacticalAmbush(level, hive, player, groundPos);
        }
        return density;
    }

    private static boolean recordTrailMovement(
            ServerLevel level,
            HiveMindSavedData hive,
            PurificationZoneTracker tracker,
            Player player,
            BlockPos anchorPos
    ) {
        BlockPos prev = LAST_PLAYER_GROUND_BLOCK.put(player.getUUID(), anchorPos.immutable());
        if (Objects.equals(prev, anchorPos)) {
            return false;
        }
        if (tracker.isProtected(anchorPos) || tracker.hasNearbySymbioticNode(anchorPos, 16)) {
            return false;
        }
        long gameTime = level.getGameTime();
        if (prev != null && !prev.equals(anchorPos)
                && prev.distSqr(anchorPos) <= 256.0D
                && Math.abs(prev.getY() - anchorPos.getY()) <= 16) {
            List<BlockPos> line = HyphalAdaptiveArchitectureEngine.supercoverLine(
                    prev, anchorPos, HyphalPhysics.MAX_SUPERCOVER_STEPS);
            for (int i = 1; i < line.size(); i++) {
                BlockPos stepPos = line.get(i);
                if (!tracker.isProtected(stepPos) && !tracker.hasNearbySymbioticNode(stepPos, 16)) {
                    hive.recordTrailPoint(new ChunkPos(stepPos), stepPos, true, gameTime);
                }
            }
        } else {
            hive.recordTrailPoint(new ChunkPos(anchorPos), anchorPos, true, gameTime);
        }
        return true;
    }

    public record VeinPlacement(BlockPos veinPos, byte faceBit, BlockPos supportPos) {
    }

    private static final int MAX_STRANDS_PER_CHUNK = 8;

    public static List<HyphalVeinEntity> getChunkHyphalStrands(ServerLevel level, ChunkPos chunkPos) {
        AABB chunkBox = new AABB(
                chunkPos.getMinBlockX() - 1, level.getMinBuildHeight(), chunkPos.getMinBlockZ() - 1,
                chunkPos.getMaxBlockX() + 2, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 2
        );
        return level.getEntitiesOfClass(HyphalVeinEntity.class, chunkBox);
    }

    public static int sproutRouteVeinsInChunk(ServerLevel level, HiveMindSavedData hive, ChunkPos chunkPos) {
        double density = hive.getHyphalDensity(chunkPos);
        int rootThreshold = HyphalPhysics.getRootTraversalThreshold();
        Map<BlockPos, Integer> traversalsMap = hive.getBlockTraversalsInChunk(chunkPos);
        boolean hasRootCandidate = false;
        for (int count : traversalsMap.values()) {
            if (HyphalPhysics.isRootCandidate(count, rootThreshold)) {
                hasRootCandidate = true;
                break;
            }
        }
        if (!hasRootCandidate && density < HyphalAdaptiveArchitectureEngine.VEIN_GROWTH_THRESHOLD) {
            return 0;
        }

        List<BlockPos> orderedTrail = hive.getOrderedTrail(chunkPos);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        List<HyphalVeinEntity> existingStrands = new ArrayList<>(getChunkHyphalStrands(level, chunkPos));
        int placed = 0;

        for (Map.Entry<BlockPos, Integer> entry : traversalsMap.entrySet()) {
            int count = entry.getValue();
            if (!HyphalPhysics.isRootCandidate(count, rootThreshold)) {
                continue;
            }
            BlockPos groundHint = entry.getKey();
            if (!level.isLoaded(groundHint)) {
                continue;
            }
            VeinPlacement placement = resolveVeinPlacementForGround(level, groundHint);
            if (placement == null) {
                continue;
            }
            BlockPos veinPos = placement.veinPos();
            BlockPos supportPos = placement.supportPos();
            if (!level.isInWorldBounds(veinPos)
                    || !level.isLoaded(veinPos)
                    || tracker.isProtected(veinPos)
                    || tracker.isProtected(supportPos)
                    || hasForeignBlockEntity(level, veinPos)
                    || hasForeignBlockEntity(level, supportPos)) {
                continue;
            }

            int strength = Math.max(
                    HyphalPhysics.strengthForTraversals(count),
                    HyphalAdaptiveArchitectureEngine.computeVeinStrength(density));
            int trailIndex = orderedTrail.lastIndexOf(groundHint);
            if (trailIndex < 0) {
                trailIndex = orderedTrail.lastIndexOf(supportPos);
            }

            HyphalVeinEntity covering = null;
            for (HyphalVeinEntity s : existingStrands) {
                if (!s.isRemoved() && (s.getRootAnchor().equals(veinPos) || s.touchesBlockPos(veinPos, 1.35D))) {
                    covering = s;
                    break;
                }
            }
            if (covering != null) {
                covering.setHasRoot(true);
                covering.setStrengthFloor(Math.max(covering.getStrengthFloor(), strength));
                if (trailIndex >= 0 && covering.getTrailIndex() < 0) {
                    covering.setTrailIndex(trailIndex);
                }
                covering.recomputeTurgor(level);
                hive.registerHyphalVein(veinPos);
                continue;
            }

            if (existingStrands.size() >= MAX_STRANDS_PER_CHUNK) {
                break;
            }
            BlockState supportState = level.getBlockState(supportPos);
            BlockState existingState = level.getBlockState(veinPos);
            if (canSproutVeinOver(level, supportPos, supportState) && canPlaceVeinOverlayAt(existingState)) {
                BlockPos initialTip = veinPos;
                if (trailIndex >= 0) {
                    for (int delta : new int[]{1, -1}) {
                        int adjIdx = trailIndex + delta;
                        if (adjIdx >= 0 && adjIdx < orderedTrail.size()) {
                            VeinPlacement nextP = resolveVeinPlacementForGround(level, orderedTrail.get(adjIdx));
                            if (nextP != null && !tracker.isProtected(nextP.veinPos())) {
                                initialTip = nextP.veinPos();
                                break;
                            }
                        }
                    }
                }
                if (initialTip.equals(veinPos)) {
                    initialTip = veinPos.east();
                }

                HyphalVeinEntity strand = new HyphalVeinEntity(level, veinPos, initialTip, 3, true, 0.65F);
                strand.setStrengthFloor(strength);
                strand.setTrailIndex(trailIndex);
                strand.recomputeTurgor(level);
                level.addFreshEntity(strand);
                existingStrands.add(strand);
                hive.registerHyphalVein(veinPos);
                placed++;
            }
        }
        return placed;
    }

    public static boolean growStrandOneStep(ServerLevel level, HyphalVeinEntity strand) {
        if (!strand.hasRoot() || strand.getNodeCount() >= HyphalVerletRope.MAX_NODE_COUNT) {
            return false;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        if (hive.isMatriarchRemissionActive(level.getGameTime())) {
            return false;
        }
        BlockPos tipPos = strand.getTipAnchor();
        ChunkPos chunkPos = new ChunkPos(tipPos);
        List<BlockPos> trail = hive.getOrderedTrail(chunkPos);
        if (trail.isEmpty()) {
            return false;
        }
        int idx = strand.getTrailIndex();
        if (idx < 0 || idx >= trail.size()) {
            idx = findNearestTrailIndex(trail, tipPos.below());
            if (idx < 0) {
                return false;
            }
            strand.setTrailIndex(idx);
        }

        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        for (int delta : new int[]{1, -1}) {
            int candidateIdx = idx + delta;
            if (candidateIdx < 0 || candidateIdx >= trail.size()) {
                continue;
            }
            BlockPos nextGround = trail.get(candidateIdx);
            if (!level.isLoaded(nextGround)) {
                continue;
            }
            VeinPlacement placement = resolveVeinPlacementForGround(level, nextGround);
            if (placement == null) {
                continue;
            }
            BlockPos nextVeinPos = placement.veinPos();
            BlockPos nextSupport = placement.supportPos();
            if (!level.isInWorldBounds(nextVeinPos)
                    || !level.isLoaded(nextVeinPos)
                    || tracker.isProtected(nextVeinPos)
                    || tracker.isProtected(nextSupport)
                    || hasForeignBlockEntity(level, nextVeinPos)
                    || hasForeignBlockEntity(level, nextSupport)) {
                continue;
            }
            if (strand.touchesBlockPos(nextVeinPos, 0.70D)) {
                continue;
            }
            BlockState supportState = level.getBlockState(nextSupport);
            BlockState targetState = level.getBlockState(nextVeinPos);
            if (canSproutVeinOver(level, nextSupport, supportState) && canPlaceVeinOverlayAt(targetState)) {
                if (strand.extendTipTo(nextVeinPos)) {
                    strand.setTrailIndex(candidateIdx);
                    strand.recomputeTurgor(level);
                    hive.registerHyphalVein(nextVeinPos);
                    return true;
                }
            }
        }
        return false;
    }

    public static int growTrailVeinsNow(
            ServerLevel level,
            HiveMindSavedData hive,
            ChunkPos chunkPos,
            int maxSteps
    ) {
        int grownTotal = 0;
        int limit = Math.max(1, maxSteps);
        for (int step = 0; step < limit; step++) {
            boolean anyGrownThisStep = false;
            for (HyphalVeinEntity strand : getChunkHyphalStrands(level, chunkPos)) {
                if (grownTotal >= limit) {
                    break;
                }
                if (!strand.isRemoved() && growStrandOneStep(level, strand)) {
                    grownTotal++;
                    anyGrownThisStep = true;
                }
            }
            if (!anyGrownThisStep || grownTotal >= limit) {
                break;
            }
        }
        return grownTotal;
    }

    private static int findNearestTrailIndex(List<BlockPos> trail, BlockPos groundPos) {
        for (int i = trail.size() - 1; i >= 0; i--) {
            BlockPos tp = trail.get(i);
            if (Math.abs(tp.getX() - groundPos.getX()) <= 2
                    && Math.abs(tp.getY() - groundPos.getY()) <= 2
                    && Math.abs(tp.getZ() - groundPos.getZ()) <= 2) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public static VeinPlacement resolveVeinPlacementForGround(ServerLevel level, BlockPos hintPos) {
        BlockPos surface = findNaturalVeinGround(level, hintPos);
        if (surface != null) {
            return new VeinPlacement(surface.above(), HyphalPhysics.FACE_DOWN, surface);
        }
        if (level.isInWorldBounds(hintPos) && level.isLoaded(hintPos)) {
            BlockState hintState = level.getBlockState(hintPos);
            if (!hintState.isAir() && !hintState.liquid()) {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    if (hintState.isFaceSturdy(level, hintPos, dir)) {
                        BlockPos wallVeinPos = hintPos.relative(dir);
                        if (level.isInWorldBounds(wallVeinPos) && level.isLoaded(wallVeinPos)) {
                            if (canPlaceVeinOverlayAt(level.getBlockState(wallVeinPos))) {
                                return new VeinPlacement(wallVeinPos, HyphalPhysics.faceBit(dir.getOpposite()), hintPos);
                            }
                        }
                    }
                }
                if (hintState.isFaceSturdy(level, hintPos, Direction.DOWN)) {
                    BlockPos ceilVeinPos = hintPos.below();
                    if (level.isInWorldBounds(ceilVeinPos) && level.isLoaded(ceilVeinPos)) {
                        if (canPlaceVeinOverlayAt(level.getBlockState(ceilVeinPos))) {
                            return new VeinPlacement(ceilVeinPos, HyphalPhysics.FACE_UP, hintPos);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean spawnRouteTacticalAmbush(
            ServerLevel level,
            HiveMindSavedData hive,
            Player player,
            BlockPos origin
    ) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        if (tracker.isProtected(origin)) {
            return false;
        }
        BlockPos spawnPos = findAmbushSpawnPos(level, tracker, origin);
        if (spawnPos == null) {
            return false;
        }

        double density = hive.getHyphalDensityAt(origin);
        boolean highTier = density >= 80.0 || hive.getThreatLevel() >= 50.0;
        int spawned = 0;

        SynapticMimicEntity mimic = ModEntities.SYNAPTIC_MIMIC.get().create(level);
        if (mimic != null) {
            mimic.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            mimic.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
            mimic.applyGlobalHiveState(level, hive);
            mimic.setMimicStance(mimic.evaluateTargetStance(player));
            if (highTier) {
                mimic.promoteRank(EvolutionRank.MUTATED);
            }
            mimic.setTarget(player);
            if (level.noCollision(mimic) && level.addFreshEntity(mimic)) {
                spawned++;
            }
        }

        EntityType<? extends MycelialMob> partnerType = pickAmbushPartnerType(hive);
        BlockPos partnerPos = findAmbushSpawnPos(level, tracker, spawnPos);
        if (partnerPos != null) {
            MycelialMob partner = partnerType.create(level);
            if (partner != null) {
                partner.moveTo(partnerPos.getX() + 0.5, partnerPos.getY(), partnerPos.getZ() + 0.5,
                        level.random.nextFloat() * 360.0F, 0.0F);
                partner.finalizeSpawn(level, level.getCurrentDifficultyAt(partnerPos), MobSpawnType.EVENT, null);
                partner.applyGlobalHiveState(level, hive);
                if (highTier) {
                    partner.promoteRank(EvolutionRank.MUTATED);
                }
                partner.setTarget(player);
                if (level.noCollision(partner) && level.addFreshEntity(partner)) {
                    spawned++;
                }
            }
        }

        if (spawned > 0) {
            hive.markRouteAmbushTriggered(new ChunkPos(origin), level.getGameTime());
            level.sendParticles(ModParticles.HIVE_PULSE.get(),
                    spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5,
                    1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                    spawnPos.getX() + 0.5, spawnPos.getY() + 0.6, spawnPos.getZ() + 0.5,
                    14, 0.6, 0.4, 0.6, 0.03);
            level.playSound(null, spawnPos, ModSounds.SWARM_FLANK_CALL.get(), SoundSource.HOSTILE, 0.9F, 0.95F);
            player.displayClientMessage(Component.translatable("message.mycelialmind.hyphal_route_ambush")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
            return true;
        }
        return false;
    }

    public static int trySeverHyphalVein(
            ServerLevel level,
            BlockPos pos,
            @Nullable Player player,
            ItemStack tool
    ) {
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> targets = new ArrayList<>();
        BlockPos start = pos.immutable();
        if (!isSeverableVeinBlock(hive, start, level.getBlockState(start))
                && level.isLoaded(start.above())
                && isSeverableVeinBlock(hive, start.above(), level.getBlockState(start.above()))) {
            start = start.above().immutable();
        }
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && targets.size() < 7) {
            BlockPos current = queue.poll();
            if (!level.isLoaded(current) || hasForeignBlockEntity(level, current)) {
                continue;
            }
            BlockState st = level.getBlockState(current);
            if (!isSeverableVeinBlock(hive, current, st)) {
                continue;
            }
            targets.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        if (dy != 0 && (Math.abs(dx) + Math.abs(dz)) > 1) {
                            continue;
                        }
                        BlockPos next = current.offset(dx, dy, dz);
                        if (visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        int severedCount = 0;
        Vec3 hitCenter = Vec3.atCenterOf(pos);
        List<HyphalVeinEntity> strands = level.getEntitiesOfClass(
                HyphalVeinEntity.class, new AABB(pos).inflate(2.2D));
        for (HyphalVeinEntity strand : strands) {
            if (!strand.isRemoved() && strand.touchesBlockPos(pos, 2.2D)) {
                severedCount += strand.severAt(hitCenter, player, tool);
            }
        }

        if (targets.isEmpty() && severedCount == 0) {
            return 0;
        }

        boolean specialized = isSpecializedMycelialTool(tool);
        boolean cutting = isRightClickSeveringTool(tool) || isSeveringTool(tool);

        for (BlockPos target : targets) {
            BlockState st = level.getBlockState(target);
            if (st.is(ModBlocks.CREEPING_MYCELIUM.get()) || st.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())) {
                level.setBlockAndUpdate(target, Blocks.COARSE_DIRT.defaultBlockState());
                if (level.random.nextFloat() < 0.35F) {
                    Block.popResource(level, target.above(), new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                }
            } else {
                level.removeBlock(target, false);
            }
            hive.severHyphalVein(target, 0.0, level.getGameTime());
            severedCount++;
        }

        double densityDrop = HyphalAdaptiveArchitectureEngine.computeVeinSeverDensityDrop(specialized, cutting, severedCount);
        double remaining = hive.severHyphalVein(pos, densityDrop, level.getGameTime());

        level.sendParticles(ModParticles.PURGE.get(),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                14, 0.5, 0.3, 0.5, 0.04);
        level.sendParticles(ModParticles.CHITIN_SHARD.get(),
                pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                8, 0.4, 0.2, 0.4, 0.04);
        level.playSound(null, pos, ModSounds.SYMBIOTIC_BLADE_SLASH.get(), SoundSource.PLAYERS, 0.85F, 1.15F);

        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    "message.mycelialmind.hyphal_vein_severed",
                    severedCount,
                    String.format("%.0f", remaining)
            ).withStyle(ChatFormatting.AQUA), true);
        }
        return severedCount;
    }

    public static boolean cleanseHyphalVeinAt(ServerLevel level, BlockPos pos, double densityDrop) {
        List<HyphalVeinEntity> strands = level.getEntitiesOfClass(
                HyphalVeinEntity.class, new AABB(pos).inflate(1.8D));
        boolean cleansed = false;
        for (HyphalVeinEntity strand : strands) {
            if (!strand.isRemoved() && strand.touchesBlockPos(pos, 1.8D)) {
                strand.disintegrateWithSpores();
                cleansed = true;
            }
        }
        if (!cleansed) {
            return false;
        }
        HiveMindSavedData.get(level).severHyphalVein(pos, Math.max(0.0D, densityDrop), level.getGameTime());
        return true;
    }

    public static void recordPlayerArchitecturePlacement(
            ServerLevel level,
            @Nullable Player player,
            BlockPos pos,
            BlockState placedState
    ) {
        if (placedState == null || placedState.isAir() || isHiveOrNaturalNoiseBlock(placedState)) {
            return;
        }
        boolean isLight = placedState.getLightEmission() >= 8;
        boolean isPillar = placedState.getBlock() instanceof RotatedPillarBlock
                || placedState.is(BlockTags.LOGS)
                || placedState.is(BlockTags.WALLS)
                || placedState.is(BlockTags.FENCES);
        boolean isStructural = !isLight && (placedState.is(BlockTags.PLANKS)
                || placedState.is(BlockTags.LOGS)
                || placedState.is(BlockTags.STONE_BRICKS)
                || placedState.isCollisionShapeFullBlock(level, pos)
                || placedState.getBlock() instanceof StairBlock
                || placedState.getBlock() instanceof SlabBlock);
        if (!isStructural && !isPillar && !isLight) {
            return;
        }

        String blockId = BuiltInRegistries.BLOCK.getKey(placedState.getBlock()).toString();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.recordArchitecturalBlockPlacement(blockId, isPillar, isLight);
        hive.recordBaseAnchor(pos);
        double[] geom = measureLocalArchitectureGeometry(level, pos);
        hive.recordArchitecturalGeometry(geom[0], geom[1], geom[2]);
    }

    public static double[] measureLocalArchitectureGeometry(ServerLevel level, BlockPos pos) {
        int east = scanWallDistance(level, pos, Direction.EAST, 12);
        int west = scanWallDistance(level, pos, Direction.WEST, 12);
        int north = scanWallDistance(level, pos, Direction.NORTH, 12);
        int south = scanWallDistance(level, pos, Direction.SOUTH, 12);

        double spanX = (east > 0 && west > 0) ? (east + west + 1) : (east > 0 ? east * 2.0 : (west > 0 ? west * 2.0 : 5.0));
        double spanZ = (north > 0 && south > 0) ? (north + south + 1) : (north > 0 ? north * 2.0 : (south > 0 ? south * 2.0 : 5.0));
        double roomSpan = Mth.clamp((spanX + spanZ) * 0.5, 3.0, 14.0);

        double wallHeight = measureMaxLocalWallHeight(level, pos);
        double hToW = Mth.clamp(wallHeight / Math.max(2.0, roomSpan * 0.6), 0.45, 2.6);

        int lightSum = 0;
        int samples = 0;
        for (BlockPos probe : new BlockPos[]{
                pos, pos.above(), pos.north(), pos.south(), pos.east(), pos.west()
        }) {
            if (level.isInWorldBounds(probe)) {
                lightSum += level.getBrightness(LightLayer.BLOCK, probe);
                samples++;
            }
        }
        double localLightDensity = samples > 0 ? Mth.clamp((lightSum / (double) samples) / 15.0, 0.0, 1.0) : 0.5;
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        double combinedLightDensity = HyphalAdaptiveArchitectureEngine.computeCombinedLightDensity(
                hive.getArchitecturalLightBlockCount(),
                hive.getArchitecturalStructuralBlockCount(),
                localLightDensity);
        return new double[]{roomSpan, hToW, combinedLightDensity};
    }

    private static double measureMaxLocalWallHeight(ServerLevel level, BlockPos center) {
        int bestHeight = 1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos col = center.offset(dx, 0, dz);
                if (level.getBlockState(col).isAir() || !level.getBlockState(col).blocksMotion()) {
                    continue;
                }
                int up = 0;
                for (int i = 1; i <= 14; i++) {
                    BlockState s = level.getBlockState(col.above(i));
                    if (s.isAir() || !s.blocksMotion() || isHiveOrNaturalNoiseBlock(s)) {
                        break;
                    }
                    up++;
                }
                int down = 0;
                for (int i = 1; i <= 14; i++) {
                    BlockState s = level.getBlockState(col.below(i));
                    if (s.isAir() || !s.blocksMotion() || isHiveOrNaturalNoiseBlock(s)) {
                        break;
                    }
                    down++;
                }
                bestHeight = Math.max(bestHeight, up + down + 1);
            }
        }
        return bestHeight;
    }

    private static int scanWallDistance(ServerLevel level, BlockPos origin, Direction dir, int maxDist) {
        for (int d = 1; d <= maxDist; d++) {
            BlockPos p = origin.relative(dir, d);
            if (!level.isLoaded(p)) {
                break;
            }
            BlockState state = level.getBlockState(p);
            if (d >= 2 && !state.isAir() && state.blocksMotion() && !isHiveOrNaturalNoiseBlock(state)) {
                return d;
            }
        }
        return 0;
    }

    @Nullable
    public static BlindSpotOutbreakSite tryNucleateBlindSpotNear(
            ServerLevel level,
            HiveMindSavedData hive,
            BlockPos anchor
    ) {
        if (hive.getBlindSpotSites().size() >= HyphalAdaptiveArchitectureEngine.MAX_ACTIVE_BLIND_SPOTS) {
            return null;
        }
        if (hive.findBlindSpotSiteNear(anchor, 24.0) != null) {
            return null;
        }

        List<BlockPos> searchOrigins = new ArrayList<>();
        searchOrigins.add(anchor);
        for (BlockPos base : hive.getBaseAnchors()) {
            if (searchOrigins.size() >= 4) {
                break;
            }
            if (base.distSqr(anchor) <= 9216.0D
                    && level.isLoaded(base)
                    && hive.findBlindSpotSiteNear(base, 24.0) == null) {
                searchOrigins.add(base);
            }
        }

        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos bestPos = null;
        double bestPriority = 0.45;
        final int[] radii = {5, 7, 9, 11, 13, 16};

        for (BlockPos origin : searchOrigins) {
            for (int attempt = 0; attempt < 16; attempt++) {
                double angle = (Math.PI * 2.0 * attempt) / 16.0;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                for (int dist : radii) {
                    int x = origin.getX() + (int) Math.round(cos * dist);
                    int z = origin.getZ() + (int) Math.round(sin * dist);

                    BlockPos candidate = resolveCandidateBlindSpotPos(level, x, origin.getY(), z);
                    if (candidate == null || tracker.isProtected(candidate) || level.getBlockEntity(candidate.below()) != null) {
                        continue;
                    }
                    if (level.dimension().equals(Level.OVERWORLD)
                            && !level.getBlockState(candidate.below()).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                        continue;
                    }
                    int blockLight = measureEffectiveBlockLight(level, candidate);
                    double density = Math.max(hive.getHyphalDensityAt(candidate), hive.getHyphalDensityAt(origin));
                    BlockPos nearestNode = tracker.findNearestNode(candidate);
                    double distNearest = nearestNode != null ? Math.sqrt(nearestNode.distSqr(candidate)) : 64.0;
                    boolean headroom = hasClearHeadroom(level, candidate, 6);
                    boolean neglectedAccess = isNeglectedCornerOrUnprotectedAccess(level, candidate, tracker);
                    double score = HyphalAdaptiveArchitectureEngine.evaluateBlindSpotSuitability(
                            density, blockLight, false, true, headroom, distNearest, neglectedAccess);
                    if (score <= 0.45) {
                        continue;
                    }
                    double priority = score + (neglectedAccess ? 0.25 : 0.0);
                    if (priority > bestPriority) {
                        bestPriority = priority;
                        bestPos = candidate;
                    }
                }
            }
        }

        if (bestPos == null) {
            return null;
        }
        return createBlindSpotSiteAt(level, hive, bestPos);
    }

    public static BlindSpotOutbreakSite createBlindSpotSiteAt(
            ServerLevel level,
            HiveMindSavedData hive,
            BlockPos corePos
    ) {
        ArchitecturalAccentSnapshot accent = hive.getArchitecturalAccent();
        List<BlueprintVoxel> blueprint = HyphalAdaptiveArchitectureEngine.generateBlindSpotBlueprint(
                corePos,
                accent.proportion(),
                accent.typicalRoomSpan(),
                accent.heightToWidthRatio(),
                accent.lightDensity()
        );
        if (level.getBlockEntity(corePos.below()) == null) {
            level.setBlockAndUpdate(corePos.below(), ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
        }
        level.setBlockAndUpdate(corePos, ModBlocks.SPORE_BULB.get().defaultBlockState());

        BlindSpotOutbreakSite site = new BlindSpotOutbreakSite(
                corePos.immutable(),
                accent.primaryBlockId(),
                accent.pillarBlockId(),
                accent.proportion(),
                accent.typicalRoomSpan(),
                accent.heightToWidthRatio(),
                accent.lightDensity(),
                blueprint.size(),
                level.getGameTime()
        );
        site.addPlacedBlock(corePos.below());

        int haloVeins = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (haloVeins >= 4) {
                break;
            }
            BlockPos tendrilPos = corePos.relative(dir);
            if (!level.isLoaded(tendrilPos) || hasForeignBlockEntity(level, tendrilPos)) {
                continue;
            }
            BlockState belowSt = level.getBlockState(tendrilPos.below());
            BlockState curSt = level.getBlockState(tendrilPos);
            if (belowSt.isFaceSturdy(level, tendrilPos.below(), Direction.UP) && canPlaceVeinOverlayAt(curSt)) {
                HyphalVeinEntity haloStrand = new HyphalVeinEntity(
                        level, corePos, tendrilPos, 4, true, 0.70F);
                haloStrand.setStrengthFloor(2);
                level.addFreshEntity(haloStrand);
                hive.registerHyphalVein(tendrilPos);
                site.addPlacedBlock(tendrilPos);
                haloVeins++;
            }
        }

        hive.registerBlindSpotSite(site);

        level.sendParticles(ModParticles.BIO_LUME.get(),
                corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5,
                10, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, corePos, ModSounds.SPORE_ARCHITECT_PULSE.get(), SoundSource.HOSTILE, 0.85F, 0.9F);
        for (ServerPlayer sp : level.players()) {
            if (sp.blockPosition().distSqr(corePos) <= 1600.0D) {
                sp.displayClientMessage(Component.translatable(
                        "message.mycelialmind.blind_spot_nucleated",
                        corePos.getX(), corePos.getY(), corePos.getZ()
                ).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        return site;
    }

    public static void tickBlindSpotOutbreaks(ServerLevel level, HiveMindSavedData hive) {
        for (BlindSpotOutbreakSite site : hive.getBlindSpotSites()) {
            if (!site.isCompleted()) {
                advanceBlindSpotGrowthStep(level, hive, site, false);
            }
        }
    }

    public static boolean advanceBlindSpotGrowthStep(
            ServerLevel level,
            HiveMindSavedData hive,
            BlindSpotOutbreakSite site,
            boolean forceIgnoreTimer
    ) {
        if (site == null || site.isCompleted()) {
            return false;
        }
        BlockPos corePos = site.getCorePos();
        if (!level.isLoaded(corePos)) {
            return false;
        }
        if (!level.getBlockState(corePos).is(ModBlocks.SPORE_BULB.get())) {
            witherBlindSpotSite(level, hive, site, false);
            return false;
        }
        if (PurificationZoneTracker.get(level).isProtected(corePos)) {
            witherBlindSpotSite(level, hive, site, true);
            return false;
        }

        int light = measureEffectiveBlockLight(level, corePos);
        if (light >= HyphalAdaptiveArchitectureEngine.PHOTOPHOBIC_STALL_LIGHT) {
            int strikes = site.incrementPhotophobicStrikes();
            hive.markBlindSpotDirty();
            level.sendParticles(ModParticles.PURGE.get(),
                    corePos.getX() + 0.5, corePos.getY() + 0.6, corePos.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.02);
            if (strikes >= HyphalAdaptiveArchitectureEngine.PHOTOPHOBIC_WITHER_STRIKES || light >= 12) {
                witherBlindSpotSite(level, hive, site, true);
            } else if (strikes == 1) {
                for (ServerPlayer sp : level.players()) {
                    if (sp.blockPosition().distSqr(corePos) <= 1600.0D) {
                        sp.displayClientMessage(Component.translatable("message.mycelialmind.blind_spot_photophobic_recoil")
                                .withStyle(ChatFormatting.YELLOW), true);
                    }
                }
            }
            return false;
        }

        site.resetPhotophobicStrikes();
        if (level.random.nextInt(3) == 0) {
            level.sendParticles(ModParticles.BIO_LUME.get(),
                    corePos.getX() + 0.5, corePos.getY() + 0.55, corePos.getZ() + 0.5,
                    3, 0.25, 0.25, 0.25, 0.01);
        }
        HiveProgressionTier tier = HiveProgressionTier.fromThreat(hive.getThreatLevel());
        long delay = HyphalAdaptiveArchitectureEngine.computeGrowthStepDelayTicks(
                tier, hive.isFungalTideActive(level.getGameTime()), light);
        if (delay < 0L) {
            return false;
        }
        if (!forceIgnoreTimer && (level.getGameTime() - site.getLastStepTick()) < delay) {
            return false;
        }

        List<BlueprintVoxel> blueprint = HyphalAdaptiveArchitectureEngine.generateBlindSpotBlueprint(
                corePos,
                site.getProportion(),
                site.getRoomSpan(),
                site.getHeightToWidthRatio(),
                site.getLightDensity()
        );
        ArchitecturalAccentSnapshot siteAccent = new ArchitecturalAccentSnapshot(
                site.getPrimaryBlockId(),
                site.getPillarBlockId(),
                site.getRoomSpan(),
                site.getHeightToWidthRatio(),
                site.getLightDensity(),
                site.getPrimaryBlockId().isBlank() ? 0 : 1
        );
        ResolvedPalette palette = ArchitecturalStructureAdapter.resolvePalette(siteAccent);

        boolean placedAny = false;
        while (site.getPlacedSteps() < blueprint.size()) {
            BlueprintVoxel voxel = blueprint.get(site.getPlacedSteps());
            site.incrementPlacedSteps();
            BlockPos targetPos = voxel.pos();
            if (!level.isInWorldBounds(targetPos)
                    || targetPos.equals(corePos)
                    || !level.isLoaded(targetPos)
                    || level.getBlockEntity(targetPos) != null) {
                continue;
            }
            BlockState existing = level.getBlockState(targetPos);
            if (existing.is(Blocks.BEDROCK) || existing.getDestroySpeed(level, targetPos) < 0.0F) {
                continue;
            }
            if (voxel.role() == BlueprintVoxelRole.HANGING_TENDRIL
                    && !level.getBlockState(targetPos.above()).isSolid()) {
                continue;
            }
            BlockState toPlace = voxel.role() == BlueprintVoxelRole.LUMINESCENT_VEIN
                    && targetPos.distManhattan(corePos) < 6
                    ? ModBlocks.ANTIBODY_CYST.get().defaultBlockState()
                    : resolveVoxelBlockState(voxel.role(), palette);
            if (level.dimension().equals(Level.OVERWORLD) && voxel.role() == BlueprintVoxelRole.FOUNDATION_MYCELIUM
                    && !existing.is(ModBlocks.CREEPING_MYCELIUM.get())
                    && !hasAdjacentMycelium(level, targetPos)) {
                toPlace = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
            }
            level.setBlockAndUpdate(targetPos, toPlace);
            site.addPlacedBlock(targetPos);
            site.setLastStepTick(level.getGameTime());
            hive.markBlindSpotDirty();
            level.sendParticles(ModParticles.SPORE_MOTE.get(),
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                    6, 0.25, 0.25, 0.25, 0.02);
            level.playSound(null, targetPos, ModSounds.BLOCK_MYCELIUM_PLACE.get(), SoundSource.BLOCKS, 0.55F, 1.1F);
            placedAny = true;
            break;
        }

        if (site.getPlacedSteps() >= blueprint.size() || site.getPlacedSteps() >= site.getTotalSteps()) {
            completeBlindSpotOutbreak(level, hive, site);
        }
        return placedAny;
    }

    public static void completeBlindSpotOutbreak(
            ServerLevel level,
            HiveMindSavedData hive,
            BlindSpotOutbreakSite site
    ) {
        site.setCompleted(true);
        BlockPos corePos = site.getCorePos();
        hive.removeBlindSpotSite(corePos);

        level.setBlockAndUpdate(corePos, ModBlocks.INCUBATOR_NODE.get().defaultBlockState());
        PurificationZoneTracker.get(level).addNode(corePos, SynapticNodeType.INCUBATOR);
        for (BlockPos placed : site.getPlacedBlocks()) {
            if (placed.getY() > corePos.getY() && level.isLoaded(placed)
                    && level.getBlockState(placed).is(ModBlocks.ANTIBODY_CYST.get())) {
                level.setBlockAndUpdate(placed, ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState());
            }
        }

        EntityType<? extends MycelialMob> eliteType = switch (site.getProportion()) {
            case BUNKER_VAULT -> ModEntities.CHITIN_BRUTE.get();
            case TOWER_SPIRE -> ModEntities.MYCELIAL_STALKER.get();
            default -> ModEntities.SYNAPTIC_MIMIC.get();
        };
        MycelialMob elite = eliteType.create(level);
        if (elite != null) {
            BlockPos spawnPos = findAmbushSpawnPos(level, PurificationZoneTracker.get(level), corePos);
            if (spawnPos == null) {
                spawnPos = corePos.above();
            }
            elite.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            elite.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
            elite.applyGlobalHiveState(level, hive);
            elite.promoteRank(EvolutionRank.ELITE);
            level.addFreshEntity(elite);
        }

        AreaEffectCloud cloud = new AreaEffectCloud(level, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5);
        cloud.setParticle(ModParticles.SPORE_CLOUD.get());
        cloud.setRadius(4.0F);
        cloud.setDuration(160);
        cloud.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 160, 0));
        cloud.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 120, 0));
        level.addFreshEntity(cloud);

        level.sendParticles(ModParticles.HIVE_PULSE.get(),
                corePos.getX() + 0.5, corePos.getY() + 0.8, corePos.getZ() + 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                corePos.getX() + 0.5, corePos.getY() + 0.8, corePos.getZ() + 0.5,
                18, 0.8, 0.6, 0.8, 0.04);
        level.playSound(null, corePos, ModSounds.SPORE_BULB_BURST.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
        level.playSound(null, corePos, ModSounds.MOB_PROMOTE_ELITE.get(), SoundSource.HOSTILE, 0.9F, 1.0F);

        for (ServerPlayer sp : level.players()) {
            if (sp.blockPosition().distSqr(corePos) <= 2304.0D) {
                sp.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.blind_spot_completed",
                        corePos.getX(), corePos.getY(), corePos.getZ()
                ).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
        }
    }

    public static void destroyBlindSpotVulnerableCore(
            ServerLevel level,
            HiveMindSavedData hive,
            BlindSpotOutbreakSite site,
            @Nullable Player player
    ) {
        BlockPos corePos = site.getCorePos();
        for (BlockPos placed : site.getPlacedBlocks()) {
            if (!level.isLoaded(placed) || hasForeignBlockEntity(level, placed)) {
                continue;
            }
            if (placed.getY() <= corePos.getY() - 1) {
                if (level.getBlockState(placed).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                    level.setBlockAndUpdate(placed, Blocks.COARSE_DIRT.defaultBlockState());
                }
            } else if (!placed.equals(corePos)) {
                level.removeBlock(placed, false);
            }
        }
        for (HyphalVeinEntity strand : level.getEntitiesOfClass(
                HyphalVeinEntity.class, new AABB(corePos).inflate(4.5D))) {
            hive.severHyphalVein(strand.getRootAnchor(), 0.0, level.getGameTime());
            strand.disintegrateWithSpores();
        }
        hive.removeBlindSpotSite(corePos);
        hive.severHyphalVein(corePos, 25.0, level.getGameTime());
        hive.reduceThreat(2.0);

        Block.popResource(level, corePos, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
        Block.popResource(level, corePos, new ItemStack(ModItems.LATENT_SPORE.get()));

        level.sendParticles(ModParticles.CHITIN_SHARD.get(),
                corePos.getX() + 0.5, corePos.getY() + 0.6, corePos.getZ() + 0.5,
                14, 0.5, 0.4, 0.5, 0.05);
        level.sendParticles(ModParticles.PURGE.get(),
                corePos.getX() + 0.5, corePos.getY() + 0.6, corePos.getZ() + 0.5,
                10, 0.4, 0.3, 0.4, 0.03);
        level.playSound(null, corePos, ModSounds.EPHEMERAL_CHITIN_CRUMBLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.blind_spot_core_destroyed")
                    .withStyle(ChatFormatting.GREEN), true);
        }
    }

    public static void witherBlindSpotSite(
            ServerLevel level,
            HiveMindSavedData hive,
            BlindSpotOutbreakSite site,
            boolean notifyPlayer
    ) {
        BlockPos corePos = site.getCorePos();
        if (level.isLoaded(corePos) && level.getBlockState(corePos).is(ModBlocks.SPORE_BULB.get())) {
            level.removeBlock(corePos, false);
        }
        for (BlockPos placed : site.getPlacedBlocks()) {
            if (!level.isLoaded(placed) || hasForeignBlockEntity(level, placed)) {
                continue;
            }
            if (placed.getY() <= corePos.getY() - 1) {
                if (level.getBlockState(placed).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                    level.setBlockAndUpdate(placed, Blocks.COARSE_DIRT.defaultBlockState());
                }
            } else {
                level.removeBlock(placed, false);
            }
        }
        for (HyphalVeinEntity strand : level.getEntitiesOfClass(
                HyphalVeinEntity.class, new AABB(corePos).inflate(4.5D))) {
            hive.severHyphalVein(strand.getRootAnchor(), 0.0, level.getGameTime());
            strand.disintegrateWithSpores();
        }
        hive.removeBlindSpotSite(corePos);

        if (level.isLoaded(corePos)) {
            level.sendParticles(ModParticles.PURGE.get(),
                    corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5,
                    12, 0.4, 0.3, 0.4, 0.03);
        }
        if (notifyPlayer) {
            for (ServerPlayer sp : level.players()) {
                if (sp.blockPosition().distSqr(corePos) <= 1600.0D) {
                    sp.displayClientMessage(Component.translatable("message.mycelialmind.blind_spot_withered_light")
                            .withStyle(ChatFormatting.GOLD), true);
                }
            }
        }
    }

    public static void checkActiveBlindSpotsForPhotophobia(ServerLevel level, BlockPos lightPos) {
        if (level.getChunkSource().getLightEngine() instanceof net.minecraft.server.level.ThreadedLevelLightEngine threaded) {
            threaded.tryScheduleUpdate();
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        for (BlindSpotOutbreakSite site : hive.getBlindSpotSites()) {
            if (!site.isCompleted() && site.getCorePos().distSqr(lightPos) <= 256.0D) {
                advanceBlindSpotGrowthStep(level, hive, site, false);
            }
        }
    }

    /**
     * Reads engine block light safely without calling ThreadedLevelLightEngine.runLightUpdates()
     * (which throws UnsupportedOperationException on server ticks), combining cached light with
     * immediate unobstructed emitters inside Manhattan radius 6 so light placement and removal
     * react deterministically in the same tick.
     */
    public static int measureEffectiveBlockLight(ServerLevel level, BlockPos corePos) {
        int engineLight = Math.max(
                level.getBrightness(LightLayer.BLOCK, corePos),
                level.getBrightness(LightLayer.BLOCK, corePos.above())
        );
        int directEmitterLight = 0;
        boolean anyBrightEmitterNearby = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int cx = corePos.getX();
        int cy = corePos.getY();
        int cz = corePos.getZ();

        for (int dx = -6; dx <= 6; dx++) {
            int remXZ = 6 - Math.abs(dx);
            for (int dz = -remXZ; dz <= remXZ; dz++) {
                int remY = remXZ - Math.abs(dz);
                int minDy = Math.max(-2, -remY);
                int maxDy = Math.min(4, remY);
                for (int dy = minDy; dy <= maxDy; dy++) {
                    int bx = cx + dx;
                    int by = cy + dy;
                    int bz = cz + dz;
                    cursor.set(bx, by, bz);
                    if (!level.isInWorldBounds(cursor) || !level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState cursorState = level.getBlockState(cursor);
                    if (isHiveSelfLuminescent(cursorState)) {
                        continue;
                    }
                    int emission = cursorState.getLightEmission(level, cursor);
                    if (emission < 8) {
                        continue;
                    }
                    if (emission >= HyphalAdaptiveArchitectureEngine.PHOTOPHOBIC_STALL_LIGHT) {
                        anyBrightEmitterNearby = true;
                    }
                    int dist = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    int attenuated = Math.max(0, emission - dist);
                    if (attenuated > directEmitterLight
                            && (!hasOpaqueBlocker(level, cursor, corePos)
                            || !hasOpaqueBlocker(level, cursor, corePos.above()))) {
                        directEmitterLight = attenuated;
                    }
                }
            }
        }

        if (engineLight >= HyphalAdaptiveArchitectureEngine.PHOTOPHOBIC_STALL_LIGHT && !anyBrightEmitterNearby) {
            return directEmitterLight;
        }
        return Math.max(engineLight, directEmitterLight);
    }

    private static boolean isHiveSelfLuminescent(BlockState state) {
        return state.getBlock() instanceof MycelialNodeBlock
                || state.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())
                || state.is(ModBlocks.BIO_CHITIN_LATTICE.get())
                || state.is(ModBlocks.SPORE_BULB.get())
                || state.is(ModBlocks.SPORE_VENT.get())
                || state.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                || state.is(ModBlocks.MYCELIAL_TENDRILS.get())
                || state.is(ModBlocks.MATRIARCH_ALTAR.get())
                || state.is(ModBlocks.MYCELIAL_RIFT.get())
                || state.is(ModBlocks.ANTIBODY_CYST.get());
    }

    private static boolean hasOpaqueBlocker(ServerLevel level, BlockPos from, BlockPos to) {
        int x = from.getX();
        int y = from.getY();
        int z = from.getZ();
        int tx = to.getX();
        int ty = to.getY();
        int tz = to.getZ();
        BlockPos.MutableBlockPos step = new BlockPos.MutableBlockPos();
        int guard = 0;
        while ((x != tx || y != ty || z != tz) && guard++ < 16) {
            int ax = Math.abs(tx - x);
            int ay = Math.abs(ty - y);
            int az = Math.abs(tz - z);
            if (ax >= ay && ax >= az && ax > 0) {
                x += Integer.compare(tx, x);
            } else if (az >= ay && az > 0) {
                z += Integer.compare(tz, z);
            } else if (ay > 0) {
                y += Integer.compare(ty, y);
            }
            if (x == tx && y == ty && z == tz) {
                break;
            }
            step.set(x, y, z);
            if (level.isLoaded(step) && level.getBlockState(step).canOcclude()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAdjacentMycelium(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.isLoaded(neighbor) && level.getBlockState(neighbor).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                return true;
            }
        }
        return false;
    }

    private static BlockState resolveVoxelBlockState(BlueprintVoxelRole role, ResolvedPalette palette) {
        return switch (role) {
            case FOUNDATION_MYCELIUM -> ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
            case PLAYER_PRIMARY_WALL -> palette.primaryWall();
            case PLAYER_PILLAR_RIB -> palette.structuralPillar();
            case FUNGAL_CAP -> ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
            case LUMINESCENT_VEIN -> ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
            case SPORE_VENT_MOUTH -> ModBlocks.SPORE_VENT.get().defaultBlockState();
            case HANGING_TENDRIL -> ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState();
        };
    }

    private static boolean isHiveOrNaturalNoiseBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof MycelialNodeBlock
                || state.is(ModBlocks.CREEPING_MYCELIUM.get())
                || state.is(ModBlocks.SPORE_VENT.get())
                || state.is(ModBlocks.SPORE_BULB.get())
                || state.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                || state.is(ModBlocks.MYCELIAL_TENDRILS.get())
                || state.is(ModBlocks.EPHEMERAL_CHITIN.get())
                || state.is(ModBlocks.ANTIBODY_CYST.get())
                || state.is(ModBlocks.HIVE_ALTAR.get())
                || state.is(ModBlocks.MATRIARCH_ALTAR.get())
                || state.is(ModBlocks.MYCELIAL_RIFT.get())
                || state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS);
    }

    private static boolean isSeverableVeinBlock(HiveMindSavedData hive, BlockPos pos, BlockState state) {
        if (hive.isBlindSpotCore(pos)) {
            return false;
        }
        return state.is(ModBlocks.CREEPING_MYCELIUM.get())
                || (state.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get()) && hive.isTrackedHyphalVein(pos))
                || state.is(ModBlocks.SPORE_BULB.get())
                || state.is(ModBlocks.MYCELIAL_TENDRILS.get())
                || state.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get());
    }

    public static boolean canPlaceVeinOverlayAt(BlockState aboveState) {
        if (aboveState.isAir()) {
            return true;
        }
        return aboveState.canBeReplaced()
                && aboveState.getFluidState().isEmpty()
                && aboveState.getLightEmission() == 0
                && !aboveState.is(BlockTags.CROPS)
                && !aboveState.is(BlockTags.SAPLINGS)
                && !(aboveState.getBlock() instanceof DoorBlock);
    }

    private static boolean isNeglectedCornerOrUnprotectedAccess(
            ServerLevel level,
            BlockPos pos,
            PurificationZoneTracker tracker
    ) {
        if (tracker.isProtected(pos)) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    BlockState st = level.getBlockState(p);
                    if (st.getBlock() instanceof DoorBlock
                            || st.getBlock() instanceof FenceGateBlock
                            || st.getBlock() instanceof TrapDoorBlock
                            || st.is(BlockTags.DOORS)
                            || st.is(BlockTags.FENCE_GATES)
                            || st.is(BlockTags.TRAPDOORS)) {
                        return true;
                    }
                }
            }
        }
        if (measureEffectiveBlockLight(level, pos) <= 5) {
            boolean wallN = hasStructuralWallNear(level, pos, Direction.NORTH);
            boolean wallS = hasStructuralWallNear(level, pos, Direction.SOUTH);
            boolean wallE = hasStructuralWallNear(level, pos, Direction.EAST);
            boolean wallW = hasStructuralWallNear(level, pos, Direction.WEST);
            return (wallN || wallS) && (wallE || wallW);
        }
        return false;
    }

    private static boolean hasStructuralWallNear(ServerLevel level, BlockPos pos, Direction dir) {
        for (int d = 1; d <= 2; d++) {
            BlockPos p = pos.relative(dir, d);
            if (!level.isLoaded(p)) {
                continue;
            }
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && st.blocksMotion() && !isHiveOrNaturalNoiseBlock(st)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSpecializedMycelialTool(ItemStack tool) {
        return !tool.isEmpty() && (tool.is(ModItems.SYMBIOTIC_BLADE.get())
                || tool.is(ModItems.ENZYME_LANCE.get())
                || tool.is(ModItems.BIO_EXTRACTOR.get()));
    }

    public static boolean isRightClickSeveringTool(ItemStack tool) {
        return !tool.isEmpty() && (tool.is(ModItems.SYMBIOTIC_BLADE.get())
                || tool.is(ModItems.ENZYME_LANCE.get())
                || tool.is(ModItems.BIO_EXTRACTOR.get())
                || tool.getItem() instanceof ShearsItem
                || tool.getItem() instanceof HoeItem
                || tool.is(ItemTags.HOES));
    }

    public static boolean isSeveringTool(ItemStack tool) {
        return isRightClickSeveringTool(tool)
                || (!tool.isEmpty() && (tool.getItem() instanceof SwordItem || tool.is(ItemTags.SWORDS)));
    }

    private static boolean isNearHyphalVein(ServerLevel level, HiveMindSavedData hive, BlockPos pos) {
        if (!level.getEntitiesOfClass(HyphalVeinEntity.class, new AABB(pos).inflate(1.8D)).isEmpty()) {
            return true;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (level.isLoaded(p) && (hive.isTrackedHyphalVein(p)
                            || level.getBlockState(p).is(ModBlocks.CREEPING_MYCELIUM.get()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean canSproutVeinOver(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()
                || state.liquid()
                || level.getBlockEntity(pos) != null
                || state.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                || state.is(ModBlocks.REINFORCED_BIO_BARRIER.get())
                || state.getBlock() instanceof EntityBlock
                || state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof TrapDoorBlock) {
            return false;
        }
        if (CreepingMyceliumBlock.canInfect(level, pos, state)) {
            return true;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.STONE_BRICKS)
                || state.is(BlockTags.PLANKS)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.DEEPSLATE_BRICKS)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND);
    }

    @Nullable
    private static BlockPos findPlayerSurfaceAnchor(ServerLevel level, Player player) {
        BlockPos feetPos = player.blockPosition();
        BlockPos ground = findGroundBlock(level, feetPos);
        if (ground != null) {
            return ground;
        }
        // Climbing walls / ladders or hanging beneath ceilings
        for (int dy : new int[]{0, 1}) {
            BlockPos torso = feetPos.offset(0, dy, 0);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos wall = torso.relative(dir);
                if (level.isInWorldBounds(wall) && level.isLoaded(wall)) {
                    BlockState st = level.getBlockState(wall);
                    if (!st.isAir() && !st.liquid() && st.isFaceSturdy(level, wall, dir.getOpposite())) {
                        return wall.immutable();
                    }
                }
            }
        }
        BlockPos ceiling = feetPos.above(2);
        if (level.isInWorldBounds(ceiling) && level.isLoaded(ceiling)) {
            BlockState st = level.getBlockState(ceiling);
            if (!st.isAir() && !st.liquid() && st.isFaceSturdy(level, ceiling, Direction.DOWN)) {
                return ceiling.immutable();
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos findGroundBlock(ServerLevel level, BlockPos feetPos) {
        for (BlockPos candidate : new BlockPos[]{feetPos.below(), feetPos, feetPos.below(2)}) {
            if (level.isInWorldBounds(candidate) && level.isLoaded(candidate)) {
                BlockState state = level.getBlockState(candidate);
                if (!state.isAir() && !state.liquid() && state.isFaceSturdy(level, candidate, Direction.UP)) {
                    return candidate.immutable();
                }
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos findNaturalVeinGround(ServerLevel level, BlockPos origin) {
        for (BlockPos candidate : new BlockPos[]{origin.above(), origin, origin.below(), origin.below(2)}) {
            if (level.isInWorldBounds(candidate) && level.isLoaded(candidate)) {
                BlockState state = level.getBlockState(candidate);
                BlockState above = level.getBlockState(candidate.above());
                if (!state.isAir()
                        && !state.liquid()
                        && state.isFaceSturdy(level, candidate, Direction.UP)
                        && canPlaceVeinOverlayAt(above)) {
                    return candidate.immutable();
                }
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos resolveCandidateBlindSpotPos(ServerLevel level, int x, int refY, int z) {
        for (int dy : new int[]{0, 1, -1, -2}) {
            BlockPos local = new BlockPos(x, refY + dy, z);
            if (level.isInWorldBounds(local)
                    && level.isLoaded(local)
                    && level.isEmptyBlock(local)
                    && level.getBlockState(local.below()).isFaceSturdy(level, local.below(), Direction.UP)) {
                return local;
            }
        }
        int surfY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos surface = new BlockPos(x, surfY, z);
        if (level.isInWorldBounds(surface) && level.isLoaded(surface)
                && level.isEmptyBlock(surface)
                && !level.getBlockState(surface.below()).liquid()
                && level.getBlockState(surface.below()).isFaceSturdy(level, surface.below(), Direction.UP)) {
            return surface;
        }
        return null;
    }

    private static boolean hasClearHeadroom(ServerLevel level, BlockPos pos, int height) {
        for (int dy = 1; dy <= height; dy++) {
            BlockPos up = pos.above(dy);
            if (!level.isInWorldBounds(up) || !level.isEmptyBlock(up)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static BlockPos findAmbushSpawnPos(ServerLevel level, PurificationZoneTracker tracker, BlockPos origin) {
        for (int radius = 2; radius <= 6; radius++) {
            for (int attempt = 0; attempt < 8; attempt++) {
                double ang = (Math.PI * 2.0 * attempt) / 8.0;
                int x = origin.getX() + (int) Math.round(Math.cos(ang) * radius);
                int z = origin.getZ() + (int) Math.round(Math.sin(ang) * radius);
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos p = new BlockPos(x, origin.getY() + dy, z);
                    if (!level.isInWorldBounds(p) || !level.isLoaded(p) || tracker.isProtected(p)) {
                        continue;
                    }
                    if (level.isEmptyBlock(p) && level.isEmptyBlock(p.above())
                            && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                        return p;
                    }
                }
            }
        }
        BlockPos fallback = origin.above();
        if (!tracker.isProtected(fallback) && level.isEmptyBlock(fallback) && level.isEmptyBlock(fallback.above())) {
            return fallback;
        }
        return null;
    }

    private static EntityType<? extends MycelialMob> pickAmbushPartnerType(HiveMindSavedData hive) {
        if (hive.hasHabitAdaptation(PlayerHabit.HIGH_MOBILITY)
                || HiveSquadComposer.findDominantStimulus(hive) == StimulusType.PROJECTILE) {
            return ModEntities.ACID_SPITTER.get();
        }
        if (hive.hasHabitAdaptation(PlayerHabit.BASTION_BUILDER)
                || HiveSquadComposer.findDominantStimulus(hive) == StimulusType.EXPLOSION) {
            return ModEntities.CHITIN_BRUTE.get();
        }
        return ModEntities.MYCELIAL_STALKER.get();
    }

    public static void triggerPressurizedSpray(ServerLevel level, BlockPos pos, float turgor) {
        level.sendParticles(ModParticles.SPORE_MOTE.get(),
                pos.getX() + 0.5D, pos.getY() + 0.35D, pos.getZ() + 0.5D,
                16, 0.45D, 0.35D, 0.45D, 0.08D);
        level.sendParticles(ModParticles.BIO_LUME.get(),
                pos.getX() + 0.5D, pos.getY() + 0.35D, pos.getZ() + 0.5D,
                10, 0.35D, 0.25D, 0.35D, 0.05D);
        level.playSound(null, pos, ModSounds.SPORE_ARCHITECT_PULSE.get(),
                SoundSource.BLOCKS, 0.70F, 1.30F);

        Vec3 center = Vec3.atCenterOf(pos);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(2.5D))) {
            if (!living.isAlive() || living.isSpectator()
                    || living instanceof MycelialMob || living instanceof MycelialChimeraEntity) {
                continue;
            }
            Vec3 away = living.position().subtract(center);
            double horizLen = Math.sqrt(away.x * away.x + away.z * away.z);
            double px = horizLen > 1.0E-4D ? (away.x / horizLen) * 0.38D : 0.0D;
            double pz = horizLen > 1.0E-4D ? (away.z / horizLen) * 0.38D : 0.0D;
            living.push(px, 0.16D, pz);
            living.hurtMarked = true;

            int chitinPieces = 0;
            for (ItemStack armor : living.getArmorSlots()) {
                if (!armor.isEmpty() && armor.getItem() instanceof ChitinArmorItem) {
                    chitinPieces++;
                }
            }
            if (chitinPieces < 2) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
                living.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 60, 0, false, true));
            }
        }
    }
}
