package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxGrid;

import org.jmouse.geometry.index.Box;

import org.jmouse.numerics.Halving;

import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * What one track, this wide, must keep clear of: pieces of copper, each with the gap it keeps from this one, indexed so a point
 * or a stretch is measured only against the copper near it. A fence is made once for a board and a track, and asked many times.
 *
 * <p>The pieces and their index may be a whole layer's, shared by every fence over it ({@link BoxGrid}): a fence is then
 * only the gaps it keeps — none ({@code NaN}) from a piece that is not its obstacle —, so a board's many fences cost a gap
 * apiece, not an index apiece.</p>
 */
public final class TrackFence {

    /** How much nearer than its gap copper may come and still keep it: what a millimetre written to the pcb's places loses, as the checks forgive. */
    public static final double GAP_TOLERANCE = Precision.SAME_POINT;

    /**
     * What arithmetic in binary loses of millimetres written to the pcb's places, far below them: a gap measured exactly at the
     * tolerance between two points so written comes out a hair under it, and is still kept.
     */
    private static final double ARITHMETIC = 1e-9;

    /** A piece of copper a track keeps clear of, and how far. */
    public record Obstacle(CopperShape shape, double gap) {
    }

    private final double            width;
    private final double            clearance;
    private final List<CopperShape> shapes;
    /** The gap kept from each piece, by its number; {@code NaN} where the piece is not this fence's obstacle. */
    private final double[]          gaps;
    private final BoxIndex     cells;
    private final int               curveSteps;
    private final double            reachOut;
    /** A few obstacles more, kept clear of as well — the tracks laid over a world since its copper was indexed —; null where none. */
    private final TrackFence        also;
    /**
     * The fence this one was made from — by leaving its ends, by keeping clear of more —: the same copper and index, its own gaps;
     * null for a fence made of its own. What stands still among them is united once, for it and every fence made from it.
     */
    private final TrackFence        base;
    /** The still copper a corridor goes round, united once for this fence and every one made from it: used on a base alone. */
    private final StillCopper       still = new StillCopper();

    /**
     * A fence over obstacles of its own, indexed for it alone.
     *
     * @param grid       how the index's grid is cut
     * @param curveSteps how many straight pieces a rounded corner is followed in
     */
    public TrackFence(double width, double clearance, List<Obstacle> obstacles, BoxGrid.Cells grid, int curveSteps) {
        this(width, clearance, obstacles.stream().map(Obstacle::shape).toList(), obstacles.stream().mapToDouble(Obstacle::gap).toArray(),
             BoxGrid.of(obstacles.stream().map(obstacle -> obstacle.shape().box()).toList(), grid), curveSteps);
    }

    /** A fence over pieces already indexed — a layer's —, keeping these gaps from them ({@code NaN}: not an obstacle). */
    TrackFence(double width, double clearance, List<CopperShape> shapes, double[] gaps, BoxIndex cells, int curveSteps) {
        this(width, clearance, shapes, gaps, cells, curveSteps, null, null);
    }

    private TrackFence(double width, double clearance, List<CopperShape> shapes, double[] gaps, BoxIndex cells, int curveSteps,
                       TrackFence also, TrackFence base) {
        this.also = also;
        this.base = base;
        this.width = width;
        this.clearance = clearance;
        this.shapes = shapes;
        this.gaps = gaps;
        this.cells = cells;
        this.curveSteps = curveSteps;
        double widest = clearance;
        for (double gap : gaps) {
            if (!Double.isNaN(gap)) {
                widest = Math.max(widest, gap);
            }
        }
        this.reachOut = widest;
    }

    /**
     * This fence keeping clear of a few obstacles more as well, indexed apart: what a world asks once tracks were laid over it,
     * so its own copper is not indexed again for each one laid.
     *
     * @param grid how their index's grid is cut
     */
    TrackFence alsoKeepingClearOf(List<Obstacle> more, BoxGrid.Cells grid) {
        if (more.isEmpty()) {
            return this;
        }
        TrackFence extra = new TrackFence(width, clearance, more, grid, curveSteps);
        return new TrackFence(width, clearance, shapes, gaps, cells, curveSteps, also == null ? extra : also.joined(extra), base());
    }

