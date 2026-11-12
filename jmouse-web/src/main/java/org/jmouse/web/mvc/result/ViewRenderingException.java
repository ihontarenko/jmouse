package org.jmouse.web.mvc.result;

/** A server-side presentation failure, never a missing HTTP route. */
public final class ViewRenderingException extends RuntimeException {

    public ViewRenderingException(String message) {
        super(message);
    }

    public ViewRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
