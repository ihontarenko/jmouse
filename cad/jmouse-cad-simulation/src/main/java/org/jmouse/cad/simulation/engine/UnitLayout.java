package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.simulation.EngineName;

import org.jmouse.cad.simulation.engine.ModelEquations.Kind;
import org.jmouse.cad.simulation.engine.ModelEquations.Compiled;
import org.jmouse.cad.simulation.engine.ModelEquations.CompiledEquation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Where everything a model unit's equations read and write stands, found once when the unit is bound: which unknown
 * each argument position reads, which of the unit's unknowns each equation's tangent reaches — directly or through a
 * charge —, the row each equation writes, and the terminal each equation's current enters and leaves by. Stamping runs
 * in every Newton iteration; nothing here is looked up there again.
 *
 * <p>The unit's unknowns are its <em>locals</em>: its pins, internal nodes, branch currents and states, numbered
 * among themselves. A tangent is accumulated over those few, not over the circuit: a dense row per equation made every
 * unit cost as much as the circuit is large.</p>
 */
final class UnitLayout {

    /** An argument position that reads no unknown. */
    static final int NOT_AN_UNKNOWN = -2;
    /** No such position, row, local or terminal. */
    static final int NONE           = -1;

    /**
     * What one equation writes and where its tangent lands: for each of its partials the local unknown it reaches
     * directly, or the charge it reaches through; the local unknowns it can touch at all; its rows.
     *
     * @param positiveName the pin or internal node the equation's current leaves — by name, so two pins on one node
     *                     are still told apart when the current into each is reported
     */
    record EquationStamp(Kind kind, Compiled formula, int[] partialLocal, int[] partialCharge, int[] locals,
                         int positive, int negative, int ownRow, String positiveName, String negativeName,
                         Compiled linearized, int[] linearizedLocal) {
    }

    /** Each argument position's unknown, or {@link #NOT_AN_UNKNOWN}. */
    final int[]      columns;
    final int        timePosition;
    final int[]      discretePositions;
    /** The discrete state each position reads: a single state's name, or — a slot — its row's. */
    final String[]   discreteNames;
    /** The slot of the row each position reads, or {@link #NONE} for a single state. */
    final int[]      discreteSlots;
    /** Each charge's rate position among the arguments, and where its history and the integration's coefficient go. */
    final int[]      rateSymbols;
    final int[]      historyPositions;
    final int        coefficientPosition;
    /** The unknowns this unit touches — its pins, internal nodes and states —, by local number. */
    final int[]      unitColumns;
    /** Each charge's partials' local unknowns, in the order its partials are written. */
    final int[][]    chargeLocals;
    final EquationStamp[] stamps;
    /** Whether each equation reads a charge's rate — a capacitance's current, stored and given back, not heat. */
    final boolean[]  readsCharge;
    /**
     * Whether every equation is linearized from its written-out form — the rates as coefficient · q − history inside one
     * tape — rather than from the charges and the equations apart.
     */
    final boolean    linearizesWrittenOut;
    /** No charge and no time among what the equations read: a linearization can outlive the step it was made in. */
    final boolean    stepIndependent;
    /** Whether any equation is a state's rate. */
    final boolean    hasStates;
    /** Each continuous state's row in the system, as the unit declares them — read on every step, so never a map. */
    final int[]      stateRows;
    /** Where {@code temperature} stands among the arguments; -1 for a unit that does not read it. */
    final int        temperaturePosition;
    /**
     * The terminals the unit reports currents into — its analog pins, each under the name it is reported by —, and for
     * each equation the terminal its current leaves and the one it enters, {@link #NONE} for an internal node or a
     * digital pin.
     */
    final String[]   reportedTerminals;
    final int[]      positiveTerminal;
    final int[]      negativeTerminal;

