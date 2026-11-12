package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A line of copper as a person sees it: tracks joined end to end where nothing else meets them — no pad, no via, no arc, no
 * third track — are one line. Joined is what the copper says, not the numbers: a board read from KiCad has a track end a few
 * hundredths of a millimetre off its neighbour's, inside its copper, and that is a joint. The line's corner there is where the
 * two stretches' own directions meet. A drag of one stretch moves its neighbours as it would on a track drawn in one go, and
 * each track gets its own part of the line back, its width and layer its own.
 */
public final class JoinedTracks {

    /** How far apart, as a share of the two ends' distance, the stretches' own meeting may stand before halfway is taken instead. */
    private static final double FARTHEST_MEETING = 4;

    /** The least distance a meeting is measured against, in millimetres. */
    private static final double NEAREST_MEETING = 1e-3;

    private JoinedTracks() {
    }

    /** One track of a joined line, in the line's order: reversed when its points run against the line. */
    public record Member(int index, boolean reversed) {
    }

    /**
     * @param points every point of the line, every joint once
     * @param joints where in the line each joint stands: the point at which the next member begins
     * @param starts the place of a member's own first point in the line, by its position among the members
     */
    public record JoinedLine(List<Member> members, List<Point> points, List<Integer> joints, List<Integer> starts) {
    }

    private record Partner(int index, boolean startsThere) {
    }

    private static List<Point> pointsOf(PcbDrawing pcb, Member member) {
        List<Point> points = pcb.tracks().get(member.index()).points();
        return member.reversed() ? Lines.reversed(points) : points;
    }

    /**
     * The one other track a point where a track ends joins at a plain joint — an end of it within both tracks' copper —, and
     * whether that track starts there; empty where anything else meets it: a third track, a branch off the middle of one, an
     * arc, a via, a pad.
     */
    private static Optional<Partner> partnerAt(RoutingWorld world, int index, Point point, Set<Integer> taken) {
        PcbDrawing pcb = world.pcb();
        PcbDrawing.Track own = pcb.tracks().get(index);
        int curveSteps = world.board().checks().curveSteps();
        Partner partner = null;
        for (int other = 0; other < pcb.tracks().size(); other++) {
            PcbDrawing.Track track = pcb.tracks().get(other);
            if (other == index || !track.layer().equals(own.layer())) {
                continue;
            }
            List<Point> points = track.points();
            double reach = Math.min(own.width(), track.width()) / 2;
            boolean nearerFirst = Lines.lengthBetween(points.getFirst(), point) <= Lines.lengthBetween(points.getLast(), point);
            Point nearer = nearerFirst ? points.getFirst() : points.getLast();
            if (Lines.lengthBetween(nearer, point) <= reach) {
                if (partner != null || taken.contains(other)) {
                    return Optional.empty();
                }
                partner = new Partner(other, nearerFirst);
            } else if (CopperShapes.track(track, curveSteps).covers(point.x(), point.y())) {
                // The point stands on another track's copper away from its ends: a branch.
                return Optional.empty();
            }
        }
        if (partner == null) {
            return Optional.empty();
        }
        double reach = own.width() / 2;
        for (PcbDrawing.Arc arc : pcb.arcs()) {
            if (arc.layer().equals(own.layer()) && (Lines.lengthBetween(arc.from(), point) <= reach + arc.width() / 2
                                                    || Lines.lengthBetween(arc.to(), point) <= reach + arc.width() / 2)) {
                return Optional.empty();
            }
        }
        for (PcbDrawing.Via via : pcb.vias()) {
            if (Math.hypot(via.x() - point.x(), via.y() - point.y()) <= via.diameter() / 2) {
                return Optional.empty();
            }
        }
        for (CopperNets.PlacedPad pad : world.nets().pads()) {
            CopperShape copper = pad.copper();
            if (pad.on(own.layer()) && copper.covers(point.x(), point.y())) {
                return Optional.empty();
            }
        }
        return Optional.of(partner);
    }

    /**
     * Where two joined stretches turn: where the line of the one coming in meets the line of the one going on, so each keeps its
     * own direction; halfway between their ends where they run on in one direction, or would meet far from the joint.
     */
    private static Point cornerBetween(Point before, Point end, Point start, Point after) {
        Point halfway = new Point((end.x() + start.x()) / 2, (end.y() + start.y()) / 2);
        Optional<Point> coming = Lines.wayBetween(before, end);
        Optional<Point> going = Lines.wayBetween(start, after);
        Optional<Point> met = coming.isPresent() && going.isPresent() ? Lines.crossingOf(end, coming.get(), start, going.get()) : Optional.empty();
        double farthest = Math.max(Lines.lengthBetween(end, start) * FARTHEST_MEETING, NEAREST_MEETING);
        return met.filter(point -> Lines.lengthBetween(point, halfway) <= farthest).orElse(halfway);
    }

