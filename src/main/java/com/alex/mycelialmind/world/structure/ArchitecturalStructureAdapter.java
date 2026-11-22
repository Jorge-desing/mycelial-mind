package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalLightStyle;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion;
import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves the player's measured Architectural Accent into safe Minecraft BlockStates
 * and generates twisted mycelial architectural reflections scaled to the player's proportions.
 */
public final class ArchitecturalStructureAdapter {
    public record ResolvedPalette(
            BlockState primaryWall,
            BlockState structuralPillar,
            BlockState stairBase,
            BlockState slab,
            BlockState wallOrFence,
            BlockState wallFallback,
            BlockState glowBlock
    ) {
        public static ResolvedPalette defaultPalette() {
            return new ResolvedPalette(
                    Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
                    Blocks.POLISHED_BASALT.defaultBlockState(),
                    Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState(),
                    Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState(),
                    Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(),
                    Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(),
                    Blocks.SHROOMLIGHT.defaultBlockState()
            );
        }
    }

    private ArchitecturalStructureAdapter() {
    }

    public static ArchitecturalAccentSnapshot resolveAccentForLevel(@Nullable WorldGenLevel level) {
        if (level != null && level.getLevel() instanceof ServerLevel serverLevel) {
            ArchitecturalAccentSnapshot local = HiveMindSavedData.get(serverLevel).getArchitecturalAccent();
            if (local.hasCustomAccent()) {
                return local;
            }
            if (serverLevel.getServer() != null) {
                ArchitecturalAccentSnapshot overworld = HiveMindSavedData.get(serverLevel.getServer().overworld())
                        .getArchitecturalAccent();
                if (overworld.hasCustomAccent()) {
                    return overworld;
                }
            }
        }
        return ArchitecturalAccentSnapshot.EMPTY;
    }

    public static ResolvedPalette resolvePalette(@Nullable ArchitecturalAccentSnapshot accent) {
        if (accent == null || !accent.hasCustomAccent()) {
            return ResolvedPalette.defaultPalette();
        }
        BlockState primary = resolveSolidBlock(accent.primaryBlockId(), Blocks.DEEPSLATE_BRICKS);
        String pillarId = (accent.pillarBlockId() == null || accent.pillarBlockId().isBlank())
                ? HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId(accent.primaryBlockId())
                : accent.pillarBlockId();
        BlockState pillar = resolveSolidBlock(pillarId, primary.getBlock());

        String familyPrefix = extractFamilyPrefix(accent.primaryBlockId());
        BlockState stairs = resolveTypedVariant(familyPrefix + "_stairs", StairBlock.class, Blocks.DEEPSLATE_BRICK_STAIRS);
        BlockState slab = resolveTypedVariant(familyPrefix + "_slab", SlabBlock.class, Blocks.DEEPSLATE_BRICK_SLAB);

        BlockState wallOnly = resolveTypedVariant(familyPrefix + "_wall", WallBlock.class, Blocks.DEEPSLATE_BRICK_WALL);
        BlockState wallOrFence = wallOnly;
        if (isWoodFamily(accent.primaryBlockId())) {
            Optional<Block> fence = lookupBlock(familyPrefix + "_fence");
            if (fence.isPresent() && !(fence.get() instanceof EntityBlock)) {
                wallOrFence = fence.get().defaultBlockState();
            }
        }

        BlockState glow = switch (accent.lightStyle()) {
            case SHADOW_CRYPT -> ModBlocks.SPORE_VENT.get().defaultBlockState();
            case RADIANT_CANOPY -> Blocks.SHROOMLIGHT.defaultBlockState();
            case BALANCED_GLOW -> Blocks.OCHRE_FROGLIGHT.defaultBlockState();
        };

        return new ResolvedPalette(primary, pillar, stairs, slab, wallOrFence, wallOnly, glow);
    }

    public static BlockState resolveSolidBlock(@Nullable String blockId, Block fallback) {
        Optional<Block> opt = lookupBlock(blockId);
        if (opt.isPresent()) {
            Block b = opt.get();
            if (b != Blocks.AIR && !(b instanceof EntityBlock) && b.defaultBlockState().blocksMotion()) {
                return b.defaultBlockState();
            }
        }
        return fallback.defaultBlockState();
    }

    private static <T extends Block> BlockState resolveTypedVariant(String blockId, Class<T> expectedType, Block fallback) {
        Optional<Block> opt = lookupBlock(blockId);
        if (opt.isPresent() && expectedType.isInstance(opt.get())) {
            return opt.get().defaultBlockState();
        }
        return fallback.defaultBlockState();
    }

