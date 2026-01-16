package org.jmouse.crawler.runtime;

import java.time.Clock;
import java.time.Instant;

public final class SingleThreadRunner extends AbstractSchedulerRunner {

    public SingleThreadRunner(CrawlScheduler scheduler, Clock clock) {
        super(scheduler, clock);
    }

    @Override
    public void runUntilDrained(CrawlEngine engine) {
        CrawlEngine parallelEngine = requireParallel(engine);
        while (true) {
            switch (scheduler.nextDecision()) {
                case ScheduleDecision.TaskReady taskReady -> {
                    CrawlTask task = taskReady.task();
                    Instant now = clock.instant();
                    TaskDisposition disposition = parallelEngine.execute(task);
                    parallelEngine.apply(task, disposition, now);
                }
                case ScheduleDecision.Park park -> park(park.duration());
                case ScheduleDecision.Drained ignored -> { return; }
            }
        }
    }
}
