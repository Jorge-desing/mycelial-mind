package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * Thermal-adapted organism that absorbs fire and bursts on death.
 */
public class PyroShroomEntity extends MycelialMob {
    private int fuseTicks = -1;

    public PyroShroomEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel && this.isAlive()) {
            LivingEntity target = this.getTarget();
            if (fuseTicks < 0 && this.getHealth() <= this.getMaxHealth() * 0.35F
                    && target != null && target.isAlive()) {
                fuseTicks = 30;
                this.playSound(ModSounds.PYRO_SHROOM_IGNITE.get(), 1.0F, 1.0F);
            }
            if (fuseTicks > 0) {
                fuseTicks--;
                serverLevel.sendParticles(ModParticles.HIVE_EMBER.get(), this.getX(), this.getY(0.5), this.getZ(),
                        5, 0.3, 0.3, 0.3, 0.04);
                if (fuseTicks == 0) {
                    serverLevel.explode(this, this.getX(), this.getY(), this.getZ(), 2.6F, true, Level.ExplosionInteraction.NONE);
                    this.discard();
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            this.heal(1.0F);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide()) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.2F, true, Level.ExplosionInteraction.NONE);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PYRO_SHROOM_CRACKLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.PYRO_SHROOM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PYRO_SHROOM_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 22.0D);
    }
}
