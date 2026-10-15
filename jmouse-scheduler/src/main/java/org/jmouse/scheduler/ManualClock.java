package org.jmouse.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A clock that moves only when told — for tests of anything driven by a {@link SchedulerRunner}, in
 * this library or in a product. Thread-safe; every copy made by {@link #withZone} shares the instant.
 */
public final class ManualClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    /**
     * @param start the initial instant
     */
    public ManualClock(Instant start) {
        this(new AtomicReference<>(Objects.requireNonNull(start, "start")), ZoneOffset.UTC);
    }

    private ManualClock(AtomicReference<Instant> instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    /** Moves the clock forward (or back, for a negative amount). */
    public void advance(Duration amount) {
        instant.updateAndGet(current -> current.plus(amount));
    }

    /** Sets the clock. */
    public void set(Instant newInstant) {
        instant.set(Objects.requireNonNull(newInstant, "newInstant"));
    }

    @Override
    public Instant instant() {
        return instant.get();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new ManualClock(instant, newZone);
    }
}
