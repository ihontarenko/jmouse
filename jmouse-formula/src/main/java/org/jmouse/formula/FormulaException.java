package org.jmouse.formula;

/** An expression that does not parse or does not compile, with where in the source it went wrong. */
public class FormulaException extends RuntimeException {

    private final String source;
    private final int position;

    public FormulaException(String message, String source, int position) {
        super(message);
        this.source = source;
        this.position = position;
    }

    public String source() {
        return source;
    }

    /** Zero-based character offset, or -1 when the failure is about the expression as a whole. */
    public int position() {
        return position;
    }
}
