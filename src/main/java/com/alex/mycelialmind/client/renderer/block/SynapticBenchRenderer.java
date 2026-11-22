package com.alex.mycelialmind.client.renderer.block;

import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
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
 * Renders a small rotating sample vial on the Synaptic Research Bench tabletop.
 */
public class SynapticBenchRenderer implements BlockEntityRenderer<SynapticBenchBlockEntity> {
    private final ModelPart crystalParts;

    public SynapticBenchRenderer(BlockEntityRendererProvider.Context ctx) {
        this.crystalParts = ctx.bakeLayer(MonumentRenderParts.CRYSTAL_LAYER);
    }

    @Override
    public void render(
            SynapticBenchBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (be.getLevel() == null) {
            return;
        }
        float time = be.getLevel().getGameTime() + partialTick;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(MonumentRenderParts.ATLAS_PURIFICATION));
        poseStack.pushPose();
        poseStack.translate(0.25D, 0.96D + Math.sin(time * 0.07F) * 0.015D, 0.72D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.8F));
        poseStack.scale(0.26F, 0.30F, 0.26F);
        crystalParts.getChild("crystal").render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color(230, 120, 255, 220));
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SynapticBenchBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D, 1.5D, 1.0D);
    }
}
