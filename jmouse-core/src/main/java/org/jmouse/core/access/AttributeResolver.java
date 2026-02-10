package org.jmouse.core.access;

/**
 * Resolves custom or virtual attributes on objects during expression evaluation.
 * <p>
 * An {@code AttributeResolver} determines whether it can handle a given instance
 * type and, if so, retrieves the value of a named attribute from that instance.
 * This allows the expression language to support computed or dynamic properties
 * beyond standard field/getter access.
 * </p>
 */
public interface AttributeResolver {

    /**
     * Determines whether this resolver can handle the specified instance.
     *
     * @param instance the target object instance to check
     * @return {@code true} if this resolver supports resolving attributes on the instance;
     *         {@code false} otherwise
     */
    boolean supports(Object instance);

    /**
     * Resolves the value of the named attribute on the given instance.
     *
     * @param instance the object on which to resolve the attribute
     * @param name     the name of the attribute to resolve
     * @return the resolved attribute value, or {@code null} if not available
     * @throws IllegalArgumentException if the resolver does not support the instance
     */
    Object resolve(Object instance, String name);
}
