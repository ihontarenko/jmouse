package org.jmouse.cad.packages;

import java.math.BigDecimal;

/**
 * A generated measure as the library keeps it: to the thousandth of its unit — a micrometre on a board, a thousandth of
 * a grid step on a sheet —, without the float noise sums leave.
 */
public final class Measures {

    private Measures() {
    }

    /** To the thousandth; a negative zero is zero. */
    public static double exact(double value) {
        double rounded = Math.round(value * 1000) / 1000.0;
        return rounded == 0 ? 0 : rounded;
    }

    /** As a note reads it: to the thousandth, in as few digits as say it — {@code 1.27}, {@code 3}. */
    public static String written(double value) {
        double exact = exact(value);
        if (exact == 0) {
            return "0";
        }
        return BigDecimal.valueOf(exact).stripTrailingZeros().toPlainString();
    }
}
