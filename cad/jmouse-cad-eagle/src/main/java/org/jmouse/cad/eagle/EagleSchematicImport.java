package org.jmouse.cad.eagle;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.exchange.imported.ImportedBuses;
import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.jmouse.cad.exchange.imported.ImportedCircuitDocument;
import org.jmouse.cad.exchange.imported.ImportedNetNames;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.cad.exchange.imported.SheetMarks;
import org.jmouse.cad.exchange.imported.SheetTracing;
import org.jmouse.cad.exchange.imported.SymbolCanvas;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.eagle.EagleGraphic;
import org.jmouse.eagle.EagleLibrary;
import org.jmouse.eagle.EagleRotation;
import org.jmouse.eagle.EagleSchematic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * An Eagle schematic as jM documents: a footprint for each package its parts land on, a symbol for each gate a part of
 * one gate is drawn with, a component for each device, and one circuit — its sheets laid side by side, as Eagle joins
 * every sheet's nets by name.
 *
 * <p>A part with no package whose one pin supplies a net — Eagle's GND, VCC, +5V — is a mark: a ground for the
 * reference net, a global label for any other. A part with neither a pin nor a package — a frame, a logo — is the
 * sheet's drawing, not a part, and is left out. Eagle's wires are traced into jMX's between the terminals they reach
 * ({@link SheetTracing}); a label on a net is a label mark where its wire ends.</p>
 */
final class EagleSchematicImport {

    /**
     * What a schematic's reading hands the pcb laid out from it: the documents, the circuit's slug, the jMX net each
     * Eagle net is, and the id each part has on the circuit — which a pcb's element of the same name places.
     */
    record Converted(ImportedDocuments documents, String circuit, ImportedNetNames nets, Map<String, String> partIds) {
    }

    /** What a part of the schematic is to the circuit. */
    private enum Kind {
        /** A part: a component placed and wired. */
        PART,
        /** A supply: a mark of the net it names. */
        SUPPLY,
        /** A drawing with no pins: left out. */
        DRAWING
    }

    /** Eagle's grid, 0.1 inch: a sheet's edge stays on it, so every pin does. */
    private static final double EAGLE_GRID = 2.54;

    /** A part placed on the sheet, as the circuit has it. */
    private record Placed(String id, String component, Map<String, Map<String, String>> pinNames, EagleComponents.Drawn drawn,
                          Map<String, String> nets, String value) {
    }

    private final EagleSchematic schematic;
    private final EagleLibraries filing;
    /** How the sheet's marks are drawn: where each one's pin stands. */
    private final SheetMarks marks;
    private final ImportedNetNames nets;
    private final UniqueIdentifiers ids = new UniqueIdentifiers(ImportedCircuit.ID_OPENING, ImportedCircuit.ID_LONGEST);

    private final Map<String, DocumentNode> footprints = new LinkedHashMap<>();
    private final Map<String, DocumentNode> symbols = new LinkedHashMap<>();
    private final Map<String, DocumentNode> components = new LinkedHashMap<>();
    private final Map<String, Placed> placed = new LinkedHashMap<>();
    private final Map<String, Kind> kinds = new LinkedHashMap<>();
    /** The Eagle net each pin of each part is on, by {@code part|gate|pin}. */
    private final Map<String, String> pinNets = new LinkedHashMap<>();

    private final List<ImportedCircuit.Item> items = new ArrayList<>();
    private final List<ImportedCircuit.Wire> wires = new ArrayList<>();
    private final List<ImportedCircuit.BusLine> busLines = new ArrayList<>();
    /** The buses its bus lines declare, each once by its name. */
    private final ImportedBuses declaredBuses = new ImportedBuses();
    private final Set<String> globals = new LinkedHashSet<>();
    private final List<String> notes = new ArrayList<>();
    private final Set<String> drawnParts = new LinkedHashSet<>();

    /** Where the sheet being read stands: its left edge in Eagle's frame, and where that edge is drawn. */
    private double sheetLeft;
    private double sheetStart;
    /** The top of every sheet, in Eagle's frame: Y goes up there and down on ours. */
    private double top;
    /** The terminal on each point of the sheet being read, by the point's key, and the points in order. */
    private final Map<String, String> terminals = new LinkedHashMap<>();
    private final List<SheetTracing.Point> terminalPoints = new ArrayList<>();

