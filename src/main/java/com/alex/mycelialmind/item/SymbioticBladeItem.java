package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HyphalSiegeDirector;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Symbiotic blade that deals amplified damage when the Hive Mind has multiple active traits,
 * and unleashes a mycelial arc slash on right-click that heals on hit.
 */
public class SymbioticBladeItem extends SwordItem {
    public SymbioticBladeItem(Properties properties) {
        super(Tiers.DIAMOND, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result && target instanceof MycelialMob mob && target.level() instanceof ServerLevel serverLevel) {
            int activeCount = HiveMindSavedData.get(serverLevel).getActiveTraits().size();
            if (activeCount >= 2) {
                mob.hurt(mob.damageSources().playerAttack(attacker instanceof Player p ? p : null),
                        Math.min(5.0F, 1.25F * activeCount));
            }
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle().normalize();
            int activeCount = HiveMindSavedData.get(serverLevel).getActiveTraits().size();
            float arcDamage = 4.0F + Math.min(4, activeCount) * 0.75F;
            boolean hitAny = false;

            for (MycelialMob mob : serverLevel.getEntitiesOfClass(
                    MycelialMob.class, player.getBoundingBox().inflate(8.0D),
                    m -> m.isAlive() && !MycelialMob.isFriendlyToPlayer(m, player))) {
                Vec3 toTarget = mob.position().add(0, mob.getBbHeight() * 0.5, 0)
                        .subtract(player.getEyePosition()).normalize();
                if (look.dot(toTarget) >= 0.45D) {
                    mob.hurt(player.damageSources().playerAttack(player), arcDamage);
                    hitAny = true;
                }
            }
            BlockPos eyePos = BlockPos.containing(player.getEyePosition());
            boolean severedVein = false;
            for (BlockPos p : BlockPos.betweenClosed(eyePos.offset(-3, -2, -3), eyePos.offset(3, 2, 3))) {
                BlockState stateAtP = serverLevel.getBlockState(p);
                if (stateAtP.is(ModBlocks.EPHEMERAL_CHITIN.get())) {
                    Vec3 toBlock = Vec3.atCenterOf(p).subtract(player.getEyePosition());
                    double dSq = toBlock.lengthSqr();
                    if (dSq > 1.0E-4D && dSq <= 10.5D && look.dot(toBlock.normalize()) >= 0.45D) {
                        serverLevel.removeBlock(p, false);
                    }
                } else if (!severedVein && (stateAtP.is(ModBlocks.HYPHAL_VEIN.get())
                        || stateAtP.is(ModBlocks.CREEPING_MYCELIUM.get())
                        || stateAtP.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get()))) {
                    Vec3 toBlock = Vec3.atCenterOf(p).subtract(player.getEyePosition());
                    double dSq = toBlock.lengthSqr();
                    if (dSq > 1.0E-4D && dSq <= 12.25D && look.dot(toBlock.normalize()) >= 0.40D) {
                        if (HyphalSiegeDirector.trySeverHyphalVein(serverLevel, p.immutable(), player, stack) > 0) {
                            severedVein = true;
                        }
                    }
                }
            }
            if (hitAny) {
                player.heal(1.5F);
            }
            serverLevel.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                    player.getX() + look.x * 2.0, player.getY(0.6), player.getZ() + look.z * 2.0,
                    3, 0.5, 0.2, 0.5, 0.0);
            serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(),
                    player.getX() + look.x * 3.0, player.getY(0.6), player.getZ() + look.z * 3.0,
                    14, 1.0, 0.4, 1.0, 0.04);
            serverLevel.playSound(null, player.blockPosition(), ModSounds.SYMBIOTIC_BLADE_SLASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(this, 110);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.MYCELIAL_BIOMASS.get())
                || repair.is(ModItems.SYNAPTIC_CORE.get())
                || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.symbiotic_blade").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
