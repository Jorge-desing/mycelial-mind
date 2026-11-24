package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.entity.ai.brain.behaviour.Idle;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetPlayerLookTarget;
import com.alex.mycelialmind.entity.ai.brain.core.AllApplicableBehaviours;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCarapaceProfile;
import com.alex.mycelialmind.hive.DamageTelemetryTracker;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Sanctuary Boss: The Hive Core (350 HP, 3 Phases).
 * Phase 1 (100-65% HP): Synaptic Command — telegraphed Acid Fan, Synaptic Sweep, and Command Pulse summons.
 * Phase 2 (65-30% HP): Armored Nexus — 3 Sentinel Node anchors grant invulnerability and fire telegraphed Anchor Rays.
 * Phase 3 (<30% HP): Hive Overload — rotating Tier 2 + Synergy mutations, telegraphed Spore Shockwave,
 * and Synaptic Sweep -> Spore Shockwave combos.
 * Shattering its Chitin Bastion staggers the boss and lowers its Synaptic Crown weak point.
 */
public class HiveCoreBossEntity extends MycelialBossMob {
    private static final EntityDataAccessor<Integer> DATA_BOSS_PHASE =
            SynchedEntityData.defineId(HiveCoreBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANCHORS =
            SynchedEntityData.defineId(HiveCoreBossEntity.class, EntityDataSerializers.INT);

    private static final String TAG_PHASE = "BossPhase";
    private static final String TAG_PHASE2_DONE = "Phase2ShieldBroken";
    private static final String TAG_ANCHORS = "Phase2Anchors";

    private static final List<BossAttackId> ATTACK_KIT = List.of(
            BossAttackId.HIVE_CORE_ACID_FAN,
            BossAttackId.HIVE_CORE_SYNAPTIC_SWEEP,
            BossAttackId.HIVE_CORE_COMMAND_PULSE,
            BossAttackId.HIVE_CORE_ANCHOR_RAY,
            BossAttackId.HIVE_CORE_SPORE_SHOCKWAVE
    );
    private static final List<BossAttackId> ATTACK_KIT_NO_ANCHORS = List.of(
            BossAttackId.HIVE_CORE_ACID_FAN,
            BossAttackId.HIVE_CORE_SYNAPTIC_SWEEP,
            BossAttackId.HIVE_CORE_COMMAND_PULSE,
            BossAttackId.HIVE_CORE_SPORE_SHOCKWAVE
    );

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.mycelialmind.hive_core"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private final MycelialBossPart weakPointPart;
    private final MycelialBossPart carapacePart;
    private final MycelialBossPart[] bossParts;

    private final List<BlockPos> anchorPositions = new ArrayList<>();
    private boolean phase2ShieldBroken = false;
    private int synergyRotationIndex = 0;

    public HiveCoreBossEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 350;
        this.weakPointPart = new MycelialBossPart(this, "synaptic_crown", MycelialBossPart.PartRole.WEAK_POINT, 1.5F, 1.3F);
        this.carapacePart = new MycelialBossPart(this, "chitin_bastion", MycelialBossPart.PartRole.ARMORED_CARAPACE, 2.0F, 1.8F);
        this.bossParts = new MycelialBossPart[]{this.weakPointPart, this.carapacePart};
        this.setId(ENTITY_COUNTER.getAndAdd(this.bossParts.length + 1) + 1);
    }

    @Override
    protected BossCarapaceProfile getCarapaceProfile() {
        return BossCarapaceProfile.HIVE_CORE;
    }

    @Override
    protected List<BossAttackId> getBossAttackKit() {
        return isShieldedPhaseTwo() ? ATTACK_KIT : ATTACK_KIT_NO_ANCHORS;
    }

    @Override
    public int getActiveBossPhase() {
        return getBossPhase();
    }

    @Override
    public int getMaxBossPhases() {
        return 3;
    }

    @Override
    protected float getNextPhaseThresholdRatio() {
        int phase = getBossPhase();
        if (phase == 1) {
            return 0.65F;
        }
        if (phase == 2 && phase2ShieldBroken) {
            return 0.30F;
        }
        return -1.0F;
    }

    public int getSyncedRemainingAnchors() {
        return this.entityData.get(DATA_REMAINING_ANCHORS);
    }

