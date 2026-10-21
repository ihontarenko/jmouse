package org.jmouse.parallax.kicad;

import org.jmouse.kicad.KiCadPlacement;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadValue;
import org.jmouse.kicad.schematic.KiCadLabel;
import org.jmouse.kicad.schematic.KiCadSchematicBuilder;
import org.jmouse.kicad.symbol.KiCadPinType;
import org.jmouse.kicad.symbol.KiCadSymbolBuilder;
import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.parallax.object.ObjectDefinition.PlacedItem;
import org.jmouse.parallax.object.ObjectDefinition.SchematicWire;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A jMX circuit as a KiCad schematic: the way back from {@link KiCadSheetImport}. Every part is placed as a KiCad symbol
 * whose library copy is drawn from the part's own jMX symbol — its body and its pins where the sheet has them, so every
 * wire lands where it is drawn —, under the library id KiCad knows it by: the library map's, read backwards
 * ({@code resistor} is {@code Device:R}), the one a component made from a KiCad symbol remembers, else
 * {@code Parallax:<component>}. Each unit placed apart ({@code U4.B at …}) is its own KiCad unit; ground marks are power
 * symbols, labels labels, ports hierarchical labels, global nets global labels; wires keep their corners.
 *
 * <p>The schematic is built as KiCad's tree and printed by its printer. What a KiCad sheet cannot say is named in the
 * notes — a block of another sheet, above all, which this export does not place yet.</p>
 */
public final class KiCadSchematicExport {

    /** A sheet's twenty units are KiCad's 1.27 mm grid. */
    private static final double MILLIMETRES_PER_UNIT = 1 / KiCadSymbolDrawing.UNITS_PER_MILLIMETRE;

    /** The library a component with no KiCad counterpart is written under. */
    private static final String OWN_LIBRARY = "Parallax";

    /** What a component made from a KiCad symbol writes as its source: {@code KiCad symbol Device:LED}. */
    private static final String KICAD_SOURCE = "KiCad symbol ";
    /** How an imported footprint's source names the KiCad footprint it was read from. */
    private static final String KICAD_FOOTPRINT_SOURCE = "KiCad footprint ";

    /** How a net joins what names it, among the drawing's keys. */
    private static final String NET = "net:";

    /** The power symbol every ground mark is: its value is the net. */
    private static final String GROUND_SYMBOL = "power:GND";
    private static final String NODE_ZERO = "GND";

    /** The symbol each mark is drawn with on a jMX sheet, for where its terminal stands. */
    private static final Map<String, String> MARK_SYMBOLS = Map.of("ground", "ground-symbol", "common", "common-symbol",
            "label", "net-label-symbol", "port", "port-label-symbol", "global", "global-label-symbol");

    /** A box's pins stand two grid steps apart and leave it through a lead of one — as the sheet draws a box. */
    private static final double GRID = 20;
    private static final double BOX_PITCH = GRID * 2;
    private static final double BOX_LEAD = GRID;

    /** What an export comes out as: the file's name and text, and what a KiCad sheet could not say. */
    public record Exported(String fileName, String text, List<String> notes) {
        public Exported {
            notes = List.copyOf(notes);
        }
    }

    /** How a component is drawn and numbered, as read from its documents. */
    private record Look(String reference, String libraryId, String title, String designator, List<String> units,
                        Map<String, ComponentModel.Pin> pins, List<String> pinOrder, Block.Symbol drawing,
                        Map<String, String> ports, Map<String, List<String>> sides, Map<String, String> numbers,
                        Optional<String> valueParameter, Map<String, Double> defaults, Optional<String> footprint) {
    }

    private final String slug;
    private final Circuit circuit;
    private final ComponentLibrary library;
    private final KiCadLibraryMap map;
    private final KiCadSchematicBuilder schematic;
    private final List<String> notes = new ArrayList<>();
    private final Map<String, CircuitElement> elements = new LinkedHashMap<>();
    private final Map<String, Optional<Look>> looks = new HashMap<>();
    private final Map<String, Optional<Block.Symbol>> symbols = new HashMap<>();
    /** Each footprint's KiCad library id, by its slug. */
    private final Map<String, String> footprintLibraryIds = new HashMap<>();
    /** Which symbol each library id is drawn with on this sheet: a library id holds one drawing. */
    private final Map<String, String> drawingOfLibraryId = new HashMap<>();
    /** Where each terminal meets its wires on the KiCad sheet, by {@code id:pin}; a mark's by {@code id:}. */
    private final Map<String, KiCadPoint> terminals = new HashMap<>();
    /** What the drawing joins: terminals, marks and the nets marks name, each led to the one it is joined with. */
    private final Map<String, String> joined = new HashMap<>();
    private int powerSymbols;
    private int misplacedPins;

    private KiCadSchematicExport(String slug, String title, Circuit circuit, ComponentLibrary library, KiCadLibraryMap map) {
        this.slug = slug;
        this.circuit = circuit;
        this.library = library;
        this.map = map;
        this.schematic = KiCadSchematicBuilder.of(title == null || title.isBlank() ? slug : title, slug);
        circuit.elements().forEach(element -> elements.put(element.id(), element));
    }

