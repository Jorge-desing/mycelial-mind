package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.hive.BossCombatArchitectureEngine;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCarapaceProfile;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState;
import com.alex.mycelialmind.hive.HiveCycleEngine;
import com.alex.mycelialmind.hive.HiveCycleSavedData;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared base class for all Mycelial Mind bosses (Spore Behemoth, Hive Core, Apex Matriarch).
 *
 * <p>Implements the unified boss combat architecture:
 * <ul>
 *     <li>Exclusive state machine: {@code IDLE -> WINDUP -> ACTIVE -> RECOVERY} (+ {@code STAGGERED})</li>
 *     <li>Utility-based adaptive attack selection driven by Hive Mind telemetry and anti-spam cooldowns</li>
 *     <li>Advanced-phase single-step combo chaining</li>
 *     <li>Physical {@code ARMORED_CARAPACE} integrity pool: shattering it triggers an initial stagger,
 *         lowers the {@code WEAK_POINT} for melee punishment ({@code 1.55x}), disables carapace mitigation ({@code 1.0x}),
 *         and regenerates after the broken window expires or on phase transition</li>
 *     <li>Synced {@link EntityDataAccessor} state for client 3D animations, {@code SYNAPTIC_VISION},
 *         and real-time HUD boss telemetry</li>
 * </ul>
 */
public abstract class MycelialBossMob extends MycelialMob {
    private static final EntityDataAccessor<Integer> DATA_COMBAT_STATE =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTIVE_ATTACK =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE_TICKS =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE_TOTAL_TICKS =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_CARAPACE_INTEGRITY =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_CARAPACE_INTEGRITY =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_CARAPACE_BROKEN_TICKS =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_COMBO_ACTIVE =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TELEGRAPH_LOCKED =
            SynchedEntityData.defineId(MycelialBossMob.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_CARAPACE_INTEGRITY = "CarapaceIntegrity";
    private static final String TAG_MAX_CARAPACE_INTEGRITY = "MaxCarapaceIntegrity";
    private static final String TAG_CARAPACE_BROKEN_TICKS = "CarapaceBrokenTicks";

    protected final Map<BossAttackId, Integer> attackCooldowns = new EnumMap<>(BossAttackId.class);
    protected BossAttackId lastCompletedAttack = BossAttackId.NONE;
    protected boolean comboChainedThisSequence = false;
    protected int globalIdleDelayTicks = 20;
    private boolean carapaceScaledInitialized = false;
    @Nullable
    protected Vec3 telegraphAnchor = null;

    protected MycelialBossMob(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        float baseMax = getCarapaceProfile().maxIntegrity();
        this.entityData.set(DATA_MAX_CARAPACE_INTEGRITY, baseMax);
        this.entityData.set(DATA_CARAPACE_INTEGRITY, baseMax);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COMBAT_STATE, BossCombatState.IDLE.ordinal());
        builder.define(DATA_ACTIVE_ATTACK, BossAttackId.NONE.ordinal());
        builder.define(DATA_STATE_TICKS, 0);
        builder.define(DATA_STATE_TOTAL_TICKS, 1);
        builder.define(DATA_CARAPACE_INTEGRITY, 80.0F);
        builder.define(DATA_MAX_CARAPACE_INTEGRITY, 80.0F);
        builder.define(DATA_CARAPACE_BROKEN_TICKS, 0);
        builder.define(DATA_COMBO_ACTIVE, false);
        builder.define(DATA_TELEGRAPH_LOCKED, false);
    }

    /**
     * Melee attack goal gated by the boss combat state machine so vanilla melee never
     * competes with WINDUP, ACTIVE, RECOVERY, or STAGGERED states.
     */
    protected static class BossStateGatedMeleeGoal extends MeleeAttackGoal {
        private final MycelialBossMob boss;

        public BossStateGatedMeleeGoal(MycelialBossMob boss, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(boss, speedModifier, followingTargetEvenIfNotSeen);
            this.boss = boss;
        }

        @Override
        public boolean canUse() {
            return BossCombatArchitectureEngine.canExecuteMeleeGoal(this.boss.getCombatState()) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return BossCombatArchitectureEngine.canExecuteMeleeGoal(this.boss.getCombatState()) && super.canContinueToUse();
        }
    }

    protected abstract BossCarapaceProfile getCarapaceProfile();

