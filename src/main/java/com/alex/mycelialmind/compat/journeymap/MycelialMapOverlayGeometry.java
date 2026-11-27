package com.alex.mycelialmind.compat.journeymap;

import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry and color helpers for JourneyMap territorial overlays.
 * Free of JourneyMap API types so it can be unit-tested headlessly.
 */
public final class MycelialMapOverlayGeometry {
    public static final int DEFAULT_PYLON_SEGMENTS = 16;

    private MycelialMapOverlayGeometry() {
    }

    /**
     * Builds an approximate circle polygon around {@code center} with the given radius and segment count.
     */
    public static List<BlockPos> buildCirclePolygon(BlockPos center, int radius, int segments) {
        if (center == null) {
            return List.of();
        }
        int safeRadius = Math.max(1, radius);
        int safeSegments = Math.max(4, segments);
        List<BlockPos> points = new ArrayList<>(safeSegments);
        int y = center.getY();
        for (int i = 0; i < safeSegments; i++) {
            double angle = (2.0 * Math.PI * i) / safeSegments;
            int x = center.getX() + (int) Math.round(Math.cos(angle) * safeRadius);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * safeRadius);
            points.add(new BlockPos(x, y, z));
        }
        return points;
    }

    /**
     * Builds a compact diamond marker polygon around a Synaptic Node position.
     */
    public static List<BlockPos> buildNodeDiamondPolygon(BlockPos center, int halfSize) {
        if (center == null) {
            return List.of();
        }
        int r = Math.max(1, halfSize);
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        return List.of(
                new BlockPos(x, y, z - r),
                new BlockPos(x + r, y, z),
                new BlockPos(x, y, z + r),
                new BlockPos(x - r, y, z)
        );
    }

    /**
     * Builds the 4-corner polygon covering a Minecraft chunk ({@code 16x16} blocks).
     */
    public static List<BlockPos> buildChunkRectPolygon(int chunkX, int chunkZ, int y) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        return List.of(
                new BlockPos(minX, y, minZ),
                new BlockPos(maxX, y, minZ),
                new BlockPos(maxX, y, maxZ),
                new BlockPos(minX, y, maxZ)
        );
    }

    public static int resolvePylonColor(boolean smothered) {
        return smothered ? 0xE67E22 : 0x26D0CE;
    }

    public static int resolveNodeColor(SynapticNodeType type) {
        if (type == null) {
            return 0x9B59B6;
        }
        return switch (type) {
            case RELAY -> 0x9B59B6;
            case INCUBATOR -> 0xE74C3C;
            case SENTINEL -> 0xF1C40F;
            case SYMBIOTIC -> 0x2ECC71;
        };
    }

    public static int resolveInflamedChunkColor(int stageId) {
        return stageId >= 2 ? 0xC0392B : 0xD35400;
    }
}
