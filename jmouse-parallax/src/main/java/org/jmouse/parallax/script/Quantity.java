package org.jmouse.parallax.script;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A value as a datasheet writes it — {@code 4.7 nF}, {@code 0.3 V/us}, {@code 100 kOhm}, {@code 52.4%} — kept with its
 * unit. jMC read the same text and threw the unit away; jMX keeps it, so an interface can print {@code 10 kΩ}, scale a
 * slider logarithmically and refuse to add volts to seconds.
 *
 * @param value the value in SI: {@code 4.7 nF} is {@code 4.7e-9}
 * @param unit  the base unit without its prefix ({@code F}, {@code V/s}, {@code %}); empty for a plain number
 * @param written the value and unit exactly as written ({@code 4.7 nF})
 */
public record Quantity(double value, String unit, String written) {

    private static final Map<Character, Double> PREFIXES = Map.ofEntries(
            Map.entry('f', 1e-15), Map.entry('p', 1e-12), Map.entry('n', 1e-9), Map.entry('u', 1e-6),
            Map.entry('m', 1e-3), Map.entry('k', 1e3), Map.entry('M', 1e6), Map.entry('G', 1e9), Map.entry('T', 1e12));

    /** Units a prefix may stand in front of. */
    private static final Set<String> PREFIXABLE = Set.of(
            "V", "A", "Ohm", "F", "H", "Hz", "s", "W", "J", "K", "S", "C", "g", "m", "Pa", "N");

    /** Units that stand on their own and take no prefix. */
    private static final Set<String> STANDALONE = Set.of("%", "deg", "rad", "dB", "ppm", "degC", "div", "bit");

    /**
     * A number and, after it, whatever is written: a unit or nothing. The tail must be a unit, or the text is not a
     * quantity but an expression — {@code 1 / (1 / f)} is not «1» with the unit «/ (1 / f)», and {@code 2pi} is not 2.
     */
    private static final Pattern WRITTEN = Pattern.compile("^([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)\\s*(.*)$");

    /** One factor of a compound unit: the operator in front of it, then the symbol and its power. */
    private static final Pattern UNIT_FACTOR = Pattern.compile("([*/]?)([^*/]+)");

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
        String text = unitText == null ? "" : asRead(unitText);
        if (text.isEmpty()) {
            return Optional.of(plain(number));
        }
        return scaleOf(text).map(scale -> new Quantity(number * scale.factor(), scale.baseUnit(),
                format(number) + (text.equals("%") ? "" : " ") + text));
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

    /** Whether a word is a unit this language reads: {@code kOhm}, {@code V/us}, {@code %}. */
    public static boolean isUnit(String text) {
        return text != null && !text.isBlank() && scaleOf(asRead(text)).isPresent();
    }

    /**
     * A unit as the reader reads it: {@code µF} as {@code uF}, {@code kΩ} as {@code kOhm}. Text cut out of a document
     * is the author's ({@link SourceText}), so a range bound written {@code 10 µF} arrives with its {@code µ}.
     */
    private static String asRead(String unitText) {
        return ExperienceNotation.normalize(unitText).source().strip();
    }

    private record Scale(double factor, String baseUnit) {
    }

    /**
     * A unit as a product and quotient of factors, each with an optional power: {@code V/us}, {@code A/V^2},
     * {@code 1/s}, {@code kOhm*m}. The scale multiplies every factor's prefix raised to its power; the base unit keeps
     * the factors without their prefixes.
     */
    private static Optional<Scale> scaleOf(String unit) {
        String compact = unit.replace(" ", "");
        Matcher matcher = UNIT_FACTOR.matcher(compact);
        double factor = 1;
        StringBuilder base = new StringBuilder();
        int position = 0;
        boolean first = true;
        while (matcher.find()) {
            if (matcher.start() != position) {
                return Optional.empty();
            }
            position = matcher.end();
            String operator = matcher.group(1);
            if (first == !operator.isEmpty()) {
                return Optional.empty();
            }
            String[] symbolAndPower = matcher.group(2).split("\\^", 2);
            int power;
            try {
                power = symbolAndPower.length == 2 ? Integer.parseInt(symbolAndPower[1]) : 1;
            } catch (NumberFormatException notAPower) {
                return Optional.empty();
            }
            String symbol = symbolAndPower[0];
            Optional<Scale> scale = first && symbol.equals("1") ? Optional.of(new Scale(1, "1")) : simpleScaleOf(symbol);
            if (scale.isEmpty()) {
                return Optional.empty();
            }
            double raised = Math.pow(scale.get().factor(), power);
            factor = operator.equals("/") ? factor / raised : factor * raised;
            base.append(operator).append(scale.get().baseUnit()).append(power == 1 ? "" : "^" + power);
            first = false;
        }
        boolean complete = position == compact.length() && !first && !base.toString().equals("1");
        return complete ? Optional.of(new Scale(factor, base.toString())) : Optional.empty();
    }

    private static Optional<Scale> simpleScaleOf(String unit) {
        if (unit.equals("%")) {
            return Optional.of(new Scale(0.01, "%"));
        }
        if (unit.equals("ppm")) {
            return Optional.of(new Scale(1e-6, "ppm"));
        }
        if (STANDALONE.contains(unit)) {
            return Optional.of(new Scale(unit.equals("deg") ? Math.PI / 180 : 1, unit.equals("deg") ? "rad" : unit));
        }
        if (PREFIXABLE.contains(unit)) {
            return Optional.of(new Scale(1, unit));
        }
        if (unit.length() > 1 && PREFIXES.containsKey(unit.charAt(0)) && PREFIXABLE.contains(unit.substring(1))) {
            return Optional.of(new Scale(PREFIXES.get(unit.charAt(0)), unit.substring(1)));
        }
        return Optional.empty();
    }

    private static String format(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15
                ? String.valueOf((long) value) : String.format(Locale.ROOT, "%s", value);
    }
}
