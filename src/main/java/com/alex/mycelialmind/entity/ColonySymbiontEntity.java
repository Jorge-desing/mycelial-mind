package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.ColonySiteHabit;
import com.alex.mycelialmind.colony.ColonySiteMemory;
import com.alex.mycelialmind.colony.ColonyWitnessMemory;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.colony.SymbiontRank;
import com.alex.mycelialmind.colony.SymbiontTemperament;
import com.alex.mycelialmind.entity.ai.colony.SymbiontArchitectGoal;
import com.alex.mycelialmind.entity.ai.colony.SymbiontCourierGoal;
import com.alex.mycelialmind.entity.ai.colony.SymbiontExcavatorGoal;
import com.alex.mycelialmind.entity.ai.colony.SymbiontHarvesterGoal;
import com.alex.mycelialmind.entity.ai.colony.SymbiontRestAndFeedGoal;
import com.alex.mycelialmind.entity.ai.colony.SymbiontMemoryWitnessGoal;
import com.alex.mycelialmind.entity.ai.colony.SymbiontSentinelGoal;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Containers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Polymorphic player-aligned Sovereign Colony worker mob specializing into 5 Castes
 * with Rank progression, Temperament modifiers, internal biological state, and spatial memory.
 */
public class ColonySymbiontEntity extends PathfinderMob {
    public static final int POUCH_SLOTS = 4;

