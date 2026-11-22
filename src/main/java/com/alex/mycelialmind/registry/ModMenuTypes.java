package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.menu.SynapticBenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom MenuTypes for Mycelial Mind GUIs.
 */
public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MycelialMind.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SynapticBenchMenu>> SYNAPTIC_BENCH =
            MENUS.register("synaptic_bench", () -> IMenuTypeExtension.create(SynapticBenchMenu::new));

    private ModMenuTypes() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
