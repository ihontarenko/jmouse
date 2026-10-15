package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;
import org.jmouse.parallax.numerics.SparseSystem;
import org.jmouse.parallax.circuit.ModelEquations.Compiled;
import org.jmouse.parallax.circuit.ModelEquations.Equation;
import org.jmouse.parallax.circuit.ModelEquations.Kind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * One unit of a jMC model as a device: its equations stamped as their tangent at the Newton guess.
 *
 * <p>A current equation {@code current a -> b = f} adds f leaving a and entering b; a voltage equation adds a branch whose
 * row says {@code a − b = f}; a state equation {@code d(x)/dt = g} adds a row for x — {@code g = 0} at DC, the
 * backward-Euler or trapezoidal difference in a transient. Discrete states, written by the behaviour, enter as
 * constants between events.</p>
 *
 * <p>Stamping runs in every Newton iteration, so everything that does not depend on the guess is found once, here:
 * which unknowns each argument reads, which of the unit's unknowns each equation's tangent reaches — directly or
 * through a charge — and the row each equation writes. The tangent is accumulated over the unit's few unknowns, not
 * over the circuit: a dense row per equation made every unit cost as much as the circuit is large.</p>
 */
final class ModelDevice implements Device {

    private static final int NOT_AN_UNKNOWN = -2;
    private static final int NONE           = -1;
    private static final String THERMAL_RESISTANCE_PARAMETER = "thermal_resistance";
    /** K/W of a DIP-8 package in still air, for a part whose document says nothing. */
    private static final double PACKAGE_THERMAL_RESISTANCE = 120;
    /**
     * How far a unit's unknown may move and still leave its last linearization in use: Newton's own settling tolerance,
     * as SPICE bypasses at its reltol — what is left between the two is second order in a move already that small.
     */
    private static final double BYPASS_RELATIVE = 1e-4;
    private static final double BYPASS_ABSOLUTE = 1e-6;

    private final Netlist.ModelUnit   unit;
    private final ModelEquations      equations;
    private final Map<String, Object> discrete;

    /** Each argument position's unknown, or {@link #NOT_AN_UNKNOWN}. */
    private final int[]    columns;
    private final int      timePosition;
    private final int[]    discretePositions;
    private final String[] discreteNames;
    /** The argument array every evaluation reads, and a second one for the prediction a truncation compares with. */
    private final double[] arguments;
    private final double[] predictedArguments;
    /** The arguments a dissipated current is read with: the solved state, every charge's rate at nought. */
    private final double[] powerArguments;
    /** Whether each equation reads a charge's rate — a capacitance's current, stored and given back, not heat. */
    private final boolean[] readsCharge;

    /** The unknowns this unit touches — its pins, internal nodes and states. */
    private final int[]    unitColumns;
    private final double[] unitGuess;
    private final double[] jacobian;

    /** Each charge's rate position among the arguments, its partials as last evaluated, and their local unknowns. */
    private final int[]      rateSymbols;
    private final double[][] chargePartials;
    private final int[][]    chargeLocals;
    private final double[]   chargePrevious;
    private final double[]   ratePrevious;
    /**
     * Where the integration's coefficient and each charge's history go among the arguments, and whether every
     * equation is linearized from its written-out form — the rates as coefficient · q − history inside one tape — rather
     * than from the charges and the equations apart.
     */
    private final int        coefficientPosition;
    private final int[]      historyPositions;
    private final boolean    linearizesWrittenOut;

    private final EquationStamp[] stamps;
    private final double[]        stateRatePrevious;

    /** The last linearization: each equation's value, its tangent over its locals, and the tangent times the guess. */
    private final double[]   values;
    private final double[][] tangents;
    private final double[]   linearParts;
    private Assembly         linearizedFor;
    /** No charge and no time among the arguments: a linearization can outlive the step it was made in. */
    private final boolean    stepIndependent;

