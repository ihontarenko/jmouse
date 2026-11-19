package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.PcbWords;
import org.jmouse.cad.pcb.PcbLinesParser;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.grammar.ConnectionsNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jmouse.cad.Point;

/**
 * A pcb document's lines read into a pcb, and a line's class as a pcb editor draws it. {@link PcbDocumentWriting} writes them.
 */
public final class PcbDocumentReading {

    private PcbDocumentReading() {
    }

    /** The lines a pcb editor draws, by word: every line of a pcb. */
    private static final Set<String> DRAWN = Set.of(PcbVocabulary.CIRCUIT, "rules", "class", "pair", "sizes", "teardrops", "layers",
                                                    "edge", "place", "track", "arc", "via", "zone", "keepout", "dimension",
                                                    "text", "hole", "shape", "output");

    /**
     * A line's class, as a pcb editor draws it — its word —; empty for a statement no pcb editor draws. A group's line is drawn too: it
     * is the arrangement's grammar, shared with every drawing that keeps groups, not one of the pcb's own.
     */
    public static Optional<String> classOf(Expression statement) {
        if (ArrangementLines.isGroupLine(statement)) {
            return Optional.of(ArrangementLines.GROUP);
        }
        return statement instanceof ClauseLineNode line && line.word() != null && DRAWN.contains(line.word())
                && PcbLinesParser.grammar(line.word()) == line.grammar() ? Optional.of(line.word()) : Optional.empty();
    }

    /** The pcb a pcb document's lines make. */
    static PcbDrawing read(DocumentNode pcb) {
        String circuit = "";
        Rules rules = null;
        List<NetClass> netClasses = new ArrayList<>();
        List<Teardrop> teardrops = new ArrayList<>();
        List<Pair> pairs = new ArrayList<>();
        List<Double> trackSizes = new ArrayList<>();
        List<ViaSize> viaSizes = new ArrayList<>();
        int layers = 0;
        String outline = "";
        List<Placement> placements = new ArrayList<>();
        List<DrawnGroup> groups = new ArrayList<>();
        List<Track> tracks = new ArrayList<>();
        List<Arc> arcs = new ArrayList<>();
        List<Via> vias = new ArrayList<>();
        List<Zone> zones = new ArrayList<>();
        List<Keepout> keepouts = new ArrayList<>();
        List<Shape> shapes = new ArrayList<>();
        List<Output> outputs = new ArrayList<>();
        List<Dimension> dimensions = new ArrayList<>();
        List<Lettering> texts = new ArrayList<>();
        List<Hole> holes = new ArrayList<>();
        for (Expression statement : pcb.getExpressions()) {
            if (!(statement instanceof ClauseLineNode line)) {
                continue;
            }
            // A group is named as every item is, by its term: it has no word of its own.
            if (line.grammar() == ArrangementLines.GROUP_LINE) {
                groups.add(ArrangementLines.groupOf(line));
                continue;
            }
            switch (line.word()) {
                case PcbVocabulary.CIRCUIT -> circuit = line.name(PcbWords.SLUG).orElseThrow();
                case "rules" -> {
                    Map<String, Double> changes = new LinkedHashMap<>();
                    PcbVocabulary.RULES.stream().sorted().forEach(rule -> line.quantity(rule).ifPresent(value -> changes.put(rule, value)));
                    rules = new Rules(line.name(PcbWords.PROFILE).orElseThrow(), changes);
                }
                case "class" -> netClasses.add(new NetClass(line.name(PcbWords.NAME).orElseThrow(), line.quantity(PcbWords.WIDTH).orElseThrow(),
                        line.quantity(PcbWords.CLEARANCE).orElseThrow(), line.quantity(PcbWords.DRILL).orElse(0.0),
                        line.quantity(PcbWords.DIAMETER).orElse(0.0), line.names(PcbWords.NETS)));
                case "pair" -> {
                    List<String> nets = line.names(PcbWords.NETS);
                    pairs.add(new Pair(line.name(PcbWords.NAME).orElseThrow(), line.quantity(PcbWords.WIDTH).orElseThrow(),
                            line.quantity(PcbWords.GAP).orElseThrow(), line.quantity(PcbWords.SKEW).orElse(0.0), nets.get(0), nets.get(1)));
                }
                case "sizes" -> {
                    if (line.name(PcbWords.SIZED).map(PcbVocabulary.TRACKS::equals).orElse(false)) {
                        trackSizes.addAll(line.quantities(PcbWords.WIDTHS));
                    } else {
                        List<Double> drills = line.quantities(PcbWords.DRILL);
                        List<Double> diameters = line.quantities(PcbWords.DIAMETER);
                        for (int index = 0; index < drills.size(); index++) {
                            viaSizes.add(new ViaSize(drills.get(index), diameters.get(index)));
                        }
                    }
                }
                case "teardrops" -> teardrops.add(new Teardrop(line.name(PcbWords.TARGET).orElseThrow(), line.quantity(PcbWords.LENGTH).orElseThrow(),
                        line.quantity(PcbWords.WIDTH).orElseThrow(), line.has(PcbWords.CURVED)));
                case "layers" -> layers = (int) Math.round(quantity(line.slot(PcbWords.COUNT).orElseThrow()));
                case "edge" -> outline = path(line);
                case "place" -> placements.add(placement(line));
                case "track" -> {
                    List<Point> points = new ArrayList<>();
                    points.add(point(line.point(PcbWords.FROM).orElseThrow()));
                    line.values("to").forEach(value -> points.add(point((PointNode) value)));
                    tracks.add(new Track(line.name(PcbWords.LAYER).orElseThrow(), line.quantity(PcbWords.WIDTH).orElseThrow(),
                            line.quantity(PcbWords.ROUNDING).orElse(0.0), points, ArrangementLines.arrangementOf(line), line.word(PcbWords.FINISH).orElse(""),
                            line.quantities(PcbWords.CORNERS)));
                }
                case "arc" -> arcs.add(new Arc(line.name(PcbWords.LAYER).orElseThrow(), line.quantity(PcbWords.WIDTH).orElseThrow(),
                        point(line.point(PcbWords.FROM).orElseThrow()), point(line.point(PcbWords.THROUGH).orElseThrow()),
                        point(line.point(PcbWords.TO).orElseThrow()), ArrangementLines.arrangementOf(line), line.word(PcbWords.FINISH).orElse("")));
                case "via" -> {
                    PointNode at = line.point(PcbWords.AT).orElseThrow();
                    vias.add(new Via(at.x(), at.y(), line.quantity(PcbWords.DRILL).orElseThrow(), line.quantity(PcbWords.DIAMETER).orElseThrow(),
                            line.word(PcbWords.FROM).orElse(""), line.word(PcbWords.TO).orElse(""), line.word(PcbWords.OPEN).orElse(""), ArrangementLines.arrangementOf(line)));
                }
                case "zone" -> zones.add(new Zone(line.name(PcbWords.LAYER).orElseThrow(), line.word(PcbWords.NET).orElseThrow(),
                        line.quantity(PcbWords.CLEARANCE).orElse(0.0), (int) Math.round(line.quantity(PcbWords.PRIORITY).orElse(0.0)), path(line), line.word(PcbWords.FINISH).orElse("")));
                case "keepout" -> keepouts.add(new Keepout(line.slotNames("layers"), path(line)));
                case "dimension" -> dimensions.add(new Dimension(point(line.point(PcbWords.FROM).orElseThrow()),
                        point(line.point(PcbWords.TO).orElseThrow()), line.quantity(PcbWords.OFFSET).orElse(0.0)));
                case "text" -> {
                    PointNode at = line.point(PcbWords.AT).orElseThrow();
                    texts.add(new Lettering(line.name(PcbWords.LAYER).orElseThrow(), line.ending().orElse(""), at.x(), at.y(),
                            line.quantity(PcbWords.SIZE).orElseThrow(), line.quantity(PcbWords.ROTATE).orElse(0.0), line.word(PcbWords.ANCHOR).orElse(PcbVocabulary.TEXT_MIDDLE)));
                }
                case "output" -> outputs.add(new Output(line.name(PcbWords.NAME).orElseThrow(), line.names(PcbWords.FORMATS), line.ending().orElse("")));
                case "shape" -> shapes.add(new Shape(line.name(PcbWords.LAYER).orElseThrow(), line.quantity(PcbWords.WIDTH).orElse(0.0),
                        line.has(PcbWords.FILLED), line.word(PcbWords.NET).orElse(""), path(line)));
                case "hole" -> {
                    PointNode at = line.point(PcbWords.AT).orElseThrow();
                    holes.add(new Hole(at.x(), at.y(), line.quantity(PcbWords.DRILL).orElseThrow(), line.quantity(PcbWords.RING).orElse(0.0)));
                }
                default -> {
                }
            }
        }
        return new PcbDrawing(circuit, Optional.ofNullable(rules), netClasses, new Sizes(trackSizes, viaSizes), teardrops, pairs,
                                layers, outline, placements,
                tracks, arcs, vias, zones, keepouts, dimensions, texts, holes, shapes, outputs, groups);
    }

