package org.jmouse.cad.simulation;

import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.Seat;
import org.jmouse.cad.document.MetaNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a set of documents says about a component a part is, by slug — what a format writing a part needs whatever the
 * format: the parameter its value is, the footprints it sits on with the pads each pin takes, and which pin a package's
 * number is. A KiCad schematic, a bill of materials and a board house's placement file all ask the same; each answer is
 * worked out once.
 */
public final class ComponentFacts {

    private final CatalogueDocuments                documents;
    private final Map<String, Optional<Component>>  components = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>>  numbers    = new ConcurrentHashMap<>();
    private final Map<String, List<String>>         parameters = new ConcurrentHashMap<>();

    public ComponentFacts(CatalogueDocuments documents) {
        this.documents = documents;
    }

    /** The component as written; empty for a slug that names none. */
    public Optional<Component> component(String slug) {
        return components.computeIfAbsent(slug, wanted -> documents.find(wanted)
                .filter(document -> document.kind().equals(SimulationModule.COMPONENT))
                .map(SimulationReading::component));
    }

    /** The component with its lineage applied; empty when it does not compile. */
    public Optional<SimulatedComponent> compiled(String slug) {
        return component(slug).flatMap(written -> {
            try {
                return Optional.of(ComponentCompilation.compile(written, documents));
            } catch (RuntimeException unreadable) {
                return Optional.empty();
            }
        });
    }

    /** The parameter the component's value is — a resistor's {@code resistance} —; empty when it names none. */
    public Optional<String> valueParameterOf(String component) {
        return component(component).flatMap(written -> written.head().field(MetaNode.VALUE));
    }

    /** The names of the component's parameters, in its order. */
    public List<String> parametersOf(String component) {
        return parameters.computeIfAbsent(component, slug -> compiled(slug)
                .map(compiled -> compiled.parameters().stream().map(EvaluatedParameter::name).toList())
                .orElse(List.of()));
    }

    /** Every footprint the component sits on, its default first, each with the pads its pins take. */
    public List<Seat> seatsOf(String component) {
        return component(component).map(Component::seats).orElse(List.of());
    }

    /** The footprint a component sits on unless its part names another: its first {@code footprint} line's. */
    public Optional<String> footprintOf(String component) {
        return seatsOf(component).stream().findFirst().map(Seat::footprint);
    }

    /**
     * The component's pin by its package's number — {@code 1} is {@code anode}; on a part of several blocks the block's
     * pin, {@code 7} is {@code B.output} —; empty when the component numbers none.
     */
    public Map<String, String> pinsByNumber(String component) {
        return numbers.computeIfAbsent(component, this::numbered);
    }

    /**
     * The net each pad of a part carries on a footprint, by pad number: through the footprint's own pad map when its
     * component maps the pads of that footprint ({@code footprint sot-23-footprint { 1 base }}), else by the package
     * numbers of its pins. A pad the circuit does not wire is left out.
     */
    public Map<String, String> padNets(Part part, Optional<String> footprint) {
        Map<String, String> byPin = netsByPin(part);
        Optional<Seat> mapped = footprint.flatMap(slug -> seatsOf(part.component()).stream()
                .filter(seat -> seat.footprint().equals(slug) && !seat.pads().isEmpty()).findFirst());
        Map<String, String> pinsOfPads = mapped.map(Seat::pads).orElseGet(() -> pinsByNumber(part.component()));
        Map<String, String> nets = new LinkedHashMap<>();
        pinsOfPads.forEach((pad, pin) -> {
            // A block's pin, B.output, is wired as such or, shared by the blocks, by its name alone.
            String net = byPin.getOrDefault(pin, byPin.get(pin.substring(pin.indexOf('.') + 1)));
            if (net != null) {
                nets.put(pad, net);
            }
        });
        return Map.copyOf(nets);
    }

    /** The net on each of a part's pins: by the pin each connection names, or in the component's pin order. */
    private Map<String, String> netsByPin(Part part) {
        Map<String, String> nets = new LinkedHashMap<>();
        if (!part.inPinOrder()) {
            part.connections().forEach(connection -> nets.put(connection.pin(), connection.net()));
            return nets;
        }
        List<Pin> pins = compiled(part.component()).map(SimulatedComponent::pins).orElse(List.of());
        for (int index = 0; index < Math.min(pins.size(), part.connections().size()); index++) {
            nets.put(pins.get(index).name(), part.connections().get(index).net());
        }
        return nets;
    }

    /** Each package number's pin: a block's pin by its block, {@code B.output}; a shared one by its name. */
    private Map<String, String> numbered(String slug) {
        Optional<SimulatedComponent> compiled = compiled(slug);
        if (compiled.isEmpty()) {
            return Map.of();
        }
        SimulatedComponent component = compiled.get();
        Map<String, String> pins = new LinkedHashMap<>();
        for (Pin pin : component.pins()) {
            if (pin.numbers().isEmpty()) {
                continue;
            }
            // A block's pin is one group, a number for each block: output 1 7 is A's 1 and B's 7.
            List<String> group = pin.numbers().getFirst();
            if (component.shared(pin)) {
                pin.numbers().forEach(numbered -> numbered.forEach(number -> pins.putIfAbsent(number, pin.name())));
                continue;
            }
            for (int unit = 0; unit < Math.min(group.size(), component.units().size()); unit++) {
                pins.putIfAbsent(group.get(unit), component.units().get(unit) + "." + pin.name());
            }
        }
        return Map.copyOf(pins);
    }
}
