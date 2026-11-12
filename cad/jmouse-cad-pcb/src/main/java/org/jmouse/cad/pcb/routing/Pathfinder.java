package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.document.search.LatticeSearch;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * The way round: A* over a grid standing on the start, over every layer the track may use. A state is a point, a layer and the
 * direction it was reached in, so a corner can be charged for and the way comes out with few of them. Changing layer is one
 * more move, dearer than a long way round; so is a link over what is in the way. So a via, a pair of them hopping one track,
 * and a 0 Ω link are not cases of their own: the search takes them where they are the cheapest way there is. Quick searches
 * first, a coarse grid not far round; then finer, then farther, each only as far as its limit lets it.
 */
public final class Pathfinder {

    /** The eight steps of the grid, every other one straight. */
    private static final int[][] DIRECTIONS = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    /** Arrived on a lane by no step: just started, or just through a via or a link. */
    private static final int ARRIVED = DIRECTIONS.length;

    /** The straight steps a link may span. */
    private static final int[] STRAIGHT = {0, 2, 4, 6};

    /** Index keys shift a column and a row this far apart. */
    private static final long CELLS = 65536;

    /** One search a thread: routing runs one search at a time on a thread, its rows kept and never cleared between searches. */
    private static final ThreadLocal<LatticeSearch> SEARCH = ThreadLocal.withInitial(LatticeSearch::new);

    private Pathfinder() {
    }

    /** A layer the way may run on: the fence it keeps, and the way the layer's tracks run, with what a step against it costs. */
    public record Lane(TrackFence fence, Optional<Preferred> preferred) {
    }

    /** The way a layer's tracks run, and what a step against it costs as a share of one along it. */
    public record Preferred(RoutingSettings.Direction direction, double against) {
    }

    /** Changing layer: every lane's fence a via must keep, what it costs in steps, and how far it stands off the two ends. */
    public record ViaMove(List<TrackFence> fences, double cost, double offEnds) {
    }

    /** A link over what is in the way: how far its two pads stand apart, what each must keep on a lane, if it may stand there, and its cost. */
    public record LinkMove(double reach, IntFunction<Optional<TrackFence>> fence, double cost) {
    }

    /**
     * A way asked for.
     *
     * @param startLanes  the lanes the start reaches
     * @param goalLanes   the lanes the end reaches
     * @param angleStep   degrees between the directions a track may take: 90, or 45 for anything else, the router's own angles
     * @param searchLimit the most grid points looked at: a drag asks fewer, having a frame to answer in
     */
    public record Request(List<Lane> lanes, Point from, Point to, List<Integer> startLanes, List<Integer> goalLanes, double angleStep,
                          int searchLimit, Optional<ViaMove> via, Optional<LinkMove> link) {
    }

    /** What a way is made of, from its start to its end. */
    public sealed interface WayPart permits Stretch, ViaHop, LinkHop {
    }

    /** A run along one lane, by its corners. */
    public record Stretch(int lane, List<Point> points) implements WayPart {
    }

    /** A change of layer through a via. */
    public record ViaHop(Point at) implements WayPart {
    }

    /** A 0 Ω link over what is in the way, on a lane. */
    public record LinkHop(int lane, Point from, Point to) implements WayPart {
    }

    /** A way over the layers from its start to its end that keeps its gaps; empty where none is found. */
    public static Optional<List<WayPart>> layeredWay(Request request, RoutingSettings.Routing settings) {
        TrackFence fence = request.lanes().getFirst().fence();
        double pitch = (fence.width() + fence.clearance()) / 2;
        for (RoutingSettings.Search search : settings.searches()) {
            Optional<List<WayPart>> way = new Grid(request, pitch * search.share(), search.margin(), settings).search();
            if (way.isPresent()) {
                return way;
            }
        }
        return Optional.empty();
    }

    /** The corners of a way along one layer, the start among them; empty where none is found. */
    public static Optional<List<Point>> way(TrackFence fence, Point from, Point to, double angleStep, int searchLimit,
                                            Optional<Preferred> preferred, RoutingSettings.Routing settings) {
        Request request = new Request(List.of(new Lane(fence, preferred)), from, to, List.of(0), List.of(0), angleStep, searchLimit,
                Optional.empty(), Optional.empty());
        return layeredWay(request, settings).flatMap(parts -> parts.getFirst() instanceof Stretch stretch ? Optional.of(stretch.points())
                : Optional.empty());
    }