    /**
     * Where each equation's tangent lands in the system, per local unknown: the row it adds to (a current's positive
     * pin, a voltage's branch, a state's row), the row it subtracts from (a current's negative pin), and a state's
     * diagonal — resolved once per system rather than looked up on every add.
     */
    private final int[][]       firstSlots;
    private final int[][]       secondSlots;
    private final int[]         diagonalSlots;
    private SparseSystem  slotsFor;

    /**
     * What one equation writes and where its tangent lands: for each of its partials the local unknown it reaches
     * directly, or the charge it reaches through; the local unknowns it can touch at all; its rows.
     *
     * @param positiveName the pin or internal node the equation's current leaves — by name, so two pins on one node
     *                     are still told apart when the current into each is reported
     */
    private record EquationStamp(Kind kind, Compiled formula, int[] partialLocal, int[] partialCharge, int[] locals,
                                 int positive, int negative, int ownRow, String positiveName, String negativeName,
                                 Compiled linearized, int[] linearizedLocal) {
    }

    ModelDevice(Netlist.ModelUnit unit, Map<String, Object> discrete) {
        this.unit = unit;
        this.discrete = discrete;
        List<String> pins = new ArrayList<>(unit.pins().keySet());
        this.equations = ModelEquations.of(unit.model(), unit.parameters(), pins, List.copyOf(unit.internalNodes().keySet()));
        List<ModelEquations.Argument> layout = equations.layout();
        this.columns = new int[layout.size()];
        this.rateSymbols = new int[equations.charges().size()];
        this.historyPositions = new int[equations.charges().size()];
        int coefficientAt = NONE;
        int time = NONE;
        List<Integer> discreteAt = new ArrayList<>();
        List<String> discreteNamed = new ArrayList<>();
        for (int position = 0; position < layout.size(); position++) {
            columns[position] = NOT_AN_UNKNOWN;
            switch (layout.get(position)) {
                case ModelEquations.Argument.NodeVoltage voltage -> {
                    Netlist.PinBinding binding = unit.pins().get(voltage.node());
                    if (binding != null) {
                        columns[position] = binding.isDigitalNode() ? NOT_AN_UNKNOWN : binding.analog();
                    } else {
                        columns[position] = unit.internalNodes().get(voltage.node());
                    }
                }
                case ModelEquations.Argument.StateValue state -> {
                    if (unit.stateRows().containsKey(state.state())) {
                        columns[position] = unit.stateRows().get(state.state());
                    } else if (discrete.containsKey(state.state())) {
                        discreteAt.add(position);
                        discreteNamed.add(state.state());
                    }
                }
                case ModelEquations.Argument.ChargeRate rate -> rateSymbols[rate.charge()] = position;
                case ModelEquations.Argument.BranchCurrent branch -> columns[position] = unit.voltageBranches().get(branch.branch());
                case ModelEquations.Argument.Time ignored -> time = position;
                case ModelEquations.Argument.RateCoefficient ignored -> coefficientAt = position;
                case ModelEquations.Argument.ChargeHistory history -> historyPositions[history.charge()] = position;
            }
        }
        this.coefficientPosition = coefficientAt;
        this.timePosition = time;
        this.discretePositions = discreteAt.stream().mapToInt(Integer::intValue).toArray();
        this.discreteNames = discreteNamed.toArray(String[]::new);
        this.arguments = equations.arguments();
        this.predictedArguments = equations.arguments();
        this.powerArguments = equations.arguments();

        this.unitColumns = Arrays.stream(columns).filter(column -> column >= 0).distinct().toArray();
        this.unitGuess = new double[unitColumns.length];
        this.jacobian = new double[unitColumns.length];
        int[] localOf = new int[columns.length];
        for (int position = 0; position < columns.length; position++) {
            localOf[position] = NONE;
            for (int local = 0; local < unitColumns.length; local++) {
                if (unitColumns[local] == columns[position]) {
                    localOf[position] = local;
                }
            }
        }

        int chargeCount = equations.charges().size();
        this.chargePartials = new double[chargeCount][];
        this.chargeLocals = new int[chargeCount][];
        for (int charge = 0; charge < chargeCount; charge++) {
            int[] symbols = equations.charges().get(charge).partialSymbols();
            chargePartials[charge] = new double[symbols.length];
            chargeLocals[charge] = Arrays.stream(symbols).map(symbol -> localOf[symbol]).toArray();
        }
        this.chargePrevious = new double[chargeCount];
        this.ratePrevious = new double[chargeCount];
        int[] chargeAt = new int[columns.length];
        Arrays.fill(chargeAt, NONE);
        for (int charge = 0; charge < chargeCount; charge++) {
            chargeAt[rateSymbols[charge]] = charge;
        }

        int equationCount = equations.equations().size();
        this.stamps = new EquationStamp[equationCount];
        this.stateRatePrevious = new double[equationCount];
        this.values = new double[equationCount];
        this.tangents = new double[equationCount][];
        this.linearParts = new double[equationCount];
        this.stepIndependent = chargeCount == 0 && timePosition == NONE;
        this.firstSlots = new int[equationCount][];
        this.secondSlots = new int[equationCount][];
        this.diagonalSlots = new int[equationCount];
        int voltageBranch = 0;
        for (int position = 0; position < equationCount; position++) {
            Equation equation = equations.equations().get(position);
            Compiled formula = equation.formula();
            int[] symbols = formula.partialSymbols();
            int[] partialLocal = new int[symbols.length];
            int[] partialCharge = new int[symbols.length];
            boolean[] reached = new boolean[unitColumns.length];
            for (int partial = 0; partial < symbols.length; partial++) {
                int symbol = symbols[partial];
                partialLocal[partial] = localOf[symbol];
                partialCharge[partial] = chargeAt[symbol];
                if (partialCharge[partial] != NONE) {
                    for (int local : chargeLocals[partialCharge[partial]]) {
                        if (local != NONE) {
                            reached[local] = true;
                        }
                    }
                } else if (partialLocal[partial] != NONE) {
                    reached[partialLocal[partial]] = true;
                }
            }
            int[] locals = IntStream.range(0, reached.length).filter(local -> reached[local]).toArray();
            int positive = equation.positive() == null ? Netlist.GROUND : pin(equation.positive());
            int negative = equation.negative() == null ? Netlist.GROUND : pin(equation.negative());
            int ownRow = switch (equation.kind()) {
                case CURRENT -> NONE;
                case VOLTAGE -> unit.voltageBranches().get(voltageBranch++);
                case STATE -> unit.stateRows().get(equation.state());
            };
            tangents[position] = new double[locals.length];
            Compiled linearized = equation.linearized();
            int[] linearizedLocal = linearized == null ? null
                    : Arrays.stream(linearized.partialSymbols()).map(symbol -> localOf[symbol]).toArray();
            stamps[position] = new EquationStamp(equation.kind(), formula, partialLocal, partialCharge, locals, positive,
                    negative, ownRow, equation.positive(), equation.negative(), linearized, linearizedLocal);
        }
        this.linearizesWrittenOut = chargeCount > 0 && Arrays.stream(stamps).allMatch(stamp -> stamp.linearized() != null);
        this.readsCharge = new boolean[equationCount];
        for (int position = 0; position < equationCount; position++) {
            readsCharge[position] = Arrays.stream(stamps[position].partialCharge()).anyMatch(charge -> charge != NONE);
        }
    }

