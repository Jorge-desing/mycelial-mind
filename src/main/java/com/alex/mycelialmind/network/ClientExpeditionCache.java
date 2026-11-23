package com.alex.mycelialmind.network;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Client snapshot of the shared post-Matriarch expedition state. */
public final class ClientExpeditionCache {
    private static boolean unlocked;
    private static boolean active;
    private static long activePos;
    private static int outcomeId;
    private static int palette;
    private static int progress;
    private static int linkedFlora;
    private static int siteStress;
    private static int siteFlags;
    private static BlockPos defensePylon;
    private static boolean pulseInProgress;
    private static int attackersRemaining;
    private static List<PostMatriarchExpeditionSyncPayload.Marker> enclaves = List.of();
    private static EnclaveDirectoryPayload directory = new EnclaveDirectoryPayload(0, 0, List.of(), null);

    private ClientExpeditionCache() {
    }

    public static void clear() {
        update(new PostMatriarchExpeditionSyncPayload(false, false, 0L, 0, 0, 0, 0, 0, 0,
                null, false, 0, List.of()));
        directory = new EnclaveDirectoryPayload(0, 0, List.of(), null);
    }

    public static void update(PostMatriarchExpeditionSyncPayload payload) {
        unlocked = payload.unlocked();
        active = payload.active();
        activePos = payload.activePos();
        outcomeId = payload.outcomeId();
        palette = payload.palette();
        progress = payload.progress();
        linkedFlora = payload.linkedFlora();
        siteStress = payload.siteStress();
        siteFlags = payload.siteFlags();
        defensePylon = payload.defensePylon();
        pulseInProgress = payload.pulseInProgress();
        attackersRemaining = payload.attackersRemaining();
        enclaves = List.copyOf(payload.enclaves());
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static boolean hasActiveExpedition() {
        return active;
    }

    public static BlockPos getActivePos() {
        return active ? BlockPos.of(activePos) : null;
    }

    public static int getOutcomeId() {
        return outcomeId;
    }

    public static int getPalette() {
        return palette;
    }

    public static int getProgress() {
        return progress;
    }

    public static int getLinkedFlora() {
        return linkedFlora;
    }

    public static int getSiteStress() { return siteStress; }
    public static int getSiteFlags() { return siteFlags; }
    public static BlockPos getDefensePylon() { return defensePylon; }
    public static boolean isPulseInProgress() { return pulseInProgress; }
    public static int getAttackersRemaining() { return attackersRemaining; }

    public static List<PostMatriarchExpeditionSyncPayload.Marker> getEnclaves() {
        return enclaves;
    }

    public static void updateDirectory(EnclaveDirectoryPayload page) { directory = page; }
    public static EnclaveDirectoryPayload getDirectory() { return directory; }
}
