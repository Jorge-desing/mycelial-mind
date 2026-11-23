package com.alex.mycelialmind.item;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.client.screen.ClientCodexOpener;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine;
import com.alex.mycelialmind.hive.HyphalSiegeDirector;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionDirector;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.network.PostMatriarchExpeditionSyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialParticles;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Diagnostic scanner that reports Hive Mind threat level, active mutations,
 * nearest Mycelial Node, or nearest Hive Heart Sanctum structure.
 */
public class BioScannerItem extends Item {
    private static final TagKey<Structure> SANCTUM_TAG = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "sanctums")
    );

    public BioScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerDiscoveryData.unlockMilestone(serverPlayer, ProgressionGoalEngine.MILESTONE_BIO_SCANNER);
            if (PostMatriarchExpeditionDirector.handleScannerUseOn(
                    serverPlayer, (ServerLevel) level, pos, player.isShiftKeyDown())) {
                return InteractionResult.sidedSuccess(false);
            }
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PurificationTurretBlockEntity turret) {
            if (level instanceof ServerLevel serverLevel) {
                PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
                Component zone = Component.translatable(tracker.isProtected(pos)
                        ? "status.mycelialmind.protected"
                        : "status.mycelialmind.exposed");
                Component status = turret.isJammed()
                        ? Component.translatable("status.mycelialmind.jammed",
                        Math.max(1, (int) Math.ceil(turret.getJammedTicks() / 20.0)))
                        : Component.translatable("status.mycelialmind.operational");
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_turret_inspect",
                        turret.getTier(),
                        (int) turret.getRangeRadius(),
                        turret.getFireInterval(serverLevel),
                        zone,
                        status
                ).withStyle(turret.isJammed() ? ChatFormatting.YELLOW : ChatFormatting.AQUA));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.6F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (be instanceof SynapticNodeBlockEntity nodeBe) {
            if (level instanceof ServerLevel serverLevel) {
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_node_inspect",
                        MycelialText.nodeType(nodeBe.getNodeType()),
                        MycelialText.strain(nodeBe.getSchismStrain()),
                        String.format("%.1f", nodeBe.getVolatileTotal()),
                        nodeBe.getSymbioticStability()
                ).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (player instanceof ServerPlayer serverPlayer
                        && !(level.getBlockState(pos).getBlock() instanceof com.alex.mycelialmind.block.SymbioticNodeBlock)) {
                    HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                    PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
                    discovery.recordNodeThreatBaseline(hive.getThreatLevel(), serverLevel.dimension().location().toString());
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "message.mycelialmind.scanner_node_threat_recorded",
                            String.format("%.1f", hive.getThreatLevel())
                    ).withStyle(ChatFormatting.AQUA));
                    DiscoverySyncPayload.syncToPlayer(serverPlayer);
                }
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.getBlockState(pos).is(ModBlocks.PURIFICATION_PYLON.get())) {
            if (level instanceof ServerLevel serverLevel) {
                PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
                boolean smothered = tracker.isPylonSmothered(pos);
                int cystFaces = AntibodyCystBlock.countAdjacentCysts(serverLevel, pos);
                double stress = DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)
                        ? DimensionalHomeostasisSavedData.get(serverLevel).getEffectiveNeighborhoodStress(new ChunkPos(pos))
                        : 0.0;
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_pylon_inspect",
                        Component.translatable(smothered ? "jade.mycelialmind.pylon_smothered" : "jade.mycelialmind.pylon_active"),
                        cystFaces,
                        DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD,
                        String.format("%.0f", stress)
                ).withStyle(smothered ? ChatFormatting.RED : ChatFormatting.AQUA));
                int pylonTier = be instanceof MonumentFocusBlockEntity focus ? focus.getTier() : 1;
                int pylonRadius = be instanceof MonumentFocusBlockEntity focus
                        ? focus.getEffectiveRadius(MycelialConfig.PYLON_PURIFICATION_RADIUS.get())
                        : MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_pylon_tier",
                        pylonTier,
                        pylonRadius
                ).withStyle(ChatFormatting.AQUA));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.getBlockState(pos).is(ModBlocks.ANTIBODY_CYST.get())) {
            if (level instanceof ServerLevel serverLevel) {
                var state = level.getBlockState(pos);
                boolean active = state.hasProperty(AntibodyCystBlock.ACTIVE) && state.getValue(AntibodyCystBlock.ACTIVE);
                var homeoData = DimensionalHomeostasisSavedData.get(serverLevel);
                ChunkPos cp = new ChunkPos(pos);
                double stress = homeoData.getStress(cp);
                int sens = homeoData.getSensitization(cp);
                int signal = com.alex.mycelialmind.util.MycelialRedstoneTelemetry.antibodyCystSignal(
                        active, stress, AntibodyCystBlock.countAdjacentCysts(serverLevel, pos)
                );
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_cyst_inspect",
                        Component.translatable(active ? "jade.mycelialmind.cyst_active" : "jade.mycelialmind.cyst_withered"),
                        String.format("%.0f", stress),
                        sens,
                        signal
                ).withStyle(active ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.3F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.getBlockState(pos).is(ModBlocks.EPHEMERAL_CHITIN.get())) {
            if (level instanceof ServerLevel serverLevel) {
                boolean protectedZone = PurificationZoneTracker.get(serverLevel).isProtected(pos);
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_ephemeral_inspect",
                        Component.translatable(protectedZone
                                ? "jade.mycelialmind.pylon_active"
                                : "jade.mycelialmind.ephemeral_chitin")
                ).withStyle(ChatFormatting.DARK_PURPLE));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.35F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.getBlockEntity(pos) instanceof SynapticBenchBlockEntity bench) {
            if (level instanceof ServerLevel serverLevel) {
                var details = bench.computeSynthesisDetails(
                        com.alex.mycelialmind.world.structure.StructureProgressionPolicy.resolveTier(serverLevel));
                Component outputComp;
                if (!details.output().isEmpty()) {
                    outputComp = details.output().getHoverName();
                } else if (details.blockedByAct() != null) {
                    outputComp = Component.translatable(
                            "gui.mycelialmind.bench.requires_act",
                            details.blockedByAct().actNumber(),
                            MycelialText.progressionTier(details.blockedByAct()));
                } else {
                    outputComp = Component.translatable("jade.mycelialmind.bench_ready");
                }
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_bench_inspect",
                        outputComp
                ).withStyle(details.blockedByAct() != null ? ChatFormatting.GOLD : ChatFormatting.AQUA));
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_bench_tier",
                        bench.getLabTier()
                ).withStyle(ChatFormatting.AQUA));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.45F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        var clickedState = level.getBlockState(pos);
        if (clickedState.is(ModBlocks.SPORE_BULB.get())) {
            if (level instanceof ServerLevel serverLevel) {
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                var site = hive.getBlindSpotSite(pos);
                if (site == null) {
                    site = hive.findBlindSpotSiteNear(pos, 1.5);
                }
                if (site != null) {
                    int light = HyphalSiegeDirector.measureEffectiveBlockLight(serverLevel, site.getCorePos());
                    String statusKey = light >= HyphalAdaptiveArchitectureEngine.PHOTOPHOBIC_STALL_LIGHT
                            ? "status.mycelialmind.blind_spot_stalled"
                            : "status.mycelialmind.blind_spot_growing";
                    String paletteId = site.getPrimaryBlockId().isBlank() ? "default" : site.getPrimaryBlockId();
                    player.sendSystemMessage(Component.translatable(
                            "message.mycelialmind.scanner_blind_spot_inspect",
                            site.getPlacedSteps(),
                            site.getTotalSteps(),
                            paletteId,
                            light,
                            Component.translatable(statusKey)
                    ).withStyle(light >= HyphalAdaptiveArchitectureEngine.PHOTOPHOBIC_STALL_LIGHT
                            ? ChatFormatting.GOLD : ChatFormatting.RED));
                    serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
                    return InteractionResult.sidedSuccess(false);
                }
            } else {
                return InteractionResult.sidedSuccess(true);
            }
        }
        if (clickedState.is(ModBlocks.HYPHAL_VEIN.get()) || clickedState.is(ModBlocks.CREEPING_MYCELIUM.get())) {
            if (level instanceof ServerLevel serverLevel) {
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                ChunkPos cp = new ChunkPos(pos);
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_hyphal_vein_inspect",
                        String.format("%.1f", hive.getHyphalDensity(cp)),
                        hive.getHyphalTraversalCount(cp),
                        Component.translatable(hive.isTrackedHyphalVein(pos)
                                ? "message.mycelialmind.yes" : "message.mycelialmind.no")
                ).withStyle(ChatFormatting.LIGHT_PURPLE));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.45F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (clickedState.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                || clickedState.is(ModBlocks.MYCELIAL_TENDRILS.get())) {
            if (level instanceof ServerLevel serverLevel) {
                boolean mature = clickedState.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                        ? clickedState.getValue(com.alex.mycelialmind.block.BioluminescentMycelialCapBlock.MATURE)
                        : clickedState.getValue(com.alex.mycelialmind.block.MycelialTendrilsBlock.MATURE);
                int redstoneSignal = com.alex.mycelialmind.util.MycelialRedstoneTelemetry.regenerativeFloraSignal(mature);
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_flora_inspect",
                        clickedState.getBlock().getName(),
                        Component.translatable(mature ? "jade.mycelialmind.flora_mature" : "jade.mycelialmind.flora_regrowing"),
                        redstoneSignal
                ).withStyle(mature ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.45F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        String blockInfoKey = null;
        if (clickedState.is(ModBlocks.HIVE_ALTAR.get())) {
            blockInfoKey = "jade.mycelialmind.hive_altar";
        } else if (clickedState.is(ModBlocks.MATRIARCH_ALTAR.get())) {
            blockInfoKey = "jade.mycelialmind.matriarch_altar";
        } else if (clickedState.is(ModBlocks.MYCELIAL_RIFT.get())) {
            blockInfoKey = "jade.mycelialmind.mycelial_rift";
        } else if (clickedState.is(ModBlocks.SPORE_VENT.get())) {
            blockInfoKey = "jade.mycelialmind.spore_vent";
        } else if (clickedState.is(ModBlocks.SPORE_BULB.get())) {
            blockInfoKey = "jade.mycelialmind.spore_bulb";
        }
        if (blockInfoKey != null) {
            if (level instanceof ServerLevel serverLevel) {
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_block_inspect",
                        clickedState.getBlock().getName(),
                        Component.translatable(blockInfoKey)
                ).withStyle(ChatFormatting.LIGHT_PURPLE));
                serverLevel.playSound(null, pos, ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerDiscoveryData.unlockMilestone(serverPlayer, ProgressionGoalEngine.MILESTONE_BIO_SCANNER);
        }
        if (target instanceof MycelialMob mob) {
            if (player.level() instanceof ServerLevel serverLevel) {
                if (mob instanceof SynapticMimicEntity mimic) {
                    if (!mimic.isUnmasked()) {
                        mimic.unmaskWithSynapticShock(serverLevel, player);
                    }
                    Component mutagenComp = mimic.getParasitizedMutagen() != null
                            ? MycelialText.mutagen(mimic.getParasitizedMutagen())
                            : Component.translatable("message.mycelialmind.none");
                    player.sendSystemMessage(Component.translatable(
                            "message.mycelialmind.scanner_mimic_inspect",
                            Component.translatable(mimic.getMimicPersona().translationKey()),
                            Component.translatable(mimic.getMimicStance().translationKey()),
                            mutagenComp
                    ).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                Component tactical = formatMobTacticalStatus(mob);
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_mob_inspect",
                        mob.getDisplayName(),
                        MycelialText.rank(mob.getEvolutionRank()),
                        MycelialText.strain(mob.getHiveStrain()),
                        String.format("%.0f/%.0f", mob.getHealth(), mob.getMaxHealth()),
                        tactical
                ).withStyle(ChatFormatting.LIGHT_PURPLE));

                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                StimulusType dominantStimulus = null;
                double dominantScore = 0.0D;
                for (Map.Entry<StimulusType, Double> entry : hive.getStimulusScores().entrySet()) {
                    if (entry.getValue() > dominantScore) {
                        dominantScore = entry.getValue();
                        dominantStimulus = entry.getKey();
                    }
                }
                boolean witnessedDeflection = player instanceof ServerPlayer sp
                        && sp.getData(ModAttachments.DISCOVERY).hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_DEFLECTION_WITNESSED);
                ProgressionGoalEngine.CreatureObservation observation = ProgressionGoalEngine.resolveCreatureObservation(
                        mob.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT),
                        hive.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT),
                        witnessedDeflection,
                        hive.getStimulusScore(StimulusType.PROJECTILE),
                        dominantStimulus,
                        dominantScore,
                        MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get()
                );
                sendObservationMessage(player, observation, dominantStimulus);

                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
                    discovery.recordSpecies(mob.getType());
                    discovery.recordStrain(mob.getHiveStrain());
                    for (AdaptationTrait trait : mob.getTraits()) {
                        discovery.recordTrait(trait);
                    }
                    discovery.recordCreatureObservation(observation, serverLevel.dimension().location().toString());
                    if (mob.isPrimordialVariant()) {
                        discovery.incrementRareVariantsFound();
                        PlayerDiscoveryData.unlockSecret(serverPlayer, ProgressionGoalEngine.SECRET_PRIMORDIAL_SPECIMEN);
                        player.sendSystemMessage(Component.translatable("message.mycelialmind.scanner_primordial_detected")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    }
                    DiscoverySyncPayload.syncToPlayer(serverPlayer);
                    if (serverPlayer.connection != null) {
                        PacketDistributor.sendToPlayer(serverPlayer, HiveSyncPayload.forPlayer(serverLevel, serverPlayer));
                    }
                }
                serverLevel.playSound(null, mob.blockPosition(), ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide());
        }
        if (target instanceof MycelialChimeraEntity chimera) {
            if (player.level() instanceof ServerLevel serverLevel) {
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_chimera_inspect",
                        String.format("%.0f/%.0f", chimera.getHealth(), chimera.getMaxHealth()),
                        MycelialText.rank(chimera.getEvolutionRank()),
                        MycelialText.chimeraMode(chimera.getCommandMode()),
                        MycelialText.trait(chimera.getChassisGene()),
                        MycelialText.trait(chimera.getMutationGene())
                ).withStyle(ChatFormatting.GREEN));
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
                    discovery.recordSpecies(chimera.getType());
                    discovery.recordTrait(chimera.getChassisGene());
                    discovery.recordTrait(chimera.getMutationGene());
                    DiscoverySyncPayload.syncToPlayer(serverPlayer);
                }
                serverLevel.playSound(null, chimera.blockPosition(), ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide());
        }
        if (target instanceof PurifierDroneEntity drone) {
            if (player.level() instanceof ServerLevel serverLevel) {
                Component mode = MycelialText.chimeraMode(drone.isPatrolMode()
                        ? MycelialChimeraEntity.CommandMode.PATROL
                        : MycelialChimeraEntity.CommandMode.FOLLOW);
                Component status = drone.isJammed()
                        ? Component.translatable("status.mycelialmind.jammed",
                        Math.max(1, (int) Math.ceil(drone.getJammedTicks() / 20.0)))
                        : Component.translatable("status.mycelialmind.operational");
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_drone_inspect",
                        String.format("%.0f/%.0f", drone.getHealth(), drone.getMaxHealth()),
                        mode,
                        status
                ).withStyle(drone.isJammed() ? ChatFormatting.YELLOW : ChatFormatting.AQUA));
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
                    discovery.recordSpecies(drone.getType());
                    DiscoverySyncPayload.syncToPlayer(serverPlayer);
                }
                serverLevel.playSound(null, drone.blockPosition(), ModSounds.BIO_SCANNER_PING.get(), SoundSource.PLAYERS, 0.7F, 1.6F);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerDiscoveryData.unlockMilestone(serverPlayer, ProgressionGoalEngine.MILESTONE_BIO_SCANNER);
        }
        if (!player.isShiftKeyDown()) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, HiveSyncPayload.forPlayer(serverLevel, serverPlayer));
                PostMatriarchExpeditionSyncPayload.syncToPlayer(serverPlayer);
            } else if (level.isClientSide()) {
                ClientCodexOpener.openBioCodex();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (level instanceof ServerLevel serverLevel) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.fail(stack);
            }
            activateThermalPulse(serverLevel, player);
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, HiveSyncPayload.forPlayer(serverLevel, serverPlayer));
            }

            Component traitsComp;
            if (hive.getActiveTraits().isEmpty()) {
                traitsComp = Component.translatable("message.mycelialmind.none");
            } else {
                var combined = Component.empty();
                boolean first = true;
                for (AdaptationTrait t : hive.getActiveTraits()) {
                    if (!first) {
                        combined.append(", ");
                    }
                    combined.append(MycelialText.trait(t));
                    first = false;
                }
                traitsComp = combined;
            }

            player.sendSystemMessage(Component.translatable(
                    "message.mycelialmind.scanner_summary",
                    String.format("%.1f", hive.getThreatLevel()),
                    Component.translatable(tracker.isProtected(player.blockPosition())
                            ? "message.mycelialmind.yes"
                            : "message.mycelialmind.no"),
                    traitsComp
            ).withStyle(ChatFormatting.LIGHT_PURPLE));

            if (DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
                var homeoData = DimensionalHomeostasisSavedData.get(serverLevel);
                ChunkPos cp = new ChunkPos(player.blockPosition());
                double stress = homeoData.getEffectiveNeighborhoodStress(cp);
                int sens = homeoData.getEffectiveNeighborhoodSensitization(cp);
                var stage = DimensionalHomeostasisEngine.stageForStress(stress);
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_homeostasis_status",
                        String.format("%.0f", stress),
                        MycelialText.homeostasisStage(stage),
                        sens
                ).withStyle(ChatFormatting.AQUA));
            }

            var accent = hive.getArchitecturalAccent();
            String accentBlock = accent.hasCustomAccent() ? accent.primaryBlockId() : "default";
            player.sendSystemMessage(Component.translatable(
                    "message.mycelialmind.scanner_hyphal_architecture_summary",
                    String.format("%.1f", hive.getHyphalDensityAt(player.blockPosition())),
                    accentBlock,
                    accent.proportion().id(),
                    String.format("%.1f", accent.typicalRoomSpan()),
                    String.format("%.0f", accent.lightDensity() * 100.0),
                    hive.getBlindSpotSites().size()
            ).withStyle(ChatFormatting.DARK_AQUA));

            BlockPos nearestNode = tracker.findNearestNode(player.blockPosition());
            if (nearestNode != null) {
                int dist = (int) Math.sqrt(nearestNode.distSqr(player.blockPosition()));
                player.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.scanner_nearest_node",
                        dist, nearestNode.getX(), nearestNode.getY(), nearestNode.getZ()).withStyle(ChatFormatting.AQUA));
                if (player instanceof ServerPlayer serverPlayer && tracker.findNearestEnemyNode(player.blockPosition()) != null) {
                    PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
                    discovery.recordNodeThreatBaseline(hive.getThreatLevel(), serverLevel.dimension().location().toString());
                    DiscoverySyncPayload.syncToPlayer(serverPlayer);
                }
            } else {
                BlockPos sanctum = serverLevel.findNearestMapStructure(SANCTUM_TAG, player.blockPosition(), 100, false);
                if (sanctum != null) {
                    int dist = (int) Math.sqrt(sanctum.distSqr(player.blockPosition()));
                    player.sendSystemMessage(Component.translatable(
                            "message.mycelialmind.scanner_nearest_sanctum",
                            dist, sanctum.getX(), sanctum.getZ()).withStyle(ChatFormatting.AQUA));
                } else {
                    player.sendSystemMessage(Component.translatable("message.mycelialmind.scanner_no_targets")
                            .withStyle(ChatFormatting.GREEN));
                }
            }
            player.getCooldowns().addCooldown(this, DimensionalHomeostasisEngine.THERMAL_PULSE_COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void activateThermalPulse(ServerLevel level, Player player) {
        player.addEffect(new MobEffectInstance(
                ModEffects.SYNAPTIC_VISION,
                DimensionalHomeostasisEngine.THERMAL_PULSE_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));
        level.playSound(null, player.blockPosition(), ModSounds.BIO_SCANNER_THERMAL_PULSE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        MycelialParticles.thermalPulse(level, player);
        player.displayClientMessage(Component.translatable(
                "message.mycelialmind.thermal_pulse_activated",
                DimensionalHomeostasisEngine.THERMAL_PULSE_DURATION_TICKS / 20
        ).withStyle(ChatFormatting.AQUA), true);

        BlockPos origin = player.blockPosition();
        for (SynapticMimicEntity mimic : level.getEntitiesOfClass(
                SynapticMimicEntity.class,
                new net.minecraft.world.phys.AABB(origin).inflate(16.0D),
                m -> m.isAlive() && !m.isUnmasked()
        )) {
            mimic.unmaskWithSynapticShock(level, player);
        }
        if (level.dimension().equals(com.alex.mycelialmind.world.ModDimensions.DEEP_MYCELIUM_LEVEL)) {
            var expedition = com.alex.mycelialmind.hive.PostMatriarchExpeditionData.get(level);
            BlockPos garden = expedition.getActivePos();
            if (garden != null && expedition.getActiveOutcome()
                    == com.alex.mycelialmind.hive.PostMatriarchExpeditionData.Outcome.GARDEN
                    && level.isLoaded(garden) && level.getBlockState(garden).is(ModBlocks.SYMBIOTIC_NODE.get())
                    && garden.distSqr(origin) <= 48 * 48) {
                player.sendSystemMessage(Component.translatable("message.mycelialmind.expedition_flora_vision")
                        .withStyle(ChatFormatting.GREEN));
            }
        }
        BlockPos vaultChest = findNearbySealedVaultChest(level, origin, 16, 12);
        if (vaultChest != null) {
            int dist = (int) Math.round(Math.sqrt(vaultChest.distSqr(origin)));
            player.sendSystemMessage(Component.translatable(
                    "message.mycelialmind.scanner_hidden_vault_detected",
                    dist, vaultChest.getX(), vaultChest.getY(), vaultChest.getZ()
            ).withStyle(ChatFormatting.GOLD));
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerDiscoveryData.unlockSecret(serverPlayer, ProgressionGoalEngine.SECRET_SEALED_BIO_VAULT);
            }
        }
    }

    public static boolean isSealedVaultChest(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.CHEST)) {
            return false;
        }
        int vaultWalls = 0;
        for (Direction dir : Direction.values()) {
            BlockState adj = level.getBlockState(pos.relative(dir));
            if (adj.is(ModBlocks.MYCELIAL_ARCH_BRICK.get()) || adj.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())) {
                vaultWalls++;
            }
        }
        return vaultWalls >= 2;
    }

    private static BlockPos findNearbySealedVaultChest(ServerLevel level, BlockPos origin, int horizRadius, int vertRadius) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(
                origin.offset(-horizRadius, -vertRadius, -horizRadius),
                origin.offset(horizRadius, vertRadius, horizRadius))) {
            if (isSealedVaultChest(level, p)) {
                double dSq = p.distSqr(origin);
                if (dSq < bestDistSq) {
                    bestDistSq = dSq;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    private static void sendObservationMessage(
            Player player,
            ProgressionGoalEngine.CreatureObservation observation,
            StimulusType dominantStimulus
    ) {
        switch (observation) {
            case CONFIRMED_PROJECTILE_ADAPTATION -> player.sendSystemMessage(
                    Component.translatable(observation.translationKey()).withStyle(ChatFormatting.GOLD));
            case LEARNING_PROJECTILE -> player.sendSystemMessage(
                    Component.translatable(observation.translationKey()).withStyle(ChatFormatting.YELLOW));
            case INSUFFICIENT_EVIDENCE -> player.sendSystemMessage(
                    Component.translatable(observation.translationKey()).withStyle(ChatFormatting.GRAY));
            case OTHER_DOMINANT_RESPONSE -> player.sendSystemMessage(
                    Component.translatable(observation.translationKey(),
                            MycelialText.stimulus(dominantStimulus != null ? dominantStimulus : StimulusType.MELEE))
                            .withStyle(ChatFormatting.AQUA));
            case NONE -> { }
        }
    }

    public static Component formatMobTacticalStatus(MycelialMob mob) {
        if (mob.isDesynced()) {
            return Component.translatable("hud.mycelialmind.squad_desynced",
                    Math.max(1, (int) Math.ceil(mob.getDesyncTicks() / 20.0)));
        }
        if (mob.isFrenzyActive()) {
            return Component.translatable("hud.mycelialmind.squad_frenzy",
                    Math.max(1, (int) Math.ceil(mob.getFrenzyTicks() / 20.0)));
        }
        if (mob.isPhalanxActive()) {
            return Component.translatable("hud.mycelialmind.squad_phalanx");
        }
        if (mob.getCommandRole() == SynapticCommandSystem.CommandRole.LEADER) {
            return Component.translatable("hud.mycelialmind.squad_leader");
        }
        if (mob.getCommandRole() == SynapticCommandSystem.CommandRole.LINKED) {
            return Component.translatable("hud.mycelialmind.squad_synced");
        }
        return Component.translatable("hud.mycelialmind.squad_unlinked");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.bio_scanner").withStyle(ChatFormatting.AQUA));
    }
}
