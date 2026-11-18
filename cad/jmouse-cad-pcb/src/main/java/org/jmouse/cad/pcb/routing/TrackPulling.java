package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.geometry.index.Box;
import org.jmouse.numerics.Halving;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperShape;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * A track pulled to a shape, as far as it can be had within the rules: the shape laid where it is free, pulled as a band where it runs
 * into copper — caught behind what the stretch was pulled over, never on round its far side —, else held as far along as it goes.
 * What is asked of it is a {@link ReshapeRequest}; the hand's drags ({@link TrackReshaping}) ask it.
 */
final class TrackPulling {

    private TrackPulling() {
    }

    /**
     * A track pulled by one point of it.
     *
     * @param taken   the point of the track the reader took it by
     * @param pointer where the pointer is now
     * @param from    the corner before the taken stretch that stays, by its place on the line: the band runs from it to the point
     * @param to      the corner after the stretch that stays: the band runs from the point to it
     */
    public record Pull(Point taken, Point pointer, int from, int to) {
    }

    /**
     * The shape wanted of a track, as far as it can be had.
     *
     * @param track   the track, by its place on the pcb
     * @param shapeAt the whole line asked for at a share of the way the reader has carried it, 1 being all of it; empty where it
     *                folds on itself
     * @param heldOf  corners of a shape the reader holds: they do not go when it is pulled tight
     * @param pulled  the track pulled as a band, for where the shape asked for runs into copper
     * @param sweptPast what the stretch carried was pulled over, each drawn on in the way it was pulled: a way round it goes on the
     *                side the stretch came from, as a band pulled over a peg is caught behind it — never on round its far side
     */
    public record ReshapeRequest(RoutingWorld world, int track, TrackRouter.LayingStyle style, DoubleFunction<Optional<List<Point>>> shapeAt,
                                 Function<List<Point>, List<Point>> heldOf, Optional<Pull> pulled, List<TrackFence.Obstacle> sweptPast) {

        public ReshapeRequest {
            sweptPast = List.copyOf(sweptPast);
        }
    }

    /**
     * What a stretch of a track carried across itself was pulled onto: each piece of copper whose middle the stretch swept — between
     * where it stood and where it is pulled to — and that it now comes too near, drawn on from there in the way it was pulled, past
     * where the stretch now is. A way round such a piece goes round its near side.
     */
    static List<TrackFence.Obstacle> sweptPast(RoutingWorld world, int index, Point from, Point to, Point way) {
        double pulled = Math.hypot(way.x(), way.y());
        if (pulled == 0) {
            return List.of();
        }
        Track track = world.pcb().tracks().get(index);
        Optional<String> net = world.netOfTrack(index);
        double alongX = to.x() - from.x();
        double alongY = to.y() - from.y();
        double reach = pulled + Math.hypot(alongX, alongY);
        Point unit = new Point(way.x() / pulled, way.y() / pulled);
        CopperShape pulledTo = RoutingWorld.copperOfLine(track.width(), List.of(new Point(from.x() + way.x(), from.y() + way.y()),
                new Point(to.x() + way.x(), to.y() + way.y())));
        List<TrackFence.Obstacle> shadows = new ArrayList<>();
        for (TrackFence.Obstacle obstacle : world.obstacles(track.layer(), net, OptionalInt.of(index), OptionalDouble.empty())) {
            Box box = obstacle.shape().box();
            Point middle = new Point((box.left() + box.right()) / 2, (box.top() + box.bottom()) / 2);
            // Only what the stretch is pulled onto: copper it swept and passed clear of is behind it, nothing to be caught on.
            if (!sweptOver(middle, from, alongX, alongY, way) || TrackFence.keepsClear(pulledTo, obstacle.shape(), obstacle.gap())) {
                continue;
            }
            double half = Math.max(box.right() - box.left(), box.bottom() - box.top()) / 2;
            shadows.add(new TrackFence.Obstacle(CopperShape.line(List.of(middle, middle.moved(unit, reach)), half), obstacle.gap()));
        }
        return shadows;
    }

