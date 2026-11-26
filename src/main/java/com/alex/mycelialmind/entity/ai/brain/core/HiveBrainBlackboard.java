package com.alex.mycelialmind.entity.ai.brain.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed per-entity local blackboard storing permanent or TTL-expiring entries,
 * named cooldown timers, and selective NBT persistence.
 */
public final class HiveBrainBlackboard {
    public static final long NO_EXPIRY = Long.MAX_VALUE;
    private static final String TAG_COOLDOWNS = "Cooldowns";
    private static final String TAG_KEYS = "Keys";
    private static final String TAG_EXPIRES_AT = "ExpiresAt";

    private record Entry(Object value, long expiresAtGameTime) {
        boolean isExpired(long currentGameTime) {
            return this.expiresAtGameTime != NO_EXPIRY && currentGameTime >= this.expiresAtGameTime;
        }
    }

    private final Map<HiveBrainKey<?>, Entry> entries = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, HiveBrainKey<?>> localPersistentKeys = new HashMap<>();

    public void registerPersistentKey(HiveBrainKey<?> key) {
        Objects.requireNonNull(key, "key");
        if (key.isPersistent()) {
            this.localPersistentKeys.put(key.id(), key);
        }
    }

    public <V> void set(HiveBrainKey<V> key, @Nullable V value) {
        Objects.requireNonNull(key, "key");
        if (value == null) {
            this.entries.remove(key);
            return;
        }
        validateValueType(key, value);
        if (key.isPersistent()) {
            this.localPersistentKeys.put(key.id(), key);
        }
        this.entries.put(key, new Entry(value, NO_EXPIRY));
    }

    public <V> void setExpiring(HiveBrainKey<V> key, @Nullable V value, int ttlTicks, long currentGameTime) {
        Objects.requireNonNull(key, "key");
        if (value == null || ttlTicks <= 0) {
            this.entries.remove(key);
            return;
        }
        validateValueType(key, value);
        if (key.isPersistent()) {
            this.localPersistentKeys.put(key.id(), key);
        }
        long expiresAt = currentGameTime > Long.MAX_VALUE - ttlTicks
                ? Long.MAX_VALUE
                : currentGameTime + ttlTicks;
        this.entries.put(key, new Entry(value, expiresAt));
    }

    @Nullable
    public <V> V get(HiveBrainKey<V> key) {
        Objects.requireNonNull(key, "key");
        Entry entry = this.entries.get(key);
        return entry != null ? key.valueType().cast(entry.value()) : null;
    }

