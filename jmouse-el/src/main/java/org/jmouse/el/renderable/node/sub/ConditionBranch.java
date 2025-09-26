package org.jmouse.el.renderable.node.sub;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;

public class ConditionBranch extends AbstractNode {

    private final Expression when;
    private final Node       then;

    public ConditionBranch(Expression when, Node then) {
        this.when = when;
        this.then = then;
    }

    public Expression getWhen() {
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
