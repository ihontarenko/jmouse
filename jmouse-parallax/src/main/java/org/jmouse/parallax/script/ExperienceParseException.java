package org.jmouse.parallax.script;

/**
 * A jMX document that cannot be read: which document, which line and column, and what is wrong there.
 *
 * <p>Reading refuses only what is structurally wrong — a block that never closes, a parameter without a value, a
 * block a kind does not have. Whether a function exists or a name is bound is the compiler's to refuse, the way jMS
 * keeps its parser and its binder apart.</p>
 */
public class ExperienceParseException extends IllegalArgumentException {

    private final String document;
    private final int    line;
    private final int    column;
    private final String detail;

    public ExperienceParseException(String document, int line, int column, String detail) {
        super(document + ", line " + line + (column > 0 ? ", column " + column : "") + ": " + detail);
        this.document = document;
        this.line = line;
        this.column = column;
        this.detail = detail;
    }

    public String getDocument() {
        return document;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    /** What is wrong, without the position in front of it. */
    public String getDetail() {
        return detail;
    }
}
