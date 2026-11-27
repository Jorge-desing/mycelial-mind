package com.alex.mycelialmind.network;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Client snapshot of the shared post-Matriarch expedition state. */
public final class ClientExpeditionCache {
    private static final PostMatriarchExpeditionSyncPayload EMPTY = new PostMatriarchExpeditionSyncPayload(
            false, false, 0L, 0, 0, 0, 0, 0, 0, null, false, 0, List.of());

    private static PostMatriarchExpeditionSyncPayload state = EMPTY;
    private static EnclaveDirectoryPayload directory = new EnclaveDirectoryPayload(0, 0, List.of(), null);

    private ClientExpeditionCache() {
    }

    public static void clear() {
        state = EMPTY;
        directory = new EnclaveDirectoryPayload(0, 0, List.of(), null);
    }

    public static void update(PostMatriarchExpeditionSyncPayload payload) {
        state = new PostMatriarchExpeditionSyncPayload(
                payload.unlocked(),
                payload.active(),
                payload.activePos(),
                payload.outcomeId(),
                payload.palette(),
                payload.progress(),
                payload.linkedFlora(),
                payload.siteStress(),
                payload.siteFlags(),
                payload.defensePylon(),
                payload.pulseInProgress(),
                payload.attackersRemaining(),
                List.copyOf(payload.enclaves())
        );
    }

    public static boolean isUnlocked() {
        return state.unlocked();
    }

    public static boolean hasActiveExpedition() {
        return state.active();
    }

    public static BlockPos getActivePos() {
        return state.active() ? BlockPos.of(state.activePos()) : null;
    }

    public static int getOutcomeId() {
        return state.outcomeId();
    }

    public static int getPalette() {
        return state.palette();
    }

    public static int getProgress() {
        return state.progress();
    }

    public static int getLinkedFlora() {
        return state.linkedFlora();
    }

    public static int getSiteStress() {
        return state.siteStress();
    }

    public static int getSiteFlags() {
        return state.siteFlags();
    }

    public static BlockPos getDefensePylon() {
        return state.defensePylon();
    }

    public static boolean isPulseInProgress() {
        return state.pulseInProgress();
    }

    public static int getAttackersRemaining() {
        return state.attackersRemaining();
    }

    public static List<PostMatriarchExpeditionSyncPayload.Marker> getEnclaves() {
        return state.enclaves();
    }

    public static void updateDirectory(EnclaveDirectoryPayload page) {
        directory = page;
    }

    public static EnclaveDirectoryPayload getDirectory() {
        return directory;
    }
}
