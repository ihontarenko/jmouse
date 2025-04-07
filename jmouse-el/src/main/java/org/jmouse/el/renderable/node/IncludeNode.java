package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;

public class IncludeNode extends AbstractNode {

    private final ExpressionNode path;

    public IncludeNode(ExpressionNode path) {
        this.path = path;
    }

    public ExpressionNode getPath() {
        return path;
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
