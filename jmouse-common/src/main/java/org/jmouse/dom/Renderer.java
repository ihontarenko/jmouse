package org.jmouse.dom;

/**
 * Renders a node into the output provided by {@link NodeContext}.
 */
public interface Renderer {

    /**
     * Render the given node into the context output.
     *
     * @param node node to render
     * @param context rendering context
     */
    void render(Node node, NodeContext context);

}

