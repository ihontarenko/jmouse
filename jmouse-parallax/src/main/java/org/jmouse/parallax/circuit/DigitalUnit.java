package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ComponentModel.PinNature;
import org.jmouse.parallax.component.ComponentVocabulary;
import org.jmouse.parallax.component.ModelLanguage;
import org.jmouse.el.evaluation.BasicValuesChain;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.formula.Formula;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.Symbols;
import org.jmouse.script.el.DeclaredFunctions;
import org.jmouse.script.el.budget.ScriptExecution;
import org.jmouse.script.el.node.FunctionDeclarationNode;
import org.jmouse.script.el.node.HandlerNode;
import org.jmouse.script.el.node.ScriptNode;

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

/** A unit's discrete life: its script, states, drivers, inputs, watched expressions, periods and timers. */
final class DigitalUnit {

    private static final int DIGITAL_PIN = -1;

    final String label;
    final Netlist.ModelUnit model;
    final EventSimulation simulation;
    final Map<String, Object> discrete = new LinkedHashMap<>();
    final Map<String, DigitalDriver> drivers = new LinkedHashMap<>();
    final Map<String, LogicLevel> inputs = new LinkedHashMap<>();
    final Map<String, Double> lastChange = new HashMap<>();
    final Map<String, Double> timers = new LinkedHashMap<>();
    final List<Periodic> periodics = new ArrayList<>();
    final List<Watch> watches = new ArrayList<>();
    final Map<String, FunctionDeclarationNode> functions = new LinkedHashMap<>();
    /** Each handler's argument — the input or expression it listens to — read once, not at every event. */
    final Map<HandlerNode, String> handlerArguments = new IdentityHashMap<>();
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
        this.label = model.label();
        this.simulation = simulation;
        this.powerNode = rail(model, PinNature.POWER);
        this.groundNode = rail(model, PinNature.GROUND);
        this.family = model.family() == null ? null : LogicFamily.of(model.family());
        this.edge = family == null ? 0 : family.edge();
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
            inputs.put(name, LogicLevel.UNKNOWN);
        });
        model.pins().forEach((name, binding) -> {
            if (binding.pin().nature() != PinNature.DIGITAL) {
                return;
            }
            if (binding.pin().output()) {
                DigitalDriver driver = new DigitalDriver(label, name, binding.isDigitalNode() ? binding.digitalNode() : null);
                drivers.put(name, driver);
                if (!binding.isDigitalNode()) {
                    simulation.bridges.add(new OutputBridge(driver, binding.analog(), powerNode, groundNode, family));
                    simulation.addPinCapacitance(label + "." + name + ".Cout", binding.analog(), groundNode,
                            family.outputCapacitance());
                }
            } else {
                inputs.put(name, LogicLevel.UNKNOWN);
                if (!binding.isDigitalNode()) {
                    simulation.addPinCapacitance(label + "." + name + ".Cin", binding.analog(), groundNode,
                            family.inputCapacitance());
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
                DigitalEvent.Kind kind = DigitalEvent.Kind.of(handler.getEvent(), model.model().name());
                switch (kind) {
                    case ABOVE, BELOW -> watches.add(new Watch(kind, argument, compile(argument)));
                    case EVERY -> {
                        double period = compile(argument).evaluate(arguments(newArguments(), new double[0], 0));
                        if (!(period > 0)) {
                            throw new IllegalArgumentException(model.model().name() + ": 'every " + argument + "' is not a positive period.");
                        }
                        periodics.add(new Periodic(argument, period, 0));
                    }
                    case START, RISING, FALLING, CHANGE, TIMER -> {
                    }
                }
            }
        }
    }

    /** Takes over the discrete life of the same unit in an earlier build of the circuit. */
    void continueFrom(DigitalUnit earlier) {
        if (!label.equals(earlier.label) || !drivers.keySet().equals(earlier.drivers.keySet())
                || watches.size() != earlier.watches.size() || periodics.size() != earlier.periodics.size()) {
            throw new IllegalArgumentException("The circuit's parts changed; it starts again.");
        }
        discrete.clear();
        discrete.putAll(EventSimulation.copy(earlier.discrete));
        drivers.forEach((name, driver) -> {
            driver.continueFrom(earlier.drivers.get(name));
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

    List<HandlerNode> handlersFor(DigitalEvent event) {
        List<HandlerNode> selected = new ArrayList<>();
        for (HandlerNode handler : script.getHandlers()) {
            if (!handler.getEvent().equals(event.kind().keyword())) {
                continue;
            }
            String argument = handlerArguments.get(handler);
            boolean matches = switch (event.kind()) {
                case START -> true;
                case RISING, FALLING, CHANGE -> Arrays.stream(argument.split("[,\\s]+")).anyMatch(event.subject()::equals);
                case ABOVE, BELOW, EVERY, TIMER -> argument.equals(event.subject());
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
        Map<String, Object> working = EventSimulation.copy(before);
        working.forEach(context::setValue);
        model.pins().forEach((name, binding) -> context.setValue(name, pinValue(name, binding, state)));
        model.internalNodes().forEach((name, node) -> context.setValue(name, voltage(node, state)));
        model.internalDigitalNodes().forEach((name, node) -> context.setValue(name,
                simulation.nodeLevels.getOrDefault(node, LogicLevel.UNKNOWN).scriptValue()));
        context.setValue("X", LogicLevel.UNKNOWN.scriptValue());
        context.setValue("Z", LogicLevel.RELEASED.scriptValue());
        context.setValue("time", simulation.now);
        context.setValue(EventSimulation.SCOPE, new SimulatorFunctions.Scope(this, state));
        DeclaredFunctions.of(functions).installInto(context);
        ScriptExecution execution = ScriptExecution.begin(label, EventSimulation.BUDGET);
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
                return drivers.get(name).level().scriptValue();
            }
            return inputs.get(name).scriptValue();
        }
        return voltage(binding.analog(), state);
    }

    /** Digital inputs: a digital node's resolved level, or an analog node through the family thresholds. */
    List<DigitalEvent> readInputs(double[] state, boolean initial) {
        List<DigitalEvent> events = new ArrayList<>();
        for (String name : List.copyOf(inputs.keySet())) {
            LogicLevel level = inputLevel(name, state);
            LogicLevel held = inputs.get(name);
            if (initial) {
                inputs.put(name, level);
                continue;
            }
            if (level != held) {
                inputs.put(name, level);
                lastChange.put(name, simulation.now);
                events.add(new DigitalEvent(this, DigitalEvent.Kind.CHANGE, name));
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
        Formula parsed = ModelLanguage.read(model.model().name(), expression, 0,
                ModelLanguage.LANGUAGE.definitions(ModelLanguage.libraryText(model.model())));
        List<String> nodes = new ArrayList<>(model.pins().keySet());
        nodes.addAll(model.internalNodes().keySet());
        Formula bound = Substitution.bindHostNames(Substitution.replaceHostCalls(parsed, call -> {
            throw new IllegalArgumentException(model.model().name() + ": an event reads voltages, not the rate d(…)/dt.");
        }), name -> discrete.containsKey(name) || !nodes.contains(name) ? null : new Formula.Slot(nodes.indexOf(name), name));
        List<String> names = new ArrayList<>(discrete.keySet());
        names.add(ComponentVocabulary.TIME);
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
