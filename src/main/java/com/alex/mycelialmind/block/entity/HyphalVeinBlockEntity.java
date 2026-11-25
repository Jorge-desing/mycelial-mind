package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.hive.HyphalPhysics;
import com.alex.mycelialmind.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;

/**
 * Lightweight BlockEntity storing diagonal trail connections, surface attachment faces,
 * root status, parent link, and 0-15 nutrient state for a Hyphal Vein without blockstate explosion.
 */
public class HyphalVeinBlockEntity extends BlockEntity {
    public static final long NO_PARENT = Long.MIN_VALUE;

    public static final ModelProperty<Byte> DIAGONALS_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Byte> FACES_PROPERTY = new ModelProperty<>();

    private static final String TAG_DIAGONALS = "Diagonals";
    private static final String TAG_FACES = "Faces";
    private static final String TAG_NUTRIENT = "Nutrient";
    private static final String TAG_DISTANCE_TO_ROOT = "DistanceToRoot";
    private static final String TAG_PARENT_POS = "ParentPos";
    private static final String TAG_HAS_PARENT = "HasParent";
    private static final String TAG_TRAIL_INDEX = "TrailIndex";
    private static final String TAG_ROOT = "Root";
    private static final String TAG_TRAVERSALS = "Traversals";
    private static final String TAG_TURGOR = "Turgor";

    private byte diagonals = 0;
    private byte faces = HyphalPhysics.FACE_DOWN;
    private int nutrient = 0;
    private int distanceToRoot = 0;
    private long parentPos = NO_PARENT;
    private boolean hasParent = false;
    private int trailIndex = -1;
    private boolean root = false;
    private int traversals = 0;
    private float turgor = 0.0F;
    private float waveAmplitude = 0.0F;
    private long lastWaveTick = -100L;
    private long lastEntityStepWaveTick = -100L;
    private long nextGrowthTick = 0L;
    private long nextWitherTick = 0L;

