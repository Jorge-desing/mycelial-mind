package com.alex.mycelialmind.client.renderer;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.HyphalFilamentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Renders {@link HyphalFilamentEntity} Verlet rope chains as crossed 3D organic ribbons
 * colored by internal turgor and tension.
 */
public class HyphalFilamentRenderer extends EntityRenderer<HyphalFilamentEntity> {
    private static final ResourceLocation FILAMENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/block/hyphal_vein_line.png");

    public HyphalFilamentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(HyphalFilamentEntity entity) {
        return FILAMENT_TEXTURE;
    }

    @Override
    public boolean shouldRender(
            HyphalFilamentEntity entity,
            Frustum camera,
            double camX,
            double camY,
            double camZ
    ) {
        AABB spanBox = new AABB(
                Vec3.atCenterOf(entity.getAnchorA()),
                Vec3.atCenterOf(entity.getAnchorB())
        ).inflate(1.5D);
        return camera.isVisible(spanBox);
    }

    @Override
    public void render(
            HyphalFilamentEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Vec3[] nodes = entity.getNodePositions();
        if (nodes.length < 2) {
            return;
        }
        Vec3 origin = entity.getPosition(partialTick);
        float turgor = entity.getTurgor();
        float tension = Math.max(0.0F, Math.min(1.0F, (entity.getTensionRatio() - 1.0F) / 0.65F));
        float halfWidth = 0.03F + turgor * 0.025F;

        int red = (int) (90 + tension * 120);
        int green = (int) (35 + turgor * 70);
        int blue = (int) (130 + turgor * 80);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < nodes.length - 1; i++) {
            Vec3 p0 = nodes[i].subtract(origin);
            Vec3 p1 = nodes[i + 1].subtract(origin);
            Vec3 dir = p1.subtract(p0);
            if (dir.lengthSqr() < 1.0E-6D) {
                continue;
            }
            Vec3 sideA = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (sideA.lengthSqr() < 1.0E-5D) {
                sideA = dir.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
            sideA = sideA.normalize().scale(halfWidth);
            Vec3 sideB = dir.cross(sideA).normalize().scale(halfWidth);

            emitRibbonQuad(vc, pose, p0, p1, sideA, red, green, blue, packedLight);
            emitRibbonQuad(vc, pose, p0, p1, sideB, red, green, blue, packedLight);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void emitRibbonQuad(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 p0,
            Vec3 p1,
            Vec3 offset,
            int r,
            int g,
            int b,
            int packedLight
    ) {
        vc.addVertex(pose, (float) (p0.x - offset.x), (float) (p0.y - offset.y), (float) (p0.z - offset.z))
                .setColor(r, g, b, 255)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, (float) (p0.x + offset.x), (float) (p0.y + offset.y), (float) (p0.z + offset.z))
                .setColor(r, g, b, 255)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, (float) (p1.x + offset.x), (float) (p1.y + offset.y), (float) (p1.z + offset.z))
                .setColor(r, g, b, 255)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, (float) (p1.x - offset.x), (float) (p1.y - offset.y), (float) (p1.z - offset.z))
                .setColor(r, g, b, 255)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
