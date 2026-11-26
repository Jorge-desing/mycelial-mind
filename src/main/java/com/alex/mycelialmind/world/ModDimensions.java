package com.alex.mycelialmind.world;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.network.DiplomacySyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.network.SymbiosisSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * Registers the Deep Mycelium dimension keys and safe portal teleportation helpers.
 */
public final class ModDimensions {
    public static final ResourceKey<Level> DEEP_MYCELIUM_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "deep_mycelium")
    );

    public static final ResourceKey<DimensionType> DEEP_MYCELIUM_TYPE = ModWorldgenKeys.DEEP_MYCELIUM_TYPE;

    private ModDimensions() {
    }

    public static boolean supportsRiftTravel(Level level) {
        return level.dimension().equals(Level.OVERWORLD)
                || level.dimension().equals(DEEP_MYCELIUM_LEVEL);
    }

    public static void teleportPlayer(ServerPlayer player, ServerLevel currentLevel) {
        if (!supportsRiftTravel(currentLevel)) {
            player.setPortalCooldown();
            player.sendSystemMessage(Component.translatable("message.mycelialmind.rift_unsupported_dimension")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        ResourceKey<Level> targetKey = currentLevel.dimension().equals(DEEP_MYCELIUM_LEVEL)
                ? Level.OVERWORLD
                : DEEP_MYCELIUM_LEVEL;
        ServerLevel targetLevel = currentLevel.getServer().getLevel(targetKey);
        if (targetLevel == null) {
            player.setPortalCooldown();
            player.sendSystemMessage(Component.translatable("message.mycelialmind.rift_dimension_unavailable")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        BlockPos targetOrigin = new BlockPos(
                player.blockPosition().getX(),
                targetKey.equals(DEEP_MYCELIUM_LEVEL) ? 76 : targetLevel.getSeaLevel() + 2,
                player.blockPosition().getZ()
        );
        Optional<SafeArrival> safeArrivalResult = findSafeArrival(targetLevel, targetOrigin, player);
        if (safeArrivalResult.isEmpty()) {
            player.setPortalCooldown();
            player.sendSystemMessage(Component.translatable("message.mycelialmind.rift_no_safe_arrival")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        SafeArrival safeArrival = safeArrivalResult.orElseThrow();
        BlockPos safePos = safeArrival.floor();
        if (!targetLevel.setBlockAndUpdate(safeArrival.returnRift(), ModBlocks.MYCELIAL_RIFT.get().defaultBlockState())) {
            player.setPortalCooldown();
            player.sendSystemMessage(Component.translatable("message.mycelialmind.rift_no_safe_arrival")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        AABB searchBox = new AABB(player.blockPosition()).inflate(16.0D);
        List<MycelialChimeraEntity> chimeras = currentLevel.getEntitiesOfClass(
                MycelialChimeraEntity.class, searchBox,
                c -> c.isAlive() && player.getUUID().equals(c.getOwnerUUID())
                        && (c.getCommandMode() == MycelialChimeraEntity.CommandMode.FOLLOW || c.hasPassenger(player))
        );
        List<PurifierDroneEntity> drones = currentLevel.getEntitiesOfClass(
                PurifierDroneEntity.class, searchBox,
                d -> d.isAlive() && player.getUUID().equals(d.getOwnerUuid()) && !d.isPatrolMode()
        );
        if (player.isPassenger() && player.getVehicle() instanceof MycelialChimeraEntity) {
            player.stopRiding();
        }

        if (targetKey.equals(DEEP_MYCELIUM_LEVEL)) {
            ServerLevel overworld = currentLevel.getServer().overworld();
            boolean resonant = syncCrossDimensionProgression(overworld, targetLevel);
            if (!resonant) {
                DimensionalHomeostasisSavedData.get(targetLevel).addStress(
                        new ChunkPos(safePos), 25.0, targetLevel.getGameTime()
                );
                player.sendSystemMessage(Component.translatable("message.mycelialmind.deep_mycelium_premature_entry")
                        .withStyle(ChatFormatting.GOLD));
            }
        }

        player.setPortalCooldown();
        player.teleportTo(
                targetLevel,
                safePos.getX() + 0.5,
                safePos.getY() + 1.0,
                safePos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
        targetLevel.playSound(null, safePos, ModSounds.MYCELIAL_RIFT_HUM.get(), SoundSource.BLOCKS, 0.9F, 1.0F);

        int movedCompanions = 0;
        Vec3 companionDest = new Vec3(safePos.getX() + 0.5, safePos.getY() + 1.0, safePos.getZ() + 0.5);
        for (MycelialChimeraEntity chimera : chimeras) {
            Entity relocated = chimera.changeDimension(new DimensionTransition(
                    targetLevel, companionDest, Vec3.ZERO, chimera.getYRot(), chimera.getXRot(), DimensionTransition.DO_NOTHING
            ));
            if (relocated instanceof MycelialChimeraEntity movedChimera) {
                movedChimera.setHomePos(safePos);
                movedCompanions++;
            }
        }
        Vec3 droneDest = companionDest.add(0.0, 1.2, 0.0);
        for (PurifierDroneEntity drone : drones) {
            Entity relocated = drone.changeDimension(new DimensionTransition(
                    targetLevel, droneDest, Vec3.ZERO, drone.getYRot(), drone.getXRot(), DimensionTransition.DO_NOTHING
            ));
            if (relocated != null) {
                movedCompanions++;
            }
        }
        if (movedCompanions > 0) {
            player.sendSystemMessage(Component.translatable("message.mycelialmind.companions_transferred", movedCompanions)
                    .withStyle(ChatFormatting.AQUA));
        }

        PacketDistributor.sendToPlayer(player, HiveSyncPayload.forPlayer(targetLevel, player));
        SymbiosisSyncPayload.syncToPlayer(player);
        DiplomacySyncPayload.syncToPlayer(player);
    }

    /**
     * Bridges Overworld Act progression into Deep Mycelium and marks the breach milestone.
     * Returns true if the Hive had already reached Act III (ADAPTIVE) or higher.
     */
    public static boolean syncCrossDimensionProgression(ServerLevel overworld, ServerLevel deepMycelium) {
        HiveMindSavedData owHive = HiveMindSavedData.get(overworld);
        HiveMindSavedData deepHive = HiveMindSavedData.get(deepMycelium);
        owHive.markDeepMyceliumBreached();
        deepHive.syncProgressionFrom(owHive, overworld.getGameTime(), deepMycelium.getGameTime());
        deepHive.markDeepMyceliumBreached();
        HiveProgressionTier effectiveTier = HiveProgressionTier.fromThreat(
                Math.max(owHive.getThreatLevel(), owHive.getHighestProgressionTier().minThreat())
        );
        return HiveProgressionTier.isRiftResonantTier(effectiveTier);
    }

    private static Optional<SafeArrival> findSafeArrival(ServerLevel level, BlockPos origin, Entity traveler) {
        int minFloorY = level.getMinBuildHeight() + 1;
        int maxFloorY = level.getMaxBuildHeight() - 4;
        int originY = Math.max(minFloorY, Math.min(maxFloorY, origin.getY()));
        int maxVerticalOffset = Math.max(originY - minFloorY, maxFloorY - originY);

        // Search nearby columns and prefer floors close to the expected arrival height.
        // This never clears or replaces terrain: block entities and occupied spaces are skipped.
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int verticalOffset = 0; verticalOffset <= maxVerticalOffset; verticalOffset++) {
                        int upperY = originY + verticalOffset;
                        if (upperY <= maxFloorY) {
                            Optional<SafeArrival> arrival = findSafeArrivalAt(
                                    level, new BlockPos(origin.getX() + dx, upperY, origin.getZ() + dz), traveler
                            );
                            if (arrival.isPresent()) {
                                return arrival;
                            }
                        }
                        if (verticalOffset > 0) {
                            int lowerY = originY - verticalOffset;
                            if (lowerY >= minFloorY) {
                                Optional<SafeArrival> arrival = findSafeArrivalAt(
                                        level, new BlockPos(origin.getX() + dx, lowerY, origin.getZ() + dz), traveler
                                );
                                if (arrival.isPresent()) {
                                    return arrival;
                                }
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<SafeArrival> findSafeArrivalAt(ServerLevel level, BlockPos floor, Entity traveler) {
        double halfWidth = traveler.getBbWidth() * 0.5D;
        double arrivalX = floor.getX() + 0.5D;
        double arrivalY = floor.getY() + 1.0D;
        double arrivalZ = floor.getZ() + 0.5D;
        AABB arrivalBox = new AABB(
                arrivalX - halfWidth, arrivalY, arrivalZ - halfWidth,
                arrivalX + halfWidth, arrivalY + Math.max(1.8D, traveler.getBbHeight()), arrivalZ + halfWidth
        );
        if (!isSafeArrivalFloor(level, floor)
                || !level.isEmptyBlock(floor.above())
                || !level.isEmptyBlock(floor.above(2))
                || !level.isEmptyBlock(floor.above(3))
                || !level.getEntitiesOfClass(LivingEntity.class, arrivalBox,
                        entity -> entity != traveler && entity.isAlive() && !entity.isSpectator()).isEmpty()) {
            return Optional.empty();
        }

        // Keep a usable return rift beside the spawn point without overwriting a block.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos riftFloor = floor.offset(dx, 0, dz);
                BlockPos riftPos = riftFloor.above();
                if (isSafeArrivalFloor(level, riftFloor)
                        && level.getWorldBorder().isWithinBounds(riftPos)
                        && level.isEmptyBlock(riftPos)
                        && level.isEmptyBlock(riftPos.above())) {
                    return Optional.of(new SafeArrival(floor, riftPos));
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isSafeArrivalFloor(ServerLevel level, BlockPos floor) {
        if (!level.getWorldBorder().isWithinBounds(floor)) {
            return false;
        }
        BlockState state = level.getBlockState(floor);
        return state.isCollisionShapeFullBlock(level, floor)
                && !state.is(Blocks.MAGMA_BLOCK)
                && state.getFluidState().isEmpty()
                && level.getBlockEntity(floor) == null;
    }

    private record SafeArrival(BlockPos floor, BlockPos returnRift) {
    }
}
