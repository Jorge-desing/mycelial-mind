package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.entity.ai.brain.behaviour.AnimatableMeleeAttack;
import com.alex.mycelialmind.entity.ai.brain.behaviour.InvalidateAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetWalkTargetToAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.DelayedBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SinkholeEcosystemEngine;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * Territorial guardian anchored to the Root Cavern of a Mycelial Sinkhole.
 * Scales aggression range with HiveProgressionTier and weakens when its cavity
 * is purified by a Purification Pylon.
 */
public class RootKeeperEntity extends MycelialMob {
    public RootKeeperEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
        registerStandardAmbientGoals(0.8D, 9.0F);
        registerStandardPlayerTargets();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        var rootSlamTelegraph = new DelayedBehaviour<MycelialMob>(16) {
            @Override
            protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
                return List.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
            }

            @Override
            protected void doDelayedAction(MycelialMob entity) {
                if (entity.level() instanceof ServerLevel serverLevel) {
                    boolean purified = PurificationZoneTracker.get(serverLevel).isProtected(entity.blockPosition());
                    if (purified || entity.isDesynced()) {
                        return;
                    }
                    entity.playSound(ModSounds.ROOT_KEEPER_SLAM.get(), 1.0F, 0.92F);
                    serverLevel.sendParticles(ModParticles.CHITIN_SHARD.get(),
                            entity.getX(), entity.getY() + 0.3D, entity.getZ(),
                            14, 1.4D, 0.25D, 1.4D, 0.05D);
                    for (LivingEntity victim : serverLevel.getEntitiesOfClass(
                            LivingEntity.class, entity.getBoundingBox().inflate(3.5D),
                            e -> e.isAlive() && !(e instanceof MycelialMob) && !(e instanceof SporeGliderEntity))) {
                        victim.hurt(serverLevel.damageSources().mobAttack(entity), 5.0F);
                        victim.knockback(0.65F, entity.getX() - victim.getX(), entity.getZ() - victim.getZ());
                    }
                }
            }
        };
        rootSlamTelegraph.cooldownFor(mob -> 100);

        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new ReactToUnreachableTarget<MycelialMob>()
                        .afterTicks(40L)
                        .reaction((level, mob) -> {
                            mob.playSound(ModSounds.ROOT_KEEPER_SLAM.get(), 0.85F, 1.05F);
                            level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                                    mob.getX(), mob.getY() + 0.5D, mob.getZ(),
                                    6, 0.7D, 0.3D, 0.7D, 0.03D);
                        }),
                rootSlamTelegraph,
                new SetWalkTargetToAttackTarget<>(),
                new AnimatableMeleeAttack<>(12)
        );
    }

    @Override
    public Map<Activity, BrainActivityGroup<? extends MycelialMob>> getAdditionalTasks() {
        return HiveBrainBridge.createNodeInvestigateTasks();
    }

    @Override
    public List<Activity> getActivityPriorities() {
        return List.of(Activity.FIGHT, Activity.INVESTIGATE, Activity.IDLE);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 20 == 0) {
            boolean purified = PurificationZoneTracker.get(serverLevel).isProtected(this.blockPosition());
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            HiveProgressionTier tier = HiveProgressionTier.resolveEffectiveTier(
                    hive.getThreatLevel(), hive.getHighestTierReached());
            double aggroRange = SinkholeEcosystemEngine.computeRootKeeperAggroRange(tier, purified);
            AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null && Math.abs(followRange.getBaseValue() - aggroRange) > 0.1D) {
                followRange.setBaseValue(aggroRange);
            }
            if (purified) {
                this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false));
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
            }
        }
    }

    @Override
    protected float getBaseSoundVolume() {
        return 1.10F;
    }

    @Override
    protected float getStepSoundVolume() {
        return 0.45F;
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_HEAVY.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ROOT_KEEPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.ROOT_KEEPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ROOT_KEEPER_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 54.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 7.5D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }
}
