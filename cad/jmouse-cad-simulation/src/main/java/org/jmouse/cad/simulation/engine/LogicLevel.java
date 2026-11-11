package org.jmouse.cad.simulation.engine;

/**
 * The level a digital signal stands at. A behaviour's script reads and writes it as a value of its own language —
 * {@code 0}, {@code 1}, {@code X}, {@code Z} ({@link #scriptValue}, {@link #of}) —; the engine holds it as one of these, so
 * a level is never a number one place and a string the next.
 */
public enum LogicLevel {

    LOW(0, 0, 0),
    HIGH(1, 1, 1),
    /** Driven, but to no level anyone can say: two drivers that disagree, an input between its thresholds at the start. */
    UNKNOWN("X", 0.5, -1),
    /** Driven by nothing: an output let go, a node no driver holds. */
    RELEASED("Z", 0.5, -2);

    private final Object scriptValue;
    private final double fraction;
    private final int datasetValue;

    LogicLevel(Object scriptValue, double fraction, int datasetValue) {
        this.scriptValue = scriptValue;
        this.fraction = fraction;
        this.datasetValue = datasetValue;
    }

    /** A level written by a script: anything at or above ½ is high, below is low, and X and Z stay themselves. */
    static LogicLevel of(Object written) {
        if (written instanceof LogicLevel level) {
            return level;
        }
        if (written instanceof Number number) {
            return number.doubleValue() >= 0.5 ? HIGH : LOW;
        }
        if (written instanceof Boolean flag) {
            return flag ? HIGH : LOW;
        }
        return String.valueOf(written).equalsIgnoreCase("Z") ? RELEASED : UNKNOWN;
    }

    /** As a behaviour's script reads it: 0, 1, "X" or "Z". */
    Object scriptValue() {
        return scriptValue;
    }

    /** The level as a fraction of the supply an output pulls to: 0, 1, or ½ for one that says no level. */
    double fraction() {
        return fraction;
    }

    /** As a timing lane's column holds it: 1, 0, −1 for unknown, −2 released. */
    public int datasetValue() {
        return datasetValue;
    }
}
