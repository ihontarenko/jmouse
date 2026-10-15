package org.jmouse.scheduler.spi;

import org.jmouse.scheduler.Trigger;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A trigger as a store holds it: the definition plus the state the claim turns on.
 *
 * @param trigger    the definition
 * @param nextFireAt the next occurrence not yet confirmed, or {@code null} when the schedule is exhausted
 * @param version    incremented by every write; a claim and a confirmation succeed only against the
 *                   version they expect
 * @param plannedAt  when a lease exists, the instant the claiming runner planned from — so a runner taking
 *                   over an expired lease replays the same plan and dispatches the same occurrences;
 *                   {@code null} when nothing is in flight
 */
public record ScheduledTrigger(Trigger trigger, Instant nextFireAt, long version, Instant plannedAt) {

    public ScheduledTrigger {
        Objects.requireNonNull(trigger, "trigger");
    }

    /** The next occurrence, or empty when the schedule has no more. */
    public Optional<Instant> nextFire() {
        return Optional.ofNullable(nextFireAt);
    }

    /**
     * Whether a claim on this trigger was never confirmed. A store only returns such a trigger from
     * {@link TriggerStore#findDue} once the lease has expired.
     */
    public boolean inFlight() {
        return plannedAt != null;
    }
}
