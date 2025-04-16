package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;

import java.util.Collections;
import java.util.Map;

/**
 * Defines the contract for rendering a template into a {@link Content} object.
 * <p>
 * This interface provides methods to render a template using a fully-configured
 * {@link EvaluationContext}, or by supplying a map of variable values that will be
 * used to create a new context. A default method also exists for rendering without
 * additional variables.
 * </p>
 */
public interface Renderer {

    /**
     * Renders the specified template using the given evaluation context.
     *
     * @param template the template to render
     * @param context  the evaluation context containing variables and inheritance information
     * @return the rendered content as a {@link Content} object
     */
    Content render(Template template, EvaluationContext context);

    /**
     * Renders the specified template using a set of variables.
     * <p>
     * A new evaluation context is created for the template, and the provided variables are
     * populated into the context prior to rendering.
     * </p>
     *
     * @param template  the template to render
     * @param variables a map of variable names to their respective values, used during rendering
     * @return the rendered content as a {@link Content} object
     */
    default Content render(Template template, Map<String, Object> variables) {
        EvaluationContext context = template.newContext();
        variables.forEach(context::setValue);
        return render(template, context);
    }

    /**
     * Renders the specified template without any additional variables.
     * <p>
     * This method creates an empty variables map and delegates to the render method that
     * accepts a map of variables.
     * </p>
     *
     * @param template the template to render
     * @return the rendered content as a {@link Content} object
     */
    default Content render(Template template) {
        return render(template, Collections.emptyMap());
    }
}
