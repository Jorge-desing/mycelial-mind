package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.HyphalVeinEntity;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModStructures;
import com.alex.mycelialmind.world.MycelialLootSystem;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.alex.mycelialmind.world.structure.ArchitecturalStructureAdapter.ResolvedPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * Dimensional citadel piece composed from NBT structure templates parameterized by
 * the player's Architectural Accent (palette, proportion, and lighting style).
 */
public class TwistedCitadelPiece extends StructurePiece {
    private static final int RADIUS = 24;
    private static final int HEIGHT = 20;

    public static final ResourceLocation SPIRE_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "twisted_reflection/spire_tower");
    public static final ResourceLocation GALLERY_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "twisted_reflection/gallery_hall");
    public static final ResourceLocation CLOISTER_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "twisted_reflection/sentinel_cloister");
    public static final ResourceLocation VAULT_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "twisted_reflection/vault_bunker");

    private final BlockPos center;

    public TwistedCitadelPiece(BlockPos center) {
        super(ModStructures.TWISTED_CITADEL_PIECE.get(), 0, new BoundingBox(
                center.getX() - RADIUS, center.getY() - 12, center.getZ() - RADIUS,
                center.getX() + RADIUS, center.getY() + 24, center.getZ() + RADIUS
        ));
        this.center = center.immutable();
    }

    public TwistedCitadelPiece(CompoundTag tag) {
        super(ModStructures.TWISTED_CITADEL_PIECE.get(), tag);
        this.center = new BlockPos(tag.getInt("CenterX"), tag.getInt("CenterY"), tag.getInt("CenterZ"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CenterX", center.getX());
        tag.putInt("CenterY", center.getY());
        tag.putInt("CenterZ", center.getZ());
    }

    public static ResourceLocation selectTemplateForProportion(ArchitecturalProportion proportion) {
        return switch (proportion) {
            case TOWER_SPIRE -> SPIRE_TEMPLATE;
            case BUNKER_VAULT -> VAULT_TEMPLATE;
            case HALL_GALLERY -> GALLERY_TEMPLATE;
        };
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        ArchitecturalAccentSnapshot accent = ArchitecturalStructureAdapter.resolveAccentForLevel(level);
        ResolvedPalette palette = ArchitecturalStructureAdapter.resolvePalette(accent);
        ArchitecturalProportion proportion = accent.proportion();

        ResourceLocation primaryTemplateId = selectTemplateForProportion(proportion);
        ResourceLocation wingTemplateId = proportion == ArchitecturalProportion.BUNKER_VAULT
                ? VAULT_TEMPLATE
                : SPIRE_TEMPLATE;

        StructureTemplateManager tplMgr = level.getLevel().getStructureManager();
        Optional<StructureTemplate> primaryOpt = tplMgr.get(primaryTemplateId);
        if (primaryOpt.isEmpty()) {
            ArchitecturalStructureAdapter.placeTwistedReflectionStructure(level, box, center, accent);
            return;
        }

        StructureTemplate primaryTpl = primaryOpt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setBoundingBox(box)
                .setIgnoreEntities(false)
                .addProcessor(new ArchitecturalAccentProcessor(accent));

        // 1. Place primary hub structure centered at center
        int centerHalfX = primaryTpl.getSize().getX() / 2;
        int centerHalfZ = primaryTpl.getSize().getZ() / 2;
        BlockPos primaryOrigin = center.offset(-centerHalfX, 0, -centerHalfZ);
        clearInteriorIfNeeded(level, box, primaryOrigin, primaryTpl.getSize().getX(), primaryTpl.getSize().getY(), primaryTpl.getSize().getZ());
        primaryTpl.placeInWorld(level, primaryOrigin, primaryOrigin, settings, random, 2);
        groundTemplateCorners(level, box, primaryOrigin, primaryTpl.getSize().getX(), primaryTpl.getSize().getZ(), palette);

        // 2. Place flanking wing templates (West & East, plus North Keep for TOWER_SPIRE) + grounded bridges
        Optional<StructureTemplate> wingOpt = tplMgr.get(wingTemplateId);
        if (wingOpt.isPresent()) {
            StructureTemplate wingTpl = wingOpt.get();
            int wingHalfX = wingTpl.getSize().getX() / 2;
            int wingHalfZ = wingTpl.getSize().getZ() / 2;

            // West wing centered at (-15, deltaY, 0)
            BlockPos westProbe = center.offset(-15, 0, 0);
            int westDy = probeWingDeltaY(level, westProbe);
            BlockPos westCenter = westProbe.offset(0, westDy, 0);
            BlockPos westOrigin = westCenter.offset(-wingHalfX, 0, -wingHalfZ);
            clearInteriorIfNeeded(level, box, westOrigin, wingTpl.getSize().getX(), wingTpl.getSize().getY(), wingTpl.getSize().getZ());
            wingTpl.placeInWorld(level, westOrigin, westOrigin, settings, random, 2);
            groundTemplateCorners(level, box, westOrigin, wingTpl.getSize().getX(), wingTpl.getSize().getZ(), palette);
            placeWalkwayBridgeX(level, box, center.getX() - 15 + wingHalfX - 1, center.getX() - centerHalfX + 1,
                    westCenter.getY(), center.getY(), center.getZ(), palette);

            // East wing centered at (+15, deltaY, 0): asymmetric Sentinel Cloister when available
            StructureTemplate eastTpl = tplMgr.get(CLOISTER_TEMPLATE).orElse(wingTpl);
            int eastHalfX = eastTpl.getSize().getX() / 2;
            int eastHalfZ = eastTpl.getSize().getZ() / 2;
            BlockPos eastProbe = center.offset(15, 0, 0);
            int eastDy = probeWingDeltaY(level, eastProbe);
            BlockPos eastCenter = eastProbe.offset(0, eastDy, 0);
            BlockPos eastOrigin = eastCenter.offset(-eastHalfX, 0, -eastHalfZ);
            clearInteriorIfNeeded(level, box, eastOrigin, eastTpl.getSize().getX(), eastTpl.getSize().getY(), eastTpl.getSize().getZ());
            eastTpl.placeInWorld(level, eastOrigin, eastOrigin, settings, random, 2);
            groundTemplateCorners(level, box, eastOrigin, eastTpl.getSize().getX(), eastTpl.getSize().getZ(), palette);
            placeWalkwayBridgeX(level, box, center.getX() + centerHalfX - 1, center.getX() + 15 - eastHalfX + 1,
                    center.getY(), eastCenter.getY(), center.getZ(), palette);

            if (proportion == ArchitecturalProportion.TOWER_SPIRE) {
                BlockPos northProbe = center.offset(0, 0, -15);
                int northDy = probeWingDeltaY(level, northProbe);
                BlockPos northCenter = northProbe.offset(0, northDy, 0);
                BlockPos northOrigin = northCenter.offset(-wingHalfX, 0, -wingHalfZ);
                clearInteriorIfNeeded(level, box, northOrigin, wingTpl.getSize().getX(), wingTpl.getSize().getY(), wingTpl.getSize().getZ());
                wingTpl.placeInWorld(level, northOrigin, northOrigin, settings, random, 2);
                groundTemplateCorners(level, box, northOrigin, wingTpl.getSize().getX(), wingTpl.getSize().getZ(), palette);
                placeWalkwayBridgeZ(level, box, center.getZ() - 15 + wingHalfZ - 1, center.getZ() - centerHalfZ + 1,
                        northCenter.getY(), center.getY(), center.getX(), palette);
            }
        }

        // 3. Central loot chest & Sentinel Node anchored cleanly to the floor
        BlockPos chestPos = center.offset(1, 1, 0);
        if (box.isInside(chestPos)) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                var tier = StructureProgressionPolicy.resolveTier(level);
                MycelialLootSystem.populateStructureChest(
                        level, chestPos, chest, MycelialLootSystem.SANCTUM_SPORE_VAULT, random, tier, false);
            }
        }
        BlockPos nodePos = center.offset(0, 1, -2);
        if (box.isInside(nodePos)) {
            level.setBlock(nodePos, ModBlocks.SENTINEL_NODE.get().defaultBlockState(), 2);
            if (level.getLevel() instanceof ServerLevel serverLevel) {
                PurificationZoneTracker.get(serverLevel).addNode(nodePos, SynapticNodeType.SENTINEL);
            }
        }
    }

    private static void groundTemplateCorners(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos origin,
            int sizeX,
            int sizeZ,
            ResolvedPalette palette
    ) {
        if (sizeX <= 0 || sizeZ <= 0) {
            return;
        }
        int[][] corners = {
                {0, 0},
                {sizeX - 1, 0},
                {0, sizeZ - 1},
                {sizeX - 1, sizeZ - 1},
                {sizeX / 2, 0},
                {sizeX / 2, sizeZ - 1},
                {0, sizeZ / 2},
                {sizeX - 1, sizeZ / 2},
                {sizeX / 2, sizeZ / 2}
        };
        for (int[] c : corners) {
            BlockPos baseBlock = origin.offset(c[0], 0, c[1]);
            if (!box.isInside(baseBlock) || level.getBlockState(baseBlock).isAir()) {
                continue;
            }
            for (int dy = 1; dy <= 10; dy++) {
                BlockPos supportPos = baseBlock.below(dy);
                if (!box.isInside(supportPos) || supportPos.getY() <= level.getMinBuildHeight() + 1) {
                    break;
                }
                BlockState existing = level.getBlockState(supportPos);
                if (!existing.isAir() && existing.isSolid() && existing.getFluidState().isEmpty()) {
                    break;
                }
                level.setBlock(supportPos, (dy % 2 == 0) ? palette.structuralPillar() : palette.primaryWall(), 2);
            }
        }
    }

    private static void clearInteriorIfNeeded(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos origin,
            int sx,
            int sy,
            int sz
    ) {
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    BlockPos p = origin.offset(x, y, z);
                    if (box.isInside(p) && !level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static int probeWingDeltaY(WorldGenLevel level, BlockPos wingProbe) {
        for (int step = 0; step <= 4; step++) {
            int[] candidates = step == 0 ? new int[]{0} : new int[]{-step, step};
            for (int dy : candidates) {
                BlockPos floorProbe = wingProbe.offset(0, dy - 1, 0);
                BlockPos headProbe = wingProbe.offset(0, dy, 0);
                if (!level.isOutsideBuildHeight(floorProbe)
                        && !level.isOutsideBuildHeight(headProbe)
                        && level.getFluidState(floorProbe).isEmpty()
                        && level.getBlockState(floorProbe).isSolid()
                        && level.getBlockState(headProbe).isAir()) {
                    return dy;
                }
            }
        }
        return 0;
    }

    private static void placeWalkwayBridgeX(
            WorldGenLevel level,
            BoundingBox box,
            int minX,
            int maxX,
            int minEndY,
            int maxEndY,
            int centerZ,
            ResolvedPalette palette
    ) {
        int lo = Math.min(minX, maxX);
        int hi = Math.max(minX, maxX);
        int startY = (minX <= maxX) ? minEndY : maxEndY;
        int endY = (minX <= maxX) ? maxEndY : minEndY;
        int span = Math.max(1, hi - lo);
        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        BlockPos startTop = null;
        BlockPos endTop = null;
        int prevY = startY;
        for (int x = lo; x <= hi; x++) {
            double t = (double) (x - lo) / (double) span;
            int y = (int) Math.round(startY + (endY - startY) * t);
            int stepLoY = Math.min(prevY, y);
            int stepHiY = Math.max(prevY, y);
            for (int dz = -1; dz <= 1; dz++) {
                for (int sy = stepLoY; sy <= stepHiY; sy++) {
                    BlockPos floor = new BlockPos(x, sy, centerZ + dz);
                    if (box.isInside(floor)) {
                        boolean top = sy == y;
                        level.setBlock(floor, (top && dz == 0 && Math.floorMod(x, 2) == 0) ? mycelium : palette.primaryWall(), 2);
                    }
                }
                if (dz == 0 && box.isInside(new BlockPos(x, y + 1, centerZ))) {
                    if (startTop == null) {
                        startTop = new BlockPos(x, y + 1, centerZ);
                    }
                    endTop = new BlockPos(x, y + 1, centerZ);
                }
            }
            for (int wallZ : new int[]{-1, 1}) {
                BlockPos parapet = new BlockPos(x, y + 1, centerZ + wallZ);
                if (box.isInside(parapet) && Math.floorMod(x, 2) == 0 && level.getBlockState(parapet).isAir()) {
                    level.setBlock(parapet, palette.wallOrFence(), 2);
                }
            }
            prevY = y;
        }
        if (startTop != null && endTop != null && !startTop.equals(endTop)) {
            int midX = (lo + hi) / 2;
            int midY = (startY + endY) / 2;
            anchorBridgePier(level, box, midX, midY, centerZ, palette);
            int nodes = Math.max(4, Math.min(24, (hi - lo) + 2));
            HyphalVeinEntity strand = new HyphalVeinEntity(level.getLevel(), startTop, endTop, nodes, true, 0.80F);
            strand.setStrengthFloor(3);
            level.addFreshEntity(strand);
        }
    }

    private static void placeWalkwayBridgeZ(
            WorldGenLevel level,
            BoundingBox box,
            int minZ,
            int maxZ,
            int minEndY,
            int maxEndY,
            int centerX,
            ResolvedPalette palette
    ) {
        int lo = Math.min(minZ, maxZ);
        int hi = Math.max(minZ, maxZ);
        int startY = (minZ <= maxZ) ? minEndY : maxEndY;
        int endY = (minZ <= maxZ) ? maxEndY : minEndY;
        int span = Math.max(1, hi - lo);
        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        BlockPos startTop = null;
        BlockPos endTop = null;
        int prevY = startY;
        for (int z = lo; z <= hi; z++) {
            double t = (double) (z - lo) / (double) span;
            int y = (int) Math.round(startY + (endY - startY) * t);
            int stepLoY = Math.min(prevY, y);
            int stepHiY = Math.max(prevY, y);
            for (int dx = -1; dx <= 1; dx++) {
                for (int sy = stepLoY; sy <= stepHiY; sy++) {
                    BlockPos floor = new BlockPos(centerX + dx, sy, z);
                    if (box.isInside(floor)) {
                        boolean top = sy == y;
                        level.setBlock(floor, (top && dx == 0 && Math.floorMod(z, 2) == 0) ? mycelium : palette.primaryWall(), 2);
                    }
                }
                if (dx == 0 && box.isInside(new BlockPos(centerX, y + 1, z))) {
                    if (startTop == null) {
                        startTop = new BlockPos(centerX, y + 1, z);
                    }
                    endTop = new BlockPos(centerX, y + 1, z);
                }
            }
            for (int wallX : new int[]{-1, 1}) {
                BlockPos parapet = new BlockPos(centerX + wallX, y + 1, z);
                if (box.isInside(parapet) && Math.floorMod(z, 2) == 0 && level.getBlockState(parapet).isAir()) {
                    level.setBlock(parapet, palette.wallOrFence(), 2);
                }
            }
            prevY = y;
        }
        if (startTop != null && endTop != null && !startTop.equals(endTop)) {
            int midZ = (lo + hi) / 2;
            int midY = (startY + endY) / 2;
            anchorBridgePier(level, box, centerX, midY, midZ, palette);
            int nodes = Math.max(4, Math.min(24, (hi - lo) + 2));
            HyphalVeinEntity strand = new HyphalVeinEntity(level.getLevel(), startTop, endTop, nodes, true, 0.80F);
            strand.setStrengthFloor(3);
            level.addFreshEntity(strand);
        }
    }

    private static void anchorBridgePier(
            WorldGenLevel level,
            BoundingBox box,
            int x,
            int bridgeY,
            int z,
            ResolvedPalette palette
    ) {
        BlockPos bridgeFloor = new BlockPos(x, bridgeY, z);
        if (!box.isInside(bridgeFloor) || level.getBlockState(bridgeFloor).isAir()) {
            return;
        }
        for (int dy = 1; dy <= 10; dy++) {
            BlockPos pierPos = bridgeFloor.below(dy);
            if (!box.isInside(pierPos) || pierPos.getY() <= level.getMinBuildHeight() + 1) {
                break;
            }
            BlockState existing = level.getBlockState(pierPos);
            if (!existing.isAir() && existing.isSolid() && existing.getFluidState().isEmpty()) {
                break;
            }
            level.setBlock(pierPos, (dy % 2 == 0) ? palette.structuralPillar() : palette.primaryWall(), 2);
        }
    }
}
