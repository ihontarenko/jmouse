package org.jmouse.geometry;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two areas that share an area and also touch along an edge elsewhere: JTS answers their intersection with the edge beside the
 * area, a collection a union refuses. Their common area is the area alone — a thermal spoke touching a pour's open area refused a
 * whole pcb until 2026-11-12.
 */
class AreasTest {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private static Geometry square(double left, double top, double right, double bottom) {
        return GEOMETRY.createPolygon(new Coordinate[] {new Coordinate(left, top), new Coordinate(right, top), new Coordinate(right, bottom),
                new Coordinate(left, bottom), new Coordinate(left, top)});
    }

    @Test
    void theCommonAreaOfAreasTouchingAlongAnEdgeIsTheAreaAloneAndTakesAUnion() {
        Geometry first = square(0, 0, 10, 10).union(square(20, 0, 30, 10));
        // Overlaps the first square, and only touches the second along its left edge.
        Geometry second = square(5, 0, 20, 10);
        Geometry common = Areas.common(first, second);
        assertEquals(2, common.getDimension());
        assertEquals(50, common.getArea(), 1e-9);
        assertEquals(50 + 100, common.union(square(20, 0, 30, 10)).getArea(), 1e-9);
    }
}
