package org.jmouse.parallax.circuit;

import java.util.Map;

/**
 * The argument arrays a model unit's compiled equations read — each unknown's value at its position, the charges' rates,
 * the time, the discrete states, then the parameters —, and how a state is written into them. Two of them: the one
 * every linearization reads — and a measurement at a solution —, and one a dissipated current is read with, its rates
 * at nought. One unit's alone: its equations are evaluated by one thread at a time.
 */
final class UnitArguments {

    private final ModelEquations      equations;
    private final UnitLayout          layout;
    private final Map<String, Object> discrete;
    final double[] values;
    final double[] dissipative;

    UnitArguments(ModelEquations equations, UnitLayout layout, Map<String, Object> discrete) {
        this.equations = equations;
        this.layout = layout;
        this.discrete = discrete;
        this.values = equations.arguments();
        this.dissipative = equations.arguments();
    }

    /** A new array of its own, the parameters in place: for a reading that must leave the unit's own as they are. */
    double[] fresh() {
        return equations.arguments();
    }

    /** Every unknown's value read from a state vector (none: left as it is), the discrete states and the time. */
    void fill(double[] into, double[] state, double time) {
        if (state != null) {
            int[] columns = layout.columns;
            for (int position = 0; position < columns.length; position++) {
                if (columns[position] >= 0) {
                    into[position] = state[columns[position]];
                }
            }
        }
        fillDiscrete(into, time);
    }

    /** The time and every discrete state as the equations read them. */
    void fillDiscrete(double[] into, double time) {
        if (layout.timePosition != UnitLayout.NONE) {
            into[layout.timePosition] = time;
        }
        for (int index = 0; index < layout.discretePositions.length; index++) {
            into[layout.discretePositions[index]] = discreteValue(index);
        }
    }

    /** No charge is changing: at the start, and around the operating point a phasor is taken at. */
    void clearRates(double[] into) {
        for (int position : layout.rateSymbols) {
            into[position] = 0;
        }
    }

    /** The value the equations read at one discrete position: a single state, or one slot of a row. */
    private double discreteValue(int index) {
        Object value = discrete.get(layout.discreteNames[index]);
        int slot = layout.discreteSlots[index];
        return numeric(slot == UnitLayout.NONE ? value : ((Object[]) value)[slot]);
    }

    /** Whether every discrete state is still what the arguments last read. */
    boolean discreteUnchanged() {
        for (int index = 0; index < layout.discretePositions.length; index++) {
            if (discreteValue(index) != values[layout.discretePositions[index]]) {
                return false;
            }
        }
        return true;
    }

    /** The unit's own temperature, kelvin, into every array its equations are read with. */
    void temperature(double kelvin) {
        values[layout.temperaturePosition] = kelvin;
        dissipative[layout.temperaturePosition] = kelvin;
    }

    /** The arguments a dissipated current is evaluated with: the solved state, its time, and every charge's rate at nought. */
    void fillDissipative(double[] state, double time) {
        System.arraycopy(values, 0, dissipative, 0, values.length);
        fill(dissipative, state, time);
        clearRates(dissipative);
    }

    /** A discrete value as the equations read it: a number, or ½ for an unknown logic level. */
    static double numeric(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof Boolean flag) {
            return flag ? 1 : 0;
        }
        return 0.5;
    }
}
