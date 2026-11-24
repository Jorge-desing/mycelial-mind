package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.colony.SymbiontTemperament;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.ai.colony.SymbiontArchitectGoal;
import com.alex.mycelialmind.entity.ai.colony.SymbiontExcavatorGoal;
import com.alex.mycelialmind.entity.ai.colony.SymbiontHarvesterGoal;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BlockEntity anchoring a player's Endgame Sovereign Hive Colony.
 * Manages biomass storage, Symbiotic Node outpost links, ColonyOrderBoard scans,
 * worker incubation, tier upgrades, and hopper biomass intake.
 */
public class SovereignNexusBlockEntity extends BlockEntity {
    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final int OUTPOST_SCAN_RADIUS = 64;

    @Nullable
    private UUID ownerUuid;
    private String ownerName = "Sovereign";
    private int tickCounter = 0;

    public SovereignNexusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOVEREIGN_NEXUS.get(), pos, state);
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
        this.ownerName = player.getName().getString();
        setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            SovereignColonyData data = getOrCreateColony(serverLevel);
            if (player.isCreative()) data.enableAdministrativeOverride();
            data.setOwnerId(this.ownerUuid);
            data.setOwnerName(this.ownerName);
            SovereignColonySavedData.get(serverLevel).setDirty();
        }
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public SovereignColonyData getOrCreateColony(ServerLevel level) {
        UUID resolvedOwner = ownerUuid != null ? ownerUuid : new UUID(0L, 0L);
        return SovereignColonySavedData.get(level).getOrCreateColony(this.worldPosition, resolvedOwner, this.ownerName);
    }

    public static boolean isSovereignUnlocked(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return HiveMindSavedData.get(overworld).isMatriarchDefeated()
                || HiveMindSavedData.get(level).isMatriarchDefeated();
    }

    public static boolean isColonyActive(ServerLevel level, @Nullable BlockPos nexusPos) {
        if (isSovereignUnlocked(level)) return true;
        SovereignColonyData colony = nexusPos == null ? null : SovereignColonySavedData.get(level).getByNexus(nexusPos);
        return colony != null && colony.hasAdministrativeOverride();
    }

    public void enableAdministrativeOverride(ServerLevel level) {
        getOrCreateColony(level).enableAdministrativeOverride();
        SovereignColonySavedData.get(level).setDirty();
    }

    public ItemStack insertBiomassFromAutomation(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !stack.is(ModItems.MYCELIAL_BIOMASS.get()) || !(this.level instanceof ServerLevel serverLevel)) {
            return stack;
        }
        if (!isColonyActive(serverLevel, worldPosition)) {
            return stack;
        }
        SovereignColonyData colony = getOrCreateColony(serverLevel);
        int room = SovereignColonyEngine.MAX_BIOMASS - colony.getBiomassStored();
        if (room <= 0) {
            return stack;
        }
        int accept = Math.min(room, stack.getCount());
        if (!simulate) {
            colony.addBiomass(accept);
            colony.recalculateMorale();
            SovereignColonySavedData.get(serverLevel).setDirty();
            setChanged();
        }
        ItemStack remainder = stack.copy();
        remainder.shrink(accept);
        return remainder;
    }

    @Nullable
    public ColonySymbiontEntity incubateSymbiont(
            ServerLevel level,
            @Nullable Player initiator,
            SymbiontCaste caste,
            boolean bypassCostAndGate
    ) {
        if (!isColonyActive(level, worldPosition) && !bypassCostAndGate && (initiator == null || !initiator.isCreative())) {
            if (initiator != null) {
                initiator.displayClientMessage(
                        Component.translatable("message.mycelialmind.sovereign_nexus_locked").withStyle(ChatFormatting.DARK_PURPLE),
                        true);
            }
            return null;
        }

        if (bypassCostAndGate || (initiator != null && initiator.isCreative())) enableAdministrativeOverride(level);
        SovereignColonyData colony = getOrCreateColony(level);
        if (initiator != null && (this.ownerUuid == null || this.ownerUuid.getMostSignificantBits() == 0L)) {
            setOwner(initiator);
        }

        // Sync live entities in range to prune stale/dead entries before checking cap
        syncLiveWorkers(level, colony);
        if (!bypassCostAndGate && (initiator == null || !initiator.isCreative())
                && colony.getRoster().size() >= colony.getPopulationCap()) {
            if (initiator != null) {
                initiator.displayClientMessage(
                        Component.translatable("message.mycelialmind.colony_pop_cap",
                                colony.getRoster().size(), colony.getPopulationCap()).withStyle(ChatFormatting.RED),
                        true);
            }
            return null;
        }

        int cost = SovereignColonyEngine.getIncubationBiomassCost(colony.getTier());
        if (!bypassCostAndGate && (initiator == null || !initiator.isCreative()) && !colony.consumeBiomass(cost)) {
            if (initiator != null) {
                initiator.displayClientMessage(
                        Component.translatable("message.mycelialmind.colony_need_biomass", cost).withStyle(ChatFormatting.RED),
                        true);
            }
            return null;
        }

        ColonySymbiontEntity worker = ModEntities.COLONY_SYMBIONT.get().create(level);
        if (worker == null) {
            return null;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double spawnX = worldPosition.getX() + 0.5D + Math.cos(angle) * 2.2D;
        double spawnZ = worldPosition.getZ() + 0.5D + Math.sin(angle) * 2.2D;
        worker.moveTo(spawnX, worldPosition.getY() + 0.2D, spawnZ, level.random.nextFloat() * 360.0F, 0.0F);
        worker.finalizeSpawn(level, level.getCurrentDifficultyAt(worldPosition), MobSpawnType.TRIGGERED, null);

        SymbiontTemperament[] temps = SymbiontTemperament.values();
        worker.setTemperament(temps[level.random.nextInt(temps.length)]);
        worker.setOwnerUuid(colony.getOwnerId());
        worker.setNexusPos(worldPosition);
        worker.setCaste(caste);
        if (!level.addFreshEntity(worker)) {
            return null;
        }
        worker.syncRosterToColony();
        SovereignColonySavedData.get(level).setDirty();
        level.playSound(null, worldPosition, ModSounds.SOVEREIGN_NEXUS_PULSE.get(), SoundSource.BLOCKS, 1.0F, 1.15F);
        level.sendParticles(ModParticles.BIO_LUME.get(),
                spawnX, worldPosition.getY() + 0.8D, spawnZ, 16, 0.4, 0.4, 0.4, 0.04);
        return worker;
    }

    public boolean upgradeTier(ServerLevel level, @Nullable Player player, boolean bypassCost) {
        if (!isColonyActive(level, worldPosition) && !bypassCost && (player == null || !player.isCreative())) {
            return false;
        }
        if (bypassCost || (player != null && player.isCreative())) enableAdministrativeOverride(level);
        SovereignColonyData colony = getOrCreateColony(level);
        int current = colony.getTier();
        if (current >= SovereignColonyEngine.MAX_TIER) {
            return false;
        }
        if (!bypassCost && (player == null || !player.isCreative())) {
            if (!SovereignColonyEngine.canUpgradeTier(current, colony.getBiomassStored(), colony.getDomainXp())) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable(
                            "message.mycelialmind.colony_upgrade_requirements",
                            SovereignColonyEngine.getUpgradeBiomassCost(current),
                            SovereignColonyEngine.getUpgradeDomainXpRequirement(current)
                    ).withStyle(ChatFormatting.YELLOW), true);
                }
                return false;
            }
            colony.consumeBiomass(SovereignColonyEngine.getUpgradeBiomassCost(current));
        }
        colony.setTier(current + 1);
        colony.recalculateMorale();
        SovereignColonySavedData.get(level).setDirty();
        level.playSound(null, worldPosition, ModSounds.COLONY_SYMBIONT_PROMOTE.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                22, 0.6, 0.5, 0.6, 0.05);
        if (colony.getTier() >= 3 && player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
            var adv = serverPlayer.getServer().getAdvancements().get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            com.alex.mycelialmind.MycelialMind.MODID, "sovereign_colony_tier3"));
            if (adv != null) {
                serverPlayer.getAdvancements().award(adv, "upgrade_tier3");
            }
        }
        return true;
    }

    public int recallAllWorkers(ServerLevel level) {
        if (!isColonyActive(level, worldPosition)) return 0;
        SovereignColonyData colony = getOrCreateColony(level);
        colony.getOrderBoard().clear();
        List<ColonySymbiontEntity> workers = findColonyWorkers(level, 128.0D);
        int count = 0;
        for (ColonySymbiontEntity worker : workers) {
            double angle = (Math.PI * 2.0D * count) / Math.max(1, workers.size());
            worker.teleportTo(
                    worldPosition.getX() + 0.5D + Math.cos(angle) * 2.0D,
                    worldPosition.getY() + 0.3D,
                    worldPosition.getZ() + 0.5D + Math.sin(angle) * 2.0D
            );
            worker.getNavigation().stop();
            count++;
        }
        level.playSound(null, worldPosition, ModSounds.SOVEREIGN_NEXUS_PULSE.get(), SoundSource.BLOCKS, 0.9F, 1.25F);
        return count;
    }

    public boolean setWorkerCaste(ServerLevel level, UUID workerId, SymbiontCaste caste) {
        if (!isColonyActive(level, worldPosition)) return false;
        if (level.getEntity(workerId) instanceof ColonySymbiontEntity worker && worker.isAlive()) {
            worker.setCaste(caste);
            level.playSound(null, worker.blockPosition(), ModSounds.COMMAND_STAFF_MARK.get(), SoundSource.BLOCKS, 0.7F, 1.2F);
            return true;
        }
        return false;
    }

    public boolean toggleWorkerStandby(ServerLevel level, UUID workerId) {
        if (!isColonyActive(level, worldPosition)) return false;
        if (level.getEntity(workerId) instanceof ColonySymbiontEntity worker && worker.isAlive()) {
            worker.setStandby(!worker.isStandby());
            return true;
        }
        return false;
    }

    public List<ColonySymbiontEntity> findColonyWorkers(ServerLevel level, double range) {
        return level.getEntitiesOfClass(ColonySymbiontEntity.class, new AABB(worldPosition).inflate(range),
                w -> w.isAlive() && (worldPosition.equals(w.getNexusPos())
                        || (ownerUuid != null && ownerUuid.equals(w.getOwnerUuid()) && w.getNexusPos() == null)));
    }

    public void syncLiveWorkers(ServerLevel level, SovereignColonyData colony) {
        List<ColonySymbiontEntity> live = findColonyWorkers(level, 128.0D);
        List<UUID> liveIds = new ArrayList<>();
        for (ColonySymbiontEntity w : live) {
            if (w.getNexusPos() == null) {
                w.setNexusPos(worldPosition);
            }
            w.syncRosterToColony();
            liveIds.add(w.getUUID());
        }
        List<UUID> toRemove = new ArrayList<>();
        for (UUID id : colony.getRoster().keySet()) {
            if (!liveIds.contains(id) && level.getEntity(id) == null) {
                toRemove.add(id);
            }
        }
        for (UUID deadId : toRemove) {
            colony.removeWorker(deadId);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SovereignNexusBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel) || !isColonyActive(serverLevel, pos)) {
            return;
        }
        be.tickCounter++;
        if (be.tickCounter % 40 == 1) {
            SovereignColonyData colony = be.getOrCreateColony(serverLevel);
            List<BlockPos> outposts = PurificationZoneTracker.get(serverLevel)
                    .getNearbySymbioticOutposts(pos, OUTPOST_SCAN_RADIUS);
            for (BlockPos knownOutpost : colony.getLinkedOutposts()) {
                if (!serverLevel.isLoaded(knownOutpost) && !outposts.contains(knownOutpost)) {
                    outposts.add(knownOutpost);
                }
            }
            colony.replaceLinkedOutposts(outposts);
            be.syncLiveWorkers(serverLevel, colony);
            colony.recalculateMorale();
            serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.25, 0.15, 0.25, 0.01);
        }
        if (be.tickCounter % 60 == 20) {
            SovereignColonyData colony = be.getOrCreateColony(serverLevel);
            be.refreshOrderBoard(serverLevel, colony);
        }
    }

    public void refreshOrderBoard(ServerLevel level, SovereignColonyData colony) {
        long now = level.getGameTime();
        colony.getOrderBoard().purgeExpired(now);

        // 1. Harvest orders from marked harvest zones and immediate Nexus/outpost flora
        for (BlockPos zone : colony.getWorkZones(SovereignColonyData.WorkZoneType.HARVEST)) {
            scanHarvestCube(level, colony, zone, 3, 2, 70, now);
        }
        scanHarvestCube(level, colony, worldPosition, 6, 2, 55, now);

        // 2. Excavate orders inside marked MINE zones
        for (BlockPos mineAnchor : colony.getWorkZones(SovereignColonyData.WorkZoneType.MINE)) {
            scanMineCube(level, colony, mineAnchor, 2, 2, 60, now);
        }

        // 3. Courier orders when INPUT chests hold items and (OUTPUT chest exists or Nexus needs biomass)
        if (!colony.getInputChests().isEmpty()
                && (!colony.getOutputChests().isEmpty() || colony.getBiomassStored() < 192)) {
            for (BlockPos inputChest : colony.getInputChests()) {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputChest, null);
                if (handler != null && hasAnyItems(handler)) {
                    colony.getOrderBoard().postOrRefreshOrder(ColonyOrder.OrderType.COURIER_PICKUP, inputChest, 65, now);
                }
            }
        }

        // 4. Architect orders for damaged Symbiotic outposts, jammed turrets, creeping mycelium, or BUILD zones
        for (BlockPos outpost : colony.getLinkedOutposts()) {
            if (level.getBlockState(outpost).getBlock() instanceof SymbioticNodeBlock
                    && SymbiontArchitectGoal.needsArchitectAction(level, colony, outpost)) {
                colony.getOrderBoard().postOrRefreshOrder(ColonyOrder.OrderType.REPAIR_STRUCTURE, outpost, 80, now);
            }
        }
        for (BlockPos buildAnchor : colony.getWorkZones(SovereignColonyData.WorkZoneType.BUILD)) {
            if (SymbiontArchitectGoal.needsArchitectAction(level, colony, buildAnchor)) {
                colony.getOrderBoard().postOrRefreshOrder(ColonyOrder.OrderType.REPAIR_STRUCTURE, buildAnchor, 55, now);
            }
        }
        for (BlockPos check : BlockPos.betweenClosed(
                worldPosition.offset(-8, -2, -8),
                worldPosition.offset(8, 3, 8))) {
            if (level.getBlockState(check).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                colony.getOrderBoard().postOrRefreshOrder(ColonyOrder.OrderType.REPAIR_STRUCTURE, check, 70, now);
                break;
            }
        }
    }

    private void scanHarvestCube(
            ServerLevel level,
            SovereignColonyData colony,
            BlockPos center,
            int rx,
            int ry,
            int priority,
            long now
    ) {
        int posted = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-rx, -ry, -rx), center.offset(rx, ry, rx))) {
            if (SymbiontHarvesterGoal.isHarvestable(level.getBlockState(p))) {
                colony.getOrderBoard().postOrRefreshOrder(ColonyOrder.OrderType.HARVEST, p, priority, now);
                if (++posted >= 10) {
                    return;
                }
            }
        }
    }

    private void scanMineCube(
            ServerLevel level,
            SovereignColonyData colony,
            BlockPos center,
            int rx,
            int ry,
            int priority,
            long now
    ) {
        int posted = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-rx, -ry, -rx), center.offset(rx, ry, rx))) {
            if (SymbiontExcavatorGoal.isSafeExcavatable(level, p, level.getBlockState(p))) {
                colony.getOrderBoard().postOrRefreshOrder(ColonyOrder.OrderType.EXCAVATE, p, priority, now);
                if (++posted >= 12) {
                    return;
                }
            }
        }
    }

    private static boolean hasAnyItems(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUuid != null) {
            tag.putUUID(TAG_OWNER_UUID, ownerUuid);
        }
        tag.putString(TAG_OWNER_NAME, ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.ownerUuid = tag.hasUUID(TAG_OWNER_UUID) ? tag.getUUID(TAG_OWNER_UUID) : null;
        if (tag.contains(TAG_OWNER_NAME)) {
            this.ownerName = tag.getString(TAG_OWNER_NAME);
        }
    }
}
