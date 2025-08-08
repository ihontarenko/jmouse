package org.jmouse.mvc;

import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpStatus;

/**
 * 🧩 Default implementation of {@link InvocationOutcome}.
 *
 * Encapsulates the outcome of a handler invocation including
 * the return value, model attributes, HTTP status, headers,
 * and execution state.
 *
 * <p>Usage example:
 * <pre>{@code
 * Outcome result = new Outcome(someReturnValue);
 * result.setHttpStatus(HttpStatus.OK);
 * result.getModel().addAttribute("key", value);
 * result.setState(ExecutionState.HANDLED);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class Outcome implements InvocationOutcome {

    private final Model           model   = new DefaultModel();
    private final Headers         headers = new Headers();
    private       Object          returnValue;
    private       HttpStatus      httpStatus;
    private       MethodParameter returnType;
    private       ExecutionState  state   = ExecutionState.UNHANDLED;

    /**
     * Constructs a new result wrapping the given return value.
     *
     * @param returnValue the raw return value from handler method
     */
    public Outcome(Object returnValue) {
        this.returnValue = returnValue;
    }

    /**
     * Returns the current execution state of this invocation result.
     *
     * @return current {@link ExecutionState}
     */
    @Override
    public ExecutionState getState() {
        return state;
    }

    /**
     * Updates the execution state of this invocation result.
     *
     * @param state new {@link ExecutionState} to set
     */
    @Override
    public void setState(ExecutionState state) {
        this.state = state;
    }

    /**
     * Returns the return value produced by the handler.
     *
     * @return the handler's return value
     */
    @Override
    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * Updates the return value produced by the handler.
     *
     * @param returnValue the new return value to set
     */
    @Override
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    /**
     * 📌 Metadata describing the controller method's return type.
     *
     * @return method parameter metadata
     */
    @Override
    public MethodParameter getReturnParameter() {
        return returnType;
    }

    /**
     * 📌 Sets the method parameter metadata related to return value.
     *
     * @param parameter method return parameter metadata
     */
    @Override
    public void setReturnParameter(MethodParameter parameter) {
        this.returnType = parameter;
    }

    /**
     * Returns the model data associated with this invocation result.
     *
     * <p>This model holds attributes that can be used for view rendering.
     *
     * @return the model container
     */
    @Override
    public Model getModel() {
        return model;
    }

    /**
     * Returns the HTTP status to be used for the response.
     *
     * @return the current {@link HttpStatus}, or {@code null} if none set
     */
    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Sets the HTTP status code for the response.
     *
     * @param status the {@link HttpStatus} to set
     */
    @Override
    public void setHttpStatus(HttpStatus status) {
        this.httpStatus = status;
    }

    /**
     * Returns the HTTP headers to include in the response.
     *
     * @return the {@link Headers} instance, never {@code null}
     */
    @Override
    public Headers getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "OUTCOME: [HANDLED: %s]".formatted(isHandled());
    }
}