    /** Whether a point lies in the room a stretch swept: {@code from + s·along + t·way}, both shares between 0 and 1. */
    static boolean sweptOver(Point point, Point from, double alongX, double alongY, Point way) {
        double determinant = alongX * way.y() - alongY * way.x();
        if (Math.abs(determinant) < 1e-12) {
            return false;
        }
        double relativeX = point.x() - from.x();
        double relativeY = point.y() - from.y();
        double along = (relativeX * way.y() - relativeY * way.x()) / determinant;
        double across = (alongX * relativeY - alongY * relativeX) / determinant;
        return along >= 0 && along <= 1 && across >= 0 && across <= 1;
    }

    /** The legs of a shape that were no legs of the track: what the carrying changed. */
    static boolean[] newStretches(Track track, List<Point> shape) {
        boolean[] changed = new boolean[shape.size() - 1];
        for (int index = 0; index < changed.length; index++) {
            Point from = shape.get(index);
            Point to = shape.get(index + 1);
            boolean had = false;
            for (int at = 1; at < track.points().size() && !had; at++) {
                had = Precision.samePoint(track.points().get(at - 1), from) && Precision.samePoint(track.points().get(at), to);
            }
            changed[index] = !had;
        }
        return changed;
    }

    /** A pcb with a track's line in place and its corners rounded as far as they still fit. */
    static PcbDrawing withLine(RoutingWorld world, int index, List<Point> line, TrackFence against) {
        List<Track> changed = new ArrayList<>(world.pcb().tracks());
        Track track = changed.get(index);
        double rounding = against.roundingThatFits(line, track.rounding(), track.layer(), world.settings().routing().roundingHalvings());
        changed.set(index, track.withPoints(line).withRounding(rounding));
        return world.pcb().withTracks(changed);
    }

    /**
     * The shape laid on the board: as asked where it is free; else each new stretch walked round what it meets. Empty where one
     * cannot reach its end.
     */
    static Optional<PcbDrawing> shapeLaid(ReshapeRequest request, List<Point> shape, boolean walking) {
        RoutingWorld world = request.world();
        int index = request.track();
        Track track = world.pcb().tracks().get(index);
        Optional<String> net = world.netOfTrack(index);
        RoutingSettings settings = world.settings();
        boolean[] isNew = newStretches(track, shape);
        List<Point> held = request.heldOf().apply(shape);
        List<Point> ends = List.of(shape.getFirst(), shape.getLast());
        TrackFence fence = world.fence(track.layer(), net, track.width(), OptionalInt.of(index)).leaving(ends);
        Tightening tightening = new Tightening(fence, request.style().angleStep(), held, settings.routing());
        Function<List<Point>, Optional<PcbDrawing>> allOf = line -> {
            // Each leg the router walked came pulled tight already; the whole is the person's, and only cleaned.
            List<Point> tight = tightening.untangled(line, request.style().jogs());
            // What cannot be cleaned here is no shape to show; nor is a line that cleaning cut what the reader holds out of.
            if (LineCleaning.isTangled(tight) || !held.stream().allMatch(point -> Polylines.passesThrough(tight, point))) {
                return Optional.empty();
            }
            return Optional.of(withLine(world, index, tight, fence));
        };
        List<List<Point>> stretches = new ArrayList<>();
        for (int at = 0; at < isNew.length; at++) {
            if (isNew[at]) {
                stretches.add(List.of(shape.get(at), shape.get(at + 1)));
            }
        }
        if (stretches.stream().allMatch(fence::lineClear)) {
            return allOf.apply(shape);
        }
        if (!walking) {
            return Optional.empty();
        }
        List<Point> line = new ArrayList<>(List.of(shape.getFirst()));
        for (int at = 1; at < shape.size(); at++) {
            if (!isNew[at - 1] || fence.lineClear(List.of(shape.get(at - 1), shape.get(at)))) {
                line.add(shape.get(at));
                continue;
            }
            // A walk longer than the line asked for by its share is not the line being pulled: this leg may come out no longer than
            // what that leaves it, the rest of the shape standing at its straight length at least.
            double stretch = Polylines.lengthOf(List.of(shape.get(at - 1), shape.get(at)));
            double budget = Polylines.lengthOf(shape) * settings.carrying().longestWalk() - (Polylines.lengthOf(shape) - stretch);
            TrackRouter.RoutedLeg leg = TrackRouter.routedLeg(TrackRouter.LegRequest.walked(world, track, net, shape.get(at - 1), shape.get(at), request.style().angleStep(), index,
                    held, ends).keepingClearOf(request.sweptPast()).noLongerThan(budget));
            if (!leg.reached()) {
                return Optional.empty();
            }
            line.addAll(leg.points());
        }
        // A walk goes round what is in the way, near it: one that came out much longer than the line asked for went round
        // something else — round a whole part, or the far end of a track — and is not the line the reader is pulling.
        if (Polylines.lengthOf(line) > Polylines.lengthOf(shape) * settings.carrying().longestWalk()) {
            return Optional.empty();
        }
        return allOf.apply(line);
    }

