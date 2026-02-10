package org.jmouse.core.access;

/**
 * Defines a valueProvider for creating {@link ObjectAccessor} instances.
 * <p>
 * Implementations of this interface determine whether they support a given source object
 * and, if so, create an appropriate {@link ObjectAccessor} to wrap that object.
 * This pattern allows for flexible and modular accessor creation based on the source type.
 * </p>
 */
public interface ObjectAccessorProvider {

    /**
     * Checks if this valueProvider supports the given source object.
     *
     * @param source the object to be wrapped
     * @return {@code true} if this valueProvider can create an ObjectAccessor for the source; {@code false} otherwise
     */
    boolean supports(Object source);

    /**
     * Creates an {@link ObjectAccessor} instance for the given source object.
     *
     * @param source the object to wrap
     * @return an ObjectAccessor instance wrapping the specified source
     */
    ObjectAccessor create(Object source);
}
