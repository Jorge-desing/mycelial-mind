package com.alex.mycelialmind.entity.ai.brain.hive;

import com.alex.mycelialmind.entity.ApexMatriarchEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AnimatableMeleeAttack;
import com.alex.mycelialmind.entity.ai.brain.behaviour.Idle;
import com.alex.mycelialmind.entity.ai.brain.behaviour.InvalidateAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.LookAtTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.MoveToWalkTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetPlayerLookTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetRandomLookTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetRandomWalkTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.SetWalkTargetToAttackTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.TargetOrRetaliate;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.FirstApplicableBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.core.OneRandomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.HurtBySensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyLivingEntitySensor;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveOrder;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Native HiveBrain facade for {@link MycelialMob}: provides typed blackboard memory accessors,
 * combat XP self-promotion, and default sensor/activity presets.
 */
public final class HiveBrainBridge {
    private HiveBrainBridge() {
    }

    public static List<HiveSensor<MycelialMob>> createDefaultHiveSensors() {
        return List.of(
                new NearbyLivingEntitySensor<MycelialMob>()
                        .setPredicate((target, entity) -> target instanceof Player && !target.isSpectator()),
                new HurtBySensor<>(),
                new HiveMindSensor()
        );
    }

    @Nullable
    public static BlockPos getSharedTargetPos(MycelialMob mob) {
        return HiveBrainUtils.getMemory(mob, ModMemoryTypes.SHARED_TARGET_POS.get());
    }

    public static float getHiveAlertLevel(MycelialMob mob) {
        return HiveBrainUtils.memoryOrDefault(mob, ModMemoryTypes.HIVE_ALERT_LEVEL.get(), () -> 0.0F);
    }

    @Nullable
    public static BlockPos getNearestNodePos(MycelialMob mob) {
        return HiveBrainUtils.getMemory(mob, ModMemoryTypes.NEAREST_NODE_POS.get());
    }

    @Nullable
    public static BlockPos getNearestPylonPos(MycelialMob mob) {
        return HiveBrainUtils.getMemory(mob, ModMemoryTypes.NEAREST_PYLON_POS.get());
    }

    public static HiveOrder getHiveOrder(MycelialMob mob) {
        return mob.getBrain()
                .getMemory(ModMemoryTypes.HIVE_ORDER.get())
                .map(HiveOrder::fromName)
                .orElse(HiveOrder.NONE);
    }

    public static int getNearbyAllyCount(MycelialMob mob) {
        return HiveBrainUtils.memoryOrDefault(mob, ModMemoryTypes.NEARBY_ALLY_COUNT.get(), () -> 0);
    }

    public static int getCombatXp(MycelialMob mob) {
        return HiveBrainUtils.memoryOrDefault(mob, ModMemoryTypes.MOB_COMBAT_XP.get(), () -> 0);
    }

    @Nullable
    public static StimulusType getLastAttackStimulus(MycelialMob mob) {
        String id = HiveBrainUtils.getMemory(mob, ModMemoryTypes.LAST_ATTACK_STIMULUS.get());
        if (id == null || id.isBlank()) {
            return null;
        }
        for (StimulusType type : StimulusType.values()) {
            if (type.id().equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public static void addCombatXp(ServerLevel level, MycelialMob mob, @Nullable StimulusType stimulus, int delta) {
        if (delta <= 0) {
            return;
        }
        int nextXp = Math.min(1000, getCombatXp(mob) + delta);
        mob.getBrain().setMemory(ModMemoryTypes.MOB_COMBAT_XP.get(), nextXp);
        if (stimulus != null) {
            mob.getBrain().setMemory(ModMemoryTypes.LAST_ATTACK_STIMULUS.get(), stimulus.id());
        }

        // Exclude only main bosses (HiveCore & ApexMatriarch); mini-bosses like SporeBehemoth remain eligible
        if (mob instanceof HiveCoreBossEntity || mob instanceof ApexMatriarchEntity) {
            return;
        }

        EvolutionRank targetRank = nextXp >= 250 ? EvolutionRank.ELITE
                : (nextXp >= 100 ? EvolutionRank.MUTATED : EvolutionRank.BASE);
        if (targetRank.ordinal() > mob.getEvolutionRank().ordinal()) {
            mob.promoteRank(targetRank);
            StimulusType feedbackType = stimulus != null ? stimulus : StimulusType.MELEE;
            HiveMindSavedData.get(level).recordStimulus(feedbackType, 2.0, level.getGameTime());
        }
    }

    public static BrainActivityGroup<MycelialMob> createCoreTasks() {
        return BrainActivityGroup.coreTasks(
                new LookAtTarget<>(),
                new MoveToWalkTarget<>()
        );
    }

    public static BrainActivityGroup<MycelialMob> createIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<>(
                        new TargetOrRetaliate<MycelialMob>()
                                .attackablePredicate(target -> target instanceof LivingEntity living && living.isAlive()),
                        new SetPlayerLookTarget<>(),
                        new SetRandomLookTarget<>()
                ),
                new OneRandomBehaviour<>(
                        new HiveNodeRegenBehaviour(),
                        new HiveHotspotPatrolBehaviour(),
                        new SetRandomWalkTarget<>(),
                        new Idle<>().runFor(entity -> entity.getRandom().nextInt(30, 60))
                )
        );
    }

    public static BrainActivityGroup<MycelialMob> createFightTasks(int attackDelayTicks) {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new SetWalkTargetToAttackTarget<>(),
                new AnimatableMeleeAttack<>(attackDelayTicks)
        );
    }
}
