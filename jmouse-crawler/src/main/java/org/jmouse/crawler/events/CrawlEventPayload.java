package org.jmouse.crawler.events;

import org.jmouse.crawler.runtime.CrawlRunContext;
import org.jmouse.crawler.runtime.CrawlTask;
import org.jmouse.crawler.runtime.DecisionEntry;
import org.jmouse.crawler.runtime.DecisionSnapshot;

public sealed interface CrawlEventPayload permits
        CrawlEventPayload.RunPayload,
        CrawlEventPayload.TaskPayload,
        CrawlEventPayload.TaskFailedPayload,
        CrawlEventPayload.StepPayload,
        CrawlEventPayload.DecisionPayload {

    record RunPayload(CrawlRunContext run) implements CrawlEventPayload {}

    record TaskPayload(
            CrawlRunContext run,
            CrawlTask task,
            String routeId,
            String stageId
    ) implements CrawlEventPayload {}

    record TaskFailedPayload(
            CrawlRunContext run,
            CrawlTask task,
            String routeId,
            String stageId,
            Throwable error,
            DecisionSnapshot decisions
    ) implements CrawlEventPayload {}

    record StepPayload(
            CrawlRunContext run,
            CrawlTask task,
            String routeId,
            String stageId,
            String stepId
    ) implements CrawlEventPayload {}

    record DecisionPayload(
            CrawlRunContext run,
            CrawlTask task,
            String routeId,
            String stageId,
            DecisionEntry entry
    ) implements CrawlEventPayload {}
}

