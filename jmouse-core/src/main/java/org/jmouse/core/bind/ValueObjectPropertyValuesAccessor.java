package org.jmouse.core.bind;

import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.core.bind.introspection.structured.vo.ValueObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.vo.ValueObjectIntrospector;

public class ValueObjectPropertyValuesAccessor extends AbstractPropertyValuesAccessor {

    private final ValueObjectDescriptor<Object> descriptor;

    /**
     * Constructs an {@link ValueObjectPropertyValuesAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    @SuppressWarnings({"unchecked"})
    public ValueObjectPropertyValuesAccessor(Object source) {
        super(source);
        Class<Object> valueObjectType = (Class<Object>) source.getClass();
        this.descriptor = new ValueObjectIntrospector<>(valueObjectType).introspect().toDescriptor();
    }

    @Override
    public PropertyValuesAccessor get(String name) {
        PropertyDescriptor<Object> property = descriptor.getProperty(name);

        if (!descriptor.hasProperty(name)) {
            throw new IllegalArgumentException(
                    "Bean factory does not have property: '%s'.".formatted(name));
        }

        return PropertyValuesAccessor.wrap(property.getPropertyAccessor().obtainValue(source));
    }

    @Override
    public PropertyValuesAccessor get(int index) {
        return null;
    }
}
