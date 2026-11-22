package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.HyphalVeinBlockEntity;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers BlockEntityTypes for Mycelial Mind's active Synaptic Nodes.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MycelialMind.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SynapticNodeBlockEntity>> SYNAPTIC_NODE =
            BLOCK_ENTITIES.register("synaptic_node", () -> BlockEntityType.Builder.of(
                    SynapticNodeBlockEntity::new,
                    ModBlocks.MYCELIAL_NODE.get(),
                    ModBlocks.INCUBATOR_NODE.get(),
                    ModBlocks.SENTINEL_NODE.get(),
                    ModBlocks.SYMBIOTIC_NODE.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SynapticBenchBlockEntity>> SYNAPTIC_BENCH =
            BLOCK_ENTITIES.register("synaptic_bench", () -> BlockEntityType.Builder.of(
                    SynapticBenchBlockEntity::new,
                    ModBlocks.SYNAPTIC_RESEARCH_BENCH.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurificationTurretBlockEntity>> PURIFICATION_TURRET =
            BLOCK_ENTITIES.register("purification_turret", () -> BlockEntityType.Builder.of(
                    PurificationTurretBlockEntity::new,
                    ModBlocks.PURIFICATION_TURRET.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MonumentFocusBlockEntity>> MONUMENT_FOCUS =
            BLOCK_ENTITIES.register("monument_focus", () -> BlockEntityType.Builder.of(
                    MonumentFocusBlockEntity::new,
                    ModBlocks.PURIFICATION_PYLON.get(),
                    ModBlocks.HIVE_ALTAR.get(),
                    ModBlocks.MATRIARCH_ALTAR.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HyphalVeinBlockEntity>> HYPHAL_VEIN =
            BLOCK_ENTITIES.register("hyphal_vein", () -> BlockEntityType.Builder.of(
                    HyphalVeinBlockEntity::new,
                    ModBlocks.HYPHAL_VEIN.get()
            ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
