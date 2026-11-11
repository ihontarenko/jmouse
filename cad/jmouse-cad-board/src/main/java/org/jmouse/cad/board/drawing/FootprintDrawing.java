package org.jmouse.cad.board.drawing;

import org.jmouse.cad.board.FootprintLines;
import org.jmouse.cad.board.FootprintLinesParser;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ExtentNode;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.el.grammar.WordNode;
import org.jmouse.el.node.Expression;
import org.jmouse.svg.path.node.PathData;
import org.jmouse.svg.path.parser.PathParseException;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A footprint as data a picture is painted from — its pads, its outlines and where its reference and value are printed,
 * in millimetres from its origin, Y downwards —: what a library browser or a board preview reads. It is data; how it is
 * sent is the product's.
 */
public record FootprintDrawing(List<Pad> pads, List<Outline> outlines, List<Field> fields) {

    /** A hole that carries no copper, and so no net. */
    private static final String UNPLATED_HOLE = "unplated-hole";

    /** Where a field is printed when its line names no layer. */
    private static final String SILKSCREEN = "silkscreen";

    private static final PathParser PARSER = new PathParser();

    /** The lines a footprint editor draws: every line a footprint holds. */
    private static final Set<LineGrammar> DRAWN = Set.of(FootprintLinesParser.PAD, FootprintLinesParser.OUTLINE,
                                                         FootprintLinesParser.FIELD);

    /** A line's class, as a footprint editor draws it — its word —; empty for a statement no footprint editor draws. */
    public static Optional<String> classOf(Expression statement) {
        return statement instanceof ClauseLineNode line && DRAWN.contains(line.grammar()) ? Optional.of(line.word())
                : Optional.empty();
    }

    public FootprintDrawing {
        pads = List.copyOf(pads);
        outlines = List.copyOf(outlines);
        fields = List.copyOf(fields);
    }

    /**
     * @param type  {@code through-hole}, {@code surface}, {@code connector}, {@code unplated-hole}
     * @param shape {@code circle}, {@code rectangle}, {@code oval}, {@code trapezoid}, {@code rounded-rectangle}, {@code custom}
     * @param path  a custom pad's outline; empty for any other
     */
    public record Pad(String number, String type, String shape, double x, double y, double rotation, double width,
                      double height, double drillWidth, double drillHeight, double roundness, String path) {

        public static final String SURFACE      = "surface";
        public static final String THROUGH_HOLE = "through-hole";
        public static final String CONNECTOR    = "connector";

        public boolean isSurface() {
            return SURFACE.equals(type);
        }

        public boolean isPlatedThrough() {
            return THROUGH_HOLE.equals(type);
        }

        /** A hole of no copper: drilled, never etched, joined to no net. */
        public boolean isUnplated() {
            return UNPLATED_HOLE.equals(type);
        }

        /** Whether it has copper on a layer: on every layer when it goes through the board, on its part's side when on its surface. */
        public boolean hasCopperOn(String layer, BoardDrawing.Placement placement) {
            return switch (type) {
                case THROUGH_HOLE, CONNECTOR -> true;
                case SURFACE -> placement.side().equals(layer);
                default -> false;
            };
        }
    }

    /** @param layer {@code silkscreen}, {@code fabrication}, {@code courtyard}, {@code copper} */
    public record Outline(String layer, double width, boolean filled, String path) {
    }

    /**
     * @param subject {@code reference} or {@code value}
     * @param layer   {@code silkscreen} or {@code fabrication}
     */
    public record Field(String subject, double x, double y, double height, double rotation, String layer) {
    }

