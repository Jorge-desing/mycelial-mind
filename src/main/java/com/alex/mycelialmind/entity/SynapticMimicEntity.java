package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.entity.ai.HiveRegroupGoal;
import com.alex.mycelialmind.entity.ai.KitingRetreatGoal;
import com.alex.mycelialmind.entity.ai.SwarmFlankGoal;
import com.alex.mycelialmind.entity.ai.brain.behaviour.FleeTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.MoveToWalkTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.StrafeTarget;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.DelayedBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.core.UtilitySelectorBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveSquadComposer;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.hive.MycorrhizalAcousticTracker;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine;
import com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.MimicPersona;
import com.alex.mycelialmind.item.NeutralizingSporeLauncherItem;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.datafixers.util.Pair;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Adaptive fungal infiltrator and doppelgänger:
 * combines dual covert spoofing (diplomatic escort / acoustic echo lure),
 * tripartite network sabotage (turrets, drones, symbiotic nodes, decoys/volatile memory),
 * information warfare (Pavlovian deception + ghost radar echoes),
 * habit-aware combat stances with Symbiotic Mutagen parasitism,
 * and Bio-Scanner Synaptic Shock counterplay.
 */
public class SynapticMimicEntity extends MycelialMob {
    public enum MimicStance {
        BLADE_DANCER(0),
        SPORE_MARKSMAN(1),
        AEGIS_MIRROR(2);

        private final int id;

