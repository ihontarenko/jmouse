package org.jmouse.core.process;

/** A piped program that could not be run, or whose answer could not be read — said in the program's name. */
public class PipedProgramException extends RuntimeException {

    public PipedProgramException(String message) {
        super(message);
    }

    public PipedProgramException(String message, Throwable cause) {
        super(message, cause);
    }
}
