package org.jmouse.dom.renderer;

import org.jmouse.dom.Node;
import org.jmouse.dom.node.TextNode;

/**
 * Renderer for {@link TextNode} instances. 📝
 *
 * <p>
 * Outputs text content with proper escaping according to
 * {@link RendererContext#escapeMode()}.
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *     <li>Text is escaped via {@link RenderSupport#escapeText(String, RendererContext)}.</li>
 *     <li>Indentation and newline are applied when pretty-print mode is enabled.</li>
 *     <li>{@code null} text values are rendered as empty strings.</li>
 * </ul>
 */
public final class TextRenderer implements Renderer {

    /**
     * Supports {@link TextNode} only.
     *
     * @param node node to inspect
     * @return {@code true} if node is a {@link TextNode}
     */
    @Override
    public boolean supports(Node node) {
        return node instanceof TextNode;
    }

    /**
     * Renders the text node.
     *
     * <p>
     * The rendering engine parameter is not used since text nodes
     * do not have children.
     * </p>
     *
     * @param node text node
     * @param context renderer context (escaping, formatting)
     * @param processor rendering dispatcher (unused)
     * @return rendered text string
     */
    @Override
    public String render(Node node, RendererContext context, RenderingProcessor processor) {
        TextNode textNode = (TextNode) node;
        String text = RenderSupport.escapeText(textNode.getText(), context);

        return RenderSupport.indent(node.getDepth(), context)
                + text
                + RenderSupport.newline(context);
    }

}