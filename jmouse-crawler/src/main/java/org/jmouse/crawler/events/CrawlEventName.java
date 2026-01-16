package org.jmouse.crawler.events;

import org.jmouse.core.events.EventCategory;
import org.jmouse.core.events.EventName;

public enum CrawlEventName implements EventName {

    RUN_STARTED("crawler.run.started", "Run started", CrawlEventCategory.RUN),
    RUN_FINISHED("crawler.run.finished", "Run finished", CrawlEventCategory.RUN),

    TASK_SUBMITTED("crawler.task.submitted", "Task submitted", CrawlEventCategory.TASK),
    TASK_STARTED("crawler.task.started", "Task started", CrawlEventCategory.TASK),
    TASK_COMPLETED("crawler.task.completed", "Task completed", CrawlEventCategory.TASK),
    TASK_DISCARDED("crawler.task.discarded", "Task discarded", CrawlEventCategory.TASK),

    TASK_RETRY_SCHEDULED("crawler.task.retry_scheduled", "Retry scheduled", CrawlEventCategory.RETRY),
    TASK_DEAD_LETTERED("crawler.task.dead_lettered", "Dead lettered", CrawlEventCategory.DLQ),

    PIPELINE_STEP_STARTED("crawler.pipeline.step.started", "Pipeline step started", CrawlEventCategory.PIPELINE),
    PIPELINE_STEP_FINISHED("crawler.pipeline.step.finished", "Pipeline step finished", CrawlEventCategory.PIPELINE),
    PIPELINE_STEP_FAILED("crawler.pipeline.step.failed", "Pipeline step failed", CrawlEventCategory.PIPELINE),

    DECISION_RECORDED("crawler.decision.recorded", "Decision recorded", CrawlEventCategory.DECISION);

    private final EventCategory category;
    private final String        id;
    private final String        label;

    CrawlEventName(String id, String label, EventCategory category) {
        this.category = category;
        this.id = id;
        this.label = label;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public EventCategory category() {
        return category;
    }

    @Override
    public String label() {
        return label;
    }
}
