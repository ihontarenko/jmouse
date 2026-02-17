package org.jmouse.common.dom.rendering;

import org.jmouse.common.dom.Node;
import org.jmouse.common.dom.NodeContext;
import org.jmouse.common.dom.Renderer;

import java.util.function.Predicate;

/**
 * Registry that resolves a {@link Renderer} for a given {@link Node}.
 *
 * <p>Registrations are evaluated in order (highest order first). The first matching registration wins.</p>
 */
public interface RendererRegistry {

    /**
     * Register a renderer with a matching predicate and an order.
     *
     * @param predicate match rule for nodes
     * @param order higher values win over lower values
     * @param renderer renderer to use when predicate matches
     * @return this registry
     */
    RendererRegistry register(Predicate<? super Node> predicate, int order, Renderer renderer);

    /**
     * Register a renderer with a matching predicate using default order (0).
     *
     * @param predicate match rule for nodes
     * @param renderer renderer to use when predicate matches
     * @return this registry
     */
    default RendererRegistry register(Predicate<? super Node> predicate, Renderer renderer) {
        return register(predicate, 0, renderer);
    }

    /**
     * Resolve a renderer for the given node.
     *
     * @param node node to render
     * @param context node context
     * @return resolved renderer
     * @throws IllegalStateException if no renderer matches
     */
    Renderer resolve(Node node, NodeContext context);

}
