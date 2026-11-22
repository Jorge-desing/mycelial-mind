package com.alex.mycelialmind.item;

import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.network.SymbiosisSyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.symbiosis.PlayerSymbiosisData;
import com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Synthesized Symbiotic Mutagen that implants an adaptive trait into the player's Symbiosis slots.
 */
public class SymbioticMutagenItem extends Item {
    public SymbioticMutagenItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static SymbioticMutagen getMutagen(ItemStack stack) {
        String id = stack.get(ModDataComponents.TRAIT_ID.get());
        if (id == null) {
            return null;
        }
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            if (trait.id().equals(id)) {
                return SymbioticMutagen.fromTrait(trait);
            }
        }
        return SymbioticMutagen.fromId(id);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            SymbioticMutagen mutagen = getMutagen(stack);
            if (mutagen == null) {
                mutagen = SymbioticMutagen.CHITIN_THORNS;
            }
            PlayerSymbiosisData data = serverPlayer.getData(ModAttachments.SYMBIOSIS);
            if (data.hasMutagen(mutagen)) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.mutagen_already_implanted",
                        MycelialText.mutagen(mutagen)
                ).withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (data.equipMutagen(mutagen)) {
                SymbiosisAttributeSync.sync(serverPlayer);
                SymbiosisSyncPayload.syncToPlayer(serverPlayer);
                level.playSound(null, serverPlayer.blockPosition(), ModSounds.SYMBIOTIC_MUTAGEN_IMPLANT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.mutagen_implanted",
                        MycelialText.mutagen(mutagen),
                        data.getEquippedCount(),
                        MycelialConfig.MAX_MUTAGEN_SLOTS.get()
                ).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (!serverPlayer.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            } else {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.mycelialmind.mutagen_slots_full",
                        MycelialConfig.MAX_MUTAGEN_SLOTS.get()
                ).withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SymbioticMutagen mutagen = getMutagen(stack);
        if (mutagen != null) {
            tooltip.add(Component.translatable("tooltip.mycelialmind.mutagen_type", MycelialText.mutagen(mutagen))
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
