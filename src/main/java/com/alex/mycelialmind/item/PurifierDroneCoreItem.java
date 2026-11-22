package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Deployable item that summons an allied Purifier Drone bound to the player.
 */
public class PurifierDroneCoreItem extends Item {
    public PurifierDroneCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
            PurifierDroneEntity drone = ModEntities.PURIFIER_DRONE.get().create(serverLevel);
            if (drone != null) {
                drone.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, player.getYRot(), 0.0F);
                drone.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null);
                if (!serverLevel.noCollision(drone) || !serverLevel.addFreshEntity(drone)) {
                    return InteractionResult.FAIL;
                }
                drone.setOwner(player.getUUID());
                serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                        spawnPos.getX() + 0.5, spawnPos.getY() + 0.8, spawnPos.getZ() + 0.5,
                        18, 0.4, 0.4, 0.4, 0.04);
                serverLevel.playSound(null, spawnPos, ModSounds.PURIFIER_DRONE_DEPLOY.get(), SoundSource.PLAYERS, 0.95F, 1.0F);
                if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
                    AdvancementHolder adv = serverPlayer.getServer().getAdvancements()
                            .get(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "purifier_drone_deployed"));
                    if (adv != null) {
                        serverPlayer.getAdvancements().award(adv, "deploy_drone");
                    }
                }
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.purifier_drone_core").withStyle(ChatFormatting.AQUA));
    }
}
