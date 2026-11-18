package org.jmouse.json.reading;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * 🩹 A JSON tree as {@link StructuralReading} walks it, whatever library holds it: this one's own maps and lists, a Jackson tree.
 *
 * @param <N> what a node of the tree is
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface TreeAccess<N> {

    /** An object's member of a name; empty where the node is no object, or has none so named. */
    Optional<N> member(N node, String name);

    /** An array's element at an index; empty where the node is no array, or is shorter. */
    Optional<N> element(N node, int index);

    /** How many elements an array has; -1 for a node that is no array. */
    int size(N node);

    /** The names of an object's members; empty for a node that is no object. */
    Iterable<String> names(N node);

    /** A node read whole into a type. */
    Object read(N node, Type type);
}
