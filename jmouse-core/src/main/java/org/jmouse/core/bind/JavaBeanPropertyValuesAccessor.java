package org.jmouse.core.bind;

import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.core.bind.introspection.structured.jb.JavaBeanDescriptor;
import org.jmouse.core.bind.introspection.structured.jb.JavaBeanIntrospector;
import org.jmouse.util.Factory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link PropertyValuesAccessor} implementation for accessing properties of a structured instance.
 * <p>
 * This class allows retrieving properties dynamically from a wrapped structured instance.
 * It does not support indexed access since beans are typically key-value structures.
 * </p>
 */
public class JavaBeanPropertyValuesAccessor extends AbstractBeanPropertyValuesAccessor {

    /**
     * Creates a {@link JavaBeanPropertyValuesAccessor} for the given structured instance.
     *
     * @param source the structured instance to wrap
     * @throws IllegalArgumentException if the source is {@code null}
     */
    public JavaBeanPropertyValuesAccessor(Object source) {
        super(source);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected ObjectDescriptor<Object> getDescriptor(Class<?> type) {
        return new JavaBeanIntrospector<>((Class<Object>) type).introspect().toDescriptor();
    }

}
