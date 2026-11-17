package org.jmouse.cad.kicad;

import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.sheet.BlockFrame;
import org.jmouse.cad.exchange.sheet.PartPins;
import org.jmouse.cad.exchange.sheet.SheetGeometry;
import org.jmouse.cad.exchange.sheet.SymbolLook;
import org.jmouse.cad.exchange.sheet.SymbolLooks;
import org.jmouse.cad.exchange.imported.SymbolCanvas;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Point;
import org.jmouse.cad.Setting;
import org.jmouse.cad.Text;
import org.jmouse.cad.circuit.Bus;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.Seat;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentFacts;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.kicad.KiCadPlacement;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadValue;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.kicad.schematic.KiCadBus;
import org.jmouse.kicad.schematic.KiCadLabel;
import org.jmouse.kicad.schematic.KiCadSchematicBuilder;
import org.jmouse.kicad.symbol.KiCadPinType;
import org.jmouse.kicad.symbol.KiCadSymbolBuilder;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.jmouse.cad.exchange.sheet.SymbolLook.bare;
import static org.jmouse.cad.exchange.sheet.SymbolLook.unitOf;

/**
 * A circuit as a KiCad schematic: the way back from {@link KiCadSheetImport}. Every part is placed as a KiCad symbol
 * whose library copy is drawn from the part's own symbol — its body and its pins where the sheet has them, so every wire
 * lands where it is drawn —, under the library id KiCad knows it by: the library map's, read backwards ({@code resistor}
 * is {@code Device:R}), the one a component made from a KiCad symbol remembers, else the own library's. Each block placed
 * apart ({@code U4.B at …}) is its own KiCad unit; ground marks are power symbols, labels labels, ports hierarchical
 * labels, global nets global labels; wires keep their corners.
 *
 * <p>The schematic is built as KiCad's tree and printed by its printer. What a KiCad sheet cannot say is named in the
 * notes — a block of another sheet, above all, which this export does not place yet.</p>
 */
public final class KiCadSchematicExport {

    /** A sheet's twenty units are KiCad's 1.27 mm grid. */
    private static final double MILLIMETRES_PER_UNIT = 1 / SymbolCanvas.UNITS_PER_MILLIMETRE;

    /** How a net joins what names it, among the drawing's keys. */
    private static final String NET = "net:";

    /** KiCad's power symbol every ground mark is, its value the net, and the name it gives node 0. */
    /**
     * A power symbol as KiCad's power library draws one: its name, its strokes, and which way its body goes from its pin in its own
     * drawing (KiCad's Y up) — a ground hangs below its pin, a rail stands over it.
     */
    private record PowerShape(String name, List<KiCadPoint> strokes, KiCadPoint body, KiCadPoint valueAt) {
    }

    /** KiCad's {@code power:GND}: a stem down to a triangle. */
    private static final PowerShape GROUND_SHAPE = new PowerShape("power:GND", List.of(new KiCadPoint(0, 0), new KiCadPoint(0, -1.27),
            new KiCadPoint(1.27, -1.27), new KiCadPoint(0, -2.54), new KiCadPoint(-1.27, -1.27), new KiCadPoint(0, -1.27)),
            new KiCadPoint(0, -1), new KiCadPoint(0, -3.81));
    /** A supply rail, as KiCad's {@code power:VCC} draws one: a stem up to a bar, the rail's name over it. */
    private static final PowerShape RAIL_SHAPE = new PowerShape("power:RAIL", List.of(new KiCadPoint(0, 0), new KiCadPoint(0, 1.27),
            new KiCadPoint(-1.27, 1.27), new KiCadPoint(1.27, 1.27)), new KiCadPoint(0, 1), new KiCadPoint(0, 3.81));
    private static final String NODE_ZERO = KiCadNetNames.KICAD_GROUND;

    /** The field a symbol names its footprint in. */
    private static final String FOOTPRINT_FIELD = "Footprint";

    /** KiCad's power flag: what a designer stands on a net that is fed though no pin of it gives power. */
    private static final String POWER_FLAG_SYMBOL = "power:PWR_FLAG";
    private static final String POWER_FLAG = "PWR_FLAG";

    /** How many straight steps a curve of a drawing is followed in, KiCad drawing polylines. */
    private static final int CURVE_STEPS = 8;

    /** The property of a box element that fills it with a figure: a gauge, empty at rest. */
    private static final String GAUGE_LEVEL = "level";

    /**
     * What an export comes out as: the file's name and text, and what a KiCad sheet could not say.
     *
     * @param symbols the symbol each part is drawn as, by its reference: what the footprint of a board laid out from it names
     * @param nets    what KiCad names each of the circuit's nets, by our name: what the board's copper of it must be called
     * @param librarySymbols every library symbol its sheets draw, by its full name: what the project's own library holds of them
     */
    public record Exported(String fileName, String text, List<String> notes, Map<String, Symbol> symbols, Map<String, String> nets,
                           List<ExportedSheet> sheets, Map<String, SExpressionList> librarySymbols) {

        public Exported {
            notes = List.copyOf(notes);
            symbols = Map.copyOf(symbols);
            nets = Map.copyOf(nets);
            sheets = List.copyOf(sheets);
            librarySymbols = Collections.unmodifiableMap(new LinkedHashMap<>(librarySymbols));
        }
    }

    /** A sheet below the root, as its own file beside the root's: one for each sheet document, however often it is placed. */
    public record ExportedSheet(String fileName, String text) {
    }

    /**
     * One place a sheet stands in the design, as KiCad keeps it.
     *
     * @param path      its instance's path, {@code /root-uuid/sheet-uuid…}: its symbols' references are kept under it
     * @param boardPath the same without the root, {@code /sheet-uuid…}: what a footprint of a part on it names before its symbol
     * @param blocks    the blocks it is placed through, outermost first — what its parts and nets are named after when unfolded
     * @param page      the page KiCad numbers it as
     */
    private record Place(String path, String boardPath, List<String> blocks, String page) {
    }

    /** What every sheet of a design writes into, and where each sheet stands: walked once before any sheet is written. */
    private static final class Design {

        private final String project;
        private final String rootIdentity;
        private final UnaryOperator<String> references;
        private final CatalogueDocuments catalogue;
        private final Map<String, List<Place>> places = new LinkedHashMap<>();
        private final Map<String, Circuit> circuits = new LinkedHashMap<>();
        private final Map<String, String> titles = new LinkedHashMap<>();
        private final Map<String, Symbol> symbols = new LinkedHashMap<>();
        private final Map<String, KiCadNetName> nets = new LinkedHashMap<>();
        private final Map<String, SExpressionList> librarySymbols = new LinkedHashMap<>();
        private final List<String> notes = new ArrayList<>();
        private int pages;

        private Design(String project, UnaryOperator<String> references, CatalogueDocuments catalogue) {
            this.project = project;
            this.rootIdentity = KiCadSchematicBuilder.sheetIdentity(project);
            this.references = references;
            this.catalogue = catalogue;
        }

        /** A sheet where it stands, and every sheet its blocks place, each place a step further down every place of its parent. */
        private void place(String slug, String title, Circuit circuit, List<Place> at, Set<String> walking) {
            places.computeIfAbsent(slug, ignored -> new ArrayList<>()).addAll(at);
            circuits.putIfAbsent(slug, circuit);
            titles.putIfAbsent(slug, title);
            if (!walking.add(slug)) {
                notes.add(slug + " places itself through its blocks: its sheets are written once, not without end.");
                return;
            }
            for (Part part : circuit.parts()) {
                Optional<DocumentNode> sheet = sheetOf(catalogue, part);
                if (sheet.isEmpty()) {
                    continue;
                }
                String step = KiCadSchematicBuilder.placedSheetIdentity(slug, part.reference());
                List<Place> below = new ArrayList<>();
                for (Place place : at) {
                    List<String> blocks = new ArrayList<>(place.blocks());
                    blocks.add(part.reference());
                    below.add(new Place(place.path() + "/" + step, place.boardPath() + "/" + step, List.copyOf(blocks), String.valueOf(++pages)));
                }
                place(sheet.get().slug(), sheet.get().title(), SimulationReading.circuit(sheet.get()), below, walking);
            }
            walking.remove(slug);
        }
    }

    /** The sheet a part places, when it is a block: a circuit of the design rather than a component. */
    private static Optional<DocumentNode> sheetOf(CatalogueDocuments catalogue, Part part) {
        return catalogue.find(part.component()).filter(found -> found.kind().equals(SimulationModule.CIRCUIT));
    }

