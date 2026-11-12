package org.jmouse.eagle;

/** An Eagle file that does not read as one: what is wrong with it, said so its author can find it. */
public final class EagleReadException extends RuntimeException {

    public EagleReadException(String message) {
        super(message);
    }

    public EagleReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