    private EagleSchematicImport(EagleSchematic schematic, EagleLibraries filing, SheetMarks marks) {
        this.schematic = schematic;
        this.marks = marks;
        this.filing = filing;
        this.nets = new ImportedNetNames(filing.ground(), UnaryOperator.identity());
    }

    /**
     * @param file  the schematic's file, which the circuit says it was imported from
     * @param title what the circuit is called
     */
    static Converted read(EagleSchematic schematic, String file, String title, EagleLibraries filing, SheetMarks marks) {
        EagleSchematicImport reading = new EagleSchematicImport(schematic, filing, marks);
        reading.parts();
        reading.sheets();
        Map<String, String> partIds = new LinkedHashMap<>();
        reading.placed.forEach((name, part) -> partIds.put(name, part.id()));
        return new Converted(reading.documents(file, title), EagleNames.design(title), reading.nets, partIds);
    }

    // ─── parts and what they are made from ──────────────────────────────────────────────────────────

    private void parts() {
        for (EagleSchematic.Sheet sheet : schematic.sheets()) {
            for (EagleSchematic.Net net : sheet.nets()) {
                for (EagleSchematic.Segment segment : net.segments()) {
                    segment.pins().forEach(pin -> pinNets.put(pinKey(pin.part(), pin.gate(), pin.pin()), net.name()));
                }
            }
        }
        for (EagleSchematic.Part part : schematic.parts()) {
            EagleLibrary library = libraryOf(part);
            EagleLibrary.DeviceSet deviceSet = deviceSetOf(library, part);
            EagleLibrary.Device device = deviceSet.device(part.device()).orElseThrow(() -> new IllegalArgumentException(
                    part.name() + " is the device " + part.device() + " of " + deviceSet.name() + ", which "
                    + library.name() + " does not carry."));
            Kind kind = kindOf(library, deviceSet, device);
            kinds.put(part.name(), kind);
            switch (kind) {
                case PART -> part(part, library, deviceSet, device);
                case DRAWING -> notes.add(part.name() + ": no pins and no package — a frame, a logo —, the sheet's "
                        + "drawing rather than a part, so it is left out.");
                case SUPPLY -> {
                }
            }
        }
    }

    private void part(EagleSchematic.Part part, EagleLibrary library, EagleLibrary.DeviceSet deviceSet,
                      EagleLibrary.Device device) {
        if (device.packageName() != null) {
            EagleLibrary.Package found = library.packages().get(device.packageName());
            if (found == null) {
                throw new IllegalArgumentException(part.name() + " lands on the package " + device.packageName() + ", which "
                        + library.name() + " does not carry.");
            }
            footprints.computeIfAbsent(EagleNames.footprint(library.name(), found.name()),
                    ignored -> EagleFootprintDrawing.documentOf(library.name(), found, filing));
        }
        EagleComponents.Drawn drawn = EagleComponents.drawnOf(library, deviceSet);
        if (drawn instanceof EagleComponents.Drawn.ByGate byGate) {
            EagleLibrary.Symbol symbol = EagleComponents.symbolOf(library, byGate.gate());
            symbols.computeIfAbsent(EagleNames.symbol(library.name(), symbol.name()),
                    ignored -> EagleSymbolDrawing.documentOf(library.name(), symbol, filing));
        }
        String component = EagleNames.component(library.name(), deviceSet.name(), device.name());
        components.computeIfAbsent(component, ignored -> EagleComponents.documentOf(library, deviceSet, device, filing));
        Map<String, Map<String, String>> pinNames = EagleComponents.pinNames(library, deviceSet);
        Map<String, String> partNets = new LinkedHashMap<>();
        pinNames.forEach((gate, names) -> names.forEach((pin, name) -> {
            String net = pinNets.get(pinKey(part.name(), gate, pin));
            if (net != null) {
                partNets.put(name, nets.of(net));
            }
        }));
        placed.put(part.name(), new Placed(ids.of(part.name()), component, pinNames, drawn, partNets, part.value()));
        if (drawn instanceof EagleComponents.Drawn.AsBox) {
            notes.add(part.name() + ": " + deviceSet.gates().size() + " gates, drawn as our box where Eagle draws its first; "
                    + "the wires of its gates end in labels of their nets.");
        }
    }

