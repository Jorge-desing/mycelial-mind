package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.AcidSpitterEntity;
import com.alex.mycelialmind.entity.BurrowerEntity;
import com.alex.mycelialmind.entity.ChitinBruteEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.SporeArchitectEntity;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Observes non-lethal player tactical habits (COMBAT_MEDIC, HIGH_MOBILITY,
 * BASTION_BUILDER, SHADOW_INFILTRATOR) and applies Vanilla+ counter-adaptations.
 */
public final class HabitTelemetryTracker {
    private static final long HABIT_THROTTLE_TICKS = 20L;
    private static final long THROTTLE_CACHE_MAX_AGE = 1200L;
    private static final Map<UUID, Long> LAST_MOVEMENT_CHECK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ADHESIVE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MEDIC_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BASTION_HIT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SIEGE_ABSORPTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SHADOW_HIT = new ConcurrentHashMap<>();
    private static long lastCachePruneTick = Long.MIN_VALUE;

    private HabitTelemetryTracker() {
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.isCanceled() || !(event.getAmount() > 0.0F)
                || !(event.getEntity() instanceof Player player)
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // SPORE_ROT counter-adaptation reduces healing by 30%
        if (player.hasEffect(ModEffects.SPORE_ROT)) {
            event.setAmount(event.getAmount() * 0.70F);
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean mobsNearby = !serverLevel.getEntitiesOfClass(
                MycelialMob.class,
                new AABB(player.blockPosition()).inflate(16.0D),
                mob -> mob.isAlive() && !MycelialMob.isFriendlyToPlayer(mob, player)
        ).isEmpty();
        if (mobsNearby && event.getAmount() >= 1.5F) {
            long now = globalGameTime(serverLevel);
            Long lastMedic = LAST_MEDIC_TICK.get(player.getUUID());
            if (lastMedic == null || (now - lastMedic) >= 60L) {
                LAST_MEDIC_TICK.put(player.getUUID(), now);
                SynapticMemoryRouter.routeHabit(serverLevel, player.blockPosition(), PlayerHabit.COMBAT_MEDIC, 3.0);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel) || player.isCreative() || player.isSpectator()) {
            return;
        }
        long now = globalGameTime(serverLevel);
        pruneThrottleCaches(now);
        Long last = LAST_MOVEMENT_CHECK.get(player.getUUID());
        if (last != null && (now - last) < HABIT_THROTTLE_TICKS) {
            return;
        }
        LAST_MOVEMENT_CHECK.put(player.getUUID(), now);

        boolean highMobility = player.isSprinting() || (player.getVehicle() instanceof MycelialChimeraEntity);
        if (highMobility) {
            boolean mobsNearby = !serverLevel.getEntitiesOfClass(
                    MycelialMob.class,
                    new AABB(player.blockPosition()).inflate(18.0D),
                    mob -> mob.isAlive() && !MycelialMob.isFriendlyToPlayer(mob, player)
            ).isEmpty();
            if (mobsNearby) {
                SynapticMemoryRouter.routeHabit(serverLevel, player.blockPosition(), PlayerHabit.HIGH_MOBILITY, 2.0);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.isCreative()) {
            return;
        }
        Block placed = event.getPlacedBlock().getBlock();
        if (placed == ModBlocks.PURIFICATION_PYLON.get() || placed == ModBlocks.PURIFICATION_TURRET.get()) {
            SynapticMemoryRouter.routeHabit(serverLevel, event.getPos(), PlayerHabit.BASTION_BUILDER, 6.0);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || !(event.getAmount() > 0.0F) || event.getEntity().level().isClientSide()) {
            return;
        }
        // 1) Mob hitting player -> apply COMBAT_MEDIC / HIGH_MOBILITY counter-adaptations
        if (event.getEntity() instanceof Player player
                && player.level() instanceof ServerLevel serverLevel
                && event.getSource().getEntity() instanceof MycelialMob attacker) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            if (hive.hasHabitAdaptation(PlayerHabit.COMBAT_MEDIC) && !player.isBlocking()) {
                player.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 80, 0));
            }
            if (hive.hasHabitAdaptation(PlayerHabit.HIGH_MOBILITY)
                    && (attacker instanceof AcidSpitterEntity || attacker instanceof SporeArchitectEntity)) {
                long now = globalGameTime(serverLevel);
                Long lastStick = LAST_ADHESIVE_TICK.get(attacker.getUUID());
                if (lastStick == null || (now - lastStick) >= 160L) {
                    LAST_ADHESIVE_TICK.put(attacker.getUUID(), now);
                    player.setSprinting(false);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                    serverLevel.sendParticles(ModParticles.INSIGHT.get(),
                            player.getX(), player.getY() + 0.2, player.getZ(),
                            10, 0.4, 0.1, 0.4, 0.02);
                }
            }
            return;
        }

