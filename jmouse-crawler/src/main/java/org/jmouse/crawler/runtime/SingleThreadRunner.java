package org.jmouse.crawler.runtime;

import java.time.Clock;
import java.time.Instant;

public final class SingleThreadRunner extends AbstractSchedulerRunner {

    public SingleThreadRunner(JobScheduler scheduler, Clock clock) {
        super(scheduler, clock);
    }

    @Override
    public void runUntilDrained(ProcessingEngine engine) {
        ProcessingEngine processingEngine = requireParallel(engine);
        while (true) {
            switch (scheduler.nextDecision()) {
                case ScheduleDecision.TaskReady taskReady -> {
                    ProcessingTask task = taskReady.task();
                    Instant        now  = clock.instant();
                    TaskDisposition disposition = processingEngine.execute(task);
                    processingEngine.apply(task, disposition, now);
                }
                case ScheduleDecision.Park park -> park(park.duration());
                case ScheduleDecision.Drained ignored -> { return; }
            }
        }
    }
}
