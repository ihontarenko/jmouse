package org.jmouse.kicad.schematic;

import org.jmouse.kicad.KiCadPlacement;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.KiCadDocument;
import org.jmouse.kicad.parser.KiCadPrinter;
import org.jmouse.kicad.parser.node.KiCadAtom;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.parser.node.KiCadNode;
import org.jmouse.kicad.parser.node.KiCadString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    /** A symbol placed: its library copy's id, where and how it stands, its unit, its reference and value. */
    public record PlacedSymbol(String libraryId, KiCadPlacement placement, int unit, String reference, String value,
                               Map<String, String> properties, List<String> pinNumbers, KiCadPoint referenceAt,
                               KiCadPoint valueAt) {
        public PlacedSymbol {
            properties = Map.copyOf(properties);
            pinNumbers = List.copyOf(pinNumbers);
        }
    }

    private final String project;
    private final String seed;
    private final String uuid;
    private final Map<String, KiCadList> librarySymbols = new LinkedHashMap<>();
    private final List<KiCadList> symbols = new ArrayList<>();
    private final List<KiCadList> connections = new ArrayList<>();
    private int identities;
    private double right;
    private double bottom;

    private KiCadSchematicBuilder(String project, String seed) {
        this.project = project;
        this.seed = seed;
        this.uuid = identity("sheet");
    }

    /**
     * @param project what the project is called — the name a placed symbol's {@code instances} are kept under
     * @param seed    what makes this schematic's identities its own: the same seed, the same UUIDs
     */
    public static KiCadSchematicBuilder of(String project, String seed) {
        return new KiCadSchematicBuilder(project, seed);
    }

    /** The schematic's own identity: the root of every placed symbol's instance path. */
    public String uuid() {
        return uuid;
    }

    /** Whether the schematic already keeps a copy of that library symbol. */
    public boolean hasLibrarySymbol(String libraryId) {
        return librarySymbols.containsKey(libraryId);
    }

    /** A library symbol's copy, as {@code KiCadSymbolBuilder} builds one; the first of a name is kept. */
    public KiCadSchematicBuilder librarySymbol(String libraryId, KiCadList symbol) {
        librarySymbols.putIfAbsent(libraryId, symbol);
        return this;
    }

    public KiCadSchematicBuilder symbol(PlacedSymbol placed) {
        KiCadPlacement placement = placed.placement();
        reach(placement.at());
        List<KiCadNode> items = new ArrayList<>();
        items.add(KiCadList.of("lib_id", KiCadString.of(placed.libraryId())));
        items.add(KiCadList.of("at", number(placement.at().x()), number(placement.at().y()), KiCadAtom.of(placement.angle())));
        if (placement.mirror() != KiCadPlacement.Mirror.NONE) {
            items.add(KiCadList.of("mirror", KiCadAtom.of(placement.mirror().word())));
        }
        items.add(KiCadList.of("unit", KiCadAtom.of(placed.unit())));
        items.add(KiCadList.of("in_bom", KiCadAtom.of("yes")));
        items.add(KiCadList.of("on_board", KiCadAtom.of("yes")));
        items.add(KiCadList.of("dnp", KiCadAtom.of("no")));
        String identity = identity("symbol " + placed.reference() + " " + placed.unit());
        items.add(KiCadList.of("uuid", KiCadString.of(identity)));
        items.add(property("Reference", placed.reference(), placed.referenceAt(), false));
        items.add(property("Value", placed.value(), placed.valueAt(), false));
        placed.properties().forEach((name, value) -> items.add(property(name, value, placement.at(), true)));
        placed.pinNumbers().forEach(number -> items.add(KiCadList.of("pin", KiCadString.of(number),
                KiCadList.of("uuid", KiCadString.of(identity(identity + " pin " + number))))));
        items.add(KiCadList.of("instances", KiCadList.of("project", KiCadString.of(project),
                KiCadList.of("path", KiCadString.of("/" + uuid), KiCadList.of("reference", KiCadString.of(placed.reference())),
                        KiCadList.of("unit", KiCadAtom.of(placed.unit()))))));
        symbols.add(KiCadList.of("symbol", items.toArray(KiCadNode[]::new)));
        return this;
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
            connections.add(KiCadList.of("wire", KiCadList.of("pts", point(start), point(end)),
                    KiCadList.of("stroke", KiCadList.of("width", KiCadAtom.of(0)), KiCadList.of("type", KiCadAtom.of("default"))),
                    KiCadList.of("uuid", KiCadString.of(identity("wire")))));
        }
        return this;
    }

    public KiCadSchematicBuilder junction(KiCadPoint at) {
        reach(at);
        connections.add(KiCadList.of("junction", KiCadList.of("at", number(at.x()), number(at.y())),
                KiCadList.of("diameter", KiCadAtom.of(0)), KiCadList.of("color", KiCadAtom.of(0), KiCadAtom.of(0),
                        KiCadAtom.of(0), KiCadAtom.of(0)), KiCadList.of("uuid", KiCadString.of(identity("junction")))));
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
        List<KiCadNode> items = new ArrayList<>();
        items.add(KiCadString.of(text));
        if (kind != KiCadLabel.Kind.LOCAL) {
            items.add(KiCadList.of("shape", KiCadAtom.of(shape == null ? "passive" : shape)));
        }
        items.add(KiCadList.of("at", number(at.x()), number(at.y()), KiCadAtom.of(angle)));
        items.add(KiCadList.of("effects", KiCadList.of("font", KiCadList.of("size", KiCadAtom.of(1.27), KiCadAtom.of(1.27))),
                KiCadList.of("justify", KiCadAtom.of("left"), KiCadAtom.of("bottom"))));
        items.add(KiCadList.of("uuid", KiCadString.of(identity("label " + text))));
        connections.add(KiCadList.of(kind.keyword().word(), items.toArray(KiCadNode[]::new)));
        return this;
    }

    /** The schematic's tree. */
    public KiCadDocument document(String fileName) {
        Paper paper = PAPERS.stream().filter(candidate -> right + MARGIN <= candidate.width()
                && bottom + MARGIN <= candidate.height()).findFirst().orElse(PAPERS.getLast());
        List<KiCadNode> items = new ArrayList<>();
        items.add(KiCadList.of("version", KiCadAtom.of(String.valueOf(VERSION))));
        items.add(KiCadList.of("generator", KiCadAtom.of("jmouse")));
        items.add(KiCadList.of("uuid", KiCadString.of(uuid)));
        items.add(KiCadList.of("paper", KiCadString.of(paper.name())));
        items.add(KiCadList.of("lib_symbols", librarySymbols.values().toArray(KiCadNode[]::new)));
        items.addAll(connections);
        items.addAll(symbols);
        items.add(KiCadList.of("sheet_instances", KiCadList.of("path", KiCadString.of("/"),
                KiCadList.of("page", KiCadString.of("1")))));
        return KiCadDocument.of(fileName, KiCadList.of("kicad_sch", items.toArray(KiCadNode[]::new)));
    }

    /** The schematic's text, printed by {@link KiCadPrinter}. */
    public String print(String fileName) {
        return new KiCadPrinter().print(document(fileName));
    }

    private KiCadList property(String name, String value, KiCadPoint at, boolean hidden) {
        reach(at);
        return KiCadList.of("property", KiCadString.of(name), KiCadString.of(value == null ? "" : value),
                KiCadList.of("at", number(at.x()), number(at.y()), KiCadAtom.of(0)),
                hidden ? KiCadList.of("effects", font(), KiCadAtom.of("hide")) : KiCadList.of("effects", font()));
    }

    private static KiCadList font() {
        return KiCadList.of("font", KiCadList.of("size", KiCadAtom.of(1.27), KiCadAtom.of(1.27)));
    }

    private static KiCadList point(KiCadPoint point) {
        return KiCadList.of("xy", number(point.x()), number(point.y()));
    }

    private static KiCadAtom number(double value) {
        return KiCadAtom.of(value);
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
