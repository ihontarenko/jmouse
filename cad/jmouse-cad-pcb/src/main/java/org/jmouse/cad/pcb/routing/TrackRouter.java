package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * The router: how a leg of track is laid. Laying a track, carrying a segment or a corner ({@link TrackReshaping}), a part's
 * tracks following it ({@link PartDragging}), routing a whole pcb ({@link Autorouter}) each ask it for legs and say which
 * corners are held; how a leg goes round what it meets and how it comes out shaped is said once, here.
 *
 * <pre>world (what is there) → leg (the way) → tightened (the shape)</pre>
 */
public final class TrackRouter {

    private TrackRouter() {
    }

    /** How the reader lays and carries copper now: the directions a track may take, what becomes of a jog. */
    public record LayingStyle(double angleStep, Tightening.JogHandling jogs) {
    }

    /**
     * A stretch of track to be laid on a board.
     *
     * @param own         the track of the pcb this leg is part of: it is not in its own way
     * @param held        corners it must keep; its ends always stay
     * @param joins       where the track joins its own net's copper: only there may it stand nearer other copper than its gap, as
     *                    a track leaving a pad on a fine pitch does; empty for its start, and its end where the net's copper is
     * @param also        what it keeps clear of besides the board's copper: the way a moved part swept, which a track laid round it
     *                    does not cross back over
     */
    public record LegRequest(RoutingWorld world, String layer, Optional<String> net, double width, Point from, Point to, double angleStep,
                             OptionalInt own, List<Point> held, Optional<List<Point>> joins, List<TrackFence.Obstacle> also,
                             OptionalDouble longest) {

        public LegRequest {
            also = List.copyOf(also);
        }

        /** The same leg, of a net. */
        public LegRequest onNet(Optional<String> other) {
            return new LegRequest(world, layer, other, width, from, to, angleStep, own, held, joins, also, longest);
        }

        /**
         * The same leg, a way of it no longer than this: one longer is not wanted, so its search looks no further round than such a
         * way can go — never a window a way it could take does not need.
         */
        public LegRequest noLongerThan(double length) {
            return new LegRequest(world, layer, net, width, from, to, angleStep, own, held, joins, also, OptionalDouble.of(length));
        }

        /** The same leg keeping clear of more besides the board's copper. */
        public LegRequest keepingClearOf(List<TrackFence.Obstacle> more) {
            return new LegRequest(world, layer, net, width, from, to, angleStep, own, held, joins, Stream.concat(also.stream(), more.stream()).toList(),
                    longest);
        }

        /**
         * A leg laid by hand: of the track it carries on, if any — that track's copper is its own —, nothing held, its joins where it
         * starts and ends.
         */
        public static LegRequest of(RoutingWorld world, OptionalInt own, String layer, Optional<String> net, double width, Point from, Point to,
                                    double angleStep) {
            return new LegRequest(world, layer, net, width, from, to, angleStep, own, List.of(), Optional.empty(), List.of(), OptionalDouble.empty());
        }

        /** A leg of a track already on the pcb, walked round what it meets, joined at the given points. */
        public static LegRequest walked(RoutingWorld world, PcbDrawing.Track track, Optional<String> net, Point from, Point to, double angleStep,
                                        int own, List<Point> held, List<Point> joins) {
            return new LegRequest(world, track.layer(), net, track.width(), from, to, angleStep, OptionalInt.of(own), held, Optional.of(joins),
                    List.of(), OptionalDouble.empty());
        }
    }

    /**
     * The way a leg goes.
     *
     * @param points  the points after its start
     * @param reached whether it reaches its end; else it stands as far as it can go
     */
    public record RoutedLeg(List<Point> points, boolean reached) {
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

    /** The leg: straight where it is free, else round what it meets through the free room's corridor, else as far as it can go. */
    public static RoutedLeg routedLeg(LegRequest asked) {
        RoutingWorld world = asked.world();
        // A leg asked of no net is of the copper it starts on, as one laid from a pad is: that copper is its own, never in its way.
        LegRequest request = asked.net().isPresent() ? asked : asked.onNet(world.netAt(asked.layer(), asked.from()));
        RoutingSettings.Routing settings = world.settings().routing();
        List<Point> joins = request.joins().orElseGet(() -> {
            boolean endOnNet = request.net().isPresent() && world.netAt(request.layer(), request.to()).equals(request.net());
            return endOnNet ? List.of(request.from(), request.to()) : List.of(request.from());
        });
        TrackFence fence = world.fence(request.layer(), request.net(), request.width(), request.own()).leaving(joins)
                .alsoKeepingClearOf(request.also(), settings.bucket());
        List<Point> straight = Lines.trackStep(request.from(), request.to(), request.angleStep());
        List<Point> straightLine = Lines.joined(List.of(request.from()), straight);
        if (fence.lineClear(straightLine)) {
            return new RoutedLeg(straight, true);
        }
        // Searched through the free room's corridor: as far round as it has to go, tight on what it goes round.
        Optional<List<Point>> way = CorridorWaySearch.way(fence, request.from(), request.to(), request.angleStep(), settings, request.longest());
        if (way.isPresent()) {
            List<Point> tight = new Tightening(fence, request.angleStep(), request.held(), settings).tightened(way.get());
            return new RoutedLeg(List.copyOf(tight.subList(1, tight.size())), true);
        }
        return new RoutedLeg(stoppedShort(fence, request.from(), straight, settings.stopHalvings()), false);
    }
}
