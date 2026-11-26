package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModDamageTypes;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Armor-piercing biological lance that bypasses chitinous armor on MycelialMobs
 * and performs a piercing dash lunge on right-click.
 */
public class EnzymeLanceItem extends SwordItem {
    public EnzymeLanceItem(Properties properties) {
        super(Tiers.IRON, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result && target instanceof MycelialMob mob && !target.level().isClientSide()) {
            mob.setPhalanxActive(false);
            mob.hurt(ModDamageTypes.source(target.level(), ModDamageTypes.BIO_ENZYME, attacker, attacker), 1.5F);
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.15D, look.z).normalize().scale(1.15D);
        player.setDeltaMovement(dash);
        player.hurtMarked = true;

        if (level instanceof ServerLevel serverLevel) {
            for (MycelialMob mob : serverLevel.getEntitiesOfClass(
                    MycelialMob.class, player.getBoundingBox().inflate(3.5D),
                    m -> m.isAlive() && !MycelialMob.isFriendlyToPlayer(m, player))) {
                mob.setPhalanxActive(false);
                mob.hurt(ModDamageTypes.source(serverLevel, ModDamageTypes.BIO_ENZYME, player, player), 4.0F);
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
            serverLevel.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                    player.getX(), player.getY(0.5), player.getZ(),
                    18, 0.5, 0.3, 0.5, 0.08);
            serverLevel.playSound(null, player.blockPosition(), ModSounds.ENZYME_LANCE_THRUST.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            player.getCooldowns().addCooldown(this, 110);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.CHITIN_PLATE.get())
                || repair.is(ModItems.MYCELIAL_BIOMASS.get())
                || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.enzyme_lance").withStyle(ChatFormatting.GOLD));
    }
}
