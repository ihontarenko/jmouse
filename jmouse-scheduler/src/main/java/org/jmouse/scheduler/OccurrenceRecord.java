package org.jmouse.scheduler;

import java.time.Instant;
import java.util.Objects;

/**
 * One line of a trigger's history.
 *
 * @param triggerId   the trigger
 * @param scheduledAt the scheduled time — for a {@link OccurrenceOutcome#SKIPPED} record standing for
 *                    several occurrences, the earliest of them
 * @param recordedAt  when the runner recorded it
 * @param outcome     what became of it
 * @param count       how many occurrences this record stands for; above one only for
 *                    {@link OccurrenceOutcome#SKIPPED}
 * @param reason      why, in words — the gate's reason, the misfire summary, the dispatch failure; empty
 *                    for {@link OccurrenceOutcome#FIRED}
 */
public record OccurrenceRecord(
        TriggerId triggerId,
        Instant scheduledAt,
        Instant recordedAt,
        OccurrenceOutcome outcome,
        long count,
        String reason
) {

    /** The longest reason a record keeps; longer ones are cut, so a store column can be bounded. */
    public static final int MAXIMUM_REASON_LENGTH = 1000;

    public OccurrenceRecord {
        Objects.requireNonNull(triggerId, "triggerId");
        Objects.requireNonNull(scheduledAt, "scheduledAt");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(outcome, "outcome");

        if (count < 1) {
            throw new IllegalArgumentException("A history record stands for at least one occurrence.");
        }

        reason = reason == null ? "" : reason;

        if (reason.length() > MAXIMUM_REASON_LENGTH) {
            reason = reason.substring(0, MAXIMUM_REASON_LENGTH - 1) + "…";
        }
    }
}
