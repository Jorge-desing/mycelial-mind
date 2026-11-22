package com.alex.mycelialmind.client;

import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

import java.util.EnumSet;
import java.util.Set;

/**
 * Client-side reactive soundscape and Hive Mind adaptation alert director.
 * Plays positional ambient loops (Purification Pylon field hum, Fungal Tide spore storm,
 * Deep Mycelium Homeostasis arterial heartbeat + acid rain sizzle) and one-shot HUD/dimension
 * alerts when the Hive Mind evolves new traits, synergies, threat milestones, or IMMUNE_RESPONSE.
 */
public final class ClientSoundscapeDirector {
    private static boolean baselineCaptured = false;
    private static final Set<AdaptationTrait> knownTraits = EnumSet.noneOf(AdaptationTrait.class);
    private static final Set<HiveSynergy> knownSynergies = EnumSet.noneOf(HiveSynergy.class);
    private static int lastThreatMilestone = 0;
    private static HomeostasisStage lastHomeostasisStage = HomeostasisStage.STABLE;
    private static int tickCounter = 0;
    private static int alertCooldownTicks = 0;
    private static int primordialAlertCooldownTicks = 0;

    private ClientSoundscapeDirector() {
    }

    public static int computeThreatMilestone(double threatPercent) {
        return EvolutionRank.computeThreatMilestone(threatPercent);
    }

    public static void resetSession() {
        baselineCaptured = false;
        knownTraits.clear();
        knownSynergies.clear();
        lastThreatMilestone = 0;
        lastHomeostasisStage = HomeostasisStage.STABLE;
        tickCounter = 0;
        alertCooldownTicks = 0;
        primordialAlertCooldownTicks = 0;
    }

    public static void onClientTick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        tickCounter++;
        if (alertCooldownTicks > 0) {
            alertCooldownTicks--;
        }
        if (primordialAlertCooldownTicks > 0) {
            primordialAlertCooldownTicks--;
        }

