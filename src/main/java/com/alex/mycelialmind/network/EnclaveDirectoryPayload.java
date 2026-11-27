package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionData;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** One bounded page of shared recovered enclaves. */
public record EnclaveDirectoryPayload(int page, int total, List<Entry> entries, BlockPos pinned) implements CustomPacketPayload {
    public static final int PAGE_SIZE = 6;
    private static final Map<ServerPlayer, Integer> VIEWER_PAGES =
            Collections.synchronizedMap(new WeakHashMap<>());
    public static final Type<EnclaveDirectoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "enclave_directory"));

    public record Entry(BlockPos pos, int outcome, int palette, String name) {
    }

    public static final StreamCodec<ByteBuf, EnclaveDirectoryPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnclaveDirectoryPayload decode(ByteBuf buf) {
            int page = ByteBufCodecs.VAR_INT.decode(buf);
            int total = ByteBufCodecs.VAR_INT.decode(buf);
            List<Entry> entries = NetworkCodecSupport.decodeBoundedList(buf, PAGE_SIZE, "enclave page size",
                    b -> new Entry(BlockPos.STREAM_CODEC.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                            ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.STRING_UTF8.decode(b)));
            BlockPos pinned = NetworkCodecSupport.readOptionalBlockPos(buf);
            return new EnclaveDirectoryPayload(page, total, entries, pinned);
        }

        @Override
        public void encode(ByteBuf buf, EnclaveDirectoryPayload payload) {
            ByteBufCodecs.VAR_INT.encode(buf, payload.page);
            ByteBufCodecs.VAR_INT.encode(buf, payload.total);
            NetworkCodecSupport.encodeStrictBoundedList(buf, payload.entries, PAGE_SIZE, "enclave page size",
                    (b, entry) -> {
                        BlockPos.STREAM_CODEC.encode(b, entry.pos());
                        ByteBufCodecs.VAR_INT.encode(b, entry.outcome());
                        ByteBufCodecs.VAR_INT.encode(b, entry.palette());
                        ByteBufCodecs.STRING_UTF8.encode(b, entry.name());
                    });
            NetworkCodecSupport.writeOptionalBlockPos(buf, payload.pinned);
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
