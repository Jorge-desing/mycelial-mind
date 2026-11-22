package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.DamageTelemetryTracker;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
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
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Final Boss: The Hive Core.
 * Phase 1 (100-65% HP): Synaptic Command — 3-shot acid fan & tactical summons.
 * Phase 2 (65-30% HP): Armored Nexus — spawns 3 Sentinel Node anchors granting invulnerability until destroyed.
 * Phase 3 (<30% HP): Hive Overload — rotating Tier 2 + Synergy mutations & telegraphed spore shockwaves.
 */
public class HiveCoreBossEntity extends MycelialMob {
    private static final EntityDataAccessor<Integer> DATA_BOSS_PHASE =
            SynchedEntityData.defineId(HiveCoreBossEntity.class, EntityDataSerializers.INT);

    private static final String TAG_PHASE = "BossPhase";
    private static final String TAG_PHASE2_DONE = "Phase2ShieldBroken";
    private static final String TAG_ANCHORS = "Phase2Anchors";

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
    private int stunTicks = 0;
    private int shockwaveChargeTicks = 0;
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
        positionBossPart(this, this.weakPointPart, 0.0D, 3.20D, 0.0D);
        positionBossPart(this, this.carapacePart, 0.85D, 0.30D, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BOSS_PHASE, 1);
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
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.9D, false));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float hpRatio = this.getHealth() / this.getMaxHealth();
        int currentPhase = getBossPhase();

        if (currentPhase == 1 && hpRatio <= 0.65F) {
            enterPhaseTwo(serverLevel);
        } else if (currentPhase == 2) {
            int remaining = countRemainingAnchors(serverLevel);
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
            if (hpRatio <= 0.30F && phase2ShieldBroken) {
                enterPhaseThree(serverLevel);
            }
        }

        if (stunTicks > 0) {
            stunTicks--;
            return;
        }

        // Symbiotic node healing (Phase 1 & 3)
        if (this.tickCount % 20 == 0 && !isShieldedPhaseTwo() && hasNearbySymbioticNode(serverLevel)) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1));
            this.heal(1.0F);
            serverLevel.sendParticles(ModParticles.HIVE_EMBER.get(), this.getX(), this.getY(0.5), this.getZ(),
                    8, 0.8, 0.8, 0.8, 0.02);
        }

        // Phase-specific attacks
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            if (getBossPhase() == 1 && this.tickCount % 90 == 0 && this.distanceToSqr(target) <= 576.0D) {
                fireAcidFan(target);
            }
            if (getBossPhase() == 3) {
                handlePhaseThreeBehavior(serverLevel, target);
            }
        }

        // Summon reinforcements
        if (this.tickCount % 160 == 0 && target != null) {
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
        this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
        this.bossEvent.setName(Component.translatable("entity.mycelialmind.hive_core.phase2"));
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
        stunTicks = 60;
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
        this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        this.bossEvent.setName(Component.translatable("entity.mycelialmind.hive_core"));
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_SHIELD.get(), SoundSource.HOSTILE, 1.60F, 0.65F);
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_HURT.get(), SoundSource.HOSTILE, 1.45F, 0.78F);
        level.sendParticles(ModParticles.CHITIN_SHARD.get(), this.getX(), this.getY(0.5), this.getZ(), 6, 1.0, 1.0, 1.0, 0.05);
        if (this.getHealth() / this.getMaxHealth() <= 0.30F) {
            enterPhaseThree(level);
        }
    }

    private void enterPhaseThree(ServerLevel level) {
        setBossPhase(3);
        this.stunTicks = 0;
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.bossEvent.setName(Component.translatable("entity.mycelialmind.hive_core.phase3"));
        level.playSound(null, this.blockPosition(), ModSounds.BOSS_PHASE_SHIFT.get(), SoundSource.HOSTILE, 1.80F, 0.78F);
        level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_RESONANCE.get(), SoundSource.HOSTILE, 1.60F, 0.72F);
        rotateOverloadSynergy();
    }

    private void handlePhaseThreeBehavior(ServerLevel level, LivingEntity target) {
        if (this.tickCount % 240 == 0) {
            rotateOverloadSynergy();
            level.playSound(null, this.blockPosition(), ModSounds.HIVE_MUTATE_TIER2.get(), SoundSource.HOSTILE, 1.0F, 1.15F);
        }

        if (shockwaveChargeTicks > 0) {
            shockwaveChargeTicks--;
            level.sendParticles(ModParticles.NECROTIC_MIST.get(), this.getX(), this.getY() + 0.4, this.getZ(),
                    16, 2.5, 0.3, 2.5, 0.02);
            if (shockwaveChargeTicks == 0) {
                detonateSporeShockwave(level);
            }
        } else if (this.tickCount % 140 == 0 && this.distanceToSqr(target) <= 100.0D) {
            shockwaveChargeTicks = 30;
            level.playSound(null, this.blockPosition(), ModSounds.HIVE_CORE_RESONANCE.get(), SoundSource.HOSTILE, 1.45F, 1.32F);
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
            entity.hurt(this.damageSources().magic(), 8.0F);
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }
    }

    private void fireAcidFan(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.4) - this.getY(0.6);
        double dz = target.getZ() - this.getZ();
        float[] yawOffsets = {-0.18F, 0.0F, 0.18F};
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
