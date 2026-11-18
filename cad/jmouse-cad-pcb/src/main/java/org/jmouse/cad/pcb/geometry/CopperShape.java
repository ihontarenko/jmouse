package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.Point;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxGrid;

import org.jmouse.geometry.index.Box;

import org.jspecify.annotations.Nullable;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.DoublePredicate;

/**
 * Copper as a shape to measure between: straight pieces grown by a radius — a track is its centre line grown by half its
 * width, a round pad a point grown by its radius, a rectangle the area its sides close —, so one measure answers how near
 * any two come: the nearest pieces' distance, less both radii. Exact for what a round pen draws, which is why the checks
 * measure on it rather than on polygons a buffer approximates.
 *
 * @param pieces each straight piece as {@code x1, y1, x2, y2}; a point is a piece of no length
 * @param area   whether the pieces close round an area that counts as copper too — a rectangular pad does, a track does not
 * @param box    the box it covers, radius included: what is compared first
 * @param cells  its pieces by the cells of a grid, for a shape of many — a pour's outline —; null for one of a few
 */
public record CopperShape(List<double[]> pieces, boolean area, double radius, Box box, @Nullable BoxIndex cells) {

    /** How many pieces make a shape worth its cells: below it, walking every piece costs less than the cells do. */
    private static final int INDEXED_FROM = 32;

    /** A shape of these pieces, its cells built when it has many. */
    public CopperShape(List<double[]> pieces, boolean area, double radius, Box box) {
        this(pieces, area, radius, box, null);
    }

    /** How near two shapes come — 0 or less where they touch or overlap —, and where: the middle of the gap. */
    public record Gap(double gap, Point at) {
    }

    private static final PathParser PATHS = new PathParser();

    /** The box a piece — a stretch from one point to another — takes. */
    private static Box boxOfPiece(double[] piece) {
        return new Box(Math.min(piece[0], piece[2]), Math.min(piece[1], piece[3]), Math.max(piece[0], piece[2]), Math.max(piece[1], piece[3]));
    }

    public CopperShape {
        pieces = List.copyOf(pieces);
        if (cells == null && pieces.size() >= INDEXED_FROM) {
            cells = BoxGrid.even(pieces.stream().map(CopperShape::boxOfPiece).toList());
        }
    }

    /** A shape of these pieces, its box worked out. */
    public static CopperShape of(List<double[]> pieces, boolean area, double radius) {
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (double[] piece : pieces) {
            left = Math.min(left, Math.min(piece[0], piece[2]));
            right = Math.max(right, Math.max(piece[0], piece[2]));
            top = Math.min(top, Math.min(piece[1], piece[3]));
            bottom = Math.max(bottom, Math.max(piece[1], piece[3]));
        }
        return new CopperShape(pieces, area, radius, new Box(left - radius, top - radius, right + radius, bottom + radius));
    }

    /** A circle: a point grown by its radius. */
    public static CopperShape circle(double x, double y, double radius) {
        return of(List.<double[]>of(new double[] {x, y, x, y}), false, radius);
    }

    /** A line a round pen draws through its points, grown by the pen's radius. */
    public static CopperShape line(List<Point> points, double radius) {
        List<double[]> pieces = new ArrayList<>();
        for (int index = 1; index < points.size(); index++) {
            Point from = points.get(index - 1);
            Point to = points.get(index);
            pieces.add(new double[] {from.x(), from.y(), to.x(), to.y()});
        }
        if (pieces.isEmpty()) {
            Point only = points.getFirst();
            pieces.add(new double[] {only.x(), only.y(), only.x(), only.y()});
        }
        return of(pieces, false, radius);
    }