        // 2) Player/Bastion hitting MycelialMob -> track BASTION_BUILDER / SHADOW_INFILTRATOR
        if (event.getEntity() instanceof MycelialMob mob && mob.level() instanceof ServerLevel serverLevel) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            long now = globalGameTime(serverLevel);
            if (event.getSource().getEntity() instanceof PurifierDroneEntity
                    || (event.getSource().getDirectEntity() != null && event.getSource().getEntity() == null)) {
                Long lastBastion = LAST_BASTION_HIT.get(mob.getUUID());
                if (lastBastion == null || (now - lastBastion) >= 40L) {
                    LAST_BASTION_HIT.put(mob.getUUID(), now);
                    SynapticMemoryRouter.routeHabit(serverLevel, mob.blockPosition(), PlayerHabit.BASTION_BUILDER, 1.5);
                }
                if (hive.hasHabitAdaptation(PlayerHabit.BASTION_BUILDER)
                        && (mob instanceof ChitinBruteEntity || mob instanceof BurrowerEntity)) {
                    Long lastAbsorb = LAST_SIEGE_ABSORPTION.get(mob.getUUID());
                    if (lastAbsorb == null || (now - lastAbsorb) >= 160L) {
                        LAST_SIEGE_ABSORPTION.put(mob.getUUID(), now);
                        mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, false, false));
                    }
                }
            } else if (event.getSource().getEntity() instanceof Player attacker) {
                boolean stealthy = attacker.isCrouching()
                        || attacker.isShiftKeyDown()
                        || MycorrhizalAcousticTracker.hasActiveDecoyNear(serverLevel, attacker.blockPosition(), 16);
                if (stealthy) {
                    Long lastShadow = LAST_SHADOW_HIT.get(attacker.getUUID());
                    if (lastShadow == null || (now - lastShadow) >= 40L) {
                        LAST_SHADOW_HIT.put(attacker.getUUID(), now);
                        SynapticMemoryRouter.routeHabit(serverLevel, mob.blockPosition(), PlayerHabit.SHADOW_INFILTRATOR, 2.5);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            LAST_MOVEMENT_CHECK.clear();
            LAST_ADHESIVE_TICK.clear();
            LAST_MEDIC_TICK.clear();
            LAST_BASTION_HIT.clear();
            LAST_SIEGE_ABSORPTION.clear();
            LAST_SHADOW_HIT.clear();
            lastCachePruneTick = Long.MIN_VALUE;
        }
    }

    private static long globalGameTime(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    private static void pruneThrottleCaches(long now) {
        if (lastCachePruneTick != Long.MIN_VALUE && now - lastCachePruneTick < 200L) {
            return;
        }
        long cutoff = now - THROTTLE_CACHE_MAX_AGE;
        LAST_MOVEMENT_CHECK.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        LAST_ADHESIVE_TICK.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        LAST_MEDIC_TICK.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        LAST_BASTION_HIT.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        LAST_SIEGE_ABSORPTION.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        LAST_SHADOW_HIT.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        lastCachePruneTick = now;
    }
}
