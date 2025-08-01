package org.jmouse.mvc;

import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpStatus;

public class HandlerExecutionResult implements ExecutionResult {

    private final Model          model   = new DefaultModel();
    private       Object         returnValue;
    private final Headers        headers = new Headers();
    private       HttpStatus     httpStatus;
    private       ExecutionState state   = ExecutionState.UNHANDLED;

    public HandlerExecutionResult(Object returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public ExecutionState getState() {
        return state;
    }

    @Override
    public void setState(ExecutionState state) {
        this.state = state;
    }

    @Override
    public Object getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public void setHttpStatus(HttpStatus status) {
        this.httpStatus = status;
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

}
