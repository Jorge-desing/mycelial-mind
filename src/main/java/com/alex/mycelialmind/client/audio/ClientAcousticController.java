package com.alex.mycelialmind.client.audio;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.AcousticEnvironment;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.MaterialAbsorption;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.SwarmVoiceBudget;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine.VoicePriority;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side acoustic coordinator combining:
 * 1) Per-listener 6-direction room geometry classification & Sinkhole proximity scan;
 * 2) Main-thread multi-block material occlusion raycasting;
 * 3) Sliding-window swarm anti-cacophony voice budget limiting;
 * 4) Thread-safe handoff of precomputed acoustic snapshots to {@link OpenAlEfxBridge}.
 */
public final class ClientAcousticController {
    private static final int ROOM_PROBE_INTERVAL_TICKS = 5;
    private static final int SINKHOLE_SCAN_INTERVAL_TICKS = 20;
    private static final int MAX_RAY_DIST = 14;
    private static final int MAX_OCCLUSION_SAMPLES = 24;
    private static final int MAX_PENDING_SNAPSHOTS = 256;

    private static final SwarmVoiceBudget VOICE_BUDGET = new SwarmVoiceBudget();
    private static final Map<SoundInstance, OpenAlEfxBridge.SourceAcousticSnapshot> PENDING_SNAPSHOTS =
            new ConcurrentHashMap<>();

    private static volatile AcousticEnvironment currentEnvironment = AcousticEnvironment.OPEN_AIR;
    private static volatile float lastEnclosureRatio = 0.0F;
    private static volatile float lastAverageWallDistance = MAX_RAY_DIST;
    private static volatile float lastComputedOcclusion = 0.0F;
    private static volatile boolean playerNearSinkhole = false;
    private static volatile BlockPos nearestUpdraftVentPos = null;

    private static int roomProbeTicks = 0;
    private static int sinkholeScanTicks = 0;

    private ClientAcousticController() {
    }

    public static void onSoundEngineLoad(SoundEngineLoadEvent event) {
        OpenAlEfxBridge.markNeedsReinit();
    }

    public static void resetSession() {
        VOICE_BUDGET.reset();
        PENDING_SNAPSHOTS.clear();
        currentEnvironment = AcousticEnvironment.OPEN_AIR;
        lastEnclosureRatio = 0.0F;
        lastAverageWallDistance = MAX_RAY_DIST;
        lastComputedOcclusion = 0.0F;
        playerNearSinkhole = false;
        nearestUpdraftVentPos = null;
        roomProbeTicks = 0;
        sinkholeScanTicks = 0;
    }

