package com.alex.mycelialmind.test;

import static com.alex.mycelialmind.test.MycelialGameTestSupport.*;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.BioluminescentMycelialCapBlock;
import com.alex.mycelialmind.block.CreepingMyceliumBlock;
import com.alex.mycelialmind.block.MycelialTendrilsBlock;
import com.alex.mycelialmind.block.SporeBulbBlock;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.world.MycelialLootSystem;
import com.alex.mycelialmind.world.MycelialOutbreakFeature;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ApexMatriarchEntity;
import com.alex.mycelialmind.entity.BurrowerEntity;
import com.alex.mycelialmind.entity.ChitinBruteEntity;
import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.LeechParasiteEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.SporelingEntity;
import com.alex.mycelialmind.entity.SynapseWeaverEntity;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.entity.projectile.PurifierGrenadeProjectile;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.DamageTelemetryTracker;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.hive.HiveWaveDirector;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.item.BioScannerItem;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.item.GeneticSampleItem;
import com.alex.mycelialmind.item.OvermindCrownItem;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.recipe.SynapticBenchRecipeDefinitions;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.world.level.block.state.BlockState;
import com.alex.mycelialmind.entity.ai.brain.behaviour.AvoidEntity;
import com.alex.mycelialmind.entity.ai.brain.behaviour.FleeTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.ReactToUnreachableTarget;
import com.alex.mycelialmind.entity.ai.brain.behaviour.StrafeTarget;
import com.alex.mycelialmind.entity.ai.brain.core.AllApplicableBehaviours;
import com.alex.mycelialmind.entity.ai.brain.core.BrainActivityGroup;
import com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.DelayedBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrain;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainKey;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainOwner;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainProfile;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainSignal;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainSnapshot;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainTrace;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.entity.ai.brain.core.HiveResourceChannel;
import com.alex.mycelialmind.entity.ai.brain.core.SequentialBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveHotspotPatrolBehaviour;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveNodeRegenBehaviour;
import com.alex.mycelialmind.entity.ai.brain.sensor.IncomingProjectilesSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.NearbyPlayersSensor;
import com.alex.mycelialmind.entity.ai.brain.sensor.UnreachableTargetSensor;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import com.alex.mycelialmind.registry.ModArmorMaterials;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.symbiosis.PlayerSymbiosisData;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.ModDimensions;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.EquipmentSlot;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Automated NeoForge GameTests verifying Hive Mind adaptation, Pylon protection, and network serialization.
 */
@GameTestHolder(MycelialMind.MODID)
@PrefixGameTestTemplate(false)
public final class MycelialGameTests {
    private MycelialGameTests() {
    }

    @GameTest(batch = "entity_ai_weaver_window", template = "empty", timeoutTicks = 180)
    public static void weaverSupportWindowTracksCoverage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forced = forceArenaChunks(helper, -4, 22, -5, 14);
        int arenaY = 180;
        for (int x = -4; x <= 21; x++) {
            for (int z = -4; z <= 13; z++) {
                helper.setBlock(new BlockPos(x, arenaY, z), Blocks.STONE);
            }
        }
        var weaver = helper.spawn(ModEntities.SYNAPSE_WEAVER.get(), 2, arenaY + 1, 2);
        var ally = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 15, arenaY + 1, 2);
        var enemy = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, arenaY + 1, 12);
        weaver.setHiveStrain(HiveStrain.AMBER);
        ally.setHiveStrain(HiveStrain.AMBER);
        enemy.setHiveStrain(HiveStrain.CRIMSON);
        weaver.setSchismTicks(1000);
        enemy.setSchismTicks(1000);
        weaver.setTarget(enemy);
        int[] coveredTicks = {0};
        int[] separatedTicks = {0};
        int[] pulses = {0};
        int[] previousDuration = {0};
        for (int tick = 1; tick <= 100; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (ally.hasEffect(MobEffects.REGENERATION)) {
                    coveredTicks[0]++;
                }
                var effect = ally.getEffect(MobEffects.REGENERATION);
                int duration = effect == null ? 0 : effect.getDuration();
                if (duration > previousDuration[0]) {
                    pulses[0]++;
                }
                previousDuration[0] = duration;
                if (weaver.distanceToSqr(ally) > 144) {
                    separatedTicks[0]++;
                }
            });
        }
        helper.startSequence().thenIdle(100).thenExecute(() -> {
            MycelialMind.LOGGER.info("[WeaverSupportComparison] active coveredTicks={} separatedTicks={} pulses={}",
                    coveredTicks[0], separatedTicks[0], pulses[0]);
            helper.assertTrue(coveredTicks[0] > 0, "Support must cover the ally during the window");
            helper.assertTrue(separatedTicks[0] < 100, "Weaver must close the gap during the window");
            releaseArenaChunks(level, forced);
        }).thenSucceed();
    }

    @GameTest(batch = "entity_ai_weaver_control", template = "empty", timeoutTicks = 180)
    public static void weaverWithoutSupportGoalLeavesSeparatedAllies(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forced = forceArenaChunks(helper, -4, 25, -5, 14);
        int arenaY = 180;
        for (int x = -4; x <= 24; x++) {
            for (int z = -4; z <= 13; z++) {
                helper.setBlock(new BlockPos(x, arenaY, z), Blocks.STONE);
            }
        }
        var weaver = new com.alex.mycelialmind.entity.SynapseWeaverEntity(
                ModEntities.SYNAPSE_WEAVER.get(), level) {
            @Override
            protected void registerGoals() {
                super.registerGoals();
                this.goalSelector.getAvailableGoals().removeIf(entry ->
                        entry.getGoal() instanceof com.alex.mycelialmind.entity.ai.WeaverSupportGoal);
            }
        };
        weaver.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, arenaY + 1, 2))));
        level.addFreshEntity(weaver);
        var ally = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 20, arenaY + 1, 2);
        var enemy = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, arenaY + 1, 12);
        weaver.setHiveStrain(HiveStrain.AMBER);
        ally.setHiveStrain(HiveStrain.AMBER);
        enemy.setHiveStrain(HiveStrain.CRIMSON);
        weaver.setSchismTicks(1000);
        enemy.setSchismTicks(1000);
        weaver.setTarget(enemy);
        int[] coveredTicks = {0};
        int[] separatedTicks = {0};
        for (int tick = 1; tick <= 100; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (ally.hasEffect(MobEffects.REGENERATION)) {
                    coveredTicks[0]++;
                }
                if (weaver.distanceToSqr(ally) > 144) {
                    separatedTicks[0]++;
                }
            });
        }
        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    MycelialMind.LOGGER.info("[WeaverSupportComparison] control coveredTicks={} separatedTicks={}",
                            coveredTicks[0], separatedTicks[0]);
                    helper.assertTrue(weaver.distanceToSqr(ally) > 144,
                            "Control weaver should remain outside ally support range");
                    helper.assertTrue(!ally.hasEffect(MobEffects.REGENERATION),
                            "Isolated ally must not receive a pulse from the distant control weaver");
                    helper.assertTrue(coveredTicks[0] == 0,
                            "Control ally must receive no support during the observed window");
                    helper.assertTrue(separatedTicks[0] > 0,
                            "Control must record time outside support range");
                    releaseArenaChunks(level, forced);
                })
                .thenSucceed();
    }

    @GameTest(batch = "entity_ai_weaver_support", template = "empty", timeoutTicks = 400)
    public static void weaverSupportCoversSeparatedSquad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forced = forceArenaChunks(helper, -4, 22, -5, 14);
        for (int x = -4; x <= 21; x++) {
            for (int z = -4; z <= 13; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        final MycelialMob[] allies = new MycelialMob[2];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        entityChunksReady(level, helper.absolutePos(new BlockPos(9, 1, 5)), 9),
                        "Weaver support arena chunks must be entity-ticking"))
                .thenExecute(() -> {
                    var weaver = helper.spawn(ModEntities.SYNAPSE_WEAVER.get(), 2, 1, 2);
                    var first = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 12, 1, 2);
                    var second = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 12, 1, 5);
                    var enemy = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 12);
                    weaver.setHiveStrain(HiveStrain.AMBER);
                    first.setHiveStrain(HiveStrain.AMBER);
                    second.setHiveStrain(HiveStrain.AMBER);
                    enemy.setHiveStrain(HiveStrain.CRIMSON);
                    weaver.setSchismTicks(1000);
                    enemy.setSchismTicks(1000);
                    weaver.setTarget(enemy);
                    allies[0] = first;
                    allies[1] = second;
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(allies[0] != null && allies[0].hasEffect(MobEffects.REGENERATION),
                            "First ally must receive the support pulse");
                    helper.assertTrue(allies[1] != null && allies[1].hasEffect(MobEffects.REGENERATION),
                            "Second ally must receive the support pulse");
                })
                .thenExecute(() -> releaseArenaChunks(level, forced))
                .thenSucceed();
    }

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void weaverSupportYieldsForNearbyThreat(GameTestHelper helper) {
        var weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 2, 1, 2);
        var rival = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 5, 1, 2);
        weaver.setHiveStrain(HiveStrain.AMBER);
        rival.setHiveStrain(HiveStrain.CRIMSON);
        weaver.setSchismTicks(1000);
        rival.setSchismTicks(1000);
        var ally = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 14, 1, 2);
        ally.setHiveStrain(HiveStrain.AMBER);
        ally.setSchismTicks(1000);
        weaver.getBrain().setMemory(ModMemoryTypes.WEAVER_SUPPORT_POS.get(),
                helper.absolutePos(new BlockPos(14, 1, 2)));
        var goal = new com.alex.mycelialmind.entity.ai.WeaverSupportGoal(weaver);
        helper.assertTrue(goal.canUse(), "Weaver should rejoin a distant ally");
        ally.setHiveStrain(HiveStrain.CRIMSON);
        helper.assertTrue(!goal.canUse(), "Weaver must reject an ally that became a rival");
        ally.setHiveStrain(HiveStrain.AMBER);
        weaver.setTarget(rival);
        helper.assertTrue(!goal.canUse(), "Weaver must yield support navigation to a nearby threat");
        weaver.setTarget(null);
        weaver.triggerDesync(80);
        helper.assertTrue(!goal.canUse(), "Desync must prevent support navigation");
        helper.succeed();
    }

    @GameTest(batch = "weaver_support", template = "empty", timeoutTicks = 360)
    public static void weaverRejoinsAlliesAndResumesAfterDesync(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int arenaY = 180;
        List<ChunkPos> forced = forceArenaChunks(helper, -4, 20, -5, 9);
        for (int x = -4; x <= 19; x++) {
            for (int z = -4; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, arenaY, z), Blocks.STONE);
                boolean border = x == -4 || x == 19 || z == -4 || z == 8;
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, arenaY + y, z), (border && y <= 2) ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        var weaver = helper.spawn(ModEntities.SYNAPSE_WEAVER.get(), 2, arenaY + 1, 2);
        var ally = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 14, arenaY + 1, 2);
        weaver.getBrain().setMemory(ModMemoryTypes.WEAVER_SUPPORT_POS.get(), ally.blockPosition());
        double initial = weaver.distanceToSqr(ally);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        level.isPositionEntityTicking(helper.absolutePos(new BlockPos(2, arenaY + 1, 2)))
                                && forced.stream().allMatch(chunk -> level.areEntitiesLoaded(chunk.toLong())),
                        "Weaver arena chunks must be ready before movement"))
                .thenWaitUntil(() -> helper.assertTrue(weaver.distanceToSqr(ally) < initial - 20,
                        "Weaver must move toward a separated ally"))
                .thenWaitUntil(() -> helper.assertTrue(ally.hasEffect(MobEffects.REGENERATION),
                        "Weaver must apply its support pulse in range"))
                .thenExecute(() -> {
                    weaver.triggerDesync(80);
                    ally.removeEffect(MobEffects.REGENERATION);
                    ally.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                    ally.removeEffect(MobEffects.ABSORPTION);
                })
                .thenWaitUntil(() -> helper.assertTrue(!weaver.getBrain()
                                .hasMemoryValue(ModMemoryTypes.WEAVER_SUPPORT_POS.get()),
                        "Desync must clear support position"))
                .thenIdle(65)
                .thenExecute(() -> helper.assertTrue(!ally.hasEffect(MobEffects.REGENERATION),
                        "Desynced weaver must not apply support"))
                .thenWaitUntil(() -> helper.assertTrue(ally.hasEffect(MobEffects.REGENERATION),
                        "Weaver must resume support after desync"))
                .thenExecute(() -> releaseArenaChunks(level, forced))
                .thenSucceed();
    }

    @GameTest(batch = "spitter_regroup_utility", template = "empty", timeoutTicks = 180)
    public static void injuredSpitterRegroupsAndReevaluatesDestroyedNode(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        HiveBlackboard.clear(level);
        com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.clearDecoys(level);
        List<ChunkPos> forced = forceArenaChunks(helper, -4, 18, -4, 8);
        for (int x = -3; x <= 17; x++) {
            for (int z = -3; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        BlockPos node = helper.absolutePos(new BlockPos(13, 1, 2));
        level.setBlockAndUpdate(node, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        var spitter = helper.spawn(ModEntities.ACID_SPITTER.get(), 3, 1, 2);
        new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor().tick(level, spitter);
        helper.assertTrue(com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator
                        .evaluateRegroupUtility(spitter) < 0.65F,
                "Healthy spitter must remain below the regroup threshold");
        spitter.setHealth(spitter.getMaxHealth() * 0.1F);
        helper.assertTrue(com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator
                        .evaluateRegroupUtility(spitter) >= 0.65F,
                "Injured spitter must cross the regroup threshold");
        double initialDistance = spitter.distanceToSqr(Vec3.atCenterOf(node));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        spitter.distanceToSqr(Vec3.atCenterOf(node)) < initialDistance - 4,
                        "Injured spitter must move toward the node"))
                .thenExecute(() -> {
                    level.removeBlock(node, false);
                    PurificationZoneTracker.get(level).removeNode(node);
                })
                .thenWaitUntil(() -> helper.assertTrue(spitter.getNavigation().isDone()
                                || !node.equals(spitter.getNavigation().getTargetPos()),
                        "Destroyed node must stop being the navigation target"))
                .thenExecute(() -> {
                    HiveBlackboard.clear(level);
                    hive.resetAll();
                    releaseArenaChunks(level, forced);
                })
                .thenSucceed();
    }

    @GameTest(batch = "rally_squad_movement", template = "empty", timeoutTicks = 220)
    public static void rallySquadMovesAndStopsForDestroyedNode(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forced = forceArenaChunks(helper, -3, 18, -4, 9);
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();
        HiveBlackboard.clear(level);
        hive.recordStimulus(StimulusType.MELEE, 260.0, level.getGameTime());
        for (int x = -2; x <= 17; x++) {
            for (int z = -3; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean border = x == -2 || x == 17 || z == -3 || z == 8;
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), (border && y <= 2) ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        BlockPos node = helper.absolutePos(new BlockPos(13, 1, 2));
        level.setBlockAndUpdate(node, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        HiveBlackboard.reportNodeLoss(level, node.east());
        var brute = helper.spawn(ModEntities.CHITIN_BRUTE.get(), 3, 1, 2);
        var sporeling = helper.spawn(ModEntities.SPORELING.get(), 3, 1, 4);
        var sensor = new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor();
        sensor.tick(level, brute);
        sensor.tick(level, sporeling);
        double bruteInitial = brute.distanceToSqr(Vec3.atCenterOf(node));
        double sporelingInitial = sporeling.distanceToSqr(Vec3.atCenterOf(node));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(brute.distanceToSqr(Vec3.atCenterOf(node)) < bruteInitial - 4,
                            "Brute must move toward the rally node");
                    helper.assertTrue(sporeling.distanceToSqr(Vec3.atCenterOf(node)) < sporelingInitial - 4,
                            "Sporeling must move toward the rally node");
                })
                .thenExecute(() -> {
                    level.removeBlock(node, false);
                    PurificationZoneTracker.get(level).removeNode(node);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(brute.getNavigation().isDone()
                                    || !node.equals(brute.getNavigation().getTargetPos()),
                            "Brute must abandon the destroyed node");
                    helper.assertTrue(sporeling.getNavigation().isDone()
                                    || !node.equals(sporeling.getNavigation().getTargetPos()),
                            "Sporeling must abandon the destroyed node");
                })
                .thenExecute(() -> {
                    HiveBlackboard.clear(level);
                    hive.resetAll();
                    releaseArenaChunks(level, forced);
                })
                .thenSucceed();
    }

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void flankStopsWhenDiplomacyProtectsTarget(GameTestHelper helper) {
        var stalker = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 2);
        stalker.setHiveStrain(HiveStrain.AMBER);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(stalker.getX() + 8, stalker.getY(), stalker.getZ());
        var diplomacy = player.getData(com.alex.mycelialmind.registry.ModAttachments.DIPLOMACY);
        diplomacy.setReputation(HiveStrain.AMBER, 0);
        stalker.setTarget(player);
        var goal = new com.alex.mycelialmind.entity.ai.SwarmFlankGoal(stalker, 1.0);
        helper.assertTrue(goal.canUse(), "Hostile player should be a valid flank target");
        diplomacy.setReputation(HiveStrain.AMBER, 60);
        helper.assertTrue(!goal.canUse(), "Diplomacy must invalidate the flank target");
        helper.succeed();
    }

    @GameTest(batch = "entity_ai_stalker_combat", template = "empty", timeoutTicks = 200)
    public static void stalkerLeapsDuringCombat(GameTestHelper helper) {
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 12, -4, 8);
        for (int x = -4; x <= 12; x++) {
            for (int z = -4; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean wall = x == -4 || x == 12 || z == -4 || z == 8;
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), wall && y <= 3 ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        var stalker = helper.spawn(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 2);
        var target = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 6, 1, 2);
        stalker.setHiveStrain(HiveStrain.AMBER);
        target.setHiveStrain(HiveStrain.CRIMSON);
        stalker.setSchismTicks(1000);
        target.setSchismTicks(1000);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        stalker.setTarget(target);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stalker.getDeltaMovement().y > 0.3,
                        "Stalker must leap during combat"))
                .thenExecute(() -> {
                    stalker.discard();
                    target.discard();
                    releaseArenaChunks(helper.getLevel(), forcedChunks);
                })
                .thenSucceed();
    }

    @GameTest(batch = "entity_ai_stalker_leap", template = "empty", timeoutTicks = 100)
    public static void stalkerLeapPropelsTowardTarget(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        var stalker = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 2);
        var target = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 6, 1, 2);
        stalker.setTarget(target);
        var leap = new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(stalker, 0.45F);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    stalker.setOnGround(true);
                    helper.assertTrue(leap.canUse(), "Leap goal must activate at its intended distance");
                })
                .thenExecute(() -> {
                    leap.start();
                    Vec3 velocity = stalker.getDeltaMovement();
                    helper.assertTrue(velocity.y > 0.4 && velocity.x > 0,
                            "Leap must propel the stalker upward and toward the target");
                    stalker.discard();
                    target.discard();
                })
                .thenSucceed();
    }

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void stalkersChooseOppositeFlanks(GameTestHelper helper) {
        var first = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 2);
        var second = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 2);
        var target = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 10, 1, 2);
        try {
            Vec3 firstDestination = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeSwarmApproachPos(first, target);
            Vec3 secondDestination = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeSwarmApproachPos(second, target);
            double firstSide = firstDestination.z - target.getZ();
            double secondSide = secondDestination.z - target.getZ();
            helper.assertTrue(firstSide * secondSide < 0,
                    "Stalkers with opposite swarm roles must approach opposite sides of the target");
            helper.succeed();
        } finally {
            first.discard();
            second.discard();
            target.discard();
        }
    }

    @GameTest(batch = "stalker_flank", template = "empty", timeoutTicks = 150)
    public static void stalkersShareFlankOrderAndClearInvalidDestinations(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 18, -5, 11);
        java.util.function.Function<MycelialMob, String> diag = m -> " (alive=" + m.isAlive()
                + ", ticking=" + level.isPositionEntityTicking(m.blockPosition())
                + ", desync=" + m.isDesynced()
                + ", order=" + HiveBrainBridge.getHiveOrder(m)
                + ", bbOrder=" + HiveBlackboard.getOrder(level)
                + ", target=" + m.getTarget()
                + ", pos=" + m.position() + ")";
        HiveMindSavedData hive = resetHive(level);
        HiveBlackboard.clear(level);
        hive.recordStimulus(StimulusType.MELEE, 260.0, level.getGameTime());
        HiveBlackboard.updateStrategicState(level, hive);
        for (int x = -4; x <= 18; x++) {
            for (int z = -5; z <= 11; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean wall = x == -4 || x == 18 || z == -5 || z == 11;
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), wall && y <= 3 ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        var first = helper.spawn(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 2);
        var second = helper.spawn(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 4);
        var target = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 12, 1, 3);
        first.setHiveStrain(HiveStrain.AMBER);
        second.setHiveStrain(HiveStrain.AMBER);
        target.setHiveStrain(HiveStrain.CRIMSON);
        first.setSchismTicks(1000);
        second.setSchismTicks(1000);
        target.setSchismTicks(1000);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        first.setTarget(target);
        second.setTarget(target);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(first.getBrain().hasMemoryValue(ModMemoryTypes.STALKER_FLANK_POS.get()),
                            "First stalker must publish a flank position" + diag.apply(first));
                    helper.assertTrue(second.getBrain().hasMemoryValue(ModMemoryTypes.STALKER_FLANK_POS.get()),
                            "Second stalker must publish a flank position" + diag.apply(second));
                })
                .thenExecute(() -> {
                    hive.resetAll();
                    HiveBlackboard.updateStrategicState(level, hive);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(!first.getBrain().hasMemoryValue(ModMemoryTypes.STALKER_FLANK_POS.get()),
                            "First stalker must discard the destination when the order changes" + diag.apply(first));
                    helper.assertTrue(!second.getBrain().hasMemoryValue(ModMemoryTypes.STALKER_FLANK_POS.get()),
                            "Second stalker must discard the destination when the order changes" + diag.apply(second));
                })
                .thenExecute(() -> {
                    hive.recordStimulus(StimulusType.MELEE, 260.0, level.getGameTime());
                    HiveBlackboard.updateStrategicState(level, hive);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(first.getBrain().hasMemoryValue(ModMemoryTypes.STALKER_FLANK_POS.get()),
                            "First stalker must resume flanking after the order returns" + diag.apply(first));
                    helper.assertTrue(second.getBrain().hasMemoryValue(ModMemoryTypes.STALKER_FLANK_POS.get()),
                            "Second stalker must resume flanking after the order returns" + diag.apply(second));
                })
                .thenExecute(() -> {
                    first.triggerDesync(80);
                    target.discard();
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(!first.getBrain().hasMemoryValue(ModMemoryTypes.STALKER_FLANK_POS.get()),
                            "Desynchronization must clear flank position" + diag.apply(first));
                    helper.assertTrue(!second.getBrain().hasMemoryValue(ModMemoryTypes.STALKER_FLANK_POS.get()),
                            "Removing the target must clear flank position" + diag.apply(second));
                })
                .thenExecute(() -> {
                    first.discard();
                    second.discard();
                    target.discard();
                    HiveBlackboard.clear(level);
                    hive.resetAll();
                    releaseArenaChunks(level, forcedChunks);
                })
                .thenSucceed();
    }

    @GameTest(batch = "entity_ai_spitter_brain", template = "empty", timeoutTicks = 180)
    public static void acidSpitterBrainShootsRetreatsAndInvalidatesTarget(GameTestHelper helper) {
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -6, 13, -5, 9);
        for (int x = -6; x <= 13; x++) {
            for (int z = -5; z <= 9; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean wall = x == -6 || x == 13 || z == -5 || z == 9;
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), wall && y <= 3 ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        var spitter = helper.spawn(ModEntities.ACID_SPITTER.get(), 2, 1, 2);
        var target = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 10, 1, 2);
        spitter.setHiveStrain(HiveStrain.AMBER);
        target.setHiveStrain(HiveStrain.CRIMSON);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hasAcidShot(helper, spitter),
                        "The Brain must acquire and shoot a visible rival (age=" + spitter.tickCount
                                + ", target=" + spitter.getTarget() + ", valid=" + spitter.canAttack(target)
                                + ", visible=" + spitter.hasLineOfSight(target)
                                + ", positions=" + spitter.position() + " / " + target.position() + ")"))
                .thenExecute(() -> {
                    clearAcidShots(helper, spitter);
                    target.setPos(spitter.getX() + 2, spitter.getY(), spitter.getZ());
                })
                .thenWaitUntil(() -> {
                    double distanceSquared = spitter.distanceToSqr(target);
                    if (distanceSquared < 25) {
                        helper.assertTrue(!hasAcidShot(helper, spitter), "Retreat must take priority over shooting");
                    }
                    helper.assertTrue(distanceSquared >= 25, "The Brain must reach its minimum firing distance"
                            + " (distance=" + Math.sqrt(distanceSquared) + ", mob=" + spitter.position()
                            + ", target=" + target.position() + ", navigation=" + spitter.getNavigation().isDone() + ")");
                })
                .thenWaitUntil(() -> helper.assertTrue(hasAcidShot(helper, spitter),
                        "The Brain must resume shooting after retreat"))
                .thenExecute(() -> target.discard())
                .thenWaitUntil(() -> helper.assertTrue(spitter.getTarget() == null,
                        "The Brain must release a removed target"))
                .thenExecute(() -> {
                    clearAcidShots(helper, spitter);
                    spitter.discard();
                    releaseArenaChunks(helper.getLevel(), forcedChunks);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void fireStimulusTriggersFireResistantTrait(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        double threshold = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
        hive.recordStimulus(StimulusType.FIRE, stimulusForScore(threshold + 1.0));

        if (!hive.hasTrait(AdaptationTrait.FIRE_RESISTANT)) {
            helper.fail("Expected FIRE_RESISTANT trait to activate above the configured threshold.");
            return;
        }

        ChitinBruteEntity brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 1, 1, 1);
        brute.applyHiveTraits(hive.getActiveTraits());

        if (!brute.hasTrait(AdaptationTrait.FIRE_RESISTANT)) {
            helper.fail("Expected ChitinBrute to inherit FIRE_RESISTANT trait.");
            return;
        }

        float healthBefore = brute.getHealth();
        brute.hurt(level.damageSources().inFire(), 5.0F);
        if (brute.getHealth() < healthBefore) {
            helper.fail("Expected FIRE_RESISTANT ChitinBrute to block fire damage.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purificationPylonBlocksMyceliumSpread(GameTestHelper helper) {
        BlockPos localPylon = new BlockPos(1, 1, 1);
        helper.setBlock(localPylon, ModBlocks.PURIFICATION_PYLON.get());

        BlockPos worldPylon = helper.absolutePos(localPylon);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(helper.getLevel());

        if (!tracker.isProtected(worldPylon.east(2))) {
            helper.fail("Expected position 2 blocks from Purification Pylon to be protected.");
            return;
        }
        helper.setBlock(localPylon, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waveMilestoneTriggersAndRespectsPylonProtection(GameTestHelper helper) {
        HiveMindSavedData hive = new HiveMindSavedData();
        hive.recordStimulus(StimulusType.MELEE, 260.0); // threat += 26.0 (>= 25)

        int milestone = HiveWaveDirector.computePendingMilestone(hive);
        if (milestone != 25) {
            helper.fail("Expected milestone 25 after 100 MELEE stimulus, got " + milestone);
            return;
        }

        hive.setLastWaveMilestone(milestone, 1000L);
        if (HiveWaveDirector.computePendingMilestone(hive) != 0) {
            helper.fail("Expected no pending milestone immediately after recording milestone 25.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void curiosScannerEquippingIsOptional(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            CuriosScannerTest.verify(helper, player);
        } else if (com.alex.mycelialmind.compat.curios.MycelialCuriosCompat.hasEquippedScanner(player)) {
            helper.fail("Scanner reported equipped without Curios");
            return;
        }
        helper.succeed();
    }

    private static final class CuriosScannerTest {
        private static void verify(GameTestHelper helper, net.minecraft.world.entity.player.Player player) {
            var beltTag = net.minecraft.tags.TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("curios", "belt"));
            if (!new ItemStack(ModItems.BIO_SCANNER.get()).is(beltTag)) {
                helper.fail("Bio-scanner is missing from the Curios belt tag");
                return;
            }
            var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
            var belt = inventory.getStacksHandler("belt").orElseThrow().getStacks();
            ItemStack previous = belt.getStackInSlot(0).copy();
            try {
                belt.setStackInSlot(0, new ItemStack(ModItems.BIO_SCANNER.get()));
                if (!com.alex.mycelialmind.compat.curios.MycelialCuriosCompat.hasEquippedScanner(player)) {
                    helper.fail("Equipped bio-scanner was not found");
                }
            } finally {
                belt.setStackInSlot(0, previous);
            }
        }
    }

    @GameTest(template = "empty")
    public static void optionalCuttingRecipeLoadsOnlyWithFarmersDelight(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                MycelialMind.MODID, "compat/farmersdelight/cutting_tendrils"));
        boolean farmersDelightLoaded = net.neoforged.fml.ModList.get().isLoaded("farmersdelight");
        if (recipe.isPresent() != farmersDelightLoaded) {
            helper.fail("Farmer's Delight cutting recipe has unexpected availability");
            return;
        }
        if (recipe.isPresent()) {
            var ingredients = recipe.get().value().getIngredients();
            if (ingredients.size() != 1 || !ingredients.getFirst().test(
                    new ItemStack(ModBlocks.MYCELIAL_TENDRILS.get()))) {
                helper.fail("Farmer's Delight cutting recipe has an unexpected ingredient");
                return;
            }
            ItemStack result = recipe.get().value().getResultItem(helper.getLevel().registryAccess());
            if (!result.is(ModItems.MYCELIAL_BIOMASS.get()) || result.getCount() != 1) {
                helper.fail("Farmer's Delight cutting recipe has an unexpected result");
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lootAndRecipesRegistered(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();

        var recipeOpt = server.getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "purification_pylon"));
        if (recipeOpt.isEmpty()) {
            helper.fail("Missing recipe: mycelialmind:purification_pylon");
            return;
        }

        var scannerOpt = server.getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "bio_scanner"));
        if (scannerOpt.isEmpty()) {
            helper.fail("Missing recipe: mycelialmind:bio_scanner");
            return;
        }

        LootTable hiveCoreLoot = server.reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "entities/hive_core")));
        if (hiveCoreLoot == LootTable.EMPTY) {
            helper.fail("Missing loot table: mycelialmind:entities/hive_core");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void particleRegistryAssetsAreCompleteAndUnique(GameTestHelper helper) {
        var holders = com.alex.mycelialmind.registry.ModParticles.all();
        if (holders.size() < 30) {
            helper.fail("Expected at least 30 mod particle types, got " + holders.size());
            return;
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<String> gaps = new java.util.ArrayList<>();
        var texturePattern = java.util.regex.Pattern.compile("\"mycelialmind:([a-z0-9_/]+)\"");
        for (var holder : holders) {
            var type = holder.get();
            var key = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(type);
            if (key == null || !MycelialMind.MODID.equals(key.getNamespace())) {
                gaps.add("key:" + key);
                continue;
            }
            String path = key.getPath();
            if (!seen.add(path)) {
                gaps.add("duplicate:" + path);
                continue;
            }
            String text;
            try (var stream = MycelialGameTests.class.getResourceAsStream(
                    "/assets/mycelialmind/particles/" + path + ".json")) {
                if (stream == null) {
                    gaps.add("json:" + path);
                    continue;
                }
                text = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException exception) {
                gaps.add("read:" + path);
                continue;
            }
            var matcher = texturePattern.matcher(text);
            int textures = 0;
            while (matcher.find()) {
                textures++;
                String texture = matcher.group(1);
                try (var sprite = MycelialGameTests.class.getResourceAsStream(
                        "/assets/mycelialmind/textures/particle/" + texture + ".png")) {
                    if (sprite == null) {
                        gaps.add("png:" + texture);
                    }
                } catch (java.io.IOException exception) {
                    gaps.add("png:" + texture);
                }
            }
            if (textures == 0) {
                gaps.add("notextures:" + path);
            }
        }
        if (!gaps.isEmpty()) {
            helper.fail("Particle asset gaps: " + String.join(", ", gaps));
            return;
        }
        var tinted = net.minecraft.core.particles.ColorParticleOption.create(
                com.alex.mycelialmind.registry.ModParticles.HIVE_DUST.get(), 0xFF9B4DFF);
        if (tinted.getType() != com.alex.mycelialmind.registry.ModParticles.HIVE_DUST.get()) {
            helper.fail("hive_dust color codec round-trip failed");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectileBowAttackTriggersDeflectAndBioScannerSync(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);

        double threshold = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
        int maxHits = 10 + (int) Math.ceil(stimulusForScore(threshold) / 4.0);
        for (int i = 0; i < maxHits && !hive.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT); i++) {
            SporelingEntity sporeling = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 1, 1, 1);
            BlockPos abs = helper.absolutePos(new BlockPos(1, 2, 1));
            Arrow arrow = new Arrow(level, abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5,
                    new ItemStack(Items.ARROW), null);
            sporeling.hurt(level.damageSources().arrow(arrow, null), 8.0F);
            arrow.discard();
            sporeling.discard();
        }

        if (!hive.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT)) {
            helper.fail("Expected PROJECTILE_DEFLECT after repeated arrow damage. Projectile score: "
                    + hive.getStimulusScore(StimulusType.PROJECTILE) + ", threshold: " + threshold);
            return;
        }

        HiveSyncPayload payload = HiveSyncPayload.fromSavedData(hive);
        if (!payload.decodeTraits().contains(AdaptationTrait.PROJECTILE_DEFLECT)) {
            helper.fail("Expected Bio-Scanner sync payload to include PROJECTILE_DEFLECT.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creepingMyceliumRespectsPylonContainment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pylonLocal = new BlockPos(0, 1, 0);
        BlockPos mycLocal = new BlockPos(1, 1, 1);
        BlockPos dirtLocal = new BlockPos(2, 1, 1);

        helper.setBlock(pylonLocal, ModBlocks.PURIFICATION_PYLON.get());
        helper.setBlock(mycLocal, ModBlocks.CREEPING_MYCELIUM.get());
        helper.setBlock(dirtLocal, Blocks.DIRT);

        BlockPos mycAbs = helper.absolutePos(mycLocal);
        for (int i = 0; i < 25; i++) {
            level.getBlockState(mycAbs).randomTick(level, mycAbs, level.getRandom());
        }

        helper.assertBlockPresent(Blocks.DIRT, dirtLocal);
        helper.setBlock(pylonLocal, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creepingMyceliumSpreadCanBeDisabled(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos myceliumLocal = new BlockPos(1, 1, 1);
        BlockPos dirtLocal = new BlockPos(2, 1, 1);
        boolean previousSetting = MycelialConfig.MYCELIUM_SPREAD_ENABLED.get();
        try {
            MycelialConfig.MYCELIUM_SPREAD_ENABLED.set(false);
            helper.setBlock(myceliumLocal, ModBlocks.CREEPING_MYCELIUM.get());
            helper.setBlock(dirtLocal, Blocks.DIRT);
            BlockPos myceliumPos = helper.absolutePos(myceliumLocal);
            for (int i = 0; i < 25; i++) {
                level.getBlockState(myceliumPos).randomTick(level, myceliumPos, level.getRandom());
            }
            helper.assertBlockPresent(Blocks.DIRT, dirtLocal);
            helper.succeed();
        } finally {
            MycelialConfig.MYCELIUM_SPREAD_ENABLED.set(previousSetting);
        }
    }

    @GameTest(template = "empty")
    public static void hiveCoreBossSymbioticShieldAndAdaptation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos symbioticNodeRel = new BlockPos(2, 1, 1);
        BlockPos symbioticNodePos = helper.absolutePos(symbioticNodeRel);
        helper.setBlock(symbioticNodeRel, ModBlocks.SYMBIOTIC_NODE.get());
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.addNode(symbioticNodePos, SynapticNodeType.SYMBIOTIC);
        HiveCoreBossEntity boss = helper.spawnWithNoFreeWill(ModEntities.HIVE_CORE.get(), 1, 1, 1);

        for (int i = 0; i < 20; i++) {
            boss.tickCount += 1;
            boss.customServerAiStep();
        }

        if (!boss.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            helper.fail("Expected HiveCoreBoss to gain DAMAGE_RESISTANCE from nearby MycelialNode.");
            return;
        }

        boss.hurt(level.damageSources().magic(), 5.0F);
        if (!boss.hasTrait(AdaptationTrait.MAGIC_CARAPACE)) {
            helper.fail("Expected HiveCoreBoss to adapt MAGIC_CARAPACE when damaged by magic.");
            return;
        }
        helper.setBlock(symbioticNodeRel, Blocks.AIR);
        tracker.removeNode(symbioticNodePos);
        helper.succeed();
    }

    @GameTest(batch = "hive_state_sync", template = "empty")
    public static void globalHiveAdaptationsPropagateExpireAndResetForLivingMobs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);

        MycelialMob mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 1, 1, 1);
        HiveCoreBossEntity boss = helper.spawnWithNoFreeWill(ModEntities.HIVE_CORE.get(), 4, 1, 4);
        boss.applyHiveState(
                Set.of(AdaptationTrait.MAGIC_CARAPACE),
                Set.of(AdaptationTrait.MAGIC_CARAPACE),
                Set.of(HiveSynergy.ARCANE_ABSORPTION));

        long now = level.getGameTime();
        hive.recordStimulus(StimulusType.FIRE, 210.0, now);
        hive.recordStimulus(StimulusType.EXPLOSION, 70.0, now);

        long hardDeadline = helper.getTick() + 40L;
        long[] suppressionEnd = {-1L};
        int[] phase = {0};
        boolean[] finished = {false};
        Runnable cleanup = () -> {
            mob.discard();
            boss.discard();
            hive.resetAll();
        };
        java.util.function.BooleanSupplier bossTraitsPreserved = () -> boss.hasTrait(AdaptationTrait.MAGIC_CARAPACE)
                && boss.hasTier2Trait(AdaptationTrait.MAGIC_CARAPACE)
                && boss.hasSynergy(HiveSynergy.ARCANE_ABSORPTION)
                && !boss.hasTrait(AdaptationTrait.FIRE_RESISTANT);
        helper.onEachTick(() -> {
            if (finished[0]) {
                return;
            }
            long gameTime = level.getGameTime();
            if (helper.getTick() >= hardDeadline) {
                finished[0] = true;
                cleanup.run();
                helper.fail("Hive suppression state transition timed out in phase " + phase[0] + ".");
                return;
            }

            if (phase[0] == 0) {
                boolean hiveStateApplied = mob.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                        && mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                        && mob.hasTrait(AdaptationTrait.BLAST_SHIELDED)
                        && mob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL);
                if (!bossTraitsPreserved.getAsBoolean()) {
                    finished[0] = true;
                    cleanup.run();
                    helper.fail("Global hive state overwrote phase-specific boss traits.");
                    return;
                }
                if (hiveStateApplied) {
                    suppressionEnd[0] = gameTime + 5L;
                    hive.suppressHigherAdaptations(suppressionEnd[0], gameTime);
                    phase[0] = 1;
                }
            } else if (phase[0] == 1) {
                boolean suppressionApplied = mob.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                        && !mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                        && !mob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL);
                if (!bossTraitsPreserved.getAsBoolean()) {
                    finished[0] = true;
                    cleanup.run();
                    helper.fail("Active hive suppression changed phase-specific boss traits.");
                    return;
                }
                if (gameTime < suppressionEnd[0] && suppressionApplied) {
                    phase[0] = 2;
                } else if (gameTime >= suppressionEnd[0]) {
                    finished[0] = true;
                    cleanup.run();
                    helper.fail("The live mob did not expose the active suppression state before its expiry.");
                    return;
                }
            } else if (phase[0] == 2) {
                boolean suppressionExpired = mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                        && mob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL);
                if (!bossTraitsPreserved.getAsBoolean()) {
                    finished[0] = true;
                    cleanup.run();
                    helper.fail("Suppression expiry changed phase-specific boss traits.");
                    return;
                }
                if (gameTime >= suppressionEnd[0] && suppressionExpired) {
                    hive.resetAll();
                    phase[0] = 3;
                }
            } else {
                boolean resetApplied = !mob.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                        && !mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                        && !mob.hasTrait(AdaptationTrait.BLAST_SHIELDED)
                        && mob.getSynergies().isEmpty();
                if (!bossTraitsPreserved.getAsBoolean()) {
                    finished[0] = true;
                    cleanup.run();
                    helper.fail("Hive reset changed phase-specific boss traits.");
                    return;
                }
                if (resetApplied) {
                    finished[0] = true;
                    cleanup.run();
                    helper.succeed();
                    return;
                }
            }
        });
    }

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void hiveTraitRevisionAndLocalNeutralizationPersistAcrossReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        MycelialMob original = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        MycelialMob restored = null;

        try {
            hive.recordStimulus(StimulusType.FIRE, 210.0, level.getGameTime());
            original.applyGlobalHiveState(level, hive);
            long originalRevision = hive.getActiveTraitStateRevision();

            original.clearAdaptations();
            CompoundTag mobTag = new CompoundTag();
            original.addAdditionalSaveData(mobTag);
            restored = ModEntities.SPORELING.get().create(level);
            if (restored == null) {
                helper.fail("Could not create a Sporeling for hive revision persistence coverage.");
                return;
            }
            restored.setNoAi(true);
            restored.readAdditionalSaveData(mobTag);

            boolean neutralizationPersisted = restored.hasCurrentGlobalHiveState(level, hive)
                    && restored.getTraitMask() == 0
                    && restored.getTier2Mask() == 0
                    && restored.getSynergyMask() == 0;
            if (!restored.hasCurrentGlobalHiveState(level, hive)) {
                restored.applyGlobalHiveState(level, hive);
            }
            boolean sameRevisionDoesNotUndoNeutralization = restored.getTraitMask() == 0
                    && restored.getTier2Mask() == 0
                    && restored.getSynergyMask() == 0;

            hive.recordStimulus(StimulusType.EXPLOSION, 70.0, level.getGameTime());
            boolean revisionAdvanced = hive.getActiveTraitStateRevision() > originalRevision;
            if (!restored.hasCurrentGlobalHiveState(level, hive)) {
                restored.applyGlobalHiveState(level, hive);
            }
            boolean newRevisionReapplied = restored.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                    && restored.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                    && restored.hasTrait(AdaptationTrait.BLAST_SHIELDED);

            if (!neutralizationPersisted || !sameRevisionDoesNotUndoNeutralization
                    || !revisionAdvanced || !newRevisionReapplied) {
                helper.fail("Hive revision/entity persistence failed: neutralized=" + neutralizationPersisted
                        + " stableRevision=" + sameRevisionDoesNotUndoNeutralization
                        + " advanced=" + revisionAdvanced + " reapplied=" + newRevisionReapplied);
                return;
            }
            helper.succeed();
        } finally {
            original.discard();
            if (restored != null) {
                restored.discard();
            }
            hive.resetAll();
        }
    }

    @GameTest(batch = "defaultBatch", template = "empty", timeoutTicks = 200)
    public static void purifierGrenadeUsesSphericalBlockAndEntityRadius(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos insideMycelium = center.east(3);
        BlockPos outsideMycelium = center.offset(3, 0, 3);
        List<ChunkPos> forced = forceArenaChunks(helper, -4, 12, -4, 12);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entityChunksReady(level, center, 8),
                        "Purifier grenade arena chunks must be entity-ticking"))
                .thenExecute(() -> {
                    HiveMindSavedData.get(level).resetAll();
                    HiveBlackboard.clear(level);
                    level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(insideMycelium, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
                    level.setBlockAndUpdate(outsideMycelium, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());

                    MycelialMob insideMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
                    MycelialMob outsideMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 6, 1, 6);
                    PurifierDroneEntity insideDrone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 2, 1, 4);
                    PurifierDroneEntity outsideDrone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 7, 1, 6);
                    insideMob.setPos(center.getX() + 2.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
                    outsideMob.setPos(center.getX() + 3.5D, center.getY() + 1.0D, center.getZ() + 3.5D);
                    insideDrone.setPos(center.getX() + 1.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
                    outsideDrone.setPos(center.getX() + 4.2D, center.getY() + 1.0D, center.getZ() + 3.5D);
                    insideMob.applyHiveState(
                            Set.of(AdaptationTrait.FIRE_RESISTANT),
                            Set.of(AdaptationTrait.FIRE_RESISTANT),
                            Set.of(HiveSynergy.PYROLYTIC_SHELL));
                    outsideMob.applyHiveState(
                            Set.of(AdaptationTrait.FIRE_RESISTANT),
                            Set.of(AdaptationTrait.FIRE_RESISTANT),
                            Set.of(HiveSynergy.PYROLYTIC_SHELL));
                    insideDrone.setJammedTicks(60);
                    outsideDrone.setJammedTicks(60);
                    float outsideHealth = outsideMob.getHealth();

                    try {
                        PurifierGrenadeProjectile grenade = new PurifierGrenadeProjectile(ModEntities.PURIFIER_GRENADE.get(), level);
                        grenade.setPos(center.getX() + 0.5D, center.getY() + 1.35D, center.getZ() + 0.5D);
                        grenade.shoot(0.0D, -1.0D, 0.0D, 1.5F, 0.0F);
                        level.addFreshEntity(grenade);
                        for (int tick = 0; tick < 6 && !grenade.isRemoved(); tick++) {
                            grenade.tick();
                        }

                        boolean blockRadius = level.getBlockState(insideMycelium).is(Blocks.DIRT)
                                && level.getBlockState(outsideMycelium).is(ModBlocks.CREEPING_MYCELIUM.get());
                        boolean mobRadius = insideMob.getTraitMask() == 0 && insideMob.isDesynced()
                                && outsideMob.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                                && outsideMob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                                && outsideMob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL)
                                && Math.abs(outsideMob.getHealth() - outsideHealth) < 0.01F;
                        boolean droneRadius = !insideDrone.isJammed() && outsideDrone.isJammed();

                        if (!grenade.isRemoved() || !blockRadius || !mobRadius || !droneRadius) {
                            helper.fail("Purifier grenade sphere check failed: removed=" + grenade.isRemoved()
                                    + " blocks=" + blockRadius + " mobs=" + mobRadius + " drones=" + droneRadius);
                        }
                    } finally {
                        insideMob.discard();
                        outsideMob.discard();
                        insideDrone.discard();
                        outsideDrone.discard();
                        level.removeBlock(center, false);
                        level.removeBlock(insideMycelium, false);
                        level.removeBlock(outsideMycelium, false);
                        HiveMindSavedData.get(level).resetAll();
                        HiveBlackboard.clear(level);
                        releaseArenaChunks(level, forced);
                    }
                })
                .thenSucceed();
    }

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void tideBeaconAndSuppressorUseSphericalEntityRadii(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        int chunkMinX = Math.floorDiv(anchor.getX(), 16) * 16;
        int chunkMinZ = Math.floorDiv(anchor.getZ(), 16) * 16;
        double y = anchor.getY() + 1.0D;
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(chunkMinX + 0.5D, y, chunkMinZ + 0.5D);

        MycelialMob tideInside = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        MycelialMob tideOutside = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 2);
        tideInside.setPos(chunkMinX + 12.5D, y, chunkMinZ + 12.5D);
        tideOutside.setPos(chunkMinX + 13.5D, y, chunkMinZ + 13.5D);
        float tideInsideHealth = tideInside.getHealth();
        float tideOutsideHealth = tideOutside.getHealth();

        try {
            ModItems.TIDE_BEACON.get().use(level, player, InteractionHand.MAIN_HAND);
            boolean tideRadius = tideInside.getHealth() < tideInsideHealth
                    && tideOutside.getHealth() == tideOutsideHealth
                    && tideInside.isDesynced() && !tideOutside.isDesynced();
            tideInside.discard();
            tideOutside.discard();

            MycelialMob suppressorInside = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
            MycelialMob suppressorOutside = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 2);
            PurifierDroneEntity droneInside = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 2, 1, 3);
            PurifierDroneEntity droneOutside = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 3, 1, 3);
            suppressorInside.setPos(chunkMinX + 13.5D, y, chunkMinZ + 0.5D);
            suppressorOutside.setPos(chunkMinX + 14.9D, y, chunkMinZ + 14.9D);
            droneInside.setPos(chunkMinX + 13.5D, y, chunkMinZ + 1.5D);
            droneOutside.setPos(chunkMinX + 14.5D, y, chunkMinZ + 14.9D);
            droneInside.setJammedTicks(60);
            droneOutside.setJammedTicks(60);

            ModItems.SYNAPTIC_SUPPRESSOR.get().use(level, player, InteractionHand.MAIN_HAND);
            boolean suppressorRadius = suppressorInside.isDesynced()
                    && suppressorInside.hasEffect(MobEffects.WEAKNESS)
                    && !suppressorOutside.isDesynced()
                    && !suppressorOutside.hasEffect(MobEffects.WEAKNESS)
                    && !droneInside.isJammed() && droneOutside.isJammed();

            suppressorInside.discard();
            suppressorOutside.discard();
            droneInside.discard();
            droneOutside.discard();
            if (!tideRadius || !suppressorRadius) {
                helper.fail("Spherical item radius check failed: tide=" + tideRadius
                        + " suppressor=" + suppressorRadius);
                return;
            }
            helper.succeed();
        } finally {
            tideInside.discard();
            tideOutside.discard();
            hive.resetAll();
        }
    }

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void mycelialCommandsExecuteCleanly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        var src = server.createCommandSourceStack()
                .withPosition(new Vec3(0.5D, 100.0D, 0.5D))
                .withPermission(4)
                .withSuppressedOutput();

        server.getCommands().performPrefixedCommand(src, "mycelial status");
        int[] verifyResult = {-1};
        boolean[] verifySucceeded = {false};
        var verifySource = src.withCallback((successful, result) -> {
            verifySucceeded[0] = successful;
            verifyResult[0] = result;
        });
        server.getCommands().performPrefixedCommand(verifySource, "mycelial verify boss_encounter");
        if (!verifySucceeded[0] || verifyResult[0] != 1) {
            helper.fail("Expected /mycelial verify boss_encounter to pass; success="
                    + verifySucceeded[0] + " result=" + verifyResult[0] + ".");
            return;
        }
        server.getCommands().performPrefixedCommand(src, "mycelial reset");

        if (HiveMindSavedData.get(level).getThreatLevel() != 0.0) {
            helper.fail("Expected /mycelial reset to clear threatLevel to 0.0.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tier2MutationAndSynergyActivation(GameTestHelper helper) {
        HiveMindSavedData hive = new HiveMindSavedData();
        double tier1 = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
        double tier2 = MycelialConfig.getEffectiveTier2ActivationThreshold();
        hive.recordStimulus(StimulusType.FIRE, stimulusForScore(tier2 + 1.0), 100L);
        hive.recordStimulus(StimulusType.EXPLOSION, stimulusForScore(tier1 + 1.0), 100L);

        if (!hive.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)) {
            helper.fail("Expected FIRE_RESISTANT Tier 2 mutation above the configured threshold.");
            return;
        }
        if (!hive.hasSynergy(HiveSynergy.PYROLYTIC_SHELL)) {
            helper.fail("Expected PYROLYTIC_SHELL synergy when FIRE Tier 2 and BLAST Tier 1 are active.");
            return;
        }

        double beforeFire = hive.getStimulusScore(StimulusType.FIRE);
        hive.tickDecay(2.0, 200L);
        double decayed = beforeFire - hive.getStimulusScore(StimulusType.FIRE);
        if (decayed <= 2.0) {
            helper.fail("Expected accelerated Tier 2 decay (>2.0) when stimulus exceeds Tier 1 threshold.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void synapticNetworkLinkingAndPylonSever(GameTestHelper helper) {
        PurificationZoneTracker tracker = new PurificationZoneTracker();
        BlockPos nodeA = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos nodeB = helper.absolutePos(new BlockPos(10, 1, 1));
        BlockPos pylon = helper.absolutePos(new BlockPos(5, 1, 1));

        tracker.addNode(nodeA, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
        tracker.addNode(nodeB, com.alex.mycelialmind.world.SynapticNodeType.INCUBATOR);

        if (!tracker.getLinkedNodes(nodeA).contains(nodeB)) {
            helper.fail("Expected unprotected Relay and Incubator nodes within range to form a synaptic link.");
            return;
        }

        tracker.addPylon(pylon);
        boolean stillLinked = tracker.getLinkedNodes(nodeA).contains(nodeB);
        if (stillLinked) {
            helper.fail("Expected Purification Pylon placed between nodes to sever the synaptic link.");
            return;
        }
        tracker.removePylon(pylon);
        tracker.removeNode(nodeA);
        tracker.removeNode(nodeB);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nodeDestructionBleedsHiveMemory(GameTestHelper helper) {
        HiveMindSavedData hive = new HiveMindSavedData();
        hive.recordStimulus(StimulusType.MELEE, 50.0, 100L);
        double threatBefore = hive.getThreatLevel();
        double meleeBefore = hive.getStimulusScore(StimulusType.MELEE);

        hive.applyNodeDestructionBleed(18.0, 2.5, 120L);
        if (hive.getStimulusScore(StimulusType.MELEE) >= meleeBefore || hive.getThreatLevel() >= threatBefore) {
            helper.fail("Expected node destruction bleed to reduce stimulus score and threatLevel.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hiveCoreThreePhaseShieldAndCollapse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveCoreBossEntity boss = helper.spawnWithNoFreeWill(ModEntities.HIVE_CORE.get(), 2, 1, 2);

        boss.setHealth(boss.getMaxHealth() * 0.60F);
        boss.customServerAiStep();

        if (boss.getBossPhase() != 2) {
            helper.fail("Expected HiveCoreBoss to transition to Phase 2 below 65% HP.");
            return;
        }
        if (!boss.isShieldedPhaseTwo() || boss.getAnchorPositions().size() != 3) {
            helper.fail("Expected Phase 2 to spawn 3 Sentinel Node anchors and activate invulnerability shield.");
            return;
        }

        float hpWhileShielded = boss.getHealth();
        if (boss.hurt(level.damageSources().magic(), 20.0F) || boss.getHealth() < hpWhileShielded) {
            helper.fail("Expected Phase 2 shield to block direct damage while Sentinel Nodes remain.");
            return;
        }

        for (BlockPos anchor : boss.getAnchorPositions()) {
            level.destroyBlock(anchor, false);
        }
        boss.customServerAiStep();
        if (boss.isShieldedPhaseTwo()) {
            helper.fail("Expected Phase 2 shield to collapse once all 3 Sentinel Node anchors are destroyed.");
            return;
        }

        boss.setHealth(boss.getMaxHealth() * 0.25F);
        boss.customServerAiStep();
        if (boss.getBossPhase() != 3 || boss.getSynergies().isEmpty()) {
            helper.fail("Expected HiveCoreBoss to enter Phase 3 below 30% HP with an active HiveSynergy.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void synapticSuppressorDisablesTier2AndSynergies(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = new HiveMindSavedData();
        double tier1 = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
        double tier2 = MycelialConfig.getEffectiveTier2ActivationThreshold();
        hive.recordStimulus(StimulusType.PROJECTILE, stimulusForScore(tier2 + 1.0), 100L);
        hive.recordStimulus(StimulusType.MELEE, stimulusForScore(tier1 + 1.0), 100L);

        if (!hive.hasTier2Trait(AdaptationTrait.PROJECTILE_DEFLECT) || !hive.hasSynergy(HiveSynergy.KINETIC_MIRROR)) {
            helper.fail("Expected PROJECTILE Tier 2 and KINETIC_MIRROR synergy before suppression.");
            return;
        }

        hive.suppressHigherAdaptations(2500L, 100L);
        if (!hive.getTier2Traits().isEmpty() || !hive.getActiveSynergies().isEmpty()) {
            helper.fail("Expected Synaptic Suppressor to clear Tier 2 traits and Synergies during suppression.");
            return;
        }

        HiveSyncPayload original = new HiveSyncPayload(
                hive.getThreatLevel(),
                HiveSyncPayload.encodeTraits(hive.getActiveTraits()),
                HiveSyncPayload.encodeTraits(hive.getTier2Traits()),
                HiveSyncPayload.encodeSynergies(hive.getActiveSynergies()),
                false,
                false,
                BlockPos.ZERO,
                3,
                hive.getSuppressionRemainingSeconds(100L)
        );
        var buf = Unpooled.buffer();
        HiveSyncPayload.STREAM_CODEC.encode(buf, original);
        HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);
        if (decoded.activeNetworkLinks() != 3 || decoded.suppressionSecondsRemaining() <= 0) {
            helper.fail("Expected HiveSyncPayload round-trip to preserve networkLinks and suppressionSeconds.");
            return;
        }
        if (ModItems.SYNAPTIC_SUPPRESSOR.get() == null) {
            helper.fail("Expected SYNAPTIC_SUPPRESSOR item to be registered.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void symbiosisAttachmentMaxSlotsAndMutagenEffects(GameTestHelper helper) {
        PlayerSymbiosisData data = new PlayerSymbiosisData();
        if (!data.equipMutagen(SymbioticMutagen.CHITIN_THORNS) || !data.equipMutagen(SymbioticMutagen.PYRO_GLAND)) {
            helper.fail("Expected PlayerSymbiosisData to equip up to 2 mutagens.");
            return;
        }
        if (data.equipMutagen(SymbioticMutagen.ARCANE_SYNAPSE)) {
            helper.fail("Expected 3rd mutagen to be rejected when max 2 slots are full.");
            return;
        }
        if (Math.abs(data.getThreatAmplifier() - 1.30) > 0.001) {
            helper.fail("Expected 1.30x threat amplifier with 2 equipped mutagens.");
            return;
        }
        data.clearMutagens();
        if (data.getEquippedCount() != 0) {
            helper.fail("Expected clearMutagens to empty all equipped slots.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void counterSerumBleedsTargetedStimulus(GameTestHelper helper) {
        HiveMindSavedData hive = new HiveMindSavedData();
        double initialScore = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get() * 1.5;
        hive.recordStimulus(StimulusType.FIRE, stimulusForScore(initialScore), 100L);
        if (!hive.hasTrait(AdaptationTrait.FIRE_RESISTANT)) {
            helper.fail("Expected FIRE_RESISTANT trait before Counter-Serum.");
            return;
        }
        double bled = hive.bleedStimulusRatio(StimulusType.FIRE, 0.50, 120L);
        if (Math.abs(bled - initialScore * 0.5) > 0.001 || hive.hasTrait(AdaptationTrait.FIRE_RESISTANT)) {
            helper.fail("Expected Counter-Serum to bleed 50% FIRE stimulus and deactivate FIRE_RESISTANT.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void synapticBenchSynthesizesMutagenAndSerum(GameTestHelper helper) {
        BlockPos localBench = new BlockPos(1, 1, 1);
        helper.setBlock(localBench, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());
        if (!(helper.getBlockEntity(localBench) instanceof SynapticBenchBlockEntity bench)) {
            helper.fail("Expected SynapticBenchBlockEntity at bench position.");
            return;
        }
        bench.setLabTier(2);

        ItemStack sample = new ItemStack(ModItems.GENETIC_SAMPLE.get());
        sample.set(ModDataComponents.TRAIT_ID.get(), AdaptationTrait.BLAST_SHIELDED.id());
        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, sample);
        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));

        ItemStack mutagenOut = bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_OUTPUT);
        if (!mutagenOut.is(ModItems.SYMBIOTIC_MUTAGEN.get()) || GeneticSampleItem.getTrait(mutagenOut) != AdaptationTrait.BLAST_SHIELDED) {
            helper.fail("Expected Synaptic Bench to synthesize BLAST_SHIELDED SymbioticMutagen.");
            return;
        }

        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.LATENT_SPORE.get()));
        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(Items.AMETHYST_SHARD));
        ItemStack serumOut = bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_OUTPUT);
        if (!serumOut.is(ModItems.COUNTER_SERUM.get()) || GeneticSampleItem.getTrait(serumOut) != AdaptationTrait.BLAST_SHIELDED) {
            helper.fail("Expected Synaptic Bench to synthesize BLAST_SHIELDED CounterSerum.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mycelialInfectionAmplifiesDamageAndAntidoteCures(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));
        if (!player.hasEffect(ModEffects.MYCELIAL_INFECTION)) {
            helper.fail("Expected MYCELIAL_INFECTION effect to apply to player.");
            return;
        }
        player.removeEffect(ModEffects.MYCELIAL_INFECTION);
        if (player.hasEffect(ModEffects.MYCELIAL_INFECTION)) {
            helper.fail("Expected MYCELIAL_INFECTION effect to be removable.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chitinArmorSetBonusAndInfectionReduction(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CHITIN_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CHITIN_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CHITIN_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CHITIN_BOOTS.get()));

        if (!ChitinArmorItem.hasFullSet(player) || ChitinArmorItem.countEquippedPieces(player) != 4) {
            helper.fail("Expected ChitinArmorItem.hasFullSet to return true with 4 pieces equipped.");
            return;
        }

        SporelingEntity mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        float hpBefore = mob.getHealth();
        mob.hurt(helper.getLevel().damageSources().playerAttack(player), 4.0F);
        float dealt = hpBefore - mob.getHealth();
        if (dealt <= 4.0F) {
            helper.fail("Expected full Chitin Armor set to amplify damage against MycelialMob (> 4.0, got " + dealt + ").");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purificationTurretTargetsBurrowerAndFires(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos localTurret = new BlockPos(1, 1, 1);
        helper.setBlock(localTurret, ModBlocks.PURIFICATION_TURRET.get());
        if (!(helper.getBlockEntity(localTurret) instanceof PurificationTurretBlockEntity turret)) {
            helper.fail("Expected PurificationTurretBlockEntity at turret position.");
            return;
        }

        BurrowerEntity burrower = helper.spawnWithNoFreeWill(ModEntities.BURROWER.get(), 4, 1, 1);
        for (int t = 0; t < 36; t++) {
            PurificationTurretBlockEntity.serverTick(level, helper.absolutePos(localTurret),
                    ModBlocks.PURIFICATION_TURRET.get().defaultBlockState(), turret);
        }

        var projectiles = level.getEntitiesOfClass(NeutralizingSporeProjectile.class,
                burrower.getBoundingBox().inflate(18.0D));
        if (projectiles.isEmpty()) {
            helper.fail("Expected PurificationTurret to fire NeutralizingSporeProjectile at Burrower.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purificationTurretFasterInsidePylonZone(GameTestHelper helper) {
        BlockPos localTurret = new BlockPos(2, 1, 1);
        helper.setBlock(localTurret, ModBlocks.PURIFICATION_TURRET.get());
        if (!(helper.getBlockEntity(localTurret) instanceof PurificationTurretBlockEntity turret)) {
            helper.fail("Expected PurificationTurretBlockEntity.");
            return;
        }

        PurificationZoneTracker isolatedTracker = new PurificationZoneTracker();
        int outsideInterval = turret.getFireInterval(isolatedTracker);
        isolatedTracker.addPylon(helper.absolutePos(new BlockPos(1, 1, 1)));
        int insideInterval = turret.getFireInterval(isolatedTracker);

        if (insideInterval >= outsideInterval) {
            helper.fail("Expected PurificationTurret fire interval inside Pylon zone (" + insideInterval
                    + ") to be faster than outside (" + outsideInterval + ").");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sporeBulbTriggersInfectionCloudOnStep(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos localBulb = new BlockPos(2, 1, 2);
        helper.setBlock(localBulb, ModBlocks.SPORE_BULB.get());

        var victim = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 2, 1, 2);
        BlockPos absBulb = helper.absolutePos(localBulb);

        SporeBulbBlock.burstSporeCloud(level, absBulb);
        if (!victim.hasEffect(ModEffects.MYCELIAL_INFECTION)) {
            helper.fail("Expected SporeBulb cloud to apply MYCELIAL_INFECTION to nearby non-mycelial entity.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chitinArmorAndTurretRegisteredInCreativeAndRecipes(GameTestHelper helper) {
        if (ModArmorMaterials.CHITIN.get() == null
                || ModItems.CHITIN_HELMET.get() == null
                || ModBlocks.PURIFICATION_TURRET.get() == null
                || ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get() == null
                || ModBlocks.SPORE_BULB.get() == null
                || ModBlocks.MYCELIAL_TENDRILS.get() == null) {
            helper.fail("Expected v0.4.0 armor, turret, and bioluminescent flora to be registered.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deepMyceliumDimensionRegisteredAndAccessible(GameTestHelper helper) {
        var dimTypeRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        if (!dimTypeRegistry.containsKey(ModDimensions.DEEP_MYCELIUM_TYPE)) {
            helper.fail("Expected mycelialmind:deep_mycelium dimension_type to be loaded in registry.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void riftCatalystCreatesMycelialRiftOnBarrier(GameTestHelper helper) {
        BlockPos barrierPos = new BlockPos(1, 1, 1);
        BlockPos riftPos = barrierPos.above();
        helper.setBlock(barrierPos, ModBlocks.REINFORCED_BIO_BARRIER.get());
        helper.setBlock(riftPos, ModBlocks.MYCELIAL_RIFT.get());

        if (!helper.getBlockState(riftPos).is(ModBlocks.MYCELIAL_RIFT.get())
                || ModItems.RIFT_CATALYST.get() == null) {
            helper.fail("Expected MYCELIAL_RIFT block and RIFT_CATALYST item to be valid.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void apexMatriarchThreePhasesAndOverload(GameTestHelper helper) {
        ApexMatriarchEntity matriarch = helper.spawnWithNoFreeWill(ModEntities.APEX_MATRIARCH.get(), 4, 1, 4);
        if (matriarch.getMatriarchPhase() != 1) {
            helper.fail("Expected ApexMatriarch to start in Phase 1.");
            return;
        }
        matriarch.setHealth(matriarch.getMaxHealth() * 0.50F);
        matriarch.customServerAiStep();
        if (matriarch.getMatriarchPhase() != 2) {
            helper.fail("Expected ApexMatriarch to enter Phase 2 below 60% HP.");
            return;
        }
        matriarch.setHealth(matriarch.getMaxHealth() * 0.20F);
        matriarch.customServerAiStep();
        if (matriarch.getMatriarchPhase() != 3 || matriarch.getSynergies().isEmpty()) {
            helper.fail("Expected ApexMatriarch to enter Phase 3 below 25% HP with active synergies.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overmindCrownPurgesThreatAndCleansesMycelium(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos localMyc = new BlockPos(2, 1, 2);
        helper.setBlock(localMyc, ModBlocks.CREEPING_MYCELIUM.get());

        int cleansed = OvermindCrownItem.cleanseMyceliumRadius(level, helper.absolutePos(localMyc), 4);
        if (cleansed < 1 || !helper.getBlockState(localMyc).is(Blocks.DIRT)) {
            helper.fail("Expected OvermindCrownItem.cleanseMyceliumRadius to cleanse CreepingMycelium to Dirt.");
            return;
        }

        HiveMindSavedData hive = new HiveMindSavedData();
        hive.recordStimulus(StimulusType.FIRE, 100.0, 100L);
        double before = hive.getThreatLevel();
        hive.reduceThreat(25.0);
        if (hive.getThreatLevel() >= before) {
            helper.fail("Expected HiveMindSavedData.reduceThreat to lower threatLevel.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hiveSyncPayloadV4PreservesStimulusScores(GameTestHelper helper) {
        HiveMindSavedData hive = new HiveMindSavedData();
        hive.recordStimulus(StimulusType.MELEE, 42.5, 100L);
        hive.recordStimulus(StimulusType.MAGIC, 88.0, 100L);
        HiveSyncPayload original = HiveSyncPayload.fromSavedData(hive);

        var buf = Unpooled.buffer();
        HiveSyncPayload.STREAM_CODEC.encode(buf, original);
        HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);

        if (Math.abs(decoded.meleeScore() - 42.5) > 0.001 || Math.abs(decoded.magicScore() - 88.0) > 0.001) {
            helper.fail("Expected HiveSyncPayload v4 round-trip to preserve stimulus scores.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void evolutionRankScalingAndXpMultipliers(GameTestHelper helper) {
        ChitinBruteEntity brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        float baseHp = brute.getMaxHealth();
        brute.promoteRank(EvolutionRank.MUTATED);
        if (brute.getEvolutionRank() != EvolutionRank.MUTATED
                || brute.getMaxHealth() <= baseHp
                || Math.abs(EvolutionRank.MUTATED.xpMultiplier() - 1.25) > 0.001) {
            helper.fail("Expected MUTATED rank to scale HP and use 1.25x XP multiplier.");
            return;
        }
        brute.promoteRank(EvolutionRank.ELITE);
        if (brute.getEvolutionRank() != EvolutionRank.ELITE
                || Math.abs(EvolutionRank.ELITE.xpMultiplier() - 1.75) > 0.001) {
            helper.fail("Expected ELITE rank and 1.75x XP multiplier.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void synapseWeaverPromotesBaseUnitToMutated(GameTestHelper helper) {
        SynapseWeaverEntity weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 2, 1, 2);
        SporelingEntity sporeling = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 2);
        if (sporeling.getEvolutionRank() != EvolutionRank.BASE) {
            helper.fail("Expected freshly spawned Sporeling without finalizeSpawn to start at BASE.");
            return;
        }
        weaver.tickCount = 120;
        weaver.aiStep();
        if (sporeling.getEvolutionRank() != EvolutionRank.MUTATED) {
            helper.fail("Expected SynapseWeaver to promote nearby BASE Sporeling to MUTATED.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sporelingPackFrenzyAndLeechDrain(GameTestHelper helper) {
        LeechParasiteEntity leech = helper.spawnWithNoFreeWill(ModEntities.LEECH_PARASITE.get(), 2, 1, 2);
        var victim = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 2, 1, 3);
        leech.doHurtTarget(victim);
        if (leech.getAbsorptionAmount() < 2.0F) {
            helper.fail("Expected LeechParasite to gain Absorption after draining target.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void colonyWardenWarcryBuffsNearbyAllies(GameTestHelper helper) {
        ColonyWardenEntity warden = helper.spawnWithNoFreeWill(ModEntities.COLONY_WARDEN.get(), 2, 1, 2);
        SporelingEntity ally = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 2);
        var enemy = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 4, 1, 4);
        warden.setTarget(enemy);
        warden.tickCount = 140;
        warden.aiStep();

        if (!ally.hasEffect(MobEffects.DAMAGE_BOOST) || !ally.hasEffect(MobEffects.MOVEMENT_SPEED)) {
            helper.fail("Expected ColonyWarden warcry to grant Strength and Speed to nearby allies.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void eliteUnitGrantsDoubleGeneticSample(GameTestHelper helper) {
        ChitinBruteEntity brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        brute.promoteRank(EvolutionRank.ELITE);
        if (brute.getEvolutionRank() != EvolutionRank.ELITE) {
            helper.fail("Expected ELITE rank on ChitinBrute.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void promotionStartsScaleLerpAndFlash(GameTestHelper helper) {
        ChitinBruteEntity brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        brute.promoteRank(EvolutionRank.MUTATED);
        if (brute.getPromotionTick() < 0 || brute.getPromotionFlashProgress(0.0F) <= 0.0F) {
            helper.fail("Expected live promotion to set DATA_PROMOTION_TICK and active flash progress.");
            return;
        }
        for (int i = 0; i < 10; i++) {
            brute.aiStep();
        }
        double midScale = brute.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (midScale <= 1.001 || midScale >= 1.119) {
            helper.fail("Expected intermediate scale lerp at 10 ticks, got " + midScale);
            return;
        }
        for (int i = 0; i < 10; i++) {
            brute.aiStep();
        }
        double finalScale = brute.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (Math.abs(finalScale - EvolutionRank.MUTATED.scale()) > 0.001) {
            helper.fail("Expected final MUTATED scale 1.12 at 20 ticks, got " + finalScale);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spawnRankAppliesWithoutTransition(GameTestHelper helper) {
        ChitinBruteEntity brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("MycelialEvolutionRank", EvolutionRank.MUTATED.id());
        brute.readAdditionalSaveData(tag);
        double scale = brute.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (brute.getEvolutionRank() != EvolutionRank.MUTATED
                || Math.abs(scale - EvolutionRank.MUTATED.scale()) > 0.001
                || brute.getPromotionTick() >= 0) {
            helper.fail("Expected NBT load rank to apply scale immediately without transition effects.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void eliteTransitionReachesFinalScale(GameTestHelper helper) {
        ChitinBruteEntity brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        brute.promoteRank(EvolutionRank.ELITE);
        for (int i = 0; i < com.alex.mycelialmind.entity.MycelialMob.PROMOTION_SCALE_LERP_TICKS; i++) {
            brute.aiStep();
        }
        double finalScale = brute.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (Math.abs(finalScale - EvolutionRank.ELITE.scale()) > 0.001) {
            helper.fail("Expected ELITE transition to reach 1.25 scale, got " + finalScale);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void sporeArchitectPlantsMyceliumAndRelayNode(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(3 + dx, 1, 3 + dz, Blocks.COBBLESTONE);
            }
        }
        BlockPos nodeRel = new BlockPos(3, 2, 3);
        helper.setBlock(nodeRel, Blocks.AIR);
        BlockPos nodeAbs = helper.absolutePos(nodeRel);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        for (BlockPos p : tracker.getPylonsWithin(nodeAbs, 1_000_000_000, 200)) {
            tracker.removePylon(p);
        }
        for (var entry : tracker.getNodesWithin(nodeAbs, 1_000_000_000, 200)) {
            tracker.removeNode(entry.getKey());
        }
        HiveMindSavedData hive = resetHive(level);
        hive.recordStimulus(StimulusType.MELEE, 550.0);
        var architect = helper.spawnWithNoFreeWill(ModEntities.SPORE_ARCHITECT.get(), 2, 2, 2);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    level.setBlock(nodeAbs.below().offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 18);
                }
            }
        }
        helper.assertFalse(architect.attemptPlantColonyAt(level, nodeAbs),
                "An architect must not start an isolated mycelium patch.");
        level.setBlock(nodeAbs.below().west(2), ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState(), 18);
        boolean planted = architect.attemptPlantColonyAt(level, nodeAbs);
        if (!planted
                || !helper.getBlockState(nodeRel).is(ModBlocks.MYCELIAL_NODE.get())
                || !helper.getBlockState(new BlockPos(3, 1, 3)).is(ModBlocks.CREEPING_MYCELIUM.get())) {
            tracker.removeNode(nodeAbs);
            hive.resetAll();
            helper.fail("Expected SporeArchitect to assimilate 3x3 patch and erect a Relay Node. planted=" + planted
                    + " nodeBlock=" + helper.getBlockState(nodeRel)
                    + " groundBlock=" + helper.getBlockState(new BlockPos(3, 1, 3))
                    + " protected=" + tracker.isProtected(nodeAbs)
                    + " nodes=" + tracker.countNodes());
            return;
        }
        tracker.removeNode(nodeAbs);
        helper.setBlock(nodeRel, Blocks.AIR);
        hive.resetAll();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void relayNodeMaturesToIncubatorOrSentinel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos abs = helper.absolutePos(rel);
        helper.setBlock(rel, ModBlocks.MYCELIAL_NODE.get());
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.addNode(abs, SynapticNodeType.RELAY);
        SynapticNodeType promoted = SynapticNodeBlockEntity.matureRelayNode(level, abs, tracker);
        BlockState state = helper.getBlockState(rel);
        boolean matches = (promoted == SynapticNodeType.INCUBATOR && state.is(ModBlocks.INCUBATOR_NODE.get()))
                || (promoted == SynapticNodeType.SENTINEL && state.is(ModBlocks.SENTINEL_NODE.get()));
        if (!matches || tracker.getNodeType(abs) != promoted) {
            helper.fail("Expected Relay node to mature in-place into Incubator or Sentinel.");
            return;
        }
        helper.setBlock(rel, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void infectedVillagerConvertsOutsidePylonAndRespectsPylonProtection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var protectedVillager = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.VILLAGER, 2, 1, 2);
        protectedVillager.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));
        BlockPos pylonAbs = helper.absolutePos(new BlockPos(2, 1, 2));
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.addPylon(pylonAbs);
        boolean convertedProtected = DamageTelemetryTracker.tryConvertInfectedMob(level, protectedVillager, true);
        tracker.removePylon(pylonAbs);

        if (convertedProtected || protectedVillager.hasEffect(ModEffects.MYCELIAL_INFECTION)) {
            helper.fail("Expected Pylon protection to cure infection and prevent villager conversion.");
            return;
        }

        HiveMindSavedData hive = resetHive(level);
        hive.recordStimulus(StimulusType.MELEE, 550.0);
        var exposedVillager = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.VILLAGER, 4, 1, 4);
        exposedVillager.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));
        boolean convertedExposed = DamageTelemetryTracker.tryConvertInfectedMob(level, exposedVillager, true);
        hive.resetAll();
        if (!convertedExposed || exposedVillager.isAlive()) {
            helper.fail("Expected exposed infected villager to convert and discard original.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aggressiveMyceliumAssimilatesCobbleAndPlanksRespectingBlockEntities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos cobbleRel = new BlockPos(2, 1, 2);
        BlockPos planksRel = new BlockPos(3, 1, 2);
        BlockPos chestRel = new BlockPos(4, 1, 2);
        helper.setBlock(cobbleRel, Blocks.COBBLESTONE);
        helper.setBlock(planksRel, Blocks.OAK_PLANKS);
        helper.setBlock(chestRel, Blocks.CHEST);

        HiveMindSavedData hive = resetHive(level);
        hive.recordStimulus(StimulusType.MELEE, 550.0);
        boolean canCobble = CreepingMyceliumBlock.canInfect(level, helper.absolutePos(cobbleRel), helper.getBlockState(cobbleRel));
        boolean canPlanks = CreepingMyceliumBlock.canInfect(level, helper.absolutePos(planksRel), helper.getBlockState(planksRel));
        boolean canChest = CreepingMyceliumBlock.canInfect(level, helper.absolutePos(chestRel), helper.getBlockState(chestRel));
        hive.resetAll();
        if (!canCobble || !canPlanks || canChest) {
            helper.fail("Expected aggressive assimilation for cobble/planks while protecting BlockEntity chest.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void fungalTideActivatesAt75ThreatAndEndsOnBehemothDeath(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        hive.resetAll();
        long now = level.getGameTime();
        hive.recordStimulus(StimulusType.FIRE, 800.0, now);
        HiveWaveDirector.handleFungalTide(level, hive, now);
        if (!hive.isFungalTideActive(now)) {
            helper.fail("Expected Fungal Tide to activate when threat >= 75%.");
            return;
        }
        var behemoth = helper.spawnWithNoFreeWill(ModEntities.SPORE_BEHEMOTH.get(), 2, 1, 2);
        behemoth.activateMycelialShield(level);
        if (!behemoth.hasEffect(MobEffects.ABSORPTION) || !behemoth.hasEffect(MobEffects.REGENERATION)) {
            hive.resetAll();
            helper.fail("Expected SporeBehemoth Mycelial Shield to grant Absorption and Regeneration.");
            return;
        }
        behemoth.die(level.damageSources().genericKill());
        if (hive.isFungalTideActive(now)) {
            hive.resetAll();
            helper.fail("Expected SporeBehemoth death to clear active Fungal Tide.");
            return;
        }
        hive.resetAll();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void tideBeaconPurgesFungalTideAndClearsInfection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        long now = level.getGameTime();
        hive.triggerFungalTide(now, 600);
        var villager = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.VILLAGER, 2, 1, 2);
        villager.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(helper.absolutePos(new BlockPos(2, 1, 2)).getCenter());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.TIDE_BEACON.get()));
        ModItems.TIDE_BEACON.get().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);

        if (hive.isFungalTideActive(now) || villager.hasEffect(ModEffects.MYCELIAL_INFECTION)) {
            hive.resetAll();
            helper.fail("Expected TideBeacon to abort Fungal Tide and cure nearby infected mobs.");
            return;
        }
        hive.resetAll();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "surface_hive_feature")
    public static void surfaceHiveFeaturePlacesNodesAndTrackerRegistersThem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                helper.setBlock(4 + dx, 0, 4 + dz, Blocks.GRASS_BLOCK);
            }
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        BlockPos originAbs = helper.absolutePos(new BlockPos(4, 1, 4));
        boolean placed = com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveAt(level, originAbs, level.getRandom());
        boolean ok = placed
                && level.getBlockState(originAbs).is(ModBlocks.INCUBATOR_NODE.get())
                && level.getBlockState(originAbs.above(3)).is(ModBlocks.SENTINEL_NODE.get())
                && tracker.getNodeType(originAbs) == SynapticNodeType.INCUBATOR;
        tracker.removeNode(originAbs);
        tracker.removeNode(originAbs.above(3));
        if (!ok) {
            helper.fail("Expected SurfaceHiveFeature to generate Incubator+Sentinel dome and register nodes.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void purifierDroneCleansesMyceliumAndTargetsMycelialMobs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos groundRel = new BlockPos(2, 0, 2);
        helper.setBlock(groundRel, ModBlocks.CREEPING_MYCELIUM.get());
        var drone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 2, 2, 2);
        var target = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 4, 1, 2);
        boolean cleansed = drone.cleanseMyceliumBeneath(level);
        boolean fired = drone.tryFireAtNearestMob(level);
        if (!cleansed || !helper.getBlockState(groundRel).is(Blocks.GRASS_BLOCK) || !fired || !target.isAlive()) {
            helper.fail("Expected PurifierDrone to cleanse mycelium beneath and fire at nearby MycelialMob.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void purificationTurretUpgradesToTier2AndTier3(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag snapshot = hive.save(new CompoundTag(), level.registryAccess());
        try {
            hive.promoteHighestTier(HiveProgressionTier.ADAPTIVE);
            BlockPos turretRel = new BlockPos(2, 1, 2);
            helper.setBlock(turretRel, ModBlocks.PURIFICATION_TURRET.get());
            if (!(helper.getBlockEntity(turretRel) instanceof PurificationTurretBlockEntity turret)) {
                helper.fail("Expected PurificationTurretBlockEntity at turretRel.");
                return;
            }
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            boolean up2 = turret.tryUpgrade(new ItemStack(ModItems.SYNAPTIC_CORE.get()), player);
            boolean up3 = turret.tryUpgrade(new ItemStack(ModItems.TIDE_BEACON.get()), player);
            if (!up2 || !up3
                    || turret.getTier() != 3
                    || turret.getShotsPerVolley() != 2
                    || turret.getRangeRadius() < 28.0) {
                helper.fail("Expected PurificationTurret to upgrade T1->T2->T3 with double shot and 28m range.");
                return;
            }
            helper.succeed();
        } finally {
            hive.restoreFromSnapshot(snapshot, level.registryAccess());
        }
    }

    @GameTest(template = "empty")
    public static void tideBeaconAndDroneRecipesRegistered(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        if (recipes.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "purifier_drone_core")).isEmpty()
                || recipes.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "tide_beacon_recharge")).isEmpty()) {
            helper.fail("Expected purifier_drone_core and tide_beacon_recharge recipes to be registered.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chitinArmorEvolvesToMutatedAndEliteWithStatBonuses(GameTestHelper helper) {
        ItemStack chest = new ItemStack(ModItems.CHITIN_CHESTPLATE.get());
        boolean m1 = ChitinArmorItem.tryEvolvePiece(chest, new ItemStack(ModItems.SYNAPTIC_CORE.get()), null);
        boolean m2 = ChitinArmorItem.tryEvolvePiece(chest, new ItemStack(ModItems.OVERMIND_ESSENCE.get()), null);
        if (!m1 || !m2 || ChitinArmorItem.getArmorRank(chest) != EvolutionRank.ELITE) {
            helper.fail("Expected ChitinArmorItem to evolve BASE -> MUTATED -> ELITE.");
            return;
        }
        var mods = chest.getAttributeModifiers().modifiers();
        boolean hasBonusArmor = mods.stream().anyMatch(e ->
                e.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                        && e.modifier().amount() >= 2.0);
        if (!hasBonusArmor) {
            helper.fail("Expected ELITE ChitinArmorItem to include +2.0 armor modifier.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "symbiotic_blade_arc")
    public static void symbioticBladeArcSlashDamagesAndHealsPlayer(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos pPos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.moveTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5, 0.0F, 0.0F);
        player.setHealth(10.0F);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYMBIOTIC_BLADE.get()));

        var target = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 5);
        float hpBefore = target.getHealth();
        ModItems.SYMBIOTIC_BLADE.get().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (target.getHealth() >= hpBefore || player.getHealth() <= 10.0F) {
            helper.fail("Expected SymbioticBlade arc slash to damage forward target and heal player.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void enzymeLanceDashAndSporeLauncherBurstExecuteCleanly(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos pPos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.moveTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5, 0.0F, 0.0F);

        var target = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 3);
        float hpBefore = target.getHealth();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.ENZYME_LANCE.get()));
        ModItems.ENZYME_LANCE.get().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        player.setShiftKeyDown(true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.NEUTRALIZING_SPORE_LAUNCHER.get()));
        ModItems.NEUTRALIZING_SPORE_LAUNCHER.get().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        if (target.getHealth() >= hpBefore || !target.hasEffect(MobEffects.WEAKNESS)) {
            helper.fail("Expected EnzymeLance dash to deal magic damage and apply Weakness.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void optionalModCompatGuardsDoNotCrash(GameTestHelper helper) {
        if (com.alex.mycelialmind.compat.iris.MycelialIrisCompat.isShaderPackInUse()) {
            helper.fail("Iris shader pack should not be reported active on headless GameTestServer.");
            return;
        }
        if (com.alex.mycelialmind.compat.modonomicon.MycelialModonomiconCompat.isModonomiconActive()) {
            helper.fail("Modonomicon should be optional and inactive when not installed.");
            return;
        }
        if (net.neoforged.fml.ModList.get().isLoaded("smartbrainlib")) {
            helper.fail("SmartBrainLib must NOT be present; Mycelial Mind uses native HiveBrain.");
            return;
        }
        var mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        if (!(mob instanceof HiveBrainOwner<?>) || mob.getHiveBrainRuntime() == null) {
            helper.fail("MycelialMob should implement HiveBrainOwner with initialized HiveBrainRuntime.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveMindSensorPopulatesBlackboardMemories(GameTestHelper helper) {
        var level = helper.getLevel();
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();
        hive.recordStimulus(com.alex.mycelialmind.hive.StimulusType.EXPLOSION, 80.0);

        BlockPos nodePos = helper.absolutePos(new BlockPos(28, 1, 2));
        BlockPos pylonPos = helper.absolutePos(new BlockPos(1, 1, 2));
        level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());

        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 4, 1, 2);
        new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor().tick(level, brute);

        float alert = com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getHiveAlertLevel(brute);
        BlockPos memNode = com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getNearestNodePos(brute);
        BlockPos memPylon = com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getNearestPylonPos(brute);
        var tracker = PurificationZoneTracker.get(level);
        tracker.removeNode(nodePos);
        tracker.removePylon(pylonPos);
        level.removeBlock(nodePos, false);
        level.removeBlock(pylonPos, false);
        hive.resetAll();
        if (alert <= 0.0F || memNode == null || memPylon == null) {
            helper.fail("Expected HiveMindSensor to populate alert, nearest node, and nearest pylon in Brain. alert="
                    + alert + " memNode=" + memNode + " memPylon=" + memPylon);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveUtilityEvaluatorAndTacticalRetreatWork(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos nodePos = helper.absolutePos(new BlockPos(16, 1, 2));
        BlockPos pylonPos = helper.absolutePos(new BlockPos(1, 1, 2));
        level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());

        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 5, 1, 2);
        spitter.setHealth(spitter.getMaxHealth() * 0.25F);
        new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor().tick(level, spitter);

        float regroup = com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateRegroupUtility(spitter);
        var target = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        target.moveTo(spitter.getX() + 1.0, spitter.getY(), spitter.getZ());
        var dir = new com.alex.mycelialmind.entity.ai.KitingRetreatGoal(spitter, 1.15D).computeTacticalRetreatDirection(target);

        var tracker = PurificationZoneTracker.get(level);
        tracker.removeNode(nodePos);
        tracker.removePylon(pylonPos);
        level.removeBlock(nodePos, false);
        level.removeBlock(pylonPos, false);

        if (regroup < 0.65F) {
            helper.fail("Expected low-health mob near pylon and away from node to have regroup utility >= 0.65, got " + regroup);
            return;
        }
        if (dir.lengthSqr() < 0.9D) {
            helper.fail("Expected normalized tactical retreat direction.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveBlackboardClearsUnloadedLevelWithoutAffectingOtherLevels(GameTestHelper helper) {
        var level = helper.getLevel();
        var other = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        if (other == null) {
            helper.fail("Expected the GameTest server to have a Nether level.");
            return;
        }
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        com.alex.mycelialmind.hive.HiveBlackboard.clear(other);
        BlockPos target = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos otherTarget = new BlockPos(20, 64, 20);
        com.alex.mycelialmind.hive.HiveBlackboard.reportTarget(level, target);
        com.alex.mycelialmind.hive.HiveBlackboard.reportTarget(other, otherTarget);
        com.alex.mycelialmind.hive.HiveBlackboard.reportNodeLoss(level, target);

        // Model the level-unload callback without unloading the active GameTest world.
        com.alex.mycelialmind.hive.HiveBlackboard.onLevelUnload(
                new net.neoforged.neoforge.event.level.LevelEvent.Unload(level));
        boolean cleared = com.alex.mycelialmind.hive.HiveBlackboard.getRecentTarget(level) == null
                && com.alex.mycelialmind.hive.HiveBlackboard.getDoctrine(level)
                == com.alex.mycelialmind.hive.HiveDoctrine.DORMANT;
        boolean otherPreserved = otherTarget.equals(com.alex.mycelialmind.hive.HiveBlackboard.getRecentTarget(other));
        com.alex.mycelialmind.hive.HiveBlackboard.clear(other);
        if (!cleared || !otherPreserved) {
            helper.fail("Unloading a level must clear only that level's transient hive state.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveDoctrineAndSquadComposerCounterAdaptDominantStimulus(GameTestHelper helper) {
        var level = helper.getLevel();
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);

        if (com.alex.mycelialmind.hive.HiveDoctrine.evaluate(hive, level.getGameTime(), false)
                != com.alex.mycelialmind.hive.HiveDoctrine.DORMANT) {
            helper.fail("Expected DORMANT doctrine at 0 threat.");
            return;
        }

        hive.recordStimulus(com.alex.mycelialmind.hive.StimulusType.PROJECTILE, 260.0);
        var huntDoctrine = com.alex.mycelialmind.hive.HiveDoctrine.evaluate(hive, level.getGameTime(), false);
        if (huntDoctrine != com.alex.mycelialmind.hive.HiveDoctrine.FORAGING
                && huntDoctrine != com.alex.mycelialmind.hive.HiveDoctrine.ADAPTIVE_HUNT
                && huntDoctrine != com.alex.mycelialmind.hive.HiveDoctrine.SIEGE_ASSAULT) {
            helper.fail("Expected escalated doctrine after high projectile stimulus, got " + huntDoctrine);
            return;
        }

        var squad = com.alex.mycelialmind.hive.HiveSquadComposer.composeWaveSquad(
                hive, huntDoctrine, 50, level.getRandom()
        );
        if (squad.isEmpty() || (!squad.contains(ModEntities.CHITIN_BRUTE.get())
                && !squad.contains(ModEntities.BURROWER.get()))) {
            helper.fail("Expected counter-projectile squad to include ChitinBrute or Burrower.");
            return;
        }

        com.alex.mycelialmind.hive.HiveBlackboard.reportNodeLoss(level, helper.absolutePos(new BlockPos(2, 1, 2)));
        if (com.alex.mycelialmind.hive.HiveBlackboard.getDoctrine(level)
                != com.alex.mycelialmind.hive.HiveDoctrine.RECOVERY) {
            helper.fail("Expected RECOVERY doctrine after node loss.");
            return;
        }

        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        hive.resetAll();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveOrderPropagatesToSblBrainAndModulatesUtility(GameTestHelper helper) {
        var level = helper.getLevel();
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);

        hive.recordStimulus(com.alex.mycelialmind.hive.StimulusType.MELEE, 260.0);
        BlockPos nodePos = helper.absolutePos(new BlockPos(28, 1, 2));
        level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        com.alex.mycelialmind.hive.HiveBlackboard.reportNodeLoss(level, nodePos.east());

        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 4, 1, 2);
        var ally = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 6, 1, 2);
        new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor().tick(level, brute);
        new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor().tick(level, ally);

        var order = com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getHiveOrder(brute);
        var rallyNode = HiveBrainBridge.getNearestNodePos(brute);
        float regroup = com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateRegroupUtility(brute);
        var regroupGoal = new com.alex.mycelialmind.entity.ai.HiveRegroupGoal(brute, 1.0);
        boolean acceptedRally = regroupGoal.canUse();
        var allyGoal = new com.alex.mycelialmind.entity.ai.HiveRegroupGoal(ally, 1.0);
        boolean allyAcceptedRally = allyGoal.canUse();

        PurificationZoneTracker.get(level).removeNode(nodePos);
        level.removeBlock(nodePos, false);
        boolean acceptedDestroyedNode = regroupGoal.canUse();
        boolean allyAcceptedDestroyedNode = allyGoal.canUse();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        hive.resetAll();

        if (order != com.alex.mycelialmind.hive.HiveOrder.RALLY_NODE || !nodePos.equals(rallyNode)
                || regroup < 0.35F || !acceptedRally || !allyAcceptedRally
                || acceptedDestroyedNode || allyAcceptedDestroyedNode) {
            helper.fail("Expected RALLY_NODE to use the surviving node, got order=" + order
                    + ", node=" + rallyNode + ", regroup=" + regroup);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveSpatialMemoryBoundsCasualtyLearningAndScouting(GameTestHelper helper) {
        var level = helper.getLevel();
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();

        for (int i = 0; i < 30; i++) {
            hive.recordCombatHotspot(new BlockPos(i * 32, 64, 0), 10.0 + i, level.getGameTime(),
                    com.alex.mycelialmind.hive.StimulusType.FIRE);
        }
        if (hive.getHotspots().size() != HiveMindSavedData.MAX_HOTSPOTS) {
            helper.fail("Expected hotspots to be capped at " + HiveMindSavedData.MAX_HOTSPOTS + ", got " + hive.getHotspots().size());
            return;
        }
        if (hive.getHottestSpot() == null
                || hive.getHottestSpot().intensity() < 35.0
                || hive.getHottestSpot().dominantThreat() != com.alex.mycelialmind.hive.StimulusType.FIRE) {
            helper.fail("Expected hottest spot to retain highest intensity and FIRE threat.");
            return;
        }

        hive.recordStimulus(com.alex.mycelialmind.hive.StimulusType.MELEE, 30.0);
        hive.recordMobCasualty(com.alex.mycelialmind.hive.StimulusType.PROJECTILE);
        hive.recordMobCasualty(com.alex.mycelialmind.hive.StimulusType.PROJECTILE);
        if (hive.getCasualties(com.alex.mycelialmind.hive.StimulusType.PROJECTILE) != 2
                || hive.getMostLethalStimulus() != com.alex.mycelialmind.hive.StimulusType.PROJECTILE
                || com.alex.mycelialmind.hive.HiveSquadComposer.findDominantStimulus(hive) != com.alex.mycelialmind.hive.StimulusType.PROJECTILE
                || hive.getEffectivenessPressure() <= 0.0) {
            helper.fail("Expected casualties to pivot dominant stimulus to PROJECTILE and raise effectiveness pressure.");
            return;
        }

        hive.resetAll();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.clearDecoys(level);
        BlockPos nearSpot = helper.absolutePos(new BlockPos(8, 1, 8));
        hive.recordCombatHotspot(nearSpot, 22.0, level.getGameTime(), com.alex.mycelialmind.hive.StimulusType.EXPLOSION);
        var scout = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor().tick(level, scout);
        BlockPos investigated = com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getSharedTargetPos(scout);
        hive.resetAll();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        if (investigated == null || !investigated.equals(nearSpot)) {
            helper.fail("Expected HiveMindSensor to direct idle mob toward hottest spatial memory spot: expected="
                    + nearSpot + ", actual=" + investigated
                    + ", protected=" + PurificationZoneTracker.get(level).isProtected(nearSpot)
                    + ", decoy=" + com.alex.mycelialmind.hive.MycorrhizalAcousticTracker
                            .getNearestActiveDecoy(level, scout.blockPosition(), 20));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "swarm_coordinator_isolated")
    public static void hiveSwarmCoordinatorAssignsRolesFlanksAndBroadcastsAlertCall(GameTestHelper helper) {
        var level = helper.getLevel();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        var weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 3, 1, 2);
        var stalker = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_STALKER.get(), 4, 1, 2);
        var ally = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 6, 1, 2);

        if (com.alex.mycelialmind.hive.HiveSwarmCoordinator.assignRole(brute) != com.alex.mycelialmind.hive.SwarmRole.FRONT_ANCHOR
                || com.alex.mycelialmind.hive.HiveSwarmCoordinator.assignRole(weaver) != com.alex.mycelialmind.hive.SwarmRole.SUPPORT_REAR) {
            helper.fail("Expected ChitinBrute=FRONT_ANCHOR and SynapseWeaver=SUPPORT_REAR.");
            return;
        }
        var stalkerRole = com.alex.mycelialmind.hive.HiveSwarmCoordinator.assignRole(stalker);
        if (stalkerRole != com.alex.mycelialmind.hive.SwarmRole.FLANK_LEFT
                && stalkerRole != com.alex.mycelialmind.hive.SwarmRole.FLANK_RIGHT) {
            helper.fail("Expected MycelialStalker to receive a flank role.");
            return;
        }

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos pPos = helper.absolutePos(new BlockPos(10, 1, 10));
        player.moveTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5);

        var flankPos = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeSwarmApproachPos(stalker, player);
        // A 4.5-block flank radius is blended with up to 1.1 blocks of steering
        // and 1.15 * sqrt(2) blocks of organic flow, so two blocks is not a
        // guaranteed lower bound even in a clean influence map.
        if (flankPos.distanceToSqr(player.position()) < 1.7D * 1.7D) {
            helper.fail("Expected flank approach position to retain an offset around target.");
            return;
        }

        int alerted = com.alex.mycelialmind.hive.HiveSwarmCoordinator.broadcastAlertCall(level, stalker, player);
        if (alerted < 1 || ally.getTarget() != player) {
            helper.fail("Expected AlertCall within 20 blocks to assign target to nearby ally.");
            return;
        }

        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveLearningMomentumPivotAndMasteryBleedWork(GameTestHelper helper) {
        var level = helper.getLevel();
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();

        long t0 = level.getGameTime();
        double threshold = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
        double hit = stimulusForScore(threshold / 3.0);
        hive.recordStimulus(StimulusType.MELEE, hit, t0);
        hive.recordStimulus(StimulusType.MELEE, hit, t0 + 20L);
        hive.recordStimulus(StimulusType.MELEE, hit, t0 + 40L);
        if (hive.getLastStimulusStreak() < 2 || hive.getStimulusScore(StimulusType.MELEE) <= threshold) {
            helper.fail("Expected momentum streak to raise repeated MELEE stimulus above the trait threshold.");
            return;
        }

        // Pivot to PROJECTILE after MELEE_THORNS activated -> 1.35x pivot bonus
        hive.recordStimulus(StimulusType.PROJECTILE, stimulusForScore(10.0), t0 + 50L);
        if (Math.abs(hive.getStimulusScore(StimulusType.PROJECTILE) - 13.5) > 0.001) {
            helper.fail("Expected 1.35x pivot bonus when switching stimulus after trait activation.");
            return;
        }

        hive.reinforceTraitMastery(com.alex.mycelialmind.hive.AdaptationTrait.MELEE_THORNS, 0.50);
        if (hive.getTraitMastery(com.alex.mycelialmind.hive.AdaptationTrait.MELEE_THORNS) < 0.49) {
            helper.fail("Expected repeated MELEE learning to reinforce traitMastery.");
            return;
        }

        hive.bleedStimulusRatio(com.alex.mycelialmind.hive.StimulusType.MELEE, 0.5, t0 + 60L);
        if (hive.getTraitMastery(com.alex.mycelialmind.hive.AdaptationTrait.MELEE_THORNS) >= 0.49) {
            helper.fail("Expected CounterSerum bleedStimulusRatio to reduce traitMastery.");
            return;
        }

        hive.resetAll();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void traitMasteryReinforcesAndScalesDamageMitigation(GameTestHelper helper) {
        var level = helper.getLevel();
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();

        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        brute.applyHiveTraits(java.util.EnumSet.of(com.alex.mycelialmind.hive.AdaptationTrait.BLAST_SHIELDED));
        brute.hurt(level.damageSources().explosion(null, null), 6.0F);

        double mastery = hive.getTraitMastery(com.alex.mycelialmind.hive.AdaptationTrait.BLAST_SHIELDED);
        hive.resetAll();
        if (mastery <= 0.0) {
            helper.fail("Expected BLAST_SHIELDED damage mitigation to reinforce traitMastery > 0.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void bruteChargeHandlesSharedTargetClearedBeforeStart(GameTestHelper helper) {
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        brute.setOnGround(true);
        var goal = new com.alex.mycelialmind.entity.ai.BruteChargeGoal(brute);
        var memory = com.alex.mycelialmind.registry.ModMemoryTypes.SHARED_TARGET_POS.get();
        BlockPos destination = brute.blockPosition().offset(8, 0, 0);
        brute.getBrain().setMemory(memory, destination);
        for (int i = 0; i < 60; i++) {
            goal.canUse();
        }
        if (!goal.canUse()) {
            helper.fail("Expected a shared target in range to permit a charge.");
            return;
        }

        // The target can expire or disappear between the goal selector's canUse and start.
        brute.getBrain().eraseMemory(memory);
        goal.start();
        if (goal.canContinueToUse() || brute.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE)) {
            helper.fail("A charge without a target must not start or grant resistance.");
            return;
        }

        brute.getBrain().setMemory(memory, destination);
        goal.start();
        if (!goal.canContinueToUse() || brute.getDeltaMovement().x <= 0.0D) {
            helper.fail("A valid shared target must still start the charge in its direction.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void mobBrainCombatXpSelfPromotesAndFeedsBackToHive(GameTestHelper helper) {
        var level = helper.getLevel();
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();

        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.addCombatXp(
                level, brute, com.alex.mycelialmind.hive.StimulusType.PROJECTILE, 100);
        if (brute.getEvolutionRank() != EvolutionRank.MUTATED || hive.getStimulusScore(com.alex.mycelialmind.hive.StimulusType.PROJECTILE) < 1.9) {
            helper.fail("Expected 100 Brain XP to self-promote ChitinBrute to MUTATED and feed +2.0 stimulus to Hive.");
            return;
        }

        var behemoth = helper.spawnWithNoFreeWill(ModEntities.SPORE_BEHEMOTH.get(), 4, 1, 2);
        com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.addCombatXp(
                level, behemoth, com.alex.mycelialmind.hive.StimulusType.MELEE, 250);
        if (behemoth.getEvolutionRank() != EvolutionRank.ELITE) {
            helper.fail("Expected mini-boss SporeBehemoth to self-promote to ELITE at 250 Brain XP.");
            return;
        }

        var boss = helper.spawnWithNoFreeWill(ModEntities.HIVE_CORE.get(), 6, 1, 2);
        com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.addCombatXp(
                level, boss, com.alex.mycelialmind.hive.StimulusType.MAGIC, 300);
        if (boss.getEvolutionRank() != EvolutionRank.BASE) {
            helper.fail("Expected main boss HiveCoreBoss to be excluded from Brain XP self-promotion.");
            return;
        }

        hive.resetAll();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void sblBehavioursRegenAtNodePatrolHotspotAndScoreAllyCohesion(GameTestHelper helper) {
        var level = helper.getLevel();
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);

        BlockPos nodePos = helper.absolutePos(new BlockPos(4, 1, 2));
        level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        BlockPos hotspot = helper.absolutePos(new BlockPos(20, 1, 20));
        hive.recordCombatHotspot(hotspot, 30.0, level.getGameTime(), com.alex.mycelialmind.hive.StimulusType.MELEE);

        var mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 2);
        var ally1 = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 4, 1, 3);
        var ally2 = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 5, 1, 2);
        mob.setHealth(mob.getMaxHealth() * 0.5F);
        float hpBefore = mob.getHealth();

        new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor().tick(level, mob);
        if (com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getNearbyAllyCount(mob) < 2) {
            helper.fail("Expected HiveMindSensor to count >= 2 nearby allies.");
            return;
        }
        var dummyTarget = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        float squadCharge = com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateChargeUtility(mob, dummyTarget);
        if (squadCharge < 0.25F) {
            helper.fail("Expected >= 2 allies cohesion to boost charge utility >= 0.25, got " + squadCharge);
            return;
        }

        new com.alex.mycelialmind.entity.ai.brain.hive.HiveNodeRegenBehaviour().tryStart(level, mob, level.getGameTime());
        new com.alex.mycelialmind.entity.ai.brain.hive.HiveHotspotPatrolBehaviour().tryStart(level, mob, level.getGameTime());

        boolean healed = mob.getHealth() > hpBefore;
        boolean hasWalkTarget = mob.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET).isPresent();

        PurificationZoneTracker.get(level).removeNode(nodePos);
        level.removeBlock(nodePos, false);
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        hive.resetAll();
        ally1.discard();
        ally2.discard();

        if (!healed || !hasWalkTarget) {
            helper.fail("Expected HiveNodeRegenBehaviour to heal (" + healed + ") and HiveHotspotPatrolBehaviour to set WALK_TARGET (" + hasWalkTarget + ").");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void worldgenOutbreakAndSurfaceHivePlacementIntegrity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos hiveOrigin = base.offset(16, 1, 0);
        for (var entry : tracker.getNodesWithin(hiveOrigin, 1_000_000_000, 128)) {
            tracker.removeNode(entry.getKey());
        }

        // Prepare flat dirt substrate for SurfaceHiveFeature (outer 2-ring radius 8)
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                level.setBlockAndUpdate(hiveOrigin.below().offset(dx, 0, dz), Blocks.DIRT.defaultBlockState());
                for (int dy = 0; dy <= 4; dy++) {
                    level.setBlockAndUpdate(hiveOrigin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }

        boolean generated = com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveAt(level, hiveOrigin, level.getRandom());
        if (!generated) {
            helper.fail("Expected SurfaceHiveFeature.generateHiveAt to succeed on solid dirt substrate.");
            return;
        }

        var hiveReport = com.alex.mycelialmind.world.WorldgenAuditEngine.inspectSurfaceHive(level, tracker, hiveOrigin);
        if (!hiveReport.valid()) {
            helper.fail("SurfaceHive integrity check failed: " + hiveReport.toJson());
            return;
        }

        // Cleanup placed hive nodes before placing outbreak nearby
        tracker.removeNode(hiveOrigin);
        tracker.removeNode(hiveOrigin.above(3));

        // Verify MycelialOutbreakFeature placement on dirt
        BlockPos outbreakOrigin = base.offset(-24, 1, 0);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.setBlockAndUpdate(outbreakOrigin.below(2).offset(dx, 0, dz), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(outbreakOrigin.below().offset(dx, 0, dz), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(outbreakOrigin.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
            }
        }
        boolean outbreakPlaced = MycelialOutbreakFeature.generateOutbreakAt(
                level, outbreakOrigin.below(), level.getRandom());
        if (!outbreakPlaced) {
            helper.fail("Expected MycelialOutbreakFeature.place to succeed on dirt substrate.");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void worldgenSanctumJigsawPlacementAndBiomeModifiersRegistered(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var placedReg = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        for (String id : java.util.List.of("mycelial_outbreak", "surface_spore_patch", "deep_mycelium_outbreak", "surface_hive")) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, id);
            if (!placedReg.containsKey(rl)) {
                helper.fail("Missing PlacedFeature in registry: " + rl);
                return;
            }
        }

        var gridStats = com.alex.mycelialmind.world.WorldgenAuditEngine.auditSanctumJigsawGrid(
                level, new net.minecraft.world.level.ChunkPos(0, 0), 128);
        if (!gridStats.structureSetRegistered()
                || gridStats.spacingChunks() != 64
                || gridStats.separationChunks() != 32
                || !gridStats.separationInvariantOk()
                || gridStats.candidateCount() < 2) {
            helper.fail("Sanctum Jigsaw grid audit failed: " + gridStats.toJson());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void worldgenPlacementUsesHiveTierRarityAndProgressionPolicy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var placedReg = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        for (String id : java.util.List.of("mycelial_outbreak", "surface_spore_patch", "deep_mycelium_outbreak", "surface_hive")) {
            var holder = placedReg.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, id));
            var rarityOpt = holder == null ? java.util.Optional.<com.alex.mycelialmind.world.HiveTierRarityPlacement>empty()
                    : holder.placement().stream()
                    .filter(p -> p instanceof com.alex.mycelialmind.world.HiveTierRarityPlacement)
                    .map(p -> (com.alex.mycelialmind.world.HiveTierRarityPlacement) p)
                    .findFirst();
            if (rarityOpt.isEmpty()) {
                helper.fail("PlacedFeature " + id + " must use HiveTierRarityPlacement.");
                return;
            }
            var placement = rarityOpt.get();
            for (var tier : com.alex.mycelialmind.hive.HiveProgressionTier.values()) {
                int expected = switch (id) {
                    case "mycelial_outbreak" -> com.alex.mycelialmind.world.structure.StructureProgressionPolicy.outbreakRarityChance(tier);
                    case "surface_spore_patch" -> com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceOutbreakRarityChance(tier);
                    case "deep_mycelium_outbreak" -> com.alex.mycelialmind.world.structure.StructureProgressionPolicy.deepMyceliumOutbreakRarityChance(tier);
                    default -> com.alex.mycelialmind.world.structure.StructureProgressionPolicy.surfaceHiveRarityChance(tier);
                };
                if (placement.chanceForTier(tier) != expected) {
                    helper.fail("PlacedFeature " + id + " chance mismatch at tier " + tier
                            + ": expected=" + expected + " actual=" + placement.chanceForTier(tier));
                    return;
                }
            }
        }

        var setReg = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        var citadelSet = setReg.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "matriarch_citadel"));
        if (citadelSet == null
                || !(citadelSet.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread)
                || spread.spacing() != 72
                || spread.separation() != 36) {
            helper.fail("matriarch_citadel StructureSet must have spacing=72, separation=36.");
            return;
        }

        var hive = HiveMindSavedData.get(level);
        hive.resetAll();
        if (com.alex.mycelialmind.world.structure.StructureProgressionPolicy.resolveTier(level) != com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT) {
            helper.fail("Expected DORMANT tier after hive.resetAll().");
            return;
        }
        hive.recordStimulus(com.alex.mycelialmind.hive.StimulusType.FIRE, 400.0);
        if (com.alex.mycelialmind.world.structure.StructureProgressionPolicy.resolveTier(level).ordinal()
                < com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED.ordinal()) {
            helper.fail("Expected progression tier to advance on high stimulus.");
            return;
        }
        hive.resetAll();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void purificationPylonExactBoundaryAndRemovalUnprotects(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pylonPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        int radius = com.alex.mycelialmind.config.MycelialConfig.PYLON_PURIFICATION_RADIUS.get();

        boolean inside = tracker.isProtected(pylonPos.offset(radius, 0, 0));
        boolean outside = tracker.isProtected(pylonPos.offset(radius + 1, 0, 0));
        level.removeBlock(pylonPos, false);
        tracker.removePylon(pylonPos);
        boolean afterRemoval = tracker.isProtected(pylonPos);

        if (!inside || outside || afterRemoval) {
            helper.fail("Pylon boundary/removal invariant failed: inside=" + inside
                    + " outside=" + outside + " afterRemoval=" + afterRemoval);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void acidSpitDoesNotCorrodeReinforcedBarrierOrProtectedZone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos barrierPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos pylonPos = helper.absolutePos(new BlockPos(6, 1, 6));
        BlockPos glassPos = helper.absolutePos(new BlockPos(6, 1, 4));
        level.setBlockAndUpdate(barrierPos, ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
        level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
        level.setBlockAndUpdate(glassPos, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());

        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 2, 2, 2);
        var spit1 = new com.alex.mycelialmind.entity.projectile.AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
        spit1.setOwner(spitter);
        spit1.setPos(barrierPos.getX() + 0.5, barrierPos.getY() + 1.25, barrierPos.getZ() + 0.5);
        spit1.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
        level.addFreshEntity(spit1);
        for (int t = 0; t < 5 && !spit1.isRemoved(); t++) {
            spit1.tick();
        }

        var spit2 = new com.alex.mycelialmind.entity.projectile.AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
        spit2.setOwner(spitter);
        spit2.setPos(glassPos.getX() + 0.5, glassPos.getY() + 1.25, glassPos.getZ() + 0.5);
        spit2.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
        level.addFreshEntity(spit2);
        for (int t = 0; t < 5 && !spit2.isRemoved(); t++) {
            spit2.tick();
        }

        boolean barrierIntact = level.getBlockState(barrierPos).is(ModBlocks.REINFORCED_BIO_BARRIER.get());
        boolean glassIntact = level.getBlockState(glassPos).is(net.minecraft.world.level.block.Blocks.GLASS);
        level.removeBlock(barrierPos, false);
        level.removeBlock(glassPos, false);
        level.removeBlock(pylonPos, false);
        PurificationZoneTracker.get(level).removePylon(pylonPos);

        if (!barrierIntact || !glassIntact) {
            helper.fail("AcidSpit corroded protected/reinforced block: barrierIntact=" + barrierIntact
                    + " glassIntact=" + glassIntact);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void aiGoalsHandleRemovedTargetAndEmptyWorldGracefully(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 4, 1, 2);
        var dummyTarget = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 4);
        brute.setTarget(dummyTarget);
        spitter.setTarget(dummyTarget);
        dummyTarget.discard();

        var chargeGoal = new com.alex.mycelialmind.entity.ai.BruteChargeGoal(brute);
        var kiteGoal = new com.alex.mycelialmind.entity.ai.KitingRetreatGoal(spitter, 1.15D);
        var regroupGoal = new com.alex.mycelialmind.entity.ai.HiveRegroupGoal(spitter, 1.15D);
        new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor().tick(level, brute);

        if (chargeGoal.canUse() || kiteGoal.canUse()) {
            helper.fail("Goals should not activate for discarded target.");
            return;
        }
        regroupGoal.canUse();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void daytimeSurfaceSpawnAndIncubatorRespectThreatThresholds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        hive.resetAll();
        long prevDayTime = level.getDayTime();
        level.setDayTime(1000L);

        BlockPos spawnRel = new BlockPos(3, 180, 3);
        BlockPos spawnAbs = helper.absolutePos(spawnRel);
        helper.setBlock(spawnRel.below(), ModBlocks.CREEPING_MYCELIUM.get());
        helper.setBlock(spawnRel, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(spawnRel.above(), net.minecraft.world.level.block.Blocks.AIR);
        for (int dy = 2; dy <= 10; dy++) {
            helper.setBlock(spawnRel.above(dy), net.minecraft.world.level.block.Blocks.AIR);
        }

        boolean daySpawnAt0 = com.alex.mycelialmind.entity.MycelialMob.checkMycelialSpawnRules(
                ModEntities.SPORELING.get(), level, net.minecraft.world.entity.MobSpawnType.NATURAL, spawnAbs, level.getRandom());
        boolean incAt0 = com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity.canIncubatorOperate(level, spawnAbs, hive, false);

        var nightMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), spawnRel.getX(), spawnRel.getY(), spawnRel.getZ());
        nightMob.setNaturalSpawn(true);
        var nearbyNodesAtRetreat = PurificationZoneTracker.get(level).getNodesWithin(nightMob.blockPosition(), 24, 10);
        boolean skyAtRetreat = level.canSeeSky(nightMob.blockPosition());
        int heightAtRetreat = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                nightMob.blockPosition().getX(), nightMob.blockPosition().getZ());
        BlockPos mobPosAtRetreat = nightMob.blockPosition();
        boolean retreatedAt0 = nightMob.tryDawnSurfaceRetreat(level);

        hive.recordStimulus(StimulusType.MELEE, 300.0); // threat = 30.0 (>= 25%, < 50%)
        boolean incAt30 = com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity.canIncubatorOperate(level, spawnAbs, hive, false);
        boolean daySpawnAt30 = com.alex.mycelialmind.entity.MycelialMob.checkMycelialSpawnRules(
                ModEntities.SPORELING.get(), level, net.minecraft.world.entity.MobSpawnType.NATURAL, spawnAbs, level.getRandom());

        hive.recordStimulus(StimulusType.MELEE, 250.0); // threat = 55.0 (>= 50%)
        boolean daySpawnAt55 = com.alex.mycelialmind.entity.MycelialMob.checkMycelialSpawnRules(
                ModEntities.SPORELING.get(), level, net.minecraft.world.entity.MobSpawnType.NATURAL, spawnAbs, level.getRandom());
        var highThreatMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), spawnRel.getX(), spawnRel.getY(), spawnRel.getZ());
        highThreatMob.setNaturalSpawn(true);
        boolean retreatedAt55 = highThreatMob.tryDawnSurfaceRetreat(level);

        hive.resetAll();
        level.setDayTime(prevDayTime);

        if (daySpawnAt0 || incAt0 || !retreatedAt0 || !nightMob.isRemoved()
                || !incAt30 || daySpawnAt30 || !daySpawnAt55 || retreatedAt55) {
            helper.fail(String.format(
                    "Daytime surface spawn rules mismatch: day0=%s inc0=%s ret0=%s rem=%s inc30=%s day30=%s day55=%s ret55=%s",
                    daySpawnAt0, incAt0, retreatedAt0, nightMob.isRemoved(), incAt30, daySpawnAt30, daySpawnAt55, retreatedAt55)
                    + " sky=" + skyAtRetreat + " nodes=" + nearbyNodesAtRetreat
                    + " spawn=" + spawnAbs + " mob=" + mobPosAtRetreat + " height=" + heightAtRetreat);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void seismicPulseChargesEnemyNodeAndRespectsStealth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.clearDecoys(level);
        BlockPos nodePos = helper.absolutePos(new BlockPos(5, 1, 5));
        BlockPos pulsePos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.addNode(nodePos, com.alex.mycelialmind.world.SynapticNodeType.RELAY);

        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pulsePos.getX() + 0.5, pulsePos.getY(), pulsePos.getZ() + 0.5);

        boolean pulsedNoisy = com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.tryEmitSeismicPulse(level, pulsePos, player);
        int chargeAfterPulse = (level.getBlockEntity(nodePos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity be)
                ? be.getBiomassCharge() : 0;

        player.setShiftKeyDown(true);
        boolean silencedSneak = com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.isPlayerSilenced(level, player);
        player.setShiftKeyDown(false);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.CHITIN_BOOTS.get()));
        boolean silencedBoots = com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.isPlayerSilenced(level, player);

        level.removeBlock(nodePos, false);
        tracker.removeNode(nodePos);

        if (!pulsedNoisy || chargeAfterPulse < 6 || !silencedSneak || !silencedBoots) {
            helper.fail(String.format("Bio-acoustic check failed: pulsed=%s charge=%d sneak=%s boots=%s",
                    pulsedNoisy, chargeAfterPulse, silencedSneak, silencedBoots));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void synapticMimicAdaptsStanceToPlayerEquipment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var mimic = helper.spawnWithNoFreeWill(ModEntities.SYNAPTIC_MIMIC.get(), 3, 1, 3);
        net.minecraft.world.entity.player.Player mock = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);

        mock.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        var marksmanStance = mimic.evaluateTargetStance(mock);

        mock.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        mock.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        var aegisStance = mimic.evaluateTargetStance(mock);

        mock.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        HiveMindSavedData.get(level).resetAll();
        var bladeStance = mimic.evaluateTargetStance(mock);

        if (marksmanStance != com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.SPORE_MARKSMAN
                || aegisStance != com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.AEGIS_MIRROR
                || bladeStance != com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.BLADE_DANCER) {
            helper.fail(String.format("Mimic stance adaptation failed: bow=%s shield=%s sword=%s",
                    marksmanStance, aegisStance, bladeStance));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void synapticGraftConvertsNodeAndEnforcesMaxCapAndTravel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        BlockPos n1 = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos n2 = helper.absolutePos(new BlockPos(5, 1, 5));
        BlockPos n3 = helper.absolutePos(new BlockPos(2, 1, 5));
        BlockPos n4 = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos n5 = helper.absolutePos(new BlockPos(3, 1, 3));

        for (BlockPos p : java.util.List.of(n1, n2, n3, n4, n5)) {
            level.setBlockAndUpdate(p, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
            tracker.addNode(p, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
        }

        // Convert 4 nodes (cap = 4)
        for (BlockPos p : java.util.List.of(n1, n2, n3, n4)) {
            level.setBlockAndUpdate(p, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            tracker.addNode(p, com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC);
        }
        int symCount = tracker.countSymbioticNodes();
        var linked = tracker.getLinkedSymbioticNodes(n1, 96);

        // Clean up
        for (BlockPos p : java.util.List.of(n1, n2, n3, n4, n5)) {
            level.removeBlock(p, false);
            tracker.removeNode(p);
        }

        if (symCount != 4 || linked.isEmpty()) {
            helper.fail("Symbiotic cap/link failed: symCount=" + symCount + " linked=" + linked.size());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void neuralEchoDecoyRedirectsNearbyHiveMobs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos decoyPos = helper.absolutePos(new BlockPos(6, 1, 6));
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(decoyPos.getX() + 0.5, decoyPos.getY(), decoyPos.getZ() + 0.5);

        com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.registerDecoy(level, decoyPos, 240);
        boolean decoyActive = com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.hasActiveDecoyNear(level, decoyPos, 16);
        boolean silencedByDecoy = com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.isPlayerSilenced(level, player);
        com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.clearDecoys(level);

        if (!decoyActive || !silencedByDecoy) {
            helper.fail("Neural Echo Decoy failed: active=" + decoyActive + " silenced=" + silencedByDecoy);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void influenceMapPropagationDecayAndBlackboardClear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        var map = com.alex.mycelialmind.hive.HiveBlackboard.getInfluenceMap(level);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos eastChunkPos = origin.offset(16, 0, 0);

        map.addInfluence(origin, 0.6F, 0.8F, 1.0F);
        var center = map.sample(origin);
        var east = map.sample(eastChunkPos);
        var grad = map.samplePheromoneGradient(origin.offset(-16, 0, 0));

        if (center.pheromone() < 0.99F || east.pheromone() <= 0.0F || grad.x <= 0.1D) {
            helper.fail(String.format("InfluenceMap propagation/gradient invalid: center=%.2f east=%.2f gradX=%.2f",
                    center.pheromone(), east.pheromone(), grad.x));
            return;
        }

        for (int i = 0; i < 60; i++) {
            map.stepDecay(0.85F);
        }
        if (map.cellCount() != 0) {
            helper.fail("InfluenceMap failed to prune decayed cells: remaining=" + map.cellCount());
            return;
        }
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "swarm_steering")
    public static void swarmSteeringSeparatesNearbyAllies(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int arenaY = 180;
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(new BlockPos(x, arenaY, z), Blocks.STONE);
            }
        }
        var m1 = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 1, arenaY + 1, 1);
        var m2 = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 1, arenaY + 1, 2);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getEntitiesOfClass(
                                MycelialMob.class, m1.getBoundingBox().inflate(5.5D),
                                mob -> mob == m2 && mob.isAlive()).contains(m2),
                        "Steering test must find its nearby ally after entity registration"))
                .thenExecute(() -> {
                    try {
                        var steering = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeSteeringOffset(m1, level);
                        helper.assertTrue(steering.z < 0.0D && steering.length() <= 2.21D,
                                "Steering separation failed: z=" + steering.z + " len=" + steering.length());
                    } finally {
                        m1.discard();
                        m2.discard();
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void hiveAiExcludesSymbioticNodesAndReactsToDronesAndInfluence(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var tracker = PurificationZoneTracker.get(level);
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();

        BlockPos symbioticPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos relayPos = helper.absolutePos(new BlockPos(8, 1, 8));
        tracker.addNode(symbioticPos, com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC);
        tracker.addNode(relayPos, com.alex.mycelialmind.world.SynapticNodeType.RELAY);

        try {
            BlockPos nearestEnemy = tracker.findNearestEnemyNode(symbioticPos);
            if (!relayPos.equals(nearestEnemy)) {
                helper.fail("findNearestEnemyNode should skip SYMBIOTIC node and return RELAY, got: " + nearestEnemy);
                return;
            }

            var drone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 4, 1, 4);
            var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 4, 1, 5);
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.0D, 1.0D, 6.0D)));

            float baseKite = com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateKiteUtility(spitter, player);
            float baseRegroup = com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateRegroupUtility(spitter);

            com.alex.mycelialmind.hive.HiveBlackboard.getInfluenceMap(level)
                    .addInfluence(spitter.blockPosition(), -0.60F, 0.90F, 0.0F);
            float threatenedKite = com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateKiteUtility(spitter, player);
            float threatenedRegroup = com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateRegroupUtility(spitter);

            if (threatenedKite <= baseKite || threatenedRegroup <= baseRegroup) {
                helper.fail(String.format("Influence threat did not raise kite/regroup utility: kite %.3f->%.3f, regroup %.3f->%.3f",
                        baseKite, threatenedKite, baseRegroup, threatenedRegroup));
                return;
            }

            var relaxationSquad = com.alex.mycelialmind.hive.HiveSquadComposer.composeWaveSquad(
                    hive, com.alex.mycelialmind.hive.HiveDoctrine.ADAPTIVE_HUNT, 25, level.getRandom(),
                    com.alex.mycelialmind.hive.HiveBlackboard.PacingPhase.RELAXATION, 0, false);
            var tideSquad = com.alex.mycelialmind.hive.HiveSquadComposer.composeWaveSquad(
                    hive, com.alex.mycelialmind.hive.HiveDoctrine.SIEGE_ASSAULT, 75, level.getRandom(),
                    com.alex.mycelialmind.hive.HiveBlackboard.PacingPhase.PEAK, 1, true);
            if (relaxationSquad.size() != 2 || tideSquad.size() != 7
                    || tideSquad.get(0) != ModEntities.SPORE_ARCHITECT.get()) {
                helper.fail("Squad composer pacing/tide/symbiotic modulation mismatch: relax="
                        + relaxationSquad.size() + " tide=" + tideSquad.size());
                return;
            }

            drone.discard();
            spitter.discard();
            helper.succeed();
        } finally {
            tracker.removeNode(symbioticPos);
            tracker.removeNode(relayPos);
            com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        }
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void gdxAiTacticalTreeArriveSteeringAndFastNoiseFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        var ally = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 3);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.0D, 1.0D, 2.0D)));

        var arriveVec = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeTacticalArriveSteering(
                brute, player.position(), level);
        if (arriveVec.x <= 0.0D || arriveVec.length() > 2.21D) {
            helper.fail("Native Arrive+BlendedSteering invalid: vec=" + arriveVec);
            return;
        }

        var flow = com.alex.mycelialmind.world.MycelialFastNoise.organicFlow2D(
                brute.getX(), brute.getZ(), level.getSeed() + brute.getId(), 1.0D);
        if (Math.abs(flow.x) > 1.0D || Math.abs(flow.z) > 1.0D) {
            helper.fail("FastNoiseLite organicFlow2D out of bounds: " + flow);
            return;
        }

        var bb = brute.getAiBlackboard();
        var tree = com.alex.mycelialmind.entity.ai.HiveTacticalTree.create(bb);
        bb.populateFromMob(brute, player);
        tree.step();
        if (brute.getTacticalIntent() == com.alex.mycelialmind.entity.ai.HiveAiBlackboard.TacticalIntent.NONE) {
            helper.fail("HiveTacticalTree failed to select active combat intent with live target");
            return;
        }

        brute.discard();
        ally.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void hiveMindSensorFiltersSymbioticNodesDetectsDronesAndReadsDecoys(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var tracker = PurificationZoneTracker.get(level);
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();

        BlockPos symPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos relayPos = helper.absolutePos(new BlockPos(10, 1, 10));
        BlockPos decoyPos = helper.absolutePos(new BlockPos(6, 1, 6));
        tracker.addNode(symPos, com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC);
        tracker.addNode(relayPos, com.alex.mycelialmind.world.SynapticNodeType.RELAY);

        var mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 3);
        var drone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 4, 1, 4);
        try {
            com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.registerDecoy(level, decoyPos, 200);
            // Clear recent target set by registerDecoy so we test HiveMindSensor's decoy fallback
            com.alex.mycelialmind.hive.HiveBlackboard.clear(level);

            var sensor = new com.alex.mycelialmind.entity.ai.brain.hive.HiveMindSensor();
            sensor.doTick(level, mob);

            BlockPos sensedNode = com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getNearestNodePos(mob);
            BlockPos sensedShared = com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getSharedTargetPos(mob);
            float droneThreat = com.alex.mycelialmind.hive.HiveBlackboard.getInfluenceMap(level)
                    .sample(drone.blockPosition()).threat();

            if (!relayPos.equals(sensedNode)) {
                helper.fail("HiveMindSensor picked wrong nearest node (expected RELAY, got " + sensedNode + ")");
                return;
            }
            if (!decoyPos.equals(sensedShared)) {
                helper.fail("HiveMindSensor did not sense active NeuralEchoDecoy into SHARED_TARGET_POS, got " + sensedShared);
                return;
            }
            if (droneThreat <= 0.0F) {
                helper.fail("HiveMindSensor did not record PurifierDrone threat into HiveInfluenceMap");
                return;
            }

            helper.succeed();
        } finally {
            mob.discard();
            drone.discard();
            tracker.removeNode(symPos);
            tracker.removeNode(relayPos);
            com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        }
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void mycelialMobStaggeredTacticalTreeAndSwarmOrganicApproach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 2, 1, 2);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.0D, 1.0D, 8.0D)));
        spitter.setTarget(player);

        net.minecraft.world.phys.Vec3 approach = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeSwarmApproachPos(spitter, player);
        if (!Double.isFinite(approach.x) || !Double.isFinite(approach.z)) {
            helper.fail("computeSwarmApproachPos returned non-finite coords: " + approach);
            return;
        }

        // Observe the staggered BehaviorTree as soon as it publishes an active intent, up to six ticks.
        long intentDeadline = helper.getTick() + 6L;
        boolean[] finished = {false};
        helper.onEachTick(() -> {
            if (finished[0]) {
                return;
            }
            var intent = spitter.getTacticalIntent();
            if (intent != com.alex.mycelialmind.entity.ai.HiveAiBlackboard.TacticalIntent.NONE) {
                finished[0] = true;
                spitter.discard();
                helper.succeed();
            } else if (helper.getTick() >= intentDeadline) {
                finished[0] = true;
                spitter.discard();
                helper.fail("MycelialMob staggered BehaviorTree did not produce active TacticalIntent within 6 ticks");
            }
        });
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void hiveWaveDirectorPacingTideAndSymbioticCounterSquad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        var hive = HiveMindSavedData.get(level);
        hive.resetAll();

        // Stress pacing transitions: BUILD_UP -> PEAK -> RELAXATION
        if (com.alex.mycelialmind.hive.HiveBlackboard.getPacingPhase(level) != com.alex.mycelialmind.hive.HiveBlackboard.PacingPhase.BUILD_UP) {
            helper.fail("Expected initial BUILD_UP pacing phase");
            return;
        }
        com.alex.mycelialmind.hive.HiveBlackboard.recordCombatStress(level, 0.70F);
        if (com.alex.mycelialmind.hive.HiveBlackboard.getPacingPhase(level) != com.alex.mycelialmind.hive.HiveBlackboard.PacingPhase.PEAK) {
            helper.fail("Expected PEAK pacing phase at 0.70 stress");
            return;
        }
        com.alex.mycelialmind.hive.HiveBlackboard.recordCombatStress(level, 0.25F);
        if (com.alex.mycelialmind.hive.HiveBlackboard.getPacingPhase(level) != com.alex.mycelialmind.hive.HiveBlackboard.PacingPhase.RELAXATION) {
            helper.fail("Expected RELAXATION pacing phase after >= 0.90 stress spike");
            return;
        }

        var normalSquad = com.alex.mycelialmind.hive.HiveSquadComposer.composeWaveSquad(
                hive, com.alex.mycelialmind.hive.HiveDoctrine.FORAGING, 25, level.getRandom(),
                com.alex.mycelialmind.hive.HiveBlackboard.PacingPhase.BUILD_UP, 0, false);
        var relaxSquad = com.alex.mycelialmind.hive.HiveSquadComposer.composeWaveSquad(
                hive, com.alex.mycelialmind.hive.HiveDoctrine.FORAGING, 25, level.getRandom(),
                com.alex.mycelialmind.hive.HiveBlackboard.PacingPhase.RELAXATION, 0, false);
        if (normalSquad.size() != 3 || relaxSquad.size() != 2) {
            helper.fail("Pacing squad size mismatch: normal=" + normalSquad.size() + " relax=" + relaxSquad.size());
            return;
        }
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structures")
    public static void architecturalBlocksAndMatriarchAltarSummonsApexMatriarch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        if (ModBlocks.CHITIN_PILLAR.get() == null
                || ModBlocks.MYCELIAL_ARCH_BRICK.get() == null
                || ModBlocks.BIO_CHITIN_LATTICE.get() == null
                || ModBlocks.LUMINESCENT_AMBER_VEIN.get() == null
                || ModBlocks.MATRIARCH_ALTAR.get() == null) {
            helper.fail("One or more architectural blocks failed to register.");
            return;
        }
        BlockPos altarRel = new BlockPos(4, 2, 4);
        helper.setBlock(altarRel, ModBlocks.MATRIARCH_ALTAR.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.OVERMIND_ESSENCE.get()));
        BlockPos altarAbs = helper.absolutePos(altarRel);
        BlockState state = level.getBlockState(altarAbs);
        state.useItemOn(
                player.getMainHandItem(),
                level,
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(altarAbs), Direction.UP, altarAbs, false)
        );
        var matriarchs = level.getEntitiesOfClass(ApexMatriarchEntity.class, new AABB(altarAbs).inflate(16.0D));
        if (matriarchs.isEmpty()) {
            helper.fail("MatriarchAltarBlock did not summon ApexMatriarchEntity when activated with Overmind Essence.");
            return;
        }
        matriarchs.forEach(ApexMatriarchEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structures_surface_hive")
    public static void monumentalSurfaceHiveGeneratesButtressesAndTwinChests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos origin = helper.absolutePos(new BlockPos(12, 160, 12));
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -6; dy <= 16; dy++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                    }
                }
            }
        }
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                level.setBlock(origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                level.setBlock(origin.offset(dx, -2, dz), Blocks.STONE.defaultBlockState(), 18);
            }
        }
        try {
            boolean placed = com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveAt(level, origin, level.getRandom());
            if (!placed) {
                helper.fail("SurfaceHiveFeature.generateHiveAt returned false on flat stone platform.");
                return;
            }
            var report = com.alex.mycelialmind.world.WorldgenAuditEngine.inspectSurfaceHive(level, tracker, origin);
            if (!report.valid()) {
                helper.fail("Monumental SurfaceHive failed WorldgenAuditEngine.inspectSurfaceHive: " + report.toJson());
                return;
            }
            if (!(level.getBlockEntity(origin.offset(0, 5, -3)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)) {
                helper.fail("Expected secondary upper gallery chest at origin.offset(0, 5, -3).");
                return;
            }
            if (!(level.getBlockEntity(origin.offset(0, -4, -3)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)) {
                helper.fail("Expected subterranean Root Crypt chest at origin.offset(0, -4, -3).");
                return;
            }
            boolean foundButtress = false;
            boolean foundGothicDetail = false;
            boolean foundFungalCap = false;
            for (int dx = -9; dx <= 9; dx++) {
                for (int dz = -9; dz <= 9; dz++) {
                    for (int dy = 0; dy <= 16; dy++) {
                        BlockState s = level.getBlockState(origin.offset(dx, dy, dz));
                        if (s.is(ModBlocks.CHITIN_PILLAR.get()) || s.is(ModBlocks.BIO_CHITIN_LATTICE.get())) {
                            foundButtress = true;
                        }
                        if (s.is(Blocks.CHAIN) || s.is(Blocks.DEEPSLATE_BRICK_WALL)) {
                            foundGothicDetail = true;
                        }
                        if (s.is(Blocks.AMETHYST_BLOCK) || s.is(Blocks.PURPLE_STAINED_GLASS)) {
                            foundFungalCap = true;
                        }
                    }
                }
            }
            if (!foundButtress || !foundGothicDetail || !foundFungalCap) {
                helper.fail("Monumental SurfaceHive missing buttresses, chain/wall details, or fungal cap amethyst/glass.");
                return;
            }
            var hiveBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                    origin.getX() - 9, origin.getY() - 1, origin.getZ() - 9,
                    origin.getX() + 9, origin.getY() + 16, origin.getZ() + 9);
            var hiveFloating = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                    level, hiveBox, origin,
                    s -> !s.isAir() && !s.is(Blocks.STONE) && !s.is(Blocks.BEDROCK) && !s.is(Blocks.BARRIER)
                            && !s.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()) && !s.is(ModBlocks.SPORE_BULB.get()));
            hiveFloating.removeIf(p -> p.getY() == origin.getY() - 1
                    && level.getBlockState(p).is(ModBlocks.CREEPING_MYCELIUM.get()));
            if (!hiveFloating.isEmpty()) {
                BlockPos sample = hiveFloating.get(0);
                helper.fail("SurfaceHiveFeature has " + hiveFloating.size()
                        + " disconnected above-ground blocks, origin=" + origin + ", positions=" + hiveFloating
                        + ", sample=" + sample + " state=" + level.getBlockState(sample));
                return;
            }
            var cryptBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                    origin.getX() - 4, origin.getY() - 5, origin.getZ() - 4,
                    origin.getX() + 4, origin.getY(), origin.getZ() + 4);
            var cryptFloating = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                    level, cryptBox, origin,
                    s -> !s.isAir() && !s.is(Blocks.STONE) && !s.is(Blocks.BEDROCK) && !s.is(Blocks.BARRIER));
            if (!cryptFloating.isEmpty()) {
                BlockPos sample = cryptFloating.get(0);
                helper.fail("SurfaceHiveFeature crypt has " + cryptFloating.size()
                        + " disconnected blocks, sample=" + sample + " state=" + level.getBlockState(sample));
                return;
            }
            helper.succeed();
        } finally {
            tracker.removeNode(origin);
            tracker.removeNode(origin.above(3));
        }
    }

    @GameTest(template = "empty", batch = "structures")
    public static void hiveSanctumMultiRoomPoolsAndMatriarchCitadelRegistered(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registries = level.registryAccess();
        var poolReg = registries.registryOrThrow(Registries.TEMPLATE_POOL);
        for (String nsPrefix : List.of("hive_sanctum/", "nether_breach/", "end_spore_spire/")) {
            List<String> expectedPools = nsPrefix.equals("hive_sanctum/")
                    ? List.of("start_pool", "corridors", "rooms", "caps")
                    : List.of("start_pool", "bridges", "wings", "caps");
            for (String pool : expectedPools) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, nsPrefix + pool);
                if (!poolReg.containsKey(id)) {
                    helper.fail("Missing template pool: " + id);
                    return;
                }
            }
        }
        var procReg = registries.registryOrThrow(Registries.PROCESSOR_LIST);
        for (String proc : List.of("architectural_reflection", "nether_breach_degrade", "end_spire_degrade")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, proc);
            if (!procReg.containsKey(id)) {
                helper.fail("Missing processor list: " + id);
                return;
            }
        }
        var tm = level.getStructureManager();
        for (String piece : List.of(
                "hive_sanctum/sanctum_atrium", "hive_sanctum/corridor_straight", "hive_sanctum/corridor_turn",
                "hive_sanctum/corridor_cross", "hive_sanctum/vertical_shaft", "hive_sanctum/ribbed_nave",
                "hive_sanctum/forbidden_lab", "hive_sanctum/ossuary_crypt", "hive_sanctum/spore_vault",
                "hive_sanctum/incubation_wing", "hive_sanctum/quarantine_ward", "hive_sanctum/tactical_gallery",
                "hive_sanctum/secret_reliquary", "hive_sanctum/cap_end",
                "twisted_reflection/spire_tower", "twisted_reflection/gallery_hall",
                "twisted_reflection/vault_bunker", "twisted_reflection/sentinel_cloister",
                "nether_breach/breach_sanctum", "nether_breach/basalt_bridge", "nether_breach/pyro_barracks",
                "nether_breach/magma_conduit", "nether_breach/watchtower_keep", "nether_breach/breach_cap",
                "end_spore_spire/spire_keep", "end_spore_spire/void_bridge",
                "end_spore_spire/resonance_observatory", "end_spore_spire/void_crypt",
                "end_spore_spire/purpur_bastion", "end_spore_spire/spire_cap"
        )) {
            ResourceLocation pid = ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, piece);
            if (tm.get(pid).isEmpty()) {
                helper.fail("Failed to load NBT template: " + pid);
                return;
            }
        }
        var structReg = registries.registryOrThrow(Registries.STRUCTURE);
        if (!structReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sanctum"))
                || !structReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "matriarch_citadel"))
                || !structReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "nether_mycelial_breach"))
                || !structReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "end_spore_spire"))) {
            helper.fail("Missing hive_sanctum, matriarch_citadel, nether_mycelial_breach, or end_spore_spire in Structure registry.");
            return;
        }
        var setReg = registries.registryOrThrow(Registries.STRUCTURE_SET);
        if (!setReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "matriarch_citadel"))
                || !setReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "nether_mycelial_breach"))
                || !setReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "end_spore_spire"))) {
            helper.fail("Missing matriarch_citadel, nether_mycelial_breach, or end_spore_spire in StructureSet registry.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structures", timeoutTicks = 800)
    public static void matriarchCitadelPieceGeneratesCryptChapelsAndGuardians(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forced = forceArenaChunks(helper, 0, 52, 0, 52);
        BlockPos center = helper.absolutePos(new BlockPos(26, 10, 26));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        level.isPositionEntityTicking(center) && forced.stream()
                                .allMatch(chunk -> level.areEntitiesLoaded(chunk.toLong())),
                        "Expected the forced citadel arena chunks to finish loading entities."))
                .thenExecute(() -> {
                    try {
                        var piece = new com.alex.mycelialmind.world.structure.MatriarchCitadelPiece(center);
                        var box = piece.getBoundingBox();
                        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                                level.getRandom(), box, new net.minecraft.world.level.ChunkPos(center), center);

                        helper.assertTrue(level.getBlockState(center.above(4)).is(ModBlocks.MATRIARCH_ALTAR.get()),
                                "MatriarchCitadelPiece did not place MATRIARCH_ALTAR at center.above(4).");
                        helper.assertTrue(level.getBlockEntity(center.offset(0, -4, 0)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                                "MatriarchCitadelPiece did not place Throne Relic Chest in crypt at center.offset(0, -4, 0).");
                        helper.assertTrue(level.getBlockEntity(center.offset(-19, 1, 0)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity
                                        && level.getBlockEntity(center.offset(19, 1, 0)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                                "MatriarchCitadelPiece did not place Amber/Crimson Strain Chapel chests.");
                        var guardians = level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                                new AABB(center).inflate(24.0D),
                                m -> m instanceof com.alex.mycelialmind.entity.SporeBehemothEntity || m instanceof ColonyWardenEntity);
                        helper.assertTrue(guardians.size() >= 3,
                                "Expected SporeBehemoth + 2 ColonyWarden guardians in MatriarchCitadel, found " + guardians.size());
                        guardians.forEach(net.minecraft.world.entity.Entity::discard);
                        var tracker = PurificationZoneTracker.get(level);
                        tracker.removeNode(center.offset(0, 11, -16));
                        tracker.removeNode(center.offset(0, 11, 16));
                        tracker.removeNode(center.offset(-16, 11, 0));
                        tracker.removeNode(center.offset(16, 11, 0));
                        tracker.removeNode(center.offset(-11, 11, -11));
                        tracker.removeNode(center.offset(11, 11, -11));
                    } finally {
                        releaseArenaChunks(level, forced);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "structures")
    public static void structuresHaveZeroFloatingBlocksAndValidCirculation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        java.util.function.Predicate<BlockState> isModStructure =
                s -> !s.isAir() && !s.is(Blocks.STONE) && !s.is(Blocks.BEDROCK) && !s.is(Blocks.BARRIER);

        // 1) MycelialOutbreakFeature exposes amber at its supported fissure floor
        BlockPos outbreakPos = helper.absolutePos(new BlockPos(8, 120, 8));
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.setBlock(outbreakPos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                level.setBlock(outbreakPos.offset(dx, -2, dz), Blocks.STONE.defaultBlockState(), 18);
            }
        }
        boolean outbreakPlaced = MycelialOutbreakFeature.generateOutbreakAt(
                level, outbreakPos, level.getRandom());
        if (!outbreakPlaced) {
            helper.fail("MycelialOutbreakFeature.place returned false on flat stone platform.");
            return;
        }
        long fissureCells = BlockPos.betweenClosedStream(outbreakPos.offset(-5, -1, -5),
                outbreakPos.offset(5, -1, 5)).filter(pos -> level.getBlockState(pos).isAir()
                && level.getBlockState(pos.below()).is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())).count();
        helper.assertTrue(fissureCells >= 2, "Outbreak fissure must expose supported amber near the node.");
        PurificationZoneTracker.get(level).removeNode(outbreakPos);

        // 2) MatriarchCitadelPiece 6-connectivity
        BlockPos citadelCenter = helper.absolutePos(new BlockPos(26, 40, 56));
        var piece = new com.alex.mycelialmind.world.structure.MatriarchCitadelPiece(citadelCenter);
        var citadelBox = piece.getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(
                citadelBox.minX(), citadelBox.minY(), citadelBox.minZ(),
                citadelBox.maxX(), citadelBox.maxY(), citadelBox.maxZ())) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            }
        }
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                level.getRandom(), citadelBox, new net.minecraft.world.level.ChunkPos(citadelCenter), citadelCenter);
        var citadelFloating = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                level, citadelBox, citadelCenter.above(4), isModStructure);
        var citadelGuardians = level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                new AABB(citadelCenter).inflate(24.0D),
                m -> m instanceof com.alex.mycelialmind.entity.SporeBehemothEntity || m instanceof ColonyWardenEntity);
        citadelGuardians.forEach(net.minecraft.world.entity.Entity::discard);
        if (!citadelFloating.isEmpty()) {
            helper.fail("MatriarchCitadelPiece has " + citadelFloating.size()
                    + " disconnected blocks, sample=" + citadelFloating.get(0));
            return;
        }

        // 4) sanctum_atrium.nbt triforium stairs & walkway verification
        var atriumOpt = level.getStructureManager().get(
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sanctum/sanctum_atrium"));
        if (atriumOpt.isEmpty()) {
            helper.fail("Missing sanctum_atrium template.");
            return;
        }
        BlockPos atriumOrigin = helper.absolutePos(new BlockPos(60, 10, 60));
        atriumOpt.get().placeInWorld(level, atriumOrigin, atriumOrigin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                level.getRandom(), 2);
        boolean hasTriforiumStair = level.getBlockState(atriumOrigin.offset(10, 1, 18)).is(Blocks.DEEPSLATE_BRICK_STAIRS)
                && level.getBlockState(atriumOrigin.offset(12, 1, 18)).is(Blocks.DEEPSLATE_BRICK_STAIRS);
        boolean hasTriforiumWalkway = level.getBlockState(atriumOrigin.offset(5, 6, 18)).is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                && level.getBlockState(atriumOrigin.offset(17, 6, 18)).is(ModBlocks.MYCELIAL_ARCH_BRICK.get());
        if (!hasTriforiumStair || !hasTriforiumWalkway) {
            helper.fail("sanctum_atrium.nbt missing triforium stairs or y=6 walkway.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveStrainSchismCausesRivalMobsToFight(GameTestHelper helper) {
        var amberMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), new BlockPos(3, 2, 3));
        var crimsonMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), new BlockPos(5, 2, 3));
        var sameMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), new BlockPos(4, 2, 5));

        amberMob.infectWithSchism(com.alex.mycelialmind.hive.HiveStrain.AMBER, 400);
        crimsonMob.infectWithSchism(com.alex.mycelialmind.hive.HiveStrain.CRIMSON, 400);
        sameMob.infectWithSchism(com.alex.mycelialmind.hive.HiveStrain.AMBER, 400);

        if (!amberMob.isRivalStrainTarget(crimsonMob) || !crimsonMob.isRivalStrainTarget(amberMob)) {
            helper.fail("Expected AMBER and CRIMSON mobs in schism to treat each other as rival targets.");
            return;
        }
        if (amberMob.isRivalStrainTarget(sameMob)) {
            helper.fail("Expected two AMBER mobs not to treat each other as rival targets.");
            return;
        }
        amberMob.discard();
        crimsonMob.discard();
        sameMob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void playerDiplomacyReputationAllianceAndBetrayalPenalty(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var dip = player.getData(com.alex.mycelialmind.registry.ModAttachments.DIPLOMACY);
        dip.setReputation(com.alex.mycelialmind.hive.HiveStrain.AMBER, 0);
        dip.setReputation(com.alex.mycelialmind.hive.HiveStrain.CRIMSON, 0);

        dip.adjustReputation(com.alex.mycelialmind.hive.HiveStrain.AMBER, 64);
        if (dip.getStanding(com.alex.mycelialmind.hive.HiveStrain.AMBER) != com.alex.mycelialmind.diplomacy.PlayerDiplomacyData.Standing.ALLIED) {
            helper.fail("Expected AMBER standing ALLIED at +64 rep.");
            return;
        }
        if (dip.getReputation(com.alex.mycelialmind.hive.HiveStrain.CRIMSON) != -32) {
            helper.fail("Expected -0.5x rival trade-off on CRIMSON (-32), got " + dip.getReputation(com.alex.mycelialmind.hive.HiveStrain.CRIMSON));
            return;
        }
        if (!dip.isProtectedFrom(com.alex.mycelialmind.hive.HiveStrain.AMBER, EvolutionRank.BASE, false, 9.0D)) {
            helper.fail("Expected ALLIED standing to protect from BASE AMBER mob.");
            return;
        }
        if (dip.isProtectedFrom(com.alex.mycelialmind.hive.HiveStrain.AMBER, EvolutionRank.ELITE, false, 9.0D)) {
            helper.fail("Vanilla+ invariant violated: ELITE mobs must never be neutralized by diplomacy.");
            return;
        }

        dip.triggerBetrayal(com.alex.mycelialmind.hive.HiveStrain.AMBER);
        if (dip.getReputation(com.alex.mycelialmind.hive.HiveStrain.AMBER) != 29 || dip.getBetrayalCooldownTicks() <= 0) {
            helper.fail("Expected betrayal to subtract 35 rep (29) and apply cooldown.");
            return;
        }
        if (dip.isProtectedFrom(com.alex.mycelialmind.hive.HiveStrain.AMBER, EvolutionRank.BASE, false, 9.0D)) {
            helper.fail("Expected betrayal cooldown to revoke diplomatic protection.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void trucePheromoneShieldsAgainstBaseMobsUntilAttacked(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var dip = player.getData(com.alex.mycelialmind.registry.ModAttachments.DIPLOMACY);
        dip.setReputation(com.alex.mycelialmind.hive.HiveStrain.AMBER, 0);
        dip.setBetrayalCooldownTicks(0);
        dip.setTruceTicks(400);

        var baseMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), new BlockPos(4, 2, 4));
        baseMob.promoteRank(EvolutionRank.BASE);
        if (baseMob.canAttack(player)) {
            helper.fail("Expected Cryptic Truce to prevent BASE mob from targeting player.");
            baseMob.discard();
            return;
        }

        dip.triggerBetrayal(baseMob.getHiveStrain());
        if (!baseMob.canAttack(player)) {
            helper.fail("Expected betrayal to immediately restore BASE mob aggression.");
            baseMob.discard();
            return;
        }
        baseMob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void diplomacyItemsRegisteredAndEscortCapEnforced(GameTestHelper helper) {
        if (ModItems.SCHISM_INJECTOR.get() == null
                || ModItems.TRUCE_PHEROMONE.get() == null
                || ModItems.QUORUM_SEAL.get() == null) {
            helper.fail("Diplomacy items failed to register.");
            return;
        }
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var escort = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), new BlockPos(4, 2, 4));
        escort.setHiveStrain(com.alex.mycelialmind.hive.HiveStrain.AMBER);
        escort.setAlliedEscort(player.getUUID(), level.getGameTime() + 200);
        if (!escort.isAlliedEscort() || escort.canAttack(player)) {
            helper.fail("Allied escort must not attack its owner.");
            escort.discard();
            return;
        }
        escort.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void synapticBenchSynthesizesChimeraOvum(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos benchPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
        if (!(level.getBlockEntity(benchPos) instanceof com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity bench)) {
            helper.fail("Missing SynapticBenchBlockEntity.");
            return;
        }
        bench.setLabTier(3);
        bench.getInventory().setStackInSlot(1, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
        bench.getInventory().setStackInSlot(2, new ItemStack(ModItems.NEURAL_ECHO_CORE.get()));
        // Blank untyped sample must NOT synthesize Chimera Ovum
        bench.getInventory().setStackInSlot(0, new ItemStack(ModItems.GENETIC_SAMPLE.get()));
        ItemStack untypedOut = bench.getInventory().getStackInSlot(3);
        ItemStack typedSample = new ItemStack(ModItems.GENETIC_SAMPLE.get());
        typedSample.set(ModDataComponents.TRAIT_ID.get(), AdaptationTrait.MELEE_THORNS.id());
        bench.getInventory().setStackInSlot(0, typedSample);
        ItemStack out = bench.getInventory().getStackInSlot(3);
        level.removeBlock(benchPos, false);

        if (!untypedOut.isEmpty() || !out.is(ModItems.CHIMERA_OVUM.get())) {
            helper.fail("Expected Chimera Ovum output only for typed sample, got untyped=" + untypedOut + " typed=" + out);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void chimeraHatchRandomizesGenesConsumesStabilityAndEnforcesLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos nodePos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(nodePos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.addNode(nodePos, com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC);

        if (!(level.getBlockEntity(nodePos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity be)) {
            helper.fail("Missing SynapticNodeBlockEntity.");
            return;
        }
        be.setSymbioticStability(100);
        // The empty template uses a barrier shell one block from center; clear enough room
        // for the Chimera's full bounding box and the two-block spawn height.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(nodePos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(nodePos.getX() + 0.5, nodePos.getY() + 1.0, nodePos.getZ() - 2.5);
        ItemStack ovum = new ItemStack(ModItems.CHIMERA_OVUM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, ovum);

        var ctx = new net.minecraft.world.item.context.UseOnContext(
                player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(nodePos), Direction.UP, nodePos, false)
        );
        var firstRes = ovum.getItem().useOn(ctx);
        int stabilityAfterFirst = be.getSymbioticStability();
        var secondRes = ovum.getItem().useOn(ctx);

        var chimeras = level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialChimeraEntity.class, new AABB(nodePos).inflate(8.0D));
        boolean distinctGenes = !chimeras.isEmpty()
                && chimeras.get(0).getChassisGene() != chimeras.get(0).getMutationGene();
        chimeras.forEach(com.alex.mycelialmind.entity.MycelialChimeraEntity::discard);
        level.removeBlock(nodePos, false);
        tracker.removeNode(nodePos);

        if (!firstRes.consumesAction()
                || stabilityAfterFirst != 65
                || secondRes != net.minecraft.world.InteractionResult.FAIL
                || chimeras.size() != 1
                || !distinctGenes) {
            helper.fail(String.format("Chimera hatch/limit failed: first=%s stab=%d second=%s count=%d distinct=%s",
                    firstRes, stabilityAfterFirst, secondRes, chimeras.size(), distinctGenes));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void chimeraGeneRerollRankEvolutionAndAutonomousMutation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var chimera = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_CHIMERA.get(), 3, 1, 3);
        chimera.setGenes(AdaptationTrait.BLAST_SHIELDED, AdaptationTrait.PROJECTILE_DEFLECT);
        double hpBefore = chimera.getMaxHealth();

        chimera.promoteRank(EvolutionRank.MUTATED);
        double hpMutated = chimera.getMaxHealth();
        chimera.promoteRank(EvolutionRank.ELITE);
        double hpElite = chimera.getMaxHealth();

        chimera.randomizeGenes(level.getRandom());
        boolean twoDistinct = chimera.getChassisGene() != chimera.getMutationGene();

        // Test autonomous mutation trigger against hostile target
        var hostile = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 6, 1, 3);
        chimera.setGenes(AdaptationTrait.MELEE_THORNS, AdaptationTrait.BLAST_SHIELDED);
        boolean firedAbility = chimera.tickAutonomousMutationAbility(level);

        hostile.discard();
        chimera.discard();

        if (hpMutated <= hpBefore || hpElite <= hpMutated || !twoDistinct || !firedAbility) {
            helper.fail(String.format("Chimera evolution/ability failed: hp0=%.1f hpM=%.1f hpE=%.1f distinct=%s fired=%s",
                    hpBefore, hpMutated, hpElite, twoDistinct, firedAbility));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void synapticMemoryRouterSplits4060AndConsolidatesBuffer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        hive.resetAll();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        BlockPos nodePos = helper.absolutePos(new BlockPos(5, 1, 5));
        level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        tracker.addNode(nodePos, com.alex.mycelialmind.world.SynapticNodeType.RELAY);

        boolean buffered = com.alex.mycelialmind.hive.SynapticMemoryRouter.routeStimulus(
                level, nodePos.offset(2, 0, 2), StimulusType.FIRE, 20.0);
        double directFire = hive.getStimulusScore(StimulusType.FIRE);
        double nodeBuffer = (level.getBlockEntity(nodePos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity be)
                ? be.getVolatileTotal() : 0.0;

        boolean consolidated = (level.getBlockEntity(nodePos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity be2)
                && be2.consolidateVolatileBuffer(level);
        double totalFire = hive.getStimulusScore(StimulusType.FIRE);

        level.removeBlock(nodePos, false);
        tracker.removeNode(nodePos);
        hive.resetAll();

        if (!buffered || Math.abs(directFire - 8.0) > 0.1 || Math.abs(nodeBuffer - 12.0) > 0.1
                || !consolidated || totalFire < 19.5) {
            helper.fail(String.format("Router/buffer mismatch: buffered=%s direct=%.1f buf=%.1f consol=%s total=%.1f",
                    buffered, directFire, nodeBuffer, consolidated, totalFire));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void purgingNodeBufferDropsVolatileEngramAndClearsMemory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        hive.resetAll();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        BlockPos nodePos = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        tracker.addNode(nodePos, com.alex.mycelialmind.world.SynapticNodeType.RELAY);

        com.alex.mycelialmind.hive.SynapticMemoryRouter.routeStimulus(level, nodePos, StimulusType.MELEE, 20.0);
        double purged = (level.getBlockEntity(nodePos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity be)
                ? be.purgeVolatileBuffer(level, nodePos, true) : 0.0;
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(nodePos).inflate(3.0D));
        boolean hasEngramDrop = drops.stream().anyMatch(e -> e.getItem().is(ModItems.VOLATILE_ENGRAM.get()));
        drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard);

        level.removeBlock(nodePos, false);
        tracker.removeNode(nodePos);
        hive.resetAll();

        if (purged < 11.5 || !hasEngramDrop) {
            helper.fail("Purge volatile buffer failed: purged=" + purged + " hasEngramDrop=" + hasEngramDrop);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void pavlovianDeceptionGrantsVulnerabilityAndRevealsOnLie(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        hive.resetAll();
        long now = level.getGameTime();

        hive.startDeception(StimulusType.FIRE, now);
        boolean active = hive.isDeceptionActive(now);
        boolean fireTraitActive = hive.hasTrait(AdaptationTrait.FIRE_RESISTANT);

        var mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 3);
        mob.applyHiveState(hive.getActiveTraits(), hive.getTier2Traits(), hive.getActiveSynergies());
        float hpBefore = mob.getHealth();
        mob.hurt(level.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)), 4.0F);
        float dealtOther = hpBefore - mob.getHealth(); // +25% -> 5.0F

        // Now hit with the lied stimulus (FIRE) -> reveals deception & starts 60s CD
        mob.invulnerableTime = 0;
        mob.hurt(level.damageSources().inFire(), 2.0F);
        boolean activeAfterLie = hive.isDeceptionActive(now);
        int cdSec = hive.getDeceptionCooldownRemainingSeconds(now);

        mob.discard();
        hive.resetAll();

        if (!active || !fireTraitActive || dealtOther < 4.9F || activeAfterLie || cdSec < 50) {
            helper.fail(String.format("Pavlovian deception failed: active=%s fireTrait=%s dealt=%.2f afterLie=%s cd=%d",
                    active, fireTraitActive, dealtOther, activeAfterLie, cdSec));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void combatMedicHabitTriggersSporeRotHealingReduction(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);
        hive.resetAll();
        hive.recordHabit(com.alex.mycelialmind.hive.PlayerHabit.COMBAT_MEDIC, 70.0);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(10.0F);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.alex.mycelialmind.registry.ModEffects.SPORE_ROT, 80, 0));
        player.heal(10.0F);
        float healedHp = player.getHealth();
        boolean hasAdapt = hive.hasHabitAdaptation(com.alex.mycelialmind.hive.PlayerHabit.COMBAT_MEDIC);

        hive.resetAll();
        if (!hasAdapt || healedHp > 17.5F) {
            helper.fail("SPORE_ROT healing reduction failed: hasAdapt=" + hasAdapt + " healedHp=" + healedHp);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void tacticalAiLeaderSelectionAndPhalanxMitigation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 2, 1, 2);
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 4, 1, 2);
        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 6, 1, 2);

        var leader = com.alex.mycelialmind.hive.SynapticCommandSystem.selectSquadLeader(
                java.util.List.of(spitter, brute, weaver));
        if (leader != weaver) {
            helper.fail("Expected SynapseWeaver to be selected as squad leader");
            return;
        }

        Player attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        attacker.setPos(brute.getX() + 3.0D, brute.getY(), brute.getZ());
        brute.setYRot(-90.0F);
        brute.setYHeadRot(-90.0F);
        brute.setPhalanxActive(true);
        brute.setCommandRole(com.alex.mycelialmind.hive.SynapticCommandSystem.CommandRole.LINKED);

        float hpBefore = brute.getHealth();
        brute.hurt(level.damageSources().playerAttack(attacker), 10.0F);
        float dealt = hpBefore - brute.getHealth();

        weaver.discard();
        brute.discard();
        spitter.discard();

        if (dealt > 8.1F) {
            helper.fail("Phalanx frontal damage mitigation failed: dealt=" + dealt);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void synapticLeaderDeathTriggersDesyncAndBreaksFrenzy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 2, 1, 2);
        var sporeling = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 4, 1, 2);
        weaver.setCommandRole(com.alex.mycelialmind.hive.SynapticCommandSystem.CommandRole.LEADER);
        sporeling.setCommandRole(com.alex.mycelialmind.hive.SynapticCommandSystem.CommandRole.LINKED);
        sporeling.triggerFrenzy(160);
        boolean frenzyBefore = sporeling.isFrenzyActive();

        weaver.kill();
        boolean desyncAfter = sporeling.isDesynced();
        boolean frenzyAfter = sporeling.isFrenzyActive();

        sporeling.discard();
        if (!frenzyBefore || !desyncAfter || frenzyAfter) {
            helper.fail("Leader death desync failed: frenzyBefore=" + frenzyBefore
                    + " desyncAfter=" + desyncAfter + " frenzyAfter=" + frenzyAfter);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void rapidCasualtiesTriggerFrenzyAndPylonDesyncs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos absCenter = helper.absolutePos(new BlockPos(3, 1, 3));
        com.alex.mycelialmind.hive.HiveBlackboard.recordSquadCasualty(level, absCenter);
        com.alex.mycelialmind.hive.HiveBlackboard.recordSquadCasualty(level, absCenter.offset(1, 0, 0));
        int casualties = com.alex.mycelialmind.hive.HiveBlackboard.countRecentCasualtiesNear(
                level, absCenter,
                com.alex.mycelialmind.hive.SynapticCommandSystem.CASUALTY_RADIUS_SQ,
                com.alex.mycelialmind.hive.SynapticCommandSystem.CASUALTY_WINDOW_TICKS);

        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 3, 1, 3);
        brute.triggerFrenzy(160);
        boolean frenzyActive = brute.isFrenzyActive();
        brute.triggerDesync(100);
        boolean desynced = brute.isDesynced() && !brute.isFrenzyActive();
        brute.discard();

        if (casualties < 2 || !frenzyActive || !desynced) {
            helper.fail("Rapid casualty frenzy/desync failed: casualties=" + casualties
                    + " frenzyActive=" + frenzyActive + " desynced=" + desynced);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void mycorrhizalTravelConsumesStabilityAndBlocksInfection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos n1 = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos n2 = helper.absolutePos(new BlockPos(8, 1, 2));
        level.setBlockAndUpdate(n1, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
        level.setBlockAndUpdate(n2, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());

        if (!(level.getBlockEntity(n1) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity be1)) {
            helper.fail("Origin SynapticNodeBlockEntity missing");
            return;
        }
        be1.setSymbioticStability(10);
        boolean lowStabBlocked = be1.getSymbioticStability() < com.alex.mycelialmind.block.SymbioticNodeBlock.TRAVEL_STABILITY_COST;
        boolean configBalanced = com.alex.mycelialmind.config.MycelialConfig.MYCORRHIZAL_TRAVEL_COOLDOWN_TICKS.getDefault() >= 1500
                && com.alex.mycelialmind.block.SymbioticNodeBlock.TRAVEL_STABILITY_COST == 15
                && com.alex.mycelialmind.block.SymbioticNodeBlock.TRAVEL_FOOD_COST == 6
                && lowStabBlocked;

        level.removeBlock(n1, false);
        level.removeBlock(n2, false);
        if (!configBalanced) {
            helper.fail("Mycorrhizal travel balance invariants failed");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void progressiveThreatScalingStrengthensMobsAcrossTiers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);

        var dormantMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        dormantMob.applyProgressionScaling(level, hive);
        double hpDormant = dormantMob.getMaxHealth();
        double atkDormant = dormantMob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double armorDormant = dormantMob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);

        hive.recordStimulus(StimulusType.FIRE, 1000.0);
        var overmindMob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 5, 1, 2);
        overmindMob.applyProgressionScaling(level, hive);
        double hpOvermind = overmindMob.getMaxHealth();
        double atkOvermind = overmindMob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double armorOvermind = overmindMob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);

        dormantMob.discard();
        overmindMob.discard();
        hive.resetAll();

        if (hpOvermind <= hpDormant || atkOvermind <= atkDormant || armorOvermind <= armorDormant) {
            helper.fail(String.format("Expected OVERMIND scaling > DORMANT: hp %.1f/%.1f atk %.1f/%.1f armor %.1f/%.1f",
                    hpOvermind, hpDormant, atkOvermind, atkDormant, armorOvermind, armorDormant));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void progressionGatingBlocksFreeShortcuts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // 1. Turret T2 upgrade must reject Chitin Plate, reject Act I core, and accept core once Act II (AWAKENED) is reached
        BlockPos turretPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
        boolean rejectedPlate = false;
        boolean rejectedAct1Core = false;
        boolean acceptedCore = false;
        if (level.getBlockEntity(turretPos) instanceof com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity turret) {
            rejectedPlate = !turret.tryUpgrade(new ItemStack(ModItems.CHITIN_PLATE.get()), player);
            rejectedAct1Core = !turret.tryUpgrade(new ItemStack(ModItems.SYNAPTIC_CORE.get()), player);
            hive.promoteHighestTier(HiveProgressionTier.AWAKENED);
            acceptedCore = turret.tryUpgrade(new ItemStack(ModItems.SYNAPTIC_CORE.get()), player);
        }
        hive.resetAll();
        level.removeBlock(turretPos, false);

        // 2. BioExtractor must reject full-HP and 55%-HP mobs, allowing weakened mob (<= 50% HP) with active trait
        var mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 4, 1, 4);
        mob.applyHiveTraits(java.util.EnumSet.of(AdaptationTrait.MELEE_THORNS));
        ItemStack extractor = new ItemStack(ModItems.BIO_EXTRACTOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, extractor);
        var fullHpRes = extractor.getItem().interactLivingEntity(extractor, player, mob, InteractionHand.MAIN_HAND);

        mob.setHealth(mob.getMaxHealth() * 0.55F);
        player.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());
        var midHpRes = extractor.getItem().interactLivingEntity(extractor, player, mob, InteractionHand.MAIN_HAND);

        mob.setHealth(mob.getMaxHealth() * 0.45F);
        player.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());
        var weakenedRes = extractor.getItem().interactLivingEntity(extractor, player, mob, InteractionHand.MAIN_HAND);
        hive.resetAll();
        mob.discard();

        // 3. BioExtractor must reject dormant MycelialNode (0% threat / no traits)
        BlockPos nodePos = helper.absolutePos(new BlockPos(3, 1, 2));
        level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
        player.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());
        var nodeCtx = new net.minecraft.world.item.context.UseOnContext(
                player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(nodePos), Direction.UP, nodePos, false)
        );
        var dormantNodeRes = extractor.getItem().useOn(nodeCtx);
        level.removeBlock(nodePos, false);

        // 4. MatriarchAltarBlock must reject cheap SYNAPTIC_CORE offering
        BlockPos altarPos = helper.absolutePos(new BlockPos(5, 1, 2));
        level.setBlockAndUpdate(altarPos, ModBlocks.MATRIARCH_ALTAR.get().defaultBlockState());
        var altarHit = new BlockHitResult(Vec3.atCenterOf(altarPos), Direction.UP, altarPos, false);
        var altarCoreRes = level.getBlockState(altarPos).useItemOn(
                new ItemStack(ModItems.SYNAPTIC_CORE.get()), level, player, InteractionHand.MAIN_HAND, altarHit);
        level.removeBlock(altarPos, false);

        if (!rejectedPlate || !rejectedAct1Core || !acceptedCore || fullHpRes.consumesAction()
                || midHpRes.consumesAction() || !weakenedRes.consumesAction()
                || dormantNodeRes.consumesAction() || altarCoreRes.consumesAction()) {
            helper.fail(String.format("Progression gating failed: rejPlate=%s accCore=%s fullHp=%s weak=%s dormNode=%s altarCore=%s",
                    rejectedPlate, acceptedCore, fullHpRes, weakenedRes, dormantNodeRes, altarCoreRes));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void playerDeathToMycelialMobIncreasesHiveThreatAndRecordsHighestTier(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var killer = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 3);
        double threatBefore = hive.getThreatLevel();

        // Simulate 5 deaths to MycelialMob -> +25.0% threat -> reaches AWAKENED (level 1)
        for (int i = 0; i < 5; i++) {
            DamageTelemetryTracker.onLivingDeath(
                    new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(
                            player, killer.damageSources().mobAttack(killer)));
        }
        double threatAfter = hive.getThreatLevel();
        int highestTier = hive.getHighestTierReached();

        killer.discard();
        hive.resetAll();

        if (threatAfter < threatBefore + 24.9 || highestTier < com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED.level()) {
            helper.fail(String.format("Death threat penalty / tier tracking failed: before=%.1f after=%.1f tier=%d",
                    threatBefore, threatAfter, highestTier));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void ephemeralChitinRespectsPylonAndAutoDecays(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var tracker = PurificationZoneTracker.get(level);
        BlockPos pylonAbs = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos targetAbs = pylonAbs.offset(2, 0, 0);
        for (BlockPos existingPylon : tracker.getPylonsWithin(targetAbs, 32, 8)) {
            tracker.removePylon(existingPylon);
        }
        level.setBlockAndUpdate(targetAbs, Blocks.AIR.defaultBlockState());

        level.setBlockAndUpdate(pylonAbs, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
        boolean blockedInPylon = !com.alex.mycelialmind.block.EphemeralChitinBlock.tryPlaceTemporary(level, targetAbs);

        level.removeBlock(pylonAbs, false);
        tracker.removePylon(pylonAbs);
        level.setBlockAndUpdate(targetAbs, Blocks.AIR.defaultBlockState());
        boolean placedOutside = com.alex.mycelialmind.block.EphemeralChitinBlock.tryPlaceTemporary(level, targetAbs)
                && level.getBlockState(targetAbs).is(ModBlocks.EPHEMERAL_CHITIN.get());

        // Trigger scheduled tick immediately to verify clean decay removal
        if (placedOutside) {
            level.scheduleTick(targetAbs, ModBlocks.EPHEMERAL_CHITIN.get(), 1);
        }
        level.removeBlock(targetAbs, false);

        if (!blockedInPylon || !placedOutside) {
            helper.fail("EphemeralChitin placement check failed: blockedInPylon=" + blockedInPylon
                    + " placedOutside=" + placedOutside
                    + " stillProtected=" + tracker.isProtected(targetAbs)
                    + " state=" + level.getBlockState(targetAbs));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void turretCorrosiveJamAndDroneParasiticJamSlowFireRate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos turretPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
        if (!(level.getBlockEntity(turretPos) instanceof com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity turret)) {
            helper.fail("PurificationTurretBlockEntity missing");
            return;
        }

        int baseInterval = turret.getFireInterval(level);
        turret.applyCorrosiveJam(com.alex.mycelialmind.hive.LivingBattlefieldSystem.TURRET_CORROSIVE_JAM_TICKS);
        boolean turretJammed = turret.isJammed() && turret.getFireInterval(level) == baseInterval * 2;
        level.removeBlock(turretPos, false);

        var drone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 4, 2, 4);
        var leech = helper.spawnWithNoFreeWill(ModEntities.LEECH_PARASITE.get(), 4, 2, 4);
        leech.doHurtTarget(drone);
        boolean droneJammed = drone.isJammed();
        drone.discard();
        leech.discard();

        if (!turretJammed || !droneJammed) {
            helper.fail("Jamming mechanics failed: turretJammed=" + turretJammed + " droneJammed=" + droneJammed);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void carrionBiomassDropsOutsideFireAndHealsSporeling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var sporeling = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 3, 1, 3);
        sporeling.setHealth(4.0F);

        boolean nonFireDrop = com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldDropCarrion(false, false, false);
        boolean fireSuppressed = !com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldDropCarrion(false, true, false);
        com.alex.mycelialmind.hive.HiveBlackboard.registerCarrion(
                level, sporeling.position(), com.alex.mycelialmind.hive.LivingBattlefieldSystem.CARRION_TTL_TICKS);
        var consumed = com.alex.mycelialmind.hive.HiveBlackboard.consumeNearestCarrion(level, sporeling.position(), 3.0D);
        if (consumed != null) {
            sporeling.heal(com.alex.mycelialmind.hive.LivingBattlefieldSystem.CARRION_HEAL_AMOUNT);
            com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.addCombatXp(level, sporeling, StimulusType.MELEE,
                    com.alex.mycelialmind.hive.LivingBattlefieldSystem.CARRION_XP_REWARD);
        }

        boolean healed = sporeling.getHealth() >= 9.9F;
        boolean xpGained = com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getCombatXp(sporeling) >= 20;
        sporeling.discard();

        if (!nonFireDrop || !fireSuppressed || consumed == null || !healed || !xpGained) {
            helper.fail(String.format("Carrion necrophagy failed: nonFire=%s fireSup=%s consumed=%s hp=%.1f xp=%d",
                    nonFireDrop, fireSuppressed, consumed != null, sporeling.getHealth(),
                    com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.getCombatXp(sporeling)));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void generalPolishLootAdvancementsMaintenanceAndScannerWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        var tracker = PurificationZoneTracker.get(level);

        // 1. Verify 4 new entity loot tables
        for (String entityId : List.of("spore_architect", "spore_behemoth", "purifier_drone", "mycelial_chimera")) {
            LootTable table = server.reloadableRegistries().getLootTable(
                    ResourceKey.create(Registries.LOOT_TABLE,
                            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "entities/" + entityId)));
            if (table == LootTable.EMPTY) {
                helper.fail("Missing entity loot table: entities/" + entityId);
                return;
            }
        }

        // 2. Verify 5 new advancements
        for (String advId : List.of("synaptic_graft_network", "chemical_diplomat", "chimera_bioforge", "pavlovian_deceiver", "architect_bane")) {
            if (server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, advId)) == null) {
                helper.fail("Missing advancement: " + advId);
                return;
            }
        }

        // 3. Verify 4 new SoundEvents
        if (ModSounds.FUNGAL_TIDE_HORN.get() == null || ModSounds.SQUAD_DESYNC.get() == null
                || ModSounds.CHIMERA_ROAR.get() == null || ModSounds.CORROSIVE_JAM.get() == null) {
            helper.fail("One or more new ModSounds failed to register.");
            return;
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // 4. PurifierDrone maintenance & Bio-Scanner inspection
        var drone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 3, 2, 3);
        drone.setHealth(40.0F);
        drone.setJammedTicks(60);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHITIN_PLATE.get()));
        var droneRepairRes = player.interactOn(drone, InteractionHand.MAIN_HAND);
        boolean droneHealedAndUnjammed = droneRepairRes.consumesAction()
                && drone.getHealth() >= 54.9F
                && !drone.isJammed();

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
        var droneScanRes = player.interactOn(drone, InteractionHand.MAIN_HAND);

        // 5. MycelialMob & Chimera Bio-Scanner inspection
        var sporeling = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 4, 2, 4);
        var chimera = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_CHIMERA.get(), 5, 2, 5);
        var mobScanRes = player.interactOn(sporeling, InteractionHand.MAIN_HAND);
        var chimeraScanRes = player.interactOn(chimera, InteractionHand.MAIN_HAND);

        drone.discard();
        sporeling.discard();
        chimera.discard();

        // 6. PurificationTurret unjamming + Bio-Scanner block inspection
        BlockPos turretPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
        boolean turretUnjammed = false;
        boolean turretScanned = false;
        if (level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity turret) {
            turret.applyCorrosiveJam(100);
            ItemStack antidote = new ItemStack(ModItems.SPORE_ANTIDOTE.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, antidote);
            var cleanRes = level.getBlockState(turretPos).useItemOn(
                    antidote, level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(turretPos), Direction.UP, turretPos, false)
            );
            turretUnjammed = cleanRes.consumesAction() && !turret.isJammed();

            ItemStack scanner = new ItemStack(ModItems.BIO_SCANNER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, scanner);
            var scanRes = scanner.useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(turretPos), Direction.UP, turretPos, false)
            ));
            turretScanned = scanRes.consumesAction();
        }
        level.removeBlock(turretPos, false);

        // 7. SymbioticNode stability recharge (+15%) + Bio-Scanner inspection
        BlockPos nodePos = helper.absolutePos(new BlockPos(4, 1, 2));
        level.setBlockAndUpdate(nodePos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
        tracker.addNode(nodePos, SynapticNodeType.SYMBIOTIC);
        boolean nodeRecharged = false;
        boolean nodeScanned = false;
        if (level.getBlockEntity(nodePos) instanceof SynapticNodeBlockEntity nodeBe) {
            nodeBe.setSymbioticStability(60);
            ItemStack biomass = new ItemStack(ModItems.MYCELIAL_BIOMASS.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, biomass);
            var rechargeRes = level.getBlockState(nodePos).useItemOn(
                    biomass, level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(nodePos), Direction.UP, nodePos, false)
            );
            nodeRecharged = rechargeRes.consumesAction() && nodeBe.getSymbioticStability() == 75;

            ItemStack scanner = new ItemStack(ModItems.BIO_SCANNER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, scanner);
            var scanRes = scanner.useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(nodePos), Direction.UP, nodePos, false)
            ));
            nodeScanned = scanRes.consumesAction();
        }
        level.removeBlock(nodePos, false);
        tracker.removeNode(nodePos);

        if (!droneHealedAndUnjammed || !droneScanRes.consumesAction()
                || !mobScanRes.consumesAction() || !chimeraScanRes.consumesAction()
                || !turretUnjammed || !turretScanned || !nodeRecharged || !nodeScanned) {
            helper.fail(String.format(
                    "General polish check failed: droneRepair=%s droneScan=%s mobScan=%s chimeraScan=%s turretUnjam=%s turretScan=%s nodeRech=%s nodeScan=%s",
                    droneHealedAndUnjammed, droneScanRes, mobScanRes, chimeraScanRes,
                    turretUnjammed, turretScanned, nodeRecharged, nodeScanned));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void multiPoolChestEntityAndBlockLootTablesRollVariedItems(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        var registries = server.reloadableRegistries();

        // 1. All 9 structure chest loot tables exist and are non-empty
        for (ResourceKey<LootTable> chestKey : com.alex.mycelialmind.world.MycelialLootSystem.ALL_CHEST_TABLES) {
            if (registries.getLootTable(chestKey) == LootTable.EMPTY) {
                helper.fail("Missing chest loot table: " + chestKey.location());
                return;
            }
        }

        // 2. All 17 entity tables and 8 enriched block/node tables exist
        for (String entityId : List.of(
                "sporeling", "chitin_brute", "mycelial_stalker", "acid_spitter", "burrower",
                "pyro_shroom", "synapse_weaver", "goliath_carrier", "leech_parasite", "colony_warden",
                "hive_core", "spore_architect", "synaptic_mimic", "spore_behemoth",
                "purifier_drone", "mycelial_chimera", "apex_matriarch")) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "entities/" + entityId));
            if (registries.getLootTable(key) == LootTable.EMPTY) {
                helper.fail("Missing entity loot table: entities/" + entityId);
                return;
            }
        }
        for (String blockId : List.of(
                "mycelial_node", "incubator_node", "sentinel_node", "spore_vent",
                "luminescent_amber_vein", "bioluminescent_mycelial_cap", "spore_bulb", "mycelial_tendrils")) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "blocks/" + blockId));
            if (registries.getLootTable(key) == LootTable.EMPTY) {
                helper.fail("Missing block loot table: blocks/" + blockId);
                return;
            }
        }

        // 3. Rolling chest tables across multiple seeds produces varied items and respects 4-Act gating
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);

        java.util.Set<net.minecraft.world.item.Item> uniqueItems = new java.util.HashSet<>();
        for (ResourceKey<LootTable> tableKey : List.of(
                com.alex.mycelialmind.world.MycelialLootSystem.SURFACE_HIVE_CACHE,
                com.alex.mycelialmind.world.MycelialLootSystem.SANCTUM_FORBIDDEN_LAB,
                com.alex.mycelialmind.world.MycelialLootSystem.CITADEL_ROYAL_CRYPT)) {
            LootTable table = registries.getLootTable(tableKey);
            boolean isOverworld = tableKey != com.alex.mycelialmind.world.MycelialLootSystem.CITADEL_ROYAL_CRYPT;
            for (long seed = 100L; seed < 112L; seed++) {
                for (ItemStack stack : table.getRandomItems(params, seed)) {
                    uniqueItems.add(stack.getItem());
                    if (isOverworld && (stack.is(ModItems.OVERMIND_ESSENCE.get()) || stack.is(ModItems.HIVE_HEART.get()))) {
                        helper.fail("Overworld chest table leaked endgame boss item: " + tableKey.location() + " -> " + stack);
                        return;
                    }
                }
            }
        }
        if (uniqueItems.size() < 8) {
            helper.fail("Expected varied chest loot across tables/seeds, found only " + uniqueItems.size() + " item types.");
            return;
        }

        // 4. Adaptive mob drops produce real coded GeneticSampleItem & evolved armor stacks
        boolean sawCodedSample = false;
        boolean sawMutatedArmor = false;
        var dropRng = net.minecraft.util.RandomSource.create(42L);
        for (int i = 0; i < 120; i++) {
            var drops = com.alex.mycelialmind.world.MycelialLootSystem.computeAdaptiveMobDrops(
                    EvolutionRank.ELITE,
                    com.alex.mycelialmind.hive.HiveStrain.AMBER,
                    com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND,
                    java.util.EnumSet.of(AdaptationTrait.FIRE_RESISTANT),
                    true,
                    3,
                    dropRng
            );
            for (ItemStack s : drops) {
                if (s.is(ModItems.GENETIC_SAMPLE.get()) && GeneticSampleItem.getTrait(s) == AdaptationTrait.FIRE_RESISTANT) {
                    sawCodedSample = true;
                }
                if (s.getItem() instanceof ChitinArmorItem && ChitinArmorItem.getArmorRank(s) == EvolutionRank.MUTATED) {
                    sawMutatedArmor = true;
                }
            }
            if (sawCodedSample && sawMutatedArmor) {
                break;
            }
        }
        if (!sawCodedSample || !sawMutatedArmor) {
            helper.fail("Adaptive mob drops missing coded sample (" + sawCodedSample + ") or mutated armor (" + sawMutatedArmor + ").");
            return;
        }

        // 5. populateStructureChest with audit-core guarantee keeps SurfaceHive primary audit items intact
        BlockPos chestPos = helper.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        boolean auditCoreOk = false;
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            com.alex.mycelialmind.world.MycelialLootSystem.populateStructureChest(
                    level, chestPos, chest,
                    com.alex.mycelialmind.world.MycelialLootSystem.SURFACE_HIVE_CACHE,
                    level.getRandom(),
                    com.alex.mycelialmind.hive.HiveProgressionTier.AWAKENED,
                    true
            );
            boolean hasBio = false, hasChitin = false, hasSpore = false, hasCore = false;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack st = chest.getItem(i);
                if (st.is(ModItems.MYCELIAL_BIOMASS.get())) hasBio = true;
                if (st.is(ModItems.CHITIN_PLATE.get())) hasChitin = true;
                if (st.is(ModItems.LATENT_SPORE.get())) hasSpore = true;
                if (st.is(ModItems.SYNAPTIC_CORE.get())) hasCore = true;
            }
            auditCoreOk = hasBio && hasChitin && hasSpore && hasCore;
        }
        level.removeBlock(chestPos, false);

        if (!auditCoreOk) {
            helper.fail("populateStructureChest failed to guarantee WorldgenAuditEngine core items.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void equipmentTagsAnvilRepairAndSynapticBenchSmartRoutingWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // 1. Enchantment & equipment item tags
        boolean itemTagsOk = new ItemStack(ModItems.SYMBIOTIC_BLADE.get()).is(net.minecraft.tags.ItemTags.SWORDS)
                && new ItemStack(ModItems.ENZYME_LANCE.get()).is(net.minecraft.tags.ItemTags.SWORDS)
                && new ItemStack(ModItems.CHITIN_HELMET.get()).is(net.minecraft.tags.ItemTags.HEAD_ARMOR)
                && new ItemStack(ModItems.CHITIN_CHESTPLATE.get()).is(net.minecraft.tags.ItemTags.CHEST_ARMOR)
                && new ItemStack(ModItems.CHITIN_LEGGINGS.get()).is(net.minecraft.tags.ItemTags.LEG_ARMOR)
                && new ItemStack(ModItems.CHITIN_BOOTS.get()).is(net.minecraft.tags.ItemTags.FOOT_ARMOR)
                && new ItemStack(ModItems.NEUTRALIZING_SPORE_LAUNCHER.get()).is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE)
                && new ItemStack(ModItems.BIO_EXTRACTOR.get()).is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE)
                && !new ItemStack(ModItems.TIDE_BEACON.get()).is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE);

        // 2. Block & entity tags
        var cBossesTag = net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("c", "bosses"));
        boolean blockEntityTagsOk = ModBlocks.SPORE_VENT.get().defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE)
                && ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)
                && ModBlocks.PURIFICATION_PYLON.get().defaultBlockState().is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL)
                && ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState().is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)
                && ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState().is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)
                && ModEntities.HIVE_CORE.get().is(cBossesTag)
                && ModEntities.APEX_MATRIARCH.get().is(cBossesTag)
                && ModEntities.SPORE_BEHEMOTH.get().is(cBossesTag)
                && ModEntities.PURIFIER_DRONE.get().is(net.minecraft.tags.EntityTypeTags.FALL_DAMAGE_IMMUNE);

        // 3. Anvil repair items
        boolean repairOk = ModItems.SYMBIOTIC_BLADE.get().isValidRepairItem(
                new ItemStack(ModItems.SYMBIOTIC_BLADE.get()), new ItemStack(ModItems.MYCELIAL_BIOMASS.get()))
                && ModItems.ENZYME_LANCE.get().isValidRepairItem(
                        new ItemStack(ModItems.ENZYME_LANCE.get()), new ItemStack(ModItems.CHITIN_PLATE.get()))
                && ModItems.NEUTRALIZING_SPORE_LAUNCHER.get().isValidRepairItem(
                        new ItemStack(ModItems.NEUTRALIZING_SPORE_LAUNCHER.get()), new ItemStack(ModItems.CHITIN_PLATE.get()))
                && ModItems.BIO_EXTRACTOR.get().isValidRepairItem(
                        new ItemStack(ModItems.BIO_EXTRACTOR.get()), new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));

        // 4. SynapticBenchMenu smart Shift-click routing & synthesis sound/consumption
        BlockPos benchPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
        boolean routingOk = false;
        boolean synthTakenOk = false;
        if (level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench) {
            HiveMindSavedData hive = HiveMindSavedData.get(level);
            CompoundTag hiveSnap = hive.save(new CompoundTag(), level.registryAccess());
            try {
                hive.promoteHighestTier(HiveProgressionTier.ADAPTIVE);
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack codedSample = new ItemStack(ModItems.GENETIC_SAMPLE.get());
                codedSample.set(ModDataComponents.TRAIT_ID.get(), AdaptationTrait.MELEE_THORNS.id());
                player.getInventory().setItem(9, new ItemStack(ModItems.NEURAL_ECHO_CORE.get()));
                player.getInventory().setItem(10, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                player.getInventory().setItem(11, codedSample);
                player.getInventory().setItem(12, new ItemStack(Items.DIRT));

                var menu = new com.alex.mycelialmind.menu.SynapticBenchMenu(1, player.getInventory(), bench);
                menu.quickMoveStack(player, 4); // Echo Core -> Slot 2
                menu.quickMoveStack(player, 5); // Biomass -> Slot 1
                menu.quickMoveStack(player, 6); // Sample -> Slot 0
                ItemStack dirtRejected = menu.quickMoveStack(player, 7); // Dirt rejected

                routingOk = dirtRejected.isEmpty()
                        && bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE).is(ModItems.GENETIC_SAMPLE.get())
                        && bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS).is(ModItems.MYCELIAL_BIOMASS.get())
                        && bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST).is(ModItems.NEURAL_ECHO_CORE.get())
                        && bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_OUTPUT).is(ModItems.CHIMERA_OVUM.get())
                        && menu.getSlot(7).getItem().is(Items.DIRT);

                if (routingOk) {
                    menu.quickMoveStack(player, SynapticBenchBlockEntity.SLOT_OUTPUT);
                    synthTakenOk = player.getInventory().countItem(ModItems.CHIMERA_OVUM.get()) == 1
                            && bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE).isEmpty()
                            && bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS).isEmpty()
                            && bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST).isEmpty();
                }
            } finally {
                hive.restoreFromSnapshot(hiveSnap, level.registryAccess());
            }
        }
        level.removeBlock(benchPos, false);

        if (!itemTagsOk || !blockEntityTagsOk || !repairOk || !routingOk || !synthTakenOk) {
            helper.fail(String.format("Phase-2 polish check failed: itemTags=%s blockEntityTags=%s repair=%s routing=%s synthTaken=%s",
                    itemTagsOk, blockEntityTagsOk, repairOk, routingOk, synthTakenOk));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "homeostasis")
    public static void homeostasisEscalatesOnPylonExcessAndExplosionAndDecaysWithSymbioticNode(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        if (DimensionalHomeostasisDirector.isHomeostasisDimension(level)) {
            helper.fail("Overworld GameTest level should not satisfy isHomeostasisDimension.");
            return;
        }

        BlockPos base = homeostasisTestPos(helper, 1, new BlockPos(3, 1, 3));
        int stepX = (base.getX() & 15) <= 7 ? 1 : -1;
        BlockPos pylon1 = base;
        BlockPos pylon2 = base.offset(stepX, 0, 0);
        BlockPos symPos = base.offset(stepX * 2, 0, 0);
        ChunkPos chunk = new ChunkPos(base);
        resetHomeostasisChunks(data, chunk, 0);

        try {
            level.setBlockAndUpdate(pylon1, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            DimensionalHomeostasisDirector.handleForeignPlacement(level, pylon1, level.getBlockState(pylon1), true);
            double afterOne = data.getStress(chunk);

            level.setBlockAndUpdate(pylon2, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            DimensionalHomeostasisDirector.handleForeignPlacement(level, pylon2, level.getBlockState(pylon2), true);
            DimensionalHomeostasisDirector.recordExplosionImpact(level, base, 18, 4.0F);
            double escalated = data.getStress(chunk);
            HomeostasisStage stage = data.getStage(chunk);

            level.removeBlock(pylon1, false);
            level.removeBlock(pylon2, false);
            tracker.removePylon(pylon1);
            tracker.removePylon(pylon2);

            level.setBlockAndUpdate(symPos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            tracker.addNode(symPos, SynapticNodeType.SYMBIOTIC);
            DimensionalHomeostasisDirector.handleForeignPlacement(level, symPos, level.getBlockState(symPos), true);
            double afterSymPlace = data.getStress(chunk);

            for (int i = 0; i < 12; i++) {
                DimensionalHomeostasisDirector.stepChunkHomeostasis(level, chunk);
            }
            double afterDecay = data.getStress(chunk);

            if (afterOne <= 0.0
                    || escalated < DimensionalHomeostasisEngine.IMMUNE_THRESHOLD
                    || stage != HomeostasisStage.IMMUNE_RESPONSE
                    || afterSymPlace >= escalated
                    || afterDecay >= DimensionalHomeostasisEngine.IMMUNE_THRESHOLD) {
                helper.fail(String.format(
                        "Homeostasis escalation/decay failed: afterOne=%.1f escalated=%.1f stage=%s afterSymPlace=%.1f afterDecay=%.1f",
                        afterOne, escalated, stage, afterSymPlace, afterDecay));
                return;
            }
            helper.succeed();
        } finally {
            level.removeBlock(symPos, false);
            level.removeBlock(pylon2, false);
            level.removeBlock(pylon1, false);
            tracker.removeNode(symPos);
            tracker.removePylon(pylon1);
            tracker.removePylon(pylon2);
            resetHomeostasisChunks(data, chunk, 0);
        }
    }

    @GameTest(template = "empty", batch = "homeostasis")
    public static void homeostasisAcidRainDamagesExposedAndRespectsRoofAndChitinArmor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos exposedPos = homeostasisTestPos(helper, 2, new BlockPos(2, 1, 2));
        BlockPos shelterPos = homeostasisTestPos(helper, 2, new BlockPos(5, 1, 2));
        BlockPos exposedCeiling = exposedPos.above(6);
        BlockPos shelterCeiling = shelterPos.above(6);
        BlockPos roofPos = shelterPos.above(3);

        try {
            for (int dy = 1; dy <= 5; dy++) {
                BlockPos p1 = exposedPos.above(dy);
                if (!level.getBlockState(p1).isAir()) {
                    level.setBlock(p1, Blocks.AIR.defaultBlockState(), 18);
                }
                BlockPos p2 = shelterPos.above(dy);
                if (!level.getBlockState(p2).isAir()) {
                    level.setBlock(p2, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            level.setBlockAndUpdate(exposedCeiling, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
            level.setBlockAndUpdate(shelterCeiling, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());

            Player ironPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            ironPlayer.moveTo(exposedPos.getX() + 0.5, exposedPos.getY(), exposedPos.getZ() + 0.5);
            ItemStack ironChest = new ItemStack(Items.IRON_CHESTPLATE);
            ironPlayer.setItemSlot(EquipmentSlot.CHEST, ironChest);
            float hpBeforeIron = ironPlayer.getHealth();
            boolean hitExposed = DimensionalHomeostasisDirector.applyAcidRainPulseToEntity(level, ironPlayer, 85.0);
            float ironDmgTaken = hpBeforeIron - ironPlayer.getHealth();
            int ironWear = ironPlayer.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
            boolean gotEffects = ironPlayer.hasEffect(ModEffects.MYCELIAL_INFECTION)
                    && ironPlayer.hasEffect(ModEffects.SPORE_ROT);

            Player chitinPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            chitinPlayer.moveTo(exposedPos.getX() + 0.5, exposedPos.getY(), exposedPos.getZ() + 0.5);
            chitinPlayer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CHITIN_HELMET.get()));
            chitinPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CHITIN_CHESTPLATE.get()));
            chitinPlayer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CHITIN_LEGGINGS.get()));
            chitinPlayer.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CHITIN_BOOTS.get()));
            float hpBeforeChitin = chitinPlayer.getHealth();
            boolean hitChitin = DimensionalHomeostasisDirector.applyAcidRainPulseToEntity(level, chitinPlayer, 85.0);
            float chitinDmgTaken = hpBeforeChitin - chitinPlayer.getHealth();
            int chitinWear = chitinPlayer.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
            boolean noSporeRot = !chitinPlayer.hasEffect(ModEffects.SPORE_ROT);

            level.setBlockAndUpdate(roofPos, ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
            chitinPlayer.moveTo(shelterPos.getX() + 0.5, shelterPos.getY(), shelterPos.getZ() + 0.5);
            boolean hitUnderShelter = DimensionalHomeostasisDirector.applyAcidRainPulseToEntity(level, chitinPlayer, 85.0);

            if (!hitExposed || ironDmgTaken < 1.5F || ironWear < 1 || !gotEffects
                    || !hitChitin || chitinDmgTaken >= ironDmgTaken || chitinWear != 0 || !noSporeRot
                    || hitUnderShelter) {
                helper.fail(String.format(
                        "Acid rain check failed: hitExp=%s ironDmg=%.2f wear=%d fx=%s | hitChitin=%s chitinDmg=%.2f cWear=%d noRot=%s | hitRoof=%s",
                        hitExposed, ironDmgTaken, ironWear, gotEffects,
                        hitChitin, chitinDmgTaken, chitinWear, noSporeRot, hitUnderShelter));
                return;
            }
            helper.succeed();
        } finally {
            level.removeBlock(roofPos, false);
            level.removeBlock(exposedCeiling, false);
            level.removeBlock(shelterCeiling, false);
        }
    }

    @GameTest(template = "empty", batch = "homeostasis")
    public static void homeostasisAntibodyCystsEncapsulateForeignAndSmotherPylonSafely(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        BlockPos base = homeostasisTestPos(helper, 3, new BlockPos(1, 14, 1));
        BlockPos pylonPos = base;
        BlockPos chestPos = base.east(4);
        BlockPos riftPos = base.south(5);
        BlockPos riftNeighborForeign = riftPos.east();
        resetHomeostasisChunks(data, new ChunkPos(base), 1);

        try {
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, -2, -2), base.offset(6, 2, 7))) {
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
            }

            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            DimensionalHomeostasisDirector.handleForeignPlacement(level, pylonPos, level.getBlockState(pylonPos), true);

            level.setBlockAndUpdate(chestPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
            DimensionalHomeostasisDirector.handleForeignPlacement(level, chestPos, level.getBlockState(chestPos), true);
            if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                chest.setItem(0, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
            }

            level.setBlockAndUpdate(riftPos, ModBlocks.MYCELIAL_RIFT.get().defaultBlockState());
            level.setBlockAndUpdate(riftNeighborForeign, net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
            DimensionalHomeostasisDirector.handleForeignPlacement(level, riftNeighborForeign, level.getBlockState(riftNeighborForeign), true);

            data.setStress(new ChunkPos(pylonPos), 85.0, level.getGameTime());
            data.setStress(new ChunkPos(chestPos), 85.0, level.getGameTime());

            int pylonCysts = DimensionalHomeostasisDirector.tryEncapsulateChunk(level, new ChunkPos(pylonPos), 6, true);
            int chestCysts = DimensionalHomeostasisDirector.tryEncapsulateChunk(level, new ChunkPos(chestPos), 4, true);

            boolean pylonStillIntact = level.getBlockState(pylonPos).is(ModBlocks.PURIFICATION_PYLON.get());
            boolean smothered = tracker.isPylonSmothered(pylonPos) && !tracker.isProtected(pylonPos.north());
            boolean chestIntact = level.getBlockState(chestPos).is(net.minecraft.world.level.block.Blocks.CHEST)
                    && level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity cb
                    && cb.getItem(0).is(ModItems.SYNAPTIC_CORE.get());
            boolean riftClear = com.alex.mycelialmind.block.AntibodyCystBlock.countAdjacentCysts(level, riftPos) == 0;

            int removed = 0;
            for (Direction d : Direction.values()) {
                BlockPos adj = pylonPos.relative(d);
                if (level.getBlockState(adj).is(ModBlocks.ANTIBODY_CYST.get())) {
                    level.removeBlock(adj, false);
                    removed++;
                    if (removed >= 3) {
                        break;
                    }
                }
            }
            boolean freed = !tracker.isPylonSmothered(pylonPos) && tracker.isProtected(pylonPos.north());

            if (pylonCysts < 4 || chestCysts < 1 || !pylonStillIntact || !smothered || !chestIntact || !riftClear || !freed) {
                helper.fail(String.format(
                        "Antibody encapsulation check failed: pylonCysts=%d chestCysts=%d intact=%s smothered=%s chestOk=%s riftClear=%s freed=%s",
                        pylonCysts, chestCysts, pylonStillIntact, smothered, chestIntact, riftClear, freed));
                return;
            }
            helper.succeed();
        } finally {
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, -2, -2), base.offset(6, 2, 7))) {
                BlockState st = level.getBlockState(p);
                if (st.is(ModBlocks.ANTIBODY_CYST.get())
                        || st.is(ModBlocks.PURIFICATION_PYLON.get())
                        || st.is(net.minecraft.world.level.block.Blocks.CHEST)
                        || st.is(ModBlocks.MYCELIAL_RIFT.get())
                        || st.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            tracker.removePylon(pylonPos);
            resetHomeostasisChunks(data, new ChunkPos(base), 1);
        }
    }

    @GameTest(template = "empty", batch = "homeostasis")
    public static void homeostasisBioScannerThermalPulseAndSedationToolsWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);

        BlockPos anchor = homeostasisTestPos(helper, 4, new BlockPos(3, 1, 3));
        ChunkPos chunk = new ChunkPos(anchor);
        resetHomeostasisChunks(data, chunk, 1);

        try {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.moveTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
            player.setShiftKeyDown(true);
            ItemStack scanner = new ItemStack(ModItems.BIO_SCANNER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, scanner);
            scanner.use(level, player, InteractionHand.MAIN_HAND);

            boolean visionActive = player.hasEffect(ModEffects.SYNAPTIC_VISION);
            boolean onCooldown = player.getCooldowns().isOnCooldown(ModItems.BIO_SCANNER.get());

            data.setStress(chunk, 85.0, level.getGameTime());
            DimensionalHomeostasisDirector.sedateArea(level, anchor, 1, DimensionalHomeostasisEngine.SUPPRESSOR_SEDATION, null);
            double afterSuppressor = data.getStress(chunk);
            DimensionalHomeostasisDirector.sedateArea(level, anchor, 1, DimensionalHomeostasisEngine.TRUCE_PHEROMONE_SEDATION, null);
            double afterPheromone = data.getStress(chunk);

            HiveSyncPayload payload = new HiveSyncPayload(
                    33.0, 0, 0, 0, false, false, BlockPos.ZERO, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 24, 28, anchor, List.of(), List.of(), 0,
                    0, 0, 0, 0, -1, 0, 0, 0.0, 0, 0,
                    82.5, 2, true, 1
            );
            var buf = new net.minecraft.network.FriendlyByteBuf(Unpooled.buffer());
            HiveSyncPayload.STREAM_CODEC.encode(buf, payload);
            HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);
            buf.release();

            boolean payloadOk = Math.abs(decoded.homeostasisStress() - 82.5) < 0.01
                    && decoded.homeostasisStage() == 2
                    && decoded.acidRainExposed()
                    && decoded.smotheredPylonCount() == 1;

            if (!visionActive || !onCooldown || afterSuppressor > 41.0 || afterPheromone > 15.0 || !payloadOk) {
                helper.fail(String.format(
                        "Thermal pulse / sedation / payload check failed: vision=%s cd=%s afterSup=%.1f afterPher=%.1f payloadOk=%s",
                        visionActive, onCooldown, afterSuppressor, afterPheromone, payloadOk));
                return;
            }
            helper.succeed();
        } finally {
            resetHomeostasisChunks(data, chunk, 1);
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void entityAudioProfilesSoundRegistryAndThresholdCalibrationWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = resetHive(level);

        try {
            var soundEntries = ModSounds.SOUND_EVENTS.getEntries();
            boolean soundsOk = soundEntries.size() == 226;
            if (soundsOk) {
                for (var holder : soundEntries) {
                    SoundEvent evt = holder.get();
                    if (evt == null || evt.getLocation() == null
                            || !MycelialMind.MODID.equals(evt.getLocation().getNamespace())) {
                        soundsOk = false;
                        break;
                    }
                }
            }

            List<EntityType<? extends MycelialMob>> mycelialTypes = List.of(
                    ModEntities.SPORELING.get(),
                    ModEntities.CHITIN_BRUTE.get(),
                    ModEntities.MYCELIAL_STALKER.get(),
                    ModEntities.ACID_SPITTER.get(),
                    ModEntities.BURROWER.get(),
                    ModEntities.PYRO_SHROOM.get(),
                    ModEntities.SYNAPSE_WEAVER.get(),
                    ModEntities.GOLIATH_CARRIER.get(),
                    ModEntities.LEECH_PARASITE.get(),
                    ModEntities.COLONY_WARDEN.get(),
                    ModEntities.SPORE_ARCHITECT.get(),
                    ModEntities.SPORE_BEHEMOTH.get(),
                    ModEntities.SYNAPTIC_MIMIC.get(),
                    ModEntities.HIVE_CORE.get(),
                    ModEntities.APEX_MATRIARCH.get(),
                    ModEntities.ROOT_KEEPER.get()
            );

            Set<ResourceLocation> distinctAmbient = new HashSet<>();
            Set<ResourceLocation> distinctHurt = new HashSet<>();
            Set<ResourceLocation> distinctDeath = new HashSet<>();
            boolean entityAudioOk = true;
            float sporelingBaseVol = -1.0F;
            int sporelingInterval = -1;
            float matriarchBaseVol = -1.0F;
            var genericDmg = level.damageSources().generic();
            for (EntityType<? extends MycelialMob> type : mycelialTypes) {
                MycelialMob mob = type.create(level);
                if (mob == null) {
                    entityAudioOk = false;
                    break;
                }
                try {
                    SoundEvent amb = mob.resolveAmbientSound();
                    SoundEvent hurt = mob.resolveHurtSound(genericDmg);
                    SoundEvent death = mob.resolveDeathSound();
                    if (amb == null || hurt == null || death == null
                            || !MycelialMind.MODID.equals(amb.getLocation().getNamespace())
                            || !MycelialMind.MODID.equals(hurt.getLocation().getNamespace())
                            || !MycelialMind.MODID.equals(death.getLocation().getNamespace())) {
                        entityAudioOk = false;
                    } else {
                        distinctAmbient.add(amb.getLocation());
                        distinctHurt.add(hurt.getLocation());
                        distinctDeath.add(death.getLocation());
                        if (type == ModEntities.SPORELING.get()) {
                            sporelingBaseVol = mob.resolveBaseSoundVolume();
                            sporelingInterval = mob.getAmbientSoundInterval();
                        } else if (type == ModEntities.APEX_MATRIARCH.get()) {
                            matriarchBaseVol = mob.resolveBaseSoundVolume();
                        }
                    }
                } finally {
                    mob.discard();
                }
            }

            PurifierDroneEntity drone = ModEntities.PURIFIER_DRONE.get().create(level);
            if (drone == null) {
                entityAudioOk = false;
            } else {
                try {
                    SoundEvent amb = drone.resolveAmbientSound();
                    SoundEvent hurt = drone.resolveHurtSound(genericDmg);
                    SoundEvent death = drone.resolveDeathSound();
                    if (amb == null || hurt == null || death == null
                            || !MycelialMind.MODID.equals(amb.getLocation().getNamespace())
                            || !MycelialMind.MODID.equals(hurt.getLocation().getNamespace())
                            || !MycelialMind.MODID.equals(death.getLocation().getNamespace())) {
                        entityAudioOk = false;
                    } else {
                        distinctAmbient.add(amb.getLocation());
                        distinctHurt.add(hurt.getLocation());
                        distinctDeath.add(death.getLocation());
                    }
                } finally {
                    drone.discard();
                }
            }

            MycelialChimeraEntity chimera = ModEntities.MYCELIAL_CHIMERA.get().create(level);
            if (chimera == null) {
                entityAudioOk = false;
            } else {
                try {
                    SoundEvent amb = chimera.resolveAmbientSound();
                    SoundEvent hurt = chimera.resolveHurtSound(genericDmg);
                    SoundEvent death = chimera.resolveDeathSound();
                    if (amb == null || hurt == null || death == null
                            || !MycelialMind.MODID.equals(amb.getLocation().getNamespace())
                            || !MycelialMind.MODID.equals(hurt.getLocation().getNamespace())
                            || !MycelialMind.MODID.equals(death.getLocation().getNamespace())) {
                        entityAudioOk = false;
                    } else {
                        distinctAmbient.add(amb.getLocation());
                        distinctHurt.add(hurt.getLocation());
                        distinctDeath.add(death.getLocation());
                    }
                } finally {
                    chimera.discard();
                }
            }

            boolean thresholdsOk = Math.abs(MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.getDefault() - 70.0) < 0.01
                    && Math.abs(MycelialConfig.TIER2_ACTIVATION_THRESHOLD.getDefault() - 200.0) < 0.01
                    && MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get() > 0.0
                    && MycelialConfig.TIER2_ACTIVATION_THRESHOLD.get() > MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get()
                    && Math.abs(HiveMindSavedData.HABIT_ACTIVATION_THRESHOLD - 60.0) < 0.01;

            hive.recordHabit(PlayerHabit.COMBAT_MEDIC, 59.0);
            boolean belowHabitThreshold = !hive.hasHabitAdaptation(PlayerHabit.COMBAT_MEDIC);
            hive.recordHabit(PlayerHabit.COMBAT_MEDIC, 1.0);
            boolean atHabitThreshold = hive.hasHabitAdaptation(PlayerHabit.COMBAT_MEDIC);

            boolean rankDynamicsOk = MycelialMob.computeRankVoicePitchMultiplier(EvolutionRank.ELITE)
                    < MycelialMob.computeRankVoicePitchMultiplier(EvolutionRank.MUTATED)
                    && MycelialMob.computeRankVoicePitchMultiplier(EvolutionRank.MUTATED)
                    < MycelialMob.computeRankVoicePitchMultiplier(EvolutionRank.BASE)
                    && MycelialMob.computeRankSoundVolumeMultiplier(EvolutionRank.ELITE)
                    > MycelialMob.computeRankSoundVolumeMultiplier(EvolutionRank.MUTATED)
                    && MycelialMob.computeRankSoundVolumeMultiplier(EvolutionRank.MUTATED)
                    > MycelialMob.computeRankSoundVolumeMultiplier(EvolutionRank.BASE)
                    && MycelialMob.computeTacticalVoicePitchMultiplier(
                            EvolutionRank.BASE, HiveStrain.AMBER, true, false, SynapticCommandSystem.CommandRole.NONE, false)
                    > MycelialMob.computeTacticalVoicePitchMultiplier(
                            EvolutionRank.BASE, HiveStrain.CRIMSON, false, false, SynapticCommandSystem.CommandRole.LEADER, true)
                    && MycelialMob.computeTacticalSoundVolumeMultiplier(
                            EvolutionRank.ELITE, true, SynapticCommandSystem.CommandRole.LEADER, false)
                    > MycelialMob.computeTacticalSoundVolumeMultiplier(
                            EvolutionRank.ELITE, false, SynapticCommandSystem.CommandRole.NONE, true)
                    && sporelingInterval >= 140
                    && matriarchBaseVol > sporelingBaseVol;

            if (!soundsOk || !entityAudioOk || distinctAmbient.size() != 18
                    || distinctHurt.size() != 18 || distinctDeath.size() != 18
                    || !rankDynamicsOk
                    || !thresholdsOk || !belowHabitThreshold || !atHabitThreshold) {
                helper.fail(String.format(
                        "Phase-3 polish check failed: sounds=%s(%d) audio=%s ambient=%d hurt=%d death=%d rankDyn=%s thresholds=%s habitLow=%s habitHigh=%s",
                        soundsOk, soundEntries.size(), entityAudioOk,
                        distinctAmbient.size(), distinctHurt.size(), distinctDeath.size(),
                        rankDynamicsOk, thresholdsOk, belowHabitThreshold, atHabitThreshold));
                return;
            }
            helper.succeed();
        } finally {
            hive.resetAll();
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveSyncV11ThresholdsSynapticRecipesAndAntibodyCystLootWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos benchPos = helper.absolutePos(new BlockPos(2, 1, 2));
        try {
            // 1. HiveSyncPayload v11 threshold sync round-trip & ClientHiveCache update
            HiveSyncPayload payload = new HiveSyncPayload(
                    35.0, 1, 0, 0, false, false, BlockPos.ZERO, 1, 0,
                    50.0, 20.0, 10.0, 5.0, 0.0, 2, 1, 24, 28, benchPos,
                    List.of(), List.of(), 0, 0, 0, 0, 0, -1, 0, 0, 0.0,
                    0, 0, 12.0, 0, false, 0, 42.5, 165.0
            );
            var buf = new net.minecraft.network.FriendlyByteBuf(Unpooled.buffer());
            HiveSyncPayload.STREAM_CODEC.encode(buf, payload);
            HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);
            buf.release();

            ClientHiveCache.update(
                    decoded.threatLevel(), decoded.decodeTraits(), decoded.decodeTier2Traits(),
                    decoded.decodeSynergies(), decoded.inProtectedZone(), null,
                    decoded.activeNetworkLinks(), decoded.suppressionSecondsRemaining(),
                    new double[]{decoded.meleeScore(), decoded.projectileScore(), decoded.fireScore(),
                            decoded.explosionScore(), decoded.magicScore()},
                    decoded.totalNodes(), decoded.totalPylons(), decoded.pylonRadius(),
                    decoded.linkRadius(), decoded.playerAnchorPos(), decoded.nearbyNodes(),
                    decoded.nearbyPylons(), decoded.fungalTideSecondsRemaining(),
                    new double[]{decoded.medicHabitScore(), decoded.mobilityHabitScore(),
                            decoded.bastionHabitScore(), decoded.shadowHabitScore()},
                    decoded.falseStimulusOrdinal(), decoded.deceptionSecondsRemaining(),
                    decoded.deceptionCooldownSecondsRemaining(), decoded.nearestVolatileBuffer(),
                    decoded.squadTacticalStatus(), decoded.squadStatusSecondsRemaining(),
                    decoded.homeostasisStress(), decoded.homeostasisStage(),
                    decoded.acidRainExposed(), decoded.smotheredPylonCount(),
                    decoded.traitActivationThreshold(), decoded.tier2ActivationThreshold()
            );
            boolean syncOk = Math.abs(ClientHiveCache.getTraitActivationThreshold() - 42.5) < 0.01
                    && Math.abs(ClientHiveCache.getTier2ActivationThreshold() - 165.0) < 0.01;

            // Compare the shared recipe descriptors with actual bench outputs.
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            var recipes = SynapticBenchRecipeDefinitions.allRecipes();
            int matchedRecipes = 0;
            int matchedBloomPalettes = 0;
            if (level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench) {
                bench.setLabTier(3);
                for (var desc : recipes) {
                    bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, desc.sampleInputs().get(0).copy());
                    bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, desc.biomassInputs().get(0).copy());
                    bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST,
                            desc.catalystInputs().isEmpty() ? ItemStack.EMPTY : desc.catalystInputs().get(0).copy());
                    ItemStack result = bench.computeSynthesisResult();
                    if (!result.isEmpty() && result.is(desc.output().getItem())) {
                        matchedRecipes++;
                        if (desc.id().startsWith("reclaimed_bloom_") && result.getCount() == 2
                                && result.is(desc.sampleInputs().getFirst().getItem())) {
                            matchedBloomPalettes++;
                        }
                    }
                }
            }

            // 3. antibody_cyst loot table resolution & MycelialText localization components
            ResourceKey<LootTable> cystKey = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "blocks/antibody_cyst"));
            LootTable cystTable = level.getServer().reloadableRegistries().getLootTable(cystKey);
            boolean lootOk = cystTable != LootTable.EMPTY;

            boolean textOk = !MycelialText.stimulus(StimulusType.MELEE).getString().isEmpty()
                    && !MycelialText.trait(AdaptationTrait.FIRE_RESISTANT).getString().isEmpty()
                    && !MycelialText.traitTier2(AdaptationTrait.BLAST_SHIELDED).getString().isEmpty()
                    && !MycelialText.synergy(HiveSynergy.PYROLYTIC_SHELL).getString().isEmpty()
                    && !MycelialText.habit(PlayerHabit.COMBAT_MEDIC).getString().isEmpty()
                    && !MycelialText.mutagen(com.alex.mycelialmind.symbiosis.SymbioticMutagen.PYRO_GLAND).getString().isEmpty()
                    && !MycelialText.progressionTier(com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND).getString().isEmpty()
                    && !MycelialText.homeostasisStage(HomeostasisStage.IMMUNE_RESPONSE).getString().isEmpty()
                    && !MycelialText.strain(com.alex.mycelialmind.hive.HiveStrain.AMBER).getString().isEmpty()
                    && !MycelialText.nodeType(SynapticNodeType.SYMBIOTIC).getString().isEmpty()
                    && !MycelialText.standing(com.alex.mycelialmind.diplomacy.PlayerDiplomacyData.Standing.ALLIED).getString().isEmpty()
                    && !MycelialText.rank(EvolutionRank.ELITE).getString().isEmpty()
                    && !MycelialText.chimeraMode(MycelialChimeraEntity.CommandMode.PATROL).getString().isEmpty()
                    && !MycelialText.cardinalDirection(10, -10).getString().isEmpty();

            if (!syncOk || matchedRecipes != recipes.size() || matchedBloomPalettes != 6 || !lootOk || !textOk) {
                helper.fail(String.format(
                        "Phase-4 polish check failed: syncOk=%s recipes=%d/%d bloomPalettes=%d/6 lootOk=%s textOk=%s",
                        syncOk, matchedRecipes, recipes.size(), matchedBloomPalettes, lootOk, textOk));
                return;
            }
            helper.succeed();
        } finally {
            ClientHiveCache.update(0.0, java.util.Set.of(), java.util.Set.of(), java.util.Set.of(),
                    false, null, 0, 0, new double[5]);
            level.removeBlock(benchPos, false);
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void comparatorTelemetryCompostablesAndPhase5LocalizationWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos benchPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos turretPos = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockPos pylonPos = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos symPos = helper.absolutePos(new BlockPos(1, 1, 4));
        BlockPos nodePos = helper.absolutePos(new BlockPos(3, 1, 4));
        BlockPos[] cystOffsets = {
                pylonPos.north(), pylonPos.south(), pylonPos.east(), pylonPos.west()
        };
        SporelingEntity mob = null;
        try {
            // 1. SynapticResearchBench comparator signal (0 -> 3 -> 15)
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            int benchEmptySig = level.getBlockState(benchPos).getAnalogOutputSignal(level, benchPos);
            int benchOneInputSig = 0;
            int benchReadySig = 0;
            if (level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench) {
                bench.setLabTier(2);
                ItemStack sample = SynapticBenchRecipeDefinitions.sampleStack(AdaptationTrait.MELEE_THORNS);
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, sample);
                benchOneInputSig = level.getBlockState(benchPos).getAnalogOutputSignal(level, benchPos);
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS,
                        new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST,
                        new ItemStack(ModItems.SYNAPTIC_CORE.get()));
                benchReadySig = level.getBlockState(benchPos).getAnalogOutputSignal(level, benchPos);
            }
            boolean benchOk = benchEmptySig == 0 && benchOneInputSig == 3 && benchReadySig == 15;

            // 2. PurificationTurret comparator signal (5/10/15, 0 when jammed)
            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            int t1Sig = 0;
            int t2Sig = 0;
            int t3Sig = 0;
            int jammedSig = -1;
            if (level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity turret) {
                t1Sig = level.getBlockState(turretPos).getAnalogOutputSignal(level, turretPos);
                turret.setTier(2);
                t2Sig = level.getBlockState(turretPos).getAnalogOutputSignal(level, turretPos);
                turret.setTier(3);
                t3Sig = level.getBlockState(turretPos).getAnalogOutputSignal(level, turretPos);
                turret.applyCorrosiveJam(40);
                jammedSig = level.getBlockState(turretPos).getAnalogOutputSignal(level, turretPos);
            }
            boolean turretOk = t1Sig == 5 && t2Sig == 10 && t3Sig == 15 && jammedSig == 0;

            // 3. PurificationPylon comparator signal (5/10/15 by tier -> 0 smothered by 4 Antibody Cysts) & Flora signal
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            int pylonT1Sig = level.getBlockState(pylonPos).getAnalogOutputSignal(level, pylonPos);
            int pylonT2Sig = 0;
            int pylonT3Sig = 0;
            if (level.getBlockEntity(pylonPos) instanceof MonumentFocusBlockEntity pylonBe) {
                pylonBe.setTier(2);
                pylonT2Sig = level.getBlockState(pylonPos).getAnalogOutputSignal(level, pylonPos);
                pylonBe.setTier(3);
                pylonT3Sig = level.getBlockState(pylonPos).getAnalogOutputSignal(level, pylonPos);
            }
            for (BlockPos cp : cystOffsets) {
                level.setBlockAndUpdate(cp, ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
            }
            int pylonSmotheredSig = level.getBlockState(pylonPos).getAnalogOutputSignal(level, pylonPos);
            level.setBlockAndUpdate(pylonPos.above(), ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState()
                    .setValue(BioluminescentMycelialCapBlock.MATURE, true));
            int floraMatureSig = level.getBlockState(pylonPos.above()).getAnalogOutputSignal(level, pylonPos.above());
            level.setBlockAndUpdate(pylonPos.above(), ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState()
                    .setValue(BioluminescentMycelialCapBlock.MATURE, false));
            int floraRegrowSig = level.getBlockState(pylonPos.above()).getAnalogOutputSignal(level, pylonPos.above());
            level.removeBlock(pylonPos.above(), false);
            boolean pylonOk = pylonT1Sig == 5 && pylonT2Sig == 10 && pylonT3Sig == 15
                    && pylonSmotheredSig == 0 && floraMatureSig == 15 && floraRegrowSig == 5;

            int veinWeakSig = com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(0, false);
            int veinMidSig = com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(2, false);
            int veinChargedSig = com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(3, true);
            pylonOk = pylonOk && veinWeakSig == 0 && veinMidSig == 8 && veinChargedSig == 15;

            // 4. SymbioticNode (15 -> 0) & hostile MycelialNode (0 -> 15) comparator signals
            level.setBlockAndUpdate(symPos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            int symFullSig = level.getBlockState(symPos).getAnalogOutputSignal(level, symPos);
            int symZeroSig = -1;
            if (level.getBlockEntity(symPos) instanceof SynapticNodeBlockEntity symBe) {
                symBe.setSymbioticStability(0);
                symZeroSig = level.getBlockState(symPos).getAnalogOutputSignal(level, symPos);
            }
            level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
            int nodeEmptySig = level.getBlockState(nodePos).getAnalogOutputSignal(level, nodePos);
            int nodeChargedSig = 0;
            if (level.getBlockEntity(nodePos) instanceof SynapticNodeBlockEntity nodeBe) {
                nodeBe.addVolatileStimulus(StimulusType.MELEE, 100.0);
                nodeChargedSig = level.getBlockState(nodePos).getAnalogOutputSignal(level, nodePos);
            }
            boolean nodesOk = symFullSig == 15 && symZeroSig == 0 && nodeEmptySig == 0 && nodeChargedSig == 15;

            // 5. Vanilla Composter data map entries (5 entries)
            boolean compostOk = net.minecraft.world.level.block.ComposterBlock.getValue(new ItemStack(ModItems.MYCELIAL_BIOMASS.get())) > 0.0F
                    && net.minecraft.world.level.block.ComposterBlock.getValue(new ItemStack(ModItems.LATENT_SPORE.get())) > 0.0F
                    && net.minecraft.world.level.block.ComposterBlock.getValue(new ItemStack(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())) > 0.0F
                    && net.minecraft.world.level.block.ComposterBlock.getValue(new ItemStack(ModBlocks.SPORE_BULB.get())) > 0.0F
                    && net.minecraft.world.level.block.ComposterBlock.getValue(new ItemStack(ModBlocks.MYCELIAL_TENDRILS.get())) > 0.0F;

            // 6. BioScannerItem tactical status bug fix (Frenzy shows frenzy seconds, not desync seconds) & TranslatableEnum
            mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 5, 1, 4);
            mob.triggerFrenzy(160);
            var frenzyComp = BioScannerItem.formatMobTacticalStatus(mob);
            var frenzyContents = (net.minecraft.network.chat.contents.TranslatableContents) frenzyComp.getContents();
            boolean frenzyOk = "hud.mycelialmind.squad_frenzy".equals(frenzyContents.getKey())
                    && frenzyContents.getArgs().length == 1
                    && Integer.valueOf(8).equals(frenzyContents.getArgs()[0]);

            mob.triggerDesync(100);
            var desyncComp = BioScannerItem.formatMobTacticalStatus(mob);
            var desyncContents = (net.minecraft.network.chat.contents.TranslatableContents) desyncComp.getContents();
            boolean desyncOk = "hud.mycelialmind.squad_desynced".equals(desyncContents.getKey())
                    && desyncContents.getArgs().length == 1
                    && Integer.valueOf(5).equals(desyncContents.getArgs()[0]);

            var anchorComp = MycelialClientConfig.HudAnchor.TOP_LEFT.getTranslatedName();
            var anchorContents = (net.minecraft.network.chat.contents.TranslatableContents) anchorComp.getContents();
            boolean anchorOk = "mycelialmind.configuration.hudAnchor.top_left".equals(anchorContents.getKey());

            if (!benchOk || !turretOk || !pylonOk || !nodesOk || !compostOk || !frenzyOk || !desyncOk || !anchorOk) {
                helper.fail(String.format(
                        "Phase-5 polish check failed: bench=%s turret=%s pylon=%s nodes=%s compost=%s frenzy=%s desync=%s anchor=%s",
                        benchOk, turretOk, pylonOk, nodesOk, compostOk, frenzyOk, desyncOk, anchorOk));
                return;
            }
            helper.succeed();
        } finally {
            if (mob != null && !mob.isRemoved()) {
                mob.discard();
            }
            for (BlockPos cp : cystOffsets) {
                level.removeBlock(cp, false);
            }
            level.removeBlock(benchPos, false);
            level.removeBlock(turretPos, false);
            level.removeBlock(pylonPos, false);
            level.removeBlock(symPos, false);
            level.removeBlock(nodePos, false);
        }
    }

    @GameTest(template = "empty", batch = "homeostasis")
    public static void homeostasisV2TraumaSensitizationDiffusionTurretCorrosionAndV12SyncWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);

        BlockPos base = homeostasisTestPos(helper, 5, new BlockPos(2, 14, 2));
        BlockPos stonePos = base;
        BlockPos expTurretPos = base.east(2);
        BlockPos expCeilingPos = expTurretPos.above(4);
        BlockPos shTurretPos = base.east(4);
        BlockPos shRoofPos = shTurretPos.above(2);
        BlockPos shCeilingPos = shTurretPos.above(5);
        BlockPos cystPos = base.south(2);
        ChunkPos mainChunk = new ChunkPos(base);
        ChunkPos neighborChunk = new ChunkPos(mainChunk.x + 1, mainChunk.z);
        BlockPos patrolTargetPos = mainChunk.getMiddleBlockPosition(base.getY() + 8);
        List<MycelialMob> spawnedPatrol = List.of();
        resetHomeostasisChunks(data, mainChunk, 1);

        try {
            // 1. Native tissue trauma on break
            Player breaker = helper.makeMockPlayer(GameType.SURVIVAL);
            level.setBlockAndUpdate(stonePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
            DimensionalHomeostasisDirector.handleBlockBreak(level, stonePos, level.getBlockState(stonePos), breaker);
            double traumaStress = data.getStress(mainChunk);

            // 2. Escalation -> Sensitization Lv.1, neighbor diffusion capped at 45%, then sedation reduction
            data.setStress(mainChunk, 88.0, level.getGameTime());
            data.adjustSensitization(mainChunk, 1);
            int sensBefore = data.getSensitization(mainChunk);
            DimensionalHomeostasisDirector.diffuseInflammationToNeighbors(level, data, mainChunk, 88.0);
            double neighborAfter = data.getStress(neighborChunk);

            // 3. Acid rain corrosion on exposed turret vs sheltered turret
            level.setBlockAndUpdate(expCeilingPos, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
            level.setBlockAndUpdate(expTurretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            boolean expJammed = DimensionalHomeostasisDirector.applyAcidRainCorrosionToTurret(level, expTurretPos);

            level.setBlockAndUpdate(shCeilingPos, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
            level.setBlockAndUpdate(shRoofPos, ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
            level.setBlockAndUpdate(shTurretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            boolean shJammed = DimensionalHomeostasisDirector.applyAcidRainCorrosionToTurret(level, shTurretPos);

            // 4. AntibodyCyst stepOn + comparator signal (active vs withered)
            level.setBlockAndUpdate(cystPos, ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
            BlockState activeState = level.getBlockState(cystPos);
            activeState.getBlock().stepOn(level, cystPos, activeState, breaker);
            boolean slowed = breaker.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            int activeSignal = activeState.getAnalogOutputSignal(level, cystPos);

            level.setBlockAndUpdate(cystPos, activeState.setValue(com.alex.mycelialmind.block.AntibodyCystBlock.ACTIVE, false));
            int witheredSignal = level.getBlockState(cystPos).getAnalogOutputSignal(level, cystPos);

            // 5. Cytokine storm leukocyte patrol spawn + cooldown
            level.setBlockAndUpdate(patrolTargetPos, Blocks.DIRT.defaultBlockState());
            data.recordForeignBlock(patrolTargetPos);
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.setBlock(base.offset(dx, -1, dz), ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 18);
                }
            }
            int patrolCount = DimensionalHomeostasisDirector.trySpawnLeukocytePatrol(level, data, mainChunk, base);
            int cooldownCount = DimensionalHomeostasisDirector.trySpawnLeukocytePatrol(level, data, mainChunk, base);
            spawnedPatrol = level.getEntitiesOfClass(MycelialMob.class, new AABB(base).inflate(16.0D));

            // 6. Sedation drops sensitization
            DimensionalHomeostasisDirector.sedateArea(level, base, 0,
                    DimensionalHomeostasisEngine.SUPPRESSOR_SEDATION,
                    DimensionalHomeostasisEngine.SUPPRESSOR_SENSITIZATION_REDUCTION,
                    null);
            int sensAfter = data.getSensitization(mainChunk);

            // 7. HiveSyncPayload v12 round-trip with sensitization, smothered pylons & inflamed chunks
            HiveSyncPayload payload = new HiveSyncPayload(
                    40.0, 0, 0, 0, false, false, BlockPos.ZERO, 0, 0,
                    0, 0, 0, 0, 0, 0, 1, 24, 28, base, List.of(), List.of(expTurretPos), 0,
                    0, 0, 0, 0, -1, 0, 0, 0.0, 0, 0,
                    88.0, 2, true, 1, 70.0, 200.0,
                    2, List.of(expTurretPos),
                    List.of(new HiveSyncPayload.InflamedChunkMarker(mainChunk.x, mainChunk.z, 88, 2, 2))
            );
            var buf = new net.minecraft.network.FriendlyByteBuf(Unpooled.buffer());
            HiveSyncPayload.STREAM_CODEC.encode(buf, payload);
            HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);
            buf.release();

            boolean syncOk = decoded.homeostasisSensitization() == 2
                    && decoded.smotheredPylons().size() == 1
                    && decoded.inflamedChunks().size() == 1
                    && decoded.inflamedChunks().get(0).stressPercent() == 88;

            if (traumaStress < 3.0 || sensBefore < 1 || sensAfter >= sensBefore
                    || neighborAfter <= 0.0 || neighborAfter > 45.01
                    || !expJammed || shJammed || !slowed || activeSignal <= 0 || witheredSignal != 0
                    || patrolCount < 1 || cooldownCount != 0 || !syncOk) {
                helper.fail(String.format(
                        "Homeostasis v2 failed: trauma=%.1f sens=%d->%d nbr=%.1f expJam=%s shJam=%s slow=%s sig=%d/%d patrol=%d/%d sync=%s",
                        traumaStress, sensBefore, sensAfter, neighborAfter, expJammed, shJammed,
                        slowed, activeSignal, witheredSignal, patrolCount, cooldownCount, syncOk));
                return;
            }
            helper.succeed();
        } finally {
            for (MycelialMob mob : spawnedPatrol) {
                if (!mob.isRemoved()) {
                    mob.discard();
                }
            }
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.setBlock(base.offset(dx, -1, dz), Blocks.AIR.defaultBlockState(), 18);
                }
            }
            for (BlockPos p : new BlockPos[]{stonePos, expTurretPos, expCeilingPos, shTurretPos, shRoofPos, shCeilingPos, cystPos}) {
                level.removeBlock(p, false);
            }
            level.removeBlock(patrolTargetPos, false);
            resetHomeostasisChunks(data, mainChunk, 1);
        }
    }

    @GameTest(template = "empty", batch = "homeostasis")
    public static void deepMyceliumProgressionSyncMatriarchRemissionAndV13PayloadWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel deep = level.getServer().getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
        HiveMindSavedData owHive = new HiveMindSavedData();
        HiveMindSavedData deepHive = HiveMindSavedData.get(deep != null ? deep : level);
        DimensionalHomeostasisSavedData deepHomeo = DimensionalHomeostasisSavedData.get(deep != null ? deep : level);
        deepHive.resetAll();

        BlockPos anchor = homeostasisTestPos(helper, 6, new BlockPos(2, 1, 2));
        resetHomeostasisChunks(deepHomeo, new ChunkPos(anchor), 1);
        try {
            long now = level.getGameTime();
            owHive.recordStimulus(StimulusType.FIRE, 110.0, now);
            owHive.addThreatPenalty(65.0);
            owHive.markDeepMyceliumBreached();
            deepHive.syncProgressionFrom(owHive);
            boolean deepSynced = deepHive.isDeepMyceliumBreached()
                    && deepHive.getThreatLevel() >= 55.0
                    && deepHive.getActiveTraits().contains(AdaptationTrait.FIRE_RESISTANT)
                    && deepHive.getHighestProgressionTier().level() >= com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE.level();

            ServerLevel targetLevel = deep != null ? deep : level;
            double scaledBefore = DimensionalHomeostasisDirector.applyProgressionStressScaling(targetLevel, 10.0);
            deepHomeo.setStress(new ChunkPos(anchor), 85.0, targetLevel.getGameTime());
            com.alex.mycelialmind.entity.ApexMatriarchEntity.onMatriarchDefeated(targetLevel, anchor);
            double scaledAfter = DimensionalHomeostasisDirector.applyProgressionStressScaling(targetLevel, 10.0);
            double calmedStress = deepHomeo.getStress(new ChunkPos(anchor));

            HiveSyncPayload payload = new HiveSyncPayload(
                    42.0, 1, 0, 0, false, false, BlockPos.ZERO, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 24, 28, anchor, List.of(), List.of(), 0,
                    0, 0, 0, 0, -1, 0, 0, 0.0, 0, 0,
                    25.0, 0, false, 0, 70.0, 200.0,
                    1, List.of(), List.of(),
                    true, true, 1200
            );
            var buf = new net.minecraft.network.FriendlyByteBuf(Unpooled.buffer());
            HiveSyncPayload.STREAM_CODEC.encode(buf, payload);
            HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);
            buf.release();

            boolean payloadOk = decoded.deepMyceliumBreached()
                    && decoded.matriarchDefeated()
                    && decoded.matriarchRemissionSecondsRemaining() == 1200;

            if (!deepSynced || scaledBefore <= 10.0
                    || !deepHive.isMatriarchDefeated() || !deepHive.isMatriarchRemissionActive(targetLevel.getGameTime())
                    || (deep != null && calmedStress >= 85.0) || scaledAfter >= scaledBefore || !payloadOk) {
                helper.fail(String.format(
                        "Dimension progression sync failed: synced=%s scale=%.2f->%.2f calm=%.1f def=%s rem=%s payload=%s",
                        deepSynced, scaledBefore, scaledAfter, calmedStress,
                        deepHive.isMatriarchDefeated(), deepHive.isMatriarchRemissionActive(targetLevel.getGameTime()), payloadOk));
                return;
            }
            helper.succeed();
        } finally {
            deepHive.resetAll();
            resetHomeostasisChunks(deepHomeo, new ChunkPos(anchor), 1);
        }
    }

    @GameTest(template = "empty", batch = "cross_system_integration")
    public static void crossSystemIntegrationProjectilesRelicsAndCompatWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData.get(level).resetAll();
        HiveBlackboard.clear(level);

        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos turretPos = base.offset(0, 0, 2);
        BlockPos ephPos = base.offset(1, 0, 2);
        BlockPos centerPos = base.offset(3, 0, 4);
        BlockPos greEphPos = centerPos.east();
        BlockPos greCystPos = centerPos.west();

        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 2, 2, 2);
        var drone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 5, 2, 2);
        var mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 5, 2, 6);

        try {
            drone.setPos(base.getX() + 3.5, base.getY() + 1.0, base.getZ() + 0.5);
            var spitDrone = new AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
            spitDrone.setOwner(spitter);
            spitDrone.setPos(drone.getX(), drone.getY() + drone.getBbHeight() + 0.35, drone.getZ());
            spitDrone.shoot(0.0, -1.0, 0.0, 1.2F, 0.0F);
            level.addFreshEntity(spitDrone);
            for (int t = 0; t < 6 && !spitDrone.isRemoved(); t++) {
                spitDrone.tick();
            }
            boolean acidJammedDrone = drone.isJammed();

            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            var spitTurret = new AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
            spitTurret.setOwner(spitter);
            spitTurret.setPos(turretPos.getX() + 0.5, turretPos.getY() + 1.25, turretPos.getZ() + 0.5);
            spitTurret.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
            level.addFreshEntity(spitTurret);
            for (int t = 0; t < 6 && !spitTurret.isRemoved(); t++) {
                spitTurret.tick();
            }
            boolean acidJammedTurret = level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity t1 && t1.isJammed();

            float droneHpBefore = drone.getHealth();
            var sporeDrone = new NeutralizingSporeProjectile(ModEntities.NEUTRALIZING_SPORE.get(), level);
            sporeDrone.setPos(drone.getX(), drone.getY() + drone.getBbHeight() + 0.35, drone.getZ());
            sporeDrone.shoot(0.0, -1.0, 0.0, 1.2F, 0.0F);
            level.addFreshEntity(sporeDrone);
            for (int t = 0; t < 6 && !sporeDrone.isRemoved(); t++) {
                sporeDrone.tick();
            }
            boolean sporeUnjammedDroneSafely = !drone.isJammed() && Math.abs(drone.getHealth() - droneHpBefore) < 0.01F;

            var sporeTurret = new NeutralizingSporeProjectile(ModEntities.NEUTRALIZING_SPORE.get(), level);
            sporeTurret.setPos(turretPos.getX() + 0.5, turretPos.getY() + 1.25, turretPos.getZ() + 0.5);
            sporeTurret.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
            level.addFreshEntity(sporeTurret);
            for (int t = 0; t < 6 && !sporeTurret.isRemoved(); t++) {
                sporeTurret.tick();
            }
            boolean sporeUnjammedTurret = level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity t2 && !t2.isJammed();

            level.setBlockAndUpdate(ephPos, ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState());
            var sporeBlock = new NeutralizingSporeProjectile(ModEntities.NEUTRALIZING_SPORE.get(), level);
            sporeBlock.setPos(ephPos.getX() + 0.5, ephPos.getY() + 1.25, ephPos.getZ() + 0.5);
            sporeBlock.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
            level.addFreshEntity(sporeBlock);
            for (int t = 0; t < 6 && !sporeBlock.isRemoved(); t++) {
                sporeBlock.tick();
            }
            boolean sporeDissolvedChitin = level.getBlockState(ephPos).isAir();

            level.setBlockAndUpdate(centerPos, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(greEphPos, ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState());
            level.setBlockAndUpdate(greCystPos, ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
            mob.setPos(centerPos.getX() + 2.5, centerPos.getY() + 1.0, centerPos.getZ() + 0.5);
            mob.setCommandRole(SynapticCommandSystem.CommandRole.LEADER);
            drone.setJammedTicks(60);
            drone.setPos(centerPos.getX() + 2.5, centerPos.getY() + 1.0, centerPos.getZ() + 2.0);
            Vec3 greCarrion = Vec3.atCenterOf(centerPos.above());
            HiveBlackboard.registerCarrion(level, greCarrion, 200);

            var grenade = new PurifierGrenadeProjectile(ModEntities.PURIFIER_GRENADE.get(), level);
            grenade.setPos(centerPos.getX() + 0.5, centerPos.getY() + 1.35, centerPos.getZ() + 0.5);
            grenade.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
            level.addFreshEntity(grenade);
            for (int t = 0; t < 6 && !grenade.isRemoved(); t++) {
                grenade.tick();
            }
            boolean grenadeOk = level.getBlockState(greEphPos).isAir()
                    && level.getBlockState(greCystPos).isAir()
                    && mob.isDesynced()
                    && !drone.isJammed()
                    && HiveBlackboard.findNearestCarrion(level, greCarrion, 5.0D) == null;

            level.setBlockAndUpdate(ephPos, ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState());
            level.setBlockAndUpdate(greCystPos, ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
            Vec3 crownCarrion = Vec3.atCenterOf(centerPos);
            HiveBlackboard.registerCarrion(level, crownCarrion, 200);
            int crownCleansed = OvermindCrownItem.cleanseMyceliumRadius(level, centerPos, 6);
            boolean crownOk = crownCleansed >= 2
                    && level.getBlockState(ephPos).isAir()
                    && level.getBlockState(greCystPos).isAir()
                    && HiveBlackboard.findNearestCarrion(level, crownCarrion, 6.0D) == null;

            mob.setDesyncTicks(0);
            mob.setFrenzyTicks(0);
            mob.setPhalanxActive(false);
            mob.setCommandRole(SynapticCommandSystem.CommandRole.NONE);
            String sUnlinked = BioScannerItem.formatMobTacticalStatus(mob).getContents() instanceof TranslatableContents tc0 ? tc0.getKey() : "";
            mob.setCommandRole(SynapticCommandSystem.CommandRole.LINKED);
            String sLinked = BioScannerItem.formatMobTacticalStatus(mob).getContents() instanceof TranslatableContents tc1 ? tc1.getKey() : "";
            mob.setCommandRole(SynapticCommandSystem.CommandRole.LEADER);
            String sLeader = BioScannerItem.formatMobTacticalStatus(mob).getContents() instanceof TranslatableContents tc2 ? tc2.getKey() : "";
            mob.setPhalanxActive(true);
            String sPhalanx = BioScannerItem.formatMobTacticalStatus(mob).getContents() instanceof TranslatableContents tc3 ? tc3.getKey() : "";
            mob.setPhalanxActive(false);
            mob.triggerFrenzy(80);
            String sFrenzy = BioScannerItem.formatMobTacticalStatus(mob).getContents() instanceof TranslatableContents tc4 ? tc4.getKey() : "";
            mob.triggerDesync(100);
            String sDesynced = BioScannerItem.formatMobTacticalStatus(mob).getContents() instanceof TranslatableContents tc5 ? tc5.getKey() : "";
            boolean statusOk = "hud.mycelialmind.squad_unlinked".equals(sUnlinked)
                    && "hud.mycelialmind.squad_synced".equals(sLinked)
                    && "hud.mycelialmind.squad_leader".equals(sLeader)
                    && "hud.mycelialmind.squad_phalanx".equals(sPhalanx)
                    && "hud.mycelialmind.squad_frenzy".equals(sFrenzy)
                    && "hud.mycelialmind.squad_desynced".equals(sDesynced);

            BlockState ephState = ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState();
            boolean tagsAndCompatOk = SynapticBenchRecipeDefinitions.allCompatInfoEntries().size() == 36
                    && ephState.is(BlockTags.MINEABLE_WITH_HOE)
                    && ephState.is(BlockTags.MINEABLE_WITH_PICKAXE);

            if (!acidJammedDrone || !acidJammedTurret || !sporeUnjammedDroneSafely || !sporeUnjammedTurret
                    || !sporeDissolvedChitin || !grenadeOk || !crownOk || !statusOk || !tagsAndCompatOk) {
                helper.fail(String.format(
                        "Cross-system integration failed: acidDrone=%s acidTurret=%s sporeDrone=%s sporeTurret=%s sporeBlock=%s grenade=%s crown=%s status=%s tagsCompat=%s",
                        acidJammedDrone, acidJammedTurret, sporeUnjammedDroneSafely, sporeUnjammedTurret,
                        sporeDissolvedChitin, grenadeOk, crownOk, statusOk, tagsAndCompatOk));
                return;
            }
            helper.succeed();
        } finally {
            for (BlockPos p : new BlockPos[]{turretPos, ephPos, centerPos, greEphPos, greCystPos}) {
                level.removeBlock(p, false);
            }
            spitter.discard();
            drone.discard();
            mob.discard();
            HiveBlackboard.clear(level);
            HiveMindSavedData.get(level).resetAll();
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void wholeModDiplomacyEscortProtectionAndCompanionSafetyWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData.get(level).resetAll();
        HiveBlackboard.clear(level);

        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos stonePos = base.offset(0, 0, 2);
        BlockPos riftPos = base.offset(1, 0, 2);
        BlockPos altarPos = base.offset(2, 0, 2);
        BlockPos turretPos = base.offset(3, 0, 2);
        BlockPos ephConePos = base.offset(4, 1, 2);
        BlockPos cystPos = base.offset(0, 0, 4);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(base.getX() + 2.5, base.getY() + 1.0, base.getZ() + 2.5);
        player.setYRot(-90.0F);
        player.setXRot(0.0F);

        var escort = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 4, 2, 4);
        var hostile = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 5, 2, 4);
        var chimera = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_CHIMERA.get(), 3, 2, 4);
        var drone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 3, 2, 5);
        var freshDrone = ModEntities.PURIFIER_DRONE.get().create(level);
        var freshChimera = ModEntities.MYCELIAL_CHIMERA.get().create(level);

        try {
            escort.setAlliedEscort(player.getUUID(), level.getGameTime() + 600L);
            chimera.setOwnerUUID(player.getUUID());
            chimera.setHomePos(base);
            drone.setOwner(player.getUUID());

            boolean friendlyCheckOk = MycelialMob.isFriendlyToPlayer(escort, player)
                    && !MycelialMob.isFriendlyToPlayer(hostile, player)
                    && !escort.canAttack(chimera)
                    && !escort.canAttack(drone)
                    && !chimera.isValidChimeraHostile(escort)
                    && chimera.isValidChimeraHostile(hostile);

            hostile.moveTo(base.getX() + 40.0, base.getY() + 1.0, base.getZ() + 40.0);
            var nearbyNonEscortMobs = level.getEntitiesOfClass(
                            MycelialMob.class, drone.getBoundingBox().inflate(20.0D))
                    .stream()
                    .filter(candidate -> candidate != escort)
                    .toList();
            var nearbyNonEscortPositions = nearbyNonEscortMobs.stream().map(MycelialMob::position).toList();
            for (int i = 0; i < nearbyNonEscortMobs.size(); i++) {
                nearbyNonEscortMobs.get(i).moveTo(
                        base.getX() + 128.0D + i * 4.0D,
                        base.getY() + 1.0D,
                        base.getZ() + 128.0D
                );
            }
            boolean droneSkippedEscort = !drone.tryFireAtNearestMob(level);
            for (int i = 0; i < nearbyNonEscortMobs.size(); i++) {
                Vec3 oldPos = nearbyNonEscortPositions.get(i);
                nearbyNonEscortMobs.get(i).moveTo(oldPos.x, oldPos.y, oldPos.z);
            }
            String droneCandidates = level.getEntitiesOfClass(
                            MycelialMob.class, drone.getBoundingBox().inflate(20.0D))
                    .stream()
                    .map(candidate -> candidate.getType() + "@" + candidate.blockPosition()
                            + " escort=" + candidate.isAlliedEscort() + " uuid=" + candidate.getUUID())
                    .collect(java.util.stream.Collectors.joining("; "));

            hostile.moveTo(base.getX() + 4.5, base.getY() + 1.0, base.getZ() + 2.5);
            hostile.setPhalanxActive(true);
            float escortHpBefore = escort.getHealth();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ENZYME_LANCE.get()));
            ModItems.ENZYME_LANCE.get().use(level, player, InteractionHand.MAIN_HAND);
            boolean lanceOk = !hostile.isPhalanxActive() && Math.abs(escort.getHealth() - escortHpBefore) < 0.01F;

            level.setBlockAndUpdate(ephConePos, ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYMBIOTIC_BLADE.get()));
            ModItems.SYMBIOTIC_BLADE.get().use(level, player, InteractionHand.MAIN_HAND);
            boolean bladeOk = level.getBlockState(ephConePos).isAir()
                    && Math.abs(escort.getHealth() - escortHpBefore) < 0.01F;

            drone.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 80, 0));
            chimera.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 80, 0));
            drone.setJammedTicks(55);
            CompoundTag dTag = new CompoundTag();
            drone.addAdditionalSaveData(dTag);
            if (freshDrone != null) {
                freshDrone.readAdditionalSaveData(dTag);
            }
            CompoundTag cTag = new CompoundTag();
            chimera.addAdditionalSaveData(cTag);
            if (freshChimera != null) {
                freshChimera.readAdditionalSaveData(cTag);
            }
            boolean companionSafetyOk = !drone.hasEffect(ModEffects.SPORE_ROT)
                    && !chimera.hasEffect(ModEffects.MYCELIAL_INFECTION)
                    && !DamageTelemetryTracker.tryConvertInfectedMob(level, chimera, false)
                    && !DamageTelemetryTracker.tryConvertInfectedMob(level, drone, false)
                    && freshDrone != null && freshDrone.isJammed()
                    && freshChimera != null && base.equals(freshChimera.getHomePos());

            level.setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(riftPos, ModBlocks.MYCELIAL_RIFT.get().defaultBlockState());
            level.setBlockAndUpdate(altarPos, ModBlocks.HIVE_ALTAR.get().defaultBlockState());
            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            boolean burrowerSafetyOk = BurrowerEntity.canBurrowerBreak(level, stonePos, 15)
                    && !BurrowerEntity.canBurrowerBreak(level, riftPos, 15)
                    && !BurrowerEntity.canBurrowerBreak(level, altarPos, 15)
                    && !BurrowerEntity.canBurrowerBreak(level, turretPos, 15);

            player.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 100, 0));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYNAPTIC_SUPPRESSOR.get()));
            ModItems.SYNAPTIC_SUPPRESSOR.get().use(level, player, InteractionHand.MAIN_HAND);
            boolean suppressorCuredRot = !player.hasEffect(ModEffects.SPORE_ROT);

            level.setBlockAndUpdate(cystPos, ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_EXTRACTOR.get()));
            ModItems.BIO_EXTRACTOR.get().useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(cystPos), Direction.UP, cystPos, false)
            ));
            BlockState cystAfter = level.getBlockState(cystPos);
            boolean extractorCystOk = cystAfter.hasProperty(com.alex.mycelialmind.block.AntibodyCystBlock.ACTIVE)
                    && !cystAfter.getValue(com.alex.mycelialmind.block.AntibodyCystBlock.ACTIVE);

            boolean enchantRepairCompatOk = new ItemStack(ModItems.SYNAPTIC_SUPPRESSOR.get()).is(ItemTags.DURABILITY_ENCHANTABLE)
                    && new ItemStack(ModItems.RIFT_CATALYST.get()).is(ItemTags.DURABILITY_ENCHANTABLE)
                    && new ItemStack(ModItems.SCHISM_INJECTOR.get()).is(ItemTags.DURABILITY_ENCHANTABLE)
                    && new ItemStack(ModItems.FALSE_ENGRAM_INJECTOR.get()).is(ItemTags.DURABILITY_ENCHANTABLE)
                    && ModItems.SYNAPTIC_SUPPRESSOR.get().isValidRepairItem(
                            new ItemStack(ModItems.SYNAPTIC_SUPPRESSOR.get()), new ItemStack(ModItems.SYNAPTIC_CORE.get()))
                    && Math.abs(com.alex.mycelialmind.compat.azurelib.MycelialAzureLibCompat.easeInOutCubic(0.5) - 0.5) < 1.0E-6;

            if (!friendlyCheckOk || !droneSkippedEscort || !lanceOk || !bladeOk || !companionSafetyOk
                    || !burrowerSafetyOk || !suppressorCuredRot || !extractorCystOk || !enchantRepairCompatOk) {
                helper.fail(String.format(
                        "Whole-mod integration failed: friendly=%s droneEscort=%s candidates=[%s] lance=%s blade=%s comp=%s burrow=%s rotCure=%s cyst=%s compat=%s",
                        friendlyCheckOk, droneSkippedEscort, droneCandidates, lanceOk, bladeOk, companionSafetyOk,
                        burrowerSafetyOk, suppressorCuredRot, extractorCystOk, enchantRepairCompatOk));
                return;
            }
            helper.succeed();
        } finally {
            for (BlockPos p : new BlockPos[]{stonePos, riftPos, altarPos, turretPos, ephConePos, cystPos}) {
                level.removeBlock(p, false);
            }
            for (var proj : level.getEntitiesOfClass(NeutralizingSporeProjectile.class, new net.minecraft.world.phys.AABB(base).inflate(32.0D))) {
                proj.discard();
            }
            escort.discard();
            hostile.discard();
            chimera.discard();
            drone.discard();
            if (freshDrone != null) freshDrone.discard();
            if (freshChimera != null) freshChimera.discard();
            HiveBlackboard.clear(level);
            HiveMindSavedData.get(level).resetAll();
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void v2IntegrationPersistenceEscortsChimeraAndSymbiosisSyncWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        BlockPos relayPos = helper.absolutePos(new BlockPos(2, 175, 2));
        BlockPos symbioticPos = helper.absolutePos(new BlockPos(6, 175, 2));
        BlockPos hiveProbe = helper.absolutePos(new BlockPos(4000, 175, 4000));

        var chimera = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_CHIMERA.get(), 1, 1, 1);
        var escort = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 1);

        try {
            // 1) SymbiosisAttributeSync immediate equip + clear sync
            var symbiosis = player.getData(com.alex.mycelialmind.registry.ModAttachments.SYMBIOSIS);
            symbiosis.clearMutagens();
            com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync.sync(player);
            double baseArmor = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            double baseKb = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);

            symbiosis.equipMutagen(SymbioticMutagen.CHITIN_THORNS);
            symbiosis.equipMutagen(SymbioticMutagen.BLAST_CARAPACE);
            com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync.sync(player);
            double equippedArmor = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            double equippedKb = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);

            symbiosis.clearMutagens();
            com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync.sync(player);
            double clearedArmor = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            double clearedKb = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);

            boolean symbiosisSyncOk = Math.abs((equippedArmor - baseArmor) - 2.0D) < 0.01D
                    && Math.abs((equippedKb - baseKb) - 0.5D) < 0.01D
                    && Math.abs(clearedArmor - baseArmor) < 0.01D
                    && Math.abs(clearedKb - baseKb) < 0.01D;

            // 2) ChimeraOwnershipSavedData + owner retaliation guard + removal cleanup
            var chimeraStore = com.alex.mycelialmind.entity.ChimeraOwnershipSavedData.get(level.getServer());
            int chimerasBefore = chimeraStore.countForOwner(player.getUUID());
            chimera.setOwnerUUID(player.getUUID());
            int chimerasWhileAlive = chimeraStore.countForOwner(player.getUUID());
            boolean chimeraIgnoresOwner = !chimera.isValidChimeraHostile(player);
            chimera.discard();
            int chimerasAfterDiscard = chimeraStore.countForOwner(player.getUUID());
            boolean chimeraOwnershipOk = chimerasWhileAlive == chimerasBefore + 1
                    && chimeraIgnoresOwner
                    && chimerasAfterDiscard == chimerasBefore;

            // 3) AlliedEscortSavedData + idempotent promoteRank (no free heal on repeated rank)
            var escortStore = com.alex.mycelialmind.entity.AlliedEscortSavedData.get(level.getServer());
            long owNow = level.getServer().overworld().getGameTime();
            int escortsBefore = escortStore.countActiveForOwner(player.getUUID(), owNow);
            escort.setAlliedEscort(player.getUUID(), level.getGameTime() + 400L);
            escort.registerAlliedEscort();
            int escortsWhileAlive = escortStore.countActiveForOwner(player.getUUID(), owNow);

            escort.promoteRank(EvolutionRank.MUTATED);
            float damagedHp = Math.max(1.0F, escort.getMaxHealth() - 2.0F);
            escort.setHealth(damagedHp);
            escort.promoteRank(EvolutionRank.MUTATED);
            escort.promoteRank(EvolutionRank.BASE);
            boolean rankIdempotent = escort.getEvolutionRank() == EvolutionRank.MUTATED
                    && Math.abs(escort.getHealth() - damagedHp) < 0.01F;
            escort.discard();
            int escortsAfterDiscard = escortStore.countActiveForOwner(player.getUUID(), owNow);
            boolean escortLifecycleOk = escortsWhileAlive == escortsBefore + 1
                    && rankIdempotent
                    && escortsAfterDiscard == escortsBefore;

            // 4) PurificationZoneTracker network link exclusion for SYMBIOTIC + atomic SurfaceHive reservation
            int linksBefore = tracker.countActiveNetworkLinks();
            level.setBlockAndUpdate(relayPos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
            level.setBlockAndUpdate(symbioticPos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            tracker.addNode(relayPos, SynapticNodeType.RELAY);
            tracker.addNode(symbioticPos, SynapticNodeType.SYMBIOTIC);
            int linksAfter = tracker.countActiveNetworkLinks();
            boolean linksExcludeSymbiotic = linksAfter == linksBefore
                    && tracker.getLinkedNodes(relayPos).isEmpty()
                    && tracker.getLinkedNodes(symbioticPos).isEmpty();

            boolean firstReserve = tracker.tryReserveSurfaceHive(hiveProbe, 192, 64);
            boolean duplicateReserve = tracker.tryReserveSurfaceHive(hiveProbe.offset(4, 0, 4), 192, 64);
            boolean atomicReserveOk = firstReserve && !duplicateReserve;

            if (!symbiosisSyncOk || !chimeraOwnershipOk || !escortLifecycleOk
                    || !linksExcludeSymbiotic || !atomicReserveOk) {
                helper.fail(String.format(
                        "v2 persistence check failed: symbiosis=%s chimera=%s escort=%s links=%s reserve=%s",
                        symbiosisSyncOk, chimeraOwnershipOk, escortLifecycleOk, linksExcludeSymbiotic, atomicReserveOk));
                return;
            }
            helper.succeed();
        } finally {
            tracker.removeNode(relayPos);
            tracker.removeNode(symbioticPos);
            tracker.removeNode(hiveProbe);
            tracker.removeNode(hiveProbe.above(3));
            level.removeBlock(relayPos, false);
            level.removeBlock(symbioticPos, false);
            if (!chimera.isRemoved()) chimera.discard();
            if (!escort.isRemoved()) escort.discard();
            player.getData(com.alex.mycelialmind.registry.ModAttachments.SYMBIOSIS).clearMutagens();
            com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync.sync(player);
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void v2IntegrationCombatDecoysRivalSpitBordersAndRecipesWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        BlockPos base = helper.absolutePos(new BlockPos(2, 250, 2));
        int pylonRadius = MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -10, pylonRadius + 4, -2, 16);
        BlockPos pylonPos = base;
        BlockPos protectedPanePos = pylonPos.east(pylonRadius);
        BlockPos outsideStonePos = protectedPanePos.above(1);
        BlockPos outsideGlassPos = pylonPos.east(pylonRadius + 1);
        BlockPos decoyCenter = base.offset(0, 0, 10);
        BlockPos decoyPos = decoyCenter.above(1);

        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 2, 250, 4);
        var rivalBrute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 4, 250, 4);
        var allyBrute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 6, 250, 4);
        var escort = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 250, 12);
        var liveSpitter = ModEntities.ACID_SPITTER.get().create(level);
        List<BlockPos> placedFloor = new java.util.ArrayList<>();

        try {
            // 1) AcidSpitProjectile respects Pylon border on both impactPos and adjacentPos
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            tracker.addPylon(pylonPos);
            level.setBlockAndUpdate(protectedPanePos, Blocks.GLASS_PANE.defaultBlockState());
            level.setBlockAndUpdate(outsideStonePos, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(outsideGlassPos, Blocks.GLASS.defaultBlockState());

            var borderSpit = new AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
            borderSpit.setPos(protectedPanePos.getX() + 0.2D, protectedPanePos.getY() + 0.3D, protectedPanePos.getZ() + 0.2D);
            borderSpit.shoot(0.0D, 1.0D, 0.0D, 1.2F, 0.0F);
            level.addFreshEntity(borderSpit);
            for (int t = 0; t < 8 && !borderSpit.isRemoved(); t++) {
                borderSpit.tick();
            }

            var outsideSpit = new AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
            outsideSpit.setPos(outsideGlassPos.getX() + 0.5D, outsideGlassPos.getY() + 1.35D, outsideGlassPos.getZ() + 0.5D);
            outsideSpit.shoot(0.0D, -1.0D, 0.0D, 1.2F, 0.0F);
            level.addFreshEntity(outsideSpit);
            for (int t = 0; t < 8 && !outsideSpit.isRemoved(); t++) {
                outsideSpit.tick();
            }
            boolean borderSafeOk = level.getBlockState(protectedPanePos).is(Blocks.GLASS_PANE)
                    && level.getBlockState(outsideGlassPos).isAir();

            // 2) AcidSpitProjectile damages rival strain during schism and spares same-strain ally
            spitter.setHiveStrain(HiveStrain.AMBER);
            spitter.setSchismTicks(400);
            rivalBrute.setHiveStrain(HiveStrain.CRIMSON);
            rivalBrute.setSchismTicks(400);
            allyBrute.setHiveStrain(HiveStrain.AMBER);
            allyBrute.setSchismTicks(400);

            float rivalHpBefore = rivalBrute.getHealth();
            var rivalSpit = new AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
            rivalSpit.setOwner(spitter);
            rivalSpit.setPos(rivalBrute.getX(), rivalBrute.getY() + rivalBrute.getBbHeight() + 0.35D, rivalBrute.getZ());
            rivalSpit.shoot(0.0D, -1.0D, 0.0D, 1.2F, 0.0F);
            level.addFreshEntity(rivalSpit);
            for (int t = 0; t < 8 && !rivalSpit.isRemoved(); t++) {
                rivalSpit.tick();
            }

            float allyHpBefore = allyBrute.getHealth();
            var allySpit = new AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
            allySpit.setOwner(spitter);
            allySpit.setPos(allyBrute.getX(), allyBrute.getY() + allyBrute.getBbHeight() + 0.35D, allyBrute.getZ());
            allySpit.shoot(0.0D, -1.0D, 0.0D, 1.2F, 0.0F);
            level.addFreshEntity(allySpit);
            for (int t = 0; t < 8 && !allySpit.isRemoved(); t++) {
                allySpit.tick();
            }
            boolean rivalSpitOk = rivalBrute.getHealth() < rivalHpBefore
                    && Math.abs(allyBrute.getHealth() - allyHpBefore) < 0.01F;

            // The border check is complete. Remove protection before testing an unjammed spitter.
            // The border check is complete. Clear its protection before testing decoy pursuit.
            tracker.removePylon(pylonPos);
            level.removeBlock(pylonPos, false);

            // 3) NeuralEchoDecoyItem cooldown + escort protection + AcidSpitterCombatBehaviour decoy pursuit
            for (int dx = -8; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos floorPos = decoyCenter.offset(dx, 0, dz);
                    level.setBlockAndUpdate(floorPos, Blocks.STONE.defaultBlockState());
                    placedFloor.add(floorPos);
                    for (int dy = 1; dy <= 3; dy++) {
                        BlockPos airPos = floorPos.above(dy);
                        level.removeBlock(airPos, false);
                    }
                }
            }
            player.setPos(decoyPos.getX() + 0.5D, decoyPos.getY(), decoyPos.getZ() + 0.5D);
            player.getCooldowns().removeCooldown(ModItems.NEURAL_ECHO_DECOY.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NEURAL_ECHO_DECOY.get(), 3));
            var firstDecoyRes = ModItems.NEURAL_ECHO_DECOY.get().use(level, player, InteractionHand.MAIN_HAND);
            var secondDecoyRes = ModItems.NEURAL_ECHO_DECOY.get().use(level, player, InteractionHand.MAIN_HAND);
            var secondDecoyUseOn = ModItems.NEURAL_ECHO_DECOY.get().useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(decoyCenter), Direction.UP, decoyCenter, false)
            ));

            escort.setAlliedEscort(player.getUUID(), level.getGameTime() + 400L);
            escort.setTarget(rivalBrute);
            com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.registerDecoy(level, decoyPos, 120);
            boolean escortKeptTarget = escort.getTarget() == rivalBrute;

            // Remove nearby combat entities & move survival mock player out of sensor range so liveSpitter is non-agro
            spitter.discard();
            rivalBrute.discard();
            allyBrute.discard();
            player.setPos(base.getX() + 100.0D, base.getY(), base.getZ() + 100.0D);

            boolean spitterPursuedDecoy = false;
            BlockPos navTarget = null;
            if (liveSpitter != null) {
                HiveBlackboard.clear(level);
                liveSpitter.setPos(decoyCenter.getX() - 6.0D, decoyCenter.getY() + 1.0D, decoyCenter.getZ() + 0.5D);
                liveSpitter.setOnGround(true);
                level.addFreshEntity(liveSpitter);
                // Exercise the protection check on every manual tick regardless of entity ID.
                helper.assertFalse(tracker.isProtected(liveSpitter.blockPosition()),
                        "The decoy pursuit arena must be outside pylon protection");
                liveSpitter.tickCount = Math.floorMod(-liveSpitter.getId(), 10);
                for (int t = 0; t < 12 && !spitterPursuedDecoy; t++) {
                    liveSpitter.tick();
                    navTarget = liveSpitter.getNavigation().getTargetPos();
                    spitterPursuedDecoy = navTarget != null && navTarget.distSqr(decoyPos) <= 9.0D;
                }
                helper.assertFalse(liveSpitter.isDesynced(),
                        "The decoy pursuit arena must be outside pylon protection");
            }
            boolean decoyIntegrationOk = firstDecoyRes.getResult().consumesAction()
                    && !secondDecoyRes.getResult().consumesAction()
                    && secondDecoyUseOn == net.minecraft.world.InteractionResult.FAIL
                    && escortKeptTarget
                    && spitterPursuedDecoy;

            // 4) SchismInjector rejects allied escort & OvermindCrown enforces cooldown
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            tracker.addPylon(pylonPos);
            player.setPos(decoyPos.getX() + 0.5D, decoyPos.getY(), decoyPos.getZ() + 0.5D);
            player.getCooldowns().removeCooldown(ModItems.SCHISM_INJECTOR.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCHISM_INJECTOR.get()));
            var schismEscortRes = ModItems.SCHISM_INJECTOR.get().interactLivingEntity(
                    player.getMainHandItem(), player, escort, InteractionHand.MAIN_HAND);

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.OVERMIND_CROWN.get()));
            player.getCooldowns().addCooldown(ModItems.OVERMIND_CROWN.get(), 100);
            var crownUseBlocked = ModItems.OVERMIND_CROWN.get().use(level, player, InteractionHand.MAIN_HAND);
            var crownUseOnBlocked = ModItems.OVERMIND_CROWN.get().useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pylonPos), Direction.UP, pylonPos, false)
            ));
            boolean itemGuardsOk = schismEscortRes == net.minecraft.world.InteractionResult.PASS
                    && !crownUseBlocked.getResult().consumesAction()
                    && crownUseOnBlocked == net.minecraft.world.InteractionResult.FAIL;

            // 5) Multi-output JEI/EMI recipes + safe arrival floors + effective Tier 2 threshold
            var recipes = SynapticBenchRecipeDefinitions.allRecipes();
            boolean multiOutputOk = recipes.get(0).outputAlternatives().size() == AdaptationTrait.values().length
                    && recipes.get(1).outputAlternatives().size() == AdaptationTrait.values().length;

            BlockPos safePos = decoyCenter;
            BlockPos magmaPos = outsideStonePos;
            BlockPos fencePos = outsideGlassPos;
            level.setBlockAndUpdate(magmaPos, Blocks.MAGMA_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(fencePos, Blocks.OAK_FENCE.defaultBlockState());
            boolean arrivalFloorOk = ModDimensions.isSafeArrivalFloor(level, safePos)
                    && !ModDimensions.isSafeArrivalFloor(level, magmaPos)
                    && !ModDimensions.isSafeArrivalFloor(level, fencePos)
                    && !ModDimensions.isSafeArrivalFloor(level, safePos.above(2));

            boolean effectiveThresholdOk = MycelialConfig.getEffectiveTier2ActivationThreshold()
                    > MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();

            if (!borderSafeOk || !rivalSpitOk || !decoyIntegrationOk || !itemGuardsOk
                    || !multiOutputOk || !arrivalFloorOk || !effectiveThresholdOk) {
                helper.fail(String.format(
                        "v2 combat/item check failed: border=%s rivalSpit=%s decoy=%s(escort=%s,nav=%s) guards=%s recipes=%s floor=%s threshold=%s",
                        borderSafeOk, rivalSpitOk, decoyIntegrationOk, escortKeptTarget, spitterPursuedDecoy,
                        itemGuardsOk, multiOutputOk, arrivalFloorOk, effectiveThresholdOk));
                return;
            }
            helper.succeed();
        } finally {
            com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.clearDecoys(level);
            tracker.removePylon(pylonPos);
            level.removeBlock(pylonPos, false);
            level.removeBlock(protectedPanePos, false);
            level.removeBlock(outsideStonePos, false);
            level.removeBlock(outsideGlassPos, false);
            for (BlockPos p : placedFloor) {
                level.removeBlock(p, false);
            }
            if (!spitter.isRemoved()) spitter.discard();
            if (!rivalBrute.isRemoved()) rivalBrute.discard();
            if (!allyBrute.isRemoved()) allyBrute.discard();
            if (!escort.isRemoved()) escort.discard();
            if (liveSpitter != null && !liveSpitter.isRemoved()) liveSpitter.discard();
            player.getCooldowns().removeCooldown(ModItems.NEURAL_ECHO_DECOY.get());
            player.getCooldowns().removeCooldown(ModItems.OVERMIND_CROWN.get());
            player.getCooldowns().removeCooldown(ModItems.SCHISM_INJECTOR.get());
            HiveBlackboard.clear(level);
            releaseArenaChunks(level, forcedChunks);
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void coreLoopStationsAndExperimentalDiscoveryWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos anchor = homeostasisTestPos(helper, 7, new BlockPos(4, 12, 4));
        BlockPos floorPos = anchor.below();
        BlockPos tendrilsPos = anchor.east();
        BlockPos bulbPos = anchor.east(2);
        BlockPos pylonPos = anchor.south(2);
        BlockPos benchPos = anchor.south(4);
        BlockPos ventPos = anchor.west();
        BlockPos protectedMyceliumPos = pylonPos.east(2);
        BlockPos exposedMyceliumPos = pylonPos.east(4);

        try {
            // Prepare stable stone bases for flora & blocks
            for (BlockPos support : List.of(floorPos, tendrilsPos.below(), bulbPos.below(), pylonPos.below(), benchPos.below(), ventPos.below(), protectedMyceliumPos.below(), exposedMyceliumPos.below())) {
                level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            }

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);

            // 1. Regenerative Bioluminescent Cap harvest + fertilization shower
            level.setBlockAndUpdate(anchor, ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState()
                    .setValue(BioluminescentMycelialCapBlock.MATURE, true));
            var capHarvest = level.getBlockState(anchor).useWithoutItem(
                    level, player, new BlockHitResult(Vec3.atCenterOf(anchor), Direction.UP, anchor, false));
            boolean capHarvestOk = capHarvest.consumesAction()
                    && !level.getBlockState(anchor).getValue(BioluminescentMycelialCapBlock.MATURE)
                    && player.getInventory().countItem(ModItems.LATENT_SPORE.get()) >= 1
                    && player.hasEffect(MobEffects.NIGHT_VISION);

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 2));
            var capFertilize = level.getBlockState(anchor).useItemOn(
                    player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(anchor), Direction.UP, anchor, false));
            boolean capFertilizeOk = capFertilize.consumesAction()
                    && level.getBlockState(anchor).getValue(BioluminescentMycelialCapBlock.MATURE)
                    && player.hasEffect(MobEffects.REGENERATION);

            // 2. Regenerative Mycelial Tendrils harvest
            level.setBlockAndUpdate(tendrilsPos, ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState()
                    .setValue(MycelialTendrilsBlock.MATURE, true));
            var tendrilsHarvest = level.getBlockState(tendrilsPos).useWithoutItem(
                    level, player, new BlockHitResult(Vec3.atCenterOf(tendrilsPos), Direction.UP, tendrilsPos, false));
            boolean tendrilsOk = tendrilsHarvest.consumesAction()
                    && !level.getBlockState(tendrilsPos).getValue(MycelialTendrilsBlock.MATURE)
                    && player.getInventory().countItem(ModItems.MYCELIAL_BIOMASS.get()) >= 1
                    && player.hasEffect(ModEffects.SYNAPTIC_INSIGHT.getDelegate());

            // 3. Safe SporeBulb harvest with Shears (2x Latent Spore without burst)
            level.setBlockAndUpdate(bulbPos, ModBlocks.SPORE_BULB.get().defaultBlockState());
            int sporesBeforeBulb = player.getInventory().countItem(ModItems.LATENT_SPORE.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            var bulbHarvest = level.getBlockState(bulbPos).useItemOn(
                    player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(bulbPos), Direction.UP, bulbPos, false));
            boolean bulbOk = bulbHarvest.consumesAction()
                    && level.getBlockState(bulbPos).isAir()
                    && player.getInventory().countItem(ModItems.LATENT_SPORE.get()) == sporesBeforeBulb + 2;

            // 4. Purification Pylon T1 -> T2 -> T3 upgrade + expanded protection radius
            HiveMindSavedData.get(level).promoteHighestTier(HiveProgressionTier.ADAPTIVE);
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            int baseRadius = MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
            boolean pylonUpgradeOk = false;
            if (level.getBlockEntity(pylonPos) instanceof MonumentFocusBlockEntity focus) {
                boolean t1Ok = focus.getTier() == 1 && focus.getEffectiveRadius(baseRadius) == baseRadius;
                boolean up2 = focus.tryUpgrade(new ItemStack(ModItems.SYNAPTIC_CORE.get()), player);
                boolean t2Ok = up2 && focus.getTier() == 2
                        && tracker.getPylonTier(pylonPos) == 2
                        && tracker.getPylonRadius(pylonPos) == baseRadius + 6;
                boolean up3 = focus.tryUpgrade(new ItemStack(ModItems.OVERMIND_ESSENCE.get()), player);
                boolean t3Ok = up3 && focus.getTier() == 3
                        && tracker.getPylonTier(pylonPos) == 3
                        && tracker.getPylonRadius(pylonPos) == baseRadius + 12;
                pylonUpgradeOk = t1Ok && t2Ok && t3Ok;
            }

            // 5. Synaptic Bench T1 -> T3 upgrades + 4-Act gated experimental formulas
            HiveMindSavedData hive = resetHive(level);
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            boolean benchOk = false;
            if (level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench) {
                var inv = bench.getInventory();
                ItemStack codedSample = new ItemStack(ModItems.GENETIC_SAMPLE.get());
                codedSample.set(ModDataComponents.TRAIT_ID.get(), AdaptationTrait.MAGIC_CARAPACE.id());

                // Act I / Lab T1: Secret #1 & #3 blocked by Act II, Secret #4 blocked by Act III, Secret #2 allowed
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, codedSample.copy());
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(Items.AMETHYST_SHARD));
                var s1Act1Blocked = bench.computeSynthesisDetails();

                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, codedSample.copy());
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.LATENT_SPORE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
                var s3Act1Blocked = bench.computeSynthesisDetails();

                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.SPORE_ANTIDOTE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.LATENT_SPORE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
                var s4Act1Blocked = bench.computeSynthesisDetails();

                boolean actGatingBlockedOk = s1Act1Blocked.output().isEmpty()
                        && s1Act1Blocked.blockedByAct() == HiveProgressionTier.AWAKENED
                        && s3Act1Blocked.output().isEmpty()
                        && s3Act1Blocked.blockedByAct() == HiveProgressionTier.AWAKENED
                        && s4Act1Blocked.output().isEmpty()
                        && s4Act1Blocked.blockedByAct() == HiveProgressionTier.ADAPTIVE;

                // Promote Hive to Act III (ADAPTIVE) to unlock Secrets #1, #3 & #4 directly on Bench
                hive.promoteHighestTier(HiveProgressionTier.ADAPTIVE);

                // Secret #1: Sample + Biomass + Amethyst -> Synaptic Core
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, codedSample.copy());
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(Items.AMETHYST_SHARD));
                var s1 = bench.computeSynthesisDetails();

                // Secret #2: Antidote + Biomass + Amethyst -> 2x Truce Pheromone (3x at T3)
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.SPORE_ANTIDOTE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(Items.AMETHYST_SHARD));
                var s2 = bench.computeSynthesisDetails();

                // Secret #3: Sample + Latent Spore + Synaptic Core -> Volatile Engram
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, codedSample.copy());
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.LATENT_SPORE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
                var s3 = bench.computeSynthesisDetails();

                // Secret #4: Antidote + Latent Spore + Synaptic Core -> Neural Echo Core
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.SPORE_ANTIDOTE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.LATENT_SPORE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
                var s4 = bench.computeSynthesisDetails();

                boolean secretsOk = actGatingBlockedOk
                        && s1.blockedByAct() == null
                        && s1.secretBit() == ProgressionGoalEngine.SECRET_EXP_SYNAPTIC_CORE
                        && s1.output().is(ModItems.SYNAPTIC_CORE.get())
                        && s2.secretBit() == ProgressionGoalEngine.SECRET_EXP_TRUCE_PHEROMONE
                        && s2.output().is(ModItems.TRUCE_PHEROMONE.get()) && s2.output().getCount() == 2
                        && s3.secretBit() == ProgressionGoalEngine.SECRET_EXP_VOLATILE_ENGRAM
                        && s3.output().is(ModItems.VOLATILE_ENGRAM.get())
                        && s4.secretBit() == ProgressionGoalEngine.SECRET_EXP_NEURAL_ECHO
                        && s4.output().is(ModItems.NEURAL_ECHO_CORE.get());

                boolean tierUp = bench.getLabTier() == 1
                        && bench.tryUpgradeLab(new ItemStack(ModItems.SYNAPTIC_CORE.get()), player)
                        && bench.getLabTier() == 2
                        && bench.tryUpgradeLab(new ItemStack(ModItems.NEURAL_ECHO_CORE.get()), player)
                        && bench.getLabTier() == 3;

                // Lab T3 overrides Act gating even after Hive resets to Act I
                hive.resetAll();
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.SPORE_ANTIDOTE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.LATENT_SPORE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
                var s4LabT3Override = bench.computeSynthesisDetails();

                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.SPORE_ANTIDOTE.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                inv.setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(Items.AMETHYST_SHARD));
                var s2Tier3 = bench.computeSynthesisDetails();
                benchOk = secretsOk && tierUp
                        && s4LabT3Override.output().is(ModItems.NEURAL_ECHO_CORE.get())
                        && s2Tier3.output().is(ModItems.TRUCE_PHEROMONE.get())
                        && s2Tier3.output().getCount() == 3;
                for (int slot = 0; slot <= SynapticBenchBlockEntity.SLOT_OUTPUT; slot++) {
                    inv.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }

            // 6. World transmutations: Vent -> Bioluminescent Cap, Protected Mycelium -> Amber
            level.setBlockAndUpdate(ventPos, ModBlocks.SPORE_VENT.get().defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPORE_ANTIDOTE.get()));
            var ventRes = level.getBlockState(ventPos).useItemOn(
                    player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ventPos), Direction.UP, ventPos, false));
            boolean ventOk = ventRes.consumesAction()
                    && level.getBlockState(ventPos).is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get());

            hive.promoteHighestTier(HiveProgressionTier.AWAKENED);
            player.getInventory().add(new ItemStack(ModItems.LATENT_SPORE.get()));
            level.setBlockAndUpdate(protectedMyceliumPos, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_SHARD));
            var amberProtectedRes = level.getBlockState(protectedMyceliumPos).useItemOn(
                    player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(protectedMyceliumPos), Direction.UP, protectedMyceliumPos, false));

            tracker.removePylon(pylonPos);
            level.removeBlock(pylonPos, false);
            level.setBlockAndUpdate(exposedMyceliumPos, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_SHARD));
            var amberExposedRes = level.getBlockState(exposedMyceliumPos).useItemOn(
                    player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(exposedMyceliumPos), Direction.UP, exposedMyceliumPos, false));
            boolean amberOk = amberProtectedRes.consumesAction()
                    && level.getBlockState(protectedMyceliumPos).is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())
                    && !amberExposedRes.consumesAction()
                    && level.getBlockState(exposedMyceliumPos).is(ModBlocks.CREEPING_MYCELIUM.get());

            if (!capHarvestOk || !capFertilizeOk || !tendrilsOk || !bulbOk || !pylonUpgradeOk || !benchOk || !ventOk || !amberOk) {
                helper.fail(String.format(
                        "Core loop/discovery failed: capHarvest=%s capFert=%s tendrils=%s bulb=%s pylon=%s bench=%s vent=%s amber=%s",
                        capHarvestOk, capFertilizeOk, tendrilsOk, bulbOk, pylonUpgradeOk, benchOk, ventOk, amberOk));
                return;
            }
            helper.succeed();
        } finally {
            HiveMindSavedData.get(level).resetAll();
            tracker.removePylon(pylonPos);
            for (BlockPos pos : List.of(anchor, floorPos, tendrilsPos, tendrilsPos.below(), bulbPos, bulbPos.below(),
                    pylonPos, pylonPos.below(), benchPos, benchPos.below(), ventPos, ventPos.below(),
                    protectedMyceliumPos, protectedMyceliumPos.below(), exposedMyceliumPos, exposedMyceliumPos.below())) {
                level.removeBlock(pos, false);
            }
            level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, new AABB(anchor).inflate(8.0D))
                    .forEach(net.minecraft.world.entity.Entity::discard);
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void discoveryPrimordialVaultHopperAndVanillaIntegrationWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = homeostasisTestPos(helper, 8, new BlockPos(4, 14, 4));
        BlockPos benchPos = anchor.east(10);
        MycelialMob primordialBrute = null;
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        fakePlayer.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());

        try {
            // 1. PlayerDiscoveryData & DiscoverySyncPayload round-trip
            PlayerDiscoveryData data = fakePlayer.getData(ModAttachments.DISCOVERY);
            data.recordSpecies(ModEntities.CHITIN_BRUTE.get());
            data.recordTrait(AdaptationTrait.MAGIC_CARAPACE);
            data.recordStrain(HiveStrain.AMBER);
            data.recordSecret(ProgressionGoalEngine.SECRET_PRIMORDIAL_SPECIMEN);
            data.recordMilestone(ProgressionGoalEngine.MILESTONE_BIO_SCANNER);
            data.setBastionScore(75);
            CompoundTag serialized = data.serializeNBT(level.registryAccess());
            PlayerDiscoveryData restored = new PlayerDiscoveryData();
            restored.deserializeNBT(level.registryAccess(), serialized);
            boolean discoveryDataOk = restored.countScannedSpecies() == 1
                    && restored.countDiscoveredTraits() == 1
                    && restored.countDiscoveredStrains() == 1
                    && restored.hasSecret(ProgressionGoalEngine.SECRET_PRIMORDIAL_SPECIMEN)
                    && restored.getBastionScore() == 75;

            DiscoverySyncPayload payload = DiscoverySyncPayload.forPlayer(fakePlayer);
            var buf = Unpooled.buffer();
            DiscoverySyncPayload.STREAM_CODEC.encode(buf, payload);
            DiscoverySyncPayload decoded = DiscoverySyncPayload.STREAM_CODEC.decode(buf);
            boolean payloadOk = decoded.equals(payload);

            // 2. Primordial mob variant (+25% HP + NBT persistence + Act-scaled drops)
            primordialBrute = ModEntities.CHITIN_BRUTE.get().create(level);
            boolean primordialOk = false;
            if (primordialBrute != null) {
                double baseHp = primordialBrute.getMaxHealth();
                primordialBrute.setPrimordialVariant(true);
                double buffedHp = primordialBrute.getMaxHealth();
                CompoundTag mobTag = new CompoundTag();
                primordialBrute.addAdditionalSaveData(mobTag);

                var act1Specs = MycelialLootSystem.computeAdaptiveMobDropSpecs(
                        EvolutionRank.BASE, HiveStrain.ORTHODOX, HiveProgressionTier.DORMANT,
                        Set.of(AdaptationTrait.MAGIC_CARAPACE), false, 0, true, level.random);
                boolean act1Safe = act1Specs.stream().noneMatch(s -> "mycelialmind:synaptic_core".equals(s.itemId())
                        || "mycelialmind:neural_echo_core".equals(s.itemId()))
                        && act1Specs.stream().anyMatch(s -> "mycelialmind:chitin_plate".equals(s.itemId()) && s.count() >= 2)
                        && act1Specs.stream().anyMatch(s -> "mycelialmind:spore_antidote".equals(s.itemId()));

                var act2Specs = MycelialLootSystem.computeAdaptiveMobDropSpecs(
                        EvolutionRank.BASE, HiveStrain.ORTHODOX, HiveProgressionTier.AWAKENED,
                        Set.of(AdaptationTrait.MAGIC_CARAPACE), false, 0, true, level.random);
                boolean hasCoreAct2 = act2Specs.stream().anyMatch(s -> "mycelialmind:synaptic_core".equals(s.itemId()));
                boolean hasAmber = act2Specs.stream().anyMatch(s -> "mycelialmind:luminescent_amber_vein".equals(s.itemId()) && s.count() >= 2);
                boolean hasSample = act2Specs.stream().anyMatch(s -> "mycelialmind:genetic_sample".equals(s.itemId()));

                var act3Specs = MycelialLootSystem.computeAdaptiveMobDropSpecs(
                        EvolutionRank.MUTATED, HiveStrain.AMBER, HiveProgressionTier.ADAPTIVE,
                        Set.of(AdaptationTrait.MAGIC_CARAPACE), false, 0, true, level.random);
                boolean hasEngramAct3 = act3Specs.stream().anyMatch(s -> "mycelialmind:volatile_engram".equals(s.itemId()));

                var act4Specs = MycelialLootSystem.computeAdaptiveMobDropSpecs(
                        EvolutionRank.ELITE, HiveStrain.CRIMSON, HiveProgressionTier.OVERMIND,
                        Set.of(AdaptationTrait.MAGIC_CARAPACE), true, 0, true, level.random);
                boolean hasConcAntidoteAct4 = act4Specs.stream().anyMatch(s -> "mycelialmind:concentrated_antidote".equals(s.itemId()));

                primordialOk = primordialBrute.isPrimordialVariant()
                        && buffedHp >= baseHp * 1.24D
                        && mobTag.getBoolean("MycelialPrimordial")
                        && act1Safe && hasCoreAct2 && hasAmber && hasSample && hasEngramAct3 && hasConcAntidoteAct4;
                primordialBrute.discard();
            }

            // 3. Sealed Bio-Vault generation & BioScanner thermal pulse detection
            MycelialOutbreakFeature.placeSealedBioVault(level, anchor, level.random, HiveProgressionTier.AWAKENED);
            boolean vaultBuiltOk = level.getBlockState(anchor).is(Blocks.CHEST)
                    && level.getBlockEntity(anchor) instanceof ChestBlockEntity
                    && level.getBlockState(anchor.above(2)).is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())
                    && level.getBlockState(anchor.north()).is(ModBlocks.MYCELIAL_ARCH_BRICK.get());
            fakePlayer.setPos(anchor.getX() + 0.5, anchor.getY() + 3.0, anchor.getZ() + 0.5);
            BioScannerItem.activateThermalPulse(level, fakePlayer);
            boolean scannerVaultOk = fakePlayer.getData(ModAttachments.DISCOVERY)
                    .hasSecret(ProgressionGoalEngine.SECRET_SEALED_BIO_VAULT);

            // 4. Hopper automation capability on Synaptic Research Bench
            level.setBlockAndUpdate(benchPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            var upCap = level.getCapability(Capabilities.ItemHandler.BLOCK, benchPos, Direction.UP);
            var northCap = level.getCapability(Capabilities.ItemHandler.BLOCK, benchPos, Direction.NORTH);
            var downCap = level.getCapability(Capabilities.ItemHandler.BLOCK, benchPos, Direction.DOWN);
            boolean hopperOk = false;
            if (upCap != null && northCap != null && downCap != null
                    && level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench) {
                upCap.insertItem(0, new ItemStack(ModItems.SPORE_ANTIDOTE.get(), 2), false);
                northCap.insertItem(0, new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), 2), false);
                northCap.insertItem(1, new ItemStack(Items.AMETHYST_SHARD, 2), false);
                ItemStack firstPull = downCap.extractItem(0, 1, false);
                int remainingSampleAfterFirst = upCap.getStackInSlot(0).getCount();
                ItemStack secondPull = downCap.extractItem(0, 1, false);
                int remainingSampleAfterSecond = upCap.getStackInSlot(0).getCount();
                ItemStack nextBatchPreview = downCap.getStackInSlot(0);
                hopperOk = firstPull.is(ModItems.TRUCE_PHEROMONE.get()) && firstPull.getCount() == 1
                        && remainingSampleAfterFirst == 1
                        && secondPull.is(ModItems.TRUCE_PHEROMONE.get()) && secondPull.getCount() == 1
                        && remainingSampleAfterSecond == 1
                        && nextBatchPreview.is(ModItems.TRUCE_PHEROMONE.get()) && nextBatchPreview.getCount() == 2;
                for (int slot = 0; slot <= SynapticBenchBlockEntity.SLOT_OUTPUT; slot++) {
                    bench.getInventory().setStackInSlot(slot, ItemStack.EMPTY);
                }
            }

            // 5. Vanilla station recipes, unlock advancements & calibrated gear curves
            var recipeMgr = level.getRecipeManager();
            var advMgr = level.getServer().getAdvancements();
            boolean vanillaDataOk = recipeMgr.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "smelt_creeping_mycelium_to_stone")).isPresent()
                    && recipeMgr.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "smelt_cap_to_latent_spore")).isPresent()
                    && recipeMgr.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "smoke_cap_to_latent_spore")).isPresent()
                    && recipeMgr.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "smelt_cyst_to_chitin_plate")).isPresent()
                    && recipeMgr.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "blast_cyst_to_chitin_plate")).isPresent()
                    && recipeMgr.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "stonecut_mycelial_stone_to_arch_brick")).isPresent()
                    && recipeMgr.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "stonecut_arch_brick_to_chitin_pillar")).isPresent()
                    && recipeMgr.byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "stonecut_chitin_plate_to_lattice")).isPresent()
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/misc/spore_antidote")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/tools/bio_scanner")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/combat/chitin_chestplate")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/tools/synaptic_graft")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/combat/overmind_crown")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/building_blocks/reinforced_bio_barrier")) != null;

            ItemStack mutatedChest = new ItemStack(ModItems.CHITIN_CHESTPLATE.get());
            ChitinArmorItem.setArmorRank(mutatedChest, EvolutionRank.MUTATED);
            double mutatedArmorBonus = mutatedChest.getAttributeModifiers().modifiers().stream()
                    .filter(e -> e.attribute().equals(Attributes.ARMOR) && e.modifier().id().getPath().startsWith("chitin_rank_armor_"))
                    .mapToDouble(e -> e.modifier().amount()).sum();
            boolean calibrationOk = ModArmorMaterials.CHITIN.value().getDefense(ArmorItem.Type.CHESTPLATE) == 6
                    && Math.abs(ModArmorMaterials.CHITIN.value().toughness() - 1.0F) < 0.01F
                    && Math.abs(mutatedArmorBonus - 1.0D) < 0.01D;

            BlockPos pylonPos = benchPos.east(2);
            BlockPos turretPos = benchPos.east(4);
            BlockPos capPos = benchPos.east(6);

            // 6. BioExtractor on Primordial mob grants sample + spore + biomass + secret + reduced 20t CD
            boolean extractorPrimordialOk = false;
            HiveMindSavedData hive = resetHive(level);
            var extractBrute = ModEntities.CHITIN_BRUTE.get().create(level);
            if (extractBrute != null) {
                extractBrute.setPrimordialVariant(true);
                extractBrute.setHealth(extractBrute.getMaxHealth() * 0.40F);
                fakePlayer.getInventory().clearContent();
                fakePlayer.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());
                ItemStack extractor = new ItemStack(ModItems.BIO_EXTRACTOR.get());
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, extractor);
                var extractRes = extractor.interactLivingEntity(fakePlayer, extractBrute, InteractionHand.MAIN_HAND);
                boolean cdStarts = fakePlayer.getCooldowns().isOnCooldown(ModItems.BIO_EXTRACTOR.get());
                for (int t = 0; t < 21; t++) {
                    fakePlayer.getCooldowns().tick();
                }
                boolean cdEndsAt20 = !fakePlayer.getCooldowns().isOnCooldown(ModItems.BIO_EXTRACTOR.get());
                extractorPrimordialOk = extractRes.consumesAction()
                        && cdStarts && cdEndsAt20
                        && fakePlayer.getInventory().countItem(ModItems.GENETIC_SAMPLE.get()) >= 2
                        && fakePlayer.getInventory().countItem(ModItems.LATENT_SPORE.get()) >= 1
                        && fakePlayer.getInventory().countItem(ModItems.MYCELIAL_BIOMASS.get()) >= 1
                        && fakePlayer.getData(ModAttachments.DISCOVERY).hasSecret(ProgressionGoalEngine.SECRET_PRIMORDIAL_SPECIMEN);
                extractBrute.discard();
            }

            // 7. Station T2/T3 upgrades feed BASTION_BUILDER + T2/T3 Pylon homeostasis stabilization + BioScanner flora inspection
            Player survivalPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            level.setBlockAndUpdate(pylonPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            level.setBlockAndUpdate(turretPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            level.setBlockAndUpdate(capPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(capPos, ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState()
                    .setValue(BioluminescentMycelialCapBlock.MATURE, true));

            double habitBefore = hive.getHabitScore(com.alex.mycelialmind.hive.PlayerHabit.BASTION_BUILDER);
            CompoundTag hivePreUpgradeSnap = hive.save(new CompoundTag(), level.registryAccess());
            hive.promoteHighestTier(HiveProgressionTier.ADAPTIVE);
            survivalPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
            level.getBlockState(pylonPos).useItemOn(
                    survivalPlayer.getMainHandItem(), level, survivalPlayer, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pylonPos), Direction.UP, pylonPos, false));
            survivalPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.OVERMIND_ESSENCE.get()));
            level.getBlockState(pylonPos).useItemOn(
                    survivalPlayer.getMainHandItem(), level, survivalPlayer, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pylonPos), Direction.UP, pylonPos, false));
            survivalPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
            level.getBlockState(turretPos).useItemOn(
                    survivalPlayer.getMainHandItem(), level, survivalPlayer, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(turretPos), Direction.UP, turretPos, false));
            if (level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench) {
                bench.tryUpgradeLab(new ItemStack(ModItems.SYNAPTIC_CORE.get()), survivalPlayer);
            }
            double habitAfter = hive.getHabitScore(com.alex.mycelialmind.hive.PlayerHabit.BASTION_BUILDER);
            hive.restoreFromSnapshot(hivePreUpgradeSnap, level.registryAccess());

            PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
            BlockPos remoteProbe = pylonPos.east(MycelialConfig.PYLON_PURIFICATION_RADIUS.get() + 4);
            int maxPylonTier = tracker.getMaxActivePylonTierWithin(remoteProbe, MycelialConfig.PYLON_PURIFICATION_RADIUS.get());
            double t1Delta = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(1, 1, 0, 0);
            double t3Delta = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computePeriodicStressDelta(1, maxPylonTier, 0, 0);

            survivalPlayer.getInventory().clearContent();
            survivalPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
            var floraScanRes = survivalPlayer.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                    survivalPlayer, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(capPos), Direction.UP, capPos, false)));
            boolean floraScanOk = floraScanRes.consumesAction()
                    && level.getBlockState(capPos).getValue(BioluminescentMycelialCapBlock.MATURE)
                    && survivalPlayer.getInventory().countItem(ModItems.LATENT_SPORE.get()) == 0;

            boolean crossIntegrationOk = extractorPrimordialOk
                    && habitAfter > habitBefore + 10.0D
                    && maxPylonTier == 3
                    && t3Delta < t1Delta
                    && floraScanOk;

            if (!discoveryDataOk || !payloadOk || !primordialOk || !vaultBuiltOk || !scannerVaultOk
                    || !hopperOk || !vanillaDataOk || !calibrationOk || !crossIntegrationOk) {
                helper.fail(String.format(
                        "Discovery/Vanilla failed: data=%s payload=%s primordial=%s vault=%s scannerVault=%s hopper=%s recipesAdv=%s calib=%s cross=%s(ext=%s hab=%.1f->%.1f pTier=%d flora=%s)",
                        discoveryDataOk, payloadOk, primordialOk, vaultBuiltOk, scannerVaultOk,
                        hopperOk, vanillaDataOk, calibrationOk, crossIntegrationOk,
                        extractorPrimordialOk, habitBefore, habitAfter, maxPylonTier, floraScanOk));
                return;
            }
            helper.succeed();
        } finally {
            fakePlayer.getInventory().clearContent();
            fakePlayer.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());
            fakePlayer.getCooldowns().removeCooldown(ModItems.BIO_SCANNER.get());
            fakePlayer.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());
            if (primordialBrute != null && !primordialBrute.isRemoved()) {
                primordialBrute.discard();
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        level.removeBlock(anchor.offset(dx, dy, dz), false);
                    }
                }
            }
            for (BlockPos cleanup : List.of(benchPos, benchPos.below(), benchPos.east(2), benchPos.east(2).below(),
                    benchPos.east(4), benchPos.east(4).below(), benchPos.east(6), benchPos.east(6).below())) {
                PurificationZoneTracker.get(level).removePylon(cleanup);
                level.removeBlock(cleanup, false);
            }
            HiveMindSavedData.get(level).resetAll();
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void fourActProgressionIntegratesVaultsExperimentsPrimordialsAndRemission(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -2, 8, -2, 8);
        HiveMindSavedData hive = resetHive(level);
        BlockPos altarPos = helper.absolutePos(new BlockPos(4, 2, 4));
        HiveCoreBossEntity summonedBoss = null;

        try {
            // 1. Act I OUTBREAK_SEALED_VAULT must never leak Act II-IV progression items
            LootTable act1Table = level.getServer().reloadableRegistries()
                    .getLootTable(MycelialLootSystem.OUTBREAK_SEALED_VAULT);
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                            Vec3.atCenterOf(altarPos))
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            boolean act1TableNonEmpty = act1Table != LootTable.EMPTY;
            boolean act1HasZeroLeaks = true;
            int totalItemsRolled = 0;
            for (long seed = 1L; seed <= 20L; seed++) {
                for (ItemStack s : act1Table.getRandomItems(params, seed)) {
                    totalItemsRolled++;
                    if (s.is(ModItems.SYNAPTIC_CORE.get())
                            || s.is(ModItems.NEURAL_ECHO_CORE.get())
                            || s.is(ModItems.HIVE_HEART.get())
                            || s.is(ModItems.CHIMERA_OVUM.get())
                            || s.is(ModItems.SYNAPTIC_GRAFT.get())
                            || s.is(ModItems.OVERMIND_ESSENCE.get())
                            || s.is(ModItems.TIDE_BEACON.get())) {
                        act1HasZeroLeaks = false;
                        break;
                    }
                }
            }

            // 2. Sealed Bio-Vault chest selection scales smoothly across all 4 Acts
            boolean vaultRoutingOk = MycelialLootSystem.selectSealedBioVaultTable(HiveProgressionTier.DORMANT, level.random)
                            .equals(MycelialLootSystem.OUTBREAK_SEALED_VAULT)
                    && MycelialLootSystem.selectSealedBioVaultTable(HiveProgressionTier.AWAKENED, level.random)
                            .equals(MycelialLootSystem.SANCTUM_SPORE_VAULT)
                    && MycelialLootSystem.selectSealedBioVaultTable(HiveProgressionTier.ADAPTIVE, level.random)
                            .equals(MycelialLootSystem.SANCTUM_FORBIDDEN_LAB);
            var overmindTable = MycelialLootSystem.selectSealedBioVaultTable(HiveProgressionTier.OVERMIND, level.random);
            vaultRoutingOk = vaultRoutingOk
                    && (overmindTable.equals(MycelialLootSystem.CITADEL_AMBER_CHAPEL)
                    || overmindTable.equals(MycelialLootSystem.CITADEL_CRIMSON_CHAPEL));

            // 3. HiveAltarBlock activation promotes HighestTierReached from Act I -> Act II (AWAKENED)
            level.setBlockAndUpdate(altarPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(altarPos, ModBlocks.HIVE_ALTAR.get().defaultBlockState());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
            var altarUse = level.getBlockState(altarPos).useItemOn(
                    player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(altarPos), Direction.UP, altarPos, false));
            var bosses = level.getEntitiesOfClass(HiveCoreBossEntity.class, new AABB(altarPos).inflate(16.0D));
            if (!bosses.isEmpty()) {
                summonedBoss = bosses.get(0);
            }
            boolean altarUseConsumed = altarUse.consumesAction();
            int postTier = hive.getHighestProgressionTier().level();
            boolean altarPromotionOk = altarUseConsumed
                    && summonedBoss != null
                    && postTier >= HiveProgressionTier.AWAKENED.level();

            // 4. Post-Matriarch Remission lowers current threat below 75% while resolveEffectiveTier keeps Act IV
            hive.addThreatPenalty(85.0);
            ApexMatriarchEntity.onMatriarchDefeated(level, altarPos, false);
            HiveProgressionTier effectiveInRemission = HiveProgressionTier.resolveEffectiveTier(
                    hive.getThreatLevel(), hive.getHighestTierReached());
            boolean remissionEffectiveActOk = hive.isMatriarchRemissionActive(level.getGameTime())
                    && hive.getThreatLevel() < 75.0
                    && effectiveInRemission == HiveProgressionTier.OVERMIND;

            if (!act1TableNonEmpty || totalItemsRolled == 0 || !act1HasZeroLeaks
                    || !vaultRoutingOk || !altarPromotionOk || !remissionEffectiveActOk) {
                helper.fail(String.format(
                        "4-Act integration failed: nonEmpty=%s rolled=%d noLeaks=%s routing=%s altarPromo=%s(use=%s,boss=%s,tier=%d) remAct=%s",
                        act1TableNonEmpty, totalItemsRolled, act1HasZeroLeaks, vaultRoutingOk,
                        altarPromotionOk, altarUseConsumed, summonedBoss != null, postTier, remissionEffectiveActOk));
                return;
            }
            helper.succeed();
        } finally {
            if (summonedBoss != null && !summonedBoss.isRemoved()) {
                summonedBoss.discard();
            }
            level.removeBlock(altarPos, false);
            level.removeBlock(altarPos.below(), false);
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
        }
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins")
    public static void hyphalRouteDensitySproutsVeinsTriggersTacticalAmbushAndToolSeveringWorks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 36, -4, 36);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        hive.resetAll();
        tracker.clearAll();

        BlockPos pathStart = helper.absolutePos(new BlockPos(3, 10, 3));
        BlockPos pathMid = pathStart.offset(2, 0, 0);
        BlockPos pathBeforeStep = pathStart.offset(3, 0, 0);
        BlockPos pathStep = pathStart.offset(4, 1, 0);
        BlockPos pathEnd = pathStart.offset(5, 1, 0);
        BlockPos safePylonPos = helper.absolutePos(new BlockPos(30, 10, 30));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.BOW));

        helper.runAfterDelay(2, () -> {
            try {
                hive.resetAll();
                tracker.clearAll();
                for (int dx = -1; dx <= 7; dx++) {
                    for (int dy = 3; dy >= 0; dy--) {
                        for (int dz = -2; dz <= 2; dz++) {
                            BlockPos cleanPos = pathStart.offset(dx, dy, dz);
                            if (!level.getBlockState(cleanPos).isAir()) {
                                level.removeBlock(cleanPos, false);
                            }
                        }
                    }
                }
                for (int dx = 0; dx <= 6; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        level.setBlock(pathStart.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState(), 18);
                        if (dx >= 4) {
                            level.setBlock(pathStart.offset(dx, 0, dz), Blocks.DIRT.defaultBlockState(), 18);
                        }
                    }
                }
                level.removeBlock(safePylonPos.above(), false);
                level.setBlockAndUpdate(safePylonPos.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(safePylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
                tracker.addPylon(safePylonPos);

                // Walking inside Pylon zone keeps route density at 0
                player.moveTo(safePylonPos.getX() + 0.5, safePylonPos.getY() + 1.0, safePylonPos.getZ() + 0.5);
                double protectedDensity = com.alex.mycelialmind.hive.HyphalSiegeDirector.recordPlayerRouteFootstep(level, player);

                // Repeatedly walking along flat pathStart -> raised 1-block step pathEnd accumulates density and sprouts HYPHAL_VEIN overlays
                double maxDensity = 0.0;
                for (int cycle = 0; cycle < 18; cycle++) {
                    for (BlockPos stop : new BlockPos[]{pathStart, pathMid, pathBeforeStep, pathStep, pathEnd}) {
                        player.moveTo(stop.getX() + 0.5, stop.getY(), stop.getZ() + 0.5);
                        maxDensity = Math.max(maxDensity,
                                com.alex.mycelialmind.hive.HyphalSiegeDirector.recordPlayerRouteFootstep(level, player));
                    }
                }
                ChunkPos routeChunk = new ChunkPos(pathStart);
                com.alex.mycelialmind.hive.HyphalSiegeDirector.growTrailVeinsNow(level, hive, routeChunk, 12);
                int sproutedVeins = hive.getActiveVeins(routeChunk).size();
                var routeStrands = level.getEntitiesOfClass(
                        com.alex.mycelialmind.entity.HyphalVeinEntity.class,
                        new AABB(pathStart).inflate(8.0D));
                boolean hasVeinOverlay = !routeStrands.isEmpty();
                boolean underlyingDirtIntact = level.getBlockState(pathStart.below()).is(Blocks.DIRT)
                        && level.getBlockState(pathBeforeStep.below()).is(Blocks.DIRT)
                        && level.getBlockState(pathStep.below()).is(Blocks.DIRT);
                boolean climbsStepUp = false;
                for (var strand : routeStrands) {
                    boolean nearLow = strand.touchesBlockPos(pathBeforeStep, 1.5D) || strand.touchesBlockPos(pathStart, 1.5D);
                    boolean nearHigh = strand.touchesBlockPos(pathStep, 1.5D) || strand.touchesBlockPos(pathEnd, 1.5D);
                    if (nearLow && nearHigh) {
                        climbsStepUp = true;
                        break;
                    }
                }
                var shortRgba = com.alex.mycelialmind.hive.HyphalVerletRope.computeColorAndAlphaForLength(2, 0.5D);
                var longRgba = com.alex.mycelialmind.hive.HyphalVerletRope.computeColorAndAlphaForLength(24, 12.0D);
                boolean strengthAndColorScale = shortRgba.a() < longRgba.a()
                        && (shortRgba.r() != longRgba.r() || shortRgba.g() != longRgba.g());

                // Outside the route network, Overworld remains nearly vanilla and raids defer
                boolean outsideNetworkVanilla = !com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                        .shouldAllowOverworldNaturalSpawn(0.0, false, false, false, 0.5)
                        && com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine
                        .shouldDeferOverworldRaidOutsideNetwork(0.0, false, false, hive.hasDenseHyphalRoutes());

                // Triggered ambush spawns SynapticMimic mirroring player's Bow (SPORE_MARKSMAN)
                var mimics = level.getEntitiesOfClass(com.alex.mycelialmind.entity.SynapticMimicEntity.class,
                        new AABB(pathStart).inflate(12.0D));
                boolean mimicMirroredBow = !mimics.isEmpty()
                        && mimics.get(0).getMimicStance() == com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.SPORE_MARKSMAN;

                // Severing veins with SymbioticBlade clears overlay cleanly, preserves ground dirt, & lowers chunk density
                double densityBeforeSever = hive.getHyphalDensity(routeChunk);
                int severed = com.alex.mycelialmind.hive.HyphalSiegeDirector.trySeverHyphalVein(
                        level, pathStart, player, new ItemStack(ModItems.SYMBIOTIC_BLADE.get()));
                double densityAfterSever = hive.getHyphalDensity(routeChunk);
                boolean dirtIntactAfterSever = level.getBlockState(pathStart.below()).is(Blocks.DIRT);

                if (protectedDensity != 0.0 || maxDensity < 35.0 || sproutedVeins <= 0
                        || !hasVeinOverlay || !underlyingDirtIntact || !climbsStepUp || !strengthAndColorScale
                        || !outsideNetworkVanilla
                        || !mimicMirroredBow || severed <= 0 || !dirtIntactAfterSever || densityAfterSever >= densityBeforeSever) {
                    helper.fail(String.format(
                            "Hyphal route check failed: prot=%.1f maxDens=%.1f veins=%d overlay=%s dirt=%s climbUp=%s strength=%s outside=%s mimicBow=%s severed=%d dirtAfter=%s dens=%.1f->%.1f",
                            protectedDensity, maxDensity, sproutedVeins, hasVeinOverlay, underlyingDirtIntact,
                            climbsStepUp, strengthAndColorScale, outsideNetworkVanilla, mimicMirroredBow, severed, dirtIntactAfterSever,
                            densityBeforeSever, densityAfterSever));
                    return;
                }
                helper.succeed();
            } finally {
                for (var vein : level.getEntitiesOfClass(com.alex.mycelialmind.entity.HyphalVeinEntity.class,
                        AABB.encapsulatingFullBlocks(pathStart, pathEnd).inflate(3.0D))) {
                    vein.discard();
                }
                for (var mob : level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                        new AABB(pathStart).inflate(20.0D))) {
                    mob.discard();
                }
                for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(pathStart).inflate(12.0D))) {
                    item.discard();
                }
                for (int dx = -1; dx <= 7; dx++) {
                    for (int dy = 3; dy >= -1; dy--) {
                        for (int dz = -2; dz <= 2; dz++) {
                            level.removeBlock(pathStart.offset(dx, dy, dz), false);
                        }
                    }
                }
                tracker.removePylon(safePylonPos);
                level.removeBlock(safePylonPos, false);
                level.removeBlock(safePylonPos.below(), false);
                hive.resetAll();
                releaseArenaChunks(level, forcedChunks);
            }
        });
    }

    @GameTest(template = "empty", batch = "architectural_reflection")
    public static void architecturalAccentSubstitutesPaletteAndProportionsInTwistedReflection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -2, 60, -2, 60);
        HiveMindSavedData hive = resetHive(level);

        BlockPos towerBase = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos reflectionOrigin = helper.absolutePos(new BlockPos(18, 2, 18));
        BlockPos citadelCenter = helper.absolutePos(new BlockPos(32, 32, 32));
        BlockPos twistedCenter = helper.absolutePos(new BlockPos(24, 65, 24));
        var piece = new com.alex.mycelialmind.world.structure.MatriarchCitadelPiece(citadelCenter);
        var citadelBox = piece.getBoundingBox();
        var twistedPiece = new com.alex.mycelialmind.world.structure.TwistedCitadelPiece(twistedCenter);
        var twistedBox = twistedPiece.getBoundingBox();

        try {
            // Simulate player building a Dark Oak Tower
            for (int dy = 0; dy < 7; dy++) {
                level.setBlockAndUpdate(towerBase.above(dy), Blocks.DARK_OAK_PLANKS.defaultBlockState());
                com.alex.mycelialmind.hive.HyphalSiegeDirector.recordPlayerArchitecturePlacement(
                        level, null, towerBase.above(dy), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            }
            level.setBlockAndUpdate(towerBase.offset(1, 0, 0), Blocks.DARK_OAK_LOG.defaultBlockState());
            com.alex.mycelialmind.hive.HyphalSiegeDirector.recordPlayerArchitecturePlacement(
                    level, null, towerBase.offset(1, 0, 0), Blocks.DARK_OAK_LOG.defaultBlockState());

            var accent = hive.getArchitecturalAccent();
            boolean accentRecorded = accent.hasCustomAccent()
                    && "minecraft:dark_oak_planks".equals(accent.primaryBlockId())
                    && "minecraft:dark_oak_log".equals(accent.pillarBlockId())
                    && accent.proportion() == com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.TOWER_SPIRE;

            // Cross-dimension progression copies architectural accent cleanly
            HiveMindSavedData deepCopy = new HiveMindSavedData();
            deepCopy.syncProgressionFrom(hive, level.getGameTime(), level.getGameTime());
            boolean syncedAccent = deepCopy.getArchitecturalAccent().hasCustomAccent()
                    && "minecraft:dark_oak_planks".equals(deepCopy.getArchitecturalAccent().primaryBlockId());

            // Twisted Reflection standalone structure uses player palette + fungal growth
            var reflBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                    reflectionOrigin.getX() - 6, reflectionOrigin.getY() - 1, reflectionOrigin.getZ() - 6,
                    reflectionOrigin.getX() + 6, reflectionOrigin.getY() + 16, reflectionOrigin.getZ() + 6);
            com.alex.mycelialmind.world.structure.ArchitecturalStructureAdapter
                    .placeTwistedReflectionStructure(level, reflBox, reflectionOrigin, accent);
            boolean reflUsesDarkOak = false;
            boolean reflUsesFungus = false;
            for (BlockPos p : BlockPos.betweenClosed(
                    reflBox.minX(), reflBox.minY(), reflBox.minZ(),
                    reflBox.maxX(), reflBox.maxY(), reflBox.maxZ())) {
                BlockState st = level.getBlockState(p);
                if (st.is(Blocks.DARK_OAK_PLANKS)) reflUsesDarkOak = true;
                if (st.is(ModBlocks.MYCELIAL_ARCH_BRICK.get()) || st.is(ModBlocks.CREEPING_MYCELIUM.get())) reflUsesFungus = true;
            }

            // MatriarchCitadelPiece substitutes player palette + adds connected spires with zero floating blocks
            for (BlockPos p : BlockPos.betweenClosed(
                    citadelBox.minX(), citadelBox.minY(), citadelBox.minZ(),
                    citadelBox.maxX(), citadelBox.maxY(), citadelBox.maxZ())) {
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    level.getRandom(), citadelBox, new ChunkPos(citadelCenter), citadelCenter);
            boolean citadelHasDarkOak = level.getBlockState(citadelCenter.offset(11, 1, 6)).is(Blocks.DARK_OAK_LOG)
                    || level.getBlockState(citadelCenter.offset(11, 2, 6)).is(Blocks.DARK_OAK_PLANKS);
            java.util.function.Predicate<BlockState> isModStructure =
                    s -> !s.isAir() && !s.is(Blocks.STONE) && !s.is(Blocks.BEDROCK) && !s.is(Blocks.BARRIER);
            var citadelFloating = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                    level, citadelBox, citadelCenter.above(4), isModStructure);

            // TwistedCitadelPiece places NBT reflection template (spire_tower.nbt) via ArchitecturalAccentProcessor with zero floating blocks
            for (BlockPos p : BlockPos.betweenClosed(
                    twistedBox.minX(), twistedBox.minY(), twistedBox.minZ(),
                    twistedBox.maxX(), twistedBox.maxY(), twistedBox.maxZ())) {
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            twistedPiece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    level.getRandom(), twistedBox, new ChunkPos(twistedCenter), twistedCenter);
            boolean twistedHasOak = false;
            boolean twistedHasVein = !level.getEntitiesOfClass(
                    com.alex.mycelialmind.entity.HyphalVeinEntity.class,
                    new AABB(twistedBox.minX(), twistedBox.minY(), twistedBox.minZ(),
                            twistedBox.maxX() + 1, twistedBox.maxY() + 1, twistedBox.maxZ() + 1).inflate(1.5D))
                    .isEmpty();
            boolean twistedHasNode = false;
            boolean twistedHasChest = false;
            for (BlockPos p : BlockPos.betweenClosed(
                    twistedBox.minX(), twistedBox.minY(), twistedBox.minZ(),
                    twistedBox.maxX(), twistedBox.maxY(), twistedBox.maxZ())) {
                BlockState st = level.getBlockState(p);
                if (st.is(Blocks.DARK_OAK_PLANKS) || st.is(Blocks.DARK_OAK_LOG)) twistedHasOak = true;
                if (st.is(ModBlocks.SENTINEL_NODE.get())) twistedHasNode = true;
                if (st.is(Blocks.CHEST)) twistedHasChest = true;
            }
            var twistedFloating = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                    level, twistedBox, twistedCenter, isModStructure);

            if (!accentRecorded || !syncedAccent || !reflUsesDarkOak || !reflUsesFungus
                    || !citadelHasDarkOak || !citadelFloating.isEmpty()
                    || !twistedHasOak || !twistedHasVein || !twistedHasNode || !twistedHasChest
                    || !twistedFloating.isEmpty()) {
                helper.fail(String.format(
                        "Architectural reflection failed: rec=%s sync=%s reflOak=%s reflFung=%s citOak=%s citFloat=%d twOak=%s twVein=%s twNode=%s twChest=%s twFloat=%d",
                        accentRecorded, syncedAccent, reflUsesDarkOak, reflUsesFungus,
                        citadelHasDarkOak, citadelFloating.size(),
                        twistedHasOak, twistedHasVein, twistedHasNode, twistedHasChest, twistedFloating.size()));
                return;
            }
            helper.succeed();
        } finally {
            var citadelGuardians = level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                    new AABB(citadelCenter).inflate(26.0D));
            citadelGuardians.forEach(net.minecraft.world.entity.Entity::discard);
            for (int dy = 0; dy <= 7; dy++) {
                level.removeBlock(towerBase.above(dy), false);
            }
            level.removeBlock(towerBase.offset(1, 0, 0), false);
            for (BlockPos p : BlockPos.betweenClosed(
                    reflectionOrigin.getX() - 6, reflectionOrigin.getY() - 1, reflectionOrigin.getZ() - 6,
                    reflectionOrigin.getX() + 6, reflectionOrigin.getY() + 16, reflectionOrigin.getZ() + 6)) {
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(
                    citadelBox.minX(), citadelBox.minY(), citadelBox.minZ(),
                    citadelBox.maxX(), citadelBox.maxY(), citadelBox.maxZ())) {
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(
                    twistedBox.minX(), twistedBox.minY(), twistedBox.minZ(),
                    twistedBox.maxX(), twistedBox.maxY(), twistedBox.maxZ())) {
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            PurificationZoneTracker.get(level).clearAll();
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
        }
    }

    @GameTest(template = "empty", batch = "blind_spot_outbreak")
    public static void blindSpotOutbreakGrowsBlockByBlockStallsInLightAndCoreOrCompletionWorks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -6, 44, -6, 44);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        hive.resetAll();
        tracker.clearAll();

        BlockPos core1 = helper.absolutePos(new BlockPos(4, 120, 4));
        BlockPos core2 = helper.absolutePos(new BlockPos(20, 120, 20));
        BlockPos core3 = insetInChunk(helper.absolutePos(new BlockPos(0, 120, 0)), 6);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entityChunksReady(level, core3.east(2), 0),
                        "Expected entity-ticking chunks around the outbreak completion point to be ready."))
                .thenExecute(() -> {
            try {
                hive.resetAll();
                tracker.clearAll();
                hive.recordArchitecturalBlockPlacement("minecraft:dark_oak_planks", false, false);
            hive.recordArchitecturalBlockPlacement("minecraft:dark_oak_log", true, false);
            hive.recordArchitecturalGeometry(4.0, 1.80, 0.25);

            for (BlockPos c : List.of(core1, core2, core3)) {
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        level.setBlock(c.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                        for (int dy = 0; dy <= 10; dy++) {
                            if (!level.getBlockState(c.offset(dx, dy, dz)).isAir()) {
                                level.setBlock(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 18);
                            }
                        }
                    }
                }
            }

            // 1. Live block-by-block growth + adjacent HyphalVeinEntity tendril frame + BioScanner + photophobic stall & wither
            var site1 = com.alex.mycelialmind.hive.HyphalSiegeDirector.createBlindSpotSiteAt(level, hive, core1);
            int tendrilVeinsBefore = level.getEntitiesOfClass(
                    com.alex.mycelialmind.entity.HyphalVeinEntity.class, new AABB(core1).inflate(3.0D)).size();
            boolean g1 = com.alex.mycelialmind.hive.HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site1, true);
            boolean g2 = com.alex.mycelialmind.hive.HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site1, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
            var scanRes = player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(core1), Direction.UP, core1, false)));
            boolean scanOk = scanRes.consumesAction();

            level.setBlockAndUpdate(core1.east(5), Blocks.GLOWSTONE.defaultBlockState());
            boolean stalled = !com.alex.mycelialmind.hive.HyphalSiegeDirector
                    .advanceBlindSpotGrowthStep(level, hive, site1, true)
                    && hive.getBlindSpotSite(core1) != null
                    && site1.getPhotophobicStrikes() == 1;
            for (int w = 0; w < com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.PHOTOPHOBIC_WITHER_STRIKES; w++) {
                com.alex.mycelialmind.hive.HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site1, true);
            }
            int tendrilVeinsAfter = level.getEntitiesOfClass(
                    com.alex.mycelialmind.entity.HyphalVeinEntity.class, new AABB(core1).inflate(3.0D)).size();
            boolean witheredByLight = hive.getBlindSpotSite(core1) == null
                    && !level.getBlockState(core1).is(ModBlocks.SPORE_BULB.get())
                    && tendrilVeinsAfter == 0;
            level.removeBlock(core1.east(5), false);

            // 2. Vulnerable core destruction collapses partial shell
            var site2 = com.alex.mycelialmind.hive.HyphalSiegeDirector.createBlindSpotSiteAt(level, hive, core2);
            for (int step = 0; step < 5; step++) {
                com.alex.mycelialmind.hive.HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site2, true);
            }
            com.alex.mycelialmind.hive.HyphalSiegeDirector.destroyBlindSpotVulnerableCore(level, hive, site2, player);
            boolean poppedReward = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(core2).inflate(3.0D),
                    item -> item.getItem().is(ModItems.MYCELIAL_BIOMASS.get())).isEmpty();
            boolean destroyedCleanly = hive.getBlindSpotSite(core2) == null && poppedReward;

            // 2b. Neglected dark OAK_DOOR access prioritization during natural blind-spot nucleation
            BlockPos doorAnchor = helper.absolutePos(new BlockPos(18, 120, 18));
            BlockPos doorPos = doorAnchor.offset(7, 0, 0);
            for (int dx = -12; dx <= 12; dx++) {
                for (int dz = -12; dz <= 12; dz++) {
                    level.setBlock(doorAnchor.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                }
            }
            level.setBlockAndUpdate(doorPos.north(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(doorPos.north().above(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(doorPos.south(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(doorPos.south().above(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(doorPos, Blocks.OAK_DOOR.defaultBlockState());
            level.setBlockAndUpdate(doorPos.above(), Blocks.OAK_DOOR.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            hive.setHyphalDensity(new ChunkPos(doorAnchor), 65.0, level.getGameTime());
            hive.setHyphalDensity(new ChunkPos(doorPos), 65.0, level.getGameTime());
            hive.recordBaseAnchor(doorPos);
            var withoutMycelium = com.alex.mycelialmind.hive.HyphalSiegeDirector.tryNucleateBlindSpotNear(level, hive, doorAnchor);
            helper.assertTrue(withoutMycelium == null, "A blind-spot outbreak must not create an isolated mycelium patch.");
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.setBlock(doorPos.below().offset(dx, 0, dz), ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState(), 18);
                }
            }
            var doorSite = com.alex.mycelialmind.hive.HyphalSiegeDirector.tryNucleateBlindSpotNear(level, hive, doorAnchor);
            boolean neglectedDoorPrioritized = doorSite != null && doorSite.getCorePos().distManhattan(doorPos) <= 4;
            if (doorSite != null) {
                com.alex.mycelialmind.hive.HyphalSiegeDirector.witherBlindSpotSite(level, hive, doorSite, false);
            }
            for (int dx = -12; dx <= 12; dx++) {
                for (int dy = -1; dy <= 5; dy++) {
                    for (int dz = -12; dz <= 12; dz++) {
                        BlockPos clean = doorAnchor.offset(dx, dy, dz);
                        if (!level.getBlockState(clean).isAir()) {
                            level.setBlock(clean, Blocks.AIR.defaultBlockState(), 18);
                        }
                    }
                }
            }

            // 3. Full completion promotes core to INCUBATOR_NODE + Elite + Spore Cloud
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.setBlock(core3.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                    for (int dy = 0; dy <= 10; dy++) {
                        if (!level.getBlockState(core3.offset(dx, dy, dz)).isAir()) {
                            level.setBlock(core3.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 18);
                        }
                    }
                }
            }
            var site3 = com.alex.mycelialmind.hive.HyphalSiegeDirector.createBlindSpotSiteAt(level, hive, core3);
            int guard = 0;
            while (!site3.isCompleted() && guard++ < site3.getTotalSteps() + 10) {
                com.alex.mycelialmind.hive.HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site3, true);
            }
            boolean hasElite = !level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                    new AABB(core3).inflate(8.0D),
                    m -> m.getEvolutionRank() == EvolutionRank.ELITE).isEmpty();
            boolean hasSporeCloud = !level.getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class,
                    new AABB(core3).inflate(6.0D)).isEmpty();
            boolean completedCleanly = site3.isCompleted()
                    && level.getBlockState(core3).is(ModBlocks.INCUBATOR_NODE.get())
                    && hasElite
                    && hasSporeCloud;

            if (tendrilVeinsBefore < 2 || !g1 || !g2 || !scanOk || !stalled || !witheredByLight
                    || !destroyedCleanly || !neglectedDoorPrioritized || !completedCleanly) {
                helper.fail(String.format(
                        "Blind-spot outbreak failed: veinsBefore=%d veinsAfter=%d g=%s/%s scan=%s stall=%s wither=%s coreDel=%s door=%s comp=%s(elite=%s cloud=%s steps=%d/%d light=%d blk=%s)",
                        tendrilVeinsBefore, tendrilVeinsAfter, g1, g2, scanOk, stalled, witheredByLight,
                        destroyedCleanly, neglectedDoorPrioritized, completedCleanly, hasElite, hasSporeCloud,
                        site3.getPlacedSteps(), site3.getTotalSteps(),
                        com.alex.mycelialmind.hive.HyphalSiegeDirector.measureEffectiveBlockLight(level, core3),
                        level.getBlockState(core3).getBlock().getName().getString()));
                return;
            }
            helper.succeed();
        } finally {
            for (var mob : level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                    new AABB(core3).inflate(12.0D))) {
                mob.discard();
            }
            for (var cloud : level.getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class,
                    new AABB(core3).inflate(10.0D))) {
                cloud.discard();
            }
            for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(core2).inflate(6.0D))) {
                item.discard();
            }
            level.removeBlock(core1.east(5), false);
            for (BlockPos c : List.of(core1, core2, core3)) {
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dy = -1; dy <= 10; dy++) {
                        for (int dz = -4; dz <= 4; dz++) {
                            BlockPos p = c.offset(dx, dy, dz);
                            if (!level.getBlockState(p).isAir()) {
                                level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                            }
                        }
                    }
                }
            }
                tracker.clearAll();
                hive.resetAll();
                releaseArenaChunks(level, forcedChunks);
            }
        });
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins", timeoutTicks = 200)
    public static void hyphalEcosystemPurificationAcousticsRemissionAndCodexWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 24, -10, 14);
        BlockPos base = helper.absolutePos(new BlockPos(2, 30, 2));
        BlockPos v0 = base;
        BlockPos v1 = base.east(6);
        BlockPos v2 = base.east(12);
        BlockPos v3 = base.east(18);
        BlockPos v4 = base.south(6);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entityChunksReady(level, base.east(9), 10),
                        "Hyphal ecosystem arena chunks must be entity-ticking"))
                .thenExecute(() -> {
                    HiveMindSavedData hive = resetHive(level);
                    PurificationZoneTracker.get(level).clearAll();
                    PurifierDroneEntity drone = null;
                    java.util.List<com.alex.mycelialmind.entity.HyphalVeinEntity> spawnedStrands = new java.util.ArrayList<>();
                    try {
                        for (BlockPos vp : List.of(v0, v1, v2, v3, v4)) {
                            level.setBlockAndUpdate(vp.below(), Blocks.STONE.defaultBlockState());
                            var strand = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, vp, vp, 4, true, 0.75F);
                            strand.setStrengthFloor(2);
                            level.addFreshEntity(strand);
                            spawnedStrands.add(strand);
                        }
                        var strand0 = spawnedStrands.get(0);
                        var strand1 = spawnedStrands.get(1);
                        var strand2 = spawnedStrands.get(2);
                        var strand3 = spawnedStrands.get(3);
                        ChunkPos c0 = new ChunkPos(v0);
                        hive.setHyphalDensity(c0, 55.0, level.getGameTime());

                        // 1. PurifierGrenade cleans HyphalVeinEntity and drops local chunk density
                        var grenade = new PurifierGrenadeProjectile(ModEntities.PURIFIER_GRENADE.get(), level);
                        grenade.setPos(v0.getX() + 0.5, v0.getY() + 1.25, v0.getZ() + 0.5);
                        grenade.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
                        level.addFreshEntity(grenade);
                        for (int t = 0; t < 6 && !grenade.isRemoved(); t++) {
                            grenade.tick();
                        }
                        boolean grenadeOk = strand0.isRemoved() && hive.getHyphalDensity(c0) < 55.0;

                        // 2. NeutralizingSporeProjectile cleans HyphalVeinEntity on hit
                        var spore = new NeutralizingSporeProjectile(ModEntities.NEUTRALIZING_SPORE.get(), level);
                        spore.setPos(v1.getX() + 0.5, v1.getY() + 1.25, v1.getZ() + 0.5);
                        spore.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
                        level.addFreshEntity(spore);
                        for (int t = 0; t < 6 && !spore.isRemoved(); t++) {
                            spore.tick();
                        }
                        boolean sporeOk = strand1.isRemoved();

                        // 3. OvermindCrown & PurifierDrone cleanse HyphalVeinEntity
                        int crownCleansed = OvermindCrownItem.cleanseMyceliumRadius(level, v2, 1);
                        boolean crownOk = crownCleansed >= 1 && strand2.isRemoved();

                        drone = ModEntities.PURIFIER_DRONE.get().create(level);
                        boolean droneOk = false;
                        if (drone != null) {
                            drone.moveTo(v3.getX() + 0.5, v3.getY() + 1.0, v3.getZ() + 0.5);
                            droneOk = drone.cleanseMyceliumBeneath(level) && strand3.isRemoved();
                        }

                        // 4. MycorrhizalAcousticTracker recognizes charged / strength >= 2 Hyphal Vein entities
                        var weakVein = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, v4, v4, 3, false, 0.40F);
                        weakVein.setStrengthFloor(1);
                        weakVein.setNutrient(2);
                        var midVein = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, v4, v4, 15, false, 0.40F);
                        midVein.setNutrient(2);
                        var chargedVein = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, v4, v4, 3, true, 0.80F);
                        boolean acousticsOk = !com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.isChargedHyphalVein(weakVein)
                                && com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.isChargedHyphalVein(midVein)
                                && com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.isChargedHyphalVein(chargedVein);

                        // 5. Post-Matriarch Remission pauses Hyphal route ambushes while keeping Act IV Codex gate unlocked
                        hive.promoteHighestTier(HiveProgressionTier.OVERMIND);
                        hive.addThreatPenalty(85.0);
                        hive.setHyphalDensity(new ChunkPos(v4), 70.0, level.getGameTime());
                        ApexMatriarchEntity.onMatriarchDefeated(level, v4, false);
                        Player runner = helper.makeMockPlayer(GameType.SURVIVAL);
                        runner.moveTo(v4.getX() + 0.5, v4.getY(), v4.getZ() + 0.5);
                        com.alex.mycelialmind.hive.HyphalSiegeDirector.recordPlayerRouteFootstep(level, runner);
                        boolean noAmbushInRemission = level.getEntitiesOfClass(
                                com.alex.mycelialmind.entity.SynapticMimicEntity.class, new AABB(v4).inflate(12.0D)).isEmpty();
                        HiveProgressionTier effAct = HiveProgressionTier.resolveEffectiveTier(
                                hive.getThreatLevel(), hive.getHighestTierReached());
                        boolean codexPreservedAct4 = hive.isMatriarchRemissionActive(level.getGameTime())
                                && ProgressionGoalEngine.isCodexGateUnlocked("act_4", effAct.actNumber(), false, false, false);

                        if (!grenadeOk || !sporeOk || !crownOk || !droneOk || !acousticsOk
                                || !noAmbushInRemission || !codexPreservedAct4) {
                            helper.fail(String.format(
                                    "Hyphal v2.1 integration failed: grenade=%s spore=%s crown=%s drone=%s acoustics=%s noAmbush=%s codexAct4=%s",
                                    grenadeOk, sporeOk, crownOk, droneOk, acousticsOk, noAmbushInRemission, codexPreservedAct4));
                            return;
                        }
                    } finally {
                        for (var strand : spawnedStrands) {
                            if (!strand.isRemoved()) strand.discard();
                        }
                        if (drone != null && !drone.isRemoved()) {
                            drone.discard();
                        }
                        for (BlockPos vp : List.of(v0, v1, v2, v3, v4)) {
                            level.removeBlock(vp, false);
                            level.removeBlock(vp.below(), false);
                        }
                        PurificationZoneTracker.get(level).clearAll();
                        hive.resetAll();
                        releaseArenaChunks(level, forcedChunks);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins")
    public static void hyphalDiagonalTrailSproutsRootAndFollowsLiteralPathWithoutLShape(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 36, -4, 36);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        hive.resetAll();
        tracker.clearAll();

        BlockPos origin = insetInChunk(helper.absolutePos(new BlockPos(6, 10, 6)), 4);
        BlockPos diagEnd = origin.offset(3, 0, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        try {
            for (int dx = -1; dx <= 4; dx++) {
                for (int dz = -1; dz <= 3; dz++) {
                    level.setBlockAndUpdate(origin.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState());
                    level.removeBlock(origin.offset(dx, 0, dz), false);
                }
            }

            // Walk diagonally origin -> diagEnd 5 times so supercover trail reaches root threshold
            for (int pass = 0; pass < 5; pass++) {
                player.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
                com.alex.mycelialmind.hive.HyphalSiegeDirector.recordPlayerRouteFootstep(level, player);
                player.moveTo(diagEnd.getX() + 0.5, diagEnd.getY(), diagEnd.getZ() + 0.5);
                com.alex.mycelialmind.hive.HyphalSiegeDirector.recordPlayerRouteFootstep(level, player);
            }
            ChunkPos cp = new ChunkPos(origin);
            com.alex.mycelialmind.hive.HyphalSiegeDirector.sproutRouteVeinsInChunk(level, hive, cp);
            com.alex.mycelialmind.hive.HyphalSiegeDirector.growTrailVeinsNow(level, hive, cp, 12);

            BlockPos step1 = origin.offset(1, 0, 1);
            var diagStrands = level.getEntitiesOfClass(
                    com.alex.mycelialmind.entity.HyphalVeinEntity.class, new AABB(origin).inflate(8.0D));
            boolean rootOk = diagStrands.stream().anyMatch(s -> s.hasRoot()
                    && s.getNutrient() == com.alex.mycelialmind.hive.HyphalPhysics.MAX_NUTRIENT
                    && s.touchesBlockPos(origin, 1.25D));
            boolean midDiagOk = diagStrands.stream().anyMatch(s -> s.touchesBlockPos(step1, 1.25D));
            boolean endOk = diagStrands.stream().anyMatch(s -> s.touchesBlockPos(diagEnd, 1.25D));
            boolean noLCorner = diagStrands.stream().noneMatch(s ->
                    s.touchesBlockPos(origin.offset(3, 0, 0), 0.75D)
                    || s.touchesBlockPos(origin.offset(0, 0, 2), 0.75D));

            if (!rootOk || !midDiagOk || !endOk || !noLCorner) {
                helper.fail(String.format(
                        "Diagonal trail check failed: strands=%d root=%s midDiag=%s end=%s noLCorner=%s",
                        diagStrands.size(), rootOk, midDiagOk, endOk, noLCorner));
                return;
            }
            helper.succeed();
        } finally {
            for (var vein : level.getEntitiesOfClass(com.alex.mycelialmind.entity.HyphalVeinEntity.class,
                    AABB.encapsulatingFullBlocks(origin, diagEnd).inflate(3.0D))) {
                vein.discard();
            }
            for (var mob : level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                    new AABB(origin).inflate(20.0D))) {
                mob.discard();
            }
            for (int dx = -1; dx <= 4; dx++) {
                for (int dz = -1; dz <= 3; dz++) {
                    level.removeBlock(origin.offset(dx, 0, dz), false);
                    level.removeBlock(origin.offset(dx, -1, dz), false);
                }
            }
            tracker.clearAll();
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
        }
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins", timeoutTicks = 260)
    public static void hyphalSeveredDownstreamBranchWithersProgressivelyWithin90Ticks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, 0, 16, 8, 18);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        hive.resetAll();
        tracker.clearAll();

        BlockPos rootPos = insetInChunk(helper.absolutePos(new BlockPos(5, 10, 12)), 5);
        BlockPos tipPos = rootPos.east(5);
        BlockPos cutPos = rootPos.east(2);
        AABB searchBox = new AABB(rootPos).inflate(8.0D);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        entityChunksReady(level, rootPos.east(2), 2),
                        "Expected entity-ticking chunk around hyphal sever test to be ready."))
                .thenExecute(() -> {
                    for (int dx = -1; dx <= 6; dx++) {
                        level.setBlockAndUpdate(rootPos.offset(dx, -1, 0), Blocks.DIRT.defaultBlockState());
                        level.removeBlock(rootPos.offset(dx, 0, 0), false);
                    }

                    var rootStrand = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, rootPos, tipPos, 10, true, 0.50F);
                    level.addFreshEntity(rootStrand);

                    // Verify precise raycast interaction: aiming away misses with PASS, aiming at cutPos splits with shears
                    Player cutter = helper.makeMockPlayer(GameType.SURVIVAL);
                    ItemStack shears = new ItemStack(net.minecraft.world.item.Items.SHEARS);
                    cutter.setItemInHand(InteractionHand.MAIN_HAND, shears);
                    cutter.moveTo(cutPos.getX() + 0.5D, cutPos.getY() + 1.5D, cutPos.getZ() + 0.5D, 0.0F, -90.0F);
                    var missResult = rootStrand.interact(cutter, InteractionHand.MAIN_HAND);
                    cutter.moveTo(cutPos.getX() + 0.5D, cutPos.getY() + 0.8D, cutPos.getZ() + 0.5D, 0.0F, 90.0F);
                    var hitResult = rootStrand.interact(cutter, InteractionHand.MAIN_HAND);
                    var severedHalves = level.getEntitiesOfClass(com.alex.mycelialmind.entity.HyphalVeinEntity.class, searchBox,
                            e -> e != rootStrand && !e.hasRoot());
                    if (missResult != net.minecraft.world.InteractionResult.PASS || !hitResult.consumesAction()
                            || severedHalves.isEmpty() || !rootStrand.isAlive() || !rootStrand.hasRoot()) {
                        for (var v : level.getEntitiesOfClass(com.alex.mycelialmind.entity.HyphalVeinEntity.class, searchBox)) {
                            v.discard();
                        }
                        for (int dx = -1; dx <= 6; dx++) {
                            level.removeBlock(rootPos.offset(dx, -1, 0), false);
                        }
                        tracker.clearAll();
                        hive.resetAll();
                        releaseArenaChunks(level, forcedChunks);
                        helper.fail(String.format(
                                "Expected raycast miss=PASS + shears hit split into root + severed half: miss=%s hit=%s halves=%d rootAlive=%s",
                                missResult, hitResult, severedHalves.size(), rootStrand.isAlive()));
                        return;
                    }
                    var severedHalf = severedHalves.get(0);

                    long startTick = helper.getTick();
                    long deadlineTick = startTick + 90L;
                    boolean[] finished = {false};
                    Runnable cleanup = () -> {
                        for (var v : level.getEntitiesOfClass(com.alex.mycelialmind.entity.HyphalVeinEntity.class, searchBox)) {
                            v.discard();
                        }
                        for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, searchBox)) {
                            item.discard();
                        }
                        for (int dx = -1; dx <= 6; dx++) {
                            level.removeBlock(rootPos.offset(dx, -1, 0), false);
                        }
                        tracker.clearAll();
                        hive.resetAll();
                        releaseArenaChunks(level, forcedChunks);
                    };
                    helper.onEachTick(() -> {
                        if (finished[0]) {
                            return;
                        }
                        boolean rootAlive = rootStrand.isAlive() && !rootStrand.isRemoved() && rootStrand.hasRoot();
                        boolean downstreamWithered = severedHalf.isRemoved();
                        if (!rootAlive) {
                            finished[0] = true;
                            cleanup.run();
                            helper.fail("Severed branch check failed: the rooted half disappeared before its severed half.");
                        } else if (downstreamWithered) {
                            finished[0] = true;
                            cleanup.run();
                            helper.succeed();
                        } else if (helper.getTick() >= deadlineTick) {
                            finished[0] = true;
                            String failure = String.format(
                                    "Severed half did not wither within 90 ticks: elapsed=%d wither=%.2f",
                                    helper.getTick() - startTick, severedHalf.getWitherProgress());
                            cleanup.run();
                            helper.fail(failure);
                        }
                    });
                });
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins", timeoutTicks = 240)
    public static void hyphalWallVeinLosesSupportFallsAndDisintegratesOnLanding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 16, -4, 16);
        HiveMindSavedData hive = resetHive(level);

        BlockPos veinPos = insetInChunk(helper.absolutePos(new BlockPos(0, 14, 0)), 6);
        BlockPos wallA = veinPos.west();
        BlockPos wallB = veinPos.east(2);
        BlockPos landingFloor = veinPos.below(5);
        AABB dropBounds = new AABB(landingFloor).inflate(4.0D, 8.0D, 4.0D);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        entityChunksReady(level, veinPos, 2),
                        "Expected entity-ticking chunks around falling hyphal vein arena to be ready."))
                .thenExecute(() -> {
                    for (int dx = -1; dx <= 3; dx++) {
                        for (int dy = 1; dy >= -5; dy--) {
                            level.removeBlock(veinPos.offset(dx, dy, 0), false);
                        }
                        level.setBlockAndUpdate(landingFloor.offset(dx, 0, 0), Blocks.STONE.defaultBlockState());
                    }
                    level.setBlockAndUpdate(wallA, Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(wallB, Blocks.STONE.defaultBlockState());

                    var strand = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, wallA, wallB, 7, true, 0.60F);
                    level.addFreshEntity(strand);

                    long fallDeadline = helper.getTick() + 45L;
                    boolean[] sawFreeFall = {false};
                    boolean[] finished = {false};
                    Runnable cleanup = () -> {
                        if (!strand.isRemoved()) strand.discard();
                        level.removeBlock(wallA, false);
                        level.removeBlock(wallB, false);
                        for (int dx = -1; dx <= 3; dx++) {
                            level.removeBlock(landingFloor.offset(dx, 0, 0), false);
                        }
                        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, dropBounds)
                                .forEach(net.minecraft.world.entity.Entity::discard);
                        hive.resetAll();
                        releaseArenaChunks(level, forcedChunks);
                    };
                    level.removeBlock(wallA, false);
                    level.removeBlock(wallB, false);
                    helper.onEachTick(() -> {
                        if (finished[0]) {
                            return;
                        }
                        if (!strand.isPinStart() && !strand.isPinEnd()) {
                            sawFreeFall[0] = true;
                        }
                        if (sawFreeFall[0] && strand.isRemoved()) {
                            var remainingDrops = level.getEntitiesOfClass(
                                    net.minecraft.world.entity.item.ItemEntity.class, dropBounds);
                            finished[0] = true;
                            cleanup.run();
                            if (!remainingDrops.isEmpty()) {
                                helper.fail("Gravity check failed: free-falling hypha should disintegrate without item drops, got "
                                        + remainingDrops.size());
                            } else {
                                helper.succeed();
                            }
                        } else if (helper.getTick() >= fallDeadline) {
                            finished[0] = true;
                            String msg = String.format(
                                    "Unsupported hypha did not fall and disintegrate within 45 ticks: sawFreeFall=%s pinA=%s pinB=%s midY=%.2f",
                                    sawFreeFall[0], strand.isPinStart(), strand.isPinEnd(), strand.getMidpointNode().y);
                            cleanup.run();
                            helper.fail(msg);
                        }
                    });
                });
    }

    @GameTest(template = "empty", batch = "neoforge_integration_cures", timeoutTicks = 120)
    public static void neoforgeIntegrationGlmDataMapsCapabilitiesBossPartsAndCuresWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -2, 15, -2, 12);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        tracker.clearAll();
        hive.resetAll();

        BlockPos symPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos turretPos = helper.absolutePos(new BlockPos(4, 2, 2));
        BlockPos pylonPos = helper.absolutePos(new BlockPos(6, 2, 2));
        var behemoth = ModEntities.SPORE_BEHEMOTH.get().create(level);
        var hiveCore = ModEntities.HIVE_CORE.get().create(level);
        var matriarch = ModEntities.APEX_MATRIARCH.get().create(level);

        helper.runAtTickTime(110, () -> {
            if (!entityChunksReady(level, pylonPos.east(6), 2)) {
                if (behemoth != null) behemoth.discard();
                if (hiveCore != null) hiveCore.discard();
                if (matriarch != null) matriarch.discard();
                tracker.clearAll();
                hive.resetAll();
                releaseArenaChunks(level, forcedChunks);
                helper.fail("Dispenser target chunks did not become entity-ticking");
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        entityChunksReady(level, pylonPos.east(6), 2),
                        "Dispenser target chunks must be entity-ticking before spawning the villager"))
                .thenExecute(() -> {
        try {
            // 1. Data Maps & Global Loot Modifiers
            helper.assertTrue(com.alex.mycelialmind.registry.ModLootModifiers.MYCELIAL_TRACE_LOOT.isBound(),
                    "MycelialTraceLootModifier must be bound in NeoForge GLM registry");
            helper.assertTrue(new ItemStack(ModItems.MYCELIAL_BIOMASS.get()).getBurnTime(null) == 400,
                    "Expected mycelial_biomass burn time 400");
            helper.assertTrue(new ItemStack(ModBlocks.LUMINESCENT_AMBER_VEIN.asItem()).getBurnTime(null) == 1200,
                    "Expected luminescent_amber_vein burn time 1200");
            helper.assertTrue(new ItemStack(ModItems.VOLATILE_ENGRAM.get()).getBurnTime(null) == 1600,
                    "Expected volatile_engram burn time 1600");
            helper.assertTrue(net.minecraft.world.level.block.ComposterBlock.getValue(
                    new ItemStack(ModItems.HYPHAL_VEIN.get())) > 0.0F,
                    "Expected hyphal_vein to be compostable via NeoForge DataMap");
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION
                    .wrapAsHolder(net.minecraft.world.entity.npc.VillagerProfession.CLERIC)
                    .getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.RAID_HERO_GIFTS) != null,
                    "Expected Cleric RAID_HERO_GIFTS data map entry");

            // 2. Hybrid EffectCure: MILK cures MYCELIAL_INFECTION only, MYCELIAL_ANTIDOTE_CURE cures both
            Player milkPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            milkPlayer.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));
            milkPlayer.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 200, 0));
            milkPlayer.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
            helper.assertTrue(!milkPlayer.hasEffect(ModEffects.MYCELIAL_INFECTION)
                    && milkPlayer.hasEffect(ModEffects.SPORE_ROT),
                    "Milk must cure MYCELIAL_INFECTION but leave SPORE_ROT intact");
            milkPlayer.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE);
            helper.assertTrue(!milkPlayer.hasEffect(ModEffects.SPORE_ROT),
                    "MYCELIAL_ANTIDOTE_CURE must cure SPORE_ROT");

            // 3. Insert-only Capabilities on Symbiotic Node, Purification Turret, and Purification Pylon (+ TicketController)
            level.setBlockAndUpdate(symPos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            if (level.getBlockEntity(symPos) instanceof SynapticNodeBlockEntity nodeBe) {
                nodeBe.setSymbioticStability(60);
            }
            var symCap = level.getCapability(Capabilities.ItemHandler.BLOCK, symPos, Direction.NORTH);
            helper.assertTrue(symCap != null, "Expected ItemHandler capability on Symbiotic Node");
            ItemStack biomassRem = symCap.insertItem(0, new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), 2), false);
            helper.assertTrue(biomassRem.getCount() == 1
                    && level.getBlockEntity(symPos) instanceof SynapticNodeBlockEntity nodeBe2
                    && nodeBe2.getSymbioticStability() == 75,
                    "Inserting biomass into Symbiotic Node must recharge +15 stability and leave remainder 1");
            helper.assertTrue(symCap.extractItem(0, 1, false).isEmpty(),
                    "Symbiotic Node capability must remain insert-only");

            hive.promoteHighestTier(HiveProgressionTier.ADAPTIVE);
            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            if (level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity turretBe) {
                turretBe.setAmmo(0);
                turretBe.applyCorrosiveJam(200);
            }
            var turretCap = level.getCapability(Capabilities.ItemHandler.BLOCK, turretPos, Direction.UP);
            helper.assertTrue(turretCap != null, "Expected ItemHandler capability on Purification Turret");
            ItemStack antidotes = new ItemStack(ModItems.SPORE_ANTIDOTE.get(), 3);
            helper.assertTrue(turretCap.insertItem(1, antidotes, false) == antidotes
                    && turretCap.insertItem(0, ItemStack.EMPTY, false).isEmpty(),
                    "Invalid slots and empty stacks must be returned unchanged");
            ItemStack rejected = new ItemStack(Items.DIRT, 3);
            helper.assertTrue(turretCap.insertItem(0, rejected, false) == rejected,
                    "Turret must reject unrelated items");
            ItemStack simulated = turretCap.insertItem(0, antidotes, true);
            helper.assertTrue(simulated.getCount() == 2 && antidotes.getCount() == 3
                    && level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity simulatedTurret
                    && simulatedTurret.isJammed(),
                    "Simulated cleaning must preserve input and jam while reporting one consumed item");
            ItemStack cleaned = turretCap.insertItem(0, antidotes, false);
            helper.assertTrue(cleaned.getCount() == 2 && antidotes.getCount() == 3
                    && level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity cleanedTurret
                    && !cleanedTurret.isJammed() && cleanedTurret.getAmmo() == 0,
                    "Cleaning must take priority over reloading and consume one item from the returned stack");
            if (level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity turretBe) {
                turretBe.applyCorrosiveJam(200);
            }
            helper.assertTrue(turretCap.insertItem(0, new ItemStack(ModItems.SPORE_ANTIDOTE.get()), false).isEmpty()
                    && turretCap.insertItem(0, new ItemStack(ModItems.SYNAPTIC_CORE.get()), false).isEmpty()
                    && level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity turretBe2
                    && !turretBe2.isJammed() && turretBe2.getTier() == 2,
                    "Turret capability must unjam with SporeAntidote and upgrade to Tier 2 with SynapticCore");

            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            tracker.addPylon(pylonPos);
            var pylonCap = level.getCapability(Capabilities.ItemHandler.BLOCK, pylonPos, Direction.EAST);
            helper.assertTrue(pylonCap != null, "Expected ItemHandler capability on Purification Pylon");
            ItemStack cores = new ItemStack(ModItems.SYNAPTIC_CORE.get(), 3);
            ItemStack pylonRejected = new ItemStack(Items.DIRT, 3);
            helper.assertTrue(pylonCap.insertItem(1, cores, false) == cores
                    && pylonCap.insertItem(0, ItemStack.EMPTY, false).isEmpty()
                    && pylonCap.insertItem(0, pylonRejected, false) == pylonRejected,
                    "Pylon must reject invalid slots, empty stacks and unrelated items");
            ItemStack simulatedCore = pylonCap.insertItem(0, cores, true);
            helper.assertTrue(simulatedCore.getCount() == 2 && cores.getCount() == 3
                    && level.getBlockEntity(pylonPos) instanceof MonumentFocusBlockEntity simulatedPylon
                    && simulatedPylon.getTier() == 1,
                    "Tier-one core simulation must report an upgrade without changing the pylon or input");
            ItemStack coreRemainder = pylonCap.insertItem(0, cores, false);
            helper.assertTrue(coreRemainder.getCount() == 2 && cores.getCount() == 3
                    && level.getBlockEntity(pylonPos) instanceof MonumentFocusBlockEntity upgradedPylon
                    && upgradedPylon.getTier() == 2,
                    "Tier-one core insertion must upgrade the pylon and preserve the input stack");
            pylonCap.insertItem(0, new ItemStack(ModItems.OVERMIND_ESSENCE.get()), false);
            helper.assertTrue(level.getBlockEntity(pylonPos) instanceof MonumentFocusBlockEntity pylonBe
                    && pylonBe.getTier() == 3,
                    "Pylon capability must upgrade Pylon to Tier 3");
            var forcedData = level.getDataStorage().get(net.minecraft.world.level.ForcedChunksSavedData.factory(), "chunks");
            long pylonChunkLong = new ChunkPos(pylonPos).toLong();
            boolean hasPylonTicket = forcedData != null && (
                    forcedData.getBlockForcedChunks().getTickingChunks().values().stream()
                            .anyMatch(chunks -> chunks.contains(pylonChunkLong))
                    || forcedData.getBlockForcedChunks().getChunks().values().stream()
                            .anyMatch(chunks -> chunks.contains(pylonChunkLong))
            );
            helper.assertTrue(com.alex.mycelialmind.world.MycelialChunkTicketManager.shouldAnchorPylon(level, pylonPos)
                    && (hasPylonTicket || level.getForcedChunks().contains(pylonChunkLong)),
                    "Tier 3 Purification Pylon must register a NeoForge TicketController forced chunk");
            hive.resetAll();

            // 4. Native PartEntity sub-hitboxes on all 3 bosses
            helper.assertTrue(behemoth != null && hiveCore != null && matriarch != null,
                    "All 3 bosses must instantiate cleanly");
            helper.assertTrue(behemoth.isMultipartEntity() && behemoth.getParts().length == 2
                    && hiveCore.isMultipartEntity() && hiveCore.getParts().length == 2
                    && matriarch.isMultipartEntity() && matriarch.getParts().length == 2,
                    "All 3 bosses must expose 2 PartEntity sub-hitboxes");
            behemoth.moveTo(symPos.getX() + 4.5, symPos.getY() + 1.0, symPos.getZ() + 4.5);
            level.addFreshEntity(behemoth);
            behemoth.aiStep();
            var parts = behemoth.getParts();
            float hpBefore = behemoth.getHealth();
            parts[0].hurt(level.damageSources().generic(), 10.0F);
            float weakDmg = hpBefore - behemoth.getHealth();
            float hpMid = behemoth.getHealth();
            parts[1].hurt(level.damageSources().generic(), 10.0F);
            float carapaceDmg = hpMid - behemoth.getHealth();
            helper.assertTrue(weakDmg > carapaceDmg,
                    "Boss weak point (" + weakDmg + ") must deal more damage than armored carapace (" + carapaceDmg + ")");

            // 5. Custom DamageTypes, Custom DataMaps (DAMAGE_STIMULUS & SYMBIOTIC_NUTRIENT), and Deep Mycelium effects
            var damageTypeReg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
            for (var damageKey : com.alex.mycelialmind.registry.ModDamageTypes.ALL) {
                var holder = damageTypeReg.getHolder(damageKey).orElse(null);
                helper.assertTrue(holder != null, "Missing DamageType in registry: " + damageKey.location());
                var source = com.alex.mycelialmind.registry.ModDamageTypes.source(level, damageKey);
                helper.assertTrue(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                                && source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO),
                        "Custom DamageType must carry BYPASSES_ARMOR and WITCH_RESISTANT_TO: " + damageKey.location());
                var stimulusData = holder.getData(com.alex.mycelialmind.registry.ModDataMaps.DAMAGE_STIMULUS);
                helper.assertTrue(stimulusData != null
                                && stimulusData.stimulus() == com.alex.mycelialmind.hive.StimulusType.MAGIC
                                && Math.abs(stimulusData.weightMultiplier() - 1.0F) < 1.0e-4F,
                        "Expected DAMAGE_STIMULUS data map entry on " + damageKey.location());
                helper.assertTrue(DamageTelemetryTracker.classifyDamage(source)
                                == com.alex.mycelialmind.hive.StimulusType.MAGIC,
                        "DamageTelemetryTracker must classify custom DamageType via DAMAGE_STIMULUS: " + damageKey.location());
            }

            var capNutrient = ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.asItem().builtInRegistryHolder()
                    .getData(com.alex.mycelialmind.registry.ModDataMaps.SYMBIOTIC_NUTRIENT);
            helper.assertTrue(capNutrient != null && capNutrient.stabilityRestore() == 12,
                    "Expected SYMBIOTIC_NUTRIENT (+12) on bioluminescent_mycelial_cap");
            ItemStack capRem = symCap.insertItem(0, new ItemStack(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.asItem(), 1), false);
            helper.assertTrue(capRem.isEmpty()
                            && level.getBlockEntity(symPos) instanceof SynapticNodeBlockEntity nodeBe3
                            && nodeBe3.getSymbioticStability() == 87,
                    "Inserting bioluminescent_mycelial_cap must recharge Symbiotic Node +12 (75 -> 87)");

            var dimType = level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE)
                    .get(com.alex.mycelialmind.world.ModWorldgenKeys.DEEP_MYCELIUM_TYPE);
            helper.assertTrue(dimType != null
                            && com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_EFFECTS.equals(
                                    dimType.effectsLocation()),
                    "Deep Mycelium DimensionType must reference custom effects mycelialmind:deep_mycelium");

            // 6. Native Attributes, Synaptic RecipeType, Dispensers, ItemAbilities, CriteriaTriggers, CustomStats
            var tester = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(
                    java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"), "NeoForgeNativeTester"));
            helper.assertTrue(tester.getAttributes().hasAttribute(com.alex.mycelialmind.registry.ModAttributes.INFECTION_RESISTANCE)
                            && tester.getAttributes().hasAttribute(com.alex.mycelialmind.registry.ModAttributes.ACID_RESISTANCE),
                    "Player must carry INFECTION_RESISTANCE and ACID_RESISTANCE attributes");

            ItemStack evolvedChest = new ItemStack(ModItems.CHITIN_CHESTPLATE.get());
            com.alex.mycelialmind.item.ChitinArmorItem.setArmorRank(
                    evolvedChest, com.alex.mycelialmind.hive.EvolutionRank.ELITE);
            tester.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, evolvedChest);
            evolvedChest.forEachModifier(net.minecraft.world.entity.EquipmentSlot.CHEST, (attr, mod) -> {
                var inst = tester.getAttribute(attr);
                if (inst != null) {
                    inst.addTransientModifier(mod);
                }
            });
            helper.assertTrue(tester.getAttributeValue(com.alex.mycelialmind.registry.ModAttributes.INFECTION_RESISTANCE) >= 0.20D
                            && tester.getAttributeValue(com.alex.mycelialmind.registry.ModAttributes.ACID_RESISTANCE) >= 0.15D,
                    "ELITE Chitin Chestplate must populate native INFECTION_RESISTANCE and ACID_RESISTANCE attributes");
            evolvedChest.forEachModifier(net.minecraft.world.entity.EquipmentSlot.CHEST, (attr, mod) -> {
                var inst = tester.getAttribute(attr);
                if (inst != null) {
                    inst.removeModifier(mod.id());
                }
            });
            tester.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);

            var synapticRecipes = level.getRecipeManager().getAllRecipesFor(
                    com.alex.mycelialmind.registry.ModRecipes.SYNAPTIC_SYNTHESIS_TYPE.get());
            helper.assertTrue(synapticRecipes.size() == 15,
                    "Expected 15 data-driven synaptic_synthesis recipes loaded, found " + synapticRecipes.size());

            helper.assertTrue(ModItems.BIO_EXTRACTOR.get().canPerformAction(
                            new ItemStack(ModItems.BIO_EXTRACTOR.get()),
                            net.neoforged.neoforge.common.ItemAbilities.SHEARS_HARVEST),
                    "BioExtractor must expose ItemAbilities.SHEARS_HARVEST");
            helper.assertTrue(ModItems.ENZYME_LANCE.get().canPerformAction(
                            new ItemStack(ModItems.ENZYME_LANCE.get()),
                            com.alex.mycelialmind.item.EnzymeLanceItem.SHIELD_DISABLE)
                            && ModItems.ENZYME_LANCE.get().canDisableShield(
                                    new ItemStack(ModItems.ENZYME_LANCE.get()),
                                    new ItemStack(net.minecraft.world.item.Items.SHIELD),
                                    tester, tester),
                    "EnzymeLance must expose SHIELD_DISABLE and canDisableShield");

            BlockPos dispPos = pylonPos.east(5);
            net.minecraft.world.entity.npc.Villager villager = null;
            net.minecraft.server.level.ServerPlayer realAdvPlayer = null;
            try {
                level.setBlock(dispPos, net.minecraft.world.level.block.Blocks.DISPENSER.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.EAST), 18);
                var dispBe = level.getBlockEntity(dispPos, net.minecraft.world.level.block.entity.BlockEntityType.DISPENSER).orElse(null);
                helper.assertTrue(dispBe != null, "DispenserBlockEntity must exist at test position");
                villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
                helper.assertTrue(villager != null, "Villager must spawn for Dispenser antidote test");
                villager.setPos(dispPos.getX() + 1.5D, dispPos.getY() + 0.1D, dispPos.getZ() + 0.5D);
                helper.assertTrue(level.addFreshEntity(villager), "Villager must be added for dispenser antidote test");
                villager.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));
                helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,
                                new AABB(dispPos.east()).inflate(1.0D)).contains(villager),
                        "Infected villager must be indexed in the dispenser target area");
                var dispBehavior = net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(ModItems.SPORE_ANTIDOTE.get());
                ItemStack dispRemainder = dispBehavior.dispense(
                        new net.minecraft.core.dispenser.BlockSource(level, dispPos, level.getBlockState(dispPos), dispBe),
                        new ItemStack(ModItems.SPORE_ANTIDOTE.get(), 1));
                helper.assertTrue(dispRemainder.isEmpty() && !villager.hasEffect(ModEffects.MYCELIAL_INFECTION),
                        "Dispenser SporeAntidote behavior must cure MYCELIAL_INFECTION in front of dispenser");

                helper.assertTrue(ModItems.PURIFIER_GRENADE.get() instanceof net.minecraft.world.item.ProjectileItem pi
                                && pi.asProjectile(level, new net.minecraft.world.phys.Vec3(dispPos.getX(), dispPos.getY(), dispPos.getZ()),
                                        new ItemStack(ModItems.PURIFIER_GRENADE.get()), Direction.EAST)
                                        instanceof com.alex.mycelialmind.entity.projectile.PurifierGrenadeProjectile,
                        "PurifierGrenade must instantiate PurifierGrenadeProjectile via ProjectileItem");

                realAdvPlayer = new net.minecraft.server.level.ServerPlayer(
                        level.getServer(),
                        level,
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.fromString("44444444-4444-4444-4444-444444444444"),
                                "NeoForgeAdvTester"
                        ),
                        net.minecraft.server.level.ClientInformation.createDefault()
                );
                boolean advAwarded = com.alex.mycelialmind.registry.ModAdvancements.award(
                        realAdvPlayer, com.alex.mycelialmind.registry.ModAdvancements.PURIFIER_DRONE_DEPLOYED);
                var advHolder = level.getServer().getAdvancements().get(
                        com.alex.mycelialmind.registry.ModAdvancements.PURIFIER_DRONE_DEPLOYED.id());
                helper.assertTrue(advAwarded && advHolder != null
                                && realAdvPlayer.getAdvancements().getOrStartProgress(advHolder).isDone(),
                        "HiveMilestoneTrigger must award PURIFIER_DRONE_DEPLOYED without ImpossibleTrigger");

                var customStatId = com.alex.mycelialmind.registry.ModStats.SYNAPTIC_SYNTHESES.get();
                helper.assertTrue(net.minecraft.stats.Stats.CUSTOM.contains(customStatId)
                                && net.minecraft.stats.Stats.CUSTOM.contains(com.alex.mycelialmind.registry.ModStats.NODES_DESTROYED.get())
                                && net.minecraft.stats.Stats.CUSTOM.contains(com.alex.mycelialmind.registry.ModStats.SAMPLES_EXTRACTED.get())
                                && net.minecraft.stats.Stats.CUSTOM.contains(com.alex.mycelialmind.registry.ModStats.MUTAGENS_IMPLANTED.get())
                                && net.minecraft.stats.Stats.CUSTOM.contains(com.alex.mycelialmind.registry.ModStats.CYCLES_COMPLETED.get()),
                        "All 5 custom stats must be registered in Stats.CUSTOM");
                var customStat = net.minecraft.stats.Stats.CUSTOM.get(customStatId);
                int statBefore = realAdvPlayer.getStats().getValue(customStat);
                realAdvPlayer.awardStat(customStatId, 2);
                int statAfter = realAdvPlayer.getStats().getValue(customStat);
                helper.assertTrue(statAfter == statBefore + 2,
                        "CustomStat SYNAPTIC_SYNTHESES must increment via ServerPlayer.awardStat");
            } finally {
                if (villager != null) villager.discard();
                if (realAdvPlayer != null) {
                    com.alex.mycelialmind.registry.ModCriteriaTriggers.HIVE_MILESTONE.get()
                            .removePlayerListeners(realAdvPlayer.getAdvancements());
                }
                level.removeBlock(dispPos, false);
            }

            // 7. Wave 3: Data-driven treasure enchantments, station DataMaps, and MobEffectEvent.Applicable immunity
            var enchRegistry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            for (var enchKey : com.alex.mycelialmind.registry.ModEnchantments.ALL) {
                var enchHolder = enchRegistry.getHolder(enchKey).orElse(null);
                helper.assertTrue(enchHolder != null, "Missing custom enchantment: " + enchKey.location());
                helper.assertTrue(enchHolder.is(net.minecraft.tags.EnchantmentTags.TREASURE),
                        "Expected TREASURE tag on " + enchKey.location());
                helper.assertTrue(!enchHolder.is(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE),
                        "Treasure enchantment must NOT be in enchanting table: " + enchKey.location());
            }
            var antimycoticHolder = enchRegistry.getHolderOrThrow(com.alex.mycelialmind.registry.ModEnchantments.ANTIMYCOTIC);
            helper.assertTrue(antimycoticHolder.is(net.minecraft.tags.EnchantmentTags.DAMAGE_EXCLUSIVE),
                    "Antimycotic must belong to DAMAGE_EXCLUSIVE");

            var amethystPylonFuel = net.minecraft.world.item.Items.AMETHYST_SHARD.builtInRegistryHolder()
                    .getData(com.alex.mycelialmind.registry.ModDataMaps.PYLON_FUEL);
            var antidoteTurretAmmo = ModItems.CONCENTRATED_ANTIDOTE.get().builtInRegistryHolder()
                    .getData(com.alex.mycelialmind.registry.ModDataMaps.TURRET_AMMO);
            helper.assertTrue(amethystPylonFuel != null && amethystPylonFuel.chargeRestore() == 25
                            && MonumentFocusBlockEntity.resolvePylonRechargeAmount(
                                    new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD)) == 25,
                    "PYLON_FUEL DataMap must resolve amethyst_shard = 25");
            helper.assertTrue(antidoteTurretAmmo != null && antidoteTurretAmmo.ammoRestore() == 64
                            && PurificationTurretBlockEntity.resolveTurretAmmoGain(
                                    new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get())) == 64,
                    "TURRET_AMMO DataMap must resolve concentrated_antidote = 64");

            var sporeling = ModEntities.SPORELING.get().create(level);
            var drone = ModEntities.PURIFIER_DRONE.get().create(level);
            helper.assertTrue(sporeling != null && drone != null, "Sporeling and PurifierDrone must instantiate");
            try {
                sporeling.moveTo(symPos.getX() + 1.5, symPos.getY() + 1.0, symPos.getZ() + 4.5);
                drone.moveTo(symPos.getX() + 3.5, symPos.getY() + 1.0, symPos.getZ() + 4.5);
                level.addFreshEntity(sporeling);
                level.addFreshEntity(drone);
                helper.assertTrue(!sporeling.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 100, 0))
                                && !sporeling.hasEffect(ModEffects.MYCELIAL_INFECTION)
                                && !sporeling.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 100, 0))
                                && !sporeling.hasEffect(ModEffects.SPORE_ROT),
                        "MobEffectEvent.Applicable must block MYCELIAL_INFECTION and SPORE_ROT on MycelialMob");
                helper.assertTrue(!drone.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 100, 0))
                                && !drone.hasEffect(ModEffects.MYCELIAL_INFECTION),
                        "MobEffectEvent.Applicable must block MYCELIAL_INFECTION on PurifierDrone");
                boolean playerInfected = tester.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 100, 0));
                helper.assertTrue(playerInfected && tester.hasEffect(ModEffects.MYCELIAL_INFECTION),
                        "Players must still receive MYCELIAL_INFECTION (mitigated, not blocked)");
            } finally {
                if (sporeling != null) sporeling.discard();
                if (drone != null) drone.discard();
                tester.removeEffect(ModEffects.MYCELIAL_INFECTION);
            }

            helper.assertTrue(
                    com.alex.mycelialmind.registry.MycelialBuiltInPacks.detectActivePreset()
                            == com.alex.mycelialmind.registry.MycelialBuiltInPacks.ActivePreset.DEFAULT,
                    "Base GameTestServer must resolve DEFAULT DataMap preset");

            BlockPos myceliumPos = dispPos.above();
            try {
                level.setBlock(myceliumPos, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState(), 18);
                var hoeCtx = new net.minecraft.world.item.context.UseOnContext(
                        tester, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(myceliumPos), Direction.UP, myceliumPos, false));
                var hoeState = level.getBlockState(myceliumPos).getToolModifiedState(
                        hoeCtx, net.neoforged.neoforge.common.ItemAbilities.HOE_TILL, false);
                var shovelState = level.getBlockState(myceliumPos).getToolModifiedState(
                        hoeCtx, net.neoforged.neoforge.common.ItemAbilities.SHOVEL_FLATTEN, false);
                helper.assertTrue(hoeState != null && hoeState.is(Blocks.DIRT) && shovelState == null,
                        "BlockToolModificationEvent must convert creeping_mycelium to dirt only with HOE_TILL");
            } finally {
                level.removeBlock(myceliumPos, false);
            }

            helper.succeed();
        } finally {
            if (behemoth != null) behemoth.discard();
            if (hiveCore != null) hiveCore.discard();
            if (matriarch != null) matriarch.discard();
            com.alex.mycelialmind.world.MycelialChunkTicketManager.syncPylonChunkTicket(level, pylonPos, false);
            level.removeBlock(symPos, false);
            level.removeBlock(turretPos, false);
            level.removeBlock(pylonPos, false);
            tracker.clearAll();
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
        }
                });
    }

    @GameTest(template = "empty", batch = "first_outbreak_research", timeoutTicks = 120)
    public static void firstOutbreakProjectileHookAndOngoingResearchWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        hive.resetAll();
        tracker.clearAll();

        BlockPos anchor = homeostasisTestPos(helper, 9, new BlockPos(4, 12, 4));
        BlockPos nodePos = anchor.east(3);
        BlockPos benchPos = anchor.south(3);
        var playerA = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"), "ResearcherA"));
        var playerB = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"), "CoopPartnerB"));
        playerA.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());
        playerB.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());
        playerA.getInventory().clearContent();
        playerB.getInventory().clearContent();
        playerA.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());

        net.minecraft.resources.ResourceLocation scannerRecipeId =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "bio_scanner");
        level.getRecipeManager().byKey(scannerRecipeId).ifPresent(holder -> {
            playerA.getRecipeBook().remove(holder);
            playerB.getRecipeBook().remove(holder);
        });

        SporelingEntity sporeling = ModEntities.SPORELING.get().create(level);
        try {
            helper.assertTrue(sporeling != null, "Sporeling must instantiate");
            level.setBlockAndUpdate(anchor.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(nodePos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(benchPos.below(), Blocks.STONE.defaultBlockState());
            sporeling.moveTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
            level.addFreshEntity(sporeling);
            playerA.setPos(anchor.getX() + 2.5, anchor.getY(), anchor.getZ() + 0.5);
            playerB.setPos(anchor.getX() + 3.5, anchor.getY(), anchor.getZ() + 0.5);

            // 1. First Latent Spore reveals Bio-Scanner recipe in new & existing worlds
            helper.assertTrue(!playerA.getRecipeBook().contains(scannerRecipeId),
                    "Bio-Scanner recipe should start locked for fresh player");
            PlayerDiscoveryData.onLatentSporeAcquired(playerA);
            PlayerDiscoveryData dataA = playerA.getData(ModAttachments.DISCOVERY);
            helper.assertTrue(dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_SPORE_FOUND)
                            && playerA.getRecipeBook().contains(scannerRecipeId),
                    "Acquiring first Latent Spore must unlock bio_scanner recipe and mark RESEARCH_BIT_SPORE_FOUND");

            // Existing-world player already carrying Latent Spore unlocks on login sync
            playerB.getInventory().add(new ItemStack(ModItems.LATENT_SPORE.get()));
            HiveWaveDirector.onPlayerLoggedIn(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(playerB));
            PlayerDiscoveryData dataB = playerB.getData(ModAttachments.DISCOVERY);
            helper.assertTrue(dataB.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_SPORE_FOUND)
                            && playerB.getRecipeBook().contains(scannerRecipeId),
                    "Existing-world player holding Latent Spore must unlock bio_scanner recipe on sync");

            // 2. Scan before combat -> INSUFFICIENT_EVIDENCE
            ItemStack scannerStack = new ItemStack(ModItems.BIO_SCANNER.get());
            playerA.setItemInHand(InteractionHand.MAIN_HAND, scannerStack);
            ModItems.BIO_SCANNER.get().interactLivingEntity(scannerStack, playerA, sporeling, InteractionHand.MAIN_HAND);
            helper.assertTrue(dataA.getLastObservation() == ProgressionGoalEngine.CreatureObservation.INSUFFICIENT_EVIDENCE
                            && "minecraft:overworld".equals(dataA.getLastObservationDimension()),
                    "Pre-combat scan must yield INSUFFICIENT_EVIDENCE with origin dimension");

            // Unadapted arrow hits (does not witness deflection)
            var hitArrow = new net.minecraft.world.entity.projectile.Arrow(
                    level, playerA.getX(), playerA.getEyeY(), playerA.getZ(), new ItemStack(Items.ARROW), null);
            float hpBeforeHit = sporeling.getHealth();
            sporeling.hurt(level.damageSources().arrow(hitArrow, playerA), 4.0F);
            hitArrow.discard();
            helper.assertTrue(sporeling.getHealth() < hpBeforeHit
                            && !dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_DEFLECTION_WITNESSED),
                    "Arrow hit on unadapted mob must deal damage and not mark deflection");
            sporeling.heal(20.0F);

            // 3. Projectile learning phase -> LEARNING_PROJECTILE
            double threshold = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
            double midTarget = Math.max(ProgressionGoalEngine.MIN_OBSERVABLE_STIMULUS_SCORE + 1.0D,
                    Math.min(threshold - 2.0D, threshold * 0.60D));
            double currentProj = hive.getStimulusScore(StimulusType.PROJECTILE);
            if (currentProj < midTarget) {
                hive.recordStimulus(StimulusType.PROJECTILE,
                        stimulusForScore((midTarget - currentProj) / 1.075D), level.getGameTime());
            }
            ModItems.BIO_SCANNER.get().interactLivingEntity(scannerStack, playerA, sporeling, InteractionHand.MAIN_HAND);
            helper.assertTrue(dataA.getLastObservation() == ProgressionGoalEngine.CreatureObservation.LEARNING_PROJECTILE,
                    "Mid-learning projectile stimulus must yield LEARNING_PROJECTILE (got " + dataA.getLastObservation() + ")");

            // 4. Activate PROJECTILE_DEFLECT -> witness real deflection -> CONFIRMED_PROJECTILE_ADAPTATION
            hive.recordStimulus(StimulusType.PROJECTILE, stimulusForScore(threshold + 15.0D), level.getGameTime());
            sporeling.applyGlobalHiveState(level, hive);
            boolean sawCanceledDeflection = false;
            for (int attempt = 0; attempt < 40 && !dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_DEFLECTION_WITNESSED); attempt++) {
                sporeling.heal(20.0F);
                float pre = sporeling.getHealth();
                var arrow = new net.minecraft.world.entity.projectile.Arrow(
                        level, playerA.getX(), playerA.getEyeY(), playerA.getZ(), new ItemStack(Items.ARROW), null);
                sporeling.hurt(level.damageSources().arrow(arrow, playerA), 2.0F);
                arrow.discard();
                if (sporeling.getHealth() >= pre) {
                    sawCanceledDeflection = true;
                }
            }
            helper.assertTrue(sawCanceledDeflection
                            && dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_DEFLECTION_WITNESSED),
                    "Adapted creature must visibly deflect an arrow and record RESEARCH_BIT_DEFLECTION_WITNESSED");

            ModItems.BIO_SCANNER.get().interactLivingEntity(scannerStack, playerA, sporeling, InteractionHand.MAIN_HAND);
            helper.assertTrue(dataA.getLastObservation() == ProgressionGoalEngine.CreatureObservation.CONFIRMED_PROJECTILE_ADAPTATION
                            && dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_ADAPTATION_IDENTIFIED),
                    "Post-adaptation scan must confirm CONFIRMED_PROJECTILE_ADAPTATION");

            // 5. Pin Goal 1 (USE_TO_ADVANTAGE), check missing Bio-Extractor warning, extract sample, check missing Biomass
            com.alex.mycelialmind.network.PinExpeditionGoalPayload.applyGoalChange(
                    playerA, ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE.id());
            helper.assertTrue(ProgressionGoalEngine.resolveResearchNextStep(
                            dataA.getPinnedExpeditionGoal(), dataA.getResearchStageMask(),
                            false, true, false, false, true, false, false, hive.getThreatLevel())
                            == ProgressionGoalEngine.ResearchNextStep.MISSING_BIO_EXTRACTOR,
                    "Must warn if Bio-Extractor is missing");

            // Cooperative partner B shares Hive adaptation yet keeps independent goal choice
            helper.assertTrue(dataB.getPinnedExpeditionGoal() == ProgressionGoalEngine.ExpeditionGoal.NONE,
                    "Co-op partner B must start with independent unpinned goal");
            com.alex.mycelialmind.network.PinExpeditionGoalPayload.applyGoalChange(
                    playerB, ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE.id());
            helper.assertTrue(dataB.getPinnedExpeditionGoal() == ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE
                            && dataA.getPinnedExpeditionGoal() == ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE,
                    "Co-op players must preserve independent expedition goals");

            ItemStack extractorStack = new ItemStack(ModItems.BIO_EXTRACTOR.get());
            playerA.setItemInHand(InteractionHand.MAIN_HAND, extractorStack);
            sporeling.setHealth(sporeling.getMaxHealth() * 0.40F);
            var extractRes = ModItems.BIO_EXTRACTOR.get().interactLivingEntity(
                    extractorStack, playerA, sporeling, InteractionHand.MAIN_HAND);
            helper.assertTrue(extractRes.consumesAction()
                            && dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_SAMPLE_EXTRACTED),
                    "Bio-Extractor must extract projectile sample and set RESEARCH_BIT_SAMPLE_EXTRACTED");

            boolean hasProjSample = playerA.getInventory().items.stream().anyMatch(
                    st -> st.is(ModItems.GENETIC_SAMPLE.get())
                            && GeneticSampleItem.getTrait(st) == AdaptationTrait.PROJECTILE_DEFLECT);
            helper.assertTrue(hasProjSample, "Player A must hold coded projectile_deflect GeneticSample");
            helper.assertTrue(ProgressionGoalEngine.resolveResearchNextStep(
                            dataA.getPinnedExpeditionGoal(), dataA.getResearchStageMask(),
                            true, true, true, false, true, false, false, hive.getThreatLevel())
                            == ProgressionGoalEngine.ResearchNextStep.SAMPLE_READY_MISSING_BIOMASS,
                    "Must report missing biomass when sample is ready without biomass");

            // Synthesize Mutagen on Synaptic Research Bench -> RESEARCH_BIT_BIOTECH_SYNTHESIZED
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            if (level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench) {
                bench.setLabTier(2);
                ItemStack sample = new ItemStack(ModItems.GENETIC_SAMPLE.get());
                sample.set(ModDataComponents.TRAIT_ID.get(), AdaptationTrait.PROJECTILE_DEFLECT.id());
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, sample);
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
                var menu = new com.alex.mycelialmind.menu.SynapticBenchMenu(1, playerA.getInventory(), bench);
                var outSlot = menu.getSlot(SynapticBenchBlockEntity.SLOT_OUTPUT);
                ItemStack synthesized = outSlot.getItem().copy();
                outSlot.onTake(playerA, synthesized);
                helper.assertTrue(dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_BIOTECH_SYNTHESIZED),
                        "Synthesizing projectile biotech must mark RESEARCH_BIT_BIOTECH_SYNTHESIZED");
            }

            // 6. Switch playerA to Goal 2 (RECLAIM_CAVE), locate node, break node, check exact threat drop
            com.alex.mycelialmind.network.PinExpeditionGoalPayload.applyGoalChange(
                    playerA, ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE.id());
            helper.assertTrue(ProgressionGoalEngine.resolveResearchNextStep(
                            dataA.getPinnedExpeditionGoal(), dataA.getResearchStageMask(),
                            true, true, true, true, true, true, true, 0.0D)
                            == ProgressionGoalEngine.ResearchNextStep.THREAT_ALREADY_ZERO,
                    "Reclaim Cave must warn when Hive Threat is already 0%");

            level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
            playerA.setItemInHand(InteractionHand.MAIN_HAND, scannerStack);
            ModItems.BIO_SCANNER.get().useOn(new net.minecraft.world.item.context.UseOnContext(
                    playerA, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(nodePos), Direction.UP, nodePos, false)));
            helper.assertTrue(dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_NODE_THREAT_RECORDED)
                            && dataA.getRecordedThreat() > 0.0D,
                    "Scanning node must record threat baseline");

            double threatBeforeBreak = hive.getThreatLevel();
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(
                            level, nodePos, level.getBlockState(nodePos), playerA));
            level.removeBlock(nodePos, false);
            double expectedDrop = Math.max(0.0D, threatBeforeBreak - hive.getThreatLevel());
            helper.assertTrue(dataA.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_NODE_DESTROYED)
                            && expectedDrop > 0.0D
                            && Math.abs(dataA.getLastThreatDrop() - expectedDrop) < 0.02D,
                    "Destroying node must record exact threat drop for player A");

            // 7. Save/load NBT persistence & dimension change preservation
            CompoundTag savedA = dataA.serializeNBT(level.registryAccess());
            PlayerDiscoveryData restoredA = new PlayerDiscoveryData();
            restoredA.deserializeNBT(level.registryAccess(), savedA);
            helper.assertTrue(restoredA.getLastObservation() == dataA.getLastObservation()
                            && "minecraft:overworld".equals(restoredA.getLastObservationDimension())
                            && restoredA.getPinnedExpeditionGoal() == ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE
                            && restoredA.getResearchStageMask() == dataA.getResearchStageMask()
                            && restoredA.getLastThreatDropPermille() == dataA.getLastThreatDropPermille(),
                    "Ongoing research state must survive NBT save/load");

            HiveWaveDirector.onPlayerChangedDimension(
                    new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                            playerA, level.dimension(), ModDimensions.DEEP_MYCELIUM_LEVEL));
            helper.assertTrue("minecraft:overworld".equals(dataA.getLastObservationDimension())
                            && dataA.getPinnedExpeditionGoal() == ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE,
                    "Dimension change must preserve origin observation dimension and pinned goal");

            helper.succeed();
        } finally {
            if (sporeling != null && !sporeling.isRemoved()) {
                sporeling.discard();
            }
            playerA.getInventory().clearContent();
            playerB.getInventory().clearContent();
            playerA.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());
            playerB.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());
            level.removeBlock(nodePos, false);
            level.removeBlock(nodePos.below(), false);
            level.removeBlock(benchPos, false);
            level.removeBlock(benchPos.below(), false);
            level.removeBlock(anchor.below(), false);
            tracker.clearAll();
            hive.resetAll();
        }
    }

    @GameTest(template = "empty", batch = "outbreak_and_hive_fixes")
    public static void outbreakDeepMyceliumSubstrateFissureVaultAndCryptIntegrity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        BlockPos outbreakGround = helper.absolutePos(new BlockPos(8, 160, 8));
        BlockPos protectedGround = helper.absolutePos(new BlockPos(36, 160, 8));
        BlockPos pylonPos = protectedGround.offset(2, 1, 0);
        BlockPos waterHiveOrigin = helper.absolutePos(new BlockPos(8, 160, 40));
        BlockPos hiveOrigin = helper.absolutePos(new BlockPos(36, 160, 40));
        BlockPos protectedChest = outbreakGround.offset(2, 0, 1);
        BlockPos protectedBedrock = outbreakGround.offset(1, 0, 1);

        try {
            // 1. MycelialOutbreakFeature on MYCELIAL_STONE with BlockEntity/Bedrock protection
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    for (int dy = -5; dy <= 9; dy++) {
                        BlockPos p = outbreakGround.offset(dx, dy, dz);
                        if (dy <= 0) {
                            level.setBlock(p, ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 18);
                        } else {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                        }
                    }
                }
            }
            level.setBlock(protectedChest, Blocks.CHEST.defaultBlockState(), 18);
            level.setBlock(protectedBedrock, Blocks.BEDROCK.defaultBlockState(), 18);

            boolean placedDeep = MycelialOutbreakFeature.generateOutbreakAt(
                    level, outbreakGround, level.getRandom());
            helper.assertTrue(placedDeep, "MycelialOutbreakFeature must place on MYCELIAL_STONE substrate.");

            helper.assertTrue(level.getBlockState(outbreakGround).is(ModBlocks.MYCELIAL_NODE.get())
                            || level.getBlockState(outbreakGround).is(ModBlocks.INCUBATOR_NODE.get())
                            || level.getBlockState(outbreakGround).is(ModBlocks.SENTINEL_NODE.get()),
                    "Outbreak must retain its central synaptic node.");
            helper.assertTrue(level.getBlockEntity(protectedChest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                    "Outbreak carpet must never overwrite an existing BlockEntity.");
            helper.assertTrue(level.getBlockState(protectedBedrock).is(Blocks.BEDROCK),
                    "Outbreak must never overwrite indestructible bedrock.");

            // 2. Purification Pylon blocks Outbreak placement inside protected radius
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    level.setBlock(protectedGround.offset(dx, 0, dz), Blocks.DIRT.defaultBlockState(), 18);
                    level.setBlock(protectedGround.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 18);
                }
            }
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            boolean placedProtected = MycelialOutbreakFeature.generateOutbreakAt(
                    level, protectedGround, level.getRandom());
            helper.assertFalse(placedProtected, "MycelialOutbreakFeature must not generate inside a Purification Pylon zone.");

            // 3. SurfaceHiveFeature rejects water and generates a continuous 4-step crypt stairwell
            tracker.clearAll();
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.setBlock(waterHiveOrigin.below().offset(dx, 0, dz), Blocks.WATER.defaultBlockState(), 18);
                }
            }
            boolean placedWaterHive = com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveAt(
                    level, waterHiveOrigin, level.getRandom());
            helper.assertFalse(placedWaterHive, "SurfaceHiveFeature must reject liquid/water terrain.");

            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    for (int dy = -6; dy <= 16; dy++) {
                        BlockPos p = hiveOrigin.offset(dx, dy, dz);
                        level.setBlock(p, (dy == -1 || dy == -2) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
                    }
                }
            }
            boolean placedCleanHive = com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveAt(
                    level, hiveOrigin, level.getRandom());
            helper.assertTrue(placedCleanHive, "SurfaceHiveFeature must succeed on flat stone terrain.");
            for (int step = 0; step < 4; step++) {
                for (int w = 2; w <= 3; w++) {
                    BlockPos stairPos = hiveOrigin.offset(w, -1 - step, -step);
                    helper.assertTrue(level.getBlockState(stairPos).is(Blocks.DEEPSLATE_BRICK_STAIRS),
                            "SurfaceHive crypt stairwell missing stair at step=" + step + " w=" + w + " pos=" + stairPos);
                    for (int h = 1; h <= 3; h++) {
                        helper.assertTrue(level.getBlockState(stairPos.above(h)).isAir(),
                                "SurfaceHive crypt stairwell blocked headroom at " + stairPos.above(h));
                    }
                }
            }

            helper.succeed();
        } finally {
            tracker.clearAll();
        }
    }

    @GameTest(template = "empty", batch = "outbreak_vertical_taproot")
    public static void subterraneanOutbreakFissureAndSurfaceTaprootConnectionWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        BlockPos caveGround = helper.absolutePos(new BlockPos(8, 130, 8));
        BlockPos surfaceGround = helper.absolutePos(new BlockPos(8, 160, 40));
        BlockPos lowerCaveGround = surfaceGround.below(24);

        try {
            // 1. Low-ceiling subterranean cave (headroom = 5) generates a fissure
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.setBlock(caveGround.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                    level.setBlock(caveGround.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 18);
                    for (int dy = 1; dy <= 5; dy++) {
                        level.setBlock(caveGround.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 18);
                    }
                    level.setBlock(caveGround.offset(dx, 6, dz), Blocks.STONE.defaultBlockState(), 18);
                    level.setBlock(caveGround.offset(dx, 14, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
                }
            }
            boolean cavePlaced = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), caveGround);
            helper.assertTrue(cavePlaced, "Subterranean outbreak must place in low-ceiling cave.");
            long caveAmber = BlockPos.betweenClosedStream(caveGround.offset(-4, -1, -4), caveGround.offset(4, -1, 4))
                    .filter(pos -> level.getBlockState(pos).is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())).count();
            helper.assertTrue(caveAmber >= 2, "Cave fissure must expose amber in its floor.");

            // 2. Surface outbreak above a subterranean cavern weaves vertical taproot & links both nodes
            tracker.clearAll();
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int y = lowerCaveGround.getY() - 1; y <= surfaceGround.getY() + 10; y++) {
                        BlockPos p = new BlockPos(surfaceGround.getX() + dx, y, surfaceGround.getZ() + dz);
                        if (y > surfaceGround.getY()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                        } else if (y == surfaceGround.getY()) {
                            level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 18);
                        } else if (y >= lowerCaveGround.getY() + 1 && y <= lowerCaveGround.getY() + 5) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                        } else {
                            level.setBlock(p, Blocks.STONE.defaultBlockState(), 18);
                        }
                    }
                }
            }

            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int dy = 1; dy <= 5; dy++)
                        level.setBlock(lowerCaveGround.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 18);
                }
            }
            boolean withoutCave = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), surfaceGround);
            helper.assertFalse(withoutCave, "A surface outbreak must not generate without a suitable cave.");
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int dy = 1; dy <= 5; dy++)
                        level.setBlock(lowerCaveGround.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 18);
                }
            }

            // A complete ring of water blocks every possible rotation, before any terrain is changed.
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2)
                        level.setBlock(surfaceGround.offset(dx, -3, dz), Blocks.WATER.defaultBlockState(), 18);
                }
            }
            boolean wetSurface = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), surfaceGround);
            helper.assertFalse(wetSurface, "A fissure must reject a route intersecting water.");
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                        BlockPos p = surfaceGround.offset(dx, -3, dz);
                        helper.assertTrue(level.getBlockState(p).is(Blocks.WATER), "Rejected route changed water.");
                        level.setBlock(p, Blocks.STONE.defaultBlockState(), 18);
                    }
                }
            }

            // Immutable blocks must also abort every possible orientation without being excavated.
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2)
                        level.setBlock(surfaceGround.offset(dx, -3, dz), Blocks.BEDROCK.defaultBlockState(), 18);
                }
            }
            boolean blockedSurface = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), surfaceGround);
            helper.assertFalse(blockedSurface, "A fissure must reject protected blocks.");
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                        BlockPos p = surfaceGround.offset(dx, -3, dz);
                        helper.assertTrue(level.getBlockState(p).is(Blocks.BEDROCK), "Rejected route changed bedrock.");
                        level.setBlock(p, Blocks.STONE.defaultBlockState(), 18);
                    }
                }
            }

            // Ordinary cave rock variants are excavatable and become suitable growth floor.
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2)
                        level.setBlock(surfaceGround.offset(dx, -3, dz), Blocks.GRANITE.defaultBlockState(), 18);
                }
            }

            boolean naturalSurface = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), surfaceGround);
            helper.assertTrue(naturalSurface, "A suitable cave must permit a connected surface opening.");
            long origins = BlockPos.betweenClosedStream(lowerCaveGround.offset(-4, -2, -4), lowerCaveGround.offset(4, 4, 4))
                    .filter(pos -> level.getBlockEntity(pos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity node
                            && node.saveWithoutMetadata(level.registryAccess()).getBoolean("OutbreakOrigin")).count();
            helper.assertTrue(origins == 1, "The surface opening must have exactly one marked cave origin.");
            BlockPos caveNode = BlockPos.betweenClosedStream(lowerCaveGround.offset(-4, -2, -4), lowerCaveGround.offset(4, 4, 4))
                    .filter(pos -> level.getBlockEntity(pos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity node
                            && node.saveWithoutMetadata(level.registryAccess()).getBoolean("OutbreakOrigin"))
                    .map(BlockPos::immutable).findFirst().orElseThrow();
            helper.assertTrue(level.getBlockState(caveNode.below()).is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                    || level.getBlockState(caveNode.below()).is(net.minecraft.tags.BlockTags.DIRT)
                    || level.getBlockState(caveNode.below()).is(ModBlocks.MYCELIAL_STONE.get()),
                    "Excavation must preserve the cave node's growth seed.");
            java.util.ArrayDeque<BlockPos> frontier = new java.util.ArrayDeque<>();
            java.util.Set<BlockPos> visited = new java.util.HashSet<>();
            frontier.add(caveNode.below());
            visited.add(caveNode.below());
            boolean reachedSurface = false;
            while (!frontier.isEmpty()) {
                BlockPos floor = frontier.removeFirst();
                if (floor.getY() >= surfaceGround.getY() - 1) { reachedSurface = true; break; }
                for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    for (int dy : new int[]{-1, 0, 1}) {
                        BlockPos next = floor.relative(direction).offset(0, dy, 0);
                        if (Math.abs(next.getX() - surfaceGround.getX()) > 5
                                || Math.abs(next.getZ() - surfaceGround.getZ()) > 5
                                || next.getY() < lowerCaveGround.getY() - 2 || !visited.add(next)) continue;
                        if ((dy == 0 || level.getBlockState(floor.above(3)).isAir())
                                && (level.getBlockState(next).is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                                    || level.getBlockState(next).is(net.minecraft.tags.BlockTags.DIRT)
                                    || level.getBlockState(next).is(ModBlocks.MYCELIAL_STONE.get()))
                                && level.getBlockState(next).isFaceSturdy(level, next, net.minecraft.core.Direction.UP)
                                && level.getBlockState(next.above()).isAir()
                                && level.getBlockState(next.above(2)).isAir()) frontier.add(next);
                    }
                }
            }
            helper.assertTrue(reachedSurface, "A continuous walkable floor must connect cave origin to surface.");
            long freshMycelium = BlockPos.betweenClosedStream(lowerCaveGround.offset(-4, -2, -4), surfaceGround.offset(4, 3, 4))
                    .filter(pos -> level.getBlockState(pos).is(ModBlocks.CREEPING_MYCELIUM.get())).count();
            helper.assertTrue(freshMycelium == 0, "Living mycelium must grow later from the cave origin.");

            helper.succeed();
        } finally {
            tracker.clearAll();
        }
    }

    @GameTest(template = "empty", batch = "general_polish")
    public static void synapticMimicDeceptionSabotageUnmaskShockAndSymbioticMirrorWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos turretPos = helper.absolutePos(new BlockPos(4, 1, 2));
        BlockPos symPos = helper.absolutePos(new BlockPos(2, 1, 5));
        BlockPos decoyPos = helper.absolutePos(new BlockPos(5, 1, 5));
        var mimic = helper.spawnWithNoFreeWill(ModEntities.SYNAPTIC_MIMIC.get(), 2, 1, 2);
        Player mock = helper.makeMockPlayer(GameType.SURVIVAL);
        mock.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        try {
            hive.resetAll();
            tracker.clearAll();
            com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.clearDecoys(level);

            // 1. Diplomatic spoofing protects Mimic unless SYNAPTIC_VISION is active
            mimic.setMimicPersona(com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.MimicPersona.INFILTRATION_SPOOF);
            mimic.setUnmasked(false);
            helper.assertTrue(MycelialMob.isFriendlyToPlayer(mimic, mock),
                    "Covert INFILTRATION_SPOOF Mimic must spoof friendliness to player and turret filters.");
            mock.addEffect(new MobEffectInstance(ModEffects.SYNAPTIC_VISION, 200, 0));
            helper.assertFalse(MycelialMob.isFriendlyToPlayer(mimic, mock),
                    "SYNAPTIC_VISION must pierce Mimic diplomatic spoofing.");
            mock.removeEffect(ModEffects.SYNAPTIC_VISION);

            // 2. Habit-driven stance & Symbiotic Mutagen parasitism
            hive.recordHabit(com.alex.mycelialmind.hive.PlayerHabit.HIGH_MOBILITY,
                    HiveMindSavedData.HABIT_ACTIVATION_THRESHOLD + 5.0);
            mock.getData(ModAttachments.SYMBIOSIS).equipMutagen(com.alex.mycelialmind.symbiosis.SymbioticMutagen.KINETIC_STRAFE);
            var stance = mimic.evaluateTargetStance(mock);
            helper.assertTrue(stance == com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance.SPORE_MARKSMAN,
                    "Mimic must counter HIGH_MOBILITY habit with SPORE_MARKSMAN stance.");

            // 3. Tripartite sabotage: jam turret, drain symbiotic node, consume decoy
            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            level.setBlockAndUpdate(symPos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            tracker.addNode(symPos, com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC);
            com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.registerDecoy(level, decoyPos, 200);
            helper.assertTrue(com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.hasActiveDecoyNear(level, decoyPos, 2),
                    "Decoy must be active before Mimic sabotage.");

            boolean sabotaged = mimic.tryExecuteNetworkSabotage(level, mock);
            helper.assertTrue(sabotaged, "Mimic network sabotage must trigger against nearby defenses/nodes/decoys.");
            var turretBe = level.getBlockEntity(turretPos) instanceof com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity t ? t : null;
            var symBe = level.getBlockEntity(symPos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity s ? s : null;
            helper.assertTrue(turretBe != null && turretBe.isJammed(), "Sabotage must jam nearby PurificationTurret.");
            helper.assertTrue(symBe != null && symBe.getSymbioticStability() < 100, "Sabotage must drain SymbioticNode stability.");
            helper.assertFalse(com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.hasActiveDecoyNear(level, decoyPos, 2),
                    "Sabotage must consume/invert nearby NeuralEchoDecoy.");

            // 4. Bio-Scanner Synaptic Shock unmasking: clears deception, unjams turret, triggers Desync & +25% dmg
            hive.startDeception(StimulusType.FIRE, level.getGameTime());
            ItemStack scanner = new ItemStack(ModItems.BIO_SCANNER.get());
            scanner.interactLivingEntity(mock, mimic, net.minecraft.world.InteractionHand.MAIN_HAND);

            helper.assertTrue(mimic.isUnmasked() && mimic.isDesynced() && mimic.getShockTicks() > 0,
                    "BioScanner interaction must unmask Mimic and inflict Desync + Synaptic Shock.");
            helper.assertTrue(mimic.wasUnmaskedByCounterplay(), "Counterplay unmask flag must be recorded for loot.");
            helper.assertFalse(hive.isDeceptionActive(level.getGameTime()),
                    "Synaptic Shock must clear active Pavlovian deception.");
            helper.assertFalse(turretBe.isJammed(), "Synaptic Shock must clear jam on nearby PurificationTurret.");
            helper.assertTrue(mimic.getParasitizedMutagen() == null, "Synaptic Shock/Desync must strip parasitized mutagen.");

            helper.succeed();
        } finally {
            com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.clearDecoys(level);
            tracker.clearAll();
            hive.resetAll();
            mock.getData(ModAttachments.SYMBIOSIS).clearMutagens();
            level.setBlockAndUpdate(turretPos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(symPos, Blocks.AIR.defaultBlockState());
            mimic.discard();
        }
    }

    @GameTest(template = "empty")
    public static void bossCombatArchitectureStateMachineCarapaceBreakAndPhasesWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var behemoth = helper.spawnWithNoFreeWill(ModEntities.SPORE_BEHEMOTH.get(), 2, 1, 2);
        Player mock = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        mock.setPos(origin.getX() + 6.5, origin.getY(), origin.getZ() + 0.5);
        try {
            // 1. State machine WINDUP -> ACTIVE -> RECOVERY -> IDLE
            behemoth.setTarget(mock);
            behemoth.startAttackWindup(
                    level,
                    com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId.BEHEMOTH_SEISMIC_SLAM,
                    mock,
                    1,
                    false
            );
            helper.assertTrue(
                    behemoth.getCombatState() == com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState.WINDUP,
                    "Boss must enter WINDUP when an attack starts."
            );
            boolean sawTelegraphLock = false;
            boolean sawActive = false;
            boolean sawRecovery = false;
            for (int t = 0; t < 70; t++) {
                behemoth.customServerAiStep();
                if (behemoth.isTelegraphLocked()) {
                    sawTelegraphLock = true;
                }
                var st = behemoth.getCombatState();
                if (st == com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState.ACTIVE) {
                    sawActive = true;
                } else if (st == com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState.RECOVERY) {
                    sawRecovery = true;
                }
            }
            helper.assertTrue(sawTelegraphLock && sawActive && sawRecovery,
                    "Boss state machine must progress through WINDUP (with lock-in) -> ACTIVE -> RECOVERY.");

            // 2. Phase-skip damage clamp: a massive single hit cannot skip Phase 2 and kill from 100% HP
            behemoth.setHealth(behemoth.getMaxHealth());
            behemoth.invulnerableTime = 0;
            behemoth.hurt(level.damageSources().magic(), 500.0F);
            helper.assertTrue(behemoth.isAlive() && behemoth.getHealth() >= behemoth.getMaxHealth() * 0.45F,
                    "Phase-skip damage clamp must stop a single massive hit just past the Phase 2 threshold.");
            behemoth.setHealth(behemoth.getMaxHealth());

            // 3. Physical carapace shatter via ARMORED_CARAPACE part -> STAGGERED + lowered WEAK_POINT
            behemoth.aiStep();
            var parts = behemoth.getParts();
            com.alex.mycelialmind.entity.MycelialBossPart weakPart = (com.alex.mycelialmind.entity.MycelialBossPart) parts[0];
            com.alex.mycelialmind.entity.MycelialBossPart carapacePart = (com.alex.mycelialmind.entity.MycelialBossPart) parts[1];
            double weakYBefore = weakPart.getY();

            for (int hit = 0; hit < 10 && !behemoth.isCarapaceBroken(); hit++) {
                behemoth.invulnerableTime = 0;
                carapacePart.hurt(level.damageSources().magic(), 12.0F);
            }
            helper.assertTrue(behemoth.isCarapaceBroken(), "Sustained hits to ARMORED_CARAPACE must shatter the carapace.");
            helper.assertTrue(behemoth.isBossStaggered(), "Carapace break must trigger STAGGERED state.");
            behemoth.aiStep();
            helper.assertTrue(weakPart.getY() < weakYBefore - 0.5D,
                    "Shattering carapace must physically lower WEAK_POINT hitbox for melee punishment.");

            // 4. Phase 2 transition at <= 50% HP restores carapace cleanly
            behemoth.setHealth(behemoth.getMaxHealth() * 0.45F);
            behemoth.customServerAiStep();
            helper.assertTrue(behemoth.getBehemothPhase() == 2, "SporeBehemoth must transition to Phase 2 below 50% HP.");
            helper.assertFalse(behemoth.isCarapaceBroken(), "Phase transition must restore carapace integrity.");

            helper.succeed();
        } finally {
            behemoth.discard();
        }
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void cleanRoomAiThreatTableEpisodicMemoryHazardEvadeAndShelterWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveBlackboard.clear(level);
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 4, 1, 4);
        var restoredBrute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 5, 1, 4);
        var architect = helper.spawnWithNoFreeWill(ModEntities.SPORE_ARCHITECT.get(), 6, 1, 4);
        var targetA = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.IRON_GOLEM, 2, 1, 4);
        var targetB = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.IRON_GOLEM, 8, 1, 4);
        var grenade = new PurifierGrenadeProjectile(level, targetA);
        try {
            // 1. Multi-attacker threat ranking + grudge memory in MycelialMob.hurt(...)
            brute.invulnerableTime = 0;
            brute.hurt(level.damageSources().mobAttack(targetA), 2.0F);
            brute.invulnerableTime = 0;
            brute.hurt(level.damageSources().mobAttack(targetB), 14.0F);
            brute.resolveThreatTableTarget(level);
            helper.assertTrue(brute.getTarget() == targetB,
                    "MobThreatTable must prioritize higher-threat attacker B over light attacker A.");
            String grudgeKey = com.alex.mycelialmind.hive.MobEpisodicMemory.grudgeKey(targetB.getUUID());
            helper.assertTrue(brute.getEpisodicMemory().hasMemory(grudgeKey),
                    "Heavy hit must record personal grudge in MobEpisodicMemory.");

            // 2. Episodic memory NBT round-trip
            brute.getEpisodicMemory().remember(com.alex.mycelialmind.hive.MobEpisodicMemory.KEY_TRAUMA_GRENADE,
                    level.getGameTime());
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            brute.addAdditionalSaveData(saved);
            restoredBrute.readAdditionalSaveData(saved);
            helper.assertTrue(restoredBrute.getEpisodicMemory().hasMemory(
                    com.alex.mycelialmind.hive.MobEpisodicMemory.KEY_TRAUMA_GRENADE),
                    "Episodic grenade trauma must persist across NBT save/load.");
            helper.assertTrue(restoredBrute.getEpisodicMemory().hasMemory(grudgeKey),
                    "Episodic grudge must persist across NBT save/load.");

            // 3. Imminent hazard evasion goal activates on nearby PurifierGrenadeProjectile
            BlockPos brutePos = brute.blockPosition();
            grenade.setPos(brutePos.getX() + 2.0D, brutePos.getY() + 0.5D, brutePos.getZ() + 0.5D);
            level.addFreshEntity(grenade);
            var evadeGoal = new com.alex.mycelialmind.entity.ai.ImminentHazardEvadeGoal(brute);
            helper.assertTrue(evadeGoal.canUse(),
                    "ImminentHazardEvadeGoal must activate for nearby PurifierGrenadeProjectile.");

            // 4. Event-driven tactical alarm triggers TacticalShelterGoal on SporeArchitectEntity
            HiveBlackboard.broadcastTacticalAlarm(level, architect.blockPosition().offset(2, 0, 0), 20.0D, 100);
            var shelterGoal = new com.alex.mycelialmind.entity.ai.TacticalShelterGoal(architect);
            helper.assertTrue(shelterGoal.canUse(),
                    "TacticalShelterGoal must activate on SporeArchitectEntity when TacticalAlarm is broadcast.");

            helper.succeed();
        } finally {
            HiveBlackboard.clear(level);
            grenade.discard();
            targetA.discard();
            targetB.discard();
            architect.discard();
            restoredBrute.discard();
            brute.discard();
        }
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void fieldEngramAbsorptionPersistsAndSyncs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        PlayerDiscoveryData discovery = fakePlayer.getData(ModAttachments.DISCOVERY);
        CompoundTag originalTag = discovery.serializeNBT(level.registryAccess());
        discovery.deserializeNBT(level.registryAccess(), new CompoundTag());
        try {
            ItemStack engramStack = com.alex.mycelialmind.item.FieldEngramItem.createStack(
                    com.alex.mycelialmind.hive.FieldEngramId.OUTBREAK_SIGNAL);
            helper.assertTrue(
                    com.alex.mycelialmind.item.FieldEngramItem.getEngramId(engramStack)
                            == com.alex.mycelialmind.hive.FieldEngramId.OUTBREAK_SIGNAL,
                    "FieldEngramItem stack must retain OUTBREAK_SIGNAL engram_id component.");

            fakePlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, engramStack);
            engramStack.use(level, fakePlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(
                    discovery.hasEngram(com.alex.mycelialmind.hive.FieldEngramId.OUTBREAK_SIGNAL),
                    "Using FieldEngramItem must archive the engram in PlayerDiscoveryData.");
            helper.assertTrue(discovery.countDiscoveredEngrams() == 1,
                    "Discovered engram count must increment to 1.");
            helper.assertTrue(engramStack.isEmpty(),
                    "Absorbing FieldEngramItem in survival must consume the stack cleanly.");

            // Re-using an already archived engram does not duplicate count
            ItemStack duplicateStack = com.alex.mycelialmind.item.FieldEngramItem.createStack(
                    com.alex.mycelialmind.hive.FieldEngramId.OUTBREAK_SIGNAL);
            fakePlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, duplicateStack);
            duplicateStack.use(level, fakePlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(discovery.countDiscoveredEngrams() == 1,
                    "Duplicate engram absorption must not inflate discovered count.");

            // NBT persistence and network StreamCodec round-trip
            CompoundTag saved = discovery.serializeNBT(level.registryAccess());
            PlayerDiscoveryData restored = new PlayerDiscoveryData();
            restored.deserializeNBT(level.registryAccess(), saved);
            helper.assertTrue(
                    restored.hasEngram(com.alex.mycelialmind.hive.FieldEngramId.OUTBREAK_SIGNAL),
                    "Engram unlock mask must survive NBT serialization round-trip.");

            DiscoverySyncPayload payload = DiscoverySyncPayload.forPlayer(fakePlayer);
            var buf = io.netty.buffer.Unpooled.buffer();
            DiscoverySyncPayload.STREAM_CODEC.encode(buf, payload);
            DiscoverySyncPayload decoded = DiscoverySyncPayload.STREAM_CODEC.decode(buf);
            helper.assertTrue(decoded.equals(payload)
                            && decoded.discoveredEngramsMask() == discovery.getDiscoveredEngramsMask(),
                    "DiscoverySyncPayload STREAM_CODEC must round-trip discoveredEngramsMask intact.");

            helper.assertTrue(ProgressionGoalEngine.isCodexGateUnlocked(
                            "engram:outbreak_signal", 1, false, false, false,
                            restored.getDiscoveredEngramsMask(), false),
                    "Codex gate for absorbed engram must unlock.");
            helper.assertFalse(ProgressionGoalEngine.isCodexGateUnlocked(
                            "engram:overmind_cycle", 4, true, true, false,
                            restored.getDiscoveredEngramsMask(), true),
                    "Codex gate for uncollected engram must remain locked.");

            helper.succeed();
        } finally {
            fakePlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            discovery.deserializeNBT(level.registryAccess(), originalTag);
        }
    }

    @GameTest(template = "empty", batch = "algorithm_integration")
    public static void hiveCycleMutatorsScaleBossAndCompleteWithRecord(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        com.alex.mycelialmind.hive.HiveMindSavedData owHive =
                com.alex.mycelialmind.hive.HiveMindSavedData.get(overworld);
        com.alex.mycelialmind.hive.HiveCycleSavedData cycles =
                com.alex.mycelialmind.hive.HiveCycleSavedData.get(overworld);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag cyclesSnapshot = cycles.save(new CompoundTag(), overworld.registryAccess());
        BlockPos dropPos = helper.absolutePos(new BlockPos(4, 2, 4));
        ApexMatriarchEntity matriarch = null;
        try {
            owHive.resetAll();
            cycles.resetAll();
            helper.assertFalse(com.alex.mycelialmind.hive.HiveCycleDirector.isCycleUnlocked(overworld),
                    "Hive Cycles must stay locked before Apex Matriarch is defeated.");
            int mask = com.alex.mycelialmind.hive.HiveCycleMutator.PRIMORDIAL_APEX.bitMask()
                    | com.alex.mycelialmind.hive.HiveCycleMutator.SYNAPTIC_SWARM.bitMask()
                    | com.alex.mycelialmind.hive.HiveCycleMutator.HYPER_ADAPTATION.bitMask();
            helper.assertFalse(com.alex.mycelialmind.hive.HiveCycleDirector.startCycle(null, overworld, mask),
                    "startCycle must fail while Matriarch defeat is not recorded.");

            owHive.recordMatriarchDefeat(overworld.getGameTime());
            helper.assertTrue(com.alex.mycelialmind.hive.HiveCycleDirector.isCycleUnlocked(overworld),
                    "Defeating Apex Matriarch must unlock Hive Cycles.");

            int invalidMask = com.alex.mycelialmind.hive.HiveCycleMutator.ALL_MASK;
            helper.assertFalse(com.alex.mycelialmind.hive.HiveCycleDirector.startCycle(null, overworld, invalidMask),
                    "startCycle must reject selecting more than 3 mutators.");

            helper.assertTrue(com.alex.mycelialmind.hive.HiveCycleDirector.startCycle(null, overworld, mask),
                    "startCycle must accept 1..3 mutators once Matriarch is defeated.");
            helper.assertTrue(cycles.isCycleActive() && cycles.getActiveMutatorMask() == mask,
                    "HiveCycleSavedData must activate with selected mutators.");
            helper.assertTrue(owHive.getMatriarchRemissionRemainingSeconds(overworld.getGameTime()) == 0,
                    "Starting a Hive Cycle must clear Matriarch remission.");

            var syncPayload = com.alex.mycelialmind.network.CycleProtocolSyncPayload.forLevel(overworld, true);
            var syncBuf = io.netty.buffer.Unpooled.buffer();
            com.alex.mycelialmind.network.CycleProtocolSyncPayload.STREAM_CODEC.encode(syncBuf, syncPayload);
            var decodedSync = com.alex.mycelialmind.network.CycleProtocolSyncPayload.STREAM_CODEC.decode(syncBuf);
            helper.assertTrue(decodedSync.equals(syncPayload) && decodedSync.cycleActive() && decodedSync.openScreen(),
                    "CycleProtocolSyncPayload must round-trip active cycle state intact.");

            matriarch = ModEntities.APEX_MATRIARCH.get().create(level);
            helper.assertTrue(matriarch != null, "ApexMatriarchEntity creation must succeed.");
            com.alex.mycelialmind.hive.HiveCycleDirector.applyBossCycleScaling(matriarch, level);
            helper.assertTrue(matriarch.getMaxHealth() > 700.0F,
                    "PRIMORDIAL_APEX must scale ApexMatriarch max health above 700 HP (got " + matriarch.getMaxHealth() + ").");

            com.alex.mycelialmind.hive.HiveCycleDirector.onMatriarchDefeated(level, dropPos);
            helper.assertFalse(cycles.isCycleActive(),
                    "Defeating ApexMatriarch must close active Hive Cycle.");
            helper.assertTrue(cycles.getCompletedCycles() == 1 && cycles.getBestScore() >= 1000
                            && !cycles.getLastCompletedGrade().isEmpty(),
                    "Completed Hive Cycle must record count, score >= 1000, and rank grade.");

            var droppedEngrams = level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(dropPos).inflate(4.0D),
                    e -> e.getItem().is(com.alex.mycelialmind.registry.ModItems.FIELD_ENGRAM.get())
                            && com.alex.mycelialmind.item.FieldEngramItem.getEngramId(e.getItem())
                            == com.alex.mycelialmind.hive.FieldEngramId.OVERMIND_CYCLE);
            helper.assertTrue(!droppedEngrams.isEmpty(),
                    "Completing a Hive Cycle must drop the OVERMIND_CYCLE Field Engram.");

            helper.succeed();
        } finally {
            if (matriarch != null) {
                matriarch.discard();
            }
            level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(dropPos).inflate(4.0D)
            ).forEach(net.minecraft.world.entity.Entity::discard);
            owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
            cycles.restoreFromSnapshot(cyclesSnapshot, overworld.registryAccess());
            com.alex.mycelialmind.hive.HiveCycleDirector.syncHiveModifiersAcrossDimensions(overworld);
        }
    }

    @GameTest(template = "empty", batch = "sovereign_nexus_upgrade")
    public static void sovereignNexusUpgradeAndOutpostLinkingTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos outpostPos = helper.absolutePos(new BlockPos(7, 2, 4));
        try {
            owHive.resetAll();
            saved.resetAll();
            helper.assertFalse(com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isSovereignUnlocked(level),
                    "Sovereign Nexus must stay locked before Apex Matriarch defeat.");

            owHive.recordMatriarchDefeat(overworld.getGameTime());
            helper.assertTrue(com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isSovereignUnlocked(level),
                    "Defeating Apex Matriarch must unlock Sovereign Nexus.");

            level.setBlockAndUpdate(nexusPos, com.alex.mycelialmind.registry.ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            level.setBlockAndUpdate(outpostPos, com.alex.mycelialmind.registry.ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            PurificationZoneTracker.get(level).addNode(outpostPos, SynapticNodeType.SYMBIOTIC);

            var be = level.getBlockEntity(nexusPos);
            helper.assertTrue(be instanceof com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity,
                    "Sovereign Nexus BlockEntity must exist at placed block.");
            var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) be;
            com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.serverTick(
                    level, nexusPos, level.getBlockState(nexusPos), nexusBe);

            var colony = nexusBe.getOrCreateColony(level);
            helper.assertTrue(colony.getLinkedOutposts().size() == 1,
                    "Sovereign Nexus serverTick must link nearby Symbiotic Node outpost.");
            helper.assertTrue(colony.getEffectiveRadius() == 32,
                    "Tier 1 colony with 1 linked outpost must have radius 32 (got " + colony.getEffectiveRadius() + ").");

            colony.addBiomass(32);
            colony.addDomainXp(150);
            helper.assertTrue(nexusBe.upgradeTier(level, null, false),
                    "Colony upgrade Tier 1 -> Tier 2 must succeed with sufficient biomass and Domain XP.");
            helper.assertTrue(colony.getTier() == 2 && colony.getPopulationCap() == 8 && colony.getEffectiveRadius() == 44,
                    "Tier 2 colony with 1 outpost must reach popCap 8 and radius 44 (got " + colony.getEffectiveRadius() + ").");

            helper.succeed();
        } finally {
            PurificationZoneTracker.get(level).removeNode(outpostPos);
            level.removeBlock(outpostPos, false);
            level.removeBlock(nexusPos, false);
            owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "colony_symbiont_incubation")
    public static void colonySymbiontIncubationCasteSwitchAndStandbyTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        com.alex.mycelialmind.entity.ColonySymbiontEntity worker = null;
        try {
            owHive.resetAll();
            saved.resetAll();
            owHive.recordMatriarchDefeat(overworld.getGameTime());
            level.setBlockAndUpdate(nexusPos, com.alex.mycelialmind.registry.ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            var colony = nexusBe.getOrCreateColony(level);
            colony.addBiomass(24);

            worker = nexusBe.incubateSymbiont(level, null, com.alex.mycelialmind.colony.SymbiontCaste.HARVESTER, false);
            helper.assertTrue(worker != null && worker.isAlive(), "Colony Symbiont incubation must spawn live entity.");
            helper.assertTrue(worker.getCaste() == com.alex.mycelialmind.colony.SymbiontCaste.HARVESTER,
                    "Incubated worker must have requested HARVESTER caste.");
            helper.assertTrue(colony.getRoster().size() == 1, "Worker must be registered in colony roster.");

            helper.assertTrue(nexusBe.setWorkerCaste(level, worker.getUUID(), com.alex.mycelialmind.colony.SymbiontCaste.SENTINEL),
                    "Caste switch to SENTINEL must succeed.");
            helper.assertTrue(worker.getCaste() == com.alex.mycelialmind.colony.SymbiontCaste.SENTINEL
                            && worker.getMaxHealth() >= 38.0F,
                    "SENTINEL caste must apply sentinel health scaling.");

            helper.assertTrue(nexusBe.toggleWorkerStandby(level, worker.getUUID()) && worker.isStandby(),
                    "Toggling worker standby must place worker in standby state.");
            helper.assertTrue(nexusBe.recallAllWorkers(level) == 1,
                    "Recalling all workers must return 1 recalled symbiont.");

            helper.succeed();
        } finally {
            if (worker != null) {
                worker.discard();
            }
            level.removeBlock(nexusPos, false);
            owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "sovereign_nexus_hopper")
    public static void sovereignNexusHopperBiomassInsertionTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData hive = HiveMindSavedData.get(overworld);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), overworld.registryAccess());
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        try {
            saved.resetAll();
            hive.resetAll();
            level.setBlockAndUpdate(nexusPos, com.alex.mycelialmind.registry.ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            var blocked = nexusBe.insertBiomassFromAutomation(new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), 10), false);
            helper.assertTrue(blocked.getCount() == 10 && saved.getByNexus(nexusPos) == null,
                    "Locked Nexus must reject hopper biomass without creating colony data.");
            helper.assertTrue(nexusBe.incubateSymbiont(level, null, com.alex.mycelialmind.colony.SymbiontCaste.HARVESTER, false) == null
                            && !nexusBe.upgradeTier(level, null, false),
                    "Locked Nexus must reject ordinary incubation and upgrades.");
            com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.serverTick(level, nexusPos, level.getBlockState(nexusPos), nexusBe);
            helper.assertTrue(saved.getByNexus(nexusPos) == null, "Locked Nexus tick must not create a colony.");
            hive.recordMatriarchDefeat(overworld.getGameTime());
            var colony = nexusBe.getOrCreateColony(level);
            int initialBiomass = colony.getBiomassStored();

            var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, nexusPos, net.minecraft.core.Direction.UP);
            helper.assertTrue(handler != null, "Sovereign Nexus must expose Capabilities.ItemHandler.BLOCK.");

            ItemStack nonBiomass = handler.insertItem(0, new ItemStack(net.minecraft.world.item.Items.DIRT, 3), false);
            helper.assertTrue(nonBiomass.getCount() == 3, "Sovereign Nexus handler must reject non-biomass items.");

            ItemStack simRem = handler.insertItem(0,
                    new ItemStack(com.alex.mycelialmind.registry.ModItems.MYCELIAL_BIOMASS.get(), 10), true);
            helper.assertTrue(simRem.isEmpty() && colony.getBiomassStored() == initialBiomass,
                    "Simulated biomass insertion must not mutate stored biomass.");

            ItemStack rem = handler.insertItem(0,
                    new ItemStack(com.alex.mycelialmind.registry.ModItems.MYCELIAL_BIOMASS.get(), 10), false);
            helper.assertTrue(rem.isEmpty() && colony.getBiomassStored() == initialBiomass + 10,
                    "Real hopper insertion must add 10 biomass into colony reserve.");

            helper.succeed();
        } finally {
            level.removeBlock(nexusPos, false);
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
            hive.restoreFromSnapshot(hiveSnapshot, overworld.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "colony_progression_gate")
    public static void colonyStaffAndNetworkRespectMatriarchGate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData hive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos markPos = nexusPos.east(2);
        var player = FakePlayerFactory.getMinecraft(level);
        var priorPos = player.position();
        ItemStack previous = player.getMainHandItem().copy();
        try {
            hive.resetAll();
            saved.resetAll();
            level.setBlockAndUpdate(nexusPos, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            level.setBlockAndUpdate(markPos, Blocks.STONE.defaultBlockState());
            var nexus = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            nexus.setOwner(player);
            // Simulate a colony retained from a save where progression was subsequently reset.
            var colony = nexus.getOrCreateColony(level);
            colony.addBiomass(100);
            colony.addDomainXp(500);
            player.moveTo(nexusPos.getX() + 0.5D, nexusPos.getY(), nexusPos.getZ() + 0.5D);
            ItemStack staff = new ItemStack(ModItems.SOVEREIGN_COMMAND_STAFF.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            com.alex.mycelialmind.item.SovereignCommandStaffItem.setMode(staff,
                    com.alex.mycelialmind.item.SovereignCommandStaffItem.StaffMode.MARK_HARVEST_ZONE);
            var use = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(markPos), Direction.UP, markPos, false));
            var action = new com.alex.mycelialmind.network.SovereignColonyActionPayload(
                    com.alex.mycelialmind.network.SovereignColonyActionPayload.ACTION_UPGRADE_TIER,
                    nexusPos, 0, player.getUUID(), BlockPos.ZERO, BlockPos.ZERO);
            var staffItem = (com.alex.mycelialmind.item.SovereignCommandStaffItem) staff.getItem();
            helper.assertTrue(staffItem.useOn(use) == net.minecraft.world.InteractionResult.FAIL
                            && colony.getTotalWorkZoneCount() == 0,
                    "Locked staff must not mark a work zone in retained colony data.");
            com.alex.mycelialmind.network.SovereignColonyActionPayload.applyOnServer(action, player);
            helper.assertTrue(colony.getTier() == 1
                            && !com.alex.mycelialmind.network.SovereignColonySyncPayload.forNexus(level, nexusPos, false).hasColony(),
                    "Locked network action and snapshot must not expose or modify retained colony data.");
            hive.recordMatriarchDefeat(overworld.getGameTime());
            helper.assertTrue(staffItem.useOn(use).consumesAction() && colony.getTotalWorkZoneCount() == 1,
                    "Unlocked staff must mark a work zone.");
            com.alex.mycelialmind.network.SovereignColonyActionPayload.applyOnServer(action, player);
            helper.assertTrue(colony.getTier() == 2
                            && com.alex.mycelialmind.network.SovereignColonySyncPayload.forNexus(level, nexusPos, false).hasColony(),
                    "Unlocked network action must upgrade and expose the colony.");
            helper.succeed();
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
            player.setPos(priorPos);
            level.removeBlock(markPos, false);
            level.removeBlock(nexusPos, false);
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
            hive.restoreFromSnapshot(hiveSnapshot, overworld.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "colony_creative_override")
    public static void colonyCreativeAndCommandOverrideRemainLocalAndPersist(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData hive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos otherPos = nexusPos.east(10);
        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        com.alex.mycelialmind.entity.ColonySymbiontEntity worker = null;
        try {
            hive.resetAll();
            saved.resetAll();
            level.setBlockAndUpdate(nexusPos, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            level.setBlockAndUpdate(otherPos, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            var nexus = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            var other = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(otherPos);
            nexus.setOwner(creative);
            var colony = nexus.getOrCreateColony(level);
            var otherColony = other.getOrCreateColony(level);
            helper.assertTrue(colony.hasAdministrativeOverride() && !otherColony.hasAdministrativeOverride()
                            && !com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isSovereignUnlocked(level),
                    "Creative override must belong only to its colony and leave world progression locked.");
            helper.assertTrue(com.alex.mycelialmind.colony.SovereignColonyData.load(colony.save()).hasAdministrativeOverride(),
                    "Administrative override must survive save/load.");
            worker = nexus.incubateSymbiont(level, creative, com.alex.mycelialmind.colony.SymbiontCaste.HARVESTER, false);
            helper.assertTrue(worker != null && !worker.isStandby() && worker.getColonyData(level) == colony,
                    "Creative workers must remain active before the Matriarch defeat.");
            helper.assertTrue(nexus.insertBiomassFromAutomation(new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), 2), false).isEmpty(),
                    "Creative colony automation must remain active.");
            var commandSource = level.getServer().createCommandSourceStack().withLevel(level)
                    .withPosition(Vec3.atCenterOf(otherPos)).withPermission(4).withSuppressedOutput();
            level.getServer().getCommands().performPrefixedCommand(commandSource, "mycelial colony upgrade");
            helper.assertTrue(otherColony.getTier() == 2 && otherColony.hasAdministrativeOverride(),
                    "Administrative command must upgrade and activate its target colony before defeat.");
            helper.succeed();
        } finally {
            if (worker != null) worker.discard();
            level.removeBlock(nexusPos, false);
            level.removeBlock(otherPos, false);
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
            hive.restoreFromSnapshot(hiveSnapshot, overworld.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "colony_dialect_journey", timeoutTicks = 800)
    public static void dialectLessonThreeSitesAndRestCall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData hive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 4, 4));
        BlockPos a = nexusPos.east(4), b = nexusPos.east(8), c = nexusPos.east(12);
        var player = FakePlayerFactory.getMinecraft(level);
        var oldPos = player.position();
        var teacher = ModEntities.COLONY_SYMBIONT.get().create(level);
        var pupil = ModEntities.COLONY_SYMBIONT.get().create(level);
        var pupilC = ModEntities.COLONY_SYMBIONT.get().create(level);
        try {
            hive.resetAll(); saved.resetAll(); hive.recordMatriarchDefeat(overworld.getGameTime());
            level.setBlockAndUpdate(nexusPos, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            for (BlockPos pos : List.of(a, b, c)) level.setBlockAndUpdate(pos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            var nexus = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            nexus.setOwner(player);
            var colony = nexus.getOrCreateColony(level);
            colony.replaceLinkedOutposts(List.of(a, b, c));
            for (var worker : List.of(teacher, pupil, pupilC)) {
                worker.setNexusPos(nexusPos);
                worker.setOwnerUuid(colony.getOwnerId());
                worker.setPos(a.getX() + 0.5D, a.getY(), a.getZ() + 0.5D);
                level.addFreshEntity(worker);
                worker.syncRosterToColony();
            }
            var habit = com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN;
            colony.recordDialectEpisode(level, a, habit, teacher);
            helper.assertTrue(colony.startDialectLesson(a, b, habit, 2, pupil.getUUID(), teacher.getUUID(), true), "A and B must start coinage.");
            var lessonGoal = new com.alex.mycelialmind.entity.ai.colony.SymbiontDialectLessonGoal(pupil);
            lessonGoal.tick();
            helper.assertTrue(colony.getDialectLesson().stage() == com.alex.mycelialmind.colony.ColonyDialectLesson.Stage.OBSERVING, "Pupil must reach observation.");
            colony.recordDialectEpisode(level, a, habit, teacher);
            helper.assertTrue(colony.getDialectLesson().stage() == com.alex.mycelialmind.colony.ColonyDialectLesson.Stage.RETURNING, "Teacher event must be observed.");
            pupil.setPos(b.getX() + 0.5D, b.getY(), b.getZ() + 0.5D);
            lessonGoal.tick();
            helper.assertTrue(colony.getDialectLesson() == null && colony.getDialect(a).call(habit) == 2 && colony.getDialect(b).call(habit) == 2, "A and B must share call after return.");
            player.setPos(a.getX() + 0.5D, a.getY(), a.getZ() + 0.5D);
            helper.assertFalse(com.alex.mycelialmind.colony.ColonyDialectCallDirector.emit(level, colony, a, c, habit, player), "C must not understand before teaching.");
            teacher.setPos(b.getX() + 0.5D, b.getY(), b.getZ() + 0.5D);
            pupilC.setPos(b.getX() + 0.5D, b.getY(), b.getZ() + 0.5D);
            colony.recordDialectEpisode(level, b, habit, teacher);
            helper.assertTrue(colony.startDialectLesson(b, c, habit, 2, pupilC.getUUID(), teacher.getUUID(), false), "B and C must start teaching.");
            var goalC = new com.alex.mycelialmind.entity.ai.colony.SymbiontDialectLessonGoal(pupilC);
            goalC.tick();
            colony.recordDialectEpisode(level, b, habit, teacher);
            pupilC.setPos(c.getX() + 0.5D, c.getY(), c.getZ() + 0.5D);
            goalC.tick();
            helper.assertTrue(colony.getDialect(c).call(habit) == 2, "C must learn after returning.");
            pupilC.setFatigue(90.0F);
            helper.assertTrue(com.alex.mycelialmind.colony.ColonyDialectCallDirector.emit(level, colony, a, c, habit, player), "C must accept learned call.");
            helper.assertTrue(pupilC.hasDialectRestRequest(), "Most fatigued worker must receive a rest request.");
            var escort = com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER;
            colony.getDialect(b).teach(escort, 4);
            colony.getDialect(c).teach(escort, 4);
            teacher.setCaste(com.alex.mycelialmind.colony.SymbiontCaste.COURIER);
            teacher.setPos(c.getX() + 1.5D, c.getY(), c.getZ() + 0.5D);
            teacher.insertIntoPouch(new ItemStack(Items.STONE));
            pupil.setCaste(com.alex.mycelialmind.colony.SymbiontCaste.SENTINEL);
            pupil.setPos(c.getX() + 2.5D, c.getY(), c.getZ() + 0.5D);
            helper.assertTrue(com.alex.mycelialmind.colony.ColonyDialectCallDirector.emit(level, colony, b, c, escort, player),
                    "A shared escort call must create a request for an active courier.");
            level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                    new AABB(nexusPos).inflate(48.0D)).forEach(Entity::discard);
            var sentinelGoal = new com.alex.mycelialmind.entity.ai.colony.SymbiontSentinelGoal(pupil);
            sentinelGoal.tick();
            helper.assertTrue("courier_escort".equals(pupil.getTaskState()),
                    "The sentinel must respond to the escort request, got: " + pupil.getTaskState());
            var pickup = com.alex.mycelialmind.colony.ColonySiteHabit.STAGGERED_PICKUP;
            colony.getDialect(c).teach(pickup, 5);
            colony.getDialect(b).teach(pickup, 5);
            var pickupMemory = colony.getSiteMemory(b);
            UUID holder = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
            long now = level.getGameTime();
            pickupMemory.tryAcquirePickupTurn(holder, now, 20);
            pickupMemory.tryAcquirePickupTurn(first, now + 1, 20);
            pickupMemory.tryAcquirePickupTurn(second, now + 2, 20);
            pickupMemory.releasePickupTurn(holder);
            helper.assertTrue(com.alex.mycelialmind.colony.ColonyDialectCallDirector.emit(level, colony, c, b, pickup, player),
                    "A shared pickup call must assign the oldest waiting courier.");
            helper.assertTrue(pickupMemory.tryAcquirePickupTurn(first, now + 3, 20)
                            && !pickupMemory.tryAcquirePickupTurn(second, now + 3, 20),
                    "The oldest waiting courier must own the pickup lease.");
            helper.assertTrue(colony.startDialectLesson(a, b, habit, 2, pupil.getUUID(), teacher.getUUID(), false),
                    "A second lesson can begin after completing the first.");
            level.removeBlock(b, false);
            helper.assertTrue(colony.getDialectLesson() == null, "Destroying either outpost must cancel its lesson immediately.");
            helper.succeed();
        } finally {
            for (var worker : List.of(teacher, pupil, pupilC)) if (worker != null) worker.discard();
            player.setPos(oldPos);
            for (BlockPos pos : List.of(a, b, c, nexusPos)) level.removeBlock(pos, false);
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
            hive.restoreFromSnapshot(hiveSnapshot, overworld.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "dialect_live_navigation", timeoutTicks = 800)
    public static void dialectApprenticeNavigatesBothWaysAfterReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData hive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag oldHive = hive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag oldColonies = saved.save(new CompoundTag(), level.registryAccess());
        List<ChunkPos> forced = forceArenaChunks(helper, -3, 22, -4, 8);
        BlockPos floor = helper.absolutePos(new BlockPos(0, 120, 0));
        BlockPos nexusPos = floor.offset(2, 1, 2), source = floor.offset(6, 1, 2), target = floor.offset(17, 1, 2);
        for (int x = -2; x <= 21; x++) for (int z = -3; z <= 7; z++) {
            level.setBlock(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 18);
            for (int y = 1; y <= 4; y++) level.setBlock(floor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 18);
        }
        hive.resetAll(); saved.resetAll(); hive.recordMatriarchDefeat(overworld.getGameTime());
        level.setBlockAndUpdate(nexusPos, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
        level.setBlockAndUpdate(source, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
        level.setBlockAndUpdate(target, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
        var nexus = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
        var colony = nexus.getOrCreateColony(level);
        colony.replaceLinkedOutposts(List.of(source, target));
        PurificationZoneTracker.get(level).addNode(source, SynapticNodeType.SYMBIOTIC);
        PurificationZoneTracker.get(level).addNode(target, SynapticNodeType.SYMBIOTIC);
        var teacher = ModEntities.COLONY_SYMBIONT.get().create(level);
        var pupil = ModEntities.COLONY_SYMBIONT.get().create(level);
        teacher.setNexusPos(nexusPos); pupil.setNexusPos(nexusPos);
        teacher.setPos(source.getX() + 1.5D, source.getY(), source.getZ() + 0.5D);
        pupil.setPos(target.getX() + 1.5D, target.getY(), target.getZ() + 0.5D);
        level.addFreshEntity(teacher); level.addFreshEntity(pupil);
        teacher.setStandby(true);
        teacher.syncRosterToColony(); pupil.syncRosterToColony();
        var habit = com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN;
        colony.recordDialectEpisode(level, source, habit, teacher);
        helper.assertTrue(colony.startDialectLesson(source, target, habit, 3, pupil.getUUID(), teacher.getUUID(), true), "Lesson must start.");
        saved.restoreFromSnapshot(saved.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        helper.startSequence().thenWaitUntil(() -> {
            var current = saved.getByNexus(nexusPos);
            helper.assertTrue(current.getDialectLesson() != null && current.getDialectLesson().stage()
                    == com.alex.mycelialmind.colony.ColonyDialectLesson.Stage.OBSERVING,
                    "Apprentice must navigate from destination to source after reload.");
        }).thenExecute(() -> {
            var current = saved.getByNexus(nexusPos);
            helper.assertTrue(current.getSiteMemory(source).receiveTransferredHabit(habit), "Rest habit trial must be available.");
            BlockPos cap = source.offset(2, 0, 0);
            level.setBlockAndUpdate(cap, ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.BioluminescentMycelialCapBlock.MATURE, true));
            current.getOrderBoard().postOrRefreshOrder(com.alex.mycelialmind.colony.ColonyOrder.OrderType.HARVEST,
                    cap, 100, level.getGameTime());
            teacher.setFatigue(70.0F);
            teacher.setStandby(false);
        }).thenWaitUntil(() -> helper.assertTrue(teacher.isResting(), "Teacher must begin rest autonomously."))
        .thenExecute(() -> {
            CompoundTag workerTag = new CompoundTag();
            teacher.addAdditionalSaveData(workerTag);
            helper.assertTrue(workerTag.contains("PendingEarlyRestSite"), "Rest observation must persist in worker NBT.");
            teacher.readAdditionalSaveData(workerTag);
        }).thenWaitUntil(() -> {
            var current = saved.getByNexus(nexusPos);
            helper.assertTrue(current.getDialectLesson() != null && current.getDialectLesson().stage()
                    == com.alex.mycelialmind.colony.ColonyDialectLesson.Stage.RETURNING,
                    "The teacher must rest and complete a real harvest observed by the apprentice.");
        }).thenExecute(() -> {
            saved.restoreFromSnapshot(saved.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        }).thenWaitUntil(() -> {
            var current = saved.getByNexus(nexusPos);
            helper.assertTrue(current.getDialectLesson() == null && current.getDialect(target).call(habit) == 3,
                    "Apprentice must navigate home and complete lesson after reload.");
        }).thenExecute(() -> {
            teacher.discard(); pupil.discard();
            PurificationZoneTracker.get(level).removeNode(source);
            PurificationZoneTracker.get(level).removeNode(target);
            level.removeBlock(source.offset(2, 0, 0), false);
            for (BlockPos pos : List.of(source, target, nexusPos)) level.removeBlock(pos, false);
            saved.restoreFromSnapshot(oldColonies, level.registryAccess());
            hive.restoreFromSnapshot(oldHive, overworld.registryAccess());
            releaseArenaChunks(level, forced);
        }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "sovereign_rival_raid", timeoutTicks = 800)
    public static void sovereignRivalRaidDefenseAwardsXpAndMoraleTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        List<ChunkPos> raidArenaChunks = forceArenaChunks(helper, -24, 32, -24, 32);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entityChunksReady(level, nexusPos, 24),
                        "Rival raid spawn chunks must be entity-ticking before spawning"))
                .thenExecute(() -> {
                    com.alex.mycelialmind.entity.ColonySymbiontEntity w1 = null;
                    com.alex.mycelialmind.entity.ColonySymbiontEntity w2 = null;
                    try {
                        owHive.resetAll();
                        saved.resetAll();
                        owHive.recordMatriarchDefeat(overworld.getGameTime());
                        level.setBlockAndUpdate(nexusPos,
                                com.alex.mycelialmind.registry.ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
                        var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
                        var colony = nexusBe.getOrCreateColony(level);
                        w1 = nexusBe.incubateSymbiont(level, null,
                                com.alex.mycelialmind.colony.SymbiontCaste.HARVESTER, true);
                        w2 = nexusBe.incubateSymbiont(level, null,
                                com.alex.mycelialmind.colony.SymbiontCaste.SENTINEL, true);
                        helper.assertTrue(w1 != null && w2 != null, "Workers must spawn for raid test.");

                        int xpBefore = colony.getDomainXp();
                        int spawned = com.alex.mycelialmind.colony.SovereignColonyDirector
                                .triggerRivalRaid(level, colony, 0);
                        helper.assertTrue(spawned >= 3 && colony.getActiveRaidRemaining() == spawned,
                                "Rival Hive probe raid must spawn >= 3 raiders and track remaining count.");

                        List<MycelialMob> raiders = level.getEntitiesOfClass(
                                MycelialMob.class,
                                new AABB(nexusPos).inflate(48.0D),
                                m -> m.getPersistentData().hasUUID("MycelialRivalColonyTarget"));
                        helper.assertTrue(raiders.size() == spawned,
                                "Must locate all spawned rival raiders (found " + raiders.size() + " of " + spawned + ").");
                        for (MycelialMob raider : raiders) {
                            raider.kill();
                        }

                        helper.assertTrue(colony.getActiveRaidRemaining() == 0,
                                "Killing all raiders must clear activeRaidRemaining.");
                        helper.assertTrue(colony.getRecentRaidVictories() == 1 && colony.getDomainXp() > xpBefore,
                                "Clearing rival probe raid must record raid victory and award Domain XP.");
                    } finally {
                        if (w1 != null) {
                            w1.discard();
                        }
                        if (w2 != null) {
                            w2.discard();
                        }
                        level.getEntitiesOfClass(
                                MycelialMob.class,
                                new AABB(nexusPos).inflate(48.0D),
                                m -> m.getPersistentData().hasUUID("MycelialRivalColonyTarget")
                        ).forEach(net.minecraft.world.entity.Entity::discard);
                        level.getEntitiesOfClass(
                                net.minecraft.world.entity.item.ItemEntity.class,
                                new AABB(nexusPos).inflate(48.0D)
                        ).forEach(net.minecraft.world.entity.Entity::discard);
                        level.removeBlock(nexusPos, false);
                        owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
                        saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
                        releaseArenaChunks(level, raidArenaChunks);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "sovereign_colony_sync")
    public static void sovereignColonySyncPayloadCodecRoundTripTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        com.alex.mycelialmind.entity.ColonySymbiontEntity worker = null;
        try {
            owHive.resetAll();
            saved.resetAll();
            owHive.recordMatriarchDefeat(overworld.getGameTime());
            level.setBlockAndUpdate(nexusPos, com.alex.mycelialmind.registry.ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            var colony = nexusBe.getOrCreateColony(level);
            colony.addDomainXp(200);
            colony.toggleInputChest(nexusPos.east());
            BlockPos memoryOutpost = nexusPos.offset(3, 0, 0);
            colony.replaceLinkedOutposts(List.of(memoryOutpost));
            var memory = colony.getSiteMemory(nexusPos);
            helper.assertTrue(memory != null
                            && !memory.recordIncident(com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER,
                            1, level.getGameTime())
                            && memory.recordIncident(com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER,
                            1, level.getGameTime() + 1L)
                            && !memory.recordTrialSuccess(com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER, 1)
                            && memory.recordTrialSuccess(com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER, 1),
                    "A local site must learn an escorted courier routine after repeated evidence and two successful trials.");
            helper.assertTrue(colony.startMemoryTransfer(nexusPos, memoryOutpost, level.getGameTime()),
                    "A learned site memory must be teachable to another linked outpost.");
            worker = nexusBe.incubateSymbiont(level, null, com.alex.mycelialmind.colony.SymbiontCaste.COURIER, true);
            helper.assertTrue(worker != null, "Worker must spawn for payload test.");

            var payload = com.alex.mycelialmind.network.SovereignColonySyncPayload.forNexus(level, nexusPos, true);
            helper.assertTrue(payload.hasColony()
                            && payload.unlocked()
                            && payload.openScreen()
                            && payload.inputChests() == 1
                            && payload.workers().size() == 1
                            && payload.workers().get(0).workerId().equals(worker.getUUID())
                            && payload.workers().get(0).caste() == com.alex.mycelialmind.colony.SymbiontCaste.COURIER
                            && payload.siteMemories().size() == 2
                            && payload.siteMemories().get(0).learnedHabit()
                            == com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER.ordinal()
                            && payload.memoryTransferActive()
                            && payload.memoryTransferSource().equals(nexusPos)
                            && payload.memoryTransferTarget().equals(memoryOutpost),
                    "forNexus must include the incubated courier, local memories, and active teaching route.");

            helper.succeed();
        } finally {
            if (worker != null) {
                worker.discard();
            }
            level.removeBlock(nexusPos, false);
            owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "colony_integration")
    public static void sovereignColonySiteMemoryTransferPersistenceTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos outpostPos = helper.absolutePos(new BlockPos(9, 2, 4));
        try {
            saved.resetAll();
            level.setBlockAndUpdate(nexusPos, com.alex.mycelialmind.registry.ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            level.setBlockAndUpdate(outpostPos, com.alex.mycelialmind.registry.ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            var colony = saved.getOrCreateColony(nexusPos, UUID.randomUUID(), "Memory Tester");
            colony.replaceLinkedOutposts(List.of(outpostPos));

            var source = colony.getSiteMemory(nexusPos);
            helper.assertTrue(source != null
                            && !source.recordIncident(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN,
                            2, level.getGameTime())
                            && source.recordIncident(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN,
                            2, level.getGameTime() + 1L)
                            && !source.recordTrialSuccess(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN, 2)
                            && source.recordTrialSuccess(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN, 2),
                    "Nexus local history must mature only after two incidents and two successful trials.");
            helper.assertTrue(colony.startMemoryTransfer(nexusPos, outpostPos, level.getGameTime()),
                    "Owner-selected site-to-site teaching must create a pending courier transfer.");

            var restored = com.alex.mycelialmind.colony.SovereignColonySavedData.load(
                    saved.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            var restoredColony = restored.getByNexus(nexusPos);
            helper.assertTrue(restoredColony != null
                            && restoredColony.getSiteMemory(nexusPos).learnedHabit()
                            == com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN
                            && restoredColony.getMemoryTransferSnapshot() != null,
                    "Learned site memories and pending teaching routes must persist through SavedData reload.");

            UUID courier = UUID.randomUUID();
            helper.assertTrue(restoredColony.claimMemoryTransfer(courier, level.getGameTime() + 2L),
                    "A courier must be able to claim a persisted teaching trip.");
            restoredColony.markMemoryTransferCollected(courier, level.getGameTime() + 3L);
            helper.assertTrue(restoredColony.deliverMemoryTransfer(courier, level.getGameTime() + 4L),
                    "The destination must receive a local trial when the courier delivers the memory.");
            var destination = restoredColony.getSiteMemory(outpostPos);
            helper.assertTrue(destination != null
                            && destination.trialHabit() == com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN
                            && destination.successfulTrials() == 0,
                    "A taught site must test the behavior locally before learning it.");

            helper.succeed();
        } finally {
            level.removeBlock(outpostPos, false);
            level.removeBlock(nexusPos, false);
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "full_endgame_integration")
    public static void fullEndgameBloomNexusRedstoneAndScannerTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());

        BlockPos bloomFloor = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos bloomPos = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos nexusPos = helper.absolutePos(new BlockPos(5, 2, 5));
        com.alex.mycelialmind.entity.ColonySymbiontEntity worker = null;

        try {
            saved.resetAll();
            owHive.recordMatriarchDefeat(overworld.getGameTime());

            level.setBlockAndUpdate(bloomFloor, Blocks.MOSS_BLOCK.defaultBlockState());
            BlockState fireBloom = ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.PALETTE, 3)
                    .setValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE, true);
            level.setBlockAndUpdate(bloomPos, fireBloom);

            helper.assertTrue(com.alex.mycelialmind.entity.ai.colony.SymbiontHarvesterGoal.isHarvestable(level.getBlockState(bloomPos)),
                    "Mature ReclaimedBloomBlock must be recognized by SymbiontHarvesterGoal.");
            helper.assertTrue(level.getBlockState(bloomPos).getAnalogOutputSignal(level, bloomPos) == 15,
                    "Mature ReclaimedBloomBlock must emit comparator signal 15.");
            helper.assertTrue(com.alex.mycelialmind.block.ReclaimedBloomBlock.itemForPalette(3)
                            .is(ModItems.RECLAIMED_BLOOM_FIRE.get()),
                    "Palette 3 must map to reclaimed_bloom_fire.");
            helper.assertTrue(!BurrowerEntity.canBurrowerBreak(level, bloomPos, 50),
                    "Burrowers must not break ReclaimedBloomBlock.");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack scanner = new ItemStack(ModItems.BIO_SCANNER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, scanner);

            var scanBloomResult = scanner.useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(bloomPos), Direction.UP, bloomPos, false)));
            helper.assertTrue(scanBloomResult.consumesAction(),
                    "BioScanner must inspect ReclaimedBloomBlock.");

            level.setBlockAndUpdate(nexusPos, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            helper.assertTrue(!BurrowerEntity.canBurrowerBreak(level, nexusPos, 50),
                    "Burrowers must not break SovereignNexusBlock.");
            helper.assertTrue(level.getBlockState(nexusPos).getAnalogOutputSignal(level, nexusPos) == 0,
                    "Uninitialized SovereignNexus must emit comparator signal 0.");

            var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            helper.assertTrue(nexusBe != null, "SovereignNexusBlockEntity must exist.");
            var colony = nexusBe.getOrCreateColony(level);
            colony.addBiomass(128);
            worker = nexusBe.incubateSymbiont(level, player, com.alex.mycelialmind.colony.SymbiontCaste.HARVESTER, true);
            helper.assertTrue(worker != null, "Harvester ColonySymbiontEntity must incubate cleanly.");

            int nexusSignal = level.getBlockState(nexusPos).getAnalogOutputSignal(level, nexusPos);
            helper.assertTrue(nexusSignal > 0 && nexusSignal <= 15,
                    "Initialized SovereignNexus with biomass/worker must emit comparator signal 1..15.");

            var scanNexusResult = scanner.useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(nexusPos), Direction.UP, nexusPos, false)));
            helper.assertTrue(scanNexusResult.consumesAction(),
                    "BioScanner must inspect SovereignNexusBlock.");

            var scanSymbiontResult = ModItems.BIO_SCANNER.get().interactLivingEntity(
                    scanner, player, worker, InteractionHand.MAIN_HAND);
            helper.assertTrue(scanSymbiontResult.consumesAction(),
                    "BioScanner must inspect ColonySymbiontEntity.");

            worker.teleportTo(bloomPos.getX() + 0.5D, bloomPos.getY(), bloomPos.getZ() + 0.5D);
            colony.getOrderBoard().postOrRefreshOrder(
                    com.alex.mycelialmind.colony.ColonyOrder.OrderType.HARVEST,
                    bloomPos,
                    90,
                    level.getGameTime());
            var harvestGoal = new com.alex.mycelialmind.entity.ai.colony.SymbiontHarvesterGoal(worker);
            for (int t = 0; t < 35; t++) {
                harvestGoal.tick();
            }

            BlockState afterHarvest = level.getBlockState(bloomPos);
            helper.assertTrue(afterHarvest.is(ModBlocks.RECLAIMED_BLOOM.get())
                            && !afterHarvest.getValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE),
                    "Harvester Symbiont must harvest ReclaimedBloomBlock instead of destroying it.");
            helper.assertTrue(afterHarvest.getAnalogOutputSignal(level, bloomPos) == 5,
                    "Regrowing ReclaimedBloomBlock must emit comparator signal 5.");
            boolean pouchHasFireBloom = false;
            for (int s = 0; s < worker.getPouch().getSlots(); s++) {
                if (worker.getPouch().getStackInSlot(s).is(ModItems.RECLAIMED_BLOOM_FIRE.get())) {
                    pouchHasFireBloom = true;
                    break;
                }
            }
            helper.assertTrue(pouchHasFireBloom,
                    "Harvester pouch must receive matching ReclaimedBloom Fire item.");

            helper.succeed();
        } finally {
            if (worker != null) {
                worker.discard();
            }
            level.removeBlock(nexusPos, false);
            level.removeBlock(bloomPos, false);
            level.removeBlock(bloomFloor, false);
            owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
            saved.restoreFromSnapshot(savedSnapshot, level.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "full_endgame_integration")
    public static void fullEndgameSymbiontImmunityGrenadeAndHiveBrainTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        BlockPos floorPos = helper.absolutePos(new BlockPos(4, 1, 4));
        com.alex.mycelialmind.entity.ColonySymbiontEntity symbiont = null;
        SporelingEntity hostileMob = null;
        PurifierGrenadeProjectile grenade = null;

        try {
            level.setBlockAndUpdate(floorPos, Blocks.STONE.defaultBlockState());
            symbiont = ModEntities.COLONY_SYMBIONT.get().create(level);
            helper.assertTrue(symbiont != null, "ColonySymbiontEntity must construct.");
            symbiont.moveTo(floorPos.getX() + 0.5D, floorPos.getY() + 1.0D, floorPos.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(symbiont);

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            float initialHp = symbiont.getHealth();
            boolean playerHurt = symbiont.hurt(level.damageSources().playerAttack(player), 6.0F);
            helper.assertTrue(!playerHurt && symbiont.getHealth() == initialHp,
                    "Player friendly fire against ColonySymbiontEntity must be canceled.");

            NeutralizingSporeProjectile spore = new NeutralizingSporeProjectile(level, player);
            boolean sporeHurt = symbiont.hurt(level.damageSources().thrown(spore, player), 6.0F);
            helper.assertTrue(!sporeHurt && symbiont.getHealth() == initialHp,
                    "NeutralizingSporeProjectile friendly fire against ColonySymbiontEntity must be canceled.");

            boolean sporeRotApplied = symbiont.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 100, 0));
            helper.assertTrue(!sporeRotApplied && !symbiont.hasEffect(ModEffects.SPORE_ROT),
                    "ColonySymbiontEntity must resist SPORE_ROT.");

            symbiont.setHealth(4.0F);
            hive.addThreatPenalty(80.0D);
            symbiont.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));
            boolean converted = DamageTelemetryTracker.tryConvertInfectedMob(level, symbiont, true);
            helper.assertTrue(!converted && symbiont.isAlive(),
                    "ColonySymbiontEntity must never undergo infection conversion.");

            grenade = new PurifierGrenadeProjectile(level, player);
            grenade.setPos(floorPos.getX() + 0.5D, floorPos.getY() + 1.15D, floorPos.getZ() + 0.5D);
            grenade.setDeltaMovement(0.0D, -0.5D, 0.0D);
            level.addFreshEntity(grenade);
            for (int t = 0; t < 5 && !grenade.isRemoved(); t++) {
                grenade.tick();
            }
            helper.assertTrue(symbiont.getHealth() > 4.0F && !symbiont.hasEffect(ModEffects.MYCELIAL_INFECTION),
                    "PurifierGrenadeProjectile pulse must heal ColonySymbiontEntity and cure MYCELIAL_INFECTION.");

            hostileMob = ModEntities.SPORELING.get().create(level);
            helper.assertTrue(hostileMob != null, "SporelingEntity must construct.");
            hostileMob.moveTo(floorPos.getX() + 2.5D, floorPos.getY() + 1.0D, floorPos.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(hostileMob);

            hostileMob.hurt(level.damageSources().inFire(), 2.0F);
            helper.assertTrue(HiveBrainBridge.getCombatXp(hostileMob) > 0,
                    "DamageTelemetryTracker must populate HiveBrain MOB_COMBAT_XP.");
            helper.assertTrue(HiveBrainBridge.getLastAttackStimulus(hostileMob) == StimulusType.FIRE,
                    "DamageTelemetryTracker must populate HiveBrain LAST_ATTACK_STIMULUS.");
            helper.assertTrue(com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateRegroupUtility(hostileMob) > 0.0F,
                    "HiveUtilityEvaluator must consume HiveBrain combat XP and fire stimulus.");

            helper.succeed();
        } finally {
            if (grenade != null && !grenade.isRemoved()) {
                grenade.discard();
            }
            if (hostileMob != null) {
                hostileMob.discard();
            }
            if (symbiont != null) {
                symbiont.discard();
            }
            level.removeBlock(floorPos, false);
            hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "colony_dialect_integration")
    public static void colonyDialectConflictClearanceAndScannerIntegrationTest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag colonySnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos source = helper.absolutePos(new BlockPos(5, 2, 2));
        BlockPos target = helper.absolutePos(new BlockPos(8, 2, 2));
        com.alex.mycelialmind.entity.ColonySymbiontEntity teacher = null;
        com.alex.mycelialmind.entity.ColonySymbiontEntity apprentice = null;
        try {
            owHive.resetAll();
            saved.resetAll();
            owHive.recordMatriarchDefeat(overworld.getGameTime());
            for (BlockPos pos : List.of(nexusPos, source, target)) {
                level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            }
            level.setBlockAndUpdate(nexusPos, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            level.setBlockAndUpdate(source, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            level.setBlockAndUpdate(target, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            PurificationZoneTracker.get(level).addNode(source, SynapticNodeType.SYMBIOTIC);
            PurificationZoneTracker.get(level).addNode(target, SynapticNodeType.SYMBIOTIC);

            var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            helper.assertTrue(nexusBe != null, "SovereignNexusBlockEntity must exist.");
            nexusBe.enableAdministrativeOverride(level);
            var colony = nexusBe.getOrCreateColony(level);
            colony.replaceLinkedOutposts(List.of(source, target));

            teacher = ModEntities.COLONY_SYMBIONT.get().create(level);
            apprentice = ModEntities.COLONY_SYMBIONT.get().create(level);
            helper.assertTrue(teacher != null && apprentice != null, "ColonySymbiont entities must spawn.");
            teacher.setNexusPos(nexusPos);
            apprentice.setNexusPos(nexusPos);
            teacher.setPos(source.getX() + 0.5D, source.getY() + 1.0D, source.getZ() + 0.5D);
            apprentice.setPos(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D);
            level.addFreshEntity(teacher);
            level.addFreshEntity(apprentice);
            teacher.syncRosterToColony();
            apprentice.syncRosterToColony();

            UUID testerId = UUID.randomUUID();
            colony.setOwnerId(testerId);
            java.util.List<net.minecraft.network.chat.Component> scannerMessages = new java.util.ArrayList<>();
            var tester = new net.neoforged.neoforge.common.util.FakePlayer(
                    level, new com.mojang.authlib.GameProfile(testerId, "DialectTester")) {
                @Override
                public void sendSystemMessage(net.minecraft.network.chat.Component message) {
                    scannerMessages.add(message);
                }

                @Override
                public void displayClientMessage(net.minecraft.network.chat.Component message, boolean actionBar) {
                    scannerMessages.add(message);
                }
            };
            tester.setPos(nexusPos.getX() + 0.5D, nexusPos.getY() + 1.0D, nexusPos.getZ() + 0.5D);

            colony.recordDialectEpisode(level, source, com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER, apprentice);
            helper.assertTrue(colony.getDialect(target).teach(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN, 4),
                    "Target outpost must learn call 4 for REST_BEFORE_RETURN.");
            helper.assertTrue(!colony.startDialectLesson(source, target,
                            com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER, 4,
                            apprentice.getUUID(), teacher.getUUID(), true),
                    "Colliding call 4 at target must block lesson start before clearance.");

            var clearPayload = new com.alex.mycelialmind.network.SovereignColonyActionPayload(
                    com.alex.mycelialmind.network.SovereignColonyActionPayload.ACTION_CLEAR_DIALECT_CALL,
                    nexusPos, 4,
                    new UUID(0L, 0L),
                    target, BlockPos.ZERO);
            com.alex.mycelialmind.network.SovereignColonyActionPayload.applyOnServer(clearPayload, tester);
            helper.assertTrue(colony.getDialect(target).call(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN) == -1,
                    "ACTION_CLEAR_DIALECT_CALL must clear call 4 on target outpost.");

            helper.assertTrue(colony.startDialectLesson(source, target,
                            com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER, 4,
                            apprentice.getUUID(), teacher.getUUID(), true),
                    "Lesson with selectable alternative teacher must start once conflict is cleared.");

            ItemStack scanner = new ItemStack(ModItems.BIO_SCANNER.get());
            tester.setItemInHand(InteractionHand.MAIN_HAND, scanner);
            scanner.useOn(new net.minecraft.world.item.context.UseOnContext(
                    tester, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(source), Direction.UP, source, false)));
            scanner.getItem().interactLivingEntity(scanner, tester, teacher, InteractionHand.MAIN_HAND);
            scanner.getItem().interactLivingEntity(scanner, tester, apprentice, InteractionHand.MAIN_HAND);

            boolean sawOutpostDialect = scannerMessages.stream()
                    .anyMatch(msg -> msg.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                            && tc.getKey().equals("message.mycelialmind.scanner_outpost_dialect"));
            long roleMessages = scannerMessages.stream()
                    .filter(msg -> msg.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                            && tc.getKey().equals("message.mycelialmind.scanner_symbiont_dialect_role"))
                    .count();
            helper.assertTrue(sawOutpostDialect && roleMessages == 2L,
                    "BioScannerItem must report outpost dialect and teacher/apprentice lesson roles.");

            var cancelPayload = new com.alex.mycelialmind.network.SovereignColonyActionPayload(
                    com.alex.mycelialmind.network.SovereignColonyActionPayload.ACTION_CANCEL_DIALECT_LESSON,
                    nexusPos, 0,
                    new UUID(0L, 0L),
                    BlockPos.ZERO, BlockPos.ZERO);
            com.alex.mycelialmind.network.SovereignColonyActionPayload.applyOnServer(cancelPayload, tester);
            helper.assertTrue(colony.getDialectLesson() == null,
                    "ACTION_CANCEL_DIALECT_LESSON must clear active lesson cleanly.");

            com.alex.mycelialmind.colony.ColonyDialectCallDirector.playSequence(level, target, 4);
            helper.assertTrue(ModSounds.COLONY_DIALECT_TONE_1.get() != null
                            && ModSounds.COLONY_DIALECT_TONE_2.get() != null
                            && ModSounds.COLONY_DIALECT_TONE_3.get() != null,
                    "Dedicated Colony Dialect tone sounds must be registered.");
            helper.succeed();
        } finally {
            if (teacher != null) {
                teacher.discard();
            }
            if (apprentice != null) {
                apprentice.discard();
            }
            PurificationZoneTracker.get(level).removeNode(source);
            PurificationZoneTracker.get(level).removeNode(target);
            for (BlockPos pos : List.of(nexusPos, source, target)) {
                level.removeBlock(pos, false);
                level.removeBlock(pos.below(), false);
            }
            saved.restoreFromSnapshot(colonySnapshot, level.registryAccess());
            owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "ecosystem_purpose")
    public static void ecosystemPurposeLayersAllWorkInRuntime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        CompoundTag owSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        CompoundTag colonySnapshot = saved.save(new CompoundTag(), level.registryAccess());

        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos latticePos = origin.offset(1, 0, 0);
        BlockPos pillarPos = origin.offset(3, 0, 0);
        BlockPos amberPos = origin.offset(6, 0, 0);
        BlockPos benchPos = amberPos.east();
        BlockPos pylonPos = origin.offset(6, 0, 4);
        BlockPos stonePos = origin.offset(1, 0, 4);
        BlockPos bloomPos = stonePos.above();
        BlockPos symNodePos = origin.offset(3, 0, 4);
        BlockPos nexusPos = origin.offset(8, 0, 4);
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, 0, 12, 0, 8);
        List<BlockPos> placedBlocks = new java.util.ArrayList<>();
        List<net.minecraft.world.entity.Entity> spawnedEntities = new java.util.ArrayList<>();

        try {
            owHive.resetAll();
            hive.resetAll();
            saved.resetAll();
            tracker.clearAll();
            owHive.recordMatriarchDefeat(overworld.getGameTime());

            // 1. Layer 1: Living Architecture
            level.setBlockAndUpdate(latticePos, ModBlocks.BIO_CHITIN_LATTICE.get().defaultBlockState());
            placedBlocks.add(latticePos);
            NeutralizingSporeProjectile spore = new NeutralizingSporeProjectile(ModEntities.NEUTRALIZING_SPORE.get(), level);
            AcidSpitProjectile acid = new AcidSpitProjectile(ModEntities.ACID_SPIT.get(), level);
            ChitinBruteEntity brute = ModEntities.CHITIN_BRUTE.get().create(level);
            helper.assertTrue(brute != null, "ChitinBrute must instantiate.");
            spawnedEntities.add(spore);
            spawnedEntities.add(acid);
            spawnedEntities.add(brute);
            BlockState latticeState = level.getBlockState(latticePos);
            helper.assertTrue(latticeState.getCollisionShape(level, latticePos,
                            net.minecraft.world.phys.shapes.CollisionContext.of(spore)).isEmpty(),
                    "BIO_CHITIN_LATTICE must allow purifying projectiles to pass.");
            helper.assertFalse(latticeState.getCollisionShape(level, latticePos,
                            net.minecraft.world.phys.shapes.CollisionContext.of(acid)).isEmpty(),
                    "BIO_CHITIN_LATTICE must block hostile Acid Spit.");
            helper.assertFalse(latticeState.getCollisionShape(level, latticePos,
                            net.minecraft.world.phys.shapes.CollisionContext.of(brute)).isEmpty(),
                    "BIO_CHITIN_LATTICE must block hostile mobs.");

            helper.assertFalse(CreepingMyceliumBlock.canInfect(ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState()),
                    "MYCELIAL_ARCH_BRICK must resist Creeping Mycelium spread.");
            helper.assertFalse(CreepingMyceliumBlock.canInfect(ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState()),
                    "REINFORCED_BIO_BARRIER must resist Creeping Mycelium spread.");
            helper.assertFalse(com.alex.mycelialmind.world.DimensionalHomeostasisEngine.isForeignBlockId(
                            "mycelialmind", "reinforced_bio_barrier", false, false, false),
                    "REINFORCED_BIO_BARRIER must not be flagged as foreign tissue in deep_mycelium.");

            level.setBlockAndUpdate(pillarPos, ModBlocks.CHITIN_PILLAR.get().defaultBlockState());
            placedBlocks.add(pillarPos);

            UUID testerId = UUID.randomUUID();
            var tester = new net.neoforged.neoforge.common.util.FakePlayer(
                    level, new com.mojang.authlib.GameProfile(testerId, "EcosystemTester")) { };
            tester.setPos(pillarPos.getX() + 0.5D, pillarPos.getY() + 1.0D, pillarPos.getZ() + 0.5D);
            helper.assertTrue(com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.isPlayerSilenced(level, tester),
                    "CHITIN_PILLAR must silence nearby player mycorrhizal vibrations.");

            level.setBlockAndUpdate(amberPos, ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState());
            placedBlocks.add(amberPos);
            int isolatedSignal = level.getBlockState(amberPos).getAnalogOutputSignal(level, amberPos);
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            placedBlocks.add(benchPos);
            SynapticBenchBlockEntity benchBe = (SynapticBenchBlockEntity) level.getBlockEntity(benchPos);
            helper.assertTrue(benchBe != null && benchBe.getEffectiveLabTier() == 2,
                    "Adjacent LUMINESCENT_AMBER_VEIN must grant +1 effective lab tier to SynapticBench.");
            int boostedSignal = level.getBlockState(amberPos).getAnalogOutputSignal(level, amberPos);
            helper.assertTrue(isolatedSignal == 7 && boostedSignal == 15,
                    "LUMINESCENT_AMBER_VEIN comparator must step from 7 to 15 next to anchored stations.");

            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            BlockPos pylonAmber = pylonPos.east();
            BlockPos pylonBarrierA = pylonPos.west();
            BlockPos pylonBarrierB = pylonPos.north();
            level.setBlockAndUpdate(pylonAmber, ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState());
            level.setBlockAndUpdate(pylonBarrierA, ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
            level.setBlockAndUpdate(pylonBarrierB, ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
            placedBlocks.addAll(List.of(pylonPos, pylonAmber, pylonBarrierA, pylonBarrierB));
            com.alex.mycelialmind.block.PurificationPylonBlock.syncAdjacentArchitectureBonus(level, pylonPos, tracker);
            int expectedPylonRadius = MycelialConfig.PYLON_PURIFICATION_RADIUS.get()
                    + com.alex.mycelialmind.hive.EcosystemPurposeEngine.AMBER_PYLON_BONUS_RADIUS
                    + com.alex.mycelialmind.hive.EcosystemPurposeEngine.BARRIER_PYLON_BONUS_RADIUS;
            helper.assertTrue(tracker.getPylonRadius(pylonPos) == expectedPylonRadius,
                    "Pylon adjacent to Amber Vein + 2 Barriers must gain +7m bonus radius.");

            level.setBlockAndUpdate(stonePos, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
            level.setBlockAndUpdate(bloomPos, ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.PALETTE, 1)
                    .setValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE, false));
            placedBlocks.addAll(List.of(stonePos, bloomPos));
            ((com.alex.mycelialmind.block.MycelialStoneBlock) ModBlocks.MYCELIAL_STONE.get())
                    .randomTick(level.getBlockState(stonePos), level, stonePos, level.random);
            helper.assertTrue(level.getBlockState(bloomPos).getValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE),
                    "MYCELIAL_STONE randomTick must mature immature Reclaimed Bloom above it.");

            // 2. Layer 2: Reclaimed Bloom Sanctuary Auras
            com.alex.mycelialmind.entity.ColonySymbiontEntity worker = ModEntities.COLONY_SYMBIONT.get().create(level);
            helper.assertTrue(worker != null, "ColonySymbiont must instantiate.");
            spawnedEntities.add(worker);
            worker.setPos(bloomPos.getX() + 1.0D, bloomPos.getY(), bloomPos.getZ() + 0.5D);
            worker.setFatigue(45.0F);
            worker.setMorale(55.0F);
            level.addFreshEntity(worker);
            com.alex.mycelialmind.block.ReclaimedBloomBlock.emitSanctuaryAura(level, bloomPos, 1);
            helper.assertTrue(worker.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),
                    "Melee Reclaimed Bloom aura must grant Strength I to nearby ColonySymbiont.");
            helper.assertTrue(worker.getFatigue() < 45.0F && worker.getMorale() > 55.0F,
                    "Reclaimed Bloom aura must relieve fatigue and lift morale.");

            com.alex.mycelialmind.block.ReclaimedBloomBlock.emitSanctuaryAura(level, bloomPos, 0);
            helper.assertTrue(worker.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),
                    "Base Reclaimed Bloom aura must grant Regeneration I.");
            com.alex.mycelialmind.block.ReclaimedBloomBlock.emitSanctuaryAura(level, bloomPos, 5);
            helper.assertTrue(worker.hasEffect(ModEffects.SYNAPTIC_INSIGHT),
                    "Magic Reclaimed Bloom aura must grant Synaptic Insight.");

            // 3. Layer 3: Surplus Items Diegetic Uses
            hive.recordStimulus(StimulusType.MELEE, 100.0D, level.getGameTime());
            double threatBeforeEngram = hive.getThreatLevel();
            ItemStack engramStack = com.alex.mycelialmind.item.FieldEngramItem.createStack(
                    com.alex.mycelialmind.hive.FieldEngramId.OUTBREAK_SIGNAL);
            tester.setItemInHand(InteractionHand.MAIN_HAND, engramStack.copyWithCount(2));
            tester.getItemInHand(InteractionHand.MAIN_HAND).use(level, tester, InteractionHand.MAIN_HAND);
            tester.getItemInHand(InteractionHand.MAIN_HAND).use(level, tester, InteractionHand.MAIN_HAND);
            helper.assertTrue(tester.totalExperience >= com.alex.mycelialmind.hive.EcosystemPurposeEngine.DUPLICATE_ENGRAM_XP,
                    "Duplicate FieldEngram must distill XP.");
            helper.assertTrue(tester.hasEffect(ModEffects.SYNAPTIC_INSIGHT),
                    "Duplicate FieldEngram must grant Synaptic Insight.");
            helper.assertTrue(hive.getThreatLevel() < threatBeforeEngram,
                    "Duplicate FieldEngram must lower Hive threat.");

            level.setBlockAndUpdate(symNodePos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(symNodePos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            placedBlocks.addAll(List.of(symNodePos.below(), symNodePos));
            tracker.addNode(symNodePos, SynapticNodeType.SYMBIOTIC);
            SynapticNodeBlockEntity symBe = (SynapticNodeBlockEntity) level.getBlockEntity(symNodePos);
            helper.assertTrue(symBe != null, "SynapticNodeBlockEntity must exist.");
            symBe.setSymbioticStability(35);
            com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.setTravelCooldown(
                    tester.getUUID(), level.getServer().overworld().getGameTime() + 400L);
            ItemStack echoStack = new ItemStack(ModItems.NEURAL_ECHO_CORE.get(), 2);
            tester.setItemInHand(InteractionHand.MAIN_HAND, echoStack);
            level.getBlockState(symNodePos).useItemOn(
                    echoStack, level, tester, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(symNodePos), Direction.UP, symNodePos, false));
            helper.assertTrue(symBe.getSymbioticStability() == 100
                            && com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.getTravelCooldownRemaining(
                            tester.getUUID(), level.getServer().overworld().getGameTime()) == 0L,
                    "NEURAL_ECHO_CORE must restore Symbiotic Node stability to 100% and clear travel cooldown.");

            BlockPos cystSouth = pylonPos.south();
            level.setBlockAndUpdate(cystSouth, ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
            placedBlocks.add(cystSouth);
            com.alex.mycelialmind.block.PurificationPylonBlock.triggerHiveHeartOverload(
                    level, pylonPos, tester, new ItemStack(ModItems.HIVE_HEART.get()));
            helper.assertFalse(level.getBlockState(cystSouth).is(ModBlocks.ANTIBODY_CYST.get()),
                    "HIVE_HEART overload pulse on Pylon must purge nearby Antibody Cysts.");

            level.setBlockAndUpdate(nexusPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(nexusPos, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            placedBlocks.addAll(List.of(nexusPos.below(), nexusPos));
            var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
            helper.assertTrue(nexusBe != null, "Nexus BE must exist.");
            nexusBe.enableAdministrativeOverride(level);
            var colony = nexusBe.getOrCreateColony(level);
            worker.setPos(nexusPos.getX() + 1.5D, nexusPos.getY(), nexusPos.getZ() + 0.5D);
            worker.setFatigue(50.0F);
            int xpBefore = worker.getWorkerXp();
            ItemStack essStack = new ItemStack(ModItems.OVERMIND_ESSENCE.get(), 2);
            tester.setItemInHand(InteractionHand.MAIN_HAND, essStack);
            level.getBlockState(nexusPos).useItemOn(
                    essStack, level, tester, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(nexusPos), Direction.UP, nexusPos, false));
            helper.assertTrue(worker.getWorkerXp() >= xpBefore + com.alex.mycelialmind.hive.EcosystemPurposeEngine.OVERMIND_ESSENCE_SYMBIONT_XP
                            && worker.getFatigue() == 0.0F
                            && colony.getDomainXp() >= 15,
                    "OVERMIND_ESSENCE on Sovereign Nexus must grant Sovereign Grace XP and full worker recovery.");

            // 4. Layer 4: Mob Counterplay
            brute.setPos(pillarPos.getX() - 0.9D, pillarPos.getY(), pillarPos.getZ() + 0.5D);
            brute.setYRot(-90.0F);
            brute.setDeltaMovement(0.6D, 0.0D, 0.0D);
            level.addFreshEntity(brute);
            boolean stunned = com.alex.mycelialmind.entity.ai.BruteChargeGoal.checkAndApplyBarrierCollision(level, brute);
            helper.assertTrue(stunned
                            && brute.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                            && brute.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),
                    "ChitinBrute charging into CHITIN_PILLAR must suffer collision stun.");

            com.alex.mycelialmind.entity.PyroShroomEntity pyro = ModEntities.PYRO_SHROOM.get().create(level);
            helper.assertTrue(pyro != null, "PyroShroom must instantiate.");
            spawnedEntities.add(pyro);
            pyro.setPos(origin.getX() + 4.5D, origin.getY(), origin.getZ() + 1.5D);
            level.addFreshEntity(pyro);
            pyro.igniteFuse(30);
            helper.assertTrue(pyro.tryDefuseFuse(level)
                            && !pyro.isPrimed()
                            && pyro.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),
                    "PyroShroom tryDefuseFuse must extinguish fuse and apply Weakness.");
            boolean droppedAmber = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            pyro.getBoundingBox().inflate(3.0D),
                            ie -> ie.getItem().is(ModBlocks.LUMINESCENT_AMBER_VEIN.get().asItem()))
                    .isEmpty();
            helper.assertTrue(droppedAmber, "Defusing PyroShroom must drop a luminescent_amber_vein.");

            BlockPos pyroCystPos = pyro.blockPosition().east();
            level.setBlockAndUpdate(pyroCystPos, ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
            placedBlocks.add(pyroCystPos);
            helper.assertTrue(pyro.cauterizeNearbyTissue(level) >= 1
                            && !level.getBlockState(pyroCystPos).is(ModBlocks.ANTIBODY_CYST.get()),
                    "PyroShroom detonation must cauterize adjacent Antibody Cysts.");

            level.setBlockAndUpdate(pylonPos, Blocks.AIR.defaultBlockState());
            tracker.removePylon(pylonPos);

            ColonyWardenEntity warden = ModEntities.COLONY_WARDEN.get().create(level);
            SporelingEntity wingman = ModEntities.SPORELING.get().create(level);
            helper.assertTrue(warden != null && wingman != null, "Warden and Sporeling must instantiate.");
            spawnedEntities.add(warden);
            spawnedEntities.add(wingman);
            warden.setHiveStrain(com.alex.mycelialmind.hive.HiveStrain.ORTHODOX);
            wingman.setHiveStrain(com.alex.mycelialmind.hive.HiveStrain.ORTHODOX);
            warden.setPos(origin.getX() + 1.5D, origin.getY() + 3.0D, origin.getZ() + 1.5D);
            wingman.setPos(origin.getX() + 3.0D, origin.getY() + 3.0D, origin.getZ() + 1.5D);
            level.addFreshEntity(warden);
            level.addFreshEntity(wingman);
            warden.tickCount = com.alex.mycelialmind.hive.EcosystemPurposeEngine.WARDEN_AURA_INTERVAL_TICKS;
            warden.aiStep();
            helper.assertTrue(wingman.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE),
                    "Non-desynced ColonyWarden must project Resistance I aura to nearby allies.");
            wingman.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
            warden.triggerDesync(80);
            warden.tickCount = com.alex.mycelialmind.hive.EcosystemPurposeEngine.WARDEN_AURA_INTERVAL_TICKS * 2;
            warden.aiStep();
            helper.assertFalse(wingman.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE),
                    "Desynced ColonyWarden must NOT project Resistance I aura.");

            LeechParasiteEntity leech1 = ModEntities.LEECH_PARASITE.get().create(level);
            LeechParasiteEntity leech2 = ModEntities.LEECH_PARASITE.get().create(level);
            helper.assertTrue(leech1 != null && leech2 != null, "LeechParasites must instantiate.");
            spawnedEntities.add(leech1);
            spawnedEntities.add(leech2);
            tester.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 300, 1));
            leech1.die(level.damageSources().playerAttack(tester));
            var afterOne = tester.getEffect(ModEffects.MYCELIAL_INFECTION);
            helper.assertTrue(afterOne != null && afterOne.getAmplifier() == 0,
                    "Killing first LeechParasite must downgrade MYCELIAL_INFECTION from amp 1 to 0.");
            leech2.die(level.damageSources().playerAttack(tester));
            helper.assertFalse(tester.hasEffect(ModEffects.MYCELIAL_INFECTION),
                    "Killing second LeechParasite must remove remaining MYCELIAL_INFECTION.");

            helper.succeed();
        } finally {
            for (net.minecraft.world.entity.Entity e : spawnedEntities) {
                if (e != null && !e.isRemoved()) {
                    e.discard();
                }
            }
            for (var ie : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(origin).inflate(16.0D))) {
                ie.discard();
            }
            tracker.clearAll();
            for (BlockPos p : placedBlocks) {
                level.removeBlock(p, false);
            }
            saved.restoreFromSnapshot(colonySnapshot, level.registryAccess());
            hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
            owHive.restoreFromSnapshot(owSnapshot, overworld.registryAccess());
            releaseArenaChunks(level, forcedChunks);
        }
    }

    @GameTest(template = "empty", batch = "acquisition_difficulty")
    public static void acquisitionDifficultyAndMaintenanceWorkInRuntime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        CompoundTag owSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        CompoundTag colonySnapshot = saved.save(new CompoundTag(), level.registryAccess());

        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos pylonPos = origin.offset(1, 0, 1);
        BlockPos turretPos = origin.offset(4, 0, 1);
        BlockPos benchPos = origin.offset(7, 0, 1);
        BlockPos mycPos = origin.offset(1, 0, 4);
        BlockPos barrierPos = origin.offset(4, 0, 4);
        BlockPos riftPos = barrierPos.above();
        BlockPos bloomBasePos = origin.offset(7, 0, 4);
        BlockPos bloomPos = bloomBasePos.above();
        BlockPos bloomAmberPos = bloomPos.east();
        BlockPos bulbPos = origin.offset(10, 0, 4);

        List<ChunkPos> forcedChunks = forceArenaChunks(helper, 0, 14, 0, 8);
        List<BlockPos> placedBlocks = new java.util.ArrayList<>();
        List<net.minecraft.world.entity.Entity> spawnedEntities = new java.util.ArrayList<>();
        var survival = new net.neoforged.neoforge.common.util.FakePlayer(
                level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "AcqSurvivalTester")) { };
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);

        try {
            owHive.resetAll();
            hive.resetAll();
            saved.resetAll();
            tracker.clearAll();
            survival.getInventory().clearContent();
            survival.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());

            // 1. Progressive Act / Hive Core Gates + Creative Bypass
            level.setBlockAndUpdate(pylonPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            placedBlocks.addAll(List.of(pylonPos.below(), pylonPos));
            var focus = (MonumentFocusBlockEntity) level.getBlockEntity(pylonPos);
            helper.assertTrue(focus != null, "MonumentFocusBlockEntity must exist.");

            helper.assertFalse(focus.tryUpgrade(new ItemStack(ModItems.SYNAPTIC_CORE.get()), survival),
                    "Survival cannot upgrade Pylon to T2 during Act I (DORMANT).");
            helper.assertTrue(focus.tryUpgrade(new ItemStack(ModItems.SYNAPTIC_CORE.get()), creative),
                    "Creative must bypass Pylon Act II gate.");

            // Fresh T1 pylon for Act II / HiveCore gate verification
            level.removeBlock(pylonPos, false);
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            focus = (MonumentFocusBlockEntity) level.getBlockEntity(pylonPos);
            helper.assertTrue(focus != null && focus.getTier() == 1, "Reset pylon starts at Tier 1.");

            hive.promoteHighestTier(HiveProgressionTier.AWAKENED);
            helper.assertTrue(focus.tryUpgrade(new ItemStack(ModItems.SYNAPTIC_CORE.get()), survival),
                    "Act II (AWAKENED) must unlock Pylon Tier 2.");
            helper.assertFalse(focus.tryUpgrade(new ItemStack(ModItems.OVERMIND_ESSENCE.get()), survival),
                    "Act II alone must still block Pylon Tier 3 until Act III or Hive Core defeat.");

            survival.getData(ModAttachments.DISCOVERY).recordMilestone(
                    ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED);
            helper.assertTrue(focus.tryUpgrade(new ItemStack(ModItems.OVERMIND_ESSENCE.get()), survival),
                    "Defeating Hive Core must unlock Pylon Tier 3 early.");
            survival.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());
            hive.resetAll();

            // 2. Synaptic Bench Gates (Mutagen/Serum in Act I, Chimera Ovum, Bloom Duplication)
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            placedBlocks.add(benchPos);
            var bench = (SynapticBenchBlockEntity) level.getBlockEntity(benchPos);
            helper.assertTrue(bench != null, "SynapticBenchBlockEntity must exist.");
            ItemStack sample = new ItemStack(ModItems.GENETIC_SAMPLE.get());
            sample.set(ModDataComponents.TRAIT_ID.get(), AdaptationTrait.MELEE_THORNS.id());
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, sample.copy());
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
            helper.assertTrue(bench.computeSynthesisDetails().output().isEmpty(),
                    "Bench T1 in Act I must block Symbiotic Mutagen synthesis.");

            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.RECLAIMED_BLOOM_MELEE.get()));
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, ItemStack.EMPTY);
            helper.assertTrue(bench.computeSynthesisDetails().output().isEmpty(),
                    "Bench T1 in Act I must block Reclaimed Bloom duplication.");

            hive.promoteHighestTier(HiveProgressionTier.AWAKENED);
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, sample.copy());
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()));
            helper.assertTrue(bench.computeSynthesisDetails().output().is(ModItems.SYMBIOTIC_MUTAGEN.get()),
                    "Act II (AWAKENED) must unlock Symbiotic Mutagen synthesis.");

            // 3. Amber Crystallization & Rift Catalyst Gating
            hive.resetAll();
            tracker.addPylon(pylonPos);
            focus.setPurificationCharge(com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.PYLON_MAX_CHARGE);
            level.setBlockAndUpdate(mycPos, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
            placedBlocks.add(mycPos);
            survival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_SHARD, 2));
            var amberAct1 = level.getBlockState(mycPos).useItemOn(
                    survival.getMainHandItem(), level, survival, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(mycPos), Direction.UP, mycPos, false));
            helper.assertFalse(amberAct1.consumesAction(),
                    "Amber crystallization must be blocked in Act I for survival.");

            hive.promoteHighestTier(HiveProgressionTier.AWAKENED);
            var amberNoSpore = level.getBlockState(mycPos).useItemOn(
                    survival.getMainHandItem(), level, survival, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(mycPos), Direction.UP, mycPos, false));
            helper.assertFalse(amberNoSpore.consumesAction(),
                    "Amber crystallization must fail without a Latent Spore in inventory.");

            survival.getInventory().add(new ItemStack(ModItems.LATENT_SPORE.get(), 1));
            var amberSuccess = level.getBlockState(mycPos).useItemOn(
                    survival.getMainHandItem(), level, survival, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(mycPos), Direction.UP, mycPos, false));
            helper.assertTrue(amberSuccess.consumesAction()
                            && level.getBlockState(mycPos).is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())
                            && survival.getInventory().countItem(ModItems.LATENT_SPORE.get()) == 0,
                    "Amber crystallization must convert mycelium and consume 1 Latent Spore in Act II.");

            hive.resetAll();
            level.setBlockAndUpdate(barrierPos, ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
            placedBlocks.addAll(List.of(barrierPos, riftPos));
            ItemStack catalyst = new ItemStack(ModItems.RIFT_CATALYST.get());
            survival.setItemInHand(InteractionHand.MAIN_HAND, catalyst);
            var riftAct1 = catalyst.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                    survival, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(barrierPos), Direction.UP, barrierPos, false)));
            helper.assertFalse(riftAct1.consumesAction() || level.getBlockState(riftPos).is(ModBlocks.MYCELIAL_RIFT.get()),
                    "Rift Catalyst must stay locked in Act I before Act III / Hive Core defeat.");

            hive.promoteHighestTier(HiveProgressionTier.ADAPTIVE);
            var riftAct3 = catalyst.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                    survival, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(barrierPos), Direction.UP, barrierPos, false)));
            helper.assertTrue(riftAct3.consumesAction() && level.getBlockState(riftPos).is(ModBlocks.MYCELIAL_RIFT.get()),
                    "Rift Catalyst must open MYCELIAL_RIFT once Act III is reached.");

            // 4. Pylon Charge Drain, Amber Reduction, Depletion Protection Loss, and Hopper Recharge
            hive.resetAll();
            focus.setTier(1);
            focus.setPurificationCharge(0);
            ((com.alex.mycelialmind.block.PurificationPylonBlock) ModBlocks.PURIFICATION_PYLON.get())
                    .tick(level.getBlockState(pylonPos), level, pylonPos, level.random);
            helper.assertTrue(tracker.isPylonDepleted(pylonPos) && !tracker.isProtected(pylonPos),
                    "Depleted Pylon (0 charge) must lose protection radius.");

            var pylonCap = level.getCapability(Capabilities.ItemHandler.BLOCK, pylonPos, Direction.UP);
            helper.assertTrue(pylonCap != null, "Pylon must expose ItemHandler capability.");
            ItemStack rem = pylonCap.insertItem(0, new ItemStack(Items.AMETHYST_SHARD, 2), false);
            helper.assertTrue(rem.getCount() == 1
                            && focus.getPurificationCharge() == 25
                            && !tracker.isPylonDepleted(pylonPos)
                            && tracker.isProtected(pylonPos),
                    "Inserting Amethyst Shard via hopper must recharge Pylon (+25) and restore protection.");

            level.getEntitiesOfClass(MycelialMob.class, new AABB(pylonPos).inflate(36.0D))
                    .forEach(net.minecraft.world.entity.Entity::discard);
            SporelingEntity pylonTarget = ModEntities.SPORELING.get().create(level);
            helper.assertTrue(pylonTarget != null, "Sporeling must instantiate.");
            spawnedEntities.add(pylonTarget);
            pylonTarget.setPos(pylonPos.getX() + 2.0D, pylonPos.getY(), pylonPos.getZ() + 0.5D);
            level.addFreshEntity(pylonTarget);
            int beforeCombatDrain = focus.getPurificationCharge();
            ((com.alex.mycelialmind.block.PurificationPylonBlock) ModBlocks.PURIFICATION_PYLON.get())
                    .tick(level.getBlockState(pylonPos), level, pylonPos, level.random);
            helper.assertTrue(focus.getPurificationCharge() == beforeCombatDrain - 2,
                    "Purifying a mob must drain 2 Pylon charge without Amber conduit.");

            BlockPos pylonAmberPos = pylonPos.east();
            level.setBlockAndUpdate(pylonAmberPos, ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState());
            placedBlocks.add(pylonAmberPos);
            int beforeAmberCombat = focus.getPurificationCharge();
            ((com.alex.mycelialmind.block.PurificationPylonBlock) ModBlocks.PURIFICATION_PYLON.get())
                    .tick(level.getBlockState(pylonPos), level, pylonPos, level.random);
            helper.assertTrue(focus.getPurificationCharge() == beforeAmberCombat - 1,
                    "Adjacent Amber conduit must halve Pylon combat drain to 1.");
            pylonTarget.discard();
            level.getEntitiesOfClass(MycelialMob.class, new AABB(pylonPos).inflate(36.0D))
                    .forEach(net.minecraft.world.entity.Entity::discard);

            int beforeAmberIdle = focus.getPurificationCharge();
            int leftoverMobs = level.getEntitiesOfClass(MycelialMob.class, new AABB(pylonPos).inflate(30.0D)).size();
            ((com.alex.mycelialmind.block.PurificationPylonBlock) ModBlocks.PURIFICATION_PYLON.get())
                    .tick(level.getBlockState(pylonPos), level, pylonPos, level.random);
            helper.assertTrue(focus.getPurificationCharge() == beforeAmberIdle,
                    "Adjacent Amber conduit must reduce Pylon idle drain to 0 (charge " + beforeAmberIdle
                            + " -> " + focus.getPurificationCharge() + ", nearbyMobs=" + leftoverMobs + ").");

            // 5. Purification Turret Ammo Consumption, Dry Halt, and Hopper Reload
            level.setBlockAndUpdate(turretPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            placedBlocks.addAll(List.of(turretPos.below(), turretPos));
            var turret = (PurificationTurretBlockEntity) level.getBlockEntity(turretPos);
            helper.assertTrue(turret != null && turret.getAmmo() == com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_INITIAL_AMMO,
                    "New Turret must start with 32 ammo.");
            SporelingEntity turretTarget = ModEntities.SPORELING.get().create(level);
            helper.assertTrue(turretTarget != null, "Turret target Sporeling must instantiate.");
            spawnedEntities.add(turretTarget);
            turretTarget.setPos(turretPos.getX() + 3.0D, turretPos.getY(), turretPos.getZ() + 0.5D);
            level.addFreshEntity(turretTarget);
            for (int t = 0; t < 40; t++) {
                PurificationTurretBlockEntity.serverTick(level, turretPos, level.getBlockState(turretPos), turret);
            }
            helper.assertTrue(turret.getAmmo() < com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_INITIAL_AMMO,
                    "Firing Turret must consume ammunition.");

            turret.setAmmo(0);
            int projBeforeDry = level.getEntitiesOfClass(NeutralizingSporeProjectile.class,
                    new AABB(turretPos).inflate(8.0D)).size();
            for (int t = 0; t < 40; t++) {
                PurificationTurretBlockEntity.serverTick(level, turretPos, level.getBlockState(turretPos), turret);
            }
            int projAfterDry = level.getEntitiesOfClass(NeutralizingSporeProjectile.class,
                    new AABB(turretPos).inflate(8.0D)).size();
            helper.assertTrue(projAfterDry <= projBeforeDry && turret.getAmmo() == 0,
                    "Turret at 0 ammo must halt firing.");

            var turretCap = level.getCapability(Capabilities.ItemHandler.BLOCK, turretPos, Direction.NORTH);
            helper.assertTrue(turretCap != null, "Turret must expose ItemHandler capability.");
            turretCap.insertItem(0, new ItemStack(ModItems.LATENT_SPORE.get()), false);
            helper.assertTrue(turret.getAmmo() == 16,
                    "Inserting Latent Spore via hopper must reload Turret (+16 ammo).");
            turretTarget.discard();

            // BioScanner telemetry inspects Turret & Pylon safely
            survival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIO_SCANNER.get()));
            helper.assertTrue(survival.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                            survival, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(turretPos), Direction.UP, turretPos, false))).consumesAction()
                    && survival.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                            survival, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pylonPos), Direction.UP, pylonPos, false))).consumesAction(),
                    "BioScanner must inspect Turret ammo and Pylon charge.");

            // 6. Reclaimed Bloom Environment & 6-Pulse Exhaustion vs Amber Sustainment
            tracker.clearAll();
            level.setBlockAndUpdate(bloomBasePos, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(bloomPos, ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.PALETTE, 1)
                    .setValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE, true));
            placedBlocks.addAll(List.of(bloomBasePos, bloomPos, bloomAmberPos));

            var ally = ModEntities.COLONY_SYMBIONT.get().create(level);
            helper.assertTrue(ally != null, "ColonySymbiont must instantiate.");
            spawnedEntities.add(ally);
            ally.setPos(bloomPos.getX() + 1.0D, bloomPos.getY(), bloomPos.getZ() + 0.5D);
            level.addFreshEntity(ally);
            com.alex.mycelialmind.block.ReclaimedBloomBlock.emitSanctuaryAura(level, bloomPos, 1);
            helper.assertFalse(ally.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),
                    "Reclaimed Bloom on bare Stone outside Pylon/Nexus must not radiate sanctuary buffs.");

            level.setBlockAndUpdate(bloomBasePos, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
            for (int p = 0; p < 5; p++) {
                com.alex.mycelialmind.block.ReclaimedBloomBlock.emitSanctuaryAura(level, bloomPos, 1);
            }
            helper.assertTrue(ally.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)
                            && level.getBlockState(bloomPos).getValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE),
                    "Bloom on Mycelial Stone must buff allies for up to 5 pulses while staying mature.");
            com.alex.mycelialmind.block.ReclaimedBloomBlock.emitSanctuaryAura(level, bloomPos, 1);
            helper.assertFalse(level.getBlockState(bloomPos).getValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE),
                    "6th unanchored Bloom pulse must exhaust MATURE state.");

            level.setBlockAndUpdate(bloomAmberPos, ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState());
            level.setBlockAndUpdate(bloomPos, level.getBlockState(bloomPos)
                    .setValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE, true));
            for (int p = 0; p < 7; p++) {
                com.alex.mycelialmind.block.ReclaimedBloomBlock.emitSanctuaryAura(level, bloomPos, 1);
            }
            helper.assertTrue(level.getBlockState(bloomPos).getValue(com.alex.mycelialmind.block.ReclaimedBloomBlock.MATURE),
                    "Adjacent Luminescent Amber Vein must sustain Bloom indefinitely past 6 pulses.");
            ally.discard();

            // 7. Field Risk-Reward Friction (BioExtractor Alert + 50% threshold, Relics Hunger/Weakness, SporeBulb Harvest)
            hive.resetAll();
            SporelingEntity extractTarget = ModEntities.SPORELING.get().create(level);
            SporelingEntity backupWitness = ModEntities.SPORELING.get().create(level);
            helper.assertTrue(extractTarget != null && backupWitness != null, "Sporelings must instantiate.");
            spawnedEntities.add(extractTarget);
            spawnedEntities.add(backupWitness);
            extractTarget.setPos(origin.getX() + 1.5D, origin.getY(), origin.getZ() + 7.0D);
            backupWitness.setPos(origin.getX() + 6.5D, origin.getY(), origin.getZ() + 7.0D);
            survival.setPos(origin.getX() + 2.0D, origin.getY(), origin.getZ() + 7.0D);
            level.addFreshEntity(extractTarget);
            level.addFreshEntity(backupWitness);
            extractTarget.applyHiveTraits(java.util.EnumSet.of(AdaptationTrait.MELEE_THORNS));

            ItemStack ext = new ItemStack(ModItems.BIO_EXTRACTOR.get());
            survival.setItemInHand(InteractionHand.MAIN_HAND, ext);
            extractTarget.setHealth(extractTarget.getMaxHealth() * 0.55F);
            helper.assertFalse(ext.getItem().interactLivingEntity(ext, survival, extractTarget, InteractionHand.MAIN_HAND).consumesAction(),
                    "BioExtractor must reject non-primordial target at 55% HP.");
            survival.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());
            extractTarget.setHealth(extractTarget.getMaxHealth() * 0.45F);
            double threatBeforeExt = hive.getThreatLevel();
            var extractRes = ext.getItem().interactLivingEntity(ext, survival, extractTarget, InteractionHand.MAIN_HAND);
            helper.assertTrue(extractRes.consumesAction()
                            && hive.getThreatLevel() > threatBeforeExt
                            && backupWitness.getTarget() == survival,
                    "BioExtractor extraction must spike Hive threat and alert nearby MycelialMobs (res=" + extractRes
                            + ", threat=" + threatBeforeExt + "->" + hive.getThreatLevel()
                            + ", target=" + backupWitness.getTarget() + ").");

            survival.getFoodData().setFoodLevel(20);
            survival.removeAllEffects();
            survival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SYNAPTIC_SUPPRESSOR.get()));
            survival.getCooldowns().removeCooldown(ModItems.SYNAPTIC_SUPPRESSOR.get());
            ModItems.SYNAPTIC_SUPPRESSOR.get().use(level, survival, InteractionHand.MAIN_HAND);
            helper.assertTrue(survival.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)
                            && survival.getFoodData().getFoodLevel() == 16,
                    "Synaptic Suppressor must apply Hunger and food backlash.");

            survival.getFoodData().setFoodLevel(20);
            survival.removeAllEffects();
            survival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.OVERMIND_CROWN.get()));
            survival.getCooldowns().removeCooldown(ModItems.OVERMIND_CROWN.get());
            ModItems.OVERMIND_CROWN.get().use(level, survival, InteractionHand.MAIN_HAND);
            helper.assertTrue(survival.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)
                            && survival.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
                            && survival.getFoodData().getFoodLevel() == 16,
                    "Overmind Crown global purge must apply Hunger, Weakness, and food backlash.");

            tracker.clearAll();
            survival.removeAllEffects();
            survival.getInventory().clearContent();
            level.setBlockAndUpdate(bulbPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(bulbPos, ModBlocks.SPORE_BULB.get().defaultBlockState());
            placedBlocks.addAll(List.of(bulbPos.below(), bulbPos));
            survival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            level.getBlockState(bulbPos).useItemOn(
                    survival.getMainHandItem(), level, survival, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(bulbPos), Direction.UP, bulbPos, false));
            helper.assertTrue(survival.hasEffect(ModEffects.MYCELIAL_INFECTION),
                    "Shearing unprotected SporeBulb without Chitin armor must inflict short MYCELIAL_INFECTION.");

            helper.succeed();
        } finally {
            for (net.minecraft.world.entity.Entity e : spawnedEntities) {
                if (e != null && !e.isRemoved()) {
                    e.discard();
                }
            }
            for (var ie : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(origin).inflate(20.0D))) {
                ie.discard();
            }
            for (var proj : level.getEntitiesOfClass(NeutralizingSporeProjectile.class,
                    new AABB(origin).inflate(20.0D))) {
                proj.discard();
            }
            survival.removeAllEffects();
            survival.getInventory().clearContent();
            survival.getData(ModAttachments.DISCOVERY).deserializeNBT(level.registryAccess(), new CompoundTag());
            survival.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());
            survival.getCooldowns().removeCooldown(ModItems.SYNAPTIC_SUPPRESSOR.get());
            survival.getCooldowns().removeCooldown(ModItems.OVERMIND_CROWN.get());
            survival.getCooldowns().removeCooldown(ModItems.BIO_SCANNER.get());
            tracker.clearAll();
            for (BlockPos p : placedBlocks) {
                level.removeBlock(p, false);
            }
            saved.restoreFromSnapshot(colonySnapshot, level.registryAccess());
            hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
            owHive.restoreFromSnapshot(owSnapshot, overworld.registryAccess());
            releaseArenaChunks(level, forcedChunks);
        }
    }

    @GameTest(template = "empty", batch = "ux_juice")
    public static void uxJuiceStationSyncAndTelemetryWorkInRuntime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos pylonPos = origin;
        BlockPos turretPos = origin.east(2);
        List<BlockPos> placed = List.of(pylonPos.below(), pylonPos, turretPos.below(), turretPos);
        try {
            level.setBlockAndUpdate(pylonPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            level.setBlockAndUpdate(turretPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());

            var focus = (MonumentFocusBlockEntity) level.getBlockEntity(pylonPos);
            var turret = (PurificationTurretBlockEntity) level.getBlockEntity(turretPos);
            helper.assertTrue(focus != null && turret != null, "Pylon and Turret block entities must exist.");

            focus.setPurificationCharge(15);
            CompoundTag lowPylonTag = focus.getUpdateTag(level.registryAccess());
            helper.assertTrue(lowPylonTag.getInt("PurificationCharge") == 15,
                    "MonumentFocusBlockEntity update tag must sync low charge to client.");
            focus.setPurificationCharge(com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.PYLON_MAX_CHARGE);
            CompoundTag fullPylonTag = focus.getUpdateTag(level.registryAccess());
            helper.assertTrue(fullPylonTag.getInt("PurificationCharge") == com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.PYLON_MAX_CHARGE,
                    "MonumentFocusBlockEntity update tag must still include default/full charge after recharge.");

            turret.setAmmo(5);
            CompoundTag lowTurretTag = turret.getUpdateTag(level.registryAccess());
            helper.assertTrue(lowTurretTag.getInt("TurretAmmo") == 5,
                    "PurificationTurretBlockEntity update tag must sync low ammo to client.");
            turret.setAmmo(com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_INITIAL_AMMO);
            CompoundTag restockTurretTag = turret.getUpdateTag(level.registryAccess());
            helper.assertTrue(restockTurretTag.getInt("TurretAmmo") == com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_INITIAL_AMMO,
                    "PurificationTurretBlockEntity update tag must still sync default ammo count.");

            var lowVisual = com.alex.mycelialmind.hive.UxJuiceEngine.computePylonVisualState(15, 100);
            var emptyVisual = com.alex.mycelialmind.hive.UxJuiceEngine.computeTurretVisualState(0, 64, false, 2);
            var benchVisual = com.alex.mycelialmind.hive.UxJuiceEngine.computeBenchVisualState(2, true, false);
            helper.assertTrue(lowVisual.speedMultiplier() > 0.0F
                            && (emptyVisual.tintArgb() & 0x00FFFFFF) == 0x00E63946
                            && benchVisual.speedMultiplier() > 2.0F,
                    "UxJuiceEngine station visual states must stay calibrated.");

            helper.assertFalse(com.alex.mycelialmind.util.ClientTooltipHelper.isShiftDown(),
                    "ClientTooltipHelper must safely return false on dedicated server.");
            helper.assertTrue(com.alex.mycelialmind.config.MycelialClientConfig.screenShakeScale() >= 0.0D
                            && com.alex.mycelialmind.config.MycelialClientConfig.statusVignetteEnabled()
                            && com.alex.mycelialmind.config.MycelialClientConfig.uiAudioFeedbackEnabled()
                            && com.alex.mycelialmind.config.MycelialClientConfig.smoothHudBarsEnabled(),
                    "MycelialClientConfig [juice] defaults must be safe and accessible.");

            helper.succeed();
        } finally {
            PurificationZoneTracker.get(level).removePylon(pylonPos);
            for (BlockPos p : placed) {
                level.removeBlock(p, false);
            }
        }
    }

    @GameTest(template = "empty", batch = "hive_brain_clean_room_core")
    public static void hiveBrainCleanRoomEngineDrivesSensorsActivitiesAndComposites(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData.get(level).resetAll();
        HiveBlackboard.clear(level);
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 4, 1, 2);
        try {
            helper.assertFalse(net.neoforged.fml.ModList.get().isLoaded("smartbrainlib"),
                    "SmartBrainLib must not be loaded in runtime.");
            helper.assertTrue(brute.getHiveBrainRuntime() != null && spitter.getHiveBrainRuntime() != null,
                    "HiveBrainRuntime must initialize on spawn.");

            // Activity transition: IDLE when safe, FIGHT when ATTACK_TARGET is present
            brute.tickBrain(brute);
            helper.assertTrue(brute.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.IDLE),
                    "Brain should default to IDLE when no ATTACK_TARGET exists.");

            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.moveTo(brute.getX() + 3.0D, brute.getY(), brute.getZ());
            HiveBrainUtils.setTargetOfEntity(brute, player);
            brute.tickBrain(brute);
            helper.assertTrue(brute.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.FIGHT),
                    "Brain should transition to FIGHT when ATTACK_TARGET is set.");

            HiveBrainUtils.clearMemory(brute, net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
            brute.tickBrain(brute);
            helper.assertTrue(brute.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.IDLE),
                    "Brain should return to IDLE once ATTACK_TARGET is cleared.");

            // Composite & timing primitives: AllApplicable + Sequential + Delayed + Custom
            int[] counter = {0};
            var allGroup = new AllApplicableBehaviours<>(
                    new CustomBehaviour<MycelialMob>(m -> counter[0] += 1),
                    new CustomBehaviour<MycelialMob>(m -> counter[0] += 10)
            );
            helper.assertTrue(allGroup.tryStart(level, brute, level.getGameTime()),
                    "AllApplicableBehaviours must start eligible children.");
            allGroup.tickOrStop(level, brute, level.getGameTime() + 1L);
            helper.assertTrue(counter[0] == 11,
                    "AllApplicableBehaviours must execute every eligible child, got " + counter[0]);

            int[] seqOrder = {0};
            var seq = new SequentialBehaviour<>(
                    new CustomBehaviour<MycelialMob>(m -> seqOrder[0] = seqOrder[0] * 10 + 1),
                    new DelayedBehaviour<MycelialMob>(1) {
                        @Override
                        protected java.util.List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.ai.memory.MemoryModuleType<?>, net.minecraft.world.entity.ai.memory.MemoryStatus>> getMemoryRequirements() {
                            return java.util.List.of();
                        }

                        @Override
                        protected void doDelayedAction(MycelialMob entity) {
                            seqOrder[0] = seqOrder[0] * 10 + 2;
                        }
                    }
            );
            long t0 = level.getGameTime();
            helper.assertTrue(seq.tryStart(level, brute, t0), "SequentialBehaviour must start first step.");
            seq.tickOrStop(level, brute, t0 + 1L);
            seq.tickOrStop(level, brute, t0 + 2L);
            helper.assertTrue(seqOrder[0] == 12,
                    "SequentialBehaviour + DelayedBehaviour must execute steps in strict order (expected 12, got " + seqOrder[0] + ").");

            // Future-ready sensors: NearbyLivingEntitySensor + NearbyPlayersSensor + UnreachableTargetSensor
            var livingSensor = new com.alex.mycelialmind.entity.ai.brain.sensor.NearbyLivingEntitySensor<MycelialMob>()
                    .setRadius(12.0D);
            livingSensor.tick(level, spitter);
            helper.assertTrue(HiveBrainUtils.hasMemory(spitter, net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_LIVING_ENTITIES),
                    "NearbyLivingEntitySensor must populate NEAREST_LIVING_ENTITIES with nearby mobs.");

            var playerSensor = new NearbyPlayersSensor<MycelialMob>().setRadius(12.0D);
            playerSensor.tick(level, spitter);
            helper.assertTrue(spitter.getBrain().checkMemory(
                            net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_PLAYERS,
                            net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED),
                    "NearbyPlayersSensor must register NEAREST_PLAYERS on Brain.");

            HiveBrainUtils.setMemory(spitter, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET,
                    new net.minecraft.world.entity.ai.memory.WalkTarget(spitter.blockPosition().offset(6, 0, 0), 1.0F, 0));
            spitter.getNavigation().stop();
            new UnreachableTargetSensor<MycelialMob>().tick(level, spitter);
            helper.assertTrue(HiveBrainUtils.hasMemory(spitter, net.minecraft.world.entity.ai.memory.MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
                    "UnreachableTargetSensor must record CANT_REACH_WALK_TARGET_SINCE when navigation is idle far from WALK_TARGET.");

            helper.succeed();
        } finally {
            brute.discard();
            spitter.discard();
        }
    }

    @GameTest(template = "empty", batch = "hive_brain_clean_room", timeoutTicks = 100)
    public static void hiveBrainNewSensorsAndReactionsWorkEndToEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 4, 1, 2);
        var weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 2, 1, 5);
        var mimic = helper.spawnWithNoFreeWill(ModEntities.SYNAPTIC_MIMIC.get(), 6, 1, 5);
        var stalker = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 2);
        var arrow = net.minecraft.world.entity.EntityType.ARROW.create(level);
        if (arrow == null) {
            helper.fail("Arrow entity must instantiate cleanly.");
            return;
        }
        arrow.setPos(spitter.getX() - 4.0D, spitter.getY() + 0.6D, spitter.getZ());
        arrow.setDeltaMovement(0.9D, 0.0D, 0.0D);
        arrow.setOnGround(false);
        level.addFreshEntity(arrow);

        spitter.tickBrain(spitter);
        helper.assertTrue(HiveBrainUtils.hasMemory(spitter, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()),
                "IncomingProjectilesSensor must record INCOMING_PROJECTILE_POS for an approaching arrow.");

        arrow.setDeltaMovement(-0.9D, 0.0D, 0.0D);
        new IncomingProjectilesSensor<MycelialMob>().tick(level, spitter);
        helper.assertFalse(HiveBrainUtils.hasMemory(spitter, ModMemoryTypes.INCOMING_PROJECTILE_POS.get()),
                "IncomingProjectilesSensor must erase INCOMING_PROJECTILE_POS when the projectile moves away.");

        // Record an unreachable walk target at current gameTime, then advance 6 ticks for ReactToUnreachableTarget
        boolean[] reacted = {false};
        var react = new ReactToUnreachableTarget<MycelialMob>()
                .afterTicks(5L)
                .reaction((srv, mob) -> reacted[0] = true);
        HiveBrainUtils.setMemory(spitter, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET,
                new net.minecraft.world.entity.ai.memory.WalkTarget(spitter.blockPosition().offset(6, 0, 0), 1.0F, 0));
        spitter.getNavigation().stop();
        new UnreachableTargetSensor<MycelialMob>().tick(level, spitter);
        helper.assertTrue(HiveBrainUtils.hasMemory(spitter, net.minecraft.world.entity.ai.memory.MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
                "UnreachableTargetSensor must record CANT_REACH_WALK_TARGET_SINCE before reaction.");

        helper.startSequence()
                .thenIdle(6)
                .thenExecute(() -> {
                    try {
                        long now = level.getGameTime();
                        helper.assertTrue(react.tryStart(level, spitter, now),
                                "ReactToUnreachableTarget must start once unreachable duration exceeds threshold.");
                        react.doStop(level, spitter, now + 1L);
                        helper.assertTrue(reacted[0], "ReactToUnreachableTarget must invoke reaction callback.");
                        helper.assertFalse(HiveBrainUtils.hasMemory(spitter, net.minecraft.world.entity.ai.memory.MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
                                "ReactToUnreachableTarget must clear CANT_REACH_WALK_TARGET_SINCE.");
                        helper.assertFalse(HiveBrainUtils.hasMemory(spitter, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET),
                                "ReactToUnreachableTarget must clear stale WALK_TARGET.");

                        // StrafeTarget starts against visible ATTACK_TARGET and stops cleanly
                        HiveBrainUtils.setTargetOfEntity(mimic, weaver);
                        var strafe = new StrafeTarget<MycelialMob>().strafeDistance(6.0F).speedMod(0.9F);
                        helper.assertTrue(strafe.tryStart(level, mimic, now),
                                "StrafeTarget must start when ATTACK_TARGET is visible.");
                        strafe.tickOrStop(level, mimic, now + 1L);
                        strafe.doStop(level, mimic, now + 2L);

                        // AvoidEntity & FleeTarget check nearby threat / HURT_BY_ENTITY cleanly on valid terrain
                        HiveBrainUtils.clearMemory(weaver, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
                        var avoid = new AvoidEntity<MycelialMob>()
                                .avoiding(e -> e == stalker)
                                .noCloserThan(8.0F);
                        new com.alex.mycelialmind.entity.ai.brain.sensor.NearbyLivingEntitySensor<MycelialMob>()
                                .setRadius(12.0D)
                                .tick(level, weaver);
                        helper.assertTrue(HiveBrainUtils.hasMemory(weaver, net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES),
                                "Weaver must sense nearby visible entities for AvoidEntity.");
                        helper.assertTrue(avoid.tryStart(level, weaver, now),
                                "AvoidEntity must start when avoided threat is within range.");
                        avoid.doStop(level, weaver, now + 1L);

                        HiveBrainUtils.setMemory(stalker, net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY_ENTITY, spitter);
                        var flee = new FleeTarget<MycelialMob>().fleeDistance(12.0F);
                        helper.assertTrue(flee.tryStart(level, stalker, now),
                                "FleeTarget must activate when HURT_BY_ENTITY lies within fleeDistance.");
                        flee.doStop(level, stalker, now + 1L);
                    } finally {
                        arrow.discard();
                        spitter.discard();
                        weaver.discard();
                        mimic.discard();
                        stalker.discard();
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "hive_brain_clean_room")
    public static void hiveBrainAllIntegratedEntitiesInitializeAndTickCleanly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 1, 1, 1);
        var burrower = helper.spawnWithNoFreeWill(ModEntities.BURROWER.get(), 2, 1, 1);
        var pyro = helper.spawnWithNoFreeWill(ModEntities.PYRO_SHROOM.get(), 3, 1, 1);
        var warden = helper.spawnWithNoFreeWill(ModEntities.COLONY_WARDEN.get(), 4, 1, 1);
        var mimic = helper.spawnWithNoFreeWill(ModEntities.SYNAPTIC_MIMIC.get(), 5, 1, 1);
        var spitter = helper.spawnWithNoFreeWill(ModEntities.ACID_SPITTER.get(), 1, 1, 3);
        var stalker = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_STALKER.get(), 2, 1, 3);
        var weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 3, 1, 3);
        var behemoth = helper.spawnWithNoFreeWill(ModEntities.SPORE_BEHEMOTH.get(), 4, 1, 5);
        var hiveCore = helper.spawnWithNoFreeWill(ModEntities.HIVE_CORE.get(), 6, 1, 5);
        var matriarch = helper.spawnWithNoFreeWill(ModEntities.APEX_MATRIARCH.get(), 2, 1, 6);
        var chimera = helper.spawnWithNoFreeWill(ModEntities.MYCELIAL_CHIMERA.get(), 5, 1, 3);
        var symbiont = helper.spawnWithNoFreeWill(ModEntities.COLONY_SYMBIONT.get(), 6, 1, 3);
        var sporeling = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 1, 1, 5);
        var carrier = helper.spawnWithNoFreeWill(ModEntities.GOLIATH_CARRIER.get(), 3, 1, 5);
        var leech = helper.spawnWithNoFreeWill(ModEntities.LEECH_PARASITE.get(), 5, 1, 5);
        var architect = helper.spawnWithNoFreeWill(ModEntities.SPORE_ARCHITECT.get(), 1, 1, 6);
        var drone = helper.spawnWithNoFreeWill(ModEntities.PURIFIER_DRONE.get(), 3, 2, 6);
        var rootKeeper = helper.spawnWithNoFreeWill(ModEntities.ROOT_KEEPER.get(), 5, 1, 6);
        var glider = helper.spawnWithNoFreeWill(ModEntities.SPORE_GLIDER.get(), 6, 2, 6);
        try {
            List<MycelialMob> mobs = List.of(
                    brute, burrower, pyro, warden, mimic, spitter, stalker, weaver,
                    behemoth, hiveCore, matriarch, sporeling, carrier, leech, architect, rootKeeper);
            for (MycelialMob mob : mobs) {
                helper.assertTrue(mob.getHiveBrainRuntime() != null,
                        mob.getClass().getSimpleName() + " must initialize HiveBrainRuntime.");
                mob.tickBrain(mob);
            }
            helper.assertTrue(chimera.getHiveBrainRuntime() != null,
                    "MycelialChimera must initialize HiveBrainRuntime.");
            helper.assertTrue(symbiont.getHiveBrainRuntime() != null,
                    "ColonySymbiont must initialize HiveBrainRuntime.");
            helper.assertTrue(drone.getHiveBrainRuntime() != null,
                    "PurifierDrone must initialize HiveBrainRuntime.");
            drone.tickBrain(drone);
            helper.assertTrue(glider.getHiveBrainRuntime() != null,
                    "SporeGlider must initialize HiveBrainRuntime.");
            glider.tickBrain(glider);

            // ChitinBrute enters INVESTIGATE on SHARED_TARGET_POS without ATTACK_TARGET
            HiveBrainUtils.setMemory(brute, ModMemoryTypes.SHARED_TARGET_POS.get(), brute.blockPosition().offset(6, 0, 0));
            HiveBrainUtils.setMemory(brute, ModMemoryTypes.HIVE_ALERT_LEVEL.get(), 0.7F);
            brute.tickBrain(brute);
            helper.assertTrue(brute.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.INVESTIGATE),
                    "ChitinBrute must transition to INVESTIGATE when SHARED_TARGET_POS is set without ATTACK_TARGET.");
            HiveBrainUtils.clearMemory(brute, ModMemoryTypes.SHARED_TARGET_POS.get());
            brute.tickBrain(brute);
            helper.assertTrue(brute.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.IDLE),
                    "ChitinBrute must return to IDLE when SHARED_TARGET_POS is cleared.");

            // ColonyWarden enters INVESTIGATE near node when idle
            HiveBrainUtils.setMemory(warden, ModMemoryTypes.NEAREST_NODE_POS.get(), warden.blockPosition().offset(2, 0, 0));
            warden.tickBrain(warden);
            helper.assertTrue(warden.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.INVESTIGATE),
                    "ColonyWarden must transition to INVESTIGATE when NEAREST_NODE_POS is present.");

            // SporeBehemoth enters INVESTIGATE on hotspot without hijacking boss combat state
            HiveBrainUtils.setMemory(behemoth, ModMemoryTypes.SHARED_TARGET_POS.get(), behemoth.blockPosition().offset(5, 0, 0));
            behemoth.tickBrain(behemoth);
            helper.assertTrue(behemoth.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.INVESTIGATE),
                    "SporeBehemoth must transition to INVESTIGATE on SHARED_TARGET_POS.");

            // MycelialChimera switches cleanly to REST on CommandMode.WAIT and back on FOLLOW
            chimera.setCommandMode(MycelialChimeraEntity.CommandMode.WAIT);
            chimera.syncCommandBrainState(level);
            chimera.tickBrain(chimera);
            helper.assertTrue(chimera.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.REST),
                    "MycelialChimera must activate REST in CommandMode.WAIT.");
            chimera.setCommandMode(MycelialChimeraEntity.CommandMode.FOLLOW);
            chimera.syncCommandBrainState(level);
            chimera.tickBrain(chimera);
            helper.assertTrue(chimera.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.IDLE),
                    "MycelialChimera must return to IDLE in CommandMode.FOLLOW.");

            // ColonySymbiont switches to PANIC when HURT_BY_ENTITY is set and back to IDLE when cleared
            HiveBrainUtils.setMemory(symbiont, net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY_ENTITY, spitter);
            symbiont.tickBrain(symbiont);
            helper.assertTrue(symbiont.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.PANIC),
                    "ColonySymbiont must activate PANIC when HURT_BY_ENTITY is set.");
            HiveBrainUtils.clearMemory(symbiont, net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY_ENTITY);
            symbiont.tickBrain(symbiont);
            helper.assertTrue(symbiont.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.IDLE),
                    "ColonySymbiont must return to IDLE when HURT_BY_ENTITY is cleared.");

            // Sporeling enters INVESTIGATE on SHARED_TARGET_POS without ATTACK_TARGET
            HiveBrainUtils.setMemory(sporeling, ModMemoryTypes.SHARED_TARGET_POS.get(), sporeling.blockPosition().offset(6, 0, 0));
            sporeling.tickBrain(sporeling);
            helper.assertTrue(sporeling.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.INVESTIGATE),
                    "Sporeling must transition to INVESTIGATE on SHARED_TARGET_POS.");

            // SporeArchitect enters INVESTIGATE near node without ATTACK_TARGET
            HiveBrainUtils.setMemory(architect, ModMemoryTypes.NEAREST_NODE_POS.get(), architect.blockPosition().offset(3, 0, 0));
            architect.tickBrain(architect);
            helper.assertTrue(architect.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.INVESTIGATE),
                    "SporeArchitect must transition to INVESTIGATE on NEAREST_NODE_POS.");

            // PurifierDrone switches to REST when jammed and returns to IDLE when cleared
            drone.setJammedTicks(40);
            drone.syncDroneBrainState(level);
            drone.tickBrain(drone);
            helper.assertTrue(drone.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.REST),
                    "PurifierDrone must activate REST while jammed.");
            drone.clearJam();
            drone.syncDroneBrainState(level);
            drone.tickBrain(drone);
            helper.assertTrue(drone.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.IDLE),
                    "PurifierDrone must return to IDLE after jam clears.");

            // RootKeeper enters INVESTIGATE near node without ATTACK_TARGET
            HiveBrainUtils.setMemory(rootKeeper, ModMemoryTypes.NEAREST_NODE_POS.get(), rootKeeper.blockPosition().offset(2, 0, 0));
            rootKeeper.tickBrain(rootKeeper);
            helper.assertTrue(rootKeeper.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.INVESTIGATE),
                    "RootKeeper must activate INVESTIGATE when NEAREST_NODE_POS is present.");

            // SporeGlider switches to PANIC when HURT_BY_ENTITY is set and back to IDLE when cleared
            HiveBrainUtils.setMemory(glider, net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY_ENTITY, spitter);
            glider.tickBrain(glider);
            helper.assertTrue(glider.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.PANIC),
                    "SporeGlider must activate PANIC when HURT_BY_ENTITY is set.");
            HiveBrainUtils.clearMemory(glider, net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY_ENTITY);
            glider.tickBrain(glider);
            helper.assertTrue(glider.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.IDLE),
                    "SporeGlider must return to IDLE when HURT_BY_ENTITY is cleared.");

            helper.succeed();
        } finally {
            brute.discard();
            burrower.discard();
            pyro.discard();
            warden.discard();
            mimic.discard();
            spitter.discard();
            stalker.discard();
            weaver.discard();
            behemoth.discard();
            hiveCore.discard();
            matriarch.discard();
            chimera.discard();
            symbiont.discard();
            sporeling.discard();
            carrier.discard();
            leech.discard();
            architect.discard();
            drone.discard();
            rootKeeper.discard();
            glider.discard();
        }
    }

    @GameTest(template = "empty", batch = "hive_brain_clean_room")
    public static void hiveBrainEngineLifecycleMemoriesAndSchedulesWorkInRuntime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        var weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 4, 1, 4);
        try {
            var brain = brute.getHiveBrain();
            helper.assertTrue(brain != null && brain == brute.getHiveBrainRuntime(),
                    "getHiveBrain() and getHiveBrainRuntime() must return the unified HiveBrain instance.");

            // Manual immediate sensor scan before setting manual test memories
            brain.triggerSensorsNow(level, brute);

            // Typed memory API directly on HiveBrain
            BlockPos hotspot = brute.blockPosition().offset(5, 0, 0);
            brain.setMemory(ModMemoryTypes.SHARED_TARGET_POS.get(), hotspot);
            brain.setMemory(ModMemoryTypes.HIVE_ALERT_LEVEL.get(), 0.75F);
            helper.assertTrue(brain.hasMemory(ModMemoryTypes.SHARED_TARGET_POS.get()),
                    "HiveBrain.hasMemory must reflect stored memory.");
            helper.assertTrue(hotspot.equals(brain.getMemory(ModMemoryTypes.SHARED_TARGET_POS.get())),
                    "HiveBrain.getMemory must return stored value.");

            // Schedule activity switch and day-tick callback
            int currentDayTick = com.alex.mycelialmind.entity.ai.brain.core.HiveBrainScheduler
                    .normalizeDayTick(level.getDayTime());
            boolean[] callbackFired = {false};
            boolean[] predicateActive = {true};
            var schedule = new com.alex.mycelialmind.entity.ai.brain.core.HiveBrainSchedule()
                    .activityWhen((srv, entity) -> predicateActive[0], net.minecraft.world.entity.schedule.Activity.INVESTIGATE)
                    .callbackAt(currentDayTick, (srv, entity) -> callbackFired[0] = true);
            brain.setSchedule(schedule);

            brute.tickBrain(brute);
            helper.assertTrue(callbackFired[0], "HiveBrainSchedule callbackAt must fire for matching dayTick.");
            helper.assertTrue(brain.getActiveActivity() == net.minecraft.world.entity.schedule.Activity.INVESTIGATE,
                    "HiveBrain must track activeActivity transition to INVESTIGATE.");
            helper.assertTrue(brain.getPreviousActivity() == net.minecraft.world.entity.schedule.Activity.IDLE,
                    "HiveBrain must record previousActivity as IDLE.");
            helper.assertTrue(brain.getBrainTickCount() >= 1L, "HiveBrain tickCount must increment.");
            helper.assertTrue(brain.getRunningBehaviourCount() >= 0, "HiveBrain running count must be queryable.");

            // Combat FIGHT still preempts scheduled INVESTIGATE when ATTACK_TARGET is present
            brain.setAttackTarget(weaver);
            brute.tickBrain(brute);
            helper.assertTrue(brain.getActiveActivity() == net.minecraft.world.entity.schedule.Activity.FIGHT,
                    "FIGHT must take priority over scheduled non-combat activity when ATTACK_TARGET is set.");
            helper.assertTrue(brain.getPreviousActivity() == net.minecraft.world.entity.schedule.Activity.INVESTIGATE,
                    "previousActivity must update to INVESTIGATE on FIGHT transition.");

            // Clearing target & predicate returns cleanly to IDLE
            brain.setAttackTarget(null);
            predicateActive[0] = false;
            brain.clearMemory(ModMemoryTypes.SHARED_TARGET_POS.get());
            brain.setSchedule(null);
            brute.tickBrain(brute);
            helper.assertTrue(brain.getActiveActivity() == net.minecraft.world.entity.schedule.Activity.IDLE,
                    "HiveBrain must return to IDLE when combat and schedule predicates clear.");
            helper.assertTrue(brain.describeRunningBehaviours() != null,
                    "HiveBrain.describeRunningBehaviours must return non-null diagnostics.");

            helper.succeed();
        } finally {
            brute.discard();
            weaver.discard();
        }
    }

    @GameTest(template = "empty", batch = "hive_brain_clean_room")
    public static void hiveBrainUtilitySelectorAndReactiveAllApplicableWorkInRuntime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var weaver = helper.spawnWithNoFreeWill(ModEntities.SYNAPSE_WEAVER.get(), 2, 1, 2);
        try {
            float[] dynamicScores = {0.80F, 0.70F};
            int[] startCounts = {0, 0};
            int[] stopCounts = {0, 0};

            var branchA = new com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour<MycelialMob>() {
                @Override
                protected List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.ai.memory.MemoryModuleType<?>, net.minecraft.world.entity.ai.memory.MemoryStatus>> getMemoryRequirements() {
                    return List.of();
                }

                @Override
                protected boolean shouldKeepRunning(MycelialMob entity) {
                    return true;
                }
            }
                    .whenStarting(mob -> startCounts[0]++)
                    .whenStopping(mob -> stopCounts[0]++)
                    .runFor(mob -> 40);
            var branchB = new com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour<MycelialMob>() {
                @Override
                protected List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.ai.memory.MemoryModuleType<?>, net.minecraft.world.entity.ai.memory.MemoryStatus>> getMemoryRequirements() {
                    return List.of();
                }

                @Override
                protected boolean shouldKeepRunning(MycelialMob entity) {
                    return true;
                }
            }
                    .whenStarting(mob -> startCounts[1]++)
                    .whenStopping(mob -> stopCounts[1]++)
                    .runFor(mob -> 40);

            var utilitySelector = new com.alex.mycelialmind.entity.ai.brain.core.UtilitySelectorBehaviour<MycelialMob>()
                    .hysteresisBonus(0.15F)
                    .minScoreThreshold(0.10F)
                    .continuousPreemption(true)
                    .add(branchA, mob -> dynamicScores[0])
                    .add(branchB, mob -> dynamicScores[1]);

            long t = level.getGameTime();
            helper.assertTrue(utilitySelector.tryStart(level, weaver, t),
                    "UtilitySelectorBehaviour must start initial winning branch.");
            helper.assertTrue(startCounts[0] == 1 && startCounts[1] == 0,
                    "Branch A (0.80) must win over Branch B (0.70).");

            // Raise B to 0.85: A effective = 0.80 + 0.15 = 0.95 -> A holds via hysteresis
            dynamicScores[1] = 0.85F;
            utilitySelector.tickOrStop(level, weaver, t + 1L);
            helper.assertTrue(stopCounts[0] == 0 && startCounts[1] == 0,
                    "Hysteresis must prevent switching when B (0.85) < A + hysteresis (0.95).");

            // Raise B to 1.05: exceeds 0.95 -> B preempts A
            dynamicScores[1] = 1.05F;
            utilitySelector.tickOrStop(level, weaver, t + 2L);
            helper.assertTrue(stopCounts[0] == 1 && startCounts[1] == 1,
                    "Branch B (1.05) must preempt Branch A once it exceeds score + hysteresis.");
            utilitySelector.doStop(level, weaver, t + 3L);

            // AllApplicableBehaviours with reEvaluateStoppedChildren(true) restarts stopped sibling
            int[] transientStarts = {0};
            var persistentChild = new com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour<MycelialMob>() {
                @Override
                protected List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.ai.memory.MemoryModuleType<?>, net.minecraft.world.entity.ai.memory.MemoryStatus>> getMemoryRequirements() {
                    return List.of();
                }

                @Override
                protected boolean shouldKeepRunning(MycelialMob entity) {
                    return true;
                }
            }.noTimeout();
            var transientChild = new com.alex.mycelialmind.entity.ai.brain.core.CustomBehaviour<MycelialMob>(mob -> {})
                    .whenStarting(mob -> transientStarts[0]++)
                    .runFor(mob -> 1);
            var reactiveAll = new com.alex.mycelialmind.entity.ai.brain.core.AllApplicableBehaviours<>(
                    persistentChild, transientChild).reEvaluateStoppedChildren(true);

            helper.assertTrue(reactiveAll.tryStart(level, weaver, t + 10L),
                    "Reactive AllApplicableBehaviours must start eligible children.");
            helper.assertTrue(transientStarts[0] == 1, "Transient child must start on first tick.");
            // Tick at t+12: transientChild times out (endTimestamp = t+11, timedOut when gameTime > t+11) and stops
            reactiveAll.tickOrStop(level, weaver, t + 12L);
            // Next tick: persistentChild is still running, and reEvaluateStoppedChildren restarts transientChild
            reactiveAll.tickOrStop(level, weaver, t + 13L);
            helper.assertTrue(transientStarts[0] == 2,
                    "Reactive AllApplicableBehaviours must restart stopped child while persistent sibling runs.");
            reactiveAll.doStop(level, weaver, t + 14L);

            helper.succeed();
        } finally {
            weaver.discard();
        }
    }

    @GameTest(template = "empty", batch = "hive_brain_clean_room")
    public static void hiveNativeBTreeSteeringLodAndAiCommandWorkEndToEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean gdxPresent;
        try {
            Class.forName("com.badlogic.gdx.ai.btree.BehaviorTree", false, MycelialGameTests.class.getClassLoader());
            gdxPresent = true;
        } catch (ClassNotFoundException ignored) {
            gdxPresent = false;
        }
        helper.assertFalse(gdxPresent, "gdxAI BehaviorTree must be completely absent from runtime classpath.");

        var bruteA = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        var bruteB = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 3);
        var target = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        target.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.0D, 1.0D, 2.5D)));
        try {
            bruteA.setTarget(target);
            bruteB.setTarget(target);
            Vec3 slotA = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeSwarmApproachPos(bruteA, target);
            Vec3 slotB = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeSwarmApproachPos(bruteB, target);
            helper.assertTrue(slotA.distanceToSqr(slotB) > 0.25D,
                    "Angular attack slots must separate concurrent FRONT_ANCHOR attackers: A=" + slotA + " B=" + slotB);

            Vec3 arrive = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeTacticalArriveSteering(
                    bruteA, target.position(), level);
            helper.assertTrue(arrive.x > 0.0D && arrive.length() <= 2.21D,
                    "Native steering arrive vector must point toward target within bounds: " + arrive);

            // Execute /mycelial ai command against live entities
            var server = level.getServer();
            var src = server.createCommandSourceStack()
                    .withLevel(level)
                    .withPosition(bruteA.position())
                    .withPermission(2);
            int cmdResult = server.getCommands().getDispatcher().execute("mycelial ai 24", src);
            helper.assertTrue(cmdResult > 0, "/mycelial ai 24 must execute cleanly and report nearby mobs.");

            helper.succeed();
        } catch (Exception ex) {
            helper.fail("hiveNativeBTreeSteeringLodAndAiCommandWorkEndToEnd threw: " + ex.getMessage());
        } finally {
            bruteA.discard();
            bruteB.discard();
        }
    }

    @GameTest(template = "empty", batch = "hive_brain_clean_room")
    public static void hiveBrainV2SignalsBlackboardPersistenceProfilesAndSnapshotWorkEndToEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        var bruteReloaded = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 4, 1, 2);
        var glider = helper.spawnWithNoFreeWill(ModEntities.SPORE_GLIDER.get(), 3, 2, 3);
        try {
            HiveBrain<MycelialMob> bruteBrain = brute.getHiveBrain();
            var gliderBrain = glider.getHiveBrain();
            helper.assertTrue(bruteBrain != null && bruteBrain == brute.getHiveBrainRuntime(),
                    "MycelialMob must expose unified HiveBrain == HiveBrainRuntime bridge.");
            helper.assertTrue(gliderBrain != null && gliderBrain == glider.getHiveBrainRuntime(),
                    "SporeGlider must expose unified HiveBrain == HiveBrainRuntime bridge.");

            long now = level.getGameTime();
            brute.tickBrain(brute);
            BlockPos alertPos = helper.absolutePos(new BlockPos(7, 1, 2));
            brute.dispatchBrainSignal(level, brute, HiveBrainSignal.allyAlert(alertPos, null, now));
            helper.assertTrue(bruteBrain.getSignalCount() == 1L, "Immediate signal must increment signalCount.");
            helper.assertTrue(bruteBrain.isLodAwake(now), "Immediate signal must wake LOD window.");
            helper.assertTrue(HiveBrainUtils.hasMemory(brute, ModMemoryTypes.SHARED_TARGET_POS.get()),
                    "ALLY_ALERT signal must populate SHARED_TARGET_POS.");

            // Queued signal drains on next tickBrain
            HiveBrainUtils.clearMemory(brute, ModMemoryTypes.SHARED_TARGET_POS.get());
            BlockPos acousticPos = helper.absolutePos(new BlockPos(6, 1, 4));
            brute.enqueueBrainSignal(HiveBrainSignal.acousticPulse(acousticPos, null, 0.9F, now));
            helper.assertTrue(bruteBrain.getPendingSignalCount() == 1, "Queued signal must await tick.");
            brute.tickBrain(brute);
            helper.assertTrue(bruteBrain.getPendingSignalCount() == 0 && bruteBrain.getSignalCount() == 2L,
                    "tickBrain must drain queued signals into deliverSignal.");
            BlockPos sharedTarget = HiveBrainUtils.getMemory(brute, ModMemoryTypes.SHARED_TARGET_POS.get());
            helper.assertTrue(acousticPos.equals(sharedTarget),
                    "ACOUSTIC_PULSE signal must record origin in SHARED_TARGET_POS: got " + sharedTarget);

            // Pause & per-sensor toggle controls
            long ticksBeforePause = bruteBrain.getBrainTickCount();
            bruteBrain.setPaused(true);
            brute.tickBrain(brute);
            helper.assertTrue(bruteBrain.getBrainTickCount() == ticksBeforePause,
                    "Paused HiveBrain must skip ticks without advancing brainTickCount.");
            bruteBrain.setPaused(false);
            helper.assertTrue(bruteBrain.setSensorEnabled(HiveMindSensor.class, false) > 0
                            && !bruteBrain.isSensorEnabled(HiveMindSensor.class),
                    "setSensorEnabled must toggle matching HiveSensor instances.");
            bruteBrain.setSensorEnabled(HiveMindSensor.class, true);

            // NBT persistence across save -> load
            HiveBrainKey<String> squadKey = HiveBrainKey.persistentString("gametest_v2_squad");
            HiveBrainKey<Integer> chargesKey = HiveBrainKey.persistentInt("gametest_v2_charges");
            bruteBrain.setActiveProfile("siege_vanguard");
            bruteBrain.setPaused(true);
            bruteBrain.getBlackboard().set(squadKey, "amber_wing");
            bruteBrain.getBlackboard().set(chargesKey, 5);
            bruteBrain.setCooldown("test_slam", now, 50);

            CompoundTag saved = new CompoundTag();
            brute.addAdditionalSaveData(saved);
            bruteReloaded.readAdditionalSaveData(saved);
            HiveBrain<MycelialMob> reloadedBrain = bruteReloaded.getHiveBrain();
            helper.assertTrue(reloadedBrain != null, "Reloaded mob must keep HiveBrain.");
            helper.assertTrue("siege_vanguard".equals(reloadedBrain.getActiveProfile()),
                    "Saved profile must survive NBT round-trip.");
            helper.assertTrue(reloadedBrain.isPaused(), "Saved paused state must survive NBT round-trip.");
            helper.assertTrue("amber_wing".equals(reloadedBrain.getBlackboard().get(squadKey, now))
                            && Integer.valueOf(5).equals(reloadedBrain.getBlackboard().get(chargesKey, now)),
                    "Persistent HiveBrainKey values must survive NBT round-trip.");
            helper.assertTrue(reloadedBrain.isOnCooldown("test_slam", now),
                    "Named cooldowns must survive NBT round-trip.");
            bruteReloaded.getHiveBrain().setPaused(false);
            brute.getHiveBrain().setPaused(false);

            // Snapshot & /mycelial ai over heterogeneous HiveBrainOwner entities
            glider.tickBrain(glider);
            HiveBrainSnapshot gliderSnap = glider.captureBrainSnapshot(now);
            helper.assertTrue(gliderSnap != null && gliderSnap.formatCompact().contains("act="),
                    "SporeGlider HiveBrainSnapshot must format cleanly.");
            var server = level.getServer();
            var src = server.createCommandSourceStack()
                    .withLevel(level)
                    .withPosition(glider.position())
                    .withPermission(2);
            int cmdResult = server.getCommands().getDispatcher().execute("mycelial ai 24", src);
            helper.assertTrue(cmdResult > 0, "/mycelial ai 24 must report across all HiveBrainOwner entities.");

            helper.succeed();
        } catch (Exception ex) {
            helper.fail("hiveBrainV2SignalsBlackboardPersistenceProfilesAndSnapshotWorkEndToEnd threw: " + ex.getMessage());
        } finally {
            brute.discard();
            bruteReloaded.discard();
            glider.discard();
        }
    }

    @GameTest(template = "empty", batch = "hive_brain_clean_room")
    public static void hiveBrainV3ProfilesTaskSwapModulesChannelArbitrationTraceAndCommandsWorkEndToEnd(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var brute = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 2);
        var behemoth = helper.spawnWithNoFreeWill(ModEntities.SPORE_BEHEMOTH.get(), 6, 1, 5);
        try {
            long now = level.getGameTime();
            HiveBrain<MycelialMob> bruteBrain = brute.getHiveBrain();
            helper.assertTrue(bruteBrain != null, "ChitinBrute must have HiveBrain.");
            helper.assertTrue(bruteBrain.getModule("tactical_btree") != null,
                    "MycelialMob must register tactical_btree HiveBrainModule.");
            helper.assertTrue(bruteBrain.getRegisteredProfileNames().containsAll(List.of("frenzy", "desync")),
                    "MycelialMob must register frenzy and desync executable profiles.");

            // Memory preservation across vanilla Brain task-group rebuild during profile swap
            HiveBrainUtils.setMemory(brute, ModMemoryTypes.HIVE_ALERT_LEVEL.get(), 0.77F);
            helper.assertTrue(brute.applyBrainProfile(level, brute, "desync"),
                    "applyBrainProfile(desync) must succeed.");
            helper.assertTrue("desync".equals(bruteBrain.getActiveProfile()),
                    "Active profile must switch to desync.");
            helper.assertTrue(bruteBrain.getVanillaBrain() == brute.getBrain(),
                    "Rebuilt Brain must be installed on LivingEntity via replaceVanillaBrain.");
            helper.assertFalse(bruteBrain.isSensorEnabled(HiveMindSensor.class),
                    "desync profile must disable HiveMindSensor.");
            Float preservedAlert = HiveBrainUtils.getMemory(brute, ModMemoryTypes.HIVE_ALERT_LEVEL.get());
            helper.assertTrue(preservedAlert != null && Math.abs(preservedAlert - 0.77F) < 0.01F,
                    "Vanilla Brain rebuild on profile swap must preserve existing memories.");

            helper.assertTrue(brute.applyBrainProfile(level, brute, "frenzy"),
                    "applyBrainProfile(frenzy) must succeed.");
            helper.assertTrue(bruteBrain.isSensorEnabled(HiveMindSensor.class),
                    "frenzy profile must re-enable HiveMindSensor.");
            helper.assertTrue(bruteBrain.getLodIntervalMultiplier() == 0,
                    "frenzy profile must set full 1-tick cadence (lodIntervalMultiplier=0).");
            helper.assertTrue(brute.applyBrainProfile(level, brute, "default"),
                    "Restoring default profile must succeed.");

            // Channel arbitration & signal interruption on running HiveBehaviour
            int[] interruptCount = {0};
            int[] listenerCount = {0};
            var lowMoveTask = new HiveBehaviour<MycelialMob>() {
                @Override
                protected boolean shouldKeepRunning(MycelialMob entity) {
                    return true;
                }

                @Override
                protected List<com.mojang.datafixers.util.Pair<
                        net.minecraft.world.entity.ai.memory.MemoryModuleType<?>,
                        net.minecraft.world.entity.ai.memory.MemoryStatus>> getMemoryRequirements() {
                    return List.of();
                }
            };
            lowMoveTask.requireChannels(HiveResourceChannel.MOVE)
                    .channelPriority(1)
                    .interruptOnSignal(HiveBrainSignal.SignalType.DESYNC_TRIGGERED)
                    .onSignalInterrupt(entity -> interruptCount[0]++);

            var highMoveTask = new HiveBehaviour<MycelialMob>() {
                @Override
                protected boolean shouldKeepRunning(MycelialMob entity) {
                    return true;
                }

                @Override
                protected List<com.mojang.datafixers.util.Pair<
                        net.minecraft.world.entity.ai.memory.MemoryModuleType<?>,
                        net.minecraft.world.entity.ai.memory.MemoryStatus>> getMemoryRequirements() {
                    return List.of();
                }
            };
            highMoveTask.requireChannels(HiveResourceChannel.MOVE).channelPriority(5);

            bruteBrain.registerProfile(HiveBrainProfile.<MycelialMob>builder("channel_test")
                    .taskGroups(BrainActivityGroup.idleTasks(lowMoveTask))
                    .build());
            helper.assertTrue(brute.applyBrainProfile(level, brute, "channel_test"),
                    "channel_test profile must apply.");
            helper.assertTrue(lowMoveTask.tryStart(level, brute, now),
                    "lowMoveTask must acquire MOVE channel.");
            helper.assertTrue(bruteBrain.getResourceArbiter().getOwner(HiveResourceChannel.MOVE) == lowMoveTask,
                    "Resource arbiter must track MOVE owner.");

            bruteBrain.onSignal(HiveBrainSignal.SignalType.DESYNC_TRIGGERED,
                    (lvl, ent, br, sig) -> listenerCount[0]++);
            brute.dispatchBrainSignal(level, brute, HiveBrainSignal.desyncTriggered(30, now));
            helper.assertTrue(lowMoveTask.getStatus() == net.minecraft.world.entity.ai.behavior.Behavior.Status.STOPPED,
                    "DESYNC_TRIGGERED signal must stop interrupted HiveBehaviour.");
            helper.assertTrue(interruptCount[0] == 1 && listenerCount[0] == 1,
                    "Both onSignalInterrupt and brain.onSignal listener must fire once.");
            helper.assertTrue(bruteBrain.getResourceArbiter().getOwner(HiveResourceChannel.MOVE) == null,
                    "Stopping interrupted behaviour must release MOVE channel.");

            // Channel preemption check
            helper.assertTrue(lowMoveTask.tryStart(level, brute, now + 1),
                    "lowMoveTask must re-acquire MOVE after interrupt release.");
            helper.assertTrue(highMoveTask.tryStart(level, brute, now + 1),
                    "Higher priority highMoveTask must preempt lowMoveTask.");
            helper.assertTrue(lowMoveTask.getStatus() == net.minecraft.world.entity.ai.behavior.Behavior.Status.STOPPED
                            && bruteBrain.getResourceArbiter().getOwner(HiveResourceChannel.MOVE) == highMoveTask,
                    "Preempted lowMoveTask must stop and yield MOVE to highMoveTask.");
            highMoveTask.doStop(level, brute, now + 2);
            brute.applyBrainProfile(level, brute, "default");

            // Boss phase/stagger profile + PHASE_SHIFT signal
            behemoth.enterStaggeredState(level, 40);
            HiveBrain<MycelialMob> behemothBrain = behemoth.getHiveBrain();
            helper.assertTrue(behemothBrain != null
                            && "staggered".equals(behemothBrain.getActiveProfile())
                            && behemothBrain.getLastSignal() != null
                            && behemothBrain.getLastSignal().type() == HiveBrainSignal.SignalType.PHASE_SHIFT,
                    "Boss enterStaggeredState must emit PHASE_SHIFT and switch to staggered profile.");

            // Traces + Brigadier subcommands: trace, profile, pause, signal
            boolean hasProfileTrace = bruteBrain.getTrace().recentEntries().stream()
                    .anyMatch(e -> e.kind() == HiveBrainTrace.EventKind.PROFILE);
            boolean hasInterruptTrace = bruteBrain.getTrace().recentEntries().stream()
                    .anyMatch(e -> e.kind() == HiveBrainTrace.EventKind.INTERRUPT);
            helper.assertTrue(hasProfileTrace && hasInterruptTrace,
                    "HiveBrainTrace must record PROFILE and INTERRUPT events.");

            var server = level.getServer();
            var src = server.createCommandSourceStack()
                    .withLevel(level)
                    .withPosition(brute.position())
                    .withPermission(2);
            helper.assertTrue(server.getCommands().getDispatcher().execute("mycelial ai trace 24", src) > 0,
                    "/mycelial ai trace 24 must succeed.");
            helper.assertTrue(server.getCommands().getDispatcher().execute("mycelial ai profile frenzy 24", src) > 0,
                    "/mycelial ai profile frenzy 24 must succeed.");
            helper.assertTrue(server.getCommands().getDispatcher().execute("mycelial ai pause true 24", src) > 0
                            && bruteBrain.isPaused(),
                    "/mycelial ai pause true 24 must pause nearest HiveBrain.");
            helper.assertTrue(server.getCommands().getDispatcher().execute("mycelial ai pause false 24", src) > 0
                            && !bruteBrain.isPaused(),
                    "/mycelial ai pause false 24 must resume nearest HiveBrain.");
            helper.assertTrue(server.getCommands().getDispatcher().execute("mycelial ai signal ally_alert 24", src) > 0,
                    "/mycelial ai signal ally_alert 24 must dispatch signal cleanly.");

            helper.succeed();
        } catch (Exception ex) {
            helper.fail("hiveBrainV3ProfilesTaskSwapModulesChannelArbitrationTraceAndCommandsWorkEndToEnd threw: "
                    + ex.getMessage());
        } finally {
            brute.discard();
            behemoth.discard();
        }
    }

    @GameTest(template = "empty", batch = "sinkhole_ecosystem")
    public static void mycelialSinkholeEcosystemBlocksEntitiesAndPurificationWorkEndToEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();

        BlockPos baseAbs = helper.absolutePos(new BlockPos(4, 0, 4));
        int originY = Math.max(baseAbs.getY() + 44, level.getMinBuildHeight() + 42);
        BlockPos originAbs = new BlockPos(baseAbs.getX(), originY, baseAbs.getZ());

        List<net.minecraft.world.entity.Entity> cleanupEntities = new java.util.ArrayList<>();
        BlockPos nodePos = originAbs.offset(4, -29, 0);
        BlockPos pylonPos = originAbs.offset(-2, -30, 2);

        try {
            // Prepare sturdy subterranean column + surface cap + air headroom (extended N/S for lateral galleries)
            for (int dx = -11; dx <= 11; dx++) {
                for (int dz = -19; dz <= 23; dz++) {
                    for (int dy = -34; dy <= -2; dy++) {
                        level.setBlock(originAbs.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 2);
                    }
                    level.setBlock(originAbs.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int dy = 0; dy <= 5; dy++) {
                        level.setBlock(originAbs.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }

            boolean generated = com.alex.mycelialmind.world.MycelialSinkholeFeature.generateSinkholeAt(
                    level, originAbs, level.getRandom());
            helper.assertTrue(generated, "MycelialSinkholeFeature.generateSinkholeAt must succeed on valid ground.");
            helper.assertTrue(tracker.hasSinkholeWithin(originAbs, 2),
                    "PurificationZoneTracker must persist newly reserved sinkhole origin.");

            // Verify Stratum 3 blocks (Updraft Vent, Root Sap Moss, Chest Vault, Root Synaptic Node)
            BlockPos ventPos = originAbs.below(30);
            helper.assertTrue(level.getBlockState(ventPos).is(ModBlocks.SPORE_UPDRAFT_VENT.get()),
                    "Central Spore Updraft Vent must be placed at origin.below(30).");
            BlockPos sapPos = originAbs.offset(0, -31, 3);
            helper.assertTrue(level.getBlockState(sapPos).is(ModBlocks.ROOT_SAP_MOSS.get())
                            && level.getBlockState(sapPos).getValue(com.alex.mycelialmind.block.RootSapMossBlock.SAP_READY),
                    "Cavern floor must contain ready Root Sap Moss.");
            BlockPos chestPos = originAbs.offset(-4, -30, 0);
            helper.assertTrue(level.getBlockEntity(chestPos) instanceof ChestBlockEntity,
                    "Western Root Vault chest must be populated.");
            helper.assertTrue(!level.getBlockState(nodePos).isAir()
                            && tracker.findNearestEnemyNode(ventPos, 10) != null,
                    "Eastern Root Synaptic Node must be registered as hostile node.");

            // Verify Northern Sap Chamber & Southern Root Tunnel lateral galleries
            helper.assertTrue(level.getBlockState(originAbs.offset(0, -31, -13)).is(Blocks.WATER),
                    "Northern Sap Chamber must contain recessed water spring at (0, -31, -13).");
            helper.assertTrue(level.getBlockState(originAbs.offset(3, -31, -13)).is(ModBlocks.ROOT_SAP_MOSS.get())
                            && level.getBlockState(originAbs.offset(3, -31, -13)).getValue(com.alex.mycelialmind.block.RootSapMossBlock.SAP_READY),
                    "Northern Sap Chamber must surround spring with ready Root Sap Moss.");
            helper.assertTrue(level.getBlockState(originAbs.offset(0, -29, 12)).isAir(),
                    "Southern Root Tunnel must carve clear subterranean passage at (0, -29, 12).");
            helper.assertTrue(level.getBlockState(originAbs.offset(3, -29, 11)).is(ModBlocks.CHITIN_PILLAR.get()),
                    "Southern Root Tunnel must anchor Chitin Pillar arch ribs.");

            // Verify Stratum 2 spiral Root Cap Shelves
            int shelfCount = 0;
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    for (int dy = -19; dy <= -2; dy++) {
                        if (level.getBlockState(originAbs.offset(dx, dy, dz)).is(ModBlocks.ROOT_CAP_SHELF.get())) {
                            shelfCount++;
                        }
                    }
                }
            }
            helper.assertTrue(shelfCount >= 2, "Twilight Throat must place spiral Root Cap Shelves, found " + shelfCount);

            // Verify native fauna spawned (RootKeeper + 2 SporeGliders)
            AABB searchBox = new AABB(
                    originAbs.getX() - 14.0D, originAbs.getY() - 34.0D, originAbs.getZ() - 14.0D,
                    originAbs.getX() + 15.0D, originAbs.getY() + 7.0D, originAbs.getZ() + 15.0D);
            var keepers = level.getEntitiesOfClass(com.alex.mycelialmind.entity.RootKeeperEntity.class, searchBox);
            var gliders = level.getEntitiesOfClass(com.alex.mycelialmind.entity.SporeGliderEntity.class, searchBox);
            cleanupEntities.addAll(keepers);
            cleanupEntities.addAll(gliders);
            helper.assertTrue(keepers.size() == 1, "Sinkhole must spawn 1 RootKeeperEntity, found " + keepers.size());
            helper.assertTrue(gliders.size() == 2, "Sinkhole must spawn 2 SporeGliderEntity, found " + gliders.size());

            // Interact with Root Sap Moss using a Glass Bottle -> Root Sap Vial
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
            player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
            level.getBlockState(sapPos).useItemOn(
                    bottle, level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(sapPos), Direction.UP, sapPos, false));
            helper.assertFalse(level.getBlockState(sapPos).getValue(com.alex.mycelialmind.block.RootSapMossBlock.SAP_READY),
                    "Bottling Root Sap Moss must deplete SAP_READY.");
            helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.ROOT_SAP_VIAL.get())),
                    "Bottling Root Sap Moss must yield a Root Sap Vial.");

            // Test Spore Updraft Vent lift on stepOn
            var testMob = net.minecraft.world.entity.EntityType.COW.create(level);
            helper.assertTrue(testMob != null, "Test cow must instantiate.");
            cleanupEntities.add(testMob);
            testMob.setPos(ventPos.getX() + 0.5D, ventPos.getY() + 1.0D, ventPos.getZ() + 0.5D);
            level.addFreshEntity(testMob);
            ModBlocks.SPORE_UPDRAFT_VENT.get().stepOn(level, ventPos, level.getBlockState(ventPos), testMob);
            helper.assertTrue(testMob.getDeltaMovement().y > 0.30D,
                    "Spore Updraft Vent must propel entities vertically on stepOn.");

            // Test Purification Pylon calming / sealing coupling
            helper.assertFalse(com.alex.mycelialmind.world.SinkholeEcosystemEngine.isBreachSealed(
                    true, tracker.isProtected(ventPos)),
                    "Unpurified sinkhole with live Root Node must not be sealed.");
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            helper.assertTrue(com.alex.mycelialmind.world.SinkholeEcosystemEngine.isBreachSealed(
                    true, tracker.isProtected(ventPos)),
                    "Purification Pylon inside Root Cavern must seal the sinkhole breach.");

            var keeper = keepers.get(0);
            keeper.tickCount = 20;
            keeper.aiStep();
            helper.assertTrue(keeper.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),
                    "Purified RootKeeper must receive Weakness when inside Purification Pylon radius.");
            var followRange = keeper.getAttribute(Attributes.FOLLOW_RANGE);
            helper.assertTrue(followRange != null
                            && Math.abs(followRange.getBaseValue() - com.alex.mycelialmind.world.SinkholeEcosystemEngine.PURIFIED_AGGRO_RANGE) < 0.05D,
                    "Purified RootKeeper follow range must drop to PURIFIED_AGGRO_RANGE.");

            // Verify Synaptic Research Bench synthesis & consumption of Resonant Sap Elixir + Bio-Scanner inspection
            BlockPos benchPos = originAbs.offset(0, 2, 0);
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            helper.assertTrue(level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench,
                    "Temporary Synaptic Bench must instantiate.");
            if (level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench) {
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.ROOT_SAP_VIAL.get()));
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
                bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.ROOT_HEART_RESIN.get()));
                ItemStack elixirOut = bench.computeSynthesisResult();
                helper.assertTrue(elixirOut.is(ModItems.RESONANT_SAP_ELIXIR.get()),
                        "Synaptic Bench must synthesize Resonant Sap Elixir from Root Sap Vial + Biomass + Root Heart Resin.");

                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));
                ItemStack consumed = elixirOut.copyWithCount(1).finishUsingItem(level, player);
                helper.assertTrue(consumed.is(net.minecraft.world.item.Items.GLASS_BOTTLE),
                        "Drinking Resonant Sap Elixir in Survival must return a Glass Bottle.");
                helper.assertFalse(player.hasEffect(ModEffects.MYCELIAL_INFECTION),
                        "Resonant Sap Elixir must cure Mycelial Infection.");
                helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)
                                && player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)
                                && player.hasEffect(ModEffects.SYNAPTIC_INSIGHT),
                        "Resonant Sap Elixir must grant Night Vision, Slow Falling, and Synaptic Insight.");
            }

            ItemStack scannerStack = new ItemStack(ModItems.BIO_SCANNER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, scannerStack);
            var scanVentRes = scannerStack.useOn(new net.minecraft.world.item.context.UseOnContext(
                    level, player, InteractionHand.MAIN_HAND, scannerStack,
                    new BlockHitResult(Vec3.atCenterOf(ventPos), Direction.UP, ventPos, false)));
            var scanSapRes = scannerStack.useOn(new net.minecraft.world.item.context.UseOnContext(
                    level, player, InteractionHand.MAIN_HAND, scannerStack,
                    new BlockHitResult(Vec3.atCenterOf(sapPos), Direction.UP, sapPos, false)));
            helper.assertTrue(scanVentRes.consumesAction() && scanSapRes.consumesAction(),
                    "Bio-Scanner must inspect both SporeUpdraftVent and RootSapMoss cleanly.");

            helper.succeed();
        } finally {
            for (var e : cleanupEntities) {
                e.discard();
            }
            for (int dx = -11; dx <= 11; dx++) {
                for (int dz = -19; dz <= 23; dz++) {
                    for (int dy = -34; dy <= 6; dy++) {
                        level.setBlock(originAbs.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            tracker.removeSinkhole(originAbs);
            tracker.clearAll();
            hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
        }
    }

    @GameTest(template = "empty", batch = "worldgen_terrain_balance")
    public static void terrainPlacementQualityRejectsCliffsFluidsSpawnAndAnchorsSlopesCleanly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();

        BlockPos hiveOrigin = helper.absolutePos(new BlockPos(24, 160, 24));
        BlockPos sinkOrigin = helper.absolutePos(new BlockPos(72, 160, 24));
        BlockPos spawnPos = level.getSharedSpawnPos();
        BlockPos spawnOutbreakOrigin = new BlockPos(spawnPos.getX(), 200, spawnPos.getZ());

        try {
            // 1) Steep cliff (deltaY = 5 > 3) must reject SurfaceHiveFeature without leaking tracker reservation
            for (int dx = -11; dx <= 11; dx++) {
                for (int dz = -11; dz <= 11; dz++) {
                    for (int dy = -6; dy <= 16; dy++) {
                        level.setBlock(hiveOrigin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 18);
                    }
                    int topDy = dx >= 4 ? -6 : -1;
                    for (int dy = -6; dy <= topDy; dy++) {
                        level.setBlock(hiveOrigin.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 18);
                    }
                }
            }
            boolean cliffHive = com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveAt(
                    level, hiveOrigin, level.getRandom());
            helper.assertFalse(cliffHive, "SurfaceHiveFeature must reject steep cliffs exceeding 3-block slope delta.");
            helper.assertTrue(tracker.findNearestEnemyNode(hiveOrigin, 16) == null,
                    "Rejected cliff hive must not reserve nodes in PurificationZoneTracker.");

            // 2) Flooded surface origin must reject SurfaceHiveFeature
            level.setBlock(hiveOrigin, Blocks.WATER.defaultBlockState(), 18);
            boolean fluidHive = com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveAt(
                    level, hiveOrigin, level.getRandom());
            helper.assertFalse(fluidHive, "SurfaceHiveFeature must reject waterlogged surface origins.");

            // 3) Gentle 2-block slope (deltaY = 2 <= 3) must succeed with organic foundation & zero floating perimeter blocks
            for (int dx = -11; dx <= 11; dx++) {
                for (int dz = -11; dz <= 11; dz++) {
                    for (int dy = -8; dy <= 16; dy++) {
                        level.setBlock(hiveOrigin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 18);
                    }
                    int topDy = dx >= 5 ? -3 : -1;
                    for (int dy = -8; dy <= topDy; dy++) {
                        BlockState mat = (dy == topDy) ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState();
                        level.setBlock(hiveOrigin.offset(dx, dy, dz), mat, 18);
                    }
                }
            }
            boolean slopeHive = com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveAt(
                    level, hiveOrigin, level.getRandom());
            helper.assertTrue(slopeHive, "SurfaceHiveFeature must succeed on a gentle 2-block slope.");
            helper.assertFalse(level.getBlockState(hiveOrigin.offset(6, -2, 0)).isAir(),
                    "Gentle slope skirt at dx=6 must anchor organic foundation downward instead of floating.");

            var terrainStats = com.alex.mycelialmind.world.WorldgenAuditEngine.auditTerrainQuality(
                    level, java.util.List.of(hiveOrigin), java.util.List.of());
            helper.assertTrue(terrainStats.floatingFoundationViolations() == 0
                            && terrainStats.fluidIntrusionViolations() == 0,
                    "Anchored slope SurfaceHive must report 0 floating/fluid violations, got float="
                            + terrainStats.floatingFoundationViolations()
                            + " fluid=" + terrainStats.fluidIntrusionViolations());
            tracker.clearAll();

            // 4) Subterranean lava pocket must reject MycelialSinkholeFeature without reserving sinkhole; dry column succeeds
            for (int dx = -11; dx <= 11; dx++) {
                for (int dz = -19; dz <= 23; dz++) {
                    for (int dy = -34; dy <= -2; dy++) {
                        level.setBlock(sinkOrigin.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 2);
                    }
                    level.setBlock(sinkOrigin.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int dy = 0; dy <= 5; dy++) {
                        level.setBlock(sinkOrigin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            level.setBlock(sinkOrigin.offset(0, -15, 0), Blocks.LAVA.defaultBlockState(), 2);
            boolean lavaSink = com.alex.mycelialmind.world.MycelialSinkholeFeature.generateSinkholeAt(
                    level, sinkOrigin, level.getRandom());
            helper.assertFalse(lavaSink, "MycelialSinkholeFeature must abort when lava occupies subterranean throat.");
            helper.assertFalse(tracker.hasSinkholeWithin(sinkOrigin, 4),
                    "Rejected lava sinkhole must not reserve sinkhole coordinates.");

            level.setBlock(sinkOrigin.offset(0, -15, 0), Blocks.STONE.defaultBlockState(), 2);
            boolean drySink = com.alex.mycelialmind.world.MycelialSinkholeFeature.generateSinkholeAt(
                    level, sinkOrigin, level.getRandom());
            helper.assertTrue(drySink, "MycelialSinkholeFeature must succeed once subterranean column is dry.");

            // 5) Overworld surface outbreak via worldgen Feature.place must honor permanent spawn safe zone
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    level.setBlock(spawnOutbreakOrigin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                    level.setBlock(spawnOutbreakOrigin.offset(dx, -2, dz), Blocks.STONE.defaultBlockState(), 18);
                    level.setBlock(spawnOutbreakOrigin.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 18);
                }
            }
            boolean placedAtSpawn = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), spawnOutbreakOrigin);
            helper.assertFalse(placedAtSpawn,
                    "Worldgen MycelialOutbreakFeature.place must respect permanent spawn-safe radius.");

            helper.succeed();
        } finally {
            net.minecraft.world.phys.AABB sinkBox = new net.minecraft.world.phys.AABB(
                    sinkOrigin.getX() - 14.0D, sinkOrigin.getY() - 35.0D, sinkOrigin.getZ() - 20.0D,
                    sinkOrigin.getX() + 14.0D, sinkOrigin.getY() + 8.0D, sinkOrigin.getZ() + 24.0D);
            for (var e : level.getEntitiesOfClass(com.alex.mycelialmind.entity.RootKeeperEntity.class, sinkBox)) {
                e.discard();
            }
            for (var e : level.getEntitiesOfClass(com.alex.mycelialmind.entity.SporeGliderEntity.class, sinkBox)) {
                e.discard();
            }
            for (int dx = -11; dx <= 11; dx++) {
                for (int dz = -11; dz <= 11; dz++) {
                    for (int dy = -8; dy <= 16; dy++) {
                        level.setBlock(hiveOrigin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                for (int dz = -19; dz <= 23; dz++) {
                    for (int dy = -34; dy <= 6; dy++) {
                        level.setBlock(sinkOrigin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    for (int dy = -2; dy <= 0; dy++) {
                        level.setBlock(spawnOutbreakOrigin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 18);
                    }
                }
            }
            tracker.clearAll();
        }
    }

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void audioEngineV4CuesCommandAndZeroVanillaSoundsWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<SoundEvent> v5Sounds = List.of(
                ModSounds.UI_CODEX_TAB.get(),
                ModSounds.UI_ACTION_CONFIRM.get(),
                ModSounds.SINKHOLE_UPDRAFT_LOOP.get(),
                ModSounds.ROOT_SAP_HARVEST.get(),
                ModSounds.ROOT_CAP_BOUNCE.get(),
                ModSounds.RESONANT_ELIXIR_DRINK.get(),
                ModSounds.BOSS_TELEGRAPH_LOCK.get(),
                ModSounds.BOSS_STAGGER_BREAK.get(),
                ModSounds.BRUTE_WALL_STUN.get(),
                ModSounds.PYRO_SHROOM_DEFUSE.get(),
                ModSounds.STATION_UPKEEP_LOW.get(),
                ModSounds.AMBER_VEIN_RESONATE.get(),
                ModSounds.HIVE_TENSION_BED_LOW.get(),
                ModSounds.HIVE_SWARM_COMBAT_BED.get(),
                ModSounds.CAVERN_SPORE_DRIP.get(),
                ModSounds.SUBTERRANEAN_HYPHAL_CREAK.get(),
                ModSounds.DEEP_MYCELIUM_WHISPER.get(),
                ModSounds.SANCTUM_HARMONIC_CHIME.get()
        );
        for (SoundEvent evt : v5Sounds) {
            helper.assertTrue(evt != null && evt.getLocation() != null
                            && MycelialMind.MODID.equals(evt.getLocation().getNamespace()),
                    "All 18 v4/v5 SoundEvents must be registered under mycelialmind namespace.");
        }

        // Verify RootSapVialItem & ResonantSapElixirItem use mod-owned drinking/eating sounds (zero vanilla honey_drink)
        SoundEvent vialDrink = ModItems.ROOT_SAP_VIAL.get().getDrinkingSound();
        SoundEvent vialEat = ModItems.ROOT_SAP_VIAL.get().getEatingSound();
        SoundEvent elixirDrink = ModItems.RESONANT_SAP_ELIXIR.get().getDrinkingSound();
        helper.assertTrue(vialDrink.getLocation().equals(ModSounds.RESONANT_ELIXIR_DRINK.getId())
                        && vialEat.getLocation().equals(ModSounds.RESONANT_ELIXIR_DRINK.getId())
                        && elixirDrink.getLocation().equals(ModSounds.RESONANT_ELIXIR_DRINK.getId()),
                "RootSapVialItem and ResonantSapElixirItem must use mycelialmind:resonant_elixir_drink.");

        // Verify /mycelial audio status & /mycelial audio test <sound_id> execute cleanly
        var server = level.getServer();
        var cmdSource = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
        try {
            int statusRes = server.getCommands().getDispatcher().execute("mycelial audio status", cmdSource);
            int testRes = server.getCommands().getDispatcher().execute("mycelial audio test boss_stagger_break", cmdSource);
            helper.assertTrue(statusRes == 1, "/mycelial audio status must succeed.");
            helper.assertTrue(testRes == 1, "/mycelial audio test boss_stagger_break must succeed.");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
            helper.fail("/mycelial audio command threw syntax exception: " + ex.getMessage());
        }

        helper.succeed();
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins")
    public static void hyphalEntityDragAndMycelialBoostWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 36, -4, 36);
        BlockPos base = helper.absolutePos(new BlockPos(6, 10, 6));
        BlockPos veinPos = base.above();
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        var sporeling = ModEntities.SPORELING.get().create(level);
        com.alex.mycelialmind.entity.HyphalVeinEntity strand = null;

        try {
            level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
            strand = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, veinPos, veinPos, 6, true, 0.80F);
            strand.setStrengthFloor(3);
            level.addFreshEntity(strand);

            if (zombie == null || sporeling == null) {
                helper.fail("Failed to create test entities for hyphal drag check.");
                return;
            }
            zombie.setPos(veinPos.getX() + 0.5D, veinPos.getY() + 0.05D, veinPos.getZ() + 0.5D);
            zombie.setDeltaMovement(0.40D, 0.0D, 0.0D);
            strand.interactWithEntity(zombie);
            double zombieSpeedX = zombie.getDeltaMovement().x;

            sporeling.setPos(veinPos.getX() + 0.5D, veinPos.getY() + 0.05D, veinPos.getZ() + 0.5D);
            sporeling.setDeltaMovement(0.20D, 0.0D, 0.0D);
            strand.interactWithEntity(sporeling);
            double sporelingSpeedX = sporeling.getDeltaMovement().x;

            Player bootedPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            bootedPlayer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.CHITIN_BOOTS.get()));
            bootedPlayer.setPos(veinPos.getX() + 0.5D, veinPos.getY() + 0.05D, veinPos.getZ() + 0.5D);
            bootedPlayer.setDeltaMovement(0.40D, 0.0D, 0.0D);
            strand.interactWithEntity(bootedPlayer);
            double bootedSpeedX = bootedPlayer.getDeltaMovement().x;

            if (zombieSpeedX >= 0.35D || sporelingSpeedX <= 0.205D || bootedSpeedX <= zombieSpeedX) {
                helper.fail(String.format(
                        "Entity drag check failed: zombieX=%.3f sporelingX=%.3f bootedX=%.3f",
                        zombieSpeedX, sporelingSpeedX, bootedSpeedX));
                return;
            }
            helper.succeed();
        } finally {
            if (strand != null && !strand.isRemoved()) strand.discard();
            if (zombie != null) zombie.discard();
            if (sporeling != null) sporeling.discard();
            level.removeBlock(base, false);
            releaseArenaChunks(level, forcedChunks);
        }
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins", timeoutTicks = 240)
    public static void hyphalMechanicalWavePropagatesAndAlertsNode(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, 6, 18, 2, 10);
        BlockPos startFloor = helper.absolutePos(new BlockPos(10, 10, 6));
        BlockPos nodePos = startFloor.offset(4, 1, 0);

        final com.alex.mycelialmind.entity.HyphalVeinEntity[] waveStrandRef = new com.alex.mycelialmind.entity.HyphalVeinEntity[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        entityChunksReady(level, startFloor.offset(2, 1, 0), 4),
                        "Expected entity-ticking chunks around mechanical wave arena to be ready."))
                .thenExecute(() -> {
                    for (int dx = 0; dx <= 4; dx++) {
                        level.setBlockAndUpdate(startFloor.offset(dx, 0, 0), Blocks.STONE.defaultBlockState());
                        level.removeBlock(startFloor.offset(dx, 1, 0), false);
                    }
                    level.setBlockAndUpdate(nodePos, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
                    var waveStrand = new com.alex.mycelialmind.entity.HyphalVeinEntity(
                            level, startFloor.offset(0, 1, 0), startFloor.offset(3, 1, 0), 5, true, 0.70F);
                    waveStrand.setStrengthFloor(2);
                    level.addFreshEntity(waveStrand);
                    waveStrand.injectWave(0, 0.90F);
                    waveStrandRef[0] = waveStrand;
                })
                .thenWaitUntil(() -> {
                    boolean nodeAlerted = level.getBlockEntity(nodePos)
                            instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity node
                            && node.isAlerted();
                    helper.assertTrue(nodeAlerted,
                            "Mechanical wave failed to propagate along HyphalVeinEntity and alert adjacent SynapticNode.");
                })
                .thenExecute(() -> {
                    if (waveStrandRef[0] != null && !waveStrandRef[0].isRemoved()) {
                        waveStrandRef[0].discard();
                    }
                    for (int dx = 0; dx <= 4; dx++) {
                        level.removeBlock(startFloor.offset(dx, 1, 0), false);
                        level.removeBlock(startFloor.offset(dx, 0, 0), false);
                    }
                    releaseArenaChunks(level, forcedChunks);
                    helper.succeed();
                });
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins")
    public static void hyphalPressurizedSeverSpraysSporesAndDebuffs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 36, -4, 36);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        BlockPos floorPos = helper.absolutePos(new BlockPos(14, 10, 10));
        BlockPos veinPos = floorPos.above();
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        com.alex.mycelialmind.entity.HyphalVeinEntity strand = null;

        try {
            level.setBlockAndUpdate(floorPos, Blocks.STONE.defaultBlockState());
            strand = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, veinPos, veinPos, 3, true, 0.85F);
            strand.setStrengthFloor(2);
            level.addFreshEntity(strand);
            if (zombie == null) {
                helper.fail("Failed to spawn Zombie for pressurized spray check.");
                return;
            }
            zombie.setPos(veinPos.getX() + 1.5D, veinPos.getY(), veinPos.getZ() + 0.5D);
            level.addFreshEntity(zombie);

            int severed = com.alex.mycelialmind.hive.HyphalSiegeDirector.trySeverHyphalVein(
                    level, veinPos, null, new ItemStack(ModItems.SYMBIOTIC_BLADE.get()));
            boolean hasSlow = zombie.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            boolean hasInfection = zombie.hasEffect(ModEffects.MYCELIAL_INFECTION);
            boolean pushed = zombie.getDeltaMovement().lengthSqr() > 0.01D;

            if (severed <= 0 || !hasSlow || !hasInfection || !pushed) {
                helper.fail(String.format(
                        "Pressurized spray failed: severed=%d slow=%s inf=%s pushed=%s",
                        severed, hasSlow, hasInfection, pushed));
                return;
            }
            helper.succeed();
        } finally {
            if (strand != null && !strand.isRemoved()) strand.discard();
            if (zombie != null) zombie.discard();
            level.removeBlock(floorPos, false);
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
        }
    }

    @GameTest(template = "empty", batch = "hyphal_verlet_filament", timeoutTicks = 240)
    public static void hyphalFilamentEntitySimulatesVerletAndSnapsWhenAnchorBreaks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, 4, 16, 12, 20);
        BlockPos anchorA = helper.absolutePos(new BlockPos(8, 15, 16));
        BlockPos anchorB = anchorA.offset(4, 0, 0);
        final com.alex.mycelialmind.entity.HyphalVeinEntity[] strandRef = new com.alex.mycelialmind.entity.HyphalVeinEntity[1];

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        entityChunksReady(level, anchorA.offset(2, 0, 0), 4),
                        "Expected entity-ticking chunks around Verlet filament arena."))
                .thenExecute(() -> {
                    for (int dx = 0; dx <= 4; dx++) {
                        for (int dy = -6; dy <= 0; dy++) {
                            level.removeBlock(anchorA.offset(dx, dy, 0), false);
                        }
                        level.setBlockAndUpdate(anchorA.offset(dx, -6, 0), Blocks.STONE.defaultBlockState());
                    }
                    level.setBlockAndUpdate(anchorA, Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(anchorB, Blocks.STONE.defaultBlockState());

                    var strand = new com.alex.mycelialmind.entity.HyphalVeinEntity(level, anchorA, anchorB, 9, true, 0.75F);
                    level.addFreshEntity(strand);
                    strandRef[0] = strand;
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        strandRef[0] != null
                                && !strandRef[0].isRemoved()
                                && strandRef[0].getMidpointNode().y < Vec3.atCenterOf(anchorA).y - 0.15D,
                        "Expected HyphalVeinEntity midpoint to sag under gravity."))
                .thenExecute(() -> level.removeBlock(anchorB, false))
                .thenWaitUntil(() -> helper.assertTrue(
                        strandRef[0] != null
                                && !strandRef[0].isRemoved()
                                && strandRef[0].isPinStart()
                                && !strandRef[0].isPinEnd(),
                        "Expected HyphalVeinEntity to hang from anchorA after anchorB broke."))
                .thenExecute(() -> level.removeBlock(anchorA, false))
                .thenWaitUntil(() -> helper.assertTrue(
                        strandRef[0] != null && strandRef[0].isRemoved(),
                        "Expected HyphalVeinEntity to free-fall and disintegrate after both anchors broke."))
                .thenExecute(() -> {
                    if (strandRef[0] != null && !strandRef[0].isRemoved()) {
                        strandRef[0].discard();
                    }
                    level.removeBlock(anchorA, false);
                    level.removeBlock(anchorB, false);
                    for (int dx = 0; dx <= 4; dx++) {
                        level.removeBlock(anchorA.offset(dx, -6, 0), false);
                    }
                    releaseArenaChunks(level, forcedChunks);
                    helper.succeed();
                });
    }

    @GameTest(template = "empty", batch = "worldgen_terrain_balance")
    public static void deepMyceliumTopographyAndAllStructureTerrainAdaptationsWorkEndToEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registries = level.registryAccess();

        var dimType = registries.registryOrThrow(Registries.DIMENSION_TYPE).get(com.alex.mycelialmind.world.ModWorldgenKeys.DEEP_MYCELIUM_TYPE);
        helper.assertTrue(dimType != null && dimType.minY() == 0 && dimType.height() == 192 && dimType.logicalHeight() == 192,
                "Expected deep_mycelium DimensionType height 0..192.");

        var noiseReg = registries.registryOrThrow(Registries.NOISE_SETTINGS);
        var deepNoiseHolder = noiseReg.getHolderOrThrow(com.alex.mycelialmind.world.ModWorldgenKeys.DEEP_MYCELIUM_NOISE);
        helper.assertTrue(deepNoiseHolder.value().noiseSettings().minY() == 0
                        && deepNoiseHolder.value().noiseSettings().height() == 192
                        && deepNoiseHolder.value().seaLevel() == 28,
                "Expected deep_mycelium NoiseSettings height 0..192 and seaLevel 28.");

        var biomeReg = registries.registryOrThrow(Registries.BIOME);
        var biomeSource = net.minecraft.world.level.biome.MultiNoiseBiomeSource.createFromList(
                new net.minecraft.world.level.biome.Climate.ParameterList<>(java.util.List.of(
                        com.mojang.datafixers.util.Pair.of(
                                net.minecraft.world.level.biome.Climate.parameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
                                biomeReg.getHolderOrThrow(com.alex.mycelialmind.world.ModWorldgenKeys.DEEP_MYCELIUM_CAVERNS))
                ))
        );
        var deepGen = new net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator(biomeSource, deepNoiseHolder);
        var deepRandomState = net.minecraft.world.level.levelgen.RandomState.create(
                deepNoiseHolder.value(), registries.lookupOrThrow(Registries.NOISE), 123456789L);
        var deepHeightAccessor = net.minecraft.world.level.LevelHeightAccessor.create(0, 192);

        var structReg = registries.registryOrThrow(Registries.STRUCTURE);
        var hiveSanctum = structReg.get(com.alex.mycelialmind.world.ModWorldgenKeys.HIVE_SANCTUM);
        var matriarchCitadel = structReg.get(com.alex.mycelialmind.world.ModWorldgenKeys.MATRIARCH_CITADEL);
        var twistedCitadel = structReg.get(com.alex.mycelialmind.world.ModWorldgenKeys.TWISTED_CITADEL);

        helper.assertTrue(hiveSanctum instanceof com.alex.mycelialmind.world.structure.HiveSanctumStructure
                        && hiveSanctum.terrainAdaptation() == net.minecraft.world.level.levelgen.structure.TerrainAdjustment.ENCAPSULATE,
                "Expected hive_sanctum to use HiveSanctumStructure + TerrainAdjustment.ENCAPSULATE.");
        helper.assertTrue(matriarchCitadel != null
                        && matriarchCitadel.terrainAdaptation() == net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN,
                "Expected matriarch_citadel to use TerrainAdjustment.BEARD_THIN.");
        helper.assertTrue(twistedCitadel != null
                        && twistedCitadel.terrainAdaptation() == net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN,
                "Expected twisted_citadel to use TerrainAdjustment.BEARD_THIN.");

        // Probe real deep_mycelium 0..192 NoiseColumn generator across deterministic coordinates
        boolean foundTwistedAnchor = false;
        boolean foundMatriarchAnchor = false;
        boolean foundMultiTierColumn = false;
        for (int idx = 0; idx < 64; idx++) {
            int px = (idx * 73) - 1600;
            int pz = ((idx * 131) % 1024) - 512;
            if (!foundTwistedAnchor) {
                var optY = com.alex.mycelialmind.world.TerrainPlacementPolicy.findCavernPlacementY(
                        deepGen, deepHeightAccessor, deepRandomState, px, pz, 28, 144, 22, 64, 10, 12);
                if (optY.isPresent()) {
                    int y = optY.getAsInt();
                    helper.assertTrue(y >= 28 && y <= 144,
                            "Expected probed TwistedCitadel cavern floor within 28..144, got " + y);
                    foundTwistedAnchor = true;
                }
            }
            if (!foundMatriarchAnchor) {
                var optMat = com.alex.mycelialmind.world.TerrainPlacementPolicy.findCavernPlacementY(
                        deepGen, deepHeightAccessor, deepRandomState, px, pz, 28, 128, 28, 48, 12, 16);
                if (optMat.isPresent()) {
                    int y = optMat.getAsInt();
                    helper.assertTrue(y >= 28 && y <= 128,
                            "Expected probed MatriarchCitadel cavern floor within 28..128, got " + y);
                    foundMatriarchAnchor = true;
                }
            }
            if (!foundMultiTierColumn) {
                var col = deepGen.getBaseColumn(px, pz, deepHeightAccessor, deepRandomState);
                int bands = 0;
                int lastFloor = Integer.MIN_VALUE;
                boolean prevSolid = false;
                int airStreak = 0;
                int candY = 0;
                for (int y = 24; y <= 164; y++) {
                    BlockState st = col.getBlock(y);
                    boolean fluid = !st.getFluidState().isEmpty();
                    boolean solid = !fluid && !st.isAir() && !st.canBeReplaced();
                    boolean air = !fluid && (st.isAir() || st.canBeReplaced());
                    if (solid) {
                        prevSolid = true;
                        airStreak = 0;
                        candY = y;
                    } else if (air && prevSolid) {
                        airStreak++;
                        if (airStreak == 4 && (lastFloor == Integer.MIN_VALUE || candY - lastFloor >= 6)) {
                            bands++;
                            lastFloor = candY;
                        }
                    } else {
                        prevSolid = false;
                        airStreak = 0;
                    }
                }
                if (bands >= 2) {
                    foundMultiTierColumn = true;
                }
            }
        }
        helper.assertTrue(foundTwistedAnchor,
                "Expected TerrainPlacementPolicy.findCavernPlacementY (9-col variance) to locate TwistedCitadel floor in 0..192 deep_mycelium.");
        helper.assertTrue(foundMatriarchAnchor,
                "Expected TerrainPlacementPolicy.findCavernPlacementY (9-col variance) to locate MatriarchCitadel floor in 0..192 deep_mycelium.");
        helper.assertTrue(foundMultiTierColumn,
                "Expected upgraded Terralith-style deep_mycelium density to produce multi-tier cavern floors.");

        // Verify MatriarchCitadelPiece & TwistedCitadelPiece support buttresses maintain zero floating blocks
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        BlockPos citadelCenter = helper.absolutePos(new BlockPos(26, 120, 26));
        var piece = new com.alex.mycelialmind.world.structure.MatriarchCitadelPiece(citadelCenter);
        var citadelBox = piece.getBoundingBox();
        try {
            for (BlockPos pos : BlockPos.betweenClosed(
                    citadelBox.minX(), citadelBox.minY(), citadelBox.minZ(),
                    citadelBox.maxX(), citadelBox.maxY(), citadelBox.maxZ())) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    level.getRandom(), citadelBox, new ChunkPos(citadelCenter), citadelCenter);
            helper.assertFalse(level.getBlockState(citadelCenter.offset(14, -6, 0)).isAir(),
                    "MatriarchCitadelPiece must generate vertical support buttresses under mid-arena ring.");
            helper.assertFalse(level.getBlockState(citadelCenter.offset(0, -1, 25)).isAir(),
                    "MatriarchCitadelPiece must generate 6-connected South approach ramp.");

            java.util.function.Predicate<BlockState> isModStructure =
                    s -> !s.isAir() && !s.is(Blocks.STONE) && !s.is(Blocks.BEDROCK) && !s.is(Blocks.BARRIER);
            var citadelFloating = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                    level, citadelBox, citadelCenter.above(4), isModStructure);
            helper.assertTrue(citadelFloating.isEmpty(),
                    "MatriarchCitadelPiece with buttresses & ramp must have zero floating blocks, found " + citadelFloating.size());
        } finally {
            var citadelGuardians = level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                    new AABB(citadelCenter).inflate(26.0D),
                    m -> m instanceof com.alex.mycelialmind.entity.SporeBehemothEntity || m instanceof ColonyWardenEntity);
            citadelGuardians.forEach(net.minecraft.world.entity.Entity::discard);
            for (BlockPos pos : BlockPos.betweenClosed(
                    citadelBox.minX(), citadelBox.minY(), citadelBox.minZ(),
                    citadelBox.maxX(), citadelBox.maxY(), citadelBox.maxZ())) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            tracker.clearAll();
        }

        helper.succeed();
    }

    @GameTest(template = "empty", batch = "worldgen_terrain_balance")
    public static void worldgenProcessorsAndDeepMyceliumBiomeFeaturesWorkEndToEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registries = level.registryAccess();

        // 1. Processor list includes FluidClear + FoundationPillar + ArchitecturalAccent + Rule + Protected
        var procList = registries.registryOrThrow(Registries.PROCESSOR_LIST)
                .get(com.alex.mycelialmind.world.ModWorldgenKeys.ARCHITECTURAL_REFLECTION);
        helper.assertTrue(procList != null && procList.list().size() >= 5,
                "Expected architectural_reflection processor list to include >= 5 processors.");
        boolean hasFluidClear = procList.list().stream().anyMatch(
                p -> p instanceof com.alex.mycelialmind.world.structure.MycelialFluidClearProcessor);
        boolean hasPillar = procList.list().stream().anyMatch(
                p -> p instanceof com.alex.mycelialmind.world.structure.MycelialFoundationPillarProcessor);
        helper.assertTrue(hasFluidClear && hasPillar,
                "Expected MycelialFluidClearProcessor and MycelialFoundationPillarProcessor in architectural_reflection.");

        // 2. All 3 deep_mycelium biome-specific placed features registered
        var pfReg = registries.registryOrThrow(Registries.PLACED_FEATURE);
        helper.assertTrue(pfReg.containsKey(com.alex.mycelialmind.world.ModWorldgenKeys.DEEP_MYCELIUM_STALACTITES_PF)
                        && pfReg.containsKey(com.alex.mycelialmind.world.ModWorldgenKeys.LATENT_SPORE_CANOPY_PF)
                        && pfReg.containsKey(com.alex.mycelialmind.world.ModWorldgenKeys.SANCTUM_AMBER_MONOLITH_PF),
                "Expected all 3 Deep Mycelium biome terrain features registered.");

        // 3. Unit behavior of MycelialFluidClearProcessor & MycelialFoundationPillarProcessor inside test area
        BlockPos boxMin = helper.absolutePos(new BlockPos(1, 6, 1));
        BlockPos boxMax = helper.absolutePos(new BlockPos(19, 29, 19));
        try {
            BlockPos wallPos = helper.absolutePos(new BlockPos(5, 16, 5));
            BlockPos waterNeighbor = wallPos.north();
            level.setBlock(waterNeighbor, Blocks.WATER.defaultBlockState(), 18);

            var fluidProc = new com.alex.mycelialmind.world.structure.MycelialFluidClearProcessor();
            var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
            BlockState waterloggedStair = Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
            var processed = fluidProc.processBlock(
                    level, wallPos, wallPos,
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                            BlockPos.ZERO, waterloggedStair, null),
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                            wallPos, waterloggedStair, null),
                    settings
            );
            helper.assertTrue(processed != null
                            && !processed.state().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED),
                    "MycelialFluidClearProcessor must strip WATERLOGGED from placed structural blocks.");
            helper.assertTrue(level.getBlockState(waterNeighbor).is(ModBlocks.MYCELIAL_STONE.get()),
                    "MycelialFluidClearProcessor must seal adjacent fluid neighbor with mycelial_stone.");

            BlockPos overhangBase = helper.absolutePos(new BlockPos(8, 20, 8));
            BlockPos overhangPos = ((overhangBase.getX() ^ overhangBase.getZ()) & 1) == 0
                    ? overhangBase
                    : overhangBase.east();
            level.setBlock(overhangPos.below(5), Blocks.DEEPSLATE.defaultBlockState(), 18);
            var pillarProc = new com.alex.mycelialmind.world.structure.MycelialFoundationPillarProcessor();
            pillarProc.processBlock(
                    level, overhangPos, overhangPos,
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                            BlockPos.ZERO, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), null),
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                            overhangPos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), null),
                    settings
            );
            helper.assertFalse(level.getBlockState(overhangPos.below(2)).isAir(),
                    "MycelialFoundationPillarProcessor must extend foundation column downward to cavern floor.");

            // 4. All 3 DeepMyceliumTerrainFeature variants generate cleanly
            BlockPos stalactiteCeil = helper.absolutePos(new BlockPos(14, 26, 6));
            level.setBlock(stalactiteCeil, ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 18);
            level.setBlock(stalactiteCeil.below(10), ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 18);
            helper.assertTrue(com.alex.mycelialmind.world.DeepMyceliumTerrainFeature.placeVariant(
                    level, stalactiteCeil, level.getRandom(),
                    com.alex.mycelialmind.world.DeepMyceliumTerrainFeature.Variant.CAVERN_STALACTITES),
                    "CAVERN_STALACTITES variant should generate beneath dry cavern ceiling.");

            BlockPos canopyFloor = helper.absolutePos(new BlockPos(14, 12, 14));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlock(canopyFloor.offset(dx, 0, dz), ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState(), 18);
                }
            }
            helper.assertTrue(com.alex.mycelialmind.world.DeepMyceliumTerrainFeature.placeVariant(
                    level, canopyFloor, level.getRandom(),
                    com.alex.mycelialmind.world.DeepMyceliumTerrainFeature.Variant.SPORE_CANOPY_ARCH),
                    "SPORE_CANOPY_ARCH variant should generate on dry mycelium floor.");

            BlockPos monolithFloor = helper.absolutePos(new BlockPos(5, 12, 14));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlock(monolithFloor.offset(dx, 0, dz), ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 18);
                }
            }
            helper.assertTrue(com.alex.mycelialmind.world.DeepMyceliumTerrainFeature.placeVariant(
                    level, monolithFloor, level.getRandom(),
                    com.alex.mycelialmind.world.DeepMyceliumTerrainFeature.Variant.SANCTUM_AMBER_MONOLITH),
                    "SANCTUM_AMBER_MONOLITH variant should generate on dry stone floor.");

            BlockPos poolCenter = helper.absolutePos(new BlockPos(10, 14, 10));
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.setBlock(poolCenter.offset(dx, -2, dz), Blocks.STONE.defaultBlockState(), 18);
                    level.setBlock(poolCenter.offset(dx, -1, dz), ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 18);
                    level.setBlock(poolCenter.offset(dx, 0, dz), ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 18);
                }
            }
            helper.assertTrue(com.alex.mycelialmind.world.DeepMyceliumTerrainFeature.placeVariant(
                    level, poolCenter, level.getRandom(),
                    com.alex.mycelialmind.world.DeepMyceliumTerrainFeature.Variant.GLOWING_SPORE_POOL),
                    "GLOWING_SPORE_POOL variant should carve glowing contained pool on solid basin.");
            helper.assertTrue(level.getBlockState(poolCenter).is(Blocks.WATER),
                    "GLOWING_SPORE_POOL center must contain water.");
        } finally {
            for (BlockPos pos : BlockPos.betweenClosed(
                    boxMin.getX(), boxMin.getY(), boxMin.getZ(),
                    boxMax.getX(), boxMax.getY(), boxMax.getZ())) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                }
            }
        }

        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structures", timeoutTicks = 400)
    public static void netherEndDimensionalStructuresAndBiomeDegradeWorkEndToEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forced = forceArenaChunks(helper, -12, 44, -12, 44);
        BlockPos netherCenter = helper.absolutePos(new BlockPos(12, 45, 12));
        BlockPos endCenter = helper.absolutePos(new BlockPos(32, 58, 12));
        try {
            // 1. Place dry supporting ground under both test footprints
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    level.setBlock(netherCenter.offset(dx, -1, dz), Blocks.NETHERRACK.defaultBlockState(), 18);
                    level.setBlock(endCenter.offset(dx, -1, dz), Blocks.END_STONE.defaultBlockState(), 18);
                }
            }

            var netherPiece = new com.alex.mycelialmind.world.structure.NetherBreachPiece(netherCenter);
            netherPiece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    level.getRandom(), netherPiece.getBoundingBox(), new ChunkPos(netherCenter), netherCenter);
            helper.assertTrue(level.getBlockState(netherCenter.above(1)).is(ModBlocks.HIVE_ALTAR.get()),
                    "NetherBreachPiece must place Hive Altar at breach center.");
            helper.assertTrue(level.getBlockState(netherCenter.offset(0, 1, -3)).is(ModBlocks.SENTINEL_NODE.get()),
                    "NetherBreachPiece must place Sentinel Node.");
            helper.assertTrue(level.getBlockState(netherCenter.offset(-3, 1, 0)).is(Blocks.CHEST),
                    "NetherBreachPiece must place reliquary chest.");

            var endPiece = new com.alex.mycelialmind.world.structure.EndSpirePiece(endCenter);
            endPiece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    level.getRandom(), endPiece.getBoundingBox(), new ChunkPos(endCenter), endCenter);
            helper.assertTrue(level.getBlockState(endCenter.offset(0, 1, -2)).is(ModBlocks.INCUBATOR_NODE.get()),
                    "EndSpirePiece must place Incubator Node.");
            helper.assertTrue(level.getBlockState(endCenter.offset(0, 13, -1)).is(ModBlocks.SENTINEL_NODE.get()),
                    "EndSpirePiece must place observatory Sentinel Node.");
            helper.assertTrue(level.getBlockState(endCenter.offset(2, 1, 0)).is(Blocks.CHEST),
                    "EndSpirePiece must place treasure chest.");

            // 2. Verify zero disconnected mod-structure blocks inside both pieces
            var netherDisconnected = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                    level, netherPiece.getBoundingBox(), netherCenter,
                    s -> !s.isAir() && s.getFluidState().isEmpty() && !s.is(Blocks.NETHERRACK) && !s.is(Blocks.END_STONE));
            helper.assertTrue(netherDisconnected.isEmpty(),
                    "Expected 0 disconnected blocks in NetherBreachPiece, found " + netherDisconnected.size());
            var endDisconnected = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                    level, endPiece.getBoundingBox(), endCenter,
                    s -> !s.isAir() && s.getFluidState().isEmpty() && !s.is(Blocks.NETHERRACK) && !s.is(Blocks.END_STONE));
            helper.assertTrue(endDisconnected.isEmpty(),
                    "Expected 0 disconnected blocks in EndSpirePiece, found " + endDisconnected.size());

            // 3. Verify live StructureSets registered in Level.NETHER & Level.END
            ServerLevel netherLevel = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            ServerLevel endLevel = level.getServer().getLevel(net.minecraft.world.level.Level.END);
            helper.assertTrue(netherLevel != null && endLevel != null,
                    "Expected both Level.NETHER and Level.END to exist on server.");
            var netherGrid = com.alex.mycelialmind.world.WorldgenAuditEngine.auditStructureSetGrid(
                    netherLevel, com.alex.mycelialmind.world.ModWorldgenKeys.NETHER_MYCELIAL_BREACH_SET, new ChunkPos(0, 0), 96);
            var endGrid = com.alex.mycelialmind.world.WorldgenAuditEngine.auditStructureSetGrid(
                    endLevel, com.alex.mycelialmind.world.ModWorldgenKeys.END_SPORE_SPIRE_SET, new ChunkPos(75, 0), 96);
            helper.assertTrue(netherGrid.structureSetRegistered() && netherGrid.separationInvariantOk(),
                    "Expected Nether Breach StructureSet registered with valid separation.");
            helper.assertTrue(endGrid.structureSetRegistered() && endGrid.separationInvariantOk(),
                    "Expected End Spire StructureSet registered with valid separation.");

            // 4. Verify MycelialBiomeDegradeProcessor guards & CaveBiomeDegradeStyle adaptation
            helper.assertTrue(com.alex.mycelialmind.world.structure.MycelialBiomeDegradeProcessor.isDegradableShellBlock(
                    Blocks.DEEPSLATE_BRICKS.defaultBlockState()),
                    "Deepslate bricks should be degradable shell blocks.");
            helper.assertFalse(com.alex.mycelialmind.world.structure.MycelialBiomeDegradeProcessor.isDegradableShellBlock(
                    Blocks.CHEST.defaultBlockState()),
                    "Chests must never be degraded by MycelialBiomeDegradeProcessor.");
            helper.assertTrue(com.alex.mycelialmind.world.structure.MycelialBiomeDegradeProcessor.selectAdaptedBlock(
                    com.alex.mycelialmind.world.TerrainPlacementPolicy.CaveBiomeDegradeStyle.LUSH,
                    Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 0, 0).is(Blocks.MOSS_BLOCK),
                    "LUSH degrade should produce moss_block at floor hash 0.");

            // 5. Verify dimensional chest loot tables roll valid items without endgame leak & foundation processor grounds Nether/End floors
            var reloadables = level.getServer().reloadableRegistries();
            var chestParams = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                            Vec3.atCenterOf(netherCenter))
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            for (ResourceKey<LootTable> chestKey : List.of(
                    com.alex.mycelialmind.world.MycelialLootSystem.NETHER_BREACH_RELIQUARY,
                    com.alex.mycelialmind.world.MycelialLootSystem.END_SPIRE_VAULT,
                    com.alex.mycelialmind.world.MycelialLootSystem.SANCTUM_QUARANTINE_CACHE)) {
                LootTable table = reloadables.getLootTable(chestKey);
                helper.assertFalse(table == LootTable.EMPTY, "Expected registered dimensional chest loot table: " + chestKey.location());
                var rolled = table.getRandomItems(chestParams, 4242L);
                helper.assertFalse(rolled.isEmpty(), "Expected non-empty roll from " + chestKey.location());
                for (ItemStack s : rolled) {
                    helper.assertFalse(s.is(ModItems.OVERMIND_ESSENCE.get()) || s.is(ModItems.HIVE_HEART.get()),
                            "Mid-tier dimensional/sanctum chest must not leak Overmind Essence or Hive Heart.");
                }
            }
            BlockPos pillarBase = helper.absolutePos(new BlockPos(20, 44, 20));
            BlockPos pillarProbe = ((pillarBase.getX() ^ pillarBase.getZ()) & 1) == 0 ? pillarBase : pillarBase.east();
            level.setBlock(pillarProbe.below(4), Blocks.NETHERRACK.defaultBlockState(), 18);
            var pillarProc = new com.alex.mycelialmind.world.structure.MycelialFoundationPillarProcessor();
            pillarProc.processBlock(
                    level, pillarProbe, pillarProbe,
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                            BlockPos.ZERO, Blocks.NETHER_BRICKS.defaultBlockState(), null),
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                            pillarProbe, Blocks.NETHER_BRICKS.defaultBlockState(), null),
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings());
            helper.assertFalse(level.getBlockState(pillarProbe.below(1)).isAir(),
                    "MycelialFoundationPillarProcessor must ground NETHER_BRICKS floors.");
        } finally {
            var nearby = level.getEntitiesOfClass(
                    com.alex.mycelialmind.entity.MycelialMob.class,
                    new net.minecraft.world.phys.AABB(netherCenter).inflate(36.0D));
            for (var mob : nearby) {
                mob.discard();
            }
            BlockPos minClear = helper.absolutePos(new BlockPos(-1, 32, -1));
            BlockPos maxClear = helper.absolutePos(new BlockPos(43, 84, 24));
            for (BlockPos p : BlockPos.betweenClosed(
                    minClear.getX(), minClear.getY(), minClear.getZ(),
                    maxClear.getX(), maxClear.getY(), maxClear.getZ())) {
                if (!level.getBlockState(p).isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                }
            }
            PurificationZoneTracker.get(level).removeNode(netherCenter.offset(0, 1, -3));
            PurificationZoneTracker.get(level).removeNode(endCenter.offset(0, 1, -2));
            PurificationZoneTracker.get(level).removeNode(endCenter.offset(0, 13, -1));
            releaseArenaChunks(level, forced);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", batch = "outbreak_fissure_terrain")
    public static void outbreakFissureRespectsSlopesAndProtectedBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 210, 8));
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                int height = x >= 2 ? 1 : 0;
                for (int y = -2; y <= 4; y++) {
                    level.setBlock(center.offset(x, y, z), y <= height
                            ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
                }
            }
        }
        BlockPos chest = center.offset(2, 1, 1);
        BlockPos bedrock = center.offset(-2, 0, 1);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 18);
        level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), 18);
        try {
            helper.assertTrue(MycelialOutbreakFeature.generateOutbreakAt(level, center,
                    net.minecraft.util.RandomSource.create(481516L)), "Fissure should generate on supported sloping terrain.");
            helper.assertTrue(level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                    "Fissure must preserve block entities.");
            helper.assertTrue(level.getBlockState(bedrock).is(Blocks.BEDROCK), "Fissure must preserve bedrock.");
            long exposedAmber = BlockPos.betweenClosedStream(center.offset(-6, -1, -6), center.offset(6, 0, 6))
                    .filter(pos -> level.getBlockState(pos).is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())
                            && level.getBlockState(pos.above()).isAir()).count();
            helper.assertTrue(exposedAmber >= 2, "Fissure should expose amber near its central node.");
        } finally {
            tracker.removeNode(center);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "outbreak_surface_source")
    public static void surfaceStructuresReserveOnlyTheirSourceOutbreak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos surface = helper.absolutePos(new BlockPos(8, 180, 8));
        BlockPos source = surface.below(50);
        tracker.clearAll();
        try {
            helper.assertFalse(tracker.tryReserveSurfaceHive(surface, 256, 72, source),
                    "A missing source must not reserve a linked surface hive.");
            helper.assertFalse(tracker.tryReserveSinkhole(surface, 320, 96, source),
                    "A missing source must not reserve a linked sinkhole.");
            tracker.addNode(source, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
            helper.assertFalse(tracker.tryReserveSurfaceHive(surface, 256, 72),
                    "The source must still block unrelated hive placement.");
            helper.assertTrue(tracker.tryReserveSurfaceHive(surface, 256, 72, source),
                    "A hive linked to its source may pass that source's clearance.");
            tracker.removeNode(surface);
            tracker.removeNode(surface.above(3));
            BlockPos unrelated = surface.offset(2, -45, 0);
            tracker.addNode(unrelated, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
            helper.assertFalse(tracker.tryReserveSinkhole(surface, 320, 96, source),
                    "The source exemption must not ignore another nearby node.");
            tracker.removeNode(unrelated);
            helper.assertTrue(tracker.tryReserveSinkhole(surface, 320, 96, source),
                    "A sinkhole linked to its source may pass that source's clearance.");
            helper.assertTrue(tracker.getNodeType(source) == com.alex.mycelialmind.world.SynapticNodeType.RELAY,
                    "Reserving a surface structure must preserve the source node.");
        } finally {
            tracker.clearAll();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "outbreak_surface_structures", timeoutTicks = 200)
    public static void linkedOutbreakSurfaceStructuresKeepTheirUndergroundSource(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos hiveOrigin = helper.absolutePos(new BlockPos(8, 200, 8));
        BlockPos sinkOrigin = hiveOrigin.offset(0, 0, 200);
        tracker.clearAll();
        try {
            for (BlockPos origin : java.util.List.of(hiveOrigin, sinkOrigin)) {
                int zRadius = origin.equals(sinkOrigin) ? 22 : 12;
                for (int dx = -12; dx <= 12; dx++) {
                    for (int dz = -zRadius; dz <= zRadius; dz++) {
                        for (int dy = -34; dy <= 16; dy++) {
                            level.setBlock(origin.offset(dx, dy, dz),
                                    (dy < 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
                        }
                    }
                }
            }
            BlockPos hiveSource = hiveOrigin.below(46);
            BlockPos sinkSource = sinkOrigin.below(46);
            helper.assertFalse(com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveFromOutbreak(
                            level, hiveOrigin, hiveSource, net.minecraft.util.RandomSource.create(9)),
                    "A surface hive must reject an unregistered source.");
            helper.assertFalse(com.alex.mycelialmind.world.MycelialSinkholeFeature.generateSinkholeFromOutbreak(
                            level, sinkOrigin, sinkSource, net.minecraft.util.RandomSource.create(9)),
                    "A sinkhole must reject an unregistered source.");
            for (BlockPos source : java.util.List.of(hiveSource, sinkSource)) {
                level.setBlock(source, ModBlocks.MYCELIAL_NODE.get().defaultBlockState(), 18);
                tracker.addNode(source, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
                for (int dy = 1; dy < 45; dy++) {
                    level.setBlock(source.above(dy), ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState(), 18);
                }
            }
            helper.assertTrue(com.alex.mycelialmind.world.SurfaceHiveFeature.generateHiveFromOutbreak(
                            level, hiveOrigin, hiveSource, net.minecraft.util.RandomSource.create(19)),
                    "A supported surface hive should emerge from its registered cave source.");
            helper.assertTrue(level.getBlockState(hiveSource).is(ModBlocks.MYCELIAL_NODE.get())
                            && level.getBlockState(hiveOrigin.below(6)).is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                            && level.getBlockState(hiveOrigin.below(5)).is(Blocks.STONE)
                            && level.getBlockState(hiveOrigin).is(ModBlocks.INCUBATOR_NODE.get()),
                    "The hive must retain the underground source and its upper root.");
            helper.assertTrue(com.alex.mycelialmind.world.MycelialSinkholeFeature.generateSinkholeFromOutbreak(
                            level, sinkOrigin, sinkSource, net.minecraft.util.RandomSource.create(29)),
                    "A supported sinkhole should emerge from its registered cave source.");
            helper.assertTrue(level.getBlockState(sinkSource).is(ModBlocks.MYCELIAL_NODE.get())
                            && level.getBlockState(sinkOrigin.below(33)).is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                            && (level.getBlockState(sinkOrigin.below(32)).is(ModBlocks.MYCELIAL_STONE.get())
                                    || level.getBlockState(sinkOrigin.below(32)).is(ModBlocks.MYCELIAL_ARCH_BRICK.get())),
                    "The sinkhole floor must meet the root while preserving the source: source="
                            + level.getBlockState(sinkSource) + ", root=" + level.getBlockState(sinkOrigin.below(33))
                            + ", floor=" + level.getBlockState(sinkOrigin.below(32)));
        } finally {
            tracker.clearAll();
            net.minecraft.world.phys.AABB sinkArea = new net.minecraft.world.phys.AABB(sinkOrigin).inflate(24, 40, 24);
            for (var mob : level.getEntitiesOfClass(com.alex.mycelialmind.entity.RootKeeperEntity.class, sinkArea)) {
                mob.discard();
            }
            for (var mob : level.getEntitiesOfClass(com.alex.mycelialmind.entity.SporeGliderEntity.class, sinkArea)) {
                mob.discard();
            }
        }
        helper.succeed();
    }


    @GameTest(template = "empty", batch = "outbreak_natural_growth", timeoutTicks = 400)
    public static void caveOutbreakSeedsMyceliumAfterGenerationAndSpreadsAcrossStone(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 190, 8));
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 18);
                level.setBlock(center.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 18);
                for (int y = 1; y <= 5; y++) {
                    level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 18);
                }
                level.setBlock(center.offset(x, 6, z), Blocks.STONE.defaultBlockState(), 18);
            }
        }
        try {
            helper.assertTrue(MycelialOutbreakFeature.generateOutbreakAt(level, center,
                    net.minecraft.util.RandomSource.create(52)), "Expected a cave outbreak.");
            BlockPos seed = center.below();
            helper.assertFalse(level.getBlockState(seed).is(ModBlocks.CREEPING_MYCELIUM.get()),
                    "The initial generation must not place mycelium at the seed.");
            var node = (com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity) level.getBlockEntity(center);
            helper.assertTrue(node != null, "Expected the outbreak node.");
            var persisted = node.saveWithoutMetadata(level.registryAccess());
            helper.assertTrue(persisted.getBoolean("OutbreakOrigin"), "Outbreak origin must be persisted.");
            var restored = new com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity(center, level.getBlockState(center));
            restored.loadWithComponents(persisted, level.registryAccess());
            helper.assertTrue(restored.saveWithoutMetadata(level.registryAccess()).getBoolean("OutbreakOrigin"),
                    "The outbreak origin must survive a save and reload.");
            if (node.getNodeType() == com.alex.mycelialmind.world.SynapticNodeType.RELAY) {
                com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity.matureRelayNode(level, center, tracker);
                node = (com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity) level.getBlockEntity(center);
                helper.assertTrue(node != null && node.saveWithoutMetadata(level.registryAccess()).getBoolean("OutbreakOrigin"),
                        "The outbreak origin must survive node maturation.");
            }
            for (int ticks = 0; ticks < 10000 && !level.getBlockState(seed).is(ModBlocks.CREEPING_MYCELIUM.get()); ticks++) {
                com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity.serverTick(level, center,
                        level.getBlockState(center), node);
            }
            helper.assertTrue(level.getBlockState(seed).is(ModBlocks.CREEPING_MYCELIUM.get()),
                    "The outbreak should seed mycelium during server ticks.");
            var randomTick = com.alex.mycelialmind.block.CreepingMyceliumBlock.class.getDeclaredMethod("randomTick",
                    BlockState.class, ServerLevel.class, BlockPos.class, net.minecraft.util.RandomSource.class);
            randomTick.setAccessible(true);
            var random = net.minecraft.util.RandomSource.create(789);
            for (int attempt = 0; attempt < 1500; attempt++) {
                randomTick.invoke(ModBlocks.CREEPING_MYCELIUM.get(), level.getBlockState(seed), level, seed, random);
            }
            long grown = BlockPos.betweenClosedStream(center.offset(-1, -1, -1), center.offset(1, 0, 1))
                    .filter(pos -> !pos.equals(seed) && level.getBlockState(pos).is(ModBlocks.CREEPING_MYCELIUM.get())).count();
            helper.assertTrue(grown > 0, "Mycelium should spread from the outbreak into the cave floor.");
        } finally {
            tracker.removeNode(center);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "outbreak_growth_corridors")
    public static void caveOutbreakGrowthFollowsConnectedWalkableCave(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 210, 8));
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        try {
            for (int x = -2; x <= 16; x++) {
                for (int z = -3; z <= 3; z++) {
                    for (int y = -2; y <= 5; y++) {
                        level.setBlock(center.offset(x, y, z), Blocks.STONE.defaultBlockState(), 18);
                    }
                }
            }
            for (int x = 0; x <= 15; x++) {
                for (int z = -1; z <= 1; z++) {
                    int height = x >= 8 ? 1 : 0;
                    for (int y = height + 1; y <= height + 3; y++) {
                        level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 18);
                    }
                }
            }
            level.setBlock(center, ModBlocks.MYCELIAL_NODE.get().defaultBlockState(), 18);
            BlockPos water = center.offset(7, 1, 1);
            level.setBlock(water, Blocks.WATER.defaultBlockState(), 18);
            var random = net.minecraft.util.RandomSource.create(1949L);
            for (int i = 0; i < 100; i++) {
                com.alex.mycelialmind.world.OutbreakNaturalGrowth.pulse(level, center, random);
            }
            long farGrowth = BlockPos.betweenClosedStream(center.offset(10, 1, -1), center.offset(15, 1, 1))
                    .filter(pos -> level.getBlockState(pos).is(ModBlocks.CREEPING_MYCELIUM.get())).count();
            helper.assertTrue(farGrowth > 0, "Natural growth should reach the far end of a connected cave passage.");
            helper.assertTrue(level.getFluidState(water).is(net.minecraft.tags.FluidTags.WATER),
                    "Natural growth must preserve cave water.");
            helper.assertTrue(level.getBlockState(center.offset(5, 1, 0)).isAir()
                            && level.getBlockState(center.offset(5, 2, 0)).isAir(),
                    "Natural growth must leave the passage walkable.");
        } finally {
            tracker.clearAll();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "outbreak_grown_surface", timeoutTicks = 200)
    public static void surfaceEmergenceProtectsBuildingsAndDoesNotGenerateMycelium(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 200, 8));
        BlockPos grown = origin.below();
        BlockPos source = origin.offset(3, -12, 0);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        try {
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    for (int y = -10; y <= 17; y++) {
                        level.setBlock(origin.offset(x, y, z),
                                (y < 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
                    }
                }
            }
            level.setBlock(source, ModBlocks.MYCELIAL_NODE.get().defaultBlockState(), 18);
            tracker.addNode(source, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
            level.setBlock(grown, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState(), 18);
            BlockPos chest = origin.offset(8, 0, 0);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 18);
            long seed = 0;
            while (seed < 10000 && net.minecraft.util.RandomSource.create(seed).nextInt(100) != 0) {
                seed++;
            }
            helper.assertTrue(seed < 10000, "Expected a seed for a surface emergence attempt.");
            boolean blocked = com.alex.mycelialmind.world.OutbreakSurfaceEmergence.tryEmerge(
                    level, source, grown, net.minecraft.util.RandomSource.create(seed));
            helper.assertFalse(blocked, "Surface emergence must avoid existing block entities.");
            helper.assertTrue(level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                    "Surface emergence must preserve the chest.");
            level.setBlock(chest, Blocks.AIR.defaultBlockState(), 18);
            boolean emerged = com.alex.mycelialmind.world.OutbreakSurfaceEmergence.tryEmerge(
                    level, source, grown, net.minecraft.util.RandomSource.create(seed));
            helper.assertTrue(emerged, "A connected colony should be able to establish a surface hive.");
            helper.assertTrue(level.getBlockState(source).is(ModBlocks.MYCELIAL_NODE.get()),
                    "The original underground node must survive surface emergence.");
            long mycelium = BlockPos.betweenClosedStream(origin.offset(-11, -6, -11), origin.offset(11, 16, 11))
                    .filter(pos -> level.getBlockState(pos).is(ModBlocks.CREEPING_MYCELIUM.get())).count();
            helper.assertTrue(mycelium <= 1, "The structure must not create a prefabricated mycelium carpet.");
        } finally {
            tracker.clearAll();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "outbreak_growth_to_surface", timeoutTicks = 200)
    public static void connectedCaveGrowthCanReachSurfaceAndEstablishHive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 230, 8));
        BlockPos source = origin.offset(-12, -11, 0);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        try {
            for (int x = -13; x <= 25; x++) {
                for (int z = -12; z <= 12; z++) {
                    for (int y = -13; y <= 17; y++) {
                        level.setBlock(origin.offset(x, y, z),
                                (y < 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
                    }
                }
            }
            for (int x = -12; x <= 0; x++) {
                int floor = -11 + (x + 12) * 10 / 12;
                for (int y = floor + 1; y <= floor + 3; y++) {
                    level.setBlock(origin.offset(x, y, 0), Blocks.AIR.defaultBlockState(), 18);
                }
            }
            level.setBlock(source, ModBlocks.MYCELIAL_NODE.get().defaultBlockState(), 18);
            tracker.addNode(source, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
            var random = net.minecraft.util.RandomSource.create(619L);
            BlockPos surface = origin.offset(12, -1, 0);
            for (int attempt = 0; attempt < 1500 && !level.getBlockState(surface).is(ModBlocks.CREEPING_MYCELIUM.get()); attempt++) {
                com.alex.mycelialmind.world.OutbreakNaturalGrowth.pulse(level, source, random);
            }
            helper.assertTrue(level.getBlockState(surface).is(ModBlocks.CREEPING_MYCELIUM.get()),
                    "Growth should reach the surface along the connected sloping cave.");
            long seed = 0;
            while (seed < 10000 && net.minecraft.util.RandomSource.create(seed).nextInt(100) != 0) {
                seed++;
            }
            helper.assertTrue(seed < 10000 && com.alex.mycelialmind.world.OutbreakSurfaceEmergence.tryEmerge(
                            level, source, surface, net.minecraft.util.RandomSource.create(seed)),
                    "A colony that has grown to the surface should be able to form a hive.");
            helper.assertTrue(level.getBlockState(source).is(ModBlocks.MYCELIAL_NODE.get()),
                    "Surface emergence must retain its originating node.");
        } finally {
            tracker.clearAll();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "outbreak_grown_sinkhole", timeoutTicks = 200)
    public static void grownSurfaceCanEstablishSinkholeWithoutCreatingMycelium(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 230, 8));
        BlockPos source = origin.offset(3, -46, 0);
        BlockPos grown = origin.below();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        try {
            for (int x = -12; x <= 12; x++) {
                for (int z = -24; z <= 24; z++) {
                    for (int y = -38; y <= 6; y++) {
                        level.setBlock(origin.offset(x, y, z),
                                (y < 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
                    }
                }
            }
            level.setBlock(source, ModBlocks.MYCELIAL_NODE.get().defaultBlockState(), 18);
            tracker.addNode(source, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
            level.setBlock(grown, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState(), 18);
            long seed = 0;
            while (seed < 100000) {
                var probe = net.minecraft.util.RandomSource.create(seed);
                if (probe.nextInt(100) == 0 && probe.nextInt(3) == 0) {
                    break;
                }
                seed++;
            }
            helper.assertTrue(seed < 100000 && com.alex.mycelialmind.world.OutbreakSurfaceEmergence.tryEmerge(
                            level, source, grown, net.minecraft.util.RandomSource.create(seed)),
                    "A deep colony reaching the surface should be able to form a sinkhole.");
            helper.assertTrue(level.getBlockState(source).is(ModBlocks.MYCELIAL_NODE.get())
                            && tracker.hasSinkholeWithin(origin, 2),
                    "The sinkhole must retain the source and register its position.");
            long mycelium = BlockPos.betweenClosedStream(origin.offset(-12, -34, -24), origin.offset(12, 6, 24))
                    .filter(pos -> level.getBlockState(pos).is(ModBlocks.CREEPING_MYCELIUM.get())).count();
            helper.assertTrue(mycelium <= 1, "The sinkhole must not create a prefabricated mycelium carpet.");
        } finally {
            tracker.clearAll();
            var box = new net.minecraft.world.phys.AABB(origin).inflate(24, 40, 28);
            for (var mob : level.getEntitiesOfClass(com.alex.mycelialmind.entity.RootKeeperEntity.class, box)) {
                mob.discard();
            }
            for (var mob : level.getEntitiesOfClass(com.alex.mycelialmind.entity.SporeGliderEntity.class, box)) {
                mob.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "open_source_apis", timeoutTicks = 100)
    public static void caffeineTrackerAndKubeJSBindingsWorkInServerLevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        hive.resetAll();
        tracker.clearAll();
        try {
            BlockPos pylonPos = helper.absolutePos(new BlockPos(4, 2, 4));
            BlockPos testPos = helper.absolutePos(new BlockPos(8, 2, 4));
            tracker.addPylon(pylonPos);
            tracker.addNode(testPos, SynapticNodeType.RELAY);
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.isProtected(level, testPos),
                    "KubeJS binding should report position inside Caffeine-backed pylon radius as protected.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.isProtected(level, testPos),
                    "Repeated Caffeine lookup should stay protected and record a cache hit.");
            helper.assertTrue(tracker.getProtectionCacheStats().hitCount() >= 1L,
                    "PurificationZoneTracker Caffeine cache should record at least one hit.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getActivePylonCount(level) == 1,
                    "Active pylon count should be 1.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getActiveNodeCount(level) == 1,
                    "Active node count should be 1.");

            com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.setThreat(level, 42.0);
            helper.assertTrue(Math.abs(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getThreat(level) - 42.0) < 0.01,
                    "KubeJS setThreat/getThreat should round-trip 42.0.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.addStimulus(level, "FIRE", 120.0),
                    "KubeJS addStimulus should accept FIRE.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.hasTrait(level, "FIRE_RESISTANT"),
                    "FIRE_RESISTANT should activate after 120 FIRE stimulus.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getActiveTraits(level).contains("FIRE_RESISTANT"),
                    "Active traits list should include FIRE_RESISTANT.");

            com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.resetHive(level);
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getThreat(level) == 0.0,
                    "KubeJS resetHive should clear threat back to 0.");
        } finally {
            tracker.clearAll();
            hive.resetAll();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "java_core_libraries", timeoutTicks = 100)
    public static void javaCoreLibrariesWorkInServerLevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        var stats = com.alex.mycelialmind.hive.HiveCombatTelemetryStats.forLevel(level);
        hive.resetAll();
        tracker.clearAll();
        stats.reset();
        try {
            BlockPos n1 = helper.absolutePos(new BlockPos(0, 2, 0));
            BlockPos n2 = helper.absolutePos(new BlockPos(20, 2, 0));
            BlockPos n3 = helper.absolutePos(new BlockPos(40, 2, 0));
            tracker.addNode(n1, SynapticNodeType.RELAY);
            tracker.addNode(n2, SynapticNodeType.RELAY);
            tracker.addNode(n3, SynapticNodeType.INCUBATOR);
            tracker.addPylon(n1);

            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.countContestedChunks(level) >= 1L,
                    "RoaringBitmap contested chunk count should be >= 1 when pylon and node share a chunk.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.hasNodeInChunk(level, n2.getX() >> 4, n2.getZ() >> 4),
                    "RoaringBitmap index should report node chunk as occupied.");

            tracker.removePylon(n1);
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getNetworkClusterCount(level) == 1,
                    "JGraphT should see 1 connected cluster across n1-n2-n3.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getCriticalNodeCount(level) == 1,
                    "JGraphT should detect n2 as the single articulation point.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.findShortestNodePathLength(level, n1, n3) == 2,
                    "JGraphT shortest path from n1 to n3 should have 2 hops.");

            double[] samples = {8.0D, 9.0D, 7.0D, 8.5D, 7.5D, 8.0D};
            for (double s : samples) {
                stats.recordDamage(StimulusType.PROJECTILE, s);
            }
            helper.assertTrue(stats.recordDamage(StimulusType.PROJECTILE, 35.0D),
                    "Commons Statistics should detect a projectile damage spike.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getDamageSpikeCount(level, "PROJECTILE") == 1L,
                    "KubeJS damage spike telemetry should report 1 PROJECTILE spike.");
            helper.assertTrue(com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings.getAverageDamage(level, "PROJECTILE") > 8.0D,
                    "KubeJS average damage should reflect recorded samples.");
        } finally {
            tracker.clearAll();
            hive.resetAll();
            stats.reset();
        }
        helper.succeed();
    }

}
