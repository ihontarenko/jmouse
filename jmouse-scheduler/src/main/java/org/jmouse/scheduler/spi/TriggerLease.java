package org.jmouse.scheduler.spi;

import org.jmouse.scheduler.TriggerId;

import java.time.Instant;
import java.util.Objects;

/**
 * A successful claim: the right to dispatch a trigger's planned occurrences until the lease expires,
 * and to {@link TriggerStore#confirm confirm} them.
 *
 * @param triggerId the trigger
 * @param version   the version the claim wrote; the confirmation succeeds only while it is still current
 * @param plannedAt the instant the occurrences were planned from
 */
public record TriggerLease(TriggerId triggerId, long version, Instant plannedAt) {

    public TriggerLease {
        Objects.requireNonNull(triggerId, "triggerId");
        Objects.requireNonNull(plannedAt, "plannedAt");
    }
}
