package com.alex.mycelialmind.hive;

/**
 * Distinct fungal strains within the Mycelial Mind capable of Synaptic Schisms
 * and chemical diplomacy with players.
 */
public enum HiveStrain {
    ORTHODOX(0, "orthodox", 0xFF9B4DFF, 0.61F, 0.30F, 1.00F),
    AMBER(1, "amber", 0xFFF2A93B, 0.95F, 0.66F, 0.23F),
    CRIMSON(2, "crimson", 0xFFE03C5C, 0.88F, 0.24F, 0.36F);

    private final int id;
    private final String serializedName;
    private final int argbColor;
    private final float r;
    private final float g;
    private final float b;

    HiveStrain(int id, String serializedName, int argbColor, float r, float g, float b) {
        this.id = id;
        this.serializedName = serializedName;
        this.argbColor = argbColor;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int id() {
        return id;
    }

    public String serializedName() {
        return serializedName;
    }

    public int argbColor() {
        return argbColor;
    }

    public float r() {
        return r;
    }

    public float g() {
        return g;
    }

    public float b() {
        return b;
    }

    public boolean isSchismStrain() {
        return this != ORTHODOX;
    }

    public HiveStrain oppositeSchism() {
        return switch (this) {
            case AMBER -> CRIMSON;
            case CRIMSON -> AMBER;
            case ORTHODOX -> AMBER;
        };
    }

    public static HiveStrain byId(int id) {
        for (HiveStrain strain : values()) {
            if (strain.id == id) {
                return strain;
            }
        }
        return ORTHODOX;
    }

    public static HiveStrain byName(String name) {
        if (name == null) {
            return ORTHODOX;
        }
        for (HiveStrain strain : values()) {
            if (strain.serializedName.equalsIgnoreCase(name)) {
                return strain;
            }
        }
        return ORTHODOX;
    }
}
