package org.jmouse.spice.expression;

/**
 * What an expression's grammar or its translation refuses, and where: the offset in the expression's text the refusal
 * stands at, which {@link SpiceExpressionLanguage} turns into the deck's line and column.
 */
final class SpiceExpressionRefusal extends RuntimeException {

    private final int offset;

    /** @param offset where in the expression's text, from 0; negative for the expression as a whole */
    SpiceExpressionRefusal(int offset, String message) {
        super(message);
        this.offset = offset;
    }

    int offset() {
        return offset;
    }
}
