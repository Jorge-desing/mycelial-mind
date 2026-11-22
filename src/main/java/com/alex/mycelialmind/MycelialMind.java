package com.alex.mycelialmind;

import com.alex.mycelialmind.command.MycelialCommands;
import com.alex.mycelialmind.compat.vanilla.MycelialVanillaIntegration;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.hive.DamageTelemetryTracker;
import com.alex.mycelialmind.hive.HabitTelemetryTracker;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveWaveDirector;
import com.alex.mycelialmind.hive.HyphalSiegeDirector;
import com.alex.mycelialmind.hive.MycorrhizalAcousticTracker;
import com.alex.mycelialmind.network.ModNetworking;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.registry.ModArmorMaterials;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModCreativeTabs;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModFeatures;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModLootModifiers;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModMenuTypes;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.registry.ModStructures;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Main entrypoint for the Mycelial Mind mod.
 */
@Mod(MycelialMind.MODID)
public class MycelialMind {
    public static final String MODID = "mycelialmind";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MycelialMind(IEventBus modEventBus, ModContainer modContainer) {
        ModSounds.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModMemoryTypes.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModStructures.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModParticles.register(modEventBus);
        ModEffects.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModNetworking.register(modEventBus);
        MycelialVanillaIntegration.register(modEventBus);
        modEventBus.addListener(com.alex.mycelialmind.world.MycelialChunkTicketManager::onRegisterTicketControllers);

        modContainer.registerConfig(ModConfig.Type.SERVER, MycelialConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, MycelialClientConfig.SPEC);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            com.alex.mycelialmind.client.ClientConfigScreenRegistrar.register(modContainer);
        }

        NeoForge.EVENT_BUS.register(DamageTelemetryTracker.class);
        NeoForge.EVENT_BUS.register(HabitTelemetryTracker.class);
        NeoForge.EVENT_BUS.register(HiveBlackboard.class);
        NeoForge.EVENT_BUS.register(HiveWaveDirector.class);
        NeoForge.EVENT_BUS.register(HyphalSiegeDirector.class);
        NeoForge.EVENT_BUS.register(MycorrhizalAcousticTracker.class);
        NeoForge.EVENT_BUS.register(DimensionalHomeostasisDirector.class);
        NeoForge.EVENT_BUS.register(MycelialCommands.class);
        NeoForge.EVENT_BUS.addListener(com.alex.mycelialmind.data.CodexReloadListener::onAddReloadListeners);
        LOGGER.info("Mycelial Mind initialized.");
    }
}
