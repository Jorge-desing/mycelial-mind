package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.entity.ai.brain.behaviour.Idle;
import com.alex.mycelialmind.entity.ai.brain.behaviour.InvalidateAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetPlayerLookTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.DelayedBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCarapaceProfile;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.ModDimensions;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Dimensional Apex Boss of the Deep Mycelium: The Apex Matriarch (600 HP, 3 Phases).
 * Phase 1 (100-60%): Telegraphed 5-shot Spore Barrage + ground-telegraphed Echo Teleport strike.
 * Phase 2 (60-25%): Summons elite Warden/Weaver squad + 2 Sentinel Nodes + telegraphed Homeostatic Pulse.
 * Phase 3 (<25%): Synaptic Storm frenzy — AoE infection storms, full traits + synergies,
 * and Echo Teleport -> Synaptic Storm combos.
 * Shattering her Thorax Shield staggers her and lowers the Apex Reliquary weak point.
 */
public class ApexMatriarchEntity extends MycelialBossMob {
    private static final String TAG_PHASE_TWO_SENTINELS = "Phase2Sentinels";

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(ApexMatriarchEntity.class, EntityDataSerializers.INT);

    private static final List<BossAttackId> ATTACK_KIT = List.of(
            BossAttackId.MATRIARCH_SPORE_BARRAGE,
            BossAttackId.MATRIARCH_ECHO_TELEPORT,
            BossAttackId.MATRIARCH_HOMEOSTATIC_PULSE,
            BossAttackId.MATRIARCH_SYNAPTIC_STORM
    );

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.mycelialmind.apex_matriarch"),
            BossEvent.BossBarColor.PINK,
            BossEvent.BossBarOverlay.NOTCHED_12
    );

    private final MycelialBossPart weakPointPart;
    private final MycelialBossPart carapacePart;
    private final MycelialBossPart[] bossParts;

    private boolean phase2Triggered = false;
    private final List<BlockPos> phaseTwoSentinelPositions = new ArrayList<>();

    public ApexMatriarchEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 750;
        this.weakPointPart = new MycelialBossPart(this, "apex_reliquary", MycelialBossPart.PartRole.WEAK_POINT, 1.6F, 1.4F);
        this.carapacePart = new MycelialBossPart(this, "thorax_shield", MycelialBossPart.PartRole.ARMORED_CARAPACE, 2.2F, 2.0F);
        this.bossParts = new MycelialBossPart[]{this.weakPointPart, this.carapacePart};
        this.setId(ENTITY_COUNTER.getAndAdd(this.bossParts.length + 1) + 1);
    }

    @Override
    protected BossCarapaceProfile getCarapaceProfile() {
        return BossCarapaceProfile.APEX_MATRIARCH;
    }

    @Override
    protected List<BossAttackId> getBossAttackKit() {
        return ATTACK_KIT;
    }

    @Override
    public int getActiveBossPhase() {
        return getMatriarchPhase();
    }

    @Override
    public int getMaxBossPhases() {
        return 3;
    }

    @Override
    protected float getNextPhaseThresholdRatio() {
        int phase = getMatriarchPhase();
        if (phase == 1) {
            return 0.60F;
        }
        if (phase == 2) {
            return 0.25F;
        }
        return -1.0F;
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
        double weakY = 4.10D + getWeakPointLowerOffset();
        double weakFwd = isCarapaceBroken() ? 0.45D : 0.0D;
        positionBossPart(this, this.weakPointPart, weakFwd, weakY, 0.0D);
        positionBossPart(this, this.carapacePart, 0.95D, 0.40D, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
    }

    public int getMatriarchPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public void setMatriarchPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BossStateGatedMeleeGoal(this, 1.05D, false));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 20.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public List<HiveSensor<MycelialMob>> getSensors() {
        return HiveBrainBridge.createDefaultHiveSensorsWithProjectiles();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getCoreTasks() {
        var multiPlayerAwareness = new CustomBehaviour<MycelialMob>(mob -> {
            List<Player> players = HiveBrainUtils.getMemory(mob, MemoryModuleType.NEAREST_PLAYERS);
            if (players != null && players.size() >= 2 && mob.level() instanceof ServerLevel level) {
                Player secondary = players.get(1);
                HiveBrainUtils.setForgettableMemory(mob, MemoryModuleType.LOOK_TARGET, new EntityTracker(secondary, true), 25);
                level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                        mob.getX(), mob.getY(0.85D), mob.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        });
        multiPlayerAwareness.startCondition(mob -> {
            List<Player> players = HiveBrainUtils.getMemory(mob, MemoryModuleType.NEAREST_PLAYERS);
            return players != null && players.size() >= 2;
        });
        multiPlayerAwareness.cooldownFor(mob -> 80);

        var projectileFlare = new CustomBehaviour<MycelialMob>(mob -> {
            if (mob.level() instanceof ServerLevel level) {
                level.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                        mob.getX(), mob.getY(0.95D), mob.getZ(), 5, 0.45D, 0.25D, 0.45D, 0.02D);
            }
        });
        projectileFlare.startCondition(mob -> HiveBrainUtils.hasMemory(mob, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileFlare.cooldownFor(mob -> 40);

        return BrainActivityGroup.coreTasks(
                HiveBrainBridge.createBossTargetSyncBehaviour(),
                new LookAtTarget<>(),
                multiPlayerAwareness,
                projectileFlare
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
        var psychicPresence = new DelayedBehaviour<MycelialMob>(20) {
            @Override
            protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
                return List.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
            }

            @Override
            protected void doDelayedAction(MycelialMob entity) {
                if (entity.level() instanceof ServerLevel level && !entity.isDesynced()) {
                    level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                            entity.getX(), entity.getY(0.8D), entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        };
        psychicPresence.cooldownFor(mob -> 100);

        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new ReactToUnreachableTarget<MycelialMob>().afterTicks(45L),
                psychicPresence
        );
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / Math.max(1.0F, this.getMaxHealth()));

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float hpRatio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        int resolvedPhase = BossCombatArchitectureEngine.resolveMatriarchPhase(hpRatio);
        if (getMatriarchPhase() == 1 && resolvedPhase >= 2) {
            enterPhaseTwo(serverLevel);
        } else if (getMatriarchPhase() == 2 && resolvedPhase == 3) {
            enterPhaseThree(serverLevel);
        }

        tickBossCombatStateMachine(serverLevel, this.getTarget(), getMatriarchPhase());
    }

    private void updateBossBarAppearance() {
        if (isCarapaceBroken()) {
            this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.apex_matriarch.carapace_broken"));
            return;
        }
        int phase = getMatriarchPhase();
        if (phase >= 3) {
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.apex_matriarch.phase3"));
        } else if (phase == 2) {
            this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.apex_matriarch.phase2"));
        } else {
            this.bossEvent.setColor(BossEvent.BossBarColor.PINK);
            this.bossEvent.setName(Component.translatable("entity.mycelialmind.apex_matriarch"));
        }
    }

    @Override
    protected void onCarapaceStateChanged(ServerLevel level, boolean broken) {
        updateBossBarAppearance();
    }

    private void enterPhaseTwo(ServerLevel level) {
        setMatriarchPhase(2);
        onBossPhaseTransition(level);
        updateBossBarAppearance();
        level.playSound(null, this.blockPosition(), ModSounds.BOSS_PHASE_SHIFT.get(), SoundSource.HOSTILE, 1.65F, 0.90F);
        level.playSound(null, this.blockPosition(), ModSounds.APEX_MATRIARCH_WAIL.get(), SoundSource.HOSTILE, 1.65F, 1.05F);
        if (DimensionalHomeostasisDirector.isHomeostasisDimension(level)) {
            DimensionalHomeostasisSavedData.get(level)
                    .addStress(new ChunkPos(this.blockPosition()), 20.0, level.getGameTime());
        }

        if (!phase2Triggered) {
            phase2Triggered = true;
            BlockPos left = this.blockPosition().west(4);
            BlockPos right = this.blockPosition().east(4);
            placeSentinelIfOpen(level, left);
            placeSentinelIfOpen(level, right);

            HiveMindSavedData hive = HiveMindSavedData.get(level);
            ColonyWardenEntity warden = ModEntities.COLONY_WARDEN.get().create(level);
            SynapseWeaverEntity weaver = ModEntities.SYNAPSE_WEAVER.get().create(level);
            for (MycelialMob mob : new MycelialMob[]{warden, weaver}) {
                if (mob != null) {
                    BlockPos spawnPos = MycelialSpawnUtil.findOpenGroundPosition(
                            level, mob, this.blockPosition(), 4, 2);
                    if (spawnPos != null) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null);
                        mob.applyHiveState(hive.getActiveTraits(), hive.getTier2Traits(), hive.getActiveSynergies());
                        LivingEntity target = this.getTarget();
                        if (target != null && target.isAlive()) {
                            mob.setTarget(target);
                        }
                        if (level.noCollision(mob)) {
                            level.addFreshEntity(mob);
                        }
                    }
                }
            }
        }
    }

    private void placeSentinelIfOpen(ServerLevel level, BlockPos preferred) {
        BlockPos candidate = findOpenAnchorPosition(level, preferred);
        if (candidate != null && level.setBlockAndUpdate(candidate, ModBlocks.SENTINEL_NODE.get().defaultBlockState())) {
            if (level.getBlockEntity(candidate) instanceof SynapticNodeBlockEntity node) {
                node.markBossAnchor(this.getUUID());
                phaseTwoSentinelPositions.add(candidate.immutable());
            } else {
                level.destroyBlock(candidate, false);
            }
        }
    }

    private void enterPhaseThree(ServerLevel level) {
        setMatriarchPhase(3);
        onBossPhaseTransition(level);
        updateBossBarAppearance();
        level.playSound(null, this.blockPosition(), ModSounds.BOSS_PHASE_SHIFT.get(), SoundSource.HOSTILE, 1.85F, 0.72F);
        level.playSound(null, this.blockPosition(), ModSounds.APEX_MATRIARCH_WAIL.get(), SoundSource.HOSTILE, 1.85F, 0.80F);
        if (DimensionalHomeostasisDirector.isHomeostasisDimension(level)) {
            DimensionalHomeostasisSavedData.get(level)
                    .addStress(new ChunkPos(this.blockPosition()), 25.0, level.getGameTime());
        }
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1));
        var traits = EnumSet.allOf(com.alex.mycelialmind.hive.AdaptationTrait.class);
        applyHiveState(traits, traits, EnumSet.of(HiveSynergy.PYROLYTIC_SHELL, HiveSynergy.VENOMOUS_COUNTER));
    }

    @Override
    protected void onAttackWindupStart(ServerLevel level, BossAttackId attack, LivingEntity target) {
        switch (attack) {
            case MATRIARCH_SPORE_BARRAGE -> level.playSound(null, this.blockPosition(),
                    ModSounds.APEX_MATRIARCH_WAIL.get(), SoundSource.HOSTILE, 1.35F, 1.15F);
            case MATRIARCH_ECHO_TELEPORT -> {
                BlockPos safePos = MycelialSpawnUtil.findOpenGroundPosition(level, this, target.blockPosition(), 4, 3);
                if (safePos != null) {
                    this.telegraphAnchor = Vec3.atBottomCenterOf(safePos);
                } else {
                    this.telegraphAnchor = target.position();
                }
                level.playSound(null, this.blockPosition(),
                        ModSounds.SYMBIOTIC_NODE_TELEPORT.get(), SoundSource.HOSTILE, 1.30F, 1.25F);
            }
            case MATRIARCH_HOMEOSTATIC_PULSE -> level.playSound(null, this.blockPosition(),
                    ModSounds.APEX_MATRIARCH_WAIL.get(), SoundSource.HOSTILE, 1.45F, 0.85F);
            case MATRIARCH_SYNAPTIC_STORM -> level.playSound(null, this.blockPosition(),
                    ModSounds.APEX_MATRIARCH_WAIL.get(), SoundSource.HOSTILE, 1.65F, 0.72F);
            default -> {
            }
        }
    }

    @Override
    protected void onAttackWindupTick(ServerLevel level, BossAttackId attack, int ticksRemaining, int totalTicks, @Nullable LivingEntity target) {
        if (ticksRemaining % 3 != 0) {
            return;
        }
        Vec3 anchor = this.telegraphAnchor != null
                ? this.telegraphAnchor
                : (target != null && target.isAlive() ? target.position() : this.position());
        switch (attack) {
            case MATRIARCH_SPORE_BARRAGE -> {
                if (target != null && target.isAlive()) {
                    spawnGroundLineTelegraph(level, ModParticles.ACID_DROPLET.get(), this.position(), target.position(), 8);
                }
            }
            case MATRIARCH_ECHO_TELEPORT -> {
                // Clear ground echo ring at the exact destination before teleporting
                spawnGroundRingTelegraph(level, ModParticles.TIDE_MOTE.get(), anchor, 2.4D, 14);
                spawnGroundLineTelegraph(level, ModParticles.SYNAPSE_SPARK.get(), this.position(), anchor, 7);
            }
            case MATRIARCH_HOMEOSTATIC_PULSE -> {
                if (!isTelegraphLocked() && target != null && target.isAlive()) {
                    this.telegraphAnchor = target.position();
                }
                Vec3 pulseCenter = this.telegraphAnchor != null ? this.telegraphAnchor : anchor;
                double r = 2.0D + 4.0D * getStateProgress();
                spawnGroundRingTelegraph(level, ModParticles.NECROTIC_MIST.get(), pulseCenter, r, 16);
            }
            case MATRIARCH_SYNAPTIC_STORM -> {
                double r = 2.5D + 5.8D * getStateProgress();
                spawnGroundRingTelegraph(level, ModParticles.TIDE_MOTE.get(), this.position(), r, 20);
                level.sendParticles(ModParticles.SYNAPSE_SPARK.get(), this.getX(), this.getY(0.6), this.getZ(),
                        12, 1.4, 0.9, 1.4, 0.05);
            }
            default -> {
            }
        }
    }

    @Override
    protected void onAttackExecute(ServerLevel level, BossAttackId attack, @Nullable LivingEntity target) {
        switch (attack) {
            case MATRIARCH_SPORE_BARRAGE -> {
                if (target != null && target.isAlive()) {
                    fireSporeBarrage(target);
                }
            }
            case MATRIARCH_ECHO_TELEPORT -> executeEchoTeleport(level, target);
            case MATRIARCH_HOMEOSTATIC_PULSE -> executeHomeostaticPulse(level);
            case MATRIARCH_SYNAPTIC_STORM -> executeSynapticStorm(level);
            default -> {
            }
        }
    }

    private void executeEchoTeleport(ServerLevel serverLevel, @Nullable LivingEntity target) {
        Vec3 dest = this.telegraphAnchor;
        if (dest == null && target != null && target.isAlive()) {
            dest = target.position();
        }
        if (dest == null) {
            return;
        }
        this.teleportTo(dest.x, dest.y, dest.z);
        serverLevel.sendParticles(ModParticles.TIDE_MOTE.get(), this.getX(), this.getY(0.5), this.getZ(),
                24, 0.8, 0.9, 0.8, 0.06);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.SYMBIOTIC_NODE_TELEPORT.get(), SoundSource.HOSTILE, 1.45F, 0.68F);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.2D),
                e -> !(e instanceof MycelialMob) && e.isAlive())) {
            victim.hurt(this.damageSources().mobAttack(this), 8.0F);
            victim.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 100, 0));
        }
    }

    private void executeHomeostaticPulse(ServerLevel serverLevel) {
        Vec3 center = this.telegraphAnchor != null ? this.telegraphAnchor : this.position();
        if (DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
            DimensionalHomeostasisSavedData.get(serverLevel)
                    .addStress(new ChunkPos(BlockPos.containing(center)), 12.0, serverLevel.getGameTime());
        }
        spawnGroundRingTelegraph(serverLevel, ModParticles.NECROTIC_MIST.get(), center, 5.5D, 20);
        serverLevel.sendParticles(ModParticles.ACID_DROPLET.get(), center.x, center.y + 0.5, center.z,
                28, 2.5, 0.6, 2.5, 0.06);
        serverLevel.playSound(null, BlockPos.containing(center), ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 1.40F, 0.72F);
        AABB box = new AABB(center.x - 5.5D, center.y - 2.0D, center.z - 5.5D,
                center.x + 5.5D, center.y + 3.0D, center.z + 5.5D);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> !(e instanceof MycelialMob) && e.isAlive())) {
            victim.hurt(this.damageSources().magic(), 7.5F);
            victim.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 140, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
    }

    private void executeSynapticStorm(ServerLevel serverLevel) {
        serverLevel.sendParticles(ModParticles.TIDE_MOTE.get(), this.getX(), this.getY(0.5), this.getZ(),
                42, 3.8, 0.7, 3.8, 0.06);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.APEX_MATRIARCH_WAIL.get(), SoundSource.HOSTILE, 1.55F, 0.88F);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 1.35F, 0.62F);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(7.6D),
                e -> !(e instanceof MycelialMob) && e.isAlive())) {
            victim.hurt(this.damageSources().magic(), 8.5F);
            victim.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 160, 1));
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (BlockPos sentinel : phaseTwoSentinelPositions) {
                if (serverLevel.getBlockState(sentinel).is(ModBlocks.SENTINEL_NODE.get())
                        && serverLevel.getBlockEntity(sentinel) instanceof SynapticNodeBlockEntity node
                        && node.isBossAnchorFor(this.getUUID())) {
                    serverLevel.destroyBlock(sentinel, false);
                }
            }
        }
        super.die(damageSource);
        if (this.level() instanceof ServerLevel serverLevel) {
            onMatriarchDefeated(serverLevel, this.blockPosition());
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (recentlyHit) {
            this.spawnAtLocation(com.alex.mycelialmind.item.FieldEngramItem.createStack(
                    com.alex.mycelialmind.hive.FieldEngramId.MATRIARCH_REQUIEM));
        }
    }

    public static void onMatriarchDefeated(ServerLevel sourceLevel, BlockPos pos) {
        onMatriarchDefeated(sourceLevel, pos, true);
    }

    public static void onMatriarchDefeated(ServerLevel sourceLevel, BlockPos pos, boolean notifyPlayers) {
        long now = sourceLevel.getGameTime();
        var server = sourceLevel.getServer();
        HiveMindSavedData.get(sourceLevel).recordMatriarchDefeat(now);
        ServerLevel overworld = server.overworld();
        if (overworld != sourceLevel) {
            HiveMindSavedData.get(overworld).recordMatriarchDefeat(overworld.getGameTime());
        }
        ServerLevel deepLevel = server.getLevel(ModDimensions.DEEP_MYCELIUM_LEVEL);
        if (deepLevel != null && deepLevel != sourceLevel) {
            HiveMindSavedData.get(deepLevel).recordMatriarchDefeat(deepLevel.getGameTime());
        }
        if (DimensionalHomeostasisDirector.isHomeostasisDimension(sourceLevel)) {
            DimensionalHomeostasisDirector.sedateArea(sourceLevel, pos, 2, 60.0, 3, null);
        }
        com.alex.mycelialmind.hive.HiveCycleDirector.onMatriarchDefeated(sourceLevel, pos);
        if (notifyPlayers) {
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                if (sp.level() instanceof ServerLevel spLevel) {
                    PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(spLevel, sp));
                    com.alex.mycelialmind.network.DiscoverySyncPayload.syncToPlayer(sp);
                    com.alex.mycelialmind.network.PostMatriarchExpeditionSyncPayload.syncToPlayer(sp);
                }
                sp.sendSystemMessage(Component.translatable("message.mycelialmind.matriarch_defeated_remission")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                sp.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_unlocked")
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private void fireSporeBarrage(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.4) - this.getY(0.6);
        double dz = target.getZ() - this.getZ();
        float[] offsets = {-0.30F, -0.15F, 0.0F, 0.15F, 0.30F};
        for (float off : offsets) {
            AcidSpitProjectile spit = new AcidSpitProjectile(this.level(), this);
            double cos = Math.cos(off);
            double sin = Math.sin(off);
            spit.shoot(dx * cos - dz * sin, dy, dx * sin + dz * cos, 1.5F, 1.5F);
            this.level().addFreshEntity(spit);
        }
        this.playSound(ModSounds.APEX_MATRIARCH_BARRAGE.get(), 1.55F, 0.95F + this.random.nextFloat() * 0.10F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MatriarchPhase", getMatriarchPhase());
        tag.putBoolean("Phase2Triggered", phase2Triggered);
        ListTag sentinels = new ListTag();
        for (BlockPos pos : phaseTwoSentinelPositions) {
            sentinels.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_PHASE_TWO_SENTINELS, sentinels);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MatriarchPhase")) {
            setMatriarchPhase(tag.getInt("MatriarchPhase"));
        }
        this.phase2Triggered = tag.getBoolean("Phase2Triggered");
        phaseTwoSentinelPositions.clear();
        ListTag sentinels = tag.getList(TAG_PHASE_TWO_SENTINELS, Tag.TAG_LONG);
        for (int i = 0; i < sentinels.size(); i++) {
            phaseTwoSentinelPositions.add(BlockPos.of(((LongTag) sentinels.get(i)).getAsLong()));
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
        return 1.80F;
    }

    @Override
    protected float getStepSoundVolume() {
        return 0.78F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 105;
    }

    @Override
    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_COLOSSAL.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.APEX_MATRIARCH_WAIL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.APEX_MATRIARCH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.APEX_MATRIARCH_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 44.0D);
    }
}
