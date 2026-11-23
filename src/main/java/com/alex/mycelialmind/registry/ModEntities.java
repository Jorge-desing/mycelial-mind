package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.AcidSpitterEntity;
import com.alex.mycelialmind.entity.ApexMatriarchEntity;
import com.alex.mycelialmind.entity.BurrowerEntity;
import com.alex.mycelialmind.entity.ChitinBruteEntity;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.GoliathCarrierEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.LeechParasiteEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.MycelialStalkerEntity;
import com.alex.mycelialmind.entity.PyroShroomEntity;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.SporeArchitectEntity;
import com.alex.mycelialmind.entity.SporeBehemothEntity;
import com.alex.mycelialmind.entity.SporelingEntity;
import com.alex.mycelialmind.entity.SynapseWeaverEntity;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.entity.projectile.PurifierGrenadeProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all Mycelial Mind entity types, projectiles, and base attributes.
 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MycelialMind.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SporelingEntity>> SPORELING =
            ENTITY_TYPES.register("sporeling", () -> EntityType.Builder.of(SporelingEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.5F).build("sporeling"));

    public static final DeferredHolder<EntityType<?>, EntityType<ChitinBruteEntity>> CHITIN_BRUTE =
            ENTITY_TYPES.register("chitin_brute", () -> EntityType.Builder.of(ChitinBruteEntity::new, MobCategory.MONSTER)
                    .sized(0.95F, 2.1F).build("chitin_brute"));

    public static final DeferredHolder<EntityType<?>, EntityType<MycelialStalkerEntity>> MYCELIAL_STALKER =
            ENTITY_TYPES.register("mycelial_stalker", () -> EntityType.Builder.of(MycelialStalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.8F).build("mycelial_stalker"));

    public static final DeferredHolder<EntityType<?>, EntityType<AcidSpitterEntity>> ACID_SPITTER =
            ENTITY_TYPES.register("acid_spitter", () -> EntityType.Builder.of(AcidSpitterEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.6F).build("acid_spitter"));

    public static final DeferredHolder<EntityType<?>, EntityType<BurrowerEntity>> BURROWER =
            ENTITY_TYPES.register("burrower", () -> EntityType.Builder.of(BurrowerEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.4F).build("burrower"));

    public static final DeferredHolder<EntityType<?>, EntityType<PyroShroomEntity>> PYRO_SHROOM =
            ENTITY_TYPES.register("pyro_shroom", () -> EntityType.Builder.of(PyroShroomEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.5F).fireImmune().build("pyro_shroom"));

    public static final DeferredHolder<EntityType<?>, EntityType<SynapseWeaverEntity>> SYNAPSE_WEAVER =
            ENTITY_TYPES.register("synapse_weaver", () -> EntityType.Builder.of(SynapseWeaverEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.9F).build("synapse_weaver"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoliathCarrierEntity>> GOLIATH_CARRIER =
            ENTITY_TYPES.register("goliath_carrier", () -> EntityType.Builder.of(GoliathCarrierEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 2.8F).build("goliath_carrier"));

    public static final DeferredHolder<EntityType<?>, EntityType<LeechParasiteEntity>> LEECH_PARASITE =
            ENTITY_TYPES.register("leech_parasite", () -> EntityType.Builder.of(LeechParasiteEntity::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.4F).build("leech_parasite"));

    public static final DeferredHolder<EntityType<?>, EntityType<ColonyWardenEntity>> COLONY_WARDEN =
            ENTITY_TYPES.register("colony_warden", () -> EntityType.Builder.of(ColonyWardenEntity::new, MobCategory.MONSTER)
                    .sized(1.5F, 3.1F).build("colony_warden"));

    public static final DeferredHolder<EntityType<?>, EntityType<SporeArchitectEntity>> SPORE_ARCHITECT =
            ENTITY_TYPES.register("spore_architect", () -> EntityType.Builder.of(SporeArchitectEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 1.95F).build("spore_architect"));

    public static final DeferredHolder<EntityType<?>, EntityType<SporeBehemothEntity>> SPORE_BEHEMOTH =
            ENTITY_TYPES.register("spore_behemoth", () -> EntityType.Builder.of(SporeBehemothEntity::new, MobCategory.MONSTER)
                    .sized(2.2F, 4.0F).fireImmune().build("spore_behemoth"));

    public static final DeferredHolder<EntityType<?>, EntityType<SynapticMimicEntity>> SYNAPTIC_MIMIC =
            ENTITY_TYPES.register("synaptic_mimic", () -> EntityType.Builder.of(SynapticMimicEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 1.95F).build("synaptic_mimic"));

    public static final DeferredHolder<EntityType<?>, EntityType<PurifierDroneEntity>> PURIFIER_DRONE =
            ENTITY_TYPES.register("purifier_drone", () -> EntityType.Builder.of(PurifierDroneEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.7F).build("purifier_drone"));

    public static final DeferredHolder<EntityType<?>, EntityType<MycelialChimeraEntity>> MYCELIAL_CHIMERA =
            ENTITY_TYPES.register("mycelial_chimera", () -> EntityType.Builder.of(MycelialChimeraEntity::new, MobCategory.CREATURE)
                    .sized(1.25F, 1.55F).build("mycelial_chimera"));

    public static final DeferredHolder<EntityType<?>, EntityType<ColonySymbiontEntity>> COLONY_SYMBIONT =
            ENTITY_TYPES.register("colony_symbiont", () -> EntityType.Builder.of(ColonySymbiontEntity::new, MobCategory.CREATURE)
                    .sized(0.75F, 1.55F).build("colony_symbiont"));

    public static final DeferredHolder<EntityType<?>, EntityType<HiveCoreBossEntity>> HIVE_CORE =
            ENTITY_TYPES.register("hive_core", () -> EntityType.Builder.of(HiveCoreBossEntity::new, MobCategory.MONSTER)
                    .sized(3.0F, 4.5F).fireImmune().build("hive_core"));

    public static final DeferredHolder<EntityType<?>, EntityType<ApexMatriarchEntity>> APEX_MATRIARCH =
            ENTITY_TYPES.register("apex_matriarch", () -> EntityType.Builder.of(ApexMatriarchEntity::new, MobCategory.MONSTER)
                    .sized(3.6F, 5.6F).fireImmune().build("apex_matriarch"));

    public static final DeferredHolder<EntityType<?>, EntityType<AcidSpitProjectile>> ACID_SPIT =
            ENTITY_TYPES.register("acid_spit", () -> EntityType.Builder.<AcidSpitProjectile>of(AcidSpitProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(4).updateInterval(10).build("acid_spit"));

    public static final DeferredHolder<EntityType<?>, EntityType<NeutralizingSporeProjectile>> NEUTRALIZING_SPORE =
            ENTITY_TYPES.register("neutralizing_spore", () -> EntityType.Builder.<NeutralizingSporeProjectile>of(NeutralizingSporeProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(4).updateInterval(10).build("neutralizing_spore"));

    public static final DeferredHolder<EntityType<?>, EntityType<PurifierGrenadeProjectile>> PURIFIER_GRENADE =
            ENTITY_TYPES.register("purifier_grenade", () -> EntityType.Builder.<PurifierGrenadeProjectile>of(PurifierGrenadeProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(4).updateInterval(10).build("purifier_grenade"));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (DeferredHolder<EntityType<?>, ? extends EntityType<? extends Monster>> holder : java.util.List.of(
                SPORELING, CHITIN_BRUTE, MYCELIAL_STALKER, ACID_SPITTER, BURROWER,
                PYRO_SHROOM, SYNAPSE_WEAVER, GOLIATH_CARRIER, LEECH_PARASITE, COLONY_WARDEN, SPORE_ARCHITECT, SYNAPTIC_MIMIC)) {
            event.register(holder.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MycelialMob::checkMycelialSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SPORELING.get(), SporelingEntity.createAttributes().build());
        event.put(CHITIN_BRUTE.get(), ChitinBruteEntity.createAttributes().build());
        event.put(MYCELIAL_STALKER.get(), MycelialStalkerEntity.createAttributes().build());
        event.put(ACID_SPITTER.get(), AcidSpitterEntity.createAttributes().build());
        event.put(BURROWER.get(), BurrowerEntity.createAttributes().build());
        event.put(PYRO_SHROOM.get(), PyroShroomEntity.createAttributes().build());
        event.put(SYNAPSE_WEAVER.get(), SynapseWeaverEntity.createAttributes().build());
        event.put(GOLIATH_CARRIER.get(), GoliathCarrierEntity.createAttributes().build());
        event.put(LEECH_PARASITE.get(), LeechParasiteEntity.createAttributes().build());
        event.put(COLONY_WARDEN.get(), ColonyWardenEntity.createAttributes().build());
        event.put(SPORE_ARCHITECT.get(), SporeArchitectEntity.createAttributes().build());
        event.put(SYNAPTIC_MIMIC.get(), SynapticMimicEntity.createAttributes().build());
        event.put(SPORE_BEHEMOTH.get(), SporeBehemothEntity.createAttributes().build());
        event.put(PURIFIER_DRONE.get(), PurifierDroneEntity.createAttributes().build());
        event.put(MYCELIAL_CHIMERA.get(), MycelialChimeraEntity.createAttributes().build());
        event.put(COLONY_SYMBIONT.get(), ColonySymbiontEntity.createAttributes().build());
        event.put(HIVE_CORE.get(), HiveCoreBossEntity.createAttributes().build());
        event.put(APEX_MATRIARCH.get(), ApexMatriarchEntity.createAttributes().build());
    }
}
