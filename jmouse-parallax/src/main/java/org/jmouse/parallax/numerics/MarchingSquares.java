package org.jmouse.parallax.numerics;

import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.object.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Level sets of a sampled scalar field: contour lines of z = f(x, y), the curve f(x, y) = 0. Segments are found
 * cell by cell with linear interpolation, then chained into polylines so a renderer draws lines, not dust.
 */
public final class MarchingSquares {

    private MarchingSquares() {
    }

    /** `values` is row-major (y outer), `columns × rows` samples over x × y. */
    public static List<Dataset.Polyline> contours(float[] values, int columns, int rows, Interval x, Interval y,
                                                  double level, String label) {
        List<double[]> segments = new ArrayList<>();
        for (int row = 0; row < rows - 1; row++) {
            for (int column = 0; column < columns - 1; column++) {
                double bottomLeft = values[row * columns + column] - level;
                double bottomRight = values[row * columns + column + 1] - level;
                double topRight = values[(row + 1) * columns + column + 1] - level;
                double topLeft = values[(row + 1) * columns + column] - level;
                if (!(Double.isFinite(bottomLeft) && Double.isFinite(bottomRight) && Double.isFinite(topRight)
                        && Double.isFinite(topLeft))) {
                    continue;
                }
                double x0 = x.sample(column, columns);
                double x1 = x.sample(column + 1, columns);
                double y0 = y.sample(row, rows);
                double y1 = y.sample(row + 1, rows);
                List<double[]> crossings = new ArrayList<>(4);
                edge(crossings, bottomLeft, bottomRight, x0, y0, x1, y0);
                edge(crossings, bottomRight, topRight, x1, y0, x1, y1);
                edge(crossings, topRight, topLeft, x1, y1, x0, y1);
                edge(crossings, topLeft, bottomLeft, x0, y1, x0, y0);
                if (crossings.size() == 2) {
                    segments.add(new double[]{crossings.get(0)[0], crossings.get(0)[1], crossings.get(1)[0], crossings.get(1)[1]});
                } else if (crossings.size() == 4) {
                    // A saddle cell: the corners on the centre's side join through it, so the other two are cut off.
                    // Crossings run bottom, right, top, left: 0–1 cuts off the bottom right corner, 0–3 the bottom left.
                    double centre = (bottomLeft + bottomRight + topRight + topLeft) / 4;
                    boolean bottomLeftJoinsCentre = (centre > 0) == (bottomLeft > 0);
                    int[] pairing = bottomLeftJoinsCentre ? new int[]{0, 1, 2, 3} : new int[]{0, 3, 1, 2};
                    segments.add(segment(crossings.get(pairing[0]), crossings.get(pairing[1])));
                    segments.add(segment(crossings.get(pairing[2]), crossings.get(pairing[3])));
                }
            }
        }
        return chain(segments, label);
    }

    private static double[] segment(double[] from, double[] to) {
        return new double[]{from[0], from[1], to[0], to[1]};
    }

    private static void edge(List<double[]> crossings, double first, double second, double x0, double y0,
                             double x1, double y1) {
        if ((first > 0) == (second > 0)) {
            return;
        }
        double fraction = first / (first - second);
        crossings.add(new double[]{x0 + fraction * (x1 - x0), y0 + fraction * (y1 - y0)});
    }

    /** Joins segments that share an end point into polylines. */
    private static List<Dataset.Polyline> chain(List<double[]> segments, String label) {
        Map<EndKey, List<Integer>> byEnd = new HashMap<>();
        for (int index = 0; index < segments.size(); index++) {
            double[] segment = segments.get(index);
            byEnd.computeIfAbsent(key(segment[0], segment[1]), ignored -> new ArrayList<>()).add(index);
            byEnd.computeIfAbsent(key(segment[2], segment[3]), ignored -> new ArrayList<>()).add(index);
        }
        boolean[] used = new boolean[segments.size()];
        List<Dataset.Polyline> lines = new ArrayList<>();
        for (int start = 0; start < segments.size(); start++) {
            if (used[start]) {
                continue;
            }
            used[start] = true;
            LinkedList<double[]> points = new LinkedList<>();
            double[] first = segments.get(start);
            points.add(new double[]{first[0], first[1]});
            points.add(new double[]{first[2], first[3]});
            extend(points, segments, byEnd, used, true);
            extend(points, segments, byEnd, used, false);
            float[] flat = new float[points.size() * 2];
            int cursor = 0;
            for (double[] point : points) {
                flat[cursor++] = (float) point[0];
                flat[cursor++] = (float) point[1];
            }
            boolean closed = points.size() > 2 && key(points.getFirst()[0], points.getFirst()[1])
                    .equals(key(points.getLast()[0], points.getLast()[1]));
            lines.add(new Dataset.Polyline(flat, null, label, closed));
        }
        return lines;
    }

    private static void extend(LinkedList<double[]> points, List<double[]> segments, Map<EndKey, List<Integer>> byEnd,
                               boolean[] used, boolean atTail) {
        while (true) {
            double[] end = atTail ? points.getLast() : points.getFirst();
            List<Integer> candidates = byEnd.getOrDefault(key(end[0], end[1]), List.of());
            Integer found = null;
            for (Integer candidate : candidates) {
                if (!used[candidate]) {
                    found = candidate;
                    break;
                }
            }
            if (found == null) {
                return;
            }
            used[found] = true;
            double[] segment = segments.get(found);
            boolean startsHere = key(segment[0], segment[1]).equals(key(end[0], end[1]));
            double[] next = startsHere ? new double[]{segment[2], segment[3]} : new double[]{segment[0], segment[1]};
            if (atTail) {
                points.addLast(next);
            } else {
                points.addFirst(next);
            }
        }
    }

    /**
     * An end point, quantized so two segments meeting in one cell edge share it. Both coordinates are kept whole:
     * a key folded into one number (it was x·1000003 XOR y) collides for distant points — mirrored ones, often —
     * and chained contour pieces from opposite sides of the domain into one line.
     */
    private record EndKey(long x, long y) {
    }

    private static EndKey key(double x, double y) {
        return new EndKey(Math.round(x * 1e7), Math.round(y * 1e7));
    }
}
