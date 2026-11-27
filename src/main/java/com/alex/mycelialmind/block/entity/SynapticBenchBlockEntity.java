package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.block.LuminescentAmberVeinBlock;
import com.alex.mycelialmind.hive.DamageTelemetryTracker;
import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.SynapticMemoryRouter;
import com.alex.mycelialmind.item.ReclaimedBloomItem;
import com.alex.mycelialmind.menu.SynapticBenchMenu;
import com.alex.mycelialmind.recipe.SynapticSynthesisInput;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModRecipes;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * BlockEntity for the Synaptic Research Bench.
 * Synthesizes Symbiotic Mutagens, targeted Counter-Serums, Concentrated Antidotes,
 * Chimera Ovums, and 4 discoverable experimental biotechnology formulas across Lab Tiers 1-3.
 */
public class SynapticBenchBlockEntity extends TieredStationBlockEntity implements MenuProvider {
    public static final int SLOT_SAMPLE = 0;
    public static final int SLOT_BIOMASS = 1;
    public static final int SLOT_CATALYST = 2;
    public static final int SLOT_OUTPUT = 3;

    private static final String TAG_LAB_TIER = "LabTier";

    public record SynthesisResult(ItemStack output, int secretBit, @Nullable HiveProgressionTier blockedByAct) {
    }

