package org.jmouse.svg.path.node;

import org.jmouse.svg.path.node.PathSegment.PathPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * The runs a path draws, each a chain of exact segments — lines, Bézier curves, elliptical arcs by their centres —, every
 * command followed, absolute and relative, smooth curves given the handle they mirror. What a geometry kernel builds
 * edges from; {@link PathPolylines} steps the same runs where only polylines are drawn.
 */
public final class PathContours {

    /**
     * One run of a path: from a move to the next, or to where it is closed.
     *
     * @param closed whether the path closes it — its last segment then ends where its first began
     */
    public record PathContour(List<PathSegment> segments, boolean closed) {

        public PathContour {
            segments = List.copyOf(segments);
        }

        public PathPoint start() {
            return segments.getFirst().from();
        }
    }

    private PathContours() {
    }

    /** The runs a path draws, each with one segment at least. */
    public static List<PathContour> of(PathData path) {
        return new Pen().follow(path);
    }

    /** Where the pen stands while a path is followed, and what it has drawn. */
    private static final class Pen {

        private final List<PathContour> contours = new ArrayList<>();
        private List<PathSegment> current = new ArrayList<>();
        private double x;
        private double y;
        private double startX;
        private double startY;
        /** The last handle of the last curve, for the smooth curves that mirror it. */
        private double controlX;
        private double controlY;
        private PathCommandKind previous;

        List<PathContour> follow(PathData path) {
            for (PathCommand command : path.commands()) {
                draw(command);
                previous = command.kind();
            }
            finish(false);
            return contours;
        }

        private void draw(PathCommand command) {
            double baseX = command.relative() ? x : 0;
            double baseY = command.relative() ? y : 0;
            switch (command.kind()) {
                case MOVE -> {
                    finish(false);
                    x = baseX + command.number(0);
                    y = baseY + command.number(1);
                    startX = x;
                    startY = y;
                }
                case LINE -> lineTo(baseX + command.number(0), baseY + command.number(1));
                case HORIZONTAL -> lineTo(baseX + command.number(0), y);
                case VERTICAL -> lineTo(x, baseY + command.number(0));
                case CUBIC, SMOOTH_CUBIC -> {
                    boolean smooth = command.kind() == PathCommandKind.SMOOTH_CUBIC;
                    boolean follows = previous == PathCommandKind.CUBIC || previous == PathCommandKind.SMOOTH_CUBIC;
                    int at = smooth ? 0 : 2;
                    PathPoint first = smooth ? new PathPoint(follows ? 2 * x - controlX : x, follows ? 2 * y - controlY : y)
                            : new PathPoint(baseX + command.number(0), baseY + command.number(1));
                    PathPoint second = new PathPoint(baseX + command.number(at), baseY + command.number(at + 1));
                    PathPoint end = new PathPoint(baseX + command.number(at + 2), baseY + command.number(at + 3));
                    add(new PathSegment.Cubic(here(), first, second, end));
                    controlX = second.x();
                    controlY = second.y();
                }
                case QUADRATIC, SMOOTH_QUADRATIC -> {
                    boolean smooth = command.kind() == PathCommandKind.SMOOTH_QUADRATIC;
                    boolean follows = previous == PathCommandKind.QUADRATIC || previous == PathCommandKind.SMOOTH_QUADRATIC;
                    int at = smooth ? 0 : 2;
                    PathPoint handle = smooth ? new PathPoint(follows ? 2 * x - controlX : x, follows ? 2 * y - controlY : y)
                            : new PathPoint(baseX + command.number(0), baseY + command.number(1));
                    PathPoint end = new PathPoint(baseX + command.number(at), baseY + command.number(at + 1));
                    add(new PathSegment.Quadratic(here(), handle, end));
                    controlX = handle.x();
                    controlY = handle.y();
                }
                case ARC -> arc(Math.abs(command.number(0)), Math.abs(command.number(1)), Math.toRadians(command.number(2)),
                        command.number(3) != 0, command.number(4) != 0, baseX + command.number(5), baseY + command.number(6));
                case CLOSE -> {
                    lineTo(startX, startY);
                    finish(true);
                }
            }
        }

        private PathPoint here() {
            return new PathPoint(x, y);
        }

        private void lineTo(double toX, double toY) {
            add(new PathSegment.Line(here(), new PathPoint(toX, toY)));
        }

        /** The segment drawn, and the pen where it ends. */
        private void add(PathSegment segment) {
            current.add(segment);
            x = segment.to().x();
            y = segment.to().y();
        }

        /** The run drawn so far ended — by a move, a close or the path's end —; the pen stays where it stands. */
        private void finish(boolean closed) {
            if (!current.isEmpty()) {
                contours.add(new PathContour(current, closed));
            }
            current = new ArrayList<>();
            if (closed) {
                x = startX;
                y = startY;
            }
        }

        /** An elliptical arc by its end points, its centre found as the SVG specification finds it. */
        private void arc(double radiusX, double radiusY, double turn, boolean large, boolean sweep, double toX, double toY) {
            if (radiusX == 0 || radiusY == 0) {
                lineTo(toX, toY);
                return;
            }
            double cosine = Math.cos(turn);
            double sine = Math.sin(turn);
            double halfX = (x - toX) / 2;
            double halfY = (y - toY) / 2;
            double primeX = cosine * halfX + sine * halfY;
            double primeY = -sine * halfX + cosine * halfY;
            double scale = primeX * primeX / (radiusX * radiusX) + primeY * primeY / (radiusY * radiusY);
            if (scale > 1) {
                radiusX *= Math.sqrt(scale);
                radiusY *= Math.sqrt(scale);
            }
            double numerator = radiusX * radiusX * radiusY * radiusY - radiusX * radiusX * primeY * primeY
                    - radiusY * radiusY * primeX * primeX;
            double denominator = radiusX * radiusX * primeY * primeY + radiusY * radiusY * primeX * primeX;
            double factor = Math.sqrt(Math.max(0, numerator / denominator)) * (large == sweep ? -1 : 1);
            double centrePrimeX = factor * radiusX * primeY / radiusY;
            double centrePrimeY = -factor * radiusY * primeX / radiusX;
            double centreX = cosine * centrePrimeX - sine * centrePrimeY + (x + toX) / 2;
            double centreY = sine * centrePrimeX + cosine * centrePrimeY + (y + toY) / 2;
            double startAngle = Math.atan2((primeY - centrePrimeY) / radiusY, (primeX - centrePrimeX) / radiusX);
            double endAngle = Math.atan2((-primeY - centrePrimeY) / radiusY, (-primeX - centrePrimeX) / radiusX);
            double span = endAngle - startAngle;
            if (sweep && span < 0) {
                span += 2 * Math.PI;
            } else if (!sweep && span > 0) {
                span -= 2 * Math.PI;
            }
            add(new PathSegment.EllipticalArc(here(), new PathPoint(toX, toY), new PathPoint(centreX, centreY), radiusX, radiusY,
                    turn, startAngle, span));
        }
    }
}
