package com.alex.mycelialmind.item;

import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.item.tooltip.MycelialTooltipData;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.network.ModNetworking;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialParticles;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Targeted Counter-Serum synthesized from a GeneticSample.
 * Bleeds 50% of the targeted StimulusType score from HiveMindSavedData.
 */
public class CounterSerumItem extends Item {
    public CounterSerumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            AdaptationTrait trait = GeneticSampleItem.getTrait(stack);
            if (trait == null) {
                trait = AdaptationTrait.MELEE_THORNS;
            }
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            double bled = hive.bleedStimulusRatio(
                    trait.trigger(),
                    MycelialConfig.COUNTER_SERUM_BLEED_RATIO.get(),
                    serverLevel.getGameTime()
            );

            ModNetworking.sendToPlayersInLevelReady(serverLevel, sp -> HiveSyncPayload.forPlayer(serverLevel, sp));
            MycelialParticles.counterSerum(serverLevel, player, trait.trigger());
            serverLevel.playSound(null, player.blockPosition(), ModSounds.COUNTER_SERUM_INJECT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            player.sendSystemMessage(Component.translatable(
                    "message.mycelialmind.counter_serum_used",
                    MycelialText.stimulus(trait.trigger()),
                    String.format("%.1f", bled)
            ).withStyle(ChatFormatting.AQUA));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        AdaptationTrait trait = GeneticSampleItem.getTrait(stack);
        if (trait == null) {
            return Optional.empty();
        }
        return Optional.of(new MycelialTooltipData.TraitBadgeTooltip(
                MycelialTooltipData.colorForTrait(trait),
                MycelialText.stimulus(trait.trigger()),
                MycelialText.trait(trait)
        ));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        AdaptationTrait trait = GeneticSampleItem.getTrait(stack);
        String id = stack.get(ModDataComponents.TRAIT_ID.get());
        if (trait != null) {
            tooltip.add(Component.translatable("tooltip.mycelialmind.counter_target", MycelialText.trait(trait))
                    .withStyle(ChatFormatting.GREEN));
        } else if (id != null) {
            tooltip.add(Component.translatable("tooltip.mycelialmind.counter_target", id)
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
