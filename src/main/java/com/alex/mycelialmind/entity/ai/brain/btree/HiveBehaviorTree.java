package com.alex.mycelialmind.entity.ai.brain.btree;

/**
 * Clean-room Behavior Tree runtime driving a root {@link HiveBtTask} over a typed blackboard {@code T}.
 */
public final class HiveBehaviorTree<T> {
    private final HiveBtTask<T> root;
    private final T blackboard;

    public HiveBehaviorTree(HiveBtTask<T> root, T blackboard) {
        this.root = root;
        this.blackboard = blackboard;
    }

    public HiveBtTask<T> getRoot() {
        return this.root;
    }

    public T getBlackboard() {
        return this.blackboard;
    }

    public HiveBtStatus getStatus() {
        return this.root.getStatus();
    }

    public HiveBtStatus step() {
        if (!this.root.checkGuard(this.blackboard)) {
            this.root.cancel(this.blackboard);
            return HiveBtStatus.FAILED;
        }
        return this.root.step(this.blackboard);
    }

    public void cancel() {
        this.root.cancel(this.blackboard);
    }

    public void reset() {
        this.root.cancel(this.blackboard);
        this.root.reset();
    }
}
