package org.jmouse.core.access.descriptor;

import org.jmouse.core.access.descriptor.internal.AnnotatedElementData;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

abstract public class AnnotatedElementIntrospector<C extends AnnotatedElementData<E>, I extends AnnotatedElementIntrospector<?, ?, ?, ?>, E extends AnnotatedElement, D extends AnnotatedElementDescriptor<?, ?, ?>> extends AbstractIntrospector<C, I, E, D> {

    protected AnnotatedElementIntrospector(E target) {
        super(target);
    }

    public I annotation(AnnotationDescriptor descriptor) {
        container.addAnnotation(descriptor);
        return self();
    }

    public I annotations() {
        D parent = toDescriptor();

        for (Annotation annotation : container.getTarget().getAnnotations()) {
            annotation(new AnnotationIntrospector(annotation).introspect().toDescriptor());
        }

        return self();
    }

}
