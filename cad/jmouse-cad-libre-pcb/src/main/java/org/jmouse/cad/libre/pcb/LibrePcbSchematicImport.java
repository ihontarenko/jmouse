package org.jmouse.cad.libre.pcb;

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
import org.jmouse.libre.pcb.LibrePcbAnchor;
import org.jmouse.libre.pcb.LibrePcbBoard;
import org.jmouse.libre.pcb.LibrePcbCircuit;
import org.jmouse.libre.pcb.LibrePcbFile;
import org.jmouse.libre.pcb.LibrePcbJunction;
import org.jmouse.libre.pcb.LibrePcbLibrary;
import org.jmouse.libre.pcb.LibrePcbPoint;
import org.jmouse.libre.pcb.LibrePcbProject;
import org.jmouse.libre.pcb.LibrePcbSchematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * A project's circuit and sheets as jM documents: what its library carries of what it uses, and one circuit, its sheets laid
 * side by side, as a project joins its sheets by their nets.
 *
 * <p>A part of a component that stands on a sheet alone with one pin — GND, VCC — is a mark. One with no pin — a frame — is
 * the sheet's drawing and is left out. Wires are traced between the terminals they reach ({@link SheetTracing}).</p>
 */
final class LibrePcbSchematicImport {

    /**
     * What a reading hands the pcb laid out from it.
     *
     * @param nets    the jM net each LibrePCB net name is
     * @param partIds the id each part has on the circuit, by the part's id in the project
     */
    record Converted(ImportedDocuments documents, String circuit, ImportedNetNames nets, Map<String, String> partIds) {
    }

    /** What a part of the project is to the circuit. */
    private enum Kind {
        PART, SUPPLY, DRAWING
    }

    /** A part placed on the sheet, as the circuit has it. */
    /** The names a component's value may fill in from what it is. */
    private static final String COMPONENT_NAME = "COMPONENT";
    private static final String DEVICE_NAME    = "DEVICE";

    private record Placed(String id, String component, String footprint, LibrePcbComponents.Drawn drawn, Map<String, String> pinOfSignal,
                          Map<String, String> nets, Map<String, String> pads, String value) {
    }

    private final LibrePcbProject project;
    private final LibrePcbLibrary library;
    private final LibrePcbFiling filing;
    /** How the sheet's marks are drawn: where each one's pin stands. */
    private final SheetMarks marks;
    private final Optional<LibrePcbBoard> board;
    private final ImportedNetNames nets;
    private final UniqueIdentifiers ids = new UniqueIdentifiers(ImportedCircuit.ID_OPENING, ImportedCircuit.ID_LONGEST);
    /** What its library's elements become: made once each, as a fetch from an installed library makes them. */
    private final LibrePcbLibraryDocuments made;
    private final Map<String, Placed> placed = new LinkedHashMap<>();
    private final Map<String, Kind> kinds = new HashMap<>();
    private final Map<String, Set<String>> namedPins;
    private final List<ImportedCircuit.Item> items = new ArrayList<>();
    private final List<ImportedCircuit.Wire> wires = new ArrayList<>();
    private final List<ImportedCircuit.BusLine> busLines = new ArrayList<>();
    /** The buses its bus lines declare, each once by its name. */
    private final ImportedBuses declaredBuses = new ImportedBuses();
    /** How many stretches of wire each net of the file is drawn in. */
    private final Map<String, Long> segmentsOfNet = new HashMap<>();
    private final Set<String> globals = new LinkedHashSet<>();
    private final List<String> notes = new ArrayList<>();
    private final Set<String> drawnParts = new LinkedHashSet<>();
    private final Map<String, String> terminals = new LinkedHashMap<>();
    private double sheetLeft;
    private double sheetStart;
    private double top;

    private LibrePcbSchematicImport(LibrePcbProject project, Map<String, byte[]> modelBytes, LibrePcbFiling filing, SheetMarks marks) {
        this.marks = marks;
        this.project = project;
        this.library = project.library();
        this.filing = filing;
        this.board = project.boards().stream().findFirst();
        this.made = new LibrePcbLibraryDocuments(library, projectModels(modelBytes), board.map(LibrePcbBoard::rules), filing);
        this.namedPins = LibrePcbLibraryDocuments.namedPinsOf(project.circuit().parts().values().stream()
                .map(part -> variantOf(part, componentOf(part))).toList());
        this.nets = new ImportedNetNames(filing.ground(), UnaryOperator.identity());
    }

