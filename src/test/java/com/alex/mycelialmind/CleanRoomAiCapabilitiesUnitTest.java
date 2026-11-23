package com.alex.mycelialmind;

import com.alex.mycelialmind.hive.MobEpisodicMemory;
import com.alex.mycelialmind.hive.MobThreatTable;
import com.alex.mycelialmind.hive.TacticalEvasionEngine;
import com.alex.mycelialmind.hive.TacticalEvasionEngine.KiteBand;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CleanRoomAiCapabilitiesUnitTest {
    @Test
    void threatTableRanksDamageAppliesHysteresisGrudgeAndPrunes() {
        MobThreatTable table = new MobThreatTable();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        // Target A deals light damage close by; Target B deals heavy damage farther away
        table.recordDamage(a, 2.0F, 9.0D, 1.0F, 100L);
        table.recordDamage(b, 12.0F, 100.0D, 0.9F, 100L);
        UUID chosen = table.selectHighestThreat(a, 105L, id -> true, id -> false);
        assertEquals(b, chosen, "Heavy damage threat must beat light close damage even with current-target hysteresis");

        // Equal base threat: current-target hysteresis (1.15x) prevents ping-pong
        MobThreatTable tied = new MobThreatTable();
        tied.recordDamage(a, 10.0F, 25.0D, 1.0F, 200L);
        tied.recordDamage(b, 10.0F, 25.0D, 1.0F, 200L);
        assertEquals(a, tied.selectHighestThreat(a, 205L, id -> true, id -> false));
        assertEquals(b, tied.selectHighestThreat(b, 205L, id -> true, id -> false));

        // Grudge multiplier (1.25x) overcomes current-target hysteresis (1.15x)
        assertEquals(b, tied.selectHighestThreat(a, 205L, id -> true, id -> id.equals(b)));

        // Expiration & pruning after DEFAULT_MAX_TRACKING_TICKS
        assertNull(tied.selectHighestThreat(a, 200L + MobThreatTable.DEFAULT_MAX_TRACKING_TICKS, id -> true, id -> false));
        assertEquals(0, tied.size());

        // Invalidation removes entry
        table.invalidate(b);
        assertEquals(0.0D, table.getThreatFor(b), 0.0001D);
        assertEquals(1, table.size());
    }

    @Test
    void threatTableEnforcesBoundedCapacity() {
        MobThreatTable table = new MobThreatTable();
        for (int i = 0; i < MobThreatTable.MAX_ENTRIES + 5; i++) {
            table.recordDamage(UUID.randomUUID(), 5.0F + i, 16.0D, 1.0F, 100L + i);
        }
        assertEquals(MobThreatTable.MAX_ENTRIES, table.size());
    }

    @Test
    void episodicMemoryTracksRecencyEvictsOldestAndRoundTripsNbt() {
        MobEpisodicMemory memory = new MobEpisodicMemory();
        UUID attacker = UUID.randomUUID();
        String grudge = MobEpisodicMemory.grudgeKey(attacker);
        assertEquals("grudge:" + attacker, grudge);

        memory.remember(MobEpisodicMemory.KEY_TRAUMA_GRENADE, 100L);
        memory.remember(MobEpisodicMemory.KEY_TRAUMA_GRENADE, 150L);
        assertTrue(memory.hasMemory(MobEpisodicMemory.KEY_TRAUMA_GRENADE));
        assertEquals(2, memory.getMemoryCount(MobEpisodicMemory.KEY_TRAUMA_GRENADE));
        assertEquals(150L, memory.getLastTick(MobEpisodicMemory.KEY_TRAUMA_GRENADE));
        assertTrue(memory.hasRecentMemory(MobEpisodicMemory.KEY_TRAUMA_GRENADE, 200L, 60L));
        assertFalse(memory.hasRecentMemory(MobEpisodicMemory.KEY_TRAUMA_GRENADE, 250L, 60L));

        memory.remember(grudge, 180L);
        CompoundTag tag = new CompoundTag();
        memory.writeToNbt(tag, "MycelialEpisodicMemory");

        MobEpisodicMemory restored = new MobEpisodicMemory();
        restored.readFromNbt(tag, "MycelialEpisodicMemory");
        assertEquals(2, restored.getMemoryCount(MobEpisodicMemory.KEY_TRAUMA_GRENADE));
        assertEquals(150L, restored.getLastTick(MobEpisodicMemory.KEY_TRAUMA_GRENADE));
        assertTrue(restored.hasMemory(grudge));

        // Bounded eviction evicts oldest entry
        for (int i = 0; i < MobEpisodicMemory.MAX_ENTRIES; i++) {
            restored.remember("slot:" + i, 300L + i);
        }
        assertEquals(MobEpisodicMemory.MAX_ENTRIES, restored.size());
        assertFalse(restored.hasMemory(MobEpisodicMemory.KEY_TRAUMA_GRENADE));
    }

    @Test
    void tacticalEvasionEngineHysteresisHazardVectorAndTickIntervalsWork() {
        // Standard band hysteresis avoids jitter at 5.0m (distSqr = 25.0)
        assertEquals(KiteBand.STANDARD, TacticalEvasionEngine.evaluateKiteBand(20.0D, KiteBand.NONE, 0.20F, false));
        assertEquals(KiteBand.STANDARD, TacticalEvasionEngine.evaluateKiteBand(36.0D, KiteBand.STANDARD, 0.30F, false));
        assertEquals(KiteBand.NONE, TacticalEvasionEngine.evaluateKiteBand(55.0D, KiteBand.STANDARD, 0.30F, false));
        assertEquals(KiteBand.NONE, TacticalEvasionEngine.evaluateKiteBand(36.0D, KiteBand.NONE, 0.30F, false));

        // Emergency band hysteresis + trauma extension
        assertEquals(KiteBand.NONE, TacticalEvasionEngine.evaluateKiteBand(26.0D, KiteBand.NONE, 0.10F, false));
        assertEquals(KiteBand.STANDARD, TacticalEvasionEngine.evaluateKiteBand(18.0D, KiteBand.NONE, 0.10F, false));
        assertEquals(KiteBand.EMERGENCY, TacticalEvasionEngine.evaluateKiteBand(18.0D, KiteBand.NONE, 0.10F, true));
        assertEquals(KiteBand.EMERGENCY, TacticalEvasionEngine.evaluateKiteBand(24.0D, KiteBand.EMERGENCY, 0.10F, false));
        assertEquals(KiteBand.STANDARD, TacticalEvasionEngine.evaluateKiteBand(32.0D, KiteBand.EMERGENCY, 0.10F, false));

        assertTrue(TacticalEvasionEngine.kiteSpeedMultiplier(KiteBand.EMERGENCY)
                > TacticalEvasionEngine.kiteSpeedMultiplier(KiteBand.STANDARD));

        // Hazard evade vector steers away from hazard and biases toward safe rear node, ignoring hazard-side node
        Vec3 awayWithSafeNode = TacticalEvasionEngine.computeHazardEvadeVector(10.0D, 0.0D, 0.0D, 0.0D, new BlockPos(20, 64, 10));
        assertTrue(awayWithSafeNode.x > 0.7D && awayWithSafeNode.z > 0.15D);
        Vec3 awayIgnoringUnsafeNode = TacticalEvasionEngine.computeHazardEvadeVector(10.0D, 0.0D, 0.0D, 0.0D, new BlockPos(-10, 64, 10));
        assertEquals(1.0D, awayIgnoringUnsafeNode.x, 0.001D);
        assertEquals(0.0D, awayIgnoringUnsafeNode.z, 0.001D);

        // Adaptive tick intervals
        assertEquals(4, TacticalEvasionEngine.computeAdaptiveAiInterval(true, false, false));
        assertEquals(4, TacticalEvasionEngine.computeAdaptiveAiInterval(false, false, true));
        assertEquals(10, TacticalEvasionEngine.computeAdaptiveAiInterval(false, true, false));
        assertEquals(20, TacticalEvasionEngine.computeAdaptiveAiInterval(false, false, false));
    }
}
