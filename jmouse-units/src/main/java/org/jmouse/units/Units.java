package org.jmouse.units;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The one table of units, and the reading of a written unit against it.
 *
 * <ul>
 *     <li>{@link #asRead} — a unit as a datasheet prints it, as it is read: {@code µF} is {@code uF}, {@code kΩ} is
 *     {@code kOhm}, {@code °C} is {@code degC}, {@code m²} is {@code m^2};</li>
 *     <li>{@link #shown} — the other way, for a reader: {@code uF} is shown {@code µF}, {@code kOhm} {@code kΩ};</li>
 *     <li>{@link #read} — a whole written unit, prefixes, products, quotients and powers included.</li>
 * </ul>
 */
public final class Units {

    /** The symbols a datasheet writes a unit with, as the reader spells them. */
    private static final Map<Character, String> SYMBOLS = Map.ofEntries(
            Map.entry('Ω', "Ohm"), Map.entry('µ', "u"), Map.entry('μ', "u"), Map.entry('°', "deg"),
            Map.entry('²', "^2"), Map.entry('³', "^3"), Map.entry('·', "*"), Map.entry('×', "*"));

    private static final Map<String, Unit> TABLE = table();

    /** One factor of a compound unit: the operator in front of it, then the symbol and its power. */
    private static final Pattern FACTOR = Pattern.compile("([*/]?)([^*/]+)");

    private static final String PRODUCT = "*";
    private static final String QUOTIENT = "/";
    private static final String POWER = "^";
    private static final String ONE = "1";

    private Units() {
    }

    private static Map<String, Unit> table() {
        Map<String, Unit> table = new LinkedHashMap<>();
        for (Unit unit : new Unit[] {
                Unit.prefixable("V", Dimension.VOLTAGE), Unit.prefixable("A", Dimension.CURRENT),
                Unit.prefixable("Ohm", Dimension.RESISTANCE), Unit.prefixable("S", Dimension.CONDUCTANCE),
                Unit.prefixable("F", Dimension.CAPACITANCE), Unit.prefixable("H", Dimension.INDUCTANCE),
                Unit.prefixable("Wb", Dimension.FLUX), Unit.prefixable("C", Dimension.CHARGE),
                Unit.prefixable("W", Dimension.POWER), Unit.prefixable("J", Dimension.ENERGY),
                Unit.prefixable("Hz", Dimension.FREQUENCY), Unit.prefixable("s", Dimension.TIME),
                Unit.prefixable("K", Dimension.TEMPERATURE), Unit.prefixable("g", Dimension.MASS),
                Unit.prefixable("m", Dimension.LENGTH), Unit.prefixable("N", Dimension.FORCE),
                Unit.prefixable("Pa", Dimension.FORCE.over(Dimension.LENGTH.power(2))),
                Unit.standalone("%", Dimension.NONE, 0.01, "%"),
                Unit.standalone("ppm", Dimension.NONE, 1e-6, "ppm"),
                Unit.standalone("deg", Dimension.NONE, Math.PI / 180, "rad"),
                Unit.standalone("rad", Dimension.NONE, 1, "rad"),
                Unit.standalone("dB", Dimension.NONE, 1, "dB"),
                Unit.standalone("degC", Dimension.TEMPERATURE, 1, "degC"),
                Unit.standalone("div", null, 1, "div"),
                Unit.standalone("bit", null, 1, "bit")}) {
            table.put(unit.symbol(), unit);
        }
        return Map.copyOf(table);
    }

    /** Every unit the table reads, by its symbol. */
    public static Collection<Unit> all() {
        return TABLE.values();
    }

    /** The signs a datasheet writes that the reader spells otherwise — {@code Ω} as {@code Ohm}, {@code µ} as {@code u}. */
    public static Map<Character, String> writtenSigns() {
        return SYMBOLS;
    }

    /** The unit read with that symbol, without a prefix: {@code Ohm}, {@code degC}; {@code Ω} is read too. */
    public static Optional<Unit> unit(String symbol) {
        return Optional.ofNullable(TABLE.get(asRead(symbol)));
    }

    /** A unit as it is read: {@code µF} as {@code uF}, {@code kΩ} as {@code kOhm}, {@code °C} as {@code degC}. */
    public static String asRead(String written) {
        StringBuilder read = new StringBuilder(written.length());
        for (char symbol : written.strip().toCharArray()) {
            read.append(symbol(symbol).orElse(String.valueOf(symbol)));
        }
        return read.toString();
    }

    /**
     * What a symbol a datasheet writes a unit with is read as — Ω is {@code Ohm}, µ is {@code u}, ° is {@code deg} —;
     * empty for any other character. A language whose values carry units reads them from here.
     */
    public static Optional<String> symbol(char character) {
        return Optional.ofNullable(SYMBOLS.get(character));
    }

    /** A unit as a reader is shown it: {@code uF} as {@code µF}, {@code kOhm} as {@code kΩ}. */
    public static String shown(String unit) {
        String shown = unit.length() > 1 && unit.charAt(0) == SiPrefix.MICRO.symbol() ? "µ" + unit.substring(1) : unit;
        return shown.replace("Ohm", "Ω");
    }

    /**
     * A written unit read whole: a product and quotient of factors, each with an optional prefix and power —
     * {@code kOhm}, {@code V/us}, {@code A/V^2}, {@code 1/s}, {@code kΩ·m}. Empty for anything else, so that text which
     * is not a unit is never read as one.
     */
    public static Optional<CompoundUnit> read(String written) {
        if (written == null || written.isBlank()) {
            return Optional.empty();
        }
        String compact = asRead(written).replace(" ", "");
        Matcher matcher = FACTOR.matcher(compact);
        // In decimal, so a prefix scales exactly: a thousandth over a millionth is a thousand, not 1000.0000000000001.
        BigDecimal factor = BigDecimal.ONE;
        StringBuilder base = new StringBuilder();
        Optional<Dimension> dimension = Optional.of(Dimension.NONE);
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
            Optional<Raised> raised = raised(matcher.group(2), first);
            if (raised.isEmpty()) {
                return Optional.empty();
            }
            Raised term = raised.get();
            boolean dividing = operator.equals(QUOTIENT);
            factor = dividing ? factor.divide(term.factor(), MathContext.DECIMAL128) : factor.multiply(term.factor());
            base.append(operator).append(term.base());
            dimension = dimension.flatMap(sofar -> term.dimension().map(measured -> dividing ? sofar.over(measured) : sofar.times(measured)));
            first = false;
        }
        boolean complete = position == compact.length() && !first && !base.toString().equals(ONE);
        return complete ? Optional.of(new CompoundUnit(factor.doubleValue(), base.toString(), dimension)) : Optional.empty();
    }

    /** One factor with its power applied: {@code us} is a millionth of a second, {@code V^2} a volt squared. */
    private record Raised(BigDecimal factor, String base, Optional<Dimension> dimension) {
    }

    private static Optional<Raised> raised(String written, boolean first) {
        String[] symbolAndPower = written.split("\\" + POWER, 2);
        int power;
        try {
            power = symbolAndPower.length == 2 ? Integer.parseInt(symbolAndPower[1]) : 1;
        } catch (NumberFormatException notAPower) {
            return Optional.empty();
        }
        String symbol = symbolAndPower[0];
        String suffix = power == 1 ? "" : POWER + power;
        if (first && symbol.equals(ONE)) {
            return Optional.of(new Raised(BigDecimal.ONE, ONE + suffix, Optional.of(Dimension.NONE)));
        }
        return scaled(symbol).map(term -> new Raised(term.factor().pow(power, MathContext.DECIMAL128), term.base() + suffix,
                term.dimension().map(dimension -> dimension.power(power))));
    }

    /** One symbol, its prefix read: {@code kOhm} is a thousand ohms. */
    private static Optional<Raised> scaled(String symbol) {
        Unit whole = TABLE.get(symbol);
        if (whole != null) {
            return Optional.of(new Raised(BigDecimal.valueOf(whole.scale()), whole.base(), whole.dimension()));
        }
        if (symbol.length() < 2) {
            return Optional.empty();
        }
        Unit unit = TABLE.get(symbol.substring(1));
        if (unit == null || !unit.prefixable()) {
            return Optional.empty();
        }
        return SiPrefix.of(symbol.charAt(0)).map(prefix -> new Raised(
                BigDecimal.valueOf(prefix.factor()).multiply(BigDecimal.valueOf(unit.scale())), unit.base(),
                unit.dimension()));
    }
}
