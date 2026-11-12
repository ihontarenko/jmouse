package org.jmouse.sexpression.node;

/**
 * An unquoted word of an S-expression file: a keyword ({@code wire}, {@code yes}), a number ({@code 1.27}, {@code -90}), or a
 * bare identifier — a UUID, as KiCad 7 wrote them.
 */
public record SExpressionAtom(String value, SExpressionPosition position, String leading) implements SExpressionNode {

    public SExpressionAtom {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("An atom has a value.");
        }
    }

    /** An atom a program built. */
    public static SExpressionAtom of(String value) {
        return new SExpressionAtom(value, SExpressionPosition.NONE, null);
    }

    /** A number as KiCad writes one: no exponent, no trailing zeros, a whole one without a point. */
    public static SExpressionAtom of(double number) {
        return of(SExpressionNumbers.written(number));
    }

    /** Whether it reads as a number. */
    public boolean isNumber() {
        return SExpressionNumbers.isNumber(value);
    }

    /**
     * The number it writes.
     *
     * @throws IllegalArgumentException when it is not one
     */
    public double number() {
        if (!isNumber()) {
            throw new IllegalArgumentException("'" + value + "' at " + position + " is not a number.");
        }
        return Double.parseDouble(value);
    }
}
