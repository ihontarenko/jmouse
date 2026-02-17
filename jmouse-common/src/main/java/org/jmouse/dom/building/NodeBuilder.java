package org.jmouse.dom.building;

import org.jmouse.dom.Node;

/**
 * Builds a DOM node tree from a {@link NodeBuildRequest}.
 */
public interface NodeBuilder {

    /**
     * Whether this builder can handle the request.
     *
     * @param request build request
     * @param context build context
     * @return true if supported
     */
    boolean supports(NodeBuildRequest request, NodeBuildContext context);

    /**
     * Build a DOM node tree for the request.
     *
     * @param request build request
     * @param context build context
     * @return built node tree root
     */
    Node build(NodeBuildRequest request, NodeBuildContext context);

    /**
     * Order value. Higher value has higher priority.
     *
     * @return order
     */
    default int order() {
        return 0;
    }
}
