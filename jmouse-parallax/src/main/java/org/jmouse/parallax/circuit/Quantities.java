package org.jmouse.parallax.circuit;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a value as a datasheet writes it — {@code 2.52 nA}, {@code 0.3 V/µs}, {@code 100 kΩ}, {@code 52.4%} — into
 * SI. The unit names the quantity for the reader; only its prefix and a {@code %} change the number.
 */
public final class Quantities {

    /** One unit with its prefix and power: {@code kΩ}, {@code µF}, {@code °C}, {@code V^2}. */
    private static final String UNIT = "[fpnµμumkMGT]?(?:Ohm|Hz|dB|°C|rad|deg|ppm|div|bit|V|A|Ω|F|H|s|W|K|S|C|J|°)(?:\\^\\d+|[²³])?";

    /**
     * A number and, after it, a unit or nothing — {@code 2.52 nA}, {@code 0.3 V/µs}, {@code 52.4%}, {@code 25}. Nothing
     * else may follow: the tail was {@code (.*)} once, so {@code 1 / (1 / f)} read as the number 1 with the unit
     * «/ (1 / f)» and a frequency became 1 Hz without a word, and {@code 2pi} read as 2. Text that is not a quantity is
     * an expression, and is evaluated as one.
     */
    private static final Pattern NUMBER = Pattern.compile(
            "^([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)\\s*(%|" + UNIT + "(?:\\s*/\\s*" + UNIT + ")?)?$");

    private static final Map<Character, Double> PREFIXES = Map.ofEntries(
            Map.entry('f', 1e-15), Map.entry('p', 1e-12), Map.entry('n', 1e-9), Map.entry('µ', 1e-6),
            Map.entry('μ', 1e-6), Map.entry('u', 1e-6), Map.entry('m', 1e-3), Map.entry('k', 1e3),
            Map.entry('M', 1e6), Map.entry('G', 1e9), Map.entry('T', 1e12));

    /** Every unit {@link #UNIT} admits, so that a prefix scales each of them alike — {@code mrad} as {@code mV}. */
    private static final Set<String> UNITS = Set.of(
            "V", "A", "Ω", "Ohm", "F", "H", "Hz", "s", "W", "K", "S", "C", "J", "dB", "°", "°C", "rad", "deg", "ppm",
            "div", "bit");

    private Quantities() {
    }

    /** True when the text is a number with, at most, a unit after it. */
    public static boolean isQuantity(String text) {
        return NUMBER.matcher(text.strip()).matches();
    }

    /** The value in SI; refuses text that is not a number with, at most, a unit. */
    public static double parse(String text) {
        Matcher matcher = NUMBER.matcher(text.strip());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("'" + text + "' is not a value: a value is a number and, at most, its unit");
        }
        String unit = matcher.group(2) == null ? "" : matcher.group(2).replace(" ", "");
        return Double.parseDouble(matcher.group(1)) * scale(unit);
    }

    /** What a unit such as {@code mV}, {@code V/µs} or {@code %} multiplies a number by. */
    static double scale(String unit) {
        if (unit.isEmpty()) {
            return 1;
        }
        if (unit.equals("%")) {
            return 0.01;
        }
        int slash = unit.indexOf('/');
        if (slash >= 0) {
            return prefixOf(unit.substring(0, slash)) / prefixOf(unit.substring(slash + 1));
        }
        return prefixOf(unit);
    }

    /** A prefixed unit's factor, raised to the unit's power: {@code mV²} and {@code mV^2} are both 10⁻⁶ V². */
    private static double prefixOf(String unit) {
        String symbol = unit.replaceAll("\\^\\d+$|[²³]$", "");
        if (symbol.length() > 1 && PREFIXES.containsKey(symbol.charAt(0)) && UNITS.contains(symbol.substring(1))) {
            return Math.pow(PREFIXES.get(symbol.charAt(0)), powerOf(unit));
        }
        return 1;
    }

    private static int powerOf(String unit) {
        if (unit.endsWith("²")) {
            return 2;
        }
        if (unit.endsWith("³")) {
            return 3;
        }
        int caret = unit.indexOf('^');
        return caret < 0 ? 1 : Integer.parseInt(unit.substring(caret + 1));
    }
}
