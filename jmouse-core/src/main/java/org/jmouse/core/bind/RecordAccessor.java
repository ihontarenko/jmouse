package org.jmouse.core.bind;

import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.vo.ValueObjectIntrospector;

/**
 * An implementation of an ObjectAccessor specifically for value objects.
 * <p>
 * This accessor leverages a {@link ValueObjectIntrospector} to introspect the given value object
 * and generate an {@link ObjectDescriptor} that describes its structure. The descriptor is then
 * used to facilitate property access and manipulation.
 */
public class RecordAccessor extends AbstractBeanAccessor {

    /**
     * Constructs a new RecordAccessor for the specified source object.
     *
     * @param source the value object to be accessed
     */
    public RecordAccessor(Object source) {
        super(source);
    }

    /**
     * Retrieves an {@link ObjectDescriptor} for the specified type by introspecting the value object.
     *
     * @param type the class type of the value object to introspect
     * @return an {@link ObjectDescriptor} representing the structure of the value object
     */
    @Override
    @SuppressWarnings({"unchecked"})
    protected ObjectDescriptor<Object> getDescriptor(Class<?> type) {
        return new ValueObjectIntrospector<>((Class<Object>) type).introspect().toDescriptor();
    }
}