    private static Placement placement(ClauseLineNode line) {
        PointNode at = line.point(PcbWords.AT).orElseThrow();
        Optional<Reference> reference = line.nested("reference").map(nested -> {
            PointNode where = nested.point(PcbWords.AT).orElseThrow();
            return new Reference(where.x(), where.y(), nested.quantity(PcbWords.ROTATE).orElse(0.0), nested.word(PcbWords.LAYER).orElse(""));
        });
        return new Placement(line.name(PcbWords.REFERENCE).orElseThrow(), at.x(), at.y(), line.quantity(PcbWords.ROTATE).orElse(0.0),
                line.word(PcbWords.SIDE).orElse(PcbVocabulary.TOP), reference, line.word(PcbWords.SHOWN), line.word(PcbWords.FOOTPRINT), netsOf(line), ArrangementLines.arrangementOf(line), line.word(PcbWords.ROLE).orElse(""));
    }

    /** {@code nets (GND = 1, OUT = 2)}, by pad: a net takes several pads, a pad only one net. */
    private static Map<String, String> netsOf(ClauseLineNode line) {
        Map<String, String> nets = new LinkedHashMap<>();
        line.value("nets").map(ConnectionsNode.class::cast).ifPresent(written -> written.connections()
                .forEach(connection -> nets.put(connection.net(), connection.pin())));
        return nets;
    }

    private static Point point(PointNode point) {
        return new Point(point.x(), point.y());
    }

    private static double quantity(Expression value) {
        return ((QuantityLiteralNode) value).getQuantity().value();
    }

    private static String path(ClauseLineNode line) {
        return line.endingValue().map(value -> ((PathTextNode) value).path().toSource()).orElse("");
    }
}