    private static final EntityDataAccessor<Integer> DATA_CASTE =
            SynchedEntityData.defineId(ColonySymbiontEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RANK =
            SynchedEntityData.defineId(ColonySymbiontEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TEMPERAMENT =
            SynchedEntityData.defineId(ColonySymbiontEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_NUTRITION =
            SynchedEntityData.defineId(ColonySymbiontEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FATIGUE =
            SynchedEntityData.defineId(ColonySymbiontEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MORALE =
            SynchedEntityData.defineId(ColonySymbiontEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_STANDBY =
            SynchedEntityData.defineId(ColonySymbiontEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_TASK_STATE =
            SynchedEntityData.defineId(ColonySymbiontEntity.class, EntityDataSerializers.STRING);

    private static final String TAG_OWNER = "OwnerUUID";
    private static final String TAG_NEXUS = "NexusPos";
    private static final String TAG_HAS_NEXUS = "HasNexus";
    private static final String TAG_CASTE = "Caste";
    private static final String TAG_RANK = "Rank";
    private static final String TAG_TEMP = "Temperament";
    private static final String TAG_XP = "WorkerXp";
    private static final String TAG_NUTRITION = "Nutrition";
    private static final String TAG_FATIGUE = "Fatigue";
    private static final String TAG_MORALE = "Morale";
    private static final String TAG_STANDBY = "Standby";
    private static final String TAG_POUCH = "Pouch";
    private static final String TAG_LAST_WORK_TICK = "LastWorkTick";
    private static final String TAG_LAST_REST_TICK = "LastRestTick";
    private static final String TAG_SITE_WITNESSES = "SiteWitnessMemories";

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private BlockPos nexusPos;
    private int workerXp = 0;
    private long lastWorkTick = 0L;
    private long lastRestTick = 0L;
    private boolean resting = false;
    private boolean damagedDuringCourierTrip;
    @Nullable
    private BlockPos pendingEarlyRestSite;
    private UUID witnessMemoriesSyncedToColony;
    private final Map<ColonyWitnessMemory.Key, ColonyWitnessMemory> siteWitnessMemories = new LinkedHashMap<>();

    private final ItemStackHandler pouch = new ItemStackHandler(POUCH_SLOTS);

    public ColonySymbiontEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTE, SymbiontCaste.HARVESTER.index());
        builder.define(DATA_RANK, SymbiontRank.NOVICE.index());
        builder.define(DATA_TEMPERAMENT, SymbiontTemperament.DILIGENT.index());
        builder.define(DATA_NUTRITION, 100.0F);
        builder.define(DATA_FATIGUE, 0.0F);
        builder.define(DATA_MORALE, 80.0F);
        builder.define(DATA_STANDBY, false);
        builder.define(DATA_TASK_STATE, "idle");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SymbiontMemoryWitnessGoal(this));
        this.goalSelector.addGoal(1, new SymbiontRestAndFeedGoal(this));
        this.goalSelector.addGoal(2, new SymbiontSentinelGoal(this));
        this.goalSelector.addGoal(3, new SymbiontHarvesterGoal(this));
        this.goalSelector.addGoal(3, new SymbiontExcavatorGoal(this));
        this.goalSelector.addGoal(3, new SymbiontCourierGoal(this));
        this.goalSelector.addGoal(3, new SymbiontArchitectGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType reason,
            @Nullable SpawnGroupData spawnData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        if (!level.isClientSide() && reason == MobSpawnType.SPAWN_EGG) {
            SymbiontTemperament[] temps = SymbiontTemperament.values();
            setTemperament(temps[this.random.nextInt(temps.length)]);
            if (this.level() instanceof ServerLevel serverLevel) {
                SovereignColonyData nearest = SovereignColonySavedData.get(serverLevel).findNearestColony(this.blockPosition(), null, 96.0D);
                if (nearest != null && com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isColonyActive(serverLevel, nearest.getNexusPos())) {
                    setNexusPos(nearest.getNexusPos());
                    setOwnerUuid(nearest.getOwnerId());
                }
            }
            applyStatsFromState();
        }
        return data;
    }

    public SymbiontCaste getCaste() {
        return SymbiontCaste.fromIndex(this.entityData.get(DATA_CASTE));
    }

    public void setCaste(SymbiontCaste caste) {
        SymbiontCaste resolved = caste != null ? caste : SymbiontCaste.HARVESTER;
        SymbiontCaste previous = getCaste();
        this.entityData.set(DATA_CASTE, resolved.index());
        if (previous != resolved && this.level() instanceof ServerLevel serverLevel) {
            SovereignColonyData colony = getColonyData(serverLevel);
            if (colony != null) {
                colony.getOrderBoard().releaseWorkerReservations(this.getUUID());
            }
        }
        applyStatsFromState();
        syncRosterToColony();
    }

    public SymbiontRank getRank() {
        return SymbiontRank.fromIndex(this.entityData.get(DATA_RANK));
    }

    public void setRank(SymbiontRank rank) {
        this.entityData.set(DATA_RANK, (rank != null ? rank : SymbiontRank.NOVICE).index());
        applyStatsFromState();
        this.refreshDimensions();
    }

    public SymbiontTemperament getTemperament() {
        return SymbiontTemperament.fromIndex(this.entityData.get(DATA_TEMPERAMENT));
    }

    public void setTemperament(SymbiontTemperament temperament) {
        this.entityData.set(DATA_TEMPERAMENT, (temperament != null ? temperament : SymbiontTemperament.DILIGENT).index());
        applyStatsFromState();
    }

    public float getNutrition() {
        return this.entityData.get(DATA_NUTRITION);
    }

    public void setNutrition(float nutrition) {
        this.entityData.set(DATA_NUTRITION, Math.max(0.0F, Math.min(100.0F, nutrition)));
    }

    public float getFatigue() {
        return this.entityData.get(DATA_FATIGUE);
    }

    public void setFatigue(float fatigue) {
        this.entityData.set(DATA_FATIGUE, Math.max(0.0F, Math.min(100.0F, fatigue)));
    }

    public float getMorale() {
        return this.entityData.get(DATA_MORALE);
    }

    public void setMorale(float morale) {
        this.entityData.set(DATA_MORALE, Math.max(0.0F, Math.min(100.0F, morale)));
    }

    public boolean isStandby() {
        return this.entityData.get(DATA_STANDBY) || (this.level() instanceof ServerLevel serverLevel
                && !com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isColonyActive(serverLevel, nexusPos));
    }

    public void setStandby(boolean standby) {
        this.entityData.set(DATA_STANDBY, standby);
        if (standby && this.level() instanceof ServerLevel serverLevel) {
            SovereignColonyData colony = getColonyData(serverLevel);
            if (colony != null) {
                colony.getOrderBoard().releaseWorkerReservations(this.getUUID());
            }
            setTaskState("standby");
        }
        syncRosterToColony();
    }

    public String getTaskState() {
        return this.entityData.get(DATA_TASK_STATE);
    }

    public void setTaskState(String taskState) {
        this.entityData.set(DATA_TASK_STATE, taskState != null ? taskState : "idle");
    }

    public boolean isResting() {
        return resting;
    }

    public void setResting(boolean resting) {
        this.resting = resting;
    }

    public void markEarlyRestAt(BlockPos sitePos) {
        this.pendingEarlyRestSite = sitePos.immutable();
    }

    public boolean takeCourierDamageForMemory() {
        boolean damaged = damagedDuringCourierTrip;
        damagedDuringCourierTrip = false;
        return damaged;
    }

    public void clearCourierDamageForMemory() {
        damagedDuringCourierTrip = false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide() && getCaste() == SymbiontCaste.COURIER
                && getTaskState().startsWith("courier_")) {
            damagedDuringCourierTrip = true;
        }
        return hurt;
    }

    public int getWorkerXp() {
        return workerXp;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    @Nullable
    public BlockPos getNexusPos() {
        return nexusPos;
    }

    public void setNexusPos(@Nullable BlockPos nexusPos) {
        this.nexusPos = nexusPos == null ? null : nexusPos.immutable();
    }

    public ItemStackHandler getPouch() {
        return pouch;
    }

    public boolean isPouchEmpty() {
        for (int i = 0; i < pouch.getSlots(); i++) {
            if (!pouch.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isPouchFull() {
        for (int i = 0; i < pouch.getSlots(); i++) {
            ItemStack st = pouch.getStackInSlot(i);
            if (st.isEmpty() || st.getCount() < st.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    public ItemStack insertIntoPouch(ItemStack stack) {
        return ItemHandlerHelper.insertItem(this.pouch, stack, false);
    }

    public void pickupNearbyDrops(ServerLevel level, double radius) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(radius),
                e -> e.isAlive() && !e.getItem().isEmpty());
        for (ItemEntity itemEntity : items) {
            ItemStack remaining = insertIntoPouch(itemEntity.getItem().copy());
            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else if (remaining.getCount() < itemEntity.getItem().getCount()) {
                itemEntity.setItem(remaining);
            }
        }
    }

    public boolean depositPouchToContainer(ServerLevel level, BlockPos containerPos) {
        BlockEntity be = level.getBlockEntity(containerPos);
        if (be == null) {
            return false;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, containerPos, null);
        if (handler == null) {
            return false;
        }
        boolean movedAny = false;
        for (int i = 0; i < pouch.getSlots(); i++) {
            ItemStack stack = pouch.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItem(handler, stack.copy(), false);
            if (remainder.getCount() < stack.getCount()) {
                pouch.setStackInSlot(i, remainder);
                movedAny = true;
            }
        }
        return movedAny;
    }

    @Nullable
    public SovereignColonyData getColonyData(ServerLevel level) {
        if (!com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isColonyActive(level, nexusPos)) {
            return null;
        }
        SovereignColonySavedData saved = SovereignColonySavedData.get(level);
        if (this.nexusPos != null) {
            SovereignColonyData direct = saved.getByNexus(this.nexusPos);
            if (direct != null) {
                syncSiteWitnessMemories(direct);
                return direct;
            }
        }
        SovereignColonyData nearest = saved.findNearestColony(this.blockPosition(), this.ownerUuid, 96.0D);
        if (nearest != null) {
            this.nexusPos = nearest.getNexusPos();
            if (this.ownerUuid == null) {
                this.ownerUuid = nearest.getOwnerId();
            }
            syncSiteWitnessMemories(nearest);
        }
        return nearest;
    }

    private void syncSiteWitnessMemories(SovereignColonyData colony) {
        if (colony.getColonyId().equals(witnessMemoriesSyncedToColony)) {
            return;
        }
        for (ColonyWitnessMemory memory : siteWitnessMemories.values()) {
            colony.recordWitnessMemory(memory);
        }
        witnessMemoriesSyncedToColony = colony.getColonyId();
    }

    public void rememberCourierChest(ServerLevel level, BlockPos sitePos, BlockPos chestPos, boolean inputRole) {
        rememberSiteExperience(level, sitePos,
                inputRole ? ColonyWitnessMemory.Type.COURIER_INPUT : ColonyWitnessMemory.Type.COURIER_OUTPUT,
                chestPos);
    }

    public void rememberPatrolPoint(ServerLevel level, BlockPos sitePos, BlockPos patrolPos) {
        BlockPos offset = new BlockPos(patrolPos.getX() - sitePos.getX(),
                patrolPos.getY() - sitePos.getY(), patrolPos.getZ() - sitePos.getZ());
        if (com.alex.mycelialmind.colony.ColonyReconstructionRules.isSafePatrolOffset(offset)) {
            rememberSiteExperience(level, sitePos, ColonyWitnessMemory.Type.SENTINEL_PATROL, offset);
        }
    }

    public void rememberSuccessfulHabit(ServerLevel level, BlockPos sitePos, ColonySiteHabit habit) {
        ColonyWitnessMemory.Type type = switch (habit) {
            case ESCORTED_COURIER -> ColonyWitnessMemory.Type.HABIT_ESCORTED_COURIER;
            case REST_BEFORE_RETURN -> ColonyWitnessMemory.Type.HABIT_REST_BEFORE_RETURN;
            case STAGGERED_PICKUP -> ColonyWitnessMemory.Type.HABIT_STAGGERED_PICKUP;
            case NONE -> null;
        };
        if (type != null) {
            rememberSiteExperience(level, sitePos, type, BlockPos.ZERO);
        }
    }

    private void rememberSiteExperience(ServerLevel level, BlockPos sitePos,
                                        ColonyWitnessMemory.Type type, BlockPos memoryPos) {
        SovereignColonyData colony = getColonyData(level);
        if (colony == null) {
            return;
        }
        ColonyWitnessMemory memory = new ColonyWitnessMemory(getUUID(), sitePos, type, memoryPos);
        siteWitnessMemories.put(memory.key(), memory);
        if (colony.recordWitnessMemory(memory)) {
            SovereignColonySavedData.get(level).setDirty();
        }
    }

    public BlockPos getHomeAnchor() {
        if (this.nexusPos != null) {
            return this.nexusPos;
        }
        return this.blockPosition();
    }

    public void applyStatsFromState() {
        SymbiontCaste caste = getCaste();
        SymbiontRank rank = getRank();
        boolean guardianBonus = false;
        if (this.level() instanceof ServerLevel serverLevel) {
            SovereignColonyData colony = getColonyData(serverLevel);
            if (colony != null && colony.hasTrait(SovereignTrait.APEX_GUARDIAN_MATRIX)) {
                guardianBonus = true;
            }
        }
        double hp = caste.baseHealth() * rank.healthMultiplier() * (guardianBonus ? 1.20D : 1.0D);
        double dmg = caste.baseAttackDamage() * rank.damageMultiplier() * getTemperament().combatDamageMultiplier()
                * (guardianBonus ? 1.25D : 1.0D);
        double spd = caste.baseMoveSpeed();

        AttributeInstance maxHpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            double oldMax = maxHpAttr.getBaseValue();
            maxHpAttr.setBaseValue(hp);
            if (hp > oldMax) {
                this.heal((float) (hp - oldMax));
            } else if (this.getHealth() > (float) hp) {
                this.setHealth((float) hp);
            }
        }
        AttributeInstance atkAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.setBaseValue(dmg);
        }
        AttributeInstance spdAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spdAttr != null) {
            spdAttr.setBaseValue(spd);
        }
    }

    public void recordWorkCompleted(ServerLevel level, ColonyOrder.OrderType orderType, int workerXpGain) {
        this.lastWorkTick = level.getGameTime();
        SymbiontTemperament temp = getTemperament();
        double fatigueCost = 4.2D * temp.fatigueRateMultiplier();
        SovereignColonyData colony = getColonyData(level);
        if (colony != null && colony.hasTrait(SovereignTrait.LIVING_BULWARK)) {
            fatigueCost *= 0.72D;
        }
        setFatigue(getFatigue() + (float) fatigueCost);
        setNutrition(getNutrition() - 2.2F);
        setMorale(getMorale() + (float) (0.8D * temp.moraleStabilityMultiplier()));

        SymbiontRank before = getRank();
        this.workerXp = Math.max(0, this.workerXp + Math.max(1, workerXpGain));
        SymbiontRank after = SymbiontRank.forXp(this.workerXp);
        if (after != before) {
            setRank(after);
            level.playSound(null, this.blockPosition(), ModSounds.COLONY_SYMBIONT_PROMOTE.get(),
                    this.getSoundSource(), 0.9F, 1.15F);
            level.sendParticles(ModParticles.BIO_LUME.get(),
                    this.getX(), this.getY() + 0.9, this.getZ(), 14, 0.4, 0.5, 0.4, 0.04);
        } else {
            level.playSound(null, this.blockPosition(), ModSounds.COLONY_SYMBIONT_WORK.get(),
                    this.getSoundSource(), 0.45F, 1.0F + (this.random.nextFloat() * 0.15F));
        }

        if (colony != null) {
            if (pendingEarlyRestSite != null) {
                ColonySiteMemory memory = colony.getSiteMemory(pendingEarlyRestSite);
                if (memory != null) {
                    boolean learned = memory.recordTrialSuccess(ColonySiteHabit.REST_BEFORE_RETURN, 2);
                    if (memory.usesHabit(ColonySiteHabit.REST_BEFORE_RETURN)) {
                        rememberSiteExperience(level, pendingEarlyRestSite,
                                ColonyWitnessMemory.Type.HABIT_REST_BEFORE_RETURN, BlockPos.ZERO);
                    }
                    if (learned) {
                        level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                                pendingEarlyRestSite.getX() + 0.5D, pendingEarlyRestSite.getY() + 1.0D,
                                pendingEarlyRestSite.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                    SovereignColonySavedData.get(level).setDirty();
                }
                pendingEarlyRestSite = null;
            }
            int domainGain = (int) Math.round(SovereignColonyEngine.computeOrderDomainXp(orderType, temp, colony.getTier()));
            colony.addDomainXp(domainGain);
            SovereignColonySavedData.get(level).setDirty();
        }
        syncRosterToColony();
    }

    public void syncRosterToColony() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        SovereignColonyData colony = getColonyData(serverLevel);
        if (colony == null) {
            return;
        }
        colony.upsertWorker(new SovereignColonyData.WorkerRosterEntry(
                this.getUUID(),
                getCaste(),
                getRank(),
                getTemperament(),
                getNutrition(),
                getFatigue(),
                getMorale(),
                isStandby(),
                workerXp
        ));
        colony.recalculateMorale();
        SovereignColonySavedData.get(serverLevel).setDirty();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isColonyActive(serverLevel, nexusPos)) {
            return;
        }
        if (this.tickCount % 100 == 0) {
            float passiveFatigue = (isStandby() || resting) ? -1.5F : 0.4F;
            setFatigue(getFatigue() + passiveFatigue);
            if (!resting) {
                setNutrition(getNutrition() - 0.35F);
            }
            if (getHealth() < getMaxHealth() && getNutrition() > 50.0F && getMorale() > 50.0F) {
                this.heal(1.0F);
            }
            syncRosterToColony();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level() instanceof ServerLevel serverLevel
                && !com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isColonyActive(serverLevel, nexusPos) && !player.isCreative()) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.SOVEREIGN_COMMAND_STAFF.get())) {
            return InteractionResult.PASS;
        }
        if (stack.is(ModItems.MYCELIAL_BIOMASS.get())) {
            if (this.level() instanceof ServerLevel serverLevel) {
                this.heal(8.0F);
                setNutrition(getNutrition() + 35.0F);
                setFatigue(Math.max(0.0F, getFatigue() - 25.0F));
                setMorale(getMorale() + 8.0F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                        this.getX(), this.getY() + 0.8, this.getZ(), 10, 0.3, 0.3, 0.3, 0.03);
                serverLevel.playSound(null, this.blockPosition(), ModSounds.SYMBIOTIC_NODE_CHARGE.get(),
                        this.getSoundSource(), 0.7F, 1.25F);
                syncRosterToColony();
                player.displayClientMessage(Component.translatable("message.mycelialmind.symbiont_fed",
                        Component.translatable(getCaste().nameKey()),
                        String.format("%.0f", getNutrition()),
                        String.format("%.0f", getMorale())).withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (stack.isEmpty() && !this.level().isClientSide()) {
            if (player.isSecondaryUseActive()) {
                boolean next = !isStandby();
                setStandby(next);
                player.displayClientMessage(Component.translatable(
                        next ? "message.mycelialmind.symbiont_standby_on" : "message.mycelialmind.symbiont_standby_off",
                        Component.translatable(getCaste().nameKey())).withStyle(ChatFormatting.AQUA), true);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.mycelialmind.symbiont_status",
                        Component.translatable(getCaste().nameKey()),
                        Component.translatable(getRank().nameKey()),
                        Component.translatable(getTemperament().nameKey()),
                        String.format("%.0f", getNutrition()),
                        String.format("%.0f", getFatigue()),
                        String.format("%.0f", getMorale())
                ).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity instanceof ColonySymbiontEntity || entity instanceof MycelialChimeraEntity || entity instanceof PurifierDroneEntity) {
            return true;
        }
        if (entity instanceof Player player) {
            return this.ownerUuid == null || this.ownerUuid.equals(player.getUUID());
        }
        if (entity instanceof MycelialMob mm && mm.isAlliedEscort()) {
            return true;
        }
        return super.isAlliedTo(entity);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < pouch.getSlots(); i++) {
                ItemStack st = pouch.getStackInSlot(i);
                if (!st.isEmpty()) {
                    Containers.dropItemStack(serverLevel, getX(), getY(), getZ(), st.copy());
                    pouch.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            SovereignColonyData colony = getColonyData(serverLevel);
            if (colony != null) {
                if (damagedDuringCourierTrip) {
                    recordCourierDamageObservation(serverLevel, colony);
                }
                colony.recordWorkerLoss();
                colony.onWorkerDeath(this.getUUID());
                colony.recalculateMorale();
                SovereignColonySavedData.get(serverLevel).setDirty();
            }
        }
        super.die(source);
    }

    private void recordCourierDamageObservation(ServerLevel level, SovereignColonyData colony) {
        BlockPos site = colony.findNearestMemorySite(this.blockPosition(), 16.0D);
        if (site == null) {
            return;
        }
        ColonySiteMemory memory = colony.getSiteMemory(site);
        if (memory != null && memory.recordIncident(ColonySiteHabit.ESCORTED_COURIER, 1, level.getGameTime())) {
            level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                    site.getX() + 0.5D, site.getY() + 1.0D, site.getZ() + 0.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        SovereignColonySavedData.get(level).setDirty();
        damagedDuringCourierTrip = false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.COLONY_SYMBIONT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SPORELING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPORELING_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public boolean canRetreatToRest() {
        return getHealth() <= getMaxHealth() * getTemperament().retreatHealthRatio()
                || SovereignColonyEngine.shouldTriggerFatigueRest(getFatigue(), getNutrition(), this.getTarget() != null);
    }

    public long getLastWorkTick() {
        return lastWorkTick;
    }

    public long getLastRestTick() {
        return lastRestTick;
    }

    public void markRestedAt(long tick) {
        this.lastRestTick = tick;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) {
            tag.putUUID(TAG_OWNER, ownerUuid);
        }
        if (nexusPos != null) {
            tag.putBoolean(TAG_HAS_NEXUS, true);
            tag.putLong(TAG_NEXUS, nexusPos.asLong());
        }
        tag.putInt(TAG_CASTE, getCaste().index());
        tag.putInt(TAG_RANK, getRank().index());
        tag.putInt(TAG_TEMP, getTemperament().index());
        tag.putInt(TAG_XP, workerXp);
        tag.putFloat(TAG_NUTRITION, getNutrition());
        tag.putFloat(TAG_FATIGUE, getFatigue());
        tag.putFloat(TAG_MORALE, getMorale());
        tag.putBoolean(TAG_STANDBY, isStandby());
        tag.putLong(TAG_LAST_WORK_TICK, lastWorkTick);
        tag.putLong(TAG_LAST_REST_TICK, lastRestTick);
        net.minecraft.nbt.ListTag witnessTag = new net.minecraft.nbt.ListTag();
        for (ColonyWitnessMemory memory : siteWitnessMemories.values()) {
            witnessTag.add(memory.save());
        }
        tag.put(TAG_SITE_WITNESSES, witnessTag);
        tag.put(TAG_POUCH, pouch.serializeNBT(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ownerUuid = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        this.nexusPos = tag.getBoolean(TAG_HAS_NEXUS) ? BlockPos.of(tag.getLong(TAG_NEXUS)) : null;
        this.entityData.set(DATA_CASTE, SymbiontCaste.fromIndex(tag.getInt(TAG_CASTE)).index());
        this.workerXp = Math.max(0, tag.getInt(TAG_XP));
        this.entityData.set(DATA_RANK, SymbiontRank.fromIndex(tag.getInt(TAG_RANK)).index());
        this.entityData.set(DATA_TEMPERAMENT, SymbiontTemperament.fromIndex(tag.getInt(TAG_TEMP)).index());
        setNutrition(tag.contains(TAG_NUTRITION) ? tag.getFloat(TAG_NUTRITION) : 100.0F);
        setFatigue(tag.contains(TAG_FATIGUE) ? tag.getFloat(TAG_FATIGUE) : 0.0F);
        setMorale(tag.contains(TAG_MORALE) ? tag.getFloat(TAG_MORALE) : 80.0F);
        this.entityData.set(DATA_STANDBY, tag.getBoolean(TAG_STANDBY));
        this.lastWorkTick = tag.getLong(TAG_LAST_WORK_TICK);
        this.lastRestTick = tag.getLong(TAG_LAST_REST_TICK);
        siteWitnessMemories.clear();
        net.minecraft.nbt.ListTag witnessTag = tag.getList(TAG_SITE_WITNESSES, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < witnessTag.size(); i++) {
            ColonyWitnessMemory memory = ColonyWitnessMemory.load(witnessTag.getCompound(i));
            if (memory != null && memory.witnessId().equals(getUUID())) {
                siteWitnessMemories.put(memory.key(), memory);
            }
        }
        witnessMemoriesSyncedToColony = null;
        if (tag.contains(TAG_POUCH, CompoundTag.TAG_COMPOUND)) {
            pouch.deserializeNBT(this.registryAccess(), tag.getCompound(TAG_POUCH));
        }
        applyStatsFromState();
    }
}
