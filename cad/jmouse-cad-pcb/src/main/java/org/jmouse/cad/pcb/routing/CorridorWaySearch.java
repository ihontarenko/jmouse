package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.geometry.taut.CorridorSearch;
import org.jmouse.geometry.taut.Obstacle;
import org.jmouse.geometry.taut.ObstacleField;
import org.jmouse.geometry.taut.UnitedObstacles;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

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
        return way(fence, from, to, angleStep, settings, OptionalDouble.empty());
    }

    /**
     * The way, no longer than {@code longest} where it is said: the windows it is searched in reach no further round than such a way
     * can go, every one past that left — a way longer than wanted is no answer, so searching for it was only time.
     */
    static Optional<List<Point>> way(TrackFence fence, Point from, Point to, double angleStep, RoutingSettings.Routing settings,
                                     OptionalDouble longest) {
        return CorridorSearch.firstAccepted(windowsOf(from, to, marginsWithin(settings.corridorMargins(), longest, from, to), settings),
                window -> CorridorSearch.alone(fieldOf(fence, window, from, to), window, Plane.coordinateOf(from), Plane.coordinateOf(to)),
                parts -> {
                    List<Point> path = Plane.pointsOf(((CorridorSearch.Run<Void>) parts.getFirst()).points());
                    return OctilinearLaying.laid(path, Optional.empty(), path, fence, angleStep, settings, OctilinearLaying.Clearance.KEPT);
                });
    }

    /**
     * The windows a way between two points is searched in, a margin round the box of its ends each, laid on the grid: the window the
     * next step near this one asks for is this very window, its copper united already ({@link StillCopper}).
     */
    static List<Envelope> windowsOf(Point from, Point to, List<Double> margins, RoutingSettings.Routing settings) {
        List<Envelope> windows = new ArrayList<>();
        for (double margin : margins) {
            Envelope asked = new Envelope(from.x(), to.x(), from.y(), to.y());
            asked.expandBy(margin);
            windows.add(StillCopper.onGrid(asked, settings.corridorGrid()));
        }
        return windows;
    }

    /**
     * The margins a way no longer than {@code longest} can need: every point of such a way, its two ends a and b apart, lies in the
     * ellipse round them whose axis is that length — within its half minor axis, √((longest/2)² − (ab/2)²), of the box
     * of its ends. The ladder's margins below that, then that one; the whole ladder where no length is said.
     */
    static List<Double> marginsWithin(List<Double> ladder, OptionalDouble longest, Point from, Point to) {
        if (longest.isEmpty()) {
            return ladder;
        }
        double apart = Math.hypot(to.x() - from.x(), to.y() - from.y());
        double half = Math.max(longest.getAsDouble(), apart) / 2;
        double reach = Math.sqrt(half * half - apart * apart / 4);
        List<Double> margins = new ArrayList<>();
        for (double margin : ladder) {
            if (margin < reach) {
                margins.add(margin);
            }
        }
        if (margins.size() < ladder.size()) {
            margins.add(reach);
        }
        return margins;
    }

    /**
     * What the fence keeps the track from within a window, as room its centre line keeps out of; its two ends' own ground — a pad
     * left on a fine pitch stands nearer its neighbour than the rule — cut out of it, as the fence lets them leave. The copper that
     * stands still is the one united for the window before ({@link StillCopper}), kept for every search of fences alike — a window on
     * the grid, so the windows of a hand's steps are one —; only what moves with the ask — what the fence's ends ease, what it keeps
     * clear of besides — is united each time.
     */
    static ObstacleField fieldOf(TrackFence fence, Envelope window, Point from, Point to) {
        BitSet eased = fence.easedFromBase();
        UnitedObstacles still = fence.stillCopper().unitedOver(eased, window, region -> keptOut(
                fence.stillNear(region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY(), eased), fence, region));
        List<Obstacle> moving = keptOut(fence.movingNear(window.getMinX(), window.getMinY(), window.getMaxX(), window.getMaxY(), eased),
                fence, window);
        Geometry ground = Areas.unionOf(List.of(endGround(from, fence), endGround(to, fence)));
        return ObstacleField.of(still, moving, ground, KeptOut.SLACK, Plane.FACTORY, Plane.GRID);
    }

    /** Copper a track keeps clear of, as the room its centre line keeps out of within a region: each grown by its gap and half the track. */
    private static List<Obstacle> keptOut(List<TrackFence.Obstacle> copper, TrackFence fence, Envelope region) {
        List<Obstacle> obstacles = new ArrayList<>();
        for (TrackFence.Obstacle obstacle : copper) {
            double grow = obstacle.shape().radius() + obstacle.gap() + fence.width() / 2 - KeptOut.EASED;
            KeptOut.of(obstacle.shape(), grow, region).ifPresent(obstacles::add);
        }
        return obstacles;
    }

    /** The ground round one end of the way: as far round as half the track is wide. */
    private static Geometry endGround(Point end, TrackFence fence) {
        return Plane.FACTORY.createPoint(Plane.coordinateOf(end)).buffer(fence.width() / 2);
    }
}