        MimicStance(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public String translationKey() {
            return switch (this) {
                case BLADE_DANCER -> "stance.mycelialmind.blade_dancer";
                case SPORE_MARKSMAN -> "stance.mycelialmind.spore_marksman";
                case AEGIS_MIRROR -> "stance.mycelialmind.aegis_mirror";
            };
        }

        public static MimicStance fromId(int id) {
            for (MimicStance stance : values()) {
                if (stance.id == id) {
                    return stance;
                }
            }
            return BLADE_DANCER;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_MIMIC_STANCE =
            SynchedEntityData.defineId(SynapticMimicEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MIMIC_PERSONA =
            SynchedEntityData.defineId(SynapticMimicEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPOOFED_STRAIN =
            SynchedEntityData.defineId(SynapticMimicEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PARASITIZED_MUTAGEN =
            SynchedEntityData.defineId(SynapticMimicEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_UNMASKED =
            SynchedEntityData.defineId(SynapticMimicEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SHOCK_TICKS =
            SynchedEntityData.defineId(SynapticMimicEntity.class, EntityDataSerializers.INT);

    private static final String TAG_STANCE = "MimicStance";
    private static final String TAG_PERSONA = "MimicPersona";
    private static final String TAG_SPOOFED_STRAIN = "MimicSpoofedStrain";
    private static final String TAG_PARASITIZED_MUTAGEN = "MimicParasitizedMutagen";
    private static final String TAG_UNMASKED = "MimicUnmasked";
    private static final String TAG_UNMASKED_COUNTERPLAY = "MimicUnmaskedCounterplay";
    private static final String TAG_SHOCK_TICKS = "MimicShockTicks";

    private int stanceActionCooldown = 30;
    private int sabotageCooldown = 60;
    private int acousticPulseCooldown = 80;
    private boolean unmaskedByCounterplay = false;

    public SynapticMimicEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MIMIC_STANCE, MimicStance.BLADE_DANCER.id());
        builder.define(DATA_MIMIC_PERSONA, MimicPersona.INFILTRATION_SPOOF.id());
        builder.define(DATA_SPOOFED_STRAIN, HiveStrain.AMBER.id());
        builder.define(DATA_PARASITIZED_MUTAGEN, -1);
        builder.define(DATA_UNMASKED, false);
        builder.define(DATA_SHOCK_TICKS, 0);
    }

    public MimicStance getMimicStance() {
        return MimicStance.fromId(this.entityData.get(DATA_MIMIC_STANCE));
    }

    public void setMimicStance(MimicStance stance) {
        this.entityData.set(DATA_MIMIC_STANCE, (stance != null ? stance : MimicStance.BLADE_DANCER).id());
    }

    public MimicPersona getMimicPersona() {
        return MimicPersona.fromId(this.entityData.get(DATA_MIMIC_PERSONA));
    }

    public void setMimicPersona(MimicPersona persona) {
        this.entityData.set(DATA_MIMIC_PERSONA, (persona != null ? persona : MimicPersona.INFILTRATION_SPOOF).id());
    }

    public HiveStrain getSpoofedStrain() {
        return HiveStrain.byId(this.entityData.get(DATA_SPOOFED_STRAIN));
    }

    public void setSpoofedStrain(HiveStrain strain) {
        this.entityData.set(DATA_SPOOFED_STRAIN, (strain != null ? strain : HiveStrain.AMBER).id());
    }

    @Nullable
    public SymbioticMutagen getParasitizedMutagen() {
        int ord = this.entityData.get(DATA_PARASITIZED_MUTAGEN);
        SymbioticMutagen[] all = SymbioticMutagen.values();
        return (ord >= 0 && ord < all.length) ? all[ord] : null;
    }

    public void setParasitizedMutagen(@Nullable SymbioticMutagen mutagen) {
        this.entityData.set(DATA_PARASITIZED_MUTAGEN, mutagen != null ? mutagen.ordinal() : -1);
    }

    public boolean isUnmasked() {
        return this.entityData.get(DATA_UNMASKED);
    }

    public void setUnmasked(boolean unmasked) {
        this.entityData.set(DATA_UNMASKED, unmasked);
        if (unmasked && getMimicPersona() != MimicPersona.UNMASKED_DUELIST) {
            setMimicPersona(MimicPersona.UNMASKED_DUELIST);
        }
    }

    public boolean wasUnmaskedByCounterplay() {
        return unmaskedByCounterplay;
    }

    public int getShockTicks() {
        return this.entityData.get(DATA_SHOCK_TICKS);
    }

    public void setShockTicks(int ticks) {
        this.entityData.set(DATA_SHOCK_TICKS, Math.max(0, ticks));
    }

    public boolean isInformationWarfareActive() {
        return !isDesynced() && getShockTicks() <= 0;
    }

    public boolean isSpoofingAlliance() {
        return !isUnmasked()
                && !isDesynced()
                && getMimicPersona() == MimicPersona.INFILTRATION_SPOOF;
    }

    public boolean isSpoofingAllianceFor(@Nullable Player player) {
        if (!isSpoofingAlliance()) {
            return false;
        }
        return player == null || !player.hasEffect(ModEffects.SYNAPTIC_VISION);
    }

    @Override
    public void triggerDesync(int ticks) {
        boolean wasCovert = !isUnmasked();
        super.triggerDesync(ticks);
        if (ticks > 0) {
            setParasitizedMutagen(null);
            if (wasCovert && this.level() instanceof ServerLevel serverLevel) {
                unmaskWithSynapticShock(serverLevel, null);
            } else {
                setUnmasked(true);
                setMimicPersona(MimicPersona.UNMASKED_DUELIST);
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new KitingRetreatGoal(this, 1.15D) {
            @Override
            public boolean canUse() {
                return getMimicStance() == MimicStance.SPORE_MARKSMAN && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new SwarmFlankGoal(this, 1.20D));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(4, new HiveRegroupGoal(this, 1.15D));
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
        var shockedFlee = new FleeTarget<MycelialMob>()
                .fleeDistance(12.0F)
                .speedModifier(1.25F);
        shockedFlee.startCondition(mob -> isUnmasked() && getShockTicks() > 0);

        var marksmanStrafe = new StrafeTarget<MycelialMob>()
                .strafeDistance(9.0F)
                .speedMod(0.9F)
                .stopStrafingWhen(mob -> isDesynced() || getMimicStance() != MimicStance.SPORE_MARKSMAN);

        var covertObservation = new DelayedBehaviour<MycelialMob>(25) {
            @Override
            protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
                return List.of(Pair.of(MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryStatus.VALUE_PRESENT));
            }

            @Override
            protected boolean checkExtraStartConditions(ServerLevel level, MycelialMob entity) {
                return !isUnmasked() && !isDesynced();
            }

            @Override
            protected void doDelayedAction(MycelialMob entity) {
                LivingEntity seen = HiveBrainUtils.getMemory(entity, MemoryModuleType.NEAREST_VISIBLE_PLAYER);
                if (seen != null && seen.isAlive()) {
                    entity.getLookControl().setLookAt(seen, 25.0F, 25.0F);
                }
            }
        };
        covertObservation.cooldownFor(mob -> 60);

        return BrainActivityGroup.coreTasks(
                new LookAtTarget<>(),
                new MoveToWalkTarget<>(),
                new UtilitySelectorBehaviour<MycelialMob>()
                        .hysteresisBonus(0.10F)
                        .add(shockedFlee, mob -> (isUnmasked() && getShockTicks() > 0) ? 1.0F : 0.0F)
                        .add(marksmanStrafe, mob -> (!isDesynced() && getMimicStance() == MimicStance.SPORE_MARKSMAN) ? 0.75F : 0.0F)
                        .add(covertObservation, mob -> (!isUnmasked() && !isDesynced()) ? 0.45F : 0.0F)
        );
    }

    public MimicStance evaluateTargetStance(LivingEntity target) {
        ItemStack main = target.getMainHandItem();
        ItemStack off = target.getOffhandItem();

        boolean hasProjectileWeapon = main.getItem() instanceof ProjectileWeaponItem
                || main.getItem() instanceof NeutralizingSporeLauncherItem
                || off.getItem() instanceof ProjectileWeaponItem
                || off.getItem() instanceof NeutralizingSporeLauncherItem;
        boolean hasShield = main.getItem() instanceof ShieldItem || off.getItem() instanceof ShieldItem;
        int armorValue = target.getArmorValue();

        StimulusType dominant = null;
        Set<PlayerHabit> activeHabits = EnumSet.noneOf(PlayerHabit.class);
        if (this.level() instanceof ServerLevel serverLevel) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            dominant = HiveSquadComposer.findDominantStimulus(hive);
            for (PlayerHabit habit : PlayerHabit.values()) {
                if (hive.hasHabitAdaptation(habit)) {
                    activeHabits.add(habit);
                }
            }
        }
        return SynapticMimicArchitectureEngine.evaluateCombatStance(
                hasProjectileWeapon, hasShield, armorValue, dominant, activeHabits);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.tickCount % 10 == 0) {
                var particle = switch (getMimicStance()) {
                    case BLADE_DANCER -> ModParticles.NECROTIC_MIST.get();
                    case SPORE_MARKSMAN -> ModParticles.SPORE_FALL.get();
                    case AEGIS_MIRROR -> ModParticles.HIVE_PULSE.get();
                };
                this.level().addParticle(particle,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.6,
                        this.getY() + 1.1,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.6,
                        0.0, 0.03, 0.0);
            }
            return;
        }

        if (stanceActionCooldown > 0) {
            stanceActionCooldown--;
        }
        if (sabotageCooldown > 0) {
            sabotageCooldown--;
        }
        if (acousticPulseCooldown > 0) {
            acousticPulseCooldown--;
        }
        if (getShockTicks() > 0) {
            setShockTicks(getShockTicks() - 1);
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (this.tickCount % 20 == 0) {
            updateMimicArchitecture(serverLevel, target);
        }

        if (this.tickCount % 40 == 0 && isInformationWarfareActive()) {
            tickInformationWarfare(serverLevel);
        }

        if (sabotageCooldown <= 0 && !isDesynced()) {
            if (tryExecuteNetworkSabotage(serverLevel, target instanceof Player p ? p : null)) {
                sabotageCooldown = 100;
            } else {
                sabotageCooldown = 20;
            }
        }

        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.tickCount % 40 == 0) {
            MimicStance next = evaluateTargetStance(target);
            if (next != getMimicStance()) {
                setMimicStance(next);
                serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(),
                        this.getX(), this.getY(0.5), this.getZ(),
                        10, 0.3, 0.4, 0.3, 0.04);
            }
        }

        MimicStance stance = getMimicStance();
        if (stance == MimicStance.AEGIS_MIRROR && this.tickCount % 40 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, false, false));
        }

        if (!isUnmasked() && target instanceof Player && this.distanceToSqr(target) <= 42.25D && isInformationWarfareActive()) {
            setUnmasked(true);
            setMimicPersona(MimicPersona.UNMASKED_DUELIST);
            serverLevel.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                    this.getX(), this.getY(0.5), this.getZ(), 10, 0.35, 0.4, 0.35, 0.04);
        }

        if (stanceActionCooldown > 0) {
            return;
        }

        double distSq = this.distanceToSqr(target);
        if (stance == MimicStance.BLADE_DANCER && distSq <= 20.25D) {
            performBladeArcSlash(serverLevel, target);
            stanceActionCooldown = 60;
        } else if (stance == MimicStance.SPORE_MARKSMAN && distSq >= 12.25D && distSq <= 324.0D && this.hasLineOfSight(target)) {
            performSporeVolley(serverLevel, target);
            stanceActionCooldown = 50;
        }
    }

