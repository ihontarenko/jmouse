package org.jmouse.el.evaluation;

public class RenderingException extends RuntimeException {

    public RenderingException(String message) {
        super(message);
    }

    public RenderingException(String message, Throwable cause) {
        super(message, cause);
    }

}
