package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Persistent per-player attachment tracking genetic bestiary completion (17 species),
 * discovered traits/strains, experimental secrets (8), early crafting/upgrade milestones,
 * rare primordial encounters, and Perfect Bastion defense score.
 */
public class PlayerDiscoveryData implements INBTSerializable<CompoundTag> {
    private static final String TAG_SPECIES = "SpeciesMask";
    private static final String TAG_TRAITS = "TraitsMask";
    private static final String TAG_STRAINS = "StrainsMask";
    private static final String TAG_SECRETS = "SecretsMask";
    private static final String TAG_MILESTONES = "MilestonesMask";
    private static final String TAG_RARE_VARIANTS = "RareVariantsFound";
    private static final String TAG_BASTION_SCORE = "BastionScore";

    private int scannedSpeciesMask = 0;
    private int discoveredTraitsMask = 0;
    private int discoveredStrainsMask = 0;
    private int discoveredSecretsMask = 0;
    private int craftedMilestonesMask = 0;
    private int rareVariantsFound = 0;
    private int bastionScore = 0;

    public int getScannedSpeciesMask() {
        return scannedSpeciesMask;
    }

    public int getDiscoveredTraitsMask() {
        return discoveredTraitsMask;
    }

    public int getDiscoveredStrainsMask() {
        return discoveredStrainsMask;
    }

    public int getDiscoveredSecretsMask() {
        return discoveredSecretsMask;
    }

    public int getCraftedMilestonesMask() {
        return craftedMilestonesMask;
    }

    public int getRareVariantsFound() {
        return rareVariantsFound;
    }

    public int getBastionScore() {
        return bastionScore;
    }

    public int countScannedSpecies() {
        return Integer.bitCount(scannedSpeciesMask & ((1 << ProgressionGoalEngine.TOTAL_SPECIES) - 1));
    }

    public int countDiscoveredTraits() {
        return Integer.bitCount(discoveredTraitsMask & 0x1F);
    }

    public int countDiscoveredStrains() {
        return Integer.bitCount(discoveredStrainsMask & 0x7);
    }

    public int countDiscoveredSecrets() {
        return Integer.bitCount(discoveredSecretsMask & 0xFF);
    }

    public boolean hasSecret(int secretBit) {
        return secretBit >= 0 && secretBit < ProgressionGoalEngine.TOTAL_SECRETS
                && (discoveredSecretsMask & (1 << secretBit)) != 0;
    }

