package com.alex.mycelialmind.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Finds nearby grounded positions where summoned entities have room to spawn.
 */
public final class MycelialSpawnUtil {
    private static final int POSITION_ATTEMPTS = 32;

    private MycelialSpawnUtil() {
    }

    public static BlockPos findOpenGroundPosition(
            ServerLevel level,
            Entity entity,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius
    ) {
        if (entity == null || horizontalRadius < 2 || verticalRadius < 0) {
            return null;
        }

        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
            int dx = level.random.nextIntBetweenInclusive(-horizontalRadius, horizontalRadius);
            int dy = level.random.nextIntBetweenInclusive(-verticalRadius, verticalRadius);
            int dz = level.random.nextIntBetweenInclusive(-horizontalRadius, horizontalRadius);
            if (dx * dx + dz * dz < 4) {
                continue;
            }

            BlockPos candidate = center.offset(dx, dy, dz);
            BlockPos floor = candidate.below();
            if (!level.isInWorldBounds(candidate)
                    || !level.getWorldBorder().isWithinBounds(candidate)
                    || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                continue;
            }

            entity.moveTo(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5,
                    entity.getYRot(), entity.getXRot());
            if (level.noCollision(entity)) {
                return candidate.immutable();
            }
        }
        return null;
    }
}
