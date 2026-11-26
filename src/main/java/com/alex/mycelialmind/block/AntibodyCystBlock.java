package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialRedstoneTelemetry;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Living immune tissue grown around foreign player-placed blocks in deep_mycelium.
 * Smothers Purification Pylons when covering >= 4 faces, withers once dimensional stress
 * returns to STABLE, and yields enriched biotech samples when harvested during Immune Response.
 */
public class AntibodyCystBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AntibodyCystBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    public static int countAdjacentCysts(Level level, BlockPos targetPos) {
        int count = 0;
        Block cystBlock = ModBlocks.ANTIBODY_CYST.get();
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(targetPos.relative(dir)).is(cystBlock)) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (!oldState.is(this)) {
                DimensionalHomeostasisSavedData.get(serverLevel).adjustCystCount(new ChunkPos(pos), 1);
            }
            recheckAdjacentPylons(serverLevel, pos);
            serverLevel.updateNeighbourForOutputSignal(pos, this);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            DimensionalHomeostasisSavedData.get(serverLevel).adjustCystCount(new ChunkPos(pos), -1);
            recheckAdjacentPylons(serverLevel, pos, pos);
            serverLevel.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide() || !state.getValue(ACTIVE)) {
            return;
        }
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            if (!ChitinArmorItem.hasFullSet(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                int infTicks = ChitinArmorItem.reduceInfectionDuration(player, 60);
                player.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, infTicks, 0, false, false));
            }
        } else if (entity instanceof PurifierDroneEntity drone) {
            drone.setJammedTicks(Math.max(drone.getJammedTicks(), 40));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        double stress = level instanceof ServerLevel serverLevel
                ? DimensionalHomeostasisSavedData.get(serverLevel).getStress(new ChunkPos(pos))
                : 0.0;
        return MycelialRedstoneTelemetry.antibodyCystSignal(
                state.getValue(ACTIVE), stress, countAdjacentCysts(level, pos)
        );
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp
                && DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
            HomeostasisStage stage = DimensionalHomeostasisSavedData.get(serverLevel).getStage(new ChunkPos(pos));
            if (stage == HomeostasisStage.IMMUNE_RESPONSE && !hasSilkTouch(serverLevel, player)) {
                DimensionalHomeostasisDirector.grantHomeostasisAdvancement(sp);
                if (state.getValue(ACTIVE)) {
                    emitHistamineBurst(serverLevel, pos);
                }
                if (!player.isCreative() && serverLevel.random.nextFloat() < 0.25F) {
                    AdaptationTrait trait = pickBonusTrait(serverLevel);
                    ItemStack sample = new ItemStack(ModItems.GENETIC_SAMPLE.get());
                    sample.set(ModDataComponents.TRAIT_ID.get(), trait.id());
                    ItemEntity entity = new ItemEntity(serverLevel,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sample);
                    serverLevel.addFreshEntity(entity);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void emitHistamineBurst(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.7, 0.5, 0.7, 0.03);
        level.sendParticles(ModParticles.ACID_SPLASH.get(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.5, 0.4, 0.5, 0.02);
        AABB burstBox = new AABB(pos).inflate(2.5D);
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double radiusSq = 2.5D * 2.5D;
        for (MycelialMob mob : level.getEntitiesOfClass(MycelialMob.class, burstBox)) {
            if (mob.isAlive() && mob.distanceToSqr(centerX, centerY, centerZ) <= radiusSq) {
                mob.heal(3.0F);
            }
        }
        for (Player p : level.getEntitiesOfClass(Player.class, burstBox)) {
            if (!p.isCreative() && !p.isSpectator() && !ChitinArmorItem.hasFullSet(p)
                    && p.distanceToSqr(centerX, centerY, centerZ) <= radiusSq) {
                p.addEffect(new MobEffectInstance(ModEffects.SPORE_ROT, 60, 0));
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!DimensionalHomeostasisDirector.isHomeostasisDimension(level)) {
            return;
        }
        HomeostasisStage stage = DimensionalHomeostasisSavedData.get(level).getStage(new ChunkPos(pos));
        if (stage == HomeostasisStage.STABLE) {
            if (state.getValue(ACTIVE)) {
                level.setBlock(pos, state.setValue(ACTIVE, false), 3);
                level.updateNeighbourForOutputSignal(pos, this);
            } else {
                level.removeBlock(pos, false);
                level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        8, 0.25, 0.25, 0.25, 0.02);
            }
        } else if (!state.getValue(ACTIVE) && stage == HomeostasisStage.IMMUNE_RESPONSE) {
            level.setBlock(pos, state.setValue(ACTIVE, true), 3);
            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    private static boolean hasSilkTouch(ServerLevel level, Player player) {
        var holder = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.SILK_TOUCH);
        return holder.isPresent() && EnchantmentHelper.getEnchantmentLevel(holder.get(), player) > 0;
    }

    private static AdaptationTrait pickBonusTrait(ServerLevel level) {
        var active = HiveMindSavedData.get(level).getActiveTraits();
        if (!active.isEmpty()) {
            int idx = level.random.nextInt(active.size());
            int i = 0;
            for (AdaptationTrait t : active) {
                if (i == idx) {
                    return t;
                }
                i++;
            }
        }
        return AdaptationTrait.BLAST_SHIELDED;
    }

    public static void recheckAdjacentPylons(ServerLevel level, BlockPos origin) {
        recheckAdjacentPylons(level, origin, null);
    }

    private static void recheckAdjacentPylons(ServerLevel level, BlockPos origin, BlockPos removingPos) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        Block pylonBlock = ModBlocks.PURIFICATION_PYLON.get();
        Block cystBlock = ModBlocks.ANTIBODY_CYST.get();

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            if (!level.getBlockState(neighbor).is(pylonBlock)) {
                continue;
            }
            int faces = 0;
            for (Direction face : Direction.values()) {
                BlockPos adj = neighbor.relative(face);
                if (removingPos != null && adj.equals(removingPos)) {
                    continue;
                }
                if (level.getBlockState(adj).is(cystBlock)) {
                    faces++;
                }
            }
            boolean wasSmothered = tracker.isPylonSmothered(neighbor);
            boolean nowSmothered = faces >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
            if (wasSmothered != nowSmothered) {
                tracker.setPylonSmothered(neighbor, nowSmothered);
                if (nowSmothered) {
                    com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector.onDefensePylonDisabled(level, neighbor);
                    level.playSound(null, neighbor, ModSounds.CORROSIVE_JAM.get(), SoundSource.BLOCKS, 1.0F, 0.85F);
                    notifyNearby(level, neighbor, "message.mycelialmind.pylon_smothered", ChatFormatting.RED);
                } else {
                    level.playSound(null, neighbor, ModSounds.SYNAPTIC_PULSE.get(), SoundSource.BLOCKS, 0.9F, 1.4F);
                    notifyNearby(level, neighbor, "message.mycelialmind.pylon_freed", ChatFormatting.AQUA);
                }
            }
        }
    }

    private static void notifyNearby(ServerLevel level, BlockPos pos, String key, ChatFormatting color) {
        AABB box = new AABB(pos).inflate(32.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            player.sendSystemMessage(Component.translatable(key).withStyle(color));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.antibody_cyst").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