    /**
     * A part as the schematic places it.
     *
     * @param path  its symbol, {@code /symbol-uuid} — its first unit's —, as a footprint names the symbol it stands for
     * @param value its value as the symbol says it, which KiCad holds the footprint's to
     * @param fields every other field the symbol carries — what its line sets, {@code position} of a trimmer —, which KiCad
     *               copies onto the footprint and checks the board by: not its footprint, which the board names itself
     */
    public record Symbol(String path, String value, Map<String, String> fields) {

        public Symbol {
            fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        }
    }

    /** How strongly a sheet names a net: KiCad calls a net by the strongest name it is given. */
    private enum NameStrength {
        /** A label of this sheet, or one handed to the sheet above: the net under the sheet's path. */
        SHEET,
        /** A global label: the net by its text, everywhere. */
        GLOBAL,
        /** A power symbol: the net by its value, everywhere. */
        POWER
    }

    private record KiCadNetName(String name, NameStrength strength) {
    }

    /** What a label of this sheet names its net under: the root sheet's path. */
    private static final String ROOT_SHEET_PATH = "/";

    /**
     * How a component is drawn and numbered in KiCad: how the sheet draws it ({@link SymbolLook}), and the library symbol
     * KiCad holds it as — its id, its pins' numbers, its value and footprint.
     */
    private record Look(SymbolLook face, String libraryId, String designator, Map<String, String> numbers,
                        Optional<String> valueParameter, Map<String, Double> defaults, Optional<String> footprint,
                        Map<String, Map<String, String>> padsOf, Map<String, List<String>> stacked) {

        String title() {
            return face.title();
        }

        List<String> units() {
            return face.units();
        }

        Map<String, Pin> pins() {
            return face.pins();
        }

        Set<String> shared() {
            return face.shared();
        }

        Map<String, String> ports() {
            return face.ports();
        }

        /** The symbol's drawing; null for a component drawn as a box. */
        SymbolDrawing drawing() {
            return face.drawing().orElse(null);
        }

        /**
         * The look of a part whose pins land on these pads ({@code pin → pad}): this one where its symbol already numbers
         * every pin as the pad it lands on; else a library symbol of its own numbered by the pads, named after the footprint —
         * KiCad joins a symbol's pin to the pad of its number, so a KiCad symbol numbered otherwise than the footprint the part
         * stands on would join its pins to the wrong pads.
         */
        Look landedOn(String footprint, Map<String, List<String>> pads) {
            // A pin on several pads — a regulator's input on three — is KiCad's stacked pins: one for each pad, on one point.
            Map<String, List<String>> onPads = new LinkedHashMap<>();
            pads.forEach((pin, numbered) -> onPads.put(pin, List.copyOf(numbered)));
            boolean agrees = pads.entrySet().stream().allMatch(landing -> landing.getValue().getFirst().equals(numbers.get(landing.getKey())));
            if (agrees || footprint == null) {
                return new Look(face, libraryId, designator, numbers, valueParameter, defaults, this.footprint, padsOf, onPads);
            }
            Map<String, String> renumbered = new LinkedHashMap<>(numbers);
            pads.forEach((pin, numbered) -> renumbered.put(pin, numbered.getFirst()));
            return new Look(face, libraryId + "_" + footprint, designator, renumbered, valueParameter, defaults, this.footprint,
                    padsOf, onPads);
        }

        /** The numbers a pin is written with: every pad it lands on, its own number first; one number where it lands on one. */
        List<String> numbersOf(String pin) {
            List<String> onPads = stacked.get(pin);
            String own = numbers.getOrDefault(pin, pin);
            if (onPads == null || onPads.size() < 2) {
                return List.of(own);
            }
            List<String> all = new ArrayList<>(List.of(own));
            onPads.stream().filter(number -> !number.equals(own)).forEach(all::add);
            return List.copyOf(all);
        }
    }

    private final String slug;
    private final Circuit circuit;
    private final CatalogueDocuments catalogue;
    private final KiCadLibraryMap map;
    private final KiCadLibraries libraries;
    private final KiCadSchematicBuilder schematic;
    private final PathParser paths = new PathParser();
    private final List<String> notes = new ArrayList<>();
    private final Map<String, Part> parts = new LinkedHashMap<>();
    private final Map<String, Optional<Look>> looks = new HashMap<>();
    /** How the sheet draws each component, and the symbols it draws them with: the engine's, which the sheet's checks read too. */
    private final SymbolLooks faces;
    /** Each footprint's KiCad library id, as the board laid out from this schematic names it too. */
    private final KiCadFootprintIds footprintIds;
    /** Where each part's pins land: the one answer the pcb's copper follows too. */
    private final ComponentFacts facts;
    /** Each part's first unit placed, which its footprint names, and its value as its symbol says it. */
    private final Map<String, Integer> firstUnits = new LinkedHashMap<>();
    private final Map<String, String> values = new LinkedHashMap<>();
    /** Each part's fields besides its value and footprint, by its reference: what a board copies onto its footprint. */
    private final Map<String, Map<String, String>> partFields = new LinkedHashMap<>();
    /** What KiCad will call each net, by our name. */
    private final Map<String, KiCadNetName> kicadNets = new LinkedHashMap<>();
    /** Which symbol each library id is drawn with on this sheet: a library id holds one drawing. */
    private final Map<String, String> drawingOfLibraryId = new HashMap<>();
    /** Where each terminal meets its wires on the KiCad sheet, by {@code reference:pin}; a mark's by {@code reference:}. */
    private final Map<String, KiCadPoint> terminals = new HashMap<>();
    /** What the drawing joins: terminals, marks and the nets marks name, each led to the one it is joined with. */
    private final Map<String, String> joined = new HashMap<>();
    /** The circuit's parameters, computed: what a part's setting written over them is, where KiCad holds no parameter. */
    private final Map<String, Double> parameterValues = new LinkedHashMap<>();
    /** Where a bus leaves this sheet — its hierarchical label, a box's bus pin —, and which way its line goes from there. */
    private final List<BusAnchor> busAnchors = new ArrayList<>();

    /**
     * Where a bus crosses this sheet's edge: the point KiCad's label or pin stands on, and the way into the sheet.
     *
     * @param bus     the bus of this sheet, by name
     * @param at      the label's or the pin's point, in millimetres
     * @param inwards which way its line goes from there
     */
    private record BusAnchor(String bus, KiCadPoint at, double[] inwards) {
    }

    /** KiCad's grid: a bus entry's run along and across its bus. */
    private static final double KICAD_STEP = 2.54;
    private int powerSymbols;
    private int powerFlags;
    /** Each net of this sheet that takes power, by its name here: what {@link #flagUndrivenPower} flags. */
    private final Map<String, PowerNet> powerNets = new LinkedHashMap<>();
    private int misplacedPins;
    /** The design this sheet is one of, and whether it is its root. */
    private final Design design;
    private final boolean root;

    private KiCadSchematicExport(String slug, String title, Circuit circuit, CatalogueDocuments catalogue, KiCadLibraryMap map,
                                 KiCadLibraries libraries, Design design) {
        this.slug = slug;
        this.circuit = circuit;
        this.catalogue = catalogue;
        this.map = map;
        this.libraries = libraries;
        this.design = design;
        this.root = slug.equals(design.project);
        String titled = title == null || title.isBlank() ? slug : title;
        // KiCad keeps a symbol's reference under the project's name, which is its files': the root schematic's own name.
        this.schematic = (root ? KiCadSchematicBuilder.of(design.project, slug) : KiCadSchematicBuilder.child(design.project, slug))
                .title(titled);
        this.footprintIds = new KiCadFootprintIds(catalogue, map, libraries);
        this.faces = new SymbolLooks(catalogue);
        this.facts = new ComponentFacts(catalogue);
        circuit.parts().forEach(part -> parts.put(part.reference(), part));
        ParameterEvaluation.evaluate(slug, circuit.parameters())
                .forEach(parameter -> parameterValues.put(parameter.name(), parameter.value()));
    }

