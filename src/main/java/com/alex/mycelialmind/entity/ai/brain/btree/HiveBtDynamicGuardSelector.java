package com.alex.mycelialmind.entity.ai.brain.btree;

import javax.annotation.Nullable;

/**
 * Reactive selector that re-evaluates child guards in priority order on every tick,
 * immediately cancelling a running child when a higher-priority branch becomes valid.
 */
public final class HiveBtDynamicGuardSelector<T> extends HiveBtComposite<T, HiveBtDynamicGuardSelector<T>> {
    @Nullable
    private HiveBtTask<T> runningChild;

    @Override
    protected HiveBtStatus run(T blackboard) {
        HiveBtTask<T> chosen = null;
        for (int i = 0, size = this.children.size(); i < size; i++) {
            HiveBtTask<T> candidate = this.children.get(i);
            if (candidate.checkGuard(blackboard)) {
                chosen = candidate;
                break;
            }
        }

        if (this.runningChild != null && this.runningChild != chosen) {
            this.runningChild.cancel(blackboard);
            this.runningChild = null;
        }

        if (chosen == null) {
            return HiveBtStatus.FAILED;
        }

        HiveBtStatus childStatus = chosen.step(blackboard);
        if (childStatus == HiveBtStatus.RUNNING) {
            this.runningChild = chosen;
            return HiveBtStatus.RUNNING;
        }
        this.runningChild = null;
        return childStatus;
    }

    @Override
    public void cancel(T blackboard) {
        if (this.runningChild != null) {
            this.runningChild.cancel(blackboard);
            this.runningChild = null;
        }
        super.cancel(blackboard);
    }

    @Override
    public void reset() {
        this.runningChild = null;
        resetChildren();
        super.reset();
    }
}
