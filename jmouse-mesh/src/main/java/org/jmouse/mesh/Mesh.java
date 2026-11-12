package org.jmouse.mesh;

import java.util.List;

/**
 * A 3D body as triangles, in parts of one material each — what every format it is read from becomes, and what a viewer
 * draws ({@link org.jmouse.mesh.gltf.GlbWriter}).
 */
public record Mesh(List<MeshPart> parts) {

    public Mesh {
        parts = List.copyOf(parts);
    }

    public int triangles() {
        return parts.stream().mapToInt(part -> part.triangles().length / 3).sum();
    }

    /**
     * The body in millimetres with Z up — the frame a board is drawn in —, from a file of {@code unit} millimetres a unit
     * standing up along {@code up}. A Y-up body is turned a quarter about X, so its Y becomes Z and its Z becomes −Y.
     */
    public Mesh placed(double unit, MeshFormats.Axis up) {
        return new Mesh(parts.stream().map(part -> placed(part, (float) unit, up)).toList());
    }

    private static MeshPart placed(MeshPart part, float unit, MeshFormats.Axis up) {
        float[] positions = new float[part.positions().length];
        float[] normals = new float[part.normals().length];
        for (int corner = 0; corner < positions.length; corner += 3) {
            turn(part.positions(), positions, corner, unit, up);
            turn(part.normals(), normals, corner, 1, up);
        }
        return new MeshPart(part.name(), part.material(), positions, normals, part.triangles());
    }

    private static void turn(float[] from, float[] to, int corner, float factor, MeshFormats.Axis up) {
        float x = from[corner];
        float y = from[corner + 1];
        float z = from[corner + 2];
        to[corner] = x * factor;
        to[corner + 1] = (up == MeshFormats.Axis.Y ? -z : y) * factor;
        to[corner + 2] = (up == MeshFormats.Axis.Y ? y : z) * factor;
    }
}
