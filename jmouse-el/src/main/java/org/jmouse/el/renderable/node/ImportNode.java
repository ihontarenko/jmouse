package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class ImportNode extends AbstractNode {

    private ExpressionNode alias;
    private ExpressionNode source;

    public ExpressionNode getAlias() {
        return alias;
    }

    public void setAlias(ExpressionNode alias) {
        this.alias = alias;
    }

    public ExpressionNode getSource() {
        return source;
    }

    public void setSource(ExpressionNode source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "IMPORT: " + getSource() + (getAlias() != null ? " AS " + getAlias() : "");
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
