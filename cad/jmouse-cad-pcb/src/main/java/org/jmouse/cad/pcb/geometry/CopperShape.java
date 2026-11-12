package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Copper as a shape to measure between: straight pieces grown by a radius — a track is its centre line grown by half its
 * width, a round pad a point grown by its radius, a rectangle the area its sides close —, so one measure answers how near
 * any two come: the nearest pieces' distance, less both radii. Exact for what a round pen draws, which is why the checks
 * measure on it rather than on polygons a buffer approximates.
 *
 * @param pieces each straight piece as {@code x1, y1, x2, y2}; a point is a piece of no length
 * @param area   whether the pieces close round an area that counts as copper too — a rectangular pad does, a track does not
 * @param box    the box it covers, radius included: what is compared first
 */
public record CopperShape(List<double[]> pieces, boolean area, double radius, Box box) {

    /** A box: left, top, right, bottom. */
    public record Box(double left, double top, double right, double bottom) {

        /** Whether another box comes within a distance of this one. */
        public boolean within(Box other, double distance) {
            return left - distance <= other.right && other.left - distance <= right && top - distance <= other.bottom
                   && other.top - distance <= bottom;
        }

        /** Whether it overlaps another box, not only touches it. */
        public boolean overlaps(Box other) {
            return left < other.right && other.left < right && top < other.bottom && other.top < bottom;
        }
    }

    /** How near two shapes come — 0 or less where they touch or overlap —, and where: the middle of the gap. */
    public record Gap(double gap, PcbDrawing.Point at) {
    }

    private static final PathParser PATHS = new PathParser();

    public CopperShape {
        pieces = List.copyOf(pieces);
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
    public static CopperShape line(List<PcbDrawing.Point> points, double radius) {
        List<double[]> pieces = new ArrayList<>();
        for (int index = 1; index < points.size(); index++) {
            PcbDrawing.Point from = points.get(index - 1);
            PcbDrawing.Point to = points.get(index);
            pieces.add(new double[] {from.x(), from.y(), to.x(), to.y()});
        }
        if (pieces.isEmpty()) {
            PcbDrawing.Point only = points.getFirst();
            pieces.add(new double[] {only.x(), only.y(), only.x(), only.y()});
        }
        return of(pieces, false, radius);
    }

    /** The area a ring of points closes, its line not grown. */
    public static CopperShape ring(List<PcbDrawing.Point> points) {
        List<double[]> pieces = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            PcbDrawing.Point from = points.get(index);
            PcbDrawing.Point to = points.get((index + 1) % points.size());
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
        return new CopperShape(pieces, false, radius, box);
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
            PcbDrawing.Point at = area ? new PcbDrawing.Point(second[0], second[1]) : new PcbDrawing.Point(first[0], first[1]);
            return new Gap(-Math.max(radius, other.radius), at);
        }
        return new Gap(best - radius - other.radius, new PcbDrawing.Point((from[0] + to[0]) / 2, (from[1] + to[1]) / 2));
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
