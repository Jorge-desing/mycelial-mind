package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.entity.ai.brain.behaviour.AnimatableMeleeAttack;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AvoidEntity;
import com.alex.mycelialmind.entity.ai.brain.behaviour.InvalidateAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetWalkTargetToAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.DelayedBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.FirstApplicableBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.SequentialBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Thermal-adapted organism that absorbs fire, cauterizes surrounding organic barriers
 * on detonation, and can have its lit fuse extinguished for a luminescent amber vein drop.
 */
public class PyroShroomEntity extends MycelialMob {
    private static final String TAG_DEFUSED_DROP_CLAIMED = "DefusedDropClaimed";

    private int fuseTicks = -1;
    private boolean defusedDropClaimed = false;

    public PyroShroomEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        registerStandardAmbientGoals(0.9D, 8.0F);
        registerStandardPlayerTargets();
    }

    @Override
    public List<HiveSensor<MycelialMob>> getSensors() {
        return HiveBrainBridge.createDefaultHiveSensorsWithProjectiles();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        var retreatWhenDefused = new AvoidEntity<MycelialMob>()
                .avoiding(target -> target instanceof Player player && !player.isCreative() && !player.isSpectator())
                .noCloserThan(7.0F)
                .speedModifier(1.22F);
        retreatWhenDefused.startCondition(mob -> !isPrimed() && mob.hasEffect(MobEffects.WEAKNESS));

        var thermalTelegraph = new SequentialBehaviour<>(
                new DelayedBehaviour<MycelialMob>(10) {
                    @Override
                    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
                        return List.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
                    }

                    @Override
                    protected void start(MycelialMob entity) {
                        if (entity.level() instanceof ServerLevel level) {
                            level.sendParticles(ModParticles.HIVE_EMBER.get(),
                                    entity.getX(), entity.getY(0.75D), entity.getZ(),
                                    4, 0.25D, 0.2D, 0.25D, 0.02D);
                        }
                    }

                    @Override
                    protected void doDelayedAction(MycelialMob entity) {
                        entity.playSound(ModSounds.PYRO_SHROOM_CRACKLE.get(), 0.7F, 1.2F);
                    }
                },
                new SetWalkTargetToAttackTarget<MycelialMob>().speedMod(1.1F)
        );
        thermalTelegraph.cooldownFor(mob -> 60);

        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new ReactToUnreachableTarget<MycelialMob>().afterTicks(45L),
                new FirstApplicableBehaviour<>(
                        retreatWhenDefused,
                        thermalTelegraph,
                        new SetWalkTargetToAttackTarget<>()
                ),
                new AnimatableMeleeAttack<>(8)
        );
    }

    public boolean isPrimed() {
        return fuseTicks > 0;
    }

    public int getFuseTicks() {
        return fuseTicks;
    }

    public void igniteFuse(int ticks) {
        if (this.fuseTicks < 0 && ticks > 0) {
            this.fuseTicks = ticks;
            this.playSound(ModSounds.PYRO_SHROOM_IGNITE.get(), 1.0F, 1.0F);
        }
    }

    public boolean tryDefuseFuse(ServerLevel level) {
        if (this.fuseTicks <= 0) {
            return false;
        }
        this.fuseTicks = -1;
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                this.getX(), this.getY(0.6D), this.getZ(),
                10, 0.35D, 0.35D, 0.35D, 0.03D);
        level.playSound(null, this.blockPosition(), ModSounds.PYRO_SHROOM_DEFUSE.get(),
                SoundSource.NEUTRAL, 0.85F, 1.05F);
        if (!this.defusedDropClaimed) {
            this.defusedDropClaimed = true;
            this.spawnAtLocation(new ItemStack(ModBlocks.LUMINESCENT_AMBER_VEIN.get().asItem()));
        }
        return true;
    }

    public int cauterizeNearbyTissue(ServerLevel level) {
        int r = EcosystemPurposeEngine.PYRO_SHROOM_CAUTERIZE_RADIUS;
        BlockPos center = this.blockPosition();
        int removed = 0;
        for (BlockPos scan : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            BlockState st = level.getBlockState(scan);
            if ((st.is(ModBlocks.ANTIBODY_CYST.get()) || st.is(ModBlocks.EPHEMERAL_CHITIN.get()))
                    && level.removeBlock(scan, false)) {
                removed++;
            }
        }
        if (removed > 0) {
            level.sendParticles(ModParticles.HIVE_EMBER.get(),
                    this.getX(), this.getY(0.5D), this.getZ(),
                    12, 0.8D, 0.5D, 0.8D, 0.04D);
        }
        return removed;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel && this.isAlive()) {
            LivingEntity target = this.getTarget();
            if (fuseTicks < 0 && this.getHealth() <= this.getMaxHealth() * 0.35F
                    && target != null && target.isAlive()) {
                igniteFuse(30);
            }
            if (fuseTicks > 0) {
                if (EcosystemPurposeEngine.shouldDefusePyroShroom(
                        fuseTicks, this.isInWaterRainOrBubble(), false)) {
                    tryDefuseFuse(serverLevel);
                    return;
                }
                fuseTicks--;
                serverLevel.sendParticles(ModParticles.HIVE_EMBER.get(), this.getX(), this.getY(0.5), this.getZ(),
                        5, 0.3, 0.3, 0.3, 0.04);
                if (fuseTicks == 0) {
                    cauterizeNearbyTissue(serverLevel);
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
        if (this.level() instanceof ServerLevel serverLevel
                && EcosystemPurposeEngine.shouldDefusePyroShroom(
                        this.fuseTicks,
                        source.is(DamageTypeTags.IS_DROWNING) || this.isInWaterRainOrBubble(),
                        false)) {
            tryDefuseFuse(serverLevel);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (this.level() instanceof ServerLevel serverLevel) {
            cauterizeNearbyTissue(serverLevel);
            serverLevel.explode(this, this.getX(), this.getY(), this.getZ(), 2.2F, true, Level.ExplosionInteraction.NONE);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_DEFUSED_DROP_CLAIMED, defusedDropClaimed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.defusedDropClaimed = tag.getBoolean(TAG_DEFUSED_DROP_CLAIMED);
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
