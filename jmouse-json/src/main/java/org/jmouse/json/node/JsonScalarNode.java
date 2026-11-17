package org.jmouse.json.node;

import org.jmouse.el.node.AbstractNode;

/**
 * 🧾 A JSON value with no parts: a string, a number, {@code true}, {@code false} or {@code null}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonScalarNode extends AbstractNode implements JsonNode {

    private final Object value;

    public JsonScalarNode(Object value) {
        this.value = value;
    }

    @Override
    public Object value() {
        return value;
    }
}
