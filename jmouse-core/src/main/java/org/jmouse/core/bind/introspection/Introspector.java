package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.DataContainer;

/**
 * Defines a contract for introspecting data containers and generating metadata descriptors.
 * <p>
 * This interface is used to analyze and extract metadata from a data container and
 * construct a corresponding {@link Descriptor}. Implementations provide a fluent API
 * for setting properties and performing introspection.
 * </p>
 *
 * @param <C> the type of the data container holding metadata about {@code T}
 * @param <I> the specific implementation type of {@code Introspector} (used for fluent API)
 * @param <T> the type of the data being introspected
 * @param <D> the type of the descriptor generated from introspection
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public interface Introspector<C extends DataContainer<T>, I extends Introspector<?, ?, ?, ?>, T, D extends Descriptor<?, ?, ?>> {

    /**
     * Sets a custom name for the introspected element.
     *
     * @param name the name to set
     * @return this introspector instance for method chaining
     */
    I name(String name);

    /**
     * Automatically determines and sets the name for the introspected element.
     *
     * @return this introspector instance for method chaining
     */
    I name();

    /**
     * Performs the introspection process to analyze the data container.
     *
     * @return this introspector instance for method chaining
     */
    I introspect();

    /**
     * Converts the introspected data into a metadata descriptor.
     *
     * @return a {@link Descriptor} representing the analyzed data
     */
    D toDescriptor();

    /**
     * Returns this instance, ensuring proper type resolution in fluent method calls.
     *
     * @return this introspector instance, cast to the correct type
     */
    @SuppressWarnings({"unchecked"})
    default I self() {
        return (I) this;
    }

    C getContainerFor(T target);

}
