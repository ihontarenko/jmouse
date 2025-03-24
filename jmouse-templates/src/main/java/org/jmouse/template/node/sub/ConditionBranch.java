package org.jmouse.template.node.sub;

import org.jmouse.el.node.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.RenderableNode;

public class ConditionBranch extends AbstractRenderableNode {

    private final ExpressionNode when;
    private final RenderableNode then;

    public ConditionBranch(ExpressionNode when, RenderableNode then) {
        this.when = when;
        this.then = then;
    }

    public ExpressionNode getWhen() {
        return when;
    }

    public RenderableNode getThen() {
        return then;
    }

}
