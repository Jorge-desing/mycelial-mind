package com.alex.mycelialmind.world.structure;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * Shared 6-neighbor voxel engineering utilities for Mycelial Mind structures.
 * Guarantees zero floating/corner-only blocks in tentacles, roots, arches, and chandeliers.
 */
public final class VoxelStructureOps {
    private VoxelStructureOps() {
    }

    /**
     * Places a strictly 6-connected (face-adjacent) path through the given waypoints.
     */
    public static List<BlockPos> placeFaceConnectedPath(
            WorldGenLevel level,
            BoundingBox box,
            List<BlockPos> waypoints,
            IntFunction<BlockState> stateForStep
    ) {
        List<BlockPos> placed = new ArrayList<>();
        if (waypoints == null || waypoints.isEmpty()) {
            return placed;
        }
        int stepIdx = 0;
        BlockPos current = waypoints.get(0);
        if (box.isInside(current)) {
            level.setBlock(current, stateForStep.apply(stepIdx), 2);
            placed.add(current);
        }
        for (int i = 1; i < waypoints.size(); i++) {
            BlockPos target = waypoints.get(i);
            while (!current.equals(target)) {
                int dx = Integer.compare(target.getX(), current.getX());
                int dy = Integer.compare(target.getY(), current.getY());
                int dz = Integer.compare(target.getZ(), current.getZ());
                // Advance one orthogonal axis at a time so every block shares a full face
                if (dx != 0) {
                    current = current.offset(dx, 0, 0);
                } else if (dy != 0) {
                    current = current.offset(0, dy, 0);
                } else if (dz != 0) {
                    current = current.offset(0, 0, dz);
                }
                stepIdx++;
                if (box.isInside(current)) {
                    level.setBlock(current, stateForStep.apply(stepIdx), 2);
                    placed.add(current);
                }
            }
        }
        return placed;
    }

    /**
     * Welds a solid anchor block at ceilingPos and hangs chainCount chains + lampBlock below it.
     */
    public static void placeAnchoredChain(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos ceilingPos,
            int chainCount,
            BlockState anchorBlock,
            BlockState lampBlock
    ) {
        if (box.isInside(ceilingPos)) {
            level.setBlock(ceilingPos, anchorBlock, 2);
        }
        for (int i = 1; i <= chainCount; i++) {
            BlockPos cp = ceilingPos.below(i);
            if (box.isInside(cp)) {
                level.setBlock(cp, Blocks.CHAIN.defaultBlockState(), 2);
            }
        }
        BlockPos lampPos = ceilingPos.below(chainCount + 1);
        if (box.isInside(lampPos)) {
            level.setBlock(lampPos, lampBlock, 2);
        }
    }

    /**
     * Lays a continuous 6-connected synaptic conduit (veinBlock embedded in bedBlock) between two floor points.
     */
    public static void placeSynapticConduit(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos from,
            BlockPos to,
            BlockState bedBlock,
            BlockState veinBlock
    ) {
        BlockPos current = from;
        placeConduitCell(level, box, current, bedBlock, veinBlock);
        while (current.getX() != to.getX() || current.getZ() != to.getZ()) {
            int dx = Integer.compare(to.getX(), current.getX());
            int dz = Integer.compare(to.getZ(), current.getZ());
            if (dx != 0) {
                current = current.offset(dx, 0, 0);
            } else {
                current = current.offset(0, 0, dz);
            }
            placeConduitCell(level, box, current, bedBlock, veinBlock);
        }
    }

    private static void placeConduitCell(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos pos,
            BlockState bedBlock,
            BlockState veinBlock
    ) {
        if (box.isInside(pos)) {
            level.setBlock(pos, veinBlock, 2);
        }
        BlockPos bed = pos.below();
        if (box.isInside(bed)) {
            level.setBlock(bed, bedBlock, 2);
        }
    }

    /**
     * Runs a 6-neighbor BFS flood-fill from seed inside box and returns any structure blocks
     * that are not face-connected to the main connected component.
     */
    public static List<BlockPos> findDisconnectedPositions(
            LevelReader level,
            BoundingBox box,
            BlockPos seed,
            Predicate<BlockState> isStructureBlock
    ) {
        LongOpenHashSet visited = new LongOpenHashSet();
        Queue<BlockPos> queue = new ArrayDeque<>();
        if (box.isInside(seed) && isStructureBlock.test(level.getBlockState(seed))) {
            visited.add(seed.asLong());
            queue.add(seed);
        }
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (box.isInside(next)) {
                    long key = next.asLong();
                    if (!visited.contains(key) && isStructureBlock.test(level.getBlockState(next))) {
                        visited.add(key);
                        queue.add(next);
                    }
                }
            }
        }

        List<BlockPos> disconnected = new ArrayList<>();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!visited.contains(p.asLong()) && isStructureBlock.test(level.getBlockState(p))) {
                        disconnected.add(p);
                    }
                }
            }
        }
        return disconnected;
    }
}
