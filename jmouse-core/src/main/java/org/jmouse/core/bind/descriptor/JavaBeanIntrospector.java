package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeIntrospector;
import org.jmouse.core.bind.introspection.MethodDescriptor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.Reflections;

import java.util.Map;

public class JavaBeanIntrospector<T> extends ObjectIntrospector<JavaBeanIntrospector<T>, Class<T>, JavaBeanDescriptor<T>> {

    public JavaBeanIntrospector(Class<T> target) {
        super(target);
    }

    @Override
    public JavaBeanIntrospector<T> name() {
        return name("[JBD] %s".formatted(Reflections.getShortName(container.getTarget())));
    }

    public JavaBeanIntrospector<T> type() {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forClass(container.getTarget()));
        container.setType(introspector.introspect().toDescriptor());
        return self();
    }

    @Override
    public JavaBeanIntrospector<T> introspect() {
        JavaBeanIntrospector<T> introspector = super.introspect().type();
        ClassTypeDescriptor     descriptor   = container.getType();

        for (Map.Entry<String, MethodDescriptor> entry : descriptor.getMethods().entrySet()) {
            MethodDescriptor method = entry.getValue();

            //
        }

        return introspector;
    }

    public JavaBeanIntrospector<T> property(MethodDescriptor md) {

    }

    @Override
    public JavaBeanDescriptor<T> toDescriptor() {
        return getDescriptor(() -> new JavaBeanDescriptor<>(this, container));
    }
}
