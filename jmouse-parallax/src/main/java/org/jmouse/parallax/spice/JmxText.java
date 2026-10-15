package org.jmouse.parallax.spice;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * How the converter spells things in jMX: quantities with their prefixes ({@code 4.7 nF}, {@code 1 kOhm}), texts in
 * quotes, and names that jMX reads as one word.
 */
final class JmxText {

    /** Prefixes by power of a thousand, from 10⁻¹⁵ to 10¹², as {@code Quantity} reads them. */
    private static final Map<Integer, String> PREFIXES = Map.of(-15, "f", -12, "p", -9, "n", -6, "u", -3, "m", 0, "",
            3, "k", 6, "M", 9, "G", 12, "T");

    /** Units that take no prefix: a temperature in kelvin reads better as {@code 298.15 K} than {@code 0.29815 kK}. */
    private static final Set<String> UNPREFIXED = Set.of("", "K", "rad", "%");

    /** Significant digits a value is written with: a datasheet's figure survives, the last bit of a product does not. */
    private static final MathContext DIGITS = new MathContext(15);

    private JmxText() {
    }

    /** {@code 4.7e-9} in {@code F} → {@code 4.7 nF}; a value without a unit is a plain number. */
    static String quantity(double value, String unit) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("A jMX value is finite, not " + value + ".");
        }
        if (value == 0 || UNPREFIXED.contains(unit)) {
            return number(value) + (unit.isEmpty() ? "" : " " + unit);
        }
        int power = (int) Math.floor(Math.log10(Math.abs(value)) / 3) * 3;
        power = Math.max(-15, Math.min(12, power));
        BigDecimal scaled = new BigDecimal(value).round(DIGITS).movePointLeft(power).stripTrailingZeros();
        return scaled.toPlainString() + " " + PREFIXES.get(power) + unit;
    }

    /** The shortest plain decimal that reads back to the same {@code double}. */
    static String number(double value) {
        if (value == 0) {
            return "0";
        }
        return new BigDecimal(value).round(DIGITS).stripTrailingZeros().toPlainString();
    }

    /** A text in double quotes; a double quote inside it becomes a single one, which jMX reads inside double quotes. */
    static String quoted(String text) {
        return "\"" + text.replace('"', '\'').replace('\n', ' ').strip() + "\"";
    }

    /**
     * Names that jMX reads as one word: a SPICE node or element name made into {@code [A-Za-z_][A-Za-z0-9_]*}, kept
     * distinct, each renaming remembered so a note can list them. {@code 0} is ground and stays {@code 0}.
     */
    static final class Names {

        private final Map<String, String> chosen  = new LinkedHashMap<>();
        private final Map<String, String> renamed = new LinkedHashMap<>();
        private final Set<String>         taken   = new HashSet<>();
        private final String prefix;

        /** @param prefix what a name that starts with a digit is given in front: {@code n} for a node */
        Names(String prefix) {
            this.prefix = prefix;
        }

        /** Takes a name before any other is chosen, so a renaming never lands on it. */
        void reserve(String name) {
            taken.add(name);
            chosen.put(name, name);
        }

        String of(String written) {
            if (written.equals("0")) {
                return "0";
            }
            String known = chosen.get(written);
            if (known != null) {
                return known;
            }
            String name = written.replaceAll("[^A-Za-z0-9_]", "_");
            if (name.isEmpty() || !Character.isLetter(name.charAt(0)) && name.charAt(0) != '_') {
                name = prefix + name;
            }
            if (name.length() > 32) {
                name = name.substring(0, 32);
            }
            String candidate = name;
            int counter = 2;
            while (taken.contains(candidate)) {
                candidate = name + "_" + counter++;
            }
            taken.add(candidate);
            chosen.put(written, candidate);
            if (!candidate.equals(written)) {
                renamed.put(written, candidate);
            }
            return candidate;
        }

        /** Each name that had to change → what it became, in the order met. */
        Map<String, String> renamed() {
            return renamed;
        }
    }
}
