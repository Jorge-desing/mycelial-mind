package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.item.GeneticSampleItem;
import com.alex.mycelialmind.item.ReclaimedBloomItem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.menu.SynapticBenchMenu;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * BlockEntity for the Synaptic Research Bench.
 * Synthesizes Symbiotic Mutagens, targeted Counter-Serums, Concentrated Antidotes,
 * Chimera Ovums, and 4 discoverable experimental biotechnology formulas across Lab Tiers 1-3.
 */
public class SynapticBenchBlockEntity extends BlockEntity implements MenuProvider {
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
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    public static boolean isValidSlotInput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case SLOT_SAMPLE -> stack.is(ModItems.GENETIC_SAMPLE.get())
                    || stack.is(ModItems.SPORE_ANTIDOTE.get())
                    || isReclaimedBloom(stack);
            case SLOT_BIOMASS -> stack.is(ModItems.MYCELIAL_BIOMASS.get())
                    || stack.is(ModItems.LATENT_SPORE.get());
            case SLOT_CATALYST -> stack.is(ModItems.SYNAPTIC_CORE.get())
                    || stack.is(ModItems.PURIFIER_GRENADE.get())
                    || stack.is(Items.AMETHYST_SHARD)
                    || stack.is(ModItems.NEURAL_ECHO_CORE.get());
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

