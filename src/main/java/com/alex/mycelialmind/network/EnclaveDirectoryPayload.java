package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionData;
import com.alex.mycelialmind.world.ModDimensions;
import com.alex.mycelialmind.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.ArrayList;
import java.util.List;

/** One bounded page of shared recovered enclaves. */
public record EnclaveDirectoryPayload(int page, int total, List<Entry> entries, BlockPos pinned) implements CustomPacketPayload {
    public static final int PAGE_SIZE = 6;
    private static final java.util.Map<ServerPlayer, Integer> VIEWER_PAGES =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    public static final Type<EnclaveDirectoryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "enclave_directory"));
    public record Entry(BlockPos pos, int outcome, int palette, String name) { }
    public static final StreamCodec<ByteBuf, EnclaveDirectoryPayload> STREAM_CODEC = new StreamCodec<>() {
        public EnclaveDirectoryPayload decode(ByteBuf buf) {
            int page = ByteBufCodecs.VAR_INT.decode(buf);
            int total = ByteBufCodecs.VAR_INT.decode(buf);
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            if (size < 0 || size > PAGE_SIZE) throw new io.netty.handler.codec.DecoderException("Invalid enclave page size");
            List<Entry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entries.add(new Entry(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));
            }
            BlockPos pinned = ByteBufCodecs.BOOL.decode(buf) ? BlockPos.STREAM_CODEC.decode(buf) : null;
            return new EnclaveDirectoryPayload(page, total, entries, pinned);
        }
        public void encode(ByteBuf buf, EnclaveDirectoryPayload payload) {
            if (payload.entries.size() > PAGE_SIZE) throw new io.netty.handler.codec.EncoderException("Invalid enclave page size");
            ByteBufCodecs.VAR_INT.encode(buf, payload.page);
            ByteBufCodecs.VAR_INT.encode(buf, payload.total);
            ByteBufCodecs.VAR_INT.encode(buf, payload.entries.size());
            for (Entry entry : payload.entries) {
                BlockPos.STREAM_CODEC.encode(buf, entry.pos());
                ByteBufCodecs.VAR_INT.encode(buf, entry.outcome());
                ByteBufCodecs.VAR_INT.encode(buf, entry.palette());
                ByteBufCodecs.STRING_UTF8.encode(buf, entry.name());
            }
            ByteBufCodecs.BOOL.encode(buf, payload.pinned != null);
            if (payload.pinned != null) BlockPos.STREAM_CODEC.encode(buf, payload.pinned);
        }
    };
    public static void sendPage(ServerPlayer player, int requestedPage) {
        ServerLevel level = player.server.getLevel(ModDimensions.DEEP_MYCELIUM_LEVEL);
        if (level == null || player.connection == null) return;
        List<PostMatriarchExpeditionData.Enclave> all = PostMatriarchExpeditionData.get(level).getEnclaves();
        int lastPage = Math.max(0, (all.size() - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(lastPage, requestedPage));
        VIEWER_PAGES.put(player, page);
        int start = page * PAGE_SIZE;
        List<Entry> entries = all.subList(start, Math.min(all.size(), start + PAGE_SIZE)).stream()
                .map(e -> new Entry(e.pos(), e.outcome().id(), e.palette(), e.name())).toList();
        PacketDistributor.sendToPlayer(player, new EnclaveDirectoryPayload(page, all.size(), entries,
                player.getData(ModAttachments.DISCOVERY).getPinnedEnclave()));
    }
    public static void refreshPage(ServerPlayer player) {
        sendPage(player, VIEWER_PAGES.getOrDefault(player, 0));
    }
    public static void handleOnClient(EnclaveDirectoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientExpeditionCache.updateDirectory(payload));
    }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
