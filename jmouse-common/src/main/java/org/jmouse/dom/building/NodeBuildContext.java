package org.jmouse.dom.building;

import org.jmouse.dom.Node;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Context for recursive node building.
 */
public interface NodeBuildContext {

    /**
     * Build a node tree for a nested value with a declared type.
     *
     * @param value nested value
     * @param declaredType declared type
     * @return built node root
     */
    Node build(Object value, Type declaredType);

    /**
     * Build using a full request.
     *
     * @param request build request
     * @return built node root
     */
    Node build(NodeBuildRequest request);

    /**
     * Access to the builder registry.
     *
     * @return registry
     */
    NodeBuilderRegistry registry();

    /**
     * Utility to require non-null values in builders.
     *
     * @param value value
     * @param name name
     * @param <T> type
     * @return value
     */
    default <T> T requireNonNull(T value, String name) {
        return Objects.requireNonNull(value, name);
    }
}
