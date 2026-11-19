package org.jmouse.el.lexer;

public final class BytesCursorException extends IllegalArgumentException {
    private final String source;
    private final long offset;

    public BytesCursorException(String source, long offset, String message) {
        super(source + ":" + offset + ": " + message);
        this.source = source;
        this.offset = offset;
    }

    public String source() {
        return source;
    }

    public long offset() {
        return offset;
    }
}
