package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Roaming Fungal Tide Mini-Boss (180 HP) with Seismic Charge, Spore Rain,
 * and Mycelial Shield (damage absorption + regeneration suppressed by Pylons/Suppressor).
 */
public class SporeBehemothEntity extends MycelialMob {
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_6
    );

    private final MycelialBossPart weakPointPart;
    private final MycelialBossPart carapacePart;
    private final MycelialBossPart[] bossParts;

    private int seismicCooldown = 80;
    private int sporeRainCooldown = 120;
    private int shieldCooldown = 140;
    private boolean lowHpShieldTriggered = false;

    public SporeBehemothEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 45;
        this.weakPointPart = new MycelialBossPart(this, "dorsal_sac", MycelialBossPart.PartRole.WEAK_POINT, 1.3F, 1.1F);
        this.carapacePart = new MycelialBossPart(this, "frontal_carapace", MycelialBossPart.PartRole.ARMORED_CARAPACE, 1.6F, 1.7F);
        this.bossParts = new MycelialBossPart[]{this.weakPointPart, this.carapacePart};
        this.setId(ENTITY_COUNTER.getAndAdd(this.bossParts.length + 1) + 1);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.bossParts;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < this.bossParts.length; i++) {
            this.bossParts[i].setId(id + i + 1);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        positionBossPart(this, this.weakPointPart, -0.35D, 2.85D, 0.0D);
        positionBossPart(this, this.carapacePart, 0.75D, 0.25D, 0.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / Math.max(1.0F, this.getMaxHealth()));
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (seismicCooldown > 0) seismicCooldown--;
        if (sporeRainCooldown > 0) sporeRainCooldown--;
        if (shieldCooldown > 0) shieldCooldown--;

        PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        long gameTime = serverLevel.getGameTime();
        boolean inPylon = tracker.isProtected(this.blockPosition());
        boolean suppressed = hive.isSuppressed(gameTime);

        // Mycelial Shield (Absorption + Regeneration)
        boolean triggerLowHp = !lowHpShieldTriggered && this.getHealth() <= this.getMaxHealth() * 0.5F;
        if (!inPylon && !suppressed && (shieldCooldown <= 0 || triggerLowHp)) {
            if (triggerLowHp) {
                lowHpShieldTriggered = true;
            }
            activateMycelialShield(serverLevel);
            shieldCooldown = 160;
        }

        // Extra regeneration while shielded on mycelium or during Fungal Tide (outside Pylons)
        if (!inPylon && !suppressed && this.tickCount % 20 == 0 && this.hasEffect(MobEffects.ABSORPTION)) {
            boolean onMycelium = serverLevel.getBlockState(this.blockPosition().below()).is(ModBlocks.CREEPING_MYCELIUM.get());
            if (onMycelium || hive.isFungalTideActive(gameTime)) {
                this.heal(1.5F);
            }
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distSq = this.distanceToSqr(target);

        // Seismic Charge
        if (seismicCooldown <= 0 && distSq >= 9.0D && distSq <= 196.0D) {
            Vec3 dash = target.position().subtract(this.position()).normalize().scale(1.15D);
            this.setDeltaMovement(dash.x, 0.32D, dash.z);
            this.hasImpulse = true;
            serverLevel.sendParticles(ModParticles.CHITIN_SHARD.get(), this.getX(), this.getY() + 0.5, this.getZ(), 4, 0.8, 0.3, 0.8, 0.02);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.SPORE_BEHEMOTH_SLAM.get(), SoundSource.HOSTILE, 1.45F, 0.95F);
            for (LivingEntity hit : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.2D))) {
                if (hit != this && !(hit instanceof MycelialMob)) {
                    hit.hurt(this.damageSources().mobAttack(this), 6.0F);
                    hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
            }
            seismicCooldown = 100;
        }

        // Spore Rain (3-way Acid Spit fan)
        if (sporeRainCooldown <= 0 && distSq <= 324.0D) {
            Vec3 baseDir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
            for (int i = -1; i <= 1; i++) {
                double yaw = i * 0.18;
                double rx = baseDir.x * Math.cos(yaw) - baseDir.z * Math.sin(yaw);
                double rz = baseDir.x * Math.sin(yaw) + baseDir.z * Math.cos(yaw);
                AcidSpitProjectile spit = new AcidSpitProjectile(serverLevel, this);
                spit.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
                spit.shoot(rx, baseDir.y + 0.08, rz, 1.35F, 2.0F);
                serverLevel.addFreshEntity(spit);
            }
            serverLevel.playSound(null, this.blockPosition(), ModSounds.ACID_SPITTER_SPIT.get(), SoundSource.HOSTILE, 1.35F, 0.62F);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.SPORE_BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 0.85F, 1.12F);
            sporeRainCooldown = 140;
        }
    }

    public void activateMycelialShield(ServerLevel level) {
        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 2, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, true));
        level.sendParticles(ModParticles.HIVE_PULSE.get(), this.getX(), this.getY(0.5), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ModParticles.TIDE_MOTE.get(), this.getX(), this.getY(0.5), this.getZ(), 16, 0.7, 0.8, 0.7, 0.04);
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_SHIELD.get(), SoundSource.HOSTILE, 1.15F, 0.72F);
        level.playSound(null, this.blockPosition(), ModSounds.SPORE_BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 1.10F, 1.05F);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (this.level() instanceof ServerLevel serverLevel) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            long gameTime = serverLevel.getGameTime();
            if (hive.isFungalTideActive(gameTime)) {
                hive.endFungalTide(gameTime);
                hive.reduceThreat(12.0);
                for (ServerPlayer player : serverLevel.players()) {
                    PacketDistributor.sendToPlayer(player, HiveSyncPayload.forPlayer(serverLevel, player));
                    player.sendSystemMessage(Component.translatable("message.mycelialmind.fungal_tide_cleared")
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (recentlyHit) {
            this.spawnAtLocation(ModItems.TIDE_BEACON.get());
            this.spawnAtLocation(ModItems.SYNAPTIC_CORE.get());
        }
    }

    @Override
    protected float getBaseSoundVolume() {
        return 1.45F;
    }

    @Override
    protected float getStepSoundVolume() {
        return 0.68F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 115;
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_COLOSSAL.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SPORE_BEHEMOTH_ROAR.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SPORE_BEHEMOTH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPORE_BEHEMOTH_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }
}
