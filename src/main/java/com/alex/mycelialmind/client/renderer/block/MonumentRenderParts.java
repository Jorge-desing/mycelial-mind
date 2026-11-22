package com.alex.mycelialmind.client.renderer.block;

import com.alex.mycelialmind.MycelialMind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * Shared procedural ModelPart meshes and fullbright rendering helpers for 3D BlockEntityRenderers.
 */
public final class MonumentRenderParts {
    public static final ModelLayerLocation CRYSTAL_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "monument_crystal"), "main");
    public static final ModelLayerLocation TURRET_HEAD_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "purification_turret_head"), "main");

    public static final ResourceLocation ATLAS_HIVE = ResourceLocation.fromNamespaceAndPath(
            MycelialMind.MODID, "textures/model_atlas/atlas_blocks_hive.png");
    public static final ResourceLocation ATLAS_PURIFICATION = ResourceLocation.fromNamespaceAndPath(
            MycelialMind.MODID, "textures/model_atlas/atlas_blocks_purification.png");
    public static final ResourceLocation ATLAS_ALTARS = ResourceLocation.fromNamespaceAndPath(
            MycelialMind.MODID, "textures/model_atlas/atlas_blocks_altars.png");

    private MonumentRenderParts() {
    }

    public static LayerDefinition createCrystalLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("crystal",
                CubeListBuilder.create()
                        .texOffs(48, 0).addBox(-2.0F, -3.5F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(16, 48).addBox(-1.2F, -5.0F, -1.2F, 2.4F, 10.0F, 2.4F),
                PartPose.ZERO);
        root.addOrReplaceChild("ring_inner",
                CubeListBuilder.create()
                        .texOffs(32, 32).addBox(-5.0F, -0.5F, -5.0F, 10.0F, 1.0F, 1.0F)
                        .texOffs(32, 32).addBox(-5.0F, -0.5F, 4.0F, 10.0F, 1.0F, 1.0F)
                        .texOffs(32, 32).addBox(-5.0F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F)
                        .texOffs(32, 32).addBox(4.0F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("ring_outer",
                CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-7.0F, -0.5F, -7.0F, 14.0F, 1.0F, 1.0F)
                        .texOffs(0, 48).addBox(-7.0F, -0.5F, 6.0F, 14.0F, 1.0F, 1.0F)
                        .texOffs(0, 48).addBox(-7.0F, -0.5F, -6.0F, 1.0F, 1.0F, 12.0F)
                        .texOffs(0, 48).addBox(6.0F, -0.5F, -6.0F, 1.0F, 1.0F, 12.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createTurretHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("optic_crest",
                CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-2.0F, -1.5F, -3.5F, 4.0F, 2.5F, 5.0F)
                        .texOffs(48, 0).addBox(-1.0F, -2.5F, -4.5F, 2.0F, 2.0F, 2.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("tier_wing_r",
                CubeListBuilder.create()
                        .texOffs(48, 32).addBox(-5.5F, -2.0F, -1.5F, 1.5F, 4.0F, 3.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("tier_wing_l",
                CubeListBuilder.create()
                        .texOffs(48, 32).mirror().addBox(4.0F, -2.0F, -1.5F, 1.5F, 4.0F, 3.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static void renderCrystalAndRings(
            ModelPart rootPart,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ResourceLocation texture,
            float time,
            double centerY,
            float crystalScale,
            int argbTint,
            boolean showOuterRing
    ) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        ModelPart crystal = rootPart.getChild("crystal");
        ModelPart ringInner = rootPart.getChild("ring_inner");
        ModelPart ringOuter = rootPart.getChild("ring_outer");

        float bob = (float) Math.sin(time * 0.08F) * 0.05F;

        poseStack.pushPose();
        poseStack.translate(0.5D, centerY + bob, 0.5D);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
        poseStack.mulPose(Axis.XP.rotationDegrees(15.0F * (float) Math.sin(time * 0.05F)));
        poseStack.scale(crystalScale, crystalScale, crystalScale);
        crystal.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argbTint);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 3.1F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(26.0F));
        poseStack.scale(crystalScale, crystalScale, crystalScale);
        ringInner.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argbTint);
        poseStack.popPose();

        if (showOuterRing) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.9F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
            poseStack.scale(crystalScale, crystalScale, crystalScale);
            ringOuter.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    FastColor.ARGB32.color(220, 255, 220, 140));
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
