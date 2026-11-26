package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.block.RootSapMossBlock;
import com.alex.mycelialmind.entity.ai.brain.behaviour.FleeTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.Idle;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.MoveToWalkTarget;
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
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyPlayersSensor;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Peaceful gliding pollinator inhabiting the Twilight Throat and Root Cavern of
 * Mycelial Sinkholes. Transfers luminescence between the surface and subterranean
 * moss, replenishing depleted Root Sap Moss.
 */
public class SporeGliderEntity extends PathfinderMob implements HiveBrainOwner<SporeGliderEntity> {
    private static final String TAG_ANCHOR_X = "SinkholeAnchorX";
    private static final String TAG_ANCHOR_Y = "SinkholeAnchorY";
    private static final String TAG_ANCHOR_Z = "SinkholeAnchorZ";

    @Nullable
    private HiveBrain<SporeGliderEntity> hiveBrain;
    @Nullable
    private BlockPos homeAnchor;
    private int pollinateCooldown = 100;
    private int glidePhase = 0;

    public SporeGliderEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public void setHomeAnchor(@Nullable BlockPos anchor) {
        this.homeAnchor = anchor != null ? anchor.immutable() : null;
    }

    @Nullable
    public BlockPos getHomeAnchor() {
        return homeAnchor;
    }

    @Override
    protected Brain.Provider<?> brainProvider() {
        return HiveBrainFactory.createProvider(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<?> brain = super.makeBrain(dynamic);
        this.hiveBrain = HiveBrainFactory.initializeBrain(this, (Brain<SporeGliderEntity>) brain);
        return brain;
    }

    @Override
    @Nullable
    public HiveBrain<SporeGliderEntity> getHiveBrain() {
        return this.hiveBrain;
    }

    @Override
    public void setHiveBrain(@Nullable HiveBrain<SporeGliderEntity> brain) {
        this.hiveBrain = brain;
    }

    @Override
    public boolean shouldTickBrain(ServerLevel level, SporeGliderEntity entity) {
        boolean highPriority = getTarget() != null
                || HiveBrainUtils.hasMemory(this, ModMemoryTypes.INCOMING_PROJECTILE_POS.get());
        return HiveBrainOwner.evaluateStandardLod(level, entity, highPriority);
    }

    @Override
    public List<HiveSensor<SporeGliderEntity>> getSensors() {
        return List.of(
                new IncomingProjectilesSensor<>(),
                new NearbyPlayersSensor<SporeGliderEntity>().setRadius(12.0D),
                new HurtBySensor<>()
        );
    }

    @Override
    public BrainActivityGroup<SporeGliderEntity> getCoreTasks() {
        var projectileEvade = new CustomBehaviour<SporeGliderEntity>(
                glider -> HiveBrainBridge.performProjectileSidestep(glider, 1.15F, false));
        projectileEvade.startCondition(glider ->
                HiveBrainUtils.hasMemory(glider, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileEvade.cooldownFor(glider -> 30);

        return BrainActivityGroup.coreTasks(
                new LookAtTarget<>(),
                projectileEvade,
                new MoveToWalkTarget<>()
        );
    }

    @Override
    public BrainActivityGroup<SporeGliderEntity> getIdleTasks() {
        var glideLoop = new CustomBehaviour<SporeGliderEntity>(glider -> {
            if (glider.level().isClientSide()) {
                return;
            }
            BlockPos center = glider.homeAnchor != null ? glider.homeAnchor : glider.blockPosition();
            glider.glidePhase = (glider.glidePhase + 1) % 360;
            double angle = (glider.glidePhase * 0.45D) + (glider.getId() * 0.7D);
            double verticalSweep = Math.sin(angle * 0.5D) * 5.0D;
            double tx = center.getX() + 0.5D + Math.cos(angle) * 3.2D;
            double ty = center.getY() + 1.5D + verticalSweep;
            double tz = center.getZ() + 0.5D + Math.sin(angle) * 3.2D;
            glider.getNavigation().moveTo(tx, ty, tz, 1.0D);
        });
        glideLoop.cooldownFor(glider -> 25);

        return BrainActivityGroup.idleTasks(
                glideLoop,
                new SetPlayerLookTarget<>(),
                new Idle<SporeGliderEntity>().runFor(glider -> 30)
        );
    }

    @Override
    public Map<Activity, BrainActivityGroup<? extends SporeGliderEntity>> getAdditionalTasks() {
        BrainActivityGroup<SporeGliderEntity> panicGroup = new BrainActivityGroup<SporeGliderEntity>(Activity.PANIC)
                .onlyStartWithMemoryStatus(MemoryModuleType.HURT_BY_ENTITY, MemoryStatus.VALUE_PRESENT)
                .behaviours(
                        new FleeTarget<SporeGliderEntity>().fleeDistance(12.0F).speedModifier(1.3F),
                        new MoveToWalkTarget<>()
                );
        return Map.of(Activity.PANIC, panicGroup);
    }

    @Override
    public List<Activity> getActivityPriorities() {
        return List.of(Activity.PANIC, Activity.IDLE);
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
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.homeAnchor == null) {
            this.homeAnchor = this.blockPosition();
        }
        tickBrain(this);
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.pollinateCooldown > 0) {
                this.pollinateCooldown--;
            } else if (tryPollinateNearbyMoss(serverLevel)) {
                this.pollinateCooldown = 160;
            } else {
                this.pollinateCooldown = 60;
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.tickCount % 10 == 0) {
            this.level().addParticle(ModParticles.BIO_LUME.get(),
                    this.getX(), this.getY() + 0.15D, this.getZ(), 0.0D, -0.01D, 0.0D);
        }
    }

    private boolean tryPollinateNearbyMoss(ServerLevel level) {
        BlockPos origin = this.blockPosition();
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-5, -5, -5), origin.offset(5, 2, 5))) {
            BlockState state = level.getBlockState(candidate);
            if (state.is(ModBlocks.ROOT_SAP_MOSS.get()) && !state.getValue(RootSapMossBlock.SAP_READY)) {
                level.setBlockAndUpdate(candidate, state.setValue(RootSapMossBlock.SAP_READY, true));
                level.updateNeighbourForOutputSignal(candidate, state.getBlock());
                level.sendParticles(ModParticles.BIO_LUME.get(),
                        candidate.getX() + 0.5D, candidate.getY() + 1.05D, candidate.getZ() + 0.5D,
                        8, 0.3D, 0.2D, 0.3D, 0.02D);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // Gliders ignore fall damage during vertical throat flight
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL) || super.isInvulnerableTo(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.homeAnchor != null) {
            tag.putInt(TAG_ANCHOR_X, this.homeAnchor.getX());
            tag.putInt(TAG_ANCHOR_Y, this.homeAnchor.getY());
            tag.putInt(TAG_ANCHOR_Z, this.homeAnchor.getZ());
        }
        saveBrainState(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_ANCHOR_X) && tag.contains(TAG_ANCHOR_Y) && tag.contains(TAG_ANCHOR_Z)) {
            this.homeAnchor = new BlockPos(tag.getInt(TAG_ANCHOR_X), tag.getInt(TAG_ANCHOR_Y), tag.getInt(TAG_ANCHOR_Z));
        }
        loadBrainState(tag);
    }

    @Override
    protected float getSoundVolume() {
        return 0.68F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.MOB_STEP_HOVER.get(), 0.14F, 1.15F + this.random.nextFloat() * 0.10F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SPORE_GLIDER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SPORE_GLIDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPORE_GLIDER_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.36D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }
}
