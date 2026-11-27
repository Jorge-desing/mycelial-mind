package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.AcquisitionDifficultyEngine;
import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDamageTypes;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.ClientTooltipHelper;
import com.alex.mycelialmind.util.MycelialRedstoneTelemetry;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.MycelialChunkTicketManager;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Defensive containment pylon that halts mycelial spread and weakens adaptive organisms within its radius.
 */
public class PurificationPylonBlock extends Block implements EntityBlock {
    public PurificationPylonBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MonumentFocusBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            tracker.addPylon(pos);
            syncAdjacentArchitectureBonus(serverLevel, pos, tracker);
            serverLevel.scheduleTick(pos, this, 40);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            syncAdjacentArchitectureBonus(serverLevel, pos, PurificationZoneTracker.get(serverLevel));
        }
    }

    public static int syncAdjacentArchitectureBonus(ServerLevel level, BlockPos pos, PurificationZoneTracker tracker) {
        boolean hasAmber = LuminescentAmberVeinBlock.hasAdjacentConduit(level, pos);
        int barriers = countAdjacentBarriers(level, pos);
        int bonus = (hasAmber ? EcosystemPurposeEngine.AMBER_PYLON_BONUS_RADIUS : 0)
                + (barriers >= EcosystemPurposeEngine.BARRIER_PYLON_REQUIRED_FACES
                ? EcosystemPurposeEngine.BARRIER_PYLON_BONUS_RADIUS : 0);
        tracker.setPylonBonusRadius(pos, bonus);
        return bonus;
    }

    private static int countAdjacentBarriers(Level level, BlockPos pos) {
        Block barrier = ModBlocks.REINFORCED_BIO_BARRIER.get();
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(barrier)) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            PostMatriarchExpeditionDirector.onDefensePylonDisabled(serverLevel, pos);
            MycelialChunkTicketManager.syncPylonChunkTicket(serverLevel, pos, false);
            PurificationZoneTracker.get(serverLevel).removePylon(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.is(ModItems.BIO_SCANNER.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof MonumentFocusBlockEntity focus) {
            if (focus.resolveNextTierForCatalyst(stack) > 0) {
                if (focus.tryUpgrade(stack, player)) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
                if (level instanceof ServerLevel) {
                    return ItemInteractionResult.FAIL;
                }
            }
            if (focus.tryRecharge(stack, player)) {
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, pos, ModSounds.PURIFICATION_PYLON_PULSE.get(),
                            SoundSource.BLOCKS, 0.85F, 1.3F);
                    serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            10, 0.35, 0.35, 0.35, 0.03);
                    player.displayClientMessage(Component.translatable(
                            "message.mycelialmind.pylon_recharged",
                            focus.getPurificationCharge(),
                            AcquisitionDifficultyEngine.PYLON_MAX_CHARGE)
                            .withStyle(ChatFormatting.AQUA), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        if (stack.is(ModItems.HIVE_HEART.get())) {
            if (level instanceof ServerLevel serverLevel) {
                triggerHiveHeartOverload(serverLevel, pos, player, stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.is(ModItems.OVERMIND_ESSENCE.get())) {
            if (level instanceof ServerLevel serverLevel) {
                triggerOvermindPylonSedation(serverLevel, pos, player, stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    public static void triggerHiveHeartOverload(ServerLevel serverLevel, BlockPos pos, Player player, ItemStack stack) {
        int r = EcosystemPurposeEngine.HIVE_HEART_CYST_PURGE_RADIUS;
        int purgedCysts = 0;
        int unjammedTurrets = 0;
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-r, -6, -r), pos.offset(r, 6, r))) {
            if (scan.distSqr(pos) > (double) r * r) {
                continue;
            }
            BlockState st = serverLevel.getBlockState(scan);
            if (st.is(ModBlocks.ANTIBODY_CYST.get())) {
                serverLevel.removeBlock(scan, false);
                purgedCysts++;
            } else if (serverLevel.getBlockEntity(scan) instanceof PurificationTurretBlockEntity turret && turret.isJammed()) {
                turret.clearCorrosiveJam();
                unjammedTurrets++;
            }
        }
        AABB box = new AABB(pos).inflate(EcosystemPurposeEngine.HIVE_HEART_PULSE_RADIUS);
        for (PurifierDroneEntity drone : serverLevel.getEntitiesOfClass(PurifierDroneEntity.class, box)) {
            drone.clearJam();
            drone.heal(20.0F);
        }
        for (MycelialChimeraEntity chimera : serverLevel.getEntitiesOfClass(MycelialChimeraEntity.class, box)) {
            chimera.heal(20.0F);
        }
        HiveMindSavedData.get(serverLevel).reduceThreat(EcosystemPurposeEngine.HIVE_HEART_THREAT_CALM);
        if (DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
            DimensionalHomeostasisDirector.sedateArea(
                    serverLevel, pos, 1, EcosystemPurposeEngine.HIVE_HEART_HOMEOSTASIS_CALM, 1,
                    player instanceof ServerPlayer sp ? sp : null);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5,
                20, 0.8, 0.6, 0.8, 0.05);
        serverLevel.sendParticles(ModParticles.HIVE_PULSE.get(),
                pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.playSound(null, pos, ModSounds.PURIFICATION_PYLON_PULSE.get(), SoundSource.BLOCKS, 1.0F, 1.25F);
        player.displayClientMessage(Component.translatable(
                "message.mycelialmind.pylon_heart_pulse", purgedCysts + unjammedTurrets).withStyle(ChatFormatting.AQUA), true);
    }

    public static void triggerOvermindPylonSedation(ServerLevel serverLevel, BlockPos pos, Player player, ItemStack stack) {
        HiveMindSavedData.get(serverLevel).reduceThreat(EcosystemPurposeEngine.OVERMIND_ESSENCE_THREAT_CALM);
        if (DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
            DimensionalHomeostasisDirector.sedateArea(
                    serverLevel, pos, 1, EcosystemPurposeEngine.OVERMIND_ESSENCE_HOMEOSTASIS_CALM, 1,
                    player instanceof ServerPlayer sp ? sp : null);
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        player.addEffect(new MobEffectInstance(ModEffects.SYNAPTIC_INSIGHT,
                EcosystemPurposeEngine.OVERMIND_ESSENCE_INSIGHT_TICKS, 0));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverLevel.sendParticles(ModParticles.SPORE_FALL.get(),
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                24, 1.2, 0.6, 1.2, 0.04);
        serverLevel.playSound(null, pos, ModSounds.PURIFICATION_PYLON_PULSE.get(), SoundSource.BLOCKS, 1.0F, 0.9F);
        player.displayClientMessage(Component.translatable("message.mycelialmind.pylon_overmind_sedation")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        boolean wasSmothered = tracker.isPylonSmothered(pos);
        boolean smothered = AntibodyCystBlock.countAdjacentCysts(level, pos) >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
        tracker.setPylonSmothered(pos, smothered);
        if (smothered && !wasSmothered) {
            PostMatriarchExpeditionDirector.onDefensePylonDisabled(level, pos);
        }
        MonumentFocusBlockEntity focus = (level.getBlockEntity(pos) instanceof MonumentFocusBlockEntity mf) ? mf : null;
        int tier = focus != null ? focus.getTier() : 1;
        boolean depleted = focus != null && focus.isDepleted();
        boolean wasDepleted = tracker.isPylonDepleted(pos);
        tracker.setPylonDepleted(pos, depleted);
        if (depleted && !wasDepleted && !smothered && !wasSmothered) {
            PostMatriarchExpeditionDirector.onDefensePylonDisabled(level, pos);
        }
        if (tracker.getPylonTier(pos) != tier) {
            tracker.setPylonTier(pos, tier);
        }
        if (wasSmothered != smothered || wasDepleted != depleted) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
            level.playSound(null, pos, ModSounds.PURIFICATION_PYLON_PULSE.get(), SoundSource.BLOCKS, 0.85F, (smothered || depleted) ? 0.65F : 1.25F);
        }
        MycelialChunkTicketManager.syncPylonChunkTicket(level, pos, tier >= 3 && !smothered && !depleted);
        if (smothered) {
            level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    3, 0.25, 0.25, 0.25, 0.01);
            level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                    pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                    3, 0.25, 0.25, 0.25, 0.01);
            level.scheduleTick(pos, this, 40);
            return;
        }
        if (depleted) {
            level.sendParticles(ModParticles.SPORE_FALL.get(),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    4, 0.25, 0.2, 0.25, 0.01);
            level.scheduleTick(pos, this, 40);
            return;
        }
        int baseRadius = MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
        int tierRadius = focus != null
                ? focus.getEffectiveRadius(baseRadius)
                : baseRadius;
        int bonusRadius = syncAdjacentArchitectureBonus(level, pos, tracker);
        boolean hasAmberConduit = LuminescentAmberVeinBlock.hasAdjacentConduit(level, pos);
        int radius = EcosystemPurposeEngine.computePylonEffectiveRadius(
                tierRadius,
                hasAmberConduit,
                countAdjacentBarriers(level, pos)
        );
        if (bonusRadius > 0 && radius < tierRadius + bonusRadius) {
            radius = tierRadius + bonusRadius;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        HiveProgressionTier effectiveAct = HiveProgressionTier.resolveEffectiveTier(
                hive.getThreatLevel(), hive.getHighestTierReached());
        boolean inRemission = hive.isMatriarchRemissionActive(level.getGameTime());
        double radiusSq = (double) radius * radius;
        AABB box = new AABB(pos).inflate(radius);
        boolean purifiedAny = false;
        float t3PurgeDmg = effectiveAct.pylonTier3PurgeDamage();
        for (MycelialMob mob : level.getEntitiesOfClass(MycelialMob.class, box,
                m -> m.isAlive() && !m.isAlliedEscort())) {
            if (mob.blockPosition().distSqr(pos) <= radiusSq) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                if (tier >= 3) {
                    mob.hurt(ModDamageTypes.source(level, ModDamageTypes.PURIFICATION_PURGE), t3PurgeDmg);
                }
                purifiedAny = true;
            }
        }
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (player.blockPosition().distSqr(pos) <= radiusSq) {
                if (player.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE)) {
                    purifiedAny = true;
                }
                if (tier >= 2) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, false));
                }
                if (tier >= 3) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false));
                    if (effectiveAct.level() >= HiveProgressionTier.ADAPTIVE.level()) {
                        player.addEffect(new MobEffectInstance(ModEffects.SYNAPTIC_INSIGHT, 80, 0, false, false));
                    }
                }
            }
        }
        if (inRemission && tier >= 2 && DimensionalHomeostasisDirector.isHomeostasisDimension(level)) {
            DimensionalHomeostasisSavedData.get(level).sedateArea(
                    new ChunkPos(pos), 0, 0.5, 0, level.getGameTime());
        }
        if (purifiedAny) {
            level.playSound(null, pos, ModSounds.PURIFICATION_PYLON_PULSE.get(), SoundSource.BLOCKS, 0.6F, 1.05F);
        }
        if (focus != null) {
            int drain = AcquisitionDifficultyEngine.computePylonDrain(
                    tier, purifiedAny, DimensionalHomeostasisDirector.isHomeostasisDimension(level), hasAmberConduit);
            if (drain > 0) {
                focus.consumePurificationCharge(drain);
                if (focus.isDepleted()) {
                    tracker.setPylonDepleted(pos, true);
                    level.updateNeighbourForOutputSignal(pos, state.getBlock());
                    PostMatriarchExpeditionDirector.onDefensePylonDisabled(level, pos);
                    MycelialChunkTicketManager.syncPylonChunkTicket(level, pos, false);
                }
            }
        }
        level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                2, 0.15, 0.2, 0.15, 0.01);
        if (focus != null && focus.getPurificationCharge() > 0 && focus.getPurificationCharge() <= 20) {
            level.sendParticles(ModParticles.HIVE_EMBER.get(),
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    3, 0.2, 0.2, 0.2, 0.015);
        }
        level.scheduleTick(pos, this, 40);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        boolean inactive = (level instanceof ServerLevel sl && (PurificationZoneTracker.get(sl).isPylonSmothered(pos)
                || PurificationZoneTracker.get(sl).isPylonDepleted(pos)))
                || AntibodyCystBlock.countAdjacentCysts(level, pos) >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
        int tier = (level.getBlockEntity(pos) instanceof MonumentFocusBlockEntity focus) ? focus.getTier() : 1;
        return MycelialRedstoneTelemetry.pylonSignal(inactive, tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.purification_pylon").withStyle(ChatFormatting.AQUA));
        ClientTooltipHelper.appendShiftDetails(tooltip,
                "tooltip.mycelialmind.block.purification_pylon.shift_gate",
                "tooltip.mycelialmind.block.purification_pylon.shift_upkeep");
    }
}
