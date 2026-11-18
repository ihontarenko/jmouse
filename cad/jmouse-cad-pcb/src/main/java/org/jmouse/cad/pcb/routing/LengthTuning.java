package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.cad.pcb.geometry.NetLengths;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * A pair's shorter net lengthened to its partner's — or a track to a length asked for —, as KiCad's length tuner does it: a
 * segment bent into a meander of the pattern asked for ({@link TuningPattern}), as tall as the length wants and no taller than the
 * settings allow, a spacing of track widths apart, on the side where it keeps its gap from every other net's copper. Its length is the track's
 * own, corners rounded as the track rounds them, so what is added is what the pair's lengths then say.
 */
public final class LengthTuning {

    /** A track bent to length, or why it cannot be. */
    public sealed interface Tuning permits Tuned, Refused {
    }

    /**
     * A track bent to length: which, its corners now and the rounding they bear — no more than the meander's bumps keep their shape
     * under —, how much longer it is, and the lengths it was tuned between: its net's before, and the one reached.
     */
    public record Tuned(int track, List<Point> points, double rounding, double added, Optional<String> net, double from, double to) implements Tuning {
    }

    /** Why a track cannot be tuned, in words. */
    public record Refused(String refusal) implements Tuning {
    }

    /** The length a track's net is tuned to: the one asked for, else its pair partner's; what it is now; and the partner. */
    public record Lengths(Optional<String> net, double own, OptionalDouble target, Optional<String> partner) {
    }

    /** Where a meander goes: a track, one of its segments, and how far along it, 0 to 1, the bumps are centred. */
    private record Place(int track, int segment, double along) {
    }

    private final RoutingWorld               world;
    private final RoutingSettings.Tuning     settings;
    private final Map<String, NetLengths.Length> lengths;

    public LengthTuning(RoutingWorld world) {
        this.world = world;
        this.settings = world.settings().tuning();
        this.lengths = NetLengths.of(world.pcb(), world.nets(), world.rules().value(PcbVocabulary.RULE_PCB_THICKNESS));
    }

    /** The lengths a track is tuned between: the one asked for, else its pair partner's, and its net's now. */
    public Lengths lengthsOf(int index, OptionalDouble length) {
        Optional<String> net = world.netOfTrack(index);
        Optional<String> partner = net.flatMap(name -> world.pcb().pairs().stream()
                .filter(pair -> pair.positive().equals(name) || pair.negative().equals(name)).findFirst()
                .map(pair -> pair.positive().equals(name) ? pair.negative() : pair.positive()));
        double own = net.map(lengths::get).map(NetLengths.Length::total).orElse(NetLengths.trackLength(world.pcb().tracks().get(index)));
        OptionalDouble target = length.isPresent() ? length
                : partner.map(lengths::get).map(found -> OptionalDouble.of(found.total())).orElse(OptionalDouble.empty());
        return new Lengths(net, own, target, partner);
    }

    /**
     * The track under a point on a layer lengthened, the meander where it was pointed at: to the length asked for, or so its net is
     * as long as its pair partner; refused with words for no track there, copper of no pair, the longer net of its pair, a segment
     * too short, or no side with room.
     *
     * @param reach how far off a track's edge a point may be and still mean it
     */
    public Tuning tunedAt(String layer, Point point, double reach, OptionalDouble length, TuningPattern pattern) {
        Place best = null;
        double bestGap = Double.POSITIVE_INFINITY;
        List<Track> tracks = world.pcb().tracks();
        for (int index = 0; index < tracks.size(); index++) {
            Track track = tracks.get(index);
            if (!track.layer().equals(layer)) {
                continue;
            }
            for (int segment = 0; segment < track.points().size() - 1; segment++) {
                Point from = track.points().get(segment);
                Point to = track.points().get(segment + 1);
                double dx = to.x() - from.x();
                double dy = to.y() - from.y();
                double square = dx * dx + dy * dy;
                double along = square == 0 ? 0 : Math.max(0, Math.min(1, ((point.x() - from.x()) * dx + (point.y() - from.y()) * dy) / square));
                double gap = Math.hypot(point.x() - from.x() - along * dx, point.y() - from.y() - along * dy) - track.width() / 2;
                if (gap <= reach && gap < bestGap) {
                    best = new Place(index, segment, along);
                    bestGap = gap;
                }
            }
        }
        if (best == null) {
            return new Refused("Point at a track: the meander goes on the segment under the pointer");
        }
        return meanderOn(best, length, pattern);
    }

    /**
     * A chosen track lengthened wherever on it a meander fits best: its segments tried longest first, each with the bumps in its
     * middle. The words are the longest segment's, when none of them can.
     */
    public Tuning tunedAnywhere(int index, OptionalDouble length, TuningPattern pattern) {
        if (index < 0 || index >= world.pcb().tracks().size() || world.pcb().tracks().get(index).points().size() < 2) {
            return new Refused("Choose a track to lengthen");
        }
        List<Point> points = world.pcb().tracks().get(index).points();
        List<Integer> segments = new ArrayList<>();
        for (int segment = 0; segment < points.size() - 1; segment++) {
            segments.add(segment);
        }
        segments.sort(Comparator.comparingDouble((Integer segment) -> -points.get(segment).distanceTo(points.get(segment + 1))));
        Tuning first = null;
        for (int segment : segments) {
            Tuning answer = meanderOn(new Place(index, segment, 0.5), length, pattern);
            if (answer instanceof Tuned) {
                return answer;
            }
            first = first == null ? answer : first;
        }
        return first;
    }

