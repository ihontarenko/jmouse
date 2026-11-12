package org.jmouse.cad.document.source;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * The files a document holds besides its text — a 3D model's {@code .wrl} and {@code .step}, a datasheet —, each by the
 * name the document gives it, wherever the document is held: blobs of the database, files beside it in a folder,
 * resources of a jar. Whoever reads them never knows which.
 */
@FunctionalInterface
public interface DocumentResources {

    /**
     * @throws FileNotFoundException when the document holds no file of that name
     */
    InputStream open(String name) throws IOException;

    /** The names of the files it holds, where the place can tell them; empty where it cannot. */
    default List<String> names() {
        return List.of();
    }

    /** A document that holds no files. */
    static DocumentResources none() {
        return name -> {
            throw new FileNotFoundException("The document holds no file " + name);
        };
    }
}
