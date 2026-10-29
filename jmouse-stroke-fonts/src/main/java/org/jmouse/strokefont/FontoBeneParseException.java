package org.jmouse.strokefont;

/** A FontoBene text that breaks the format, refused with the line it broke on. */
public final class FontoBeneParseException extends IllegalArgumentException {

    private final int line;

    public FontoBeneParseException(int line, String detail) {
        super(detail + " (line " + line + ")");
        this.line = line;
    }

    public int line() {
        return line;
    }
}
