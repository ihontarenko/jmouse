package org.jmouse.el.node;

import org.jmouse.el.evaluation.EvaluationContext;

import java.io.Writer;

public interface RenderableNode extends Node {

    void render(Writer writer, EvaluationContext context);

}
