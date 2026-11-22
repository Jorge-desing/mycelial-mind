package com.alex.mycelialmind.client.renderer.block;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.world.SynapticNodeType;
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
import net.minecraft.world.phys.AABB;

/**
 * Renders a subtle inner pulsing core inside Symbiotic Nodes while keeping each node's grounded 3D silhouette distinct.
 */
public class SynapticNodeRenderer implements BlockEntityRenderer<SynapticNodeBlockEntity> {
    private final ModelPart crystalParts;

    public SynapticNodeRenderer(BlockEntityRendererProvider.Context ctx) {
        this.crystalParts = ctx.bakeLayer(MonumentRenderParts.CRYSTAL_LAYER);
    }

    @Override
    public void render(
            SynapticNodeBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (be.getLevel() == null || be.getNodeType() != SynapticNodeType.SYMBIOTIC) {
            return;
        }
        float time = (be.getLevel().getGameTime() + partialTick) + (be.getBlockPos().hashCode() & 63);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(MonumentRenderParts.ATLAS_HIVE));
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.50D + Math.sin(time * 0.08F) * 0.02D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
        poseStack.scale(0.28F, 0.28F, 0.28F);
        crystalParts.getChild("crystal").render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color(230, 120, 255, 175));
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SynapticNodeBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D, 1.5D, 1.0D);
    }
}
