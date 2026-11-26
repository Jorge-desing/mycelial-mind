package com.alex.mycelialmind.world;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.item.FieldEngramItem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Coordinates multi-pool structure chest loot tables, tier/trait/strain-aware
 * adaptive mob drops, and progression/audit invariants across Mycelial Mind.
 */
public final class MycelialLootSystem {
    public static final ResourceKey<LootTable> OUTBREAK_SEALED_VAULT = chestKey("outbreak_sealed_vault");
    public static final ResourceKey<LootTable> SURFACE_HIVE_CACHE = chestKey("surface_hive_cache");
    public static final ResourceKey<LootTable> SURFACE_HIVE_GALLERY = chestKey("surface_hive_gallery");
    public static final ResourceKey<LootTable> SURFACE_HIVE_CRYPT = chestKey("surface_hive_crypt");
    public static final ResourceKey<LootTable> SANCTUM_FORBIDDEN_LAB = chestKey("sanctum_forbidden_lab");
    public static final ResourceKey<LootTable> SANCTUM_OSSUARY_CRYPT = chestKey("sanctum_ossuary_crypt");
    public static final ResourceKey<LootTable> SANCTUM_SPORE_VAULT = chestKey("sanctum_spore_vault");
    public static final ResourceKey<LootTable> CITADEL_AMBER_CHAPEL = chestKey("citadel_amber_chapel");
    public static final ResourceKey<LootTable> CITADEL_CRIMSON_CHAPEL = chestKey("citadel_crimson_chapel");
    public static final ResourceKey<LootTable> CITADEL_ROYAL_CRYPT = chestKey("citadel_royal_crypt");
    public static final ResourceKey<LootTable> SINKHOLE_ROOT_VAULT = chestKey("sinkhole_root_vault");
    public static final ResourceKey<LootTable> SANCTUM_QUARANTINE_CACHE = chestKey("sanctum_quarantine_cache");
    public static final ResourceKey<LootTable> NETHER_BREACH_RELIQUARY = chestKey("nether_breach_reliquary");
    public static final ResourceKey<LootTable> END_SPIRE_VAULT = chestKey("end_spire_vault");

    public static final List<ResourceKey<LootTable>> ALL_CHEST_TABLES = List.of(
            OUTBREAK_SEALED_VAULT,
            SURFACE_HIVE_CACHE,
            SURFACE_HIVE_GALLERY,
            SURFACE_HIVE_CRYPT,
            SANCTUM_FORBIDDEN_LAB,
            SANCTUM_OSSUARY_CRYPT,
            SANCTUM_SPORE_VAULT,
            SANCTUM_QUARANTINE_CACHE,
            CITADEL_AMBER_CHAPEL,
            CITADEL_CRIMSON_CHAPEL,
            CITADEL_ROYAL_CRYPT,
            SINKHOLE_ROOT_VAULT,
            NETHER_BREACH_RELIQUARY,
            END_SPIRE_VAULT
    );

    public static ResourceKey<LootTable> selectSealedBioVaultTable(HiveProgressionTier tier, RandomSource random) {
        if (tier == null) {
            return OUTBREAK_SEALED_VAULT;
        }
        return switch (tier) {
            case DORMANT -> OUTBREAK_SEALED_VAULT;
            case AWAKENED -> SANCTUM_SPORE_VAULT;
            case ADAPTIVE -> SANCTUM_FORBIDDEN_LAB;
            case OVERMIND -> (random != null && random.nextBoolean()) ? CITADEL_AMBER_CHAPEL : CITADEL_CRIMSON_CHAPEL;
        };
    }

    /**
     * Pure descriptor for unit testing adaptive drop logic without bootstrapping registries.
     */
    public record AdaptiveDropSpec(
            String itemId,
            int count,
            @Nullable AdaptationTrait sampleTrait,
            @Nullable EvolutionRank armorRank
    ) {
    }

    private MycelialLootSystem() {
    }

