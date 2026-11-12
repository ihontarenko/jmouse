package org.jmouse.cad.pcb.geometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Copper as an area, and only as one. Where two areas meet only along an edge or at a corner, JTS answers their intersection
 * with the lines and points they touch at beside the area they share — a collection no later union or difference takes: a
 * thermal spoke touching the edge of a pour's open area refused a whole pcb on 2026-11-12. What copper is made of is the area.
 */
public final class Areas {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private Areas() {
    }

    /** The area of a geometry: its polygons, the lines and points beside them left out. */
    public static Geometry areal(Geometry geometry) {
        if (geometry.getDimension() == 2 && !(geometry.getGeometryType().equals(Geometry.TYPENAME_GEOMETRYCOLLECTION))) {
            return geometry;
        }
        List<Polygon> polygons = new ArrayList<>();
        collect(geometry, polygons);
        return GEOMETRY.createMultiPolygon(polygons.toArray(Polygon[]::new));
    }

    /** Two areas' common area: what both cover, never the edges they only touch along. */
    public static Geometry common(Geometry first, Geometry second) {
        return areal(first.intersection(second));
    }

    private static void collect(Geometry geometry, List<Polygon> polygons) {
        for (int index = 0; index < geometry.getNumGeometries(); index++) {
            Geometry part = geometry.getGeometryN(index);
            if (part instanceof Polygon polygon) {
                if (!polygon.isEmpty()) {
                    polygons.add(polygon);
                }
            } else if (part != geometry) {
                collect(part, polygons);
            }
        }
    }
}
