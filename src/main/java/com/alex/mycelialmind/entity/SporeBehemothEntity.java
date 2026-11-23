package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCarapaceProfile;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Roaming Fungal Tide Mini-Boss (200 HP, 2 Phases).
 * Phase 1 (100-50%): Telegraphed Seismic Charge, Seismic Slam, 3-way Spore Rain, Mycelial Shield.
 * Phase 2 (<50%): Enraged Siege — 5-way Spore Rain, faster tempo, and Seismic Charge -> Seismic Slam combo.
 * Shattering its frontal carapace staggers the Behemoth and lowers its dorsal sac for melee punishment.
 */
public class SporeBehemothEntity extends MycelialBossMob {
    private static final EntityDataAccessor<Integer> DATA_BEHEMOTH_PHASE =
            SynchedEntityData.defineId(SporeBehemothEntity.class, EntityDataSerializers.INT);
    private static final String TAG_PHASE = "BehemothPhase";

    private static final List<BossAttackId> ATTACK_KIT = List.of(
            BossAttackId.BEHEMOTH_SEISMIC_CHARGE,
            BossAttackId.BEHEMOTH_SEISMIC_SLAM,
            BossAttackId.BEHEMOTH_SPORE_RAIN,
            BossAttackId.BEHEMOTH_MYCELIAL_SHIELD
    );

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_6
    );

    private final MycelialBossPart weakPointPart;
    private final MycelialBossPart carapacePart;
    private final MycelialBossPart[] bossParts;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BEHEMOTH_PHASE, 1);
    }

    @Override
    protected BossCarapaceProfile getCarapaceProfile() {
        return BossCarapaceProfile.SPORE_BEHEMOTH;
    }

    @Override
    protected List<BossAttackId> getBossAttackKit() {
        return ATTACK_KIT;
    }

    public int getBehemothPhase() {
        return this.entityData.get(DATA_BEHEMOTH_PHASE);
    }

    public void setBehemothPhase(int phase) {
        this.entityData.set(DATA_BEHEMOTH_PHASE, Math.max(1, Math.min(2, phase)));
    }

    @Override
    public int getActiveBossPhase() {
        return getBehemothPhase();
    }

    @Override
    public int getMaxBossPhases() {
        return 2;
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
        double weakY = 2.85D + getWeakPointLowerOffset();
        double weakFwd = isCarapaceBroken() ? 0.15D : -0.35D;
        positionBossPart(this, this.weakPointPart, weakFwd, weakY, 0.0D);
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
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / Math.max(1.0F, this.getMaxHealth()));
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float hpRatio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        if (getBehemothPhase() == 1 && BossCombatArchitectureEngine.resolveBehemothPhase(hpRatio) == 2) {
            enterPhaseTwo(serverLevel);
        }

        PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        long gameTime = serverLevel.getGameTime();
        boolean inPylon = tracker.isProtected(this.blockPosition());
        boolean suppressed = hive.isSuppressed(gameTime);

        // Guaranteed low-HP shield burst when entering Phase 2 outside Pylons
        if (!lowHpShieldTriggered && hpRatio <= 0.50F && !inPylon && !suppressed) {
            lowHpShieldTriggered = true;
            activateMycelialShield(serverLevel);
        }

        // Extra regeneration while shielded on mycelium or during Fungal Tide (outside Pylons)
        if (!inPylon && !suppressed && this.tickCount % 20 == 0 && this.hasEffect(MobEffects.ABSORPTION)) {
            boolean onMycelium = serverLevel.getBlockState(this.blockPosition().below()).is(ModBlocks.CREEPING_MYCELIUM.get());
            if (onMycelium || hive.isFungalTideActive(gameTime)) {
                this.heal(1.5F);
            }
        }

        tickBossCombatStateMachine(serverLevel, this.getTarget(), getBehemothPhase());
    }

    private void enterPhaseTwo(ServerLevel level) {
        setBehemothPhase(2);
        onBossPhaseTransition(level);
        updateBossBarAppearance();
        level.playSound(null, this.blockPosition(), ModSounds.BOSS_PHASE_SHIFT.get(), SoundSource.HOSTILE, 1.55F, 0.88F);
        level.playSound(null, this.blockPosition(), ModSounds.SPORE_BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 1.45F, 0.78F);
        level.sendParticles(ModParticles.TIDE_MOTE.get(), this.getX(), this.getY(0.5), this.getZ(), 24, 1.2, 1.0, 1.2, 0.08);
    }

    private void updateBossBarAppearance() {
        if (isCarapaceBroken()) {
            this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.spore_behemoth.carapace_broken"));
        } else if (getBehemothPhase() >= 2) {
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.spore_behemoth.phase2"));
        } else {
            this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    protected void onCarapaceStateChanged(ServerLevel level, boolean broken) {
        updateBossBarAppearance();
    }

    @Override
    protected boolean isShieldAttackAllowed(ServerLevel level) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        return !tracker.isProtected(this.blockPosition()) && !hive.isSuppressed(level.getGameTime());
    }

    @Override
    protected void onAttackWindupStart(ServerLevel level, BossAttackId attack, LivingEntity target) {
        switch (attack) {
            case BEHEMOTH_SEISMIC_CHARGE -> level.playSound(null, this.blockPosition(),
                    ModSounds.SPORE_BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 1.35F, 0.78F);
            case BEHEMOTH_SEISMIC_SLAM -> level.playSound(null, this.blockPosition(),
                    ModSounds.SPORE_BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 1.20F, 0.92F);
            case BEHEMOTH_SPORE_RAIN -> level.playSound(null, this.blockPosition(),
                    ModSounds.SPORE_BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 1.05F, 1.18F);
            case BEHEMOTH_MYCELIAL_SHIELD -> level.playSound(null, this.blockPosition(),
                    ModSounds.HIVE_CORE_SHIELD.get(), SoundSource.HOSTILE, 1.10F, 0.90F);
            default -> {
            }
        }
    }

    @Override
    protected void onAttackWindupTick(ServerLevel level, BossAttackId attack, int ticksRemaining, int totalTicks, @Nullable LivingEntity target) {
        if (ticksRemaining % 3 != 0) {
            return;
        }
        Vec3 anchor = target != null && target.isAlive() ? target.position() : this.telegraphAnchor;
        switch (attack) {
            case BEHEMOTH_SEISMIC_CHARGE -> {
                if (anchor != null) {
                    this.telegraphAnchor = anchor;
                    spawnGroundLineTelegraph(level, ModParticles.CHITIN_SHARD.get(), this.position(), anchor, 8);
                    spawnGroundRingTelegraph(level, ModParticles.TIDE_MOTE.get(), anchor, 2.2D, 10);
                }
            }
            case BEHEMOTH_SEISMIC_SLAM -> {
                double radius = 2.0D + 3.2D * getStateProgress();
                spawnGroundRingTelegraph(level, ModParticles.CHITIN_SHARD.get(), this.position(), radius, 14);
            }
            case BEHEMOTH_SPORE_RAIN -> {
                level.sendParticles(ModParticles.ACID_DROPLET.get(), this.getX(), this.getY() + 2.4D, this.getZ(),
                        8, 0.9, 0.4, 0.9, 0.04);
            }
            case BEHEMOTH_MYCELIAL_SHIELD -> {
                spawnGroundRingTelegraph(level, ModParticles.TIDE_MOTE.get(), this.position(), 2.4D, 12);
            }
            default -> {
            }
        }
    }

    @Override
    protected void onAttackExecute(ServerLevel level, BossAttackId attack, @Nullable LivingEntity target) {
        switch (attack) {
            case BEHEMOTH_SEISMIC_CHARGE -> executeSeismicCharge(level, target);
            case BEHEMOTH_SEISMIC_SLAM -> executeSeismicSlam(level);
            case BEHEMOTH_SPORE_RAIN -> executeSporeRain(level, target);
            case BEHEMOTH_MYCELIAL_SHIELD -> activateMycelialShield(level);
            default -> {
            }
        }
    }

    private void executeSeismicCharge(ServerLevel serverLevel, @Nullable LivingEntity target) {
        Vec3 dest = target != null && target.isAlive() ? target.position() : this.telegraphAnchor;
        if (dest != null) {
            Vec3 delta = dest.subtract(this.position());
            Vec3 dash = delta.lengthSqr() > 0.01D ? delta.normalize().scale(1.22D) : this.getLookAngle().scale(1.15D);
            this.setDeltaMovement(dash.x, 0.34D, dash.z);
            this.hasImpulse = true;
        }
        serverLevel.sendParticles(ModParticles.CHITIN_SHARD.get(), this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.9, 0.3, 0.9, 0.05);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.SPORE_BEHEMOTH_SLAM.get(), SoundSource.HOSTILE, 1.45F, 0.95F);
        float dmg = getBehemothPhase() >= 2 ? 8.5F : 6.5F;
        for (LivingEntity hit : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.2D))) {
            if (hit != this && !(hit instanceof MycelialMob) && hit.isAlive()) {
                hit.hurt(this.damageSources().mobAttack(this), dmg);
                hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
        }
    }

    private void executeSeismicSlam(ServerLevel serverLevel) {
        spawnGroundRingTelegraph(serverLevel, ModParticles.CHITIN_SHARD.get(), this.position(), 5.0D, 20);
        serverLevel.sendParticles(ModParticles.IMPACT.get(), this.getX(), this.getY() + 0.35, this.getZ(), 4, 1.2, 0.2, 1.2, 0.08);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.SPORE_BEHEMOTH_SLAM.get(), SoundSource.HOSTILE, 1.65F, 0.75F);
        float dmg = getBehemothPhase() >= 2 ? 10.5F : 8.0F;
        for (LivingEntity hit : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.2D))) {
            if (hit != this && !(hit instanceof MycelialMob) && hit.isAlive()) {
                hit.hurt(this.damageSources().mobAttack(this), dmg);
                Vec3 knock = hit.position().subtract(this.position());
                if (knock.lengthSqr() > 0.01D) {
                    Vec3 dir = knock.normalize().scale(0.65D);
                    hit.push(dir.x, 0.35D, dir.z);
                }
                hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
            }
        }
    }

    private void executeSporeRain(ServerLevel serverLevel, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 baseDir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
        int halfSpread = getBehemothPhase() >= 2 ? 2 : 1;
        for (int i = -halfSpread; i <= halfSpread; i++) {
            double yaw = i * 0.16D;
            double rx = baseDir.x * Math.cos(yaw) - baseDir.z * Math.sin(yaw);
            double rz = baseDir.x * Math.sin(yaw) + baseDir.z * Math.cos(yaw);
            AcidSpitProjectile spit = new AcidSpitProjectile(serverLevel, this);
            spit.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
            spit.shoot(rx, baseDir.y + 0.08, rz, 1.38F, 2.0F);
            serverLevel.addFreshEntity(spit);
        }
        serverLevel.playSound(null, this.blockPosition(), ModSounds.ACID_SPITTER_SPIT.get(), SoundSource.HOSTILE, 1.35F, 0.62F);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.SPORE_BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 0.85F, 1.12F);
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_PHASE, getBehemothPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_PHASE)) {
            setBehemothPhase(tag.getInt(TAG_PHASE));
        }
        updateBossBarAppearance();
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
            this.spawnAtLocation(com.alex.mycelialmind.item.FieldEngramItem.createStack(
                    com.alex.mycelialmind.hive.FieldEngramId.TIDE_PRESSURE));
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
