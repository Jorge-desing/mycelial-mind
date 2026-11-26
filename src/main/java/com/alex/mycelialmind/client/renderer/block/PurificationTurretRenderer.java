package com.alex.mycelialmind.client.renderer.block;

import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.entity.MycelialMob;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Renders animated target-tracking turret optics and Tier 2/3 orbital stabilizers on Purification Turrets.
 */
public class PurificationTurretRenderer implements BlockEntityRenderer<PurificationTurretBlockEntity> {
    private final ModelPart turretParts;
    private final ModelPart crystalParts;

    public PurificationTurretRenderer(BlockEntityRendererProvider.Context ctx) {
        this.turretParts = ctx.bakeLayer(MonumentRenderParts.TURRET_HEAD_LAYER);
        this.crystalParts = ctx.bakeLayer(MonumentRenderParts.CRYSTAL_LAYER);
    }

    @Override
    public void render(
            PurificationTurretBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (be.getLevel() == null) {
            return;
        }
        float time = (be.getLevel().getGameTime() + partialTick);
        long gameTime = be.getLevel().getGameTime();
        if (be.clientAimTargetUpdateTick == Long.MIN_VALUE
                || gameTime - be.clientAimTargetUpdateTick >= 5L) {
            be.clientAimTargetUpdateTick = gameTime;
            boolean impaired = be.isJammed() || !be.hasAmmo();
            float targetYaw = time * (impaired ? 0.25F : 1.4F);
            double rangeRadius = be.getRangeRadius();
            double rangeSq = rangeRadius * rangeRadius;
            List<? extends Player> nearbyPlayers = be.getLevel().players().stream()
                    .filter(player -> !player.isSpectator()
                            && player.blockPosition().distSqr(be.getBlockPos()) <= rangeSq)
                    .toList();
            List<MycelialMob> mobs = impaired ? List.of() : be.getLevel().getEntitiesOfClass(
                    MycelialMob.class,
                    new AABB(be.getBlockPos()).inflate(rangeRadius),
                    MycelialMob::isAlive);
            if (!mobs.isEmpty()) {
                mobs.removeIf(mob -> !PurificationTurretBlockEntity.isEligibleTarget(mob, nearbyPlayers));
            }
            MycelialMob target = PurificationTurretBlockEntity.pickPriorityTarget(be.getBlockPos(), mobs);
            if (target != null) {
                double dx = target.getX() - (be.getBlockPos().getX() + 0.5);
                double dz = target.getZ() - (be.getBlockPos().getZ() + 0.5);
                targetYaw = (float) (-Math.toDegrees(Math.atan2(dx, dz)));
            }
            be.clientAimTargetYaw = targetYaw;
        }
        int tier = be.getTier();
        var visual = com.alex.mycelialmind.hive.UxJuiceEngine.computeTurretVisualState(
                be.getAmmo(),
                com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_MAX_AMMO,
                be.isJammed(),
                tier);
        be.clientAimYaw = Mth.rotLerp(visual.aimLerpSpeed(), be.clientAimYaw, be.clientAimTargetYaw);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(MonumentRenderParts.ATLAS_PURIFICATION));
        int tint = visual.tintArgb();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.72D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(be.clientAimYaw));
        turretParts.getChild("optic_crest").render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
        if (tier >= 2) {
            turretParts.getChild("tier_wing_r").render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
            turretParts.getChild("tier_wing_l").render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PurificationTurretBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0D, 1.5D, 1.0D);
    }
}
