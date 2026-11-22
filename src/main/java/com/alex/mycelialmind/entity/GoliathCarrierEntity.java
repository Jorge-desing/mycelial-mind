package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Heavy brood carrier that releases Sporelings upon death.
 */
public class GoliathCarrierEntity extends MycelialMob {
    private boolean releasedMidFight = false;

    public GoliathCarrierEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.NecrophagyGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.85D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity target = this.getTarget();
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 110 == 0
                && target != null && target.isAlive()) {
            serverLevel.sendParticles(ModParticles.CHITIN_SHARD.get(), this.getX(), this.getY() + 0.2, this.getZ(),
                    6, 1.5, 0.2, 1.5, 0.02);
            this.playSound(ModSounds.GOLIATH_CARRIER_THUD.get(), 1.0F, 0.9F);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0D),
                    e -> !(e instanceof MycelialMob) && e.isAlive())) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.42, 0.0));
                victim.hurtMarked = true;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.releasedMidFight && this.isAlive()
                && this.getHealth() <= this.getMaxHealth() * 0.5F
                && this.level() instanceof ServerLevel serverLevel) {
            this.releasedMidFight = true;
            spawnBroodSporeling(serverLevel, 0.0);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 2; i++) {
                spawnBroodSporeling(serverLevel, i - 0.5);
            }
        }
    }

    private void spawnBroodSporeling(ServerLevel serverLevel, double xOffset) {
        SporelingEntity sporeling = ModEntities.SPORELING.get().create(serverLevel);
        if (sporeling != null) {
            var center = this.blockPosition().offset((int) Math.round(xOffset), 0, 0);
            var spawnPos = MycelialSpawnUtil.findOpenGroundPosition(serverLevel, sporeling, center, 3, 1);
            if (spawnPos != null) {
                sporeling.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.REINFORCEMENT, null);
                if (serverLevel.noCollision(sporeling)) {
                    serverLevel.addFreshEntity(sporeling);
                    serverLevel.playSound(null, spawnPos, ModSounds.GOLIATH_CARRIER_SPAWN.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
                }
            }
        }
    }

    @Override
    protected float getBaseSoundVolume() {
        return 1.25F;
    }

    @Override
    protected float getStepSoundVolume() {
        return 0.58F;
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_COLOSSAL.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GOLIATH_CARRIER_THUD.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.GOLIATH_CARRIER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GOLIATH_CARRIER_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 55.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.21D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 22.0D);
    }
}