    /** What a step costs for the way it goes against the layer's: along it, its length; across it, {@code against} times that; a diagonal half way. */
    private static double againstFactor(Lane lane, int[] direction) {
        if (lane.preferred().isEmpty()) {
            return 1;
        }
        Preferred preferred = lane.preferred().get();
        boolean along = preferred.direction() == RoutingSettings.Direction.HORIZONTAL ? direction[0] != 0 && direction[1] == 0
                : direction[1] != 0 && direction[0] == 0;
        boolean diagonal = direction[0] != 0 && direction[1] != 0;
        if (along) {
            return 1;
        }
        return diagonal ? (1 + preferred.against()) / 2 : preferred.against();
    }

    /** One search over a grid of one step, standing on the start and reaching a margin past both ends. */
    private static final class Grid {

        private final Request                     request;
        private final double                      step;
        private final RoutingSettings.Routing     settings;
        private final int                         left;
        private final int                         top;
        private final int                         columns;
        private final int                         rows;
        private final int                         lanes;
        private final int[]                       moves;
        private final List<Map<Long, Double>>     rooms = new ArrayList<>();
        private final Map<Long, Boolean>          stepsKnown = new HashMap<>();
        private final Map<Long, Boolean>          viasKnown = new HashMap<>();
        private final int                         goalColumn;
        private final int                         goalRow;
        private final boolean                     diagonal;

        Grid(Request request, double step, double margin, RoutingSettings.Routing settings) {
            this.request = request;
            this.step = step;
            this.settings = settings;
            this.diagonal = request.angleStep() != 90;
            Point from = request.from();
            Point to = request.to();
            double dx = to.x() - from.x();
            double dy = to.y() - from.y();
            this.left = (int) Math.floor((Math.min(0, dx) - margin) / step);
            int right = (int) Math.ceil((Math.max(0, dx) + margin) / step);
            this.top = (int) Math.floor((Math.min(0, dy) - margin) / step);
            int bottom = (int) Math.ceil((Math.max(0, dy) + margin) / step);
            this.columns = right - left + 1;
            this.rows = bottom - top + 1;
            this.lanes = request.lanes().size();
            this.moves = diagonal ? new int[] {0, 1, 2, 3, 4, 5, 6, 7} : new int[] {0, 2, 4, 6};
            this.goalColumn = (int) Math.round(dx / step);
            this.goalRow = (int) Math.round(dy / step);
            String gridName = from.x() + "," + from.y() + "," + step;
            for (Lane lane : request.lanes()) {
                rooms.add(lane.fence().gridMemory(gridName));
            }
        }

        private boolean inside(int column, int row) {
            return column >= left && column < left + columns && row >= top && row < top + rows;
        }

        private Point pointOf(int column, int row) {
            return new Point(request.from().x() + column * step, request.from().y() + row * step);
        }

        private int nodeOf(int column, int row) {
            return (row - top) * columns + (column - left);
        }

        private static long key(int column, int row) {
            return (column + CELLS / 2) * CELLS + (row + CELLS / 2);
        }

        private double roomOf(int lane, int column, int row) {
            Map<Long, Double> room = rooms.get(lane);
            long at = key(column, row);
            Double known = room.get(at);
            if (known == null) {
                known = request.lanes().get(lane).fence().roomAt(pointOf(column, row));
                room.put(at, known);
            }
            return known;
        }

        private boolean isFree(int lane, int column, int row) {
            return roomOf(lane, column, row) >= -TrackFence.GAP_TOLERANCE;
        }

        /**
         * Whether a step between two free points keeps its gaps all along: what the way stands on is not all it runs over. A step
         * shorter than the room where it starts does; any other is measured, once for each of the eight ways from a point.
         */
        private boolean stepFree(int lane, int column, int row, int way, int nextColumn, int nextRow) {
            if (roomOf(lane, column, row) >= step * (way % 2 == 0 ? 1 : Math.sqrt(2))) {
                return true;
            }
            long at = (key(column, row) * 8 + way) * lanes + lane;
            return stepsKnown.computeIfAbsent(at, ignored -> request.lanes().get(lane).fence()
                    .lineClear(List.of(pointOf(column, row), pointOf(nextColumn, nextRow))));
        }

        private boolean viaFree(int column, int row) {
            Optional<ViaMove> via = request.via();
            if (via.isEmpty()) {
                return false;
            }
            return viasKnown.computeIfAbsent(key(column, row), ignored -> {
                Point point = pointOf(column, row);
                boolean offEnds = Math.min(Lines.lengthBetween(point, request.from()), Lines.lengthBetween(point, request.to())) >= via.get().offEnds();
                return offEnds && via.get().fences().stream().allMatch(fence -> fence.pointClear(point));
            });
        }

        private double estimate(int column, int row) {
            double across = Math.abs(goalColumn - column);
            double down = Math.abs(goalRow - row);
            return diagonal ? Math.max(across, down) + (Math.sqrt(2) - 1) * Math.min(across, down) : across + down;
        }

