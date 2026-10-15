package org.jmouse.scheduler.spi;

import org.jmouse.scheduler.Occurrence;
import org.jmouse.scheduler.TriggerParameters;

/**
 * The work a trigger names by key — {@code scan-source}, {@code vectorise-waiting}.
 *
 * <p>Registered in a {@link org.jmouse.scheduler.HandlerRegistry}. Whether {@link #handle} runs in
 * this process or in a worker that took the occurrence off a queue is the
 * {@link TriggerDispatcher}'s decision, not the handler's.
 */
public interface TriggerHandler {

    /** The key triggers name: lower-case words joined by {@code -} or {@code .}. */
    String key();

    /**
     * Checks a trigger's parameters when the trigger is registered, so a malformed trigger is refused
     * then rather than failing at three in the morning.
     *
     * @param parameters the parameters
     * @throws IllegalArgumentException naming the parameter at fault
     */
    default void validate(TriggerParameters parameters) {
    }

    /**
     * Does the work.
     *
     * @param occurrence the occurrence, with its parameters
     */
    void handle(Occurrence occurrence);
}
