package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.svg.path.node.ArcThrough;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Artwork as the area it covers — what a solid model of the pcb extrudes: each stroke grown by half its pen with round
 * ends, each pad its outline, each region less its holes, all of a layer united into one area. In the pcb's frame.
 */
public final class ArtworkAreas {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private final PadOutline outlines;
    private final int        curveSteps;

    /** @param curveSteps how many straight pieces an arc, and a quarter of a pen's round end, is drawn with */
    public ArtworkAreas(PadOutline outlines, int curveSteps) {
        this.outlines = outlines;
        this.curveSteps = curveSteps;
    }

    /** Every feature's area, united; empty for none. */
    public Geometry areaOf(List<ArtworkFeature> features) {
        List<Geometry> areas = new ArrayList<>();
        for (ArtworkFeature feature : features) {
            areaOf(feature).filter(area -> !area.isEmpty()).ifPresent(areas::add);
        }
        return Areas.unionOf(areas, GEOMETRY);
    }

    private Optional<Geometry> areaOf(ArtworkFeature feature) {
        return switch (feature) {
            case ArtworkFeature.Stroke stroke -> Optional.of(stroked(stroke.points(), stroke.width()));
            case ArtworkFeature.Arc arc -> Optional.of(stroked(arcPoints(arc), arc.width()));
            case ArtworkFeature.Pad pad -> polygon(outlines.placed(pad.placement(), pad.pad(), pad.growth()), List.of());
            case ArtworkFeature.Disc disc -> Optional.of(GEOMETRY.createPoint(coordinate(disc.centre())).buffer(disc.diameter() / 2, curveSteps));
            case ArtworkFeature.Region region -> polygon(region.outline(), region.holes());
        };
    }

    private Geometry stroked(List<Point> points, double width) {
        Coordinate[] line = points.stream().map(ArtworkAreas::coordinate).toArray(Coordinate[]::new);
        Geometry centre = line.length == 1 ? GEOMETRY.createPoint(line[0]) : GEOMETRY.createLineString(line);
        return centre.buffer(width / 2, curveSteps);
    }

    /** The arc as straight pieces from its start through its middle to its end; three in a line are a straight run. */
    public List<Point> arcPoints(ArtworkFeature.Arc arc) {
        Optional<ArcThrough> circle = ArcThrough.of(arc.from().x(), arc.from().y(), arc.through().x(), arc.through().y(),
                arc.to().x(), arc.to().y());
        if (circle.isEmpty()) {
            return List.of(arc.from(), arc.to());
        }
        int steps = Math.max(2, (int) Math.ceil(curveSteps * Math.abs(circle.get().sweep()) / (Math.PI / 2)));
        List<Point> points = new ArrayList<>();
        for (int step = 0; step <= steps; step++) {
            double[] at = circle.get().pointAt(step, steps);
            points.add(new Point(at[0], at[1]));
        }
        return points;
    }


    private static Optional<Geometry> polygon(List<Point> outline, List<List<Point>> holes) {
        Optional<LinearRing> shell = ring(outline);
        if (shell.isEmpty()) {
            return Optional.empty();
        }
        LinearRing[] inner = holes.stream().map(ArtworkAreas::ring).flatMap(Optional::stream).toArray(LinearRing[]::new);
        Polygon polygon = GEOMETRY.createPolygon(shell.get(), inner);
        // A drawn outline may cross itself; a zero buffer makes it the area it encloses.
        return Optional.of(polygon.isValid() ? polygon : polygon.buffer(0));
    }

    private static Optional<LinearRing> ring(List<Point> points) {
        List<Coordinate> coordinates = new ArrayList<>(points.stream().map(ArtworkAreas::coordinate).toList());
        if (coordinates.size() < 3) {
            return Optional.empty();
        }
        if (!coordinates.getFirst().equals2D(coordinates.getLast())) {
            coordinates.add(coordinates.getFirst());
        }
        return coordinates.size() < 4 ? Optional.empty() : Optional.of(GEOMETRY.createLinearRing(coordinates.toArray(Coordinate[]::new)));
    }

    private static Coordinate coordinate(Point point) {
        return new Coordinate(point.x(), point.y());
    }
}