    /** @param modelBytes the project's 3D models, by their path inside it; a footprint names only those it was sent */
    static Converted read(LibrePcbProject project, Map<String, byte[]> modelBytes, String file, String title, LibrePcbFiling filing, SheetMarks marks) {
        LibrePcbSchematicImport reading = new LibrePcbSchematicImport(project, modelBytes, filing, marks);
        reading.parts();
        reading.sheets();
        Map<String, String> partIds = new LinkedHashMap<>();
        reading.placed.forEach((id, part) -> partIds.put(id, part.id()));
        return new Converted(reading.documents(file, title), LibrePcbNames.design(title), reading.nets, partIds);
    }

    // ─── parts and what they are made from ───────────────────────────────────────────────────────────────────────────

    private void parts() {
        for (LibrePcbCircuit.Part part : project.circuit().parts().values()) {
            LibrePcbLibrary.Component component = componentOf(part);
            LibrePcbLibrary.Variant variant = variantOf(part, component);
            Kind kind = kindOf(component, variant);
            kinds.put(part.id(), kind);
            switch (kind) {
                case PART -> part(part, component, variant);
                case DRAWING -> notes.add(part.name() + ": no pins and no package — a frame, a logo —, the sheet's drawing rather than a part, so it is left out.");
                case SUPPLY -> {
                }
            }
        }
    }

    private Kind kindOf(LibrePcbLibrary.Component component, LibrePcbLibrary.Variant variant) {
        int pins = variant.gates().stream().mapToInt(gate -> LibrePcbComponents.symbolOf(library, gate).pins().size()).sum();
        if (!component.schematicOnly()) {
            return Kind.PART;
        }
        return pins == 0 ? Kind.DRAWING : pins == 1 ? Kind.SUPPLY : Kind.PART;
    }

    private void part(LibrePcbCircuit.Part part, LibrePcbLibrary.Component component, LibrePcbLibrary.Variant variant) {
        Optional<LibrePcbLibrary.Device> device = part.device().map(library.devices()::get);
        // The component lands on what its package offers first; a part the board stands on another says so itself.
        LibrePcbLibraryDocuments.Part madePart = made.part(component, variant, device, namedPins);
        LibrePcbComponents.Drawn drawn = madePart.drawn();
        String slug = madePart.slug();
        Optional<LibrePcbLibrary.Package> found = device.map(LibrePcbLibrary.Device::packageId).map(library.packages()::get);
        String own = found.flatMap(given -> boardFootprint(part, given).map(footprint -> made.footprint(given, footprint)))
                .filter(code -> !code.equals(madePart.footprint())).orElse(null);
        Map<String, String> pinOfSignal = LibrePcbComponents.pinOfSignal(component, drawn);
        Map<String, String> partNets = new LinkedHashMap<>();
        part.nets().forEach((signal, net) -> Optional.ofNullable(pinOfSignal.get(signal)).ifPresent(pin -> partNets.put(pin, netName(net))));
        Map<String, String> pads = device.map(given -> LibrePcbComponents.padsOf(library, given, pinOfSignal)).orElse(Map.of());
        Map<String, String> known = new LinkedHashMap<>();
        known.put(COMPONENT_NAME, component.name());
        device.ifPresent(given -> known.put(DEVICE_NAME, given.name()));
        placed.put(part.id(), new Placed(ids.of(part.name()), slug, own, drawn, pinOfSignal, partNets, pads, part.valueInWords(known)));
        if (drawn instanceof LibrePcbComponents.Drawn.AsBox) {
            notes.add(part.name() + ": " + variant.gates().size() + " gates, drawn as our box; the wires of its gates end in labels of their nets.");
        }
    }

    private Optional<LibrePcbLibrary.Footprint> boardFootprint(LibrePcbCircuit.Part part, LibrePcbLibrary.Package found) {
        return board.flatMap(given -> given.devices().stream().filter(device -> device.id().equals(part.id())).findFirst())
                .flatMap(device -> found.footprint(device.footprint()));
    }

