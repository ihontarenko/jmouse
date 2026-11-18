package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What pulling a stretch of a track means, as an engineer means it: the stretch goes across itself, staying parallel to where it
 * was. A neighbouring stretch keeps its slope and grows or shrinks. At a line's own end there is no neighbour — the end stays on
 * its pad — so the line leaves the pad at 45° to the stretch, towards where it has gone, and meets it: one corner. The shape
 * changes only as far as the pointer does, so it never jumps.
 */
public final class StretchShift {

    /** Less than this across the stretch is no move at all. */
    private static final double NO_SHIFT = 1e-6;

    private StretchShift() {
    }

    /** The line with one stretch moved across itself by (dx, dy); empty where it does not move across itself, or would run backwards. */
    public static Optional<List<Point>> shifted(List<Point> points, int segment, double dx, double dy, double angleStep) {
        if (segment < 0 || segment + 1 >= points.size()) {
            return Optional.empty();
        }
        Point from = points.get(segment);
        Point to = points.get(segment + 1);
        Optional<Point> way = Ways.wayBetween(from, to);
        if (way.isEmpty()) {
            return Optional.empty();
        }
        Point along = way.get();
        Point across = new Point(-along.y(), along.x());
        double shift = dx * across.x() + dy * across.y();
        if (Math.abs(shift) < NO_SHIFT) {
            return Optional.of(List.copyOf(points));
        }
        double side = Math.signum(shift);
        Point onLine = from.moved(across, shift);
        Point backwards = new Point(-along.x(), -along.y());

        List<Point> head;
        if (segment > 0) {
            Optional<Point> met = meetingNeighbour(points.get(segment - 1), from, onLine, along);
            if (met.isPresent()) {
                head = Polylines.joined(points.subList(0, segment), List.of(Precision.settled(met.get())));
            } else {
                Optional<Point> left = leftFrom(points.get(segment - 1), along, across, side, onLine, along, angleStep);
                head = left.map(point -> Polylines.joined(points.subList(0, segment), List.of(point))).orElse(null);
            }
        } else {
            head = leftFrom(from, along, across, side, onLine, along, angleStep).map(point -> List.of(from, point)).orElse(null);
        }
        List<Point> tail;
        if (segment + 2 < points.size()) {
            Optional<Point> met = meetingNeighbour(points.get(segment + 2), to, onLine, along);
            if (met.isPresent()) {
                tail = Polylines.joined(List.of(Precision.settled(met.get())), points.subList(segment + 2, points.size()));
            } else {
                Optional<Point> left = leftFrom(points.get(segment + 2), backwards, across, side, onLine, along, angleStep);
                tail = left.map(point -> Polylines.joined(List.of(point), points.subList(segment + 2, points.size()))).orElse(null);
            }
        } else {
            tail = leftFrom(to, backwards, across, side, onLine, along, angleStep).map(point -> List.of(point, to)).orElse(null);
        }
        if (head == null || tail == null) {
            return Optional.empty();
        }
        Point start = head.getLast();
        Point end = tail.getFirst();
        // A stretch carried past where it runs out is gone, and the two lines either side of it meet where they cross: at the
        // moment it runs out they cross just there, so nothing jumps. Past where they can, a plain step joins them.
        if (Ways.reachAlong(start, along, end) <= 0 || start.distanceTo(end) < NO_SHIFT) {
            Point before = head.get(head.size() - 2);
            Point after = tail.get(1);
            Optional<Point> coming = Ways.wayBetween(before, start);
            Optional<Point> going = Ways.wayBetween(after, end);
            Optional<Point> met = coming.isPresent() && going.isPresent() ? Ways.crossingOf(before, coming.get(), after, going.get()) : Optional.empty();
            boolean reaches = met.isPresent() && Ways.reachAlong(before, coming.get(), met.get()) > 0 && Ways.reachAlong(after, going.get(), met.get()) > 0;
            List<Point> joining;
            if (reaches) {
                joining = List.of(Precision.settled(met.get()));
            } else {
                List<Point> step = AngleSteps.trackStep(before, after, angleStep);
                joining = step.subList(0, step.size() - 1);
            }
            return Optional.of(LineCleaning.withoutStraightPoints(Polylines.joined(head.subList(0, head.size() - 1), joining, tail.subList(1, tail.size()))));
        }
        List<Point> line = new ArrayList<>(head);
        line.addAll(tail);
        return Optional.of(LineCleaning.withoutStraightPoints(line));
    }

    /**
     * Where the line leaves its end for the moved stretch: straight out of the end at 45° to the stretch, towards where it went,
     * until it meets it; square across for a track of right angles. One corner, as KiCad drags a segment off a pad.
     */
    private static Optional<Point> leftFrom(Point end, Point way, Point across, double side, Point onLine, Point along, double angleStep) {
        double half = Math.sqrt(0.5);
        Point turned = angleStep == 90 ? new Point(across.x() * side, across.y() * side)
                : new Point((way.x() + across.x() * side) * half, (way.y() + across.y() * side) * half);
        return Ways.crossingOf(end, turned, onLine, along).map(Precision::settled);
    }

    /**
     * Where a neighbouring stretch, kept on its slope, meets the moved one; empty where it would have to run on past its own far
     * end — the neighbour is used up, and the moved stretch meets that far corner as it meets a line's end.
     */
    private static Optional<Point> meetingNeighbour(Point far, Point near, Point onLine, Point along) {
        Optional<Point> way = Ways.wayBetween(far, near);
        if (way.isEmpty()) {
            return Optional.empty();
        }
        return Ways.crossingOf(far, way.get(), onLine, along).filter(met -> Ways.reachAlong(far, way.get(), met) > NO_SHIFT);
    }
}
