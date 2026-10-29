package org.jmouse.svg.path.node;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines a path draws, as polylines: what a drawing becomes where only polylines are drawn — a KiCad library symbol.
 * Every command is followed, absolute and relative; a curve or an arc in short straight steps, enough that a symbol's
 * small curves read as curves.
 */
public final class PathPolylines {

    private PathPolylines() {
    }

    /**
     * The polylines a path draws, each a list of points {x, y}; a closed one ends where it began.
     *
     * @param steps how many straight steps follow a curve or an arc
     */
    public static List<List<double[]>> of(PathData path, int steps) {
        return new Pen(steps).follow(path);
    }

    /** Where the pen stands while a path is followed, and what it has drawn. */
    private static final class Pen {

        private final int steps;
        private final List<List<double[]>> outlines = new ArrayList<>();
        private List<double[]> current;
        private double x;
        private double y;
        private double startX;
        private double startY;
        /** The last control point of the last curve, for the smooth curves that mirror it. */
        private double controlX;
        private double controlY;
        private PathCommandKind previous;

        Pen(int steps) {
            this.steps = steps;
        }

        List<List<double[]>> follow(PathData path) {
            for (PathCommand command : path.commands()) {
                draw(command);
                previous = command.kind();
            }
            outlines.removeIf(outline -> outline.size() < 2);
            return outlines;
        }

        private void draw(PathCommand command) {
            double baseX = command.relative() ? x : 0;
            double baseY = command.relative() ? y : 0;
            switch (command.kind()) {
                case MOVE -> {
                    x = baseX + command.number(0);
                    y = baseY + command.number(1);
                    startX = x;
                    startY = y;
                    current = new ArrayList<>();
                    current.add(new double[] {x, y});
                    outlines.add(current);
                }
                case LINE -> {
                    x = baseX + command.number(0);
                    y = baseY + command.number(1);
                    lineTo(x, y);
                }
                case HORIZONTAL -> {
                    x = baseX + command.number(0);
                    lineTo(x, y);
                }
                case VERTICAL -> {
                    y = baseY + command.number(0);
                    lineTo(x, y);
                }
                case CUBIC, SMOOTH_CUBIC -> {
                    boolean smooth = command.kind() == PathCommandKind.SMOOTH_CUBIC;
                    boolean follows = previous == PathCommandKind.CUBIC || previous == PathCommandKind.SMOOTH_CUBIC;
                    int at = smooth ? 0 : 2;
                    double firstX = smooth ? (follows ? 2 * x - controlX : x) : baseX + command.number(0);
                    double firstY = smooth ? (follows ? 2 * y - controlY : y) : baseY + command.number(1);
                    double secondX = baseX + command.number(at);
                    double secondY = baseY + command.number(at + 1);
                    double endX = baseX + command.number(at + 2);
                    double endY = baseY + command.number(at + 3);
                    for (int step = 1; step <= steps; step++) {
                        double t = (double) step / steps;
                        double u = 1 - t;
                        lineTo(u * u * u * x + 3 * u * u * t * firstX + 3 * u * t * t * secondX + t * t * t * endX,
                                u * u * u * y + 3 * u * u * t * firstY + 3 * u * t * t * secondY + t * t * t * endY);
                    }
                    controlX = secondX;
                    controlY = secondY;
                    x = endX;
                    y = endY;
                }
                case QUADRATIC, SMOOTH_QUADRATIC -> {
                    boolean smooth = command.kind() == PathCommandKind.SMOOTH_QUADRATIC;
                    boolean follows = previous == PathCommandKind.QUADRATIC || previous == PathCommandKind.SMOOTH_QUADRATIC;
                    int at = smooth ? 0 : 2;
                    double handleX = smooth ? (follows ? 2 * x - controlX : x) : baseX + command.number(0);
                    double handleY = smooth ? (follows ? 2 * y - controlY : y) : baseY + command.number(1);
                    double endX = baseX + command.number(at);
                    double endY = baseY + command.number(at + 1);
                    for (int step = 1; step <= steps; step++) {
                        double t = (double) step / steps;
                        double u = 1 - t;
                        lineTo(u * u * x + 2 * u * t * handleX + t * t * endX, u * u * y + 2 * u * t * handleY + t * t * endY);
                    }
                    controlX = handleX;
                    controlY = handleY;
                    x = endX;
                    y = endY;
                }
                case ARC -> {
                    double endX = baseX + command.number(5);
                    double endY = baseY + command.number(6);
                    arc(Math.abs(command.number(0)), Math.abs(command.number(1)), Math.toRadians(command.number(2)),
                            command.number(3) != 0, command.number(4) != 0, endX, endY);
                    x = endX;
                    y = endY;
                }
                case CLOSE -> {
                    lineTo(startX, startY);
                    x = startX;
                    y = startY;
                }
            }
        }

        private void lineTo(double toX, double toY) {
            if (current == null) {
                current = new ArrayList<>();
                current.add(new double[] {toX, toY});
                outlines.add(current);
            }
            current.add(new double[] {toX, toY});
        }

        /** An elliptical arc by its end points, followed in steps (SVG's arc, its centre found as the specification does). */
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
            for (int step = 1; step <= steps; step++) {
                double angle = startAngle + span * step / steps;
                double ellipseX = radiusX * Math.cos(angle);
                double ellipseY = radiusY * Math.sin(angle);
                lineTo(centreX + cosine * ellipseX - sine * ellipseY, centreY + sine * ellipseX + cosine * ellipseY);
            }
        }
    }
}
