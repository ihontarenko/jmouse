package org.jmouse.dom.rendering;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.Renderer;

import java.util.function.Predicate;

/**
 * Resolves a {@link Renderer} for a given {@link Node}.
 *
 * <p>Registrations are evaluated from highest order to lowest order.
 * The first matching registration wins.</p>
 */
public interface RendererRegistry {

    /**
     * Register a renderer with the given predicate and order.
     *
     * @param predicate matching rule
     * @param order higher value wins
     * @param renderer renderer instance
     * @return this registry
     */
    RendererRegistry register(Predicate<? super Node> predicate, int order, Renderer renderer);

    /**
     * Resolve a renderer for the given node.
     *
     * @param node node to render
     * @param context rendering context
     * @return resolved renderer
     */
    Renderer resolve(Node node, NodeContext context);
}