    /**
     * @param slug      the circuit's address: the file is named after it
     * @param title     what the project is called in KiCad
     * @param catalogue the components and symbols the circuit's parts name
     * @param map       which KiCad symbol each component is, read backwards
     * @throws IllegalArgumentException when the circuit has no drawing: KiCad draws a schematic, and a circuit without a
     *                                  {@code layout} has only a netlist
     */
    public static Exported of(String slug, String title, Circuit circuit, CatalogueDocuments catalogue, KiCadLibraryMap map,
                              KiCadLibraries libraries) {
        return of(slug, title, circuit, catalogue, map, libraries, UnaryOperator.identity());
    }

    /**
     * The same, each part called in KiCad what the board laid out beside it calls it.
     *
     * @param references what KiCad calls a part, by its name in the unfolded design ({@code S1_R1})
     */
    public static Exported of(String slug, String title, Circuit circuit, CatalogueDocuments catalogue, KiCadLibraryMap map,
                              KiCadLibraries libraries, UnaryOperator<String> references) {
        if (!drawn(circuit)) {
            throw new IllegalArgumentException("The circuit has no drawing to export: open it on the bench and save it, so "
                    + "its layout is written.");
        }
        Design design = new Design(slug, references, catalogue);
        design.pages = 1;
        design.place(slug, title, circuit, List.of(new Place("/" + design.rootIdentity, "", List.of(), "1")), new LinkedHashSet<>());
        String text = new KiCadSchematicExport(slug, title, circuit, catalogue, map, libraries, design).write();
        List<ExportedSheet> sheets = new ArrayList<>();
        for (Map.Entry<String, Circuit> sheet : design.circuits.entrySet()) {
            if (sheet.getKey().equals(slug)) {
                continue;
            }
            String written = new KiCadSchematicExport(sheet.getKey(), design.titles.get(sheet.getKey()), sheet.getValue(), catalogue, map,
                    libraries, design).write();
            sheets.add(new ExportedSheet(sheet.getKey() + KICAD_SCHEMATIC, written));
        }
        Map<String, String> nets = new LinkedHashMap<>();
        design.nets.forEach((net, named) -> nets.put(net, named.name()));
        return new Exported(slug + KICAD_SCHEMATIC, text, design.notes, design.symbols, nets, sheets, design.librarySymbols);
    }

    /** What a schematic's file is named with. */
    private static final String KICAD_SCHEMATIC = ".kicad_sch";

    /**
     * Whether a circuit is drawn — its layout places something besides wires —: a schematic is written only of one that is.
     * A circuit of no parts is: its drawing is an empty sheet.
     */
    public static boolean drawn(Circuit circuit) {
        return circuit.parts().isEmpty() || circuit.layout().stream().anyMatch(placement -> !(placement instanceof Placement.Wire));
    }

    /** This sheet's text; what it names and places for the board goes into the design. */
    private String write() {
        Map<String, List<Placement.PlacedPart>> placements = new LinkedHashMap<>();
        for (Placement placement : circuit.layout()) {
            switch (placement) {
                case Placement.PlacedPart placed -> placements.computeIfAbsent(placed.reference(), ignored -> new ArrayList<>())
                        .add(placed);
                case Placement.Mark mark -> mark(mark);
                case Placement.BusLine line -> busLine(line);
                case Placement.Note ignored -> {
                }
                case Placement.Drawn ignored -> {
                }
                case Placement.Wire ignored -> {
                }
            }
        }
        placements.forEach(this::part);
        circuit.layout().stream().filter(Placement.Wire.class::isInstance).map(Placement.Wire.class::cast).forEach(this::wire);
        busMembersReachKiCad();
        joinUndrawnNets();
        flagUndrivenPower();
        if (misplacedPins > 0) {
            notes.add(misplacedPins + " pins stand where KiCad's turn of the symbol puts them, a little off the sheet's "
                    + "drawing: their wires were led there.");
        }
        String fileName = slug + KICAD_SCHEMATIC;
        // Every place this sheet stands: its parts by their unfolded names, its own nets under the place's path.
        for (Place place : places()) {
            firstUnits.forEach((reference, unit) -> design.symbols.put(SheetUnfolding.partName(place.blocks(), reference),
                    new Symbol(place.boardPath() + "/" + KiCadSchematicBuilder.symbolIdentity(slug, reference, unit), values.get(reference),
                            partFields.getOrDefault(reference, Map.of()))));
            kicadNets.forEach((net, named) -> namedAt(place, net, named));
        }
        notes.forEach(note -> design.notes.add(root ? note : slug + ": " + note));
        schematic.librarySymbols().forEach(design.librarySymbols::putIfAbsent);
        return schematic.print(fileName);
    }

    /** Where this sheet stands in the design: once for the root, once for each block that places it below. */
    private List<Place> places() {
        return design.places.getOrDefault(slug, List.of());
    }

    /**
     * A net this sheet names, at one of its places, as KiCad calls it there: a label of the sheet under the place's path
     * ({@code /S1/mid}) — but a port, which the parent names —; a global label or a power symbol by its text everywhere.
     */
    private void namedAt(Place place, String net, KiCadNetName named) {
        boolean everywhere = net.equals(Circuit.GROUND) || circuit.boundary().nets().stream()
                .anyMatch(written -> written.global() && written.name().equals(net));
        if (named.strength() == NameStrength.SHEET) {
            if (!root && circuit.boundary().ports().stream().anyMatch(port -> port.net().equals(net))) {
                return;
            }
            String path = place.blocks().isEmpty() ? "" : ROOT_SHEET_PATH + String.join(ROOT_SHEET_PATH, place.blocks());
            mergeName(SheetUnfolding.netName(place.blocks(), net), new KiCadNetName(path + named.name(), named.strength()));
            return;
        }
        mergeName(everywhere ? net : SheetUnfolding.netName(place.blocks(), net), named);
    }

