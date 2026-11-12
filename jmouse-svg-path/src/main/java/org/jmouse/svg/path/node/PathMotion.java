package org.jmouse.svg.path.node;

import org.jmouse.svg.path.node.PathContours.PathContour;
import org.jmouse.svg.path.node.PathSegment.PathPoint;

import java.util.function.UnaryOperator;

/**
 * A path turned and moved as one rigid piece: its curves and arcs stay curves and arcs, nothing is stepped into lines. A path
 * has no sense of which way is up, so the turn is told in radians of the frame's own angle: from X towards Y.
 */
public final class PathMotion {

    /** How far from a whole number the sine of a quarter turn lands, in doubles. */
    private static final double QUARTER_TURN_NOISE = 1e-12;

    private PathMotion() {
    }

    /**
     * The path turned about the origin, then moved.
     *
     * @param turn   radians, from X towards Y
     * @param acrossX how far it then moves along X
     * @param acrossY how far it then moves along Y
     */
    public static PathData of(PathData path, double turn, double acrossX, double acrossY) {
        double cosine = settled(Math.cos(turn));
        double sine = settled(Math.sin(turn));
        UnaryOperator<PathPoint> moved = point -> new PathPoint(point.x() * cosine - point.y() * sine + acrossX,
                                                                point.x() * sine + point.y() * cosine + acrossY);
        PathBuilder builder = PathBuilder.path();
        for (PathContour contour : PathContours.of(path)) {
            PathPoint start = moved.apply(contour.start());
            builder.moveTo(start.x(), start.y());
            for (PathSegment segment : contour.segments()) {
                segment(builder, segment, moved, turn);
            }
            if (contour.closed()) {
                builder.close();
            }
        }
        return builder.build();
    }

    /** A cosine or sine a whole quarter turn gives: exactly what it is, so a turned path's corners stay on the numbers they were. */
    private static double settled(double value) {
        double whole = Math.rint(value);
        return Math.abs(value - whole) < QUARTER_TURN_NOISE ? whole : value;
    }

    private static void segment(PathBuilder builder, PathSegment segment, UnaryOperator<PathPoint> moved, double turn) {
        PathPoint to = moved.apply(segment.to());
        switch (segment) {
            case PathSegment.Line ignored -> builder.lineTo(to.x(), to.y());
            case PathSegment.Quadratic curve -> {
                PathPoint handle = moved.apply(curve.handle());
                builder.quadraticTo(handle.x(), handle.y(), to.x(), to.y());
            }
            case PathSegment.Cubic curve -> {
                PathPoint first = moved.apply(curve.first());
                PathPoint second = moved.apply(curve.second());
                builder.cubicTo(first.x(), first.y(), second.x(), second.y(), to.x(), to.y());
            }
            case PathSegment.EllipticalArc arc -> builder.arcTo(arc.radiusX(), arc.radiusY(), Math.toDegrees(arc.turn() + turn),
                    Math.abs(arc.span()) > Math.PI, arc.span() > 0, to.x(), to.y());
        }
    }
}
