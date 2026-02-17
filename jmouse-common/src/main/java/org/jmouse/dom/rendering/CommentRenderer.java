package org.jmouse.dom.rendering;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.Renderer;
import org.jmouse.dom.node.CommentNode;

/**
 * Renders a {@link CommentNode}.
 */
public final class CommentRenderer extends AbstractRendererSupport implements Renderer {

    @Override
    public void render(Node node, NodeContext context) {
        Verify.nonNull(node, "node");
        Verify.nonNull(context, "context");

        if (!(node instanceof CommentNode commentNode)) {
            throw new IllegalArgumentException("COMMENT-RENDERER supports only CommentNode, got: " + node.getClass().getName());
        }

        context.output()
                .append(indentation(node))
                .append("<!-- ")
                .append(commentNode.getComment())
                .append(" -->")
                .append('\n');
    }
}
