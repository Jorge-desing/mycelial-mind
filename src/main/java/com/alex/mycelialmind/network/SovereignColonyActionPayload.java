package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.colony.ColonySiteMemory;
import com.alex.mycelialmind.colony.ColonyReconstruction;
import com.alex.mycelialmind.colony.ColonyReconstructionStatus;
import com.alex.mycelialmind.colony.ColonyWitnessMemory;
import com.alex.mycelialmind.hive.ColonyReconstructionDirector;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.colony.SymbiontCaste;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Client-to-server control actions for the Sovereign Nexus screen.
 */
public record SovereignColonyActionPayload(
        int action,
        BlockPos nexusPos,
        int param,
        UUID targetWorkerId,
        BlockPos sourceSitePos,
        BlockPos targetSitePos
) implements CustomPacketPayload {
    public static final int ACTION_INCUBATE = 1;
    public static final int ACTION_UPGRADE_TIER = 2;
    public static final int ACTION_RECALL_ALL = 3;
    public static final int ACTION_CYCLE_WORKER_CASTE = 4;
    public static final int ACTION_TOGGLE_WORKER_STANDBY = 5;
    public static final int ACTION_REQUEST_SYNC = 6;
    public static final int ACTION_ADOPT_SITE_MEMORY = 7;
    public static final int ACTION_FORGET_SITE_MEMORY = 8;
    public static final int ACTION_TEACH_SITE_MEMORY = 9;
    public static final int ACTION_CANCEL_MEMORY_TRANSFER = 10;
    public static final int ACTION_SET_RECONSTRUCTION_SUCCESSOR = 11;
    public static final int ACTION_CALL_RECONSTRUCTION_WITNESS = 12;
    public static final int ACTION_ABANDON_RECONSTRUCTION = 13;
    public static final int ACTION_START_DIALECT_LESSON = 14;
    public static final int ACTION_CANCEL_DIALECT_LESSON = 15;

    private static final UUID ZERO_UUID = new UUID(0L, 0L);

    public static final Type<SovereignColonyActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "sovereign_colony_action"));

    public static final StreamCodec<ByteBuf, SovereignColonyActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SovereignColonyActionPayload decode(ByteBuf buf) {
            int action = ByteBufCodecs.VAR_INT.decode(buf);
            BlockPos pos = BlockPos.of(buf.readLong());
            int param = ByteBufCodecs.VAR_INT.decode(buf);
            UUID workerId = new UUID(buf.readLong(), buf.readLong());
            BlockPos sourceSite = BlockPos.of(buf.readLong());
            BlockPos targetSite = BlockPos.of(buf.readLong());
            return new SovereignColonyActionPayload(action, pos, param, workerId, sourceSite, targetSite);
        }

        @Override
        public void encode(ByteBuf buf, SovereignColonyActionPayload payload) {
            ByteBufCodecs.VAR_INT.encode(buf, payload.action());
            buf.writeLong(payload.nexusPos().asLong());
            ByteBufCodecs.VAR_INT.encode(buf, payload.param());
            UUID id = payload.targetWorkerId() != null ? payload.targetWorkerId() : ZERO_UUID;
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
            buf.writeLong(payload.sourceSitePos().asLong());
            buf.writeLong(payload.targetSitePos().asLong());
        }
    };

    public static void sendToServer(int action, BlockPos nexusPos, int param, UUID workerId) {
        PacketDistributor.sendToServer(new SovereignColonyActionPayload(
                action,
                nexusPos != null ? nexusPos : BlockPos.ZERO,
                param,
                workerId != null ? workerId : ZERO_UUID,
                BlockPos.ZERO,
                BlockPos.ZERO
        ));
    }

    public static void sendMemoryAction(int action, BlockPos nexusPos, BlockPos sourceSitePos, BlockPos targetSitePos) {
        PacketDistributor.sendToServer(new SovereignColonyActionPayload(
                action,
                nexusPos != null ? nexusPos : BlockPos.ZERO,
                0,
                ZERO_UUID,
                sourceSitePos != null ? sourceSitePos : BlockPos.ZERO,
                targetSitePos != null ? targetSitePos : BlockPos.ZERO
        ));
    }

    public static void sendReconstructionAction(int action, BlockPos nexusPos, BlockPos ruinedSite,
                                                BlockPos successorSite, UUID witnessId,
                                                ColonyWitnessMemory.Type memoryType) {
        PacketDistributor.sendToServer(new SovereignColonyActionPayload(
                action,
                nexusPos != null ? nexusPos : BlockPos.ZERO,
                memoryType != null ? memoryType.ordinal() : 0,
                witnessId != null ? witnessId : ZERO_UUID,
                ruinedSite != null ? ruinedSite : BlockPos.ZERO,
                successorSite != null ? successorSite : BlockPos.ZERO
        ));
    }

    public static void handleOnServer(SovereignColonyActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> applyOnServer(payload, player));
    }

    /** Executes the same validated action on the server thread, including progression checks. */
    public static void applyOnServer(SovereignColonyActionPayload payload, ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            BlockPos nexusPos = payload.nexusPos();
            if (!level.isLoaded(nexusPos) || player.blockPosition().distSqr(nexusPos) > 128.0D * 128.0D) {
                return;
            }
            if (!(level.getBlockEntity(nexusPos) instanceof SovereignNexusBlockEntity nexus)) {
                return;
            }
            if (!SovereignNexusBlockEntity.isColonyActive(level, nexusPos) && !player.isCreative()) return;
            if (player.isCreative()) nexus.enableAdministrativeOverride(level);
            SovereignColonyData colony = nexus.getOrCreateColony(level);
            switch (payload.action()) {
                case ACTION_INCUBATE -> nexus.incubateSymbiont(level, player, SymbiontCaste.fromIndex(payload.param()), false);
                case ACTION_UPGRADE_TIER -> nexus.upgradeTier(level, player, false);
                case ACTION_RECALL_ALL -> nexus.recallAllWorkers(level);
                case ACTION_CYCLE_WORKER_CASTE -> {
                    if (level.getEntity(payload.targetWorkerId()) instanceof com.alex.mycelialmind.entity.ColonySymbiontEntity worker) {
                        nexus.setWorkerCaste(level, payload.targetWorkerId(), worker.getCaste().next());
                    }
                }
                case ACTION_TOGGLE_WORKER_STANDBY -> nexus.toggleWorkerStandby(level, payload.targetWorkerId());
                case ACTION_REQUEST_SYNC -> {
                }
                case ACTION_ADOPT_SITE_MEMORY -> {
                    if (player.getUUID().equals(colony.getOwnerId())) {
                        ColonySiteMemory memory = colony.getSiteMemory(payload.sourceSitePos());
                        if (memory != null && memory.adoptTrialHabit()) {
                            SovereignColonySavedData.get(level).setDirty();
                        }
                    }
                }
                case ACTION_FORGET_SITE_MEMORY -> {
                    if (player.getUUID().equals(colony.getOwnerId())) {
                        ColonySiteMemory memory = colony.getSiteMemory(payload.sourceSitePos());
                        if (memory != null) {
                            memory.forget();
                            SovereignColonyData.MemoryTransferSnapshot transfer = colony.getMemoryTransferSnapshot();
                            if (transfer != null && (transfer.sourcePos().equals(payload.sourceSitePos())
                                    || transfer.targetPos().equals(payload.sourceSitePos()))) {
                                colony.cancelMemoryTransfer();
                            }
                            SovereignColonySavedData.get(level).setDirty();
                        }
                    }
                }
                case ACTION_TEACH_SITE_MEMORY -> {
                    if (player.getUUID().equals(colony.getOwnerId())
                            && colony.startMemoryTransfer(payload.sourceSitePos(), payload.targetSitePos(), level.getGameTime())) {
                        SovereignColonySavedData.get(level).setDirty();
                    }
                }
                case ACTION_CANCEL_MEMORY_TRANSFER -> {
                    if (player.getUUID().equals(colony.getOwnerId()) && colony.getMemoryTransferSnapshot() != null) {
                        colony.cancelMemoryTransfer();
                        SovereignColonySavedData.get(level).setDirty();
                    }
                }
                case ACTION_SET_RECONSTRUCTION_SUCCESSOR -> {
                    if (player.getUUID().equals(colony.getOwnerId())
                            && colony.setReconstructionSuccessor(payload.sourceSitePos(), payload.targetSitePos())) {
                        SovereignColonySavedData.get(level).setDirty();
                    }
                }
                case ACTION_CALL_RECONSTRUCTION_WITNESS -> {
                    if (player.getUUID().equals(colony.getOwnerId())) {
                        ColonyWitnessMemory.Type type = ColonyWitnessMemory.Type.fromIndex(payload.param());
                        ColonyReconstruction entry = colony.getReconstruction(payload.sourceSitePos());
                        ColonyWitnessMemory memory = type == null ? null
                                : colony.getWitnessMemory(payload.targetWorkerId(), payload.sourceSitePos(), type);
                        if (entry != null && memory != null) {
                            ColonyReconstructionStatus status = ColonyReconstructionDirector.getCandidateStatus(
                                    level, colony, entry, memory);
                            if ((status == ColonyReconstructionStatus.READY || status == ColonyReconstructionStatus.NO_PATH
                                    || status == ColonyReconstructionStatus.HABIT_ALREADY_PRESENT)
                                    && colony.beginReconstructionTrip(payload.sourceSitePos(), payload.targetWorkerId(), type)) {
                                SovereignColonySavedData.get(level).setDirty();
                            } else {
                                player.displayClientMessage(Component.translatable(
                                        "message.mycelialmind.colony.reconstruction_unavailable",
                                        Component.translatable(status.translationKey())), true);
                            }
                        }
                    }
                }
                case ACTION_ABANDON_RECONSTRUCTION -> {
                    if (player.getUUID().equals(colony.getOwnerId())
                            && colony.abandonReconstruction(payload.sourceSitePos())) {
                        SovereignColonySavedData.get(level).setDirty();
                    }
                }
                case ACTION_START_DIALECT_LESSON -> {
                    int meaningIndex = payload.param() & 3;
                    int call = (payload.param() >> 2) & 15;
                    boolean coinage = (payload.param() & 64) != 0;
                    if (player.getUUID().equals(colony.getOwnerId())
                            && colony.startDialectLesson(payload.sourceSitePos(), payload.targetSitePos(),
                            com.alex.mycelialmind.colony.ColonySiteHabit.fromIndex(meaningIndex), call,
                            payload.targetWorkerId(), coinage)) {
                        SovereignColonySavedData.get(level).setDirty();
                    }
                }
                case ACTION_CANCEL_DIALECT_LESSON -> {
                    if (player.getUUID().equals(colony.getOwnerId()) && colony.getDialectLesson() != null) {
                        colony.cancelDialectLesson();
                        SovereignColonySavedData.get(level).setDirty();
                    }
                }
                default -> {
                }
            }
            nexus.syncLiveWorkers(level, colony);
            SovereignColonySyncPayload.syncToPlayer(player, nexusPos, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
