package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ApexMatriarchEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles Mycorrhizal Bio-Acoustics (seismic vibration pulses on Creeping Mycelium),
 * stealth counterplay (sneak, Chitin Boots, Symbiotic Nodes, Pylons),
 * Neural Echo Decoys, and Mycorrhizal Fast Travel cooldowns.
 */
public final class MycorrhizalAcousticTracker {
    private static final long PULSE_COOLDOWN_TICKS = 40L;
    private static final long PULSE_RETRY_TICKS = 5L;
    private static final int ACOUSTIC_NODE_RANGE = 32;
    private static final int PATROL_ALERT_RANGE = 18;
    private static final int DECOY_SILENCE_RANGE = 16;
    private static final int DECOY_LURE_RANGE = 20;

    private static final Map<UUID, Long> LAST_PULSE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PULSE_ATTEMPT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TRAVEL_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<DecoyEntry>> ACTIVE_DECOYS = new ConcurrentHashMap<>();
    private static long lastPlayerCachePruneTick = Long.MIN_VALUE;

    public record DecoyEntry(BlockPos pos, long expiresAtTick) {
    }

    private MycorrhizalAcousticTracker() {
    }

    public static long getTravelCooldownRemaining(UUID playerId, long nowTick) {
        Long until = TRAVEL_COOLDOWN_UNTIL.get(playerId);
        if (until == null) {
            return 0L;
        }
        if (until <= nowTick) {
            TRAVEL_COOLDOWN_UNTIL.remove(playerId, until);
            return 0L;
        }
        return until - nowTick;
    }

    public static void setTravelCooldown(UUID playerId, long untilTick) {
        TRAVEL_COOLDOWN_UNTIL.put(playerId, untilTick);
    }

    public static void registerDecoy(ServerLevel level, BlockPos pos, int durationTicks) {
        long expiresAt = level.getGameTime() + Math.max(20, durationTicks);
        ACTIVE_DECOYS.computeIfAbsent(level, k -> new CopyOnWriteArrayList<>())
                .add(new DecoyEntry(pos.immutable(), expiresAt));
        lureNearbyMobsToDecoy(level, pos);
    }

    public static void clearDecoys(ServerLevel level) {
        ACTIVE_DECOYS.remove(level);
    }

    public static boolean hasActiveDecoyNear(ServerLevel level, BlockPos origin, int radius) {
        return getNearestActiveDecoy(level, origin, radius) != null;
    }

