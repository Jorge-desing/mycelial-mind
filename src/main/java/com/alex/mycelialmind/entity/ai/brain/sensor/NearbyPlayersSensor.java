package com.alex.mycelialmind.entity.ai.brain.sensor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Scans nearby players and populates {@code NEAREST_PLAYERS}, {@code NEAREST_VISIBLE_PLAYER},
 * and {@code NEAREST_VISIBLE_ATTACKABLE_PLAYER}.
 */
public class NearbyPlayersSensor<E extends LivingEntity> extends PredicateSensor<Player, E> {
    private static final List<MemoryModuleType<?>> MEMORIES = List.of(
            MemoryModuleType.NEAREST_PLAYERS,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER
    );

    protected double radiusXZ = 16.0D;
    protected double radiusY = 16.0D;

    public NearbyPlayersSensor() {
        super((player, entity) -> !player.isSpectator());
    }

    public NearbyPlayersSensor<E> setRadius(double radius) {
        return setRadius(radius, radius);
    }

    public NearbyPlayersSensor<E> setRadius(double radiusXZ, double radiusY) {
        this.radiusXZ = Math.max(1.0D, radiusXZ);
        this.radiusY = Math.max(1.0D, radiusY);
        return this;
    }

    @Override
    public NearbyPlayersSensor<E> setPredicate(BiPredicate<Player, E> predicate) {
        super.setPredicate(predicate);
        return this;
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return MEMORIES;
    }

    @Override
    public SensorType<? extends Sensor<?>> type() {
        return SensorType.NEAREST_PLAYERS;
    }

    @Override
    protected void scan(ServerLevel level, E entity) {
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (Math.abs(player.getX() - entity.getX()) <= this.radiusXZ
                    && Math.abs(player.getY() - entity.getY()) <= this.radiusY
                    && Math.abs(player.getZ() - entity.getZ()) <= this.radiusXZ
                    && entity.distanceToSqr(player) <= this.radiusXZ * this.radiusXZ + this.radiusY * this.radiusY
                    && predicate().test(player, entity)) {
                nearbyPlayers.add(player);
            }
        }
        nearbyPlayers.sort(Comparator.comparingDouble(entity::distanceToSqr));

        Brain<?> brain = entity.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_PLAYERS, nearbyPlayers);

        Player nearestVisible = null;
        Player nearestAttackable = null;
        for (Player player : nearbyPlayers) {
            if (entity.hasLineOfSight(player)) {
                if (nearestVisible == null) {
                    nearestVisible = player;
                }
                if (nearestAttackable == null && !player.isCreative() && entity.canAttack(player)) {
                    nearestAttackable = player;
                }
            }
            if (nearestVisible != null && nearestAttackable != null) {
                break;
            }
        }
        if (nearestVisible != null) {
            brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, nearestVisible);
        } else {
            brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        }
        if (nearestAttackable != null) {
            brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, nearestAttackable);
        } else {
            brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
        }
    }
}
