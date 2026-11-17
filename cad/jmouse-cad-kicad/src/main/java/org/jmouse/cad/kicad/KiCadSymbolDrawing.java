package org.jmouse.cad.kicad;

import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.cad.exchange.imported.SymbolCanvas;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.el.node.Expression;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /** What every slug of a document converted from KiCad opens with. */
    static final String KICAD = "kicad";

    /** A sub-symbol's name: {@code NE5532_2_1} is unit 2 in style 1; unit 0 is every unit's, style 2 De Morgan's. */
    private static final Pattern BODY = Pattern.compile(".*_(\\d+)_(\\d+)$");
    private static final int DE_MORGAN = 2;

    private KiCadSymbolDrawing() {
    }

    /** The units a symbol draws apart, in order: {@code [1]} for a symbol of one. */
    public static List<Integer> units(KiCadSymbolDefinition symbol) {
        TreeSet<Integer> units = new TreeSet<>();
        for (SExpressionList body : symbol.tree().lists("symbol")) {
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

    /** What a KiCad pin is called here: by its number, as every import names a pin ({@link ImportedPins#nameOf}). */
    public static String pinName(KiCadPin pin) {
        return ImportedPins.nameOf(pin.number());
    }

    /**
     * The slug of the component a KiCad symbol stands for when the catalogue has none yet: {@code Transistor_BJT:TIP42}
     * is {@code kicad-transistor-bjt-tip42} — what a model written for it is called.
     */
    public static String componentSlugOf(KiCadSymbolDefinition symbol) {
        // A part library of its own is named as its one part (AD5292BRUZ-100-RL7:AD5292BRUZ-100-RL7): said once.
        String library = symbol.name().contains(":") ? symbol.name().substring(0, symbol.name().indexOf(':')) : "";
        return library.equalsIgnoreCase(titleOf(symbol)) ? ImportedNames.slug(KICAD, titleOf(symbol))
                : ImportedNames.slug(KICAD, library, titleOf(symbol));
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
     * The code a unit's symbol is named by, and every component drawn with it names it by: its library and name in
     * capitals — {@code Device:R} is {@code DEVICE:R:S}, a dual's second unit {@code …-UNIT-B}.
     */
    public static String codeOf(KiCadSymbolDefinition symbol, int unit) {
        return codeOf(symbol.name(), units(symbol).size() > 1 ? Optional.of(unitLetter(unit)) : Optional.empty());
    }

    /** The code of a symbol by its KiCad id — {@code Device:R} — and the letter of its unit where it has several. */
    public static String codeOf(String libraryId, Optional<String> unit) {
        int colon = libraryId.indexOf(':');
        String library = colon < 0 ? "" : libraryId.substring(0, colon);
        String name = libraryId.substring(colon + 1) + unit.map(letter -> " unit " + letter).orElse("");
        return ImportedNames.code(TechnicalClass.SYMBOL, library.isEmpty() ? KICAD : library, name);
    }

    /** The unit a symbol document is of, as its slug says it ({@link #slugOf}); empty for a symbol of one unit. */
    public static Optional<String> unitOfSlug(String slug) {
        Matcher unit = UNIT_IN_SLUG.matcher(slug);
        return unit.find() ? Optional.of(unit.group(1).toUpperCase(Locale.ROOT)) : Optional.empty();
    }

    private static final Pattern UNIT_IN_SLUG = Pattern.compile("-unit-([a-z])-symbol$");

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
     * @param installed whether the symbol is read from KiCad's installed libraries, and so is theirs
     */
    public static DocumentNode documentOf(KiCadSymbolDefinition symbol, int unit, Map<String, String> pinNames,
                                          KiCadLibraries libraries, boolean installed) {
        String title = titleOf(symbol) + (units(symbol).size() > 1 ? " unit " + unitLetter(unit) : "");
        String summary = firstOf(symbol.property("Description"), symbol.property("ki_description"), symbol.name())
                + ", drawn as KiCad draws it";
        DocumentNode document = new DocumentNode(SchematicModule.SYMBOL, title, slugOf(symbol, unit), null, 1);
        Provenance provenance = libraries.provenance(KiCadSources.symbol(symbol.name()), installed);
        MetaNode meta = MetaNode.filed(summary, codeOf(symbol, unit), libraries.categoryOf(provenance), libraries.symbolTags());
        provenance.writeInto(meta);
        document.addExpression(meta);
        drawing(symbol, unit, pinNames).forEach(document::addExpression);
        return document;
    }

    /** One unit of a symbol as the lines of its drawing, each pin drawn by the name {@code pinNames} gives its number. */
    private static List<Expression> drawing(KiCadSymbolDefinition symbol, int unit, Map<String, String> pinNames) {
        double[] centre = centreOf(symbol, unit);
        SymbolCanvas canvas = new SymbolCanvas(centre[0], centre[1]);
        outline(symbol, unit, canvas);
        boolean namesShown = symbol.tree().list("pin_names").map(names -> !names.says("hide")).orElse(true);
        for (KiCadPin pin : symbol.pinsOf(unit)) {
            pinLine(canvas, pin);
            double[] at = point(canvas, pin.at());
            boolean named = namesShown && !pin.name().equals(KiCadPin.NO_NAME) && !pin.name().isBlank();
            canvas.pin(pinNames.getOrDefault(pin.number(), pinName(pin)), at, pin.length() > 0 ? point(canvas, bodyEndOf(pin)) : null, named ? pin.name() : null);
        }
        label(symbol, "Reference", "name", canvas);
        label(symbol, "Value", "value", canvas);
        return canvas.lines();
    }

    /**
     * Where a unit's body and pins are centred, in drawing units from KiCad's origin, on the grid: a drawing stands
     * around its origin, while a vendor's KiCad symbol often has its origin at a corner — at pin 1 —, which left its
     * drawing in one quarter of its own box. The drawing is moved by this, and a part placed with it by the same turned.
     */
    public static double[] centreOf(KiCadSymbolDefinition symbol, int unit) {
        SymbolCanvas measured = new SymbolCanvas(0, 0);
        outline(symbol, unit, measured);
        symbol.pinsOf(unit).forEach(pin -> {
            pinLine(measured, pin);
            point(measured, pin.at());
        });
        return measured.centre();
    }

    /** A unit's body — every shape of its own and of every unit's, the De Morgan alternative left out. */
    private static void outline(KiCadSymbolDefinition symbol, int unit, SymbolCanvas canvas) {
        for (SExpressionList body : symbol.tree().lists("symbol")) {
            Matcher name = BODY.matcher(body.word(1));
            int bodyUnit = name.matches() ? Integer.parseInt(name.group(1)) : 0;
            int style = name.matches() ? Integer.parseInt(name.group(2)) : 1;
            if (style == DE_MORGAN || (bodyUnit != 0 && bodyUnit != unit)) {
                continue;
            }
            body.lists("polyline").forEach(shape -> polyline(canvas, shape));
            body.lists("rectangle").forEach(shape -> rectangle(canvas, shape));
            body.lists("circle").forEach(shape -> circle(canvas, shape));
            body.lists("arc").forEach(shape -> arc(canvas, shape));
            body.lists("bezier").forEach(shape -> bezier(canvas, shape));
        }
    }

    private static void label(KiCadSymbolDefinition symbol, String property, String subject, SymbolCanvas canvas) {
        symbol.tree().lists("property").stream().filter(list -> list.word(1).equals(property)).findFirst()
                .flatMap(list -> list.list("at")).ifPresent(at -> canvas.label(subject, point(canvas, KiCadPoint.of(at))));
    }

    private static void polyline(SymbolCanvas canvas, SExpressionList shape) {
        List<double[]> points = points(canvas, shape);
        if (points.isEmpty()) {
            return;
        }
        RoundedPath path = canvas.path(fillOf(shape));
        path.moveTo(points.getFirst()[0], points.getFirst()[1]);
        for (double[] point : points.subList(1, points.size())) {
            path.lineTo(point[0], point[1]);
        }
    }

    private static void rectangle(SymbolCanvas canvas, SExpressionList shape) {
        double[] start = point(canvas, KiCadPoint.of(shape.requireList("start")));
        double[] end = point(canvas, KiCadPoint.of(shape.requireList("end")));
        canvas.path(fillOf(shape)).moveTo(start[0], start[1]).horizontalTo(end[0]).verticalTo(end[1]).horizontalTo(start[0]).close();
    }

    private static void circle(SymbolCanvas canvas, SExpressionList shape) {
        double[] center = point(canvas, KiCadPoint.of(shape.requireList("center")));
        double radius = shape.requireList("radius").number(1) * SymbolCanvas.UNITS_PER_MILLIMETRE;
        canvas.reach(center[0] - radius, center[1] - radius);
        canvas.reach(center[0] + radius, center[1] + radius);
        canvas.path(fillOf(shape)).circle(center[0], center[1], radius);
    }

    /** {@code (arc (start …) (mid …) (end …))}: the circle through the three, from start through mid to end. */
    private static void arc(SymbolCanvas canvas, SExpressionList shape) {
        if (shape.list("mid").isEmpty()) {
            return;
        }
        double[] start = point(canvas, KiCadPoint.of(shape.requireList("start")));
        double[] middle = point(canvas, KiCadPoint.of(shape.requireList("mid")));
        double[] end = point(canvas, KiCadPoint.of(shape.requireList("end")));
        canvas.path(fillOf(shape)).arcThrough(start[0], start[1], middle[0], middle[1], end[0], end[1]);
    }

    private static void bezier(SymbolCanvas canvas, SExpressionList shape) {
        List<double[]> points = points(canvas, shape);
        if (points.size() != 4) {
            return;
        }
        canvas.path(fillOf(shape)).moveTo(points.get(0)[0], points.get(0)[1]).cubicTo(points.get(1)[0], points.get(1)[1],
                points.get(2)[0], points.get(2)[1], points.get(3)[0], points.get(3)[1]);
    }

    /** Where a pin's lead reaches the body. */
    private static KiCadPoint bodyEndOf(KiCadPin pin) {
        double radians = Math.toRadians(pin.angle());
        return new KiCadPoint(pin.at().x() + Math.cos(radians) * pin.length(), pin.at().y() + Math.sin(radians) * pin.length());
    }

    /** A pin's line, from where it connects to the body, and the bubble of an inverted one; nothing for a hidden one. */
    private static void pinLine(SymbolCanvas canvas, KiCadPin pin) {
        double[] at = point(canvas, pin.at());
        if (pin.hidden()) {
            return;
        }
        canvas.pinLine(at, point(canvas, bodyEndOf(pin)), pin.shape().startsWith("inverted"));
    }

    /** How KiCad fills a shape: with its line colour, solid; as a body; or not at all, a stroke. */
    private static SymbolCanvas.Fill fillOf(SExpressionList shape) {
        return switch (shape.list("fill").flatMap(written -> written.wordOf("type")).orElse("none")) {
            case "outline" -> SymbolCanvas.Fill.SOLID;
            case "background" -> SymbolCanvas.Fill.BODY;
            default -> SymbolCanvas.Fill.STROKE;
        };
    }

    private static List<double[]> points(SymbolCanvas canvas, SExpressionList shape) {
        List<double[]> points = new ArrayList<>();
        shape.list("pts").ifPresent(pts -> pts.lists("xy").forEach(xy -> points.add(point(canvas, KiCadPoint.of(xy)))));
        return points;
    }

    /** A KiCad point, millimetres with Y upwards, on the canvas. */
    private static double[] point(SymbolCanvas canvas, KiCadPoint point) {
        return canvas.point(point.x() * SymbolCanvas.UNITS_PER_MILLIMETRE, -point.y() * SymbolCanvas.UNITS_PER_MILLIMETRE);
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
