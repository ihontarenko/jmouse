package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ComponentVocabulary;
import org.jmouse.parallax.component.ModelLanguage;
import org.jmouse.parallax.component.PinQuantities;

import org.jmouse.formula.Differentiation;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.GradientCompiler;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.Symbols;
import org.jmouse.parallax.component.ComponentModel.EquationKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.function.IntPredicate;

/**
 * A model's {@code equations} compiled for one unit: every line a jMF formula over the unit's pin voltages, its
 * states and its parameters, with the exact partial derivative by each unknown it reads — the Jacobian Newton needs.
 *
 * <p>A pin's or an internal node's name <em>is</em> its voltage: {@code (anode - j) / R_series}. The names are bound on
 * the tree, inside the bodies of the locals too ({@link Substitution#bindHostNames}), each to a {@link Formula.Slot} at
 * its node's position; a parameter, a local or a state may not take a pin's name, so a name always means one thing.
 * {@code d(q)/dt} is the rate of a charge {@code q}: the slot of a rate the device computes from {@code q} and its
 * history, so a capacitance written as a charge is integrated by the same rule as every other capacitor in the
 * circuit. It reaches jMF as the host call {@code ddt(q)} — how this solver marks it, never what an author writes.</p>
 *
 * <p>{@code current(pin)} is the current into a pin, as a {@code measure} reads it: the branch current of every
 * {@code voltage} equation on that pin — an unknown of its own, so an ammeter's {@code voltage in -> out = R *
 * current(in)} is a shunt —, and the right side of every {@code current} equation on it, written out. A {@code current}
 * equation written out so may not read {@code current(…)} itself.</p>
 */
final class ModelEquations {

    /** What one position of the argument array holds; the parameters follow the last of them. */
    sealed interface Argument {

        /** A pin's or an internal node's voltage, bound from {@code V(…)}. */
        record NodeVoltage(String node) implements Argument {
        }

        /** The rate of the charge at {@code charge}, bound from {@code ddt(…)}. */
        record ChargeRate(int charge) implements Argument {
        }

        /** A state the model declares, by its own name. */
        record StateValue(String state) implements Argument {
        }

        /** The simulated time. */
        record Time() implements Argument {
        }

        /** The branch current of the {@code branch}-th voltage equation, bound from {@code current(pin)}. */
        record BranchCurrent(int branch) implements Argument {
        }

        /** The integration's coefficient: a linearized equation's rate is coefficient · q − history; 0 at rest. */
        record RateCoefficient() implements Argument {
        }

        /** What the rate of the charge at {@code charge} carries from the steps before: coefficient · q_previous + …. */
        record ChargeHistory(int charge) implements Argument {
        }
    }

    /**
     * One compiled formula and its partial derivatives by the unknowns it reads, by argument position. A formula a
     * tape can carry is evaluated as one (value and every partial in two passes, see {@link GradientCompiler}); the
     * symbolic partials remain only for the rest. Holds scratch space: one per unit, one thread at a time.
     */
    record Compiled(RealFunction value, int[] partialSymbols, RealFunction[] partials, GradientCompiler.GradientFunction tape,
                    double[] derivatives) {

        /** The same formula with scratch space of its own. */
        Compiled copy() {
            return new Compiled(value, partialSymbols, partials, tape == null ? null : tape.copy(), new double[derivatives.length]);
        }

        double evaluate(double[] arguments) {
            return tape != null ? tape.evaluate(arguments, null) : value.evaluate(arguments);
        }

        /** The value, with every partial written into {@link #derivatives()} in the order of {@link #partialSymbols()}. */
        double evaluateWithPartials(double[] arguments) {
            if (tape != null) {
                return tape.evaluate(arguments, derivatives);
            }
            for (int partial = 0; partial < partials.length; partial++) {
                derivatives[partial] = partials[partial].evaluate(arguments);
            }
            return value.evaluate(arguments);
        }
    }

    /**
     * One equation: what it contributes, between which pins (or for which state), and its formula. {@code linearized}
     * is the same equation with every charge's rate written out as coefficient · q − history, so a step's tangent comes
     * from one tape — a diode's diffusion exponential, read by its current and by its charge, computed once. It is the
     * formula itself where the equation reads no charge, and null where the written-out tape would not become code.
     */
    record Equation(EquationKind kind, String positive, String negative, String state, Compiled formula, Compiled linearized) {