    /**
     * The bumps laid along a segment: so many of the pattern, so tall to one side (+1 the segment's left, -1 its right — each bump's
     * own as the pattern turns it), a spacing apart, centred as near a share of the segment as its ends allow; empty where the segment
     * cannot hold them with a spacing clear at each end.
     */
    private static Optional<List<Point>> bumpsOf(TuningPattern pattern, Point from, Point to, double along, int count, double height, double spacing,
                                                 int side) {
        double length = from.distanceTo(to);
        double span = (2 * count - 1) * spacing;
        if (span + 2 * spacing > length) {
            return Optional.empty();
        }
        double start = Math.max(spacing, Math.min(length - spacing - span, along * length - span / 2));
        double unitX = (to.x() - from.x()) / length;
        double unitY = (to.y() - from.y()) / length;
        List<Point> points = new ArrayList<>();
        for (int bump = 0; bump < count; bump++) {
            int bumpSide = pattern.sideOf(bump, side);
            double normalX = unitY * bumpSide;
            double normalY = -unitX * bumpSide;
            for (double[] corner : pattern.bump(start + bump * 2 * spacing, spacing, height)) {
                Point point = new Point(from.x() + unitX * corner[0] + normalX * corner[1], from.y() + unitY * corner[0] + normalY * corner[1]);
                // A pattern whose bump starts where its last point stood — a rounded top on no shoulder — gives that corner once.
                if (points.isEmpty() || !Precision.samePoint(points.getLast(), point)) {
                    points.add(point);
                }
            }
        }
        return Optional.of(points);
    }

    private static List<Point> withBumps(Track track, int segment, List<Point> bumps) {
        return Polylines.joined(track.points().subList(0, segment + 1), bumps, track.points().subList(segment + 1, track.points().size()));
    }

    private static String millimetres(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** A meander laid at a place on a track, as tall as the length wants; or why it cannot be. */
    private Tuning meanderOn(Place place, OptionalDouble length, TuningPattern pattern) {
        Lengths tuned = lengthsOf(place.track(), length);
        if (tuned.target().isEmpty()) {
            return new Refused("Track " + (place.track() + 1) + " is in no differential pair: write the length to reach in Tune to");
        }
        double target = tuned.target().getAsDouble();
        double missing = target - tuned.own();
        if (missing <= settings.tolerance()) {
            if (length.isPresent() || tuned.partner().isEmpty()) {
                return new Refused("It is " + millimetres(tuned.own()) + " mm long already: more than " + millimetres(target) + " mm");
            }
            String partner = tuned.partner().get();
            return new Refused(tuned.net().orElse("") + " is already as long as " + partner + (missing < 0 ? ": lengthen " + partner + "'s track instead" : ""));
        }
        Track standing = world.pcb().tracks().get(place.track());
        double gap = world.clearanceOf(tuned.net());
        double spacing = Math.max(standing.width() * settings.spacing(), standing.width() + gap);
        // The track as the meander is laid on it: its corners rounded no more than the bumps bear, or a zigzag's teeth are rounded away.
        Track track = standing.withRounding(pattern.roundingFor(standing.rounding(), spacing));
        Point from = track.points().get(place.segment());
        Point to = track.points().get(place.segment() + 1);
        double tallest = settings.amplitude();
        TrackFence fence = world.fenceOver(track.width(), gap,
                world.obstacles(track.layer(), tuned.net(), OptionalInt.of(place.track()), OptionalDouble.of(gap)));
        double before = NetLengths.trackLength(standing);
        java.util.function.ToDoubleFunction<List<Point>> added = bumps ->
                NetLengths.trackLength(track.withPoints(withBumps(track, place.segment(), bumps))) - before;
        int curveSteps = world.board().checks().curveSteps();
        java.util.function.Predicate<List<Point>> clear = bumps ->
                fence.copperClear(CopperShapes.track(track.withPoints(Polylines.joined(List.of(from), bumps, List.of(to))), curveSteps));
        // Whether a meander long enough was laid and refused for the copper beside it, not for want of segment.
        boolean blocked = false;
        int fewest = Math.max(1, (int) Math.ceil(missing / (2 * tallest)));
        for (int count = fewest; count < (int) Math.ceil(missing / (2 * tallest)) + settings.bumpsTried(); count++) {
            for (int side : new int[] {1, -1}) {
                int bumps = count;
                java.util.function.DoubleFunction<Optional<List<Point>>> at = height -> bumpsOf(pattern, from, to, place.along(), bumps, height, spacing, side);
                Optional<List<Point>> tallestBumps = at.apply(tallest);
                if (tallestBumps.isEmpty()) {
                    return new Refused(blocked ? "No side of this segment has room for the meander: try a longer, freer segment"
                            : "Segment " + (place.segment() + 1) + " of track " + (place.track() + 1) + " is too short for the meander: "
                              + millimetres(missing) + " mm is missing");
                }
                if (added.applyAsDouble(tallestBumps.get()) < missing) {
                    continue;
                }
                // As tall as the length wants: the shortest bump that adds it, found by halving.
                double low = 0;
                double high = tallest;
                while (high - low > settings.tolerance() / 4) {
                    double middle = (low + high) / 2;
                    if (added.applyAsDouble(at.apply(middle).orElseThrow()) < missing) {
                        low = middle;
                    } else {
                        high = middle;
                    }
                }
                List<Point> laid = at.apply(Precision.settled(high)).orElseThrow().stream().map(Precision::settled).toList();
                if (clear.test(laid)) {
                    return new Tuned(place.track(), withBumps(track, place.segment(), laid), track.rounding(), added.applyAsDouble(laid), tuned.net(),
                            tuned.own(), target);
                }
                blocked = true;
            }
        }
        return new Refused("No side of this segment has room for the meander: try a longer, freer segment");
    }
}
