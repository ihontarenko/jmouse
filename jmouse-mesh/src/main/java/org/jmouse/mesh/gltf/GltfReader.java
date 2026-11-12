package org.jmouse.mesh.gltf;

import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * glTF 2.0 as JSON: a body read whole from one file, so its buffers are embedded as base64 data URIs — a glTF whose
 * buffers are separate files is refused, saying which, and is read once packed as GLB.
 */
public final class GltfReader implements MeshReader {

    @Override
    public String format() {
        return "gltf";
    }

    @Override
    public Mesh read(InputStream input, String name) throws IOException {
        return new GltfScene(GltfJson.parse(input.readAllBytes(), name), Optional.empty(), name).read();
    }
}
