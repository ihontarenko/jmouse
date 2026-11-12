package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * What one track, this wide, must keep clear of: its obstacles, each with the gap it keeps from this one, indexed so a point
 * or a stretch is measured only against the copper near it. A fence is made once for a board and a track, and asked many times.
 */
public final class TrackFence {

    /** How much nearer than its gap copper may come and still keep it: what a millimetre written to the pcb's places loses, as the checks forgive. */
    public static final double GAP_TOLERANCE = Lines.SAME_POINT;

    /** A piece of copper a track keeps clear of, and how far. */
    public record Obstacle(CopperShape shape, double gap) {
    }

    /** The scale a cell of the index is keyed by, so a column and a row make one key. */
    private static final long CELLS = 65536;

    private final double                     width;
    private final double                     clearance;
    private final List<Obstacle>             obstacles;
    private final double                     bucket;
    private final double                     roomLooked;
    private final int                        curveSteps;
    private final double                     reachOut;
    private final Map<Long, List<Obstacle>>  buckets = new HashMap<>();
    /** Which grid points a search found free, for the grid it last asked of: a leg asked again as the pointer moves keeps them. */
    private String                           gridKey;
    private Map<Long, Double>                gridRooms;

    /**
     * @param bucket     how wide a cell of the index is, in millimetres
     * @param roomLooked how far round a point its room is looked for, in millimetres
     * @param curveSteps how many straight pieces a rounded corner is followed in
     */
    public TrackFence(double width, double clearance, List<Obstacle> obstacles, double bucket, double roomLooked, int curveSteps) {
        this.width = width;
        this.clearance = clearance;
        this.obstacles = List.copyOf(obstacles);
        this.bucket = bucket;
        this.roomLooked = roomLooked;
        this.curveSteps = curveSteps;
        double widest = clearance;
        for (Obstacle obstacle : this.obstacles) {
            widest = Math.max(widest, obstacle.gap());
            CopperShape.Box box = obstacle.shape().box();
            for (long column = cell(box.left()); column <= cell(box.right()); column++) {
                for (long row = cell(box.top()); row <= cell(box.bottom()); row++) {
                    buckets.computeIfAbsent(key(column, row), ignored -> new ArrayList<>()).add(obstacle);
                }
            }
        }
        this.reachOut = widest;
    }

    public double width() {
        return width;
    }

    public double clearance() {
        return clearance;
    }

    public List<Obstacle> obstacles() {
        return obstacles;
    }

    private long cell(double coordinate) {
        return (long) Math.floor(coordinate / bucket);
    }

    private static long key(long column, long row) {
        return (column + CELLS / 2) * CELLS + (row + CELLS / 2);
    }

    /** Whether a piece of copper keeps its gap from an obstacle. */
    private static boolean keepsFrom(CopperShape copper, Obstacle obstacle) {
        return !copper.within(obstacle.shape(), obstacle.gap()) || copper.gapTo(obstacle.shape()).gap() >= obstacle.gap() - GAP_TOLERANCE;
    }

    /** The obstacles whose cells reach within {@code reach} of a box, each once; all of them where the box covers more cells than there are obstacles. */
    private Iterable<Obstacle> near(double left, double top, double right, double bottom) {
        long fromColumn = cell(left);
        long toColumn = cell(right);
        long fromRow = cell(top);
        long toRow = cell(bottom);
        if ((toColumn - fromColumn + 1) * (toRow - fromRow + 1) > obstacles.size()) {
            return obstacles;
        }
        Set<Obstacle> found = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Obstacle> ordered = new ArrayList<>();
        for (long column = fromColumn; column <= toColumn; column++) {
            for (long row = fromRow; row <= toRow; row++) {
                for (Obstacle obstacle : buckets.getOrDefault(key(column, row), List.of())) {
                    if (found.add(obstacle)) {
                        ordered.add(obstacle);
                    }
                }
            }
        }
        return ordered;
    }

    /** Whether a piece of copper keeps its gap from everything near it. */
    public boolean copperClear(CopperShape copper) {
        CopperShape.Box box = copper.box();
        for (Obstacle obstacle : near(box.left() - reachOut, box.top() - reachOut, box.right() + reachOut, box.bottom() + reachOut)) {
            if (!keepsFrom(copper, obstacle)) {
                return false;
            }
        }
        return true;
    }

    /** Whether the track's end standing at a point keeps its gaps. */
    public boolean pointClear(Point point) {
        return copperClear(CopperShape.circle(point.x(), point.y(), width / 2));
    }

    /**
     * How much further than its gaps the track's end standing at a point is from everything: negative where it is too near, the
     * room looked for where nothing stands within that. Anything that moves less than this from the point keeps its gaps, so a
     * short step from it needs no measure.
     */
    public double roomAt(Point point) {
        CopperShape end = CopperShape.circle(point.x(), point.y(), width / 2);
        double reach = reachOut + roomLooked;
        double room = roomLooked;
        for (Obstacle obstacle : near(point.x() - reach, point.y() - reach, point.x() + reach, point.y() + reach)) {
            if (end.within(obstacle.shape(), obstacle.gap() + room)) {
                room = Math.min(room, end.gapTo(obstacle.shape()).gap() - obstacle.gap());
            }
        }
        return room;
    }

    /** Whether a line through points keeps its gaps, stretch by stretch. */
    public boolean lineClear(List<Point> points) {
        for (int index = 1; index < points.size(); index++) {
            if (!copperClear(CopperShape.line(List.of(points.get(index - 1), points.get(index)), width / 2))) {
                return false;
            }
        }
        return true;
    }

    /** The largest of the corners asked for down to none that keeps this line's gaps, its corners measured as they are drawn. */
    public double roundingThatFits(List<Point> points, double wanted, String layer) {
        for (double rounding : new double[] {wanted, wanted / 2, wanted / 4}) {
            if (rounding > 0 && copperClear(CopperShapes.track(new PcbDrawing.Track(layer, width, rounding, points), curveSteps))) {
                return rounding;
            }
        }
        return 0;
    }

    /**
     * The fence as a track standing at these points sees it: copper a point already stands too near — the neighbour of the pad
     * it leaves, on a fine pitch — may not be come nearer than it is, but need not be left at once. A track may leave it, never
     * cross it.
     */
    public TrackFence leaving(List<Point> points) {
        List<CopperShape> ends = points.stream().map(point -> CopperShape.circle(point.x(), point.y(), width / 2)).toList();
        boolean relaxed = false;
        List<Obstacle> eased = new ArrayList<>(obstacles.size());
        for (Obstacle obstacle : obstacles) {
            double tightest = Double.POSITIVE_INFINITY;
            for (CopperShape end : ends) {
                if (!keepsFrom(end, obstacle)) {
                    tightest = Math.min(tightest, end.gapTo(obstacle.shape()).gap());
                }
            }
            if (tightest == Double.POSITIVE_INFINITY) {
                eased.add(obstacle);
                continue;
            }
            relaxed = true;
            eased.add(new Obstacle(obstacle.shape(), Math.max(0, tightest) - GAP_TOLERANCE));
        }
        return relaxed ? new TrackFence(width, clearance, eased, bucket, roomLooked, curveSteps) : this;
    }

    /** The room a search has found at the points of the grid it asks by this key ({@link #roomAt}): kept while the same grid is asked again. */
    public Map<Long, Double> gridMemory(String key) {
        if (!key.equals(gridKey)) {
            gridKey = key;
            gridRooms = new HashMap<>();
        }
        return gridRooms;
    }
}
