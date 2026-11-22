package com.alex.mycelialmind.hive;

/**
 * Dynamic combat role assigned by HiveSwarmCoordinator to avoid front-stacking
 * and create coordinated pincer flanks and rear support.
 */
public enum SwarmRole {
    FRONT_ANCHOR,
    FLANK_LEFT,
    FLANK_RIGHT,
    SUPPORT_REAR
}
