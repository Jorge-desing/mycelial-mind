package com.alex.mycelialmind.network;

import com.alex.mycelialmind.compat.curios.MycelialCuriosCompat;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.item.GeneticSampleItem;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/**
 * Client-side cache of the latest synchronized Hive Mind state, progression Act tier,
 * Genetic Bestiary, experimental secrets, Perfect Bastion defense rating,
 * and Ongoing Research expedition state.
 */
public final class ClientDiscoveryCache {
    private static boolean initialized = false;
    private static int lastAnnouncedActNumber = 1;
    private static int scannedSpeciesMask = 0;
    private static int discoveredTraitsMask = 0;
    private static int discoveredStrainsMask = 0;
    private static int discoveredSecretsMask = 0;
    private static int craftedMilestonesMask = 0;
    private static int discoveredEngramsMask = 0;
    private static int rareVariantsFound = 0;
    private static int bastionScore = 0;
    private static int highestActTier = 0;
    private static int lastObservationCode = 0;
    private static String lastObservationDimension = "";
    private static int pinnedExpeditionGoal = 0;
    private static int researchStageMask = 0;
    private static int recordedThreatPermille = 0;
    private static int lastThreatDropPermille = 0;

    private ClientDiscoveryCache() {
    }

    public static void clear() {
        initialized = false;
        lastAnnouncedActNumber = 1;
        scannedSpeciesMask = 0;
        discoveredTraitsMask = 0;
        discoveredStrainsMask = 0;
        discoveredSecretsMask = 0;
        craftedMilestonesMask = 0;
        discoveredEngramsMask = 0;
        rareVariantsFound = 0;
        bastionScore = 0;
        highestActTier = 0;
        lastObservationCode = 0;
        lastObservationDimension = "";
        pinnedExpeditionGoal = 0;
        researchStageMask = 0;
        recordedThreatPermille = 0;
        lastThreatDropPermille = 0;
    }

    public static void update(DiscoverySyncPayload payload) {
        boolean wasInitialized = initialized;
        int prevSecrets = discoveredSecretsMask;
        int prevEngrams = discoveredEngramsMask;

        scannedSpeciesMask = payload.scannedSpeciesMask();
        discoveredTraitsMask = payload.discoveredTraitsMask();
        discoveredStrainsMask = payload.discoveredStrainsMask();
        discoveredSecretsMask = payload.discoveredSecretsMask();
        craftedMilestonesMask = payload.craftedMilestonesMask();
        discoveredEngramsMask = payload.discoveredEngramsMask() & com.alex.mycelialmind.hive.FieldEngramId.ALL_MASK;
        rareVariantsFound = Math.max(0, payload.rareVariantsFound());
        bastionScore = Math.max(0, Math.min(100, payload.bastionScore()));
        highestActTier = Math.max(0, Math.min(HiveProgressionTier.OVERMIND.level(), payload.highestActTier()));
        lastObservationCode = ProgressionGoalEngine.CreatureObservation.byId(payload.lastObservationCode()).id();
        lastObservationDimension = payload.lastObservationDimension() != null ? payload.lastObservationDimension() : "";
        pinnedExpeditionGoal = ProgressionGoalEngine.ExpeditionGoal.byId(payload.pinnedExpeditionGoal()).id();
        researchStageMask = Math.max(0, payload.researchStageMask());
        recordedThreatPermille = Math.max(0, Math.min(100_000, payload.recordedThreatPermille()));
        lastThreatDropPermille = Math.max(0, Math.min(100_000, payload.lastThreatDropPermille()));

        HiveProgressionTier effectiveTier = getEffectiveActTier();
        int effectiveAct = effectiveTier.actNumber();
        initialized = true;

        if (!wasInitialized) {
            lastAnnouncedActNumber = Math.max(1, effectiveAct);
            return;
        }

        if (effectiveAct > lastAnnouncedActNumber) {
            lastAnnouncedActNumber = effectiveAct;
            com.alex.mycelialmind.client.screen.ClientUnreadCodexTracker.markActEntriesUnread(effectiveAct);
            com.alex.mycelialmind.client.hud.DiegeticToastManager.enqueue(
                    com.alex.mycelialmind.client.hud.DiegeticToastManager.ToastCategory.ACT_TRANSITION,
                    Component.translatable("toast.mycelialmind.act_unlocked.title",
                            effectiveAct, com.alex.mycelialmind.util.MycelialText.progressionTier(effectiveTier)),
                    Component.translatable("toast.mycelialmind.act_unlocked.sub")
            );
        }

        int newEngrams = discoveredEngramsMask & ~prevEngrams;
        if (newEngrams != 0) {
            for (var engram : com.alex.mycelialmind.hive.FieldEngramId.values()) {
                if ((newEngrams & engram.bitMask()) != 0) {
                    com.alex.mycelialmind.client.screen.ClientUnreadCodexTracker.markUnread(
                            engram.codexChapterId(), engram.codexEntryId());
                    com.alex.mycelialmind.client.hud.DiegeticToastManager.enqueue(
                            com.alex.mycelialmind.client.hud.DiegeticToastManager.ToastCategory.FIELD_ENGRAM,
                            Component.translatable(engram.titleKey()),
                            Component.translatable("toast.mycelialmind.engram_unlocked.sub", engram.actNumber())
                    );
                }
            }
        }

        int newSecrets = discoveredSecretsMask & ~prevSecrets;
        if (newSecrets != 0) {
            for (int i = 0; i < ProgressionGoalEngine.TOTAL_SECRETS; i++) {
                if ((newSecrets & (1 << i)) != 0) {
                    com.alex.mycelialmind.client.screen.ClientUnreadCodexTracker.markUnread(
                            "secretos_logros", "secretos_pistas_seguras");
                    com.alex.mycelialmind.client.hud.DiegeticToastManager.enqueue(
                            com.alex.mycelialmind.client.hud.DiegeticToastManager.ToastCategory.SECRET_UNLOCKED,
                            Component.translatable("codex.mycelialmind.secret.name." + (i + 1)),
                            Component.translatable("toast.mycelialmind.secret_unlocked.sub")
                    );
                }
            }
        }
    }

