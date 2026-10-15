package org.jmouse.scheduler.spi;

import org.jmouse.scheduler.Occurrence;

/**
 * A condition evaluated before every dispatch of the triggers that name it — "the budget preflight
 * must pass".
 *
 * <p>A refusal is recorded as a {@link org.jmouse.scheduler.OccurrenceOutcome#REFUSED} occurrence with
 * the verdict's reason, and the occurrence is not dispatched. ⚠️ Gates fail closed: a gate that throws,
 * and a gate key nothing is registered under, both refuse.
 */
public interface TriggerGate {

    /** The key triggers name: lower-case words joined by {@code -} or {@code .}. */
    String key();

    /**
     * @param occurrence the occurrence about to be dispatched, with the trigger's parameters
     * @return whether it may go
     */
    GateVerdict evaluate(Occurrence occurrence);
}
