package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 🧭 A way through the free room of one field or of several layered over each other, from a point to another, found over the room's
 * triangles rather than a grid: A* from the triangle holding the start to the one holding the end, across the sides two triangles
 * share — each crossed where its middle is, the rest of the way measured straight to the end —, and from one layer to another, or
 * over what is in the way, by the hops the asker allows at a triangle's middle; then each run along a layer pulled taut through the
 * corridor of triangles it found ({@link Funnel}): the shortest path that goes round the obstacles that way. Its corners are the
 * obstacles' corners and the hops' points and nothing else, however far round them the way goes — a grid searched near the straight
 * line finds no such way, and a walk round what it meets turns back before it.
 *
 * <p>A* over the triangles finds a way, not always the shortest of every way round: it measures the way between the sides' middles.
 * Within the corridor it finds, the path is the shortest there is.</p>
 */
public final class CorridorSearch {

    private CorridorSearch() {
    }

    /** What a stretch along a layer costs, from a point to another: its length, or more where it runs against the layer's way. */
    @FunctionalInterface
    public interface StepCost {

        /** A stretch costing its length. */
        StepCost LENGTH = Coordinate::distance;

        double of(Coordinate from, Coordinate to);
    }

    /** A layer a way may run along: its free room, and what a stretch along it costs. */
    public record Layer(ObstacleField field, StepCost cost) {
    }

    /**
     * A hop from a point of a layer — the run before it ends there —: onto a layer — another at the same point, through the board,
     * or the same one at a point further on, over what is in the way —, at a cost; what it is, the asker's own word for it.
     */
    public record Hop<K>(Coordinate from, int layer, Coordinate to, double cost, K kind) {
    }

    /**
     * The hops the asker allows from near a point of a layer, the middle of a triangle there — each from a point the asker may settle
     * near it, within the triangle —; none where it allows none.
     */
    @FunctionalInterface
    public interface Hops<K> {
        List<Hop<K>> from(int layer, Coordinate at);
    }

    /**
     * A way asked for over layers.
     *
     * @param startLayers the layers the start stands on
     * @param goalLayers  the layers the end stands on
     */
    public record Request<K>(List<Layer> layers, Envelope window, Coordinate from, Coordinate to, Set<Integer> startLayers,
                             Set<Integer> goalLayers, Hops<K> hops) {

        public Request {
            layers = List.copyOf(layers);
            startLayers = Set.copyOf(startLayers);
            goalLayers = Set.copyOf(goalLayers);
        }
    }

    /** What a way over layers is made of, from its start to its end. */
    public sealed interface Part<K> permits Run, Hopped {
    }

    /** A run along one layer, pulled taut: its corners, its two ends among them. */
    public record Run<K>(int layer, List<Coordinate> points) implements Part<K> {
    }

    /** A hop from the end of one run to the start of the next. */
    public record Hopped<K>(int fromLayer, Hop<K> hop) implements Part<K> {
    }

    /** What a search answered: the way, or why there is none — and with it whether a larger window may find one. */
    public sealed interface Outcome<K> permits Found, ClosedIn, CutOffByTheWindow, Unknown {

        /** The way, where one was found. */
        default Optional<List<Part<K>>> parts() {
            return Optional.empty();
        }
    }

    /** The way, run by run with the hops between them. */
    public record Found<K>(List<Part<K>> found) implements Outcome<K> {

        @Override
        public Optional<List<Part<K>>> parts() {
            return Optional.of(found);
        }
    }

    /**
     * No way: the room the start reaches is closed in, or an end stands in no free room — no larger window finds one either.
     */
    public record ClosedIn<K>() implements Outcome<K> {
    }

    /** No way within the window: the room the start reaches runs to its edge, past which a way may go round. */
    public record CutOffByTheWindow<K>() implements Outcome<K> {
    }

    /** The room could not be cut into triangles — rings JTS finds invalid —: another window may be. */
    public record Unknown<K>() implements Outcome<K> {
    }

    /**
     * The shortest way round the obstacles the corridor found goes, from a point to another within a window — the start first, the
     * end last —; empty where the room is not known, a point is in no free triangle, or no corridor joins them.
     */
    public static Optional<List<Coordinate>> way(ObstacleField field, Envelope window, Coordinate from, Coordinate to) {
        return searched(alone(field, window, from, to)).parts().map(parts -> ((Run<Void>) parts.getFirst()).points());
    }

    /** A way asked of one field alone: one layer, nothing to hop by. */
    public static Request<Void> alone(ObstacleField field, Envelope window, Coordinate from, Coordinate to) {
        return new Request<>(List.of(new Layer(field, StepCost.LENGTH)), window, from, to, Set.of(0), Set.of(0), (layer, at) -> List.of());
    }

    /** A way over the layers, run by run with the hops between them; or why there is none. */
    public static <K> Outcome<K> searched(Request<K> request) {
        List<FreeSpace> rooms = new ArrayList<>();
        for (Layer layer : request.layers()) {
            Optional<FreeSpace> room = FreeSpace.of(layer.field(), request.window());
            if (room.isEmpty()) {
                return new Unknown<>();
            }
            rooms.add(room.get());
        }
        return new Search<>(request, rooms).found();
    }

    /** A triangle of a layer, as one key. */
    private static long keyOf(int layer, int triangle) {
        return (long) layer << 32 | triangle & 0xffffffffL;
    }

    private static int layerOf(long key) {
        return (int) (key >>> 32);
    }

    private static int triangleOf(long key) {
        return (int) key;
    }

