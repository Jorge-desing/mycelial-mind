package com.alex.mycelialmind.block;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SporeGliderEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SinkholeEcosystemEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Vertical thermal-spore chimney vent anchored in the Root Cavern of a Mycelial Sinkhole.
 * Lifts entities through the Twilight Throat, emits passive Hive threat while unsealed,
 * and stabilizes into a clean bioluminescent elevator when purified by a Purification Pylon.
 */
public class SporeUpdraftVentBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final int TICK_INTERVAL = 4;

    public SporeUpdraftVentBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, TICK_INTERVAL);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide() && state.getValue(ACTIVE) && entity instanceof LivingEntity living && !living.isShiftKeyDown()) {
            Vec3 dm = living.getDeltaMovement();
            if (dm.y < 0.25D) {
                living.setDeltaMovement(dm.x, 0.34D, dm.z);
                living.hurtMarked = true;
                living.resetFallDistance();
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        boolean purified = tracker.isProtected(pos);
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        HiveProgressionTier tier = HiveProgressionTier.resolveEffectiveTier(
                hive.getThreatLevel(), hive.getHighestTierReached());

        int maxHeight = SinkholeEcosystemEngine.computeUpdraftHeight(tier, purified);
        AABB column = new AABB(
                pos.getX() - 0.65D, pos.getY() + 0.5D, pos.getZ() - 0.65D,
                pos.getX() + 1.65D, pos.getY() + 1.0D + maxHeight, pos.getZ() + 1.65D
        );
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, column,
                e -> e.isAlive() && !e.isSpectator());
        long gameTime = level.getGameTime();
        boolean applyInfectionWindow = !purified
                && tier.level() >= HiveProgressionTier.ADAPTIVE.level()
                && (gameTime % 40L) < TICK_INTERVAL;

        for (LivingEntity entity : entities) {
            double dist = entity.getY() - (pos.getY() + 1.0D);
            boolean sneaking = entity.isShiftKeyDown();
            double boost = SinkholeEcosystemEngine.computeUpdraftVerticalBoost(dist, maxHeight, sneaking);
            Vec3 dm = entity.getDeltaMovement();
            if (!sneaking && boost > 0.0D) {
                double nextY = Math.min(0.56D, Math.max(dm.y + boost, 0.24D));
                entity.setDeltaMovement(dm.x, nextY, dm.z);
                entity.hurtMarked = true;
                entity.resetFallDistance();
            } else if (sneaking && dist >= 0.0D && dist <= maxHeight) {
                if (dm.y < -0.18D) {
                    entity.setDeltaMovement(dm.x, Math.max(-0.18D, dm.y * 0.60D), dm.z);
                    entity.hurtMarked = true;
                }
                entity.resetFallDistance();
            }

            if (applyInfectionWindow
                    && !(entity instanceof MycelialMob)
                    && !(entity instanceof SporeGliderEntity)) {
                if (entity instanceof Player player) {
                    if (!player.getAbilities().instabuild && !ChitinArmorItem.hasFullSet(player)) {
                        int pieces = ChitinArmorItem.countEquippedPieces(player);
                        int duration = Math.max(20, (int) Math.round(70 * (1.0D - 0.20D * pieces)));
                        player.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, duration, 0));
                    }
                } else {
                    entity.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 70, 0));
                }
            }
        }

        if ((gameTime & 7L) < TICK_INTERVAL) {
            level.sendParticles(
                    purified ? ModParticles.BIO_LUME.get() : ModParticles.SPORE_MOTE.get(),
                    pos.getX() + 0.5D, pos.getY() + 1.15D, pos.getZ() + 0.5D,
                    4, 0.35D, Math.min(4.0D, maxHeight * 0.25D), 0.35D, 0.06D
            );
        }

        long phase = Math.floorMod(pos.asLong() + gameTime, 200L);
        if (phase < TICK_INTERVAL) {
            boolean rootNodeAlive = tracker.findNearestEnemyNode(pos, 28) != null;
            double pulse = SinkholeEcosystemEngine.computePassiveThreatPulse(tier, purified, rootNodeAlive);
            if (pulse > 0.0D) {
                hive.addThreatPenalty(pulse);
            }
            level.playSound(null, pos, ModSounds.SPORE_UPDRAFT_VENT_PULSE.get(), SoundSource.BLOCKS,
                    0.45F, purified ? 1.25F : 0.92F + random.nextFloat() * 0.12F);
        }

        level.scheduleTick(pos, this, TICK_INTERVAL);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!state.getValue(ACTIVE)) {
            return 0;
        }
        if (level instanceof ServerLevel serverLevel && PurificationZoneTracker.get(serverLevel).isProtected(pos)) {
            return 15;
        }
        return 7;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.spore_updraft_vent")
                .withStyle(ChatFormatting.AQUA));
    }
}
