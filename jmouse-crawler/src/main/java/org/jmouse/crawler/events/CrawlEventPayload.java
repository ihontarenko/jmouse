package org.jmouse.crawler.events;

import org.jmouse.crawler.runtime.RunContext;
import org.jmouse.crawler.runtime.ProcessingTask;
import org.jmouse.crawler.runtime.DecisionEntry;
import org.jmouse.crawler.runtime.DecisionSnapshot;

public sealed interface CrawlEventPayload permits
        CrawlEventPayload.RunPayload,
        CrawlEventPayload.TaskPayload,
        CrawlEventPayload.TaskFailedPayload,
        CrawlEventPayload.StepPayload,
        CrawlEventPayload.DecisionPayload {

    record RunPayload(RunContext run) implements CrawlEventPayload {}

    record TaskPayload(
            RunContext run,
            ProcessingTask task,
            String routeId,
            String stageId
    ) implements CrawlEventPayload {}

    record TaskFailedPayload(
            RunContext run,
            ProcessingTask task,
            String routeId,
            String stageId,
            Throwable error,
            DecisionSnapshot decisions
    ) implements CrawlEventPayload {}

    record StepPayload(
            RunContext run,
            ProcessingTask task,
            String routeId,
            String stageId,
            String stepId
    ) implements CrawlEventPayload {}

    record DecisionPayload(
            RunContext run,
            ProcessingTask task,
            String routeId,
            String stageId,
            DecisionEntry entry
    ) implements CrawlEventPayload {}
}

