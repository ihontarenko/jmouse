package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.geometry.taut.CorridorSearch;
import org.jmouse.geometry.taut.Obstacle;
import org.jmouse.geometry.taut.ObstacleField;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 🧭 A way searched for through the free room between what a track keeps clear of ({@link CorridorSearch}): the copper its fence
 * keeps it from, as the room its centre line keeps out of ({@link KeptOut}), cut into triangles; A* over them finds the corridor,
 * the way is pulled taut through it and laid at the reader's angles ({@link OctilinearLaying}). It sees as far round as its window,
 * so a way that goes far round a part — up past a connector and down its other side — is found where a grid searched near the
 * straight line, or a walk round what it meets, gives up. The window grows margin by margin ({@link RoutingSettings.Routing#corridorMargins})
 * until a way is found — never past a room found closed in, which no larger window opens.
 */
final class CorridorWaySearch {

    private CorridorWaySearch() {
    }

    /** The way from a point to another — its corners, the start among them — at an angle step, keeping the fence's gaps; empty where none is found. */
    static Optional<List<Point>> way(TrackFence fence, Point from, Point to, double angleStep, RoutingSettings.Routing settings) {
        for (double margin : settings.corridorMargins()) {
            Envelope window = new Envelope(from.x(), to.x(), from.y(), to.y());
            window.expandBy(margin);
            CorridorSearch.Outcome<Void> searched = CorridorSearch.searched(CorridorSearch.alone(fieldOf(fence, window, from, to), window,
                    Plane.coordinateOf(from), Plane.coordinateOf(to)));
            if (searched instanceof CorridorSearch.ClosedIn<Void>) {
                // The room the start reaches is closed in: no larger window goes round.
                return Optional.empty();
            }
            Optional<List<Coordinate>> taut = searched.parts().map(parts -> ((CorridorSearch.Run<Void>) parts.getFirst()).points());
            if (taut.isPresent()) {
                List<Point> path = Plane.pointsOf(taut.get());
                Optional<List<Point>> laid = OctilinearLaying.laid(path, Optional.empty(), path, fence, angleStep, settings,
                        OctilinearLaying.Clearance.KEPT);
                if (laid.isPresent()) {
                    return laid;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * What the fence keeps the track from within a window, as room its centre line keeps out of; its two ends' own ground — a pad
     * left on a fine pitch stands nearer its neighbour than the rule — cut out of it, as the fence lets them leave.
     */
    static ObstacleField fieldOf(TrackFence fence, Envelope window, Point from, Point to) {
        List<Obstacle> obstacles = new ArrayList<>();
        for (TrackFence.Obstacle obstacle : fence.obstaclesNear(window.getMinX(), window.getMinY(), window.getMaxX(), window.getMaxY())) {
            double grow = obstacle.shape().radius() + obstacle.gap() + fence.width() / 2 - KeptOut.EASED;
            KeptOut.of(obstacle.shape(), grow, window).ifPresent(obstacles::add);
        }
        Geometry ground = Areas.unionOf(List.of(endGround(from, fence), endGround(to, fence)));
        return ObstacleField.of(obstacles, ground, KeptOut.SLACK, Plane.FACTORY);
    }

    /** The ground round one end of the way: as far round as half the track is wide. */
    private static Geometry endGround(Point end, TrackFence fence) {
        return Plane.FACTORY.createPoint(Plane.coordinateOf(end)).buffer(fence.width() / 2);
    }
}
