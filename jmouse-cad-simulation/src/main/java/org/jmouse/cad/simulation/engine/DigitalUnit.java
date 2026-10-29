package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.State;
import org.jmouse.cad.simulation.EngineName;
import org.jmouse.cad.simulation.ModelLanguage;
import org.jmouse.el.evaluation.BasicValuesChain;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.formula.Formula;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.Symbols;
import org.jmouse.script.el.DeclaredFunctions;
import org.jmouse.script.el.budget.ScriptBudgetExceededException;
import org.jmouse.script.el.budget.ScriptExecution;
import org.jmouse.script.el.node.DeclarationNode;
import org.jmouse.script.el.node.FunctionDeclarationNode;
import org.jmouse.script.el.node.HandlerNode;
import org.jmouse.script.el.node.ScriptNode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/** A unit's discrete life: its script, states, drivers, inputs, watched expressions, periods and timers. */
final class DigitalUnit {

    private static final int DIGITAL_PIN = -1;

    final String label;
    final Netlist.ModelUnit model;
    final EventSimulation simulation;
    final Map<String, Object> discrete = new LinkedHashMap<>();
    final Map<String, DigitalDriver> drivers = new LinkedHashMap<>();
    final Map<String, LogicLevel> inputs = new LinkedHashMap<>();
    /** The bus each bit belongs to, by the bit's pin: read on every input change, so worked out once. */
    private final Map<String, String> busByPin = new HashMap<>();
    final Map<String, Double> lastChange = new HashMap<>();
    final Map<String, Double> timers = new LinkedHashMap<>();
    final List<Periodic> periodics = new ArrayList<>();
    final List<Watch> watches = new ArrayList<>();
    final Map<String, FunctionDeclarationNode> functions = new LinkedHashMap<>();
    /** The behaviour's own names — its `final let` lines — bound in every handler before it runs, in written order. */
    private final List<DeclarationNode> constants = new ArrayList<>();
    /** Its handlers, each with what it listens to read once rather than at every event, in written order. */
    private final List<Listener> listeners = new ArrayList<>();
    final ScriptNode script;
    /** The logic family its digital pins follow, compiled from its document; null for a part with no digital pin. */
    final LogicFamily family;
    /** Its outputs' transition time: the family's; nought for a part with no family. */
    final double edge;
    /** The analog nodes of its supply and ground pins, found once: read at every candidate step. */
    final int powerNode;
    final int groundNode;
    /** The node each pin and internal node reads, in the order of the watched expressions' arguments. */
    final int[] argumentNodes;
    /** The arguments at the end of a step, and at its start, rewritten for every reading. */
    final double[] argumentsAfter;
    final double[] argumentsBefore;

