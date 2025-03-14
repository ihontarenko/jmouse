package org.jmouse.template.node.renderable;

import org.jmouse.template.node.AbstractRenderableNode;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.node.RenderableNode;

public class IfCondition extends AbstractRenderableNode {

    private final ExpressionNode when;
    private final RenderableNode then;

    public IfCondition(ExpressionNode when, RenderableNode then) {
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
