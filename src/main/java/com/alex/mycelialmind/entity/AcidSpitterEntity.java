package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.core.AllApplicableBehaviours;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.AcidSpitterCombatBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveNodeRegenBehaviour;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.HurtBySensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.IncomingProjectilesSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyLivingEntitySensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyPlayersSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.UnreachableTargetSensor;
import com.alex.mycelialmind.entity.ai.TacticalTerraformingGoal;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ranged artillery organism that launches corrosive AcidSpitProjectiles.
 */
public class AcidSpitterEntity extends MycelialMob implements RangedAttackMob {
    public AcidSpitterEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TacticalTerraformingGoal(this));
    }

    @Override
    public List<HiveSensor<MycelialMob>> getSensors() {
        return List.of(
                new NearbyLivingEntitySensor<MycelialMob>()
                        .setRadius(24)
                        .setPredicate((target, mob) -> target instanceof Player || mob.isRivalStrainTarget(target)),
                new NearbyPlayersSensor<MycelialMob>().setRadius(24),
                new HurtBySensor<>(),
                new UnreachableTargetSensor<>(),
                new IncomingProjectilesSensor<>(),
                new HiveMindSensor()
        );
    }

    @Override
    public BrainActivityGroup<MycelialMob> getCoreTasks() {
        var evadeProjectile = new CustomBehaviour<MycelialMob>(
                mob -> HiveBrainBridge.performProjectileSidestep(mob, 1.25F, false));
        evadeProjectile.startCondition(mob -> !mob.isDesynced()
                && HiveBrainUtils.hasMemory(mob, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));

        var lookAtProjectile = new CustomBehaviour<MycelialMob>(mob -> {
            BlockPos pos = HiveBrainBridge.getIncomingProjectilePos(mob);
            if (pos != null) {
                HiveBrainUtils.setForgettableMemory(mob, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos), 20);
                if (mob.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                            mob.getX(), mob.getY(0.8D), mob.getZ(), 2, 0.2D, 0.15D, 0.2D, 0.02D);
                }
            }
        });
        lookAtProjectile.startCondition(mob -> !mob.isDesynced()
                && HiveBrainUtils.hasMemory(mob, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));

        var parallelReaction = new AllApplicableBehaviours<>(evadeProjectile, lookAtProjectile);
        parallelReaction.cooldownFor(mob -> 25);

        return BrainActivityGroup.coreTasks(
                new AcidSpitterCombatBehaviour(this),
                parallelReaction
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
                new ReactToUnreachableTarget<MycelialMob>().afterTicks(45L)
        );
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        AcidSpitProjectile spit = new AcidSpitProjectile(this.level(), this);
        double maxLead = isDesynced()
                ? 0.0D
                : (getCommandRole() != SynapticCommandSystem.CommandRole.NONE
                        ? SynapticCommandSystem.MAX_SYNCED_LEAD_SECONDS
                        : SynapticCommandSystem.SOLO_BASE_LEAD_SECONDS);
        Vec3 targetBase = new Vec3(target.getX(), target.getY(0.3333333333333333D), target.getZ());
        Vec3 aimPos = SynapticCommandSystem.computeBallisticLeadPos(
                spit.position(), targetBase, target.getDeltaMovement(), 1.35D, maxLead);

        double dx = aimPos.x - this.getX();
        double dy = aimPos.y - spit.getY();
        double dz = aimPos.z - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float inaccuracy = isDesynced() ? 10.0F : 5.0F;
        spit.shoot(dx, dy + horizontal * 0.2D, dz, 1.35F, inaccuracy);
        this.playSound(ModSounds.ACID_SPITTER_SPIT.get(), 1.0F, 1.0F);
        this.level().addFreshEntity(spit);

        if (getEvolutionRank() == EvolutionRank.ELITE && !isDesynced()) {
            AcidSpitProjectile split = new AcidSpitProjectile(this.level(), this);
            split.shoot(dx * 0.96 - dz * 0.15, dy + horizontal * 0.2D, dx * 0.15 + dz * 0.96, 1.35F, 4.0F);
            this.level().addFreshEntity(split);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_ARACHNID.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ACID_SPITTER_GURGLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.ACID_SPITTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ACID_SPITTER_DEATH.get();
    }
}
