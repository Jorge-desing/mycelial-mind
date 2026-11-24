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

import java.util.Comparator;
import java.util.List;

/** Resolves one spoken call on a single linked colony route. */
public final class ColonyDialectCallDirector {
    private ColonyDialectCallDirector() { }

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
        for (int pulse = 0; pulse < 2; pulse++) {
            int tone = pulse == 0 ? call / 3 : call % 3;
            level.sendParticles(ModParticles.PSYCHIC_WAVE.get(), target.getX() + 0.5D,
                    target.getY() + 1.0D + pulse * 0.45D, target.getZ() + 0.5D,
                    1, 0, 0, 0, 0);
            level.playSound(null, target, ModSounds.COMMAND_STAFF_MARK.get(), SoundSource.BLOCKS,
                    0.7F, 0.75F + tone * 0.25F);
        }
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
