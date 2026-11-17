package org.jmouse.libre.pcb;

/** A LibrePCB project that cannot be read: a file missing, or one that does not read, said in the reader's words. */
public class LibrePcbReadException extends IllegalArgumentException {

    public LibrePcbReadException(String message) {
        super(message);
    }

    public LibrePcbReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
