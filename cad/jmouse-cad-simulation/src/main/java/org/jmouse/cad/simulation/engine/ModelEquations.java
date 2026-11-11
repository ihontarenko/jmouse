package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.component.Equation;
import org.jmouse.cad.component.State;
import org.jmouse.cad.simulation.EngineName;
import org.jmouse.cad.simulation.ModelLanguage;
import org.jmouse.cad.simulation.PinQuantities;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.formula.Differentiation;
import org.jmouse.formula.Formula;
import org.jmouse.formula.GradientCompiler;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.Symbols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

/**
 * A component's equations compiled for one unit: every line a jMF formula over the unit's pin voltages, its states and its
 * parameters, with the exact partial derivative by each unknown it reads — the Jacobian Newton needs.
 *
 * <p>A pin's or an internal node's name <em>is</em> its voltage: {@code (anode - j) / R_series}. The names are bound on
 * the tree, inside the bodies of the locals too ({@link Substitution#bindHostNames}), each to a {@link Formula.Slot} at its
 * node's position; a parameter, a local or a state may not take a pin's name, so a name always means one thing.
 * {@code d(q)/dt} is the rate of a charge {@code q}: the slot of a rate the device computes from {@code q} and its
 * history, so a capacitance written as a charge is integrated by the same rule as every other capacitor.</p>
 *
 * <p>{@code current(pin)} is the current into a pin: the branch current of every {@code voltage} equation on that pin —
 * an unknown of its own —, and the right side of every {@code current} equation on it, written out.</p>
 *
 * <p>What it is read from is jmCAD's model, and no text is ever put together for jMF to read.</p>
 */
public final class ModelEquations {

    /** What a line of a component's equations is, for the solver. */
    public enum Kind {
        /** {@code current a -> b = …}: a current through the part. */
        CURRENT,
        /** {@code voltage a -> b = …}: a branch holding one pin at a voltage above another, its current an unknown. */
        VOLTAGE,
        /** {@code d(x)/dt = …}: how fast a continuous state changes. */
        STATE
    }

    /** What one position of the argument array holds; the parameters follow the last of them. */
    public sealed interface Argument {

        /** A pin's or an internal node's voltage. */
        record NodeVoltage(String node) implements Argument {
        }

        /** The rate of the charge at {@code charge}. */
        record ChargeRate(int charge) implements Argument {
        }

        /** A state the component declares, by its own name. */
        record StateValue(String state) implements Argument {
        }

        /** The simulated time. */
        record Time() implements Argument {
        }

        /** The branch current of the {@code branch}-th voltage equation. */
        record BranchCurrent(int branch) implements Argument {
        }

        /** The integration's coefficient: a linearized equation's rate is coefficient · q − history; 0 at rest. */
        record RateCoefficient() implements Argument {
        }

        /** What the rate of the charge at {@code charge} carries from the steps before. */
        record ChargeHistory(int charge) implements Argument {
        }
    }