    /**
     * The shape asked for where it is free or what is there gives way. Where it runs into copper the pointer leads, and only what
     * meets something goes round it; then the shape asked for walked round; last, as far along as it can get. In free space a
     * stretch pulled stays parallel.
     */
    public static Optional<PcbDrawing> reshapedTrack(ReshapeRequest request) {
        Optional<List<Point>> whole = request.shapeAt().apply(1);
        Optional<PcbDrawing> free = whole.flatMap(shape -> shapeLaid(request, shape, false));
        if (free.isPresent()) {
            return free;
        }
        // Where the point the track was taken by goes all the way, the shape asked for is what is meant, and what is in its way is
        // walked round; where something stops the point, the track presses on it as a band.
        boolean stopped = request.pulled().map(pull -> !pulledFreely(request, pull)).orElse(true);
        Supplier<Optional<PcbDrawing>> walked = () -> whole.flatMap(shape -> shapeLaid(request, shape, true));
        Supplier<Optional<PcbDrawing>> band = () -> request.pulled().flatMap(pull -> bandLaid(request, pull));
        Optional<PcbDrawing> laid = stopped ? band.get().or(walked) : walked.get().or(band);
        return laid.or(() -> farthestHeld(request));
    }

    /** Whether the point a track is pulled by gets all the way to the pointer with nothing in its way. */
    static boolean pulledFreely(ReshapeRequest request, Pull pull) {
        RoutingWorld world = request.world();
        Track track = world.pcb().tracks().get(request.track());
        return world.fenceOfTrack(track, request.track(), world.netOfTrack(request.track())).lineClear(List.of(pull.taken(), pull.pointer()));
    }