    public static int getHighestActTier() {
        return highestActTier;
    }

    public static HiveProgressionTier getEffectiveActTier() {
        return HiveProgressionTier.resolveEffectiveTier(ClientHiveCache.getThreatLevel(), highestActTier);
    }

    public static int getScannedSpeciesMask() {
        return scannedSpeciesMask;
    }

    public static int getDiscoveredTraitsMask() {
        return discoveredTraitsMask;
    }

    public static int getDiscoveredStrainsMask() {
        return discoveredStrainsMask;
    }

    public static int getDiscoveredSecretsMask() {
        return discoveredSecretsMask;
    }

    public static int getCraftedMilestonesMask() {
        return craftedMilestonesMask;
    }

    public static int getDiscoveredEngramsMask() {
        return discoveredEngramsMask;
    }

    public static int countDiscoveredEngrams() {
        return com.alex.mycelialmind.hive.FieldEngramId.countUnlocked(discoveredEngramsMask);
    }

    public static boolean hasEngram(@Nullable com.alex.mycelialmind.hive.FieldEngramId engram) {
        return engram != null && engram.isUnlocked(discoveredEngramsMask);
    }

    public static int getRareVariantsFound() {
        return rareVariantsFound;
    }

    public static int getBastionScore() {
        return bastionScore;
    }

    public static int countScannedSpecies() {
        return Integer.bitCount(scannedSpeciesMask & ((1 << ProgressionGoalEngine.TOTAL_SPECIES) - 1));
    }

    public static int countDiscoveredTraits() {
        return Integer.bitCount(discoveredTraitsMask & 0x1F);
    }

    public static int countDiscoveredSecrets() {
        return Integer.bitCount(discoveredSecretsMask & 0xFF);
    }

    public static boolean hasSecret(int secretBit) {
        return secretBit >= 0 && secretBit < ProgressionGoalEngine.TOTAL_SECRETS
                && (discoveredSecretsMask & (1 << secretBit)) != 0;
    }

    public static ProgressionGoalEngine.ShortTermGoal getActiveShortTermGoal() {
        return ProgressionGoalEngine.resolveShortTermGoal(
                craftedMilestonesMask, discoveredTraitsMask, discoveredSecretsMask);
    }

    public static int getLongTermMasteryPercent() {
        return ProgressionGoalEngine.computeLongTermMasteryPercent(
                countScannedSpecies(), countDiscoveredTraits(), countDiscoveredSecrets(), bastionScore);
    }

    public static int getLastObservationCode() {
        return lastObservationCode;
    }