        Set<AdaptationTrait> currentTraits = ClientHiveCache.getActiveTraits();
        Set<HiveSynergy> currentSynergies = ClientHiveCache.getActiveSynergies();
        int currentMilestone = computeThreatMilestone(ClientHiveCache.getThreatLevel());
        boolean inDeepMycelium = mc.level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);
        HomeostasisStage stage = ClientHiveCache.getHomeostasisStage();

        if (!baselineCaptured) {
            // Capture initial synced state after joining world so we don't fire false alerts on login
            if (tickCounter >= 25) {
                knownTraits.clear();
                knownTraits.addAll(currentTraits);
                knownSynergies.clear();
                knownSynergies.addAll(currentSynergies);
                lastThreatMilestone = currentMilestone;
                lastHomeostasisStage = stage;
                baselineCaptured = true;
            }
            return;
        }

        // 1. Hive Adaptation & Threat Milestone Audio Alerts
        boolean newSynergyUnlocked = false;
        for (HiveSynergy syn : currentSynergies) {
            if (!knownSynergies.contains(syn)) {
                newSynergyUnlocked = true;
                break;
            }
        }
        boolean newTraitAdapted = false;
        for (AdaptationTrait trait : currentTraits) {
            if (!knownTraits.contains(trait)) {
                newTraitAdapted = true;
                break;
            }
        }
        boolean milestoneEscalated = currentMilestone > lastThreatMilestone;
        boolean immuneSurgeTriggered = EvolutionRank.shouldTriggerImmuneSurgeAlert(
                inDeepMycelium,
                lastHomeostasisStage.ordinal(),
                stage.ordinal(),
                HomeostasisStage.IMMUNE_RESPONSE.ordinal()
        );

        knownTraits.clear();
        knownTraits.addAll(currentTraits);
        knownSynergies.clear();
        knownSynergies.addAll(currentSynergies);
        lastThreatMilestone = currentMilestone;
        lastHomeostasisStage = stage;

        if (MycelialClientConfig.hiveAdaptationAudioAlerts() && alertCooldownTicks == 0) {
            if (newSynergyUnlocked) {
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.HIVE_SYNERGY_UNLOCK.get(), SoundSource.HOSTILE,
                        0.78F, 1.0F, false
                );
                alertCooldownTicks = 30;
            } else if (immuneSurgeTriggered) {
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.IMMUNE_SURGE.get(), SoundSource.HOSTILE,
                        0.84F, 0.96F + mc.level.random.nextFloat() * 0.08F, false
                );
                alertCooldownTicks = 30;
            } else if (newTraitAdapted) {
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.HIVE_TRAIT_ADAPT.get(), SoundSource.HOSTILE,
                        0.72F, 1.0F, false
                );
                alertCooldownTicks = 25;
            } else if (milestoneEscalated) {
                float pitch = 0.90F + (currentMilestone / 100.0F) * 0.20F;
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.HIVE_THREAT_MILESTONE.get(), SoundSource.HOSTILE,
                        0.75F, pitch, false
                );
                alertCooldownTicks = 25;
            }
        }

        // 2. Positional Ambient Soundscape Loops
        float envVol = (float) MycelialClientConfig.ambientSoundscapeVolume();
        if (envVol <= 0.01F) {
            return;
        }

        // 2a. Fungal Tide storm howl (when active outside Pylon protection)
        if (ClientHiveCache.getFungalTideSecondsRemaining() > 0
                && !ClientHiveCache.isInProtectedZone()
                && tickCounter % 72 == 0) {
            mc.level.playLocalSound(
                    mc.player.getX(), mc.player.getY() + 2.0D, mc.player.getZ(),
                    ModSounds.FUNGAL_TIDE_AMBIENCE.get(), SoundSource.AMBIENT,
                    0.48F * envVol, 0.96F + mc.level.random.nextFloat() * 0.08F, false
            );
        }

        // 2b. Deep Mycelium Homeostasis arterial heartbeat + acid rain sizzle under open sky
        if (inDeepMycelium) {
            if (stage != HomeostasisStage.STABLE) {
                int interval = stage == HomeostasisStage.IMMUNE_RESPONSE ? 36 : 60;
                if (tickCounter % interval == 0) {
                    float stress01 = (float) Math.min(1.0D, Math.max(0.2D, ClientHiveCache.getHomeostasisStress() / 100.0D));
                    float hbPitch = stage == HomeostasisStage.IMMUNE_RESPONSE ? 1.08F : 0.96F;
                    mc.level.playLocalSound(
                            mc.player.getX(), mc.player.getY() - 1.0D, mc.player.getZ(),
                            ModSounds.HOMEOSTASIS_HEARTBEAT.get(), SoundSource.AMBIENT,
                            (0.32F + 0.35F * stress01) * envVol, hbPitch, false
                    );
                }
            }
            if (stage == HomeostasisStage.IMMUNE_RESPONSE && tickCounter % 44 == 12) {
                BlockPos headPos = mc.player.blockPosition().above();
                boolean sheltered = false;
                for (int dy = 1; dy <= 10; dy++) {
                    BlockPos checkPos = headPos.above(dy);
                    if (!mc.level.getBlockState(checkPos).getCollisionShape(mc.level, checkPos).isEmpty()) {
                        sheltered = true;
                        break;
                    }
                }
                float sizzleVol = EvolutionRank.computeAcidRainSizzleVolume(
                        ClientHiveCache.getHomeostasisStress(), sheltered, envVol);
                if (sizzleVol > 0.01F) {
                    mc.level.playLocalSound(
                            mc.player.getX(), mc.player.getY() + 1.5D, mc.player.getZ(),
                            ModSounds.ACID_RAIN_SIZZLE.get(), SoundSource.WEATHER,
                            sizzleVol, 0.92F + mc.level.random.nextFloat() * 0.16F, false
                    );
                }
            }
        }

        // 2c. Nearby active Purification Pylon electromagnetic field hum (tier-scaled radius & pitch)
        if (tickCounter % 50 == 25) {
            BlockPos playerPos = mc.player.blockPosition();
            BlockPos closestPylon = null;
            int closestTier = 1;
            double bestDistSq = Double.MAX_VALUE;
            for (BlockPos pylonPos : ClientHiveCache.getNearbyPylons()) {
                int tier = mc.level.getBlockEntity(pylonPos) instanceof com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity focus
                        ? focus.getTier()
                        : 1;
                double maxR = EvolutionRank.computePylonHumRadius(tier);
                double dSq = pylonPos.distSqr(playerPos);
                if (dSq <= maxR * maxR && dSq < bestDistSq) {
                    boolean smothered = AntibodyCystBlock.countAdjacentCysts(mc.level, pylonPos)
                            >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
                    if (!smothered) {
                        bestDistSq = dSq;
                        closestPylon = pylonPos;
                        closestTier = tier;
                    }
                }
            }
            if (closestPylon != null) {
                float basePitch = EvolutionRank.computePylonHumPitch(closestTier);
                mc.level.playLocalSound(
                        closestPylon.getX() + 0.5D, closestPylon.getY() + 0.8D, closestPylon.getZ() + 0.5D,
                        ModSounds.PURIFICATION_PYLON_HUM.get(), SoundSource.BLOCKS,
                        0.40F * envVol, basePitch * (0.98F + mc.level.random.nextFloat() * 0.04F), false
                );
            }
        }

        // 2d. Nearby Primordial Bioluminescent Mob resonance pulse
        if (MycelialClientConfig.hiveAdaptationAudioAlerts()
                && primordialAlertCooldownTicks == 0
                && tickCounter % 40 == 10) {
            boolean primordialNear = !mc.level.getEntitiesOfClass(
                    com.alex.mycelialmind.entity.MycelialMob.class,
                    mc.player.getBoundingBox().inflate(20.0D),
                    m -> m.isAlive() && m.isPrimordialVariant()
            ).isEmpty();
            if (primordialNear) {
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.BIO_SCANNER_PING.get(), SoundSource.HOSTILE,
                        0.48F * envVol, 1.45F, false
                );
                primordialAlertCooldownTicks = 200;
            }
        }
    }
}