    public void setLabTier(int tier) {
        this.labTier = Math.max(1, Math.min(3, tier));
        setChanged();
        refreshOutputSlot();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
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
        if (nextTier == 0) {
            return false;
        }
        setLabTier(nextTier);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player != null && !player.isCreative()) {
            com.alex.mycelialmind.hive.SynapticMemoryRouter.routeHabit(
                    serverLevel, worldPosition, com.alex.mycelialmind.hive.PlayerHabit.BASTION_BUILDER, 3.0D * nextTier);
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

    public SynthesisResult computeSynthesisDetails(HiveProgressionTier effectiveAct) {
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        ItemStack s0 = inventory.getStackInSlot(SLOT_SAMPLE);
        ItemStack s1 = inventory.getStackInSlot(SLOT_BIOMASS);
        ItemStack s2 = inventory.getStackInSlot(SLOT_CATALYST);

        if (isReclaimedBloom(s0) && s1.is(ModItems.MYCELIAL_BIOMASS.get()) && s2.isEmpty()) {
            ItemStack output = s0.copyWithCount(2);
            return new SynthesisResult(output, -1, null);
        }

        // Recipe A: GeneticSample (typed) + MycelialBiomass + SynapticCore -> SymbioticMutagen
        if (s0.is(ModItems.GENETIC_SAMPLE.get())
                && s1.is(ModItems.MYCELIAL_BIOMASS.get())
                && s2.is(ModItems.SYNAPTIC_CORE.get())) {
            AdaptationTrait trait = GeneticSampleItem.getTrait(s0);
            if (trait != null) {
                ItemStack out = new ItemStack(ModItems.SYMBIOTIC_MUTAGEN.get());
                out.set(ModDataComponents.TRAIT_ID.get(), trait.id());
                return new SynthesisResult(out, -1, null);
            }
        }

        // Recipe B: GeneticSample (typed) + LatentSpore + (PurifierGrenade or AmethystShard) -> CounterSerum
        if (s0.is(ModItems.GENETIC_SAMPLE.get())
                && s1.is(ModItems.LATENT_SPORE.get())
                && (s2.is(ModItems.PURIFIER_GRENADE.get()) || s2.is(Items.AMETHYST_SHARD))) {
            AdaptationTrait trait = GeneticSampleItem.getTrait(s0);
            if (trait != null) {
                ItemStack out = new ItemStack(ModItems.COUNTER_SERUM.get(), labTier >= 3 ? 2 : 1);
                out.set(ModDataComponents.TRAIT_ID.get(), trait.id());
                return new SynthesisResult(out, -1, null);
            }
        }

        // Recipe C: SporeAntidote + LatentSpore + AmethystShard -> ConcentratedAntidote
        if (s0.is(ModItems.SPORE_ANTIDOTE.get())
                && s1.is(ModItems.LATENT_SPORE.get())
                && s2.is(Items.AMETHYST_SHARD)) {
            return new SynthesisResult(new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get(), labTier >= 3 ? 2 : 1), -1, null);
        }

        // Recipe D: GeneticSample (typed) + MycelialBiomass + NeuralEchoCore -> ChimeraOvum
        if (s0.is(ModItems.GENETIC_SAMPLE.get())
                && GeneticSampleItem.getTrait(s0) != null
                && s1.is(ModItems.MYCELIAL_BIOMASS.get())
                && s2.is(ModItems.NEURAL_ECHO_CORE.get())) {
            return new SynthesisResult(new ItemStack(ModItems.CHIMERA_OVUM.get()), -1, null);
        }

        // Secret #1: GeneticSample (typed) + MycelialBiomass + AmethystShard -> Artificial SynapticCore (Act II or Lab T2+)
        if (s0.is(ModItems.GENETIC_SAMPLE.get())
                && GeneticSampleItem.getTrait(s0) != null
                && s1.is(ModItems.MYCELIAL_BIOMASS.get())
                && s2.is(Items.AMETHYST_SHARD)) {
            if (safeAct.meetsRequiredAct(HiveProgressionTier.AWAKENED) || labTier >= 2) {
                return new SynthesisResult(new ItemStack(ModItems.SYNAPTIC_CORE.get()),
                        ProgressionGoalEngine.SECRET_EXP_SYNAPTIC_CORE, null);
            }
            return new SynthesisResult(ItemStack.EMPTY,
                    ProgressionGoalEngine.SECRET_EXP_SYNAPTIC_CORE, HiveProgressionTier.AWAKENED);
        }

        // Secret #2: SporeAntidote + MycelialBiomass + AmethystShard -> 2x TrucePheromone (+1 at T3) [Act I+]
        if (s0.is(ModItems.SPORE_ANTIDOTE.get())
                && s1.is(ModItems.MYCELIAL_BIOMASS.get())
                && s2.is(Items.AMETHYST_SHARD)) {
            return new SynthesisResult(new ItemStack(ModItems.TRUCE_PHEROMONE.get(), labTier >= 3 ? 3 : 2),
                    ProgressionGoalEngine.SECRET_EXP_TRUCE_PHEROMONE, null);
        }

        // Secret #3: GeneticSample (typed) + LatentSpore + SynapticCore -> VolatileEngram (Act II or Lab T2+)
        if (s0.is(ModItems.GENETIC_SAMPLE.get())
                && GeneticSampleItem.getTrait(s0) != null
                && s1.is(ModItems.LATENT_SPORE.get())
                && s2.is(ModItems.SYNAPTIC_CORE.get())) {
            if (safeAct.meetsRequiredAct(HiveProgressionTier.AWAKENED) || labTier >= 2) {
                return new SynthesisResult(new ItemStack(ModItems.VOLATILE_ENGRAM.get()),
                        ProgressionGoalEngine.SECRET_EXP_VOLATILE_ENGRAM, null);
            }
            return new SynthesisResult(ItemStack.EMPTY,
                    ProgressionGoalEngine.SECRET_EXP_VOLATILE_ENGRAM, HiveProgressionTier.AWAKENED);
        }

        // Secret #4: SporeAntidote + LatentSpore + SynapticCore -> NeuralEchoCore (Act III or Lab T3)
        if (s0.is(ModItems.SPORE_ANTIDOTE.get())
                && s1.is(ModItems.LATENT_SPORE.get())
                && s2.is(ModItems.SYNAPTIC_CORE.get())) {
            if (safeAct.meetsRequiredAct(HiveProgressionTier.ADAPTIVE) || labTier >= 3) {
                return new SynthesisResult(new ItemStack(ModItems.NEURAL_ECHO_CORE.get()),
                        ProgressionGoalEngine.SECRET_EXP_NEURAL_ECHO, null);
            }
            return new SynthesisResult(ItemStack.EMPTY,
                    ProgressionGoalEngine.SECRET_EXP_NEURAL_ECHO, HiveProgressionTier.ADAPTIVE);
        }

        return new SynthesisResult(ItemStack.EMPTY, -1, null);
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
        float preserveBiomassChance = reproducingBloom ? 0.0F : (labTier >= 3 ? 0.50F : (labTier == 2 ? 0.25F : 0.0F));
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
