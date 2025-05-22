package org.jmouse.el.extension;

public class IllegalLambdaExecutionException extends ExtensionException {

    public IllegalLambdaExecutionException(String message) {
        super(message);
    }

    public IllegalLambdaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

}