    @Override
    protected boolean isBossInvulnerableToCarapaceDamage(ServerLevel level) {
        return isShieldedPhaseTwo();
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
        double weakY = 3.20D + getWeakPointLowerOffset();
        double weakFwd = isCarapaceBroken() ? 0.35D : 0.0D;
        positionBossPart(this, this.weakPointPart, weakFwd, weakY, 0.0D);
        positionBossPart(this, this.carapacePart, 0.85D, 0.30D, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BOSS_PHASE, 1);
        builder.define(DATA_REMAINING_ANCHORS, 0);
    }

    public int getBossPhase() {
        return this.entityData.get(DATA_BOSS_PHASE);
    }

    public void setBossPhase(int phase) {
        this.entityData.set(DATA_BOSS_PHASE, phase);
    }

    public List<BlockPos> getAnchorPositions() {
        return List.copyOf(anchorPositions);
    }

    public boolean isShieldedPhaseTwo() {
        if (getBossPhase() != 2 || phase2ShieldBroken || !(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return countRemainingAnchors(serverLevel) > 0;
    }

    public int countRemainingAnchors(ServerLevel level) {
        int remaining = 0;
        for (BlockPos pos : anchorPositions) {
            if (level.getBlockState(pos).is(ModBlocks.SENTINEL_NODE.get())
                    && level.getBlockEntity(pos) instanceof SynapticNodeBlockEntity node
                    && node.isBossAnchorFor(this.getUUID())) {
                remaining++;
            }
        }
        return remaining;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BossStateGatedMeleeGoal(this, 0.9D, false));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public List<HiveSensor<MycelialMob>> getSensors() {
        return HiveBrainBridge.createDefaultHiveSensorsWithProjectiles();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getCoreTasks() {
        var projectileAlert = new CustomBehaviour<MycelialMob>(mob -> {
            BlockPos incoming = HiveBrainBridge.getIncomingProjectilePos(mob);
            if (incoming != null && mob.level() instanceof ServerLevel serverLevel) {
                HiveBlackboard.reportTarget(serverLevel, incoming);
                serverLevel.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                        mob.getX(), mob.getY(1.0D), mob.getZ(),
                        6, 0.5D, 0.3D, 0.5D, 0.03D);
            }
        });
        projectileAlert.startCondition(mob -> HiveBrainUtils.hasMemory(mob, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileAlert.cooldownFor(mob -> 40);

        return BrainActivityGroup.coreTasks(
                HiveBrainBridge.createBossTargetSyncBehaviour(),
                new AllApplicableBehaviours<>(
                        new LookAtTarget<MycelialMob>().runFor(mob -> 15),
                        projectileAlert
                )
        );
    }

    @Override
    public BrainActivityGroup<MycelialMob> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new SetPlayerLookTarget<>(),
                new Idle<MycelialMob>().runFor(mob -> 40)
        );
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        return BrainActivityGroup.fightTasks();
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / Math.max(1.0F, this.getMaxHealth()));

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float hpRatio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        int currentPhase = getBossPhase();

        if (currentPhase == 1 && BossCombatArchitectureEngine.resolveHiveCorePhase(hpRatio, false) >= 2) {
            enterPhaseTwo(serverLevel);
        } else if (currentPhase == 2) {
            int remaining = countRemainingAnchors(serverLevel);
            this.entityData.set(DATA_REMAINING_ANCHORS, phase2ShieldBroken ? 0 : remaining);
            if (remaining > 0) {
                if (this.tickCount % 15 == 0) {
                    for (BlockPos anchor : anchorPositions) {
                        if (serverLevel.getBlockState(anchor).is(ModBlocks.SENTINEL_NODE.get())
                                && serverLevel.getBlockEntity(anchor) instanceof SynapticNodeBlockEntity node
                                && node.isBossAnchorFor(this.getUUID())) {
                            spawnAnchorBeam(serverLevel, anchor);
                        }
                    }
                }
            } else if (!phase2ShieldBroken) {
                onPhaseTwoShieldCollapsed(serverLevel);
            }
            if (BossCombatArchitectureEngine.resolveHiveCorePhase(hpRatio, phase2ShieldBroken) == 3) {
                enterPhaseThree(serverLevel);
            }
        } else if (this.entityData.get(DATA_REMAINING_ANCHORS) != 0) {
            this.entityData.set(DATA_REMAINING_ANCHORS, 0);
        }

        // Phase 3 rotating HiveSynergy
        if (getBossPhase() == 3 && this.tickCount % 240 == 0 && !isBossStaggered()) {
            rotateOverloadSynergy();
            serverLevel.playSound(null, this.blockPosition(), ModSounds.HIVE_MUTATE_TIER2.get(), SoundSource.HOSTILE, 1.0F, 1.15F);
        }

        // Symbiotic node healing (Phase 1 & 3)
        if (this.tickCount % 20 == 0 && !isShieldedPhaseTwo() && !isBossStaggered() && hasNearbySymbioticNode(serverLevel)) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1));
            this.heal(1.0F);
            serverLevel.sendParticles(ModParticles.HIVE_EMBER.get(), this.getX(), this.getY(0.5), this.getZ(),
                    8, 0.8, 0.8, 0.8, 0.02);
        }

        tickBossCombatStateMachine(serverLevel, this.getTarget(), getBossPhase());
    }

    private void updateBossBarAppearance() {
        if (isCarapaceBroken()) {
            this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.hive_core.carapace_broken"));
            return;
        }
        int phase = getBossPhase();
        if (phase == 2 && !phase2ShieldBroken) {
            this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.hive_core.phase2"));
        } else if (phase >= 3) {
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.hive_core.phase3"));
        } else {
            this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.hive_core"));
        }
    }

    @Override
    protected void onCarapaceStateChanged(ServerLevel level, boolean broken) {
        updateBossBarAppearance();
    }

    private void enterPhaseTwo(ServerLevel level) {
        setBossPhase(2);
        phase2ShieldBroken = false;
        anchorPositions.clear();
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2.0 * i) / 3.0;
            int ax = (int) Math.round(this.getX() + Math.cos(angle) * 6.0);
            int az = (int) Math.round(this.getZ() + Math.sin(angle) * 6.0);
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ax, az);
            int ay = Math.abs(surfaceY - this.blockPosition().getY()) <= 4 ? surfaceY : this.blockPosition().getY();
            BlockPos preferredPos = new BlockPos(ax, ay, az);
            BlockPos anchorPos = findOpenAnchorPosition(level, preferredPos);
            if (anchorPos == null || !level.setBlockAndUpdate(anchorPos, ModBlocks.SENTINEL_NODE.get().defaultBlockState())) {
                continue;
            }
            if (level.getBlockEntity(anchorPos) instanceof SynapticNodeBlockEntity node) {
                node.markBossAnchor(this.getUUID());
                anchorPositions.add(anchorPos.immutable());
            } else {
                level.destroyBlock(anchorPos, false);
            }
        }
        this.entityData.set(DATA_REMAINING_ANCHORS, anchorPositions.size());
        onBossPhaseTransition(level);
        updateBossBarAppearance();
        level.playSound(null, this.blockPosition(), ModSounds.BOSS_PHASE_SHIFT.get(), SoundSource.HOSTILE, 1.65F, 0.92F);
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_SHIELD.get(), SoundSource.HOSTILE, 1.40F, 0.92F);
    }

    private static BlockPos findOpenAnchorPosition(ServerLevel level, BlockPos preferred) {
        for (int distance = 0; distance <= 4; distance++) {
            int[] offsets = distance == 0 ? new int[]{0} : new int[]{distance, -distance};
            for (int offset : offsets) {
                BlockPos candidate = preferred.offset(0, offset, 0);
                if (level.isInWorldBounds(candidate) && level.isEmptyBlock(candidate)
                        && level.getBlockEntity(candidate) == null
                        && level.getEntitiesOfClass(LivingEntity.class, new AABB(candidate), LivingEntity::isAlive).isEmpty()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void onPhaseTwoShieldCollapsed(ServerLevel level) {
        phase2ShieldBroken = true;
        enterStaggeredState(level, 60);
        updateBossBarAppearance();
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_SHIELD.get(), SoundSource.HOSTILE, 1.60F, 0.65F);
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_HURT.get(), SoundSource.HOSTILE, 1.45F, 0.78F);
        level.sendParticles(ModParticles.CHITIN_SHARD.get(), this.getX(), this.getY(0.5), this.getZ(), 10, 1.0, 1.0, 1.0, 0.06);
        if (this.getHealth() / Math.max(1.0F, this.getMaxHealth()) <= 0.30F) {
            enterPhaseThree(level);
        }
    }

    private void enterPhaseThree(ServerLevel level) {
        setBossPhase(3);
        this.entityData.set(DATA_REMAINING_ANCHORS, 0);
        onBossPhaseTransition(level);
        updateBossBarAppearance();
        level.playSound(null, this.blockPosition(), ModSounds.BOSS_PHASE_SHIFT.get(), SoundSource.HOSTILE, 1.80F, 0.78F);
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_RESONANCE.get(), SoundSource.HOSTILE, 1.60F, 0.72F);
        rotateOverloadSynergy();
    }

    @Override
    protected void onAttackWindupStart(ServerLevel level, BossAttackId attack, LivingEntity target) {
        switch (attack) {
            case HIVE_CORE_ACID_FAN -> level.playSound(null, this.blockPosition(),
                    ModSounds.HIVE_CORE_RESONANCE.get(), SoundSource.HOSTILE, 1.25F, 1.15F);
            case HIVE_CORE_SYNAPTIC_SWEEP -> level.playSound(null, this.blockPosition(),
                    ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 1.35F, 1.10F);
            case HIVE_CORE_COMMAND_PULSE -> level.playSound(null, this.blockPosition(),
                    ModSounds.HIVE_CORE_RESONANCE.get(), SoundSource.HOSTILE, 1.40F, 0.85F);
            case HIVE_CORE_ANCHOR_RAY -> level.playSound(null, this.blockPosition(),
                    ModSounds.HIVE_CORE_SHIELD.get(), SoundSource.HOSTILE, 1.35F, 1.35F);
            case HIVE_CORE_SPORE_SHOCKWAVE -> level.playSound(null, this.blockPosition(),
                    ModSounds.HIVE_CORE_RESONANCE.get(), SoundSource.HOSTILE, 1.55F, 1.32F);
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
            case HIVE_CORE_ACID_FAN -> {
                if (anchor != null) {
                    spawnGroundLineTelegraph(level, ModParticles.ACID_DROPLET.get(), this.position(), anchor, 7);
                }
            }
            case HIVE_CORE_SYNAPTIC_SWEEP -> {
                double r = 2.0D + 4.5D * getStateProgress();
                spawnGroundRingTelegraph(level, ModParticles.SYNAPSE_SPARK.get(), this.position(), r, 14);
            }
            case HIVE_CORE_COMMAND_PULSE -> {
                spawnGroundRingTelegraph(level, ModParticles.HIVE_PULSE.get(), this.position(), 4.2D, 12);
            }
            case HIVE_CORE_ANCHOR_RAY -> {
                if (anchor != null) {
                    if (!isTelegraphLocked()) {
                        this.telegraphAnchor = anchor;
                    }
                    Vec3 lockedAnchor = this.telegraphAnchor != null ? this.telegraphAnchor : anchor;
                    spawnGroundRingTelegraph(level, ModParticles.SYNAPSE_SPARK.get(), lockedAnchor, 2.8D, 12);
                    for (BlockPos pos : anchorPositions) {
                        if (level.getBlockState(pos).is(ModBlocks.SENTINEL_NODE.get())) {
                            spawnGroundLineTelegraph(level, ModParticles.HIVE_PULSE.get(), Vec3.atCenterOf(pos), lockedAnchor, 6);
                        }
                    }
                }
            }
            case HIVE_CORE_SPORE_SHOCKWAVE -> {
                double r = 2.5D + 5.2D * getStateProgress();
                spawnGroundRingTelegraph(level, ModParticles.NECROTIC_MIST.get(), this.position(), r, 18);
                level.sendParticles(ModParticles.NECROTIC_MIST.get(), this.getX(), this.getY() + 0.5, this.getZ(),
                        12, 2.2, 0.3, 2.2, 0.02);
            }
            default -> {
            }
        }
    }

    @Override
    protected void onAttackExecute(ServerLevel level, BossAttackId attack, @Nullable LivingEntity target) {
        switch (attack) {
            case HIVE_CORE_ACID_FAN -> {
                if (target != null && target.isAlive()) {
                    fireAcidFan(target);
                }
            }
            case HIVE_CORE_SYNAPTIC_SWEEP -> executeSynapticSweep(level);
            case HIVE_CORE_COMMAND_PULSE -> executeCommandPulse(level, target);
            case HIVE_CORE_ANCHOR_RAY -> executeAnchorRay(level);
            case HIVE_CORE_SPORE_SHOCKWAVE -> detonateSporeShockwave(level);
            default -> {
            }
        }
    }

    private void executeSynapticSweep(ServerLevel level) {
        spawnGroundRingTelegraph(level, ModParticles.HIVE_EMBER.get(), this.position(), 6.5D, 22);
        level.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 1.45F, 0.85F);
        float dmg = getBossPhase() >= 3 ? 9.5F : 7.5F;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.4D),
                e -> !(e instanceof MycelialMob) && e.isAlive())) {
            entity.hurt(this.damageSources().mobAttack(this), dmg);
            Vec3 away = entity.position().subtract(this.position());
            if (away.lengthSqr() > 0.01D) {
                Vec3 dir = away.normalize().scale(0.55D);
                entity.push(dir.x, 0.25D, dir.z);
            }
        }
    }

    private void executeCommandPulse(ServerLevel serverLevel, @Nullable LivingEntity target) {
        serverLevel.sendParticles(ModParticles.HIVE_PULSE.get(), this.getX(), this.getY(0.6), this.getZ(),
                4, 1.2, 0.6, 1.2, 0.02);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 1.30F, 0.78F);
        if (target == null || !target.isAlive()) {
            return;
        }
        MycelialMob summon = switch (getBossPhase()) {
            case 3 -> (this.random.nextBoolean() ? ModEntities.ACID_SPITTER.get() : ModEntities.SYNAPSE_WEAVER.get()).create(serverLevel);
            case 2 -> ModEntities.MYCELIAL_STALKER.get().create(serverLevel);
            default -> ModEntities.SPORELING.get().create(serverLevel);
        };
        if (summon != null) {
            BlockPos summonPos = MycelialSpawnUtil.findOpenGroundPosition(
                    serverLevel, summon, this.blockPosition(), 4, 2);
            if (summonPos != null) {
                summon.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(summonPos), MobSpawnType.MOB_SUMMONED, null);
                summon.setTarget(target);
                if (serverLevel.noCollision(summon)) {
                    serverLevel.addFreshEntity(summon);
                }
            }
        }
    }

    private void executeAnchorRay(ServerLevel level) {
        Vec3 targetPos = this.telegraphAnchor != null ? this.telegraphAnchor : this.position();
        spawnGroundRingTelegraph(level, ModParticles.HIVE_EMBER.get(), targetPos, 3.0D, 16);
        level.sendParticles(ModParticles.SYNAPSE_SPARK.get(), targetPos.x, targetPos.y + 0.5, targetPos.z,
                20, 1.2, 0.5, 1.2, 0.06);
        level.playSound(null, BlockPos.containing(targetPos), ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 1.45F, 1.25F);
        AABB blastBox = new AABB(targetPos.x - 3.0D, targetPos.y - 1.5D, targetPos.z - 3.0D,
                targetPos.x + 3.0D, targetPos.y + 2.5D, targetPos.z + 3.0D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, blastBox,
                e -> !(e instanceof MycelialMob) && e.isAlive())) {
            entity.hurt(this.damageSources().magic(), 7.5F);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
    }

    private void rotateOverloadSynergy() {
        HiveSynergy[] all = HiveSynergy.values();
        HiveSynergy chosen = all[synergyRotationIndex % all.length];
        synergyRotationIndex++;
        EnumSet<AdaptationTrait> traits = EnumSet.of(chosen.first(), chosen.second());
        applyHiveState(traits, traits, EnumSet.of(chosen));
    }

    private void detonateSporeShockwave(ServerLevel level) {
        level.sendParticles(ModParticles.HIVE_EMBER.get(), this.getX(), this.getY(0.5), this.getZ(),
                40, 3.0, 0.8, 3.0, 0.08);
        level.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 1.50F, 0.65F);
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_SHIELD.get(), SoundSource.HOSTILE, 1.20F, 0.58F);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(7.2D),
                e -> !(e instanceof MycelialMob) && e.isAlive())) {
            entity.hurt(this.damageSources().magic(), 8.5F);
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }
    }

    private void fireAcidFan(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.4) - this.getY(0.6);
        double dz = target.getZ() - this.getZ();
        float[] yawOffsets = getBossPhase() >= 3
                ? new float[]{-0.26F, -0.13F, 0.0F, 0.13F, 0.26F}
                : new float[]{-0.18F, 0.0F, 0.18F};
        for (float offset : yawOffsets) {
            AcidSpitProjectile spit = new AcidSpitProjectile(this.level(), this);
            double cos = Math.cos(offset);
            double sin = Math.sin(offset);
            double rx = dx * cos - dz * sin;
            double rz = dx * sin + dz * cos;
            spit.shoot(rx, dy, rz, 1.35F, 2.0F);
            this.level().addFreshEntity(spit);
        }
        this.playSound(ModSounds.ACID_SPITTER_SPIT.get(), 1.25F, 0.65F);
    }

    private void spawnAnchorBeam(ServerLevel level, BlockPos anchor) {
        double sx = this.getX();
        double sy = this.getY(0.6);
        double sz = this.getZ();
        double dx = (anchor.getX() + 0.5) - sx;
        double dy = (anchor.getY() + 0.5) - sy;
        double dz = (anchor.getZ() + 0.5) - sz;
        for (int i = 1; i <= 5; i++) {
            double t = i / 6.0;
            level.sendParticles(ModParticles.HIVE_PULSE.get(), sx + dx * t, sy + dy * t, sz + dz * t,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private boolean hasNearbySymbioticNode(ServerLevel level) {
        return PurificationZoneTracker.get(level).hasNearbySymbioticNode(this.blockPosition(), 10);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && isShieldedPhaseTwo()) {
            this.playSound(ModSounds.HIVE_CORE_SHIELD.get(), 1.0F, 1.15F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ModParticles.HIVE_PULSE.get(), this.getX(), this.getY(0.5), this.getZ(),
                        2, 0.6, 0.8, 0.6, 0.02);
                serverLevel.sendParticles(ModParticles.CHITIN_SHARD.get(), this.getX(), this.getY(0.5), this.getZ(),
                        6, 0.6, 0.5, 0.6, 0.03);
            }
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide() && getBossPhase() < 3) {
            StimulusType stimulus = DamageTelemetryTracker.classifyDamage(source);
            if (stimulus != null) {
                EnumSet<AdaptationTrait> shifted = getTraits().isEmpty()
                        ? EnumSet.noneOf(AdaptationTrait.class)
                        : EnumSet.copyOf(getTraits());
                for (AdaptationTrait trait : AdaptationTrait.values()) {
                    if (trait.trigger() == stimulus) {
                        shifted.add(trait);
                    }
                }
                applyHiveState(shifted, getTier2Traits(), getSynergies());
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource cause) {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (BlockPos anchor : anchorPositions) {
                if (serverLevel.getBlockState(anchor).is(ModBlocks.SENTINEL_NODE.get())
                        && serverLevel.getBlockEntity(anchor) instanceof SynapticNodeBlockEntity node
                        && node.isBossAnchorFor(this.getUUID())) {
                    serverLevel.destroyBlock(anchor, false);
                }
            }
        }
        super.die(cause);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (recentlyHit) {
            this.spawnAtLocation(com.alex.mycelialmind.item.FieldEngramItem.createStack(
                    com.alex.mycelialmind.hive.FieldEngramId.CHITIN_RESONANCE));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_PHASE, getBossPhase());
        tag.putBoolean(TAG_PHASE2_DONE, phase2ShieldBroken);
        ListTag list = new ListTag();
        for (BlockPos pos : anchorPositions) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_ANCHORS, list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_PHASE)) {
            setBossPhase(tag.getInt(TAG_PHASE));
        }
        this.phase2ShieldBroken = tag.getBoolean(TAG_PHASE2_DONE);
        anchorPositions.clear();
        ListTag list = tag.getList(TAG_ANCHORS, Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            anchorPositions.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
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
    protected float getBaseSoundVolume() {
        return 1.60F;
    }

    @Override
    protected float getStepSoundVolume() {
        return 0.55F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 110;
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_COLOSSAL.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HIVE_CORE_RESONANCE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.HIVE_CORE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HIVE_CORE_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 350.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }
}
