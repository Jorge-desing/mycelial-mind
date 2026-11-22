package com.alex.mycelialmind.world;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;

import java.util.List;

/**
 * Manages NeoForge chunk-loading tickets for active, unsmothered Tier 3 Purification Pylons.
 */
public final class MycelialChunkTicketManager {
    public static final TicketController PYLON_TICKET_CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "purification_pylon_anchor"),
            MycelialChunkTicketManager::validateTickets
    );

    private MycelialChunkTicketManager() {
    }

    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        event.register(PYLON_TICKET_CONTROLLER);
    }

    public static boolean shouldAnchorPylon(ServerLevel level, BlockPos pos) {
        if (!MycelialConfig.PYLON_TIER3_CHUNK_LOADING_ENABLED.get()) {
            return false;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        if (tracker.isPylonSmothered(pos)) {
            return false;
        }
        if (!level.getBlockState(pos).is(ModBlocks.PURIFICATION_PYLON.get())) {
            return false;
        }
        int tier = (level.getBlockEntity(pos) instanceof MonumentFocusBlockEntity focus)
                ? focus.getTier()
                : tracker.getPylonTier(pos);
        return tier >= 3;
    }

    public static void syncPylonChunkTicket(ServerLevel level, BlockPos pos, boolean anchor) {
        boolean effective = anchor && MycelialConfig.PYLON_TIER3_CHUNK_LOADING_ENABLED.get();
        ChunkPos cp = new ChunkPos(pos);
        PYLON_TICKET_CONTROLLER.forceChunk(level, pos.immutable(), cp.x, cp.z, effective, true);
    }

    public static void syncPylonChunkTicket(ServerLevel level, BlockPos pos) {
        syncPylonChunkTicket(level, pos, shouldAnchorPylon(level, pos));
    }

    private static void validateTickets(ServerLevel level, TicketHelper ticketHelper) {
        if (!MycelialConfig.PYLON_TIER3_CHUNK_LOADING_ENABLED.get()) {
            for (BlockPos owner : List.copyOf(ticketHelper.getBlockTickets().keySet())) {
                ticketHelper.removeAllTickets(owner);
            }
            return;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        for (BlockPos owner : List.copyOf(ticketHelper.getBlockTickets().keySet())) {
            boolean keep = tracker.getPylonTier(owner) >= 3 && !tracker.isPylonSmothered(owner);
            if (!keep) {
                ticketHelper.removeAllTickets(owner);
            }
        }
    }
}
