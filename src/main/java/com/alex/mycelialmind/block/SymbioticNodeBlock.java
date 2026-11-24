package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.MycorrhizalAcousticTracker;
import com.alex.mycelialmind.hive.ColonyReconstructionDirector;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Allied reprogrammed Synaptic Node created by applying a Synaptic Graft.
 * Silences mycorrhizal vibrations in 16m, drains dominant Hive stimuli,
 * and allows balanced node-to-node Mycorrhizal Fast Travel on Shift+Right-click.
 */
public class SymbioticNodeBlock extends MycelialNodeBlock {
    public SymbioticNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected SynapticNodeType nodeType() {
        return SynapticNodeType.SYMBIOTIC;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            ColonyReconstructionDirector.recordOutpostDestroyed(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Allied symbiotic nodes never spawn hostile sporelings.
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
        if (stack.is(ModItems.MYCELIAL_BIOMASS.get())
                && !player.isSecondaryUseActive()
                && level.getBlockEntity(pos) instanceof SynapticNodeBlockEntity be
                && be.getSymbioticStability() < 100) {
            if (level instanceof ServerLevel serverLevel) {
                int nextStability = Math.min(100, be.getSymbioticStability() + 15);
                be.setSymbioticStability(nextStability);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                        pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                        4, 0.35, 0.35, 0.35, 0.04);
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                        pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                        6, 0.35, 0.35, 0.35, 0.02);
                serverLevel.playSound(null, pos, ModSounds.SYMBIOTIC_NODE_CHARGE.get(), SoundSource.BLOCKS, 0.85F, 1.15F);
                player.displayClientMessage(
                        Component.translatable("message.mycelialmind.symbiotic_node_recharged", nextStability)
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
        int range = MycelialConfig.MYCORRHIZAL_TRAVEL_RANGE.get();
        List<BlockPos> linked = tracker.getLinkedSymbioticNodes(pos, range);
        int stability = (level.getBlockEntity(pos) instanceof SynapticNodeBlockEntity be)
                ? be.getSymbioticStability()
                : 100;

        if (!player.isSecondaryUseActive()) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.symbiotic_node_status", stability, linked.size())
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (linked.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.mycorrhizal_travel_no_link", range)
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer && tryMycorrhizalTravel(serverLevel, serverPlayer, pos, linked.get(0))) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.CONSUME;
    }

    public static final int TRAVEL_FOOD_COST = 6;
    public static final int TRAVEL_STABILITY_COST = 15;
    public static final int TRAVEL_COMBAT_LOCK_TICKS = 100;

    public static boolean tryMycorrhizalTravel(ServerLevel level, ServerPlayer player, BlockPos origin, BlockPos destination) {
        long now = level.getServer().overworld().getGameTime();
        long remainingTicks = MycorrhizalAcousticTracker.getTravelCooldownRemaining(player.getUUID(), now);
        if (remainingTicks > 0L && !player.isCreative()) {
            int sec = (int) Math.ceil(remainingTicks / 20.0);
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.mycorrhizal_travel_cooldown", sec)
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        if (!player.isCreative() && (player.tickCount - player.getLastHurtByMobTimestamp()) < TRAVEL_COMBAT_LOCK_TICKS) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.mycorrhizal_travel_combat").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        if (!player.isCreative() && player.hasEffect(ModEffects.MYCELIAL_INFECTION)) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.mycorrhizal_travel_infected").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        if (!hasSafeTravelArrival(level, destination)) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.mycorrhizal_travel_destination_blocked")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        SynapticNodeBlockEntity originBe = level.getBlockEntity(origin) instanceof SynapticNodeBlockEntity be
                ? be
                : null;
        if (!player.isCreative() && originBe != null && originBe.getSymbioticStability() < TRAVEL_STABILITY_COST) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.mycorrhizal_travel_low_stability").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        if (!player.isCreative() && player.getFoodData().getFoodLevel() < TRAVEL_FOOD_COST) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.mycorrhizal_travel_hungry")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        if (!player.isCreative()) {
            if (originBe != null) {
                originBe.setSymbioticStability(originBe.getSymbioticStability() - TRAVEL_STABILITY_COST);
            }
            player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - TRAVEL_FOOD_COST));
            player.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 240, 0));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 60, 0, false, false));
            MycorrhizalAcousticTracker.setTravelCooldown(player.getUUID(), now + MycelialConfig.MYCORRHIZAL_TRAVEL_COOLDOWN_TICKS.get());
        }

        level.sendParticles(ModParticles.ENGRAM.get(), origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
                10, 0.35, 0.5, 0.35, 0.08);
        level.playSound(null, origin, ModSounds.SYMBIOTIC_NODE_TELEPORT.get(), SoundSource.PLAYERS, 0.85F, 1.0F);

        AABB companionBox = new AABB(origin).inflate(12.0D);
        List<MycelialChimeraEntity> chimeras = level.getEntitiesOfClass(
                MycelialChimeraEntity.class, companionBox,
                c -> c.isAlive() && player.getUUID().equals(c.getOwnerUUID())
                        && (c.getCommandMode() == MycelialChimeraEntity.CommandMode.FOLLOW || c.hasPassenger(player))
        );
        List<PurifierDroneEntity> drones = level.getEntitiesOfClass(
                PurifierDroneEntity.class, companionBox,
                d -> d.isAlive() && player.getUUID().equals(d.getOwnerUuid()) && !d.isPatrolMode()
        );
        if (player.isPassenger() && player.getVehicle() instanceof MycelialChimeraEntity) {
            player.stopRiding();
        }

        double destX = destination.getX() + 0.5;
        double destY = destination.getY() + 1.05;
        double destZ = destination.getZ() + 0.5;
        player.teleportTo(level, destX, destY, destZ, player.getYRot(), player.getXRot());
        player.resetFallDistance();

        int movedCompanions = 0;
        for (MycelialChimeraEntity chimera : chimeras) {
            chimera.teleportTo(level, destX, destY, destZ, java.util.Set.of(), chimera.getYRot(), chimera.getXRot());
            chimera.setHomePos(destination);
            chimera.resetFallDistance();
            movedCompanions++;
        }
        for (PurifierDroneEntity drone : drones) {
            drone.teleportTo(level, destX, destY + 1.2, destZ, java.util.Set.of(), drone.getYRot(), drone.getXRot());
            drone.resetFallDistance();
            movedCompanions++;
        }
        if (movedCompanions > 0) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.companions_transferred", movedCompanions)
                    .withStyle(ChatFormatting.AQUA));
        }

        level.sendParticles(ModParticles.PURIFIER_CROSS.get(), destX, destY + 0.5, destZ,
                4, 0.45, 0.5, 0.45, 0.04);
        level.sendParticles(ModParticles.BIO_LUME.get(), destX, destY + 0.7, destZ,
                6, 0.45, 0.5, 0.45, 0.02);
        level.playSound(null, destination, ModSounds.SYMBIOTIC_NODE_TELEPORT.get(), SoundSource.PLAYERS, 0.9F, 1.25F);

        player.displayClientMessage(
                Component.translatable("message.mycelialmind.mycorrhizal_travel_success",
                        destination.getX(), destination.getY(), destination.getZ())
                        .withStyle(ChatFormatting.AQUA),
                true
        );
        return true;
    }

    private static boolean hasSafeTravelArrival(ServerLevel level, BlockPos destination) {
        BlockPos feet = destination.above();
        BlockPos head = feet.above();
        return level.isInWorldBounds(feet)
                && level.isInWorldBounds(head)
                && level.getWorldBorder().isWithinBounds(feet)
                && level.getBlockState(destination).isFaceSturdy(level, destination, Direction.UP)
                && level.isEmptyBlock(feet)
                && level.isEmptyBlock(head);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.symbiotic_node").withStyle(ChatFormatting.GREEN));
    }
}
