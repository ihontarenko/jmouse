package org.jmouse.kicad.schematic;

import org.jmouse.kicad.KiCadPlacement;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.sexpression.SExpressionDocument;
import org.jmouse.sexpression.SExpressionPrinter;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A schematic built as KiCad's tree and printed as a {@code .kicad_sch} KiCad 7 and newer open — what
 * {@link KiCadSchematic} reads back. The library copies of the symbols it places, the symbols, wires, junctions and
 * labels are added in sheet coordinates (millimetres, Y downwards); every identity is a UUID made from the schematic's
 * seed and what it identifies, so the same schematic is printed the same way twice.
 */
public final class KiCadSchematicBuilder {

    /** KiCad 7's format: the oldest {@link KiCadSchematic} reads and the newest every KiCad since opens. */
    public static final long VERSION = 20230121L;

    /** The sheets KiCad offers, smallest first, landscape, in millimetres. */
    private static final List<Paper> PAPERS = List.of(new Paper("A4", 297, 210), new Paper("A3", 420, 297),
            new Paper("A2", 594, 420), new Paper("A1", 841, 594), new Paper("A0", 1189, 841));

    /** Kept clear between the drawing and the sheet's edge, and its frame. */
    private static final double MARGIN = 20;

    private record Paper(String name, double width, double height) {
    }

    /**
     * A symbol placed: its library copy's id, where and how it stands, its unit, its reference and value.
     *
     * @param identity  its UUID; null for one made from the seed, its reference and its unit ({@link #symbolIdentity})
     * @param instances its reference in each instance of the sheet it is on, by the instance's path; none on a root
     *                  sheet, where it has one, under the root's own path
     * @param presence  whether it is a part of the board, or of the schematic alone — a power symbol, a power flag, the
     *                  bench's generator —, which KiCad keeps off its board and out of its bill
     */
    public record PlacedSymbol(String libraryId, KiCadPlacement placement, int unit, String reference, String value,
                               Map<String, String> properties, List<String> pinNumbers, KiCadPoint referenceAt,
                               KiCadPoint valueAt, String identity, List<Instance> instances, Presence presence) {
        public PlacedSymbol {
            properties = Map.copyOf(properties);
            pinNumbers = List.copyOf(pinNumbers);
            instances = instances == null ? List.of() : List.copyOf(instances);
        }

        /** A symbol of a root sheet, its identity made from the seed. */
        public PlacedSymbol(String libraryId, KiCadPlacement placement, int unit, String reference, String value,
                            Map<String, String> properties, List<String> pinNumbers, KiCadPoint referenceAt, KiCadPoint valueAt) {
            this(libraryId, placement, unit, reference, value, properties, pinNumbers, referenceAt, valueAt, null, List.of(), Presence.BOARD);
        }
    }

    /** Where a placed symbol exists: a part of the board too, or of the schematic alone. */
    public enum Presence {
        /** A part the board carries and the bill lists ({@code in_bom yes}, {@code on_board yes}). */
        BOARD,
        /** Of the schematic alone ({@code in_bom no}, {@code on_board no}): a power symbol, a power flag, the bench's equipment. */
        SCHEMATIC
    }

    /** One place a sheet stands in the design: its path — {@code /root-uuid/sheet-uuid} — and what it is called there. */
    public record Instance(String path, String reference) {
    }

    /**
     * A sheet placed on this one: the box of a child schematic, its pins on its left and right edges.
     *
     * @param identity  its UUID: the child's instances are the paths of this sheet's instances with it added
     * @param name      what the box is called ({@code Sheetname}): the block it stands for
     * @param file      the child's file, beside this one
     * @param at        its top-left corner
     * @param pins      each pin, where it stands on the box's edge
     * @param instances each instance of this box — one for each instance of the sheet it is on —, its path and its page
     */
    public record PlacedSheet(String identity, String name, String file, KiCadPoint at, KiCadPoint size, List<SheetPin> pins,
                              List<SheetInstance> instances) {
        public PlacedSheet {
            pins = List.copyOf(pins);
            instances = List.copyOf(instances);
        }
    }

