package org.jmouse.el.rendering;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;

/**
 * 🔍 Represents a node that can be rendered in the template engine.
 * <p>
 * A RenderableNode extends {@link Node} by providing a render method that outputs
 * its content to the provided {@link Content} container. The render method also receives
 * the current {@link RenderableEntity} (which may represent a template, block, or fragment)
 * and the {@link EvaluationContext} used to resolve expressions during rendering.
 * </p>
 *
 * @see Content
 * @see RenderableEntity
 * @see EvaluationContext
 */
public interface RenderableNode extends Node {

    /**
     * Renders this node, appending its output to the given content.
     * <p>
     * The method uses the specified renderable entity as the current context and
     * the evaluation context for expression resolution.
     * </p>
     *
     * @param content the content container to which the output is appended
     * @param entity  the current renderable entity (e.g., template, block, or fragment)
     * @param context the evaluation context for resolving expressions and variables
     */
    void render(Content content, RenderableEntity entity, EvaluationContext context);
}