    private static Optional<Block> lookupBlock(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation rl = ResourceLocation.tryParse(rawId.trim().toLowerCase());
        if (rl == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.BLOCK.getOptional(rl);
    }

    private static boolean isWoodFamily(@Nullable String blockId) {
        if (blockId == null) {
            return false;
        }
        String lower = blockId.toLowerCase();
        return lower.endsWith("_planks") || lower.endsWith("_log") || lower.endsWith("_wood") || lower.endsWith("_stem");
    }

    private static String extractFamilyPrefix(@Nullable String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return "minecraft:deepslate_brick";
        }
        String lower = blockId.trim().toLowerCase();
        String ns = "minecraft";
        String path = lower;
        int colon = lower.indexOf(':');
        if (colon >= 0) {
            ns = lower.substring(0, colon);
            path = lower.substring(colon + 1);
        }
        for (String suffix : new String[]{"_planks", "_logs", "_log", "_wood", "_stem", "_bricks", "_tiles", "_block"}) {
            if (path.endsWith(suffix) && path.length() > suffix.length()) {
                String stem = path.substring(0, path.length() - suffix.length());
                if ("_bricks".equals(suffix)) {
                    stem = stem + "_brick";
                } else if ("_tiles".equals(suffix)) {
                    stem = stem + "_tile";
                }
                return ns + ":" + stem;
            }
        }
        return ns + ":" + path;
    }

