package org.jmouse.cad.simulation;

import org.jmouse.cad.Formula;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Equation;
import org.jmouse.cad.component.Figure;
import org.jmouse.cad.component.InternalNode;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.State;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A component as the engine runs it: its lineage — the components it extends, the most general first — folded into one,
 * as a member of a base folds over it ({@link ComponentCompilation}). Every parameter is in SI; a bus is its bits, each
 * a pin of its own — {@code q[0..7]} is {@code q_0} … {@code q_7} —, and {@code buses} says which bits a bus is.
 *
 * @param units       the blocks of its package, each with its own pin numbers and states; empty for a package of one
 * @param locals      the values its equations name — {@code let V_be : voltage = …} —, in the order written
 * @param equations   its branches and derivatives: what the solver stamps
 * @param formulas    the definitions its equations may call: every library it uses, depth first, then its own
 * @param behaviours  its jMS, the most general first: a member's runs over its base's, a function it writes again
 *                    replacing the base's of that name
 * @param heat        how it warms, or null for a part that does not
 * @param parts       what it is made of, when it is a subcircuit; empty for a component its equations say all of
 * @param family      the logic family its digital pins follow, by slug, or null
 * @param lineage     the components it is made of, the most general first, itself last
 */
public record SimulatedComponent(String slug, String title, String type, List<EvaluatedParameter> parameters, List<Pin> pins,
                                 Map<String, List<String>> buses, List<String> units, List<InternalNode> nodes, List<State> states,
                                 List<Equation.Local> locals, List<Equation> equations, List<Equation> formulas,
                                 List<String> behaviours, HeatLaw heat, String family, List<Component> lineage,
                                 List<Part> parts) {

    public SimulatedComponent {
        parameters = List.copyOf(parameters);
        pins = List.copyOf(pins);
        buses = Map.copyOf(buses);
        units = List.copyOf(units);
        nodes = List.copyOf(nodes);
        states = List.copyOf(states);
        locals = List.copyOf(locals);
        equations = List.copyOf(equations);
        formulas = List.copyOf(formulas);
        behaviours = List.copyOf(behaviours);
        lineage = List.copyOf(lineage);
        parts = List.copyOf(parts);
    }

    /** What a circuit placed on another sheet as a block is a component of. */
    public static final String SHEET = "sheet";

    /**
     * A circuit placed on another sheet as a block, as a part of it is placed: a pin for each of its ports, in the order
     * the sheet writes them, and nothing to run — a bench unfolds a block into the sheet's own parts.
     */
    public static SimulatedComponent sheet(Circuit circuit) {
        List<Pin> pins = circuit.boundary().ports().stream().map(SimulatedComponent::pinOf).toList();
        return new SimulatedComponent(circuit.head().slug(), circuit.head().title(), SHEET, List.of(), pins, Map.of(),
                                      List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null,
                                      null, List.of(), List.of());
    }

    /** A port as the pin of its block: a supply or a ground by its nature, an input or an output by its direction. */
    private static Pin pinOf(SheetBoundary.Port port) {
        String direction = port.direction();
        boolean rail = POWER.equals(direction) || GROUND.equals(direction);
        boolean flowing = INPUT.equals(direction) || OUTPUT.equals(direction);
        return new Pin(port.net(), null, rail ? direction : ANALOG, flowing ? direction : null, null, List.of(), false, port.net());
    }

    /** The words a port's direction is written with, as the {@code ports} block writes them. */
    private static final String POWER  = "power";
    private static final String GROUND = "ground";
    private static final String INPUT  = "input";
    private static final String OUTPUT = "output";
    private static final String ANALOG = "analog";

    /** The blocks of its package; one unnamed block when it declares none. */
    public List<String> unitNames() {
        return units.isEmpty() ? List.of("") : units;
    }

    /**
     * Whether a pin is the whole package's rather than each block's: every pin of a package of one, and a pin written with
     * one number for all its blocks — a dual amplifier's supply.
     */
    public boolean shared(Pin pin) {
        return units.isEmpty() || pin.numbers().isEmpty() || pin.numbers().getFirst().size() <= 1;
    }

    public Optional<Pin> pin(String name) {
        return pins.stream().filter(pin -> pin.name().equals(name)).findFirst();
    }

    public Optional<EvaluatedParameter> parameter(String name) {
        return parameters.stream().filter(parameter -> parameter.name().equals(name)).findFirst();
    }

    /** Whether it says what it does itself — equations, behaviour or parts — rather than leaving it to its base. */
    public boolean behaves() {
        return !equations.isEmpty() || !locals.isEmpty() || !behaviours.isEmpty() || !parts.isEmpty();
    }

    /** Whether it is made of parts of its own: a subcircuit. */
    public boolean subcircuit() {
        return !parts.isEmpty();
    }

    /** Whether it does something itself besides its parts: equations, states or behaviour of its own. */
    public boolean actsItself() {
        return !equations.isEmpty() || !behaviours.isEmpty() || !states.isEmpty();
    }

    /**
     * What it shows on its symbol while it runs, by name, each an expression over the part: its {@code display} figures
     * and what its symbol binds each input to — both read alike, so a symbol input and an indicator are fed the same
     * way —, a member's over its base's of the same name.
     *
     * @throws IllegalArgumentException when one document both displays a name and binds a symbol input to it
     */
    public Map<String, Formula> displays() {
        Map<String, Formula> displays = new LinkedHashMap<>();
        for (Component component : lineage) {
            Map<String, Formula> own = new LinkedHashMap<>();
            component.display().forEach(figure -> {
                if (!(figure.value() instanceof Formula expression)) {
                    return;
                }
                if (figure.index() == null) {
                    own.put(figure.name(), expression);
                    return;
                }
                // A row of figures shows a row state slot by slot: cell[32] of shown is cell_0 of shown_0, and on.
                State row = states.stream().filter(state -> state.index() != null && state.name().equals(expression.written().strip()))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException(component.head().title() + ": the row of "
                                + "figures '" + figure.name() + "' shows a row state, not '" + expression.written() + "'"));
                List<String> names = figure.index().names(figure.name());
                List<String> slots = row.index().names(row.name());
                for (int index = 0; index < Math.min(names.size(), slots.size()); index++) {
                    own.put(names.get(index), new Formula(slots.get(index)));
                }
            });
            component.drawn().ifPresent(symbol -> symbol.bindings().forEach(binding -> {
                if (binding.value() instanceof Formula expression && own.putIfAbsent(binding.input(), expression) != null) {
                    throw new IllegalArgumentException(component.head().title() + ": '" + binding.input() + "' is both "
                            + "displayed and bound to a symbol input; a symbol input of that name reads the display figure "
                            + "without a binding");
                }
            }));
            displays.putAll(own);
        }
        return displays;
    }

    /** What an instrument reads of it: the readings of the last of its lineage that writes any. */
    public List<Figure> readings() {
        for (int index = lineage.size() - 1; index >= 0; index--) {
            if (!lineage.get(index).readings().isEmpty()) {
                return lineage.get(index).readings();
            }
        }
        return List.of();
    }

    /** Whether an instrument reads it — a {@code measure} of its own or of a component it is made of. */
    public boolean instrument() {
        return lineage.stream().anyMatch(component -> !component.readings().isEmpty());
    }

    /**
     * Its drive, when it is a generator: one {@code voltage} or {@code current} equation between its only two pins, no
     * state, no behaviour, no internal node, and no instrument reading it — an ideal source the engine treats as one.
     */
    public Optional<Equation.Branch> generated() {
        if (instrument() || equations.size() != 1 || pins.size() != 2 || !states.isEmpty() || !behaviours.isEmpty()
                || !nodes.isEmpty() || !(equations.getFirst() instanceof Equation.Branch branch)) {
            return Optional.empty();
        }
        List<String> names = pins.stream().map(Pin::name).toList();
        boolean between = names.contains(branch.from()) && names.contains(branch.to()) && !branch.from().equals(branch.to());
        return between ? Optional.of(branch) : Optional.empty();
    }

    public Optional<HeatLaw> warms() {
        return Optional.ofNullable(heat);
    }

    public Optional<String> logicFamily() {
        return Optional.ofNullable(family);
    }

    /** The component as written, the last of its lineage. */
    public Component written() {
        return lineage.getLast();
    }
}