    DigitalUnit(Netlist.ModelUnit model, EventSimulation simulation) {
        this.model = model;
        model.component().buses().forEach((bus, bits) -> bits.forEach(bit -> busByPin.put(bit, bus)));
        this.label = model.label();
        this.simulation = simulation;
        this.powerNode = rail(model, Pin::power);
        this.groundNode = rail(model, Pin::ground);
        this.family = model.family() == null ? null : LogicFamily.of(model.family());
        this.edge = family == null ? 0 : family.edge();
        this.argumentNodes = IntStream.concat(
                model.pins().values().stream().mapToInt(binding -> binding.pin().digital() ? DIGITAL_PIN : binding.analog()),
                model.internalNodes().values().stream().mapToInt(Integer::intValue)).toArray();
        for (State state : model.component().states()) {
            if (!state.continuous()) {
                discrete.put(state.name(), StateStart.of(state, label));
            }
        }
        model.internalDigitalNodes().forEach((name, node) -> {
            drivers.put(name, new DigitalDriver(label, name, node));
            inputs.put(name, LogicLevel.UNKNOWN);
        });
        model.pins().forEach((name, binding) -> {
            if (!binding.pin().digital()) {
                return;
            }
            // A bidirectional pin is both: a driver on its line and an input reading it back. Its capacitance is the
            // output's, which already includes the input stage on the same pad.
            if (binding.pin().output()) {
                DigitalDriver driver = new DigitalDriver(label, name, binding.isDigitalNode() ? binding.digitalNode() : null,
                        binding.pin().openDrain());
                drivers.put(name, driver);
                if (!binding.isDigitalNode()) {
                    simulation.bridges.add(new OutputBridge(driver, binding.analog(), powerNode, groundNode, family));
                    simulation.addPinCapacitance(label + "." + name + ".Cout", binding.analog(), groundNode,
                            family.outputCapacitance());
                }
            }
            if (binding.pin().reads()) {
                inputs.put(name, LogicLevel.UNKNOWN);
                if (!binding.isDigitalNode() && !binding.pin().output()) {
                    simulation.addPinCapacitance(label + "." + name + ".Cin", binding.analog(), groundNode,
                            family.inputCapacitance());
                }
            }
        });
        this.argumentsAfter = newArguments();
        this.argumentsBefore = newArguments();
        this.script = BehaviourScripts.of(model.component());
        if (script != null) {
            for (Expression statement : script.getExpressions()) {
                switch (statement) {
                    case FunctionDeclarationNode function -> {
                        // A function the language already has would win the call and leave the part's own unheard —
                        // refused here, by name, rather than failing at the first event with a message about something else.
                        if (simulation.extensions.getFunction(function.getName()) != null) {
                            throw new IllegalArgumentException(model.component().title() + ": function '" + function.getName()
                                    + "' is the language's own; give the part's another name.");
                        }
                        requireOwnName(function.getName(), "function");
                        functions.put(function.getName(), function);
                    }
                    case DeclarationNode declaration when declaration.getKind() == DeclarationNode.Kind.FINAL -> {
                        requireOwnName(declaration.getName(), "constant");
                        constants.add(declaration);
                    }
                    case HandlerNode handler -> listeners.add(listen(handler));
                    default -> throw new IllegalArgumentException(model.component().title() + ": a behaviour holds functions, "
                            + "'on' handlers and 'final let' constants; '" + statement.toSource() + "' is none of them — "
                            + "what a part remembers between events is a state.");
                }
            }
        }
    }

    /**
     * A function or constant named like a state, parameter, pin or another of the behaviour's own: an assignment writes
     * the one and an equation reads the other, and neither says so — most easily met where a base's behaviour is inherited.
     */
    private void requireOwnName(String name, String what) {
        boolean taken = discrete.containsKey(name) || model.parameters().containsKey(name) || model.texts().containsKey(name)
                || model.pins().containsKey(name) || functions.containsKey(name)
                || constants.stream().anyMatch(constant -> constant.getName().equals(name));
        if (taken) {
            throw new IllegalArgumentException(model.component().title() + ": " + what + " '" + name + "' has the name of one of "
                    + "its states, parameters, pins, functions or constants; give one of them another name.");
        }
    }

    /** A handler and what it listens to, compiled for the events that are watched or periodic. */
    private Listener listen(HandlerNode handler) {
        String argument = argumentOf(handler);
        DigitalEvent.Kind kind = DigitalEvent.Kind.of(handler.getEvent(), model.component().title());
        switch (kind) {
            case ABOVE, BELOW -> watches.add(new Watch(kind, argument, compile(argument)));
            case EVERY -> {
                double period = compile(argument).evaluate(arguments(newArguments(), new double[0], 0));
                if (!(period > 0)) {
                    throw new IllegalArgumentException(model.component().title() + ": 'every " + argument + "' is not a positive period.");
                }
                periodics.add(new Periodic(argument, period, 0));
            }
            case START, RISING, FALLING, CHANGE, TIMER -> {
            }
        }
        return new Listener(handler, kind, argument, Set.copyOf(Arrays.asList(argument.split("[,\\s]+"))));
    }

    /** One handler: the event it answers, its argument as written, and — for an edge or a change — the inputs it names. */
    private record Listener(HandlerNode handler, DigitalEvent.Kind kind, String argument, Set<String> inputs) {

