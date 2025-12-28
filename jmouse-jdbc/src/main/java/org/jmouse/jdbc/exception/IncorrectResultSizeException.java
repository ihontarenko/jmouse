package org.jmouse.jdbc.exception;

public class IncorrectResultSizeException extends DataAccessException {

    private final int expectedSize;
    private final int actualSize;

    public IncorrectResultSizeException(int expectedSize, int actualSize, String message) {
        super(message);
        this.expectedSize = expectedSize;
        this.actualSize = actualSize;
    }

    public int getExpectedSize() {
        return expectedSize;
    }

    public int getActualSize() {
        return actualSize;
    }

}