    private void updateMimicArchitecture(ServerLevel level, @Nullable LivingEntity target) {
        BlockPos origin = this.blockPosition();
        boolean nearSabotageTarget = false;
        boolean hasNearbyDefenses = !level.getEntitiesOfClass(
                PurifierDroneEntity.class, this.getBoundingBox().inflate(16.0D), PurifierDroneEntity::isAlive).isEmpty()
                || !level.getEntitiesOfClass(
                MycelialChimeraEntity.class, this.getBoundingBox().inflate(16.0D), MycelialChimeraEntity::isAlive).isEmpty();

        for (PurificationTurretBlockEntity ignored : findNearbyTurrets(level, 16)) {
            hasNearbyDefenses = true;
            if (ignored.getBlockPos().distSqr(origin) <= 64.0D) {
                nearSabotageTarget = true;
            }
            break;
        }
        for (PurifierDroneEntity drone : level.getEntitiesOfClass(
                PurifierDroneEntity.class, this.getBoundingBox().inflate(8.0D), PurifierDroneEntity::isAlive)) {
            if (!drone.isJammed()) {
                nearSabotageTarget = true;
                break;
            }
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        if (tracker.hasNearbySymbioticNode(origin, 16)) {
            hasNearbyDefenses = true;
            if (tracker.hasNearbySymbioticNode(origin, 8)) {
                nearSabotageTarget = true;
            }
        }
        if (MycorrhizalAcousticTracker.hasActiveDecoyNear(level, origin, 12)) {
            nearSabotageTarget = true;
        }

        BlockState below = level.getBlockState(origin.below());
        BlockState here = level.getBlockState(origin);
        boolean onMyceliumOrDecoy = below.is(ModBlocks.CREEPING_MYCELIUM.get())
                || here.is(ModBlocks.HYPHAL_VEIN.get())
                || MycorrhizalAcousticTracker.hasActiveDecoyNear(level, origin, 16);

        Player inspectPlayer = target instanceof Player p ? p : level.getNearestPlayer(this, 24.0D);
        if (inspectPlayer != null) {
            var dip = inspectPlayer.getData(ModAttachments.DIPLOMACY);
            HiveStrain bestStrain = HiveStrain.AMBER;
            int bestRank = -1;
            for (HiveStrain candidate : new HiveStrain[]{HiveStrain.AMBER, HiveStrain.CRIMSON, HiveStrain.ORTHODOX}) {
                int sOrd = dip.getStanding(candidate).ordinal();
                if (sOrd > bestRank) {
                    bestRank = sOrd;
                    bestStrain = candidate;
                }
                if (bestRank >= 1) {
                    hasNearbyDefenses = true;
                }
            }
            setSpoofedStrain(bestStrain);
            Set<SymbioticMutagen> equipped = inspectPlayer.getData(ModAttachments.SYMBIOSIS).getEquippedMutagens();
            setParasitizedMutagen(SynapticMimicArchitectureEngine.selectParasitizedMutagen(
                    equipped, getMimicStance(), isDesynced()));
        } else {
            setParasitizedMutagen(null);
        }

        MimicPersona nextPersona = SynapticMimicArchitectureEngine.evaluatePersona(
                isUnmasked(), isDesynced(), nearSabotageTarget, hasNearbyDefenses, onMyceliumOrDecoy);
        setMimicPersona(nextPersona);
    }

    private void tickInformationWarfare(ServerLevel level) {
        Player nearest = level.getNearestPlayer(this, 24.0D);
        if (nearest == null || nearest.isSpectator()) {
            return;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        long now = level.getGameTime();
        if (hive.canStartDeception(now)) {
            StimulusType dominant = HiveSquadComposer.findDominantStimulus(hive);
            StimulusType falseType = SynapticMimicArchitectureEngine.selectDeceptiveFalseStimulus(
                    getMimicStance(), dominant);
            hive.startDeception(falseType, now);
        }
        if (getMimicPersona() == MimicPersona.ECHO_LURE && !isUnmasked() && acousticPulseCooldown <= 0) {
            acousticPulseCooldown = 100;
            level.sendParticles(ModParticles.HIVE_PULSE.get(),
                    this.getX(), this.getY(0.5), this.getZ(),
                    8, 0.45, 0.35, 0.45, 0.02);
            level.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_MIMIC_ECHO.get(),
                    SoundSource.HOSTILE, 0.9F, 0.95F);
        }
    }

    public boolean tryExecuteNetworkSabotage(ServerLevel level, @Nullable Player fallbackPlayer) {
        if (isDesynced()) {
            return false;
        }
        BlockPos origin = this.blockPosition();
        boolean sabotaged = false;
        Player notifyPlayer = fallbackPlayer != null ? fallbackPlayer : level.getNearestPlayer(this, 24.0D);

        for (PurificationTurretBlockEntity turret : findNearbyTurrets(level, 7)) {
            if (!turret.isJammed()) {
                turret.applyCorrosiveJam(120);
                sabotaged = true;
                level.sendParticles(ModParticles.HIVE_EMBER.get(),
                        turret.getBlockPos().getX() + 0.5, turret.getBlockPos().getY() + 1.0, turret.getBlockPos().getZ() + 0.5,
                        8, 0.25, 0.25, 0.25, 0.02);
                break;
            }
        }

        if (!sabotaged) {
            for (PurifierDroneEntity drone : level.getEntitiesOfClass(
                    PurifierDroneEntity.class, this.getBoundingBox().inflate(7.0D), PurifierDroneEntity::isAlive)) {
                if (!drone.isJammed()) {
                    drone.setJammedTicks(120);
                    sabotaged = true;
                    level.sendParticles(ModParticles.HIVE_EMBER.get(),
                            drone.getX(), drone.getY(0.5), drone.getZ(),
                            8, 0.25, 0.25, 0.25, 0.02);
                    break;
                }
            }
        }

        for (BlockPos nodePos : BlockPos.betweenClosed(origin.offset(-7, -4, -7), origin.offset(7, 4, 7))) {
            if (nodePos.distSqr(origin) > 49.0D || !level.isLoaded(nodePos)) {
                continue;
            }
            if (level.getBlockEntity(nodePos) instanceof SynapticNodeBlockEntity nodeBe
                    && nodeBe.getNodeType() == SynapticNodeType.SYMBIOTIC
                    && nodeBe.getSymbioticStability() > 0) {
                int next = SynapticMimicArchitectureEngine.computeSymbioticStabilityDrain(
                        nodeBe.getSymbioticStability(), getEvolutionRank());
                nodeBe.setSymbioticStability(next);
                if (notifyPlayer != null) {
                    long overworldNow = level.getServer().overworld().getGameTime();
                    MycorrhizalAcousticTracker.setTravelCooldown(notifyPlayer.getUUID(), overworldNow + 200L);
                }
                level.sendParticles(ModParticles.HIVE_PULSE.get(),
                        nodePos.getX() + 0.5, nodePos.getY() + 0.8, nodePos.getZ() + 0.5,
                        10, 0.35, 0.35, 0.35, 0.03);
                sabotaged = true;
                break;
            }
        }

        if (MycorrhizalAcousticTracker.consumeNearestDecoy(level, origin, 12)) {
            sabotaged = true;
            consolidateNearbyHostileBuffer(level, origin, 24);
        } else if (consolidateNearbyHostileBuffer(level, origin, 16)) {
            sabotaged = true;
        }

        if (sabotaged) {
            level.playSound(null, origin, ModSounds.SYNAPTIC_MIMIC_STRIKE.get(), SoundSource.HOSTILE, 0.85F, 0.75F);
            if (notifyPlayer != null) {
                notifyPlayer.displayClientMessage(
                        Component.translatable("message.mycelialmind.mimic_sabotage_alert").withStyle(ChatFormatting.YELLOW),
                        true);
            }
        }
        return sabotaged;
    }

    private boolean consolidateNearbyHostileBuffer(ServerLevel level, BlockPos origin, int radius) {
        int rSq = radius * radius;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-radius, -6, -radius), origin.offset(radius, 6, radius))) {
            if (p.distSqr(origin) > rSq || !level.isLoaded(p)) {
                continue;
            }
            if (level.getBlockEntity(p) instanceof SynapticNodeBlockEntity nodeBe
                    && nodeBe.getNodeType() != SynapticNodeType.SYMBIOTIC) {
                nodeBe.triggerAlert(200);
                if (nodeBe.getVolatileTotal() > 0.0D) {
                    nodeBe.consolidateVolatileBuffer(level);
                    return true;
                }
            }
        }
        return false;
    }

    private java.util.List<PurificationTurretBlockEntity> findNearbyTurrets(ServerLevel level, int radius) {
        java.util.List<PurificationTurretBlockEntity> out = new java.util.ArrayList<>();
        BlockPos origin = this.blockPosition();
        int rSq = radius * radius;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-radius, -4, -radius), origin.offset(radius, 4, radius))) {
            if (p.distSqr(origin) <= rSq && level.isLoaded(p)
                    && level.getBlockEntity(p) instanceof PurificationTurretBlockEntity turret) {
                out.add(turret);
            }
        }
        return out;
    }

    public boolean unmaskWithSynapticShock(ServerLevel level, @Nullable Player sourcePlayer) {
        boolean firstUnmask = !isUnmasked();
        if (firstUnmask) {
            this.unmaskedByCounterplay = true;
        }
        setUnmasked(true);
        setMimicPersona(MimicPersona.UNMASKED_DUELIST);
        setParasitizedMutagen(null);
        setShockTicks(120);
        if (!isDesynced()) {
            super.triggerDesync(SynapticCommandSystem.DESYNC_DURATION_TICKS);
        }

        HiveMindSavedData.get(level).revealDeception(level.getGameTime());

        for (PurificationTurretBlockEntity turret : findNearbyTurrets(level, 12)) {
            turret.clearCorrosiveJam();
        }
        for (PurifierDroneEntity drone : level.getEntitiesOfClass(
                PurifierDroneEntity.class, this.getBoundingBox().inflate(12.0D), PurifierDroneEntity::isAlive)) {
            drone.clearJam();
        }

        level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                this.getX(), this.getY(0.6), this.getZ(), 16, 0.45, 0.5, 0.45, 0.05);
        level.playSound(null, this.blockPosition(), ModSounds.SUPPRESSOR_DISCHARGE.get(),
                SoundSource.PLAYERS, 0.9F, 1.35F);

        if (sourcePlayer != null) {
            sourcePlayer.sendSystemMessage(Component.translatable("message.mycelialmind.mimic_unmasked_shock")
                    .withStyle(ChatFormatting.AQUA));
        }
        return firstUnmask;
    }

    private void performBladeArcSlash(ServerLevel level, LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.push(dir.x * 0.45, 0.12, dir.z * 0.45);
        SymbioticMutagen mirrored = isDesynced() ? null : getParasitizedMutagen();
        float slashDamage = 5.5F + (mirrored == SymbioticMutagen.CHITIN_THORNS ? 2.0F : 0.0F);
        AABB slashBox = this.getBoundingBox().inflate(2.4D, 0.8D, 2.4D);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, slashBox, e -> e != this && !(e instanceof MycelialMob))) {
            if (hit.hurt(this.damageSources().mobAttack(this), slashDamage) && mirrored == SymbioticMutagen.PYRO_GLAND) {
                hit.igniteForSeconds(3);
            }
        }
        level.sendParticles(ModParticles.PSYCHIC_WAVE.get(), this.getX() + dir.x * 1.1, this.getY(0.5), this.getZ() + dir.z * 1.1,
                2, 0.2, 0.1, 0.2, 0.0);
        level.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_MIMIC_STRIKE.get(), SoundSource.HOSTILE, 0.9F, 1.1F);
    }

    private void performSporeVolley(ServerLevel level, LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5) - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        SymbioticMutagen mirrored = isDesynced() ? null : getParasitizedMutagen();
        float velocity = mirrored == SymbioticMutagen.KINETIC_STRAFE ? 1.55F : 1.25F;
        if (mirrored == SymbioticMutagen.KINETIC_STRAFE) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        }
        float[] yawOffsets = {-10.0F, 0.0F, 10.0F};
        for (float yawDeg : yawOffsets) {
            double rad = Math.toRadians(yawDeg);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double rx = dx * cos - dz * sin;
            double rz = dx * sin + dz * cos;
            AcidSpitProjectile spit = new AcidSpitProjectile(level, this);
            spit.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
            spit.shoot(rx, dy, rz, velocity, 2.5F);
            level.addFreshEntity(spit);
        }
        level.playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_MIMIC_STRIKE.get(), SoundSource.HOSTILE, 0.85F, 0.85F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float adjusted = amount * SynapticMimicArchitectureEngine.computeUnmaskShockDamageMultiplier(
                getShockTicks() > 0, isDesynced());
        SymbioticMutagen mirrored = isDesynced() ? null : getParasitizedMutagen();
        if (mirrored == SymbioticMutagen.BLAST_CARAPACE && source.is(DamageTypeTags.IS_EXPLOSION)) {
            adjusted *= 0.65F;
        } else if (mirrored == SymbioticMutagen.ARCANE_SYNAPSE && source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            adjusted *= 0.70F;
        }

        boolean result = super.hurt(source, adjusted);
        if (result && !this.level().isClientSide()) {
            if (!isUnmasked() && source.getEntity() instanceof Player) {
                setUnmasked(true);
                setMimicPersona(MimicPersona.UNMASKED_DUELIST);
            }
            if ((getMimicStance() == MimicStance.AEGIS_MIRROR || mirrored == SymbioticMutagen.CHITIN_THORNS)
                    && source.getEntity() instanceof LivingEntity attacker
                    && attacker != this
                    && !(attacker instanceof MycelialMob)) {
                float ratio = (mirrored == SymbioticMutagen.BLAST_CARAPACE || mirrored == SymbioticMutagen.CHITIN_THORNS)
                        ? 0.45F : 0.35F;
                float reflected = Math.max(1.0F, adjusted * ratio);
                attacker.hurt(this.damageSources().thorns(this), reflected);
                if (mirrored == SymbioticMutagen.ARCANE_SYNAPSE) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                }
                this.level().playSound(null, this.blockPosition(), ModSounds.SYNAPTIC_MIMIC_STRIKE.get(), SoundSource.HOSTILE, 0.75F, 1.25F);
            }
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_STANCE, getMimicStance().id());
        tag.putInt(TAG_PERSONA, getMimicPersona().id());
        tag.putInt(TAG_SPOOFED_STRAIN, getSpoofedStrain().id());
        tag.putInt(TAG_PARASITIZED_MUTAGEN, this.entityData.get(DATA_PARASITIZED_MUTAGEN));
        tag.putBoolean(TAG_UNMASKED, isUnmasked());
        tag.putBoolean(TAG_UNMASKED_COUNTERPLAY, this.unmaskedByCounterplay);
        tag.putInt(TAG_SHOCK_TICKS, getShockTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_STANCE)) {
            setMimicStance(MimicStance.fromId(tag.getInt(TAG_STANCE)));
        }
        if (tag.contains(TAG_PERSONA)) {
            setMimicPersona(MimicPersona.fromId(tag.getInt(TAG_PERSONA)));
        }
        if (tag.contains(TAG_SPOOFED_STRAIN)) {
            setSpoofedStrain(HiveStrain.byId(tag.getInt(TAG_SPOOFED_STRAIN)));
        }
        if (tag.contains(TAG_PARASITIZED_MUTAGEN)) {
            int ord = tag.getInt(TAG_PARASITIZED_MUTAGEN);
            SymbioticMutagen[] all = SymbioticMutagen.values();
            setParasitizedMutagen(ord >= 0 && ord < all.length ? all[ord] : null);
        }
        if (tag.contains(TAG_UNMASKED)) {
            this.entityData.set(DATA_UNMASKED, tag.getBoolean(TAG_UNMASKED));
        }
        this.unmaskedByCounterplay = tag.getBoolean(TAG_UNMASKED_COUNTERPLAY);
        if (tag.contains(TAG_SHOCK_TICKS)) {
            setShockTicks(tag.getInt(TAG_SHOCK_TICKS));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SYNAPTIC_MIMIC_ECHO.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SYNAPTIC_MIMIC_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SYNAPTIC_MIMIC_DEATH.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 65.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }
}
