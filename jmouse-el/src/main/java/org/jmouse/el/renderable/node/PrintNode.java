package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class PrintNode extends AbstractNode {

    private final Expression expression;

    public PrintNode(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
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
        } else {
            visitor.visit(this);
        }
    }

}
