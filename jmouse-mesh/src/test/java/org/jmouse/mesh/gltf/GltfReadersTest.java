package org.jmouse.mesh.gltf;

import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.mesh.MeshPart;
import org.jmouse.mesh.MeshReadException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GltfReadersTest {

    private static final float CLOSE = 1e-5f;

    /** One triangle in the XY plane, counter-clockwise seen from +Z. */
    private static final float[] TRIANGLE = {0, 0, 0, 1, 0, 0, 0, 1, 0};

    @Test
    void readsBackWhatTheWriterWrote() throws IOException {
        MeshMaterial copper = new MeshMaterial("copper", 0.9, 0.6, 0.3, 1, 1, 0.3);
        MeshMaterial glass = new MeshMaterial("glass", 0.8, 0.9, 1, 0.4, 0, 0.1);
        Mesh written = new MeshBuilder()
                .triangle(copper, new double[] {0, 0, 0}, new double[] {2, 0, 0}, new double[] {0, 3, 0})
                .triangle(copper, new double[] {2, 0, 0}, new double[] {2, 3, 0}, new double[] {0, 3, 0})
                .triangle(glass, new double[] {0, 0, 1}, new double[] {1, 0, 1}, new double[] {0, 1, 1})
                .build();

        Mesh read = new GlbReader().read(new ByteArrayInputStream(new GlbWriter().write(written)), "written.glb");

        assertEquals(written.triangles(), read.triangles());
        assertEquals(2, read.parts().size());
        for (int index = 0; index < written.parts().size(); index++) {
            MeshPart expected = written.parts().get(index);
            MeshPart actual = read.parts().get(index);
            assertArrayEquals(expected.positions(), actual.positions(), CLOSE);
            assertArrayEquals(expected.normals(), actual.normals(), CLOSE);
            assertEquals(expected.material().name(), actual.material().name());
            assertEquals(expected.material().red(), actual.material().red(), CLOSE);
            assertEquals(expected.material().roughness(), actual.material().roughness(), CLOSE);
        }
        assertEquals(0.4, read.parts().get(1).material().opacity(), CLOSE);
    }

    @Test
    void placesANodeAndKeepsAMirroredFaceFacingOut() throws IOException {
        String gltf = document(floats(TRIANGLE), """
                "nodes": [{"mesh": 0, "translation": [10, 20, 30], "scale": [-1, 1, 1]}],
                "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
                "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"}],
                "bufferViews": [{"buffer": 0, "byteLength": 36}],
                """);

        MeshPart part = new GltfReader().read(input(gltf), "mirrored.gltf").parts().getFirst();

        float[] bounds = part.bounds();
        assertArrayEquals(new float[] {9, 20, 30, 10, 21, 30}, bounds, CLOSE);
        assertEquals(1, part.normals()[2], CLOSE, "the mirrored face is turned back to face +Z");
        assertEquals(MeshMaterial.PLAIN, part.material());
    }

    @Test
    void composesAParentsMatrixWithItsChildsRotation() throws IOException {
        double half = Math.sqrt(0.5);
        String gltf = document(floats(TRIANGLE), """
                "nodes": [
                  {"children": [1], "matrix": [2,0,0,0, 0,2,0,0, 0,0,2,0, 5,0,0,1]},
                  {"mesh": 0, "rotation": [0, 0, %s, %s]}
                ],
                "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
                "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"}],
                "bufferViews": [{"buffer": 0, "byteLength": 36}],
                """.formatted(half, half));

        float[] positions = new GltfReader().read(input(gltf), "nested.gltf").parts().getFirst().positions();

        // A quarter turn about Z puts (1, 0, 0) at (0, 1, 0); doubled and moved 5 along X it is (5, 2, 0).
        assertArrayEquals(new float[] {5, 0, 0, 5, 2, 0, 3, 0, 0}, positions, CLOSE);
    }

    @Test
    void readsAStripOfInterleavedCornersByByteIndices() throws IOException {
        // Four corners, each a position followed by a normal the reader does not need: a stride of 24 bytes.
        float[] interleaved = {0, 0, 0, 9, 9, 9, 1, 0, 0, 9, 9, 9, 0, 1, 0, 9, 9, 9, 1, 1, 0, 9, 9, 9};
        byte[] corners = floats(interleaved);
        byte[] bytes = new byte[corners.length + 4];
        System.arraycopy(corners, 0, bytes, 0, corners.length);
        bytes[corners.length] = 0;
        bytes[corners.length + 1] = 1;
        bytes[corners.length + 2] = 2;
        bytes[corners.length + 3] = 3;
        String gltf = document(bytes, """
                "nodes": [{"mesh": 0}],
                "meshes": [{"primitives": [{"attributes": {"POSITION": 0}, "indices": 1, "mode": 5, "material": 0}]}],
                "materials": [{"name": "solder mask", "pbrMetallicRoughness": {"baseColorFactor": [0, 0.4, 0, 0.5]}}],
                "accessors": [
                  {"bufferView": 0, "componentType": 5126, "count": 4, "type": "VEC3"},
                  {"bufferView": 1, "componentType": 5121, "count": 4, "type": "SCALAR"}
                ],
                "bufferViews": [
                  {"buffer": 0, "byteLength": 96, "byteStride": 24},
                  {"buffer": 0, "byteOffset": 96, "byteLength": 4}
                ],
                """);

        MeshPart part = new GltfReader().read(input(gltf), "strip.gltf").parts().getFirst();

        assertEquals(6, part.corners(), "a strip of four corners is two triangles");
        for (int corner = 0; corner < part.corners(); corner++) {
            assertEquals(1, part.normals()[corner * 3 + 2], CLOSE, "both triangles of the strip face +Z");
        }
        assertEquals("solder mask", part.material().name());
        assertEquals(1, part.material().opacity(), CLOSE, "an opaque material ignores its alpha");
        assertEquals(1, part.material().metallic(), CLOSE, "glTF's metallic factor is 1 unwritten");
    }

    @Test
    void replacesTheCornersASparseAccessorNames() throws IOException {
        byte[] base = floats(TRIANGLE);
        byte[] replacement = floats(new float[] {0, 0, 5});
        ByteBuffer bytes = ByteBuffer.allocate(base.length + 4 + replacement.length).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(base).putInt(0).put(replacement);
        String gltf = document(bytes.array(), """
                "nodes": [{"mesh": 0}],
                "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
                "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3",
                  "sparse": {"count": 1, "indices": {"bufferView": 1, "componentType": 5125}, "values": {"bufferView": 2}}}],
                "bufferViews": [
                  {"buffer": 0, "byteLength": 36},
                  {"buffer": 0, "byteOffset": 36, "byteLength": 4},
                  {"buffer": 0, "byteOffset": 40, "byteLength": 12}
                ],
                """);

        float[] positions = new GltfReader().read(input(gltf), "sparse.gltf").parts().getFirst().positions();

        assertArrayEquals(new float[] {0, 0, 5, 1, 0, 0, 0, 1, 0}, positions, CLOSE);
    }

    @Test
    void refusesAnExtensionItCannotRead() {
        String gltf = """
                {"asset": {"version": "2.0"}, "extensionsRequired": ["KHR_draco_mesh_compression"]}
                """;

        MeshReadException refused = assertThrows(MeshReadException.class,
                () -> new GltfReader().read(input(gltf), "compressed.gltf"));

        assertTrue(refused.getMessage().contains("KHR_draco_mesh_compression"), refused.getMessage());
    }

    @Test
    void refusesABufferInAnotherFile() {
        String gltf = """
                {"asset": {"version": "2.0"}, "buffers": [{"uri": "body.bin", "byteLength": 36}]}
                """;

        MeshReadException refused = assertThrows(MeshReadException.class,
                () -> new GltfReader().read(input(gltf), "separate.gltf"));

        assertTrue(refused.getMessage().contains("body.bin"), refused.getMessage());
    }

    @Test
    void refusesBytesThatAreNotBinaryGltf() {
        byte[] text = "solid not a glb".getBytes(StandardCharsets.US_ASCII);

        assertThrows(MeshReadException.class, () -> new GlbReader().read(new ByteArrayInputStream(text), "text.glb"));
    }

    @Test
    void readsEveryRootWhenNoSceneIsWritten() throws IOException {
        String gltf = document(floats(TRIANGLE), """
                "nodes": [{"mesh": 0}, {"mesh": 0, "translation": [0, 0, 4]}],
                "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
                "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"}],
                "bufferViews": [{"buffer": 0, "byteLength": 36}],
                """).replace("\"scene\": 0, \"scenes\": [{\"nodes\": [0]}],", "");

        assertEquals(2, new GltfReader().read(input(gltf), "sceneless.gltf").triangles());
    }

    /** A glTF document of one scene holding node 0, its one buffer {@code bytes} embedded, and {@code body} between. */
    private static String document(byte[] bytes, String body) {
        return "{\"asset\": {\"version\": \"2.0\"}, \"scene\": 0, \"scenes\": [{\"nodes\": [0]}]," + body
                + "\"buffers\": [{\"byteLength\": " + bytes.length + ", \"uri\": \"data:application/octet-stream;base64,"
                + Base64.getEncoder().encodeToString(bytes) + "\"}]}";
    }

    private static byte[] floats(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asFloatBuffer().put(values);
        return buffer.array();
    }

    private static ByteArrayInputStream input(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