    public HyphalVeinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYPHAL_VEIN.get(), pos, state);
    }

    public byte getDiagonals() {
        return diagonals;
    }

    public boolean setDiagonals(byte diagonals) {
        byte masked = (byte) (diagonals & HyphalPhysics.ALL_DIAGONALS_MASK);
        if (this.diagonals == masked) {
            return false;
        }
        this.diagonals = masked;
        setChanged();
        return true;
    }

    public boolean addDiagonal(byte diagonalBit) {
        return setDiagonals((byte) (this.diagonals | diagonalBit));
    }

    public byte getFaces() {
        return HyphalPhysics.effectiveFaces(faces);
    }

    public boolean setFaces(byte faces) {
        byte normalized = HyphalPhysics.effectiveFaces(faces);
        if (this.faces == normalized) {
            return false;
        }
        this.faces = normalized;
        setChanged();
        return true;
    }

    public boolean setRawFaces(byte faces) {
        byte masked = (byte) (faces & HyphalPhysics.ALL_FACES_MASK);
        if (this.faces == masked) {
            return false;
        }
        this.faces = masked;
        setChanged();
        return true;
    }

    public boolean addFace(byte faceBit) {
        return setFaces((byte) (getFaces() | faceBit));
    }

    public int getNutrient() {
        return nutrient;
    }

    public boolean setNutrient(int nutrient) {
        int clamped = Math.max(0, Math.min(HyphalPhysics.MAX_NUTRIENT, nutrient));
        if (this.nutrient == clamped) {
            return false;
        }
        this.nutrient = clamped;
        setChanged();
        return true;
    }

    public int getDistanceToRoot() {
        return distanceToRoot;
    }

    public void setDistanceToRoot(int distanceToRoot) {
        int clamped = Math.max(0, distanceToRoot);
        if (this.distanceToRoot != clamped) {
            this.distanceToRoot = clamped;
            setChanged();
        }
    }

    public long getParentPos() {
        return hasParent ? parentPos : NO_PARENT;
    }

    @Nullable
    public BlockPos getParentBlockPos() {
        return hasParent ? BlockPos.of(parentPos) : null;
    }

    public boolean hasParent() {
        return hasParent;
    }

    public void setParentPos(@Nullable BlockPos parent) {
        if (parent == null) {
            if (hasParent) {
                hasParent = false;
                parentPos = NO_PARENT;
                setChanged();
            }
        } else {
            long packed = parent.asLong();
            if (!hasParent || parentPos != packed) {
                hasParent = true;
                parentPos = packed;
                setChanged();
            }
        }
    }

    public int getTrailIndex() {
        return trailIndex;
    }

    public void setTrailIndex(int trailIndex) {
        if (this.trailIndex != trailIndex) {
            this.trailIndex = trailIndex;
            setChanged();
        }
    }

    public boolean isRoot() {
        return root;
    }

    public boolean setRoot(boolean root) {
        if (this.root == root) {
            return false;
        }
        this.root = root;
        if (root) {
            this.nutrient = HyphalPhysics.MAX_NUTRIENT;
            this.distanceToRoot = 0;
            this.hasParent = false;
            this.parentPos = NO_PARENT;
        }
        setChanged();
        return true;
    }

    public int getTraversals() {
        return traversals;
    }

    public boolean setTraversals(int traversals) {
        int clamped = Math.max(0, traversals);
        if (this.traversals == clamped) {
            return false;
        }
        this.traversals = clamped;
        setChanged();
        return true;
    }

    public long getNextGrowthTick() {
        return nextGrowthTick;
    }

    public void setNextGrowthTick(long nextGrowthTick) {
        this.nextGrowthTick = nextGrowthTick;
    }

    public long getNextWitherTick() {
        return nextWitherTick;
    }

    public void setNextWitherTick(long nextWitherTick) {
        this.nextWitherTick = nextWitherTick;
    }

    public float getTurgor() {
        return turgor;
    }

    public boolean setTurgor(float turgor) {
        float clamped = Float.isFinite(turgor) ? Math.max(0.0F, Math.min(1.0F, turgor)) : 0.0F;
        if (Float.compare(this.turgor, clamped) == 0) {
            return false;
        }
        this.turgor = clamped;
        setChanged();
        return true;
    }

    public float getWaveAmplitude() {
        return waveAmplitude;
    }

    public void setWaveAmplitude(float waveAmplitude) {
        this.waveAmplitude = Float.isFinite(waveAmplitude)
                ? Math.max(0.0F, Math.min(1.0F, waveAmplitude))
                : 0.0F;
    }

    public long getLastWaveTick() {
        return lastWaveTick;
    }

    public boolean triggerWave(float amplitude, long gameTime) {
        if (!Float.isFinite(amplitude) || amplitude < HyphalPhysics.WAVE_MIN_AMPLITUDE) {
            return false;
        }
        if ((gameTime - lastWaveTick) < HyphalPhysics.WAVE_COOLDOWN_TICKS && amplitude <= waveAmplitude) {
            return false;
        }
        this.waveAmplitude = Math.min(1.0F, Math.max(this.waveAmplitude, amplitude));
        this.lastWaveTick = gameTime;
        return true;
    }

    public boolean canTriggerStepWave(long gameTime) {
        if ((gameTime - lastEntityStepWaveTick) < 10L) {
            return false;
        }
        lastEntityStepWaveTick = gameTime;
        return true;
    }

    public void syncToClient() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(DIAGONALS_PROPERTY, diagonals)
                .with(FACES_PROPERTY, HyphalPhysics.effectiveFaces(faces))
                .build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte(TAG_DIAGONALS, diagonals);
        tag.putByte(TAG_FACES, faces);
        tag.putInt(TAG_NUTRIENT, nutrient);
        tag.putInt(TAG_DISTANCE_TO_ROOT, distanceToRoot);
        tag.putBoolean(TAG_HAS_PARENT, hasParent);
        if (hasParent) {
            tag.putLong(TAG_PARENT_POS, parentPos);
        }
        tag.putInt(TAG_TRAIL_INDEX, trailIndex);
        tag.putBoolean(TAG_ROOT, root);
        tag.putInt(TAG_TRAVERSALS, traversals);
        tag.putFloat(TAG_TURGOR, turgor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        diagonals = (byte) (tag.getByte(TAG_DIAGONALS) & HyphalPhysics.ALL_DIAGONALS_MASK);
        faces = tag.contains(TAG_FACES)
                ? HyphalPhysics.effectiveFaces(tag.getByte(TAG_FACES))
                : HyphalPhysics.FACE_DOWN;
        nutrient = Math.max(0, Math.min(HyphalPhysics.MAX_NUTRIENT, tag.getInt(TAG_NUTRIENT)));
        distanceToRoot = Math.max(0, tag.getInt(TAG_DISTANCE_TO_ROOT));
        hasParent = tag.getBoolean(TAG_HAS_PARENT);
        parentPos = hasParent ? tag.getLong(TAG_PARENT_POS) : NO_PARENT;
        trailIndex = tag.contains(TAG_TRAIL_INDEX) ? tag.getInt(TAG_TRAIL_INDEX) : -1;
        root = tag.getBoolean(TAG_ROOT);
        traversals = Math.max(0, tag.getInt(TAG_TRAVERSALS));
        turgor = tag.contains(TAG_TURGOR)
                ? Math.max(0.0F, Math.min(1.0F, tag.getFloat(TAG_TURGOR)))
                : 0.0F;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket pkt,
            HolderLookup.Provider lookupProvider
    ) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }
}
