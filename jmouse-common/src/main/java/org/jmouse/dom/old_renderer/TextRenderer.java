package org.jmouse.dom.old_renderer;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.Renderer;
import org.jmouse.dom.node.TextNode;

public class TextRenderer implements Renderer {

    @Override
    public String render(Node node, NodeContext context) {
        if (node instanceof TextNode textNode) {
            return indentation(node.getDepth()) + textNode.getText() + "\n";
        }

        throw new IllegalArgumentException("Incorrect node type for renderer");
    }

}