    public boolean recordSpecies(EntityType<?> type) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (key == null) {
            return false;
        }
        int idx = ProgressionGoalEngine.speciesIndex(key.getPath());
        if (idx < 0) {
            return false;
        }
        int bit = 1 << idx;
        if ((scannedSpeciesMask & bit) != 0) {
            return false;
        }
        scannedSpeciesMask |= bit;
        return true;
    }

    public boolean recordTrait(AdaptationTrait trait) {
        if (trait == null) {
            return false;
        }
        int bit = 1 << trait.ordinal();
        if ((discoveredTraitsMask & bit) != 0) {
            return false;
        }
        discoveredTraitsMask |= bit;
        return true;
    }

    public boolean recordStrain(HiveStrain strain) {
        if (strain == null) {
            return false;
        }
        int bit = 1 << strain.ordinal();
        if ((discoveredStrainsMask & bit) != 0) {
            return false;
        }
        discoveredStrainsMask |= bit;
        return true;
    }

    public boolean recordMilestone(int milestoneBit) {
        if (milestoneBit < 0 || milestoneBit >= 16) {
            return false;
        }
        int bit = 1 << milestoneBit;
        if ((craftedMilestonesMask & bit) != 0) {
            return false;
        }
        craftedMilestonesMask |= bit;
        return true;
    }

    public boolean recordSecret(int secretBit) {
        if (secretBit < 0 || secretBit >= ProgressionGoalEngine.TOTAL_SECRETS) {
            return false;
        }
        int bit = 1 << secretBit;
        if ((discoveredSecretsMask & bit) != 0) {
            return false;
        }
        discoveredSecretsMask |= bit;
        return true;
    }

    public void incrementRareVariantsFound() {
        rareVariantsFound = Math.min(999, rareVariantsFound + 1);
    }

    public boolean setBastionScore(int score) {
        int clamped = Math.max(0, Math.min(100, score));
        if (clamped == this.bastionScore) {
            return false;
        }
        this.bastionScore = clamped;
        return true;
    }

    public static boolean unlockSecret(ServerPlayer player, int secretBit) {
        PlayerDiscoveryData data = player.getData(ModAttachments.DISCOVERY);
        if (!data.recordSecret(secretBit)) {
            return false;
        }
        player.sendSystemMessage(Component.translatable(
                "message.mycelialmind.secret_discovered",
                Component.translatable("codex.mycelialmind.secret.name." + (secretBit + 1))
        ).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        DiscoverySyncPayload.syncToPlayer(player);
        return true;
    }

    public static void unlockMilestone(ServerPlayer player, int milestoneBit) {
        PlayerDiscoveryData data = player.getData(ModAttachments.DISCOVERY);
        if (data.recordMilestone(milestoneBit)) {
            DiscoverySyncPayload.syncToPlayer(player);
        }
    }

    public static int computeBastionScoreForPlayer(ServerLevel level, ServerPlayer player) {
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos anchor = player.blockPosition();
        var nearbyPylons = tracker.getPylonsWithin(anchor, 96, 16);
        int pylonCount = nearbyPylons.size();
        int maxPylonTier = 1;
        for (BlockPos pylonPos : nearbyPylons) {
            if (level.isLoaded(pylonPos) && level.getBlockEntity(pylonPos) instanceof MonumentFocusBlockEntity focus) {
                maxPylonTier = Math.max(maxPylonTier, focus.getTier());
            }
        }

        int turretCount = 0;
        int maxTurretTier = 1;
        int benchTier = 0;
        int minChunkX = (anchor.getX() - 64) >> 4;
        int maxChunkX = (anchor.getX() + 64) >> 4;
        int minChunkZ = (anchor.getZ() - 64) >> 4;
        int maxChunkZ = (anchor.getZ() + 64) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof PurificationTurretBlockEntity turret && turret.getBlockPos().distSqr(anchor) <= 96.0D * 96.0D) {
                        turretCount++;
                        maxTurretTier = Math.max(maxTurretTier, turret.getTier());
                    } else if (be instanceof SynapticBenchBlockEntity bench && bench.getBlockPos().distSqr(anchor) <= 96.0D * 96.0D) {
                        benchTier = Math.max(benchTier, bench.getLabTier());
                    }
                }
            }
        }

        int symbioticNodes = tracker.countSymbioticNodes();
        AABB companionBox = new AABB(anchor).inflate(64.0D);
        boolean hasCompanion = !level.getEntitiesOfClass(
                PurifierDroneEntity.class, companionBox,
                d -> d.isAlive() && player.getUUID().equals(d.getOwnerUuid())
        ).isEmpty() || !level.getEntitiesOfClass(
                MycelialChimeraEntity.class, companionBox,
                c -> c.isAlive() && player.getUUID().equals(c.getOwnerUUID())
        ).isEmpty();

        return ProgressionGoalEngine.computeBastionScore(
                pylonCount, maxPylonTier, turretCount, maxTurretTier, symbioticNodes, benchTier, hasCompanion
        );
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_SPECIES, scannedSpeciesMask);
        tag.putInt(TAG_TRAITS, discoveredTraitsMask);
        tag.putInt(TAG_STRAINS, discoveredStrainsMask);
        tag.putInt(TAG_SECRETS, discoveredSecretsMask);
        tag.putInt(TAG_MILESTONES, craftedMilestonesMask);
        tag.putInt(TAG_RARE_VARIANTS, rareVariantsFound);
        tag.putInt(TAG_BASTION_SCORE, bastionScore);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        scannedSpeciesMask = nbt.getInt(TAG_SPECIES);
        discoveredTraitsMask = nbt.getInt(TAG_TRAITS);
        discoveredStrainsMask = nbt.getInt(TAG_STRAINS);
        discoveredSecretsMask = nbt.getInt(TAG_SECRETS);
        craftedMilestonesMask = nbt.getInt(TAG_MILESTONES);
        rareVariantsFound = Math.max(0, nbt.getInt(TAG_RARE_VARIANTS));
        bastionScore = Math.max(0, Math.min(100, nbt.getInt(TAG_BASTION_SCORE)));
    }
}
