package svit.dom.renderer;

import svit.dom.Node;
import svit.dom.NodeContext;
import svit.dom.Renderer;
import svit.dom.node.CommentNode;

public class CommentRenderer implements Renderer {

    @Override
    public String render(Node node, NodeContext context) {
        if (node instanceof CommentNode commentNode) {
            return indentation(node.getDepth()) + "<!-- " + commentNode.getComment() + " -->\n";
        }

        throw new IllegalArgumentException("INCORRECT NODE TYPE FOR RENDERER");
    }

}
