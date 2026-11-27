package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

/**
 * Exposes optional built-in server datapacks ({@code hardcore_overmind} and {@code relaxed_research})
 * via {@link AddPackFindersEvent}, disabled by default so vanilla balance remains unchanged unless opted in.
 * Skipped under {@link GameTestHooks#isGametestServer()} because {@code GameTestServer} forcibly activates
 * every discovered {@code PackType.SERVER_DATA} pack simultaneously.
 */
public final class MycelialBuiltInPacks {
    private MycelialBuiltInPacks() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || GameTestHooks.isGametestServer()) {
            return;
        }
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "packs/hardcore_overmind"),
                PackType.SERVER_DATA,
                Component.translatable("pack.mycelialmind.hardcore_overmind.title"),
                PackSource.FEATURE,
                false,
                Pack.Position.TOP
        );
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "packs/relaxed_research"),
                PackType.SERVER_DATA,
                Component.translatable("pack.mycelialmind.relaxed_research.title"),
                PackSource.FEATURE,
                false,
                Pack.Position.TOP
        );
    }
}
