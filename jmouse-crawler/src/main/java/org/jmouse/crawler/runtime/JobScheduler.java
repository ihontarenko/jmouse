package org.jmouse.crawler.runtime;

public interface JobScheduler {
    ScheduleDecision nextDecision();
}