    @Override
    public boolean nonlinear() {
        return true;
    }

    String label() {
        return unit.label();
    }

    /** A unit of a multi-unit part (`A1.b`) is that part. */
    @Override
    public String part() {
        String label = unit.label();
        int dot = label.indexOf('.');
        return dot < 0 ? label : label.substring(0, dot);
    }

    /**
     * The power it turns into heat: what its currents carry across the pins they flow between — a current equation's
     * value times its voltage, a voltage equation's branch current times its own —, the currents that charge its
     * capacitances left out. A charging current stores energy and gives it back; it is not dissipated. Counted in, it
     * was worse than wrong: after an edge the trapezoidal rule's rate rings, flipping sign every step and never dying
     * while the voltage stands still, and with steps that grow the flips did not cancel — a switched-off MOSFET at
     * 12 V "dissipated" 9 kW and heated as if it did.
     */
    @Override
    public double power(double[] state, double time) {
        double power = 0;
        boolean filled = false;
        for (int position = 0; position < stamps.length; position++) {
            EquationStamp stamp = stamps[position];
            double across = valueOf(state, stamp.positive()) - valueOf(state, stamp.negative());
            switch (stamp.kind()) {
                case CURRENT -> {
                    if (readsCharge[position] && !filled) {
                        fillDissipativeArguments(state, time);
                        filled = true;
                    }
                    power += (readsCharge[position] ? stamp.formula().evaluate(powerArguments) : solvedCurrent(position, state)) * across;
                }
                case VOLTAGE -> power += state[stamp.ownRow()] * across;
                case STATE -> {
                }
            }
        }
        return power;
    }