    UnitLayout(Netlist.ModelUnit unit, ModelEquations equations, Map<String, Object> discrete) {
        List<ModelEquations.Argument> layout = equations.layout();
        this.columns = new int[layout.size()];
        int chargeCount = equations.charges().size();
        this.rateSymbols = new int[chargeCount];
        this.historyPositions = new int[chargeCount];
        int coefficientAt = NONE;
        int time = NONE;
        List<Integer> discreteAt = new ArrayList<>();
        List<String> discreteNamed = new ArrayList<>();
        List<Integer> discreteSlotted = new ArrayList<>();
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
                        discreteSlotted.add(NONE);
                    } else if (slotOf(state.state(), discrete) != NONE) {
                        // registers_12: slot 12 of the row registers.
                        String row = state.state().substring(0, state.state().lastIndexOf('_'));
                        discreteAt.add(position);
                        discreteNamed.add(row);
                        discreteSlotted.add(slotOf(state.state(), discrete));
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
        this.discreteSlots = discreteSlotted.stream().mapToInt(Integer::intValue).toArray();

        this.unitColumns = Arrays.stream(columns).filter(column -> column >= 0).distinct().toArray();
        int[] localOf = new int[columns.length];
        for (int position = 0; position < columns.length; position++) {
            localOf[position] = NONE;
            for (int local = 0; local < unitColumns.length; local++) {
                if (unitColumns[local] == columns[position]) {
                    localOf[position] = local;
                }
            }
        }

        this.chargeLocals = new int[chargeCount][];
        for (int charge = 0; charge < chargeCount; charge++) {
            int[] symbols = equations.charges().get(charge).partialSymbols();
            chargeLocals[charge] = Arrays.stream(symbols).map(symbol -> localOf[symbol]).toArray();
        }
        int[] chargeAt = new int[columns.length];
        Arrays.fill(chargeAt, NONE);
        for (int charge = 0; charge < chargeCount; charge++) {
            chargeAt[rateSymbols[charge]] = charge;
        }

        int equationCount = equations.equations().size();
        this.stamps = new EquationStamp[equationCount];
        // The time has a place among the arguments always; what counts is whether an equation reads it.
        this.stepIndependent = chargeCount == 0 && !equations.readsTime();
        int voltageBranch = 0;
        for (int position = 0; position < equationCount; position++) {
            CompiledEquation equation = equations.equations().get(position);
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
            int positive = equation.positive() == null ? Netlist.GROUND : row(unit, equation.positive());
            int negative = equation.negative() == null ? Netlist.GROUND : row(unit, equation.negative());
            int ownRow = switch (equation.kind()) {
                case CURRENT -> NONE;
                case VOLTAGE -> unit.voltageBranches().get(voltageBranch++);
                case STATE -> unit.stateRows().get(equation.state());
            };
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
        this.hasStates = Arrays.stream(stamps).anyMatch(stamp -> stamp.kind() == Kind.STATE);
        this.stateRows = unit.stateRows().values().stream().mapToInt(Integer::intValue).toArray();
        this.temperaturePosition = equations.parameterPosition(EngineName.TEMPERATURE.written());

        List<String> analogPins = new ArrayList<>();
        unit.pins().forEach((name, binding) -> {
            if (!binding.isDigitalNode()) {
                analogPins.add(name);
            }
        });
        this.reportedTerminals = analogPins.stream().map(pin -> unit.reportedAs().getOrDefault(pin, pin)).toArray(String[]::new);
        this.positiveTerminal = new int[equationCount];
        this.negativeTerminal = new int[equationCount];
        for (int position = 0; position < equationCount; position++) {
            positiveTerminal[position] = stamps[position].positiveName() == null ? NONE : analogPins.indexOf(stamps[position].positiveName());
            negativeTerminal[position] = stamps[position].negativeName() == null ? NONE : analogPins.indexOf(stamps[position].negativeName());
        }
    }

    int charges() {
        return rateSymbols.length;
    }

    /** The row a pin's or an internal node's voltage is, {@link Netlist#GROUND} for a digital pin. */
    private static int row(Netlist.ModelUnit unit, String name) {
        Netlist.PinBinding binding = unit.pins().get(name);
        if (binding != null) {
            return binding.isDigitalNode() ? Netlist.GROUND : binding.analog();
        }
        return unit.internalNodes().get(name);
    }

    /** The slot a name like {@code registers_12} reads of a row state, or {@link #NONE} when it names no slot. */
    static int slotOf(String name, Map<String, Object> discrete) {
        int underscore = name.lastIndexOf('_');
        if (underscore < 1 || underscore == name.length() - 1) {
            return NONE;
        }
        String index = name.substring(underscore + 1);
        if (!index.chars().allMatch(Character::isDigit) || !(discrete.get(name.substring(0, underscore)) instanceof Object[] row)) {
            return NONE;
        }
        int slot = Integer.parseInt(index);
        return slot < row.length ? slot : NONE;
    }
}
