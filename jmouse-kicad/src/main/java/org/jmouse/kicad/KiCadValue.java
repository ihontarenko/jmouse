package org.jmouse.kicad;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The number a symbol's value writes, as a schematic's author writes it: {@code 100nF}, {@code 10uF}, {@code 1K},
 * {@code 2.2u}, and the letter-for-the-point notation of resistors and capacitors — {@code 4K7} is 4.7 kΩ,
 * {@code 0R1} is 0.1 Ω, {@code 00R5} is 0.5 Ω. What follows a space is a rating, not the value: {@code 0R1 5W},
 * {@code 1mF 50V}. A part's name ({@code NE5532}), a range ({@code 5-10K}) or a bare letter ({@code R}) is no number.
 */
public final class KiCadValue {

    /** {@code 2.2u}, {@code 100nF}, {@code 1K}, {@code 15mF}: a number, a multiplier, the unit's letters. */
    private static final Pattern DECIMAL = Pattern.compile("(\\d+(?:[.,]\\d+)?)([pnuµmkKMGT]?)(?:F|H|Ω|Ohm|ohm|R|V|A)?");

    /** {@code 4K7}, {@code 0R1}, {@code 2n2}: the multiplier stands where the decimal point does. */
    private static final Pattern LETTER_FOR_POINT = Pattern.compile("(\\d*)([RpnuµmkKMGT])(\\d+)(?:F|H|Ω)?");

    /** Each multiplier as a power of ten; {@code R} is one, a resistor's point. */
    private static final Map<String, Integer> POWERS = Map.ofEntries(Map.entry("", 0), Map.entry("R", 0),
            Map.entry("p", -12), Map.entry("n", -9), Map.entry("u", -6), Map.entry("µ", -6), Map.entry("m", -3),
            Map.entry("k", 3), Map.entry("K", 3), Map.entry("M", 6), Map.entry("G", 9), Map.entry("T", 12));

    private KiCadValue() {
    }

    /** The value's number, its multiplier applied; empty when what is written is not a number. */
    public static Optional<Double> number(String written) {
        if (written == null || written.isBlank()) {
            return Optional.empty();
        }
        String value = written.strip().split("\\s+")[0];
        Matcher decimal = DECIMAL.matcher(value);
        if (decimal.matches()) {
            return Optional.of(scaled(decimal.group(1).replace(',', '.'), decimal.group(2)));
        }
        Matcher letter = LETTER_FOR_POINT.matcher(value);
        if (letter.matches()) {
            String whole = letter.group(1).isEmpty() ? "0" : letter.group(1);
            return Optional.of(scaled(whole + "." + letter.group(3), letter.group(2)));
        }
        return Optional.empty();
    }

    /** In decimal, so {@code 4K7} is exactly the double nearest 4700. */
    private static double scaled(String digits, String multiplier) {
        return new BigDecimal(digits).scaleByPowerOfTen(POWERS.get(multiplier)).doubleValue();
    }
}
