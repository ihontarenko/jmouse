package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class RenderNode extends AbstractNode {

    private Expression name;

    public Expression getName() {
        return name;
    }

    public void setName(Expression name) {
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
