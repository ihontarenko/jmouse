package org.jmouse.dom.rendering;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.Renderer;
import org.jmouse.dom.node.TextNode;

/**
 * Renders a {@link TextNode}.
 */
public final class TextRenderer extends AbstractRendererSupport implements Renderer {

    @Override
    public void render(Node node, NodeContext context) {
        Verify.nonNull(node, "node");
        Verify.nonNull(context, "context");

        if (!(node instanceof TextNode textNode)) {
            throw new IllegalArgumentException("TEXT-RENDERER supports only TextNode, got: " + node.getClass().getName());
        }

        context.output()
                .append(indentation(node))
                .append(textNode.getText())
                .append('\n');
    }
}
