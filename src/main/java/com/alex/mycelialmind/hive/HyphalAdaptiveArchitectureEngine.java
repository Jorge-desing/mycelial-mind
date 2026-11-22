package com.alex.mycelialmind.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure, deterministic algorithms for:
 * 1) Coarse chunk-level Hyphal Route Network density, decay, and vein interpolation,
 * 2) Player Architectural Accent telemetry (palette, scale, H/W ratio, light density),
 * 3) Blind-Spot live outbreak suitability, photophobic stalling, and voxel blueprints.
 */
public final class HyphalAdaptiveArchitectureEngine {
    public static final double MAX_NETWORK_DENSITY = 100.0;
    public static final double VANILLA_CALM_THRESHOLD = 20.0;
    public static final double VEIN_GROWTH_THRESHOLD = 35.0;
    public static final double HIGH_DENSITY_VEIN_THRESHOLD = 70.0;
    public static final double AMBUSH_DENSITY_THRESHOLD = 60.0;
    public static final int MAX_TRACKED_ROUTE_CHUNKS = 128;
    public static final int MAX_WAYPOINTS_PER_CHUNK = 16;
    public static final int MAX_VEINS_PER_CHUNK = 32;
    public static final long AMBUSH_COOLDOWN_TICKS = 1200L;

    public static final int BLIND_SPOT_MAX_BLOCK_LIGHT = 4;
    public static final int PHOTOPHOBIC_STALL_LIGHT = 9;
    public static final int PHOTOPHOBIC_WITHER_STRIKES = 4;
    public static final int MAX_ACTIVE_BLIND_SPOTS = 6;

    public enum ArchitecturalProportion {
        TOWER_SPIRE("tower_spire"),
        HALL_GALLERY("hall_gallery"),
        BUNKER_VAULT("bunker_vault");

        private final String id;

        ArchitecturalProportion(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static ArchitecturalProportion fromId(@Nullable String raw) {
            if (raw != null) {
                for (ArchitecturalProportion p : values()) {
                    if (p.id.equalsIgnoreCase(raw) || p.name().equalsIgnoreCase(raw)) {
                        return p;
                    }
                }
            }
            return HALL_GALLERY;
        }
    }

    public enum ArchitecturalLightStyle {
        SHADOW_CRYPT("shadow_crypt"),
        BALANCED_GLOW("balanced_glow"),
        RADIANT_CANOPY("radiant_canopy");

        private final String id;

