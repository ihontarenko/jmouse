package org.jmouse.svg.path.node;

import org.jmouse.svg.path.node.PathSegment.PathPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines a path draws, as polylines: what a drawing becomes where only polylines are drawn — a KiCad library symbol.
 * The path's runs ({@link PathContours}), a curve or an arc in short straight steps, enough that a symbol's small curves
 * read as curves.
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
        List<List<double[]>> polylines = new ArrayList<>();
        for (PathContours.PathContour contour : PathContours.of(path)) {
            List<double[]> points = new ArrayList<>();
            points.add(pointOf(contour.start()));
            for (PathSegment segment : contour.segments()) {
                if (segment instanceof PathSegment.Line line) {
                    points.add(pointOf(line.to()));
                    continue;
                }
                for (int step = 1; step <= steps; step++) {
                    points.add(pointOf(segment.at((double) step / steps)));
                }
            }
            polylines.add(points);
        }
        return polylines;
    }

    private static double[] pointOf(PathPoint point) {
        return new double[] {point.x(), point.y()};
    }
}
