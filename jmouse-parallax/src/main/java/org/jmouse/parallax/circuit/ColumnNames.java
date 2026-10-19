package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.ObjectDefinition;

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
            case ObjectDefinition.PRIMITIVE_POSITIVE -> part;
            case ObjectDefinition.PRIMITIVE_NEGATIVE -> null;
            default -> part + "." + terminal;
        };
    }

    /** Whether a part reports in a primitive's terminals, "+" first — the terminal its own column carries. */
    public static boolean isPrimitiveTerminal(String terminal) {
        return ObjectDefinition.PRIMITIVE_POSITIVE.equals(terminal);
    }
}
