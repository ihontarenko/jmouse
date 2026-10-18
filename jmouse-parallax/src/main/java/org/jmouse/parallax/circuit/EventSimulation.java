package org.jmouse.parallax.circuit;

import org.jmouse.parallax.circuit.ComponentModel.PinNature;
import org.jmouse.el.evaluation.BasicValuesChain;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ExpressionLanguageConversion;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Function;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.formula.Formula;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.Symbols;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.script.el.DeclaredFunctions;
import org.jmouse.script.el.ScriptExtension;
import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.el.budget.ScriptExecution;
import org.jmouse.script.el.node.FunctionDeclarationNode;
import org.jmouse.script.el.node.HandlerNode;
import org.jmouse.script.el.node.ScriptNode;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * The discrete half of a circuit: every model's {@code behaviour} run as events, every digital node resolved from
 * its drivers, and every digital pin on an analog node read through its family's thresholds.
 *
 * <p>The events a behaviour may handle, all written {@code on <event> '<argument>' [when …] do … end}:</p>
 * <ul>
 *   <li>{@code start} — once, at t = 0;</li>
 *   <li>{@code rising 'clk'}, {@code falling 'clk'}, {@code change 'd, nclr'} — a digital input changed level;</li>
 *   <li>{@code above 'V(thr, ctrl)'}, {@code below '…'} — a jMF expression of the unit's voltages crossed zero;</li>
 *   <li>{@code every '1 / fosc'} — periodically, from t = 0;</li>
 *   <li>{@code timer 'name'} — when a {@code schedule('name', delay)} comes due.</li>
 * </ul>
 *
 * <p>⚠️ <strong>Handlers that fire at the same instant read the state as it was before it</strong>, and their writes
 * land together after — the way a register samples on an edge. That is what makes two clocks tied together behave
 * as a datasheet says.</p>
 */
final class EventSimulation {

    private static final String  SCOPE   = "__emulator";
    private static final int     DELTAS  = 10_000;
    private static final double  INSTANT = 1e-15;

    private static final ScriptBudget BUDGET = ScriptBudget.builder().steps(200_000).loopIterations(100_000).recursionDepth(64).build();

    private final List<Unit> units = new ArrayList<>();
    private final Map<String, List<DigitalDriver>> driversByNode = new LinkedHashMap<>();
    private final Map<String, Object> nodeLevels = new LinkedHashMap<>();
    private final List<OutputBridge> bridges = new ArrayList<>();
    private final List<Netlist.Element> inputCapacitances = new ArrayList<>();
    private final StandardExtensionContainer extensions = new StandardExtensionContainer();
    /** One conversion for every handler run: building it registers every converter, which cost more than the handler. */
    private final ExpressionLanguageConversion conversion = new ExpressionLanguageConversion();
    private double now;

    EventSimulation(Netlist netlist) {
        extensions.importExtension(new ScriptExtension());
        extensions.importExtension(new EmulatorExtension());
        netlist.digitalNodes().forEach(node -> {
            driversByNode.put(node, new ArrayList<>());
            nodeLevels.put(node, DigitalDriver.RELEASED);
        });
        for (Netlist.ModelUnit modelUnit : netlist.units()) {
            Unit unit = new Unit(modelUnit, this);
            units.add(unit);
            unit.drivers.values().forEach(driver -> {
                if (driver.digitalNode != null) {
                    driversByNode.get(driver.digitalNode).add(driver);
                }
            });
        }
    }

    boolean active() {
        return units.stream().anyMatch(unit -> unit.script != null || !unit.drivers.isEmpty() || !unit.inputs.isEmpty());
    }

    /** The discrete states of one unit, shared with its {@link ModelDevice}. */
    Map<String, Object> discrete(String label) {
        return units.stream().filter(unit -> unit.label.equals(label)).findFirst().map(unit -> unit.discrete).orElseThrow();
    }

    /** The devices that put digital outputs onto analog nodes. */
    List<OutputBridge> bridges() {
        return bridges;
    }

