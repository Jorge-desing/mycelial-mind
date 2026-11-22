package com.alex.mycelialmind.compat.sbl;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.MycorrhizalAcousticTracker;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;

import java.util.Comparator;
import java.util.List;

/**
 * SmartBrainLib sensor synchronizing HiveMindSavedData, HiveBlackboard, and PurificationZoneTracker
 * into each MycelialMob's Brain memory modules.
 */
public class HiveMindSensor extends ExtendedSensor<MycelialMob> {
    private static final List<MemoryModuleType<?>> MEMORIES = List.of(
            ModMemoryTypes.SHARED_TARGET_POS.get(),
            ModMemoryTypes.HIVE_ALERT_LEVEL.get(),
            ModMemoryTypes.NEAREST_NODE_POS.get(),
            ModMemoryTypes.NEAREST_PYLON_POS.get(),
            ModMemoryTypes.HIVE_ORDER.get(),
            ModMemoryTypes.NEARBY_ALLY_COUNT.get(),
            ModMemoryTypes.MOB_COMBAT_XP.get(),
            ModMemoryTypes.LAST_ATTACK_STIMULUS.get()
    );

    public HiveMindSensor() {
        setScanRate(entity -> 20);
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return MEMORIES;
    }

    @Override
    public SensorType<? extends ExtendedSensor<?>> type() {
        return ModMemoryTypes.HIVE_MIND_SENSOR.get();
    }

    @Override
    public void doTick(ServerLevel level, MycelialMob entity) {
        Brain<?> brain = entity.getBrain();

        LivingEntity currentTarget = entity.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            HiveBlackboard.reportTarget(level, currentTarget.blockPosition());
            MycelialSblBridge.addCombatXp(level, entity, null, 2);
        }

        int allies = level.getEntitiesOfClass(MycelialMob.class, entity.getBoundingBox().inflate(12.0D),
                ally -> ally != entity && ally.isAlive()).size();
        brain.setMemory(ModMemoryTypes.NEARBY_ALLY_COUNT.get(), allies);

        BlockPos sharedTarget = HiveBlackboard.getRecentTarget(level);
        if (sharedTarget != null) {
            brain.setMemory(ModMemoryTypes.SHARED_TARGET_POS.get(), sharedTarget);
        } else {
            brain.eraseMemory(ModMemoryTypes.SHARED_TARGET_POS.get());
        }

        HiveMindSavedData hive = HiveMindSavedData.get(level);
        HiveBlackboard.updateStrategicState(level, hive);
        float alert = (float) Math.min(1.0, hive.getThreatLevel() / 100.0);
        if (hive.isFungalTideActive(level.getGameTime())) {
            alert = Math.min(1.0F, alert + 0.25F);
        }
        brain.setMemory(ModMemoryTypes.HIVE_ALERT_LEVEL.get(), alert);
        brain.setMemory(ModMemoryTypes.HIVE_ORDER.get(), HiveBlackboard.getOrder(level).name());

        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos origin = entity.blockPosition();
        List<PurifierDroneEntity> nearbyDrones = level.getEntitiesOfClass(
                PurifierDroneEntity.class,
                entity.getBoundingBox().inflate(20.0D),
                PurifierDroneEntity::isAlive
        );
        nearbyDrones.stream()
                .min(Comparator.comparingDouble(drone -> entity.distanceToSqr(drone)))
                .ifPresent(drone -> HiveBlackboard.getInfluenceMap(level)
                        .addInfluence(drone.blockPosition(), -0.10F, 0.35F, 0.0F));
        if (tracker.hasNearbySymbioticNode(origin, 20)) {
            HiveBlackboard.getInfluenceMap(level).addInfluence(origin, -0.12F, 0.20F, 0.0F);
        }

        if (sharedTarget == null) {
            BlockPos decoyPos = MycorrhizalAcousticTracker.getNearestActiveDecoy(level, origin, 20);
            if (decoyPos != null) {
                brain.setMemory(ModMemoryTypes.SHARED_TARGET_POS.get(), decoyPos);
            } else {
                var hottest = hive.getHottestSpot();
                if (hottest != null && hottest.intensity() >= 15.0
                        && hottest.pos().distSqr(origin) <= 4096.0D
                        && !tracker.isProtected(hottest.pos())) {
                    brain.setMemory(ModMemoryTypes.SHARED_TARGET_POS.get(), hottest.pos());
                }
            }
        }
        boolean rallying = HiveBlackboard.getOrder(level) == com.alex.mycelialmind.hive.HiveOrder.RALLY_NODE;
        BlockPos rallyPos = rallying ? HiveBlackboard.getRallyNodePos(level) : null;
        if (rallyPos != null && (!isLoadedNode(level, rallyPos)
                || tracker.getNodeType(rallyPos) == com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC
                || tracker.isProtected(rallyPos))) {
            rallyPos = null;
        }
        BlockPos nearestNode = rallyPos;
        if (nearestNode == null) {
            nearestNode = rallying
                    ? tracker.findNearestEnemyNode(origin, 4096, pos -> isLoadedNode(level, pos))
                    : tracker.findNearestEnemyNode(origin);
        }
        if (nearestNode != null) {
            brain.setMemory(ModMemoryTypes.NEAREST_NODE_POS.get(), nearestNode);
        } else {
            brain.eraseMemory(ModMemoryTypes.NEAREST_NODE_POS.get());
        }

        List<BlockPos> pylons = tracker.getPylonsWithin(origin, 48, 1);
        if (!pylons.isEmpty()) {
            brain.setMemory(ModMemoryTypes.NEAREST_PYLON_POS.get(), pylons.get(0));
        } else {
            brain.eraseMemory(ModMemoryTypes.NEAREST_PYLON_POS.get());
        }
    }

    private static boolean isLoadedNode(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof SynapticNodeBlockEntity;
    }
}
