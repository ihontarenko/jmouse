package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;


import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleFunction;

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
            Point taken = Stretches.nearestOnStretch(pressed.orElse(from.halfwayTo(to)), from, to);
            Optional<Point> along = Ways.wayBetween(from, to);
            Point pointer = along.map(way -> {
                double across = -way.y() * dx + way.x() * dy;
                return Precision.settled(new Point(taken.x() - way.y() * across, taken.y() + way.x() * across));
            }).orElse(Precision.settled(new Point(taken.x() + dx, taken.y() + dy)));
            TrackPulling.Pull pull = new TrackPulling.Pull(taken, pointer, Math.max(0, segment - 1), Math.min(old.size() - 1, segment + 2));
            RoutingWorld world = engine.worldOf(working);
            return TrackPulling.reshapedTrack(new TrackPulling.ReshapeRequest(world, index, style,
                    share -> StretchShift.shifted(old, segment, dx * share, dy * share, style.angleStep()).map(LineCleaning::withoutFolds),
                    // What is held is the segment where it now stands: the corners that were not on the track before.
                    shape -> shape.stream().filter(point -> old.stream().noneMatch(was -> Precision.samePoint(was, point))).toList(),
                    Optional.of(pull), TrackPulling.sweptPast(world, index, from, to, new Point(pointer.x() - taken.x(), pointer.y() - taken.y()))));
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
            DoubleFunction<Point> target = share -> Precision.settled(standing.toward(to, share));
            return TrackPulling.reshapedTrack(new TrackPulling.ReshapeRequest(engine.worldOf(working), index, style, share -> {
                Point at = target.apply(share);
                List<Point> coming = before.map(point -> AngleSteps.stepKeepingRun(point, standing, at, style.angleStep())).orElse(List.of(at));
                List<Point> going = after.map(point -> leavingCorner(at, before.map(previous -> Polylines.joined(List.of(previous), coming)).orElse(coming),
                        point, standing, style.angleStep())).orElse(List.of());
                return Optional.of(LineCleaning.withoutStraightPoints(Polylines.joined(old.subList(0, Math.max(0, corner - 1)),
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
        List<Point> kept = Polylines.reversed(AngleSteps.stepKeepingRun(after, standing, at, angleStep));
        List<Point> plain = Polylines.joined(kept.subList(1, kept.size()), List.of(after));
        Optional<Point> before = coming.size() > 1 ? Optional.of(coming.get(coming.size() - 2)) : Optional.empty();
        Optional<Point> into = before.flatMap(point -> Ways.wayBetween(point, at));
        Optional<Point> outOf = Ways.wayBetween(at, plain.getFirst());
        Optional<Point> run = Ways.wayBetween(after, standing);
        if (before.isEmpty() || into.isEmpty() || outOf.isEmpty() || run.isEmpty()) {
            return plain;
        }
        double towards = into.get().cross(outOf.get()) > 0 ? 1 : -1;
        double stub = Math.min(engine.board().settings().carrying().endStub(), at.distanceTo(standing));
        List<List<Point>> shapes = new ArrayList<>();
        shapes.add(plain);
        shapes.addAll(shapesTo(towards, at, into.get(), after, run.get(), stub, angleStep));
        shapes.addAll(shapesTo(-towards, at, into.get(), after, run.get(), stub, angleStep));
        // The first in order that is not tangled and is no way round for nothing: back to the run, not past it and out again.
        double direct = at.distanceTo(after) * engine.board().settings().carrying().widestDetour() + stub;
        List<List<Point>> fit = shapes.stream()
                .filter(shape -> !LineCleaning.isTangled(LineCleaning.withoutStraightPoints(Polylines.joined(List.of(before.get(), at), shape)))).toList();
        return fit.stream().filter(shape -> Polylines.lengthOf(Polylines.joined(List.of(at), shape)) <= direct).findFirst()
                .orElse(fit.isEmpty() ? plain : fit.getLast());
    }

    /** To one side, the shapes a line may go on by from a carried corner to its run. */
    private static List<List<Point>> shapesTo(double side, Point at, Point into, Point after, Point run, double stub, double angleStep) {
        Point across = new Point(-into.y() * side, into.x() * side);
        Point shoulder = Precision.settled(at.moved(across, stub));
        List<List<Point>> shapes = new ArrayList<>();
        onRun(at, across, after, run).ifPresent(point -> shapes.add(List.of(point, after)));
        if (angleStep != 90) {
            double half = Math.sqrt(0.5);
            onRun(shoulder, new Point((across.x() - into.x()) * half, (across.y() - into.y()) * half), after, run)
                    .ifPresent(point -> shapes.add(List.of(shoulder, point, after)));
        }
        onRun(shoulder, new Point(-into.x(), -into.y()), after, run).ifPresent(point -> shapes.add(List.of(shoulder, point, after)));
        shapes.add(Polylines.joined(List.of(shoulder), AngleSteps.trackStep(shoulder, after, angleStep)));
        return shapes;
    }

    /** Where a way from a point meets the run going back from the corner that stayed, ahead of both. */
    private static Optional<Point> onRun(Point from, Point way, Point after, Point run) {
        return Ways.crossingOf(from, way, after, run)
                .filter(met -> Ways.reachAlong(from, way, met) > 0 && Ways.reachAlong(after, run, met) >= 0).map(Precision::settled);
    }
}
