package org.jmouse.crawler.dsl;

import org.jmouse.crawler.runtime.RunContext;
import org.jmouse.crawler.runtime.JobScheduler;

@FunctionalInterface
public interface SchedulerFactory {
    JobScheduler create(RunContext runContext);
}