        private int stateOf(int node, int heading, int lane) {
            return (node * (DIRECTIONS.length + 1) + heading) * lanes + lane;
        }

        Optional<List<WayPart>> search() {
            if ((long) columns * rows * lanes > request.searchLimit()) {
                return Optional.empty();
            }
            if (request.goalLanes().stream().noneMatch(lane -> isFree(lane, goalColumn, goalRow))) {
                return Optional.empty();
            }
            int headings = DIRECTIONS.length + 1;
            LatticeSearch search = SEARCH.get();
            search.begin(columns * rows * headings * lanes);
            for (int lane : request.startLanes()) {
                search.offer(stateOf(nodeOf(0, 0), ARRIVED, lane), 0, -1, estimate(0, 0));
            }
            int linkSteps = request.link().map(link -> Math.max(1, (int) Math.round(link.reach() / step))).orElse(0);
            int reached = -1;
            for (int state = search.next(); state >= 0; state = search.next()) {
                int lane = state % lanes;
                double reachedCost = search.costOf(state);
                int rest = state / lanes;
                int heading = rest % headings;
                int node = rest / headings;
                int column = node % columns + left;
                int row = node / columns + top;
                if (column == goalColumn && row == goalRow && request.goalLanes().contains(lane)) {
                    reached = state;
                    break;
                }
                for (int index : moves) {
                    int[] direction = DIRECTIONS[index];
                    int nextColumn = column + direction[0];
                    int nextRow = row + direction[1];
                    if (!inside(nextColumn, nextRow) || !isFree(lane, nextColumn, nextRow)) {
                        continue;
                    }
                    // A diagonal step may not cut a corner the two straight steps beside it would both refuse.
                    if (direction[0] != 0 && direction[1] != 0 && !isFree(lane, column + direction[0], row) && !isFree(lane, column, row + direction[1])) {
                        continue;
                    }
                    if (!stepFree(lane, column, row, index, nextColumn, nextRow)) {
                        continue;
                    }
                    double turned = heading != ARRIVED && heading != index ? settings.turnCost() : 0;
                    double total = reachedCost + Math.hypot(direction[0], direction[1]) * againstFactor(request.lanes().get(lane), direction) + turned;
                    search.offer(stateOf(nodeOf(nextColumn, nextRow), index, lane), total, state, total + estimate(nextColumn, nextRow));
                }
                if (request.via().isPresent() && lanes > 1 && viaFree(column, row)) {
                    for (int other = 0; other < lanes; other++) {
                        if (other != lane && isFree(other, column, row)) {
                            double total = reachedCost + request.via().get().cost();
                            search.offer(stateOf(node, ARRIVED, other), total, state, total + estimate(column, row));
                        }
                    }
                }
                if (request.link().isPresent()) {
                    LinkMove link = request.link().get();
                    Optional<TrackFence> padFence = link.fence().apply(lane);
                    if (padFence.isPresent() && padFence.get().pointClear(pointOf(column, row))) {
                        for (int index : STRAIGHT) {
                            int nextColumn = column + DIRECTIONS[index][0] * linkSteps;
                            int nextRow = row + DIRECTIONS[index][1] * linkSteps;
                            if (inside(nextColumn, nextRow) && padFence.get().pointClear(pointOf(nextColumn, nextRow))) {
                                double total = reachedCost + link.cost();
                                search.offer(stateOf(nodeOf(nextColumn, nextRow), ARRIVED, lane), total, state, total + estimate(nextColumn, nextRow));
                            }
                        }
                    }
                }
            }
            if (reached < 0) {
                return Optional.empty();
            }
            return ended(partsOf(search.wayTo(reached)));
        }

        /** A grid point a way passes through, on its lane. */
        private record Passed(int lane, int column, int row) {
        }

        /** The way back to its start, as what it passes through, and what that is made of. */
        private Parts partsOf(int[] states) {
            int headings = DIRECTIONS.length + 1;
            List<Passed> passed = new ArrayList<>();
            for (int state : states) {
                int node = (state / lanes) / headings;
                passed.add(new Passed(state % lanes, node % columns + left, node / columns + top));
            }
            List<WayPart> parts = new ArrayList<>();
            int stretchLane = passed.getFirst().lane();
            List<Point> stretch = new ArrayList<>(List.of(pointOf(passed.getFirst().column(), passed.getFirst().row())));
            for (int at = 1; at < passed.size(); at++) {
                Passed was = passed.get(at - 1);
                Passed now = passed.get(at);
                Point point = Lines.settled(pointOf(now.column(), now.row()));
                if (now.lane() != was.lane()) {
                    close(parts, stretchLane, stretch);
                    parts.add(new ViaHop(point));
                    stretchLane = now.lane();
                    stretch = new ArrayList<>(List.of(point));
                } else if (Math.max(Math.abs(now.column() - was.column()), Math.abs(now.row() - was.row())) > 1) {
                    close(parts, stretchLane, stretch);
                    parts.add(new LinkHop(now.lane(), Lines.settled(pointOf(was.column(), was.row())), point));
                    stretchLane = now.lane();
                    stretch = new ArrayList<>(List.of(point));
                } else {
                    stretch.add(point);
                }
            }
            close(parts, stretchLane, stretch);
            return new Parts(parts, passed.getLast().lane());
        }

