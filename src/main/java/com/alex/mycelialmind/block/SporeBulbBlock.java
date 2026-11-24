package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Fragile organic spore bulb that bursts into an infectious spore cloud when stepped on or broken.
 */
public class SporeBulbBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 9.0D, 12.0D);

    public SporeBulbBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel
                && !HiveMindSavedData.get(serverLevel).isBlindSpotCore(pos)
                && entity instanceof LivingEntity living
                && !(living instanceof MycelialMob)
                && !(living instanceof MycelialChimeraEntity)
                && !(living instanceof PurifierDroneEntity)) {
            if (living instanceof Player player && ChitinArmorItem.hasFullSet(player)) {
                return;
            }
            burstSporeCloud(serverLevel, pos);
            serverLevel.destroyBlock(pos, false);
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
        if (stack.is(Items.SHEARS) || stack.is(ModItems.BIO_EXTRACTOR.get())) {
            if (level instanceof ServerLevel serverLevel) {
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                var blindSite = hive.getBlindSpotSite(pos);
                if (blindSite != null && !blindSite.isCompleted()) {
                    com.alex.mycelialmind.hive.HyphalSiegeDirector.destroyBlindSpotVulnerableCore(serverLevel, hive, blindSite, player);
                    stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    serverLevel.removeBlock(pos, false);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                ItemStack reward = new ItemStack(ModItems.LATENT_SPORE.get(), 2);
                if (!player.getInventory().add(reward)) {
                    Block.popResource(serverLevel, pos, reward);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerDiscoveryData.onLatentSporeAcquired(serverPlayer);
                }
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                if (!player.isCreative()
                        && !com.alex.mycelialmind.world.PurificationZoneTracker.get(serverLevel).isProtected(pos)
                        && !ChitinArmorItem.hasFullSet(player)) {
                    player.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 60, 0));
                    serverLevel.sendParticles(ModParticles.SPORE_CLOUD.get(),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            8, 0.25, 0.25, 0.25, 0.02);
                }
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        12, 0.25, 0.25, 0.25, 0.03);
                serverLevel.playSound(null, pos, ModSounds.BIO_EXTRACTOR_DRAW.get(), SoundSource.BLOCKS, 0.85F, 1.25F);
                serverLevel.removeBlock(pos, false);
                player.displayClientMessage(Component.translatable("message.mycelialmind.flora_harvested_bulb")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel && !player.isCreative()) {
            burstSporeCloud(serverLevel, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public static void burstSporeCloud(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.SPORE_MOTE.get(),
                pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                22, 0.8, 0.5, 0.8, 0.04);
        level.playSound(null, pos, ModSounds.SPORE_BULB_BURST.get(), SoundSource.BLOCKS, 0.85F, 1.0F);
        AABB box = new AABB(pos).inflate(3.5D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> !(e instanceof MycelialMob)
                        && !(e instanceof MycelialChimeraEntity)
                        && !(e instanceof PurifierDroneEntity)
                        && e.isAlive())) {
            int duration = 80;
            if (target instanceof Player player) {
                if (ChitinArmorItem.hasFullSet(player)) {
                    continue;
                }
                int pieces = ChitinArmorItem.countEquippedPieces(player);
                duration = Math.max(16, (int) Math.round(duration * (1.0 - 0.20 * pieces)));
            }
            target.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, duration, 0));
        }
        HiveMindSavedData.get(level).recordStimulus(StimulusType.MELEE, 2.0, level.getGameTime());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.spore_bulb").withStyle(ChatFormatting.YELLOW));
    }
}