    /** The project's 3D models, by where a package it carries keeps one: what its footprints are made with. */
    private static LibrePcbLibraryDocuments.PackageModels projectModels(Map<String, byte[]> modelBytes) {
        return new LibrePcbLibraryDocuments.PackageModels() {
            @Override
            public Optional<byte[]> read(LibrePcbLibrary.Package found, String modelId) {
                return Optional.ofNullable(modelBytes.get(LibrePcbFile.modelPath(found.id(), modelId)));
            }

            @Override
            public String absence() {
                return "is not among the project's files: choose the project's whole folder, or its archive.";
            }
        };
    }

    // ─── sheets ──────────────────────────────────────────────────────────────────────────────────────────────────────

    private void sheets() {
        top = gridUp(project.schematics().stream().mapToDouble(sheet -> bounds(sheet)[3]).max().orElse(0));
        double start = 0;
        project.schematics().forEach(sheet -> sheet.segments().forEach(segment -> segmentsOfNet.merge(netName(segment.net()), 1L, Long::sum)));
        for (LibrePcbSchematic sheet : project.schematics()) {
            double[] bounds = bounds(sheet);
            sheetLeft = gridDown(bounds[0]);
            sheetStart = start;
            terminals.clear();
            Map<String, LibrePcbSchematic.SymbolOnSheet> onSheet = new HashMap<>();
            sheet.symbols().forEach(symbol -> onSheet.put(symbol.id(), symbol));
            sheet.symbols().forEach(this::symbol);
            sheet.segments().forEach(segment -> wire(segment, onSheet));
            sheet.busSegments().forEach(this::bus);
            start = gridUp(start + bounds[2] - sheetLeft + filing.sheetGap());
        }
    }

    private void symbol(LibrePcbSchematic.SymbolOnSheet symbol) {
        LibrePcbCircuit.Part part = project.circuit().parts().get(symbol.part());
        Kind kind = part == null ? null : kinds.get(part.id());
        if (kind == null || kind == Kind.DRAWING) {
            return;
        }
        LibrePcbLibrary.Component component = componentOf(part);
        LibrePcbLibrary.Gate gate = variantOf(part, component).gates().stream().filter(candidate -> candidate.id().equals(symbol.gate())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(part.name() + " is drawn by a gate its component does not have."));
        LibrePcbLibrary.Symbol drawing = LibrePcbComponents.symbolOf(library, gate);
        if (kind == Kind.SUPPLY) {
            supply(part, gate, drawing.pins().getFirst(), symbol);
            return;
        }
        Placed drawn = placed.get(part.id());
        switch (drawn.drawn()) {
            case LibrePcbComponents.Drawn.AsBox ignored -> boxedGate(drawn, gate, drawing, symbol);
            case LibrePcbComponents.Drawn.ByGate byGate -> {
                // A gate the part is not drawn by has no pins: nothing of it is on the circuit.
                if (byGate.gate().id().equals(gate.id())) {
                    placeByGate(drawn, drawing, symbol);
                }
            }
        }
    }

    /** A part drawn by its gate's symbol. LibrePCB mirrors then turns counter-clockwise; a sheet mirrors then turns clockwise, so the angle is its complement. */
    private void placeByGate(Placed part, LibrePcbLibrary.Symbol drawing, LibrePcbSchematic.SymbolOnSheet symbol) {
        double rotation = (360 - symbol.transform().rotation()) % 360;
        boolean mirrored = symbol.transform().mirrored();
        double[] at = drawn(symbol.transform().at());
        double[] centre = SheetMarks.turned(LibrePcbSymbolDrawing.centreOf(drawing), rotation, mirrored);
        double x = at[0] + centre[0];
        double y = at[1] + centre[1];
        items.add(new ImportedCircuit.Item(part.id(), null, null, x, y, rotation, mirrored)
                .withTexts(writtenAt(symbol, "{{NAME}}", x, y), writtenAt(symbol, "{{VALUE}}", x, y)));
        Map<String, String> names = LibrePcbSymbolDrawing.pinNames(drawing);
        for (LibrePcbLibrary.Pin pin : drawing.pins()) {
            terminal(point(symbol.transform().placed(pin.at())), part.id() + "." + names.get(pin.id()));
        }
    }

