package org.jmouse.template;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.Template;

public interface Renderer {
    Content render(Template template, EvaluationContext context);
}
