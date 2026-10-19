package org.jmouse.parallax.circuit;

/**
 * A model unit's equations as they are stamped: each one's value at the last linearization point, its tangent over the
 * unit's locals ({@link UnitLayout}), and the tangent times that point — the line {@code value + tangent · (x − guess)}.
 * A {@link UnitLinearization} finds it; the unit stamps it, and reads its currents off it at a solution.
 */
final class UnitLine {

    private final UnitLayout layout;
    final double[]   values;
    final double[][] tangents;
    final double[]   linearParts;
    /** The point it was linearized at, by local. */
    final double[]   guess;
    /** Scratch: one equation's tangent over every local while it is accumulated. */
    final double[]   jacobian;

    UnitLine(UnitLayout layout) {
        this.layout = layout;
        int equations = layout.stamps.length;
        this.values = new double[equations];
        this.tangents = new double[equations][];
        for (int position = 0; position < equations; position++) {
            tangents[position] = new double[layout.stamps[position].locals().length];
        }
        this.linearParts = new double[equations];
        this.guess = new double[layout.unitColumns.length];
        this.jacobian = new double[layout.unitColumns.length];
    }

    /** Takes the assembly's guess as the point to linearize at. */
    void takeGuess(Assembly assembly) {
        int[] columns = layout.unitColumns;
        for (int local = 0; local < columns.length; local++) {
            guess[local] = assembly.unknown(columns[local]);
        }
    }

    /**
     * A current equation's value at a solved state: the line it was stamped as, read at the solution — the current
     * Kirchhoff's sums were solved with. The value at the guess alone is not: a diode that turned off within the step
     * kept its conducting current there, and that current times its reverse voltage read as a diode delivering tens of
     * milliwatts.
     */
    double solved(int position, double[] state) {
        int[] locals = layout.stamps[position].locals();
        int[] columns = layout.unitColumns;
        double[] tangent = tangents[position];
        double current = values[position];
        for (int index = 0; index < locals.length; index++) {
            current += tangent[index] * (state[columns[locals[index]]] - guess[locals[index]]);
        }
        return current;
    }
}