        ArchitecturalLightStyle(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public record ArchitecturalAccentSnapshot(
            String primaryBlockId,
            String pillarBlockId,
            double typicalRoomSpan,
            double heightToWidthRatio,
            double lightDensity,
            int sampledBlocksCount
    ) {
        public static final ArchitecturalAccentSnapshot EMPTY = new ArchitecturalAccentSnapshot(
                "", "", 5.0, 1.0, 0.5, 0
        );

        public boolean hasCustomAccent() {
            return sampledBlocksCount > 0 && primaryBlockId != null && !primaryBlockId.isBlank();
        }

        public ArchitecturalProportion proportion() {
            return classifyProportion(heightToWidthRatio);
        }

        public ArchitecturalLightStyle lightStyle() {
            return classifyLightStyle(lightDensity);
        }
    }

    public enum BlueprintVoxelRole {
        FOUNDATION_MYCELIUM,
        PLAYER_PRIMARY_WALL,
        PLAYER_PILLAR_RIB,
        FUNGAL_CAP,
        LUMINESCENT_VEIN,
        SPORE_VENT_MOUTH,
        HANGING_TENDRIL
    }

    public record BlueprintVoxel(BlockPos pos, BlueprintVoxelRole role) {
    }

    private HyphalAdaptiveArchitectureEngine() {
    }

    public static double computeRouteDensityDelta(
            double currentDensity,
            int routeTraversalCount,
            boolean inProtectedZone,
            boolean nearSymbioticNode,
            boolean isCrouching
    ) {
        if (inProtectedZone || nearSymbioticNode) {
            return -4.0;
        }
        double safeCurrent = Double.isFinite(currentDensity)
                ? Mth.clamp(currentDensity, 0.0, MAX_NETWORK_DENSITY)
                : 0.0;
        double baseGain = isCrouching ? 1.0 : 2.5;
        double repetitionMult = 1.0 + Math.min(1.5, Math.max(0, routeTraversalCount) * 0.25);
        double delta = baseGain * repetitionMult;
        if (safeCurrent >= 85.0) {
            delta *= 0.5;
        }
        return Mth.clamp(safeCurrent + delta, 0.0, MAX_NETWORK_DENSITY) - safeCurrent;
    }

    public static double computeDecayedRouteDensity(
            double currentDensity,
            long ticksSinceLastVisit,
            double baseDecayAmount
    ) {
        if (!Double.isFinite(currentDensity) || currentDensity <= 0.0) {
            return 0.0;
        }
        double clampedCurrent = Mth.clamp(currentDensity, 0.0, MAX_NETWORK_DENSITY);
        if (!Double.isFinite(baseDecayAmount) || baseDecayAmount <= 0.0) {
            return clampedCurrent;
        }
        long safeTicks = Math.max(0L, ticksSinceLastVisit);
        double inactivityFactor = safeTicks >= 1200L
                ? (1.0 + Math.min(2.0, (safeTicks - 1200L) / 2400.0))
                : 0.5;
        double next = Math.max(0.0, clampedCurrent - baseDecayAmount * inactivityFactor);
        return next < 0.5 ? 0.0 : next;
    }

    public static boolean isOutsideHyphalNetwork(double chunkDensity) {
        return !Double.isFinite(chunkDensity) || chunkDensity < VANILLA_CALM_THRESHOLD;
    }

    public static boolean shouldSproutRouteVein(double chunkDensity, boolean inProtectedZone) {
        return !inProtectedZone && Double.isFinite(chunkDensity) && chunkDensity >= VEIN_GROWTH_THRESHOLD;
    }

    public static int computeVeinStrength(double chunkDensity) {
        if (!Double.isFinite(chunkDensity) || chunkDensity < 50.0) {
            return 0;
        }
        if (chunkDensity < 65.0) {
            return 1;
        }
        if (chunkDensity < 85.0) {
            return 2;
        }
        return 3;
    }

    public static boolean shouldTriggerTacticalAmbush(
            double chunkDensity,
            boolean nearVeinBlock,
            boolean inProtectedZone,
            long ticksSinceLastAmbush
    ) {
        return !inProtectedZone
                && nearVeinBlock
                && Double.isFinite(chunkDensity)
                && chunkDensity >= AMBUSH_DENSITY_THRESHOLD
                && ticksSinceLastAmbush >= AMBUSH_COOLDOWN_TICKS;
    }

    public static double computeOverworldActivityMultiplier(
            double chunkDensity,
            boolean nearEnemyNode,
            boolean fungalTideActive
    ) {
        if (fungalTideActive || nearEnemyNode) {
            return 1.0;
        }
        if (!Double.isFinite(chunkDensity) || chunkDensity < VANILLA_CALM_THRESHOLD) {
            return 0.08;
        }
        if (chunkDensity < VEIN_GROWTH_THRESHOLD) {
            return 0.45;
        }
        if (chunkDensity >= AMBUSH_DENSITY_THRESHOLD) {
            return 1.35;
        }
        return 1.0;
    }

    public static boolean shouldAllowOverworldNaturalSpawn(
            double chunkDensity,
            boolean onMycelialSurface,
            boolean nearEnemyNode,
            boolean fungalTideActive,
            double randomRoll
    ) {
        if (onMycelialSurface || nearEnemyNode || fungalTideActive) {
            return true;
        }
        double mult = computeOverworldActivityMultiplier(chunkDensity, false, false);
        if (mult >= 1.0) {
            return true;
        }
        return Double.isFinite(randomRoll) && randomRoll >= 0.0 && randomRoll < mult;
    }

    public static boolean shouldDeferOverworldRaidOutsideNetwork(
            double chunkDensity,
            boolean nearEnemyNode,
            boolean fungalTideActive,
            boolean hiveHasEstablishedRoutes
    ) {
        return !fungalTideActive
                && !nearEnemyNode
                && hiveHasEstablishedRoutes
                && isOutsideHyphalNetwork(chunkDensity);
    }

    public static double computeVeinSeverDensityDrop(
            boolean isSpecializedMycelialTool,
            boolean isCuttingTool,
            int severedBlocksCount
    ) {
        int count = Math.max(1, Math.min(8, severedBlocksCount));
        if (isSpecializedMycelialTool) {
            return 18.0 + count * 4.5;
        }
        if (isCuttingTool) {
            return 12.0 + count * 3.0;
        }
        return 6.0 + count * 1.5;
    }

    public static List<BlockPos> supercoverLine(
            @Nullable BlockPos from,
            @Nullable BlockPos to,
            int maxSteps
    ) {
        return HyphalPhysics.supercoverLine(from, to, maxSteps);
    }

    /**
     * @deprecated Use {@link #supercoverLine(BlockPos, BlockPos, int)} for literal diagonal trails instead of L-shaped interpolation.
     */
    @Deprecated
    public static List<BlockPos> interpolateRouteVeinPositions(
            @Nullable BlockPos from,
            @Nullable BlockPos to,
            int maxSteps
    ) {
        if (from == null) {
            return List.of();
        }
        if (to == null) {
            return List.of(from.immutable());
        }
        double dxTotal = to.getX() - from.getX();
        double dzTotal = to.getZ() - from.getZ();
        if ((dxTotal * dxTotal + dzTotal * dzTotal) > 256.0D) {
            return List.of(from.immutable());
        }
        int cappedSteps = Math.max(1, maxSteps);
        List<BlockPos> result = new ArrayList<>();
        BlockPos current = from.immutable();
        result.add(current);
        int steps = 0;
        while (!current.equals(to) && steps < cappedSteps) {
            int dx = Integer.compare(to.getX(), current.getX());
            int dy = Integer.compare(to.getY(), current.getY());
            int dz = Integer.compare(to.getZ(), current.getZ());
            if (dx != 0) {
                current = current.offset(dx, 0, 0);
            } else if (dz != 0) {
                current = current.offset(0, 0, dz);
            } else if (dy != 0) {
                current = current.offset(0, dy, 0);
            }
            result.add(current);
            steps++;
        }
        return result;
    }

    public static ArchitecturalProportion classifyProportion(double heightToWidthRatio) {
        if (!Double.isFinite(heightToWidthRatio)) {
            return ArchitecturalProportion.HALL_GALLERY;
        }
        if (heightToWidthRatio >= 1.35) {
            return ArchitecturalProportion.TOWER_SPIRE;
        }
        if (heightToWidthRatio <= 0.75) {
            return ArchitecturalProportion.BUNKER_VAULT;
        }
        return ArchitecturalProportion.HALL_GALLERY;
    }

    public static ArchitecturalLightStyle classifyLightStyle(double lightDensity) {
        if (!Double.isFinite(lightDensity)) {
            return ArchitecturalLightStyle.BALANCED_GLOW;
        }
        if (lightDensity < 0.30) {
            return ArchitecturalLightStyle.SHADOW_CRYPT;
        }
        if (lightDensity >= 0.68) {
            return ArchitecturalLightStyle.RADIANT_CANOPY;
        }
        return ArchitecturalLightStyle.BALANCED_GLOW;
    }

    public static double updateRunningMetric(
            double currentAvg,
            double newSample,
            int sampleCount,
            double minVal,
            double maxVal
    ) {
        double lo = Math.min(minVal, maxVal);
        double hi = Math.max(minVal, maxVal);
        double clampedSample = Double.isFinite(newSample) ? Mth.clamp(newSample, lo, hi) : lo;
        double safeAvg = Double.isFinite(currentAvg) ? Mth.clamp(currentAvg, lo, hi) : clampedSample;
        double alpha = sampleCount <= 1 ? 1.0 : Math.max(0.25, 1.0 / Math.min(8, sampleCount));
        return Mth.clamp(safeAvg * (1.0 - alpha) + clampedSample * alpha, lo, hi);
    }

    public static int[] computeScaledDimensions(double typicalRoomSpan, double heightToWidthRatio) {
        double safeSpan = Double.isFinite(typicalRoomSpan) ? typicalRoomSpan : 5.0;
        double safeRatio = Double.isFinite(heightToWidthRatio) ? Mth.clamp(heightToWidthRatio, 0.45, 2.6) : 1.0;
        int halfWidth = Mth.clamp((int) Math.round(safeSpan * 0.45), 2, 6);
        int diameter = halfWidth * 2 + 1;
        int height = Mth.clamp((int) Math.round(diameter * safeRatio), 4, 16);
        return new int[]{halfWidth, height};
    }

    public static String resolveCompanionPillarBlockId(@Nullable String primaryBlockId) {
        if (primaryBlockId == null || primaryBlockId.isBlank()) {
            return "minecraft:polished_basalt";
        }
        String normalized = primaryBlockId.trim().toLowerCase();
        String namespace = "minecraft";
        String path = normalized;
        int colon = normalized.indexOf(':');
        if (colon >= 0) {
            namespace = normalized.substring(0, colon);
            path = normalized.substring(colon + 1);
        }
        if ("crimson_planks".equals(path)) {
            return namespace + ":crimson_stem";
        }
        if ("warped_planks".equals(path)) {
            return namespace + ":warped_stem";
        }
        if (path.endsWith("_planks") && path.length() > "_planks".length()) {
            return namespace + ":" + path.substring(0, path.length() - "_planks".length()) + "_log";
        }
        if ("stone_bricks".equals(path)) {
            return namespace + ":chiseled_stone_bricks";
        }
        if ("deepslate_bricks".equals(path)) {
            return namespace + ":deepslate_tiles";
        }
        if ("cobblestone".equals(path)) {
            return namespace + ":mossy_cobblestone";
        }
        if ("quartz_block".equals(path)) {
            return namespace + ":quartz_pillar";
        }
        if ("copper_block".equals(path)) {
            return namespace + ":cut_copper";
        }
        return namespace + ":" + path;
    }

    public static double computeCombinedLightDensity(
            int lightBlocksPlaced,
            int structuralBlocksPlaced,
            double localAmbientLight
    ) {
        double safeLocal = Double.isFinite(localAmbientLight) ? Mth.clamp(localAmbientLight, 0.0, 1.0) : 0.5;
        if (structuralBlocksPlaced <= 0) {
            return safeLocal;
        }
        double safeLights = Math.max(0, lightBlocksPlaced);
        double ratioScore = Mth.clamp((safeLights / (double) Math.max(1, structuralBlocksPlaced)) * 6.0, 0.0, 1.0);
        return Mth.clamp(ratioScore * 0.65 + safeLocal * 0.35, 0.0, 1.0);
    }

    public static double evaluateBlindSpotSuitability(
            double chunkRouteDensity,
            int blockLight,
            boolean inProtectedZone,
            boolean hasSolidFloor,
            boolean hasHeadroom,
            double distanceToNearestOutbreak
    ) {
        return evaluateBlindSpotSuitability(
                chunkRouteDensity, blockLight, inProtectedZone, hasSolidFloor, hasHeadroom, distanceToNearestOutbreak, false);
    }

    public static double evaluateBlindSpotSuitability(
            double chunkRouteDensity,
            int blockLight,
            boolean inProtectedZone,
            boolean hasSolidFloor,
            boolean hasHeadroom,
            double distanceToNearestOutbreak,
            boolean nearNeglectedCornerOrAccess
    ) {
        if (inProtectedZone
                || !hasSolidFloor
                || !hasHeadroom
                || blockLight > BLIND_SPOT_MAX_BLOCK_LIGHT
                || !Double.isFinite(chunkRouteDensity)
                || chunkRouteDensity < 25.0
                || !Double.isFinite(distanceToNearestOutbreak)
                || distanceToNearestOutbreak < 16.0) {
            return 0.0;
        }
        double darknessScore = (BLIND_SPOT_MAX_BLOCK_LIGHT + 1 - Math.max(0, blockLight))
                / (double) (BLIND_SPOT_MAX_BLOCK_LIGHT + 1);
        double densityScore = Mth.clamp(chunkRouteDensity / 100.0, 0.25, 1.0);
        double neglectedBonus = nearNeglectedCornerOrAccess ? 0.25 : 0.0;
        return Mth.clamp(darknessScore * 0.6 + densityScore * 0.4 + neglectedBonus, 0.0, 1.0);
    }

    public static long computeGrowthStepDelayTicks(
            @Nullable HiveProgressionTier tier,
            boolean fungalTideActive,
            int currentBlockLight
    ) {
        if (currentBlockLight >= PHOTOPHOBIC_STALL_LIGHT) {
            return -1L;
        }
        HiveProgressionTier effective = tier != null ? tier : HiveProgressionTier.DORMANT;
        double ticks = switch (effective) {
            case DORMANT -> 1800.0;
            case AWAKENED -> 1500.0;
            case ADAPTIVE -> 1200.0;
            case OVERMIND -> 900.0;
        };
        if (fungalTideActive) {
            ticks *= 0.5;
        }
        if (currentBlockLight >= 5) {
            ticks *= 1.5;
        }
        return Math.max(40L, Math.round(ticks));
    }

    public static List<BlueprintVoxel> generateBlindSpotBlueprint(
            BlockPos coreOrigin,
            @Nullable ArchitecturalProportion proportion,
            double typicalRoomSpan,
            double heightToWidthRatio,
            double lightDensity
    ) {
        ArchitecturalProportion safeProp = proportion != null ? proportion : ArchitecturalProportion.HALL_GALLERY;
        double safeSpan = Double.isFinite(typicalRoomSpan) ? typicalRoomSpan : 5.0;
        double safeRatio = Double.isFinite(heightToWidthRatio) ? heightToWidthRatio : 1.0;
        double safeLight = Double.isFinite(lightDensity) ? Mth.clamp(lightDensity, 0.0, 1.0) : 0.5;

        int r;
        int height;
        switch (safeProp) {
            case TOWER_SPIRE -> {
                r = safeSpan >= 6.5 ? 2 : 1;
                height = Mth.clamp((int) Math.round(5.0 + Math.max(0.0, safeRatio - 1.35) * 3.0), 6, 8);
            }
            case BUNKER_VAULT -> {
                r = 2;
                height = 3;
            }
            default -> {
                r = 2;
                height = Mth.clamp((int) Math.round(4.0 + (safeRatio - 0.75) * 1.5), 4, 5);
            }
        }

        List<BlueprintVoxel> voxels = new ArrayList<>();
        Set<BlockPos> seen = new LinkedHashSet<>();

        // Phase 1: 6-connected foundation pad beneath the core (coreOrigin itself stays reserved)
        addVoxel(voxels, seen, coreOrigin.below(), BlueprintVoxelRole.FOUNDATION_MYCELIUM);
        for (int[] card : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            addVoxel(voxels, seen, coreOrigin.offset(card[0], -1, card[1]), BlueprintVoxelRole.FOUNDATION_MYCELIUM);
        }
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                addVoxel(voxels, seen, coreOrigin.offset(sx, -1, sz), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
            }
        }
        if (r >= 2) {
            for (int[] arm : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
                addVoxel(voxels, seen, coreOrigin.offset(arm[0], -1, arm[1]), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
            }
            for (int sx : new int[]{-2, 2}) {
                for (int sz : new int[]{-1, 1}) {
                    addVoxel(voxels, seen, coreOrigin.offset(sx, -1, sz), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
                    addVoxel(voxels, seen, coreOrigin.offset(sx, -1, sz * 2), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
                }
            }
        }

        // Phase 2: 4 corner pillars (leaving cardinal faces at ground height open to strike the core)
        for (int dy = 0; dy < height; dy++) {
            for (int sx : new int[]{-r, r}) {
                for (int sz : new int[]{-r, r}) {
                    addVoxel(voxels, seen, coreOrigin.offset(sx, dy, sz), BlueprintVoxelRole.PLAYER_PILLAR_RIB);
                }
            }
            if (safeProp == ArchitecturalProportion.BUNKER_VAULT && dy == 1) {
                for (int[] card : new int[][]{{r, 0}, {-r, 0}, {0, r}, {0, -r}}) {
                    addVoxel(voxels, seen, coreOrigin.offset(card[0], dy, card[1]), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
                }
            }
        }

        // Upper arch perimeter ring connecting the 4 pillars at dy = height - 1
        int ringY = height - 1;
        for (int d = -r + 1; d <= r - 1; d++) {
            addVoxel(voxels, seen, coreOrigin.offset(d, ringY, -r), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
            addVoxel(voxels, seen, coreOrigin.offset(d, ringY, r), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
            addVoxel(voxels, seen, coreOrigin.offset(-r, ringY, d), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
            addVoxel(voxels, seen, coreOrigin.offset(r, ringY, d), BlueprintVoxelRole.PLAYER_PRIMARY_WALL);
        }

        // Phase 3: Fungal cap crown, luminescent veins scaled by lightDensity, base vents & hanging tendrils
        ArchitecturalLightStyle lightStyle = classifyLightStyle(safeLight);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                boolean isCorner = Math.abs(dx) == r && Math.abs(dz) == r;
                boolean isCenter = dx == 0 && dz == 0;
                BlueprintVoxelRole capRole = BlueprintVoxelRole.FUNGAL_CAP;
                if (lightStyle == ArchitecturalLightStyle.RADIANT_CANOPY && (isCorner || isCenter)) {
                    capRole = BlueprintVoxelRole.LUMINESCENT_VEIN;
                } else if (lightStyle == ArchitecturalLightStyle.BALANCED_GLOW && isCenter) {
                    capRole = BlueprintVoxelRole.LUMINESCENT_VEIN;
                }
                addVoxel(voxels, seen, coreOrigin.offset(dx, height, dz), capRole);
            }
        }
        // Apex spire block above center of cap
        addVoxel(voxels, seen, coreOrigin.offset(0, height + 1, 0),
                lightStyle == ArchitecturalLightStyle.SHADOW_CRYPT
                        ? BlueprintVoxelRole.FUNGAL_CAP
                        : BlueprintVoxelRole.LUMINESCENT_VEIN);

        // Base flank spore vents sitting on the foundation arms
        addVoxel(voxels, seen, coreOrigin.offset(r, 0, 0), BlueprintVoxelRole.SPORE_VENT_MOUTH);
        if (r >= 2) {
            addVoxel(voxels, seen, coreOrigin.offset(-r, 0, 0), BlueprintVoxelRole.SPORE_VENT_MOUTH);
        }

        // Hanging tendrils directly underneath solid cap blocks
        addVoxel(voxels, seen, coreOrigin.offset(0, height - 1, 0), BlueprintVoxelRole.HANGING_TENDRIL);
        if (r >= 2) {
            addVoxel(voxels, seen, coreOrigin.offset(1, height - 1, 0), BlueprintVoxelRole.HANGING_TENDRIL);
            addVoxel(voxels, seen, coreOrigin.offset(-1, height - 1, 0), BlueprintVoxelRole.HANGING_TENDRIL);
        }

        return voxels;
    }

    private static void addVoxel(List<BlueprintVoxel> voxels, Set<BlockPos> seen, BlockPos pos, BlueprintVoxelRole role) {
        BlockPos immutable = pos.immutable();
        if (seen.add(immutable)) {
            voxels.add(new BlueprintVoxel(immutable, role));
        }
    }
}
