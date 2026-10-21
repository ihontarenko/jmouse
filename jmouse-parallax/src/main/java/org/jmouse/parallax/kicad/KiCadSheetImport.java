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
import java.util.Comparator;
import java.util.Collection;
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

    /** KiCad's standard libraries of connectors start so: {@code Connector:}, {@code Connector_Generic:}. */
    private static final String CONNECTOR_LIBRARY = "Connector";

    /** How many divisions a rail stands up from the screen's middle at the scale chosen for it. */
    private static final double RAIL_DIVISIONS = 3;

    /** The oscilloscope's channels an import sets up, in the order the rails are given to them. */
    private static final List<String> CHANNELS = List.of("CH1", "CH2");

    /** The sheet's grid in drawing units: KiCad's 1.27 mm grid, which every symbol's pins stand on. */
    private static final double GRID = 20;

    /**
     * How far from its KiCad point a pin may be drawn and still be wired to it, along both axes: a catalogue symbol
     * shaped otherwise than KiCad's puts a pin a grid step or three away; a pin further off is on another unit's place.
     */
    private static final double LEAD_LIMIT = 6 * GRID;

    /** Where a mark's symbol connects, in its own drawing, and which way its body goes from there. */
    private record MarkShape(String terminal, double pinY, double bodyY) {
    }

    private static final MarkShape GROUND = new MarkShape("ground", -20, 1);
    private static final MarkShape LABEL = new MarkShape("label", 20, -1);
    private static final MarkShape PORT = new MarkShape("label", 24, -1);
    private static final MarkShape GLOBAL = new MarkShape("label", 22, -1);

    /**
     * What one sheet comes out as.
     *
     * @param components the KiCad symbols the library map names no component for: each needs one, made from the symbol
     *                   ({@link KiCadComponentStub}) where the catalogue has none
     */
    public record Sheet(ObjectDefinition.Circuit circuit, Set<SymbolUse> symbols, Set<KiCadSymbolDefinition> components,
                        List<String> notes, Map<String, String> footprints) {
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
    private final Set<KiCadSymbolDefinition> components = new LinkedHashSet<>();
    /** Each part's KiCad footprint, by the part's id: the library id its {@code Footprint} property names. */
    private final Map<String, String> partFootprints = new LinkedHashMap<>();
    /** The parts of each symbol the library map names no component for, by what was made for them: one note each. */
    private final Map<String, List<String>> unmapped = new LinkedHashMap<>();
    /** The rails KiCad's power symbols name, by net, with the voltage their value says: {@code +48V} is 48. */
    private final Map<String, Double> rails = new LinkedHashMap<>();
    /** The parts a design is plugged into the world through — KiCad's connectors —, by reference. */
    private final List<String> connectors = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final List<ObjectDefinition.SheetPort> ports = new ArrayList<>();
    private final Set<String> globals = new LinkedHashSet<>();
    /** The terminal at each connection point, by the point's key, and where the terminal is drawn. */
    private final Map<String, Terminal> terminals = new LinkedHashMap<>();

    /**
     * A terminal of the drawing and where it is drawn; for a part's pin, also where the part stands, which says the way
     * the pin leaves its body — the way a stub leads a wire into it.
     */
    private record Terminal(String name, double[] drawn, double[] partCentre) {
    }

    /** Two terminals KiCad joins by touching, pin on pin, with no wire between them: the drawing gets one. */
    private record Touching(Terminal first, Terminal second, KiCadPoint point) {
    }

    private final List<Touching> touching = new ArrayList<>();

    /** The label a far pin is reached by, once per pin. */
    private final Map<String, Terminal> pinLabels = new HashMap<>();

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
        sheet.touchingWires();
        ObjectDefinition.SheetBoundary boundary = new ObjectDefinition.SheetBoundary(sheet.ports, sheet.globals);
        sheet.noteConnectors();
        ObjectDefinition.Circuit circuit = new ObjectDefinition.Circuit(sheet.elements, new Interval(0, TIME),
                new ObjectDefinition.SchematicLayout(sheet.items, sheet.wires), null, sheet.railProbes(), List.of(), null,
                boundary);
        return new Sheet(circuit, sheet.symbols, sheet.components, sheet.notes, Map.copyOf(sheet.partFootprints));
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
        unmapped.forEach((made, parts) -> notes.add(String.join(", ", parts) + ": " + made + ", made from KiCad's symbol "
                + "— its pins, and its drawing unless the map names one of ours —, since the library map names no component "
                + "for it; it computes nothing until its model is written."));
    }

    private void part(String reference, List<KiCadSymbolInstance> units, Map<String, String> pinNets) {
        KiCadSymbolInstance first = units.getFirst();
        KiCadSymbolDefinition definition = schematic.definitionOf(first).orElseThrow();
        String id = id(reference);
        Optional<KiCadLibraryMap.Mapping> mapping = library.mappingOf(first.libraryId());
        // With no model, the drawing the map names for it, when that drawing places every pin.
        Optional<KiCadLibraryMap.Drawing> ownDrawing = mapping.isPresent() ? Optional.empty()
                : KiCadComponentStub.drawnWith(definition, library, catalogue);
        if (first.libraryId().startsWith(CONNECTOR_LIBRARY)) {
            connectors.add(id);
        }
        // The map's own numbering where it writes one — KiCad's diode is 1 = K, a catalogue diode's 1 is its anode —,
        // else the component's package numbers, which KiCad's numbers are.
        Map<String, String> numbered = mapping.map(found -> found.pins().isEmpty()
                ? catalogue.pinsByNumber(found.component()) : found.pins())
                .orElseGet(() -> ownDrawing.map(KiCadLibraryMap.Drawing::pins).orElse(Map.of()));
        Map<String, String> pins = new LinkedHashMap<>();
        // Where each KiCad unit's pins stand on the sheet, by the name the part gives them: `A.output`, `Vcc`.
        Map<KiCadSymbolInstance, Map<String, KiCadPoint>> unitPoints = new LinkedHashMap<>();
        for (KiCadSymbolInstance unit : units) {
            Map<String, KiCadPoint> placed = unitPoints.computeIfAbsent(unit, ignored -> new LinkedHashMap<>());
            for (KiCadPin pin : definition.pinsOf(unit.unit())) {
                String name = numbered.getOrDefault(pin.number(), KiCadSymbolDrawing.pinName(pin));
                String net = pinNets.getOrDefault(unit.uuid() + ":" + pin.number(), nets.of(id + "_" + name + "_open"));
                pins.putIfAbsent(name, net);
                // A pin not connected inside the part has no place on a drawing of ours, which draws what connects.
                if (ownDrawing.isPresent() && !numbered.containsKey(pin.number())) {
                    continue;
                }
                placed.putIfAbsent(name, unit.placement().place(pin.at()));
            }
        }
        Map<String, KiCadPoint> points = unitPoints.get(first);
        String footprintLibraryId = first.property("Footprint").strip();
        if (!footprintLibraryId.isEmpty()) {
            partFootprints.put(id, footprintLibraryId);
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
            // Of the symbols the component offers, the one whose pins stand most where KiCad's do: a catalogue symbol
            // drawn smaller than KiCad's lands none, and every wire into it would come by a label.
            List<KiCadCatalogue.PartDrawing> offered = catalogue.drawingsOf(component);
            drawn = offered.stream().max(Comparator.comparingDouble(candidate -> fit(candidate, unitPoints.get(first))));
            if (drawn.isPresent() && !offered.isEmpty() && drawn.get() != offered.getFirst()) {
                symbol = drawn.get().symbol();
            }
        } else {
            component = KiCadSymbolDrawing.componentSlugOf(definition);
            components.add(definition);
            drawn = ownDrawing.flatMap(found -> catalogue.symbolDrawing(found.symbol()));
            unmapped.computeIfAbsent(first.libraryId() + " → '" + component + "'"
                    + drawn.map(found -> " drawn with " + found.symbol()).orElse(""), ignored -> new ArrayList<>()).add(id);
        }
        if (drawn.isPresent()) {
            placeUnits(id, drawn.get(), first, unitPoints);
        } else if (mapping.isPresent()) {
            // A part is drawn only with a symbol its component offers: one with none stands where KiCad places it.
            notes.add(id + ": " + component + " has no symbol of its own, so it stands where KiCad places it, drawn as "
                    + "its component is.");
            placeAsKiCad(id, first, points);
        } else {
            // A chip is drawn as our box: its part names no symbol. Only a design's own small symbol is drawn as it came.
            if (!KiCadComponentStub.boxed(definition, library)) {
                symbol = KiCadSymbolDrawing.slugOf(definition, first.unit());
                symbols.add(new SymbolUse(definition, first.unit(), Map.of()));
            }
            placeAsKiCad(id, first, points);
        }
        if (units.size() > 1 && drawn.isEmpty()) {
            notes.add(id + " is drawn by its unit " + KiCadSymbolDrawing.unitLetter(first.unit()) + " alone: KiCad draws "
                    + "each of its units apart and a part is drawn with one symbol; the other units are wired by their nets.");
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
        // The symbol is drawn around the middle of its body, not KiCad's origin: the part stands that far away, turned.
        double[] centre = turned(KiCadSymbolDrawing.centreOf(schematic.definitionOf(instance).orElseThrow(), instance.unit()),
                rotation, mirrored);
        items.add(new PlacedItem(id, null, null, at[0] + centre[0], at[1] + centre[1], rotation, mirrored));
        points.forEach((pin, point) -> terminal(point, id + "." + pin, drawn(point)));
    }

    /**
     * A part of the catalogue drawn where KiCad draws its units. The first unit KiCad places is the part's own line; every
     * other whose pins all belong to one unit of the component — {@code B.output}, {@code B.inverting_input} — is placed
     * on its own, {@code U4.B at …}, with the same symbol; a unit of shared pins alone (KiCad's power unit) is drawn by the
     * part's symbol, which carries those pins, and its wires are led there.
     */
    private void placeUnits(String id, KiCadCatalogue.PartDrawing drawing, KiCadSymbolInstance first,
                            Map<KiCadSymbolInstance, Map<String, KiCadPoint>> unitPoints) {
        Map<String, KiCadPoint> own = unitPoints.get(first);
        Optional<String> partUnit = componentUnitOf(own.keySet());
        double[] part = placeAligned(id, null, drawing, own);
        Map<String, KiCadPoint> shared = new LinkedHashMap<>();
        unitPoints.forEach((unit, points) -> {
            if (unit == first) {
                return;
            }
            Optional<String> letter = componentUnitOf(points.keySet());
            if (letter.isPresent() && !letter.equals(partUnit)) {
                placeAligned(id, letter.get(), drawing, points);
            } else {
                points.forEach(shared::putIfAbsent);
            }
        });
        shared.forEach((pin, point) -> {
            double[] onSymbol = drawing.pins().get(symbolPinOf(pin));
            double[] turned = onSymbol == null ? new double[] {0, 0} : turned(onSymbol, part[2], part[3] == 1);
            terminal(point, id + "." + pin, new double[] {part[0] + turned[0], part[1] + turned[1]}, new double[] {part[0], part[1]});
        });
    }

    /** The one unit of the component a KiCad unit's pins belong to ({@code B} for {@code B.output}), when they all do. */
    private static Optional<String> componentUnitOf(Collection<String> pins) {
        Set<String> letters = new HashSet<>();
        for (String pin : pins) {
            int dot = pin.indexOf('.');
            if (dot > 0) {
                letters.add(pin.substring(0, dot));
            }
        }
        return letters.size() == 1 ? Optional.of(letters.iterator().next()) : Optional.empty();
    }

    /** A unit's pin, {@code A.output}, is the pin the symbol draws, {@code output}: a symbol draws one unit. */
    private static String symbolPinOf(String pin) {
        return pin.substring(pin.indexOf('.') + 1);
    }

    /**
     * A part, or one of its units, drawn with the catalogue's symbol, turned, mirrored and moved so its pins stand where
     * KiCad's do: of the eight ways it can stand, the one whose pins are nearest KiCad's.
     *
     * @param unit the unit placed on its own, {@code B}; null for the part's own line
     * @return where it stands: x, y, rotation, and 1 when mirrored
     */
    private double[] placeAligned(String id, String unit, KiCadCatalogue.PartDrawing drawing, Map<String, KiCadPoint> points) {
        double[] best = alignment(drawing, points);
        double x = best[0];
        double y = best[1];
        boolean mirrored = best[3] == 1;
        double rotation = best[2];
        Map<String, double[]> local = localPins(drawing, points);
        items.add(new PlacedItem(id, null, null, x, y, rotation, mirrored, Map.of(), List.of(), unit));
        points.forEach((pin, point) -> {
            double[] onSymbol = local.get(pin);
            double[] turned = onSymbol == null ? new double[] {0, 0} : turned(onSymbol, rotation, mirrored);
            terminal(point, id + "." + pin, new double[] {x + turned[0], y + turned[1]}, new double[] {x, y});
        });
        return new double[] {x, y, rotation, mirrored ? 1 : 0};
    }

    /** How well a symbol can stand where KiCad's pins are: the pins it lands, less a little for the distance of the rest. */
    private static double fit(KiCadCatalogue.PartDrawing drawing, Map<String, KiCadPoint> points) {
        double[] best = alignment(drawing, points);
        return best[4] - best[5] / 1e9;
    }

    /** A symbol's pins that stand for these KiCad pins: `A.output` is the symbol's `output`. */
    private static Map<String, double[]> localPins(KiCadCatalogue.PartDrawing drawing, Map<String, KiCadPoint> points) {
        Map<String, double[]> local = new LinkedHashMap<>();
        points.keySet().forEach(pin -> {
            double[] point = drawing.pins().get(symbolPinOf(pin));
            if (point != null) {
                local.put(pin, point);
            }
        });
        return local;
    }

    /** Where a symbol stands best for these KiCad pins: x, y, rotation, 1 when mirrored, the pins landed, the error left. */
    private static double[] alignment(KiCadCatalogue.PartDrawing drawing, Map<String, KiCadPoint> points) {
        Map<String, double[]> local = new LinkedHashMap<>();
        points.keySet().forEach(pin -> {
            double[] point = drawing.pins().get(symbolPinOf(pin));
            if (point != null) {
                local.put(pin, point);
            }
        });
        List<String> shared = List.copyOf(local.keySet());
        // Of the eight ways the symbol can stand, and of the places on the grid it can stand at, the one where most of its
        // pins land on KiCad's exactly; then the one nearest the rest. A pin that does not land is led to by a short
        // stub, so a place on the grid matters more than a mean that lands none.
        int bestLanded = -1;
        double bestError = Double.MAX_VALUE;
        double[] best = {0, 0, 0, 0};
        for (int quarter = 0; quarter < 4; quarter++) {
            for (int flip = 0; flip < 2; flip++) {
                double rotation = quarter * 90;
                boolean mirrored = flip == 1;
                for (double[] place : candidatePlaces(shared, local, points, rotation, mirrored)) {
                    int landed = 0;
                    double error = 0;
                    boolean onAnother = false;
                    for (String pin : shared) {
                        double[] kicad = drawn(points.get(pin));
                        double[] turned = turned(local.get(pin), rotation, mirrored);
                        double[] at = {place[0] + turned[0], place[1] + turned[1]};
                        double distance = Math.hypot(at[0] - kicad[0], at[1] - kicad[1]);
                        if (distance < 0.01) {
                            landed++;
                        }
                        error += distance * distance;
                        // A pin on the point where KiCad draws another of the part's pins would be joined to that one's net.
                        for (String another : shared) {
                            // Pins KiCad stacks on one point (two grounds) land on each other by design.
                            if (!another.equals(pin) && same(at, drawn(points.get(another))) && !same(kicad, drawn(points.get(another)))) {
                                onAnother = true;
                            }
                        }
                    }
                    if (onAnother) {
                        landed = -1;
                    }
                    if (landed > bestLanded || (landed == bestLanded && error < bestError - 1e-9)) {
                        bestLanded = landed;
                        bestError = error;
                        best = new double[] {place[0], place[1], rotation, flip};
                    }
                }
            }
        }
        return new double[] {best[0], best[1], best[2], best[3], bestLanded, bestError};
    }

    /**
     * Where a symbol turned this way may stand: on the grid, wherever one of its pins lands on its KiCad point, and the
     * grid point nearest the place that suits all pins on average.
     */
    private static List<double[]> candidatePlaces(List<String> pins, Map<String, double[]> local, Map<String, KiCadPoint> points,
                                                  double rotation, boolean mirrored) {
        List<double[]> places = new ArrayList<>();
        double sumX = 0;
        double sumY = 0;
        for (String pin : pins) {
            double[] kicad = drawn(points.get(pin));
            double[] turned = turned(local.get(pin), rotation, mirrored);
            places.add(new double[] {grid(kicad[0] - turned[0]), grid(kicad[1] - turned[1])});
            sumX += kicad[0] - turned[0];
            sumY += kicad[1] - turned[1];
        }
        if (!pins.isEmpty()) {
            places.add(new double[] {grid(sumX / pins.size()), grid(sumY / pins.size())});
        } else {
            places.add(new double[] {0, 0});
        }
        return places;
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
                KiCadValue.volts(value).ifPresent(volts -> rails.putIfAbsent(net, volts));
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
            // Drawn the size KiCad draws it, on the grid, so its name and its sheet stand where they stood there.
            ObjectDefinition.BlockSize drawnSize = new ObjectDefinition.BlockSize(Math.max(GRID, grid(size[0])),
                    Math.max(GRID, grid(size[1])));
            items.add(new PlacedItem(id, null, null, grid(at[0] + size[0] / 2), grid(at[1] + size[1] / 2), 0, false,
                    Map.of(), List.of(), null, drawnSize));
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

    /**
     * A wire between two terminals. Where a terminal is drawn off KiCad's point — a catalogue symbol is shaped otherwise
     * than KiCad's —, the wire still passes KiCad's point and is led from it into the pin by a stub of straight stretches.
     */
    private void wire(Terminal from, double[] fromPoint, List<double[]> bends, Terminal to, double[] toPoint) {
        from = byNameWhenFar(from, fromPoint);
        to = byNameWhenFar(to, toPoint);
        List<double[]> path = new ArrayList<>(lead(from, fromPoint).reversed());
        path.addAll(bends);
        path.addAll(lead(to, toPoint));
        List<ObjectDefinition.Bend> through = new ArrayList<>();
        double[] previous = from.drawn();
        for (double[] point : path) {
            if (!same(point, previous)) {
                through.add(new ObjectDefinition.Bend(point[0], point[1]));
                previous = point;
            }
        }
        if (!through.isEmpty() && same(previous, to.drawn())) {
            through.removeLast();
        }
        wires.add(new SchematicWire("W" + (wires.size() + 1), from.name(), to.name(), through));
    }

    /**
     * The corners a wire takes from KiCad's point into a terminal drawn elsewhere, the terminal itself left out: none when
     * it stands on the point, KiCad's point when they share a line, else KiCad's point and one corner — chosen so the
     * last stretch enters the pin the way the pin leaves its part's body, as a schematic draws a lead.
     */
    private List<double[]> lead(Terminal terminal, double[] kicadPoint) {
        double[] pin = terminal.drawn();
        if (same(pin, kicadPoint)) {
            return List.of();
        }
        if (Math.abs(pin[0] - kicadPoint[0]) < 0.01 || Math.abs(pin[1] - kicadPoint[1]) < 0.01) {
            return List.of(kicadPoint);
        }
        List<List<double[]>> candidates = leadCandidates(terminal, kicadPoint);
        return candidates.stream().filter(candidate -> clear(candidate, terminal)).findFirst().orElse(candidates.getFirst());
    }

    /**
     * The ways a stub may run from KiCad's point into a pin, simplest first: one corner, entering the pin the way it leaves
     * its body; the other corner; then a jog out along the pin's own direction, one to three grid steps clear of the body,
     * so a stub hemmed in by its neighbours' pins still finds a free lane. Every corner is on the grid.
     */
    private static List<List<double[]>> leadCandidates(Terminal terminal, double[] kicadPoint) {
        double[] pin = terminal.drawn();
        double[] centre = terminal.partCentre();
        boolean sideways = centre == null || Math.abs(pin[0] - centre[0]) >= Math.abs(pin[1] - centre[1]);
        double[] preferred = sideways ? new double[] {kicadPoint[0], pin[1]} : new double[] {pin[0], kicadPoint[1]};
        double[] other = sideways ? new double[] {pin[0], kicadPoint[1]} : new double[] {kicadPoint[0], pin[1]};
        List<List<double[]>> candidates = new ArrayList<>();
        candidates.add(List.of(kicadPoint, preferred));
        candidates.add(List.of(kicadPoint, other));
        // Outwards from the body: the side of the pin away from the part's centre.
        double outwards = centre == null ? 1 : Math.signum(sideways ? pin[0] - centre[0] : pin[1] - centre[1]);
        for (int steps = 1; steps <= 3; steps++) {
            double lane = outwards * steps * GRID;
            if (sideways) {
                double x = pin[0] + lane;
                candidates.add(List.of(kicadPoint, new double[] {x, kicadPoint[1]}, new double[] {x, pin[1]}));
                candidates.add(List.of(kicadPoint, new double[] {kicadPoint[0], pin[1] + lane},
                        new double[] {x, pin[1] + lane}, new double[] {x, pin[1]}));
            } else {
                double y = pin[1] + lane;
                candidates.add(List.of(kicadPoint, new double[] {kicadPoint[0], y}, new double[] {pin[0], y}));
                candidates.add(List.of(kicadPoint, new double[] {pin[0] + lane, kicadPoint[1]},
                        new double[] {pin[0] + lane, y}, new double[] {pin[0], y}));
            }
        }
        return candidates;
    }

    /** Whether a stub — KiCad's point, its corners, then the pin — runs over no pin of another net. */
    private boolean clear(List<double[]> corners, Terminal terminal) {
        List<double[]> points = new ArrayList<>(corners);
        points.add(terminal.drawn());
        for (int index = 1; index < points.size(); index++) {
            if (crossesPin(points.get(index - 1), points.get(index), terminal)) {
                return false;
            }
        }
        return true;
    }

    /** Whether one straight stretch of a stub runs over a pin of another net. */
    private boolean crossesPin(double[] from, double[] to, Terminal own) {
        Optional<String> ownNet = netOf(own.name());
        for (Terminal other : terminals.values()) {
            if (other == own || other.name().equals(own.name())) {
                continue;
            }
            if (onSegment(other.drawn(), from, to) && (ownNet.isEmpty() || !ownNet.equals(netOf(other.name())))) {
                return true;
            }
        }
        return false;
    }

    /** Whether a point lies on an axis-parallel segment, its ends included. */
    private static boolean onSegment(double[] point, double[] from, double[] to) {
        boolean withinX = point[0] >= Math.min(from[0], to[0]) - 0.01 && point[0] <= Math.max(from[0], to[0]) + 0.01;
        boolean withinY = point[1] >= Math.min(from[1], to[1]) - 0.01 && point[1] <= Math.max(from[1], to[1]) + 0.01;
        boolean onLine = Math.abs(from[0] - to[0]) < 0.01 ? Math.abs(point[0] - from[0]) < 0.01
                : Math.abs(from[1] - to[1]) < 0.01 && Math.abs(point[1] - from[1]) < 0.01;
        return withinX && withinY && onLine;
    }

    /** Whether every stub from KiCad's point into this terminal would run over another pin. */
    private boolean stubBlocked(Terminal terminal, double[] kicadPoint) {
        double[] pin = terminal.drawn();
        if (same(pin, kicadPoint)) {
            return false;
        }
        return leadCandidates(terminal, kicadPoint).stream().noneMatch(candidate -> clear(candidate, terminal));
    }

    /**
     * A terminal drawn far from its KiCad point — a pin KiCad draws on another unit, the power unit's supply pins drawn
     * on the part's own symbol — is reached by its net's name, as KiCad reaches a power pin: the wire ends at KiCad's
     * point on a label of the net, and the pin gets a label of its own. A long stub across the sheet would cross other
     * nets' wires and, written back to KiCad, join them where a corner lands on one.
     */
    private Terminal byNameWhenFar(Terminal terminal, double[] kicadPoint) {
        double[] pin = terminal.drawn();
        boolean near = Math.abs(pin[0] - kicadPoint[0]) + Math.abs(pin[1] - kicadPoint[1]) <= LEAD_LIMIT;
        if (near && !stubBlocked(terminal, kicadPoint)) {
            return terminal;
        }
        Optional<String> net = netOf(terminal.name());
        if (net.isEmpty()) {
            return terminal;
        }
        pinLabels.computeIfAbsent(terminal.name(), name -> {
            double[] centre = terminal.partCentre() == null ? kicadPoint : terminal.partCentre();
            Terminal label = labelAt(net.get(), pin, new double[] {pin[0] - centre[0], pin[1] - centre[1]});
            wires.add(new SchematicWire("W" + (wires.size() + 1), terminal.name(), label.name(), List.of()));
            return label;
        });
        return labelAt(net.get(), kicadPoint, new double[] {0, -1});
    }

    /**
     * A mark of a net whose pin stands on a drawn point, its body pointing along {@code away}: a ground for node 0, a
     * label for any other net.
     */
    private Terminal labelAt(String net, double[] at, double[] away) {
        boolean ground = net.equals(ObjectDefinition.Circuit.GROUND);
        MarkShape shape = ground ? GROUND : LABEL;
        double rotation = rotationTowards(away, shape.bodyY());
        double[] pin = turned(new double[] {0, shape.pinY()}, rotation, false);
        String id = id((ground ? "ground" : "label") + (items.size() + 1));
        items.add(new PlacedItem(id, ground ? "ground" : "label", ground ? null : net, at[0] - pin[0], at[1] - pin[1], rotation,
                false));
        return new Terminal(id + "." + shape.terminal(), at, null);
    }

    /** The net a terminal is on: a part's pin by its connections, a mark by the net it names, a ground by node 0. */
    private Optional<String> netOf(String terminal) {
        int dot = terminal.indexOf('.');
        String id = terminal.substring(0, dot);
        String pin = terminal.substring(dot + 1);
        for (CircuitElement element : elements) {
            if (element.id().equals(id) && element.pins() != null) {
                return Optional.ofNullable(element.pins().get(pin));
            }
        }
        for (PlacedItem item : items) {
            if (item.id().equals(id) && item.mark() != null && !item.mark().equals("junction")) {
                return Optional.of(item.net() != null ? item.net() : ObjectDefinition.Circuit.GROUND);
            }
        }
        return Optional.empty();
    }

    /**
     * What the oscilloscope opens on: the design's supply rails as KiCad's power symbols name them — the highest
     * positive one, then the most negative or the next highest —, each at the 1-2-5 scale that holds it within
     * {@link #RAIL_DIVISIONS} divisions. An arbitrary net at millivolts per division is what a first run showed without.
     */
    private List<ObjectDefinition.BenchProbe> railProbes() {
        List<Map.Entry<String, Double>> positive = rails.entrySet().stream().filter(rail -> rail.getValue() > 0)
                .sorted((one, other) -> Double.compare(other.getValue(), one.getValue())).toList();
        List<Map.Entry<String, Double>> negative = rails.entrySet().stream().filter(rail -> rail.getValue() < 0)
                .sorted((one, other) -> Double.compare(one.getValue(), other.getValue())).toList();
        List<Map.Entry<String, Double>> chosen = new ArrayList<>();
        if (!positive.isEmpty()) {
            chosen.add(positive.getFirst());
        }
        if (!negative.isEmpty()) {
            chosen.add(negative.getFirst());
        } else if (positive.size() > 1) {
            chosen.add(positive.get(1));
        }
        List<ObjectDefinition.BenchProbe> probes = new ArrayList<>();
        for (int index = 0; index < chosen.size() && index < CHANNELS.size(); index++) {
            probes.add(new ObjectDefinition.BenchProbe(CHANNELS.get(index), "voltage", chosen.get(index).getKey(), null,
                    scaleFor(Math.abs(chosen.get(index).getValue())), null, null));
        }
        return probes;
    }

    /** The smallest step of the 1-2-5 series at which a voltage stands within {@link #RAIL_DIVISIONS} divisions. */
    private static double scaleFor(double volts) {
        double wanted = volts / RAIL_DIVISIONS;
        double decade = Math.pow(10, Math.floor(Math.log10(wanted)));
        for (double step : new double[] {1, 2, 5, 10}) {
            if (step * decade >= wanted) {
                return step * decade;
            }
        }
        return 10 * decade;
    }

    /**
     * A design is plugged into the world through its connectors — mains in, a potentiometer or a load off the board —,
     * and runs on the bench only once something stands at them: the note names them, so the reader knows where to put a
     * source, a load or a meter.
     */
    private void noteConnectors() {
        if (!connectors.isEmpty()) {
            notes.add(String.join(", ", connectors) + " connect the design to what is off the board — its supply, its "
                    + "controls, its load: nothing stands at them on the bench, so place a source, a load or a meter at "
                    + "each to run it as it is used.");
        }
    }

    /** Terminals that touch at one KiCad point, pin on pin: a wire between them through that point. */
    private void touchingWires() {
        for (Touching pair : touching) {
            double[] point = drawn(pair.point());
            if (same(pair.first().drawn(), pair.second().drawn())) {
                wires.add(new SchematicWire("W" + (wires.size() + 1), pair.first().name(), pair.second().name(), List.of()));
            } else {
                wire(pair.first(), point, List.of(), pair.second(), point);
            }
        }
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
        terminal(point, name, drawn, null);
    }

    /** A terminal at a KiCad point; a second one at the same point touches the first, and is wired to it. */
    private void terminal(KiCadPoint point, String name, double[] drawn, double[] partCentre) {
        Terminal terminal = new Terminal(name, drawn, partCentre);
        Terminal first = terminals.putIfAbsent(point.key(), terminal);
        if (first != null && !first.name().equals(name)) {
            touching.add(new Touching(first, terminal, point));
        }
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

    /** The nearest point of the sheet's grid: KiCad's 1.27 mm, twenty drawing units. */
    private static double grid(double value) {
        return Math.round(value / GRID) * GRID;
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
