package com.alex.mycelialmind.client.particle;

/**
 * Immutable visual definition of one particle type: motion archetype, sprite
 * frames, lifetime, size curve, render style and optional client-side emitter.
 */
public record ParticleSpec(
        ParticleArchetype archetype,
        Style style,
        int frames,
        int minLifetime,
        int maxLifetime,
        float baseSize,
        float sizeStart,
        float sizeEnd,
        float gravity,
        float drag,
        float spin,
        boolean fullbright,
        Emission emission,
        float tintRed,
        float tintGreen,
        float tintBlue
) {
    public enum Style {
        TRANSLUCENT,
        ADDITIVE
    }

    public enum Emission {
        NONE,
        RECOVERY_ARCS,
        PURGE_RING,
        SCAN_DOUBLE,
        SERUM_PAIRS,
        ENGRAM_TRIO,
        IMPACT_FAN
    }

    /** Archetype defaults; callers chain {@code life/size/spin/...} for overrides. */
    public static ParticleSpec of(ParticleArchetype archetype, Style style, int frames) {
        int min = 20;
        int max = 32;
        float base = 0.09F;
        float start = 1.0F;
        float end = 0.82F;
        float gravity = 0.0F;
        float drag = 0.97F;
        float spin = 0.03F;
        switch (archetype) {
            case ORBIT -> { min = 16; max = 26; base = 0.085F; }
            case VORTEX -> { min = 22; max = 34; base = 0.10F; }
            case BURST -> { min = 14; max = 22; base = 0.10F; end = 0.85F; }
            case ARC -> { min = 18; max = 28; base = 0.10F; end = 0.86F; gravity = 1.0F; spin = 0.08F; }
            case TUMBLE -> { min = 20; max = 30; base = 0.11F; end = 0.88F; gravity = 1.0F; spin = 0.22F; }
            case FALL -> { min = 24; max = 36; base = 0.085F; gravity = 0.4F; spin = 0.05F; }
            case SPARK -> { min = 10; max = 16; base = 0.08F; end = 0.70F; }
            case RING -> { min = 16; max = 26; base = 0.65F; start = 0.55F; end = 2.2F; spin = 0.0F; }
            case GLYPH -> { min = 24; max = 36; base = 0.18F; end = 0.88F; spin = 0.05F; }
            case MIST -> { min = 30; max = 44; base = 0.55F; start = 0.9F; end = 1.6F; spin = 0.01F; }
            case RISE -> { min = 18; max = 30; base = 0.09F; end = 0.80F; spin = 0.02F; }
            default -> { }
        }
        return new ParticleSpec(archetype, style, frames, min, max, base, start, end, gravity, drag, spin,
                true, Emission.NONE, 1.0F, 1.0F, 1.0F);
    }

    public ParticleSpec life(int min, int max) {
        return new ParticleSpec(archetype, style, frames, min, max, baseSize, sizeStart, sizeEnd, gravity, drag,
                spin, fullbright, emission, tintRed, tintGreen, tintBlue);
    }

    public ParticleSpec size(float base, float start, float end) {
        return new ParticleSpec(archetype, style, frames, minLifetime, maxLifetime, base, start, end, gravity, drag,
                spin, fullbright, emission, tintRed, tintGreen, tintBlue);
    }

    public ParticleSpec spin(float value) {
        return new ParticleSpec(archetype, style, frames, minLifetime, maxLifetime, baseSize, sizeStart, sizeEnd,
                gravity, drag, value, fullbright, emission, tintRed, tintGreen, tintBlue);
    }

    public ParticleSpec glow() {
        return new ParticleSpec(archetype, style, frames, minLifetime, maxLifetime, baseSize, sizeStart, sizeEnd,
                gravity, drag, spin, true, emission, tintRed, tintGreen, tintBlue);
    }

    public ParticleSpec emitter(Emission kind) {
        return new ParticleSpec(archetype, style, frames, minLifetime, maxLifetime, baseSize, sizeStart, sizeEnd,
                gravity, drag, spin, fullbright, kind, tintRed, tintGreen, tintBlue);
    }

    public ParticleSpec tint(float red, float green, float blue) {
        return new ParticleSpec(archetype, style, frames, minLifetime, maxLifetime, baseSize, sizeStart, sizeEnd,
                gravity, drag, spin, fullbright, emission, red, green, blue);
    }

    public int lifetime(net.minecraft.util.RandomSource random) {
        int span = Math.max(1, maxLifetime - minLifetime + 1);
        return minLifetime + random.nextInt(span);
    }
}
