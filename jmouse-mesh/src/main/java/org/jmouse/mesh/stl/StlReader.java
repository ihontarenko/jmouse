package org.jmouse.mesh.stl;

import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.mesh.MeshReadException;
import org.jmouse.mesh.MeshReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * STL, binary or text: triangles and nothing else — no colour, no unit. A binary file is told by its length, which its
 * header fixes: 84 bytes and 50 for each triangle; a text file is every {@code vertex x y z}, three a facet. The facet
 * normals a file writes are not read: a triangle's own corners say it.
 */
public final class StlReader implements MeshReader {

    private static final int HEADER = 80;
    private static final int COUNTED = HEADER + 4;
    private static final int TRIANGLE = 50;
    private static final String VERTEX = "vertex";

    @Override
    public String format() {
        return "stl";
    }

    @Override
    public Mesh read(InputStream input, String name) throws IOException {
        byte[] bytes = input.readAllBytes();
        return isBinary(bytes) ? binary(bytes) : text(new String(bytes, StandardCharsets.US_ASCII), name);
    }

    private static boolean isBinary(byte[] bytes) {
        if (bytes.length < COUNTED) {
            return false;
        }
        long count = Integer.toUnsignedLong(ByteBuffer.wrap(bytes, HEADER, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        return COUNTED + count * TRIANGLE == bytes.length;
    }

    private static Mesh binary(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int count = buffer.getInt(HEADER);
        MeshBuilder builder = new MeshBuilder();
        for (int triangle = 0; triangle < count; triangle++) {
            int at = COUNTED + triangle * TRIANGLE + 12;
            builder.triangle(MeshMaterial.PLAIN, corner(buffer, at), corner(buffer, at + 12), corner(buffer, at + 24));
        }
        return builder.build();
    }

    private static double[] corner(ByteBuffer buffer, int at) {
        return new double[] {buffer.getFloat(at), buffer.getFloat(at + 4), buffer.getFloat(at + 8)};
    }

    private static Mesh text(String text, String name) {
        MeshBuilder builder = new MeshBuilder();
        List<double[]> facet = new ArrayList<>(3);
        String[] words = text.trim().split("\s+");
        for (int index = 0; index < words.length; index++) {
            if (!words[index].equalsIgnoreCase(VERTEX)) {
                continue;
            }
            if (index + 3 >= words.length) {
                throw new MeshReadException(name + ": a vertex ends the file before its z");
            }
            try {
                facet.add(new double[] {Double.parseDouble(words[index + 1]), Double.parseDouble(words[index + 2]),
                                        Double.parseDouble(words[index + 3])});
            } catch (NumberFormatException refused) {
                throw new MeshReadException(name + ": a vertex is not three numbers: " + refused.getMessage(), refused);
            }
            if (facet.size() == 3) {
                builder.triangle(MeshMaterial.PLAIN, facet.get(0), facet.get(1), facet.get(2));
                facet.clear();
            }
        }
        return builder.build();
    }
}
