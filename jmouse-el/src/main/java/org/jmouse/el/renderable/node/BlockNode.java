package org.jmouse.el.renderable.node;

import org.jmouse.el.core.node.AbstractNode;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.node.Node;

public class BlockNode extends AbstractNode {

    private ExpressionNode name;
    private Node           body;

    public ExpressionNode getName() {
        return name;
    }

    public void setName(ExpressionNode name) {
        this.name = name;
    }

    public Node getBody() {
        return body;
    }

    public void setBody(Node body) {
        this.body = body;
    }

}
