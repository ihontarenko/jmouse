package org.jmouse.cad.altium;

import org.jmouse.cad.exchange.imported.NamedFootprint;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.altium.AltiumSchematic;
import org.jmouse.altium.AltiumSchematic.Component;
import org.jmouse.altium.AltiumSchematic.Point;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.exchange.imported.ImportedBuses;
import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.jmouse.cad.exchange.imported.ImportedCircuitDocument;
import org.jmouse.cad.exchange.imported.ImportedNetNames;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.imported.SheetMarks;
import org.jmouse.cad.exchange.imported.SheetTracing;
import org.jmouse.cad.exchange.imported.SymbolCanvas;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.cad.simulation.SimulationVocabulary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * An Altium schematic sheet as jM documents: a symbol for each part of one piece, a component for each library part, and
 * the sheet's circuit — its parts where Altium places them, its power ports and labels as marks, its wires traced between
 * the terminals they reach ({@link SheetTracing}).
 *
 * <p>Altium keeps no list of nets: a net is what its wires join, named by a power port or a label on it, and every
 * port and label of one name is one net — so are the hidden power pins of that name. A net nothing names is called
 * after a pin on it. A part placed as several pieces is our box where its first piece stands, each piece's wired pins
 * labels of their nets.</p>
 */
public final class AltiumSchematicImport {

    /** What a terminal is, for a net: a part's pin it names after, or a mark that names it. */
    private record Terminal(String name, Optional<String> net) {
    }

    private final AltiumSchematic sheet;
    private final AltiumLibraries filing;
    /** How the sheet's marks are drawn: where each one's pin stands. */
    private final SheetMarks marks;
    private final ImportedNetNames nets;
    private final UniqueIdentifiers ids = new UniqueIdentifiers(ImportedCircuit.ID_OPENING, ImportedCircuit.ID_LONGEST);

    private final Map<String, DocumentNode> symbols = new LinkedHashMap<>();
    private final Map<String, DocumentNode> components = new LinkedHashMap<>();
    private final List<ImportedCircuit.Item> items = new ArrayList<>();
    private final List<ImportedCircuit.Wire> wires = new ArrayList<>();
    private final List<ImportedCircuit.BusLine> busLines = new ArrayList<>();
    /** The buses its bus lines declare, each once by its name. */
    private final ImportedBuses declaredBuses = new ImportedBuses();
    private final Set<String> globals = new LinkedHashSet<>();
    private final List<String> ports = new ArrayList<>();
    /** The buses the sheet hands across whole: its ports written as a bus. */
    private final List<String> busPorts = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    /** Where the import is read: what a footprint the sheet names by its bare name is matched against. */
    private final CatalogueDocuments catalogue;
    /** Each footprint name the sheet writes, matched once. */
    private final Map<String, NamedFootprint> footprints = new LinkedHashMap<>();

    /** The first terminal on each point of the sheet, by the point's key — the one a wire reaches —, and the points. */
    private final Map<String, Terminal> terminals = new LinkedHashMap<>();
    /** Every terminal, the ones sharing a point with another too. */
    private final List<Terminal> everyTerminal = new ArrayList<>();
    private final List<SheetTracing.Point> terminalPoints = new ArrayList<>();
    /** Terminals that share a point with the first one on it: joined as a wire joins them. */
    private final List<String[]> touching = new ArrayList<>();
    /** Each part's pins, by part id, each pin's terminal name. */
    private final Map<String, Map<String, String>> partPins = new LinkedHashMap<>();
    /** The nets hidden power pins join by their name, by part id and pin. */
    private final Map<String, Map<String, String>> hiddenNets = new LinkedHashMap<>();
    private final Map<String, String> partComponents = new LinkedHashMap<>();

    private double left;
    private double top;

    private AltiumSchematicImport(AltiumSchematic sheet, AltiumLibraries filing, SheetMarks marks, CatalogueDocuments catalogue) {
        this.marks = marks;
        this.catalogue = catalogue;
        this.sheet = sheet;
        this.filing = filing;
        this.nets = new ImportedNetNames(filing.ground(), UnaryOperator.identity());
    }

