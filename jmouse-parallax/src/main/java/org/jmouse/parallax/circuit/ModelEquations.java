package org.jmouse.parallax.circuit;

import org.jmouse.formula.Differentiation;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.GradientCompiler;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.Symbols;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** The call a written {@code d(q)/dt} reaches the solver as: not a name, so an author can never write it. */
    static final String RATE    = "d/dt";

    /**
     * A noise current an equation adds, {@code noise(density)}: nothing to the solution, to a noise analysis a random
     * current along the equation of that one-sided power spectral density, A²/Hz — a junction's {@code 2 * q * I}.
     */
    static final String NOISE   = "noise";

    /**
     * The state a model's own heat is kept in, kelvin above the bench: a model that declares it heats itself on a
     * realistic bench. On an ideal bench it is nought everywhere but in its own rate equation — so every drift it would
     * move is computed once, as the bench's temperature is, not on every Newton iteration.
     */
    static final String HEATING = "heating";

    /** The bench's flag a model reads to know it is realistic. */
    static final String REALISTIC = "realistic";

    /** SPICE's spellings, read as calls only to be refused with what this language writes instead. */
    private static final Map<String, String> BORROWED = Map.of(
            "V", "a pin's name is its voltage — write 'anode - j', not V(anode, j), and 'output', not V(output).",
            "I", "a current is written on the left: 'current anode -> j = …', or read as 'anode.current'.",
            "ddt", "a rate is written d(q)/dt, not ddt(q).");

    /**
     * The language every model expression is read in: jMF, with the solver's rate — {@code d(q)/dt}, recognised in the
     * tree — and noise, a pin's members, and SPICE's spellings, read only to be refused.
     */
    static final FormulaLanguage LANGUAGE = FormulaLanguage.standard()
            .withHostCalls(NOISE, PinQuantities.CURRENT, PinQuantities.VOLTAGE)
            .withHostCalls(BORROWED.keySet().toArray(String[]::new))
            .withRate("t", RATE);

    /** The name a model's expressions read the simulated time by. */
    static final String TIME = "time";

    enum Kind { CURRENT, VOLTAGE, STATE }

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
    record Equation(Kind kind, String positive, String negative, String state, Compiled formula, Compiled linearized) {

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
                        List<String> states, List<String> parameterNames, List<String> pinNames, List<String> internalNodes,
                        boolean selfHeating) {
    }

    /** How many forms are kept: a catalogue's worth of models, and the drafts of the one being written. */
    private static final int KEPT_FORMS = 256;

    /**
     * The equations compiled so far, by form. Parsing and compiling a model is most of what building a circuit costs,
     * and every slider move builds it again — a voltage multiplier's twenty-four diodes compiled one diode
     * twenty-four times on each move.
     */
    private static final Map<Form, ModelEquations> COMPILED = java.util.Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Form, ModelEquations> eldest) {
                    return size() > KEPT_FORMS;
                }
            });

    private final ComponentModel model;
    private final List<Argument> layout = new ArrayList<>();
    private final List<String>   nodes;
    private final List<String>   parameterNames;
    private final double[]       parameterValues;
    private final List<Equation> equations = new ArrayList<>();
    private final List<Compiled> charges = new ArrayList<>();
    private final List<Noise>    noises  = new ArrayList<>();
    /** How many voltage equations there are: each has a branch current, laid out after the node voltages. */
    private final int branchCount;

    /**
     * The model's equations for one unit with these parameter values: compiled once per form, and copied for every
     * unit after the first, each copy with its own parameter values and scratch space.
     */
    static ModelEquations of(ComponentModel model, Map<String, Double> parameters, List<String> pinNames, List<String> internalNodes) {
        Form form = new Form(model.name(), model.formulas(), model.locals(), model.equations(),
                model.states().stream().map(ComponentModel.State::name).toList(), List.copyOf(parameters.keySet()),
                List.copyOf(pinNames), List.copyOf(internalNodes), selfHeating(parameters));
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
        this.parameterNames = compiled.parameterNames;
        this.parameterValues = parameters.values().stream().mapToDouble(Double::doubleValue).toArray();
        this.branchCount = compiled.branchCount;
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

        requireDistinctFromNodes(parameterNames, "parameter");
        requireDistinctFromNodes(List.copyOf(model.locals().keySet()), "let");
        requireDistinctFromNodes(model.states().stream().map(ComponentModel.State::name).toList(), "state");
        List<Formula.Definition> library = new ArrayList<>(LANGUAGE.definitions(libraryText(model)));
        library.addAll(locals(library));

        this.branchCount = (int) model.equations().stream().filter(equation -> kindOf(equation) == Kind.VOLTAGE).count();
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
            Formula parsed = PinQuantities.bind(read(model.name(), equation.formula(), equation.line(), library), pinNames,
                    written -> model.name() + ", line " + equation.line() + ": '" + written + "'");
            readsCurrents.add(PinQuantities.readsCurrent(parsed));
            int index = parsedEquations.size();
            parsedEquations.add(parsed);
            Formula rates = Substitution.replaceHostCalls(parsed, call -> NOISE.equals(call.name())
                    ? noise(call, equation, noiseDensities.computeIfAbsent(index, ignored -> new ArrayList<>()))
                    : rate(call, chargeFormulas, equation.line()));
            rightSides.add(Substitution.bindHostNames(rates, this::nodeVoltage));
        }
        bindPinCurrents(rightSides, readsCurrents);

        // The same equations with every rate written out over the charge itself: coefficient · q − history.
        int coefficientSlot = nodes.size() + branchCount + chargeFormulas.size();
        List<Formula> writtenOut = new ArrayList<>();
        for (Formula parsed : parsedEquations) {
            Formula rates = Substitution.replaceHostCalls(parsed, call -> NOISE.equals(call.name()) ? new Formula.Constant(0)
                    : writtenRate(call, chargeFormulas, coefficientSlot));
            writtenOut.add(Substitution.bindHostNames(rates, this::nodeVoltage));
        }
        bindPinCurrents(writtenOut, readsCurrents);

        // On an ideal bench its own heat is nought: wherever it is read but in its own rate equation.
        if (!selfHeating(parameters)) {
            for (int index = 0; index < model.equations().size(); index++) {
                if (!HEATING.equals(model.equations().get(index).state())) {
                    rightSides.set(index, withoutHeating(rightSides.get(index)));
                    writtenOut.set(index, withoutHeating(writtenOut.get(index)));
                }
            }
            chargeFormulas.replaceAll(ModelEquations::withoutHeating);
            noiseDensities.values().forEach(densities -> densities.replaceAll(pair -> pair.stream().map(ModelEquations::withoutHeating).toList()));
        }

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
        names.add(TIME);
        Symbols table = Symbols.withSlots(reserved, names, parameterNames);

        for (Formula charge : chargeFormulas) {
            charges.add(compile(charge, table, slot -> true));
        }
        // On an ideal bench the rate equation of its own heat is left out with the state; the rest keep their order.
        boolean[] leftOut = new boolean[model.equations().size()];
        int[] position = new int[model.equations().size()];
        int kept = 0;
        for (int index = 0; index < leftOut.length; index++) {
            leftOut[index] = !selfHeating(parameters) && HEATING.equals(model.equations().get(index).state());
            position[index] = leftOut[index] ? -1 : kept++;
        }
        noiseDensities.forEach((equation, densities) -> densities.forEach(density ->
                noises.add(new Noise(position[equation], compile(density.get(0), table, slot -> false),
                        compile(density.get(1), table, slot -> false)))));
        int firstRate = nodes.size() + branchCount;
        int afterHistories = coefficientSlot + 1 + chargeFormulas.size();
        for (int index = 0; index < model.equations().size(); index++) {
            if (leftOut[index]) {
                continue;
            }
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
            equations.add(new Equation(Kind.valueOf(equation.kind().name()), equation.positive(), equation.negative(),
                    equation.state(), formula, linearized));
        }
    }

    /** Whether the bench is realistic, so that a model that heats itself does. */
    private static boolean selfHeating(Map<String, Double> parameters) {
        return parameters.getOrDefault(REALISTIC, 0.0) != 0;
    }

    /** A formula with its model's own heat read as nought, inside the locals it calls as well. */
    private static Formula withoutHeating(Formula formula) {
        return Substitution.bindHostNames(formula, name -> HEATING.equals(name) ? new Formula.Constant(0) : null);
    }

    /** Every {@code current(pin)} in the right sides, written out of the other equations. */
    private void bindPinCurrents(List<Formula> rightSides, List<Boolean> readsCurrents) {
        List<Formula> unbound = List.copyOf(rightSides);
        for (int index = 0; index < rightSides.size(); index++) {
            int line = model.equations().get(index).line();
            rightSides.set(index, Substitution.bindHostNames(unbound.get(index), name -> PinQuantities.pinOf(name) != null
                    ? currentInto(PinQuantities.pinOf(name), unbound, readsCurrents, line) : null));
        }
    }

    /** A charge's rate written out: coefficient · q − history, both read from slots the device fills each step. */
    private Formula writtenRate(Formula.HostCall call, List<Formula> chargeFormulas, int coefficientSlot) {
        Formula charge = Substitution.bindHostNames(call.arguments().getFirst(), this::nodeVoltage);
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
            definitions = LANGUAGE.definitions(text.toString(), library).stream()
                    .map(definition -> new Formula.Definition(definition.name(), definition.parameters(),
                            refuseBorrowed(model.name() + ": ", definition.body())))
                    .toList();
        } catch (FormulaException failure) {
            throw new IllegalArgumentException(model.name() + ": " + failure.getMessage(), failure);
        }
        List<Formula.Definition> bound = new ArrayList<>();
        for (Formula.Definition definition : definitions) {
            Formula rates = Substitution.replaceHostCalls(definition.body(), call -> {
                throw new IllegalArgumentException(model.name() + ": write d(q)/dt in the current it adds to, not in 'local "
                        + definition.name() + "'.");
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
        if (charge.mentionsHostCall()) {
            throw new IllegalArgumentException(model.name() + ", line " + line + ": a charge cannot hold another d(…)/dt.");
        }
        // The charge is compiled on its own, so its pins are read as voltages here, not by the right side it sits in.
        chargeFormulas.add(Substitution.bindHostNames(charge, this::nodeVoltage));
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
        if (kindOf(equation) != Kind.CURRENT) {
            throw new IllegalArgumentException(model.name() + ", line " + equation.line()
                    + ": noise(…) is a current; write it in a current equation.");
        }
        Formula corner = call.arguments().size() == 2 ? call.arguments().get(1) : new Formula.Constant(0);
        densities.add(List.of(Substitution.bindHostNames(call.arguments().getFirst(), this::nodeVoltage),
                Substitution.bindHostNames(corner, this::nodeVoltage)));
        return new Formula.Constant(0);
    }

    private static Kind kindOf(ComponentModel.Equation equation) {
        return Kind.valueOf(equation.kind().name());
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
            Kind kind = kindOf(equation);
            Formula through = switch (kind) {
                case VOLTAGE -> new Formula.Slot(nodes.size() + branch, "current of branch " + branch++);
                case CURRENT -> rightSides.get(index);
                case STATE -> null;
            };
            if (through == null || !pin.equals(equation.positive()) && !pin.equals(equation.negative())) {
                continue;
            }
            if (kind == Kind.CURRENT && readsCurrents.get(index)) {
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

    /**
     * A model's expression as jMF reads it — every {@code d(q)/dt} already the solver's rate, recognised in the tree —,
     * with SPICE's spellings ({@code V(a, b)}, {@code I(…)}, {@code ddt(q)}) refused saying what to write instead.
     *
     * @throws IllegalArgumentException when it does not read, naming the model, the line and the text
     */
    static Formula read(String model, String text, int line, List<Formula.Definition> library) {
        String where = model + (line > 0 ? ", line " + line : "") + ": ";
        Formula parsed;
        try {
            parsed = LANGUAGE.parse(text, library);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(where + failure.getMessage() + " — in: " + text, failure);
        }
        return refuseBorrowed(where, parsed);
    }

    /** {@code parsed}, refused where it calls one of SPICE's spellings. */
    private static Formula refuseBorrowed(String where, Formula parsed) {
        return Substitution.replaceHostCalls(parsed, call -> {
            String instead = BORROWED.get(call.name());
            if (instead != null) {
                throw new IllegalArgumentException(where + instead);
            }
            return call;
        });
    }

    /** The formula and its exact partial derivatives by every slot {@code differentiated} admits and every state it reads. */
    private Compiled compile(Formula formula, Symbols table, java.util.function.IntPredicate differentiated) {
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

    /** The jMF definitions a model's equations may call, as one text. */
    static String libraryText(ComponentModel model) {
        StringBuilder text = new StringBuilder();
        for (String definitions : model.formulas()) {
            text.append(definitions).append('\n');
        }
        return text.toString();
    }
}
