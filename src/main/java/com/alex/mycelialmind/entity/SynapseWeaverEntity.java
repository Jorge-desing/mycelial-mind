package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.ai.EvasiveStrafeGoal;
import com.alex.mycelialmind.entity.ai.WeaverSupportGoal;
import com.alex.mycelialmind.entity.ai.brain.hive.WeaverSupportBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveNodeRegenBehaviour;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AvoidEntity;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.MoveToWalkTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetPlayerLookTarget;
import com.alex.mycelialmind.entity.ai.brain.core.AllApplicableBehaviours;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.FirstApplicableBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Support organism that links nearby mycelial mobs with regeneration and resistance.
 */
public class SynapseWeaverEntity extends MycelialMob {
    public SynapseWeaverEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EvasiveStrafeGoal(this));
        this.goalSelector.addGoal(1, new WeaverSupportGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.95D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
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
        var avoidPlayers = new AvoidEntity<MycelialMob>()
                .avoiding(candidate -> candidate instanceof Player player
                        && !player.isCreative()
                        && !player.isSpectator()
                        && !MycelialMob.isFriendlyToPlayer(this, player))
                .noCloserThan(6.5F)
                .speedModifier(1.18F);
        avoidPlayers.startCondition(mob -> !mob.isDesynced());

        var projectileSidestep = new CustomBehaviour<MycelialMob>(
                mob -> HiveBrainBridge.performProjectileSidestep(mob, 1.15F, true));
        projectileSidestep.startCondition(mob -> !mob.isDesynced()
                && HiveBrainUtils.hasMemory(mob, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileSidestep.cooldownFor(mob -> 35);

        return BrainActivityGroup.coreTasks(
                new WeaverSupportBehaviour(),
                new FirstApplicableBehaviour<>(avoidPlayers, projectileSidestep),
                new LookAtTarget<>(),
                new MoveToWalkTarget<>()
        );
    }

    @Override
    public BrainActivityGroup<MycelialMob> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new AllApplicableBehaviours<>(
                        new HiveNodeRegenBehaviour().startCondition(mob -> mob.getTarget() == null),
                        new SetPlayerLookTarget<>()
                )
        );
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new ReactToUnreachableTarget<MycelialMob>().afterTicks(50L)
        );
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel && !this.isDesynced() && this.tickCount % 60 == 0) {
            int linked = 0;
            for (MycelialMob ally : serverLevel.getEntitiesOfClass(MycelialMob.class, this.getBoundingBox().inflate(12.0D))) {
                if (ally == this || !ally.isAlive() || this.isRivalStrainTarget(ally)) {
                    continue;
                }
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 90, hasSynergy(HiveSynergy.ARCANE_ABSORPTION) ? 1 : 0));
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90, 0));
                ally.removeEffect(MobEffects.WEAKNESS);
                ally.removeEffect(MobEffects.POISON);
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 90, 0, false, false));
                if (linked < 3) {
                    serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                            (this.getX() + ally.getX()) * 0.5,
                            (this.getY(0.6) + ally.getY(0.6)) * 0.5,
                            (this.getZ() + ally.getZ()) * 0.5,
                            4, 0.15, 0.15, 0.15, 0.01);
                    linked++;
                }
            }
            if (linked > 0) {
                serverLevel.playSound(null, this.blockPosition(), ModSounds.SYNAPSE_WEAVER_BUFF.get(), SoundSource.HOSTILE, 0.55F, 1.0F);
            }
        }
        // Every 120 ticks, promote 1 nearby BASE unit to MUTATED (nearest first)
        if (this.level() instanceof ServerLevel serverLevel && !this.isDesynced() && this.tickCount % 120 == 0) {
            var allies = serverLevel.getEntitiesOfClass(MycelialMob.class, this.getBoundingBox().inflate(10.0D));
            allies.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
            for (MycelialMob ally : allies) {
                if (ally != this && ally.isAlive()
                        && !this.isRivalStrainTarget(ally)
                        && !(ally instanceof HiveCoreBossEntity)
                        && !(ally instanceof ApexMatriarchEntity)
                        && ally.getEvolutionRank() == EvolutionRank.BASE) {
                    ally.promoteRank(EvolutionRank.MUTATED);
                    serverLevel.sendParticles(ModParticles.BIO_LUME.get(), ally.getX(), ally.getY(0.5), ally.getZ(),
                            12, 0.3, 0.5, 0.3, 0.03);
                    break;
                }
            }
        }
    }

    @Override
    protected float getStepSoundVolume() {
        return 0.22F;
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_HOVER.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SYNAPSE_WEAVER_HUM.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SYNAPSE_WEAVER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SYNAPSE_WEAVER_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }
}
