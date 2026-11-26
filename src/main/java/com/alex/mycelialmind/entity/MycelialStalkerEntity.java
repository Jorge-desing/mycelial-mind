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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import com.alex.mycelialmind.entity.ai.brain.behaviour.FleeTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.MoveToWalkTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.FirstApplicableBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveNodeRegenBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.StalkerFlankBehaviour;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.registry.ModMemoryTypes;

import java.util.List;

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
        registerStandardAmbientGoals(1.0D, 8.0F);
        registerStandardPlayerTargets();
    }

    @Override
    public List<HiveSensor<MycelialMob>> getSensors() {
        return HiveBrainBridge.createDefaultHiveSensorsWithProjectiles();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getCoreTasks() {
        var fleeLowHp = new FleeTarget<MycelialMob>()
                .fleeDistance(12.0F)
                .speedModifier(1.3F);
        fleeLowHp.startCondition(mob -> mob.getHealth() < mob.getMaxHealth() * 0.35F && !mob.isFrenzyActive());

        var projectileSidestep = new CustomBehaviour<MycelialMob>(
                mob -> HiveBrainBridge.performProjectileSidestep(mob, 1.3F, true));
        projectileSidestep.startCondition(mob -> !mob.isDesynced()
                && HiveBrainUtils.hasMemory(mob, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileSidestep.cooldownFor(mob -> 30);

        return BrainActivityGroup.coreTasks(
                new StalkerFlankBehaviour(),
                new FirstApplicableBehaviour<>(fleeLowHp, projectileSidestep),
                new LookAtTarget<>(),
                new MoveToWalkTarget<>()
        );
    }

    @Override
    public BrainActivityGroup<MycelialMob> getIdleTasks() {
        return BrainActivityGroup.idleTasks(new HiveNodeRegenBehaviour()
                .startCondition(mob -> mob.getTarget() == null));
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new ReactToUnreachableTarget<MycelialMob>().afterTicks(40L)
        );
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
