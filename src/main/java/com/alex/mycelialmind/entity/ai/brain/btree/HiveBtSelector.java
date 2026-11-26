package com.alex.mycelialmind.entity.ai.brain.btree;

/**
 * Fallback selector composite: tries children sequentially until one succeeds or keeps running.
 */
public final class HiveBtSelector<T> extends HiveBtComposite<T, HiveBtSelector<T>> {
    private int currentChildIndex = 0;

    @Override
    protected void start(T blackboard) {
        this.currentChildIndex = 0;
    }

    @Override
    protected HiveBtStatus run(T blackboard) {
        for (int size = this.children.size(); this.currentChildIndex < size; this.currentChildIndex++) {
            HiveBtTask<T> child = this.children.get(this.currentChildIndex);
            if (!child.checkGuard(blackboard)) {
                continue;
            }
            HiveBtStatus result = child.step(blackboard);
            if (result == HiveBtStatus.RUNNING) {
                return HiveBtStatus.RUNNING;
            }
            if (result == HiveBtStatus.SUCCEEDED) {
                this.currentChildIndex = 0;
                return HiveBtStatus.SUCCEEDED;
            }
        }
        this.currentChildIndex = 0;
        return HiveBtStatus.FAILED;
    }

    @Override
    public void cancel(T blackboard) {
        if (this.currentChildIndex >= 0 && this.currentChildIndex < this.children.size()) {
            this.children.get(this.currentChildIndex).cancel(blackboard);
        }
        this.currentChildIndex = 0;
        super.cancel(blackboard);
    }

    @Override
    public void reset() {
        this.currentChildIndex = 0;
        resetChildren();
        super.reset();
    }
}
