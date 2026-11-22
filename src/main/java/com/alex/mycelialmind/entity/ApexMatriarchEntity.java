package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.ModDimensions;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Dimensional Apex Boss of the Deep Mycelium: The Apex Matriarch (600 HP).
 * Phase 1 (100-60%): 5-shot infectious acid fan + short tactical teleport.
 * Phase 2 (60-25%): Summons elite Warden/Weaver squads + 2 Sentinel Nodes.
 * Phase 3 (<25%): Overload frenzy with AoE Mycelial Infection pulses & synergies.
 */
public class ApexMatriarchEntity extends MycelialMob {
    private static final String TAG_PHASE_TWO_SENTINELS = "Phase2Sentinels";

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(ApexMatriarchEntity.class, EntityDataSerializers.INT);

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
        positionBossPart(this, this.weakPointPart, 0.0D, 4.10D, 0.0D);
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
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05D, false));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 20.0F));

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
        if (getMatriarchPhase() == 1 && hpRatio <= 0.60F) {
            enterPhaseTwo(serverLevel);
        } else if (getMatriarchPhase() == 2 && hpRatio <= 0.25F) {
            enterPhaseThree(serverLevel);
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.tickCount % 80 == 0 && this.distanceToSqr(target) <= 784.0D) {
            fireSporeBarrage(target);
        }

        if (getMatriarchPhase() >= 1 && this.tickCount % 140 == 0 && this.distanceToSqr(target) > 144.0D) {
            double tx = target.getX() + (this.random.nextDouble() - 0.5) * 6.0;
            double tz = target.getZ() + (this.random.nextDouble() - 0.5) * 6.0;
            this.teleportTo(tx, target.getY(), tz);
            serverLevel.sendParticles(ModParticles.TIDE_MOTE.get(), this.getX(), this.getY(0.5), this.getZ(),
                    20, 0.5, 0.8, 0.5, 0.05);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.SYMBIOTIC_NODE_TELEPORT.get(), SoundSource.HOSTILE, 1.40F, 0.68F);
        }

        if (getMatriarchPhase() == 3 && this.tickCount % 100 == 0) {
            serverLevel.sendParticles(ModParticles.TIDE_MOTE.get(), this.getX(), this.getY(0.5), this.getZ(),
                    35, 3.5, 0.6, 3.5, 0.05);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.APEX_MATRIARCH_WAIL.get(), SoundSource.HOSTILE, 1.55F, 0.88F);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 1.30F, 0.62F);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(7.6D),
                    e -> !(e instanceof MycelialMob) && e.isAlive())) {
                victim.hurt(this.damageSources().magic(), 6.0F);
                victim.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 160, 1));
            }
        }
    }

    private void enterPhaseTwo(ServerLevel level) {
        setMatriarchPhase(2);
        this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        this.bossEvent.setName(Component.translatable("entity.mycelialmind.apex_matriarch.phase2"));
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
        BlockPos candidate = findOpenSentinelPosition(level, preferred);
        if (candidate != null && level.setBlockAndUpdate(candidate, ModBlocks.SENTINEL_NODE.get().defaultBlockState())) {
            if (level.getBlockEntity(candidate) instanceof SynapticNodeBlockEntity node) {
                node.markBossAnchor(this.getUUID());
                phaseTwoSentinelPositions.add(candidate.immutable());
            } else {
                level.destroyBlock(candidate, false);
            }
        }
    }

    private static BlockPos findOpenSentinelPosition(ServerLevel level, BlockPos preferred) {
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

    private void enterPhaseThree(ServerLevel level) {
        setMatriarchPhase(3);
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.bossEvent.setName(Component.translatable("entity.mycelialmind.apex_matriarch.phase3"));
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
        if (notifyPlayers) {
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                if (sp.level() instanceof ServerLevel spLevel) {
                    PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(spLevel, sp));
                    com.alex.mycelialmind.network.DiscoverySyncPayload.syncToPlayer(sp);
                }
                sp.sendSystemMessage(Component.translatable("message.mycelialmind.matriarch_defeated_remission")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
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
