package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Setting;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A project's sheets unfolded into the one circuit a bench runs, as KiCad's hierarchy is: a part whose component is
 * another circuit is that sheet placed as a block, and it becomes the sheet's own parts.
 *
 * <h3>How the nets of a placed sheet are named</h3>
 * <ul>
 *   <li>node {@value Circuit#GROUND} stays node 0 — every sheet's;</li>
 *   <li>a port (its {@code ports} block) is the net the block's pin is wired to on the parent sheet; a port left unwired
 *       is the instance's own;</li>
 *   <li>a global net ({@code nets { vcc global }}) keeps its name — one net across the project, whichever sheet names it,
 *       the top one included;</li>
 *   <li>every other net is the instance's own: {@code S1.mid} — two blocks of one sheet never share it.</li>
 * </ul>
 * <p>A part of a placed sheet is named after its block, {@code S1_R1}, as a part of a subcircuit is; one deeper,
 * {@code S1_S2_R1}. What it sets is the sheet's own expressions worked out over the sheet's parameters — their values,
 * or what the block sets ({@code S1 power-stage (VIN = vin) L = 22 µH}) —, since the parent knows none of its names.</p>
 *
 * <p>This runs before any part's component is read, so everything after — the components' subcircuits
 * ({@link ComponentHierarchy}), the bench, the engine — sees one flat circuit.</p>
 */
final class SheetHierarchy {

    /** What joins a block's reference to what is its own inside it: its parts, {@code S1_R1}; its nets, {@code S1.mid}. */
    private static final String PART_SEPARATOR = "_";

    /**
     * A block of the circuit as it was unfolded: the parts it became, so what it carries can be read off them.
     *
     * @param block   the block as the unfolded circuit names it ({@code S1}, one deeper {@code S1_S2})
     * @param pins    each of its pins — a port of its sheet — → the net it is wired to
     * @param members every part it became, however deep, by the unfolded circuit's names
     */
    record Block(String block, Map<String, String> pins, List<String> members) {

        Block {
            pins = Map.copyOf(pins);
            members = List.copyOf(members);
        }
    }

    /** The circuit with its sheets unfolded, and the blocks it unfolded — an inner block before the block that places it. */
    record Unfolded(Circuit circuit, List<Block> blocks) {

        Unfolded {
            blocks = List.copyOf(blocks);
        }
    }

    private final CatalogueDocuments catalogue;
    private final List<Part> parts = new ArrayList<>();
    private final List<Block> blocks = new ArrayList<>();
    private final Set<String> references = new HashSet<>();

    private SheetHierarchy(CatalogueDocuments catalogue, List<Part> top) {
        this.catalogue = catalogue;
        top.forEach(part -> references.add(part.reference()));
    }

    /**
     * The circuit with every placed sheet unfolded into its parts, however deep; the circuit itself, and no blocks, when it
     * places none.
     *
     * @param values the circuit's parameter values, which a block's settings are written over
     */
    static Unfolded unfold(Circuit circuit, Map<String, Double> values, CatalogueDocuments catalogue) {
        if (circuit.parts().stream().noneMatch(part -> sheetOf(part, catalogue).isPresent())) {
            return new Unfolded(circuit, List.of());
        }
        SheetHierarchy hierarchy = new SheetHierarchy(catalogue, circuit.parts());
        for (Part part : circuit.parts()) {
            hierarchy.add(part, values, new LinkedHashSet<>());
        }
        Circuit unfolded = new Circuit(circuit.head(), circuit.parameters(), hierarchy.parts, circuit.layout(), circuit.probes(),
                circuit.timeBase(), circuit.expectations(), circuit.analysis(), circuit.views(), circuit.frame(),
                circuit.boundary());
        return new Unfolded(unfolded, hierarchy.blocks);
    }

    /**
     * One placed sheet as it runs inside the circuit: the block a path of references reaches, outermost first — {@code S1},
     * then {@code S2} inside it —, its sheet as written, and what its parts and nets are called in the unfolded run.
     *
     * @throws IllegalArgumentException when a reference of the path is no block of the sheet it is looked for in
     */
    static SheetInstance placed(Circuit circuit, Map<String, Double> values, CatalogueDocuments catalogue, List<String> path) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Name the block to open: its reference, outermost first.");
        }
        Circuit current = circuit;
        Map<String, Double> currentValues = values;
        String prefix = null;
        Map<String, String> pins = Map.of();
        SheetBoundary boundary = SheetBoundary.NONE;
        Circuit sheet = null;
        for (String step : path) {
            Circuit parent = current;
            Part block = parent.part(step).orElseThrow(() -> new IllegalArgumentException("'" + step + "' is no part of "
                    + parent.head().title() + "."));
            sheet = sheetOf(block, catalogue).orElseThrow(() -> new IllegalArgumentException("'" + step + "' places no sheet: "
                    + block.component() + " is a component."));
            // The block's pins are its parent's nets as the run names them: the parent is a placed sheet itself below the top.
            Map<String, String> ownPins = pinsOf(block, sheet);
            Map<String, String> runPins = new LinkedHashMap<>();
            String parentPrefix = prefix;
            Map<String, String> parentPins = pins;
            SheetBoundary parentBoundary = boundary;
            ownPins.forEach((port, net) -> runPins.put(port, parentPrefix == null ? net
                    : net(parentPrefix, parentPins, parentBoundary, net)));
            currentValues = sheetValues(block, sheet, currentValues);
            prefix = parentPrefix == null ? step : parentPrefix + PART_SEPARATOR + step;
            pins = runPins;
            boundary = sheet.boundary();
            current = sheet;
        }
        Map<String, String> parts = new LinkedHashMap<>();
        Map<String, String> nets = new LinkedHashMap<>();
        for (Part part : sheet.parts()) {
            parts.put(part.reference(), prefix + PART_SEPARATOR + part.reference());
            for (Connection connection : part.connections()) {
                nets.putIfAbsent(connection.net(), net(prefix, pins, boundary, connection.net()));
            }
        }
        return new SheetInstance(prefix, sheet, parts, nets, currentValues);
    }

    /** The sheet a part places, when its component is a circuit the catalogue holds. */
    private static Optional<Circuit> sheetOf(Part part, CatalogueDocuments catalogue) {
        return catalogue.find(part.component()).filter(document -> document.kind().equals(SimulationModule.CIRCUIT))
                .map(SimulationReading::circuit);
    }

    /** A part as it is run: kept, or — a placed sheet — replaced by the sheet's parts. */
    private void add(Part part, Map<String, Double> values, Set<String> path) {
        Optional<Circuit> found = sheetOf(part, catalogue);
        if (found.isEmpty()) {
            parts.add(part);
            return;
        }
        Circuit sheet = found.get();
        if (!path.add(part.component())) {
            throw new IllegalArgumentException("A sheet places itself: " + String.join(" -> ", path) + " -> " + part.component());
        }
        SheetBoundary boundary = sheet.boundary();
        Map<String, String> pins = pinsOf(part, sheet);
        Map<String, Double> sheetValues = sheetValues(part, sheet, values);
        int first = parts.size();
        for (Part inner : sheet.parts()) {
            add(placedPart(part.reference(), pins, boundary, inner, sheetValues), sheetValues, path);
        }
        // Its pins are the nets its ports are wired to; what it became is everything added since, however deep.
        blocks.add(new Block(part.reference(), pins, parts.subList(first, parts.size()).stream().map(Part::reference).toList()));
        path.remove(part.component());
    }

    /** Each port of the sheet a block wires — by name, or in the order the sheet writes its ports — → the parent's net. */
    private static Map<String, String> pinsOf(Part block, Circuit sheet) {
        List<SheetBoundary.Port> ports = sheet.boundary().ports();
        Map<String, String> pins = new LinkedHashMap<>();
        List<Connection> connections = block.connections();
        for (int index = 0; index < connections.size(); index++) {
            Connection connection = connections.get(index);
            int position = index;
            String port = connection.named().orElseGet(() -> position < ports.size() ? ports.get(position).net() : null);
            if (port == null || ports.stream().noneMatch(written -> written.net().equals(port))) {
                throw new IllegalArgumentException("Block '" + block.reference() + "': sheet '" + block.component()
                        + "' has no port '" + (port == null ? "#" + (index + 1) : port) + "'" + portsNamed(sheet.boundary()) + ".");
            }
            pins.put(port, connection.net());
        }
        return pins;
    }

    /** One part of a placed sheet, as the circuit it is unfolded into runs it. */
    private Part placedPart(String block, Map<String, String> pins, SheetBoundary boundary, Part part,
                            Map<String, Double> sheetValues) {
        String reference = uniqueReference(block + PART_SEPARATOR + part.reference());
        List<Connection> connections = part.connections().stream()
                .map(connection -> new Connection(connection.pin(), net(block, pins, boundary, connection.net()))).toList();
        List<Setting> settings = part.settings().stream().map(setting -> worked(reference, setting, sheetValues)).toList();
        return new Part(reference, part.component(), connections, settings, part.symbol(), part.footprint());
    }

    /** A setting worked out over the sheet's values: a number the parent can read; a text as it is. */
    private static Setting worked(String reference, Setting setting, Map<String, Double> sheetValues) {
        if (!(setting.value() instanceof Formula formula)) {
            return setting;
        }
        double value = ParameterEvaluation.value(reference + ": its " + setting.name(), formula, sheetValues);
        return new Setting(setting.name(), new Formula(Double.toString(value)));
    }

    /** What a net of a placed sheet is called in the circuit it is unfolded into. */
    private static String net(String block, Map<String, String> pins, SheetBoundary boundary, String net) {
        if (net == null || net.equals(Circuit.GROUND)
                || boundary.nets().stream().anyMatch(written -> written.global() && written.name().equals(net))) {
            return net;
        }
        if (pins.containsKey(net)) {
            return pins.get(net);
        }
        return block + ElaboratedCircuit.UNIT_SEPARATOR + net;
    }

    /** The sheet's parameters: their own values, or what the block sets, worked out over the parent's values. */
    private static Map<String, Double> sheetValues(Part block, Circuit sheet, Map<String, Double> parentValues) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (EvaluatedParameter parameter : ParameterEvaluation.evaluate(sheet.head().title(), sheet.parameters())) {
            values.put(parameter.name(), parameter.value());
        }
        for (Setting setting : block.settings()) {
            if (!values.containsKey(setting.name())) {
                throw new IllegalArgumentException("Block '" + block.reference() + "': sheet '" + block.component()
                        + "' has no parameter '" + setting.name() + "'"
                        + (values.isEmpty() ? "; it has none" : "; it has " + String.join(", ", values.keySet())) + ".");
            }
            if (!(setting.value() instanceof Formula formula)) {
                throw new IllegalArgumentException("Block '" + block.reference() + "': its " + setting.name() + " is a number, "
                        + "not a text");
            }
            values.put(setting.name(), ParameterEvaluation.value("Block '" + block.reference() + "': its " + setting.name(),
                    formula, parentValues));
        }
        return values;
    }

    private String uniqueReference(String wanted) {
        String candidate = wanted;
        for (int suffix = 2; !references.add(candidate); suffix++) {
            candidate = wanted + PART_SEPARATOR + suffix;
        }
        return candidate;
    }

    private static String portsNamed(SheetBoundary boundary) {
        if (boundary.ports().isEmpty()) {
            return "; it hands none to its parent — write them in its ports block";
        }
        return "; its ports are " + String.join(", ", boundary.ports().stream().map(SheetBoundary.Port::net).toList());
    }
}
