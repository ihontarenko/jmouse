package org.jmouse.kicad.pcb;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadLayer;
import org.jmouse.kicad.KiCadOutline;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.sexpression.SExpressionDocument;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A {@code .kicad_pcb} as KiCad wrote it: its copper layers, the footprints standing on it, its copper and the graphics
 * drawn on its layers, in millimetres with Y downwards, in the frame of KiCad's page. Versions 7 and 8 write the same
 * lists; only their layout on the lines differs.
 *
 * @param copper its copper layers, front first, as KiCad names them
 * @param images how many pictures it holds, which this model does not read
 * @param groups its groups: each names its members by their identities
 */
public record KiCadPcb(String name, List<String> copper, List<KiCadPcbFootprint> footprints, List<Track> tracks, List<Arc> arcs,
                       List<Via> vias, List<Zone> zones, List<KiCadFootprintGraphic> graphics, List<Text> texts,
                       List<Dimension> dimensions, int images, List<Group> groups) {

    public KiCadPcb {
        copper = List.copyOf(copper);
        footprints = List.copyOf(footprints);
        tracks = List.copyOf(tracks);
        arcs = List.copyOf(arcs);
        vias = List.copyOf(vias);
        zones = List.copyOf(zones);
        graphics = List.copyOf(graphics);
        texts = List.copyOf(texts);
        dimensions = List.copyOf(dimensions);
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    /**
     * A group of the board — {@code (group "front panel" (uuid …) (locked yes) (members "…" "…"))} —: its name, how it stands
     * itself, and its members by their identities.
     */
    public record Group(String name, KiCadStanding standing, List<String> members) {

        public Group {
            members = List.copyOf(members);
        }

        static Group of(SExpressionList group) {
            String name = group.size() > 1 && !(group.item(1) instanceof SExpressionList) ? group.word(1) : "";
            List<String> members = group.list(MEMBERS).map(KiCadPcb::wordsOf).orElse(List.of());
            return new Group(name, KiCadStanding.of(group), members);
        }
    }

    /** A straight run of copper. */
    public record Track(KiCadPoint start, KiCadPoint end, double width, String layer, KiCadStanding standing) {
    }

    /** A curved run of copper, from its start through its middle to its end. */
    public record Arc(KiCadPoint start, KiCadPoint middle, KiCadPoint end, double width, String layer, KiCadStanding standing) {
    }

    /**
     * A hole joining copper layers.
     *
     * @param layers the two layers it runs between
     */
    public record Via(KiCadPoint at, double diameter, double drill, List<String> layers, Tenting tenting, KiCadStanding standing) {

        public Via {
            layers = List.copyOf(layers);
        }
    }

    /** What an area of a board is: copper poured on a net, or a rule keeping things out. */
    public enum ZoneKind {
        COPPER, KEEPOUT
    }

    /**
     * An area drawn on one or more layers.
     *
     * @param net       the net it pours, as KiCad names it; empty for one on no net
     * @param clearance how far it stays from other copper; 0 when it does not say
     * @param priority  which pours first where zones overlap, the higher first
     * @param outlines  each closed outline it draws
     */
    public record Zone(ZoneKind kind, String net, List<String> layers, double clearance, int priority,
                       List<KiCadOutline> outlines) {

        public Zone {
            layers = List.copyOf(layers);
            outlines = List.copyOf(outlines);
        }
    }

    /** How words stand on their point along their line, and across it. */
    public enum Along {
        LEFT, CENTRE, RIGHT
    }

    public enum Across {
        TOP, MIDDLE, BOTTOM
    }

    /** Words printed on a layer. */
    public record Text(String text, KiCadPoint at, double angle, String layer, double height, Along along, Across across) {
    }

    /**
     * A measure drawn between two points.
     *
     * @param height how far from them its line stands
     */
    public record Dimension(KiCadPoint start, KiCadPoint end, double height) {
    }

    /**
     * The board a document holds.
     *
     * @throws IllegalArgumentException when it is not a board
     */
    public static KiCadPcb of(SExpressionDocument document) {
        SExpressionList root = document.root();
        if (!root.is(PCB_FILE)) {
            throw new IllegalArgumentException(document.name() + " is a " + document.kind() + ", not a board.");
        }
        return new KiCadPcb(document.name(), copperOf(root), root.lists(FOOTPRINT).stream().map(KiCadPcbFootprint::of).toList(),
                root.lists(SEGMENT).stream().map(KiCadPcb::track).toList(), root.lists(TRACK_ARC).stream().map(KiCadPcb::arc).toList(),
                root.lists(VIA).stream().map(via -> via(via, usualTenting(root))).toList(), root.lists(ZONE).stream().map(KiCadPcb::zone).toList(),
                graphicsOf(root), root.lists(BOARD_TEXT).stream().map(KiCadPcb::text).toList(),
                root.lists(DIMENSION).stream().flatMap(written -> dimension(written).stream()).toList(),
                root.lists(IMAGE).size(), root.lists(GROUP).stream().map(Group::of).toList());
    }

    /** {@code (layers (0 "F.Cu" signal) … (31 "B.Cu" signal) (32 "B.Adhes" user …))}: those of copper, as they are written. */
    private static List<String> copperOf(SExpressionList root) {
        List<String> copper = new ArrayList<>();
        root.list(LAYERS).ifPresent(layers -> {
            for (SExpressionNode item : layers.items()) {
                if (item instanceof SExpressionList layer && layer.size() > 1 && layer.word(1).endsWith(KiCadLayer.COPPER_SUFFIX)) {
                    copper.add(layer.word(1));
                }
            }
        });
        return copper;
    }

    private static List<KiCadFootprintGraphic> graphicsOf(SExpressionList root) {
        List<KiCadFootprintGraphic> graphics = new ArrayList<>();
        for (SExpressionNode item : root.items()) {
            if (item instanceof SExpressionList list) {
                KiCadFootprintGraphic.of(list).ifPresent(graphics::add);
            }
        }
        return graphics;
    }

    private static Track track(SExpressionList segment) {
        return new Track(KiCadPoint.of(segment.requireList(START)), KiCadPoint.of(segment.requireList(END)),
                segment.requireList(WIDTH).number(1), segment.wordOf(LAYER).orElse(""), KiCadStanding.of(segment));
    }

    /**
     * Which sides of a via the solder mask covers: {@code (tenting front back)}, or as KiCad 9 writes it
     * {@code (tenting (front yes) (back no))}.
     */
    public record Tenting(boolean front, boolean back) {

        public static final Tenting BOTH = new Tenting(true, true);

        static Tenting of(SExpressionList tenting) {
            List<String> words = wordsOf(tenting);
            boolean front = tenting.wordOf(FRONT).map("yes"::equals).orElse(words.contains(FRONT.word()));
            boolean back = tenting.wordOf(BACK).map("yes"::equals).orElse(words.contains(BACK.word()));
            return new Tenting(front, back);
        }
    }

    private static Arc arc(SExpressionList arc) {
        return new Arc(KiCadPoint.of(arc.requireList(START)), KiCadPoint.of(arc.requireList(MIDDLE)),
                KiCadPoint.of(arc.requireList(END)), arc.requireList(WIDTH).number(1), arc.wordOf(LAYER).orElse(""), KiCadStanding.of(arc));
    }

    private static Via via(SExpressionList via, Tenting usual) {
        return new Via(KiCadPoint.of(via.requireList(AT)), via.requireList(SIZE).number(1), via.requireList(DRILL).number(1),
                wordsOf(via.requireList(LAYERS)), via.list(TENTING).map(Tenting::of).orElse(usual), KiCadStanding.of(via));
    }

    /** What the board says of a via that says nothing; a board that says nothing covers both sides, as KiCad makes one. */
    private static Tenting usualTenting(SExpressionList root) {
        return root.list(SETUP).flatMap(setup -> setup.list(TENTING)).map(Tenting::of).orElse(Tenting.BOTH);
    }

    private static Zone zone(SExpressionList zone) {
        List<String> layers = zone.list(LAYERS).map(KiCadPcb::wordsOf)
                .orElseGet(() -> zone.wordOf(LAYER).map(List::of).orElse(List.of()));
        double clearance = zone.list(CONNECT_PADS).flatMap(pads -> pads.list(CLEARANCE)).map(written -> written.number(1)).orElse(0.0);
        int priority = zone.list(PRIORITY).map(written -> (int) written.number(1)).orElse(0);
        List<KiCadOutline> outlines = new ArrayList<>();
        zone.lists(ZONE_OUTLINE).forEach(polygon -> polygon.list(POINTS).map(KiCadOutline::of).ifPresent(outlines::add));
        return new Zone(zone.list(KEEPOUT).isPresent() ? ZoneKind.KEEPOUT : ZoneKind.COPPER, zone.wordOf(NET_NAME).orElse(""),
                layers, clearance, priority, outlines);
    }

    private static Text text(SExpressionList text) {
        SExpressionList at = text.requireList(AT);
        double height = text.list(EFFECTS).flatMap(effects -> effects.list(FONT)).flatMap(font -> font.list(SIZE))
                .map(size -> size.number(1)).orElse(1.0);
        return new Text(text.word(1), KiCadPoint.of(at), at.size() > 3 ? at.number(3) : 0, text.wordOf(LAYER).orElse(""), height,
                alongOf(text), acrossOf(text));
    }

    /** How a text stands along its point, as its {@code (effects (justify …))} says: centred where it says nothing. */
    public static Along alongOf(SExpressionList text) {
        Set<String> justified = justifiedOf(text);
        return justified.contains("left") ? Along.LEFT : justified.contains("right") ? Along.RIGHT : Along.CENTRE;
    }

    /** How a text stands across its point, as its {@code (effects (justify …))} says: on its middle where it says nothing. */
    public static Across acrossOf(SExpressionList text) {
        Set<String> justified = justifiedOf(text);
        return justified.contains("top") ? Across.TOP : justified.contains("bottom") ? Across.BOTTOM : Across.MIDDLE;
    }

    /** The words a {@code justify} writes for how a text stands on its point: none for one on its middle, KiCad's default. */
    public static List<SExpressionNode> justifyWords(Along along, Across across) {
        List<SExpressionNode> words = new ArrayList<>();
        switch (along) {
            case LEFT -> words.add(SExpressionAtom.of("left"));
            case RIGHT -> words.add(SExpressionAtom.of("right"));
            case CENTRE -> {
            }
        }
        switch (across) {
            case TOP -> words.add(SExpressionAtom.of("top"));
            case BOTTOM -> words.add(SExpressionAtom.of("bottom"));
            case MIDDLE -> {
            }
        }
        return words;
    }

    private static Set<String> justifiedOf(SExpressionList text) {
        return text.list(EFFECTS).flatMap(effects -> effects.list(JUSTIFY)).map(justify -> Set.copyOf(wordsAfterName(justify))).orElse(Set.of());
    }

    /** One with its two points, as an aligned or orthogonal measure writes them; a leader or a centre mark measures nothing. */
    private static Optional<Dimension> dimension(SExpressionList dimension) {
        List<KiCadPoint> points = dimension.list(POINTS).map(written -> written.lists(POINT).stream().map(KiCadPoint::of).toList())
                .orElse(List.of());
        if (points.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(new Dimension(points.get(0), points.get(1),
                dimension.list(HEIGHT).map(written -> written.number(1)).orElse(0.0)));
    }

    /** {@code (justify left bottom)}: its bare words. */
    private static List<String> wordsAfterName(SExpressionList list) {
        List<String> words = new ArrayList<>();
        for (SExpressionNode item : list.items().subList(1, list.items().size())) {
            if (item instanceof SExpressionAtom atom) {
                words.add(atom.value());
            }
        }
        return words;
    }

    private static List<String> wordsOf(SExpressionList list) {
        List<String> words = new ArrayList<>();
        for (SExpressionNode item : list.items().subList(1, list.items().size())) {
            if (item instanceof SExpressionString text) {
                words.add(text.text());
            }
        }
        return words;
    }
}
