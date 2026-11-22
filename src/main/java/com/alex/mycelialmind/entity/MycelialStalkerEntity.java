package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.entity.ai.EvasiveStrafeGoal;
import com.alex.mycelialmind.entity.ai.FlankingAmbushGoal;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import com.alex.mycelialmind.compat.sbl.HiveNodeRegenBehaviour;
import com.alex.mycelialmind.compat.sbl.StalkerFlankBehaviour;

/**
 * Ambush assassin that camouflages itself while standing on Creeping Mycelium.
 */
public class MycelialStalkerEntity extends MycelialMob {
    public MycelialStalkerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EvasiveStrafeGoal(this));
        this.goalSelector.addGoal(1, new FlankingAmbushGoal(this, 1.4D));
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.SwarmFlankGoal(this, 1.35D));
        this.goalSelector.addGoal(2, new com.alex.mycelialmind.entity.ai.HiveRegroupGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public BrainActivityGroup<MycelialMob> getCoreTasks() {
        return BrainActivityGroup.coreTasks(new StalkerFlankBehaviour());
    }

    @Override
    public BrainActivityGroup<MycelialMob> getIdleTasks() {
        return BrainActivityGroup.idleTasks(new HiveNodeRegenBehaviour()
                .startCondition(mob -> mob.getTarget() == null));
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        return BrainActivityGroup.fightTasks();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            if (this.level().getBlockState(this.blockPosition().below()).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide()) {
            this.playSound(ModSounds.STALKER_POUNCE.get(), 0.9F, 1.0F);
            boolean fromBehind = target.getLookAngle().dot(this.position().subtract(target.position()).normalize()) < -0.25D;
            if (!this.isDesynced() && (this.hasEffect(MobEffects.INVISIBILITY) || fromBehind)) {
                target.hurt(this.damageSources().mobAttack(this), 3.0F);
            }
            var right = this.getLookAngle().yRot((float) (Math.PI / 2.0));
            this.setDeltaMovement(right.x * 0.45, 0.15, right.z * 0.45);
            this.hasImpulse = true;
        }
        return hit;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_ARACHNID.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.STALKER_HISS.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.STALKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STALKER_DEATH.get();
    }
}
