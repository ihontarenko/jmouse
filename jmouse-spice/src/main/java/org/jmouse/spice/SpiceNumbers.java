package org.jmouse.spice;

import org.jmouse.units.SiPrefix;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Numbers as SPICE writes them: a decimal, then an optional scale factor, then any letters SPICE ignores.
 *
 * <h2>What is read, decided once</h2>
 * <ul>
 *     <li>The scale factors are SPICE's, compared without regard to case: {@code T} 10¹², {@code G} 10⁹, {@code MEG}
 *     10⁶, {@code K} 10³, {@code MIL} 25.4·10⁻⁶, {@code M} 10⁻³, {@code U} (and {@code µ}) 10⁻⁶, {@code N} 10⁻⁹,
 *     {@code P} 10⁻¹², {@code F} 10⁻¹⁵.</li>
 *     <li>Letters after the factor are a unit and change nothing: {@code 10uF}, {@code 1kOhm}, {@code 5V}, {@code 50Hz}.</li>
 *     <li>⚠️ So {@code 1M} is a thousandth and {@code 1F} a femto-unit, exactly as SPICE reads them — the reader keeps
 *     SPICE's meaning and the {@link SpiceAnalyzer} names a lone {@code M} in a note, since it is the usual slip for
 *     mega.</li>
 *     <li>⚠️ {@code 1k5} — a digit after the factor, the European way of writing 1.5 k — is not SPICE, and is refused
 *     rather than read as {@code 1k} with the {@code 5} dropped, which is what SPICE itself would do.</li>
 * </ul>
 */
public final class SpiceNumbers {

    private static final Pattern NUMBER = Pattern.compile(
            "([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)([A-Za-zµμΩ_%]*)");

    private SpiceNumbers() {
    }

    /**
     * A number as written.
     *
     * @param value  the number in SI
     * @param suffix the letters after the digits, as written: {@code meg}, {@code uF}, empty for none
     */
    public record Engineering(double value, String suffix) {

        /** Whether the suffix is a lone {@code M}: a thousandth to SPICE, and mega to whoever wrote it, usually. */
        public boolean isLoneCapitalM() {
            return suffix.equals("M");
        }
    }

    /** The number a word writes, or empty when the word is not a number. */
    public static Optional<Engineering> read(String word) {
        Matcher matcher = NUMBER.matcher(word.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String suffix = matcher.group(2);
        // Scaled in decimal, as written: 4.7n is the double nearest 4.7·10⁻⁹, not 4.7 times the double nearest 10⁻⁹.
        BigDecimal scaled = new BigDecimal(matcher.group(1)).multiply(BigDecimal.valueOf(scale(suffix)));
        return Optional.of(new Engineering(scaled.doubleValue(), suffix));
    }

    /** What the letters after a number multiply it by. */
    static double scale(String suffix) {
        String lower = suffix.toLowerCase(Locale.ROOT);
        if (lower.startsWith("meg")) {
            return SiPrefix.MEGA.factor();
        }
        if (lower.startsWith("mil")) {
            return 25.4e-6;
        }
        if (lower.isEmpty()) {
            return 1;
        }
        // SPICE's letters, which ignore case — 'm' and 'M' are both a thousandth —, onto the SI factors.
        SiPrefix prefix = switch (lower.charAt(0)) {
            case 't' -> SiPrefix.TERA;
            case 'g' -> SiPrefix.GIGA;
            case 'k' -> SiPrefix.KILO;
            case 'm' -> SiPrefix.MILLI;
            case 'u', 'µ', 'μ' -> SiPrefix.MICRO;
            case 'n' -> SiPrefix.NANO;
            case 'p' -> SiPrefix.PICO;
            case 'f' -> SiPrefix.FEMTO;
            default -> null;
        };
        return prefix == null ? 1 : prefix.factor();
    }

    /** Significant digits a number is written with: a datasheet's figure survives, the last bit of a product does not. */
    private static final MathContext DIGITS = new MathContext(15);

    /**
     * A number as SPICE writes it back, to fifteen significant digits — so {@code 14.34e-15} is {@code 1.434e-14}, not
     * the {@code 1.4340000000000002e-14} its {@code double} spells — with an exponent when it is very large or very
     * small: {@code 1000}, {@code 2.52e-9}.
     */
    public static String write(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("A SPICE value is finite, not " + value + ".");
        }
        if (value == 0) {
            return "0";
        }
        BigDecimal rounded = new BigDecimal(value).round(DIGITS).stripTrailingZeros();
        double magnitude = Math.abs(value);
        if (magnitude >= 1e-3 && magnitude < 1e9) {
            return rounded.toPlainString();
        }
        int exponent = rounded.precision() - rounded.scale() - 1;
        String mantissa = rounded.movePointLeft(exponent).stripTrailingZeros().toPlainString();
        return mantissa + "e" + exponent;
    }
}
