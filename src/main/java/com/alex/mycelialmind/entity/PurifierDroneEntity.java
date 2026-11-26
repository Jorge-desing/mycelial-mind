package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.ai.brain.behaviour.Idle;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetPlayerLookTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrain;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainFactory;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainOwner;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.HurtBySensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.IncomingProjectilesSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyLivingEntitySensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyPlayersSensor;
import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.mojang.serialization.Dynamic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Allied hovering Purifier Drone deployed by players via PurifierDroneCoreItem.
 * Follows its owner or patrols nearby Pylons, fires NeutralizingSporeProjectiles
 * at MycelialMobs, and cleanses Creeping Mycelium beneath it.
 */
public class PurifierDroneEntity extends PathfinderMob implements HiveBrainOwner<PurifierDroneEntity> {
    private static final EntityDataAccessor<Boolean> DATA_PATROL_MODE =
            SynchedEntityData.defineId(PurifierDroneEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_OWNER = "OwnerUUID";
    private static final String TAG_PATROL = "PatrolMode";
    private static final String TAG_JAMMED = "JammedTicks";

    @Nullable
    private UUID ownerUuid;
    private int fireCooldown = 15;
    private int cleanseCooldown = 20;
    private int jammedTicks = 0;
    @Nullable
    private HiveBrain<PurifierDroneEntity> hiveBrain;

    public int getJammedTicks() {
        return jammedTicks;
    }

    public boolean isJammed() {
        return jammedTicks > 0;
    }

    public void setJammedTicks(int ticks) {
        boolean wasJammed = isJammed();
        this.jammedTicks = Math.max(this.jammedTicks, Math.max(0, ticks));
        if (!wasJammed && this.jammedTicks > 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), ModSounds.CORROSIVE_JAM.get(), SoundSource.NEUTRAL, 0.85F, 1.2F);
        }
    }

    public boolean clearJam() {
        if (this.jammedTicks <= 0) {
            return false;
        }
        this.jammedTicks = 0;
        return true;
    }

    public PurifierDroneEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    @Override
    protected Brain.Provider<?> brainProvider() {
        return HiveBrainFactory.createProvider(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<?> brain = super.makeBrain(dynamic);
        this.hiveBrain = HiveBrainFactory.initializeBrain(this, (Brain<PurifierDroneEntity>) brain);
        return brain;
    }

    @Override
    @Nullable
    public HiveBrain<PurifierDroneEntity> getHiveBrain() {
        return this.hiveBrain;
    }

    @Override
    public void setHiveBrain(@Nullable HiveBrain<PurifierDroneEntity> brain) {
        this.hiveBrain = brain;
    }

    @Override
    public boolean shouldTickBrain(ServerLevel level, PurifierDroneEntity entity) {
        boolean highPriority = getTarget() != null
                || isJammed()
                || !isPatrolMode()
                || HiveBrainUtils.hasMemory(this, ModMemoryTypes.INCOMING_PROJECTILE_POS.get());
        return HiveBrainOwner.evaluateStandardLod(level, entity, highPriority);
    }

    @Override
    public List<HiveSensor<PurifierDroneEntity>> getSensors() {
        return List.of(
                new NearbyLivingEntitySensor<PurifierDroneEntity>()
                        .setRadius(18.0D)
                        .setPredicate((target, drone) -> target instanceof MycelialMob mm && !mm.isAlliedEscort()),
                new NearbyPlayersSensor<PurifierDroneEntity>().setRadius(16.0D),
                new HurtBySensor<>(),
                new IncomingProjectilesSensor<>()
        );
    }

    @Override
    public BrainActivityGroup<PurifierDroneEntity> getCoreTasks() {
        var projectileEvade = new CustomBehaviour<PurifierDroneEntity>(
                drone -> HiveBrainBridge.performProjectileSidestep(drone, 1.2F, false));
        projectileEvade.startCondition(drone -> !drone.isJammed()
                && HiveBrainUtils.hasMemory(drone, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileEvade.cooldownFor(drone -> 30);

        return BrainActivityGroup.coreTasks(
                new LookAtTarget<>(),
                projectileEvade
        );
    }

    @Override
    public BrainActivityGroup<PurifierDroneEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new SetPlayerLookTarget<>(),
                new Idle<PurifierDroneEntity>().runFor(drone -> 40)
        );
    }

    @Override
    public Map<Activity, BrainActivityGroup<? extends PurifierDroneEntity>> getAdditionalTasks() {
        BrainActivityGroup<PurifierDroneEntity> restGroup = new BrainActivityGroup<PurifierDroneEntity>(Activity.REST)
                .onlyStartWithMemoryStatus(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT)
                .behaviours(new Idle<PurifierDroneEntity>().runFor(drone -> 40));
        return Map.of(Activity.REST, restGroup);
    }

    @Override
    public List<Activity> getActivityPriorities() {
        return List.of(Activity.REST, Activity.IDLE);
    }

    public void syncDroneBrainState(ServerLevel serverLevel) {
        if (isJammed()) {
            if (!HiveBrainUtils.hasMemory(this, MemoryModuleType.HOME)) {
                HiveBrainUtils.setMemory(this, MemoryModuleType.HOME,
                        GlobalPos.of(serverLevel.dimension(), this.blockPosition()));
            }
        } else {
            HiveBrainUtils.clearMemory(this, MemoryModuleType.HOME);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.level() instanceof ServerLevel serverLevel) {
            syncDroneBrainState(serverLevel);
        }
        tickBrain(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PATROL_MODE, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DroneFollowOrPatrolGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    public void setOwner(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public boolean isPatrolMode() {
        return this.entityData.get(DATA_PATROL_MODE);
    }

    public void setPatrolMode(boolean patrol) {
        this.entityData.set(DATA_PATROL_MODE, patrol);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.BIO_SCANNER.get())) {
            return InteractionResult.PASS;
        }
        if (ownerUuid == null || ownerUuid.equals(player.getUUID())) {
            boolean repairMaterial = stack.is(ModItems.CHITIN_PLATE.get())
                    || stack.is(Items.IRON_INGOT)
                    || stack.is(ModItems.SPORE_ANTIDOTE.get())
                    || stack.is(ModItems.CONCENTRATED_ANTIDOTE.get());
            if (repairMaterial && (this.getHealth() < this.getMaxHealth() || this.isJammed())) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    this.heal(15.0F);
                    this.jammedTicks = 0;
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                            this.getX(), this.getY() + 0.5, this.getZ(),
                            14, 0.35, 0.35, 0.35, 0.04);
                    serverLevel.playSound(null, this.blockPosition(), ModSounds.PURIFICATION_TURRET_REPAIR.get(), SoundSource.NEUTRAL, 0.9F, 1.15F);
                    player.sendSystemMessage(Component.translatable("message.mycelialmind.drone_repaired",
                            String.format("%.0f/%.0f", this.getHealth(), this.getMaxHealth())).withStyle(ChatFormatting.GREEN));
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
            if (!this.level().isClientSide()) {
                boolean next = !isPatrolMode();
                setPatrolMode(next);
                player.sendSystemMessage(Component.translatable(
                        next ? "message.mycelialmind.drone_mode_patrol" : "message.mycelialmind.drone_mode_follow")
                        .withStyle(ChatFormatting.AQUA));
                this.level().playSound(null, this.blockPosition(), ModSounds.PURIFIER_DRONE_DEPLOY.get(), SoundSource.NEUTRAL, 0.7F, 1.3F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.tickCount % 6 == 0) {
                this.level().addParticle(ModParticles.PURIFIER_CROSS.get(),
                        this.getX(), this.getY() + 0.1, this.getZ(), 0.0, -0.02, 0.0);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.fireCooldown > 0) this.fireCooldown--;
        if (this.cleanseCooldown > 0) this.cleanseCooldown--;
        if (this.jammedTicks > 0) {
            this.jammedTicks--;
            if (this.tickCount % 6 == 0) {
                serverLevel.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                        this.getX(), this.getY() + 0.3, this.getZ(),
                        3, 0.2, 0.2, 0.2, 0.02);
            }
        } else if (this.fireCooldown <= 0) {
            if (tryFireAtNearestMob(serverLevel)) {
                this.fireCooldown = 25;
            } else {
                this.fireCooldown = 10;
            }
        }
        if (this.cleanseCooldown <= 0) {
            cleanseMyceliumBeneath(serverLevel);
            this.cleanseCooldown = 40;
        }
    }

    public boolean tryFireAtNearestMob(ServerLevel level) {
        Player owner = this.ownerUuid != null ? level.getPlayerByUUID(this.ownerUuid) : null;
        List<MycelialMob> mobs = level.getEntitiesOfClass(
                MycelialMob.class, this.getBoundingBox().inflate(20.0D),
                m -> m.isAlive() && !m.isAlliedEscort() && (owner == null || !MycelialMob.isFriendlyToPlayer(m, owner))
        );
        Optional<MycelialMob> nearest = mobs.stream().min(Comparator.comparingDouble(m -> {
            double weight = m instanceof LeechParasiteEntity ? 0.30D
                    : (m.getCommandRole() == SynapticCommandSystem.CommandRole.LEADER ? 0.45D : 1.0D);
            return this.distanceToSqr(m) * weight;
        }));
        if (nearest.isEmpty()) {
            return false;
        }
        MycelialMob target = nearest.get();
        NeutralizingSporeProjectile proj = new NeutralizingSporeProjectile(level, this);
        proj.setPos(this.getX(), this.getY() + 0.3, this.getZ());
        proj.shoot(
                target.getX() - this.getX(),
                target.getY(0.5) - (this.getY() + 0.3),
                target.getZ() - this.getZ(),
                1.45F, 1.0F
        );
        if (!level.addFreshEntity(proj)) {
            return false;
        }
        level.playSound(null, this.blockPosition(), ModSounds.PURIFIER_DRONE_FIRE.get(), SoundSource.NEUTRAL, 0.85F, 1.0F);
        return true;
    }

    public boolean cleanseMyceliumBeneath(ServerLevel level) {
        BlockPos feet = this.blockPosition();
        for (int dy = 0; dy >= -3; dy--) {
            BlockPos check = feet.offset(0, dy, 0);
            BlockState state = level.getBlockState(check);
            if (state.is(ModBlocks.CREEPING_MYCELIUM.get())) {
                level.setBlockAndUpdate(check, Blocks.GRASS_BLOCK.defaultBlockState());
                level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                        check.getX() + 0.5, check.getY() + 1.05, check.getZ() + 0.5,
                        8, 0.25, 0.15, 0.25, 0.02);
                return true;
            }
            if (state.is(ModBlocks.EPHEMERAL_CHITIN.get()) || state.is(ModBlocks.ANTIBODY_CYST.get())) {
                level.removeBlock(check, false);
                level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                        check.getX() + 0.5, check.getY() + 0.5, check.getZ() + 0.5,
                        8, 0.25, 0.15, 0.25, 0.02);
                return true;
            }
            if (com.alex.mycelialmind.hive.HyphalSiegeDirector.cleanseHyphalVeinAt(level, check, 5.0D)) {
                level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                        check.getX() + 0.5, check.getY() + 0.2, check.getZ() + 0.5,
                        8, 0.25, 0.15, 0.25, 0.02);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().equals(ModEffects.MYCELIAL_INFECTION)
                || effect.getEffect().equals(ModEffects.SPORE_ROT)
                || effect.getEffect().equals(MobEffects.POISON)) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID(TAG_OWNER, this.ownerUuid);
        }
        tag.putBoolean(TAG_PATROL, isPatrolMode());
        if (this.jammedTicks > 0) {
            tag.putInt(TAG_JAMMED, this.jammedTicks);
        }
        saveBrainState(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) {
            this.ownerUuid = tag.getUUID(TAG_OWNER);
        }
        if (tag.contains(TAG_PATROL)) {
            setPatrolMode(tag.getBoolean(TAG_PATROL));
        }
        this.jammedTicks = tag.contains(TAG_JAMMED) ? Math.max(0, tag.getInt(TAG_JAMMED)) : 0;
        loadBrainState(tag);
    }

    @Override
    protected float getSoundVolume() {
        return 0.72F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 180;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.MOB_STEP_HOVER.get(), 0.18F, 0.96F + this.random.nextFloat() * 0.08F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PURIFIER_DRONE_HUM.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.PURIFIER_DRONE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PURIFIER_DRONE_DEATH.get();
    }

    public SoundEvent resolveAmbientSound() {
        return getAmbientSound();
    }

    public SoundEvent resolveHurtSound(DamageSource damageSource) {
        return getHurtSound(damageSource);
    }

    public SoundEvent resolveDeathSound() {
        return getDeathSound();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.45D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    private static final class DroneFollowOrPatrolGoal extends Goal {
        private final PurifierDroneEntity drone;
        private int timer = 0;

        private DroneFollowOrPatrolGoal(PurifierDroneEntity drone) {
            this.drone = drone;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            this.timer++;
            if (this.timer % 15 != 0 || !(this.drone.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (this.drone.isPatrolMode()) {
                var pylons = PurificationZoneTracker.get(serverLevel).getPylonsWithin(this.drone.blockPosition(), 48, 1);
                if (!pylons.isEmpty()) {
                    BlockPos p = pylons.get(0);
                    double ang = (this.drone.tickCount * 0.05) % (Math.PI * 2.0);
                    this.drone.getNavigation().moveTo(
                            p.getX() + 0.5 + Math.cos(ang) * 6.0,
                            p.getY() + 2.5,
                            p.getZ() + 0.5 + Math.sin(ang) * 6.0,
                            1.1D);
                    return;
                }
            }
            if (this.drone.ownerUuid != null) {
                Player owner = serverLevel.getPlayerByUUID(this.drone.ownerUuid);
                if (owner != null && owner.isAlive() && this.drone.distanceToSqr(owner) > 20.0D) {
                    this.drone.getNavigation().moveTo(owner.getX(), owner.getY() + 2.0, owner.getZ(), 1.15D);
                }
            }
        }
    }
}
