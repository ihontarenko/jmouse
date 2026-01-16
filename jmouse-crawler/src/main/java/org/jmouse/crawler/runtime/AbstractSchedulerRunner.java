package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;

import java.time.Clock;
import java.time.Duration;

public abstract class AbstractSchedulerRunner implements CrawlRunner {

    protected final CrawlScheduler scheduler;
    protected final Clock          clock;

    protected AbstractSchedulerRunner(CrawlScheduler scheduler, Clock clock) {
        this.scheduler = Verify.nonNull(scheduler, "scheduler");
        this.clock = clock;
    }

    protected final void park(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            Thread.onSpinWait();
            return;
        }

        try {
            Thread.sleep(Math.min(duration.toMillis(), 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected final ParallelCrawlEngine requireParallel(CrawlEngine engine) {
        return Verify.instanceOf(engine, ParallelCrawlEngine.class, "engine");
    }
}
