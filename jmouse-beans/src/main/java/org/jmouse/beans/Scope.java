package org.jmouse.beans;

/**
 * Interface representing a generic scope for managing structured lifecycles in the container.
 * <p>
 * Scopes define the lifecycle and sharing behavior of beans, such as whether they are
 * singletons, prototypes, or specific to certain contexts like HTTP requests or sessions.
 */
public interface Scope {

    /**
     * Retrieves the unique identifier for the scope.
     * <p>
     * This identifier is typically used for internal purposes, such as comparisons or
     * serialization, and is expected to be unique across all scopes.
     *
     * @return the unique identifier of the scope.
     */
    int id();

    /**
     * Retrieves the name of the scope.
     * <p>
     * The name typically matches the enum constant name or a human-readable identifier
     * for the scope.
     *
     * @return the name of the scope.
     */
    String name();

}
