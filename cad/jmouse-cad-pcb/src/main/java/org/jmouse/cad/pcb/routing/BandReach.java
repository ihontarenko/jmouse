package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import java.util.List;
import java.util.Optional;

/**
 * How much of a line a band works on: the line's corners, and the two of them that hold the stretch worked on — or the line's own
 * ends, held by what holds them. What lies before the first and after the last stands as it ran.
 *
 * @param corners   the line's corners, its ends among them
 * @param from      the corner the stretch worked on starts at
 * @param to        the corner it ends at
 * @param lineStart what holds the line's own first end
 * @param lineEnd   what holds its own last end
 */
record BandReach(List<Point> corners, int from, int to, BandEnd lineStart, BandEnd lineEnd) {

    /** How much sharper a turn must be to count, in radians: what a point settled to the precision kept turns a run by. */
    private static final double ANGLE_TOLERANCE = 1e-3;

    BandReach {
        corners = List.copyOf(corners);
    }

    /** The whole line worked on. */
    static BandReach whole(List<Point> corners, BandEnd lineStart, BandEnd lineEnd) {
        return new BandReach(corners, 0, corners.size() - 1, lineStart, lineEnd);
    }

    /** What holds the stretch's first point: the line's own end, or the corner it is cut at. */
    BandEnd start() {
        return from == 0 ? lineStart : new BandEnd.Held(corners.get(from));
    }

    /** What holds the stretch's last point. */
    BandEnd end() {
        return to == corners.size() - 1 ? lineEnd : new BandEnd.Held(corners.get(to));
    }

    /** The line before the stretch, its holding corner last; empty where the line's own end holds it. */
    List<Point> head() {
        return from == 0 ? List.of() : corners.subList(0, from + 1);
    }

    /** The stretch as the line ran it. */
    List<Point> worked() {
        return corners.subList(from, to + 1);
    }

    /** The line after the stretch, its holding corner first; empty where the line's own end holds it. */
    List<Point> tail() {
        return to == corners.size() - 1 ? List.of() : corners.subList(to, corners.size());
    }

    /**
     * The reach let go of a corner that holds it where the path held there turns at it sharper than the line itself turned there, or
     * the other way: worked on from the corner before. A corner beyond the region holds the band only as the line's own corner — a
     * drag never makes a line sharper where it does not work —, and a rubber band would not stay bent round a corner nothing
     * pulls it round. Empty where neither end turns so, or there is no corner left to let go of.
     */
    Optional<BandReach> letGoWhereFolded(List<Point> path) {
        boolean headFolds = from > 0 && path.size() >= 2
                            && sharper(corners.get(from - 1), corners.get(from), corners.get(from + 1), path.get(1));
        boolean tailFolds = to < corners.size() - 1 && path.size() >= 2
                            && sharper(corners.get(to + 1), corners.get(to), corners.get(to - 1), path.get(path.size() - 2));
        if (!headFolds && !tailFolds) {
            return Optional.empty();
        }
        return Optional.of(new BandReach(corners, headFolds ? from - 1 : from, tailFolds ? to + 1 : to, lineStart, lineEnd));
    }

    /**
     * Whether a line coming from {@code before} through a corner turns there sharper towards {@code pulled} than it turned towards
     * {@code ran}, or turns the other way.
     */
    private static boolean sharper(Point before, Point corner, Point ran, Point pulled) {
        double turned = turnAt(before, corner, ran);
        double pulledTurn = turnAt(before, corner, pulled);
        boolean otherWay = turned * pulledTurn < 0 && Math.abs(pulledTurn) > ANGLE_TOLERANCE;
        return otherWay || Math.abs(pulledTurn) > Math.abs(turned) + ANGLE_TOLERANCE;
    }

    /** How far a line turns at a corner, in radians: positive one way round, negative the other. */
    private static double turnAt(Point before, Point corner, Point after) {
        double in = Math.atan2(corner.y() - before.y(), corner.x() - before.x());
        double out = Math.atan2(after.y() - corner.y(), after.x() - corner.x());
        double turn = out - in;
        while (turn > Math.PI) {
            turn -= 2 * Math.PI;
        }
        while (turn < -Math.PI) {
            turn += 2 * Math.PI;
        }
        return turn;
    }
}