    private static ResourceKey<LootTable> chestKey(String path) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "chests/" + path)
        );
    }

    public static ItemStack createCodedSample(@Nullable AdaptationTrait trait) {
        ItemStack sample = new ItemStack(ModItems.GENETIC_SAMPLE.get());
        if (trait != null) {
            sample.set(ModDataComponents.TRAIT_ID.get(), trait.id());
        }
        return sample;
    }

    public static void populateStructureChest(
            WorldGenLevel level,
            BlockPos pos,
            ChestBlockEntity chest,
            ResourceKey<LootTable> tableKey,
            RandomSource random,
            HiveProgressionTier tier,
            boolean ensureSurfaceHiveAuditCore
    ) {
        if (level != null && level.getLevel() instanceof ServerLevel serverLevel) {
            LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(tableKey);
            if (table != LootTable.EMPTY) {
                LootParams params = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .create(LootContextParamSets.CHEST);
                table.fill(chest, params, random.nextLong());
            }

            int bonus = StructureProgressionPolicy.chestBonusCount(tier);
            if (bonus >= 1 && random.nextFloat() < (0.40F + 0.15F * bonus)) {
                AdaptationTrait picked = pickTrait(serverLevel, random);
                placeInFreeSlot(chest, createCodedSample(picked), random);
                ItemStack extraSupply = random.nextBoolean()
                        ? new ItemStack(ModItems.CHITIN_PLATE.get(), 1 + bonus)
                        : new ItemStack(ModItems.SPORE_ANTIDOTE.get(), 1 + (bonus >= 2 ? 1 : 0));
                placeInFreeSlot(chest, extraSupply, random);
            }

            FieldEngramId chestEngram = selectChestEngram(tableKey, random);
            if (chestEngram != null && random.nextFloat() < 0.55F) {
                placeInFreeSlot(chest, FieldEngramItem.createStack(chestEngram), random);
            }

            boolean isCryptOrCitadel = isCryptOrCitadelTable(tableKey);
            if (bonus >= 2 || (isCryptOrCitadel && random.nextFloat() < 0.30F)) {
                boolean evolvedExisting = false;
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (stack.getItem() instanceof ChitinArmorItem
                            && ChitinArmorItem.getArmorRank(stack) == EvolutionRank.BASE) {
                        ChitinArmorItem.tryEvolvePiece(stack, new ItemStack(ModItems.SYNAPTIC_CORE.get()), null);
                        evolvedExisting = true;
                        break;
                    }
                }
                if (!evolvedExisting && isCryptOrCitadel && tier.level() >= HiveProgressionTier.ADAPTIVE.level()) {
                    ItemStack piece = randomChitinArmorPiece(random);
                    ChitinArmorItem.tryEvolvePiece(piece, new ItemStack(ModItems.SYNAPTIC_CORE.get()), null);
                    placeInFreeSlot(chest, piece, random);
                }
            }
        }

        if (ensureSurfaceHiveAuditCore) {
            ensureSurfaceHiveCoreItems(chest);
        }
    }

    @Nullable
    public static FieldEngramId selectChestEngram(ResourceKey<LootTable> tableKey, @Nullable RandomSource random) {
        if (tableKey == null) {
            return null;
        }
        boolean coin = random != null && random.nextBoolean();
        if (tableKey == OUTBREAK_SEALED_VAULT) {
            return FieldEngramId.OUTBREAK_SIGNAL;
        }
        if (tableKey == SANCTUM_FORBIDDEN_LAB || tableKey == SANCTUM_SPORE_VAULT || tableKey == SANCTUM_OSSUARY_CRYPT) {
            return coin ? FieldEngramId.SYNAPTIC_BLOOM : FieldEngramId.HYPHAL_MEMORY;
        }
        if (tableKey == SURFACE_HIVE_CACHE || tableKey == SURFACE_HIVE_GALLERY || tableKey == SURFACE_HIVE_CRYPT) {
            return coin ? FieldEngramId.TIDE_PRESSURE : FieldEngramId.HYPHAL_MEMORY;
        }
        if (tableKey == CITADEL_AMBER_CHAPEL || tableKey == CITADEL_CRIMSON_CHAPEL || tableKey == CITADEL_ROYAL_CRYPT) {
            return coin ? FieldEngramId.HOMEOSTASIS_TRAUMA : FieldEngramId.PRIMORDIAL_LINEAGE;
        }
        return null;
    }

    private static boolean isCryptOrCitadelTable(ResourceKey<LootTable> tableKey) {
        return tableKey == SURFACE_HIVE_CRYPT
                || tableKey == SANCTUM_OSSUARY_CRYPT
                || tableKey == CITADEL_AMBER_CHAPEL
                || tableKey == CITADEL_CRIMSON_CHAPEL
                || tableKey == CITADEL_ROYAL_CRYPT;
    }

    private static AdaptationTrait pickTrait(ServerLevel level, RandomSource random) {
        Set<AdaptationTrait> active = HiveMindSavedData.get(level).getActiveTraits();
        if (!active.isEmpty()) {
            List<AdaptationTrait> ordered = orderedTraits(active);
            return ordered.get(random.nextInt(ordered.size()));
        }
        AdaptationTrait[] all = AdaptationTrait.values();
        return all[random.nextInt(all.length)];
    }

    private static List<AdaptationTrait> orderedTraits(Set<AdaptationTrait> active) {
        List<AdaptationTrait> ordered = new ArrayList<>(active.size());
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            if (active.contains(trait)) {
                ordered.add(trait);
            }
        }
        return ordered;
    }

    private static ItemStack randomChitinArmorPiece(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> new ItemStack(ModItems.CHITIN_HELMET.get());
            case 1 -> new ItemStack(ModItems.CHITIN_CHESTPLATE.get());
            case 2 -> new ItemStack(ModItems.CHITIN_LEGGINGS.get());
            default -> new ItemStack(ModItems.CHITIN_BOOTS.get());
        };
    }

    private static void placeInFreeSlot(ChestBlockEntity chest, ItemStack stack, RandomSource random) {
        if (stack.isEmpty()) {
            return;
        }
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).isEmpty()) {
                emptySlots.add(i);
            }
        }
        if (!emptySlots.isEmpty()) {
            int chosen = emptySlots.get(random.nextInt(emptySlots.size()));
            chest.setItem(chosen, stack);
        }
    }

    private static void ensureSurfaceHiveCoreItems(ChestBlockEntity chest) {
        Item[] required = {
                ModItems.MYCELIAL_BIOMASS.get(),
                ModItems.CHITIN_PLATE.get(),
                ModItems.LATENT_SPORE.get(),
                ModItems.SYNAPTIC_CORE.get()
        };
        for (Item item : required) {
            if (containsItem(chest, item)) {
                continue;
            }
            boolean placed = false;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (chest.getItem(i).isEmpty()) {
                    chest.setItem(i, new ItemStack(item, 1));
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                for (int i = chest.getContainerSize() - 1; i >= 0; i--) {
                    Item existing = chest.getItem(i).getItem();
                    if (existing != required[0] && existing != required[1]
                            && existing != required[2] && existing != required[3]) {
                        chest.setItem(i, new ItemStack(item, 1));
                        break;
                    }
                }
            }
        }
    }

    private static boolean containsItem(ChestBlockEntity chest, Item target) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(target)) {
                return true;
            }
        }
        return false;
    }

    public static List<AdaptiveDropSpec> computeAdaptiveMobDropSpecs(
            EvolutionRank rank,
            HiveStrain strain,
            HiveProgressionTier tier,
            Set<AdaptationTrait> activeTraits,
            boolean inDeepMycelium,
            int lootingLevel,
            RandomSource random
    ) {
        return computeAdaptiveMobDropSpecs(
                rank, strain, tier, activeTraits, inDeepMycelium, lootingLevel, false, random);
    }

    public static List<AdaptiveDropSpec> computeAdaptiveMobDropSpecs(
            EvolutionRank rank,
            HiveStrain strain,
            HiveProgressionTier tier,
            Set<AdaptationTrait> activeTraits,
            boolean inDeepMycelium,
            int lootingLevel,
            boolean isPrimordial,
            RandomSource random
    ) {
        List<AdaptiveDropSpec> specs = new ArrayList<>();
        EvolutionRank safeRank = rank != null ? rank : EvolutionRank.BASE;
        HiveStrain safeStrain = strain != null ? strain : HiveStrain.ORTHODOX;
        HiveProgressionTier safeTier = tier != null ? tier : HiveProgressionTier.DORMANT;
        int clampedLooting = Math.max(0, Math.min(5, lootingLevel));
        float lootingBonus = clampedLooting * 0.06F;
        float tierBonus = safeTier.ordinal() * 0.04F;

        // 1. Contextual coded genetic sample if traits are active
        if (activeTraits != null && !activeTraits.isEmpty()) {
            float sampleChance = (switch (safeRank) {
                case ELITE -> 0.35F;
                case MUTATED -> 0.15F;
                case BASE -> 0.04F;
            }) + lootingBonus;
            if (random.nextFloat() < sampleChance) {
                List<AdaptationTrait> eligible = orderedTraits(activeTraits);
                AdaptationTrait chosen = eligible.get(random.nextInt(eligible.size()));
                specs.add(new AdaptiveDropSpec("mycelialmind:genetic_sample", 1, chosen, null));
            }
        }

        // 2. Strain / dimension contextual drops
        switch (safeStrain) {
            case AMBER -> {
                if (random.nextFloat() < 0.22F + lootingBonus) {
                    if (random.nextFloat() < 0.65F) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:luminescent_amber_vein", 1 + random.nextInt(2), null, null));
                    } else {
                        specs.add(new AdaptiveDropSpec("mycelialmind:truce_pheromone", 1, null, null));
                    }
                }
            }
            case CRIMSON -> {
                if (random.nextFloat() < 0.22F + lootingBonus) {
                    if (random.nextFloat() < 0.60F) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:spore_antidote", 1, null, null));
                    } else {
                        specs.add(new AdaptiveDropSpec("mycelialmind:volatile_engram", 1, null, null));
                    }
                }
            }
            case ORTHODOX -> {
                if (inDeepMycelium && random.nextFloat() < 0.25F + lootingBonus) {
                    float roll = random.nextFloat();
                    if (roll < 0.55F) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:chitin_plate", 1 + random.nextInt(2), null, null));
                    } else if (roll < 0.85F) {
                        specs.add(new AdaptiveDropSpec("minecraft:amethyst_shard", 1 + random.nextInt(2), null, null));
                    } else {
                        specs.add(new AdaptiveDropSpec("mycelialmind:neural_echo_core", 1, null, null));
                    }
                }
            }
        }

        // 3. EvolutionRank weighted bonus pools
        switch (safeRank) {
            case BASE -> {
                if ((safeTier.ordinal() >= HiveProgressionTier.ADAPTIVE.ordinal() || clampedLooting > 0)
                        && random.nextFloat() < (0.15F + lootingBonus + tierBonus)) {
                    String item = random.nextBoolean() ? "mycelialmind:mycelial_biomass" : "mycelialmind:latent_spore";
                    specs.add(new AdaptiveDropSpec(item, 1, null, null));
                }
            }
            case MUTATED -> {
                if (random.nextFloat() < (0.45F + lootingBonus + tierBonus)) {
                    int w = random.nextInt(100);
                    if (w < 40) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:mycelial_biomass", 1 + random.nextInt(2), null, null));
                    } else if (w < 70) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:chitin_plate", 1, null, null));
                    } else if (w < 90) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:latent_spore", 1, null, null));
                    } else {
                        specs.add(new AdaptiveDropSpec("mycelialmind:spore_antidote", 1, null, null));
                    }
                }
            }
            case ELITE -> {
                if (random.nextFloat() < (0.75F + lootingBonus)) {
                    int w1 = random.nextInt(100);
                    if (w1 < 30) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:chitin_plate", 1 + random.nextInt(2), null, null));
                    } else if (w1 < 55) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:latent_spore", 1 + random.nextInt(2), null, null));
                    } else if (w1 < 80) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:synaptic_core", 1, null, null));
                    } else if (w1 < 92) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:volatile_engram", 1, null, null));
                    } else {
                        specs.add(new AdaptiveDropSpec("mycelialmind:neural_echo_core", 1, null, null));
                    }
                }
                if ((inDeepMycelium || safeTier == HiveProgressionTier.OVERMIND || safeTier == HiveProgressionTier.ADAPTIVE)
                        && random.nextFloat() < (0.25F + lootingBonus)) {
                    int w2 = random.nextInt(100);
                    if (w2 < 55) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:chitin_plate", 1 + random.nextInt(2), null, null));
                    } else if (w2 < 85) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:purifier_grenade", 1 + random.nextInt(2), null, null));
                    } else {
                        String pieceId = switch (random.nextInt(4)) {
                            case 0 -> "mycelialmind:chitin_helmet";
                            case 1 -> "mycelialmind:chitin_chestplate";
                            case 2 -> "mycelialmind:chitin_leggings";
                            default -> "mycelialmind:chitin_boots";
                        };
                        specs.add(new AdaptiveDropSpec(pieceId, 1, null, EvolutionRank.MUTATED));
                    }
                }
            }
        }
        if (isPrimordial) {
            specs.add(new AdaptiveDropSpec("mycelialmind:luminescent_amber_vein", 2, null, null));
            specs.add(new AdaptiveDropSpec("mycelialmind:latent_spore", 2, null, null));
            if (safeTier == HiveProgressionTier.DORMANT) {
                specs.add(new AdaptiveDropSpec("mycelialmind:chitin_plate", 2, null, null));
                specs.add(new AdaptiveDropSpec("mycelialmind:spore_antidote", 1, null, null));
            } else {
                specs.add(new AdaptiveDropSpec("mycelialmind:synaptic_core", 1, null, null));
                AdaptationTrait chosen;
                if (activeTraits != null && !activeTraits.isEmpty()) {
                    List<AdaptationTrait> eligible = orderedTraits(activeTraits);
                    chosen = eligible.get(random.nextInt(eligible.size()));
                } else {
                    AdaptationTrait[] all = AdaptationTrait.values();
                    chosen = all[random.nextInt(all.length)];
                }
                specs.add(new AdaptiveDropSpec("mycelialmind:genetic_sample", 1, chosen, null));
                if (safeTier.level() >= HiveProgressionTier.ADAPTIVE.level()) {
                    specs.add(new AdaptiveDropSpec("mycelialmind:volatile_engram", 1, null, null));
                    if (random.nextFloat() < 0.35F) {
                        specs.add(new AdaptiveDropSpec("mycelialmind:neural_echo_core", 1, null, null));
                    }
                }
                if (safeTier == HiveProgressionTier.OVERMIND) {
                    specs.add(new AdaptiveDropSpec("mycelialmind:concentrated_antidote", 1, null, null));
                    if (random.nextFloat() < 0.40F) {
                        specs.add(new AdaptiveDropSpec(
                                random.nextBoolean() ? "mycelialmind:truce_pheromone" : "mycelialmind:neural_echo_core",
                                1, null, null));
                    }
                }
            }
        }
        return specs;
    }

    public static List<ItemStack> computeAdaptiveMobDrops(
            EvolutionRank rank,
            HiveStrain strain,
            HiveProgressionTier tier,
            Set<AdaptationTrait> activeTraits,
            boolean inDeepMycelium,
            int lootingLevel,
            RandomSource random
    ) {
        return computeAdaptiveMobDrops(
                rank, strain, tier, activeTraits, inDeepMycelium, lootingLevel, false, random);
    }

    public static List<ItemStack> computeAdaptiveMobDrops(
            EvolutionRank rank,
            HiveStrain strain,
            HiveProgressionTier tier,
            Set<AdaptationTrait> activeTraits,
            boolean inDeepMycelium,
            int lootingLevel,
            boolean isPrimordial,
            RandomSource random
    ) {
        List<AdaptiveDropSpec> specs = computeAdaptiveMobDropSpecs(
                rank, strain, tier, activeTraits, inDeepMycelium, lootingLevel, isPrimordial, random
        );
        List<ItemStack> stacks = new ArrayList<>(specs.size());
        for (AdaptiveDropSpec spec : specs) {
            ItemStack stack = materializeSpec(spec);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static ItemStack materializeSpec(AdaptiveDropSpec spec) {
        if ("mycelialmind:genetic_sample".equals(spec.itemId())) {
            ItemStack sample = createCodedSample(spec.sampleTrait());
            sample.setCount(Math.max(1, spec.count()));
            return sample;
        }
        Item item = switch (spec.itemId()) {
            case "mycelialmind:mycelial_biomass" -> ModItems.MYCELIAL_BIOMASS.get();
            case "mycelialmind:chitin_plate" -> ModItems.CHITIN_PLATE.get();
            case "mycelialmind:latent_spore" -> ModItems.LATENT_SPORE.get();
            case "mycelialmind:synaptic_core" -> ModItems.SYNAPTIC_CORE.get();
            case "mycelialmind:luminescent_amber_vein" -> ModBlocks.LUMINESCENT_AMBER_VEIN.asItem();
            case "mycelialmind:truce_pheromone" -> ModItems.TRUCE_PHEROMONE.get();
            case "mycelialmind:spore_antidote" -> ModItems.SPORE_ANTIDOTE.get();
            case "mycelialmind:concentrated_antidote" -> ModItems.CONCENTRATED_ANTIDOTE.get();
            case "mycelialmind:volatile_engram" -> ModItems.VOLATILE_ENGRAM.get();
            case "mycelialmind:neural_echo_core" -> ModItems.NEURAL_ECHO_CORE.get();
            case "mycelialmind:false_engram_injector" -> ModItems.FALSE_ENGRAM_INJECTOR.get();
            case "mycelialmind:purifier_grenade" -> ModItems.PURIFIER_GRENADE.get();
            case "mycelialmind:chitin_helmet" -> ModItems.CHITIN_HELMET.get();
            case "mycelialmind:chitin_chestplate" -> ModItems.CHITIN_CHESTPLATE.get();
            case "mycelialmind:chitin_leggings" -> ModItems.CHITIN_LEGGINGS.get();
            case "mycelialmind:chitin_boots" -> ModItems.CHITIN_BOOTS.get();
            case "minecraft:amethyst_shard" -> Items.AMETHYST_SHARD;
            default -> Items.AIR;
        };
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, Math.max(1, spec.count()));
        if (spec.armorRank() != null && stack.getItem() instanceof ChitinArmorItem) {
            ChitinArmorItem.setArmorRank(stack, spec.armorRank());
        }
        return stack;
    }

    public static void rollAdaptiveMobBonusDrops(MycelialMob mob, ServerLevel level, DamageSource damageSource) {
        int lootingLevel = 0;
        if (damageSource != null && damageSource.getEntity() instanceof LivingEntity attacker) {
            var lootingHolder = level.registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(Enchantments.LOOTING);
            if (lootingHolder.isPresent()) {
                lootingLevel = EnchantmentHelper.getEnchantmentLevel(lootingHolder.get(), attacker);
            }
        }

        HiveMindSavedData hive = HiveMindSavedData.get(level);
        EnumSet<AdaptationTrait> combinedTraits = EnumSet.noneOf(AdaptationTrait.class);
        combinedTraits.addAll(mob.getTraits());
        combinedTraits.addAll(hive.getActiveTraits());

        HiveProgressionTier tier = HiveProgressionTier.resolveEffectiveTier(
                hive.getThreatLevel(), hive.getHighestTierReached());
        boolean inDeepMycelium = level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);

        List<ItemStack> drops = computeAdaptiveMobDrops(
                mob.getEvolutionRank(),
                mob.getHiveStrain(),
                tier,
                combinedTraits,
                inDeepMycelium,
                lootingLevel,
                mob.isPrimordialVariant(),
                mob.getRandom()
        );
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                mob.spawnAtLocation(drop);
            }
        }
        if (mob instanceof com.alex.mycelialmind.entity.SynapticMimicEntity mimic) {
            String counterId = com.alex.mycelialmind.hive.SynapticMimicArchitectureEngine.computeMimicCounterIntelDropId(
                    mob.getEvolutionRank(),
                    mimic.wasUnmaskedByCounterplay(),
                    mob.getRandom().nextFloat()
            );
            if (counterId != null) {
                ItemStack counterDrop = materializeSpec(new AdaptiveDropSpec(counterId, 1, null, null));
                if (!counterDrop.isEmpty()) {
                    mob.spawnAtLocation(counterDrop);
                }
            }
            float engramChance = mimic.wasUnmaskedByCounterplay() ? 1.0F : 0.45F;
            if (mob.getRandom().nextFloat() < engramChance) {
                mob.spawnAtLocation(FieldEngramItem.createStack(FieldEngramId.MIMIC_DECEPTION));
            }
        }
    }

    /**
     * Pure weighted trace-loot spec shared by NeoForge Global Loot Modifiers and headless unit tests.
     */
    @Nullable
    public static AdaptiveDropSpec rollTraceDropSpec(String poolPreset, RandomSource random) {
        if (random == null || poolPreset == null) {
            return null;
        }
        if ("underground_traces".equals(poolPreset)) {
            int w = random.nextInt(100);
            if (w < 40) {
                return new AdaptiveDropSpec("mycelialmind:latent_spore", 1 + random.nextInt(2), null, null);
            }
            if (w < 75) {
                return new AdaptiveDropSpec("mycelialmind:mycelial_biomass", 2 + random.nextInt(3), null, null);
            }
            if (w < 90) {
                return new AdaptiveDropSpec("mycelialmind:spore_antidote", 1, null, null);
            }
            return new AdaptiveDropSpec("mycelialmind:luminescent_amber_vein", 1 + random.nextInt(2), null, null);
        }
        if ("deep_traces".equals(poolPreset)) {
            int w = random.nextInt(100);
            if (w < 30) {
                return new AdaptiveDropSpec("mycelialmind:luminescent_amber_vein", 1 + random.nextInt(2), null, null);
            }
            if (w < 55) {
                return new AdaptiveDropSpec("mycelialmind:chitin_plate", 2 + random.nextInt(3), null, null);
            }
            if (w < 75) {
                return new AdaptiveDropSpec("mycelialmind:concentrated_antidote", 1, null, null);
            }
            if (w < 90) {
                return new AdaptiveDropSpec("mycelialmind:volatile_engram", 1, null, null);
            }
            return new AdaptiveDropSpec("mycelialmind:synaptic_core", 1, null, null);
        }
        return null;
    }

    /**
     * Materializes a weighted trace-loot roll for NeoForge Global Loot Modifiers.
     */
    public static ItemStack rollTraceDrop(String poolPreset, RandomSource random) {
        AdaptiveDropSpec spec = rollTraceDropSpec(poolPreset, random);
        return spec == null ? ItemStack.EMPTY : materializeSpec(spec);
    }
}
