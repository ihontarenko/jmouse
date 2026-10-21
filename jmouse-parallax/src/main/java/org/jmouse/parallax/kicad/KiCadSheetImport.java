package org.jmouse.parallax.kicad;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.KiCadValue;
import org.jmouse.kicad.connectivity.KiCadConnection;
import org.jmouse.kicad.connectivity.KiCadConnectivity;
import org.jmouse.kicad.connectivity.KiCadNet;
import org.jmouse.kicad.schematic.KiCadLabel;
import org.jmouse.kicad.schematic.KiCadSchematic;
import org.jmouse.kicad.schematic.KiCadSheet;
import org.jmouse.kicad.schematic.KiCadSymbolInstance;
import org.jmouse.kicad.schematic.KiCadWire;
import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.PlacedItem;
import org.jmouse.parallax.object.ObjectDefinition.SchematicWire;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * One KiCad sheet as a jMX circuit, drawn where KiCad draws it.
 *
 * <ul>
 *   <li>A symbol the library map names is that component, drawn with the catalogue's living symbol set where KiCad's
 *       pins stand; any other is a part of the component {@code kicad-…}, which the catalogue does not hold until a
 *       model is written for it, drawn with KiCad's own symbol.</li>
 *   <li>A power symbol is a mark: KiCad's {@code GND} a ground of node 0, another ground a ground of its net, a rail a
 *       global net. A local label is a label, a hierarchical one a port, a global one a global net.</li>
 *   <li>A box placing another sheet is a part whose component is that sheet's circuit, its pins the sheet's ports.</li>
 *   <li>KiCad's wires, which meet at points, become wires from terminal to terminal through their corners; a point
 *       where three meet is a junction.</li>
 * </ul>
 */
public final class KiCadSheetImport {

    /** The span a transient of an imported sheet covers until its author sets one, seconds. */
    private static final double TIME = 0.02;

    /** Where a mark's symbol connects, in its own drawing, and which way its body goes from there. */
    private record MarkShape(String terminal, double pinY, double bodyY) {
    }

    private static final MarkShape GROUND = new MarkShape("ground", -20, 1);
    private static final MarkShape LABEL = new MarkShape("label", 20, -1);
    private static final MarkShape PORT = new MarkShape("label", 24, -1);
    private static final MarkShape GLOBAL = new MarkShape("label", 22, -1);

    /** What one sheet comes out as. */
    public record Sheet(ObjectDefinition.Circuit circuit, Set<SymbolUse> symbols, List<String> notes) {
    }

    /** A unit of a KiCad symbol the sheet draws with KiCad's own drawing, its pins named as the part names them. */
    public record SymbolUse(KiCadSymbolDefinition symbol, int unit, Map<String, String> pinNames) {
    }

    private final KiCadSchematic schematic;
    private final KiCadLibraryMap library;
    private final KiCadCatalogue catalogue;
    private final Function<String, String> sheetSlug;

    private final KiCadProject alone;
    private final String path;
    private final KiCadConnectivity connectivity;
    private final KiCadNetNames nets;
    private final Set<String> ids = new HashSet<>();
    private final List<CircuitElement> elements = new ArrayList<>();
    private final List<PlacedItem> items = new ArrayList<>();
    private final List<SchematicWire> wires = new ArrayList<>();
    private final Set<SymbolUse> symbols = new LinkedHashSet<>();
    private final List<String> notes = new ArrayList<>();
    private final List<ObjectDefinition.SheetPort> ports = new ArrayList<>();
    private final Set<String> globals = new LinkedHashSet<>();
    /** The terminal at each connection point, by the point's key, and where the terminal is drawn. */
    private final Map<String, Terminal> terminals = new LinkedHashMap<>();

    private record Terminal(String name, double[] drawn) {
    }

    private KiCadSheetImport(KiCadSchematic schematic, String instancePath, String ground, KiCadLibraryMap library,
                             KiCadCatalogue catalogue, Function<String, String> sheetSlug) {
        this.nets = new KiCadNetNames(ground);
        this.schematic = schematic;
        this.library = library;
        this.catalogue = catalogue;
        this.sheetSlug = sheetSlug;
        this.alone = KiCadProject.single(schematic, instancePath);
        this.path = alone.root().path();
        this.connectivity = KiCadConnectivity.of(alone);
    }

