package com.alex.mycelialmind.block.entity;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.BurrowerEntity;
import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SporeArchitectEntity;
import com.alex.mycelialmind.entity.SynapseWeaverEntity;
import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
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
import org.jetbrains.annotations.Nullable;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Autonomous base defense BlockEntity that fires NeutralizingSporeProjectiles
 * at nearby MycelialMobs (prioritizing Burrowers and Colony Wardens).
 */
public class PurificationTurretBlockEntity extends BlockEntity {
    private static final String TAG_TIER = "TurretTier";
    private static final String TAG_JAMMED_TICKS = "JammedTicks";
    private static final String TAG_AMMO = "TurretAmmo";

    private int tickCounter = 0;
    private int fireCooldownTicks = -1;
    private int observedFireInterval = -1;
    private int tier = 1;
    private int jammedTicks = 0;
    private int ammo = com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_INITIAL_AMMO;
    public float clientAimYaw = 0.0F;
    public float clientAimTargetYaw = 0.0F;
    public long clientAimTargetUpdateTick = Long.MIN_VALUE;

    public PurificationTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PURIFICATION_TURRET.get(), pos, state);
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(1, Math.min(3, tier));
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    public double getRangeRadius() {
        return switch (tier) {
            case 2 -> 22.0D;
            case 3 -> 28.0D;
            default -> 16.0D;
        };
    }

    public int getShotsPerVolley() {
        return tier >= 3 ? 2 : 1;
    }

    public int getJammedTicks() {
        return jammedTicks;
    }

    public boolean isJammed() {
        return jammedTicks > 0;
    }

    public void applyCorrosiveJam(int ticks) {
        if (ticks <= 0) {
            return;
        }
        boolean wasJammed = isJammed();
        this.jammedTicks = Math.max(this.jammedTicks, ticks);
        setChanged();
        if (!wasJammed && this.level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.worldPosition, ModSounds.CORROSIVE_JAM.get(), SoundSource.BLOCKS, 0.9F, 1.1F);
        }
    }

    public void clearCorrosiveJam() {
        if (this.jammedTicks > 0) {
            this.jammedTicks = 0;
            setChanged();
        }
    }

    public boolean canCleanJamWith(ItemStack stack) {
        if (!isJammed() || stack.isEmpty()) {
            return false;
        }
        return stack.is(ModItems.SPORE_ANTIDOTE.get())
                || stack.is(ModItems.CONCENTRATED_ANTIDOTE.get())
                || stack.is(ModItems.CHITIN_PLATE.get());
    }

    public int getAmmo() {
        return this.ammo;
    }

    public boolean hasAmmo() {
        return this.ammo > 0;
    }

    public void setAmmo(int ammo) {
        int clamped = Math.max(0, Math.min(com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_MAX_AMMO, ammo));
        if (this.ammo != clamped) {
            this.ammo = clamped;
            if (this.level instanceof ServerLevel serverLevel) {
                BlockState state = getBlockState();
                serverLevel.sendBlockUpdated(this.worldPosition, state, state, 3);
            }
            setChanged();
        }
    }

    public void consumeAmmo(int amount) {
        if (amount <= 0 || this.ammo <= 0) {
            return;
        }
        setAmmo(this.ammo - amount);
    }

    public boolean canReloadWith(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.turretAmmoGain(path) > 0
                && this.ammo < com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_MAX_AMMO;
    }

    public boolean tryReload(ItemStack stack, @Nullable Player player) {
        if (!(this.level instanceof ServerLevel serverLevel) || stack.isEmpty()) {
            return false;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        int gain = com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.turretAmmoGain(path);
        if (gain <= 0 || this.ammo >= com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_MAX_AMMO) {
            return false;
        }
        setAmmo(this.ammo + gain);
        if (player == null || (!player.isCreative() && !player.getAbilities().instabuild)) {
            stack.shrink(1);
        }
        serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.25, worldPosition.getZ() + 0.5,
                8, 0.25, 0.25, 0.25, 0.03);
        serverLevel.playSound(null, worldPosition, ModSounds.PURIFICATION_TURRET_UPGRADE.get(),
                SoundSource.BLOCKS, 0.7F, 1.35F);
        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    "message.mycelialmind.turret_reloaded",
                    this.ammo,
                    com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_MAX_AMMO)
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return true;
    }

    public int resolveNextTierForCatalyst(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (this.tier == 1 && stack.is(ModItems.SYNAPTIC_CORE.get())) {
            return 2;
        }
        if (this.tier == 2 && (stack.is(ModItems.TIDE_BEACON.get()) || stack.is(ModItems.OVERMIND_ESSENCE.get()))) {
            return 3;
        }
        return 0;
    }

    public boolean canUpgradeWith(ItemStack stack) {
        int nextTier = resolveNextTierForCatalyst(stack);
        if (nextTier == 0) {
            return false;
        }
        if (this.level instanceof ServerLevel serverLevel) {
            com.alex.mycelialmind.hive.HiveProgressionTier effectiveAct =
                    com.alex.mycelialmind.world.structure.StructureProgressionPolicy.resolveTier(serverLevel);
            boolean defeated = com.alex.mycelialmind.hive.DamageTelemetryTracker.isHiveCoreDefeated(serverLevel, null);
            return com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.canUpgradeToTier(
                    nextTier, effectiveAct, defeated, false);
        }
        return true;
    }

    public boolean tryCleanJam(ItemStack stack, @Nullable Player player) {
        if (!(this.level instanceof ServerLevel serverLevel) || !canCleanJamWith(stack)) {
            return false;
        }
        clearCorrosiveJam();
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                14, 0.3, 0.3, 0.3, 0.04);
        serverLevel.playSound(null, worldPosition, ModSounds.PURIFICATION_TURRET_REPAIR.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.turret_unjammed")
                    .withStyle(ChatFormatting.GREEN));
        }
        return true;
    }

    public int getFireInterval(ServerLevel level) {
        return getFireInterval(PurificationZoneTracker.get(level));
    }

    public int getFireInterval(PurificationZoneTracker tracker) {
        boolean protectedZone = tracker.isProtected(this.worldPosition);
        int base = switch (tier) {
            case 2 -> protectedZone ? 16 : 24;
            case 3 -> protectedZone ? 12 : 18;
            default -> protectedZone ? 22 : 34;
        };
        return isJammed() ? base * 2 : base;
    }

    public boolean tryUpgrade(ItemStack stack, @Nullable Player player) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return false;
        }
        int nextTier = resolveNextTierForCatalyst(stack);
        if (nextTier == 0) {
            return false;
        }
        com.alex.mycelialmind.hive.HiveProgressionTier effectiveAct =
                com.alex.mycelialmind.world.structure.StructureProgressionPolicy.resolveTier(serverLevel);
        boolean defeated = com.alex.mycelialmind.hive.DamageTelemetryTracker.isHiveCoreDefeated(serverLevel, player);
        boolean creative = player != null && (player.isCreative() || player.getAbilities().instabuild);
        if (!com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.canUpgradeToTier(
                nextTier, effectiveAct, defeated, creative)) {
            if (player != null) {
                com.alex.mycelialmind.hive.HiveProgressionTier reqAct =
                        com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.requiredActForTier(nextTier);
                player.displayClientMessage(
                        Component.translatable("message.mycelialmind.gate_requires_act", reqAct.name(), nextTier)
                                .withStyle(ChatFormatting.YELLOW),
                        true);
            }
            return false;
        }
        setTier(nextTier);
        if (player != null && !creative) {
            stack.shrink(1);
        }
        serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.3, worldPosition.getZ() + 0.5,
                22, 0.3, 0.3, 0.3, 0.05);
        serverLevel.playSound(null, worldPosition, ModSounds.PURIFICATION_TURRET_UPGRADE.get(), SoundSource.BLOCKS, 1.0F, 0.95F + nextTier * 0.1F);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.turret_upgraded", nextTier)
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PurificationTurretBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (be.jammedTicks > 0) {
            be.jammedTicks--;
            if (be.jammedTicks % 20 == 0) {
                be.setChanged();
            }
            if (be.tickCounter % 10 == 0) {
                serverLevel.sendParticles(ModParticles.HIVE_EMBER.get(),
                        pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        2, 0.2, 0.2, 0.2, 0.01);
            }
        }
        be.tickCounter++;
        int interval = be.getFireInterval(serverLevel);
        if (be.observedFireInterval < 0) {
            be.observedFireInterval = interval;
            be.fireCooldownTicks = interval;
        } else if (interval != be.observedFireInterval) {
            be.fireCooldownTicks = Math.max(0,
                    be.fireCooldownTicks + interval - be.observedFireInterval);
            be.observedFireInterval = interval;
        }
        if (be.fireCooldownTicks > 0) {
            be.fireCooldownTicks--;
            if (be.fireCooldownTicks > 0) {
                return;
            }
        }
        be.fireCooldownTicks = interval;
        if (be.ammo > 0 && be.ammo <= 6 && be.tickCounter % 15 == 0) {
            serverLevel.sendParticles(ModParticles.HIVE_EMBER.get(),
                    pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5,
                    2, 0.16, 0.14, 0.16, 0.01);
        }
        if (!be.hasAmmo()) {
            serverLevel.sendParticles(ModParticles.SPORE_FALL.get(),
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    2, 0.18, 0.15, 0.18, 0.01);
            return;
        }

        double rangeRadius = be.getRangeRadius();
        AABB range = new AABB(pos).inflate(rangeRadius);
        List<MycelialMob> mobs = serverLevel.getEntitiesOfClass(MycelialMob.class, range, MycelialMob::isAlive);
        double rangeSq = rangeRadius * rangeRadius;
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        List<ServerPlayer> nearbyPlayers = serverLevel.players().stream()
                .filter(p -> !p.isSpectator() && p.distanceToSqr(centerX, centerY, centerZ) <= rangeSq)
                .toList();
        mobs.removeIf(mob -> mob.distanceToSqr(centerX, centerY, centerZ) > rangeSq
                || !isEligibleTarget(mob, nearbyPlayers));
        if (mobs.isEmpty()) {
            return;
        }

        int shots = be.getShotsPerVolley();
        boolean firedAny = false;
        for (int s = 0; s < shots && !mobs.isEmpty() && be.hasAmmo(); s++) {
            MycelialMob target = pickPriorityTarget(pos, mobs);
            if (target == null) {
                break;
            }
            NeutralizingSporeProjectile proj = new NeutralizingSporeProjectile(
                    ModEntities.NEUTRALIZING_SPORE.get(),
                    serverLevel
            );
            proj.setPos(pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5);
            double dx = target.getX() - (pos.getX() + 0.5);
            double dy = target.getY(0.5) - (pos.getY() + 1.15);
            double dz = target.getZ() - (pos.getZ() + 0.5);
            proj.shoot(dx, dy, dz, 1.45F + (be.tier - 1) * 0.15F, 1.0F);
            if (!serverLevel.addFreshEntity(proj)) {
                break;
            }
            be.consumeAmmo(1);
            mobs.remove(target);
            firedAny = true;
        }

        if (firedAny) {
            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    6 + be.tier * 2, 0.15, 0.15, 0.15, 0.02);
            serverLevel.playSound(null, pos, ModSounds.PURIFICATION_TURRET_FIRE.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_TIER, this.tier);
        if (this.jammedTicks > 0) {
            tag.putInt(TAG_JAMMED_TICKS, this.jammedTicks);
        }
        tag.putInt(TAG_AMMO, this.ammo);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_TIER)) {
            this.tier = Math.max(1, Math.min(3, tag.getInt(TAG_TIER)));
        }
        this.jammedTicks = tag.contains(TAG_JAMMED_TICKS) ? Math.max(0, tag.getInt(TAG_JAMMED_TICKS)) : 0;
        if (tag.contains(TAG_AMMO)) {
            this.ammo = Math.max(0, Math.min(
                    com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_MAX_AMMO,
                    tag.getInt(TAG_AMMO)));
        } else {
            this.ammo = com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_INITIAL_AMMO;
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

    public static boolean isEligibleTarget(MycelialMob mob, List<? extends Player> nearbyPlayers) {
        if (mob.isAlliedEscort()) {
            return false;
        }
        if (!mob.getHiveStrain().isSchismStrain()) {
            return true;
        }
        for (Player player : nearbyPlayers) {
            if (MycelialMob.isFriendlyToPlayer(mob, player)) {
                return false;
            }
        }
        return true;
    }

    public static MycelialMob pickPriorityTarget(BlockPos origin, List<MycelialMob> mobs) {
        MycelialMob best = null;
        double bestScore = Double.MAX_VALUE;
        for (MycelialMob mob : mobs) {
            double distSq = mob.distanceToSqr(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
            double priorityFactor;
            if (mob instanceof BurrowerEntity || mob instanceof ColonyWardenEntity) {
                priorityFactor = 0.25;
            } else if (mob instanceof SynapseWeaverEntity || mob instanceof SporeArchitectEntity
                    || mob.getCommandRole() == SynapticCommandSystem.CommandRole.LEADER) {
                priorityFactor = 0.35;
            } else {
                priorityFactor = 1.0;
            }
            double score = distSq * priorityFactor;
            if (score < bestScore) {
                bestScore = score;
                best = mob;
            }
        }
        return best;
    }
}
