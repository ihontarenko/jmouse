package org.jmouse.core.bind;

public class PropertyValueResolverAccessorProvider implements ObjectAccessorProvider {
    /**
     * Checks if this provider supports the given source object.
     *
     * @param source the object to be wrapped
     * @return {@code true} if this provider can create an ObjectAccessor for the source; {@code false} otherwise
     */
    @Override
    public boolean supports(Object source) {
        return source instanceof PropertyValueResolver;
    }

    /**
     * Creates an {@link ObjectAccessor} instance for the given source object.
     *
     * @param source the object to wrap
     * @return an ObjectAccessor instance wrapping the specified source
     */
    @Override
    public ObjectAccessor create(Object source) {
        return new PropertyValueResolverAccessor((PropertyValueResolver) source);
    }

}
