package com.alex.mycelialmind.compat.journeymap;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.util.MycelialText;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.model.TextProperties;
import journeymap.api.v2.common.event.ClientEventRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Optional JourneyMap API v2 plugin rendering Purification Pylon protection perimeters,
 * Synaptic Nodes, and inflamed Deep Mycelium chunks synchronized via {@link ClientHiveCache}.
 */
@JourneyMapPlugin(apiVersion = IClientAPI.API_VERSION)
public final class MycelialJourneyMapPlugin implements IClientPlugin {
    private IClientAPI jmApi;

    @Override
    public String getModId() {
        return MycelialMind.MODID;
    }

    @Override
    public void initialize(IClientAPI clientAPI) {
        this.jmApi = clientAPI;
        ClientHiveCache.addUpdateListener(this::refreshOverlays);
        ClientEventRegistry.MAPPING_EVENT.subscribe(MycelialMind.MODID, event -> {
            if (event.getStage() == MappingEvent.Stage.MAPPING_STOPPED) {
                clearOverlays();
            } else if (event.getStage() == MappingEvent.Stage.MAPPING_STARTED) {
                refreshOverlays();
            }
        });
        MycelialMind.LOGGER.info("Registered JourneyMap v2 territorial overlay plugin.");
    }

    private void clearOverlays() {
        if (jmApi != null) {
            jmApi.removeAll(MycelialMind.MODID);
        }
    }

    private void refreshOverlays() {
        if (jmApi == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            clearOverlays();
            return;
        }
        if (!MycelialClientConfig.journeyMapOverlaysEnabled()
                || !jmApi.playerAccepts(MycelialMind.MODID, DisplayType.Polygon)) {
            clearOverlays();
            return;
        }

        ResourceKey<Level> dimension = mc.level.dimension();
        jmApi.removeAll(MycelialMind.MODID, DisplayType.Polygon);

        String groupName = Component.translatable("journeymap.mycelialmind.group").getString();
        int pylonRadius = Math.max(4, ClientHiveCache.getPylonRadius());

        for (BlockPos pylonPos : ClientHiveCache.getNearbyPylons()) {
            boolean smothered = ClientHiveCache.isPylonSmothered(pylonPos);
            int color = MycelialMapOverlayGeometry.resolvePylonColor(smothered);
            List<BlockPos> points = MycelialMapOverlayGeometry.buildCirclePolygon(
                    pylonPos, pylonRadius, MycelialMapOverlayGeometry.DEFAULT_PYLON_SEGMENTS);
            ShapeProperties shape = new ShapeProperties()
                    .setFillColor(color)
                    .setFillOpacity(smothered ? 0.14F : 0.16F)
                    .setStrokeColor(color)
                    .setStrokeOpacity(0.82F)
                    .setStrokeWidth(2.0F);
            String label = Component.translatable(
                    smothered
                            ? "journeymap.mycelialmind.pylon_smothered"
                            : "journeymap.mycelialmind.pylon_active",
                    pylonRadius).getString();
            showPolygon(dimension, groupName, label, shape, points, color, 1);
        }

        for (HiveSyncPayload.NodeMarker marker : ClientHiveCache.getNearbyNodes()) {
            int color = MycelialMapOverlayGeometry.resolveNodeColor(marker.type());
            List<BlockPos> points = MycelialMapOverlayGeometry.buildNodeDiamondPolygon(marker.pos(), 3);
            ShapeProperties shape = new ShapeProperties()
                    .setFillColor(color)
                    .setFillOpacity(0.55F)
                    .setStrokeColor(0x111111)
                    .setStrokeOpacity(0.9F)
                    .setStrokeWidth(1.5F);
            String label = Component.translatable(
                    "journeymap.mycelialmind.node",
                    MycelialText.nodeType(marker.type()).getString()).getString();
            showPolygon(dimension, groupName, label, shape, points, color, 2);
        }

        int anchorY = ClientHiveCache.getPlayerAnchorPos().getY();
        for (HiveSyncPayload.InflamedChunkMarker chunk : ClientHiveCache.getInflamedChunks()) {
            int color = MycelialMapOverlayGeometry.resolveInflamedChunkColor(chunk.stageId());
            List<BlockPos> points = MycelialMapOverlayGeometry.buildChunkRectPolygon(
                    chunk.chunkX(), chunk.chunkZ(), anchorY);
            ShapeProperties shape = new ShapeProperties()
                    .setFillColor(color)
                    .setFillOpacity(0.22F)
                    .setStrokeColor(color)
                    .setStrokeOpacity(0.85F)
                    .setStrokeWidth(2.0F);
            String label = Component.translatable(
                    "journeymap.mycelialmind.inflamed_chunk",
                    chunk.stressPercent()).getString();
            showPolygon(dimension, groupName, label, shape, points, color, 0);
        }
    }

    private void showPolygon(
            ResourceKey<Level> dimension,
            String groupName,
            String label,
            ShapeProperties shape,
            List<BlockPos> points,
            int textColor,
            int displayOrder
    ) {
        if (points.size() < 3) {
            return;
        }
        try {
            PolygonOverlay overlay = new PolygonOverlay(
                    MycelialMind.MODID,
                    dimension,
                    shape,
                    new MapPolygon(points)
            );
            overlay.setOverlayGroupName(groupName)
                    .setTitle(label)
                    .setLabel(label)
                    .setDisplayOrder(displayOrder)
                    .setActiveUIs(Context.UI.all())
                    .setActiveMapTypes(Context.MapType.all())
                    .setTextProperties(new TextProperties()
                            .setColor(textColor)
                            .setFontShadow(true)
                            .setScale(0.85F));
            jmApi.show(overlay);
        } catch (Exception ex) {
            MycelialMind.LOGGER.debug("Failed to update JourneyMap overlay '{}': {}", label, ex.toString());
        }
    }
}
