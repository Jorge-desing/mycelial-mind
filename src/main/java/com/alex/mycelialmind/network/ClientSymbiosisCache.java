package com.alex.mycelialmind.network;

import com.alex.mycelialmind.symbiosis.SymbioticMutagen;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Client-side cache of the local player's active Symbiotic Mutagens and Mycelial Infection state.
 */
public final class ClientSymbiosisCache {
    private static Set<SymbioticMutagen> mutagens = EnumSet.noneOf(SymbioticMutagen.class);
    private static int infectionLevel = 0;

    private ClientSymbiosisCache() {
    }

    public static void clear() {
        update(Set.of(), 0);
    }

    public static void update(Set<SymbioticMutagen> active, int infection) {
        mutagens = active.isEmpty() ? EnumSet.noneOf(SymbioticMutagen.class) : EnumSet.copyOf(active);
        infectionLevel = infection;
    }

    public static Set<SymbioticMutagen> getEquippedMutagens() {
        return Collections.unmodifiableSet(mutagens);
    }

    public static int getInfectionLevel() {
        return infectionLevel;
    }
}
