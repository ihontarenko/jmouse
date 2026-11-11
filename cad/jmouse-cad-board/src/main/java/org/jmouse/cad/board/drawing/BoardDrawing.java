package org.jmouse.cad.board.drawing;

import org.jmouse.cad.board.BoardLinesParser;
import org.jmouse.cad.board.BoardVocabulary;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.svg.path.parser.PathParseException;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A board as data an editor works on — the circuit it lays out, how it is made, its copper layers and edge, where each
 * part stands and the copper between them —, in millimetres, Y downwards. It is data; how it is sent is the product's.
 *
 * @param copperLayers how many copper layers it has; 0 when it does not say
 */
public record BoardDrawing(String circuit, Optional<Rules> rules, List<NetClass> netClasses, Sizes sizes, List<Teardrop> teardrops,
                           List<Pair> pairs, int copperLayers, String outline, List<Placement> placements, List<Track> tracks,
                           List<Arc> arcs, List<Via> vias, List<Zone> zones, List<Keepout> keepouts, List<Dimension> dimensions,
                           List<Text> texts, List<Hole> holes) {

    public BoardDrawing {
        netClasses = List.copyOf(netClasses);
        sizes = sizes == null ? Sizes.NONE : sizes;
        teardrops = List.copyOf(teardrops);
        pairs = List.copyOf(pairs);
        placements = List.copyOf(placements);
        tracks = List.copyOf(tracks);
        arcs = List.copyOf(arcs);
        vias = List.copyOf(vias);
        zones = List.copyOf(zones);
        keepouts = List.copyOf(keepouts);
        dimensions = List.copyOf(dimensions);
        texts = List.copyOf(texts);
        holes = List.copyOf(holes);
    }

    /** The side a part, a pad or a copper layer is on — and the side a part stands on when its line says none. */
    public static final String TOP = "top";

    public static final String BOTTOM = "bottom";

    /** How many copper layers a board has that does not say. */
    private static final int USUAL_COPPER_LAYERS = 2;

    /**
     * Its copper layers from the top down, by id — {@code top}, {@code inner-1} …, {@code bottom} —; a board of one layer
     * is copper on the bottom alone.
     */
    public List<String> copperLayerIds() {
        int count = copperLayers > 0 ? copperLayers : USUAL_COPPER_LAYERS;
        if (count == 1) {
            return List.of(BOTTOM);
        }
        List<String> layers = new ArrayList<>();
        layers.add(TOP);
        for (int inner = 1; inner <= count - 2; inner++) {
            layers.add("inner-" + inner);
        }
        layers.add(BOTTOM);
        return List.copyOf(layers);
    }

    /** Whether a via has copper on a layer: from its first layer to its last, the board's top and bottom unless it names them. */
    public boolean reaches(Via via, String layer) {
        List<String> layers = copperLayerIds();
        int from = via.from() == null || via.from().isBlank() ? 0 : layers.indexOf(via.from());
        int to = via.to() == null || via.to().isBlank() ? layers.size() - 1 : layers.indexOf(via.to());
        int depth = layers.indexOf(layer);
        return depth >= 0 && depth >= Math.min(from, to) && depth <= Math.max(from, to);
    }

    /** A point of the board. */
    public record Point(double x, double y) {
    }

    /** @param changes the values the board changes of its way of making, by rule word, in millimetres */
    public record Rules(String profile, Map<String, Double> changes) {

        public Rules {
            changes = Collections.unmodifiableMap(new LinkedHashMap<>(changes));
        }
    }

    /** @param drill 0 with {@code diameter}: the board's usual via */
    public record NetClass(String name, double width, double clearance, double drill, double diameter, List<String> nets) {

        public NetClass {
            nets = List.copyOf(nets);
        }
    }

    /** @param target {@code round}, {@code surface} or {@code tracks} */
    /**
     * The sizes a board offers new copper in, beside what its classes say: track widths, and via drills each with its
     * diameter. Empty lists: the editor's own.
     */
    public record Sizes(List<Double> tracks, List<ViaSize> vias) {

        public static final Sizes NONE = new Sizes(List.of(), List.of());

        public Sizes {
            tracks = List.copyOf(tracks);
            vias = List.copyOf(vias);
        }
    }

    /** A via's drill and the copper ring's diameter around it, millimetres. */
    public record ViaSize(double drill, double diameter) {
    }

    public record Teardrop(String target, double length, double width, boolean curved) {
    }

    /** @param skew 0: the two lengths are not matched */
    public record Pair(String name, double width, double gap, double skew, String positive, String negative) {
    }

    /**
     * @param side      {@code top} or {@code bottom}
     * @param reference where its designator stands in the part's own frame; empty for where its footprint's field puts it
     */
    public record Placement(String designator, double x, double y, double rotation, String side, Optional<Reference> reference) {

        public boolean onBottom() {
            return BOTTOM.equals(side);
        }

        /**
         * Where a point of the part's footprint lands on the board: mirrored first when the part sits on the bottom, then
         * turned with the part, counter-clockwise as the board is seen.
         */
        public Point placed(double footprintX, double footprintY) {
            double mirroredX = onBottom() ? -footprintX : footprintX;
            double radians = Math.toRadians(-rotation);
            double cosine = Math.cos(radians);
            double sine = Math.sin(radians);
            return new Point(x + mirroredX * cosine - footprintY * sine, y + mirroredX * sine + footprintY * cosine);
        }
    }

    /** Where a part's designator stands in its own frame, and how it is turned. */
    public record Reference(double x, double y, double rotation) {
    }

    public record Track(String layer, double width, double rounding, List<Point> points) {

        public Track {
            points = List.copyOf(points);
        }
    }

    /** A track bent as an arc, through three points. */
    public record Arc(String layer, double width, Point from, Point through, Point to) {
    }

    /** @param from the layer it starts on; empty for through the board */
    public record Via(double x, double y, double drill, double diameter, String from, String to) {
    }

    /** @param clearance 0 for the board's own */
    public record Zone(String layer, String net, double clearance, int priority, String path) {
    }

    public record Keepout(List<String> layers, String path) {

        public Keepout {
            layers = List.copyOf(layers);
        }
    }

    public record Dimension(Point from, Point to, double offset) {
    }

    /** @param layer {@code top-silkscreen} or {@code bottom-silkscreen} */
    public record Text(String layer, String text, double x, double y, double size, double rotation) {
    }

    /** @param ring 0: a bare hole */
    public record Hole(double x, double y, double drill, double ring) {
    }

    private static final PathParser PATHS = new PathParser();

    /** The lines a board editor draws, by word: every line of a board. */
    private static final Set<String> DRAWN = Set.of(BoardVocabulary.CIRCUIT, "rules", "class", "pair", "sizes", "teardrops", "layers",
                                                    "edge", "place", "track", "arc", "via", "zone", "keepout", "dimension",
                                                    "text", "hole");

    /** A line's class, as a board editor draws it — its word —; empty for a statement no board editor draws. */
    public static Optional<String> classOf(Expression statement) {
        return statement instanceof ClauseLineNode line && line.word() != null && DRAWN.contains(line.word())
                && BoardLinesParser.grammar(line.word()) == line.grammar() ? Optional.of(line.word()) : Optional.empty();
    }

    /**
     * The board as its document's lines, built on the grammars that read them, in the order a board is written: its
     * circuit, how it is made, its classes, pairs and teardrops, its layers and edge, its parts, then its copper, keepouts,
     * dimensions, texts and holes. A value that is its line's default — no rotation, the top side — is not written.
     *
     * @throws IllegalArgumentException when a path does not read as path data
     */
    public List<ClauseLineNode> lines() {
        List<ClauseLineNode> lines = new ArrayList<>();
        if (circuit != null && !circuit.isEmpty()) {
            lines.add(line(BoardVocabulary.CIRCUIT).slot("slug", Nodes.word(circuit)).build());
        }
        rules.ifPresent(made -> {
            LineBuilder line = line("rules").slot("profile", Nodes.word(made.profile()));
            made.changes().forEach((rule, value) -> line.clause(rule, Nodes.number(value)));
            lines.add(line.build());
        });
        for (NetClass netClass : netClasses) {
            LineBuilder line = line("class").slot("name", Nodes.word(netClass.name()))
                    .clause("width", Nodes.number(netClass.width())).clause("clearance", Nodes.number(netClass.clearance()));
            if (netClass.drill() > 0) {
                line.clause("drill", Nodes.number(netClass.drill()));
            }
            if (netClass.diameter() > 0) {
                line.clause("diameter", Nodes.number(netClass.diameter()));
            }
            lines.add(line.clause("nets", Nodes.words(netClass.nets())).build());
        }
        for (Pair pair : pairs) {
            LineBuilder line = line("pair").slot("name", Nodes.word(pair.name())).clause("width", Nodes.number(pair.width()))
                    .clause("gap", Nodes.number(pair.gap()));
            if (pair.skew() > 0) {
                line.clause("skew", Nodes.number(pair.skew()));
            }
            lines.add(line.clause("nets", Nodes.words(List.of(pair.positive(), pair.negative()))).build());
        }
        if (!sizes.tracks().isEmpty()) {
            lines.add(line("sizes").slot("sized", Nodes.word(BoardVocabulary.TRACKS)).slot("widths", Nodes.numbers(sizes.tracks())).build());
        }
        if (!sizes.vias().isEmpty()) {
            lines.add(line("sizes").slot("sized", Nodes.word(BoardVocabulary.VIAS))
                              .clause("drill", Nodes.numbers(sizes.vias().stream().map(ViaSize::drill).toList()))
                              .clause("diameter", Nodes.numbers(sizes.vias().stream().map(ViaSize::diameter).toList())).build());
        }
        for (Teardrop teardrop : teardrops) {
            LineBuilder line = line("teardrops").slot("target", Nodes.word(teardrop.target()))
                    .clause("length", Nodes.number(teardrop.length())).clause("width", Nodes.number(teardrop.width()));
            if (teardrop.curved()) {
                line.flag("curved");
            }
            lines.add(line.build());
        }
        if (copperLayers > 0) {
            lines.add(line("layers").slot("count", Nodes.number(copperLayers)).build());
        }
        if (outline != null && !outline.isEmpty()) {
            lines.add(line("edge").ending(pathText("the edge", outline)).build());
        }
        placements.forEach(placement -> lines.add(placement(placement)));
        for (Track track : tracks) {
            LineBuilder line = line("track").slot("layer", Nodes.word(track.layer())).clause("width", Nodes.number(track.width()));
            if (track.rounding() > 0) {
                line.clause("rounding", Nodes.number(track.rounding()));
            }
            line.clause("from", point(track.points().getFirst()));
            track.points().stream().skip(1).forEach(point -> line.clause("to", point(point)));
            lines.add(line.build());
        }
        for (Arc arc : arcs) {
            lines.add(line("arc").slot("layer", Nodes.word(arc.layer())).clause("width", Nodes.number(arc.width()))
                    .clause("from", point(arc.from())).clause("through", point(arc.through())).clause("to", point(arc.to()))
                    .build());
        }
        for (Via via : vias) {
            LineBuilder line = line("via").clause("at", Nodes.point(via.x(), via.y())).clause("drill", Nodes.number(via.drill()))
                    .clause("diameter", Nodes.number(via.diameter()));
            if (via.from() != null && !via.from().isEmpty()) {
                line.clause("from", Nodes.word(via.from()));
            }
            if (via.to() != null && !via.to().isEmpty()) {
                line.clause("to", Nodes.word(via.to()));
            }
            lines.add(line.build());
        }
        for (Zone zone : zones) {
            LineBuilder line = line("zone").slot("layer", Nodes.word(zone.layer())).clause("net", Nodes.word(zone.net()));
            if (zone.clearance() > 0) {
                line.clause("clearance", Nodes.number(zone.clearance()));
            }
            if (zone.priority() > 0) {
                line.clause("priority", Nodes.number(zone.priority()));
            }
            lines.add(line.ending(pathText("the zone on " + zone.layer(), zone.path())).build());
        }
        for (Keepout keepout : keepouts) {
            lines.add(line("keepout").slot("layers", Nodes.words(keepout.layers()))
                    .ending(pathText("the keepout", keepout.path())).build());
        }
        for (Dimension dimension : dimensions) {
            LineBuilder line = line("dimension").clause("from", point(dimension.from())).clause("to", point(dimension.to()));
            if (dimension.offset() != 0) {
                line.clause("offset", Nodes.number(dimension.offset()));
            }
            lines.add(line.build());
        }
        for (Text text : texts) {
            LineBuilder line = line("text").slot("layer", Nodes.word(text.layer())).clause("size", Nodes.number(text.size()))
                    .clause("at", Nodes.point(text.x(), text.y()));
            if (text.rotation() != 0) {
                line.clause("rotate", Nodes.number(text.rotation()));
            }
            lines.add(line.ending(Nodes.text(text.text())).build());
        }
        for (Hole hole : holes) {
            LineBuilder line = line("hole").clause("at", Nodes.point(hole.x(), hole.y())).clause("drill", Nodes.number(hole.drill()));
            if (hole.ring() > 0) {
                line.clause("ring", Nodes.number(hole.ring()));
            }
            lines.add(line.build());
        }
        return lines;
    }

    /** {@code place R1 at x, y [rotate r] [side bottom] [reference at x, y [rotate r]]}. */
    private static ClauseLineNode placement(Placement placement) {
        LineBuilder line = line("place").slot("reference", Nodes.word(placement.designator()))
                .clause("at", Nodes.point(placement.x(), placement.y()));
        if (placement.rotation() != 0) {
            line.clause("rotate", Nodes.number(placement.rotation()));
        }
        if (placement.side() != null && !placement.side().equals(TOP)) {
            line.clause("side", Nodes.word(placement.side()));
        }
        placement.reference().ifPresent(reference -> {
            LineBuilder nested = LineBuilder.line(BoardLinesParser.grammar("place").clause("reference").nested())
                    .clause("at", Nodes.point(reference.x(), reference.y()));
            if (reference.rotation() != 0) {
                nested.clause("rotate", Nodes.number(reference.rotation()));
            }
            line.clause("reference", nested.build().asHeader());
        });
        return line.build();
    }

    private static LineBuilder line(String word) {
        return LineBuilder.line(BoardLinesParser.grammar(word));
    }

    private static PointNode point(Point point) {
        return Nodes.point(point.x(), point.y());
    }

    private static PathTextNode pathText(String origin, String written) {
        try {
            return new PathTextNode(PATHS.parse(origin, written == null ? "" : written));
        } catch (PathParseException refused) {
            throw new IllegalArgumentException("the path data of " + origin + " does not read: " + refused.detail());
        }
    }

    /** The board a board document's lines make. */
    public static BoardDrawing of(DocumentNode board) {
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
        List<Track> tracks = new ArrayList<>();
        List<Arc> arcs = new ArrayList<>();
        List<Via> vias = new ArrayList<>();
        List<Zone> zones = new ArrayList<>();
        List<Keepout> keepouts = new ArrayList<>();
        List<Dimension> dimensions = new ArrayList<>();
        List<Text> texts = new ArrayList<>();
        List<Hole> holes = new ArrayList<>();
        for (Expression statement : board.getExpressions()) {
            if (!(statement instanceof ClauseLineNode line)) {
                continue;
            }
            switch (line.word()) {
                case BoardVocabulary.CIRCUIT -> circuit = line.name("slug").orElseThrow();
                case "rules" -> {
                    Map<String, Double> changes = new LinkedHashMap<>();
                    BoardVocabulary.RULES.stream().sorted().forEach(rule -> line.quantity(rule).ifPresent(value -> changes.put(rule, value)));
                    rules = new Rules(line.name("profile").orElseThrow(), changes);
                }
                case "class" -> netClasses.add(new NetClass(line.name("name").orElseThrow(), line.quantity("width").orElseThrow(),
                        line.quantity("clearance").orElseThrow(), line.quantity("drill").orElse(0.0),
                        line.quantity("diameter").orElse(0.0), line.names("nets")));
                case "pair" -> {
                    List<String> nets = line.names("nets");
                    pairs.add(new Pair(line.name("name").orElseThrow(), line.quantity("width").orElseThrow(),
                            line.quantity("gap").orElseThrow(), line.quantity("skew").orElse(0.0), nets.get(0), nets.get(1)));
                }
                case "sizes" -> {
                    if (line.name("sized").map(BoardVocabulary.TRACKS::equals).orElse(false)) {
                        trackSizes.addAll(line.quantities("widths"));
                    } else {
                        List<Double> drills = line.quantities("drill");
                        List<Double> diameters = line.quantities("diameter");
                        for (int index = 0; index < drills.size(); index++) {
                            viaSizes.add(new ViaSize(drills.get(index), diameters.get(index)));
                        }
                    }
                }
                case "teardrops" -> teardrops.add(new Teardrop(line.name("target").orElseThrow(), line.quantity("length").orElseThrow(),
                        line.quantity("width").orElseThrow(), line.has("curved")));
                case "layers" -> layers = (int) Math.round(quantity(line.slot("count").orElseThrow()));
                case "edge" -> outline = path(line);
                case "place" -> placements.add(placement(line));
                case "track" -> {
                    List<Point> points = new ArrayList<>();
                    points.add(point(line.point("from").orElseThrow()));
                    line.values("to").forEach(value -> points.add(point((PointNode) value)));
                    tracks.add(new Track(line.name("layer").orElseThrow(), line.quantity("width").orElseThrow(),
                            line.quantity("rounding").orElse(0.0), points));
                }
                case "arc" -> arcs.add(new Arc(line.name("layer").orElseThrow(), line.quantity("width").orElseThrow(),
                        point(line.point("from").orElseThrow()), point(line.point("through").orElseThrow()),
                        point(line.point("to").orElseThrow())));
                case "via" -> {
                    PointNode at = line.point("at").orElseThrow();
                    vias.add(new Via(at.x(), at.y(), line.quantity("drill").orElseThrow(), line.quantity("diameter").orElseThrow(),
                            line.word("from").orElse(""), line.word("to").orElse("")));
                }
                case "zone" -> zones.add(new Zone(line.name("layer").orElseThrow(), line.word("net").orElseThrow(),
                        line.quantity("clearance").orElse(0.0), (int) Math.round(line.quantity("priority").orElse(0.0)), path(line)));
                case "keepout" -> keepouts.add(new Keepout(line.slotNames("layers"), path(line)));
                case "dimension" -> dimensions.add(new Dimension(point(line.point("from").orElseThrow()),
                        point(line.point("to").orElseThrow()), line.quantity("offset").orElse(0.0)));
                case "text" -> {
                    PointNode at = line.point("at").orElseThrow();
                    texts.add(new Text(line.name("layer").orElseThrow(), line.ending().orElse(""), at.x(), at.y(),
                            line.quantity("size").orElseThrow(), line.quantity("rotate").orElse(0.0)));
                }
                case "hole" -> {
                    PointNode at = line.point("at").orElseThrow();
                    holes.add(new Hole(at.x(), at.y(), line.quantity("drill").orElseThrow(), line.quantity("ring").orElse(0.0)));
                }
                default -> {
                }
            }
        }
        return new BoardDrawing(circuit, Optional.ofNullable(rules), netClasses, new Sizes(trackSizes, viaSizes), teardrops, pairs,
                                layers, outline, placements,
                tracks, arcs, vias, zones, keepouts, dimensions, texts, holes);
    }

    private static Placement placement(ClauseLineNode line) {
        PointNode at = line.point("at").orElseThrow();
        Optional<Reference> reference = line.nested("reference").map(nested -> {
            PointNode where = nested.point("at").orElseThrow();
            return new Reference(where.x(), where.y(), nested.quantity("rotate").orElse(0.0));
        });
        return new Placement(line.name("reference").orElseThrow(), at.x(), at.y(), line.quantity("rotate").orElse(0.0),
                line.word("side").orElse(TOP), reference);
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
