package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.MycelialNodeBlock;
import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extracts typed GeneticSample items from adapted MycelialMobs or active Synaptic Nodes.
 */
public class BioExtractorItem extends Item {
    public BioExtractorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof MycelialMob mob)) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResult.FAIL;
            }
            if (mob.getHealth() > mob.getMaxHealth() * 0.60F) {
                player.sendSystemMessage(Component.translatable("message.mycelialmind.extractor_target_too_strong")
                        .withStyle(ChatFormatting.YELLOW));
                return InteractionResult.FAIL;
            }
            List<AdaptationTrait> active = new ArrayList<>(mob.getTraits());
            boolean primordial = mob.isPrimordialVariant();
            if (active.isEmpty() && primordial) {
                active.add(findDominantTrait(HiveMindSavedData.get(serverLevel)));
            }
            if (active.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.mycelialmind.extractor_no_traits")
                        .withStyle(ChatFormatting.YELLOW));
                return InteractionResult.FAIL;
            }
            AdaptationTrait sampled = active.get(mob.getRandom().nextInt(active.size()));
            int count = (mob.getEvolutionRank() == EvolutionRank.ELITE ? 2 : 1) + (primordial ? 1 : 0);
            giveSample(player, sampled, count);
            if (primordial) {
                ItemStack extraSpore = new ItemStack(ModItems.LATENT_SPORE.get(), 1);
                if (!player.getInventory().add(extraSpore)) {
                    player.drop(extraSpore, false);
                }
                ItemStack extraBiomass = new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), 1);
                if (!player.getInventory().add(extraBiomass)) {
                    player.drop(extraBiomass, false);
                }
                player.sendSystemMessage(Component.translatable("message.mycelialmind.extractor_primordial_bonus")
                        .withStyle(ChatFormatting.GOLD));
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
                    discovery.recordSpecies(mob.getType());
                    discovery.recordStrain(mob.getHiveStrain());
                    discovery.incrementRareVariantsFound();
                    PlayerDiscoveryData.unlockSecret(serverPlayer, ProgressionGoalEngine.SECRET_PRIMORDIAL_SPECIMEN);
                    PlayerDiscoveryData.onLatentSporeAcquired(serverPlayer);
                    DiscoverySyncPayload.syncToPlayer(serverPlayer);
                }
            }
            serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(), mob.getX(), mob.getY(0.5), mob.getZ(),
                    12, 0.3, 0.4, 0.3, 0.04);
            serverLevel.playSound(null, mob.blockPosition(), ModSounds.BIO_EXTRACTOR_DRAW.get(), SoundSource.PLAYERS, 0.9F, 1.2F);
            player.getCooldowns().addCooldown(this, primordial ? 20 : 40);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        if (context.getLevel() instanceof ServerLevel serverLevel) {
            BlockState clickedState = serverLevel.getBlockState(pos);
            if (clickedState.is(ModBlocks.ANTIBODY_CYST.get())
                    && clickedState.hasProperty(AntibodyCystBlock.ACTIVE)
                    && clickedState.getValue(AntibodyCystBlock.ACTIVE)) {
                if (player.getCooldowns().isOnCooldown(this)) {
                    return InteractionResult.FAIL;
                }
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                AdaptationTrait dominant = findDominantTrait(hive);
                giveSample(player, dominant, 1);
                serverLevel.setBlockAndUpdate(pos, clickedState.setValue(AntibodyCystBlock.ACTIVE, false));
                serverLevel.updateNeighbourForOutputSignal(pos, clickedState.getBlock());
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        12, 0.3, 0.3, 0.3, 0.04);
                serverLevel.playSound(null, pos, ModSounds.BIO_EXTRACTOR_DRAW.get(), SoundSource.PLAYERS, 0.9F, 1.3F);
                player.getCooldowns().addCooldown(this, 60);
                context.getItemInHand().hurtAndBreak(1, player,
                        context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                return InteractionResult.CONSUME;
            }
            if (clickedState.getBlock() instanceof MycelialNodeBlock) {
                if (player.getCooldowns().isOnCooldown(this)) {
                    return InteractionResult.FAIL;
                }
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                var activeTraits = hive.getActiveTraits();
                if (activeTraits.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.mycelialmind.extractor_node_dormant")
                            .withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.FAIL;
                }
                AdaptationTrait dominant = null;
                double dominantScore = Double.NEGATIVE_INFINITY;
                Map<StimulusType, Double> stimulusScores = hive.getStimulusScores();
                for (AdaptationTrait trait : activeTraits) {
                    double score = stimulusScores.getOrDefault(trait.trigger(), 0.0);
                    if (score > dominantScore) {
                        dominant = trait;
                        dominantScore = score;
                    }
                }
                if (dominant == null) {
                    return InteractionResult.FAIL;
                }
                giveSample(player, dominant, 1);
                if (!(clickedState.getBlock() instanceof SymbioticNodeBlock)) {
                    PurificationZoneTracker.get(serverLevel).alertNearbyNodes(serverLevel, pos, 160);
                }
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        12, 0.3, 0.3, 0.3, 0.04);
                serverLevel.playSound(null, pos, ModSounds.BIO_EXTRACTOR_DRAW.get(), SoundSource.PLAYERS, 0.9F, 1.1F);
                player.getCooldowns().addCooldown(this, 60);
                context.getItemInHand().hurtAndBreak(1, player,
                        context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    private static AdaptationTrait findDominantTrait(HiveMindSavedData hive) {
        StimulusType best = StimulusType.MELEE;
        double bestScore = -1.0;
        for (Map.Entry<StimulusType, Double> entry : hive.getStimulusScores().entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                best = entry.getKey();
            }
        }
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            if (trait.trigger() == best) {
                return trait;
            }
        }
        return AdaptationTrait.MELEE_THORNS;
    }

    private static void giveSample(Player player, AdaptationTrait trait, int count) {
        ItemStack sample = new ItemStack(ModItems.GENETIC_SAMPLE.get(), count);
        sample.set(ModDataComponents.TRAIT_ID.get(), trait.id());
        if (!player.getInventory().add(sample)) {
            player.drop(sample, false);
        }
        player.sendSystemMessage(Component.translatable("message.mycelialmind.sample_extracted", MycelialText.trait(trait))
                .withStyle(ChatFormatting.AQUA));
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
            discovery.recordTrait(trait);
            discovery.recordMilestone(ProgressionGoalEngine.MILESTONE_BIO_EXTRACTOR);
            if (trait == AdaptationTrait.PROJECTILE_DEFLECT) {
                discovery.markResearchBit(ProgressionGoalEngine.RESEARCH_BIT_SAMPLE_EXTRACTED);
                discovery.markResearchBit(ProgressionGoalEngine.RESEARCH_BIT_ADAPTATION_IDENTIFIED);
            }
            DiscoverySyncPayload.syncToPlayer(serverPlayer);
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.CHITIN_PLATE.get()) || repair.is(ModItems.MYCELIAL_BIOMASS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.bio_extractor").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
