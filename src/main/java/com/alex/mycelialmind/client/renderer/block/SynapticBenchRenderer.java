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
        boolean hasOutput = !be.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_OUTPUT).isEmpty();
        var visual = com.alex.mycelialmind.hive.UxJuiceEngine.computeBenchVisualState(
                be.getEffectiveLabTier(), hasOutput, false);
        float time = be.getLevel().getGameTime() + partialTick;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(MonumentRenderParts.ATLAS_PURIFICATION));
        poseStack.pushPose();
        poseStack.translate(0.25D, 0.96D + Math.sin(time * 0.07F) * visual.bobAmplitude(), 0.72D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * visual.speedMultiplier()));
        poseStack.scale(0.26F, 0.30F, 0.26F);
        crystalParts.getChild("crystal").render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                visual.tintArgb());
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SynapticBenchBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D, 1.5D, 1.0D);
    }
}