    /** The arguments a dissipated current is evaluated with: the solved state, its time, and every charge's rate at nought. */
    private void fillDissipativeArguments(double[] state, double time) {
        System.arraycopy(arguments, 0, powerArguments, 0, arguments.length);
        fillArguments(powerArguments, state, time);
        clearRates(powerArguments);
    }

    /**
     * A current equation's value at a solved state: the line it was stamped as — its value and tangent at the last
     * Newton guess — read at the solution, which is the current Kirchhoff's sums were solved with. The value at the guess
     * alone is not: a diode that turned off within the step kept its conducting current there, and that current times
     * its reverse voltage read as a diode delivering tens of milliwatts.
     */
    private double solvedCurrent(int position, double[] state) {
        int[] locals = stamps[position].locals();
        double[] tangent = tangents[position];
        double current = values[position];
        for (int index = 0; index < locals.length; index++) {
            current += tangent[index] * (state[unitColumns[locals[index]]] - unitGuess[locals[index]]);
        }
        return current;
    }

    @Override
    public Map<String, Double> stateValues(double[] state) {
        Map<String, Double> stateValues = new LinkedHashMap<>();
        unit.stateRows().forEach((name, row) -> stateValues.put(name, state[row]));
        discrete.forEach((name, value) -> {
            if (value instanceof Number number) {
                stateValues.put(name, number.doubleValue());
            } else if (value instanceof Boolean flag) {
                stateValues.put(name, flag ? 1.0 : 0.0);
            }
        });
        return stateValues;
    }

    /**
     * The current into each pin: a current equation's value enters its positive pin and leaves by its negative one, as
     * does a voltage equation's branch current — from the last, converged linearization, like {@link #power}.
     *
     * <p>The current into each analog pin: the sum of the currents of the equations that name it — what an equation
     * {@code current a -> b} carries leaves pin {@code a} inside the part, so it enters the part there. By name, never by
     * node: two pins on one node (a potentiometer's wiper and end both on ground) or a pin on ground each carry their
     * own, and a current between internal nodes reaches no pin.
     */
    @Override
    public Map<String, Double> terminalCurrents(double[] state, double[] companion, double time) {
        Map<String, Double> byName = new HashMap<>();
        for (int position = 0; position < stamps.length; position++) {
            EquationStamp stamp = stamps[position];
            double through = switch (stamp.kind()) {
                case CURRENT -> solvedCurrent(position, state);
                case VOLTAGE -> state[stamp.ownRow()];
                case STATE -> 0;
            };
            if (through != 0) {
                if (stamp.positiveName() != null) {
                    byName.merge(stamp.positiveName(), through, Double::sum);
                }
                if (stamp.negativeName() != null) {
                    byName.merge(stamp.negativeName(), -through, Double::sum);
                }
            }
        }
        Map<String, Double> pins = new LinkedHashMap<>();
        unit.pins().forEach((name, binding) -> {
            if (!binding.isDigitalNode()) {
                pins.put(name, byName.getOrDefault(name, 0.0));
            }
        });
        return pins;
    }

    /** The part's own figure when its document gives one (`thermal_resistance`), else a DIP-8 package in still air. */
    @Override
    public double thermalResistance() {
        Double declared = unit.parameters().get(THERMAL_RESISTANCE_PARAMETER);
        return declared != null && declared > 0 ? declared : PACKAGE_THERMAL_RESISTANCE;
    }