    /**
     * A supply is a device with no package and one pin that supplies its net; a drawing has neither a package nor a pin;
     * anything else is a part — a mounting hole too, which has a package and no pin.
     */
    private static Kind kindOf(EagleLibrary library, EagleLibrary.DeviceSet deviceSet, EagleLibrary.Device device) {
        List<EagleLibrary.Pin> pins = deviceSet.gates().stream()
                .flatMap(gate -> EagleComponents.symbolOf(library, gate).pins().stream()).toList();
        boolean packaged = device.packageName() != null;
        if (!packaged && pins.isEmpty()) {
            return Kind.DRAWING;
        }
        boolean supply = !packaged && pins.size() == 1 && pins.getFirst().direction() == EagleLibrary.Pin.Direction.SUP;
        return supply ? Kind.SUPPLY : Kind.PART;
    }

    // ─── sheets ─────────────────────────────────────────────────────────────────────────────────────

    /** Each sheet in turn, laid right of the one before it by the filing's gap. */
    private void sheets() {
        top = gridUp(schematic.sheets().stream().mapToDouble(sheet -> bounds(sheet)[3]).max().orElse(0));
        double start = 0;
        for (EagleSchematic.Sheet sheet : schematic.sheets()) {
            double[] bounds = bounds(sheet);
            sheetLeft = gridDown(bounds[0]);
            sheetStart = start;
            terminals.clear();
            terminalPoints.clear();
            sheet.instances().forEach(this::instance);
            sheet.nets().forEach(this::labels);
            wire(sheet);
            sheet.buses().forEach(this::bus);
            // Once the wires stand: a stretch that still reaches no mark gets its own.
            sheet.nets().stream().filter(net -> net.segments().size() > 1).forEach(net -> net.segments().stream()
                    .filter(segment -> segment.labels().isEmpty()).forEach(segment -> labelOfStretch(net, segment)));
            start = gridUp(start + bounds[2] - sheetLeft + filing.sheetGap());
        }
    }

    private void instance(EagleSchematic.Instance instance) {
        Kind kind = kinds.get(instance.part());
        if (kind == null || kind == Kind.DRAWING) {
            return;
        }
        EagleSchematic.Part part = schematic.part(instance.part()).orElseThrow();
        EagleLibrary library = libraryOf(part);
        EagleLibrary.Gate gate = deviceSetOf(library, part).gate(instance.gate()).orElseThrow(() -> new IllegalArgumentException(
                part.name() + " is drawn by its gate " + instance.gate() + ", which its device set does not have."));
        EagleLibrary.Symbol symbol = EagleComponents.symbolOf(library, gate);
        if (kind == Kind.SUPPLY) {
            supply(part, gate, symbol.pins().getFirst(), instance);
            return;
        }
        Placed drawnPart = placed.get(part.name());
        switch (drawnPart.drawn()) {
            case EagleComponents.Drawn.AsBox ignored -> boxedGate(drawnPart, gate, symbol, instance);
            case EagleComponents.Drawn.ByGate byGate -> {
                // A gate the part is not drawn by has no pins: nothing of it is on the circuit.
                if (byGate.gate().equals(gate)) {
                    placeByGate(drawnPart, gate, symbol, instance);
                }
            }
        }
    }

    /** A part drawn by its gate's symbol: turned and mirrored as Eagle turns it, its pins where Eagle connects them. */
    private void placeByGate(Placed drawnPart, EagleLibrary.Gate gate, EagleLibrary.Symbol symbol,
                             EagleSchematic.Instance instance) {
        double rotation = EagleGeometry.sheetRotation(instance.rotation());
        boolean mirrored = instance.rotation().mirrored();
        double[] at = drawn(instance.x(), instance.y());
        double[] centre = SheetMarks.turned(EagleSymbolDrawing.centreOf(symbol), rotation, mirrored);
        items.add(new ImportedCircuit.Item(drawnPart.id(), null, null, at[0] + centre[0], at[1] + centre[1], rotation, mirrored));
        Map<String, String> names = drawnPart.pinNames().get(gate.name());
        for (EagleLibrary.Pin pin : symbol.pins()) {
            terminal(pinPoint(instance, pin), drawnPart.id() + "." + names.get(pin.name()));
        }
    }

