package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server payload allowing each player to pin or switch their active expedition goal
 * ("Use to my advantage" vs "Reclaim this cave") in Ongoing Research and on the HUD.
 */
public record PinExpeditionGoalPayload(int goalId) implements CustomPacketPayload {
    public static final Type<PinExpeditionGoalPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "pin_expedition_goal"));

    public static final StreamCodec<ByteBuf, PinExpeditionGoalPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PinExpeditionGoalPayload::goalId,
            PinExpeditionGoalPayload::new
    );

    public static void sendToServer(ProgressionGoalEngine.ExpeditionGoal goal) {
        PacketDistributor.sendToServer(new PinExpeditionGoalPayload(goal != null ? goal.id() : 0));
    }

    public static void handleOnServer(PinExpeditionGoalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            applyGoalChange(serverPlayer, payload.goalId());
        });
    }

    public static void applyGoalChange(ServerPlayer serverPlayer, int goalId) {
        ProgressionGoalEngine.ExpeditionGoal goal = ProgressionGoalEngine.ExpeditionGoal.byId(goalId);
        PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
        if (discovery.setPinnedExpeditionGoal(goal.id())) {
            DiscoverySyncPayload.syncToPlayer(serverPlayer);
            if (goal == ProgressionGoalEngine.ExpeditionGoal.NONE) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.mycelialmind.expedition_goal_cleared")
                                .withStyle(ChatFormatting.GRAY),
                        true
                );
            } else {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.mycelialmind.expedition_goal_pinned",
                                Component.translatable(goal.translationKey()))
                                .withStyle(ChatFormatting.GOLD),
                        true
                );
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
