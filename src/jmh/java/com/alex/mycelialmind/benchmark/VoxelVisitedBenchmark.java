package com.alex.mycelialmind.benchmark;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class VoxelVisitedBenchmark {
    @Param({"512", "8192"}) public int count;
    @Param({"compact", "dispersed"}) public String layout;
    private long[] positions;

    @Setup public void setup() {
        positions = new long[count];
        for (int i = 0; i < count; i++) {
            int x = i % 32;
            int z = i / 32;
            if (layout.equals("dispersed")) { x *= 31; z *= 37; }
            positions[i] = ((long) x & 0x3ffffffL) << 38 | ((long) z & 0x3ffffffL) << 12 | 64L;
        }
    }

    @Benchmark public int boxedVisited() {
        var visited = new HashSet<Long>();
        for (long pos : positions) visited.add(pos);
        int found = 0;
        for (long pos : positions) if (visited.contains(pos)) found++;
        return found;
    }

    @Benchmark public int primitiveVisited() {
        var visited = new LongOpenHashSet();
        for (long pos : positions) visited.add(pos);
        int found = 0;
        for (long pos : positions) if (visited.contains(pos)) found++;
        return found;
    }
    @Benchmark public int boxedFloodFill() {
        var visited = new HashSet<Long>();
        var queue = new java.util.ArrayDeque<Long>();
        visited.add(positions[0]);
        queue.add(positions[0]);
        var available = new HashSet<Long>();
        for (long pos : positions) available.add(pos);
        while (!queue.isEmpty()) {
            long pos = queue.remove();
            for (long delta : new long[]{1L << 38, -(1L << 38), 1L << 12, -(1L << 12)}) {
                long next = pos + delta;
                if (available.contains(next) && visited.add(next)) queue.add(next);
            }
        }
        return visited.size();
    }

    @Benchmark public int primitiveFloodFill() {
        var visited = new LongOpenHashSet();
        var queue = new java.util.ArrayDeque<Long>();
        visited.add(positions[0]);
        queue.add(positions[0]);
        var available = new HashSet<Long>();
        for (long pos : positions) available.add(pos);
        while (!queue.isEmpty()) {
            long pos = queue.remove();
            for (long delta : new long[]{1L << 38, -(1L << 38), 1L << 12, -(1L << 12)}) {
                long next = pos + delta;
                if (available.contains(next) && visited.add(next)) queue.add(next);
            }
        }
        return visited.size();
    }
}
