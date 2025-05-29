package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanIntrospector;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.CachedSupplier;
import org.jmouse.util.Factory;
import org.jmouse.util.SingletonSupplier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.MethodMatchers.hasParameterCount;
import static org.jmouse.core.reflection.MethodMatchers.nameStarts;
import static org.jmouse.core.reflection.Reflections.findFirstConstructor;
import static org.jmouse.core.reflection.Reflections.instantiate;

/**
 * Represents a JavaBean-based model that encapsulates metadata and properties of a standard Java object.
 * <p>
 * This class extracts and manages properties based on standard JavaBean conventions,
 * allowing dynamic property access and manipulation.
 * </p>
 *
 * @param <T> the type of the JavaBean instance
 */
public final class JavaBean<T> extends Bean<T> {

    /**
     * Constructs a JavaBean representation for the given type.
     *
     * @param type the Java type of the structured
     */
    public JavaBean(JavaType type) {
        super(type, new JavaBeanIntrospector<T>(type.getRawType()).introspect().toDescriptor());
    }

    /**
     * Constructs a JavaBean representation for the given class.
     *
     * @param type the class of the structured
     */
    public JavaBean(Class<?> type) {
        this(JavaType.forClass(type));
    }

    /**
     * Creates a JavaBean instance for a given class.
     *
     * @param <T>  the type of the structured
     * @param type the class of the structured
     * @return a JavaBean instance
     */
    public static <T> JavaBean<T> of(Class<T> type) {
        return new JavaBean<>(type);
    }

    /**
     * Creates a JavaBean instance for a given Java type.
     *
     * @param <T>  the type of the structured
     * @param type the JavaType of the structured
     * @return a JavaBean instance
     */
    public static <T> JavaBean<T> of(JavaType type) {
        return new JavaBean<>(type);
    }

    /**
     * Creates a factory for constructing instances of this bindable.
     *
     * @param bindable the bindable
     * @return a factory that creates instances of the bindable
     */
    @SuppressWarnings("unchecked")
    public Factory<T> getFactory(Bindable<T> bindable) {
        return Factory.of(new CachedSupplier<>(() -> {
            T           instance = null;
            Supplier<T> supplier = bindable.getValue();

            if (supplier != null) {
                instance = supplier.get();
            }

            if (instance == null) {
                Constructor<T> constructor = (Constructor<T>) findFirstConstructor(type.getRawType());
                instance = instantiate(constructor);
            }

            return instance;
        }));
    }

    @Override
    public String toString() {
        return "JavaBean: %s; Properties: %d".formatted(type, getProperties().size());
    }

    /**
     * Retrieves all properties defined in this structured.
     *
     * @return a collection of properties
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public Collection<? extends PropertyDescriptor<T>> getProperties() {
        return ((JavaBeanDescriptor<T>)descriptor).getProperties().values();
    }

    /**
     * Retrieves a specific property by name.
     *
     * @param name the name of the property
     * @return the property associated with the given name, or {@code null} if not found
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public PropertyDescriptor<T> getProperty(String name) {
        return ((JavaBeanDescriptor<T>)descriptor).getProperty(name);
    }

    /**
     * Checks whether this structured contains a property with the given name.
     *
     * @param name the property name to check
     * @return {@code true} if the property exists, otherwise {@code false}
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public boolean hasProperty(String name) {
        return ((JavaBeanDescriptor<T>)descriptor).hasProperty(name);
    }
}
