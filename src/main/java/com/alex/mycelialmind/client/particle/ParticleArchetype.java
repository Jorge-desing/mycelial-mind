package com.alex.mycelialmind.client.particle;

/** Signature motion of a particle family. */
public enum ParticleArchetype {
    /** Buoyant drift with a gentle lateral wobble. */
    DRIFT,
    /** Upward acceleration, used by embers and insight motes. */
    RISE,
    /** Short orbit around the spawn point. */
    ORBIT,
    /** Tangential swirl with inward pull. */
    VORTEX,
    /** Radial burst that decelerates and reverses after mid-life. */
    BURST,
    /** Ballistic arc with gravity and one ground bounce. */
    ARC,
    /** Gravity plus tumbling rotation and one ground bounce. */
    TUMBLE,
    /** Constant falling flutter with a fading wake. */
    FALL,
    /** Fast zig-zag spark with a short afterglow. */
    SPARK,
    /** Expanding billboard ring (ground-aligned when requested). */
    RING,
    /** Rising glyph that rotates over its lifetime. */
    GLYPH,
    /** Slow expanding soft cloud. */
    MIST
}
