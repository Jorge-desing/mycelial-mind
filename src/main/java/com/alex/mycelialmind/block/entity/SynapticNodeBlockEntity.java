package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.IncubatorNodeBlock;
import com.alex.mycelialmind.block.SentinelNodeBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.MycelialSpawnUtil;
import com.alex.mycelialmind.entity.SporeArchitectEntity;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveSquadComposer;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared BlockEntity for Relay, Incubator, and Sentinel Synaptic Nodes.
 * Renders network link particles, dispatches tactical squads, and projects sentinel auras.
 */
public class SynapticNodeBlockEntity extends BlockEntity {
    private static final String TAG_ALERT_TICKS = "AlertTicks";
    private static final String TAG_BIOMASS = "BiomassCharge";
    private static final String TAG_STABILITY = "SymbioticStability";
    private static final String TAG_SCHISM_STRAIN = "SchismStrain";
    private static final String TAG_SCHISM_TICKS = "SchismTicks";
    private static final String TAG_VOLATILE_STIM = "VolatileStimuli";
    private static final String TAG_VOLATILE_HABIT = "VolatileHabits";
    private static final String TAG_BUFFER_AGE = "BufferAgeTicks";
    private static final String TAG_BOSS_ANCHOR_OWNER = "BossAnchorOwner";
    public static final int BUFFER_CONSOLIDATION_TICKS = 600; // 30s

    private int alertTicksRemaining = 0;
    private int biomassCharge = 0;
    private int symbioticStability = 100;
    private int schismStrainId = 0;
    private int schismTicksRemaining = 0;
    private int bufferAgeTicks = 0;
    private UUID bossAnchorOwner;
    private int tickCounter = 0;
    private final EnumMap<StimulusType, Double> volatileStimuli = new EnumMap<>(StimulusType.class);
    private final EnumMap<PlayerHabit, Double> volatileHabits = new EnumMap<>(PlayerHabit.class);

