package org.jmouse.mesh.gltf;

import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.mesh.MeshPart;
import org.jmouse.mesh.MeshScene;
import org.jmouse.mesh.Placement;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A mesh, or a scene of them, as binary glTF 2.0 (GLB): one JSON chunk saying what is where, one binary chunk holding
 * it — positions, normals and triangle indices of every part, each part a primitive in its material, each body a glTF
 * mesh written once however many nodes stand it somewhere. It is what a viewer draws with no knowledge of the format the
 * body came from; the numbers are written as they are, so a mesh {@link Mesh#placed placed} in millimetres with Z up is
 * served so, and a scene whose root is the format's frame is a file any glTF reader places.
 */
public final class GlbWriter {

    private static final int FLOAT = 5126;
    private static final int UNSIGNED_INT = 5125;
    private static final int ARRAY_BUFFER = 34962;
    private static final int ELEMENT_ARRAY_BUFFER = 34963;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** What the file is written as, as it is gathered. */
    private static final class Written {

        private final ByteArrayOutputStream binary = new ByteArrayOutputStream();
        private final List<String> views = new ArrayList<>();
        private final List<String> accessors = new ArrayList<>();
        private final Map<MeshMaterial, Integer> materials = new LinkedHashMap<>();
    }

    public byte[] write(Mesh mesh) {
        return write(MeshScene.of(mesh));
    }

    public byte[] write(MeshScene scene) {
        Written written = new Written();
        List<String> meshes = new ArrayList<>();
        // A body with no triangles is no glTF mesh: the nodes that name it are left out.
        int[] meshIndexes = new int[scene.meshes().size()];
        for (int index = 0; index < scene.meshes().size(); index++) {
            List<String> primitives = primitivesOf(scene.meshes().get(index), written);
            meshIndexes[index] = primitives.isEmpty() ? -1 : meshes.size();
            if (!primitives.isEmpty()) {
                meshes.add("{\"primitives\":[" + String.join(",", primitives) + "]}");
            }
        }
        List<String> nodes = new ArrayList<>();
        List<String> children = new ArrayList<>();
        nodes.add(null);
        for (MeshScene.Node node : scene.nodes()) {
            if (meshIndexes[node.mesh()] < 0) {
                continue;
            }
            children.add(String.valueOf(nodes.size()));
            nodes.add("{" + named(node.name()) + "\"mesh\":" + meshIndexes[node.mesh()] + matrix(node.placement()) + "}");
        }
        nodes.set(0, "{" + named(scene.name()) + "\"children\":[" + String.join(",", children) + "]" + matrix(scene.root()) + extras(scene) + "}");
        String json = "{\"asset\":{\"version\":\"2.0\",\"generator\":\"jmouse-mesh\"},\"scene\":0,\"scenes\":[{\"nodes\":[0]}],"
                + "\"nodes\":[" + String.join(",", nodes) + "],"
                + "\"meshes\":[" + String.join(",", meshes) + "],"
                + "\"materials\":[" + written.materials.keySet().stream().map(GlbWriter::material).collect(Collectors.joining(",")) + "],"
                + "\"accessors\":[" + String.join(",", written.accessors) + "],"
                + "\"bufferViews\":[" + String.join(",", written.views) + "],"
                + "\"buffers\":[{\"byteLength\":" + written.binary.size() + "}]}";
        return GlbContainer.write(json.getBytes(StandardCharsets.UTF_8), written.binary.toByteArray());
    }

    /** A body's parts, each a primitive in its material; a part with no triangles is none. */
    private static List<String> primitivesOf(Mesh mesh, Written written) {
        List<String> primitives = new ArrayList<>();
        for (MeshPart part : mesh.parts()) {
            if (part.triangles().length == 0) {
                continue;
            }
            float[] bounds = part.bounds();
            int position = written.accessors.size();
            written.accessors.add(accessor(view(written, floats(part.positions()), ARRAY_BUFFER), FLOAT, part.corners(), "VEC3",
                    ",\"min\":[%s,%s,%s],\"max\":[%s,%s,%s]".formatted(number(bounds[0]), number(bounds[1]),
                            number(bounds[2]), number(bounds[3]), number(bounds[4]), number(bounds[5]))));
            int normal = written.accessors.size();
            written.accessors.add(accessor(view(written, floats(part.normals()), ARRAY_BUFFER), FLOAT, part.corners(), "VEC3", ""));
            int indices = written.accessors.size();
            written.accessors.add(accessor(view(written, integers(part.triangles()), ELEMENT_ARRAY_BUFFER), UNSIGNED_INT,
                    part.triangles().length, "SCALAR", ""));
            int material = written.materials.computeIfAbsent(part.material(), ignored -> written.materials.size());
            primitives.add("{\"attributes\":{\"POSITION\":%d,\"NORMAL\":%d},\"indices\":%d,\"material\":%d}"
                    .formatted(position, normal, indices, material));
        }
        return primitives;
    }

    private static String view(Written written, byte[] bytes, int target) {
        int offset = written.binary.size();
        written.binary.writeBytes(bytes);
        written.views.add("{\"buffer\":0,\"byteOffset\":%d,\"byteLength\":%d,\"target\":%d}".formatted(offset, bytes.length, target));
        return String.valueOf(written.views.size() - 1);
    }

    private static String accessor(String view, int componentType, int count, String type, String bounds) {
        return "{\"bufferView\":%s,\"componentType\":%d,\"count\":%d,\"type\":\"%s\"%s}"
                .formatted(view, componentType, count, type, bounds);
    }

    private static String material(MeshMaterial material) {
        String blending = material.transparent() ? ",\"alphaMode\":\"BLEND\"" : "";
        return "{\"name\":%s,\"pbrMetallicRoughness\":{\"baseColorFactor\":[%s,%s,%s,%s],\"metallicFactor\":%s,\"roughnessFactor\":%s}%s,\"doubleSided\":false}"
                .formatted(text(material.name()), number(material.red()), number(material.green()), number(material.blue()),
                        number(material.opacity()), number(material.metallic()), number(material.roughness()), blending);
    }

    /** A node's name as its first member, or nothing for a node with none. */
    private static String named(String name) {
        return name == null ? "" : "\"name\":" + text(name) + ",";
    }

    /** What the scene says of itself, as the root's {@code extras}: a glTF reader hands it on as the root's own data. */
    private static String extras(MeshScene scene) {
        return scene.extras() == null ? "" : ",\"extras\":" + JSON.writeValueAsString(scene.extras());
    }

    /** A node's placement as its member, or nothing where it stands as its parent does. */
    private static String matrix(Placement placement) {
        if (placement.identity()) {
            return "";
        }
        return ",\"matrix\":[" + Arrays.stream(placement.columns()).mapToObj(GlbWriter::number).collect(Collectors.joining(",")) + "]";
    }

    /** A JSON string: its quotes, backslashes and control characters escaped. */
    private static String text(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                default -> {
                    if (character < 0x20) {
                        escaped.append("\\u%04x".formatted((int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.9g", value);
    }

    private static byte[] floats(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asFloatBuffer().put(values);
        return buffer.array();
    }

    private static byte[] integers(int[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asIntBuffer().put(values);
        return buffer.array();
    }
}
