package org.jmouse.context.binding.bean;

import org.jmouse.context.binding.Bindable;
import org.jmouse.context.binding.CachedSupplier;
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

final public class JavaBean<T> extends Bean<T> {

    public static final String GETTER_GET_PREFIX = "get";
    public static final String GETTER_IS_PREFIX  = "is";
    public static final String SETTER_PREFIX     = "set";

    public JavaBean(JavaType type) {
        super(type);
        addProperties();
    }

    public JavaBean(Class<?> type) {
        this(JavaType.forClass(type));
    }

    public static <T> JavaBean<T> of(Class<T> type) {
        return new JavaBean<>(type);
    }

    public static <T> JavaBean<T> of(JavaType type) {
        return new JavaBean<>(type);
    }

    private void addProperties() {
        Matcher<Executable> anyMatcher = nameStarts(GETTER_GET_PREFIX).or(nameStarts(GETTER_IS_PREFIX))
                .or(nameStarts(SETTER_PREFIX));
        Matcher<Executable> isSetter   = nameStarts(SETTER_PREFIX).and(hasParameterCount(1));
        Matcher<Executable> isGetter   = hasParameterCount(0);

        for (Method method : getMethods()) {
            if (anyMatcher.matches(method)) {
                addProperty(method, SETTER_PREFIX, isSetter, Property::setRawSetter);
                addProperty(method, GETTER_GET_PREFIX, isGetter.and(nameStarts(GETTER_GET_PREFIX)), Property::setRawGetter);
                addProperty(method, GETTER_IS_PREFIX, isGetter.and(nameStarts(GETTER_IS_PREFIX)), Property::setRawGetter);
            }
        }
    }

    private Collection<Method> getMethods() {
        return new MethodFinder().getMembers(type.getRawType(), true);
    }

    private void addProperty(Method method, String prefix, Matcher<Executable> matcher, BiConsumer<Property<T>, Method> adder) {
        if (matcher.matches(method)) {
            String   name     = Reflections.getPropertyName(method, prefix);
            Bean.Property<T> property = properties.computeIfAbsent(name, this::createProperty);
            adder.accept((Property<T>) property, method);
        }
    }

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

    private Bean.Property<T> createProperty(String name) {
        return new Property<>(name, type);
    }

    @Override
    public String toString() {
        return "JavaBean: %s; Properties: %d".formatted(type, properties.size());
    }

    public static class Property<T> extends Bean.Property<T> {

        public Property(String name, JavaType owner) {
            super(name, owner);
        }

        public Property(String name, Class<?> owner) {
            this(name, JavaType.forClass(owner));
        }

    }

}
