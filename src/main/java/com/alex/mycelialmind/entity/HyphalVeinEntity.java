package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HyphalPhysics;
import com.alex.mycelialmind.hive.HyphalSiegeDirector;
import com.alex.mycelialmind.hive.HyphalVerletRope;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Unified 3D Hyphal Vein entity replacing HyphalVeinBlock and HyphalFilamentEntity.
 * Each entity represents a continuous multi-node classic Verlet strand (2..32 nodes)
 * pinned only at its endpoints, with free interior nodes colliding against solid terrain,
 * organic entity drag/boost, traveling mechanical/seismic waves, fluid turgor pressure,
 * finite-speed literal-trail growth, mid-strand sever splitting + progressive withering,
 * and length-scaled opacity/color (nearly transparent when short -> opaque dark obsidian when long).
 */
public class HyphalVeinEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> DATA_ROOT_ANCHOR =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_TIP_ANCHOR =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> DATA_PIN_START =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PIN_END =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_ROOT =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_NUTRIENT =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STRENGTH_FLOOR =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TURGOR =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TENSION_RATIO =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WAVE_POS =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WAVE_AMP =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_REST_LENGTH =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WITHER_PROGRESS =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> DATA_PACKED_NODES =
            SynchedEntityData.defineId(HyphalVeinEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private static final String TAG_ROOT_ANCHOR = "RootAnchor";
    private static final String TAG_TIP_ANCHOR = "TipAnchor";
    private static final String TAG_PIN_START = "PinStart";
    private static final String TAG_PIN_END = "PinEnd";
    private static final String TAG_HAS_ROOT = "HasRoot";
    private static final String TAG_NUTRIENT = "Nutrient";
    private static final String TAG_STRENGTH_FLOOR = "StrengthFloor";
    private static final String TAG_TURGOR = "Turgor";
    private static final String TAG_REST_LENGTH = "RestLength";
    private static final String TAG_WITHER = "WitherProgress";
    private static final String TAG_TRAIL_INDEX = "TrailIndex";
    private static final String TAG_NODES = "Nodes";
    private static final String TAG_VERSION = "Ver";

    private Vec3[] verletPos = new Vec3[0];
    private Vec3[] verletPrevPos = new Vec3[0];
    private int localNodeVersion = -1;
    private int trailIndex = -1;
    private int freeFallTicks = 0;
    private int waveDirection = 1;
    private long lastWaveTick = -100L;
    private long lastStepWaveTick = -100L;
    private long nextGrowthTick = 0L;

    public HyphalVeinEntity(EntityType<? extends HyphalVeinEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public HyphalVeinEntity(
            Level level,
            BlockPos rootAnchor,
            BlockPos tipAnchor,
            int nodeCount,
            boolean hasRoot,
            float turgor
    ) {
        this(ModEntities.HYPHAL_VEIN.get(), level);
        Vec3 pA = computeAnchorVec(level, rootAnchor);
        Vec3 pB = computeAnchorVec(level, tipAnchor);
        Vec3[] init = HyphalVerletRope.initLine(pA, pB, nodeCount);
        configureStrand(
                init,
                rootAnchor,
                tipAnchor,
                true,
                true,
                hasRoot,
                hasRoot ? HyphalPhysics.MAX_NUTRIENT : 0,
                turgor,
                Math.max(1.0D, pA.distanceTo(pB)) * 1.05D
        );
    }

    public HyphalVeinEntity(
            Level level,
            List<Vec3> nodePositions,
            BlockPos rootAnchor,
            BlockPos tipAnchor,
            boolean pinStart,
            boolean pinEnd,
            boolean hasRoot,
            int nutrient,
            float turgor
    ) {
        this(ModEntities.HYPHAL_VEIN.get(), level);
        Vec3[] arr = nodePositions.toArray(new Vec3[0]);
        if (arr.length < HyphalVerletRope.MIN_NODE_COUNT) {
            Vec3 pA = computeAnchorVec(level, rootAnchor);
            Vec3 pB = computeAnchorVec(level, tipAnchor);
            arr = HyphalVerletRope.initLine(pA, pB, HyphalVerletRope.MIN_NODE_COUNT);
        }
        double naturalLen = Math.max(0.8D, HyphalVerletRope.computeTotalLength(arr)) * 1.03D;
        configureStrand(arr, rootAnchor, tipAnchor, pinStart, pinEnd, hasRoot, nutrient, turgor, naturalLen);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ROOT_ANCHOR, BlockPos.ZERO);
        builder.define(DATA_TIP_ANCHOR, BlockPos.ZERO);
        builder.define(DATA_PIN_START, Boolean.TRUE);
        builder.define(DATA_PIN_END, Boolean.TRUE);
        builder.define(DATA_HAS_ROOT, Boolean.TRUE);
        builder.define(DATA_NUTRIENT, HyphalPhysics.MAX_NUTRIENT);
        builder.define(DATA_STRENGTH_FLOOR, 0);
        builder.define(DATA_TURGOR, 0.5F);
        builder.define(DATA_TENSION_RATIO, 1.0F);
        builder.define(DATA_WAVE_POS, -1.0F);
        builder.define(DATA_WAVE_AMP, 0.0F);
        builder.define(DATA_REST_LENGTH, 2.0F);
        builder.define(DATA_WITHER_PROGRESS, 0.0F);
        builder.define(DATA_PACKED_NODES, new CompoundTag());
    }

    public void configureStrand(
            Vec3[] nodes,
            BlockPos rootAnchor,
            BlockPos tipAnchor,
            boolean pinStart,
            boolean pinEnd,
            boolean hasRoot,
            int nutrient,
            float turgor,
            double restLength
    ) {
        this.verletPos = nodes.clone();
        this.verletPrevPos = nodes.clone();
        entityData.set(DATA_ROOT_ANCHOR, rootAnchor.immutable());
        entityData.set(DATA_TIP_ANCHOR, tipAnchor.immutable());
        entityData.set(DATA_PIN_START, pinStart);
        entityData.set(DATA_PIN_END, pinEnd);
        entityData.set(DATA_HAS_ROOT, hasRoot);
        entityData.set(DATA_NUTRIENT, Math.max(0, Math.min(HyphalPhysics.MAX_NUTRIENT, nutrient)));
        entityData.set(DATA_TURGOR, clamp01(turgor));
        entityData.set(DATA_REST_LENGTH, (float) Math.max(0.5D, restLength));
        entityData.set(DATA_WITHER_PROGRESS, 0.0F);
        pushNodesToSynchedData();
        recenterEntity();
    }

    private void pushNodesToSynchedData() {
        CompoundTag tag = new CompoundTag();
        int nextVer = Math.max(0, localNodeVersion) + 1;
        tag.putInt(TAG_VERSION, nextVer);
        ListTag list = new ListTag();
        for (Vec3 v : verletPos) {
            list.add(DoubleTag.valueOf(v.x));
            list.add(DoubleTag.valueOf(v.y));
            list.add(DoubleTag.valueOf(v.z));
        }
        tag.put(TAG_NODES, list);
        this.localNodeVersion = nextVer;
        entityData.set(DATA_PACKED_NODES, tag);
    }

    private void syncNodesFromClientDataIfNeeded() {
        CompoundTag tag = entityData.get(DATA_PACKED_NODES);
        if (tag.contains(TAG_VERSION)) {
            int ver = tag.getInt(TAG_VERSION);
            if (ver != localNodeVersion && tag.contains(TAG_NODES, Tag.TAG_LIST)) {
                ListTag list = tag.getList(TAG_NODES, Tag.TAG_DOUBLE);
                int count = list.size() / 3;
                if (count >= HyphalVerletRope.MIN_NODE_COUNT) {
                    Vec3[] loaded = new Vec3[count];
                    for (int i = 0; i < count; i++) {
                        loaded[i] = new Vec3(
                                list.getDouble(i * 3),
                                list.getDouble(i * 3 + 1),
                                list.getDouble(i * 3 + 2));
                    }
                    this.verletPos = loaded;
                    this.verletPrevPos = loaded.clone();
                    this.localNodeVersion = ver;
                    return;
                }
            }
        }
        if (verletPos.length < HyphalVerletRope.MIN_NODE_COUNT) {
            Vec3 pA = computeAnchorVec(level(), getRootAnchor());
            Vec3 pB = computeAnchorVec(level(), getTipAnchor());
            this.verletPos = HyphalVerletRope.initLine(pA, pB, HyphalVerletRope.DEFAULT_NODE_COUNT);
            this.verletPrevPos = this.verletPos.clone();
        }
    }

    public static Vec3 computeAnchorVec(Level level, BlockPos anchor) {
        if (level != null && level.isLoaded(anchor)) {
            if (level.getBlockState(anchor).blocksMotion()) {
                return Vec3.atCenterOf(anchor);
            }
            if (level.isLoaded(anchor.below()) && level.getBlockState(anchor.below()).blocksMotion()) {
                return new Vec3(anchor.getX() + 0.5D, anchor.getY() + 0.06D, anchor.getZ() + 0.5D);
            }
        }
        return Vec3.atCenterOf(anchor);
    }

    public static boolean isAnchorSupported(Level level, BlockPos anchor) {
        if (!level.isLoaded(anchor)) {
            return true;
        }
        BlockState state = level.getBlockState(anchor);
        if (!state.isAir() && !state.liquid()) {
            return true;
        }
        for (Direction dir : Direction.values()) {
            BlockPos adj = anchor.relative(dir);
            if (level.isLoaded(adj) && level.getBlockState(adj).isFaceSturdy(level, adj, dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    public BlockPos getRootAnchor() {
        return entityData.get(DATA_ROOT_ANCHOR);
    }

    public BlockPos getTipAnchor() {
        return entityData.get(DATA_TIP_ANCHOR);
    }

    public boolean isPinStart() {
        return entityData.get(DATA_PIN_START);
    }

    public void setPinStart(boolean pin) {
        entityData.set(DATA_PIN_START, pin);
    }

    public boolean isPinEnd() {
        return entityData.get(DATA_PIN_END);
    }

    public void setPinEnd(boolean pin) {
        entityData.set(DATA_PIN_END, pin);
    }

    public boolean hasRoot() {
        return entityData.get(DATA_HAS_ROOT);
    }

    public void setHasRoot(boolean hasRoot) {
        entityData.set(DATA_HAS_ROOT, hasRoot);
        if (hasRoot) {
            entityData.set(DATA_NUTRIENT, HyphalPhysics.MAX_NUTRIENT);
            entityData.set(DATA_WITHER_PROGRESS, 0.0F);
        }
    }

    public int getNutrient() {
        return entityData.get(DATA_NUTRIENT);
    }

    public void setNutrient(int nutrient) {
        entityData.set(DATA_NUTRIENT, Math.max(0, Math.min(HyphalPhysics.MAX_NUTRIENT, nutrient)));
    }

    public int getStrengthFloor() {
        return entityData.get(DATA_STRENGTH_FLOOR);
    }

    public void setStrengthFloor(int strength) {
        entityData.set(DATA_STRENGTH_FLOOR, Math.max(0, Math.min(3, strength)));
    }

    public int getEffectiveStrength() {
        return Math.max(getStrengthFloor(), HyphalVerletRope.strengthForLength(getNodeCount()));
    }

    public boolean isCharged() {
        return HyphalPhysics.chargedForNutrient(getNutrient());
    }

    public float getTurgor() {
        return entityData.get(DATA_TURGOR);
    }

    public void setTurgor(float turgor) {
        entityData.set(DATA_TURGOR, clamp01(turgor));
    }

    public float getTensionRatio() {
        return entityData.get(DATA_TENSION_RATIO);
    }

    public float getWavePos() {
        return entityData.get(DATA_WAVE_POS);
    }

    public float getWaveAmp() {
        return entityData.get(DATA_WAVE_AMP);
    }

    public float getRestLength() {
        return entityData.get(DATA_REST_LENGTH);
    }

    public float getWitherProgress() {
        return entityData.get(DATA_WITHER_PROGRESS);
    }

    public int getTrailIndex() {
        return trailIndex;
    }

    public void setTrailIndex(int trailIndex) {
        this.trailIndex = trailIndex;
    }

    public int getNodeCount() {
        syncNodesFromClientDataIfNeeded();
        return verletPos.length;
    }

    public Vec3[] getNodePositions() {
        syncNodesFromClientDataIfNeeded();
        return verletPos.clone();
    }

    public Vec3 getMidpointNode() {
        syncNodesFromClientDataIfNeeded();
        return verletPos[verletPos.length / 2];
    }

    public double getTotalLength() {
        syncNodesFromClientDataIfNeeded();
        return HyphalVerletRope.computeTotalLength(verletPos);
    }

    public AABB getStrandBoundingBox() {
        syncNodesFromClientDataIfNeeded();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 v : verletPos) {
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            minZ = Math.min(minZ, v.z);
            maxX = Math.max(maxX, v.x);
            maxY = Math.max(maxY, v.y);
            maxZ = Math.max(maxZ, v.z);
        }
        if (!Double.isFinite(minX)) {
            return getBoundingBox();
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(0.35D);
    }

    public boolean touchesBlockPos(BlockPos pos, double radius) {
        syncNodesFromClientDataIfNeeded();
        Vec3 center = Vec3.atCenterOf(pos);
        double rSq = radius * radius;
        for (Vec3 v : verletPos) {
            if (v.distanceToSqr(center) <= rSq) {
                return true;
            }
        }
        return false;
    }

    public boolean extendTipTo(BlockPos newTipAnchor) {
        syncNodesFromClientDataIfNeeded();
        if (verletPos.length >= HyphalVerletRope.MAX_NODE_COUNT) {
            return false;
        }
        Vec3 newTip = computeAnchorVec(level(), newTipAnchor);
        Vec3 oldTip = verletPos[verletPos.length - 1];
        if (oldTip.distanceToSqr(newTip) < 0.04D) {
            return false;
        }
        double addedLen = oldTip.distanceTo(newTip);
        this.verletPos = HyphalVerletRope.extendRope(this.verletPos, newTip, HyphalVerletRope.MAX_NODE_COUNT);
        this.verletPrevPos = HyphalVerletRope.extendRope(this.verletPrevPos, newTip, HyphalVerletRope.MAX_NODE_COUNT);
        entityData.set(DATA_TIP_ANCHOR, newTipAnchor.immutable());
        entityData.set(DATA_PIN_END, Boolean.TRUE);
        entityData.set(DATA_REST_LENGTH, (float) Math.max(getRestLength() + addedLen * 1.03D,
                HyphalVerletRope.computeTotalLength(verletPos) * 1.02D));
        pushNodesToSynchedData();
        recenterEntity();
        return true;
    }

    private void recenterEntity() {
        if (verletPos.length > 0) {
            Vec3 mid = verletPos[verletPos.length / 2];
            setPos(mid.x, mid.y, mid.z);
            setBoundingBox(getStrandBoundingBox());
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getStrandBoundingBox().inflate(0.5D);
    }

    public boolean injectWave(int startNodeIndex, float amplitude) {
        if (!Float.isFinite(amplitude) || amplitude < HyphalPhysics.WAVE_MIN_AMPLITUDE) {
            return false;
        }
        long now = level().getGameTime();
        if ((now - lastWaveTick) < HyphalPhysics.WAVE_COOLDOWN_TICKS && amplitude <= getWaveAmp()) {
            return false;
        }
        syncNodesFromClientDataIfNeeded();
        int clampedIdx = Math.max(0, Math.min(verletPos.length - 1, startNodeIndex));
        this.waveDirection = clampedIdx >= (verletPos.length / 2) ? -1 : 1;
        this.lastWaveTick = now;
        entityData.set(DATA_WAVE_POS, (float) clampedIdx);
        entityData.set(DATA_WAVE_AMP, Math.min(1.0F, amplitude));
        return true;
    }

    public boolean injectWaveNear(Vec3 worldPos, float amplitude) {
        syncNodesFromClientDataIfNeeded();
        int idx = HyphalVerletRope.findClosestNodeIndex(verletPos, worldPos);
        return idx >= 0 && injectWave(idx, amplitude);
    }

    public void interactWithEntity(Entity entity) {
        if (entity == this
                || entity instanceof HyphalVeinEntity
                || entity.isSpectator()
                || !entity.isAlive()) {
            return;
        }
        syncNodesFromClientDataIfNeeded();
        Vec3 entityMid = entity.position().add(0.0D, entity.getBbHeight() * 0.4D, 0.0D);
        HyphalVerletRope.applyEntityImpulse(verletPos, verletPrevPos, entityMid, entity.getDeltaMovement(), 1.15D);

        int strength = getEffectiveStrength();
        float turgor = getTurgor();
        boolean isMycelial = entity instanceof MycelialMob || entity instanceof MycelialChimeraEntity;
        boolean wearingChitinBoots = entity instanceof LivingEntity living
                && living.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CHITIN_BOOTS.get());
        boolean crouching = entity.isCrouching();

        float drag = HyphalPhysics.computeEntityDragFactor(
                strength, turgor, isMycelial, wearingChitinBoots, crouching);
        Vec3 vel = entity.getDeltaMovement();
        if (drag > 1.0F) {
            double hSq = vel.x * vel.x + vel.z * vel.z;
            if (hSq > 1.0E-4D && hSq < 0.20D) {
                double nx = Math.max(-0.45D, Math.min(0.45D, vel.x * drag));
                double nz = Math.max(-0.45D, Math.min(0.45D, vel.z * drag));
                entity.setDeltaMovement(nx, vel.y, nz);
            }
        } else if (drag < 1.0F) {
            entity.setDeltaMovement(vel.x * drag, vel.y, vel.z * drag);
        }

        if (level() instanceof ServerLevel serverLevel
                && !isMycelial
                && !crouching
                && !wearingChitinBoots
                && entity instanceof LivingEntity) {
            double hSq = vel.x * vel.x + vel.z * vel.z;
            long now = serverLevel.getGameTime();
            if ((hSq > 0.008D || entity.fallDistance > 0.5F) && (now - lastStepWaveTick) >= 10L) {
                lastStepWaveTick = now;
                injectWaveNear(entity.position(), 0.45F);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        syncNodesFromClientDataIfNeeded();
        if (verletPos.length < HyphalVerletRope.MIN_NODE_COUNT) {
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }

        BlockPos rootA = getRootAnchor();
        BlockPos tipB = getTipAnchor();
        if (!level().isClientSide()) {
            if (isPinStart() && !isAnchorSupported(level(), rootA)) {
                setPinStart(false);
            }
            if (isPinEnd() && !isAnchorSupported(level(), tipB)) {
                setPinEnd(false);
            }
        }

        boolean pinA = isPinStart();
        boolean pinB = isPinEnd();
        Vec3 pA = computeAnchorVec(level(), rootA);
        Vec3 pB = computeAnchorVec(level(), tipB);
        double restLen = Math.max(0.5D, getRestLength());
        double segLen = Math.max(0.08D, restLen / (double) (verletPos.length - 1));

        double phase = (tickCount + getId() * 7) * 0.08D;
        Vec3 wind = new Vec3(Math.sin(phase) * 0.0022D, 0.0D, Math.cos(phase * 0.7D) * 0.0022D);

        AABB strandBox = getStrandBoundingBox();
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, strandBox.inflate(0.35D))) {
            if (!living.isSpectator() && living.isAlive()) {
                Vec3 lp = living.position();
                int closest = HyphalVerletRope.findClosestNodeIndex(verletPos, lp);
                if (closest >= 0 && verletPos[closest].distanceToSqr(lp) <= 1.21D) {
                    interactWithEntity(living);
                }
            }
        }

        boolean touchedSolid = HyphalVerletRope.stepVerlet(
                verletPos,
                verletPrevPos,
                pA,
                pB,
                pinA,
                pinB,
                segLen,
                HyphalVerletRope.DEFAULT_GRAVITY,
                HyphalVerletRope.DEFAULT_DAMPING,
                wind,
                HyphalVerletRope.DEFAULT_CONSTRAINT_ITERATIONS,
                bp -> !(pinA && bp.equals(rootA))
                        && !(pinB && bp.equals(tipB))
                        && level().isLoaded(bp)
                        && level().getBlockState(bp).blocksMotion()
        );
        recenterEntity();

        if (!pinA && !pinB) {
            freeFallTicks++;
            if (!level().isClientSide() && ((freeFallTicks >= 3 && touchedSolid) || freeFallTicks >= 40)) {
                disintegrateWithSpores();
                return;
            }
        } else {
            freeFallTicks = 0;
        }

        if (level() instanceof ServerLevel serverLevel) {
            double tension = HyphalVerletRope.computeTensionRatio(verletPos, restLen);
            entityData.set(DATA_TENSION_RATIO, (float) tension);
            if (pinA && pinB && HyphalVerletRope.shouldSnap(tension, HyphalVerletRope.DEFAULT_MAX_TENSION_RATIO)) {
                disintegrateWithSpores();
                return;
            }

            // Traveling mechanical/seismic wave step every WAVE_STEP_TICKS
            float waveAmp = getWaveAmp();
            float wavePos = getWavePos();
            if (waveAmp >= HyphalPhysics.WAVE_MIN_AMPLITUDE && wavePos >= 0.0F
                    && tickCount % HyphalPhysics.WAVE_STEP_TICKS == 0) {
                int idx = Math.max(0, Math.min(verletPos.length - 1, Math.round(wavePos)));
                Vec3 nodeVec = verletPos[idx];
                BlockPos nodeBlock = BlockPos.containing(nodeVec);

                if (HyphalPhysics.shouldDetachFromWave(waveAmp, getEffectiveStrength(), getTurgor())) {
                    setPinStart(false);
                    setPinEnd(false);
                    entityData.set(DATA_WAVE_AMP, 0.0F);
                    entityData.set(DATA_WAVE_POS, -1.0F);
                } else {
                    int color = waveAmp >= 0.65F ? 0xFFE68A1E : 0xFF7B3FE4;
                    serverLevel.sendParticles(ColorParticleOption.create(ModParticles.HIVE_DUST.get(), color),
                            nodeVec.x, nodeVec.y + 0.08D, nodeVec.z,
                            waveAmp >= 0.65F ? 5 : 2, 0.18D, 0.08D, 0.18D, 0.02D);
                    alertHiveNearWaveNode(serverLevel, nodeBlock, waveAmp);

                    float nextAmp = HyphalPhysics.nextWaveAmplitude(waveAmp);
                    int nextIdx = idx + waveDirection;
                    if (nextAmp < HyphalPhysics.WAVE_MIN_AMPLITUDE) {
                        entityData.set(DATA_WAVE_AMP, 0.0F);
                        entityData.set(DATA_WAVE_POS, -1.0F);
                    } else if (nextIdx < 0 || nextIdx >= verletPos.length) {
                        // Hand off wave to adjacent HyphalVeinEntity strands
                        for (HyphalVeinEntity neighbor : serverLevel.getEntitiesOfClass(
                                HyphalVeinEntity.class, new AABB(nodeBlock).inflate(1.8D))) {
                            if (neighbor != this) {
                                neighbor.injectWaveNear(nodeVec, nextAmp);
                            }
                        }
                        entityData.set(DATA_WAVE_AMP, 0.0F);
                        entityData.set(DATA_WAVE_POS, -1.0F);
                    } else {
                        entityData.set(DATA_WAVE_POS, (float) nextIdx);
                        entityData.set(DATA_WAVE_AMP, nextAmp);
                    }
                }
            }

            // Periodic turgor recalculation (every 20 ticks)
            if (tickCount % 20 == 0) {
                recomputeTurgor(serverLevel);
            }

            // Progressive withering when disconnected from root (<= 60 ticks)
            if (!hasRoot()) {
                setNutrient(0);
                float nextWither = getWitherProgress() + (1.0F / 60.0F);
                entityData.set(DATA_WITHER_PROGRESS, Math.min(1.0F, nextWither));
                if (tickCount % 10 == 0) {
                    Vec3 mid = getMidpointNode();
                    serverLevel.sendParticles(ModParticles.SPORE_MOTE.get(),
                            mid.x, mid.y + 0.08D, mid.z, 3, 0.25D, 0.10D, 0.25D, 0.02D);
                }
                if (nextWither >= 1.0F) {
                    disintegrateWithSpores();
                    return;
                }
            } else if (verletPos.length < HyphalVerletRope.MAX_NODE_COUNT
                    && trailIndex >= 0
                    && serverLevel.getGameTime() >= nextGrowthTick) {
                boolean grew = HyphalSiegeDirector.growStrandOneStep(serverLevel, this);
                nextGrowthTick = serverLevel.getGameTime() + HyphalPhysics.getGrowthIntervalTicks();
                if (!grew) {
                    nextGrowthTick = serverLevel.getGameTime() + HyphalPhysics.getGrowthIntervalTicks() * 2L;
                }
            }
        }
    }

    private void alertHiveNearWaveNode(ServerLevel level, BlockPos nodePos, float waveAmp) {
        for (BlockPos p : BlockPos.betweenClosed(nodePos.offset(-2, -1, -2), nodePos.offset(2, 1, 2))) {
            if (!level.isLoaded(p)) {
                continue;
            }
            if (level.getBlockEntity(p) instanceof SynapticNodeBlockEntity node
                    && node.getNodeType() != SynapticNodeType.SYMBIOTIC) {
                node.triggerAlert(120);
                HiveBlackboard.getInfluenceMap(level).addInfluence(nodePos, 0.10F, 0.25F * waveAmp, 0.20F);
                return;
            }
        }
        var mobs = level.getEntitiesOfClass(MycelialMob.class, new AABB(nodePos).inflate(4.0D));
        if (!mobs.isEmpty()) {
            HiveBlackboard.getInfluenceMap(level).addInfluence(nodePos, 0.08F, 0.20F * waveAmp, 0.25F);
        }
    }

    public void recomputeTurgor(ServerLevel level) {
        BlockPos midPos = BlockPos.containing(getMidpointNode());
        boolean wet = level.isRainingAt(midPos) || hasWaterNearby(level, midPos);
        boolean hot = level.dimension() == Level.NETHER || hasHeatNearby(level, midPos);
        float next = HyphalPhysics.computeTurgor(getNutrient(), wet, hot);
        if (Math.abs(next - getTurgor()) >= 0.05F) {
            setTurgor(next);
        }
    }

    private static boolean hasWaterNearby(Level level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHeatNearby(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (level.getFluidState(adj).is(FluidTags.LAVA)) {
                return true;
            }
            BlockState st = level.getBlockState(adj);
            if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE) || st.is(Blocks.MAGMA_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Severs this strand near {@code hitPos}. If long enough (>= 4 nodes) and rooted,
     * splits into a rooted surviving strand and a disconnected strand that hangs loose
     * and withers within <= 100 ticks. Triggers pressurized spore spray if turgor >= 0.65.
     */
    public int severAt(Vec3 hitPos, @Nullable Player player, ItemStack tool) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        syncNodesFromClientDataIfNeeded();
        if (HyphalPhysics.isPressurizedForSpray(getTurgor())) {
            HyphalSiegeDirector.triggerPressurizedSpray(serverLevel, BlockPos.containing(hitPos), getTurgor());
        }

        int splitIdx = HyphalVerletRope.findClosestNodeIndex(verletPos, hitPos);
        HyphalVerletRope.SplitResult split = HyphalVerletRope.splitRope(verletPos, splitIdx);
        if (split != null && hasRoot()) {
            BlockPos oldRoot = getRootAnchor();
            BlockPos oldTip = getTipAnchor();
            boolean oldPinStart = isPinStart();
            boolean oldPinEnd = isPinEnd();
            float oldTurgor = getTurgor();

            // Rooted half remains alive, cut end hangs free
            BlockPos newRootTip = BlockPos.containing(split.rootSegment()[split.rootSegment().length - 1]);
            configureStrand(
                    split.rootSegment(),
                    oldRoot,
                    newRootTip,
                    oldPinStart,
                    false,
                    true,
                    HyphalPhysics.MAX_NUTRIENT,
                    oldTurgor,
                    Math.max(0.6D, HyphalVerletRope.computeTotalLength(split.rootSegment())) * 1.03D
            );

            // Disconnected half spawns unrooted with cut end free and withers progressively
            BlockPos severedStart = BlockPos.containing(split.severedSegment()[0]);
            HyphalVeinEntity severedHalf = new HyphalVeinEntity(
                    serverLevel,
                    List.of(split.severedSegment()),
                    severedStart,
                    oldTip,
                    false,
                    oldPinEnd,
                    false,
                    0,
                    oldTurgor * 0.5F
            );
            serverLevel.addFreshEntity(severedHalf);
            if (serverLevel.random.nextFloat() < 0.35F) {
                Block.popResource(serverLevel, BlockPos.containing(hitPos),
                        new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
            }
            return split.severedSegment().length;
        }

        int count = verletPos.length;
        if (serverLevel.random.nextFloat() < 0.35F) {
            Block.popResource(serverLevel, BlockPos.containing(hitPos),
                    new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
        }
        HiveMindSavedData.get(serverLevel).severHyphalVein(getRootAnchor(), 0.0, serverLevel.getGameTime());
        disintegrateWithSpores();
        return Math.max(1, count);
    }

    public void disintegrateWithSpores() {
        if (level() instanceof ServerLevel serverLevel) {
            syncNodesFromClientDataIfNeeded();
            int particleCount = HyphalPhysics.isPressurizedForSpray(getTurgor()) ? 6 : 3;
            for (int i = 0; i < verletPos.length; i += Math.max(1, verletPos.length / 4)) {
                Vec3 node = verletPos[i];
                serverLevel.sendParticles(ModParticles.SPORE_MOTE.get(),
                        node.x, node.y + 0.08D, node.z, particleCount, 0.16D, 0.12D, 0.16D, 0.03D);
            }
            Vec3 mid = getMidpointNode();
            serverLevel.playSound(null, mid.x, mid.y, mid.z,
                    ModSounds.EPHEMERAL_CHITIN_CRUMBLE.get(), SoundSource.BLOCKS, 0.55F, 1.30F);
        }
        discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.BIO_SCANNER.get())) {
            if (level() instanceof ServerLevel serverLevel) {
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                BlockPos pos = getRootAnchor();
                net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(pos);
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.mycelialmind.scanner_hyphal_vein_inspect",
                        String.format("%.1f", hive.getHyphalDensity(cp)),
                        hive.getHyphalTraversalCount(cp),
                        net.minecraft.network.chat.Component.translatable(hive.isTrackedHyphalVein(pos)
                                ? "message.mycelialmind.yes" : "message.mycelialmind.no")
                ).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.45F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (held.is(Items.SHEARS) || HyphalSiegeDirector.isRightClickSeveringTool(held)) {
            if (level() instanceof ServerLevel serverLevel) {
                int severed = severAt(player.getEyePosition().add(player.getLookAngle().scale(2.0D)), player, held);
                if (severed > 0) {
                    held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    HiveMindSavedData.get(serverLevel).severHyphalVein(
                            getRootAnchor(),
                            HyphalSiegeDirector.isSpecializedMycelialTool(held) ? 18.0D : 12.0D,
                            serverLevel.getGameTime());
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        return super.interact(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (level() instanceof ServerLevel) {
            Player player = source.getEntity() instanceof Player p ? p : null;
            ItemStack tool = player != null ? player.getMainHandItem() : ItemStack.EMPTY;
            Vec3 hit = source.getSourcePosition() != null ? source.getSourcePosition() : getMidpointNode();
            severAt(hit, player, tool);
        }
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 80.0D * 80.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        BlockPos r = tag.contains(TAG_ROOT_ANCHOR) ? BlockPos.of(tag.getLong(TAG_ROOT_ANCHOR)) : blockPosition();
        BlockPos t = tag.contains(TAG_TIP_ANCHOR) ? BlockPos.of(tag.getLong(TAG_TIP_ANCHOR)) : blockPosition();
        boolean pStart = !tag.contains(TAG_PIN_START) || tag.getBoolean(TAG_PIN_START);
        boolean pEnd = !tag.contains(TAG_PIN_END) || tag.getBoolean(TAG_PIN_END);
        boolean root = !tag.contains(TAG_HAS_ROOT) || tag.getBoolean(TAG_HAS_ROOT);
        int nutr = tag.contains(TAG_NUTRIENT) ? tag.getInt(TAG_NUTRIENT) : (root ? HyphalPhysics.MAX_NUTRIENT : 0);
        int sFloor = tag.getInt(TAG_STRENGTH_FLOOR);
        float turg = tag.contains(TAG_TURGOR) ? tag.getFloat(TAG_TURGOR) : 0.5F;
        float rLen = tag.contains(TAG_REST_LENGTH) ? tag.getFloat(TAG_REST_LENGTH) : 2.0F;
        float wither = tag.getFloat(TAG_WITHER);
        trailIndex = tag.contains(TAG_TRAIL_INDEX) ? tag.getInt(TAG_TRAIL_INDEX) : -1;

        Vec3[] nodes = null;
        if (tag.contains(TAG_NODES, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_NODES, Tag.TAG_DOUBLE);
            int count = list.size() / 3;
            if (count >= HyphalVerletRope.MIN_NODE_COUNT) {
                nodes = new Vec3[count];
                for (int i = 0; i < count; i++) {
                    nodes[i] = new Vec3(
                            list.getDouble(i * 3),
                            list.getDouble(i * 3 + 1),
                            list.getDouble(i * 3 + 2));
                }
            }
        }
        if (nodes == null) {
            nodes = HyphalVerletRope.initLine(Vec3.atCenterOf(r), Vec3.atCenterOf(t), HyphalVerletRope.DEFAULT_NODE_COUNT);
        }
        configureStrand(nodes, r, t, pStart, pEnd, root, nutr, turg, rLen);
        setStrengthFloor(sFloor);
        entityData.set(DATA_WITHER_PROGRESS, clamp01(wither));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        syncNodesFromClientDataIfNeeded();
        tag.putLong(TAG_ROOT_ANCHOR, getRootAnchor().asLong());
        tag.putLong(TAG_TIP_ANCHOR, getTipAnchor().asLong());
        tag.putBoolean(TAG_PIN_START, isPinStart());
        tag.putBoolean(TAG_PIN_END, isPinEnd());
        tag.putBoolean(TAG_HAS_ROOT, hasRoot());
        tag.putInt(TAG_NUTRIENT, getNutrient());
        tag.putInt(TAG_STRENGTH_FLOOR, getStrengthFloor());
        tag.putFloat(TAG_TURGOR, getTurgor());
        tag.putFloat(TAG_REST_LENGTH, getRestLength());
        tag.putFloat(TAG_WITHER, getWitherProgress());
        tag.putInt(TAG_TRAIL_INDEX, trailIndex);
        ListTag list = new ListTag();
        for (Vec3 v : verletPos) {
            list.add(DoubleTag.valueOf(v.x));
            list.add(DoubleTag.valueOf(v.y));
            list.add(DoubleTag.valueOf(v.z));
        }
        tag.put(TAG_NODES, list);
    }

    private static float clamp01(float v) {
        return Float.isFinite(v) ? Math.max(0.0F, Math.min(1.0F, v)) : 0.0F;
    }
}
