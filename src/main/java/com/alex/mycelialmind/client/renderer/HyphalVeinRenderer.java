package com.alex.mycelialmind.client.renderer;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.HyphalVeinEntity;
import com.alex.mycelialmind.hive.HyphalVerletRope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Translucent 3D Verlet strand renderer for {@link HyphalVeinEntity}.
 * Scales opacity and color intensity by strand length (short hyphae are nearly transparent,
 * long mature hyphae deepen to fully opaque dark obsidian-violet), tapers width from root to tip,
 * and illuminates nodes carrying active mechanical/seismic waves.
 */
public class HyphalVeinRenderer extends EntityRenderer<HyphalVeinEntity> {
    private static final ResourceLocation VEIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/block/hyphal_vein_line.png");

    public HyphalVeinRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(HyphalVeinEntity entity) {
        return VEIN_TEXTURE;
    }

    @Override
    public boolean shouldRender(
            HyphalVeinEntity entity,
            Frustum camera,
            double camX,
            double camY,
            double camZ
    ) {
        return camera.isVisible(entity.getStrandBoundingBox().inflate(1.5D));
    }

    @Override
    public void render(
            HyphalVeinEntity entity,
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
        double totalLen = HyphalVerletRope.computeTotalLength(nodes);
        HyphalVerletRope.StrandRgba baseRgba = HyphalVerletRope.computeColorAndAlphaForLength(nodes.length, totalLen);

        float turgor = entity.getTurgor();
        float witherFade = 1.0F - Mth.clamp(entity.getWitherProgress(), 0.0F, 0.90F);
        float wavePos = entity.getWavePos();
        float waveAmp = entity.getWaveAmp();

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < nodes.length - 1; i++) {
            Vec3 p0 = nodes[i].subtract(origin);
            Vec3 p1 = nodes[i + 1].subtract(origin);
            Vec3 dir = p1.subtract(p0);
            if (dir.lengthSqr() < 1.0E-6D) {
                continue;
            }
            float progress = i / (float) Math.max(1, nodes.length - 1);
            // Taper thickness from root (thicker) to tip (finer), boosted slightly by turgor
            float halfWidth = Mth.lerp(progress, 0.055F, 0.025F) + turgor * 0.018F;

            // Luminous pulse when seismic wave passes over this node segment
            float pulse = 0.0F;
            if (wavePos >= 0.0F && waveAmp > 0.05F) {
                float distToWave = Math.abs((i + 0.5F) - wavePos);
                if (distToWave < 2.2F) {
                    pulse = (1.0F - distToWave / 2.2F) * waveAmp;
                }
            }

            int r = Math.min(255, Math.round(baseRgba.r() + pulse * 170.0F));
            int g = Math.min(255, Math.round(baseRgba.g() + pulse * 95.0F));
            int b = Math.min(255, Math.round(baseRgba.b() + pulse * 190.0F));
            int a = Math.max(24, Math.min(255, Math.round(baseRgba.a() * witherFade + pulse * 70.0F)));

            Vec3 sideA = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (sideA.lengthSqr() < 1.0E-5D) {
                sideA = dir.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
            sideA = sideA.normalize().scale(halfWidth);
            Vec3 sideB = dir.cross(sideA).normalize().scale(halfWidth);

            emitRibbonQuad(vc, pose, p0, p1, sideA, r, g, b, a, packedLight);
            emitRibbonQuad(vc, pose, p0, p1, sideB, r, g, b, a, packedLight);
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
            int a,
            int packedLight
    ) {
        vc.addVertex(pose, (float) (p0.x - offset.x), (float) (p0.y - offset.y), (float) (p0.z - offset.z))
                .setColor(r, g, b, a)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, (float) (p0.x + offset.x), (float) (p0.y + offset.y), (float) (p0.z + offset.z))
                .setColor(r, g, b, a)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, (float) (p1.x + offset.x), (float) (p1.y + offset.y), (float) (p1.z + offset.z))
                .setColor(r, g, b, a)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        vc.addVertex(pose, (float) (p1.x - offset.x), (float) (p1.y - offset.y), (float) (p1.z - offset.z))
                .setColor(r, g, b, a)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
