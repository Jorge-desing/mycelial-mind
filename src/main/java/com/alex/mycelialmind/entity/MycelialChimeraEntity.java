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
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainProfile;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.HurtBySensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.IncomingProjectilesSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyLivingEntitySensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyPlayersSensor;
import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.item.GeneticSampleItem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialText;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Allied modular bio-forged Chimera mount and combat guardian.
 * Randomizes 2 genes (Chassis + Autonomous Active Mutation) on hatch and supports
 * live re-mutation, rank evolution (BASE -> MUTATED -> ELITE), and 3 command modes.
 */
public class MycelialChimeraEntity extends PathfinderMob implements HiveBrainOwner<MycelialChimeraEntity> {
    public enum CommandMode {
        FOLLOW(0, "FOLLOW"),
        PATROL(1, "PATROL"),
        WAIT(2, "WAIT");

        private final int id;
        private final String label;

        CommandMode(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public int id() {
            return id;
        }

        public String label() {
            return label;
        }

        public CommandMode next() {
            CommandMode[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }

        public static CommandMode fromId(int id) {
            for (CommandMode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return FOLLOW;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_CHASSIS_GENE =
            SynchedEntityData.defineId(MycelialChimeraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MUTATION_GENE =
            SynchedEntityData.defineId(MycelialChimeraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COMMAND_MODE =
            SynchedEntityData.defineId(MycelialChimeraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EVOLUTION_RANK =
            SynchedEntityData.defineId(MycelialChimeraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PROMOTION_TICK =
            SynchedEntityData.defineId(MycelialChimeraEntity.class, EntityDataSerializers.INT);
    private static final int PROMOTION_FLASH_TICKS = 30;

    private static final String TAG_OWNER = "OwnerUUID";
    private static final String TAG_CHASSIS = "ChassisGene";
    private static final String TAG_MUTATION = "MutationGene";
    private static final String TAG_MODE = "CommandMode";
    private static final String TAG_RANK = "EvolutionRank";
    private static final String TAG_HOME_X = "HomeX";
    private static final String TAG_HOME_Y = "HomeY";
    private static final String TAG_HOME_Z = "HomeZ";

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private BlockPos homePos;
    private int mutationCooldownTicks = 40;
    private int rerollCooldownTicks = 0;
    private int healCooldownTicks = 0;
    @Nullable
    private HiveBrain<MycelialChimeraEntity> hiveBrain;

    public MycelialChimeraEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Brain.Provider<?> brainProvider() {
        return HiveBrainFactory.createProvider(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<?> brain = super.makeBrain(dynamic);
        this.hiveBrain = HiveBrainFactory.initializeBrain(this, (Brain<MycelialChimeraEntity>) brain);
        return brain;
    }

    @Override
    @Nullable
    public HiveBrain<MycelialChimeraEntity> getHiveBrain() {
        return this.hiveBrain;
    }

    @Override
    public void setHiveBrain(@Nullable HiveBrain<MycelialChimeraEntity> brain) {
        this.hiveBrain = brain;
    }

    @Override
    public boolean replaceVanillaBrain(Brain<MycelialChimeraEntity> newBrain) {
        if (newBrain == null) {
            return false;
        }
        this.brain = newBrain;
        return true;
    }

    @Override
    public Map<String, HiveBrainProfile<MycelialChimeraEntity>> getBrainProfiles() {
        return Map.of(
                "wait", HiveBrainProfile.<MycelialChimeraEntity>builder("wait")
                        .activityPriorities(List.of(Activity.REST, Activity.IDLE))
                        .disableSensors(IncomingProjectilesSensor.class)
                        .lodIntervalMultiplier(2)
                        .build(),
                "patrol", HiveBrainProfile.<MycelialChimeraEntity>builder("patrol")
                        .lodIntervalMultiplier(1)
                        .build(),
                "follow", HiveBrainProfile.<MycelialChimeraEntity>builder("follow")
                        .lodIntervalMultiplier(0)
                        .build()
        );
    }

    @Override
    public boolean shouldTickBrain(ServerLevel level, MycelialChimeraEntity entity) {
        boolean highPriority = getTarget() != null
                || isVehicle()
                || getCommandMode() == CommandMode.FOLLOW
                || HiveBrainUtils.hasMemory(this, ModMemoryTypes.INCOMING_PROJECTILE_POS.get());
        return HiveBrainOwner.evaluateStandardLod(level, entity, highPriority);
    }

    @Override
    public List<HiveSensor<MycelialChimeraEntity>> getSensors() {
        return List.of(
                new NearbyLivingEntitySensor<MycelialChimeraEntity>()
                        .setRadius(20.0D)
                        .setPredicate((target, entity) -> target instanceof Monster && entity.isValidChimeraHostile(target)),
                new NearbyPlayersSensor<MycelialChimeraEntity>().setRadius(16.0D),
                new HurtBySensor<>(),
                new IncomingProjectilesSensor<>()
        );
    }

    @Override
    public BrainActivityGroup<MycelialChimeraEntity> getCoreTasks() {
        var projectileEvade = new CustomBehaviour<MycelialChimeraEntity>(
                chimera -> HiveBrainBridge.performProjectileSidestep(chimera, 1.2F, false));
        projectileEvade.startCondition(chimera -> !chimera.isVehicle()
                && chimera.getCommandMode() != CommandMode.WAIT
                && HiveBrainUtils.hasMemory(chimera, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()));
        projectileEvade.cooldownFor(chimera -> 30);

        return BrainActivityGroup.coreTasks(
                new LookAtTarget<>(),
                projectileEvade
        );
    }

    @Override
    public BrainActivityGroup<MycelialChimeraEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new SetPlayerLookTarget<>(),
                new Idle<MycelialChimeraEntity>().runFor(chimera -> 40)
        );
    }

    @Override
    public Map<Activity, BrainActivityGroup<? extends MycelialChimeraEntity>> getAdditionalTasks() {
        BrainActivityGroup<MycelialChimeraEntity> restGroup = new BrainActivityGroup<MycelialChimeraEntity>(Activity.REST)
                .onlyStartWithMemoryStatus(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT)
                .behaviours(new SetPlayerLookTarget<>(), new Idle<MycelialChimeraEntity>().runFor(chimera -> 40));
        return Map.of(Activity.REST, restGroup);
    }

    @Override
    public List<Activity> getActivityPriorities() {
        return List.of(Activity.REST, Activity.IDLE);
    }

    public void syncCommandBrainState(ServerLevel serverLevel) {
        CommandMode mode = getCommandMode();
        if (mode == CommandMode.WAIT) {
            if (!HiveBrainUtils.hasMemory(this, MemoryModuleType.HOME)) {
                HiveBrainUtils.setMemory(this, MemoryModuleType.HOME,
                        GlobalPos.of(serverLevel.dimension(), this.blockPosition()));
            }
        } else {
            HiveBrainUtils.clearMemory(this, MemoryModuleType.HOME);
        }
        String desiredProfile = switch (mode) {
            case WAIT -> "wait";
            case PATROL -> "patrol";
            case FOLLOW -> "follow";
        };
        applyBrainProfile(serverLevel, this, desiredProfile);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.level() instanceof ServerLevel serverLevel) {
            syncCommandBrainState(serverLevel);
        }
        tickBrain(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHASSIS_GENE, AdaptationTrait.MELEE_THORNS.ordinal());
        builder.define(DATA_MUTATION_GENE, AdaptationTrait.PROJECTILE_DEFLECT.ordinal());
        builder.define(DATA_COMMAND_MODE, CommandMode.FOLLOW.id());
        builder.define(DATA_EVOLUTION_RANK, EvolutionRank.BASE.ordinal());
        builder.define(DATA_PROMOTION_TICK, -1000);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new ChimeraFollowOrPatrolGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse()
                        && MycelialChimeraEntity.this.isValidChimeraHostile(
                                MycelialChimeraEntity.this.getLastHurtByMob()
                        );
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Monster.class, 10, true, false,
                this::isValidChimeraHostile
        ));
    }

    public boolean isValidChimeraHostile(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (target instanceof Player player && ownerUuid != null && ownerUuid.equals(player.getUUID())) {
            return false;
        }
        if (target instanceof MycelialMob mm) {
            if (mm.isAlliedEscort()) {
                return false;
            }
            if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(this.ownerUuid);
                if (owner != null && MycelialMob.isFriendlyToPlayer(mm, owner)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setOwnerUUID(@Nullable UUID ownerUuid) {
        if (this.ownerUuid != null && !this.ownerUuid.equals(ownerUuid)
                && this.level() instanceof ServerLevel serverLevel) {
            ChimeraOwnershipSavedData.get(serverLevel.getServer()).unregister(this.getUUID());
        }
        this.ownerUuid = ownerUuid;
        if (ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
            ChimeraOwnershipSavedData.get(serverLevel.getServer()).register(this.getUUID(), ownerUuid);
        }
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUuid;
    }

    public boolean isOwner(Player player) {
        return ownerUuid == null || ownerUuid.equals(player.getUUID());
    }

    public void setHomePos(@Nullable BlockPos pos) {
        this.homePos = pos == null ? null : pos.immutable();
    }

    public AdaptationTrait getChassisGene() {
        AdaptationTrait[] traits = AdaptationTrait.values();
        int idx = Math.max(0, Math.min(traits.length - 1, this.entityData.get(DATA_CHASSIS_GENE)));
        return traits[idx];
    }

    public AdaptationTrait getMutationGene() {
        AdaptationTrait[] traits = AdaptationTrait.values();
        int idx = Math.max(0, Math.min(traits.length - 1, this.entityData.get(DATA_MUTATION_GENE)));
        return traits[idx];
    }

    public void setGenes(AdaptationTrait chassis, AdaptationTrait mutation) {
        this.entityData.set(DATA_CHASSIS_GENE, chassis.ordinal());
        this.entityData.set(DATA_MUTATION_GENE, mutation.ordinal());
        applyGeneticAttributes();
    }

    public boolean hasGene(AdaptationTrait trait) {
        return getChassisGene() == trait || getMutationGene() == trait;
    }

    public CommandMode getCommandMode() {
        return CommandMode.fromId(this.entityData.get(DATA_COMMAND_MODE));
    }

    public void setCommandMode(CommandMode mode) {
        this.entityData.set(DATA_COMMAND_MODE, mode.id());
    }

    public EvolutionRank getEvolutionRank() {
        EvolutionRank[] ranks = EvolutionRank.values();
        int idx = Math.max(0, Math.min(ranks.length - 1, this.entityData.get(DATA_EVOLUTION_RANK)));
        return ranks[idx];
    }

    public void promoteRank(EvolutionRank targetRank) {
        if (targetRank.ordinal() <= getEvolutionRank().ordinal()) {
            return;
        }
        this.entityData.set(DATA_EVOLUTION_RANK, targetRank.ordinal());
        this.entityData.set(DATA_PROMOTION_TICK, this.tickCount);
        applyGeneticAttributes();
        this.heal(this.getMaxHealth() * 0.25F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(), this.getX(), this.getY(0.6), this.getZ(),
                    22, 0.5, 0.5, 0.5, 0.05);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_PULSE.get(),
                    SoundSource.NEUTRAL, 1.0F, targetRank == EvolutionRank.ELITE ? 1.4F : 1.15F);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.CHIMERA_ROAR.get(),
                    SoundSource.NEUTRAL, 0.9F, targetRank == EvolutionRank.ELITE ? 1.2F : 1.0F);
        }
    }

    public float getPromotionFlashProgress(float partialTick) {
        int startTick = this.entityData.get(DATA_PROMOTION_TICK);
        if (startTick < 0) {
            return 0.0F;
        }
        float elapsed = (this.tickCount + partialTick) - startTick;
        if (elapsed < 0.0F || elapsed >= PROMOTION_FLASH_TICKS) {
            return 0.0F;
        }
        return 1.0F - (elapsed / PROMOTION_FLASH_TICKS);
    }

    /**
     * Randomizes both the Chassis gene and Autonomous Mutation gene (ensuring 2 distinct traits)
     * and recalculates attributes immediately.
     */
    public void randomizeGenes(RandomSource random) {
        AdaptationTrait[] traits = AdaptationTrait.values();
        AdaptationTrait prevChassis = getChassisGene();
        AdaptationTrait prevMutation = getMutationGene();

        AdaptationTrait nextChassis = traits[random.nextInt(traits.length)];
        AdaptationTrait nextMutation = traits[random.nextInt(traits.length)];
        int guard = 0;
        while ((nextMutation == nextChassis || (nextChassis == prevChassis && nextMutation == prevMutation)) && guard < 12) {
            nextChassis = traits[random.nextInt(traits.length)];
            nextMutation = traits[random.nextInt(traits.length)];
            guard++;
        }
        if (nextMutation == nextChassis) {
            nextMutation = traits[(nextChassis.ordinal() + 1) % traits.length];
        }
        setGenes(nextChassis, nextMutation);
    }

    public void applyGeneticAttributes() {
        AdaptationTrait chassis = getChassisGene();
        EvolutionRank rank = getEvolutionRank();
        double rankMult = switch (rank) {
            case BASE -> 1.0D;
            case MUTATED -> 1.15D;
            case ELITE -> 1.35D;
        };
        float scale = switch (rank) {
            case BASE -> 1.0F;
            case MUTATED -> 1.12F;
            case ELITE -> 1.25F;
        };

        double baseHp = (chassis == AdaptationTrait.BLAST_SHIELDED ? 68.0D : 52.0D) * rankMult;
        double baseDmg = (chassis == AdaptationTrait.MELEE_THORNS ? 8.5D : 5.5D) * rankMult;
        double baseArmor = (chassis == AdaptationTrait.BLAST_SHIELDED ? 8.0D : 4.0D);
        double baseSpeed = (chassis == AdaptationTrait.PROJECTILE_DEFLECT ? 0.35D : 0.30D);

        setAttr(Attributes.MAX_HEALTH, baseHp);
        setAttr(Attributes.ATTACK_DAMAGE, baseDmg);
        setAttr(Attributes.ARMOR, baseArmor);
        setAttr(Attributes.MOVEMENT_SPEED, baseSpeed);
        setAttr(Attributes.SCALE, scale);
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void setAttr(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double val) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst != null) {
            inst.setBaseValue(val);
        }
    }

    @Override
    public boolean fireImmune() {
        return getChassisGene() == AdaptationTrait.FIRE_RESISTANT || super.fireImmune();
    }

    @Override
    public float maxUpStep() {
        return 1.2F;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (this.getFirstPassenger() instanceof Player player && isOwner(player)) {
            return player;
        }
        return null;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isAlive() && this.isVehicle() && getControllingPassenger() instanceof Player rider) {
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;

            float strafe = rider.xxa * 0.5F;
            float forward = rider.zza;
            if (forward <= 0.0F) {
                forward *= 0.45F;
            }
            float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (this.level().getBlockState(this.blockPosition().below()).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                speed *= 1.20F;
            }
            this.setSpeed(speed);
            super.travel(new Vec3(strafe, travelVector.y, forward));
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.rerollCooldownTicks > 0) {
            this.rerollCooldownTicks--;
        }
        if (this.mutationCooldownTicks > 0) {
            this.mutationCooldownTicks--;
        }
        if (this.healCooldownTicks > 0) {
            this.healCooldownTicks--;
        }

        if (this.level().isClientSide()) {
            if (this.tickCount % 12 == 0) {
                this.level().addParticle(ModParticles.SPORE_MOTE.get(),
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                        this.getY() + 0.9,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                        0.0, 0.02, 0.0);
            }
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (getChassisGene() == AdaptationTrait.MAGIC_CARAPACE
                && this.tickCount % 100 == 0
                && this.hurtTime == 0
                && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }

        if (this.mutationCooldownTicks <= 0) {
            tickAutonomousMutationAbility(serverLevel);
        }
    }

    public boolean tickAutonomousMutationAbility(ServerLevel level) {
        AdaptationTrait mutation = getMutationGene();
        double range = mutation == AdaptationTrait.PROJECTILE_DEFLECT ? 16.0D : 10.0D;
        LivingEntity enemy = findNearestHostile(level, range);
        if (enemy == null) {
            return false;
        }

        switch (mutation) {
            case MELEE_THORNS -> {
                Vec3 dir = enemy.position().subtract(this.position()).normalize();
                this.push(dir.x * 0.45, 0.15, dir.z * 0.45);
                AABB hitBox = this.getBoundingBox().inflate(2.5D, 1.0D, 2.5D);
                for (Monster m : level.getEntitiesOfClass(Monster.class, hitBox)) {
                    if (!isValidChimeraHostile(m)) {
                        continue;
                    }
                    m.hurt(this.damageSources().mobAttack(this), 4.5F);
                    m.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                }
                level.sendParticles(ModParticles.PSYCHIC_WAVE.get(), this.getX(), this.getY(0.5), this.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
                level.playSound(null, this.blockPosition(), ModSounds.MYCELIAL_CHIMERA_STRIKE.get(), SoundSource.NEUTRAL, 0.9F, 1.0F);
            }
            case PROJECTILE_DEFLECT -> {
                double dx = enemy.getX() - this.getX();
                double dy = enemy.getY(0.5) - this.getEyeY();
                double dz = enemy.getZ() - this.getZ();
                for (float yawDeg : new float[]{-8.0F, 8.0F}) {
                    double rad = Math.toRadians(yawDeg);
                    double rx = dx * Math.cos(rad) - dz * Math.sin(rad);
                    double rz = dx * Math.sin(rad) + dz * Math.cos(rad);
                    NeutralizingSporeProjectile spore = new NeutralizingSporeProjectile(level, this);
                    spore.setPos(this.getX(), this.getEyeY() + 0.1, this.getZ());
                    spore.shoot(rx, dy, rz, 1.35F, 2.0F);
                    level.addFreshEntity(spore);
                }
                level.playSound(null, this.blockPosition(), ModSounds.MYCELIAL_CHIMERA_STRIKE.get(), SoundSource.NEUTRAL, 0.9F, 1.2F);
            }
            case FIRE_RESISTANT -> {
                AABB box = new AABB(this.blockPosition()).inflate(4.5D);
                for (Monster m : level.getEntitiesOfClass(Monster.class, box)) {
                    if (!isValidChimeraHostile(m)) {
                        continue;
                    }
                    m.hurt(this.damageSources().onFire(), 4.0F);
                    m.igniteForSeconds(3);
                }
                if (getControllingPassenger() instanceof Player rider) {
                    rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
                }
                level.sendParticles(ModParticles.HIVE_EMBER.get(), this.getX(), this.getY(0.4), this.getZ(), 24, 1.5, 0.3, 1.5, 0.05);
                level.playSound(null, this.blockPosition(), ModSounds.MYCELIAL_CHIMERA_STRIKE.get(), SoundSource.NEUTRAL, 0.85F, 0.95F);
            }
            case BLAST_SHIELDED -> {
                AABB box = new AABB(this.blockPosition()).inflate(5.0D);
                for (Monster m : level.getEntitiesOfClass(Monster.class, box)) {
                    if (!isValidChimeraHostile(m)) {
                        continue;
                    }
                    m.hurt(this.damageSources().mobAttack(this), 3.5F);
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                    Vec3 kb = m.position().subtract(this.position()).normalize().scale(0.65);
                    m.push(kb.x, 0.28, kb.z);
                }
                level.sendParticles(ModParticles.CHITIN_SHARD.get(), this.getX(), this.getY() + 0.3, this.getZ(), 4, 1.0, 0.2, 1.0, 0.02);
                level.playSound(null, this.blockPosition(), ModSounds.MYCELIAL_CHIMERA_STRIKE.get(), SoundSource.NEUTRAL, 0.85F, 0.8F);
            }
            case MAGIC_CARAPACE -> {
                this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, false, false));
                if (getControllingPassenger() instanceof Player rider) {
                    rider.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, false, false));
                }
                level.sendParticles(ModParticles.ENGRAM.get(), this.getX(), this.getY(0.6), this.getZ(), 8, 0.8, 0.6, 0.8, 0.04);
                level.playSound(null, this.blockPosition(), ModSounds.MYCELIAL_CHIMERA_STRIKE.get(), SoundSource.NEUTRAL, 0.9F, 1.35F);
            }
        }
        this.mutationCooldownTicks = this.isVehicle() ? 160 : 140;
        return true;
    }

