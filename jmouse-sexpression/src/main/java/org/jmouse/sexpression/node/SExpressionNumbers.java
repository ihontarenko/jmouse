package org.jmouse.sexpression.node;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Numbers as KiCad writes them: {@code 1.27}, {@code -90}, {@code 0} — never an exponent. */
public final class SExpressionNumbers {

    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)");

    /** The most fractional digits KiCad writes: its internal unit is a nanometre, a millimetre's sixth decimal. */
    private static final int DECIMALS = 6;

    private SExpressionNumbers() {
    }

    public static boolean isNumber(String written) {
        return written != null && NUMBER.matcher(written).matches();
    }

    /** A number as KiCad writes it: at most six decimals, no trailing zeros, a whole one without a point. */
    public static String written(double number) {
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException(number + " is no number an S-expression file can hold.");
        }
        BigDecimal rounded = BigDecimal.valueOf(number).setScale(DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.signum() == 0 ? "0" : rounded.toPlainString();
    }
}
