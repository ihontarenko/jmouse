package org.jmouse.json.node;

import org.jmouse.el.node.Node;

/**
 * 🧾 A JSON value in the engine's tree: what it writes, as Java holds it.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface JsonNode extends Node {

    /** The value: a map, a list, a number, a string, a truth value or {@code null}. */
    Object value();
}
