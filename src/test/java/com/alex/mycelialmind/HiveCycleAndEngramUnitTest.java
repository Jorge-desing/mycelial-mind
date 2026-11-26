package com.alex.mycelialmind;

import com.alex.mycelialmind.client.hud.DiegeticToastManager;
import com.alex.mycelialmind.client.screen.ClientUnreadCodexTracker;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.hive.HiveCycleEngine;
import com.alex.mycelialmind.hive.HiveCycleMutator;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiveCycleAndEngramUnitTest {

    @Test
    void fieldEngramCatalogHasTenUniqueBitsAndLookup() {
        assertEquals(10, FieldEngramId.TOTAL_COUNT);
        assertEquals((1 << 10) - 1, FieldEngramId.ALL_MASK);

        Set<String> ids = new HashSet<>();
        Set<Integer> bits = new HashSet<>();
        Set<String> entries = new HashSet<>();
        int mask = 0;
        for (FieldEngramId engram : FieldEngramId.values()) {
            assertTrue(ids.add(engram.id()), "Duplicate id: " + engram.id());
            assertTrue(bits.add(engram.bitIndex()), "Duplicate bitIndex: " + engram.bitIndex());
            assertTrue(entries.add(engram.codexEntryId()), "Duplicate codexEntryId: " + engram.codexEntryId());
            assertEquals("engram_" + engram.id(), engram.codexEntryId());
            assertTrue(engram.actNumber() >= 1 && engram.actNumber() <= 4);
            assertEquals("engram.mycelialmind." + engram.id() + ".title", engram.titleKey());
            assertEquals("engram.mycelialmind." + engram.id() + ".hint", engram.hintKey());
            assertFalse(engram.isUnlocked(mask));
            mask |= engram.bitMask();
            assertTrue(engram.isUnlocked(mask));
        }

        assertEquals(FieldEngramId.ALL_MASK, mask);
        assertEquals(10, FieldEngramId.countUnlocked(mask));
        assertEquals(10, FieldEngramId.countUnlocked(0xFFFF_FFFF));
        assertEquals(FieldEngramId.MATRIARCH_REQUIEM, FieldEngramId.byId(" MATRIARCH_REQUIEM "));
        assertEquals(FieldEngramId.OUTBREAK_SIGNAL, FieldEngramId.byId("nonexistent"));
        assertNull(FieldEngramId.byIdOrNull(null));
        assertNull(FieldEngramId.byIdOrNull("  "));
        assertNull(FieldEngramId.byIdOrNull("missing"));
        assertEquals(FieldEngramId.OVERMIND_CYCLE, FieldEngramId.byIdOrNull("overmind_cycle"));
    }

    @Test
    void hiveCycleMutatorsAndEngineScalingCoverAllTiersAndGrades() {
        assertEquals(6, HiveCycleMutator.values().length);
        assertEquals(3, HiveCycleMutator.MAX_ACTIVE_MUTATORS);
        assertEquals((1 << 6) - 1, HiveCycleMutator.ALL_MASK);

        assertFalse(HiveCycleEngine.isValidMutatorSelection(0));
        assertFalse(HiveCycleEngine.isValidMutatorSelection(1 << 6));
        int fourMutators = HiveCycleMutator.HYPER_ADAPTATION.bitMask()
                | HiveCycleMutator.PERPETUAL_TIDE.bitMask()
                | HiveCycleMutator.ACUTE_HOMEOSTASIS.bitMask()
                | HiveCycleMutator.SYNAPTIC_SWARM.bitMask();
        assertFalse(HiveCycleEngine.isValidMutatorSelection(fourMutators));

        int threeMutators = HiveCycleMutator.PRIMORDIAL_APEX.bitMask()
                | HiveCycleMutator.SYNAPTIC_SWARM.bitMask()
                | HiveCycleMutator.HYPER_ADAPTATION.bitMask();
        assertTrue(HiveCycleEngine.isValidMutatorSelection(threeMutators));
        assertEquals(3, HiveCycleMutator.countActive(threeMutators));
        assertEquals(3, HiveCycleMutator.decodeMask(threeMutators).size());
        assertEquals(HiveCycleMutator.PRIMORDIAL_APEX, HiveCycleMutator.byId("PRIMORDIAL_APEX"));
        assertNull(HiveCycleMutator.byId("invalid"));
        assertEquals(HiveCycleMutator.HYPHAL_OVERLOAD, HiveCycleMutator.byBit(5));

        assertEquals(1.0D, HiveCycleEngine.computeBossHealthMultiplier(false, 5, threeMutators), 1.0E-6);
        assertEquals(1.0D, HiveCycleEngine.computeBossDamageMultiplier(false, 5, threeMutators), 1.0E-6);
        assertEquals(1.0D, HiveCycleEngine.computeBossCooldownScale(false, 5, threeMutators), 1.0E-6);

        assertEquals(1.25D, HiveCycleEngine.computeBossHealthMultiplier(true, 1, HiveCycleMutator.PRIMORDIAL_APEX.bitMask()), 1.0E-6);
        assertEquals(1.15D, HiveCycleEngine.computeBossDamageMultiplier(true, 1, HiveCycleMutator.PRIMORDIAL_APEX.bitMask()), 1.0E-6);
        assertEquals(0.70D, HiveCycleEngine.computeBossCooldownScale(true, 20, HiveCycleMutator.PRIMORDIAL_APEX.bitMask()), 1.0E-6);
        assertEquals(0.80D, HiveCycleEngine.computeBossCooldownScale(true, 20, HiveCycleMutator.HYPHAL_OVERLOAD.bitMask()), 1.0E-6);

        assertEquals(0, HiveCycleEngine.computeExpeditionExtraAttackers(false, 4, threeMutators));
        assertEquals(1, HiveCycleEngine.computeExpeditionExtraAttackers(true, 1, HiveCycleMutator.SYNAPTIC_SWARM.bitMask()));
        assertEquals(2, HiveCycleEngine.computeExpeditionExtraAttackers(true, 3, HiveCycleMutator.SYNAPTIC_SWARM.bitMask()));
        assertEquals(0.45D, HiveCycleEngine.computeExpeditionEliteBonus(true, 10, HiveCycleMutator.SYNAPTIC_SWARM.bitMask()), 1.0E-6);

        assertEquals(1.45D, HiveCycleEngine.computeLearningMultiplier(true, 1, HiveCycleMutator.HYPER_ADAPTATION.bitMask()), 1.0E-6);
        assertEquals(0.85D, HiveCycleEngine.computeThresholdScale(true, HiveCycleMutator.HYPER_ADAPTATION.bitMask()), 1.0E-6);
        assertEquals(1.0D, HiveCycleEngine.computeThresholdScale(true, HiveCycleMutator.PERPETUAL_TIDE.bitMask()), 1.0E-6);

        assertEquals(1.40D, HiveCycleEngine.computeHomeostasisStressMultiplier(true, 1, HiveCycleMutator.ACUTE_HOMEOSTASIS.bitMask()), 1.0E-6);
        assertEquals(1.35D, HiveCycleEngine.computeHyphalPressureMultiplier(true, 1, HiveCycleMutator.HYPHAL_OVERLOAD.bitMask()), 1.0E-6);
        assertEquals(1.50D, HiveCycleEngine.computeTideDurationMultiplier(true, HiveCycleMutator.PERPETUAL_TIDE.bitMask()), 1.0E-6);
        assertEquals(1.0D, HiveCycleEngine.computeTideDurationMultiplier(true, HiveCycleMutator.HYPER_ADAPTATION.bitMask()), 1.0E-6);

        int highScore = HiveCycleEngine.computeCycleScore(3, threeMutators, 0L, 0);
        assertTrue(highScore >= 2600);
        assertEquals("S+", HiveCycleEngine.resolveRankGrade(highScore));
        assertEquals("S", HiveCycleEngine.resolveRankGrade(2000));
        assertEquals("A", HiveCycleEngine.resolveRankGrade(1500));
        assertEquals("B", HiveCycleEngine.resolveRankGrade(1000));
        assertEquals("C", HiveCycleEngine.resolveRankGrade(999));

        int flooredScore = HiveCycleEngine.computeCycleScore(1, HiveCycleMutator.HYPHAL_OVERLOAD.bitMask(), 50_000L, 20);
        assertEquals(HiveCycleEngine.MIN_CYCLE_SCORE, flooredScore);
    }

    @Test
    void codexGatesSupportEngramsAndPostMatriarch() {
        int mask = FieldEngramId.TIDE_PRESSURE.bitMask();
        assertTrue(ProgressionGoalEngine.isCodexGateUnlocked(
                "engram:tide_pressure", 1, false, false, false, mask, false));
        assertFalse(ProgressionGoalEngine.isCodexGateUnlocked(
                "engram:overmind_cycle", 4, true, true, false, mask, true));
        assertFalse(ProgressionGoalEngine.isCodexGateUnlocked(
                "engram:unknown_key", 4, true, true, false, FieldEngramId.ALL_MASK, true));
        assertTrue(ProgressionGoalEngine.isCodexGateUnlocked(
                "engram:unknown_key", 1, false, false, true, 0, false));

        assertFalse(ProgressionGoalEngine.isCodexGateUnlocked(
                "post_matriarch", 4, true, true, false, 0, false));
        assertTrue(ProgressionGoalEngine.isCodexGateUnlocked(
                "post_matriarch", 4, true, false, false, 0, true));
    }

    @Test
    void contextualCombatHudToastsAndUnreadTrackerBehaveDeterministically() {
        assertFalse(MycelialClientConfig.shouldUseCombatCompactHud(true, false, false));
        assertTrue(MycelialClientConfig.shouldUseCombatCompactHud(true, false, true));
        assertFalse(MycelialClientConfig.shouldUseCombatCompactHud(false, false, true));
        assertTrue(MycelialClientConfig.shouldUseCombatCompactHud(false, true, false));

        float[] expired = DiegeticToastManager.computeToastAlphaAndSlide(-1L, 4500);
        assertEquals(0.0F, expired[0], 1.0E-5F);
        float[] fadeIn = DiegeticToastManager.computeToastAlphaAndSlide(160L, 4500);
        assertEquals(0.5F, fadeIn[0], 1.0E-4F);
        assertEquals(7.0F, fadeIn[1], 1.0E-4F);
        float[] hold = DiegeticToastManager.computeToastAlphaAndSlide(2000L, 4500);
        assertEquals(1.0F, hold[0], 1.0E-5F);
        assertEquals(0.0F, hold[1], 1.0E-5F);
        float[] fadeOut = DiegeticToastManager.computeToastAlphaAndSlide(4240L, 4500);
        assertEquals(0.5F, fadeOut[0], 1.0E-4F);
        assertTrue(fadeOut[1] > 0.0F);

        DiegeticToastManager.clear();
        DiegeticToastManager.enqueue(
                DiegeticToastManager.ToastCategory.FIELD_ENGRAM,
                Component.literal("Title"),
                Component.literal("Sub"));
        assertEquals(1, DiegeticToastManager.getPendingCount());
        DiegeticToastManager.clear();
        assertEquals(0, DiegeticToastManager.getPendingCount());

        ClientUnreadCodexTracker.clear();
        ClientUnreadCodexTracker.markUnread("secretos_logros", "engram_outbreak_signal");
        ClientUnreadCodexTracker.markUnread("secretos_logros", "engram_tide_pressure");
        assertTrue(ClientUnreadCodexTracker.hasUnreadInChapter("secretos_logros"));
        assertTrue(ClientUnreadCodexTracker.isEntryUnread("secretos_logros", "engram_outbreak_signal"));
        assertEquals(2, ClientUnreadCodexTracker.getTotalUnreadCount());
        ClientUnreadCodexTracker.markRead("secretos_logros", "engram_outbreak_signal");
        assertFalse(ClientUnreadCodexTracker.isEntryUnread("secretos_logros", "engram_outbreak_signal"));
        assertEquals(1, ClientUnreadCodexTracker.getTotalUnreadCount());
        ClientUnreadCodexTracker.clear();
        assertFalse(ClientUnreadCodexTracker.hasUnreadInChapter("secretos_logros"));
        assertEquals(0, ClientUnreadCodexTracker.getTotalUnreadCount());
    }
}
