package com.alex.mycelialmind.command;

import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Shared command messaging, block/hive snapshot restoration, and verification suite helpers for {@link MycelialCommands}.
 */
final class MycelialCommandSupport {

    record SavedBlock(BlockState state, CompoundTag blockEntityData) {
    }

    record VerifyCheck(String name, ToIntFunction<CommandSourceStack> runner) {
    }

    private MycelialCommandSupport() {
    }

    static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }

    static int pass(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    static int runVerifyChecks(CommandSourceStack source, List<VerifyCheck> checks) {
        StringBuilder failedScenarios = new StringBuilder();
        for (VerifyCheck check : checks) {
            if (check.runner().applyAsInt(source) != 1) {
                if (!failedScenarios.isEmpty()) {
                    failedScenarios.append(", ");
                }
                failedScenarios.append(check.name());
            }
        }
        if (failedScenarios.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[Mycelial Verify] ALL " + checks.size()
                    + " automated scenario checks PASSED.")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
            return 1;
        }
        return fail(source, "[Mycelial Verify] Failed scenarios: " + failedScenarios + ".");
    }

    static void syncAllPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, HiveSyncPayload.forPlayer(level, player));
        }
    }

    static Map<BlockPos, SavedBlock> snapshotBlocks(ServerLevel level, Set<BlockPos> positions) {
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

    static boolean ensureScenarioPositionsLoaded(CommandSourceStack source, String scenario,
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

    static void restoreBlocks(ServerLevel level, Map<BlockPos, SavedBlock> snapshot) {
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

    static void restoreHiveAndSync(ServerLevel level, HiveMindSavedData hive, CompoundTag hiveSnapshot) {
        try {
            hive.restoreFromSnapshot(hiveSnapshot, level.registryAccess());
        } finally {
            syncAllPlayers(level);
        }
    }

    static void restoreTrackerHiveAndSync(ServerLevel level,
                                          PurificationZoneTracker tracker,
                                          CompoundTag trackerSnapshot,
                                          HiveMindSavedData hive,
                                          CompoundTag hiveSnapshot) {
        try {
            tracker.restoreFromSnapshot(trackerSnapshot, level.registryAccess());
        } finally {
            restoreHiveAndSync(level, hive, hiveSnapshot);
        }
    }
}
