package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialRedstoneTelemetry;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
            PurificationZoneTracker.get(serverLevel).addPylon(pos);
            serverLevel.scheduleTick(pos, this, 40);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector.onDefensePylonDisabled(serverLevel, pos);
            com.alex.mycelialmind.world.MycelialChunkTicketManager.syncPylonChunkTicket(serverLevel, pos, false);
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
        if (level.getBlockEntity(pos) instanceof MonumentFocusBlockEntity focus && focus.tryUpgrade(stack, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        boolean wasSmothered = tracker.isPylonSmothered(pos);
        boolean smothered = AntibodyCystBlock.countAdjacentCysts(level, pos) >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
        tracker.setPylonSmothered(pos, smothered);
        if (smothered && !wasSmothered) {
            com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector.onDefensePylonDisabled(level, pos);
        }
        int tier = (level.getBlockEntity(pos) instanceof MonumentFocusBlockEntity focus) ? focus.getTier() : 1;
        if (tracker.getPylonTier(pos) != tier) {
            tracker.setPylonTier(pos, tier);
        }
        if (wasSmothered != smothered) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
            level.playSound(null, pos, ModSounds.PURIFICATION_PYLON_PULSE.get(), SoundSource.BLOCKS, 0.85F, smothered ? 0.65F : 1.25F);
        }
        com.alex.mycelialmind.world.MycelialChunkTicketManager.syncPylonChunkTicket(level, pos, tier >= 3 && !smothered);
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
        int baseRadius = MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
        int radius = (level.getBlockEntity(pos) instanceof MonumentFocusBlockEntity focus)
                ? focus.getEffectiveRadius(baseRadius)
                : baseRadius;
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        HiveProgressionTier effectiveAct = HiveProgressionTier.resolveEffectiveTier(
                hive.getThreatLevel(), hive.getHighestTierReached());
        boolean inRemission = hive.isMatriarchRemissionActive(level.getGameTime());
        double radiusSq = (double) radius * radius;
        AABB box = new AABB(pos).inflate(radius);
        boolean purifiedAny = false;
        float t3PurgeDmg = effectiveAct.pylonTier3PurgeDamage();
        for (MycelialMob mob : level.getEntitiesOfClass(MycelialMob.class, box)) {
            if (mob.blockPosition().distSqr(pos) <= radiusSq) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                if (tier >= 3 && !mob.isAlliedEscort()) {
                    mob.hurt(level.damageSources().magic(), t3PurgeDmg);
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
        level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                2, 0.15, 0.2, 0.15, 0.01);
        level.scheduleTick(pos, this, 40);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        boolean smothered = (level instanceof ServerLevel sl && PurificationZoneTracker.get(sl).isPylonSmothered(pos))
                || AntibodyCystBlock.countAdjacentCysts(level, pos) >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
        int tier = (level.getBlockEntity(pos) instanceof MonumentFocusBlockEntity focus) ? focus.getTier() : 1;
        return MycelialRedstoneTelemetry.pylonSignal(smothered, tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.purification_pylon").withStyle(ChatFormatting.AQUA));
    }
}
