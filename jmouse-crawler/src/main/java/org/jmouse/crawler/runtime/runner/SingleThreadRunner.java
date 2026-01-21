package org.jmouse.crawler.runtime.runner;

import org.jmouse.core.context.execution.ExecutionContextHolder;
import org.jmouse.crawler.api.ProcessingEngine;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.runtime.schedule.ScheduleDecision;
import org.jmouse.crawler.runtime.core.TaskDisposition;
import org.jmouse.crawler.runtime.schedule.JobScheduler;
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
            switch (scheduler.nextDecision()) {
                case ScheduleDecision.TaskReady taskReady -> {
                    ProcessingTask task = taskReady.task();
                    Instant        now  = clock.instant();
                    try (var ignored = ExecutionContextHolder.open(
                            ExecutionContextHolder.current().with(TraceKeys.TRACE, task.trace())
                    )) {
                        TaskDisposition disposition = processingEngine.execute(task);
                        processingEngine.apply(task, disposition, now);
                    }
                }
                case ScheduleDecision.Park park -> park(park.duration());
                case ScheduleDecision.Drained ignored -> { return; }
            }
        }
    }
}
