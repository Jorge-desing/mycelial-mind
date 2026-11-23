package com.alex.mycelialmind.colony;

import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.HiveCycleSavedData;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.network.SovereignColonySyncPayload;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

/**
 * Coordinates Sovereign Colony passive aura buffs (Chimeras/Drones/Symbionts),
 * Rival Hive Mind probe raids against active colonies, and periodic client syncs.
 */
public final class SovereignColonyDirector {
    private static final String TAG_RIVAL_COLONY_ID = "MycelialRivalColonyTarget";

    private SovereignColonyDirector() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now % 100L != 0L) {
            return;
        }

        SovereignColonySavedData saved = SovereignColonySavedData.get(level);
        if (saved.getAllColonies().isEmpty()) {
            return;
        }

        HiveMindSavedData hostileHive = HiveMindSavedData.get(level);
        double threat = hostileHive.getThreatLevel();
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        int activeCycle = cycles.isCycleActive() ? cycles.getActiveCycleNumber() : 0;

        for (SovereignColonyData colony : saved.getAllColonies()) {
            if (!level.isLoaded(colony.getNexusPos())) {
                continue;
            }
            applyGuardianAura(level, colony);

            // Check raid timeout
            if (colony.getActiveRaidRemaining() > 0 && now >= colony.getActiveRaidUntilTick()) {
                colony.setActiveRaidRemaining(0);
                saved.setDirty();
            }

            // Trigger rival Hive Mind probe if hostile threat >= 50% (or active Cycle) and cooldown elapsed
            boolean highThreat = threat >= 50.0D || activeCycle > 0;
            if (highThreat
                    && colony.getRoster().size() >= 2
                    && colony.getActiveRaidRemaining() == 0
                    && (now - colony.getLastRaidTick()) >= SovereignColonyEngine.RIVAL_RAID_COOLDOWN_TICKS) {
                triggerRivalRaid(level, colony, activeCycle);
            }
        }

        if (now % 200L == 0L) {
            for (ServerPlayer player : level.players()) {
                SovereignColonyData nearest = saved.findContainingColony(player.blockPosition());
                if (nearest != null) {
                    SovereignColonySyncPayload.syncToPlayer(player, nearest.getNexusPos(), false);
                }
            }
        }
    }

    private static void applyGuardianAura(ServerLevel level, SovereignColonyData colony) {
        boolean guardianMatrix = colony.hasTrait(SovereignTrait.APEX_GUARDIAN_MATRIX);
        double range = Math.min(48.0D, colony.getEffectiveRadius());
        AABB box = new AABB(colony.getNexusPos()).inflate(range);
        if (guardianMatrix) {
            for (MycelialChimeraEntity chimera : level.getEntitiesOfClass(MycelialChimeraEntity.class, box)) {
                if (chimera.isAlive() && chimera.getHealth() < chimera.getMaxHealth()) {
                    chimera.heal(2.0F);
                }
                chimera.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 140, 0, false, false));
            }
            for (PurifierDroneEntity drone : level.getEntitiesOfClass(PurifierDroneEntity.class, box)) {
                if (drone.isAlive() && drone.getHealth() < drone.getMaxHealth()) {
                    drone.heal(1.5F);
                }
            }
        }
    }

    public static int triggerRivalRaid(ServerLevel level, SovereignColonyData colony, int activeCycle) {
        long now = level.getGameTime();
        int count = SovereignColonyEngine.computeRivalRaidSquadSize(colony.getTier(), colony.getRoster().size(), activeCycle);
        BlockPos nexus = colony.getNexusPos();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            int dist = 16 + level.random.nextInt(8);
            int sx = nexus.getX() + (int) Math.round(Math.cos(angle) * dist);
            int sz = nexus.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            if (Math.abs(sy - nexus.getY()) > 12) {
                sy = nexus.getY();
            }

            EntityType<? extends MycelialMob> type = pickRaiderType(colony.getTier(), i);
            MycelialMob mob = type.create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(sx + 0.5D, sy + 0.2D, sz + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
            mob.getPersistentData().putUUID(TAG_RIVAL_COLONY_ID, colony.getColonyId());

            // Target nearest symbiont so combat engages immediately
            List<ColonySymbiontEntity> defenders = level.getEntitiesOfClass(
                    ColonySymbiontEntity.class, new AABB(nexus).inflate(32.0D), LivingEntity::isAlive);
            if (!defenders.isEmpty()) {
                mob.setTarget(defenders.get(i % defenders.size()));
            }
            if (level.addFreshEntity(mob)) {
                spawned++;
            }
        }

        if (spawned > 0) {
            colony.setLastRaidTick(now);
            colony.setActiveRaidUntilTick(now + 2400L);
            colony.setActiveRaidRemaining(spawned);
            SovereignColonySavedData.get(level).setDirty();
            level.playSound(null, nexus, ModSounds.FUNGAL_TIDE_HORN.get(), SoundSource.HOSTILE, 1.1F, 1.15F);
            level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                    nexus.getX() + 0.5, nexus.getY() + 1.2, nexus.getZ() + 0.5, 1, 0, 0, 0, 0);

            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(nexus).inflate(64.0D))) {
                player.displayClientMessage(
                        Component.translatable("message.mycelialmind.sovereign_rival_raid_started", spawned)
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        false);
                SovereignColonySyncPayload.syncToPlayer(player, nexus, false);
            }
        }
        return spawned;
    }

    private static EntityType<? extends MycelialMob> pickRaiderType(int tier, int index) {
        if (tier >= 3 && index == 0) {
            return ModEntities.COLONY_WARDEN.get();
        }
        return switch (index % 3) {
            case 0 -> ModEntities.CHITIN_BRUTE.get();
            case 1 -> ModEntities.ACID_SPITTER.get();
            default -> ModEntities.MYCELIAL_STALKER.get();
        };
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof MycelialMob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!mob.getPersistentData().hasUUID(TAG_RIVAL_COLONY_ID)) {
            return;
        }
        var colonyId = mob.getPersistentData().getUUID(TAG_RIVAL_COLONY_ID);
        SovereignColonySavedData saved = SovereignColonySavedData.get(level);
        SovereignColonyData colony = saved.getById(colonyId);
        if (colony == null || colony.getActiveRaidRemaining() <= 0) {
            return;
        }
        int remaining = Math.max(0, colony.getActiveRaidRemaining() - 1);
        colony.setActiveRaidRemaining(remaining);
        if (remaining == 0) {
            int xpGain = SovereignColonyEngine.computeRaidDefenseDomainXp(colony.getTier(), 4);
            colony.recordRaidVictory();
            colony.addDomainXp(xpGain);
            colony.recalculateMorale();
            level.playSound(null, colony.getNexusPos(), ModSounds.COLONY_SYMBIONT_PROMOTE.get(),
                    SoundSource.BLOCKS, 1.1F, 1.2F);
            for (ServerPlayer player : level.getEntitiesOfClass(
                    ServerPlayer.class, new AABB(colony.getNexusPos()).inflate(64.0D))) {
                player.displayClientMessage(Component.translatable(
                        "message.mycelialmind.sovereign_rival_raid_defeated", xpGain)
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
                SovereignColonySyncPayload.syncToPlayer(player, colony.getNexusPos(), false);
            }
        }
        saved.setDirty();
    }
}
