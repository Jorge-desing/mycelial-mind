package com.alex.mycelialmind.entity.ai.brain.core;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fixed-capacity ring buffer capturing recent AI decision events (activity transitions,
 * profile switches, reactive signals, behaviour interrupts, channel preemptions, and pauses)
 * for live diagnostics without unbounded allocation.
 */
public final class HiveBrainTrace {
    public static final int DEFAULT_CAPACITY = 16;

    public enum EventKind {
        ACTIVITY,
        PROFILE,
        SIGNAL,
        INTERRUPT,
        CHANNEL_PREEMPT,
        MODULE,
        PAUSE
    }

    public record Entry(long gameTime, EventKind kind, String detail) {
        public Entry {
            Objects.requireNonNull(kind, "kind");
            detail = detail != null ? detail : "";
        }

        public String format() {
            return "[t=" + this.gameTime + "] " + this.kind.name() + ": " + this.detail;
        }
    }

    private final Entry[] buffer;
    private int head = 0;
    private int count = 0;

    public HiveBrainTrace() {
        this(DEFAULT_CAPACITY);
    }

    public HiveBrainTrace(int capacity) {
        this.buffer = new Entry[Math.max(1, capacity)];
    }

    public void record(long gameTime, EventKind kind, String detail) {
        Entry entry = new Entry(gameTime, kind, detail);
        int writeIndex = (this.head + this.count) % this.buffer.length;
        this.buffer[writeIndex] = entry;
        if (this.count < this.buffer.length) {
            this.count++;
        } else {
            this.head = (this.head + 1) % this.buffer.length;
        }
    }

    public List<Entry> recentEntries() {
        if (this.count == 0) {
            return List.of();
        }
        List<Entry> result = new ArrayList<>(this.count);
        for (int i = 0; i < this.count; i++) {
            result.add(this.buffer[(this.head + i) % this.buffer.length]);
        }
        return result;
    }

    @Nullable
    public Entry latest() {
        if (this.count == 0) {
            return null;
        }
        int lastIndex = (this.head + this.count - 1) % this.buffer.length;
        return this.buffer[lastIndex];
    }

    public int size() {
        return this.count;
    }

    public int capacity() {
        return this.buffer.length;
    }

    public void clear() {
        for (int i = 0; i < this.buffer.length; i++) {
            this.buffer[i] = null;
        }
        this.head = 0;
        this.count = 0;
    }
}
