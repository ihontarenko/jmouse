package org.jmouse.gerber;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** The precision the formats keep a decimal to: an aperture's parameter to a nanometre, a drill coordinate to a micrometre. */
public final class GerberNumbers {

    /** How many decimals an aperture's parameter keeps: a nanometre. */
    public static final int APERTURE_DECIMALS = 6;

    /** How many decimals a drill coordinate keeps: a micrometre. */
    public static final int DRILL_DECIMALS = 3;

    private GerberNumbers() {
    }

    /** A value kept to this many decimals, trailing zeros dropped. */
    public static BigDecimal rounded(double value, int decimals) {
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
