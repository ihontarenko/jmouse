package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.simulation.engine.ModelEquations.Kind;

import org.jmouse.cad.simulation.engine.ModelEquations.Compiled;

/**
 * The line of a unit whose equations are not proved affine: every equation evaluated with its partials on its tape at
 * the assembly's guess, and the last linearization kept — as SPICE bypasses a device — where nothing it reads has moved.
 */
final class TapedLinearization implements UnitLinearization {

    /** How far its own temperature may move from where it was last linearized before it is linearized again, kelvin. */
    private static final double TEMPERATURE_BYPASS = 0.01;
    /**
     * How far a unit's unknown may move and still leave its last linearization in use: Newton's own settling tolerance,
     * as SPICE bypasses at its reltol — what is left between the two is second order in a move already that small.
     */
    private static final double BYPASS_RELATIVE = 1e-4;
    private static final double BYPASS_ABSOLUTE = 1e-6;

    private final ModelEquations equations;
    private final UnitLayout     layout;
    private final UnitArguments  arguments;
    private final UnitLine       line;
    private final UnitHistory    history;
    /** Its own heat where its equations read the temperature it makes — a realistic bench —; null where they do not. */
    private final PartHeat       ownHeat;
    /** The attempt the line was last made in, and its own temperature then, kelvin. */
    private Assembly             linearizedFor;
    private double               temperatureLinearized;

    TapedLinearization(ModelEquations equations, UnitLayout layout, UnitArguments arguments, UnitLine line,
                       UnitHistory history, PartHeat ownHeat) {
        this.equations = equations;
        this.layout = layout;
        this.arguments = arguments;
        this.line = line;
        this.history = history;
        this.ownHeat = ownHeat;
    }

    @Override
    public void linearize(Assembly assembly, double coefficient, boolean trapezoidal, boolean transientAnalysis) {
        if (bypasses(assembly)) {
            return;
        }
        linearizedFor = assembly;
        if (ownHeat != null) {
            arguments.temperature(ownHeat.temperature());
        }
        temperatureLinearized = ownHeat != null ? ownHeat.temperature() : Double.NaN;
        line.takeGuess(assembly);
        double[] values = arguments.values;
        int[] columns = layout.columns;
        for (int position = 0; position < columns.length; position++) {
            if (columns[position] >= 0) {
                values[position] = assembly.unknown(columns[position]);
            }
        }
        arguments.fillDiscrete(values, assembly.time());
        if (layout.linearizesWrittenOut) {
            fillHistories(coefficient, trapezoidal, transientAnalysis);
        } else {
            evaluateRates(coefficient, trapezoidal, transientAnalysis);
        }
        for (int position = 0; position < layout.stamps.length; position++) {
            UnitLayout.EquationStamp stamp = layout.stamps[position];
            if (layout.linearizesWrittenOut) {
                line.values[position] = stamp.linearized().evaluateWithPartials(values);
                accumulateWrittenOutTangent(stamp);
            } else {
                line.values[position] = stamp.formula().evaluateWithPartials(values);
                accumulateTangent(stamp, coefficient);
            }
            int[] locals = stamp.locals();
            double[] tangent = line.tangents[position];
            double linear = 0;
            for (int index = 0; index < locals.length; index++) {
                tangent[index] = line.jacobian[locals[index]];
                linear += tangent[index] * line.guess[locals[index]];
            }
            line.linearParts[position] = linear;
        }
    }

    /**
     * Whether the last linearization still holds: none of the unit's unknowns has moved since by more than the bypass
     * tolerance, and nothing else its equations read has changed. Within one assembly — one attempt at a step — that is
     * all of it. Across steps it holds only for a unit without charges and without time, whose equations read nothing
     * but its unknowns and its discrete states: a comparator between edges, an amplifier at rest. The integration's
     * coefficient enters a state row when it is written, not when it is linearized, so a new step size does not undo the
     * bypass. A unit never linearized has nothing to keep.
     */
    private boolean bypasses(Assembly assembly) {
        if (linearizedFor == null) {
            return false;
        }
        if (assembly != linearizedFor && !(layout.stepIndependent && arguments.discreteUnchanged())) {
            return false;
        }
        if (ownHeat != null && Math.abs(ownHeat.temperature() - temperatureLinearized) > TEMPERATURE_BYPASS) {
            return false;
        }
        int[] columns = layout.unitColumns;
        for (int local = 0; local < columns.length; local++) {
            double guess = assembly.unknown(columns[local]);
            if (Math.abs(guess - line.guess[local]) > BYPASS_RELATIVE * Math.abs(guess) + BYPASS_ABSOLUTE) {
                return false;
            }
        }
        return true;
    }

