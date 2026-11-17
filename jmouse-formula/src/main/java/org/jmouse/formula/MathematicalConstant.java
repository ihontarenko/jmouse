package org.jmouse.formula;

import java.util.Arrays;
import java.util.Optional;

public enum MathematicalConstant {

    /** π: a circle's circumference over its diameter. */
    PI("pi", "\\pi", Math.PI),
    /** τ = 2π: a whole turn, in radians. */
    TAU("tau", "\\tau", 2 * Math.PI),
    /** e: the base of the natural logarithm. */
    E("e", "e", Math.E),
    /** φ: the golden ratio, (1 + √5) / 2. */
    PHI("phi", "\\varphi", (1 + Math.sqrt(5)) / 2),
    /** Infinity, for the bound of a series or of an improper integral. */
    INFINITY("inf", "\\infty", Double.POSITIVE_INFINITY);

    private final String symbol;
    private final String latex;
    private final double value;

    MathematicalConstant(String symbol, String latex, double value) {
        this.symbol = symbol;
        this.latex = latex;
        this.value = value;
    }

    public String symbol() {
        return symbol;
    }

    public String latex() {
        return latex;
    }

    public double value() {
        return value;
    }

    public static Optional<MathematicalConstant> bySymbol(String symbol) {
        return Arrays.stream(values()).filter(constant -> constant.symbol.equals(symbol)).findFirst();
    }
}