    public SynapticNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SYNAPTIC_NODE.get(), pos, state);
    }

    public SynapticNodeType getNodeType() {
        Block block = getBlockState().getBlock();
        if (block instanceof SymbioticNodeBlock) {
            return SynapticNodeType.SYMBIOTIC;
        }
        if (block instanceof IncubatorNodeBlock) {
            return SynapticNodeType.INCUBATOR;
        }
        if (block instanceof SentinelNodeBlock) {
            return SynapticNodeType.SENTINEL;
        }
        return SynapticNodeType.RELAY;
    }

    public int getSymbioticStability() {
        return symbioticStability;
    }

    public void setSymbioticStability(int stability) {
        this.symbioticStability = Math.max(0, Math.min(100, stability));
        setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.updateNeighbourForOutputSignal(this.worldPosition, getBlockState().getBlock());
        }
    }

    public boolean canRechargeSymbiotic(ItemStack stack) {
        return getNodeType() == SynapticNodeType.SYMBIOTIC
                && !stack.isEmpty()
                && stack.is(ModItems.MYCELIAL_BIOMASS.get())
                && this.symbioticStability < 100;
    }

    public boolean tryRechargeSymbiotic(ItemStack stack, boolean simulate) {
        if (!canRechargeSymbiotic(stack)) {
            return false;
        }
        if (simulate) {
            return true;
        }
        int nextStability = Math.min(100, this.symbioticStability + 15);
        setSymbioticStability(nextStability);
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5,
                    4, 0.35, 0.35, 0.35, 0.04);
            serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5,
                    6, 0.35, 0.35, 0.35, 0.02);
            serverLevel.playSound(null, worldPosition, ModSounds.SYMBIOTIC_NODE_CHARGE.get(),
                    SoundSource.BLOCKS, 0.85F, 1.15F);
        }
        return true;
    }

    public void triggerAlert(int ticks) {
        this.alertTicksRemaining = Math.max(this.alertTicksRemaining, ticks);
        setChanged();
    }

    public boolean isAlerted() {
        return alertTicksRemaining > 0;
    }

    public void markBossAnchor(UUID owner) {
        if (owner == null) {
            return;
        }
        this.bossAnchorOwner = owner;
        setChanged();
    }

    public boolean isBossAnchorFor(UUID owner) {
        return owner != null && owner.equals(bossAnchorOwner);
    }

    public HiveStrain getSchismStrain() {
        return HiveStrain.byId(schismStrainId);
    }

    public void induceSchism(HiveStrain strain, int durationTicks) {
        if (strain == null || !strain.isSchismStrain()) {
            return;
        }
        this.schismStrainId = strain.id();
        this.schismTicksRemaining = Math.max(this.schismTicksRemaining, durationTicks);
        setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            AABB box = new AABB(this.worldPosition).inflate(16.0D);
            List<MycelialMob> mobs = serverLevel.getEntitiesOfClass(
                    MycelialMob.class, box,
                    mob -> mob.isAlive() && !mob.isAlliedEscort() && !mob.isBossMob()
            );
            for (int i = 0; i < mobs.size(); i++) {
                MycelialMob mob = mobs.get(i);
                HiveStrain assigned = (i % 3 == 2) ? strain.oppositeSchism() : strain;
                mob.infectWithSchism(assigned, durationTicks);
            }
        }
    }

    public int getBiomassCharge() {
        return biomassCharge;
    }

    public void addBiomassCharge(int delta) {
        this.biomassCharge = Math.max(0, Math.min(100, this.biomassCharge + delta));
        setChanged();
    }

    public void addVolatileStimulus(StimulusType type, double amount) {
        if (amount <= 0.0 || getNodeType() == SynapticNodeType.SYMBIOTIC) {
            return;
        }
        volatileStimuli.merge(type, amount, Double::sum);
        setChanged();
    }

    public void addVolatileHabit(PlayerHabit habit, double amount) {
        if (amount <= 0.0 || getNodeType() == SynapticNodeType.SYMBIOTIC) {
            return;
        }
        volatileHabits.merge(habit, amount, Double::sum);
        setChanged();
    }

    public double getVolatileTotal() {
        double sum = 0.0;
        for (double v : volatileStimuli.values()) {
            sum += v;
        }
        for (double v : volatileHabits.values()) {
            sum += v;
        }
        return sum;
    }

    public boolean consolidateVolatileBuffer(ServerLevel level) {
        if (getVolatileTotal() <= 0.0) {
            bufferAgeTicks = 0;
            return false;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        long now = level.getGameTime();
        for (Map.Entry<StimulusType, Double> entry : volatileStimuli.entrySet()) {
            if (entry.getValue() > 0.0) {
                hive.recordStimulus(entry.getKey(), entry.getValue(), now);
            }
        }
        for (Map.Entry<PlayerHabit, Double> entry : volatileHabits.entrySet()) {
            if (entry.getValue() > 0.0) {
                hive.recordHabit(entry.getKey(), entry.getValue());
            }
        }
        volatileStimuli.clear();
        volatileHabits.clear();
        bufferAgeTicks = 0;
        setChanged();
        level.sendParticles(ModParticles.ENGRAM.get(),
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                10, 0.3, 0.4, 0.3, 0.04);
        return true;
    }

    public double purgeVolatileBuffer(ServerLevel level, BlockPos pos, boolean dropEngram) {
        double total = getVolatileTotal();
        volatileStimuli.clear();
        volatileHabits.clear();
        bufferAgeTicks = 0;
        setChanged();
        if (dropEngram && total >= 10.0) {
            ItemEntity drop = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    new ItemStack(ModItems.VOLATILE_ENGRAM.get()));
            level.addFreshEntity(drop);
        }
        return total;
    }

    public static boolean canIncubatorOperate(ServerLevel level, BlockPos pos, HiveMindSavedData hive, boolean alerted) {
        if (alerted) {
            return true;
        }
        if (MycelialMob.isOverworldSurface(level, pos.above()) && level.isDay()) {
            return hive.getThreatLevel() >= 25.0 || hive.isFungalTideActive(level.getGameTime());
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SynapticNodeBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        be.tickCounter++;
        if (be.alertTicksRemaining > 0) {
            be.alertTicksRemaining--;
            if (be.alertTicksRemaining % 20 == 0) {
                be.setChanged();
            }
        }
        if (be.schismTicksRemaining > 0) {
            be.schismTicksRemaining--;
            if (be.schismTicksRemaining % 20 == 0) {
                if (be.schismTicksRemaining == 0) {
                    be.schismStrainId = 0;
                }
                be.setChanged();
            }
        }

        if (be.tickCounter % 20 != 0) {
            return;
        }
        if (be.schismTicksRemaining > 0) {
            HiveStrain schism = be.getSchismStrain();
            serverLevel.sendParticles(
                    ColorParticleOption.create(ModParticles.HIVE_DUST.get(), schism.argbColor()),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    8, 0.45, 0.45, 0.45, 0.02
            );
            for (MycelialMob mob : serverLevel.getEntitiesOfClass(
                    MycelialMob.class, new AABB(pos).inflate(12.0D),
                    mob -> mob.isAlive() && !mob.isAlliedEscort() && !mob.isBossMob()
            )) {
                if (!mob.isSchismActive()) {
                    mob.infectWithSchism(schism, Math.min(200, be.schismTicksRemaining));
                }
            }
        }

        PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
        SynapticNodeType nodeType = be.getNodeType();
        if (nodeType == SynapticNodeType.SYMBIOTIC) {
            tickSymbioticNode(serverLevel, pos, be, tracker);
            return;
        }

        if (be.getVolatileTotal() > 0.0) {
            be.bufferAgeTicks += 20;
            be.setChanged();
            serverLevel.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    3, 0.3, 0.3, 0.3, 0.02);
            if (be.bufferAgeTicks >= BUFFER_CONSOLIDATION_TICKS) {
                be.consolidateVolatileBuffer(serverLevel);
            }
        }

        if (be.tickCounter % 200 == 0 && !tracker.isProtected(pos)) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            if (hive.hasHabitAdaptation(PlayerHabit.SHADOW_INFILTRATOR)) {
                AABB revealBox = new AABB(pos).inflate(12.0D);
                for (Player p : serverLevel.getEntitiesOfClass(Player.class, revealBox)) {
                    if (!p.isCreative() && !p.isSpectator() && !tracker.isProtected(p.blockPosition())) {
                        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
                    }
                }
                serverLevel.sendParticles(ModParticles.THERMAL_RETICLE.get(),
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        1, 0.0, 0.0, 0.0, 0.0);
                serverLevel.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        6, 0.6, 0.3, 0.6, 0.02);
            }
        }

        if (tracker.isProtected(pos)) {
            return;
        }

        List<BlockPos> linked = tracker.getLinkedNodes(pos);
        // Visual synaptic link beams toward connected nodes (draw once per pair using pos order)
        for (BlockPos other : linked) {
            if (pos.asLong() < other.asLong()) {
                spawnLinkParticles(serverLevel, pos, other, be.isAlerted());
            }
        }

        boolean slowedBySymbiotic = tracker.hasNearbySymbioticNode(pos, 32);
        if (nodeType == SynapticNodeType.SENTINEL) {
            AABB auraBox = new AABB(pos).inflate(14.0D);
            for (MycelialMob mob : serverLevel.getEntitiesOfClass(MycelialMob.class, auraBox)) {
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, false, false));
                if (mob.getAbsorptionAmount() < 4.0F) {
                    mob.setAbsorptionAmount(4.0F);
                }
            }
            if (be.tickCounter % 60 == 0) {
                serverLevel.sendParticles(ModParticles.ENGRAM.get(),
                        pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        5, 0.4, 0.3, 0.4, 0.02);
            }
        } else if (nodeType == SynapticNodeType.RELAY) {
            if (be.tickCounter % 80 == 0) {
                AABB healBox = new AABB(pos).inflate(12.0D);
                for (MycelialMob mob : serverLevel.getEntitiesOfClass(MycelialMob.class, healBox)) {
                    if (mob.getHealth() < mob.getMaxHealth()) {
                        mob.heal(1.5F);
                    }
                }
            }
            if (MycelialConfig.AUTONOMOUS_EXPANSION_ENABLED.get()) {
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                int tideMult = hive.isFungalTideActive(serverLevel.getGameTime()) ? 2 : 1;
                int gain = (linked.isEmpty() ? 2 : 4) * tideMult;
                if (slowedBySymbiotic) {
                    gain = Math.max(1, (int) Math.floor(gain * 0.75));
                }
                be.addBiomassCharge(gain);
                if (be.biomassCharge >= 100 && (!linked.isEmpty() || hive.getThreatLevel() >= 30.0)) {
                    matureRelayNode(serverLevel, pos, tracker);
                }
            }
        } else if (nodeType == SynapticNodeType.INCUBATOR) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            if (!canIncubatorOperate(serverLevel, pos, hive, be.isAlerted())) {
                return;
            }
            int tideMult = hive.isFungalTideActive(serverLevel.getGameTime()) ? 2 : 1;
            int chargeGain = (5 + Math.min(10, linked.size() * 3) + (be.isAlerted() ? 10 : 0)) * tideMult;
            if (slowedBySymbiotic) {
                chargeGain = Math.max(1, (int) Math.floor(chargeGain * 0.75));
            }
            be.addBiomassCharge(chargeGain);

            if (be.biomassCharge >= 100) {
                Player nearestPlayer = serverLevel.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 48.0, true);
                if (nearestPlayer != null && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator()) {
                    if (dispatchIncubatorSquad(serverLevel, pos, nearestPlayer, hive.getThreatLevel(), !linked.isEmpty()) > 0) {
                        be.biomassCharge = 0;
                        be.setChanged();
                    }
                } else if (MycelialConfig.AUTONOMOUS_EXPANSION_ENABLED.get()
                        && tracker.countNodes() < MycelialConfig.MAX_AUTONOMOUS_NODES.get()) {
                    int nearbyArchitects = serverLevel.getEntitiesOfClass(
                            SporeArchitectEntity.class, new AABB(pos).inflate(32.0D)).size();
                    if (nearbyArchitects < 2 && dispatchSporeArchitect(serverLevel, pos)) {
                        be.biomassCharge = 0;
                        be.setChanged();
                    }
                }
            }
        }
    }

    public static SynapticNodeType matureRelayNode(ServerLevel level, BlockPos pos, PurificationZoneTracker tracker) {
        SynapticNodeType targetType = tracker.getRecommendedPromotionType();
        BlockState nextState = targetType == SynapticNodeType.INCUBATOR
                ? ModBlocks.INCUBATOR_NODE.get().defaultBlockState()
                : ModBlocks.SENTINEL_NODE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, nextState);
        tracker.addNode(pos, targetType);
        level.sendParticles(ModParticles.TIDE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                28, 0.4, 0.5, 0.4, 0.06);
        level.playSound(null, pos, ModSounds.SYNAPTIC_NODE_MATURE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return targetType;
    }

    private static boolean dispatchSporeArchitect(ServerLevel level, BlockPos pos) {
        SporeArchitectEntity architect = ModEntities.SPORE_ARCHITECT.get().create(level);
        if (architect == null) {
            return false;
        }
        BlockPos spawnPos = MycelialSpawnUtil.findOpenGroundPosition(level, architect, pos, 3, 2);
        if (spawnPos == null) {
            return false;
        }
        architect.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null);
        architect.setOriginNode(pos);
        if (!level.noCollision(architect) || !level.addFreshEntity(architect)) {
            return false;
        }
        level.sendParticles(ModParticles.DESYNC_FRAGMENT.get(), spawnPos.getX() + 0.5, spawnPos.getY() + 1.2, spawnPos.getZ() + 0.5,
                12, 0.3, 0.4, 0.3, 0.04);
        return true;
    }

    private static int dispatchIncubatorSquad(ServerLevel level, BlockPos pos, Player target, double threat, boolean hasLinks) {
        var pair = com.alex.mycelialmind.hive.HiveSquadComposer.composeIncubatorPair(
                com.alex.mycelialmind.hive.HiveMindSavedData.get(level),
                com.alex.mycelialmind.hive.HiveBlackboard.getDoctrine(level),
                hasLinks
        );
        MycelialMob first = pair.get(0).create(level);
        MycelialMob escort = pair.get(1).create(level);
        int spawned = 0;

        if (first != null) {
            BlockPos spawnPos = MycelialSpawnUtil.findOpenGroundPosition(level, first, pos, 3, 2);
            if (spawnPos != null) {
                first.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null);
                if (hasLinks) {
                    first.promoteRank(threat >= 65.0 ? EvolutionRank.ELITE : EvolutionRank.MUTATED);
                }
                first.setTarget(target);
                if (level.noCollision(first) && level.addFreshEntity(first)) {
                    spawned++;
                }
            }
        }
        if (escort != null) {
            BlockPos spawnPos = MycelialSpawnUtil.findOpenGroundPosition(level, escort, pos, 3, 2);
            if (spawnPos != null) {
                escort.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null);
                if (hasLinks) {
                    escort.promoteRank(EvolutionRank.MUTATED);
                }
                escort.setTarget(target);
                if (level.noCollision(escort) && level.addFreshEntity(escort)) {
                    spawned++;
                }
            }
        }
        if (spawned > 0) {
            level.sendParticles(ModParticles.DESYNC_FRAGMENT.get(),
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    14, 0.4, 0.4, 0.4, 0.05);
            level.playSound(null, pos, ModSounds.SYNAPTIC_NODE_MATURE.get(), SoundSource.BLOCKS, 0.85F, 1.15F);
        }
        return spawned;
    }

    private static void spawnLinkParticles(ServerLevel level, BlockPos a, BlockPos b, boolean alerted) {
        int steps = 6;
        double ax = a.getX() + 0.5;
        double ay = a.getY() + 0.7;
        double az = a.getZ() + 0.5;
        double dx = (b.getX() + 0.5) - ax;
        double dy = (b.getY() + 0.7) - ay;
        double dz = (b.getZ() + 0.5) - az;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            level.sendParticles(
                    alerted ? ModParticles.NECROTIC_MIST.get() : ModParticles.HIVE_PULSE.get(),
                    ax + dx * t, ay + dy * t, az + dz * t,
                    1, 0.02, 0.02, 0.02, 0.0
            );
        }
    }

    private static void tickSymbioticNode(ServerLevel level, BlockPos pos, SynapticNodeBlockEntity be, PurificationZoneTracker tracker) {
        List<BlockPos> linkedSymbiotic = tracker.getLinkedSymbioticNodes(pos, MycelialConfig.MYCORRHIZAL_TRAVEL_RANGE.get());
        for (BlockPos other : linkedSymbiotic) {
            if (pos.asLong() < other.asLong()) {
                spawnSymbioticLinkParticles(level, pos, other);
            }
        }
        if (be.tickCounter % 40 == 0) {
            level.sendParticles(ModParticles.BIO_LUME.get(),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    3, 0.35, 0.35, 0.35, 0.01);
        }

        HiveMindSavedData hive = HiveMindSavedData.get(level);
        if (be.tickCounter % 400 == 0) {
            StimulusType dominant = HiveSquadComposer.findDominantStimulus(hive);
            hive.bleedStimulusRatio(dominant, 0.02, level.getGameTime());
        }

        int hostileCount = level.getEntitiesOfClass(
                MycelialMob.class,
                new AABB(pos).inflate(6.0D),
                mob -> mob.isAlive() && !mob.isAlliedEscort()
        ).size();
        int drain = 0;
        if (hostileCount > 0) {
            drain += hostileCount * 4;
        }
        if (hive.isFungalTideActive(level.getGameTime())) {
            drain += 2;
        }
        if (drain > 0) {
            be.setSymbioticStability(be.symbioticStability - drain);
            level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    6, 0.35, 0.35, 0.35, 0.02);
            if (be.symbioticStability <= 0) {
                level.setBlockAndUpdate(pos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
                tracker.addNode(pos, SynapticNodeType.RELAY);
                level.playSound(null, pos, ModSounds.SYNAPTIC_NODE_MATURE.get(), SoundSource.BLOCKS, 1.0F, 0.75F);
                level.sendParticles(ModParticles.DESYNC_FRAGMENT.get(),
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        20, 0.4, 0.4, 0.4, 0.05);
            }
        }
    }

    private static void spawnSymbioticLinkParticles(ServerLevel level, BlockPos a, BlockPos b) {
        int steps = 8;
        double ax = a.getX() + 0.5;
        double ay = a.getY() + 0.7;
        double az = a.getZ() + 0.5;
        double dx = (b.getX() + 0.5) - ax;
        double dy = (b.getY() + 0.7) - ay;
        double dz = (b.getZ() + 0.5) - az;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            level.sendParticles(ModParticles.BIO_LUME.get(),
                    ax + dx * t, ay + dy * t, az + dz * t,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_ALERT_TICKS, alertTicksRemaining);
        tag.putInt(TAG_BIOMASS, biomassCharge);
        tag.putInt(TAG_STABILITY, symbioticStability);
        tag.putInt(TAG_SCHISM_STRAIN, schismStrainId);
        tag.putInt(TAG_SCHISM_TICKS, schismTicksRemaining);
        tag.putInt(TAG_BUFFER_AGE, bufferAgeTicks);
        if (bossAnchorOwner != null) {
            tag.putUUID(TAG_BOSS_ANCHOR_OWNER, bossAnchorOwner);
        }
        CompoundTag vStim = new CompoundTag();
        for (Map.Entry<StimulusType, Double> entry : volatileStimuli.entrySet()) {
            vStim.putDouble(entry.getKey().id(), entry.getValue());
        }
        tag.put(TAG_VOLATILE_STIM, vStim);
        CompoundTag vHabit = new CompoundTag();
        for (Map.Entry<PlayerHabit, Double> entry : volatileHabits.entrySet()) {
            vHabit.putDouble(entry.getKey().id(), entry.getValue());
        }
        tag.put(TAG_VOLATILE_HABIT, vHabit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.alertTicksRemaining = tag.getInt(TAG_ALERT_TICKS);
        this.biomassCharge = tag.getInt(TAG_BIOMASS);
        this.symbioticStability = tag.contains(TAG_STABILITY) ? tag.getInt(TAG_STABILITY) : 100;
        this.schismStrainId = tag.getInt(TAG_SCHISM_STRAIN);
        this.schismTicksRemaining = tag.getInt(TAG_SCHISM_TICKS);
        this.bufferAgeTicks = tag.getInt(TAG_BUFFER_AGE);
        this.bossAnchorOwner = tag.hasUUID(TAG_BOSS_ANCHOR_OWNER) ? tag.getUUID(TAG_BOSS_ANCHOR_OWNER) : null;
        this.volatileStimuli.clear();
        if (tag.contains(TAG_VOLATILE_STIM)) {
            CompoundTag vStim = tag.getCompound(TAG_VOLATILE_STIM);
            for (StimulusType st : StimulusType.values()) {
                if (vStim.contains(st.id())) {
                    this.volatileStimuli.put(st, Math.max(0.0, vStim.getDouble(st.id())));
                }
            }
        }
        this.volatileHabits.clear();
        if (tag.contains(TAG_VOLATILE_HABIT)) {
            CompoundTag vHabit = tag.getCompound(TAG_VOLATILE_HABIT);
            for (PlayerHabit ph : PlayerHabit.values()) {
                if (vHabit.contains(ph.id())) {
                    this.volatileHabits.put(ph, Math.max(0.0, vHabit.getDouble(ph.id())));
                }
            }
        }
    }
}
