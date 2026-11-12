package org.jmouse.sexpression;

import org.jmouse.sexpression.node.SExpressionPosition;

/** An S-expression file that does not read, with where it stops reading. */
public final class SExpressionParseException extends RuntimeException {

    private final SExpressionPosition position;

    public SExpressionParseException(String file, SExpressionPosition position, String detail) {
        super(file + ", " + position + ": " + detail);
        this.position = position;
    }

    public SExpressionPosition position() {
        return position;
    }
}
