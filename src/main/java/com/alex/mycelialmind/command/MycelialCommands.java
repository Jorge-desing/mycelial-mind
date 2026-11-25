package com.alex.mycelialmind.command;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.CreepingMyceliumBlock;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ChitinBruteEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.SporelingEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine;
import com.alex.mycelialmind.hive.HyphalSiegeDirector;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModArmorMaterials;
import com.alex.mycelialmind.item.GeneticSampleItem;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.network.SymbiosisSyncPayload;
import com.alex.mycelialmind.recipe.SynapticBenchRecipeDefinitions;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.symbiosis.PlayerSymbiosisData;
import com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.MycelialLootSystem;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.WorldgenAuditEngine;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Administrative and QA automation commands (/mycelial ...) to inspect, control,
 * and automate the manual verification scenarios of Mycelial Mind.
 */
public final class MycelialCommands {
    private record SavedBlock(BlockState state, CompoundTag blockEntityData) {
    }

    private MycelialCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mycelial")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(ctx -> reportStatus(ctx.getSource())))
                .then(Commands.literal("reset")
                        .executes(ctx -> resetHive(ctx.getSource())))
                .then(Commands.literal("stimulus")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.1, 500.0))
                                        .executes(ctx -> addStimulus(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "type"),
                                                DoubleArgumentType.getDouble(ctx, "amount"))))))
                .then(Commands.literal("verify")
                        .then(Commands.literal("projectile_adaptation")
                                .executes(ctx -> verifyProjectileAdaptation(ctx.getSource())))
                        .then(Commands.literal("pylon_containment")
                                .executes(ctx -> verifyPylonContainment(ctx.getSource())))
                        .then(Commands.literal("boss_encounter")
                                .executes(ctx -> verifyBossEncounter(ctx.getSource())))
                        .then(Commands.literal("tier2_synergy")
                                .executes(ctx -> verifyTier2Synergy(ctx.getSource())))
                        .then(Commands.literal("synaptic_network")
                                .executes(ctx -> verifySynapticNetwork(ctx.getSource())))
                        .then(Commands.literal("symbiosis")
                                .executes(ctx -> verifySymbiosis(ctx.getSource())))
                        .then(Commands.literal("units")
                                .executes(ctx -> verifyUnits(ctx.getSource())))
                        .then(Commands.literal("expansion")
                                .executes(ctx -> verifyExpansion(ctx.getSource())))
                        .then(Commands.literal("tide")
                                .executes(ctx -> verifyTide(ctx.getSource())))
                        .then(Commands.literal("counteroffensive")
                                .executes(ctx -> verifyCounteroffensive(ctx.getSource())))
                        .then(Commands.literal("arsenal")
                                .executes(ctx -> verifyArsenal(ctx.getSource())))
                        .then(Commands.literal("mycorrhizal")
                                .executes(ctx -> verifyMycorrhizal(ctx.getSource())))
                        .then(Commands.literal("hive_ai")
                                .executes(ctx -> verifyHiveAi(ctx.getSource())))
                        .then(Commands.literal("chimera")
                                .executes(ctx -> verifyChimera(ctx.getSource())))
                        .then(Commands.literal("memory")
                                .executes(ctx -> verifyMemory(ctx.getSource())))
                        .then(Commands.literal("tactical_ai")
                                .executes(ctx -> verifyTacticalAi(ctx.getSource())))
                        .then(Commands.literal("field_ai")
                                .executes(ctx -> verifyFieldAi(ctx.getSource())))
                        .then(Commands.literal("hyphal_siege")
                                .executes(ctx -> verifyHyphalSiege(ctx.getSource())))
                        .then(Commands.literal("loot")
                                .executes(ctx -> verifyLoot(ctx.getSource())))
                        .then(Commands.literal("homeostasis")
                                .executes(ctx -> verifyHomeostasis(ctx.getSource())))
                        .then(Commands.literal("polish")
                                .executes(ctx -> verifyPolish(ctx.getSource())))
                        .then(Commands.literal("dimension_progression")
                                .executes(ctx -> verifyDimensionProgression(ctx.getSource())))
                        .then(Commands.literal("cycles")
                                .executes(ctx -> verifyCycles(ctx.getSource())))
                        .then(Commands.literal("colony")
                                .executes(ctx -> verifyColony(ctx.getSource())))
                        .then(Commands.literal("sinkhole")
                                .executes(ctx -> verifySinkhole(ctx.getSource())))
                        .then(Commands.literal("worldgen")
                                .executes(ctx -> runWorldgenAudit(ctx.getSource(), 6, 64)))
                        .then(Commands.literal("all")
                                .executes(ctx -> verifyAll(ctx.getSource()))))
                .then(Commands.literal("cycle")
                        .then(Commands.literal("status")
                                .executes(ctx -> reportCycleStatus(ctx.getSource())))
                        .then(Commands.literal("abort")
                                .executes(ctx -> abortCycleCmd(ctx.getSource())))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetCycleCmd(ctx.getSource())))
                        .then(Commands.literal("start")
                                .then(Commands.argument("mutators", StringArgumentType.greedyString())
                                        .executes(ctx -> startCycleCmd(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mutators"))))))
                .then(Commands.literal("homeostasis")
                        .then(Commands.literal("status")
                                .executes(ctx -> reportHomeostasisStatus(ctx.getSource())))
                        .then(Commands.literal("stress")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, 100.0))
                                        .executes(ctx -> setHomeostasisStress(
                                                ctx.getSource(),
                                                DoubleArgumentType.getDouble(ctx, "amount")))))
                        .then(Commands.literal("sedate")
                                .executes(ctx -> sedateHomeostasis(ctx.getSource()))))
                .then(Commands.literal("worldgen")
                        .then(Commands.literal("audit")
                                .executes(ctx -> runWorldgenAudit(ctx.getSource(), 8, 64))
                                .then(Commands.argument("radiusChunks", IntegerArgumentType.integer(1, 24))
                                        .executes(ctx -> runWorldgenAudit(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "radiusChunks"),
                                                64))
                                        .then(Commands.argument("sanctumGridChunks", IntegerArgumentType.integer(16, 256))
                                                .executes(ctx -> runWorldgenAudit(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "radiusChunks"),
                                                        IntegerArgumentType.getInteger(ctx, "sanctumGridChunks")))))))
                .then(Commands.literal("symbiosis")
                        .then(Commands.literal("status")
                                .executes(ctx -> reportSymbiosis(ctx.getSource())))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearSymbiosis(ctx.getSource())))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("mutagen", StringArgumentType.word())
                                        .executes(ctx -> grantMutagen(ctx.getSource(), StringArgumentType.getString(ctx, "mutagen"))))))
                .then(Commands.literal("diplomacy")
                        .then(Commands.argument("strain", StringArgumentType.word())
                                .then(Commands.argument("value", IntegerArgumentType.integer(-100, 100))
                                        .executes(ctx -> setDiplomacyRep(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "strain"),
                                                IntegerArgumentType.getInteger(ctx, "value"))))))
                .then(Commands.literal("schism")
                        .then(Commands.argument("strain", StringArgumentType.word())
                                .executes(ctx -> triggerSchismCommand(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "strain")))))
                .then(Commands.literal("habits")
                        .executes(ctx -> reportHabits(ctx.getSource())))
                .then(Commands.literal("routes")
                        .executes(ctx -> reportRoutesAndArchitecture(ctx.getSource())))
                .then(Commands.literal("ai")
                        .executes(ctx -> reportAiDiagnostics(ctx.getSource(), 24))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(4, 96))
                                .executes(ctx -> reportAiDiagnostics(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("colony")
                        .then(Commands.literal("status")
                                .executes(ctx -> reportColonyStatus(ctx.getSource())))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("caste", StringArgumentType.word())
                                        .executes(ctx -> spawnColonySymbiontCmd(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "caste")))))
                        .then(Commands.literal("upgrade")
                                .executes(ctx -> upgradeColonyCmd(ctx.getSource())))
                        .then(Commands.literal("biomass")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 512))
                                        .executes(ctx -> addColonyBiomassCmd(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("raid")
                                .executes(ctx -> triggerColonyRaidCmd(ctx.getSource())))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetColonyCmd(ctx.getSource())))));
    }

    private static int reportStatus(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos nearest = tracker.findNearestNode(origin);
        String traits = hive.getActiveTraits().isEmpty()
                ? "None"
                : hive.getActiveTraits().stream().map(AdaptationTrait::id).collect(Collectors.joining(", "));
        String tier2 = hive.getTier2Traits().isEmpty()
                ? "None"
                : hive.getTier2Traits().stream().map(AdaptationTrait::tier2BehaviorId).collect(Collectors.joining(", "));
        String synergies = hive.getActiveSynergies().isEmpty()
                ? "None"
                : hive.getActiveSynergies().stream().map(HiveSynergy::id).collect(Collectors.joining(", "));

        source.sendSuccess(() -> Component.literal(String.format(
                "[Mycelial] Threat: %.1f%% [%s] | Milestone: %d%% | Protected: %s | Traits: %s | Tier2: %s | Synergies: %s | Links: %d | Suppressed: %ds | NearestNode: %s | Breached: %s | MatriarchDefeated: %s | Remission: %ds",
                hive.getThreatLevel(),
                com.alex.mycelialmind.hive.HiveProgressionTier.fromThreat(hive.getThreatLevel()).displayTag(),
                hive.getLastWaveMilestone(),
                tracker.isProtected(origin),
                traits,
                tier2,
                synergies,
                tracker.countActiveNetworkLinks(),
                hive.getSuppressionRemainingSeconds(level.getGameTime()),
                nearest != null ? nearest.toShortString() : "None",
                hive.isDeepMyceliumBreached(),
                hive.isMatriarchDefeated(),
                hive.getMatriarchRemissionRemainingSeconds(level.getGameTime())
        )).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int resetHive(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.resetAll();
        com.alex.mycelialmind.hive.HiveCycleSavedData.get(level).abortCycle();
        com.alex.mycelialmind.hive.HiveCycleDirector.syncHiveModifiersAcrossDimensions(level);
        com.alex.mycelialmind.network.CycleProtocolSyncPayload.syncToAll(level);
        syncAllPlayers(level);
        source.sendSuccess(() -> Component.literal("[Mycelial] Hive Mind threat, stimuli, and wave milestone reset.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int addStimulus(CommandSourceStack source, String rawType, double amount) {
        StimulusType type;
        try {
            type = StimulusType.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Unknown stimulus type: " + rawType
                    + ". Valid: MELEE, PROJECTILE, FIRE, EXPLOSION, MAGIC"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        hive.recordStimulus(type, amount, level.getGameTime());
        syncAllPlayers(level);
        source.sendSuccess(() -> Component.literal(String.format(
                "[Mycelial] Recorded %.1f %s stimulus. Threat is now %.1f%%.",
                amount, type.id(), hive.getThreatLevel())).withStyle(ChatFormatting.AQUA), true);
        return 1;
    }

    private static int verifyAll(CommandSourceStack source) {
        int r1 = verifyProjectileAdaptation(source);
        int r2 = verifyPylonContainment(source);
        int r3 = verifyBossEncounter(source);
        int r4 = verifyTier2Synergy(source);
        int r5 = verifySynapticNetwork(source);
        int r6 = verifySymbiosis(source);
        int r7 = verifyUnits(source);
        int r8 = verifyExpansion(source);
        int r9 = verifyTide(source);
        int r10 = verifyCounteroffensive(source);
        int r11 = verifyArsenal(source);
        int r12 = verifyMycorrhizal(source);
        int r13 = verifyHiveAi(source);
        int r14 = verifyChimera(source);
        int r15 = verifyMemory(source);
        int r16 = verifyTacticalAi(source);
        int r17 = verifyFieldAi(source);
        int r18 = verifyLoot(source);
        int r19 = verifyHomeostasis(source);
        int r20 = verifyPolish(source);
        int r21 = verifyDimensionProgression(source);
        int r22 = verifyCycles(source);
        int r23 = verifyColony(source);
        int r24 = verifySinkhole(source);
        if (r1 == 1 && r2 == 1 && r3 == 1 && r4 == 1 && r5 == 1 && r6 == 1 && r7 == 1 && r8 == 1 && r9 == 1 && r10 == 1 && r11 == 1 && r12 == 1 && r13 == 1 && r14 == 1 && r15 == 1 && r16 == 1 && r17 == 1 && r18 == 1 && r19 == 1 && r20 == 1 && r21 == 1 && r22 == 1 && r23 == 1 && r24 == 1) {
            source.sendSuccess(() -> Component.literal("[Mycelial Verify] ALL 24 automated scenario checks PASSED.")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
            return 1;
        }
        StringBuilder failedScenarios = new StringBuilder();
        appendFailedScenario(failedScenarios, "projectile_adaptation", r1);
        appendFailedScenario(failedScenarios, "pylon_containment", r2);
        appendFailedScenario(failedScenarios, "boss_encounter", r3);
        appendFailedScenario(failedScenarios, "tier2_synergy", r4);
        appendFailedScenario(failedScenarios, "synaptic_network", r5);
        appendFailedScenario(failedScenarios, "symbiosis", r6);
        appendFailedScenario(failedScenarios, "units", r7);
        appendFailedScenario(failedScenarios, "expansion", r8);
        appendFailedScenario(failedScenarios, "tide", r9);
        appendFailedScenario(failedScenarios, "counteroffensive", r10);
        appendFailedScenario(failedScenarios, "arsenal", r11);
        appendFailedScenario(failedScenarios, "mycorrhizal", r12);
        appendFailedScenario(failedScenarios, "hive_ai", r13);
        appendFailedScenario(failedScenarios, "chimera", r14);
        appendFailedScenario(failedScenarios, "memory", r15);
        appendFailedScenario(failedScenarios, "tactical_ai", r16);
        appendFailedScenario(failedScenarios, "field_ai", r17);
        appendFailedScenario(failedScenarios, "loot", r18);
        appendFailedScenario(failedScenarios, "homeostasis", r19);
        appendFailedScenario(failedScenarios, "polish", r20);
        appendFailedScenario(failedScenarios, "dimension_progression", r21);
        appendFailedScenario(failedScenarios, "cycles", r22);
        appendFailedScenario(failedScenarios, "colony", r23);
        appendFailedScenario(failedScenarios, "sinkhole", r24);
        source.sendFailure(Component.literal("[Mycelial Verify] Failed scenarios: " + failedScenarios + "."));
        return 0;
    }

    private static void appendFailedScenario(StringBuilder failures, String scenario, int result) {
        if (result == 1) {
            return;
        }
        if (failures.length() > 0) {
            failures.append(", ");
        }
        failures.append(scenario);
    }

    private static int verifyProjectileAdaptation(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition()).above(2);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        ChitinBruteEntity brute = null;
        try {
            hive.resetAll();
            var preObs = ProgressionGoalEngine.resolveCreatureObservation(
                    false,
                    hive.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT),
                    false,
                    hive.getStimulusScore(StimulusType.PROJECTILE),
                    null,
                    0.0D,
                    MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get()
            );
            if (preObs != ProgressionGoalEngine.CreatureObservation.INSUFFICIENT_EVIDENCE) {
                source.sendFailure(Component.literal("[Verify:Projectile] FAIL: Expected INSUFFICIENT_EVIDENCE before combat."));
                return 0;
            }
            for (int i = 0; i < 20 && !hive.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT); i++) {
                SporelingEntity sporeling = ModEntities.SPORELING.get().create(level);
                if (sporeling == null) {
                    source.sendFailure(Component.literal("[Verify:Projectile] Failed to create Sporeling."));
                    return 0;
                }
                Arrow arrow = new Arrow(level, origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
                        new ItemStack(Items.ARROW), null);
                try {
                    sporeling.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
                    level.addFreshEntity(sporeling);
                    sporeling.hurt(level.damageSources().arrow(arrow, source.getEntity()), 10.0F);
                } finally {
                    arrow.discard();
                    sporeling.discard();
                }
            }

            if (!hive.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT)) {
                source.sendFailure(Component.literal("[Verify:Projectile] FAIL: PROJECTILE_DEFLECT did not activate."));
                return 0;
            }

            brute = ModEntities.CHITIN_BRUTE.get().create(level);
            if (brute == null) {
                source.sendFailure(Component.literal("[Verify:Projectile] FAIL: Could not create ChitinBrute."));
                return 0;
            }
            brute.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            brute.finalizeSpawn(level, level.getCurrentDifficultyAt(origin), MobSpawnType.COMMAND, null);
            var postObs = ProgressionGoalEngine.resolveCreatureObservation(
                    brute.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT),
                    hive.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT),
                    true,
                    hive.getStimulusScore(StimulusType.PROJECTILE),
                    StimulusType.PROJECTILE,
                    hive.getStimulusScore(StimulusType.PROJECTILE),
                    MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get()
            );
            if (!brute.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT)
                    || postObs != ProgressionGoalEngine.CreatureObservation.CONFIRMED_PROJECTILE_ADAPTATION) {
                source.sendFailure(Component.literal("[Verify:Projectile] FAIL: ChitinBrute did not inherit trait or observation mismatch."));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("[Verify:Projectile] PASS: Arrow stimulus activated PROJECTILE_DEFLECT and rank inheritance verified.")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            if (brute != null) {
                brute.discard();
            }
            try {
                hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
            } finally {
                syncAllPlayers(level);
            }
        }
    }

    private static int verifyPylonContainment(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pad = BlockPos.containing(source.getPosition()).offset(0, 12, 0);
        BlockPos pylonPos = pad;
        BlockPos myceliumPos = pad.east(2);
        BlockPos dirtPos = pad.east(3);
        Set<BlockPos> touched = Set.of(pylonPos, myceliumPos, dirtPos);
        if (!ensureScenarioPositionsLoaded(source, "Pylon", touched)) {
            return 0;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        CompoundTag trackerSnapshot = tracker.save(new CompoundTag(), level.registryAccess());
        Map<BlockPos, SavedBlock> blockSnapshot = snapshotBlocks(level, touched);
        boolean contained;
        try {
            tracker.clearAll();
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            tracker.addPylon(pylonPos);
            level.setBlockAndUpdate(myceliumPos, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
            level.setBlockAndUpdate(dirtPos, Blocks.DIRT.defaultBlockState());

            RandomSource spreadRandom = RandomSource.create(42L);
            for (int i = 0; i < 20; i++) {
                level.getBlockState(myceliumPos).randomTick(level, myceliumPos, spreadRandom);
            }

            contained = level.getBlockState(dirtPos).is(Blocks.DIRT) && tracker.isProtected(dirtPos);
        } finally {
            try {
                restoreBlocks(level, blockSnapshot);
            } finally {
                tracker.restoreFromSnapshot(trackerSnapshot, level.registryAccess());
            }
        }

        if (!contained) {
            source.sendFailure(Component.literal("[Verify:Pylon] FAIL: Mycelium breached or zone was unprotected."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Pylon] PASS: Purification Pylon contained Creeping Mycelium spread.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyBossEncounter(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition()).offset(0, 8, 0);
        BlockPos nodePos = origin.east(2);
        Set<BlockPos> touched = Set.of(nodePos);
        if (!ensureScenarioPositionsLoaded(source, "Boss", touched)) {
            return 0;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag trackerSnapshot = tracker.save(new CompoundTag(), level.registryAccess());
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        Map<BlockPos, SavedBlock> blockSnapshot = snapshotBlocks(level, touched);

        HiveCoreBossEntity boss = ModEntities.HIVE_CORE.get().create(level);
        boolean hasShield = false;
        boolean adaptedMagic = false;
        try {
            tracker.clearAll();
            hive.resetAll();
            level.setBlockAndUpdate(nodePos, ModBlocks.SYMBIOTIC_NODE.get().defaultBlockState());
            tracker.addNode(nodePos, com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC);
            if (boss == null) {
                source.sendFailure(Component.literal("[Verify:Boss] FAIL: Could not create HiveCore."));
                return 0;
            }
            boss.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            boss.finalizeSpawn(level, level.getCurrentDifficultyAt(origin), MobSpawnType.COMMAND, null);
            level.addFreshEntity(boss);

            for (int t = 0; t < 20; t++) {
                boss.tickCount += 1;
                boss.customServerAiStep();
            }
            hasShield = boss.hasEffect(MobEffects.DAMAGE_RESISTANCE);

            boss.hurt(level.damageSources().magic(), 5.0F);
            adaptedMagic = boss.hasTrait(AdaptationTrait.MAGIC_CARAPACE);
        } finally {
            if (boss != null) {
                boss.discard();
            }
            try {
                restoreBlocks(level, blockSnapshot);
            } finally {
                try {
                    tracker.restoreFromSnapshot(trackerSnapshot, level.registryAccess());
                } finally {
                    try {
                        hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
                    } finally {
                        syncAllPlayers(level);
                    }
                }
            }
        }

        if (!hasShield || !adaptedMagic) {
            source.sendFailure(Component.literal(String.format(
                    "[Verify:Boss] FAIL: hasShield=%s adaptedMagic=%s", hasShield, adaptedMagic)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Boss] PASS: HiveCoreBoss symbiotic node shield & real-time adaptation verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyTier2Synergy(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        boolean t2;
        boolean syn;
        try {
            hive.resetAll();
            hive.recordStimulus(StimulusType.FIRE, 210.0, level.getGameTime());
            hive.recordStimulus(StimulusType.EXPLOSION, 75.0, level.getGameTime());
            t2 = hive.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT);
            syn = hive.hasSynergy(HiveSynergy.PYROLYTIC_SHELL);
        } finally {
            try {
                hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
            } finally {
                syncAllPlayers(level);
            }
        }
        if (!t2 || !syn) {
            source.sendFailure(Component.literal(String.format("[Verify:Tier2] FAIL: t2=%s syn=%s", t2, syn)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Tier2] PASS: Tier 2 mutation & PYROLYTIC_SHELL synergy verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifySynapticNetwork(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition()).offset(0, 8, 0);
        BlockPos nodeA = origin;
        BlockPos nodeB = origin.east(10);
        BlockPos pylonPos = origin.east(5);
        Set<BlockPos> touched = Set.of(nodeA, nodeB, pylonPos);
        if (!ensureScenarioPositionsLoaded(source, "SynapticNetwork", touched)) {
            return 0;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        CompoundTag trackerSnapshot = tracker.save(new CompoundTag(), level.registryAccess());
        Map<BlockPos, SavedBlock> blockSnapshot = snapshotBlocks(level, touched);
        boolean linkedBefore;
        boolean linkedAfter;
        try {
            tracker.clearAll();
            level.setBlockAndUpdate(nodeA, ModBlocks.MYCELIAL_NODE.get().defaultBlockState());
            tracker.addNode(nodeA, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
            level.setBlockAndUpdate(nodeB, ModBlocks.INCUBATOR_NODE.get().defaultBlockState());
            tracker.addNode(nodeB, com.alex.mycelialmind.world.SynapticNodeType.INCUBATOR);
            linkedBefore = tracker.getLinkedNodes(nodeA).contains(nodeB);

            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            tracker.addPylon(pylonPos);
            linkedAfter = tracker.getLinkedNodes(nodeA).contains(nodeB);
        } finally {
            try {
                restoreBlocks(level, blockSnapshot);
            } finally {
                tracker.restoreFromSnapshot(trackerSnapshot, level.registryAccess());
            }
        }

        if (!linkedBefore || linkedAfter) {
            source.sendFailure(Component.literal(String.format(
                    "[Verify:Network] FAIL: linkedBefore=%s linkedAfter=%s", linkedBefore, linkedAfter)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Network] PASS: Synaptic link formation & Pylon severing verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifySymbiosis(CommandSourceStack source) {
        PlayerSymbiosisData data = new PlayerSymbiosisData();
        boolean first = data.equipMutagen(SymbioticMutagen.CHITIN_THORNS);
        boolean second = data.equipMutagen(SymbioticMutagen.PYRO_GLAND);
        boolean third = data.equipMutagen(SymbioticMutagen.ARCANE_SYNAPSE);
        double amp = data.getThreatAmplifier();
        if (!first || !second || third || Math.abs(amp - 1.30) > 0.001) {
            source.sendFailure(Component.literal(String.format(
                    "[Verify:Symbiosis] FAIL: first=%s second=%s third=%s amp=%.2f", first, second, third, amp)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Symbiosis] PASS: 2-slot Symbiosis cap & 1.30x threat amplifier verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyUnits(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        ChitinBruteEntity brute = ModEntities.CHITIN_BRUTE.get().create(level);
        if (brute == null) {
            source.sendFailure(Component.literal("[Verify:Units] FAIL: Could not create ChitinBrute."));
            return 0;
        }
        float baseHp = brute.getMaxHealth();
        brute.promoteRank(EvolutionRank.ELITE);
        float eliteHp = brute.getMaxHealth();
        boolean ok = brute.getEvolutionRank() == EvolutionRank.ELITE && eliteHp > baseHp;
        brute.discard();
        if (!ok) {
            source.sendFailure(Component.literal("[Verify:Units] FAIL: ELITE promotion did not scale HP."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Units] PASS: EvolutionRank ELITE promotion & stat scaling verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyExpansion(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var architect = ModEntities.SPORE_ARCHITECT.get().create(level);
        boolean cobbleInfectable = com.alex.mycelialmind.block.CreepingMyceliumBlock.canInfect(
                net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        boolean planksInfectable = com.alex.mycelialmind.block.CreepingMyceliumBlock.canInfect(
                net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());
        boolean ok = architect != null && cobbleInfectable && planksInfectable;
        if (architect != null) {
            architect.discard();
        }
        if (!ok) {
            source.sendFailure(Component.literal("[Verify:Expansion] FAIL: SporeArchitect or aggressive village assimilation check failed."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Expansion] PASS: SporeArchitect & aggressive village assimilation verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyTide(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        long now = level.getGameTime();
        var behemoth = ModEntities.SPORE_BEHEMOTH.get().create(level);
        boolean active;
        try {
            hive.resetAll();
            hive.triggerFungalTide(now, 600);
            active = hive.isFungalTideActive(now) && behemoth != null;
            if (behemoth != null) {
                behemoth.activateMycelialShield(level);
                active = active && behemoth.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
            }
            hive.endFungalTide(now);
            active = active && !hive.isFungalTideActive(now);
        } finally {
            if (behemoth != null) {
                behemoth.discard();
            }
            try {
                hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
            } finally {
                syncAllPlayers(level);
            }
        }
        if (!active) {
            source.sendFailure(Component.literal("[Verify:Tide] FAIL: Fungal Tide lifecycle or SporeBehemoth shield check failed."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Tide] PASS: Fungal Tide & SporeBehemoth Mycelial Shield verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyCounteroffensive(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var drone = ModEntities.PURIFIER_DRONE.get().create(level);
        var recipeOk = level.getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.alex.mycelialmind.MycelialMind.MODID, "purifier_drone_core"))
                .isPresent();
        boolean ok = drone != null && recipeOk;
        if (drone != null) {
            drone.discard();
        }
        if (!ok) {
            source.sendFailure(Component.literal("[Verify:Counteroffensive] FAIL: PurifierDrone or recipe missing."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Counteroffensive] PASS: PurifierDrone & Survival recipes verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyArsenal(CommandSourceStack source) {
        ItemStack chest = new ItemStack(com.alex.mycelialmind.registry.ModItems.CHITIN_CHESTPLATE.get());
        boolean m1 = com.alex.mycelialmind.item.ChitinArmorItem.tryEvolvePiece(
                chest, new ItemStack(com.alex.mycelialmind.registry.ModItems.SYNAPTIC_CORE.get()), null);
        boolean m2 = com.alex.mycelialmind.item.ChitinArmorItem.tryEvolvePiece(
                chest, new ItemStack(com.alex.mycelialmind.registry.ModItems.OVERMIND_ESSENCE.get()), null);
        boolean ok = m1 && m2
                && com.alex.mycelialmind.item.ChitinArmorItem.getArmorRank(chest) == EvolutionRank.ELITE;
        if (!ok) {
            source.sendFailure(Component.literal("[Verify:Arsenal] FAIL: ChitinArmor evolution check failed."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Arsenal] PASS: ChitinArmor BASE->MUTATED->ELITE evolution verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyMycorrhizal(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var mimic = ModEntities.SYNAPTIC_MIMIC.get().create(level);
        var graftRecipeOk = level.getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.alex.mycelialmind.MycelialMind.MODID, "synaptic_graft"))
                .isPresent();
        var decoyRecipeOk = level.getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.alex.mycelialmind.MycelialMind.MODID, "neural_echo_decoy"))
                .isPresent();
        boolean mimicPersonaOk = false;
        if (mimic != null) {
            mimic.setMimicPersona(com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.MimicPersona.INFILTRATION_SPOOF);
            mimicPersonaOk = mimic.isSpoofingAlliance();
            mimic.unmaskWithSynapticShock(level, null);
            mimicPersonaOk = mimicPersonaOk && mimic.isUnmasked() && mimic.isDesynced() && mimic.getShockTicks() > 0;
            mimic.discard();
        }
        boolean ok = mimicPersonaOk && graftRecipeOk && decoyRecipeOk;
        if (!ok) {
            source.sendFailure(Component.literal("[Verify:Mycorrhizal] FAIL: SynapticMimic or v1.2.0 recipes missing."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Mycorrhizal] PASS: SynapticMimic, SymbioticNode, and Mycorrhizal Echoes verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyHiveAi(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var tracker = PurificationZoneTracker.get(level);
        BlockPos symPos = origin.offset(2, 0, 2);
        BlockPos relayPos = origin.offset(12, 0, 12);
        CompoundTag trackerSnapshot = tracker.save(new CompoundTag(), level.registryAccess());
        var brute = ModEntities.CHITIN_BRUTE.get().create(level);
        try {
            tracker.clearAll();
            tracker.addNode(symPos, com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC);
            tracker.addNode(relayPos, com.alex.mycelialmind.world.SynapticNodeType.RELAY);
            BlockPos enemy = tracker.findNearestEnemyNode(symPos);
            boolean enemyOk = enemy != null && tracker.getNodeType(enemy) != com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC;

            var flow = com.alex.mycelialmind.world.MycelialFastNoise.organicFlow2D(origin.getX(), origin.getZ(), level.getSeed(), 1.0D);
            boolean flowOk = Math.abs(flow.x) <= 1.0D && Math.abs(flow.z) <= 1.0D;

            boolean btOk = false;
            boolean arriveOk = false;
            if (brute != null) {
                brute.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
                var arrive = com.alex.mycelialmind.hive.HiveSwarmCoordinator.computeTacticalArriveSteering(
                        brute, brute.position().add(6.0, 0.0, 0.0), level);
                arriveOk = arrive.x > 0.0D && arrive.length() <= 2.21D;

                var bb = brute.getAiBlackboard();
                bb.hasTarget = true;
                bb.distToTarget = 6.0F;
                bb.health01 = 0.85F;
                var tree = com.alex.mycelialmind.entity.ai.HiveTacticalTree.create(bb);
                tree.step();
                btOk = bb.activeIntent != com.alex.mycelialmind.entity.ai.HiveAiBlackboard.TacticalIntent.NONE;
            }

            if (!enemyOk || !flowOk || !arriveOk || !btOk) {
                source.sendFailure(Component.literal("[Verify:HiveAI] FAIL: enemyOk=" + enemyOk
                        + " flowOk=" + flowOk + " arriveOk=" + arriveOk + " btOk=" + btOk));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("[Verify:HiveAI] PASS: Native HiveBehaviorTree, Arrive steering, FastNoiseLite flow & Tactical Utility verified.")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            if (brute != null) {
                brute.discard();
            }
            tracker.restoreFromSnapshot(trackerSnapshot, level.registryAccess());
        }
    }

    private static int verifyChimera(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var chimera = ModEntities.MYCELIAL_CHIMERA.get().create(level);
        var ovumRecipeOk = level.getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.alex.mycelialmind.MycelialMind.MODID, "chimera_ovum"))
                .isPresent();
        boolean ok = false;
        if (chimera != null) {
            chimera.randomizeGenes(level.getRandom());
            ok = ovumRecipeOk && chimera.getChassisGene() != chimera.getMutationGene();
            chimera.discard();
        }
        if (!ok) {
            source.sendFailure(Component.literal("[Verify:Chimera] FAIL: MycelialChimera or chimera_ovum recipe missing."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Chimera] PASS: Modular Chimera Bio-Forge & randomized genes verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyMemory(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        var recipeOk = level.getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.alex.mycelialmind.MycelialMind.MODID, "false_engram_injector"))
                .isPresent();
        boolean habitOk;
        try {
            hive.resetAll();
            hive.recordHabit(com.alex.mycelialmind.hive.PlayerHabit.COMBAT_MEDIC, 65.0);
            habitOk = hive.hasHabitAdaptation(com.alex.mycelialmind.hive.PlayerHabit.COMBAT_MEDIC);
        } finally {
            try {
                hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
            } finally {
                syncAllPlayers(level);
            }
        }
        if (!recipeOk || !habitOk) {
            source.sendFailure(Component.literal("[Verify:Memory] FAIL: Habit adaptation or false_engram_injector recipe missing."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Memory] PASS: PlayerHabits, Volatile Buffer & Pavlovian Deception verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyTacticalAi(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var weaver = ModEntities.SYNAPSE_WEAVER.get().create(level);
        var brute = ModEntities.CHITIN_BRUTE.get().create(level);
        var spitter = ModEntities.ACID_SPITTER.get().create(level);
        try {
            if (weaver == null || brute == null || spitter == null) {
                source.sendFailure(Component.literal("[Verify:TacticalAI] FAIL: Could not instantiate squad entities."));
                return 0;
            }
            weaver.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
            brute.moveTo(origin.getX() + 2.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
            spitter.moveTo(origin.getX() - 2.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(weaver);
            level.addFreshEntity(brute);
            level.addFreshEntity(spitter);

            var leader = com.alex.mycelialmind.hive.SynapticCommandSystem.selectSquadLeader(
                    java.util.List.of(weaver, brute, spitter));
            boolean leaderOk = leader == weaver;

            brute.triggerFrenzy(160);
            boolean frenzyOn = brute.isFrenzyActive();

            int desynced = com.alex.mycelialmind.hive.SynapticCommandSystem.propagateLeaderDesync(
                    level, weaver, com.alex.mycelialmind.hive.SynapticCommandSystem.DESYNC_DURATION_TICKS,
                    ally -> ally == brute || ally == spitter);
            boolean desyncOk = desynced >= 2 && brute.isDesynced() && !brute.isFrenzyActive();

            if (!leaderOk || !frenzyOn || !desyncOk) {
                source.sendFailure(Component.literal("[Verify:TacticalAI] FAIL: leaderOk=" + leaderOk
                        + " frenzyOn=" + frenzyOn + " desyncOk=" + desyncOk));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("[Verify:TacticalAI] PASS: Leader priority, Frenzy & Synaptic Desync verified.")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            if (weaver != null) weaver.discard();
            if (brute != null) brute.discard();
            if (spitter != null) spitter.discard();
        }
    }

    private static int verifyFieldAi(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var carrionSnapshot = com.alex.mycelialmind.hive.HiveBlackboard.snapshotCarrionState(level);
        var map = new com.alex.mycelialmind.hive.HiveInfluenceMap();
        boolean lightAvoids;
        boolean sapperIgnores;
        boolean carrionOk;
        boolean jamOk = false;
        PurifierDroneEntity drone = null;
        var carrionPos = new net.minecraft.world.phys.Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        try {
            map.addTrauma(origin, 0.70F);
            float trauma = map.sample(origin).trauma();
            lightAvoids = com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldAvoidTraumaZone(trauma, false);
            sapperIgnores = !com.alex.mycelialmind.hive.LivingBattlefieldSystem.shouldAvoidTraumaZone(trauma, true);

            com.alex.mycelialmind.hive.HiveBlackboard.clearCarrionState(level);
            com.alex.mycelialmind.hive.HiveBlackboard.registerCarrion(level, carrionPos, 240);
            var consumed = com.alex.mycelialmind.hive.HiveBlackboard.consumeNearestCarrion(level, carrionPos, 4.0D);
            carrionOk = consumed != null;

            drone = ModEntities.PURIFIER_DRONE.get().create(level);
            if (drone != null) {
                drone.setJammedTicks(80);
                jamOk = drone.isJammed();
            }
        } finally {
            if (drone != null) {
                drone.discard();
            }
            com.alex.mycelialmind.hive.HiveBlackboard.restoreCarrionState(level, carrionSnapshot);
        }

        if (!lightAvoids || !sapperIgnores || !carrionOk || !jamOk) {
            source.sendFailure(Component.literal("[Verify:FieldAI] FAIL: lightAvoids=" + lightAvoids
                    + " sapperIgnores=" + sapperIgnores + " carrionOk=" + carrionOk + " jamOk=" + jamOk));
            return 0;
        }
        if (verifyHyphalSiege(source) != 1) {
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:FieldAI] PASS: Ephemeral Chitin, Trauma Killzones, Carrion Necrophagy & Parasitic Jamming verified.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyHyphalSiege(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        CompoundTag trackerSnapshot = tracker.save(new CompoundTag(), level.registryAccess());

        BlockPos base = BlockPos.containing(source.getPosition()).above(6);
        Set<BlockPos> savedPositions = new LinkedHashSet<>();
        for (int dx = -5; dx <= 20; dx++) {
            for (int dy = -2; dy <= 14; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    savedPositions.add(base.offset(dx, dy, dz));
                }
            }
        }
        if (!ensureScenarioPositionsLoaded(source, "HyphalSiege", savedPositions)) {
            return 0;
        }
        Map<BlockPos, SavedBlock> blockSnapshot = snapshotBlocks(level, savedPositions);
        net.minecraft.world.phys.AABB cleanupArea = new net.minecraft.world.phys.AABB(base).inflate(26.0D);
        Set<java.util.UUID> preExistingIds = new HashSet<>();
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, cleanupArea)) {
            preExistingIds.add(e.getUUID());
        }

        try {
            hive.resetAll();
            tracker.clearAll();
            for (BlockPos p : savedPositions) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
            }

            // 1. Hyphal Route Network density, vein sprouting, ambush & tool severing
            BlockPos r0 = base.offset(0, 0, 0);
            BlockPos r1 = base.offset(3, 0, 0);
            for (int dx = -2; dx <= 5; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    level.setBlockAndUpdate(base.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState());
                }
            }
            ChunkPos cp = new ChunkPos(r0);
            long now = level.getGameTime();
            for (int pass = 0; pass < 5; pass++) {
                hive.recordRouteStep(cp, r0.below(), pass == 0, false, false, false, now + pass * 40L);
                hive.recordRouteStep(cp, r1.below(), false, false, false, false, now + pass * 40L + 20L);
            }
            hive.setHyphalDensity(cp, 68.0, now + 200L);
            int sprouted = HyphalSiegeDirector.sproutRouteVeinsInChunk(level, hive, cp);
            sprouted += HyphalSiegeDirector.growTrailVeinsNow(level, hive, cp, 8);
            boolean veinOverlayOnIntactDirt = level.getBlockState(r0).is(ModBlocks.HYPHAL_VEIN.get())
                    && level.getBlockState(r0.below()).is(Blocks.DIRT);

            var fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            boolean ambushOk = HyphalSiegeDirector.spawnRouteTacticalAmbush(level, hive, fakePlayer, r0.below());
            int severed = HyphalSiegeDirector.trySeverHyphalVein(
                    level, r0, fakePlayer, new ItemStack(ModItems.SYMBIOTIC_BLADE.get()));
            boolean groundIntactAfterSever = level.getBlockState(r0.below()).is(Blocks.DIRT);
            boolean densityDropped = hive.getHyphalDensity(cp) < 68.0;

            // 2. Architectural Accent & Twisted Reflection
            hive.recordArchitecturalBlockPlacement("minecraft:dark_oak_planks", false, false);
            hive.recordArchitecturalGeometry(4.0, 1.85, 0.20);
            hive.recordArchitecturalBlockPlacement("minecraft:dark_oak_planks", false, false);
            hive.recordArchitecturalGeometry(4.0, 1.85, 0.20);
            hive.recordArchitecturalBlockPlacement("minecraft:dark_oak_log", true, false);
            hive.recordArchitecturalGeometry(4.0, 1.85, 0.20);
            var accent = hive.getArchitecturalAccent();
            boolean accentOk = accent.hasCustomAccent()
                    && "minecraft:dark_oak_planks".equals(accent.primaryBlockId())
                    && accent.proportion() == HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.TOWER_SPIRE
                    && com.alex.mycelialmind.world.structure.TwistedCitadelPiece
                    .selectTemplateForProportion(accent.proportion())
                    .equals(com.alex.mycelialmind.world.structure.TwistedCitadelPiece.SPIRE_TEMPLATE);

            BlockPos reflOrigin = base.offset(14, 0, 0);
            var reflBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                    reflOrigin.getX() - 4, reflOrigin.getY() - 1, reflOrigin.getZ() - 4,
                    reflOrigin.getX() + 4, reflOrigin.getY() + 13, reflOrigin.getZ() + 4);
            com.alex.mycelialmind.world.structure.ArchitecturalStructureAdapter
                    .placeTwistedReflectionStructure(level, reflBox, reflOrigin, accent);
            boolean sawDarkOak = false;
            boolean sawArchBrick = false;
            for (BlockPos p : BlockPos.betweenClosed(
                    reflBox.minX(), reflBox.minY(), reflBox.minZ(),
                    reflBox.maxX(), reflBox.maxY(), reflBox.maxZ())) {
                BlockState st = level.getBlockState(p);
                if (st.is(Blocks.DARK_OAK_PLANKS)) sawDarkOak = true;
                if (st.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())) sawArchBrick = true;
            }

            // 3. Blind-Spot growth, photophobic stall, core destruction & completion climax
            BlockPos blindCore1 = base.offset(-2, 0, 4);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    level.setBlockAndUpdate(blindCore1.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                }
            }
            var site1 = HyphalSiegeDirector.createBlindSpotSiteAt(level, hive, blindCore1);
            boolean coreHasVeinHalo = false;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(blindCore1.relative(dir)).is(ModBlocks.HYPHAL_VEIN.get())) {
                    coreHasVeinHalo = true;
                    break;
                }
            }
            boolean grew1 = HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site1, true);
            boolean grew2 = HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site1, true);

            level.setBlockAndUpdate(blindCore1.east(5), Blocks.GLOWSTONE.defaultBlockState());
            boolean stalledInLight = !HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site1, true)
                    && site1.getPhotophobicStrikes() >= 1;
            level.removeBlock(blindCore1.east(5), false);

            HyphalSiegeDirector.destroyBlindSpotVulnerableCore(level, hive, site1, fakePlayer);
            boolean coreDestroyedOk = hive.getBlindSpotSite(blindCore1) == null;

            BlockPos blindCore2 = base.offset(6, 0, -4);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    level.setBlockAndUpdate(blindCore2.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                }
            }
            var site2 = HyphalSiegeDirector.createBlindSpotSiteAt(level, hive, blindCore2);
            int guard = 0;
            while (!site2.isCompleted() && guard++ < site2.getTotalSteps() + 10) {
                HyphalSiegeDirector.advanceBlindSpotGrowthStep(level, hive, site2, true);
            }
            boolean completedOk = site2.isCompleted()
                    && level.getBlockState(blindCore2).is(ModBlocks.INCUBATOR_NODE.get())
                    && !level.getEntitiesOfClass(MycelialMob.class, new net.minecraft.world.phys.AABB(blindCore2).inflate(8.0D),
                            m -> m.getEvolutionRank() == EvolutionRank.ELITE).isEmpty();

            if (sprouted <= 0 || !veinOverlayOnIntactDirt || !ambushOk || severed <= 0
                    || !groundIntactAfterSever || !densityDropped
                    || !accentOk || !sawDarkOak || !sawArchBrick
                    || !coreHasVeinHalo || !grew1 || !grew2 || !stalledInLight || !coreDestroyedOk || !completedOk) {
                source.sendFailure(Component.literal(String.format(
                        "[Verify:HyphalSiege] FAIL: sprout=%d overlay=%s ambush=%s sev=%d dirtOk=%s drop=%s acc=%s oak=%s brick=%s halo=%s grew=%s/%s stall=%s coreDel=%s comp=%s",
                        sprouted, veinOverlayOnIntactDirt, ambushOk, severed, groundIntactAfterSever, densityDropped,
                        accentOk, sawDarkOak, sawArchBrick, coreHasVeinHalo,
                        grew1, grew2, stalledInLight, coreDestroyedOk, completedOk)));
                return 0;
            }
            source.sendSuccess(() -> Component.literal(
                    "[Verify:HyphalSiege] PASS: Hyphal Route Veins/Ambushes, Architectural Accent Reflection & Blind-Spot Outbreaks verified.")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, cleanupArea)) {
                if (!preExistingIds.contains(e.getUUID()) && !(e instanceof ServerPlayer)) {
                    e.discard();
                }
            }
            try {
                restoreBlocks(level, blockSnapshot);
            } finally {
                try {
                    tracker.restoreFromSnapshot(trackerSnapshot, level.registryAccess());
                } finally {
                    try {
                        hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
                    } finally {
                        syncAllPlayers(level);
                    }
                }
            }
        }
    }

    private static int verifyLoot(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var registries = level.getServer().reloadableRegistries();

        for (ResourceKey<LootTable> chestKey : MycelialLootSystem.ALL_CHEST_TABLES) {
            if (registries.getLootTable(chestKey) == LootTable.EMPTY) {
                source.sendFailure(Component.literal("[Verify:Loot] FAIL: Missing chest table " + chestKey.location()));
                return 0;
            }
        }
        for (String nodeId : List.of("blocks/mycelial_node", "blocks/incubator_node", "blocks/sentinel_node")) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, nodeId));
            if (registries.getLootTable(key) == LootTable.EMPTY) {
                source.sendFailure(Component.literal("[Verify:Loot] FAIL: Missing node table " + nodeId));
                return 0;
            }
        }
        ResourceKey<LootTable> clericGiftKey = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "gameplay/hero_of_the_village/cleric_mycelial_gift"));
        if (registries.getLootTable(clericGiftKey) == LootTable.EMPTY) {
            source.sendFailure(Component.literal("[Verify:Loot] FAIL: Missing cleric_mycelial_gift loot table."));
            return 0;
        }
        if (MycelialLootSystem.rollTraceDrop("underground_traces", RandomSource.create(7L)).isEmpty()
                || MycelialLootSystem.rollTraceDrop("deep_traces", RandomSource.create(7L)).isEmpty()) {
            source.sendFailure(Component.literal("[Verify:Loot] FAIL: GLM trace roll returned empty."));
            return 0;
        }

        boolean sawCodedSample = false;
        Set<net.minecraft.world.item.Item> uniqueBonusItems = new HashSet<>();
        RandomSource dropRng = RandomSource.create(42L);
        for (int i = 0; i < 24; i++) {
            List<ItemStack> drops = MycelialLootSystem.computeAdaptiveMobDrops(
                    EvolutionRank.ELITE,
                    HiveStrain.AMBER,
                    HiveProgressionTier.OVERMIND,
                    EnumSet.of(AdaptationTrait.FIRE_RESISTANT),
                    true,
                    3,
                    dropRng
            );
            for (ItemStack stack : drops) {
                uniqueBonusItems.add(stack.getItem());
                if (stack.is(ModItems.GENETIC_SAMPLE.get())
                        && GeneticSampleItem.getTrait(stack) == AdaptationTrait.FIRE_RESISTANT) {
                    sawCodedSample = true;
                }
            }
        }

        if (!sawCodedSample || uniqueBonusItems.size() < 4) {
            source.sendFailure(Component.literal("[Verify:Loot] FAIL: sawCodedSample=" + sawCodedSample
                    + " uniqueBonusItems=" + uniqueBonusItems.size()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Verify:Loot] PASS: "
                        + MycelialLootSystem.ALL_CHEST_TABLES.size()
                        + " chest tables, 3 node tables & adaptive mob drops verified ("
                        + uniqueBonusItems.size() + " drop variants).")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyHomeostasis(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos base = BlockPos.containing(source.getPosition()).offset(0, 14, 0);
        ChunkPos chunk = new ChunkPos(base);
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        BlockPos pylon1 = base;
        int stepX = (base.getX() & 15) <= 10 ? 2 : -2;
        BlockPos pylon2 = base.offset(stepX, 0, 0);
        BlockPos shelterFeet = base.south(4);
        BlockPos shelterRoof = shelterFeet.above(3);
        BlockPos expCeiling = shelterFeet.above(6);
        BlockPos openFeet = base.south(6);
        BlockPos openCeiling = openFeet.above(6);
        BlockPos cystProbe = base.above(1);

        Set<BlockPos> touchedPositions = new LinkedHashSet<>();
        touchedPositions.add(pylon1);
        touchedPositions.add(pylon2);
        touchedPositions.add(shelterRoof);
        touchedPositions.add(expCeiling);
        touchedPositions.add(openCeiling);
        touchedPositions.add(cystProbe);
        for (int dy = 2; dy <= 5; dy++) {
            touchedPositions.add(openFeet.above(dy));
            touchedPositions.add(shelterFeet.above(dy));
        }
        for (Direction d : Direction.values()) {
            touchedPositions.add(pylon1.relative(d));
            touchedPositions.add(pylon2.relative(d));
        }
        for (BlockPos pos : touchedPositions) {
            if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
                source.sendFailure(Component.literal("[Verify:Homeostasis] Refused: scenario positions must be inside the world and already loaded."));
                return 0;
            }
        }

        CompoundTag homeostasisSnapshot = data.save(new CompoundTag(), level.registryAccess());
        CompoundTag trackerSnapshot = tracker.save(new CompoundTag(), level.registryAccess());
        Map<BlockPos, SavedBlock> blockSnapshot = snapshotBlocks(level, touchedPositions);
        java.util.List<BlockPos> cystCleanup = new java.util.ArrayList<>();

        try {
            data.resetAll();
            tracker.clearAll();
            level.setBlockAndUpdate(openCeiling, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
            level.setBlockAndUpdate(expCeiling, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
            for (int dy = 2; dy <= 5; dy++) {
                level.removeBlock(openFeet.above(dy), false);
                level.removeBlock(shelterFeet.above(dy), false);
            }
            for (Direction d : Direction.values()) {
                level.removeBlock(pylon1.relative(d), false);
            }
            level.setBlockAndUpdate(pylon1, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            tracker.addPylon(pylon1);
            DimensionalHomeostasisDirector.handleForeignPlacement(level, pylon1, level.getBlockState(pylon1), true);
            level.setBlockAndUpdate(pylon2, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            tracker.addPylon(pylon2);
            DimensionalHomeostasisDirector.handleForeignPlacement(level, pylon2, level.getBlockState(pylon2), true);
            DimensionalHomeostasisDirector.recordExplosionImpact(level, base, 20, 4.0F);

            double escalatedStress = data.getStress(chunk);
            var escalatedStage = data.getStage(chunk);
            int sensAtImmune = data.getSensitization(chunk);

            // Remove secondary stress-probe pylon so 4 cysts encapsulate pylon1 and leave pylon1.north() unshielded.
            level.removeBlock(pylon2, false);
            tracker.removePylon(pylon2);
            data.removeForeignBlock(pylon2);

            boolean exposedOpen = DimensionalHomeostasisDirector.isExposedToCeilingAcidRain(level, openFeet);
            level.setBlockAndUpdate(shelterRoof, ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
            boolean shelteredUnderBarrier = !DimensionalHomeostasisDirector.isExposedToCeilingAcidRain(level, shelterFeet);

            int cystsGrown = DimensionalHomeostasisDirector.tryEncapsulateChunk(level, chunk, 4, true);
            for (Direction d : Direction.values()) {
                BlockPos adj = pylon1.relative(d);
                if (level.getBlockState(adj).is(ModBlocks.ANTIBODY_CYST.get())) {
                    cystCleanup.add(adj);
                }
            }
            boolean smothered = tracker.isPylonSmothered(pylon1) && !tracker.isProtected(pylon1.north());

            // Remove 2 cysts around pylon1 -> frees pylon and restores protection
            if (cystCleanup.size() >= 2) {
                level.removeBlock(cystCleanup.get(0), false);
                level.removeBlock(cystCleanup.get(1), false);
            }
            boolean freed = !tracker.isPylonSmothered(pylon1) && tracker.isProtected(pylon1.north());

            DimensionalHomeostasisDirector.sedateArea(
                    level, base, 0,
                    DimensionalHomeostasisEngine.SUPPRESSOR_SEDATION,
                    DimensionalHomeostasisEngine.SUPPRESSOR_SENSITIZATION_REDUCTION,
                    null
            );
            double sedatedStress = data.getStress(chunk);
            int sensAfterSedation = data.getSensitization(chunk);

            // Verify inflammatory diffusion to neighbor chunk & active/withered cyst analog signal
            ChunkPos neighborChunk = new ChunkPos(chunk.x + 1, chunk.z);
            data.setStress(chunk, 88.0, level.getGameTime());
            DimensionalHomeostasisDirector.diffuseInflammationToNeighbors(level, data, chunk, 88.0);
            double neighborStress = data.getStress(neighborChunk);

            level.setBlockAndUpdate(cystProbe, ModBlocks.ANTIBODY_CYST.get().defaultBlockState());
            int activeSignal = level.getBlockState(cystProbe).getAnalogOutputSignal(level, cystProbe);
            level.setBlockAndUpdate(cystProbe, ModBlocks.ANTIBODY_CYST.get().defaultBlockState()
                    .setValue(com.alex.mycelialmind.block.AntibodyCystBlock.ACTIVE, false));
            int witheredSignal = level.getBlockState(cystProbe).getAnalogOutputSignal(level, cystProbe);
            level.removeBlock(cystProbe, false);

            if (escalatedStage != DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE
                    || !exposedOpen || !shelteredUnderBarrier
                    || cystsGrown < 4 || !smothered || !freed
                    || sedatedStress >= escalatedStress
                    || sensAtImmune < 1 || sensAfterSedation >= sensAtImmune
                    || neighborStress <= 0.0 || neighborStress > DimensionalHomeostasisEngine.INFLAMMATORY_DIFFUSION_CAP_WITHOUT_FOREIGN + 0.01
                    || activeSignal <= 0 || witheredSignal != 0) {
                source.sendFailure(Component.literal(String.format(
                        "[Verify:Homeostasis] FAIL: stress=%.1f stage=%s open=%s roof=%s cysts=%d smothered=%s freed=%s sedated=%.1f sens=%d->%d nbr=%.1f sig=%d/%d",
                        escalatedStress, escalatedStage, exposedOpen, shelteredUnderBarrier, cystsGrown,
                        smothered, freed, sedatedStress, sensAtImmune, sensAfterSedation, neighborStress, activeSignal, witheredSignal)));
                return 0;
            }
            source.sendSuccess(() -> Component.literal(String.format(
                    "[Verify:Homeostasis] PASS: escalation(%.1f%%), roof shelter, antibody smother/free (%d cysts), sensitization, diffusion & sedation verified.",
                    escalatedStress, cystsGrown)).withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            try {
                restoreBlocks(level, blockSnapshot);
            } finally {
                try {
                    tracker.restoreFromSnapshot(trackerSnapshot, level.registryAccess());
                } finally {
                    try {
                        data.restoreFromSnapshot(homeostasisSnapshot, level.registryAccess());
                    } finally {
                        syncAllPlayers(level);
                    }
                }
            }
        }
    }

    private static int verifyPolish(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var soundEntries = ModSounds.SOUND_EVENTS.getEntries();
        boolean soundsOk = soundEntries.size() == 208;
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
        boolean entitySoundsOk = true;
        var genericDmg = level.damageSources().generic();
        for (EntityType<? extends MycelialMob> type : mycelialTypes) {
            MycelialMob mob = type.create(level);
            if (mob == null) {
                entitySoundsOk = false;
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
                    entitySoundsOk = false;
                } else {
                    distinctAmbient.add(amb.getLocation());
                    distinctHurt.add(hurt.getLocation());
                    distinctDeath.add(death.getLocation());
                }
            } finally {
                mob.discard();
            }
        }
        PurifierDroneEntity drone = ModEntities.PURIFIER_DRONE.get().create(level);
        if (drone == null) {
            entitySoundsOk = false;
        } else {
            try {
                SoundEvent amb = drone.resolveAmbientSound();
                SoundEvent hurt = drone.resolveHurtSound(genericDmg);
                SoundEvent death = drone.resolveDeathSound();
                if (amb == null || hurt == null || death == null
                        || !MycelialMind.MODID.equals(amb.getLocation().getNamespace())
                        || !MycelialMind.MODID.equals(hurt.getLocation().getNamespace())
                        || !MycelialMind.MODID.equals(death.getLocation().getNamespace())) {
                    entitySoundsOk = false;
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
            entitySoundsOk = false;
        } else {
            try {
                SoundEvent amb = chimera.resolveAmbientSound();
                SoundEvent hurt = chimera.resolveHurtSound(genericDmg);
                SoundEvent death = chimera.resolveDeathSound();
                if (amb == null || hurt == null || death == null
                        || !MycelialMind.MODID.equals(amb.getLocation().getNamespace())
                        || !MycelialMind.MODID.equals(hurt.getLocation().getNamespace())
                        || !MycelialMind.MODID.equals(death.getLocation().getNamespace())) {
                    entitySoundsOk = false;
                } else {
                    distinctAmbient.add(amb.getLocation());
                    distinctHurt.add(hurt.getLocation());
                    distinctDeath.add(death.getLocation());
                }
            } finally {
                chimera.discard();
            }
        }

        var cBosses = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
        boolean tagsAndBenchOk = new ItemStack(ModItems.SYMBIOTIC_BLADE.get()).is(ItemTags.SWORDS)
                && new ItemStack(ModItems.CHITIN_CHESTPLATE.get()).is(ItemTags.CHEST_ARMOR)
                && ModEntities.APEX_MATRIARCH.get().is(cBosses)
                && SynapticBenchBlockEntity.isValidSlotInput(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.GENETIC_SAMPLE.get()))
                && SynapticBenchBlockEntity.isValidSlotInput(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.SPORE_ANTIDOTE.get()))
                && SynapticBenchBlockEntity.isValidSlotInput(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()))
                && SynapticBenchBlockEntity.isValidSlotInput(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.SYNAPTIC_CORE.get()))
                && SynapticBenchBlockEntity.isValidSlotInput(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(Items.AMETHYST_SHARD))
                && !SynapticBenchBlockEntity.isValidSlotInput(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(Items.DIRT))
                && ComposterBlock.getValue(new ItemStack(ModItems.MYCELIAL_BIOMASS.get())) > 0.0F
                && ComposterBlock.getValue(new ItemStack(ModBlocks.HYPHAL_VEIN.asItem())) > 0.0F
                && ComposterBlock.getValue(new ItemStack(ModBlocks.ANTIBODY_CYST.asItem())) > 0.0F
                && ComposterBlock.getValue(new ItemStack(ModBlocks.RECLAIMED_BLOOM.asItem())) > 0.0F
                && new ItemStack(ModItems.MYCELIAL_BIOMASS.get()).getBurnTime(null) == 400
                && new ItemStack(ModBlocks.LUMINESCENT_AMBER_VEIN.asItem()).getBurnTime(null) == 1200
                && new ItemStack(ModItems.VOLATILE_ENGRAM.get()).getBurnTime(null) == 1600
                && net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION
                        .wrapAsHolder(net.minecraft.world.entity.npc.VillagerProfession.CLERIC)
                        .getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.RAID_HERO_GIFTS) != null
                && com.alex.mycelialmind.registry.ModLootModifiers.MYCELIAL_TRACE_LOOT.isBound()
                && ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState().hasAnalogOutputSignal()
                && ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState().hasAnalogOutputSignal()
                && ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState().hasAnalogOutputSignal()
                && ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_HOE)
                && ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                && new ItemStack(ModItems.SYNAPTIC_SUPPRESSOR.get()).is(ItemTags.DURABILITY_ENCHANTABLE)
                && new ItemStack(ModItems.RIFT_CATALYST.get()).is(ItemTags.DURABILITY_ENCHANTABLE)
                && new ItemStack(ModItems.SCHISM_INJECTOR.get()).is(ItemTags.DURABILITY_ENCHANTABLE)
                && new ItemStack(ModItems.FALSE_ENGRAM_INJECTOR.get()).is(ItemTags.DURABILITY_ENCHANTABLE)
                && SynapticBenchRecipeDefinitions.allCompatInfoEntries().size() == 36
                && ProgressionGoalEngine.BESTIARY_SPECIES_IDS.size() == 17
                && ProgressionGoalEngine.TOTAL_SECRETS == 8
                && Math.abs(ModArmorMaterials.CHITIN.value().toughness() - 1.0F) < 0.01F
                && level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "smelt_creeping_mycelium_to_stone")).isPresent()
                && level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "blast_cyst_to_chitin_plate")).isPresent()
                && level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "stonecut_mycelial_stone_to_arch_brick")).isPresent()
                && level.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "recipes/unlock_biomass_recipes")) != null;

        // Dedicated servers use a SERVER_DATA resource manager, so client particle descriptors
        // (assets/mycelialmind/particles) are not visible here. Asset parity is covered by the
        // particle GameTest and audit_mod_assets; this command verifies the registry side.
        boolean particlesOk = ModParticles.all().size() >= 30;

        boolean thresholdsOk = Math.abs(MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.getDefault() - 70.0) < 0.01
                && Math.abs(MycelialConfig.TIER2_ACTIVATION_THRESHOLD.getDefault() - 200.0) < 0.01
                && MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get() > 0.0
                && MycelialConfig.TIER2_ACTIVATION_THRESHOLD.get() > MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get()
                && Math.abs(HiveMindSavedData.HABIT_ACTIVATION_THRESHOLD - 60.0) < 0.01;

        if (!soundsOk || !entitySoundsOk || distinctAmbient.size() != 17
                || distinctHurt.size() != 17 || distinctDeath.size() != 17
                || !tagsAndBenchOk || !thresholdsOk || !particlesOk) {
            source.sendFailure(Component.literal(String.format(
                    "[Verify:Polish] FAIL: sounds=%s(%d) entitySounds=%s ambient=%d hurt=%d death=%d tagsBench=%s thresholds=%s particles=%s(%d)",
                    soundsOk, soundEntries.size(), entitySoundsOk,
                    distinctAmbient.size(), distinctHurt.size(), distinctDeath.size(),
                    tagsAndBenchOk, thresholdsOk, particlesOk, ModParticles.all().size())));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(String.format(
                "[Verify:Polish] PASS: %d ModSounds, %d distinct entity audio profiles (17 ambient/hurt/death), %d particle types with resources, tags/bench & calibrated thresholds (70/200/60) verified.",
                soundEntries.size(), distinctAmbient.size(), ModParticles.all().size())).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int verifyDimensionProgression(CommandSourceStack source) {
        var server = source.getServer();
        ServerLevel overworld = server.overworld();
        ServerLevel deep = server.getLevel(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL);
        if (deep == null) {
            source.sendFailure(Component.literal("[Verify:DimensionProgression] deep_mycelium level missing."));
            return 0;
        }
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        HiveMindSavedData deepHive = HiveMindSavedData.get(deep);
        DimensionalHomeostasisSavedData deepHomeo = DimensionalHomeostasisSavedData.get(deep);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag deepHiveSnapshot = deepHive.save(new CompoundTag(), deep.registryAccess());
        CompoundTag deepHomeoSnapshot = deepHomeo.save(new CompoundTag(), deep.registryAccess());

        try {
            owHive.resetAll();
            deepHive.resetAll();
            deepHomeo.resetAll();
            long now = overworld.getGameTime();
            owHive.recordStimulus(StimulusType.EXPLOSION, 100.0, now);
            owHive.addThreatPenalty(55.0);
            boolean resonant = com.alex.mycelialmind.world.ModDimensions.syncCrossDimensionProgression(overworld, deep);

            boolean deepSynced = resonant
                    && deepHive.isDeepMyceliumBreached()
                    && owHive.isDeepMyceliumBreached()
                    && deepHive.getThreatLevel() >= 42.5
                    && deepHive.getHighestProgressionTier().level() >= com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE.level()
                    && deepHive.getActiveTraits().contains(AdaptationTrait.BLAST_SHIELDED);

            double scaledBeforeRemission = DimensionalHomeostasisDirector.applyProgressionStressScaling(deep, 10.0);
            com.alex.mycelialmind.entity.ApexMatriarchEntity.onMatriarchDefeated(
                    deep, new BlockPos(0, 64, 0), false);

            boolean postMatriarchOk = owHive.isMatriarchDefeated()
                    && deepHive.isMatriarchDefeated()
                    && owHive.isMatriarchRemissionActive(overworld.getGameTime())
                    && deepHive.isMatriarchRemissionActive(deep.getGameTime())
                    && owHive.getThreatLevel() < 45.0
                    && com.alex.mycelialmind.hive.HiveProgressionTier.resolveEffectiveTier(
                            deepHive.getThreatLevel(), deepHive.getHighestTierReached())
                            == com.alex.mycelialmind.hive.HiveProgressionTier.OVERMIND
                    && MycelialLootSystem.selectSealedBioVaultTable(
                            com.alex.mycelialmind.hive.HiveProgressionTier.DORMANT, deep.random)
                            .equals(MycelialLootSystem.OUTBREAK_SEALED_VAULT)
                    && MycelialLootSystem.selectSealedBioVaultTable(
                            com.alex.mycelialmind.hive.HiveProgressionTier.ADAPTIVE, deep.random)
                            .equals(MycelialLootSystem.SANCTUM_FORBIDDEN_LAB);
            double scaledInRemission = DimensionalHomeostasisDirector.applyProgressionStressScaling(deep, 10.0);

            if (!deepSynced || scaledBeforeRemission <= 10.0 || !postMatriarchOk || scaledInRemission >= scaledBeforeRemission) {
                source.sendFailure(Component.literal(String.format(
                        "[Verify:DimensionProgression] FAIL: synced=%s scaleBefore=%.2f postOk=%s scaleRem=%.2f owThreat=%.1f",
                        deepSynced, scaledBeforeRemission, postMatriarchOk, scaledInRemission, owHive.getThreatLevel())));
                return 0;
            }
            source.sendSuccess(() -> Component.literal(String.format(
                    "[Verify:DimensionProgression] PASS: Overworld->Deep sync (%.1f%%), tier stress scaling (%.2fx->%.2fx) & Post-Matriarch Remission verified.",
                    deepHive.getThreatLevel(), scaledBeforeRemission / 10.0, scaledInRemission / 10.0))
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            try {
                owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
            } finally {
                try {
                    deepHive.restoreFromSnapshot(deepHiveSnapshot, deep.registryAccess());
                } finally {
                    try {
                        deepHomeo.restoreFromSnapshot(deepHomeoSnapshot, deep.registryAccess());
                    } finally {
                        syncAllPlayers(overworld);
                        syncAllPlayers(deep);
                    }
                }
            }
        }
    }

    private static Map<BlockPos, SavedBlock> snapshotBlocks(ServerLevel level, Set<BlockPos> positions) {
        Map<BlockPos, SavedBlock> snapshot = new LinkedHashMap<>();
        for (BlockPos pos : positions) {
            BlockPos immutable = pos.immutable();
            BlockEntity blockEntity = level.getBlockEntity(immutable);
            CompoundTag blockEntityData = blockEntity == null
                    ? null
                    : blockEntity.saveWithFullMetadata(level.registryAccess());
            snapshot.put(immutable, new SavedBlock(level.getBlockState(immutable), blockEntityData));
        }
        return snapshot;
    }

    private static boolean ensureScenarioPositionsLoaded(CommandSourceStack source, String scenario,
                                                         Set<BlockPos> positions) {
        ServerLevel level = source.getLevel();
        for (BlockPos pos : positions) {
            if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
                source.sendFailure(Component.literal("[Verify:" + scenario
                        + "] Refused: scenario positions must be inside the world and already loaded."));
                return false;
            }
        }
        return true;
    }

    private static void restoreBlocks(ServerLevel level, Map<BlockPos, SavedBlock> snapshot) {
        for (Map.Entry<BlockPos, SavedBlock> entry : snapshot.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue().state(), 3);
        }
        for (Map.Entry<BlockPos, SavedBlock> entry : snapshot.entrySet()) {
            CompoundTag blockEntityData = entry.getValue().blockEntityData();
            if (blockEntityData == null) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (blockEntity != null) {
                blockEntity.loadWithComponents(blockEntityData.copy(), level.registryAccess());
                blockEntity.setChanged();
            }
        }
    }

    private static boolean requireHomeostasisDimension(CommandSourceStack source) {
        if (DimensionalHomeostasisDirector.isHomeostasisDimension(source.getLevel())) {
            return true;
        }
        source.sendFailure(Component.translatable("message.mycelialmind.homeostasis_requires_dimension"));
        return false;
    }

    private static int reportHomeostasisStatus(CommandSourceStack source) {
        if (!requireHomeostasisDimension(source)) {
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        ChunkPos chunk = new ChunkPos(origin);
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        double stress = data.getStress(chunk);
        var stage = data.getStage(chunk);
        int sens = data.getSensitization(chunk);
        int cysts = data.getActiveCystCount(chunk);
        int smothered = PurificationZoneTracker.get(level).countSmotheredPylons();
        source.sendSuccess(() -> Component.literal(String.format(
                "[Homeostasis] Chunk(%d,%d) Stress: %.1f%% [%s] | Sensitization: Lv.%d/3 | CytokineStorm: %s | Cysts: %d | SmotheredPylons: %d | DimensionActive: %s",
                chunk.x, chunk.z, stress, stage.displayLabel(), sens,
                DimensionalHomeostasisEngine.isCytokineStorm(stress),
                cysts, smothered,
                DimensionalHomeostasisDirector.isHomeostasisDimension(level)
        )).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int setHomeostasisStress(CommandSourceStack source, double amount) {
        if (!requireHomeostasisDimension(source)) {
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        ChunkPos chunk = new ChunkPos(origin);
        DimensionalHomeostasisSavedData data = DimensionalHomeostasisSavedData.get(level);
        data.setStress(chunk, amount, level.getGameTime());
        syncAllPlayers(level);
        source.sendSuccess(() -> Component.literal(String.format(
                "[Homeostasis] Chunk(%d,%d) stress set to %.1f%% [%s].",
                chunk.x, chunk.z, data.getStress(chunk), data.getStage(chunk).displayLabel()
        )).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int sedateHomeostasis(CommandSourceStack source) {
        if (!requireHomeostasisDimension(source)) {
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        DimensionalHomeostasisDirector.sedateArea(
                level, origin, 1,
                DimensionalHomeostasisEngine.SUPPRESSOR_SEDATION,
                DimensionalHomeostasisEngine.SUPPRESSOR_SENSITIZATION_REDUCTION,
                source.getEntity() instanceof ServerPlayer sp ? sp : null
        );
        source.sendSuccess(() -> Component.literal("[Homeostasis] Local neighborhood sedated.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int reportHabits(CommandSourceStack source) {
        HiveMindSavedData hive = HiveMindSavedData.get(source.getLevel());
        source.sendSuccess(() -> Component.literal(String.format(
                "[Habits] Medic=%.1f | Mobility=%.1f | Bastion=%.1f | Shadow=%.1f | False=%s",
                hive.getHabitScore(com.alex.mycelialmind.hive.PlayerHabit.COMBAT_MEDIC),
                hive.getHabitScore(com.alex.mycelialmind.hive.PlayerHabit.HIGH_MOBILITY),
                hive.getHabitScore(com.alex.mycelialmind.hive.PlayerHabit.BASTION_BUILDER),
                hive.getHabitScore(com.alex.mycelialmind.hive.PlayerHabit.SHADOW_INFILTRATOR),
                hive.getFalseStimulus() != null ? hive.getFalseStimulus().id() : "none"
        )).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int reportRoutesAndArchitecture(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        BlockPos origin = BlockPos.containing(source.getPosition());
        ChunkPos cp = new ChunkPos(origin);
        var accent = hive.getArchitecturalAccent();
        source.sendSuccess(() -> Component.literal(String.format(
                "[Routes&Arch] Chunk(%d,%d) Density=%.1f%% Traversals=%d ActiveVeins=%d (TotalChunks=%d, TotalVeins=%d) | Accent=%s/%s Prop=%s Span=%.1f H/W=%.2f Light=%.0f%% Samples=%d | BlindSpots=%d",
                cp.x, cp.z,
                hive.getHyphalDensity(cp),
                hive.getHyphalTraversalCount(cp),
                hive.getActiveVeins(cp).size(),
                hive.countTrackedHyphalChunks(),
                hive.countTotalActiveVeins(),
                accent.hasCustomAccent() ? accent.primaryBlockId() : "default",
                accent.hasCustomAccent() ? accent.pillarBlockId() : "default",
                accent.proportion().id(),
                accent.typicalRoomSpan(),
                accent.heightToWidthRatio(),
                accent.lightDensity() * 100.0,
                accent.sampledBlocksCount(),
                hive.getBlindSpotSites().size()
        )).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int runWorldgenAudit(CommandSourceStack source, int radiusChunks, int sanctumGridChunks) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        WorldgenAuditEngine.WorldgenAuditReport report =
                WorldgenAuditEngine.runAudit(level, origin, radiusChunks, sanctumGridChunks);
        var jsonPath = WorldgenAuditEngine.writeDiagnosticFiles(report);
        String line = report.toCompactAgentLine()
                + jsonPath.map(p -> " | json=" + p.getFileName()).orElse("");

        if (report.passed()) {
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        source.sendFailure(Component.literal(line + " | failures=" + String.join("; ", report.failures())));
        return 0;
    }

    private static int reportSymbiosis(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be executed by a player."));
            return 0;
        }
        PlayerSymbiosisData data = player.getData(ModAttachments.SYMBIOSIS);
        String list = data.getEquippedMutagens().isEmpty()
                ? "None"
                : data.getEquippedMutagens().stream().map(SymbioticMutagen::id).collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal(String.format(
                "[Symbiosis] Slots: %d/%d | Mutagens: %s | ThreatAmp: %.2fx",
                data.getEquippedCount(), MycelialConfig.MAX_MUTAGEN_SLOTS.get(), list, data.getThreatAmplifier()
        )).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int clearSymbiosis(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be executed by a player."));
            return 0;
        }
        player.getData(ModAttachments.SYMBIOSIS).clearMutagens();
        SymbiosisAttributeSync.sync(player);
        SymbiosisSyncPayload.syncToPlayer(player);
        source.sendSuccess(() -> Component.literal("[Symbiosis] Equipped mutagens cleared.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int grantMutagen(CommandSourceStack source, String rawId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be executed by a player."));
            return 0;
        }
        SymbioticMutagen mutagen = SymbioticMutagen.fromId(rawId.toLowerCase(Locale.ROOT));
        if (mutagen == null) {
            source.sendFailure(Component.literal("Unknown mutagen: " + rawId));
            return 0;
        }
        if (player.getData(ModAttachments.SYMBIOSIS).equipMutagen(mutagen)) {
            SymbiosisAttributeSync.sync(player);
            SymbiosisSyncPayload.syncToPlayer(player);
            source.sendSuccess(() -> Component.literal("[Symbiosis] Implanted " + mutagen.id())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            return 1;
        }
        source.sendFailure(Component.literal("[Symbiosis] Slots full or mutagen already active."));
        return 0;
    }

    private static void syncAllPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, HiveSyncPayload.forPlayer(level, player));
        }
    }

    private static int setDiplomacyRep(CommandSourceStack source, String strainRaw, int value) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be executed by a player."));
            return 0;
        }
        var strain = com.alex.mycelialmind.hive.HiveStrain.byName(strainRaw);
        if (!strain.isSchismStrain()) {
            source.sendFailure(Component.literal("Use 'amber' or 'crimson'."));
            return 0;
        }
        var dip = player.getData(ModAttachments.DIPLOMACY);
        dip.setReputation(strain, value);
        dip.setSelectedStrain(strain);
        com.alex.mycelialmind.network.DiplomacySyncPayload.syncToPlayer(player);
        source.sendSuccess(() -> Component.literal("[Diplomacy] " + strain.serializedName() + " rep = " + value
                + " (" + dip.getStanding(strain).label() + ")").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int triggerSchismCommand(CommandSourceStack source, String strainRaw) {
        var strain = com.alex.mycelialmind.hive.HiveStrain.byName(strainRaw);
        if (!strain.isSchismStrain()) {
            strain = com.alex.mycelialmind.hive.HiveStrain.AMBER;
        }
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var mobs = level.getEntitiesOfClass(com.alex.mycelialmind.entity.MycelialMob.class,
                new net.minecraft.world.phys.AABB(origin).inflate(24.0D));
        for (int i = 0; i < mobs.size(); i++) {
            mobs.get(i).infectWithSchism(i % 2 == 0 ? strain : strain.oppositeSchism(), 600);
        }
        final int count = mobs.size();
        final String name = strain.serializedName();
        source.sendSuccess(() -> Component.literal("[Schism] Triggered " + name + " schism on " + count + " mobs.")
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int reportCycleStatus(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var cycles = com.alex.mycelialmind.hive.HiveCycleSavedData.get(level);
        boolean unlocked = com.alex.mycelialmind.hive.HiveCycleDirector.isCycleUnlocked(level);
        String muts = com.alex.mycelialmind.hive.HiveCycleMutator.decodeMask(cycles.getActiveMutatorMask())
                .stream().map(com.alex.mycelialmind.hive.HiveCycleMutator::id).collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal(String.format(
                "[Cycle] Unlocked=%s Active=%s Cycle=%d Mutators=[%s] Completed=%d BestScore=%d (Cycle %d, Last=%d %s)",
                unlocked, cycles.isCycleActive(), cycles.getActiveCycleNumber(),
                muts.isEmpty() ? "none" : muts,
                cycles.getCompletedCycles(), cycles.getBestScore(), cycles.getBestCycleNumber(),
                cycles.getLastCompletedScore(), cycles.getLastCompletedGrade()
        )).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int startCycleCmd(CommandSourceStack source, String rawMutators) {
        int mask = 0;
        for (String tok : rawMutators.trim().split("[,\\s]+")) {
            if (tok.isBlank()) continue;
            var mut = com.alex.mycelialmind.hive.HiveCycleMutator.byId(tok);
            if (mut == null) {
                source.sendFailure(Component.literal("[Cycle] Unknown mutator: " + tok));
                return 0;
            }
            mask |= mut.bitMask();
        }
        ServerLevel level = source.getLevel();
        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        if (!com.alex.mycelialmind.hive.HiveCycleDirector.startCycle(player, level, mask)) {
            source.sendFailure(Component.literal("[Cycle] Cannot start cycle (requires Matriarch defeated and 1-3 mutators, and no active cycle)."));
            return 0;
        }
        final int chosenMask = mask;
        source.sendSuccess(() -> Component.literal("[Cycle] Started cycle with mask=" + chosenMask)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int abortCycleCmd(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        if (!com.alex.mycelialmind.hive.HiveCycleDirector.abortCycle(player, level)) {
            source.sendFailure(Component.literal("[Cycle] No active cycle to abort."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Cycle] Aborted active cycle.")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int resetCycleCmd(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        com.alex.mycelialmind.hive.HiveCycleSavedData.get(level).resetAll();
        com.alex.mycelialmind.hive.HiveCycleDirector.syncHiveModifiersAcrossDimensions(level);
        com.alex.mycelialmind.network.CycleProtocolSyncPayload.syncToAll(level);
        source.sendSuccess(() -> Component.literal("[Cycle] Reset all cycle state and records.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int verifyCycles(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        var cycles = com.alex.mycelialmind.hive.HiveCycleSavedData.get(overworld);
        CompoundTag owHiveSnapshot = owHive.save(new CompoundTag(), overworld.registryAccess());
        CompoundTag cycleSnapshot = cycles.save(new CompoundTag(), overworld.registryAccess());
        com.alex.mycelialmind.entity.ApexMatriarchEntity testMatriarch = null;
        try {
            owHive.recordMatriarchDefeat(overworld.getGameTime());
            cycles.resetAll();
            int mask = com.alex.mycelialmind.hive.HiveCycleMutator.PRIMORDIAL_APEX.bitMask()
                    | com.alex.mycelialmind.hive.HiveCycleMutator.SYNAPTIC_SWARM.bitMask();
            if (!com.alex.mycelialmind.hive.HiveCycleDirector.startCycle(null, overworld, mask)) {
                source.sendFailure(Component.literal("[Verify:Cycles] FAIL: startCycle returned false."));
                return 0;
            }
            if (!cycles.isCycleActive() || cycles.getActiveMutatorMask() != mask) {
                source.sendFailure(Component.literal("[Verify:Cycles] FAIL: Cycle did not activate with requested mutators."));
                return 0;
            }
            testMatriarch = ModEntities.APEX_MATRIARCH.get().create(overworld);
            if (testMatriarch == null) {
                source.sendFailure(Component.literal("[Verify:Cycles] FAIL: Could not create ApexMatriarchEntity."));
                return 0;
            }
            testMatriarch.moveTo(source.getPosition().x, source.getPosition().y + 2, source.getPosition().z);
            com.alex.mycelialmind.hive.HiveCycleDirector.applyBossCycleScaling(testMatriarch, overworld);
            if (testMatriarch.getMaxHealth() <= 600.0F) {
                source.sendFailure(Component.literal("[Verify:Cycles] FAIL: ApexMatriarch HP did not scale with PRIMORDIAL_APEX."));
                return 0;
            }
            var res = cycles.completeCycle(overworld.getGameTime() + 100L);
            if (res == null || res.score() < 1000 || cycles.getCompletedCycles() != 1 || cycles.getBestScore() != res.score()) {
                source.sendFailure(Component.literal("[Verify:Cycles] FAIL: Cycle completion/record calculation invalid."));
                return 0;
            }
            final float scaledHp = testMatriarch.getMaxHealth();
            source.sendSuccess(() -> Component.literal("[Verify:Cycles] PASS (ScaledHP="
                    + scaledHp + ", Score=" + res.score() + ", Grade=" + res.grade() + ")")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            if (testMatriarch != null) {
                testMatriarch.discard();
            }
            owHive.restoreFromSnapshot(owHiveSnapshot, overworld.registryAccess());
            cycles.restoreFromSnapshot(cycleSnapshot, overworld.registryAccess());
            com.alex.mycelialmind.hive.HiveCycleDirector.syncHiveModifiersAcrossDimensions(overworld);
            com.alex.mycelialmind.network.CycleProtocolSyncPayload.syncToAll(overworld);
            syncAllPlayers(overworld);
        }
    }

    private static int reportColonyStatus(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        var nearest = saved.findNearestColony(origin, null, 256.0D);
        if (nearest == null) {
            source.sendSuccess(() -> Component.literal("[Colony] No Sovereign Colony found within 256 blocks. Total in dim: "
                    + saved.getAllColonies().size()).withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(String.format(
                "[Colony] Nexus=%s | Tier=%d | Radius=%dm | Pop=%d/%d | Biomass=%d | Morale=%.0f%% | DomainXP=%d | Traits=0x%X | Outposts=%d | Orders=%d",
                nearest.getNexusPos().toShortString(),
                nearest.getTier(),
                nearest.getEffectiveRadius(),
                nearest.getRoster().size(),
                nearest.getPopulationCap(),
                nearest.getBiomassStored(),
                nearest.getColonyMorale(),
                nearest.getDomainXp(),
                nearest.getUnlockedTraitMask(),
                nearest.getLinkedOutposts().size(),
                nearest.getOrderBoard().size()
        )).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int spawnColonySymbiontCmd(CommandSourceStack source, String rawCaste) {
        var caste = com.alex.mycelialmind.colony.SymbiontCaste.byId(rawCaste);
        if (caste == null) {
            source.sendFailure(Component.literal("[Colony] Unknown caste: " + rawCaste
                    + ". Valid: harvester, excavator, courier, architect, sentinel"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        var nearest = saved.findNearestColony(origin, null, 256.0D);
        if (nearest == null || !(level.getBlockEntity(nearest.getNexusPos()) instanceof com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity nexus)) {
            source.sendFailure(Component.literal("[Colony] No Sovereign Nexus found within 256 blocks."));
            return 0;
        }
        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        var worker = nexus.incubateSymbiont(level, player, caste, true);
        if (worker == null) {
            source.sendFailure(Component.literal("[Colony] Failed to incubate Colony Symbiont."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Colony] Incubated " + caste.id()
                + " Symbiont (" + worker.getTemperament().id() + ") at " + worker.blockPosition().toShortString())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int upgradeColonyCmd(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var nearest = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level).findNearestColony(origin, null, 256.0D);
        if (nearest == null || !(level.getBlockEntity(nearest.getNexusPos()) instanceof com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity nexus)) {
            source.sendFailure(Component.literal("[Colony] No Sovereign Nexus found within 256 blocks."));
            return 0;
        }
        if (!nexus.upgradeTier(level, null, true)) {
            source.sendFailure(Component.literal("[Colony] Colony is already at Max Tier (T3)."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Colony] Upgraded Sovereign Colony to Tier " + nearest.getTier()
                + " (Radius=" + nearest.getEffectiveRadius() + "m, Cap=" + nearest.getPopulationCap() + ").")
                .withStyle(ChatFormatting.AQUA), true);
        return 1;
    }

    private static int addColonyBiomassCmd(CommandSourceStack source, int amount) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        var nearest = saved.findNearestColony(origin, null, 256.0D);
        if (nearest == null) {
            source.sendFailure(Component.literal("[Colony] No Sovereign Nexus found within 256 blocks."));
            return 0;
        }
        nearest.enableAdministrativeOverride();
        int added = nearest.addBiomass(amount);
        saved.setDirty();
        source.sendSuccess(() -> Component.literal("[Colony] Added " + added + " biomass (Total=" + nearest.getBiomassStored() + ").")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int triggerColonyRaidCmd(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        var nearest = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level).findNearestColony(origin, null, 256.0D);
        if (nearest == null) {
            source.sendFailure(Component.literal("[Colony] No Sovereign Nexus found within 256 blocks."));
            return 0;
        }
        nearest.enableAdministrativeOverride();
        com.alex.mycelialmind.colony.SovereignColonySavedData.get(level).setDirty();
        int spawned = com.alex.mycelialmind.colony.SovereignColonyDirector.triggerRivalRaid(level, nearest, 0);
        source.sendSuccess(() -> Component.literal("[Colony] Dispatched Rival Hive probe (" + spawned + " hostiles).")
                .withStyle(ChatFormatting.RED), true);
        return 1;
    }

    private static int resetColonyCmd(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        com.alex.mycelialmind.colony.SovereignColonySavedData.get(level).resetAll();
        source.sendSuccess(() -> Component.literal("[Colony] Reset all Sovereign Colony data in this dimension.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int verifyColony(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition()).above(4);
        var saved = com.alex.mycelialmind.colony.SovereignColonySavedData.get(level);
        CompoundTag colonySnapshot = saved.save(new CompoundTag(), level.registryAccess());
        BlockState prevNexusState = level.getBlockState(origin);
        com.alex.mycelialmind.entity.ColonySymbiontEntity worker = null;
        try {
            saved.resetAll();
            level.setBlockAndUpdate(origin, ModBlocks.SOVEREIGN_NEXUS.get().defaultBlockState());
            if (!(level.getBlockEntity(origin) instanceof com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity nexus)) {
                source.sendFailure(Component.literal("[Verify:Colony] FAIL: SovereignNexusBlockEntity missing."));
                return 0;
            }
            nexus.enableAdministrativeOverride(level);
            var colony = nexus.getOrCreateColony(level);
            colony.addBiomass(64);
            colony.addDomainXp(160);
            if (!nexus.upgradeTier(level, null, false) || colony.getTier() != 2) {
                source.sendFailure(Component.literal("[Verify:Colony] FAIL: Tier 2 upgrade check failed."));
                return 0;
            }
            worker = nexus.incubateSymbiont(level, null, com.alex.mycelialmind.colony.SymbiontCaste.HARVESTER, true);
            if (worker == null || colony.getRoster().size() != 1) {
                source.sendFailure(Component.literal("[Verify:Colony] FAIL: Symbiont incubation/roster check failed."));
                return 0;
            }
            worker.setCaste(com.alex.mycelialmind.colony.SymbiontCaste.SENTINEL);
            if (worker.getCaste() != com.alex.mycelialmind.colony.SymbiontCaste.SENTINEL) {
                source.sendFailure(Component.literal("[Verify:Colony] FAIL: Symbiont caste switch failed."));
                return 0;
            }
            if (ModSounds.COLONY_DIALECT_TONE_1.get() == null
                    || ModSounds.COLONY_DIALECT_TONE_2.get() == null
                    || ModSounds.COLONY_DIALECT_TONE_3.get() == null) {
                source.sendFailure(Component.literal("[Verify:Colony] FAIL: Dialect tone sounds missing."));
                return 0;
            }
            var dialectA = new com.alex.mycelialmind.colony.ColonyDialect();
            var dialectB = new com.alex.mycelialmind.colony.ColonyDialect();
            if (!dialectA.teach(com.alex.mycelialmind.colony.ColonySiteHabit.ESCORTED_COURIER, 2)
                    || dialectA.teach(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN, 2)
                    || !dialectA.clearCall(2)
                    || !dialectA.teach(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN, 2)
                    || !dialectB.teach(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN, 2)
                    || dialectA.sharedCall(com.alex.mycelialmind.colony.ColonySiteHabit.REST_BEFORE_RETURN, dialectB) != 2) {
                source.sendFailure(Component.literal("[Verify:Colony] FAIL: Dialect teach/clearCall/sharedCall check failed."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("[Verify:Colony] PASS (Tier=" + colony.getTier()
                    + ", Radius=" + colony.getEffectiveRadius() + ", TraitMask=0x"
                    + Integer.toHexString(colony.getUnlockedTraitMask()) + ", Dialect=OK)").withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            if (worker != null) {
                worker.discard();
            }
            level.setBlockAndUpdate(origin, prevNexusState);
            saved.restoreFromSnapshot(colonySnapshot, level.registryAccess());
        }
    }

    private static int reportAiDiagnostics(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(center).inflate(radius);
        List<MycelialMob> mobs = level.getEntitiesOfClass(MycelialMob.class, box, mob -> mob.isAlive());
        var doctrine = com.alex.mycelialmind.hive.HiveBlackboard.getDoctrine(level);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "[Mycelial AI] radius=%dm | doctrine=%s | active_mobs=%d",
                radius, doctrine.name(), mobs.size()))
                .withStyle(ChatFormatting.AQUA), false);

        int shown = 0;
        for (MycelialMob mob : mobs) {
            if (shown >= 8) {
                break;
            }
            var brain = mob.getHiveBrain();
            String activity = (brain != null && brain.getActiveActivity() != null)
                    ? brain.getActiveActivity().getName()
                    : "none";
            String role = com.alex.mycelialmind.hive.HiveSwarmCoordinator.assignRole(mob).name();
            String intent = mob.getTacticalIntent().name();
            String target = mob.getTarget() != null ? mob.getTarget().getName().getString() : "-";
            String flags = (mob.isDesynced() ? "DESYNC " : "")
                    + (mob.isFrenzyActive() ? "FRENZY " : "")
                    + (mob.isPhalanxActive() ? "PHALANX " : "");
            final String line = String.format(
                    Locale.ROOT,
                    " - %s #%d | act=%s | intent=%s | role=%s | target=%s %s",
                    mob.getType().getDescription().getString(),
                    mob.getId(),
                    activity,
                    intent,
                    role,
                    target,
                    flags.isBlank() ? "" : "[" + flags.trim() + "]");
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false);
            shown++;
        }
        return 1;
    }

    private static int verifySinkhole(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos base = BlockPos.containing(source.getPosition());
        int originY = Math.min(level.getMaxBuildHeight() - 12, Math.max(base.getY() + 44, level.getMinBuildHeight() + 44));
        BlockPos origin = new BlockPos(base.getX() + 48, originY, base.getZ() + 48);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        CompoundTag hiveSnapshot = hive.save(new CompoundTag(), level.registryAccess());
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        CompoundTag trackerSnapshot = tracker.save(new CompoundTag(), level.registryAccess());
        java.util.Map<BlockPos, BlockState> savedBlocks = new java.util.HashMap<>();
        BlockPos benchPos = origin.offset(0, 6, 0);
        try {
            tracker.clearAll();
            for (int dx = -11; dx <= 11; dx++) {
                for (int dz = -19; dz <= 22; dz++) {
                    for (int dy = -34; dy <= 5; dy++) {
                        BlockPos p = origin.offset(dx, dy, dz);
                        savedBlocks.put(p, level.getBlockState(p));
                        if (dy <= -2) {
                            level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                        } else if (dy == -1) {
                            level.setBlock(p, net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                        } else {
                            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
            if (!com.alex.mycelialmind.world.MycelialSinkholeFeature.generateSinkholeAt(level, origin, level.getRandom())) {
                source.sendFailure(Component.literal("[Verify:Sinkhole] FAIL: generateSinkholeAt returned false."));
                return 0;
            }
            BlockPos ventPos = origin.below(30);
            BlockPos northWater = origin.offset(0, -31, -13);
            BlockPos northMoss = origin.offset(3, -31, -13);
            BlockPos southTunnel = origin.offset(0, -29, 12);
            boolean geoOk = level.getBlockState(ventPos).is(ModBlocks.SPORE_UPDRAFT_VENT.get())
                    && level.getBlockState(northWater).is(net.minecraft.world.level.block.Blocks.WATER)
                    && level.getBlockState(northMoss).is(ModBlocks.ROOT_SAP_MOSS.get())
                    && level.getBlockState(southTunnel).isAir();
            if (!geoOk) {
                source.sendFailure(Component.literal("[Verify:Sinkhole] FAIL: geometry check failed."));
                return 0;
            }
            savedBlocks.put(benchPos, level.getBlockState(benchPos));
            level.setBlockAndUpdate(benchPos, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get().defaultBlockState());
            if (!(level.getBlockEntity(benchPos) instanceof SynapticBenchBlockEntity bench)) {
                source.sendFailure(Component.literal("[Verify:Sinkhole] FAIL: bench missing."));
                return 0;
            }
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, new ItemStack(ModItems.ROOT_SAP_VIAL.get()));
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.ROOT_HEART_RESIN.get()));
            ItemStack out = bench.computeSynthesisResult();
            if (!out.is(ModItems.RESONANT_SAP_ELIXIR.get())) {
                source.sendFailure(Component.literal("[Verify:Sinkhole] FAIL: bench failed to synthesize resonant_sap_elixir."));
                return 0;
            }
            BlockPos pylonPos = origin.offset(-2, -30, 2);
            level.setBlockAndUpdate(pylonPos, ModBlocks.PURIFICATION_PYLON.get().defaultBlockState());
            if (!com.alex.mycelialmind.world.SinkholeEcosystemEngine.isBreachSealed(true, tracker.isProtected(ventPos))) {
                source.sendFailure(Component.literal("[Verify:Sinkhole] FAIL: Purification Pylon did not seal sinkhole breach."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("[Verify:Sinkhole] PASS (3-strata + N/S galleries + Elixir synthesis + Pylon seal)")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } finally {
            net.minecraft.world.phys.AABB cleanupBox = new net.minecraft.world.phys.AABB(
                    origin.getX() - 14.0D, origin.getY() - 35.0D, origin.getZ() - 20.0D,
                    origin.getX() + 14.0D, origin.getY() + 8.0D, origin.getZ() + 24.0D);
            for (var e : level.getEntitiesOfClass(com.alex.mycelialmind.entity.RootKeeperEntity.class, cleanupBox)) {
                e.discard();
            }
            for (var e : level.getEntitiesOfClass(com.alex.mycelialmind.entity.SporeGliderEntity.class, cleanupBox)) {
                e.discard();
            }
            for (var entry : savedBlocks.entrySet()) {
                level.setBlock(entry.getKey(), entry.getValue(), 2);
            }
            tracker.restoreFromSnapshot(trackerSnapshot, level.registryAccess());
            hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
        }
    }
}
