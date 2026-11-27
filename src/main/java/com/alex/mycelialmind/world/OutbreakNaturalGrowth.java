package com.alex.mycelialmind.world;

import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Advances a cave outbreak by one exposed, connected floor block per pulse. */
public final class OutbreakNaturalGrowth {
    private static final int MAX_VISITED = 1024;
    private static final int MAX_DISTANCE = 32;
    private static final Direction[] DIRECTIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private OutbreakNaturalGrowth() {}

    public static boolean pulse(ServerLevel level, BlockPos source, RandomSource random) {
        return grow(level, source, random) != null;
    }

    public static BlockPos grow(ServerLevel level, BlockPos source, RandomSource random) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos seed = source.below();
        if (tracker.isProtected(source) || !level.hasChunkAt(seed)) {
            return null;
        }
        if (!level.getBlockState(seed).is(ModBlocks.CREEPING_MYCELIUM.get())) {
            BlockState original = level.getBlockState(seed);
            if (tracker.isProtected(seed) || level.getBlockEntity(seed) != null || !level.getFluidState(seed).isEmpty()
                    || !(original.is(BlockTags.BASE_STONE_OVERWORLD) || original.is(BlockTags.DIRT)
                    || original.is(ModBlocks.MYCELIAL_STONE.get()))) {
                return null;
            }
            return level.setBlockAndUpdate(seed, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState()) ? seed : null;
        }

        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(seed);
        visited.add(seed);
        BlockPos chosen = null;
        int candidates = 0;
        int rotation = random.nextInt(DIRECTIONS.length);
        int processed = 0;
        while (!pending.isEmpty() && processed++ < MAX_VISITED) {
            BlockPos floor = pending.removeLast();
            for (int step = 0; step < DIRECTIONS.length; step++) {
                Direction direction = DIRECTIONS[(step + rotation) % DIRECTIONS.length];
                for (int dy : new int[]{0, 1, -1}) {
                    BlockPos next = floor.relative(direction).offset(0, dy, 0);
                    if (Math.abs(next.getX() - source.getX()) > MAX_DISTANCE
                            || Math.abs(next.getZ() - source.getZ()) > MAX_DISTANCE
                            || Math.abs(next.getY() - source.getY()) > 72
                            || !level.hasChunkAt(next) || !visited.add(next) || tracker.isProtected(next)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(next);
                    if (state.is(ModBlocks.CREEPING_MYCELIUM.get())) {
                        pending.addLast(next);
                        continue;
                    }
                    if (level.isOutsideBuildHeight(next.above(2)) || level.getBlockEntity(next) != null
                            || !level.getFluidState(next).isEmpty()
                            || !(state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)
                            || state.is(ModBlocks.MYCELIAL_STONE.get()))
                            || !state.isFaceSturdy(level, next, Direction.UP)
                            || !level.getBlockState(next.above()).isAir()
                            || !level.getBlockState(next.above(2)).isAir()) {
                        continue;
                    }
                    if (dy != 0 && !level.getBlockState(floor.above(3)).isAir()) {
                        continue;
                    }
                    if (random.nextInt(++candidates) == 0) {
                        chosen = next;
                    }
                }
            }
        }
        return chosen != null && level.setBlockAndUpdate(chosen, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState())
                ? chosen : null;
    }
}
