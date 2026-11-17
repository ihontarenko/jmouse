package org.jmouse.cad.pcb.routing;

import org.jmouse.geometry.Areas;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.locationtech.jts.algorithm.MinimumAreaRectangle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 🧲 How a track leaves a pad, as KiCad's smart pads lay it: out through one of the pad's breakouts — straight out of each side of a
 * pad along its own axes, whatever it is turned to; any of eight ways out of a round one —, then on to the line at the reader's
 * angles. A track leaving a thin pad from its middle on a slant runs out over the pad's corner, nearer its neighbour than the pitch
 * meant, and leaves a sharp wedge of copper where it meets the pad's side; one leaving through a breakout does neither.
 *
 * <p>A pad near enough square is left at a slant too, through its corners. Each breakout is tried joining the line at each of its
 * next few corners, in each posture the angle step allows; the shortest that keeps every gap and turns back nowhere is laid. A line
 * that already leaves along a breakout, or that none can carry on clear, is left as it is — and so is every line laid at any angle,
 * whose straight way out of a pad's middle is already the one it is meant to take.</p>
 */
final class PadBreakouts {

    /** Twice this is how far past a pad a breakout's ray is cast to find the pad's edge, in its own size: always out of it. */
    private static final double RAY_REACH = 4;

    /** How far off a breakout's way a line's first stretch may run and still be taken as leaving along it: a point's precision. */
    private static final double ALONG = Lines.SAME_POINT;

    private PadBreakouts() {
    }

    /** Whether a line turns back on itself, sharper than a right angle, at any corner up to the one given: a hook no track is laid with. */
    private static boolean turnsBackAnywhere(List<Point> line, int upTo) {
        for (int corner = 1; corner < Math.min(upTo + 1, line.size() - 1); corner++) {
            Point before = line.get(corner - 1);
            Point at = line.get(corner);
            Point after = line.get(corner + 1);
            if ((at.x() - before.x()) * (after.x() - at.x()) + (at.y() - before.y()) * (after.y() - at.y()) < 0) {
                return true;
            }
        }
        return false;
    }

    /** A way out of a pad: from where the line starts on it, to the edge of its copper. */
    record Breakout(Point from, Point exit) {

        /** Whether a stretch from its start runs out along it. */
        boolean carries(Point next) {
            double wayX = exit.x() - from.x();
            double wayY = exit.y() - from.y();
            double nextX = next.x() - from.x();
            double nextY = next.y() - from.y();
            double length = Math.hypot(wayX, wayY);
            return length > 0 && Math.abs(wayX * nextY - wayY * nextX) / length <= ALONG && wayX * nextX + wayY * nextY > 0;
        }

    }

    /**
     * The breakouts out of a pad, from a point on it: along its axes — through its corners too where it is near enough square, eight
     * ways for a round one —, each to its copper's edge.
     *
     * @param slantedUpTo how many times longer than wide a pad may be and still be left through its corners
     */
    static List<Breakout> of(CopperNets.PlacedPad pad, Point from, double slantedUpTo) {
        Geometry shape = pad.shape();
        List<double[]> ways = new ArrayList<>();
        if ("circle".equals(pad.pad().shape())) {
            for (int way = 0; way < 8; way++) {
                double angle = way * Math.PI / 4;
                ways.add(new double[] {Math.cos(angle), Math.sin(angle)});
            }
        } else {
            Coordinate[] corners = MinimumAreaRectangle.getMinimumRectangle(shape).getCoordinates();
            if (corners.length < 4) {
                return List.of();
            }
            double[] across = {corners[1].x - corners[0].x, corners[1].y - corners[0].y};
            double[] down = {corners[3].x - corners[0].x, corners[3].y - corners[0].y};
            double acrossLength = Math.hypot(across[0], across[1]);
            double downLength = Math.hypot(down[0], down[1]);
            if (acrossLength == 0 || downLength == 0) {
                return List.of();
            }
            double[] acrossWay = {across[0] / acrossLength, across[1] / acrossLength};
            double[] downWay = {down[0] / downLength, down[1] / downLength};
            List<double[]> axes = new ArrayList<>(List.of(acrossWay, downWay));
            if (Math.max(acrossLength, downLength) <= slantedUpTo * Math.min(acrossLength, downLength)) {
                // At a slant: halfway between its axes — 45° to its sides, whatever its corners' own slant.
                axes.add(new double[] {acrossWay[0] + downWay[0], acrossWay[1] + downWay[1]});
                axes.add(new double[] {acrossWay[0] - downWay[0], acrossWay[1] - downWay[1]});
            }
            for (double[] axis : axes) {
                double length = Math.hypot(axis[0], axis[1]);
                ways.add(new double[] {axis[0] / length, axis[1] / length});
                ways.add(new double[] {-axis[0] / length, -axis[1] / length});
            }
        }
        double reach = RAY_REACH * Math.max(shape.getEnvelopeInternal().getWidth(), shape.getEnvelopeInternal().getHeight());
        List<Breakout> breakouts = new ArrayList<>();
        for (double[] way : ways) {
            edgeAlong(shape, from, way, reach).map(exit -> new Breakout(from, Lines.settled(exit))).ifPresent(breakouts::add);
        }
        return breakouts;
    }

