package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.DoubleFunction;
import java.util.function.Function;

/**
 * The router: what every way of making copper comes to. Laying a track, carrying a segment or a corner, a part's tracks
 * following it, routing a whole pcb are each only a way of asking for legs and of saying which corners are held; how a leg
 * goes round what it meets, what gives way and how it comes out shaped is said once, here.
 *
 * <pre>world (what is there) → leg (the way, what gave way) → tightened (the shape)</pre>
 */
public final class TrackRouter {

    private TrackRouter() {
    }

    /** How a track meets other nets' copper, as KiCad's router has it: it walks round it, or their tracks are shoved aside. */
    public enum RoutingMode {
        WALKAROUND, SHOVE
    }

    /** How the reader lays and carries copper now: what gives way, the directions a track may take, what becomes of a jog. */
    public record LayingStyle(RoutingMode mode, double angleStep, Tightening.JogHandling jogs) {
    }

    /**
     * A stretch of track to be laid on a board.
     *
     * @param own         the track of the pcb this leg is part of: it is not in its own way
     * @param held        corners it must keep; its ends always stay
     * @param joins       where the track joins its own net's copper: only there may it stand nearer other copper than its gap, as
     *                    a track leaving a pad on a fine pitch does; empty for its start, and its end where the net's copper is
     * @param searchLimit the most grid points looked at
     * @param preferred   the way the layer's tracks run, and what a step against it costs
     */
    public record LegRequest(RoutingWorld world, String layer, Optional<String> net, double width, Point from, Point to, double angleStep,
                             RoutingMode mode, OptionalInt own, List<Point> held, Optional<List<Point>> joins, int searchLimit,
                             Optional<Pathfinder.Preferred> preferred) {

        /** A leg laid by hand: nothing of the pcb its own, nothing held, its joins where it starts and ends. */
        public static LegRequest of(RoutingWorld world, String layer, Optional<String> net, double width, Point from, Point to,
                                    double angleStep, RoutingMode mode, int searchLimit) {
            return new LegRequest(world, layer, net, width, from, to, angleStep, mode, OptionalInt.empty(), List.of(), Optional.empty(),
                    searchLimit, Optional.empty());
        }

        /** A leg of a track already on the pcb, walked round what it meets, joined at the given points. */
        public static LegRequest walked(RoutingWorld world, PcbDrawing.Track track, Optional<String> net, Point from, Point to, double angleStep,
                                        int own, List<Point> held, List<Point> joins, int searchLimit) {
            return new LegRequest(world, track.layer(), net, track.width(), from, to, angleStep, RoutingMode.WALKAROUND, OptionalInt.of(own),
                    held, Optional.of(joins), searchLimit, Optional.empty());
        }
    }

    /**
     * The way a leg goes.
     *
     * @param points  the points after its start
     * @param tracks  every track of the pcb as they stand after giving way; empty when none had to
     * @param reached whether it reaches its end; else it stands as far as it can go
     */
    public record RoutedLeg(List<Point> points, Optional<List<PcbDrawing.Track>> tracks, boolean reached) {
    }

    /** The straight step cut where it would first come too near copper: as far as a track can go towards what it cannot reach. */
    private static List<Point> stoppedShort(TrackFence fence, Point from, List<Point> straight, int halvings) {
        List<Point> points = Lines.joined(List.of(from), straight);
        for (int index = 1; index < points.size(); index++) {
            if (fence.lineClear(points.subList(index - 1, index + 1))) {
                continue;
            }
            Point start = points.get(index - 1);
            Point end = points.get(index);
            double low = 0;
            double high = 1;
            for (int halving = 0; halving < halvings; halving++) {
                double middle = (low + high) / 2;
                Point reached = new Point(start.x() + (end.x() - start.x()) * middle, start.y() + (end.y() - start.y()) * middle);
                if (fence.lineClear(List.of(start, reached))) {
                    low = middle;
                } else {
                    high = middle;
                }
            }
            Point stop = Lines.settled(new Point(start.x() + (end.x() - start.x()) * low, start.y() + (end.y() - start.y()) * low));
            List<Point> kept = Lines.withoutStraightPoints(Lines.joined(List.of(from), points.subList(1, index), List.of(stop)));
            return kept.size() > 1 ? List.copyOf(kept.subList(1, kept.size())) : List.of(from);
        }
        return straight;
    }

