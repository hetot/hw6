package com.github.javarar.rejected.task;

import java.util.Map;
import java.util.concurrent.*;

// Реализован для Runnable
public class DelayedOnRejectedThreadExecutor extends ThreadPoolExecutor {
    private final long maxRetries;
    private final Map<Runnable, Integer> retries = new ConcurrentHashMap<>();
    private final long delay;

    public DelayedOnRejectedThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, long maxRetries, long delay) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.maxRetries = maxRetries;
        this.delay = delay;
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t != null) {
            retry(r);
        } else if (r instanceof Future<?>) {
            try {
                ((Future<?>) r).get();
            } catch (CancellationException | ExecutionException e) {
                if (shouldRetry(r)) {
                    retry(r);
                } else {
                    retries.remove(r);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean shouldRetry(Runnable r) {
        final Integer nbRetries = retries.getOrDefault(r, 0);
        return nbRetries < maxRetries;
    }

    private void retry(Runnable r) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final Integer nbRetries = retries.getOrDefault(r, 0);
        retries.put(r, nbRetries + 1);
        this.execute(r);
    }
}
