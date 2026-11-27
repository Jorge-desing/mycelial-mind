package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.hive.AcquisitionDifficultyEngine;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.SynapticMemoryRouter;
import com.alex.mycelialmind.registry.ModAdvancements;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.MycelialChunkTicketManager;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * BlockEntity anchoring animated 3D BlockEntityRenderers and persistent tier state
 * on Purification Pylons (T1-T3) and Ritual Altars.
 */
public class MonumentFocusBlockEntity extends TieredStationBlockEntity {
    private static final String TAG_PYLON_TIER = "PylonTier";
    private static final String TAG_PURIFICATION_CHARGE = "PurificationCharge";

    private int tier = 1;
    private int purificationCharge = AcquisitionDifficultyEngine.PYLON_INITIAL_CHARGE;

    public MonumentFocusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MONUMENT_FOCUS.get(), pos, state);
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(1, Math.min(3, tier));
        setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            if (getBlockState().is(ModBlocks.PURIFICATION_PYLON.get())) {
                PurificationZoneTracker.get(serverLevel).setPylonTier(this.worldPosition, this.tier);
                MycelialChunkTicketManager.syncPylonChunkTicket(serverLevel, this.worldPosition);
            }
            syncToClients();
            serverLevel.updateNeighbourForOutputSignal(this.worldPosition, getBlockState().getBlock());
        }
    }

    public boolean canUpgradeWith(ItemStack catalyst) {
        int nextTier = resolveNextTierForCatalyst(catalyst);
        return nextTier > 0 && canPassTierUpgradeGate(nextTier, null, false);
    }

    public int resolveNextTierForCatalyst(ItemStack catalyst) {
        if (catalyst.isEmpty() || !getBlockState().is(ModBlocks.PURIFICATION_PYLON.get())) {
            return 0;
        }
        if (this.tier == 1 && catalyst.is(ModItems.SYNAPTIC_CORE.get())) {
            return 2;
        }
        if (this.tier == 2
                && (catalyst.is(ModItems.OVERMIND_ESSENCE.get()) || catalyst.is(ModItems.TIDE_BEACON.get()))) {
            return 3;
        }
        return 0;
    }

    public int getEffectiveRadius(int baseRadius) {
        return switch (this.tier) {
            case 2 -> baseRadius + 6;
            case 3 -> baseRadius + 12;
            default -> baseRadius;
        };
    }

    public boolean tryUpgrade(ItemStack catalyst, @Nullable Player player) {
        if (!(this.level instanceof ServerLevel serverLevel)
                || !getBlockState().is(ModBlocks.PURIFICATION_PYLON.get())
                || catalyst.isEmpty()) {
            return false;
        }
        int nextTier = resolveNextTierForCatalyst(catalyst);
        if (nextTier == 0 || !canPassTierUpgradeGate(nextTier, player, true)) {
            return false;
        }
        setTier(nextTier);
        if (!isCreativeOrInstabuild(player)) {
            catalyst.shrink(1);
        }
        if (player != null && !player.isCreative()) {
            SynapticMemoryRouter.routeHabit(
                    serverLevel, worldPosition, PlayerHabit.BASTION_BUILDER, 4.0D * nextTier);
        }
        int effectiveRadius = getEffectiveRadius(MycelialConfig.PYLON_PURIFICATION_RADIUS.get());
        serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.05);
        serverLevel.playSound(null, worldPosition, ModSounds.PURIFICATION_TURRET_UPGRADE.get(),
                SoundSource.BLOCKS, 1.0F, 0.95F + nextTier * 0.1F);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.pylon_upgraded", nextTier, effectiveRadius)
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerDiscoveryData.unlockMilestone(serverPlayer, ProgressionGoalEngine.MILESTONE_STATION_UPGRADED);
                if (nextTier >= 3) {
                    ModAdvancements.award(serverPlayer, ModAdvancements.BASTION_TIER3);
                }
            }
        }
        return true;
    }

    public int getPurificationCharge() {
        return this.purificationCharge;
    }

    public boolean isDepleted() {
        return this.purificationCharge <= 0;
    }

    public void setPurificationCharge(int charge) {
        int clamped = Math.max(0, Math.min(AcquisitionDifficultyEngine.PYLON_MAX_CHARGE, charge));
        if (this.purificationCharge == clamped) {
            return;
        }
        int previous = this.purificationCharge;
        this.purificationCharge = clamped;
        if (this.level instanceof ServerLevel serverLevel) {
            if (getBlockState().is(ModBlocks.PURIFICATION_PYLON.get())) {
                PurificationZoneTracker.get(serverLevel)
                        .setPylonDepleted(getBlockPos(), this.purificationCharge <= 0);
                boolean crossedLow = previous > 15 && clamped <= 15 && clamped > 0;
                boolean crossedEmpty = previous > 0 && clamped == 0;
                if (crossedLow || crossedEmpty) {
                    serverLevel.playSound(null, this.worldPosition, ModSounds.STATION_UPKEEP_LOW.get(),
                            SoundSource.BLOCKS, crossedEmpty ? 0.90F : 0.65F, crossedEmpty ? 0.86F : 1.0F);
                }
            }
            syncToClients();
        }
        setChanged();
    }

    public void consumePurificationCharge(int amount) {
        if (amount <= 0 || this.purificationCharge <= 0) {
            return;
        }
        setPurificationCharge(this.purificationCharge - amount);
    }

    public boolean canRechargeWith(ItemStack catalyst) {
        if (catalyst.isEmpty() || !getBlockState().is(ModBlocks.PURIFICATION_PYLON.get())) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(catalyst.getItem()).getPath();
        return AcquisitionDifficultyEngine.pylonRechargeAmount(path) > 0
                && this.purificationCharge < AcquisitionDifficultyEngine.PYLON_MAX_CHARGE;
    }

    public boolean tryRecharge(ItemStack catalyst, @Nullable Player player) {
        if (!(this.level instanceof ServerLevel)
                || !getBlockState().is(ModBlocks.PURIFICATION_PYLON.get())
                || catalyst.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(catalyst.getItem()).getPath();
        int gain = AcquisitionDifficultyEngine.pylonRechargeAmount(path);
        if (gain <= 0 || this.purificationCharge >= AcquisitionDifficultyEngine.PYLON_MAX_CHARGE) {
            return false;
        }
        setPurificationCharge(this.purificationCharge + gain);
        if (!isCreativeOrInstabuild(player)) {
            catalyst.shrink(1);
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.tier > 1) {
            tag.putInt(TAG_PYLON_TIER, this.tier);
        }
        tag.putInt(TAG_PURIFICATION_CHARGE, this.purificationCharge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_PYLON_TIER)) {
            this.tier = Math.max(1, Math.min(3, tag.getInt(TAG_PYLON_TIER)));
        } else {
            this.tier = 1;
        }
        if (tag.contains(TAG_PURIFICATION_CHARGE)) {
            this.purificationCharge = Math.max(0, Math.min(
                    AcquisitionDifficultyEngine.PYLON_MAX_CHARGE,
                    tag.getInt(TAG_PURIFICATION_CHARGE)));
        } else {
            this.purificationCharge = AcquisitionDifficultyEngine.PYLON_INITIAL_CHARGE;
        }
    }
}
