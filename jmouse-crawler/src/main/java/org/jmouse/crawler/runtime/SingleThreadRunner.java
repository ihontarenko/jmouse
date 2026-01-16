package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;

public final class SingleThreadRunner implements CrawlRunner {

    private final CrawlScheduler scheduler;

    public SingleThreadRunner(CrawlScheduler scheduler) {
        this.scheduler = Verify.nonNull(scheduler, "scheduler");
    }

    @Override
    public void runUntilDrained(CrawlEngine engine) {
        ParallelCrawlEngine parallelEngine = Verify.instanceOf(engine, ParallelCrawlEngine.class, "engine");

        while (true) {
            ScheduleDecision decision = scheduler.nextDecision();

            switch (decision) {
                case ScheduleDecision.TaskReady taskReady -> {
                    TaskDisposition disposition = parallelEngine.execute(taskReady.task(), taskReady.now());
                    parallelEngine.apply(taskReady.task(), disposition, taskReady.now());
                }
                case ScheduleDecision.Park park -> park(park.duration());
                case ScheduleDecision.Drained ignored -> {
                    return;
                }
            }
        }
    }

    private static void park(java.time.Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            Thread.onSpinWait();
            return;
        }
        try {
            Thread.sleep(Math.min(duration.toMillis(), 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}