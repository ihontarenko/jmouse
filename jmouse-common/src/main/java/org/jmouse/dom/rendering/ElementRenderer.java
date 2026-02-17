package org.jmouse.dom.rendering;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.NodeType;
import org.jmouse.dom.Renderer;

import java.util.ConcurrentModificationException;
import java.util.Map;

public final class ElementRenderer extends AbstractRendererSupport implements Renderer {

    @Override
    public void render(Node node, NodeContext context) {
        Verify.nonNull(node, "node");
        Verify.nonNull(context, "context");

        if (node.getNodeType() != NodeType.ELEMENT) {
            throw new IllegalArgumentException("ELEMENT-RENDERER supports only element nodes, got: " + node.getNodeType());
        }

        writeOpenTag(node, context);

        if (node.hasChildren()) {
            writeChildren(node, context);
            writeCloseTag(node, context);
        }
    }

    private void writeOpenTag(Node node, NodeContext context) {
        Map<String, String> attributes = node.getAttributes();
        boolean hasChildren = node.hasChildren();

        context.output()
                .append(indentation(node))
                .append('<')
                .append(node.getTagName());

        writeAttributes(attributes, context);

        if (hasChildren) {
            context.output().append('>').append('\n');
        } else {
            context.output().append(" />").append('\n');
        }
    }

    private void writeCloseTag(Node node, NodeContext context) {
        context.output()
                .append(indentation(node))
                .append("</")
                .append(node.getTagName())
                .append('>')
                .append('\n');
    }

    private void writeAttributes(Map<String, String> attributes, NodeContext context) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            context.output()
                    .append(' ')
                    .append(entry.getKey())
                    .append("=\"")
                    .append(entry.getValue())
                    .append('"');
        }
    }

    private void writeChildren(Node node, NodeContext context) {
        try {
            for (Node child : node.getChildren()) {
                Renderer renderer = context.registry().resolve(child, context);
                renderer.render(child, context);
            }
        } catch (ConcurrentModificationException exception) {
            throw new IllegalStateException("Tree modification during rendering is not allowed.", exception);
        }
    }
}
