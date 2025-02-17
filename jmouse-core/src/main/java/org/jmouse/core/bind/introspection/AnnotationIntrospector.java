package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.AnnotationData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationIntrospector extends AbstractIntrospector<AnnotationData, AnnotationIntrospector, Annotation, AnnotationDescriptor> {

    protected AnnotationIntrospector(Annotation target) {
        super(target);
    }

    @Override
    public AnnotationIntrospector name() {
        return name("@" + container.getTarget().annotationType().getSimpleName());
    }

    @Override
    public AnnotationIntrospector introspect() {
        return name().annotationType();
    }

    public AnnotationIntrospector annotationType() {
        Annotation annotation = container.getTarget();

        ClassTypeDescriptor descriptor = new ClassTypeIntrospector(annotation.annotationType())
                .type().name().toDescriptor();

        container.setAnnotationType(descriptor);

        return self();
    }

    public AnnotationIntrospector attributes() {
        Annotation                  annotation     = container.getTarget();
        Class<? extends Annotation> annotationType = annotation.annotationType();

        try {
            for (Method method : annotationType.getDeclaredMethods()) {
                container.addAttribute(method.getName(), method.invoke(annotation));
            }
        } catch (Exception ignored) {
        }

        return self();
    }

    @Override
    public AnnotationDescriptor toDescriptor() {
        return new AnnotationDescriptor(this, container);
    }

    @Override
    public AnnotationData getContainerFor(Annotation target) {
        return new AnnotationData(target);
    }

}
