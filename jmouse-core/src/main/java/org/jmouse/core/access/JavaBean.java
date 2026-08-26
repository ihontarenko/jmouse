package org.jmouse.core.access;

import org.jmouse.core.access.descriptor.structured.bean.JavaBeanIntrospector;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.CachedSupplier;
import org.jmouse.core.Factory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private volatile Constructor<T> constructor;

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
     * One {@link JavaBean} per class, because building one introspects that class in full.
     *
     * <p>⚠️ {@link #of(InferredType)} sits on the mapping engine's hot path - once per mapped object -
     * and every call used to re-run {@link JavaBeanIntrospector} over the target class: its fields, its
     * methods, and the annotations on each of them. Profiling the flat bean path put <b>57%</b> of the
     * engine's time inside this constructor, introspecting the same class over and over.</p>
     *
     * <p>A {@code JavaBean} holds a type and a descriptor and mutates neither, and
     * {@link #getFactory(TypedValue)} builds a fresh factory per call, so one instance is safely shared.
     * The public constructors remain the way to force a fresh introspection.</p>
     *
     * <p>Keyed by the raw class: that is what the introspector is given, so two {@link InferredType}s
     * over the same class describe the same bean.</p>
     */
    private static final Map<Class<?>, JavaBean<?>> CACHE = new ConcurrentHashMap<>();

    /**
     * Creates a JavaBean instance for a given class.
     *
     * @param <T>  the type of the structured
     * @param type the class of the structured
     * @return a JavaBean instance
     */
    public static <T> JavaBean<T> of(Class<T> type) {
        return of(InferredType.forClass(type));
    }

    /**
     * Creates a JavaBean instance for a given Java type.
     *
     * @param <T>  the type of the structured
     * @param type the JavaType of the structured
     * @return a JavaBean instance
     */
    @SuppressWarnings("unchecked")
    public static <T> JavaBean<T> of(InferredType type) {
        return (JavaBean<T>) CACHE.computeIfAbsent(type.getRawType(), ignore -> new JavaBean<>(type));
    }

    /**
     * Creates a factory for constructing instances of this bindable.
     *
     * @param typedValue the bindable
     * @return a factory that creates instances of the bindable
     */
    public Factory<T> getFactory(TypedValue<T> typedValue) {
        return Factory.of(new CachedSupplier<>(() -> {
            T           instance = null;
            Supplier<T> supplier = typedValue.getValue();

            if (supplier != null) {
                instance = supplier.get();
            }

            if (instance == null) {
                instance = instantiate(constructor());
            }

            return instance;
        }));
    }

    /**
     * The constructor used to materialize this bean, resolved once.
     *
     * <p>⚠️ This used to be looked up inside the supplier above, which reads as cached and is not: a
     * fresh {@link CachedSupplier} is built by every {@code getFactory} call, and the mapping engine
     * calls it once per mapped object. Finding it streams over the class's declared constructors and
     * copies each one out of the reflection layer - and after descriptor caching landed, that search
     * was the single largest thing left on the flat path.</p>
     *
     * <p>Resolved lazily rather than in the constructor, because a bean used only as a mapping
     * <em>source</em> is never instantiated and may legitimately offer nothing to instantiate it with.
     * Two threads racing here both resolve the same constructor, so the race is harmless.</p>
     *
     * @return the constructor to instantiate this bean with
     */
    @SuppressWarnings("unchecked")
    private Constructor<T> constructor() {
        Constructor<T> resolved = constructor;

        if (resolved == null) {
            resolved = (Constructor<T>) findFirstConstructor(type.getRawType());
            constructor = resolved;
        }

        return resolved;
    }

    @Override
    public String toString() {
        return "JavaBean: %s; Properties: %d".formatted(type, getProperties().size());
    }
}
