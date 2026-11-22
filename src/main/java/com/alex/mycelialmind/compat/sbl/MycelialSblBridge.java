package com.alex.mycelialmind.compat.sbl;

import com.alex.mycelialmind.entity.ApexMatriarchEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetWalkTargetToAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;

import java.util.List;

/**
 * Optional SmartBrainLib (SBL) modular AI bridge for MycelialMob.
 * Safely isolated so Mycelial Mind runs with or without SBL installed.
 */
public final class MycelialSblBridge {
    private MycelialSblBridge() {
    }

    public static boolean isSblLoaded() {
        return ModList.get().isLoaded("smartbrainlib");
    }

    public static List<ExtendedSensor<MycelialMob>> createDefaultHiveSensors() {
        return List.of(
                new NearbyLivingEntitySensor<MycelialMob>()
                        .setPredicate((target, entity) -> target instanceof Player && !target.isSpectator()),
                new HurtBySensor<>(),
                new HiveMindSensor()
        );
    }

    @Nullable
    public static BlockPos getSharedTargetPos(MycelialMob mob) {
        return mob.getBrain().getMemory(ModMemoryTypes.SHARED_TARGET_POS.get()).orElse(null);
    }

    public static float getHiveAlertLevel(MycelialMob mob) {
        return mob.getBrain().getMemory(ModMemoryTypes.HIVE_ALERT_LEVEL.get()).orElse(0.0F);
    }

    @Nullable
    public static BlockPos getNearestNodePos(MycelialMob mob) {
        return mob.getBrain().getMemory(ModMemoryTypes.NEAREST_NODE_POS.get()).orElse(null);
    }

    @Nullable
    public static BlockPos getNearestPylonPos(MycelialMob mob) {
        return mob.getBrain().getMemory(ModMemoryTypes.NEAREST_PYLON_POS.get()).orElse(null);
    }

    public static com.alex.mycelialmind.hive.HiveOrder getHiveOrder(MycelialMob mob) {
        return mob.getBrain()
                .getMemory(ModMemoryTypes.HIVE_ORDER.get())
                .map(com.alex.mycelialmind.hive.HiveOrder::fromName)
                .orElse(com.alex.mycelialmind.hive.HiveOrder.NONE);
    }

    public static int getNearbyAllyCount(MycelialMob mob) {
        return mob.getBrain().getMemory(ModMemoryTypes.NEARBY_ALLY_COUNT.get()).orElse(0);
    }

    public static int getCombatXp(MycelialMob mob) {
        return mob.getBrain().getMemory(ModMemoryTypes.MOB_COMBAT_XP.get()).orElse(0);
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
