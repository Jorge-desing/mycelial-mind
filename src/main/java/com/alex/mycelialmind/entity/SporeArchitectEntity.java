package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.CreepingMyceliumBlock;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AvoidEntity;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.MoveToWalkTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.FirstApplicableBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveNodeRegenBehaviour;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Autonomous colony builder unit that marches outward from mature nodes,
 * seeds Creeping Mycelium patches, and erects new Relay Nodes outside Pylon protection.
 */
public class SporeArchitectEntity extends MycelialMob {
    private static final String TAG_ORIGIN_POS = "OriginNodePos";
    private static final String TAG_BUILD_COOLDOWN = "BuildCooldown";

    @Nullable
    private BlockPos originNodePos;
    private int buildCooldownTicks = 40;

    public SporeArchitectEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public void setOriginNode(@Nullable BlockPos pos) {
        this.originNodePos = pos == null ? null : pos.immutable();
    }

    @Nullable
    public BlockPos getOriginNode() {
        return originNodePos;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.alex.mycelialmind.entity.ai.TacticalTerraformingGoal(this));
        this.goalSelector.addGoal(2, new BuildColonyGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public List<HiveSensor<MycelialMob>> getSensors() {
        return HiveBrainBridge.createDefaultHiveSensorsWithProjectiles();
    }

    @Override
    public BrainActivityGroup<MycelialMob> getCoreTasks() {
        var avoidPlayers = new AvoidEntity<MycelialMob>()
                .avoiding(candidate -> candidate instanceof Player player
                        && !player.isCreative()
                        && !player.isSpectator()
                        && !MycelialMob.isFriendlyToPlayer(this, player))
                .noCloserThan(7.0F)
                .speedModifier(1.18F);
        avoidPlayers.startCondition(mob -> !mob.isDesynced());

        var projectileSidestep = new CustomBehaviour<MycelialMob>(
                mob -> HiveBrainBridge.performProjectileSidestep(mob, 1.15F, true));
        projectileSidestep.startCondition(mob -> !mob.isDesynced()
                && HiveBrainUtils.hasMemory(mob, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileSidestep.cooldownFor(mob -> 35);

        return BrainActivityGroup.coreTasks(
                new LookAtTarget<>(),
                new MoveToWalkTarget<>(),
                new FirstApplicableBehaviour<>(avoidPlayers, projectileSidestep)
        );
    }

    @Override
    public Map<Activity, BrainActivityGroup<? extends MycelialMob>> getAdditionalTasks() {
        BrainActivityGroup<MycelialMob> investigate = new BrainActivityGroup<MycelialMob>(Activity.INVESTIGATE)
                .onlyStartWithMemoryStatus(ModMemoryTypes.NEAREST_NODE_POS.get(), MemoryStatus.VALUE_PRESENT)
                .onlyStartWithMemoryStatus(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT)
                .behaviours(new HiveNodeRegenBehaviour());
        return Map.of(Activity.INVESTIGATE, investigate);
    }

    @Override
    public List<Activity> getActivityPriorities() {
        return List.of(Activity.FIGHT, Activity.INVESTIGATE, Activity.IDLE);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.buildCooldownTicks > 0) {
            this.buildCooldownTicks--;
        }
        if (this.level().isClientSide() && this.tickCount % 10 == 0) {
            this.level().addParticle(ModParticles.SPORE_MOTE.get(),
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                    this.getY() + 0.9,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                    0.0, 0.02, 0.0);
        }
    }

    /**
     * Attempts to seed a 3x3 Creeping Mycelium patch around ground level and erect a Relay Node
     * at {@code nodePos} if Pylon protection, block entities, spacing, and node cap allow it.
     */
    public boolean attemptPlantColonyAt(ServerLevel level, BlockPos nodePos) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        if (tracker.isProtected(nodePos)) {
            return false;
        }
        BlockPos groundCenter = nodePos.below();
        boolean seededAny = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = groundCenter.offset(dx, 0, dz);
                if (tracker.isProtected(p)) {
                    continue;
                }
                BlockState state = level.getBlockState(p);
                if (CreepingMyceliumBlock.canInfect(level, p, state)) {
                    level.setBlockAndUpdate(p, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
                    seededAny = true;
                }
            }
        }

        boolean placedNode = false;
        BlockState atNode = level.getBlockState(nodePos);
        BlockState groundAfter = level.getBlockState(groundCenter);
        boolean solidSupport = !groundAfter.liquid() && groundAfter.isFaceSturdy(level, groundCenter, Direction.UP);
        if (solidSupport
                && (atNode.isAir() || atNode.canBeReplaced())
                && level.getBlockEntity(nodePos) == null
                && tracker.countNodes() < MycelialConfig.MAX_AUTONOMOUS_NODES.get()
                && !tracker.hasAnyNodeWithin(nodePos, MycelialConfig.MIN_NODE_SPACING.get())) {
            level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
            tracker.addNode(nodePos, SynapticNodeType.RELAY);
            placedNode = true;
            level.sendParticles(ModParticles.HIVE_PULSE.get(), nodePos.getX() + 0.5, nodePos.getY() + 0.6, nodePos.getZ() + 0.5,
                    1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ModParticles.SPORE_MOTE.get(), nodePos.getX() + 0.5, nodePos.getY() + 0.6, nodePos.getZ() + 0.5,
                    16, 0.4, 0.5, 0.4, 0.05);
            level.playSound(null, nodePos, ModSounds.SPORE_ARCHITECT_CONSTRUCT.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
        } else if (seededAny) {
            level.playSound(null, groundCenter, ModSounds.SPORE_ARCHITECT_CONSTRUCT.get(), SoundSource.HOSTILE, 0.55F, 1.15F);
        }
        if (seededAny || placedNode) {
            this.buildCooldownTicks = 240;
            return true;
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.originNodePos != null) {
            tag.putLong(TAG_ORIGIN_POS, this.originNodePos.asLong());
        }
        tag.putInt(TAG_BUILD_COOLDOWN, this.buildCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_ORIGIN_POS)) {
            this.originNodePos = BlockPos.of(tag.getLong(TAG_ORIGIN_POS));
        }
        if (tag.contains(TAG_BUILD_COOLDOWN)) {
            this.buildCooldownTicks = tag.getInt(TAG_BUILD_COOLDOWN);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SPORE_ARCHITECT_PULSE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SPORE_ARCHITECT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPORE_ARCHITECT_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    private static final class BuildColonyGoal extends Goal {
        private final SporeArchitectEntity mob;
        @Nullable
        private BlockPos targetPos;
        private int pathTimer;
        private int searchCooldown;

        private BuildColonyGoal(SporeArchitectEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!MycelialConfig.AUTONOMOUS_EXPANSION_ENABLED.get()) {
                return false;
            }
            if (this.searchCooldown > 0) {
                this.searchCooldown--;
                return false;
            }
            if (this.mob.buildCooldownTicks > 0 || this.mob.getTarget() != null) {
                return false;
            }
            if (!(this.mob.level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            this.targetPos = pickCandidate(serverLevel);
            if (this.targetPos == null) {
                this.searchCooldown = 40;
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetPos != null
                    && this.mob.buildCooldownTicks <= 0
                    && this.mob.getTarget() == null
                    && this.pathTimer < 200;
        }

        @Override
        public void start() {
            this.pathTimer = 0;
            if (this.targetPos != null) {
                this.mob.getNavigation().moveTo(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5, 1.05D);
            }
        }

        @Override
        public void tick() {
            this.pathTimer++;
            if (this.targetPos == null || !(this.mob.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (this.mob.blockPosition().distSqr(this.targetPos) <= 9.0D) {
                this.mob.attemptPlantColonyAt(serverLevel, this.targetPos);
                this.targetPos = null;
                this.mob.getNavigation().stop();
            } else if (this.pathTimer % 20 == 0) {
                this.mob.getNavigation().moveTo(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5, 1.05D);
            }
        }

        @Override
        public void stop() {
            this.targetPos = null;
        }

        @Nullable
        private BlockPos pickCandidate(ServerLevel level) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
            BlockPos anchor = this.mob.originNodePos != null ? this.mob.originNodePos : this.mob.blockPosition();
            for (int attempt = 0; attempt < 6; attempt++) {
                double angle = this.mob.getRandom().nextDouble() * Math.PI * 2.0;
                double dist = 16.0 + this.mob.getRandom().nextDouble() * 12.0;
                int x = anchor.getX() + (int) Math.round(Math.cos(angle) * dist);
                int z = anchor.getZ() + (int) Math.round(Math.sin(angle) * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos candidate = new BlockPos(x, y, z);
                BlockPos belowPos = candidate.below();
                BlockState belowState = level.getBlockState(belowPos);
                if (!tracker.isProtected(candidate)
                        && !belowState.liquid()
                        && belowState.isFaceSturdy(level, belowPos, Direction.UP)
                        && level.getBlockState(candidate).canBeReplaced()) {
                    return candidate;
                }
            }
            return null;
        }
    }
}
