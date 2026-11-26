package com.alex.mycelialmind.entity.ai.brain.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Type-safe key for {@link HiveBrainBlackboard} supporting both ephemeral runtime values
 * and NBT-serializable persistent entries without registry dependencies.
 */
public final class HiveBrainKey<V> {
    private static final String VALUE_TAG = "v";
    private static final ConcurrentHashMap<String, HiveBrainKey<?>> PERSISTENT_REGISTRY = new ConcurrentHashMap<>();

    private final String id;
    private final Class<V> valueType;
    @Nullable
    private final BiConsumer<CompoundTag, V> nbtWriter;
    @Nullable
    private final Function<CompoundTag, V> nbtReader;

    private HiveBrainKey(
            String id,
            Class<V> valueType,
            @Nullable BiConsumer<CompoundTag, V> nbtWriter,
            @Nullable Function<CompoundTag, V> nbtReader) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("HiveBrainKey id must not be blank");
        }
        this.id = id;
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.nbtWriter = nbtWriter;
        this.nbtReader = nbtReader;
    }

    public static <V> HiveBrainKey<V> of(String id, Class<V> valueType) {
        return new HiveBrainKey<>(id, valueType, null, null);
    }

    public static <V> HiveBrainKey<V> persistent(
            String id,
            Class<V> valueType,
            BiConsumer<CompoundTag, V> nbtWriter,
            Function<CompoundTag, V> nbtReader) {
        Objects.requireNonNull(nbtWriter, "nbtWriter");
        Objects.requireNonNull(nbtReader, "nbtReader");
        HiveBrainKey<V> key = new HiveBrainKey<>(id, valueType, nbtWriter, nbtReader);
        PERSISTENT_REGISTRY.put(id, key);
        return key;
    }

    @Nullable
    public static HiveBrainKey<?> lookupPersistent(String id) {
        return id != null ? PERSISTENT_REGISTRY.get(id) : null;
    }

    public static HiveBrainKey<Integer> persistentInt(String id) {
        return persistent(
                id,
                Integer.class,
                (tag, value) -> tag.putInt(VALUE_TAG, value),
                tag -> tag.contains(VALUE_TAG, Tag.TAG_ANY_NUMERIC) ? tag.getInt(VALUE_TAG) : null
        );
    }

    public static HiveBrainKey<Long> persistentLong(String id) {
        return persistent(
                id,
                Long.class,
                (tag, value) -> tag.putLong(VALUE_TAG, value),
                tag -> tag.contains(VALUE_TAG, Tag.TAG_ANY_NUMERIC) ? tag.getLong(VALUE_TAG) : null
        );
    }

    public static HiveBrainKey<Float> persistentFloat(String id) {
        return persistent(
                id,
                Float.class,
                (tag, value) -> tag.putFloat(VALUE_TAG, value),
                tag -> tag.contains(VALUE_TAG, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(VALUE_TAG) : null
        );
    }

    public static HiveBrainKey<Double> persistentDouble(String id) {
        return persistent(
                id,
                Double.class,
                (tag, value) -> tag.putDouble(VALUE_TAG, value),
                tag -> tag.contains(VALUE_TAG, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(VALUE_TAG) : null
        );
    }

    public static HiveBrainKey<Boolean> persistentBoolean(String id) {
        return persistent(
                id,
                Boolean.class,
                (tag, value) -> tag.putBoolean(VALUE_TAG, value),
                tag -> tag.contains(VALUE_TAG) ? tag.getBoolean(VALUE_TAG) : null
        );
    }

    public static HiveBrainKey<String> persistentString(String id) {
        return persistent(
                id,
                String.class,
                (tag, value) -> tag.putString(VALUE_TAG, value),
                tag -> tag.contains(VALUE_TAG, Tag.TAG_STRING) ? tag.getString(VALUE_TAG) : null
        );
    }

    public static HiveBrainKey<BlockPos> persistentBlockPos(String id) {
        return persistent(
                id,
                BlockPos.class,
                (tag, value) -> tag.putLong(VALUE_TAG, value.asLong()),
                tag -> tag.contains(VALUE_TAG, Tag.TAG_ANY_NUMERIC) ? BlockPos.of(tag.getLong(VALUE_TAG)) : null
        );
    }

    public static HiveBrainKey<UUID> persistentUuid(String id) {
        return persistent(
                id,
                UUID.class,
                (tag, value) -> tag.putUUID(VALUE_TAG, value),
                tag -> tag.hasUUID(VALUE_TAG) ? tag.getUUID(VALUE_TAG) : null
        );
    }

    public String id() {
        return this.id;
    }

    public Class<V> valueType() {
        return this.valueType;
    }

    public boolean isPersistent() {
        return this.nbtWriter != null && this.nbtReader != null;
    }

    public void writeToNbt(CompoundTag tag, Object value) {
        if (!isPersistent() || value == null) {
            return;
        }
        if (!this.valueType.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Invalid value type for HiveBrainKey '" + this.id + "': expected "
                            + this.valueType.getName() + ", got " + value.getClass().getName());
        }
        this.nbtWriter.accept(tag, this.valueType.cast(value));
    }

    @Nullable
    public V readFromNbt(CompoundTag tag) {
        if (!isPersistent() || tag == null) {
            return null;
        }
        V decoded = this.nbtReader.apply(tag);
        if (decoded == null || !this.valueType.isInstance(decoded)) {
            return null;
        }
        return decoded;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HiveBrainKey<?> other)) {
            return false;
        }
        return this.id.equals(other.id) && this.valueType.equals(other.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.valueType);
    }

    @Override
    public String toString() {
        return "HiveBrainKey[" + this.id + ":" + this.valueType.getSimpleName() + "]";
    }
}