    /** The drawing a footprint document's lines make. */
    public static FootprintDrawing of(DocumentNode footprint) {
        List<Pad> pads = new ArrayList<>();
        List<Outline> outlines = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        for (Expression statement : footprint.getExpressions()) {
            if (!(statement instanceof ClauseLineNode line)) {
                continue;
            }
            if (line.grammar() == FootprintLinesParser.PAD) {
                pads.add(pad(line));
            } else if (line.grammar() == FootprintLinesParser.OUTLINE) {
                outlines.add(new Outline(line.name("layer").orElseThrow(), line.quantity("width").orElse(0.0), line.has("filled"),
                        path(line)));
            } else if (line.grammar() == FootprintLinesParser.FIELD) {
                PointNode at = line.point("at").orElseThrow();
                fields.add(new Field(line.name("subject").orElseThrow(), at.x(), at.y(), line.quantity("size").orElseThrow(),
                        line.quantity("rotate").orElse(0.0), line.word("layer").orElse(SILKSCREEN)));
            }
        }
        return new FootprintDrawing(pads, outlines, fields);
    }

    /**
     * The drawing as a footprint's lines — its pads, its outlines, its fields, each in its order —, built on the grammars
     * that read them: what a footprint editor's drawing is written with. A field printed where a field is printed when
     * its line names no layer names none.
     *
     * @throws IllegalArgumentException when a path does not read as path data
     */
    public List<ClauseLineNode> lines() {
        List<ClauseLineNode> lines = new ArrayList<>();
        for (Pad pad : pads) {
            Optional<PathData> outline = pad.path() == null || pad.path().isBlank() ? Optional.empty()
                    : Optional.of(pathData("pad " + pad.number(), pad.path()));
            lines.add(FootprintLines.pad(new FootprintLines.Pad(pad.number(), pad.type(), pad.shape(), pad.x(), pad.y(),
                    pad.rotation(), pad.width(), pad.height(), pad.drillWidth(), pad.drillHeight(), pad.roundness(), outline),
                    List.of()));
        }
        for (Outline outline : outlines) {
            lines.add(FootprintLines.outline(new FootprintLines.Outline(outline.layer(), outline.width(), outline.filled(),
                    pathData("the " + outline.layer() + " outline", outline.path())), List.of()));
        }
        for (Field field : fields) {
            String layer = SILKSCREEN.equals(field.layer()) ? null : field.layer();
            lines.add(FootprintLines.field(new FootprintLines.Field(field.subject(), field.x(), field.y(), field.height(),
                    field.rotation(), layer), List.of()));
        }
        return lines;
    }

    private static PathData pathData(String origin, String written) {
        try {
            return PARSER.parse(origin, written == null ? "" : written);
        } catch (PathParseException refused) {
            throw new IllegalArgumentException("the path data of " + origin + " does not read: " + refused.detail());
        }
    }

    private static Pad pad(ClauseLineNode line) {
        PointNode at = line.point("at").orElseThrow();
        ExtentNode size = (ExtentNode) line.value("size").orElseThrow();
        double drillWidth = 0;
        double drillHeight = 0;
        Expression drill = line.value("drill").orElse(null);
        if (drill instanceof ExtentNode oblong) {
            drillWidth = oblong.width();
            drillHeight = oblong.height();
        } else if (drill != null) {
            drillWidth = line.quantity("drill").orElseThrow();
            drillHeight = drillWidth;
        }
        String number = ((WordNode) line.slot("number").orElseThrow()).word();
        return new Pad(number, line.name("type").orElseThrow(), line.name("shape").orElseThrow(), at.x(), at.y(),
                line.quantity("rotate").orElse(0.0), size.width(), size.height(), drillWidth, drillHeight,
                line.quantity("roundness").orElse(0.0), path(line));
    }

    private static String path(ClauseLineNode line) {
        return line.endingValue().map(value -> ((PathTextNode) value).path().toSource()).orElse("");
    }

    /** The numbers its copper pads carry, each once — what a component's package pin numbers must be among. */
    public Set<String> padNumbers() {
        Set<String> numbers = new LinkedHashSet<>();
        pads.stream().filter(pad -> !pad.type().equals(UNPLATED_HOLE) && !pad.number().isEmpty()).forEach(pad -> numbers.add(pad.number()));
        return numbers;
    }
}
