package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;

/**
 * Registers BlockEntityTypes for Mycelial Mind's active Synaptic Nodes.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MycelialMind.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SynapticNodeBlockEntity>> SYNAPTIC_NODE =
            register("synaptic_node", SynapticNodeBlockEntity::new,
                    ModBlocks.MYCELIAL_NODE, ModBlocks.INCUBATOR_NODE, ModBlocks.SENTINEL_NODE, ModBlocks.SYMBIOTIC_NODE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SynapticBenchBlockEntity>> SYNAPTIC_BENCH =
            register("synaptic_bench", SynapticBenchBlockEntity::new, ModBlocks.SYNAPTIC_RESEARCH_BENCH);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurificationTurretBlockEntity>> PURIFICATION_TURRET =
            register("purification_turret", PurificationTurretBlockEntity::new, ModBlocks.PURIFICATION_TURRET);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MonumentFocusBlockEntity>> MONUMENT_FOCUS =
            register("monument_focus", MonumentFocusBlockEntity::new,
                    ModBlocks.PURIFICATION_PYLON, ModBlocks.HIVE_ALTAR, ModBlocks.MATRIARCH_ALTAR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SovereignNexusBlockEntity>> SOVEREIGN_NEXUS =
            register("sovereign_nexus", SovereignNexusBlockEntity::new, ModBlocks.SOVEREIGN_NEXUS);

    private ModBlockEntities() {
    }

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, DeferredBlock<?>... blocks) {
        return BLOCK_ENTITIES.register(name, () -> {
            Block[] validBlocks = Arrays.stream(blocks).map(DeferredBlock::get).toArray(Block[]::new);
            return BlockEntityType.Builder.of(factory, validBlocks).build(null);
        });
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
