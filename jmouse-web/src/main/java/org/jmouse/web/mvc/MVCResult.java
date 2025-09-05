package org.jmouse.web.mvc;

import org.jmouse.core.MethodParameter;

/**
 * 🎯 Represents the outcome of invoking an MVC handler.
 *
 * <p>Encapsulates:
 * <ul>
 *   <li>Handler mapping info</li>
 *   <li>Returned value and its type</li>
 *   <li>Associated {@link Model}</li>
 * </ul>
 * </p>
 */
public final class MVCResult {

    /**
     * 🔗 The handler that was matched and invoked.
     */
    private final MappedHandler mappedHandler;

    /**
     * 📑 The model associated with this execution (always present).
     */
    private final Model model = new DefaultModel();

    /**
     * 🎁 The actual return value produced by the handler.
     */
    private Object returnValue;

    /**
     * 🏷️ Metadata describing the return type.
     */
    private MethodParameter returnType;

    /**
     * 🏗️ Create a new MVC result.
     *
     * @param returnValue   the handler’s return value
     * @param returnType    the declared return type
     * @param mappedHandler the matched handler
     */
    public MVCResult(Object returnValue, MethodParameter returnType, MappedHandler mappedHandler) {
        this.returnValue = returnValue;
        this.returnType = returnType;
        this.mappedHandler = mappedHandler;
    }

    /**
     * @return the raw handler return value
     */
    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * @param returnValue set a new return value
     */
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    /**
     * @return metadata about the declared return type
     */
    public MethodParameter getReturnType() {
        return returnType;
    }

    /**
     * @param returnType set new return type metadata
     */
    public void setReturnType(MethodParameter returnType) {
        this.returnType = returnType;
    }

    /**
     * @return the matched handler
     */
    public MappedHandler getMappedHandler() {
        return mappedHandler;
    }

    /**
     * @return the model associated with this result
     */
    public Model getModel() {
        return model;
    }

}
