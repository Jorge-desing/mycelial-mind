package com.alex.mycelialmind.item;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.network.DiplomacySyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Injects a rival HiveStrain into a Synaptic Node or MycelialMob, sparking a local
 * 35s Synaptic Schism (16m radius, does not affect bosses) and raising strain reputation.
 */
public class SchismInjectorItem extends Item {
    public static final int SCHISM_DURATION_TICKS = 700;
    public static final int COOLDOWN_TICKS = 400;

    public SchismInjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                PlayerDiplomacyData dip = sp.getData(ModAttachments.DIPLOMACY);
                dip.toggleSelectedStrain();
                DiplomacySyncPayload.syncToPlayer(sp);
                sp.displayClientMessage(Component.translatable("message.mycelialmind.strain_selected",
                        MycelialText.strain(dip.getSelectedStrain())).withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof MycelialMob mob) || !mob.isAlive() || mob.isBossMob() || mob.isAlliedEscort()) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        if (player.level() instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            PlayerDiplomacyData dip = sp.getData(ModAttachments.DIPLOMACY);
            HiveStrain chosen = dip.getSelectedStrain();
            List<MycelialMob> nearby = serverLevel.getEntitiesOfClass(MycelialMob.class, new AABB(mob.blockPosition()).inflate(16.0D));
            int injectedCount = 0;
            int nearbyEligibleIndex = 0;
            boolean primaryInjected = false;
            for (MycelialMob m : nearby) {
                if (!m.isAlive() || m.isAlliedEscort() || m.isBossMob()) {
                    continue;
                }
                HiveStrain strain = chosen;
                if (m != mob && nearbyEligibleIndex++ % 3 == 2) {
                    strain = chosen.oppositeSchism();
                }
                m.infectWithSchism(strain, SCHISM_DURATION_TICKS);
                injectedCount++;
                primaryInjected |= m == mob;
            }
            if (!primaryInjected) {
                mob.infectWithSchism(chosen, SCHISM_DURATION_TICKS);
                injectedCount++;
            }
            com.alex.mycelialmind.compat.curios.MycelialCuriosCompat.adjustReputationWithSeal(dip, sp, chosen, 8);
            DiplomacySyncPayload.syncToPlayer(sp);

            serverLevel.sendParticles(ColorParticleOption.create(ModParticles.HIVE_DUST.get(), chosen.argbColor()),
                    mob.getX(), mob.getY(0.5), mob.getZ(), 24, 0.6, 0.6, 0.6, 0.04);
            serverLevel.playSound(null, mob.blockPosition(), ModSounds.SCHISM_INJECTOR_STING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            stack.hurtAndBreak(1, sp, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            sp.displayClientMessage(Component.translatable("message.mycelialmind.schism_injected",
                    MycelialText.strain(chosen), injectedCount).withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof SynapticNodeBlockEntity nodeBe) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
                PlayerDiplomacyData dip = sp.getData(ModAttachments.DIPLOMACY);
                HiveStrain chosen = dip.getSelectedStrain();
                nodeBe.induceSchism(chosen, SCHISM_DURATION_TICKS);
                com.alex.mycelialmind.compat.curios.MycelialCuriosCompat.adjustReputationWithSeal(dip, sp, chosen, 8);
                DiplomacySyncPayload.syncToPlayer(sp);

                serverLevel.sendParticles(ColorParticleOption.create(ModParticles.HIVE_DUST.get(), chosen.argbColor()),
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 24, 0.6, 0.6, 0.6, 0.05);
                serverLevel.playSound(null, pos, ModSounds.SCHISM_INJECTOR_STING.get(), SoundSource.PLAYERS, 1.0F, 0.85F);
                sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                context.getItemInHand().hurtAndBreak(1, sp,
                        context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                sp.displayClientMessage(Component.translatable("message.mycelialmind.schism_node_injected",
                        MycelialText.strain(chosen)).withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.SYNAPTIC_CORE.get()) || repair.is(ModItems.MYCELIAL_BIOMASS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.schism_injector").withStyle(ChatFormatting.GOLD));
    }
}
