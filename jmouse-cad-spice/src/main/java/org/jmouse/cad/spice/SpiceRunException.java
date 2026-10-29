package org.jmouse.cad.spice;

/** A SPICE run that did not give an answer: the program would not start, failed, or ran out of time. */
public class SpiceRunException extends RuntimeException {

    public SpiceRunException(String message) {
        super(message);
    }

    public SpiceRunException(String message, Throwable cause) {
        super(message, cause);
    }
}
