package org.jmouse.mvc;

public class ArgumentResolverException extends RuntimeException {

    public ArgumentResolverException() {
    }

    public ArgumentResolverException(String message) {
        super(message);
    }

    public ArgumentResolverException(String message, Throwable cause) {
        super(message, cause);
    }

}
