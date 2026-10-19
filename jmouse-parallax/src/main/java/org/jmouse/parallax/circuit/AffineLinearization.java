package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;

import org.jmouse.parallax.circuit.ModelEquations.Compiled;

/**
 * The line of a unit whose equations are affine in its unknowns and read no time and no discrete state — a resistance,
 * a capacitance, a meter, a winding: each equation is f₀ + D·x + Σ R·rate, each charge q₀ + Q·x, with every coefficient
 * a constant of its parameters and its temperature. They are found once, again only when its own temperature moves, and
 * a step is stamped from them as SPICE stamps a capacitor: no tape is run on any attempt. Proved from the formulas
 * ({@link ModelEquations#linear}), never assumed.
 */
final class AffineLinearization implements UnitLinearization {

    /**
     * How far its own temperature may move from where its coefficients were found before they are found again, kelvin:
     * a hundredth of a kelvin moves a junction's current by a few hundredths of a percent.
     */
    private static final double TEMPERATURE_BYPASS = 0.01;

    private final ModelEquations equations;
    private final UnitLayout     layout;
    private final UnitArguments  arguments;
    private final UnitLine       line;
    private final UnitHistory    history;
    /** Its own heat where its equations read the temperature it makes — a realistic bench —; null where they do not. */
    private final PartHeat       ownHeat;

    /** Each equation's value with every unknown and every rate at nought. */
    private double[]   value;
    /** Each equation's partial by each of the unit's unknowns, by local. */
    private double[][] direct;
    /** Each equation's partial by each charge's rate. */
    private double[][] rate;
    /** Each charge's value with every unknown at nought, and its partials by its locals. */
    private double[]   charge;
    private final double[][] chargePartials;
    /** The temperature the coefficients were found at, and the coefficient the tangents were last built for. */
    private double     temperature = Double.NaN;
    private double     tangentCoefficient = Double.NaN;

    AffineLinearization(ModelEquations equations, UnitLayout layout, UnitArguments arguments, UnitLine line,
                        UnitHistory history, PartHeat ownHeat) {
        this.equations = equations;
        this.layout = layout;
        this.arguments = arguments;
        this.line = line;
        this.history = history;
        this.ownHeat = ownHeat;
        this.chargePartials = new double[layout.charges()][];
        for (int index = 0; index < layout.charges(); index++) {
            chargePartials[index] = new double[layout.chargeLocals[index].length];
        }
    }

    /**
     * The coefficients at its present temperature: every equation and every charge evaluated once, with its partials, at
     * all unknowns and rates nought — where an affine formula's value is its constant and its partials are what they are
     * everywhere.
     */
    private void prepare() {
        double[] atZero = arguments.fresh();
        if (layout.temperaturePosition >= 0) {
            atZero[layout.temperaturePosition] = arguments.values[layout.temperaturePosition];
        }
        int charges = layout.charges();
        charge = new double[charges];
        for (int index = 0; index < charges; index++) {
            Compiled formula = equations.charges().get(index);
            charge[index] = formula.evaluateWithPartials(atZero);
            System.arraycopy(formula.derivatives(), 0, chargePartials[index], 0, chargePartials[index].length);
        }
        UnitLayout.EquationStamp[] stamps = layout.stamps;
        value = new double[stamps.length];
        direct = new double[stamps.length][layout.unitColumns.length];
        rate = new double[stamps.length][charges];
        for (int position = 0; position < stamps.length; position++) {
            UnitLayout.EquationStamp stamp = stamps[position];
            value[position] = stamp.formula().evaluateWithPartials(atZero);
            double[] derivatives = stamp.formula().derivatives();
            for (int partial = 0; partial < derivatives.length; partial++) {
                if (stamp.partialCharge()[partial] != UnitLayout.NONE) {
                    rate[position][stamp.partialCharge()[partial]] += derivatives[partial];
                } else if (stamp.partialLocal()[partial] != UnitLayout.NONE) {
                    direct[position][stamp.partialLocal()[partial]] += derivatives[partial];
                }
            }
        }
        temperature = layout.temperaturePosition >= 0 ? atZero[layout.temperaturePosition] : 0;
        tangentCoefficient = Double.NaN;
    }

