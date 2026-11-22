package com.alex.mycelialmind.block;

import com.alex.mycelialmind.block.entity.HyphalVeinBlockEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HyphalPhysics;
import com.alex.mycelialmind.hive.HyphalSiegeDirector;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Lightning-black surface mycelial vein overlay that follows literal player trails, climbs
 * walls/ceilings, conducts 0-15 nutrients from traversal root nodes, withers when severed,
 * and falls as a disintegrating FallingBlockEntity if it loses all solid support.
 */
public class HyphalVeinBlock extends Block implements EntityBlock, Fallable {
    public enum VeinConnection implements StringRepresentable {
        NONE("none"),
        SIDE("side"),
        UP("up");

        private final String serializedName;

        VeinConnection(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final EnumProperty<VeinConnection> NORTH = EnumProperty.create("north", VeinConnection.class);
    public static final EnumProperty<VeinConnection> EAST = EnumProperty.create("east", VeinConnection.class);
    public static final EnumProperty<VeinConnection> SOUTH = EnumProperty.create("south", VeinConnection.class);
    public static final EnumProperty<VeinConnection> WEST = EnumProperty.create("west", VeinConnection.class);
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 0, 3);

    private static final int NUTRIENT_STEP_TICKS = 2;

    private static final VoxelShape SHAPE_DOWN = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape SHAPE_UP = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape SHAPE_EAST = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape[] SHAPE_BY_FACE_MASK = buildShapeLookup();

    private static VoxelShape[] buildShapeLookup() {
        VoxelShape[] table = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = Shapes.empty();
            if ((mask & HyphalPhysics.FACE_DOWN) != 0) {
                shape = Shapes.or(shape, SHAPE_DOWN);
            }
            if ((mask & HyphalPhysics.FACE_NORTH) != 0) {
                shape = Shapes.or(shape, SHAPE_NORTH);
            }
            if ((mask & HyphalPhysics.FACE_EAST) != 0) {
                shape = Shapes.or(shape, SHAPE_EAST);
            }
            if ((mask & HyphalPhysics.FACE_SOUTH) != 0) {
                shape = Shapes.or(shape, SHAPE_SOUTH);
            }
            if ((mask & HyphalPhysics.FACE_WEST) != 0) {
                shape = Shapes.or(shape, SHAPE_WEST);
            }
            if ((mask & HyphalPhysics.FACE_UP) != 0) {
                shape = Shapes.or(shape, SHAPE_UP);
            }
            table[mask] = shape.isEmpty() ? SHAPE_DOWN : shape;
        }
        return table;
    }