    /** The area a ring of points closes, its line not grown. */
    public static CopperShape ring(List<Point> points) {
        List<double[]> pieces = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            Point from = points.get(index);
            Point to = points.get((index + 1) % points.size());
            pieces.add(new double[] {from.x(), from.y(), to.x(), to.y()});
        }
        return of(pieces, true, 0);
    }

    /**
     * What a path draws, as pieces: every subpath, curves followed in {@code curveSteps} straight pieces; empty for a path
     * that draws nothing.
     *
     * @param area   whether what it closes counts as copper — a zone, the pcb's edge as an area —, or only its line does
     * @param radius how far its line is grown: half a pen's width, 0 for an area
     */
    public static Optional<CopperShape> ofPath(String path, boolean area, double radius, int curveSteps) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        List<double[]> pieces = new ArrayList<>();
        for (List<double[]> run : PathPolylines.of(PATHS.parse(path), curveSteps)) {
            for (int index = 1; index < run.size(); index++) {
                double[] from = run.get(index - 1);
                double[] to = run.get(index);
                if (from[0] != to[0] || from[1] != to[1]) {
                    pieces.add(new double[] {from[0], from[1], to[0], to[1]});
                }
            }
        }
        return pieces.isEmpty() ? Optional.empty() : Optional.of(of(pieces, area, radius));
    }

    /** The same pieces as a line alone, nothing inside counting: the pcb's edge as what copper keeps its gap from. */
    public CopperShape lineOnly() {
        return new CopperShape(pieces, false, radius, box, cells);
    }

    /** Whether another shape's box comes within a distance of this one's: what is asked before measuring. */
    public boolean within(CopperShape other, double distance) {
        return box.within(other.box, distance);
    }

    /** Whether a point stands inside the area the pieces close, by how many of them a ray from it crosses. */
    public boolean encloses(double x, double y) {
        boolean inside = false;
        for (double[] piece : pieces) {
            double y1 = piece[1];
            double y2 = piece[3];
            if ((y1 > y) != (y2 > y) && x < (piece[2] - piece[0]) * (y - y1) / (y2 - y1) + piece[0]) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Whether a point stands on the copper: inside the area the pieces close, or within the radius of them. */
    public boolean covers(double x, double y) {
        if (area && encloses(x, y)) {
            return true;
        }
        for (double[] piece : pieces) {
            double[] near = nearestOnPiece(piece, x, y);
            if (Math.hypot(near[0] - x, near[1] - y) <= radius + 1e-9) {
                return true;
            }
        }
        return false;
    }

    /** How near another shape's copper comes — 0 or less where they touch or overlap —, and where. */
    public Gap gapTo(CopperShape other) {
        double best = Double.POSITIVE_INFINITY;
        double[] from = {0, 0};
        double[] to = {0, 0};
        for (double[] one : pieces) {
            for (double[] two : other.pieces) {
                // The pieces' boxes stand at least as far apart as the pieces: a pair no nearer than the best already found is
                // passed over — the answer is the same, and a router asking this at every step of its search no longer measures
                // every piece of a pour against every piece of a track.
                if (boxGap(one, two) >= best) {
                    continue;
                }
                double[][] nearest = nearestOfPieces(one, two);
                double distance = Math.hypot(nearest[1][0] - nearest[0][0], nearest[1][1] - nearest[0][1]);
                if (distance < best) {
                    best = distance;
                    from = nearest[0];
                    to = nearest[1];
                }
            }
        }
        double[] first = pieces.getFirst();
        double[] second = other.pieces.getFirst();
        if ((area && encloses(second[0], second[1])) || (other.area && other.encloses(first[0], first[1]))) {
            Point at = area ? new Point(second[0], second[1]) : new Point(first[0], first[1]);
            return new Gap(-Math.max(radius, other.radius), at);
        }
        return new Gap(best - radius - other.radius, new Point((from[0] + to[0]) / 2, (from[1] + to[1]) / 2));
    }

    /**
     * Whether another shape's copper keeps at least a gap from this one's — what {@code gapTo(other).gap() >= gap} answers —, decided
     * without finding the nearest pair: a pair whose boxes stand that far apart already is passed over, and the first pair nearer
     * settles it. A router asks this of every step of its search, against every piece of a pour near it.
     */
    public boolean keepsGap(CopperShape other, double gap) {
        double[] first = pieces.getFirst();
        double[] second = other.pieces.getFirst();
        if ((area && encloses(second[0], second[1])) || (other.area && other.encloses(first[0], first[1]))) {
            return -Math.max(radius, other.radius) >= gap;
        }
        // Between the centre lines, the gap and both radii.
        double needed = gap + radius + other.radius;
        return !anyPairWithin(other, needed, distance -> distance < needed);
    }

    /**
     * Whether another shape's copper comes within a distance of this one's — what {@code gapTo(other).gap() <= distance} answers —,
     * decided by the first pair that near, a pair whose boxes stand farther apart passed over.
     */
    public boolean comesWithin(CopperShape other, double distance) {
        double[] first = pieces.getFirst();
        double[] second = other.pieces.getFirst();
        if ((area && encloses(second[0], second[1])) || (other.area && other.encloses(first[0], first[1]))) {
            return -Math.max(radius, other.radius) <= distance;
        }
        double reach = distance + radius + other.radius;
        return anyPairWithin(other, reach, near -> near <= reach);
    }

    /**
     * How near another shape's copper comes — what {@code gapTo(other).gap()} answers — where that is nearer than a limit; the limit
     * where it is not. What a question that cares only within a limit needs — the room round a point of a route search —: the pieces
     * farther off are never measured, and no nearest point is made.
     */
    public double gapWithin(CopperShape other, double limit) {
        double[] first = pieces.getFirst();
        double[] second = other.pieces.getFirst();
        if ((area && encloses(second[0], second[1])) || (other.area && other.encloses(first[0], first[1]))) {
            return Math.min(-Math.max(radius, other.radius), limit);
        }
        // Between the centre lines: the nearest pair found so far, a pair whose boxes stand no nearer passed over.
        double reach = limit + radius + other.radius;
        double[] nearest = {reach};
        anyPair(other, reach, (one, two) -> {
            if (boxGap(one, two) < nearest[0]) {
                nearest[0] = Math.min(nearest[0], pieceDistance(one, two));
            }
            return nearest[0] <= 0;
        });
        return nearest[0] < reach ? nearest[0] - radius - other.radius : limit;
    }

    /** Whether a piece of this shape and one of the other stand near enough to say yes: only pairs whose boxes come within a reach are measured. */
    private boolean anyPairWithin(CopperShape other, double reach, DoublePredicate near) {
        return anyPair(other, reach, (one, two) -> boxGap(one, two) <= reach && near.test(pieceDistance(one, two)));
    }

    /**
     * Whether any pair — a piece of this shape and one of the other — answers yes, the pairs whose boxes stand farther apart than a
     * reach possibly never asked: through the cells of the larger shape where it has them, so a step of a track asks the few pieces
     * of a pour beside it, not the hundreds round the pour.
     */
    private boolean anyPair(CopperShape other, double reach, BiPredicate<double[], double[]> question) {
        CopperShape indexed = cells != null && pieces.size() >= other.pieces.size() ? this : other.cells != null ? other : null;
        if (indexed == null) {
            for (double[] one : pieces) {
                for (double[] two : other.pieces) {
                    if (question.test(one, two)) {
                        return true;
                    }
                }
            }
            return false;
        }
        boolean indexedIsThis = indexed == this;
        CopperShape walked = indexedIsThis ? other : this;
        for (double[] one : walked.pieces) {
            boolean found = indexed.cells.anyNear(boxOfPiece(one).grown(reach), index -> {
                        double[] two = indexed.pieces.get(index);
                        return indexedIsThis ? question.test(two, one) : question.test(one, two);
                    });
            if (found) {
                return true;
            }
        }
        return false;
    }

    /** How near two pieces come: 0 where they cross, else the nearest of each end to the other piece — counted, never allocated. */
    private static double pieceDistance(double[] first, double[] second) {
        if (cross(first, second)) {
            return 0;
        }
        return Math.min(Math.min(distanceToPiece(second, first[0], first[1]), distanceToPiece(second, first[2], first[3])),
                        Math.min(distanceToPiece(first, second[0], second[1]), distanceToPiece(first, second[2], second[3])));
    }

    /** How far a point stands from a piece. */
    private static double distanceToPiece(double[] piece, double x, double y) {
        double dx = piece[2] - piece[0];
        double dy = piece[3] - piece[1];
        double length = dx * dx + dy * dy;
        double along = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - piece[0]) * dx + (y - piece[1]) * dy) / length));
        return Math.hypot(piece[0] + along * dx - x, piece[1] + along * dy - y);
    }

    /** How far apart two pieces' boxes stand — never farther than the pieces themselves —; 0 where the boxes meet. */
    private static double boxGap(double[] first, double[] second) {
        double across = Math.max(0, Math.max(Math.min(second[0], second[2]) - Math.max(first[0], first[2]),
                                             Math.min(first[0], first[2]) - Math.max(second[0], second[2])));
        double down = Math.max(0, Math.max(Math.min(second[1], second[3]) - Math.max(first[1], first[3]),
                                           Math.min(first[1], first[3]) - Math.max(second[1], second[3])));
        return Math.hypot(across, down);
    }

    /** The nearest two points of two pieces: where they cross, else the nearest of each end to the other piece. */
    private static double[][] nearestOfPieces(double[] first, double[] second) {
        if (cross(first, second)) {
            double[] at = crossing(first, second);
            return new double[][] {at, at};
        }
        double[][] ends = {{first[0], first[1]}, {first[2], first[3]}, {second[0], second[1]}, {second[2], second[3]}};
        double[][] on = {nearestOnPiece(second, first[0], first[1]), nearestOnPiece(second, first[2], first[3]),
                         nearestOnPiece(first, second[0], second[1]), nearestOnPiece(first, second[2], second[3])};
        double best = Double.POSITIVE_INFINITY;
        double[][] answer = {ends[0], ends[0]};
        for (int index = 0; index < 4; index++) {
            double distance = Math.hypot(on[index][0] - ends[index][0], on[index][1] - ends[index][1]);
            if (distance < best) {
                best = distance;
                answer = index < 2 ? new double[][] {ends[index], on[index]} : new double[][] {on[index], ends[index]};
            }
        }
        return answer;
    }

    private static double[] nearestOnPiece(double[] piece, double x, double y) {
        double dx = piece[2] - piece[0];
        double dy = piece[3] - piece[1];
        double length = dx * dx + dy * dy;
        double along = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - piece[0]) * dx + (y - piece[1]) * dy) / length));
        return new double[] {piece[0] + along * dx, piece[1] + along * dy};
    }

    private static double side(double ax, double ay, double bx, double by, double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static boolean cross(double[] first, double[] second) {
        double one = side(first[0], first[1], first[2], first[3], second[0], second[1]);
        double two = side(first[0], first[1], first[2], first[3], second[2], second[3]);
        double three = side(second[0], second[1], second[2], second[3], first[0], first[1]);
        double four = side(second[0], second[1], second[2], second[3], first[2], first[3]);
        return one * two < 0 && three * four < 0;
    }

    private static double[] crossing(double[] first, double[] second) {
        double denominator = (first[0] - first[2]) * (second[1] - second[3]) - (first[1] - first[3]) * (second[0] - second[2]);
        double along = ((first[0] - second[0]) * (second[1] - second[3]) - (first[1] - second[1]) * (second[0] - second[2])) / denominator;
        return new double[] {first[0] + along * (first[2] - first[0]), first[1] + along * (first[3] - first[1])};
    }
}
