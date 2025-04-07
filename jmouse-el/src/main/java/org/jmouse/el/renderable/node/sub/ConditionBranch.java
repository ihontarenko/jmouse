package org.jmouse.el.renderable.node.sub;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;

public class ConditionBranch extends AbstractNode {

    private final ExpressionNode when;
    private final Node           then;

    public ConditionBranch(ExpressionNode when, Node then) {
        this.when = when;
        this.then = then;
    }

    public ExpressionNode getWhen() {
        return when;
    }

    public Node getThen() {
        return then;
    }

    @Override
    public String toString() {
        return when == null ? "ELSE" : "IF(-ELSE)";
    }

}
