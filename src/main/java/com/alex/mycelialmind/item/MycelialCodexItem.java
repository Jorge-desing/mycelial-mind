package com.alex.mycelialmind.item;

import com.alex.mycelialmind.client.screen.ClientCodexOpener;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Physical standalone Codex book item opening the native MycelialCodexBookScreen.
 */
public class MycelialCodexItem extends Item {
    public MycelialCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, HiveSyncPayload.forPlayer(serverLevel, serverPlayer));
            DiscoverySyncPayload.syncToPlayer(serverPlayer);
            serverLevel.playSound(null, player.blockPosition(), ModSounds.BIO_SCANNER_PING.get(),
                    SoundSource.PLAYERS, 0.55F, 1.35F);
        } else if (level.isClientSide()) {
            ClientCodexOpener.openBookCodex();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.mycelial_codex").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
