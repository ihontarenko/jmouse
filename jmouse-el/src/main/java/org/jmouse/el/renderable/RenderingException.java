package org.jmouse.el.renderable;

public class RenderingException extends RuntimeException {

    public RenderingException(String message) {
        super(message);
    }

    public RenderingException(String message, Throwable cause) {
        super(message, cause);
    }

}
