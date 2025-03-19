package org.jmouse.core.bind;

/**
 * A default implementation of {@link PropertyValueResolver} that resolves property values
 * using an underlying {@link ObjectAccessor} and, optionally, a {@link VirtualPropertyResolver}
 * for virtual (computed) properties.
 * <p>
 * This resolver first attempts to retrieve a property value via the underlying accessor's
 * standard resolution mechanism (e.g. via structured property navigation). If that lookup
 * returns {@code null} and a virtual property resolver is available, it attempts to resolve
 * the property virtually.
 * </p>
 */
public class DefaultPropertyValueResolver implements PropertyValueResolver, VirtualPropertyResolver.Aware {

    private final ObjectAccessor          accessor;
    private       VirtualPropertyResolver resolver;

    /**
     * Constructs a DefaultPropertyValueResolver with the specified ObjectAccessor and VirtualPropertyResolver.
     *
     * @param accessor the underlying ObjectAccessor used for standard property access
     * @param resolver the VirtualPropertyResolver used for resolving virtual properties; may be {@code null}
     */
    public DefaultPropertyValueResolver(ObjectAccessor accessor, VirtualPropertyResolver resolver) {
        this.accessor = accessor;
        this.resolver = resolver;
    }

    /**
     * Constructs a DefaultPropertyValueResolver with the specified ObjectAccessor.
     * <p>
     * In this constructor, no VirtualPropertyResolver is provided, so only direct property resolution will be performed.
     * </p>
     *
     * @param accessor the underlying ObjectAccessor used for property access
     */
    public DefaultPropertyValueResolver(ObjectAccessor accessor) {
        this(accessor, null);
    }

    /**
     * Returns the underlying ObjectAccessor.
     *
     * @return the ObjectAccessor used for property access
     */
    @Override
    public ObjectAccessor getAccessor() {
        return accessor;
    }

    /**
     * Retrieves the value of the specified property.
     * <p>
     * This method first attempts to resolve the property using the standard resolution mechanism.
     * If that returns {@code null} and a VirtualPropertyResolver is set, it then attempts to resolve
     * the property virtually. If a virtual property is found and is readable, its value is returned.
     * </p>
     *
     * @param name the name of the property to resolve
     * @return the resolved property value, or {@code null} if no value is found
     */
    @Override
    public Object getProperty(String name) {
        Object value = null;

        try {
            // Attempt to resolve the property via the default resolution mechanism.
            value = resolve(name);
        } catch (IllegalArgumentException ignored) {
            // Ignore exceptions during standard resolution.
        }

        // If the direct value is null, try resolving it as a virtual property.
        if (value == null && getVirtualProperties() != null) {
            @SuppressWarnings("unchecked")
            VirtualProperty<Object> virtual = (VirtualProperty<Object>) getVirtualProperties().resolveProperty(
                    accessor.unwrap(), name);
            if (virtual != null && virtual.isReadable()) {
                value = virtual.readValue(accessor.unwrap());
            }
        }

        return value;
    }

    /**
     * Returns the currently set VirtualPropertyResolver.
     *
     * @return the VirtualPropertyResolver, or {@code null} if none is set
     */
    @Override
    public VirtualPropertyResolver getVirtualProperties() {
        return resolver;
    }

    /**
     * Sets the VirtualPropertyResolver to be used for resolving virtual properties.
     *
     * @param resolver the VirtualPropertyResolver to set
     */
    @Override
    public void setVirtualProperties(VirtualPropertyResolver resolver) {
        this.resolver = resolver;
    }
}
