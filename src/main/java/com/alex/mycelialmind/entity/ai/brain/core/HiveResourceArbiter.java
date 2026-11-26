package com.alex.mycelialmind.entity.ai.brain.core;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Zero-allocation priority-based channel arbiter preventing conflicting concurrent
 * {@link HiveBehaviour} execution across {@link HiveResourceChannel} locks.
 */
public final class HiveResourceArbiter {
    private static final int CHANNEL_COUNT = HiveResourceChannel.values().length;

    private final Object[] owners = new Object[CHANNEL_COUNT];
    private final int[] priorities = new int[CHANNEL_COUNT];
    private final Object[] preemptedScratch = new Object[CHANNEL_COUNT];

    public boolean canAcquire(Object owner, int channelMask, int priority) {
        if (channelMask == 0) {
            return true;
        }
        Objects.requireNonNull(owner, "owner");
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            if ((channelMask & (1 << i)) == 0) {
                continue;
            }
            Object currentOwner = this.owners[i];
            if (currentOwner != null && currentOwner != owner && priority <= this.priorities[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean acquire(
            Object owner,
            int channelMask,
            int priority,
            @Nullable Consumer<Object> onPreempted) {
        if (channelMask == 0) {
            return true;
        }
        if (!canAcquire(owner, channelMask, priority)) {
            return false;
        }

        int preemptedCount = 0;
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            if ((channelMask & (1 << i)) == 0) {
                continue;
            }
            Object currentOwner = this.owners[i];
            if (currentOwner != null && currentOwner != owner) {
                boolean seen = false;
                for (int p = 0; p < preemptedCount; p++) {
                    if (this.preemptedScratch[p] == currentOwner) {
                        seen = true;
                        break;
                    }
                }
                if (!seen) {
                    this.preemptedScratch[preemptedCount++] = currentOwner;
                }
            }
        }

        for (int p = 0; p < preemptedCount; p++) {
            Object displaced = this.preemptedScratch[p];
            this.preemptedScratch[p] = null;
            release(displaced);
            if (onPreempted != null) {
                onPreempted.accept(displaced);
            }
        }

        for (int i = 0; i < CHANNEL_COUNT; i++) {
            if ((channelMask & (1 << i)) != 0) {
                this.owners[i] = owner;
                this.priorities[i] = priority;
            }
        }
        return true;
    }

    public void release(@Nullable Object owner) {
        if (owner == null) {
            return;
        }
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            if (this.owners[i] == owner) {
                this.owners[i] = null;
                this.priorities[i] = 0;
            }
        }
    }

    public void releaseAll() {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            this.owners[i] = null;
            this.priorities[i] = 0;
            this.preemptedScratch[i] = null;
        }
    }

    public int occupiedMask() {
        int mask = 0;
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            if (this.owners[i] != null) {
                mask |= (1 << i);
            }
        }
        return mask;
    }

    public String formatOccupied() {
        return HiveResourceChannel.formatMask(occupiedMask());
    }

    @Nullable
    public Object getOwner(HiveResourceChannel channel) {
        return channel != null ? this.owners[channel.ordinal()] : null;
    }
}
