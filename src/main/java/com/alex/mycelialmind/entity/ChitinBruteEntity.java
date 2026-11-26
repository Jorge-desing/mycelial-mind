package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.entity.ai.BruteChargeGoal;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AnimatableMeleeAttack;
import com.alex.mycelialmind.entity.ai.brain.behaviour.InvalidateAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetWalkTargetToAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.core.SequentialBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveHotspotPatrolBehaviour;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * Heavy frontline organism that strongly expresses defensive Hive Mind mutations.
 */
public class ChitinBruteEntity extends MycelialMob {
    public ChitinBruteEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.TacticalTerraformingGoal(this));
        this.goalSelector.addGoal(1, new BruteChargeGoal(this));
        this.goalSelector.addGoal(2, new com.alex.mycelialmind.entity.ai.SwarmFlankGoal(this, 1.05D));
        this.goalSelector.addGoal(2, new com.alex.mycelialmind.entity.ai.HiveRegroupGoal(this, 1.05D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        registerStandardAmbientGoals(0.8D, 8.0F);
        registerStandardPlayerTargets();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        var lookAndPress = new SequentialBehaviour<>(
                new CustomBehaviour<MycelialMob>(mob -> {
                    LivingEntity target = HiveBrainUtils.getTargetOfEntity(mob, mob.getTarget());
                    if (target != null && target.isAlive()) {
                        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                    }
                }),
                new SetWalkTargetToAttackTarget<MycelialMob>().speedMod(1.12F)
        );
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new ReactToUnreachableTarget<MycelialMob>()
                        .afterTicks(40L)
                        .reaction((level, mob) -> {
                            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                            if (mob.getTarget() != null) {
                                mob.getLookControl().setLookAt(mob.getTarget(), 30.0F, 30.0F);
                            }
                        }),
                lookAndPress,
                new AnimatableMeleeAttack<>(8)
        );
    }

    @Override
    public Map<Activity, BrainActivityGroup<? extends MycelialMob>> getAdditionalTasks() {
        var patrol = new HiveHotspotPatrolBehaviour();
        patrol.startCondition(mob -> HiveBrainBridge.getHiveAlertLevel(mob) >= 0.4F);
        BrainActivityGroup<MycelialMob> investigate = new BrainActivityGroup<MycelialMob>(Activity.INVESTIGATE)
                .onlyStartWithMemoryStatus(ModMemoryTypes.SHARED_TARGET_POS.get(), MemoryStatus.VALUE_PRESENT)
                .onlyStartWithMemoryStatus(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT)
                .behaviours(patrol);
        return Map.of(Activity.INVESTIGATE, investigate);
    }

    @Override
    public List<Activity> getActivityPriorities() {
        return List.of(Activity.FIGHT, Activity.INVESTIGATE, Activity.IDLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide()) {
            if (target instanceof Player player && player.isBlocking()) {
                player.disableShield();
            }
            this.playSound(ModSounds.CHITIN_BRUTE_SMASH.get(), 0.95F, 0.9F + this.random.nextFloat() * 0.2F);
        }
        return hit;
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
        return ModSounds.CHITIN_BRUTE_GROWL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.CHITIN_BRUTE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CHITIN_BRUTE_DEATH.get();
    }
}
