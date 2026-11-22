package com.alex.mycelialmind.menu;

import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModMenuTypes;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Server/Client container menu for the Synaptic Research Bench (3 inputs + 1 synthesis output).
 */
public class SynapticBenchMenu extends AbstractContainerMenu {
    private final SynapticBenchBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    public SynapticBenchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    public SynapticBenchMenu(int containerId, Inventory playerInventory, SynapticBenchBlockEntity blockEntity) {
        super(ModMenuTypes.SYNAPTIC_BENCH.get(), containerId);
        this.blockEntity = blockEntity;
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        var inv = blockEntity.getInventory();
        // 3 input slots
        this.addSlot(new SlotItemHandler(inv, SynapticBenchBlockEntity.SLOT_SAMPLE, 34, 34));
        this.addSlot(new SlotItemHandler(inv, SynapticBenchBlockEntity.SLOT_BIOMASS, 56, 34));
        this.addSlot(new SlotItemHandler(inv, SynapticBenchBlockEntity.SLOT_CATALYST, 78, 34));

        // Synthesis output slot
        this.addSlot(new SlotItemHandler(inv, SynapticBenchBlockEntity.SLOT_OUTPUT, 130, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                var details = blockEntity.computeSynthesisDetails();
                if (blockEntity.isOutputInputsConsumed()) {
                    blockEntity.refreshOutputSlot();
                } else {
                    blockEntity.consumeInputsForSynthesis();
                }
                if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
                    blockEntity.getLevel().playSound(null, blockEntity.getBlockPos(),
                            ModSounds.SYNAPTIC_BENCH_SYNTHESIZE.get(), SoundSource.BLOCKS, 0.85F, 1.0F);
                    if (player instanceof ServerPlayer serverPlayer && !details.output().isEmpty()) {
                        PlayerDiscoveryData.unlockMilestone(
                                serverPlayer, ProgressionGoalEngine.MILESTONE_SYNTHESIZED_BIOTECH);
                        if (details.secretBit() >= 0) {
                            PlayerDiscoveryData.unlockSecret(serverPlayer, details.secretBit());
                        }
                    }
                }
                super.onTake(player, stack);
            }
        });

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar (1x9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static SynapticBenchBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof SynapticBenchBlockEntity bench) {
            return bench;
        }
        throw new IllegalStateException("Expected SynapticBenchBlockEntity at menu position.");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == SynapticBenchBlockEntity.SLOT_OUTPUT) {
            if (!this.moveItemStackTo(stack, 4, 40, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index >= 4) {
            if (!this.moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 4, 40, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    public int getLabTier() {
        return blockEntity.getLabTier();
    }

    @javax.annotation.Nullable
    public HiveProgressionTier getBlockedAct(HiveProgressionTier clientEffectiveAct) {
        return blockEntity.computeSynthesisDetails(clientEffectiveAct).blockedByAct();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());
    }
}
