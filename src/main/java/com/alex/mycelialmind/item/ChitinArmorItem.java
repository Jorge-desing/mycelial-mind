package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Reactive & Evolving Chitin Armor piece (BASE -> MUTATED -> ELITE).
 * Each equipped piece reduces Mycelial Infection duration by 20%,
 * MUTATED regenerates durability on mycelium/Fungal Tide,
 * and ELITE grants periodic Absorption and reflects damage to MycelialMobs.
 */
public class ChitinArmorItem extends ArmorItem {
    public ChitinArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static EvolutionRank getArmorRank(ItemStack stack) {
        Integer raw = stack.get(ModDataComponents.ARMOR_RANK.get());
        return raw == null ? EvolutionRank.BASE : EvolutionRank.fromId(raw);
    }

    public static void setArmorRank(ItemStack stack, EvolutionRank rank) {
        stack.set(ModDataComponents.ARMOR_RANK.get(), rank.id());
    }

    public static boolean tryEvolvePiece(ItemStack armor, ItemStack catalyst, @Nullable Player player) {
        if (!(armor.getItem() instanceof ChitinArmorItem) || catalyst.isEmpty()) {
            return false;
        }
        EvolutionRank current = getArmorRank(armor);
        EvolutionRank next = null;
        if (current == EvolutionRank.BASE && catalyst.is(ModItems.SYNAPTIC_CORE.get())) {
            next = EvolutionRank.MUTATED;
        } else if (current == EvolutionRank.MUTATED
                && (catalyst.is(ModItems.OVERMIND_ESSENCE.get()) || catalyst.is(ModItems.TIDE_BEACON.get()))) {
            next = EvolutionRank.ELITE;
        }
        if (next == null) {
            return false;
        }
        setArmorRank(armor, next);
        if (player == null || !player.getAbilities().instabuild) {
            catalyst.shrink(1);
        }
        if (player != null && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(), player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.4, 0.5, 0.4, 0.05);
            serverLevel.playSound(null, player.blockPosition(), ModSounds.CHITIN_ARMOR_EVOLVE.get(),
                    SoundSource.PLAYERS, 1.0F, next == EvolutionRank.ELITE ? 1.15F : 1.0F);
            player.sendSystemMessage(Component.translatable("message.mycelialmind.armor_evolved", MycelialText.rank(next))
                    .withStyle(next == EvolutionRank.ELITE ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD));
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerDiscoveryData.unlockMilestone(serverPlayer, ProgressionGoalEngine.MILESTONE_CHITIN_EVOLVED);
            }
        }
        return true;
    }

    public static int countEquippedPieces(LivingEntity entity) {
        int count = 0;
        for (ItemStack armor : entity.getArmorSlots()) {
            if (armor.getItem() instanceof ChitinArmorItem) {
                count++;
            }
        }
        return count;
    }

    public static int countElitePieces(Player player) {
        int count = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof ChitinArmorItem && getArmorRank(armor) == EvolutionRank.ELITE) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasFullSet(LivingEntity entity) {
        return countEquippedPieces(entity) >= 4;
    }

    public static int reduceInfectionDuration(LivingEntity target, int baseTicks) {
        if (target instanceof Player player) {
            int pieces = countEquippedPieces(player);
            if (pieces > 0) {
                return Math.max(20, (int) Math.round(baseTicks * (1.0 - 0.20 * pieces)));
            }
        }
        return baseTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack catalyst = player.getItemInHand(otherHand);
        if (!catalyst.isEmpty() && !level.isClientSide() && tryEvolvePiece(stack, catalyst, player)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers base = super.getDefaultAttributeModifiers(stack);
        EvolutionRank rank = getArmorRank(stack);
        if (rank == EvolutionRank.BASE) {
            return base;
        }
        double bonusArmor = rank == EvolutionRank.ELITE ? 2.0D : 1.0D;
        double bonusTough = rank == EvolutionRank.ELITE ? 0.75D : 0.5D;
        EquipmentSlotGroup slotGroup = EquipmentSlotGroup.bySlot(this.getEquipmentSlot());
        ResourceLocation armorId = ResourceLocation.fromNamespaceAndPath(
                MycelialMind.MODID, "chitin_rank_armor_" + this.type.getName());
        ResourceLocation toughId = ResourceLocation.fromNamespaceAndPath(
                MycelialMind.MODID, "chitin_rank_toughness_" + this.type.getName());
        ItemAttributeModifiers updated = base
                .withModifierAdded(Attributes.ARMOR,
                        new AttributeModifier(armorId, bonusArmor, AttributeModifier.Operation.ADD_VALUE), slotGroup)
                .withModifierAdded(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(toughId, bonusTough, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        if (rank == EvolutionRank.ELITE) {
            ResourceLocation kbId = ResourceLocation.fromNamespaceAndPath(
                    MycelialMind.MODID, "chitin_rank_knockback_" + this.type.getName());
            updated = updated.withModifierAdded(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(kbId, 0.05D, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        return updated;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }
        if (player.getItemBySlot(this.getEquipmentSlot()) != stack) {
            return;
        }
        EvolutionRank rank = getArmorRank(stack);
        if (rank.id() >= EvolutionRank.MUTATED.id() && stack.isDamaged() && player.tickCount % 200 == 0) {
            boolean onMycelium = serverLevel.getBlockState(player.blockPosition().below()).is(ModBlocks.CREEPING_MYCELIUM.get());
            boolean inTide = HiveMindSavedData.get(serverLevel).isFungalTideActive(serverLevel.getGameTime());
            if (onMycelium || inTide) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            }
        }
        if (this.type == Type.CHESTPLATE && player.tickCount % 240 == 0 && countElitePieces(player) >= 2) {
            if (!player.hasEffect(MobEffects.ABSORPTION)) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, false, false));
            }
        }
    }

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        EvolutionRank rank = getArmorRank(stack);
        int layerNum = innerModel ? 2 : 1;
        String suffix = switch (rank) {
            case MUTATED -> "_mutated";
            case ELITE -> "_elite";
            default -> "";
        };
        return ResourceLocation.fromNamespaceAndPath(
                MycelialMind.MODID,
                "textures/models/armor/chitin_layer_" + layerNum + suffix + ".png"
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EvolutionRank rank = getArmorRank(stack);
        ChatFormatting col = switch (rank) {
            case ELITE -> ChatFormatting.LIGHT_PURPLE;
            case MUTATED -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };
        tooltip.add(Component.translatable("tooltip.mycelialmind.armor_rank", MycelialText.rank(rank)).withStyle(col));
        tooltip.add(Component.translatable("tooltip.mycelialmind.chitin_piece_bonus")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.mycelialmind.chitin_set_bonus")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