    @Nullable
    public <V> V get(HiveBrainKey<V> key, long currentGameTime) {
        Objects.requireNonNull(key, "key");
        Entry entry = this.entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(currentGameTime)) {
            this.entries.remove(key);
            return null;
        }
        return key.valueType().cast(entry.value());
    }

    public <V> Optional<V> find(HiveBrainKey<V> key, long currentGameTime) {
        return Optional.ofNullable(get(key, currentGameTime));
    }

    public boolean has(HiveBrainKey<?> key, long currentGameTime) {
        return get(key, currentGameTime) != null;
    }

    public void remove(HiveBrainKey<?> key) {
        if (key != null) {
            this.entries.remove(key);
        }
    }

    public void clear() {
        this.entries.clear();
        this.cooldowns.clear();
    }

    public int size() {
        return this.entries.size();
    }

    public int activeEntryCount(long currentGameTime) {
        pruneExpiredEntries(currentGameTime);
        return this.entries.size();
    }

    public void setCooldown(String name, long currentGameTime, int cooldownTicks) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (cooldownTicks <= 0) {
            this.cooldowns.remove(name);
            return;
        }
        long endTick = HiveBrainScheduler.computeCooldownFinishedAt(currentGameTime, cooldownTicks);
        if (endTick == HiveBrainScheduler.INITIAL_TICK_SENTINEL) {
            this.cooldowns.remove(name);
        } else {
            this.cooldowns.put(name, endTick);
        }
    }

    public boolean isOnCooldown(String name, long currentGameTime) {
        if (name == null || name.isBlank()) {
            return false;
        }
        Long endTick = this.cooldowns.get(name);
        if (endTick == null) {
            return false;
        }
        if (!HiveBrainScheduler.isOnCooldown(currentGameTime, endTick)) {
            this.cooldowns.remove(name);
            return false;
        }
        return true;
    }

    public long getRemainingCooldown(String name, long currentGameTime) {
        if (!isOnCooldown(name, currentGameTime)) {
            return 0L;
        }
        Long endTick = this.cooldowns.get(name);
        return endTick != null ? Math.max(0L, endTick - currentGameTime) : 0L;
    }

    public void clearCooldown(String name) {
        if (name != null) {
            this.cooldowns.remove(name);
        }
    }

    public int getActiveCooldownCount(long currentGameTime) {
        pruneExpiredCooldowns(currentGameTime);
        return this.cooldowns.size();
    }

    public int pruneExpired(long currentGameTime) {
        return pruneExpiredEntries(currentGameTime) + pruneExpiredCooldowns(currentGameTime);
    }

    private int pruneExpiredEntries(long currentGameTime) {
        int removed = 0;
        Iterator<Map.Entry<HiveBrainKey<?>, Entry>> it = this.entries.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired(currentGameTime)) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    private int pruneExpiredCooldowns(long currentGameTime) {
        int removed = 0;
        Iterator<Map.Entry<String, Long>> it = this.cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            if (!HiveBrainScheduler.isOnCooldown(currentGameTime, it.next().getValue())) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public CompoundTag saveToNbt(CompoundTag root, long currentGameTime) {
        if (currentGameTime >= 0L) {
            pruneExpired(currentGameTime);
        }

        if (!this.cooldowns.isEmpty()) {
            CompoundTag cooldownsTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : this.cooldowns.entrySet()) {
                cooldownsTag.putLong(entry.getKey(), entry.getValue());
            }
            if (!cooldownsTag.isEmpty()) {
                root.put(TAG_COOLDOWNS, cooldownsTag);
            }
        }

        if (!this.entries.isEmpty()) {
            CompoundTag keysTag = new CompoundTag();
            for (Map.Entry<HiveBrainKey<?>, Entry> entry : this.entries.entrySet()) {
                HiveBrainKey<?> key = entry.getKey();
                if (!key.isPersistent()) {
                    continue;
                }
                CompoundTag valueTag = new CompoundTag();
                key.writeToNbt(valueTag, entry.getValue().value());
                if (entry.getValue().expiresAtGameTime() != NO_EXPIRY) {
                    valueTag.putLong(TAG_EXPIRES_AT, entry.getValue().expiresAtGameTime());
                }
                if (!valueTag.isEmpty()) {
                    keysTag.put(key.id(), valueTag);
                }
            }
            if (!keysTag.isEmpty()) {
                root.put(TAG_KEYS, keysTag);
            }
        }
        return root;
    }

    public void loadFromNbt(CompoundTag root) {
        this.cooldowns.clear();
        if (root == null) {
            return;
        }
        if (root.contains(TAG_COOLDOWNS, Tag.TAG_COMPOUND)) {
            CompoundTag cooldownsTag = root.getCompound(TAG_COOLDOWNS);
            for (String name : cooldownsTag.getAllKeys()) {
                if (cooldownsTag.contains(name, Tag.TAG_ANY_NUMERIC)) {
                    this.cooldowns.put(name, cooldownsTag.getLong(name));
                }
            }
        }
        if (root.contains(TAG_KEYS, Tag.TAG_COMPOUND)) {
            CompoundTag keysTag = root.getCompound(TAG_KEYS);
            for (String keyId : keysTag.getAllKeys()) {
                HiveBrainKey<?> key = resolvePersistentKey(keyId);
                if (key == null || !keysTag.contains(keyId, Tag.TAG_COMPOUND)) {
                    continue;
                }
                CompoundTag valueTag = keysTag.getCompound(keyId);
                Object decoded = key.readFromNbt(valueTag);
                if (decoded == null) {
                    continue;
                }
                long expiresAt = valueTag.contains(TAG_EXPIRES_AT, Tag.TAG_ANY_NUMERIC)
                        ? valueTag.getLong(TAG_EXPIRES_AT)
                        : NO_EXPIRY;
                this.localPersistentKeys.put(key.id(), key);
                this.entries.put(key, new Entry(decoded, expiresAt));
            }
        }
    }

    @Nullable
    private HiveBrainKey<?> resolvePersistentKey(String keyId) {
        HiveBrainKey<?> local = this.localPersistentKeys.get(keyId);
        return local != null ? local : HiveBrainKey.lookupPersistent(keyId);
    }

    private static <V> void validateValueType(HiveBrainKey<V> key, Object value) {
        if (!key.valueType().isInstance(value)) {
            throw new IllegalArgumentException(
                    "Invalid value type for HiveBrainKey '" + key.id() + "': expected "
                            + key.valueType().getName() + ", got " + value.getClass().getName());
        }
    }
}
