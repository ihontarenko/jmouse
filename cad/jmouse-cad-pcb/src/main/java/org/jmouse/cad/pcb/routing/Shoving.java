package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Other nets' tracks giving way to copper laid over them, as KiCad's router shoves them: each that comes too near has the
 * stretch in the way laid again, from a point clear before it to a point clear after it, round the new copper and everything
 * else, and pulled tight as any track is. What cannot give way — a pad, a via, an arc, a keepout — is the caller's to walk round.
 */
public final class Shoving {

    /** Shoved tracks go round at 45°: the router's own angle. */
    private static final double SHOVE_ANGLE = 45;

    private Shoving() {
    }

    /** A point along a track, and the segment it stands on. */
    private record Along(Point point, int segment) {
    }

    /** A track walked along from end to end, a point every sampling step, with each corner. */
    private static List<Along> walkedAlong(List<Point> points, double step) {
        List<Along> walked = new ArrayList<>();
        for (int segment = 0; segment < points.size() - 1; segment++) {
            Point from = points.get(segment);
            Point to = points.get(segment + 1);
            int count = Math.max(1, (int) Math.ceil(Lines.lengthBetween(from, to) / step));
            for (int at = 0; at < count; at++) {
                walked.add(new Along(new Point(from.x() + (to.x() - from.x()) * at / count, from.y() + (to.y() - from.y()) * at / count), segment));
            }
        }
        walked.add(new Along(points.getLast(), points.size() - 2));
        return walked;
    }

    private static boolean staysAway(CopperShape copper, CopperShape other, double gap) {
        return !copper.within(other, gap) || copper.gapTo(other).gap() >= gap - TrackFence.GAP_TOLERANCE;
    }

    /** A track bent round the copper: empty for one whose end is in the way, or that has no way round. */
    private static Optional<PcbDrawing.Track> bentRound(RoutingWorld world, int index, CopperShape laid, OptionalInt own, double gap) {
        PcbDrawing pcb = world.pcb();
        PcbDrawing.Track track = pcb.tracks().get(index);
        Optional<String> trackNet = world.netOfTrack(index);
        RoutingSettings.Routing settings = world.settings().routing();
        int curveSteps = world.board().checks().curveSteps();
        List<Along> walked = walkedAlong(track.points(), settings.shoveSampling());
        int first = -1;
        int last = -1;
        for (int at = 0; at < walked.size(); at++) {
            Point point = walked.get(at).point();
            if (!staysAway(CopperShape.circle(point.x(), point.y(), track.width() / 2), laid, gap)) {
                first = first < 0 ? at : first;
                last = at;
            }
        }
        // Room to turn at 45° out of the old line and back into it: the track's width and the gap, each way.
        int room = (int) Math.ceil((track.width() + gap) * 2 / settings.shoveSampling());
        int before = first - room;
        int after = last + room;
        if (first < 0 || before <= 0 || after >= walked.size() - 1) {
            return Optional.empty();
        }
        Point start = Lines.settled(walked.get(before).point());
        Point end = Lines.settled(walked.get(after).point());
        List<TrackFence.Obstacle> obstacles = new ArrayList<>();
        obstacles.add(new TrackFence.Obstacle(laid, gap));
        for (int position = 0; position < pcb.tracks().size(); position++) {
            PcbDrawing.Track other = pcb.tracks().get(position);
            boolean ownTrack = own.isPresent() && position == own.getAsInt();
            if (position != index && !ownTrack && other.layer().equals(track.layer())
                && (trackNet.isEmpty() || !world.netOfTrack(position).equals(trackNet))) {
                obstacles.add(new TrackFence.Obstacle(CopperShapes.track(other, curveSteps), gap));
            }
        }
        obstacles.addAll(world.obstacles(track.layer(), trackNet, false, OptionalInt.empty(), OptionalDouble.of(gap)));
        TrackFence fence = world.fenceOver(track.width(), gap, obstacles);
        Optional<List<Point>> way = Pathfinder.way(fence, start, end, SHOVE_ANGLE, settings.searchLimit(), Optional.empty(), settings);
        if (way.isEmpty()) {
            return Optional.empty();
        }
        List<Point> line = new Tightening(fence, SHOVE_ANGLE, List.of(), settings).tightened(way.get());
        List<Point> points = Lines.joined(track.points().subList(0, walked.get(before).segment() + 1), line,
                track.points().subList(walked.get(after).segment() + 1, track.points().size()));
        return Optional.of(track.withPoints(points));
    }

    /**
     * Every track of the pcb, those of other nets that come too near the copper bent round it: the pcb's own list when none
     * does, empty when one cannot. {@code own} is the track that is that copper, not bent round itself.
     */
    public static Optional<List<PcbDrawing.Track>> tracksGivingWay(RoutingWorld world, String layer, Optional<String> net, CopperShape laid,
                                                                   OptionalInt own) {
        double ownGap = world.clearanceOf(net);
        List<PcbDrawing.Track> tracks = new ArrayList<>();
        int curveSteps = world.board().checks().curveSteps();
        List<PcbDrawing.Track> all = world.pcb().tracks();
        for (int index = 0; index < all.size(); index++) {
            PcbDrawing.Track track = all.get(index);
            Optional<String> trackNet = world.netOfTrack(index);
            boolean ownTrack = own.isPresent() && index == own.getAsInt();
            if (ownTrack || !track.layer().equals(layer) || (net.isPresent() && trackNet.equals(net))) {
                tracks.add(track);
                continue;
            }
            double gap = Math.max(ownGap, world.clearanceOf(trackNet));
            if (staysAway(laid, CopperShapes.track(track, curveSteps), gap)) {
                tracks.add(track);
                continue;
            }
            // A track held where it stands gives way to nothing: the copper laid must go round it.
            if (world.pcb().drawnGroups().holds(track.arrangement())) {
                return Optional.empty();
            }
            Optional<PcbDrawing.Track> bent = bentRound(world, index, laid, own, gap);
            if (bent.isEmpty()) {
                return Optional.empty();
            }
            tracks.add(bent.get());
        }
        return Optional.of(tracks);
    }
}
