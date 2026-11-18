package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.geometry.taut.TautPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A line a drag pulls or pushes, as a rubber band: its tracks joined end to end ({@link JoinedTracks}), the stretch of it the drag
 * works on — between what holds its two ends —, the path that stretch runs now and how that came out. It keeps only which way round
 * everything it goes; pulled, it comes as short as that lets it. What of the line runs beyond the region the drag works in stands
 * as it ran, and the corners round the region hold the band ({@link RegionCut}), as KiCad keeps its optimizer to the area a drag
 * changed.
 *
 * @param line      its tracks, in the line's order
 * @param layer     the layer it is on
 * @param width     its widest track's width: what it keeps clear by
 * @param net       its net; empty for a line of none
 * @param start     what holds the first point of the stretch worked on
 * @param end       what holds its last point
 * @param reach     how much of the line it works on
 * @param path      the path the stretch worked on runs now
 * @param tautness  how its last pull came out
 */
record Band(JoinedTracks.JoinedLine line, String layer, double width, Optional<String> net, BandEnd start, BandEnd end, BandReach reach,
            List<Point> path, TautPath.Tautness tautness) {

    Band {
        path = List.copyOf(path);
    }

    /** A band of so much of a line, as it ran. */
    static Band of(JoinedTracks.JoinedLine line, String layer, double width, Optional<String> net, BandReach reach) {
        return new Band(line, layer, width, net, reach.start(), reach.end(), reach, reach.worked(), TautPath.Tautness.TAUT);
    }

    /** A band of a whole line, from end to end, as it runs. */
    static Band whole(JoinedTracks.JoinedLine line, String layer, double width, Optional<String> net, BandEnd start, BandEnd end) {
        return of(line, layer, width, net, BandReach.whole(Polylines.cornersOf(line.points()), start, end));
    }

    /**
     * The band worked on further where its path folds at a corner that holds it ({@link BandReach#letGoWhereFolded}): from that
     * corner's neighbour, as the line ran, pulled afresh. Empty where it does not fold.
     */
    Optional<Band> letGoWhereFolded() {
        return reach.letGoWhereFolded(path).map(further -> of(line, layer, width, net, further));
    }

    /**
     * The band worked on as far as it must be not to fold where it is held, pulled straight between where its ends stand when the drag
     * is let go: the fold a pull would find, found before it — what is pulled round something folds no less.
     */
    Band unfoldedAsLetGo(DragMotion motion) {
        Band band = this;
        Optional<Band> further = band.reach.letGoWhereFolded(List.of(band.start.at(motion, 1), band.end.at(motion, 1)))
                .map(reach -> of(line, layer, width, net, reach));
        while (further.isPresent()) {
            band = further.get();
            Band current = band;
            further = current.reach.letGoWhereFolded(List.of(current.start.at(motion, 1), current.end.at(motion, 1)))
                    .map(reach -> of(line, layer, width, net, reach));
        }
        return band;
    }

    /** The line before the stretch worked on, as it ran — its last point the stretch's first —; empty where there is none. */
    List<Point> head() {
        return reach.head();
    }

    /** The line after it, as it ran — its first point the stretch's last —; empty where there is none. */
    List<Point> tail() {
        return reach.tail();
    }

    /**
     * Whether something takes one of its ends along — a moved pad, a sliding joint —: such a band pushes the still ones of other nets
     * ahead of it rather than going round them, for it is what moves.
     */
    boolean moving() {
        return !(start instanceof BandEnd.Held) || !(end instanceof BandEnd.Held);
    }

    /** The track it is told by: its first. */
    int track() {
        return line.members().getFirst().index();
    }

    /** Its tracks, by their places. */
    List<Integer> tracks() {
        return line.members().stream().map(JoinedTracks.Member::index).toList();
    }

    /** The same band running another path, come out so. */
    Band running(List<Point> other, TautPath.Tautness how) {
        return new Band(line, layer, width, net, start, end, reach, other, how);
    }

    /** The same band held otherwise at its ends. */
    Band withEnds(BandEnd otherStart, BandEnd otherEnd) {
        return new Band(line, layer, width, net, otherStart, otherEnd, reach, path, tautness);
    }

    /** The band once the drag is over: its ends held where the drag left them. */
    Band atRest(DragMotion motion) {
        return withEnds(new BandEnd.Held(start.at(motion, 1)), new BandEnd.Held(end.at(motion, 1)));
    }

    /** Its path with its ends where they stand a share of the way along the drag. */
    List<Point> pathAt(DragMotion motion, double share) {
        if (path.size() < 2) {
            return List.of(start.at(motion, share), end.at(motion, share));
        }
        List<Point> moved = new ArrayList<>(path);
        moved.set(0, start.at(motion, share));
        moved.set(moved.size() - 1, end.at(motion, share));
        return moved;
    }

    /** The whole line as it runs now: what stands before the stretch worked on, the stretch, what stands after it. */
    List<Point> fullPath() {
        return wholeAlong(path);
    }

    /** The whole line with the stretch worked on running these points; where it is cut and runs on straight, no corner. */
    List<Point> wholeAlong(List<Point> stretch) {
        List<Point> head = head();
        List<Point> tail = tail();
        return LineCleaning.withoutStraightPoints(Polylines.joined(head.isEmpty() ? List.of() : head.subList(0, head.size() - 1), stretch,
                                                        tail.isEmpty() ? List.of() : tail.subList(1, tail.size())));
    }

    /** Its whole line as one track: its first track's layer, its widest width. */
    Track asTrack(PcbDrawing pcb) {
        return pcb.tracks().get(track()).withPoints(fullPath());
    }

    /** The board with the band's whole line laid along these points, each of its tracks its own stretch of them. */
    RoutingWorld laidOn(RoutingWorld world, List<Point> points) {
        PcbDrawing laid = JoinedTracks.withJoinedLine(world.pcb(), line, points);
        RoutingWorld standing = world;
        for (int index : tracks()) {
            standing = standing.withTrack(index, laid.tracks().get(index).points());
        }
        return standing;
    }
}
