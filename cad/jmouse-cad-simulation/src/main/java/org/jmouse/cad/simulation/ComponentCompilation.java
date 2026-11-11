package org.jmouse.cad.simulation;

import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Equation;
import org.jmouse.cad.component.Family;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.State;
import org.jmouse.cad.document.DocumentNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the {@link SimulatedComponent} a component is run as: its lineage resolved through the catalogue, then folded
 * from the most general down — each member over its base, as {@link #over} says —, and a part that warms and writes no
 * law of its own given the catalogue's shared one.
 */
public final class ComponentCompilation {

    /** The catalogue's library whose heat law a part warms by when it writes, uses and inherits none. */
    public static final String SHARED_HEAT = "heat";

    /** The parameter that says a part warms at all: kelvin above the bench per watt. */
    public static final String THERMAL_RESISTANCE = "thermal_resistance";

    private ComponentCompilation() {
    }

    public static SimulatedComponent compile(Component component, CatalogueDocuments catalogue) {
        List<Component> lineage = ComponentLineage.of(component, catalogue);
        SimulatedComponent folded = null;
        for (int index = 0; index < lineage.size(); index++) {
            SimulatedComponent own = own(lineage.get(index), catalogue);
            folded = folded == null ? own : over(own, folded, lineage.subList(0, index + 1));
        }
        if (folded.heat() == null && folded.parameter(THERMAL_RESISTANCE).isPresent()) {
            folded = withHeat(folded, sharedHeat(folded, catalogue));
        }
        return folded;
    }

    /** A logic family as its parts run on it: its parameters in SI, its definitions after those of the libraries it uses. */
    public static SimulatedFamily family(Family family, CatalogueDocuments catalogue) {
        List<Equation> formulas = new ArrayList<>();
        used(family.uses(), catalogue, new HashSet<>(), formulas);
        formulas.addAll(family.formulas());
        String title = family.head().title();
        return new SimulatedFamily(family.head().slug(), title, ParameterEvaluation.evaluate(title, family.parameters()), formulas);
    }

    /** A component by itself, as though it extended nothing. */
    private static SimulatedComponent own(Component component, CatalogueDocuments catalogue) {
        String title = component.head().title();
        Map<String, List<String>> buses = new LinkedHashMap<>();
        List<Pin> pins = new ArrayList<>();
        for (Pin pin : component.pins()) {
            if (pin.index() == null) {
                pins.add(pin);
                continue;
            }
            buses.put(pin.name(), pin.names());
            pins.addAll(bits(component, pin));
        }
        List<Equation.Local> locals = new ArrayList<>();
        List<Equation> equations = new ArrayList<>();
        for (Equation equation : component.equations()) {
            switch (equation) {
                case Equation.Local local -> locals.add(local);
                case Equation.Branch branch -> equations.add(branch);
                case Equation.Derivative derivative -> equations.add(derivative);
                case Equation.Defined defined -> equations.add(derivative(title, defined));
            }
        }
        List<Equation> formulas = new ArrayList<>();
        used(component.uses(), catalogue, new HashSet<>(), formulas);
        formulas.addAll(component.formulas());
        return new SimulatedComponent(component.head().slug(), title, component.head().role().orElse(""),
                ParameterEvaluation.evaluate(title, component.parameters()), pins, buses, component.units(), component.nodes(),
                component.states(), locals, equations, formulas,
                component.script().map(List::of).orElse(List.of()), usedHeat(component.uses(), catalogue).orElse(null),
                usedFamily(component.uses(), catalogue).orElse(null), List.of(component), component.parts());
    }

    /** A line {@code left = right} of a component's equations: a derivative, as jMF reads its left side, or refused. */
    private static Equation.Derivative derivative(String title, Equation.Defined defined) {
        return ModelLanguage.rateOf(defined.left()).map(state -> new Equation.Derivative(state, defined.right()))
                .orElseThrow(() -> new IllegalArgumentException(title + ": '" + defined.left() + " = …' is no equation of a "
                        + "component; its equations are current a -> b = …, voltage a -> b = …, d(x)/dt = … and let name = …"));
    }

    /** A heat law's lines: its derivatives as jMF reads them, its other lines as written. */
    private static List<Equation> heatLines(List<Equation> lines) {
        return lines.stream().map(line -> line instanceof Equation.Defined defined
                ? ModelLanguage.rateOf(defined.left()).<Equation>map(state -> new Equation.Derivative(state, defined.right()))
                        .orElse(defined)
                : line).toList();
    }

    /** A bus's bits, each a pin with its group of numbers: {@code q[0..7] … 15, 1, 2, 3, 4, 5, 6, 7}. */
    private static List<Pin> bits(Component component, Pin bus) {
        List<String> names = bus.names();
        if (!bus.numbers().isEmpty() && bus.numbers().size() != names.size()) {
            throw new IllegalArgumentException(component.head().title() + ": the bus '" + bus.name() + "' has " + names.size()
                    + " bits and " + bus.numbers().size() + " groups of numbers; each bit is a group");
        }
        List<Pin> bits = new ArrayList<>();
        for (int bit = 0; bit < names.size(); bit++) {
            List<List<String>> numbers = bus.numbers().isEmpty() ? List.of() : List.of(bus.numbers().get(bit));
            bits.add(new Pin(names.get(bit), null, bus.nature(), bus.direction(), bus.drive(), numbers, bus.edge(), bus.label()));
        }
        return bits;
    }

    /**
     * A member over its base. Its parameters over the base's by name, in the base's order, its new ones after; its role,
     * pins and buses where it writes them. What it does is its own only when it says what it does itself
     * ({@link SimulatedComponent#behaves()}) — then its nodes, equations and definitions, its states over the discrete
     * ones a behaving base keeps its protocol in, its behaviour over the base's —; otherwise the base's, whole.
     */
    private static SimulatedComponent over(SimulatedComponent member, SimulatedComponent base, List<Component> lineage) {
        Map<String, EvaluatedParameter> parameters = new LinkedHashMap<>();
        base.parameters().forEach(parameter -> parameters.put(parameter.name(), parameter));
        member.parameters().forEach(parameter -> parameters.put(parameter.name(), parameter));
        boolean ownPins = !member.pins().isEmpty();
        boolean behaves = member.behaves();
        // The base's handlers, then the member's: every one of them heard.
        List<String> behaviours = new ArrayList<>(base.behaviours());
        if (behaves) {
            behaviours.addAll(member.behaviours());
        }
        return new SimulatedComponent(member.slug(), member.title(), member.type().isEmpty() ? base.type() : member.type(),
                List.copyOf(parameters.values()), ownPins ? member.pins() : base.pins(), ownPins ? member.buses() : base.buses(),
                member.units().isEmpty() ? base.units() : member.units(),
                behaves ? member.nodes() : base.nodes(), behaves ? statesOver(member, base) : base.states(),
                behaves ? member.locals() : base.locals(), behaves ? member.equations() : base.equations(),
                behaves ? member.formulas() : base.formulas(), behaviours,
                member.heat() == null ? base.heat() : member.heat(), member.family(), lineage,
                behaves ? member.parts() : base.parts());
    }

    /** A member's states over the discrete ones a behaving base keeps its protocol in, by name. */
    private static List<State> statesOver(SimulatedComponent member, SimulatedComponent base) {
        if (base.behaviours().isEmpty()) {
            return member.states();
        }
        Map<String, State> merged = new LinkedHashMap<>();
        base.states().stream().filter(state -> !state.continuous()).forEach(state -> merged.put(state.name(), state));
        member.states().forEach(state -> merged.put(state.name(), state));
        return List.copyOf(merged.values());
    }

    /** The definitions of every library of functions used, depth first, so a definition comes after those it calls. */
    private static void used(List<String> uses, CatalogueDocuments catalogue, Set<String> seen, List<Equation> into) {
        for (String slug : uses) {
            if (!seen.add(slug)) {
                continue;
            }
            functions(slug, catalogue).ifPresent(library -> {
                used(library.uses(), catalogue, seen, into);
                into.addAll(library.formulas());
            });
        }
    }

    /** The heat law of the first library of functions used that writes one. */
    private static Optional<HeatLaw> usedHeat(List<String> uses, CatalogueDocuments catalogue) {
        for (String slug : uses) {
            Optional<HeatLaw> heat = functions(slug, catalogue).filter(library -> !library.heat().isEmpty())
                    .map(library -> heatOf(library, catalogue));
            if (heat.isPresent()) {
                return heat;
            }
        }
        return Optional.empty();
    }

    /** The logic family named by a {@code use}: the one used document that is a family. */
    private static Optional<String> usedFamily(List<String> uses, CatalogueDocuments catalogue) {
        return uses.stream().filter(slug -> catalogue.find(slug).map(DocumentNode::kind)
                .filter(SimulationModule.FAMILY::equals).isPresent()).findFirst();
    }

    private static Optional<Family> functions(String slug, CatalogueDocuments catalogue) {
        return catalogue.find(slug).filter(document -> document.kind().equals(SimulationModule.FUNCTIONS))
                .map(SimulationReading::family);
    }

    private static HeatLaw heatOf(Family library, CatalogueDocuments catalogue) {
        List<Equation> formulas = new ArrayList<>();
        used(library.uses(), catalogue, new HashSet<>(), formulas);
        formulas.addAll(library.formulas());
        return new HeatLaw(library.head().slug(), heatLines(library.heat()), formulas);
    }

    private static HeatLaw sharedHeat(SimulatedComponent component, CatalogueDocuments catalogue) {
        return functions(SHARED_HEAT, catalogue).filter(library -> !library.heat().isEmpty())
                .map(library -> heatOf(library, catalogue))
                .orElseThrow(() -> new IllegalArgumentException(component.title() + " gives a " + THERMAL_RESISTANCE
                        + ", writes no heat law of its own, and the catalogue holds no shared '" + SHARED_HEAT
                        + "' law to warm it by."));
    }

    private static SimulatedComponent withHeat(SimulatedComponent component, HeatLaw heat) {
        return new SimulatedComponent(component.slug(), component.title(), component.type(), component.parameters(),
                component.pins(), component.buses(), component.units(), component.nodes(), component.states(), component.locals(),
                component.equations(), component.formulas(), component.behaviours(), heat, component.family(),
                component.lineage(), component.parts());
    }
}
