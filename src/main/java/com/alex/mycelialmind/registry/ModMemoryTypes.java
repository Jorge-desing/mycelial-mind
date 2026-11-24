package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * Custom Brain MemoryModuleTypes and SensorTypes for the shared Hive Mind blackboard.
 */
public final class ModMemoryTypes {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_TYPES =
            DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, MycelialMind.MODID);

    public static final DeferredRegister<SensorType<?>> SENSOR_TYPES =
            DeferredRegister.create(Registries.SENSOR_TYPE, MycelialMind.MODID);

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> SHARED_TARGET_POS =
            MEMORY_TYPES.register("shared_target_pos", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Float>> HIVE_ALERT_LEVEL =
            MEMORY_TYPES.register("hive_alert_level", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> NEAREST_NODE_POS =
            MEMORY_TYPES.register("nearest_node_pos", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> STALKER_FLANK_POS =
            MEMORY_TYPES.register("stalker_flank_pos", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> WEAVER_SUPPORT_POS =
            MEMORY_TYPES.register("weaver_support_pos", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> NEAREST_PYLON_POS =
            MEMORY_TYPES.register("nearest_pylon_pos", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<String>> HIVE_ORDER =
            MEMORY_TYPES.register("hive_order", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> MOB_COMBAT_XP =
            MEMORY_TYPES.register("mob_combat_xp", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<String>> LAST_ATTACK_STIMULUS =
            MEMORY_TYPES.register("last_attack_stimulus", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> NEARBY_ALLY_COUNT =
            MEMORY_TYPES.register("nearby_ally_count", () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<SensorType<?>, SensorType<HiveMindSensor>> HIVE_MIND_SENSOR =
            SENSOR_TYPES.register("hive_mind_sensor", () -> new SensorType<>(HiveMindSensor::new));

    private ModMemoryTypes() {
    }

    public static void register(IEventBus modEventBus) {
        MEMORY_TYPES.register(modEventBus);
        SENSOR_TYPES.register(modEventBus);
    }
}
