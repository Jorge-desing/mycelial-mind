package com.alex.mycelialmind.world;

import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModFeatures;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * PlacementFilter whose 1-in-N chunk rarity scales dynamically with the dimension's {@link HiveProgressionTier}.
 */
public class HiveTierRarityPlacement extends PlacementFilter {
    public static final MapCodec<HiveTierRarityPlacement> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ExtraCodecs.POSITIVE_INT.fieldOf("dormant_chance").forGetter(p -> p.dormantChance),
                    ExtraCodecs.POSITIVE_INT.fieldOf("awakened_chance").forGetter(p -> p.awakenedChance),
                    ExtraCodecs.POSITIVE_INT.fieldOf("adaptive_chance").forGetter(p -> p.adaptiveChance),
                    ExtraCodecs.POSITIVE_INT.fieldOf("overmind_chance").forGetter(p -> p.overmindChance)
            ).apply(instance, HiveTierRarityPlacement::new)
    );

    private final int dormantChance;
    private final int awakenedChance;
    private final int adaptiveChance;
    private final int overmindChance;

    public HiveTierRarityPlacement(int dormantChance, int awakenedChance, int adaptiveChance, int overmindChance) {
        this.dormantChance = dormantChance;
        this.awakenedChance = awakenedChance;
        this.adaptiveChance = adaptiveChance;
        this.overmindChance = overmindChance;
    }

    public int chanceForTier(HiveProgressionTier tier) {
        return switch (tier) {
            case DORMANT -> dormantChance;
            case AWAKENED -> awakenedChance;
            case ADAPTIVE -> adaptiveChance;
            case OVERMIND -> overmindChance;
        };
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(context.getLevel());
        int chance = Math.max(1, chanceForTier(tier));
        return random.nextInt(chance) == 0;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModFeatures.HIVE_TIER_RARITY.get();
    }
}
