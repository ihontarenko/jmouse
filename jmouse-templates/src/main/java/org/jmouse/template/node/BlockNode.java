package org.jmouse.template.node;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.rendering.*;

public class BlockNode extends AbstractRenderableNode {

    private ExpressionNode name;
    private RenderableNode body;

    public ExpressionNode getName() {
        return name;
    }

    public void setName(ExpressionNode name) {
        this.name = name;
    }

    public RenderableNode getBody() {
        return body;
    }

    public void setBody(RenderableNode body) {
        this.body = body;
    }

    @Override
    public void render(Content content, Template self, EvaluationContext context) {
        Conversion conversion = context.getConversion();
        Object     compiled   = getName().evaluate(context);
        String     name       = conversion.convert(compiled, String.class);

        self.renderBlock(name, content, context);
    }

}
