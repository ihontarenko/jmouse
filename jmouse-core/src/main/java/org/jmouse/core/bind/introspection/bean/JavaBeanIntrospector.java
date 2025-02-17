package org.jmouse.core.bind.introspection.bean;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeIntrospector;
import org.jmouse.core.bind.introspection.MethodDescriptor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Getter;

import java.util.List;
import java.util.Map;

public class JavaBeanIntrospector<T> extends ObjectIntrospector<JavaBeanIntrospector<T>, Class<T>, JavaBeanDescriptor<T>> {

    public JavaBeanIntrospector(Class<T> target) {
        super(target);
    }

    @Override
    public JavaBeanIntrospector<T> name() {
        return name(Reflections.getShortName(container.getTarget()));
    }

    public JavaBeanIntrospector<T> type() {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forClass(container.getTarget()));
        container.setType(introspector.introspect().toDescriptor());
        return self();
    }

    @Override
    public JavaBeanIntrospector<T> introspect() {
        return name().type().properties();
    }

    public JavaBeanIntrospector<T> properties() {
        ClassTypeDescriptor   descriptor = container.getType();
        JavaBeanDescriptor<T> parent     = toDescriptor();

        for (Map.Entry<String, MethodDescriptor> entry : descriptor.getMethods().entrySet()) {
            MethodDescriptor method = entry.getValue();

            String propertyName = Reflections.getPropertyName(method.unwrap(), method.isGetter() && method.isGetter("is") ? "xx" : "xxx");

            if (method.isGetter()) {
                Getter<T, Object> getter = Getter.ofMethod(method.unwrap());

                if (method.isGetter("is")) {
                    Getter<T, Boolean> booleanGetter = Getter.ofMethod(method.unwrap());
                }

                JavaBeanPropertyDescriptor<?> propertyDescriptor = new JavaBeanPropertyIntrospector<>(null)
                        .introspect().toDescriptor();

                container.addProperty(propertyDescriptor);
            }

            List<Long> longs = List.of(1L, 2L, 3L);
            add(longs);

//            container.addProperty();
            System.out.println(propertyName);
        }

        return self();
    }

    public void add(List<? extends Number> numbers) {

    }

    public JavaBeanIntrospector<T> property(MethodDescriptor descriptor) {
        return self();
    }

    @Override
    public JavaBeanDescriptor<T> toDescriptor() {
        return getCachedDescriptor(() -> new JavaBeanDescriptor<>(this, container));
    }

    @Override
    public String toString() {
        return "Introspector: " + container;
    }
}