    /** One instance of a placed sheet: its path, and the page KiCad numbers it as. */
    public record SheetInstance(String path, String page) {
    }

    /**
     * A pin of a placed sheet: the child's hierarchical label of its name, on the box's edge.
     *
     * @param shape {@code input}, {@code output}, {@code bidirectional}, {@code passive}
     * @param angle which way it points out of the box: 180 on the left edge, 0 on the right
     */
    public record SheetPin(String name, String shape, KiCadPoint at, int angle) {
    }

    private final String project;
    private final String seed;
    private final String uuid;
    private final Map<String, SExpressionList> librarySymbols = new LinkedHashMap<>();
    private final List<SExpressionList> symbols = new ArrayList<>();
    private final List<SExpressionList> connections = new ArrayList<>();
    private int identities;
    private String title;
    private final boolean root;
    private final List<SExpressionList> sheets = new ArrayList<>();
    private double right;
    private double bottom;

    private KiCadSchematicBuilder(String project, String seed, boolean root) {
        this.project = project;
        this.seed = seed;
        this.uuid = sheetIdentity(seed);
        this.root = root;
    }

    /**
     * @param project what the project is called — the name a placed symbol's {@code instances} are kept under
     * @param seed    what makes this schematic's identities its own: the same seed, the same UUIDs
     */
    public static KiCadSchematicBuilder of(String project, String seed) {
        return new KiCadSchematicBuilder(project, seed, true);
    }

    /**
     * A sheet placed below the root: its symbols' references are kept under the paths of its instances, which each
     * {@link PlacedSymbol} carries; only the root lists the design's sheets.
     */
    public static KiCadSchematicBuilder child(String project, String seed) {
        return new KiCadSchematicBuilder(project, seed, false);
    }

