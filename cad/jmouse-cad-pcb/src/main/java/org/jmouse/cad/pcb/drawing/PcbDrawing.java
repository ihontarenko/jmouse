package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.PcbWords;
import org.jmouse.cad.pcb.PcbLinesParser;
import org.jmouse.cad.pcb.PcbVocabulary;
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

import org.jmouse.el.grammar.ConnectionsNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A pcb as data an editor works on — the circuit it lays out, how it is made, its copper layers and edge, where each
 * part stands and the copper between them —, in millimetres, Y downwards. It is data; how it is sent is the product's.
 *
 * @param copperLayers how many copper layers it has; 0 when it does not say
 */
public record PcbDrawing(String circuit, Optional<Rules> rules, List<NetClass> netClasses, Sizes sizes, List<Teardrop> teardrops,
                           List<Pair> pairs, int copperLayers, String outline, List<Placement> placements, List<Track> tracks,
                           List<Arc> arcs, List<Via> vias, List<Zone> zones, List<Keepout> keepouts, List<Dimension> dimensions,
                           List<Text> texts, List<Hole> holes, List<Shape> shapes, List<Output> outputs) {

    public PcbDrawing {
        netClasses = listed(netClasses);
        sizes = sizes == null ? Sizes.NONE : sizes;
        teardrops = listed(teardrops);
        pairs = listed(pairs);
        placements = listed(placements);
        tracks = listed(tracks);
        arcs = listed(arcs);
        vias = listed(vias);
        zones = listed(zones);
        keepouts = listed(keepouts);
        dimensions = listed(dimensions);
        texts = listed(texts);
        holes = listed(holes);
        shapes = listed(shapes);
        outputs = listed(outputs);
    }

    /** A list a pcb does not have is left out of what is written: absent is empty. */
    private static <T> List<T> listed(List<T> items) {
        return items == null ? List.of() : List.copyOf(items);
    }

    /** The side a part, a pad or a copper layer is on — and the side a part stands on when its line says none. */
    public static final String TOP = "top";

    public static final String BOTTOM = "bottom";
    /** Where words stand on their point. */
    public static final String START = PcbVocabulary.TEXT_START;
    public static final String MIDDLE = PcbVocabulary.TEXT_MIDDLE;
    public static final String END = PcbVocabulary.TEXT_END;
    /** The rules a pcb's line changes that its importers set from a board's usual net class. */
    public static final String RULE_CLEARANCE = PcbVocabulary.RULE_CLEARANCE;
    public static final String RULE_TRACK_WIDTH = PcbVocabulary.RULE_TRACK_WIDTH;
    public static final String RULE_VIA_DRILL = PcbVocabulary.RULE_VIA_DRILL;
    public static final String RULE_VIA_DIAMETER = PcbVocabulary.RULE_VIA_DIAMETER;
    private static final String INNER_COPPER = "inner-";
    private static final String SILKSCREEN_ENDING = "-silkscreen";
    private static final String SLUG_ENDING = "-pcb";

    /** An inner copper layer's id, counted from the top: {@code inner-1}. */
    public static String otherSide(String side) {
        return TOP.equals(side) ? BOTTOM : TOP;
    }

    public static String layerOf(String side, String purpose) {
        return side + "-" + purpose;
    }

    public static String innerCopper(int number) {
        return INNER_COPPER + number;
    }

    /** Whether a copper layer is one between the outer two: {@code inner-1}. */
    public static boolean isInnerCopper(String layer) {
        return layer.startsWith(INNER_COPPER);
    }

    /** A side's silkscreen layer: {@code top-silkscreen}. */
    public static String silkscreen(String side) {
        return side + SILKSCREEN_ENDING;
    }

    /** The slug a circuit's pcb has, beside the circuit: {@code amplifier-pcb}; a bench looks for it there. */
    public static String slugOf(String circuit) {
        return circuit + SLUG_ENDING;
    }

    /** How many copper layers a pcb has that does not say. */
    private static final int USUAL_COPPER_LAYERS = 2;

    /**
     * Its copper layers from the top down, by id — {@code top}, {@code inner-1} …, {@code bottom} —; a pcb of one layer
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
            layers.add(innerCopper(inner));
        }
        layers.add(BOTTOM);
        return List.copyOf(layers);
    }

    /** The same pcb with other tracks. */
    public PcbDrawing withTracks(List<Track> others) {
        return new PcbDrawing(circuit, rules, netClasses, sizes, teardrops, pairs, copperLayers, outline, placements, others, arcs, vias,
                zones, keepouts, dimensions, texts, holes, shapes, outputs);
    }

    /** The same pcb with other vias. */
    public PcbDrawing withVias(List<Via> others) {
        return new PcbDrawing(circuit, rules, netClasses, sizes, teardrops, pairs, copperLayers, outline, placements, tracks, arcs, others,
                zones, keepouts, dimensions, texts, holes, shapes, outputs);
    }

    /** The same pcb with other placements. */
    public PcbDrawing withPlacements(List<Placement> others) {
        return new PcbDrawing(circuit, rules, netClasses, sizes, teardrops, pairs, copperLayers, outline, others, tracks, arcs, vias,
                zones, keepouts, dimensions, texts, holes, shapes, outputs);
    }

    /** The same pcb with one track run through other points. */
    public PcbDrawing withTrackPoints(int index, List<Point> points) {
        List<Track> changed = new ArrayList<>(tracks);
        changed.set(index, tracks.get(index).withPoints(points));
        return withTracks(changed);
    }

    /** Whether a via has copper on a layer: from its first layer to its last, the pcb's top and bottom unless it names them. */
    public boolean reaches(Via via, String layer) {
        List<String> layers = copperLayerIds();
        int from = via.from() == null || via.from().isBlank() ? 0 : layers.indexOf(via.from());
        int to = via.to() == null || via.to().isBlank() ? layers.size() - 1 : layers.indexOf(via.to());
        int depth = layers.indexOf(layer);
        return depth >= 0 && depth >= Math.min(from, to) && depth <= Math.max(from, to);
    }

    /** A point of the pcb. */
    public record Point(double x, double y) {
    }

    /** @param changes the values the pcb changes of its way of making, by rule word, in millimetres */
    public record Rules(String profile, Map<String, Double> changes) {

        public Rules {
            changes = Collections.unmodifiableMap(new LinkedHashMap<>(changes));
        }
    }

    /** @param drill 0 with {@code diameter}: the pcb's usual via */
    public record NetClass(String name, double width, double clearance, double drill, double diameter, List<String> nets) {

        public NetClass {
            nets = List.copyOf(nets);
        }
    }

    /** @param target {@code round}, {@code surface} or {@code tracks} */
    /**
     * The sizes a pcb offers new copper in, beside what its classes say: track widths, and via drills each with its
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
     * @param group     the group it is chosen and moved with; empty for none
     * @param role      what it is to the copper beyond its pads: {@code link} for a 0 Ω link, its pads one piece of copper; empty for a part
     */
    public record Placement(String designator, double x, double y, double rotation, String side, Optional<Reference> reference,
                            Optional<String> shown, Optional<String> footprint, Map<String, String> nets, String group, String role) {

        public Placement {
            nets = nets == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(nets));
            group = group == null ? "" : group;
            role = role == null ? "" : role;
        }

        /** A placement in no group. */
        public Placement(String designator, double x, double y, double rotation, String side, Optional<Reference> reference,
                         Optional<String> shown, Optional<String> footprint, Map<String, String> nets) {
            this(designator, x, y, rotation, side, reference, shown, footprint, nets, "", "");
        }

        /** A part of the circuit, printed as it is named and standing on the footprint the circuit gives it. */
        public Placement(String designator, double x, double y, double rotation, String side, Optional<Reference> reference) {
            this(designator, x, y, rotation, side, reference, Optional.empty(), Optional.empty(), Map.of());
        }

        /** What is printed on the pcb and listed for a person: on an unfolded design a part is {@code S1_R1} but reads {@code R1}. */
        public String printed() {
            return shown.orElse(designator);
        }

        /** Whether it is only the pcb's — a mounting hole, a logo — and no part of the circuit: it names its own footprint. */
        public boolean ownedByPcb() {
            return footprint.isPresent();
        }

        public boolean onBottom() {
            return BOTTOM.equals(side);
        }

        /**
         * Where a point of the part's footprint lands on the pcb: mirrored first when the part sits on the bottom, then
         * turned with the part, counter-clockwise as the pcb is seen.
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
    public record Reference(double x, double y, double rotation, String layer) {

        /** A designator standing where its footprint's field says it is printed. */
        public Reference(double x, double y, double rotation) {
            this(x, y, rotation, "");
        }
    }

    /**
     * @param group  the group it is chosen and moved with; empty for none
     * @param finish what its copper is left as: {@link PcbVocabulary#FINISH_BARE}, the solder mask open along it for solder to be run on;
     *               empty for under the mask
     */
    public record Track(String layer, double width, double rounding, List<Point> points, String group, String finish) {

        public Track {
            points = List.copyOf(points);
            group = group == null ? "" : group;
            finish = finish == null ? "" : finish;
        }

        /** A track in no group, under the mask. */
        public Track(String layer, double width, double rounding, List<Point> points) {
            this(layer, width, rounding, points, "", "");
        }

        public boolean bare() {
            return PcbVocabulary.FINISH_BARE.equals(finish);
        }

        /** The same track run through other points. */
        public Track withPoints(List<Point> others) {
            return new Track(layer, width, rounding, others, group, finish);
        }

        /** The same track with its corners rounded so. */
        public Track withRounding(double other) {
            return new Track(layer, width, other, points, group, finish);
        }
    }

    /** A track bent as an arc, through three points. */
    public record Arc(String layer, double width, Point from, Point through, Point to, String group, String finish) {

        public Arc {
            group = group == null ? "" : group;
            finish = finish == null ? "" : finish;
        }

        /** An arc in no group, under the mask. */
        public Arc(String layer, double width, Point from, Point through, Point to) {
            this(layer, width, from, through, to, "", "");
        }

        public boolean bare() {
            return PcbVocabulary.FINISH_BARE.equals(finish);
        }
    }

    /** @param from the layer it starts on; empty for through the pcb */
    public record Via(double x, double y, double drill, double diameter, String from, String to, String open, String group) {

        public Via {
            group = group == null ? "" : group;
        }

        /** A via in no group. */
        public Via(double x, double y, double drill, double diameter, String from, String to, String open) {
            this(x, y, drill, diameter, from, to, open, "");
        }

        /** A via covered by the solder mask on both sides. */
        public Via(double x, double y, double drill, double diameter, String from, String to) {
            this(x, y, drill, diameter, from, to, "");
        }

        /** Whether the solder mask is left open over it on a side. */
        public boolean opensMaskOn(String side) {
            return PcbVocabulary.VIA_OPEN_BOTH.equals(open) || side.equals(open);
        }

    }

    /** @param clearance 0 for the pcb's own */
    /** @param finish what its copper is left as: {@link PcbVocabulary#FINISH_BARE}, the solder mask open over all of it; empty for under the mask */
    public record Zone(String layer, String net, double clearance, int priority, String path, String finish) {

        public Zone {
            finish = finish == null ? "" : finish;
        }

        /** A pour under the mask. */
        public Zone(String layer, String net, double clearance, int priority, String path) {
            this(layer, net, clearance, priority, path, "");
        }

        public boolean bare() {
            return PcbVocabulary.FINISH_BARE.equals(finish);
        }
    }

    public record Keepout(List<String> layers, String path) {

        public Keepout {
            layers = List.copyOf(layers);
        }
    }

    public record Dimension(Point from, Point to, double offset) {
    }

    /**
     * @param layer  {@code top-silkscreen} or {@code bottom-silkscreen}
     * @param anchor which part of the words stands on their point: {@code start}, {@code middle} — the usual — or {@code end}
     */
    public record Text(String layer, String text, double x, double y, double size, double rotation, String anchor) {

        /** Words standing round their point. */
        public Text(String layer, String text, double x, double y, double size, double rotation) {
            this(layer, text, x, y, size, rotation, MIDDLE);
        }

        public Text {
            anchor = anchor == null ? MIDDLE : anchor;
        }
    }

    /**
     * Something drawn on a layer of the pcb itself, which no part brought: a logo on the silkscreen, a window in the mask, a
     * note on the documentation, copper that is no track.
     *
     * @param layer  the layer's id: {@code top}, {@code inner-1}, {@code top-silkscreen}, {@code bottom-mask}, {@code comments}
     * @param width  what it is stroked with; 0 when it is filled
     * @param filled whether it is an area rather than lines
     * @param net    the net copper carries; empty for none
     * @param path   what it draws, as path data
     */
    public record Shape(String layer, double width, boolean filled, String net, String path) {

        public Shape {
            net = net == null ? "" : net;
        }
    }

    /**
     * What this pcb is released as: a set of formats written together.
     *
     * @param name    one word it is known by
     * @param formats the formats, by the word each is written under: {@code gerber}, {@code step}
     * @param title   how a reader is shown it
     */
    public record Output(String name, List<String> formats, String title) {

        public Output {
            formats = List.copyOf(formats);
        }
    }

    /** @param ring 0: a bare hole */
    public record Hole(double x, double y, double drill, double ring) {
    }

    private static final PathParser PATHS = new PathParser();

    /** The lines a pcb editor draws, by word: every line of a pcb. */
    private static final Set<String> DRAWN = Set.of(PcbVocabulary.CIRCUIT, "rules", "class", "pair", "sizes", "teardrops", "layers",
                                                    "edge", "place", "track", "arc", "via", "zone", "keepout", "dimension",
                                                    "text", "hole", "shape", "output");

    /** A line's class, as a pcb editor draws it — its word —; empty for a statement no pcb editor draws. */
    public static Optional<String> classOf(Expression statement) {
        return statement instanceof ClauseLineNode line && line.word() != null && DRAWN.contains(line.word())
                && PcbLinesParser.grammar(line.word()) == line.grammar() ? Optional.of(line.word()) : Optional.empty();
    }

    /**
     * The pcb as its document's lines, built on the grammars that read them, in the order a pcb is written: its
     * circuit, how it is made, its classes, pairs and teardrops, its layers and edge, its parts, then its copper, keepouts,
     * dimensions, texts and holes. A value that is its line's default — no rotation, the top side — is not written.
     *
     * @throws IllegalArgumentException when a path does not read as path data
     */
    public List<ClauseLineNode> lines() {
        List<ClauseLineNode> lines = new ArrayList<>();
        if (circuit != null && !circuit.isEmpty()) {
            lines.add(line(PcbVocabulary.CIRCUIT).slot(PcbWords.SLUG, Nodes.word(circuit)).build());
        }
        rules.ifPresent(made -> {
            LineBuilder line = line("rules").slot(PcbWords.PROFILE, Nodes.word(made.profile()));
            made.changes().forEach((rule, value) -> line.clause(rule, Nodes.number(value)));
            lines.add(line.build());
        });
        for (NetClass netClass : netClasses) {
            LineBuilder line = line("class").slot(PcbWords.NAME, Nodes.word(netClass.name()))
                    .clause(PcbWords.WIDTH, Nodes.number(netClass.width())).clause(PcbWords.CLEARANCE, Nodes.number(netClass.clearance()));
            if (netClass.drill() > 0) {
                line.clause(PcbWords.DRILL, Nodes.number(netClass.drill()));
            }
            if (netClass.diameter() > 0) {
                line.clause(PcbWords.DIAMETER, Nodes.number(netClass.diameter()));
            }
            lines.add(line.clause(PcbWords.NETS, Nodes.words(netClass.nets())).build());
        }
        for (Pair pair : pairs) {
            LineBuilder line = line("pair").slot(PcbWords.NAME, Nodes.word(pair.name())).clause(PcbWords.WIDTH, Nodes.number(pair.width()))
                    .clause(PcbWords.GAP, Nodes.number(pair.gap()));
            if (pair.skew() > 0) {
                line.clause(PcbWords.SKEW, Nodes.number(pair.skew()));
            }
            lines.add(line.clause(PcbWords.NETS, Nodes.words(List.of(pair.positive(), pair.negative()))).build());
        }
        if (!sizes.tracks().isEmpty()) {
            lines.add(line("sizes").slot(PcbWords.SIZED, Nodes.word(PcbVocabulary.TRACKS)).slot(PcbWords.WIDTHS, Nodes.numbers(sizes.tracks())).build());
        }
        if (!sizes.vias().isEmpty()) {
            lines.add(line("sizes").slot(PcbWords.SIZED, Nodes.word(PcbVocabulary.VIAS))
                              .clause(PcbWords.DRILL, Nodes.numbers(sizes.vias().stream().map(ViaSize::drill).toList()))
                              .clause(PcbWords.DIAMETER, Nodes.numbers(sizes.vias().stream().map(ViaSize::diameter).toList())).build());
        }
        for (Teardrop teardrop : teardrops) {
            LineBuilder line = line("teardrops").slot(PcbWords.TARGET, Nodes.word(teardrop.target()))
                    .clause(PcbWords.LENGTH, Nodes.number(teardrop.length())).clause(PcbWords.WIDTH, Nodes.number(teardrop.width()));
            if (teardrop.curved()) {
                line.flag(PcbWords.CURVED);
            }
            lines.add(line.build());
        }
        if (copperLayers > 0) {
            lines.add(line("layers").slot(PcbWords.COUNT, Nodes.number(copperLayers)).build());
        }
        if (outline != null && !outline.isEmpty()) {
            lines.add(line("edge").ending(pathText("the edge", outline)).build());
        }
        placements.forEach(placement -> lines.add(placement(placement)));
        for (Track track : tracks) {
            LineBuilder line = line("track").slot(PcbWords.LAYER, Nodes.word(track.layer())).clause(PcbWords.WIDTH, Nodes.number(track.width()));
            if (track.rounding() > 0) {
                line.clause(PcbWords.ROUNDING, Nodes.number(track.rounding()));
            }
            line.clause(PcbWords.FROM, point(track.points().getFirst()));
            track.points().stream().skip(1).forEach(point -> line.clause(PcbWords.TO, point(point)));
            inGroup(line, track.group());
            finished(line, track.finish());
            lines.add(line.build());
        }
        for (Arc arc : arcs) {
            LineBuilder line = line("arc").slot(PcbWords.LAYER, Nodes.word(arc.layer())).clause(PcbWords.WIDTH, Nodes.number(arc.width()))
                    .clause(PcbWords.FROM, point(arc.from())).clause(PcbWords.THROUGH, point(arc.through())).clause(PcbWords.TO, point(arc.to()));
            inGroup(line, arc.group());
            finished(line, arc.finish());
            lines.add(line.build());
        }
        for (Via via : vias) {
            LineBuilder line = line("via").clause(PcbWords.AT, Nodes.point(via.x(), via.y())).clause(PcbWords.DRILL, Nodes.number(via.drill()))
                    .clause(PcbWords.DIAMETER, Nodes.number(via.diameter()));
            if (via.from() != null && !via.from().isEmpty()) {
                line.clause(PcbWords.FROM, Nodes.word(via.from()));
            }
            if (via.to() != null && !via.to().isEmpty()) {
                line.clause(PcbWords.TO, Nodes.word(via.to()));
            }
            if (via.open() != null && !via.open().isEmpty()) {
                line.clause(PcbWords.OPEN, Nodes.word(via.open()));
            }
            inGroup(line, via.group());
            lines.add(line.build());
        }
        for (Zone zone : zones) {
            LineBuilder line = line("zone").slot(PcbWords.LAYER, Nodes.word(zone.layer())).clause(PcbWords.NET, Nodes.word(zone.net()));
            if (zone.clearance() > 0) {
                line.clause(PcbWords.CLEARANCE, Nodes.number(zone.clearance()));
            }
            if (zone.priority() > 0) {
                line.clause(PcbWords.PRIORITY, Nodes.number(zone.priority()));
            }
            finished(line, zone.finish());
            lines.add(line.ending(pathText("the zone on " + zone.layer(), zone.path())).build());
        }
        for (Keepout keepout : keepouts) {
            lines.add(line("keepout").slot(PcbWords.LAYERS, Nodes.words(keepout.layers()))
                    .ending(pathText("the keepout", keepout.path())).build());
        }
        for (Dimension dimension : dimensions) {
            LineBuilder line = line("dimension").clause(PcbWords.FROM, point(dimension.from())).clause(PcbWords.TO, point(dimension.to()));
            if (dimension.offset() != 0) {
                line.clause(PcbWords.OFFSET, Nodes.number(dimension.offset()));
            }
            lines.add(line.build());
        }
        for (Text text : texts) {
            LineBuilder line = line("text").slot(PcbWords.LAYER, Nodes.word(text.layer())).clause(PcbWords.SIZE, Nodes.number(text.size()))
                    .clause(PcbWords.AT, Nodes.point(text.x(), text.y()));
            if (text.rotation() != 0) {
                line.clause(PcbWords.ROTATE, Nodes.number(text.rotation()));
            }
            if (!MIDDLE.equals(text.anchor())) {
                line.clause(PcbWords.ANCHOR, Nodes.word(text.anchor()));
            }
            lines.add(line.ending(Nodes.text(text.text())).build());
        }
        for (Shape shape : shapes) {
            LineBuilder line = line("shape").slot(PcbWords.LAYER, Nodes.word(shape.layer()));
            if (shape.width() > 0) {
                line.clause(PcbWords.WIDTH, Nodes.number(shape.width()));
            }
            if (shape.filled()) {
                line.flag(PcbWords.FILLED);
            }
            if (!shape.net().isEmpty()) {
                line.clause(PcbWords.NET, Nodes.word(shape.net()));
            }
            lines.add(line.ending(pathText("the shape on " + shape.layer(), shape.path())).build());
        }
        for (Hole hole : holes) {
            LineBuilder line = line("hole").clause(PcbWords.AT, Nodes.point(hole.x(), hole.y())).clause(PcbWords.DRILL, Nodes.number(hole.drill()));
            if (hole.ring() > 0) {
                line.clause(PcbWords.RING, Nodes.number(hole.ring()));
            }
            lines.add(line.build());
        }
        for (Output output : outputs) {
            lines.add(line("output").slot(PcbWords.NAME, Nodes.word(output.name())).clause(PcbWords.FORMATS, Nodes.words(output.formats()))
                    .ending(Nodes.text(output.title())).build());
        }
        return lines;
    }

    /** {@code place R1 [shown R1] [footprint slug [nets (GND = 1, OUT = 2)]] at x, y [rotate r] [side bottom] [reference at x, y [rotate r]]}. */
    /** What a line's copper is left as, said when it is not under the mask. */
    private static void finished(LineBuilder line, String finish) {
        if (!finish.isEmpty()) {
            line.clause(PcbWords.FINISH, Nodes.word(finish));
        }
    }

    /** The group a line's thing is in, said when it is in one. */
    private static void inGroup(LineBuilder line, String group) {
        if (!group.isEmpty()) {
            line.clause(PcbWords.GROUP, Nodes.word(group));
        }
    }

    private static ClauseLineNode placement(Placement placement) {
        LineBuilder line = line("place").slot(PcbWords.REFERENCE, Nodes.word(placement.designator()));
        placement.shown().ifPresent(shown -> line.clause(PcbWords.SHOWN, Nodes.word(shown)));
        placement.footprint().ifPresent(footprint -> line.clause(PcbWords.FOOTPRINT, Nodes.word(footprint)));
        if (!placement.nets().isEmpty()) {
            List<ConnectionsNode.Connection> nets = new ArrayList<>();
            placement.nets().forEach((pad, net) -> nets.add(new ConnectionsNode.Connection(net, pad)));
            line.clause(PcbWords.NETS, new ConnectionsNode(nets));
        }
        line.clause(PcbWords.AT, Nodes.point(placement.x(), placement.y()));
        if (placement.rotation() != 0) {
            line.clause(PcbWords.ROTATE, Nodes.number(placement.rotation()));
        }
        if (placement.side() != null && !placement.side().equals(TOP)) {
            line.clause(PcbWords.SIDE, Nodes.word(placement.side()));
        }
        inGroup(line, placement.group());
        if (!placement.role().isEmpty()) {
            line.clause(PcbWords.ROLE, Nodes.word(placement.role()));
        }
        placement.reference().ifPresent(reference -> {
            LineBuilder nested = LineBuilder.line(PcbLinesParser.grammar("place").clause(PcbWords.REFERENCE).nested())
                    .clause(PcbWords.AT, Nodes.point(reference.x(), reference.y()));
            if (reference.rotation() != 0) {
                nested.clause(PcbWords.ROTATE, Nodes.number(reference.rotation()));
            }
            if (reference.layer() != null && !reference.layer().isEmpty()) {
                nested.clause(PcbWords.LAYER, Nodes.word(reference.layer()));
            }
            line.clause(PcbWords.REFERENCE, nested.build().asHeader());
        });
        return line.build();
    }

    private static LineBuilder line(String word) {
        return LineBuilder.line(PcbLinesParser.grammar(word));
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

    /** The pcb a pcb document's lines make. */
    public static PcbDrawing of(DocumentNode pcb) {
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
        List<Shape> shapes = new ArrayList<>();
        List<Output> outputs = new ArrayList<>();
        List<Dimension> dimensions = new ArrayList<>();
        List<Text> texts = new ArrayList<>();
        List<Hole> holes = new ArrayList<>();
        for (Expression statement : pcb.getExpressions()) {
            if (!(statement instanceof ClauseLineNode line)) {
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
                            line.quantity(PcbWords.ROUNDING).orElse(0.0), points, line.word(PcbWords.GROUP).orElse(""), line.word(PcbWords.FINISH).orElse("")));
                }
                case "arc" -> arcs.add(new Arc(line.name(PcbWords.LAYER).orElseThrow(), line.quantity(PcbWords.WIDTH).orElseThrow(),
                        point(line.point(PcbWords.FROM).orElseThrow()), point(line.point(PcbWords.THROUGH).orElseThrow()),
                        point(line.point(PcbWords.TO).orElseThrow()), line.word(PcbWords.GROUP).orElse(""), line.word(PcbWords.FINISH).orElse("")));
                case "via" -> {
                    PointNode at = line.point(PcbWords.AT).orElseThrow();
                    vias.add(new Via(at.x(), at.y(), line.quantity(PcbWords.DRILL).orElseThrow(), line.quantity(PcbWords.DIAMETER).orElseThrow(),
                            line.word(PcbWords.FROM).orElse(""), line.word(PcbWords.TO).orElse(""), line.word(PcbWords.OPEN).orElse(""), line.word(PcbWords.GROUP).orElse("")));
                }
                case "zone" -> zones.add(new Zone(line.name(PcbWords.LAYER).orElseThrow(), line.word(PcbWords.NET).orElseThrow(),
                        line.quantity(PcbWords.CLEARANCE).orElse(0.0), (int) Math.round(line.quantity(PcbWords.PRIORITY).orElse(0.0)), path(line), line.word(PcbWords.FINISH).orElse("")));
                case "keepout" -> keepouts.add(new Keepout(line.slotNames("layers"), path(line)));
                case "dimension" -> dimensions.add(new Dimension(point(line.point(PcbWords.FROM).orElseThrow()),
                        point(line.point(PcbWords.TO).orElseThrow()), line.quantity(PcbWords.OFFSET).orElse(0.0)));
                case "text" -> {
                    PointNode at = line.point(PcbWords.AT).orElseThrow();
                    texts.add(new Text(line.name(PcbWords.LAYER).orElseThrow(), line.ending().orElse(""), at.x(), at.y(),
                            line.quantity(PcbWords.SIZE).orElseThrow(), line.quantity(PcbWords.ROTATE).orElse(0.0), line.word(PcbWords.ANCHOR).orElse(MIDDLE)));
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
                tracks, arcs, vias, zones, keepouts, dimensions, texts, holes, shapes, outputs);
    }

    private static Placement placement(ClauseLineNode line) {
        PointNode at = line.point(PcbWords.AT).orElseThrow();
        Optional<Reference> reference = line.nested("reference").map(nested -> {
            PointNode where = nested.point(PcbWords.AT).orElseThrow();
            return new Reference(where.x(), where.y(), nested.quantity(PcbWords.ROTATE).orElse(0.0), nested.word(PcbWords.LAYER).orElse(""));
        });
        return new Placement(line.name(PcbWords.REFERENCE).orElseThrow(), at.x(), at.y(), line.quantity(PcbWords.ROTATE).orElse(0.0),
                line.word(PcbWords.SIDE).orElse(TOP), reference, line.word(PcbWords.SHOWN), line.word(PcbWords.FOOTPRINT), netsOf(line), line.word(PcbWords.GROUP).orElse(""), line.word(PcbWords.ROLE).orElse(""));
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
