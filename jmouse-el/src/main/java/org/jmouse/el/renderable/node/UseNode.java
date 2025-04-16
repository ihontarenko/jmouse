package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.expression.NameSetNode;

public class UseNode extends AbstractNode {

    private ExpressionNode path;
    private NameSetNode    names;

    public ExpressionNode getPath() {
        return path;
    }

    public void setPath(ExpressionNode path) {
        this.path = path;
    }

    public NameSetNode getNames() {
        return names;
    }

    public void setNames(NameSetNode names) {
        this.names = names;
    }

}
