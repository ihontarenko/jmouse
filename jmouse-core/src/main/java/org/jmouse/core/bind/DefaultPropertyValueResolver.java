package org.jmouse.core.bind;

public class DefaultPropertyValueResolver implements PropertyValueResolver, VirtualPropertyResolver.Aware {

    private final ObjectAccessor accessor;
    private VirtualPropertyResolver resolver;

    public DefaultPropertyValueResolver(ObjectAccessor accessor, VirtualPropertyResolver resolver) {
        this.accessor = accessor;
        this.resolver = resolver;
    }

    public DefaultPropertyValueResolver(ObjectAccessor accessor) {
        this(accessor, null);
    }

    @Override
    public ObjectAccessor getAccessor() {
        return accessor;
    }

    @Override
    public Object getProperty(String name) {
        Object value = null;

        try {
            value = resolve(name);
        } catch (IllegalArgumentException ignored) {}

        if (value == null && getVirtualProperties() != null) {
            @SuppressWarnings({"unchecked"})
            VirtualProperty<Object> virtualProperty = (VirtualProperty<Object>) getVirtualProperties()
                    .resolveProperty(accessor.unwrap(), name);
            if (virtualProperty != null && virtualProperty.isReadable()) {
                value = virtualProperty.readValue(accessor.unwrap());
            }
        }

        return value;
    }

    @Override
    public VirtualPropertyResolver getVirtualProperties() {
        return resolver;
    }

    @Override
    public void setVirtualProperties(VirtualPropertyResolver resolver) {
        this.resolver = resolver;
    }
}
