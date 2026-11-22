package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.ai.KitingRetreatGoal;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveSquadComposer;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.item.NeutralizingSporeLauncherItem;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Adaptive fungal doppelgänger that scans the target player's equipment and dominant
 * Hive stimulus, dynamically shifting between Blade Dancer, Spore Marksman, and Aegis Mirror stances.
 */
public class SynapticMimicEntity extends MycelialMob {
    public enum MimicStance {
        BLADE_DANCER(0),
        SPORE_MARKSMAN(1),
        AEGIS_MIRROR(2);

        private final int id;

        MimicStance(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static MimicStance fromId(int id) {
            for (MimicStance stance : values()) {
                if (stance.id == id) {
                    return stance;
                }
            }
            return BLADE_DANCER;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_MIMIC_STANCE =
            SynchedEntityData.defineId(SynapticMimicEntity.class, EntityDataSerializers.INT);
    private static final String TAG_STANCE = "MimicStance";

    private int stanceActionCooldown = 30;

    public SynapticMimicEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MIMIC_STANCE, MimicStance.BLADE_DANCER.id());
    }

    public MimicStance getMimicStance() {
        return MimicStance.fromId(this.entityData.get(DATA_MIMIC_STANCE));
    }

    public void setMimicStance(MimicStance stance) {
        this.entityData.set(DATA_MIMIC_STANCE, stance.id());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new KitingRetreatGoal(this, 1.15D) {
            @Override
            public boolean canUse() {
                return getMimicStance() == MimicStance.SPORE_MARKSMAN && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public MimicStance evaluateTargetStance(LivingEntity target) {
        ItemStack main = target.getMainHandItem();
        ItemStack off = target.getOffhandItem();

        if (main.getItem() instanceof ProjectileWeaponItem
                || main.getItem() instanceof NeutralizingSporeLauncherItem
                || off.getItem() instanceof ProjectileWeaponItem) {
            return MimicStance.SPORE_MARKSMAN;
        }
        if (main.getItem() instanceof ShieldItem
                || off.getItem() instanceof ShieldItem
                || target.getArmorValue() >= 15) {
            return MimicStance.AEGIS_MIRROR;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            StimulusType dominant = HiveSquadComposer.findDominantStimulus(HiveMindSavedData.get(serverLevel));
            if (dominant == StimulusType.PROJECTILE) {
                return MimicStance.SPORE_MARKSMAN;
            }
            if (dominant == StimulusType.EXPLOSION || dominant == StimulusType.MAGIC) {
                return MimicStance.AEGIS_MIRROR;
            }
        }
        return MimicStance.BLADE_DANCER;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.tickCount % 10 == 0) {
                var particle = switch (getMimicStance()) {
                    case BLADE_DANCER -> ModParticles.NECROTIC_MIST.get();
                    case SPORE_MARKSMAN -> ModParticles.SPORE_FALL.get();
                    case AEGIS_MIRROR -> ModParticles.HIVE_PULSE.get();
                };
                this.level().addParticle(particle,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.6,
                        this.getY() + 1.1,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.6,
                        0.0, 0.03, 0.0);
            }
            return;
        }

        if (stanceActionCooldown > 0) {
            stanceActionCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.tickCount % 40 == 0) {
            MimicStance next = evaluateTargetStance(target);
            if (next != getMimicStance()) {
                setMimicStance(next);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(),
                            this.getX(), this.getY(0.5), this.getZ(),
                            10, 0.3, 0.4, 0.3, 0.04);
                }
            }
        }

        MimicStance stance = getMimicStance();
        if (stance == MimicStance.AEGIS_MIRROR && this.tickCount % 40 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, false, false));
        }

        if (stanceActionCooldown > 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double distSq = this.distanceToSqr(target);
        if (stance == MimicStance.BLADE_DANCER && distSq <= 20.25D) {
            performBladeArcSlash(serverLevel, target);
            stanceActionCooldown = 60;
        } else if (stance == MimicStance.SPORE_MARKSMAN && distSq >= 12.25D && distSq <= 324.0D && this.hasLineOfSight(target)) {
            performSporeVolley(serverLevel, target);
            stanceActionCooldown = 50;
        }
    }

    private void performBladeArcSlash(ServerLevel level, LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.push(dir.x * 0.45, 0.12, dir.z * 0.45);
        AABB slashBox = this.getBoundingBox().inflate(2.4D, 0.8D, 2.4D);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, slashBox, e -> e != this && !(e instanceof MycelialMob))) {
            hit.hurt(this.damageSources().mobAttack(this), 5.5F);
        }
        level.sendParticles(ModParticles.PSYCHIC_WAVE.get(), this.getX() + dir.x * 1.1, this.getY(0.5), this.getZ() + dir.z * 1.1,
                2, 0.2, 0.1, 0.2, 0.0);
        level.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_MIMIC_STRIKE.get(), SoundSource.HOSTILE, 0.9F, 1.1F);
    }

    private void performSporeVolley(ServerLevel level, LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5) - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        float[] yawOffsets = {-10.0F, 0.0F, 10.0F};
        for (float yawDeg : yawOffsets) {
            double rad = Math.toRadians(yawDeg);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double rx = dx * cos - dz * sin;
            double rz = dx * sin + dz * cos;
            AcidSpitProjectile spit = new AcidSpitProjectile(level, this);
            spit.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
            spit.shoot(rx, dy, rz, 1.25F, 2.5F);
            level.addFreshEntity(spit);
        }
        level.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_MIMIC_STRIKE.get(), SoundSource.HOSTILE, 0.85F, 0.85F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide()
                && getMimicStance() == MimicStance.AEGIS_MIRROR
                && source.getEntity() instanceof LivingEntity attacker
                && attacker != this
                && !(attacker instanceof MycelialMob)) {
            float reflected = Math.max(1.0F, amount * 0.35F);
            attacker.hurt(this.damageSources().thorns(this), reflected);
            this.level().playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_MIMIC_STRIKE.get(), SoundSource.HOSTILE, 0.75F, 1.25F);
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_STANCE, getMimicStance().id());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_STANCE)) {
            setMimicStance(MimicStance.fromId(tag.getInt(TAG_STANCE)));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SYNAPTIC_MIMIC_ECHO.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SYNAPTIC_MIMIC_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SYNAPTIC_MIMIC_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 65.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }
}
