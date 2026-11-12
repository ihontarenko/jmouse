package org.jmouse.mesh.gltf;

import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshReader;

import java.io.IOException;
import java.io.InputStream;

/** Binary glTF 2.0 (GLB) as a mesh: its JSON chunk read as any glTF is, its first buffer the binary chunk it carries. */
public final class GlbReader implements MeshReader {

    @Override
    public String format() {
        return "gltf-binary";
    }

    @Override
    public Mesh read(InputStream input, String name) throws IOException {
        GlbContainer.Chunks chunks = GlbContainer.read(input.readAllBytes(), name);
        return new GltfScene(GltfJson.parse(chunks.json(), name), chunks.binary(), name).read();
    }
}
