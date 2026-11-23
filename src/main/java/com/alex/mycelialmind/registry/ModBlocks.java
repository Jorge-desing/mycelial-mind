package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.BioluminescentMycelialCapBlock;
import com.alex.mycelialmind.block.CreepingMyceliumBlock;
import com.alex.mycelialmind.block.EphemeralChitinBlock;
import com.alex.mycelialmind.block.HiveAltarBlock;
import com.alex.mycelialmind.block.HyphalVeinBlock;
import com.alex.mycelialmind.block.IncubatorNodeBlock;
import com.alex.mycelialmind.block.MatriarchAltarBlock;
import com.alex.mycelialmind.block.MycelialNodeBlock;
import com.alex.mycelialmind.block.MycelialRiftBlock;
import com.alex.mycelialmind.block.MycelialTendrilsBlock;
import com.alex.mycelialmind.block.PurificationPylonBlock;
import com.alex.mycelialmind.block.PurificationTurretBlock;
import com.alex.mycelialmind.block.ReinforcedBioBarrierBlock;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.block.SentinelNodeBlock;
import com.alex.mycelialmind.block.SovereignNexusBlock;
import com.alex.mycelialmind.block.SporeBulbBlock;
import com.alex.mycelialmind.block.SporeVentBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.block.SynapticResearchBenchBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registers all territory, infection, and containment blocks for Mycelial Mind.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MycelialMind.MODID);

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
            () -> new MycelialNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 12.0F)
                    .sound(ModSounds.SYNAPTIC_NODE_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 9)));

    public static final DeferredBlock<Block> INCUBATOR_NODE = registerBlock("incubator_node",
            () -> new IncubatorNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(5.5F, 12.0F)
                    .sound(ModSounds.SYNAPTIC_NODE_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 10)));

    public static final DeferredBlock<Block> SENTINEL_NODE = registerBlock("sentinel_node",
            () -> new SentinelNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(4.0F, 16.0F)
                    .sound(ModSounds.SYNAPTIC_NODE_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 11)));

    public static final DeferredBlock<Block> SYMBIOTIC_NODE = registerBlock("symbiotic_node",
            () -> new SymbioticNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(4.5F, 18.0F)
                    .sound(ModSounds.SYNAPTIC_NODE_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 10)));

    public static final DeferredBlock<Block> SOVEREIGN_NEXUS = registerBlock("sovereign_nexus",
            () -> new SovereignNexusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(6.5F, 36.0F)
                    .sound(ModSounds.SYNAPTIC_NODE_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 12)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PURIFICATION_PYLON = registerBlock("purification_pylon",
            () -> new PurificationPylonBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.0F, 24.0F)
                    .sound(ModSounds.PURIFICATION_TECH_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 12)));

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
            () -> new SynapticResearchBenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5F, 12.0F)
                    .sound(ModSounds.PURIFICATION_TECH_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 7)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PURIFICATION_TURRET = registerBlock("purification_turret",
            () -> new PurificationTurretBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.0F, 24.0F)
                    .sound(ModSounds.PURIFICATION_TECH_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 11)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BIOLUMINESCENT_MYCELIAL_CAP = registerBlock("bioluminescent_mycelial_cap",
            () -> new BioluminescentMycelialCapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)
                    .lightLevel(s -> s.getValue(BioluminescentMycelialCapBlock.MATURE) ? 10 : 5)));

    public static final DeferredBlock<ReclaimedBloomBlock> RECLAIMED_BLOOM = registerBlock("reclaimed_bloom",
            () -> new ReclaimedBloomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .noLootTable()
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)));

    public static final DeferredBlock<Block> SPORE_BULB = registerBlock("spore_bulb",
            () -> new SporeBulbBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.4F)
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)
                    .lightLevel(s -> 6)));

    public static final DeferredBlock<Block> MYCELIAL_TENDRILS = registerBlock("mycelial_tendrils",
            () -> new MycelialTendrilsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)
                    .lightLevel(s -> s.getValue(MycelialTendrilsBlock.MATURE) ? 8 : 3)));

    public static final DeferredBlock<Block> MYCELIAL_STONE = registerBlock("mycelial_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5F, 8.0F)
                    .sound(ModSounds.MYCELIAL_STONE_SOUNDS)
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
            () -> new StructuralRelicPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(4.0F, 12.0F)
                    .sound(ModSounds.CHITIN_RELIC_SOUNDS)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MYCELIAL_ARCH_BRICK = registerBlock("mycelial_arch_brick",
            () -> new StructuralRelicBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.5F, 18.0F)
                    .sound(ModSounds.MYCELIAL_STONE_SOUNDS)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BIO_CHITIN_LATTICE = registerBlock("bio_chitin_lattice",
            () -> new StructuralRelicBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.5F, 10.0F)
                    .sound(ModSounds.CHITIN_RELIC_SOUNDS)
                    .noOcclusion()
                    .lightLevel(s -> 10)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> LUMINESCENT_AMBER_VEIN = registerBlock("luminescent_amber_vein",
            () -> new StructuralRelicBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 14.0F)
                    .sound(ModSounds.SYNAPTIC_NODE_SOUNDS)
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

    public static final DeferredBlock<Block> HYPHAL_VEIN = registerBlock("hyphal_vein",
            () -> new HyphalVeinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollission()
                    .replaceable()
                    .strength(0.2F)
                    .sound(ModSounds.MYCELIAL_FLORA_SOUNDS)
                    .pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        registerBlockItem(name, block);
        return block;
    }

    private static <T extends Block> DeferredItem<BlockItem> registerBlockItem(String name, DeferredBlock<T> block) {
        return ModItems.ITEMS.register(name, () -> name.equals("reclaimed_bloom")
                ? new com.alex.mycelialmind.item.ReclaimedBloomItem(
                        (ReclaimedBloomBlock) block.get(), 0, new Item.Properties())
                : new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private static final class StructuralRelicBlock extends Block {
        private StructuralRelicBlock(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.mycelialmind.block.structural_relic").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private static final class StructuralRelicPillarBlock extends RotatedPillarBlock {
        private StructuralRelicPillarBlock(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.mycelialmind.block.structural_relic").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
