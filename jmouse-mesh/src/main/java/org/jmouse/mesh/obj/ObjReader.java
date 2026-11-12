package org.jmouse.mesh.obj;

import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.mesh.MeshReadException;
import org.jmouse.mesh.MeshReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Wavefront OBJ: its vertices ({@code v x y z}) and faces ({@code f 1 2 3}, {@code f 1/1/1 2/2/2 3/3/3}, an index below
 * zero counted back from the last vertex), a face of more corners cut into a fan. Each {@code usemtl} starts a part of
 * its own, named as the file names it; its colour is in a separate {@code .mtl} file a single file does not bring, so
 * the part is drawn plain.
 */
public final class ObjReader implements MeshReader {

    @Override
    public String format() {
        return "obj";
    }

    @Override
    public Mesh read(InputStream input, String name) throws IOException {
        MeshBuilder builder = new MeshBuilder();
        List<double[]> vertices = new ArrayList<>();
        MeshMaterial material = MeshMaterial.PLAIN;
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        int lineNumber = 0;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            lineNumber++;
            String[] words = line.trim().split("\s+");
            try {
                switch (words[0]) {
                    case "v" -> vertices.add(new double[] {Double.parseDouble(words[1]), Double.parseDouble(words[2]),
                                                           Double.parseDouble(words[3])});
                    case "f" -> builder.polygon(material, face(words, vertices));
                    case "usemtl" -> material = new MeshMaterial(words.length > 1 ? words[1] : "unnamed", MeshMaterial.PLAIN.red(),
                            MeshMaterial.PLAIN.green(), MeshMaterial.PLAIN.blue(), 1, MeshMaterial.PLAIN.metallic(),
                            MeshMaterial.PLAIN.roughness());
                    default -> {
                    }
                }
            } catch (RuntimeException refused) {
                throw new MeshReadException(name + ", line " + lineNumber + ": '" + line.trim() + "' does not read", refused);
            }
        }
        return builder.build();
    }

    private static List<double[]> face(String[] words, List<double[]> vertices) {
        List<double[]> corners = new ArrayList<>(words.length - 1);
        for (int index = 1; index < words.length; index++) {
            int slash = words[index].indexOf('/');
            int written = Integer.parseInt(slash < 0 ? words[index] : words[index].substring(0, slash));
            corners.add(vertices.get(written < 0 ? vertices.size() + written : written - 1));
        }
        return corners;
    }
}
