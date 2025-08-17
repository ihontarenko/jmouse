package org.jmouse.web.mvc;

import org.jmouse.core.MethodParameter;

public final class MVCResult {

    private final MappedHandler   mappedHandler;
    private final Model           model = new DefaultModel();
    private       Object          returnValue;
    private       MethodParameter returnType;

    public MVCResult(Object returnValue, MethodParameter returnType, MappedHandler mappedHandler) {
        this.returnValue = returnValue;
        this.returnType = returnType;
        this.mappedHandler = mappedHandler;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public MethodParameter getReturnType() {
        return returnType;
    }

    public void setReturnType(MethodParameter returnType) {
        this.returnType = returnType;
    }

    public MappedHandler getMappedHandler() {
        return mappedHandler;
    }

    public Model getModel() {
        return model;
    }


}