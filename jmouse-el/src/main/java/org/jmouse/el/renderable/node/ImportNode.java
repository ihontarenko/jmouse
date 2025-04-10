package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class ImportNode extends AbstractNode {

    private ExpressionNode scope;
    private ExpressionNode path;

    public ExpressionNode getScope() {
        return scope;
    }

    public void setScope(ExpressionNode scope) {
        this.scope = scope;
    }

    public ExpressionNode getPath() {
        return path;
    }

    public void setPath(ExpressionNode path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "IMPORT: " + getPath() + (getScope() != null ? " AS " + getScope() : "");
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

}