    private static double valueOf(double[] state, int node) {
        return node == Netlist.GROUND ? 0 : state[node];
    }

    @Override
    public void initialize(double[] state) {
        for (ComponentModel.State declared : unit.model().states()) {
            // A state with no row — its own heat on an ideal bench — has nothing to start.
            if (!declared.discrete() && unit.stateRows().containsKey(declared.name())) {
                state[unit.stateRows().get(declared.name())] = numeric(declared.initial());
            }
        }
    }

    @Override
    public void begin(double[] state) {
        fillArguments(arguments, state, 0);
        clearRates(arguments);
        for (int charge = 0; charge < equations.charges().size(); charge++) {
            chargePrevious[charge] = equations.charges().get(charge).evaluate(arguments);
            ratePrevious[charge] = 0;
        }
        for (int position = 0; position < stamps.length; position++) {
            if (stamps[position].kind() == Kind.STATE) {
                stateRatePrevious[position] = stamps[position].formula().evaluate(arguments);
            }
        }
    }

    /** A model's charges, measured as charges, and its continuous states, as themselves. */
    @Override
    public double truncation(double[] solution, double[] predicted) {
        double largest = 0;
        if (!equations.charges().isEmpty()) {
            fillArguments(arguments, solution, 0);
            fillArguments(predictedArguments, predicted, 0);
            for (Compiled formula : equations.charges()) {
                double stored = formula.evaluate(arguments);
                double error = stored - formula.evaluate(predictedArguments);
                largest = Math.max(largest, Truncation.share(error, stored, Truncation.CHARGE));
            }
        }
        for (int row : unit.stateRows().values()) {
            largest = Math.max(largest, Truncation.share(solution[row] - predicted[row], solution[row], Truncation.STATE));
        }
        return largest;
    }

    @Override
    public void stamp(Assembly assembly) {
        double coefficient = coefficient(assembly);
        boolean transientAnalysis = assembly.isTransient();
        boolean trapezoidal = transientAnalysis && assembly.integration() == Assembly.Integration.TRAPEZOIDAL;
        if (!bypasses(assembly)) {
            linearize(assembly, coefficient, trapezoidal, transientAnalysis);
        }
        if (slotsFor != assembly.system()) {
            resolveSlots(assembly);
        }
        for (int position = 0; position < stamps.length; position++) {
            EquationStamp stamp = stamps[position];
            double[] tangent = tangents[position];
            int[] first = firstSlots[position];
            double constant = values[position] - linearParts[position];
            switch (stamp.kind()) {
                case CURRENT -> {
                    int[] second = secondSlots[position];
                    for (int index = 0; index < tangent.length; index++) {
                        assembly.addAt(first[index], tangent[index]);
                        assembly.addAt(second[index], -tangent[index]);
                    }
                    assembly.current(stamp.positive(), stamp.negative(), constant);
                }
                case VOLTAGE -> {
                    assembly.branch(stamp.positive(), stamp.negative(), stamp.ownRow());
                    for (int index = 0; index < tangent.length; index++) {
                        assembly.addAt(first[index], -tangent[index]);
                    }
                    assembly.branchRightSide(stamp.ownRow(), constant);
                }
                case STATE -> {
                    // g linearized at the guess, against coefficient · (x − x_previous) − the previous rate: the guess
                    // itself cancels out of the right side.
                    int row = stamp.ownRow();
                    for (int index = 0; index < tangent.length; index++) {
                        assembly.addAt(first[index], -tangent[index]);
                    }
                    double right = constant;
                    if (transientAnalysis) {
                        assembly.addAt(diagonalSlots[position], coefficient);
                        right += coefficient * assembly.previousValue(row) + (trapezoidal ? stateRatePrevious[position] : 0);
                    }
                    assembly.inject(row, right);
                }
            }
        }
    }

    private boolean discreteUnchanged() {
        for (int index = 0; index < discretePositions.length; index++) {
            if (numeric(discrete.get(discreteNames[index])) != arguments[discretePositions[index]]) {
                return false;
            }
        }
        return true;
    }

