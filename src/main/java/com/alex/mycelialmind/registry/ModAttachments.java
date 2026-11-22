package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.symbiosis.PlayerSymbiosisData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registers NeoForge AttachmentTypes for player symbiosis state.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MycelialMind.MODID);

    public static final Supplier<AttachmentType<PlayerSymbiosisData>> SYMBIOSIS =
            ATTACHMENT_TYPES.register("symbiosis", () -> AttachmentType.serializable(PlayerSymbiosisData::new).build());

    public static final Supplier<AttachmentType<PlayerDiplomacyData>> DIPLOMACY =
            ATTACHMENT_TYPES.register("diplomacy", () -> AttachmentType.serializable(PlayerDiplomacyData::new).copyOnDeath().build());

    public static final Supplier<AttachmentType<PlayerDiscoveryData>> DISCOVERY =
            ATTACHMENT_TYPES.register("discovery", () -> AttachmentType.serializable(PlayerDiscoveryData::new).copyOnDeath().build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
