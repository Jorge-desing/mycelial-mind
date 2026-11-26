package com.alex.mycelialmind.compat.curios;

import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Objects;

/**
 * Optional Curios integration for Bio-Scanner (belt), Overmind Crown (head), and Quorum Seal (charm),
 * plus pure passive scaling helpers tested independently of Curios runtime presence.
 */
public final class MycelialCuriosCompat {
    private static final String CURIOS_MODID = "curios";

    private MycelialCuriosCompat() {
    }

    public static boolean hasEquippedScanner(@Nullable Player player) {
        return canQueryCurios(player) && CuriosAccess.hasEquippedItem(Objects.requireNonNull(player), ModItems.BIO_SCANNER.get());
    }

    public static boolean hasEquippedOvermindCrown(@Nullable Player player) {
        return canQueryCurios(player) && CuriosAccess.hasEquippedItem(Objects.requireNonNull(player), ModItems.OVERMIND_CROWN.get());
    }

    public static boolean hasEquippedQuorumSeal(@Nullable Player player) {
        return canQueryCurios(player) && CuriosAccess.hasEquippedItem(Objects.requireNonNull(player), ModItems.QUORUM_SEAL.get());
    }

    /**
     * Reduces Mycelial Infection duration by 35% (floor 20 ticks) when Overmind Crown is equipped in Curios.
     */
    public static int applyOvermindCrownInfectionReduction(int baseTicks, boolean crownEquipped) {
        if (!crownEquipped || baseTicks <= 20) {
            return baseTicks;
        }
        return Math.max(20, (int) Math.round(baseTicks * 0.65D));
    }

    /**
     * Mitigates Deep Mycelium enzymatic acid rain damage by 30% when Overmind Crown is equipped in Curios.
     */
    public static float applyOvermindCrownAcidRainMitigation(float baseDamage, boolean crownEquipped) {
        if (!crownEquipped || baseDamage <= 0.0F) {
            return baseDamage;
        }
        return baseDamage * 0.70F;
    }

    /**
     * Boosts positive Hive Strain diplomacy reputation gains by 20% (minimum +1) when Quorum Seal is equipped.
     */
    public static int applyQuorumSealReputationBonus(int delta, boolean sealEquipped) {
        if (!sealEquipped || delta <= 0) {
            return delta;
        }
        return Math.max(delta + 1, (int) Math.round(delta * 1.20D));
    }

    /**
     * Halves accidental betrayal cooldown when Quorum Seal is equipped in the Curios charm slot.
     */
    public static int applyQuorumSealBetrayalCooldown(int baseCooldownTicks, boolean sealEquipped) {
        if (!sealEquipped || baseCooldownTicks <= 0) {
            return Math.max(0, baseCooldownTicks);
        }
        return baseCooldownTicks / 2;
    }

    public static void adjustReputationWithSeal(
            PlayerDiplomacyData diplomacy,
            @Nullable Player player,
            HiveStrain strain,
            int delta
    ) {
        int adjusted = applyQuorumSealReputationBonus(delta, hasEquippedQuorumSeal(player));
        diplomacy.adjustReputation(strain, adjusted);
    }

    public static void triggerBetrayalWithSeal(
            PlayerDiplomacyData diplomacy,
            @Nullable Player player,
            HiveStrain attackedStrain
    ) {
        diplomacy.triggerBetrayal(attackedStrain);
        if (hasEquippedQuorumSeal(player)) {
            diplomacy.setBetrayalCooldownTicks(
                    applyQuorumSealBetrayalCooldown(diplomacy.getBetrayalCooldownTicks(), true)
            );
        }
    }

    private static boolean canQueryCurios(@Nullable Player player) {
        if (player == null) {
            return false;
        }
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(CURIOS_MODID);
    }

    private static final class CuriosAccess {
        private static boolean hasEquippedItem(Player player, Item item) {
            return CuriosApi.getCuriosInventory(player)
                    .flatMap(inventory -> inventory.findFirstCurio(item))
                    .isPresent();
        }
    }
}