    /** The middle of a triangle: where a hop from it stands. */
    private static Coordinate middleOf(FreeSpace.Triangle triangle) {
        Coordinate[] corners = triangle.corners();
        return new Coordinate((corners[0].x + corners[1].x + corners[2].x) / 3, (corners[0].y + corners[1].y + corners[2].y) / 3);
    }

    /** One search over the layers' triangles. */
    private static final class Search<K> {

        /** A triangle of a layer reached: from which, where it was entered, the way so far, and the hop it was reached by, if one. */
        private record Reached<K>(long from, Coordinate at, double way, Hop<K> hop) {
        }

        /** A triangle waiting to be gone on from: the way so far and what is left of it measured straight. */
        private record Open(long key, double way, double estimate) {
        }

        private final Request<K>             request;
        private final List<FreeSpace>        rooms;
        private final Map<Long, Reached<K>>  reached = new HashMap<>();
        private final PriorityQueue<Open>    open = new PriorityQueue<>((one, other) -> Double.compare(one.estimate(), other.estimate()));

        Search(Request<K> request, List<FreeSpace> rooms) {
            this.request = request;
            this.rooms = rooms;
        }

        Outcome<K> found() {
            Map<Integer, Integer> goals = new HashMap<>();
            for (int layer : request.goalLayers()) {
                rooms.get(layer).triangleAt(request.to()).ifPresent(triangle -> goals.put(layer, triangle));
            }
            if (goals.isEmpty()) {
                return new ClosedIn<>();
            }
            boolean edgeReached = false;
            for (int layer : request.startLayers()) {
                rooms.get(layer).triangleAt(request.from()).ifPresent(triangle -> offer(keyOf(layer, triangle), -1, request.from(), 0, null));
            }
            while (!open.isEmpty()) {
                Open next = open.poll();
                Reached<K> here = reached.get(next.key());
                if (next.way() > here.way()) {
                    continue;
                }
                int layer = layerOf(next.key());
                int number = triangleOf(next.key());
                if (goals.containsKey(layer) && goals.get(layer) == number) {
                    return new Found<>(partsTo(next.key()));
                }
                FreeSpace.Triangle triangle = rooms.get(layer).triangle(number);
                StepCost cost = request.layers().get(layer).cost();
                for (int side = 0; side < 3; side++) {
                    int neighbour = triangle.neighbours()[side];
                    if (neighbour < 0) {
                        edgeReached = edgeReached || FreeSpace.alongTheEdge(triangle, side, request.window());
                        continue;
                    }
                    Coordinate one = triangle.sideFrom(side);
                    Coordinate other = triangle.sideTo(side);
                    Coordinate entered = goals.containsKey(layer) && goals.get(layer) == neighbour ? request.to()
                            : new Coordinate((one.x + other.x) / 2, (one.y + other.y) / 2);
                    offer(keyOf(layer, neighbour), next.key(), entered, here.way() + cost.of(here.at(), entered), null);
                }
                Coordinate middle = middleOf(triangle);
                for (Hop<K> hop : request.hops().from(layer, middle)) {
                    Optional<Integer> landed = rooms.get(hop.layer()).triangleAt(hop.to());
                    if (landed.isPresent()) {
                        double way = here.way() + cost.of(here.at(), hop.from()) + hop.cost();
                        offer(keyOf(hop.layer(), landed.get()), next.key(), hop.to(), way, hop);
                    }
                }
            }
            return edgeReached ? new CutOffByTheWindow<>() : new ClosedIn<>();
        }

        private void offer(long key, long from, Coordinate at, double way, Hop<K> hop) {
            Reached<K> known = reached.get(key);
            if (known == null || way < known.way()) {
                reached.put(key, new Reached<>(from, at, way, hop));
                open.add(new Open(key, way, way + at.distance(request.to())));
            }
        }

        /** The way back to the start, as runs pulled taut through their corridors and the hops between them. */
        private List<Part<K>> partsTo(long goal) {
            List<Long> keys = new ArrayList<>();
            for (long key = goal; key != -1; key = reached.get(key).from()) {
                keys.add(key);
            }
            Collections.reverse(keys);
            List<Part<K>> parts = new ArrayList<>();
            List<Integer> corridor = new ArrayList<>();
            Coordinate entry = request.from();
            for (int at = 0; at < keys.size(); at++) {
                long key = keys.get(at);
                corridor.add(triangleOf(key));
                boolean last = at == keys.size() - 1;
                Hop<K> hop = last ? null : reached.get(keys.get(at + 1)).hop();
                if (last || hop != null) {
                    int layer = layerOf(key);
                    FreeSpace room = rooms.get(layer);
                    Coordinate exit = last ? request.to() : hop.from();
                    parts.add(new Run<>(layer, Funnel.pulled(entry, portalsOf(room, corridor), exit)));
                    if (hop != null) {
                        parts.add(new Hopped<>(layer, hop));
                        entry = hop.to();
                    }
                    corridor = new ArrayList<>();
                }
            }
            return parts;
        }
    }

    /** The sides a corridor crosses, from triangle to triangle: each with its end on the left of the way across it and its end on the right. */
    private static List<Sleeve.Portal> portalsOf(FreeSpace space, List<Integer> corridor) {
        List<Sleeve.Portal> portals = new ArrayList<>();
        for (int at = 0; at + 1 < corridor.size(); at++) {
            FreeSpace.Triangle triangle = space.triangle(corridor.get(at));
            for (int side = 0; side < 3; side++) {
                if (triangle.neighbours()[side] == corridor.get(at + 1)) {
                    // A triangle's corners run counter-clockwise, its inside on the left of each side: going out across it, the side's
                    // far end is on the left.
                    portals.add(new Sleeve.Portal(triangle.sideTo(side), triangle.sideFrom(side)));
                    break;
                }
            }
        }
        return portals;
    }
}
