package org.jmouse.geometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Areas combined so that no combination throws: every union, intersection and difference of areas goes through here, on JTS's robust
 * overlay ({@link OverlayNGRobust}) — floating first, then snapped, then on a precision model it chooses — never the legacy overlay
 * {@link Geometry#union(Geometry)} and its kin take, which refuse two edges that almost meet with a {@code TopologyException}
 * («found non-noded intersection»): a board's pour on 2026-11-16, two segments ending a thousandth of a millimetre apart.
 * <p>
 * And copper is an area, and only one: where two areas meet only along an edge or at a corner, an intersection answers with the lines and
 * points they touch at beside the area they share — a collection no later union or difference takes —, so what is common is the area alone
 * ({@link #common}).
 */
public final class Areas {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private Areas() {
    }

    /** The area of a geometry: its polygons, the lines and points beside them left out. */
    public static Geometry areal(Geometry geometry) {
        if (geometry.getDimension() == 2 && !geometry.getGeometryType().equals(Geometry.TYPENAME_GEOMETRYCOLLECTION)) {
            return geometry;
        }
        List<Polygon> polygons = new ArrayList<>();
        collect(geometry, polygons);
        return GEOMETRY.createMultiPolygon(polygons.toArray(Polygon[]::new));
    }

    /** Two areas' common area: what both cover, never the edges they only touch along. */
    public static Geometry common(Geometry first, Geometry second) {
        return areal(OverlayNGRobust.overlay(first, second, OverlayNG.INTERSECTION));
    }

    /** What two geometries share, of whatever dimension — where a line crosses an outline. */
    public static Geometry crossing(Geometry first, Geometry second) {
        return OverlayNGRobust.overlay(first, second, OverlayNG.INTERSECTION);
    }

    /** Both areas together. */
    public static Geometry union(Geometry first, Geometry second) {
        return OverlayNGRobust.overlay(first, second, OverlayNG.UNION);
    }

    /** An area without another. */
    public static Geometry difference(Geometry area, Geometry taken) {
        return OverlayNGRobust.overlay(area, taken, OverlayNG.DIFFERENCE);
    }

    /** What one area or the other covers, not both: a ring's hole cut from the outline it lies in. */
    public static Geometry exclusive(Geometry first, Geometry second) {
        return OverlayNGRobust.overlay(first, second, OverlayNG.SYMDIFFERENCE);
    }

    /** Every area together, in one union; an empty area for none. */
    public static Geometry unionOf(Collection<? extends Geometry> areas) {
        return unionOf(areas, GEOMETRY);
    }

    /** Every area together, in one union, made by a factory of the caller's — an empty area of it for none. */
    public static Geometry unionOf(Collection<? extends Geometry> areas, GeometryFactory factory) {
        if (areas.isEmpty()) {
            return factory.createPolygon();
        }
        return OverlayNGRobust.union(factory.buildGeometry(areas));
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
