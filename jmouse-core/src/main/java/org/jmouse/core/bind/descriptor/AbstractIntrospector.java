package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.DataContainer;

import java.util.function.Supplier;


/**
 * Provides an abstract base implementation for introspects, encapsulating common functionality
 * such as naming and descriptor caching mechanisms.
 *
 * @param <C> type of data container holding descriptor data
 * @param <I> introspector type itself
 * @param <T> introspected target type
 * @param <D> type of resulting descriptor
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class AbstractIntrospector<C extends DataContainer<T>, I extends Introspector<?, ?, ?, ?>, T, D extends Descriptor<?, ?, ?>>
        implements Introspector<C, I, T, D> {

    protected C container;
    protected D descriptor;

    protected AbstractIntrospector(T target) {
        this.container = getContainerFor(target);
    }

    /**
     * Sets a name for the introspected element.
     *
     * @param name the element's name
     * @return current introspector instance for chaining
     */
    @Override
    public I name(String name) {
        this.container.setName(name);
        return self();
    }

    /**
     * Returns a cached descriptor, creating it via a supplier if necessary.
     *
     * @param factory supplier to create the descriptor if not already cached
     * @return cached descriptor instance
     */
    protected D getCachedDescriptor(Supplier<D> factory) {
        D descriptor = this.descriptor;

        if (descriptor == null) {
            descriptor = factory.get();
            this.descriptor = descriptor;
        }

        return descriptor;
    }

}
