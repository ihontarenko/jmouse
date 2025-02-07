package org.jmouse.expression.ast;

import org.jmouse.common.ast.node.EntryNode;
import org.jmouse.common.ast.node.Node;

public class ParameterNode extends EntryNode {

    private Node key;
    private Node value;

    public Node getKey() {
        return key;
    }

    public void setKey(Node key) {
        this.key = key;
    }

    public Node getValue() {
        return value;
    }

    public void setValue(Node value) {
        this.value = value;
    }

}
