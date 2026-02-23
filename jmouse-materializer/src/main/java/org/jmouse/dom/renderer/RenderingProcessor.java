package org.jmouse.dom.renderer;

import org.jmouse.dom.Node;

import java.util.ConcurrentModificationException;

import static org.jmouse.core.Verify.nonNull;

/**
 * Rendering dispatcher that converts a DOM {@link Node} tree into a string. 🧵
 *
 * <p>
 * {@code RenderingProcessor} coordinates renderer selection via {@link RendererRegistry}
 * and performs recursive rendering by delegating each node to a matching {@link Renderer}.
 * </p>
 *
 * <h3>Key properties</h3>
 * <ul>
 *     <li>Renderer lookup is delegated to {@link RendererRegistry#findRenderer(Node)}.</li>
 *     <li>Recursion is performed through {@link #renderInternal(Node, RendererContext)}.</li>
 *     <li>Pretty-printing, escaping, and void-tags are controlled by {@link RendererContext}.</li>
 * </ul>
 *
 * <h3>Mutation safety</h3>
 * <p>
 * Rendering expects the node tree to be stable during traversal.
 * If the tree is modified concurrently (or structurally modified during iteration),
 * a {@link ConcurrentModificationException} may occur and is translated into
 * an {@link IllegalStateException} with a diagnostic message.
 * </p>
 */
public final class RenderingProcessor {

    private final RendererRegistry registry;

    /**
     * Creates a processor using the provided registry.
     *
     * @param registry renderer registry
     */
    public RenderingProcessor(RendererRegistry registry) {
        this.registry = nonNull(registry, "registry");
    }

    /**
     * Renders the given node tree starting at {@code root}.
     *
     * <p>
     * Delegates rendering to matching {@link Renderer}s. If the node tree is modified
     * during traversal, a {@link ConcurrentModificationException} is caught and rethrown
     * as an {@link IllegalStateException} with additional context.
     * </p>
     *
     * @param root root node to render
     * @param context renderer context (escaping, indentation, pretty print, void tags)
     * @return rendered markup string
     *
     * @throws IllegalStateException if rendering fails due to concurrent modification
     */
    public String render(Node root, RendererContext context) {
        nonNull(root, "root");
        nonNull(context, "context");

        try {
            return renderInternal(root, context);
        } catch (ConcurrentModificationException exception) {
            throw new IllegalStateException(
                    "Rendering failed: node tree was modified during rendering. " +
                            "Avoid mutations inside renderers / hooks or freeze the tree before render.",
                    exception
            );
        }
    }

    /**
     * Renders a node by selecting an appropriate {@link Renderer}.
     *
     * <p>
     * Package-private to limit direct calls; renderers should use this method
     * to render children to ensure consistent renderer selection.
     * </p>
     *
     * @param node node to render
     * @param context renderer context
     * @return rendered node string
     */
    String renderInternal(Node node, RendererContext context) {
        Renderer renderer = registry.findRenderer(node);
        return renderer.render(node, context, this);
    }
}