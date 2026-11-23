package com.alex.mycelialmind.test;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionData;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionRules;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import com.alex.mycelialmind.network.PostMatriarchExpeditionSyncPayload;
import com.alex.mycelialmind.network.EnclaveDirectoryPayload;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.UUID;

@GameTestHolder(MycelialMind.MODID)
@PrefixGameTestTemplate(false)
public final class PostMatriarchExpeditionGameTests {
    private PostMatriarchExpeditionGameTests() {
    }

    @GameTest(template = "empty")
    public static void expeditionClientCacheClearsBetweenWorlds(GameTestHelper helper) {
        var pos = new BlockPos(12, 50, -32);
        com.alex.mycelialmind.network.ClientExpeditionCache.update(new PostMatriarchExpeditionSyncPayload(
                true, true, pos.asLong(), 1, 2, 1, 0, 50, 1, pos, true, 2,
                List.of(new PostMatriarchExpeditionSyncPayload.Marker(pos, 1, 2, 0, true))));
        com.alex.mycelialmind.network.ClientExpeditionCache.updateDirectory(new EnclaveDirectoryPayload(
                1, 7, List.of(new EnclaveDirectoryPayload.Entry(pos, 1, 2, "Route")), pos));
        com.alex.mycelialmind.network.ClientExpeditionCache.clear();
        helper.assertTrue(!com.alex.mycelialmind.network.ClientExpeditionCache.isUnlocked()
                && !com.alex.mycelialmind.network.ClientExpeditionCache.hasActiveExpedition()
                && com.alex.mycelialmind.network.ClientExpeditionCache.getEnclaves().isEmpty()
                && com.alex.mycelialmind.network.ClientExpeditionCache.getDirectory().pinned() == null
                && com.alex.mycelialmind.network.ClientExpeditionCache.getDirectory().entries().isEmpty(),
                "Leaving a world clears its expedition, markers and personal destination");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void personalDestinationsAndDirectoryPacketSurviveReload(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        BlockPos first = new BlockPos(-1234, -24, 7654);
        BlockPos second = new BlockPos(8042, 81, -2400);
        PlayerDiscoveryData one = new PlayerDiscoveryData();
        PlayerDiscoveryData two = new PlayerDiscoveryData();
        one.setPinnedEnclave(first);
        two.setPinnedEnclave(second);
        PlayerDiscoveryData restoredOne = new PlayerDiscoveryData();
        PlayerDiscoveryData restoredTwo = new PlayerDiscoveryData();
        restoredOne.deserializeNBT(registries, one.serializeNBT(registries));
        restoredTwo.deserializeNBT(registries, two.serializeNBT(registries));
        helper.assertTrue(first.equals(restoredOne.getPinnedEnclave()) && second.equals(restoredTwo.getPinnedEnclave()),
                "Each player keeps a separate destination after reload");
        var entries = List.of(new EnclaveDirectoryPayload.Entry(first, 1, 0, "Ruta a la Ciudadela"),
                new EnclaveDirectoryPayload.Entry(second, 2, 5, "Jardín"));
        var original = new EnclaveDirectoryPayload(3, 20, entries, second);
        var buffer = Unpooled.buffer();
        try {
            EnclaveDirectoryPayload.STREAM_CODEC.encode(buffer, original);
            helper.assertTrue(original.equals(EnclaveDirectoryPayload.STREAM_CODEC.decode(buffer)),
                    "Page, accented names, coordinates and the personal destination survive the packet");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void benchKeepsPartialOutputWhenInputsAreRefilled(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());
        SynapticBenchBlockEntity bench = (SynapticBenchBlockEntity) helper.getBlockEntity(local);
        ItemStack red = new ItemStack(ModItems.RECLAIMED_BLOOM_MELEE.get());
        ItemStack blue = new ItemStack(ModItems.RECLAIMED_BLOOM_PROJECTILE.get());
        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, red.copy());
        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
        helper.assertTrue(ItemStack.isSameItemSameComponents(bench.extractOutputForAutomation(1, false), red), "First flower is red");
        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, blue.copy());
        bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
        CompoundTag saved = bench.saveWithoutMetadata(helper.getLevel().registryAccess());
        SynapticBenchBlockEntity restored = new SynapticBenchBlockEntity(bench.getBlockPos(), bench.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.handleUpdateTag(saved, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.isSameItemSameComponents(restored.extractOutputForAutomation(1, false), red),
                "Refilling inputs cannot replace the remaining red flower");
        ItemStack next = restored.extractOutputForAutomation(2, false);
        helper.assertTrue(ItemStack.isSameItemSameComponents(next, blue) && next.getCount() == 2,
                "The next recipe starts after the committed output is fully extracted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void expeditionTelemetryPacketPreservesSiteConditions(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var marker = new PostMatriarchExpeditionSyncPayload.Marker(pos, 1, 2, 0, true);
        int flags = PostMatriarchExpeditionSyncPayload.TIDE | PostMatriarchExpeditionSyncPayload.PYLON_SMOTHERED;
        var original = new PostMatriarchExpeditionSyncPayload(true, true, pos.asLong(), 1, 2, 1, 0, 87, flags,
                pos.offset(3, 0, 0), true, 2, List.of(marker));
        var buffer = Unpooled.buffer();
        try {
            PostMatriarchExpeditionSyncPayload.STREAM_CODEC.encode(buffer, original);
            var decoded = PostMatriarchExpeditionSyncPayload.STREAM_CODEC.decode(buffer);
            helper.assertTrue(decoded.equals(original), "Site conditions and protected marker survive packet encoding");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void abandonedExpeditionCanBeReplacedWithoutStaleState(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.load(new CompoundTag(), registries);
        BlockPos first = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos second = helper.absolutePos(new BlockPos(2, 1, 1));
        data.start(first, PostMatriarchExpeditionData.Outcome.ROUTE, 2);
        data.beginPulse(second, List.of(UUID.randomUUID()), 100);
        helper.assertTrue(!data.abandon(second) && data.isPulseInProgress(), "A stale request cannot abandon another destination");
        helper.assertTrue(data.abandon(first), "The selected expedition can be cleared");
        PostMatriarchExpeditionData reloaded = PostMatriarchExpeditionData.load(data.save(new CompoundTag(), registries), registries);
        helper.assertTrue(!reloaded.hasActiveExpedition() && !reloaded.isPulseInProgress()
                && reloaded.getPulseAttackers().isEmpty(), "Abandonment survives reload and clears pulse state");
        helper.assertTrue(reloaded.start(second, PostMatriarchExpeditionData.Outcome.GARDEN, 0),
                "A new destination can be chosen after abandonment");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void enclaveDirectoryPreservesMoreThanOneNetworkPage(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = new CompoundTag();
        ListTag enclaves = new ListTag();
        for (int i = 0; i < 140; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("ActivePos", new BlockPos(i, 64, 0).asLong());
            entry.putInt("Outcome", 1);
            entry.putInt("Palette", 0);
            enclaves.add(entry);
        }
        saved.put("Enclaves", enclaves);
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.load(saved, registries);
        helper.assertTrue(data.getEnclaves().size() == 140, "All saved enclaves must load");
        helper.assertTrue(data.renameEnclave(new BlockPos(0, 64, 0), "Camino al portal"), "A shared name can be set");
        helper.assertTrue(!data.renameEnclave(new BlockPos(0, 64, 0), "x".repeat(33)), "Overlong names are rejected");
        PostMatriarchExpeditionData restored = PostMatriarchExpeditionData.load(data.save(new CompoundTag(), registries), registries);
        helper.assertTrue(restored.getEnclaves().size() == 140
                && restored.getEnclaves().get(0).name().equals("Camino al portal"),
                "Names and all enclaves survive reload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void benchReproducesAllBloomColorsThroughAutomation(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());
        SynapticBenchBlockEntity bench = (SynapticBenchBlockEntity) helper.getBlockEntity(local);
        ItemStack[] blooms = {
                new ItemStack(ModBlocks.RECLAIMED_BLOOM.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_MELEE.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_PROJECTILE.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_FIRE.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_EXPLOSION.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_MAGIC.get())
        };
        bench.setLabTier(3);
        for (ItemStack bloom : blooms) {
            helper.assertTrue(SynapticBenchBlockEntity.isValidSlotInput(SynapticBenchBlockEntity.SLOT_SAMPLE, bloom),
                    "Each bloom must be accepted in the sample slot");
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE, bloom.copy());
            bench.getInventory().setStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS, new ItemStack(ModItems.MYCELIAL_BIOMASS.get()));
            ItemStack preview = bench.extractOutputForAutomation(2, true);
            helper.assertTrue(ItemStack.isSameItemSameComponents(preview, bloom) && preview.getCount() == 2,
                    "Automation preview preserves the bloom variant");
            helper.assertTrue(!bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS).isEmpty(),
                    "Simulation cannot consume biomass");
            ItemStack first = bench.extractOutputForAutomation(1, false);
            ItemStack second = bench.extractOutputForAutomation(1, false);
            helper.assertTrue(ItemStack.isSameItemSameComponents(first, bloom)
                    && ItemStack.isSameItemSameComponents(second, bloom), "Both extracted flowers keep their color");
            helper.assertTrue(bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_SAMPLE).isEmpty()
                    && bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_BIOMASS).isEmpty()
                    && bench.extractOutputForAutomation(1, false).isEmpty(), "One recipe consumes one flower and biomass once");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pulseSurvivesReloadAndRequiresEveryAttacker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.load(new CompoundTag(), level.registryAccess());
        BlockPos node = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos pylon = helper.absolutePos(new BlockPos(3, 2, 1));
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        helper.assertTrue(data.start(node, PostMatriarchExpeditionData.Outcome.ROUTE, 0), "Start route");
        helper.assertTrue(data.beginPulse(pylon, List.of(first, second), 100), "Start pulse");
        helper.assertTrue(!data.bindDefensePylon(pylon.offset(1, 0, 0)) && pylon.equals(data.getDefensePylon()),
                "Another player cannot switch the defended pylon during a pulse");
        helper.assertTrue(!data.beginPulse(pylon, List.of(first), 100), "A second player cannot start the same pulse twice");
        helper.assertTrue(data.getPulsesCompleted() == 0 && !data.finishPulse(101), "Starting does not complete a pulse");
        PostMatriarchExpeditionData restored = PostMatriarchExpeditionData.load(
                data.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        helper.assertTrue(restored.isPulseInProgress() && pylon.equals(restored.getDefensePylon())
                && restored.getPulseAttackers().size() == 2, "Pending attackers and pylon survive reload");
        helper.assertTrue(restored.resolveAttacker(first) && !restored.finishPulse(102), "One attacker remains");
        helper.assertTrue(!restored.resolveAttacker(first), "A death cannot be counted twice");
        helper.assertTrue(restored.resolveAttacker(second) && restored.finishPulse(103), "Both attackers resolved");
        helper.assertTrue(restored.getPulsesCompleted() == 1 && !restored.finishPulse(104), "Pulse recorded exactly once");
        restored.recordPulse(200);
        restored.recordPulse(300);
        helper.assertTrue(restored.complete() != null, "The route can be completed after three defended pulses");
        PostMatriarchExpeditionData completed = PostMatriarchExpeditionData.load(
                restored.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        helper.assertTrue(pylon.equals(completed.getEnclaves().get(0).pylon()), "The route's pylon survives reload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void interruptedPulseKeepsOnlyDefendedProgress(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.load(new CompoundTag(), registries);
        BlockPos node = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos pylon = helper.absolutePos(new BlockPos(3, 1, 1));
        UUID attacker = UUID.randomUUID();
        data.start(node, PostMatriarchExpeditionData.Outcome.ROUTE, 0);
        data.recordPulse(50);
        helper.assertTrue(data.beginPulse(pylon, List.of(attacker), 100), "Start the second pulse");
        helper.assertTrue(data.interruptPulse().equals(List.of(attacker)), "The interrupted attackers are returned for cleanup");
        var restored = PostMatriarchExpeditionData.load(data.save(new CompoundTag(), registries), registries);
        helper.assertTrue(restored.getPulsesCompleted() == 1 && !restored.isPulseInProgress()
                && restored.getPulseAttackers().isEmpty(), "Only the defended pulse survives interruption and reload");
        helper.assertTrue(restored.consumeRetiredAttacker(attacker), "An unloaded attacker remains scheduled for cleanup after reload");
        helper.assertTrue(!restored.consumeRetiredAttacker(attacker), "Cleanup is consumed only once");
        BlockPos rebuiltPylon = pylon.offset(1, 0, 0);
        helper.assertTrue(restored.bindDefensePylon(rebuiltPylon), "A rebuilt pylon can be bound after interruption");
        helper.assertTrue(restored.beginPulse(rebuiltPylon, List.of(UUID.randomUUID()), 200), "The interrupted pulse can be retried");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void expeditionProgressAndRecoveredEnclavesSurviveSaveReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.load(new CompoundTag(), level.registryAccess());
        CompoundTag preExpeditionSave = new CompoundTag();
        preExpeditionSave.putBoolean("MatriarchDefeated", true);
        HiveMindSavedData legacyHive = HiveMindSavedData.load(preExpeditionSave, level.registryAccess());
        helper.assertTrue(PostMatriarchExpeditionRules.isUnlocked(legacyHive),
                "A legacy world with the saved Matriarch victory flag should unlock expeditions without new tags.");
        BlockPos routePos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos gardenPos = helper.absolutePos(new BlockPos(6, 2, 1));

        helper.assertTrue(data.start(routePos, PostMatriarchExpeditionData.Outcome.ROUTE,
                        PostMatriarchExpeditionRules.PROJECTILE_PALETTE),
                "A route expedition should start at a new enclave.");
        data.recordPulse(100L);
        CompoundTag activeSnapshot = data.save(new CompoundTag(), level.registryAccess());
        PostMatriarchExpeditionData reloaded = PostMatriarchExpeditionData.load(activeSnapshot, level.registryAccess());
        helper.assertTrue(reloaded.hasActiveExpedition()
                        && reloaded.getActivePos().equals(routePos)
                        && reloaded.getPulsesCompleted() == 1,
                "The active destination and completed pulse should survive a save and reload.");
        reloaded.recordPulse(200L);
        reloaded.recordPulse(300L);
        helper.assertTrue(reloaded.complete() != null,
                "A route should complete after its three persisted pulses.");

        helper.assertTrue(reloaded.start(gardenPos, PostMatriarchExpeditionData.Outcome.GARDEN,
                        PostMatriarchExpeditionRules.MAGIC_PALETTE),
                "A second destination should be available after the first enclave is recovered.");
        BlockPos firstFocus = gardenPos.offset(1, 0, 0);
        BlockPos secondFocus = gardenPos.offset(2, 0, 0);
        BlockPos thirdFocus = gardenPos.offset(3, 0, 0);
        helper.assertTrue(reloaded.addFlora(firstFocus), "The first flora focus should link.");
        helper.assertFalse(reloaded.addFlora(firstFocus),
                "Two participants scanning the same focus should not count it twice.");
        helper.assertTrue(reloaded.addFlora(secondFocus), "The second flora focus should link.");
        CompoundTag gardenSnapshot = reloaded.save(new CompoundTag(), level.registryAccess());
        PostMatriarchExpeditionData reconnected = PostMatriarchExpeditionData.load(gardenSnapshot, level.registryAccess());
        helper.assertTrue(reconnected.hasActiveExpedition() && reconnected.getLinkedFlora().size() == 2,
                "A reconnect during the garden should resume at the last linked flora focus.");
        helper.assertTrue(reconnected.complete() == null,
                "A garden with fewer than three linked flora foci must remain incomplete.");
        helper.assertTrue(reconnected.addFlora(thirdFocus), "The third flora focus should link.");
        helper.assertTrue(reconnected.complete() != null,
                "The garden should complete after three linked flora foci.");

        CompoundTag completedSnapshot = reconnected.save(new CompoundTag(), level.registryAccess());
        PostMatriarchExpeditionData completed = PostMatriarchExpeditionData.load(completedSnapshot, level.registryAccess());
        helper.assertTrue(!completed.hasActiveExpedition() && completed.getEnclaves().size() == 2,
                "Both recovered outcomes should be visible after a world reload.");
        helper.assertTrue(completed.hasCompletedEnclave(routePos) && completed.hasCompletedEnclave(gardenPos),
                "Recovered positions should remain reserved as enclave markers.");
        helper.succeed();
    }
}
