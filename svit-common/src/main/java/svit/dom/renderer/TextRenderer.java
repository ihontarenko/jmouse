package svit.dom.renderer;

import svit.dom.Node;
import svit.dom.NodeContext;
import svit.dom.Renderer;
import svit.dom.node.TextNode;

public class TextRenderer implements Renderer {

    @Override
    public String render(Node node, NodeContext context) {
        if (node instanceof TextNode textNode) {
            return indentation(node.getDepth()) + textNode.getText() + "\n";
        }

        throw new IllegalArgumentException("Incorrect node type for renderer");
    }

}
