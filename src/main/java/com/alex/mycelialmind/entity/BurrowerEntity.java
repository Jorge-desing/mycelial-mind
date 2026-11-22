package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ai.HiveAlertSupportGoal;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
                    if (canBurrowerBreak(serverLevel, targetPos, maxHardness)) {
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
                || state.is(ModBlocks.EPHEMERAL_CHITIN.get())) {
            return false;
        }
        float speed = state.getDestroySpeed(level, targetPos);
        return speed >= 0.0F && speed < maxHardness;
    }
}
