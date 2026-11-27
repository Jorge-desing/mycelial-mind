package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.entity.MycelialBossMob;
import com.alex.mycelialmind.item.FieldEngramItem;
import com.alex.mycelialmind.network.CycleProtocolSyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Server coordinator for Post-Matriarch Hive Cycles and Mutators.
 */
public final class HiveCycleDirector {
    private static final ResourceLocation CYCLE_HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "cycle_boss_health");
    private static final ResourceLocation CYCLE_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "cycle_boss_damage");

    private HiveCycleDirector() {
    }

    public static boolean isCycleUnlocked(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return false;
        }
        return HiveMindSavedData.get(level.getServer().overworld()).isMatriarchDefeated()
                || HiveMindSavedData.get(level).isMatriarchDefeated();
    }

    public static boolean startCycle(@Nullable ServerPlayer actor, ServerLevel level, int mutatorMask) {
        if (level == null || level.getServer() == null) {
            return false;
        }
        if (!isCycleUnlocked(level)) {
            if (actor != null) {
                actor.displayClientMessage(Component.translatable("message.mycelialmind.cycle_locked")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return false;
        }
        if (!HiveCycleEngine.isValidMutatorSelection(mutatorMask)) {
            if (actor != null) {
                actor.displayClientMessage(Component.translatable("message.mycelialmind.cycle_invalid_mutators")
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        ServerLevel overworld = level.getServer().overworld();
        HiveCycleSavedData cycles = HiveCycleSavedData.get(overworld);
        if (cycles.isCycleActive()) {
            if (actor != null) {
                actor.displayClientMessage(Component.translatable("message.mycelialmind.cycle_already_active",
                                cycles.getActiveCycleNumber())
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return false;
        }
        if (!cycles.startCycle(mutatorMask, overworld.getGameTime())) {
            return false;
        }
        clearRemissionAcrossDimensions(level);
        syncHiveModifiersAcrossDimensions(level);

        int cycleNum = cycles.getActiveCycleNumber();
        List<HiveCycleMutator> activeMutators = HiveCycleMutator.decodeMask(cycles.getActiveMutatorMask());
        var mutListComp = Component.empty();
        for (int i = 0; i < activeMutators.size(); i++) {
            if (i > 0) {
                mutListComp.append(", ");
            }
            mutListComp.append(Component.translatable(activeMutators.get(i).nameKey()));
        }

        for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
            sp.sendSystemMessage(Component.translatable("message.mycelialmind.cycle_started",
                    cycleNum, mutListComp).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            sp.serverLevel().playSound(null, sp.blockPosition(), ModSounds.MATRIARCH_ALTAR_AWAKEN.get(),
                    SoundSource.HOSTILE, 0.9F, 1.15F);
            PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(sp.serverLevel(), sp));
        }
        CycleProtocolSyncPayload.syncToAll(level);
        return true;
    }

    public static boolean abortCycle(@Nullable ServerPlayer actor, ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return false;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        if (!cycles.abortCycle()) {
            if (actor != null) {
                actor.displayClientMessage(Component.translatable("message.mycelialmind.cycle_none_active")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        syncHiveModifiersAcrossDimensions(level);
        for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
            sp.sendSystemMessage(Component.translatable("message.mycelialmind.cycle_aborted")
                    .withStyle(ChatFormatting.YELLOW));
        }
        CycleProtocolSyncPayload.syncToAll(level);
        return true;
    }

    public static void onMatriarchDefeated(ServerLevel sourceLevel, BlockPos pos) {
        if (sourceLevel == null || sourceLevel.getServer() == null) {
            return;
        }
        ServerLevel overworld = sourceLevel.getServer().overworld();
        HiveCycleSavedData cycles = HiveCycleSavedData.get(overworld);
        if (!cycles.isCycleActive()) {
            CycleProtocolSyncPayload.syncToAll(sourceLevel);
            return;
        }
        HiveCycleSavedData.CycleCompletionResult result = cycles.completeCycle(overworld.getGameTime());
        syncHiveModifiersAcrossDimensions(sourceLevel);
        if (result == null) {
            return;
        }
        int colonyXpReward = 50 * Math.max(1, result.cycleNumber());
        awardCycleRewardsToColonies(overworld, colonyXpReward);
        if (sourceLevel != overworld) {
            awardCycleRewardsToColonies(sourceLevel, colonyXpReward);
        }

        ItemStack cycleEngram = FieldEngramItem.createStack(FieldEngramId.OVERMIND_CYCLE);
        ItemEntity dropped = new ItemEntity(
                sourceLevel,
                pos.getX() + 0.5D,
                pos.getY() + 0.8D,
                pos.getZ() + 0.5D,
                cycleEngram
        );
        sourceLevel.addFreshEntity(dropped);
        sourceLevel.sendParticles(ModParticles.HIVE_PULSE.get(),
                pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);

        String messageKey = result.newRecord()
                ? "message.mycelialmind.cycle_completed_record"
                : "message.mycelialmind.cycle_completed";
        for (ServerPlayer sp : sourceLevel.getServer().getPlayerList().getPlayers()) {
            sp.awardStat(com.alex.mycelialmind.registry.ModStats.CYCLES_COMPLETED.get());
            sp.sendSystemMessage(Component.translatable(
                    messageKey,
                    result.cycleNumber(),
                    result.score(),
                    result.grade()
            ).withStyle(result.newRecord() ? ChatFormatting.GOLD : ChatFormatting.GREEN, ChatFormatting.BOLD));
            sp.serverLevel().playSound(null, sp.blockPosition(), ModSounds.HIVE_SYNERGY_UNLOCK.get(),
                    SoundSource.PLAYERS, 1.0F, 1.15F);
        }
        CycleProtocolSyncPayload.syncToAll(sourceLevel);
    }

    public static void applyBossCycleScaling(MycelialBossMob boss, ServerLevel level) {
        if (boss == null || level == null || level.getServer() == null) {
            return;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        boolean active = cycles.isCycleActive();
        int cycleNum = cycles.getActiveCycleNumber();
        int mask = cycles.getActiveMutatorMask();

        double hpMult = HiveCycleEngine.computeBossHealthMultiplier(active, cycleNum, mask);
        double dmgMult = HiveCycleEngine.computeBossDamageMultiplier(active, cycleNum, mask);

        applyMultiplierModifier(boss.getAttribute(Attributes.MAX_HEALTH), CYCLE_HEALTH_MODIFIER_ID, hpMult - 1.0D);
        applyMultiplierModifier(boss.getAttribute(Attributes.ATTACK_DAMAGE), CYCLE_DAMAGE_MODIFIER_ID, dmgMult - 1.0D);
        boss.setHealth(boss.getMaxHealth());
    }

    public static double getActiveBossCooldownScale(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return 1.0D;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        return HiveCycleEngine.computeBossCooldownScale(
                cycles.isCycleActive(), cycles.getActiveCycleNumber(), cycles.getActiveMutatorMask());
    }

    public static double getActiveLearningMultiplier(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return 1.0D;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        return HiveCycleEngine.computeLearningMultiplier(
                cycles.isCycleActive(), cycles.getActiveCycleNumber(), cycles.getActiveMutatorMask());
    }

    public static double getActiveThresholdScale(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return 1.0D;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        return HiveCycleEngine.computeThresholdScale(cycles.isCycleActive(), cycles.getActiveMutatorMask());
    }

    public static double getActiveHomeostasisStressMultiplier(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return 1.0D;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        return HiveCycleEngine.computeHomeostasisStressMultiplier(
                cycles.isCycleActive(), cycles.getActiveCycleNumber(), cycles.getActiveMutatorMask());
    }

    public static double getActiveHyphalPressureMultiplier(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return 1.0D;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        return HiveCycleEngine.computeHyphalPressureMultiplier(
                cycles.isCycleActive(), cycles.getActiveCycleNumber(), cycles.getActiveMutatorMask());
    }

    public static double getActiveTideDurationMultiplier(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return 1.0D;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        return HiveCycleEngine.computeTideDurationMultiplier(cycles.isCycleActive(), cycles.getActiveMutatorMask());
    }

    public static int getActiveExpeditionExtraAttackers(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return 0;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        return HiveCycleEngine.computeExpeditionExtraAttackers(
                cycles.isCycleActive(), cycles.getActiveCycleNumber(), cycles.getActiveMutatorMask());
    }

    public static double getActiveExpeditionEliteBonus(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return 0.0D;
        }
        HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
        return HiveCycleEngine.computeExpeditionEliteBonus(
                cycles.isCycleActive(), cycles.getActiveCycleNumber(), cycles.getActiveMutatorMask());
    }

    public static void syncHiveModifiersAcrossDimensions(ServerLevel sourceLevel) {
        if (sourceLevel == null || sourceLevel.getServer() == null) {
            return;
        }
        double learnMult = getActiveLearningMultiplier(sourceLevel);
        double threshScale = getActiveThresholdScale(sourceLevel);
        ServerLevel overworld = sourceLevel.getServer().overworld();
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        owHive.setCycleModifiers(learnMult, threshScale, overworld.getGameTime());

        ServerLevel deep = sourceLevel.getServer().getLevel(ModDimensions.DEEP_MYCELIUM_LEVEL);
        if (deep != null) {
            HiveMindSavedData deepHive = HiveMindSavedData.get(deep);
            deepHive.setCycleModifiers(learnMult, threshScale, deep.getGameTime());
        }
    }

    private static void clearRemissionAcrossDimensions(ServerLevel sourceLevel) {
        var server = sourceLevel.getServer();
        ServerLevel overworld = server.overworld();
        HiveMindSavedData.get(overworld).clearMatriarchRemission();
        ServerLevel deep = server.getLevel(ModDimensions.DEEP_MYCELIUM_LEVEL);
        if (deep != null) {
            HiveMindSavedData.get(deep).clearMatriarchRemission();
        }
    }

    private static void applyMultiplierModifier(
            @Nullable AttributeInstance instance,
            ResourceLocation id,
            double bonus
    ) {
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (bonus > 0.0001D) {
            instance.addPermanentModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel level) {
            syncHiveModifiersAcrossDimensions(level);
            CycleProtocolSyncPayload.syncToPlayer(serverPlayer, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel level) {
            syncHiveModifiersAcrossDimensions(level);
            CycleProtocolSyncPayload.syncToPlayer(serverPlayer, false);
        }
    }

    private static void awardCycleRewardsToColonies(ServerLevel colonyLevel, int xp) {
        SovereignColonySavedData colonySaved = SovereignColonySavedData.get(colonyLevel);
        if (colonySaved.getAllColonies().isEmpty()) {
            return;
        }
        for (SovereignColonyData colony : colonySaved.getAllColonies()) {
            colony.addDomainXp(xp);
            colony.addBiomass(16);
            colony.recalculateMorale();
        }
        colonySaved.setDirty();
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel level) {
            HiveCycleSavedData cycles = HiveCycleSavedData.get(level);
            if (cycles.isCycleActive()) {
                cycles.recordDeath();
                CycleProtocolSyncPayload.syncToAll(level);
            }
        }
    }
}
