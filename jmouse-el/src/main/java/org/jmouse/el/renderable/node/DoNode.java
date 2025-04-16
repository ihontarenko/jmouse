package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class DoNode extends AbstractNode {

    ExpressionNode expression;

    /**
     * Constructs an {@code AbstractNode} with no parent.
     */
    public DoNode(ExpressionNode expression) {
        this.expression = expression;
    }

    /**
     * Constructs an {@code AbstractNode} with the specified parent node.
     *
     * @param parent the parent node, or {@code null} if this is a root node
     */
    public DoNode(Node parent, ExpressionNode expression) {
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    public void setExpression(ExpressionNode expression) {
        this.expression = expression;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    @Override
    public String toString() {
        return "DO: " + getExpression();
    }

}