    /**
     * A gate of a part drawn as our box: the box stands where its first gate does, and each wired pin of every gate is
     * a label of its net where Eagle connects it, its body pointing out from the pin.
     */
    private void boxedGate(Placed part, EagleLibrary.Gate gate, EagleLibrary.Symbol symbol, EagleSchematic.Instance instance) {
        if (drawnParts.add(part.id())) {
            double[] at = drawn(instance.x(), instance.y());
            items.add(new ImportedCircuit.Item(part.id(), null, null, ImportedCircuit.onGrid(at[0]), ImportedCircuit.onGrid(at[1]), 0, false));
        }
        Map<String, String> names = part.pinNames().get(gate.name());
        for (EagleLibrary.Pin pin : symbol.pins()) {
            String net = part.nets().get(names.get(pin.name()));
            if (net == null) {
                continue;
            }
            double[] inward = sheetDirection(pin.rotation(), instance.rotation());
            SheetMarks.Shape shape = net.equals(Circuit.GROUND) ? SheetMarks.GROUND : SheetMarks.LABEL;
            mark("label", net, shape, pinPoint(instance, pin), new double[] {-inward[0], -inward[1]});
        }
    }

    /** A supply: a ground mark for the reference net, a global label for any other, its body where Eagle draws it. */
    private void supply(EagleSchematic.Part part, EagleLibrary.Gate gate, EagleLibrary.Pin pin, EagleSchematic.Instance instance) {
        String eagleNet = pinNets.get(pinKey(part.name(), gate.name(), pin.name()));
        if (eagleNet == null) {
            notes.add(part.name() + ": a supply on no net, so it is left out.");
            return;
        }
        String net = nets.of(eagleNet);
        double[] body = sheetDirection(pin.rotation(), instance.rotation());
        boolean ground = net.equals(Circuit.GROUND)
                || ImportedPins.natureOf(eagleNet, ImportedPins.Role.POWER).equals(SimulationVocabulary.GROUND);
        if (!ground) {
            globals.add(net);
        }
        mark(ground ? "ground" : "power", net, ground ? SheetMarks.GROUND : SheetMarks.POWER, pinPoint(instance, pin), body);
    }

    /** Each label of a net: a label mark where the nearest wire of its segment ends loose, its body pointing out of it. */
    private void labels(EagleSchematic.Net net) {
        for (EagleSchematic.Segment segment : net.segments()) {
            for (EagleSchematic.Point label : segment.labels()) {
                SheetTracing.Point nearest = null;
                SheetTracing.Point other = null;
                for (EagleGraphic.Wire wire : segment.wires()) {
                    SheetTracing.Point first = new SheetTracing.Point(wire.x1(), wire.y1());
                    SheetTracing.Point second = new SheetTracing.Point(wire.x2(), wire.y2());
                    for (SheetTracing.Point[] end : new SheetTracing.Point[][] {{first, second}, {second, first}}) {
                        if (terminals.containsKey(end[0].key())) {
                            continue;
                        }
                        if (nearest == null || distance(end[0], label) < distance(nearest, label)) {
                            nearest = end[0];
                            other = end[1];
                        }
                    }
                }
                if (nearest != null) {
                    double[] outward = {nearest.x() - other.x(), other.y() - nearest.y()};
                    mark("label", nets.of(net.name()), SheetMarks.LABEL, nearest, outward);
                }
            }
        }
    }

