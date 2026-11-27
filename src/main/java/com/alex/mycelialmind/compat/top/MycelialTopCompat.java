package com.alex.mycelialmind.compat.top;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.BioluminescentMycelialCapBlock;
import com.alex.mycelialmind.block.EphemeralChitinBlock;
import com.alex.mycelialmind.block.HiveAltarBlock;
import com.alex.mycelialmind.block.LuminescentAmberVeinBlock;
import com.alex.mycelialmind.block.MatriarchAltarBlock;
import com.alex.mycelialmind.block.MycelialRiftBlock;
import com.alex.mycelialmind.block.MycelialTendrilsBlock;
import com.alex.mycelialmind.block.PurificationPylonBlock;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.block.RootCapShelfBlock;
import com.alex.mycelialmind.block.RootSapMossBlock;
import com.alex.mycelialmind.block.SovereignNexusBlock;
import com.alex.mycelialmind.block.SporeBulbBlock;
import com.alex.mycelialmind.block.SporeUpdraftVentBlock;
import com.alex.mycelialmind.block.SporeVentBlock;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.HyphalVeinEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.SporeGliderEntity;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.hive.AcquisitionDifficultyEngine;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.SynapticNodeType;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

import java.util.function.Function;

/**
 * Optional runtime bridge for The One Probe (TOP), registered via IMC only when
 * {@code theoneprobe} is loaded so Mycelial Mind runs cleanly standalone.
 */
public final class MycelialTopCompat {
    public static final String TOP_MODID = "theoneprobe";
    private static final String IMC_GET_TOP = "getTheOneProbe";

    private MycelialTopCompat() {
    }

    public static void enqueueImc(InterModEnqueueEvent event) {
        ModList modList = ModList.get();
        if (modList == null || !modList.isLoaded(TOP_MODID)) {
            return;
        }
        InterModComms.sendTo(TOP_MODID, IMC_GET_TOP, Plugin::new);
        MycelialMind.LOGGER.info("Queued The One Probe IMC registration.");
    }

    /**
     * Separate inner class so {@code mcjty.theoneprobe.api.*} classes are only resolved
     * when {@code theoneprobe} is present at runtime.
     */
    public static final class Plugin implements Function<ITheOneProbe, Void> {
        private static final ResourceLocation BLOCK_ID =
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "top_block_info");
        private static final String ENTITY_ID = MycelialMind.MODID + ":top_entity_info";

        @Override
        public Void apply(ITheOneProbe probe) {
            probe.registerEntityProvider(new EntityInfoProvider());
            probe.registerProvider(new BlockInfoProvider());
            return null;
        }

        private static final class EntityInfoProvider implements IProbeInfoEntityProvider {
            @Override
            public String getID() {
                return ENTITY_ID;
            }

