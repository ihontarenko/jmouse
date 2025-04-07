package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;

public class IncludeNode extends AbstractNode {

    private final ExpressionNode path;

    public IncludeNode(ExpressionNode path) {
        this.path = path;
    }

    public ExpressionNode getPath() {
        return path;
    }

}
