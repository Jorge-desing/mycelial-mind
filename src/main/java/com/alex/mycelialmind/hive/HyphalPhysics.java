package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.config.MycelialConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, world-independent physics and topology primitives for literal-trail Hyphal Veins:
 * 3D Bresenham/supercover interpolation (preserving diagonal steps), root-node thresholding,
 * 0-15 nutrient attenuation, traffic-based thickening, and bitmask encoding for diagonals/faces.
 */
public final class HyphalPhysics {
    public static final int DEFAULT_ROOT_TRAVERSAL_THRESHOLD = 5;
    public static final int DEFAULT_GROWTH_INTERVAL_TICKS = 10;
    public static final int WITHER_INTERVAL_TICKS = 20;
    public static final int MAX_NUTRIENT = 15;
    public static final int CHARGED_NUTRIENT_THRESHOLD = 8;
    public static final int DEFAULT_MAX_TRAIL_POINTS_PER_CHUNK = 64;
    public static final int MAX_TRAVERSAL_ENTRIES_PER_CHUNK = 256;
    public static final int MAX_SUPERCOVER_STEPS = 16;

    // Diagonal connection bitmask (horizontal plane: X/Z)
    public static final byte DIAG_NE = 1 << 0; // +X, -Z
    public static final byte DIAG_NW = 1 << 1; // -X, -Z
    public static final byte DIAG_SE = 1 << 2; // +X, +Z
    public static final byte DIAG_SW = 1 << 3; // -X, +Z
    public static final byte ALL_DIAGONALS_MASK = DIAG_NE | DIAG_NW | DIAG_SE | DIAG_SW;

    // Attached surface bitmask (direction from vein block toward the supporting solid face)
    public static final byte FACE_DOWN = 1 << 0;
    public static final byte FACE_NORTH = 1 << 1;
    public static final byte FACE_EAST = 1 << 2;
    public static final byte FACE_SOUTH = 1 << 3;
    public static final byte FACE_WEST = 1 << 4;
    public static final byte FACE_UP = 1 << 5;
    public static final byte ALL_FACES_MASK = FACE_DOWN | FACE_NORTH | FACE_EAST | FACE_SOUTH | FACE_WEST | FACE_UP;

    private HyphalPhysics() {
    }

    public static int getRootTraversalThreshold() {
        return MycelialConfig.SPEC.isLoaded()
                ? MycelialConfig.ROOT_TRAVERSAL_THRESHOLD.get()
                : DEFAULT_ROOT_TRAVERSAL_THRESHOLD;
    }

    public static int getGrowthIntervalTicks() {
        return MycelialConfig.SPEC.isLoaded()
                ? MycelialConfig.GROWTH_INTERVAL_TICKS.get()
                : DEFAULT_GROWTH_INTERVAL_TICKS;
    }

    public static int getMaxTrailPointsPerChunk() {
        return MycelialConfig.SPEC.isLoaded()
                ? MycelialConfig.MAX_TRAIL_POINTS_PER_CHUNK.get()
                : DEFAULT_MAX_TRAIL_POINTS_PER_CHUNK;
    }

