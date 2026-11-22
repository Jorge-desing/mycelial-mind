package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.ai.HiveAlertSupportGoal;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Mini-boss guardian of Mycelial Nodes.
 */
public class ColonyWardenEntity extends MycelialMob {
    public ColonyWardenEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.TacticalTerraformingGoal(this));
        this.goalSelector.addGoal(1, new HiveAlertSupportGoal(this));
        this.goalSelector.addGoal(2, new com.alex.mycelialmind.entity.ai.SwarmFlankGoal(this, 1.05D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity target = this.getTarget();
        if (this.level() instanceof ServerLevel serverLevel && target != null && target.isAlive()) {
            if (this.tickCount % 60 == 0) {
                for (MycelialMob ally : serverLevel.getEntitiesOfClass(MycelialMob.class, this.getBoundingBox().inflate(12.0D))) {
                    if (!this.isRivalStrainTarget(ally)
                            && (ally instanceof SporelingEntity || ally instanceof LeechParasiteEntity)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, false, false));
                    }
                }
            }
            if (this.tickCount % 140 == 0) {
                this.playSound(ModSounds.COLONY_WARDEN_SHOCKWAVE.get(), 1.0F, 1.0F);
                serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(), this.getX(), this.getY(0.6), this.getZ(),
                        24, 2.5, 0.8, 2.5, 0.05);
                for (MycelialMob ally : serverLevel.getEntitiesOfClass(MycelialMob.class, this.getBoundingBox().inflate(12.0D))) {
                    if (!this.isRivalStrainTarget(ally)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, false));
                        ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false));
                    }
                }
                for (LivingEntity enemy : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0D),
                        e -> e.isAlive() && (!(e instanceof MycelialMob mycelial) || this.isRivalStrainTarget(mycelial)))) {
                    enemy.knockback(0.9F, this.getX() - enemy.getX(), this.getZ() - enemy.getZ());
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 110.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.6D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected float getBaseSoundVolume() {
        return 1.18F;
    }

    @Override
    protected float getStepSoundVolume() {
        return 0.44F;
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_HEAVY.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.COLONY_WARDEN_BELLOW.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.COLONY_WARDEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.COLONY_WARDEN_DEATH.get();
    }
}
