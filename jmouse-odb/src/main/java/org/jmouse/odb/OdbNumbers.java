package org.jmouse.odb;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** How the files of a job write a number: plainly, no exponent, no trailing zeros. */
final class OdbNumbers {

    /** The places a coordinate in millimetres is written to: a nanometre. */
    private static final int MILLIMETRE_PLACES = 6;

    /** The places a symbol's size in microns is written to. */
    private static final int MICRON_PLACES = 3;

    private OdbNumbers() {
    }

    /** A length in millimetres: {@code 12.7}, {@code -0.25}. */
    static String millimetres(double value) {
        return plain(value, MILLIMETRE_PLACES);
    }

    /** A length in millimetres written in microns, as a symbol's name says its size: 0.25 mm is {@code 250}. */
    static String microns(double millimetres) {
        return plain(millimetres * 1000, MICRON_PLACES);
    }

    private static String plain(double value, int places) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.signum() == 0 ? "0" : rounded.toPlainString();
    }
}
