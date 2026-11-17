package org.jmouse.json.node;

import org.jmouse.el.node.AbstractNode;

/**
 * 🧾 A member of a JSON object: its name, and its value — the one child.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonMemberNode extends AbstractNode {

    private final String name;

    public JsonMemberNode(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** Its value's node. */
    public JsonNode valueNode() {
        return (JsonNode) getFirst();
    }
}
