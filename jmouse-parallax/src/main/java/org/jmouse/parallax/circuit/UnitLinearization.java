package org.jmouse.parallax.circuit;

/**
 * How a model unit finds the line its equations are stamped as ({@link UnitLine}) — and, from the same knowledge, what
 * it stores and what it dissipates. Two ways, chosen once from what the formulas are proved to be:
 *
 * <ul>
 *   <li>{@link AffineLinearization} — equations affine in the unit's unknowns, reading no time and no discrete state: a
 *       resistance, a capacitance, a meter, a winding. Their coefficients are constants, found once.</li>
 *   <li>{@link TapedLinearization} — anything else: every equation evaluated with its partials on its tape at each guess,
 *       the last linearization kept where nothing it reads has moved.</li>
 * </ul>
 */
sealed interface UnitLinearization permits AffineLinearization, TapedLinearization {

    /** The line at the assembly's guess, for this iteration and the ones after it may stand for. */
    void linearize(Assembly assembly, double coefficient, boolean trapezoidal, boolean transientAnalysis);

    /** The largest share of its allowed error any charge used over a step: the stored charge against the predicted one. */
    double chargeTruncation(double[] solution, double[] predicted);

    /**
     * Each current equation's current at a solved state with every charge's rate at nought — what it dissipates: a
     * charging current stores energy and gives it back. Written into {@code into} at the equation's place; the other
     * places are left as they are.
     */
    void dissipatedCurrents(double[] state, double time, double[] into);

    /**
     * Takes an accepted step into the unit's history from the coefficients alone, when they are all it needs.
     *
     * @return false when the step is to be taken from the arguments, as a unit with states takes it
     */
    boolean acceptFromCoefficients(double[] state, double coefficient, boolean trapezoidal);
}
