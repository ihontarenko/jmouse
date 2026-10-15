package org.jmouse.scheduler;

/** What became of an occurrence, as history records it. */
public enum OccurrenceOutcome {

    /** Handed to the dispatcher, which accepted it. */
    FIRED,

    /** Dropped by the misfire policy after downtime. One record may stand for many occurrences. */
    SKIPPED,

    /** The trigger's gate refused it; the record carries the gate's reason. */
    REFUSED,

    /** No handler was registered for the key, or the dispatcher threw. */
    FAILED
}
