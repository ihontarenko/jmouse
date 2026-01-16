package org.jmouse.crawler.dsl;

import org.jmouse.crawler.runtime.CrawlRunner;
import org.jmouse.crawler.runtime.CrawlRunContext;
import org.jmouse.crawler.runtime.CrawlScheduler;

@FunctionalInterface
public interface CrawlRunnerFactory {
    CrawlRunner create(CrawlRunContext runContext, CrawlScheduler scheduler);
}