    /**
     * The track as a band pulled by the point it was taken by: that point carried straight towards the pointer and stopped where a
     * track standing on it would first come too near copper; then the band from the corner that stays before the stretch to the
     * point, and from the point to the corner after it, each the shortest way round what it meets, pulled tight. As the pointer
     * moves, the point moves with it, so the shape never jumps; and it never goes past what stopped it into a gap beyond.
     */
    static Optional<PcbDrawing> bandLaid(ReshapeRequest request, Pull pull) {
        RoutingWorld world = request.world();
        int index = request.track();
        double angleStep = request.style().angleStep();
        Track track = world.pcb().tracks().get(index);
        Optional<String> net = world.netOfTrack(index);
        List<Point> line = track.points();
        if (pull.from() < 0 || pull.to() >= line.size()) {
            return Optional.empty();
        }
        Point before = line.get(pull.from());
        Point after = line.get(pull.to());
        List<Point> ends = List.of(line.getFirst(), line.getLast());
        TrackFence fence = world.fence(track.layer(), net, track.width(), OptionalInt.of(index)).leaving(ends);
        if (!fence.pointClear(pull.taken())) {
            return Optional.empty();
        }
        RoutingSettings settings = world.settings();
        List<Point> kept = Polylines.joined(line.subList(0, pull.from() + 1), line.subList(pull.to(), line.size()));
        Function<Point, Optional<List<Point>>> through = pin -> {
            TrackRouter.RoutedLeg coming = Precision.samePoint(before, pin) ? null : TrackRouter.routedLeg(TrackRouter.LegRequest.walked(world, track, net, before, pin, angleStep, index,
                    List.of(), ends).keepingClearOf(request.sweptPast()));
            TrackRouter.RoutedLeg going = Precision.samePoint(pin, after) ? null : TrackRouter.routedLeg(TrackRouter.LegRequest.walked(world, track, net, after, pin, angleStep, index,
                    List.of(), ends).keepingClearOf(request.sweptPast()));
            if ((coming != null && !coming.reached()) || (going != null && !going.reached())) {
                return Optional.empty();
            }
            List<Point> comingBack = new ArrayList<>();
            if (going != null) {
                List<Point> back = Polylines.reversed(going.points());
                comingBack.addAll(back.subList(1, back.size()));
                comingBack.add(after);
            }
            List<Point> band = Polylines.joined(line.subList(0, pull.from() + 1), coming == null ? List.of() : coming.points(), comingBack,
                    line.subList(pull.to() + 1, line.size()));
            List<Point> tight = new Tightening(fence, angleStep, Polylines.joined(List.of(pin), kept), settings.routing()).tightened(band);
            boolean shapely = !LineCleaning.isTangled(tight) && Polylines.passesThrough(tight, pin) && fence.lineClear(tight) && turnAt(tight, pin) <= angleStep + 1e-6;
            return shapely ? Optional.of(tight) : Optional.empty();
        };
        // The point goes towards the pointer as far as copper lets it, and no further than the band can follow it; set in whole
        // steps from where it was taken, so a point that has come to rest stands the same however far the pointer goes on.
        double dx = pull.pointer().x() - pull.taken().x();
        double dy = pull.pointer().y() - pull.taken().y();
        double distance = Math.hypot(dx, dy);
        double step = settings.carrying().bandStep();
        IntFunction<Point> pinAt = steps -> distance == 0 ? pull.taken()
                : Precision.settled(new Point(pull.taken().x() + dx / distance * steps * step, pull.taken().y() + dy / distance * steps * step));
        IntFunction<Optional<List<Point>>> bandAt = steps -> fence.lineClear(List.of(pull.taken(), pinAt.apply(steps)))
                ? through.apply(pinAt.apply(steps)) : Optional.empty();
        int low = 0;
        int high = (int) Math.floor(distance / step);
        Optional<List<Point>> tight = bandAt.apply(high);
        if (tight.isEmpty()) {
            tight = bandAt.apply(low);
            while (tight.isPresent() && high - low > 1) {
                int middle = (low + high) / 2;
                Optional<List<Point>> found = bandAt.apply(middle);
                if (found.isPresent()) {
                    tight = found;
                    low = middle;
                } else {
                    high = middle;
                }
            }
        }
        return tight.map(points -> withLine(world, index, points, fence));
    }

    /** How far a line turns at one of its corners, in degrees: 0 where it runs straight on, or where the point is no corner of it. */
    static double turnAt(List<Point> line, Point corner) {
        int at = -1;
        for (int index = 0; index < line.size(); index++) {
            if (Precision.samePoint(line.get(index), corner)) {
                at = index;
                break;
            }
        }
        if (at <= 0 || at >= line.size() - 1) {
            return 0;
        }
        Point previous = line.get(at - 1);
        Point next = line.get(at + 1);
        double inX = corner.x() - previous.x();
        double inY = corner.y() - previous.y();
        double outX = next.x() - corner.x();
        double outY = next.y() - corner.y();
        double lengths = Math.hypot(inX, inY) * Math.hypot(outX, outY);
        return lengths == 0 ? 0 : Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, (inX * outX + inY * outY) / lengths))));
    }

    /** The shape asked for at the largest share of the way that can be laid, found by halving; empty where none can. */
    static Optional<PcbDrawing> farthestHeld(ReshapeRequest request) {
        Optional<PcbDrawing> best = Halving.farthestMade(0, 1, request.world().settings().carrying().holdSteps(),
                share -> request.shapeAt().apply(share).flatMap(shape -> shapeLaid(request, shape, true)));
        return best;
    }
}
