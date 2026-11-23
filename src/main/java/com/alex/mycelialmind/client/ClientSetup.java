package com.alex.mycelialmind.client;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.HyphalVeinBlock;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.client.model.HyphalVeinDynamicModel;
import com.alex.mycelialmind.client.model.AcidSpitterModel;
import com.alex.mycelialmind.client.model.ApexMatriarchModel;
import com.alex.mycelialmind.client.model.BurrowerModel;
import com.alex.mycelialmind.client.model.ChitinBruteModel;
import com.alex.mycelialmind.client.model.ColonyWardenModel;
import com.alex.mycelialmind.client.model.GoliathCarrierModel;
import com.alex.mycelialmind.client.model.HiveCoreModel;
import com.alex.mycelialmind.client.model.LeechParasiteModel;
import com.alex.mycelialmind.client.model.MycelialChimeraModel;
import com.alex.mycelialmind.client.model.MycelialStalkerModel;
import com.alex.mycelialmind.client.model.PyroShroomModel;
import com.alex.mycelialmind.client.model.PurifierDroneModel;
import com.alex.mycelialmind.client.model.SporeArchitectModel;
import com.alex.mycelialmind.client.model.SporeBehemothModel;
import com.alex.mycelialmind.client.model.SporelingModel;
import com.alex.mycelialmind.client.model.SynapseWeaverModel;
import com.alex.mycelialmind.client.model.SynapticMimicModel;
import com.alex.mycelialmind.client.renderer.GlowOrbProjectileRenderer;
import com.alex.mycelialmind.client.renderer.block.MonumentFocusRenderer;
import com.alex.mycelialmind.client.renderer.block.MonumentRenderParts;
import com.alex.mycelialmind.client.renderer.block.PurificationTurretRenderer;
import com.alex.mycelialmind.client.renderer.block.SynapticBenchRenderer;
import com.alex.mycelialmind.client.renderer.block.SynapticNodeRenderer;
import com.alex.mycelialmind.client.screen.ClientCodexOpener;
import com.alex.mycelialmind.client.screen.SynapticBenchScreen;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.network.ClientDiplomacyCache;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientExpeditionCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.network.ClientSymbiosisCache;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModMenuTypes;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import com.alex.mycelialmind.world.ModDimensions;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-side custom 3D model layers, entity/projectile renderers, and emissive glow layers.
 */
