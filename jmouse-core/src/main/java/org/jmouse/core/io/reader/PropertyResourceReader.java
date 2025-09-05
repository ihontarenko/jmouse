package org.jmouse.core.io.reader;

import org.jmouse.core.io.Resource;

import java.io.IOException;
import java.util.Map;

/**
 * 🧩 Converts a Resource into a flat key-value map.
 */
public interface PropertyResourceReader {
    /** Whether this reader can parse the given resource (by extension, content-type, etc.). */
    boolean supports(Resource resource);

    /** Parse resource into a (possibly nested) map; caller may flatten. */
    Map<String, Object> read(Resource resource) throws IOException;
}