    /** The obstacles of two fences of one track kept clear of together. */
    private TrackFence joined(TrackFence other) {
        return new TrackFence(width, clearance, shapes, gaps, cells, curveSteps, also == null ? other : also.joined(other), base());
    }

    /** The fence this one was made from, or this one where it was made of its own. */
    TrackFence base() {
        return base == null ? this : base;
    }

    /** The still copper a corridor goes round, kept by this fence's base for every fence made from it. */
    StillCopper stillCopper() {
        return base().still;
    }

    /** The numbers of the base's obstacles this fence keeps another gap from: eased where its ends stand. */
    BitSet easedFromBase() {
        BitSet eased = new BitSet();
        double[] own = base().gaps;
        if (own != gaps) {
            for (int number = 0; number < gaps.length; number++) {
                if (Double.compare(own[number], gaps[number]) != 0) {
                    eased.set(number);
                }
            }
        }
        return eased;
    }

    /** The base's obstacles whose cells a box reaches, with the gaps the base keeps, but those numbered in {@code leftOut}. */
    List<Obstacle> stillNear(double left, double top, double right, double bottom, BitSet leftOut) {
        TrackFence own = base();
        List<Obstacle> found = new ArrayList<>();
        BitSet near = own.near(left, top, right, bottom);
        near.andNot(leftOut);
        for (int number = near.nextSetBit(0); number >= 0; number = near.nextSetBit(number + 1)) {
            found.add(new Obstacle(shapes.get(number), own.gaps[number]));
        }
        return found;
    }

    /** What this fence keeps clear of besides its base's still copper near a box: the obstacles it eases, and the ones more it keeps clear of. */
    List<Obstacle> movingNear(double left, double top, double right, double bottom, BitSet eased) {
        List<Obstacle> found = new ArrayList<>();
        BitSet near = near(left, top, right, bottom);
        near.and(eased);
        for (int number = near.nextSetBit(0); number >= 0; number = near.nextSetBit(number + 1)) {
            found.add(new Obstacle(shapes.get(number), gaps[number]));
        }
        if (also != null) {
            found.addAll(also.obstaclesNear(left, top, right, bottom));
        }
        return found;
    }

    public double width() {
        return width;
    }

    public double clearance() {
        return clearance;
    }

    /** Whether copper keeps a gap from other copper, within the tolerance a router allows: the one test every fence asks. */
    public static boolean keepsClear(CopperShape copper, CopperShape other, double gap) {
        return !copper.within(other, gap) || copper.keepsGap(other, gap - GAP_TOLERANCE - ARITHMETIC);
    }

    /** The numbers of this fence's obstacles whose cells a box reaches, in order. */
    private BitSet near(double left, double top, double right, double bottom) {
        BitSet near = cells.nearSet(new Box(left, top, right, bottom));
        for (int number = near.nextSetBit(0); number >= 0; number = near.nextSetBit(number + 1)) {
            if (Double.isNaN(gaps[number])) {
                near.clear(number);
            }
        }
        return near;
    }

    /** How far from a track's centre line copper may stand and still be in its way: half its width and the widest gap it keeps. */
    double reach() {
        return width / 2 + Math.max(reachOut, also == null ? 0 : also.reachOut);
    }

    /** The obstacles whose cells a box reaches, each with the gap this track keeps from it — those of the fence joined to it too. */
    List<Obstacle> obstaclesNear(double left, double top, double right, double bottom) {
        List<Obstacle> found = new ArrayList<>();
        BitSet near = near(left, top, right, bottom);
        for (int number = near.nextSetBit(0); number >= 0; number = near.nextSetBit(number + 1)) {
            found.add(new Obstacle(shapes.get(number), gaps[number]));
        }
        if (also != null) {
            found.addAll(also.obstaclesNear(left, top, right, bottom));
        }
        return found;
    }

