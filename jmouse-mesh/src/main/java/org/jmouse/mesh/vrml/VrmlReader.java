package org.jmouse.mesh.vrml;

import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.mesh.MeshReader;
import org.jmouse.mesh.Placement;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * VRML97 as a mesh: every {@code Shape} whose geometry is an {@code IndexedFaceSet}, placed by the {@code Transform}s
 * above it and coloured by its {@code Material} — what a body exported for a 3D viewer is written as. Grouping nodes are
 * walked; a {@code Switch} shows its chosen child, an {@code LOD} its finest level. Lights, viewpoints, sensors, scripts
 * and texts draw no body and are passed over, as are the primitive {@code Box}, {@code Cone}, {@code Cylinder} and
 * {@code Sphere}, which a body exported as faces does not use.
 */
public final class VrmlReader implements MeshReader {

    private static final String END_OF_FACE = "-1";
    private static final double DIFFUSE = 0.8;
    private static final double SHININESS = 0.2;

    private final VrmlParser parser = new VrmlParser();

    @Override
    public String format() {
        return "vrml";
    }

    @Override
    public Mesh read(InputStream input, String name) throws IOException {
        List<VrmlNode> nodes = parser.parse(name, new String(input.readAllBytes(), StandardCharsets.UTF_8));
        MeshBuilder builder = new MeshBuilder();
        nodes.forEach(node -> walk(node, Placement.IDENTITY, builder));
        return builder.build();
    }

    private void walk(VrmlNode node, Placement placement, MeshBuilder builder) {
        switch (node.type()) {
            case "Transform" -> node.nodes("children").forEach(child -> walk(child, placement.then(local(node)), builder));
            case "Group", "Anchor", "Collision", "Billboard" ->
                    node.nodes("children").forEach(child -> walk(child, placement, builder));
            case "Switch" -> {
                int chosen = (int) node.numbers("whichChoice").filter(values -> values.length > 0).map(values -> values[0])
                        .orElse(-1.0).doubleValue();
                List<VrmlNode> choices = node.nodes("choice");
                if (chosen >= 0 && chosen < choices.size()) {
                    walk(choices.get(chosen), placement, builder);
                }
            }
            case "LOD" -> node.nodes("level").stream().findFirst().ifPresent(finest -> walk(finest, placement, builder));
            case "Shape" -> shape(node, placement, builder);
            default -> {
            }
        }
    }

    private static void shape(VrmlNode shape, Placement placement, MeshBuilder builder) {
        Optional<VrmlNode> geometry = shape.node("geometry").filter(node -> node.type().equals("IndexedFaceSet"));
        if (geometry.isEmpty()) {
            return;
        }
        Optional<VrmlNode> appearance = shape.node("appearance");
        MeshMaterial material = appearance.flatMap(look -> look.node("material"))
                .map(found -> material(found, appearance.get().name())).orElse(MeshMaterial.PLAIN);
        double[] points = geometry.get().node("coord").flatMap(coordinate -> coordinate.numbers("point")).orElse(new double[0]);
        double[] indices = geometry.get().numbers("coordIndex").orElse(new double[0]);
        boolean counterClockwise = !geometry.get().word("ccw").orElse("TRUE").equals("FALSE");
        boolean reversed = counterClockwise == placement.mirrors();
        List<double[]> face = new ArrayList<>();
        for (double written : indices) {
            int index = (int) written;
            if (index < 0) {
                emit(face, reversed, material, builder);
                continue;
            }
            if (index * 3 + 2 < points.length) {
                face.add(placement.apply(points[index * 3], points[index * 3 + 1], points[index * 3 + 2]));
            }
        }
        emit(face, reversed, material, builder);
    }

    private static void emit(List<double[]> face, boolean reversed, MeshMaterial material, MeshBuilder builder) {
        if (face.size() >= 3) {
            List<double[]> corners = new ArrayList<>(face);
            if (reversed) {
                java.util.Collections.reverse(corners);
            }
            builder.polygon(material, corners);
        }
        face.clear();
    }

    /**
     * VRML's Phong material as metallic-roughness: its diffuse colour the base, its shininess the gloss. Named as its DEF
     * names it, or as its appearance's where only that is named (as OpenCascade writes it).
     */
    private static MeshMaterial material(VrmlNode material, String appearanceName) {
        double[] diffuse = material.numbers("diffuseColor").filter(values -> values.length >= 3)
                .orElse(new double[] {DIFFUSE, DIFFUSE, DIFFUSE});
        double transparency = material.numbers("transparency").filter(values -> values.length > 0).map(values -> values[0])
                .orElse(0.0);
        double shininess = material.numbers("shininess").filter(values -> values.length > 0).map(values -> values[0])
                .orElse(SHININESS);
        String name = material.name() != null ? material.name() : appearanceName != null ? appearanceName : "material";
        // VRML writes a colour as it is shown; a mesh holds the light it stands for.
        return new MeshMaterial(name, MeshMaterial.linear(diffuse[0]), MeshMaterial.linear(diffuse[1]), MeshMaterial.linear(diffuse[2]),
                1 - clamped(transparency), 0, 1 - clamped(shininess));
    }

    private static double clamped(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /** A Transform's own placement: T × C × R × SR × S × −SR × −C (ISO/IEC 14772-1, 6.52). */
    private static Placement local(VrmlNode transform) {
        double[] translation = vector(transform, "translation", new double[] {0, 0, 0});
        double[] center = vector(transform, "center", new double[] {0, 0, 0});
        double[] rotation = vector(transform, "rotation", new double[] {0, 0, 1, 0});
        double[] scale = vector(transform, "scale", new double[] {1, 1, 1});
        double[] scaleOrientation = vector(transform, "scaleOrientation", new double[] {0, 0, 1, 0});
        return Placement.translation(translation[0], translation[1], translation[2])
                .then(Placement.translation(center[0], center[1], center[2]))
                .then(Placement.rotation(rotation))
                .then(Placement.rotation(scaleOrientation))
                .then(Placement.scale(scale[0], scale[1], scale[2]))
                .then(Placement.rotation(new double[] {scaleOrientation[0], scaleOrientation[1], scaleOrientation[2],
                                                       -scaleOrientation[3]}))
                .then(Placement.translation(-center[0], -center[1], -center[2]));
    }

    private static double[] vector(VrmlNode node, String field, double[] unwritten) {
        return node.numbers(field).filter(values -> values.length >= unwritten.length).orElse(unwritten);
    }
}
