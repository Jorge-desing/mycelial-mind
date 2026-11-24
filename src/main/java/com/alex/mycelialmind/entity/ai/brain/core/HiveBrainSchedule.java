package com.alex.mycelialmind.entity.ai.brain.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Clean-room declarative activity schedule for {@link HiveBrain}.
 * Supports day-tick points/windows (including overnight wrap-around), predicate rules,
 * and once-per-game-tick callbacks without Access Transformers or Mixins.
 */
public final class HiveBrainSchedule {
    private final List<WindowEntry> windows = new ArrayList<>();
    private final List<PredicateRule> rules = new ArrayList<>();
    private final List<CallbackEntry> callbacks = new ArrayList<>();
    private long lastCallbackGameTime = HiveBrainScheduler.INITIAL_TICK_SENTINEL;

    public HiveBrainSchedule activityAt(int dayTick, Activity activity) {
        int normalized = HiveBrainScheduler.normalizeDayTick(dayTick);
        return activityWindow(normalized, normalized, activity);
    }

    public HiveBrainSchedule activityWindow(int startDayTick, int endDayTick, Activity activity) {
        if (activity != null) {
            this.windows.add(new WindowEntry(
                    HiveBrainScheduler.normalizeDayTick(startDayTick),
                    HiveBrainScheduler.normalizeDayTick(endDayTick),
                    activity
            ));
        }
        return this;
    }

    public HiveBrainSchedule activityWhen(BiPredicate<ServerLevel, LivingEntity> condition, Activity activity) {
        if (condition != null && activity != null) {
            this.rules.add(new PredicateRule(condition, activity));
        }
        return this;
    }

    public HiveBrainSchedule callbackAt(int dayTick, BiConsumer<ServerLevel, LivingEntity> callback) {
        if (callback != null) {
            this.callbacks.add(new CallbackEntry(HiveBrainScheduler.normalizeDayTick(dayTick), callback));
        }
        return this;
    }

    @Nullable
    public Activity resolveActivity(ServerLevel level, LivingEntity entity) {
        for (int i = 0; i < this.rules.size(); i++) {
            PredicateRule rule = this.rules.get(i);
            if (rule.condition.test(level, entity)) {
                return rule.activity;
            }
        }
        if (this.windows.isEmpty()) {
            return null;
        }
        int dayTick = HiveBrainScheduler.normalizeDayTick(level.getDayTime());
        Activity resolved = null;
        for (int i = 0; i < this.windows.size(); i++) {
            WindowEntry window = this.windows.get(i);
            if (HiveBrainScheduler.isTickWithinWindow(dayTick, window.startTick, window.endTick)) {
                resolved = window.activity;
            }
        }
        return resolved;
    }

    public void runCallbacks(ServerLevel level, LivingEntity entity) {
        if (this.callbacks.isEmpty()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (this.lastCallbackGameTime == gameTime) {
            return;
        }
        this.lastCallbackGameTime = gameTime;
        int dayTick = HiveBrainScheduler.normalizeDayTick(level.getDayTime());
        for (int i = 0; i < this.callbacks.size(); i++) {
            CallbackEntry entry = this.callbacks.get(i);
            if (entry.dayTick == dayTick) {
                entry.callback.accept(level, entity);
            }
        }
    }

    private record WindowEntry(int startTick, int endTick, Activity activity) {
    }

    private record PredicateRule(BiPredicate<ServerLevel, LivingEntity> condition, Activity activity) {
    }

    private record CallbackEntry(int dayTick, BiConsumer<ServerLevel, LivingEntity> callback) {
    }
}
