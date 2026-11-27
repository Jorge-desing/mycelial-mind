package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.hive.AcquisitionDifficultyEngine;
import com.alex.mycelialmind.hive.DamageTelemetryTracker;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Shared base BlockEntity for tiered progression stations (Turret, Pylon/Monument, Bench)
 * providing Act-gated upgrade checks and client synchronization.
 */
public abstract class TieredStationBlockEntity extends SyncingBlockEntity {
    protected TieredStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected boolean canPassTierUpgradeGate(int nextTier, @Nullable Player player, boolean notifyOnFail) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return !notifyOnFail;
        }
        HiveProgressionTier effectiveAct = StructureProgressionPolicy.resolveTier(serverLevel);
        boolean defeated = DamageTelemetryTracker.isHiveCoreDefeated(serverLevel, player);
        boolean creative = isCreativeOrInstabuild(player);
        if (AcquisitionDifficultyEngine.canUpgradeToTier(nextTier, effectiveAct, defeated, creative)) {
            return true;
        }
        if (notifyOnFail && player != null) {
            HiveProgressionTier reqAct = AcquisitionDifficultyEngine.requiredActForTier(nextTier);
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.gate_requires_act", reqAct.name(), nextTier)
                            .withStyle(ChatFormatting.YELLOW),
                    true);
        }
        return false;
    }

    protected static boolean isCreativeOrInstabuild(@Nullable Player player) {
        return player != null && (player.isCreative() || player.getAbilities().instabuild);
    }
}
