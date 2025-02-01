package org.jmouse.context.binding;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.FieldFinder;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.jmouse.core.matcher.Matcher.constant;
import static org.jmouse.core.reflection.MethodMatchers.hasParameterCount;
import static org.jmouse.core.reflection.MethodMatchers.nameStarts;
import static org.jmouse.core.reflection.Reflections.findFirstConstructor;
import static org.jmouse.core.reflection.Reflections.instantiate;
import static org.jmouse.util.Strings.uncapitalize;

final public class JavaBean<T> {

    public static final String GETTER_GET_PREFIX = "get";
    public static final String GETTER_IS_PREFIX  = "is";
    public static final String SETTER_PREFIX     = "set";

    private final Map<String, Property> properties = new LinkedHashMap<>();
    private final JavaType              type;

    public JavaBean(JavaType type) {
        this.type = type;
        addProperties();
    }

    public JavaBean(Class<?> type) {
        this(JavaType.forClass(type));
    }

    private void addProperties() {
        Matcher<Executable> matcher = nameStarts(GETTER_GET_PREFIX)
                .or(nameStarts(GETTER_IS_PREFIX)).or(nameStarts(SETTER_PREFIX));

        for (Method method : getDeclaredMethods()) {
            if (matcher.matches(method)) {
                addMethod(method, SETTER_PREFIX, nameStarts(SETTER_PREFIX).and(hasParameterCount(1)),
                          Property::setSetter);
                addMethod(method, GETTER_GET_PREFIX, nameStarts(GETTER_GET_PREFIX).and(hasParameterCount(0)),
                          Property::setGetter);
                addMethod(method, GETTER_IS_PREFIX, nameStarts(GETTER_IS_PREFIX).and(hasParameterCount(0)),
                          Property::setGetter);
            }
        }

        getDeclaredFields().forEach(field -> addField(field, constant(true)));
    }

    private Collection<Method> getDeclaredMethods() {
        return new MethodFinder().getMembers(type.getRawType(), true);
    }

    private Collection<Field> getDeclaredFields() {
        return new FieldFinder().getMembers(type.getRawType(), true);
    }

    public void addMethod(Method method, String prefix, Matcher<Executable> matcher, BiConsumer<Property, Method> adder) {
        if (matcher.matches(method)) {
            adder.accept(properties.computeIfAbsent(
                    uncapitalize(method.getName().substring(prefix.length())), this::createProperty), method);
        }
    }

    public void addField(Field field, Matcher<Field> matcher) {
        if (matcher.matches(field)) {
            ofNullable(properties.get(field.getName())).ifPresent(property -> property.setField(field));
        }
    }

    public Collection<Property> getProperties() {
        return properties.values();
    }

    public Property getProperty(String name) {
        return properties.get(name);
    }

    @SuppressWarnings("unchecked")
    public Supplier<T> getSupplier(Bindable<T> bean) {
        return new CachedSupplier<>(() -> {
            T instance = null;
            Supplier<T> supplier = bean.getValue();

            if (supplier != null) {
                instance = supplier.get();
            }

            if (instance == null) {
                Constructor<T> constructor = (Constructor<T>) findFirstConstructor(type.getRawType());
                instance = instantiate(constructor);
            }

            return instance;
        });
    }

    public Property createProperty(String name) {
        return new Property(name, type);
    }

    public static <T> JavaBean<T> of(Class<T> type) {
        return new JavaBean<>(type);
    }

    public static <T> JavaBean<T> of(JavaType type) {
        return new JavaBean<>(type);
    }

    @Override
    public String toString() {
        return "Bean: %s; Properties: %d".formatted(type, properties.size());
    }

    public static class Property {

        private final String   name;
        private final JavaType owner;
        private       Field    field;
        private       Method   setter;
        private       Method   getter;

        public Property(String name, JavaType owner) {
            this.name = name;
            this.owner = owner;
        }

        public Property(String name, Class<?> owner) {
            this.name = name;
            this.owner = JavaType.forClass(owner);
        }

        public String getName() {
            return name;
        }

        public JavaType getType() {
            JavaType type = JavaType.forMethodReturnType(getter);

            // because setter is better
            if (setter != null) {
                type = JavaType.forMethodParameter(setter, 0);
            }

            return type;
        }

        public Supplier<Object> getValue(Supplier<?> instance) {
            return () -> {
                try {
                    getter.setAccessible(true);
                    return getter.invoke(instance.get());
                } catch (Exception exception) {
                    throw new IllegalStateException(
                            "Unable to get value from '%s' for property: '%s'"
                                    .formatted(Reflections.getMethodName(getter), name), exception);
                }
            };
        }

        public void setValue(Supplier<?> instance, Object value) {
            try {
                setter.setAccessible(true);
                setter.invoke(instance.get(), value);
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Unable to set value for property: '%s' via setter: '%s'"
                                .formatted(name, Reflections.getMethodName(setter)), exception);
            }
        }

        public Method getSetter() {
            return setter;
        }

        public void setSetter(Method setter) {
            this.setter = setter;
        }

        public Method getGetter() {
            return getter;
        }

        public void setGetter(Method getter) {
            this.getter = getter;
        }

        public Field getField() {
            return field;
        }

        public void setField(Field field) {
            this.field = field;
        }

        public boolean isWritable() {
            return setter != null;
        }

        public boolean isReadable() {
            return getter != null;
        }

        @Override
        public String toString() {
            return "(%s) [%s] : %s".formatted(owner, name, getType());
        }
    }

}
