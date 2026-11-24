package com.alex.mycelialmind.block;

import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import com.alex.mycelialmind.hive.EcosystemPurposeEngine.BloomAuraKind;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialRedstoneTelemetry;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A harvestable ornamental bloom whose palette records a recovered enclave's trait
 * and radiates a tranquilizing garden sanctuary aura for allies and Sovereign Colony workers when mature.
 */
public final class ReclaimedBloomBlock extends Block {
    public static final IntegerProperty PALETTE = IntegerProperty.create("palette", 0, 5);
    public static final BooleanProperty MATURE = BooleanProperty.create("mature");
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D);

    public ReclaimedBloomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PALETTE, 0).setValue(MATURE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PALETTE, MATURE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(MATURE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && state.getValue(MATURE)) {
            level.scheduleTick(pos, this, EcosystemPurposeEngine.BLOOM_AURA_INTERVAL_TICKS);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MATURE) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            emitSanctuaryAura(level, pos, state.getValue(PALETTE));
            level.scheduleTick(pos, this, EcosystemPurposeEngine.BLOOM_AURA_INTERVAL_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(MATURE)) {
            level.setBlockAndUpdate(pos, state.setValue(MATURE, true));
            level.updateNeighbourForOutputSignal(pos, this);
            level.playSound(null, pos, ModSounds.RECLAIMED_BLOOM_PULSE.get(), SoundSource.BLOCKS, 0.48F, 1.0F);
            level.scheduleTick(pos, this, EcosystemPurposeEngine.BLOOM_AURA_INTERVAL_TICKS);
            return;
        }
        emitSanctuaryAura(level, pos, state.getValue(PALETTE));
        level.scheduleTick(pos, this, EcosystemPurposeEngine.BLOOM_AURA_INTERVAL_TICKS);
    }

    public static void emitSanctuaryAura(ServerLevel level, BlockPos pos, int palette) {
        BloomAuraKind kind = EcosystemPurposeEngine.bloomAuraForPalette(palette);
        Holder<MobEffect> primaryEffect = switch (kind) {
            case STRENGTH -> MobEffects.DAMAGE_BOOST;
            case SPEED -> MobEffects.MOVEMENT_SPEED;
            case FIRE_RESISTANCE -> MobEffects.FIRE_RESISTANCE;
            case RESISTANCE -> MobEffects.DAMAGE_RESISTANCE;
            case INSIGHT -> ModEffects.SYNAPTIC_INSIGHT;
            case SANCTUARY_REGEN -> MobEffects.REGENERATION;
        };

        double radius = EcosystemPurposeEngine.BLOOM_AURA_RADIUS;
        double radiusSq = radius * radius;
        AABB box = new AABB(pos).inflate(radius);
        boolean affectedAny = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!entity.isAlive() || entity.blockPosition().distSqr(pos) > radiusSq) {
                continue;
            }
            boolean isAlly = (entity instanceof Player player && !player.isSpectator())
                    || entity instanceof ColonySymbiontEntity
                    || entity instanceof MycelialChimeraEntity
                    || entity instanceof PurifierDroneEntity;
            if (!isAlly) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(
                    primaryEffect,
                    EcosystemPurposeEngine.BLOOM_AURA_DURATION_TICKS,
                    0,
                    true,
                    false
            ));
            if (kind == BloomAuraKind.INSIGHT && entity instanceof Player) {
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
            }
            if (entity instanceof ColonySymbiontEntity symbiont) {
                symbiont.setFatigue(symbiont.getFatigue() - EcosystemPurposeEngine.BLOOM_FATIGUE_RELIEF);
                symbiont.setMorale(symbiont.getMorale() + EcosystemPurposeEngine.BLOOM_MORALE_BOOST);
                symbiont.syncRosterToColony();
            }
            affectedAny = true;
        }

        if (kind == BloomAuraKind.SANCTUARY_REGEN && DimensionalHomeostasisDirector.isHomeostasisDimension(level)) {
            DimensionalHomeostasisSavedData.get(level).sedateArea(
                    new ChunkPos(pos), 0, EcosystemPurposeEngine.BLOOM_BASE_HOMEOSTASIS_CALM, 0, level.getGameTime());
        }

        if (affectedAny) {
            level.sendParticles(ModParticles.BIO_LUME.get(),
                    pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D,
                    2, 0.3D, 0.2D, 0.3D, 0.01D);
        }
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
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(MATURE)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            harvest(serverLevel, pos, state, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public void harvest(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        if (!state.getValue(MATURE)) {
            return;
        }
        if (!player.isCreative()) {
            popResource(level, pos, itemForPalette(state.getValue(PALETTE)));
        }
        level.setBlockAndUpdate(pos, state.setValue(MATURE, false));
        level.updateNeighbourForOutputSignal(pos, this);
        level.scheduleTick(pos, this, 1200);
        level.sendParticles(ModParticles.BIO_LUME.get(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.02);
        level.playSound(null, pos, ModSounds.RECLAIMED_BLOOM_HARVEST.get(), SoundSource.BLOCKS, 0.82F, 1.0F);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return MycelialRedstoneTelemetry.regenerativeFloraSignal(state.getValue(MATURE));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(level, pos, state, player);
    }

    public static int colorForPalette(int palette) {
        return switch (palette) {
            case 1 -> 0xFFE5646F;
            case 2 -> 0xFF54D6E8;
            case 3 -> 0xFFFFA64D;
            case 4 -> 0xFFFFD35A;
            case 5 -> 0xFFB691FF;
            default -> 0xFF8EDB78;
        };
    }

    public static ItemStack itemForPalette(int palette) {
        return switch (palette) {
            case 1 -> new ItemStack(ModItems.RECLAIMED_BLOOM_MELEE.get());
            case 2 -> new ItemStack(ModItems.RECLAIMED_BLOOM_PROJECTILE.get());
            case 3 -> new ItemStack(ModItems.RECLAIMED_BLOOM_FIRE.get());
            case 4 -> new ItemStack(ModItems.RECLAIMED_BLOOM_EXPLOSION.get());
            case 5 -> new ItemStack(ModItems.RECLAIMED_BLOOM_MAGIC.get());
            default -> new ItemStack(ModBlocks.RECLAIMED_BLOOM.get().asItem());
        };
    }
}