    /** The leg, by the way the reader lays tracks: straight where it is free, tracks shoved where they may be, round what cannot give way. */
    public static RoutedLeg routedLeg(LegRequest request) {
        RoutingWorld world = request.world();
        RoutingSettings.Routing settings = world.settings().routing();
        List<Point> joins = request.joins().orElseGet(() -> {
            boolean endOnNet = request.net().isPresent() && world.netAt(request.layer(), request.to()).equals(request.net());
            return endOnNet ? List.of(request.from(), request.to()) : List.of(request.from());
        });
        TrackFence fence = world.fence(request.layer(), request.net(), request.width(), request.own(), true).leaving(joins);
        List<Point> straight = Lines.trackStep(request.from(), request.to(), request.angleStep());
        List<Point> straightLine = Lines.joined(List.of(request.from()), straight);
        if (fence.lineClear(straightLine)) {
            return new RoutedLeg(straight, Optional.empty(), true);
        }
        if (request.mode() == RoutingMode.SHOVE) {
            TrackFence fixed = world.fence(request.layer(), request.net(), request.width(), request.own(), false).leaving(joins);
            if (fixed.lineClear(straightLine)) {
                Optional<List<PcbDrawing.Track>> tracks = Shoving.tracksGivingWay(world, request.layer(), request.net(),
                        RoutingWorld.copperOfLine(request.width(), straightLine), request.own());
                if (tracks.isPresent()) {
                    return new RoutedLeg(straight, tracks, true);
                }
            }
        }
        Optional<List<Point>> way = Pathfinder.way(fence, request.from(), request.to(), request.angleStep(), request.searchLimit(),
                request.preferred(), settings);
        if (way.isPresent()) {
            List<Point> tight = new Tightening(fence, request.angleStep(), request.held(), settings).tightened(way.get());
            return new RoutedLeg(List.copyOf(tight.subList(1, tight.size())), Optional.empty(), true);
        }
        return new RoutedLeg(stoppedShort(fence, request.from(), straight, settings.stopHalvings()), Optional.empty(), false);
    }