    private void resolveSlots(Assembly assembly) {
        slotsFor = assembly.system();
        for (int position = 0; position < stamps.length; position++) {
            EquationStamp stamp = stamps[position];
            int[] locals = stamp.locals();
            int firstRow = stamp.kind() == Kind.CURRENT ? stamp.positive() : stamp.ownRow();
            firstSlots[position] = new int[locals.length];
            secondSlots[position] = new int[locals.length];
            for (int index = 0; index < locals.length; index++) {
                int column = unitColumns[locals[index]];
                firstSlots[position][index] = assembly.slot(firstRow, column);
                secondSlots[position][index] = stamp.kind() == Kind.CURRENT ? assembly.slot(stamp.negative(), column) : -1;
            }
            diagonalSlots[position] = stamp.kind() == Kind.STATE ? assembly.slot(stamp.ownRow(), stamp.ownRow()) : -1;
        }
    }

    /**
     * Whether the last linearization still holds, as SPICE bypasses a device: none of the unit's unknowns has moved
     * since by more than the bypass tolerance, and nothing else its equations read has changed. Within one assembly —
     * one attempt at a step — that is all of it. Across steps it holds only for a unit without charges and without
     * time, whose equations read nothing but its unknowns and its discrete states: a comparator between edges, an
     * amplifier at rest. The integration's coefficient enters a state row when it is written, not when it is
     * linearized, so a new step size does not undo the bypass. A unit never linearized has nothing to keep.
     */
    private boolean bypasses(Assembly assembly) {
        if (linearizedFor == null) {
            return false;
        }
        if (assembly != linearizedFor && !(stepIndependent && discreteUnchanged())) {
            return false;
        }
        for (int local = 0; local < unitColumns.length; local++) {
            double guess = assembly.unknown(unitColumns[local]);
            if (Math.abs(guess - unitGuess[local]) > BYPASS_RELATIVE * Math.abs(guess) + BYPASS_ABSOLUTE) {
                return false;
            }
        }
        return true;
    }

    /** Evaluates every equation and its tangent at the assembly's guess, for this and the bypassed iterations after. */
    private void linearize(Assembly assembly, double coefficient, boolean trapezoidal, boolean transientAnalysis) {
        linearizedFor = assembly;
        for (int local = 0; local < unitColumns.length; local++) {
            unitGuess[local] = assembly.unknown(unitColumns[local]);
        }
        for (int position = 0; position < columns.length; position++) {
            if (columns[position] >= 0) {
                arguments[position] = assembly.unknown(columns[position]);
            }
        }
        fillDiscrete(arguments, assembly.time());
        if (linearizesWrittenOut) {
            fillHistories(coefficient, trapezoidal, transientAnalysis);
        } else {
            evaluateRates(coefficient, trapezoidal, transientAnalysis);
        }
        for (int position = 0; position < stamps.length; position++) {
            EquationStamp stamp = stamps[position];
            if (linearizesWrittenOut) {
                values[position] = stamp.linearized().evaluateWithPartials(arguments);
                accumulateWrittenOutTangent(stamp);
            } else {
                values[position] = stamp.formula().evaluateWithPartials(arguments);
                accumulateTangent(stamp, coefficient);
            }
            int[] locals = stamp.locals();
            double[] tangent = tangents[position];
            double linear = 0;
            for (int index = 0; index < locals.length; index++) {
                tangent[index] = jacobian[locals[index]];
                linear += tangent[index] * unitGuess[locals[index]];
            }
            linearParts[position] = linear;
        }
    }

    @Override
    public void accept(Assembly assembly, double[] state) {
        double coefficient = coefficient(assembly);
        boolean trapezoidal = assembly.integration() == Assembly.Integration.TRAPEZOIDAL;
        fillArguments(arguments, state, assembly.time());
        for (int charge = 0; charge < equations.charges().size(); charge++) {
            double value = equations.charges().get(charge).evaluate(arguments);
            ratePrevious[charge] = coefficient * (value - chargePrevious[charge]) - (trapezoidal ? ratePrevious[charge] : 0);
            chargePrevious[charge] = value;
            arguments[rateSymbols[charge]] = ratePrevious[charge];
        }
        for (int position = 0; position < stamps.length; position++) {
            if (stamps[position].kind() == Kind.STATE) {
                stateRatePrevious[position] = stamps[position].formula().evaluate(arguments);
            }
        }
    }

