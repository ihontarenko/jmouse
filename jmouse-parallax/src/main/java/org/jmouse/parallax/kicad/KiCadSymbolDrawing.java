package org.jmouse.parallax.kicad;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.script.Block.SymbolMotion;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.script.engine.SymbolDrawing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A KiCad symbol as a jMX symbol: one {@link SymbolDrawing} for each of its units — its lines, boxes, circles, arcs and
 * curves as path data, its pins where KiCad connects them, its reference and value where KiCad writes them —, and the
 * symbol document jMX's own writer makes of it.
 *
 * <p>KiCad draws a symbol in millimetres with Y upwards; jMX in drawing units with Y downwards. KiCad's grid, 1.27 mm,
 * is twenty units — the grid a jMX schematic is laid out on —, so a pin on KiCad's grid is a pin on ours, and a body
 * comes out the size of the catalogue's own.</p>
 */
public final class KiCadSymbolDrawing {

    /** KiCad's 1.27 mm grid is twenty drawing units. */
    static final double UNITS_PER_MILLIMETRE = 20 / 1.27;

    /** The grid a symbol's size is rounded up to. */
    private static final double GRID = 20;

    /** The radius of the bubble an inverted pin carries at the body, millimetres. */
    private static final double BUBBLE_RADIUS = 0.635;

    /** A sub-symbol's name: {@code NE5532_2_1} is unit 2 in style 1; unit 0 is every unit's, style 2 De Morgan's. */
    private static final Pattern BODY = Pattern.compile(".*_(\\d+)_(\\d+)$");
    private static final int DE_MORGAN = 2;

    /** What an outline KiCad fills is filled with: the colour the drawing's lines take. */
    private static final SymbolDrawing.Value FILLED = new SymbolDrawing.Value("signal", List.of("signal"), SymbolMotion.STILL);

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

    /** What a pin is called in jMX: {@code pin} and its number, the package's name for it — {@code pin1}, {@code pinA3}. */
    public static String pinName(KiCadPin pin) {
        return "pin" + pin.number().replaceAll("[^A-Za-z0-9]", "_");
    }

    /** A slug for a KiCad library id and unit: {@code Device:R} is {@code kicad-device-r}, a dual's second {@code …-unit-b}. */
    public static String slugOf(KiCadSymbolDefinition symbol, int unit) {
        // A sign that opens a name is a word: power:+12V and power:-12V are two symbols.
        String signed = symbol.name().replaceAll("(^|:)\\+", "$1plus-").replaceAll("(^|:)-", "$1minus-");
        String slug = "kicad-" + signed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return units(symbol).size() > 1 ? slug + "-unit-" + unitLetter(unit).toLowerCase(Locale.ROOT) : slug;
    }

    /** One unit of a symbol as the jMX symbol document jMX's writer makes of it. */
    public static String document(KiCadSymbolDefinition symbol, int unit) {
        return ExperienceDefinitions.write(object(symbol, unit));
    }

    /** One unit of a symbol as a jMX symbol object. */
    public static DefinedObject object(KiCadSymbolDefinition symbol, int unit) {
        String title = symbol.name() + (units(symbol).size() > 1 ? " unit " + unitLetter(unit) : "");
        String summary = firstOf(symbol.property("Description"), symbol.property("ki_description"), symbol.name())
                + ", drawn as KiCad draws it";
        return new DefinedObject(slugOf(symbol, unit), title, summary, null, "circuits",
                List.of("electronics", "schematic", "kicad"), false, null,
                new ObjectDefinition.Symbol(drawing(symbol, unit).toJson()), List.of(), List.of(), List.of(), List.of(),
                List.of());
    }

