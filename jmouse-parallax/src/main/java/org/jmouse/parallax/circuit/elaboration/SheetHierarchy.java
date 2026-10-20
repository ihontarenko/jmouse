package org.jmouse.parallax.circuit.elaboration;

import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.parallax.object.ObjectDefinition.SheetBoundary;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A project's sheets unfolded into the one circuit a bench runs, as KiCad's hierarchy is: a part whose reference is
 * another circuit is that sheet placed as a block, and it becomes the sheet's own parts.
 *
 * <h3>How the nets of a placed sheet are named</h3>
 * <ul>
 *   <li>node {@value Circuit#GROUND} stays node 0 — every sheet's;</li>
 *   <li>a port (its {@code ports} block) is the net the block's pin is wired to on the parent sheet; a port left
 *       unwired is the instance's own;</li>
 *   <li>a global net ({@code nets { vcc global }}) keeps its name — one net across the project, whichever sheet names
 *       it, the top one included;</li>
 *   <li>every other net is the instance's own: {@code S1.mid} — two blocks of one sheet never share it.</li>
 * </ul>
 * <p>A part of a placed sheet is named after its block, {@code S1_R1}, as a part of a subcircuit is; one deeper,
 * {@code S1_S2_R1}. Its values are the sheet's own expressions worked out over the sheet's parameters — their defaults,
 * or what the block sets ({@code S1 power-stage (VIN = vin) L = 22 µH}) —, since the parent knows none of its names.</p>
 *
 * <p>This runs where a circuit is compiled, before anything reads its parts, so everything after — the components'
 * subcircuits ({@link ComponentHierarchy}), the bench conditions, the engine — sees one flat circuit.</p>
 */
public final class SheetHierarchy {

    private SheetHierarchy() {
    }

    /** Whether a jMX text is a circuit — a sheet a part may place — rather than a component. */
    public static boolean isSheetSource(String source) {
        for (String line : source.split("\\R")) {
            String stripped = line.strip();
            if (stripped.isEmpty() || stripped.startsWith("#")) {
                continue;
            }
            return stripped.startsWith("circuit ") || stripped.startsWith("circuit\t");
        }
        return false;
    }

    /** The sheet a part places, when its reference names a circuit the library holds. */
    public static Optional<String> sheetSource(CircuitElement element, ComponentLibrary library) {
        if (element.type() != CircuitElementType.MODEL) {
            return Optional.empty();
        }
        return library.source(element.component()).filter(SheetHierarchy::isSheetSource);
    }

    /**
     * A block of the circuit as it was unfolded: the parts it became, so what it carries can be read off them — its
     * pins carry what crosses its edge, as a part made of a subcircuit does ({@link ComponentHierarchy.Assembly}).
     *
     * @param block   the block as the unfolded circuit names it ({@code S1}, one deeper {@code S1_S2})
     * @param pins    each of its pins — a port of its sheet — → the net it is wired to
     * @param members every part it became, however deep, by the unfolded circuit's names
     */
    public record UnfoldedBlock(String block, Map<String, String> pins, List<String> members) {
        public UnfoldedBlock {
            pins = Map.copyOf(pins);
            members = List.copyOf(members);
        }
    }

    /** The circuit with its sheets unfolded, and the blocks it unfolded — an inner block before the block that places it. */
    public record Unfolded(Circuit circuit, List<UnfoldedBlock> blocks) {
        public Unfolded {
            blocks = List.copyOf(blocks);
        }
    }

    /**
     * The circuit with every placed sheet unfolded into its parts, however deep; the circuit itself, and no blocks, when
     * it places none.
     *
     * @param values the circuit's parameter values, which a block's settings are written over
     */
    public static Unfolded unfold(Circuit circuit, Map<String, Double> values, ComponentLibrary library) {
        if (circuit.elements().stream().noneMatch(element -> sheetSource(element, library).isPresent())) {
            return new Unfolded(circuit, List.of());
        }
        Unfolding unfolding = new Unfolding(library, circuit.elements());
        for (CircuitElement element : circuit.elements()) {
            unfolding.add(element, values, new LinkedHashSet<>());
        }
        return new Unfolded(circuit.withElements(unfolding.elements), unfolding.blocks);
    }

    /**
     * A sheet as one of its instances is run, to be drawn on the bench of the circuit that places it: its parts named
     * as the unfolded circuit names them ({@code S1_R1}), its nets too ({@code S1.mid}; a port as the parent's net it is
     * wired to), its values worked out, and its layout renamed to match — so what a live run reports reaches its parts.
     *
     * @param slug  the sheet's own address — where it is edited
     * @param title the sheet's title
     * @param block the block that places this instance, as the unfolded circuit names it ({@code S1_S2})
     */
    public record PlacedSheet(String slug, String title, String block, Circuit circuit) {
    }

    /**
     * The sheet a path of blocks reaches from the top circuit — {@code [S1]} is the sheet block S1 places, {@code [S1,
     * S2]} the one S2 places inside it — as that instance is run.
     *
     * @throws IllegalArgumentException when a block on the path is not on its sheet, or places no sheet
     */
    public static PlacedSheet placed(Circuit top, Map<String, Double> values, ComponentLibrary library, List<String> path) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Name the block whose sheet to show.");
        }
        Circuit current = top;
        Map<String, Double> currentValues = values;
        Unfolding unfolding = new Unfolding(library, top.elements());
        String prefix = null;
        PlacedSheet placed = null;
        Set<String> visited = new LinkedHashSet<>();
        for (String step : path) {
            String id = prefix == null ? step : prefix + "_" + step;
            CircuitElement block = current.elements().stream().filter(element -> element.id().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("There is no block '" + step + "' on this sheet."));
            String source = sheetSource(block, library)
                    .orElseThrow(() -> new IllegalArgumentException("'" + step + "' is a part, not a sheet."));
            if (!visited.add(block.component())) {
                throw new IllegalArgumentException("A sheet places itself: " + String.join(" -> ", visited) + " -> " + block.component());
            }
            DefinedObject sheet = ExperienceDefinitions.read(source, block.component());
            if (!(sheet.definition() instanceof Circuit circuit)) {
                throw new IllegalArgumentException("'" + block.component() + "' is not a circuit.");
            }
            currentValues = Unfolding.sheetValues(block, sheet.parameters(), currentValues, block.component());
            current = unfolding.instance(block, circuit, currentValues);
            prefix = block.id();
            placed = new PlacedSheet(block.component(), sheet.title(), block.id(), current);
        }
        return placed;
    }

    private static final class Unfolding {

        private final ComponentLibrary library;
        private final List<CircuitElement> elements = new ArrayList<>();
        private final List<UnfoldedBlock> blocks = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();

        Unfolding(ComponentLibrary library, List<CircuitElement> top) {
            this.library = library;
            top.forEach(element -> ids.add(element.id()));
        }

        /** A part as it is run: kept, or — a placed sheet — replaced by the sheet's parts. */
        void add(CircuitElement element, Map<String, Double> values, Set<String> path) {
            Optional<String> source = sheetSource(element, library);
            if (source.isEmpty()) {
                elements.add(element);
                return;
            }
            String reference = element.component();
            if (!path.add(reference)) {
                throw new IllegalArgumentException("A sheet places itself: " + String.join(" -> ", path) + " -> " + reference);
            }
            DefinedObject sheet = ExperienceDefinitions.read(source.get(), reference);
            if (!(sheet.definition() instanceof Circuit placed)) {
                throw new IllegalArgumentException("'" + reference + "' is not a circuit.");
            }
            SheetBoundary boundary = placed.boundary();
            element.pins().keySet().forEach(port -> {
                if (!boundary.isPort(port)) {
                    throw new IllegalArgumentException("Block '" + element.id() + "': sheet '" + reference + "' has no port '"
                            + port + "'" + portsNamed(boundary) + ".");
                }
            });
            Map<String, Double> sheetValues = sheetValues(element, sheet.parameters(), values, reference);
            int first = elements.size();
            for (CircuitElement part : placed.elements()) {
                add(placedPart(element, boundary, part, sheetValues), sheetValues, path);
            }
            // Its pins are the nets its ports are wired to; what it became is everything added since, however deep.
            blocks.add(new UnfoldedBlock(element.id(), element.pins(),
                    elements.subList(first, elements.size()).stream().map(CircuitElement::id).toList()));
            path.remove(reference);
        }

        /** A placed sheet as its instance is run and drawn: its parts, its marks and its wires under the run's names. */
        Circuit instance(CircuitElement block, Circuit sheet, Map<String, Double> sheetValues) {
            SheetBoundary boundary = sheet.boundary();
            List<CircuitElement> parts = sheet.elements().stream()
                    .map(part -> placedPart(block, boundary, part, sheetValues)).toList();
            ObjectDefinition.SchematicLayout layout = sheet.layout() == null ? null : new ObjectDefinition.SchematicLayout(
                    sheet.layout().items().stream().map(item -> new ObjectDefinition.PlacedItem(block.id() + "_" + item.id(),
                            item.mark(), item.net() == null ? null : net(block, boundary, item.net()), item.x(), item.y(),
                            item.rotation(), item.mirrored(), item.labels(), item.shows())).toList(),
                    sheet.layout().wires().stream().map(wire -> new ObjectDefinition.SchematicWire(block.id() + "_" + wire.id(),
                            terminal(block, wire.from()), terminal(block, wire.to()), wire.bends())).toList());
            return new Circuit(parts, sheet.time(), layout, null, List.of(), List.of(), null, boundary);
        }

        /** A wire's end on a placed sheet: {@code R1.positive} is {@code S1_R1.positive}. */
        private static String terminal(CircuitElement block, String written) {
            return block.id() + "_" + written;
        }

        /** One part of a placed sheet, as the circuit it is unfolded into runs it. */
        private CircuitElement placedPart(CircuitElement block, SheetBoundary boundary, CircuitElement part,
                                          Map<String, Double> sheetValues) {
            String id = uniqueId(block.id() + "_" + part.id());
            Map<String, String> pins = new LinkedHashMap<>();
            part.pins().forEach((pin, net) -> pins.put(pin, net(block, boundary, net)));
            Map<String, String> overrides = new LinkedHashMap<>();
            part.overrides().forEach((name, expression) -> overrides.put(name, worked(expression, sheetValues)));
            if (part.type() == CircuitElementType.MODEL) {
                // A model's settings are worked out here; a block it is stays a block, its settings the sheet's numbers.
                return new CircuitElement(id, CircuitElementType.MODEL, null, null, part.value(), part.component(), pins,
                        overrides, part.symbol());
            }
            return new CircuitElement(id, part.type(), net(block, boundary, part.positive()), net(block, boundary, part.negative()),
                    worked(part.value(), sheetValues), part.component(), pins, overrides, part.symbol());
        }

        /** What a net of a placed sheet is called in the circuit it is unfolded into. */
        private static String net(CircuitElement block, SheetBoundary boundary, String net) {
            if (net == null || net.equals(Circuit.GROUND) || boundary.globals().contains(net)) {
                return net;
            }
            if (boundary.isPort(net) && block.pins().containsKey(net)) {
                return block.pins().get(net);
            }
            return block.id() + "." + net;
        }

        /** The sheet's parameters: their defaults, or what the block sets, worked out over the parent's values. */
        private static Map<String, Double> sheetValues(CircuitElement block, List<ParameterDefinition> parameters,
                                                       Map<String, Double> parentValues, String reference) {
            Map<String, Double> values = new LinkedHashMap<>();
            parameters.forEach(parameter -> values.put(parameter.name(), parameter.defaultValue()));
            block.overrides().forEach((name, expression) -> {
                if (!values.containsKey(name)) {
                    throw new IllegalArgumentException("Block '" + block.id() + "': sheet '" + reference + "' has no parameter '"
                            + name + "'" + (values.isEmpty() ? "; it has none" : "; it has " + String.join(", ", values.keySet())) + ".");
                }
                values.put(name, ExpressionValues.valueOf(expression, parentValues));
            });
            return values;
        }

        private static String worked(String expression, Map<String, Double> values) {
            return Double.toString(ExpressionValues.valueOf(expression, values));
        }

        private String uniqueId(String wanted) {
            String candidate = wanted;
            for (int suffix = 2; !ids.add(candidate); suffix++) {
                candidate = wanted + "_" + suffix;
            }
            return candidate;
        }

        private static String portsNamed(SheetBoundary boundary) {
            if (boundary.ports().isEmpty()) {
                return "; it hands none to its parent — write them in its ports block";
            }
            return "; its ports are " + String.join(", ", boundary.ports().stream().map(port -> port.net()).toList());
        }
    }
}
