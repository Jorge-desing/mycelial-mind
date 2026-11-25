package com.alex.mycelialmind.client.renderer;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.HyphalVeinEntity;
import com.alex.mycelialmind.hive.HyphalVerletRope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Translucent 3D Verlet strand renderer for {@link HyphalVeinEntity}.
 * Renders smooth partialTick-interpolated crossed ribbons with continuous joint tangents,
 * per-segment block light sampling, emissive seismic wave pulses, length-scaled opacity/color,
 * dry ash desaturation when withering, and a subtle root knot marker.
 */
public class HyphalVeinRenderer extends EntityRenderer<HyphalVeinEntity> {
    private static final ResourceLocation VEIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/block/hyphal_vein_line.png");
    private static final ResourceLocation ROOT_DOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/block/hyphal_vein_dot.png");

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
        Vec3[] nodes = entity.getInterpolatedNodePositions(partialTick);
        int n = nodes.length;
        if (n < 2) {
            return;
        }
        Vec3 origin = entity.getPosition(partialTick);
        double totalLen = HyphalVerletRope.computeTotalLength(nodes);
        HyphalVerletRope.StrandRgba baseRgba = HyphalVerletRope.computeColorAndAlphaForLength(n, totalLen);
        float witherProgress = entity.getWitherProgress();
        HyphalVerletRope.StrandRgba tintedBase = HyphalVerletRope.applyWitherTint(baseRgba, witherProgress);

        float turgor = entity.getTurgor();
        float wavePos = entity.getWavePos();
        float waveAmp = entity.getWaveAmp();

        // Precompute smooth tangents, twist-free ribbon axes, and root-to-tip width taper per node
        Vec3[] unitA = new Vec3[n];
        Vec3[] unitB = new Vec3[n];
        float[] halfWidths = new float[n];
        Vec3 prevTangent = new Vec3(1.0D, 0.0D, 0.0D);

        for (int i = 0; i < n; i++) {
            Vec3 tan;
            if (i == 0) {
                tan = nodes[1].subtract(nodes[0]);
            } else if (i == n - 1) {
                tan = nodes[n - 1].subtract(nodes[n - 2]);
            } else {
                tan = nodes[i + 1].subtract(nodes[i - 1]);
            }
            if (tan.lengthSqr() < 1.0E-6D) {
                tan = prevTangent;
            } else {
                tan = tan.normalize();
                prevTangent = tan;
            }

            Vec3 refUp = Math.abs(tan.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 sA = tan.cross(refUp);
            if (sA.lengthSqr() < 1.0E-6D) {
                sA = tan.cross(new Vec3(0.0D, 0.0D, 1.0D));
            }
            sA = sA.normalize();
            if (i > 0 && sA.dot(unitA[i - 1]) < 0.0D) {
                sA = sA.scale(-1.0D);
            }
            Vec3 sB = tan.cross(sA).normalize();
            if (i > 0 && sB.dot(unitB[i - 1]) < 0.0D) {
                sB = sB.scale(-1.0D);
            }
            unitA[i] = sA;
            unitB[i] = sB;

            float nodeProgress = i / (float) Math.max(1, n - 1);
            halfWidths[i] = Mth.lerp(nodeProgress, 0.055F, 0.024F) + turgor * 0.018F;
        }

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();

        float accV = 0.0F;
        for (int i = 0; i < n - 1; i++) {
            Vec3 p0 = nodes[i].subtract(origin);
            Vec3 p1 = nodes[i + 1].subtract(origin);
            Vec3 dir = p1.subtract(p0);
            double segLenSq = dir.lengthSqr();
            if (segLenSq < 1.0E-6D) {
                continue;
            }
            double segLen = Math.sqrt(segLenSq);
            float v0 = accV;
            accV += (float) (segLen * 1.5D);
            float v1 = accV;

            // Luminous wave pulse along the strand
            float pulse = 0.0F;
            if (wavePos >= 0.0F && waveAmp > 0.05F) {
                float distToWave = Math.abs((i + 0.5F) - wavePos);
                if (distToWave < 2.2F) {
                    pulse = (1.0F - distToWave / 2.2F) * waveAmp;
                }
            }

            int r = Math.min(255, Math.round(tintedBase.r() + pulse * 175.0F));
            int g = Math.min(255, Math.round(tintedBase.g() + pulse * 95.0F));
            int b = Math.min(255, Math.round(tintedBase.b() + pulse * 195.0F));
            int a = Math.max(24, Math.min(255, Math.round(tintedBase.a() + pulse * 75.0F)));

            Vec3 midWorld = nodes[i].add(nodes[i + 1]).scale(0.5D);
            int segLight = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(midWorld));
            if (pulse > 0.15F) {
                int blk = Math.max(LightTexture.block(segLight), Math.min(15, Math.round(pulse * 15.0F)));
                int sky = Math.max(LightTexture.sky(segLight), Math.min(15, Math.round(pulse * 12.0F)));
                segLight = LightTexture.pack(blk, sky);
            }

            Vec3 off0A = unitA[i].scale(halfWidths[i]);
            Vec3 off1A = unitA[i + 1].scale(halfWidths[i + 1]);
            Vec3 off0B = unitB[i].scale(halfWidths[i]);
            Vec3 off1B = unitB[i + 1].scale(halfWidths[i + 1]);

            Vec3 normA = unitB[i];
            Vec3 normB = unitA[i];
            emitRibbonQuad(vc, pose, p0, p1, off0A, off1A, normA, v0, v1, r, g, b, a, segLight);
            emitRibbonQuad(vc, pose, p0, p1, off0B, off1B, normB, v0, v1, r, g, b, a, segLight);
        }

