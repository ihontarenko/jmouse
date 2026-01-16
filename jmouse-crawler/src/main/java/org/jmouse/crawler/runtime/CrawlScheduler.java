package org.jmouse.crawler.runtime;

public interface CrawlScheduler {

    ScheduleDecision nextDecision();

    void submit(CrawlTask task);

}