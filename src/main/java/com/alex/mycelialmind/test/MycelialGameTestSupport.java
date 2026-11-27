package com.alex.mycelialmind.test;

import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

/**
 * Reusable arena chunk-forcing, state reset, and probe helpers shared across NeoForge GameTest suites.
 */
final class MycelialGameTestSupport {

    private static final Map<Long, Integer> ARENA_CHUNK_REFS = new HashMap<>();
    private static final TicketType<ChunkPos> ARENA_ENTITY_TICKET =
            TicketType.create("mycelial_gametest_arena", Comparator.comparingLong(ChunkPos::toLong));

    private MycelialGameTestSupport() {
    }

    static HiveMindSavedData resetHive(ServerLevel level) {
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
        return hive;
    }

    static double stimulusForScore(double score) {
        return score / MycelialConfig.LEARNING_MULTIPLIER.get();
    }

    static BlockPos insetInChunk(BlockPos pos, int margin) {
        ChunkPos chunk = new ChunkPos(pos);
        int x = Math.clamp(pos.getX() - chunk.getMinBlockX(), margin, 15 - margin);
        int z = Math.clamp(pos.getZ() - chunk.getMinBlockZ(), margin, 15 - margin);
        return chunk.getBlockAt(x, pos.getY(), z);
    }

    static List<ChunkPos> forceArenaChunks(GameTestHelper helper, int minX, int maxX, int minZ, int maxZ) {
        ServerLevel level = helper.getLevel();
        BlockPos minPos = helper.absolutePos(new BlockPos(minX, 0, minZ));
        BlockPos maxPos = helper.absolutePos(new BlockPos(maxX, 0, maxZ));
        int minCx = Math.min(minPos.getX(), maxPos.getX()) >> 4;
        int maxCx = Math.max(minPos.getX(), maxPos.getX()) >> 4;
        int minCz = Math.min(minPos.getZ(), maxPos.getZ()) >> 4;
        int maxCz = Math.max(minPos.getZ(), maxPos.getZ()) >> 4;
        List<ChunkPos> tracked = new ArrayList<>();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                ChunkPos cp = new ChunkPos(cx, cz);
                long key = cp.toLong();
                int refs = ARENA_CHUNK_REFS.getOrDefault(key, 0);
                if (refs > 0) {
                    ARENA_CHUNK_REFS.put(key, refs + 1);
                } else {
                    level.getChunkSource().addRegionTicket(ARENA_ENTITY_TICKET, cp, 3, cp, true);
                    level.getChunk(cx, cz);
                    ARENA_CHUNK_REFS.put(key, 1);
                }
                tracked.add(cp);
            }
        }
        return tracked;
    }

    static boolean entityChunksReady(ServerLevel level, BlockPos center, int radius) {
        ChunkPos min = new ChunkPos(center.offset(-radius, 0, -radius));
        ChunkPos max = new ChunkPos(center.offset(radius, 0, radius));
        for (int chunkX = min.x; chunkX <= max.x; chunkX++) {
            for (int chunkZ = min.z; chunkZ <= max.z; chunkZ++) {
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                if (!level.isPositionEntityTicking(chunk.getWorldPosition())
                        || !level.areEntitiesLoaded(chunk.toLong())) {
                    LockSupport.parkNanos(2_000_000L);
                    return false;
                }
            }
        }
        return true;
    }

    static void releaseArenaChunks(ServerLevel level, List<ChunkPos> forcedChunks) {
        for (ChunkPos cp : forcedChunks) {
            long key = cp.toLong();
            int refs = ARENA_CHUNK_REFS.getOrDefault(key, 0);
            if (refs <= 1) {
                ARENA_CHUNK_REFS.remove(key);
                level.getChunkSource().removeRegionTicket(ARENA_ENTITY_TICKET, cp, 3, cp, true);
            } else {
                ARENA_CHUNK_REFS.put(key, refs - 1);
            }
        }
    }

    static boolean hasAcidShot(GameTestHelper helper, MycelialMob owner) {
        return !helper.getLevel().getEntitiesOfClass(AcidSpitProjectile.class,
                owner.getBoundingBox().inflate(20), shot -> shot.getOwner() == owner).isEmpty();
    }

    static void clearAcidShots(GameTestHelper helper, MycelialMob owner) {
        helper.getLevel().getEntitiesOfClass(AcidSpitProjectile.class,
                        owner.getBoundingBox().inflate(20), shot -> shot.getOwner() == owner)
                .forEach(AcidSpitProjectile::discard);
    }

    static void resetHomeostasisChunks(DimensionalHomeostasisSavedData data, ChunkPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                data.resetChunk(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
    }

    static BlockPos homeostasisTestPos(GameTestHelper helper, int slot, BlockPos relativePos) {
        // GameTest batches run concurrently and homeostasis state is keyed by chunk.
        // Keep each scenario's live blocks in a separate chunk strip (3 chunks / 48 blocks apart along X).
        int separation = slot * 48;
        return helper.absolutePos(relativePos).offset(separation, 0, 0);
    }
}
