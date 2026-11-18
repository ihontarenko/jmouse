package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.PcbLayers;
import org.jspecify.annotations.Nullable;
import org.jmouse.cad.pcb.PcbWords;
import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.FootprintLinesParser;
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
 * in millimetres from its origin, Y downwards —, and its package's body when the footprint says how high it stands: what
 * a library browser, a pcb preview or a 3D view reads. It is data; how it is sent is the product's.
 *
 * <p>The body is not a drawn line: a footprint editor neither draws nor rewrites it ({@link #classOf}), so its line is kept as
 * written while the rest is redrawn. Its models it does set, so they are written with the pads and outlines.</p>
 *
 * @param pads     its pads, in their order
 * @param outlines the lines and areas drawn on its layers
 * @param fields   where its reference and value are printed
 * @param body     the package's body; null when the footprint does not say
 * @param models   the 3D models that show the body, in their order; none when the footprint names none
 */
public record FootprintDrawing(List<Pad> pads, List<Outline> outlines, List<Field> fields, @Nullable Body body, List<Model> models) {

    /** A hole that carries no copper, and so no net. */
    private static final String UNPLATED_HOLE = "unplated-hole";

    /** Where a field is printed when its line names no layer. */
    private static final String SILKSCREEN = "silkscreen";

    private static final PathParser PARSER = new PathParser();

    /** The lines a footprint editor draws: every line a footprint holds. */
    private static final Set<LineGrammar> DRAWN = Set.of(FootprintLinesParser.PAD, FootprintLinesParser.OUTLINE,
                                                         FootprintLinesParser.FIELD, FootprintLinesParser.MODEL);

    /** A line's class, as a footprint editor draws it — its word —; empty for a statement no footprint editor draws. */
    public static Optional<String> classOf(Expression statement) {
        return statement instanceof ClauseLineNode line && DRAWN.contains(line.grammar()) ? Optional.of(line.word())
                : Optional.empty();
    }

    /** A footprint without fields or models — a drawing that writes none — has none: the lists are optional where it is written. */
    public FootprintDrawing {
        pads = List.copyOf(pads);
        outlines = List.copyOf(outlines);
        fields = fields == null ? List.of() : List.copyOf(fields);
        models = models == null ? List.of() : List.copyOf(models);
    }

    /**
     * A pad: where a part's pin meets the copper, or a hole drilled for it.
     *
     * @param number      the pin it is, as the part numbers its pins
     * @param type        {@code through-hole}, {@code surface}, {@code connector}, {@code unplated-hole}
     * @param shape       {@code circle}, {@code rectangle}, {@code oval}, {@code trapezoid}, {@code rounded-rectangle}, {@code custom}
     * @param x           where its middle stands, millimetres right of the footprint's origin
     * @param y           where its middle stands, millimetres below the footprint's origin
     * @param rotation    how far it is turned, degrees
     * @param width       how wide it is, millimetres
     * @param height      how tall it is, millimetres
     * @param drillWidth  its hole's width, millimetres; 0 for a pad with no hole
     * @param drillHeight its hole's height, millimetres — taller than wide for a slot —; 0 for a pad with no hole
     * @param roundness   a rounded rectangle's corner radius, as a share of its shorter side; 0 for any other shape
     * @param path        a custom pad's outline; empty for any other
     * @param face        the side of the pcb a surface pad is on, from its part's: {@code near} — the usual — or {@code far}; empty for near
     * @param opens       what it leaves open over its copper: {@code both}, {@code mask}, {@code paste} or {@code none}; empty for
     *                    what its type usually does
     * @param maskMargin  how far the solder mask stands back from its copper, millimetres; null for the pcb's own
     * @param pasteMargin how far the solder paste stands back from its copper, millimetres; null for the pcb's own
     */
    public record Pad(String number, String type, String shape, double x, double y, double rotation, double width,
                      double height, double drillWidth, double drillHeight, double roundness, String path,
                      String face, String opens, @Nullable Double maskMargin, @Nullable Double pasteMargin) {

        /** Whether it leaves the solder mask open: every pad of copper, unless it says otherwise. */
        public boolean opensMask() {
            return opens == null || opens.isEmpty() ? !isUnplated() : PcbVocabulary.OPENS_BOTH.equals(opens) || PcbVocabulary.OPENS_MASK.equals(opens);
        }

        /** Whether it takes solder paste: a surface pad, unless it says otherwise. */
        public boolean opensPaste() {
            return opens == null || opens.isEmpty() ? isSurface() : PcbVocabulary.OPENS_BOTH.equals(opens) || PcbVocabulary.OPENS_PASTE.equals(opens);
        }

        public static final String SURFACE      = "surface";
        public static final String THROUGH_HOLE = "through-hole";
        public static final String CONNECTOR    = "connector";

        /** Whether it is a surface pad: copper on its part's side alone, no hole. */
        public boolean isSurface() {
            return SURFACE.equals(type);
        }

        /** Whether it is a plated hole: copper on every layer, through the pcb. */
        public boolean isPlatedThrough() {
            return THROUGH_HOLE.equals(type);
        }

        /** A hole of no copper: drilled, never etched, joined to no net. */
        public boolean isUnplated() {
            return UNPLATED_HOLE.equals(type);
        }

        /** Whether it has copper on a layer: on every layer when it goes through the pcb, on its part's side when on its surface. */
        public boolean hasCopperOn(String layer, Placement placement) {
            return switch (type) {
                case THROUGH_HOLE, CONNECTOR -> true;
                case SURFACE -> (PcbVocabulary.FACE_FAR.equals(face) ? PcbLayers.otherSide(placement.side()) : placement.side()).equals(layer);
                default -> false;
            };
        }
    }

    /**
     * A line or an area drawn on one of the footprint's layers.
     *
     * @param layer  {@code silkscreen}, {@code fabrication}, {@code courtyard}, {@code copper}
     * @param width  what its lines are stroked with, millimetres; 0 for an area filled alone
     * @param filled whether it is an area rather than lines
     * @param path   what it draws, as path data in the footprint's millimetres
     */
    public record Outline(String layer, double width, boolean filled, String path) {
    }

    /**
     * Where a placed part's reference or value is printed, in the footprint's own frame.
     *
     * @param subject  {@code reference} or {@code value}
     * @param x        where it stands, millimetres right of the footprint's origin
     * @param y        where it stands, millimetres below the footprint's origin
     * @param height   how tall its letters are, millimetres
     * @param rotation how far it is turned, degrees
     * @param layer    {@code silkscreen} or {@code fabrication}
     * @param anchor   which part of the letters stands on the point: {@code start}, {@code middle} — the usual — or {@code end}
     */
    public record Field(String subject, double x, double y, double height, double rotation, String layer, String anchor) {
    }

    /**
     * A package's body over its footprint, its outline the fabrication layer's.
     *
     * @param height millimetres above the pcb
     */
    public record Body(double height) {
    }

    /**
     * A 3D model set onto the footprint, as {@link FootprintLinesParser#MODEL} places it.
     *
     * @param reference the model document's slug
     * @param offset    millimetres: x, y, z
     * @param rotation  degrees about each axis: x, y, z
     * @param scale     each axis' factor: x, y, z
     * @param hidden    whether it is kept but not shown
     */
    public record Model(String reference, List<Double> offset, List<Double> rotation, List<Double> scale, boolean hidden) {

        private static final List<Double> ZERO = List.of(0.0, 0.0, 0.0);
        private static final List<Double> ONE = List.of(1.0, 1.0, 1.0);

        static Model of(ClauseLineNode line) {
            return new Model(((WordNode) line.slot(PcbWords.REFERENCE).orElseThrow()).word(), axes(line, "offset", ZERO),
                    axes(line, "rotate", ZERO), axes(line, "scale", ONE), line.has(PcbWords.HIDDEN));
        }

        private static List<Double> axes(ClauseLineNode line, String clause, List<Double> unwritten) {
            List<Double> written = line.quantities(clause);
            return written.isEmpty() ? unwritten : written;
        }
    }

    /** The drawing a footprint document's lines make. */
    public static FootprintDrawing of(DocumentNode footprint) {
        List<Pad> pads = new ArrayList<>();
        List<Outline> outlines = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        Body body = null;
        List<Model> models = new ArrayList<>();
        for (Expression statement : footprint.getExpressions()) {
            if (!(statement instanceof ClauseLineNode line)) {
                continue;
            }
            if (line.grammar() == FootprintLinesParser.PAD) {
                pads.add(pad(line));
            } else if (line.grammar() == FootprintLinesParser.OUTLINE) {
                outlines.add(new Outline(line.name(PcbWords.LAYER).orElseThrow(), line.quantity(PcbWords.WIDTH).orElse(0.0), line.has(PcbWords.FILLED),
                        path(line)));
            } else if (line.grammar() == FootprintLinesParser.FIELD) {
                PointNode at = line.point(PcbWords.AT).orElseThrow();
                fields.add(new Field(line.name(PcbWords.SUBJECT).orElseThrow(), at.x(), at.y(), line.quantity(PcbWords.SIZE).orElseThrow(),
                        line.quantity(PcbWords.ROTATE).orElse(0.0), line.word(PcbWords.LAYER).orElse(SILKSCREEN),
                        line.word(PcbWords.ANCHOR).orElse(PcbVocabulary.TEXT_MIDDLE)));
            } else if (line.grammar() == FootprintLinesParser.BODY) {
                body = new Body(line.quantity(PcbWords.HEIGHT).orElseThrow());
            } else if (line.grammar() == FootprintLinesParser.MODEL) {
                models.add(Model.of(line));
            }
        }
        return new FootprintDrawing(pads, outlines, fields, body, models);
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
                    pad.rotation(), pad.width(), pad.height(), pad.drillWidth(), pad.drillHeight(), pad.roundness(), outline,
                    pad.face(), pad.opens(), pad.maskMargin(), pad.pasteMargin()),
                    List.of()));
        }
        for (Outline outline : outlines) {
            lines.add(FootprintLines.outline(new FootprintLines.Outline(outline.layer(), outline.width(), outline.filled(),
                    pathData("the " + outline.layer() + " outline", outline.path())), List.of()));
        }
        for (Field field : fields) {
            String layer = SILKSCREEN.equals(field.layer()) ? null : field.layer();
            lines.add(FootprintLines.field(new FootprintLines.Field(field.subject(), field.x(), field.y(), field.height(),
                    field.rotation(), layer, field.anchor()), List.of()));
        }
        for (Model model : models) {
            lines.add(FootprintLines.model(new FootprintLines.Model(model.reference(), axes(model.offset()), axes(model.rotation()), axes(model.scale()),
                    model.hidden()), List.of()));
        }
        return lines;
    }

    private static FootprintLines.Axes axes(List<Double> values) {
        return new FootprintLines.Axes(values.get(0), values.get(1), values.get(2));
    }

    private static PathData pathData(String origin, String written) {
        try {
            return PARSER.parse(origin, written == null ? "" : written);
        } catch (PathParseException refused) {
            throw new IllegalArgumentException("the path data of " + origin + " does not read: " + refused.detail());
        }
    }

    private static Pad pad(ClauseLineNode line) {
        PointNode at = line.point(PcbWords.AT).orElseThrow();
        ExtentNode size = (ExtentNode) line.value("size").orElseThrow();
        double drillWidth = 0;
        double drillHeight = 0;
        Expression drill = line.value("drill").orElse(null);
        if (drill instanceof ExtentNode oblong) {
            drillWidth = oblong.width();
            drillHeight = oblong.height();
        } else if (drill != null) {
            drillWidth = line.quantity(PcbWords.DRILL).orElseThrow();
            drillHeight = drillWidth;
        }
        String number = ((WordNode) line.slot(PcbWords.NUMBER).orElseThrow()).word();
        return new Pad(number, line.name(PcbWords.TYPE).orElseThrow(), line.name(PcbWords.SHAPE).orElseThrow(), at.x(), at.y(),
                line.quantity(PcbWords.ROTATE).orElse(0.0), size.width(), size.height(), drillWidth, drillHeight,
                line.quantity(PcbWords.ROUNDNESS).orElse(0.0), path(line), line.word(PcbWords.FACE).orElse(""), line.word(PcbWords.OPENS).orElse(""),
                line.quantity(PcbWords.MASK_MARGIN).orElse(null), line.quantity(PcbWords.PASTE_MARGIN).orElse(null));
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