    /**
     * One compiled formula and its partial derivatives by the unknowns it reads, by argument position. A formula a tape
     * can carry is evaluated as one; the symbolic partials remain only for the rest. Holds scratch space: one per unit,
     * one thread at a time.
     */
    public record Compiled(RealFunction value, int[] partialSymbols, RealFunction[] partials,
                           GradientCompiler.GradientFunction tape, double[] derivatives) {

        /** The same formula with scratch space of its own. */
        Compiled copy() {
            return new Compiled(value, partialSymbols, partials, tape == null ? null : tape.copy(), new double[derivatives.length]);
        }

        public double evaluate(double[] arguments) {
            return tape != null ? tape.evaluate(arguments, null) : value.evaluate(arguments);
        }

        /** The value, with every partial written into {@link #derivatives()} in the order of {@link #partialSymbols()}. */
        public double evaluateWithPartials(double[] arguments) {
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
     * One equation: what it contributes, between which pins or for which state, and its formula. {@code linearized} is
     * the same equation with every charge's rate written out as coefficient · q − history; the formula itself where it
     * reads no charge, and null where the written-out tape would not become code.
     */
    public record CompiledEquation(Kind kind, String positive, String negative, String state, Compiled formula,
                                   Compiled linearized) {

        CompiledEquation copy() {
            Compiled copied = formula.copy();
            return new CompiledEquation(kind, positive, negative, state, copied,
                    linearized == null ? null : linearized == formula ? copied : linearized.copy());
        }
    }

    /**
     * A noise current an equation adds: in which equation, its white density and its flicker corner, each a formula of the
     * unit's arguments.
     */
    public record Noise(int equation, Compiled density, Compiled corner) {

        Noise copy() {
            return new Noise(equation, density.copy(), corner.copy());
        }
    }

    /** A line of the component's equations as the solver reads it: its kind, its pins or its state, its formula. */
    private record Line(Kind kind, String positive, String negative, String state, String formula) {

        static Line of(Equation equation) {
            return switch (equation) {
                case Equation.Branch branch -> new Line(branch.kind().equals("voltage") ? Kind.VOLTAGE : Kind.CURRENT,
                        branch.from(), branch.to(), null, branch.value().written());
                case Equation.Derivative derivative -> new Line(Kind.STATE, null, null, derivative.state(),
                        derivative.rate().written());
                default -> throw new IllegalArgumentException("'" + equation + "' is no equation the solver stamps");
            };
        }

        String where(String component) {
            return component + ", '" + (kind == Kind.STATE ? "d(" + state + ")/dt" : kind.name().toLowerCase() + " " + positive
                    + " -> " + negative) + " = " + formula + "'";
        }
    }

    /** What decides a unit's compiled equations — everything but the parameter values, which are arguments. */
    private record Form(String component, List<Line> lines, List<Equation> formulas, List<Equation.Local> locals,
                        List<String> states, List<String> parameterNames, List<String> pinNames, List<String> internalNodes) {
    }

    /** How many forms are kept: a catalogue's worth of components, and the drafts of the one being written. */
    private static final int KEPT_FORMS = 256;

    /** The equations compiled so far, by form: compiling is most of what building a circuit costs. */
    private static final Map<Form, ModelEquations> COMPILED = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Form, ModelEquations> eldest) {
                    return size() > KEPT_FORMS;
                }
            });

    private final String                 title;
    private final List<Line>             lines;
    private final List<State>            states;
    private final List<Argument>         layout = new ArrayList<>();
    private final List<String>           nodes;
    private final List<String>           pins;
    private final List<String>           parameterNames;
    private final double[]               parameterValues;
    private final List<CompiledEquation> equations = new ArrayList<>();
    private final List<Compiled>         charges = new ArrayList<>();
    private final List<Noise>            noises = new ArrayList<>();
    private final int                    branchCount;
    /** Whether every equation and charge is linear in the unit's unknowns: solved in one step, no Newton. */
    private final boolean                linear;
    /** Whether any equation or charge reads the time. */
    private final boolean                readsTime;

    /**
     * The component's equations for one unit with these parameter values: compiled once per form, and copied for every
     * unit after the first, each copy with its own parameter values and scratch space.
     */
    public static ModelEquations of(SimulatedComponent component, Map<String, Double> parameters, List<String> pinNames,
                                    List<String> internalNodes) {
        List<Line> lines = component.equations().stream().map(Line::of).toList();
        List<String> states = equationStateNames(component, lines);
        Form form = new Form(component.title(), lines, component.formulas(), component.locals(), states,
                List.copyOf(parameters.keySet()), List.copyOf(pinNames), List.copyOf(internalNodes));
        ModelEquations compiled = COMPILED.get(form);
        if (compiled == null) {
            compiled = new ModelEquations(component, lines, states, parameters, pinNames, internalNodes);
            COMPILED.put(form, compiled);
        }
        return new ModelEquations(compiled, parameters);
    }

    private ModelEquations(ModelEquations compiled, Map<String, Double> parameters) {
        this.title = compiled.title;
        this.lines = compiled.lines;
        this.states = compiled.states;
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

    private ModelEquations(SimulatedComponent component, List<Line> lines, List<String> stateNames, Map<String, Double> parameters,
                           List<String> pinNames, List<String> internalNodes) {
        this.title = component.title();
        this.lines = lines;
        this.states = component.states();
        this.parameterNames = List.copyOf(parameters.keySet());
        this.parameterValues = parameters.values().stream().mapToDouble(Double::doubleValue).toArray();
        List<String> allNodes = new ArrayList<>(pinNames);
        allNodes.addAll(internalNodes);
        this.nodes = List.copyOf(allNodes);
        this.pins = List.copyOf(pinNames);

        requireDistinctFromNodes(parameterNames, "parameter");
        requireDistinctFromNodes(component.locals().stream().map(Equation.Local::name).toList(), "let");
        requireDistinctFromNodes(states.stream().map(State::name).toList(), "state");
        List<Formula.Definition> library = new ArrayList<>(ModelLanguage.definitions(title, component.formulas()));
        library.addAll(locals(component.locals(), library));

        this.branchCount = (int) lines.stream().filter(line -> line.kind() == Kind.VOLTAGE).count();
        List<Formula> chargeFormulas = new ArrayList<>();
        List<Formula> parsedEquations = new ArrayList<>();
        Map<Integer, List<List<Formula>>> noiseDensities = new LinkedHashMap<>();
        List<Formula> rightSides = new ArrayList<>();
        List<Boolean> readsCurrents = new ArrayList<>();
        for (Line line : lines) {
            for (String pin : new String[] {line.positive(), line.negative()}) {
                if (pin != null && !nodes.contains(pin)) {
                    throw new IllegalArgumentException(line.where(title) + ": '" + pin + "' is not a pin or a node of the component.");
                }
            }
            Formula parsed = PinQuantities.bind(ModelLanguage.read(line.where(title), line.formula(), library), pinNames,
                    written -> line.where(title) + ": '" + written + "'");
            readsCurrents.add(PinQuantities.readsCurrent(parsed));
            int index = parsedEquations.size();
            parsedEquations.add(parsed);
            Formula rates = Substitution.replaceHostCalls(parsed, call -> switch (call.name()) {
                case ModelLanguage.NOISE -> noise(call, line, noiseDensities.computeIfAbsent(index, ignored -> new ArrayList<>()));
                case ModelLanguage.RATE -> rate(call, chargeFormulas, line);
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

        List<String> continuousStates = states.stream().filter(State::continuous).map(State::name).toList();
        this.linear = linearIn(nodes.size() + branchCount + chargeFormulas.size(), continuousStates, rightSides, chargeFormulas);
        this.readsTime = Stream.concat(rightSides.stream(), chargeFormulas.stream())
                .anyMatch(formula -> formula.dependsOn(EngineName.TIME.written()));

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
        stateNames.forEach(state -> {
            layout.add(new Argument.StateValue(state));
            names.add(state);
        });
        layout.add(new Argument.Time());
        names.add(EngineName.TIME.written());
        Symbols table = Symbols.withSlots(reserved, names, parameterNames);

        for (Formula charge : chargeFormulas) {
            charges.add(compile(charge, table, slot -> true));
        }
        noiseDensities.forEach((equation, densities) -> densities.forEach(density ->
                noises.add(new Noise(equation, compile(density.get(0), table, slot -> false),
                        compile(density.get(1), table, slot -> false)))));
        int firstRate = nodes.size() + branchCount;
        int afterHistories = coefficientSlot + 1 + chargeFormulas.size();
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
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
            equations.add(new CompiledEquation(line.kind(), line.positive(), line.negative(), line.state(), formula, linearized));
        }
    }

    /**
     * Every name its equations reach: the pins and nodes they flow between, and what their formulas read — the
     * definitions they call included —, so that a digital pin an equation reads is an analog node.
     */
    public static Set<String> namesReached(SimulatedComponent component) {
        return namesReached(component, component.equations().stream().map(Line::of).toList());
    }

    private static Set<String> namesReached(SimulatedComponent component, List<Line> lines) {
        Set<String> reached = new HashSet<>();
        List<Formula.Definition> library = ModelLanguage.definitions(component.title(), component.formulas());
        for (Line line : lines) {
            if (line.positive() != null) {
                reached.add(line.positive());
            }
            if (line.negative() != null) {
                reached.add(line.negative());
            }
            reached.addAll(ModelLanguage.read(line.where(component.title()), line.formula(), library).variables());
        }
        return reached;
    }

    /**
     * The states its equations read, by name: every single state, and of a row only the slots the equations name —
     * {@code registers_12} — as {@code row_index}, the way a display reads them.
     */
    private static List<String> equationStateNames(SimulatedComponent component, List<Line> lines) {
        Set<String> reached = namesReached(component, lines);
        List<String> names = new ArrayList<>();
        for (State state : component.states()) {
            if (state.index() == null) {
                names.add(state.name());
                continue;
            }
            state.index().names(state.name()).stream().filter(reached::contains).forEach(names::add);
        }
        return names;
    }

    /** Whether every formula is linear in the unknowns: each partial by one of them reads none of them. */
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

    public boolean linear() {
        return linear;
    }

    public boolean readsTime() {
        return readsTime;
    }

    /** Where a parameter's value stands in {@link #arguments()}; -1 for a parameter the unit does not have. */
    public int parameterPosition(String name) {
        int index = parameterNames.indexOf(name);
        return index < 0 ? -1 : layout.size() + index;
    }

    /** What each position of the argument array holds, before the parameters. */
    public List<Argument> layout() {
        return layout;
    }

    public List<CompiledEquation> equations() {
        return equations;
    }

    public List<Compiled> charges() {
        return charges;
    }

    public List<Noise> noises() {
        return noises;
    }

    /** An argument array with the parameter values already after the positions of {@link #layout()}. */
    public double[] arguments() {
        double[] arguments = new double[layout.size() + parameterValues.length];
        System.arraycopy(parameterValues, 0, arguments, layout.size(), parameterValues.length);
        return arguments;
    }

    /** Every {@code current(pin)} in the right sides, written out of the other equations. */
    private void bindPinCurrents(List<Formula> rightSides, List<Boolean> readsCurrents) {
        List<Formula> unbound = List.copyOf(rightSides);
        for (int index = 0; index < rightSides.size(); index++) {
            Line line = lines.get(index);
            rightSides.set(index, PinQuantities.bindCurrents(unbound.get(index), pins, pin -> currentInto(pin, unbound, readsCurrents, line)));
        }
    }

    /** A charge's rate written out: coefficient · q − history, both read from slots the device fills each step. */
    private Formula writtenRate(Formula.HostCall call, List<Formula> chargeFormulas, int coefficientSlot) {
        Formula charge = chargeOf(call.arguments().getFirst(), null);
        int index = chargeFormulas.indexOf(charge);
        Formula scaled = new Formula.Binary(Formula.Operator.MULTIPLY, new Formula.Slot(coefficientSlot, "rate coefficient"), charge);
        return new Formula.Binary(Formula.Operator.SUBTRACT, scaled,
                new Formula.Slot(coefficientSlot + 1 + index, "history of q" + (index + 1)));
    }

    /**
     * The component's {@code let} values as definitions, each read by jMF against the library and the lets before it,
     * and built from what jMF read — never from a text put together for it.
     */
    private List<Formula.Definition> locals(List<Equation.Local> locals, List<Formula.Definition> library) {
        List<Formula.Definition> bound = new ArrayList<>();
        List<Formula.Definition> known = new ArrayList<>(library);
        for (Equation.Local local : locals) {
            String where = title + ", let " + local.name();
            Formula body = ModelLanguage.read(where, local.value().written(), known);
            known.add(new Formula.Definition(local.name(), List.of(), body));
            // A pin's current and voltage are read in a let as in an equation; only a rate belongs where it adds a current.
            Formula members = PinQuantities.bind(body, pins, written -> where + ": '" + written + "'");
            Formula rates = Substitution.replaceHostCalls(members, call -> {
                if (PinQuantities.isCurrent(call)) {
                    return call;
                }
                throw new IllegalArgumentException(title + ": write " + (ModelLanguage.NOISE.equals(call.name()) ? "noise(…)" : "d(q)/dt")
                        + " in the current it adds to, not in 'let " + local.name() + "'.");
            });
            bound.add(new Formula.Definition(local.name(), List.of(), Substitution.bindHostNames(rates, this::nodeVoltage)));
        }
        return rebind(bound);
    }

    /**
     * Locals call one another; once each body is bound, a later local must call the bound version of an earlier one, not
     * the version the parser linked it to. Applications are relinked by name, in order.
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

    /** {@code d(q)/dt} as the slot of a new charge's rate. */
    private Formula rate(Formula.HostCall call, List<Formula> chargeFormulas, Line line) {
        if (call.arguments().size() != 1) {
            throw new IllegalArgumentException(line.where(title) + ": a rate is the rate of one charge: d(q)/dt.");
        }
        Formula charge = call.arguments().getFirst();
        if (holdsRate(charge)) {
            throw new IllegalArgumentException(line.where(title) + ": a charge cannot hold another d(…)/dt.");
        }
        chargeFormulas.add(chargeOf(charge, line));
        return new Formula.Slot(nodes.size() + branchCount + chargeFormulas.size() - 1, "d(q" + chargeFormulas.size() + ")/dt");
    }

    /** A {@code noise(density)} term: kept for the noise analysis, nothing to the solution; refused off a current equation. */
    private Formula noise(Formula.HostCall call, Line line, List<List<Formula>> densities) {
        if (call.arguments().isEmpty() || call.arguments().size() > 2) {
            throw new IllegalArgumentException(line.where(title)
                    + ": noise(…) takes a density in A²/Hz and, if it has 1/f noise, its corner frequency in Hz.");
        }
        if (line.kind() != Kind.CURRENT) {
            throw new IllegalArgumentException(line.where(title) + ": noise(…) is a current; write it in a current equation.");
        }
        Formula corner = call.arguments().size() == 2 ? call.arguments().get(1) : new Formula.Constant(0);
        densities.add(List.of(Substitution.bindHostNames(call.arguments().getFirst(), this::nodeVoltage),
                Substitution.bindHostNames(corner, this::nodeVoltage)));
        return new Formula.Constant(0);
    }

    /**
     * The current into {@code pin}: each voltage equation's branch current where its branch starts on the pin, less where
     * it ends there, and each current equation's right side alike.
     */
    private Formula currentInto(String pin, List<Formula> rightSides, List<Boolean> readsCurrents, Line asking) {
        Formula sum = new Formula.Constant(0);
        int branch = 0;
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            Formula through = switch (line.kind()) {
                case VOLTAGE -> new Formula.Slot(nodes.size() + branch, "current of branch " + branch++);
                case CURRENT -> rightSides.get(index);
                case STATE -> null;
            };
            if (through == null || !pin.equals(line.positive()) && !pin.equals(line.negative())) {
                continue;
            }
            if (line.kind() == Kind.CURRENT && readsCurrents.get(index)) {
                throw new IllegalArgumentException(asking.where(title) + ": current(" + pin + ") needs " + line.where(title)
                        + ", which reads a pin's current itself; write that line without current(…).");
            }
            if (pin.equals(line.positive())) {
                sum = new Formula.Binary(Formula.Operator.ADD, sum, through);
            }
            if (pin.equals(line.negative())) {
                sum = new Formula.Binary(Formula.Operator.SUBTRACT, sum, through);
            }
        }
        return sum;
    }

    /** A charge with its names bound: a pin's name to its voltage, a pin's current to the branch currents on it. */
    private Formula chargeOf(Formula written, Line line) {
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

    /** The current into a pin through the {@code voltage} equations on it; refused where a current equation carries it. */
    private Formula branchCurrentInto(String pin, Line asking) {
        Formula sum = new Formula.Constant(0);
        int branch = 0;
        for (Line line : lines) {
            boolean touches = pin.equals(line.positive()) || pin.equals(line.negative());
            if (line.kind() == Kind.CURRENT && touches) {
                throw new IllegalArgumentException((asking == null ? title : asking.where(title)) + ": a charge reads " + pin
                        + ".current only where a voltage equation carries it; " + line.where(title)
                        + " carries it as a current, which the charge's own rate decides.");
            }
            if (line.kind() != Kind.VOLTAGE) {
                continue;
            }
            Formula through = new Formula.Slot(nodes.size() + branch, "current of branch " + branch);
            branch++;
            if (pin.equals(line.positive())) {
                sum = new Formula.Binary(Formula.Operator.ADD, sum, through);
            }
            if (pin.equals(line.negative())) {
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
                throw new IllegalArgumentException(title + ": '" + name + "' is both a pin and a " + what
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
        for (State state : states) {
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
            for (State state : states) {
                if (formula.dependsOn(state.name())) {
                    partials.add(RealCompiler.compile(Differentiation.derivative(formula, state.name()), table, source));
                }
            }
        }
        return new Compiled(value, symbols, partials.toArray(RealFunction[]::new), tape, new double[symbols.length]);
    }
}
