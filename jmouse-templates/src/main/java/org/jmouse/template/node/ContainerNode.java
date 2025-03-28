package org.jmouse.template.node;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.NodeMatcher;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.rendering.AbstractRenderableNode;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.Template;
import org.jmouse.el.rendering.RenderableNode;

/**
 * 📝 ContainerNode represents a container for a sequence of renderable nodes.
 * <p>
 * It iterates over its child nodes and renders each one. Optionally, when only safe nodes should be rendered,
 * a matcher is used to filter out unsafe nodes if the entity has a parent.
 * </p>
 */
public class ContainerNode extends AbstractRenderableNode {

    private Matcher<Node> matcher       = Matcher.constant(false);
    private boolean       onlySafeNodes = false;

    /**
     * Renders all child nodes into the provided content.
     * <p>
     * If onlySafeNodes is enabled and the entity has a parent, nodes not matching the safe criteria are skipped.
     * </p>
     *
     * @param content the content container to which the output is appended
     * @param self  the current renderable entity
     * @param context the evaluation context used during rendering
     */
    @Override
    public void render(Content content, Template self, EvaluationContext context) {
        for (Node child : children()) {
            if (child instanceof RenderableNode renderable) {
                if (context.getInheritance().getParent() != null && isOnlySafeNodes() && !matcher.matches(child)) {
                    continue;
                }
                renderable.render(content, self, context);
            }
        }
    }

    /**
     * Returns whether only safe nodes should be rendered.
     *
     * @return {@code true} if only safe nodes are rendered; {@code false} otherwise
     */
    public boolean isOnlySafeNodes() {
        return onlySafeNodes;
    }

    /**
     * Sets whether only safe nodes should be rendered.
     *
     * @param onlySafeNodes {@code true} to render only safe nodes; {@code false} otherwise
     */
    public void setOnlySafeNodes(boolean onlySafeNodes) {
        this.onlySafeNodes = onlySafeNodes;
    }

    /**
     * Returns the matcher used to determine which nodes are considered safe.
     *
     * @return the Matcher for safe nodes
     */
    public Matcher<Node> getMatcher() {
        return matcher;
    }

    /**
     * Sets the types of nodes considered safe.
     * <p>
     * This method updates the matcher to consider nodes of the specified types as safe.
     * </p>
     *
     * @param types one or more Node classes that are deemed safe
     */
    @SafeVarargs
    public final void setSafeNodes(Class<? extends Node>... types) {
        this.matcher = NodeMatcher.types(types);
    }
}
