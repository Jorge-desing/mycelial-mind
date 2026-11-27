package com.alex.mycelialmind.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Shared Netty buffer helpers for bounded list, optional BlockPos, and UUID codecs across payloads.
 */
final class NetworkCodecSupport {
    private static final UUID ZERO_UUID = new UUID(0L, 0L);

    private NetworkCodecSupport() {
    }

    static int decodeBoundedCount(ByteBuf buf, int max, String fieldName) {
        int count = ByteBufCodecs.VAR_INT.decode(buf);
        if (count < 0 || count > max) {
            throw new DecoderException("Invalid " + fieldName + " count: " + count);
        }
        return count;
    }

    static int decodeClampedCount(ByteBuf buf, int max) {
        return Math.max(0, Math.min(max, ByteBufCodecs.VAR_INT.decode(buf)));
    }

    static void writeUuid(ByteBuf buf, @Nullable UUID uuid) {
        UUID value = uuid != null ? uuid : ZERO_UUID;
        buf.writeLong(value.getMostSignificantBits());
        buf.writeLong(value.getLeastSignificantBits());
    }

    static UUID readUuid(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    static void writeOptionalUuid(ByteBuf buf, @Nullable UUID uuid) {
        ByteBufCodecs.BOOL.encode(buf, uuid != null);
        if (uuid != null) {
            writeUuid(buf, uuid);
        }
    }

    @Nullable
    static UUID readOptionalUuid(ByteBuf buf) {
        return ByteBufCodecs.BOOL.decode(buf) ? readUuid(buf) : null;
    }

    static void writeOptionalBlockPos(ByteBuf buf, @Nullable BlockPos pos) {
        ByteBufCodecs.BOOL.encode(buf, pos != null);
        if (pos != null) {
            BlockPos.STREAM_CODEC.encode(buf, pos);
        }
    }

    @Nullable
    static BlockPos readOptionalBlockPos(ByteBuf buf) {
        return ByteBufCodecs.BOOL.decode(buf) ? BlockPos.STREAM_CODEC.decode(buf) : null;
    }

    static <T> void encodeBoundedList(
            ByteBuf buf, @Nullable List<T> list, int max, BiConsumer<ByteBuf, T> encoder) {
        List<T> safeList = list != null ? list : List.of();
        int count = Math.min(max, safeList.size());
        ByteBufCodecs.VAR_INT.encode(buf, count);
        for (int i = 0; i < count; i++) {
            encoder.accept(buf, safeList.get(i));
        }
    }

    static <T> void encodeStrictBoundedList(
            ByteBuf buf, List<T> list, int max, String fieldName, BiConsumer<ByteBuf, T> encoder) {
        if (list.size() > max) {
            throw new EncoderException("Too many " + fieldName + ": " + list.size());
        }
        ByteBufCodecs.VAR_INT.encode(buf, list.size());
        for (T element : list) {
            encoder.accept(buf, element);
        }
    }

    static <T> List<T> decodeBoundedList(
            ByteBuf buf, int max, String fieldName, Function<ByteBuf, T> decoder) {
        int count = decodeBoundedCount(buf, max, fieldName);
        List<T> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(decoder.apply(buf));
        }
        return items;
    }

    static <T> List<T> decodeClampedList(ByteBuf buf, int max, Function<ByteBuf, T> decoder) {
        int count = decodeClampedCount(buf, max);
        List<T> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(decoder.apply(buf));
        }
        return items;
    }
}