    public HyphalVeinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, VeinConnection.NONE)
                .setValue(EAST, VeinConnection.NONE)
                .setValue(SOUTH, VeinConnection.NONE)
                .setValue(WEST, VeinConnection.NONE)
                .setValue(CHARGED, Boolean.FALSE)
                .setValue(STRENGTH, 0));
    }

    public static int colorForStrength(int strength) {
        return switch (Math.max(0, Math.min(3, strength))) {
            case 0 -> 0xA69FB0;
            case 1 -> 0x7E4BA3;
            case 2 -> 0x521878;
            default -> 0x240533;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, CHARGED, STRENGTH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HyphalVeinBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        byte faces = HyphalPhysics.FACE_DOWN;
        if (level.getBlockEntity(pos) instanceof HyphalVeinBlockEntity veinBe) {
            faces = veinBe.getFaces();
        }
        return SHAPE_BY_FACE_MASK[faces & HyphalPhysics.ALL_FACES_MASK];
    }

    public static boolean isFaceSupported(LevelReader level, BlockPos pos, Direction attachDir) {
        BlockPos supportPos = pos.relative(attachDir);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, attachDir.getOpposite());
    }

    public static byte filterSupportedFaces(LevelReader level, BlockPos pos, byte rawFaces) {
        byte effective = HyphalPhysics.effectiveFaces(rawFaces);
        byte supported = 0;
        for (Direction dir : Direction.values()) {
            byte bit = HyphalPhysics.faceBit(dir);
            if ((effective & bit) != 0 && isFaceSupported(level, pos, dir)) {
                supported |= bit;
            }
        }
        return supported;
    }

    public static byte detectAvailableFaces(LevelReader level, BlockPos pos) {
        byte detected = 0;
        for (Direction dir : Direction.values()) {
            if (isFaceSupported(level, pos, dir)) {
                detected |= HyphalPhysics.faceBit(dir);
            }
        }
        return detected;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        byte faces = HyphalPhysics.FACE_DOWN;
        if (level.getBlockEntity(pos) instanceof HyphalVeinBlockEntity be) {
            faces = be.getFaces();
        }
        return filterSupportedFaces(level, pos, faces) != 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return recomputeConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            if (level.getBlockEntity(pos) instanceof HyphalVeinBlockEntity be) {
                if (filterSupportedFaces(level, pos, be.getFaces()) == 0) {
                    byte detected = detectAvailableFaces(level, pos);
                    if (detected != 0) {
                        be.setFaces(detected);
                        be.syncToClient();
                    }
                }
            }
            if (!state.canSurvive(level, pos)) {
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            scheduleConnectedVeinNeighbors(serverLevel, pos, null, NUTRIENT_STEP_TICKS);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (level.getBlockEntity(pos) instanceof HyphalVeinBlockEntity be) {
            byte currentFaces = be.getFaces();
            byte remaining = filterSupportedFaces(level, pos, currentFaces);
            if (remaining == 0) {
                level.scheduleTick(pos, this, 1);
                return state;
            }
            if (remaining != currentFaces) {
                be.setFaces(remaining);
                be.syncToClient();
            }
        } else if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
            return state;
        }
        return recomputeConnections(level, pos, state);
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
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            scheduleConnectedVeinNeighbors(level, pos,
                    level.getBlockEntity(pos) instanceof HyphalVeinBlockEntity be ? be : null,
                    NUTRIENT_STEP_TICKS);
            HiveMindSavedData.get(level).severHyphalVein(pos, 0.0, level.getGameTime());
            FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
            falling.disableDrop();
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof HyphalVeinBlockEntity be)) {
            return;
        }

        int maxNeighborNutrient = computeMaxNeighborNutrient(level, pos, be);
        int nextNutrient = HyphalPhysics.nextNutrient(maxNeighborNutrient, be.isRoot());
        boolean nutrientChanged = be.setNutrient(nextNutrient);

        BlockState workingState = state;
        boolean desiredCharged = HyphalPhysics.chargedForNutrient(nextNutrient);
        if (workingState.getValue(CHARGED) != desiredCharged) {
            workingState = workingState.setValue(CHARGED, desiredCharged);
        }

        if (!be.isRoot() && nextNutrient == 0) {
            long now = level.getGameTime();
            if (be.getNextWitherTick() == 0L) {
                be.setNextWitherTick(now + HyphalPhysics.WITHER_INTERVAL_TICKS);
            }
            if (now >= be.getNextWitherTick()) {
                spawnSporeBurst(level, pos, 3);
                int curStrength = workingState.getValue(STRENGTH);
                if (curStrength > 0) {
                    workingState = workingState.setValue(STRENGTH, curStrength - 1).setValue(CHARGED, Boolean.FALSE);
                    if (!workingState.equals(state)) {
                        level.setBlock(pos, workingState, Block.UPDATE_ALL);
                    }
                    be.syncToClient();
                    be.setNextWitherTick(now + HyphalPhysics.WITHER_INTERVAL_TICKS);
                    level.scheduleTick(pos, this, HyphalPhysics.WITHER_INTERVAL_TICKS);
                } else {
                    HiveMindSavedData.get(level).severHyphalVein(pos, 0.0, now);
                    level.removeBlock(pos, false);
                    refreshLocalNeighborhood(level, pos);
                    return;
                }
            } else {
                if (!workingState.equals(state)) {
                    level.setBlock(pos, workingState, Block.UPDATE_ALL);
                }
                if (nutrientChanged) {
                    be.syncToClient();
                }
                int delay = (int) Math.max(1L, Math.min(HyphalPhysics.WITHER_INTERVAL_TICKS, be.getNextWitherTick() - now));
                level.scheduleTick(pos, this, delay);
            }
        } else {
            be.setNextWitherTick(0L);
            int minStrength = HyphalPhysics.strengthForTraversals(be.getTraversals());
            int curStrength = workingState.getValue(STRENGTH);
            if (curStrength < minStrength) {
                workingState = workingState.setValue(STRENGTH, minStrength);
            }
            if (!workingState.equals(state)) {
                level.setBlock(pos, workingState, Block.UPDATE_ALL);
            }
            if (nutrientChanged) {
                be.syncToClient();
            }
        }

        if (nutrientChanged) {
            scheduleConnectedVeinNeighbors(level, pos, be, NUTRIENT_STEP_TICKS);
        }

        if ((be.isRoot() || nextNutrient > 1) && be.getTrailIndex() >= 0) {
            long now = level.getGameTime();
            if (now >= be.getNextGrowthTick()) {
                boolean grew = HyphalSiegeDirector.growOneTrailStepFromVein(level, pos, be);
                if (grew) {
                    be.setNextGrowthTick(now + HyphalPhysics.getGrowthIntervalTicks());
                }
            } else {
                int delay = (int) Math.max(1L, Math.min(HyphalPhysics.getGrowthIntervalTicks(), be.getNextGrowthTick() - now));
                level.scheduleTick(pos, this, delay);
            }
        }
    }

    public static int computeMaxNeighborNutrient(LevelReader level, BlockPos pos, HyphalVeinBlockEntity self) {
        int best = 0;
        // Cardinal horizontal (same Y, step up Y+1, step down Y-1)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy : new int[]{0, 1, -1}) {
                BlockPos adj = pos.relative(dir).offset(0, dy, 0);
                if (level.getBlockEntity(adj) instanceof HyphalVeinBlockEntity neighbor) {
                    best = Math.max(best, neighbor.getNutrient());
                }
            }
        }
        // Vertical climbing (wall columns)
        for (int dy : new int[]{1, -1}) {
            BlockPos adj = pos.offset(0, dy, 0);
            if (level.getBlockEntity(adj) instanceof HyphalVeinBlockEntity neighbor) {
                best = Math.max(best, neighbor.getNutrient());
            }
        }
        // Diagonal horizontal (same Y, step up Y+1, step down Y-1)
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                byte myBit = HyphalPhysics.diagonalBit(dx, dz);
                byte oppBit = HyphalPhysics.oppositeDiagonalBit(dx, dz);
                for (int dy : new int[]{0, 1, -1}) {
                    BlockPos adj = pos.offset(dx, dy, dz);
                    if (level.getBlockEntity(adj) instanceof HyphalVeinBlockEntity neighbor) {
                        if ((self.getDiagonals() & myBit) != 0 || (neighbor.getDiagonals() & oppBit) != 0) {
                            best = Math.max(best, neighbor.getNutrient());
                        }
                    }
                }
            }
        }
        return best;
    }

    public static void scheduleConnectedVeinNeighbors(
            ServerLevel level,
            BlockPos center,
            @Nullable HyphalVeinBlockEntity centerBe,
            int delayTicks
    ) {
        int safeDelay = Math.max(1, delayTicks);
        Block veinBlock = ModBlocks.HYPHAL_VEIN.get();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos adj = center.offset(dx, dy, dz);
                    if (!level.isLoaded(adj) || !level.getBlockState(adj).is(veinBlock)) {
                        continue;
                    }
                    if (dx != 0 && dz != 0) {
                        byte myBit = HyphalPhysics.diagonalBit(dx, dz);
                        byte oppBit = HyphalPhysics.oppositeDiagonalBit(dx, dz);
                        boolean selfConnected = centerBe != null && (centerBe.getDiagonals() & myBit) != 0;
                        boolean otherConnected = level.getBlockEntity(adj) instanceof HyphalVeinBlockEntity nbe
                                && (nbe.getDiagonals() & oppBit) != 0;
                        if (!selfConnected && !otherConnected) {
                            continue;
                        }
                    }
                    level.scheduleTick(adj, veinBlock, safeDelay);
                }
            }
        }
    }

    @Override
    public void onLand(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockState replaceableState,
            FallingBlockEntity fallingBlock
    ) {
        level.removeBlock(pos, false);
        if (level instanceof ServerLevel serverLevel) {
            spawnSporeBurst(serverLevel, pos, 8);
            serverLevel.playSound(null, pos, ModSounds.EPHEMERAL_CHITIN_CRUMBLE.get(),
                    SoundSource.BLOCKS, 0.55F, 1.25F);
        }
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (level instanceof ServerLevel serverLevel) {
            spawnSporeBurst(serverLevel, pos, 8);
            serverLevel.playSound(null, pos, ModSounds.EPHEMERAL_CHITIN_CRUMBLE.get(),
                    SoundSource.BLOCKS, 0.55F, 1.25F);
        }
    }

    private static void spawnSporeBurst(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(ModParticles.SPORE_MOTE.get(),
                pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                count, 0.25D, 0.15D, 0.25D, 0.02D);
        level.sendParticles(ColorParticleOption.create(ModParticles.HIVE_DUST.get(), 0xFF1A0E2E),
                pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                Math.max(2, count / 2), 0.25D, 0.15D, 0.25D, 0.02D);
    }

    public static BlockState recomputeConnections(LevelReader level, BlockPos pos, BlockState baseState) {
        return baseState
                .setValue(NORTH, computeDirectionConnection(level, pos, Direction.NORTH))
                .setValue(EAST, computeDirectionConnection(level, pos, Direction.EAST))
                .setValue(SOUTH, computeDirectionConnection(level, pos, Direction.SOUTH))
                .setValue(WEST, computeDirectionConnection(level, pos, Direction.WEST));
    }

    public static void refreshLocalNeighborhood(Level level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    BlockState st = level.getBlockState(p);
                    if (st.is(ModBlocks.HYPHAL_VEIN.get())) {
                        BlockState updated = recomputeConnections(level, p, st);
                        if (!updated.equals(st)) {
                            level.setBlock(p, updated, 2);
                        }
                    }
                }
            }
        }
    }

    public static VeinConnection computeDirectionConnection(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos adj = pos.relative(dir);
        BlockPos abovePos = pos.above();
        if (!level.getBlockState(abovePos).canOcclude()) {
            BlockPos adjUp = adj.above();
            if (isConnectableBlock(level.getBlockState(adjUp))) {
                return VeinConnection.UP;
            }
        }
        BlockState adjState = level.getBlockState(adj);
        if (isConnectableBlock(adjState)) {
            return VeinConnection.SIDE;
        }
        if (!adjState.canOcclude() && isConnectableBlock(level.getBlockState(adj.below()))) {
            return VeinConnection.SIDE;
        }
        return VeinConnection.NONE;
    }

    private static boolean isConnectableBlock(BlockState state) {
        return state.is(ModBlocks.HYPHAL_VEIN.get())
                || state.is(ModBlocks.CREEPING_MYCELIUM.get())
                || state.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())
                || state.is(ModBlocks.SPORE_BULB.get())
                || state.getBlock() instanceof MycelialNodeBlock;
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
        if (hand == InteractionHand.MAIN_HAND && HyphalSiegeDirector.isRightClickSeveringTool(stack)) {
            if (level instanceof ServerLevel serverLevel) {
                if (HyphalSiegeDirector.trySeverHyphalVein(serverLevel, pos, player, stack) > 0) {
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    return ItemInteractionResult.sidedSuccess(false);
                }
            } else {
                return ItemInteractionResult.sidedSuccess(true);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if ((state.getValue(CHARGED) || state.getValue(STRENGTH) >= 2) && random.nextInt(4) == 0) {
            double x = pos.getX() + 0.15D + random.nextDouble() * 0.70D;
            double y = pos.getY() + 0.08D;
            double z = pos.getZ() + 0.15D + random.nextDouble() * 0.70D;
            level.addParticle(ColorParticleOption.create(ModParticles.HIVE_DUST.get(), 0xFF1A0E2E),
                    x, y, z, 0.0D, 0.015D, 0.0D);
            if (random.nextInt(3) == 0) {
                level.addParticle(ModParticles.SPORE_MOTE.get(), x, y, z, 0.0D, 0.01D, 0.0D);
            }
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return com.alex.mycelialmind.util.MycelialRedstoneTelemetry.hyphalVeinSignal(
                state.getValue(STRENGTH), state.getValue(CHARGED));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.hyphal_vein").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
