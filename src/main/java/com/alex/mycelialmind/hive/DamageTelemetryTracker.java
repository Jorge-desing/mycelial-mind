package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.compat.combat.ModdedDamageClassifier;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.entity.projectile.PurifierGrenadeProjectile;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.network.DiplomacySyncPayload;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.symbiosis.PlayerSymbiosisData;
import com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Observes combat against MycelialMob organisms, feeds HiveMindSavedData,
 * enforces Tier 1/Tier 2 traits & HiveSynergies, and handles active node-destruction bleed.
 */
public final class DamageTelemetryTracker {
    private static final int DECAY_INTERVAL_TICKS = 1200; // 1 in-game minute

    private DamageTelemetryTracker() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || !(event.getAmount() > 0.0F) || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            handlePlayerIncomingDamage(event, player);
            return;
        }
        if (event.getEntity() instanceof ColonySymbiontEntity) {
            if (isAlliedFriendlyFireSource(event.getSource())) {
                event.setCanceled(true);
            }
            return;
        }
        if (!(event.getEntity() instanceof MycelialMob mob)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.getDirectEntity() instanceof NeutralizingSporeProjectile
                || source.getDirectEntity() instanceof PurifierGrenadeProjectile
                || (source.getEntity() instanceof Player supPlayer
                        && (supPlayer.getMainHandItem().is(ModItems.SYNAPTIC_SUPPRESSOR.get())
                                || supPlayer.getOffhandItem().is(ModItems.SYNAPTIC_SUPPRESSOR.get())))) {
            mob.triggerDesync(SynapticCommandSystem.DESYNC_DURATION_TICKS);
        }

        if (mob.isPhalanxActive()
                && !mob.isDesynced()
                && !source.is(DamageTypeTags.BYPASSES_ARMOR)
                && !source.is(DamageTypeTags.IS_EXPLOSION)
                && source.getEntity() != null
                && SynapticCommandSystem.isPhalanxFrontHit(mob.getLookAngle(), source.getEntity().position(), mob.position())) {
            event.setAmount(event.getAmount() * (float) SynapticCommandSystem.PHALANX_FRONT_DAMAGE_MULTIPLIER);
        }

        if (source.getEntity() instanceof Player attacker && ChitinArmorItem.hasFullSet(attacker)) {
            boolean hasMutagen = attacker.getData(ModAttachments.SYMBIOSIS).getEquippedCount() > 0;
            float chitinMult = mob.isBossMob() ? 1.08F : (hasMutagen ? 1.15F : 1.08F);
            event.setAmount(event.getAmount() * chitinMult);
        }

        if (mob.level() instanceof ServerLevel sl) {
            HiveMindSavedData hive = HiveMindSavedData.get(sl);
            if (source.getEntity() instanceof Player attacker
                    && attacker.hasEffect(ModEffects.SYNAPTIC_INSIGHT)
                    && !hive.getActiveTraits().isEmpty()) {
                event.setAmount(event.getAmount() * (mob.isBossMob() ? 1.10F : 1.15F));
            }
            long now = sl.getGameTime();
            if (hive.isDeceptionActive(now)) {
                StimulusType incomingType = classifyDamage(source);
                if (incomingType != null) {
                    if (incomingType == hive.getFalseStimulus()) {
                        hive.revealDeception(now);
                        if (source.getEntity() instanceof Player p) {
                            p.displayClientMessage(
                                    Component.translatable("message.mycelialmind.deception_revealed",
                                            MycelialText.stimulus(incomingType)).withStyle(ChatFormatting.RED),
                                    true
                            );
                        }
                    } else {
                        event.setAmount(event.getAmount() * (mob.isBossMob() ? 1.10F : 1.25F));
                    }
                }
            }
        }

        if (mob.hasTrait(AdaptationTrait.FIRE_RESISTANT) && source.is(DamageTypeTags.IS_FIRE)) {
            mob.clearFire();
            if (mob.level() instanceof ServerLevel sl) {
                HiveMindSavedData.get(sl).reinforceTraitMastery(AdaptationTrait.FIRE_RESISTANT, 0.05);
            }
            if (mob.hasTier2Trait(AdaptationTrait.FIRE_RESISTANT)) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, false));
                if (mob.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ModParticles.HIVE_EMBER.get(), mob.getX(), mob.getY() + 0.3, mob.getZ(),
                            8, 0.35, 0.2, 0.35, 0.03);
                }
            }
            event.setCanceled(true);
            return;
        }

        if (mob.hasTrait(AdaptationTrait.PROJECTILE_DEFLECT) && source.is(DamageTypeTags.IS_PROJECTILE)) {
            double mastery = mob.level() instanceof ServerLevel sl
                    ? HiveMindSavedData.get(sl).getTraitMastery(AdaptationTrait.PROJECTILE_DEFLECT)
                    : 0.0;
            if (mob.level() instanceof ServerLevel sl) {
                HiveMindSavedData.get(sl).reinforceTraitMastery(AdaptationTrait.PROJECTILE_DEFLECT, 0.05);
            }
            double chance = Math.min(0.75, MycelialConfig.PROJECTILE_DEFLECT_CHANCE.get() + mastery * 0.12);
            if (mob.getRandom().nextDouble() < chance) {
                if (mob.level() instanceof ServerLevel sl) {
                    emitProjectileDeflectionSignal(sl, mob, source);
                }
                if (mob.hasSynergy(HiveSynergy.KINETIC_MIRROR) && source.getEntity() instanceof LivingEntity attacker && attacker != mob) {
                    reflectSynapticFragment(mob, attacker);
                }
                event.setCanceled(true);
                return;
            }
            event.setAmount(event.getAmount() * (0.72F - 0.12F * (float) mastery));
        }

        if (mob.hasTrait(AdaptationTrait.BLAST_SHIELDED) && source.is(DamageTypeTags.IS_EXPLOSION)) {
            double mastery = 0.0;
            if (mob.level() instanceof ServerLevel sl) {
                HiveMindSavedData h = HiveMindSavedData.get(sl);
                mastery = h.getTraitMastery(AdaptationTrait.BLAST_SHIELDED);
                h.reinforceTraitMastery(AdaptationTrait.BLAST_SHIELDED, 0.05);
            }
            event.setAmount(event.getAmount() * (0.65F - 0.12F * (float) mastery));
            if (mob.hasSynergy(HiveSynergy.PYROLYTIC_SHELL) && mob.level() instanceof ServerLevel serverLevel) {
                triggerPyrolyticPulse(serverLevel, mob);
            }
        }

        if (mob.hasTrait(AdaptationTrait.MAGIC_CARAPACE) && source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            double mastery = 0.0;
            if (mob.level() instanceof ServerLevel sl) {
                HiveMindSavedData h = HiveMindSavedData.get(sl);
                mastery = h.getTraitMastery(AdaptationTrait.MAGIC_CARAPACE);
                h.reinforceTraitMastery(AdaptationTrait.MAGIC_CARAPACE, 0.05);
            }
            float rawAmount = event.getAmount();
            event.setAmount(rawAmount * (0.68F - 0.12F * (float) mastery));
            if (mob.hasSynergy(HiveSynergy.ARCANE_ABSORPTION)) {
                for (MycelialMob ally : mob.level().getEntitiesOfClass(MycelialMob.class, mob.getBoundingBox().inflate(8.0D))) {
                    if (ally.isAlive() && !mob.isRivalStrainTarget(ally)) {
                        ally.heal(Math.min(3.0F, rawAmount * 0.2F));
                    }
                }
            }
            if (mob.hasTier2Trait(AdaptationTrait.MAGIC_CARAPACE)
                    && source.getEntity() instanceof LivingEntity attacker
                    && attacker != mob) {
                attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            }
        }
    }

    @SubscribeEvent
    public static void onPostDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            if (event.getNewDamage() > 0.0F && event.getSource().getEntity() instanceof MycelialMob attackerMob
                    && player.level() instanceof ServerLevel sl) {
                com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.addCombatXp(sl, attackerMob, null, 20);
            }
            handlePlayerPostDamage(event, player);
            return;
        }
        if (!(event.getEntity() instanceof MycelialMob mob)) {
            if (event.getEntity() instanceof Mob vanillaMob
                    && !(vanillaMob instanceof ColonySymbiontEntity)
                    && vanillaMob.level() instanceof ServerLevel serverLevel) {
                PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
                if (event.getSource().getEntity() instanceof MycelialMob attackerMob) {
                    if (event.getNewDamage() > 0.0F) {
                        com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.addCombatXp(serverLevel, attackerMob, null, 20);
                    }
                    if (!tracker.isProtected(vanillaMob.blockPosition())) {
                        vanillaMob.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 200, 0));
                    }
                }
                if (MycelialConfig.INFECTION_CONVERSION_ENABLED.get()
                        && vanillaMob.isAlive()
                        && vanillaMob.hasEffect(ModEffects.MYCELIAL_INFECTION)
                        && vanillaMob.getHealth() <= vanillaMob.getMaxHealth() * 0.25F) {
                    tryConvertInfectedMob(serverLevel, vanillaMob, true);
                }
            }
            return;
        }
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float dealt = event.getNewDamage();
        if (dealt <= 0.0F) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker && !mob.isBossMob()) {
            PlayerDiplomacyData dip = attacker.getData(ModAttachments.DIPLOMACY);
            var standing = dip.getStanding(mob.getHiveStrain());
            boolean attackedAlly = mob.isAlliedEscort() && attacker.getUUID().equals(mob.getAlliedOwnerUuid());
            if (dip.getBetrayalCooldownTicks() <= 0
                    && (attackedAlly || dip.getTruceTicks() > 0
                    || standing == PlayerDiplomacyData.Standing.PACT
                    || standing == PlayerDiplomacyData.Standing.ALLIED)) {
                com.alex.mycelialmind.compat.curios.MycelialCuriosCompat.triggerBetrayalWithSeal(
                        dip, attacker, mob.getHiveStrain());
                DiplomacySyncPayload.syncToPlayer(attacker);
                attacker.sendSystemMessage(Component.translatable("message.mycelialmind.diplomacy_betrayal",
                        MycelialText.strain(mob.getHiveStrain())).withStyle(ChatFormatting.RED));
            }
        }
        StimulusType stimulus = classifyDamage(source);
        if (stimulus != null) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            Set<AdaptationTrait> prevT2 = hive.getTier2Traits().isEmpty()
                    ? EnumSet.noneOf(AdaptationTrait.class)
                    : EnumSet.copyOf(hive.getTier2Traits());
            Set<HiveSynergy> prevSyn = hive.getActiveSynergies().isEmpty()
                    ? EnumSet.noneOf(HiveSynergy.class)
                    : EnumSet.copyOf(hive.getActiveSynergies());

            double relayMult = PurificationZoneTracker.get(serverLevel)
                    .hasNearbyUnprotectedNode(mob.blockPosition(), SynapticNodeType.RELAY, 24) ? 1.5 : 1.0;
            double symbiosisMult = source.getEntity() instanceof Player attacker
                    ? attacker.getData(ModAttachments.SYMBIOSIS).getThreatAmplifier()
                    : 1.0;
            SynapticMemoryRouter.routeStimulus(serverLevel, mob.blockPosition(), stimulus, dealt * relayMult * symbiosisMult);
            if (dealt >= 4.0F) {
                hive.recordCombatHotspot(mob.blockPosition(), Math.min(25.0, dealt * 1.5), serverLevel.getGameTime(), stimulus);
            }
            if (mob.isAlive()) {
                com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge.addCombatXp(serverLevel, mob, stimulus, 15);
                if (mob.getHealth() <= mob.getMaxHealth() * 0.40F
                        && source.getEntity() instanceof LivingEntity attacker) {
                    HiveSwarmCoordinator.broadcastAlertCall(serverLevel, mob, attacker);
                }
            }

            notifyNewEvolutions(serverLevel, mob.blockPosition(), prevT2, prevSyn, hive);
            notifyTierAdvancement(serverLevel, hive);
        }

        if (stimulus == StimulusType.MELEE && mob.hasTrait(AdaptationTrait.MELEE_THORNS)) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            double mastery = hive.getTraitMastery(AdaptationTrait.MELEE_THORNS);
            hive.reinforceTraitMastery(AdaptationTrait.MELEE_THORNS, 0.05);
            if (source.getEntity() instanceof LivingEntity attacker && attacker != mob) {
                attacker.hurt(mob.damageSources().thorns(mob),
                        Math.max(1.0F, dealt * 0.25F * (1.0F + 0.15F * (float) mastery)));
                if (mob.hasSynergy(HiveSynergy.VENOMOUS_COUNTER)) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                    attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                    mob.heal(1.5F);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player deadPlayer
                && deadPlayer.level() instanceof ServerLevel serverLevel) {
            double penalty = MycelialConfig.DEATH_THREAT_PENALTY.get();
            if (penalty > 0.0) {
                boolean killedByHive = event.getSource().getEntity() instanceof MycelialMob
                        || deadPlayer.getLastHurtByMob() instanceof MycelialMob
                        || deadPlayer.hasEffect(ModEffects.MYCELIAL_INFECTION)
                        || deadPlayer.hasEffect(ModEffects.SPORE_ROT);
                if (!killedByHive) {
                    BlockPos nearestNode = PurificationZoneTracker.get(serverLevel).findNearestEnemyNode(deadPlayer.blockPosition());
                    killedByHive = nearestNode != null && nearestNode.distSqr(deadPlayer.blockPosition()) <= 256.0;
                }
                if (killedByHive) {
                    HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                    hive.addThreatPenalty(penalty);
                    deadPlayer.sendSystemMessage(Component.translatable("message.mycelialmind.hive_fed_on_death",
                            String.format("%.1f", penalty)).withStyle(ChatFormatting.DARK_RED));
                    notifyTierAdvancement(serverLevel, hive);
                }
            }
            return;
        }
        if (event.getEntity() instanceof MycelialMob deadMob && deadMob.level() instanceof ServerLevel serverLevel) {
            if (!deadMob.isBossMob()) {
                HiveBlackboard.recordSquadCasualty(serverLevel, deadMob.blockPosition());
                if (deadMob.getCommandRole() == SynapticCommandSystem.CommandRole.LEADER) {
                    SynapticCommandSystem.propagateLeaderDesync(
                            serverLevel, deadMob, SynapticCommandSystem.DESYNC_DURATION_TICKS);
                }
            }
            boolean highTraumaCause = event.getSource().getDirectEntity() instanceof NeutralizingSporeProjectile
                    || event.getSource().getDirectEntity() instanceof PurifierGrenadeProjectile
                    || event.getSource().is(DamageTypeTags.IS_EXPLOSION);
            HiveBlackboard.getInfluenceMap(serverLevel).addTrauma(
                    deadMob.blockPosition(), highTraumaCause ? 0.45F : 0.30F);
            boolean isFire = event.getSource().is(DamageTypeTags.IS_FIRE) || deadMob.isOnFire();
            boolean inPylon = PurificationZoneTracker.get(serverLevel).isProtected(deadMob.blockPosition());
            if (LivingBattlefieldSystem.shouldDropCarrion(deadMob.isBossMob(), isFire, inPylon)) {
                HiveBlackboard.registerCarrion(
                        serverLevel, deadMob.position(), LivingBattlefieldSystem.CARRION_TTL_TICKS);
                serverLevel.sendParticles(ModParticles.NECROTIC_MIST.get(),
                        deadMob.getX(), deadMob.getY() + 0.35, deadMob.getZ(),
                        10, 0.3, 0.2, 0.3, 0.02);
            }
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            StimulusType cause = classifyDamage(event.getSource());
            if (cause != null) {
                hive.recordMobCasualty(cause);
            }
            hive.recordCombatHotspot(deadMob.blockPosition(), 12.0, serverLevel.getGameTime(),
                    cause != null ? cause : StimulusType.MELEE);
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                HiveSwarmCoordinator.broadcastAlertCall(serverLevel, deadMob, attacker);
            }
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                if (deadMob.getHiveStrain().isSchismStrain()) {
                    PlayerDiplomacyData dip = killer.getData(ModAttachments.DIPLOMACY);
                    dip.adjustReputation(deadMob.getHiveStrain(), -6);
                    com.alex.mycelialmind.compat.curios.MycelialCuriosCompat.adjustReputationWithSeal(
                            dip, killer, deadMob.getHiveStrain().oppositeSchism(), 2);
                    DiplomacySyncPayload.syncToPlayer(killer);
                }
                PlayerDiscoveryData discovery = killer.getData(ModAttachments.DISCOVERY);
                discovery.recordSpecies(deadMob.getType());
                discovery.recordStrain(deadMob.getHiveStrain());
                for (AdaptationTrait trait : deadMob.getTraits()) {
                    discovery.recordTrait(trait);
                }
                if (deadMob.isPrimordialVariant()) {
                    discovery.incrementRareVariantsFound();
                    PlayerDiscoveryData.unlockSecret(killer, ProgressionGoalEngine.SECRET_PRIMORDIAL_SPECIMEN);
                }
                if (deadMob instanceof HiveCoreBossEntity) {
                    hive.promoteHighestTier(HiveProgressionTier.AWAKENED);
                    discovery.recordMilestone(ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED);
                    for (ServerPlayer nearby : serverLevel.players()) {
                        if (nearby != killer && nearby.blockPosition().distSqr(deadMob.blockPosition()) <= 48.0D * 48.0D) {
                            PlayerDiscoveryData.unlockMilestone(nearby, ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED);
                        }
                    }
                }
                DiscoverySyncPayload.syncToPlayer(killer);
            } else if (deadMob instanceof HiveCoreBossEntity) {
                hive.promoteHighestTier(HiveProgressionTier.AWAKENED);
                for (ServerPlayer nearby : serverLevel.players()) {
                    if (nearby.blockPosition().distSqr(deadMob.blockPosition()) <= 48.0D * 48.0D) {
                        PlayerDiscoveryData.unlockMilestone(nearby, ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED);
                    }
                }
            }
            return;
        }
        if (!MycelialConfig.INFECTION_CONVERSION_ENABLED.get()) {
            return;
        }
        if (event.getEntity() instanceof Mob victim && victim.level() instanceof ServerLevel serverLevel) {
            if (victim.hasEffect(ModEffects.MYCELIAL_INFECTION)) {
                tryConvertInfectedMob(serverLevel, victim, false);
            }
        }
    }

    public static boolean isHiveCoreDefeated(ServerLevel level, @org.jetbrains.annotations.Nullable Player player) {
        if (level == null) {
            return false;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        HiveMindSavedData overworldHive = level.getServer() != null
                ? HiveMindSavedData.get(level.getServer().overworld())
                : hive;
        if (hive.isMatriarchDefeated() || overworldHive.isMatriarchDefeated()) {
            return true;
        }
        if (player instanceof ServerPlayer sp
                && sp.getData(ModAttachments.DISCOVERY).hasMilestone(ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED)) {
            return true;
        }
        if (level.getServer() != null) {
            for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
                if (sp.getData(ModAttachments.DISCOVERY).hasMilestone(ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void notifyTierAdvancement(ServerLevel level, HiveMindSavedData hive) {
        HiveProgressionTier reached = hive.tryAdvanceHighestTier();
        if (reached == null || reached == HiveProgressionTier.DORMANT) {
            return;
        }
        String key = switch (reached) {
            case AWAKENED -> "message.mycelialmind.tier_awakened";
            case ADAPTIVE -> "message.mycelialmind.tier_adaptive";
            case OVERMIND -> "message.mycelialmind.tier_overmind";
            default -> null;
        };
        String unlockKey = switch (reached) {
            case AWAKENED -> "message.mycelialmind.tier_unlock_awakened";
            case ADAPTIVE -> "message.mycelialmind.tier_unlock_adaptive";
            case OVERMIND -> "message.mycelialmind.tier_unlock_overmind";
            default -> null;
        };
        if (key == null) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            if (unlockKey != null) {
                player.sendSystemMessage(Component.translatable(unlockKey).withStyle(ChatFormatting.GOLD));
            }
            level.playSound(null, player.blockPosition(), ModSounds.HIVE_THREAT_MILESTONE.get(), SoundSource.HOSTILE, 0.85F, 1.0F);
            DiscoverySyncPayload.syncToPlayer(player);
        }
    }

    public static boolean tryConvertInfectedMob(ServerLevel level, Mob victim, boolean discardOriginal) {
        if (victim.isRemoved()
                || victim instanceof MycelialMob
                || victim instanceof ColonySymbiontEntity
                || victim instanceof MycelialChimeraEntity
                || victim instanceof PurifierDroneEntity
                || victim instanceof Warden
                || victim.getType().is(Tags.EntityTypes.BOSSES)) {
            return false;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        if (tracker.isProtected(victim.blockPosition())) {
            victim.removeEffect(ModEffects.MYCELIAL_INFECTION);
            return false;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        if (hive.getThreatLevel() < 50.0 && !hive.isFungalTideActive(level.getGameTime())) {
            return false;
        }

        EntityType<? extends MycelialMob> targetType;
        if (victim instanceof IronGolem) {
            targetType = ModEntities.CHITIN_BRUTE.get();
        } else if (victim instanceof Villager villager) {
            VillagerProfession prof = villager.getVillagerData().getProfession();
            targetType = (prof == VillagerProfession.ARMORER || prof == VillagerProfession.WEAPONSMITH || prof == VillagerProfession.TOOLSMITH)
                    ? ModEntities.CHITIN_BRUTE.get()
                    : ModEntities.MYCELIAL_STALKER.get();
        } else if (victim instanceof WanderingTrader) {
            targetType = ModEntities.MYCELIAL_STALKER.get();
        } else if (victim instanceof Animal) {
            targetType = ModEntities.SPORELING.get();
        } else {
            targetType = level.random.nextBoolean() ? ModEntities.ACID_SPITTER.get() : ModEntities.MYCELIAL_STALKER.get();
        }

        MycelialMob converted = targetType.create(level);
        if (converted == null) {
            return false;
        }
        converted.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
        converted.finalizeSpawn(level, level.getCurrentDifficultyAt(victim.blockPosition()), MobSpawnType.CONVERSION, null);
        if (!level.addFreshEntity(converted)) {
            return false;
        }
        level.sendParticles(ModParticles.NECROTIC_MIST.get(), victim.getX(), victim.getY(0.5), victim.getZ(), 20, 0.35, 0.45, 0.35, 0.05);
        level.sendParticles(ModParticles.NECROTIC_MIST.get(), victim.getX(), victim.getY(0.5), victim.getZ(), 14, 0.35, 0.45, 0.35, 0.03);
        level.playSound(null, victim.blockPosition(), ModSounds.INFECTED_MOB_CONVERT.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
        HiveMindSavedData.get(level).recordStimulus(StimulusType.MAGIC, 2.5, level.getGameTime());
        victim.removeEffect(ModEffects.MYCELIAL_INFECTION);

        if (discardOriginal) {
            victim.discard();
        }
        return true;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);

        if (state.is(ModBlocks.MYCELIAL_NODE.get())
                || state.is(ModBlocks.INCUBATOR_NODE.get())
                || state.is(ModBlocks.SENTINEL_NODE.get())) {
            if (serverLevel.getBlockEntity(pos) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity nodeBe) {
                nodeBe.purgeVolatileBuffer(serverLevel, pos, true);
            }
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            tracker.alertNearbyNodes(serverLevel, pos, 240);
            HiveBlackboard.reportNodeLoss(serverLevel, pos);
            HiveBlackboard.broadcastTacticalAlarm(serverLevel, pos, 22.0D, 100);
            double threatBefore = hive.getThreatLevel();
            hive.applyNodeDestructionBleed(
                    MycelialConfig.NODE_DESTRUCTION_STIMULUS_BLEED.get(),
                    2.5,
                    serverLevel.getGameTime()
            );
            double threatAfter = hive.getThreatLevel();
            double threatDrop = Math.max(0.0D, threatBefore - threatAfter);
            if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
                PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
                discovery.recordNodeDestroyedThreatDrop(threatDrop);
                if (discovery.getPinnedExpeditionGoal() == ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE
                        || discovery.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_NODE_THREAT_RECORDED)
                        || hasBioScannerInInventory(serverPlayer)) {
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "message.mycelialmind.scanner_node_threat_drop",
                            String.format("%.1f", threatDrop),
                            String.format("%.1f", threatAfter)
                    ).withStyle(ChatFormatting.AQUA));
                }
                DiscoverySyncPayload.syncToPlayer(serverPlayer);
                if (serverPlayer.connection != null) {
                    PacketDistributor.sendToPlayer(serverPlayer, HiveSyncPayload.forPlayer(serverLevel, serverPlayer));
                }
            }
            serverLevel.playSound(null, pos, ModSounds.SYNAPTIC_PULSE.get(), SoundSource.HOSTILE, 0.9F, 0.65F);
            serverLevel.sendParticles(ModParticles.HIVE_EMBER.get(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    16, 0.4, 0.4, 0.4, 0.04);
        } else if (state.is(ModBlocks.SPORE_VENT.get())) {
            hive.recordStimulus(StimulusType.MELEE, 4.0, serverLevel.getGameTime());
        } else if (state.is(ModBlocks.CREEPING_MYCELIUM.get())) {
            hive.recordStimulus(StimulusType.MELEE, 1.5, serverLevel.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.is(ModBlocks.CREEPING_MYCELIUM.get())
                    || state.is(ModBlocks.SPORE_VENT.get())
                    || state.is(ModBlocks.MYCELIAL_NODE.get())
                    || state.is(ModBlocks.INCUBATOR_NODE.get())
                    || state.is(ModBlocks.SENTINEL_NODE.get())) {
                HiveMindSavedData.get(serverLevel).recordStimulus(StimulusType.EXPLOSION, 8.0, serverLevel.getGameTime());
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        long gameTime = serverLevel.getGameTime();
        if (gameTime % DECAY_INTERVAL_TICKS == 0) {
            HiveMindSavedData.get(serverLevel).tickDecay(MycelialConfig.DECAY_PER_MINUTE.get(), gameTime);
        }
        if (gameTime % 20 == 0) {
            for (ServerPlayer player : serverLevel.players()) {
                PlayerDiplomacyData dip = player.getData(ModAttachments.DIPLOMACY);
                if (dip.getTruceTicks() > 0 || dip.getBetrayalCooldownTicks() > 0) {
                    dip.setTruceTicks(Math.max(0, dip.getTruceTicks() - 20));
                    dip.setBetrayalCooldownTicks(Math.max(0, dip.getBetrayalCooldownTicks() - 20));
                }
                DiplomacySyncPayload.syncToPlayer(player);
            }
        }
        if (gameTime % 60 == 0) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            for (ServerPlayer player : serverLevel.players()) {
                PlayerSymbiosisData sym = player.getData(ModAttachments.SYMBIOSIS);
                SymbiosisAttributeSync.sync(player);
                if (sym.hasMutagen(SymbioticMutagen.ARCANE_SYNAPSE)) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
                }
                if (sym.getEquippedCount() > 0
                        && !tracker.isProtected(player.blockPosition())
                        && serverLevel.getBlockState(player.blockPosition().below()).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                    player.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 120, 0));
                }
            }
        }
    }

    private static void handlePlayerIncomingDamage(LivingIncomingDamageEvent event, Player player) {
        DamageSource source = event.getSource();
        MobEffectInstance infection = player.getEffect(ModEffects.MYCELIAL_INFECTION);
        if (infection != null && source.getEntity() instanceof MycelialMob) {
            event.setAmount(event.getAmount() * (1.0F + 0.15F * (infection.getAmplifier() + 1)));
        }

        PlayerSymbiosisData sym = player.getData(ModAttachments.SYMBIOSIS);
        if (sym.hasMutagen(SymbioticMutagen.PYRO_GLAND) && source.is(DamageTypeTags.IS_FIRE)) {
            player.clearFire();
            event.setCanceled(true);
            return;
        }
        if (sym.hasMutagen(SymbioticMutagen.KINETIC_STRAFE) && source.is(DamageTypeTags.IS_PROJECTILE)) {
            if (player.getRandom().nextDouble() < 0.30) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
                event.setCanceled(true);
                return;
            }
        }
        if (sym.hasMutagen(SymbioticMutagen.BLAST_CARAPACE) && source.is(DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * 0.55F);
        }
        if (sym.hasMutagen(SymbioticMutagen.ARCANE_SYNAPSE) && source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            event.setAmount(event.getAmount() * 0.60F);
        }
    }

    private static void handlePlayerPostDamage(LivingDamageEvent.Post event, Player player) {
        float dealt = event.getNewDamage();
        if (dealt <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        PlayerSymbiosisData sym = player.getData(ModAttachments.SYMBIOSIS);
        if (source.getEntity() instanceof LivingEntity attacker && attacker != player && classifyDamage(source) == StimulusType.MELEE) {
            if (sym.hasMutagen(SymbioticMutagen.CHITIN_THORNS)) {
                attacker.hurt(player.damageSources().thorns(player), Math.max(1.0F, dealt * 0.25F));
            }
            if (sym.hasMutagen(SymbioticMutagen.PYRO_GLAND)) {
                attacker.igniteForSeconds(3);
            }
        }
        int eliteArmor = ChitinArmorItem.countElitePieces(player);
        if (eliteArmor > 0 && source.getEntity() instanceof MycelialMob mycelialAttacker && mycelialAttacker.isAlive()) {
            mycelialAttacker.hurt(player.damageSources().magic(), 1.25F * eliteArmor);
        }
    }

    private static void emitProjectileDeflectionSignal(ServerLevel sl, MycelialMob mob, DamageSource source) {
        Vec3 impact = source.getDirectEntity() != null
                ? source.getDirectEntity().position()
                : new Vec3(mob.getX(), mob.getY(0.6D), mob.getZ());
        double ix = impact.x;
        double iy = Math.max(mob.getY() + 0.2D, impact.y);
        double iz = impact.z;
        sl.sendParticles(ModParticles.SYNAPSE_SPARK.get(), ix, iy, iz, 10, 0.25, 0.25, 0.25, 0.06);
        sl.sendParticles(ModParticles.CHITIN_SHARD.get(), ix, iy, iz, 6, 0.2, 0.2, 0.2, 0.08);
        sl.playSound(null, ix, iy, iz, ModSounds.HIVE_CORE_SHIELD.get(), SoundSource.HOSTILE, 0.85F, 1.35F);

        Set<ServerPlayer> witnesses = new LinkedHashSet<>();
        if (source.getEntity() instanceof ServerPlayer attacker) {
            witnesses.add(attacker);
        }
        for (ServerPlayer nearby : sl.players()) {
            if (nearby.blockPosition().distSqr(mob.blockPosition()) <= 24.0D * 24.0D) {
                witnesses.add(nearby);
            }
        }
        for (ServerPlayer witness : witnesses) {
            PlayerDiscoveryData discovery = witness.getData(ModAttachments.DISCOVERY);
            if (discovery.recordDeflectionWitnessed()) {
                witness.displayClientMessage(
                        Component.translatable("message.mycelialmind.projectile_deflected_signal")
                                .withStyle(ChatFormatting.AQUA),
                        true
                );
                DiscoverySyncPayload.syncToPlayer(witness);
            }
        }
    }

    private static boolean hasBioScannerInInventory(ServerPlayer player) {
        if (player.getMainHandItem().is(ModItems.BIO_SCANNER.get())
                || player.getOffhandItem().is(ModItems.BIO_SCANNER.get())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.BIO_SCANNER.get())) {
                return true;
            }
        }
        return false;
    }

    private static void reflectSynapticFragment(MycelialMob sourceMob, LivingEntity attacker) {
        AcidSpitProjectile shard = new AcidSpitProjectile(sourceMob.level(), sourceMob);
        double dx = attacker.getX() - sourceMob.getX();
        double dy = attacker.getY(0.5) - shard.getY();
        double dz = attacker.getZ() - sourceMob.getZ();
        shard.shoot(dx, dy, dz, 1.4F, 2.0F);
        sourceMob.level().addFreshEntity(shard);
    }

    private static void triggerPyrolyticPulse(ServerLevel level, MycelialMob sourceMob) {
        level.sendParticles(ModParticles.HIVE_EMBER.get(), sourceMob.getX(), sourceMob.getY(0.5), sourceMob.getZ(),
                12, 0.6, 0.4, 0.6, 0.05);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sourceMob.getBoundingBox().inflate(4.0D),
                e -> !(e instanceof MycelialMob) && e.isAlive())) {
            target.igniteForSeconds(2);
            target.hurt(sourceMob.damageSources().inFire(), 2.0F);
        }
    }

    private static void notifyNewEvolutions(
            ServerLevel level,
            BlockPos pos,
            Set<AdaptationTrait> prevT2,
            Set<HiveSynergy> prevSyn,
            HiveMindSavedData hive
    ) {
        boolean announced = false;
        for (AdaptationTrait trait : hive.getTier2Traits()) {
            if (!prevT2.contains(trait)) {
                announced = true;
                for (ServerPlayer player : level.players()) {
                    if (player.blockPosition().distSqr(pos) <= 64.0 * 64.0) {
                        player.sendSystemMessage(Component.translatable("message.mycelialmind.tier2_evolved",
                                        MycelialText.traitTier2(trait))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }
        }
        for (HiveSynergy synergy : hive.getActiveSynergies()) {
            if (!prevSyn.contains(synergy)) {
                announced = true;
                for (ServerPlayer player : level.players()) {
                    if (player.blockPosition().distSqr(pos) <= 64.0 * 64.0) {
                        player.sendSystemMessage(Component.translatable("message.mycelialmind.synergy_formed",
                                        MycelialText.synergy(synergy))
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                    }
                }
            }
        }
        if (announced) {
            level.playSound(null, pos, ModSounds.HIVE_MUTATE_TIER2.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    private static boolean isAlliedFriendlyFireSource(DamageSource source) {
        if (source.getDirectEntity() instanceof NeutralizingSporeProjectile
                || source.getDirectEntity() instanceof PurifierGrenadeProjectile) {
            return true;
        }
        var attacker = source.getEntity();
        return attacker instanceof Player
                || attacker instanceof PurifierDroneEntity
                || attacker instanceof MycelialChimeraEntity
                || attacker instanceof ColonySymbiontEntity
                || (attacker instanceof MycelialMob allied && allied.isAlliedEscort());
    }

    public static StimulusType classifyDamage(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return StimulusType.FIRE;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return StimulusType.EXPLOSION;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return StimulusType.PROJECTILE;
        }
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return StimulusType.MAGIC;
        }
        StimulusType moddedType = ModdedDamageClassifier.classifyModdedDamageType(
                source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(null));
        if (moddedType != null) {
            return moddedType;
        }
        if (source.getDirectEntity() instanceof LivingEntity) {
            return StimulusType.MELEE;
        }
        return null;
    }
}
