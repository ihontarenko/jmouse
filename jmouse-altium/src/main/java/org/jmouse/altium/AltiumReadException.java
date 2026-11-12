package org.jmouse.altium;

/** An Altium file that cannot be read: not one, or one broken past reading — said with where. */
public final class AltiumReadException extends RuntimeException {

    public AltiumReadException(String message) {
        super(message);
    }

    public AltiumReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
