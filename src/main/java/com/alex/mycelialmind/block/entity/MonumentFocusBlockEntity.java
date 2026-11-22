package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * BlockEntity anchoring animated 3D BlockEntityRenderers and persistent tier state
 * on Purification Pylons (T1-T3) and Ritual Altars.
 */
public class MonumentFocusBlockEntity extends BlockEntity {
    private static final String TAG_PYLON_TIER = "PylonTier";

    private int tier = 1;

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
                com.alex.mycelialmind.world.MycelialChunkTicketManager.syncPylonChunkTicket(serverLevel, this.worldPosition);
            }
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, 3);
            serverLevel.updateNeighbourForOutputSignal(this.worldPosition, state.getBlock());
        }
    }

    public boolean canUpgradeWith(ItemStack catalyst) {
        if (catalyst.isEmpty() || !getBlockState().is(ModBlocks.PURIFICATION_PYLON.get())) {
            return false;
        }
        if (this.tier == 1 && catalyst.is(ModItems.SYNAPTIC_CORE.get())) {
            return true;
        }
        return this.tier == 2
                && (catalyst.is(ModItems.OVERMIND_ESSENCE.get()) || catalyst.is(ModItems.TIDE_BEACON.get()));
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
        int nextTier = 0;
        if (this.tier == 1 && catalyst.is(ModItems.SYNAPTIC_CORE.get())) {
            nextTier = 2;
        } else if (this.tier == 2
                && (catalyst.is(ModItems.OVERMIND_ESSENCE.get()) || catalyst.is(ModItems.TIDE_BEACON.get()))) {
            nextTier = 3;
        }
        if (nextTier == 0) {
            return false;
        }
        setTier(nextTier);
        if (player == null || !player.getAbilities().instabuild) {
            catalyst.shrink(1);
        }
        if (player != null && !player.isCreative()) {
            com.alex.mycelialmind.hive.SynapticMemoryRouter.routeHabit(
                    serverLevel, worldPosition, com.alex.mycelialmind.hive.PlayerHabit.BASTION_BUILDER, 4.0D * nextTier);
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
                if (nextTier >= 3 && serverPlayer.getServer() != null) {
                    AdvancementHolder adv = serverPlayer.getServer().getAdvancements()
                            .get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "bastion_tier3"));
                    if (adv != null) {
                        serverPlayer.getAdvancements().award(adv, "upgrade_tier3");
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.tier > 1) {
            tag.putInt(TAG_PYLON_TIER, this.tier);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_PYLON_TIER)) {
            this.tier = Math.max(1, Math.min(3, tag.getInt(TAG_PYLON_TIER)));
        } else {
            this.tier = 1;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
