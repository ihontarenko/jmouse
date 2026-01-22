package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanIntrospector;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.CachedSupplier;
import org.jmouse.core.Factory;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

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
    public JavaBean(InferredType type) {
        super(type, new JavaBeanIntrospector<T>(type.getRawType()).introspect().toDescriptor());
    }

    /**
     * Constructs a JavaBean representation for the given class.
     *
     * @param type the class of the structured
     */
    public JavaBean(Class<?> type) {
        this(InferredType.forClass(type));
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
    public static <T> JavaBean<T> of(InferredType type) {
        return new JavaBean<>(type);
    }

    /**
     * Creates a factory for constructing instances of this bindable.
     *
     * @param bindable the bindable
     * @return a factory that creates instances of the bindable
     */
    @SuppressWarnings("unchecked")
    public Factory<T> getFactory(TypedValue<T> bindable) {
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
}