    @Nullable
    private LivingEntity findNearestHostile(ServerLevel level, double range) {
        LivingEntity current = this.getTarget();
        if (isValidChimeraHostile(current) && this.distanceToSqr(current) <= range * range) {
            return current;
        }
        List<Monster> nearby = level.getEntitiesOfClass(Monster.class, new AABB(this.blockPosition()).inflate(range));
        return nearby.stream()
                .filter(this::isValidChimeraHostile)
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.BIO_SCANNER.get())) {
            return InteractionResult.PASS;
        }
        if (!isOwner(player)) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide()) {
            if (stack.is(ModItems.HIVE_HEART.get())) {
                this.setHealth(this.getMaxHealth());
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1, false, true));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0, false, true));
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                ((ServerLevel) this.level()).sendParticles(ModParticles.HIVE_PULSE.get(),
                        this.getX(), this.getY(0.7), this.getZ(), 2, 0.35, 0.35, 0.35, 0.02);
                this.level().playSound(null, this.blockPosition(), ModSounds.SOVEREIGN_NEXUS_PULSE.get(),
                        SoundSource.NEUTRAL, 0.9F, 1.2F);
                player.displayClientMessage(
                        Component.translatable("message.mycelialmind.chimera_heart_empowered").withStyle(ChatFormatting.LIGHT_PURPLE),
                        true);
                return InteractionResult.CONSUME;
            }

            // Heal with Mycelial Biomass (bounded +2s cooldown)
            if (stack.is(ModItems.MYCELIAL_BIOMASS.get()) && this.getHealth() < this.getMaxHealth()) {
                if (this.healCooldownTicks > 0) {
                    return InteractionResult.CONSUME;
                }
                this.heal(6.0F);
                this.healCooldownTicks = 40;
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                ((ServerLevel) this.level()).sendParticles(ModParticles.PURIFIER_CROSS.get(), this.getX(), this.getY(0.8), this.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
                return InteractionResult.CONSUME;
            }

            // Re-roll random genes with Genetic Sample (10s cooldown)
            if (stack.getItem() instanceof GeneticSampleItem) {
                if (this.rerollCooldownTicks > 0) {
                    player.displayClientMessage(
                            Component.translatable("message.mycelialmind.chimera_reroll_cooldown",
                                    (int) Math.ceil(this.rerollCooldownTicks / 20.0)).withStyle(ChatFormatting.YELLOW),
                            true
                    );
                    return InteractionResult.CONSUME;
                }
                randomizeGenes(this.random);
                this.rerollCooldownTicks = 200;
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                ((ServerLevel) this.level()).sendParticles(ModParticles.NECROTIC_MIST.get(), this.getX(), this.getY(0.7), this.getZ(), 18, 0.5, 0.5, 0.5, 0.04);
                this.level().playSound(null, this.blockPosition(), ModSounds.CHIMERA_ROAR.get(), SoundSource.NEUTRAL, 0.85F, 1.2F);
                player.displayClientMessage(
                        Component.translatable("message.mycelialmind.chimera_genes_summary",
                                MycelialText.trait(getChassisGene()),
                                MycelialText.trait(getMutationGene()),
                                MycelialText.rank(getEvolutionRank()))
                                .withStyle(ChatFormatting.LIGHT_PURPLE),
                        true
                );
                return InteractionResult.CONSUME;
            }

            // Evolve rank: SynapticCore -> MUTATED; OvermindEssence/TideBeacon -> ELITE
            if (stack.is(ModItems.SYNAPTIC_CORE.get()) && getEvolutionRank() == EvolutionRank.BASE) {
                promoteRank(EvolutionRank.MUTATED);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                player.displayClientMessage(Component.translatable("message.mycelialmind.chimera_evolved",
                        MycelialText.rank(EvolutionRank.MUTATED)).withStyle(ChatFormatting.GOLD), true);
                return InteractionResult.CONSUME;
            }
            if ((stack.is(ModItems.OVERMIND_ESSENCE.get()) || stack.is(ModItems.TIDE_BEACON.get()))
                    && getEvolutionRank() == EvolutionRank.MUTATED) {
                promoteRank(EvolutionRank.ELITE);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                player.displayClientMessage(Component.translatable("message.mycelialmind.chimera_evolved",
                        MycelialText.rank(EvolutionRank.ELITE)).withStyle(ChatFormatting.DARK_PURPLE), true);
                return InteractionResult.CONSUME;
            }

            // Shift + Right-click: cycle command mode (FOLLOW -> PATROL -> WAIT)
            if (player.isSecondaryUseActive()) {
                CommandMode next = getCommandMode().next();
                setCommandMode(next);
                player.displayClientMessage(
                        Component.translatable("message.mycelialmind.chimera_mode",
                                MycelialText.chimeraMode(next),
                                MycelialText.trait(getChassisGene()),
                                MycelialText.trait(getMutationGene()))
                                .withStyle(ChatFormatting.AQUA),
                        true
                );
                return InteractionResult.CONSUME;
            }

            // Normal Right-click: mount Chimera
            if (!this.isVehicle()) {
                player.startRiding(this);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && isOwner(p) && !p.isCreative()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().equals(ModEffects.MYCELIAL_INFECTION)) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public static int countActiveChimerasForOwner(ServerLevel level, UUID ownerUuid) {
        ChimeraOwnershipSavedData ownership = ChimeraOwnershipSavedData.get(level.getServer());
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (MycelialChimeraEntity chimera : serverLevel.getEntities(ModEntities.MYCELIAL_CHIMERA.get(),
                    entity -> entity.isAlive() && ownerUuid.equals(entity.getOwnerUUID()))) {
                ownership.register(chimera.getUUID(), ownerUuid);
            }
        }
        return ownership.countForOwner(ownerUuid);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
            ChimeraOwnershipSavedData.get(serverLevel.getServer()).unregister(this.getUUID());
        }
        super.remove(reason);
    }

    @Nullable
    public BlockPos getHomePos() {
        return homePos;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) {
            tag.putUUID(TAG_OWNER, ownerUuid);
        }
        if (homePos != null) {
            tag.putInt(TAG_HOME_X, homePos.getX());
            tag.putInt(TAG_HOME_Y, homePos.getY());
            tag.putInt(TAG_HOME_Z, homePos.getZ());
        }
        tag.putInt(TAG_CHASSIS, getChassisGene().ordinal());
        tag.putInt(TAG_MUTATION, getMutationGene().ordinal());
        tag.putInt(TAG_MODE, getCommandMode().id());
        tag.putInt(TAG_RANK, getEvolutionRank().ordinal());
        saveBrainState(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) {
            this.ownerUuid = tag.getUUID(TAG_OWNER);
            if (this.level() instanceof ServerLevel serverLevel) {
                ChimeraOwnershipSavedData.get(serverLevel.getServer()).register(this.getUUID(), this.ownerUuid);
            }
        }
        if (tag.contains(TAG_HOME_X) && tag.contains(TAG_HOME_Y) && tag.contains(TAG_HOME_Z)) {
            this.homePos = new BlockPos(tag.getInt(TAG_HOME_X), tag.getInt(TAG_HOME_Y), tag.getInt(TAG_HOME_Z));
        }
        if (tag.contains(TAG_CHASSIS)) {
            this.entityData.set(DATA_CHASSIS_GENE, tag.getInt(TAG_CHASSIS));
        }
        if (tag.contains(TAG_MUTATION)) {
            this.entityData.set(DATA_MUTATION_GENE, tag.getInt(TAG_MUTATION));
        }
        if (tag.contains(TAG_MODE)) {
            this.entityData.set(DATA_COMMAND_MODE, tag.getInt(TAG_MODE));
        }
        if (tag.contains(TAG_RANK)) {
            this.entityData.set(DATA_EVOLUTION_RANK, tag.getInt(TAG_RANK));
        }
        loadBrainState(tag);
        applyGeneticAttributes();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * MycelialMob.computeRankVoicePitchMultiplier(getEvolutionRank());
    }

    @Override
    protected float getSoundVolume() {
        return 1.05F * MycelialMob.computeRankSoundVolumeMultiplier(getEvolutionRank());
    }

    @Override
    public int getAmbientSoundInterval() {
        return 140;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        EvolutionRank rank = getEvolutionRank();
        float stepVol = 0.40F * MycelialMob.computeRankSoundVolumeMultiplier(rank);
        float stepPitch = (0.92F + this.random.nextFloat() * 0.14F) * MycelialMob.computeRankVoicePitchMultiplier(rank);
        this.playSound(ModSounds.MOB_STEP_HEAVY.get(), stepVol, stepPitch);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MYCELIAL_CHIMERA_GROWL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.MYCELIAL_CHIMERA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MYCELIAL_CHIMERA_DEATH.get();
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
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    private static final class ChimeraFollowOrPatrolGoal extends Goal {
        private final MycelialChimeraEntity chimera;

        private ChimeraFollowOrPatrolGoal(MycelialChimeraEntity chimera) {
            this.chimera = chimera;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !chimera.isVehicle()
                    && chimera.getTarget() == null
                    && chimera.getCommandMode() != CommandMode.WAIT;
        }

        @Override
        public void tick() {
            if (!(chimera.level() instanceof ServerLevel level)) {
                return;
            }
            if (chimera.getCommandMode() == CommandMode.FOLLOW && chimera.ownerUuid != null) {
                Player owner = level.getPlayerByUUID(chimera.ownerUuid);
                if (owner != null && chimera.distanceToSqr(owner) > 36.0D) {
                    chimera.getNavigation().moveTo(owner, 1.15D);
                }
            } else if (chimera.getCommandMode() == CommandMode.PATROL && chimera.homePos != null) {
                if (chimera.blockPosition().distSqr(chimera.homePos) > 196.0D) {
                    chimera.getNavigation().moveTo(
                            chimera.homePos.getX() + 0.5,
                            chimera.homePos.getY() + 1.0,
                            chimera.homePos.getZ() + 0.5,
                            1.05D
                    );
                }
            }
        }
    }
}
