package org.jmouse.parallax.circuit;


import org.jmouse.el.evaluation.ExpressionLanguageConversion;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.script.el.ScriptExtension;
import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.el.node.HandlerNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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

    static final String  SCOPE   = "__simulator";
    private static final int     DELTAS  = 10_000;
    static final double  INSTANT = 1e-15;

    static final ScriptBudget BUDGET = ScriptBudget.builder().steps(200_000).loopIterations(100_000).recursionDepth(64).build();

    private final List<DigitalUnit> units = new ArrayList<>();
    /** The same units by label: every model device asks for its own, and four hundred of them searched the list. */
    private final Map<String, DigitalUnit> unitsByLabel = new LinkedHashMap<>();
    private final Map<String, List<DigitalDriver>> driversByNode = new LinkedHashMap<>();
    final Map<String, LogicLevel> nodeLevels = new LinkedHashMap<>();
    /** Counts every change of a node's level: a reader copies the levels again only when it moved. */
    private long levelChanges;
    final List<OutputBridge> bridges = new ArrayList<>();
    private final List<Netlist.Element> pinCapacitances = new ArrayList<>();
    final StandardExtensionContainer extensions = new StandardExtensionContainer();
    /** One conversion for every handler run: building it registers every converter, which cost more than the handler. */
    final ExpressionLanguageConversion conversion = new ExpressionLanguageConversion();
    double now;

    EventSimulation(Netlist netlist) {
        extensions.importExtension(new ScriptExtension());
        extensions.importExtension(new SimulatorFunctions.Functions());
        extensions.importExtension(new LogicFunctions());
        netlist.digitalNodes().forEach(node -> {
            driversByNode.put(node, new ArrayList<>());
            nodeLevels.put(node, LogicLevel.RELEASED);
        });
        for (Netlist.ModelUnit modelUnit : netlist.units()) {
            DigitalUnit unit = new DigitalUnit(modelUnit, this);
            units.add(unit);
            unitsByLabel.put(unit.label, unit);
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
        DigitalUnit unit = unitsByLabel.get(label);
        if (unit == null) {
            throw new NoSuchElementException("No unit '" + label + "' in the event simulation.");
        }
        return unit.discrete;
    }

    /** The devices that put digital outputs onto analog nodes. */
    List<OutputBridge> bridges() {
        return bridges;
    }

    /**
     * Every digital pin's capacitance on an analog node, input and output alike, as an ordinary solver capacitor — one
     * integration, one step control, for a pin as for any capacitor in the circuit.
     */
    List<Netlist.Element> pinCapacitances() {
        return pinCapacitances;
    }

    void addPinCapacitance(String id, int node, int ground, double capacitance) {
        if (capacitance > 0) {
            pinCapacitances.add(new Netlist.Element(id, Netlist.Element.Kind.CAPACITOR, node, ground, capacitance));
        }
    }

    /** t = 0: inputs read, {@code start} handlers run, everything they drive at once applied. */
    void start(double[] state) {
        now = 0;
        for (DigitalUnit unit : units) {
            unit.watch(state, 0);
            unit.readInputs(state, true);
        }
        List<DigitalEvent> events = new ArrayList<>();
        for (DigitalUnit unit : units) {
            events.add(new DigitalEvent(unit, DigitalEvent.Kind.START, null));
        }
        settle(state, events);
    }

    /** The discrete half where it stands, kept apart from it: every unit's, the node levels and the clock. */
    record Snapshot(List<DigitalUnit.Snapshot> units, Map<String, LogicLevel> nodeLevels, double now) {
    }

    Snapshot snapshot() {
        return new Snapshot(units.stream().map(DigitalUnit::snapshot).toList(), new LinkedHashMap<>(nodeLevels), now);
    }

    /**
     * Goes on from where it stood: the same circuit with other values — a wiper moved, a switch closed —, or the same
     * circuit earlier in its run, keeps every discrete thing it had: each unit's states, its drivers with their pending
     * transactions, its inputs, timers and periods, the sign of each watched expression, the node levels and the clock.
     *
     * @throws IllegalArgumentException when the two do not have the same units, drivers and handlers
     */
    void continueFrom(Snapshot previous) {
        if (units.size() != previous.units().size()) {
            throw new IllegalArgumentException("The circuit's parts changed; it starts again.");
        }
        for (int index = 0; index < units.size(); index++) {
            units.get(index).continueFrom(previous.units().get(index));
        }
        nodeLevels.clear();
        nodeLevels.putAll(previous.nodeLevels());
        levelChanges++;
        now = previous.now();
    }

    /** The next instant something discrete is due, after {@code time}; infinity when nothing is. */
    double nextTime(double time) {
        double next = Double.POSITIVE_INFINITY;
        for (DigitalUnit unit : units) {
            for (DigitalDriver driver : unit.drivers.values()) {
                Double due = driver.dueAfter(time + INSTANT);
                if (due != null) {
                    next = Math.min(next, due);
                }
                if (driver.changedAt() + unit.edge > time + INSTANT && unit.edge > 0) {
                    next = Math.min(next, driver.changedAt() + unit.edge);
                }
            }
            for (double due : unit.timers.values()) {
                if (due > time + INSTANT) {
                    next = Math.min(next, due);
                }
            }
            for (DigitalUnit.Periodic periodic : unit.periodics) {
                next = Math.min(next, periodic.next);
            }
        }
        return next;
    }

    /**
     * Where in a step from {@code before} to {@code after} something discrete first happens, as a share of the step:
     * for a watched expression, the root of the straight line between its values at the two ends (regula falsi);
     * for a digital input crossing its threshold, the middle. 1 when nothing happens in the step.
     */
    double crossingShare(double[] before, double timeBefore, double[] after, double timeAfter) {
        double share = 1;
        for (DigitalUnit unit : units) {
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
        List<DigitalEvent> events = new ArrayList<>();
        for (DigitalUnit unit : units) {
            events.addAll(unit.fireCrossings(state, time));
            events.addAll(unit.readInputs(state, false));
        }
        return settle(state, events);
    }

    /** The level of every digital node, for the datasets. */
    Map<String, LogicLevel> levels() {
        return nodeLevels;
    }

    /** How many times a node's level has changed: the same count means the same levels. */
    long levelChanges() {
        return levelChanges;
    }

    // ── one instant ──────────────────────────────────────────────────────────────────────────────────────────────

    private boolean settle(double[] state, List<DigitalEvent> initial) {
        boolean changed = false;
        List<DigitalEvent> events = new ArrayList<>(initial);
        for (int delta = 0; delta < DELTAS; delta++) {
            events.addAll(applyDue(state));
            for (DigitalUnit unit : units) {
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
    private List<DigitalEvent> applyDue(double[] state) {
        List<DigitalEvent> events = new ArrayList<>();
        List<String> touchedNodes = new ArrayList<>();
        for (DigitalUnit unit : units) {
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
            if (nodeLevels.put(node, resolve(driversByNode.get(node))) != nodeLevels.get(node)) {
                levelChanges++;
            }
        }
        for (DigitalUnit unit : units) {
            events.addAll(unit.readInputs(state, false));
        }
        return events;
    }

    /** A node's level from its drivers: the one level they agree on, unknown where two disagree, released where none drives. */
    private static LogicLevel resolve(List<DigitalDriver> drivers) {
        LogicLevel level = LogicLevel.RELEASED;
        for (DigitalDriver driver : drivers) {
            if (driver.level() == LogicLevel.RELEASED) {
                continue;
            }
            if (level == LogicLevel.RELEASED) {
                level = driver.level();
            } else if (level != driver.level()) {
                return LogicLevel.UNKNOWN;
            }
        }
        return level;
    }

    /** Runs every handler the events select, each over a copy of the state before the instant; writes land after. */
    private boolean run(double[] state, List<DigitalEvent> events) {
        Map<DigitalUnit, Map<String, Object>> before = new LinkedHashMap<>();
        for (DigitalEvent event : events) {
            before.computeIfAbsent(event.unit(), unit -> copy(unit.discrete));
        }
        Map<DigitalUnit, Map<String, Object>> writes = new LinkedHashMap<>();
        boolean changed = false;
        for (DigitalEvent event : events) {
            DigitalUnit unit = event.unit();
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
        for (Map.Entry<DigitalUnit, Map<String, Object>> entry : writes.entrySet()) {
            entry.getKey().discrete.putAll(entry.getValue());
            changed = true;
        }
        for (DigitalUnit unit : units) {
            changed |= unit.drivers.values().stream().anyMatch(driver -> driver.hasPending() || driver.changedAt() == now);
        }
        return changed;
    }

    static Map<String, Object> copy(Map<String, Object> state) {
        Map<String, Object> copy = new LinkedHashMap<>();
        state.forEach((name, value) -> copy.put(name, value instanceof Object[] array ? array.clone() : value));
        return copy;
    }
}
