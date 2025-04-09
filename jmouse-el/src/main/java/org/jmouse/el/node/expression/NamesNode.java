package org.jmouse.el.node.expression;

import org.jmouse.el.node.Node;

import java.util.ArrayList;
import java.util.List;

public class NamesNode extends ArrayNode {

    public List<NameNode> getNames() {
        List<NameNode> names = new ArrayList<>();

        for (Node child : getChildren()) {
            if (child instanceof NameNode nameNode) {
                names.add(nameNode);
            }
        }

        return names;
    }

    @Override
    public String toString() {
        return "NAMES: " + getChildren();
    }

}
