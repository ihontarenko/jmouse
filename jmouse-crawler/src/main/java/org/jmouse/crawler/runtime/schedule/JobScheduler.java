package org.jmouse.crawler.runtime.schedule;

public interface JobScheduler {
    ScheduleDecision nextDecision();
}