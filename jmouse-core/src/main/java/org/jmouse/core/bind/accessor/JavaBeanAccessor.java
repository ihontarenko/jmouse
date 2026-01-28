package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.AbstractBeanAccessor;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.bean.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.bean.JavaBeanIntrospector;

import java.util.HashMap;
import java.util.Map;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link ObjectAccessor} implementation for accessing properties of a structured instance.
 * <p>
 * This class allows retrieving properties dynamically from a wrapped structured instance.
 * It does not support indexed access since beans are typically key-value structures.
 * </p>
 */
public class JavaBeanAccessor extends AbstractBeanAccessor {

    public static final Map<Class<?>, JavaBeanDescriptor<?>> CACHED_DESCRIPTORS = new HashMap<>();

    /**
     * Creates a {@link JavaBeanAccessor} for the given structured instance.
     *
     * @param source the structured instance to wrap
     * @throws IllegalArgumentException if the source is {@code null}
     */
    public JavaBeanAccessor(Object source) {
        super(source);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected ObjectDescriptor<Object> getDescriptor(Class<?> type) {
        return (ObjectDescriptor<Object>) CACHED_DESCRIPTORS.computeIfAbsent(type, (t) ->
            new JavaBeanIntrospector<>(t).introspect().toDescriptor()
        );
    }

}
