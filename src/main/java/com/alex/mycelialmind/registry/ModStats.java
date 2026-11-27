package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom vanilla-integrated player statistics surfaced in Minecraft's Statistics screen.
 */
public final class ModStats {
    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS =
            DeferredRegister.create(Registries.CUSTOM_STAT, MycelialMind.MODID);

    public static final DeferredHolder<ResourceLocation, ResourceLocation> NODES_DESTROYED =
            registerStat("nodes_destroyed");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> SAMPLES_EXTRACTED =
            registerStat("samples_extracted");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> SYNAPTIC_SYNTHESES =
            registerStat("synaptic_syntheses");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> MUTAGENS_IMPLANTED =
            registerStat("mutagens_implanted");
    public static final DeferredHolder<ResourceLocation, ResourceLocation> CYCLES_COMPLETED =
            registerStat("cycles_completed");

    private ModStats() {
    }

    private static DeferredHolder<ResourceLocation, ResourceLocation> registerStat(String path) {
        return CUSTOM_STATS.register(path, () -> ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, path));
    }

    public static void register(IEventBus modEventBus) {
        CUSTOM_STATS.register(modEventBus);
        modEventBus.addListener(ModStats::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Stats.CUSTOM.get(NODES_DESTROYED.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(SAMPLES_EXTRACTED.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(SYNAPTIC_SYNTHESES.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(MUTAGENS_IMPLANTED.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(CYCLES_COMPLETED.get(), StatFormatter.DEFAULT);
        });
    }
}
