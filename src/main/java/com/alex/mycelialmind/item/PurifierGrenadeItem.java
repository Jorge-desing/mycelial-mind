package com.alex.mycelialmind.item;

import com.alex.mycelialmind.entity.projectile.PurifierGrenadeProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Throwable grenade that purifies nearby mycelial blocks and strips mob traits.
 */
public class PurifierGrenadeItem extends Item {
    public PurifierGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            PurifierGrenadeProjectile grenade = new PurifierGrenadeProjectile(level, player);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.9F, 1.0F);
            if (!level.addFreshEntity(grenade)) {
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.PURIFIER_GRENADE_THROW.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.purifier_grenade").withStyle(ChatFormatting.AQUA));
    }
}
