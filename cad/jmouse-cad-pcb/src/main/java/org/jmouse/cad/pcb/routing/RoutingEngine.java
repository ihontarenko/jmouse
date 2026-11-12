package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

/**
 * What an editor asks of the router for one pcb: everything a person does to copper by hand, as one thing. A drag is asked of
 * the layout it began on, frame after frame, and what is worked out of that layout is kept for the whole of it. One request at
 * a time: the methods are the engine's, and a board's engine works one of them through before the next.
 */
public final class RoutingEngine {

    /** How many layouts of the board are kept worked out: a drag's start, and the frames it is asked of. */
    private static final int KEPT_WORLDS = 8;

    private final RoutingBoard                   board;
    private final Map<PcbDrawing, RoutingWorld>  worlds = new LinkedHashMap<>(KEPT_WORLDS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PcbDrawing, RoutingWorld> eldest) {
            return size() > KEPT_WORLDS;
        }
    };

    public RoutingEngine(RoutingBoard board) {
        this.board = board;
    }

    public RoutingBoard board() {
        return board;
    }

    private RoutingSettings settings() {
        return board.settings();
    }

    /** The board with this layout on it, worked out once. */
    public synchronized RoutingWorld worldOf(PcbDrawing pcb) {
        return worlds.computeIfAbsent(pcb, layout -> new RoutingWorld(layout, board));
    }

    /**
     * A leg as it is laid: the way it goes, and the corners it is drawn with — those the reader asked for as far as the leg's
     * gaps let them, for a corner rounded is copper nearer what stands inside it than the sharp one the way was found by.
     */
    public record LaidLeg(TrackRouter.RoutedLeg leg, double rounding) {
    }

    /** Where a track being laid goes, from its last corner toward the pointer, with the corners it may be drawn with. */
    public synchronized LaidLeg leg(PcbDrawing pcb, String layer, Optional<String> net, double width, Point from, Point to, double angleStep,
                                    TrackRouter.RoutingMode mode, double rounding) {
        TrackRouter.RoutedLeg leg = TrackRouter.routedLeg(TrackRouter.LegRequest.of(worldOf(pcb), layer, net, width, from, to, angleStep, mode,
                settings().carrying().searchLimit()));
        List<Point> line = Lines.joined(List.of(from), leg.points());
        RoutingWorld laidOn = leg.tracks().map(tracks -> worldOf(pcb.withTracks(tracks))).orElse(worldOf(pcb));
        double fits = laidOn.fence(layer, net, width).leaving(List.of(from, line.getLast())).roundingThatFits(line, rounding, layer);
        return new LaidLeg(leg, fits);
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
        JoinedTracks.JoinedLine joined = JoinedTracks.joinedLine(worldOf(pcb), index);
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
    public synchronized Optional<PcbDrawing> segmentCarried(PcbDrawing pcb, int index, int own, double dx, double dy,
                                                            TrackRouter.LayingStyle style, Optional<Point> pressed) {
        if (index < 0 || index >= pcb.tracks().size() || own < 0 || own >= pcb.tracks().get(index).points().size() - 1) {
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
            TrackRouter.Pull pull = new TrackRouter.Pull(taken, pointer, Math.max(0, segment - 1), Math.min(old.size() - 1, segment + 2));
            return TrackRouter.reshapedTrack(new TrackRouter.ReshapeRequest(worldOf(working), index, style,
                    share -> StretchShift.shifted(old, segment, dx * share, dy * share, style.angleStep()).map(Lines::withoutFolds),
                    // What is held is the segment where it now stands: the corners that were not on the track before.
                    shape -> shape.stream().filter(point -> old.stream().noneMatch(was -> Lines.samePoint(was, point))).toList(),
                    Optional.of(pull)));
        });
    }

    /** The pcb with a track's corner carried to a point, the runs either side of it kept. */
    public synchronized PcbDrawing cornerCarried(PcbDrawing pcb, int index, int own, Point to, TrackRouter.LayingStyle style) {
        if (index < 0 || index >= pcb.tracks().size()) {
            return pcb;
        }
        return onJoinedLine(pcb, index, own, (working, old, corner, reversed) -> {
            if (corner < 0 || corner >= old.size()) {
                return Optional.empty();
            }
            Point standing = old.get(corner);
            Optional<Point> before = corner > 0 ? Optional.of(old.get(corner - 1)) : Optional.empty();
            Optional<Point> after = corner + 1 < old.size() ? Optional.of(old.get(corner + 1)) : Optional.empty();
            java.util.function.DoubleFunction<Point> target = share -> Lines.settled(new Point(standing.x() + (to.x() - standing.x()) * share,
                    standing.y() + (to.y() - standing.y()) * share));
            return TrackRouter.reshapedTrack(new TrackRouter.ReshapeRequest(worldOf(working), index, style, share -> {
                Point at = target.apply(share);
                List<Point> coming = before.map(point -> TrackRouter.stepKeepingRun(point, standing, at, style.angleStep())).orElse(List.of(at));
                List<Point> going = after.map(point -> leavingCorner(at, before.map(previous -> Lines.joined(List.of(previous), coming)).orElse(coming),
                        point, standing, style.angleStep())).orElse(List.of());
                return Optional.of(Lines.withoutStraightPoints(Lines.joined(old.subList(0, Math.max(0, corner - 1)),
                        before.map(List::of).orElse(List.of()), coming, going, old.subList(Math.min(old.size(), corner + 2), old.size()))));
            }, shape -> List.of(target.apply(1)), Optional.empty()));
        }).orElse(pcb);
    }

    /**
     * How a line goes on from a corner carried to where it is, to the corner that stayed after it. The first of these that is not
     * tangled, in the order of how little they change: in along that corner's run; square to how it came until it meets the run;
     * across for a little, a shoulder, and down to the run at 45° or square. So the corner stays under the pointer wherever it is
     * pulled, and no spike is drawn.
     */
    private List<Point> leavingCorner(Point at, List<Point> coming, Point after, Point standing, double angleStep) {
        List<Point> kept = Lines.reversed(TrackRouter.stepKeepingRun(after, standing, at, angleStep));
        List<Point> plain = Lines.joined(kept.subList(1, kept.size()), List.of(after));
        Optional<Point> before = coming.size() > 1 ? Optional.of(coming.get(coming.size() - 2)) : Optional.empty();
        Optional<Point> into = before.flatMap(point -> Lines.wayBetween(point, at));
        Optional<Point> outOf = Lines.wayBetween(at, plain.getFirst());
        Optional<Point> run = Lines.wayBetween(after, standing);
        if (before.isEmpty() || into.isEmpty() || outOf.isEmpty() || run.isEmpty()) {
            return plain;
        }
        double towards = Lines.cross(into.get(), outOf.get()) > 0 ? 1 : -1;
        double stub = Math.min(settings().carrying().endStub(), Lines.lengthBetween(at, standing));
        List<List<Point>> shapes = new ArrayList<>();
        shapes.add(plain);
        shapes.addAll(shapesTo(towards, at, into.get(), after, run.get(), stub, angleStep));
        shapes.addAll(shapesTo(-towards, at, into.get(), after, run.get(), stub, angleStep));
        // The first in order that is not tangled and is no way round for nothing: back to the run, not past it and out again.
        double direct = Lines.lengthBetween(at, after) * settings().carrying().widestDetour() + stub;
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

    /** The pcb's tracks with their jogs merged, those chosen or every one where none is, each only where the corner left keeps its gaps. */
    public synchronized PcbDrawing withoutJogs(PcbDrawing pcb, List<Integer> chosen) {
        RoutingWorld world = worldOf(pcb);
        List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
        for (int index = 0; index < tracks.size(); index++) {
            if (!chosen.isEmpty() && !chosen.contains(index)) {
                continue;
            }
            PcbDrawing.Track track = tracks.get(index);
            List<Point> line = track.points();
            TrackFence fence = world.fence(track.layer(), world.netOfTrack(index), track.width(), OptionalInt.of(index), true)
                    .leaving(List.of(line.getFirst(), line.getLast()));
            List<Point> merged = new Tightening(fence, 45, List.of(), settings().routing()).withoutShortJogs(line, false);
            if (merged.size() < line.size()) {
                tracks.set(index, track.withPoints(merged));
            }
        }
        return pcb.withTracks(tracks);
    }

    /** The pcb's tracks with their corners rounded so, those chosen or every one where none is, each only as far as its gaps let it. */
    public synchronized PcbDrawing withCorners(PcbDrawing pcb, double rounding, List<Integer> chosen) {
        RoutingWorld world = worldOf(pcb);
        List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
        for (int index = 0; index < tracks.size(); index++) {
            if (!chosen.isEmpty() && !chosen.contains(index)) {
                continue;
            }
            PcbDrawing.Track track = tracks.get(index);
            TrackFence fence = world.fence(track.layer(), world.netOfTrack(index), track.width(), OptionalInt.of(index), true)
                    .leaving(List.of(track.points().getFirst(), track.points().getLast()));
            tracks.set(index, track.withRounding(fence.roundingThatFits(track.points(), rounding, track.layer())));
        }
        return pcb.withTracks(tracks);
    }

    /** The track under a point on a layer bent to a length, the meander where it was pointed at; or why it cannot be. */
    public synchronized LengthTuning.Tuning tunedAt(PcbDrawing pcb, String layer, Point point, double reach, java.util.OptionalDouble length) {
        return new LengthTuning(worldOf(pcb)).tunedAt(layer, point, reach, length);
    }

    /** A chosen track bent to a length wherever on it a meander fits best; or why it cannot be. */
    public synchronized LengthTuning.Tuning tunedAnywhere(PcbDrawing pcb, int track, java.util.OptionalDouble length) {
        return new LengthTuning(worldOf(pcb)).tunedAnywhere(track, length);
    }

    /** Tracks a moved part pulled, each laid from the end that moved to the corner that stayed, for as long as a frame allows. */
    public synchronized PcbDrawing tracksFollowing(PcbDrawing before, PcbDrawing moved, double angleStep) {
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(settings().carrying().frameBudget());
        return laidAfresh(before, moved, angleStep, until, settings().carrying().searchLimit());
    }

    /** The same when the part is let go: all of them, however long it takes, and what the part now stands on giving way. */
    public synchronized PcbDrawing tracksLaid(PcbDrawing before, PcbDrawing moved, double angleStep) {
        return madeWayFor(laidAfresh(before, moved, angleStep, Long.MAX_VALUE, settings().routing().searchLimit()), angleStep);
    }

    /** Which ends of a track went with what moved, of a track that moved at one end only; empty otherwise. */
    private static Optional<Boolean> pulledByStart(PcbDrawing.Track track, PcbDrawing.Track was) {
        if (was == null || track.equals(was) || track.points().size() < 2) {
            return Optional.empty();
        }
        boolean start = !Lines.samePoint(track.points().getFirst(), was.points().getFirst());
        boolean end = !Lines.samePoint(track.points().getLast(), was.points().getLast());
        return start != end ? Optional.of(start) : Optional.empty();
    }

    /**
     * Each track a drag changed laid afresh from the end that moved. A pulled track is only stretched until then, and stretched
     * copper may run over a pad and seem to short nets: what the board is measured by has each such track drawn in to a point at
     * its moved end, so its net is its pad's and it is in nobody's way. Each one laid goes on the board for the next to go round.
     */
    private PcbDrawing laidAfresh(PcbDrawing before, PcbDrawing moved, double angleStep, long until, int searchLimit) {
        List<Optional<Boolean>> pulled = new ArrayList<>();
        List<PcbDrawing.Track> drawn = new ArrayList<>();
        for (int index = 0; index < moved.tracks().size(); index++) {
            PcbDrawing.Track track = moved.tracks().get(index);
            Optional<Boolean> byStart = pulledByStart(track, index < before.tracks().size() ? before.tracks().get(index) : null);
            pulled.add(byStart);
            drawn.add(byStart.map(start -> {
                Point end = start ? track.points().getFirst() : track.points().getLast();
                return track.withPoints(List.of(end, end));
            }).orElse(track));
        }
        PcbDrawing pcb = moved.withTracks(drawn);
        for (int index = 0; index < moved.tracks().size(); index++) {
            if (pulled.get(index).isEmpty()) {
                continue;
            }
            boolean start = pulled.get(index).get();
            PcbDrawing.Track track = moved.tracks().get(index);
            List<Point> path = start ? track.points() : Lines.reversed(track.points());
            PcbDrawing.Track was = before.tracks().get(index);
            int stayed = -1;
            for (int position = 1; position < path.size() && stayed < 0; position++) {
                Point point = path.get(position);
                if (was.points().stream().anyMatch(old -> Lines.samePoint(old, point))) {
                    stayed = position;
                }
            }
            RoutingWorld world = worldOf(pcb);
            Optional<String> net = world.netOfTrack(index);
            List<Point> ends = List.of(path.getFirst(), path.getLast());
            TrackFence fence = world.fence(track.layer(), net, track.width(), OptionalInt.of(index), true).leaving(ends);
            Tightening tightening = new Tightening(fence, angleStep, List.of(), settings().routing());
            int trackIndex = index;
            // Out of time for a frame, only the search goes: the shape is still worked out, so what is shown does not jump.
            java.util.function.Function<Point, TrackRouter.RoutedLeg> legTo = to -> TrackRouter.routedLeg(TrackRouter.LegRequest.walked(world, track,
                    net, path.getFirst(), to, angleStep, trackIndex, List.of(), ends, System.nanoTime() > until ? 0 : searchLimit));
            // On to the corner that stayed, the rest as it was; where that does not reach, or leaves it tangled, laid again from end
            // to end. Pulled tight either way.
            List<Point> whole = null;
            if (stayed > 0) {
                TrackRouter.RoutedLeg near = legTo.apply(path.get(stayed));
                if (near.reached()) {
                    List<Point> kept = tightening.tightened(Lines.joined(List.of(path.getFirst()), near.points(), path.subList(stayed + 1, path.size())));
                    whole = !Lines.isTangled(kept) && fence.lineClear(kept) ? kept : null;
                }
            }
            if (whole == null) {
                TrackRouter.RoutedLeg far = legTo.apply(path.getLast());
                whole = far.reached() ? tightening.tightened(Lines.joined(List.of(path.getFirst()), far.points()))
                        : Lines.withoutFolds(Lines.withoutLoops(path));
            }
            pcb = pcb.withTrackPoints(index, start ? whole : Lines.reversed(whole));
        }
        return pcb;
    }

    /**
     * What the part now stands on gives way: each track of another net it comes too near, laid again from end to end round it,
     * one after another on the board as it then stands, as a person clears the place a part was put down in.
     */
    private PcbDrawing madeWayFor(PcbDrawing moved, double angleStep) {
        PcbDrawing pcb = moved;
        int count = moved.tracks().size();
        // Twice: a track laid round the part may stand in the way of one already looked at.
        for (int position = 0; position < count * 2; position++) {
            int index = position % count;
            RoutingWorld world = worldOf(pcb);
            PcbDrawing.Track track = pcb.tracks().get(index);
            Optional<String> net = world.netOfTrack(index);
            List<Point> line = track.points();
            List<Point> ends = List.of(line.getFirst(), line.getLast());
            TrackFence fence = world.fence(track.layer(), net, track.width(), OptionalInt.of(index), true).leaving(ends);
            if (fence.lineClear(line)) {
                continue;
            }
            TrackRouter.RoutedLeg leg = TrackRouter.routedLeg(TrackRouter.LegRequest.walked(world, track, net, line.getFirst(), line.getLast(),
                    angleStep, index, List.of(), ends, settings().routing().searchLimit()));
            if (leg.reached()) {
                List<Point> points = new Tightening(fence, angleStep, List.of(), settings().routing())
                        .tightened(Lines.joined(List.of(line.getFirst()), leg.points()));
                pcb = pcb.withTrackPoints(index, points);
            }
        }
        return pcb;
    }

    /** The point of a stretch nearest a point: where on it the reader took it. */
    private static Point nearestOnStretch(Point point, Point from, Point to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double length = dx * dx + dy * dy;
        double share = length == 0 ? 0 : Math.max(0, Math.min(1, ((point.x() - from.x()) * dx + (point.y() - from.y()) * dy) / length));
        return new Point(from.x() + dx * share, from.y() + dy * share);
    }
}
