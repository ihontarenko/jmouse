package org.jmouse.cad.kicad;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.cad.schematic.DrawingGrammar;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.el.grammar.AssignedNode;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.node.Expression;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.svg.path.node.PathData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A KiCad symbol as a symbol document, one for each of its units, built as its tree: its lines, boxes, circles, arcs and
 * curves as path data, its pins where KiCad connects them, its reference and value where KiCad writes them.
 *
 * <p>KiCad draws a symbol in millimetres with Y upwards; a drawing is in drawing units with Y downwards. KiCad's grid,
 * 1.27 mm, is twenty units — the grid a schematic is laid out on —, so a pin on KiCad's grid is a pin on ours, and a body
 * comes out the size of the catalogue's own.</p>
 */
public final class KiCadSymbolDrawing {

    /** KiCad's 1.27 mm grid is twenty drawing units. */
    static final double UNITS_PER_MILLIMETRE = 20 / 1.27;

    /** The grid a symbol's size is rounded up to. */
    private static final double GRID = 20;

    /** A coordinate to a hundredth of a drawing unit: KiCad's nanometres are far below what a drawing shows. */
    private static final int PLACES = 2;

    /** What a pin named by its KiCad number starts with: {@code pin1}, {@code pinA}. */
    private static final String PIN_PREFIX = "pin";

    /** The radius of the bubble an inverted pin carries at the body, millimetres. */
    private static final double BUBBLE_RADIUS = 0.635;

    /** A sub-symbol's name: {@code NE5532_2_1} is unit 2 in style 1; unit 0 is every unit's, style 2 De Morgan's. */
    private static final Pattern BODY = Pattern.compile(".*_(\\d+)_(\\d+)$");
    private static final int DE_MORGAN = 2;

    /** What an outline KiCad fills with its line colour ({@code fill (type outline)}) is filled with: the drawing's ink. */
    private static final String INK = "signal";

    /**
     * What an outline KiCad fills as a body ({@code fill (type background)}) is filled with: the body fill of the drawing
     * theme — pale yellow on drafting paper —, its outline stroked over it.
     */
    private static final String BODY_FILL = "body";

    private static final String FILL = "fill";
    private static final String PATH = "path";

    private KiCadSymbolDrawing() {
    }

    /** The units a symbol draws apart, in order: {@code [1]} for a symbol of one. */
    public static List<Integer> units(KiCadSymbolDefinition symbol) {
        TreeSet<Integer> units = new TreeSet<>();
        for (KiCadList body : symbol.tree().lists("symbol")) {
            Matcher name = BODY.matcher(body.word(1));
            if (name.matches() && Integer.parseInt(name.group(1)) > 0) {
                units.add(Integer.parseInt(name.group(1)));
            }
        }
        symbol.pins().stream().filter(pin -> pin.unit() > 0).forEach(pin -> units.add(pin.unit()));
        return units.isEmpty() ? List.of(1) : List.copyOf(units);
    }

    /** The letter KiCad shows a unit by: 1 is A. */
    public static String unitLetter(int unit) {
        return String.valueOf((char) ('A' + unit - 1));
    }

    /** What a pin is called here: {@code pin} and its number, the package's name for it — {@code pin1}, {@code pinA3}. */
    public static String pinName(KiCadPin pin) {
        return PIN_PREFIX + pin.number().replaceAll("[^A-Za-z0-9]", "_");
    }

    /**
     * The KiCad number a pin named by {@link #pinName} stands for: {@code pinA} is {@code A}. A sign the name could not
     * keep was written as {@code _} and stays one.
     */
    public static Optional<String> numberOfPinName(String name) {
        return name.matches(PIN_PREFIX + "[A-Za-z0-9_]+") ? Optional.of(name.substring(PIN_PREFIX.length())) : Optional.empty();
    }

