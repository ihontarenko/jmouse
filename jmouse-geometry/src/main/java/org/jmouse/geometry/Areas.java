package org.jmouse.geometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.operation.overlayng.UnaryUnionNG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Areas combined so that no combination throws: every union, intersection and difference of areas goes through here, on JTS's robust
 * overlay ({@link OverlayNGRobust}) — floating first, then snapped, then on a precision model it chooses — never the legacy overlay
 * {@link Geometry#union(Geometry)} and its kin take, which refuse two edges that almost meet with a {@code TopologyException}
 * («found non-noded intersection»): a board's pour on 2026-11-16, two segments ending a thousandth of a millimetre apart.
 * <p>
 * Where a caller needs more than that, it combines on a grid ({@link #onGrid}): snap rounding, which no arrangement of edges can make
 * fail — the floating overlay's tries can all fail where edges almost meet in a dense pcb, and which do depends on the order the parts
 * come in, which is not the same in every runtime. The grid is the operation's, never the geometry factory's: a factory of a fixed
 * grid would make every buffer of its geometries keep to that grid too, and a buffer kept to one grid has no coarser one to fall back on.
 * <p>
 * And copper is an area, and only one: where two areas meet only along an edge or at a corner, an intersection answers with the lines and
 * points they touch at beside the area they share — a collection no later union or difference takes —, so what is common is the area alone
 * ({@link #common}).
 */
public final class Areas {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private Areas() {
    }

    /**
     * How areas are combined: in floating point, robustly ({@link #FLOATING}), or on a fixed grid by snap rounding — no near-miss
     * of two edges can make that fail.
     *
     * @param precision the grid; a floating one for the floating way
     */
    public record Grid(PrecisionModel precision) {

        /** Areas combined in floating point, robustly: what {@link Areas}' own methods do. */
        public static final Grid FLOATING = new Grid(new PrecisionModel());

        /** Both areas together. */
        public Geometry union(Geometry first, Geometry second) {
            return overlay(first, second, OverlayNG.UNION);
        }

        /** An area without another. */
        public Geometry difference(Geometry area, Geometry taken) {
            return overlay(area, taken, OverlayNG.DIFFERENCE);
        }

        /** Two areas' common area alone. */
        public Geometry common(Geometry first, Geometry second) {
            return areal(overlay(first, second, OverlayNG.INTERSECTION));
        }

        /** Every area together, made by a factory of the caller's — an empty area of it for none. */
        public Geometry unionOf(Collection<? extends Geometry> areas, GeometryFactory factory) {
            if (areas.isEmpty()) {
                return factory.createPolygon();
            }
            Geometry together = factory.buildGeometry(areas);
            // Snapped to the grid, a sliver may collapse to a line: an area's union is its area alone.
            return precision.isFloating() ? OverlayNGRobust.union(together) : areal(UnaryUnionNG.union(together, precision));
        }

        /** Two areas overlaid: robustly in floating point, or on the grid in strict mode — an area of areas, no collapsed line beside it. */
        private Geometry overlay(Geometry first, Geometry second, int operation) {
            if (precision.isFloating()) {
                return OverlayNGRobust.overlay(first, second, operation);
            }
            OverlayNG onGrid = new OverlayNG(first, second, precision, operation);
            onGrid.setStrictMode(true);
            return areal(onGrid.getResult());
        }
    }

    /** Areas combined on a grid of this many units a unit — a nanometre a millimetre — by snap rounding. */
    public static Grid onGrid(double scale) {
        return new Grid(new PrecisionModel(scale));
    }

    /** The area of a geometry: its polygons, the lines and points beside them left out. */
    public static Geometry areal(Geometry geometry) {
        if (geometry.getDimension() == 2 && !geometry.getGeometryType().equals(Geometry.TYPENAME_GEOMETRYCOLLECTION)) {
            return geometry;
        }
        List<Polygon> polygons = new ArrayList<>();
        collect(geometry, polygons);
        // Of the geometry's own factory, so an area of a fixed grid stays on it.
        return geometry.getFactory().createMultiPolygon(polygons.toArray(Polygon[]::new));
    }

    /** Two areas' common area: what both cover, never the edges they only touch along. */
    public static Geometry common(Geometry first, Geometry second) {
        return Grid.FLOATING.common(first, second);
    }

    /** What two geometries share, of whatever dimension — where a line crosses an outline. */
    public static Geometry crossing(Geometry first, Geometry second) {
        return OverlayNGRobust.overlay(first, second, OverlayNG.INTERSECTION);
    }

    /** Both areas together. */
    public static Geometry union(Geometry first, Geometry second) {
        return Grid.FLOATING.union(first, second);
    }

    /** An area without another. */
    public static Geometry difference(Geometry area, Geometry taken) {
        return Grid.FLOATING.difference(area, taken);
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
        return Grid.FLOATING.unionOf(areas, factory);
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
