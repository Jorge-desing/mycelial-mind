package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.registry.ModEffects;
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
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
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
 * Parasitic disrupter that drains stamina (Hunger / Mining Fatigue) and leeches health on hit.
 */
public class LeechParasiteEntity extends MycelialMob {
    public LeechParasiteEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.35F));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.NecrophagyGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PurifierDroneEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.IronGolem.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            if (target instanceof PurifierDroneEntity drone) {
                drone.setJammedTicks(com.alex.mycelialmind.hive.LivingBattlefieldSystem.DRONE_PARASITIC_JAM_TICKS);
            } else if (target instanceof net.minecraft.world.entity.animal.IronGolem golem) {
                golem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        com.alex.mycelialmind.hive.LivingBattlefieldSystem.DRONE_PARASITIC_JAM_TICKS, 1));
                golem.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        com.alex.mycelialmind.hive.LivingBattlefieldSystem.DRONE_PARASITIC_JAM_TICKS, 0));
            }
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140, 1));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0));
            int infectionTicks = ChitinArmorItem.reduceInfectionDuration(living, 160);
            living.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, infectionTicks, 0));
            if (living instanceof Player p) {
                p.getFoodData().setSaturation(Math.max(0.0F, p.getFoodData().getSaturationLevel() - 1.0F));
            }
            this.heal(2.0F);
            this.playSound(ModSounds.LEECH_PARASITE_SIPHON.get(), 0.85F, 1.0F);
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, 0, false, false));
            for (MycelialMob ally : this.level().getEntitiesOfClass(MycelialMob.class, this.getBoundingBox().inflate(10.0D))) {
                if (!this.isRivalStrainTarget(ally)
                        && (ally instanceof ColonyWardenEntity || ally instanceof HiveCoreBossEntity)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, 0, false, false));
                    break;
                }
            }
        }
        return hit;
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
        return ModSounds.LEECH_PARASITE_SCREECH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.LEECH_PARASITE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LEECH_PARASITE_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }
}
