package org.jmouse.el.renderable.node;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.rendering.AbstractRenderableNode;
import org.jmouse.el.core.rendering.Content;
import org.jmouse.el.core.rendering.Template;
import org.jmouse.el.core.rendering.RenderingException;

/**
 * Represents a raw text node in a templating system.
 *
 * <p>This node is used to store plain text that does not require further processing,
 * such as static HTML or text content in a template.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RawTextNode extends AbstractRenderableNode {

    /**
     * The raw text content of this node.
     */
    private final char[] data;

    /**
     * Constructs a {@code RawTextNode} with the specified text content.
     *
     * @param string the raw text content
     */
    public RawTextNode(String string) {
        this.data = string.toCharArray();
    }

    /**
     * Returns the raw text content of this node.
     *
     * @return the raw text content
     */
    public String getString() {
        return new String(data);
    }

    /**
     * Renders the node to the given content using the provided renderable entity and evaluation context.
     * <p>
     * This default implementation always throws a {@link RenderingException}, as rendering is not supported
     * until overridden by a subclass.
     * </p>
     *
     * @param content the container for the rendered output
     * @param self  the current renderable entity (template, block, etc.)
     * @param context the evaluation context with runtime data and services
     * @throws RenderingException always, since rendering is not implemented in this abstract class
     */
    @Override
    public void render(Content content, Template self, EvaluationContext context) {
        content.append(getString());
    }

    @Override
    public String toString() {
        return "RAW[%s]".formatted(new String(data));
    }
}