    /** Where a ray from a point on a pad leaves its copper: the nearest crossing of the pad's edge. */
    private static Optional<Point> edgeAlong(Geometry shape, Point from, double[] way, double reach) {
        LineString ray = Plane.FACTORY.createLineString(new Coordinate[] {Plane.coordinateOf(from),
                new Coordinate(from.x() + way[0] * reach, from.y() + way[1] * reach)});
        Point nearest = null;
        for (Coordinate crossing : Areas.crossing(shape.getBoundary(), ray).getCoordinates()) {
            Point point = new Point(crossing.x, crossing.y);
            if (nearest == null || Lines.lengthBetween(from, point) < Lines.lengthBetween(from, nearest)) {
                nearest = point;
            }
        }
        return Optional.ofNullable(nearest).filter(point -> Lines.lengthBetween(from, point) > Lines.SAME_POINT);
    }

    /**
     * A line with each of its ends that stands on a pad of its layer leaving it through the pad's best breakout.
     *
     * @param fence what the line keeps clear of
     * @param pads  how a track leaves a pad: how far on a breakout may join the line, which pads are left at a slant
     */
    static List<Point> atPads(RoutingWorld world, String layer, List<Point> line, TrackFence fence, double angleStep, RoutingSettings.Pads pads) {
        if (angleStep != 45 && angleStep != 90) {
            return line;
        }
        List<Point> started = padAt(world, layer, line.getFirst())
                .map(pad -> fromPad(line, of(pad, line.getFirst(), pads.slantedUpTo()), fence, angleStep, pads.lookAhead())).orElse(line);
        return padAt(world, layer, started.getLast())
                .map(pad -> Lines.reversed(fromPad(Lines.reversed(started), of(pad, started.getLast(), pads.slantedUpTo()), fence, angleStep,
                        pads.lookAhead())))
                .orElse(started);
    }

    private static Optional<CopperNets.PlacedPad> padAt(RoutingWorld world, String layer, Point point) {
        return world.nodes().at(layer, point).filter(node -> node.kind() == CopperNodeKind.PAD)
                .map(node -> world.nets().pads().get(node.item().getAsInt()));
    }

    /** A line starting on a pad, its start through the best breakout that carries it on clear; as it is where none does or it already does. */
    static List<Point> fromPad(List<Point> line, List<Breakout> breakouts, TrackFence fence, double angleStep, int lookAhead) {
        if (line.size() < 2 || breakouts.isEmpty() || breakouts.stream().anyMatch(breakout -> breakout.carries(line.get(1)))) {
            return line;
        }
        List<Point> best = null;
        double shortest = Double.POSITIVE_INFINITY;
        for (Breakout breakout : breakouts) {
            for (int corner = 1; corner <= Math.min(lookAhead, line.size() - 1); corner++) {
                for (List<Point> posture : Lines.posturesOf(breakout.exit(), line.get(corner), angleStep)) {
                    List<Point> out = Lines.joined(List.of(breakout.exit()), posture);
                    List<Point> candidate = Lines.joined(List.of(breakout.from()), out, line.subList(corner + 1, line.size()));
                    double length = Lines.lengthBetween(breakout.from(), breakout.exit()) + Lines.lengthOf(out);
                    if (length < shortest && !turnsBackAnywhere(candidate, corner + 2) && fence.lineClear(out)) {
                        shortest = length;
                        best = candidate;
                    }
                }
            }
        }
        return best == null ? line : Lines.withoutStraightPoints(best);
    }
}
