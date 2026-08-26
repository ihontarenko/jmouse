package org.jmouse.core.access.accessor;

import org.jmouse.core.access.AbstractBeanAccessor;
import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectIntrospector;

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
    /**
     * {@inheritDoc}
     *
     * <p>⚠️ Answered from {@link DescriptorResolver}, which caches. This ran a full introspection on
     * every construction, and an accessor is constructed per wrapped object - so a record's components
     * were being rediscovered, with their annotations, for each one that passed through.</p>
     */
    @Override
    @SuppressWarnings({"unchecked"})
    protected ObjectDescriptor<Object> getDescriptor(Class<?> type) {
        return (ObjectDescriptor<Object>) (ObjectDescriptor<?>) DescriptorResolver.ofRecordType(
                (Class<? extends Record>) type);
    }
}
