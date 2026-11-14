package org.jmouse.cad.altium;

import org.jmouse.altium.AltiumSchematic.Component;
import org.jmouse.altium.AltiumSchematic.Graphic;
import org.jmouse.altium.AltiumSchematic.Pin;
import org.jmouse.altium.AltiumSchematic.Point;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.cad.exchange.imported.SymbolCanvas;
import org.jmouse.cad.schematic.SchematicModule;

import java.util.List;

/**
 * An Altium part's drawing as a symbol document, built as its tree: its lines, polygons, rectangles, ellipses, arcs and
 * curves as path data, its pins where a wire connects them — the line back to the body, a dot for an inverted one —, its
 * designator and comment where Altium writes them. The drawing is taken from a placed part, its turn and mirror undone
 * ({@link AltiumGeometry#local}), and stands round the middle of its body ({@link #centreOf}).
 */
final class AltiumSymbolDrawing {

    /** How far a Bézier's handle stands along a quarter ellipse: 4/3 · (√2 − 1). */
    private static final double QUARTER = 0.5522847498;

    private AltiumSymbolDrawing() {
    }

    static DocumentNode documentOf(Component component, AltiumLibraries filing) {
        String reference = component.libraryReference();
        DocumentNode document = new DocumentNode(SchematicModule.SYMBOL, reference, AltiumNames.symbol(reference), null, 1);
        String said = component.description().isBlank() ? reference : component.description();
        Provenance provenance = filing.provenance(AltiumNames.symbolSource(component.sourceLibrary(), reference), false);
        MetaNode meta = MetaNode.filed(said + ", drawn as Altium draws it", AltiumNames.symbolCode(component.sourceLibrary(), reference),
                filing.categoryOf(provenance), filing.symbolTags());
        provenance.writeInto(meta);
        document.addExpression(meta);
        double[] centre = centreOf(component);
        SymbolCanvas canvas = new SymbolCanvas(centre[0], centre[1]);
        draw(component, canvas, true);
        canvas.lines().forEach(document::addExpression);
        return document;
    }

    /** Where the part's body and pins are centred, in drawing units from its location, Y downwards, on the grid. */
    static double[] centreOf(Component component) {
        SymbolCanvas measured = new SymbolCanvas(0, 0);
        draw(component, measured, false);
        return measured.centre();
    }

    private static void draw(Component component, SymbolCanvas canvas, boolean named) {
        for (Graphic graphic : component.graphics()) {
            switch (graphic) {
                case Graphic.Polyline polyline -> run(component, canvas, canvas.path(SymbolCanvas.Fill.STROKE), polyline.points(), false);
                case Graphic.Polygon polygon -> run(component, canvas,
                        canvas.path(polygon.solid() ? SymbolCanvas.Fill.SOLID : SymbolCanvas.Fill.STROKE), polygon.points(), true);
                case Graphic.Rectangle rectangle -> {
                    Point corner = rectangle.corner();
                    Point opposite = rectangle.opposite();
                    run(component, canvas, canvas.path(rectangle.solid() ? SymbolCanvas.Fill.BODY : SymbolCanvas.Fill.STROKE),
                            List.of(corner, new Point(opposite.x(), corner.y()), opposite, new Point(corner.x(), opposite.y())), true);
                }
                case Graphic.Ellipse ellipse -> ellipse(component, canvas, ellipse);
                case Graphic.Arc arc -> arc(component, canvas, arc);
                case Graphic.Bezier bezier -> bezier(component, canvas, bezier.points());
            }
        }
        for (Pin pin : component.pins()) {
            double[] end = point(component, canvas, pin.end());
            double[] body = point(component, canvas, pin.location());
            if (!pin.hidden()) {
                canvas.pinLine(end, body, pin.inverted());
            }
            if (named) {
                canvas.pin(ImportedPins.nameOf(pin.designator()), end, body, pin.nameShown() && !pin.hidden() ? pin.name() : null);
            }
        }
        if (named) {
            component.designatorAt().ifPresent(at -> canvas.label("name", point(component, canvas, at)));
            component.commentAt().ifPresent(at -> canvas.label("value", point(component, canvas, at)));
        }
    }