    /** One unit of a symbol as a drawing. */
    public static SymbolDrawing drawing(KiCadSymbolDefinition symbol, int unit) {
        Shapes shapes = new Shapes();
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
        boolean namesShown = symbol.tree().list("pin_names").map(names -> !names.says("hide")).orElse(true);
        List<SymbolDrawing.Pin> pins = new ArrayList<>();
        for (KiCadPin pin : symbol.pinsOf(unit)) {
            shapes.pin(pin);
            double[] at = shapes.point(pin.at());
            boolean named = namesShown && !pin.name().equals(KiCadPin.NO_NAME) && !pin.name().isBlank();
            pins.add(new SymbolDrawing.Pin(pinName(pin), at[0], at[1], named ? pin.name() : ""));
        }
        List<SymbolDrawing.Label> labels = new ArrayList<>();
        label(symbol, "Reference", "name", shapes).ifPresent(labels::add);
        label(symbol, "Value", "value", shapes).ifPresent(labels::add);
        List<String> paths = shapes.strokes.isEmpty() ? List.of() : List.of(String.join("", shapes.strokes));
        List<SymbolDrawing.Element> elements = new ArrayList<>();
        for (int index = 0; index < shapes.fills.size(); index++) {
            elements.add(new SymbolDrawing.Element("path", "filled_" + (index + 1), false, shapes.fills.get(index),
                    Map.of(), null, Map.of("fill", FILLED), List.of(), List.of()));
        }
        return new SymbolDrawing(shapes.width(), shapes.height(), paths, pins, labels, List.of(), List.of(), List.of(),
                elements);
    }

    private static Optional<SymbolDrawing.Label> label(KiCadSymbolDefinition symbol, String property, String subject,
                                                       Shapes shapes) {
        return symbol.tree().lists("property").stream().filter(list -> list.word(1).equals(property)).findFirst()
                .flatMap(list -> list.list("at")).map(at -> {
                    double[] point = shapes.point(KiCadPoint.of(at));
                    return new SymbolDrawing.Label(subject, rounded(point[0]), rounded(point[1]));
                });
    }

    /** The path data of a unit, gathered shape by shape, and how far it reaches. */
    private static final class Shapes {

        private final List<String> strokes = new ArrayList<>();
        private final List<String> fills = new ArrayList<>();
        private double reachX;
        private double reachY;

        void polyline(KiCadList shape) {
            List<double[]> points = points(shape);
            if (points.isEmpty()) {
                return;
            }
            StringBuilder path = new StringBuilder("M").append(pair(points.getFirst()));
            for (double[] point : points.subList(1, points.size())) {
                path.append('L').append(pair(point));
            }
            add(shape, path.toString());
        }

        void rectangle(KiCadList shape) {
            double[] start = point(KiCadPoint.of(shape.requireList("start")));
            double[] end = point(KiCadPoint.of(shape.requireList("end")));
            add(shape, "M" + pair(start) + "H" + number(end[0]) + "V" + number(end[1]) + "H" + number(start[0]) + "Z");
        }

        void circle(KiCadList shape) {
            double[] center = point(KiCadPoint.of(shape.requireList("center")));
            double radius = shape.requireList("radius").number(1) * UNITS_PER_MILLIMETRE;
            reach(center[0] - radius, center[1] - radius);
            reach(center[0] + radius, center[1] + radius);
            add(shape, circlePath(center, radius));
        }

