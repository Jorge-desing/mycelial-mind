package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.ai.FlankingAmbushGoal;
import com.alex.mycelialmind.entity.ai.brain.behaviour.FleeTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.MoveToWalkTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.FirstApplicableBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveHotspotPatrolBehaviour;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * Fast swarm scout of the Mycelial Mind.
 */
public class SporelingEntity extends MycelialMob {
    public SporelingEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FlankingAmbushGoal(this, 1.3D));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.NecrophagyGoal(this, 1.25D));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.SwarmFlankGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new com.alex.mycelialmind.entity.ai.HiveRegroupGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public List<HiveSensor<MycelialMob>> getSensors() {
        return HiveBrainBridge.createDefaultHiveSensorsWithProjectiles();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getCoreTasks() {
        var fleeAloneLowHp = new FleeTarget<MycelialMob>()
                .fleeDistance(12.0F)
                .speedModifier(1.3F);
        fleeAloneLowHp.startCondition(mob -> HiveBrainBridge.getNearbyAllyCount(mob) == 0
                && mob.getHealth() < mob.getMaxHealth() * 0.45F
                && !mob.isFrenzyActive());

        var projectileSidestep = new CustomBehaviour<MycelialMob>(
                mob -> HiveBrainBridge.performProjectileSidestep(mob, 1.3F, true));
        projectileSidestep.startCondition(mob -> !mob.isDesynced()
                && HiveBrainUtils.hasMemory(mob, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileSidestep.cooldownFor(mob -> 25);

        return BrainActivityGroup.coreTasks(
                new LookAtTarget<>(),
                new MoveToWalkTarget<>(),
                new FirstApplicableBehaviour<>(fleeAloneLowHp, projectileSidestep)
        );
    }

    @Override
    public Map<Activity, BrainActivityGroup<? extends MycelialMob>> getAdditionalTasks() {
        BrainActivityGroup<MycelialMob> investigate = new BrainActivityGroup<MycelialMob>(Activity.INVESTIGATE)
                .onlyStartWithMemoryStatus(ModMemoryTypes.SHARED_TARGET_POS.get(), MemoryStatus.VALUE_PRESENT)
                .onlyStartWithMemoryStatus(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT)
                .behaviours(new HiveHotspotPatrolBehaviour());
        return Map.of(Activity.INVESTIGATE, investigate);
    }

    @Override
    public List<Activity> getActivityPriorities() {
        return List.of(Activity.FIGHT, Activity.INVESTIGATE, Activity.IDLE);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.tickCount % 30 == 0) {
            int packCount = this.level().getEntitiesOfClass(SporelingEntity.class, this.getBoundingBox().inflate(6.0D),
                    s -> s != this && s.isAlive()).size();
            if (packCount > 0) {
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide()) {
            int packCount = Math.min(3, this.level().getEntitiesOfClass(SporelingEntity.class, this.getBoundingBox().inflate(6.0D),
                    s -> s != this && s.isAlive()).size());
            if (packCount > 0) {
                target.hurt(this.damageSources().mobAttack(this), packCount * 1.0F);
            }
        }
        return hit;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (this.level() instanceof ServerLevel serverLevel && cause.getEntity() instanceof ServerPlayer player) {
            BlockPos respawn = player.getRespawnPosition();
            if (respawn != null && respawn.distSqr(this.blockPosition()) <= 32.0 * 32.0) {
                HiveMindSavedData.get(serverLevel).recordStimulus(
                        StimulusType.MELEE, 5.0, serverLevel.getGameTime());
                serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(), this.getX(), this.getY() + 0.4, this.getZ(),
                        10, 0.3, 0.4, 0.3, 0.04);
                serverLevel.playSound(null, this.blockPosition(), ModSounds.SPORELING_BURST.get(), SoundSource.HOSTILE, 0.85F, 1.0F);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected float getBaseSoundVolume() {
        return 0.78F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 165;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SPORELING_CHITTER.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SPORELING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPORELING_DEATH.get();
    }
}
