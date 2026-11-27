package com.alex.mycelialmind.item;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.SynapticMemoryRouter;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Consumable antidote that clears spore-induced debuffs and grants brief regeneration.
 */
public class SporeAntidoteItem extends AbstractAntidoteItem {
    public SporeAntidoteItem(Properties properties) {
        super(properties, false, 100, 1, 100, 0, 1.05F, "tooltip.mycelialmind.spore_antidote");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            applyBaseSelfCure(serverLevel, player);
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
            serverLevel.playSound(null, player.blockPosition(), ModSounds.SPORE_ANTIDOTE_CURE.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
            if (!serverLevel.getEntitiesOfClass(MycelialMob.class, player.getBoundingBox().inflate(16.0D)).isEmpty()) {
                SynapticMemoryRouter.routeHabit(
                        serverLevel, player.blockPosition(), PlayerHabit.COMBAT_MEDIC, 2.5);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
