package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
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
 * A track reshaped by the hand, as KiCad drags one: a segment carried, its neighbours following; a corner carried, the runs either
 * side of it kept. A line joined of several tracks is reshaped as one. What it asks of the board — what is free, a leg round what
 * is in the way — it asks of the router ({@link TrackRouter}); the shape is said here.
 */
final class TrackReshaping {

    private final RoutingEngine engine;

    TrackReshaping(RoutingEngine engine) {
        this.engine = engine;
    }

    /** A track drag worked out on the line the track is joined into, or the track itself. */
    private interface LineReshaping {
        Optional<PcbDrawing> reshaped(PcbDrawing working, List<Point> line, int at, boolean reversed);
    }

    /**
     * A drag of a track worked on the whole line it is joined into: the line stands in the track's place while the drag is
     * worked out, and each track of it takes its own part of the answer back.
     */
    private Optional<PcbDrawing> onJoinedLine(PcbDrawing pcb, int index, int own, LineReshaping reshaping) {
        JoinedTracks.JoinedLine joined = JoinedTracks.joinedLine(engine.worldOf(pcb), index);
        int position = 0;
        while (joined.members().get(position).index() != index) {
            position++;
        }
        JoinedTracks.Member member = joined.members().get(position);
        int count = pcb.tracks().get(index).points().size();
        int at = joined.starts().get(position) + (member.reversed() ? count - 1 - own : own);
        PcbDrawing working = joined.members().size() == 1 ? pcb : pcb.withTrackPoints(index, joined.points());
        Optional<PcbDrawing> answer = reshaping.reshaped(working, joined.points(), at, member.reversed());
        if (answer.isPresent() && joined.members().size() > 1) {
            return Optional.of(JoinedTracks.withJoinedLine(answer.get(), joined, answer.get().tracks().get(index).points()));
        }
        return answer;
    }

    /** The pcb with a track's segment carried by (dx, dy), its neighbours following; empty where it cannot move at all. */
    Optional<PcbDrawing> segmentCarried(PcbDrawing pcb, int index, int own, double dx, double dy,
                                                            TrackRouter.LayingStyle style, Optional<Point> pressed) {
        if (index < 0 || index >= pcb.tracks().size() || own < 0 || own >= pcb.tracks().get(index).points().size() - 1 || RoutingEngine.held(pcb, index)) {
            return Optional.empty();
        }
        // On a track that runs against the line, its stretch ends at the point its own stretch starts from.
        return onJoinedLine(pcb, index, own, (working, old, start, reversed) -> {
            int segment = reversed ? start - 1 : start;
            Point from = old.get(segment);
            Point to = old.get(segment + 1);
            // Taken by the point of the stretch under where it was pressed: the point of the band the pointer carries. A stretch is
            // pulled across itself, as it is shifted in free space, so it presses on what stands in front of it.
            Point taken = nearestOnStretch(pressed.orElse(new Point((from.x() + to.x()) / 2, (from.y() + to.y()) / 2)), from, to);
            Optional<Point> along = Lines.wayBetween(from, to);
            Point pointer = along.map(way -> {
                double across = -way.y() * dx + way.x() * dy;
                return Lines.settled(new Point(taken.x() - way.y() * across, taken.y() + way.x() * across));
            }).orElse(Lines.settled(new Point(taken.x() + dx, taken.y() + dy)));
            Pull pull = new Pull(taken, pointer, Math.max(0, segment - 1), Math.min(old.size() - 1, segment + 2));
            RoutingWorld world = engine.worldOf(working);
            return reshapedTrack(new ReshapeRequest(world, index, style,
                    share -> StretchShift.shifted(old, segment, dx * share, dy * share, style.angleStep()).map(Lines::withoutFolds),
                    // What is held is the segment where it now stands: the corners that were not on the track before.
                    shape -> shape.stream().filter(point -> old.stream().noneMatch(was -> Lines.samePoint(was, point))).toList(),
                    Optional.of(pull), sweptPast(world, index, from, to, new Point(pointer.x() - taken.x(), pointer.y() - taken.y()))));
        });
    }

    /** The pcb with a track's corner carried to a point, the runs either side of it kept. */
    PcbDrawing cornerCarried(PcbDrawing pcb, int index, int own, Point to, TrackRouter.LayingStyle style) {
        if (index < 0 || index >= pcb.tracks().size() || RoutingEngine.held(pcb, index)) {
            return pcb;
        }
        return onJoinedLine(pcb, index, own, (working, old, corner, reversed) -> {
            if (corner < 0 || corner >= old.size()) {
                return Optional.empty();
            }
            Point standing = old.get(corner);
            Optional<Point> before = corner > 0 ? Optional.of(old.get(corner - 1)) : Optional.empty();
            Optional<Point> after = corner + 1 < old.size() ? Optional.of(old.get(corner + 1)) : Optional.empty();
            DoubleFunction<Point> target = share -> Lines.settled(new Point(standing.x() + (to.x() - standing.x()) * share,
                    standing.y() + (to.y() - standing.y()) * share));
            return reshapedTrack(new ReshapeRequest(engine.worldOf(working), index, style, share -> {
                Point at = target.apply(share);
                List<Point> coming = before.map(point -> Lines.stepKeepingRun(point, standing, at, style.angleStep())).orElse(List.of(at));
                List<Point> going = after.map(point -> leavingCorner(at, before.map(previous -> Lines.joined(List.of(previous), coming)).orElse(coming),
                        point, standing, style.angleStep())).orElse(List.of());
                return Optional.of(Lines.withoutStraightPoints(Lines.joined(old.subList(0, Math.max(0, corner - 1)),
                        before.map(List::of).orElse(List.of()), coming, going, old.subList(Math.min(old.size(), corner + 2), old.size()))));
            }, shape -> List.of(target.apply(1)), Optional.empty(), List.of()));
        }).orElse(pcb);
    }

