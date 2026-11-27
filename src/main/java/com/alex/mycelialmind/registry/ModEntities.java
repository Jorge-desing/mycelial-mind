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
import com.alex.mycelialmind.entity.HyphalVeinEntity;
import com.alex.mycelialmind.entity.LeechParasiteEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.MycelialStalkerEntity;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.PyroShroomEntity;
import com.alex.mycelialmind.entity.RootKeeperEntity;
import com.alex.mycelialmind.entity.SporeArchitectEntity;
import com.alex.mycelialmind.entity.SporeBehemothEntity;
import com.alex.mycelialmind.entity.SporeGliderEntity;
import com.alex.mycelialmind.entity.SporelingEntity;
import com.alex.mycelialmind.entity.SynapseWeaverEntity;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.entity.projectile.PurifierGrenadeProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
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

import java.util.List;
import java.util.function.Supplier;

/**
 * Registers all Mycelial Mind entity types, projectiles, and base attributes.
 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MycelialMind.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SporelingEntity>> SPORELING =
            ENTITY_TYPES.register("sporeling", mob("sporeling", SporelingEntity::new, MobCategory.MONSTER, 0.6F, 0.5F));

    public static final DeferredHolder<EntityType<?>, EntityType<ChitinBruteEntity>> CHITIN_BRUTE =
            ENTITY_TYPES.register("chitin_brute", mob("chitin_brute", ChitinBruteEntity::new, MobCategory.MONSTER, 0.95F, 2.1F));

    public static final DeferredHolder<EntityType<?>, EntityType<MycelialStalkerEntity>> MYCELIAL_STALKER =
            ENTITY_TYPES.register("mycelial_stalker", mob("mycelial_stalker", MycelialStalkerEntity::new, MobCategory.MONSTER, 0.7F, 1.8F));

    public static final DeferredHolder<EntityType<?>, EntityType<AcidSpitterEntity>> ACID_SPITTER =
            ENTITY_TYPES.register("acid_spitter", mob("acid_spitter", AcidSpitterEntity::new, MobCategory.MONSTER, 0.8F, 1.6F));

    public static final DeferredHolder<EntityType<?>, EntityType<BurrowerEntity>> BURROWER =
            ENTITY_TYPES.register("burrower", mob("burrower", BurrowerEntity::new, MobCategory.MONSTER, 0.9F, 1.4F));

    public static final DeferredHolder<EntityType<?>, EntityType<PyroShroomEntity>> PYRO_SHROOM =
            ENTITY_TYPES.register("pyro_shroom", fireImmuneMob("pyro_shroom", PyroShroomEntity::new, 0.8F, 1.5F));

    public static final DeferredHolder<EntityType<?>, EntityType<SynapseWeaverEntity>> SYNAPSE_WEAVER =
            ENTITY_TYPES.register("synapse_weaver", mob("synapse_weaver", SynapseWeaverEntity::new, MobCategory.MONSTER, 0.8F, 1.9F));

    public static final DeferredHolder<EntityType<?>, EntityType<GoliathCarrierEntity>> GOLIATH_CARRIER =
            ENTITY_TYPES.register("goliath_carrier", mob("goliath_carrier", GoliathCarrierEntity::new, MobCategory.MONSTER, 1.6F, 2.8F));

    public static final DeferredHolder<EntityType<?>, EntityType<LeechParasiteEntity>> LEECH_PARASITE =
            ENTITY_TYPES.register("leech_parasite", mob("leech_parasite", LeechParasiteEntity::new, MobCategory.MONSTER, 0.5F, 0.4F));

    public static final DeferredHolder<EntityType<?>, EntityType<ColonyWardenEntity>> COLONY_WARDEN =
            ENTITY_TYPES.register("colony_warden", mob("colony_warden", ColonyWardenEntity::new, MobCategory.MONSTER, 1.5F, 3.1F));

    public static final DeferredHolder<EntityType<?>, EntityType<SporeArchitectEntity>> SPORE_ARCHITECT =
            ENTITY_TYPES.register("spore_architect", mob("spore_architect", SporeArchitectEntity::new, MobCategory.MONSTER, 0.75F, 1.95F));

    public static final DeferredHolder<EntityType<?>, EntityType<SporeBehemothEntity>> SPORE_BEHEMOTH =
            ENTITY_TYPES.register("spore_behemoth", fireImmuneMob("spore_behemoth", SporeBehemothEntity::new, 2.2F, 4.0F));

    public static final DeferredHolder<EntityType<?>, EntityType<SynapticMimicEntity>> SYNAPTIC_MIMIC =
            ENTITY_TYPES.register("synaptic_mimic", mob("synaptic_mimic", SynapticMimicEntity::new, MobCategory.MONSTER, 0.75F, 1.95F));

    public static final DeferredHolder<EntityType<?>, EntityType<PurifierDroneEntity>> PURIFIER_DRONE =
            ENTITY_TYPES.register("purifier_drone", mob("purifier_drone", PurifierDroneEntity::new, MobCategory.CREATURE, 0.7F, 0.7F));

    public static final DeferredHolder<EntityType<?>, EntityType<MycelialChimeraEntity>> MYCELIAL_CHIMERA =
            ENTITY_TYPES.register("mycelial_chimera", mob("mycelial_chimera", MycelialChimeraEntity::new, MobCategory.CREATURE, 1.25F, 1.55F));

    public static final DeferredHolder<EntityType<?>, EntityType<ColonySymbiontEntity>> COLONY_SYMBIONT =
            ENTITY_TYPES.register("colony_symbiont", mob("colony_symbiont", ColonySymbiontEntity::new, MobCategory.CREATURE, 0.75F, 1.55F));

    public static final DeferredHolder<EntityType<?>, EntityType<HiveCoreBossEntity>> HIVE_CORE =
            ENTITY_TYPES.register("hive_core", fireImmuneMob("hive_core", HiveCoreBossEntity::new, 3.0F, 4.5F));

    public static final DeferredHolder<EntityType<?>, EntityType<ApexMatriarchEntity>> APEX_MATRIARCH =
            ENTITY_TYPES.register("apex_matriarch", fireImmuneMob("apex_matriarch", ApexMatriarchEntity::new, 3.6F, 5.6F));

    public static final DeferredHolder<EntityType<?>, EntityType<SporeGliderEntity>> SPORE_GLIDER =
            ENTITY_TYPES.register("spore_glider", mob("spore_glider", SporeGliderEntity::new, MobCategory.CREATURE, 0.85F, 0.55F));

    public static final DeferredHolder<EntityType<?>, EntityType<RootKeeperEntity>> ROOT_KEEPER =
            ENTITY_TYPES.register("root_keeper", mob("root_keeper", RootKeeperEntity::new, MobCategory.MONSTER, 1.15F, 2.3F));

    public static final DeferredHolder<EntityType<?>, EntityType<AcidSpitProjectile>> ACID_SPIT =
            ENTITY_TYPES.register("acid_spit", projectile("acid_spit", AcidSpitProjectile::new));

    public static final DeferredHolder<EntityType<?>, EntityType<NeutralizingSporeProjectile>> NEUTRALIZING_SPORE =
            ENTITY_TYPES.register("neutralizing_spore", projectile("neutralizing_spore", NeutralizingSporeProjectile::new));

    public static final DeferredHolder<EntityType<?>, EntityType<PurifierGrenadeProjectile>> PURIFIER_GRENADE =
            ENTITY_TYPES.register("purifier_grenade", projectile("purifier_grenade", PurifierGrenadeProjectile::new));

    public static final DeferredHolder<EntityType<?>, EntityType<HyphalVeinEntity>> HYPHAL_VEIN =
            ENTITY_TYPES.register("hyphal_vein", () -> EntityType.Builder.<HyphalVeinEntity>of(HyphalVeinEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.5F).clientTrackingRange(10).updateInterval(2).build("hyphal_vein"));

    private ModEntities() {
    }

    private static <T extends Entity> Supplier<EntityType<T>> mob(
            String name, EntityType.EntityFactory<T> factory, MobCategory category, float width, float height) {
        return () -> EntityType.Builder.of(factory, category).sized(width, height).build(name);
    }

    private static <T extends Entity> Supplier<EntityType<T>> fireImmuneMob(
            String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return () -> EntityType.Builder.of(factory, MobCategory.MONSTER).sized(width, height).fireImmune().build(name);
    }

    private static <T extends Entity> Supplier<EntityType<T>> projectile(
            String name, EntityType.EntityFactory<T> factory) {
        return () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(0.35F, 0.35F).clientTrackingRange(4).updateInterval(10).build(name);
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (DeferredHolder<EntityType<?>, ? extends EntityType<? extends Monster>> holder : List.of(
                SPORELING, CHITIN_BRUTE, MYCELIAL_STALKER, ACID_SPITTER, BURROWER,
                PYRO_SHROOM, SYNAPSE_WEAVER, GOLIATH_CARRIER, LEECH_PARASITE, COLONY_WARDEN,
                SPORE_ARCHITECT, SYNAPTIC_MIMIC, ROOT_KEEPER)) {
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
        event.put(SPORE_GLIDER.get(), SporeGliderEntity.createAttributes().build());
        event.put(ROOT_KEEPER.get(), RootKeeperEntity.createAttributes().build());
    }
}
