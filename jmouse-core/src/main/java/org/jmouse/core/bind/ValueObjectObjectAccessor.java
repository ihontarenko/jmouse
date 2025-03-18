package org.jmouse.core.bind;

import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.vo.ValueObjectIntrospector;

public class ValueObjectObjectAccessor extends AbstractBeanObjectAccessor {

    public ValueObjectObjectAccessor(Object source) {
        super(source);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected ObjectDescriptor<Object> getDescriptor(Class<?> type) {
        return new ValueObjectIntrospector<>((Class<Object>) type).introspect().toDescriptor();
    }

}
