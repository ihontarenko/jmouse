package org.jmouse.dom.building;

import java.util.Objects;

/**
 * Registry that resolves a {@link NodeBuilder} for a {@link NodeBuildRequest}.
 */
public interface NodeBuilderRegistry {

    /**
     * Register a builder.
     *
     * @param builder builder
     * @return this registry
     */
    NodeBuilderRegistry register(NodeBuilder builder);

    /**
     * Resolve the best builder for the request.
     *
     * @param request build request
     * @param context build context
     * @return resolved builder
     */
    NodeBuilder resolve(NodeBuildRequest request, NodeBuildContext context);

    static NodeBuilderRegistry createDefault() {
        return new DefaultNodeBuilderRegistry();
    }
}