    /** The line a track is part of: the track alone where it joins nothing at a plain joint. */
    public static JoinedLine joinedLine(RoutingWorld world, int index) {
        PcbDrawing pcb = world.pcb();
        List<Member> members = new ArrayList<>(List.of(new Member(index, false)));
        Set<Integer> taken = new HashSet<>(Set.of(index));
        // Back from its first point, then on from its last.
        while (true) {
            Member first = members.getFirst();
            Optional<Partner> partner = partnerAt(world, first.index(), pointsOf(pcb, first).getFirst(), taken);
            if (partner.isEmpty()) {
                break;
            }
            // Before the line, a partner that ends at the joint runs with it; one that starts there runs against it.
            members.addFirst(new Member(partner.get().index(), partner.get().startsThere()));
            taken.add(partner.get().index());
        }
        while (true) {
            Member last = members.getLast();
            Optional<Partner> partner = partnerAt(world, last.index(), pointsOf(pcb, last).getLast(), taken);
            if (partner.isEmpty()) {
                break;
            }
            members.addLast(new Member(partner.get().index(), !partner.get().startsThere()));
            taken.add(partner.get().index());
        }
        List<Point> points = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> joints = new ArrayList<>();
        for (int position = 0; position < members.size(); position++) {
            List<Point> own = pointsOf(pcb, members.get(position));
            if (position > 0) {
                // One corner for both: the line's end and the member's start become where their stretches meet.
                Point corner = cornerBetween(points.get(points.size() - 2), points.getLast(), own.get(0), own.get(1));
                points.set(points.size() - 1, corner);
                joints.add(points.size() - 1);
            }
            starts.add(Math.max(0, points.size() - 1));
            points.addAll(position == 0 ? own : own.subList(1, own.size()));
        }
        return new JoinedLine(List.copyOf(members), List.copyOf(points), List.copyOf(joints), List.copyOf(starts));
    }

    /** Where on a new line each old joint goes: the nearest corner, in order, none at an end; a point put in where no corner is left. */
    private record Placed(List<Point> line, List<Integer> at) {
    }

    private static Placed jointsOn(List<Point> line, List<Point> joints) {
        List<Point> result = new ArrayList<>(line);
        List<Integer> at = new ArrayList<>();
        int from = 1;
        for (int position = 0; position < joints.size(); position++) {
            Point joint = joints.get(position);
            int left = joints.size() - position - 1;
            int last = result.size() - 2 - left;
            int best = -1;
            for (int corner = from; corner <= last; corner++) {
                if (best < 0 || Lines.lengthBetween(result.get(corner), joint) < Lines.lengthBetween(result.get(best), joint)) {
                    best = corner;
                }
            }
            if (best < 0) {
                // No corner left: the joint goes in halfway along the stretch it would have stood on.
                int after = Math.min(Math.max(from, 1), result.size() - 1);
                Point a = result.get(after - 1);
                Point b = result.get(after);
                result.add(after, new Point((a.x() + b.x()) / 2, (a.y() + b.y()) / 2));
                best = after;
            }
            at.add(best);
            from = best + 1;
        }
        return new Placed(result, at);
    }

    /** The pcb with a joined line's new points given back to its tracks, each its own stretch of it, as it ran before. */
    public static PcbDrawing withJoinedLine(PcbDrawing pcb, JoinedLine joined, List<Point> line) {
        if (joined.members().size() == 1) {
            return pcb.withTrackPoints(joined.members().getFirst().index(), line);
        }
        Placed placed = jointsOn(line, joined.joints().stream().map(joined.points()::get).toList());
        List<Integer> bounds = new ArrayList<>();
        bounds.add(0);
        bounds.addAll(placed.at());
        bounds.add(placed.line().size() - 1);
        Map<Integer, List<Point>> pieces = new HashMap<>();
        for (int position = 0; position < joined.members().size(); position++) {
            Member member = joined.members().get(position);
            List<Point> piece = placed.line().subList(bounds.get(position), bounds.get(position + 1) + 1);
            pieces.put(member.index(), member.reversed() ? Lines.reversed(piece) : List.copyOf(piece));
        }
        List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
        pieces.forEach((index, points) -> tracks.set(index, tracks.get(index).withPoints(points)));
        return pcb.withTracks(tracks);
    }
}
