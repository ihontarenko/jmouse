package org.jmouse.core.access.accessor;

import org.jmouse.core.access.AbstractBeanAccessor;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;

/**
 * A {@link ObjectAccessor} implementation for accessing properties of a structured instance.
 * <p>
 * This class allows retrieving properties dynamically from a wrapped structured instance.
 * It does not support indexed access since beans are typically key-value structures.
 * </p>
 */
public class JavaBeanAccessor extends AbstractBeanAccessor {

    /**
     * Creates a {@link JavaBeanAccessor} for the given structured instance.
     *
     * @param source the structured instance to wrap
     * @throws IllegalArgumentException if the source is {@code null}
     */
    public JavaBeanAccessor(Object source) {
        super(source);
    }

    /**
     * {@inheritDoc}
     *
     * <p>⚠️ Answered from {@link DescriptorResolver}, which is where bean descriptors are cached, rather
     * than from a cache of this class's own. An accessor is built per wrapped object, so this is asked
     * constantly; it previously memoized into a plain {@link java.util.HashMap} populated with
     * {@code computeIfAbsent} from whatever thread arrived, which is a resize away from a corrupt table.
     * Sharing the one cache also stops the same class being introspected twice and held twice.</p>
     */
    @Override
    @SuppressWarnings({"unchecked"})
    protected ObjectDescriptor<Object> getDescriptor(Class<?> type) {
        return (ObjectDescriptor<Object>) DescriptorResolver.ofBeanType(type);
    }

}