        boolean answers(DigitalEvent event) {
            if (kind != event.kind()) {
                return false;
            }
            return switch (kind) {
                case START -> true;
                case RISING, FALLING, CHANGE -> inputs.contains(event.subject());
                case ABOVE, BELOW, EVERY, TIMER -> argument.equals(event.subject());
            };
        }
    }

    /**
     * Where a unit stands, kept apart from it: its discrete states, its drivers, its inputs, when each changed, its
     * timers, the sign of each watched expression and when each periodic event is next due.
     */
    record Snapshot(String label, Map<String, Object> discrete, Map<String, DigitalDriver.Snapshot> drivers,
                    Map<String, LogicLevel> inputs, Map<String, Double> lastChange, Map<String, Double> timers,
                    double[] watchSigns, double[] periodicNext) {
    }

    Snapshot snapshot() {
        Map<String, DigitalDriver.Snapshot> driven = new LinkedHashMap<>();
        drivers.forEach((name, driver) -> driven.put(name, driver.snapshot()));
        return new Snapshot(label, EventSimulation.copy(discrete), driven, new LinkedHashMap<>(inputs),
                new HashMap<>(lastChange), new LinkedHashMap<>(timers),
                watches.stream().mapToDouble(watch -> watch.sign).toArray(),
                periodics.stream().mapToDouble(periodic -> periodic.next).toArray());
    }

    /** Takes over where the same unit stood — in an earlier build of the circuit, or earlier in this run. */
    void continueFrom(Snapshot earlier) {
        if (!label.equals(earlier.label()) || !drivers.keySet().equals(earlier.drivers().keySet())
                || watches.size() != earlier.watchSigns().length || periodics.size() != earlier.periodicNext().length) {
            throw new IllegalArgumentException("The circuit's parts changed; it starts again.");
        }
        discrete.clear();
        discrete.putAll(EventSimulation.copy(earlier.discrete()));
        drivers.forEach((name, driver) -> driver.continueFrom(earlier.drivers().get(name)));
        inputs.clear();
        inputs.putAll(earlier.inputs());
        lastChange.clear();
        lastChange.putAll(earlier.lastChange());
        timers.clear();
        timers.putAll(earlier.timers());
        for (int index = 0; index < watches.size(); index++) {
            watches.get(index).sign = earlier.watchSigns()[index];
        }
        for (int index = 0; index < periodics.size(); index++) {
            periodics.get(index).next = earlier.periodicNext()[index];
        }
    }

    private static int rail(Netlist.ModelUnit model, Predicate<Pin> nature) {
        return model.pins().values().stream().filter(binding -> nature.test(binding.pin()))
                .map(Netlist.PinBinding::analog).findFirst().orElse(Netlist.GROUND);
    }

    List<HandlerNode> handlersFor(DigitalEvent event) {
        List<HandlerNode> selected = new ArrayList<>();
        for (Listener listener : listeners) {
            if (listener.answers(event)) {
                selected.add(listener.handler());
            }
        }
        return selected;
    }

