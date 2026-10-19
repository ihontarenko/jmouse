package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ComponentVocabulary;

import org.jmouse.formula.Complex;
import org.jmouse.parallax.component.ComponentModel.EquationKind;
import org.jmouse.parallax.circuit.ModelEquations.Compiled;
import org.jmouse.parallax.numerics.SparseSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One unit of a jMC model as a device: its equations stamped as their tangent at the Newton guess.
 *
 * <p>A current equation {@code current a -> b = f} adds f leaving a and entering b; a voltage equation adds a branch whose
 * row says {@code a − b = f}; a state equation {@code d(x)/dt = g} adds a row for x — {@code g = 0} at DC, the
 * backward-Euler or trapezoidal difference in a transient. Discrete states, written by the behaviour, enter as
 * constants between events.</p>
 *
 * <p>It is put together from what it is made of: where everything stands ({@link UnitLayout}), the arguments its
 * equations read ({@link UnitArguments}), the line it is stamped as ({@link UnitLine}) and how that line is found
 * ({@link UnitLinearization} — from constant coefficients for an affine unit, from its tapes for any other), and what
 * it carries from step to step ({@link UnitHistory}). This class stamps the line, and reads what the unit does off it.</p>
 */
final class ModelDevice implements Device {

    private final Netlist.ModelUnit   unit;
    private final ModelEquations      equations;
    private final Map<String, Object> discrete;
    private final UnitLayout          layout;
    private final UnitArguments       arguments;
    private final UnitLine            line;
    private final UnitHistory         history;
    private final UnitLinearization   linearization;
    /** Each current equation's dissipated current, as {@link #power} reads it. */
    private final double[]            dissipated;

    /**
     * Its own heat, as its component's {@code heat} law writes it ({@link PartHeat}); null for a part that does not
     * warm. The engine advances it after every accepted step; on a realistic bench ({@code feedsBack}) its equations
     * read the temperature it makes as {@code temperature}, between steps and never inside Newton — so a part that
     * drifts with its temperature is as linear, and as cheap, as it is at a fixed one. On an ideal bench it only says
     * how warm the part would run.
     */
    private final PartHeat heat;
    private final boolean  feedsBack;

    /**
     * Where each equation's tangent lands in the system, per local unknown: the row it adds to (a current's positive
     * pin, a voltage's branch, a state's row), the row it subtracts from (a current's negative pin), and a state's
     * diagonal — resolved once per system rather than looked up on every add.
     */
    private final int[][]  firstSlots;
    private final int[][]  secondSlots;
    private final int[]    diagonalSlots;
    private SparseSystem   slotsFor;

    ModelDevice(Netlist.ModelUnit unit, Map<String, Object> discrete) {
        this.unit = unit;
        this.discrete = discrete;
        this.equations = ModelEquations.of(unit.model(), unit.parameters(), new ArrayList<>(unit.pins().keySet()),
                List.copyOf(unit.internalNodes().keySet()));
        this.layout = new UnitLayout(unit, equations, discrete);
        this.arguments = new UnitArguments(equations, layout, discrete);
        this.line = new UnitLine(layout);
        this.history = new UnitHistory(layout.charges(), layout.stamps.length);
        this.dissipated = new double[layout.stamps.length];
        boolean realistic = unit.parameters().getOrDefault(ComponentVocabulary.REALISTIC, 0.0) != 0;
        this.heat = layout.temperaturePosition < 0 ? null
                : PartHeat.of(unit.model(), unit.parameters(), arguments.values[layout.temperaturePosition]);
        this.feedsBack = realistic && heat != null;
        PartHeat ownHeat = feedsBack ? heat : null;
        boolean affine = equations.linear() && !equations.readsTime() && layout.discretePositions.length == 0;
        this.linearization = affine
                ? new AffineLinearization(equations, layout, arguments, line, history, ownHeat)
                : new TapedLinearization(equations, layout, arguments, line, history, ownHeat);
        int equationCount = layout.stamps.length;
        this.firstSlots = new int[equationCount][];
        this.secondSlots = new int[equationCount][];
        this.diagonalSlots = new int[equationCount];
    }

    @Override
    public PartHeat heat() {
        return heat;
    }

    /** On a realistic bench, its own temperature into every argument array its equations are read with. */
    private void readOwnTemperature() {
        if (feedsBack) {
            arguments.temperature(heat.temperature());
        }
    }

    /** Nonlinear unless its equations are proved linear in its unknowns: a linear model is solved in one step, as a resistor. */
    @Override
    public boolean nonlinear() {
        return !equations.linear();
    }

