package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.PolitenessPolicy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.lang.Math.max;
import static org.jmouse.core.Verify.nonNull;

public final class DefaultScheduler implements JobScheduler {

    private static final String REASON_POLITENESS = "politeness";

    private final Frontier         frontier;
    private final PolitenessPolicy politeness;
    private final RetryBuffer      retryBuffer;
    private final Clock            clock;
    private final int              retryDrainBatch;
    private final Duration         maxDuration;
    private final Duration         minDuration       = Duration.ZERO;
    private final int              scanFrontierBatch = 128;

    public DefaultScheduler(
            Frontier frontier,
            PolitenessPolicy politeness,
            RetryBuffer retryBuffer,
            Clock clock,
            int retryDrainBatch,
            Duration maxDuration
    ) {
        this.frontier = nonNull(frontier, "frontier");
        this.retryBuffer = nonNull(retryBuffer, "retryBuffer");
        this.politeness = nonNull(politeness, "politeness");
        this.clock = nonNull(clock, "clock");
        this.retryDrainBatch = max(1, retryDrainBatch);
        this.maxDuration = nonNull(maxDuration, "maxParkDuration");
    }

    @Override
    public ScheduleDecision nextDecision() {
        Instant now = clock.instant();

        moveReadyRetries(now);

        for (int i = 0; i < scanFrontierBatch; i++) {
            ProcessingTask task = frontier.poll();

            if (task == null) {
                break;
            }

            Instant notBefore = politeness.notBefore(task.url(), now);

            if (notBefore != null && notBefore.isAfter(now)) {
                retryBuffer.schedule(task.schedule(notBefore), notBefore, REASON_POLITENESS, null);
                continue;
            }

            return new ScheduleDecision.TaskReady(task);
        }

        return toDecision(now);
    }

    private void moveReadyRetries(Instant now) {
        for (ProcessingTask ready : retryBuffer.drainReady(now, retryDrainBatch)) {
            frontier.offer(ready);
        }
    }

    private ScheduleDecision toDecision(Instant now) {
        ScheduleDecision decision = ScheduleDecision.Drained.INSTANCE;

        if (frontier.size() > 0 || retryBuffer.size() > 0) {
            decision = toScheduleDecision(now);
        }

        return decision;
    }

    private ScheduleDecision toScheduleDecision(Instant instant) {
        Instant nextNotBefore = retryBuffer.peekNotBefore();

        if (nextNotBefore == null) {
            Duration fallback = Duration.ofMillis(10);
            return new ScheduleDecision.Park(fallback, instant.plus(fallback));
        }

        Duration duration = Duration.between(instant, nextNotBefore);

        if (duration.isNegative()) {
            duration = minDuration;
        }

        if (duration.compareTo(maxDuration) > 0) {
            duration = maxDuration;
        }

        return new ScheduleDecision.Park(duration, nextNotBefore);
    }
}

