package org.jmouse.crawler.runtime;

import java.time.Duration;
import java.time.Instant;

public sealed interface ScheduleDecision
        permits ScheduleDecision.TaskReady, ScheduleDecision.Park, ScheduleDecision.Drained {

    record TaskReady(CrawlTask task, Instant now) implements ScheduleDecision { }

    record Park(Duration duration, Instant until) implements ScheduleDecision { }

    record Drained() implements ScheduleDecision {
        public static final Drained INSTANCE = new Drained();
    }
}