package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.ModWorldgenKeys;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;
import java.util.OptionalLong;

/**
 * Datapack bootstrap definitions for the Deep Mycelium dimension type, noise settings,
 * and level stem.
 */
public final class MycelialDimensionBootstrap {
    private static final ResourceKey<DensityFunction> SHIFT_X =
            ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace("shift_x"));
    private static final ResourceKey<DensityFunction> SHIFT_Z =
            ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace("shift_z"));
    private static final ResourceKey<DensityFunction> NETHER_BASE_3D_NOISE =
            ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace("nether/base_3d_noise"));

    private MycelialDimensionBootstrap() {
    }

    public static void bootstrapDimensionTypes(BootstrapContext<DimensionType> context) {
        context.register(ModWorldgenKeys.DEEP_MYCELIUM_TYPE, new DimensionType(
                OptionalLong.empty(),
                false,
                true,
                false,
                false,
                1.0D,
                false,
                false,
                0,
                128,
                128,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.NETHER_EFFECTS,
                0.15F,
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 11), 15)
        ));
    }

    public static void bootstrapNoiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
        HolderGetter<DensityFunction> densityFunctions = context.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);

        DensityFunction shiftX = holderFunction(densityFunctions, SHIFT_X);
        DensityFunction shiftZ = holderFunction(densityFunctions, SHIFT_Z);
        DensityFunction base3dNoise = holderFunction(densityFunctions, NETHER_BASE_3D_NOISE);

        DensityFunction temperature = DensityFunctions.shiftedNoise2d(
                shiftX, shiftZ, 0.25D, noises.getOrThrow(Noises.TEMPERATURE));
        DensityFunction vegetation = DensityFunctions.shiftedNoise2d(
                shiftX, shiftZ, 0.25D, noises.getOrThrow(Noises.VEGETATION));

        DensityFunction topSlide = DensityFunctions.add(
                DensityFunctions.constant(0.9375D),
                DensityFunctions.mul(
                        DensityFunctions.yClampedGradient(104, 128, 1.0D, 0.0D),
                        DensityFunctions.add(DensityFunctions.constant(-0.9375D), base3dNoise)
                )
        );
        DensityFunction fullSlide = DensityFunctions.add(
                DensityFunctions.constant(2.5D),
                DensityFunctions.mul(
                        DensityFunctions.yClampedGradient(-8, 24, 0.0D, 1.0D),
                        DensityFunctions.add(DensityFunctions.constant(-2.5D), topSlide)
                )
        );
        DensityFunction finalDensity = DensityFunctions.mul(
                DensityFunctions.constant(0.64D),
                DensityFunctions.interpolated(DensityFunctions.blendDensity(fullSlide))
        ).squeeze();

        NoiseRouter router = new NoiseRouter(
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                temperature,
                vegetation,
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                finalDensity,
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero()
        );

        SurfaceRules.RuleSource bedrockFloor = SurfaceRules.ifTrue(
                SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)),
                SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())
        );
        SurfaceRules.RuleSource bedrockRoof = SurfaceRules.ifTrue(
                SurfaceRules.not(SurfaceRules.verticalGradient("bedrock_roof", VerticalAnchor.belowTop(5), VerticalAnchor.top())),
                SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())
        );
        SurfaceRules.RuleSource floorCover = SurfaceRules.ifTrue(
                SurfaceRules.stoneDepthCheck(0, false, CaveSurface.FLOOR),
                SurfaceRules.state(ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState())
        );

        context.register(ModWorldgenKeys.DEEP_MYCELIUM_NOISE, new NoiseGeneratorSettings(
                NoiseSettings.create(0, 128, 1, 2),
                ModBlocks.MYCELIAL_STONE.get().defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                router,
                SurfaceRules.sequence(bedrockFloor, bedrockRoof, floorCover),
                List.of(),
                24,
                false,
                false,
                false,
                true
        ));
    }

    public static void bootstrapLevelStems(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);

        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(List.of(
                com.mojang.datafixers.util.Pair.of(
                        Climate.parameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
                        biomes.getOrThrow(ModWorldgenKeys.DEEP_MYCELIUM_CAVERNS)),
                com.mojang.datafixers.util.Pair.of(
                        Climate.parameters(0.4F, 0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
                        biomes.getOrThrow(ModWorldgenKeys.LATENT_SPORE_FOREST)),
                com.mojang.datafixers.util.Pair.of(
                        Climate.parameters(-0.4F, -0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
                        biomes.getOrThrow(ModWorldgenKeys.HIVE_HEART_SANCTUM))
        )));

        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                biomeSource,
                noiseSettings.getOrThrow(ModWorldgenKeys.DEEP_MYCELIUM_NOISE)
        );
        context.register(
                ModWorldgenKeys.DEEP_MYCELIUM_STEM,
                new LevelStem(dimensionTypes.getOrThrow(ModWorldgenKeys.DEEP_MYCELIUM_TYPE), generator)
        );
    }

    private static DensityFunction holderFunction(HolderGetter<DensityFunction> lookup, ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(lookup.getOrThrow(key));
    }
}
