package com.alex.mycelialmind.entity.projectile;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.compat.claims.ClaimProtectionCompat;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.LivingBattlefieldSystem;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDamageTypes;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Corrosive projectile launched by Acid Spitters.
 */
public class AcidSpitProjectile extends ThrowableProjectile {
    public AcidSpitProjectile(EntityType<? extends AcidSpitProjectile> type, Level level) {
        super(type, level);
    }

    public AcidSpitProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.ACID_SPIT.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ModParticles.ACID_DROPLET.get(), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hitEntity = result.getEntity();
        Entity resolved = hitEntity instanceof PartEntity<?> part ? part.getParent() : hitEntity;
        if (!this.level().isClientSide() && resolved instanceof LivingEntity target) {
            boolean isMycelial = target instanceof MycelialMob;
            boolean rivalMycelial = isMycelial
                    && this.getOwner() instanceof MycelialMob shooter
                    && shooter.isRivalStrainTarget(target);
            if (!isMycelial || rivalMycelial) {
                hitEntity.hurt(ModDamageTypes.source(this.level(), ModDamageTypes.ACID_SPIT, this, this.getOwner()), 5.0F);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                if (!isMycelial) {
                    int infectionTicks = ChitinArmorItem.reduceInfectionDuration(target, 140);
                    target.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, infectionTicks, 0));
                }
                if (target instanceof PurifierDroneEntity drone && this.level() instanceof ServerLevel serverLevel) {
                    drone.setJammedTicks(LivingBattlefieldSystem.DRONE_PARASITIC_JAM_TICKS);
                    serverLevel.sendParticles(ModParticles.ACID_DROPLET.get(),
                            drone.getX(), drone.getY() + 0.5, drone.getZ(), 8, 0.25, 0.25, 0.25, 0.02);
                }
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos impactPos = result.getBlockPos();
            BlockPos adjacentPos = impactPos.relative(result.getDirection());
            if (serverLevel.getBlockEntity(impactPos) instanceof PurificationTurretBlockEntity turret) {
                turret.applyCorrosiveJam(LivingBattlefieldSystem.TURRET_CORROSIVE_JAM_TICKS);
                serverLevel.sendParticles(ModParticles.ACID_DROPLET.get(),
                        impactPos.getX() + 0.5, impactPos.getY() + 1.1, impactPos.getZ() + 0.5,
                        8, 0.25, 0.2, 0.25, 0.02);
            }
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (!tracker.isProtected(impactPos)) {
                tryCorrode(serverLevel, impactPos);
            }
            if (!tracker.isProtected(adjacentPos)) {
                tryCorrode(serverLevel, adjacentPos);
            }
        }
    }

    private static void tryCorrode(ServerLevel level, BlockPos pos) {
        if (ClaimProtectionCompat.isClaimProtected(level, pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.REINFORCED_BIO_BARRIER.get())
                || state.is(ModBlocks.HIVE_PROTECTED_BLOCKS)) {
            return;
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                || state.is(BlockTags.WOODEN_DOORS)) {
            level.destroyBlock(pos, false);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), ModSounds.ACID_SPIT_IMPACT.get(), SoundSource.HOSTILE, 0.75F, 0.95F + this.random.nextFloat() * 0.1F);
            this.discard();
        }
    }
}
