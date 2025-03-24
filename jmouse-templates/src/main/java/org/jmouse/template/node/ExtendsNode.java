package org.jmouse.template.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractRenderableNode;
import org.jmouse.el.node.ExpressionNode;

import java.io.Writer;

public class ExtendsNode extends AbstractRenderableNode {

    private ExpressionNode parent;

    public ExpressionNode getParent() {
        return parent;
    }

    public void setParent(ExpressionNode parent) {
        this.parent = parent;
    }

    @Override
    public void render(Writer writer, EvaluationContext context) {
        super.render(writer, context);
    }

}
