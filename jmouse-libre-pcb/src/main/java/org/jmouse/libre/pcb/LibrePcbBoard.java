package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** One board of a project: how it is made, the devices standing on it, its copper, and what else it draws. */
public record LibrePcbBoard(String name, int innerLayers, double thickness, Set<String> printedLayers, DesignRules rules, List<DeviceOnBoard> devices,
                            List<Segment> segments, List<Plane> planes, List<LibrePcbGraphic> graphics,
                            List<LibrePcbLibrary.Hole> holes) {

    /** @param printedLayers the layers the board's silkscreen is made of: what a part's name on any other never reaches */
    public LibrePcbBoard {
        printedLayers = Set.copyOf(printedLayers);
        devices = List.copyOf(devices);
        segments = List.copyOf(segments);
        planes = List.copyOf(planes);
        graphics = List.copyOf(graphics);
        holes = List.copyOf(holes);
    }

    /** A clearance that grows with what it is about: a share of its size, between a least and a most. */
    public record Clearance(double ratio, double least, double most) {

        public static final Clearance NONE = new Clearance(0, 0, 0);

        static Clearance of(SExpressionList rules, LibrePcbKeyword name) {
            return rules.list(name).map(list -> new Clearance(LibrePcbValues.number(list, LibrePcbKeyword.RATIO),
                    LibrePcbValues.number(list, LibrePcbKeyword.MINIMUM), LibrePcbValues.number(list, LibrePcbKeyword.MAXIMUM))).orElse(NONE);
        }

        /** The clearance for something of this shorter side. */
        public double of(double shorterSide) {
            return Math.min(most, Math.max(least, shorterSide * ratio));
        }
    }

    /**
     * @param maskClearance  how far a pad's mask opening grows
     * @param pasteClearance how far a pad's paste shrinks
     * @param largestTented  the largest drill the mask covers on a via that leaves it to the rules
     * @param checks         the numbers of the board's {@code design_rule_check}, by LibrePCB's name
     */
    public record DesignRules(double traceWidth, double viaDrill, Clearance maskClearance, Clearance pasteClearance, double largestTented,
                              Map<String, Double> checks) {

        public DesignRules {
            checks = Map.copyOf(checks);
        }

        static DesignRules of(SExpressionList board) {
            Map<String, Double> checks = new LinkedHashMap<>();
            board.list(LibrePcbKeyword.DESIGN_RULE_CHECK).ifPresent(check -> check.lists().forEach(list -> {
                if (list.size() == 2) {
                    LibrePcbValues.parsed(list.word(1)).ifPresent(number -> checks.put(list.name(), number));
                }
            }));
            Optional<SExpressionList> written = board.list(LibrePcbKeyword.DESIGN_RULES);
            return new DesignRules(written.map(rules -> LibrePcbValues.numberOr(rules, LibrePcbKeyword.DEFAULT_TRACE_WIDTH, 0)).orElse(0.0),
                    written.map(rules -> LibrePcbValues.numberOr(rules, LibrePcbKeyword.DEFAULT_VIA_DRILL, 0)).orElse(0.0),
                    written.map(rules -> Clearance.of(rules, LibrePcbKeyword.MASK_CLEARANCE)).orElse(Clearance.NONE),
                    written.map(rules -> Clearance.of(rules, LibrePcbKeyword.PASTE_CLEARANCE)).orElse(Clearance.NONE),
                    written.map(rules -> LibrePcbValues.numberOr(rules, LibrePcbKeyword.LARGEST_TENTED_DRILL, 0)).orElse(0.0), checks);
        }
    }

    /** A part standing on the board; its {@code id} is the part's. */
    public record DeviceOnBoard(String id, String device, String footprint, LibrePcbTransform transform, List<LibrePcbGraphic.Text> texts) {

        public DeviceOnBoard {
            texts = List.copyOf(texts);
        }
    }

    public record Via(String id, String from, String to, LibrePcbPoint at, double drill, double size, LibrePcbOpening exposure) {
    }

    public record Trace(String layer, double width, LibrePcbAnchor from, LibrePcbAnchor to) {
    }

    public record Segment(String net, List<Via> vias, List<LibrePcbJunction> junctions, List<Trace> traces) {

        public Segment {
            vias = List.copyOf(vias);
            junctions = List.copyOf(junctions);
            traces = List.copyOf(traces);
        }
    }

    /** A pour of a net on a copper layer. */
    public record Plane(String layer, String net, int priority, double clearance, List<LibrePcbGraphic.Vertex> vertices) {

        public Plane {
            vertices = List.copyOf(vertices);
        }
    }

    static LibrePcbBoard of(SExpressionList board) {
        return new LibrePcbBoard(LibrePcbValues.text(board, LibrePcbKeyword.NAME),
                board.list(LibrePcbKeyword.LAYERS).map(layers -> (int) LibrePcbValues.number(layers, LibrePcbKeyword.INNER)).orElse(0),
                LibrePcbValues.numberOr(board, LibrePcbKeyword.THICKNESS, 0), printedLayers(board), DesignRules.of(board),
                board.lists(LibrePcbKeyword.DEVICE).stream().map(LibrePcbBoard::device).toList(),
                board.lists(LibrePcbKeyword.SEGMENT).stream().map(LibrePcbBoard::segment).toList(),
                board.lists(LibrePcbKeyword.PLANE).stream().map(LibrePcbBoard::plane).toList(), LibrePcbGraphic.of(board),
                board.lists(LibrePcbKeyword.HOLE).stream().map(LibrePcbLibrary.Hole::of).toList());
    }

    /** {@code (silkscreen_layers_top top_legend top_names)}, and the same for the bottom. */
    private static Set<String> printedLayers(SExpressionList board) {
        Set<String> printed = new HashSet<>();
        for (LibrePcbKeyword side : List.of(LibrePcbKeyword.SILKSCREEN_TOP, LibrePcbKeyword.SILKSCREEN_BOTTOM)) {
            board.list(side).ifPresent(layers -> {
                for (int index = 1; index < layers.size(); index++) {
                    printed.add(layers.word(index));
                }
            });
        }
        return printed;
    }

    private static DeviceOnBoard device(SExpressionList device) {
        return new DeviceOnBoard(device.word(1), LibrePcbValues.text(device, LibrePcbKeyword.LIBRARY_DEVICE),
                LibrePcbValues.text(device, LibrePcbKeyword.LIBRARY_FOOTPRINT), new LibrePcbTransform(LibrePcbValues.position(device),
                LibrePcbValues.number(device, LibrePcbKeyword.ROTATION), LibrePcbValues.flag(device, LibrePcbKeyword.FLIP)),
                device.lists(LibrePcbKeyword.STROKE_TEXT).stream().map(LibrePcbGraphic.Text::of).toList());
    }

    private static Segment segment(SExpressionList segment) {
        return new Segment(LibrePcbValues.text(segment, LibrePcbKeyword.NET),
                segment.lists(LibrePcbKeyword.VIA).stream().map(via -> new Via(via.word(1), LibrePcbValues.text(via, LibrePcbKeyword.FROM),
                        LibrePcbValues.text(via, LibrePcbKeyword.TO), LibrePcbValues.position(via), LibrePcbValues.number(via, LibrePcbKeyword.DRILL),
                        LibrePcbValues.number(via, LibrePcbKeyword.SIZE), LibrePcbOpening.of(via.wordOf(LibrePcbKeyword.EXPOSURE)))).toList(),
                segment.lists(LibrePcbKeyword.JUNCTION).stream().map(LibrePcbJunction::of).toList(),
                segment.lists(LibrePcbKeyword.TRACE).stream().map(trace -> new Trace(LibrePcbValues.text(trace, LibrePcbKeyword.LAYER),
                        LibrePcbValues.number(trace, LibrePcbKeyword.WIDTH), LibrePcbAnchor.of(trace.requireList(LibrePcbKeyword.FROM)),
                        LibrePcbAnchor.of(trace.requireList(LibrePcbKeyword.TO)))).toList());
    }

    private static Plane plane(SExpressionList plane) {
        return new Plane(LibrePcbValues.text(plane, LibrePcbKeyword.LAYER), LibrePcbValues.text(plane, LibrePcbKeyword.NET),
                (int) LibrePcbValues.number(plane, LibrePcbKeyword.PRIORITY), LibrePcbValues.number(plane, LibrePcbKeyword.MINIMUM_COPPER_CLEARANCE),
                LibrePcbGraphic.verticesOf(plane));
    }
}
