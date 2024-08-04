package com.github.javarar.limit.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// Реализован только для Runnable
public class LimitSchedulerThreadExecutor extends ScheduledThreadPoolExecutor {
    private final Map<Runnable, Integer> scheduledTasks = new ConcurrentHashMap<>();

    public LimitSchedulerThreadExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit, int repeatFactor) {
        scheduledTasks.put(command, repeatFactor);
        return super.schedule(command, delay, unit);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (scheduledTasks.containsKey(r)) {
            scheduledTasks.put(r, scheduledTasks.get(r) - 1);
            if (scheduledTasks.get(r) <= 0) {
                super.getQueue().remove(r);
                scheduledTasks.remove(r);
            }
        }
    }
}