    public static ProgressionGoalEngine.CreatureObservation getLastObservation() {
        return ProgressionGoalEngine.CreatureObservation.byId(lastObservationCode);
    }

    public static String getLastObservationDimension() {
        return lastObservationDimension;
    }

    public static int getPinnedExpeditionGoalId() {
        return pinnedExpeditionGoal;
    }

    public static ProgressionGoalEngine.ExpeditionGoal getPinnedExpeditionGoal() {
        return ProgressionGoalEngine.ExpeditionGoal.byId(pinnedExpeditionGoal);
    }

    public static void setClientPinnedExpeditionGoal(int goalId) {
        pinnedExpeditionGoal = ProgressionGoalEngine.ExpeditionGoal.byId(goalId).id();
    }

    public static int getResearchStageMask() {
        return researchStageMask;
    }

    public static boolean hasResearchBit(int bit) {
        return (researchStageMask & bit) != 0;
    }

    public static double getRecordedThreat() {
        return recordedThreatPermille / 1000.0D;
    }

    public static double getLastThreatDrop() {
        return lastThreatDropPermille / 1000.0D;
    }

    public static boolean hasItemInInventory(@Nullable Player player, Item item) {
        if (player == null || item == null) {
            return false;
        }
        if (player.getMainHandItem().is(item) || player.getOffhandItem().is(item)) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasProjectileDeflectSample(@Nullable Player player) {
        if (player == null) {
            return false;
        }
        if (isProjectileSample(player.getMainHandItem()) || isProjectileSample(player.getOffhandItem())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isProjectileSample(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProjectileSample(ItemStack stack) {
        return stack.is(ModItems.GENETIC_SAMPLE.get())
                && GeneticSampleItem.getTrait(stack) == AdaptationTrait.PROJECTILE_DEFLECT;
    }

    public static boolean hasSampleCatalyst(@Nullable Player player) {
        return hasItemInInventory(player, ModItems.SYNAPTIC_CORE.get())
                || hasItemInInventory(player, Items.AMETHYST_SHARD)
                || hasItemInInventory(player, ModItems.PURIFIER_GRENADE.get())
                || hasItemInInventory(player, ModItems.NEURAL_ECHO_CORE.get());
    }

    public static ProgressionGoalEngine.ResearchNextStep resolveClientResearchNextStep(@Nullable Player player) {
        boolean hasExtractor = hasItemInInventory(player, ModItems.BIO_EXTRACTOR.get());
        boolean hasScanner = hasItemInInventory(player, ModItems.BIO_SCANNER.get())
                || MycelialCuriosCompat.hasEquippedScanner(player);
        boolean hasProjSample = hasProjectileDeflectSample(player);
        boolean hasBiomass = hasItemInInventory(player, ModItems.MYCELIAL_BIOMASS.get());
        boolean hasLatentSpore = hasItemInInventory(player, ModItems.LATENT_SPORE.get());
        boolean hasCatalyst = hasSampleCatalyst(player);
        boolean hasBenchMilestone = ProgressionGoalEngine.hasMilestone(
                craftedMilestonesMask, ProgressionGoalEngine.MILESTONE_SYNAPTIC_BENCH)
                || hasItemInInventory(player, com.alex.mycelialmind.registry.ModBlocks.SYNAPTIC_RESEARCH_BENCH.asItem());
        return ProgressionGoalEngine.resolveResearchNextStep(
                getPinnedExpeditionGoal(),
                researchStageMask,
                hasExtractor,
                hasScanner,
                hasProjSample,
                hasBiomass,
                hasLatentSpore,
                hasCatalyst,
                hasBenchMilestone,
                ClientHiveCache.getThreatLevel()
        );
    }

    public static Component formatDimensionComponent(@Nullable String dimId) {
        if (dimId == null || dimId.isEmpty()) {
            return Component.translatable("codex.mycelialmind.research.dim.unknown");
        }
        return switch (dimId) {
            case "minecraft:overworld" -> Component.translatable("codex.mycelialmind.research.dim.overworld");
            case "minecraft:the_nether" -> Component.translatable("codex.mycelialmind.research.dim.nether");
            case "minecraft:the_end" -> Component.translatable("codex.mycelialmind.research.dim.end");
            case "mycelialmind:deep_mycelium" -> Component.translatable("dimension.mycelialmind.deep_mycelium");
            default -> Component.literal(dimId);
        };
    }
}