    /** How far its own heat has raised it above the bench, kelvin, in a solved state; NaN for a model that keeps none. */
    @Override
    public double heating(double[] state) {
        Integer row = unit.stateRows().get(ModelEquations.HEATING);
        return row == null ? Double.NaN : state[row];
    }

    /** Each {@code noise(density)} of the equations, its density read at the operating point, along its equation. */
    @Override
    public List<NoiseCurrent> noise(double[] operatingPoint, double temperature) {
        if (equations.noises().isEmpty()) {
            return List.of();
        }
        // Arguments of its own: a transient asks for noise between its steps, and must find its own as it left them.
        double[] at = equations.arguments();
        fillArguments(at, operatingPoint, 0);
        clearRates(at);
        List<NoiseCurrent> currents = new ArrayList<>();
        for (ModelEquations.Noise noise : equations.noises()) {
            EquationStamp stamp = stamps[noise.equation()];
            currents.add(new NoiseCurrent(stamp.positive(), stamp.negative(), Math.max(0, noise.density().evaluate(at)),
                    Math.max(0, noise.corner().evaluate(at))));
        }
        return currents;
    }

    @Override
    public void stampPhasor(PhasorAssembly assembly) {
        fillArguments(arguments, null, 0);
        clearRates(arguments);
        for (int position = 0; position < columns.length; position++) {
            if (columns[position] >= 0) {
                arguments[position] = assembly.operatingValue(columns[position]);
            }
        }
        Complex rate = new Complex(0, assembly.angular());
        for (EquationStamp stamp : stamps) {
            Map<Integer, Complex> phasorJacobian = new LinkedHashMap<>();
            Compiled formula = stamp.formula();
            formula.evaluateWithPartials(arguments);
            double[] derivatives = formula.derivatives().clone();
            for (int partial = 0; partial < derivatives.length; partial++) {
                double derivative = derivatives[partial];
                int charge = stamp.partialCharge()[partial];
                if (charge != NONE) {
                    Compiled chargeFormula = equations.charges().get(charge);
                    chargeFormula.evaluateWithPartials(arguments);
                    for (int inner = 0; inner < chargeLocals[charge].length; inner++) {
                        int local = chargeLocals[charge][inner];
                        if (local != NONE) {
                            double value = derivative * chargeFormula.derivatives()[inner];
                            phasorJacobian.merge(unitColumns[local], rate.multiply(Complex.of(value)), Complex::add);
                        }
                    }
                } else if (stamp.partialLocal()[partial] != NONE) {
                    phasorJacobian.merge(unitColumns[stamp.partialLocal()[partial]], Complex.of(derivative), Complex::add);
                }
            }
            switch (stamp.kind()) {
                case CURRENT -> phasorJacobian.forEach((column, value) -> {
                    assembly.add(stamp.positive(), column, value);
                    assembly.add(stamp.negative(), column, value.multiply(Complex.of(-1)));
                });
                case VOLTAGE -> {
                    assembly.branch(stamp.positive(), stamp.negative(), stamp.ownRow());
                    phasorJacobian.forEach((column, value) -> assembly.add(stamp.ownRow(), column, value.multiply(Complex.of(-1))));
                }
                case STATE -> {
                    assembly.add(stamp.ownRow(), stamp.ownRow(), rate);
                    phasorJacobian.forEach((column, value) -> assembly.add(stamp.ownRow(), column, value.multiply(Complex.of(-1))));
                }
            }
        }
    }

    private double coefficient(Assembly assembly) {
        if (!assembly.isTransient()) {
            return 0;
        }
        return (assembly.integration() == Assembly.Integration.TRAPEZOIDAL ? 2 : 1) / assembly.step();
    }

