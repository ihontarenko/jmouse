package org.jmouse.common.pipeline;

public class CorruptedLinkException extends RuntimeException {
    public CorruptedLinkException() {
        super();
    }

    public CorruptedLinkException(String message) {
        super(message);
    }

    public CorruptedLinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
