package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.geometry.taut.TautPath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Bands pulled tight laid as tracks are drawn: each stretch between two corners of the band laid at the angles the reader lays at —
 * at 45°, a run and a diagonal; at 90°, two runs —, in whichever of its two postures keeps clear, the one nearer where the line
 * ran before the drag first: a line pushed aside bends where it is pushed and runs on where it ran, never shifted along its whole
 * length. A stretch neither posture keeps clear on is halved where the band runs and laid half by half. What the band could not be
 * laid clear of is said, for a release to refuse.
 */
final class OctilinearBands {

    /** The tracks laid, the bands they were laid as, and those that could not be laid clear. */
    record Laid(PcbDrawing pcb, List<Band> bands, List<Fault> faults) {

        Laid {
            bands = List.copyOf(bands);
            faults = List.copyOf(faults);
        }

        /** What could not be laid clear, in words. */
        List<String> said() {
            return faults.stream().map(Fault::words).toList();
        }
    }

    /** A band that could not be laid clear, and why, in words. */
    record Fault(Band band, String words) {
    }

    private OctilinearBands() {
    }

    static Laid laid(RoutingWorld standing, List<Band> bands, PartDrag drag) {
        RoutingWorld world = standing;
        for (Band band : bands) {
            world = band.laidOn(world, band.fullPath());
        }
        List<Fault> faults = new ArrayList<>();
        for (Band band : bands) {
            String named = "Track " + (band.track() + 1);
            PcbDrawing.Track track = band.asTrack(world.pcb());
            TrackFence fence = world.fenceOfTrack(track, band.track(), band.net());
            List<Point> laid;
            if (band.tautness() == TautPath.Tautness.SQUEEZED) {
                faults.add(new Fault(band, named + " is squeezed between copper it cannot pass"));
                laid = endsSteppedIn(band, fence, drag.angleStep());
            } else {
                Optional<List<Point>> clear = laidAlong(world, band, fence, drag, OctilinearLaying.Clearance.KEPT);
                if (clear.isPresent()) {
                    laid = clear.get();
                } else {
                    faults.add(new Fault(band, named + " could not be laid clear of what it now meets"));
                    laid = laidAlong(world, band, fence, drag, OctilinearLaying.Clearance.OVERLOOKED).orElseThrow();
                }
            }
            RoutingSettings.Pads pads = world.settings().pads();
            if (pads.exitKind() == PadExit.SMART) {
                laid = PadBreakouts.atPads(world, band.layer(), laid, fence, drag.angleStep(), pads);
            }
            world = band.laidOn(world, asItRanIfUnchanged(band, band.wholeAlong(laid)));
        }
        return new Laid(world.pcb(), bands, faults);
    }

    /** The line as it ran, point for point, where laying it again comes to the same line: copper nothing changed is left as it is. */
    private static List<Point> asItRanIfUnchanged(Band band, List<Point> laid) {
        List<Point> ran = band.line().points();
        List<Point> was = Lines.withoutStraightPoints(ran);
        boolean same = was.size() == laid.size();
        for (int point = 0; same && point < was.size(); point++) {
            same = Lines.samePoint(was.get(point), laid.get(point));
        }
        return same ? ran : laid;
    }

    /** The band's path laid stretch by stretch at the reader's angles; empty where a stretch could not be laid clear at all. */
    private static Optional<List<Point>> laidAlong(RoutingWorld world, Band band, TrackFence fence, PartDrag drag, OctilinearLaying.Clearance clearance) {
        List<Point> head = band.head();
        Optional<Point> coming = head.size() >= 2 ? Lines.wayBetween(head.get(head.size() - 2), head.getLast()) : Optional.empty();
        return OctilinearLaying.laid(band.path(), coming, band.line().points(), fence, drag.angleStep(), world.settings().routing(), clearance);
    }

    /**
     * A squeezed band as the line ran — it was not pulled —, but for a stretch into an end that moved: that one stepped in at the
     * reader's angles, so nothing is shown at any other.
     */
    private static List<Point> endsSteppedIn(Band band, TrackFence fence, double angleStep) {
        List<Point> path = band.path();
        List<Point> ran = band.reach().worked();
        if (path.size() < 2) {
            return path;
        }
        List<Point> laid = new ArrayList<>(List.of(Lines.settled(path.getFirst())));
        for (int corner = 1; corner < path.size(); corner++) {
            Point from = laid.getLast();
            Point to = Lines.settled(path.get(corner));
            boolean intoMovedEnd = corner == 1 && !Lines.samePoint(from, ran.getFirst())
                                   || corner == path.size() - 1 && !Lines.samePoint(to, ran.getLast());
            List<Point> stretch = intoMovedEnd
                    ? OctilinearLaying.postured(from, to, Optional.empty(), band.line().points(), fence, OctilinearLaying.Clearance.OVERLOOKED, angleStep).orElse(List.of(to))
                    : List.of(to);
            laid.addAll(stretch);
        }
        return Lines.withoutStraightPoints(laid);
    }


}


