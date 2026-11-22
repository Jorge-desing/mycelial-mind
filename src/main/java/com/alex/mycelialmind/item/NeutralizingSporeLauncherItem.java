package com.alex.mycelialmind.item;

import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Launches neutralizing spores that strip active mutations from MycelialMobs.
 * Shift+Right-Click fires a 3-spore fan burst.
 */
public class NeutralizingSporeLauncherItem extends Item {
    public NeutralizingSporeLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean burst = player.isShiftKeyDown();
        if (!level.isClientSide()) {
            if (burst) {
                int spawned = 0;
                for (float yawOffset : new float[]{-9.0F, 0.0F, 9.0F}) {
                    NeutralizingSporeProjectile spore = new NeutralizingSporeProjectile(level, player);
                    spore.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0F, 1.85F, 1.0F);
                    if (level.addFreshEntity(spore)) {
                        spawned++;
                    }
                }
                if (spawned == 0) {
                    return InteractionResultHolder.fail(stack);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SPORE_LAUNCHER_FIRE.get(), SoundSource.PLAYERS, 0.9F, 0.9F);
                stack.hurtAndBreak(spawned, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                player.getCooldowns().addCooldown(this, 36);
            } else {
                NeutralizingSporeProjectile spore = new NeutralizingSporeProjectile(level, player);
                spore.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.8F, 1.0F);
                if (!level.addFreshEntity(spore)) {
                    return InteractionResultHolder.fail(stack);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SPORE_LAUNCHER_FIRE.get(), SoundSource.PLAYERS, 0.85F, 1.05F);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                player.getCooldowns().addCooldown(this, 20);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.CHITIN_PLATE.get()) || repair.is(ModItems.MYCELIAL_BIOMASS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.spore_launcher").withStyle(ChatFormatting.AQUA));
    }
}