    /** A run of straight stretches through points of the sheet, closed for an area. */
    private static void run(Component component, SymbolCanvas canvas, RoundedPath path, List<Point> points, boolean closed) {
        if (points.size() < 2) {
            return;
        }
        double[] first = point(component, canvas, points.getFirst());
        path.moveTo(first[0], first[1]);
        for (Point point : points.subList(1, points.size())) {
            double[] at = point(component, canvas, point);
            path.lineTo(at[0], at[1]);
        }
        if (closed) {
            path.close();
        }
    }

    /** An ellipse as four quarter curves through the ends of its axes — a circle where its radii agree. */
    private static void ellipse(Component component, SymbolCanvas canvas, Graphic.Ellipse ellipse) {
        double x = ellipse.centre().x();
        double y = ellipse.centre().y();
        double across = ellipse.radius();
        double up = ellipse.secondaryRadius();
        RoundedPath path = canvas.path(ellipse.solid() ? SymbolCanvas.Fill.SOLID : SymbolCanvas.Fill.STROKE);
        double[][] ends = {{x + across, y}, {x, y + up}, {x - across, y}, {x, y - up}};
        double[][] handles = {{0, QUARTER * up}, {-QUARTER * across, 0}, {0, -QUARTER * up}, {QUARTER * across, 0}};
        double[] start = point(component, canvas, new Point(ends[0][0], ends[0][1]));
        path.moveTo(start[0], start[1]);
        for (int quarter = 0; quarter < 4; quarter++) {
            double[] from = ends[quarter];
            double[] to = ends[(quarter + 1) % 4];
            double[] leaving = handles[quarter];
            double[] arriving = handles[(quarter + 1) % 4];
            double[] first = point(component, canvas, new Point(from[0] + leaving[0], from[1] + leaving[1]));
            double[] second = point(component, canvas, new Point(to[0] - arriving[0], to[1] - arriving[1]));
            double[] end = point(component, canvas, new Point(to[0], to[1]));
            path.cubicTo(first[0], first[1], second[0], second[1], end[0], end[1]);
        }
        path.close();
    }

    /** An arc counter-clockwise from its start angle to its end, through its middle. */
    private static void arc(Component component, SymbolCanvas canvas, Graphic.Arc arc) {
        double start = arc.startAngle();
        double end = arc.endAngle() <= start ? arc.endAngle() + 360 : arc.endAngle();
        double[] from = point(component, canvas, onCircle(arc, start));
        double[] through = point(component, canvas, onCircle(arc, (start + end) / 2));
        double[] to = point(component, canvas, onCircle(arc, end));
        canvas.path(SymbolCanvas.Fill.STROKE).arcThrough(from[0], from[1], through[0], through[1], to[0], to[1]);
    }

    private static Point onCircle(Graphic.Arc arc, double degrees) {
        double radians = Math.toRadians(degrees);
        return new Point(arc.centre().x() + arc.radius() * Math.cos(radians), arc.centre().y() + arc.radius() * Math.sin(radians));
    }

    /** A run of cubic curves sharing their ends: the first point, then three for each curve. */
    private static void bezier(Component component, SymbolCanvas canvas, List<Point> points) {
        if (points.size() < 4) {
            return;
        }
        RoundedPath path = canvas.path(SymbolCanvas.Fill.STROKE);
        double[] start = point(component, canvas, points.getFirst());
        path.moveTo(start[0], start[1]);
        for (int index = 1; index + 2 < points.size(); index += 3) {
            double[] first = point(component, canvas, points.get(index));
            double[] second = point(component, canvas, points.get(index + 1));
            double[] end = point(component, canvas, points.get(index + 2));
            path.cubicTo(first[0], first[1], second[0], second[1], end[0], end[1]);
        }
    }

    /** A point of the sheet on the canvas: in the part's own frame, in drawing units, Y downwards. */
    private static double[] point(Component component, SymbolCanvas canvas, Point onSheet) {
        Point local = AltiumGeometry.local(component, onSheet);
        return canvas.point(local.x() * AltiumGeometry.UNITS, -local.y() * AltiumGeometry.UNITS);
    }
}