    /**
     * Where this sheet writes one of a symbol's texts, from the part's centre, for a text our sheet always sets level. Words
     * LibrePCB runs up the page stand beside the body at its middle, on the side LibrePCB's stand on; level words keep their
     * point, raised or lowered by half their height for how LibrePCB hangs them on it, so a name and a value at one point part.
     */
    private double[] writtenAt(LibrePcbSchematic.SymbolOnSheet symbol, String says, double x, double y) {
        return symbol.texts().stream().filter(text -> text.value().equals(says)).findFirst().map(text -> {
            double[] written = drawn(text.at());
            if (text.rotation() % 180 != 0) {
                return new double[] {ImportedCircuit.rounded(written[0] - x), 0};
            }
            double half = text.height() * SymbolCanvas.UNITS_PER_MILLIMETRE / 2;
            double hung = switch (text.across()) {
                case BOTTOM -> -half;
                case TOP -> half;
                case MIDDLE -> 0;
            };
            return new double[] {ImportedCircuit.rounded(written[0] - x), ImportedCircuit.rounded(written[1] + hung - y)};
        }).orElse(null);
    }

    /** A gate of a part drawn as our box: the box stands where its first gate does, each wired pin a label of its net. */
    private void boxedGate(Placed part, LibrePcbLibrary.Gate gate, LibrePcbLibrary.Symbol drawing, LibrePcbSchematic.SymbolOnSheet symbol) {
        if (drawnParts.add(part.id())) {
            double[] at = drawn(symbol.transform().at());
            items.add(new ImportedCircuit.Item(part.id(), null, null, ImportedCircuit.onGrid(at[0]), ImportedCircuit.onGrid(at[1]), 0, false));
        }
        for (LibrePcbLibrary.Pin pin : drawing.pins()) {
            String net = Optional.ofNullable(gate.signals().get(pin.id())).map(part.pinOfSignal()::get).map(part.nets()::get).orElse(null);
            if (net != null) {
                double[] inward = inward(pin, symbol);
                SheetMarks.Shape shape = net.equals(Circuit.GROUND) ? SheetMarks.GROUND : SheetMarks.LABEL;
                mark("label", net, shape, point(symbol.transform().placed(pin.at())), new double[] {-inward[0], -inward[1]});
            }
        }
    }

    /** A supply: a ground mark for the reference net, a global label for any other, its body where LibrePCB draws it. */
    private void supply(LibrePcbCircuit.Part part, LibrePcbLibrary.Gate gate, LibrePcbLibrary.Pin pin, LibrePcbSchematic.SymbolOnSheet symbol) {
        Optional<String> written = Optional.ofNullable(gate.signals().get(pin.id())).map(part.nets()::get).map(this::writtenNet);
        if (written.isEmpty()) {
            notes.add(part.name() + ": a supply on no net, so it is left out.");
            return;
        }
        String net = nets.of(written.get());
        boolean ground = net.equals(Circuit.GROUND) || ImportedPins.natureOf(written.get(), ImportedPins.Role.POWER).equals(SimulationVocabulary.GROUND);
        if (!ground) {
            globals.add(net);
        }
        mark(ground ? "ground" : "power", net, ground ? SheetMarks.GROUND : SheetMarks.POWER, point(symbol.transform().placed(pin.at())), inward(pin, symbol));
    }

    /** The way a pin's line runs from where it connects into its body, on our sheet, Y downwards. */
    private static double[] inward(LibrePcbLibrary.Pin pin, LibrePcbSchematic.SymbolOnSheet symbol) {
        double radians = Math.toRadians(pin.rotation());
        LibrePcbPoint turned = symbol.transform().turned(new LibrePcbPoint(Math.cos(radians), Math.sin(radians)));
        return new double[] {turned.x(), -turned.y()};
    }

    /**
     * One segment's wires traced between its own terminals. Each is traced alone: only the file says where a net runs, and two
     * nets may cross on one point of a sheet, which tracing the whole sheet would take for a join.
     */
    /**
     * A bus segment of the sheet as the bus lines it makes: its lines joined end to end, named as the circuit names its bus —
     * {@code D[0..7]} — with the nets that name carries. A line ends on the segment's own junctions.
     */
    private void bus(LibrePcbSchematic.BusSegment segment) {
        Map<String, LibrePcbPoint> junctions = new HashMap<>();
        segment.junctions().forEach(junction -> junctions.put(junction.id(), junction.at()));
        List<List<double[]>> stretches = new ArrayList<>();
        for (LibrePcbSchematic.Wire line : segment.lines()) {
            if (line.from() instanceof LibrePcbAnchor.OnJunction from && line.to() instanceof LibrePcbAnchor.OnJunction to
                    && junctions.containsKey(from.junction()) && junctions.containsKey(to.junction())) {
                LibrePcbPoint start = junctions.get(from.junction());
                LibrePcbPoint end = junctions.get(to.junction());
                stretches.add(List.of(new double[] {start.x(), start.y()}, new double[] {end.x(), end.y()}));
            }
        }
        LibrePcbCircuit.Bus named = project.circuit().buses().get(segment.bus());
        for (List<double[]> line : ImportedBuses.joined(stretches)) {
            busLines.add(declaredBuses.line("B" + (busLines.size() + 1), named == null ? null : named.name(),
                    line.stream().map(point -> drawn(new SheetTracing.Point(point[0], point[1]))).toList(), nets::of));
        }
    }

