package org.jmouse.kicad;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The number a symbol's value writes, as a schematic's author writes it: {@code 100nF}, {@code 10uF}, {@code 1K},
 * {@code 2.2u}, and the letter-for-the-point notation of resistors and capacitors — {@code 4K7} is 4.7 kΩ,
 * {@code 0R1} is 0.1 Ω, {@code 00R5} is 0.5 Ω, and a rail's {@code 3V3} is 3.3 V. A power symbol's value carries its
 * sign: {@code +48V}, {@code -12V}. What follows a space is a rating, not the value: {@code 0R1 5W},
 * {@code 1mF 50V}. A part's name ({@code NE5532}), a range ({@code 5-10K}) or a bare letter ({@code R}) is no number.
 */
public final class KiCadValue {

    /** {@code 2.2u}, {@code 100nF}, {@code 1K}, {@code 15mF}: a number, a multiplier, the unit's letters. */
    private static final Pattern DECIMAL = Pattern.compile("(\\d+(?:[.,]\\d+)?)([pnuµmkKMGT]?)(?:F|H|Ω|Ohm|ohm|R|V|A)?");

    /** {@code 4K7}, {@code 0R1}, {@code 2n2}: the multiplier stands where the decimal point does. */
    private static final Pattern LETTER_FOR_POINT = Pattern.compile("(\\d*)([RVpnuµmkKMGT])(\\d+)(?:F|H|Ω)?");

    /** Each multiplier as a power of ten; {@code R} and {@code V} are one, a resistor's and a rail's point. */
    private static final Map<String, Integer> POWERS = Map.ofEntries(Map.entry("", 0), Map.entry("R", 0), Map.entry("V", 0),
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
        double sign = 1;
        if (value.startsWith("+") || value.startsWith("-")) {
            sign = value.startsWith("-") ? -1 : 1;
            value = value.substring(1);
        }
        Matcher decimal = DECIMAL.matcher(value);
        if (decimal.matches()) {
            return Optional.of(sign * scaled(decimal.group(1).replace(',', '.'), decimal.group(2)));
        }
        Matcher letter = LETTER_FOR_POINT.matcher(value);
        if (letter.matches()) {
            String whole = letter.group(1).isEmpty() ? "0" : letter.group(1);
            return Optional.of(sign * scaled(whole + "." + letter.group(3), letter.group(2)));
        }
        return Optional.empty();
    }

    /**
     * A rail's voltage, as a power symbol's value writes it — {@code +48V}, {@code -12V}, {@code 3V3}, {@code +5} —;
     * empty for a value in another unit or a name: {@code +5F} is a rail called so, not five farads.
     */
    public static Optional<Double> volts(String written) {
        if (written == null || !VOLTS.matcher(written.strip()).matches()) {
            return Optional.empty();
        }
        return number(written);
    }

    /** A voltage written alone: a sign, digits, a multiplier, {@code V} or the {@code V} that stands for the point. */
    private static final Pattern VOLTS = Pattern.compile("[+-]?(?:\\d+(?:[.,]\\d+)?[mkK]?V?|\\d*V\\d+)");

    /** The multipliers a value is written with, a power of ten each, from the smallest. */
    private static final List<String> MULTIPLIERS = List.of("p", "n", "u", "m", "", "k", "M", "G", "T");
    private static final int SMALLEST_POWER = -12;

    /**
     * A number as a schematic's author writes a value: three significant digits at most, its multiplier a letter —
     * {@code 10k}, {@code 100n}, {@code 4.7u}, {@code 470}, {@code 2.2M}. What {@link #number} reads back.
     */
    public static String written(double number) {
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException(number + " is no value a schematic can write.");
        }
        if (number == 0) {
            return "0";
        }
        BigDecimal value = new BigDecimal(number).round(new MathContext(3));
        int power = Math.floorDiv(value.precision() - value.scale() - 1, 3) * 3;
        power = Math.max(SMALLEST_POWER, Math.min(SMALLEST_POWER + 3 * (MULTIPLIERS.size() - 1), power));
        BigDecimal mantissa = value.scaleByPowerOfTen(-power).stripTrailingZeros();
        return mantissa.toPlainString() + MULTIPLIERS.get((power - SMALLEST_POWER) / 3);
    }

    /** In decimal, so {@code 4K7} is exactly the double nearest 4700. */
    private static double scaled(String digits, String multiplier) {
        return new BigDecimal(digits).scaleByPowerOfTen(POWERS.get(multiplier)).doubleValue();
    }
}