    /**
     * A placed symbol's identity: made from the seed, its reference and its unit alone, so a board laid out from the same
     * schematic names the symbol a footprint stands for ({@code (path "/…")}) without the schematic in hand.
     */
    public static String symbolIdentity(String seed, String reference, int unit) {
        return UUID.nameUUIDFromBytes((seed + "/symbol " + reference + " " + unit).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** A schematic's own identity, made from its seed alone: a root's is the first step of every path in the design. */
    public static String sheetIdentity(String seed) {
        return UUID.nameUUIDFromBytes((seed + "/sheet").getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** The identity of a sheet placed as {@code block} on the schematic of this seed: the step its instances add to a path. */
    public static String placedSheetIdentity(String seed, String block) {
        return UUID.nameUUIDFromBytes((seed + "/placed sheet " + block).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** What the schematic is called on its title block. */
    public KiCadSchematicBuilder title(String title) {
        this.title = title;
        return this;
    }

    /** The schematic's own identity: the root of every placed symbol's instance path. */
    public String uuid() {
        return uuid;
    }

    /** Every library symbol this schematic keeps a copy of, by its full name, in the order it was first drawn. */
    public Map<String, SExpressionList> librarySymbols() {
        return Collections.unmodifiableMap(librarySymbols);
    }

    /** Whether the schematic already keeps a copy of that library symbol. */
    public boolean hasLibrarySymbol(String libraryId) {
        return librarySymbols.containsKey(libraryId);
    }

    /** A library symbol's copy, as {@code KiCadSymbolBuilder} builds one; the first of a name is kept. */
    public KiCadSchematicBuilder librarySymbol(String libraryId, SExpressionList symbol) {
        librarySymbols.putIfAbsent(libraryId, symbol);
        return this;
    }

    public KiCadSchematicBuilder symbol(PlacedSymbol placed) {
        KiCadPlacement placement = placed.placement();
        reach(placement.at());
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionList.of("lib_id", SExpressionString.of(placed.libraryId())));
        items.add(SExpressionList.of("at", number(placement.at().x()), number(placement.at().y()), SExpressionAtom.of(placement.angle())));
        if (placement.mirror() != KiCadPlacement.Mirror.NONE) {
            items.add(SExpressionList.of("mirror", SExpressionAtom.of(placement.mirror().word())));
        }
        items.add(SExpressionList.of("unit", SExpressionAtom.of(placed.unit())));
        String onBoard = placed.presence() == Presence.BOARD ? "yes" : "no";
        items.add(SExpressionList.of("in_bom", SExpressionAtom.of(onBoard)));
        items.add(SExpressionList.of("on_board", SExpressionAtom.of(onBoard)));
        items.add(SExpressionList.of("dnp", SExpressionAtom.of("no")));
        String identity = placed.identity() != null ? placed.identity() : symbolIdentity(seed, placed.reference(), placed.unit());
        items.add(SExpressionList.of("uuid", SExpressionString.of(identity)));
        items.add(property("Reference", placed.reference(), placed.referenceAt(), false));
        items.add(property("Value", placed.value(), placed.valueAt(), false));
        placed.properties().forEach((name, value) -> items.add(property(name, value, placement.at(), true)));
        placed.pinNumbers().forEach(number -> items.add(SExpressionList.of("pin", SExpressionString.of(number),
                SExpressionList.of("uuid", SExpressionString.of(identity(identity + " pin " + number))))));
        List<Instance> instances = placed.instances().isEmpty() ? List.of(new Instance("/" + uuid, placed.reference())) : placed.instances();
        List<SExpressionNode> paths = new ArrayList<>();
        paths.add(SExpressionString.of(project));
        instances.forEach(instance -> paths.add(SExpressionList.of("path", SExpressionString.of(instance.path()),
                SExpressionList.of("reference", SExpressionString.of(instance.reference())), SExpressionList.of("unit", SExpressionAtom.of(placed.unit())))));
        items.add(SExpressionList.of("instances", SExpressionList.of("project", paths.toArray(SExpressionNode[]::new))));
        symbols.add(SExpressionList.of("symbol", items.toArray(SExpressionNode[]::new)));
        return this;
    }

    /** A sheet placed on this one, as KiCad 7 writes a hierarchical sheet. */
    public KiCadSchematicBuilder sheet(PlacedSheet placed) {
        reach(placed.at());
        reach(new KiCadPoint(placed.at().x() + placed.size().x(), placed.at().y() + placed.size().y()));
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionList.of("at", number(placed.at().x()), number(placed.at().y())));
        items.add(SExpressionList.of("size", number(placed.size().x()), number(placed.size().y())));
        items.add(SExpressionList.of("stroke", SExpressionList.of("width", SExpressionAtom.of(0)), SExpressionList.of("type", SExpressionAtom.of("solid"))));
        items.add(SExpressionList.of("fill", SExpressionList.of("color", SExpressionAtom.of(0), SExpressionAtom.of(0), SExpressionAtom.of(0),
                SExpressionAtom.of(0))));
        items.add(SExpressionList.of("uuid", SExpressionString.of(placed.identity())));
        items.add(sheetField("Sheetname", placed.name(), placed.at(), "bottom"));
        items.add(sheetField("Sheetfile", placed.file(), new KiCadPoint(placed.at().x(), placed.at().y() + placed.size().y()), "top"));
        for (SheetPin pin : placed.pins()) {
            items.add(SExpressionList.of("pin", SExpressionString.of(pin.name()), SExpressionAtom.of(pin.shape()),
                    SExpressionList.of("at", number(pin.at().x()), number(pin.at().y()), SExpressionAtom.of(pin.angle())),
                    SExpressionList.of("effects", font(), SExpressionList.of("justify", SExpressionAtom.of(pin.angle() == 180 ? "left" : "right"))),
                    SExpressionList.of("uuid", SExpressionString.of(identity(placed.identity() + " pin " + pin.name())))));
        }
        List<SExpressionNode> paths = new ArrayList<>();
        paths.add(SExpressionString.of(project));
        placed.instances().forEach(instance -> paths.add(SExpressionList.of("path", SExpressionString.of(instance.path()),
                SExpressionList.of("page", SExpressionString.of(instance.page())))));
        items.add(SExpressionList.of("instances", SExpressionList.of("project", paths.toArray(SExpressionNode[]::new))));
        sheets.add(SExpressionList.of("sheet", items.toArray(SExpressionNode[]::new)));
        return this;
    }

    private SExpressionList sheetField(String name, String value, KiCadPoint at, String across) {
        return SExpressionList.of("property", SExpressionString.of(name), SExpressionString.of(value),
                SExpressionList.of("at", number(at.x()), number(at.y()), SExpressionAtom.of(0)),
                SExpressionList.of("effects", font(), SExpressionList.of("justify", SExpressionAtom.of("left"), SExpressionAtom.of(across))));
    }

    /** A wire through the points, a segment between each two, as KiCad keeps its wires. */
    public KiCadSchematicBuilder wire(List<KiCadPoint> points) {
        for (int index = 1; index < points.size(); index++) {
            KiCadPoint start = points.get(index - 1);
            KiCadPoint end = points.get(index);
            if (start.sameAs(end)) {
                continue;
            }
            reach(start);
            reach(end);
            connections.add(SExpressionList.of("wire", SExpressionList.of("pts", point(start), point(end)),
                    SExpressionList.of("stroke", SExpressionList.of("width", SExpressionAtom.of(0)), SExpressionList.of("type", SExpressionAtom.of("default"))),
                    SExpressionList.of("uuid", SExpressionString.of(identity("wire")))));
        }
        return this;
    }

    /** A bus line through the points, a segment between each two: it joins nothing — the labels on it name its bus. */
    public KiCadSchematicBuilder bus(List<KiCadPoint> points) {
        for (int index = 1; index < points.size(); index++) {
            KiCadPoint start = points.get(index - 1);
            KiCadPoint end = points.get(index);
            if (start.sameAs(end)) {
                continue;
            }
            reach(start);
            reach(end);
            connections.add(SExpressionList.of("bus", SExpressionList.of("pts", point(start), point(end)),
                    SExpressionList.of("stroke", SExpressionList.of("width", SExpressionAtom.of(0)), SExpressionList.of("type", SExpressionAtom.of("default"))),
                    SExpressionList.of("uuid", SExpressionString.of(identity("bus")))));
        }
        return this;
    }

    /** A bus entry from a point on a bus line to where a wire takes a net off it: drawing only, as KiCad keeps one. */
    public KiCadSchematicBuilder busEntry(KiCadPoint onBus, KiCadPoint wireEnd) {
        reach(onBus);
        reach(wireEnd);
        connections.add(SExpressionList.of("bus_entry", SExpressionList.of("at", number(onBus.x()), number(onBus.y())),
                SExpressionList.of("size", number(wireEnd.x() - onBus.x()), number(wireEnd.y() - onBus.y())),
                SExpressionList.of("stroke", SExpressionList.of("width", SExpressionAtom.of(0)), SExpressionList.of("type", SExpressionAtom.of("default"))),
                SExpressionList.of("uuid", SExpressionString.of(identity("bus entry")))));
        return this;
    }

    public KiCadSchematicBuilder junction(KiCadPoint at) {
        reach(at);
        connections.add(SExpressionList.of("junction", SExpressionList.of("at", number(at.x()), number(at.y())),
                SExpressionList.of("diameter", SExpressionAtom.of(0)), SExpressionList.of("color", SExpressionAtom.of(0), SExpressionAtom.of(0),
                        SExpressionAtom.of(0), SExpressionAtom.of(0)), SExpressionList.of("uuid", SExpressionString.of(identity("junction")))));
        return this;
    }

    /**
     * A net's name where a wire ends: a label of this sheet, a global label, or a hierarchical label — the pin of the
     * box that places this sheet.
     *
     * @param shape a global or hierarchical label's: {@code input}, {@code output}, {@code bidirectional},
     *              {@code passive}; ignored for a local one
     */
    public KiCadSchematicBuilder label(KiCadLabel.Kind kind, String text, KiCadPoint at, int angle, String shape) {
        reach(at);
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionString.of(text));
        if (kind != KiCadLabel.Kind.LOCAL) {
            items.add(SExpressionList.of("shape", SExpressionAtom.of(shape == null ? "passive" : shape)));
        }
        items.add(SExpressionList.of("at", number(at.x()), number(at.y()), SExpressionAtom.of(angle)));
        items.add(SExpressionList.of("effects", SExpressionList.of("font", SExpressionList.of("size", SExpressionAtom.of(1.27), SExpressionAtom.of(1.27))),
                SExpressionList.of("justify", SExpressionAtom.of("left"), SExpressionAtom.of("bottom"))));
        items.add(SExpressionList.of("uuid", SExpressionString.of(identity("label " + text))));
        connections.add(SExpressionList.of(kind.keyword().word(), items.toArray(SExpressionNode[]::new)));
        return this;
    }

    /** The schematic's tree. */
    public SExpressionDocument document(String fileName) {
        Paper paper = PAPERS.stream().filter(candidate -> right + MARGIN <= candidate.width()
                && bottom + MARGIN <= candidate.height()).findFirst().orElse(PAPERS.getLast());
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionList.of("version", SExpressionAtom.of(String.valueOf(VERSION))));
        items.add(SExpressionList.of("generator", SExpressionAtom.of("jmouse")));
        items.add(SExpressionList.of("uuid", SExpressionString.of(uuid)));
        items.add(SExpressionList.of("paper", SExpressionString.of(paper.name())));
        if (title != null && !title.isBlank()) {
            items.add(SExpressionList.of("title_block", SExpressionList.of("title", SExpressionString.of(title))));
        }
        items.add(SExpressionList.of("lib_symbols", librarySymbols.values().toArray(SExpressionNode[]::new)));
        items.addAll(connections);
        items.addAll(symbols);
        items.addAll(sheets);
        if (root) {
            items.add(SExpressionList.of("sheet_instances", SExpressionList.of("path", SExpressionString.of("/"),
                    SExpressionList.of("page", SExpressionString.of("1")))));
        }
        return SExpressionDocument.of(fileName, SExpressionList.of("kicad_sch", items.toArray(SExpressionNode[]::new)));
    }

    /** The schematic's text, printed by {@link SExpressionPrinter}. */
    public String print(String fileName) {
        return new SExpressionPrinter().print(document(fileName));
    }

    private SExpressionList property(String name, String value, KiCadPoint at, boolean hidden) {
        reach(at);
        return SExpressionList.of("property", SExpressionString.of(name), SExpressionString.of(value == null ? "" : value),
                SExpressionList.of("at", number(at.x()), number(at.y()), SExpressionAtom.of(0)),
                hidden ? SExpressionList.of("effects", font(), SExpressionAtom.of("hide")) : SExpressionList.of("effects", font()));
    }

    private static SExpressionList font() {
        return SExpressionList.of("font", SExpressionList.of("size", SExpressionAtom.of(1.27), SExpressionAtom.of(1.27)));
    }

    private static SExpressionList point(KiCadPoint point) {
        return SExpressionList.of("xy", number(point.x()), number(point.y()));
    }

    private static SExpressionAtom number(double value) {
        return SExpressionAtom.of(value);
    }

    /** How far the drawing reaches: the paper is chosen to hold it. */
    private void reach(KiCadPoint point) {
        right = Math.max(right, point.x());
        bottom = Math.max(bottom, point.y());
    }

    /** The next identity, made from the seed, what it names and how many came before it. */
    private String identity(String what) {
        identities++;
        return UUID.nameUUIDFromBytes((seed + "/" + what + "/" + identities).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
