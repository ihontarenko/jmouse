package org.jmouse.kicad.footprint;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadKeyword;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * A line drawn on a layer — a footprint's silkscreen, its fabrication outline, its courtyard; a board's own edge —, in
 * millimetres, Y downwards.
 */
public sealed interface KiCadFootprintGraphic {

    /** The layer it is drawn on: {@code F.SilkS}, {@code F.Fab}, {@code F.CrtYd} … */
    String layer();

    /** The stroke's width; 0 when KiCad writes none. */
    double width();

    /** Whether its inside is filled — a solid polygon, a filled rectangle. */
    boolean filled();

    record Line(KiCadPoint start, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {
    }

    record Rectangle(KiCadPoint start, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {
    }

    /** A circle through {@code end}, around {@code center}. */
    record Circle(KiCadPoint center, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {

        public double radius() {
            return Math.hypot(end.x() - center.x(), end.y() - center.y());
        }
    }

    /** An arc from {@code start} through {@code middle} to {@code end}. */
    record Arc(KiCadPoint start, KiCadPoint middle, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {
    }

    record Polygon(List<KiCadPoint> points, String layer, double width, boolean filled) implements KiCadFootprintGraphic {
        public Polygon {
            points = List.copyOf(points);
        }
    }

    /** A cubic Bézier curve from {@code start} to {@code end}, pulled by its two handles, as KiCad 7 and newer draw one. */
    record Curve(KiCadPoint start, KiCadPoint first, KiCadPoint second, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {
    }

    /**
     * The points that hold it between them, exactly: its ends and corners, and where an arc, a circle or a curve reaches
     * furthest left, right, up and down — a board's frame is where its edge reaches, wherever the board is read or written.
     */
    default List<KiCadPoint> reach() {
        return switch (this) {
            case Line line -> List.of(line.start(), line.end());
            case Rectangle box -> List.of(box.start(), box.end());
            case Circle circle -> List.of(new KiCadPoint(circle.center().x() - circle.radius(), circle.center().y() - circle.radius()),
                    new KiCadPoint(circle.center().x() + circle.radius(), circle.center().y() + circle.radius()));
            case Arc arc -> arcReach(arc);
            case Polygon polygon -> polygon.points();
            case Curve curve -> curveReach(curve);
        };
    }

    /** An arc's ends, and each point due left, right, up or down of its centre that it passes on its way through its middle. */
    private static List<KiCadPoint> arcReach(Arc arc) {
        List<KiCadPoint> points = new ArrayList<>(List.of(arc.start(), arc.end()));
        double ax = arc.start().x();
        double ay = arc.start().y();
        double bx = arc.middle().x();
        double by = arc.middle().y();
        double cx = arc.end().x();
        double cy = arc.end().y();
        double determinant = 2 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by));
        if (Math.abs(determinant) < 1e-12) {
            return points;
        }
        double centreX = ((ax * ax + ay * ay) * (by - cy) + (bx * bx + by * by) * (cy - ay) + (cx * cx + cy * cy) * (ay - by)) / determinant;
        double centreY = ((ax * ax + ay * ay) * (cx - bx) + (bx * bx + by * by) * (ax - cx) + (cx * cx + cy * cy) * (bx - ax)) / determinant;
        double radius = Math.hypot(ax - centreX, ay - centreY);
        double start = Math.atan2(ay - centreY, ax - centreX);
        double sweep = turn(Math.atan2(cy - centreY, cx - centreX) - start);
        // The way round that passes the middle: the other way when going this way would miss it.
        if (turn(Math.atan2(by - centreY, bx - centreX) - start) > sweep) {
            sweep -= 2 * Math.PI;
        }
        for (int quarter = 0; quarter < 4; quarter++) {
            double angle = quarter * Math.PI / 2;
            double along = sweep >= 0 ? turn(angle - start) : turn(start - angle);
            if (along <= Math.abs(sweep)) {
                points.add(new KiCadPoint(centreX + radius * Math.cos(angle), centreY + radius * Math.sin(angle)));
            }
        }
        return points;
    }

    /** A curve's ends, and where it turns back along either axis: where its derivative is nought between its ends. */
    private static List<KiCadPoint> curveReach(Curve curve) {
        List<KiCadPoint> points = new ArrayList<>(List.of(curve.start(), curve.end()));
        double[] xs = {curve.start().x(), curve.first().x(), curve.second().x(), curve.end().x()};
        double[] ys = {curve.start().y(), curve.first().y(), curve.second().y(), curve.end().y()};
        for (double[] axis : List.of(xs, ys)) {
            // The derivative of a cubic is a quadratic a·t² + b·t + c.
            double a = -axis[0] + 3 * axis[1] - 3 * axis[2] + axis[3];
            double b = 2 * (axis[0] - 2 * axis[1] + axis[2]);
            double c = axis[1] - axis[0];
            for (double t : roots(3 * a, 3 * b, 3 * c)) {
                if (t > 0 && t < 1) {
                    double u = 1 - t;
                    points.add(new KiCadPoint(u * u * u * xs[0] + 3 * u * u * t * xs[1] + 3 * u * t * t * xs[2] + t * t * t * xs[3],
                            u * u * u * ys[0] + 3 * u * u * t * ys[1] + 3 * u * t * t * ys[2] + t * t * t * ys[3]));
                }
            }
        }
        return points;
    }

    /** The real roots of {@code a·t² + b·t + c}; a line's one where {@code a} is nought. */
    private static List<Double> roots(double a, double b, double c) {
        if (Math.abs(a) < 1e-12) {
            return Math.abs(b) < 1e-12 ? List.of() : List.of(-c / b);
        }
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return List.of();
        }
        double root = Math.sqrt(discriminant);
        return List.of((-b + root) / (2 * a), (-b - root) / (2 * a));
    }

    /** An angle brought into {@code [0, 2π)}. */
    private static double turn(double angle) {
        double turned = angle % (2 * Math.PI);
        return turned < 0 ? turned + 2 * Math.PI : turned;
    }

    /**
     * The same graphic with every point of it moved by a move that keeps a rectangle one — a shift, a mirror about an axis —,
     * on another layer.
     */
    default KiCadFootprintGraphic moved(UnaryOperator<KiCadPoint> moved, String layer) {
        return switch (this) {
            case Line line -> new Line(moved.apply(line.start()), moved.apply(line.end()), layer, line.width(), line.filled());
            case Rectangle box -> new Rectangle(moved.apply(box.start()), moved.apply(box.end()), layer, box.width(), box.filled());
            case Circle circle -> new Circle(moved.apply(circle.center()), moved.apply(circle.end()), layer, circle.width(), circle.filled());
            case Arc arc -> new Arc(moved.apply(arc.start()), moved.apply(arc.middle()), moved.apply(arc.end()), layer, arc.width(),
                    arc.filled());
            case Polygon polygon -> new Polygon(polygon.points().stream().map(moved).toList(), layer, polygon.width(), polygon.filled());
            case Curve curve -> new Curve(moved.apply(curve.start()), moved.apply(curve.first()), moved.apply(curve.second()),
                    moved.apply(curve.end()), layer, curve.width(), curve.filled());
        };
    }

    /**
     * The graphic as a footprint or a board writes it — {@code fp_line} in a footprint, {@code gr_line} on a board —, the
     * list {@link #of} reads back.
     *
     * @param identity its UUID; null for a custom pad's primitive, which has none
     */
    default SExpressionList written(boolean onBoard, String identity) {
        List<SExpressionNode> items = new ArrayList<>();
        KiCadKeyword name = switch (this) {
            case Line line -> {
                items.add(point(START, line.start()));
                items.add(point(END, line.end()));
                yield onBoard ? BOARD_LINE : LINE;
            }
            case Rectangle box -> {
                items.add(point(START, box.start()));
                items.add(point(END, box.end()));
                yield onBoard ? BOARD_RECTANGLE : RECTANGLE;
            }
            case Circle circle -> {
                items.add(point(CENTER, circle.center()));
                items.add(point(END, circle.end()));
                yield onBoard ? BOARD_CIRCLE : CIRCLE;
            }
            case Arc arc -> {
                items.add(point(START, arc.start()));
                items.add(point(MIDDLE, arc.middle()));
                items.add(point(END, arc.end()));
                yield onBoard ? BOARD_ARC : ARC;
            }
            case Polygon polygon -> {
                items.add(points(polygon.points()));
                yield onBoard ? BOARD_POLYGON : POLYGON;
            }
            case Curve curve -> {
                items.add(points(List.of(curve.start(), curve.first(), curve.second(), curve.end())));
                yield onBoard ? BOARD_CURVE : FOOTPRINT_CURVE;
            }
        };
        items.add(SExpressionList.of(STROKE, SExpressionList.of(WIDTH, SExpressionAtom.of(width())),
                SExpressionList.of(TYPE, SExpressionAtom.of("solid"))));
        if (!(this instanceof Line) && !(this instanceof Arc) && !(this instanceof Curve)) {
            items.add(SExpressionList.of(FILL, SExpressionAtom.of(filled() ? "solid" : "none")));
        }
        // A custom pad's primitive is on its pad's layers, and names none of its own.
        if (!layer().isEmpty()) {
            items.add(SExpressionList.of(LAYER, SExpressionString.of(layer())));
        }
        if (identity != null) {
            items.add(SExpressionList.of(UUID, SExpressionString.of(identity)));
        }
        return SExpressionList.of(name, items.toArray(SExpressionNode[]::new));
    }

    private static SExpressionList point(KiCadKeyword name, KiCadPoint point) {
        return SExpressionList.of(name, SExpressionAtom.of(point.x()), SExpressionAtom.of(point.y()));
    }

    private static SExpressionList points(List<KiCadPoint> points) {
        return SExpressionList.of(POINTS, points.stream().map(point -> point(POINT, point)).toArray(SExpressionNode[]::new));
    }

    /** A footprint's or a board's graphic list, if it is one of the shapes they draw; empty for anything else (texts, dimensions). */
    static Optional<KiCadFootprintGraphic> of(SExpressionList list) {
        String layer = list.wordOf(LAYER).orElse("");
        double width = list.list(STROKE).flatMap(stroke -> stroke.list(WIDTH)).map(stroked -> stroked.number(1))
                .or(() -> list.list(WIDTH).map(written -> written.number(1))).orElse(0.0);
        boolean filled = list.list(FILL).map(KiCadFootprintGraphic::fills).orElse(false);
        if (list.is(LINE) || list.is(BOARD_LINE)) {
            return Optional.of(new Line(point(list, START), point(list, END), layer, width, filled));
        }
        if (list.is(RECTANGLE) || list.is(BOARD_RECTANGLE)) {
            return Optional.of(new Rectangle(point(list, START), point(list, END), layer, width, filled));
        }
        if (list.is(CIRCLE) || list.is(BOARD_CIRCLE)) {
            return Optional.of(new Circle(point(list, CENTER), point(list, END), layer, width, filled));
        }
        if ((list.is(ARC) || list.is(BOARD_ARC)) && list.list(MIDDLE).isPresent()) {
            return Optional.of(new Arc(point(list, START), point(list, MIDDLE), point(list, END), layer, width, filled));
        }
        if (list.is(FOOTPRINT_CURVE) || list.is(BOARD_CURVE)) {
            List<KiCadPoint> handles = new ArrayList<>();
            list.list(POINTS).ifPresent(written -> written.lists(POINT).forEach(xy -> handles.add(KiCadPoint.of(xy))));
            return handles.size() == 4 ? Optional.of(new Curve(handles.get(0), handles.get(1), handles.get(2), handles.get(3), layer,
                    width, filled)) : Optional.empty();
        }
        if (list.is(POLYGON) || list.is(BOARD_POLYGON)) {
            List<KiCadPoint> points = new ArrayList<>();
            list.list(POINTS).ifPresent(written -> written.lists(POINT).forEach(xy -> points.add(KiCadPoint.of(xy))));
            return Optional.of(new Polygon(points, layer, width, filled));
        }
        return Optional.empty();
    }

    /** KiCad 8 writes {@code (fill yes)}, {@code (fill solid)}; KiCad 6 and 7 {@code (fill (type none))}, or {@code (fill)} for none. */
    private static boolean fills(SExpressionList fill) {
        String said = fill.wordOf(TYPE).orElseGet(() -> fill.size() > 1 && !(fill.item(1) instanceof SExpressionList) ? fill.word(1) : "none");
        return !said.equals("none") && !said.equals("no");
    }

    private static KiCadPoint point(SExpressionList list, org.jmouse.kicad.KiCadKeyword name) {
        return KiCadPoint.of(list.requireList(name));
    }
}
