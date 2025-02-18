package org.jmouse.beans;

/**
 * Functional interface for resolving a {@link Scope} based on a given structured name.
 * <p>
 * This interface allows for dynamic determination of a structured's lifecycle scope
 * during its resolution or creation in the container.
 * </p>
 */
@FunctionalInterface
public interface ScopeResolver {

    /**
     * Resolves the {@link Scope} for a given structured name.
     *
     * @param name the name of the structured whose scope is to be resolved.
     * @return the {@link Scope} associated with the given structured name.
     */
    Scope resolveScope(String name);
}
