package com.alex.mycelialmind.compat.jade;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.BioluminescentMycelialCapBlock;
import com.alex.mycelialmind.block.EphemeralChitinBlock;
import com.alex.mycelialmind.block.HiveAltarBlock;
import com.alex.mycelialmind.block.HyphalVeinBlock;
import com.alex.mycelialmind.block.IncubatorNodeBlock;
import com.alex.mycelialmind.block.MatriarchAltarBlock;
import com.alex.mycelialmind.block.MycelialNodeBlock;
import com.alex.mycelialmind.block.MycelialRiftBlock;
import com.alex.mycelialmind.block.MycelialTendrilsBlock;
import com.alex.mycelialmind.block.PurificationPylonBlock;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.block.PurificationTurretBlock;
import com.alex.mycelialmind.block.RootCapShelfBlock;
import com.alex.mycelialmind.block.RootSapMossBlock;
import com.alex.mycelialmind.block.SentinelNodeBlock;
import com.alex.mycelialmind.block.SovereignNexusBlock;
import com.alex.mycelialmind.block.SporeBulbBlock;
import com.alex.mycelialmind.block.SporeUpdraftVentBlock;
import com.alex.mycelialmind.block.SporeVentBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.block.SynapticResearchBenchBlock;
import com.alex.mycelialmind.block.entity.HyphalVeinBlockEntity;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.SporeGliderEntity;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientExpeditionCache;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.world.ModDimensions;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Optional Jade integration exposing localized live Hive entity and block telemetry.
 */
