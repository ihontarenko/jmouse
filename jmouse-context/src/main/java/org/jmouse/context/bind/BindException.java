package org.jmouse.context.bind;

public class BindException extends RuntimeException {

    public BindException(String message) {
        super(message);
    }

    public BindException(String message, Throwable cause) {
        super(message, cause);
    }

    public BindException(NamePath path, Bindable<?> bindable, Throwable cause) {
        super(getExceptionMessage(path, bindable), cause);
    }

    public static String getExceptionMessage(NamePath path, Bindable<?> bindable) {
        return "Binding '%s' type to the '%s' property is failed".formatted(bindable.getType(), path);
    }

}
