package org.jmouse.cad.plugin.mesh;

import org.jmouse.cad.exchange.pcb.PcbSolidLayers;
import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshMaterial;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.triangulate.polygon.ConstrainedDelaunayTriangulator;

/**
 * An area of the pcb's plane raised into a solid of triangles — the pcb with its holes, a layer over a face, a part's
 * body —: its top and its bottom the area cut into triangles, its sides a wall along every ring, each face turned
 * outwards. The area is in the drawing's frame, whose Y grows downwards; the solid in the model's, Y upwards and Z up.
 */
final class AreaPrisms {

    private AreaPrisms() {
    }

    /** The area raised from {@code bottom} by {@code thickness}, into the builder in the material. */
    static void raise(MeshBuilder builder, MeshMaterial material, Geometry area, double bottom, double thickness) {
        double top = bottom + thickness;
        Geometry triangles = ConstrainedDelaunayTriangulator.triangulate(area);
        for (int index = 0; index < triangles.getNumGeometries(); index++) {
            Coordinate[] corners = triangles.getGeometryN(index).getCoordinates();
            double[] first = upward(corners[0]);
            double[] second = upward(corners[1]);
            double[] third = upward(corners[2]);
            // Counter-clockwise seen from above, so the top faces up and the bottom, taken the other way round, down.
            if (Orientation.isCCW(new Coordinate[] {coordinate(first), coordinate(second), coordinate(third), coordinate(first)})) {
                cap(builder, material, first, second, third, bottom, top);
            } else {
                cap(builder, material, first, third, second, bottom, top);
            }
        }
        // What an operation leaves besides areas — a line where two met edge to edge — has no wall.
        for (Object extracted : PolygonExtracter.getPolygons(area)) {
            Polygon polygon = (Polygon) extracted;
            if (polygon.isEmpty()) {
                continue;
            }
            wall(builder, material, polygon.getExteriorRing().getCoordinates(), true, bottom, top);
            for (int hole = 0; hole < polygon.getNumInteriorRing(); hole++) {
                wall(builder, material, polygon.getInteriorRingN(hole).getCoordinates(), false, bottom, top);
            }
        }
    }

    /** {@code #rrggbb} and how it reflects as a glTF material, its sRGB colour made linear as glTF's factors are. */
    static MeshMaterial materialOf(String name, PcbSolidLayers.Finish finish) {
        int value = Integer.parseInt(finish.colour().substring(1), 16);
        return new MeshMaterial(name, linear(value >> 16 & 0xff), linear(value >> 8 & 0xff), linear(value & 0xff), 1,
                finish.metallic(), finish.roughness());
    }

    private static double linear(int channel) {
        return MeshMaterial.linear(channel / 255.0);
    }

    private static void cap(MeshBuilder builder, MeshMaterial material, double[] first, double[] second, double[] third,
                            double bottom, double top) {
        builder.triangle(material, at(first, top), at(second, top), at(third, top));
        builder.triangle(material, at(first, bottom), at(third, bottom), at(second, bottom));
    }

    /**
     * A wall along a ring, facing away from the area: walked with the area on its left — an outline counter-clockwise
     * from above, a hole clockwise —, each step's face looks to its right.
     */
    private static void wall(MeshBuilder builder, MeshMaterial material, Coordinate[] ring, boolean outline, double bottom,
                             double top) {
        Coordinate[] upward = new Coordinate[ring.length];
        for (int index = 0; index < ring.length; index++) {
            upward[index] = coordinate(upward(ring[index]));
        }
        boolean reversed = Orientation.isCCW(upward) != outline;
        for (int index = 0; index + 1 < upward.length; index++) {
            Coordinate from = reversed ? upward[index + 1] : upward[index];
            Coordinate to = reversed ? upward[index] : upward[index + 1];
            double[] fromBottom = {from.x, from.y, bottom};
            double[] toBottom = {to.x, to.y, bottom};
            double[] toTop = {to.x, to.y, top};
            double[] fromTop = {from.x, from.y, top};
            builder.triangle(material, fromBottom, toBottom, toTop);
            builder.triangle(material, fromBottom, toTop, fromTop);
        }
    }

    private static double[] upward(Coordinate point) {
        return new double[] {point.x, -point.y};
    }

    private static Coordinate coordinate(double[] point) {
        return new Coordinate(point[0], point[1]);
    }

    private static double[] at(double[] point, double height) {
        return new double[] {point[0], point[1], height};
    }
}
