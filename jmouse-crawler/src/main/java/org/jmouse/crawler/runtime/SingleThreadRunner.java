package org.jmouse.crawler.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public final class SingleThreadRunner extends AbstractSchedulerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleThreadRunner.class);

    public SingleThreadRunner(JobScheduler scheduler, Clock clock) {
        super(scheduler, clock);
    }

    @Override
    public void runUntilDrained(ProcessingEngine engine) {
        ProcessingEngine processingEngine = requireEngine(engine);
        while (true) {
            ScheduleDecision decision = scheduler.nextDecision();
            LOGGER.info("Scheduling job: {}", decision);
            switch (decision) {
                case ScheduleDecision.TaskReady taskReady -> {
                    ProcessingTask  task        = taskReady.task();
                    Instant         now         = clock.instant();
                    TaskDisposition disposition = processingEngine.execute(task);
                    processingEngine.apply(task, disposition, now);
                }
                case ScheduleDecision.Park park -> park(park.duration());
                case ScheduleDecision.Drained ignored -> { return; }
            }
        }
    }
}
