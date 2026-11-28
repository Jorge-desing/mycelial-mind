package com.alex.mycelialmind.benchmark;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class HomeostasisMapBenchmark {
    @Param({"256", "4096"})
    public int size;
    private HashMap<Long, Object> boxed;
    private Long2ObjectOpenHashMap<Object> primitive;
    private HashMap<Long, Boolean> order;
    private long[] keys;
    private int index;
    private final Object value = new Object();

    @Setup(Level.Iteration)
    public void setup() {
        boxed = new HashMap<>();
        primitive = new Long2ObjectOpenHashMap<>();
        order = new HashMap<>();
        keys = new long[size];
        for (int i = 0; i < size; i++) {
            long key = ((long) (i / 64 - 32) << 32) | ((i % 64 - 32) & 0xffffffffL);
            keys[i] = key;
            boxed.put(key, value);
            primitive.put(key, value);
            order.put(key, Boolean.TRUE);
        }
        index = 0;
    }

    private long nextKey() {
        long key = keys[index];
        index = (index + 1) % size;
        return key;
    }

    @Benchmark public Object hashMapLookup() { return boxed.get(nextKey()); }
    @Benchmark public Object fastutilLookup() { return primitive.get(nextKey()); }
    @Benchmark public Object hashMapUpdate() { return boxed.put(nextKey(), value); }
    @Benchmark public Object fastutilUpdate() { return primitive.put(nextKey(), value); }
    @Benchmark public Object hashMapRemoveAndRestore() {
        long key = nextKey();
        Object previous = boxed.remove(key);
        boxed.put(key, value);
        return previous;
    }
    @Benchmark public Object fastutilWithOrderRemoveAndRestore() {
        long key = nextKey();
        Object previous = primitive.remove(key);
        order.remove(key);
        primitive.put(key, value);
        order.put(key, Boolean.TRUE);
        return previous;
    }
    @Benchmark public Object fastutilRemoveAndRestore() {
        long key = nextKey();
        Object previous = primitive.remove(key);
        primitive.put(key, value);
        return previous;
    }
}
