package org.jmouse.dom.rendering;

import org.jmouse.dom.NodeType;

/**
 * Provides standard renderer registrations for DOM nodes.
 */
public final class StandardRendererRegistry {

    private StandardRendererRegistry() {}

    public static RendererRegistry create() {
        RendererRegistry registry = new DefaultRendererRegistry();

        registry.register(node -> node.getNodeType() == NodeType.ELEMENT, 0, new ElementRenderer());
        registry.register(node -> node.getNodeType() == NodeType.TEXT, 0, new TextRenderer());
        registry.register(node -> node.getNodeType() == NodeType.COMMENT, 0, new CommentRenderer());

        return registry;
    }
}