        /** {@code (arc (start …) (mid …) (end …))}: the circle through the three, from start through mid to end. */
        void arc(KiCadList shape) {
            if (shape.list("mid").isEmpty()) {
                return;
            }
            double[] start = point(KiCadPoint.of(shape.requireList("start")));
            double[] middle = point(KiCadPoint.of(shape.requireList("mid")));
            double[] end = point(KiCadPoint.of(shape.requireList("end")));
            double[] center = center(start, middle, end);
            if (center == null) {
                add(shape, "M" + pair(start) + "L" + pair(end));
                return;
            }
            double radius = Math.hypot(start[0] - center[0], start[1] - center[1]);
            double turn = (middle[0] - start[0]) * (end[1] - start[1]) - (middle[1] - start[1]) * (end[0] - start[0]);
            boolean clockwise = turn < 0;
            double from = Math.atan2(start[1] - center[1], start[0] - center[0]);
            double to = Math.atan2(end[1] - center[1], end[0] - center[0]);
            double span = clockwise ? to - from : from - to;
            span = ((span % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
            String flags = (span > Math.PI ? "1 " : "0 ") + (clockwise ? "1 " : "0 ");
            add(shape, "M" + pair(start) + "A" + number(radius) + " " + number(radius) + " 0 " + flags + pair(end));
        }

        void bezier(KiCadList shape) {
            List<double[]> points = points(shape);
            if (points.size() != 4) {
                return;
            }
            add(shape, "M" + pair(points.get(0)) + "C" + pair(points.get(1)) + " " + pair(points.get(2)) + " "
                    + pair(points.get(3)));
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
                KiCadPoint shortened = new KiCadPoint(body.x() - dx * 2 * BUBBLE_RADIUS, body.y() - dy * 2 * BUBBLE_RADIUS);
                KiCadPoint bubble = new KiCadPoint(body.x() - dx * BUBBLE_RADIUS, body.y() - dy * BUBBLE_RADIUS);
                strokes.add("M" + pair(at) + "L" + pair(point(shortened)));
                strokes.add(circlePath(point(bubble), BUBBLE_RADIUS * UNITS_PER_MILLIMETRE));
            } else {
                strokes.add("M" + pair(at) + "L" + pair(point(body)));
            }
        }

        private void add(KiCadList shape, String path) {
            boolean filled = shape.list("fill").flatMap(fill -> fill.wordOf("type")).filter("outline"::equals).isPresent();
            (filled ? fills : strokes).add(path);
        }

        private List<double[]> points(KiCadList shape) {
            List<double[]> points = new ArrayList<>();
            shape.list("pts").ifPresent(pts -> pts.lists("xy").forEach(xy -> points.add(point(KiCadPoint.of(xy)))));
            return points;
        }

        /** A KiCad point in drawing units, Y turned downwards; the drawing's reach grows to hold it. */
        double[] point(KiCadPoint point) {
            double[] drawn = {rounded(point.x() * UNITS_PER_MILLIMETRE), rounded(-point.y() * UNITS_PER_MILLIMETRE)};
            reach(drawn[0], drawn[1]);
            return drawn;
        }

        private void reach(double x, double y) {
            reachX = Math.max(reachX, Math.abs(x));
            reachY = Math.max(reachY, Math.abs(y));
        }

        double width() {
            return Math.max(GRID, Math.ceil(2 * reachX / GRID) * GRID);
        }

        double height() {
            return Math.max(GRID, Math.ceil(2 * reachY / GRID) * GRID);
        }
    }

    private static String circlePath(double[] center, double radius) {
        String r = number(radius);
        return "M" + number(center[0] - radius) + " " + number(center[1]) + "A" + r + " " + r + " 0 1 0 "
                + number(center[0] + radius) + " " + number(center[1]) + "A" + r + " " + r + " 0 1 0 "
                + number(center[0] - radius) + " " + number(center[1]) + "Z";
    }

    /** The centre of the circle through three points; null when they lie on a line. */
    private static double[] center(double[] a, double[] b, double[] c) {
        double determinant = 2 * (a[0] * (b[1] - c[1]) + b[0] * (c[1] - a[1]) + c[0] * (a[1] - b[1]));
        if (Math.abs(determinant) < 1e-9) {
            return null;
        }
        double a2 = a[0] * a[0] + a[1] * a[1];
        double b2 = b[0] * b[0] + b[1] * b[1];
        double c2 = c[0] * c[0] + c[1] * c[1];
        return new double[] {
                (a2 * (b[1] - c[1]) + b2 * (c[1] - a[1]) + c2 * (a[1] - b[1])) / determinant,
                (a2 * (c[0] - b[0]) + b2 * (a[0] - c[0]) + c2 * (b[0] - a[0])) / determinant};
    }

    private static String pair(double[] point) {
        return number(point[0]) + " " + number(point[1]);
    }

    /** A coordinate to a hundredth of a drawing unit: KiCad's nanometres are far below what a drawing shows. */
    private static double rounded(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN).doubleValue() + 0.0;
    }

    /** A coordinate of path data, as short as it reads back. */
    private static String number(double value) {
        return BigDecimal.valueOf(rounded(value)).stripTrailingZeros().toPlainString();
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
