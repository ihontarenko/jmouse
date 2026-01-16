package org.jmouse.crawler.runtime;

public interface CrawlScheduler {
    ScheduleDecision nextDecision();
}