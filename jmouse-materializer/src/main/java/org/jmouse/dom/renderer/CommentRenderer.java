package org.jmouse.dom.renderer;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.Renderer;
import org.jmouse.dom.node.CommentNode;

public class CommentRenderer implements Renderer {

    @Override
    public String render(Node node, NodeContext context) {
        if (node instanceof CommentNode commentNode) {
            return indentation(node.getDepth()) + "<!-- " + commentNode.getComment() + " -->\n";
        }
        throw new IllegalArgumentException("INCORRECT NODE TYPE FOR RENDERER");
    }

}
