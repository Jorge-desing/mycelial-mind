package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.util.MycelialParticles;
import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.ai.brain.btree.HiveBehaviorTree;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrain;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainFactory;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainOwner;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainRuntime;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainScheduler;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.mojang.serialization.Dynamic;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.entity.ai.HiveAiBlackboard;
import com.alex.mycelialmind.entity.ai.HiveTacticalTree;
import com.alex.mycelialmind.entity.ai.ImminentHazardEvadeGoal;
import com.alex.mycelialmind.entity.ai.TacticalShelterGoal;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine;
import com.alex.mycelialmind.hive.MobEpisodicMemory;
import com.alex.mycelialmind.hive.MobThreatTable;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.hive.TacticalEvasionEngine;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.ModDimensions;
import com.alex.mycelialmind.world.MycelialLootSystem;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for all adaptive Mycelial Mind organisms.
 * Stores bitmasks of inherited Tier 1 & Tier 2 AdaptationTraits, HiveSynergies,
 * and EvolutionRank (BASE -> MUTATED -> ELITE) synced to clients.
 */
public abstract class MycelialMob extends Monster implements HiveBrainOwner<MycelialMob> {
    private static final EntityDataAccessor<Integer> DATA_TRAIT_MASK =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TIER2_MASK =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SYNERGY_MASK =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EVOLUTION_RANK =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PROMOTION_TICK =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HIVE_STRAIN =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SCHISM_TICKS =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DESYNC_TICKS =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FRENZY_TICKS =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHALANX_ACTIVE =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_COMMAND_ROLE =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_PRIMORDIAL_VARIANT =
            SynchedEntityData.defineId(MycelialMob.class, EntityDataSerializers.BOOLEAN);

