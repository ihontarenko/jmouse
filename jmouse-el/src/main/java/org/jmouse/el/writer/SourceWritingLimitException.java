package org.jmouse.el.writer;

public final class SourceWritingLimitException extends IllegalStateException {
    private final int maximumCharacters;
    private final long requestedCharacters;

    public SourceWritingLimitException(int maximumCharacters, long requestedCharacters) {
        super(
                "Source output requires "
                        + requestedCharacters
                        + " UTF-16 units; limit is "
                        + maximumCharacters);
        this.maximumCharacters = maximumCharacters;
        this.requestedCharacters = requestedCharacters;
    }

    public int maximumCharacters() {
        return maximumCharacters;
    }

    public long requestedCharacters() {
        return requestedCharacters;
    }
}
