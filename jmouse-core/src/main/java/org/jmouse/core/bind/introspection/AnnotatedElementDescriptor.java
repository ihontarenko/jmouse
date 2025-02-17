package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.AnnotatedElementData;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.List;

abstract public class AnnotatedElementDescriptor<E extends AnnotatedElement, C extends AnnotatedElementData<E>, I extends AnnotatedElementIntrospector<?, ?, ?, ?>> extends AbstractDescriptor<E, C, I> {

    protected AnnotatedElementDescriptor(I introspector, C container) {
        super(introspector, container);
    }

    public List<AnnotationDescriptor> getAnnotations() {
        return Collections.unmodifiableList(container.getAnnotations());
    }

    public AnnotationDescriptor findAnnotation(final Class<? extends Annotation> annotationType) {
        AnnotationDescriptor descriptor = null;

        for (AnnotationDescriptor annotation : getAnnotations()) {
//            if (annotation.getType().)
        }

        return descriptor;
    }

}
