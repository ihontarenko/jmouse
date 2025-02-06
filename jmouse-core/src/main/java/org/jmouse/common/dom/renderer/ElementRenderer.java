package org.jmouse.common.dom.renderer;

import org.jmouse.common.dom.Node;
import org.jmouse.common.dom.NodeContext;
import org.jmouse.common.dom.NodeType;
import org.jmouse.common.dom.Renderer;

import java.util.ConcurrentModificationException;
import java.util.Map;

public class ElementRenderer implements Renderer {

    @Override
    public String render(Node node, NodeContext context) {
        if (node.getNodeType() == NodeType.ELEMENT) {
            StringBuilder builder = new StringBuilder();

            openElementTag(builder, node);
            preformChildren(builder, node, context);
            closeElementTag(builder, node);

            return builder.toString();
        }

        throw new IllegalArgumentException("Incorrect node type for renderer");
    }

    private void openElementTag(StringBuilder builder, Node node) {
        Map<String, String> attributes = node.getAttributes();
        String              tagEnding  = node.hasChildren() ? ">\n" : " />\n";

        builder.append(indentation(node.getDepth())).append("<").append(node.getTagName());
        performAttributes(builder, attributes);
        builder.append(tagEnding);
    }

    private void closeElementTag(StringBuilder builder, Node node) {
        if (node.hasChildren()) {
            builder.append(indentation(node.getDepth())).append("</").append(node.getTagName()).append(">\n");
        }
    }

    private void performAttributes(StringBuilder builder, Map<String, String> attributes) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            builder.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
    }

    private void preformChildren(StringBuilder builder, Node node, NodeContext context) {
        try {
            for (Node child : node.getChildren()) {
                builder.append(child.interpret(context));
            }
        } catch (ConcurrentModificationException exception) {
            throw new IllegalStateException("Any interceptors are not allowed to modify the element tree during rendering");
        }
    }

}
