package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionData;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.world.ModDimensions;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server validates the selected enclave before changing its name or a player's destination. */
public record EnclaveActionPayload(int action, BlockPos pos, String name, int page) implements CustomPacketPayload {
    public static final int PIN = 0;
    public static final int RENAME = 1;
    public static final int ABANDON = 2;
    public static final Type<EnclaveActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "enclave_action"));
    public static final StreamCodec<ByteBuf, EnclaveActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnclaveActionPayload::action, BlockPos.STREAM_CODEC, EnclaveActionPayload::pos,
            ByteBufCodecs.STRING_UTF8, EnclaveActionPayload::name, ByteBufCodecs.VAR_INT, EnclaveActionPayload::page,
            EnclaveActionPayload::new);
    public static void send(int action, BlockPos pos, String name, int page) {
        PacketDistributor.sendToServer(new EnclaveActionPayload(action, pos, name, page));
    }
    public static void handleOnServer(EnclaveActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.server.getLevel(ModDimensions.DEEP_MYCELIUM_LEVEL);
            if (level == null) return;
            PostMatriarchExpeditionData data = PostMatriarchExpeditionData.get(level);
            if (payload.action() == ABANDON) {
                BlockPos target = data.getActivePos();
                if (target == null || !target.equals(payload.pos())) return;
                if (!level.isLoaded(target)) {
                    player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_abandon_unloaded"), false);
                } else if (PostMatriarchExpeditionDirector.isExpeditionNode(level.getBlockState(target), data.getActiveOutcome())) {
                    player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_abandon_node_present"), false);
                } else {
                    var attackers = data.getPulseAttackers();
                    if (!data.abandon(target)) return;
                    PostMatriarchExpeditionDirector.discardRetiredLoadedAttackers(level, attackers);
                    player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_abandoned"));
                    PostMatriarchExpeditionSyncPayload.syncToAll(level);
                }
                return;
            }
            if (!data.hasCompletedEnclave(payload.pos())) return;
            if (payload.action() == PIN) {
                var discovery = player.getData(ModAttachments.DISCOVERY);
                discovery.setPinnedEnclave(payload.pos().equals(discovery.getPinnedEnclave()) ? null : payload.pos());
                EnclaveDirectoryPayload.sendPage(player, payload.page());
            } else if (payload.action() == RENAME && data.renameEnclave(payload.pos(), payload.name())) {
                for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
                    if (viewer == player) EnclaveDirectoryPayload.sendPage(viewer, payload.page());
                    else EnclaveDirectoryPayload.refreshPage(viewer);
                }
            }
        });
    }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
