package org.jmouse.el.renderable.node;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.renderable.AbstractRenderableNode;
import org.jmouse.el.renderable.Content;
import org.jmouse.el.renderable.Template;

public class IncludeNode extends AbstractRenderableNode {

    private final ExpressionNode path;

    public IncludeNode(ExpressionNode path) {
        this.path = path;
    }

    public ExpressionNode getPath() {
        return path;
    }

    @Override
    public void render(Content content, Template self, EvaluationContext context) {
        content.append("Include: " + path.evaluate(context));
    }
}