    /** Whether a piece of copper keeps its gap from everything near it. */
    public boolean copperClear(CopperShape copper) {
        Box box = copper.box();
        BitSet near = near(box.left() - reachOut, box.top() - reachOut, box.right() + reachOut, box.bottom() + reachOut);
        for (int number = near.nextSetBit(0); number >= 0; number = near.nextSetBit(number + 1)) {
            if (!keepsClear(copper, shapes.get(number), gaps[number])) {
                return false;
            }
        }
        return also == null || also.copperClear(copper);
    }

    /** Whether the track's end standing at a point keeps its gaps. */
    public boolean pointClear(Point point) {
        return copperClear(CopperShape.circle(point.x(), point.y(), width / 2));
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
    public double roundingThatFits(List<Point> points, double wanted, String layer, int halvings) {
        if (wanted <= 0 || points.size() < 3) {
            return Math.max(0, wanted);
        }
        if (cornersClear(points, wanted, layer)) {
            return wanted;
        }
        // Halved toward the widest that keeps every corner's gaps: the least rounding is no rounding.
        return Halving.bracket(0, wanted, halvings, tried -> cornersClear(points, tried, layer)).holds();
    }

    /**
     * Whether a line's corners, rounded so, keep their gaps: each corner by its own copper — the stretch coming in, the corner as rounded,
     * the stretch going on, the very arc the whole line is drawn with —, one corner at a time.
     */
    private boolean cornersClear(List<Point> points, double rounding, String layer) {
        for (int corner = 1; corner + 1 < points.size(); corner++) {
            Point before = points.get(corner - 1);
            Point at = points.get(corner);
            Point after = points.get(corner + 1);
            if (!copperClear(CopperShapes.track(new Track(layer, width, rounding, List.of(before, at, after)), curveSteps))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The fence as a track standing at these points sees it: copper a point already stands too near — the neighbour of the pad
     * it leaves, on a fine pitch — may not be come nearer than it is, but need not be left at once. A track may leave it, never
     * cross it. Only the obstacles near an end are asked; the fence it answers keeps the same pieces and index, its gaps eased.
     */
    public TrackFence leaving(List<Point> points) {
        // How near the nearest end stands to each piece it does not keep its gap from; infinite where every end keeps it.
        double[] tightest = null;
        for (Point point : points) {
            CopperShape end = CopperShape.circle(point.x(), point.y(), width / 2);
            Box box = end.box();
            BitSet near = near(box.left() - reachOut, box.top() - reachOut, box.right() + reachOut, box.bottom() + reachOut);
            for (int number = near.nextSetBit(0); number >= 0; number = near.nextSetBit(number + 1)) {
                CopperShape shape = shapes.get(number);
                // Copper that does not keep its gap stands nearer than it, so the gap measured within it is the whole answer.
                if (!keepsClear(end, shape, gaps[number])) {
                    if (tightest == null) {
                        tightest = new double[gaps.length];
                        Arrays.fill(tightest, Double.POSITIVE_INFINITY);
                    }
                    tightest[number] = Math.min(tightest[number], end.gapWithin(shape, gaps[number]));
                }
            }
        }
        TrackFence alsoLeaving = also == null ? null : also.leaving(points);
        if (tightest == null) {
            return alsoLeaving == also ? this : new TrackFence(width, clearance, shapes, gaps, cells, curveSteps, alsoLeaving, base());
        }
        double[] eased = Arrays.copyOf(gaps, gaps.length);
        for (int number = 0; number < eased.length; number++) {
            if (tightest[number] != Double.POSITIVE_INFINITY) {
                eased[number] = Math.max(0, tightest[number]) - GAP_TOLERANCE;
            }
        }
        return new TrackFence(width, clearance, shapes, eased, cells, curveSteps, alsoLeaving, base());
    }
}
