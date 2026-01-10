package org.jmouse.common.dom.renderer;

import org.jmouse.common.dom.Node;
import org.jmouse.common.dom.NodeContext;
import org.jmouse.common.dom.Renderer;
import org.jmouse.common.dom.node.TextNode;

public class TextRenderer implements Renderer {

    @Override
    public String render(Node node, NodeContext context) {
        if (node instanceof TextNode textNode) {
            return indentation(node.getDepth()) + textNode.getText() + "\n";
        }

        throw new IllegalArgumentException("Incorrect node type for renderer");
    }

}
