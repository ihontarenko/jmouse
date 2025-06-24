package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public class AnnotationMetadata {

    private Annotation annotation;
    private AnnotatedElement element;

    public AnnotatedElement getElement() {
        return element;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public Class<? extends Annotation> getAnnotationType() {
        return getAnnotation().annotationType();
    }

}