            @Override
            public void addProbeEntityInfo(
                    ProbeMode mode,
                    IProbeInfo probeInfo,
                    Player player,
                    Level level,
                    Entity entity,
                    IProbeHitEntityData data
            ) {
                if (entity instanceof MycelialMob mob) {
                    if (mob instanceof SynapticMimicEntity mimic) {
                        boolean hasVision = player != null && player.hasEffect(ModEffects.SYNAPTIC_VISION);
                        if (mimic.isSpoofingAlliance() && !hasVision) {
                            probeInfo.mcText(Component.translatable("jade.mycelialmind.rank",
                                    MycelialText.rank(EvolutionRank.BASE)));
                            probeInfo.mcText(Component.translatable("jade.mycelialmind.strain",
                                    MycelialText.strain(mimic.getSpoofedStrain())));
                            probeInfo.mcText(Component.translatable("jade.mycelialmind.allied_escort"));
                            return;
                        }
                    }
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.rank",
                            MycelialText.rank(mob.getEvolutionRank())));
                    if (mob.getHiveStrain().isSchismStrain()) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.strain",
                                MycelialText.strain(mob.getHiveStrain())));
                    }
                    var traits = mob.getTraits();
                    var tier2 = mob.getTier2Traits();
                    if (!traits.isEmpty()) {
                        MutableComponent list = Component.empty();
                        boolean first = true;
                        for (AdaptationTrait t : traits) {
                            if (!first) {
                                list.append(", ");
                            }
                            list.append(MycelialText.trait(t));
                            if (tier2.contains(t)) {
                                list.append(" [N2]");
                            }
                            first = false;
                        }
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.traits", list));
                    }
                    var synergies = mob.getSynergies();
                    if (!synergies.isEmpty()) {
                        MutableComponent synList = Component.empty();
                        boolean first = true;
                        for (HiveSynergy s : synergies) {
                            if (!first) {
                                synList.append(", ");
                            }
                            synList.append(MycelialText.synergy(s));
                            first = false;
                        }
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.synergies", synList));
                    }
                    if (mob.isDesynced()) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.desync",
                                (int) Math.ceil(mob.getDesyncTicks() / 20.0)));
                    } else if (mob.getCommandRole() == SynapticCommandSystem.CommandRole.LEADER) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.leader"));
                    }
                    if (mob.isPhalanxActive()) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.phalanx"));
                    }
                    if (mob.isFrenzyActive()) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.frenzy",
                                (int) Math.ceil(mob.getFrenzyTicks() / 20.0)));
                    }
                    if (mob.isPrimordialVariant()) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.primordial"));
                    }
                    if (mob instanceof SynapticMimicEntity mimic) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.mimic_state",
                                Component.translatable(mimic.getMimicPersona().translationKey()),
                                Component.translatable(mimic.getMimicStance().translationKey())));
                        if (mimic.getParasitizedMutagen() != null) {
                            probeInfo.mcText(Component.translatable("jade.mycelialmind.mimic_mutagen",
                                    MycelialText.mutagen(mimic.getParasitizedMutagen())));
                        }
                    }
                } else if (entity instanceof MycelialChimeraEntity chimera) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.rank",
                            MycelialText.rank(chimera.getEvolutionRank())));
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.chimera_genes",
                            MycelialText.trait(chimera.getChassisGene()),
                            MycelialText.trait(chimera.getMutationGene())));
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.chimera_mode",
                            MycelialText.chimeraMode(chimera.getCommandMode()),
                            MycelialText.rank(chimera.getEvolutionRank())));
                } else if (entity instanceof PurifierDroneEntity drone) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.drone_mode",
                            Component.translatable(drone.isPatrolMode()
                                    ? "chimera_mode.mycelialmind.patrol"
                                    : "chimera_mode.mycelialmind.follow")));
                    if (drone.isJammed()) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.drone_jammed",
                                (int) Math.ceil(drone.getJammedTicks() / 20.0)));
                    }
                } else if (entity instanceof ColonySymbiontEntity symbiont) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.symbiont_role",
                            Component.translatable(symbiont.getCaste().nameKey()),
                            Component.translatable(symbiont.getRank().nameKey())));
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.symbiont_morale",
                            Math.round(symbiont.getMorale())));
                    if (symbiont.isStandby()) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.symbiont_standby"));
                    }
                    if ("dialect_observing".equals(symbiont.getTaskState())) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.symbiont_dialect_observing"));
                    }
                } else if (entity instanceof SporeGliderEntity) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.spore_glider_pollinator"));
                } else if (entity instanceof HyphalVeinEntity vein) {
                    probeInfo.mcText(Component.translatable(vein.isCharged()
                            ? "jade.mycelialmind.hyphal_vein_charged"
                            : "jade.mycelialmind.hyphal_vein_dormant"));
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.hyphal_vein_strength",
                            vein.getEffectiveStrength() + 1));
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.hyphal_vein_nutrient",
                            vein.getNutrient()));
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.hyphal_vein_turgor",
                            Math.round(vein.getTurgor() * 100.0F)));
                    probeInfo.mcText(Component.translatable(vein.hasRoot()
                            ? "jade.mycelialmind.hyphal_vein_root_yes"
                            : "jade.mycelialmind.hyphal_vein_root_no",
                            vein.getNodeCount()));
                }
            }
        }

        private static final class BlockInfoProvider implements IProbeInfoProvider {
            @Override
            public ResourceLocation getID() {
                return BLOCK_ID;
            }

            @Override
            public void addProbeInfo(
                    ProbeMode mode,
                    IProbeInfo probeInfo,
                    Player player,
                    Level level,
                    BlockState state,
                    IProbeHitData data
            ) {
                BlockPos pos = data.getPos();
                BlockEntity be = level.getBlockEntity(pos);
                Block block = state.getBlock();

                if (be instanceof SynapticNodeBlockEntity node) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.node_info",
                            MycelialText.nodeType(node.getNodeType()),
                            node.getBiomassCharge(),
                            MycelialText.strain(node.getSchismStrain())));
                    if (node.getNodeType() == SynapticNodeType.SYMBIOTIC) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.node_stability",
                                node.getSymbioticStability()));
                    }
                } else if (be instanceof PurificationTurretBlockEntity turret) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.turret_info",
                            turret.getTier(),
                            (int) Math.round(turret.getRangeRadius())));
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.turret_ammo",
                            turret.getAmmo(),
                            AcquisitionDifficultyEngine.TURRET_MAX_AMMO));
                    if (turret.isJammed()) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.turret_jammed",
                                (int) Math.ceil(turret.getJammedTicks() / 20.0)));
                    }
                } else if (block instanceof PurificationPylonBlock) {
                    MonumentFocusBlockEntity focus = be instanceof MonumentFocusBlockEntity mf ? mf : null;
                    boolean smothered = AntibodyCystBlock.countAdjacentCysts(level, pos)
                            >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
                    boolean depleted = focus != null && focus.isDepleted();
                    probeInfo.mcText(Component.translatable(smothered
                            ? "jade.mycelialmind.pylon_smothered"
                            : (depleted ? "jade.mycelialmind.pylon_depleted" : "jade.mycelialmind.pylon_active")));
                    int tier = focus != null ? focus.getTier() : 1;
                    int baseRadius = MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
                    int radius = focus != null ? focus.getEffectiveRadius(baseRadius) : baseRadius;
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.pylon_tier", tier, radius));
                    int charge = focus != null ? focus.getPurificationCharge() : AcquisitionDifficultyEngine.PYLON_MAX_CHARGE;
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.pylon_charge",
                            charge,
                            AcquisitionDifficultyEngine.PYLON_MAX_CHARGE));
                } else if (block instanceof AntibodyCystBlock) {
                    boolean active = state.hasProperty(AntibodyCystBlock.ACTIVE)
                            && state.getValue(AntibodyCystBlock.ACTIVE);
                    probeInfo.mcText(Component.translatable(active
                            ? "jade.mycelialmind.cyst_active"
                            : "jade.mycelialmind.cyst_withered"));
                    if (level instanceof ServerLevel serverLevel) {
                        int sens = DimensionalHomeostasisSavedData.get(serverLevel)
                                .getSensitization(new ChunkPos(pos));
                        if (sens > 0) {
                            probeInfo.mcText(Component.translatable("jade.mycelialmind.sensitization", sens));
                        }
                    }
                } else if (be instanceof SynapticBenchBlockEntity bench) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.bench_tier", bench.getLabTier()));
                    var out = bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_OUTPUT);
                    if (out.isEmpty()) {
                        var details = bench.computeSynthesisDetails();
                        if (details.blockedByAct() != null) {
                            probeInfo.mcText(Component.translatable(
                                    "gui.mycelialmind.bench.requires_act",
                                    details.blockedByAct().actNumber(),
                                    MycelialText.progressionTier(details.blockedByAct())));
                        } else {
                            probeInfo.mcText(Component.translatable("jade.mycelialmind.bench_ready"));
                        }
                    } else {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.bench_output", out.getHoverName()));
                    }
                } else if (block instanceof BioluminescentMycelialCapBlock) {
                    boolean mature = state.hasProperty(BioluminescentMycelialCapBlock.MATURE)
                            && state.getValue(BioluminescentMycelialCapBlock.MATURE);
                    probeInfo.mcText(Component.translatable(mature
                            ? "jade.mycelialmind.flora_mature"
                            : "jade.mycelialmind.flora_regrowing"));
                } else if (block instanceof MycelialTendrilsBlock) {
                    boolean mature = state.hasProperty(MycelialTendrilsBlock.MATURE)
                            && state.getValue(MycelialTendrilsBlock.MATURE);
                    probeInfo.mcText(Component.translatable(mature
                            ? "jade.mycelialmind.flora_mature"
                            : "jade.mycelialmind.flora_regrowing"));
                } else if (block instanceof SporeBulbBlock) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.spore_bulb"));
                } else if (block instanceof ReclaimedBloomBlock) {
                    probeInfo.mcText(Component.translatable(state.getValue(ReclaimedBloomBlock.MATURE)
                            ? "jade.mycelialmind.flora_mature"
                            : "jade.mycelialmind.flora_regrowing"));
                    boolean anchored = LuminescentAmberVeinBlock.hasAdjacentConduit(level, pos);
                    var belowState = level.getBlockState(pos.below());
                    boolean fertileSoil = belowState.is(ModBlocks.MYCELIAL_STONE.get())
                            || belowState.is(ModBlocks.CREEPING_MYCELIUM.get());
                    probeInfo.mcText(Component.translatable(anchored
                            ? "jade.mycelialmind.bloom_anchored"
                            : (fertileSoil ? "jade.mycelialmind.bloom_soil_ok" : "jade.mycelialmind.bloom_needs_soil")));
                } else if (block instanceof EphemeralChitinBlock) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.ephemeral_chitin"));
                } else if (block instanceof MycelialRiftBlock) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.mycelial_rift"));
                } else if (block instanceof HiveAltarBlock) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.hive_altar"));
                } else if (block instanceof MatriarchAltarBlock) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.matriarch_altar"));
                } else if (block instanceof SporeVentBlock) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.spore_vent"));
                } else if (block instanceof SporeUpdraftVentBlock) {
                    boolean active = state.hasProperty(SporeUpdraftVentBlock.ACTIVE)
                            && state.getValue(SporeUpdraftVentBlock.ACTIVE);
                    probeInfo.mcText(Component.translatable(active
                            ? "jade.mycelialmind.spore_updraft_vent_active"
                            : "jade.mycelialmind.spore_updraft_vent_inactive"));
                } else if (block instanceof RootSapMossBlock) {
                    boolean ready = state.hasProperty(RootSapMossBlock.SAP_READY)
                            && state.getValue(RootSapMossBlock.SAP_READY);
                    probeInfo.mcText(Component.translatable(ready
                            ? "jade.mycelialmind.root_sap_moss_ready"
                            : "jade.mycelialmind.root_sap_moss_depleted"));
                } else if (block instanceof RootCapShelfBlock) {
                    probeInfo.mcText(Component.translatable("jade.mycelialmind.root_cap_shelf"));
                } else if (block instanceof SovereignNexusBlock && level instanceof ServerLevel serverLevel) {
                    SovereignColonyData colony = SovereignColonySavedData.get(serverLevel).getByNexus(pos);
                    if (colony != null) {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.sovereign_nexus_info",
                                colony.getTier(),
                                colony.getRoster().size(),
                                colony.getPopulationCap(),
                                colony.getBiomassStored()));
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.sovereign_nexus_morale",
                                (int) Math.round(colony.getColonyMorale())));
                    } else {
                        probeInfo.mcText(Component.translatable("jade.mycelialmind.sovereign_nexus_dormant"));
                    }
                }
            }
        }
    }
}
