package org.jmouse.mvc;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.HttpStatus;

/**
 * 🧩 Execution result returned from a {@link HandlerAdapter}.
 *
 * <p>Encapsulates the outcome of a controller invocation — including
 * return value, model data, HTTP headers, and status code.
 *
 * <p>Use {@link Outcome} for concrete implementation.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @see Outcome
 */
public interface InvocationOutcome {

    /**
     * Returns current state of the result.
     */
    ExecutionState getState();

    /**
     * Sets the execution state (e.g. {@code HANDLED} or {@code UNHANDLED}).
     */
    void setState(ExecutionState state);

    /**
     * Return value from controller method (nullable).
     */
    Object getReturnValue();

    /**
     * Sets the HTTP status to be used in the response.
     *
     * <p>If {@code null}, the default status {@code 200 OK} will be applied.
     */
    void setReturnValue(Object returnValue);

    /**
     * 📌 Metadata describing the controller method's return type.
     *
     * @return method parameter metadata
     */
    MethodParameter getReturnParameter();

    /**
     * 📌 Sets the method parameter metadata related to return value.
     *
     * @param parameter method return parameter metadata
     */
    void setReturnParameter(MethodParameter parameter);

    /**
     * Mutable model for view rendering.
     */
    Model getModel();

    /**
     * Optional HTTP status code (nullable, default = 200 OK).
     */
    HttpStatus getHttpStatus();

    /**
     * 📡 Sets HTTP status for the response.
     *
     * @param status HTTP status (nullable, default = 200 OK)
     */
    void setHttpStatus(HttpStatus status);

    /**
     * ✅ Whether this result was marked as handled.
     */
    default boolean isHandled() {
        return getState() == ExecutionState.HANDLED;
    }

    /**
     * ❌ Whether this result is unhandled.
     */
    default boolean isUnhandled() {
        return getState() == ExecutionState.UNHANDLED;
    }
}