    private void wire(LibrePcbSchematic.Segment segment, Map<String, LibrePcbSchematic.SymbolOnSheet> onSheet) {
        Map<String, String> reached = new LinkedHashMap<>();
        List<SheetTracing.Point> points = new ArrayList<>();
        List<List<SheetTracing.Point>> drawnWires = new ArrayList<>();
        for (LibrePcbSchematic.Wire wire : segment.wires()) {
            for (LibrePcbAnchor end : List.of(wire.from(), wire.to())) {
                if (end instanceof LibrePcbAnchor.OnPin) {
                    SheetTracing.Point pin = pointOf(end, segment, onSheet);
                    if (reached.putIfAbsent(pin.key(), terminals.get(pin.key())) == null) {
                        points.add(pin);
                    }
                }
            }
            drawnWires.add(List.of(pointOf(wire.from(), segment, onSheet), pointOf(wire.to(), segment, onSheet)));
        }
        labels(segment, onSheet, reached, points);
        List<SheetTracing.Point> unnamed = joinedByItsNetAlone(segment, reached) ? List.copyOf(points) : List.of();
        List<SheetTracing.Chain> chains = SheetTracing.trace(List.copyOf(points), drawnWires, point -> {
            String name = junction(point);
            reached.put(point.key(), name);
        });
        for (SheetTracing.Chain chain : chains) {
            wires.add(new ImportedCircuit.Wire("W" + (wires.size() + 1), reached.get(chain.from().key()), reached.get(chain.to().key()),
                    chain.bends().stream().map(this::drawn).toList()));
        }
        unnamed.stream().findFirst().ifPresent(pin -> labelOf(segment, onSheet, pin, reached.get(pin.key())));
    }

    /**
     * Whether a stretch of wire is one with the rest of its net only by the file's saying so: LibrePCB keeps a net's stretches
     * together by its name, labelled or not, while on our sheet stretches with no mark between them are nets of their own. Such
     * a stretch is given a label, or the first save of the sheet would part it from its net.
     */
    private boolean joinedByItsNetAlone(LibrePcbSchematic.Segment segment, Map<String, String> reached) {
        boolean marked = reached.values().stream().filter(java.util.Objects::nonNull).anyMatch(terminal -> items.stream()
                .anyMatch(item -> item.mark() != null && !item.mark().equals("junction") && terminal.startsWith(item.id() + ".")));
        String net = netName(segment.net());
        return !marked && (net.equals(Circuit.GROUND) || segmentsOfNet.getOrDefault(net, 0L) > 1);
    }

    /** A label of a stretch's net, a grid step off one of its pins, away from its wire. */
    private void labelOf(LibrePcbSchematic.Segment segment, Map<String, LibrePcbSchematic.SymbolOnSheet> onSheet, SheetTracing.Point pin, String terminal) {
        SheetTracing.Point other = null;
        for (LibrePcbSchematic.Wire wire : segment.wires()) {
            SheetTracing.Point first = pointOf(wire.from(), segment, onSheet);
            SheetTracing.Point second = pointOf(wire.to(), segment, onSheet);
            if (first.key().equals(pin.key())) {
                other = second;
            } else if (second.key().equals(pin.key())) {
                other = first;
            }
        }
        if (other == null || terminal == null) {
            return;
        }
        double length = Math.max(Math.hypot(pin.x() - other.x(), pin.y() - other.y()), Double.MIN_VALUE);
        double reach = filing.sheetGrid();
        SheetTracing.Point at = new SheetTracing.Point(pin.x() + (pin.x() - other.x()) / length * reach, pin.y() + (pin.y() - other.y()) / length * reach);
        double[] body = {pin.x() - other.x(), other.y() - pin.y()};
        String net = netName(segment.net());
        boolean ground = net.equals(Circuit.GROUND);
        String label = markOf(ground ? "ground" : "label", net, ground ? SheetMarks.GROUND : SheetMarks.LABEL, at, body);
        wires.add(new ImportedCircuit.Wire("W" + (wires.size() + 1), terminal, label, List.of()));
    }