    /**
     * The way from a corner that stays to one carried, keeping the run it had: on along the line it ran, then in at a step's angle
     * to the corner, as copper is pulled aside rather than laid anew. So pulling a corner bends the track near it, and a long run
     * does not swing.
     */
    public static List<Point> stepKeepingRun(Point anchor, Point was, Point to, double angleStep) {
        Optional<Point> way = Lines.wayBetween(anchor, was);
        if (angleStep != 45 || way.isEmpty()) {
            return Lines.trackStep(anchor, to, angleStep);
        }
        Point along = way.get();
        double offLine = (to.x() - anchor.x()) * along.y() - (to.y() - anchor.y()) * along.x();
        if (Math.abs(offLine) < 1e-6) {
            return List.of(to);
        }
        double side = offLine > 0 ? -1 : 1;
        double half = Math.sqrt(0.5);
        Point inward = new Point((along.x() - side * along.y()) * half, (along.y() + side * along.x()) * half);
        Optional<Point> turn = Lines.crossingOf(anchor, along, to, inward);
        if (turn.isEmpty() || Lines.reachAlong(anchor, along, turn.get()) < 0) {
            return Lines.trackStep(anchor, to, angleStep);
        }
        // Pulled out past the corner it had, the run would go by it: it turns off that much before the corner instead, and comes
        // along beside where it ran. Both sides doing so, the corner stands out as a blunt bump, and nothing crosses.
        if (Lines.lengthBetween(anchor, turn.get()) >= Lines.lengthBetween(anchor, was) - 1e-6) {
            double off = Math.abs(offLine);
            Point leaving = Lines.offset(was, along, -off);
            Point beside = Lines.offset(leaving, inward, off * Math.sqrt(2));
            if (Lines.reachAlong(anchor, along, leaving) > 0) {
                return List.of(Lines.settled(leaving), Lines.settled(beside), to);
            }
        }
        return Lines.lengthBetween(anchor, turn.get()) < 1e-6 ? List.of(to) : List.of(Lines.settled(turn.get()), to);
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
     */
    public record ReshapeRequest(RoutingWorld world, int track, LayingStyle style, DoubleFunction<Optional<List<Point>>> shapeAt,
                                 Function<List<Point>, List<Point>> heldOf, Optional<Pull> pulled) {

        ReshapeRequest walking() {
            return new ReshapeRequest(world, track, new LayingStyle(RoutingMode.WALKAROUND, style.angleStep(), style.jogs()), shapeAt, heldOf, pulled);
        }
    }

    /** The legs of a shape that were no legs of the track: what the carrying changed. */
    private static boolean[] newStretches(PcbDrawing.Track track, List<Point> shape) {
        boolean[] changed = new boolean[shape.size() - 1];
        for (int index = 0; index < changed.length; index++) {
            Point from = shape.get(index);
            Point to = shape.get(index + 1);
            boolean had = false;
            for (int at = 1; at < track.points().size() && !had; at++) {
                had = Lines.samePoint(track.points().get(at - 1), from) && Lines.samePoint(track.points().get(at), to);
            }
            changed[index] = !had;
        }
        return changed;
    }

    /** A pcb with a track's line in place and its corners rounded as far as they still fit. */
    private static PcbDrawing withLine(List<PcbDrawing.Track> tracks, PcbDrawing pcb, int index, List<Point> line, TrackFence against) {
        List<PcbDrawing.Track> changed = new ArrayList<>(tracks);
        PcbDrawing.Track track = changed.get(index);
        changed.set(index, track.withPoints(line).withRounding(against.roundingThatFits(line, track.rounding(), track.layer())));
        return pcb.withTracks(changed);
    }

    /**
     * The shape laid on the board: as asked where it is free; with other nets' tracks shoved off it, the whole of it at once,
     * where they may be; else each new stretch walked round what it meets. Empty where one cannot reach its end.
     */
    private static Optional<PcbDrawing> shapeLaid(ReshapeRequest request, List<Point> shape, boolean walking) {
        RoutingWorld world = request.world();
        int index = request.track();
        PcbDrawing.Track track = world.pcb().tracks().get(index);
        Optional<String> net = world.netOfTrack(index);
        RoutingSettings settings = world.settings();
        boolean[] isNew = newStretches(track, shape);
        List<Point> held = request.heldOf().apply(shape);
        List<Point> ends = List.of(shape.getFirst(), shape.getLast());
        TrackFence fence = world.fence(track.layer(), net, track.width(), OptionalInt.of(index), true).leaving(ends);
        Tightening tightening = new Tightening(fence, request.style().angleStep(), held, settings.routing());
        Function<List<Point>, Function<List<PcbDrawing.Track>, Function<TrackFence, Optional<PcbDrawing>>>> allOf = line -> tracks -> against -> {
            // Each leg the router walked came pulled tight already; the whole is the person's, and only cleaned.
            List<Point> tight = tightening.against(against).untangled(line, request.style().jogs());
            // What cannot be cleaned here is no shape to show; nor is a line that cleaning cut what the reader holds out of.
            if (Lines.isTangled(tight) || !held.stream().allMatch(point -> Lines.passesThrough(tight, point))) {
                return Optional.empty();
            }
            return Optional.of(withLine(tracks, world.pcb(), index, tight, against));
        };
        List<List<Point>> stretches = new ArrayList<>();
        for (int at = 0; at < isNew.length; at++) {
            if (isNew[at]) {
                stretches.add(List.of(shape.get(at), shape.get(at + 1)));
            }
        }
        if (stretches.stream().allMatch(fence::lineClear)) {
            return allOf.apply(shape).apply(world.pcb().tracks()).apply(fence);
        }
        if (request.style().mode() == RoutingMode.SHOVE) {
            TrackFence fixed = world.fence(track.layer(), net, track.width(), OptionalInt.of(index), false).leaving(ends);
            if (stretches.stream().allMatch(fixed::lineClear)) {
                Optional<List<PcbDrawing.Track>> tracks = Shoving.tracksGivingWay(world, track.layer(), net,
                        RoutingWorld.copperOfLine(track.width(), shape), OptionalInt.of(index));
                if (tracks.isPresent()) {
                    return allOf.apply(shape).apply(tracks.get()).apply(fixed);
                }
            }
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
            RoutedLeg leg = routedLeg(LegRequest.walked(world, track, net, shape.get(at - 1), shape.get(at), request.style().angleStep(), index,
                    held, ends, settings.carrying().searchLimit()));
            if (!leg.reached()) {
                return Optional.empty();
            }
            line.addAll(leg.points());
        }
        // A walk goes round what is in the way, near it: one that came out much longer than the line asked for went round
        // something else — round a whole part, or the far end of a track — and is not the line the reader is pulling.
        if (Lines.lengthOf(line) > Lines.lengthOf(shape) * settings.carrying().longestWalk()) {
            return Optional.empty();
        }
        return allOf.apply(line).apply(world.pcb().tracks()).apply(fence);
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
        java.util.function.Supplier<Optional<PcbDrawing>> walked = () -> whole.flatMap(shape -> shapeLaid(request, shape, true));
        java.util.function.Supplier<Optional<PcbDrawing>> band = () -> request.pulled().flatMap(pull -> bandLaid(request, pull));
        Optional<PcbDrawing> laid = stopped ? band.get().or(walked) : walked.get().or(band);
        return laid.or(() -> farthestHeld(request));
    }

    /** Whether the point a track is pulled by gets all the way to the pointer with nothing in its way. */
    private static boolean pulledFreely(ReshapeRequest request, Pull pull) {
        RoutingWorld world = request.world();
        PcbDrawing.Track track = world.pcb().tracks().get(request.track());
        List<Point> line = track.points();
        return world.fence(track.layer(), world.netOfTrack(request.track()), track.width(), OptionalInt.of(request.track()), true)
                .leaving(List.of(line.getFirst(), line.getLast())).lineClear(List.of(pull.taken(), pull.pointer()));
    }

    /**
     * The track as a band pulled by the point it was taken by: that point carried straight towards the pointer and stopped where a
     * track standing on it would first come too near copper; then the band from the corner that stays before the stretch to the
     * point, and from the point to the corner after it, each the shortest way round what it meets, pulled tight. As the pointer
     * moves, the point moves with it, so the shape never jumps; and it never goes past what stopped it into a gap beyond.
     */
    private static Optional<PcbDrawing> bandLaid(ReshapeRequest request, Pull pull) {
        RoutingWorld world = request.world();
        int index = request.track();
        double angleStep = request.style().angleStep();
        PcbDrawing.Track track = world.pcb().tracks().get(index);
        Optional<String> net = world.netOfTrack(index);
        List<Point> line = track.points();
        if (pull.from() < 0 || pull.to() >= line.size()) {
            return Optional.empty();
        }
        Point before = line.get(pull.from());
        Point after = line.get(pull.to());
        List<Point> ends = List.of(line.getFirst(), line.getLast());
        TrackFence fence = world.fence(track.layer(), net, track.width(), OptionalInt.of(index), true).leaving(ends);
        if (!fence.pointClear(pull.taken())) {
            return Optional.empty();
        }
        RoutingSettings settings = world.settings();
        List<Point> kept = Lines.joined(line.subList(0, pull.from() + 1), line.subList(pull.to(), line.size()));
        Function<Point, Optional<List<Point>>> through = pin -> {
            RoutedLeg coming = Lines.samePoint(before, pin) ? null : routedLeg(LegRequest.walked(world, track, net, before, pin, angleStep, index,
                    List.of(), ends, settings.carrying().searchLimit()));
            RoutedLeg going = Lines.samePoint(pin, after) ? null : routedLeg(LegRequest.walked(world, track, net, after, pin, angleStep, index,
                    List.of(), ends, settings.carrying().searchLimit()));
            if ((coming != null && !coming.reached()) || (going != null && !going.reached())) {
                return Optional.empty();
            }
            List<Point> comingBack = new ArrayList<>();
            if (going != null) {
                List<Point> back = Lines.reversed(going.points());
                comingBack.addAll(back.subList(1, back.size()));
                comingBack.add(after);
            }
            List<Point> band = Lines.joined(line.subList(0, pull.from() + 1), coming == null ? List.of() : coming.points(), comingBack,
                    line.subList(pull.to() + 1, line.size()));
            List<Point> tight = new Tightening(fence, angleStep, Lines.joined(List.of(pin), kept), settings.routing()).tightened(band);
            boolean shapely = !Lines.isTangled(tight) && Lines.passesThrough(tight, pin) && fence.lineClear(tight) && turnAt(tight, pin) <= angleStep + 1e-6;
            return shapely ? Optional.of(tight) : Optional.empty();
        };
        // The point goes towards the pointer as far as copper lets it, and no further than the band can follow it; set in whole
        // steps from where it was taken, so a point that has come to rest stands the same however far the pointer goes on.
        double dx = pull.pointer().x() - pull.taken().x();
        double dy = pull.pointer().y() - pull.taken().y();
        double distance = Math.hypot(dx, dy);
        double step = settings.carrying().bandStep();
        java.util.function.IntFunction<Point> pinAt = steps -> distance == 0 ? pull.taken()
                : Lines.settled(new Point(pull.taken().x() + dx / distance * steps * step, pull.taken().y() + dy / distance * steps * step));
        java.util.function.IntFunction<Optional<List<Point>>> bandAt = steps -> fence.lineClear(List.of(pull.taken(), pinAt.apply(steps)))
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
        return tight.map(points -> withLine(world.pcb().tracks(), world.pcb(), index, points, fence));
    }

    /** How far a line turns at one of its corners, in degrees: 0 where it runs straight on, or where the point is no corner of it. */
    private static double turnAt(List<Point> line, Point corner) {
        int at = -1;
        for (int index = 0; index < line.size(); index++) {
            if (Lines.samePoint(line.get(index), corner)) {
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
    private static Optional<PcbDrawing> farthestHeld(ReshapeRequest request) {
        double low = 0;
        double high = 1;
        Optional<PcbDrawing> best = Optional.empty();
        ReshapeRequest walking = request.walking();
        for (int halving = 0; halving < request.world().settings().carrying().holdSteps(); halving++) {
            double share = (low + high) / 2;
            Optional<PcbDrawing> held = request.shapeAt().apply(share).flatMap(shape -> shapeLaid(walking, shape, true));
            if (held.isPresent()) {
                best = held;
                low = share;
            } else {
                high = share;
            }
        }
        return best;
    }
}
