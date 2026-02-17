package org.jmouse.common.dom.old_renderer;

import org.jmouse.common.dom.Node;
import org.jmouse.common.dom.NodeContext;
import org.jmouse.common.dom.Renderer;
import org.jmouse.common.dom.node.CommentNode;

public class CommentRenderer implements Renderer {

    @Override
    public String render(Node node, NodeContext context) {
        if (node instanceof CommentNode commentNode) {
            return indentation(node.getDepth()) + "<!-- " + commentNode.getComment() + " -->\n";
        }

        throw new IllegalArgumentException("INCORRECT NODE TYPE FOR RENDERER");
    }

}
