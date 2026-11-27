package com.alex.mycelialmind.block;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.entity.SporelingEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.util.MycelialRedstoneTelemetry;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Synaptic Relay anchor for the Mycelial Network. Amplifies local Hive learning,
 * links nearby nodes, and periodically spawns patrols when threat escalates.
 */
public class MycelialNodeBlock extends Block implements ServerTickingEntityBlock {
    public MycelialNodeBlock(Properties properties) {
        super(properties);
    }

    public SynapticNodeType nodeType() {
        return SynapticNodeType.RELAY;
    }

    protected String tooltipKey() {
        return "tooltip.mycelialmind.block.mycelial_node";
    }

    protected ChatFormatting tooltipColor() {
        return ChatFormatting.LIGHT_PURPLE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            PurificationZoneTracker.get(serverLevel).addNode(pos, nodeType());
            serverLevel.scheduleTick(pos, this, 200);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            PurificationZoneTracker.get(serverLevel).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        if (nodeType() == SynapticNodeType.RELAY && !tracker.isProtected(pos)) {
            HiveMindSavedData hive = HiveMindSavedData.get(level);
            if (hive.getThreatLevel() >= 5.0 && level.hasNearbyAlivePlayer(pos.getX(), pos.getY(), pos.getZ(), 48.0)) {
                SporelingEntity sporeling = ModEntities.SPORELING.get().create(level);
                if (sporeling != null) {
                    sporeling.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                    sporeling.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
                    if (level.noCollision(sporeling)) {
                        level.addFreshEntity(sporeling);
                    }
                }
            }
        }
        level.scheduleTick(pos, this, 240);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SynapticNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTickingEntityBlock.createServerTicker(
                level, type, ModBlockEntities.SYNAPTIC_NODE.get(), SynapticNodeBlockEntity::serverTick);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SynapticNodeBlockEntity be) {
            if (nodeType() == SynapticNodeType.SYMBIOTIC) {
                return MycelialRedstoneTelemetry.symbioticNodeSignal(be.getSymbioticStability());
            }
            return MycelialRedstoneTelemetry.hostileNodeSignal(be.getVolatileTotal(), be.getBiomassCharge());
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey()).withStyle(tooltipColor()));
    }
}
