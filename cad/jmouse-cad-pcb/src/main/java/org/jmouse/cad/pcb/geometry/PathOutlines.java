package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.svg.path.node.PathBuilder;
import org.jmouse.svg.path.node.PathPolylines;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.jmouse.svg.path.parser.PathParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A path of several outlines — the pcb's edge and its cut-outs, a shape drawn of more than one ring — told apart: each
 * subpath, whether it closes, and where an open one ends.
 */
public final class PathOutlines {

    private static final Pattern MOVE = Pattern.compile("(?=[Mm])");
    private static final Pattern CLOSED = Pattern.compile("[zZ]\s*$");
    private static final PathParser PATHS = new PathParser();

    private PathOutlines() {
    }

    /** Each outline the path draws, from each move to the next. */
    public static List<String> subpaths(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        return Arrays.stream(MOVE.split(path)).map(String::strip).filter(subpath -> !subpath.isEmpty()).toList();
    }

    /** Whether an outline closes on itself: only then does it bound anything. */
    public static boolean isClosed(String subpath) {
        return CLOSED.matcher(subpath).find();
    }

    /** Where a path is still open: both ends of every outline of it that does not close. */
    public static List<PcbDrawing.Point> openEnds(String path, int curveSteps) {
        List<PcbDrawing.Point> ends = new ArrayList<>();
        for (String subpath : subpaths(path)) {
            if (isClosed(subpath)) {
                continue;
            }
            List<List<double[]>> runs = PathPolylines.of(PATHS.parse(subpath), curveSteps);
            if (runs.isEmpty() || runs.getFirst().isEmpty()) {
                continue;
            }
            List<double[]> run = runs.getFirst();
            ends.add(new PcbDrawing.Point(run.getFirst()[0], run.getFirst()[1]));
            ends.add(new PcbDrawing.Point(run.getLast()[0], run.getLast()[1]));
        }
        return ends;
    }

    /**
     * An area as path data, each ring a closed outline — shells and holes alike, to be filled by the even-odd rule —, as
     * an editor draws a pour; empty for an empty area.
     */
    public static String pathOf(Geometry area) {
        PathBuilder path = PathBuilder.path();
        for (int index = 0; index < area.getNumGeometries(); index++) {
            if (area.getGeometryN(index) instanceof Polygon polygon) {
                ring(path, polygon.getExteriorRing().getCoordinates());
                for (int hole = 0; hole < polygon.getNumInteriorRing(); hole++) {
                    ring(path, polygon.getInteriorRingN(hole).getCoordinates());
                }
            }
        }
        return path.build().toSource();
    }

    /** A closed outline through these points as path data; empty for none. */
    public static String pathOf(List<PcbDrawing.Point> outline) {
        PathBuilder path = PathBuilder.path();
        for (int index = 0; index < outline.size(); index++) {
            PcbDrawing.Point point = outline.get(index);
            if (index == 0) {
                path.moveTo(written(point.x()), written(point.y()));
            } else {
                path.lineTo(written(point.x()), written(point.y()));
            }
        }
        if (!outline.isEmpty()) {
            path.close();
        }
        return path.build().toSource();
    }

    /** One ring: JTS closes a ring by repeating its first point, which the closing command says instead. */
    private static void ring(PathBuilder path, Coordinate[] points) {
        int last = points.length > 1 && points[0].equals2D(points[points.length - 1]) ? points.length - 1 : points.length;
        for (int index = 0; index < last; index++) {
            if (index == 0) {
                path.moveTo(written(points[index].x), written(points[index].y));
            } else {
                path.lineTo(written(points[index].x), written(points[index].y));
            }
        }
        if (last > 0) {
            path.close();
        }
    }

    /** A millimetre as a pcb writes it: to {@link PcbVocabulary#DECIMALS} places, what is finer being arithmetic's noise. */
    private static double written(double millimetres) {
        return BigDecimal.valueOf(millimetres).setScale(PcbVocabulary.DECIMALS, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * An area as closed rings of points, each without its closing point: every outline turning one way and every hole the
     * other, as JTS normalises them — what a 3D view fills by their turning.
     */
    public static List<List<PcbDrawing.Point>> ringsOf(Geometry area) {
        Geometry normal = area.copy();
        normal.normalize();
        List<List<PcbDrawing.Point>> rings = new ArrayList<>();
        for (int index = 0; index < normal.getNumGeometries(); index++) {
            if (normal.getGeometryN(index) instanceof Polygon polygon && !polygon.isEmpty()) {
                rings.add(pointsOf(polygon.getExteriorRing().getCoordinates()));
                for (int hole = 0; hole < polygon.getNumInteriorRing(); hole++) {
                    rings.add(pointsOf(polygon.getInteriorRingN(hole).getCoordinates()));
                }
            }
        }
        return rings;
    }

    private static List<PcbDrawing.Point> pointsOf(Coordinate[] ring) {
        int last = ring.length > 1 && ring[0].equals2D(ring[ring.length - 1]) ? ring.length - 1 : ring.length;
        List<PcbDrawing.Point> points = new ArrayList<>(last);
        for (int index = 0; index < last; index++) {
            points.add(new PcbDrawing.Point(written(ring[index].x), written(ring[index].y)));
        }
        return points;
    }
}