    /**
     * @param file  the sheet's file, which the circuit says it was imported from
     * @param title     what the circuit is called
     * @param catalogue where the import is read: Altium names a part's footprint without its library, and the footprint of
     *                  that name seen here — a PCB library imported before, the place's own — is the one meant
     */
    public static ImportedDocuments read(AltiumSchematic sheet, String file, String title, AltiumLibraries filing, SheetMarks marks,
                                         CatalogueDocuments catalogue) {
        AltiumSchematicImport reading = new AltiumSchematicImport(sheet, filing, marks, catalogue);
        reading.frame();
        reading.parts();
        reading.marks();
        reading.wire();
        reading.buses();
        // Every Altium library a part was placed from is a category of its own, under Altium's.
        return reading.documents(file, title);
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    /** Each part once: a part of several pieces gathered by its designator. */
    private void parts() {
        Map<String, List<Component>> pieces = new LinkedHashMap<>();
        for (Component component : sheet.components()) {
            String key = component.multiPart() ? component.designator() : component.designator() + "#" + component.index();
            pieces.computeIfAbsent(key, ignored -> new ArrayList<>()).add(component);
        }
        pieces.values().forEach(this::part);
        footprints.values().forEach(footprint -> footprint.note().ifPresent(notes::add));
        sheet.leftOut().forEach(what -> notes.add("Not read yet: " + what + "."));
    }

    /** The footprint a part names, matched once per name; empty for a part that names none. */
    private Optional<NamedFootprint> footprintOf(Component part) {
        return part.footprint().map(written -> footprints.computeIfAbsent(written, name -> NamedFootprint.of(name, catalogue)));
    }

    private void part(List<Component> pieces) {
        Component first = pieces.getFirst();
        String component = AltiumNames.component(first.libraryReference());
        components.computeIfAbsent(component, ignored -> AltiumComponents.documentOf(pieces, footprintOf(first), filing));
        String id = ids.of(first.designator().isBlank() ? first.libraryReference() : first.designator());
        partComponents.put(id, component);
        partPins.put(id, new LinkedHashMap<>());
        hiddenNets.put(id, new LinkedHashMap<>());
        if (first.multiPart()) {
            boxed(id, pieces);
            return;
        }
        symbols.computeIfAbsent(AltiumNames.symbol(first.libraryReference()),
                ignored -> AltiumSymbolDrawing.documentOf(first, filing));
        double rotation = AltiumGeometry.sheetRotation(first);
        double[] at = drawn(first.location());
        double[] centre = SheetMarks.turned(AltiumSymbolDrawing.centreOf(first), rotation, first.mirrored());
        items.add(new ImportedCircuit.Item(id, null, null, ImportedCircuit.rounded(at[0] + centre[0]),
                ImportedCircuit.rounded(at[1] + centre[1]), rotation, first.mirrored()));
        first.pins().forEach(pin -> pin(id, pin));
    }

    /** A part of several pieces: our box where its first piece stands, every wired pin of every piece a label of its net. */
    private void boxed(String id, List<Component> pieces) {
        double[] at = drawn(pieces.getFirst().location());
        items.add(new ImportedCircuit.Item(id, null, null, ImportedCircuit.onGrid(at[0]), ImportedCircuit.onGrid(at[1]), 0, false));
        for (Component piece : pieces) {
            piece.pins().forEach(pin -> pin(id, pin));
        }
        notes.add(id + ": " + pieces.size() + " pieces, drawn as our box where Altium draws the first; the wires of its pieces "
                + "end at its pins' places, joined by their nets.");
    }

    /** A pin: a terminal where a wire connects; a hidden one joins the net of its name, as Altium joins it. */
    private void pin(String id, AltiumSchematic.Pin pin) {
        String name = ImportedPins.nameOf(pin.designator());
        String terminal = id + "." + name;
        partPins.get(id).put(name, terminal);
        if (pin.hidden()) {
            hiddenNets.get(id).put(name, nets.of(pin.name()));
            return;
        }
        terminal(point(pin.end()), new Terminal(terminal, Optional.empty()));
    }

    // ─── marks ──────────────────────────────────────────────────────────────────────────────────────

    private void marks() {
        for (AltiumSchematic.PowerPort port : sheet.powerPorts()) {
            String net = nets.of(port.net());
            boolean ground = port.style().ground() || net.equals(Circuit.GROUND)
                    || ImportedPins.natureOf(port.net(), ImportedPins.Role.POWER).equals(SimulationVocabulary.GROUND);
            if (!ground) {
                globals.add(net);
            }
            double radians = Math.toRadians(port.orientation().degrees());
            double[] body = {Math.cos(radians), -Math.sin(radians)};
            mark(ground ? "ground" : "power", net, ground ? SheetMarks.GROUND : SheetMarks.POWER, port.location(), body);
        }
        for (AltiumSchematic.NetLabel label : sheet.netLabels()) {
            // A label on a bus line names the line, and joins nothing: the bus takes its words.
            if (ImportedBuses.onAnyBus(at(label.location()), busLines())) {
                continue;
            }
            mark("label", nets.of(label.text()), SheetMarks.LABEL, label.location(), new double[] {0, -1});
        }
        for (AltiumSchematic.Port port : sheet.ports()) {
            // A port written as a bus — `D[0..7]` — hands the whole bus across, declared on the sheet; any other a net.
            if (ImportedBuses.namesBus(port.name())) {
                String bus = declaredBuses.declareWritten(port.name(), nets::of);
                if (!busPorts.contains(bus)) {
                    busPorts.add(bus);
                }
                mark("port", bus, SheetMarks.PORT, port.location(), new double[] {0, -1});
                continue;
            }
            String net = nets.of(port.name());
            if (!ports.contains(net)) {
                ports.add(net);
            }
            mark("port", net, SheetMarks.PORT, port.location(), new double[] {0, -1});
        }
        globals.removeIf(ports::contains);
    }

    private void mark(String prefix, String net, SheetMarks.Shape shape, Point at, double[] body) {
        String id = ids.of(prefix + (items.size() + 1));
        items.add(marks.mark(id, net, shape, drawn(at), body));
        terminal(point(at), new Terminal(id + "." + shape.terminal(), Optional.of(net)));
    }

    // ─── wires and nets ─────────────────────────────────────────────────────────────────────────────

    /** The sheet's bus stretches, each a line of Altium's points. */
    private List<List<double[]>> busLines() {
        return sheet.buses().stream().map(bus -> bus.points().stream().map(AltiumSchematicImport::at).toList()).toList();
    }

    /** The sheet's bus lines, each named by the net label that stands on it, as Altium names one: {@code D[0..7]}. */
    private void buses() {
        for (List<double[]> line : ImportedBuses.joined(busLines())) {
            String written = ImportedBuses.writtenOn(line, sheet.netLabels(), label -> at(label.location()), AltiumSchematic.NetLabel::text);
            busLines.add(declaredBuses.line("B" + (busLines.size() + 1), written,
                    line.stream().map(point -> drawn(new SheetTracing.Point(point[0], point[1]))).toList(), nets::of));
        }
    }

    /** The sheet's wires traced between its terminals; a meeting of three and a loose end are junctions. */
    private void wire() {
        List<List<SheetTracing.Point>> drawnWires = sheet.wires().stream()
                .map(wire -> wire.points().stream().map(AltiumSchematicImport::point).toList()).toList();
        for (SheetTracing.Chain chain : SheetTracing.trace(List.copyOf(terminalPoints), drawnWires, this::junction)) {
            String from = terminals.get(chain.from().key()).name();
            String to = terminals.get(chain.to().key()).name();
            wires.add(new ImportedCircuit.Wire("W" + (wires.size() + 1), from, to, chain.bends().stream().map(this::drawn).toList()));
        }
    }

    private void junction(SheetTracing.Point point) {
        double[] at = drawn(point);
        String id = ids.of("junction" + (items.size() + 1));
        items.add(new ImportedCircuit.Item(id, "junction", null, at[0], at[1], 0, false));
        terminal(point, new Terminal(id + ".j", Optional.empty()));
    }

    /** A terminal on a point of the sheet; a second on the same point touches the first, as a wire would join them. */
    private void terminal(SheetTracing.Point point, Terminal terminal) {
        everyTerminal.add(terminal);
        Terminal first = terminals.putIfAbsent(point.key(), terminal);
        if (first == null) {
            terminalPoints.add(point);
        } else {
            touching.add(new String[] {first.name(), terminal.name()});
        }
    }

    /**
     * Each terminal's net: what a wire or a shared point joins is one net, named by a mark on it — a power port, a label,
     * a port —, and every mark of one name is one net; a net no mark names is called after a pin on it.
     */
    private Map<String, String> netOfTerminals() {
        Map<String, String> joined = new HashMap<>();
        wires.forEach(wire -> join(joined, wire.from(), wire.to()));
        touching.forEach(pair -> join(joined, pair[0], pair[1]));
        Map<String, String> named = new HashMap<>();
        everyTerminal.forEach(terminal -> terminal.net().ifPresent(net -> named.putIfAbsent(root(joined, terminal.name()), net)));
        Map<String, String> netOf = new HashMap<>();
        for (Terminal terminal : everyTerminal) {
            String root = root(joined, terminal.name());
            String net = named.computeIfAbsent(root, ignored -> nets.of(terminal.name().replace('.', '_')));
            netOf.put(terminal.name(), net);
        }
        return netOf;
    }

    private static void join(Map<String, String> joined, String one, String other) {
        String first = root(joined, one);
        String second = root(joined, other);
        if (!first.equals(second)) {
            joined.put(second, first);
        }
    }

    private static String root(Map<String, String> joined, String terminal) {
        String root = terminal;
        while (joined.containsKey(root)) {
            root = joined.get(root);
        }
        return root;
    }

    // ─── documents ──────────────────────────────────────────────────────────────────────────────────

    private ImportedDocuments documents(String file, String title) {
        Map<String, String> netOf = netOfTerminals();
        List<ImportedCircuit.Part> parts = new ArrayList<>();
        partComponents.forEach((id, component) -> {
            Map<String, String> connections = new LinkedHashMap<>();
            partPins.get(id).forEach((pin, terminal) -> {
                if (hiddenNets.get(id).containsKey(pin)) {
                    connections.put(pin, hiddenNets.get(id).get(pin));
                } else if (connected(terminal)) {
                    connections.put(pin, netOf.get(terminal));
                }
            });
            parts.add(new ImportedCircuit.Part(id, component, connections, Map.of(), null, null));
        });
        ImportedCircuit circuit = new ImportedCircuit(parts, items, wires, List.of(), Stream.concat(ports.stream().map(ImportedCircuit::netPort),
                busPorts.stream().map(ImportedCircuit::busPort)).toList(), globals, notes, declaredBuses.buses(), busLines);
        List<DocumentNode> library = new ArrayList<>(symbols.values());
        library.addAll(components.values());
        DocumentNode design = ImportedCircuitDocument.of(AltiumNames.design(title), title, "Imported from Altium: " + file, circuit,
                filing);
        return new ImportedDocuments(library, List.of(design), notes);
    }

    /** Whether a pin's terminal reaches anything: a wire, a mark or another pin on its point. */
    private boolean connected(String terminal) {
        return wires.stream().anyMatch(wire -> wire.from().equals(terminal) || wire.to().equals(terminal))
                || touching.stream().anyMatch(pair -> pair[0].equals(terminal) || pair[1].equals(terminal));
    }

    // ─── geometry ───────────────────────────────────────────────────────────────────────────────────

    /** Where the drawing starts: the sheet's left and top, on the grid, so every pin stays on it. */
    private void frame() {
        List<Point> points = new ArrayList<>();
        sheet.components().forEach(component -> {
            points.add(component.location());
            component.pins().forEach(pin -> points.add(pin.end()));
        });
        sheet.wires().forEach(wire -> points.addAll(wire.points()));
        sheet.buses().forEach(bus -> points.addAll(bus.points()));
        sheet.powerPorts().forEach(port -> points.add(port.location()));
        sheet.netLabels().forEach(label -> points.add(label.location()));
        double step = SymbolCanvas.GRID / AltiumGeometry.UNITS;
        left = Math.floor(points.stream().mapToDouble(Point::x).min().orElse(0) / step) * step;
        top = Math.ceil(points.stream().mapToDouble(Point::y).max().orElse(0) / step) * step;
    }

    private static double[] at(Point point) {
        return new double[] {point.x(), point.y()};
    }

    private static SheetTracing.Point point(Point point) {
        return new SheetTracing.Point(point.x(), point.y());
    }

    private double[] drawn(Point point) {
        return drawn(point(point));
    }

    /** A point of Altium's sheet, Y upwards, in drawing units on ours, Y downwards. */
    private double[] drawn(SheetTracing.Point point) {
        return new double[] {ImportedCircuit.rounded((point.x() - left) * AltiumGeometry.UNITS),
                ImportedCircuit.rounded((top - point.y()) * AltiumGeometry.UNITS)};
    }
}
