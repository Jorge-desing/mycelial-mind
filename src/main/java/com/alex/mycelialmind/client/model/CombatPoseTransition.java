package com.alex.mycelialmind.client.model;

import java.util.Map;
import java.util.WeakHashMap;

/** Tracks each rendered entity independently, without retaining unloaded entities. */
public final class CombatPoseTransition<K> {
    private static final float HALF_LIFE_TICKS = 2.0F;
    private static final float AGGRESSION_HOLD_TICKS = 8.0F;
    private final Map<K, State> states = new WeakHashMap<>();

    public float sample(K entity, boolean aggressive, float ageInTicks) {
        State state = states.get(entity);
        if (state == null || ageInTicks < state.age) {
            float initial = aggressive ? 1.0F : 0.0F;
            states.put(entity, new State(initial, ageInTicks, aggressive ? ageInTicks : Float.NEGATIVE_INFINITY));
            return initial;
        }
        if (aggressive) state.lastAggressiveAge = ageInTicks;
        float target = aggressive || ageInTicks - state.lastAggressiveAge < AGGRESSION_HOLD_TICKS ? 1.0F : 0.0F;
        float elapsed = ageInTicks - state.age;
        state.value = target + (state.value - target)
                * (float) Math.pow(0.5D, elapsed / HALF_LIFE_TICKS);
        state.age = ageInTicks;
        return state.value;
    }

    private static final class State {
        private float value;
        private float age;
        private float lastAggressiveAge;

        private State(float value, float age, float lastAggressiveAge) {
            this.value = value;
            this.age = age;
            this.lastAggressiveAge = lastAggressiveAge;
        }
    }
}
