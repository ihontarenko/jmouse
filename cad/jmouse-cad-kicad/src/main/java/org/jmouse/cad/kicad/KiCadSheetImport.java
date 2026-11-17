package org.jmouse.cad.kicad;

import org.jmouse.cad.exchange.imported.ImportedNetNames;
import org.jmouse.cad.exchange.imported.SymbolCanvas;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.cad.exchange.imported.SheetMarks;
import org.jmouse.cad.exchange.imported.SheetTracing;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProperty;
import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.KiCadValue;
import org.jmouse.kicad.connectivity.KiCadConnection;
import org.jmouse.kicad.connectivity.KiCadConnectivity;
import org.jmouse.kicad.connectivity.KiCadNet;
import org.jmouse.kicad.schematic.KiCadBus;
import org.jmouse.kicad.schematic.KiCadBusEntry;
import org.jmouse.kicad.schematic.KiCadBusLines;
import org.jmouse.kicad.schematic.KiCadLabel;
import org.jmouse.kicad.schematic.KiCadSchematic;
import org.jmouse.kicad.schematic.KiCadSheet;
import org.jmouse.kicad.schematic.KiCadSymbolInstance;
import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadPinType;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.jmouse.cad.exchange.imported.ImportedCircuit.Item;
import org.jmouse.cad.exchange.imported.ImportedCircuit.Part;
import org.jmouse.cad.exchange.imported.ImportedCircuit.Probe;
import org.jmouse.cad.kicad.ImportedSheet.SymbolUse;
import org.jmouse.cad.exchange.imported.ImportedBuses;
import org.jmouse.cad.exchange.imported.ImportedCircuit.Wire;

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

    /** KiCad's standard libraries of connectors start so: {@code Connector:}, {@code Connector_Generic:}. */
    /** What KiCad writes where a field says nothing. */
    private static final String KICAD_UNSAID = "~";

    private static final String CONNECTOR_LIBRARY = "Connector";

    /** How many divisions a rail stands up from the screen's middle at the scale chosen for it. */
    private static final double RAIL_DIVISIONS = 3;

    /** The oscilloscope's channels an import sets up, in the order the rails are given to them. */
    private static final List<String> CHANNELS = List.of("CH1", "CH2");

    /** The sheet's grid in drawing units: KiCad's 1.27 mm grid, which every symbol's pins stand on. */
    private static final double GRID = SymbolCanvas.GRID;

    /**
     * How far from its KiCad point a pin may be drawn and still be wired to it, along both axes: a catalogue symbol
     * shaped otherwise than KiCad's puts a pin a grid step or three away; a pin further off is on another unit's place.
     */
    private static final double LEAD_LIMIT = 6 * GRID;

    private final KiCadSchematic schematic;
    private final KiCadLibraryMap library;
    /** What an own component's library id opens with: the own library's name and its colon. */
    private final String ownLibrary;
    private final KiCadCatalogue catalogue;
    /** How the sheet's marks are drawn: where each one's pin stands. */
    private final SheetMarks marks;
    private final Function<String, String> sheetSlug;
    private final Function<String, Map<String, String>> portsOf;

    private final KiCadProject alone;
    private final String path;
    private final KiCadConnectivity connectivity;
    private final ImportedNetNames nets;
    private final UniqueIdentifiers ids = new UniqueIdentifiers(ImportedCircuit.ID_OPENING, ImportedCircuit.ID_LONGEST);
    private final List<Part> elements = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Wire> wires = new ArrayList<>();
    private final Set<SymbolUse> symbols = new LinkedHashSet<>();
    private final Set<KiCadSymbolDefinition> components = new LinkedHashSet<>();
    /** Each part's KiCad footprint, by the part's id: the library id its {@code Footprint} property names. */
    private final Map<String, String> partFootprints = new LinkedHashMap<>();
    private final Map<String, String> identifiers = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> partPads = new LinkedHashMap<>();
    /** The parts of each symbol the library map names no component for, by what was made for them: one note each. */
    private final Map<String, List<String>> unmapped = new LinkedHashMap<>();
    /** The rails KiCad's power symbols name, by net, with the voltage their value says: {@code +48V} is 48. */
    private final Map<String, Double> rails = new LinkedHashMap<>();
    /** The parts a design is plugged into the world through — KiCad's connectors —, by reference. */
    private final List<String> connectors = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final List<String> ports = new ArrayList<>();
    /** The buses the sheet hands across its edge whole, by name: its hierarchical labels naming a bus. */
    private final List<String> busPorts = new ArrayList<>();
    /** The buses the sheet declares: those its bus lines, its bus ports and its boxes' bus pins name. */
    private final ImportedBuses declaredBuses = new ImportedBuses();
    /** The design's bus aliases, by which a label or a pin may name a bus. */
    private final Map<String, List<String>> busAliases;
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

    private KiCadSheetImport(KiCadSchematic schematic, String instancePath, KiCadDesignNets designNets, KiCadLibraryMap library,
                             String ownLibrary, KiCadCatalogue catalogue, Function<String, String> sheetSlug,
                             Function<String, Map<String, String>> portsOf, SheetMarks marks) {
        this.marks = marks;
        this.nets = designNets.names();
        this.busAliases = designNets.busAliases();
        this.schematic = schematic;
        this.library = library;
        this.ownLibrary = ownLibrary + ":";
        this.catalogue = catalogue;
        this.sheetSlug = sheetSlug;
        this.portsOf = portsOf;
        this.alone = KiCadProject.single(schematic, instancePath);
        this.path = alone.root().path();
        this.connectivity = KiCadConnectivity.of(alone);
    }

    /**
     * @param instancePath the path of the place the sheet stands in whose references the parts take: KiCad numbers
     *                     a sheet's parts for each place it stands, and the first place's numbers are the document's
     * @param designNets   how the design names its nets: node 0, and the net each name joining across the design is
     * @param ownLibrary   the KiCad library an export writes a component with no KiCad counterpart under: a symbol of it
     *                     is that component again, when the catalogue holds it
     * @param sheetSlug    the slug of the circuit a sheet file becomes, for a box placing it
     * @param portsOf      the port each hierarchical label of a sheet file is, by the label's words, for a box's pins to meet
     * @param marks        how the sheet's marks are drawn
     */
    public static ImportedSheet of(KiCadSchematic schematic, String instancePath, KiCadDesignNets designNets, KiCadLibraryMap library,
                                   String ownLibrary, KiCadCatalogue catalogue, Function<String, String> sheetSlug,
                                   Function<String, Map<String, String>> portsOf, SheetMarks marks) {
        KiCadSheetImport sheet = new KiCadSheetImport(schematic, instancePath, designNets, library, ownLibrary, catalogue,
                sheetSlug, portsOf, marks);
        sheet.parts();
        sheet.marks();
        sheet.boxes();
        sheet.wires();
        sheet.touchingWires();
        sheet.noteConnectors();
        List<ImportedCircuit.BusLine> busLines = sheet.busLines();
        List<SheetBoundary.Port> ports = new ArrayList<>(sheet.ports.stream().map(ImportedCircuit::netPort).toList());
        sheet.busPorts.forEach(bus -> ports.add(ImportedCircuit.busPort(bus)));
        return new ImportedSheet(new ImportedCircuit(sheet.elements, sheet.items, sheet.wires, sheet.railProbes(), ports,
                sheet.globals, sheet.notes, sheet.declaredBuses.buses(), busLines), sheet.symbols, sheet.components, sheet.partFootprints, sheet.identifiers, sheet.partPads);
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    private void parts() {
        Map<String, String> pinNets = new HashMap<>();
        for (KiCadNet net : connectivity.nets()) {
            // A pin nothing else joins is open — KiCad names its lone net unconnected-…, a no-connect mark or none —: a
            // contact of the part, no connection of the circuit, until it is wired.
            if (net.name().startsWith(KiCadNetNames.UNCONNECTED)) {
                continue;
            }
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

    /**
     * A symbol of the library an export writes our own components under — {@code jmCAD:voltage-source} — is that
     * component again, numbered by its package, when the catalogue holds it: what was exported comes back as itself.
     */
    private Optional<KiCadLibraryMap.Mapping> ownMapping(String libraryId) {
        if (!libraryId.startsWith(ownLibrary)) {
            return Optional.empty();
        }
        String component = libraryId.substring(ownLibrary.length());
        return catalogue.pinsByNumber(component).isEmpty() ? Optional.empty()
                : Optional.of(new KiCadLibraryMap.Mapping(component, Map.of(), catalogue.valueParameterOf(component)));
    }

    /**
     * The port each hierarchical label of a sheet is, by the label's words: its net as the sheet names it, as {@link #marks}
     * names it, so two labels on one wire are one port. A label naming a bus — {@code IRQ-[1..7]} — is the port of that bus, by the
     * bus's name as the circuit declares it: KiCad hands the bus across whole, its nets member by member.
     */
    public static Map<String, String> portsOf(KiCadSchematic schematic, String instancePath, KiCadDesignNets designNets) {
        KiCadProject alone = KiCadProject.single(schematic, instancePath);
        KiCadConnectivity connectivity = KiCadConnectivity.of(alone);
        ImportedNetNames nets = designNets.names();
        // Declared in the order the sheet's own import declares them, so a bus is called here what it is called there.
        ImportedBuses names = new ImportedBuses();
        Map<String, String> ports = new LinkedHashMap<>();
        for (KiCadLabel label : schematic.labels()) {
            if (label.kind() != KiCadLabel.Kind.HIERARCHICAL) {
                continue;
            }
            Optional<KiCadBus> bus = KiCadBus.of(label.text(), designNets.busAliases());
            if (bus.isPresent()) {
                ports.put(label.text(), names.declare(bus.get().name(), membersHere(bus.get(), schematic, connectivity, instancePath, nets)));
            } else {
                ports.put(label.text(), connectivity.netAt(instancePath, label.at()).map(nets::of).orElse(nets.of(label.text())));
            }
        }
        return ports;
    }

    /**
     * The net each of a bus's members is on a sheet, by the member's name, as KiCad joins it (checked with kicad-cli 10): the net an
     * entry takes off a bus there, or a label of the name names — by name, an entry or none. A member no net of the sheet is named is
     * left out.
     */
    private static Map<String, String> memberNetsOf(KiCadBus bus, KiCadSchematic schematic, KiCadConnectivity connectivity, String path,
                                                    ImportedNetNames nets) {
        Map<String, String> takenOff = new LinkedHashMap<>();
        for (KiCadBusEntry entry : schematic.busEntries()) {
            for (KiCadPoint end : List.of(entry.at(), entry.end())) {
                connectivity.netAt(path, end).ifPresent(net -> takenOff.putIfAbsent(net.substring(net.lastIndexOf('/') + 1), net));
            }
        }
        Map<String, String> members = new LinkedHashMap<>();
        for (String member : bus.members()) {
            Optional<String> net = Optional.ofNullable(takenOff.get(member));
            if (net.isEmpty()) {
                net = schematic.labels().stream().filter(label -> label.kind() != KiCadLabel.Kind.HIERARCHICAL && label.text().equals(member))
                        .findFirst().flatMap(label -> connectivity.netAt(path, label.at()));
            }
            net.map(nets::of).ifPresent(named -> members.put(member, named));
        }
        return members;
    }

    /** A part's value left in words: KiCad's value, unless a number of it set the component's parameter or KiCad left it unsaid. */
    private static String wordsOfValue(KiCadSymbolInstance part, Optional<KiCadLibraryMap.Mapping> mapping) {
        boolean set = mapping.flatMap(KiCadLibraryMap.Mapping::value).isPresent() && KiCadValue.number(part.value()).isPresent();
        return set || part.value().equals(KICAD_UNSAID) ? null : part.value();
    }

    private void part(String reference, List<KiCadSymbolInstance> units, Map<String, String> pinNets) {
        KiCadSymbolInstance first = units.getFirst();
        KiCadSymbolDefinition definition = schematic.definitionOf(first).orElseThrow();
        String id = id(reference);
        units.forEach(unit -> identifiers.put(unit.uuid(), id));
        Optional<KiCadLibraryMap.Mapping> mapping = library.mappingOf(first.libraryId()).or(() -> ownMapping(first.libraryId()));
        // With no model, the drawing the map names for it, when that drawing places every pin.
        Optional<KiCadComponentStub.CatalogueDrawing> ownDrawing = mapping.isPresent() ? Optional.empty()
                : KiCadComponentStub.drawnWith(definition, library, catalogue);
        if (first.libraryId().startsWith(CONNECTOR_LIBRARY)) {
            connectors.add(id);
        }
        // The map's own numbering where it writes one — KiCad's diode is 1 = K, a catalogue diode's 1 is its anode —,
        // else the component's package numbers, which KiCad's numbers are.
        Map<String, String> numbered = mapping.map(found -> found.pins().isEmpty()
                ? catalogue.pinsByNumber(found.component()) : found.pins())
                .orElseGet(() -> ownDrawing.map(KiCadComponentStub.CatalogueDrawing::pins).orElse(Map.of()));
        Map<String, String> pins = new LinkedHashMap<>();
        // Where each KiCad unit's pins stand on the sheet, by the name the part gives them: `A.output`, `Vcc`.
        Map<KiCadSymbolInstance, Map<String, KiCadPoint>> unitPoints = new LinkedHashMap<>();
        for (KiCadSymbolInstance unit : units) {
            Map<String, KiCadPoint> placed = unitPoints.computeIfAbsent(unit, ignored -> new LinkedHashMap<>());
            for (KiCadPin pin : definition.pinsOf(unit.unit())) {
                String name = numbered.getOrDefault(pin.number(), KiCadSymbolDrawing.pinName(pin));
                String net = pinNets.get(unit.uuid() + ":" + pin.number());
                partPads.computeIfAbsent(id, ignored -> new LinkedHashMap<>()).put(pin.number(), name);
                if (net != null) {
                    pins.putIfAbsent(name, net);
                }
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
            // A field named as one of the component's parameters sets it — what an export writes beside the value —;
            // every other field of a design is not ours to read.
            for (String parameter : catalogue.parametersOf(component)) {
                String field = first.property(parameter).strip();
                if (field.isEmpty() || overrides.containsKey(parameter)) {
                    continue;
                }
                KiCadValue.number(field).ifPresentOrElse(number -> overrides.put(parameter, plain(number)),
                        () -> notes.add(id + ": its field " + parameter + " = '" + field + "' is not a number, so "
                                + parameter + " keeps the component's own."));
            }
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
            placeAsKiCad(id, first, drawnByBox(points, pins));
        } else {
            // A chip is drawn as our box: its part names no symbol. Only a design's own small symbol is drawn as it came.
            boolean boxed = KiCadComponentStub.boxed(definition, library);
            if (!boxed) {
                symbol = KiCadSymbolDrawing.codeOf(definition, first.unit());
                symbols.add(new SymbolUse(definition, first.unit(), Map.of()));
            }
            placeAsKiCad(id, first, boxed ? drawnByBox(points, pins) : points);
        }
        if (units.size() > 1 && drawn.isEmpty()) {
            notes.add(id + " is drawn by its unit " + KiCadSymbolDrawing.unitLetter(first.unit()) + " alone: KiCad draws "
                    + "each of its units apart and a part is drawn with one symbol; the other units are wired by their nets.");
        }
        elements.add(new Part(id, component, pins, overrides, symbol, null).withValue(wordsOfValue(first, mapping)));
    }

    /**
     * The pins a part drawn as our box stands at: those it is connected by. A box draws what connects, so a pin left open
     * has no place on it, and a wire KiCad drew from one — a stub that joins nothing — has nothing here to end on.
     */
    private static Map<String, KiCadPoint> drawnByBox(Map<String, KiCadPoint> points, Map<String, String> pins) {
        Map<String, KiCadPoint> drawn = new LinkedHashMap<>(points);
        drawn.keySet().retainAll(pins.keySet());
        return drawn;
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
        double[] centre = SheetMarks.turned(KiCadSymbolDrawing.centreOf(schematic.definitionOf(instance).orElseThrow(), instance.unit()),
                rotation, mirrored);
        double x = at[0] + centre[0];
        double y = at[1] + centre[1];
        items.add(new Item(id, null, null, x, y, rotation, mirrored)
                .withTexts(writtenAt(instance, KiCadProperty.REFERENCE, x, y), writtenAt(instance, KiCadProperty.VALUE, x, y)));
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
            double[] turned = onSymbol == null ? new double[] {0, 0} : SheetMarks.turned(onSymbol, part[2], part[3] == 1);
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

    /** Where the sheet writes a part's property, from the part's centre; null for one it does not show. */
    private static double[] writtenAt(KiCadSymbolInstance instance, KiCadProperty property, double x, double y) {
        KiCadPoint written = property.writtenIn(instance.written());
        if (written == null) {
            return null;
        }
        double[] at = drawn(written);
        return new double[] {ImportedCircuit.rounded(at[0] - x), ImportedCircuit.rounded(at[1] - y)};
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
        items.add(new Item(id, null, null, x, y, rotation, mirrored, unit));
        points.forEach((pin, point) -> {
            double[] onSymbol = local.get(pin);
            double[] turned = onSymbol == null ? new double[] {0, 0} : SheetMarks.turned(onSymbol, rotation, mirrored);
            terminal(point, id + "." + pin, new double[] {x + turned[0], y + turned[1]}, new double[] {x, y});
        });
        return new double[] {x, y, rotation, mirrored ? 1 : 0};
    }

    /** How well a symbol can stand where KiCad's pins are: the pins it lands, less a little for the distance of the rest. */
    private double fit(KiCadCatalogue.PartDrawing drawing, Map<String, KiCadPoint> points) {
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

    /**
     * Where a symbol stands best for these KiCad pins: x, y, rotation, 1 when mirrored, the pins landed, the error left. A
     * place where a pin would stand on a point the sheet joins to another net — another part's pin, a label, a sheet's pin
     * — is no place: the drawing would join that net to the pin's, in KiCad and to any eye.
     */
    private double[] alignment(KiCadCatalogue.PartDrawing drawing, Map<String, KiCadPoint> points) {
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
                        double[] turned = SheetMarks.turned(local.get(pin), rotation, mirrored);
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
                        if (!same(at, kicad) && joinsAnotherNet(at, points.get(pin))) {
                            onAnother = true;
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
            double[] turned = SheetMarks.turned(local.get(pin), rotation, mirrored);
            places.add(new double[] {ImportedCircuit.onGrid(kicad[0] - turned[0]), ImportedCircuit.onGrid(kicad[1] - turned[1])});
            sumX += kicad[0] - turned[0];
            sumY += kicad[1] - turned[1];
        }
        if (!pins.isEmpty()) {
            places.add(new double[] {ImportedCircuit.onGrid(sumX / pins.size()), ImportedCircuit.onGrid(sumY / pins.size())});
        } else {
            places.add(new double[] {0, 0});
        }
        return places;
    }


    // ─── marks ──────────────────────────────────────────────────────────────────────────────────────

    private void marks() {
        for (KiCadSymbolInstance instance : schematic.symbols()) {
            Optional<KiCadSymbolDefinition> definition = schematic.definitionOf(instance);
            if (definition.isEmpty() || !definition.get().power() || definition.get().pins().isEmpty()) {
                continue;
            }
            KiCadPin pin = definition.get().pins().getFirst();
            // A power symbol whose pin gives power — KiCad's power flag — names no net: ERC's, not the circuit's.
            if (pin.type() != KiCadPinType.POWER_INPUT) {
                continue;
            }
            KiCadPoint point = instance.placement().place(pin.at());
            double radians = Math.toRadians(pin.angle());
            KiCadPoint into = instance.placement().place(new KiCadPoint(pin.at().x() + Math.cos(radians),
                    pin.at().y() + Math.sin(radians)));
            double[] body = {into.x() - point.x(), into.y() - point.y()};
            String value = instance.value();
            // Its net as the sheet's wires make it, as a label's and a part's pin's: two power symbols on one wire are one net, named once.
            String net = connectivity.netAt(path, point).map(nets::of).orElse(nets.of(value));
            boolean ground = value.toUpperCase().startsWith("GND") || value.equalsIgnoreCase("VSS");
            if (ground) {
                // A power symbol joins its net across the design, a second ground as much as a rail; only node 0 needs no saying.
                globals.add(net);
                mark("ground", net, SheetMarks.GROUND, point, body);
            } else {
                globals.add(net);
                KiCadValue.volts(value).ifPresent(volts -> rails.putIfAbsent(net, volts));
                mark("power", net, SheetMarks.POWER, point, body);
            }
        }
        for (KiCadLabel label : schematic.labels()) {
            // A hierarchical label naming a bus hands each of its nets across the sheet's edge: each a port of the circuit.
            Optional<KiCadBus> bus = KiCadBus.of(label.text(), busAliases);
            if (label.kind() == KiCadLabel.Kind.HIERARCHICAL && bus.isPresent()) {
                String declared = declared(bus.get());
                if (!busPorts.contains(declared)) {
                    busPorts.add(declared);
                }
                // Drawn where KiCad drew it: a port of the sheet naming the bus it hands across.
                double turned = Math.toRadians(label.angle());
                mark("port", declared, SheetMarks.PORT, label.at(), new double[] {Math.cos(turned), -Math.sin(turned)});
                continue;
            }
            // A label on a bus line names the line, and joins nothing: the bus takes its words.
            if (ImportedBuses.onAnyBus(new double[] {label.at().x(), label.at().y()}, busStretches())) {
                continue;
            }
            String net = connectivity.netAt(path, label.at()).map(nets::of).orElse(nets.of(label.text()));
            double radians = Math.toRadians(label.angle());
            double[] body = {Math.cos(radians), -Math.sin(radians)};
            switch (label.kind()) {
                case LOCAL -> mark("label", net, SheetMarks.LABEL, label.at(), body);
                case GLOBAL -> {
                    globals.add(net);
                    mark("global", net, SheetMarks.GLOBAL, label.at(), body);
                }
                case HIERARCHICAL -> {
                    if (!ports.contains(net)) {
                        ports.add(net);
                    }
                    mark("port", net, SheetMarks.PORT, label.at(), body);
                }
            }
        }
        // A net a hidden power pin names joins across the design as a power symbol's does, with nothing drawn for it.
        connectivity.globals().values().stream().map(nets::of).forEach(globals::add);
        // A bus entry: a mark of the net its wire takes off the bus, its pin the wire's end, its body slanting back to the bus.
        for (KiCadBusEntry entry : schematic.busEntries()) {
            boolean startOnBus = ImportedBuses.onAnyBus(new double[] {entry.at().x(), entry.at().y()}, busStretches());
            KiCadPoint onBus = startOnBus ? entry.at() : entry.end();
            KiCadPoint wireEnd = startOnBus ? entry.end() : entry.at();
            String net = connectivity.netAt(path, wireEnd).map(nets::of).orElse(null);
            double[] body = {Math.signum(onBus.x() - wireEnd.x()), Math.signum(onBus.y() - wireEnd.y())};
            mark("entry", net, SheetMarks.ENTRY, wireEnd, body);
        }
        // Node 0 is every sheet's already: a global label of the reference ground hands nothing across a sheet's edge.
        globals.removeIf(net -> ports.contains(net) || net.equals(Circuit.GROUND));
        for (KiCadPoint junction : schematic.junctions()) {
            junction(junction);
        }
    }

    /**
     * A mark whose symbol connects at a point and whose body goes one way from it: turned so its body goes the way
     * KiCad's does, moved so its pin stands on KiCad's point.
     */
    private void mark(String prefix, String net, SheetMarks.Shape shape, KiCadPoint point, double[] body) {
        double[] at = drawn(point);
        String id = id(prefix + (items.size() + 1));
        items.add(marks.mark(id, net, shape, at, body));
        terminal(point, id + "." + shape.terminal(), at);
    }

    private void junction(KiCadPoint point) {
        if (terminals.containsKey(point.key())) {
            return;
        }
        double[] at = drawn(point);
        String id = id("junction" + (items.size() + 1));
        items.add(new Item(id, "junction", null, at[0], at[1], 0, false));
        terminal(point, id + ".j", at);
    }

    // ─── sheets placed as blocks ────────────────────────────────────────────────────────────────────

    private void boxes() {
        for (KiCadSheet box : schematic.sheets()) {
            String id = id(box.name().isBlank() ? "sheet" : box.name());
            identifiers.put(box.uuid(), id);
            double[] at = drawn(box.at());
            double[] size = {box.size().x() * SymbolCanvas.UNITS_PER_MILLIMETRE,
                    box.size().y() * SymbolCanvas.UNITS_PER_MILLIMETRE};
            // Drawn the size KiCad draws it, on the grid, so its name and its sheet stand where they stood there.
            double[] drawnSize = {Math.max(GRID, ImportedCircuit.onGrid(size[0])), Math.max(GRID, ImportedCircuit.onGrid(size[1]))};
            double[] centre = {ImportedCircuit.onGrid(at[0] + size[0] / 2), ImportedCircuit.onGrid(at[1] + size[1] / 2)};
            Map<String, String> pins = new LinkedHashMap<>();
            List<Placement.PortPlace> placed = new ArrayList<>();
            Map<String, String> labelled = portsOf.apply(box.file());
            for (KiCadSheet.Pin pin : box.pins()) {
                Optional<KiCadBus> bus = KiCadBus.of(pin.name(), busAliases);
                if (bus.isPresent()) {
                    busPin(id, box, pin, bus.get(), labelled, pins).ifPresent(port -> placed.add(portPlace(port, box, pin, centre, drawnSize)));
                    continue;
                }
                String port = labelled.get(pin.name());
                // Joined to nothing inside: KiCad only warns of it, and a circuit has no port to wire it to.
                if (port == null) {
                    notes.add(id + ": its pin " + pin.name() + " has no label of that name on " + box.file() + ", so it joins nothing.");
                    continue;
                }
                String net = connectivity.netAt(path, pin.at()).map(nets::of).orElse(nets.of(id + "_" + port + "_open"));
                // Two labels on one wire inside are one port; their two pins out here are then one net.
                String joined = pins.putIfAbsent(port, net);
                if (joined != null && !joined.equals(net)) {
                    notes.add(id + ": its pins " + pin.name() + " and " + port + " are one net on " + box.file() + " but apart here: only "
                            + port + " is taken.");
                }
                if (joined == null) {
                    Placement.PortPlace onFrame = portPlace(port, box, pin, centre, drawnSize);
                    placed.add(onFrame);
                    terminal(pin.at(), id + "." + port, onFrameAt(onFrame, centre, drawnSize));
                } else {
                    terminal(pin.at(), id + "." + port, drawn(pin.at()));
                }
            }
            elements.add(new Part(id, sheetSlug.apply(box.file()), pins, Map.of(), null, null));
            items.add(new Item(id, null, null, centre[0], centre[1], 0, false, null, drawnSize).withPorts(placed));
        }
    }

    /**
     * A box's pin carrying a bus: the child's bus of its name wired to the bus of its name here, declared on this sheet with the nets
     * its members are here — KiCad joins them member by member, which the circuit's unfolding does from the two declarations.
     *
     * @return the child's port the pin wires; empty when the child hands no bus of the pin's words across
     */
    private Optional<String> busPin(String id, KiCadSheet box, KiCadSheet.Pin pin, KiCadBus bus, Map<String, String> labelled,
                                    Map<String, String> pins) {
        String port = labelled.get(pin.name());
        if (port == null) {
            notes.add(id + ": its pin " + bus.text() + " has no label of that bus on " + box.file() + ", so it joins nothing.");
            return Optional.empty();
        }
        // Bound to the bus the line reaching the pin is named by — another name's, in order, as KiCad binds it — else the pin's own.
        KiCadBus above = new KiCadBusLines(schematic, busAliases).busReaching(pin.at()).filter(reached -> reached.members().size() == bus.members().size())
                .orElse(bus);
        String joined = pins.putIfAbsent(port, declared(above));
        return joined == null ? Optional.of(port) : Optional.empty();
    }

    /** A bus KiCad names here, declared on this sheet: each member the net of its name here, else a net of the bus's own that joins nothing. */
    private String declared(KiCadBus bus) {
        return declaredBuses.declare(bus.name(), membersHere(bus, schematic, connectivity, path, nets));
    }

    /** The nets a bus KiCad names carries on a sheet: each the net of its member's name there, else an open net of the bus's own. */
    private static List<String> membersHere(KiCadBus bus, KiCadSchematic schematic, KiCadConnectivity connectivity, String path,
                                            ImportedNetNames nets) {
        Map<String, String> here = memberNetsOf(bus, schematic, connectivity, path, nets);
        String word = ImportedBuses.wordOf(bus.name());
        return bus.members().stream().map(member -> here.getOrDefault(member, nets.of(word + "_" + member + "_open"))).toList();
    }

    /**
     * Where a box's pin stands on the frame the sheet draws the box with: the edge KiCad's pin is nearest — a pin stands
     * on the box's edge —, and how far along it from its start, so a wire KiCad drew to it ends on it here too.
     */
    private static Placement.PortPlace portPlace(String port, KiCadSheet box, KiCadSheet.Pin pin, double[] centre, double[] size) {
        double toLeft = Math.abs(pin.at().x() - box.at().x());
        double toRight = Math.abs(pin.at().x() - (box.at().x() + box.size().x()));
        double toTop = Math.abs(pin.at().y() - box.at().y());
        double toBottom = Math.abs(pin.at().y() - (box.at().y() + box.size().y()));
        double nearest = Math.min(Math.min(toLeft, toRight), Math.min(toTop, toBottom));
        Placement.PortPlace.Edge edge = nearest == toLeft ? Placement.PortPlace.Edge.LEFT : nearest == toRight
                ? Placement.PortPlace.Edge.RIGHT : nearest == toTop ? Placement.PortPlace.Edge.TOP : Placement.PortPlace.Edge.BOTTOM;
        double[] point = drawn(pin.at());
        double[] corner = frameCorner(centre, size);
        boolean side = edge == Placement.PortPlace.Edge.LEFT || edge == Placement.PortPlace.Edge.RIGHT;
        return new Placement.PortPlace(port, edge, ImportedCircuit.onGrid(side ? point[1] - corner[1] : point[0] - corner[0]));
    }

    /** A port's point on the frame the sheet draws a box of this centre and size with. */
    private static double[] onFrameAt(Placement.PortPlace place, double[] centre, double[] size) {
        double[] corner = frameCorner(centre, size);
        double right = 2 * centre[0] - corner[0];
        double bottom = 2 * centre[1] - corner[1];
        return switch (place.edge()) {
            case LEFT -> new double[] {corner[0], corner[1] + place.along()};
            case RIGHT -> new double[] {right, corner[1] + place.along()};
            case TOP -> new double[] {corner[0] + place.along(), corner[1]};
            case BOTTOM -> new double[] {corner[0] + place.along(), bottom};
        };
    }

    /** The top-left corner of the frame the sheet draws a box with: its half sizes rounded up to the grid. */
    private static double[] frameCorner(double[] centre, double[] size) {
        return new double[] {centre[0] - Math.ceil(size[0] / 2 / GRID) * GRID, centre[1] - Math.ceil(size[1] / 2 / GRID) * GRID};
    }

    // ─── wires ──────────────────────────────────────────────────────────────────────────────────────

    /** KiCad's wires as jMX's, traced between the sheet's terminals as every import traces them ({@link SheetTracing}). */
    private void wires() {
        List<SheetTracing.Point> stops = terminals.keySet().stream().map(key -> traced(pointOf(key))).toList();
        List<List<SheetTracing.Point>> drawnWires = schematic.wires().stream()
                .map(wire -> wire.points().stream().map(KiCadSheetImport::traced).toList()).toList();
        for (SheetTracing.Chain chain : SheetTracing.trace(stops, drawnWires, point -> junction(kicad(point)))) {
            KiCadPoint from = kicad(chain.from());
            KiCadPoint to = kicad(chain.to());
            wire(terminals.get(from.key()), drawn(from), chain.bends().stream().map(bend -> drawn(kicad(bend))).toList(),
                    terminals.get(to.key()), drawn(to));
        }
    }

    /** The sheet's bus stretches, each a line of KiCad's points. */
    private List<List<double[]>> busStretches() {
        return schematic.buses().stream().map(bus -> bus.points().stream().map(point -> new double[] {point.x(), point.y()}).toList()).toList();
    }

    /** The sheet's bus lines, each naming the bus the label standing on it declares. */
    private List<ImportedCircuit.BusLine> busLines() {
        List<ImportedCircuit.BusLine> buses = new ArrayList<>();
        for (List<double[]> line : ImportedBuses.joined(busStretches())) {
            String written = ImportedBuses.writtenOn(line, schematic.labels(), label -> new double[] {label.at().x(), label.at().y()}, KiCadLabel::text);
            String id = "B" + (buses.size() + 1);
            List<double[]> via = line.stream().map(point -> drawn(new KiCadPoint(point[0], point[1]))).toList();
            // KiCad's own bus syntax read by KiCad's reading, each member the net a label of its name is here; a word alone names the line.
            // Named by a label on it; a line no label names takes the bus of the pin or the port it is drawn to, as KiCad takes it.
            Optional<KiCadBus> named = Optional.ofNullable(written).flatMap(text -> KiCadBus.of(text, busAliases))
                    .or(() -> new KiCadBusLines(schematic, busAliases).busReaching(new KiCadPoint(line.getFirst()[0], line.getFirst()[1])));
            buses.add(named.map(bus -> new ImportedCircuit.BusLine(id, declared(bus), via))
                    .orElseGet(() -> declaredBuses.line(id, written, via, nets::of)));
        }
        return buses;
    }

    private static SheetTracing.Point traced(KiCadPoint point) {
        return new SheetTracing.Point(point.x(), point.y());
    }

    private static KiCadPoint kicad(SheetTracing.Point point) {
        return new KiCadPoint(point.x(), point.y());
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
        List<double[]> through = new ArrayList<>();
        double[] previous = from.drawn();
        for (double[] point : path) {
            if (!same(point, previous)) {
                through.add(point);
                previous = point;
            }
        }
        if (!through.isEmpty() && same(previous, to.drawn())) {
            through.removeLast();
        }
        wires.add(new Wire("W" + (wires.size() + 1), from.name(), to.name(), through));
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
            wires.add(new Wire("W" + (wires.size() + 1), terminal.name(), label.name(), List.of()));
            return label;
        });
        return labelAt(net.get(), kicadPoint, new double[] {0, -1});
    }

    /**
     * A mark of a net whose pin stands on a drawn point, its body pointing along {@code away}: a ground for node 0, a
     * label for any other net.
     */
    private Terminal labelAt(String net, double[] at, double[] away) {
        boolean ground = net.equals(Circuit.GROUND);
        SheetMarks.Shape shape = ground ? SheetMarks.GROUND : SheetMarks.LABEL;
        String id = id((ground ? "ground" : "label") + (items.size() + 1));
        items.add(marks.mark(id, net, shape, at, away));
        return new Terminal(id + "." + shape.terminal(), at, null);
    }

    /** The net a terminal is on: a part's pin by its connections, a mark by the net it names, a ground by node 0. */
    private Optional<String> netOf(String terminal) {
        int dot = terminal.indexOf('.');
        String id = terminal.substring(0, dot);
        String pin = terminal.substring(dot + 1);
        for (Part element : elements) {
            if (element.id().equals(id) && element.pins() != null) {
                return Optional.ofNullable(element.pins().get(pin));
            }
        }
        for (Item item : items) {
            if (item.id().equals(id) && item.mark() != null && !item.mark().equals("junction")) {
                return Optional.of(item.net() != null ? item.net() : Circuit.GROUND);
            }
        }
        return Optional.empty();
    }

    /**
     * What the oscilloscope opens on: the design's supply rails as KiCad's power symbols name them — the highest
     * positive one, then the most negative or the next highest —, each at the 1-2-5 scale that holds it within
     * {@link #RAIL_DIVISIONS} divisions. An arbitrary net at millivolts per division is what a first run showed without.
     */
    private List<Probe> railProbes() {
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
        List<Probe> probes = new ArrayList<>();
        for (int index = 0; index < chosen.size() && index < CHANNELS.size(); index++) {
            probes.add(new Probe(CHANNELS.get(index), chosen.get(index).getKey(),
                    scaleFor(Math.abs(chosen.get(index).getValue()))));
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
     * A design is plugged into the world through its connectors — mains in, a potentiometer or a load off the pcb —,
     * and runs on the bench only once something stands at them: the note names them, so the reader knows where to put a
     * source, a load or a meter.
     */
    private void noteConnectors() {
        if (!connectors.isEmpty()) {
            notes.add(String.join(", ", connectors) + " connect the design to what is off the pcb — its supply, its "
                    + "controls, its load: nothing stands at them on the bench, so place a source, a load or a meter at "
                    + "each to run it as it is used.");
        }
    }

    /** Terminals that touch at one KiCad point, pin on pin: a wire between them through that point. */
    private void touchingWires() {
        for (Touching pair : touching) {
            double[] point = drawn(pair.point());
            if (same(pair.first().drawn(), pair.second().drawn())) {
                wires.add(new Wire("W" + (wires.size() + 1), pair.first().name(), pair.second().name(), List.of()));
            } else {
                wire(pair.first(), point, List.of(), pair.second(), point);
            }
        }
    }

    /**
     * Whether the sheet joins a drawn point to another net than the one at a pin's own KiCad point: the pin standing there
     * would be joined to it.
     */
    private boolean joinsAnotherNet(double[] at, KiCadPoint own) {
        KiCadPoint point = new KiCadPoint(at[0] / SymbolCanvas.UNITS_PER_MILLIMETRE, at[1] / SymbolCanvas.UNITS_PER_MILLIMETRE);
        Optional<String> its = connectivity.netAt(path, own);
        return connectivity.netAt(path, point).filter(there -> !Optional.of(there).equals(its)).isPresent();
    }

    private static boolean same(double[] one, double[] other) {
        return Math.abs(one[0] - other[0]) < 0.01 && Math.abs(one[1] - other[1]) < 0.01;
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
        return new double[] {ImportedCircuit.rounded(point.x() * SymbolCanvas.UNITS_PER_MILLIMETRE),
                ImportedCircuit.rounded(point.y() * SymbolCanvas.UNITS_PER_MILLIMETRE)};
    }

    /** An id no other part or mark of the sheet has: KiCad's reference where it is a name. */
    private String id(String wanted) {
        return ids.of(wanted);
    }

    private static String plain(double number) {
        return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }
}
