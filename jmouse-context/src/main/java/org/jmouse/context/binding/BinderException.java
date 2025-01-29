package org.jmouse.context.binding;

public class BinderException extends RuntimeException{

    public BinderException(String message) {
        super(message);
    }

    public BinderException(String message, Throwable cause) {
        super(message, cause);
    }

}
