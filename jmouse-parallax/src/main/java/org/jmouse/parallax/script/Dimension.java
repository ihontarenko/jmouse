package org.jmouse.parallax.script;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What a physical quantity is measured in, as powers of the SI base units it is built from — kilogram, metre, second,
 * ampere, kelvin. A voltage is kg·m²·s⁻³·A⁻¹, so a voltage divided by a current is a resistance whatever the two were
 * called: two quantities agree when their powers do.
 *
 * @param mass        the power of the kilogram
 * @param length      the power of the metre
 * @param time        the power of the second
 * @param current     the power of the ampere
 * @param temperature the power of the kelvin
 */
public record Dimension(int mass, int length, int time, int current, int temperature) {

    public static final Dimension NONE = new Dimension(0, 0, 0, 0, 0);
    public static final Dimension MASS = new Dimension(1, 0, 0, 0, 0);
    public static final Dimension LENGTH = new Dimension(0, 1, 0, 0, 0);
    public static final Dimension TIME = new Dimension(0, 0, 1, 0, 0);
    public static final Dimension CURRENT = new Dimension(0, 0, 0, 1, 0);
    public static final Dimension TEMPERATURE = new Dimension(0, 0, 0, 0, 1);
    public static final Dimension VOLTAGE = new Dimension(1, 2, -3, -1, 0);
    public static final Dimension CHARGE = CURRENT.times(TIME);
    public static final Dimension POWER = VOLTAGE.times(CURRENT);
    public static final Dimension ENERGY = POWER.times(TIME);
    public static final Dimension RESISTANCE = VOLTAGE.over(CURRENT);
    public static final Dimension CONDUCTANCE = CURRENT.over(VOLTAGE);
    public static final Dimension CAPACITANCE = CHARGE.over(VOLTAGE);
    public static final Dimension FLUX = VOLTAGE.times(TIME);
    public static final Dimension INDUCTANCE = FLUX.over(CURRENT);
    public static final Dimension FREQUENCY = NONE.over(TIME);
    public static final Dimension FORCE = MASS.times(LENGTH).over(TIME.power(2));

    /** Unit symbols as a datasheet writes them, without a prefix, and the dimension each stands for. */
    private static final Map<String, Dimension> UNITS = units();

    private static final String PREFIXES = "fpnuµμmkMGT";

    public Dimension times(Dimension other) {
        return new Dimension(mass + other.mass, length + other.length, time + other.time, current + other.current,
                temperature + other.temperature);
    }

    public Dimension over(Dimension other) {
        return times(other.power(-1));
    }

    public Dimension power(int exponent) {
        return new Dimension(mass * exponent, length * exponent, time * exponent, current * exponent,
                temperature * exponent);
    }

    /** The square root, when every power is even; empty otherwise — the root of a voltage is no quantity. */
    public Optional<Dimension> root() {
        if (mass % 2 != 0 || length % 2 != 0 || time % 2 != 0 || current % 2 != 0 || temperature % 2 != 0) {
            return Optional.empty();
        }
        return Optional.of(new Dimension(mass / 2, length / 2, time / 2, current / 2, temperature / 2));
    }

    public boolean isNone() {
        return equals(NONE);
    }