    /**
     * @param slug    the circuit's address: the file is named after it
     * @param title   what the project is called in KiCad
     * @param library the components and symbols the circuit's parts name
     * @param map     which KiCad symbol each component is, read backwards
     * @throws IllegalArgumentException when the circuit has no drawing: KiCad draws a schematic, and a circuit without a
     *                                  {@code layout} has only a netlist
     */
    public static Exported of(String slug, String title, Circuit circuit, ComponentLibrary library, KiCadLibraryMap map) {
        if (circuit.layout() == null || circuit.layout().items().isEmpty()) {
            throw new IllegalArgumentException("The circuit has no drawing to export: open it on the bench and save it, so "
                    + "its layout is written.");
        }
        return new KiCadSchematicExport(slug, title, circuit, library, map).export();
    }

    private Exported export() {
        Map<String, List<PlacedItem>> placements = new LinkedHashMap<>();
        for (PlacedItem item : circuit.layout().items()) {
            if (item.mark() == null) {
                placements.computeIfAbsent(item.id(), ignored -> new ArrayList<>()).add(item);
            } else {
                mark(item);
            }
        }
        placements.forEach(this::part);
        for (SchematicWire wire : circuit.layout().wires()) {
            wire(wire);
        }
        joinUndrawnNets();
        if (misplacedPins > 0) {
            notes.add(misplacedPins + " pins stand where KiCad's turn of the symbol puts them, a little off the sheet's "
                    + "drawing: their wires were led there.");
        }
        String fileName = slug + ".kicad_sch";
        return new Exported(fileName, schematic.print(fileName), notes);
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    private void part(String id, List<PlacedItem> placed) {
        CircuitElement element = elements.get(id);
        if (element == null) {
            notes.add(id + " is drawn but is no part of the circuit: left out.");
            return;
        }
        String reference = element.type() == CircuitElementType.MODEL ? element.component()
                : element.type().primitiveComponent();
        Optional<Look> found = look(reference, element.symbol());
        if (found.isEmpty()) {
            notes.add(id + ": '" + reference + "' is a sheet placed as a block, or no component this reader may see: a "
                    + "hierarchical sheet is not exported yet, so it is left out with its wires.");
            return;
        }
        Look look = found.get();
        Map<String, String> terminalPins = terminalPins(element, look);
        Set<String> apart = new LinkedHashSet<>();
        placed.stream().filter(item -> item.unit() != null).forEach(item -> apart.add(item.unit()));
        List<String> together = look.units().stream().filter(unit -> !apart.contains(unit)).toList();
        if (together.size() > 1 && terminalPins.keySet().stream().anyMatch(terminal -> together.subList(1, together.size())
                .stream().anyMatch(unit -> terminal.startsWith(unit + ".")))) {
            notes.add(id + ": units " + String.join(", ", together) + " are drawn as one on the sheet, so in KiCad their "
                    + "wires end on unit " + together.getFirst() + "'s pins.");
        }
        for (PlacedItem item : placed) {
            String unit = item.unit() != null ? item.unit() : together.isEmpty() ? null : together.getFirst();
            Map<String, String> own = new LinkedHashMap<>();
            terminalPins.forEach((terminal, pin) -> {
                String letter = unitOf(terminal);
                boolean mine = item.unit() != null ? item.unit().equals(letter) : letter == null || !apart.contains(letter);
                if (mine) {
                    own.put(terminal, pin);
                }
            });
            placeSymbol(element, look, item, unit, own);
        }
    }

    /** Each terminal of a part and the component's pin it is: a model's by name, a primitive's by order. */
    private static Map<String, String> terminalPins(CircuitElement element, Look look) {
        Map<String, String> terminals = new LinkedHashMap<>();
        if (element.type() == CircuitElementType.MODEL) {
            element.pins().keySet().stream().sorted((first, second) -> Integer.compare(order(look, first), order(look, second)))
                    .forEach(terminal -> terminals.put(terminal, terminal));
        } else if (look.pinOrder().size() >= 2) {
            terminals.put("positive", look.pinOrder().get(0));
            terminals.put("negative", look.pinOrder().get(1));
        }
        return terminals;
    }

    private void placeSymbol(CircuitElement element, Look look, PlacedItem item, String unit, Map<String, String> own) {
        Map<String, double[]> local = new LinkedHashMap<>();
        boolean drawn = look.drawing() != null && own.values().stream().allMatch(pin -> symbolPoint(look, bare(pin)) != null);
        String libraryId = drawn ? look.libraryId() : look.libraryId() + (look.drawing() != null ? "_box" : "");
        if (drawn) {
            own.forEach((terminal, pin) -> local.put(terminal, symbolPoint(look, bare(pin))));
            if (!schematic.hasLibrarySymbol(libraryId)) {
                schematic.librarySymbol(libraryId, drawnSymbol(look));
            }
        } else {
            Map<String, double[]> box = boxPorts(look, own);
            local.putAll(box);
            if (!schematic.hasLibrarySymbol(libraryId)) {
                schematic.librarySymbol(libraryId, boxSymbol(look, libraryId, own, box));
            }
        }
        double rotation = Math.floorMod(Math.round(item.rotation() / 90.0) * 90, 360);
        if (Math.abs(rotation - Math.floorMod((long) item.rotation(), 360)) > 1e-9) {
            notes.add(element.id() + " is turned " + item.rotation() + "°: KiCad turns a symbol by quarters, so it stands at "
                    + rotation + "°.");
        }
        KiCadPlacement placement = new KiCadPlacement(millimetres(item.x(), item.y()),
                item.mirrored() ? (int) rotation : (int) ((360 - rotation) % 360),
                item.mirrored() ? KiCadPlacement.Mirror.Y : KiCadPlacement.Mirror.NONE);
        local.forEach((terminal, point) -> {
            KiCadPoint onSheet = placement.place(library(point));
            KiCadPoint drawnAt = millimetres(sheet(item, point));
            if (!onSheet.sameAs(drawnAt)) {
                misplacedPins++;
            }
            terminals.put(element.id() + ":" + terminal, onSheet);
        });
        int kicadUnit = unit == null ? 1 : look.units().indexOf(unit) + 1;
        List<String> numbers = new ArrayList<>();
        own.values().forEach(pin -> numbers.add(look.numbers().getOrDefault(pin, pin)));
        schematic.symbol(new KiCadSchematicBuilder.PlacedSymbol(libraryId, placement, kicadUnit, element.id(),
                value(element, look), Map.of("Footprint", footprintOf(element, look)), numbers.stream().distinct().toList(),
                labelPoint(look, item, placement, "name", -1), labelPoint(look, item, placement, "value", 1)));
    }

    /** Where a part's name or value is written: where its symbol puts it, else beside the part. */
    private KiCadPoint labelPoint(Look look, PlacedItem item, KiCadPlacement placement, String subject, int side) {
        if (look.drawing() != null) {
            for (Block.SymbolLabel label : look.drawing().labels()) {
                if (label.subject().equals(subject)) {
                    return placement.place(library(new double[] {label.x(), label.y()}));
                }
            }
        }
        return millimetres(item.x(), item.y() + side * 2 * GRID);
    }

    /** The library copy of a part drawn with its jMX symbol: its body in unit 0, each pin in its unit. */
    private org.jmouse.kicad.parser.node.KiCadList drawnSymbol(Look look) {
        KiCadSymbolBuilder symbol = KiCadSymbolBuilder.named(look.libraryId())
                .property("Reference", look.designator()).property("Value", look.title()).property("Footprint", "")
                .property("Datasheet", "~");
        drawBody(symbol, look.drawing());
        boolean named = look.drawing().pins().stream().anyMatch(pin -> pin.label() != null && !pin.label().isBlank());
        if (!named) {
            symbol.hidePinNames();
        }
        if (look.drawing().pins().size() <= 2) {
            symbol.hidePinNumbers();
        }
        Set<String> placed = new LinkedHashSet<>();
        for (Block.SymbolPin drawnPin : look.drawing().pins()) {
            String pinName = look.ports().getOrDefault(drawnPin.pin(), drawnPin.pin());
            ComponentModel.Pin pin = look.pins().get(pinName);
            if (pin == null) {
                continue;
            }
            KiCadPoint at = library(new double[] {drawnPin.x(), drawnPin.y()});
            int angle = inward(drawnPin.x(), -drawnPin.y());
            if (pin.shared() || look.units().isEmpty()) {
                if (placed.add(pinName)) {
                    // A pin every unit shares is the first unit's, as the sheet draws it on the part's own line: in
                    // every unit (KiCad's unit 0) each unit placed apart carried an unwired copy, and KiCad reads the
                    // pad from that one.
                    symbol.pin(1, typeOf(pin), at, angle, 0, drawnPin.label(),
                            look.numbers().getOrDefault(pinName, pinName));
                }
                continue;
            }
            for (int index = 0; index < look.units().size(); index++) {
                String unitPin = look.units().get(index) + "." + pinName;
                symbol.pin(index + 1, typeOf(pin), at, angle, 0, drawnPin.label(),
                        look.numbers().getOrDefault(unitPin, unitPin));
            }
        }
        return symbol.build();
    }

    /** A part without a drawing of its own, as the sheet draws it: a box, its pins on their sides, named inside. */
    private org.jmouse.kicad.parser.node.KiCadList boxSymbol(Look look, String libraryId, Map<String, String> own,
                                                             Map<String, double[]> box) {
        KiCadSymbolBuilder symbol = KiCadSymbolBuilder.named(libraryId)
                .property("Reference", look.designator()).property("Value", look.title()).property("Footprint", "")
                .property("Datasheet", "~");
        double halfWidth = 0;
        double halfHeight = 0;
        for (double[] point : box.values()) {
            halfWidth = Math.max(halfWidth, Math.abs(point[0]) - (Math.abs(point[0]) >= Math.abs(point[1]) ? BOX_LEAD : 0));
            halfHeight = Math.max(halfHeight, Math.abs(point[1]) - (Math.abs(point[1]) > Math.abs(point[0]) ? BOX_LEAD : 0));
        }
        halfWidth = Math.max(halfWidth, GRID);
        halfHeight = Math.max(halfHeight, GRID);
        symbol.rectangle(0, library(new double[] {-halfWidth, -halfHeight}), library(new double[] {halfWidth, halfHeight}));
        for (Map.Entry<String, double[]> port : box.entrySet()) {
            double[] point = port.getValue();
            boolean across = Math.abs(point[0]) >= Math.abs(point[1]);
            double[] edge = across ? new double[] {Math.signum(point[0]) * halfWidth, point[1]}
                    : new double[] {point[0], Math.signum(point[1]) * halfHeight};
            symbol.polyline(0, List.of(library(point), library(edge)));
            String pinName = own.get(port.getKey());
            ComponentModel.Pin pin = look.pins().get(bare(pinName));
            String letter = unitOf(pinName);
            int unit = letter == null ? (look.units().isEmpty() ? 1 : 0) : look.units().indexOf(letter) + 1;
            symbol.pin(unit, pin == null ? KiCadPinType.PASSIVE : typeOf(pin), library(point),
                    inward(point[0], -point[1]), 0, bare(pinName), look.numbers().getOrDefault(pinName, pinName));
        }
        return symbol.build();
    }

    /** A drawing's body: its paths and every element a symbol shows at rest — not a layer drawn while a part fails. */
    private static void drawBody(KiCadSymbolBuilder symbol, Block.Symbol drawing) {
        drawing.paths().forEach(path -> outline(symbol, path));
        drawing.elements().forEach(element -> drawElement(symbol, element));
    }

    private static void drawElement(KiCadSymbolBuilder symbol, Block.SymbolElement element) {
        switch (element.shape()) {
            case "path" -> outline(symbol, element.path());
            case "line" -> {
                List<Double> from = element.geometry().get("from");
                List<Double> to = element.geometry().get("to");
                if (from != null && to != null) {
                    symbol.polyline(0, List.of(library(new double[] {from.get(0), from.get(1)}),
                            library(new double[] {to.get(0), to.get(1)})));
                }
            }
            case "circle" -> {
                List<Double> at = element.geometry().getOrDefault("at", List.of(0.0, 0.0));
                List<Double> radius = element.geometry().get("radius");
                if (radius != null) {
                    symbol.circle(0, library(new double[] {at.get(0), at.get(1)}), radius.getFirst() * MILLIMETRES_PER_UNIT);
                }
            }
            case "box" -> {
                // A box that fills with a figure is a gauge: empty at rest, so not drawn.
                boolean gauge = element.properties().stream().anyMatch(property -> property.name().equals("level"));
                List<Double> at = element.geometry().getOrDefault("at", List.of(0.0, 0.0));
                List<Double> size = element.geometry().get("size");
                if (!gauge && size != null) {
                    symbol.rectangle(0, library(new double[] {at.get(0) - size.get(0) / 2, at.get(1) - size.get(1) / 2}),
                            library(new double[] {at.get(0) + size.get(0) / 2, at.get(1) + size.get(1) / 2}));
                }
                element.children().forEach(child -> drawElement(symbol, child));
            }
            case "group" -> element.children().forEach(child -> drawElement(symbol, child));
            default -> {
                // A 'when' layer is drawn while its condition holds — a failure, a state —, not at rest; a glyph and a
                // bitmap are a display's contents.
            }
        }
    }

    private static void outline(KiCadSymbolBuilder symbol, String path) {
        for (List<double[]> line : PathOutlines.of(path)) {
            symbol.polyline(0, line.stream().map(KiCadSchematicExport::library).toList());
        }
    }

    /**
     * Where each terminal of a part drawn as a box stands: its pins on the sides its component writes — power on top,
     * ground below, outputs right, the rest left —, two grid steps apart, as the sheet's {@code boxGeometry} lays them.
     */
    private static Map<String, double[]> boxPorts(Look look, Map<String, String> own) {
        Map<String, List<String>> sides = new LinkedHashMap<>();
        for (String side : List.of("left", "right", "top", "bottom")) {
            sides.put(side, new ArrayList<>());
        }
        own.forEach((terminal, pin) -> sides.get(sideOf(look, bare(pin))).add(terminal));
        sides.values().forEach(list -> list.sort((first, second) -> Integer.compare(order(look, bare(own.get(first))),
                order(look, bare(own.get(second))))));
        double across = Math.max(Math.max(widest(sides.get("top")), widest(sides.get("bottom"))), BOX_PITCH);
        double horizontalPitch = roundUp(across + 8, BOX_PITCH);
        boolean nameInside = !sides.get("top").isEmpty() && !sides.get("bottom").isEmpty();
        double inner = Math.max(Math.max(widest(sides.get("left")) + widest(sides.get("right")) + 12
                + (nameInside ? labelWidth(look.title()) : 0), sides.get("top").size() * horizontalPitch),
                Math.max(sides.get("bottom").size() * horizontalPitch, 48));
        double halfWidth = roundUp(inner / 2, GRID);
        double halfHeight = Math.max(GRID, roundUp(Math.max(sides.get("left").size(), sides.get("right").size())
                * BOX_PITCH / 2, GRID));
        Map<String, double[]> ports = new LinkedHashMap<>();
        for (String side : List.of("left", "right", "top", "bottom")) {
            List<String> list = sides.get(side);
            for (int index = 0; index < list.size(); index++) {
                if (side.equals("left") || side.equals("right")) {
                    double y = snap(along(list, index, BOX_PITCH));
                    double edge = side.equals("left") ? -halfWidth : halfWidth;
                    ports.put(list.get(index), new double[] {edge + (side.equals("left") ? -BOX_LEAD : BOX_LEAD), y});
                } else {
                    double x = snap(along(list, index, horizontalPitch));
                    double edge = side.equals("top") ? -halfHeight : halfHeight;
                    ports.put(list.get(index), new double[] {x, edge + (side.equals("top") ? -BOX_LEAD : BOX_LEAD)});
                }
            }
        }
        return ports;
    }

    private static String sideOf(Look look, String pin) {
        for (Map.Entry<String, List<String>> side : look.sides().entrySet()) {
            if (side.getValue().contains(pin)) {
                return side.getKey().equals("power") ? "top" : side.getKey();
            }
        }
        ComponentModel.Pin model = look.pins().get(pin);
        if (model != null && model.nature() == ComponentModel.PinNature.POWER) {
            return "top";
        }
        if (model != null && model.nature() == ComponentModel.PinNature.GROUND) {
            return "bottom";
        }
        return model != null && model.output() ? "right" : "left";
    }

    /** A pin's place along its side: as the box's rows write it, then as the component's pins do. */
    private static int order(Look look, String terminal) {
        String pin = bare(terminal);
        int index = 0;
        for (List<String> row : look.sides().values()) {
            for (String written : row) {
                if (written.equals(pin)) {
                    return index;
                }
                index++;
            }
        }
        int declared = look.pinOrder().indexOf(pin);
        return declared < 0 ? Integer.MAX_VALUE : index + declared;
    }

    private static double widest(List<String> names) {
        return names.stream().mapToDouble(name -> labelWidth(name)).max().orElse(0);
    }

    /** How wide a pin's name is written inside a box: 9 px technical type. */
    private static double labelWidth(String text) {
        return text.length() * 5.5 + 6;
    }

    private static double along(List<String> list, int index, double pitch) {
        return (index - (list.size() - 1) / 2.0) * pitch;
    }

    private static double roundUp(double value, double step) {
        return Math.ceil(value / step) * step;
    }

    private static double snap(double value) {
        return Math.round(value / GRID) * GRID;
    }

    // ─── marks ──────────────────────────────────────────────────────────────────────────────────────

    private void mark(PlacedItem item) {
        if (item.mark().equals("junction")) {
            KiCadPoint at = millimetres(item.x(), item.y());
            terminals.put(item.id() + ":", at);
            schematic.junction(at);
            return;
        }
        double[] pin = markPin(item.mark());
        KiCadPoint at = millimetres(sheet(item, pin));
        terminals.put(item.id() + ":", at);
        join(item.id() + ":", NET + (item.net() == null ? ObjectDefinition.Circuit.GROUND : item.net()));
        // The way its body goes from the point it connects at: towards the mark's own origin.
        double[] body = turned(new double[] {-pin[0], -pin[1]}, item.rotation(), item.mirrored());
        switch (item.mark()) {
            case "ground", "common" -> groundSymbol(at, body, item.net() == null ? NODE_ZERO : item.net());
            case "label" -> schematic.label(KiCadLabel.Kind.LOCAL, net(item), at, labelAngle(body), null);
            case "global" -> schematic.label(KiCadLabel.Kind.GLOBAL, net(item), at, labelAngle(body), "passive");
            case "port" -> schematic.label(KiCadLabel.Kind.HIERARCHICAL, net(item), at, labelAngle(body),
                    portShape(net(item)));
            default -> notes.add(item.id() + " is a '" + item.mark() + "' mark, which has no KiCad counterpart.");
        }
    }

    /** A ground: KiCad's power symbol, its value the net — GND for node 0. */
    private void groundSymbol(KiCadPoint at, double[] body, String value) {
        if (!schematic.hasLibrarySymbol(GROUND_SYMBOL)) {
            schematic.librarySymbol(GROUND_SYMBOL, KiCadSymbolBuilder.named(GROUND_SYMBOL).power()
                    .property("Reference", "#PWR").property("Value", NODE_ZERO).property("Footprint", "")
                    .property("Datasheet", "").hidePinNumbers()
                    .polyline(0, List.of(new KiCadPoint(0, 0), new KiCadPoint(0, -1.27), new KiCadPoint(1.27, -1.27),
                            new KiCadPoint(0, -2.54), new KiCadPoint(-1.27, -1.27), new KiCadPoint(0, -1.27)))
                    .pin(1, KiCadPinType.POWER_INPUT, new KiCadPoint(0, 0), 270, 0, NODE_ZERO, "1").build());
        }
        // The body hangs below its pin unturned; of the four turns, the one that hangs it the way the sheet does.
        int best = 0;
        double bestDot = -Double.MAX_VALUE;
        for (int angle = 0; angle < 360; angle += 90) {
            KiCadPlacement turn = new KiCadPlacement(new KiCadPoint(0, 0), angle, KiCadPlacement.Mirror.NONE);
            KiCadPoint hangs = turn.place(new KiCadPoint(0, -1));
            double dot = hangs.x() * body[0] + hangs.y() * body[1];
            if (dot > bestDot) {
                bestDot = dot;
                best = angle;
            }
        }
        KiCadPlacement placement = new KiCadPlacement(at, best, KiCadPlacement.Mirror.NONE);
        powerSymbols++;
        schematic.symbol(new KiCadSchematicBuilder.PlacedSymbol(GROUND_SYMBOL, placement, 1,
                String.format(Locale.ROOT, "#PWR%02d", powerSymbols), value, Map.of(), List.of("1"),
                placement.place(new KiCadPoint(0, -6.35)), placement.place(new KiCadPoint(0, -3.81))));
    }

    /** The point a mark's symbol connects at, in its own drawing. */
    private double[] markPin(String mark) {
        return symbol(MARK_SYMBOLS.get(mark)).filter(drawing -> !drawing.pins().isEmpty())
                .map(drawing -> new double[] {drawing.pins().getFirst().x(), drawing.pins().getFirst().y()})
                .orElse(new double[] {0, 0});
    }

    private String net(PlacedItem item) {
        return item.net() == null ? NODE_ZERO : item.net();
    }

    private String portShape(String net) {
        ObjectDefinition.SheetBoundary boundary = circuit.boundary();
        String direction = boundary == null ? "analog" : boundary.ports().stream().filter(port -> port.net().equals(net))
                .map(ObjectDefinition.SheetPort::direction).findFirst().orElse("analog");
        return switch (direction) {
            case "input" -> "input";
            case "output" -> "output";
            default -> "passive";
        };
    }

    /** A label's angle: its text runs from the point it connects at the way the mark's body goes. */
    private static int labelAngle(double[] body) {
        double degrees = Math.toDegrees(Math.atan2(-body[1], body[0]));
        return (int) Math.floorMod(Math.round(degrees / 90) * 90, 360);
    }

    // ─── wires ──────────────────────────────────────────────────────────────────────────────────────

    private void wire(SchematicWire wire) {
        Optional<KiCadPoint> from = terminal(wire.from());
        Optional<KiCadPoint> to = terminal(wire.to());
        if (from.isEmpty() || to.isEmpty()) {
            notes.add(wire.id() + " runs " + wire.from() + " to " + wire.to() + ", an end of which is not on the KiCad sheet: "
                    + "left out.");
            return;
        }
        List<KiCadPoint> points = new ArrayList<>();
        points.add(from.get());
        wire.bends().forEach(bend -> points.add(millimetres(bend.x(), bend.y())));
        points.add(to.get());
        schematic.wire(points);
        join(terminalKey(wire.from()), terminalKey(wire.to()));
    }

    /**
     * Every net the circuit has that the drawing does not join whole — pins that touched in KiCad and were imported with
     * no wire, a part's power pins no wire reaches — is joined in KiCad by a label of its name on each piece of it that
     * no mark names yet: a ground symbol for node 0, a global or hierarchical label for a net that is one, else a label
     * of this sheet. The netlist is the truth; the drawing only shows it.
     */
    private void joinUndrawnNets() {
        Map<String, List<String>> nets = new LinkedHashMap<>();
        for (CircuitElement element : circuit.elements()) {
            element.pins().forEach((pin, net) -> nets.computeIfAbsent(net, ignored -> new ArrayList<>())
                    .add(element.id() + ":" + pin));
            if (element.positive() != null) {
                nets.computeIfAbsent(element.positive(), ignored -> new ArrayList<>()).add(element.id() + ":positive");
            }
            if (element.negative() != null) {
                nets.computeIfAbsent(element.negative(), ignored -> new ArrayList<>()).add(element.id() + ":negative");
            }
        }
        int labelled = 0;
        for (Map.Entry<String, List<String>> net : nets.entrySet()) {
            Map<String, String> pieces = new LinkedHashMap<>();
            net.getValue().stream().filter(terminals::containsKey).forEach(key -> pieces.putIfAbsent(find(key), key));
            // A net drawn in one piece is whole, named or not.
            if (pieces.size() < 2) {
                continue;
            }
            String named = find(NET + net.getKey());
            boolean anyNamed = pieces.containsKey(named) && joined.containsKey(NET + net.getKey());
            for (Map.Entry<String, String> piece : pieces.entrySet()) {
                if (anyNamed && piece.getKey().equals(named)) {
                    continue;
                }
                connector(net.getKey(), terminals.get(piece.getValue()));
                join(piece.getValue(), NET + net.getKey());
                labelled++;
            }
        }
        if (labelled > 0) {
            notes.add(labelled + " pieces of nets the sheet draws no wire between — pins that touched in KiCad, a part's "
                    + "power pins — are joined in KiCad by a label of their net's name.");
        }
    }

    /** What names a net at a point: a ground symbol for node 0, the label its kind of net takes elsewhere. */
    private void connector(String net, KiCadPoint at) {
        if (net.equals(ObjectDefinition.Circuit.GROUND)) {
            groundSymbol(at, new double[] {0, 1}, NODE_ZERO);
            return;
        }
        ObjectDefinition.SheetBoundary boundary = circuit.boundary();
        if (boundary != null && boundary.ports().stream().anyMatch(port -> port.net().equals(net))) {
            schematic.label(KiCadLabel.Kind.HIERARCHICAL, net, at, 0, portShape(net));
        } else if (boundary != null && boundary.globals().contains(net)) {
            schematic.label(KiCadLabel.Kind.GLOBAL, net, at, 0, "passive");
        } else {
            schematic.label(KiCadLabel.Kind.LOCAL, net, at, 0, null);
        }
    }

    private void join(String first, String second) {
        joined.putIfAbsent(first, first);
        joined.putIfAbsent(second, second);
        String firstRoot = find(first);
        String secondRoot = find(second);
        if (!firstRoot.equals(secondRoot)) {
            joined.put(firstRoot, secondRoot);
        }
    }

    private String find(String key) {
        String root = joined.getOrDefault(key, key);
        while (!root.equals(joined.getOrDefault(root, root))) {
            root = joined.getOrDefault(root, root);
        }
        return root;
    }

    /** What a wire's end is joined as: a mark by its id, a part's terminal by its id and pin. */
    private String terminalKey(String written) {
        int dot = written.indexOf('.');
        String id = dot < 0 ? written : written.substring(0, dot);
        return terminals.containsKey(id + ":") ? id + ":" : id + ":" + (dot < 0 ? "" : written.substring(dot + 1));
    }

    /** Where a wire's end meets its terminal: {@code R1.positive}, {@code U4.B.output}, {@code GND1.ground}, {@code J1.j}. */
    private Optional<KiCadPoint> terminal(String written) {
        int dot = written.indexOf('.');
        String id = dot < 0 ? written : written.substring(0, dot);
        String pin = dot < 0 ? "" : written.substring(dot + 1);
        KiCadPoint mark = terminals.get(id + ":");
        return mark != null ? Optional.of(mark) : Optional.ofNullable(terminals.get(id + ":" + pin));
    }

    // ─── components ─────────────────────────────────────────────────────────────────────────────────

    /**
     * How a component is drawn with one of its symbols: the one a part names when it is drawn with another than the
     * default ({@code symbol operational-amplifier-wide-symbol}), else its default.
     */
    private Optional<Look> look(String reference, String variant) {
        return looks.computeIfAbsent(reference + "|" + (variant == null ? "" : variant), key -> read(reference, variant));
    }

    private Optional<Look> read(String reference, String variant) {
        Optional<String> source = library.source(reference);
        if (source.isEmpty()) {
            return Optional.empty();
        }
        ExperienceDocument document = ExperienceReader.read(source.get(), reference + ".jmx").getFirst();
        if (document.kind() != Kind.COMPONENT) {
            return Optional.empty();
        }
        ComponentModel model = ComponentDocuments.read(source.get(), reference, library);
        String slug = reference.replaceFirst("@.*$", "");
        Optional<KiCadLibraryMap.Mapping> mapping = map.libraryIdOf(slug).flatMap(map::mappingOf);
        String libraryId = map.libraryIdOf(slug)
                .or(() -> Optional.ofNullable(document.meta().source()).filter(written -> written.startsWith(KICAD_SOURCE))
                        .map(written -> written.substring(KICAD_SOURCE.length()).strip()))
                .orElse(OWN_LIBRARY + ":" + slug);
        Map<String, ComponentModel.Pin> pins = new LinkedHashMap<>();
        model.pins().forEach(pin -> pins.put(pin.name(), pin));
        Map<String, String> numbers = new LinkedHashMap<>();
        model.pins().forEach(pin -> pin.numbers().forEach((unit, number) -> numbers.putIfAbsent(
                unit.isEmpty() || pin.shared() ? pin.name() : unit + "." + pin.name(), String.valueOf(number))));
        // A pin a jMX number cannot say — KiCad's A, S1 or B3 — is named after it by the import, pinA: the number is read back.
        model.pins().stream().filter(pin -> pin.numbers().isEmpty()).forEach(pin -> KiCadSymbolDrawing.numberOfPinName(pin.name())
                .ifPresent(number -> numbers.putIfAbsent(pin.name(), number)));
        // The map's own numbering wins: KiCad's diode is 1 = K where the catalogue's 1 is its anode.
        mapping.ifPresent(found -> found.pins().forEach((number, pin) -> numbers.put(pin, number)));
        Optional<Block.Symbol> uses = document.block(Block.Symbol.class);
        // The symbol the part is drawn with: the variant it names when its component offers it, else the default.
        Optional<String> drawnWith = uses.filter(symbol -> !symbol.uses().isEmpty()).map(symbol ->
                variant != null && symbol.uses().contains(variant) ? variant : symbol.uses().getFirst());
        Block.Symbol drawing = drawnWith.flatMap(this::symbol).orElse(null);
        // KiCad keeps one drawing to a library symbol: the first drawing of a component holds its library id — the one
        // the library map reads back —, a second, different one of the same component on this sheet a name of its own.
        if (drawnWith.isPresent()) {
            String holder = drawingOfLibraryId.putIfAbsent(libraryId, drawnWith.get());
            if (holder != null && !holder.equals(drawnWith.get())) {
                libraryId = libraryId + "_" + drawnWith.get();
            }
        }
        Map<String, List<String>> sides = new LinkedHashMap<>();
        uses.filter(symbol -> symbol.shape().isPresent()).ifPresent(symbol -> symbol.sides().forEach((side, written) ->
                sides.put(side, written.stream().map(entry -> entry.strip().split("\\s+")[0]).toList())));
        Map<String, Double> defaults = new LinkedHashMap<>();
        model.parameters().forEach(parameter -> defaults.put(parameter.name(), parameter.value()));
        return Optional.of(new Look(reference, libraryId, document.title(), designatorOf(document),
                List.copyOf(model.units()), pins, List.copyOf(pins.keySet()), drawing,
                uses.map(Block.Symbol::ports).orElse(Map.of()), sides, numbers,
                mapping.flatMap(KiCadLibraryMap.Mapping::value), defaults,
                document.block(Block.FootprintUse.class).map(Block.FootprintUse::footprint)));
    }

    /** The KiCad footprint a part sits on: the one its own line names, else its component's; empty when neither does. */
    private String footprintOf(CircuitElement element, Look look) {
        return Optional.ofNullable(element.footprint()).or(look::footprint).map(this::footprintLibraryId).orElse("");
    }

    /** A footprint as KiCad names it: the library id it was imported from, else the slug in this product's library. */
    private String footprintLibraryId(String footprint) {
        return footprintLibraryIds.computeIfAbsent(footprint, slug -> library.source(slug)
                .map(source -> ExperienceReader.read(source, slug + ".jmx").getFirst().meta().source())
                .filter(written -> written != null && written.startsWith(KICAD_FOOTPRINT_SOURCE))
                .map(written -> written.substring(KICAD_FOOTPRINT_SOURCE.length()).strip())
                .orElse(OWN_LIBRARY + ":" + slug));
    }

    private Optional<Block.Symbol> symbol(String slug) {
        return symbols.computeIfAbsent(slug, wanted -> library.source(wanted)
                .map(source -> ExperienceReader.read(source, wanted + ".jmx").getFirst())
                .flatMap(document -> document.block(Block.Symbol.class)));
    }

    private static String designatorOf(ExperienceDocument document) {
        return document.meta().field("designator").map(value -> switch (value) {
            case Meta.MetaValue.Names names -> names.names().isEmpty() ? "U" : names.names().getFirst();
            case Meta.MetaValue.Text text -> text.text();
            default -> "U";
        }).orElse("U");
    }

    /** The point the drawing puts a component's pin at, through the port its symbol line maps to it. */
    private static double[] symbolPoint(Look look, String pin) {
        for (Block.SymbolPin drawn : look.drawing().pins()) {
            if (look.ports().getOrDefault(drawn.pin(), drawn.pin()).equals(pin)) {
                return new double[] {drawn.x(), drawn.y()};
            }
        }
        return null;
    }

    /** What KiCad shows as the part's value: its value parameter as KiCad writes it ({@code 10k}), else its name. */
    private static String value(CircuitElement element, Look look) {
        if (element.type() != CircuitElementType.MODEL) {
            return written(element.value()).orElse(look.title());
        }
        return look.valueParameter().flatMap(parameter -> Optional.ofNullable(element.overrides().get(parameter))
                .flatMap(KiCadSchematicExport::written)
                .or(() -> Optional.ofNullable(look.defaults().get(parameter)).map(KiCadValue::written)))
                .orElse(look.title());
    }

    private static Optional<String> written(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Quantity.parse(value).map(quantity -> KiCadValue.written(quantity.value())).or(() -> Optional.of(value));
    }

    private static KiCadPinType typeOf(ComponentModel.Pin pin) {
        if (pin.nature() == ComponentModel.PinNature.POWER || pin.nature() == ComponentModel.PinNature.GROUND) {
            return pin.direction() == ComponentModel.PinDirection.OUTPUT ? KiCadPinType.POWER_OUTPUT : KiCadPinType.POWER_INPUT;
        }
        return switch (pin.direction()) {
            case OUTPUT -> KiCadPinType.OUTPUT;
            case BIDIRECTIONAL -> KiCadPinType.BIDIRECTIONAL;
            case INPUT -> pin.nature() == ComponentModel.PinNature.DIGITAL ? KiCadPinType.INPUT : KiCadPinType.PASSIVE;
        };
    }

    /**
     * Which way a pin points from where a wire meets it into the body, in a library's terms (Y upwards): a pin on the
     * left points right (0), on the right left (180), on top down (270), below up (90).
     */
    private static int inward(double x, double y) {
        if (Math.abs(x) >= Math.abs(y)) {
            return x < 0 ? 0 : 180;
        }
        return y > 0 ? 270 : 90;
    }

    // ─── geometry ───────────────────────────────────────────────────────────────────────────────────

    /** A unit's pin, {@code A.output}, is the pin {@code output}. */
    private static String bare(String pin) {
        return pin == null ? "" : pin.substring(pin.indexOf('.') + 1);
    }

    private static String unitOf(String terminal) {
        int dot = terminal.indexOf('.');
        return dot > 0 ? terminal.substring(0, dot) : null;
    }

    /** A point of a jMX drawing (Y downwards) in a KiCad library's terms (millimetres, Y upwards). */
    private static KiCadPoint library(double[] point) {
        return new KiCadPoint(rounded(point[0] * MILLIMETRES_PER_UNIT), rounded(-point[1] * MILLIMETRES_PER_UNIT));
    }

    private static KiCadPoint millimetres(double x, double y) {
        return new KiCadPoint(rounded(x * MILLIMETRES_PER_UNIT), rounded(y * MILLIMETRES_PER_UNIT));
    }

    private static KiCadPoint millimetres(double[] point) {
        return millimetres(point[0], point[1]);
    }

    /** A point of a part's drawing on the jMX sheet: mirrored left to right first, then turned clockwise, then moved. */
    private static double[] sheet(PlacedItem item, double[] point) {
        double[] turned = turned(point, item.rotation(), item.mirrored());
        return new double[] {item.x() + turned[0], item.y() + turned[1]};
    }

    private static double[] turned(double[] point, double rotation, boolean mirrored) {
        double radians = Math.toRadians(rotation);
        double localX = mirrored ? -point[0] : point[0];
        return new double[] {localX * Math.cos(radians) - point[1] * Math.sin(radians),
                localX * Math.sin(radians) + point[1] * Math.cos(radians)};
    }

    /** To a hundred-thousandth of a millimetre: what is left of a binary fraction is noise, not a position. */
    private static double rounded(double value) {
        return Math.round(value * 100_000) / 100_000.0;
    }
}
