package org.jmouse.crawler.dsl;

import org.jmouse.crawler.runtime.CrawlRunner;
import org.jmouse.crawler.runtime.RunContext;
import org.jmouse.crawler.runtime.JobScheduler;

@FunctionalInterface
public interface CrawlRunnerFactory {
    CrawlRunner create(RunContext runContext, JobScheduler scheduler);
}