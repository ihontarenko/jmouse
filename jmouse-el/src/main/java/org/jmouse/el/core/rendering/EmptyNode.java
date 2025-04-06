package org.jmouse.el.core.rendering;

import org.jmouse.el.core.evaluation.EvaluationContext;

/**
 * Represents an empty renderable node.
 * <p>
 * This node produces no visible output. Its render method simply appends an empty string
 * to the provided content. It can serve as a placeholder or default when no content is available.
 * </p>
 */
public class EmptyNode extends AbstractRenderableNode {

    /**
     * Renders the empty node by appending an empty string to the output content.
     *
     * @param content the content to render into
     * @param self    the current template instance
     * @param context the evaluation context providing runtime data and services
     */
    @Override
    public void render(Content content, Template self, EvaluationContext context) {
        content.append("");
    }
}
