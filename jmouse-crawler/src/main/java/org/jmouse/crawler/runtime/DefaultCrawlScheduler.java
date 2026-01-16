package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.PolitenessPolicy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.lang.Math.max;
import static org.jmouse.core.Verify.nonNull;

public final class DefaultCrawlScheduler implements CrawlScheduler {

    private static final String REASON_POLITENESS = "politeness";

    private final Frontier         frontier;
    private final PolitenessPolicy politeness;
    private final RetryBuffer      retryBuffer;
    private final Clock            clock;
    private final int              retryDrainBatch;
    private final Duration         maxParkDuration;

    public DefaultCrawlScheduler(
            Frontier frontier,
            PolitenessPolicy politeness,
            RetryBuffer retryBuffer,
            Clock clock,
            int retryDrainBatch,
            Duration maxParkDuration
    ) {
        this.frontier = nonNull(frontier, "frontier");
        this.retryBuffer = nonNull(retryBuffer, "retryBuffer");
        this.politeness = nonNull(politeness, "politeness");
        this.clock = nonNull(clock, "clock");
        this.retryDrainBatch = max(1, retryDrainBatch);
        this.maxParkDuration = nonNull(maxParkDuration, "maxParkDuration");
    }

    @Override
    public ScheduleDecision nextDecision() {
        Instant now = clock.instant();

        moveReadyRetries(now);

        CrawlTask task = frontier.poll();
        if (task != null) {
            // politeness gate HERE
            Instant notBefore = politeness.notBefore(task.url(), now);
            if (notBefore != null && notBefore.isAfter(now)) {
                retryBuffer.schedule(task.schedule(notBefore), notBefore, REASON_POLITENESS, null);
                return parkDecision(now);
            }

            return new ScheduleDecision.TaskReady(task);
        }

        return parkOrDrained(now);
    }

    private void moveReadyRetries(Instant now) {
        for (CrawlTask ready : retryBuffer.drainReady(now, retryDrainBatch)) {
            frontier.offer(ready);
        }
    }

    private ScheduleDecision parkOrDrained(Instant now) {
        if (frontier.size() == 0 && retryBuffer.size() == 0) {
            return ScheduleDecision.Drained.INSTANCE;
        }
        return parkDecision(now);
    }

    private ScheduleDecision parkDecision(Instant now) {
        Instant nextNotBefore = retryBuffer.peekNotBefore();
        if (nextNotBefore == null) {
            Duration fallback = Duration.ofMillis(10);
            return new ScheduleDecision.Park(fallback, now.plus(fallback));
        }

        Duration park = Duration.between(now, nextNotBefore);
        if (park.isNegative()) park = Duration.ZERO;
        if (park.compareTo(maxParkDuration) > 0) park = maxParkDuration;

        return new ScheduleDecision.Park(park, nextNotBefore);
    }
}

