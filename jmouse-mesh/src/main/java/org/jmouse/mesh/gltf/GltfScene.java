package org.jmouse.mesh.gltf;

import tools.jackson.databind.JsonNode;
import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.mesh.MeshReadException;
import org.jmouse.mesh.Placement;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A glTF 2.0 document as a mesh, whichever container it came in: the nodes of its scene walked from the roots, each
 * placed by its own matrix or translation × rotation × scale after its parents', and every primitive of their meshes
 * drawn as triangles — a list, a strip or a fan — in its material's base colour, metalness and roughness. Points and
 * lines draw no body and are passed over, as are textures, cameras, skins and animations: a body is read as it stands.
 */
final class GltfScene {

    /** The required extensions read here: quantized attributes are integers read as any accessor is. */
    private static final Set<String> UNDERSTOOD_EXTENSIONS = Set.of("KHR_mesh_quantization");

    private static final int TRIANGLES = 4;
    private static final int TRIANGLE_STRIP = 5;
    private static final int TRIANGLE_FAN = 6;

    private static final int BYTE = 5120;
    private static final int UNSIGNED_BYTE = 5121;
    private static final int SHORT = 5122;
    private static final int UNSIGNED_SHORT = 5123;
    private static final int UNSIGNED_INT = 5125;
    private static final int FLOAT = 5126;

    private static final String DATA_URI = "data:";
    private static final String BASE64 = ";base64,";

    private final JsonNode document;
    private final List<ByteBuffer> buffers;
    private final String name;

    /**
     * @param document  the glTF JSON
     * @param carried   the bytes a GLB file carries for its first buffer, which then names no URI
     * @param name      what the file is called, for a refusal to name
     */
    GltfScene(JsonNode document, Optional<ByteBuffer> carried, String name) {
        this.document = document;
        this.name = name;
        refuseUnreadExtensions();
        this.buffers = buffers(carried);
    }

    Mesh read() {
        MeshBuilder builder = new MeshBuilder();
        try {
            for (int root : roots()) {
                walk(root, Placement.IDENTITY, builder, new HashSet<>());
            }
        } catch (IndexOutOfBoundsException | IllegalArgumentException outside) {
            throw new MeshReadException(name + ": " + outside.getMessage(), outside);
        }
        return builder.build();
    }

    private void refuseUnreadExtensions() {
        List<String> unread = new ArrayList<>();
        document.path("extensionsRequired").forEach(extension -> {
            if (!UNDERSTOOD_EXTENSIONS.contains(extension.asString())) {
                unread.add(extension.asString());
            }
        });
        if (!unread.isEmpty()) {
            throw new MeshReadException(name + ": it requires " + String.join(", ", unread) + ", which is not read here");
        }
    }

    /** Every buffer's bytes: a GLB's own, or a base64 data URI. A buffer in a file beside this one is not reachable. */
    private List<ByteBuffer> buffers(Optional<ByteBuffer> carried) {
        List<ByteBuffer> resolved = new ArrayList<>();
        JsonNode declared = document.path("buffers");
        for (int index = 0; index < declared.size(); index++) {
            JsonNode buffer = declared.get(index);
            String uri = buffer.path("uri").asString(null);
            if (uri == null) {
                if (index != 0 || carried.isEmpty()) {
                    throw new MeshReadException(name + ": buffer " + index + " names no URI and no GLB chunk carries it");
                }
                resolved.add(carried.get());
            } else if (uri.startsWith(DATA_URI) && uri.contains(BASE64)) {
                resolved.add(ByteBuffer.wrap(decoded(uri, index)).order(ByteOrder.LITTLE_ENDIAN));
            } else {
                throw new MeshReadException(name + ": buffer " + index + " is the separate file '" + uri
                        + "'; a glTF is read here whole — as GLB, or with its buffers embedded");
            }
        }
        return resolved;
    }

    private byte[] decoded(String uri, int index) {
        try {
            return Base64.getDecoder().decode(uri.substring(uri.indexOf(BASE64) + BASE64.length()));
        } catch (IllegalArgumentException refused) {
            throw new MeshReadException(name + ": buffer " + index + " is not base64: " + refused.getMessage(), refused);
        }
    }

