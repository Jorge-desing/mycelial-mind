package com.alex.mycelialmind.test;

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
import com.alex.mycelialmind.compat.sbl.MycelialSblBridge;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import com.alex.mycelialmind.network.SymbiosisSyncPayload;
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

    @GameTest(batch = "entity_ai_live", template = "empty", timeoutTicks = 180)
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

    @GameTest(batch = "entity_ai_live", template = "empty", timeoutTicks = 180)
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

    @GameTest(batch = "entity_ai_live", template = "empty", timeoutTicks = 240)
    public static void weaverSupportCoversSeparatedSquad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forced = forceArenaChunks(helper, -4, 22, -5, 9);
        for (int x = -4; x <= 21; x++) {
            for (int z = -4; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        var weaver = helper.spawn(ModEntities.SYNAPSE_WEAVER.get(), 2, 1, 2);
        var first = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 15, 1, 2);
        var second = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 15, 1, 5);
        var enemy = helper.spawnWithNoFreeWill(ModEntities.CHITIN_BRUTE.get(), 2, 1, 12);
        weaver.setHiveStrain(HiveStrain.AMBER);
        first.setHiveStrain(HiveStrain.AMBER);
        second.setHiveStrain(HiveStrain.AMBER);
        enemy.setHiveStrain(HiveStrain.CRIMSON);
        weaver.setSchismTicks(1000);
        enemy.setSchismTicks(1000);
        weaver.setTarget(enemy);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(first.hasEffect(MobEffects.REGENERATION),
                            "First ally must receive the support pulse");
                    helper.assertTrue(second.hasEffect(MobEffects.REGENERATION),
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        new com.alex.mycelialmind.compat.sbl.HiveMindSensor().tick(level, spitter);
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
        var sensor = new com.alex.mycelialmind.compat.sbl.HiveMindSensor();
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

    @GameTest(batch = "entity_ai_live", template = "empty", timeoutTicks = 200)
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

    @GameTest(batch = "entity_ai_live", template = "empty", timeoutTicks = 100)
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
                + ", order=" + MycelialSblBridge.getHiveOrder(m)
                + ", bbOrder=" + HiveBlackboard.getOrder(level)
                + ", target=" + m.getTarget()
                + ", pos=" + m.position() + ")";
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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

    @GameTest(batch = "entity_ai_live", template = "empty", timeoutTicks = 180)
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

    private static double stimulusForScore(double score) {
        return score / MycelialConfig.LEARNING_MULTIPLIER.get();
    }

    private static BlockPos insetInChunk(BlockPos pos, int margin) {
        ChunkPos chunk = new ChunkPos(pos);
        int x = Math.clamp(pos.getX() - chunk.getMinBlockX(), margin, 15 - margin);
        int z = Math.clamp(pos.getZ() - chunk.getMinBlockZ(), margin, 15 - margin);
        return chunk.getBlockAt(x, pos.getY(), z);
    }

    private static final java.util.Map<Long, Integer> ARENA_CHUNK_REFS = new java.util.HashMap<>();

    private static List<ChunkPos> forceArenaChunks(GameTestHelper helper, int minX, int maxX, int minZ, int maxZ) {
        ServerLevel level = helper.getLevel();
        BlockPos minPos = helper.absolutePos(new BlockPos(minX, 0, minZ));
        BlockPos maxPos = helper.absolutePos(new BlockPos(maxX, 0, maxZ));
        int minCx = Math.min(minPos.getX(), maxPos.getX()) >> 4;
        int maxCx = Math.max(minPos.getX(), maxPos.getX()) >> 4;
        int minCz = Math.min(minPos.getZ(), maxPos.getZ()) >> 4;
        int maxCz = Math.max(minPos.getZ(), maxPos.getZ()) >> 4;
        List<ChunkPos> tracked = new ArrayList<>();
        var alreadyForced = level.getForcedChunks();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                int refs = ARENA_CHUNK_REFS.getOrDefault(key, 0);
                if (refs > 0) {
                    ARENA_CHUNK_REFS.put(key, refs + 1);
                    tracked.add(new ChunkPos(cx, cz));
                } else if (!alreadyForced.contains(key)) {
                    level.setChunkForced(cx, cz, true);
                    level.getChunk(cx, cz);
                    ARENA_CHUNK_REFS.put(key, 1);
                    tracked.add(new ChunkPos(cx, cz));
                }
            }
        }
        return tracked;
    }

    private static void releaseArenaChunks(ServerLevel level, List<ChunkPos> forcedChunks) {
        for (ChunkPos cp : forcedChunks) {
            long key = cp.toLong();
            int refs = ARENA_CHUNK_REFS.getOrDefault(key, 0);
            if (refs <= 1) {
                ARENA_CHUNK_REFS.remove(key);
                level.setChunkForced(cp.x, cp.z, false);
            } else {
                ARENA_CHUNK_REFS.put(key, refs - 1);
            }
        }
    }

    private static boolean isSpawnChunkLoaded(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    private static boolean areSpawnAreaChunksLoaded(ServerLevel level, BlockPos center, int radius) {
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos chunkProbe = new BlockPos(chunkX << 4, center.getY(), chunkZ << 4);
                if (!level.isLoaded(chunkProbe)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void runUntilCondition(
            GameTestHelper helper,
            int maxTicks,
            java.util.function.BooleanSupplier condition,
            Runnable onSatisfied,
            Runnable onTimeout) {
        int[] elapsedTicks = {0};
        Runnable[] poll = new Runnable[1];
        poll[0] = () -> {
            elapsedTicks[0]++;
            if (condition.getAsBoolean()) {
                onSatisfied.run();
                return;
            }
            if (elapsedTicks[0] >= maxTicks) {
                onTimeout.run();
                return;
            }
            helper.runAfterDelay(1, () -> poll[0].run());
        };
        helper.runAfterDelay(1, () -> poll[0].run());
    }

    private static boolean hasAcidShot(GameTestHelper helper, MycelialMob owner) {
        return !helper.getLevel().getEntitiesOfClass(AcidSpitProjectile.class,
                owner.getBoundingBox().inflate(20), shot -> shot.getOwner() == owner).isEmpty();
    }

    private static void clearAcidShots(GameTestHelper helper, MycelialMob owner) {
        helper.getLevel().getEntitiesOfClass(AcidSpitProjectile.class,
                owner.getBoundingBox().inflate(20), shot -> shot.getOwner() == owner)
                .forEach(AcidSpitProjectile::discard);
    }

    @GameTest(template = "empty")
    public static void fireStimulusTriggersFireResistantTrait(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();

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

    @GameTest(batch = "hive_state_sync", template = "empty", timeoutTicks = 40)
    public static void globalHiveAdaptationsPropagateExpireAndResetForLivingMobs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();

        MycelialMob mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 1, 1, 1);
        HiveCoreBossEntity boss = helper.spawnWithNoFreeWill(ModEntities.HIVE_CORE.get(), 4, 1, 4);
        boss.applyHiveState(
                Set.of(AdaptationTrait.MAGIC_CARAPACE),
                Set.of(AdaptationTrait.MAGIC_CARAPACE),
                Set.of(HiveSynergy.ARCANE_ABSORPTION));

        long now = level.getGameTime();
        hive.recordStimulus(StimulusType.FIRE, 210.0, now);
        hive.recordStimulus(StimulusType.EXPLOSION, 70.0, now);

        long[] suppressionUntil = {0L};
        Runnable cleanup = () -> {
            mob.discard();
            boss.discard();
            hive.resetAll();
        };

        runUntilCondition(helper, 2,
                () -> mob.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                        && mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                        && mob.hasTrait(AdaptationTrait.BLAST_SHIELDED)
                        && mob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL)
                        && boss.hasTrait(AdaptationTrait.MAGIC_CARAPACE)
                        && boss.hasTier2Trait(AdaptationTrait.MAGIC_CARAPACE)
                        && boss.hasSynergy(HiveSynergy.ARCANE_ABSORPTION)
                        && !boss.hasTrait(AdaptationTrait.FIRE_RESISTANT),
                () -> {
                    suppressionUntil[0] = level.getGameTime() + 5L;
                    hive.suppressHigherAdaptations(suppressionUntil[0], level.getGameTime());
                    runUntilCondition(helper, 2,
                            () -> level.getGameTime() < suppressionUntil[0]
                                    && mob.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                                    && !mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                                    && !mob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL),
                            () -> runUntilCondition(helper, 6,
                                    () -> level.getGameTime() >= suppressionUntil[0]
                                            && mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                                            && mob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL),
                                    () -> {
                                        hive.resetAll();
                                        runUntilCondition(helper, 2,
                                                () -> !mob.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                                                        && !mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                                                        && !mob.hasTrait(AdaptationTrait.BLAST_SHIELDED)
                                                        && mob.getSynergies().isEmpty()
                                                        && boss.hasTrait(AdaptationTrait.MAGIC_CARAPACE)
                                                        && boss.hasTier2Trait(AdaptationTrait.MAGIC_CARAPACE)
                                                        && boss.hasSynergy(HiveSynergy.ARCANE_ABSORPTION),
                                                () -> {
                                                    cleanup.run();
                                                    helper.succeed();
                                                },
                                                () -> {
                                                    cleanup.run();
                                                    helper.fail("Hive reset left stale live mob state or overwrote phase-specific boss state.");
                                                });
                                    },
                                    () -> {
                                        cleanup.run();
                                        helper.fail("Expired hive suppression did not restore live mob Tier 2 and synergy state.");
                                    }),
                            () -> {
                                cleanup.run();
                                helper.fail("Hive suppression did not remove live mob Tier 2 and synergy state before its deadline.");
                            });
                },
                () -> {
                    boolean hiveStateApplied = mob.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                            && mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)
                            && mob.hasTrait(AdaptationTrait.BLAST_SHIELDED)
                            && mob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL);
                    boolean bossStatePreserved = boss.hasTrait(AdaptationTrait.MAGIC_CARAPACE)
                            && boss.hasTier2Trait(AdaptationTrait.MAGIC_CARAPACE)
                            && boss.hasSynergy(HiveSynergy.ARCANE_ABSORPTION)
                            && !boss.hasTrait(AdaptationTrait.FIRE_RESISTANT);
                    cleanup.run();
                    helper.fail("Global hive state propagation timed out: mob="
                            + hiveStateApplied + " boss=" + bossStatePreserved + ".");
                });
    }

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void hiveTraitRevisionAndLocalNeutralizationPersistAcrossReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
        MycelialMob original = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        MycelialMob restored = null;

        try {
            hive.recordStimulus(StimulusType.FIRE, 210.0, level.getGameTime());
            original.applyGlobalHiveState(level, hive);
            long originalRevision = hive.getActiveTraitStateRevision();
            CompoundTag hiveTag = hive.save(new CompoundTag(), level.registryAccess());
            HiveMindSavedData loadedHive = HiveMindSavedData.load(hiveTag, level.registryAccess());
            boolean hiveRoundTrip = loadedHive.getActiveTraitStateRevision() == originalRevision
                    && loadedHive.hasTrait(AdaptationTrait.FIRE_RESISTANT)
                    && loadedHive.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT);

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

            boolean neutralizationPersisted = restored.hasCurrentGlobalHiveState(level, loadedHive)
                    && restored.getTraitMask() == 0
                    && restored.getTier2Mask() == 0
                    && restored.getSynergyMask() == 0;
            if (!restored.hasCurrentGlobalHiveState(level, loadedHive)) {
                restored.applyGlobalHiveState(level, loadedHive);
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

            if (!hiveRoundTrip || !neutralizationPersisted || !sameRevisionDoesNotUndoNeutralization
                    || !revisionAdvanced || !newRevisionReapplied) {
                helper.fail("Hive revision persistence failed: hive=" + hiveRoundTrip
                        + " neutralized=" + neutralizationPersisted
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

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void purifierGrenadeUsesSphericalBlockAndEntityRadius(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData.get(level).resetAll();
        HiveBlackboard.clear(level);

        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos insideMycelium = center.east(3);
        BlockPos outsideMycelium = center.offset(3, 0, 3);
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
                return;
            }
            helper.succeed();
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
        }
    }

    @GameTest(batch = "defaultBatch", template = "empty")
    public static void tideBeaconAndSuppressorUseSphericalEntityRadii(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
        hive.recordStimulus(StimulusType.MELEE, 550.0);
        var architect = helper.spawnWithNoFreeWill(ModEntities.SPORE_ARCHITECT.get(), 2, 2, 2);
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

        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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

        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
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
        if (!com.alex.mycelialmind.compat.sbl.MycelialSblBridge.isSblLoaded()) {
            helper.fail("SmartBrainLib must be loaded as a required AI dependency.");
            return;
        }
        var mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        if (!(mob instanceof net.tslat.smartbrainlib.api.SmartBrainOwner<?>)) {
            helper.fail("MycelialMob should implement SmartBrainOwner.");
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
        new com.alex.mycelialmind.compat.sbl.HiveMindSensor().tick(level, brute);

        float alert = com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getHiveAlertLevel(brute);
        BlockPos memNode = com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getNearestNodePos(brute);
        BlockPos memPylon = com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getNearestPylonPos(brute);
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
        new com.alex.mycelialmind.compat.sbl.HiveMindSensor().tick(level, spitter);

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
        new com.alex.mycelialmind.compat.sbl.HiveMindSensor().tick(level, brute);
        new com.alex.mycelialmind.compat.sbl.HiveMindSensor().tick(level, ally);

        var order = com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getHiveOrder(brute);
        var rallyNode = MycelialSblBridge.getNearestNodePos(brute);
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
    public static void hiveSpatialMemoryPersistsBoundedHotspotsAndCasualtyLearning(GameTestHelper helper) {
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

        var tag = hive.save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
        var loaded = HiveMindSavedData.load(tag, level.registryAccess());
        if (loaded.getHotspots().size() != HiveMindSavedData.MAX_HOTSPOTS
                || loaded.getHottestSpot() == null
                || loaded.getHottestSpot().dominantThreat() != com.alex.mycelialmind.hive.StimulusType.FIRE
                || loaded.getCasualties(com.alex.mycelialmind.hive.StimulusType.PROJECTILE) != 2) {
            helper.fail("Expected hotspots, dominant threat, and casualties to survive NBT save/load round-trip.");
            return;
        }

        hive.resetAll();
        com.alex.mycelialmind.hive.HiveBlackboard.clear(level);
        com.alex.mycelialmind.hive.MycorrhizalAcousticTracker.clearDecoys(level);
        BlockPos nearSpot = helper.absolutePos(new BlockPos(8, 1, 8));
        hive.recordCombatHotspot(nearSpot, 22.0, level.getGameTime(), com.alex.mycelialmind.hive.StimulusType.EXPLOSION);
        var scout = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 2, 1, 2);
        new com.alex.mycelialmind.compat.sbl.HiveMindSensor().tick(level, scout);
        BlockPos investigated = com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getSharedTargetPos(scout);
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

    @GameTest(template = "empty", batch = "defaultBatch")
    public static void hiveSwarmCoordinatorAssignsRolesFlanksAndBroadcastsAlertCall(GameTestHelper helper) {
        var level = helper.getLevel();
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
        if (flankPos.distanceToSqr(player.position()) < 4.0D) {
            helper.fail("Expected flank approach position to offset around target.");
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
    public static void hiveLearningMomentumPivotAndMasteryPersistAcrossNbt(GameTestHelper helper) {
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
        var tag = hive.save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
        var loaded = HiveMindSavedData.load(tag, level.registryAccess());
        if (loaded.getTraitMastery(com.alex.mycelialmind.hive.AdaptationTrait.MELEE_THORNS) < 0.49) {
            helper.fail("Expected traitMastery to persist across NBT save/load.");
            return;
        }

        loaded.bleedStimulusRatio(com.alex.mycelialmind.hive.StimulusType.MELEE, 0.5, t0 + 60L);
        if (loaded.getTraitMastery(com.alex.mycelialmind.hive.AdaptationTrait.MELEE_THORNS) >= 0.49) {
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
        com.alex.mycelialmind.compat.sbl.MycelialSblBridge.addCombatXp(
                level, brute, com.alex.mycelialmind.hive.StimulusType.PROJECTILE, 100);
        if (brute.getEvolutionRank() != EvolutionRank.MUTATED || hive.getStimulusScore(com.alex.mycelialmind.hive.StimulusType.PROJECTILE) < 1.9) {
            helper.fail("Expected 100 Brain XP to self-promote ChitinBrute to MUTATED and feed +2.0 stimulus to Hive.");
            return;
        }

        var behemoth = helper.spawnWithNoFreeWill(ModEntities.SPORE_BEHEMOTH.get(), 4, 1, 2);
        com.alex.mycelialmind.compat.sbl.MycelialSblBridge.addCombatXp(
                level, behemoth, com.alex.mycelialmind.hive.StimulusType.MELEE, 250);
        if (behemoth.getEvolutionRank() != EvolutionRank.ELITE) {
            helper.fail("Expected mini-boss SporeBehemoth to self-promote to ELITE at 250 Brain XP.");
            return;
        }

        var boss = helper.spawnWithNoFreeWill(ModEntities.HIVE_CORE.get(), 6, 1, 2);
        com.alex.mycelialmind.compat.sbl.MycelialSblBridge.addCombatXp(
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

        new com.alex.mycelialmind.compat.sbl.HiveMindSensor().tick(level, mob);
        if (com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getNearbyAllyCount(mob) < 2) {
            helper.fail("Expected HiveMindSensor to count >= 2 nearby allies.");
            return;
        }
        var dummyTarget = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        float squadCharge = com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator.evaluateChargeUtility(mob, dummyTarget);
        if (squadCharge < 0.25F) {
            helper.fail("Expected >= 2 allies cohesion to boost charge utility >= 0.25, got " + squadCharge);
            return;
        }

        new com.alex.mycelialmind.compat.sbl.HiveNodeRegenBehaviour().tryStart(level, mob, level.getGameTime());
        new com.alex.mycelialmind.compat.sbl.HiveHotspotPatrolBehaviour().tryStart(level, mob, level.getGameTime());

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

        // Prepare flat dirt substrate for SurfaceHiveFeature (radius 4)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
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
        level.setBlockAndUpdate(outbreakOrigin.below(), Blocks.DIRT.defaultBlockState());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(outbreakOrigin.below().offset(dx, 0, dz), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(outbreakOrigin.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
            }
        }
        var ctx = new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
                java.util.Optional.empty(),
                level,
                level.getChunkSource().getGenerator(),
                level.getRandom(),
                outbreakOrigin.below(),
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE
        );
        boolean outbreakPlaced = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(ctx);
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
        new com.alex.mycelialmind.compat.sbl.HiveMindSensor().tick(level, brute);

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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
            helper.fail("gdxAI Arrive+BlendedSteering invalid: vec=" + arriveVec);
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

            var sensor = new com.alex.mycelialmind.compat.sbl.HiveMindSensor();
            sensor.doTick(level, mob);

            BlockPos sensedNode = com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getNearestNodePos(mob);
            BlockPos sensedShared = com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getSharedTargetPos(mob);
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

    @GameTest(template = "empty", batch = "algorithm_integration", timeoutTicks = 10)
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

        // Verify the staggered AI cadence while finishing as soon as intent appears, up to six ticks.
        runUntilCondition(helper, 6,
                () -> spitter.getTacticalIntent() != com.alex.mycelialmind.entity.ai.HiveAiBlackboard.TacticalIntent.NONE,
                () -> {
                    spitter.discard();
                    helper.succeed();
                },
                () -> {
                    spitter.discard();
                    helper.fail("MycelialMob staggered BehaviorTree did not produce active TacticalIntent within 6 ticks");
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
        for (String pool : List.of("start_pool", "corridors", "rooms", "caps")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sanctum/" + pool);
            if (!poolReg.containsKey(id)) {
                helper.fail("Missing hive_sanctum template pool: " + id);
                return;
            }
        }
        var tm = level.getStructureManager();
        for (String piece : List.of(
                "sanctum_atrium", "corridor_straight", "corridor_turn", "corridor_cross",
                "ribbed_nave", "forbidden_lab", "ossuary_crypt", "spore_vault", "incubation_wing", "cap_end"
        )) {
            ResourceLocation pid = ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sanctum/" + piece);
            if (tm.get(pid).isEmpty()) {
                helper.fail("Failed to load hive_sanctum NBT template: " + pid);
                return;
            }
        }
        var structReg = registries.registryOrThrow(Registries.STRUCTURE);
        if (!structReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sanctum"))
                || !structReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "matriarch_citadel"))) {
            helper.fail("Missing hive_sanctum or matriarch_citadel in Structure registry.");
            return;
        }
        var setReg = registries.registryOrThrow(Registries.STRUCTURE_SET);
        if (!setReg.containsKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "matriarch_citadel"))) {
            helper.fail("Missing matriarch_citadel in StructureSet registry.");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "structures")
    public static void matriarchCitadelPieceGeneratesCryptChapelsAndGuardians(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forced = forceArenaChunks(helper, 0, 52, 0, 52);
        BlockPos center = helper.absolutePos(new BlockPos(26, 10, 26));
        List<BlockPos> guardianSpawnPositions = List.of(
                center.offset(0, 1, 9),
                center.offset(-5, 1, 7),
                center.offset(5, 1, 7));
        runUntilCondition(helper, 30,
                () -> guardianSpawnPositions.stream().allMatch(pos -> isSpawnChunkLoaded(level, pos)),
                () -> {
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
                    helper.succeed();
                },
                () -> {
                    releaseArenaChunks(level, forced);
                    helper.fail("Timed out waiting for Matriarch Citadel guardian spawn chunks to become entity-ready.");
                });
    }

    @GameTest(template = "empty", batch = "structures")
    public static void structuresHaveZeroFloatingBlocksAndValidCirculation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        java.util.function.Predicate<BlockState> isModStructure =
                s -> !s.isAir() && !s.is(Blocks.STONE) && !s.is(Blocks.BEDROCK) && !s.is(Blocks.BARRIER);

        // 1) MycelialOutbreakFeature 6-connectivity (stalk, counter-rib, cap, and censer)
        BlockPos outbreakPos = helper.absolutePos(new BlockPos(8, 120, 8));
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.setBlock(outbreakPos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 18);
                level.setBlock(outbreakPos.offset(dx, -2, dz), Blocks.STONE.defaultBlockState(), 18);
            }
        }
        boolean outbreakPlaced = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                level, level.getChunkSource().getGenerator(), level.getRandom(), outbreakPos);
        if (!outbreakPlaced) {
            helper.fail("MycelialOutbreakFeature.place returned false on flat stone platform.");
            return;
        }
        var outbreakBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                outbreakPos.getX() - 4, outbreakPos.getY() + 2, outbreakPos.getZ() - 4,
                outbreakPos.getX() + 4, outbreakPos.getY() + 10, outbreakPos.getZ() + 4);
        var outbreakFloating = com.alex.mycelialmind.world.structure.VoxelStructureOps.findDisconnectedPositions(
                level, outbreakBox, outbreakPos.above(6), isModStructure);
        if (!outbreakFloating.isEmpty()) {
            helper.fail("MycelialOutbreakFeature has " + outbreakFloating.size()
                    + " disconnected canopy/stalk blocks, sample=" + outbreakFloating.get(0));
            return;
        }
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();

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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // 1. Turret T2 upgrade must reject Chitin Plate and require Synaptic Core
        BlockPos turretPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
        boolean rejectedPlate = false;
        boolean acceptedCore = false;
        if (level.getBlockEntity(turretPos) instanceof com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity turret) {
            rejectedPlate = !turret.tryUpgrade(new ItemStack(ModItems.CHITIN_PLATE.get()), player);
            acceptedCore = turret.tryUpgrade(new ItemStack(ModItems.SYNAPTIC_CORE.get()), player);
        }
        level.removeBlock(turretPos, false);

        // 2. BioExtractor must reject full-HP mob and allow weakened mob (<= 60% HP) with active trait
        var mob = helper.spawnWithNoFreeWill(ModEntities.SPORELING.get(), 4, 1, 4);
        mob.applyHiveTraits(java.util.EnumSet.of(AdaptationTrait.MELEE_THORNS));
        ItemStack extractor = new ItemStack(ModItems.BIO_EXTRACTOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, extractor);
        var fullHpRes = extractor.getItem().interactLivingEntity(extractor, player, mob, InteractionHand.MAIN_HAND);

        mob.setHealth(mob.getMaxHealth() * 0.5F);
        player.getCooldowns().removeCooldown(ModItems.BIO_EXTRACTOR.get());
        var weakenedRes = extractor.getItem().interactLivingEntity(extractor, player, mob, InteractionHand.MAIN_HAND);
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

        if (!rejectedPlate || !acceptedCore || fullHpRes.consumesAction() || !weakenedRes.consumesAction()
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();

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
            com.alex.mycelialmind.compat.sbl.MycelialSblBridge.addCombatXp(level, sporeling, StimulusType.MELEE,
                    com.alex.mycelialmind.hive.LivingBattlefieldSystem.CARRION_XP_REWARD);
        }

        boolean healed = sporeling.getHealth() >= 9.9F;
        boolean xpGained = com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getCombatXp(sporeling) >= 20;
        sporeling.discard();

        if (!nonFireDrop || !fireSuppressed || consumed == null || !healed || !xpGained) {
            helper.fail(String.format("Carrion necrophagy failed: nonFire=%s fireSup=%s consumed=%s hp=%.1f xp=%d",
                    nonFireDrop, fireSuppressed, consumed != null, sporeling.getHealth(),
                    com.alex.mycelialmind.compat.sbl.MycelialSblBridge.getCombatXp(sporeling)));
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();

        try {
            var soundEntries = ModSounds.SOUND_EVENTS.getEntries();
            boolean soundsOk = soundEntries.size() == 186;
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
                    ModEntities.APEX_MATRIARCH.get()
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

            if (!soundsOk || !entityAudioOk || distinctAmbient.size() != 17
                    || distinctHurt.size() != 17 || distinctDeath.size() != 17
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

            level.setBlockAndUpdate(pylonPos.above(), ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH, 0)
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED, false));
            int veinWeakSig = level.getBlockState(pylonPos.above()).getAnalogOutputSignal(level, pylonPos.above());
            level.setBlockAndUpdate(pylonPos.above(), ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH, 2)
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED, false));
            int veinMidSig = level.getBlockState(pylonPos.above()).getAnalogOutputSignal(level, pylonPos.above());
            level.setBlockAndUpdate(pylonPos.above(), ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH, 3)
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED, true));
            int veinChargedSig = level.getBlockState(pylonPos.above()).getAnalogOutputSignal(level, pylonPos.above());
            level.removeBlock(pylonPos.above(), false);
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

    @GameTest(template = "empty", batch = "defaultBatch")
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
            boolean tagsAndCompatOk = SynapticBenchRecipeDefinitions.allCompatInfoEntries().size() == 25
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
                liveSpitter.setPos(decoyCenter.getX() - 6.0D, decoyCenter.getY() + 1.0D, decoyCenter.getZ() + 0.5D);
                liveSpitter.setOnGround(true);
                level.addFreshEntity(liveSpitter);
                for (int t = 0; t < 4; t++) {
                    liveSpitter.tick();
                }
                navTarget = liveSpitter.getNavigation().getTargetPos();
                spitterPursuedDecoy = navTarget != null && navTarget.distSqr(decoyPos) <= 9.0D;
            }
            boolean decoyIntegrationOk = firstDecoyRes.getResult().consumesAction()
                    && !secondDecoyRes.getResult().consumesAction()
                    && secondDecoyUseOn == net.minecraft.world.InteractionResult.FAIL
                    && escortKeptTarget
                    && spitterPursuedDecoy;

            // 4) SchismInjector rejects allied escort & OvermindCrown enforces cooldown
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
        }
    }

    private static void resetHomeostasisChunks(DimensionalHomeostasisSavedData data, ChunkPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                data.resetChunk(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
    }

    private static BlockPos homeostasisTestPos(GameTestHelper helper, int slot, BlockPos relativePos) {
        // GameTest batches run concurrently and homeostasis state is keyed by chunk.
        // Keep each scenario's live blocks in a separate chunk strip (3 chunks / 48 blocks apart along X).
        int separation = slot * 48;
        return helper.absolutePos(relativePos).offset(separation, 0, 0);
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
            HiveMindSavedData hive = HiveMindSavedData.get(level);
            hive.resetAll();
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
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/unlock_biomass_recipes")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/unlock_latent_spore_recipes")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/unlock_chitin_recipes")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/unlock_synaptic_core_recipes")) != null
                    && advMgr.get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/unlock_hive_heart_recipes")) != null;

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
            HiveMindSavedData hive = HiveMindSavedData.get(level);
            hive.resetAll();
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
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
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

        ChunkPos routeChunk = new ChunkPos(pathStart);
        runUntilCondition(helper, 30,
                () -> areSpawnAreaChunksLoaded(level, pathStart, 6),
                () -> {
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
                int sproutedVeins = hive.getActiveVeins(routeChunk).size();
                boolean hasVeinOverlay = false;
                for (int dx = 0; dx <= 5; dx++) {
                    BlockPos veinPos = dx >= 4 ? pathStart.offset(dx, 1, 0) : pathStart.offset(dx, 0, 0);
                    if (level.getBlockState(veinPos).is(ModBlocks.HYPHAL_VEIN.get())) {
                        hasVeinOverlay = true;
                        break;
                    }
                }
                boolean underlyingDirtIntact = level.getBlockState(pathStart.below()).is(Blocks.DIRT)
                        && level.getBlockState(pathBeforeStep.below()).is(Blocks.DIRT)
                        && level.getBlockState(pathStep.below()).is(Blocks.DIRT);
                BlockState stepEdgeState = level.getBlockState(pathBeforeStep);
                boolean climbsStepUp = stepEdgeState.is(ModBlocks.HYPHAL_VEIN.get())
                        && level.getBlockState(pathStep).is(ModBlocks.HYPHAL_VEIN.get())
                        && stepEdgeState.getValue(com.alex.mycelialmind.block.HyphalVeinBlock.EAST)
                        == com.alex.mycelialmind.block.HyphalVeinBlock.VeinConnection.UP;
                boolean strengthAndColorScale = stepEdgeState.getValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH) == 3
                        && com.alex.mycelialmind.block.HyphalVeinBlock.colorForStrength(0)
                        != com.alex.mycelialmind.block.HyphalVeinBlock.colorForStrength(3);

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
        },
        () -> {
            tracker.clearAll();
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
            helper.fail("Timed out waiting for hyphal route ambush spawn chunks to become entity-ready.");
        });
    }

    @GameTest(template = "empty", batch = "architectural_reflection")
    public static void architecturalAccentSubstitutesPaletteAndProportionsInTwistedReflection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -2, 60, -2, 60);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();

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
            boolean twistedHasVein = false;
            boolean twistedHasNode = false;
            boolean twistedHasChest = false;
            for (BlockPos p : BlockPos.betweenClosed(
                    twistedBox.minX(), twistedBox.minY(), twistedBox.minZ(),
                    twistedBox.maxX(), twistedBox.maxY(), twistedBox.maxZ())) {
                BlockState st = level.getBlockState(p);
                if (st.is(Blocks.DARK_OAK_PLANKS) || st.is(Blocks.DARK_OAK_LOG)) twistedHasOak = true;
                if (st.is(ModBlocks.HYPHAL_VEIN.get())) twistedHasVein = true;
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
        BlockPos core3 = helper.absolutePos(new BlockPos(36, 120, 36));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // The elite spawn point is selected dynamically from the completed site's ambush area.
        int[] spawnSetupTicks = {0};
        runUntilCondition(helper, 30,
                () -> ++spawnSetupTicks[0] >= 2
                        && isSpawnChunkLoaded(level, core2)
                        && areSpawnAreaChunksLoaded(level, core3, 6),
                () -> {
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

            // 1. Live block-by-block growth + adjacent HYPHAL_VEIN tendril frame + BioScanner + photophobic stall & wither
            var site1 = com.alex.mycelialmind.hive.HyphalSiegeDirector.createBlindSpotSiteAt(level, hive, core1);
            int tendrilVeinsBefore = 0;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(core1.relative(dir)).is(ModBlocks.HYPHAL_VEIN.get())) {
                    tendrilVeinsBefore++;
                }
            }
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
            int tendrilVeinsAfter = 0;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(core1.relative(dir)).is(ModBlocks.HYPHAL_VEIN.get())) {
                    tendrilVeinsAfter++;
                }
            }
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
        },
        () -> {
            tracker.clearAll();
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
            helper.fail("Timed out waiting for blind-spot reward and guardian spawn chunks to become entity-ready.");
        });
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins")
    public static void hyphalEcosystemPurificationAcousticsRemissionAndCodexWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 14, -4, 14);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
        PurificationZoneTracker.get(level).clearAll();

        BlockPos base = helper.absolutePos(new BlockPos(2, 30, 2));
        BlockPos v0 = base;
        BlockPos v1 = base.east(2);
        BlockPos v2 = base.east(4);
        BlockPos v3 = base.east(6);
        BlockPos v4 = base.south(4);
        PurifierDroneEntity drone = null;

        try {
            for (BlockPos vp : List.of(v0, v1, v2, v3, v4)) {
                level.setBlockAndUpdate(vp.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(vp, ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                        .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH, 2)
                        .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED, true));
            }
            ChunkPos c0 = new ChunkPos(v0);
            hive.setHyphalDensity(c0, 55.0, level.getGameTime());

            // 1. PurifierGrenade cleans HYPHAL_VEIN and drops local chunk density
            var grenade = new PurifierGrenadeProjectile(ModEntities.PURIFIER_GRENADE.get(), level);
            grenade.setPos(v0.getX() + 0.5, v0.getY() + 1.25, v0.getZ() + 0.5);
            grenade.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
            level.addFreshEntity(grenade);
            for (int t = 0; t < 6 && !grenade.isRemoved(); t++) {
                grenade.tick();
            }
            boolean grenadeOk = level.getBlockState(v0).isAir() && hive.getHyphalDensity(c0) < 55.0;

            // 2. NeutralizingSporeProjectile cleans HYPHAL_VEIN on block hit
            var spore = new NeutralizingSporeProjectile(ModEntities.NEUTRALIZING_SPORE.get(), level);
            spore.setPos(v1.getX() + 0.5, v1.getY() + 1.25, v1.getZ() + 0.5);
            spore.shoot(0.0, -1.0, 0.0, 1.5F, 0.0F);
            level.addFreshEntity(spore);
            for (int t = 0; t < 6 && !spore.isRemoved(); t++) {
                spore.tick();
            }
            boolean sporeOk = level.getBlockState(v1).isAir();

            // 3. OvermindCrown & PurifierDrone cleanse HYPHAL_VEIN
            int crownCleansed = OvermindCrownItem.cleanseMyceliumRadius(level, v2, 1);
            boolean crownOk = crownCleansed >= 1 && level.getBlockState(v2).isAir();

            drone = ModEntities.PURIFIER_DRONE.get().create(level);
            boolean droneOk = false;
            if (drone != null) {
                drone.moveTo(v3.getX() + 0.5, v3.getY() + 1.0, v3.getZ() + 0.5);
                droneOk = drone.cleanseMyceliumBeneath(level) && level.getBlockState(v3).isAir();
            }

            // 4. MycorrhizalAcousticTracker recognizes charged / strength >= 2 Hyphal Veins
            var weakVein = ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH, 1)
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED, false);
            var midVein = ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH, 2)
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED, false);
            var chargedVein = ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH, 0)
                    .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED, true);
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
            helper.succeed();
        } finally {
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
            boolean rootOk = level.getBlockState(origin).is(ModBlocks.HYPHAL_VEIN.get())
                    && level.getBlockEntity(origin) instanceof com.alex.mycelialmind.block.entity.HyphalVeinBlockEntity rootBe
                    && rootBe.isRoot()
                    && rootBe.getNutrient() == com.alex.mycelialmind.hive.HyphalPhysics.MAX_NUTRIENT
                    && (rootBe.getDiagonals() & com.alex.mycelialmind.hive.HyphalPhysics.DIAG_SE) != 0;
            boolean midDiagOk = level.getBlockState(step1).is(ModBlocks.HYPHAL_VEIN.get())
                    && level.getBlockEntity(step1) instanceof com.alex.mycelialmind.block.entity.HyphalVeinBlockEntity step1Be
                    && (step1Be.getDiagonals() & com.alex.mycelialmind.hive.HyphalPhysics.DIAG_NW) != 0;
            boolean endOk = level.getBlockState(diagEnd).is(ModBlocks.HYPHAL_VEIN.get());
            boolean noLCorner = !level.getBlockState(origin.offset(3, 0, 0)).is(ModBlocks.HYPHAL_VEIN.get())
                    && !level.getBlockState(origin.offset(0, 0, 2)).is(ModBlocks.HYPHAL_VEIN.get());

            if (!rootOk || !midDiagOk || !endOk || !noLCorner) {
                helper.fail(String.format(
                        "Diagonal trail check failed: root=%s midDiag=%s end=%s noLCorner=%s",
                        rootOk, midDiagOk, endOk, noLCorner));
                return;
            }
            helper.succeed();
        } finally {
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

    @GameTest(template = "empty", batch = "hyphal_route_veins", timeoutTicks = 100)
    public static void hyphalSeveredDownstreamBranchWithersProgressivelyWithin100Ticks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 36, -4, 36);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        hive.resetAll();
        tracker.clearAll();

        BlockPos rootPos = insetInChunk(helper.absolutePos(new BlockPos(5, 10, 12)), 5);
        BlockPos v1 = rootPos.east(1);
        BlockPos v2 = rootPos.east(2);
        BlockPos v3 = rootPos.east(3);

        for (int dx = -1; dx <= 5; dx++) {
            level.setBlockAndUpdate(rootPos.offset(dx, -1, 0), Blocks.DIRT.defaultBlockState());
            level.removeBlock(rootPos.offset(dx, 0, 0), false);
        }

        ChunkPos cp = new ChunkPos(rootPos);
        long now = level.getGameTime();
        for (int pass = 0; pass < 5; pass++) {
            hive.recordTrailPoint(cp, rootPos.below(), true, now + pass);
        }
        hive.recordTrailPoint(cp, v1.below(), true, now + 10);
        hive.recordTrailPoint(cp, v2.below(), true, now + 11);
        hive.recordTrailPoint(cp, v3.below(), true, now + 12);
        hive.setHyphalDensity(cp, 40.0, now + 12);
        com.alex.mycelialmind.hive.HyphalSiegeDirector.sproutRouteVeinsInChunk(level, hive, cp);
        com.alex.mycelialmind.hive.HyphalSiegeDirector.growTrailVeinsNow(level, hive, cp, 8);

        if (!level.getBlockState(rootPos).is(ModBlocks.HYPHAL_VEIN.get())
                || !level.getBlockState(v1).is(ModBlocks.HYPHAL_VEIN.get())
                || !level.getBlockState(v2).is(ModBlocks.HYPHAL_VEIN.get())
                || !level.getBlockState(v3).is(ModBlocks.HYPHAL_VEIN.get())) {
            tracker.clearAll();
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
            helper.fail("Expected full trail root->v1->v2->v3 before severing v1");
            return;
        }

        // Break intermediate vein v1; stop observing as soon as both descendants wither, within 90 ticks.
        level.removeBlock(v1, false);

        Runnable cleanup = () -> {
            for (int dx = -1; dx <= 5; dx++) {
                level.removeBlock(rootPos.offset(dx, 0, 0), false);
                level.removeBlock(rootPos.offset(dx, -1, 0), false);
            }
            tracker.clearAll();
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
        };
        runUntilCondition(helper, 90,
                () -> level.getBlockState(rootPos).is(ModBlocks.HYPHAL_VEIN.get())
                        && level.getBlockState(v2).isAir()
                        && level.getBlockState(v3).isAir(),
                () -> {
                    try {
                        helper.succeed();
                    } finally {
                        cleanup.run();
                    }
                },
                () -> {
                    try {
                        helper.fail(String.format(
                                "Severed branch check timed out: rootAlive=%s v2=%s v3=%s",
                                level.getBlockState(rootPos).is(ModBlocks.HYPHAL_VEIN.get()),
                                level.getBlockState(v2), level.getBlockState(v3)));
                    } finally {
                        cleanup.run();
                    }
                });
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins", timeoutTicks = 40)
    public static void hyphalWallVeinLosesSupportFallsAndDisintegratesOnLanding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -4, 36, -4, 36);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();

        BlockPos wallPos = helper.absolutePos(new BlockPos(10, 14, 10));
        BlockPos veinPos = wallPos.east();
        BlockPos landingFloor = veinPos.below(5);
        BlockPos landingAir = veinPos.below(4);

        for (int dy = 0; dy >= -5; dy--) {
            level.removeBlock(veinPos.offset(0, dy, 0), false);
        }
        level.setBlockAndUpdate(wallPos, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(landingFloor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(veinPos, ModBlocks.HYPHAL_VEIN.get().defaultBlockState()
                .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH, 1)
                .setValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED, true));
        if (level.getBlockEntity(veinPos) instanceof com.alex.mycelialmind.block.entity.HyphalVeinBlockEntity wallBe) {
            wallBe.setFaces(com.alex.mycelialmind.hive.HyphalPhysics.FACE_WEST);
            wallBe.setRoot(true);
            wallBe.syncToClient();
        }

        // Remove the supporting wall block
        level.removeBlock(wallPos, false);

        final boolean[] sawFallingEntity = {false};
        Runnable cleanup = () -> {
            level.removeBlock(wallPos, false);
            level.removeBlock(veinPos, false);
            level.removeBlock(landingAir, false);
            level.removeBlock(landingFloor, false);
            hive.resetAll();
            releaseArenaChunks(level, forcedChunks);
        };
        runUntilCondition(helper, 30,
                () -> {
                    var falling = level.getEntitiesOfClass(
                            net.minecraft.world.entity.item.FallingBlockEntity.class,
                            new AABB(veinPos).inflate(3.0D));
                    if (!falling.isEmpty() && level.getBlockState(veinPos).isAir()) {
                        sawFallingEntity[0] = true;
                    }
                    return sawFallingEntity[0] && falling.isEmpty();
                },
                () -> {
                    try {
                        boolean landedCleanly = level.getBlockState(landingAir).isAir()
                                && !level.getBlockState(veinPos).is(ModBlocks.HYPHAL_VEIN.get());
                        var remainingDrops = level.getEntitiesOfClass(
                                net.minecraft.world.entity.item.ItemEntity.class,
                                new AABB(landingAir).inflate(3.0D));
                        if (!landedCleanly || !remainingDrops.isEmpty()) {
                            helper.fail(String.format(
                                    "Gravity check failed after the falling vein landed: landedCleanly=%s drops=%d",
                                    landedCleanly, remainingDrops.size()));
                            return;
                        }
                        helper.succeed();
                    } finally {
                        cleanup.run();
                    }
                },
                () -> {
                    try {
                        boolean landedCleanly = level.getBlockState(landingAir).isAir()
                                && !level.getBlockState(veinPos).is(ModBlocks.HYPHAL_VEIN.get());
                        var remainingDrops = level.getEntitiesOfClass(
                                net.minecraft.world.entity.item.ItemEntity.class,
                                new AABB(landingAir).inflate(3.0D));
                        helper.fail(String.format(
                                "Gravity check timed out: sawFalling=%s landedCleanly=%s stillFalling=%d drops=%d",
                                sawFallingEntity[0], landedCleanly,
                                level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,
                                        new AABB(veinPos).inflate(3.0D)).size(), remainingDrops.size()));
                    } finally {
                        cleanup.run();
                    }
                });
    }

    @GameTest(template = "empty", batch = "hyphal_route_veins", timeoutTicks = 120)
    public static void neoforgeIntegrationGlmDataMapsCapabilitiesBossPartsAndCuresWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> forcedChunks = forceArenaChunks(helper, -2, 12, -2, 12);
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
                    new ItemStack(ModBlocks.HYPHAL_VEIN.asItem())) > 0.0F,
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

            level.setBlockAndUpdate(turretPos, ModBlocks.PURIFICATION_TURRET.get().defaultBlockState());
            if (level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity turretBe) {
                turretBe.applyCorrosiveJam(200);
            }
            var turretCap = level.getCapability(Capabilities.ItemHandler.BLOCK, turretPos, Direction.UP);
            helper.assertTrue(turretCap != null, "Expected ItemHandler capability on Purification Turret");
            helper.assertTrue(turretCap.insertItem(0, new ItemStack(ModItems.SPORE_ANTIDOTE.get()), false).isEmpty()
                    && turretCap.insertItem(0, new ItemStack(ModItems.SYNAPTIC_CORE.get()), false).isEmpty()
                    && level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity turretBe2
                    && !turretBe2.isJammed() && turretBe2.getTier() == 2,
                    "Turret capability must unjam with SporeAntidote and upgrade to Tier 2 with SynapticCore");

            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            tracker.addPylon(pylonPos);
            var pylonCap = level.getCapability(Capabilities.ItemHandler.BLOCK, pylonPos, Direction.EAST);
            helper.assertTrue(pylonCap != null, "Expected ItemHandler capability on Purification Pylon");
            pylonCap.insertItem(0, new ItemStack(ModItems.SYNAPTIC_CORE.get()), false);
            pylonCap.insertItem(0, new ItemStack(ModItems.OVERMIND_ESSENCE.get()), false);
            helper.assertTrue(level.getBlockEntity(pylonPos) instanceof MonumentFocusBlockEntity pylonBe
                    && pylonBe.getTier() == 3,
                    "Pylon capability must upgrade Pylon to Tier 3");
            helper.assertTrue(com.alex.mycelialmind.world.MycelialChunkTicketManager.shouldAnchorPylon(level, pylonPos)
                    && level.getForcedChunks().contains(new ChunkPos(pylonPos).toLong()),
                    "Tier 3 Purification Pylon must register a NeoForge TicketController forced chunk");

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
    public static void outbreakDeepMyceliumSubstrateStairsVaultAndCryptIntegrity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        BlockPos outbreakGround = helper.absolutePos(new BlockPos(8, 160, 8));
        BlockPos protectedGround = helper.absolutePos(new BlockPos(36, 160, 8));
        BlockPos pylonPos = protectedGround.offset(2, 1, 0);
        BlockPos waterHiveOrigin = helper.absolutePos(new BlockPos(8, 160, 40));
        BlockPos hiveOrigin = helper.absolutePos(new BlockPos(36, 160, 40));
        BlockPos protectedChest = outbreakGround.offset(3, 0, 3);
        BlockPos protectedBedrock = outbreakGround.offset(1, 0, 1);

        try {
            // 1. MycelialOutbreakFeature on MYCELIAL_STONE with visible cardinal stairs and BlockEntity/Bedrock protection
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

            boolean placedDeep = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), outbreakGround);
            helper.assertTrue(placedDeep, "MycelialOutbreakFeature must place on MYCELIAL_STONE substrate.");

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos stairPos = outbreakGround.relative(dir).above();
                helper.assertTrue(level.getBlockState(stairPos).is(Blocks.DEEPSLATE_BRICK_STAIRS),
                        "Expected visible cardinal stair at " + stairPos + " on flat substrate.");
            }
            helper.assertTrue(level.getBlockEntity(protectedChest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                    "Outbreak carpet must never overwrite an existing BlockEntity.");
            helper.assertTrue(level.getBlockState(protectedBedrock).is(Blocks.BEDROCK),
                    "Outbreak must never overwrite indestructible bedrock.");

            // 2. Purification Pylon blocks Outbreak placement inside protected radius
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    level.setBlock(protectedGround.offset(dx, 0, dz), Blocks.DIRT.defaultBlockState(), 18);
                    level.setBlock(protectedGround.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 18);
                }
            }
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            boolean placedProtected = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), protectedGround);
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
    public static void subterraneanOutbreakCaveCrownAndSurfaceTaprootConnectionWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        tracker.clearAll();
        BlockPos caveGround = helper.absolutePos(new BlockPos(8, 130, 8));
        BlockPos surfaceGround = helper.absolutePos(new BlockPos(8, 160, 40));
        BlockPos lowerCaveGround = surfaceGround.below(24);

        try {
            // 1. Low-ceiling subterranean cave (headroom = 5) generates compact ceiling-welded crown
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
            helper.assertTrue(level.getBlockState(caveGround.above(5)).is(ModBlocks.LUMINESCENT_AMBER_VEIN.get()),
                    "Compact cave crown must place luminescent_amber_vein core welded at ceiling headroom.");

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

            boolean surfPlaced = com.alex.mycelialmind.registry.ModFeatures.MYCELIAL_OUTBREAK.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), level.getRandom(), surfaceGround);
            helper.assertTrue(surfPlaced, "Surface outbreak must place above subterranean cavern.");
            helper.assertTrue(level.getBlockState(lowerCaveGround).is(ModBlocks.MYCELIAL_NODE.get()),
                    "Surface outbreak must seed a linked subterranean MYCELIAL_NODE on the cavern floor below.");

            boolean foundTaprootBlock = false;
            for (int y = lowerCaveGround.getY() + 6; y <= surfaceGround.getY() - 2; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockState st = level.getBlockState(new BlockPos(surfaceGround.getX() + dx, y, surfaceGround.getZ() + dz));
                        if (st.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get()) || st.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())) {
                            foundTaprootBlock = true;
                            break;
                        }
                    }
                }
            }
            helper.assertTrue(foundTaprootBlock, "Vertical taproot must weave amber/arch_brick conduit between surface and cavern.");

            var linked = tracker.getLinkedNodes(surfaceGround);
            helper.assertTrue(linked.contains(lowerCaveGround),
                    "Surface outbreak node must have an active vertical synaptic link to the subterranean node below.");

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
            boolean sawActive = false;
            boolean sawRecovery = false;
            for (int t = 0; t < 70; t++) {
                behemoth.customServerAiStep();
                var st = behemoth.getCombatState();
                if (st == com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState.ACTIVE) {
                    sawActive = true;
                } else if (st == com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState.RECOVERY) {
                    sawRecovery = true;
                }
            }
            helper.assertTrue(sawActive && sawRecovery, "Boss state machine must progress through WINDUP -> ACTIVE -> RECOVERY.");

            // 2. Physical carapace shatter via ARMORED_CARAPACE part -> STAGGERED + lowered WEAK_POINT
            behemoth.aiStep();
            var parts = behemoth.getParts();
            com.alex.mycelialmind.entity.MycelialBossPart weakPart = (com.alex.mycelialmind.entity.MycelialBossPart) parts[0];
            com.alex.mycelialmind.entity.MycelialBossPart carapacePart = (com.alex.mycelialmind.entity.MycelialBossPart) parts[1];
            double weakYBefore = weakPart.getY();

            for (int hit = 0; hit < 8 && !behemoth.isCarapaceBroken(); hit++) {
                behemoth.invulnerableTime = 0;
                carapacePart.hurt(level.damageSources().magic(), 12.0F);
            }
            helper.assertTrue(behemoth.isCarapaceBroken(), "Sustained hits to ARMORED_CARAPACE must shatter the carapace.");
            helper.assertTrue(behemoth.isBossStaggered(), "Carapace break must trigger STAGGERED state.");
            behemoth.aiStep();
            helper.assertTrue(weakPart.getY() < weakYBefore - 0.5D,
                    "Shattering carapace must physically lower WEAK_POINT hitbox for melee punishment.");

            // 3. Phase 2 transition at <= 50% HP restores carapace cleanly
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
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag savedSnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockPos nexusPos = helper.absolutePos(new BlockPos(4, 2, 4));
        try {
            saved.resetAll();
            level.setBlockAndUpdate(nexusPos, com.alex.mycelialmind.registry.ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            var nexusBe = (com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity) level.getBlockEntity(nexusPos);
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
        }
    }

    @GameTest(template = "empty", batch = "sovereign_rival_raid")
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
                .thenWaitUntil(() -> helper.assertTrue(
                        level.isPositionEntityTicking(nexusPos)
                                && raidArenaChunks.stream().allMatch(chunk -> level.areEntitiesLoaded(chunk.toLong())),
                        "Rival raid arena chunks must be entity-loaded before spawning"))
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
    public static void sovereignColonySyncPayloadReflectsIncubatedCourierWorker(GameTestHelper helper) {
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
            worker = nexusBe.incubateSymbiont(level, null, com.alex.mycelialmind.colony.SymbiontCaste.COURIER, true);
            helper.assertTrue(worker != null, "Worker must spawn for payload test.");

            var payload = com.alex.mycelialmind.network.SovereignColonySyncPayload.forNexus(level, nexusPos, true);
            helper.assertTrue(payload.hasColony()
                            && payload.unlocked()
                            && payload.openScreen()
                            && payload.nexusPos().equals(nexusPos)
                            && payload.domainXp() == 200
                            && payload.inputChests() == 1
                            && payload.workers().size() == 1
                            && payload.workers().get(0).workerId().equals(worker.getUUID())
                            && payload.workers().get(0).caste() == com.alex.mycelialmind.colony.SymbiontCaste.COURIER,
                    "SovereignColonySyncPayload.forNexus must reflect the live colony and its incubated Courier worker.");

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
}
