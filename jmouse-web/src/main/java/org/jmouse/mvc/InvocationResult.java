package org.jmouse.mvc;

import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpStatus;

/**
 * 🧩 Execution result returned from a {@link HandlerAdapter}.
 *
 * Represents a structured handler result including return value, model,
 * headers, status, and metadata for further processing.
 *
 * Use {@link DefaultInvocationResult} for construction.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @see DefaultInvocationResult
 */
public interface InvocationResult {

    /**
     * Returns current state of the result.
     */
    ExecutionState getState();

    /**
     * Sets the state of execution, such as {@code HANDLED} or {@code UNHANDLED}.
     */
    void setState(ExecutionState state);

    /**
     * Return value from controller method (can be null).
     */
    Object getReturnValue();

    void setReturnValue(Object returnValue);

    /**
     * Mutable model for view rendering.
     */
    Model getModel();

    /**
     * HTTP headers.
     */
    Headers getHeaders();

    /**
     * Optional HTTP status (null = 200 OK).
     */
    HttpStatus getHttpStatus();

    void setHttpStatus(HttpStatus status);

    /**
     * Utility: whether this result was marked as handled.
     */
    default boolean isHandled() {
        return getState() == ExecutionState.HANDLED;
    }

    /**
     * Utility: whether this result is unhandled.
     */
    default boolean isUnhandled() {
        return getState() == ExecutionState.UNHANDLED;
    }

}
