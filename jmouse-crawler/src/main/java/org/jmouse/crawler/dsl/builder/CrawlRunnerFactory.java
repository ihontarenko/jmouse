package org.jmouse.crawler.dsl.builder;

import org.jmouse.crawler.runtime.runner.JobRunner;
import org.jmouse.crawler.api.RunContext;
import org.jmouse.crawler.runtime.schedule.JobScheduler;

@FunctionalInterface
public interface CrawlRunnerFactory {
    JobRunner create(RunContext runContext, JobScheduler scheduler);
}