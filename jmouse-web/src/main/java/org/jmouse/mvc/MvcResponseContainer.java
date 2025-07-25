package org.jmouse.mvc;

import org.jmouse.web.request.http.HttpStatus;

import java.util.Map;

/**
 * 📦 Container for holding model attributes, view name or direct return value,
 * and response status during request processing.
 * <p>
 * Used internally by {@link ReturnValueHandler}s and {@link HandlerAdapter}.
 *
 * @author Ivan Hontarenko
 * @see HandlerResult
 * @see ReturnValueHandler
 * @see HandlerAdapter
 */
public class MvcResponseContainer {

    private       HttpStatus httpStatus     = HttpStatus.OK;
    private final Model      model          = new DefaultModel();
    private       String     viewName;
    private       Object     returnValue;
    private       boolean    requestHandled = false;

    /**
     * Adds an attribute to the model.
     *
     * @param name  attribute name
     * @param value attribute value
     */
    public void addAttribute(String name, Object value) {
        this.model.addAttribute(name, value);
    }

    /**
     * Returns the model map.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Returns the name of the view to render.
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * Sets the name of the view to render.
     */
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    /**
     * Returns the return value.
     */
    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * Sets the return value directly (e.g., JSON, response body).
     */
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    /**
     * Returns the HTTP status.
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Sets the HTTP status to be returned.
     */
    public void setHttpStatus(HttpStatus status) {
        this.httpStatus = status;
    }

    /**
     * Marks this request as handled (response committed).
     */
    public void markRequestHandled() {
        this.requestHandled = true;
    }

    /**
     * Whether the request has been fully handled (no further processing needed).
     */
    public boolean isRequestHandled() {
        return requestHandled;
    }

    /**
     * Convenience method: true if either a return value or a view is set.
     */
    public boolean isResolved() {
        return returnValue != null || viewName != null;
    }
}

