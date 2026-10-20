package org.jmouse.units;

import java.util.Arrays;
import java.util.Optional;

/**
 * The SI prefixes a datasheet writes in front of a unit: {@code n} in {@code nF}, {@code k} in {@code kOhm}. The letters
 * are case-sensitive, as SI writes them — {@code m} is a thousandth, {@code M} a million; a language that reads them
 * otherwise, as SPICE does, maps its own letters onto these factors.
 */
public enum SiPrefix {

    FEMTO('f', 1e-15),
    PICO('p', 1e-12),
    NANO('n', 1e-9),
    MICRO('u', 1e-6),
    MILLI('m', 1e-3),
    KILO('k', 1e3),
    MEGA('M', 1e6),
    GIGA('G', 1e9),
    TERA('T', 1e12);

    private final char symbol;
    private final double factor;

    SiPrefix(char symbol, double factor) {
        this.symbol = symbol;
        this.factor = factor;
    }

    /** The letter as a unit is read: {@code u} for micro, which a datasheet prints {@code µ}. */
    public char symbol() {
        return symbol;
    }

    public double factor() {
        return factor;
    }

    /** The prefix written with that letter; {@code µ} and {@code μ} are micro too. */
    public static Optional<SiPrefix> of(char symbol) {
        char read = Units.asRead(String.valueOf(symbol)).charAt(0);
        return Arrays.stream(values()).filter(prefix -> prefix.symbol == read).findFirst();
    }
}