        // Subtle root knot marker on node 0 when strand is connected to a live Hive root
        if (entity.hasRoot() && witherProgress < 0.85F) {
            Vec3 rootLocal = nodes[0].subtract(origin);
            float knotRadius = halfWidths[0] * 1.45F;
            int knotLight = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(nodes[0]));
            int ka = Math.max(40, Math.round(tintedBase.a() * 0.92F));
            int kr = Math.min(255, tintedBase.r() + 45);
            int kg = Math.min(255, tintedBase.g() + 18);
            int kb = Math.min(255, tintedBase.b() + 65);

            VertexConsumer dotVc = bufferSource.getBuffer(RenderType.entityTranslucent(ROOT_DOT_TEXTURE));
            Vec3 kx = unitA[0].scale(knotRadius);
            Vec3 ky = unitB[0].scale(knotRadius);
            Vec3 kz = nodes[1].subtract(nodes[0]);
            kz = kz.lengthSqr() > 1.0E-6D ? kz.normalize().scale(knotRadius) : new Vec3(0.0D, 0.0D, knotRadius);

            emitRibbonQuad(dotVc, pose, rootLocal.subtract(kz), rootLocal.add(kz),
                    kx, kx, unitB[0], 0.0F, 1.0F, kr, kg, kb, ka, knotLight);
            emitRibbonQuad(dotVc, pose, rootLocal.subtract(kz), rootLocal.add(kz),
                    ky, ky, unitA[0], 0.0F, 1.0F, kr, kg, kb, ka, knotLight);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void emitRibbonQuad(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 p0,
            Vec3 p1,
            Vec3 offset0,
            Vec3 offset1,
            Vec3 normal,
            float v0,
            float v1,
            int r,
            int g,
            int b,
            int a,
            int packedLight
    ) {
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;

        vc.addVertex(pose, (float) (p0.x - offset0.x), (float) (p0.y - offset0.y), (float) (p0.z - offset0.z))
                .setColor(r, g, b, a)
                .setUv(0.0F, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) (p0.x + offset0.x), (float) (p0.y + offset0.y), (float) (p0.z + offset0.z))
                .setColor(r, g, b, a)
                .setUv(1.0F, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) (p1.x + offset1.x), (float) (p1.y + offset1.y), (float) (p1.z + offset1.z))
                .setColor(r, g, b, a)
                .setUv(1.0F, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) (p1.x - offset1.x), (float) (p1.y - offset1.y), (float) (p1.z - offset1.z))
                .setColor(r, g, b, a)
                .setUv(0.0F, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }
}
