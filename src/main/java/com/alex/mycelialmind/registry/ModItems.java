package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.item.BioExtractorItem;
import com.alex.mycelialmind.item.BioScannerItem;
import com.alex.mycelialmind.item.ChimeraOvumItem;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.item.ConcentratedAntidoteItem;
import com.alex.mycelialmind.item.CounterSerumItem;
import com.alex.mycelialmind.item.EnzymeLanceItem;
import com.alex.mycelialmind.item.FalseEngramInjectorItem;
import com.alex.mycelialmind.item.FieldEngramItem;
import com.alex.mycelialmind.item.GeneticSampleItem;
import com.alex.mycelialmind.item.MycelialCodexItem;
import com.alex.mycelialmind.item.NeuralEchoDecoyItem;
import com.alex.mycelialmind.item.NeutralizingSporeLauncherItem;
import com.alex.mycelialmind.item.OvermindCrownItem;
import com.alex.mycelialmind.item.PurifierDroneCoreItem;
import com.alex.mycelialmind.item.PurifierGrenadeItem;
import com.alex.mycelialmind.item.QuorumSealItem;
import com.alex.mycelialmind.item.ReclaimedBloomItem;
import com.alex.mycelialmind.item.ResonantSapElixirItem;
import com.alex.mycelialmind.item.RiftCatalystItem;
import com.alex.mycelialmind.item.RootSapVialItem;
import com.alex.mycelialmind.item.SchismInjectorItem;
import com.alex.mycelialmind.item.SovereignCommandStaffItem;
import com.alex.mycelialmind.item.SporeAntidoteItem;
import com.alex.mycelialmind.item.SymbioticBladeItem;
import com.alex.mycelialmind.item.SymbioticMutagenItem;
import com.alex.mycelialmind.item.SynapticGraftItem;
import com.alex.mycelialmind.item.SynapticSuppressorItem;
import com.alex.mycelialmind.item.TideBeaconItem;
import com.alex.mycelialmind.item.TooltipMaterialItem;
import com.alex.mycelialmind.item.TrucePheromoneItem;
import com.alex.mycelialmind.item.VolatileEngramItem;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers items, bio-tools, weapons, consumables, and spawn eggs for Mycelial Mind.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MycelialMind.MODID);

    public static final TagKey<Item> BIO_EXTRACTOR_ENCHANTABLE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "enchantable/bio_extractor"));

    // Tools & Arsenal
    public static final DeferredItem<Item> BIO_SCANNER = ITEMS.register("bio_scanner",
            () -> new BioScannerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> MYCELIAL_CODEX = ITEMS.register("mycelial_codex",
            () -> new MycelialCodexItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FIELD_ENGRAM = ITEMS.register("field_engram",
            () -> new FieldEngramItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ENZYME_LANCE = ITEMS.register("enzyme_lance",
            () -> new EnzymeLanceItem(new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 2, -2.2F))));

    public static final DeferredItem<Item> NEUTRALIZING_SPORE_LAUNCHER = ITEMS.register("neutralizing_spore_launcher",
            () -> new NeutralizingSporeLauncherItem(new Item.Properties().durability(128)));

    public static final DeferredItem<Item> SYMBIOTIC_BLADE = ITEMS.register("symbiotic_blade",
            () -> new SymbioticBladeItem(new Item.Properties().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F))));

    public static final DeferredItem<Item> SPORE_ANTIDOTE = ITEMS.register("spore_antidote",
            () -> new SporeAntidoteItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> PURIFIER_GRENADE = ITEMS.register("purifier_grenade",
            () -> new PurifierGrenadeItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> SYNAPTIC_SUPPRESSOR = ITEMS.register("synaptic_suppressor",
            () -> new SynapticSuppressorItem(new Item.Properties().durability(8).rarity(Rarity.EPIC)));

    // Biotechnology & Symbiosis (v0.3.0)
    public static final DeferredItem<Item> BIO_EXTRACTOR = ITEMS.register("bio_extractor",
            () -> new BioExtractorItem(new Item.Properties().durability(64)));

    public static final DeferredItem<Item> GENETIC_SAMPLE = ITEMS.register("genetic_sample",
            () -> new GeneticSampleItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> SYMBIOTIC_MUTAGEN = ITEMS.register("symbiotic_mutagen",
            () -> new SymbioticMutagenItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> COUNTER_SERUM = ITEMS.register("counter_serum",
            () -> new CounterSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> CONCENTRATED_ANTIDOTE = ITEMS.register("concentrated_antidote",
            () -> new ConcentratedAntidoteItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // Reactive Chitin Armor (v0.4.0)
    public static final DeferredItem<Item> CHITIN_HELMET = ITEMS.register("chitin_helmet", chitinArmor(ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> CHITIN_CHESTPLATE = ITEMS.register("chitin_chestplate", chitinArmor(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> CHITIN_LEGGINGS = ITEMS.register("chitin_leggings", chitinArmor(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> CHITIN_BOOTS = ITEMS.register("chitin_boots", chitinArmor(ArmorItem.Type.BOOTS));

    // Deep Mycelium & Overmind Relics (v0.5.0)
    public static final DeferredItem<Item> RIFT_CATALYST = ITEMS.register("rift_catalyst",
            () -> new RiftCatalystItem(new Item.Properties().durability(8).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> OVERMIND_ESSENCE = ITEMS.register("overmind_essence",
            material("overmind_essence", new Item.Properties().rarity(Rarity.EPIC).stacksTo(16), ChatFormatting.DARK_PURPLE, ChatFormatting.AQUA));

    public static final DeferredItem<Item> HYPHAL_VEIN = ITEMS.register("hyphal_vein",
            material("hyphal_vein", new Item.Properties().stacksTo(64), ChatFormatting.DARK_PURPLE));

    public static final DeferredItem<Item> OVERMIND_CROWN = ITEMS.register("overmind_crown",
            () -> new OvermindCrownItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> TIDE_BEACON = ITEMS.register("tide_beacon",
            () -> new TideBeaconItem(new Item.Properties().durability(3).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> PURIFIER_DRONE_CORE = ITEMS.register("purifier_drone_core",
            () -> new PurifierDroneCoreItem(new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON)));

    // Mycorrhizal Echoes & Hackable Nodes (v1.2.0)
    public static final DeferredItem<Item> NEURAL_ECHO_CORE = ITEMS.register("neural_echo_core",
            material("neural_echo_core", new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ChatFormatting.AQUA, ChatFormatting.GREEN));

    public static final DeferredItem<Item> SYNAPTIC_GRAFT = ITEMS.register("synaptic_graft",
            () -> new SynapticGraftItem(new Item.Properties().stacksTo(8).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> RECLAIMED_BLOOM_MELEE = ITEMS.register("reclaimed_bloom_melee", reclaimedBloom(1));
    public static final DeferredItem<Item> RECLAIMED_BLOOM_PROJECTILE = ITEMS.register("reclaimed_bloom_projectile", reclaimedBloom(2));
    public static final DeferredItem<Item> RECLAIMED_BLOOM_FIRE = ITEMS.register("reclaimed_bloom_fire", reclaimedBloom(3));
    public static final DeferredItem<Item> RECLAIMED_BLOOM_EXPLOSION = ITEMS.register("reclaimed_bloom_explosion", reclaimedBloom(4));
    public static final DeferredItem<Item> RECLAIMED_BLOOM_MAGIC = ITEMS.register("reclaimed_bloom_magic", reclaimedBloom(5));

    public static final DeferredItem<Item> NEURAL_ECHO_DECOY = ITEMS.register("neural_echo_decoy",
            () -> new NeuralEchoDecoyItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // Synaptic Schism & Chemical Diplomacy (v1.3.0)
    public static final DeferredItem<Item> SCHISM_INJECTOR = ITEMS.register("schism_injector",
            () -> new SchismInjectorItem(new Item.Properties().durability(12).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> TRUCE_PHEROMONE = ITEMS.register("truce_pheromone",
            () -> new TrucePheromoneItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> QUORUM_SEAL = ITEMS.register("quorum_seal",
            () -> new QuorumSealItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> SYNAPTIC_MIMIC_SPAWN_EGG = ITEMS.register("synaptic_mimic_spawn_egg",
            spawnEgg(ModEntities.SYNAPTIC_MIMIC, 0x3A2449, 0x38B000));

    // Modular Chimera Bio-Forge (v1.4.0)
    public static final DeferredItem<Item> CHIMERA_OVUM = ITEMS.register("chimera_ovum",
            () -> new ChimeraOvumItem(new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> MYCELIAL_CHIMERA_SPAWN_EGG = ITEMS.register("mycelial_chimera_spawn_egg",
            spawnEgg(ModEntities.MYCELIAL_CHIMERA, 0x2B193D, 0x9DE0AD));

    // Synaptic Memory & Pavlovian Deception (v1.5.0)
    public static final DeferredItem<Item> VOLATILE_ENGRAM = ITEMS.register("volatile_engram",
            () -> new VolatileEngramItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> FALSE_ENGRAM_INJECTOR = ITEMS.register("false_engram_injector",
            () -> new FalseEngramInjectorItem(new Item.Properties().durability(4).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> APEX_MATRIARCH_SPAWN_EGG = ITEMS.register("apex_matriarch_spawn_egg",
            spawnEgg(ModEntities.APEX_MATRIARCH, 0x2A0845, 0xFF3388));

    // Biological Materials & Drops
    public static final DeferredItem<Item> MYCELIAL_BIOMASS = ITEMS.register("mycelial_biomass",
            material("mycelial_biomass", new Item.Properties(), ChatFormatting.GREEN));

    public static final DeferredItem<Item> CHITIN_PLATE = ITEMS.register("chitin_plate",
            material("chitin_plate", new Item.Properties(), ChatFormatting.GRAY));

    public static final DeferredItem<Item> LATENT_SPORE = ITEMS.register("latent_spore",
            material("latent_spore", new Item.Properties(), ChatFormatting.LIGHT_PURPLE, ChatFormatting.AQUA));

    public static final DeferredItem<Item> SYNAPTIC_CORE = ITEMS.register("synaptic_core",
            material("synaptic_core", new Item.Properties().rarity(Rarity.UNCOMMON), ChatFormatting.GOLD));

    public static final DeferredItem<Item> HIVE_HEART = ITEMS.register("hive_heart",
            material("hive_heart", new Item.Properties().rarity(Rarity.EPIC).stacksTo(1), ChatFormatting.DARK_RED, ChatFormatting.GOLD));

    // Spawn Eggs
    public static final DeferredItem<Item> SPORELING_SPAWN_EGG = ITEMS.register("sporeling_spawn_egg",
            spawnEgg(ModEntities.SPORELING, 0x5B3A70, 0x9DE0AD));

    public static final DeferredItem<Item> CHITIN_BRUTE_SPAWN_EGG = ITEMS.register("chitin_brute_spawn_egg",
            spawnEgg(ModEntities.CHITIN_BRUTE, 0x3D234A, 0xE27D60));

    public static final DeferredItem<Item> MYCELIAL_STALKER_SPAWN_EGG = ITEMS.register("mycelial_stalker_spawn_egg",
            spawnEgg(ModEntities.MYCELIAL_STALKER, 0x2B193D, 0xA3F7B5));

    public static final DeferredItem<Item> ACID_SPITTER_SPAWN_EGG = ITEMS.register("acid_spitter_spawn_egg",
            spawnEgg(ModEntities.ACID_SPITTER, 0x4E5B31, 0xB8F397));

    public static final DeferredItem<Item> BURROWER_SPAWN_EGG = ITEMS.register("burrower_spawn_egg",
            spawnEgg(ModEntities.BURROWER, 0x4A3728, 0x9C6644));

    public static final DeferredItem<Item> PYRO_SHROOM_SPAWN_EGG = ITEMS.register("pyro_shroom_spawn_egg",
            spawnEgg(ModEntities.PYRO_SHROOM, 0x7A1C1C, 0xFF9F1C));

    public static final DeferredItem<Item> SYNAPSE_WEAVER_SPAWN_EGG = ITEMS.register("synapse_weaver_spawn_egg",
            spawnEgg(ModEntities.SYNAPSE_WEAVER, 0x3E2F5B, 0x00F5D4));

    public static final DeferredItem<Item> GOLIATH_CARRIER_SPAWN_EGG = ITEMS.register("goliath_carrier_spawn_egg",
            spawnEgg(ModEntities.GOLIATH_CARRIER, 0x33203A, 0x7B5E7B));

    public static final DeferredItem<Item> LEECH_PARASITE_SPAWN_EGG = ITEMS.register("leech_parasite_spawn_egg",
            spawnEgg(ModEntities.LEECH_PARASITE, 0x521945, 0xD81159));

    public static final DeferredItem<Item> COLONY_WARDEN_SPAWN_EGG = ITEMS.register("colony_warden_spawn_egg",
            spawnEgg(ModEntities.COLONY_WARDEN, 0x241623, 0xD00000));

    public static final DeferredItem<Item> HIVE_CORE_SPAWN_EGG = ITEMS.register("hive_core_spawn_egg",
            spawnEgg(ModEntities.HIVE_CORE, 0x1A0933, 0xFF007F));

    public static final DeferredItem<Item> SPORE_ARCHITECT_SPAWN_EGG = ITEMS.register("spore_architect_spawn_egg",
            spawnEgg(ModEntities.SPORE_ARCHITECT, 0x2B193D, 0xF06EFF));

    public static final DeferredItem<Item> SPORE_BEHEMOTH_SPAWN_EGG = ITEMS.register("spore_behemoth_spawn_egg",
            spawnEgg(ModEntities.SPORE_BEHEMOTH, 0x1A0410, 0xFF4D8D));

    public static final DeferredItem<Item> PURIFIER_DRONE_SPAWN_EGG = ITEMS.register("purifier_drone_spawn_egg",
            spawnEgg(ModEntities.PURIFIER_DRONE, 0x123C42, 0x2AE4CC));

    public static final DeferredItem<Item> SOVEREIGN_COMMAND_STAFF = ITEMS.register("sovereign_command_staff",
            () -> new SovereignCommandStaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> COLONY_SYMBIONT_SPAWN_EGG = ITEMS.register("colony_symbiont_spawn_egg",
            spawnEgg(ModEntities.COLONY_SYMBIONT, 0x1C3A2B, 0x4AD66D));

    // Sinkhole Ecosystem Expansion
    public static final DeferredItem<Item> ROOT_SAP_VIAL = ITEMS.register("root_sap_vial",
            () -> new RootSapVialItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> CANOPY_MEMBRANE = ITEMS.register("canopy_membrane",
            material("canopy_membrane", new Item.Properties(), ChatFormatting.AQUA));

    public static final DeferredItem<Item> ROOT_HEART_RESIN = ITEMS.register("root_heart_resin",
            material("root_heart_resin", new Item.Properties().rarity(Rarity.UNCOMMON), ChatFormatting.GOLD, ChatFormatting.GREEN));

    public static final DeferredItem<Item> RESONANT_SAP_ELIXIR = ITEMS.register("resonant_sap_elixir",
            () -> new ResonantSapElixirItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> SPORE_GLIDER_SPAWN_EGG = ITEMS.register("spore_glider_spawn_egg",
            spawnEgg(ModEntities.SPORE_GLIDER, 0x26547C, 0x72EFDD));

    public static final DeferredItem<Item> ROOT_KEEPER_SPAWN_EGG = ITEMS.register("root_keeper_spawn_egg",
            spawnEgg(ModEntities.ROOT_KEEPER, 0x3A1F47, 0xFFB703));

    private ModItems() {
    }

    private static Supplier<Item> chitinArmor(ArmorItem.Type type) {
        return () -> new ChitinArmorItem(
                ModArmorMaterials.CHITIN, type, new Item.Properties().durability(type.getDurability(32)));
    }

    private static Supplier<Item> reclaimedBloom(int variant) {
        return () -> new ReclaimedBloomItem(ModBlocks.RECLAIMED_BLOOM.get(), variant, new Item.Properties());
    }

    private static Supplier<Item> material(String name, Item.Properties properties, ChatFormatting color) {
        return () -> new TooltipMaterialItem(properties, "tooltip.mycelialmind." + name, color);
    }

    private static Supplier<Item> material(
            String name, Item.Properties properties, ChatFormatting color, ChatFormatting hintColor) {
        return () -> new TooltipMaterialItem(
                properties, "tooltip.mycelialmind." + name, color,
                "tooltip.mycelialmind." + name + ".hint", hintColor);
    }

    private static Supplier<Item> spawnEgg(
            Supplier<? extends EntityType<? extends Mob>> entityType, int primaryColor, int secondaryColor) {
        return () -> new DeferredSpawnEggItem(entityType, primaryColor, secondaryColor, new Item.Properties());
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
