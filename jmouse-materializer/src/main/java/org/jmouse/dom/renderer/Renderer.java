package org.jmouse.dom.renderer;

import org.jmouse.dom.Node;

/**
 * Strategy interface for rendering a {@link Node} into textual output. 🎨
 *
 * <p>
 * A {@code Renderer} is responsible for handling a specific kind of DOM node
 * (e.g. element, text, comment) and converting it into a string representation.
 * </p>
 *
 * <p>
 * Renderers are typically selected by a {@link RenderingProcessor}
 * based on {@link #supports(Node)}.
 * </p>
 *
 * <h3>Lifecycle</h3>
 *
 * <pre>{@code
 * if (renderer.supports(node)) {
 *     return renderer.render(node, context, processor);
 * }
 * }</pre>
 *
 * <p>
 * Implementations should be stateless and thread-safe.
 * </p>
 */
public interface Renderer {

    /**
     * Determines whether this renderer can handle the given node.
     *
     * <p>
     * Usually implemented by checking {@code node.getNodeType()}.
     * </p>
     *
     * @param node node to inspect
     * @return {@code true} if this renderer supports the node
     */
    boolean supports(Node node);

    /**
     * Renders a node into its string representation.
     *
     * <p>
     * Child nodes should typically be rendered via
     * {@link RenderingProcessor#renderInternal(Node, RendererContext)}
     * to ensure proper renderer delegation.
     * </p>
     *
     * @param node node to render
     * @param context rendering configuration (escaping, indentation, formatting)
     * @param engine rendering dispatcher used for recursive rendering
     * @return rendered string (never {@code null})
     */
    String render(Node node, RendererContext context, RenderingProcessor engine);
}