    /** Evaluates every charge with its partials and writes its rate into the arguments. */
    private void evaluateRates(double coefficient, boolean trapezoidal, boolean transientAnalysis) {
        double[] values = arguments.values;
        for (int charge = 0; charge < layout.charges(); charge++) {
            Compiled formula = equations.charges().get(charge);
            double value = formula.evaluateWithPartials(values);
            values[layout.rateSymbols[charge]] = transientAnalysis
                    ? coefficient * (value - history.chargePrevious[charge]) - (trapezoidal ? history.ratePrevious[charge] : 0)
                    : 0;
        }
    }

    /**
     * What a written-out equation reads besides the unknowns: the coefficient, and each charge's history — so that its
     * rate, coefficient · q − history, is coefficient · (q − q_previous), less the previous rate under the trapezoidal
     * rule; at rest every rate is nought.
     */
    private void fillHistories(double coefficient, boolean trapezoidal, boolean transientAnalysis) {
        double[] values = arguments.values;
        values[layout.coefficientPosition] = transientAnalysis ? coefficient : 0;
        for (int charge = 0; charge < layout.historyPositions.length; charge++) {
            values[layout.historyPositions[charge]] = transientAnalysis ? history.carried(charge, coefficient, trapezoidal) : 0;
        }
    }

    /** A written-out equation's tangent: its partials alone, the charges' already inside them. */
    private void accumulateWrittenOutTangent(UnitLayout.EquationStamp stamp) {
        double[] jacobian = line.jacobian;
        for (int local : stamp.locals()) {
            jacobian[local] = 0;
        }
        double[] derivatives = stamp.linearized().derivatives();
        int[] linearizedLocal = stamp.linearizedLocal();
        for (int partial = 0; partial < derivatives.length; partial++) {
            if (linearizedLocal[partial] != UnitLayout.NONE) {
                jacobian[linearizedLocal[partial]] += derivatives[partial];
            }
        }
    }

    /** The equation's tangent over the unit's unknowns: its direct partials, and through each charge rate the charge's. */
    private void accumulateTangent(UnitLayout.EquationStamp stamp, double coefficient) {
        double[] jacobian = line.jacobian;
        for (int local : stamp.locals()) {
            jacobian[local] = 0;
        }
        double[] derivatives = stamp.formula().derivatives();
        int[] partialLocal = stamp.partialLocal();
        int[] partialCharge = stamp.partialCharge();
        for (int partial = 0; partial < derivatives.length; partial++) {
            int charge = partialCharge[partial];
            if (charge != UnitLayout.NONE) {
                double scale = derivatives[partial] * coefficient;
                int[] locals = layout.chargeLocals[charge];
                double[] partials = equations.charges().get(charge).derivatives();
                for (int inner = 0; inner < locals.length; inner++) {
                    if (locals[inner] != UnitLayout.NONE) {
                        jacobian[locals[inner]] += scale * partials[inner];
                    }
                }
            } else if (partialLocal[partial] != UnitLayout.NONE) {
                jacobian[partialLocal[partial]] += derivatives[partial];
            }
        }
    }

    /** Each charge evaluated at the state: the arguments are the linearization's, and the next one fills them again. */
    @Override
    public void charges(double[] state, double time, double[] into) {
        double[] values = arguments.values;
        arguments.fill(values, state, time);
        for (int charge = 0; charge < into.length; charge++) {
            into[charge] = equations.charges().get(charge).evaluate(values);
        }
    }

    /**
     * A current that reads no charge is the line it was stamped as, read at the solution; one that reads a charge is
     * evaluated again with every rate at nought — once the arguments are filled for the first of them.
     */
    @Override
    public void dissipatedCurrents(double[] state, double time, double[] into) {
        boolean filled = false;
        for (int position = 0; position < layout.stamps.length; position++) {
            if (layout.stamps[position].kind() != Kind.CURRENT) {
                continue;
            }
            if (!layout.readsCharge[position]) {
                into[position] = line.solved(position, state);
                continue;
            }
            if (!filled) {
                arguments.fillDissipative(state, time);
                filled = true;
            }
            into[position] = layout.stamps[position].formula().evaluate(arguments.dissipative);
        }
    }
}