        Equation copy() {
            Compiled copied = formula.copy();
            return new Equation(kind, positive, negative, state, copied,
                    linearized == null ? null : linearized == formula ? copied : linearized.copy());
        }
    }

    /**
     * What decides a model's compiled equations — everything but the parameter values, which are arguments. The
     * {@code name} is only in the messages, and is part of it so that they keep naming the model they are about.
     */
    private record Form(String name, List<String> formulas, Map<String, String> locals, List<ComponentModel.Equation> equations,
                        List<String> states, List<String> parameterNames, List<String> pinNames, List<String> internalNodes) {
    }

    /** How many forms are kept: a catalogue's worth of models, and the drafts of the one being written. */
    private static final int KEPT_FORMS = 256;

    /**
     * The equations compiled so far, by form. Parsing and compiling a model is most of what building a circuit costs,
     * and every slider move builds it again — a voltage multiplier's twenty-four diodes compiled one diode
     * twenty-four times on each move.
     */
    private static final Map<Form, ModelEquations> COMPILED = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Form, ModelEquations> eldest) {
                    return size() > KEPT_FORMS;
                }
            });

    private final ComponentModel model;
    private final List<Argument> layout = new ArrayList<>();
    private final List<String>   nodes;
    /** The pins among {@link #nodes}: what a pin's current may be asked of. */
    private final List<String>   pins;
    private final List<String>   parameterNames;
    private final double[]       parameterValues;
    private final List<Equation> equations = new ArrayList<>();
    private final List<Compiled> charges = new ArrayList<>();
    private final List<Noise>    noises  = new ArrayList<>();
    /** How many voltage equations there are: each has a branch current, laid out after the node voltages. */
    private final int branchCount;
    /**
     * Whether every equation and every charge is linear in the unit's unknowns — its node voltages, branch currents,
     * charge rates and continuous states: no partial by one of them reads one of them. Proved once from the formulas,
     * so a model written as a capacitance behind a resistance is solved as the linear part it is, in one solve a step,
     * however its values move with the time, the temperature or its discrete states.
     */
    private final boolean linear;
    /**
     * Whether any equation or charge reads the time — a generator's signal, a timer's ramp. The time always has its
     * place among the arguments; this says whether anything is read from it.
     */
    private final boolean readsTime;

    /**
     * The model's equations for one unit with these parameter values: compiled once per form, and copied for every
     * unit after the first, each copy with its own parameter values and scratch space.
     */
    static ModelEquations of(ComponentModel model, Map<String, Double> parameters, List<String> pinNames, List<String> internalNodes) {
        Form form = new Form(model.name(), model.formulas(), model.locals(), model.equations(),
                model.states().stream().map(ComponentModel.State::name).toList(), List.copyOf(parameters.keySet()),
                List.copyOf(pinNames), List.copyOf(internalNodes));
        ModelEquations compiled = COMPILED.get(form);
        if (compiled == null) {
            compiled = new ModelEquations(model, parameters, pinNames, internalNodes);
            COMPILED.put(form, compiled);
        }
        return new ModelEquations(compiled, parameters);
    }

    /** A copy of {@code compiled} for other parameter values: the same layout and formulas, its own scratch space. */
    private ModelEquations(ModelEquations compiled, Map<String, Double> parameters) {
        this.model = compiled.model;
        this.nodes = compiled.nodes;
        this.pins = compiled.pins;
        this.parameterNames = compiled.parameterNames;
        this.parameterValues = parameters.values().stream().mapToDouble(Double::doubleValue).toArray();
        this.branchCount = compiled.branchCount;
        this.linear = compiled.linear;
        this.readsTime = compiled.readsTime;
        this.layout.addAll(compiled.layout);
        compiled.equations.forEach(equation -> equations.add(equation.copy()));
        compiled.charges.forEach(charge -> charges.add(charge.copy()));
        compiled.noises.forEach(noise -> noises.add(noise.copy()));
    }

    private ModelEquations(ComponentModel model, Map<String, Double> parameters, List<String> pinNames, List<String> internalNodes) {
        this.model = model;
        this.parameterNames = List.copyOf(parameters.keySet());
        this.parameterValues = parameters.values().stream().mapToDouble(Double::doubleValue).toArray();
        List<String> allNodes = new ArrayList<>(pinNames);
        allNodes.addAll(internalNodes);
        this.nodes = List.copyOf(allNodes);
        this.pins = List.copyOf(pinNames);

        requireDistinctFromNodes(parameterNames, "parameter");
        requireDistinctFromNodes(List.copyOf(model.locals().keySet()), "let");
        requireDistinctFromNodes(model.states().stream().map(ComponentModel.State::name).toList(), "state");
        List<Formula.Definition> library = new ArrayList<>(ModelLanguage.LANGUAGE.definitions(ModelLanguage.libraryText(model)));
        library.addAll(locals(library));

        this.branchCount = (int) model.equations().stream().filter(equation -> equation.kind() == EquationKind.VOLTAGE).count();
        // Bind every equation first: the charges it finds decide how many slots the table reserves.
        List<Formula> chargeFormulas = new ArrayList<>();
        List<Formula> parsedEquations = new ArrayList<>();
        Map<Integer, List<List<Formula>>> noiseDensities = new LinkedHashMap<>();
        List<Formula> rightSides = new ArrayList<>();
        List<Boolean> readsCurrents = new ArrayList<>();
        for (ComponentModel.Equation equation : model.equations()) {
            for (String pin : new String[] {equation.positive(), equation.negative()}) {
                if (pin != null && !nodes.contains(pin)) {
                    throw new IllegalArgumentException(model.name() + ", line " + equation.line() + ": '" + pin
                            + "' is not a pin or a node of the model.");
                }
            }
            Formula parsed = PinQuantities.bind(ModelLanguage.read(model.name(), equation.formula(), equation.line(), library), pinNames,
                    written -> model.name() + ", line " + equation.line() + ": '" + written + "'");
            readsCurrents.add(PinQuantities.readsCurrent(parsed));
            int index = parsedEquations.size();
            parsedEquations.add(parsed);
            Formula rates = Substitution.replaceHostCalls(parsed, call -> switch (call.name()) {
                case ModelLanguage.NOISE -> noise(call, equation, noiseDensities.computeIfAbsent(index, ignored -> new ArrayList<>()));
                case ModelLanguage.RATE -> rate(call, chargeFormulas, equation.line());
                default -> call;
            });
            rightSides.add(Substitution.bindHostNames(rates, this::nodeVoltage));
        }
        bindPinCurrents(rightSides, readsCurrents);

        // The same equations with every rate written out over the charge itself: coefficient · q − history.
        int coefficientSlot = nodes.size() + branchCount + chargeFormulas.size();
        List<Formula> writtenOut = new ArrayList<>();
        for (Formula parsed : parsedEquations) {
            Formula rates = Substitution.replaceHostCalls(parsed, call -> switch (call.name()) {
                case ModelLanguage.NOISE -> new Formula.Constant(0);
                case ModelLanguage.RATE -> writtenRate(call, chargeFormulas, coefficientSlot);
                default -> call;
            });
            writtenOut.add(Substitution.bindHostNames(rates, this::nodeVoltage));
        }
        bindPinCurrents(writtenOut, readsCurrents);

        List<String> continuousStates = model.states().stream().filter(state -> !state.discrete())
                .map(ComponentModel.State::name).toList();
        this.linear = linearIn(nodes.size() + branchCount + chargeFormulas.size(), continuousStates, rightSides, chargeFormulas);
        this.readsTime = Stream.concat(rightSides.stream(), chargeFormulas.stream()).anyMatch(formula -> formula.dependsOn(ComponentVocabulary.TIME));

        nodes.forEach(node -> layout.add(new Argument.NodeVoltage(node)));
        for (int branch = 0; branch < branchCount; branch++) {
            layout.add(new Argument.BranchCurrent(branch));
        }
        for (int charge = 0; charge < chargeFormulas.size(); charge++) {
            layout.add(new Argument.ChargeRate(charge));
        }
        if (!chargeFormulas.isEmpty()) {
            layout.add(new Argument.RateCoefficient());
            for (int charge = 0; charge < chargeFormulas.size(); charge++) {
                layout.add(new Argument.ChargeHistory(charge));
            }
        }
        int reserved = layout.size();
        List<String> names = new ArrayList<>();
        model.states().forEach(state -> {
            layout.add(new Argument.StateValue(state.name()));
            names.add(state.name());
        });
        layout.add(new Argument.Time());
        names.add(ComponentVocabulary.TIME);
        Symbols table = Symbols.withSlots(reserved, names, parameterNames);

        for (Formula charge : chargeFormulas) {
            charges.add(compile(charge, table, slot -> true));
        }
        noiseDensities.forEach((equation, densities) -> densities.forEach(density ->
                noises.add(new Noise(equation, compile(density.get(0), table, slot -> false),
                        compile(density.get(1), table, slot -> false)))));
        int firstRate = nodes.size() + branchCount;
        int afterHistories = coefficientSlot + 1 + chargeFormulas.size();
        for (int index = 0; index < model.equations().size(); index++) {
            ComponentModel.Equation equation = model.equations().get(index);
            Formula rightSide = rightSides.get(index);
            Compiled formula = compile(rightSide, table, slot -> true);
            boolean readsRate = rightSide.slots().stream().anyMatch(slot -> slot >= firstRate && slot < coefficientSlot);
            Compiled linearized = formula;
            if (readsRate) {
                // The coefficient and the histories are given, not solved for: no partial by them.
                linearized = compile(writtenOut.get(index), table, slot -> slot < coefficientSlot || slot >= afterHistories);
                if (linearized.tape() == null || !linearized.tape().becomesCode()) {
                    linearized = null;
                }
            }
            equations.add(new Equation(equation.kind(), equation.positive(), equation.negative(),
                    equation.state(), formula, linearized));
        }
    }

    /**
     * Whether every formula is linear in the unknowns — the slots below {@code unknownSlots} and the continuous states:
     * each partial by one of them reads none of them. A formula the differentiation cannot take apart is not proved
     * linear, and the model stays nonlinear: solved by Newton, as before.
     */
    private static boolean linearIn(int unknownSlots, List<String> states, List<Formula> rightSides, List<Formula> charges) {
        List<Formula> formulas = new ArrayList<>(rightSides);
        formulas.addAll(charges);
        try {
            for (Formula formula : formulas) {
                for (int slot : formula.slots()) {
                    if (slot < unknownSlots && readsUnknown(Differentiation.derivative(formula, new Formula.Slot(slot, "slot " + slot)),
                            unknownSlots, states)) {
                        return false;
                    }
                }
                for (String state : states) {
                    if (formula.dependsOn(state) && readsUnknown(Differentiation.derivative(formula, state), unknownSlots, states)) {
                        return false;
                    }
                }
            }
        } catch (RuntimeException notDifferentiable) {
            return false;
        }
        return true;
    }

    private static boolean readsUnknown(Formula partial, int unknownSlots, List<String> states) {
        return partial.slots().stream().anyMatch(slot -> slot < unknownSlots) || states.stream().anyMatch(partial::dependsOn);
    }

    /** Whether every equation and charge is linear in the unit's unknowns: solved in one step, no Newton. */
    boolean linear() {
        return linear;
    }

    /** Whether any of its equations or charges reads the time. */
    boolean readsTime() {
        return readsTime;
    }

    /** Where a parameter's value stands in {@link #arguments()}; -1 for a parameter the unit does not have. */
    int parameterPosition(String name) {
        int index = parameterNames.indexOf(name);
        return index < 0 ? -1 : layout.size() + index;
    }

    /** Every {@code current(pin)} in the right sides, written out of the other equations. */
    private void bindPinCurrents(List<Formula> rightSides, List<Boolean> readsCurrents) {
        List<Formula> unbound = List.copyOf(rightSides);
        for (int index = 0; index < rightSides.size(); index++) {
            int line = model.equations().get(index).line();
            rightSides.set(index, PinQuantities.bindCurrents(unbound.get(index), pins, pin -> currentInto(pin, unbound, readsCurrents, line)));
        }
    }

    /** A charge's rate written out: coefficient · q − history, both read from slots the device fills each step. */
    private Formula writtenRate(Formula.HostCall call, List<Formula> chargeFormulas, int coefficientSlot) {
        Formula charge = chargeOf(call.arguments().getFirst(), 0);
        int index = chargeFormulas.indexOf(charge);
        Formula scaled = new Formula.Binary(Formula.Operator.MULTIPLY, new Formula.Slot(coefficientSlot, "rate coefficient"), charge);
        return new Formula.Binary(Formula.Operator.SUBTRACT, scaled,
                new Formula.Slot(coefficientSlot + 1 + index, "history of q" + (index + 1)));
    }

    /** What each position of the argument array holds, before the parameters. */
    List<Argument> layout() {
        return layout;
    }

    List<Equation> equations() {
        return equations;
    }

    List<Compiled> charges() {
        return charges;
    }

    /** The noise currents the equations add, each with its equation and its density. */
    List<Noise> noises() {
        return noises;
    }

    /**
     * A {@code noise(density[, corner])} term: in which equation, its white density and its flicker corner — the
     * frequency below which 1/f noise rises over the white —, each a formula of the unit's arguments.
     */
    record Noise(int equation, Compiled density, Compiled corner) {

        Noise copy() {
            return new Noise(equation, density.copy(), corner.copy());
        }
    }

    /** An argument array with the parameter values already after the positions of {@link #layout()}. */
    double[] arguments() {
        double[] arguments = new double[layout.size() + parameterValues.length];
        System.arraycopy(parameterValues, 0, arguments, layout.size(), parameterValues.length);
        return arguments;
    }

    /** The model's {@code local} quantities as definitions, each able to read the library and the locals before it. */
    private List<Formula.Definition> locals(List<Formula.Definition> library) {
        if (model.locals().isEmpty()) {
            return List.of();
        }
        StringBuilder text = new StringBuilder();
        model.locals().forEach((name, formula) -> text.append(name).append(" = ").append(formula).append('\n'));
        List<Formula.Definition> definitions;
        try {
            definitions = ModelLanguage.LANGUAGE.definitions(text.toString(), library).stream()
                    .map(definition -> new Formula.Definition(definition.name(), definition.parameters(),
                            ModelLanguage.refuseBorrowed(model.name() + ": ", definition.body())))
                    .toList();
        } catch (FormulaException failure) {
            throw new IllegalArgumentException(model.name() + ": " + failure.getMessage(), failure);
        }
        List<Formula.Definition> bound = new ArrayList<>();
        for (Formula.Definition definition : definitions) {
            // A pin's current and voltage are read in a let as in an equation; only a rate belongs where it adds a current.
            Formula members = PinQuantities.bind(definition.body(), pins,
                    written -> model.name() + ", let " + definition.name() + ": '" + written + "'");
            Formula rates = Substitution.replaceHostCalls(members, call -> {
                if (PinQuantities.isCurrent(call)) {
                    return call;
                }
                throw new IllegalArgumentException(model.name() + ": write " + (ModelLanguage.NOISE.equals(call.name()) ? "noise(…)" : "d(q)/dt")
                        + " in the current it adds to, not in 'let " + definition.name() + "'.");
            });
            Formula body = Substitution.bindHostNames(rates, this::nodeVoltage);
            bound.add(new Formula.Definition(definition.name(), definition.parameters(), body));
        }
        return rebind(bound);
    }

    /**
     * Locals call one another; once each body is bound, a later local must call the bound version of an earlier one,
     * not the version the parser linked it to. Applications are relinked by name, in order.
     */
    private static List<Formula.Definition> rebind(List<Formula.Definition> bound) {
        Map<String, Formula.Definition> byName = new LinkedHashMap<>();
        List<Formula.Definition> result = new ArrayList<>();
        for (Formula.Definition definition : bound) {
            Formula body = relink(definition.body(), byName);
            Formula.Definition linked = new Formula.Definition(definition.name(), definition.parameters(), body);
            byName.put(linked.name(), linked);
            result.add(linked);
        }
        return result;
    }

    private static Formula relink(Formula expression, Map<String, Formula.Definition> byName) {
        return switch (expression) {
            case Formula.Application application -> new Formula.Application(
                    byName.getOrDefault(application.definition().name(), application.definition()),
                    application.arguments().stream().map(argument -> relink(argument, byName)).toList());
            case Formula.Negation negation -> new Formula.Negation(relink(negation.operand(), byName));
            case Formula.Not not -> new Formula.Not(relink(not.operand(), byName));
            case Formula.Binary binary -> new Formula.Binary(binary.operator(), relink(binary.left(), byName),
                    relink(binary.right(), byName));
            case Formula.Comparison comparison -> new Formula.Comparison(comparison.relation(),
                    relink(comparison.left(), byName), relink(comparison.right(), byName));
            case Formula.Logical logical -> new Formula.Logical(logical.connective(), relink(logical.left(), byName),
                    relink(logical.right(), byName));
            case Formula.Equation equation -> new Formula.Equation(relink(equation.left(), byName),
                    relink(equation.right(), byName));
            case Formula.Call call -> new Formula.Call(call.function(),
                    call.arguments().stream().map(argument -> relink(argument, byName)).toList());
            case Formula.HostCall hostCall -> new Formula.HostCall(hostCall.name(),
                    hostCall.arguments().stream().map(argument -> relink(argument, byName)).toList());
            case Formula.Piecewise piecewise -> new Formula.Piecewise(piecewise.branches().stream()
                    .map(branch -> new Formula.Piecewise.Branch(relink(branch.condition(), byName), relink(branch.value(), byName)))
                    .toList(), piecewise.otherwise() == null ? null : relink(piecewise.otherwise(), byName));
            case Formula.Accumulation accumulation -> new Formula.Accumulation(accumulation.accumulator(),
                    accumulation.index(), relink(accumulation.from(), byName), relink(accumulation.to(), byName),
                    relink(accumulation.body(), byName));
            case Formula.Integral integral -> new Formula.Integral(integral.variable(), relink(integral.lower(), byName),
                    relink(integral.upper(), byName), relink(integral.body(), byName));
            case Formula.Variable variable -> variable;
            case Formula.Slot slot -> slot;
            case Formula.Constant constant -> constant;
            case Formula.NamedConstant named -> named;
            case Formula.ImaginaryUnit unit -> unit;
        };
    }

    /** {@code d(q)/dt} — reaching here as {@code ddt(q)} — as the slot of a new charge's rate. */
    private Formula rate(Formula.HostCall call, List<Formula> chargeFormulas, int line) {
        if (call.arguments().size() != 1) {
            throw new IllegalArgumentException(model.name() + ", line " + line + ": a rate is the rate of one charge: d(q)/dt.");
        }
        Formula charge = call.arguments().getFirst();
        if (holdsRate(charge)) {
            throw new IllegalArgumentException(model.name() + ", line " + line + ": a charge cannot hold another d(…)/dt.");
        }
        // The charge is compiled on its own, so its names are bound here, not by the right side it sits in.
        chargeFormulas.add(chargeOf(charge, line));
        // The rates follow the node voltages and the branch currents; the table is laid out once every charge is known.
        return new Formula.Slot(nodes.size() + branchCount + chargeFormulas.size() - 1, "d(q" + chargeFormulas.size() + ")/dt");
    }

    /**
     * A {@code noise(density)} term: kept for the noise analysis, nothing to the solution. A noise current runs along a
     * current equation, so it is refused anywhere else.
     */
    private Formula noise(Formula.HostCall call, ComponentModel.Equation equation, List<List<Formula>> densities) {
        if (call.arguments().isEmpty() || call.arguments().size() > 2) {
            throw new IllegalArgumentException(model.name() + ", line " + equation.line()
                    + ": noise(…) takes a density in A²/Hz and, if it has 1/f noise, its corner frequency in Hz.");
        }
        if (equation.kind() != EquationKind.CURRENT) {
            throw new IllegalArgumentException(model.name() + ", line " + equation.line()
                    + ": noise(…) is a current; write it in a current equation.");
        }
        Formula corner = call.arguments().size() == 2 ? call.arguments().get(1) : new Formula.Constant(0);
        densities.add(List.of(Substitution.bindHostNames(call.arguments().getFirst(), this::nodeVoltage),
                Substitution.bindHostNames(corner, this::nodeVoltage)));
        return new Formula.Constant(0);
    }

    /**
     * The current into {@code pin}: each voltage equation's branch current where its branch starts on the pin, less
     * where it ends there, and each current equation's right side alike — what {@code current a -> b} carries enters at a.
     */
    private Formula currentInto(String pin, List<Formula> rightSides, List<Boolean> readsCurrents, int line) {
        Formula sum = new Formula.Constant(0);
        int branch = 0;
        for (int index = 0; index < model.equations().size(); index++) {
            ComponentModel.Equation equation = model.equations().get(index);
            EquationKind kind = equation.kind();
            Formula through = switch (kind) {
                case VOLTAGE -> new Formula.Slot(nodes.size() + branch, "current of branch " + branch++);
                case CURRENT -> rightSides.get(index);
                case STATE -> null;
            };
            if (through == null || !pin.equals(equation.positive()) && !pin.equals(equation.negative())) {
                continue;
            }
            if (kind == EquationKind.CURRENT && readsCurrents.get(index)) {
                throw new IllegalArgumentException(model.name() + ", line " + line + ": current(" + pin + ") needs line "
                        + equation.line() + ", which reads a pin's current itself; write that line without current(…).");
            }
            if (pin.equals(equation.positive())) {
                sum = new Formula.Binary(Formula.Operator.ADD, sum, through);
            }
            if (pin.equals(equation.negative())) {
                sum = new Formula.Binary(Formula.Operator.SUBTRACT, sum, through);
            }
        }
        return sum;
    }

    /**
     * A charge with its names bound: a pin's name to its voltage, a pin's current to the branch currents of the
     * {@code voltage} equations on it — so a winding writes its flux as what it is, {@code d(L * terminal_one.current)/dt},
     * and needs no node carrying its current as a voltage. A current a {@code current} equation carries is refused: it
     * is a function of the very rates the charge decides.
     */
    private Formula chargeOf(Formula written, int line) {
        return PinQuantities.bindCurrents(Substitution.bindHostNames(written, this::nodeVoltage), pins, pin -> branchCurrentInto(pin, line));
    }

    /** Whether a charge holds a rate of its own, inside the definitions it calls too. */
    private static boolean holdsRate(Formula charge) {
        boolean[] holds = {false};
        Substitution.replaceHostCalls(charge, call -> {
            holds[0] |= ModelLanguage.RATE.equals(call.name());
            return call;
        });
        return holds[0];
    }

    /** The current into a pin through the {@code voltage} equations on it: the branch currents, signed; refused for any other. */
    private Formula branchCurrentInto(String pin, int line) {
        Formula sum = new Formula.Constant(0);
        int branch = 0;
        for (ComponentModel.Equation equation : model.equations()) {
            EquationKind kind = equation.kind();
            boolean touches = pin.equals(equation.positive()) || pin.equals(equation.negative());
            if (kind == EquationKind.CURRENT && touches) {
                throw new IllegalArgumentException(model.name() + ", line " + line + ": a charge reads " + pin
                        + ".current only where a voltage equation carries it; line " + equation.line()
                        + " carries it as a current, which the charge's own rate decides.");
            }
            if (kind != EquationKind.VOLTAGE) {
                continue;
            }
            Formula through = new Formula.Slot(nodes.size() + branch, "current of branch " + branch);
            branch++;
            if (pin.equals(equation.positive())) {
                sum = new Formula.Binary(Formula.Operator.ADD, sum, through);
            }
            if (pin.equals(equation.negative())) {
                sum = new Formula.Binary(Formula.Operator.SUBTRACT, sum, through);
            }
        }
        return sum;
    }

    /** A pin's or an internal node's name read as its voltage: the slot at its position; null for any other name. */
    private Formula nodeVoltage(String name) {
        int index = nodes.indexOf(name);
        return index < 0 ? null : new Formula.Slot(index, name);
    }

    private void requireDistinctFromNodes(List<String> names, String what) {
        for (String name : names) {
            if (nodes.contains(name)) {
                throw new IllegalArgumentException(model.name() + ": '" + name + "' is both a pin and a " + what
                        + " — a pin's name reads its voltage, so it cannot name anything else.");
            }
        }
    }

    /** The formula and its exact partial derivatives by every slot {@code differentiated} admits and every state it reads. */
    private Compiled compile(Formula formula, Symbols table, IntPredicate differentiated) {
        String source = formula.toString();
        RealFunction value = RealCompiler.compile(formula, table, source);
        List<Integer> slots = formula.slots().stream().filter(differentiated::test).toList();
        List<Integer> positions = new ArrayList<>(slots);
        for (ComponentModel.State state : model.states()) {
            if (formula.dependsOn(state.name())) {
                positions.add(table.slotOf(state.name()));
            }
        }
        int[] symbols = positions.stream().mapToInt(Integer::intValue).toArray();
        GradientCompiler.GradientFunction tape = GradientCompiler.compile(formula, table, symbols, layout.size());
        List<RealFunction> partials = new ArrayList<>();
        if (tape == null) {
            for (int slot : slots) {
                partials.add(RealCompiler.compile(Differentiation.derivative(formula, new Formula.Slot(slot, "slot " + slot)), table, source));
            }
            for (ComponentModel.State state : model.states()) {
                if (formula.dependsOn(state.name())) {
                    partials.add(RealCompiler.compile(Differentiation.derivative(formula, state.name()), table, source));
                }
            }
        }
        return new Compiled(value, symbols, partials.toArray(RealFunction[]::new), tape, new double[symbols.length]);
    }


}
