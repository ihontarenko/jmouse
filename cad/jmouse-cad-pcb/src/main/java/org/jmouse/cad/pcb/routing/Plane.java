package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;
import java.util.function.UnaryOperator;

/** The pcb's points as the plane geometry has them, and back; shapes carried as a part is. */
final class Plane {

    /**
     * The grid the plane's areas are combined on: a nanometre, the unit KiCad keeps a pcb's every coordinate in, far below any gap a
     * router keeps. Areas of a fixed grid are united and cut by snap rounding ({@code Areas}), which no near-miss of two edges can make
     * fail — in any runtime, whatever order the obstacles come in.
     */
    private static final double NANOMETRES_A_MILLIMETRE = 1_000_000;

    static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(NANOMETRES_A_MILLIMETRE));

    private Plane() {
    }

    static Coordinate coordinateOf(Point point) {
        return new Coordinate(point.x(), point.y());
    }

    static List<Coordinate> coordinatesOf(List<Point> points) {
        return points.stream().map(Plane::coordinateOf).toList();
    }

    static List<Point> pointsOf(List<Coordinate> coordinates) {
        return coordinates.stream().map(coordinate -> new Point(coordinate.x, coordinate.y)).toList();
    }

    /** A shape with every point of it moved as a part takes it from where it stood to where it stands. */
    static Geometry carried(Geometry shape, PcbDrawing.Placement from, PcbDrawing.Placement to) {
        return moved(shape, point -> from.carriedTo(to, point));
    }

    /** A shape with every point of it moved so. */
    static Geometry moved(Geometry shape, UnaryOperator<Point> moving) {
        Geometry copy = shape.copy();
        copy.apply((CoordinateFilter) coordinate -> {
            Point moved = moving.apply(new Point(coordinate.x, coordinate.y));
            coordinate.x = moved.x();
            coordinate.y = moved.y();
        });
        copy.geometryChanged();
        return copy;
    }
}
