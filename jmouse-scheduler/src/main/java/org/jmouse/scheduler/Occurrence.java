package org.jmouse.scheduler;

import java.time.Instant;
import java.util.Objects;

/**
 * One firing of one trigger: what a gate evaluates and a dispatcher hands on.
 *
 * <p>{@link #identity()} is unique per trigger and scheduled time, and stable across processes and
 * re-dispatches. Firing is at-least-once — a runner that dies before confirming has its occurrences
 * dispatched again, with the same identity, once its lease expires — so ⚠️ <strong>a dispatcher must be
 * idempotent on the identity</strong>: a durable queue puts a unique constraint on it and treats a
 * duplicate insert as already done.
 *
 * @param triggerId    the trigger
 * @param handlerKey   the handler to run
 * @param parameters   the trigger's parameters at the time of firing
 * @param scheduledAt  when the schedule said it was due
 * @param firedAt      when the runner fired it; later than {@code scheduledAt} by the polling delay, or
 *                     by the downtime for a late occurrence
 * @param late         whether it fired under the misfire policy rather than on time
 */
public record Occurrence(
        TriggerId triggerId,
        String handlerKey,
        TriggerParameters parameters,
        Instant scheduledAt,
        Instant firedAt,
        boolean late
) {

    public Occurrence {
        Objects.requireNonNull(triggerId, "triggerId");
        Objects.requireNonNull(handlerKey, "handlerKey");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(scheduledAt, "scheduledAt");
        Objects.requireNonNull(firedAt, "firedAt");
    }

    /** {@code <trigger>@<scheduled epoch milliseconds>} — the idempotency key of this occurrence. */
    public String identity() {
        return triggerId.value() + "@" + scheduledAt.toEpochMilli();
    }
}
