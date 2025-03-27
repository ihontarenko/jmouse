package org.jmouse.el.rendering;

import org.jmouse.el.evaluation.EvaluationContext;

public class EmptyNode extends AbstractRenderableNode {

    @Override
    public void render(Content content, Template entity, EvaluationContext context) {
        content.append("<empty node>");
    }

}
