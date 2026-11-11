package org.jmouse.cad.simulation.engine;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * What a circuit's readings are called and in which unit, wherever they are reported — an operating point, a sweep, a
 * transient, a live trace: a node's voltage by the node, a part's current by the part ({@code R1}, the current into its
 * first terminal) or by its pin ({@code U1.output}), a part's power as {@code R1.power}. One place, so every analysis
 * names a reading the same way and a reader of one finds it in another.
 */
public final class ColumnNames {

    /** The unit of a voltage column. */
    public static final String VOLTS   = "V";
    /** The unit of a current column. */
    public static final String AMPERES = "A";
    /** The unit of a power column. */
    public static final String WATTS   = "W";
    /** The unit of a logic channel: a level, 1 high, 0 low, -1 unknown, -2 let go. */
    public static final String LEVEL   = "level";
    /** The unit of a gain column. */
    public static final String DECIBELS = "dB";
    /** The unit of a phase column. */
    public static final String DEGREES = "°";
    /** The unit of a noise density column. */
    public static final String NOISE_DENSITY = "V/√Hz";

    /** A node's gain over frequency is its name and this: {@code out gain}. */
    public static final String GAIN_SUFFIX  = " gain";
    /** A node's phase over frequency: {@code out phase}. */
    public static final String PHASE_SUFFIX = " phase";
    /** A node's noise density over frequency: {@code out noise}. */
    public static final String NOISE_SUFFIX = " noise";

    private static final String POWER_SUFFIX = ".power";

    private ColumnNames() {
    }

    /** The column a part's power is reported in, in watts: {@code R1.power}. */
    public static String partPower(String part) {
        return part + POWER_SUFFIX;
    }

    /**
     * The column a terminal's current is reported in, in amperes: the part itself ({@code R1}) for a primitive's first
     * terminal — its second carries the same back and has none (null) —, {@code U1.output} for a named pin.
     */
    public static String partCurrent(String part, String terminal) {
        return switch (terminal) {
            case Netlist.Element.PRIMITIVE_POSITIVE -> part;
            case Netlist.Element.PRIMITIVE_NEGATIVE -> null;
            default -> part + "." + terminal;
        };
    }

    /**
     * Every column a part's currents are reported in, by its terminals' currents: each terminal's own — {@code U1.output}
     * —, and for a part of two terminals its own name as well, the current into its first one — a generator is
     * {@code G1} as well as {@code G1.positive}, a resistor {@code R1} as well as {@code R1.positive} —, as a bare element
     * is read.
     */
    public static Map<String, Double> partCurrents(String part, Map<String, Double> terminals) {
        Map<String, Double> columns = new LinkedHashMap<>();
        terminals.forEach((terminal, current) -> {
            String column = partCurrent(part, terminal);
            if (column != null) {
                columns.putIfAbsent(column, current);
            }
        });
        if (terminals.size() == 2 && !terminals.containsKey(Netlist.Element.PRIMITIVE_POSITIVE)) {
            columns.putIfAbsent(part, terminals.values().iterator().next());
        }
        return columns;
    }

    /** Whether a part reports in a primitive's terminals, "+" first — the terminal its own column carries. */
    public static boolean isPrimitiveTerminal(String terminal) {
        return Netlist.Element.PRIMITIVE_POSITIVE.equals(terminal);
    }
}
