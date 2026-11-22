package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.HyphalVeinBlock;
import com.alex.mycelialmind.block.entity.HyphalVeinBlockEntity;
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
    public static final ResourceLocation VAULT_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "twisted_reflection/vault_bunker");

    private final BlockPos center;

    public TwistedCitadelPiece(BlockPos center) {
        super(ModStructures.TWISTED_CITADEL_PIECE.get(), 0, new BoundingBox(
                center.getX() - RADIUS, center.getY() - 4, center.getZ() - RADIUS,
                center.getX() + RADIUS, center.getY() + HEIGHT, center.getZ() + RADIUS
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

        // 2. Place flanking wing templates (West & East, plus North Keep for TOWER_SPIRE) + grounded bridges
        Optional<StructureTemplate> wingOpt = tplMgr.get(wingTemplateId);
        if (wingOpt.isPresent()) {
            StructureTemplate wingTpl = wingOpt.get();
            int wingHalfX = wingTpl.getSize().getX() / 2;
            int wingHalfZ = wingTpl.getSize().getZ() / 2;

            // West wing centered at (-15, 0, 0)
            BlockPos westCenter = center.offset(-15, 0, 0);
            BlockPos westOrigin = westCenter.offset(-wingHalfX, 0, -wingHalfZ);
            clearInteriorIfNeeded(level, box, westOrigin, wingTpl.getSize().getX(), wingTpl.getSize().getY(), wingTpl.getSize().getZ());
            wingTpl.placeInWorld(level, westOrigin, westOrigin, settings, random, 2);
            placeWalkwayBridgeX(level, box, center.getX() - 15 + wingHalfX - 1, center.getX() - centerHalfX + 1,
                    center.getY(), center.getZ(), palette);

            // East wing centered at (+15, 0, 0)
            BlockPos eastCenter = center.offset(15, 0, 0);
            BlockPos eastOrigin = eastCenter.offset(-wingHalfX, 0, -wingHalfZ);
            clearInteriorIfNeeded(level, box, eastOrigin, wingTpl.getSize().getX(), wingTpl.getSize().getY(), wingTpl.getSize().getZ());
            wingTpl.placeInWorld(level, eastOrigin, eastOrigin, settings, random, 2);
            placeWalkwayBridgeX(level, box, center.getX() + centerHalfX - 1, center.getX() + 15 - wingHalfX + 1,
                    center.getY(), center.getZ(), palette);

            if (proportion == ArchitecturalProportion.TOWER_SPIRE) {
                BlockPos northCenter = center.offset(0, 0, -15);
                BlockPos northOrigin = northCenter.offset(-wingHalfX, 0, -wingHalfZ);
                clearInteriorIfNeeded(level, box, northOrigin, wingTpl.getSize().getX(), wingTpl.getSize().getY(), wingTpl.getSize().getZ());
                wingTpl.placeInWorld(level, northOrigin, northOrigin, settings, random, 2);
                placeWalkwayBridgeZ(level, box, center.getZ() - 15 + wingHalfZ - 1, center.getZ() - centerHalfZ + 1,
                        center.getY(), center.getX(), palette);
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

    private static void placeWalkwayBridgeX(
            WorldGenLevel level,
            BoundingBox box,
            int minX,
            int maxX,
            int y,
            int centerZ,
            ResolvedPalette palette
    ) {
        int lo = Math.min(minX, maxX);
        int hi = Math.max(minX, maxX);
        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        BlockState vein = ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                .setValue(HyphalVeinBlock.CHARGED, true)
                .setValue(HyphalVeinBlock.STRENGTH, 3);
        for (int x = lo; x <= hi; x++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = new BlockPos(x, y, centerZ + dz);
                if (box.isInside(floor)) {
                    level.setBlock(floor, (dz == 0 && Math.floorMod(x, 2) == 0) ? mycelium : palette.primaryWall(), 2);
                }
                if (dz == 0) {
                    BlockPos top = floor.above();
                    if (box.isInside(top)) {
                        level.setBlock(top, HyphalVeinBlock.recomputeConnections(level, top, vein), 2);
                    }
                }
            }
            for (int wallZ : new int[]{-1, 1}) {
                BlockPos parapet = new BlockPos(x, y + 1, centerZ + wallZ);
                if (box.isInside(parapet) && Math.floorMod(x, 2) == 0 && level.getBlockState(parapet).isAir()) {
                    level.setBlock(parapet, palette.wallOrFence(), 2);
                }
            }
        }
        for (int x = lo; x <= hi; x++) {
            BlockPos top = new BlockPos(x, y + 1, centerZ);
            if (box.isInside(top) && level.getBlockState(top).is(ModBlocks.HYPHAL_VEIN.get())) {
                level.setBlock(top, HyphalVeinBlock.recomputeConnections(level, top, vein), 2);
                if (level.getBlockEntity(top) instanceof HyphalVeinBlockEntity veinBe) {
                    veinBe.setRoot(true);
                }
            }
        }
    }

    private static void placeWalkwayBridgeZ(
            WorldGenLevel level,
            BoundingBox box,
            int minZ,
            int maxZ,
            int y,
            int centerX,
            ResolvedPalette palette
    ) {
        int lo = Math.min(minZ, maxZ);
        int hi = Math.max(minZ, maxZ);
        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        BlockState vein = ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                .setValue(HyphalVeinBlock.CHARGED, true)
                .setValue(HyphalVeinBlock.STRENGTH, 3);
        for (int z = lo; z <= hi; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos floor = new BlockPos(centerX + dx, y, z);
                if (box.isInside(floor)) {
                    level.setBlock(floor, (dx == 0 && Math.floorMod(z, 2) == 0) ? mycelium : palette.primaryWall(), 2);
                }
                if (dx == 0) {
                    BlockPos top = floor.above();
                    if (box.isInside(top)) {
                        level.setBlock(top, HyphalVeinBlock.recomputeConnections(level, top, vein), 2);
                    }
                }
            }
            for (int wallX : new int[]{-1, 1}) {
                BlockPos parapet = new BlockPos(centerX + wallX, y + 1, z);
                if (box.isInside(parapet) && Math.floorMod(z, 2) == 0 && level.getBlockState(parapet).isAir()) {
                    level.setBlock(parapet, palette.wallOrFence(), 2);
                }
            }
        }
        for (int z = lo; z <= hi; z++) {
            BlockPos top = new BlockPos(centerX, y + 1, z);
            if (box.isInside(top) && level.getBlockState(top).is(ModBlocks.HYPHAL_VEIN.get())) {
                level.setBlock(top, HyphalVeinBlock.recomputeConnections(level, top, vein), 2);
                if (level.getBlockEntity(top) instanceof HyphalVeinBlockEntity veinBe) {
                    veinBe.setRoot(true);
                }
            }
        }
    }
}