    /** Evaluates every charge with its partials and writes its rate into the arguments. */
    private void evaluateRates(double coefficient, boolean trapezoidal, boolean transientAnalysis) {
        for (int charge = 0; charge < rateSymbols.length; charge++) {
            Compiled formula = equations.charges().get(charge);
            double value = formula.evaluateWithPartials(arguments);
            arguments[rateSymbols[charge]] = transientAnalysis
                    ? coefficient * (value - chargePrevious[charge]) - (trapezoidal ? ratePrevious[charge] : 0)
                    : 0;
            System.arraycopy(formula.derivatives(), 0, chargePartials[charge], 0, chargePartials[charge].length);
        }
    }

    /**
     * What a written-out equation reads besides the unknowns: the coefficient, and each charge's history — so that its
     * rate, coefficient · q − history, is coefficient · (q − q_previous), less the previous rate under the trapezoidal
     * rule; at rest every rate is nought.
     */
    private void fillHistories(double coefficient, boolean trapezoidal, boolean transientAnalysis) {
        arguments[coefficientPosition] = transientAnalysis ? coefficient : 0;
        for (int charge = 0; charge < historyPositions.length; charge++) {
            arguments[historyPositions[charge]] = transientAnalysis
                    ? coefficient * chargePrevious[charge] + (trapezoidal ? ratePrevious[charge] : 0)
                    : 0;
        }
    }

    /** A written-out equation's tangent: its partials alone, the charges' already inside them. */
    private void accumulateWrittenOutTangent(EquationStamp stamp) {
        for (int local : stamp.locals()) {
            jacobian[local] = 0;
        }
        double[] derivatives = stamp.linearized().derivatives();
        int[] linearizedLocal = stamp.linearizedLocal();
        for (int partial = 0; partial < derivatives.length; partial++) {
            if (linearizedLocal[partial] != NONE) {
                jacobian[linearizedLocal[partial]] += derivatives[partial];
            }
        }
    }

    /** The equation's tangent over the unit's unknowns: its direct partials, and through each charge rate the charge's. */
    private void accumulateTangent(EquationStamp stamp, double coefficient) {
        for (int local : stamp.locals()) {
            jacobian[local] = 0;
        }
        double[] derivatives = stamp.formula().derivatives();
        int[] partialLocal = stamp.partialLocal();
        int[] partialCharge = stamp.partialCharge();
        for (int partial = 0; partial < derivatives.length; partial++) {
            int charge = partialCharge[partial];
            if (charge != NONE) {
                double scale = derivatives[partial] * coefficient;
                int[] locals = chargeLocals[charge];
                double[] partials = chargePartials[charge];
                for (int inner = 0; inner < locals.length; inner++) {
                    if (locals[inner] != NONE) {
                        jacobian[locals[inner]] += scale * partials[inner];
                    }
                }
            } else if (partialLocal[partial] != NONE) {
                jacobian[partialLocal[partial]] += derivatives[partial];
            }
        }
    }

    /** Every symbol's value read from a state vector (none: left as it is), the discrete states and the time. */
    private void fillArguments(double[] into, double[] state, double time) {
        if (state != null) {
            for (int position = 0; position < columns.length; position++) {
                if (columns[position] >= 0) {
                    into[position] = state[columns[position]];
                }
            }
        }
        fillDiscrete(into, time);
    }

    /** No charge is changing: at the start, and around the operating point a phasor is taken at. */
    private void clearRates(double[] into) {
        for (int position : rateSymbols) {
            into[position] = 0;
        }
    }

    private void fillDiscrete(double[] into, double time) {
        if (timePosition != NONE) {
            into[timePosition] = time;
        }
        for (int index = 0; index < discretePositions.length; index++) {
            into[discretePositions[index]] = numeric(discrete.get(discreteNames[index]));
        }
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

    private int pin(String name) {
        Netlist.PinBinding binding = unit.pins().get(name);
        if (binding != null) {
            return binding.isDigitalNode() ? Netlist.GROUND : binding.analog();
        }
        return unit.internalNodes().get(name);
    }
}
