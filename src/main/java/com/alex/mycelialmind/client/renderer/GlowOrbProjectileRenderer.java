package com.alex.mycelialmind.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Billboard bioluminescent orb renderer for Mycelial Mind projectiles.
 */
public class GlowOrbProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    private final ResourceLocation texture;
    private final float scale;

    public GlowOrbProjectileRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        super(context);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.18F, 0.0F);
        poseStack.scale(this.scale, this.scale, this.scale);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(this.texture));
        vertex(consumer, pose, -0.5F, -0.5F, 0.0F, 1.0F);
        vertex(consumer, pose, 0.5F, -0.5F, 1.0F, 1.0F);
        vertex(consumer, pose, 0.5F, 0.5F, 1.0F, 0.0F);
        vertex(consumer, pose, -0.5F, 0.5F, 0.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
