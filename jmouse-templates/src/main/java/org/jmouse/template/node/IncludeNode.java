package org.jmouse.template.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.rendering.AbstractRenderableNode;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.Template;

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