    /** Each label of a segment: a label mark where the nearest wire of it ends on no pin, its body pointing out of it. */
    private void labels(LibrePcbSchematic.Segment segment, Map<String, LibrePcbSchematic.SymbolOnSheet> onSheet, Map<String, String> reached,
                        List<SheetTracing.Point> points) {
        for (LibrePcbPoint label : segment.labels()) {
            SheetTracing.Point nearest = null;
            SheetTracing.Point other = null;
            for (LibrePcbSchematic.Wire wire : segment.wires()) {
                SheetTracing.Point first = pointOf(wire.from(), segment, onSheet);
                SheetTracing.Point second = pointOf(wire.to(), segment, onSheet);
                for (SheetTracing.Point[] end : new SheetTracing.Point[][] {{first, second}, {second, first}}) {
                    if (!reached.containsKey(end[0].key()) && (nearest == null || distance(end[0], label) < distance(nearest, label))) {
                        nearest = end[0];
                        other = end[1];
                    }
                }
            }
            if (nearest != null) {
                double[] body = {nearest.x() - other.x(), other.y() - nearest.y()};
                reached.put(nearest.key(), markOf("label", netName(segment.net()), SheetMarks.LABEL, nearest, body));
                points.add(nearest);
            }
        }
    }

    private String junction(SheetTracing.Point point) {
        double[] at = drawn(point);
        String id = ids.of("junction" + (items.size() + 1));
        items.add(new ImportedCircuit.Item(id, "junction", null, at[0], at[1], 0, false));
        return id + ".j";
    }

    /** A mark on a point of the sheet, its body going one way from it: the terminal a wire reaches it by. */
    private String markOf(String prefix, String net, SheetMarks.Shape shape, SheetTracing.Point point, double[] body) {
        String id = ids.of(prefix + (items.size() + 1));
        items.add(marks.mark(id, net, shape, drawn(point), body));
        return id + "." + shape.terminal();
    }

    /** A mark standing on a pin: wires reach it as they reach the pin. */
    private void mark(String prefix, String net, SheetMarks.Shape shape, SheetTracing.Point point, double[] body) {
        terminal(point, markOf(prefix, net, shape, point, body));
    }

    private void terminal(SheetTracing.Point point, String name) {
        terminals.putIfAbsent(point.key(), name);
    }

    // ─── documents ───────────────────────────────────────────────────────────────────────────────────────────────────

    private ImportedDocuments documents(String file, String title) {
        List<ImportedCircuit.Part> parts = new ArrayList<>();
        placed.values().forEach(part -> parts.add(new ImportedCircuit.Part(part.id(), part.component(), part.nets(), Map.of(), null, part.footprint(), part.pads())
                .withValue(part.value())));
        ImportedCircuit circuit = new ImportedCircuit(parts, items, wires, List.of(), List.of(), globals, notes, declaredBuses.buses(), busLines);
        // What a design is built from, each before what names it: a footprint before the component landing on it.
        List<DocumentNode> built = made.documents();
        DocumentNode design = ImportedCircuitDocument.of(LibrePcbNames.design(title), title, "Imported from LibrePCB: " + file, circuit, filing);
        return new ImportedDocuments(built, List.of(design), notes, made.files());
    }

    // ─── geometry ────────────────────────────────────────────────────────────────────────────────────────────────────

    private SheetTracing.Point pointOf(LibrePcbAnchor anchor, LibrePcbSchematic.Segment segment, Map<String, LibrePcbSchematic.SymbolOnSheet> onSheet) {
        return switch (anchor) {
            case LibrePcbAnchor.OnJunction junction -> point(segment.junctions().stream().filter(candidate -> candidate.id().equals(junction.junction()))
                    .findFirst().map(LibrePcbJunction::at).orElseThrow(() -> new IllegalArgumentException("A wire ends on a junction its segment does not have.")));
            case LibrePcbAnchor.OnPin pin -> point(pinPoint(pin, onSheet));
            case LibrePcbAnchor.OnPad ignored -> throw new IllegalArgumentException("A wire of a sheet ends on a pad of a board.");
            case LibrePcbAnchor.OnVia ignored -> throw new IllegalArgumentException("A wire of a sheet ends on a via of a board.");
        };
    }