    private int labTier = 1;
    private boolean outputInputsConsumed = false;
    private boolean consumingInputs = false;

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidSlotInput(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot != SLOT_OUTPUT && !consumingInputs) {
                if (!outputInputsConsumed) refreshOutputSlot();
            }
            syncToClients();
        }
    };

    public static boolean isValidSlotInput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case SLOT_SAMPLE -> stack.is(ModItems.GENETIC_SAMPLE.get())
                    || stack.is(ModItems.SPORE_ANTIDOTE.get())
                    || stack.is(ModItems.ROOT_SAP_VIAL.get())
                    || isReclaimedBloom(stack);
            case SLOT_BIOMASS -> stack.is(ModItems.MYCELIAL_BIOMASS.get())
                    || stack.is(ModItems.LATENT_SPORE.get());
            case SLOT_CATALYST -> stack.is(ModItems.SYNAPTIC_CORE.get())
                    || stack.is(ModItems.PURIFIER_GRENADE.get())
                    || stack.is(Items.AMETHYST_SHARD)
                    || stack.is(ModItems.NEURAL_ECHO_CORE.get())
                    || stack.is(ModItems.ROOT_HEART_RESIN.get());
            default -> false;
        };
    }

    private static boolean isReclaimedBloom(ItemStack stack) {
        return stack.is(ModBlocks.RECLAIMED_BLOOM.get().asItem())
                || stack.getItem() instanceof ReclaimedBloomItem;
    }

    public SynapticBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SYNAPTIC_BENCH.get(), pos, state);
    }

    public int getLabTier() {
        return labTier;
    }

    public int getEffectiveLabTier() {
        boolean hasAmberConduit = this.level != null
                && LuminescentAmberVeinBlock.hasAdjacentConduit(this.level, this.worldPosition);
        return EcosystemPurposeEngine.computeBenchEffectiveLabTier(this.labTier, hasAmberConduit);
    }

    public void setLabTier(int tier) {
        this.labTier = Math.max(1, Math.min(3, tier));
        setChanged();
        refreshOutputSlot();
        syncToClients();
    }

    public boolean tryUpgradeLab(ItemStack stack, @Nullable Player player) {
        if (!(this.level instanceof ServerLevel serverLevel) || stack.isEmpty()) {
            return false;
        }
        int nextTier = 0;
        if (this.labTier == 1 && stack.is(ModItems.SYNAPTIC_CORE.get())) {
            nextTier = 2;
        } else if (this.labTier == 2
                && (stack.is(ModItems.NEURAL_ECHO_CORE.get()) || stack.is(ModItems.OVERMIND_ESSENCE.get()))) {
            nextTier = 3;
        }
        if (nextTier == 0 || !canPassTierUpgradeGate(nextTier, player, true)) {
            return false;
        }
        setLabTier(nextTier);
        if (!isCreativeOrInstabuild(player)) {
            stack.shrink(1);
        }
        if (player != null && !player.isCreative()) {
            SynapticMemoryRouter.routeHabit(
                    serverLevel, worldPosition, PlayerHabit.BASTION_BUILDER, 3.0D * nextTier);
        }
        serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.15, worldPosition.getZ() + 0.5,
                18, 0.3, 0.25, 0.3, 0.04);
        serverLevel.playSound(null, worldPosition, ModSounds.PURIFICATION_TURRET_UPGRADE.get(),
                SoundSource.BLOCKS, 0.95F, 1.0F + nextTier * 0.1F);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.bench_upgraded", nextTier)
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerDiscoveryData.unlockMilestone(serverPlayer, ProgressionGoalEngine.MILESTONE_STATION_UPGRADED);
            }
        }
        return true;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void refreshOutputSlot() {
        if (outputInputsConsumed && !inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) return;
        ItemStack result = computeSynthesisResult();
        inventory.setStackInSlot(SLOT_OUTPUT, result);
    }

    public ItemStack computeSynthesisResult() {
        return computeSynthesisDetails().output();
    }

    public SynthesisResult computeSynthesisDetails() {
        HiveProgressionTier effectiveAct = this.level instanceof ServerLevel sl
                ? StructureProgressionPolicy.resolveTier(sl)
                : HiveProgressionTier.DORMANT;
        return computeSynthesisDetails(effectiveAct);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null && !level.isClientSide() && !outputInputsConsumed) {
            refreshOutputSlot();
        }
    }

    public SynthesisResult computeSynthesisDetails(HiveProgressionTier effectiveAct) {
        if (this.level == null) {
            return new SynthesisResult(ItemStack.EMPTY, -1, null);
        }
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        int effectiveLabTier = getEffectiveLabTier();
        boolean hiveCoreDefeated = this.level instanceof ServerLevel sl
                && DamageTelemetryTracker.isHiveCoreDefeated(sl, null);
        ItemStack s0 = inventory.getStackInSlot(SLOT_SAMPLE);
        ItemStack s1 = inventory.getStackInSlot(SLOT_BIOMASS);
        ItemStack s2 = inventory.getStackInSlot(SLOT_CATALYST);
        SynapticSynthesisInput input = new SynapticSynthesisInput(s0, s1, s2);
        return this.level.getRecipeManager()
                .getRecipeFor(ModRecipes.SYNAPTIC_SYNTHESIS_TYPE.get(), input, this.level)
                .map(holder -> holder.value().evaluate(input, safeAct, effectiveLabTier, hiveCoreDefeated))
                .orElseGet(() -> new SynthesisResult(ItemStack.EMPTY, -1, null));
    }

    public void consumeInputsForSynthesis() {
        consumeInputIngredientsOnly();
        outputInputsConsumed = false;
        refreshOutputSlot();
    }

    public void finishManualOutputTake() {
        if (!outputInputsConsumed) consumeInputIngredientsOnly();
        outputInputsConsumed = !inventory.getStackInSlot(SLOT_OUTPUT).isEmpty();
        if (!outputInputsConsumed) refreshOutputSlot();
        setChanged();
    }

    private void consumeInputIngredientsOnly() {
        boolean reproducingBloom = isReclaimedBloom(inventory.getStackInSlot(SLOT_SAMPLE));
        int effectiveLabTier = getEffectiveLabTier();
        float preserveBiomassChance = reproducingBloom ? 0.0F : (effectiveLabTier >= 3 ? 0.50F : (effectiveLabTier == 2 ? 0.25F : 0.0F));
        consumingInputs = true;
        try {
        for (int i = 0; i < SLOT_OUTPUT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (i == SLOT_BIOMASS
                        && preserveBiomassChance > 0.0F
                        && level != null
                        && !level.isClientSide()
                        && level.random.nextFloat() < preserveBiomassChance) {
                    continue;
                }
                stack.shrink(1);
                inventory.setStackInSlot(i, stack);
            }
        }
        } finally {
            consumingInputs = false;
        }
    }

    public ItemStack extractOutputForAutomation(int amount, boolean simulate) {
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (simulate) {
            return inventory.extractItem(SLOT_OUTPUT, amount, true);
        }
        if (!outputInputsConsumed) {
            consumeInputIngredientsOnly();
            outputInputsConsumed = true;
            setChanged();
        }
        ItemStack extracted = inventory.extractItem(SLOT_OUTPUT, amount, false);
        if (inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            outputInputsConsumed = false;
            refreshOutputSlot();
        }
        return extracted;
    }

    public boolean isOutputInputsConsumed() {
        return outputInputsConsumed;
    }

    public void dropContents() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        for (int i = 0; i < SLOT_OUTPUT; i++) {
            ItemStack stack = inventory.extractItem(i, inventory.getStackInSlot(i).getCount(), false);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(this.level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
        if (outputInputsConsumed) {
            ItemStack output = inventory.extractItem(SLOT_OUTPUT, inventory.getStackInSlot(SLOT_OUTPUT).getCount(), false);
            if (!output.isEmpty()) Containers.dropItemStack(this.level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mycelialmind.synaptic_research_bench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SynapticBenchMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putBoolean("OutputInputsConsumed", outputInputsConsumed);
        if (labTier > 1) {
            tag.putInt(TAG_LAB_TIER, labTier);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.labTier = tag.contains(TAG_LAB_TIER) ? Math.max(1, Math.min(3, tag.getInt(TAG_LAB_TIER))) : 1;
        outputInputsConsumed = tag.getBoolean("OutputInputsConsumed");
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) outputInputsConsumed = false;
        refreshOutputSlot();
    }
}