    protected abstract List<BossAttackId> getBossAttackKit();

    public abstract int getActiveBossPhase();

    public abstract int getMaxBossPhases();

    protected abstract void onAttackWindupStart(ServerLevel level, BossAttackId attack, LivingEntity target);

    protected abstract void onAttackWindupTick(ServerLevel level, BossAttackId attack, int ticksRemaining, int totalTicks, @Nullable LivingEntity target);

    protected abstract void onAttackExecute(ServerLevel level, BossAttackId attack, @Nullable LivingEntity target);

    protected void onAttackActiveTick(ServerLevel level, BossAttackId attack, int ticksRemaining, int totalTicks, @Nullable LivingEntity target) {
    }

    protected boolean isShieldAttackAllowed(ServerLevel level) {
        return true;
    }

    protected boolean isBossInvulnerableToCarapaceDamage(ServerLevel level) {
        return false;
    }

    /**
     * Returns the HP ratio threshold of the next phase (e.g. {@code 0.50F}),
     * or {@code -1.0F} when already in the final phase. Used to prevent single hits from skipping a phase.
     */
    protected float getNextPhaseThresholdRatio() {
        return -1.0F;
    }

    protected void onCarapaceStateChanged(ServerLevel level, boolean broken) {
    }

    // --- Synced state getters ---

    public BossCombatState getCombatState() {
        return BossCombatState.fromOrdinal(this.entityData.get(DATA_COMBAT_STATE));
    }

    protected void setCombatState(BossCombatState state, int durationTicks) {
        int safeDuration = Math.max(0, durationTicks);
        this.entityData.set(DATA_COMBAT_STATE, state.ordinal());
        this.entityData.set(DATA_STATE_TICKS, safeDuration);
        this.entityData.set(DATA_STATE_TOTAL_TICKS, Math.max(1, safeDuration));
    }

    public BossAttackId getActiveAttack() {
        return BossAttackId.fromOrdinal(this.entityData.get(DATA_ACTIVE_ATTACK));
    }

    protected void setActiveAttack(BossAttackId attack) {
        this.entityData.set(DATA_ACTIVE_ATTACK, (attack == null ? BossAttackId.NONE : attack).ordinal());
    }

    public int getStateTicksRemaining() {
        return this.entityData.get(DATA_STATE_TICKS);
    }

    public int getStateTotalTicks() {
        return Math.max(1, this.entityData.get(DATA_STATE_TOTAL_TICKS));
    }

    /**
     * Normalized progress {@code 0.0F..1.0F} through the current state (0 at start, 1 at completion).
     */
    public float getStateProgress() {
        int total = getStateTotalTicks();
        int remaining = Math.max(0, Math.min(total, getStateTicksRemaining()));
        return 1.0F - ((float) remaining / (float) total);
    }

    public float getCarapaceIntegrity() {
        return this.entityData.get(DATA_CARAPACE_INTEGRITY);
    }

    public float getMaxCarapaceIntegrity() {
        return Math.max(1.0F, this.entityData.get(DATA_MAX_CARAPACE_INTEGRITY));
    }

    public float getCarapaceIntegrityRatio() {
        float max = getMaxCarapaceIntegrity();
        return Math.max(0.0F, Math.min(1.0F, getCarapaceIntegrity() / max));
    }

    public boolean isCarapaceBroken() {
        return this.entityData.get(DATA_CARAPACE_BROKEN_TICKS) > 0;
    }

    public int getCarapaceBrokenTicksRemaining() {
        return this.entityData.get(DATA_CARAPACE_BROKEN_TICKS);
    }

    public boolean isComboActive() {
        return this.entityData.get(DATA_COMBO_ACTIVE);
    }

    public boolean isTelegraphLocked() {
        return this.entityData.get(DATA_TELEGRAPH_LOCKED);
    }

    public double getWeakPointLowerOffset() {
        return isCarapaceBroken() ? getCarapaceProfile().weakPointLowerOffset() : 0.0D;
    }

    public boolean isBossStaggered() {
        return getCombatState() == BossCombatState.STAGGERED;
    }

    // --- State machine tick ---

