package com.alex.mycelialmind.entity.ai.brain.btree;

/**
 * Execution status for nodes in a native clean-room {@link HiveBehaviorTree}.
 */
public enum HiveBtStatus {
    FRESH,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
