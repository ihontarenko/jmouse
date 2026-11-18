package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.geometry.Areas;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.List;
import java.util.function.UnaryOperator;

/** The pcb's points as the plane geometry has them, and back; shapes carried as a part is. */
final class Plane {

    /** How many nanometres a millimetre is: KiCad keeps a pcb's every coordinate in nanometres. */
    private static final double NANOMETRES_A_MILLIMETRE = 1_000_000;

    static final GeometryFactory FACTORY = new GeometryFactory();

    /**
     * How a corridor's obstacles are united and cut: on a nanometre grid, by snap rounding, which no near-miss of two edges can make
     * fail — in any runtime, whatever order the obstacles come in —, far below any gap a router keeps. The grid is the operation's,
     * never the factory's: a buffer of a geometry of a fixed grid keeps to it, with no coarser one to fall back on.
     */
    static final Areas.Grid GRID = Areas.onGrid(NANOMETRES_A_MILLIMETRE);

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
    static Geometry carried(Geometry shape, Placement from, Placement to) {
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
