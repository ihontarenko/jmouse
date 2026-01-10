package org.jmouse.common.dom;

import org.jmouse.common.dom.renderer.CommentRenderer;
import org.jmouse.common.dom.renderer.ElementRenderer;
import org.jmouse.common.dom.renderer.TextRenderer;

import java.util.HashMap;
import java.util.Map;

public class RendererFactory {
    private static final Map<NodeType, Renderer> renderers = new HashMap<>();

    static {
        renderers.put(NodeType.ELEMENT, new ElementRenderer());
        renderers.put(NodeType.TEXT, new TextRenderer());
        renderers.put(NodeType.COMMENT, new CommentRenderer());
    }

    public Renderer getRenderer(Node node) {
        Renderer renderer = renderers.get(node.getNodeType());

        if (renderer != null) {
            return renderer;
        }

        throw new IllegalArgumentException("RENDERER NOT FOUND FOR NODE: " + node);
    }
}