    /**
     * The slug of the component a KiCad symbol stands for when the catalogue has none yet: {@code Transistor_BJT:TIP42}
     * is {@code kicad-transistor-bjt-tip42} — what a model written for it is called.
     */
    public static String componentSlugOf(KiCadSymbolDefinition symbol) {
        // A part library of its own is named as its one part (AD5292BRUZ-100-RL7:AD5292BRUZ-100-RL7): said once.
        String library = symbol.name().contains(":") ? symbol.name().substring(0, symbol.name().indexOf(':')) : "";
        String named = library.equalsIgnoreCase(titleOf(symbol)) ? titleOf(symbol) : symbol.name();
        // A sign that opens a name is a word: power:+12V and power:-12V are two symbols.
        String signed = named.replaceAll("(^|:)\\+", "$1plus-").replaceAll("(^|:)-", "$1minus-");
        return "kicad-" + signed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    /** The KiCad library a symbol comes from: {@code Regulator_Linear} of {@code Regulator_Linear:LM317_TO-220}; empty for none. */
    public static String libraryOf(KiCadSymbolDefinition symbol) {
        int colon = symbol.name().indexOf(':');
        return colon < 0 ? "" : symbol.name().substring(0, colon);
    }

    /** What a symbol is called without its library: {@code LM317_TO-220} of {@code Regulator_Linear:LM317_TO-220}. */
    public static String titleOf(KiCadSymbolDefinition symbol) {
        return symbol.name().substring(symbol.name().indexOf(':') + 1);
    }

    /**
     * The slug of a unit's symbol, a symbol's as the catalogue's are: {@code Device:R} is {@code kicad-device-r-symbol},
     * a dual's second unit {@code …-unit-b-symbol}.
     */
    public static String slugOf(KiCadSymbolDefinition symbol, int unit) {
        String slug = componentSlugOf(symbol);
        return (units(symbol).size() > 1 ? slug + "-unit-" + unitLetter(unit).toLowerCase(Locale.ROOT) : slug) + "-symbol";
    }

    /**
     * One unit of a symbol as a symbol document: what it is called and says of itself, and its drawing.
     *
     * @param pinNames the name each pin is drawn by, by its number, where a component names it — {@code 1} is
     *                 {@code ref} on a TL431 —; a pin it does not name is {@link #pinName}
     */
    public static DocumentNode documentOf(KiCadSymbolDefinition symbol, int unit, Map<String, String> pinNames,
                                          KiCadLibraries libraries) {
        String title = titleOf(symbol) + (units(symbol).size() > 1 ? " unit " + unitLetter(unit) : "");
        String summary = firstOf(symbol.property("Description"), symbol.property("ki_description"), symbol.name())
                + ", drawn as KiCad draws it";
        DocumentNode document = new DocumentNode(SchematicModule.SYMBOL, title, slugOf(symbol, unit), true, null, 1);
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(summary)));
        meta.addExpression(MetaLineNode.of(MetaNode.CATEGORY, Nodes.word(libraries.category())));
        meta.addExpression(MetaLineNode.of(MetaNode.TAGS, Nodes.words(libraries.symbolTags())));
        meta.addExpression(MetaLineNode.of(MetaNode.SOURCE, Nodes.text(KiCadSources.symbol(symbol.name()))));
        document.addExpression(meta);
        drawing(symbol, unit, pinNames).forEach(document::addExpression);
        return document;
    }

    /** One unit of a symbol as the lines of its drawing, each pin drawn by the name {@code pinNames} gives its number. */
    private static List<Expression> drawing(KiCadSymbolDefinition symbol, int unit,
                                                               Map<String, String> pinNames) {
        double[] centre = centreOf(symbol, unit);
        Shapes shapes = new Shapes(centre[0], centre[1]);
        outline(symbol, unit, shapes);
        boolean namesShown = symbol.tree().list("pin_names").map(names -> !names.says("hide")).orElse(true);
        List<ClauseLineNode> pins = new ArrayList<>();
        for (KiCadPin pin : symbol.pinsOf(unit)) {
            shapes.pin(pin);
            double[] at = shapes.point(pin.at());
            boolean named = namesShown && !pin.name().equals(KiCadPin.NO_NAME) && !pin.name().isBlank();
            LineBuilder line = LineBuilder.line(DrawingGrammar.PIN)
                    .slot("name", Nodes.name(pinNames.getOrDefault(pin.number(), pinName(pin))))
                    .clause("at", Nodes.point(at[0], at[1]));
            if (named) {
                line.ending(Nodes.text(pin.name()));
            }
            pins.add(line.build());
        }
        List<ClauseLineNode> labels = new ArrayList<>();
        label(symbol, "Reference", "name", shapes).ifPresent(labels::add);
        label(symbol, "Value", "value", shapes).ifPresent(labels::add);
        List<Expression> lines = new ArrayList<>();
        lines.add(LineBuilder.line(DrawingGrammar.SIZE).slot("extent", Nodes.extent(shapes.width(), shapes.height())).build());
        if (!shapes.strokes.isEmpty()) {
            lines.add(element(null, shapes.strokes.build(), null));
        }
        lines.addAll(pins);
        lines.addAll(labels);
        // The bodies first, under what is filled solid: a body is the ground a part's marks stand on.
        for (int index = 0; index < shapes.bodies.size(); index++) {
            lines.add(element("body_" + (index + 1), shapes.bodies.get(index).build(), BODY_FILL));
        }
        for (int index = 0; index < shapes.fills.size(); index++) {
            lines.add(element("filled_" + (index + 1), shapes.fills.get(index).build(), INK));
        }
        return lines;
    }

    /** {@code path [name] "data" [{ fill = … }]}. */
    private static ClauseBlockNode element(String name, PathData path, String fill) {
        LineBuilder head = LineBuilder.line(DrawingGrammar.element(PATH));
        if (name != null) {
            head.slot("name", Nodes.name(name));
        }
        ClauseBlockNode element = new ClauseBlockNode(PATH, head.ending(new PathTextNode(path)).build());
        if (fill != null) {
            element.open();
            element.addExpression(LineBuilder.line(DrawingGrammar.PROPERTY).slot("name", Nodes.name(FILL))
                    .slot("value", new AssignedNode(Nodes.formula(Nodes.word(fill)))).build());
        }
        return element;
    }

    /**
     * Where a unit's body and pins are centred, in drawing units from KiCad's origin, on the grid: a drawing stands
     * around its origin, while a vendor's KiCad symbol often has its origin at a corner — at pin 1 —, which left its
     * drawing in one quarter of its own box. The drawing is moved by this, and a part placed with it by the same turned.
     */
    public static double[] centreOf(KiCadSymbolDefinition symbol, int unit) {
        Shapes measured = new Shapes(0, 0);
        outline(symbol, unit, measured);
        symbol.pinsOf(unit).forEach(pin -> {
            measured.pin(pin);
            measured.point(pin.at());
        });
        if (measured.minimumX > measured.maximumX) {
            return new double[] {0, 0};
        }
        return new double[] {Math.round((measured.minimumX + measured.maximumX) / 2 / GRID) * GRID,
                Math.round((measured.minimumY + measured.maximumY) / 2 / GRID) * GRID};
    }

    /** A unit's body — every shape of its own and of every unit's, the De Morgan alternative left out. */
    private static void outline(KiCadSymbolDefinition symbol, int unit, Shapes shapes) {
        for (KiCadList body : symbol.tree().lists("symbol")) {
            Matcher name = BODY.matcher(body.word(1));
            int bodyUnit = name.matches() ? Integer.parseInt(name.group(1)) : 0;
            int style = name.matches() ? Integer.parseInt(name.group(2)) : 1;
            if (style == DE_MORGAN || (bodyUnit != 0 && bodyUnit != unit)) {
                continue;
            }
            body.lists("polyline").forEach(shapes::polyline);
            body.lists("rectangle").forEach(shapes::rectangle);
            body.lists("circle").forEach(shapes::circle);
            body.lists("arc").forEach(shapes::arc);
            body.lists("bezier").forEach(shapes::bezier);
        }
    }

    private static Optional<ClauseLineNode> label(KiCadSymbolDefinition symbol, String property, String subject,
                                                  Shapes shapes) {
        return symbol.tree().lists("property").stream().filter(list -> list.word(1).equals(property)).findFirst()
                .flatMap(list -> list.list("at")).map(at -> {
                    double[] point = shapes.point(KiCadPoint.of(at));
                    return LineBuilder.line(DrawingGrammar.LABEL).slot("subject", Nodes.word(subject))
                            .clause("at", Nodes.point(point[0], point[1])).build();
                });
    }

    /** The paths of a unit, gathered shape by shape, and how far it reaches. */
    private static final class Shapes {

        private final RoundedPath strokes = new RoundedPath(PLACES);
        private final List<RoundedPath> fills = new ArrayList<>();
        /** The outlines KiCad fills as a part's body: filled with the theme's body fill, and stroked. */
        private final List<RoundedPath> bodies = new ArrayList<>();
        /** Where the drawing's origin stands in KiCad's, in drawing units: every point is drawn from it. */
        private final double originX;
        private final double originY;
        private double reachX;
        private double reachY;
        private double minimumX = Double.POSITIVE_INFINITY;
        private double maximumX = Double.NEGATIVE_INFINITY;
        private double minimumY = Double.POSITIVE_INFINITY;
        private double maximumY = Double.NEGATIVE_INFINITY;

        Shapes(double originX, double originY) {
            this.originX = originX;
            this.originY = originY;
        }

        void polyline(KiCadList shape) {
            List<double[]> points = points(shape);
            if (points.isEmpty()) {
                return;
            }
            RoundedPath path = target(shape);
            path.moveTo(points.getFirst()[0], points.getFirst()[1]);
            for (double[] point : points.subList(1, points.size())) {
                path.lineTo(point[0], point[1]);
            }
        }

        void rectangle(KiCadList shape) {
            double[] start = point(KiCadPoint.of(shape.requireList("start")));
            double[] end = point(KiCadPoint.of(shape.requireList("end")));
            target(shape).moveTo(start[0], start[1]).horizontalTo(end[0]).verticalTo(end[1]).horizontalTo(start[0]).close();
        }

        void circle(KiCadList shape) {
            double[] center = point(KiCadPoint.of(shape.requireList("center")));
            double radius = shape.requireList("radius").number(1) * UNITS_PER_MILLIMETRE;
            reach(center[0] - radius, center[1] - radius);
            reach(center[0] + radius, center[1] + radius);
            target(shape).circle(center[0], center[1], radius);
        }

        /** {@code (arc (start …) (mid …) (end …))}: the circle through the three, from start through mid to end. */
        void arc(KiCadList shape) {
            if (shape.list("mid").isEmpty()) {
                return;
            }
            double[] start = point(KiCadPoint.of(shape.requireList("start")));
            double[] middle = point(KiCadPoint.of(shape.requireList("mid")));
            double[] end = point(KiCadPoint.of(shape.requireList("end")));
            target(shape).arcThrough(start[0], start[1], middle[0], middle[1], end[0], end[1]);
        }

        void bezier(KiCadList shape) {
            List<double[]> points = points(shape);
            if (points.size() != 4) {
                return;
            }
            target(shape).moveTo(points.get(0)[0], points.get(0)[1]).cubicTo(points.get(1)[0], points.get(1)[1],
                    points.get(2)[0], points.get(2)[1], points.get(3)[0], points.get(3)[1]);
        }

        /** A pin's line, from where it connects to the body, and the bubble of an inverted one. */
        void pin(KiCadPin pin) {
            double[] at = point(pin.at());
            if (pin.hidden()) {
                return;
            }
            double radians = Math.toRadians(pin.angle());
            double dx = Math.cos(radians);
            double dy = Math.sin(radians);
            KiCadPoint body = new KiCadPoint(pin.at().x() + dx * pin.length(), pin.at().y() + dy * pin.length());
            if (pin.shape().startsWith("inverted")) {
                double[] shortened = point(new KiCadPoint(body.x() - dx * 2 * BUBBLE_RADIUS, body.y() - dy * 2 * BUBBLE_RADIUS));
                double[] bubble = point(new KiCadPoint(body.x() - dx * BUBBLE_RADIUS, body.y() - dy * BUBBLE_RADIUS));
                strokes.moveTo(at[0], at[1]).lineTo(shortened[0], shortened[1]);
                strokes.circle(bubble[0], bubble[1], BUBBLE_RADIUS * UNITS_PER_MILLIMETRE);
            } else {
                double[] reached = point(body);
                strokes.moveTo(at[0], at[1]).lineTo(reached[0], reached[1]);
            }
        }

        /** Where a shape is drawn: among the strokes, as a solid fill of its own, or as a body of its own. */
        private RoundedPath target(KiCadList shape) {
            String fill = shape.list("fill").flatMap(written -> written.wordOf("type")).orElse("none");
            switch (fill) {
                case "outline" -> {
                    fills.add(new RoundedPath(PLACES));
                    return fills.getLast();
                }
                case "background" -> {
                    bodies.add(new RoundedPath(PLACES));
                    return bodies.getLast();
                }
                default -> {
                    return strokes;
                }
            }
        }

        private List<double[]> points(KiCadList shape) {
            List<double[]> points = new ArrayList<>();
            shape.list("pts").ifPresent(pts -> pts.lists("xy").forEach(xy -> points.add(point(KiCadPoint.of(xy)))));
            return points;
        }

        /** A KiCad point in drawing units, Y turned downwards; the drawing's reach grows to hold it. */
        double[] point(KiCadPoint point) {
            double[] drawn = {strokes.rounded(point.x() * UNITS_PER_MILLIMETRE - originX),
                    strokes.rounded(-point.y() * UNITS_PER_MILLIMETRE - originY)};
            reach(drawn[0], drawn[1]);
            return drawn;
        }

        private void reach(double x, double y) {
            reachX = Math.max(reachX, Math.abs(x));
            reachY = Math.max(reachY, Math.abs(y));
            minimumX = Math.min(minimumX, x);
            maximumX = Math.max(maximumX, x);
            minimumY = Math.min(minimumY, y);
            maximumY = Math.max(maximumY, y);
        }

        double width() {
            return Math.max(GRID, Math.ceil(2 * reachX / GRID) * GRID);
        }

        double height() {
            return Math.max(GRID, Math.ceil(2 * reachY / GRID) * GRID);
        }
    }

    private static String firstOf(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.strip();
            }
        }
        return "";
    }
}
