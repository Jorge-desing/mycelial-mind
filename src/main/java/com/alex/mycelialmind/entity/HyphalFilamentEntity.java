package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.hive.HyphalPhysics;
import com.alex.mycelialmind.hive.HyphalSiegeDirector;
import com.alex.mycelialmind.hive.HyphalVerletRope;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated 3D suspended hyphal filament entity simulated with a multi-node Verlet rope
 * between two arbitrary world anchors: exhibits gravity sag, wind sway, entity collision
 * deflection, and tension/anchor-loss rupture.
 */
public class HyphalFilamentEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> DATA_ANCHOR_A =
            SynchedEntityData.defineId(HyphalFilamentEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_ANCHOR_B =
            SynchedEntityData.defineId(HyphalFilamentEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Float> DATA_REST_LENGTH =
            SynchedEntityData.defineId(HyphalFilamentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TENSION_RATIO =
            SynchedEntityData.defineId(HyphalFilamentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURGOR =
            SynchedEntityData.defineId(HyphalFilamentEntity.class, EntityDataSerializers.FLOAT);

    private static final String TAG_ANCHOR_A = "AnchorA";
    private static final String TAG_ANCHOR_B = "AnchorB";
    private static final String TAG_REST_LENGTH = "RestLength";
    private static final String TAG_TURGOR = "Turgor";

    private final Vec3[] verletPos = new Vec3[HyphalVerletRope.DEFAULT_NODE_COUNT];
    private final Vec3[] verletPrevPos = new Vec3[HyphalVerletRope.DEFAULT_NODE_COUNT];
    private boolean verletInitialized = false;
    private BlockPos lastInitAnchorA = BlockPos.ZERO;
    private BlockPos lastInitAnchorB = BlockPos.ZERO;

    public HyphalFilamentEntity(EntityType<? extends HyphalFilamentEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public HyphalFilamentEntity(Level level, BlockPos anchorA, BlockPos anchorB, float turgor) {
        this(ModEntities.HYPHAL_FILAMENT.get(), level);
        setAnchors(anchorA, anchorB);
        setTurgor(turgor);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ANCHOR_A, BlockPos.ZERO);
        builder.define(DATA_ANCHOR_B, BlockPos.ZERO);
        builder.define(DATA_REST_LENGTH, 2.0F);
        builder.define(DATA_TENSION_RATIO, 1.0F);
        builder.define(DATA_TURGOR, 0.5F);
    }

    public void setAnchors(BlockPos anchorA, BlockPos anchorB) {
        BlockPos a = anchorA.immutable();
        BlockPos b = anchorB.immutable();
        entityData.set(DATA_ANCHOR_A, a);
        entityData.set(DATA_ANCHOR_B, b);
        Vec3 pA = Vec3.atCenterOf(a);
        Vec3 pB = Vec3.atCenterOf(b);
        float restLen = (float) Math.max(1.0D, pA.distanceTo(pB)) * 1.05F;
        entityData.set(DATA_REST_LENGTH, restLen);
        Vec3 mid = pA.lerp(pB, 0.5D);
        setPos(mid.x, mid.y, mid.z);
        initVerletNodes(pA, pB, a, b);
    }

    public BlockPos getAnchorA() {
        return entityData.get(DATA_ANCHOR_A);
    }

    public BlockPos getAnchorB() {
        return entityData.get(DATA_ANCHOR_B);
    }

    public float getRestLength() {
        return entityData.get(DATA_REST_LENGTH);
    }

    public float getTensionRatio() {
        return entityData.get(DATA_TENSION_RATIO);
    }

    public float getTurgor() {
        return entityData.get(DATA_TURGOR);
    }

    public void setTurgor(float turgor) {
        float clamped = Float.isFinite(turgor) ? Math.max(0.0F, Math.min(1.0F, turgor)) : 0.0F;
        entityData.set(DATA_TURGOR, clamped);
    }

    private void initVerletNodes(Vec3 pA, Vec3 pB, BlockPos a, BlockPos b) {
        Vec3[] init = HyphalVerletRope.initLine(pA, pB, HyphalVerletRope.DEFAULT_NODE_COUNT);
        for (int i = 0; i < init.length; i++) {
            verletPos[i] = init[i];
            verletPrevPos[i] = init[i];
        }
        verletInitialized = true;
        lastInitAnchorA = a.immutable();
        lastInitAnchorB = b.immutable();
    }

    public Vec3[] getNodePositions() {
        ensureVerletInitialized();
        return verletPos.clone();
    }

    public Vec3 getMidpointNode() {
        ensureVerletInitialized();
        return verletPos[verletPos.length / 2];
    }

    private void ensureVerletInitialized() {
        BlockPos a = getAnchorA();
        BlockPos b = getAnchorB();
        if (!verletInitialized || !a.equals(lastInitAnchorA) || !b.equals(lastInitAnchorB)) {
            initVerletNodes(Vec3.atCenterOf(a), Vec3.atCenterOf(b), a, b);
        }
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos a = getAnchorA();
        BlockPos b = getAnchorB();
        if (a.equals(b)) {
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }
        ensureVerletInitialized();

        if (!level().isClientSide() && (!isAnchorIntact(level(), a) || !isAnchorIntact(level(), b))) {
            snapAndDiscard();
            return;
        }

        Vec3 pA = Vec3.atCenterOf(a);
        Vec3 pB = Vec3.atCenterOf(b);
        double restLength = Math.max(0.5D, getRestLength());
        double segLen = Math.max(0.1D, restLength / (double) (HyphalVerletRope.DEFAULT_NODE_COUNT - 1));

        double phase = (tickCount + getId() * 7) * 0.08D;
        Vec3 wind = new Vec3(Math.sin(phase) * 0.0025D, 0.0D, Math.cos(phase * 0.7D) * 0.0025D);

        AABB spanBox = new AABB(pA, pB).inflate(1.2D);
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, spanBox)) {
            if (!living.isSpectator() && living.isAlive()) {
                HyphalVerletRope.applyEntityImpulse(
                        verletPos,
                        verletPrevPos,
                        living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D),
                        living.getDeltaMovement(),
                        1.1D);
            }
        }

        HyphalVerletRope.stepVerlet(
                verletPos,
                verletPrevPos,
                pA,
                pB,
                segLen,
                HyphalVerletRope.DEFAULT_GRAVITY,
                HyphalVerletRope.DEFAULT_DAMPING,
                wind,
                HyphalVerletRope.DEFAULT_CONSTRAINT_ITERATIONS);

        double tension = HyphalVerletRope.computeTensionRatio(verletPos, restLength);
        if (!level().isClientSide()) {
            entityData.set(DATA_TENSION_RATIO, (float) tension);
            if (HyphalVerletRope.shouldSnap(tension, HyphalVerletRope.DEFAULT_MAX_TENSION_RATIO)) {
                snapAndDiscard();
            }
        }
    }

    private static boolean isAnchorIntact(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.liquid();
    }

    public void snapAndDiscard() {
        if (level() instanceof ServerLevel serverLevel) {
            ensureVerletInitialized();
            int count = HyphalPhysics.isPressurizedForSpray(getTurgor()) ? 6 : 3;
            for (int i = 1; i < verletPos.length - 1; i += 2) {
                Vec3 node = verletPos[i];
                serverLevel.sendParticles(ModParticles.SPORE_MOTE.get(),
                        node.x, node.y, node.z, count, 0.12D, 0.12D, 0.12D, 0.03D);
            }
            Vec3 mid = verletPos[verletPos.length / 2];
            serverLevel.playSound(null, mid.x, mid.y, mid.z,
                    ModSounds.EPHEMERAL_CHITIN_CRUMBLE.get(), SoundSource.BLOCKS, 0.65F, 1.35F);
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
        if (held.is(Items.SHEARS) || HyphalSiegeDirector.isSeveringTool(held)) {
            if (!level().isClientSide()) {
                snapAndDiscard();
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
        if (!level().isClientSide()) {
            snapAndDiscard();
        }
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0D * 64.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TAG_ANCHOR_A) && tag.contains(TAG_ANCHOR_B)) {
            BlockPos a = BlockPos.of(tag.getLong(TAG_ANCHOR_A));
            BlockPos b = BlockPos.of(tag.getLong(TAG_ANCHOR_B));
            entityData.set(DATA_ANCHOR_A, a);
            entityData.set(DATA_ANCHOR_B, b);
            initVerletNodes(Vec3.atCenterOf(a), Vec3.atCenterOf(b), a, b);
        }
        if (tag.contains(TAG_REST_LENGTH)) {
            entityData.set(DATA_REST_LENGTH, Math.max(0.5F, tag.getFloat(TAG_REST_LENGTH)));
        }
        if (tag.contains(TAG_TURGOR)) {
            setTurgor(tag.getFloat(TAG_TURGOR));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong(TAG_ANCHOR_A, getAnchorA().asLong());
        tag.putLong(TAG_ANCHOR_B, getAnchorB().asLong());
        tag.putFloat(TAG_REST_LENGTH, getRestLength());
        tag.putFloat(TAG_TURGOR, getTurgor());
    }
}
