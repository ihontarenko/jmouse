package org.jmouse.cad.kicad;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.svg.path.node.PathContours;
import org.jmouse.svg.path.node.PathSegment;
import org.jmouse.svg.path.node.PathSegment.PathPoint;
import org.jmouse.svg.path.parser.PathParseException;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A drawing's path data as KiCad's graphics — the way back from {@link KiCadFootprintDrawing#pathOf}. Lines stay lines,
 * arcs of a circle stay arcs (a whole circle a circle), Bézier curves stay curves, a quadratic one raised to the cubic
 * it is: nothing a line is drawn with is stepped. An area is different: KiCad keeps a pour's outline and a filled
 * shape's as points, so there an arc or a curve is stepped, never further off than KiCad's own steps are when it fills
 * ({@link KiCadLibraries#areaStep()}). An arc of an ellipse, which KiCad cannot draw, is stepped the same way.
 */
final class KiCadPathGraphics {

    private static final PathParser PARSER = new PathParser();
    /** How near two points may be and be one. */
    private static final double SAME = 1e-9;
    /** A whole turn, which two half arcs of one circle make. */
    private static final double WHOLE_TURN = 2 * Math.PI;

    private final double step;

    /** @param step how far a stepped arc or curve may stray from itself, in millimetres */
    KiCadPathGraphics(double step) {
        this.step = step;
    }

    /** The path's runs read; a path that does not read draws nothing, said by the caller. */
    static List<PathContours.PathContour> contoursOf(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        try {
            return PathContours.of(PARSER.parse("path", path));
        } catch (PathParseException unreadable) {
            throw new IllegalArgumentException("A path does not read: " + unreadable.detail(), unreadable);
        }
    }

    /** What a stroked path draws: each of its pieces a graphic of its own on the layer. */
    List<KiCadFootprintGraphic> stroked(String path, String layer, double width) {
        List<KiCadFootprintGraphic> graphics = new ArrayList<>();
        for (PathContours.PathContour contour : contoursOf(path)) {
            wholeCircle(contour, layer, width, false).ifPresentOrElse(graphics::add,
                    () -> contour.segments().forEach(segment -> graphics.addAll(piece(segment, layer, width))));
        }
        return graphics;
    }

    /** What a filled path draws: each of its runs a filled polygon, or a filled circle where it is one. */
    List<KiCadFootprintGraphic> filled(String path, String layer) {
        List<KiCadFootprintGraphic> graphics = new ArrayList<>();
        for (PathContours.PathContour contour : contoursOf(path)) {
            wholeCircle(contour, layer, 0, true).ifPresentOrElse(graphics::add,
                    () -> graphics.add(new KiCadFootprintGraphic.Polygon(outlineOf(contour), layer, 0, true)));
        }
        return graphics;
    }

    /** Each run of a path as the points of an area's outline. */
    List<List<KiCadPoint>> outlines(String path) {
        return contoursOf(path).stream().map(this::outlineOf).filter(outline -> outline.size() > 2).toList();
    }

    /** A run as points, its arcs and curves stepped; its closing point, which repeats its first, left out. */
    private List<KiCadPoint> outlineOf(PathContours.PathContour contour) {
        List<KiCadPoint> points = new ArrayList<>();
        points.add(point(contour.start()));
        for (PathSegment segment : contour.segments()) {
            int steps = stepsOf(segment);
            for (int index = 1; index <= steps; index++) {
                KiCadPoint next = point(segment.at((double) index / steps));
                if (!next.sameAs(points.getLast())) {
                    points.add(next);
                }
            }
        }
        if (points.size() > 1 && points.getLast().sameAs(points.getFirst())) {
            points.removeLast();
        }
        return points;
    }

    /** One piece of a stroked path as KiCad draws it. */
    private List<KiCadFootprintGraphic> piece(PathSegment segment, String layer, double width) {
        return switch (segment) {
            case PathSegment.Line line -> distance(line.from(), line.to()) <= SAME ? List.of()
                    : List.of(new KiCadFootprintGraphic.Line(point(line.from()), point(line.to()), layer, width, false));
            case PathSegment.Cubic cubic -> List.of(new KiCadFootprintGraphic.Curve(point(cubic.from()), point(cubic.first()),
                    point(cubic.second()), point(cubic.to()), layer, width, false));
            case PathSegment.Quadratic quadratic -> List.of(new KiCadFootprintGraphic.Curve(point(quadratic.from()),
                    point(towards(quadratic.from(), quadratic.handle())), point(towards(quadratic.to(), quadratic.handle())),
                    point(quadratic.to()), layer, width, false));
            case PathSegment.EllipticalArc arc when arc.circular() -> List.of(new KiCadFootprintGraphic.Arc(point(arc.from()),
                    point(arc.at(0.5)), point(arc.to()), layer, width, false));
            case PathSegment.EllipticalArc arc -> {
                List<KiCadFootprintGraphic> lines = new ArrayList<>();
                int steps = stepsOf(arc);
                for (int index = 0; index < steps; index++) {
                    lines.add(new KiCadFootprintGraphic.Line(point(arc.at((double) index / steps)), point(arc.at((double) (index + 1) / steps)),
                            layer, width, false));
                }
                yield lines;
            }
        };
    }

    /** A run of arcs of one circle turning once round it is that circle — as a circle is written in path data, two halves. */
    private static Optional<KiCadFootprintGraphic> wholeCircle(PathContours.PathContour contour, String layer, double width,
                                                                          boolean filled) {
        List<PathSegment.EllipticalArc> arcs = new ArrayList<>();
        for (PathSegment segment : contour.segments()) {
            if (segment instanceof PathSegment.EllipticalArc arc && arc.circular()) {
                arcs.add(arc);
            } else if (!(segment instanceof PathSegment.Line line && distance(line.from(), line.to()) <= SAME)) {
                return Optional.empty();
            }
        }
        if (arcs.isEmpty()) {
            return Optional.empty();
        }
        PathSegment.EllipticalArc first = arcs.getFirst();
        boolean oneCircle = arcs.stream().allMatch(arc -> distance(arc.centre(), first.centre()) <= SAME
                && Math.abs(arc.radiusX() - first.radiusX()) <= SAME);
        double turned = arcs.stream().mapToDouble(PathSegment.EllipticalArc::span).sum();
        if (!oneCircle || Math.abs(Math.abs(turned) - WHOLE_TURN) > SAME) {
            return Optional.empty();
        }
        KiCadPoint centre = point(first.centre());
        return Optional.of(new KiCadFootprintGraphic.Circle(centre, new KiCadPoint(centre.x() + first.radiusX(), centre.y()),
                layer, width, filled));
    }

    /**
     * How many straight steps a piece is followed in so that none strays further than the step allows: one for a line; for
     * an arc, as many as its sagitta needs; for a curve, as many as Wang's bound on its bending needs.
     */
    private int stepsOf(PathSegment segment) {
        return switch (segment) {
            case PathSegment.Line ignored -> 1;
            case PathSegment.EllipticalArc arc -> {
                double radius = Math.max(arc.radiusX(), arc.radiusY());
                double angle = radius <= step ? Math.PI / 2 : 2 * Math.acos(1 - step / radius);
                yield Math.max(1, (int) Math.ceil(Math.abs(arc.span()) / angle));
            }
            case PathSegment.Quadratic quadratic -> wang(bending(quadratic.from(), quadratic.handle(), quadratic.to()), 2);
            case PathSegment.Cubic cubic -> wang(Math.max(bending(cubic.from(), cubic.first(), cubic.second()),
                    bending(cubic.first(), cubic.second(), cubic.to())), 3);
        };
    }

    /** Wang's bound: a curve of degree {@code n} bent by {@code m} strays no further than the step in this many pieces. */
    private int wang(double bending, int degree) {
        return Math.max(1, (int) Math.ceil(Math.sqrt(degree * (degree - 1) * bending / (8 * step))));
    }

    /** How much three handles in a row bend: the length of their second difference. */
    private static double bending(PathPoint first, PathPoint second, PathPoint third) {
        return Math.hypot(first.x() - 2 * second.x() + third.x(), first.y() - 2 * second.y() + third.y());
    }

    /** Two thirds of the way from an end towards a quadratic curve's handle: the cubic handle it raises to. */
    private static PathPoint towards(PathPoint end, PathPoint handle) {
        return new PathPoint(end.x() + 2.0 / 3 * (handle.x() - end.x()), end.y() + 2.0 / 3 * (handle.y() - end.y()));
    }

    private static double distance(PathPoint first, PathPoint second) {
        return Math.hypot(first.x() - second.x(), first.y() - second.y());
    }

    private static KiCadPoint point(PathPoint point) {
        return new KiCadPoint(point.x(), point.y());
    }
}