    @Nullable
    public static BlockPos getNearestActiveDecoy(ServerLevel level, BlockPos origin, int radius) {
        List<DecoyEntry> entries = ACTIVE_DECOYS.get(level);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        long now = level.getGameTime();
        double bestDistSq = (double) radius * radius;
        BlockPos best = null;
        for (DecoyEntry entry : entries) {
            if (entry.expiresAtTick() > now) {
                double distSq = entry.pos().distSqr(origin);
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    best = entry.pos();
                }
            }
        }
        return best;
    }

    public static boolean consumeNearestDecoy(ServerLevel level, BlockPos origin, int radius) {
        List<DecoyEntry> entries = ACTIVE_DECOYS.get(level);
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        double bestDistSq = (double) radius * radius;
        DecoyEntry best = null;
        for (DecoyEntry entry : entries) {
            if (entry.expiresAtTick() > now) {
                double distSq = entry.pos().distSqr(origin);
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    best = entry;
                }
            }
        }
        if (best != null && entries.remove(best)) {
            return true;
        }
        return false;
    }

    public static boolean isPlayerSilenced(ServerLevel level, Player player) {
        if (player.isCreative() || player.isSpectator() || player.isCrouching() || player.isShiftKeyDown()) {
            return true;
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CHITIN_BOOTS.get())) {
            return true;
        }
        BlockPos pos = player.blockPosition();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        if (tracker.isProtected(pos) || tracker.hasNearbySymbioticNode(pos, 16)) {
            return true;
        }
        return hasActiveDecoyNear(level, pos, DECOY_SILENCE_RANGE);
    }

    public static boolean isChargedHyphalVein(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.HYPHAL_VEIN.get())
                && (state.getValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED)
                || state.getValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH) >= 2);
    }

    public static boolean tryEmitSeismicPulse(ServerLevel level, BlockPos origin, @Nullable Player sourcePlayer) {
        if (!MycelialConfig.MYCORRHIZAL_ACOUSTICS_ENABLED.get()) {
            return false;
        }
        if (sourcePlayer != null && isPlayerSilenced(level, sourcePlayer)) {
            return false;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos nearestNode = tracker.findNearestEnemyNode(origin, ACOUSTIC_NODE_RANGE);
        if (nearestNode == null) {
            return false;
        }

        spawnSeismicWave(level, origin, nearestNode);
        if (level.getBlockEntity(nearestNode) instanceof SynapticNodeBlockEntity be) {
            be.addBiomassCharge(6);
            be.triggerAlert(120);
        }

        if (!hasActiveDecoyNear(level, origin, DECOY_LURE_RANGE)) {
            HiveBlackboard.reportTarget(level, origin);
            if (sourcePlayer != null) {
                AABB alertBox = new AABB(origin).inflate(PATROL_ALERT_RANGE);
                for (MycelialMob mob : level.getEntitiesOfClass(MycelialMob.class, alertBox)) {
                    if (mob.isAlive()
                            && mob.getTarget() == null
                            && !MycelialMob.isFriendlyToPlayer(mob, sourcePlayer)) {
                        mob.setTarget(sourcePlayer);
                    }
                }
            }
        }
        return true;
    }

    private static void spawnSeismicWave(ServerLevel level, BlockPos from, BlockPos to) {
        int steps = 10;
        double ax = from.getX() + 0.5;
        double ay = from.getY() + 0.25;
        double az = from.getZ() + 0.5;
        double dx = (to.getX() + 0.5) - ax;
        double dy = (to.getY() + 0.6) - ay;
        double dz = (to.getZ() + 0.5) - az;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            level.sendParticles(
                    i % 2 == 0 ? ModParticles.PSYCHIC_WAVE.get() : ModParticles.SPORE_MOTE.get(),
                    ax + dx * t, ay + dy * t, az + dz * t,
                    1, 0.05, 0.05, 0.05, 0.01
            );
        }
        level.playSound(null, from, ModSounds.MYCORRHIZAL_TREMOR.get(), SoundSource.BLOCKS, 0.45F, 1.25F);
    }

    private static void lureNearbyMobsToDecoy(ServerLevel level, BlockPos decoyPos) {
        HiveBlackboard.reportTarget(level, decoyPos);
        AABB box = new AABB(decoyPos).inflate(DECOY_LURE_RANGE);
        for (MycelialMob mob : level.getEntitiesOfClass(MycelialMob.class, box)) {
            if (!mob.isAlive() || mob.isAlliedEscort()
                    || mob instanceof HiveCoreBossEntity || mob instanceof ApexMatriarchEntity) {
                continue;
            }
            if (mob.getTarget() instanceof Player) {
                mob.setTarget(null);
            }
            mob.getNavigation().moveTo(decoyPos.getX() + 0.5, decoyPos.getY(), decoyPos.getZ() + 0.5, 1.15D);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (player.isSpectator() || player.isCreative()) {
            return;
        }
        boolean noisyMotion = player.isSprinting() || (player.onGround() && player.fallDistance > 1.5F);
        if (!noisyMotion) {
            return;
        }

        BlockPos pos = player.blockPosition();
        var atFeet = serverLevel.getBlockState(pos);
        var belowFeet = serverLevel.getBlockState(pos.below());
        boolean onMycelium = belowFeet.is(ModBlocks.CREEPING_MYCELIUM.get())
                || atFeet.is(ModBlocks.CREEPING_MYCELIUM.get());
        boolean onChargedVein = isChargedHyphalVein(atFeet) || isChargedHyphalVein(belowFeet);
        if (!onMycelium && !onChargedVein) {
            return;
        }

        long now = globalGameTime(serverLevel);
        Long last = LAST_PULSE_TICK.get(player.getUUID());
        if (last != null && (now - last) < PULSE_COOLDOWN_TICKS) {
            return;
        }
        Long lastAttempt = LAST_PULSE_ATTEMPT_TICK.get(player.getUUID());
        if (lastAttempt != null && (now - lastAttempt) < PULSE_RETRY_TICKS) {
            return;
        }
        LAST_PULSE_ATTEMPT_TICK.put(player.getUUID(), now);
        if (tryEmitSeismicPulse(serverLevel, pos, player)) {
            LAST_PULSE_TICK.put(player.getUUID(), now);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (event.getState().is(ModBlocks.CREEPING_MYCELIUM.get())) {
            tryEmitSeismicPulse(serverLevel, event.getPos(), event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getGameTime() % 20L != 0L) {
            return;
        }
        prunePlayerCaches(serverLevel);
        List<DecoyEntry> entries = ACTIVE_DECOYS.get(serverLevel);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        long now = serverLevel.getGameTime();
        Iterator<DecoyEntry> it = entries.iterator();
        while (it.hasNext()) {
            DecoyEntry entry = it.next();
            if (entry.expiresAtTick() <= now) {
                entries.remove(entry);
                continue;
            }
            BlockPos p = entry.pos();
            serverLevel.sendParticles(ModParticles.ENGRAM.get(), p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5,
                    3, 0.35, 0.25, 0.35, 0.02);
            serverLevel.sendParticles(ModParticles.ENGRAM.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    4, 0.5, 0.2, 0.5, 0.02);
            serverLevel.playSound(null, p, ModSounds.MYCORRHIZAL_TREMOR.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
            lureNearbyMobsToDecoy(serverLevel, p);
        }
    }

    private static void prunePlayerCaches(ServerLevel level) {
        long now = globalGameTime(level);
        if (now % 200L != 0L || lastPlayerCachePruneTick == now) {
            return;
        }
        LAST_PULSE_TICK.entrySet().removeIf(entry -> now - entry.getValue() > 1200L);
        LAST_PULSE_ATTEMPT_TICK.entrySet().removeIf(entry -> now - entry.getValue() > 1200L);
        TRAVEL_COOLDOWN_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
        lastPlayerCachePruneTick = now;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ACTIVE_DECOYS.remove(level);
            LAST_PULSE_TICK.clear();
            LAST_PULSE_ATTEMPT_TICK.clear();
            TRAVEL_COOLDOWN_UNTIL.clear();
        }
    }

    private static long globalGameTime(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }
}
