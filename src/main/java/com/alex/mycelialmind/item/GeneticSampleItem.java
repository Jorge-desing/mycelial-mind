package com.alex.mycelialmind.item;

import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.item.tooltip.MycelialTooltipData;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Biological tissue sample carrying a coded AdaptationTrait ID for Synaptic Bench synthesis.
 */
public class GeneticSampleItem extends Item {
    public GeneticSampleItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static AdaptationTrait getTrait(ItemStack stack) {
        String id = stack.get(ModDataComponents.TRAIT_ID.get());
        if (id == null) {
            return null;
        }
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            if (trait.id().equals(id)) {
                return trait;
            }
        }
        return null;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        AdaptationTrait trait = getTrait(stack);
        if (trait == null) {
            return Optional.empty();
        }
        return Optional.of(new MycelialTooltipData.TraitBadgeTooltip(
                MycelialTooltipData.colorForTrait(trait),
                MycelialText.trait(trait),
                MycelialText.stimulus(trait.trigger())
        ));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        AdaptationTrait trait = getTrait(stack);
        if (trait != null) {
            tooltip.add(Component.translatable("tooltip.mycelialmind.coded_trait", MycelialText.trait(trait))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("tooltip.mycelialmind.uncoded_sample")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
