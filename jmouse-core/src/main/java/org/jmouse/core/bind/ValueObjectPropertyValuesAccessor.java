package org.jmouse.core.bind;

import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.vo.ValueObjectIntrospector;

public class ValueObjectPropertyValuesAccessor extends AbstractBeanPropertyValuesAccessor {

    public ValueObjectPropertyValuesAccessor(Object source) {
        super(source);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected ObjectDescriptor<Object> getDescriptor(Class<?> type) {
        return new ValueObjectIntrospector<>((Class<Object>) type).introspect().toDescriptor();
    }

}
