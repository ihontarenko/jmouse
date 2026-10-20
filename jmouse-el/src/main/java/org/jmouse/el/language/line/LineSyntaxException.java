package org.jmouse.el.language.line;

/** A document of a line language that does not read, with where it stops reading: its name, 1-based line and column. */
public class LineSyntaxException extends RuntimeException {

    private final String document;
    private final int line;
    private final int column;
    private final String detail;

    public LineSyntaxException(String document, int line, int column, String detail) {
        super(document + ", line " + line + ", column " + column + ": " + detail);
        this.document = document;
        this.line = line;
        this.column = column;
        this.detail = detail;
    }

    public String document() {
        return document;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    /** What is wrong, without where. */
    public String detail() {
        return detail;
    }
}
