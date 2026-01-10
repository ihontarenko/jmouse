package org.jmouse.core.context;

/**
 * 🛑 Processing control flag.
 *
 * <p>
 * Indicates whether further processing in a pipeline,
 * handler chain, or interceptor flow should be stopped.
 * </p>
 *
 * <p>
 * Used as a lightweight, read-only signal — no side effects.
 * </p>
 */
public interface ProcessingControl {

    /**
     * 🚦 Check whether processing is stopped.
     *
     * @return {@code true} if further processing must be skipped
     */
    boolean isProcessingStopped();

}
