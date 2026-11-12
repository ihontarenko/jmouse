package org.jmouse.mesh;

/** A file that does not read as the format it is said to be, with where it stops when that is known. */
public class MeshReadException extends RuntimeException {

    public MeshReadException(String message) {
        super(message);
    }

    public MeshReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