        private record Parts(List<WayPart> parts, int lastLane) {
        }

        private static void close(List<WayPart> parts, int lane, List<Point> stretch) {
            if (stretch.size() > 1) {
                parts.add(new Stretch(lane, Lines.cornersOf(stretch).stream().map(Lines::settled).toList()));
            }
        }

        /**
         * The way brought from the grid point nearest the end onto the end: best, the corner before it slides along the stretch
         * that reaches it until one straight leg at an allowed angle goes on to the end; else the last leg is laid from a corner
         * to the end. Empty when neither keeps clear: a stub where the grid missed would be no way.
         */
        private Optional<List<WayPart>> ended(Parts found) {
            List<WayPart> parts = found.parts();
            Point to = request.to();
            int lastLane = found.lastLane();
            TrackFence fence = request.lanes().get(lastLane).fence();
            WayPart last = parts.isEmpty() ? null : parts.getLast();
            if (!(last instanceof Stretch stretch)) {
                // It arrives by a via or a link, on the grid point nearest the end; or it never left the start.
                Point at = switch (last) {
                    case ViaHop via -> via.at();
                    case LinkHop link -> link.to();
                    case null, default -> request.from();
                };
                if (at.x() == to.x() && at.y() == to.y()) {
                    return Optional.of(parts);
                }
                List<Point> onto = Lines.joined(List.of(at), Lines.trackStep(at, to, request.angleStep()));
                if (!fence.lineClear(onto)) {
                    return Optional.empty();
                }
                List<WayPart> ended = new ArrayList<>(parts);
                ended.add(new Stretch(lastLane, onto));
                return Optional.of(ended);
            }
            List<Point> corners = stretch.points();
            List<Point> body = corners.subList(0, corners.size() - 1);
            if (body.size() >= 2) {
                Point previous = body.get(body.size() - 2);
                Point corner = body.getLast();
                double length = Lines.lengthBetween(previous, corner);
                Point along = new Point((corner.x() - previous.x()) / length, (corner.y() - previous.y()) / length);
                record Slide(double slide, Point at) {
                }
                List<Slide> slides = new ArrayList<>();
                for (int index : moves) {
                    Point direction = new Point(DIRECTIONS[index][0], DIRECTIONS[index][1]);
                    double crossing = along.x() * direction.y() - along.y() * direction.x();
                    if (Math.abs(crossing) < 1e-9) {
                        continue;
                    }
                    double dx = to.x() - corner.x();
                    double dy = to.y() - corner.y();
                    double slide = (dx * direction.y() - dy * direction.x()) / crossing;
                    double reach = (along.x() * dy - along.y() * dx) / crossing;
                    if (reach > Lines.SAME_POINT && slide > -length + Lines.SAME_POINT) {
                        slides.add(new Slide(slide, Lines.settled(Lines.offset(corner, along, slide))));
                    }
                }
                slides.sort(Comparator.comparingDouble(candidate -> Math.abs(candidate.slide())));
                for (Slide slide : slides) {
                    if (fence.lineClear(List.of(previous, slide.at(), to))) {
                        return Optional.of(finished(parts, lastLane, Lines.joined(body.subList(0, body.size() - 1), List.of(slide.at(), to))));
                    }
                }
            }
            for (int kept = corners.size() - 2; kept <= corners.size() - 1; kept++) {
                if (kept < 0) {
                    continue;
                }
                List<Point> onto = Lines.trackStep(corners.get(kept), to, request.angleStep());
                if (fence.lineClear(Lines.joined(List.of(corners.get(kept)), onto))) {
                    return Optional.of(finished(parts, lastLane, Lines.joined(corners.subList(0, kept + 1), onto)));
                }
            }
            return Optional.empty();
        }

        private static List<WayPart> finished(List<WayPart> parts, int lane, List<Point> points) {
            List<WayPart> ended = new ArrayList<>(parts.subList(0, parts.size() - 1));
            ended.add(new Stretch(lane, points));
            return ended;
        }
    }
}
