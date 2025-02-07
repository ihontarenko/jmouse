package org.jmouse.core.bind;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Collection;
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

    public static final String GETTER_GET_PREFIX = "get";
    public static final String GETTER_IS_PREFIX  = "is";
    public static final String SETTER_PREFIX     = "set";

    /**
     * Constructs a JavaBean representation for the given type.
     *
     * @param type the Java type of the bean
     */
    public JavaBean(JavaType type) {
        super(type);
        addProperties();
    }

    /**
     * Constructs a JavaBean representation for the given class.
     *
     * @param type the class of the bean
     */
    public JavaBean(Class<?> type) {
        this(JavaType.forClass(type));
    }

    /**
     * Creates a JavaBean instance for a given class.
     *
     * @param <T>  the type of the bean
     * @param type the class of the bean
     * @return a JavaBean instance
     */
    public static <T> JavaBean<T> of(Class<T> type) {
        return new JavaBean<>(type);
    }

    /**
     * Creates a JavaBean instance for a given Java type.
     *
     * @param <T>  the type of the bean
     * @param type the JavaType of the bean
     * @return a JavaBean instance
     */
    public static <T> JavaBean<T> of(JavaType type) {
        return new JavaBean<>(type);
    }

    /**
     * Scans and adds properties to the bean based on getter and setter method conventions.
     */
    private void addProperties() {
        Matcher<Executable> anyMatcher = nameStarts(GETTER_GET_PREFIX).or(nameStarts(GETTER_IS_PREFIX))
                .or(nameStarts(SETTER_PREFIX));
        Matcher<Executable> isSetter = nameStarts(SETTER_PREFIX).and(hasParameterCount(1));
        Matcher<Executable> isGetter = hasParameterCount(0);

        for (Method method : getMethods()) {
            if (anyMatcher.matches(method)) {
                addProperty(method, SETTER_PREFIX, isSetter, Property::setRawSetter);
                addProperty(method, GETTER_GET_PREFIX, isGetter.and(nameStarts(GETTER_GET_PREFIX)), Property::setRawGetter);
                addProperty(method, GETTER_IS_PREFIX, isGetter.and(nameStarts(GETTER_IS_PREFIX)), Property::setRawGetter);
            }
        }
    }

    /**
     * Retrieves all methods declared in the bean class.
     *
     * @return a collection of methods
     */
    private Collection<Method> getMethods() {
        return new MethodFinder().getMembers(type.getRawType(), true);
    }

    /**
     * Adds a property to the bean if the method matches the specified criteria.
     *
     * @param method  the method to analyze
     * @param prefix  the expected prefix (e.g., "get", "set")
     * @param matcher the matcher to check if the method qualifies
     * @param adder   the consumer that assigns the method to a property
     */
    private void addProperty(Method method, String prefix, Matcher<Executable> matcher, BiConsumer<Property<T>, Method> adder) {
        if (matcher.matches(method)) {
            String           name     = Reflections.getPropertyName(method, prefix);
            Bean.Property<T> property = properties.computeIfAbsent(name, this::createProperty);
            adder.accept((Property<T>) property, method);
        }
    }

    /**
     * Creates a factory for constructing instances of this bean.
     *
     * @param bean the bindable bean
     * @return a factory that creates instances of the bean
     */
    @SuppressWarnings("unchecked")
    public Factory<T> getFactory(Bindable<T> bean) {
        return Factory.of(new CachedSupplier<>(() -> {
            T           instance = null;
            Supplier<T> supplier = bean.getValue();

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

    /**
     * Creates a new property instance.
     *
     * @param name the name of the property
     * @return the property instance
     */
    private Bean.Property<T> createProperty(String name) {
        return new Property<>(name, type);
    }

    @Override
    public String toString() {
        return "JavaBean: %s; Properties: %d".formatted(type, properties.size());
    }

    /**
     * Represents a property of a JavaBean.
     *
     * @param <T> the type of the bean containing the property
     */
    public static class Property<T> extends Bean.Property<T> {

        /**
         * Constructs a JavaBean property.
         *
         * @param name  the name of the property
         * @param owner the JavaType of the bean containing the property
         */
        public Property(String name, JavaType owner) {
            super(name, owner);
        }

        /**
         * Constructs a JavaBean property using a class reference.
         *
         * @param name  the name of the property
         * @param owner the class of the bean containing the property
         */
        public Property(String name, Class<?> owner) {
            this(name, JavaType.forClass(owner));
        }
    }
}
