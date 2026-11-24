package com.alex.mycelialmind.colony;

import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import java.util.Comparator;
import java.util.List;

/** Resolves one spoken call on a single linked colony route. */
public final class ColonyDialectCallDirector {
    private record PendingPulse(BlockPos pos, int tone, long tick) { }
    private static final Map<ServerLevel, List<PendingPulse>> PENDING = new IdentityHashMap<>();

    private ColonyDialectCallDirector() { }

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<PendingPulse> pending = PENDING.get(level);
        if (pending == null) return;
        pending.removeIf(pulse -> {
            if (level.getGameTime() < pulse.tick()) return false;
            if (level.isLoaded(pulse.pos())) playPulse(level, pulse.pos(), pulse.tone(), 2);
            return true;
        });
        if (pending.isEmpty()) PENDING.remove(level);
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) PENDING.remove(level);
    }

    private static void playPulse(ServerLevel level, BlockPos pos, int tone, int index) {
        var particle = switch (tone) {
            case 0 -> ModParticles.SCAN.get();
            case 1 -> ModParticles.ENGRAM.get();
            default -> ModParticles.INSIGHT.get();
        };
        level.sendParticles(particle, pos.getX() + 0.5D, pos.getY() + 1.25D, pos.getZ() + 0.5D, 1, 0, 0, 0, 0);
        level.playSound(null, pos, ModSounds.COMMAND_STAFF_MARK.get(), SoundSource.BLOCKS, 0.7F, 0.75F + tone * 0.25F);
        for (ServerPlayer listener : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(16.0D))) {
            listener.displayClientMessage(Component.translatable("message.mycelialmind.colony.dialect_pulse", index, tone + 1), true);
        }
    }

    public static boolean emit(ServerLevel level, SovereignColonyData colony, BlockPos source,
                               BlockPos target, ColonySiteHabit meaning, Player player) {
        if (source.equals(target) || !colony.getLinkedOutposts().contains(source)
                || !colony.getLinkedOutposts().contains(target) || !level.isLoaded(source) || !level.isLoaded(target)) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.colony.dialect_bad_route"), true);
            return false;
        }
        ColonyDialect from = colony.getDialect(source);
        ColonyDialect to = colony.getDialect(target);
        int call = from.call(meaning);
        if (call < 0 || to.call(meaning) != call) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.colony.dialect_unknown"), true);
            return false;
        }
        long now = level.getGameTime();
        if (!from.tryEmit(now)) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.colony.dialect_cooldown"), true);
            return false;
        }
        List<ColonySymbiontEntity> workers = level.getEntitiesOfClass(ColonySymbiontEntity.class,
                new AABB(target).inflate(16.0D), worker -> worker.isAlive() && !worker.isStandby()
                        && worker.getColonyData(level) == colony);
        boolean accepted = switch (meaning) {
            case ESCORTED_COURIER -> requestEscort(level, colony, target, workers);
            case REST_BEFORE_RETURN -> requestRest(workers);
            case STAGGERED_PICKUP -> colony.getSiteMemory(target).givePickupTurnToOldest(now, 100L);
            default -> false;
        };
        SovereignColonySavedData.get(level).setDirty();
        playPulse(level, target, call / 3, 1);
        PENDING.computeIfAbsent(level, ignored -> new ArrayList<>())
                .add(new PendingPulse(target.immutable(), call % 3, now + 8L));
        player.displayClientMessage(Component.translatable(accepted
                ? "message.mycelialmind.colony.dialect_accepted"
                : "message.mycelialmind.colony.dialect_no_worker", Component.translatable(meaning.translationKey())), true);
        return accepted;
    }

    private static boolean requestEscort(ServerLevel level, SovereignColonyData colony, BlockPos target,
                                         List<ColonySymbiontEntity> workers) {
        boolean sentinel = workers.stream().anyMatch(w -> w.getCaste() == SymbiontCaste.SENTINEL && w.getTarget() == null);
        ColonySymbiontEntity courier = workers.stream().filter(w -> w.getCaste() == SymbiontCaste.COURIER
                && !w.isPouchEmpty()).findFirst().orElse(null);
        return sentinel && courier != null && colony.getSiteMemory(target).beginCourierEscort(courier.getUUID(), level.getGameTime());
    }

    private static boolean requestRest(List<ColonySymbiontEntity> workers) {
        ColonySymbiontEntity worker = workers.stream().filter(w -> !w.isResting() && w.getTarget() == null)
                .max(Comparator.comparingDouble(ColonySymbiontEntity::getFatigue)).orElse(null);
        if (worker == null) return false;
        worker.requestDialectRest();
        return true;
    }
}
