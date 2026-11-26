package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.compat.claims.ClaimProtectionCompat;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ai.HiveAlertSupportGoal;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AnimatableMeleeAttack;
import com.alex.mycelialmind.entity.ai.brain.behaviour.InvalidateAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetWalkTargetToAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.DelayedBehaviour;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Siege organism capable of excavating through non-reinforced base walls.
 */
public class BurrowerEntity extends MycelialMob {
    public BurrowerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.TacticalTerraformingGoal(this));
        this.goalSelector.addGoal(1, new HiveAlertSupportGoal(this));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.SwarmFlankGoal(this, 1.15D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
        registerStandardAmbientGoals(0.85D, 8.0F);
        registerStandardPlayerTargets();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        var tunnelTremor = new DelayedBehaviour<MycelialMob>(15) {
            @Override
            protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
                return List.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
            }

            @Override
            protected boolean checkExtraStartConditions(ServerLevel level, MycelialMob entity) {
                return !entity.isDesynced() && !PurificationZoneTracker.get(level).isProtected(entity.blockPosition());
            }

            @Override
            protected void doDelayedAction(MycelialMob entity) {
                if (entity.level() instanceof ServerLevel level) {
                    level.sendParticles(ModParticles.SPORE_MOTE.get(),
                            entity.getX(), entity.getY() + 0.15D, entity.getZ(),
                            8, 0.4D, 0.15D, 0.4D, 0.02D);
                }
            }
        };
        tunnelTremor.cooldownFor(mob -> 80);

        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new ReactToUnreachableTarget<MycelialMob>()
                        .afterTicks(30L)
                        .reaction((level, mob) -> {
                            if (!mob.isDesynced() && !PurificationZoneTracker.get(level).isProtected(mob.blockPosition())) {
                                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 1, false, false));
                                level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                                        mob.getX(), mob.getY() + 0.2D, mob.getZ(),
                                        10, 0.45D, 0.2D, 0.45D, 0.03D);
                            }
                        }),
                tunnelTremor,
                new SetWalkTargetToAttackTarget<>(),
                new AnimatableMeleeAttack<>(8)
        );
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity target = this.getTarget();
        boolean siegeBoost = hasTier2Trait(AdaptationTrait.BLAST_SHIELDED) || hasSynergy(HiveSynergy.SIEGE_JUGGERNAUT);
        int interval = siegeBoost ? 18 : 30;
        if (this.level() instanceof ServerLevel serverLevel
                && this.tickCount % interval == 0
                && target != null && target.isAlive()
                && MycelialConfig.BURROWER_BLOCK_BREAKING.get()
                && !PurificationZoneTracker.get(serverLevel).isProtected(this.blockPosition())) {
            BlockPos front = this.blockPosition().relative(this.getDirection());
            int maxHardness = siegeBoost ? 25 : 15;
            int lateralLimit = siegeBoost ? 1 : 0;
            int broken = 0;
            for (int dy = 0; dy <= 1 && broken < (siegeBoost ? 2 : 1); dy++) {
                for (int lat = 0; lat <= lateralLimit && broken < (siegeBoost ? 2 : 1); lat++) {
                    BlockPos targetPos = front.above(dy).relative(this.getDirection().getClockWise(), lat);
                    if (!ClaimProtectionCompat.isClaimProtected(serverLevel, targetPos)
                            && canBurrowerBreak(serverLevel, targetPos, maxHardness)) {
                        serverLevel.destroyBlock(targetPos, true, this);
                        broken++;
                    }
                }
            }
            if (broken > 0) {
                serverLevel.playSound(null, this.blockPosition(), ModSounds.BURROWER_TUNNEL.get(), SoundSource.HOSTILE, 0.85F, 1.0F);
            }
        }
        // Subterranean ambush lunge when closing distance outside Pylon protection
        if (this.level() instanceof ServerLevel serverLevel
                && this.tickCount % 60 == 0
                && target != null && target.isAlive()
                && this.distanceToSqr(target) > 64.0D
                && !PurificationZoneTracker.get(serverLevel).isProtected(this.blockPosition())) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 1, false, false));
            serverLevel.sendParticles(ModParticles.SPORE_CLOUD.get(), this.getX(), this.getY() + 0.2, this.getZ(),
                    14, 0.5, 0.2, 0.5, 0.04);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.BURROWER_TUNNEL.get(), SoundSource.HOSTILE, 0.75F, 1.2F);
        }
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_ARACHNID.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BURROWER_GRIND.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.BURROWER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BURROWER_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public static boolean canBurrowerBreak(ServerLevel level, BlockPos targetPos, int maxHardness) {
        if (!level.isInWorldBounds(targetPos) || PurificationZoneTracker.get(level).isProtected(targetPos)) {
            return false;
        }
        if (level.getBlockEntity(targetPos) != null) {
            return false;
        }
        BlockState state = level.getBlockState(targetPos);
        if (state.isAir()
                || state.is(ModBlocks.REINFORCED_BIO_BARRIER.get())
                || state.is(ModBlocks.MYCELIAL_RIFT.get())
                || state.is(ModBlocks.HIVE_ALTAR.get())
                || state.is(ModBlocks.MATRIARCH_ALTAR.get())
                || state.is(ModBlocks.PURIFICATION_PYLON.get())
                || state.is(ModBlocks.PURIFICATION_TURRET.get())
                || state.is(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get())
                || state.is(ModBlocks.MYCELIAL_NODE.get())
                || state.is(ModBlocks.INCUBATOR_NODE.get())
                || state.is(ModBlocks.SENTINEL_NODE.get())
                || state.is(ModBlocks.SYMBIOTIC_NODE.get())
                || state.is(ModBlocks.SOVEREIGN_NEXUS.get())
                || state.is(ModBlocks.RECLAIMED_BLOOM.get())
                || state.is(ModBlocks.EPHEMERAL_CHITIN.get())
                || state.is(ModBlocks.HIVE_PROTECTED_BLOCKS)) {
            return false;
        }
        float speed = state.getDestroySpeed(level, targetPos);
        return speed >= 0.0F && speed < maxHardness;
    }
}
