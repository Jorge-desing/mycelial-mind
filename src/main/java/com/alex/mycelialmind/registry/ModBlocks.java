package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.BioChitinLatticeBlock;
import com.alex.mycelialmind.block.BioluminescentMycelialCapBlock;
import com.alex.mycelialmind.block.ChitinPillarBlock;
import com.alex.mycelialmind.block.CreepingMyceliumBlock;
import com.alex.mycelialmind.block.EphemeralChitinBlock;
import com.alex.mycelialmind.block.HiveAltarBlock;
import com.alex.mycelialmind.block.IncubatorNodeBlock;
import com.alex.mycelialmind.block.LuminescentAmberVeinBlock;
import com.alex.mycelialmind.block.MatriarchAltarBlock;
import com.alex.mycelialmind.block.MycelialArchBrickBlock;
import com.alex.mycelialmind.block.MycelialNodeBlock;
import com.alex.mycelialmind.block.MycelialRiftBlock;
import com.alex.mycelialmind.block.MycelialStoneBlock;
import com.alex.mycelialmind.block.MycelialTendrilsBlock;
import com.alex.mycelialmind.block.PurificationPylonBlock;
import com.alex.mycelialmind.block.PurificationTurretBlock;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.block.ReinforcedBioBarrierBlock;
import com.alex.mycelialmind.block.RootCapShelfBlock;
import com.alex.mycelialmind.block.RootSapMossBlock;
import com.alex.mycelialmind.block.SentinelNodeBlock;
import com.alex.mycelialmind.block.SovereignNexusBlock;
import com.alex.mycelialmind.block.SporeBulbBlock;
import com.alex.mycelialmind.block.SporeUpdraftVentBlock;
import com.alex.mycelialmind.block.SporeVentBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.block.SynapticResearchBenchBlock;
import com.alex.mycelialmind.item.ReclaimedBloomItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers all territory, infection, and containment blocks for Mycelial Mind.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MycelialMind.MODID);
    public static final TagKey<Block> HIVE_PROTECTED_BLOCKS =
            BlockTags.create(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_protected_blocks"));

    public static final DeferredBlock<Block> CREEPING_MYCELIUM = registerBlock("creeping_mycelium",
            () -> new CreepingMyceliumBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.8F)
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)
                    .randomTicks()));

    public static final DeferredBlock<Block> SPORE_VENT = registerBlock("spore_vent",
            () -> new SporeVentBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(2.0F)
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 4)));

    public static final DeferredBlock<Block> MYCELIAL_NODE = registerBlock("mycelial_node",
            () -> new MycelialNodeBlock(synapticNodeProps(MapColor.COLOR_PURPLE, 5.0F, 12.0F, 9)));

    public static final DeferredBlock<Block> INCUBATOR_NODE = registerBlock("incubator_node",
            () -> new IncubatorNodeBlock(synapticNodeProps(MapColor.COLOR_MAGENTA, 5.5F, 12.0F, 10)));

    public static final DeferredBlock<Block> SENTINEL_NODE = registerBlock("sentinel_node",
            () -> new SentinelNodeBlock(synapticNodeProps(MapColor.COLOR_BLUE, 4.0F, 16.0F, 11)));

    public static final DeferredBlock<Block> SYMBIOTIC_NODE = registerBlock("symbiotic_node",
            () -> new SymbioticNodeBlock(synapticNodeProps(MapColor.COLOR_LIGHT_GREEN, 4.5F, 18.0F, 10)));

    public static final DeferredBlock<Block> SOVEREIGN_NEXUS = registerBlock("sovereign_nexus",
            () -> new SovereignNexusBlock(synapticNodeProps(MapColor.COLOR_LIGHT_GREEN, 6.5F, 36.0F, 12)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PURIFICATION_PYLON = registerBlock("purification_pylon",
            () -> new PurificationPylonBlock(purificationTechProps(4.0F, 24.0F, 12)));

    public static final DeferredBlock<Block> REINFORCED_BIO_BARRIER = registerBlock("reinforced_bio_barrier",
            () -> new ReinforcedBioBarrierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(18.0F, 1200.0F)
                    .sound(ModSounds.MYCELIAL_STONE_SOUNDS)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> HIVE_ALTAR = registerBlock("hive_altar",
            () -> new HiveAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(10.0F, 36.0F)
                    .sound(ModSounds.HIVE_ALTAR_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 11)));

    public static final DeferredBlock<Block> SYNAPTIC_RESEARCH_BENCH = registerBlock("synaptic_research_bench",
            () -> new SynapticResearchBenchBlock(purificationTechProps(3.5F, 12.0F, 7)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PURIFICATION_TURRET = registerBlock("purification_turret",
            () -> new PurificationTurretBlock(purificationTechProps(4.0F, 24.0F, 11)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BIOLUMINESCENT_MYCELIAL_CAP = registerBlock("bioluminescent_mycelial_cap",
            () -> new BioluminescentMycelialCapBlock(delicateFloraProps(MapColor.COLOR_LIGHT_BLUE)
                    .instabreak()
                    .lightLevel(s -> s.getValue(BioluminescentMycelialCapBlock.MATURE) ? 10 : 5)));

    public static final DeferredBlock<ReclaimedBloomBlock> RECLAIMED_BLOOM = registerBlock("reclaimed_bloom",
            () -> new ReclaimedBloomBlock(delicateFloraProps(MapColor.COLOR_LIGHT_GREEN)
                    .instabreak()
                    .noLootTable()),
            b -> new ReclaimedBloomItem(b, 0, new Item.Properties()));

    public static final DeferredBlock<Block> SPORE_BULB = registerBlock("spore_bulb",
            () -> new SporeBulbBlock(delicateFloraProps(MapColor.COLOR_MAGENTA)
                    .strength(0.4F)
                    .lightLevel(s -> 6)));

    public static final DeferredBlock<Block> MYCELIAL_TENDRILS = registerBlock("mycelial_tendrils",
            () -> new MycelialTendrilsBlock(delicateFloraProps(MapColor.COLOR_CYAN)
                    .instabreak()
                    .lightLevel(s -> s.getValue(MycelialTendrilsBlock.MATURE) ? 8 : 3)));

    public static final DeferredBlock<Block> MYCELIAL_STONE = registerBlock("mycelial_stone",
            () -> new MycelialStoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5F, 8.0F)
                    .sound(ModSounds.MYCELIAL_STONE_SOUNDS)
                    .randomTicks()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MYCELIAL_RIFT = registerBlock("mycelial_rift",
            () -> new MycelialRiftBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .noCollission()
                    .noOcclusion()
                    .strength(6.0F, 36.0F)
                    .sound(ModSounds.HIVE_ALTAR_SOUNDS)
                    .lightLevel(s -> 13)));

    public static final DeferredBlock<Block> CHITIN_PILLAR = registerBlock("chitin_pillar",
            () -> new ChitinPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(4.0F, 12.0F)
                    .sound(ModSounds.CHITIN_RELIC_SOUNDS)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MYCELIAL_ARCH_BRICK = registerBlock("mycelial_arch_brick",
            () -> new MycelialArchBrickBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.5F, 18.0F)
                    .sound(ModSounds.MYCELIAL_STONE_SOUNDS)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BIO_CHITIN_LATTICE = registerBlock("bio_chitin_lattice",
            () -> new BioChitinLatticeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.5F, 10.0F)
                    .sound(ModSounds.CHITIN_RELIC_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 10)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> LUMINESCENT_AMBER_VEIN = registerBlock("luminescent_amber_vein",
            () -> new LuminescentAmberVeinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 14.0F)
                    .sound(ModSounds.SYNAPTIC_NODE_SOUNDS)
                    .randomTicks()
                    .lightLevel(s -> 13)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MATRIARCH_ALTAR = registerBlock("matriarch_altar",
            () -> new MatriarchAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(25.0F, 1200.0F)
                    .sound(ModSounds.HIVE_ALTAR_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 14)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> EPHEMERAL_CHITIN = registerBlock("ephemeral_chitin",
            () -> new EphemeralChitinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.4F, 1.0F)
                    .sound(ModSounds.CHITIN_RELIC_SOUNDS)
                    .noOcclusion()
                    .noLootTable()));

    public static final DeferredBlock<Block> ANTIBODY_CYST = registerBlock("antibody_cyst",
            () -> new AntibodyCystBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(2.2F, 8.0F)
                    .sound(ModSounds.ANTIBODY_CYST_SOUNDS)
                    .randomTicks()
                    .lightLevel(s -> s.getValue(AntibodyCystBlock.ACTIVE) ? 5 : 2)));

    public static final DeferredBlock<Block> SPORE_UPDRAFT_VENT = registerBlock("spore_updraft_vent",
            () -> new SporeUpdraftVentBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.8F, 10.0F)
                    .sound(ModSounds.MYCELIAL_STONE_SOUNDS)
                    .lightLevel(s -> s.getValue(SporeUpdraftVentBlock.ACTIVE) ? 8 : 3)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> ROOT_SAP_MOSS = registerBlock("root_sap_moss",
            () -> new RootSapMossBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(1.0F, 3.0F)
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)
                    .randomTicks()
                    .lightLevel(s -> s.getValue(RootSapMossBlock.SAP_READY) ? 7 : 2)));

    public static final DeferredBlock<Block> ROOT_CAP_SHELF = registerBlock("root_cap_shelf",
            () -> new RootCapShelfBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(0.9F, 4.0F)
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 4)));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties synapticNodeProps(
            MapColor color, float destroyTime, float explosionResistance, int lightLevel) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(destroyTime, explosionResistance)
                .sound(ModSounds.SYNAPTIC_NODE_SOUNDS)
                .noOcclusion()
                .lightLevel(s -> lightLevel);
    }

    private static BlockBehaviour.Properties purificationTechProps(
            float destroyTime, float explosionResistance, int lightLevel) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(destroyTime, explosionResistance)
                .sound(ModSounds.PURIFICATION_TECH_SOUNDS)
                .noOcclusion()
                .lightLevel(s -> lightLevel);
    }

    private static BlockBehaviour.Properties delicateFloraProps(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .noCollission()
                .noOcclusion()
                .sound(ModSounds.MYCELIAL_FLORA_SOUNDS);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        return registerBlock(name, blockSupplier, b -> new BlockItem(b, new Item.Properties()));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(
            String name, Supplier<T> blockSupplier, Function<T, ? extends BlockItem> itemFactory) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> itemFactory.apply(block.get()));
        return block;
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
