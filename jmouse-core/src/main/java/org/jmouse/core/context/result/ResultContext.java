package org.jmouse.core.context.result;

/**
 * 🎯 Execution result context.
 *
 * <p>
 * Represents the outcome of an execution step,
 * including a return value and optional error details.
 * </p>
 *
 * <p>
 * Designed to decouple execution flow
 * from exception-based error handling.
 * </p>
 */
public interface ResultContext {

    /**
     * 📦 Get execution return value.
     *
     * @param <T> expected return type
     * @return return value or {@code null}
     */
    <T> T getReturnValue();

    /**
     * ❌ Check whether errors are present.
     *
     * @return {@code true} if at least one error exists
     */
    boolean hasErrors();

    /**
     * 📋 Get all collected errors.
     *
     * @return iterable error details
     */
    Iterable<ErrorDetails> getErrors();

    /**
     * 🔎 Get error by code.
     *
     * @param code error code
     * @return matching error or {@code null}
     */
    ErrorDetails getError(String code);
}
