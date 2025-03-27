package org.jmouse.el.rendering;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractNode;

/**
 * 🚀 An abstract base class for renderable nodes.
 * <p>
 * This class extends {@link AbstractNode} and implements the {@link RenderableNode} interface.
 * Its default {@code render} method throws a {@link RenderingException}, indicating that the
 * render functionality must be provided by concrete subclasses.
 * </p>
 */
public class AbstractRenderableNode extends AbstractNode implements RenderableNode {

    /**
     * Renders the node to the given content using the provided renderable entity and evaluation context.
     * <p>
     * This default implementation always throws a {@link RenderingException}, as rendering is not supported
     * until overridden by a subclass.
     * </p>
     *
     * @param content the container for the rendered output
     * @param entity  the current renderable entity (template, block, etc.)
     * @param context the evaluation context with runtime data and services
     * @throws RenderingException always, since rendering is not implemented in this abstract class
     */
    @Override
    public void render(Content content, Template entity, EvaluationContext context) {
        throw new RenderingException("Renderable node '%s' is not supported yet.".formatted(this.getClass()));
    }
}
