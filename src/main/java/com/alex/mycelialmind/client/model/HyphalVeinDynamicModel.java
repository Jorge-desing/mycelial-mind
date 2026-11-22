package com.alex.mycelialmind.client.model;

import com.alex.mycelialmind.block.entity.HyphalVeinBlockEntity;
import com.alex.mycelialmind.hive.HyphalPhysics;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic baked model wrapper for Hyphal Vein blocks that injects diagonal filament quads
 * (NE/NW/SE/SW) and wall/ceiling attachment face quads from BlockEntity {@link ModelData}
 * while preserving the existing cardinal blockstate variants and tintindex=0 coloration.
 */
public final class HyphalVeinDynamicModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel {
    private final List<BakedQuad> diagNE;
    private final List<BakedQuad> diagSE;
    private final List<BakedQuad> diagSW;
    private final List<BakedQuad> diagNW;
    private final List<BakedQuad> wallNorth;
    private final List<BakedQuad> wallEast;
    private final List<BakedQuad> wallSouth;
    private final List<BakedQuad> wallWest;
    private final List<BakedQuad> ceilingQuads;

    public HyphalVeinDynamicModel(
            BakedModel originalModel,
            @Nullable BakedModel diagBaseModel,
            @Nullable BakedModel wallBaseModel,
            @Nullable BakedModel ceilingBaseModel
    ) {
        super(originalModel);
        List<BakedQuad> rawDiag = extractUnCulledQuads(diagBaseModel);
        List<BakedQuad> rawWall = extractUnCulledQuads(wallBaseModel);
        this.diagNE = rawDiag;
        this.diagSE = rotateQuadsY(rawDiag, 1);
        this.diagSW = rotateQuadsY(rawDiag, 2);
        this.diagNW = rotateQuadsY(rawDiag, 3);
        this.wallNorth = rawWall;
        this.wallEast = rotateQuadsY(rawWall, 1);
        this.wallSouth = rotateQuadsY(rawWall, 2);
        this.wallWest = rotateQuadsY(rawWall, 3);
        this.ceilingQuads = extractUnCulledQuads(ceilingBaseModel);
    }

    private static List<BakedQuad> extractUnCulledQuads(@Nullable BakedModel model) {
        if (model == null) {
            return List.of();
        }
        RandomSource rand = RandomSource.create(0L);
        List<BakedQuad> out = new ArrayList<>();
        out.addAll(model.getQuads(null, null, rand, ModelData.EMPTY, null));
        for (Direction dir : Direction.values()) {
            out.addAll(model.getQuads(null, dir, rand, ModelData.EMPTY, null));
        }
        return List.copyOf(out);
    }

    private static List<BakedQuad> rotateQuadsY(List<BakedQuad> source, int quarterTurnsClockwise) {
        if (source.isEmpty() || (quarterTurnsClockwise & 3) == 0) {
            return source;
        }
        float radians = (float) Math.toRadians(-90.0D * (quarterTurnsClockwise & 3));
        Matrix4f mat = new Matrix4f()
                .translate(0.5F, 0.5F, 0.5F)
                .rotateY(radians)
                .translate(-0.5F, -0.5F, -0.5F);
        IQuadTransformer transformer = QuadTransformers.applying(new Transformation(mat));
        return List.copyOf(transformer.process(source));
    }

    @Override
    public ModelData getModelData(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            ModelData modelData
    ) {
        if (modelData == ModelData.EMPTY && level.getBlockEntity(pos) instanceof HyphalVeinBlockEntity be) {
            return be.getModelData();
        }
        return modelData;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData extraData,
            @Nullable RenderType renderType
    ) {
        byte faces = extraData.has(HyphalVeinBlockEntity.FACES_PROPERTY)
                ? extraData.get(HyphalVeinBlockEntity.FACES_PROPERTY)
                : HyphalPhysics.FACE_DOWN;
        byte diagonals = extraData.has(HyphalVeinBlockEntity.DIAGONALS_PROPERTY)
                ? extraData.get(HyphalVeinBlockEntity.DIAGONALS_PROPERTY)
                : 0;

        boolean hasFloor = (faces & HyphalPhysics.FACE_DOWN) != 0;
        if (side != null) {
            return hasFloor ? originalModel.getQuads(state, side, rand, extraData, renderType) : List.of();
        }

        boolean hasExtra = diagonals != 0 || (faces & ~HyphalPhysics.FACE_DOWN) != 0;
        if (!hasExtra) {
            return hasFloor ? originalModel.getQuads(state, null, rand, extraData, renderType) : List.of();
        }

        List<BakedQuad> out = new ArrayList<>();
        if (hasFloor) {
            out.addAll(originalModel.getQuads(state, null, rand, extraData, renderType));
        }
        if ((diagonals & HyphalPhysics.DIAG_NE) != 0) {
            out.addAll(diagNE);
        }
        if ((diagonals & HyphalPhysics.DIAG_SE) != 0) {
            out.addAll(diagSE);
        }
        if ((diagonals & HyphalPhysics.DIAG_SW) != 0) {
            out.addAll(diagSW);
        }
        if ((diagonals & HyphalPhysics.DIAG_NW) != 0) {
            out.addAll(diagNW);
        }
        if ((faces & HyphalPhysics.FACE_NORTH) != 0) {
            out.addAll(wallNorth);
        }
        if ((faces & HyphalPhysics.FACE_EAST) != 0) {
            out.addAll(wallEast);
        }
        if ((faces & HyphalPhysics.FACE_SOUTH) != 0) {
            out.addAll(wallSouth);
        }
        if ((faces & HyphalPhysics.FACE_WEST) != 0) {
            out.addAll(wallWest);
        }
        if ((faces & HyphalPhysics.FACE_UP) != 0) {
            out.addAll(ceilingQuads);
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return originalModel.getRenderTypes(state, rand, data);
    }
}
