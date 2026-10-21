package org.jmouse.units;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A value as a datasheet writes it — {@code 4.7 nF}, {@code 0.3 V/us}, {@code 100 kOhm}, {@code 52.4%} — kept with its
 * unit, so an interface can print {@code 10 kΩ}, scale a slider logarithmically and refuse to add volts to seconds.
 * sheet writes a datasheet's values with it, and jMX a circuit's; the units are {@link Units}'.
 *
 * @param value   the value in SI: {@code 4.7 nF} is {@code 4.7e-9}
 * @param unit    the base unit without its prefix ({@code F}, {@code V/s}, {@code %}); empty for a plain number
 * @param written the value and unit as written ({@code 4.7 nF}, {@code -40 °C})
 */
public record Quantity(double value, String unit, String written) {

    /**
     * A number and, after it, whatever is written: a unit or nothing. The tail must be a unit, or the text is not a
     * quantity but an expression — {@code 1 / (1 / f)} is not «1» with the unit «/ (1 / f)», and {@code 2pi} is not 2.
     */
    private static final Pattern WRITTEN = Pattern.compile("^([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)\\s*(.*)$");

    private static final String PERCENT = "%";

    public Quantity {
        unit = unit == null ? "" : unit;
        written = written == null ? format(value) : written;
    }

    public static Quantity plain(double value) {
        return new Quantity(value, "", format(value));
    }

    /**
     * Reads a number and the unit written after it. The unit may be prefixed ({@code nF}), a ratio ({@code V/us}) or
     * standalone ({@code %}, {@code deg}); anything else is not a unit and yields nothing.
     */
    public static Optional<Quantity> of(double number, String unitText) {
        if (unitText == null || unitText.isBlank()) {
            return Optional.of(plain(number));
        }
        // ⚠️ Written with the unit as the author wrote it — °C, not degC; µs, not us —: the symbols are read for the
        // scale, and a document written back says what it said.
        String authored = unitText.strip();
        // Scaled in decimal: 450 nm is the double nearest 4.5e-7, not 450 times the double nearest 1e-9.
        return Units.read(authored).map(read -> new Quantity(scaled(number, read.factor()), read.base(),
                format(number) + (authored.equals(PERCENT) ? "" : " ") + authored));
    }

    /**
     * A value as a datasheet or a part line writes it — {@code 2.52 nA}, {@code 0.3 V/µs}, {@code 52.4%}, {@code 25} —;
     * empty for text that is not a number with, at most, its unit: an expression, evaluated as one. The one reader of a
     * written value, so that {@code 90 deg} is π/2 and {@code 100 ppm/K} is 10⁻⁴ per kelvin wherever it is written.
     */
    public static Optional<Quantity> parse(String text) {
        Matcher matcher = WRITTEN.matcher(text == null ? "" : text.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return of(Double.parseDouble(matcher.group(1)), matcher.group(2));
    }

    /**
     * The value in SI of text that must be a number with, at most, its unit.
     *
     * @throws IllegalArgumentException for anything else
     */
    public static double valueOf(String text) {
        return parse(text).map(Quantity::value).orElseThrow(() -> new IllegalArgumentException(
                "'" + text + "' is not a value: a value is a number and, at most, its unit"));
    }

    /** Whether the text is a number with, at most, a unit after it. */
    public static boolean isQuantity(String text) {
        return parse(text).isPresent();
    }

    /** Whether a word is a unit: {@code kOhm}, {@code V/us}, {@code %}. */
    public static boolean isUnit(String text) {
        return Units.read(text).isPresent();
    }

    /** The number as written, before its unit's scale: {@code 60} for {@code 60 deg}, {@code 4.7} for {@code 4.7 nF}. */
    public double writtenNumber() {
        Matcher matcher = WRITTEN.matcher(written.strip());
        return matcher.matches() ? Double.parseDouble(matcher.group(1)) : value;
    }

    /** The unit as written, prefix included: {@code nF} for {@code 4.7 nF}; empty for a plain number. */
    public String writtenUnit() {
        Matcher matcher = WRITTEN.matcher(written.strip());
        return matcher.matches() ? matcher.group(2).strip() : "";
    }

    /** What the value measures; empty for a plain number's unit that names nothing, such as {@code div}. */
    public Optional<Dimension> dimension() {
        return unit.isEmpty() ? Optional.of(Dimension.NONE) : Units.read(unit).flatMap(CompoundUnit::dimension);
    }

    private static double scaled(double number, double factor) {
        return BigDecimal.valueOf(number).multiply(BigDecimal.valueOf(factor)).doubleValue();
    }

    private static String format(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15
                ? String.valueOf((long) value) : String.format(Locale.ROOT, "%s", value);
    }
}
