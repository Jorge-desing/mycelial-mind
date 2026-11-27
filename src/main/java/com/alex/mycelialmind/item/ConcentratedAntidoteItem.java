package com.alex.mycelialmind.item;

import com.alex.mycelialmind.network.SymbiosisSyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Concentrated biological purge that cures Mycelial Infection, clears debuffs,
 * and purges all implanted Symbiotic Mutagens so new ones can be equipped.
 */
public class ConcentratedAntidoteItem extends AbstractAntidoteItem {
    public ConcentratedAntidoteItem(Properties properties) {
        super(properties, true, 140, 1, 140, 1, 1.2F, "tooltip.mycelialmind.concentrated_antidote");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            applyBaseSelfCure(serverLevel, serverPlayer);

            serverPlayer.getData(ModAttachments.SYMBIOSIS).clearMutagens();
            SymbiosisAttributeSync.sync(serverPlayer);
            SymbiosisSyncPayload.syncToPlayer(serverPlayer);

            level.playSound(null, serverPlayer.blockPosition(), ModSounds.SPORE_ANTIDOTE_CURE.get(), SoundSource.PLAYERS, 1.0F, 1.15F);
            serverPlayer.sendSystemMessage(Component.translatable("message.mycelialmind.symbiosis_purged")
                    .withStyle(ChatFormatting.GREEN));

            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
