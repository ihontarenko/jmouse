package org.jmouse.parallax.file;

import org.jmouse.parallax.Dataset;

import java.io.IOException;
import java.io.Writer;

/**
 * A file format a dataset can be written in: a mesh as OBJ, STL or PLY, a table as CSV. A format says which
 * datasets it can carry; a composite dataset is written through the first part the format accepts.
 */
public interface DatasetFormat {

    /** The name an API asks for: "obj", "stl", "ply", "csv". */
    String name();

    String extension();

    String mediaType();

    boolean accepts(Dataset dataset);

    void write(Dataset dataset, Writer target) throws IOException;
}
