package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.entity.ai.BruteChargeGoal;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
