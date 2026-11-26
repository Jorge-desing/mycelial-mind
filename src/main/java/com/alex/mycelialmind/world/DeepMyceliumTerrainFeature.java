package com.alex.mycelialmind.world;

import com.alex.mycelialmind.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Generates biome-specific terrain landmarks across the 3 biomes of {@code deep_mycelium}:
 * cavern stalactite/stalagmite pairs, latent spore canopy arches, and sanctum amber monoliths.
 */
public class DeepMyceliumTerrainFeature extends Feature<DeepMyceliumTerrainFeature.Config> {
    public enum Variant implements StringRepresentable {
        CAVERN_STALACTITES("cavern_stalactites"),
        SPORE_CANOPY_ARCH("spore_canopy_arch"),
        SANCTUM_AMBER_MONOLITH("sanctum_amber_monolith"),
        GLOWING_SPORE_POOL("glowing_spore_pool");

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);
        private final String serializedName;

        Variant(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public record Config(Variant variant) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Variant.CODEC.fieldOf("variant").forGetter(Config::variant)
                ).apply(instance, Config::new)
        );
    }

    public DeepMyceliumTerrainFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        return placeVariant(context.level(), context.origin(), context.random(), context.config().variant());
    }

    public static boolean placeVariant(
            WorldGenLevel level,
            BlockPos origin,
            RandomSource random,
            Variant variant
    ) {
        if (level == null || origin == null || variant == null || level.isOutsideBuildHeight(origin)) {
            return false;
        }
        return switch (variant) {
            case CAVERN_STALACTITES -> placeCavernStalactites(level, origin, random);
            case SPORE_CANOPY_ARCH -> placeSporeCanopyArch(level, origin, random);
            case SANCTUM_AMBER_MONOLITH -> placeSanctumAmberMonolith(level, origin, random);
            case GLOWING_SPORE_POOL -> placeGlowingSporePool(level, origin, random);
        };
    }

    private static boolean placeGlowingSporePool(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos floor = findDryFloorWithHeadroom(level, origin, 8, 4);
        if (floor == null) {
            return false;
        }
        // Require solid ground under the entire 4-block basin envelope so pool water is strictly contained.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double dist = Math.hypot(dx, dz);
                if (dist > 4.1) {
                    continue;
                }
                BlockPos cell = floor.offset(dx, 0, dz);
                if (!isDrySolid(level, cell) || !isDrySolid(level, cell.below())) {
                    return false;
                }
            }
        }

        BlockState stone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        BlockState moss = ModBlocks.ROOT_SAP_MOSS.get().defaultBlockState();
        BlockState amber = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState cap = ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState();
        BlockState bulb = ModBlocks.SPORE_BULB.get().defaultBlockState();

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double dist = Math.hypot(dx, dz);
                if (dist > 4.1) {
                    continue;
                }
                BlockPos surface = floor.offset(dx, 0, dz);
                if (dist <= 2.35) {
                    safeSetBlock(level, surface.below(2), stone);
                    safeSetBlock(level, surface.below(), (Math.abs(dx) + Math.abs(dz) <= 1) ? amber : stone);
                    safeSetBlock(level, surface, water);
                    safeSetBlock(level, surface.above(), Blocks.AIR.defaultBlockState());
                } else if (dist <= 3.6) {
                    safeSetBlock(level, surface.below(), stone);
                    safeSetBlock(level, surface, (random.nextInt(4) == 0) ? moss : stone);
                    if (random.nextInt(6) == 0 && isOpenDryAir(level, surface.above())) {
                        safeSetBlock(level, surface.above(), random.nextBoolean() ? cap : bulb);
                    }
                } else {
                    safeSetBlock(level, surface, (random.nextInt(3) == 0) ? moss : stone);
                }
            }
        }
        return true;
    }

    private static boolean placeCavernStalactites(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos ceiling = null;
        for (int step = 0; step <= 10; step++) {
            int[] offsets = step == 0 ? new int[]{0} : new int[]{step, -step};
            for (int dy : offsets) {
                BlockPos cand = origin.offset(0, dy, 0);
                if (!level.isOutsideBuildHeight(cand)
                        && !level.isOutsideBuildHeight(cand.below(5))
                        && isDrySolid(level, cand)
                        && hasVerticalAirSpan(level, cand.below(), -1, 5)) {
                    ceiling = cand;
                    break;
                }
            }
            if (ceiling != null) {
                break;
            }
        }
        if (ceiling == null) {
            return false;
        }

        BlockState stone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        BlockState pillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState amber = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState tendrils = ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState();

        int airBelow = 0;
        for (int d = 1; d <= 14; d++) {
            BlockPos p = ceiling.below(d);
            if (level.isOutsideBuildHeight(p) || !isOpenDryAir(level, p)) {
                break;
            }
            airBelow = d;
        }
        if (airBelow < 5) {
            return false;
        }
        int length = Math.min(airBelow - 2, 3 + random.nextInt(4));
        for (int d = 1; d <= length; d++) {
            BlockPos tipPos = ceiling.below(d);
            BlockState st = (d == length) ? amber : ((d % 2 == 0) ? pillar : stone);
            safeSetBlock(level, tipPos, st);
        }
        if (isOpenDryAir(level, ceiling.below(length + 1))) {
            safeSetBlock(level, ceiling.below(length + 1), tendrils);
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos sideCeil = ceiling.relative(dir);
            if (isDrySolid(level, sideCeil) && isOpenDryAir(level, sideCeil.below())) {
                safeSetBlock(level, sideCeil.below(), stone);
                if (length >= 4 && isOpenDryAir(level, sideCeil.below(2))) {
                    safeSetBlock(level, sideCeil.below(2), (random.nextFloat() < 0.35F) ? tendrils : pillar);
                }
            }
        }

        // Grounded stalagmite beneath when dry cavern floor is within 14 blocks
        if (airBelow < 14) {
            BlockPos floor = ceiling.below(airBelow + 1);
            if (!level.isOutsideBuildHeight(floor) && isDrySolid(level, floor) && floor.getY() + 3 < ceiling.below(length).getY()) {
                safeSetBlock(level, floor.above(), stone);
                safeSetBlock(level, floor.above(2), pillar);
                BlockPos sideFloor = floor.east();
                if (isDrySolid(level, sideFloor) && isOpenDryAir(level, sideFloor.above())) {
                    BlockState flora = random.nextBoolean()
                            ? ModBlocks.SPORE_VENT.get().defaultBlockState()
                            : ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState();
                    safeSetBlock(level, sideFloor.above(), flora);
                }
            }
        }
        return true;
    }

    private static boolean placeSporeCanopyArch(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos floor = findDryFloorWithHeadroom(level, origin, 8, 7);
        if (floor == null) {
            return false;
        }
        BlockState floorState = level.getBlockState(floor);
        if (!floorState.is(ModBlocks.CREEPING_MYCELIUM.get())
                && !floorState.is(ModBlocks.ROOT_SAP_MOSS.get())
                && !floorState.is(ModBlocks.MYCELIAL_STONE.get())
                && !floorState.is(Blocks.MUD)) {
            return false;
        }

        BlockState pillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState stone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        BlockState amber = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState sapMoss = ModBlocks.ROOT_SAP_MOSS.get().defaultBlockState();
        BlockState tendrils = ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState();
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);

        // Grounded 5-block curved stalk anchored firmly to floor
        for (int dy = 1; dy <= 3; dy++) {
            safeSetBlock(level, floor.above(dy), pillar);
        }
        BlockPos shoulder = floor.relative(lean).above(3);
        safeSetBlock(level, shoulder, archBrick);
        safeSetBlock(level, shoulder.above(), pillar);
        BlockPos capCenter = shoulder.above(2);
        safeSetBlock(level, capCenter, amber);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r > 2.4D || (dx == 0 && dz == 0)) {
                    continue;
                }
                BlockPos rim = capCenter.offset(dx, 0, dz);
                if (isOpenDryAir(level, rim)) {
                    BlockState b = ((Math.abs(dx) + Math.abs(dz)) % 3 == 0) ? archBrick : stone;
                    safeSetBlock(level, rim, b);
                    if (r >= 1.8D && isOpenDryAir(level, rim.below()) && ((Math.abs(dx) + Math.abs(dz)) % 2 == 0)) {
                        safeSetBlock(level, rim.below(), tendrils);
                    }
                }
            }
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = floor.relative(dir);
            if (!level.isOutsideBuildHeight(side) && isDrySolid(level, side)) {
                safeSetBlock(level, side, sapMoss);
                if (isOpenDryAir(level, side.above()) && random.nextFloat() < 0.55F) {
                    safeSetBlock(level, side.above(), random.nextBoolean()
                            ? ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState()
                            : ModBlocks.SPORE_BULB.get().defaultBlockState());
                }
            }
        }
        return true;
    }

    private static boolean placeSanctumAmberMonolith(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos floor = findDryFloorWithHeadroom(level, origin, 8, 8);
        if (floor == null) {
            return false;
        }
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState deepslateTiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState pillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState amber = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();

        // 3x3 stepped plinth grounded up to 4 blocks down
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos base = floor.offset(dx, 0, dz);
                boolean isCorner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                if (!level.isOutsideBuildHeight(base) && isSafeToOverwrite(level, base)) {
                    safeSetBlock(level, base, isCorner ? deepslateTiles : archBrick);
                    TerrainPlacementPolicy.anchorOrganicFoundation(
                            level, base.below(), 4, deepslateTiles, archBrick, p -> isSafeToOverwrite(level, p));
                }
            }
        }

        // Tapered 6-block ritual obelisk
        safeSetBlock(level, floor.above(1), archBrick);
        safeSetBlock(level, floor.above(2), pillar);
        safeSetBlock(level, floor.above(3), archBrick);
        safeSetBlock(level, floor.above(4), pillar);
        safeSetBlock(level, floor.above(5), amber);
        safeSetBlock(level, floor.above(6), amethyst);

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos wing = floor.relative(dir).above();
            if (isOpenDryAir(level, wing)) {
                safeSetBlock(level, wing, (random.nextBoolean() ? deepslateTiles : archBrick));
            }
        }
        return true;
    }

    private static BlockPos findDryFloorWithHeadroom(WorldGenLevel level, BlockPos origin, int maxDelta, int minHeadroom) {
        for (int step = 0; step <= maxDelta; step++) {
            int[] offsets = step == 0 ? new int[]{0} : new int[]{step, -step};
            for (int dy : offsets) {
                BlockPos cand = origin.offset(0, dy, 0);
                if (!level.isOutsideBuildHeight(cand)
                        && !level.isOutsideBuildHeight(cand.above(minHeadroom))
                        && isDrySolid(level, cand)
                        && hasVerticalAirSpan(level, cand.above(), 1, minHeadroom)) {
                    return cand;
                }
            }
        }
        return null;
    }

    private static boolean hasVerticalAirSpan(WorldGenLevel level, BlockPos start, int stepY, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos p = start.offset(0, i * stepY, 0);
            if (level.isOutsideBuildHeight(p) || !isOpenDryAir(level, p)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDrySolid(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.isSolid() && state.isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean isOpenDryAir(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.getFluidState(pos).isEmpty() || level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static boolean isSafeToOverwrite(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return !state.is(ModBlocks.PURIFICATION_PYLON.get())
                && !state.is(ModBlocks.HIVE_ALTAR.get())
                && !state.is(ModBlocks.MATRIARCH_ALTAR.get())
                && !state.is(ModBlocks.MYCELIAL_RIFT.get())
                && !state.is(ModBlocks.SOVEREIGN_NEXUS.get())
                && !state.is(ModBlocks.RECLAIMED_BLOOM.get());
    }

    private static boolean safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!isSafeToOverwrite(level, pos)) {
            return false;
        }
        return level.setBlock(pos, state, 2);
    }
}
