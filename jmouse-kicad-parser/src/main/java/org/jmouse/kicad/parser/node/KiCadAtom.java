package org.jmouse.kicad.parser.node;

/**
 * An unquoted word of a KiCad file: a keyword ({@code wire}, {@code yes}), a number ({@code 1.27}, {@code -90}), or a
 * bare identifier — a UUID, as KiCad 7 wrote them.
 */
public record KiCadAtom(String value, KiCadPosition position, String leading) implements KiCadNode {

    public KiCadAtom {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("An atom has a value.");
        }
    }

    /** An atom a program built. */
    public static KiCadAtom of(String value) {
        return new KiCadAtom(value, KiCadPosition.NONE, null);
    }

    /** A number as KiCad writes one: no exponent, no trailing zeros, a whole one without a point. */
    public static KiCadAtom of(double number) {
        return of(KiCadNumbers.written(number));
    }

    /** Whether it reads as a number. */
    public boolean isNumber() {
        return KiCadNumbers.isNumber(value);
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
