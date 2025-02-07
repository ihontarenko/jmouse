package org.jmouse.core.bind;

public class UnsupportedDataSourceException extends RuntimeException {

    public UnsupportedDataSourceException(String message) {
        super(message);
    }

    public UnsupportedDataSourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
