package com.alex.mycelialmind.client.hud;

import com.alex.mycelialmind.MycelialMind;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Tracks the boss bars actually drawn in the current GUI frame. */
@EventBusSubscriber(modid = MycelialMind.MODID, value = Dist.CLIENT)
public final class BossOverlaySpace {
    private static int bottom;

    private BossOverlaySpace() {}

    public static int bottom() { return bottom; }

    @SubscribeEvent
    public static void beforeLayer(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.BOSS_OVERLAY.equals(event.getName())) bottom = 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!event.isCanceled()) bottom = Math.max(bottom, event.getY() + 5);
    }
}