    private void mergeName(String net, KiCadNetName named) {
        design.nets.merge(net, named, (known, given) -> given.strength().compareTo(known.strength()) > 0 ? given : known);
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    private void part(String reference, List<Placement.PlacedPart> placed) {
        Part part = parts.get(reference);
        if (part == null) {
            notes.add(reference + " is drawn but is no part of the circuit: left out.");
            return;
        }
        Optional<DocumentNode> sheet = sheetOf(catalogue, part);
        if (sheet.isPresent()) {
            placeSheet(part, placed.getFirst(), sheet.get());
            return;
        }
        Optional<Look> found = look(part.component(), part.symbol());
        if (found.isEmpty()) {
            notes.add(reference + ": '" + part.component() + "' is no component this reader may see: left out with its wires.");
            return;
        }
        // The footprint the part sits on: the one its own line names, else its component's default.
        Optional<String> standsOn = Optional.ofNullable(part.footprint()).or(found.get()::footprint);
        Map<String, List<String>> pinPads = new LinkedHashMap<>();
        facts.pinsOfPads(part, standsOn).forEach((pad, pin) -> pinPads.computeIfAbsent(pin, ignored -> new ArrayList<>()).add(pad));
        Look look = found.get().landedOn(standsOn.orElse(null), pinPads);
        PartPins.Arrangement arrangement = PartPins.of(part, look.face(), placed);
        List<String> together = arrangement.together();
        if (together.size() > 1 && look.face().terminalPins(part).keySet().stream().anyMatch(terminal -> together.subList(1, together.size())
                .stream().anyMatch(unit -> terminal.startsWith(unit + ".")))) {
            notes.add(reference + ": units " + String.join(", ", together) + " are drawn as one on the sheet, so in KiCad their "
                    + "wires end on unit " + together.getFirst() + "'s pins.");
        }
        arrangement.placed().forEach(unit -> placeSymbol(part, look, unit));
    }

    /** The way KiCad turns a sheet's pin on each edge of its box: its text reads inwards. */
    private static final Map<Placement.PortPlace.Edge, Integer> SHEET_PIN_ANGLES = Map.of(Placement.PortPlace.Edge.LEFT, 180,
            Placement.PortPlace.Edge.RIGHT, 0, Placement.PortPlace.Edge.TOP, 90, Placement.PortPlace.Edge.BOTTOM, 270);

    /**
     * A block as KiCad's sheet box, framed as the sheet frames it ({@link BlockFrame}). A KiCad sheet pin stands on the box's
     * edge, so the block's terminals are there and the lead the sheet draws is the wire's first step.
     */
    private void placeSheet(Part part, Placement.PlacedPart item, DocumentNode sheet) {
        Circuit child = design.circuits.get(sheet.slug());
        BlockFrame frame;
        try {
            frame = BlockFrame.of(part, item, child);
        } catch (IllegalArgumentException misfit) {
            notes.add(part.reference() + ": " + misfit.getMessage() + " Left out with its wires.");
            return;
        }
        if (Math.floorMod(Math.round(item.rotation()), 360) != 0 || item.mirrored()) {
            notes.add(part.reference() + " is turned or mirrored, which a KiCad sheet cannot be: it stands upright.");
        }
        double halfWidth = frame.halfWidth();
        double halfHeight = frame.halfHeight();
        List<KiCadSchematicBuilder.SheetPin> pins = new ArrayList<>();
        for (BlockFrame.PortAt placed : frame.ports()) {
            SheetBoundary.Port port = placed.port();
            KiCadPoint at = millimetres(item.at().x() + placed.edge().x(), item.at().y() + placed.edge().y());
            if (port.direction().equals(SimulationVocabulary.BUS)) {
                // A bus pin is reached by a bus line of the parent's bus it is bound to, not by a wire.
                Optional.ofNullable(frame.wired().get(port.net())).filter(bound -> circuit.bus(bound).isPresent())
                        .ifPresent(bound -> busAnchors.add(new BusAnchor(bound, at, outwardsOf(placed.place().edge()))));
            } else {
                terminals.put(part.reference() + ":" + port.net(), at);
            }
            // A bus pin is named by the child's bus as KiCad writes it, the text its hierarchical label inside says too.
            String pinName = port.direction().equals(SimulationVocabulary.BUS)
                    ? child.bus(port.net()).map(KiCadSchematicExport::busText).orElse(port.net()) : port.net();
            pins.add(new KiCadSchematicBuilder.SheetPin(pinName, shapeOf(port.direction()), at, SHEET_PIN_ANGLES.get(placed.place().edge())));
        }
        String step = KiCadSchematicBuilder.placedSheetIdentity(slug, part.reference());
        Map<String, String> pages = new LinkedHashMap<>();
        design.places.getOrDefault(sheet.slug(), List.of()).forEach(place -> pages.put(place.path(), place.page()));
        List<KiCadSchematicBuilder.SheetInstance> instances = places().stream()
                .map(place -> new KiCadSchematicBuilder.SheetInstance(place.path(), pages.getOrDefault(place.path() + "/" + step, "")))
                .toList();
        KiCadPoint corner = millimetres(item.at().x() - halfWidth, item.at().y() - halfHeight);
        schematic.sheet(new KiCadSchematicBuilder.PlacedSheet(step, part.reference(), sheet.slug() + KICAD_SCHEMATIC, corner,
                new KiCadPoint(rounded(halfWidth * 2 * MILLIMETRES_PER_UNIT), rounded(halfHeight * 2 * MILLIMETRES_PER_UNIT)), pins, instances));
    }

    /**
     * A part, or one block of it, as KiCad places a symbol: its library symbol — its own drawing, or a box —, turned by
     * quarters, each pin where KiCad's turn puts it (a wire is led there), and what it says of its nets' power.
     */
    private void placeSymbol(Part part, Look look, PartPins.PlacedUnit placed) {
        Placement.PlacedPart item = placed.item();
        String unit = placed.unit();
        Map<String, String> own = placed.own();
        Map<String, double[]> local = new LinkedHashMap<>();
        placed.local().forEach((terminal, point) -> local.put(terminal, new double[] {point.x(), point.y()}));
        String libraryId = placed.drawn() ? look.libraryId() : look.libraryId() + (look.drawing() != null ? "_box" : "");
        if (!schematic.hasLibrarySymbol(libraryId)) {
            schematic.librarySymbol(libraryId, placed.drawn() ? drawnSymbol(look) : boxSymbol(look, libraryId, own, local));
        }
        double rotation = Math.floorMod(Math.round(item.rotation() / 90.0) * 90, 360);
        if (Math.abs(rotation - Math.floorMod((long) item.rotation(), 360)) > 1e-9) {
            notes.add(part.reference() + " is turned " + item.rotation() + "°: KiCad turns a symbol by quarters, so it stands at "
                    + rotation + "°.");
        }
        KiCadPlacement placement = new KiCadPlacement(millimetres(item.at().x(), item.at().y()),
                item.mirrored() ? (int) rotation : (int) ((360 - rotation) % 360),
                item.mirrored() ? KiCadPlacement.Mirror.Y : KiCadPlacement.Mirror.NONE);
        local.forEach((terminal, point) -> {
            KiCadPoint onSheet = placement.place(library(point));
            KiCadPoint drawnAt = millimetres(SheetGeometry.onSheet(item, new Point(point[0], point[1])));
            if (!onSheet.sameAs(drawnAt)) {
                misplacedPins++;
            }
            terminals.put(part.reference() + ":" + terminal, onSheet);
        });
        // Which of its pins take power and which give it, on which net: what KiCad's checks ask a power net to have.
        Map<String, String> nets = look.face().wiring(part);
        own.forEach((terminal, pin) -> {
            Pin modelled = look.pins().get(bare(pin));
            String net = nets.get(terminal);
            KiCadPoint at = terminals.get(part.reference() + ":" + terminal);
            if (modelled != null && net != null && at != null) {
                powered(net, typeOf(modelled), at);
            }
        });
        int kicadUnit = unit == null ? 1 : look.units().indexOf(unit) + 1;
        List<String> numbers = new ArrayList<>();
        own.values().forEach(pin -> numbers.addAll(look.numbersOf(pin)));
        firstUnits.merge(part.reference(), kicadUnit, Math::min);
        values.putIfAbsent(part.reference(), value(part, look));
        Map<String, String> carried = new LinkedHashMap<>(fields(part, look));
        carried.remove(FOOTPRINT_FIELD);
        partFields.putIfAbsent(part.reference(), carried);
        List<KiCadSchematicBuilder.Instance> instances = places().stream().map(place -> new KiCadSchematicBuilder.Instance(place.path(),
                design.references.apply(SheetUnfolding.partName(place.blocks(), part.reference())))).toList();
        schematic.symbol(new KiCadSchematicBuilder.PlacedSymbol(libraryId, placement, kicadUnit,
                instances.isEmpty() ? part.reference() : instances.getFirst().reference(),
                value(part, look), fields(part, look), numbers.stream().distinct().toList(),
                labelPoint(look, item, placement, "name", -1), labelPoint(look, item, placement, "value", 1),
                KiCadSchematicBuilder.symbolIdentity(slug, part.reference(), kicadUnit), instances,
                // The bench's equipment — a generator, a supply, a meter — is drawn and simulated, never mounted: no board, no bill.
                facts.roleOf(part.component()).ofTheBench() ? KiCadSchematicBuilder.Presence.SCHEMATIC : KiCadSchematicBuilder.Presence.BOARD));
    }

    /** Where a part's name or value is written: where its symbol puts it, else beside the part. */
    private KiCadPoint labelPoint(Look look, Placement.PlacedPart item, KiCadPlacement placement, String subject, int side) {
        if (look.drawing() != null) {
            for (SymbolDrawing.Label label : look.drawing().labels()) {
                if (label.subject().equals(subject)) {
                    return placement.place(library(new double[] {label.x(), label.y()}));
                }
            }
        }
        return millimetres(item.at().x(), item.at().y() + side * 2 * SheetGeometry.GRID);
    }

    /** The library copy of a part drawn with its own symbol: its body in unit 0, each pin in its unit. */
    private SExpressionList drawnSymbol(Look look) {
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
        for (SymbolDrawing.Pin drawnPin : look.drawing().pins()) {
            String pinName = look.ports().getOrDefault(drawnPin.name(), drawnPin.name());
            Pin pin = look.pins().get(pinName);
            if (pin == null) {
                continue;
            }
            KiCadPoint at = library(new double[] {drawnPin.x(), drawnPin.y()});
            int angle = inward(drawnPin.x(), -drawnPin.y());
            if (look.shared().contains(pinName) || look.units().isEmpty()) {
                if (placed.add(pinName)) {
                    // A pin every unit shares is the first unit's, as the sheet draws it on the part's own line: in
                    // every unit (KiCad's unit 0) each unit placed apart carried an unwired copy, and KiCad reads the
                    // pad from that one.
                    for (String number : look.numbersOf(pinName)) {
                        symbol.pin(1, typeOf(pin), at, angle, 0, drawnPin.label(), number);
                    }
                }
                continue;
            }
            for (int index = 0; index < look.units().size(); index++) {
                String unitPin = look.units().get(index) + "." + pinName;
                for (String number : look.numbersOf(unitPin)) {
                    symbol.pin(index + 1, typeOf(pin), at, angle, 0, drawnPin.label(), number);
                }
            }
        }
        return symbol.build();
    }

    /** A part without a drawing of its own, as the sheet draws it: a box, its pins on their sides, named inside. */
    private SExpressionList boxSymbol(Look look, String libraryId, Map<String, String> own, Map<String, double[]> box) {
        KiCadSymbolBuilder symbol = KiCadSymbolBuilder.named(libraryId)
                .property("Reference", look.designator()).property("Value", look.title()).property("Footprint", "")
                .property("Datasheet", "~");
        double halfWidth = 0;
        double halfHeight = 0;
        for (double[] point : box.values()) {
            halfWidth = Math.max(halfWidth, Math.abs(point[0]) - (Math.abs(point[0]) >= Math.abs(point[1]) ? SheetGeometry.BOX_LEAD : 0));
            halfHeight = Math.max(halfHeight, Math.abs(point[1]) - (Math.abs(point[1]) > Math.abs(point[0]) ? SheetGeometry.BOX_LEAD : 0));
        }
        halfWidth = Math.max(halfWidth, SheetGeometry.GRID);
        halfHeight = Math.max(halfHeight, SheetGeometry.GRID);
        symbol.rectangle(0, library(new double[] {-halfWidth, -halfHeight}), library(new double[] {halfWidth, halfHeight}));
        for (Map.Entry<String, double[]> port : box.entrySet()) {
            double[] point = port.getValue();
            boolean across = Math.abs(point[0]) >= Math.abs(point[1]);
            double[] edge = across ? new double[] {Math.signum(point[0]) * halfWidth, point[1]}
                    : new double[] {point[0], Math.signum(point[1]) * halfHeight};
            symbol.polyline(0, List.of(library(point), library(edge)));
            String pinName = own.get(port.getKey());
            Pin pin = look.pins().get(bare(pinName));
            String letter = unitOf(pinName);
            int unit = letter == null ? (look.units().isEmpty() ? 1 : 0) : look.units().indexOf(letter) + 1;
            for (String number : look.numbersOf(pinName)) {
                symbol.pin(unit, pin == null ? KiCadPinType.PASSIVE : typeOf(pin), library(point),
                        inward(point[0], -point[1]), 0, bare(pinName), number);
            }
        }
        return symbol.build();
    }

    /** A drawing's body: its paths and every element a symbol shows at rest — not a layer drawn while a part fails. */
    private void drawBody(KiCadSymbolBuilder symbol, SymbolDrawing drawing) {
        drawing.paths().forEach(path -> outline(symbol, path));
        drawing.elements().forEach(element -> drawElement(symbol, element));
    }

    private void drawElement(KiCadSymbolBuilder symbol, SymbolDrawing.Element element) {
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
                boolean gauge = element.properties().containsKey(GAUGE_LEVEL);
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

    private void outline(KiCadSymbolBuilder symbol, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        for (List<double[]> line : PathPolylines.of(paths.parse(path), CURVE_STEPS)) {
            symbol.polyline(0, line.stream().map(KiCadSchematicExport::library).toList());
        }
    }

    // ─── marks ──────────────────────────────────────────────────────────────────────────────────────

    private void mark(Placement.Mark item) {
        // A pin left open on purpose is said to our own checks; it joins nothing, so nothing of it is drawn here.
        if (item.symbol().equals(SimulationVocabulary.NO_CONNECT)) {
            return;
        }
        if (item.symbol().equals(SimulationVocabulary.JUNCTION)) {
            KiCadPoint at = millimetres(item.at().x(), item.at().y());
            terminals.put(item.reference() + ":", at);
            schematic.junction(at);
            return;
        }
        Point pin = markPin(item.symbol());
        KiCadPoint at = millimetres(SheetGeometry.onSheet(item.at(), item.rotation(), false, pin));
        terminals.put(item.reference() + ":", at);
        join(item.reference() + ":", NET + (item.net() == null ? Circuit.GROUND : item.net()));
        // The way its body goes from the point it connects at: towards the mark's own origin.
        Point away = SheetGeometry.turned(new Point(-pin.x(), -pin.y()), item.rotation(), false);
        double[] body = {away.x(), away.y()};
        String ours = item.net() == null ? Circuit.GROUND : item.net();
        switch (item.symbol()) {
            case "ground", "common" -> {
                powerSymbol(GROUND_SHAPE, at, body, net(item));
                powered(ours, KiCadPinType.POWER_INPUT, at);
                named(ours, net(item), NameStrength.POWER);
            }
            // A supply rail is KiCad's power symbol of the rail's name: the net by its value, everywhere — as a ground is.
            case "power" -> {
                powerSymbol(RAIL_SHAPE, at, body, net(item));
                powered(ours, KiCadPinType.POWER_INPUT, at);
                named(ours, net(item), NameStrength.POWER);
            }
            case "label" -> {
                schematic.label(KiCadLabel.Kind.LOCAL, net(item), at, labelAngle(body), null);
                named(ours, ROOT_SHEET_PATH + net(item), NameStrength.SHEET);
            }
            case "global" -> {
                schematic.label(KiCadLabel.Kind.GLOBAL, net(item), at, labelAngle(body), "passive");
                named(ours, net(item), NameStrength.GLOBAL);
            }
            case "port" -> {
                // A port naming a bus hands the whole bus across: KiCad's hierarchical label of the bus's text.
                Optional<Bus> bus = circuit.bus(net(item));
                if (bus.isPresent()) {
                    schematic.label(KiCadLabel.Kind.HIERARCHICAL, busText(bus.get()), at, labelAngle(body), "bidirectional");
                    busAnchors.add(new BusAnchor(bus.get().name(), at, new double[] {-body[0], -body[1]}));
                } else {
                    schematic.label(KiCadLabel.Kind.HIERARCHICAL, net(item), at, labelAngle(body), portShape(net(item)));
                    named(ours, ROOT_SHEET_PATH + net(item), NameStrength.SHEET);
                }
            }
            case "entry" -> {
                // The stroke from the bus — where the mark stands — to the wire's end, and the member's label there, which joins it.
                schematic.busEntry(millimetres(item.at().x(), item.at().y()), at);
                schematic.label(KiCadLabel.Kind.LOCAL, net(item), at, labelAngle(body), null);
                named(ours, ROOT_SHEET_PATH + net(item), NameStrength.SHEET);
            }
            default -> notes.add(item.reference() + " is a '" + item.symbol() + "' mark, which has no KiCad counterpart.");
        }
    }


    /** A net of this sheet that takes power: where the first pin taking it stands, and whether a pin gives it. */
    private record PowerNet(KiCadPoint input, boolean driven) {
    }

    /** A pin of a net that takes or gives power, as KiCad types it; any other pin says nothing of the net's power. */
    private void powered(String net, KiCadPinType type, KiCadPoint at) {
        if (type == KiCadPinType.POWER_INPUT) {
            powerNets.merge(net, new PowerNet(at, false), (was, now) -> was);
        } else if (type == KiCadPinType.POWER_OUTPUT) {
            powerNets.merge(net, new PowerNet(at, true), (was, now) -> new PowerNet(was.input(), true));
        }
    }

    /**
     * KiCad's power flag on every net of this sheet that takes power and has no pin giving it — a regulator's input fed
     * through a bridge's diodes, a ground that comes in on a connector —, as a designer marks one: KiCad's checks hold such a
     * net unpowered, while on the bench and on the board it is fed. The flag stands on the first pin taking the power.
     */
    private void flagUndrivenPower() {
        powerNets.values().stream().filter(net -> !net.driven()).forEach(net -> {
            if (!schematic.hasLibrarySymbol(POWER_FLAG_SYMBOL)) {
                schematic.librarySymbol(POWER_FLAG_SYMBOL, KiCadSymbolBuilder.named(POWER_FLAG_SYMBOL).power()
                        .property("Reference", "#FLG").property("Value", POWER_FLAG).property("Footprint", "")
                        .property("Datasheet", "").hidePinNumbers()
                        .polyline(0, List.of(new KiCadPoint(0, 0), new KiCadPoint(0, 1.27), new KiCadPoint(-1.016, 1.905),
                                new KiCadPoint(0, 2.54), new KiCadPoint(1.016, 1.905), new KiCadPoint(0, 1.27)))
                        .pin(1, KiCadPinType.POWER_OUTPUT, new KiCadPoint(0, 0), 90, 0, "pwr", "1").build());
            }
            KiCadPlacement placement = new KiCadPlacement(net.input(), 0, KiCadPlacement.Mirror.NONE);
            powerFlags++;
            schematic.symbol(new KiCadSchematicBuilder.PlacedSymbol(POWER_FLAG_SYMBOL, placement, 1,
                    String.format(Locale.ROOT, "#FLG%02d", powerFlags), POWER_FLAG, Map.of(), List.of("1"),
                    placement.place(new KiCadPoint(0, -3.81)), placement.place(new KiCadPoint(0, -5.08)), null, List.of(),
                    KiCadSchematicBuilder.Presence.SCHEMATIC));
        });
    }
    /** A ground: KiCad's power symbol, its value the net — GND for node 0. */
    /** A power symbol of a shape placed where a mark stands, its body turned the way the mark's goes, its value the net it names. */
    private void powerSymbol(PowerShape shape, KiCadPoint at, double[] body, String value) {
        if (!schematic.hasLibrarySymbol(shape.name())) {
            schematic.librarySymbol(shape.name(), KiCadSymbolBuilder.named(shape.name()).power()
                    .property("Reference", "#PWR").property("Value", value).property("Footprint", "")
                    .property("Datasheet", "").hidePinNumbers()
                    .polyline(0, shape.strokes())
                    .pin(1, KiCadPinType.POWER_INPUT, new KiCadPoint(0, 0), shape.body().y() < 0 ? 270 : 90, 0, value, "1").build());
        }
        // The body goes from its pin unturned as the shape says; of the four turns, the one that sends it the way the sheet's does.
        int best = 0;
        double bestDot = -Double.MAX_VALUE;
        for (int angle = 0; angle < 360; angle += 90) {
            KiCadPlacement turn = new KiCadPlacement(new KiCadPoint(0, 0), angle, KiCadPlacement.Mirror.NONE);
            KiCadPoint goes = turn.place(shape.body());
            double dot = goes.x() * body[0] + goes.y() * body[1];
            if (dot > bestDot) {
                bestDot = dot;
                best = angle;
            }
        }
        KiCadPlacement placement = new KiCadPlacement(at, best, KiCadPlacement.Mirror.NONE);
        powerSymbols++;
        schematic.symbol(new KiCadSchematicBuilder.PlacedSymbol(shape.name(), placement, 1,
                String.format(Locale.ROOT, "#PWR%02d", powerSymbols), value, Map.of(), List.of("1"),
                placement.place(new KiCadPoint(0, -6.35)), placement.place(shape.valueAt()), null, List.of(),
                KiCadSchematicBuilder.Presence.SCHEMATIC));
    }

    /** The point a mark's symbol connects at, in its own drawing. */
    private Point markPin(String mark) {
        return Optional.ofNullable(libraries.markSymbols().get(mark)).flatMap(faces::symbol)
                .filter(drawing -> !drawing.pins().isEmpty())
                .map(drawing -> new Point(drawing.pins().getFirst().x(), drawing.pins().getFirst().y()))
                .orElse(new Point(0, 0));
    }

    private static String net(Placement.Mark item) {
        return item.net() == null ? NODE_ZERO : item.net();
    }

    /** A block's ports and the parent's nets they are; none when the block does not fit its sheet. */
    private Optional<Map<String, String>> blockPorts(Part block, DocumentNode sheet) {
        try {
            return Optional.of(SheetUnfolding.portsOf(block, design.circuits.getOrDefault(sheet.slug(), SimulationReading.circuit(sheet))));
        } catch (IllegalArgumentException misfit) {
            return Optional.empty();
        }
    }

    private String portShape(String net) {
        return shapeOf(circuit.boundary().ports().stream().filter(port -> port.net().equals(net))
                .map(SheetBoundary.Port::direction).findFirst().orElse(SimulationVocabulary.ANALOG));
    }

    /** What KiCad calls a port's way across a sheet's edge. */
    private static String shapeOf(String direction) {
        return switch (direction) {
            case SimulationVocabulary.INPUT -> "input";
            case SimulationVocabulary.OUTPUT -> "output";
            default -> "passive";
        };
    }

    /** A label's angle: its text runs from the point it connects at the way the mark's body goes. */
    private static int labelAngle(double[] body) {
        double degrees = Math.toDegrees(Math.atan2(-body[1], body[0]));
        return (int) Math.floorMod(Math.round(degrees / 90) * 90, 360);
    }

    // ─── buses ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * A bus line as KiCad draws one: its stretches, and a label of its bus's text on its first corner — KiCad names a bus by a label
     * on it, and joins a bus pin to it by that name, member by member.
     */
    private void busLine(Placement.BusLine line) {
        List<KiCadPoint> points = line.via().stream().map(corner -> millimetres(corner.x(), corner.y())).toList();
        if (points.size() < 2) {
            return;
        }
        schematic.bus(points);
        circuit.bus(line.bus()).ifPresentOrElse(
                bus -> schematic.label(KiCadLabel.Kind.LOCAL, busText(bus), points.getFirst(), 0, null),
                () -> notes.add(line.reference() + " draws the bus " + line.bus() + ", which the sheet does not declare: it is drawn unnamed."));
    }

    /** A bus as KiCad writes it: a vector {@code D_[0..7]} — its nets our indexed names {@code D_0} … —, a group {@code {SDA SCL}}. */
    private static String busText(Bus bus) {
        return bus.numbers().map(index -> KiCadBus.vectorText(bus.name() + "_", index.first(), index.last()))
                .orElseGet(() -> KiCadBus.groupText(bus.members()));
    }

    /**
     * Each bus crossing this sheet's edge reaching KiCad member by member: KiCad joins a member across the edge only where a bus
     * entry takes its net off a line of the bus, which our language does by the member's name alone. So every member no entry of
     * the sheet takes off gets one on the bus's line at its crossing — the line drawn from there, or made — and a label of its
     * name, joined to the member's net so the net is labelled the same where it is drawn.
     */
    private void busMembersReachKiCad() {
        Set<String> takenOff = circuit.layout().stream().filter(Placement.Mark.class::isInstance).map(Placement.Mark.class::cast)
                .filter(mark -> mark.symbol().equals(SimulationVocabulary.ENTRY) && mark.net() != null).map(Placement.Mark::net)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> reached = new java.util.HashSet<>();
        for (BusAnchor anchor : busAnchors) {
            Optional<Bus> bus = circuit.bus(anchor.bus());
            if (bus.isEmpty() || !reached.add(anchor.bus())) {
                continue;
            }
            List<String> members = bus.get().members().stream().filter(member -> !takenOff.contains(member)).toList();
            if (members.isEmpty()) {
                continue;
            }
            List<KiCadPoint> path = new ArrayList<>(lineFrom(anchor));
            double needed = (members.size() + 1) * KICAD_STEP;
            double length = lengthOf(path);
            if (length < needed) {
                // A line too short, or none, goes on the way it went — into the sheet from the crossing — as far as the entries need.
                double[] way = path.size() < 2 ? anchor.inwards() : directionOf(path.get(path.size() - 2), path.getLast());
                KiCadPoint from = path.getLast();
                KiCadPoint to = new KiCadPoint(rounded(from.x() + way[0] * (needed - length)), rounded(from.y() + way[1] * (needed - length)));
                schematic.bus(List.of(from, to));
                path.add(to);
            }
            for (int index = 0; index < members.size(); index++) {
                double along = (index + 1) * KICAD_STEP;
                KiCadPoint onBus = pointAlong(path, along);
                double[] way = directionAlong(path, along);
                double[] across = {-way[1], way[0]};
                KiCadPoint end = new KiCadPoint(rounded(onBus.x() + (across[0] + way[0]) * KICAD_STEP), rounded(onBus.y() + (across[1] + way[1]) * KICAD_STEP));
                String member = members.get(index);
                // As KiCad draws one: the entry, a short wire on from its end, and the member's label on the wire.
                KiCadPoint labelled = new KiCadPoint(rounded(end.x() + across[0] * KICAD_STEP), rounded(end.y() + across[1] * KICAD_STEP));
                schematic.busEntry(onBus, end);
                schematic.wire(List.of(end, labelled));
                schematic.label(KiCadLabel.Kind.LOCAL, member, labelled, labelAngle(across), null);
                String key = "bus-entry:" + anchor.bus() + ":" + member;
                terminals.put(key, labelled);
                join(key, NET + member);
                named(member, ROOT_SHEET_PATH + member, NameStrength.SHEET);
            }
        }
    }

    /** The line of an anchor's bus that starts or ends where the bus crosses the edge, from there; just the crossing when none does. */
    private List<KiCadPoint> lineFrom(BusAnchor anchor) {
        for (Placement placement : circuit.layout()) {
            if (placement instanceof Placement.BusLine line && line.bus().equals(anchor.bus()) && line.via().size() > 1) {
                List<KiCadPoint> points = line.via().stream().map(KiCadSchematicExport::millimetres).toList();
                if (points.getFirst().sameAs(anchor.at())) {
                    return points;
                }
                if (points.getLast().sameAs(anchor.at())) {
                    return points.reversed();
                }
            }
        }
        return List.of(anchor.at());
    }

    private static double lengthOf(List<KiCadPoint> path) {
        double length = 0;
        for (int index = 1; index < path.size(); index++) {
            length += Math.hypot(path.get(index).x() - path.get(index - 1).x(), path.get(index).y() - path.get(index - 1).y());
        }
        return length;
    }

    private static double[] directionOf(KiCadPoint from, KiCadPoint to) {
        double length = Math.hypot(to.x() - from.x(), to.y() - from.y());
        return length == 0 ? new double[] {1, 0} : new double[] {(to.x() - from.x()) / length, (to.y() - from.y()) / length};
    }

    /** The point a distance along a path; its stretch in which it stands, for its direction. */
    private static int stretchAt(List<KiCadPoint> path, double along) {
        double gone = 0;
        for (int index = 1; index < path.size(); index++) {
            double stretch = Math.hypot(path.get(index).x() - path.get(index - 1).x(), path.get(index).y() - path.get(index - 1).y());
            if (gone + stretch >= along - 1e-9) {
                return index;
            }
            gone += stretch;
        }
        return path.size() - 1;
    }

    private static KiCadPoint pointAlong(List<KiCadPoint> path, double along) {
        int index = stretchAt(path, along);
        double gone = lengthOf(path.subList(0, index));
        double[] way = directionOf(path.get(index - 1), path.get(index));
        double left = along - gone;
        return new KiCadPoint(rounded(path.get(index - 1).x() + way[0] * left), rounded(path.get(index - 1).y() + way[1] * left));
    }

    private static double[] directionAlong(List<KiCadPoint> path, double along) {
        int index = stretchAt(path, along);
        return directionOf(path.get(index - 1), path.get(index));
    }

    /** Which way into the parent's sheet a pin on a box's edge leads: out of the box. */
    private static double[] outwardsOf(Placement.PortPlace.Edge edge) {
        return switch (edge) {
            case LEFT -> new double[] {-1, 0};
            case RIGHT -> new double[] {1, 0};
            case TOP -> new double[] {0, -1};
            case BOTTOM -> new double[] {0, 1};
        };
    }

    /** A block's pins without those carrying a bus: a bus pin wires no net. */
    private Map<String, String> withoutBusPins(Map<String, String> wired, DocumentNode sheet) {
        Circuit child = design.circuits.getOrDefault(sheet.slug(), SimulationReading.circuit(sheet));
        Map<String, String> nets = new LinkedHashMap<>(wired);
        child.boundary().ports().stream().filter(port -> port.direction().equals(SimulationVocabulary.BUS)).forEach(port -> nets.remove(port.net()));
        return nets;
    }

    // ─── wires ──────────────────────────────────────────────────────────────────────────────────────

    private void wire(Placement.Wire wire) {
        Optional<KiCadPoint> from = terminal(wire.from());
        Optional<KiCadPoint> to = terminal(wire.to());
        if (from.isEmpty() || to.isEmpty()) {
            notes.add(wire.reference() + " runs " + wire.from() + " to " + wire.to() + ", an end of which is not on the KiCad "
                    + "sheet: left out.");
            return;
        }
        List<KiCadPoint> points = new ArrayList<>();
        points.add(from.get());
        wire.via().forEach(bend -> points.add(millimetres(bend.x(), bend.y())));
        points.add(to.get());
        schematic.wire(points);
        join(terminalKey(wire.from()), terminalKey(wire.to()));
    }

    /**
     * Every net the circuit has that the drawing does not join whole — pins that touched in KiCad and were imported with
     * no wire, a part's power pins no wire reaches — is joined in KiCad by a label of its name on each piece of it that
     * no mark names yet: a ground symbol for node 0, a global or hierarchical label for a net that is one, else a label
     * of this sheet. A net drawn whole that no mark names is given one such label too, so it comes back under its name.
     * The netlist is the truth; the drawing only shows it.
     */
    private void joinUndrawnNets() {
        Map<String, List<String>> nets = new LinkedHashMap<>();
        for (Part part : circuit.parts()) {
            Optional<Look> look = look(part.component(), part.symbol());
            // A block's bus pin is bound to a bus, no net: what it carries joins by the bus's entries.
            Optional<Map<String, String>> ports = sheetOf(catalogue, part).flatMap(sheet -> blockPorts(part, sheet).map(wired -> withoutBusPins(wired, sheet)));
            Map<String, String> wired = ports.isPresent() ? ports.get() : look.map(found -> found.face().wiring(part)).orElseGet(() -> {
                Map<String, String> named = new LinkedHashMap<>();
                part.connections().forEach(connection -> connection.named().ifPresent(pin -> named.put(pin, connection.net())));
                return named;
            });
            wired.forEach((pin, net) -> nets.computeIfAbsent(net, ignored -> new ArrayList<>()).add(part.reference() + ":" + pin));
        }
        int labelled = 0;
        for (Map.Entry<String, List<String>> net : nets.entrySet()) {
            Map<String, String> pieces = new LinkedHashMap<>();
            net.getValue().stream().filter(terminals::containsKey).forEach(key -> pieces.putIfAbsent(find(key), key));
            // A net drawn in one piece is whole; it keeps its name only by a label, since KiCad names a net by its labels.
            if (pieces.size() < 2) {
                boolean unnamed = !net.getKey().equals(Circuit.GROUND) && !joined.containsKey(NET + net.getKey());
                if (pieces.size() == 1 && unnamed) {
                    String terminal = pieces.values().iterator().next();
                    connector(net.getKey(), terminals.get(terminal));
                    join(terminal, NET + net.getKey());
                }
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
        if (net.equals(Circuit.GROUND)) {
            powerSymbol(GROUND_SHAPE, at, new double[] {0, 1}, NODE_ZERO);
            named(net, NODE_ZERO, NameStrength.POWER);
            return;
        }
        SheetBoundary boundary = circuit.boundary();
        if (boundary.ports().stream().anyMatch(port -> port.net().equals(net))) {
            schematic.label(KiCadLabel.Kind.HIERARCHICAL, net, at, 0, portShape(net));
            named(net, ROOT_SHEET_PATH + net, NameStrength.SHEET);
        } else if (boundary.nets().stream().anyMatch(written -> written.global() && written.name().equals(net))) {
            schematic.label(KiCadLabel.Kind.GLOBAL, net, at, 0, "passive");
            named(net, net, NameStrength.GLOBAL);
        } else {
            schematic.label(KiCadLabel.Kind.LOCAL, net, at, 0, null);
            named(net, ROOT_SHEET_PATH + net, NameStrength.SHEET);
        }
    }

    /** A name the sheet gives a net: kept when it is stronger than the one it had, as KiCad keeps the strongest. */
    private void named(String net, String kicad, NameStrength strength) {
        kicadNets.merge(net, new KiCadNetName(kicad, strength),
                (known, given) -> given.strength().compareTo(known.strength()) > 0 ? given : known);
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

    /** What a wire's end is joined as: a mark by its reference, a part's terminal by its reference and pin. */
    private String terminalKey(String written) {
        int dot = written.indexOf('.');
        String reference = dot < 0 ? written : written.substring(0, dot);
        return terminals.containsKey(reference + ":") ? reference + ":"
                : reference + ":" + (dot < 0 ? "" : written.substring(dot + 1));
    }

    /** Where a wire's end meets its terminal: {@code R1.positive}, {@code U4.B.output}, {@code GND1.ground}, {@code J1.j}. */
    private Optional<KiCadPoint> terminal(String written) {
        int dot = written.indexOf('.');
        String reference = dot < 0 ? written : written.substring(0, dot);
        String pin = dot < 0 ? "" : written.substring(dot + 1);
        KiCadPoint mark = terminals.get(reference + ":");
        return mark != null ? Optional.of(mark) : Optional.ofNullable(terminals.get(reference + ":" + pin));
    }

    // ─── components ─────────────────────────────────────────────────────────────────────────────────

    /**
     * How a component is drawn with one of its symbols: the one a part names when it is drawn with another than the
     * default ({@code symbol operational-amplifier-wide-symbol}), else its default.
     */
    private Optional<Look> look(String component, String variant) {
        return looks.computeIfAbsent(component + "|" + (variant == null ? "" : variant), key -> read(component, variant));
    }

    private Optional<Look> read(String slug, String variant) {
        return faces.look(slug, variant).map(face -> kicadLook(slug, face));
    }

    /** How KiCad holds a component the sheet draws so: its library id, its pins' numbers, its value and footprint. */
    private Look kicadLook(String slug, SymbolLook face) {
        Component written = face.written();
        SimulatedComponent compiled = face.compiled();
        String bare = slug.replaceFirst("@.*$", "");
        Optional<KiCadLibraryMap.Mapping> mapping = map.libraryIdOf(bare).flatMap(map::mappingOf);
        String libraryId = map.libraryIdOf(bare)
                .or(() -> written.head().field(MetaNode.SOURCE).flatMap(KiCadSources::symbolIdOf))
                .orElse(libraries.ownLibrary() + ":" + bare);
        Map<String, String> numbers = numbersOf(compiled);
        // A pin a number cannot say — KiCad's A, S1 or B3 — is named after it by the import, pinA: the number is read back.
        compiled.pins().stream().filter(pin -> pin.numbers().isEmpty()).forEach(pin -> ImportedPins.numberOf(pin.name())
                .ifPresent(number -> numbers.putIfAbsent(pin.name(), number)));
        // The map's own numbering wins: KiCad's diode is 1 = K where the catalogue's 1 is its anode.
        mapping.ifPresent(found -> found.pins().forEach((number, pin) -> numbers.put(pin, number)));
        Optional<String> drawnWith = face.drawnWith();
        // KiCad keeps one drawing to a library symbol: the first drawing of a component holds its library id — the one
        // the library map reads back —, a second, different one of the same component on this sheet a name of its own.
        if (drawnWith.isPresent()) {
            String holder = drawingOfLibraryId.putIfAbsent(libraryId, drawnWith.get());
            if (holder != null && !holder.equals(drawnWith.get())) {
                libraryId = libraryId + "_" + drawnWith.get();
            }
        }
        Map<String, Double> defaults = new LinkedHashMap<>();
        compiled.parameters().forEach(parameter -> defaults.put(parameter.name(), parameter.value()));
        Map<String, Map<String, String>> padsOf = new LinkedHashMap<>();
        for (Seat seat : written.seats()) {
            if (!seat.pads().isEmpty()) {
                Map<String, String> pinPads = new LinkedHashMap<>();
                seat.pads().forEach((pad, pin) -> pinPads.put(pin, pad));
                padsOf.put(seat.footprint(), Map.copyOf(pinPads));
            }
        }
        return new Look(face, libraryId, written.head().field(MetaNode.DESIGNATOR).orElse("U"), numbers,
                // The parameter its value is: the library map's, else the one the component says — meta { value: U_output }.
                mapping.flatMap(KiCadLibraryMap.Mapping::value).or(() -> written.head().field(MetaNode.VALUE)), defaults,
                written.seats().stream().findFirst().map(Seat::footprint), padsOf, Map.of());
    }

    /**
     * Each pin's package number as a part names it: a block's pin by its block, {@code B.output} — its one group a number
     * for each block, {@code output 1 7} —; a shared pin by its name.
     */
    private static Map<String, String> numbersOf(SimulatedComponent component) {
        Map<String, String> numbers = new LinkedHashMap<>();
        for (Pin pin : component.pins()) {
            if (pin.numbers().isEmpty() || pin.numbers().getFirst().isEmpty()) {
                continue;
            }
            List<String> group = pin.numbers().getFirst();
            if (component.shared(pin)) {
                numbers.putIfAbsent(pin.name(), group.getFirst());
                continue;
            }
            for (int unit = 0; unit < Math.min(group.size(), component.units().size()); unit++) {
                numbers.putIfAbsent(component.units().get(unit) + "." + pin.name(), group.get(unit));
            }
        }
        return numbers;
    }

    /** The KiCad footprint a part sits on: the one its own line names, else its component's; empty when neither does. */
    private String footprintOf(Part part, Look look) {
        return Optional.ofNullable(part.footprint()).or(look::footprint).map(footprintIds::of).orElse("");
    }

    /**
     * What KiCad shows as the part's value: its value parameter as KiCad writes it ({@code 10k}), else its name. A setting
     * written over the circuit's parameters — {@code resistance = R}, {@code 2 * R} — is computed, since KiCad holds no
     * parameter to read it by; one that cannot be computed is written as it stands, and an import notes it.
     */
    private String value(Part part, Look look) {
        // What the part says in words is its value, before any parameter: a connector's OUTPUT_PLUS, as on the pcb.
        Optional<String> words = part.valueInWords();
        if (words.isPresent()) {
            return words.get();
        }
        return look.valueParameter().flatMap(parameter -> part.setting(parameter)
                        .filter(setting -> setting.value() instanceof Formula)
                        .map(setting -> written(part.reference() + "." + parameter, (Formula) setting.value()))
                        .or(() -> Optional.ofNullable(look.defaults().get(parameter)).map(KiCadValue::written)))
                .orElse(look.title());
    }

    /**
     * The fields a placed symbol carries beside its value: its footprint, and each setting of the part but the value's —
     * a switch's {@code closed} —, under its parameter's name, which an import reads back. A text is written as it is.
     */
    private Map<String, String> fields(Part part, Look look) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FOOTPRINT_FIELD, footprintOf(part, look));
        for (Setting setting : part.settings()) {
            // The value, in words or by its parameter, is the symbol's Value: KiCad reads a field of that name in any case as it.
            if (look.valueParameter().filter(setting.name()::equals).isPresent() || setting.name().equals(Part.VALUE)
                    || setting.value() == null) {
                continue;
            }
            fields.put(setting.name(), switch (setting.value()) {
                case Formula formula -> written(part.reference() + "." + setting.name(), formula);
                case Text words -> words.text();
            });
        }
        return fields;
    }

    private String written(String where, Formula formula) {
        try {
            return KiCadValue.written(ParameterEvaluation.value(where, formula, parameterValues));
        } catch (IllegalArgumentException uncomputed) {
            return formula.written();
        }
    }

    private static KiCadPinType typeOf(Pin pin) {
        if (pin.power() || pin.ground()) {
            return pin.reads() ? KiCadPinType.POWER_INPUT : KiCadPinType.POWER_OUTPUT;
        }
        if (pin.bidirectional()) {
            return KiCadPinType.BIDIRECTIONAL;
        }
        // An output that only pulls one way shares its line, as KiCad's open collector does: transistors in parallel.
        if (pin.pullsOneWay() && pin.output()) {
            return KiCadPinType.OPEN_COLLECTOR;
        }
        if (!pin.reads()) {
            return KiCadPinType.OUTPUT;
        }
        return pin.digital() ? KiCadPinType.INPUT : KiCadPinType.PASSIVE;
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

    /** A point of a drawing (Y downwards) in a KiCad library's terms (millimetres, Y upwards). */
    private static KiCadPoint library(double[] point) {
        return new KiCadPoint(rounded(point[0] * MILLIMETRES_PER_UNIT), rounded(-point[1] * MILLIMETRES_PER_UNIT));
    }

    private static KiCadPoint millimetres(double x, double y) {
        return new KiCadPoint(rounded(x * MILLIMETRES_PER_UNIT), rounded(y * MILLIMETRES_PER_UNIT));
    }

    private static KiCadPoint millimetres(Point point) {
        return millimetres(point.x(), point.y());
    }

    /** To a hundred-thousandth of a millimetre: what is left of a binary fraction is noise, not a position. */
    private static double rounded(double value) {
        return Math.round(value * 100_000) / 100_000.0;
    }
}