    protected void tickBossCombatStateMachine(ServerLevel level, @Nullable LivingEntity target, int currentPhase) {
        if (!carapaceScaledInitialized) {
            carapaceScaledInitialized = true;
            refreshScaledCarapaceMax(level, true);
        }

        // 1. Decrement attack cooldowns
        for (Map.Entry<BossAttackId, Integer> entry : attackCooldowns.entrySet()) {
            int val = entry.getValue();
            if (val > 0) {
                entry.setValue(val - 1);
            }
        }
        if (globalIdleDelayTicks > 0) {
            globalIdleDelayTicks--;
        }

        // 2. Carapace regeneration timer
        int brokenTicks = this.entityData.get(DATA_CARAPACE_BROKEN_TICKS);
        if (brokenTicks > 0) {
            brokenTicks--;
            this.entityData.set(DATA_CARAPACE_BROKEN_TICKS, brokenTicks);
            if (brokenTicks == 0) {
                restoreCarapace(level, true);
            }
        }

        // 3. Exclusive combat state machine
        BossCombatState state = getCombatState();
        int ticksLeft = this.entityData.get(DATA_STATE_TICKS);
        BossAttackId activeAttack = getActiveAttack();

        switch (state) {
            case STAGGERED -> {
                this.getNavigation().stop();
                this.setDeltaMovement(0.0D, Math.min(this.getDeltaMovement().y, 0.0D), 0.0D);
                if (this.tickCount % 6 == 0) {
                    level.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                            this.getX(), this.getY() + this.getBbHeight() * 0.7D, this.getZ(),
                            4, 0.55, 0.35, 0.55, 0.04);
                }
                if (ticksLeft > 1) {
                    this.entityData.set(DATA_STATE_TICKS, ticksLeft - 1);
                } else {
                    setActiveAttack(BossAttackId.NONE);
                    setCombatState(BossCombatState.IDLE, 0);
                    globalIdleDelayTicks = 12;
                }
            }
            case WINDUP -> {
                if (activeAttack == BossAttackId.NONE) {
                    this.entityData.set(DATA_TELEGRAPH_LOCKED, false);
                    this.entityData.set(DATA_COMBO_ACTIVE, false);
                    setCombatState(BossCombatState.IDLE, 0);
                    return;
                }
                this.getNavigation().stop();
                boolean lockedNow = BossCombatArchitectureEngine.isTelegraphLocked(ticksLeft);
                boolean wasLocked = isTelegraphLocked();
                if (lockedNow && !wasLocked) {
                    this.entityData.set(DATA_TELEGRAPH_LOCKED, true);
                    Vec3 flashPos = this.telegraphAnchor != null ? this.telegraphAnchor : this.position();
                    level.sendParticles(ModParticles.THERMAL_RETICLE.get(),
                            flashPos.x, flashPos.y + 0.6D, flashPos.z, 4, 0.35, 0.2, 0.35, 0.02);
                    level.sendParticles(ModParticles.IMPACT.get(),
                            this.getX(), this.getY(0.65), this.getZ(), 1, 0.1, 0.1, 0.1, 0.05);
                    level.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_PULSE.get(),
                            SoundSource.HOSTILE, 1.05F, 1.55F);
                }
                if (!lockedNow && target != null && target.isAlive()) {
                    this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                onAttackWindupTick(level, activeAttack, ticksLeft, getStateTotalTicks(), target);
                if (ticksLeft > 1) {
                    this.entityData.set(DATA_STATE_TICKS, ticksLeft - 1);
                } else {
                    // Transition WINDUP -> ACTIVE
                    this.entityData.set(DATA_TELEGRAPH_LOCKED, false);
                    setCombatState(BossCombatState.ACTIVE, Math.max(1, activeAttack.activeTicks()));
                    onAttackExecute(level, activeAttack, target);
                }
            }
            case ACTIVE -> {
                if (activeAttack == BossAttackId.NONE) {
                    setCombatState(BossCombatState.IDLE, 0);
                    return;
                }
                onAttackActiveTick(level, activeAttack, ticksLeft, getStateTotalTicks(), target);
                if (ticksLeft > 1) {
                    this.entityData.set(DATA_STATE_TICKS, ticksLeft - 1);
                } else {
                    // Put completed attack on cooldown
                    int baseCd = BossCombatArchitectureEngine.resolveEffectiveCooldownTicks(activeAttack, currentPhase);
                    double cycleCdScale = com.alex.mycelialmind.hive.HiveCycleDirector.getActiveBossCooldownScale(level);
                    int cd = Math.max(25, (int) Math.round(baseCd * cycleCdScale));
                    attackCooldowns.put(activeAttack, cd);
                    lastCompletedAttack = activeAttack;

                    // Check for single-step advanced phase combo
                    if (target != null && target.isAlive()) {
                        double dist = this.distanceTo(target);
                        BossAttackId followUp = BossCombatArchitectureEngine.resolveComboFollowUp(
                                activeAttack, currentPhase, dist, comboChainedThisSequence);
                        if (followUp != BossAttackId.NONE) {
                            comboChainedThisSequence = true;
                            startAttackWindup(level, followUp, target, currentPhase, true);
                            return;
                        }
                    }

                    // Transition ACTIVE -> RECOVERY
                    comboChainedThisSequence = false;
                    this.entityData.set(DATA_COMBO_ACTIVE, false);
                    setCombatState(BossCombatState.RECOVERY, Math.max(1, activeAttack.recoveryTicks()));
                }
            }
            case RECOVERY -> {
                this.getNavigation().stop();
                if (ticksLeft > 1) {
                    this.entityData.set(DATA_STATE_TICKS, ticksLeft - 1);
                } else {
                    setActiveAttack(BossAttackId.NONE);
                    this.entityData.set(DATA_COMBO_ACTIVE, false);
                    this.entityData.set(DATA_TELEGRAPH_LOCKED, false);
                    setCombatState(BossCombatState.IDLE, 0);
                    globalIdleDelayTicks = Math.max(10, 18 - currentPhase * 3);
                }
            }
            case IDLE -> {
                if (target == null || !target.isAlive() || globalIdleDelayTicks > 0) {
                    return;
                }
                HiveMindSavedData hive = HiveMindSavedData.get(level);
                StimulusType dominant = resolveDominantStimulus(hive);
                Set<PlayerHabit> habits = resolveActiveHabits(hive);
                double distance = this.distanceTo(target);
                double targetHeightDelta = target.getY() - this.getY();
                BossAttackId chosen = BossCombatArchitectureEngine.selectNextAttack(
                        getBossAttackKit(),
                        currentPhase,
                        distance,
                        attackCooldowns,
                        lastCompletedAttack,
                        dominant,
                        habits,
                        isShieldAttackAllowed(level),
                        targetHeightDelta
                );
                if (chosen != BossAttackId.NONE) {
                    comboChainedThisSequence = false;
                    startAttackWindup(level, chosen, target, currentPhase, false);
                }
            }
        }
    }

    private static @Nullable StimulusType resolveDominantStimulus(HiveMindSavedData hive) {
        StimulusType best = null;
        double bestScore = 0.0D;
        for (StimulusType type : StimulusType.values()) {
            double s = hive.getStimulusScore(type);
            if (s > bestScore) {
                bestScore = s;
                best = type;
            }
        }
        return best;
    }

    private static Set<PlayerHabit> resolveActiveHabits(HiveMindSavedData hive) {
        java.util.EnumSet<PlayerHabit> active = java.util.EnumSet.noneOf(PlayerHabit.class);
        for (PlayerHabit habit : PlayerHabit.values()) {
            if (hive.hasHabitAdaptation(habit)) {
                active.add(habit);
            }
        }
        return active;
    }

    public void startAttackWindup(
            ServerLevel level,
            BossAttackId attack,
            LivingEntity target,
            int currentPhase,
            boolean isComboFollowUp
    ) {
        if (attack == null || attack == BossAttackId.NONE) {
            return;
        }
        int windup = BossCombatArchitectureEngine.resolveEffectiveWindupTicks(attack, currentPhase, isComboFollowUp);
        this.telegraphAnchor = target.position();
        this.entityData.set(DATA_COMBO_ACTIVE, isComboFollowUp);
        this.entityData.set(DATA_TELEGRAPH_LOCKED, BossCombatArchitectureEngine.isTelegraphLocked(windup));
        setActiveAttack(attack);
        setCombatState(BossCombatState.WINDUP, Math.max(1, windup));
        onAttackWindupStart(level, attack, target);
    }

    public void enterStaggeredState(ServerLevel level, int durationTicks) {
        int ticks = Math.max(10, durationTicks);
        this.comboChainedThisSequence = false;
        this.telegraphAnchor = null;
        this.entityData.set(DATA_COMBO_ACTIVE, false);
        this.entityData.set(DATA_TELEGRAPH_LOCKED, false);
        setActiveAttack(BossAttackId.NONE);
        setCombatState(BossCombatState.STAGGERED, ticks);
        this.getNavigation().stop();
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, true));
    }

    protected void onBossPhaseTransition(ServerLevel level) {
        this.comboChainedThisSequence = false;
        this.telegraphAnchor = null;
        this.entityData.set(DATA_COMBO_ACTIVE, false);
        this.entityData.set(DATA_TELEGRAPH_LOCKED, false);
        if (getCombatState() != BossCombatState.STAGGERED) {
            setActiveAttack(BossAttackId.NONE);
            setCombatState(BossCombatState.IDLE, 0);
            this.globalIdleDelayTicks = 16;
        }
        refreshScaledCarapaceMax(level, false);
        restoreCarapace(level, false);
        emitPhaseTransitionShockwave(level);
    }

    protected void emitPhaseTransitionShockwave(ServerLevel level) {
        spawnGroundRingTelegraph(level, ModParticles.HIVE_PULSE.get(), this.position(), 5.5D, 18);
        level.sendParticles(ModParticles.IMPACT.get(), this.getX(), this.getY(0.55), this.getZ(), 3, 0.8, 0.5, 0.8, 0.08);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.5D),
                e -> e != this && !(e instanceof MycelialMob) && e.isAlive())) {
            Vec3 away = entity.position().subtract(this.position());
            if (away.lengthSqr() > 0.01D) {
                Vec3 dir = away.normalize().scale(0.80D);
                entity.push(dir.x, 0.32D, dir.z);
            }
        }
    }

    public void refreshScaledCarapaceMax(ServerLevel level, boolean preserveRatio) {
        int nearbyPlayers = level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0D),
                p -> p.isAlive() && !p.isSpectator()).size();
        float cycleHpMult = 1.0F;
        if (level.getServer() != null) {
            HiveCycleSavedData cycles = HiveCycleSavedData.get(level.getServer().overworld());
            cycleHpMult = (float) HiveCycleEngine.computeBossHealthMultiplier(
                    cycles.isCycleActive(), cycles.getActiveCycleNumber(), cycles.getActiveMutatorMask());
        }
        float scaledMax = BossCombatArchitectureEngine.computeScaledCarapaceIntegrity(
                getCarapaceProfile().maxIntegrity(), Math.max(1, nearbyPlayers), cycleHpMult);
        float oldMax = getMaxCarapaceIntegrity();
        float oldCurrent = getCarapaceIntegrity();
        this.entityData.set(DATA_MAX_CARAPACE_INTEGRITY, scaledMax);
        if (!isCarapaceBroken()) {
            if (preserveRatio && oldMax > 0.0F) {
                float ratio = Math.max(0.0F, Math.min(1.0F, oldCurrent / oldMax));
                this.entityData.set(DATA_CARAPACE_INTEGRITY, scaledMax * ratio);
            } else {
                this.entityData.set(DATA_CARAPACE_INTEGRITY, scaledMax);
            }
        }
    }

    public void restoreCarapace(ServerLevel level, boolean notify) {
        boolean wasBroken = isCarapaceBroken();
        this.entityData.set(DATA_CARAPACE_INTEGRITY, getMaxCarapaceIntegrity());
        this.entityData.set(DATA_CARAPACE_BROKEN_TICKS, 0);
        if (wasBroken) {
            onCarapaceStateChanged(level, false);
            if (notify) {
                level.sendParticles(ModParticles.HIVE_PULSE.get(),
                        this.getX(), this.getY(0.5), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_SHIELD.get(),
                        SoundSource.HOSTILE, 1.15F, 1.25F);
            }
        }
    }

    public boolean triggerCarapaceBreak(ServerLevel level) {
        if (isCarapaceBroken()) {
            return false;
        }
        BossCarapaceProfile profile = getCarapaceProfile();
        this.entityData.set(DATA_CARAPACE_INTEGRITY, 0.0F);
        this.entityData.set(DATA_CARAPACE_BROKEN_TICKS, profile.brokenWindowTicks());
        enterStaggeredState(level, profile.staggerTicks());
        onCarapaceStateChanged(level, true);

        level.sendParticles(ModParticles.CHITIN_SHARD.get(),
                this.getX(), this.getY(0.55), this.getZ(), 24, 0.9, 0.8, 0.9, 0.12);
        level.sendParticles(ModParticles.IMPACT.get(),
                this.getX(), this.getY(0.6), this.getZ(), 2, 0.2, 0.2, 0.2, 0.10);
        level.playSound(null, this.blockPosition(), ModSounds.BOSS_CARAPACE_RICOCHET.get(),
                SoundSource.HOSTILE, 1.55F, 0.55F);
        level.playSound(null, this.blockPosition(), ModSounds.BOSS_WEAKPOINT_CRIT.get(),
                SoundSource.HOSTILE, 1.35F, 1.20F);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float effectiveAmount = amount;
        if (!this.level().isClientSide() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float nextPhaseRatio = getNextPhaseThresholdRatio();
            if (nextPhaseRatio > 0.0F) {
                effectiveAmount = BossCombatArchitectureEngine.clampPhaseTransitionDamage(
                        this.getHealth(), this.getMaxHealth(), amount, nextPhaseRatio);
            }
        }
        return super.hurt(source, effectiveAmount);
    }

    @Override
    public boolean hurtFromBossPart(MycelialBossPart part, DamageSource source, float modifiedAmount) {
        boolean hurt = super.hurtFromBossPart(part, source, modifiedAmount);
        if (hurt && this.level() instanceof ServerLevel serverLevel
                && part.getRole() == MycelialBossPart.PartRole.ARMORED_CARAPACE
                && !isCarapaceBroken()
                && !isBossInvulnerableToCarapaceDamage(serverLevel)) {
            float carapaceDamage = BossCombatArchitectureEngine.computeCarapaceIntegrityDamage(
                    modifiedAmount,
                    source.is(DamageTypeTags.BYPASSES_ARMOR),
                    source.is(DamageTypeTags.WITCH_RESISTANT_TO)
            );
            float remaining = Math.max(0.0F, getCarapaceIntegrity() - carapaceDamage);
            this.entityData.set(DATA_CARAPACE_INTEGRITY, remaining);
            if (remaining <= 0.0F) {
                triggerCarapaceBreak(serverLevel);
            }
        }
        return hurt;
    }

    // --- Telegraph particle helpers ---

    protected void spawnGroundRingTelegraph(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        int count = Math.max(6, points);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, px, center.y + 0.15D, pz, 1, 0.02, 0.02, 0.02, 0.01);
        }
    }

    protected void spawnGroundLineTelegraph(ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end, int steps) {
        int count = Math.max(4, steps);
        Vec3 delta = end.subtract(start);
        for (int i = 1; i <= count; i++) {
            double t = (double) i / (double) count;
            level.sendParticles(particle,
                    start.x + delta.x * t,
                    start.y + delta.y * t + 0.2D,
                    start.z + delta.z * t,
                    1, 0.08, 0.05, 0.08, 0.01);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(TAG_CARAPACE_INTEGRITY, getCarapaceIntegrity());
        tag.putFloat(TAG_MAX_CARAPACE_INTEGRITY, getMaxCarapaceIntegrity());
        tag.putInt(TAG_CARAPACE_BROKEN_TICKS, getCarapaceBrokenTicksRemaining());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_MAX_CARAPACE_INTEGRITY)) {
            this.entityData.set(DATA_MAX_CARAPACE_INTEGRITY, Math.max(1.0F, tag.getFloat(TAG_MAX_CARAPACE_INTEGRITY)));
            this.carapaceScaledInitialized = true;
        } else {
            this.entityData.set(DATA_MAX_CARAPACE_INTEGRITY, getCarapaceProfile().maxIntegrity());
        }
        if (tag.contains(TAG_CARAPACE_INTEGRITY)) {
            this.entityData.set(DATA_CARAPACE_INTEGRITY, Math.max(0.0F, tag.getFloat(TAG_CARAPACE_INTEGRITY)));
        } else {
            this.entityData.set(DATA_CARAPACE_INTEGRITY, getMaxCarapaceIntegrity());
        }
        if (tag.contains(TAG_CARAPACE_BROKEN_TICKS)) {
            this.entityData.set(DATA_CARAPACE_BROKEN_TICKS, Math.max(0, tag.getInt(TAG_CARAPACE_BROKEN_TICKS)));
        }
    }
}
