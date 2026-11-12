package org.jmouse.sexpression.node;

/**
 * Where something stands in an S-expression file: its 1-based line and column, as an editor counts them.
 *
 * @param line   the 1-based line; 0 for something a program built rather than read
 * @param column the 1-based column; 0 when only the line is known
 */
public record SExpressionPosition(int line, int column) {

    /** The position of a node a program built rather than read. */
    public static final SExpressionPosition NONE = new SExpressionPosition(0, 0);

    @Override
    public String toString() {
        return column > 0 ? "line " + line + ":" + column : "line " + line;
    }
}