    /**
     * Whether its stamp is the same for the whole run, as a resistor's is: linear, reading no charge, no time and no
     * discrete state, at a temperature its own heat does not move. Stamped once, a model written as a resistance costs
     * what the engine's own resistor does.
     */
    @Override
    public boolean invariant() {
        return equations.linear() && layout.stepIndependent && layout.discretePositions.length == 0 && !feedsBack;
    }

    /** A unit of a multi-unit part (`A1.b`) is that part. */
    @Override
    public String part() {
        return unit.part();
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
        linearization.dissipatedCurrents(state, time, dissipated);
        double power = 0;
        for (int position = 0; position < layout.stamps.length; position++) {
            UnitLayout.EquationStamp stamp = layout.stamps[position];
            double across = valueOf(state, stamp.positive()) - valueOf(state, stamp.negative());
            switch (stamp.kind()) {
                case CURRENT -> power += dissipated[position] * across;
                case VOLTAGE -> power += state[stamp.ownRow()] * across;
                case STATE -> {
                }
            }
        }
        return power;
    }

    /**
     * Its states by name, and — when it heats itself — its own {@code temperature}, so that a figure it displays over
     * the temperature reads the one its equations read.
     */
    @Override
    public Map<String, Double> stateValues(double[] state) {
        Map<String, Double> stateValues = new LinkedHashMap<>();
        unit.stateRows().forEach((name, row) -> stateValues.put(name, state[row]));
        if (feedsBack) {
            stateValues.put(ComponentVocabulary.TEMPERATURE, heat.temperature());
        }
        discrete.forEach((name, value) -> {
            if (value instanceof Number number) {
                stateValues.put(name, number.doubleValue());
            } else if (value instanceof Boolean flag) {
                stateValues.put(name, flag ? 1.0 : 0.0);
            }
        });
        return stateValues;
    }

    /** Its analog pins, each under the name it is reported by. */
    @Override
    public String[] reportedTerminals() {
        return layout.reportedTerminals;
    }

    /**
     * The current into each analog pin: the sum of the currents of the equations that name it — what an equation
     * {@code current a -> b} carries leaves pin {@code a} inside the part, so it enters the part there, as does a voltage
     * equation's branch current —, from the last, converged linearization, like {@link #power}. By name, never by node:
     * two pins on one node (a potentiometer's wiper and end both on ground) or a pin on ground each carry their own, and
     * a current between internal nodes reaches no pin.
     */
    @Override
    public void terminalCurrents(double[] state, double[] companion, double time, double[] into) {
        Arrays.fill(into, 0, layout.reportedTerminals.length, 0);
        for (int position = 0; position < layout.stamps.length; position++) {
            UnitLayout.EquationStamp stamp = layout.stamps[position];
            double through = switch (stamp.kind()) {
                case CURRENT -> line.solved(position, state);
                case VOLTAGE -> state[stamp.ownRow()];
                case STATE -> 0;
            };
            if (layout.positiveTerminal[position] != UnitLayout.NONE) {
                into[layout.positiveTerminal[position]] += through;
            }
            if (layout.negativeTerminal[position] != UnitLayout.NONE) {
                into[layout.negativeTerminal[position]] -= through;
            }
        }
    }

    private static double valueOf(double[] state, int node) {
        return node == Netlist.GROUND ? 0 : state[node];
    }

    @Override
    public void initialize(double[] state) {
        for (ComponentModel.State declared : unit.model().states()) {
            // A state with no row — its own heat on an ideal bench — has nothing to start.
            if (!declared.discrete() && unit.stateRows().containsKey(declared.name())) {
                state[unit.stateRows().get(declared.name())] = UnitArguments.numeric(declared.initial());
            }
        }
    }

    @Override
    public void begin(double[] state) {
        arguments.fill(arguments.values, state, 0);
        arguments.clearRates(arguments.values);
        for (int charge = 0; charge < layout.charges(); charge++) {
            history.start(charge, equations.charges().get(charge).evaluate(arguments.values));
        }
        for (int position = 0; position < layout.stamps.length; position++) {
            if (layout.stamps[position].kind() == EquationKind.STATE) {
                history.stateRatePrevious[position] = layout.stamps[position].formula().evaluate(arguments.values);
            }
        }
    }

    /** What it carries from step to step: its charges' history and its continuous states' rates. */
    @Override
    public boolean stores() {
        return layout.charges() > 0 || layout.hasStates;
    }

