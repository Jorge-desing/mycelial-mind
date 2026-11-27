package com.alex.mycelialmind.compat.vanilla;

import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.item.TrucePheromoneItem;
import com.alex.mycelialmind.network.DiplomacySyncPayload;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.network.ModNetworking;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialParticles;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Integrates Mycelial Mind organically with Vanilla Minecraft systems:
 * - Hopper / sided IItemHandler capability on the Synaptic Research Bench,
 *   Purification Turret, Purification Pylon, and Symbiotic Nodes
 * - Brewing Stand recipes for Spore Antidote and Concentrated Antidote
 * - Cleric, Mason, and Wandering Trader offers
 * - Crafting / pickup progression milestone detection
 */
public final class MycelialVanillaIntegration {
    private MycelialVanillaIntegration() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MycelialVanillaIntegration::onRegisterCapabilities);
        modEventBus.addListener(MycelialVanillaIntegration::onCommonSetup);
        modEventBus.addListener(MycelialVanillaIntegration::onConfigReloading);
        NeoForge.EVENT_BUS.register(MycelialVanillaIntegration.class);
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != MycelialConfig.SPEC) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (ServerLevel level : server.getAllLevels()) {
                HiveMindSavedData.get(level).recomputeActiveTraits(level.getGameTime());
                ModNetworking.sendToPlayersInLevelReady(level, player -> HiveSyncPayload.forPlayer(level, player));
            }
        });
    }

    public static boolean isNativelyImmuneToMycelialDebuffs(@Nullable LivingEntity entity) {
        return entity instanceof MycelialMob
                || entity instanceof PurifierDroneEntity
                || entity instanceof MycelialChimeraEntity
                || entity instanceof ColonySymbiontEntity;
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!event.getEffectInstance().is(ModEffects.MYCELIAL_INFECTION)
                && !event.getEffectInstance().is(ModEffects.SPORE_ROT)) {
            return;
        }
        if (isNativelyImmuneToMycelialDebuffs(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.HOE_TILL
                || !event.getState().is(ModBlocks.CREEPING_MYCELIUM.get())) {
            return;
        }
        event.setFinalState(Blocks.DIRT.defaultBlockState());
        if (!event.isSimulated() && event.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = event.getPos();
            serverLevel.sendParticles(ModParticles.SPORE_MOTE.get(),
                    pos.getX() + 0.5D, pos.getY() + 1.02D, pos.getZ() + 0.5D,
                    8, 0.30D, 0.08D, 0.30D, 0.02D);
        }
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DispenserBlock.registerProjectileBehavior(ModItems.PURIFIER_GRENADE.get());
            DispenserBlock.registerBehavior(ModItems.SPORE_ANTIDOTE.get(), createAntidoteDispenserBehavior(false));
            DispenserBlock.registerBehavior(ModItems.CONCENTRATED_ANTIDOTE.get(), createAntidoteDispenserBehavior(true));
            DispenserBlock.registerBehavior(ModItems.TRUCE_PHEROMONE.get(), createTrucePheromoneDispenserBehavior());
        });
    }

    private static DispenseItemBehavior createAntidoteDispenserBehavior(boolean concentrated) {
        return new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                ServerLevel level = source.level();
                BlockPos front = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                AABB box = new AABB(front).inflate(1.0D);
                List<LivingEntity> infected = level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> !(entity instanceof MycelialMob)
                                && entity.isAlive()
                                && (entity.hasEffect(ModEffects.MYCELIAL_INFECTION)
                                        || entity.hasEffect(ModEffects.SPORE_ROT)));
                if (infected.isEmpty()) {
                    return super.execute(source, stack);
                }
                for (LivingEntity target : infected) {
                    target.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE);
                    if (concentrated) {
                        target.removeEffect(ModEffects.SPORE_ROT);
                    }
                    MycelialParticles.antidote(level, target, concentrated);
                    target.addEffect(new MobEffectInstance(
                            MobEffects.REGENERATION, concentrated ? 140 : 100, concentrated ? 1 : 0));
                }
                level.playSound(null, front, ModSounds.SPORE_ANTIDOTE_CURE.get(),
                        SoundSource.BLOCKS, 0.9F, concentrated ? 1.15F : 1.0F);
                stack.shrink(1);
                return stack;
            }
        };
    }

    private static DispenseItemBehavior createTrucePheromoneDispenserBehavior() {
        return new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                ServerLevel level = source.level();
                BlockPos front = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(front).inflate(4.0D))) {
                    PlayerDiplomacyData dip = player.getData(ModAttachments.DIPLOMACY);
                    if (dip.getBetrayalCooldownTicks() <= 0) {
                        dip.setTruceTicks(TrucePheromoneItem.TRUCE_DURATION_TICKS);
                        DiplomacySyncPayload.syncToPlayer(player);
                    }
                }
                if (DimensionalHomeostasisDirector.isHomeostasisDimension(level)) {
                    DimensionalHomeostasisDirector.sedateArea(
                            level,
                            front,
                            1,
                            DimensionalHomeostasisEngine.TRUCE_PHEROMONE_SEDATION,
                            DimensionalHomeostasisEngine.TRUCE_PHEROMONE_SENSITIZATION_REDUCTION,
                            null
                    );
                }
                level.sendParticles(ModParticles.HISTAMINE_PULSE.get(),
                        front.getX() + 0.5D, front.getY() + 0.5D, front.getZ() + 0.5D,
                        18, 0.45D, 0.45D, 0.45D, 0.04D);
                level.playSound(null, front, ModSounds.TRUCE_PHEROMONE_SPRAY.get(),
                        SoundSource.BLOCKS, 0.95F, 1.0F);
                stack.shrink(1);
                return stack;
            }
        };
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SYNAPTIC_BENCH.get(),
                SidedSynapticBenchHandler::new
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.PURIFICATION_TURRET.get(),
                (turret, side) -> new PurificationTurretItemHandler(turret)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MONUMENT_FOCUS.get(),
                (focus, side) -> focus.getBlockState().is(ModBlocks.PURIFICATION_PYLON.get())
                        ? new MonumentFocusItemHandler(focus)
                        : null
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SYNAPTIC_NODE.get(),
                (node, side) -> new SynapticNodeItemHandler(node)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SOVEREIGN_NEXUS.get(),
                (nexus, side) -> new SovereignNexusItemHandler(nexus)
        );
    }

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        builder.addRecipe(
                DataComponentIngredient.of(false, PotionContents.createItemStack(Items.POTION, Potions.AWKWARD)),
                Ingredient.of(ModItems.LATENT_SPORE.get()),
                new ItemStack(ModItems.SPORE_ANTIDOTE.get())
        );
        builder.addRecipe(
                Ingredient.of(ModItems.SPORE_ANTIDOTE.get()),
                Ingredient.of(Items.AMETHYST_SHARD),
                new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get())
        );
        builder.addRecipe(
                DataComponentIngredient.of(false, PotionContents.createItemStack(Items.POTION, Potions.AWKWARD)),
                Ingredient.of(ModItems.ROOT_SAP_VIAL.get()),
                new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get())
        );
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.CLERIC) {
            event.getTrades().get(2).add(new BasicItemListing(
                    new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), 8),
                    new ItemStack(Items.EMERALD, 1),
                    8, 5, 0.05F));
            event.getTrades().get(3).add(new BasicItemListing(
                    7,
                    new ItemStack(ModItems.SPORE_ANTIDOTE.get(), 1),
                    4, 10, 0.05F));
            event.getTrades().get(4).add(new BasicItemListing(
                    8,
                    new ItemStack(ModItems.LATENT_SPORE.get(), 2),
                    3, 15, 0.05F));
            event.getTrades().get(5).add(new BasicItemListing(
                    18,
                    new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get(), 1),
                    2, 20, 0.05F));
        } else if (event.getType() == VillagerProfession.MASON) {
            event.getTrades().get(3).add(new BasicItemListing(
                    6,
                    new ItemStack(ModBlocks.MYCELIAL_ARCH_BRICK.get(), 2),
                    4, 10, 0.05F));
            event.getTrades().get(4).add(new BasicItemListing(
                    10,
                    new ItemStack(ModBlocks.CHITIN_PILLAR.get(), 1),
                    3, 15, 0.05F));
        }
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(new BasicItemListing(
                5,
                new ItemStack(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get(), 1),
                4, 5, 0.05F));
        event.getGenericTrades().add(new BasicItemListing(
                8,
                new ItemStack(ModItems.SPORE_ANTIDOTE.get(), 1),
                3, 5, 0.05F));
        event.getRareTrades().add(new BasicItemListing(
                16,
                new ItemStack(ModBlocks.LUMINESCENT_AMBER_VEIN.get(), 1),
                2, 15, 0.05F));
        event.getRareTrades().add(new BasicItemListing(
                14,
                new ItemStack(ModItems.TRUCE_PHEROMONE.get(), 1),
                2, 15, 0.05F));
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            recordMilestoneForItem(serverPlayer, event.getCrafting());
        }
    }

    @SubscribeEvent
    public static void onItemPickedUp(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            recordMilestoneForItem(serverPlayer, event.getCurrentStack());
        }
    }

    private static void recordMilestoneForItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.is(ModItems.LATENT_SPORE.get())) {
            PlayerDiscoveryData.onLatentSporeAcquired(player);
        } else if (stack.is(ModItems.BIO_SCANNER.get())) {
            PlayerDiscoveryData.unlockMilestone(player, ProgressionGoalEngine.MILESTONE_BIO_SCANNER);
        } else if (stack.is(ModBlocks.SYNAPTIC_RESEARCH_BENCH.asItem())) {
            PlayerDiscoveryData.unlockMilestone(player, ProgressionGoalEngine.MILESTONE_SYNAPTIC_BENCH);
        } else if (stack.is(ModItems.BIO_EXTRACTOR.get())) {
            PlayerDiscoveryData.unlockMilestone(player, ProgressionGoalEngine.MILESTONE_BIO_EXTRACTOR);
        } else if (stack.is(ModItems.SYMBIOTIC_MUTAGEN.get()) || stack.is(ModItems.COUNTER_SERUM.get())) {
            PlayerDiscoveryData discovery = player.getData(ModAttachments.DISCOVERY);
            boolean changed = discovery.recordMilestone(ProgressionGoalEngine.MILESTONE_SYNTHESIZED_BIOTECH);
            if (AdaptationTrait.PROJECTILE_DEFLECT.id().equals(stack.get(ModDataComponents.TRAIT_ID.get()))) {
                changed |= discovery.markResearchBit(ProgressionGoalEngine.RESEARCH_BIT_BIOTECH_SYNTHESIZED);
            }
            if (changed) {
                DiscoverySyncPayload.syncToPlayer(player);
            }
        }
    }

    private static ItemStack remainderAfterSingleUse(ItemStack stack) {
        return stack.getCount() <= 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
    }

    private static abstract class InsertOnlySingleUseHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 0;
        }
    }

    public static final class PurificationTurretItemHandler extends InsertOnlySingleUseHandler {
        private final PurificationTurretBlockEntity turret;

        public PurificationTurretItemHandler(PurificationTurretBlockEntity turret) {
            this.turret = turret;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && (turret.canCleanJamWith(stack)
                    || turret.canUpgradeWith(stack)
                    || turret.canReloadWith(stack));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            if (turret.canCleanJamWith(stack)) {
                if (!simulate && !turret.tryCleanJam(stack.copyWithCount(1), null)) {
                    return stack;
                }
                return remainderAfterSingleUse(stack);
            }
            if (turret.canUpgradeWith(stack)) {
                if (!simulate && !turret.tryUpgrade(stack.copyWithCount(1), null)) {
                    return stack;
                }
                return remainderAfterSingleUse(stack);
            }
            if (turret.canReloadWith(stack)) {
                if (!simulate && !turret.tryReload(stack.copyWithCount(1), null)) {
                    return stack;
                }
                return remainderAfterSingleUse(stack);
            }
            return stack;
        }
    }

    public static final class MonumentFocusItemHandler extends InsertOnlySingleUseHandler {
        private final MonumentFocusBlockEntity focus;

        public MonumentFocusItemHandler(MonumentFocusBlockEntity focus) {
            this.focus = focus;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            boolean rechargeAllowed = !(focus.getTier() == 1 && stack.is(ModItems.SYNAPTIC_CORE.get()))
                    && focus.canRechargeWith(stack);
            return slot == 0 && (focus.canUpgradeWith(stack) || rechargeAllowed);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            if (focus.canUpgradeWith(stack)) {
                if (!simulate && !focus.tryUpgrade(stack.copyWithCount(1), null)) {
                    return stack;
                }
                return remainderAfterSingleUse(stack);
            }
            boolean rechargeAllowed = !(focus.getTier() == 1 && stack.is(ModItems.SYNAPTIC_CORE.get()))
                    && focus.canRechargeWith(stack);
            if (rechargeAllowed) {
                if (!simulate && !focus.tryRecharge(stack.copyWithCount(1), null)) {
                    return stack;
                }
                return remainderAfterSingleUse(stack);
            }
            return stack;
        }
    }

    public static final class SynapticNodeItemHandler extends InsertOnlySingleUseHandler {
        private final SynapticNodeBlockEntity node;

        public SynapticNodeItemHandler(SynapticNodeBlockEntity node) {
            this.node = node;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && node.canRechargeSymbiotic(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !node.canRechargeSymbiotic(stack)) {
                return stack;
            }
            if (!node.tryRechargeSymbiotic(stack.copyWithCount(1), simulate)) {
                return stack;
            }
            return remainderAfterSingleUse(stack);
        }
    }

    public static final class SidedSynapticBenchHandler implements IItemHandler {
        private final SynapticBenchBlockEntity bench;
        @Nullable
        private final Direction side;

        public SidedSynapticBenchHandler(SynapticBenchBlockEntity bench, @Nullable Direction side) {
            this.bench = bench;
            this.side = side;
        }

        private int mapSlot(int localSlot) {
            if (side == null) {
                return localSlot;
            }
            if (side == Direction.UP) {
                return SynapticBenchBlockEntity.SLOT_SAMPLE;
            }
            if (side == Direction.DOWN) {
                return SynapticBenchBlockEntity.SLOT_OUTPUT;
            }
            return localSlot == 0 ? SynapticBenchBlockEntity.SLOT_BIOMASS : SynapticBenchBlockEntity.SLOT_CATALYST;
        }

        @Override
        public int getSlots() {
            if (side == null) {
                return 4;
            }
            if (side == Direction.UP || side == Direction.DOWN) {
                return 1;
            }
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                return ItemStack.EMPTY;
            }
            return bench.getInventory().getStackInSlot(mapSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= getSlots() || side == Direction.DOWN) {
                return stack;
            }
            int targetSlot = mapSlot(slot);
            if (targetSlot == SynapticBenchBlockEntity.SLOT_OUTPUT
                    || !SynapticBenchBlockEntity.isValidSlotInput(targetSlot, stack)) {
                return stack;
            }
            return bench.getInventory().insertItem(targetSlot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot < 0 || slot >= getSlots()) {
                return ItemStack.EMPTY;
            }
            int targetSlot = mapSlot(slot);
            if (targetSlot != SynapticBenchBlockEntity.SLOT_OUTPUT) {
                return ItemStack.EMPTY;
            }
            return bench.extractOutputForAutomation(amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                return 0;
            }
            return bench.getInventory().getSlotLimit(mapSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < 0 || slot >= getSlots() || side == Direction.DOWN) {
                return false;
            }
            int targetSlot = mapSlot(slot);
            return targetSlot != SynapticBenchBlockEntity.SLOT_OUTPUT
                    && SynapticBenchBlockEntity.isValidSlotInput(targetSlot, stack);
        }
    }

    private static final class SovereignNexusItemHandler implements IItemHandler {
        private final SovereignNexusBlockEntity nexus;

        private SovereignNexusItemHandler(SovereignNexusBlockEntity nexus) {
            this.nexus = nexus;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0) {
                return stack;
            }
            return nexus.insertBiomassFromAutomation(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.is(ModItems.MYCELIAL_BIOMASS.get());
        }
    }
}
