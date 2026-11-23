package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
                        output.accept(ModItems.BIO_SCANNER.get());
                        output.accept(ModItems.BIO_EXTRACTOR.get());
                        output.accept(ModItems.MYCELIAL_CODEX.get());
                        for (var engramId : com.alex.mycelialmind.hive.FieldEngramId.values()) {
                            output.accept(com.alex.mycelialmind.item.FieldEngramItem.createStack(engramId));
                        }
                        output.accept(ModItems.ENZYME_LANCE.get());
                        output.accept(ModItems.NEUTRALIZING_SPORE_LAUNCHER.get());
                        output.accept(ModItems.SYMBIOTIC_BLADE.get());
                        output.accept(ModItems.SPORE_ANTIDOTE.get());
                        output.accept(ModItems.CONCENTRATED_ANTIDOTE.get());
                        output.accept(ModItems.PURIFIER_GRENADE.get());
                        output.accept(ModItems.SYNAPTIC_SUPPRESSOR.get());
                        output.accept(ModItems.GENETIC_SAMPLE.get());
                        output.accept(ModItems.SYMBIOTIC_MUTAGEN.get());
                        output.accept(ModItems.COUNTER_SERUM.get());
                        output.accept(ModItems.CHITIN_HELMET.get());
                        output.accept(ModItems.CHITIN_CHESTPLATE.get());
                        output.accept(ModItems.CHITIN_LEGGINGS.get());
                        output.accept(ModItems.CHITIN_BOOTS.get());
                        output.accept(ModItems.RIFT_CATALYST.get());
                        output.accept(ModItems.OVERMIND_ESSENCE.get());
                        output.accept(ModItems.OVERMIND_CROWN.get());
                        output.accept(ModItems.TIDE_BEACON.get());
                        output.accept(ModItems.PURIFIER_DRONE_CORE.get());
                        output.accept(ModItems.NEURAL_ECHO_CORE.get());
                        output.accept(ModItems.SYNAPTIC_GRAFT.get());
                        output.accept(ModBlocks.RECLAIMED_BLOOM.get());
                        output.accept(ModItems.RECLAIMED_BLOOM_MELEE.get());
                        output.accept(ModItems.RECLAIMED_BLOOM_PROJECTILE.get());
                        output.accept(ModItems.RECLAIMED_BLOOM_FIRE.get());
                        output.accept(ModItems.RECLAIMED_BLOOM_EXPLOSION.get());
                        output.accept(ModItems.RECLAIMED_BLOOM_MAGIC.get());
                        output.accept(ModItems.NEURAL_ECHO_DECOY.get());
                        output.accept(ModItems.CHIMERA_OVUM.get());
                        output.accept(ModItems.VOLATILE_ENGRAM.get());
                        output.accept(ModItems.FALSE_ENGRAM_INJECTOR.get());
                        output.accept(ModItems.SCHISM_INJECTOR.get());
                        output.accept(ModItems.TRUCE_PHEROMONE.get());
                        output.accept(ModItems.QUORUM_SEAL.get());

                        output.accept(ModItems.MYCELIAL_BIOMASS.get());
                        output.accept(ModItems.CHITIN_PLATE.get());
                        output.accept(ModItems.LATENT_SPORE.get());
                        output.accept(ModItems.SYNAPTIC_CORE.get());
                        output.accept(ModItems.HIVE_HEART.get());

                        output.accept(ModBlocks.CREEPING_MYCELIUM.get());
                        output.accept(ModBlocks.HYPHAL_VEIN.get());
                        output.accept(ModBlocks.SPORE_VENT.get());
                        output.accept(ModBlocks.MYCELIAL_NODE.get());
                        output.accept(ModBlocks.INCUBATOR_NODE.get());
                        output.accept(ModBlocks.SENTINEL_NODE.get());
                        output.accept(ModBlocks.SYMBIOTIC_NODE.get());
                        output.accept(ModBlocks.PURIFICATION_PYLON.get());
                        output.accept(ModBlocks.PURIFICATION_TURRET.get());
                        output.accept(ModBlocks.REINFORCED_BIO_BARRIER.get());
                        output.accept(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());
                        output.accept(ModBlocks.HIVE_ALTAR.get());
                        output.accept(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get());
                        output.accept(ModBlocks.SPORE_BULB.get());
                        output.accept(ModBlocks.MYCELIAL_TENDRILS.get());
                        output.accept(ModBlocks.MYCELIAL_STONE.get());
                        output.accept(ModBlocks.CHITIN_PILLAR.get());
                        output.accept(ModBlocks.MYCELIAL_ARCH_BRICK.get());
                        output.accept(ModBlocks.BIO_CHITIN_LATTICE.get());
                        output.accept(ModBlocks.LUMINESCENT_AMBER_VEIN.get());
                        output.accept(ModBlocks.MATRIARCH_ALTAR.get());
                        output.accept(ModBlocks.MYCELIAL_RIFT.get());
                        output.accept(ModBlocks.ANTIBODY_CYST.get());
                        output.accept(ModBlocks.EPHEMERAL_CHITIN.get());

                        output.accept(ModItems.SPORELING_SPAWN_EGG.get());
                        output.accept(ModItems.CHITIN_BRUTE_SPAWN_EGG.get());
                        output.accept(ModItems.MYCELIAL_STALKER_SPAWN_EGG.get());
                        output.accept(ModItems.ACID_SPITTER_SPAWN_EGG.get());
                        output.accept(ModItems.BURROWER_SPAWN_EGG.get());
                        output.accept(ModItems.PYRO_SHROOM_SPAWN_EGG.get());
                        output.accept(ModItems.SYNAPSE_WEAVER_SPAWN_EGG.get());
                        output.accept(ModItems.GOLIATH_CARRIER_SPAWN_EGG.get());
                        output.accept(ModItems.LEECH_PARASITE_SPAWN_EGG.get());
                        output.accept(ModItems.COLONY_WARDEN_SPAWN_EGG.get());
                        output.accept(ModItems.SPORE_ARCHITECT_SPAWN_EGG.get());
                        output.accept(ModItems.SYNAPTIC_MIMIC_SPAWN_EGG.get());
                        output.accept(ModItems.MYCELIAL_CHIMERA_SPAWN_EGG.get());
                        output.accept(ModItems.SPORE_BEHEMOTH_SPAWN_EGG.get());
                        output.accept(ModItems.PURIFIER_DRONE_SPAWN_EGG.get());
                        output.accept(ModItems.HIVE_CORE_SPAWN_EGG.get());
                        output.accept(ModItems.APEX_MATRIARCH_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