    /**
     * Computes a 3D Bresenham line between {@code a} and {@code b} without gaps (26-connected),
     * taking diagonal steps directly instead of L-shaped cardinal detours.
     * Distances beyond 16 blocks horizontally or vertically are treated as teleports and not interpolated.
     */
    public static List<BlockPos> supercoverLine(@Nullable BlockPos a, @Nullable BlockPos b, int maxSteps) {
        if (a == null) {
            return List.of();
        }
        if (b == null) {
            return List.of(a.immutable());
        }
        int dxRaw = b.getX() - a.getX();
        int dyRaw = b.getY() - a.getY();
        int dzRaw = b.getZ() - a.getZ();
        if ((dxRaw * dxRaw + dzRaw * dzRaw) > 256 || Math.abs(dyRaw) > 16) {
            return List.of(a.immutable());
        }

        int cappedSteps = Math.max(1, maxSteps);
        List<BlockPos> result = new ArrayList<>();
        int x = a.getX();
        int y = a.getY();
        int z = a.getZ();
        result.add(new BlockPos(x, y, z));
        if (dxRaw == 0 && dyRaw == 0 && dzRaw == 0) {
            return result;
        }

        int dx = Math.abs(dxRaw);
        int dy = Math.abs(dyRaw);
        int dz = Math.abs(dzRaw);
        int sx = Integer.compare(dxRaw, 0);
        int sy = Integer.compare(dyRaw, 0);
        int sz = Integer.compare(dzRaw, 0);

        int steps = 0;
        if (dx >= dy && dx >= dz) {
            int p1 = 2 * dy - dx;
            int p2 = 2 * dz - dx;
            while (x != b.getX() && steps < cappedSteps) {
                x += sx;
                if (p1 >= 0) {
                    y += sy;
                    p1 -= 2 * dx;
                }
                if (p2 >= 0) {
                    z += sz;
                    p2 -= 2 * dx;
                }
                p1 += 2 * dy;
                p2 += 2 * dz;
                result.add(new BlockPos(x, y, z));
                steps++;
            }
        } else if (dy >= dx && dy >= dz) {
            int p1 = 2 * dx - dy;
            int p2 = 2 * dz - dy;
            while (y != b.getY() && steps < cappedSteps) {
                y += sy;
                if (p1 >= 0) {
                    x += sx;
                    p1 -= 2 * dy;
                }
                if (p2 >= 0) {
                    z += sz;
                    p2 -= 2 * dy;
                }
                p1 += 2 * dx;
                p2 += 2 * dz;
                result.add(new BlockPos(x, y, z));
                steps++;
            }
        } else {
            int p1 = 2 * dy - dz;
            int p2 = 2 * dx - dz;
            while (z != b.getZ() && steps < cappedSteps) {
                z += sz;
                if (p1 >= 0) {
                    y += sy;
                    p1 -= 2 * dz;
                }
                if (p2 >= 0) {
                    x += sx;
                    p2 -= 2 * dz;
                }
                p1 += 2 * dy;
                p2 += 2 * dx;
                result.add(new BlockPos(x, y, z));
                steps++;
            }
        }
        return result;
    }

    public static boolean isRootCandidate(int traversals) {
        return isRootCandidate(traversals, getRootTraversalThreshold());
    }

    public static boolean isRootCandidate(int traversals, int threshold) {
        return traversals >= Math.max(1, threshold);
    }

    public static int nextNutrient(int maxNeighborNutrient, boolean isRoot) {
        if (isRoot) {
            return MAX_NUTRIENT;
        }
        int clampedNeighbor = Math.max(0, Math.min(MAX_NUTRIENT, maxNeighborNutrient));
        return Math.max(0, clampedNeighbor - 1);
    }

    public static int strengthForTraversals(int count) {
        if (count < 5) {
            return 0;
        }
        if (count < 10) {
            return 1;
        }
        if (count < 20) {
            return 2;
        }
        return 3;
    }

    public static boolean chargedForNutrient(int nutrient) {
        return nutrient >= CHARGED_NUTRIENT_THRESHOLD;
    }

    public static byte diagonalBit(int dx, int dz) {
        if (dx == 1 && dz == -1) {
            return DIAG_NE;
        }
        if (dx == -1 && dz == -1) {
            return DIAG_NW;
        }
        if (dx == 1 && dz == 1) {
            return DIAG_SE;
        }
        if (dx == -1 && dz == 1) {
            return DIAG_SW;
        }
        return 0;
    }

    public static byte oppositeDiagonalBit(int dx, int dz) {
        return diagonalBit(-dx, -dz);
    }

    public static int diagonalDx(byte bit) {
        return (bit == DIAG_NE || bit == DIAG_SE) ? 1 : ((bit == DIAG_NW || bit == DIAG_SW) ? -1 : 0);
    }

    public static int diagonalDz(byte bit) {
        return (bit == DIAG_SE || bit == DIAG_SW) ? 1 : ((bit == DIAG_NE || bit == DIAG_NW) ? -1 : 0);
    }

    public static byte faceBit(Direction attachDir) {
        return switch (attachDir) {
            case DOWN -> FACE_DOWN;
            case NORTH -> FACE_NORTH;
            case EAST -> FACE_EAST;
            case SOUTH -> FACE_SOUTH;
            case WEST -> FACE_WEST;
            case UP -> FACE_UP;
        };
    }

    public static byte effectiveFaces(byte faces) {
        return (faces & ALL_FACES_MASK) == 0 ? FACE_DOWN : (byte) (faces & ALL_FACES_MASK);
    }

    public static boolean hasFace(byte faces, byte bit) {
        return (effectiveFaces(faces) & bit) != 0;
    }
}