    private static final ResourceLocation PRIMORDIAL_HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "primordial_variant_health");

    private static final ResourceLocation FRENZY_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "swarm_frenzy_speed");
    private static final ResourceLocation THREAT_HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "threat_tier_health");
    private static final ResourceLocation THREAT_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "threat_tier_damage");
    private static final ResourceLocation THREAT_ARMOR_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "threat_tier_armor");
    private static final ResourceLocation THREAT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "threat_tier_speed");
    private static final ResourceLocation BLAST_SHIELDED_ARMOR_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "trait_blast_shielded_armor");
    private static final ResourceLocation SIEGE_JUGGERNAUT_KNOCKBACK_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "synergy_siege_juggernaut_knockback");

    public static final int PROMOTION_SCALE_LERP_TICKS = 20;
    public static final int PROMOTION_FLASH_TICKS = 30;

    private static final String TAG_TRAIT_MASK = "MycelialTraitMask";
    private static final String TAG_TIER2_MASK = "MycelialTier2Mask";
    private static final String TAG_SYNERGY_MASK = "MycelialSynergyMask";
    private static final String TAG_HIVE_TRAIT_REVISION = "MycelialHiveTraitRevision";
    private static final String TAG_HIVE_TRAIT_DIMENSION = "MycelialHiveTraitDimension";
    private static final String TAG_EVOLUTION_RANK = "MycelialEvolutionRank";
    private static final String TAG_NATURAL_SPAWN = "MycelialNaturalSpawn";
    private static final String TAG_HIVE_STRAIN = "MycelialHiveStrain";
    private static final String TAG_SCHISM_TICKS = "MycelialSchismTicks";
    private static final String TAG_DESYNC_TICKS = "MycelialDesyncTicks";
    private static final String TAG_FRENZY_TICKS = "MycelialFrenzyTicks";
    private static final String TAG_ESCORT_EXPIRY = "MycelialEscortExpiry";
    private static final String TAG_ESCORT_GLOBAL_CLOCK = "MycelialEscortGlobalClock";
    private static final String TAG_ALLIED_OWNER = "MycelialAlliedOwner";
    private static final String TAG_TRAIT_ATTRIBUTE_VERSION = "MycelialTraitAttributeVersion";
    private static final String TAG_BASE_MAX_HEALTH = "MycelialBaseMaxHealth";
    private static final String TAG_BASE_ATTACK_DAMAGE = "MycelialBaseAttackDamage";
    private static final String TAG_BASE_XP_REWARD = "MycelialBaseXpReward";
    private static final String TAG_PRIMORDIAL = "MycelialPrimordial";
    private static final String TAG_EPISODIC_MEMORY = "MycelialEpisodicMemory";

    public static final double SURFACE_DAY_SPAWN_THREAT_THRESHOLD = 50.0;

    private double baseMaxHealth = -1.0;
    private double baseAttackDamage = -1.0;
    private int baseXpReward = -1;
    private float scaleFrom = 1.0F;
    private float scaleTo = 1.0F;
    private int scaleLerpRemainingTicks = 0;
    private boolean naturalSpawn = false;
    private long appliedHiveTraitStateRevision = Long.MIN_VALUE;
    private String appliedHiveTraitDimension = "";
    private long escortExpiryTick = -1L;
    @Nullable
    private UUID alliedOwnerUuid = null;
    private final HiveAiBlackboard aiBlackboard = new HiveAiBlackboard();
    private final MobThreatTable threatTable = new MobThreatTable();
    private final MobEpisodicMemory episodicMemory = new MobEpisodicMemory();
    private int nextTacticalTreeTick = 0;
    private int lastDesyncTicks = 0;
    @Nullable
    private HiveBehaviorTree<HiveAiBlackboard> tacticalTree;
    @Nullable
    private HiveBrainRuntime<MycelialMob> hiveBrainRuntime;

    protected MycelialMob(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        if (!level.isClientSide()) {
            this.tacticalTree = HiveTacticalTree.create(this.aiBlackboard);
            if (!(this instanceof AcidSpitterEntity)) {
                this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                        this, MycelialMob.class, 10, true, false, this::isRivalStrainTarget));
            }
            if (!(this instanceof MycelialBossMob) && !isBossMob() && !(this instanceof AcidSpitterEntity)) {
                this.goalSelector.addGoal(1, new ImminentHazardEvadeGoal(this));
                if (TacticalShelterGoal.isEligibleMob(this)) {
                    this.goalSelector.addGoal(2, new TacticalShelterGoal(this));
                }
            }
            captureBaseStats();
        }
    }

    public HiveAiBlackboard getAiBlackboard() {
        return aiBlackboard;
    }

    public MobThreatTable getThreatTable() {
        return threatTable;
    }

    public MobEpisodicMemory getEpisodicMemory() {
        return episodicMemory;
    }

    public HiveAiBlackboard.TacticalIntent getTacticalIntent() {
        return aiBlackboard.activeIntent;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TRAIT_MASK, 0);
        builder.define(DATA_TIER2_MASK, 0);
        builder.define(DATA_SYNERGY_MASK, 0);
        builder.define(DATA_EVOLUTION_RANK, EvolutionRank.BASE.id());
        builder.define(DATA_PROMOTION_TICK, -1);
        builder.define(DATA_HIVE_STRAIN, HiveStrain.ORTHODOX.id());
        builder.define(DATA_SCHISM_TICKS, 0);
        builder.define(DATA_DESYNC_TICKS, 0);
        builder.define(DATA_FRENZY_TICKS, 0);
        builder.define(DATA_PHALANX_ACTIVE, false);
        builder.define(DATA_COMMAND_ROLE, SynapticCommandSystem.CommandRole.NONE.id());
        builder.define(DATA_PRIMORDIAL_VARIANT, false);
    }

    public boolean isPrimordialVariant() {
        return this.entityData.get(DATA_PRIMORDIAL_VARIANT);
    }

    public void setPrimordialVariant(boolean primordial) {
        boolean prev = isPrimordialVariant();
        this.entityData.set(DATA_PRIMORDIAL_VARIANT, primordial);
        AttributeInstance maxHp = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.removeModifier(PRIMORDIAL_HEALTH_MODIFIER_ID);
            if (primordial) {
                maxHp.addPermanentModifier(new AttributeModifier(
                        PRIMORDIAL_HEALTH_MODIFIER_ID, 0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                if (!prev) {
                    this.setHealth(this.getMaxHealth());
                }
            } else if (this.getHealth() > this.getMaxHealth()) {
                this.setHealth(this.getMaxHealth());
            }
        }
    }

    public int getDesyncTicks() {
        return this.entityData.get(DATA_DESYNC_TICKS);
    }

    public void setDesyncTicks(int ticks) {
        this.entityData.set(DATA_DESYNC_TICKS, Math.max(0, ticks));
    }

    public boolean isDesynced() {
        return getDesyncTicks() > 0;
    }

    public void triggerDesync(int ticks) {
        if (isBossMob() || ticks <= 0) {
            return;
        }
        setDesyncTicks(Math.max(getDesyncTicks(), ticks));
        setFrenzyTicks(0);
        setPhalanxActive(false);
        setCommandRole(SynapticCommandSystem.CommandRole.NONE);
    }

    public int getFrenzyTicks() {
        return this.entityData.get(DATA_FRENZY_TICKS);
    }

    public void setFrenzyTicks(int ticks) {
        this.entityData.set(DATA_FRENZY_TICKS, Math.max(0, ticks));
    }

    public boolean isFrenzyActive() {
        return getFrenzyTicks() > 0 && !isDesynced();
    }

    public void triggerFrenzy(int ticks) {
        if (isBossMob() || isDesynced() || ticks <= 0) {
            return;
        }
        boolean wasActive = isFrenzyActive();
        setFrenzyTicks(Math.max(getFrenzyTicks(), ticks));
        if (!wasActive && this.level() instanceof ServerLevel serverLevel) {
            float pitch = computeTacticalVoicePitchMultiplier(
                    getEvolutionRank(), getHiveStrain(), true, false, getCommandRole(), false);
            serverLevel.playSound(
                    null, this.blockPosition(), ModSounds.SWARM_FLANK_CALL.get(),
                    SoundSource.HOSTILE, 0.72F, pitch
            );
        }
    }

    public boolean isPhalanxActive() {
        return this.entityData.get(DATA_PHALANX_ACTIVE) && !isDesynced();
    }

    public void setPhalanxActive(boolean active) {
        this.entityData.set(DATA_PHALANX_ACTIVE, active);
    }

    public SynapticCommandSystem.CommandRole getCommandRole() {
        return SynapticCommandSystem.CommandRole.byId(this.entityData.get(DATA_COMMAND_ROLE));
    }

    public void setCommandRole(SynapticCommandSystem.CommandRole role) {
        this.entityData.set(DATA_COMMAND_ROLE, (role != null ? role : SynapticCommandSystem.CommandRole.NONE).id());
    }

    public HiveStrain getHiveStrain() {
        return HiveStrain.byId(this.entityData.get(DATA_HIVE_STRAIN));
    }

    public void setHiveStrain(HiveStrain strain) {
        this.entityData.set(DATA_HIVE_STRAIN, (strain != null ? strain : HiveStrain.ORTHODOX).id());
    }

    public int getSchismTicks() {
        return this.entityData.get(DATA_SCHISM_TICKS);
    }

    public void setSchismTicks(int ticks) {
        this.entityData.set(DATA_SCHISM_TICKS, Math.max(0, ticks));
    }

    public boolean isSchismActive() {
        return getSchismTicks() > 0;
    }

    public void infectWithSchism(HiveStrain strain, int durationTicks) {
        if (isBossMob()) {
            return;
        }
        setHiveStrain(strain);
        setSchismTicks(Math.max(getSchismTicks(), durationTicks));
    }

    public void setAlliedEscort(@Nullable UUID ownerUuid, long expiryTick) {
        this.alliedOwnerUuid = ownerUuid;
        long remainingTicks = Math.max(0L, expiryTick - this.level().getGameTime());
        this.escortExpiryTick = getEscortClock() + remainingTicks;
    }

    public boolean isAlliedEscort() {
        return this.alliedOwnerUuid != null && this.escortExpiryTick > 0L;
    }

    public boolean isAlliedEscortActive() {
        return isAlliedEscort() && getEscortClock() < this.escortExpiryTick;
    }

    public void registerAlliedEscort() {
        if (isAlliedEscortActive() && this.level() instanceof ServerLevel serverLevel) {
            AlliedEscortSavedData.get(serverLevel.getServer())
                    .register(this.getUUID(), this.alliedOwnerUuid, this.escortExpiryTick);
        }
    }

    private long getEscortClock() {
        return this.level() instanceof ServerLevel serverLevel
                ? serverLevel.getServer().overworld().getGameTime()
                : this.level().getGameTime();
    }

    @Nullable
    public UUID getAlliedOwnerUuid() {
        return alliedOwnerUuid;
    }

    public boolean isBossMob() {
        return this instanceof HiveCoreBossEntity || this instanceof ApexMatriarchEntity;
    }

    public boolean isRivalStrainTarget(LivingEntity candidate) {
        if (!(candidate instanceof MycelialMob other) || other == this || isBossMob() || other.isBossMob()) {
            return false;
        }
        if (this.isAlliedEscort()) {
            if (other.isAlliedEscort() && this.alliedOwnerUuid != null && this.alliedOwnerUuid.equals(other.alliedOwnerUuid)) {
                return false;
            }
            return this.getHiveStrain() != other.getHiveStrain() || !other.isAlliedEscort();
        }
        if (other.isAlliedEscort()) {
            return this.getHiveStrain() != other.getHiveStrain();
        }
        if (this.isSchismActive() || other.isSchismActive()) {
            return this.getHiveStrain() != other.getHiveStrain();
        }
        return this.getHiveStrain().isSchismStrain()
                && other.getHiveStrain().isSchismStrain()
                && this.getHiveStrain() != other.getHiveStrain();
    }

    public static boolean isFriendlyToPlayer(MycelialMob mob, @Nullable Player player) {
        if (mob.isAlliedEscort()) {
            return player == null || player.getUUID().equals(mob.getAlliedOwnerUuid());
        }
        if (mob instanceof SynapticMimicEntity mimic
                && mimic.isSpoofingAllianceFor(player)
                && (player == null || !player.hasEffect(ModEffects.SYNAPTIC_VISION))) {
            return true;
        }
        if (player == null || mob.isBossMob()) {
            return false;
        }
        if (mob.getTarget() == player || mob.getLastHurtByMob() == player) {
            return false;
        }
        PlayerDiplomacyData diplomacy = player.getData(ModAttachments.DIPLOMACY);
        return diplomacy.isProtectedFrom(mob.getHiveStrain(), mob.getEvolutionRank(), false, mob.distanceToSqr(player));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.isAlliedEscort() && this.alliedOwnerUuid != null) {
            if (target instanceof MycelialChimeraEntity chimera && this.alliedOwnerUuid.equals(chimera.getOwnerUUID())) {
                return false;
            }
            if (target instanceof PurifierDroneEntity drone && this.alliedOwnerUuid.equals(drone.getOwnerUuid())) {
                return false;
            }
        }
        if (target instanceof Player player) {
            if (this.isAlliedEscort() && player.getUUID().equals(this.alliedOwnerUuid)) {
                return false;
            }
            if (!isBossMob()) {
                var diplomacy = player.getData(ModAttachments.DIPLOMACY);
                if (diplomacy.isProtectedFrom(getHiveStrain(), getEvolutionRank(), false, this.distanceToSqr(player))
                        && this.getLastHurtByMob() != player) {
                    return false;
                }
            }
        }
        return super.canAttack(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.level() instanceof ServerLevel serverLevel
                && source.getEntity() instanceof LivingEntity attacker
                && attacker != this
                && this.canAttack(attacker)
                && !(attacker instanceof Player player && isFriendlyToPlayer(this, player))) {
            float healthRatio = attacker.getHealth() / Math.max(1.0F, attacker.getMaxHealth());
            long now = serverLevel.getGameTime();
            this.threatTable.recordDamage(attacker.getUUID(), amount, this.distanceToSqr(attacker), healthRatio, now);
            if (amount >= this.getMaxHealth() * 0.25F || this.getHealth() <= this.getMaxHealth() * 0.35F) {
                this.episodicMemory.remember(MobEpisodicMemory.grudgeKey(attacker.getUUID()), now);
            }
        }
        return hurt;
    }

    public EvolutionRank getEvolutionRank() {
        return EvolutionRank.fromId(this.entityData.get(DATA_EVOLUTION_RANK));
    }

    public int getPromotionTick() {
        return this.entityData.get(DATA_PROMOTION_TICK);
    }

    public float getPromotionFlashProgress(float partialTick) {
        int promoTick = getPromotionTick();
        if (promoTick < 0) {
            return 0.0F;
        }
        float elapsed = (this.level().getGameTime() - promoTick) + partialTick;
        if (elapsed < 0.0F || elapsed >= PROMOTION_FLASH_TICKS) {
            return 0.0F;
        }
        return 1.0F - (elapsed / (float) PROMOTION_FLASH_TICKS);
    }

    public void promoteRank(EvolutionRank newRank) {
        promoteRank(newRank, true);
    }

    private void promoteRank(EvolutionRank newRank, boolean allowTransitionEffects) {
        EvolutionRank previousRank = getEvolutionRank();
        if (newRank == null || newRank.id() <= previousRank.id()) {
            return;
        }
        captureBaseStats();
        this.entityData.set(DATA_EVOLUTION_RANK, newRank.id());

        AttributeInstance maxHp = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null && baseMaxHealth > 0) {
            maxHp.setBaseValue(baseMaxHealth * newRank.healthMultiplier());
            this.setHealth(this.getMaxHealth());
        }
        AttributeInstance atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && baseAttackDamage > 0) {
            atk.setBaseValue(baseAttackDamage * newRank.damageMultiplier());
        }
        AttributeInstance scale = this.getAttribute(Attributes.SCALE);
        float currentScale = scale != null ? (float) scale.getBaseValue() : previousRank.scale();
        if (allowTransitionEffects && newRank.id() > previousRank.id() && this.level() instanceof ServerLevel serverLevel) {
            this.scaleFrom = currentScale;
            this.scaleTo = newRank.scale();
            this.scaleLerpRemainingTicks = PROMOTION_SCALE_LERP_TICKS;
            this.entityData.set(DATA_PROMOTION_TICK, (int) serverLevel.getGameTime());

            MycelialParticles.promotion(serverLevel, this, newRank == EvolutionRank.ELITE);
            if (newRank == EvolutionRank.ELITE) {
                serverLevel.playSound(null, this.blockPosition(), ModSounds.MOB_PROMOTE_ELITE.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            } else {
                serverLevel.playSound(null, this.blockPosition(), ModSounds.MOB_PROMOTE_MUTATED.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            }
        } else if (scale != null) {
            this.scaleLerpRemainingTicks = 0;
            scale.setBaseValue(newRank.scale());
        }
        if (baseXpReward > 0) {
            this.xpReward = Math.max(1, (int) Math.round(baseXpReward * newRank.xpMultiplier()));
        }
    }

    private void captureBaseStats() {
        if (baseMaxHealth < 0) {
            AttributeInstance maxHp = this.getAttribute(Attributes.MAX_HEALTH);
            baseMaxHealth = maxHp != null ? maxHp.getBaseValue() : 20.0D;
        }
        if (baseAttackDamage < 0) {
            AttributeInstance atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
            baseAttackDamage = atk != null ? atk.getBaseValue() : 4.0D;
        }
        if (baseXpReward < 0) {
            baseXpReward = Math.max(1, this.xpReward);
        }
    }

    public int getTraitMask() {
        return this.entityData.get(DATA_TRAIT_MASK);
    }

    public void setTraitMask(int mask) {
        this.entityData.set(DATA_TRAIT_MASK, mask);
        onTraitsUpdated();
    }

    public int getTier2Mask() {
        return this.entityData.get(DATA_TIER2_MASK);
    }

    public void setTier2Mask(int mask) {
        this.entityData.set(DATA_TIER2_MASK, mask);
    }

    public int getSynergyMask() {
        return this.entityData.get(DATA_SYNERGY_MASK);
    }

    public void setSynergyMask(int mask) {
        this.entityData.set(DATA_SYNERGY_MASK, mask);
        onTraitsUpdated();
    }

    public void clearAdaptations() {
        if (isBossMob()) {
            return;
        }
        this.entityData.set(DATA_TRAIT_MASK, 0);
        this.entityData.set(DATA_TIER2_MASK, 0);
        this.entityData.set(DATA_SYNERGY_MASK, 0);
        onTraitsUpdated();
        refreshDynamicAiGoals();
    }

    public boolean hasCurrentGlobalHiveState(ServerLevel serverLevel, HiveMindSavedData hive) {
        return !isBossMob()
                && appliedHiveTraitStateRevision == hive.getActiveTraitStateRevision()
                && appliedHiveTraitDimension.equals(serverLevel.dimension().location().toString());
    }

    public void applyGlobalHiveState(ServerLevel serverLevel, HiveMindSavedData hive) {
        if (isBossMob()) {
            return;
        }
        applyHiveState(hive.getActiveTraits(), hive.getTier2Traits(), hive.getActiveSynergies());
        appliedHiveTraitStateRevision = hive.getActiveTraitStateRevision();
        appliedHiveTraitDimension = serverLevel.dimension().location().toString();
    }

    public boolean hasTrait(AdaptationTrait trait) {
        return (getTraitMask() & (1 << trait.ordinal())) != 0;
    }

    public boolean hasTier2Trait(AdaptationTrait trait) {
        return (getTier2Mask() & (1 << trait.ordinal())) != 0;
    }

    public boolean hasSynergy(HiveSynergy synergy) {
        return (getSynergyMask() & (1 << synergy.ordinal())) != 0;
    }

    public Set<AdaptationTrait> getTraits() {
        return decodeTraits(getTraitMask());
    }

    public Set<AdaptationTrait> getTier2Traits() {
        return decodeTraits(getTier2Mask());
    }

    public Set<HiveSynergy> getSynergies() {
        EnumSet<HiveSynergy> synergies = EnumSet.noneOf(HiveSynergy.class);
        int mask = getSynergyMask();
        for (HiveSynergy synergy : HiveSynergy.values()) {
            if ((mask & (1 << synergy.ordinal())) != 0) {
                synergies.add(synergy);
            }
        }
        return synergies;
    }

    private static Set<AdaptationTrait> decodeTraits(int mask) {
        EnumSet<AdaptationTrait> traits = EnumSet.noneOf(AdaptationTrait.class);
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            if ((mask & (1 << trait.ordinal())) != 0) {
                traits.add(trait);
            }
        }
        return traits;
    }

    public void applyHiveTraits(Set<AdaptationTrait> traits) {
        applyHiveState(traits, EnumSet.noneOf(AdaptationTrait.class), EnumSet.noneOf(HiveSynergy.class));
    }

    public void applyHiveState(
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies
    ) {
        int mask = 0;
        for (AdaptationTrait trait : traits) {
            mask |= (1 << trait.ordinal());
        }
        int t2Mask = 0;
        for (AdaptationTrait trait : tier2) {
            t2Mask |= (1 << trait.ordinal());
        }
        int synMask = 0;
        for (HiveSynergy synergy : synergies) {
            synMask |= (1 << synergy.ordinal());
        }
        this.entityData.set(DATA_TRAIT_MASK, mask);
        setTier2Mask(t2Mask);
        this.entityData.set(DATA_SYNERGY_MASK, synMask);
        onTraitsUpdated();
        refreshDynamicAiGoals();
    }

    protected void refreshDynamicAiGoals() {
        // Subclasses and dynamic goal controllers can react to runtime mutation shifts here.
    }

    protected void onTraitsUpdated() {
        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(BLAST_SHIELDED_ARMOR_MODIFIER_ID);
            if (hasTrait(AdaptationTrait.BLAST_SHIELDED)) {
                double bonus = Math.max(0.0D, 8.0D - armor.getBaseValue());
                if (bonus > 0.0D) {
                    armor.addTransientModifier(new AttributeModifier(
                            BLAST_SHIELDED_ARMOR_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }
        AttributeInstance kbRes = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbRes != null) {
            kbRes.removeModifier(SIEGE_JUGGERNAUT_KNOCKBACK_MODIFIER_ID);
            if (hasSynergy(HiveSynergy.SIEGE_JUGGERNAUT)) {
                double bonus = Math.max(0.0D, 0.75D - kbRes.getBaseValue());
                if (bonus > 0.0D) {
                    kbRes.addTransientModifier(new AttributeModifier(
                            SIEGE_JUGGERNAUT_KNOCKBACK_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.scaleLerpRemainingTicks > 0) {
            this.scaleLerpRemainingTicks--;
            float t = 1.0F - ((float) this.scaleLerpRemainingTicks / (float) PROMOTION_SCALE_LERP_TICKS);
            float eased = 1.0F - (float) Math.pow(1.0F - t, 3);
            float value = this.scaleLerpRemainingTicks == 0 ? this.scaleTo : Mth.lerp(eased, this.scaleFrom, this.scaleTo);
            AttributeInstance scale = this.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(value);
            }
        }
        if (!this.level().isClientSide()) {
            int schism = getSchismTicks();
            if (schism > 0) {
                setSchismTicks(schism - 1);
            }
            if (this.escortExpiryTick > 0L && getEscortClock() >= this.escortExpiryTick) {
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ModParticles.SPORE_CLOUD.get(), this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.25, 0.25, 0.25, 0.03);
                }
                this.discard();
                return;
            }
        }
        if (getEvolutionRank() == EvolutionRank.ELITE && this.tickCount % 12 == 0) {
            if (this.level().isClientSide()) {
                this.level().addParticle(ModParticles.NECROTIC_MIST.get(),
                        this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        0.0, 0.02, 0.0);
            }
        }
        HiveStrain strain = getHiveStrain();
        if (strain.isSchismStrain() && this.tickCount % 10 == 0 && this.level().isClientSide()) {
            this.level().addParticle(
                    ColorParticleOption.create(ModParticles.HIVE_DUST.get(), strain.argbColor()),
                    this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                    this.getY() + this.getBbHeight() * 0.65,
                    this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                    0.0, 0.015, 0.0
            );
        }
        if (isPrimordialVariant()) {
            if (this.level().isClientSide() && this.tickCount % 8 == 0) {
                this.level().addParticle(
                        this.random.nextBoolean() ? ModParticles.BIO_LUME.get() : ModParticles.THERMAL_RETICLE.get(),
                        this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        0.0, 0.025, 0.0
                );
            } else if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 40 == 0 && this.isAlive()) {
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                HiveProgressionTier effectiveTier = HiveProgressionTier.resolveEffectiveTier(
                        hive.getThreatLevel(), hive.getHighestTierReached());
                float healAmt = effectiveTier.primordialAllyHealAmount(
                        hive.isMatriarchRemissionActive(serverLevel.getGameTime()));
                for (MycelialMob ally : this.level().getEntitiesOfClass(
                        MycelialMob.class, this.getBoundingBox().inflate(6.0D),
                        m -> m.isAlive() && !this.isRivalStrainTarget(m))) {
                    if (ally.getHealth() < ally.getMaxHealth()) {
                        ally.heal(healAmt);
                    }
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (!recentlyHit || isBossMob()) {
            return;
        }
        MycelialLootSystem.rollAdaptiveMobBonusDrops(this, level, damageSource);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData);
        if (levelAccessor instanceof ServerLevel serverLevel) {
            if (spawnType == MobSpawnType.NATURAL) {
                this.naturalSpawn = true;
            }
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            hive.recomputeActiveTraits(serverLevel.getGameTime());
            applyGlobalHiveState(serverLevel, hive);

            if (!(this instanceof HiveCoreBossEntity) && !(this instanceof ApexMatriarchEntity)) {
                double threat = hive.getThreatLevel();
                if (threat >= 65.0
                        || !hive.getActiveSynergies().isEmpty()
                        || serverLevel.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
                    promoteRank(EvolutionRank.ELITE, false);
                } else if (threat >= 25.0 || !hive.getActiveTraits().isEmpty()) {
                    promoteRank(EvolutionRank.MUTATED, false);
                }
            }

            int extraPlayers = Math.max(0, serverLevel.players().size() - 1);
            if (extraPlayers > 0) {
                AttributeInstance maxHp = this.getAttribute(Attributes.MAX_HEALTH);
                if (maxHp != null) {
                    double multiplier = 1.0 + extraPlayers * MycelialConfig.PLAYER_COUNT_SCALING.get();
                    maxHp.setBaseValue(maxHp.getBaseValue() * multiplier);
                    baseMaxHealth *= multiplier;
                    this.setHealth(this.getMaxHealth());
                }
            }
            applyProgressionScaling(serverLevel, hive);
            if (this instanceof MycelialBossMob boss) {
                com.alex.mycelialmind.hive.HiveCycleDirector.applyBossCycleScaling(boss, serverLevel);
            }
            if (!isBossMob() && !isAlliedEscort()) {
                HiveProgressionTier effectiveTier = HiveProgressionTier.resolveEffectiveTier(
                        hive.getThreatLevel(), hive.getHighestTierReached());
                boolean inDeep = serverLevel.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);
                boolean inTide = hive.isFungalTideActive(serverLevel.getGameTime());
                boolean inRemission = hive.isMatriarchRemissionActive(serverLevel.getGameTime());
                float primordialChance = effectiveTier.primordialSpawnChance(inDeep, inTide, inRemission);
                if (serverLevel.random.nextFloat() < primordialChance) {
                    setPrimordialVariant(true);
                }
            }
        }
        return data;
    }

    public void applyProgressionScaling(ServerLevel serverLevel, HiveMindSavedData hive) {
        if (!MycelialConfig.PROGRESSIVE_THREAT_SCALING.get()) {
            return;
        }
        HiveProgressionTier tier = HiveProgressionTier.fromThreat(hive.getThreatLevel());
        boolean inDeepMycelium = serverLevel.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);

        double healthBonus;
        double damageBonus;
        double armorBonus;
        double speedBonus;
        if (isBossMob()) {
            healthBonus = tier == HiveProgressionTier.OVERMIND ? 0.20 : (tier == HiveProgressionTier.ADAPTIVE ? 0.10 : 0.0);
            damageBonus = tier == HiveProgressionTier.OVERMIND ? 0.10 : 0.0;
            armorBonus = tier == HiveProgressionTier.OVERMIND ? 2.0 : 0.0;
            speedBonus = 0.0;
        } else {
            healthBonus = (tier.healthMultiplier() - 1.0) + (inDeepMycelium ? HiveProgressionTier.DEEP_MYCELIUM_HEALTH_BONUS : 0.0);
            damageBonus = (tier.damageMultiplier() - 1.0) + (inDeepMycelium ? HiveProgressionTier.DEEP_MYCELIUM_DAMAGE_BONUS : 0.0);
            armorBonus = tier.armorBonus() + (inDeepMycelium ? HiveProgressionTier.DEEP_MYCELIUM_ARMOR_BONUS : 0.0);
            speedBonus = (tier.speedMultiplier() - 1.0) + (inDeepMycelium ? HiveProgressionTier.DEEP_MYCELIUM_SPEED_BONUS : 0.0);
        }

        applyProgressionModifier(Attributes.MAX_HEALTH, THREAT_HEALTH_MODIFIER_ID, healthBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyProgressionModifier(Attributes.ATTACK_DAMAGE, THREAT_DAMAGE_MODIFIER_ID, damageBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyProgressionModifier(Attributes.ARMOR, THREAT_ARMOR_MODIFIER_ID, armorBonus, AttributeModifier.Operation.ADD_VALUE);
        applyProgressionModifier(Attributes.MOVEMENT_SPEED, THREAT_SPEED_MODIFIER_ID, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.setHealth(this.getMaxHealth());
    }

    private void applyProgressionModifier(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id,
            double amount,
            AttributeModifier.Operation operation
    ) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount > 0.0) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_TRAIT_MASK, getTraitMask());
        tag.putInt(TAG_TIER2_MASK, getTier2Mask());
        tag.putInt(TAG_SYNERGY_MASK, getSynergyMask());
        tag.putLong(TAG_HIVE_TRAIT_REVISION, appliedHiveTraitStateRevision);
        tag.putString(TAG_HIVE_TRAIT_DIMENSION, appliedHiveTraitDimension);
        tag.putInt(TAG_EVOLUTION_RANK, getEvolutionRank().id());
        tag.putBoolean(TAG_NATURAL_SPAWN, this.naturalSpawn);
        tag.putInt(TAG_HIVE_STRAIN, getHiveStrain().id());
        tag.putInt(TAG_SCHISM_TICKS, getSchismTicks());
        tag.putInt(TAG_DESYNC_TICKS, getDesyncTicks());
        tag.putInt(TAG_FRENZY_TICKS, getFrenzyTicks());
        if (isPrimordialVariant()) {
            tag.putBoolean(TAG_PRIMORDIAL, true);
        }
        tag.putInt(TAG_TRAIT_ATTRIBUTE_VERSION, 1);
        if (baseMaxHealth > 0.0D) {
            tag.putDouble(TAG_BASE_MAX_HEALTH, baseMaxHealth);
        }
        if (baseAttackDamage > 0.0D) {
            tag.putDouble(TAG_BASE_ATTACK_DAMAGE, baseAttackDamage);
        }
        if (baseXpReward > 0) {
            tag.putInt(TAG_BASE_XP_REWARD, baseXpReward);
        }
        if (this.escortExpiryTick > 0L) {
            tag.putLong(TAG_ESCORT_EXPIRY, this.escortExpiryTick);
            tag.putBoolean(TAG_ESCORT_GLOBAL_CLOCK, true);
        }
        if (this.alliedOwnerUuid != null) {
            tag.putUUID(TAG_ALLIED_OWNER, this.alliedOwnerUuid);
        }
        this.episodicMemory.writeToNbt(tag, TAG_EPISODIC_MEMORY);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        EvolutionRank savedRank = tag.contains(TAG_EVOLUTION_RANK)
                ? EvolutionRank.fromId(tag.getInt(TAG_EVOLUTION_RANK))
                : EvolutionRank.BASE;
        baseMaxHealth = readBaseAttribute(tag, TAG_BASE_MAX_HEALTH, Attributes.MAX_HEALTH,
                savedRank.healthMultiplier(), baseMaxHealth);
        baseAttackDamage = readBaseAttribute(tag, TAG_BASE_ATTACK_DAMAGE, Attributes.ATTACK_DAMAGE,
                savedRank.damageMultiplier(), baseAttackDamage);
        if (tag.contains(TAG_BASE_XP_REWARD)) {
            int savedBaseXp = tag.getInt(TAG_BASE_XP_REWARD);
            if (savedBaseXp > 0) {
                baseXpReward = savedBaseXp;
            }
        }
        if (!tag.contains(TAG_TRAIT_ATTRIBUTE_VERSION)) {
            migrateLegacyTraitBaseAttribute(Attributes.ARMOR, 8.0D);
            migrateLegacyTraitBaseAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.75D);
        }
        if (tag.contains(TAG_TRAIT_MASK)) {
            setTraitMask(tag.getInt(TAG_TRAIT_MASK));
        }
        if (tag.contains(TAG_TIER2_MASK)) {
            setTier2Mask(tag.getInt(TAG_TIER2_MASK));
        }
        if (tag.contains(TAG_SYNERGY_MASK)) {
            setSynergyMask(tag.getInt(TAG_SYNERGY_MASK));
        }
        appliedHiveTraitStateRevision = tag.contains(TAG_HIVE_TRAIT_REVISION)
                ? tag.getLong(TAG_HIVE_TRAIT_REVISION)
                : Long.MIN_VALUE;
        appliedHiveTraitDimension = tag.getString(TAG_HIVE_TRAIT_DIMENSION);
        if (tag.contains(TAG_EVOLUTION_RANK)) {
            float savedHealth = this.getHealth();
            promoteRank(EvolutionRank.fromId(tag.getInt(TAG_EVOLUTION_RANK)), false);
            this.setHealth(Math.min(savedHealth, this.getMaxHealth()));
        }
        this.naturalSpawn = tag.getBoolean(TAG_NATURAL_SPAWN);
        if (tag.contains(TAG_HIVE_STRAIN)) {
            setHiveStrain(HiveStrain.byId(tag.getInt(TAG_HIVE_STRAIN)));
        }
        if (tag.contains(TAG_SCHISM_TICKS)) {
            setSchismTicks(tag.getInt(TAG_SCHISM_TICKS));
        }
        if (tag.contains(TAG_DESYNC_TICKS)) {
            setDesyncTicks(tag.getInt(TAG_DESYNC_TICKS));
        }
        if (tag.contains(TAG_FRENZY_TICKS)) {
            setFrenzyTicks(tag.getInt(TAG_FRENZY_TICKS));
        }
        if (tag.getBoolean(TAG_PRIMORDIAL)) {
            float savedHp = this.getHealth();
            setPrimordialVariant(true);
            this.setHealth(Math.min(savedHp, this.getMaxHealth()));
        }
        if (tag.contains(TAG_ESCORT_EXPIRY)) {
            long savedExpiry = tag.getLong(TAG_ESCORT_EXPIRY);
            if (tag.getBoolean(TAG_ESCORT_GLOBAL_CLOCK)) {
                this.escortExpiryTick = savedExpiry;
            } else {
                long remainingTicks = Math.max(0L, savedExpiry - this.level().getGameTime());
                this.escortExpiryTick = getEscortClock() + remainingTicks;
            }
        } else {
            this.escortExpiryTick = -1L;
        }
        this.alliedOwnerUuid = tag.hasUUID(TAG_ALLIED_OWNER) ? tag.getUUID(TAG_ALLIED_OWNER) : null;
        this.episodicMemory.readFromNbt(tag, TAG_EPISODIC_MEMORY);
        if (isAlliedEscortActive() && this.level() instanceof ServerLevel serverLevel) {
            AlliedEscortSavedData.get(serverLevel.getServer())
                    .register(this.getUUID(), this.alliedOwnerUuid, this.escortExpiryTick);
        }
        onTraitsUpdated();
    }

    private double readBaseAttribute(
            CompoundTag tag,
            String key,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double rankMultiplier,
            double constructorBase
    ) {
        if (tag.contains(key)) {
            double savedBase = tag.getDouble(key);
            if (Double.isFinite(savedBase) && savedBase > 0.0D) {
                return savedBase;
            }
        }
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return constructorBase;
        }
        double multiplier = Math.max(0.0001D, rankMultiplier);
        double recoveredBase = instance.getBaseValue() / multiplier;
        return Double.isFinite(recoveredBase) && recoveredBase > 0.0D ? recoveredBase : constructorBase;
    }

    private void migrateLegacyTraitBaseAttribute(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double legacyMinimum
    ) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> livingEntityType =
                (EntityType<? extends LivingEntity>) (EntityType<?>) this.getType();
        double defaultValue = DefaultAttributes.getSupplier(livingEntityType).getValue(attribute);
        if (defaultValue < legacyMinimum && Math.abs(instance.getBaseValue() - legacyMinimum) < 1.0E-6D) {
            instance.setBaseValue(defaultValue);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel serverLevel) {
            com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector.onAttackerRemoved(serverLevel, getUUID());
        }
        if (reason.shouldDestroy() && this.alliedOwnerUuid != null && this.level() instanceof ServerLevel serverLevel) {
            AlliedEscortSavedData.get(serverLevel.getServer()).unregister(this.getUUID());
        }
        super.remove(reason);
    }

    public boolean isNaturalSpawn() {
        return naturalSpawn;
    }

    public void setNaturalSpawn(boolean naturalSpawn) {
        this.naturalSpawn = naturalSpawn;
    }

    public static boolean isOverworldSurface(ServerLevelAccessor level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasFixedTime()) {
            return false;
        }
        if (level.getBrightness(LightLayer.SKY, pos) > 0) {
            return true;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return pos.getY() >= surfaceY - 4;
    }

    public static boolean checkMycelialSpawnRules(
            EntityType<? extends Monster> type,
            ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (levelAccessor.getDifficulty() == Difficulty.PEACEFUL
                || !Mob.checkMobSpawnRules(type, levelAccessor, spawnType, pos, random)) {
            return false;
        }
        ServerLevel serverLevel = levelAccessor.getLevel();
        if ((spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION)
                && serverLevel != null
                && serverLevel.dimension().equals(Level.OVERWORLD)) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            boolean onMycelial = levelAccessor.getBlockState(pos).is(ModBlocks.HYPHAL_VEIN.get())
                    || levelAccessor.getBlockState(pos.below()).is(ModBlocks.CREEPING_MYCELIUM.get())
                    || levelAccessor.getBlockState(pos.below()).is(ModBlocks.HYPHAL_VEIN.get());
            boolean tideActive = hive.isFungalTideActive(serverLevel.getGameTime());
            if (isOverworldSurface(levelAccessor, pos) && serverLevel.isDay()) {
                boolean highThreat = hive.getThreatLevel() >= SURFACE_DAY_SPAWN_THREAT_THRESHOLD || tideActive;
                if (!highThreat) {
                    return false;
                }
                if (onMycelial) {
                    return true;
                }
            }
            double chunkDensity = hive.getHyphalDensityAt(pos);
            BlockPos nearestNode = PurificationZoneTracker.get(serverLevel).findNearestEnemyNode(pos);
            boolean nearNode = nearestNode != null && nearestNode.distSqr(pos) <= 32.0D * 32.0D;
            if (!HyphalAdaptiveArchitectureEngine.shouldAllowOverworldNaturalSpawn(
                    chunkDensity, onMycelial, nearNode, tideActive, random.nextDouble())) {
                return false;
            }
        }
        return Monster.isDarkEnoughToSpawn(levelAccessor, pos, random);
    }

    public boolean tryDawnSurfaceRetreat(ServerLevel serverLevel) {
        if (!this.naturalSpawn || !serverLevel.isDay() || !this.isAlive()) {
            return false;
        }
        if (!isOverworldSurface(serverLevel, this.blockPosition()) || !serverLevel.canSeeSky(this.blockPosition())) {
            return false;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
        if (hive.getThreatLevel() >= SURFACE_DAY_SPAWN_THREAT_THRESHOLD
                || hive.isFungalTideActive(serverLevel.getGameTime())) {
            return false;
        }
        LivingEntity target = this.getTarget();
        if ((target != null && target.isAlive()) || this.hurtTime > 0) {
            return false;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
        if (!tracker.getNodesWithin(this.blockPosition(), 24, 1).isEmpty()) {
            return false;
        }
        serverLevel.sendParticles(ModParticles.SPORE_MOTE.get(),
                this.getX(), this.getY() + 0.35, this.getZ(),
                14, 0.35, 0.25, 0.35, 0.04);
        serverLevel.sendParticles(ModParticles.SPORE_CLOUD.get(),
                this.getX(), this.getY() + 0.2, this.getZ(),
                2, 0.25, 0.15, 0.25, 0.02);
        serverLevel.playSound(null, this.blockPosition(), ModSounds.MOB_DAWN_RETREAT.get(), SoundSource.HOSTILE, 0.7F, 0.9F);
        this.discard();
        return true;
    }

    @Override
    protected Brain.Provider<?> brainProvider() {
        return HiveBrainFactory.createProvider(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<?> brain = super.makeBrain(dynamic);
        this.hiveBrainRuntime = HiveBrainFactory.initializeBrain(this, (Brain<MycelialMob>) brain);
        return brain;
    }

    @Override
    @Nullable
    public HiveBrainRuntime<MycelialMob> getHiveBrainRuntime() {
        return this.hiveBrainRuntime;
    }

    @Override
    public void setHiveBrainRuntime(@Nullable HiveBrainRuntime<MycelialMob> runtime) {
        this.hiveBrainRuntime = runtime;
    }

    @Override
    public boolean shouldTickBrain(ServerLevel level, MycelialMob entity) {
        HiveBrain<MycelialMob> brain = getHiveBrain();
        long tickCountVal = brain != null ? brain.getBrainTickCount() : 0L;
        boolean highPriority = this.tickCount <= 2
                || isBossMob()
                || getTarget() != null
                || isDesynced()
                || isFrenzyActive()
                || this.hurtTime > 0
                || HiveBrainUtils.hasMemory(this, ModMemoryTypes.SHARED_TARGET_POS.get())
                || HiveBrainUtils.hasMemory(this, ModMemoryTypes.INCOMING_PROJECTILE_POS.get())
                || (brain != null && brain.getActiveActivity() != getDefaultActivity());
        boolean nearPlayer = level.getNearestPlayer(this, 32.0D) != null;
        return HiveBrainScheduler.shouldTickBrainWithLod(
                tickCountVal, this.getId(), level.getGameTime(), highPriority, nearPlayer);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.getTarget() instanceof Player player && !isBossMob()
                && player.getData(ModAttachments.DIPLOMACY).isProtectedFrom(
                        getHiveStrain(), getEvolutionRank(), false, this.distanceToSqr(player))
                && this.getLastHurtByMob() != player) {
            this.setTarget(null);
        }
        if (this.tickCount % 40 == 0 && this.level() instanceof ServerLevel serverLevel) {
            if (tryDawnSurfaceRetreat(serverLevel)) {
                return;
            }
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            tickTacticalCommandState(serverLevel);
            int currentDesync = getDesyncTicks();
            if (this.lastDesyncTicks > 0 && currentDesync == 0) {
                this.episodicMemory.remember(MobEpisodicMemory.KEY_SURVIVED_DESYNC, serverLevel.getGameTime());
            }
            this.lastDesyncTicks = currentDesync;

            if (this.tickCount % 20 == 0 && !(this instanceof MycelialBossMob) && !isBossMob()
                    && PurificationZoneTracker.get(serverLevel).isProtected(this.blockPosition())) {
                this.episodicMemory.remember(MobEpisodicMemory.KEY_TRAUMA_PYLON, serverLevel.getGameTime());
            }

            if (((this.tickCount + this.getId()) % 10) == 0
                    && !(this instanceof MycelialBossMob) && !isBossMob()
                    && !isDesynced() && !isAlliedEscort()
                    && !(this instanceof AcidSpitterEntity)) {
                resolveThreatTableTarget(serverLevel);
            }

            boolean hasTarget = this.getTarget() != null && this.getTarget().isAlive();
            boolean nearPlayer = serverLevel.getNearestPlayer(this, 24.0D) != null;
            boolean inAlarmOrFrenzy = isFrenzyActive()
                    || HiveBlackboard.getNearestActiveAlarm(serverLevel, this.blockPosition()) != null;
            if ((hasTarget || inAlarmOrFrenzy) && this.nextTacticalTreeTick > this.tickCount + 4) {
                this.nextTacticalTreeTick = this.tickCount + 4;
            }
            if (this.tacticalTree != null && this.tickCount >= this.nextTacticalTreeTick) {
                this.aiBlackboard.populateFromMob(this, this.getTarget());
                this.tacticalTree.step();
                int interval = TacticalEvasionEngine.computeAdaptiveAiInterval(hasTarget, nearPlayer, inAlarmOrFrenzy);
                this.nextTacticalTreeTick = this.tickCount + interval;
            }
        }
        tickBrain(this);
    }

    public void resolveThreatTableTarget(ServerLevel serverLevel) {
        if (this instanceof MycelialBossMob || isBossMob() || isDesynced() || isAlliedEscort()
                || this instanceof AcidSpitterEntity) {
            return;
        }
        LivingEntity currentTarget = this.getTarget();
        long now = serverLevel.getGameTime();
        if (currentTarget != null && currentTarget.isAlive() && this.canAttack(currentTarget)) {
            float ratio = currentTarget.getHealth() / Math.max(1.0F, currentTarget.getMaxHealth());
            this.threatTable.recordProximity(currentTarget.getUUID(), this.distanceToSqr(currentTarget), ratio, now);
        }
        if (this.threatTable.size() == 0) {
            return;
        }
        UUID currentId = currentTarget != null ? currentTarget.getUUID() : null;
        UUID bestId = this.threatTable.selectHighestThreat(
                currentId,
                now,
                id -> serverLevel.getEntity(id) instanceof LivingEntity living
                        && living.isAlive()
                        && this.canAttack(living)
                        && !(living instanceof Player player && isFriendlyToPlayer(this, player))
                        && this.distanceToSqr(living) <= 32.0D * 32.0D,
                id -> this.episodicMemory.hasMemory(MobEpisodicMemory.grudgeKey(id))
        );
        if (bestId != null && !bestId.equals(currentId)
                && serverLevel.getEntity(bestId) instanceof LivingEntity nextTarget) {
            this.setTarget(nextTarget);
        }
    }

    private void tickTacticalCommandState(ServerLevel serverLevel) {
        int desync = getDesyncTicks();
        if (desync > 0) {
            setDesyncTicks(desync - 1);
            if (this.tickCount % 8 == 0) {
                serverLevel.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                        this.getX(), this.getY(0.85), this.getZ(),
                        2, 0.25, 0.2, 0.25, 0.02);
            }
        }
        int frenzy = getFrenzyTicks();
        if (frenzy > 0) {
            setFrenzyTicks(isDesynced() ? 0 : frenzy - 1);
            if (isFrenzyActive() && this.tickCount % 6 == 0) {
                serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(),
                        this.getX(), this.getY(0.5), this.getZ(),
                        3, 0.3, 0.3, 0.3, 0.02);
            }
        }

        AttributeInstance speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            boolean hasMod = speedAttr.getModifier(FRENZY_SPEED_MODIFIER_ID) != null;
            boolean wantMod = isFrenzyActive();
            if (wantMod && !hasMod) {
                speedAttr.addTransientModifier(new AttributeModifier(
                        FRENZY_SPEED_MODIFIER_ID, 0.22D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!wantMod && hasMod) {
                speedAttr.removeModifier(FRENZY_SPEED_MODIFIER_ID);
            }
        }

        if (isBossMob() || ((this.tickCount + this.getId()) % 10) != 0) {
            return;
        }

        if (PurificationZoneTracker.get(serverLevel).isProtected(this.blockPosition())) {
            if (getDesyncTicks() < 40) {
                triggerDesync(SynapticCommandSystem.DESYNC_DURATION_TICKS);
            }
            return;
        }

        if (isDesynced()) {
            setPhalanxActive(false);
            setCommandRole(SynapticCommandSystem.CommandRole.NONE);
            return;
        }

        List<MycelialMob> squad = serverLevel.getEntitiesOfClass(
                MycelialMob.class,
                this.getBoundingBox().inflate(SynapticCommandSystem.COMMAND_LINK_RADIUS),
                ally -> ally.isAlive() && !ally.isBossMob() && !ally.isDesynced()
        );
        MycelialMob leader = SynapticCommandSystem.selectSquadLeader(squad);
        if (leader == this) {
            setCommandRole(SynapticCommandSystem.CommandRole.LEADER);
            serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(),
                    this.getX(), this.getY() + this.getBbHeight() + 0.35D, this.getZ(),
                    1, 0.08, 0.05, 0.08, 0.0);
        } else if (leader != null) {
            setCommandRole(SynapticCommandSystem.CommandRole.LINKED);
            if ((this.tickCount + this.getId()) % 20 == 0) {
                double midX = (this.getX() + leader.getX()) * 0.5D;
                double midY = (this.getY(0.6) + leader.getY(0.6)) * 0.5D;
                double midZ = (this.getZ() + leader.getZ()) * 0.5D;
                serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(), midX, midY, midZ, 1, 0.05, 0.05, 0.05, 0.0);
            }
        } else {
            setCommandRole(SynapticCommandSystem.CommandRole.NONE);
        }

        LivingEntity target = this.getTarget();
        if ((this instanceof ChitinBruteEntity || this instanceof ColonyWardenEntity)
                && target != null && target.isAlive()) {
            boolean hasRangedNearby = false;
            for (MycelialMob ally : squad) {
                if (ally != this && (ally instanceof AcidSpitterEntity || ally instanceof SynapseWeaverEntity)
                        && ally.distanceToSqr(this) <= 144.0D) {
                    hasRangedNearby = true;
                    break;
                }
            }
            setPhalanxActive(hasRangedNearby && leader != null);
        } else {
            setPhalanxActive(false);
        }

        if (leader != null && !isFrenzyActive()
                && HiveBlackboard.countRecentCasualtiesNear(
                        serverLevel,
                        this.blockPosition(),
                        SynapticCommandSystem.CASUALTY_RADIUS_SQ,
                        SynapticCommandSystem.CASUALTY_WINDOW_TICKS) >= SynapticCommandSystem.CASUALTY_FRENZY_THRESHOLD) {
            triggerFrenzy(SynapticCommandSystem.FRENZY_DURATION_TICKS);
        }
    }

    @Override
    public List<HiveSensor<MycelialMob>> getSensors() {
        return HiveBrainBridge.createDefaultHiveSensors();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getCoreTasks() {
        return HiveBrainBridge.createCoreTasks();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getIdleTasks() {
        return HiveBrainBridge.createIdleTasks();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getFightTasks() {
        return HiveBrainBridge.createFightTasks(8);
    }

    public static float computeRankVoicePitchMultiplier(EvolutionRank rank) {
        return rank == null ? 1.0F : rank.voicePitchMultiplier();
    }

    public static float computeRankSoundVolumeMultiplier(EvolutionRank rank) {
        return rank == null ? 1.0F : rank.soundVolumeMultiplier();
    }

    public static float computeTacticalVoicePitchMultiplier(
            EvolutionRank rank,
            HiveStrain strain,
            boolean frenzy,
            boolean desynced,
            SynapticCommandSystem.CommandRole role,
            boolean occluded
    ) {
        return EvolutionRank.computeTacticalVoicePitchMultiplier(rank, strain, frenzy, desynced, role, occluded);
    }

    public static float computeTacticalSoundVolumeMultiplier(
            EvolutionRank rank,
            boolean frenzy,
            SynapticCommandSystem.CommandRole role,
            boolean occluded
    ) {
        return EvolutionRank.computeTacticalSoundVolumeMultiplier(rank, frenzy, role, occluded);
    }

    /**
     * Per-listener wall occlusion is handled client-side via {@code ClientAcousticController} and
     * {@code OpenAlEfxBridge} (OpenAL EFX direct low-pass filtering) so multiplayer listeners each hear
     * accurate occlusion without reading client config on the server thread.
     */
    protected boolean isAudioOccludedToNearestPlayer() {
        return false;
    }

    protected float getBaseSoundVolume() {
        return 0.95F;
    }

    protected float getStepSoundVolume() {
        return 0.26F;
    }

    @Override
    public float getVoicePitch() {
        boolean desynced = isDesynced();
        float jitter = desynced ? (0.94F + this.random.nextFloat() * 0.14F) : 1.0F;
        return super.getVoicePitch() * jitter * computeTacticalVoicePitchMultiplier(
                getEvolutionRank(),
                getHiveStrain(),
                isFrenzyActive(),
                desynced,
                getCommandRole(),
                isAudioOccludedToNearestPlayer()
        );
    }

    @Override
    protected float getSoundVolume() {
        return getBaseSoundVolume() * computeTacticalSoundVolumeMultiplier(
                getEvolutionRank(),
                isFrenzyActive(),
                getCommandRole(),
                isAudioOccludedToNearestPlayer()
        );
    }

    @Override
    public int getAmbientSoundInterval() {
        return isFrenzyActive() ? 98 : 140;
    }

    public float resolveBaseSoundVolume() {
        return getSoundVolume();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        EvolutionRank rank = getEvolutionRank();
        boolean occluded = isAudioOccludedToNearestPlayer();
        float stepVol = getStepSoundVolume() * computeTacticalSoundVolumeMultiplier(
                rank, isFrenzyActive(), getCommandRole(), occluded);
        float stepPitch = (0.92F + this.random.nextFloat() * 0.16F) * computeTacticalVoicePitchMultiplier(
                rank, getHiveStrain(), isFrenzyActive(), isDesynced(), getCommandRole(), occluded);
        this.playSound(getStepSoundEvent(), stepVol, stepPitch);
    }

    protected SoundEvent getStepSoundEvent() {
        return ModSounds.MOB_STEP_CHITIN.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SPORELING_CHITTER.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SPORELING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPORELING_DEATH.get();
    }

    public SoundEvent resolveAmbientSound() {
        return getAmbientSound();
    }

    public SoundEvent resolveHurtSound(DamageSource source) {
        return getHurtSound(source);
    }

    public SoundEvent resolveDeathSound() {
        return getDeathSound();
    }

    public boolean hurtFromBossPart(MycelialBossPart part, DamageSource source, float modifiedAmount) {
        boolean hurt = this.hurt(source, modifiedAmount);
        if (hurt && this.level() instanceof ServerLevel serverLevel) {
            double px = part.getX();
            double py = part.getY() + part.getBbHeight() * 0.5D;
            double pz = part.getZ();
            if (part.getRole() == MycelialBossPart.PartRole.WEAK_POINT) {
                serverLevel.sendParticles(ModParticles.SYNAPSE_SPARK.get(), px, py, pz,
                        8, 0.25, 0.25, 0.25, 0.05);
                serverLevel.sendParticles(ModParticles.IMPACT.get(), px, py, pz,
                        1, 0.0, 0.1, 0.0, 0.15);
                serverLevel.playSound(null, part.blockPosition(), ModSounds.BOSS_WEAKPOINT_CRIT.get(),
                        SoundSource.HOSTILE, 0.95F, 1.0F);
            } else {
                boolean carapaceBroken = this instanceof MycelialBossMob boss && boss.isCarapaceBroken();
                serverLevel.sendParticles(ModParticles.CHITIN_SHARD.get(), px, py, pz,
                        5, 0.3, 0.2, 0.3, 0.03);
                SoundEvent partSound = carapaceBroken
                        ? ModSounds.SYNAPTIC_PULSE.get()
                        : ModSounds.BOSS_CARAPACE_RICOCHET.get();
                serverLevel.playSound(null, part.blockPosition(), partSound,
                        SoundSource.HOSTILE, 0.85F, carapaceBroken ? 1.25F : 1.0F);
            }
        }
        return hurt;
    }

    protected static void positionBossPart(
            MycelialMob parent,
            MycelialBossPart part,
            double forwardOffset,
            double yOffset,
            double sideOffset
    ) {
        double yawRad = Math.toRadians(parent.yBodyRot);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);
        double px = parent.getX() + fx * forwardOffset + rx * sideOffset;
        double py = parent.getY() + yOffset;
        double pz = parent.getZ() + fz * forwardOffset + rz * sideOffset;
        part.xo = part.getX();
        part.yo = part.getY();
        part.zo = part.getZ();
        part.xOld = part.xo;
        part.yOld = part.yo;
        part.zOld = part.zo;
        part.setPos(px, py, pz);
    }
}
