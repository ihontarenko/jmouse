package org.jmouse.template.node.sub;

import org.jmouse.el.rendering.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.rendering.RenderableNode;

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

    @Override
    public String toString() {
        return when == null ? "ELSE" : "IF(-ELSE)";
    }

}