    /**
     * What a written unit measures — {@code V}, {@code mA}, {@code kOhm}, {@code K/W}, {@code A/V^2}, {@code 1/s},
     * {@code %} —; empty for one this cannot name ({@code div}, {@code bit}), which a check then leaves alone.
     */
    public static Optional<Dimension> ofUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return Optional.empty();
        }
        String written = unit.replace(" ", "").replace("·", "*");
        Dimension result = NONE;
        int position = 0;
        boolean dividing = false;
        while (position <= written.length()) {
            int next = nextOperator(written, position);
            String factor = written.substring(position, next);
            if (!(factor.equals("1") && position == 0)) {
                Optional<Dimension> measured = ofFactor(factor);
                if (measured.isEmpty()) {
                    return Optional.empty();
                }
                result = dividing ? result.over(measured.get()) : result.times(measured.get());
            }
            if (next >= written.length()) {
                break;
            }
            dividing = written.charAt(next) == '/';
            position = next + 1;
        }
        return Optional.of(result);
    }

    private static int nextOperator(String written, int from) {
        for (int index = from; index < written.length(); index++) {
            if (written.charAt(index) == '/' || written.charAt(index) == '*') {
                return index;
            }
        }
        return written.length();
    }

    /** One unit with its prefix and its power: {@code mV}, {@code s^2}, {@code m²}. */
    private static Optional<Dimension> ofFactor(String factor) {
        int exponent = 1;
        String symbol = factor;
        if (symbol.endsWith("²")) {
            exponent = 2;
            symbol = symbol.substring(0, symbol.length() - 1);
        } else if (symbol.endsWith("³")) {
            exponent = 3;
            symbol = symbol.substring(0, symbol.length() - 1);
        } else if (symbol.contains("^")) {
            try {
                exponent = Integer.parseInt(symbol.substring(symbol.indexOf('^') + 1));
            } catch (NumberFormatException unreadable) {
                return Optional.empty();
            }
            symbol = symbol.substring(0, symbol.indexOf('^'));
        }
        Dimension measured = UNITS.get(symbol);
        if (measured == null && symbol.length() > 1 && PREFIXES.indexOf(symbol.charAt(0)) >= 0) {
            measured = UNITS.get(symbol.substring(1));
        }
        int raised = exponent;
        return Optional.ofNullable(measured).map(dimension -> dimension.power(raised));
    }

    private static Map<String, Dimension> units() {
        Map<String, Dimension> units = new LinkedHashMap<>();
        units.put("V", VOLTAGE);
        units.put("A", CURRENT);
        units.put("Ohm", RESISTANCE);
        units.put("Ω", RESISTANCE);
        units.put("S", CONDUCTANCE);
        units.put("F", CAPACITANCE);
        units.put("H", INDUCTANCE);
        units.put("Wb", FLUX);
        units.put("C", CHARGE);
        units.put("W", POWER);
        units.put("J", ENERGY);
        units.put("Hz", FREQUENCY);
        units.put("s", TIME);
        units.put("K", TEMPERATURE);
        units.put("°C", TEMPERATURE);
        units.put("degC", TEMPERATURE);
        units.put("g", MASS);
        units.put("m", LENGTH);
        units.put("N", FORCE);
        units.put("Pa", FORCE.over(LENGTH.power(2)));
        for (String ratio : new String[] {"%", "ppm", "dB", "rad", "deg", "°"}) {
            units.put(ratio, NONE);
        }
        return Map.copyOf(units);
    }

    /** The quantities a reader knows by name, and the unit each is written in. */
    private static final Map<Dimension, String> NAMED = Map.ofEntries(
            Map.entry(VOLTAGE, "a voltage [V]"), Map.entry(CURRENT, "a current [A]"), Map.entry(CHARGE, "a charge [C]"),
            Map.entry(POWER, "a power [W]"), Map.entry(ENERGY, "an energy [J]"), Map.entry(RESISTANCE, "a resistance [Ω]"),
            Map.entry(CONDUCTANCE, "a conductance [S]"), Map.entry(CAPACITANCE, "a capacitance [F]"),
            Map.entry(INDUCTANCE, "an inductance [H]"), Map.entry(FLUX, "a flux [Wb]"),
            Map.entry(FREQUENCY, "a frequency [Hz]"), Map.entry(TIME, "a time [s]"),
            Map.entry(TEMPERATURE, "a temperature [K]"), Map.entry(LENGTH, "a length [m]"));

    /** As a reader would name it: {@code a voltage [V]}, else its base units, {@code kg·m²·s⁻³}. */
    public String describe() {
        if (isNone()) {
            return "a pure number";
        }
        String named = NAMED.get(this);
        if (named != null) {
            return named;
        }
        StringBuilder written = new StringBuilder();
        append(written, "kg", mass);
        append(written, "m", length);
        append(written, "s", time);
        append(written, "A", current);
        append(written, "K", temperature);
        return written.toString();
    }

    private static void append(StringBuilder written, String unit, int exponent) {
        if (exponent == 0) {
            return;
        }
        if (!written.isEmpty()) {
            written.append('·');
        }
        written.append(unit);
        if (exponent != 1) {
            written.append('^').append(exponent);
        }
    }
}
