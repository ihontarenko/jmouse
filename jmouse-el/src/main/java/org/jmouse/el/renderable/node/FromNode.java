package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.NamesNode;
import org.jmouse.el.renderable.NodeVisitor;

public class FromNode extends AbstractNode {

    private ExpressionNode source;
    private NamesNode      names;

    public ExpressionNode getSource() {
        return source;
    }

    public void setSource(ExpressionNode source) {
        this.source = source;
    }

    public NamesNode getNames() {
        return names;
    }

    public void setNames(NamesNode names) {
        this.names = names;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

}
