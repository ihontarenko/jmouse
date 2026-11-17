package org.jmouse.json.node;

import org.jmouse.el.node.AbstractNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🧾 A JSON object: its members, in the order written. A name written twice holds its last value, as JavaScript reads it.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonObjectNode extends AbstractNode implements JsonNode {

    /** Its members, in order. */
    public List<JsonMemberNode> members() {
        return getChildren(JsonMemberNode.class);
    }

    @Override
    public Map<String, Object> value() {
        Map<String, Object> object = new LinkedHashMap<>();
        for (JsonMemberNode member : members()) {
            object.put(member.name(), member.valueNode().value());
        }
        return object;
    }
}
