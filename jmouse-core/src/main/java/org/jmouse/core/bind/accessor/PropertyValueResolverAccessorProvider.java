package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorProvider;
import org.jmouse.core.bind.PropertyValueResolver;

/**
 * An {@link ObjectAccessorProvider} implementation for sources that implement
 * {@link PropertyValueResolver}. This valueProvider checks if the given source is a
 * {@code PropertyValueResolver} and creates an appropriate {@link ObjectAccessor}
 * to wrap it.
 */
public class PropertyValueResolverAccessorProvider implements ObjectAccessorProvider {

    /**
     * Checks if this valueProvider supports the given source object.
     *
     * @param source the object to be wrapped
     * @return {@code true} if the source is an instance of {@link PropertyValueResolver};
     *         {@code false} otherwise
     */
    @Override
    public boolean supports(Object source) {
        return source instanceof PropertyValueResolver;
    }

    /**
     * Creates an {@link ObjectAccessor} instance for the given source object.
     * <p>
     * If the source is an instance of {@link PropertyValueResolver}, this method wraps it
     * in a {@link PropertyValueResolverAccessor} and returns the resulting {@link ObjectAccessor}.
     * </p>
     *
     * @param source the object to wrap
     * @return an {@link ObjectAccessor} instance wrapping the specified source
     */
    @Override
    public ObjectAccessor create(Object source) {
        return new PropertyValueResolverAccessor((PropertyValueResolver) source);
    }
}
