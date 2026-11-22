package com.alex.mycelialmind.diplomacy;

import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveStrain;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Per-player chemical diplomacy state tracking reputation with Amber and Crimson strains,
 * temporary Cryptic Truce camouflage, and betrayal cooldowns (Vanilla+ balanced).
 */
public class PlayerDiplomacyData implements INBTSerializable<CompoundTag> {
    public static final int MIN_REP = -100;
    public static final int MAX_REP = 100;
    public static final int PACT_THRESHOLD = 25;
    public static final int ALLIED_THRESHOLD = 60;
    public static final int HOSTILE_THRESHOLD = -40;
    public static final int BETRAYAL_PENALTY = -35;
    public static final int BETRAYAL_COOLDOWN_TICKS = 600; // 30s

    private static final String TAG_AMBER = "AmberRep";
    private static final String TAG_CRIMSON = "CrimsonRep";
    private static final String TAG_TRUCE = "TruceTicks";
    private static final String TAG_BETRAYAL = "BetrayalCooldown";
    private static final String TAG_SELECTED = "SelectedStrain";

    public enum Standing {
        HOSTILE("Hostile", 0xFFFF595E),
        NEUTRAL("Neutral", 0xFFADB5BD),
        PACT("Pact", 0xFFFFD166),
        ALLIED("Allied", 0xFF06D6A0);

        private final String label;
        private final int color;

        Standing(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public int color() {
            return color;
        }
    }

    private int amberRep = 0;
    private int crimsonRep = 0;
    private int truceTicks = 0;
    private int betrayalCooldownTicks = 0;
    private HiveStrain selectedStrain = HiveStrain.AMBER;

    public int getReputation(HiveStrain strain) {
        return switch (strain) {
            case AMBER -> amberRep;
            case CRIMSON -> crimsonRep;
            case ORTHODOX -> -100;
        };
    }

    public void setReputation(HiveStrain strain, int value) {
        int clamped = Mth.clamp(value, MIN_REP, MAX_REP);
        if (strain == HiveStrain.AMBER) {
            amberRep = clamped;
        } else if (strain == HiveStrain.CRIMSON) {
            crimsonRep = clamped;
        }
    }

    /**
     * Adjusts reputation for a schism strain; positive gains apply a -0.5x rival trade-off.
     */
    public void adjustReputation(HiveStrain strain, int delta) {
        if (!strain.isSchismStrain() || delta == 0) {
            return;
        }
        setReputation(strain, getReputation(strain) + delta);
        if (delta > 0) {
            HiveStrain rival = strain.oppositeSchism();
            int rivalShift = Math.max(1, delta / 2);
            setReputation(rival, getReputation(rival) - rivalShift);
        }
    }

    public Standing getStanding(HiveStrain strain) {
        if (strain == HiveStrain.ORTHODOX) {
            return Standing.HOSTILE;
        }
        int rep = getReputation(strain);
        if (rep >= ALLIED_THRESHOLD) {
            return Standing.ALLIED;
        }
        if (rep >= PACT_THRESHOLD) {
            return Standing.PACT;
        }
        if (rep <= HOSTILE_THRESHOLD) {
            return Standing.HOSTILE;
        }
        return Standing.NEUTRAL;
    }

    public HiveStrain getSelectedStrain() {
        return selectedStrain.isSchismStrain() ? selectedStrain : HiveStrain.AMBER;
    }

    public void toggleSelectedStrain() {
        selectedStrain = (selectedStrain == HiveStrain.AMBER) ? HiveStrain.CRIMSON : HiveStrain.AMBER;
    }

    public void setSelectedStrain(HiveStrain strain) {
        if (strain != null && strain.isSchismStrain()) {
            this.selectedStrain = strain;
        }
    }

    public int getTruceTicks() {
        return truceTicks;
    }

    public void setTruceTicks(int ticks) {
        this.truceTicks = Math.max(0, ticks);
    }

    public int getBetrayalCooldownTicks() {
        return betrayalCooldownTicks;
    }

    public void setBetrayalCooldownTicks(int ticks) {
        this.betrayalCooldownTicks = Math.max(0, ticks);
    }

    public void tickTimers() {
        if (truceTicks > 0) {
            truceTicks--;
        }
        if (betrayalCooldownTicks > 0) {
            betrayalCooldownTicks--;
        }
    }

    /**
     * Triggered when a player attacks an allied/pact strain mob or breaks Cryptic Truce.
     */
    public void triggerBetrayal(HiveStrain attackedStrain) {
        if (attackedStrain.isSchismStrain()) {
            setReputation(attackedStrain, getReputation(attackedStrain) + BETRAYAL_PENALTY);
        }
        truceTicks = 0;
        betrayalCooldownTicks = BETRAYAL_COOLDOWN_TICKS;
    }

    /**
     * Vanilla+ protection check: never protects against ELITE mobs, bosses, or while under betrayal penalty.
     */
    public boolean isProtectedFrom(HiveStrain mobStrain, EvolutionRank rank, boolean isBoss, double distanceSq) {
        if (isBoss || rank == EvolutionRank.ELITE || betrayalCooldownTicks > 0) {
            return false;
        }
        if (truceTicks > 0) {
            return true;
        }
        Standing standing = getStanding(mobStrain);
        if (standing == Standing.ALLIED) {
            return true;
        }
        // PACT grants passive non-aggression beyond 4.5 blocks
        return standing == Standing.PACT && distanceSq > 20.25D;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_AMBER, amberRep);
        tag.putInt(TAG_CRIMSON, crimsonRep);
        tag.putInt(TAG_TRUCE, truceTicks);
        tag.putInt(TAG_BETRAYAL, betrayalCooldownTicks);
        tag.putInt(TAG_SELECTED, selectedStrain.id());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        amberRep = Mth.clamp(tag.getInt(TAG_AMBER), MIN_REP, MAX_REP);
        crimsonRep = Mth.clamp(tag.getInt(TAG_CRIMSON), MIN_REP, MAX_REP);
        truceTicks = Math.max(0, tag.getInt(TAG_TRUCE));
        betrayalCooldownTicks = Math.max(0, tag.getInt(TAG_BETRAYAL));
        HiveStrain loaded = HiveStrain.byId(tag.getInt(TAG_SELECTED));
        selectedStrain = loaded.isSchismStrain() ? loaded : HiveStrain.AMBER;
    }
}