    /**
     * Generates a strictly 6-connected twisted mycelial reflection structure using the player's
     * architectural palette, proportion, and lighting density.
     */
    public static void placeTwistedReflectionStructure(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos origin,
            ArchitecturalAccentSnapshot accent
    ) {
        ResolvedPalette palette = resolvePalette(accent);
        int[] dims = HyphalAdaptiveArchitectureEngine.computeScaledDimensions(
                accent.typicalRoomSpan(), accent.heightToWidthRatio());
        int halfWidth = dims[0];
        int height = dims[1];
        ArchitecturalProportion proportion = accent.proportion();
        ArchitecturalLightStyle lightStyle = accent.lightStyle();

        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState chitinPillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState mycelialStone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        BlockState tendrils = ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState();
        BlockState sporeVent = ModBlocks.SPORE_VENT.get().defaultBlockState();

        // 1. Grounded foundation floor (dy = 0)
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                boolean border = Math.abs(dx) == halfWidth || Math.abs(dz) == halfWidth;
                BlockState floorBlock = border
                        ? palette.primaryWall()
                        : (((Math.abs(dx) + Math.abs(dz)) % 3 == 0) ? mycelium : palette.primaryWall());
                setIfInside(level, box, origin.offset(dx, 0, dz), floorBlock);
            }
        }

        // 2. Proportional body
        int bodyHeight = switch (proportion) {
            case TOWER_SPIRE -> Math.max(7, Math.min(12, height));
            case BUNKER_VAULT -> Math.max(4, Math.min(6, height));
            case HALL_GALLERY -> Math.max(5, Math.min(8, height));
        };

        for (int dy = 1; dy <= bodyHeight; dy++) {
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                    boolean isCorner = Math.abs(dx) == halfWidth && Math.abs(dz) == halfWidth;
                    boolean onWall = Math.abs(dx) == halfWidth || Math.abs(dz) == halfWidth;
                    if (!onWall) {
                        setIfInside(level, box, origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    // South entrance doorway
                    if (dz == halfWidth && Math.abs(dx) <= 1 && dy <= 3) {
                        setIfInside(level, box, origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    if (isCorner) {
                        setIfInside(level, box, origin.offset(dx, dy, dz),
                                (dy % 4 == 0) ? chitinPillar : palette.structuralPillar());
                    } else if (dy == bodyHeight || (dy % 3 == 0 && (dx == 0 || dz == 0))) {
                        setIfInside(level, box, origin.offset(dx, dy, dz),
                                (lightStyle == ArchitecturalLightStyle.RADIANT_CANOPY && dy == bodyHeight && (dx == 0 || dz == 0))
                                        ? palette.glowBlock()
                                        : palette.primaryWall());
                    } else if (proportion == ArchitecturalProportion.HALL_GALLERY && dy >= 2 && dy <= 3
                            && (Math.abs(dx) == 1 || Math.abs(dz) == 1)) {
                        setIfInside(level, box, origin.offset(dx, dy, dz), ModBlocks.BIO_CHITIN_LATTICE.get().defaultBlockState());
                    } else {
                        setIfInside(level, box, origin.offset(dx, dy, dz),
                                ((Math.abs(dx) + Math.abs(dz) + dy) % 5 == 0) ? archBrick : palette.primaryWall());
                    }
                }
            }
        }

        // 3. Fungal roof / cap crown + lighting + coiling root
        int roofBaseY = bodyHeight + 1;
        int capRadius = halfWidth + 1;
        for (int dx = -capRadius; dx <= capRadius; dx++) {
            for (int dz = -capRadius; dz <= capRadius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > capRadius + 0.35) {
                    continue;
                }
                boolean outerRim = Math.abs(dx) == capRadius || Math.abs(dz) == capRadius;
                BlockState roofBlock;
                if (dx == 0 && dz == 0) {
                    roofBlock = lightStyle == ArchitecturalLightStyle.SHADOW_CRYPT ? archBrick : palette.glowBlock();
                } else if (outerRim) {
                    roofBlock = (Math.abs(dx + dz) % 2 == 0) ? palette.slab() : mycelialStone;
                } else {
                    roofBlock = ((Math.abs(dx) + Math.abs(dz)) % 3 == 0) ? archBrick : palette.primaryWall();
                }
                setIfInside(level, box, origin.offset(dx, roofBaseY, dz), roofBlock);
                if (outerRim && !(roofBlock.getBlock() instanceof SlabBlock)
                        && (Math.abs(dx) + Math.abs(dz)) % 2 == 0 && dist <= capRadius) {
                    BlockPos belowRim = origin.offset(dx, roofBaseY - 1, dz);
                    if (box.isInside(belowRim) && level.isEmptyBlock(belowRim)) {
                        level.setBlock(belowRim, tendrils, 2);
                    }
                }
            }
        }

        // Upper spire cap layer
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setIfInside(level, box, origin.offset(dx, roofBaseY + 1, dz),
                        (dx == 0 && dz == 0) ? amberVein : mycelialStone);
            }
        }

        // 4. Coiling 6-connected mycelial growth climbing the facade
        List<BlockPos> coilWaypoints = new ArrayList<>();
        coilWaypoints.add(origin.offset(halfWidth, 0, -halfWidth));
        coilWaypoints.add(origin.offset(halfWidth, bodyHeight / 2, 0));
        coilWaypoints.add(origin.offset(halfWidth, bodyHeight, halfWidth));
        coilWaypoints.add(origin.offset(0, roofBaseY, halfWidth));
        VoxelStructureOps.placeFaceConnectedPath(level, box, coilWaypoints,
                step -> (step % 3 == 0) ? amberVein : chitinPillar);

        if (proportion == ArchitecturalProportion.BUNKER_VAULT) {
            setIfInside(level, box, origin.offset(-halfWidth + 1, 1, -halfWidth + 1), sporeVent);
            setIfInside(level, box, origin.offset(halfWidth - 1, 1, -halfWidth + 1), sporeVent);
        } else if (lightStyle == ArchitecturalLightStyle.RADIANT_CANOPY) {
            VoxelStructureOps.placeAnchoredChain(level, box, origin.offset(0, roofBaseY, 0),
                    1, archBrick, palette.glowBlock());
        }
    }

    /**
     * Builds a strictly 6-connected architectural wing spire inside MatriarchCitadelPiece,
     * reflecting the player's primary block palette, pillar ribs, H/W ratio, and light density.
     */
    public static void placeCitadelReflectionSpire(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos anchor,
            ArchitecturalAccentSnapshot accent
    ) {
        ResolvedPalette palette = resolvePalette(accent);
        ArchitecturalProportion proportion = accent.proportion();
        ArchitecturalLightStyle lightStyle = accent.lightStyle();
        int spireHeight = switch (proportion) {
            case TOWER_SPIRE -> Mth.clamp((int) Math.round(6.0 + Math.max(0.0, accent.heightToWidthRatio() - 1.35) * 2.5), 6, 9);
            case BUNKER_VAULT -> 4;
            case HALL_GALLERY -> 5;
        };

        BlockState chitinPillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();

        // Ground plinth at dy = 0 (face-adjacent to Citadel floor at anchor.below())
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean isCorner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                BlockState base = isCorner ? palette.structuralPillar()
                        : ((dx == 0 && dz == 0) ? mycelium : palette.primaryWall());
                setIfInside(level, box, anchor.offset(dx, 0, dz), base);
            }
        }

        // Central core column + corner ribs rising cleanly from dy = 0
        for (int dy = 1; dy <= spireHeight; dy++) {
            setIfInside(level, box, anchor.above(dy),
                    (dy == spireHeight && lightStyle != ArchitecturalLightStyle.SHADOW_CRYPT)
                            ? palette.glowBlock()
                            : palette.primaryWall());
            if (dy <= spireHeight - 1) {
                for (int sx : new int[]{-1, 1}) {
                    for (int sz : new int[]{-1, 1}) {
                        setIfInside(level, box, anchor.offset(sx, dy, sz),
                                (dy % 3 == 0) ? chitinPillar : palette.structuralPillar());
                    }
                }
            }
            if (dy == spireHeight - 1) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    setIfInside(level, box, anchor.relative(d).above(dy),
                            lightStyle == ArchitecturalLightStyle.RADIANT_CANOPY ? palette.glowBlock() : archBrick);
                }
            }
        }
        setIfInside(level, box, anchor.above(spireHeight + 1), amberVein);
    }

    private static void setIfInside(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }
}