    /**
     * @param instancePath the path of the place the sheet stands in whose references the parts take: KiCad numbers
     *                     a sheet's parts for each place it stands, and the first place's numbers are the document's
     * @param ground       the KiCad net that is node 0: the design's reference ground
     * @param sheetSlug    the slug of the circuit a sheet file becomes, for a box placing it
     */
    public static Sheet of(KiCadSchematic schematic, String instancePath, String ground, KiCadLibraryMap library,
                           KiCadCatalogue catalogue, Function<String, String> sheetSlug) {
        KiCadSheetImport sheet = new KiCadSheetImport(schematic, instancePath, ground, library, catalogue, sheetSlug);
        sheet.parts();
        sheet.marks();
        sheet.boxes();
        sheet.wires();
        ObjectDefinition.SheetBoundary boundary = new ObjectDefinition.SheetBoundary(sheet.ports, sheet.globals);
        ObjectDefinition.Circuit circuit = new ObjectDefinition.Circuit(sheet.elements, new Interval(0, TIME),
                new ObjectDefinition.SchematicLayout(sheet.items, sheet.wires), null, List.of(), List.of(), null, boundary);
        return new Sheet(circuit, sheet.symbols, sheet.notes);
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    private void parts() {
        Map<String, String> pinNets = new HashMap<>();
        for (KiCadNet net : connectivity.nets()) {
            for (KiCadConnection pin : net.pins()) {
                pinNets.put(pin.symbol().uuid() + ":" + pin.pin().number(), nets.of(net.name()));
            }
        }
        Map<String, List<KiCadSymbolInstance>> byReference = new LinkedHashMap<>();
        for (KiCadSymbolInstance instance : schematic.symbols()) {
            Optional<KiCadSymbolDefinition> definition = schematic.definitionOf(instance);
            if (definition.isEmpty() || definition.get().power() || definition.get().pins().isEmpty()) {
                continue;
            }
            byReference.computeIfAbsent(instance.reference(path), ignored -> new ArrayList<>()).add(instance);
        }
        byReference.forEach((reference, units) -> part(reference, units, pinNets));
    }

    private void part(String reference, List<KiCadSymbolInstance> units, Map<String, String> pinNets) {
        KiCadSymbolInstance first = units.getFirst();
        KiCadSymbolDefinition definition = schematic.definitionOf(first).orElseThrow();
        String id = id(reference);
        Optional<KiCadLibraryMap.Mapping> mapping = library.mappingOf(first.libraryId());
        // The map's own numbering where it writes one — KiCad's diode is 1 = K, a catalogue diode's 1 is its anode —,
        // else the component's package numbers, which KiCad's numbers are.
        Map<String, String> numbered = mapping.map(found -> found.pins().isEmpty()
                ? catalogue.pinsByNumber(found.component()) : found.pins()).orElse(Map.of());
        Map<String, String> pins = new LinkedHashMap<>();
        Map<String, KiCadPoint> points = new LinkedHashMap<>();
        for (KiCadSymbolInstance unit : units) {
            for (KiCadPin pin : definition.pinsOf(unit.unit())) {
                String name = numbered.getOrDefault(pin.number(), KiCadSymbolDrawing.pinName(pin));
                String net = pinNets.getOrDefault(unit.uuid() + ":" + pin.number(), nets.of(id + "_" + name + "_open"));
                pins.putIfAbsent(name, net);
                if (unit == first) {
                    points.putIfAbsent(name, unit.placement().place(pin.at()));
                }
            }
        }
        if (units.size() > 1) {
            notes.add(id + " is drawn by its unit " + KiCadSymbolDrawing.unitLetter(first.unit()) + " alone: a jMX sheet places "
                    + "a part once, and its other units are wired by their nets.");
        }
        Map<String, String> overrides = new LinkedHashMap<>();
        Optional<KiCadCatalogue.PartDrawing> drawn = Optional.empty();
        String component;
        String symbol = null;
        if (mapping.isPresent()) {
            component = mapping.get().component();
            mapping.get().value().ifPresent(parameter -> KiCadValue.number(first.value()).ifPresentOrElse(
                    number -> overrides.put(parameter, plain(number)),
                    () -> notes.add(id + ": its value '" + first.value() + "' is not a number, so " + parameter
                            + " keeps the component's own.")));
            drawn = catalogue.drawingOf(component);
        } else {
            component = KiCadSymbolDrawing.componentSlugOf(definition);
            notes.add(id + " (" + first.libraryId() + ") has no component in the library map: it is drawn, and a model "
                    + "named '" + component + "' is what simulating it needs.");
        }
        if (drawn.isPresent()) {
            placeAligned(id, drawn.get(), points);
        } else if (mapping.isPresent()) {
            // A part is drawn only with a symbol its component offers: one with none stands where KiCad places it.
            notes.add(id + ": " + component + " has no symbol of its own, so it stands where KiCad places it, drawn as "
                    + "its component is.");
            placeAsKiCad(id, first, points);
        } else {
            symbol = KiCadSymbolDrawing.slugOf(definition, first.unit());
            symbols.add(new SymbolUse(definition, first.unit(), Map.of()));
            placeAsKiCad(id, first, points);
        }
        elements.add(new CircuitElement(id, ObjectDefinition.CircuitElementType.MODEL, null, null, "", component, pins,
                overrides, symbol));
    }

    /** A part drawn with KiCad's symbol: the same origin, turned and mirrored as KiCad turns and mirrors it. */
    private void placeAsKiCad(String id, KiCadSymbolInstance instance, Map<String, KiCadPoint> points) {
        double[] at = drawn(instance.placement().at());
        int angle = instance.placement().angle();
        double rotation;
        boolean mirrored;
        switch (instance.placement().mirror()) {
            case Y -> {
                rotation = angle;
                mirrored = true;
            }
            case X -> {
                rotation = (angle + 180) % 360;
                mirrored = true;
            }
            default -> {
                rotation = (360 - angle) % 360;
                mirrored = false;
            }
        }
        items.add(new PlacedItem(id, null, null, at[0], at[1], rotation, mirrored));
        points.forEach((pin, point) -> terminal(point, id + "." + pin, drawn(point)));
    }

    /**
     * A part drawn with the catalogue's symbol, turned, mirrored and moved so its pins stand where KiCad's do: of the
     * eight ways it can stand, the one whose pins are nearest KiCad's.
     */
    private void placeAligned(String id, KiCadCatalogue.PartDrawing drawing, Map<String, KiCadPoint> points) {
        // A block's pin, A.output, stands where the symbol draws its pin, output: a symbol draws one block.
        Map<String, double[]> local = new LinkedHashMap<>();
        points.keySet().forEach(pin -> {
            double[] point = drawing.pins().get(pin.replaceFirst("^[A-Z]\\.", ""));
            if (point != null) {
                local.put(pin, point);
            }
        });
        List<String> shared = List.copyOf(local.keySet());
        double bestError = Double.MAX_VALUE;
        double[] best = {0, 0, 0, 0};
        for (int quarter = 0; quarter < 4; quarter++) {
            for (int flip = 0; flip < 2; flip++) {
                double rotation = quarter * 90;
                boolean mirrored = flip == 1;
                double sumX = 0;
                double sumY = 0;
                for (String pin : shared) {
                    double[] kicad = drawn(points.get(pin));
                    double[] turned = turned(local.get(pin), rotation, mirrored);
                    sumX += kicad[0] - turned[0];
                    sumY += kicad[1] - turned[1];
                }
                double x = shared.isEmpty() ? 0 : sumX / shared.size();
                double y = shared.isEmpty() ? 0 : sumY / shared.size();
                double error = 0;
                for (String pin : shared) {
                    double[] kicad = drawn(points.get(pin));
                    double[] turned = turned(local.get(pin), rotation, mirrored);
                    error += Math.pow(x + turned[0] - kicad[0], 2) + Math.pow(y + turned[1] - kicad[1], 2);
                }
                if (error < bestError - 1e-9) {
                    bestError = error;
                    best = new double[] {x, y, rotation, flip};
                }
            }
        }
        double x = grid(best[0]);
        double y = grid(best[1]);
        boolean mirrored = best[3] == 1;
        double rotation = best[2];
        items.add(new PlacedItem(id, null, null, x, y, rotation, mirrored));
        points.forEach((pin, point) -> {
            double[] onSymbol = local.get(pin);
            double[] turned = onSymbol == null ? new double[] {0, 0} : turned(onSymbol, rotation, mirrored);
            terminal(point, id + "." + pin, new double[] {x + turned[0], y + turned[1]});
        });
    }

    /** A point of a symbol on the sheet as jMX turns it: mirrored left to right first, then turned clockwise. */
    private static double[] turned(double[] point, double rotation, boolean mirrored) {
        double radians = Math.toRadians(rotation);
        double localX = mirrored ? -point[0] : point[0];
        return new double[] {localX * Math.cos(radians) - point[1] * Math.sin(radians),
                localX * Math.sin(radians) + point[1] * Math.cos(radians)};
    }

    // ─── marks ──────────────────────────────────────────────────────────────────────────────────────

    private void marks() {
        for (KiCadSymbolInstance instance : schematic.symbols()) {
            Optional<KiCadSymbolDefinition> definition = schematic.definitionOf(instance);
            if (definition.isEmpty() || !definition.get().power() || definition.get().pins().isEmpty()) {
                continue;
            }
            KiCadPin pin = definition.get().pins().getFirst();
            KiCadPoint point = instance.placement().place(pin.at());
            double radians = Math.toRadians(pin.angle());
            KiCadPoint into = instance.placement().place(new KiCadPoint(pin.at().x() + Math.cos(radians),
                    pin.at().y() + Math.sin(radians)));
            double[] body = {into.x() - point.x(), into.y() - point.y()};
            String value = instance.value();
            String net = nets.of(value);
            boolean ground = value.toUpperCase().startsWith("GND") || value.equalsIgnoreCase("VSS");
            if (ground) {
                mark("ground", "ground", net.equals(ObjectDefinition.Circuit.GROUND) ? null : net, GROUND, point, body);
            } else {
                globals.add(net);
                mark("power", "global", net, GLOBAL, point, body);
            }
        }
        for (KiCadLabel label : schematic.labels()) {
            String net = connectivity.netAt(path, label.at()).map(nets::of).orElse(nets.of(label.text()));
            double radians = Math.toRadians(label.angle());
            double[] body = {Math.cos(radians), -Math.sin(radians)};
            switch (label.kind()) {
                case LOCAL -> mark("label", "label", net, LABEL, label.at(), body);
                case GLOBAL -> {
                    globals.add(net);
                    mark("global", "global", net, GLOBAL, label.at(), body);
                }
                case HIERARCHICAL -> {
                    if (ports.stream().noneMatch(port -> port.net().equals(net))) {
                        ports.add(new ObjectDefinition.SheetPort(net, "analog"));
                    }
                    mark("port", "port", net, PORT, label.at(), body);
                }
            }
        }
        globals.removeIf(net -> ports.stream().anyMatch(port -> port.net().equals(net)));
        for (KiCadPoint junction : schematic.junctions()) {
            junction(junction);
        }
    }

    /**
     * A mark whose symbol connects at a point and whose body goes one way from it: turned so its body goes the way
     * KiCad's does, moved so its pin stands on KiCad's point.
     */
    private void mark(String prefix, String mark, String net, MarkShape shape, KiCadPoint point, double[] body) {
        double rotation = rotationTowards(body, shape.bodyY());
        double[] pin = turned(new double[] {0, shape.pinY()}, rotation, false);
        double[] at = drawn(point);
        String id = id(prefix + (items.size() + 1));
        items.add(new PlacedItem(id, mark, net, at[0] - pin[0], at[1] - pin[1], rotation, false));
        terminal(point, id + "." + shape.terminal(), at);
    }

    /** The quarter turn that points a body drawn along Y ({@code bodyY} is its sign) along a direction on the sheet. */
    private static double rotationTowards(double[] direction, double bodyY) {
        double best = 0;
        double bestDot = -Double.MAX_VALUE;
        for (int quarter = 0; quarter < 4; quarter++) {
            double[] turned = turned(new double[] {0, bodyY}, quarter * 90, false);
            double dot = turned[0] * direction[0] + turned[1] * direction[1];
            if (dot > bestDot) {
                bestDot = dot;
                best = quarter * 90;
            }
        }
        return best;
    }

    private void junction(KiCadPoint point) {
        if (terminals.containsKey(point.key())) {
            return;
        }
        double[] at = drawn(point);
        String id = id("junction" + (items.size() + 1));
        items.add(new PlacedItem(id, "junction", null, at[0], at[1], 0, false));
        terminal(point, id + ".j", at);
    }

    // ─── sheets placed as blocks ────────────────────────────────────────────────────────────────────

    private void boxes() {
        for (KiCadSheet box : schematic.sheets()) {
            String id = id(box.name().isBlank() ? "sheet" : box.name());
            Map<String, String> pins = new LinkedHashMap<>();
            for (KiCadSheet.Pin pin : box.pins()) {
                String port = nets.of(pin.name());
                String net = connectivity.netAt(path, pin.at()).map(nets::of).orElse(nets.of(id + "_" + port + "_open"));
                pins.put(port, net);
                terminal(pin.at(), id + "." + port, drawn(pin.at()));
            }
            elements.add(new CircuitElement(id, ObjectDefinition.CircuitElementType.MODEL, null, null, "",
                    sheetSlug.apply(box.file()), pins, Map.of()));
            double[] at = drawn(box.at());
            double[] size = {box.size().x() * KiCadSymbolDrawing.UNITS_PER_MILLIMETRE,
                    box.size().y() * KiCadSymbolDrawing.UNITS_PER_MILLIMETRE};
            items.add(new PlacedItem(id, null, null, grid(at[0] + size[0] / 2), grid(at[1] + size[1] / 2), 0, false));
        }
    }

    // ─── wires ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * KiCad's wires as jMX's: segments are cut wherever a connection point lies on them, and every chain of segments
     * between two terminals is one wire whose corners are its bends. A point where three segments meet with no
     * terminal on it is a junction; so is the loose end of a wire.
     */
    private void wires() {
        Map<String, KiCadPoint> places = new LinkedHashMap<>();
        Map<String, Set<String>> neighbours = new LinkedHashMap<>();
        List<KiCadPoint> stops = new ArrayList<>();
        terminals.keySet().forEach(key -> stops.add(pointOf(key)));
        for (KiCadWire wire : schematic.wires()) {
            for (KiCadPoint corner : wire.points()) {
                stops.add(corner);
            }
        }
        for (KiCadWire wire : schematic.wires()) {
            for (int index = 1; index < wire.points().size(); index++) {
                KiCadPoint start = wire.points().get(index - 1);
                KiCadPoint end = wire.points().get(index);
                List<KiCadPoint> along = new ArrayList<>();
                for (KiCadPoint stop : stops) {
                    if (stop.liesOn(start, end) && !stop.sameAs(start) && !stop.sameAs(end)) {
                        along.add(stop);
                    }
                }
                along.sort((one, other) -> Double.compare(distance(start, one), distance(start, other)));
                KiCadPoint previous = start;
                along.add(end);
                for (KiCadPoint next : along) {
                    if (!next.sameAs(previous)) {
                        places.put(previous.key(), previous);
                        places.put(next.key(), next);
                        neighbours.computeIfAbsent(previous.key(), ignored -> new LinkedHashSet<>()).add(next.key());
                        neighbours.computeIfAbsent(next.key(), ignored -> new LinkedHashSet<>()).add(previous.key());
                    }
                    previous = next;
                }
            }
        }
        // A meeting of three or more, and a loose end, stand for a terminal of their own.
        neighbours.forEach((key, around) -> {
            if (!terminals.containsKey(key) && around.size() != 2) {
                junction(places.get(key));
            }
        });
        Set<String> walked = new HashSet<>();
        for (String start : new ArrayList<>(terminals.keySet())) {
            for (String first : neighbours.getOrDefault(start, Set.of())) {
                if (walked.contains(edge(start, first))) {
                    continue;
                }
                List<double[]> bends = new ArrayList<>();
                String previous = start;
                String current = first;
                walked.add(edge(previous, current));
                while (!terminals.containsKey(current)) {
                    bends.add(drawn(places.get(current)));
                    String came = previous;
                    Optional<String> next = neighbours.get(current).stream().filter(key -> !key.equals(came)).findFirst();
                    if (next.isEmpty()) {
                        break;
                    }
                    previous = current;
                    current = next.get();
                    walked.add(edge(previous, current));
                }
                if (terminals.containsKey(current) && !current.equals(start)) {
                    wire(terminals.get(start), drawn(places.get(start)), bends, terminals.get(current),
                            drawn(places.get(current)));
                }
            }
        }
    }

    /** A wire between two terminals; where a terminal is drawn off KiCad's point, the wire passes KiCad's point. */
    private void wire(Terminal from, double[] fromPoint, List<double[]> bends, Terminal to, double[] toPoint) {
        List<ObjectDefinition.Bend> through = new ArrayList<>();
        if (!same(from.drawn(), fromPoint)) {
            through.add(new ObjectDefinition.Bend(fromPoint[0], fromPoint[1]));
        }
        bends.forEach(bend -> through.add(new ObjectDefinition.Bend(bend[0], bend[1])));
        if (!same(to.drawn(), toPoint)) {
            through.add(new ObjectDefinition.Bend(toPoint[0], toPoint[1]));
        }
        wires.add(new SchematicWire("W" + (wires.size() + 1), from.name(), to.name(), through));
    }

    private static boolean same(double[] one, double[] other) {
        return Math.abs(one[0] - other[0]) < 0.01 && Math.abs(one[1] - other[1]) < 0.01;
    }

    private static String edge(String one, String other) {
        return one.compareTo(other) < 0 ? one + "|" + other : other + "|" + one;
    }

    private static double distance(KiCadPoint one, KiCadPoint other) {
        return Math.hypot(one.x() - other.x(), one.y() - other.y());
    }

    // ─── helpers ────────────────────────────────────────────────────────────────────────────────────

    private final Map<String, KiCadPoint> terminalPoints = new HashMap<>();

    private void terminal(KiCadPoint point, String name, double[] drawn) {
        terminals.putIfAbsent(point.key(), new Terminal(name, drawn));
        terminalPoints.putIfAbsent(point.key(), point);
    }

    private KiCadPoint pointOf(String key) {
        return terminalPoints.get(key);
    }

    /** A KiCad sheet point in drawing units: the same 1.27 mm grid as twenty units, Y downwards on both. */
    private static double[] drawn(KiCadPoint point) {
        return new double[] {rounded(point.x() * KiCadSymbolDrawing.UNITS_PER_MILLIMETRE),
                rounded(point.y() * KiCadSymbolDrawing.UNITS_PER_MILLIMETRE)};
    }

    private static double grid(double value) {
        return Math.round(value / 10) * 10;
    }

    private static double rounded(double value) {
        return Math.round(value * 100) / 100.0;
    }

    /** An id no other part or mark of the sheet has: KiCad's reference where it is a name. */
    private String id(String wanted) {
        String base = wanted.replaceAll("[^A-Za-z0-9_]", "_");
        if (base.isEmpty() || !Character.isLetter(base.charAt(0))) {
            base = "P" + base;
        }
        base = base.length() > 28 ? base.substring(0, 28) : base;
        String id = base;
        for (int suffix = 2; !ids.add(id); suffix++) {
            id = base + "_" + suffix;
        }
        return id;
    }

    private static String plain(double number) {
        return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }
}
