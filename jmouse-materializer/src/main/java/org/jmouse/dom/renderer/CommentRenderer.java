package org.jmouse.dom.renderer;

import org.jmouse.dom.Node;
import org.jmouse.dom.node.CommentNode;

/**
 * Renderer for {@link CommentNode} instances. 💬
 *
 * <p>
 * Produces HTML/XML comment output in the form:
 * </p>
 *
 * <pre>{@code
 * <!-- comment text -->
 * }</pre>
 *
 * <h3>Safety handling</h3>
 * <p>
 * The sequence {@code "--"} is replaced with an em dash ({@code —})
 * to prevent invalid comment syntax (HTML/XML comments must not contain {@code "--"}).
 * </p>
 *
 * <p>
 * Indentation and newline handling respect {@link RendererContext}.
 * </p>
 */
public final class CommentRenderer implements Renderer {

    /**
     * Supports {@link CommentNode} only.
     *
     * @param node node to inspect
     * @return {@code true} if node is a {@link CommentNode}
     */
    @Override
    public boolean supports(Node node) {
        return node instanceof CommentNode;
    }

    /**
     * Renders the comment node.
     *
     * <p>
     * Null comment values are treated as empty strings.
     * The output is indented according to node depth when pretty-print is enabled.
     * </p>
     *
     * @param node comment node
     * @param context renderer context (indentation/newline policy)
     * @param processor rendering dispatcher (not used here)
     * @return rendered comment string
     */
    @Override
    public String render(Node node, RendererContext context, RenderingProcessor processor) {
        CommentNode comment   = (CommentNode) node;
        String      value     = comment.getComment() == null ? "" : comment.getComment();
        String      safeValue = value.replace("--", "—");

        return RenderSupport.indent(node.getDepth(), context)
                + "<!-- " + safeValue + " -->"
                + RenderSupport.newline(context);
    }
}