    /**
     * How a line goes on from a corner carried to where it is, to the corner that stayed after it. The first of these that is not
     * tangled, in the order of how little they change: in along that corner's run; square to how it came until it meets the run;
     * across for a little, a shoulder, and down to the run at 45° or square. So the corner stays under the pointer wherever it is
     * pulled, and no spike is drawn.
     */
    private List<Point> leavingCorner(Point at, List<Point> coming, Point after, Point standing, double angleStep) {
        List<Point> kept = Lines.reversed(Lines.stepKeepingRun(after, standing, at, angleStep));
        List<Point> plain = Lines.joined(kept.subList(1, kept.size()), List.of(after));
        Optional<Point> before = coming.size() > 1 ? Optional.of(coming.get(coming.size() - 2)) : Optional.empty();
        Optional<Point> into = before.flatMap(point -> Lines.wayBetween(point, at));
        Optional<Point> outOf = Lines.wayBetween(at, plain.getFirst());
        Optional<Point> run = Lines.wayBetween(after, standing);
        if (before.isEmpty() || into.isEmpty() || outOf.isEmpty() || run.isEmpty()) {
            return plain;
        }
        double towards = Lines.cross(into.get(), outOf.get()) > 0 ? 1 : -1;
        double stub = Math.min(engine.board().settings().carrying().endStub(), Lines.lengthBetween(at, standing));
        List<List<Point>> shapes = new ArrayList<>();
        shapes.add(plain);
        shapes.addAll(shapesTo(towards, at, into.get(), after, run.get(), stub, angleStep));
        shapes.addAll(shapesTo(-towards, at, into.get(), after, run.get(), stub, angleStep));
        // The first in order that is not tangled and is no way round for nothing: back to the run, not past it and out again.
        double direct = Lines.lengthBetween(at, after) * engine.board().settings().carrying().widestDetour() + stub;
        List<List<Point>> fit = shapes.stream()
                .filter(shape -> !Lines.isTangled(Lines.withoutStraightPoints(Lines.joined(List.of(before.get(), at), shape)))).toList();
        return fit.stream().filter(shape -> Lines.lengthOf(Lines.joined(List.of(at), shape)) <= direct).findFirst()
                .orElse(fit.isEmpty() ? plain : fit.getLast());
    }

    /** To one side, the shapes a line may go on by from a carried corner to its run. */
    private static List<List<Point>> shapesTo(double side, Point at, Point into, Point after, Point run, double stub, double angleStep) {
        Point across = new Point(-into.y() * side, into.x() * side);
        Point shoulder = Lines.settled(Lines.offset(at, across, stub));
        List<List<Point>> shapes = new ArrayList<>();
        onRun(at, across, after, run).ifPresent(point -> shapes.add(List.of(point, after)));
        if (angleStep != 90) {
            double half = Math.sqrt(0.5);
            onRun(shoulder, new Point((across.x() - into.x()) * half, (across.y() - into.y()) * half), after, run)
                    .ifPresent(point -> shapes.add(List.of(shoulder, point, after)));
        }
        onRun(shoulder, new Point(-into.x(), -into.y()), after, run).ifPresent(point -> shapes.add(List.of(shoulder, point, after)));
        shapes.add(Lines.joined(List.of(shoulder), Lines.trackStep(shoulder, after, angleStep)));
        return shapes;
    }

    /** Where a way from a point meets the run going back from the corner that stayed, ahead of both. */
    private static Optional<Point> onRun(Point from, Point way, Point after, Point run) {
        return Lines.crossingOf(from, way, after, run)
                .filter(met -> Lines.reachAlong(from, way, met) > 0 && Lines.reachAlong(after, run, met) >= 0).map(Lines::settled);
    }

