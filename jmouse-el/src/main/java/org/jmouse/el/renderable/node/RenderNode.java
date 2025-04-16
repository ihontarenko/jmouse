package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class RenderNode extends AbstractNode {

    private ExpressionNode name;

    public ExpressionNode getName() {
        return name;
    }

    public void setName(ExpressionNode name) {
        this.name = name;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    @Override
    public String toString() {
        return "RENDER: '%s'".formatted(name);
    }

}
