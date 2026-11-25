package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModAdvancements;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Generates all 31 progression advancements under {@code data/mycelialmind/advancement/*.json},
 * preserving exact parent hierarchy, criterion keys used at runtime, and bilingual lang keys.
 */
public final class MycelialAdvancementProvider implements AdvancementProvider.AdvancementGenerator {
    private static final ResourceLocation ROOT_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/block/mycelium_side.png");

    @Override
    public void generate(
            HolderLookup.Provider registries,
            Consumer<AdvancementHolder> saver,
            ExistingFileHelper existingFileHelper
    ) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(
                        ModBlocks.MYCELIAL_NODE.get(),
                        title("root"),
                        description("root"),
                        ROOT_BACKGROUND,
                        AdvancementType.TASK,
                        false,
                        false,
                        false
                )
                .addCriterion("has_biomass", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.MYCELIAL_BIOMASS.get()))
                .save(saver, id("root"), existingFileHelper);

        saveItemTask(saver, existingFileHelper, "chitin_bulwark", root, ModItems.CHITIN_CHESTPLATE.get(),
                AdvancementType.TASK, "has_chitin_armor",
                anyOf(
                        ModItems.CHITIN_HELMET.get(),
                        ModItems.CHITIN_CHESTPLATE.get(),
                        ModItems.CHITIN_LEGGINGS.get(),
                        ModItems.CHITIN_BOOTS.get()
                ));

        AdvancementHolder sinkholeDescend = saveItemTask(
                saver, existingFileHelper, "sinkhole_descend", root, ModItems.ROOT_SAP_VIAL.get(),
                AdvancementType.TASK, "has_sinkhole_reagent",
                anyOf(
                        ModItems.ROOT_SAP_VIAL.get(),
                        ModItems.CANOPY_MEMBRANE.get(),
                        ModItems.ROOT_HEART_RESIN.get()
                ));

        saveSimpleItem(saver, existingFileHelper, "sinkhole_elixir", sinkholeDescend,
                ModItems.RESONANT_SAP_ELIXIR.get(), AdvancementType.GOAL, "has_elixir", ModItems.RESONANT_SAP_ELIXIR.get());

        AdvancementHolder firstAdaptation = saveSimpleItem(
                saver, existingFileHelper, "first_adaptation", root,
                ModItems.BIO_SCANNER.get(), AdvancementType.TASK, "has_scanner", ModItems.BIO_SCANNER.get());

        AdvancementHolder geneticSampler = saveSimpleItem(
                saver, existingFileHelper, "genetic_sampler", firstAdaptation,
                ModItems.BIO_EXTRACTOR.get(), AdvancementType.TASK, "has_sample", ModItems.GENETIC_SAMPLE.get());

        AdvancementHolder biotechResearch = saveSimpleItem(
                saver, existingFileHelper, "biotech_research", geneticSampler,
                ModBlocks.SYNAPTIC_RESEARCH_BENCH.get(), AdvancementType.TASK,
                "has_bench", ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());

        AdvancementHolder symbioticVessel = saveSimpleItem(
                saver, existingFileHelper, "symbiotic_vessel", biotechResearch,
                ModItems.SYMBIOTIC_MUTAGEN.get(), AdvancementType.TASK, "has_mutagen", ModItems.SYMBIOTIC_MUTAGEN.get());

        saveSimpleItem(saver, existingFileHelper, "chimera_bioforge", symbioticVessel,
                ModItems.CHIMERA_OVUM.get(), AdvancementType.GOAL, "has_chimera_ovum", ModItems.CHIMERA_OVUM.get());

        AdvancementHolder targetedPurge = saveSimpleItem(
                saver, existingFileHelper, "targeted_purge", biotechResearch,
                ModItems.COUNTER_SERUM.get(), AdvancementType.TASK, "has_serum", ModItems.COUNTER_SERUM.get());

        baseBuilder("pavlovian_deceiver", targetedPurge, ModItems.FALSE_ENGRAM_INJECTOR.get(), AdvancementType.CHALLENGE)
                .addCriterion("has_engram", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.VOLATILE_ENGRAM.get()))
                .addCriterion("has_injector", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.FALSE_ENGRAM_INJECTOR.get()))
                .save(saver, id("pavlovian_deceiver"), existingFileHelper);

        AdvancementHolder tier2Awakening = saveSimpleItem(
                saver, existingFileHelper, "tier2_awakening", firstAdaptation,
                ModBlocks.PURIFICATION_PYLON.get(), AdvancementType.TASK, "has_pylon", ModBlocks.PURIFICATION_PYLON.get());

        AdvancementHolder baseSentinel = saveSimpleItem(
                saver, existingFileHelper, "base_sentinel", tier2Awakening,
                ModBlocks.PURIFICATION_TURRET.get(), AdvancementType.TASK,
                "has_turret", ModBlocks.PURIFICATION_TURRET.get());

        baseBuilder("architect_bane", baseSentinel, ModItems.NEUTRALIZING_SPORE_LAUNCHER.get(), AdvancementType.TASK)
                .addCriterion("killed_architect", KilledTrigger.TriggerInstance.playerKilledEntity(
                        EntityPredicate.Builder.entity().of(ModEntities.SPORE_ARCHITECT.get())))
                .save(saver, id("architect_bane"), existingFileHelper);

        AdvancementHolder purifierDroneDeployed = saveImpossible(
                saver, existingFileHelper, ModAdvancements.PURIFIER_DRONE_DEPLOYED, baseSentinel,
                ModItems.PURIFIER_DRONE_CORE.get(), AdvancementType.TASK);

        AdvancementHolder bastionTier3 = saveImpossible(
                saver, existingFileHelper, ModAdvancements.BASTION_TIER3, purifierDroneDeployed,
                ModBlocks.PURIFICATION_TURRET.get(), AdvancementType.GOAL);

        baseBuilder("counteroffensive_master", bastionTier3, ModItems.OVERMIND_CROWN.get(), AdvancementType.CHALLENGE)
                .addCriterion("has_beacon", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.TIDE_BEACON.get()))
                .addCriterion("has_drone_core", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PURIFIER_DRONE_CORE.get()))
                .save(saver, id("counteroffensive_master"), existingFileHelper);

        AdvancementHolder severTheNetwork = saveSimpleItem(
                saver, existingFileHelper, "sever_the_network", tier2Awakening,
                ModItems.SYNAPTIC_CORE.get(), AdvancementType.TASK, "has_synaptic_core", ModItems.SYNAPTIC_CORE.get());

        AdvancementHolder synapticGraftNetwork = saveSimpleItem(
                saver, existingFileHelper, "synaptic_graft_network", severTheNetwork,
                ModItems.SYNAPTIC_GRAFT.get(), AdvancementType.TASK,
                "has_synaptic_graft", ModItems.SYNAPTIC_GRAFT.get());

        saveSimpleItem(saver, existingFileHelper, "chemical_diplomat", synapticGraftNetwork,
                ModItems.QUORUM_SEAL.get(), AdvancementType.GOAL, "has_quorum_seal", ModItems.QUORUM_SEAL.get());

        AdvancementHolder hiveCoreSlayer = saveSimpleItem(
                saver, existingFileHelper, "hive_core_slayer", severTheNetwork,
                ModItems.HIVE_HEART.get(), AdvancementType.CHALLENGE, "has_hive_heart", ModItems.HIVE_HEART.get());

        saveSimpleItem(saver, existingFileHelper, "synaptic_mastery", hiveCoreSlayer,
                ModItems.SYNAPTIC_SUPPRESSOR.get(), AdvancementType.CHALLENGE,
                "has_suppressor", ModItems.SYNAPTIC_SUPPRESSOR.get());

        saveSimpleItem(saver, existingFileHelper, "tide_breaker", hiveCoreSlayer,
                ModItems.TIDE_BEACON.get(), AdvancementType.GOAL, "has_tide_beacon", ModItems.TIDE_BEACON.get());

        AdvancementHolder enterDeepMycelium = baseBuilder(
                "enter_deep_mycelium", hiveCoreSlayer, ModItems.RIFT_CATALYST.get(), AdvancementType.TASK)
                .addCriterion("entered_deep_mycelium",
                        ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.DEEP_MYCELIUM_LEVEL))
                .save(saver, id("enter_deep_mycelium"), existingFileHelper);

        saveImpossible(saver, existingFileHelper, ModAdvancements.IMMUNE_HOMEOSTASIS, enterDeepMycelium,
                ModBlocks.ANTIBODY_CYST.get(), AdvancementType.CHALLENGE);

        AdvancementHolder apexMatriarchSlayer = saveSimpleItem(
                saver, existingFileHelper, "apex_matriarch_slayer", enterDeepMycelium,
                ModItems.OVERMIND_ESSENCE.get(), AdvancementType.CHALLENGE,
                "has_essence", ModItems.OVERMIND_ESSENCE.get());

        saveSimpleItem(saver, existingFileHelper, "overmind_ascendant", apexMatriarchSlayer,
                ModItems.OVERMIND_CROWN.get(), AdvancementType.CHALLENGE, "has_crown", ModItems.OVERMIND_CROWN.get());

        saveImpossible(saver, existingFileHelper, ModAdvancements.RECOVERED_GARDEN, apexMatriarchSlayer,
                ModBlocks.RECLAIMED_BLOOM.get(), AdvancementType.GOAL);

        saveImpossible(saver, existingFileHelper, ModAdvancements.RECOVERED_ROUTE, apexMatriarchSlayer,
                ModBlocks.PURIFICATION_PYLON.get(), AdvancementType.GOAL);

        AdvancementHolder sovereignNexus = saveSimpleItem(
                saver, existingFileHelper, "sovereign_nexus", apexMatriarchSlayer,
                ModBlocks.SOVEREIGN_NEXUS.get(), AdvancementType.GOAL,
                "has_nexus", ModBlocks.SOVEREIGN_NEXUS.get());

        saveImpossible(saver, existingFileHelper, ModAdvancements.SOVEREIGN_COLONY_TIER3, sovereignNexus,
                ModItems.SOVEREIGN_COMMAND_STAFF.get(), AdvancementType.CHALLENGE);
    }

    private static Advancement.Builder baseBuilder(
            String name,
            @Nullable AdvancementHolder parent,
            ItemLike icon,
            AdvancementType type
    ) {
        Advancement.Builder builder = Advancement.Builder.advancement();
        if (parent != null) {
            builder.parent(parent);
        }
        return builder.display(icon, title(name), description(name), null, type, true, true, false);
    }

    private static AdvancementHolder saveSimpleItem(
            Consumer<AdvancementHolder> saver,
            ExistingFileHelper existingFileHelper,
            String name,
            AdvancementHolder parent,
            ItemLike icon,
            AdvancementType type,
            String criterionKey,
            ItemLike requiredItem
    ) {
        return baseBuilder(name, parent, icon, type)
                .addCriterion(criterionKey, InventoryChangeTrigger.TriggerInstance.hasItems(requiredItem))
                .save(saver, id(name), existingFileHelper);
    }

    private static AdvancementHolder saveItemTask(
            Consumer<AdvancementHolder> saver,
            ExistingFileHelper existingFileHelper,
            String name,
            AdvancementHolder parent,
            ItemLike icon,
            AdvancementType type,
            String criterionKey,
            Criterion<?> criterion
    ) {
        return baseBuilder(name, parent, icon, type)
                .addCriterion(criterionKey, criterion)
                .save(saver, id(name), existingFileHelper);
    }

    private static AdvancementHolder saveImpossible(
            Consumer<AdvancementHolder> saver,
            ExistingFileHelper existingFileHelper,
            ModAdvancements.TriggeredAdvancement triggered,
            AdvancementHolder parent,
            ItemLike icon,
            AdvancementType type
    ) {
        return baseBuilder(triggered.id().getPath(), parent, icon, type)
                .addCriterion(triggered.criterion(),
                        CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(saver, triggered.id(), existingFileHelper);
    }

    private static Criterion<?> anyOf(ItemLike... items) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, path);
    }

    private static Component title(String name) {
        return Component.translatable("advancement." + MycelialMind.MODID + "." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancement." + MycelialMind.MODID + "." + name + ".description");
    }
}
