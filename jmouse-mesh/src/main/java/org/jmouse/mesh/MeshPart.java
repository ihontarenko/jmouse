package org.jmouse.mesh;

/**
 * One part of a body drawn in one material: its corners and the triangles between them.
 *
 * @param positions x, y, z of every corner
 * @param normals   x, y, z of the surface's normal at every corner, as many as the positions
 * @param triangles three corners' indices for every triangle, counter-clockwise seen from outside
 */
public record MeshPart(String name, MeshMaterial material, float[] positions, float[] normals, int[] triangles) {

    public MeshPart {
        if (positions.length % 3 != 0 || normals.length != positions.length || triangles.length % 3 != 0) {
            throw new IllegalArgumentException("A mesh part holds x, y, z for each corner and its normal, three corners a triangle");
        }
    }

    public int corners() {
        return positions.length / 3;
    }

    /** The smallest and largest x, y, z: {@code [minimum x, minimum y, minimum z, maximum x, maximum y, maximum z]}. */
    public float[] bounds() {
        float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int corner = 0; corner < positions.length; corner += 3) {
            for (int axis = 0; axis < 3; axis++) {
                bounds[axis] = Math.min(bounds[axis], positions[corner + axis]);
                bounds[axis + 3] = Math.max(bounds[axis + 3], positions[corner + axis]);
            }
        }
        return bounds;
    }
}
