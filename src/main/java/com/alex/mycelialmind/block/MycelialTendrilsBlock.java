package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Hanging bioluminescent mycelial tendrils that grow from cave ceilings above outbreaks
 * and allow regenerative biomass harvesting with brief Synaptic Insight.
 */
public class MycelialTendrilsBlock extends Block {
    public static final BooleanProperty MATURE = BooleanProperty.create("mature");
    private static final VoxelShape SHAPE = Block.box(3.0D, 3.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    public MycelialTendrilsBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MATURE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MATURE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).isSolid();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(MATURE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(MATURE)) {
            BlockState above = level.getBlockState(pos.above());
            boolean rich = above.is(ModBlocks.CREEPING_MYCELIUM.get()) || above.is(ModBlocks.MYCELIAL_STONE.get());
            HiveMindSavedData hive = HiveMindSavedData.get(level);
            HiveProgressionTier act = HiveProgressionTier.resolveEffectiveTier(
                    hive.getThreatLevel(), hive.getHighestTierReached());
            boolean inRemission = hive.isMatriarchRemissionActive(level.getGameTime());
            float chance = Math.min(0.95F, (rich ? 0.50F : 0.25F) * act.floraRegrowthMultiplier(inRemission));
            if (random.nextFloat() < chance) {
                level.setBlockAndUpdate(pos, state.setValue(MATURE, true));
                level.updateNeighbourForOutputSignal(pos, this);
                level.sendParticles(ModParticles.BIO_LUME.get(),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        4, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.getMainHandItem().is(ModItems.BIO_SCANNER.get())
                || player.getOffhandItem().is(ModItems.BIO_SCANNER.get())) {
            return InteractionResult.PASS;
        }
        if (!state.getValue(MATURE)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.setBlockAndUpdate(pos, state.setValue(MATURE, false));
            serverLevel.updateNeighbourForOutputSignal(pos, this);
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            HiveProgressionTier act = HiveProgressionTier.resolveEffectiveTier(
                    hive.getThreatLevel(), hive.getHighestTierReached());
            int yieldCount = serverLevel.random.nextFloat() < act.floraBonusYieldChance() ? 2 : 1;
            ItemStack biomass = new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), yieldCount);
            if (!player.getInventory().add(biomass)) {
                Block.popResource(serverLevel, pos, biomass);
            }
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), yieldCount);
            player.addEffect(new MobEffectInstance(ModEffects.SYNAPTIC_INSIGHT, 160, 0, false, false));
            serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.25, 0.25, 0.25, 0.02);
            serverLevel.playSound(null, pos, ModSounds.SPORE_ANTIDOTE_CURE.get(), SoundSource.BLOCKS, 0.70F, 1.35F);
            player.displayClientMessage(Component.translatable("message.mycelialmind.flora_harvested_tendrils")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
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
        if (!state.getValue(MATURE)
                && (stack.is(Items.BONE_MEAL) || stack.is(ModItems.LATENT_SPORE.get()))) {
            if (level instanceof ServerLevel serverLevel) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                serverLevel.setBlockAndUpdate(pos, state.setValue(MATURE, true));
                serverLevel.updateNeighbourForOutputSignal(pos, this);
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        10, 0.3, 0.3, 0.3, 0.02);
                serverLevel.playSound(null, pos, ModSounds.BLOCK_MYCELIUM_PLACE.get(), SoundSource.BLOCKS, 0.8F, 1.15F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return com.alex.mycelialmind.util.MycelialRedstoneTelemetry.regenerativeFloraSignal(state.getValue(MATURE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.mycelial_tendrils").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
