package org.jmouse.crawler.runtime;

import java.time.Instant;

public final class SingleThreadRunner extends AbstractSchedulerRunner {

    public SingleThreadRunner(CrawlScheduler scheduler) {
        super(scheduler);
    }

    @Override
    public void runUntilDrained(CrawlEngine engine) {
        ParallelCrawlEngine parallelEngine = requireParallel(engine);

        while (true) {
            ScheduleDecision decision = scheduler.nextDecision();
            switch (decision) {
                case ScheduleDecision.TaskReady taskReady -> {
                    Instant now = taskReady.now();
                    TaskDisposition disposition = parallelEngine.execute(taskReady.task(), now);
                    parallelEngine.apply(taskReady.task(), disposition, now);
                }
                case ScheduleDecision.Park park -> park(park.duration());
                case ScheduleDecision.Drained ignored -> {
                    return;
                }
            }
        }
    }
}