    /** Input-pin capacitances on analog nodes, as ordinary solver capacitors. */
    List<Netlist.Element> inputCapacitances() {
        return inputCapacitances;
    }

    /** t = 0: inputs read, {@code start} handlers run, everything they drive at once applied. */
    void start(double[] state) {
        now = 0;
        for (Unit unit : units) {
            unit.watch(state, 0);
            unit.readInputs(state, true);
        }
        List<Event> events = new ArrayList<>();
        for (Unit unit : units) {
            events.add(new Event(unit, "start", null));
        }
        settle(state, events);
    }

    /**
     * Goes on from where {@code previous} is: the same circuit with other values — a wiper moved, a switch closed —
     * keeps every discrete thing it had: each unit's states, its drivers with their pending transactions, its inputs,
     * timers and periods, the sign of each watched expression, the node levels and the clock.
     *
     * @throws IllegalArgumentException when the two do not have the same units, drivers and handlers
     */
    void continueFrom(EventSimulation previous) {
        if (units.size() != previous.units.size()) {
            throw new IllegalArgumentException("The circuit's parts changed; it starts again.");
        }
        for (int index = 0; index < units.size(); index++) {
            units.get(index).continueFrom(previous.units.get(index));
        }
        nodeLevels.clear();
        nodeLevels.putAll(previous.nodeLevels);
        now = previous.now;
    }

    /** The next instant something discrete is due, after {@code time}; infinity when nothing is. */
    double nextTime(double time) {
        double next = Double.POSITIVE_INFINITY;
        for (Unit unit : units) {
            for (DigitalDriver driver : unit.drivers.values()) {
                Double due = driver.pending.higherKey(time + INSTANT);
                if (due != null) {
                    next = Math.min(next, due);
                }
                if (driver.changedAt + unit.edge > time + INSTANT && unit.edge > 0) {
                    next = Math.min(next, driver.changedAt + unit.edge);
                }
            }
            for (double due : unit.timers.values()) {
                if (due > time + INSTANT) {
                    next = Math.min(next, due);
                }
            }
            for (Periodic periodic : unit.periodics) {
                next = Math.min(next, periodic.next);
            }
        }
        return next;
    }

