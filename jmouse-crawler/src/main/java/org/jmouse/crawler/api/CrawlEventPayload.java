package org.jmouse.crawler.api;

import org.jmouse.crawler.runtime.state.DecisionEntry;
import org.jmouse.crawler.runtime.state.DecisionSnapshot;

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

