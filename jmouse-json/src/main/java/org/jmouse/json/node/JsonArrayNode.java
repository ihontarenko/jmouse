package org.jmouse.json.node;

import org.jmouse.el.node.AbstractNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧾 A JSON array: its elements, in order.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonArrayNode extends AbstractNode implements JsonNode {

    /** Its elements, in order. */
    public List<JsonNode> elements() {
        return getChildren(JsonNode.class);
    }

    @Override
    public List<Object> value() {
        List<Object> array = new ArrayList<>();
        for (JsonNode element : elements()) {
            array.add(element.value());
        }
        return array;
    }
}