    /** Runs one handler over a copy of the pre-instant state; answers the states it changed. */
    Map<String, Object> runHandler(HandlerNode handler, Map<String, Object> before, double[] state) {
        DefaultEvaluationContext context = new DefaultEvaluationContext(new BasicValuesChain(), simulation.extensions,
                simulation.conversion);
        model.parameters().forEach(context::setValue);
        // A text parameter is read as its words: a message to write, a label to show.
        model.texts().forEach(context::setValue);
        Map<String, Object> working = EventSimulation.copy(before);
        working.forEach(context::setValue);
        model.pins().forEach((name, binding) -> context.setValue(name, pinValue(name, binding, state)));
        // A bus reads as one number, its first pin the least significant bit; one bit not known makes the whole unknown.
        model.component().buses().forEach((name, bits) -> context.setValue(name, busValue(bits.stream().map(context::getValue).toList())));
        model.internalNodes().forEach((name, node) -> context.setValue(name, voltage(node, state)));
        model.internalDigitalNodes().forEach((name, node) -> context.setValue(name,
                simulation.nodeLevels.getOrDefault(node, LogicLevel.UNKNOWN).scriptValue()));
        context.setValue("X", LogicLevel.UNKNOWN.scriptValue());
        context.setValue("Z", LogicLevel.RELEASED.scriptValue());
        context.setValue(EngineName.TIME.written(), simulation.now);
        context.setValue(EventSimulation.SCOPE, new SimulatorFunctions.Scope(this, state));
        DeclaredFunctions.of(functions).installInto(context);
        // The behaviour's constants, after everything they may be written over: a parameter, a text, another constant.
        constants.forEach(constant -> constant.evaluate(context));
        ScriptExecution execution = ScriptExecution.begin(label, EventSimulation.BUDGET);
        execution.installInto(context);
        try {
            handler.evaluate(context);
        } catch (ScriptBudgetExceededException exhausted) {
            // It names the part already, and is answered as a budget, not as a mistake in the document.
            throw exhausted;
        } catch (RuntimeException failure) {
            // Whatever a handler cannot go on from — a refusal, a final name written, a slot past a row — names the part.
            throw new IllegalArgumentException(label + ": " + failure.getMessage(), failure);
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

    /**
     * A bus as one value: the number its bits make, bit 0 the first; {@code Z} when every bit is released, {@code X} when
     * any bit is not a level the number can hold.
     */
    static Object busValue(List<?> levels) {
        if (levels.size() > LogicFunctions.MAXIMUM_WIDTH) {
            throw new IllegalArgumentException("A bus or a row is read as a number of at most " + LogicFunctions.MAXIMUM_WIDTH
                    + " bits, not " + levels.size() + ".");
        }
        long value = 0;
        boolean released = true;
        boolean known = true;
        for (int position = 0; position < levels.size(); position++) {
            Object level = levels.get(position);
            released &= LogicLevel.of(level) == LogicLevel.RELEASED;
            if (level instanceof Number number) {
                value |= (number.doubleValue() >= 0.5 ? 1L : 0L) << position;
            } else {
                known = false;
            }
        }
        if (released) {
            return LogicLevel.RELEASED.scriptValue();
        }
        return known ? LogicFunctions.narrowest(value) : LogicLevel.UNKNOWN.scriptValue();
    }

    /**
     * Drives one output or internal node, or a bus of them at once: a number puts its bits on the bus's pins, bit 0 on the
     * first; a row of levels puts each slot on its pin; {@code X} or {@code Z} puts that level on every pin.
     */
    void drive(String target, Object value, double delay) {
        double at = simulation.now + Math.max(delay, 0);
        DigitalDriver driver = drivers.get(target);
        if (driver != null) {
            driver.schedule(at, LogicLevel.of(value));
            return;
        }
        List<String> bits = model.component().buses().get(target);
        if (bits == null) {
            throw new IllegalArgumentException(model.component().title() + ": drive('" + target + "') names no digital output, internal node or bus.");
        }
        boolean isRow = (value != null && value.getClass().isArray()) || value instanceof List<?>;
        if (value instanceof Number number) {
            requireFits(target, number, bits.size());
        }
        Object[] row = isRow ? arrayOf(value) : null;
        if (row != null && row.length != bits.size()) {
            throw new IllegalArgumentException(model.component().title() + ": bus '" + target + "' has " + bits.size() + " bits, the row driven on it "
                    + row.length + ".");
        }
        for (int position = 0; position < bits.size(); position++) {
            DigitalDriver bit = drivers.get(bits.get(position));
            if (bit == null) {
                throw new IllegalArgumentException(model.component().title() + ": bus '" + target + "' has an input bit, " + bits.get(position)
                        + ": only a bus of outputs is driven.");
            }
            bit.schedule(at, bitLevel(value, row, position));
        }
    }

    /** A number driven onto a bus: whole, and within its width — unsigned, or negative in two's complement. */
    private void requireFits(String bus, Number number, int width) {
        double value = number.doubleValue();
        double largest = Math.pow(2, width);
        if (value != Math.rint(value) || value >= largest || value < -largest / 2) {
            throw new IllegalArgumentException(model.component().title() + ": bus '" + bus + "' has " + width
                    + " bits; " + number + " is not a whole number they hold.");
        }
    }

    /** The level one bit of a bus is driven to: a number's bit, a row's slot, or the one level the whole bus is given. */
    private static LogicLevel bitLevel(Object value, Object[] row, int position) {
        if (row != null) {
            return LogicLevel.of(row[position]);
        }
        if (value instanceof Number number) {
            return ((number.longValue() >>> position) & 1) == 1 ? LogicLevel.HIGH : LogicLevel.LOW;
        }
        return LogicLevel.of(value);
    }

    /** A bus's bits as a row, each level as a handler reads its pin. */
    Object[] busLevels(String bus, double[] state) {
        List<String> bits = model.component().buses().get(bus);
        if (bits == null) {
            throw new IllegalArgumentException(model.component().title() + ": levels('" + bus + "') names no bus.");
        }
        Object[] row = new Object[bits.size()];
        for (int position = 0; position < row.length; position++) {
            row[position] = pinValue(bits.get(position), model.pins().get(bits.get(position)), state);
        }
        return row;
    }

    /** An analog pin's voltage now, by the pin's name. */
    double analogPinVoltage(String pin, double[] state) {
        Netlist.PinBinding binding = model.pins().get(pin);
        if (binding == null || binding.pin().digital()) {
            throw new IllegalArgumentException(model.component().title() + ": pin_voltage('" + pin + "') names no analog pin.");
        }
        return voltage(binding.analog(), state);
    }

    Object pinValue(String name, Netlist.PinBinding binding, double[] state) {
        if (binding.pin().digital()) {
            // A pin that reads is its line — a bidirectional one hears what another part pulls it to —; an output alone
            // is what it drives.
            if (binding.pin().reads()) {
                return inputs.get(name).scriptValue();
            }
            return drivers.get(name).level().scriptValue();
        }
        return voltage(binding.analog(), state);
    }

    /** Digital inputs: a digital node's resolved level, or an analog node through the family thresholds. */
    List<DigitalEvent> readInputs(double[] state, boolean initial) {
        List<DigitalEvent> events = new ArrayList<>();
        Set<String> busesChanged = new HashSet<>();
        for (Map.Entry<String, LogicLevel> input : inputs.entrySet()) {
            String name = input.getKey();
            LogicLevel level = inputLevel(name, state);
            if (initial) {
                input.setValue(level);
                continue;
            }
            if (level != input.getValue()) {
                input.setValue(level);
                lastChange.put(name, simulation.now);
                events.add(new DigitalEvent(this, DigitalEvent.Kind.CHANGE, name));
                // A bit of a bus changing changes the bus: 'on change "address"' hears any of its bits, once.
                String bus = busByPin.get(name);
                if (bus != null && busesChanged.add(bus)) {
                    events.add(new DigitalEvent(this, DigitalEvent.Kind.CHANGE, bus));
                }
                if (level == LogicLevel.HIGH) {
                    events.add(new DigitalEvent(this, DigitalEvent.Kind.RISING, name));
                } else if (level == LogicLevel.LOW) {
                    events.add(new DigitalEvent(this, DigitalEvent.Kind.FALLING, name));
                }
            }
        }
        return events;
    }


    boolean inputsWouldChange(double[] state) {
        for (String name : inputs.keySet()) {
            Netlist.PinBinding binding = model.pins().get(name);
            if (binding != null && !binding.isDigitalNode() && inputLevel(name, state) != inputs.get(name)) {
                return true;
            }
        }
        return false;
    }

    private LogicLevel inputLevel(String name, double[] state) {
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
        if (!family.powers(supply)) {
            return LogicLevel.UNKNOWN;
        }
        if (value >= family.inputHigh(supply)) {
            return LogicLevel.HIGH;
        }
        if (value <= family.inputLow(supply)) {
            return LogicLevel.LOW;
        }
        LogicLevel previous = inputs.get(name);
        // Between the thresholds an input holds what it last was: what a slow edge does on a real part is not
        // promised, and holding is the reading that keeps a clean edge clean.
        return previous == null ? LogicLevel.UNKNOWN : previous;
    }

    /** What an input on a digital node reads: the node's resolved level, a released node read as unknown. */
    private LogicLevel digitalNodeLevel(String node) {
        LogicLevel level = simulation.nodeLevels.getOrDefault(node, LogicLevel.RELEASED);
        return level == LogicLevel.RELEASED ? LogicLevel.UNKNOWN : level;
    }

    void watch(double[] state, double time) {
        double[] arguments = arguments(argumentsAfter, state, time);
        for (Watch watch : watches) {
            watch.sign = Math.signum(watch.function.evaluate(arguments));
        }
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

    List<DigitalEvent> fireCrossings(double[] state, double time) {
        List<DigitalEvent> events = new ArrayList<>();
        double[] arguments = arguments(argumentsAfter, state, time);
        for (Watch watch : watches) {
            double sign = Math.signum(watch.function.evaluate(arguments));
            if (crosses(watch, sign)) {
                events.add(new DigitalEvent(this, watch.kind, watch.argument));
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
        return watch.kind == DigitalEvent.Kind.ABOVE ? watch.sign <= 0 && sign > 0 : watch.sign >= 0 && sign < 0;
    }

    List<DigitalEvent> dueTimers(double time) {
        List<DigitalEvent> events = new ArrayList<>();
        for (Map.Entry<String, Double> timer : List.copyOf(timers.entrySet())) {
            if (timer.getValue() <= time + EventSimulation.INSTANT) {
                timers.remove(timer.getKey());
                events.add(new DigitalEvent(this, DigitalEvent.Kind.TIMER, timer.getKey()));
            }
        }
        for (Periodic periodic : periodics) {
            while (periodic.next <= time + EventSimulation.INSTANT) {
                periodic.next += periodic.period;
                events.add(new DigitalEvent(this, DigitalEvent.Kind.EVERY, periodic.argument));
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
        Formula parsed = ModelLanguage.read(model.component().title(), expression,
                ModelLanguage.definitions(model.component().title(), model.component().formulas()));
        List<String> nodes = new ArrayList<>(model.pins().keySet());
        nodes.addAll(model.internalNodes().keySet());
        Formula bound = Substitution.bindHostNames(Substitution.replaceHostCalls(parsed, call -> {
            throw new IllegalArgumentException(model.component().title() + ": an event reads voltages, not the rate d(…)/dt.");
        }), name -> discrete.containsKey(name) || !nodes.contains(name) ? null : new Formula.Slot(nodes.indexOf(name), name));
        List<String> names = new ArrayList<>(discrete.keySet());
        names.add(EngineName.TIME.written());
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
            into[position++] = UnitArguments.numeric(value);
        }
        into[position] = time;
        return into;
    }

    /** How much slower its delays are at this supply than as it writes them: its family's law; 1 without a family. */
    double delayScale(double supply) {
        return family == null ? 1 : family.delayScale(supply);
    }

    private static double voltage(int node, double[] state) {
        return node == Netlist.GROUND || node >= state.length ? 0 : state[node];
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

    static final class Watch {
        final DigitalEvent.Kind kind;
        final String argument;
        final RealFunction function;
        double sign;

        Watch(DigitalEvent.Kind kind, String argument, RealFunction function) {
            this.kind = kind;
            this.argument = argument;
            this.function = function;
        }
    }

    static final class Periodic {
        final String argument;
        final double period;
        double next;

        Periodic(String argument, double period, double next) {
            this.argument = argument;
            this.period = period;
            this.next = next;
        }
    }
}