    private void ensurePrepared() {
        if (value == null) {
            prepare();
        }
    }

    /**
     * Its line at the guess, from the constant coefficients: the tangent D + coefficient · R·Q — built again only when the
     * step's coefficient changes —, and the constant f₀ + R · (coefficient · q₀ − what each charge carries from the steps
     * before). The same line a tape would find, without running one.
     */
    @Override
    public void linearize(Assembly assembly, double coefficient, boolean trapezoidal, boolean transientAnalysis) {
        if (value == null) {
            prepare();
        } else if (ownHeat != null) {
            // Only its own heat moves what the coefficients were found at.
            arguments.temperature(ownHeat.temperature());
            if (Math.abs(arguments.values[layout.temperaturePosition] - temperature) > TEMPERATURE_BYPASS) {
                prepare();
            }
        }
        line.takeGuess(assembly);
        boolean freshTangent = coefficient != tangentCoefficient;
        double[] jacobian = line.jacobian;
        for (int position = 0; position < layout.stamps.length; position++) {
            int[] locals = layout.stamps[position].locals();
            double[] tangent = line.tangents[position];
            double[] rates = rate[position];
            if (freshTangent) {
                for (int local : locals) {
                    jacobian[local] = direct[position][local];
                }
                for (int index = 0; index < rates.length; index++) {
                    if (rates[index] != 0) {
                        for (int inner = 0; inner < layout.chargeLocals[index].length; inner++) {
                            int local = layout.chargeLocals[index][inner];
                            if (local != UnitLayout.NONE) {
                                jacobian[local] += coefficient * rates[index] * chargePartials[index][inner];
                            }
                        }
                    }
                }
                for (int index = 0; index < locals.length; index++) {
                    tangent[index] = jacobian[locals[index]];
                }
            }
            double constant = value[position];
            if (transientAnalysis) {
                for (int index = 0; index < rates.length; index++) {
                    double carried = coefficient * history.chargePrevious[index] + (trapezoidal ? history.ratePrevious[index] : 0);
                    constant += rates[index] * (coefficient * charge[index] - carried);
                }
            }
            double linear = 0;
            for (int index = 0; index < locals.length; index++) {
                linear += tangent[index] * line.guess[locals[index]];
            }
            line.values[position] = linear + constant;
            line.linearParts[position] = linear;
        }
        tangentCoefficient = coefficient;
    }

    /** A charge at a state: q₀ + Q · x. */
    private double chargeAt(int index, double[] state) {
        double stored = charge[index];
        int[] locals = layout.chargeLocals[index];
        for (int inner = 0; inner < locals.length; inner++) {
            if (locals[inner] != UnitLayout.NONE) {
                stored += chargePartials[index][inner] * state[layout.unitColumns[locals[inner]]];
            }
        }
        return stored;
    }

    @Override
    public double chargeTruncation(double[] solution, double[] predicted) {
        ensurePrepared();
        double largest = 0;
        for (int index = 0; index < layout.charges(); index++) {
            double stored = chargeAt(index, solution);
            largest = Math.max(largest, Truncation.share(stored - chargeAt(index, predicted), stored, Truncation.CHARGE));
        }
        return largest;
    }

    /** f₀ + D · x: a capacitance's charging current, which only stores, is left out exactly. */
    @Override
    public void dissipatedCurrents(double[] state, double time, double[] into) {
        ensurePrepared();
        int[] columns = layout.unitColumns;
        for (int position = 0; position < layout.stamps.length; position++) {
            if (layout.stamps[position].kind() != ComponentModel.EquationKind.CURRENT) {
                continue;
            }
            double current = value[position];
            double[] partials = direct[position];
            for (int local = 0; local < columns.length; local++) {
                if (partials[local] != 0) {
                    current += partials[local] * state[columns[local]];
                }
            }
            into[position] = current;
        }
    }

    /** Stateless, each charge is q₀ + Q·x: nothing else of the arguments is read. */
    @Override
    public boolean acceptFromCoefficients(double[] state, double coefficient, boolean trapezoidal) {
        if (layout.hasStates || value == null) {
            return false;
        }
        for (int index = 0; index < layout.charges(); index++) {
            history.accept(index, chargeAt(index, state), coefficient, trapezoidal);
        }
        return true;
    }
}