@WailaPlugin
public final class MycelialJadePlugin implements IWailaPlugin {
    public static final ResourceLocation ENTITY_UID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "entity_info");
    public static final ResourceLocation BLOCK_UID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "block_info");
    public static final ResourceLocation NEXUS_DATA_UID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "nexus_data");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(NexusServerDataProvider.INSTANCE, SovereignNexusBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(EntityProvider.INSTANCE, MycelialMob.class);
        registration.registerEntityComponent(EntityProvider.INSTANCE, MycelialChimeraEntity.class);
        registration.registerEntityComponent(EntityProvider.INSTANCE, PurifierDroneEntity.class);
        registration.registerEntityComponent(EntityProvider.INSTANCE, ColonySymbiontEntity.class);
        registration.registerEntityComponent(EntityProvider.INSTANCE, SporeGliderEntity.class);

        registration.registerBlockComponent(BlockProvider.INSTANCE, MycelialNodeBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, IncubatorNodeBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, SentinelNodeBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, SymbioticNodeBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, PurificationTurretBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, PurificationPylonBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, AntibodyCystBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, EphemeralChitinBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, SynapticResearchBenchBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, MycelialRiftBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, HiveAltarBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, MatriarchAltarBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, SporeVentBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, BioluminescentMycelialCapBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, MycelialTendrilsBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, SporeBulbBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, HyphalVeinBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, ReclaimedBloomBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, SovereignNexusBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, SporeUpdraftVentBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, RootSapMossBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, RootCapShelfBlock.class);
    }

    public enum EntityProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof MycelialMob mob) {
                if (mob instanceof SynapticMimicEntity mimic) {
                    Minecraft mc = Minecraft.getInstance();
                    boolean hasVision = mc.player != null && mc.player.hasEffect(ModEffects.SYNAPTIC_VISION);
                    if (mimic.isSpoofingAlliance() && !hasVision) {
                        tooltip.add(Component.translatable("jade.mycelialmind.rank",
                                MycelialText.rank(EvolutionRank.BASE)));
                        tooltip.add(Component.translatable("jade.mycelialmind.strain",
                                MycelialText.strain(mimic.getSpoofedStrain())));
                        tooltip.add(Component.translatable("jade.mycelialmind.allied_escort"));
                        return;
                    }
                }
                tooltip.add(Component.translatable("jade.mycelialmind.rank",
                        MycelialText.rank(mob.getEvolutionRank())));
                if (mob.getHiveStrain().isSchismStrain()) {
                    tooltip.add(Component.translatable("jade.mycelialmind.strain",
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
                    tooltip.add(Component.translatable("jade.mycelialmind.traits", list));
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
                    tooltip.add(Component.translatable("jade.mycelialmind.synergies", synList));
                }
                if (mob.isDesynced()) {
                    tooltip.add(Component.translatable("jade.mycelialmind.desync",
                            (int) Math.ceil(mob.getDesyncTicks() / 20.0)));
                } else if (mob.getCommandRole() == SynapticCommandSystem.CommandRole.LEADER) {
                    tooltip.add(Component.translatable("jade.mycelialmind.leader"));
                }
                if (mob.isPhalanxActive()) {
                    tooltip.add(Component.translatable("jade.mycelialmind.phalanx"));
                }
                if (mob.isFrenzyActive()) {
                    tooltip.add(Component.translatable("jade.mycelialmind.frenzy",
                            (int) Math.ceil(mob.getFrenzyTicks() / 20.0)));
                }
                if (mob.isPrimordialVariant()) {
                    tooltip.add(Component.translatable("jade.mycelialmind.primordial"));
                }
                if (mob instanceof SynapticMimicEntity mimic) {
                    tooltip.add(Component.translatable("jade.mycelialmind.mimic_state",
                            Component.translatable(mimic.getMimicPersona().translationKey()),
                            Component.translatable(mimic.getMimicStance().translationKey())));
                    if (mimic.getParasitizedMutagen() != null) {
                        tooltip.add(Component.translatable("jade.mycelialmind.mimic_mutagen",
                                MycelialText.mutagen(mimic.getParasitizedMutagen())));
                    }
                }
            } else if (accessor.getEntity() instanceof MycelialChimeraEntity chimera) {
                tooltip.add(Component.translatable("jade.mycelialmind.rank",
                        MycelialText.rank(chimera.getEvolutionRank())));
                tooltip.add(Component.translatable("jade.mycelialmind.chimera_genes",
                        MycelialText.trait(chimera.getChassisGene()),
                        MycelialText.trait(chimera.getMutationGene())));
                tooltip.add(Component.translatable("jade.mycelialmind.chimera_mode",
                        MycelialText.chimeraMode(chimera.getCommandMode()),
                        MycelialText.rank(chimera.getEvolutionRank())));
            } else if (accessor.getEntity() instanceof PurifierDroneEntity drone) {
                tooltip.add(Component.translatable("jade.mycelialmind.drone_mode",
                        Component.translatable(drone.isPatrolMode()
                                ? "chimera_mode.mycelialmind.patrol"
                                : "chimera_mode.mycelialmind.follow")));
                if (drone.isJammed()) {
                    tooltip.add(Component.translatable("jade.mycelialmind.drone_jammed",
                            (int) Math.ceil(drone.getJammedTicks() / 20.0)));
                }
            } else if (accessor.getEntity() instanceof ColonySymbiontEntity symbiont) {
                tooltip.add(Component.translatable("jade.mycelialmind.symbiont_role",
                        Component.translatable(symbiont.getCaste().nameKey()),
                        Component.translatable(symbiont.getRank().nameKey())));
                tooltip.add(Component.translatable("jade.mycelialmind.symbiont_morale",
                        Math.round(symbiont.getMorale())));
                if (symbiont.isStandby()) {
                    tooltip.add(Component.translatable("jade.mycelialmind.symbiont_standby"));
                }
                if ("dialect_observing".equals(symbiont.getTaskState())) {
                    tooltip.add(Component.translatable("jade.mycelialmind.symbiont_dialect_observing"));
                }
            } else if (accessor.getEntity() instanceof SporeGliderEntity) {
                tooltip.add(Component.translatable("jade.mycelialmind.spore_glider_pollinator"));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ENTITY_UID;
        }
    }

    public enum BlockProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof SynapticNodeBlockEntity node) {
                tooltip.add(Component.translatable("jade.mycelialmind.node_info",
                        MycelialText.nodeType(node.getNodeType()),
                        node.getBiomassCharge(),
                        MycelialText.strain(node.getSchismStrain())));
                if (node.getNodeType() == SynapticNodeType.SYMBIOTIC) {
                    tooltip.add(Component.translatable("jade.mycelialmind.node_stability",
                            node.getSymbioticStability()));
                    var nodePos = accessor.getPosition();
                    var site = com.alex.mycelialmind.network.ClientColonyCache.getSiteMemories().stream()
                            .filter(s -> s.sitePos().equals(nodePos))
                            .findFirst()
                            .orElse(null);
                    if (site != null) {
                        int callCount = Integer.bitCount(site.escortCall())
                                + Integer.bitCount(site.restCall())
                                + Integer.bitCount(site.pickupCall());
                        if (callCount > 0 || site.lessonStage() >= 0) {
                            tooltip.add(Component.translatable("jade.mycelialmind.node_dialect_calls", callCount));
                        }
                    }
                }
            } else if (accessor.getBlockEntity() instanceof PurificationTurretBlockEntity turret) {
                tooltip.add(Component.translatable("jade.mycelialmind.turret_info",
                        turret.getTier(),
                        (int) Math.round(turret.getRangeRadius())));
                tooltip.add(Component.translatable("jade.mycelialmind.turret_ammo",
                        turret.getAmmo(),
                        com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.TURRET_MAX_AMMO));
                if (turret.isJammed()) {
                    tooltip.add(Component.translatable("jade.mycelialmind.turret_jammed",
                            (int) Math.ceil(turret.getJammedTicks() / 20.0)));
                }
            } else if (accessor.getBlock() instanceof PurificationPylonBlock) {
                MonumentFocusBlockEntity focus = accessor.getBlockEntity() instanceof MonumentFocusBlockEntity mf ? mf : null;
                boolean smothered = AntibodyCystBlock.countAdjacentCysts(accessor.getLevel(), accessor.getPosition())
                        >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
                boolean depleted = focus != null && focus.isDepleted();
                tooltip.add(Component.translatable(smothered
                        ? "jade.mycelialmind.pylon_smothered"
                        : (depleted ? "jade.mycelialmind.pylon_depleted" : "jade.mycelialmind.pylon_active")));
                int tier = focus != null ? focus.getTier() : 1;
                int radius = focus != null
                        ? focus.getEffectiveRadius(MycelialConfig.PYLON_PURIFICATION_RADIUS.get())
                        : MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
                tooltip.add(Component.translatable("jade.mycelialmind.pylon_tier", tier, radius));
                int charge = focus != null
                        ? focus.getPurificationCharge()
                        : com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.PYLON_MAX_CHARGE;
                tooltip.add(Component.translatable("jade.mycelialmind.pylon_charge",
                        charge,
                        com.alex.mycelialmind.hive.AcquisitionDifficultyEngine.PYLON_MAX_CHARGE));
            } else if (accessor.getBlock() instanceof AntibodyCystBlock) {
                boolean active = accessor.getBlockState().hasProperty(AntibodyCystBlock.ACTIVE)
                        && accessor.getBlockState().getValue(AntibodyCystBlock.ACTIVE);
                tooltip.add(Component.translatable(active
                        ? "jade.mycelialmind.cyst_active"
                        : "jade.mycelialmind.cyst_withered"));
                int sens = com.alex.mycelialmind.network.ClientHiveCache.getHomeostasisSensitization();
                if (sens > 0) {
                    tooltip.add(Component.translatable("jade.mycelialmind.sensitization", sens));
                }
            } else if (accessor.getBlockEntity() instanceof SynapticBenchBlockEntity bench) {
                tooltip.add(Component.translatable("jade.mycelialmind.bench_tier", bench.getLabTier()));
                var out = bench.getInventory().getStackInSlot(SynapticBenchBlockEntity.SLOT_OUTPUT);
                if (out.isEmpty()) {
                    var details = bench.computeSynthesisDetails(ClientDiscoveryCache.getEffectiveActTier());
                    if (details.blockedByAct() != null) {
                        tooltip.add(Component.translatable(
                                "gui.mycelialmind.bench.requires_act",
                                details.blockedByAct().actNumber(),
                                MycelialText.progressionTier(details.blockedByAct())));
                    } else {
                        tooltip.add(Component.translatable("jade.mycelialmind.bench_ready"));
                    }
                } else {
                    tooltip.add(Component.translatable("jade.mycelialmind.bench_output", out.getHoverName()));
                }
            } else if (accessor.getBlock() instanceof BioluminescentMycelialCapBlock) {
                boolean mature = accessor.getBlockState().hasProperty(BioluminescentMycelialCapBlock.MATURE)
                        && accessor.getBlockState().getValue(BioluminescentMycelialCapBlock.MATURE);
                tooltip.add(Component.translatable(mature
                        ? "jade.mycelialmind.flora_mature"
                        : "jade.mycelialmind.flora_regrowing"));
            } else if (accessor.getBlock() instanceof MycelialTendrilsBlock) {
                boolean mature = accessor.getBlockState().hasProperty(MycelialTendrilsBlock.MATURE)
                        && accessor.getBlockState().getValue(MycelialTendrilsBlock.MATURE);
                tooltip.add(Component.translatable(mature
                        ? "jade.mycelialmind.flora_mature"
                        : "jade.mycelialmind.flora_regrowing"));
            } else if (accessor.getBlock() instanceof SporeBulbBlock) {
                tooltip.add(Component.translatable("jade.mycelialmind.spore_bulb"));
            } else if (accessor.getBlock() instanceof ReclaimedBloomBlock) {
                tooltip.add(Component.translatable(accessor.getBlockState().getValue(ReclaimedBloomBlock.MATURE)
                        ? "jade.mycelialmind.flora_mature" : "jade.mycelialmind.flora_regrowing"));
                boolean anchored = com.alex.mycelialmind.block.LuminescentAmberVeinBlock.hasAdjacentConduit(
                        accessor.getLevel(), accessor.getPosition());
                var belowState = accessor.getLevel().getBlockState(accessor.getPosition().below());
                boolean fertileSoil = belowState.is(com.alex.mycelialmind.registry.ModBlocks.MYCELIAL_STONE.get())
                        || belowState.is(com.alex.mycelialmind.registry.ModBlocks.CREEPING_MYCELIUM.get());
                tooltip.add(Component.translatable(anchored
                        ? "jade.mycelialmind.bloom_anchored"
                        : (fertileSoil ? "jade.mycelialmind.bloom_soil_ok" : "jade.mycelialmind.bloom_needs_soil")));
            } else if (accessor.getBlock() instanceof HyphalVeinBlock) {
                boolean charged = accessor.getBlockState().hasProperty(HyphalVeinBlock.CHARGED)
                        && accessor.getBlockState().getValue(HyphalVeinBlock.CHARGED);
                int strength = accessor.getBlockState().hasProperty(HyphalVeinBlock.STRENGTH)
                        ? accessor.getBlockState().getValue(HyphalVeinBlock.STRENGTH)
                        : 0;
                tooltip.add(Component.translatable(charged
                        ? "jade.mycelialmind.hyphal_vein_charged"
                        : "jade.mycelialmind.hyphal_vein_dormant"));
                tooltip.add(Component.translatable("jade.mycelialmind.hyphal_vein_strength", strength + 1));
                if (accessor.getBlockEntity() instanceof HyphalVeinBlockEntity veinBe) {
                    tooltip.add(Component.translatable("jade.mycelialmind.hyphal_vein_nutrient",
                            veinBe.getNutrient()));
                    tooltip.add(Component.translatable(veinBe.isRoot()
                            ? "jade.mycelialmind.hyphal_vein_root_yes"
                            : "jade.mycelialmind.hyphal_vein_root_no",
                            veinBe.getDistanceToRoot()));
                }
            } else if (accessor.getBlock() instanceof EphemeralChitinBlock) {
                tooltip.add(Component.translatable("jade.mycelialmind.ephemeral_chitin"));
            } else if (accessor.getBlock() instanceof MycelialRiftBlock) {
                tooltip.add(Component.translatable("jade.mycelialmind.mycelial_rift"));
            } else if (accessor.getBlock() instanceof HiveAltarBlock) {
                tooltip.add(Component.translatable("jade.mycelialmind.hive_altar"));
            } else if (accessor.getBlock() instanceof MatriarchAltarBlock) {
                tooltip.add(Component.translatable("jade.mycelialmind.matriarch_altar"));
            } else if (accessor.getBlock() instanceof SporeVentBlock) {
                tooltip.add(Component.translatable("jade.mycelialmind.spore_vent"));
            } else if (accessor.getBlock() instanceof SporeUpdraftVentBlock) {
                boolean active = accessor.getBlockState().hasProperty(SporeUpdraftVentBlock.ACTIVE)
                        && accessor.getBlockState().getValue(SporeUpdraftVentBlock.ACTIVE);
                tooltip.add(Component.translatable(active
                        ? "jade.mycelialmind.spore_updraft_vent_active"
                        : "jade.mycelialmind.spore_updraft_vent_inactive"));
            } else if (accessor.getBlock() instanceof RootSapMossBlock) {
                boolean ready = accessor.getBlockState().hasProperty(RootSapMossBlock.SAP_READY)
                        && accessor.getBlockState().getValue(RootSapMossBlock.SAP_READY);
                tooltip.add(Component.translatable(ready
                        ? "jade.mycelialmind.root_sap_moss_ready"
                        : "jade.mycelialmind.root_sap_moss_depleted"));
            } else if (accessor.getBlock() instanceof RootCapShelfBlock) {
                tooltip.add(Component.translatable("jade.mycelialmind.root_cap_shelf"));
            } else if (accessor.getBlock() instanceof SovereignNexusBlock) {
                CompoundTag serverData = accessor.getServerData();
                if (serverData.contains("ColonyTier")) {
                    tooltip.add(Component.translatable("jade.mycelialmind.sovereign_nexus_info",
                            serverData.getInt("ColonyTier"),
                            serverData.getInt("Workers"),
                            serverData.getInt("PopCap"),
                            serverData.getInt("Biomass")));
                    tooltip.add(Component.translatable("jade.mycelialmind.sovereign_nexus_morale",
                            serverData.getInt("Morale")));
                } else {
                    tooltip.add(Component.translatable("jade.mycelialmind.sovereign_nexus_dormant"));
                }
            }
            if (accessor.getLevel().dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)
                    && ClientExpeditionCache.hasActiveExpedition()) {
                var pos = accessor.getPosition();
                if (pos.equals(ClientExpeditionCache.getActivePos())) {
                    if (ClientExpeditionCache.getOutcomeId() == 2) {
                        tooltip.add(Component.translatable("jade.mycelialmind.expedition_garden", ClientExpeditionCache.getLinkedFlora(), 3));
                    } else {
                        tooltip.add(Component.translatable("jade.mycelialmind.expedition_route", ClientExpeditionCache.getProgress(), 3));
                    }
                }
                if (pos.equals(ClientExpeditionCache.getDefensePylon())) {
                    tooltip.add(Component.translatable("jade.mycelialmind.expedition_pylon", ClientExpeditionCache.getProgress(), 3));
                    if (ClientExpeditionCache.isPulseInProgress()) {
                        tooltip.add(Component.translatable("jade.mycelialmind.expedition_attackers", ClientExpeditionCache.getAttackersRemaining()));
                    }
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BLOCK_UID;
        }
    }

    public enum NexusServerDataProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }
            SovereignColonyData colony = SovereignColonySavedData.get(serverLevel).getByNexus(accessor.getPosition());
            if (colony == null) {
                return;
            }
            data.putInt("ColonyTier", colony.getTier());
            data.putInt("Workers", colony.getRoster().size());
            data.putInt("PopCap", colony.getPopulationCap());
            data.putInt("Biomass", colony.getBiomassStored());
            data.putInt("Morale", (int) Math.round(colony.getColonyMorale()));
        }

        @Override
        public ResourceLocation getUid() {
            return NEXUS_DATA_UID;
        }
    }
}
