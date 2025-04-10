package org.jmouse.el.node.expression;

import java.util.List;

public class NameSetNode extends ArrayNode {

    public List<NameNode> getSet() {
        return getChildren(NameNode.class);
    }

    @Override
    public String toString() {
        return "NAMES: " + getSet();
    }

}