    /** Its charges, at the attempt's solution, once: the error and the acceptance both read them. */
    @Override
    public void measure(double[] solution, double time) {
        if (layout.charges() > 0) {
            readOwnTemperature();
            linearization.charges(solution, time, history.measured);
        }
    }

    /** A model's charges, measured as charges against their own history, and its continuous states, as themselves. */
    @Override
    public double truncation(double[] solution, Prediction prediction) {
        double largest = history.chargeTruncation(prediction);
        double[] predicted = prediction.values();
        for (int row : layout.stateRows) {
            largest = Math.max(largest, Truncation.share(solution[row] - predicted[row], solution[row], Truncation.STATE));
        }
        return largest;
    }

    @Override
    public void stamp(Assembly assembly) {
        double coefficient = coefficient(assembly);
        boolean transientAnalysis = assembly.isTransient();
        boolean trapezoidal = transientAnalysis && assembly.integration() == Assembly.Integration.TRAPEZOIDAL;
        linearization.linearize(assembly, coefficient, trapezoidal, transientAnalysis);
        if (slotsFor != assembly.system()) {
            resolveSlots(assembly);
        }
        for (int position = 0; position < layout.stamps.length; position++) {
            UnitLayout.EquationStamp stamp = layout.stamps[position];
            double[] tangent = line.tangents[position];
            int[] first = firstSlots[position];
            double constant = line.values[position] - line.linearParts[position];
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
                        right += coefficient * assembly.previousValue(row) + (trapezoidal ? history.stateRatePrevious[position] : 0);
                    }
                    assembly.inject(row, right);
                }
            }
        }
    }

    private void resolveSlots(Assembly assembly) {
        slotsFor = assembly.system();
        for (int position = 0; position < layout.stamps.length; position++) {
            UnitLayout.EquationStamp stamp = layout.stamps[position];
            int[] locals = stamp.locals();
            int firstRow = stamp.kind() == EquationKind.CURRENT ? stamp.positive() : stamp.ownRow();
            firstSlots[position] = new int[locals.length];
            secondSlots[position] = new int[locals.length];
            for (int index = 0; index < locals.length; index++) {
                int column = layout.unitColumns[locals[index]];
                firstSlots[position][index] = assembly.slot(firstRow, column);
                secondSlots[position][index] = stamp.kind() == EquationKind.CURRENT ? assembly.slot(stamp.negative(), column) : -1;
            }
            diagonalSlots[position] = stamp.kind() == EquationKind.STATE ? assembly.slot(stamp.ownRow(), stamp.ownRow()) : -1;
        }
    }

    @Override
    public void accept(Assembly assembly, double[] state) {
        double coefficient = coefficient(assembly);
        boolean trapezoidal = assembly.integration() == Assembly.Integration.TRAPEZOIDAL;
        history.accept(coefficient, trapezoidal);
        if (!layout.hasStates) {
            return;
        }
        // A state's rate reads the charges' rates over the step just taken.
        readOwnTemperature();
        double[] values = arguments.values;
        arguments.fill(values, state, assembly.time());
        for (int charge = 0; charge < layout.charges(); charge++) {
            values[layout.rateSymbols[charge]] = history.ratePrevious[charge];
        }
        for (int position = 0; position < layout.stamps.length; position++) {
            if (layout.stamps[position].kind() == EquationKind.STATE) {
                history.stateRatePrevious[position] = layout.stamps[position].formula().evaluate(values);
            }
        }
    }

    /** Each {@code noise(density)} of the equations, its density read at the operating point, along its equation. */
    @Override
    public List<NoiseCurrent> noise(double[] operatingPoint) {
        if (equations.noises().isEmpty()) {
            return List.of();
        }
        // Arguments of its own: a transient asks for noise between its steps, and must find its own as it left them.
        double[] at = arguments.fresh();
        if (feedsBack) {
            // As warm as it is: a junction's shot noise and a channel's thermal noise follow its own temperature.
            at[layout.temperaturePosition] = heat.temperature();
        }
        arguments.fill(at, operatingPoint, 0);
        arguments.clearRates(at);
        List<NoiseCurrent> currents = new ArrayList<>();
        for (ModelEquations.Noise noise : equations.noises()) {
            UnitLayout.EquationStamp stamp = layout.stamps[noise.equation()];
            currents.add(new NoiseCurrent(stamp.positive(), stamp.negative(), Math.max(0, noise.density().evaluate(at)),
                    Math.max(0, noise.corner().evaluate(at))));
        }
        return currents;
    }

    @Override
    public void stampPhasor(PhasorAssembly assembly) {
        double[] values = arguments.values;
        arguments.fill(values, null, 0);
        arguments.clearRates(values);
        for (int position = 0; position < layout.columns.length; position++) {
            if (layout.columns[position] >= 0) {
                values[position] = assembly.operatingValue(layout.columns[position]);
            }
        }
        Complex rate = new Complex(0, assembly.angular());
        for (UnitLayout.EquationStamp stamp : layout.stamps) {
            Map<Integer, Complex> phasorJacobian = phasorJacobian(stamp, rate);
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

    /**
     * One equation's small-signal tangent at the arguments as they stand — the operating point —, by unknown: a direct
     * partial as it is, a partial through a charge times jω. What {@link #stampPhasor} stamps and
     * {@link #phasorCurrent} reads, so the two never differ.
     */
    private Map<Integer, Complex> phasorJacobian(UnitLayout.EquationStamp stamp, Complex rate) {
        Map<Integer, Complex> phasorJacobian = new LinkedHashMap<>();
        Compiled formula = stamp.formula();
        formula.evaluateWithPartials(arguments.values);
        double[] derivatives = formula.derivatives().clone();
        for (int partial = 0; partial < derivatives.length; partial++) {
            double derivative = derivatives[partial];
            int charge = stamp.partialCharge()[partial];
            if (charge != UnitLayout.NONE) {
                Compiled chargeFormula = equations.charges().get(charge);
                chargeFormula.evaluateWithPartials(arguments.values);
                for (int inner = 0; inner < layout.chargeLocals[charge].length; inner++) {
                    int local = layout.chargeLocals[charge][inner];
                    if (local != UnitLayout.NONE) {
                        double value = derivative * chargeFormula.derivatives()[inner];
                        phasorJacobian.merge(layout.unitColumns[local], rate.multiply(Complex.of(value)), Complex::add);
                    }
                }
            } else if (stamp.partialLocal()[partial] != UnitLayout.NONE) {
                phasorJacobian.merge(layout.unitColumns[stamp.partialLocal()[partial]], Complex.of(derivative), Complex::add);
            }
        }
        return phasorJacobian;
    }

    /**
     * The phasor of the current into a two-pin model's first pin, out of its second: the small-signal current of every
     * equation leaving or entering that pin, and every branch current on it; null for a model with other than two pins.
     */
    @Override
    public Complex phasorCurrent(Complex[] solution, double angular, double[] operatingPoint) {
        List<String> pins = new ArrayList<>(unit.pins().keySet());
        if (pins.size() != 2) {
            return null;
        }
        String first = pins.getFirst();
        arguments.fill(arguments.values, operatingPoint, 0);
        arguments.clearRates(arguments.values);
        Complex rate = new Complex(0, angular);
        Complex current = Complex.of(0);
        for (UnitLayout.EquationStamp stamp : layout.stamps) {
            int sign = first.equals(stamp.positiveName()) ? 1 : first.equals(stamp.negativeName()) ? -1 : 0;
            if (sign == 0) {
                continue;
            }
            Complex through = switch (stamp.kind()) {
                case CURRENT -> phasorJacobian(stamp, rate).entrySet().stream()
                        .map(entry -> entry.getValue().multiply(solution[entry.getKey()]))
                        .reduce(Complex.of(0), Complex::add);
                case VOLTAGE -> solution[stamp.ownRow()];
                case STATE -> Complex.of(0);
            };
            current = current.add(through.multiply(Complex.of(sign)));
        }
        return current;
    }

    /** The phasor across a two-pin model, first pin less second; null for a model with other than two pins. */
    @Override
    public Complex phasorVoltage(Complex[] solution) {
        List<Netlist.PinBinding> bindings = new ArrayList<>(unit.pins().values());
        if (bindings.size() != 2 || bindings.get(0).isDigitalNode() || bindings.get(1).isDigitalNode()) {
            return null;
        }
        return Device.phasorAt(solution, bindings.get(0).analog()).subtract(Device.phasorAt(solution, bindings.get(1).analog()));
    }

    /** A generator drives the circuit: a voltage of the time between its two pins ({@link ComponentModel#isGenerator}). */
    @Override
    public boolean source() {
        return unit.model().isGenerator();
    }

    private double coefficient(Assembly assembly) {
        if (!assembly.isTransient()) {
            return 0;
        }
        return (assembly.integration() == Assembly.Integration.TRAPEZOIDAL ? 2 : 1) / assembly.step();
    }
}
