package com.alex.mycelialmind.client;

import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.client.audio.ClientAcousticController;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.entity.MycelialBossMob;
import com.alex.mycelialmind.hive.AcousticPhysicsEngine;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.network.ClientColonyCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side reactive soundscape and Hive Mind adaptation alert director.
 * Drives smooth fading {@link ReactiveLoopSoundInstance} beds (Purification Pylon hum, Fungal Tide storm,
 * Deep Mycelium Homeostasis arterial heartbeat + acid rain sizzle, Sovereign Nexus hum, and Boss tension bed),
 * cinematic audio ducking during alerts, and dedicated Primordial / Hive adaptation cues.
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
    private static int microAmbienceCooldownTicks = 80;
    private static int duckTicksRemaining = 0;
    private static int duckTotalTicks = 0;
    private static float duckMaxReduction = 0.0F;
    private static boolean lastBossNearby = false;
    private static float lastBossHpRatio = 1.0F;
    private static int lastNearbyHostileCount = 0;

    @Nullable
    private static ReactiveLoopSoundInstance fungalTideLoop;
    @Nullable
    private static ReactiveLoopSoundInstance homeostasisHeartbeatLoop;
    @Nullable
    private static ReactiveLoopSoundInstance acidRainSizzleLoop;
    @Nullable
    private static ReactiveLoopSoundInstance pylonHumLoop;
    @Nullable
    private static ReactiveLoopSoundInstance sovereignNexusHumLoop;
    @Nullable
    private static ReactiveLoopSoundInstance bossTensionBedLoop;
    @Nullable
    private static ReactiveLoopSoundInstance sinkholeUpdraftLoop;
    @Nullable
    private static ReactiveLoopSoundInstance hiveTensionBedLowLoop;
    @Nullable
    private static ReactiveLoopSoundInstance hiveSwarmCombatBedLoop;

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
        microAmbienceCooldownTicks = 80;
        duckTicksRemaining = 0;
        duckTotalTicks = 0;
        duckMaxReduction = 0.0F;
        lastBossNearby = false;
        lastBossHpRatio = 1.0F;
        lastNearbyHostileCount = 0;

        Minecraft mc = Minecraft.getInstance();
        stopLoopImmediate(mc, fungalTideLoop);
        stopLoopImmediate(mc, homeostasisHeartbeatLoop);
        stopLoopImmediate(mc, acidRainSizzleLoop);
        stopLoopImmediate(mc, pylonHumLoop);
        stopLoopImmediate(mc, sovereignNexusHumLoop);
        stopLoopImmediate(mc, bossTensionBedLoop);
        stopLoopImmediate(mc, sinkholeUpdraftLoop);
        stopLoopImmediate(mc, hiveTensionBedLowLoop);
        stopLoopImmediate(mc, hiveSwarmCombatBedLoop);
        fungalTideLoop = null;
        homeostasisHeartbeatLoop = null;
        acidRainSizzleLoop = null;
        pylonHumLoop = null;
        sovereignNexusHumLoop = null;
        bossTensionBedLoop = null;
        sinkholeUpdraftLoop = null;
        hiveTensionBedLowLoop = null;
        hiveSwarmCombatBedLoop = null;
    }

    public static int getActiveLoopCount() {
        int active = 0;
        if (fungalTideLoop != null && !fungalTideLoop.isStopped()) active++;
        if (homeostasisHeartbeatLoop != null && !homeostasisHeartbeatLoop.isStopped()) active++;
        if (acidRainSizzleLoop != null && !acidRainSizzleLoop.isStopped()) active++;
        if (pylonHumLoop != null && !pylonHumLoop.isStopped()) active++;
        if (sovereignNexusHumLoop != null && !sovereignNexusHumLoop.isStopped()) active++;
        if (bossTensionBedLoop != null && !bossTensionBedLoop.isStopped()) active++;
        if (sinkholeUpdraftLoop != null && !sinkholeUpdraftLoop.isStopped()) active++;
        if (hiveTensionBedLowLoop != null && !hiveTensionBedLowLoop.isStopped()) active++;
        if (hiveSwarmCombatBedLoop != null && !hiveSwarmCombatBedLoop.isStopped()) active++;
        return active;
    }

    private static void stopLoopImmediate(@Nullable Minecraft mc, @Nullable ReactiveLoopSoundInstance loop) {
        if (loop == null) {
            return;
        }
        loop.forceStopImmediate();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().stop(loop);
        }
    }

    public static void triggerBossCueDucking() {
        triggerDucking(45, 0.42F);
    }

    private static void triggerDucking(int ticks, float reduction) {
        if (!MycelialClientConfig.dynamicAudioDucking() || ticks <= 0) {
            return;
        }
        duckTotalTicks = Math.max(ticks, 1);
        duckTicksRemaining = ticks;
        duckMaxReduction = Math.max(duckMaxReduction, reduction);
    }

    @Nullable
    private static ReactiveLoopSoundInstance updateReactiveLoop(
            Minecraft mc,
            @Nullable ReactiveLoopSoundInstance current,
            SoundEvent soundEvent,
            SoundSource source,
            boolean positional,
            double x,
            double y,
            double z,
            float targetVol,
            float targetPitch,
            float maxStepPerTick
    ) {
        if (targetVol <= 0.005F) {
            if (current != null) {
                current.requestStop();
                if (current.isStopped() || !mc.getSoundManager().isActive(current)) {
                    return null;
                }
            }
            return current;
        }

        if (current == null || current.isStopped() || !mc.getSoundManager().isActive(current)) {
            ReactiveLoopSoundInstance created = new ReactiveLoopSoundInstance(
                    soundEvent, source, positional, targetPitch, maxStepPerTick);
            if (positional) {
                created.setPosition(x, y, z);
            }
            created.updateTarget(targetVol, targetPitch);
            mc.getSoundManager().play(created);
            return created;
        }

        if (positional) {
            current.setPosition(x, y, z);
        }
        current.updateTarget(targetVol, targetPitch);
        return current;
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
        if (duckTicksRemaining > 0) {
            duckTicksRemaining--;
            if (duckTicksRemaining == 0) {
                duckMaxReduction = 0.0F;
            }
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
                triggerDucking(55, 0.55F);
                alertCooldownTicks = 30;
            } else if (immuneSurgeTriggered) {
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.IMMUNE_SURGE.get(), SoundSource.HOSTILE,
                        0.84F, 0.96F + mc.level.random.nextFloat() * 0.08F, false
                );
                triggerDucking(55, 0.55F);
                alertCooldownTicks = 30;
            } else if (newTraitAdapted) {
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.HIVE_TRAIT_ADAPT.get(), SoundSource.HOSTILE,
                        0.72F, 1.0F, false
                );
                triggerDucking(40, 0.45F);
                alertCooldownTicks = 25;
            } else if (milestoneEscalated) {
                float pitch = 0.90F + (currentMilestone / 100.0F) * 0.20F;
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.HIVE_THREAT_MILESTONE.get(), SoundSource.HOSTILE,
                        0.75F, pitch, false
                );
                triggerDucking(45, 0.50F);
                alertCooldownTicks = 25;
            }
        }

        // 2. Reactive Tickable Ambient Loops with smooth fade-in/fade-out & ducking
        float envVol = (float) MycelialClientConfig.ambientSoundscapeVolume();
        float alertDuck = MycelialClientConfig.dynamicAudioDucking()
                ? EvolutionRank.computeAudioDuckingMultiplier(duckTicksRemaining, duckTotalTicks, duckMaxReduction)
                : 1.0F;

        // 2a. Fungal Tide storm loop (active outside Purification Pylon protection)
        boolean tideStormActive = envVol > 0.01F
                && ClientHiveCache.getFungalTideSecondsRemaining() > 0
                && !ClientHiveCache.isInProtectedZone();
        float tideTargetVol = tideStormActive ? 0.48F * envVol * alertDuck : 0.0F;
        fungalTideLoop = updateReactiveLoop(
                mc, fungalTideLoop, ModSounds.FUNGAL_TIDE_AMBIENCE.get(), SoundSource.AMBIENT,
                false, 0.0D, 0.0D, 0.0D, tideTargetVol, 1.0F, 0.025F
        );

        // 2b. Deep Mycelium Homeostasis arterial heartbeat + acid rain sizzle under open sky
        float heartbeatVol = 0.0F;
        float heartbeatPitch = 0.96F;
        float sizzleVol = 0.0F;
        if (envVol > 0.01F && inDeepMycelium && stage != HomeostasisStage.STABLE) {
            float stress01 = (float) Math.min(1.0D, Math.max(0.2D, ClientHiveCache.getHomeostasisStress() / 100.0D));
            heartbeatPitch = stage == HomeostasisStage.IMMUNE_RESPONSE ? 1.08F : 0.96F;
            heartbeatVol = (0.28F + 0.34F * stress01) * envVol * alertDuck;
            if (stage == HomeostasisStage.IMMUNE_RESPONSE) {
                BlockPos headPos = mc.player.blockPosition().above();
                boolean sheltered = false;
                for (int dy = 1; dy <= 10; dy++) {
                    BlockPos checkPos = headPos.above(dy);
                    if (!mc.level.getBlockState(checkPos).getCollisionShape(mc.level, checkPos).isEmpty()) {
                        sheltered = true;
                        break;
                    }
                }
                sizzleVol = EvolutionRank.computeAcidRainSizzleVolume(
                        ClientHiveCache.getHomeostasisStress(), sheltered, envVol) * alertDuck;
            }
        }
        homeostasisHeartbeatLoop = updateReactiveLoop(
                mc, homeostasisHeartbeatLoop, ModSounds.HOMEOSTASIS_HEARTBEAT.get(), SoundSource.AMBIENT,
                false, 0.0D, 0.0D, 0.0D, heartbeatVol, heartbeatPitch, 0.025F
        );
        acidRainSizzleLoop = updateReactiveLoop(
                mc, acidRainSizzleLoop, ModSounds.ACID_RAIN_SIZZLE.get(), SoundSource.WEATHER,
                false, 0.0D, 0.0D, 0.0D, sizzleVol, 1.0F, 0.04F
        );

        // 2c. Nearby active Purification Pylon electromagnetic field hum (tier-scaled radius & pitch)
        BlockPos closestPylon = null;
        int closestTier = 1;
        if (envVol > 0.01F) {
            BlockPos playerPos = mc.player.blockPosition();
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
        }
        float pylonVol = closestPylon != null ? 0.40F * envVol * alertDuck : 0.0F;
        float pylonPitch = EvolutionRank.computePylonHumPitch(closestTier);
        double px = closestPylon != null ? closestPylon.getX() + 0.5D : mc.player.getX();
        double py = closestPylon != null ? closestPylon.getY() + 0.8D : mc.player.getY();
        double pz = closestPylon != null ? closestPylon.getZ() + 0.5D : mc.player.getZ();
        pylonHumLoop = updateReactiveLoop(
                mc, pylonHumLoop, ModSounds.PURIFICATION_PYLON_HUM.get(), SoundSource.BLOCKS,
                true, px, py, pz, pylonVol, pylonPitch, 0.03F
        );

        // 2d. Active Sovereign Nexus ambient field hum
        BlockPos nexusPos = ClientColonyCache.hasColony() && ClientColonyCache.isUnlocked()
                ? ClientColonyCache.getNexusPos()
                : null;
        float nexusVol = 0.0F;
        double nx = mc.player.getX();
        double ny = mc.player.getY();
        double nz = mc.player.getZ();
        if (envVol > 0.01F && nexusPos != null && mc.level.isLoaded(nexusPos)
                && mc.level.getBlockEntity(nexusPos) instanceof SovereignNexusBlockEntity) {
            double maxNexusRadius = 24.0D;
            double dSq = nexusPos.distSqr(mc.player.blockPosition());
            nexusVol = EvolutionRank.computeSovereignNexusHumVolume(dSq, maxNexusRadius, envVol, alertDuck);
            nx = nexusPos.getX() + 0.5D;
            ny = nexusPos.getY() + 1.0D;
            nz = nexusPos.getZ() + 0.5D;
        }
        sovereignNexusHumLoop = updateReactiveLoop(
                mc, sovereignNexusHumLoop, ModSounds.SOVEREIGN_NEXUS_HUM.get(), SoundSource.BLOCKS,
                true, nx, ny, nz, nexusVol, 1.0F, 0.02F
        );

        // 2e. Boss combat tension bed (sub-harmonic drone bed scaling as boss HP drops)
        if (tickCounter % 5 == 0) {
            List<MycelialBossMob> bosses = envVol > 0.01F && MycelialClientConfig.bossTensionBedEnabled()
                    ? mc.level.getEntitiesOfClass(
                            MycelialBossMob.class,
                            mc.player.getBoundingBox().inflate(48.0D),
                            m -> m.isAlive())
                    : List.of();
            lastBossNearby = !bosses.isEmpty();
            if (!bosses.isEmpty()) {
                MycelialBossMob nearestBoss = bosses.get(0);
                double bestDist = nearestBoss.distanceToSqr(mc.player);
                for (int i = 1; i < bosses.size(); i++) {
                    double d = bosses.get(i).distanceToSqr(mc.player);
                    if (d < bestDist) {
                        bestDist = d;
                        nearestBoss = bosses.get(i);
                    }
                }
                lastBossHpRatio = nearestBoss.getMaxHealth() > 0.0F
                        ? nearestBoss.getHealth() / nearestBoss.getMaxHealth()
                        : 1.0F;
            } else {
                lastBossHpRatio = 1.0F;
            }
        } else if (envVol <= 0.01F || !MycelialClientConfig.bossTensionBedEnabled()) {
            lastBossNearby = false;
            lastBossHpRatio = 1.0F;
        }
        float bossBedVol = (envVol > 0.01F && MycelialClientConfig.bossTensionBedEnabled() && lastBossNearby)
                ? EvolutionRank.computeBossTensionBedVolume(true, lastBossHpRatio, envVol, alertDuck)
                : 0.0F;
        float bossBedPitch = 0.96F + 0.10F * (1.0F - Math.max(0.0F, Math.min(1.0F, lastBossHpRatio)));
        bossTensionBedLoop = updateReactiveLoop(
                mc, bossTensionBedLoop, ModSounds.BOSS_COMBAT_TENSION_BED.get(), SoundSource.AMBIENT,
                false, 0.0D, 0.0D, 0.0D, bossBedVol, bossBedPitch, 0.025F
        );

        float layerDuck = AcousticPhysicsEngine.computeLayerDuckMultiplier(
                lastBossNearby, alertDuck, MycelialClientConfig.dynamicAudioDucking());

        // 2e2. Sinkhole thermal spore-wind updraft bed (7th reactive loop near SporeUpdraftVentBlock)
        BlockPos updraftPos = ClientAcousticController.getNearestUpdraftVentPos();
        float updraftVol = 0.0F;
        double ux = mc.player.getX();
        double uy = mc.player.getY();
        double uz = mc.player.getZ();
        if (envVol > 0.01F && updraftPos != null && MycelialClientConfig.sinkholeUpdraftBedEnabled()) {
            double dSq = updraftPos.distSqr(mc.player.blockPosition());
            updraftVol = AcousticPhysicsEngine.computeSinkholeUpdraftLoopVolume(
                    dSq, 18.0D, envVol, layerDuck, true);
            ux = updraftPos.getX() + 0.5D;
            uy = updraftPos.getY() + 1.0D;
            uz = updraftPos.getZ() + 0.5D;
        }
        sinkholeUpdraftLoop = updateReactiveLoop(
                mc, sinkholeUpdraftLoop, ModSounds.SINKHOLE_UPDRAFT_LOOP.get(), SoundSource.AMBIENT,
                true, ux, uy, uz, updraftVol, 1.0F, 0.025F
        );

        // 2e3. Adaptive Hive Tension Exploration Bed (8th) & Swarm Skirmish Combat Bed (9th)
        if (tickCounter % 5 == 2) {
            lastNearbyHostileCount = envVol > 0.01F && MycelialClientConfig.adaptiveAmbientLayersEnabled()
                    ? mc.level.getEntitiesOfClass(
                            com.alex.mycelialmind.entity.MycelialMob.class,
                            mc.player.getBoundingBox().inflate(24.0D),
                            m -> m.isAlive() && !(m instanceof MycelialBossMob)
                    ).size()
                    : 0;
        }
        boolean adaptiveLayersEnabled = MycelialClientConfig.adaptiveAmbientLayersEnabled();
        double threatPct = ClientHiveCache.getThreatLevel();
        float explorationBedVol = envVol > 0.01F
                ? AcousticPhysicsEngine.computeAdaptiveExplorationBedVolume(
                        threatPct, lastBossNearby, envVol, layerDuck, adaptiveLayersEnabled)
                : 0.0F;
        hiveTensionBedLowLoop = updateReactiveLoop(
                mc, hiveTensionBedLowLoop, ModSounds.HIVE_TENSION_BED_LOW.get(), SoundSource.AMBIENT,
                false, 0.0D, 0.0D, 0.0D, explorationBedVol, 1.0F, 0.02F
        );
        float swarmBedVol = envVol > 0.01F
                ? AcousticPhysicsEngine.computeSwarmCombatBedVolume(
                        lastNearbyHostileCount, threatPct, lastBossNearby, envVol, layerDuck, adaptiveLayersEnabled)
                : 0.0F;
        hiveSwarmCombatBedLoop = updateReactiveLoop(
                mc, hiveSwarmCombatBedLoop, ModSounds.HIVE_SWARM_COMBAT_BED.get(), SoundSource.AMBIENT,
                false, 0.0D, 0.0D, 0.0D, swarmBedVol, 1.0F, 0.025F
        );

        // 2e4. Periodic 3D directional spatial micro-emitters (drips, hyphal creaks, whispers, sanctum chimes)
        if (microAmbienceCooldownTicks > 0) {
            microAmbienceCooldownTicks--;
        } else if (envVol > 0.01F && MycelialClientConfig.spatialMicroAmbienceEnabled()) {
            AcousticPhysicsEngine.AcousticEnvironment env = ClientAcousticController.getCurrentEnvironment();
            SoundEvent microEvt = switch (env) {
                case SANCTUM_VAULT -> ModSounds.SANCTUM_HARMONIC_CHIME.get();
                case DEEP_MYCELIUM_ABYSS -> mc.level.random.nextBoolean()
                        ? ModSounds.DEEP_MYCELIUM_WHISPER.get()
                        : ModSounds.CAVERN_SPORE_DRIP.get();
                case CAVERN_SINKHOLE -> mc.level.random.nextBoolean()
                        ? ModSounds.CAVERN_SPORE_DRIP.get()
                        : ModSounds.SUBTERRANEAN_HYPHAL_CREAK.get();
                default -> null;
            };
            if (microEvt != null) {
                for (int attempt = 0; attempt < 3; attempt++) {
                    double ang = mc.level.random.nextDouble() * Math.PI * 2.0D;
                    double rad = 6.0D + mc.level.random.nextDouble() * 7.5D;
                    double tx = mc.player.getX() + Math.cos(ang) * rad;
                    double tz = mc.player.getZ() + Math.sin(ang) * rad;
                    double ty = mc.player.getEyeY() + (mc.level.random.nextDouble() * 5.0D - 1.5D);
                    BlockPos probe = BlockPos.containing(tx, ty, tz);
                    if (mc.level.hasChunkAt(probe) && mc.level.getBlockState(probe).isAir()) {
                        mc.level.playLocalSound(
                                tx, ty, tz, microEvt, SoundSource.AMBIENT,
                                0.34F * envVol * layerDuck,
                                0.94F + mc.level.random.nextFloat() * 0.12F,
                                false
                        );
                        break;
                    }
                }
            }
            microAmbienceCooldownTicks = 95 + mc.level.random.nextInt(70);
        }

        // 2f. Nearby Primordial Bioluminescent Mob resonance pulse (dedicated event)
        if (envVol > 0.01F
                && MycelialClientConfig.hiveAdaptationAudioAlerts()
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
                        ModSounds.PRIMORDIAL_PRESENCE_PULSE.get(), SoundSource.HOSTILE,
                        0.68F * envVol, 1.0F, false
                );
                triggerDucking(25, 0.35F);
                primordialAlertCooldownTicks = 200;
            }
        }
    }
}
