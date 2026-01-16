package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class DefaultCrawlScheduler implements CrawlScheduler {

    private final Frontier    frontier;
    private final RetryBuffer retryBuffer;
    private final Clock       clock;
    private final int         retryDrainBatch;
    private final Duration    maxParkDuration;

    public DefaultCrawlScheduler(
            Frontier frontier,
            RetryBuffer retryBuffer,
            Clock clock,
            int retryDrainBatch,
            Duration maxParkDuration
    ) {
        this.frontier = Verify.nonNull(frontier, "frontier");
        this.retryBuffer = Verify.nonNull(retryBuffer, "retryBuffer");
        this.clock = Verify.nonNull(clock, "clock");
        this.retryDrainBatch = Math.max(1, retryDrainBatch);
        this.maxParkDuration = Verify.nonNull(maxParkDuration, "maxParkDuration");
    }

    @Override
    public void submit(CrawlTask task) {
        if (task == null) {
            return;
        }
        frontier.offer(task);
    }

    @Override
    public ScheduleDecision nextDecision() {
        Instant now = clock.instant();

        moveReadyRetries(now);

        CrawlTask task = frontier.poll();
        if (task != null) {
            return new ScheduleDecision.TaskReady(task, now);
        }

        // no task in frontier
        if (frontier.size() == 0 && retryBuffer.size() == 0) {
            return ScheduleDecision.Drained.INSTANCE;
        }

        Instant nextNotBefore = retryBuffer.peekNotBefore();
        if (nextNotBefore == null) {
            // defensive fallback: if buffer has items but cannot peek
            return new ScheduleDecision.Park(Duration.ofMillis(10), now.plusMillis(10));
        }

        Duration park = Duration.between(now, nextNotBefore);

        if (park.isNegative()) {
            park = Duration.ZERO;
        }

        if (park.compareTo(maxParkDuration) > 0) {
            park = maxParkDuration;
        }

        return new ScheduleDecision.Park(park, nextNotBefore);
    }

    private void moveReadyRetries(Instant now) {
        List<CrawlTask> readyTasks = retryBuffer.drainReady(now, retryDrainBatch);
        for (CrawlTask readyTask : readyTasks) {
            frontier.offer(readyTask);
        }
    }
}