    /** Whether, at this candidate state, a watched expression or a bridged input would change. */
    boolean crossed(double[] state, double time) {
        for (Unit unit : units) {
            if (unit.crossings(state, time) || unit.inputsWouldChange(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Where in a step from {@code before} to {@code after} something discrete first happens, as a share of the step:
     * for a watched expression, the root of the straight line between its values at the two ends (regula falsi);
     * for a digital input crossing its threshold, the middle. 1 when nothing happens in the step.
     */
    double crossingShare(double[] before, double timeBefore, double[] after, double timeAfter) {
        double share = 1;
        for (Unit unit : units) {
            share = Math.min(share, unit.crossingShare(before, timeBefore, after, timeAfter));
            if (unit.inputsWouldChange(after)) {
                share = Math.min(share, 0.5);
            }
        }
        return share;
    }

    /** Commits the accepted state at {@code time}: crossings fire, and everything due runs. True if anything changed. */
    boolean advance(double[] state, double time) {
        now = time;
        List<Event> events = new ArrayList<>();
        for (Unit unit : units) {
            events.addAll(unit.fireCrossings(state, time));
            events.addAll(unit.readInputs(state, false));
        }
        return settle(state, events);
    }

    /** The level of every digital node, for the datasets. */
    Map<String, Object> levels() {
        return nodeLevels;
    }

    // ── one instant ──────────────────────────────────────────────────────────────────────────────────────────────

    private record Event(Unit unit, String kind, String subject) {
    }

    private boolean settle(double[] state, List<Event> initial) {
        boolean changed = false;
        List<Event> events = new ArrayList<>(initial);
        for (int delta = 0; delta < DELTAS; delta++) {
            events.addAll(applyDue(state));
            for (Unit unit : units) {
                events.addAll(unit.dueTimers(now));
            }
            if (events.isEmpty()) {
                return changed;
            }
            changed |= run(state, events);
            events = new ArrayList<>();
        }
        throw new IllegalArgumentException("The digital part does not settle at t = %s s: more than %d changes in one instant."
                .formatted(String.format("%.6g", now), DELTAS));
    }

    /** Applies transactions due now; answers the input changes they cause. */
    private List<Event> applyDue(double[] state) {
        List<Event> events = new ArrayList<>();
        List<String> touchedNodes = new ArrayList<>();
        for (Unit unit : units) {
            for (DigitalDriver driver : unit.drivers.values()) {
                if (driver.applyDue(now + INSTANT)) {
                    unit.lastChange.put(driver.pin, now);
                    if (driver.digitalNode != null) {
                        touchedNodes.add(driver.digitalNode);
                    }
                }
            }
        }
        for (String node : touchedNodes) {
            nodeLevels.put(node, resolve(driversByNode.get(node)));
        }
        for (Unit unit : units) {
            events.addAll(unit.readInputs(state, false));
        }
        return events;
    }

    private static Object resolve(List<DigitalDriver> drivers) {
        Object level = DigitalDriver.RELEASED;
        for (DigitalDriver driver : drivers) {
            if (driver.level.equals(DigitalDriver.RELEASED)) {
                continue;
            }
            if (level.equals(DigitalDriver.RELEASED)) {
                level = driver.level;
            } else if (!level.equals(driver.level)) {
                return DigitalDriver.UNKNOWN;
            }
        }
        return level;
    }

    /** Runs every handler the events select, each over a copy of the state before the instant; writes land after. */
    private boolean run(double[] state, List<Event> events) {
        Map<Unit, Map<String, Object>> before = new LinkedHashMap<>();
        for (Event event : events) {
            before.computeIfAbsent(event.unit(), unit -> copy(unit.discrete));
        }
        Map<Unit, Map<String, Object>> writes = new LinkedHashMap<>();
        boolean changed = false;
        for (Event event : events) {
            Unit unit = event.unit();
            if (unit.script == null) {
                continue;
            }
            for (HandlerNode handler : unit.handlersFor(event)) {
                Map<String, Object> written = unit.runHandler(handler, before.get(unit), state);
                if (!written.isEmpty()) {
                    writes.computeIfAbsent(unit, key -> new LinkedHashMap<>()).putAll(written);
                }
            }
        }
        for (Map.Entry<Unit, Map<String, Object>> entry : writes.entrySet()) {
            entry.getKey().discrete.putAll(entry.getValue());
            changed = true;
        }
        for (Unit unit : units) {
            changed |= unit.drivers.values().stream().anyMatch(driver -> !driver.pending.isEmpty() || driver.changedAt == now);
        }
        return changed;
    }

    private static Map<String, Object> copy(Map<String, Object> state) {
        Map<String, Object> copy = new LinkedHashMap<>();
        state.forEach((name, value) -> copy.put(name, value instanceof Object[] array ? array.clone() : value));
        return copy;
    }

    // ── one unit ─────────────────────────────────────────────────────────────────────────────────────────────────

    /** A unit's discrete life: its script, states, drivers, inputs, watched expressions, periods and timers. */
    private static final class Unit {

        private static final int DIGITAL_PIN = -1;

        final String label;
        final Netlist.ModelUnit model;
        final EventSimulation simulation;
        final Map<String, Object> discrete = new LinkedHashMap<>();
        final Map<String, DigitalDriver> drivers = new LinkedHashMap<>();
        final Map<String, Object> inputs = new LinkedHashMap<>();
        final Map<String, Double> lastChange = new HashMap<>();
        final Map<String, Double> timers = new LinkedHashMap<>();
        final List<Periodic> periodics = new ArrayList<>();
        final List<Watch> watches = new ArrayList<>();
        final Map<String, FunctionDeclarationNode> functions = new LinkedHashMap<>();
        /** Each handler's argument — the input or expression it listens to — read once, not at every event. */
        final Map<HandlerNode, String> handlerArguments = new IdentityHashMap<>();
        final ScriptNode script;
        final double edge;
        final double inputLow;
        final double inputHigh;
        final double delayReference;
        final double delayExponent;
        /** The analog nodes of its supply and ground pins, found once: read at every candidate step. */
        final int powerNode;
        final int groundNode;
        /** The node each pin and internal node reads, in the order of the watched expressions' arguments. */
        final int[] argumentNodes;
        /** The arguments at the end of a step, and at its start, rewritten for every reading. */
        final double[] argumentsAfter;
        final double[] argumentsBefore;

        Unit(Netlist.ModelUnit model, EventSimulation simulation) {
            this.model = model;
            this.label = model.label();
            this.simulation = simulation;
            this.powerNode = rail(model, PinNature.POWER);
            this.groundNode = rail(model, PinNature.GROUND);
            ComponentModel family = model.family();
            this.edge = familyValue(family, "edge", 5e-9);
            this.inputLow = familyValue(family, "input_low", 0.3);
            this.inputHigh = familyValue(family, "input_high", 0.7);
            this.delayReference = familyValue(family, "delay_reference", 4.5);
            this.delayExponent = familyValue(family, "delay_exponent", 0);
            this.argumentNodes = IntStream.concat(
                    model.pins().values().stream().mapToInt(binding -> binding.pin().nature() == PinNature.DIGITAL ? DIGITAL_PIN : binding.analog()),
                    model.internalNodes().values().stream().mapToInt(Integer::intValue)).toArray();
            for (ComponentModel.State state : model.model().states()) {
                if (state.discrete()) {
                    Object initial = state.initial();
                    discrete.put(state.name(), initial instanceof Object[] array ? array.clone() : initial);
                }
            }
            model.internalDigitalNodes().forEach((name, node) -> {
                drivers.put(name, new DigitalDriver(label, name, node));
                inputs.put(name, DigitalDriver.UNKNOWN);
            });
            model.pins().forEach((name, binding) -> {
                if (binding.pin().nature() != PinNature.DIGITAL) {
                    return;
                }
                if (binding.pin().output()) {
                    DigitalDriver driver = new DigitalDriver(label, name, binding.isDigitalNode() ? binding.digitalNode() : null);
                    drivers.put(name, driver);
                    if (!binding.isDigitalNode()) {
                        simulation.bridges.add(new OutputBridge(driver, binding.analog(), powerNode, groundNode,
                                familyValue(family, "output_resistance", 50), delayReference, edge,
                                familyValue(family, "output_capacitance", 0)));
                    }
                } else {
                    inputs.put(name, DigitalDriver.UNKNOWN);
                    double capacitance = familyValue(family, "input_capacitance", 0);
                    if (!binding.isDigitalNode() && capacitance > 0) {
                        simulation.inputCapacitances.add(new Netlist.Element(label + "." + name + ".Cin",
                                CircuitElementType.CAPACITOR,
                                binding.analog(), groundNode, capacitance, -1));
                    }
                }
            });
            this.argumentsAfter = newArguments();
            this.argumentsBefore = newArguments();
            this.script = model.model().behaviour();
            if (script != null) {
                script.getFunctions().forEach(function -> functions.put(function.getName(), function));
                for (HandlerNode handler : script.getHandlers()) {
                    String argument = argumentOf(handler);
                    handlerArguments.put(handler, argument);
                    switch (handler.getEvent()) {
                        case "above", "below" -> watches.add(new Watch(handler.getEvent(), argument, compile(argument)));
                        case "every" -> {
                            double period = compile(argument).evaluate(arguments(newArguments(), new double[0], 0));
                            if (!(period > 0)) {
                                throw new IllegalArgumentException(model.model().name() + ": 'every " + argument + "' is not a positive period.");
                            }
                            periodics.add(new Periodic(argument, period, 0));
                        }
                        case "start", "rising", "falling", "change", "timer" -> {
                        }
                        default -> throw new IllegalArgumentException(model.model().name() + ": 'on " + handler.getEvent()
                                + "' is not an event of the emulator — start, rising, falling, change, above, below, every, timer.");
                    }
                }
            }
        }

        /** Takes over the discrete life of the same unit in an earlier build of the circuit. */
        void continueFrom(Unit earlier) {
            if (!label.equals(earlier.label) || !drivers.keySet().equals(earlier.drivers.keySet())
                    || watches.size() != earlier.watches.size() || periodics.size() != earlier.periodics.size()) {
                throw new IllegalArgumentException("The circuit's parts changed; it starts again.");
            }
            discrete.clear();
            discrete.putAll(copy(earlier.discrete));
            drivers.forEach((name, driver) -> {
                DigitalDriver before = earlier.drivers.get(name);
                driver.level = before.level;
                driver.previous = before.previous;
                driver.changedAt = before.changedAt;
                driver.pending.clear();
                driver.pending.putAll(before.pending);
            });
            inputs.clear();
            inputs.putAll(earlier.inputs);
            lastChange.clear();
            lastChange.putAll(earlier.lastChange);
            timers.clear();
            timers.putAll(earlier.timers);
            for (int index = 0; index < watches.size(); index++) {
                watches.get(index).sign = earlier.watches.get(index).sign;
            }
            for (int index = 0; index < periodics.size(); index++) {
                periodics.get(index).next = earlier.periodics.get(index).next;
            }
        }

        private static int rail(Netlist.ModelUnit model, PinNature nature) {
            return model.pins().values().stream().filter(binding -> binding.pin().nature() == nature)
                    .map(Netlist.PinBinding::analog).findFirst().orElse(Netlist.GROUND);
        }

        List<HandlerNode> handlersFor(Event event) {
            List<HandlerNode> selected = new ArrayList<>();
            for (HandlerNode handler : script.getHandlers()) {
                if (!handler.getEvent().equals(event.kind())) {
                    continue;
                }
                String argument = handlerArguments.get(handler);
                boolean matches = switch (event.kind()) {
                    case "start" -> true;
                    case "rising", "falling", "change" -> Arrays.stream(argument.split("[,\\s]+")).anyMatch(event.subject()::equals);
                    default -> argument.equals(event.subject());
                };
                if (matches) {
                    selected.add(handler);
                }
            }
            return selected;
        }

        /** Runs one handler over a copy of the pre-instant state; answers the states it changed. */
        Map<String, Object> runHandler(HandlerNode handler, Map<String, Object> before, double[] state) {
            DefaultEvaluationContext context = new DefaultEvaluationContext(new BasicValuesChain(), simulation.extensions,
                    simulation.conversion);
            model.parameters().forEach(context::setValue);
            Map<String, Object> working = copy(before);
            working.forEach(context::setValue);
            model.pins().forEach((name, binding) -> context.setValue(name, pinValue(name, binding, state)));
            model.internalNodes().forEach((name, node) -> context.setValue(name, voltage(node, state)));
            model.internalDigitalNodes().forEach((name, node) -> context.setValue(name,
                    simulation.nodeLevels.getOrDefault(node, DigitalDriver.UNKNOWN)));
            context.setValue("X", DigitalDriver.UNKNOWN);
            context.setValue("Z", DigitalDriver.RELEASED);
            context.setValue("time", simulation.now);
            context.setValue(SCOPE, new Scope(this, state));
            DeclaredFunctions.of(functions).installInto(context);
            ScriptExecution execution = ScriptExecution.begin(label, BUDGET);
            execution.installInto(context);
            try {
                handler.evaluate(context);
            } finally {
                ScriptExecution.clear(context);
            }
            Map<String, Object> written = new LinkedHashMap<>();
            for (String name : discrete.keySet()) {
                Object after = context.getValue(name);
                if (!same(after, before.get(name))) {
                    written.put(name, after instanceof List<?> list ? list.toArray() : after);
                }
            }
            return written;
        }

        Object pinValue(String name, Netlist.PinBinding binding, double[] state) {
            if (binding.pin().nature() == PinNature.DIGITAL) {
                if (binding.pin().output()) {
                    return drivers.get(name).level;
                }
                return inputs.get(name);
            }
            return voltage(binding.analog(), state);
        }

        /** Digital inputs: a digital node's resolved level, or an analog node through the family thresholds. */
        List<Event> readInputs(double[] state, boolean initial) {
            List<Event> events = new ArrayList<>();
            for (String name : List.copyOf(inputs.keySet())) {
                Object level = inputLevel(name, state);
                Object held = inputs.get(name);
                if (initial) {
                    inputs.put(name, level);
                    continue;
                }
                if (!level.equals(held)) {
                    inputs.put(name, level);
                    lastChange.put(name, simulation.now);
                    events.add(new Event(this, "change", name));
                    if (Objects.equals(level, 1)) {
                        events.add(new Event(this, "rising", name));
                    } else if (Objects.equals(level, 0)) {
                        events.add(new Event(this, "falling", name));
                    }
                }
            }
            return events;
        }

        boolean inputsWouldChange(double[] state) {
            for (String name : inputs.keySet()) {
                Netlist.PinBinding binding = model.pins().get(name);
                if (binding != null && !binding.isDigitalNode() && !inputLevel(name, state).equals(inputs.get(name))) {
                    return true;
                }
            }
            return false;
        }

        private Object inputLevel(String name, double[] state) {
            Netlist.PinBinding binding = model.pins().get(name);
            if (binding == null) {
                return digitalNodeLevel(model.internalDigitalNodes().get(name));
            }
            if (binding.isDigitalNode()) {
                return digitalNodeLevel(binding.digitalNode());
            }
            double ground = voltage(groundNode, state);
            double supply = voltage(powerNode, state) - ground;
            double value = voltage(binding.analog(), state) - ground;
            if (supply < 0.5) {
                return DigitalDriver.UNKNOWN;
            }
            if (value >= inputHigh * supply) {
                return 1;
            }
            if (value <= inputLow * supply) {
                return 0;
            }
            Object previous = inputs.get(name);
            // Between the thresholds an input holds what it last was: what a slow edge does on a real part is not
            // promised, and holding is the reading that keeps a clean edge clean.
            return previous == null ? DigitalDriver.UNKNOWN : previous;
        }

        /** What an input on a digital node reads: the node's resolved level, a released node read as unknown. */
        private Object digitalNodeLevel(String node) {
            Object level = simulation.nodeLevels.getOrDefault(node, DigitalDriver.RELEASED);
            return level.equals(DigitalDriver.RELEASED) ? DigitalDriver.UNKNOWN : level;
        }

        void watch(double[] state, double time) {
            double[] arguments = arguments(argumentsAfter, state, time);
            for (Watch watch : watches) {
                watch.sign = Math.signum(watch.function.evaluate(arguments));
            }
        }

        boolean crossings(double[] state, double time) {
            double[] arguments = arguments(argumentsAfter, state, time);
            for (Watch watch : watches) {
                double sign = Math.signum(watch.function.evaluate(arguments));
                if (crosses(watch, sign)) {
                    return true;
                }
            }
            return false;
        }

        double crossingShare(double[] before, double timeBefore, double[] after, double timeAfter) {
            double share = 1;
            double[] atEnd = arguments(argumentsAfter, after, timeAfter);
            double[] atStart = null;
            for (Watch watch : watches) {
                double end = watch.function.evaluate(atEnd);
                if (!crosses(watch, Math.signum(end))) {
                    continue;
                }
                if (atStart == null) {
                    atStart = arguments(argumentsBefore, before, timeBefore);
                }
                double start = watch.function.evaluate(atStart);
                double root = start == end || !Double.isFinite(start) ? 0.5 : start / (start - end);
                share = Math.min(share, Math.max(0, Math.min(1, root)));
            }
            return share;
        }

        List<Event> fireCrossings(double[] state, double time) {
            List<Event> events = new ArrayList<>();
            double[] arguments = arguments(argumentsAfter, state, time);
            for (Watch watch : watches) {
                double sign = Math.signum(watch.function.evaluate(arguments));
                if (crosses(watch, sign)) {
                    events.add(new Event(this, watch.kind, watch.argument));
                }
                if (sign != 0) {
                    watch.sign = sign;
                }
            }
            return events;
        }

        /**
         * A zero sign means "not on either side yet" — what every expression is in the state at rest before the
         * sources come up — so the first side it lands on counts as a crossing: a trigger held low at power-up
         * fires the way it does on a real part.
         */
        private static boolean crosses(Watch watch, double sign) {
            return watch.kind.equals("above") ? watch.sign <= 0 && sign > 0 : watch.sign >= 0 && sign < 0;
        }

        List<Event> dueTimers(double time) {
            List<Event> events = new ArrayList<>();
            for (Map.Entry<String, Double> timer : List.copyOf(timers.entrySet())) {
                if (timer.getValue() <= time + INSTANT) {
                    timers.remove(timer.getKey());
                    events.add(new Event(this, "timer", timer.getKey()));
                }
            }
            for (Periodic periodic : periodics) {
                while (periodic.next <= time + INSTANT) {
                    periodic.next += periodic.period;
                    events.add(new Event(this, "every", periodic.argument));
                }
            }
            return events;
        }

        /**
         * A jMF expression of the unit's voltages, discrete states, parameters and the time. A pin's or a node's name is
         * its voltage, bound on the tree to its slot — the pins first, then the internal nodes, as {@link #arguments}
         * lays them —; a digital pin's name is its level, as the behaviour writes it, so a discrete state keeps its name.
         */
        private RealFunction compile(String expression) {
            Formula parsed = ModelEquations.read(model.model().name(), expression, 0,
                    ModelEquations.LANGUAGE.definitions(ModelEquations.libraryText(model.model())));
            List<String> nodes = new ArrayList<>(model.pins().keySet());
            nodes.addAll(model.internalNodes().keySet());
            Formula bound = Substitution.bindHostNames(Substitution.replaceHostCalls(parsed, call -> {
                throw new IllegalArgumentException(model.model().name() + ": an event reads voltages, not the rate d(…)/dt.");
            }), name -> discrete.containsKey(name) || !nodes.contains(name) ? null : new Formula.Slot(nodes.indexOf(name), name));
            List<String> names = new ArrayList<>(discrete.keySet());
            names.add(ModelEquations.TIME);
            return RealCompiler.compile(bound, Symbols.withSlots(nodes.size(), names, List.copyOf(model.parameters().keySet())),
                    expression);
        }

        /** A fresh argument array for the watched expressions, its parameters already in place. */
        private double[] newArguments() {
            double[] arguments = new double[argumentNodes.length + discrete.size() + 1 + model.parameters().size()];
            int position = argumentNodes.length + discrete.size() + 1;
            for (double value : model.parameters().values()) {
                arguments[position++] = value;
            }
            return arguments;
        }

        /** Writes the pins' and nodes' voltages, the discrete states and the time into an argument array. */
        private double[] arguments(double[] into, double[] state, double time) {
            int position = 0;
            for (int node : argumentNodes) {
                into[position++] = node == DIGITAL_PIN ? 0 : voltage(node, state);
            }
            for (Object value : discrete.values()) {
                into[position++] = ModelDevice.numeric(value);
            }
            into[position] = time;
            return into;
        }

        double delayScale(double supply) {
            return delayExponent == 0 || supply <= 0 ? 1 : Math.pow(delayReference / supply, delayExponent);
        }

        private static double voltage(int node, double[] state) {
            return node == Netlist.GROUND || node >= state.length ? 0 : state[node];
        }

        private static double familyValue(ComponentModel family, String name, double fallback) {
            return family == null ? fallback : family.parameter(name).map(ComponentModel.Parameter::value).orElse(fallback);
        }

        private static String argumentOf(HandlerNode handler) {
            if (handler.getArgument() == null) {
                return "";
            }
            Object value = handler.getArgument().evaluate(new DefaultEvaluationContext());
            return String.valueOf(value).strip();
        }

        private static boolean same(Object left, Object right) {
            Object[] leftArray = arrayOf(left);
            Object[] rightArray = arrayOf(right);
            if (leftArray != null && rightArray != null) {
                return Arrays.equals(leftArray, rightArray);
            }
            if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
                return leftNumber.doubleValue() == rightNumber.doubleValue();
            }
            return Objects.equals(left, right);
        }

        static Object[] arrayOf(Object value) {
            if (value instanceof Object[] array) {
                return array;
            }
            if (value instanceof List<?> list) {
                return list.toArray();
            }
            if (value != null && value.getClass().isArray()) {
                Object[] array = new Object[Array.getLength(value)];
                for (int index = 0; index < array.length; index++) {
                    array[index] = Array.get(value, index);
                }
                return array;
            }
            return null;
        }
    }

    private static final class Watch {
        final String kind;
        final String argument;
        final RealFunction function;
        double sign;

        Watch(String kind, String argument, RealFunction function) {
            this.kind = kind;
            this.argument = argument;
            this.function = function;
        }
    }

    private static final class Periodic {
        final String argument;
        final double period;
        double next;

        Periodic(String argument, double period, double next) {
            this.argument = argument;
            this.period = period;
            this.next = next;
        }
    }

    /** What the emulator's functions reach while a handler runs. */
    private record Scope(Unit unit, double[] state) {
    }

    // ── the emulator's functions ─────────────────────────────────────────────────────────────────────────────────

    /**
     * What a behaviour may call besides jMS's own vocabulary: {@code drive}, {@code schedule}, {@code since},
     * {@code V}, {@code item}, {@code with} and the family's {@code delay_scale}. Nothing reaches outside the unit.
     */
    private static final class EmulatorExtension implements Extension {
        @Override
        public List<Function> getFunctions() {
            return List.of(
                    function("drive", (arguments, scope) -> {
                        String pin = String.valueOf(arguments.get(0));
                        DigitalDriver driver = scope.unit().drivers.get(pin);
                        if (driver == null) {
                            throw new IllegalArgumentException(scope.unit().model.model().name() + ": drive('" + pin + "') names no digital output or internal node.");
                        }
                        double delay = arguments.size() > 2 ? number(arguments.get(2)) : 0;
                        driver.schedule(scope.unit().simulation.now + Math.max(delay, 0), DigitalDriver.normalized(arguments.get(1)));
                        return null;
                    }),
                    function("schedule", (arguments, scope) -> {
                        scope.unit().timers.put(String.valueOf(arguments.get(0)), scope.unit().simulation.now + Math.max(number(arguments.get(1)), 0));
                        return null;
                    }),
                    function("since", (arguments, scope) ->
                            scope.unit().simulation.now - scope.unit().lastChange.getOrDefault(String.valueOf(arguments.get(0)), Double.NEGATIVE_INFINITY)),
                    function("item", (arguments, scope) -> Unit.arrayOf(arguments.get(0))[(int) number(arguments.get(1))]),
                    function("with", (arguments, scope) -> {
                        Object[] copy = Unit.arrayOf(arguments.get(0)).clone();
                        copy[(int) number(arguments.get(1))] = arguments.get(2);
                        return copy;
                    }),
                    function("delay_scale", (arguments, scope) -> scope.unit().delayScale(number(arguments.get(0)))));
        }

        private interface Body {
            Object apply(Arguments arguments, Scope scope);
        }

        private static Function function(String name, Body body) {
            return new Function() {
                @Override
                public Object execute(Arguments arguments, EvaluationContext context) {
                    return body.apply(arguments, (Scope) context.getValue(SCOPE));
                }

                @Override
                public String getName() {
                    return name;
                }
            };
        }

        private static double number(Object value) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            throw new IllegalArgumentException("'" + value + "' is not a number.");
        }
    }
}