    /** The point of a stretch nearest a point: where on it the reader took it. */
    private static Point nearestOnStretch(Point point, Point from, Point to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double length = dx * dx + dy * dy;
        double share = length == 0 ? 0 : Math.max(0, Math.min(1, ((point.x() - from.x()) * dx + (point.y() - from.y()) * dy) / length));
        return new Point(from.x() + dx * share, from.y() + dy * share);
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
    private static List<TrackFence.Obstacle> sweptPast(RoutingWorld world, int index, Point from, Point to, Point way) {
        double pulled = Math.hypot(way.x(), way.y());
        if (pulled == 0) {
            return List.of();
        }
        PcbDrawing.Track track = world.pcb().tracks().get(index);
        Optional<String> net = world.netOfTrack(index);
        double alongX = to.x() - from.x();
        double alongY = to.y() - from.y();
        double reach = pulled + Math.hypot(alongX, alongY);
        Point unit = new Point(way.x() / pulled, way.y() / pulled);
        CopperShape pulledTo = RoutingWorld.copperOfLine(track.width(), List.of(new Point(from.x() + way.x(), from.y() + way.y()),
                new Point(to.x() + way.x(), to.y() + way.y())));
        List<TrackFence.Obstacle> shadows = new ArrayList<>();
        for (TrackFence.Obstacle obstacle : world.obstacles(track.layer(), net, OptionalInt.of(index), OptionalDouble.empty())) {
            CopperShape.Box box = obstacle.shape().box();
            Point middle = new Point((box.left() + box.right()) / 2, (box.top() + box.bottom()) / 2);
            // Only what the stretch is pulled onto: copper it swept and passed clear of is behind it, nothing to be caught on.
            if (!sweptOver(middle, from, alongX, alongY, way) || TrackFence.keepsClear(pulledTo, obstacle.shape(), obstacle.gap())) {
                continue;
            }
            double half = Math.max(box.right() - box.left(), box.bottom() - box.top()) / 2;
            shadows.add(new TrackFence.Obstacle(CopperShape.line(List.of(middle, Lines.offset(middle, unit, reach)), half), obstacle.gap()));
        }
        return shadows;
    }

    /** Whether a point lies in the room a stretch swept: {@code from + s·along + t·way}, both shares between 0 and 1. */
    private static boolean sweptOver(Point point, Point from, double alongX, double alongY, Point way) {
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
     * The shape laid on the board: as asked where it is free; else each new stretch walked round what it meets. Empty where one
     * cannot reach its end.
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
        TrackFence fence = world.fence(track.layer(), net, track.width(), OptionalInt.of(index)).leaving(ends);
        Tightening tightening = new Tightening(fence, request.style().angleStep(), held, settings.routing());
        Function<List<Point>, Optional<PcbDrawing>> allOf = line -> {
            // Each leg the router walked came pulled tight already; the whole is the person's, and only cleaned.
            List<Point> tight = tightening.untangled(line, request.style().jogs());
            // What cannot be cleaned here is no shape to show; nor is a line that cleaning cut what the reader holds out of.
            if (Lines.isTangled(tight) || !held.stream().allMatch(point -> Lines.passesThrough(tight, point))) {
                return Optional.empty();
            }
            return Optional.of(withLine(world.pcb().tracks(), world.pcb(), index, tight, fence));
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
            double stretch = Lines.lengthOf(List.of(shape.get(at - 1), shape.get(at)));
            double budget = Lines.lengthOf(shape) * settings.carrying().longestWalk() - (Lines.lengthOf(shape) - stretch);
            TrackRouter.RoutedLeg leg = TrackRouter.routedLeg(TrackRouter.LegRequest.walked(world, track, net, shape.get(at - 1), shape.get(at), request.style().angleStep(), index,
                    held, ends).keepingClearOf(request.sweptPast()).noLongerThan(budget));
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
    private static boolean pulledFreely(ReshapeRequest request, Pull pull) {
        RoutingWorld world = request.world();
        PcbDrawing.Track track = world.pcb().tracks().get(request.track());
        return world.fenceOfTrack(track, request.track(), world.netOfTrack(request.track())).lineClear(List.of(pull.taken(), pull.pointer()));
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
        TrackFence fence = world.fence(track.layer(), net, track.width(), OptionalInt.of(index)).leaving(ends);
        if (!fence.pointClear(pull.taken())) {
            return Optional.empty();
        }
        RoutingSettings settings = world.settings();
        List<Point> kept = Lines.joined(line.subList(0, pull.from() + 1), line.subList(pull.to(), line.size()));
        Function<Point, Optional<List<Point>>> through = pin -> {
            TrackRouter.RoutedLeg coming = Lines.samePoint(before, pin) ? null : TrackRouter.routedLeg(TrackRouter.LegRequest.walked(world, track, net, before, pin, angleStep, index,
                    List.of(), ends).keepingClearOf(request.sweptPast()));
            TrackRouter.RoutedLeg going = Lines.samePoint(pin, after) ? null : TrackRouter.routedLeg(TrackRouter.LegRequest.walked(world, track, net, after, pin, angleStep, index,
                    List.of(), ends).keepingClearOf(request.sweptPast()));
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
        IntFunction<Point> pinAt = steps -> distance == 0 ? pull.taken()
                : Lines.settled(new Point(pull.taken().x() + dx / distance * steps * step, pull.taken().y() + dy / distance * steps * step));
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
        for (int halving = 0; halving < request.world().settings().carrying().holdSteps(); halving++) {
            double share = (low + high) / 2;
            Optional<PcbDrawing> held = request.shapeAt().apply(share).flatMap(shape -> shapeLaid(request, shape, true));
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
