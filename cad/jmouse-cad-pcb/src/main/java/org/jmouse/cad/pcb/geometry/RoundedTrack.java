package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A track as its copper runs: straight between its points, each corner rounded by its own radius ({@link Track#roundingAt}) — the arc cut back
 * from the corner as far as the turn allows, never past half a leg. What every format a track is written in draws.
 */
public final class RoundedTrack {

    /** A corner sharper than this, or straighter, is left as it is drawn. */
    private static final double STRAIGHT = 1e-6;

    /** One stretch of a track's copper. */
    public sealed interface Piece {
    }

    /** Straight through its points: the legs between two rounded corners, or the whole track. */
    public record Run(List<Point> points) implements Piece {

        public Run {
            points = List.copyOf(points);
        }
    }

    /** A rounded corner: from where it leaves one leg, through the point nearest the corner, to where it meets the next. */
    public record Bend(Point from, Point through, Point to) implements Piece {
    }

    private RoundedTrack() {
    }

    /** The track's pieces in their order, runs and bends taking turns; none for a track of fewer than two points. */
    public static List<Piece> of(Track track) {
        List<Point> points = track.points();
        if (points.size() < 2) {
            return List.of();
        }
        List<Piece> pieces = new ArrayList<>();
        List<Point> run = new ArrayList<>(List.of(points.getFirst()));
        for (int index = 1; index < points.size() - 1; index++) {
            Optional<Bend> rounded = corner(points.get(index - 1), points.get(index), points.get(index + 1), track.roundingAt(index - 1));
            if (rounded.isEmpty()) {
                run.add(points.get(index));
                continue;
            }
            run.add(rounded.get().from());
            pieces.add(new Run(run));
            pieces.add(rounded.get());
            run = new ArrayList<>(List.of(rounded.get().to()));
        }
        run.add(points.getLast());
        pieces.add(new Run(run));
        return pieces;
    }

    private static Optional<Bend> corner(Point previous, Point corner, Point next, double rounding) {
        double inLength = Math.hypot(corner.x() - previous.x(), corner.y() - previous.y());
        double outLength = Math.hypot(next.x() - corner.x(), next.y() - corner.y());
        if (rounding <= 0 || inLength == 0 || outLength == 0) {
            return Optional.empty();
        }
        double[] inward = {(previous.x() - corner.x()) / inLength, (previous.y() - corner.y()) / inLength};
        double[] outward = {(next.x() - corner.x()) / outLength, (next.y() - corner.y()) / outLength};
        double between = Math.acos(Math.max(-1, Math.min(1, inward[0] * outward[0] + inward[1] * outward[1])));
        if (between > Math.PI - STRAIGHT || between < STRAIGHT) {
            return Optional.empty();
        }
        double tangent = Math.tan(between / 2);
        double cut = Math.min(rounding / tangent, Math.min(inLength / 2, outLength / 2));
        double radius = cut * tangent;
        Point start = new Point(corner.x() + inward[0] * cut, corner.y() + inward[1] * cut);
        Point end = new Point(corner.x() + outward[0] * cut, corner.y() + outward[1] * cut);
        // The centre lies along the corner's bisector, as far from the corner as the arc's radius over the half angle's sine.
        double[] bisector = {inward[0] + outward[0], inward[1] + outward[1]};
        double bisectorLength = Math.hypot(bisector[0], bisector[1]);
        double reach = radius / Math.sin(between / 2);
        double[] centre = {corner.x() + bisector[0] / bisectorLength * reach, corner.y() + bisector[1] / bisectorLength * reach};
        // The arc's middle: the point of the circle nearest the corner.
        Point through = new Point(centre[0] - bisector[0] / bisectorLength * radius,
                centre[1] - bisector[1] / bisectorLength * radius);
        return Optional.of(new Bend(start, through, end));
    }
}