    /** The scene's root nodes: the one it names, else the first; with no scene at all, every node no node holds. */
    private List<Integer> roots() {
        JsonNode scenes = document.path("scenes");
        if (scenes.size() > 0) {
            return integers(node("scenes", document.path("scene").asInt(0)).path("nodes"));
        }
        JsonNode nodes = document.path("nodes");
        Set<Integer> held = new HashSet<>();
        nodes.forEach(node -> held.addAll(integers(node.path("children"))));
        List<Integer> roots = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            if (!held.contains(index)) {
                roots.add(index);
            }
        }
        return roots;
    }

    private void walk(int index, Placement parent, MeshBuilder builder, Set<Integer> above) {
        if (!above.add(index)) {
            throw new MeshReadException(name + ": node " + index + " holds itself");
        }
        JsonNode node = node("nodes", index);
        Placement placement = parent.then(local(node));
        if (node.has("mesh")) {
            for (JsonNode primitive : node("meshes", node.path("mesh").asInt()).path("primitives")) {
                primitive(primitive, placement, builder);
            }
        }
        for (int child : integers(node.path("children"))) {
            walk(child, placement, builder, above);
        }
        above.remove(index);
    }

    /** A node's own placement: its matrix, or T × R × S. */
    private static Placement local(JsonNode node) {
        if (node.has("matrix")) {
            return Placement.columnMajor(numbers(node.path("matrix"), new double[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}));
        }
        double[] translation = numbers(node.path("translation"), new double[] {0, 0, 0});
        double[] rotation = numbers(node.path("rotation"), new double[] {0, 0, 0, 1});
        double[] scale = numbers(node.path("scale"), new double[] {1, 1, 1});
        return Placement.translation(translation[0], translation[1], translation[2])
                .then(Placement.quaternion(rotation))
                .then(Placement.scale(scale[0], scale[1], scale[2]));
    }

    private void primitive(JsonNode primitive, Placement placement, MeshBuilder builder) {
        int mode = primitive.path("mode").asInt(TRIANGLES);
        JsonNode attributes = primitive.path("attributes");
        if (mode < TRIANGLES || mode > TRIANGLE_FAN || !attributes.has("POSITION")) {
            return;
        }
        double[] positions = accessor(attributes.path("POSITION").asInt());
        int corners = positions.length / 3;
        int[] order = primitive.has("indices") ? indices(accessor(primitive.path("indices").asInt())) : sequence(corners);
        MeshMaterial material = primitive.has("material") ? material(primitive.path("material").asInt()) : MeshMaterial.PLAIN;
        boolean mirrored = placement.mirrors();
        for (int[] triangle : triangles(mode, order)) {
            double[] first = corner(positions, triangle[0], placement);
            double[] second = corner(positions, triangle[1], placement);
            double[] third = corner(positions, triangle[2], placement);
            if (mirrored) {
                builder.triangle(material, first, third, second);
            } else {
                builder.triangle(material, first, second, third);
            }
        }
    }

    /** A primitive's triangles by its corners' indices (glTF 2.0, 3.7.2.1). */
    private static List<int[]> triangles(int mode, int[] order) {
        List<int[]> triangles = new ArrayList<>();
        switch (mode) {
            case TRIANGLES -> {
                for (int at = 0; at + 2 < order.length; at += 3) {
                    triangles.add(new int[] {order[at], order[at + 1], order[at + 2]});
                }
            }
            case TRIANGLE_STRIP -> {
                for (int at = 0; at + 2 < order.length; at++) {
                    int odd = at % 2;
                    triangles.add(new int[] {order[at], order[at + 1 + odd], order[at + 2 - odd]});
                }
            }
            case TRIANGLE_FAN -> {
                for (int at = 0; at + 2 < order.length; at++) {
                    triangles.add(new int[] {order[at + 1], order[at + 2], order[0]});
                }
            }
            default -> {
            }
        }
        return triangles;
    }

    private double[] corner(double[] positions, int index, Placement placement) {
        if (index < 0 || index * 3 + 2 >= positions.length) {
            throw new MeshReadException(name + ": an index points at corner " + index + " of " + positions.length / 3);
        }
        return placement.apply(positions[index * 3], positions[index * 3 + 1], positions[index * 3 + 2]);
    }

    /** glTF's metallic-roughness material, as it is written; its textures are not read. */
    private MeshMaterial material(int index) {
        JsonNode material = node("materials", index);
        JsonNode pbr = material.path("pbrMetallicRoughness");
        double[] base = numbers(pbr.path("baseColorFactor"), new double[] {1, 1, 1, 1});
        double opacity = material.path("alphaMode").asString("OPAQUE").equals("BLEND") ? base[3] : 1;
        return new MeshMaterial(material.path("name").asString("material " + index), base[0], base[1], base[2], opacity,
                pbr.path("metallicFactor").asDouble(1), pbr.path("roughnessFactor").asDouble(1));
    }

    /** An accessor's values, component after component, element after element (glTF 2.0, 3.6.2). */
    private double[] accessor(int index) {
        JsonNode accessor = node("accessors", index);
        int count = accessor.path("count").asInt();
        int components = components(accessor.path("type").asString());
        int componentType = accessor.path("componentType").asInt();
        boolean normalized = accessor.path("normalized").asBoolean(false);
        double[] values = new double[count * components];
        if (accessor.has("bufferView")) {
            read(accessor.path("bufferView").asInt(), accessor.path("byteOffset").asInt(0), componentType, components,
                    normalized, values, count);
        }
        JsonNode sparse = accessor.path("sparse");
        if (sparse.has("count")) {
            int replaced = sparse.path("count").asInt();
            JsonNode sparseIndices = sparse.path("indices");
            double[] at = new double[replaced];
            read(sparseIndices.path("bufferView").asInt(), sparseIndices.path("byteOffset").asInt(0),
                    sparseIndices.path("componentType").asInt(), 1, false, at, replaced);
            JsonNode sparseValues = sparse.path("values");
            double[] written = new double[replaced * components];
            read(sparseValues.path("bufferView").asInt(), sparseValues.path("byteOffset").asInt(0), componentType, components,
                    normalized, written, replaced);
            for (int element = 0; element < replaced; element++) {
                System.arraycopy(written, element * components, values, (int) at[element] * components, components);
            }
        }
        return values;
    }

    private void read(int viewIndex, int accessorOffset, int componentType, int components, boolean normalized,
                      double[] into, int count) {
        JsonNode view = node("bufferViews", viewIndex);
        ByteBuffer buffer = buffer(view.path("buffer").asInt());
        int size = size(componentType);
        int stride = view.path("byteStride").asInt(size * components);
        int start = view.path("byteOffset").asInt(0) + accessorOffset;
        for (int element = 0; element < count; element++) {
            for (int component = 0; component < components; component++) {
                into[element * components + component] =
                        component(buffer, start + element * stride + component * size, componentType, normalized);
            }
        }
    }

    /** One component; a normalized integer as glTF maps it into −1 … 1 or 0 … 1. */
    private static double component(ByteBuffer buffer, int at, int componentType, boolean normalized) {
        return switch (componentType) {
            case FLOAT -> buffer.getFloat(at);
            case BYTE -> normalized ? Math.max(buffer.get(at) / 127.0, -1) : buffer.get(at);
            case UNSIGNED_BYTE -> normalized ? Byte.toUnsignedInt(buffer.get(at)) / 255.0 : Byte.toUnsignedInt(buffer.get(at));
            case SHORT -> normalized ? Math.max(buffer.getShort(at) / 32767.0, -1) : buffer.getShort(at);
            case UNSIGNED_SHORT ->
                    normalized ? Short.toUnsignedInt(buffer.getShort(at)) / 65535.0 : Short.toUnsignedInt(buffer.getShort(at));
            case UNSIGNED_INT -> Integer.toUnsignedLong(buffer.getInt(at));
            default -> throw new IllegalArgumentException("component type " + componentType + " is not glTF's");
        };
    }

    private static int size(int componentType) {
        return switch (componentType) {
            case BYTE, UNSIGNED_BYTE -> 1;
            case SHORT, UNSIGNED_SHORT -> 2;
            case UNSIGNED_INT, FLOAT -> 4;
            default -> throw new IllegalArgumentException("component type " + componentType + " is not glTF's");
        };
    }

    private static int components(String type) {
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4", "MAT2" -> 4;
            case "MAT3" -> 9;
            case "MAT4" -> 16;
            default -> throw new IllegalArgumentException("accessor type '" + type + "' is not glTF's");
        };
    }

    private ByteBuffer buffer(int index) {
        if (index < 0 || index >= buffers.size()) {
            throw new MeshReadException(name + ": a buffer view names buffer " + index + " of " + buffers.size());
        }
        return buffers.get(index);
    }

    private JsonNode node(String array, int index) {
        JsonNode found = document.path(array).path(index);
        if (found.isMissingNode()) {
            throw new MeshReadException(name + ": " + array + " " + index + " is named but not written");
        }
        return found;
    }

    private static int[] indices(double[] values) {
        int[] indices = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            indices[index] = (int) values[index];
        }
        return indices;
    }

    private static int[] sequence(int count) {
        int[] sequence = new int[count];
        for (int index = 0; index < count; index++) {
            sequence[index] = index;
        }
        return sequence;
    }

    private static List<Integer> integers(JsonNode array) {
        List<Integer> integers = new ArrayList<>();
        array.forEach(value -> integers.add(value.asInt()));
        return integers;
    }

    /** An array of numbers, or {@code unwritten} when it is absent or shorter. */
    private static double[] numbers(JsonNode array, double[] unwritten) {
        if (!array.isArray() || array.size() < unwritten.length) {
            return unwritten;
        }
        double[] numbers = new double[unwritten.length];
        for (int index = 0; index < numbers.length; index++) {
            numbers[index] = array.get(index).asDouble();
        }
        return numbers;
    }
}