    private LibrePcbPoint pinPoint(LibrePcbAnchor.OnPin anchor, Map<String, LibrePcbSchematic.SymbolOnSheet> onSheet) {
        LibrePcbSchematic.SymbolOnSheet symbol = onSheet.get(anchor.symbol());
        if (symbol == null) {
            throw new IllegalArgumentException("A wire ends on a symbol its sheet does not have.");
        }
        LibrePcbCircuit.Part part = project.circuit().parts().get(symbol.part());
        LibrePcbLibrary.Component component = componentOf(part);
        LibrePcbLibrary.Gate gate = variantOf(part, component).gates().stream().filter(candidate -> candidate.id().equals(symbol.gate())).findFirst().orElseThrow();
        LibrePcbLibrary.Pin pin = LibrePcbComponents.symbolOf(library, gate).pins().stream().filter(candidate -> candidate.id().equals(anchor.pin())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("A wire ends on a pin the symbol of " + part.name() + " does not have."));
        return symbol.transform().placed(pin.at());
    }

    private double[] bounds(LibrePcbSchematic sheet) {
        double[] bounds = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        sheet.symbols().forEach(symbol -> reach(bounds, symbol.transform().at()));
        sheet.segments().forEach(segment -> segment.junctions().forEach(junction -> reach(bounds, junction.at())));
        sheet.busSegments().forEach(segment -> segment.junctions().forEach(junction -> reach(bounds, junction.at())));
        return bounds[0] > bounds[2] ? new double[] {0, 0, 0, 0} : bounds;
    }

    private static void reach(double[] bounds, LibrePcbPoint point) {
        bounds[0] = Math.min(bounds[0], point.x());
        bounds[1] = Math.min(bounds[1], point.y());
        bounds[2] = Math.max(bounds[2], point.x());
        bounds[3] = Math.max(bounds[3], point.y());
    }

    private static SheetTracing.Point point(LibrePcbPoint point) {
        return new SheetTracing.Point(point.x(), point.y());
    }

    private double[] drawn(LibrePcbPoint point) {
        return drawn(point(point));
    }

    /** A point of LibrePCB's sheet, millimetres with Y upwards, in drawing units on ours: its sheet's place, Y downwards. */
    private double[] drawn(SheetTracing.Point point) {
        return new double[] {ImportedCircuit.rounded((point.x() - sheetLeft + sheetStart) * SymbolCanvas.UNITS_PER_MILLIMETRE),
                ImportedCircuit.rounded((top - point.y()) * SymbolCanvas.UNITS_PER_MILLIMETRE)};
    }

    private double gridDown(double millimetres) {
        return Math.floor(millimetres / filing.sheetGrid()) * filing.sheetGrid();
    }

    private double gridUp(double millimetres) {
        return Math.ceil(millimetres / filing.sheetGrid()) * filing.sheetGrid();
    }

    private static double distance(SheetTracing.Point point, LibrePcbPoint other) {
        return Math.hypot(point.x() - other.x(), point.y() - other.y());
    }

    // ─── lookups ─────────────────────────────────────────────────────────────────────────────────────────────────────

    private String writtenNet(String net) {
        LibrePcbCircuit.Net found = project.circuit().nets().get(net);
        if (found == null) {
            throw new IllegalArgumentException("A part is on a net the circuit does not have.");
        }
        return found.name();
    }

    private String netName(String net) {
        return nets.of(writtenNet(net));
    }

    private LibrePcbLibrary.Component componentOf(LibrePcbCircuit.Part part) {
        LibrePcbLibrary.Component component = library.components().get(part.libraryComponent());
        if (component == null) {
            throw new IllegalArgumentException(part.name() + " is a component the project's library does not carry.");
        }
        return component;
    }

    private static LibrePcbLibrary.Variant variantOf(LibrePcbCircuit.Part part, LibrePcbLibrary.Component component) {
        return component.variant(part.variant()).orElseThrow(() -> new IllegalArgumentException(
                part.name() + " is a variant its component " + component.name() + " does not have."));
    }
}
