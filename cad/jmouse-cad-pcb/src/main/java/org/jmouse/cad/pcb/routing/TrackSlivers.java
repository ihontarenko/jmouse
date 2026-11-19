package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Slivers untied: a track shorter, all along, than its own width — straight, or knotted on itself —, standing between two other
 * tracks' ends, as an import leaves one at a joint. Inside the copper of both tracks it joins, it carries nothing and makes the joint
 * a knot: the three overlap, so the joint reads as a junction and the line through it is no line. Untied, the sliver goes and the two tracks meet where their lines
 * cross, as any two joined stretches turn ({@link JoinedTracks#cornerBetween}); the joint is then a plain one, and what a line
 * does at a joint — a jog merged, a corner rounded — is done there too.
 */
final class TrackSlivers implements LineMerging {

    /** A track end: which track, and whether it is its last point. */
    private record End(int track, boolean last) {
    }

    /**
     * Every sliver touching a chosen track untied, one after another — a knot of several slivers is untied whole —; none chosen,
     * every sliver. A sliver at a via, or whose end meets anything but one other track's end, is a joint of its own and stays.
     */
    @Override
    public Merged merged(PcbDrawing pcb, List<Integer> chosen) {
        List<Track> tracks = new ArrayList<>(pcb.tracks());
        List<Boolean> asked = new ArrayList<>(IntStream.range(0, tracks.size()).mapToObj(index -> chosen.isEmpty() || chosen.contains(index)).toList());
        for (int round = tracks.size(); round > 0; round--) {
            Optional<Integer> sliver = IntStream.range(0, tracks.size()).boxed()
                    .filter(index -> untiable(pcb.vias(), tracks, asked, index)).findFirst();
            if (sliver.isEmpty()) {
                break;
            }
            untie(tracks, asked, sliver.get());
        }
        List<Integer> standing = IntStream.range(0, tracks.size()).filter(asked::get).boxed().toList();
        return new Merged(pcb.withTracks(List.copyOf(tracks)), chosen.isEmpty() ? List.of() : standing);
    }

    /** Whether a track is a sliver that may go: asked about, or joined to a track that is. */
    private static boolean untiable(List<Via> vias, List<Track> tracks, List<Boolean> asked, int index) {
        Track track = tracks.get(index);
        List<Point> points = track.points();
        if (Polylines.lengthOf(points) >= track.width()) {
            return false;
        }
        if (vias.stream().anyMatch(via -> points.stream().anyMatch(point -> Precision.samePoint(point, new Point(via.x(), via.y()))))) {
            return false;
        }
        Optional<End> before = onlyEndAt(tracks, index, points.getFirst());
        Optional<End> after = onlyEndAt(tracks, index, points.getLast());
        return before.isPresent() && after.isPresent() && before.get().track() != after.get().track()
               && (asked.get(index) || asked.get(before.get().track()) || asked.get(after.get().track()));
    }

    /** The one other track end standing at a point on the sliver's layer; empty where none or more than one does. */
    private static Optional<End> onlyEndAt(List<Track> tracks, int sliver, Point point) {
        String layer = tracks.get(sliver).layer();
        List<End> ends = IntStream.range(0, tracks.size()).filter(index -> index != sliver && tracks.get(index).layer().equals(layer))
                .boxed().flatMap(index -> {
                    List<Point> points = tracks.get(index).points();
                    return Stream.of(new End(index, false), new End(index, true))
                            .filter(end -> Precision.samePoint(end.last() ? points.getLast() : points.getFirst(), point));
                }).toList();
        return ends.size() == 1 ? Optional.of(ends.getFirst()) : Optional.empty();
    }

    /** The sliver gone, the two tracks it joined meeting where their lines cross; what was asked about stays asked about. */
    private static void untie(List<Track> tracks, List<Boolean> asked, int index) {
        List<Point> points = tracks.get(index).points();
        End before = onlyEndAt(tracks, index, points.getFirst()).orElseThrow();
        End after = onlyEndAt(tracks, index, points.getLast()).orElseThrow();
        Point corner = Precision.settled(JoinedTracks.cornerBetween(beside(tracks, before), points.getFirst(), points.getLast(), beside(tracks, after)));
        moveEnd(tracks, before, corner);
        moveEnd(tracks, after, corner);
        boolean wasAsked = asked.get(index);
        asked.set(before.track(), asked.get(before.track()) || wasAsked);
        asked.set(after.track(), asked.get(after.track()) || wasAsked);
        tracks.remove(index);
        asked.remove(index);
    }

    /** The point of a track next to one of its ends: where the stretch ending there comes from. */
    private static Point beside(List<Track> tracks, End end) {
        List<Point> points = tracks.get(end.track()).points();
        return end.last() ? points.get(points.size() - 2) : points.get(1);
    }

    private static void moveEnd(List<Track> tracks, End end, Point point) {
        Track track = tracks.get(end.track());
        List<Point> points = new ArrayList<>(track.points());
        points.set(end.last() ? points.size() - 1 : 0, point);
        tracks.set(end.track(), track.withPoints(List.copyOf(points)));
    }
}
