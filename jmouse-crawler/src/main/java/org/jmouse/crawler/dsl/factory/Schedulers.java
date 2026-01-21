package org.jmouse.crawler.dsl.factory;

import org.jmouse.core.Verify;
import org.jmouse.crawler.dsl.builder.SchedulerFactory;
import org.jmouse.crawler.runtime.schedule.DefaultScheduler;

import java.time.Duration;

public final class Schedulers {

    private Schedulers() {}

    public static SchedulerFactory defaultScheduler() {
        return defaultScheduler(256, Duration.ofMillis(50));
    }

    public static SchedulerFactory defaultScheduler(int retryDrainBatch, Duration maxParkDuration) {
        Verify.state(retryDrainBatch > 0, "retryDrainBatch must be > 0");
        Verify.nonNull(maxParkDuration, "maxParkDuration");
        return runContext -> new DefaultScheduler(
                runContext.frontier(),
                runContext.politeness(),
                runContext.retryBuffer(),
                runContext.clock(),
                retryDrainBatch,
                maxParkDuration
        );
    }
}
