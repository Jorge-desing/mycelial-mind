package com.alex.mycelialmind.client.renderer.block;

import com.alex.mycelialmind.block.HiveAltarBlock;
import com.alex.mycelialmind.block.MatriarchAltarBlock;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

/**
 * Renders levitating animated crystals and ritual sigil rings above Purification Pylons and Boss Altars.
 */
public class MonumentFocusRenderer implements BlockEntityRenderer<MonumentFocusBlockEntity> {
    private final ModelPart crystalParts;

    public MonumentFocusRenderer(BlockEntityRendererProvider.Context ctx) {
        this.crystalParts = ctx.bakeLayer(MonumentRenderParts.CRYSTAL_LAYER);
    }

    @Override
    public void render(
            MonumentFocusBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (be.getLevel() == null) {
            return;
        }
        float time = (be.getLevel().getGameTime() + partialTick) + (be.getBlockPos().hashCode() & 31);
        Block block = be.getBlockState().getBlock();
        if (block instanceof HiveAltarBlock || block instanceof MatriarchAltarBlock) {
            // Altars remain grounded in their distinct monolithic 3D geometry without floating crystals.
            return;
        }

        // Purification Pylon: subtle small rotating prism inside the tripod frame (no outer rings)
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(MonumentRenderParts.ATLAS_PURIFICATION));
        ModelPart crystal = crystalParts.getChild("crystal");
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.56D + Math.sin(time * 0.06F) * 0.025D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.6F));
        poseStack.scale(0.38F, 0.42F, 0.38F);
        crystal.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color(235, 130, 245, 255));
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MonumentFocusBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D, 1.5D, 1.0D);
    }
}
