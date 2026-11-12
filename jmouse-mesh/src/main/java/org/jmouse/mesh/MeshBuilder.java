package org.jmouse.mesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gathers a body triangle by triangle, a part for each material. A flat face gives each triangle its face's normal: a
 * package's box is flat faces and sharp edges, and a flat normal draws it as it is — a file's own normals, present or
 * not, need not be trusted. A turned body ({@link #revolved}) is shaded smooth, each corner with the normal of the round
 * it lies on, so a sleeve or a rolled top reads as round however few sides it is drawn with.
 */
public final class MeshBuilder {

    private final Map<MeshMaterial, Gathered> parts = new LinkedHashMap<>();

    /** A part as it grows: its corners' positions and normals, x, y, z each. */
    private static final class Gathered {

        private float[] positions = new float[3 * 3 * 64];
        private float[] normals = new float[3 * 3 * 64];
        private int size;

        void corner(double x, double y, double z, double normalX, double normalY, double normalZ) {
            if (size + 3 > positions.length) {
                positions = Arrays.copyOf(positions, positions.length * 2);
                normals = Arrays.copyOf(normals, normals.length * 2);
            }
            positions[size] = (float) x;
            positions[size + 1] = (float) y;
            positions[size + 2] = (float) z;
            normals[size] = (float) normalX;
            normals[size + 1] = (float) normalY;
            normals[size + 2] = (float) normalZ;
            size += 3;
        }
    }

    /** A triangle, its corners counter-clockwise seen from outside; one of no area is left out. */
    public MeshBuilder triangle(MeshMaterial material, double[] first, double[] second, double[] third) {
        double ux = second[0] - first[0];
        double uy = second[1] - first[1];
        double uz = second[2] - first[2];
        double vx = third[0] - first[0];
        double vy = third[1] - first[1];
        double vz = third[2] - first[2];
        double nx = uy * vz - uz * vy;
        double ny = uz * vx - ux * vz;
        double nz = ux * vy - uy * vx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length == 0 || Double.isNaN(length)) {
            return this;
        }
        Gathered part = parts.computeIfAbsent(material, ignored -> new Gathered());
        for (double[] corner : new double[][] {first, second, third}) {
            part.corner(corner[0], corner[1], corner[2], nx / length, ny / length, nz / length);
        }
        return this;
    }

    /** A flat polygon, its corners in order, cut into a fan of triangles from its first. */
    public MeshBuilder polygon(MeshMaterial material, List<double[]> corners) {
        for (int corner = 1; corner + 1 < corners.size(); corner++) {
            triangle(material, corners.getFirst(), corners.get(corner), corners.get(corner + 1));
        }
        return this;
    }

    /**
     * A box {@code width} along X, {@code depth} along Y and {@code height} along Z, centred on its own origin and set
     * where {@code placement} puts it.
     */
    public MeshBuilder box(MeshMaterial material, Placement placement, double width, double depth, double height) {
        double x = width / 2;
        double y = depth / 2;
        double z = height / 2;
        double[][] corners = {
                {-x, -y, -z}, {x, -y, -z}, {x, y, -z}, {-x, y, -z},
                {-x, -y, z}, {x, -y, z}, {x, y, z}, {-x, y, z}};
        // Each face counter-clockwise seen from outside: bottom, top, then the four sides.
        int[][] faces = {{0, 3, 2, 1}, {4, 5, 6, 7}, {0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}};
        for (int[] face : faces) {
            List<double[]> placed = new ArrayList<>();
            for (int corner : face) {
                placed.add(placement.apply(corners[corner][0], corners[corner][1], corners[corner][2]));
            }
            polygon(material, outward(placed, placement));
        }
        return this;
    }

    /**
     * A cylinder of {@code radius}, {@code length} long along Z, centred on its own origin and set where
     * {@code placement} puts it; its round drawn with {@code steps} flat sides.
     */
    public MeshBuilder cylinder(MeshMaterial material, Placement placement, double radius, double length, int steps) {
        double half = length / 2;
        List<double[]> bottom = new ArrayList<>();
        List<double[]> top = new ArrayList<>();
        for (int step = 0; step < steps; step++) {
            double angle = 2 * Math.PI * step / steps;
            bottom.add(placement.apply(radius * Math.cos(angle), radius * Math.sin(angle), -half));
            top.add(placement.apply(radius * Math.cos(angle), radius * Math.sin(angle), half));
        }
        polygon(material, outward(top, placement));
        polygon(material, outward(bottom.reversed(), placement));
        for (int step = 0; step < steps; step++) {
            int next = (step + 1) % steps;
            polygon(material, outward(List.of(bottom.get(step), bottom.get(next), top.get(next), top.get(step)), placement));
        }
        return this;
    }

    /**
     * A convex outline of the XY plane, {@code [x, y]} counter-clockwise seen from above, raised along Z by {@code height}
     * from its own origin — a TO-92's D, a chamfered body —, set where {@code placement} puts it.
     */
    public MeshBuilder prism(MeshMaterial material, Placement placement, List<double[]> outline, double height) {
        List<double[]> bottom = new ArrayList<>();
        List<double[]> top = new ArrayList<>();
        for (double[] point : outline) {
            bottom.add(placement.apply(point[0], point[1], 0));
            top.add(placement.apply(point[0], point[1], height));
        }
        polygon(material, outward(top, placement));
        polygon(material, outward(bottom.reversed(), placement));
        for (int corner = 0; corner < outline.size(); corner++) {
            int next = (corner + 1) % outline.size();
            polygon(material, outward(List.of(bottom.get(corner), bottom.get(next), top.get(next), top.get(corner)), placement));
        }
        return this;
    }

    /**
     * A body turned about Z: its {@code profile}, {@code [radius, z]} points from the bottom up, swept from
     * {@code fromDegrees} to {@code toDegrees} counter-clockwise seen from above — a whole turn for a sleeve, a part of one
     * for a stripe on it —, set where {@code placement} puts it. Its surface is shaded smooth round the turn and along the
     * profile, and keeps an edge where the profile turns sharper than {@value #CREASE_DEGREES}°: a rolled top stays round,
     * a sleeve's rim stays sharp. A whole turn whose profile ends off the axis is closed there by a flat disc.
     *
     * @param steps how many sides a whole turn is drawn with
     */
    public MeshBuilder revolved(MeshMaterial material, Placement placement, List<double[]> profile, double fromDegrees, double toDegrees,
                                int steps) {
        if (profile.size() < 2) {
            return this;
        }
        double sweep = toDegrees - fromDegrees;
        boolean whole = Math.abs(sweep) >= 360 - 1e-9;
        int sides = Math.max(1, (int) Math.ceil(steps * Math.abs(sweep) / 360));
        double[][] segmentNormals = new double[profile.size() - 1][];
        for (int index = 0; index + 1 < profile.size(); index++) {
            double radius = profile.get(index + 1)[0] - profile.get(index)[0];
            double rise = profile.get(index + 1)[1] - profile.get(index)[1];
            double length = Math.hypot(radius, rise);
            segmentNormals[index] = length == 0 ? new double[] {1, 0} : new double[] {rise / length, -radius / length};
        }
        Gathered part = parts.computeIfAbsent(material, ignored -> new Gathered());
        for (int index = 0; index + 1 < profile.size(); index++) {
            double[] lower = cornerNormal(segmentNormals, index, true);
            double[] upper = cornerNormal(segmentNormals, index, false);
            for (int side = 0; side < sides; side++) {
                double from = Math.toRadians(fromDegrees + sweep * side / sides);
                double to = Math.toRadians(fromDegrees + sweep * (side + 1) / sides);
                double[][] corners = {turned(profile.get(index), from), turned(profile.get(index), to),
                        turned(profile.get(index + 1), to), turned(profile.get(index + 1), from)};
                double[][] normals = {turnedNormal(lower, from), turnedNormal(lower, to), turnedNormal(upper, to), turnedNormal(upper, from)};
                smoothQuad(part, placement, corners, normals);
            }
        }
        if (whole) {
            disc(material, placement, profile.getFirst(), steps, false);
            disc(material, placement, profile.getLast(), steps, true);
        }
        return this;
    }

    /** How sharp a profile's turn may be and still be shaded as one round surface. */
    private static final double CREASE_DEGREES = 35;

    /** The normal at one end of a profile's segment: its own, or shared with its neighbour where the turn between is gentle. */
    private static double[] cornerNormal(double[][] segmentNormals, int index, boolean lower) {
        double[] own = segmentNormals[index];
        int neighbour = lower ? index - 1 : index + 1;
        if (neighbour < 0 || neighbour >= segmentNormals.length) {
            return own;
        }
        double[] other = segmentNormals[neighbour];
        double cosine = own[0] * other[0] + own[1] * other[1];
        if (cosine < Math.cos(Math.toRadians(CREASE_DEGREES))) {
            return own;
        }
        double radius = own[0] + other[0];
        double rise = own[1] + other[1];
        double length = Math.hypot(radius, rise);
        return length == 0 ? own : new double[] {radius / length, rise / length};
    }

    private static double[] turned(double[] point, double angle) {
        return new double[] {point[0] * Math.cos(angle), point[0] * Math.sin(angle), point[1]};
    }

    private static double[] turnedNormal(double[] normal, double angle) {
        return new double[] {normal[0] * Math.cos(angle), normal[0] * Math.sin(angle), normal[1]};
    }

    /** A flat disc closing a turned body where its profile ends off the axis: facing up at the top, down at the bottom. */
    private void disc(MeshMaterial material, Placement placement, double[] end, int steps, boolean top) {
        if (end[0] <= 0) {
            return;
        }
        List<double[]> rim = new ArrayList<>();
        for (int step = 0; step < steps; step++) {
            double angle = 2 * Math.PI * step / steps;
            rim.add(placement.apply(end[0] * Math.cos(angle), end[0] * Math.sin(angle), end[1]));
        }
        polygon(material, outward(top ? rim : rim.reversed(), placement));
    }

    /** A four-cornered piece of a curved surface, each corner with its own normal, as two triangles. */
    private static void smoothQuad(Gathered part, Placement placement, double[][] corners, double[][] normals) {
        double[][] placed = new double[4][];
        double[][] turned = new double[4][];
        double[] origin = placement.apply(0, 0, 0);
        for (int corner = 0; corner < 4; corner++) {
            placed[corner] = placement.apply(corners[corner][0], corners[corner][1], corners[corner][2]);
            double[] tip = placement.apply(normals[corner][0], normals[corner][1], normals[corner][2]);
            double x = tip[0] - origin[0];
            double y = tip[1] - origin[1];
            double z = tip[2] - origin[2];
            double length = Math.sqrt(x * x + y * y + z * z);
            turned[corner] = length == 0 ? new double[] {0, 0, 1} : new double[] {x / length, y / length, z / length};
        }
        int[][] triangles = placement.mirrors() ? new int[][] {{0, 2, 1}, {0, 3, 2}} : new int[][] {{0, 1, 2}, {0, 2, 3}};
        for (int[] triangle : triangles) {
            double[] first = placed[triangle[0]];
            double[] second = placed[triangle[1]];
            double[] third = placed[triangle[2]];
            double ux = second[0] - first[0];
            double uy = second[1] - first[1];
            double uz = second[2] - first[2];
            double vx = third[0] - first[0];
            double vy = third[1] - first[1];
            double vz = third[2] - first[2];
            double area = Math.sqrt(Math.pow(uy * vz - uz * vy, 2) + Math.pow(uz * vx - ux * vz, 2) + Math.pow(ux * vy - uy * vx, 2));
            if (area == 0 || Double.isNaN(area)) {
                continue;
            }
            for (int corner : triangle) {
                part.corner(placed[corner][0], placed[corner][1], placed[corner][2], turned[corner][0], turned[corner][1], turned[corner][2]);
            }
        }
    }

    /** A face's corners as drawn: a placement that turns a body inside out reverses which way round they go. */
    private static List<double[]> outward(List<double[]> corners, Placement placement) {
        return placement.mirrors() ? corners.reversed() : corners;
    }

    public Mesh build() {
        List<MeshPart> built = new ArrayList<>();
        parts.forEach((material, part) -> {
            float[] positions = Arrays.copyOf(part.positions, part.size);
            int[] triangles = new int[positions.length / 3];
            for (int corner = 0; corner < triangles.length; corner++) {
                triangles[corner] = corner;
            }
            built.add(new MeshPart(material.name(), material, positions, Arrays.copyOf(part.normals, part.size), triangles));
        });
        return new Mesh(built);
    }
}
