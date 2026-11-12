package org.jmouse.compound;

/** A compound file that cannot be read: not one at all, or one whose tables point somewhere it does not reach. */
public final class CompoundFileException extends RuntimeException {

    public CompoundFileException(String message) {
        super(message);
    }
}
