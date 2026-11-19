package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import java.util.ArrayList;
import java.util.List;

/** A line of points as a whole, as every part of the router speaks of a track's shape: its length, its corners, its parts joined. */
public final class Polylines {

    /** How much a cross product may be and the point still stand on the straight run through its neighbours. */
    private static final double STRAIGHT = 1e-9;

    private Polylines() {
    }

    /** How long a line is, stretch by stretch. */
    public static double lengthOf(List<Point> points) {
        double sum = 0;
        for (int index = 1; index < points.size(); index++) {
            sum += points.get(index - 1).distanceTo(points.get(index));
        }
        return sum;
    }

    /** Whether a line runs through a point: at a corner of it, or along one of its stretches. */
    public static boolean passesThrough(List<Point> line, Point point) {
        for (int index = 0; index < line.size(); index++) {
            if (Precision.samePoint(line.get(index), point) || (index > 0 && Stretches.liesBetween(point, line.get(index - 1), line.get(index)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * A line's corners: its two ends and every point where it turns — a point standing on a straight run between its
     * neighbours is no corner and is left out.
     */
    public static List<Point> cornersOf(List<Point> line) {
        List<Point> corners = new ArrayList<>();
        for (int index = 0; index < line.size(); index++) {
            if (index == 0 || index == line.size() - 1) {
                corners.add(line.get(index));
                continue;
            }
            Point point = line.get(index);
            Point previous = line.get(index - 1);
            Point next = line.get(index + 1);
            double turn = (point.x() - previous.x()) * (next.y() - point.y()) - (point.y() - previous.y()) * (next.x() - point.x());
            if (Math.abs(turn) > STRAIGHT) {
                corners.add(point);
            }
        }
        return corners;
    }

    /** A line of these parts, one after another. */
    @SafeVarargs
    public static List<Point> joined(List<Point>... parts) {
        List<Point> line = new ArrayList<>();
        for (List<Point> part : parts) {
            line.addAll(part);
        }
        return line;
    }

    /**
     * The part of a line between two lengths along it, its corners between kept: from the point {@code from} along it to the point
     * {@code to} along it, each held within the line's own length.
     */
    public static List<Point> between(List<Point> line, double from, double to) {
        List<Point> part = new ArrayList<>();
        double walked = 0;
        for (int index = 1; index < line.size(); index++) {
            Point start = line.get(index - 1);
            Point end = line.get(index);
            double length = start.distanceTo(end);
            double after = walked + length;
            if (part.isEmpty() && from <= after) {
                part.add(Precision.settled(start.toward(end, length == 0 ? 0 : Math.max(0, from - walked) / length)));
            }
            if (!part.isEmpty() && to <= after) {
                part.add(Precision.settled(start.toward(end, length == 0 ? 1 : Math.max(0, to - walked) / length)));
                return part;
            }
            if (!part.isEmpty()) {
                part.add(end);
            }
            walked = after;
        }
        if (part.isEmpty()) {
            part.add(line.getLast());
        }
        return part;
    }

    /** A line run the other way. */
    public static List<Point> reversed(List<Point> line) {
        return new ArrayList<>(line).reversed();
    }
}
