package org.jmouse.crawler.dsl;

import org.jmouse.crawler.runtime.CrawlRunContext;
import org.jmouse.crawler.runtime.CrawlScheduler;

@FunctionalInterface
public interface SchedulerFactory {
    CrawlScheduler create(CrawlRunContext runContext);
}