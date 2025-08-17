package org.jmouse.web.mvc.view.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.renderable.Renderer;
import org.jmouse.el.renderable.Template;
import org.jmouse.web.mvc.view.AbstractView;
import org.jmouse.web.mvc.View;

import java.io.IOException;
import java.util.Map;

/**
 * 🧩 A concrete {@link View} implementation that renders a {@link Template}
 * using a {@link Renderer} and an {@link EvaluationContext}.
 *
 * <p>This class is typically used for rendering internal view-based views where model attributes
 * are evaluated and injected into the view content dynamically.</p>
 *
 * <p>It is suitable for views written using a custom templating engine or expression language
 * defined in the <code>org.jmouse.el</code> package.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @see Template
 * @see Renderer
 */
public class InternalView extends AbstractView {

    private final Template template;
    private final Renderer renderer;

    /**
     * Constructs a new InternalView with the given view and renderer.
     *
     * @param template the parsed view to be rendered
     * @param renderer the rendering engine responsible for producing output
     */
    public InternalView(Template template, Renderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    /**
     * Renders the view by evaluating it against the provided model,
     * and writes the result to the HTTP response.
     *
     * @param model    the model attributes to expose in the view
     * @param request  the current HTTP servlet request
     * @param response the current HTTP servlet response
     */
    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
        EvaluationContext context = template.newContext();
        model.forEach(context::setValue);

        try {
            char[] data = renderer.render(template, context).getDataArray();
            response.getWriter().write(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render view", e);
        }
    }
}