    public static void onClientTick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (++sinkholeScanTicks >= SINKHOLE_SCAN_INTERVAL_TICKS) {
            sinkholeScanTicks = 0;
            scanSinkholeProximity(mc.level, mc.player);
        }
        if (++roomProbeTicks >= ROOM_PROBE_INTERVAL_TICKS) {
            roomProbeTicks = 0;
            updateRoomEnvironment(mc.level, mc.player);
        }
        if (PENDING_SNAPSHOTS.size() > MAX_PENDING_SNAPSHOTS) {
            long cutoff = System.currentTimeMillis() - 2500L;
            PENDING_SNAPSHOTS.entrySet().removeIf(e -> e.getValue().createdAtMs() < cutoff);
        }
    }

    /**
     * Runs on the main client thread BEFORE SoundEngine dispatches the sound to its OpenAL executor thread.
     */
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            return;
        }

        ResourceLocation loc = sound.getLocation();
        if (loc == null) {
            return;
        }
        String namespace = loc.getNamespace();
        String path = loc.getPath();

        // 1) Swarm anti-cacophony budget (applies to mycelialmind:* world sounds; never drops UI/loops/bosses/alerts)
        if (MycelialClientConfig.swarmVoiceLimitingEnabled()
                && MycelialMind.MODID.equals(namespace)
                && sound.getSource() != SoundSource.MASTER
                && sound.getSource() != SoundSource.MUSIC) {
            VoicePriority prio = AcousticPhysicsEngine.classifySoundPriority(path, sound.isLooping(), sound.isRelative());
            double dx = sound.getX() - player.getX();
            double dy = sound.getY() - player.getEyeY();
            double dz = sound.getZ() - player.getZ();
            double distSq = sound.isRelative() ? 0.0D : (dx * dx + dy * dy + dz * dz);
            if (!VOICE_BUDGET.evaluate(path, prio, distSq, System.currentTimeMillis())) {
                event.setSound(null);
                return;
            }
        }

        // 2) Precompute OpenAL EFX snapshot safely on main client thread
        if (sound.isRelative() || sound.getSource() == SoundSource.MASTER || sound.getSource() == SoundSource.MUSIC) {
            return;
        }
        boolean inDeepMycelium = level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);
        boolean shouldApply = AcousticPhysicsEngine.shouldApplyEfx(
                namespace,
                inDeepMycelium,
                playerNearSinkhole,
                MycelialClientConfig.openAlEfxAcousticsEnabled(),
                MycelialClientConfig.openAlEfxVanillaInMycelialZones()
        );
        if (!shouldApply) {
            return;
        }

        float occlusion = 0.0F;
        if (MycelialClientConfig.caveSoundOcclusion()) {
            occlusion = computeMaterialOcclusion(level, player.getEyePosition(), new Vec3(sound.getX(), sound.getY(), sound.getZ()));
            lastComputedOcclusion = occlusion;
        }

        AcousticEnvironment env = currentEnvironment;
        boolean applyReverb = !sound.isLooping() || MycelialMind.MODID.equals(namespace);
        PENDING_SNAPSHOTS.put(
                sound,
                new OpenAlEfxBridge.SourceAcousticSnapshot(
                        env,
                        occlusion,
                        player.isUnderWater(),
                        applyReverb,
                        System.currentTimeMillis()
                )
        );
    }

    /**
     * Runs on the OpenAL SoundEngine executor thread right after static source playback begins.
     */
    public static void onPlaySoundSource(PlaySoundSourceEvent event) {
        OpenAlEfxBridge.SourceAcousticSnapshot snap = PENDING_SNAPSHOTS.remove(event.getSound());
        if (snap != null) {
            OpenAlEfxBridge.applyToChannel(event.getChannel(), snap);
        }
    }

    /**
     * Runs on the OpenAL SoundEngine executor thread right after streaming source playback begins.
     */
    public static void onPlayStreamingSource(PlayStreamingSourceEvent event) {
        OpenAlEfxBridge.SourceAcousticSnapshot snap = PENDING_SNAPSHOTS.remove(event.getSound());
        if (snap != null) {
            OpenAlEfxBridge.applyToChannel(event.getChannel(), snap);
        }
    }

    private static void scanSinkholeProximity(Level level, Player player) {
        BlockPos origin = player.blockPosition();
        BlockPos bestVent = null;
        double bestVentSq = Double.MAX_VALUE;
        int sinkholeBlockCount = 0;

        var updraftVent = ModBlocks.SPORE_UPDRAFT_VENT.get();
        var rootSapMoss = ModBlocks.ROOT_SAP_MOSS.get();
        var rootCapShelf = ModBlocks.ROOT_CAP_SHELF.get();
        var amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get();

        for (int dx = -16; dx <= 16; dx += 2) {
            for (int dy = -8; dy <= 8; dy += 2) {
                for (int dz = -16; dz <= 16; dz += 2) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.is(updraftVent)) {
                        sinkholeBlockCount += 3;
                        double dSq = pos.distSqr(origin);
                        if (dSq < bestVentSq) {
                            bestVentSq = dSq;
                            bestVent = pos.immutable();
                        }
                    } else if (state.is(rootSapMoss) || state.is(rootCapShelf) || state.is(amberVein)) {
                        sinkholeBlockCount++;
                    }
                }
            }
        }

        // Fine scan around the discovered updraft vent or immediate player vicinity for exact block position
        if (bestVent == null) {
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-6, -4, -6), origin.offset(6, 4, 6))) {
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(updraftVent)) {
                    double dSq = pos.distSqr(origin);
                    if (dSq < bestVentSq) {
                        bestVentSq = dSq;
                        bestVent = pos.immutable();
                        sinkholeBlockCount += 3;
                    }
                }
            }
        }

        nearestUpdraftVentPos = bestVent;
        playerNearSinkhole = (bestVent != null) || (sinkholeBlockCount >= 4);
    }

    private static void updateRoomEnvironment(Level level, Player player) {
        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        boolean inDeepMycelium = level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);
        boolean hasSky = level.canSeeSky(eyePos);

        int hits = 0;
        float distSum = 0.0F;
        boolean sawSanctumBlock = false;
        var altarBlock = ModBlocks.HIVE_ALTAR.get();
        var matriarchAltar = ModBlocks.MATRIARCH_ALTAR.get();
        var bioBarrier = ModBlocks.REINFORCED_BIO_BARRIER.get();

        for (Direction dir : Direction.values()) {
            int hitDist = MAX_RAY_DIST;
            for (int step = 1; step <= MAX_RAY_DIST; step++) {
                BlockPos probe = eyePos.relative(dir, step);
                if (!level.hasChunkAt(probe)) {
                    break;
                }
                BlockState state = level.getBlockState(probe);
                if (state.is(altarBlock) || state.is(matriarchAltar) || state.is(bioBarrier)) {
                    sawSanctumBlock = true;
                }
                if (classifyBlockAbsorption(level, probe, state) != MaterialAbsorption.AIR) {
                    hits++;
                    hitDist = step;
                    break;
                }
            }
            distSum += hitDist;
        }

        float enclosure = hits / 6.0F;
        float avgWallDist = distSum / 6.0F;
        lastEnclosureRatio = enclosure;
        lastAverageWallDistance = avgWallDist;
        currentEnvironment = AcousticPhysicsEngine.classifyEnvironment(
                inDeepMycelium,
                playerNearSinkhole,
                sawSanctumBlock,
                hasSky,
                enclosure,
                avgWallDist
        );
    }

    private static float computeMaterialOcclusion(Level level, Vec3 listenerEye, Vec3 sourcePos) {
        double dist = listenerEye.distanceTo(sourcePos);
        if (dist <= 1.25D || dist > 64.0D) {
            return 0.0F;
        }
        int samples = Mth.clamp((int) Math.ceil(dist * 1.15D), 2, MAX_OCCLUSION_SAMPLES);
        double stepLen = dist / (double) samples;
        float cumulativeAbsorption = 0.0F;
        long lastBlockKey = Long.MIN_VALUE;

        for (int i = 1; i < samples; i++) {
            double frac = i / (double) samples;
            double x = Mth.lerp(frac, listenerEye.x, sourcePos.x);
            double y = Mth.lerp(frac, listenerEye.y, sourcePos.y);
            double z = Mth.lerp(frac, listenerEye.z, sourcePos.z);
            BlockPos bp = BlockPos.containing(x, y, z);
            long key = bp.asLong();
            if (key == lastBlockKey) {
                continue;
            }
            lastBlockKey = key;
            if (!level.hasChunkAt(bp)) {
                continue;
            }
            MaterialAbsorption mat = classifyBlockAbsorption(level, bp, level.getBlockState(bp));
            if (mat != MaterialAbsorption.AIR) {
                cumulativeAbsorption += mat.absorptionPerBlock() * (float) Math.max(0.45D, Math.min(1.5D, stepLen));
                if (cumulativeAbsorption >= 3.2F) {
                    break;
                }
            }
        }
        return AcousticPhysicsEngine.computeOcclusionFactor(cumulativeAbsorption);
    }

    private static MaterialAbsorption classifyBlockAbsorption(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.canBeReplaced()) {
            return MaterialAbsorption.AIR;
        }
        if (state.is(ModBlocks.REINFORCED_BIO_BARRIER.get())
                || state.is(ModBlocks.HIVE_ALTAR.get())
                || state.is(ModBlocks.MATRIARCH_ALTAR.get())
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.BEDROCK)) {
            return MaterialAbsorption.DENSE_BARRIER_OBSIDIAN;
        }
        if (state.is(BlockTags.LEAVES)
                || state.is(Blocks.GLASS)
                || state.is(ModBlocks.SPORE_BULB.get())
                || state.is(ModBlocks.MYCELIAL_TENDRILS.get())
                || state.is(ModBlocks.BIO_CHITIN_LATTICE.get())
                || !state.isCollisionShapeFullBlock(level, pos)) {
            return state.blocksMotion() ? MaterialAbsorption.FOLIAGE_GLASS_WATER : MaterialAbsorption.AIR;
        }
        if (state.is(ModBlocks.CREEPING_MYCELIUM.get())
                || state.is(ModBlocks.ROOT_SAP_MOSS.get())
                || state.is(ModBlocks.ROOT_CAP_SHELF.get())
                || state.is(ModBlocks.ANTIBODY_CYST.get())
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.WOOL)) {
            return MaterialAbsorption.MYCELIUM_SOIL_MOSS;
        }
        if (state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS)
                || state.is(ModBlocks.MYCELIAL_NODE.get())
                || state.is(ModBlocks.INCUBATOR_NODE.get())
                || state.is(ModBlocks.SENTINEL_NODE.get())
                || state.is(ModBlocks.SYMBIOTIC_NODE.get())
                || state.is(ModBlocks.CHITIN_PILLAR.get())
                || state.is(ModBlocks.EPHEMERAL_CHITIN.get())) {
            return MaterialAbsorption.CHITIN_WOOD_BONE;
        }
        return MaterialAbsorption.STONE_DEEPSLATE_METAL;
    }

    public static AcousticEnvironment getCurrentEnvironment() {
        return currentEnvironment;
    }

    public static float getLastEnclosureRatio() {
        return lastEnclosureRatio;
    }

    public static float getLastAverageWallDistance() {
        return lastAverageWallDistance;
    }

    public static float getLastComputedOcclusion() {
        return lastComputedOcclusion;
    }

    public static boolean isPlayerNearSinkhole() {
        return playerNearSinkhole;
    }

    public static BlockPos getNearestUpdraftVentPos() {
        return nearestUpdraftVentPos;
    }

    public static long getVoiceAcceptedCount() {
        return VOICE_BUDGET.getAcceptedCount();
    }

    public static long getVoiceSuppressedCount() {
        return VOICE_BUDGET.getSuppressedCount();
    }

    public static int getActiveVoiceWindowCount() {
        return VOICE_BUDGET.getActiveWindowCount(System.currentTimeMillis());
    }
}
