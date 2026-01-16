package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractSchedulerRunner implements CrawlRunner {

    protected final JobScheduler scheduler;
    protected final Clock        clock;

    protected AbstractSchedulerRunner(JobScheduler scheduler, Clock clock) {
        this.scheduler = Verify.nonNull(scheduler, "scheduler");
        this.clock = clock;
    }

    protected final void park(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            Thread.yield();
            return;
        }

        long nanos = duration.toNanos();

        if (nanos < 1_000_000L) {
            LockSupport.parkNanos(nanos);
            return;
        }

        try {
            Thread.sleep(Math.min(duration.toMillis(), 50L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected final ProcessingEngine requireParallel(ProcessingEngine engine) {
        return Verify.instanceOf(engine, ProcessingEngine.class, "engine");
    }
}
