package org.jmouse.grabber;

/**
 * 💥 The module's own unchecked failure.
 *
 * <p>Thrown where something could not be done at all — a transport failure, an unreadable document,
 * a journal that cannot be written. It is deliberately <b>not</b> thrown for an unwelcome HTTP status:
 * deciding what a 404 or a 503 means belongs to the run, not to the machinery, and that decision
 * arrives as a {@link Verdict} instead.</p>
 */
public class GrabberException extends RuntimeException {

    public GrabberException(String message) {
        super(message);
    }

    public GrabberException(String message, Throwable cause) {
        super(message, cause);
    }

}
