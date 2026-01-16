package org.jmouse.crawler.runtime;

import java.time.Duration;
import java.time.Instant;

public sealed interface ScheduleDecision permits
        ScheduleDecision.TaskReady, ScheduleDecision.Park, ScheduleDecision.Drained {

    record TaskReady(CrawlTask task) implements ScheduleDecision {}

    record Park(Duration duration, Instant wakeUpAt) implements ScheduleDecision {}

    enum Drained implements ScheduleDecision { INSTANCE }
}