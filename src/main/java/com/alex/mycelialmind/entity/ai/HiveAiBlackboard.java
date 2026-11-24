package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.AcidSpitterEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SynapseWeaverEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Typed zero-allocation Blackboard fields class for gdxAI BehaviorTree + Utility AI.
 * Separates perception facts, persistent cooldown deadlines, and execution-local counters.
 */
public final class HiveAiBlackboard {
    public enum TacticalIntent {
        NONE,
        REGROUP,
        CHARGE,
        KITE,
        FLANK,
        PATROL,
        DESYNC_SCATTER,
        FRENZY_CHARGE,
        PHALANX_GUARD
    }

    // Perception facts (written by sensor / caller before tree.step())
    public float health01 = 1.0F;
    public float distToTarget = 0.0F;
    public boolean hasTarget = false;
    public boolean hasSharedTarget = false;
    public boolean hasNearestNode = false;
    public boolean nearPylon = false;
    public float hiveAlert01 = 0.0F;
    public float influenceThreat01 = 0.0F;
    public float influenceControl01 = 0.0F;
    public float influencePheromone01 = 0.0F;
    public boolean fungalTideActive = false;
    public boolean isRangedOrKiter = false;
    public boolean canRallyOrder = false;
    public boolean desynced = false;
    public boolean frenzyActive = false;
    public boolean phalanxActive = false;
    public int nearbyAllies = 0;
    public long gameTime = 0L;

    // Persistent cooldown deadlines (must survive BT node Reset())
    public long chargeCooldownUntil = 0L;
    public long kiteCooldownUntil = 0L;

    // Output tactical intent & diagnostic lifecycle counters
    public TacticalIntent activeIntent = TacticalIntent.NONE;
    public int runningTicks = 0;
    public int resetCount = 0;

    public void populateFromMob(MycelialMob mob, @Nullable LivingEntity target) {
        this.health01 = Mth.clamp(mob.getHealth() / Math.max(1.0F, mob.getMaxHealth()), 0.0F, 1.0F);
        this.hasTarget = target != null && target.isAlive();
        this.distToTarget = this.hasTarget ? (float) Math.sqrt(mob.distanceToSqr(target)) : 0.0F;
        this.hasSharedTarget = HiveBrainBridge.getSharedTargetPos(mob) != null;

        BlockPos node = HiveBrainBridge.getNearestNodePos(mob);
        this.hasNearestNode = node != null;
        BlockPos pylon = HiveBrainBridge.getNearestPylonPos(mob);
        this.nearPylon = pylon != null && mob.blockPosition().distSqr(pylon) < 400.0D;

        this.hiveAlert01 = HiveBrainBridge.getHiveAlertLevel(mob);
        this.nearbyAllies = HiveBrainBridge.getNearbyAllyCount(mob);
        this.canRallyOrder = HiveBrainBridge.getHiveOrder(mob) == HiveOrder.RALLY_NODE;
        this.isRangedOrKiter = mob instanceof AcidSpitterEntity || mob instanceof SynapseWeaverEntity;
        this.desynced = mob.isDesynced();
        this.frenzyActive = mob.isFrenzyActive();
        this.phalanxActive = mob.isPhalanxActive();

        if (mob.level() instanceof ServerLevel serverLevel) {
            this.gameTime = serverLevel.getGameTime();
            var cell = HiveBlackboard.getInfluenceMap(serverLevel).sample(mob.blockPosition());
            this.influenceThreat01 = cell.threat();
            this.influenceControl01 = cell.control();
            this.influencePheromone01 = cell.pheromone();
            this.fungalTideActive = HiveMindSavedData.get(serverLevel).isFungalTideActive(this.gameTime);
        }
    }
}