    /**
     * A stretch of a net with no label and no supply on it: Eagle keeps it with the rest of its net by the net's name, while on
     * our sheet it would be a net of its own the first time the sheet is saved. It is given a label, a grid step off its first pin.
     */
    private void labelOfStretch(EagleSchematic.Net net, EagleSchematic.Segment segment) {
        SheetTracing.Point pin = null;
        SheetTracing.Point other = null;
        for (EagleGraphic.Wire wire : segment.wires()) {
            SheetTracing.Point first = new SheetTracing.Point(wire.x1(), wire.y1());
            SheetTracing.Point second = new SheetTracing.Point(wire.x2(), wire.y2());
            for (SheetTracing.Point[] end : new SheetTracing.Point[][] {{first, second}, {second, first}}) {
                String terminal = terminals.get(end[0].key());
                if (terminal == null) {
                    continue;
                }
                boolean marked = items.stream().anyMatch(item -> item.mark() != null && !item.mark().equals("junction") && terminal.startsWith(item.id() + "."));
                if (marked) {
                    return;
                }
                if (pin == null) {
                    pin = end[0];
                    other = end[1];
                }
            }
        }
        if (pin == null) {
            return;
        }
        double length = Math.max(Math.hypot(pin.x() - other.x(), pin.y() - other.y()), Double.MIN_VALUE);
        double reach = EAGLE_GRID;
        SheetTracing.Point at = new SheetTracing.Point(pin.x() + (pin.x() - other.x()) / length * reach, pin.y() + (pin.y() - other.y()) / length * reach);
        double[] body = {pin.x() - other.x(), other.y() - pin.y()};
        String name = nets.of(net.name());
        boolean ground = name.equals(Circuit.GROUND);
        String id = ids.of((ground ? "ground" : "label") + (items.size() + 1));
        SheetMarks.Shape shape = ground ? SheetMarks.GROUND : SheetMarks.LABEL;
        items.add(marks.mark(id, name, shape, drawn(at), body));
        wires.add(new ImportedCircuit.Wire("W" + (wires.size() + 1), terminals.get(pin.key()), id + "." + shape.terminal(), List.of()));
    }

    /** The sheet's wires traced between its terminals; a meeting of three and a loose end are junctions. */
    /** A bus line of the sheet, as many as its wires make: Eagle's name says the nets it carries. */
    private void bus(EagleSchematic.Bus bus) {
        List<List<double[]>> stretches = bus.wires().stream().map(wire -> List.of(new double[] {wire.x1(), wire.y1()}, new double[] {wire.x2(), wire.y2()})).toList();
        for (List<double[]> line : ImportedBuses.joined(stretches)) {
            busLines.add(declaredBuses.line("B" + (busLines.size() + 1), bus.name(), line.stream().map(point -> drawn(point[0], point[1])).toList(), nets::of));
        }
    }

    private void wire(EagleSchematic.Sheet sheet) {
        List<List<SheetTracing.Point>> drawnWires = new ArrayList<>();
        sheet.nets().forEach(net -> net.segments().forEach(segment -> segment.wires().forEach(wire -> drawnWires.add(List.of(
                new SheetTracing.Point(wire.x1(), wire.y1()), new SheetTracing.Point(wire.x2(), wire.y2()))))));
        for (SheetTracing.Chain chain : SheetTracing.trace(List.copyOf(terminalPoints), drawnWires, this::junction)) {
            String from = terminals.get(chain.from().key());
            String to = terminals.get(chain.to().key());
            List<double[]> bends = chain.bends().stream().map(this::drawn).toList();
            wires.add(new ImportedCircuit.Wire("W" + (wires.size() + 1), from, to, bends));
        }
    }

    private void junction(SheetTracing.Point point) {
        double[] at = drawn(point);
        String id = ids.of("junction" + (items.size() + 1));
        items.add(new ImportedCircuit.Item(id, "junction", null, at[0], at[1], 0, false));
        terminal(point, id + ".j");
    }

    /** A mark whose pin stands on a point of Eagle's sheet and whose body goes one way from it on ours. */
    private void mark(String prefix, String net, SheetMarks.Shape shape, SheetTracing.Point point, double[] body) {
        String id = ids.of(prefix + (items.size() + 1));
        items.add(marks.mark(id, net, shape, drawn(point), body));
        terminal(point, id + "." + shape.terminal());
    }

    /** A terminal on a point of Eagle's sheet; the first one on a point is the one a wire reaches. */
    private void terminal(SheetTracing.Point point, String name) {
        if (terminals.putIfAbsent(point.key(), name) == null) {
            terminalPoints.add(point);
        }
    }

    // ─── documents ──────────────────────────────────────────────────────────────────────────────────

