package com.alex.mycelialmind.benchmark;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
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
    @Param({"16", "32"}) public int side;
    @Param({"compact", "sparse"}) public String layout;
    private boolean[] occupied;
    private int volume;

    @Setup public void setup() {
        volume = side * side * side;
        occupied = new boolean[volume];
        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    occupied[(y * side + z) * side + x] = layout.equals("compact")
                            || x == 0 || z == 0 || y == 0 || (x % 5 == 0 && z % 5 == 0);
                }
            }
        }
        int boxed = boxedFloodFill();
        int primitive = primitiveFloodFill();
        if (boxed != primitive || boxed != 0) {
            throw new IllegalStateException("Benchmark structure is not connected or results differ");
        }
    }

    @Benchmark public int boxedFloodFill() {
        return floodFill(new HashSet<>(), null);
    }

    @Benchmark public int primitiveFloodFill() {
        return floodFill(null, new LongOpenHashSet());
    }

    private int floodFill(HashSet<Long> boxed, LongOpenHashSet primitive) {
        var queue = new ArrayDeque<Integer>();
        queue.add(0);
        if (boxed != null) boxed.add(0L); else primitive.add(0L);
        int[] dx = {1, -1, 0, 0, 0, 0};
        int[] dy = {0, 0, 1, -1, 0, 0};
        int[] dz = {0, 0, 0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int cell = queue.remove();
            int x = cell % side, z = (cell / side) % side, y = cell / (side * side);
            for (int d = 0; d < 6; d++) {
                int nx = x + dx[d], ny = y + dy[d], nz = z + dz[d];
                if (nx < 0 || ny < 0 || nz < 0 || nx >= side || ny >= side || nz >= side) continue;
                int next = (ny * side + nz) * side + nx;
                if (!occupied[next]) continue;
                boolean added = boxed != null ? boxed.add((long) next) : primitive.add(next);
                if (added) queue.add(next);
            }
        }
        int disconnected = 0;
        for (int i = 0; i < volume; i++) {
            boolean visited = boxed != null ? boxed.contains((long) i) : primitive.contains(i);
            if (occupied[i] && !visited) disconnected++;
        }
        return disconnected;
    }
}
