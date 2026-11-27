package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.item.FieldEngramItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Creative tab for Mycelial Mind blocks, bio-tools, weapons, and spawn eggs.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MycelialMind.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MYCELIAL_TAB =
            CREATIVE_MODE_TABS.register("mycelial_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mycelialmind"))
                    .icon(() -> new ItemStack(ModItems.BIO_SCANNER.get()))
                    .displayItems((params, output) -> {
                        acceptAll(output,
                                ModItems.BIO_SCANNER,
                                ModItems.BIO_EXTRACTOR,
                                ModItems.MYCELIAL_CODEX);
                        for (FieldEngramId engramId : FieldEngramId.values()) {
                            output.accept(FieldEngramItem.createStack(engramId));
                        }
                        acceptAll(output,
                                ModItems.ENZYME_LANCE,
                                ModItems.NEUTRALIZING_SPORE_LAUNCHER,
                                ModItems.SYMBIOTIC_BLADE,
                                ModItems.SPORE_ANTIDOTE,
                                ModItems.CONCENTRATED_ANTIDOTE,
                                ModItems.PURIFIER_GRENADE,
                                ModItems.SYNAPTIC_SUPPRESSOR,
                                ModItems.GENETIC_SAMPLE,
                                ModItems.SYMBIOTIC_MUTAGEN,
                                ModItems.COUNTER_SERUM,
                                ModItems.CHITIN_HELMET,
                                ModItems.CHITIN_CHESTPLATE,
                                ModItems.CHITIN_LEGGINGS,
                                ModItems.CHITIN_BOOTS,
                                ModItems.RIFT_CATALYST,
                                ModItems.OVERMIND_ESSENCE,
                                ModItems.OVERMIND_CROWN,
                                ModItems.TIDE_BEACON,
                                ModItems.PURIFIER_DRONE_CORE,
                                ModItems.NEURAL_ECHO_CORE,
                                ModItems.SYNAPTIC_GRAFT,
                                ModBlocks.RECLAIMED_BLOOM,
                                ModItems.RECLAIMED_BLOOM_MELEE,
                                ModItems.RECLAIMED_BLOOM_PROJECTILE,
                                ModItems.RECLAIMED_BLOOM_FIRE,
                                ModItems.RECLAIMED_BLOOM_EXPLOSION,
                                ModItems.RECLAIMED_BLOOM_MAGIC,
                                ModItems.NEURAL_ECHO_DECOY,
                                ModItems.CHIMERA_OVUM,
                                ModItems.VOLATILE_ENGRAM,
                                ModItems.FALSE_ENGRAM_INJECTOR,
                                ModItems.SCHISM_INJECTOR,
                                ModItems.TRUCE_PHEROMONE,
                                ModItems.QUORUM_SEAL,
                                ModItems.SOVEREIGN_COMMAND_STAFF,
                                ModItems.MYCELIAL_BIOMASS,
                                ModItems.CHITIN_PLATE,
                                ModItems.LATENT_SPORE,
                                ModItems.SYNAPTIC_CORE,
                                ModItems.HIVE_HEART,
                                ModItems.ROOT_SAP_VIAL,
                                ModItems.RESONANT_SAP_ELIXIR,
                                ModItems.CANOPY_MEMBRANE,
                                ModItems.ROOT_HEART_RESIN,
                                ModBlocks.CREEPING_MYCELIUM,
                                ModItems.HYPHAL_VEIN,
                                ModBlocks.SPORE_VENT,
                                ModBlocks.SPORE_UPDRAFT_VENT,
                                ModBlocks.ROOT_SAP_MOSS,
                                ModBlocks.ROOT_CAP_SHELF,
                                ModBlocks.MYCELIAL_NODE,
                                ModBlocks.INCUBATOR_NODE,
                                ModBlocks.SENTINEL_NODE,
                                ModBlocks.SYMBIOTIC_NODE,
                                ModBlocks.SOVEREIGN_NEXUS,
                                ModBlocks.PURIFICATION_PYLON,
                                ModBlocks.PURIFICATION_TURRET,
                                ModBlocks.REINFORCED_BIO_BARRIER,
                                ModBlocks.SYNAPTIC_RESEARCH_BENCH,
                                ModBlocks.HIVE_ALTAR,
                                ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP,
                                ModBlocks.SPORE_BULB,
                                ModBlocks.MYCELIAL_TENDRILS,
                                ModBlocks.MYCELIAL_STONE,
                                ModBlocks.CHITIN_PILLAR,
                                ModBlocks.MYCELIAL_ARCH_BRICK,
                                ModBlocks.BIO_CHITIN_LATTICE,
                                ModBlocks.LUMINESCENT_AMBER_VEIN,
                                ModBlocks.MATRIARCH_ALTAR,
                                ModBlocks.MYCELIAL_RIFT,
                                ModBlocks.ANTIBODY_CYST,
                                ModBlocks.EPHEMERAL_CHITIN,
                                ModItems.SPORELING_SPAWN_EGG,
                                ModItems.CHITIN_BRUTE_SPAWN_EGG,
                                ModItems.MYCELIAL_STALKER_SPAWN_EGG,
                                ModItems.ACID_SPITTER_SPAWN_EGG,
                                ModItems.BURROWER_SPAWN_EGG,
                                ModItems.PYRO_SHROOM_SPAWN_EGG,
                                ModItems.SYNAPSE_WEAVER_SPAWN_EGG,
                                ModItems.GOLIATH_CARRIER_SPAWN_EGG,
                                ModItems.LEECH_PARASITE_SPAWN_EGG,
                                ModItems.COLONY_WARDEN_SPAWN_EGG,
                                ModItems.SPORE_ARCHITECT_SPAWN_EGG,
                                ModItems.SYNAPTIC_MIMIC_SPAWN_EGG,
                                ModItems.MYCELIAL_CHIMERA_SPAWN_EGG,
                                ModItems.COLONY_SYMBIONT_SPAWN_EGG,
                                ModItems.SPORE_BEHEMOTH_SPAWN_EGG,
                                ModItems.PURIFIER_DRONE_SPAWN_EGG,
                                ModItems.SPORE_GLIDER_SPAWN_EGG,
                                ModItems.ROOT_KEEPER_SPAWN_EGG,
                                ModItems.HIVE_CORE_SPAWN_EGG,
                                ModItems.APEX_MATRIARCH_SPAWN_EGG
                        );
                    })
                    .build());

    private ModCreativeTabs() {
    }

    @SafeVarargs
    private static void acceptAll(CreativeModeTab.Output output, Supplier<? extends ItemLike>... items) {
        for (Supplier<? extends ItemLike> item : items) {
            output.accept(item.get());
        }
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
