package com.alex.mycelialmind.recipe;

import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity.SynthesisResult;
import com.alex.mycelialmind.hive.AcquisitionDifficultyEngine;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.item.GeneticSampleItem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Data-driven synthesis recipe for the Synaptic Research Bench, supporting optional catalysts,
 * coded trait preservation, Tier 3 yield bonuses, secret formula unlocks, and Act/Lab gating.
 */
public record SynapticSynthesisRecipe(
        Ingredient sample,
        Ingredient biomass,
        Ingredient catalyst,
        ItemStack result,
        boolean requiresTypedSample,
        boolean copyTraitFromSample,
        int t3BonusCount,
        String gatingMode,
        String minAct,
        int minLabTier,
        int secretIndex
) implements Recipe<SynapticSynthesisInput> {
    public static final String GATING_NONE = "none";
    public static final String GATING_ACT_OR_LAB = "act_or_lab";
    public static final String GATING_MUTAGEN_OR_SERUM = "mutagen_or_serum";
    public static final String GATING_CHIMERA_OVUM = "chimera_ovum";
    public static final String GATING_BLOOM_DUPLICATION = "bloom_duplication";

    @Override
    public boolean matches(SynapticSynthesisInput input, Level level) {
        if (!sample.test(input.sample()) || !biomass.test(input.biomass())) {
            return false;
        }
        if (requiresTypedSample && GeneticSampleItem.getTrait(input.sample()) == null) {
            return false;
        }
        if (catalyst.isEmpty()) {
            return input.catalyst().isEmpty();
        }
        return catalyst.test(input.catalyst());
    }

    @Override
    public ItemStack assemble(SynapticSynthesisInput input, HolderLookup.Provider registries) {
        return buildOutput(input, 1);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(sample);
        list.add(biomass);
        if (!catalyst.isEmpty()) {
            list.add(catalyst);
        }
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SYNAPTIC_SYNTHESIS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SYNAPTIC_SYNTHESIS_TYPE.get();
    }

    public SynthesisResult evaluate(
            SynapticSynthesisInput input,
            @Nullable HiveProgressionTier effectiveAct,
            int effectiveLabTier,
            boolean hiveCoreDefeated
    ) {
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        HiveProgressionTier blockedBy = checkBlockedAct(safeAct, effectiveLabTier, hiveCoreDefeated);
        if (blockedBy != null) {
            return new SynthesisResult(ItemStack.EMPTY, secretIndex, blockedBy);
        }
        ItemStack out = buildOutput(input, effectiveLabTier);
        if (out.isEmpty()) {
            return new SynthesisResult(ItemStack.EMPTY, -1, null);
        }
        return new SynthesisResult(out, secretIndex, null);
    }

    @Nullable
    private HiveProgressionTier checkBlockedAct(
            HiveProgressionTier safeAct,
            int effectiveLabTier,
            boolean hiveCoreDefeated
    ) {
        return switch (gatingMode) {
            case GATING_BLOOM_DUPLICATION -> AcquisitionDifficultyEngine.canSynthesizeBloomDuplication(
                    safeAct, effectiveLabTier, hiveCoreDefeated) ? null : HiveProgressionTier.ADAPTIVE;
            case GATING_MUTAGEN_OR_SERUM -> AcquisitionDifficultyEngine.canSynthesizeMutagenOrSerum(
                    safeAct, effectiveLabTier) ? null : HiveProgressionTier.AWAKENED;
            case GATING_CHIMERA_OVUM -> AcquisitionDifficultyEngine.canSynthesizeChimeraOvum(
                    safeAct, effectiveLabTier, hiveCoreDefeated) ? null : HiveProgressionTier.ADAPTIVE;
            case GATING_ACT_OR_LAB -> {
                HiveProgressionTier reqAct = parseActTier(minAct);
                yield (safeAct.meetsRequiredAct(reqAct) || effectiveLabTier >= minLabTier) ? null : reqAct;
            }
            default -> null;
        };
    }

    private ItemStack buildOutput(SynapticSynthesisInput input, int effectiveLabTier) {
        ItemStack out = result.copy();
        if (t3BonusCount > 0 && effectiveLabTier >= 3) {
            out.grow(t3BonusCount);
        }
        if (copyTraitFromSample) {
            AdaptationTrait trait = GeneticSampleItem.getTrait(input.sample());
            if (trait == null) {
                return ItemStack.EMPTY;
            }
            out.set(ModDataComponents.TRAIT_ID.get(), trait.id());
        }
        return out;
    }

    public static HiveProgressionTier parseActTier(String raw) {
        if (raw == null || raw.isBlank()) {
            return HiveProgressionTier.DORMANT;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "awakened" -> HiveProgressionTier.AWAKENED;
            case "adaptive" -> HiveProgressionTier.ADAPTIVE;
            case "overmind" -> HiveProgressionTier.OVERMIND;
            default -> HiveProgressionTier.DORMANT;
        };
    }

    public static final class Serializer implements RecipeSerializer<SynapticSynthesisRecipe> {
        public static final MapCodec<SynapticSynthesisRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("sample").forGetter(SynapticSynthesisRecipe::sample),
                        Ingredient.CODEC_NONEMPTY.fieldOf("biomass").forGetter(SynapticSynthesisRecipe::biomass),
                        Ingredient.CODEC.optionalFieldOf("catalyst", Ingredient.EMPTY).forGetter(SynapticSynthesisRecipe::catalyst),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SynapticSynthesisRecipe::result),
                        Codec.BOOL.optionalFieldOf("requires_typed_sample", false).forGetter(SynapticSynthesisRecipe::requiresTypedSample),
                        Codec.BOOL.optionalFieldOf("copy_trait_from_sample", false).forGetter(SynapticSynthesisRecipe::copyTraitFromSample),
                        Codec.INT.optionalFieldOf("t3_bonus_count", 0).forGetter(SynapticSynthesisRecipe::t3BonusCount),
                        Codec.STRING.optionalFieldOf("gating_mode", GATING_NONE).forGetter(SynapticSynthesisRecipe::gatingMode),
                        Codec.STRING.optionalFieldOf("min_act", "dormant").forGetter(SynapticSynthesisRecipe::minAct),
                        Codec.INT.optionalFieldOf("min_lab_tier", 1).forGetter(SynapticSynthesisRecipe::minLabTier),
                        Codec.INT.optionalFieldOf("secret_index", -1).forGetter(SynapticSynthesisRecipe::secretIndex)
                ).apply(instance, SynapticSynthesisRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, SynapticSynthesisRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public SynapticSynthesisRecipe decode(RegistryFriendlyByteBuf buf) {
                        Ingredient sample = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        Ingredient biomass = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                        boolean requiresTypedSample = ByteBufCodecs.BOOL.decode(buf);
                        boolean copyTraitFromSample = ByteBufCodecs.BOOL.decode(buf);
                        int t3BonusCount = ByteBufCodecs.VAR_INT.decode(buf);
                        String gatingMode = ByteBufCodecs.STRING_UTF8.decode(buf);
                        String minAct = ByteBufCodecs.STRING_UTF8.decode(buf);
                        int minLabTier = ByteBufCodecs.VAR_INT.decode(buf);
                        int secretIndex = ByteBufCodecs.VAR_INT.decode(buf);
                        return new SynapticSynthesisRecipe(
                                sample, biomass, catalyst, result,
                                requiresTypedSample, copyTraitFromSample, t3BonusCount,
                                gatingMode, minAct, minLabTier, secretIndex
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, SynapticSynthesisRecipe recipe) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.sample());
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.biomass());
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.catalyst());
                        ItemStack.STREAM_CODEC.encode(buf, recipe.result());
                        ByteBufCodecs.BOOL.encode(buf, recipe.requiresTypedSample());
                        ByteBufCodecs.BOOL.encode(buf, recipe.copyTraitFromSample());
                        ByteBufCodecs.VAR_INT.encode(buf, recipe.t3BonusCount());
                        ByteBufCodecs.STRING_UTF8.encode(buf, recipe.gatingMode());
                        ByteBufCodecs.STRING_UTF8.encode(buf, recipe.minAct());
                        ByteBufCodecs.VAR_INT.encode(buf, recipe.minLabTier());
                        ByteBufCodecs.VAR_INT.encode(buf, recipe.secretIndex());
                    }
                };

        @Override
        public MapCodec<SynapticSynthesisRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SynapticSynthesisRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
