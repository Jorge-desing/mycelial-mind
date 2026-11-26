package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AnimatableMeleeAttack;
import com.alex.mycelialmind.entity.ai.brain.behaviour.InvalidateAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetWalkTargetToAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.DelayedBehaviour;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import com.mojang.datafixers.util.Pair;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

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
        registerStandardAmbientGoals(0.75D, 8.0F);
        registerStandardPlayerTargets();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        var tremorTelegraph = new DelayedBehaviour<MycelialMob>(18) {
            @Override
            protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
                return List.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
            }

            @Override
            protected void doDelayedAction(MycelialMob entity) {
                if (entity.level() instanceof ServerLevel level) {
                    level.sendParticles(ModParticles.CHITIN_SHARD.get(),
                            entity.getX(), entity.getY() + 0.2D, entity.getZ(),
                            6, 0.8D, 0.2D, 0.8D, 0.02D);
                }
            }
        };
        tremorTelegraph.cooldownFor(mob -> 90);

        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new ReactToUnreachableTarget<MycelialMob>()
                        .afterTicks(45L)
                        .reaction((level, mob) -> {
                            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                            level.sendParticles(ModParticles.CHITIN_SHARD.get(),
                                    mob.getX(), mob.getY() + 0.2D, mob.getZ(),
                                    6, 0.8D, 0.2D, 0.8D, 0.02D);
                            mob.playSound(ModSounds.GOLIATH_CARRIER_THUD.get(), 0.9F, 0.9F);
                        }),
                tremorTelegraph,
                new SetWalkTargetToAttackTarget<>(),
                new AnimatableMeleeAttack<>(10)
        );
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
