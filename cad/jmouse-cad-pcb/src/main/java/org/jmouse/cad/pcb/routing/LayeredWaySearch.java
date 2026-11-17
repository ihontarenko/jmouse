package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.geometry.taut.CorridorSearch;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * 🧭 A connection's way over the board's layers, as the autorouter lays it: through the free room's corridor on every layer at once
 * ({@link CorridorSearch}), the same search a track laid by hand takes on its one layer ({@link CorridorWaySearch}) — from layer to
 * layer through a via where one stands clear on every layer and off the connection's ends, over what is in the way by a 0 Ω link
 * where the request allows one; a stretch against a layer's way costs more. Each run along a layer is laid at the router's angles.
 * The room grows margin by margin ({@link RoutingSettings.Routing#corridorMargins}) until a way is found — never past a room
 * found closed in, which no larger window opens.
 */
final class LayeredWaySearch {

    /** The four straight ways a link may span. */
    private static final List<Point> STRAIGHT = List.of(new Point(1, 0), new Point(0, 1), new Point(-1, 0), new Point(0, -1));

    private LayeredWaySearch() {
    }

    /** A layer the way may run on: the fence it keeps, and the way the layer's tracks run, with what a stretch against it costs. */
    record Lane(TrackFence fence, Optional<Preferred> preferred) {
    }

    /** The way a layer's tracks run, and what a stretch across it costs as a share of one along it. */
    record Preferred(RoutingSettings.Direction direction, double against) {
    }

    /** Changing layer: every lane's fence a via must keep, what it costs as a length of track, and how far it stands off the two ends. */
    record ViaMove(List<TrackFence> fences, double cost, double offEnds) {
    }

    /** A link over what is in the way: how far its two pads stand apart, what each must keep on a lane, if it may stand there, and its cost. */
    record LinkMove(double reach, IntFunction<Optional<TrackFence>> fence, double cost) {
    }

    /**
     * A way asked for.
     *
     * @param startLanes the lanes the start reaches
     * @param goalLanes  the lanes the end reaches
     * @param angleStep  degrees between the directions a track may take: 90, or 45 for anything else, the router's own angles
     */
    record Request(List<Lane> lanes, Point from, Point to, List<Integer> startLanes, List<Integer> goalLanes, double angleStep,
                   Optional<ViaMove> via, Optional<LinkMove> link) {
    }

    /** What a way is made of, from its start to its end. */
    sealed interface WayPart permits Stretch, ViaHop, LinkHop {
    }

    /** A run along one lane, by its corners. */
    record Stretch(int lane, List<Point> points) implements WayPart {
    }

    /** A change of layer through a via. */
    record ViaHop(Point at) implements WayPart {
    }

    /** A 0 Ω link over what is in the way, on a lane. */
    record LinkHop(int lane, Point from, Point to) implements WayPart {
    }

    /** What a hop of the search is. */
    private enum HopKind {
        VIA, LINK
    }

    /** A way over the layers from its start to its end that keeps its gaps; empty where none is found. */
    static Optional<List<WayPart>> way(Request request, RoutingSettings.Routing settings) {
        Map<HopAt, List<CorridorSearch.Hop<HopKind>>> known = new HashMap<>();
        CorridorSearch.Hops<HopKind> hops = (layer, at) -> known.computeIfAbsent(new HopAt(layer, at.x, at.y), ignored -> hopsFrom(request, layer, at));
        for (double margin : settings.corridorMargins()) {
            Envelope asked = new Envelope(request.from().x(), request.to().x(), request.from().y(), request.to().y());
            asked.expandBy(margin);
            // Laid on the grid, as a leg's window is: its copper united once for the windows near it.
            Envelope window = StillCopper.onGrid(asked, settings.corridorGrid());
            List<CorridorSearch.Layer> layers = request.lanes().stream()
                    .map(lane -> new CorridorSearch.Layer(CorridorWaySearch.fieldOf(lane.fence(), window, request.from(), request.to()), costOf(lane)))
                    .toList();
            CorridorSearch.Outcome<HopKind> searched = CorridorSearch.searched(new CorridorSearch.Request<>(layers, window,
                    Plane.coordinateOf(request.from()), Plane.coordinateOf(request.to()), new HashSet<>(request.startLanes()),
                    new HashSet<>(request.goalLanes()), hops));
            if (searched instanceof CorridorSearch.ClosedIn<HopKind>) {
                // The room the start reaches is closed in on every layer: no larger window goes round.
                return Optional.empty();
            }
            Optional<List<WayPart>> laid = searched.parts().flatMap(found -> laidAtAngles(request, found, settings));
            if (laid.isPresent()) {
                return laid;
            }
        }
        return Optional.empty();
    }

    /** Where hops were asked from: a layer and a point. */
    private record HopAt(int layer, double x, double y) {
    }

    /** The hops from a point of a lane: a via to each other lane where one stands clear, a link each straight way where its pads do. */
    private static List<CorridorSearch.Hop<HopKind>> hopsFrom(Request request, int lane, Coordinate at) {
        Point point = Lines.settled(new Point(at.x, at.y));
        List<CorridorSearch.Hop<HopKind>> hops = new ArrayList<>();
        request.via().filter(via -> viaStands(request, via, point)).ifPresent(via -> {
            for (int other = 0; other < request.lanes().size(); other++) {
                if (other != lane) {
                    hops.add(new CorridorSearch.Hop<>(Plane.coordinateOf(point), other, Plane.coordinateOf(point), via.cost(), HopKind.VIA));
                }
            }
        });
        request.link().ifPresent(link -> link.fence().apply(lane).filter(pads -> pads.pointClear(point)).ifPresent(pads -> {
            for (Point way : STRAIGHT) {
                Point far = Lines.settled(Lines.offset(point, way, link.reach()));
                if (pads.pointClear(far)) {
                    hops.add(new CorridorSearch.Hop<>(Plane.coordinateOf(point), lane, Plane.coordinateOf(far), link.cost(), HopKind.LINK));
                }
            }
        }));
        return hops;
    }

    private static boolean viaStands(Request request, ViaMove via, Point point) {
        double offEnds = Math.min(Lines.lengthBetween(point, request.from()), Lines.lengthBetween(point, request.to()));
        return offEnds >= via.offEnds() && via.fences().stream().allMatch(fence -> fence.pointClear(point));
    }

    /** What a stretch along a lane costs: its length along the lane's way, {@code against} times its length across it. */
    private static CorridorSearch.StepCost costOf(Lane lane) {
        if (lane.preferred().isEmpty()) {
            return CorridorSearch.StepCost.LENGTH;
        }
        Preferred preferred = lane.preferred().get();
        boolean horizontal = preferred.direction() == RoutingSettings.Direction.HORIZONTAL;
        return (from, to) -> {
            double along = Math.abs(horizontal ? to.x - from.x : to.y - from.y);
            double across = Math.abs(horizontal ? to.y - from.y : to.x - from.x);
            return along + preferred.against() * across;
        };
    }

    /** The parts found, each run laid at the router's angles on its lane; empty where one cannot be laid clear. */
    private static Optional<List<WayPart>> laidAtAngles(Request request, List<CorridorSearch.Part<HopKind>> found, RoutingSettings.Routing settings) {
        List<WayPart> parts = new ArrayList<>();
        for (CorridorSearch.Part<HopKind> part : found) {
            switch (part) {
                case CorridorSearch.Run<HopKind> run -> {
                    List<Point> path = Plane.pointsOf(run.points());
                    if (path.size() < 2) {
                        continue;
                    }
                    Optional<List<Point>> laid = OctilinearLaying.laid(path, Optional.empty(), path, request.lanes().get(run.layer()).fence(),
                            request.angleStep(), settings, OctilinearLaying.Clearance.KEPT);
                    if (laid.isEmpty()) {
                        return Optional.empty();
                    }
                    parts.add(new Stretch(run.layer(), laid.get()));
                }
                case CorridorSearch.Hopped<HopKind> hopped -> {
                    Point from = Plane.pointsOf(List.of(hopped.hop().from())).getFirst();
                    parts.add(switch (hopped.hop().kind()) {
                        case VIA -> new ViaHop(from);
                        case LINK -> new LinkHop(hopped.fromLayer(), from, Plane.pointsOf(List.of(hopped.hop().to())).getFirst());
                    });
                }
            }
        }
        return Optional.of(parts);
    }
}