@EventBusSubscriber(modid = MycelialMind.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private static final ResourceLocation ACID_SPIT_TEX =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/entity/acid_spit.png");
    private static final ResourceLocation NEUTRALIZING_SPORE_TEX =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/item/latent_spore.png");
    private static final ResourceLocation PURIFIER_GRENADE_TEX =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/item/purifier_grenade.png");
    private static final ModelResourceLocation HYPHAL_VEIN_DIAG_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "block/hyphal_vein_diagonal"));
    private static final ModelResourceLocation HYPHAL_VEIN_WALL_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "block/hyphal_vein_wall"));
    private static final ModelResourceLocation HYPHAL_VEIN_CEILING_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "block/hyphal_vein_ceiling"));
    private static final ResourceLocation HYPHAL_VEIN_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hyphal_vein");

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(HYPHAL_VEIN_DIAG_MODEL);
        event.register(HYPHAL_VEIN_WALL_MODEL);
        event.register(HYPHAL_VEIN_CEILING_MODEL);
    }

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        BakedModel diagModel = models.get(HYPHAL_VEIN_DIAG_MODEL);
        BakedModel wallModel = models.get(HYPHAL_VEIN_WALL_MODEL);
        BakedModel ceilingModel = models.get(HYPHAL_VEIN_CEILING_MODEL);
        for (var entry : models.entrySet()) {
            ModelResourceLocation loc = entry.getKey();
            if (loc.id().equals(HYPHAL_VEIN_ID) && !"inventory".equals(loc.variant())) {
                entry.setValue(new HyphalVeinDynamicModel(entry.getValue(), diagModel, wallModel, ceilingModel));
            }
        }
    }

    private static ResourceLocation entityTex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/entity/" + name + ".png");
    }

    private static ResourceLocation entityEmis(String name) {
        return ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/entity/" + name + "_emissive.png");
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0) {
                        return 0xFFFFFFFF;
                    }
                    int strength = state.hasProperty(HyphalVeinBlock.STRENGTH)
                            ? state.getValue(HyphalVeinBlock.STRENGTH)
                            : 0;
                    return HyphalVeinBlock.colorForStrength(strength);
                },
                ModBlocks.HYPHAL_VEIN.get()
        );
        event.register(
                (state, level, pos, tintIndex) -> tintIndex == 0
                        ? ReclaimedBloomBlock.colorForPalette(state.getValue(ReclaimedBloomBlock.PALETTE))
                        : 0xFFFFFFFF,
                ModBlocks.RECLAIMED_BLOOM.get()
        );
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFFFF;
            }
            if (stack.is(ModItems.RECLAIMED_BLOOM_MELEE.get())) {
                return ReclaimedBloomBlock.colorForPalette(1);
            }
            if (stack.is(ModItems.RECLAIMED_BLOOM_PROJECTILE.get())) {
                return ReclaimedBloomBlock.colorForPalette(2);
            }
            if (stack.is(ModItems.RECLAIMED_BLOOM_FIRE.get())) {
                return ReclaimedBloomBlock.colorForPalette(3);
            }
            if (stack.is(ModItems.RECLAIMED_BLOOM_EXPLOSION.get())) {
                return ReclaimedBloomBlock.colorForPalette(4);
            }
            if (stack.is(ModItems.RECLAIMED_BLOOM_MAGIC.get())) {
                return ReclaimedBloomBlock.colorForPalette(5);
            }
            return ReclaimedBloomBlock.colorForPalette(0);
        }, ModBlocks.RECLAIMED_BLOOM.get(),
                ModItems.RECLAIMED_BLOOM_MELEE.get(),
                ModItems.RECLAIMED_BLOOM_PROJECTILE.get(),
                ModItems.RECLAIMED_BLOOM_FIRE.get(),
                ModItems.RECLAIMED_BLOOM_EXPLOSION.get(),
                ModItems.RECLAIMED_BLOOM_MAGIC.get());
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SYNAPTIC_BENCH.get(), SynapticBenchScreen::new);
        if (Boolean.getBoolean("mycelialmind.visualSmoke")) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(VisualSmokeClient.class);
        }
        if (Boolean.getBoolean("mycelialmind.structureVisualSmoke")) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(StructureVisualSmokeClient.class);
        }
        if (Boolean.getBoolean("mycelialmind.hiveLifecycleSmoke")) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(HiveLifecycleSmokeClient.class);
        }
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SporelingModel.LAYER_LOCATION, SporelingModel::createBodyLayer);
        event.registerLayerDefinition(LeechParasiteModel.LAYER_LOCATION, LeechParasiteModel::createBodyLayer);
        event.registerLayerDefinition(ChitinBruteModel.LAYER_LOCATION, ChitinBruteModel::createBodyLayer);
        event.registerLayerDefinition(MycelialStalkerModel.LAYER_LOCATION, MycelialStalkerModel::createBodyLayer);
        event.registerLayerDefinition(AcidSpitterModel.LAYER_LOCATION, AcidSpitterModel::createBodyLayer);
        event.registerLayerDefinition(BurrowerModel.LAYER_LOCATION, BurrowerModel::createBodyLayer);
        event.registerLayerDefinition(PyroShroomModel.LAYER_LOCATION, PyroShroomModel::createBodyLayer);
        event.registerLayerDefinition(SynapseWeaverModel.LAYER_LOCATION, SynapseWeaverModel::createBodyLayer);
        event.registerLayerDefinition(GoliathCarrierModel.LAYER_LOCATION, GoliathCarrierModel::createBodyLayer);
        event.registerLayerDefinition(ColonyWardenModel.LAYER_LOCATION, ColonyWardenModel::createBodyLayer);
        event.registerLayerDefinition(SporeArchitectModel.LAYER_LOCATION, SporeArchitectModel::createBodyLayer);
        event.registerLayerDefinition(SynapticMimicModel.LAYER_LOCATION, SynapticMimicModel::createBodyLayer);
        event.registerLayerDefinition(SporeBehemothModel.LAYER_LOCATION, SporeBehemothModel::createBodyLayer);
        event.registerLayerDefinition(PurifierDroneModel.LAYER_LOCATION, PurifierDroneModel::createBodyLayer);
        event.registerLayerDefinition(MycelialChimeraModel.LAYER_LOCATION, MycelialChimeraModel::createBodyLayer);
        event.registerLayerDefinition(HiveCoreModel.LAYER_LOCATION, HiveCoreModel::createBodyLayer);
        event.registerLayerDefinition(ApexMatriarchModel.LAYER_LOCATION, ApexMatriarchModel::createBodyLayer);
        event.registerLayerDefinition(MonumentRenderParts.CRYSTAL_LAYER, MonumentRenderParts::createCrystalLayer);
        event.registerLayerDefinition(MonumentRenderParts.TURRET_HEAD_LAYER, MonumentRenderParts::createTurretHeadLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPORELING.get(),
                ctx -> buildMobRenderer(ctx, new SporelingModel<>(ctx.bakeLayer(SporelingModel.LAYER_LOCATION)), 0.35F, "sporeling"));
        event.registerEntityRenderer(ModEntities.LEECH_PARASITE.get(),
                ctx -> buildMobRenderer(ctx, new LeechParasiteModel<>(ctx.bakeLayer(LeechParasiteModel.LAYER_LOCATION)), 0.28F, "leech_parasite"));
        event.registerEntityRenderer(ModEntities.CHITIN_BRUTE.get(),
                ctx -> buildMobRenderer(ctx, new ChitinBruteModel<>(ctx.bakeLayer(ChitinBruteModel.LAYER_LOCATION)), 0.65F, "chitin_brute"));
        event.registerEntityRenderer(ModEntities.MYCELIAL_STALKER.get(),
                ctx -> buildMobRenderer(ctx, new MycelialStalkerModel<>(ctx.bakeLayer(MycelialStalkerModel.LAYER_LOCATION)), 0.50F, "mycelial_stalker"));
        event.registerEntityRenderer(ModEntities.ACID_SPITTER.get(),
                ctx -> buildMobRenderer(ctx, new AcidSpitterModel<>(ctx.bakeLayer(AcidSpitterModel.LAYER_LOCATION)), 0.60F, "acid_spitter"));
        event.registerEntityRenderer(ModEntities.BURROWER.get(),
                ctx -> buildMobRenderer(ctx, new BurrowerModel<>(ctx.bakeLayer(BurrowerModel.LAYER_LOCATION)), 0.70F, "burrower"));
        event.registerEntityRenderer(ModEntities.PYRO_SHROOM.get(),
                ctx -> buildMobRenderer(ctx, new PyroShroomModel<>(ctx.bakeLayer(PyroShroomModel.LAYER_LOCATION)), 0.55F, "pyro_shroom"));
        event.registerEntityRenderer(ModEntities.SYNAPSE_WEAVER.get(),
                ctx -> buildMobRenderer(ctx, new SynapseWeaverModel<>(ctx.bakeLayer(SynapseWeaverModel.LAYER_LOCATION)), 0.50F, "synapse_weaver"));
        event.registerEntityRenderer(ModEntities.GOLIATH_CARRIER.get(),
                ctx -> buildMobRenderer(ctx, new GoliathCarrierModel<>(ctx.bakeLayer(GoliathCarrierModel.LAYER_LOCATION)), 1.05F, "goliath_carrier", true, 1.10F));
        event.registerEntityRenderer(ModEntities.COLONY_WARDEN.get(),
                ctx -> buildMobRenderer(ctx, new ColonyWardenModel<>(ctx.bakeLayer(ColonyWardenModel.LAYER_LOCATION)), 1.05F, "colony_warden", true, 1.10F));
        event.registerEntityRenderer(ModEntities.SPORE_ARCHITECT.get(),
                ctx -> buildMobRenderer(ctx, new SporeArchitectModel<>(ctx.bakeLayer(SporeArchitectModel.LAYER_LOCATION)), 0.55F, "spore_architect"));
        event.registerEntityRenderer(ModEntities.SYNAPTIC_MIMIC.get(),
                ctx -> buildMobRenderer(ctx, new SynapticMimicModel<>(ctx.bakeLayer(SynapticMimicModel.LAYER_LOCATION)), 0.55F, "synaptic_mimic"));
        event.registerEntityRenderer(ModEntities.SPORE_BEHEMOTH.get(),
                ctx -> buildMobRenderer(ctx, new SporeBehemothModel<>(ctx.bakeLayer(SporeBehemothModel.LAYER_LOCATION)), 1.45F, "spore_behemoth", true, 1.45F));
        event.registerEntityRenderer(ModEntities.PURIFIER_DRONE.get(),
                ctx -> buildMobRenderer(ctx, new PurifierDroneModel<>(ctx.bakeLayer(PurifierDroneModel.LAYER_LOCATION)), 0.40F, "purifier_drone", false, 1.0F));
        event.registerEntityRenderer(ModEntities.MYCELIAL_CHIMERA.get(),
                ctx -> buildMobRenderer(ctx, new MycelialChimeraModel<>(ctx.bakeLayer(MycelialChimeraModel.LAYER_LOCATION)), 0.75F, "mycelial_chimera", true, 1.0F));
        event.registerEntityRenderer(ModEntities.HIVE_CORE.get(),
                ctx -> buildMobRenderer(ctx, new HiveCoreModel<>(ctx.bakeLayer(HiveCoreModel.LAYER_LOCATION)), 1.85F, "hive_core", false, 1.70F));
        event.registerEntityRenderer(ModEntities.APEX_MATRIARCH.get(),
                ctx -> buildMobRenderer(ctx, new ApexMatriarchModel<>(ctx.bakeLayer(ApexMatriarchModel.LAYER_LOCATION)), 2.35F, "apex_matriarch", false, 1.90F));

        event.registerEntityRenderer(ModEntities.ACID_SPIT.get(),
                ctx -> new GlowOrbProjectileRenderer<>(ctx, ACID_SPIT_TEX, 0.45F));
        event.registerEntityRenderer(ModEntities.NEUTRALIZING_SPORE.get(),
                ctx -> new GlowOrbProjectileRenderer<>(ctx, NEUTRALIZING_SPORE_TEX, 0.45F));
        event.registerEntityRenderer(ModEntities.PURIFIER_GRENADE.get(),
                ctx -> new GlowOrbProjectileRenderer<>(ctx, PURIFIER_GRENADE_TEX, 0.55F));

        event.registerBlockEntityRenderer(ModBlockEntities.SYNAPTIC_NODE.get(), SynapticNodeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PURIFICATION_TURRET.get(), PurificationTurretRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SYNAPTIC_BENCH.get(), SynapticBenchRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MONUMENT_FOCUS.get(), MonumentFocusRenderer::new);
    }

    private static <T extends Mob, M extends EntityModel<T>> MobRenderer<T, M> buildMobRenderer(
            EntityRendererProvider.Context ctx, M model, float shadow, String id) {
        return buildMobRenderer(ctx, model, shadow, id, true, 1.0F);
    }

    private static <T extends Mob, M extends EntityModel<T>> MobRenderer<T, M> buildMobRenderer(
            EntityRendererProvider.Context ctx, M model, float shadow, String id, boolean rankVariants) {
        return buildMobRenderer(ctx, model, shadow, id, rankVariants, 1.0F);
    }

    private static <T extends Mob, M extends EntityModel<T>> MobRenderer<T, M> buildMobRenderer(
            EntityRendererProvider.Context ctx, M model, float shadow, String id, boolean rankVariants, float renderScale) {
        ResourceLocation baseTex = entityTex(id);
        ResourceLocation mutatedTex = rankVariants ? entityTex(id + "_mutated") : baseTex;
        ResourceLocation eliteTex = rankVariants ? entityTex(id + "_elite") : baseTex;

        ResourceLocation baseEmis = entityEmis(id);
        ResourceLocation mutatedEmis = rankVariants ? entityEmis(id + "_mutated") : baseEmis;
        ResourceLocation eliteEmis = rankVariants ? entityEmis(id + "_elite") : baseEmis;

        MobRenderer<T, M> renderer = new MobRenderer<>(ctx, model, shadow) {
            @Override
            public ResourceLocation getTextureLocation(T entity) {
                if (rankVariants) {
                    EvolutionRank rank = null;
                    if (entity instanceof MycelialMob mycelial) {
                        rank = mycelial.getEvolutionRank();
                    } else if (entity instanceof MycelialChimeraEntity chimera) {
                        rank = chimera.getEvolutionRank();
                    }
                    if (rank == EvolutionRank.MUTATED) {
                        return mutatedTex;
                    }
                    if (rank == EvolutionRank.ELITE) {
                        return eliteTex;
                    }
                }
                return baseTex;
            }

            @Override
            protected void scale(T livingEntity, PoseStack poseStack, float partialTickTime) {
                super.scale(livingEntity, poseStack, partialTickTime);
                if (renderScale != 1.0F) {
                    poseStack.scale(renderScale, renderScale, renderScale);
                }
            }
        };
        if (!Boolean.getBoolean("mycelialmind.animationVisualSmoke")
                || !Boolean.getBoolean("mycelialmind.animationHideEmissive")) {
            renderer.addLayer(new EmissiveOverlayLayer<>(renderer, baseEmis, mutatedEmis, eliteEmis));
        }
        return renderer;
    }

    private static final class EmissiveOverlayLayer<T extends Mob, M extends EntityModel<T>> extends RenderLayer<T, M> {
        private final RenderType baseRenderType;
        private final RenderType mutatedRenderType;
        private final RenderType eliteRenderType;

        private EmissiveOverlayLayer(
                RenderLayerParent<T, M> parent,
                ResourceLocation baseEmis,
                ResourceLocation mutatedEmis,
                ResourceLocation eliteEmis
        ) {
            super(parent);
            this.baseRenderType = RenderType.eyes(baseEmis);
            this.mutatedRenderType = RenderType.eyes(mutatedEmis);
            this.eliteRenderType = RenderType.eyes(eliteEmis);
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                int packedLight,
                T entity,
                float limbSwing,
                float limbSwingAmount,
                float partialTick,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
        ) {
            RenderType activeType = this.baseRenderType;
            float flashProgress = 0.0F;
            EvolutionRank rank = EvolutionRank.BASE;
            if (entity instanceof MycelialMob mycelial) {
                rank = mycelial.getEvolutionRank();
                if (rank == EvolutionRank.MUTATED) {
                    activeType = this.mutatedRenderType;
                } else if (rank == EvolutionRank.ELITE) {
                    activeType = this.eliteRenderType;
                }
                flashProgress = mycelial.getPromotionFlashProgress(partialTick);
            } else if (entity instanceof MycelialChimeraEntity chimera) {
                rank = chimera.getEvolutionRank();
                if (rank == EvolutionRank.MUTATED) {
                    activeType = this.mutatedRenderType;
                } else if (rank == EvolutionRank.ELITE) {
                    activeType = this.eliteRenderType;
                }
                flashProgress = chimera.getPromotionFlashProgress(partialTick);
            }

            VertexConsumer vertexConsumer = bufferSource.getBuffer(activeType);
            this.getParentModel().renderToBuffer(
                    poseStack,
                    vertexConsumer,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    -1
            );

            if (flashProgress > 0.0F) {
                int targetRgb = rank == EvolutionRank.ELITE ? 0xFF4D8D : 0xFFC857;
                float tr = ((targetRgb >> 16) & 0xFF) / 255.0F;
                float tg = ((targetRgb >> 8) & 0xFF) / 255.0F;
                float tb = (targetRgb & 0xFF) / 255.0F;
                float mixToRank = 1.0F - flashProgress;
                float intensity = flashProgress;
                int r = Math.min(255, Math.max(0, Math.round((1.0F * flashProgress + tr * mixToRank) * intensity * 255.0F)));
                int g = Math.min(255, Math.max(0, Math.round((1.0F * flashProgress + tg * mixToRank) * intensity * 255.0F)));
                int b = Math.min(255, Math.max(0, Math.round((1.0F * flashProgress + tb * mixToRank) * intensity * 255.0F)));
                int a = Math.min(255, Math.max(0, Math.round(intensity * 255.0F)));
                int flashColor = (a << 24) | (r << 16) | (g << 8) | b;

                VertexConsumer flashConsumer = bufferSource.getBuffer(activeType);
                this.getParentModel().renderToBuffer(
                        poseStack,
                        flashConsumer,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        flashColor
                );
            }
        }
    }

    @EventBusSubscriber(modid = MycelialMind.MODID, value = Dist.CLIENT)
    public static final class ClientAtmosphereHandler {
        private static final List<BlockPos> CACHED_THERMAL_CYSTS = new ArrayList<>();
        private static final List<BlockPos> CACHED_THERMAL_TURRETS = new ArrayList<>();
        private static final List<BlockPos> CACHED_THERMAL_VAULTS = new ArrayList<>();
        private static final List<BlockPos> CACHED_THERMAL_VEINS = new ArrayList<>();
        private static final List<BlockPos> CACHED_THERMAL_BLIND_SPOTS = new ArrayList<>();
        private static final List<BlockPos> CACHED_GARDEN_FLORA = new ArrayList<>();
        private static int gardenScanTick;
        private static int cystScanTick = 0;
        private static boolean clientSessionActive = false;

        private ClientAtmosphereHandler() {
        }

        @SubscribeEvent
        public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null
                    && mc.level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
                HomeostasisStage stage = ClientHiveCache.getHomeostasisStage();
                if (stage != HomeostasisStage.STABLE) {
                    if (mc.player.hasEffect(ModEffects.SYNAPTIC_VISION)) {
                        event.setRed(event.getRed() * 0.55F + 0.08F * 0.45F);
                        event.setGreen(event.getGreen() * 0.55F + 0.24F * 0.45F);
                        event.setBlue(event.getBlue() * 0.55F + 0.30F * 0.45F);
                        return;
                    }
                    if (stage == HomeostasisStage.IMMUNE_RESPONSE) {
                        event.setRed(event.getRed() * 0.18F + 0.22F * 0.82F);
                        event.setGreen(event.getGreen() * 0.18F + 0.30F * 0.82F);
                        event.setBlue(event.getBlue() * 0.18F + 0.14F * 0.82F);
                        return;
                    }
                    event.setRed(event.getRed() * 0.55F + 0.26F * 0.45F);
                    event.setGreen(event.getGreen() * 0.55F + 0.20F * 0.45F);
                    event.setBlue(event.getBlue() * 0.55F + 0.32F * 0.45F);
                    return;
                }
            }
            if (ClientHiveCache.getFungalTideSecondsRemaining() > 0 && !ClientHiveCache.isInProtectedZone()) {
                event.setRed(event.getRed() * 0.35F + 0.26F * 0.65F);
                event.setGreen(event.getGreen() * 0.35F + 0.08F * 0.65F);
                event.setBlue(event.getBlue() * 0.35F + 0.40F * 0.65F);
            }
        }

        @SubscribeEvent
        public static void onRenderFog(ViewportEvent.RenderFog event) {
            Minecraft mc = Minecraft.getInstance();
            boolean irisActive = com.alex.mycelialmind.compat.iris.MycelialIrisCompat.isShaderPackInUse();
            double fogScale = MycelialClientConfig.sporeFogDensityScale();
            if (mc.level != null && mc.player != null
                    && mc.level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
                HomeostasisStage stage = ClientHiveCache.getHomeostasisStage();
                if (stage != HomeostasisStage.STABLE) {
                    boolean thermal = mc.player.hasEffect(ModEffects.SYNAPTIC_VISION);
                    var range = DimensionalHomeostasisEngine.computeSporeFogRange(
                            stage, ClientHiveCache.getHomeostasisStress(), thermal, irisActive
                    );
                    if (range != null) {
                        float scaledFar = MycelialClientConfig.computeScaledFogFarPlane(
                                range.farPlane(), range.nearPlane(), fogScale);
                        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), range.nearPlane()));
                        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), scaledFar));
                        event.setCanceled(true);
                        return;
                    }
                }
            }
            if (ClientHiveCache.getFungalTideSecondsRemaining() > 0 && !ClientHiveCache.isInProtectedZone()) {
                float near = irisActive ? 12.0F : 8.0F;
                float far = MycelialClientConfig.computeScaledFogFarPlane(irisActive ? 96.0F : 72.0F, near, fogScale);
                event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), near));
                event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), far));
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                if (clientSessionActive) {
                    ClientHiveCache.clear();
                    ClientSymbiosisCache.clear();
                    ClientDiplomacyCache.clear();
                    ClientDiscoveryCache.clear();
                    ClientExpeditionCache.clear();
                    ClientSoundscapeDirector.resetSession();
                    CACHED_THERMAL_CYSTS.clear();
                    CACHED_THERMAL_TURRETS.clear();
                    CACHED_THERMAL_VAULTS.clear();
                    CACHED_THERMAL_VEINS.clear();
                    CACHED_THERMAL_BLIND_SPOTS.clear();
                    cystScanTick = 0;
                    clientSessionActive = false;
                }
                return;
            }
            clientSessionActive = true;
            ClientSoundscapeDirector.onClientTick(mc);
            if (mc.screen == null) {
                while (MycelialKeyBindings.OPEN_CODEX.consumeClick()) {
                    boolean hasScanner = mc.player.isCreative()
                            || mc.player.getMainHandItem().is(ModItems.BIO_SCANNER.get())
                            || mc.player.getOffhandItem().is(ModItems.BIO_SCANNER.get())
                            || containsScannerInInventory(mc.player);
                    if (hasScanner) {
                        ClientCodexOpener.openBioCodex();
                    } else {
                        mc.player.displayClientMessage(
                                Component.translatable("message.mycelialmind.codex_requires_scanner")
                                        .withStyle(ChatFormatting.YELLOW), true);
                    }
                }
                while (MycelialKeyBindings.TOGGLE_HUD_COMPACT.consumeClick()) {
                    boolean next = !MycelialClientConfig.isHudCompact();
                    if (MycelialClientConfig.SPEC.isLoaded()) {
                        MycelialClientConfig.HUD_COMPACT_MODE.set(next);
                        MycelialClientConfig.SPEC.save();
                    }
                    mc.player.displayClientMessage(
                            Component.translatable(next
                                    ? "message.mycelialmind.hud_compact_on"
                                    : "message.mycelialmind.hud_compact_off")
                                    .withStyle(ChatFormatting.AQUA), true);
                }
            }
            if (mc.isPaused()) {
                return;
            }
            if (mc.level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)) {
                HomeostasisStage stage = ClientHiveCache.getHomeostasisStage();
                if (stage != HomeostasisStage.STABLE) {
                    int count = stage == HomeostasisStage.IMMUNE_RESPONSE ? 6 : 2;
                    var rng = mc.level.random;
                    for (int i = 0; i < count; i++) {
                        double px = mc.player.getX() + (rng.nextDouble() - 0.5D) * 16.0D;
                        double py = mc.player.getY() + (rng.nextDouble() - 0.3D) * 7.0D;
                        double pz = mc.player.getZ() + (rng.nextDouble() - 0.5D) * 16.0D;
                        var particle = stage == HomeostasisStage.IMMUNE_RESPONSE
                                ? (i % 2 == 0 ? ModParticles.SPORE_FALL.get() : ModParticles.SPORE_FALL.get())
                                : ModParticles.NECROTIC_MIST.get();
                        mc.level.addParticle(particle, px, py, pz, 0.0D, -0.02D, 0.0D);
                    }
                }
            }

            if (mc.player.hasEffect(ModEffects.SYNAPTIC_VISION)) {
                if (++gardenScanTick >= 40) {
                    gardenScanTick = 0;
                    CACHED_GARDEN_FLORA.clear();
                    BlockPos garden = ClientExpeditionCache.getActivePos();
                    if (garden != null && ClientExpeditionCache.getOutcomeId() == 2
                            && mc.level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL)
                            && mc.level.hasChunkAt(garden)
                            && mc.level.getBlockState(garden).is(ModBlocks.SYMBIOTIC_NODE.get())
                            && garden.distSqr(mc.player.blockPosition()) <= 48 * 48) {
                        for (BlockPos p : BlockPos.betweenClosed(garden.offset(-24, -24, -24), garden.offset(24, 24, 24))) {
                            if (p.distSqr(garden) > 24 * 24 || !mc.level.hasChunkAt(p)) continue;
                            var state = mc.level.getBlockState(p);
                            if ((state.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                                    || state.is(ModBlocks.SPORE_BULB.get())
                                    || state.is(ModBlocks.MYCELIAL_TENDRILS.get()))
                                    && ModBlocks.RECLAIMED_BLOOM.get().defaultBlockState().canSurvive(mc.level, p)) {
                                CACHED_GARDEN_FLORA.add(p.immutable());
                            }
                        }
                    }
                }
                if (++cystScanTick >= 10) {
                    cystScanTick = 0;
                    CACHED_THERMAL_CYSTS.clear();
                    CACHED_THERMAL_TURRETS.clear();
                    CACHED_THERMAL_VAULTS.clear();
                    CACHED_THERMAL_VEINS.clear();
                    CACHED_THERMAL_BLIND_SPOTS.clear();
                    BlockPos origin = mc.player.blockPosition();
                    var cystBlock = ModBlocks.ANTIBODY_CYST.get();
                    var turretBlock = ModBlocks.PURIFICATION_TURRET.get();
                    var veinBlock = ModBlocks.HYPHAL_VEIN.get();
                    var bulbBlock = ModBlocks.SPORE_BULB.get();
                    for (BlockPos p : BlockPos.betweenClosed(origin.offset(-14, -6, -14), origin.offset(14, 6, 14))) {
                        var st = mc.level.getBlockState(p);
                        if (st.is(cystBlock) && CACHED_THERMAL_CYSTS.size() < 48) {
                            CACHED_THERMAL_CYSTS.add(p.immutable());
                        } else if (st.is(turretBlock) && CACHED_THERMAL_TURRETS.size() < 24) {
                            CACHED_THERMAL_TURRETS.add(p.immutable());
                        } else if (st.is(net.minecraft.world.level.block.Blocks.CHEST)
                                && CACHED_THERMAL_VAULTS.size() < 8
                                && com.alex.mycelialmind.item.BioScannerItem.isSealedVaultChest(mc.level, p)) {
                            CACHED_THERMAL_VAULTS.add(p.immutable());
                        } else if (st.is(veinBlock)
                                && CACHED_THERMAL_VEINS.size() < 32
                                && (st.getValue(com.alex.mycelialmind.block.HyphalVeinBlock.CHARGED)
                                || st.getValue(com.alex.mycelialmind.block.HyphalVeinBlock.STRENGTH) >= 2)) {
                            CACHED_THERMAL_VEINS.add(p.immutable());
                        } else if (st.is(bulbBlock)
                                && CACHED_THERMAL_BLIND_SPOTS.size() < 8
                                && isBlindSpotThermalCore(mc.level, p)) {
                            CACHED_THERMAL_BLIND_SPOTS.add(p.immutable());
                        }
                    }
                }
            } else if (!CACHED_THERMAL_CYSTS.isEmpty()
                    || !CACHED_THERMAL_TURRETS.isEmpty()
                    || !CACHED_THERMAL_VAULTS.isEmpty()
                    || !CACHED_THERMAL_VEINS.isEmpty()
                    || !CACHED_THERMAL_BLIND_SPOTS.isEmpty()
                    || !CACHED_GARDEN_FLORA.isEmpty()) {
                CACHED_THERMAL_CYSTS.clear();
                CACHED_THERMAL_TURRETS.clear();
                CACHED_THERMAL_VAULTS.clear();
                CACHED_THERMAL_VEINS.clear();
                CACHED_THERMAL_BLIND_SPOTS.clear();
                CACHED_GARDEN_FLORA.clear();
                cystScanTick = 0;
            }
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || !mc.player.hasEffect(ModEffects.SYNAPTIC_VISION)) {
                return;
            }

            PoseStack poseStack = event.getPoseStack();
            Vec3 cam = event.getCamera().getPosition();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            float pulse = 0.78F + 0.22F * (float) Math.sin(mc.level.getGameTime() * 0.25D);

            poseStack.pushPose();
            poseStack.translate(-cam.x, -cam.y, -cam.z);

            AABB scanBox = mc.player.getBoundingBox().inflate(32.0D);
            for (Entity entity : mc.level.getEntities(mc.player, scanBox, e -> e.isAlive())) {
                if (entity instanceof MycelialMob mob) {
                    if (mob instanceof com.alex.mycelialmind.entity.SynapticMimicEntity mimic) {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, entity.getBoundingBox().inflate(0.11D),
                                0.95F, 0.18F * pulse, 1.0F, 0.98F);
                        if (!mimic.isUnmasked()) {
                            LevelRenderer.renderLineBox(poseStack, lineConsumer, entity.getBoundingBox().inflate(0.15D),
                                    1.0F, 0.55F, 0.95F, 0.90F);
                        }
                    } else if (mob instanceof com.alex.mycelialmind.entity.MycelialBossMob bossMob
                            && bossMob.getCombatState() == com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState.WINDUP) {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, entity.getBoundingBox().inflate(0.14D),
                                1.0F, 0.36F * pulse, 0.08F, 0.98F);
                    } else if (mob.isPrimordialVariant()) {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, entity.getBoundingBox().inflate(0.12D),
                                0.72F, 1.0F, 0.22F * pulse, 0.98F);
                    } else if (mob.getCommandRole() == SynapticCommandSystem.CommandRole.LEADER && !mob.isDesynced()) {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, entity.getBoundingBox().inflate(0.10D),
                                0.96F, 0.76F * pulse, 0.24F, 0.96F);
                    } else {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, entity.getBoundingBox().inflate(0.06D),
                                1.0F, 0.28F * pulse, 0.16F, 0.92F);
                    }
                    if (mob.isMultipartEntity() && mob.getParts() != null) {
                        boolean bossCarapaceBroken = mob instanceof com.alex.mycelialmind.entity.MycelialBossMob bossMob && bossMob.isCarapaceBroken();
                        for (net.neoforged.neoforge.entity.PartEntity<?> part : mob.getParts()) {
                            if (part instanceof com.alex.mycelialmind.entity.MycelialBossPart bossPart) {
                                if (bossPart.getRole() == com.alex.mycelialmind.entity.MycelialBossPart.PartRole.WEAK_POINT) {
                                    if (bossCarapaceBroken) {
                                        LevelRenderer.renderLineBox(poseStack, lineConsumer, bossPart.getBoundingBox().inflate(0.10D),
                                                1.0F, 0.85F * pulse, 0.16F, 1.0F);
                                    } else {
                                        LevelRenderer.renderLineBox(poseStack, lineConsumer, bossPart.getBoundingBox().inflate(0.04D),
                                                1.0F, 0.20F, 0.90F * pulse, 0.96F);
                                    }
                                } else {
                                    if (bossCarapaceBroken) {
                                        LevelRenderer.renderLineBox(poseStack, lineConsumer, bossPart.getBoundingBox().inflate(0.01D),
                                                0.55F, 0.35F, 0.25F, 0.35F);
                                    } else {
                                        LevelRenderer.renderLineBox(poseStack, lineConsumer, bossPart.getBoundingBox().inflate(0.02D),
                                                0.75F, 0.55F, 0.25F, 0.85F);
                                    }
                                }
                            }
                        }
                    }
                } else if (entity instanceof Monster) {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, entity.getBoundingBox().inflate(0.06D),
                            1.0F, 0.28F * pulse, 0.16F, 0.92F);
                } else if (entity instanceof PurifierDroneEntity
                        || entity instanceof MycelialChimeraEntity
                        || entity instanceof Player) {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, entity.getBoundingBox().inflate(0.05D),
                            0.20F, 0.95F, 0.72F, 0.85F);
                }
            }

            BlockPos playerPos = mc.player.blockPosition();
            for (var marker : ClientHiveCache.getNearbyNodes()) {
                BlockPos np = marker.pos();
                if (np.distSqr(playerPos) <= 36.0D * 36.0D) {
                    if (marker.type() == SynapticNodeType.SYMBIOTIC) {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(np).inflate(0.03D),
                                0.25F, 1.0F, 0.45F, 0.85F);
                    } else {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(np).inflate(0.03D),
                                0.85F, 0.25F, 1.0F, 0.82F);
                    }
                }
            }

            for (BlockPos pylonPos : ClientHiveCache.getNearbyPylons()) {
                if (pylonPos.distSqr(playerPos) <= 36.0D * 36.0D) {
                    boolean smothered = AntibodyCystBlock.countAdjacentCysts(mc.level, pylonPos)
                            >= DimensionalHomeostasisEngine.SMOTHER_FACE_THRESHOLD;
                    if (smothered) {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(pylonPos).inflate(0.04D),
                                1.0F, 0.25F, 0.20F, 0.90F);
                    } else {
                        LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(pylonPos).inflate(0.04D),
                                0.20F, 0.95F, 1.0F, 0.88F);
                    }
                }
            }

            for (BlockPos cystPos : CACHED_THERMAL_CYSTS) {
                LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(cystPos).inflate(0.01D),
                        1.0F, 0.76F, 0.15F, 0.75F);
            }

            for (BlockPos turretPos : CACHED_THERMAL_TURRETS) {
                boolean jammed = mc.level.getBlockEntity(turretPos) instanceof PurificationTurretBlockEntity tbe
                        && tbe.isJammed();
                if (jammed) {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(turretPos).inflate(0.04D),
                            1.0F, 0.45F * pulse, 0.10F, 0.92F);
                } else {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(turretPos).inflate(0.04D),
                            0.20F, 0.95F, 1.0F, 0.85F);
                }
            }

            for (BlockPos vaultPos : CACHED_THERMAL_VAULTS) {
                LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(vaultPos).inflate(0.05D),
                        1.0F, 0.84F * pulse, 0.18F, 0.95F);
            }

            for (BlockPos veinPos : CACHED_THERMAL_VEINS) {
                LevelRenderer.renderLineBox(poseStack, lineConsumer,
                        new AABB(veinPos.getX(), veinPos.getY(), veinPos.getZ(),
                                veinPos.getX() + 1.0D, veinPos.getY() + 0.12D, veinPos.getZ() + 1.0D),
                        0.68F, 0.22F * pulse, 1.0F, 0.85F);
            }

            for (BlockPos blindPos : CACHED_THERMAL_BLIND_SPOTS) {
                LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(blindPos).inflate(0.06D),
                        1.0F, 0.18F, 0.72F * pulse, 0.96F);
            }

            for (BlockPos flora : CACHED_GARDEN_FLORA) {
                LevelRenderer.renderLineBox(poseStack, lineConsumer, new AABB(flora).inflate(0.05D),
                        0.35F, 1.0F, 0.35F, 0.95F);
            }

            poseStack.popPose();

            float prevFogStart = RenderSystem.getShaderFogStart();
            float prevFogEnd = RenderSystem.getShaderFogEnd();
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            try {
                bufferSource.endBatch(RenderType.lines());
            } finally {
                RenderSystem.setShaderFogStart(prevFogStart);
                RenderSystem.setShaderFogEnd(prevFogEnd);
            }
        }

        private static boolean isBlindSpotThermalCore(net.minecraft.world.level.Level level, BlockPos pos) {
            var veinBlock = ModBlocks.HYPHAL_VEIN.get();
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(pos.relative(dir)).is(veinBlock)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean containsScannerInInventory(Player player) {
            for (ItemStack invStack : player.getInventory().items) {
                if (invStack.is(ModItems.BIO_SCANNER.get())) {
                    return true;
                }
            }
            return false;
        }
    }
}
