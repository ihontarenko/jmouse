package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;

import java.util.Collections;
import java.util.Map;

/**
 * The Renderer interface defines the contract for rendering a template into a {@link Content} object.
 */
public interface Renderer {

    /**
     * Renders the given template using the specified evaluation context.
     *
     * @param template the template to render
     * @param context  the evaluation context containing variables and inheritance information
     * @return the rendered content as a {@link Content} object
     */
    Content render(Template template, EvaluationContext context);

    default Content render(Template template, Map<String, Object> variables) {
        EvaluationContext context = template.newContext();
        variables.forEach(context::setValue);
        return render(template, context);
    }

    default Content render(Template template) {
        return render(template, Collections.emptyMap());
    }

}
