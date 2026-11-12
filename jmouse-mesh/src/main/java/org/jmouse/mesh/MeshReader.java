package org.jmouse.mesh;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads one format into a mesh, in the file's own units and axes — making it millimetres and Z up is
 * {@link Mesh#placed}'s, which knows what the format and the source say of them.
 */
public interface MeshReader {

    /** The format it reads, by its id in {@link MeshFormats}. */
    String format();

    /** Whether it can read now; one that stands on a program of the server cannot while no program is set. */
    default boolean available() {
        return true;
    }

    /**
     * @param name what the file is called, for a refusal to name
     * @throws MeshReadException when the bytes are not the format
     */
    Mesh read(InputStream input, String name) throws IOException;
}
