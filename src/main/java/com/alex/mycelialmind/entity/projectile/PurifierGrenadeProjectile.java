package com.alex.mycelialmind.entity.projectile;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.MobEpisodicMemory;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDamageTypes;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Purifying grenade that cleanses Creeping Mycelium and Spore Vents in a 4-block radius.
 */
public class PurifierGrenadeProjectile extends ThrowableProjectile {
    public PurifierGrenadeProjectile(EntityType<? extends PurifierGrenadeProjectile> type, Level level) {
        super(type, level);
    }

    public PurifierGrenadeProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.PURIFIER_GRENADE.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ModParticles.PURIFIER_CROSS.get(), this.getX(), this.getY(), this.getZ(), 0, 0.01, 0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos center = this.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -3, -4), center.offset(4, 3, 4))) {
                if (pos.distSqr(center) <= 16.0) {
                    BlockState state = serverLevel.getBlockState(pos);
                    if (state.is(ModBlocks.CREEPING_MYCELIUM.get())) {
                        serverLevel.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                    } else if (state.is(ModBlocks.SPORE_VENT.get())) {
                        serverLevel.destroyBlock(pos, false);
                    } else if (state.is(ModBlocks.EPHEMERAL_CHITIN.get()) || state.is(ModBlocks.ANTIBODY_CYST.get())) {
                        serverLevel.removeBlock(pos, false);
                    }
                    if (serverLevel.getBlockEntity(pos) instanceof PurificationTurretBlockEntity turret) {
                        turret.clearCorrosiveJam();
                    }
                }
            }
            com.alex.mycelialmind.hive.HyphalSiegeDirector.cleanseHyphalVeinAt(serverLevel, center, 6.0D);
            for (com.alex.mycelialmind.entity.HyphalVeinEntity strand : serverLevel.getEntitiesOfClass(
                    com.alex.mycelialmind.entity.HyphalVeinEntity.class, new AABB(center).inflate(4.5D))) {
                strand.disintegrateWithSpores();
                com.alex.mycelialmind.hive.HiveMindSavedData.get(serverLevel)
                        .severHyphalVein(center, 6.0D, serverLevel.getGameTime());
            }
            AABB area = new AABB(center).inflate(4.5D);
            for (MycelialMob mob : serverLevel.getEntitiesOfClass(MycelialMob.class, area)) {
                if (mob.isAlliedEscort()) {
                    continue;
                }
                if (mob.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) > 16.0D) {
                    continue;
                }
                mob.clearAdaptations();
                mob.triggerDesync(SynapticCommandSystem.DESYNC_DURATION_TICKS);
                mob.hurt(
                        ModDamageTypes.source(serverLevel, ModDamageTypes.PURIFICATION_PURGE, this, this.getOwner()),
                        10.0F
                );
                if (mob.isAlive()) {
                    mob.getEpisodicMemory().remember(MobEpisodicMemory.KEY_TRAUMA_GRENADE, serverLevel.getGameTime());
                }
            }
            HiveBlackboard.broadcastTacticalAlarm(serverLevel, center, 18.0D, 100);
            for (PurifierDroneEntity drone : serverLevel.getEntitiesOfClass(PurifierDroneEntity.class, area)) {
                if (drone.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) <= 16.0D) {
                    drone.clearJam();
                }
            }
            for (ColonySymbiontEntity symbiont : serverLevel.getEntitiesOfClass(ColonySymbiontEntity.class, area)) {
                if (symbiont.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) <= 16.0D) {
                    symbiont.heal(6.0F);
                    symbiont.removeEffect(ModEffects.MYCELIAL_INFECTION);
                    symbiont.removeEffect(ModEffects.SPORE_ROT);
                }
            }
            HiveBlackboard.purgeCarrionWithin(serverLevel, Vec3.atCenterOf(center), 5.0D);
            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                    this.getX(), this.getY() + 0.3, this.getZ(),
                    24, 1.5, 0.8, 1.5, 0.05);
            serverLevel.playSound(null, center, ModSounds.PURIFIER_GRENADE_DETONATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            this.discard();
        }
    }
}
