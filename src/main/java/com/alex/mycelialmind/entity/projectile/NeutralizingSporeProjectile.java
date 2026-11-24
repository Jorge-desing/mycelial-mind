package com.alex.mycelialmind.entity.projectile;

import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Counter-adaptation spore projectile that strips active AdaptationTraits from MycelialMobs.
 */
public class NeutralizingSporeProjectile extends ThrowableProjectile {
    public NeutralizingSporeProjectile(EntityType<? extends NeutralizingSporeProjectile> type, Level level) {
        super(type, level);
    }

    public NeutralizingSporeProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.NEUTRALIZING_SPORE.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.tickCount % 2 == 0) {
            this.level().addParticle(ModParticles.TRAIL.get(), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity hitEntity = result.getEntity();
            Entity target = hitEntity instanceof PartEntity<?> part ? part.getParent() : hitEntity;
            if (target instanceof MycelialMob mob && !mob.isAlliedEscort()) {
                if (mob instanceof com.alex.mycelialmind.entity.PyroShroomEntity pyro
                        && com.alex.mycelialmind.hive.EcosystemPurposeEngine.shouldDefusePyroShroom(
                                pyro.getFuseTicks(), false, true)) {
                    pyro.tryDefuseFuse(serverLevel);
                }
                mob.clearAdaptations();
                mob.triggerDesync(SynapticCommandSystem.DESYNC_DURATION_TICKS);
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
                hitEntity.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 4.0F);
                emitImpact(serverLevel, result);
            } else if (target instanceof PurifierDroneEntity drone
                    && drone.clearJam()) {
                emitImpact(serverLevel, result);
            } else if (target instanceof ColonySymbiontEntity symbiont) {
                symbiont.heal(4.0F);
                symbiont.removeEffect(ModEffects.MYCELIAL_INFECTION);
                symbiont.removeEffect(ModEffects.SPORE_ROT);
                emitImpact(serverLevel, result);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = result.getBlockPos();
            if (serverLevel.getBlockState(pos).is(ModBlocks.EPHEMERAL_CHITIN.get())) {
                serverLevel.removeBlock(pos, false);
                emitImpact(serverLevel, result);
            } else if (serverLevel.getBlockState(pos).is(ModBlocks.HYPHAL_VEIN.get())
                    && com.alex.mycelialmind.hive.HyphalSiegeDirector.cleanseHyphalVeinAt(serverLevel, pos, 5.0D)) {
                emitImpact(serverLevel, result);
            }
            if (serverLevel.getBlockEntity(pos) instanceof PurificationTurretBlockEntity turret && turret.isJammed()) {
                turret.clearCorrosiveJam();
                emitImpact(serverLevel, result);
            }
        }
    }

    private void emitImpact(ServerLevel level, HitResult hit) {
        net.minecraft.world.phys.Vec3 direction = this.getDeltaMovement().normalize().scale(-1.0);
        net.minecraft.world.phys.Vec3 position = hit.getLocation();
        level.sendParticles(ModParticles.IMPACT.get(), position.x, position.y, position.z,
                0, direction.x, direction.y, direction.z, 1);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            HiveBlackboard.purgeCarrionWithin(serverLevel, result.getLocation(), 2.5D);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.NEUTRALIZING_SPORE_IMPACT.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
            this.discard();
        }
    }
}
