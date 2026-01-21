package org.jmouse.crawler.dsl.builder;

import org.jmouse.crawler.api.RunContext;
import org.jmouse.crawler.runtime.schedule.JobScheduler;

@FunctionalInterface
public interface SchedulerFactory {
    JobScheduler create(RunContext runContext);
}