    private ImportedDocuments documents(String file, String title) {
        List<ImportedCircuit.Part> parts = new ArrayList<>();
        placed.values().forEach(part -> parts.add(new ImportedCircuit.Part(part.id(), part.component(), part.nets(), Map.of(),
                null, null).withValue(part.value())));
        ImportedCircuit circuit = new ImportedCircuit(parts, items, wires, List.of(), List.of(), globals, notes, declaredBuses.buses(), busLines);
        // What a design is built from, each before what names it: a footprint before the component landing on it.
        List<DocumentNode> library = new ArrayList<>(footprints.values());
        library.addAll(symbols.values());
        library.addAll(components.values());
        DocumentNode design = ImportedCircuitDocument.of(EagleNames.design(title), title, "Imported from Eagle: " + file, circuit,
                filing);
        return new ImportedDocuments(library, List.of(design), notes);
    }

    // ─── geometry ───────────────────────────────────────────────────────────────────────────────────

    /** Where a pin of a gate stands on Eagle's sheet. */
    private static SheetTracing.Point pinPoint(EagleSchematic.Instance instance, EagleLibrary.Pin pin) {
        double[] turned = EagleGeometry.turned(pin.x(), pin.y(), instance.rotation());
        return new SheetTracing.Point(instance.x() + turned[0], instance.y() + turned[1]);
    }

    /** The way a pin's line runs from where it connects into its body, on our sheet. */
    private static double[] sheetDirection(EagleRotation pin, EagleRotation instance) {
        double[] direction = EagleGeometry.direction(pin);
        double[] turned = EagleGeometry.turned(direction[0], direction[1], instance);
        return new double[] {turned[0], -turned[1]};
    }

    /** How far a sheet reaches in Eagle's frame: left, bottom, right, top. */
    private static double[] bounds(EagleSchematic.Sheet sheet) {
        double[] bounds = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        sheet.instances().forEach(instance -> reach(bounds, instance.x(), instance.y()));
        sheet.nets().forEach(net -> net.segments().forEach(segment -> segment.wires().forEach(wire -> {
            reach(bounds, wire.x1(), wire.y1());
            reach(bounds, wire.x2(), wire.y2());
        })));
        return bounds[0] > bounds[2] ? new double[] {0, 0, 0, 0} : bounds;
    }

    private static void reach(double[] bounds, double x, double y) {
        bounds[0] = Math.min(bounds[0], x);
        bounds[1] = Math.min(bounds[1], y);
        bounds[2] = Math.max(bounds[2], x);
        bounds[3] = Math.max(bounds[3], y);
    }

    private double[] drawn(SheetTracing.Point point) {
        return drawn(point.x(), point.y());
    }

    /** A point of Eagle's sheet, millimetres with Y upwards, in drawing units on ours: its sheet's place, Y downwards. */
    private double[] drawn(double x, double y) {
        return new double[] {ImportedCircuit.rounded((x - sheetLeft + sheetStart) * SymbolCanvas.UNITS_PER_MILLIMETRE),
                ImportedCircuit.rounded((top - y) * SymbolCanvas.UNITS_PER_MILLIMETRE)};
    }

    private static double gridDown(double millimetres) {
        return Math.floor(millimetres / EAGLE_GRID) * EAGLE_GRID;
    }

    private static double gridUp(double millimetres) {
        return Math.ceil(millimetres / EAGLE_GRID) * EAGLE_GRID;
    }

    private static double distance(SheetTracing.Point point, EagleSchematic.Point other) {
        return Math.hypot(point.x() - other.x(), point.y() - other.y());
    }

    // ─── lookups ────────────────────────────────────────────────────────────────────────────────────

    private EagleLibrary libraryOf(EagleSchematic.Part part) {
        return schematic.library(part.library()).orElseThrow(() -> new IllegalArgumentException(
                part.name() + " comes from the library " + part.library() + ", which the schematic does not carry."));
    }

    private static EagleLibrary.DeviceSet deviceSetOf(EagleLibrary library, EagleSchematic.Part part) {
        EagleLibrary.DeviceSet deviceSet = library.deviceSets().get(part.deviceSet());
        if (deviceSet == null) {
            throw new IllegalArgumentException(part.name() + " is the device set " + part.deviceSet() + ", which "
                    + library.name() + " does not carry.");
        }
        return deviceSet;
    }

    private static String pinKey(String part, String gate, String pin) {
        return part + "|" + gate + "|" + pin;
    }
}
