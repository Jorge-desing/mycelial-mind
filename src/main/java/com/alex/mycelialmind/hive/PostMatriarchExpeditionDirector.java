package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.block.BioluminescentMycelialCapBlock;
import com.alex.mycelialmind.block.IncubatorNodeBlock;
import com.alex.mycelialmind.block.MycelialNodeBlock;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.block.SentinelNodeBlock;
import com.alex.mycelialmind.block.SporeBulbBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.network.PostMatriarchExpeditionSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.ModDimensions;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/** Runs the shared, persistent recovery expeditions in deep_mycelium. */
public final class PostMatriarchExpeditionDirector {
    private static final int MAX_NODE_RANGE = 24;
    private static final int PYLON_RANGE = 20;

    private PostMatriarchExpeditionDirector() {
    }

    public static boolean handleScannerUseOn(ServerPlayer player, ServerLevel level, BlockPos pos, boolean sneaking) {
        if (!level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
            return false;
        }
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.get(level);
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.PURIFICATION_PYLON.get())) {
            BlockPos target = data.getActivePos();
            if (data.hasActiveExpedition() && data.getActiveOutcome() == PostMatriarchExpeditionData.Outcome.ROUTE
                    && target != null && target.distSqr(pos) <= (double) PYLON_RANGE * PYLON_RANGE) {
                if (!isActivePylon(level, pos)) {
                    player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_pylon_unavailable")
                            .withStyle(ChatFormatting.YELLOW), true);
                    return true;
                }
                if (!data.bindDefensePylon(pos)) {
                    player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_clear_hostiles", data.getPulseAttackers().size())
                            .withStyle(ChatFormatting.YELLOW), true);
                    return true;
                }
                showAdaptationBriefing(player, HiveMindSavedData.get(level));
                beginRoutePulse(player, level, data, target);
                return true;
            }
        }
        if (isEnemyNode(state)) {
            return handleNodeScan(player, level, data, pos, sneaking);
        }
        if (data.hasActiveExpedition() && data.getActiveOutcome() == PostMatriarchExpeditionData.Outcome.GARDEN) {
            BlockPos activePos = data.getActivePos();
            if (pos.equals(activePos) && state.getBlock() instanceof SymbioticNodeBlock) {
                showGardenStatus(player, data, pos);
                return true;
            }
            if (isFloraFocus(state)) {
                return linkGardenFocus(player, level, data, pos);
            }
        }
        if (data.hasActiveExpedition() && pos.equals(data.getActivePos())
                && state.getBlock() instanceof SymbioticNodeBlock) {
            showGardenStatus(player, data, pos);
            return true;
        }
        return false;
    }

    private static boolean handleNodeScan(
            ServerPlayer player,
            ServerLevel level,
            PostMatriarchExpeditionData data,
            BlockPos pos,
            boolean sneaking
    ) {
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        if (!PostMatriarchExpeditionRules.isUnlocked(hive)) {
            if (sneaking) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_locked")
                        .withStyle(ChatFormatting.GRAY), true);
                return true;
            }
            return false;
        }
        if (data.hasCompletedEnclave(pos)) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_already_recovered")
                    .withStyle(ChatFormatting.GREEN), true);
            return true;
        }
        if (!data.hasActiveExpedition()) {
            if (!sneaking) {
                showAdaptationBriefing(player, hive);
                return false;
            }
            showAdaptationBriefing(player, hive);
            int palette = PostMatriarchExpeditionRules.paletteForStimuli(hive.getStimulusScores());
            data.start(pos, PostMatriarchExpeditionData.Outcome.ROUTE, palette);
            player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_started",
                    pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_choices")
                    .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_toggle_hint")
                    .withStyle(ChatFormatting.GRAY));
            sync(level);
            return true;
        }

        if (!pos.equals(data.getActivePos())) {
            if (sneaking) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_one_active")
                        .withStyle(ChatFormatting.YELLOW), true);
                return true;
            }
            showAdaptationBriefing(player, hive);
            return false;
        }
        showAdaptationBriefing(player, hive);
        if (sneaking) {
            PostMatriarchExpeditionData.Outcome next = data.getActiveOutcome() == PostMatriarchExpeditionData.Outcome.ROUTE
                    ? PostMatriarchExpeditionData.Outcome.GARDEN
                    : PostMatriarchExpeditionData.Outcome.ROUTE;
            if (data.switchOutcome(next)) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_outcome_selected",
                        Component.translatable(outcomeTranslationKey(next))).withStyle(ChatFormatting.GOLD), true);
                if (next == PostMatriarchExpeditionData.Outcome.GARDEN) {
                    player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_graft_warning")
                            .withStyle(ChatFormatting.YELLOW), true);
                }
                sync(level);
            } else {
                player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_outcome_locked")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return true;
        }
        if (data.getActiveOutcome() == PostMatriarchExpeditionData.Outcome.GARDEN) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_use_graft")
                    .withStyle(ChatFormatting.AQUA), true);
            return true;
        }
        player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_route_needs_pylon", PYLON_RANGE)
                .withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    public static boolean beforeNodeGrafted(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
            return true;
        }
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.get(level);
        if (!data.hasActiveExpedition() || !pos.equals(data.getActivePos())) {
            return true;
        }
        if (data.getActiveOutcome() == PostMatriarchExpeditionData.Outcome.ROUTE) {
            if (data.getPulsesCompleted() > 0 || !data.switchOutcome(PostMatriarchExpeditionData.Outcome.GARDEN)) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_outcome_locked")
                        .withStyle(ChatFormatting.RED), true);
                return false;
            }
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_outcome_selected",
                    Component.translatable(outcomeTranslationKey(PostMatriarchExpeditionData.Outcome.GARDEN)))
                    .withStyle(ChatFormatting.GOLD), true);
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_graft_confirm")
                    .withStyle(ChatFormatting.YELLOW), true);
            sync(level);
            return false;
        }
        sync(level);
        return true;
    }

    private static void showAdaptationBriefing(ServerPlayer player, HiveMindSavedData hive) {
        var active = Component.empty();
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            if (hive.hasTrait(trait)) {
                if (!active.getSiblings().isEmpty()) active.append(", ");
                active.append(MycelialText.trait(trait));
            }
        }
        if (active.getSiblings().isEmpty()) active.append(Component.translatable("message.mycelialmind.none"));
        player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_active_traits", active)
                .withStyle(ChatFormatting.AQUA));
        if (hive.isSuppressed(player.serverLevel().getGameTime())) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_suppressed")
                    .withStyle(ChatFormatting.YELLOW));
        }
        StimulusType dominant = PostMatriarchExpeditionRules.dominantStimulus(hive.getStimulusScores());
        if (dominant == null) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_adaptation_none")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        AdaptationTrait trait = HiveMindSavedData.traitFor(dominant);
        if (hive.hasTrait(trait)) {
            Component advice = Component.translatable("message.mycelialmind.expedition_advice." + dominant.id());
            player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_adaptation_active",
                    MycelialText.trait(trait), advice).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_adaptation_learning",
                    MycelialText.stimulus(dominant)).withStyle(ChatFormatting.AQUA));
        }
    }

    private static void beginRoutePulse(ServerPlayer player, ServerLevel level, PostMatriarchExpeditionData data, BlockPos pos) {
        BlockPos pylon = data.getDefensePylon();
        boolean hasPylon = pylon != null && isActivePylon(level, pylon);
        if (!hasPylon) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_route_needs_pylon", PYLON_RANGE)
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (data.isPulseInProgress()) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_clear_hostiles", data.getPulseAttackers().size())
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        long now = level.getGameTime();
        if (!PostMatriarchExpeditionRules.canStartNextRoutePulse(
                true, 0, data.getPulsesCompleted(), now, data.getLastPulseGameTime())) {
            long waitSeconds = Math.max(1L, (PostMatriarchExpeditionRules.ROUTE_PULSE_COOLDOWN_TICKS
                    - (now - data.getLastPulseGameTime()) + 19L) / 20L);
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_pulse_wait", waitSeconds)
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int pulse = data.getPulsesCompleted() + 1;
        WaveSpawnResult wave = spawnPulseWave(level, pylon, pulse);
        List<UUID> attackers = wave.attackers();
        if (attackers.isEmpty()) {
            String reason = !wave.foundSpace() ? "message.mycelialmind.expedition_no_space"
                    : !wave.foundTarget() ? "message.mycelialmind.expedition_no_target"
                    : "message.mycelialmind.expedition_spawn_failed";
            player.displayClientMessage(Component.translatable(reason)
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (!data.beginPulse(pylon, attackers, now)) {
            for (UUID id : attackers) {
                var entity = level.getEntity(id);
                if (entity != null) entity.discard();
            }
            return;
        }
        level.sendParticles(ModParticles.SPORE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                24, 0.9, 0.8, 0.9, 0.03);
        level.playSound(null, pos, ModSounds.HIVE_THREAT_MILESTONE.get(), SoundSource.BLOCKS, 0.9F, 0.75F + pulse * 0.12F);
        player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_pulse_started",
                pulse, PostMatriarchExpeditionRules.REQUIRED_ROUTE_PULSES).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        sync(level);
    }

    private record WaveSpawnResult(List<UUID> attackers, boolean foundSpace, boolean foundTarget) { }

    private static WaveSpawnResult spawnPulseWave(ServerLevel level, BlockPos center, int pulse) {
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        List<EntityType<? extends MycelialMob>> squad = new ArrayList<>(HiveSquadComposer.composeWaveSquad(
                hive, HiveBlackboard.getDoctrine(level), pulse * 25, level.random,
                HiveBlackboard.getPacingPhase(level), tracker.countSymbioticNodes(),
                hive.isFungalTideActive(level.getGameTime())));
        int extraAttackers = HiveCycleDirector.getActiveExpeditionExtraAttackers(level);
        for (int i = 0; i < extraAttackers && !squad.isEmpty(); i++) {
            squad.add(squad.get(i % squad.size()));
        }
        List<UUID> attackers = new ArrayList<>();
        boolean foundSpace = false;
        boolean foundTarget = false;
        double promotionChance = hive.getEffectivenessPressure()
                + HiveProgressionTier.fromThreat(hive.getThreatLevel()).wavePromotionBonus()
                + HiveCycleDirector.getActiveExpeditionEliteBonus(level);
        for (EntityType<? extends MycelialMob> type : squad) {
            MycelialMob mob = type.create(level);
            if (mob == null) continue;
            boolean spawned = false;
            boolean prepared = false;
            for (int attempt = 0; attempt < 40 && !spawned; attempt++) {
                int dx = level.random.nextIntBetweenInclusive(-10, 10);
                int dz = level.random.nextIntBetweenInclusive(-10, 10);
                if (dx * dx + dz * dz < 16) continue;
                for (int dy = 4; dy >= -4 && !spawned; dy--) {
                    BlockPos at = center.offset(dx, dy, dz);
                    if (!level.isLoaded(at) || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) continue;
                    mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
                    if (!level.noCollision(mob)) continue;
                    foundSpace = true;
                    if (!prepared) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
                        HiveWaveDirector.prepareWaveMob(mob, hive, level.random, promotionChance);
                        prepared = true;
                    }
                    ServerPlayer target = level.players().stream()
                            .filter(candidate -> candidate.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5)
                                    <= 32.0 * 32.0 && mob.canAttack(candidate))
                            .min(java.util.Comparator.comparingDouble(mob::distanceToSqr))
                            .orElse(null);
                    if (target == null) break;
                    foundTarget = true;
                    mob.setPersistenceRequired();
                    mob.setTarget(target);
                    if (level.addFreshEntity(mob)) {
                        attackers.add(mob.getUUID());
                        spawned = true;
                    }
                }
            }
        }
        return new WaveSpawnResult(attackers, foundSpace, foundTarget);
    }

    private static boolean linkGardenFocus(ServerPlayer player, ServerLevel level, PostMatriarchExpeditionData data, BlockPos focusPos) {
        BlockPos nodePos = data.getActivePos();
        if (focusPos.distSqr(nodePos) > (double) MAX_NODE_RANGE * MAX_NODE_RANGE) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_focus_too_far", MAX_NODE_RANGE)
                    .withStyle(ChatFormatting.YELLOW), true);
            return true;
        }
        if (!(level.getBlockState(nodePos).getBlock() instanceof SymbioticNodeBlock)) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_use_graft")
                    .withStyle(ChatFormatting.AQUA), true);
            return true;
        }
        BlockState bloom = ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState()
                .setValue(ReclaimedBloomBlock.PALETTE, data.getActivePalette());
        if (!bloom.canSurvive(level, focusPos)) {
            player.displayClientMessage(Component.translatable("message.mycelialmind.expedition_focus_support")
                    .withStyle(ChatFormatting.YELLOW), true);
            return true;
        }
        if (!level.setBlock(focusPos, bloom, 3) || !data.addFlora(focusPos)) {
            return true;
        }
        level.sendParticles(ModParticles.SPORE_MOTE.get(), focusPos.getX() + 0.5, focusPos.getY() + 0.5,
                focusPos.getZ() + 0.5, 16, 0.35, 0.4, 0.35, 0.02);
        level.playSound(null, focusPos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.BLOCKS, 0.75F, 1.4F);
        player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_focus_linked",
                data.getLinkedFlora().size(), PostMatriarchExpeditionRules.REQUIRED_GARDEN_FOCI)
                .withStyle(ChatFormatting.GREEN));
        if (PostMatriarchExpeditionRules.gardenIsComplete(data.getLinkedFlora().size())) {
            completeEnclave(level, data, false);
        } else {
            sync(level);
        }
        return true;
    }

    private static void showGardenStatus(ServerPlayer player, PostMatriarchExpeditionData data, BlockPos pos) {
        player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_garden_status",
                pos.getX(), pos.getY(), pos.getZ(), data.getLinkedFlora().size(),
                PostMatriarchExpeditionRules.REQUIRED_GARDEN_FOCI).withStyle(ChatFormatting.AQUA));
        List<BlockPos> linked = data.getLinkedFlora();
        int needed = PostMatriarchExpeditionRules.REQUIRED_GARDEN_FOCI - linked.size();
        if (needed <= 0) return;
        ServerLevel level = player.serverLevel();
        int found = 0;
        BlockState bloom = ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState();
        for (BlockPos candidate : BlockPos.betweenClosed(
                pos.offset(-MAX_NODE_RANGE, -MAX_NODE_RANGE, -MAX_NODE_RANGE),
                pos.offset(MAX_NODE_RANGE, MAX_NODE_RANGE, MAX_NODE_RANGE))) {
            if (pos.distSqr(candidate) <= MAX_NODE_RANGE * MAX_NODE_RANGE
                    && !level.isOutsideBuildHeight(candidate) && level.isLoaded(candidate)
                    && isFloraFocus(level.getBlockState(candidate)) && !linked.contains(candidate)
                    && bloom.canSurvive(level, candidate)) {
                if (++found >= needed) return;
            }
        }
        player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_flora_shortfall",
                found, needed, MAX_NODE_RANGE).withStyle(ChatFormatting.YELLOW));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)
                || level.getGameTime() % 20L != 0L) {
            return;
        }
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.get(level);
        if (level.getGameTime() % 100L == 0L && (data.hasActiveExpedition() || !data.getEnclaves().isEmpty())) {
            for (ServerPlayer player : level.players()) {
                PostMatriarchExpeditionSyncPayload.syncSummaryToPlayer(player);
            }
        }
        if (!data.hasActiveExpedition() || data.getActiveOutcome() != PostMatriarchExpeditionData.Outcome.ROUTE) {
            return;
        }
        BlockPos nodePos = data.getActivePos();
        if (!level.isLoaded(nodePos) || !isEnemyNode(level.getBlockState(nodePos))) return;
        boolean playerNearby = level.players().stream()
                .filter(player -> player.isAlive() && !player.isSpectator())
                .anyMatch(player -> player.distanceToSqr(nodePos.getX() + 0.5, nodePos.getY() + 0.5, nodePos.getZ() + 0.5)
                        <= (double) MAX_NODE_RANGE * MAX_NODE_RANGE);
        boolean pausedChanged = data.setPulsePaused(data.isPulseInProgress() && !playerNearby);
        if (pausedChanged) {
            boolean pause = data.isPulsePaused();
            for (UUID attacker : data.getPulseAttackers()) {
                if (level.getEntity(attacker) instanceof MycelialMob mob) mob.setNoAi(pause);
            }
            Component notice = Component.translatable("message.mycelialmind.expedition_pulse_resumed")
                    .withStyle(ChatFormatting.YELLOW);
            for (ServerPlayer player : level.players()) {
                if (!pause && player.distanceToSqr(nodePos.getX() + 0.5, nodePos.getY() + 0.5, nodePos.getZ() + 0.5)
                        <= (double) MAX_NODE_RANGE * MAX_NODE_RANGE) {
                    player.displayClientMessage(notice, true);
                }
            }
            sync(level);
        }
        BlockPos pylon = data.getDefensePylon();
        if (pylon == null) pylon = findActivePylon(level, nodePos);
        if (playerNearby && pylon != null && level.isLoaded(pylon) && !isActivePylon(level, pylon)
                && data.isPulseInProgress()) {
            onDefensePylonDisabled(level, pylon);
        }
        if (!playerNearby || pylon == null || !isActivePylon(level, pylon)) return;
        boolean resolvedEscort = false;
        for (UUID attacker : data.getPulseAttackers()) {
            if (level.getEntity(attacker) instanceof MycelialMob mob && mob.isAlliedEscortActive()) {
                resolvedEscort |= data.resolveAttacker(attacker);
            }
        }
        if (resolvedEscort) sync(level);
        if (data.isPulseInProgress() && data.getPulseAttackers().isEmpty()) {
            if (data.finishPulse(level.getGameTime())) {
                level.playSound(null, pylon, ModSounds.PURIFICATION_PYLON_PULSE.get(),
                        SoundSource.BLOCKS, 0.85F, 1.25F);
                level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                        pylon.getX() + 0.5, pylon.getY() + 1.1, pylon.getZ() + 0.5,
                        16, 0.45, 0.35, 0.45, 0.03);
            }
            sync(level);
        }
        if (PostMatriarchExpeditionRules.routeIsComplete(data.getPulsesCompleted(), 0)) {
            completeEnclave(level, data, true);
        }
    }

    /** Called when the bound pylon is removed or becomes smothered, before it can be restored. */
    public static void onDefensePylonDisabled(ServerLevel level, BlockPos pylon) {
        if (!level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) return;
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.get(level);
        BlockPos nodePos = data.getActivePos();
        if (!data.isPulseInProgress() || nodePos == null || !pylon.equals(data.getDefensePylon())) return;
        discardRetiredLoadedAttackers(level, data.interruptPulse());
        level.sendParticles(ModParticles.DESYNC_FRAGMENT.get(),
                pylon.getX() + 0.5, pylon.getY() + 0.8, pylon.getZ() + 0.5,
                16, 0.5, 0.4, 0.5, 0.04);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(nodePos.getX() + 0.5, nodePos.getY() + 0.5, nodePos.getZ() + 0.5)
                    <= (double) MAX_NODE_RANGE * MAX_NODE_RANGE) {
                player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_pulse_interrupted")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        sync(level);
    }

    public static void onAttackerRemoved(ServerLevel level, UUID attacker) {
        if (level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
            var data = PostMatriarchExpeditionData.get(level);
            data.consumeRetiredAttacker(attacker);
            if (data.resolveAttacker(attacker)) sync(level);
        }
    }

    @SubscribeEvent
    public static void onAttackerLoad(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof MycelialMob mob
                && event.getLevel() instanceof ServerLevel level
                && level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
            PostMatriarchExpeditionData data = PostMatriarchExpeditionData.get(level);
            if (data.consumeRetiredAttacker(mob.getUUID())) {
                event.setCanceled(true);
            } else if (data.getPulseAttackers().contains(mob.getUUID())) {
                // The entity may have been unloaded when the pulse resumed.
                mob.setNoAi(data.isPulsePaused());
            }
        }
    }

    public static void discardRetiredLoadedAttackers(ServerLevel level, List<UUID> attackers) {
        for (UUID attacker : attackers) {
            var entity = level.getEntity(attacker);
            if (entity instanceof MycelialMob mob) mob.discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PostMatriarchExpeditionSyncPayload.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)
                || !(event.getState().getBlock() instanceof ReclaimedBloomBlock bloom)
                || !PostMatriarchExpeditionData.get(level).isGardenFloraPosition(event.getPos())) {
            return;
        }
        event.setCanceled(true);
        bloom.harvest(level, event.getPos(), event.getState(), event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PostMatriarchExpeditionSyncPayload.syncToPlayer(player);
        }
    }

    private static void completeEnclave(ServerLevel level, PostMatriarchExpeditionData data, boolean shutDownNode) {
        BlockPos pos = data.getActivePos();
        if (pos == null) {
            return;
        }
        PostMatriarchExpeditionData.Outcome outcome = data.getActiveOutcome();
        if (shutDownNode && outcome == PostMatriarchExpeditionData.Outcome.ROUTE) {
            level.setBlockAndUpdate(pos, ModBlocks.MYCELIAL_STONE.get().defaultBlockState());
            PurificationZoneTracker.get(level).removeNode(pos);
        }
        PostMatriarchExpeditionData.Enclave enclave = data.complete();
        if (enclave == null) {
            return;
        }
        SovereignColonySavedData colonySaved = SovereignColonySavedData.get(level);
        SovereignColonyData rewardColony = colonySaved.findNearestColony(pos, null, 128.0D);
        if (rewardColony != null) {
            rewardColony.addDomainXp(25);
            rewardColony.addBiomass(10);
            rewardColony.recalculateMorale();
            colonySaved.setDirty();
        } else {
            ServerLevel overworld = level.getServer().overworld();
            SovereignColonySavedData overworldSaved = SovereignColonySavedData.get(overworld);
            SovereignColonyData primary = overworldSaved.getAllColonies().stream().findFirst().orElse(null);
            if (primary != null) {
                primary.addDomainXp(25);
                primary.addBiomass(10);
                primary.recalculateMorale();
                overworldSaved.setDirty();
            }
        }
        String advancementId = outcome == PostMatriarchExpeditionData.Outcome.ROUTE ? "recovered_route" : "recovered_garden";
        String criterion = outcome == PostMatriarchExpeditionData.Outcome.ROUTE ? "recover_route" : "recover_garden";
        var advancement = level.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, advancementId));
        if (advancement != null) {
            for (ServerPlayer participant : level.players()) {
                if (participant.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 48.0 * 48.0) {
                    participant.getAdvancements().award(advancement, criterion);
                }
            }
        }
        level.sendParticles(ModParticles.SPORE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                48, 1.6, 1.4, 1.6, 0.04);
        level.playSound(null, pos, ModSounds.HIVE_THREAT_MILESTONE.get(), SoundSource.BLOCKS, 1.0F, 1.15F);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_completed",
                    Component.translatable(outcomeTranslationKey(outcome)), pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
        sync(level);
    }

    private static BlockPos findActivePylon(ServerLevel level, BlockPos center) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        double rangeSqr = (double) PYLON_RANGE * PYLON_RANGE;
        for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {
            for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {
                for (BlockPos pylon : tracker.getPylonsInChunk(chunkX, chunkZ)) {
                    if (!tracker.isPylonSmothered(pylon)
                            && pylon.distSqr(center) <= rangeSqr
                            && level.getBlockState(pylon).is(ModBlocks.PURIFICATION_PYLON.get())) {
                        return pylon;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isActivePylon(ServerLevel level, BlockPos pylon) {
        return level.isLoaded(pylon) && level.getBlockState(pylon).is(ModBlocks.PURIFICATION_PYLON.get())
                && !PurificationZoneTracker.get(level).isPylonSmothered(pylon);
    }

    private static boolean isEnemyNode(BlockState state) {
        return (state.getBlock() instanceof MycelialNodeBlock && !(state.getBlock() instanceof SymbioticNodeBlock))
                || state.getBlock() instanceof IncubatorNodeBlock
                || state.getBlock() instanceof SentinelNodeBlock;
    }

    public static boolean isExpeditionNode(BlockState state, PostMatriarchExpeditionData.Outcome outcome) {
        return isEnemyNode(state) || (outcome == PostMatriarchExpeditionData.Outcome.GARDEN
                && state.getBlock() instanceof SymbioticNodeBlock);
    }

    private static boolean isFloraFocus(BlockState state) {
        return state.getBlock() instanceof BioluminescentMycelialCapBlock
                || state.getBlock() instanceof SporeBulbBlock
                || state.getBlock() instanceof com.alex.mycelialmind.block.MycelialTendrilsBlock;
    }

    private static String outcomeTranslationKey(PostMatriarchExpeditionData.Outcome outcome) {
        return outcome == PostMatriarchExpeditionData.Outcome.GARDEN
                ? "codex.mycelialmind.expedition.outcome.garden"
                : "codex.mycelialmind.expedition.outcome.route";
    }

    private static void sync(ServerLevel level) {
        PostMatriarchExpeditionSyncPayload.syncToAll(level);
    }
}
