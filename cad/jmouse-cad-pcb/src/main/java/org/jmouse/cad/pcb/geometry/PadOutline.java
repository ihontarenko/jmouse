package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.List;

/**
 * A pad's outline, grown all round by what a layer asks — nothing for copper, the mask's expansion for the mask —: its
 * rectangle with the corners its shape rounds, an oval's ends, or a custom pad's own path; turned by the pad's rotation.
 */
public final class PadOutline {

    private static final PathParser PATHS = new PathParser();

    private final int curveSteps;
    private final int cornerSteps;

    /**
     * @param curveSteps  how many straight pieces a custom pad's curve is drawn with
     * @param cornerSteps how many straight pieces a quarter of a rounded corner is drawn with
     */
    public PadOutline(int curveSteps, int cornerSteps) {
        this.curveSteps = curveSteps;
        this.cornerSteps = cornerSteps;
    }

    /** How many steps a curve of a custom pad's path is drawn in. */
    public int curveSteps() {
        return curveSteps;
    }

    /** How many steps a rounded corner is drawn in. */
    public int cornerSteps() {
        return cornerSteps;
    }

    /** The outline in the footprint's frame about the pad's centre. */
    public List<Point> of(FootprintDrawing.Pad pad, double growth) {
        double turn = Math.toRadians(pad.rotation());
        double cosine = Math.cos(turn);
        double sine = Math.sin(turn);
        List<double[]> corners = new ArrayList<>();
        if ("custom".equals(pad.shape()) && pad.path() != null && !pad.path().isBlank()) {
            PathPolylines.of(PATHS.parse(pad.path()), curveSteps).stream().findFirst().ifPresent(corners::addAll);
        } else {
            double halfWidth = pad.width() / 2 + growth;
            double halfHeight = pad.height() / 2 + growth;
            double radius = switch (pad.shape()) {
                case "oval", "circle" -> Math.min(halfWidth, halfHeight);
                case "rounded-rectangle" -> Math.min(pad.width(), pad.height()) * pad.roundness() + growth;
                default -> growth;
            };
            double insetX = halfWidth - radius;
            double insetY = halfHeight - radius;
            corners.addAll(corner(insetX, insetY, 0, radius));
            corners.addAll(corner(-insetX, insetY, Math.PI / 2, radius));
            corners.addAll(corner(-insetX, -insetY, Math.PI, radius));
            corners.addAll(corner(insetX, -insetY, 3 * Math.PI / 2, radius));
        }
        return corners.stream()
                .map(point -> new Point(point[0] * cosine + point[1] * sine, -point[0] * sine + point[1] * cosine))
                .toList();
    }

    /** The outline where its part stands it on the pcb. */
    public List<Point> placed(Placement placement, FootprintDrawing.Pad pad, double growth) {
        return of(pad, growth).stream().map(point -> placement.placed(pad.x() + point.x(), pad.y() + point.y())).toList();
    }

    /** A rounded corner: a quarter of a circle about {@code (x, y)} from {@code start}; a sharp one is its point. */
    private List<double[]> corner(double x, double y, double start, double radius) {
        if (radius <= 0) {
            return List.of(new double[] {x, y});
        }
        List<double[]> points = new ArrayList<>();
        for (int step = 0; step <= cornerSteps; step++) {
            double angle = start + (double) step / cornerSteps * (Math.PI / 2);
            points.add(new double[] {x + radius * Math.cos(angle), y + radius * Math.sin(angle)});
        }
        return points;
    }
}
