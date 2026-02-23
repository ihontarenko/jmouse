package org.jmouse.dom.renderer;

import org.jmouse.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.core.Verify.nonNull;

/**
 * Registry responsible for resolving a {@link Renderer} for a given {@link Node}. 🗂️
 *
 * <p>
 * The registry maintains an ordered list of renderers.
 * Resolution is performed sequentially — the first renderer
 * for which {@link Renderer#supports(Node)} returns {@code true}
 * is selected.
 * </p>
 *
 * <p>
 * Ordering therefore matters: more specific renderers should be
 * registered before more generic ones.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * RendererRegistry registry = RendererRegistry.builder()
 *     .add(new ElementRenderer())
 *     .add(new TextRenderer())
 *     .build();
 *
 * Renderer renderer = registry.findRenderer(node);
 * }</pre>
 *
 * <p>
 * This class is immutable and thread-safe after construction.
 * </p>
 */
public final class RendererRegistry {

    private final List<Renderer> renderers;

    /**
     * Creates a registry with the provided renderer list.
     *
     * @param renderers ordered renderer list (must not be {@code null})
     */
    public RendererRegistry(List<Renderer> renderers) {
        nonNull(renderers, "renderers");
        this.renderers = List.copyOf(renderers);
    }

    /**
     * Finds the first renderer that supports the given node.
     *
     * @param node node to render
     * @return matching renderer
     *
     * @throws IllegalStateException if no renderer supports the node
     */
    public Renderer findRenderer(Node node) {
        nonNull(node, "node");

        for (Renderer renderer : renderers) {
            if (renderer.supports(node)) {
                return renderer;
            }
        }

        throw new IllegalStateException(
                "No renderer registered for node type: " +
                        node.getNodeType() +
                        " (" + node.getClass().getName() + "). " +
                        "Registered renderers: " + renderers.stream()
                        .map(r -> r.getClass().getSimpleName())
                        .toList()
        );
    }

    /**
     * Creates a new builder for assembling a {@link RendererRegistry}.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RendererRegistry}. 🏗️
     *
     * <p>
     * Allows incremental registration of renderers
     * before constructing an immutable registry.
     * </p>
     */
    public static final class Builder {

        private final List<Renderer> renderers = new ArrayList<>();

        /**
         * Adds a renderer to the registry.
         *
         * <p>
         * Renderers are evaluated in insertion order.
         * </p>
         *
         * @param renderer renderer to add
         * @return this builder
         */
        public Builder add(Renderer renderer) {
            renderers.add(nonNull(renderer, "renderer"));
            return this;
        }

        /**
         * Builds an immutable {@link RendererRegistry}.
         *
         * @return registry instance
         */
        public RendererRegistry build() {
            return new RendererRegistry(renderers);
        }
    }
}