package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.NameSetNode;
import org.jmouse.el.renderable.NodeVisitor;

public class FromNode extends AbstractNode {

    private ExpressionNode source;
    private NameSetNode    names;

    public ExpressionNode getSource() {
        return source;
    }

    public void setSource(ExpressionNode source) {
        this.source = source;
    }

    public NameSetNode getNameSet() {
        return names;
    }

    public void setNames(NameSetNode names) {
        this.names = names;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        } else {
            visitor.visit(this);
        }
    }

}
