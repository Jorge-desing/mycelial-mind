package com.alex.mycelialmind.util;

/**
 * Lightweight runtime probe verifying that Mixin and MixinExtras hooks are active without altering gameplay.
 * Lives outside the Mixin package because SpongePowered Mixin forbids direct class loading from mixin.*.
 */
public final class MixinRuntimeProbe {
    private static volatile boolean livingEntityHookInvoked = false;

    private MixinRuntimeProbe() {
    }

    public static void markLivingEntityHookInvoked() {
        livingEntityHookInvoked = true;
    }

    public static boolean isLivingEntityHookInvoked() {
        return livingEntityHookInvoked;
    }

    public static void resetForTests() {
        livingEntityHookInvoked = false;
    }
}
